# Testing Data Paths, Observability, and Regression Harness

Spring Data quality is proven through test selection, query diagnostics, and failure evidence.

## Test layers

1. **Unit-level contract tests:** method behavior and parameter validation.
2. **Repository behavior tests:** query generation and result mapping rules.
3. **Integration tests with real store:** transaction, index, ordering, and concurrency behavior.
4. **Failure-path tests:** duplicates, stale versions, timeout and rollback.

## Query observability model

```text
Repository call -> SQL / store statement log -> row count/latency -> result mapping -> business assertion
```

Interviews value evidence: capture query count and duration for critical operations.

## Deterministic fixtures

Fixtures should be:

- ordered,
- repeatable,
- minimal,
- scoped to one behavior.

Avoid random fixtures that make flaky transaction tests pass/fail.

## Quick check

1. Which test layer catches N+1 first?
2. Why do in-memory databases hide query shape?
3. What evidence proves a lock path is actually exercised?

## Debugging exercise

Repository integration tests pass locally but fail against target store.

List investigation steps.

Expected: check driver/version compatibility, timezone/ordering, transaction isolation defaults, and index availability.

## Practice

- **Foundation:** Write a repository test for stable ordering and page shape.
- **Interview Core:** Add assertions for no-over-fetch and bounded response size.
- **SDE-2 Follow-up:** Create a failure test for optimistic lock exception recovery.
