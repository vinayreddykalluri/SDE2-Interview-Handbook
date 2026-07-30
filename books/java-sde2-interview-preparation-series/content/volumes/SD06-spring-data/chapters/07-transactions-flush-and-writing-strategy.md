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

## Flush behavior (why it matters)

`flush` controls when pending changes are sent to the store.

- At commit: predictable, fewer intermediate SQL operations.
- Manual early flush: detects failures sooner.
- Too many manual flushes can increase round trips.

For read-write commands, explicit flush can be a useful interview choice when a subsequent validation depends on DB constraints.

## Propagation and isolation quick map

- `REQUIRED`: join existing transaction or start one.
- `REQUIRES_NEW`: isolate independent retry path.
- Isolation tuning appears only after correctness and lock trade-offs are mapped.

## Common mistake

- Assuming repository write failure happens at method return.
- Assuming read-only methods can safely call writing repository operations.
- Ignoring optimistic lock exceptions and treating them as fatal framework issues.

## Quick check

1. What does transaction boundary ownership belong to in clean architecture?
2. Why can `flush` be useful before a slow external call?
3. Which propagation mode helps nested retry jobs?

## Debugging exercise

Service A calls Service B with propagation defaults and performs a remote payment call.

Payment succeeds, then DB check fails on B.

Explain the rollback behavior and interview-safe fix.

Expected: separate the boundaries intentionally, define compensation strategy where business requires, or move side-effect outside the transaction with a durable intent.

## Practice

- **Foundation:** Draw the service-repository-transaction boundary for one command endpoint.
- **Interview Core:** Describe `REQUIRES_NEW` in one sentence and one practical use case.
- **SDE-2 Follow-up:** Explain lock escalation and deadlock behavior at a boundary layer.
