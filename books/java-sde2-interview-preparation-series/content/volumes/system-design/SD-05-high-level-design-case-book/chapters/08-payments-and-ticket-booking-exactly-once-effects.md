# 8. Payments and Ticket Booking: Exactly-Once Effects

## Learning objectives

By the end of this chapter, you should be able to:

- make a payment request safe to retry with an idempotency key, and state what the key must cover;
- record money movements in a double-entry ledger and assert its invariant;
- integrate an external payment provider whose calls may time out with an unknown result;
- keep the order database and the event stream consistent with a transactional outbox;
- design seat inventory so that no seat is sold twice under a flash-sale load; and
- reconcile your records against the provider's.

## Why this matters at SDE-2

Payments and booking are where "eventually consistent" stops being an acceptable answer. Charging a customer twice, or selling one seat to two people, is a visible, costly failure. Interviewers use these prompts to see whether a candidate knows that networks deliver at least once, that exactly-once is built from idempotency plus deduplication, and that the hard part is the timeout whose outcome you do not know.

## First-principles model

Exactly-once **delivery** is impossible over an unreliable network. Exactly-once **effects** are achievable: deliver at least once, and make every effect idempotent so that repeats do nothing.

Three tools do all the work in this chapter:

1. **Idempotency keys** make repeated requests return the original result.
2. **A double-entry ledger** makes every money movement balance, so that errors are detectable.
3. **Conditional updates** make contended inventory changes atomic.

## Core terminology

- **Idempotency key:** a client-generated id for one logical operation; the server stores the result under it.
- **Double-entry ledger:** every transaction is a set of entries across accounts that sum to zero.
- **Payment service provider (PSP):** the external system that actually moves money (card networks, wallets).
- **Transactional outbox:** writing an event to an outbox table in the same database transaction as the state change, then publishing it asynchronously.
- **Saga:** a sequence of local transactions with compensating actions on failure.
- **Reconciliation:** comparing internal records with the provider's settlement reports.
- **Hold (reservation):** a temporary claim on inventory that expires unless confirmed.

## Detailed mechanics

### Idempotency keys

The client generates a key per logical payment - typically per checkout attempt - and sends it with every retry. The server stores the key with the result. A repeated key returns the stored result without charging again.

The companion replays 100 orders where each request is delivered one to four times:

```text
100 orders, 238 charge requests (retries included) -> 100 charges
merchant revenue $1,999.00, every transaction sums to zero: true
```

Details that matter:

- **Scope the key to the operation, not the request.** A new key per HTTP retry defeats the purpose.
- **Store the key and the effect atomically.** If the charge is recorded and the key is not, a retry charges again. In one database, write both in one transaction; the unique constraint on the key is what makes concurrent duplicates safe.
- **Bind the key to the request body.** A reused key with a different amount is an error, not a cache hit.
- **Keep keys long enough** to cover every client's retry window - days, not minutes.

### The double-entry ledger

Never store a balance as a single mutable number that you add to and subtract from. Record every movement as a transaction with at least two entries that sum to zero:

```text
txn pay-17:   customer:c3        -1,999
              merchant:revenue   +1,999
```

A balance is the sum of an account's entries (cached for speed). The invariant - every transaction sums to zero, so the whole ledger sums to zero - is checkable at any time, and the companion checks it after every charge. A bug that creates or destroys money shows up as a non-zero sum, rather than as a customer complaint weeks later. Entries are append-only; a refund is a new transaction, never an edit.

### The external provider and the unknown outcome

The hardest case: you call the provider to charge a card and the call times out. The charge may or may not have happened.

- **Never assume failure and retry blindly with a new request.** That is how customers get charged twice.
- **Send your idempotency key to the provider**, so that a retry with the same key is safe on their side too; most providers support this.
- **Record a `PENDING` state before the call**, and resolve it afterwards: by the call's response, by a webhook from the provider, or by querying the provider's status for your key.
- **A reconciler** periodically resolves payments stuck in `PENDING` by querying the provider.

The state machine is short and every edge is explicit: `CREATED -> PENDING -> SUCCEEDED | FAILED`, with `PENDING` resolved only by evidence from the provider.

### Keeping the database and the event stream consistent

After a payment succeeds, other services need to know - to ship the order, send a receipt, update analytics. Writing to the database and then publishing to a queue is two operations; a crash between them loses the event, and publishing first can announce a payment that never committed.

