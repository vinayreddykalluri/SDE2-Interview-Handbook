# Testing, SQL Traces, and Production Diagnostics

ORM tests must prove database behavior, not only Java method calls.

## Test layers

1. **Domain unit tests:** invariants without JPA.
2. **Mapping/behavior tests:** real provider plus a disposable database; lifecycle, SQL count, cascade, locking.
3. **Target-engine integration tests:** exact MySQL version for collation, generated DDL, plans, isolation, locks, and dialect features.
4. **Service tests:** transaction boundaries, exception translation, outbox/idempotency.
5. **Production telemetry:** query digests, traces, pool waits, locks, database metrics.

H2 is fast and portable for JPA mechanics, but compatibility mode does not turn it into MySQL. A passing H2 test cannot certify InnoDB next-key locks, MySQL collation, optimizer plans, or online DDL.

## Assert outcomes and SQL budgets

For an order-detail use case, assert:

- returned state;
- transaction commit/rollback;
- constraint and version conflicts;
- bounded statement count;
- no SQL after leaving the service transaction;
- generated query shape where regression risk is high.

Avoid brittle snapshots of every generated alias. Count statements and assert important joins/predicates through datasource instrumentation or Hibernate statistics; use exact SQL snapshots selectively.

## Statistics

Hibernate statistics can expose entity loads, fetches, query executions, prepared statements, cache behavior, and flushes. Enabling them has overhead; use deliberately in tests and controlled diagnostics.

```java
SessionFactory sessionFactory = entityManagerFactory.unwrap(SessionFactory.class);
Statistics statistics = sessionFactory.getStatistics();
statistics.clear();
// execute use case
long statements = statistics.getPrepareStatementCount();
```

## A diagnostic sequence

When an endpoint regresses:

1. separate pool wait, transaction time, query time, and response mapping;
2. correlate trace span to normalized SQL/query count;
3. identify unexpected flushes or lazy loads;
4. inspect actual database plan and lock waits;
5. compare bind/tenant distribution;
6. reproduce with production-like cardinality;
7. fix query/fetch/index/transaction scope and add a regression test.

## Symptom-to-cause map

| Symptom | Likely ORM cause | Evidence |
|---|---|---|
| endpoint query count grows with rows | N+1 | statement count and stack trace per query |
| exception occurs on unrelated select | AUTO flush exposed deferred failure | transaction log and flush event |
| update missing after bulk DML | stale managed object overwrote/ignored DB state | clear/refresh and SQL timeline |
| memory grows in import | persistence context retains entities | managed count/heap and absent clear |
| lock held during HTTP call | oversized transaction boundary | transaction trace and DB lock duration |
| page duplicates/misses | unstable sort or collection join pagination | SQL rows and ordering key |

## Test isolation

Rollback-after-each-test is convenient but can hide commit-time behavior. Add tests that truly commit for constraints, optimistic locks, outbox state, and callbacks. Generate unique keys, avoid order-dependent fixtures, and reset caches/statistics between tests.

## Security and logging

SQL logs can expose tokens, emails, and personal data. Use redaction and controlled sampling. Do not enable verbose binds globally during an incident without scope, retention, and access controls.

## Practice

- **Foundation:** Write a test proving dirty checking updates at commit.
- **Interview Core:** Add a query-count test that fails when N+1 returns.
- **Interview Core:** Test stale optimistic updates with two persistence contexts.
- **SDE-2 Follow-up:** Build a target-MySQL test for a lock-range claim while keeping H2 tests fast.
