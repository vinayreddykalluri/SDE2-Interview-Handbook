# Transactions, Isolation, Locks, Deadlocks, and Retries

A transaction is a boundary for atomic database work. It is not a magic wrapper around network calls, message publication, or external APIs.

## ACID in operational language

- **Atomicity:** the database commits the transaction’s effects or rolls them back.
- **Consistency:** declared database rules plus correct transaction logic preserve invariants; the database cannot invent missing business rules.
- **Isolation:** concurrent executions observe behavior defined by the isolation level and statement types.
- **Durability:** after acknowledged commit, recovery should preserve effects according to configured durability guarantees.

## Autocommit changes the unit of work

With autocommit, each statement is normally its own transaction. A read-then-write sequence is therefore not atomic unless you create an explicit transaction or encode the condition in one statement.

```sql
UPDATE inventory
SET available = available - 1
WHERE sku = ? AND available > 0;
```

If affected rows is one, stock was reserved atomically. If zero, it was missing or exhausted. This is often clearer than `SELECT`, Java decrement, `UPDATE`.

## Isolation anomalies

| Anomaly | Shape | Protection approach |
|---|---|---|
| dirty read | read another transaction’s uncommitted data | use supported normal isolation; InnoDB does not expose uncommitted versions in ordinary reads as a general design tool |
| nonrepeatable read | same row reads differently | snapshot semantics or locking read, depending requirement |
| phantom | repeated predicate yields new matching rows | predicate/range locking or serializable strategy when invariant needs it |
| lost update | later writer overwrites earlier change | conditional update, version column, or lock |
| write skew | transactions update different rows after shared predicate | materialize/lock invariant or serialize decision |

MySQL/InnoDB behavior depends on isolation level and whether a read is consistent (`SELECT`) or locking (`SELECT ... FOR UPDATE`). State both. Default isolation can also be configured; verify the environment instead of reciting a default as universal.

## MVCC is not the same as “no locks”

Ordinary consistent reads use versions. Writes acquire locks. Locking reads acquire locks. InnoDB may use record, gap, or next-key locking to protect searched ranges depending on isolation, index, uniqueness, and predicate.

```sql
START TRANSACTION;
SELECT available
FROM inventory
WHERE sku = ?
FOR UPDATE;
-- validate and update quickly
COMMIT;
```

Use locking reads when the invariant truly needs serialization and keep the transaction small. Do not hold a row lock while calling a payment provider.

## Deadlocks are cycles, not merely slow locks

```text
T1 holds order 10, waits for order 20
T2 holds order 20, waits for order 10
                 -> cycle -> one victim is rolled back
```

Prevention and response:

1. access shared resources in a stable order;
2. use selective indexes so statements do not lock more range than intended;
3. keep transactions short and avoid user/network wait inside them;
4. log enough context to diagnose the cycle;
5. retry the **entire** transaction only when its side effects are safe and the error is classified retryable;
6. bound retries, add jitter, and surface persistent contention.

A lock timeout is waiting beyond a configured limit; a deadlock is a detected cycle. Both may invite a retry policy, but their evidence and remediation differ.

## Unknown commit outcome

If the socket breaks around `COMMIT`, the client may not know whether the server committed. Retrying a non-idempotent insert can duplicate work.

Use a unique request key:

```sql
INSERT INTO payment_request(request_key, order_id, amount_cents, status)
VALUES (?, ?, ?, 'STARTED');
```

After reconnect, query by `request_key`. The same pattern coordinates an outbox record for later message publication without pretending SQL and Kafka share one atomic transaction.

## Optimistic versus pessimistic control

**Optimistic:** update with expected version and retry/reject on zero rows. Best when collisions are uncommon and work can be repeated.

```sql
UPDATE purchase_order
SET status = ?, version = version + 1
WHERE order_id = ? AND version = ?;
```

**Pessimistic:** lock before decision. Best when collision is likely or late failure is very expensive, but it consumes locks/connections and can deadlock.

## Concurrency case study: reserve two seats

Naive logic reads each seat, calls another service, then updates. It holds locks too long and can partially act externally.

A stronger design:

1. sort seat IDs;
2. transactionally lock/update both in the same order;
3. insert reservation and outbox event with unique request key;
4. commit;
5. publish asynchronously; consumer deduplicates;
6. compensate an expired reservation through an explicit state transition.

## Failure matrix

| Event | Transaction state | Retry? |
|---|---|---|
| syntax/constraint error | rollback unit of work | fix request/code, generally no |
| deadlock victim | rolled back | bounded whole-transaction retry |
| lock timeout | framework/driver may mark failure; rollback explicitly | only after classification and policy |
| connection lost before begin | no work known | reconnect; repeat if safe |
| connection lost during commit | unknown outcome | reconcile by idempotency key |
| Java caught exception but transaction continues | dangerous partial-work assumption | mark rollback or fail boundary |

## Practice

- **Foundation:** Explain why autocommit breaks a read-then-write unit.
- **Interview Core:** Protect coupon redemption limited to 100 uses.
- **Interview Core:** Draw a two-row deadlock and remove its cycle.
- **SDE-2 Follow-up:** Design retry handling when a database transaction also schedules an email.

## Solution direction

Prefer one conditional increment guarded by `< 100`, or a locked counter when more validation is required. Use an outbox for email intent; do not send the email inside the retried transaction.
