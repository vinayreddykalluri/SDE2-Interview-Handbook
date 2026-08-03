# Transactions, Flush Boundaries, and Write Strategy

Many Spring Data interview issues are not repository syntax. They are transaction lifecycle issues.

## Transaction boundary model

```text
Service method entry
    -> begin tx
    -> repository write(s)
    -> flush/dirty checks
    -> commit or rollback
```

If transaction annotations are absent, each repository call may still run, but exception handling and visibility become confusing.

The application-service method normally owns the transaction because it owns the invariant. Repository methods participate; they do not make a multi-step use case atomic merely by being repositories.

## Flush behavior (why it matters)

`flush` controls when pending changes are sent to the store.

- At commit: predictable, fewer intermediate SQL operations.
- Manual early flush: detects failures sooner.
- Too many manual flushes can increase round trips.

An explicit early flush can be useful when the application deliberately needs a known database constraint result before continuing local work. After a persistence exception, treat the transaction as failed and roll it back; do not keep using a possibly inconsistent persistence context.

Do **not** use flush to justify a slow remote call inside the database transaction. Flushing sends SQL, but the transaction can still hold locks and a pooled connection until commit or rollback. Commit local state plus a durable intent/outbox, then call the remote system outside the local transaction with idempotency and reconciliation.

## Propagation and isolation quick map

- `REQUIRED`: join existing transaction or start one.
- `REQUIRES_NEW`: suspend the caller's transaction and create an independent physical transaction when supported. It is not a retry mechanism and can require another pooled connection.
- Isolation tuning appears only after correctness and lock trade-offs are mapped.

A retry policy is separate: classify a transient database failure, roll back the failed transaction, refresh/re-read state, start a fresh transaction, keep one logical idempotency identity, and stop within an attempt/deadline budget.

## Write path from method to durable state

```text
service proxy enters
  -> transaction manager obtains/binds connection
  -> repository proxy selects implementation
  -> EntityManager changes managed state
  -> flush converts changes to SQL
  -> database checks constraints/locks/version predicate
  -> transaction commits
  -> only now is local state durable
```

SQL can also flush before commit when a query must observe pending changes, when `saveAndFlush`/`flush` is called, or under provider-specific flush rules. The Java line containing `save` is not a universal durability point.

## Failure and recovery matrix

| Failure | State at observation | Correct response |
|---|---|---|
| validation before transaction | no database work | correct request/domain input |
| unique constraint at flush | transaction failed/rollback required | classify known constraint; return conflict or replay |
| optimistic version conflict | another commit won | re-read, reapply if business-safe, bounded fresh-transaction retry |
| deadlock victim/serialization failure | database rolled back transaction | retry whole idempotent local unit if vendor code is classified |
| remote timeout after local commit | remote outcome unknown; DB committed | status lookup/retry same idempotency key/reconcile |
| `REQUIRES_NEW` waits for connection | outer transaction may hold one while inner needs another | revisit semantics and bound concurrency/pool demand |

## Common mistake

- Assuming repository write failure happens at method return.
- Assuming read-only methods can safely call writing repository operations.
- Ignoring optimistic lock exceptions and treating them as fatal framework issues.

## Quick check

1. What does transaction boundary ownership belong to in clean architecture?
2. Why does flush not make a following remote call atomic or cheap?
3. Why must a retry begin after the failed transaction has rolled back?

## Debugging exercise

Service A calls Service B with propagation defaults and performs a remote payment call.

Payment succeeds, then DB check fails on B.

Explain the rollback behavior and interview-safe fix.

Expected: separate the boundaries intentionally, define compensation strategy where business requires, or move side-effect outside the transaction with a durable intent.

## Practice

- **Foundation:** Draw the service-repository-transaction boundary for one command endpoint.
- **Interview Core:** Describe `REQUIRES_NEW` in one sentence, including its connection and atomicity cost.
- **SDE-2 Follow-up:** For your named database, explain its documented lock types and deadlock evidence. Do not assume vendor-independent lock escalation.

## Interviewer question and model answer

**Interviewer:** Should I call `saveAndFlush`, then invoke a payment provider so I know the order was written?

**Model answer:** Not as a default. Flush can surface a database constraint earlier, but the transaction is still uncommitted and can retain locks and its connection during the remote call. The provider cannot participate in that local atomic commit, and a timeout has an unknown remote outcome. I commit an order state plus outbox or payment intent in one short transaction, call the provider with a stable idempotency key outside it, and update/reconcile status in another transaction. I use `saveAndFlush` only when early SQL synchronization is itself required and tested.