The **transactional outbox** fixes this: in the same database transaction as the state change, insert an event row into an `outbox` table. A separate relay reads the outbox and publishes to the queue, marking rows as sent. Publishing is at-least-once, so consumers deduplicate by event id. SD-02 covers the pattern in depth; here, name it and place it.

### Ticket booking: contended inventory

Booking has the same exactly-once concerns plus heavy contention on a small set of rows during a popular sale.

The core operation is a conditional update on the seat:

```text
UPDATE seats SET state = 'HELD', hold_id = ?, hold_expires = now() + 10 min
WHERE event_id = ? AND seat_id = ? AND state = 'AVAILABLE'
```

One affected row means the hold succeeded; zero means someone else got it. The companion runs the in-memory equivalent - 50 buyers racing for 10 seats with compare-and-set:

```text
50 buyers race for 10 seats with compare-and-set: 10 booked, 10 distinct holders
```

The rest of the design follows from the flow:

- **Hold, then pay, then confirm.** A seat is held while the customer pays; confirmation turns the hold into a booking. Expired holds return to available - lazily when read, and with a periodic sweep.
- **Payment is the idempotent operation from earlier in this chapter**, keyed by the hold id, so a retried checkout cannot pay twice for one hold.
- **Flash-sale load:** put a virtual waiting room in front of the booking service so that it admits users at a rate the inventory database can serve, rather than letting a million requests contend for the same rows at once.
- **General admission** (a count, not specific seats) is a conditional decrement - `UPDATE ... SET remaining = remaining - n WHERE remaining >= n` - or a pre-split set of counters across shards when one row is too hot.

### Reconciliation

Once a day at least, compare your ledger with the provider's settlement report, line by line. Mismatches - a charge they have that you do not, an amount that differs - go to a queue for investigation. Reconciliation is the backstop that catches everything the rest of the design missed, and financial systems are expected to have one.

## Failure modes and common mistakes

- **Retrying a timed-out charge without an idempotency key.** Double charges.
- **A mutable balance column.** Errors are silent; use a ledger with a zero-sum invariant.
- **Key and effect stored in separate transactions.** A crash between them re-enables duplicates.
- **Dual write to database and queue.** Lost or phantom events; use an outbox.
- **Check-then-update seat booking.** Double-sold seats; use a conditional update.
- **Holds that never expire.** Inventory leaks until someone notices.
- **No reconciliation.** Discrepancies are found by customers or auditors instead.

## Interview questions and model answers

**Q: How do you avoid charging a customer twice?**
A: The client sends an idempotency key per checkout attempt, on every retry. The server stores the key and the result in the same transaction as the charge, under a unique constraint, and returns the stored result for a repeated key. I pass the same key to the payment provider so that retries are safe on their side too.

**Q: The provider call times out. Was the customer charged?**
A: Unknown, so the payment stays `PENDING`. It is resolved by the provider's webhook or by querying the provider for our idempotency key - never by assuming it failed and charging again. A reconciler sweeps anything left pending.

**Q: How do you store money movements?**
A: As a double-entry ledger: each transaction is entries that sum to zero, balances are sums of entries, and entries are append-only. The zero-sum invariant is checked continuously, so a bug that creates or destroys money is detected.

**Q: How do you prevent a seat being sold twice in a flash sale?**
A: A conditional update from `AVAILABLE` to `HELD`, so only one request can win. Holds expire after a few minutes. In front of it, a waiting room admits users at a rate the database can handle, so that a million people are not contending for the same rows at once.

## Exercises

1. Write the full state machine for a payment, including refunds and partial refunds, and list which transitions are triggered by the provider.
2. Design idempotency-key storage for 10 million payments per day with a seven-day retention.
3. For a stadium sale of 50,000 seats and 2 million interested users, design the waiting room: admission rate, fairness, and what users see.

## Chapter summary

Exactly-once effects come from at-least-once delivery plus idempotency. Idempotency keys, stored atomically with the effect and passed to the provider, make charges safe to retry; timeouts leave payments `PENDING` until evidence resolves them. A double-entry ledger makes money movements checkable; an outbox keeps state and events consistent. Seat inventory uses conditional updates and expiring holds, protected by a waiting room during flash sales, and reconciliation catches whatever is left.

## Revision checklist

- [ ] I can list four rules for idempotency keys.
- [ ] I can explain the double-entry invariant and why it beats a balance column.
- [ ] I can handle a provider timeout without risking a double charge.
- [ ] I can explain the transactional outbox and why consumers still deduplicate.
- [ ] I can write the conditional update for a seat hold and describe the waiting room.
