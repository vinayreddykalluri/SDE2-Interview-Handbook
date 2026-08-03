# Hibernate/JPA Live Interviews, Answered Questions, and Readiness

## Live interview 1: N+1 in production

**Interviewer:** “A list endpoint went from 20 to 2,001 SQL statements. Fix it.”

**Candidate:** “I would confirm query count and which getter initializes the association. For summary rows I prefer a DTO projection. If a bounded aggregate is needed, use a fetch join/entity graph; if paging roots with collections, page root IDs then fetch children. I would compare rows transferred, not merely query count, and add a statement-budget regression test.”

## Live interview 2: `save()` and transaction truth

**Interviewer:** “The code called save, then a remote API, then commit failed. What happened?”

**Candidate:** “For a managed/new entity, save/persist may only schedule state. SQL or a deferred constraint can fail at flush/commit after the external call. A database transaction cannot roll back that remote effect. I would persist state plus an outbox atomically, commit, then perform the remote action idempotently.”

## Live interview 3: detached update

**Interviewer:** “The controller posts an entity JSON and we call `merge`. Any issue?”

**Candidate:** “It permits over-posting and copies detached graph state, potentially overwriting fields the client never owned and concurrent changes. I would accept a command DTO, load the aggregate in a transaction, authorize and validate explicit mutations, and rely on `@Version` for conflict detection.”

## Live interview 4: optimistic versus pessimistic

**Interviewer:** “Which lock for the final concert ticket?”

**Candidate:** “I first ask collision rate, invariant, transaction duration, and failure cost. A single conditional SQL update can be best. Optimistic versioning suits low contention and replayable commands; pessimistic locking avoids late conflict but holds scarce resources and can deadlock. I would load test the chosen strategy and keep external calls outside the lock.”

## Live interview 5: collection fetch plus paging

**Interviewer:** “Why does `join fetch o.lines` break our page?”

**Candidate:** “The SQL page is over root-child rows, so one root consumes several rows and the provider may paginate in memory. I would page ordered root IDs, then fetch roots and lines in a second query, or return a projection. The count query is separate and must not inherit the collection join.”

## Live interview 6: bulk update bug

**Interviewer:** “A JPQL bulk update changed rows, but the response returned old values.”

**Candidate:** “Bulk DML bypasses the managed entity state. Flush relevant pending work before it, execute the bulk statement, then clear or refresh. I would also define version/cache invalidation behavior and keep the bulk operation in its own explicit boundary.”

## Live interview 7: batch import OOM

**Interviewer:** “Hibernate import crashes after 300,000 rows.”

**Candidate:** “The persistence context likely retains every managed entity, and statements may not batch due to identity generation or mixed shapes. I would use bounded transactions/chunks, flush and clear each chunk, configure and instrument JDBC batching, and consider set-based JDBC/native loading. I would make the job restartable and idempotent.”

## Rapid answered questions

1. **JPA versus Hibernate?** JPA is the portable contract; Hibernate is a provider and extension set.
2. **Is `EntityManager` thread-safe?** No; scope it to a unit of work/request transaction.
3. **Does `persist` always insert immediately?** No; SQL timing depends on flush and ID strategy.
4. **Does `flush` commit?** No; it synchronizes SQL inside the current transaction.
5. **What does `merge` return?** A managed instance containing copied state; the argument remains detached.
6. **What is the first-level cache?** The persistence context’s identity map, not a cross-request cache.
7. **Owning side?** The side whose mapping controls the foreign key/join table update.
8. **Cascade versus orphan removal?** Cascade propagates entity operations; orphan removal deletes a privately owned child removed from its association.
9. **Cascade versus `ON DELETE CASCADE`?** ORM operation propagation versus database referential action.
10. **Lazy loading failure outside transaction?** The proxy/collection lacks an open usable context; fetch a DTO/graph inside the service boundary.
11. **Why not eager everywhere?** It causes unconditional joins/secondary selects and graph explosion.
12. **What is N+1?** One root query followed by a query per result/association access.
13. **Can a fetch join always fix N+1?** No; multiple collections and paging can multiply rows or break pagination.
14. **JPQL names what?** Entity types and attributes, not physical table/column names.
15. **What does `@Version` do?** Adds version verification/update so stale writes affect zero rows and fail optimistically.
16. **Does bulk JPQL honor each entity callback/version?** It bypasses ordinary per-entity lifecycle/tracked state; manage version and context explicitly.
17. **Second-level cache by default?** Provider/configuration dependent and optional; first-level context is fundamental.
18. **Why can equality load SQL?** Generated equality may traverse lazy associations/proxies.
19. **Why can a query trigger an insert error?** `AUTO` flush synchronizes pending inserts before a relevant query.
20. **Can H2 prove MySQL locking behavior?** No; use the exact engine/version for dialect, plan, isolation, and locks.

## Cumulative assessment

Implement and explain an order aggregate with lines:

1. schema and expected SQL first;
2. IDs, value mappings, version, and constraints;
3. ownership, cascade, orphan behavior, and helper methods;
4. detail and summary fetch plans with query-count budgets;
5. optimistic concurrency conflict test;
6. bulk expiry job with clear/cache policy;
7. batch import with bounded context;
8. target-MySQL tests for locks and plans;
9. incident runbook for N+1, pool wait, and slow flush.

## Readiness rubric

| Level | Evidence |
|---|---|
| Foundation | maps entities and explains lifecycle/transactions |
| Interview Core | predicts SQL, owns associations, controls fetch and versions |
| SDE-2 ready | reasons across ORM, JDBC, database, failure, and observability |

## Selected authoritative references

- Jakarta Persistence 3.2 specification: entity lifecycle, persistence context, flush, query, locking, and cache contracts.
- Hibernate ORM User Guide (current release): provider-specific mappings, fetching, batching, caches, statistics, and SQL behavior.
- MySQL 8.4 Reference Manual and Connector/J guide: database isolation, plans, dialect behavior, and JDBC driver settings.

Version-label provider-specific behavior and verify it against the runtime in the lab or target service. The accompanying H2 fixture validates portable ORM mechanics, not MySQL-specific plans, collations, or locks.
