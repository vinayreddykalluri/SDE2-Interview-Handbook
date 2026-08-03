# Replication, Read/Write Concerns, Transactions, and Retries

Replica sets provide redundancy and elections. A primary accepts ordinary writes; secondaries replicate the operation log and apply changes. Failover changes which member is primary and can interrupt in-flight operations.

## Internal write flow

```text
Java driver selects primary
  -> command reaches primary
  -> document/index changes in WiredTiger transaction
  -> journal/checkpoint machinery supports recovery
  -> oplog record replicated to secondaries
  -> write concern decides acknowledgement point
  -> response or network failure reaches client
```

The exact storage timing/configuration varies. Do not equate “acknowledged” with “every secondary has applied it” or “data file page flushed everywhere.” State the selected write concern and journaling policy.

## Write concern

Write concern controls required acknowledgement. Majority acknowledgement generally protects against rollback through ordinary elections better than a single-member acknowledgement, at latency/availability cost. Timeouts limit waiting but can yield an **unknown outcome**: the write may later satisfy the requested acknowledgement even though the client received a timeout.

Use a unique business/request key so retry or reconciliation can discover the result.

## Read preference and read concern are separate

- **Read preference** chooses eligible members: primary, primary-preferred, secondary, and variants.
- **Read concern** controls consistency/isolation guarantees of returned data.

Reading from a secondary can reduce some source read load or place reads geographically, but it can be stale. `majority` read concern means majority-committed data, not necessarily the newest operation currently known anywhere. `linearizable` is a stronger single-document real-time read contract with restrictions/cost and should use a time bound. `snapshot` serves point-in-time semantics in supported contexts.

Do not memorize settings without a product requirement. Order confirmation may need primary/causal read-after-write; analytics may tolerate secondary lag.

## Causal sessions

A causally consistent session can preserve relationships such as read-your-writes and monotonic reads when used with compatible concerns. The session context/operation time must flow with the logical request. A random next request on another session does not inherit causality.

## Retryable reads and writes

Modern drivers can retry selected operations after transient network/election failures. Retryability is not permission to repeat arbitrary application workflows. A driver can retry a supported single write with an operation identity; it cannot make an email, HTTP call, and database change atomic.

Classify errors using driver labels such as transient transaction or unknown commit results per current driver guidance. Bound application retries and record attempts/latency.

## Transactions

```java
session.withTransaction(() -> {
    orders.updateOne(session, orderFilter, orderUpdate);
    outbox.insertOne(session, eventDocument);
    return null;
});
```

The callback can run more than once. Keep it deterministic and database-only, or make external behavior idempotent outside it. Transaction read concern is set at transaction start; commit uses transaction-level write concern. Long transactions retain resources/history and increase conflict/failure probability.

Transactions require replica-set or sharded-cluster support, not a standalone server.

## Conflict/commit timeline

```text
attempt 1: read snapshot -> update -> transient conflict -> abort
attempt 2: callback reruns -> update -> commit request -> network loss
commit result: unknown -> retry commit/reconcile, not whole external workflow blindly
```

## Failure matrix

| Failure | State | Response |
|---|---|---|
| primary stepdown before write | likely no accepted write | driver retry if labeled/supported |
| timeout after send | unknown | reconcile by idempotency key |
| transient transaction error | attempt aborted | retry whole DB callback within budget |
| unknown transaction commit result | commit uncertain | retry commit according to driver contract |
| duplicate key | invariant conflict/prior request | interpret, do not generic retry |
| stale secondary read | allowed by routing/lag | stronger route/session if contract needs it |

## Practice and solution direction

- **Foundation:** Separate read preference from read concern.
- **Interview Core:** Select guarantees for order confirmation versus recommendation feed.
- **SDE-2 Follow-up:** Design a transaction callback that writes an order and event without emailing twice. Persist an outbox document in the transaction; deliver idempotently afterward.
