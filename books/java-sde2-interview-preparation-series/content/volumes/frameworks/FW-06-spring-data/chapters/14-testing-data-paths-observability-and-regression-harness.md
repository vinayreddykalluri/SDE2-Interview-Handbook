# Testing Data Paths, Observability, and Regression Harness

Spring Data quality is proven through test selection, query diagnostics, and failure evidence.

## Test layers

1. **Unit-level contract tests:** method behavior and parameter validation.
2. **Repository behavior tests:** query generation and result mapping rules.
3. **Integration tests with real store:** transaction, index, ordering, and concurrency behavior.
4. **Failure-path tests:** duplicates, stale versions, timeout and rollback.

The SD06 executable lab demonstrates these layers with Spring Boot, Spring Data JPA, Hibernate, and H2. H2 proves repository wiring and generic JPA contracts; it does **not** prove MySQL plans, gap locks, collation, or deadlock behavior. Those claims require the target engine, preferably through Testcontainers in the MySQL/Hibernate volumes.

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

## Claims and the test that can prove them

| Claim | Minimum useful evidence |
|---|---|
| derived query order is deterministic | equal primary sort keys plus asserted ID tie-breaker |
| `Slice` continuation works | `size + 1` data, first/next content, `hasNext` |
| rollback protects state | invoke real service proxy and read in a fresh transaction |
| optimistic conflict is detected | two snapshots/transactions and stale version write |
| pessimistic mode is requested | active transaction and managed entity lock-mode assertion; target DB test for blocking |
| N+1 is absent | SQL statement count and bounded relationship data |
| query is fast | target-engine plan plus representative distribution/load |

## Observability without leaking data

Capture normalized operation name, duration, rows, query count, pool wait, lock wait, conflict/retry count, and result size. Do not tag metrics with raw SQL, customer ID, request key, or arbitrary repository method parameters. Logs may carry controlled correlation and a sanitized query fingerprint.

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

## Interviewer question and model answer

**Interviewer:** Why is `@DataJpaTest` with H2 not enough?

**Model answer:** It is excellent for repository wiring, JPQL parsing, mapping, and many lifecycle contracts. It cannot prove the target database's dialect, collation, indexes, optimizer plan, isolation anomalies, lock ranges, deadlocks, or online migration behavior. I keep fast H2 tests for feedback and add a smaller set of Testcontainers tests against the production engine for every claim that depends on that engine.
