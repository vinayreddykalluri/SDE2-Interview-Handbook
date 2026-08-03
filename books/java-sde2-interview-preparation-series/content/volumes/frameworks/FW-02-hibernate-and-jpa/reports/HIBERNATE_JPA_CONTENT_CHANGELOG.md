# Hibernate/JPA Content Changelog — Backend Wave 2

| Chapter | Original weakness | Change made |
|---|---|---|
| 00 | ORM topics jumped ahead of SQL | added SQL/JDBC bridge, layer contracts, SQL prediction card, failure timeline |
| 01 | JPA/Hibernate distinction too brief | added portable/provider boundaries, Java SE bootstrap, resource scope, schema ownership |
| 02 | mapping listed only | added identity strategies, access, embeddables, enums/converters, inheritance, equality and schema alignment |
| 03 | lifecycle listed only | added states, identity map, dirty checking, flush modes, merge return semantics, clear/refresh failures |
| 04 | associations listed only | added owner/inverse code, two-sided helpers, cascade/orphan distinctions, aggregate and many-to-many guidance |
| 05 | fetching listed only | added proxy lifecycle, N+1 SQL trace, fetch joins/graphs/batches, row multiplication, paging repair |
| 06 | query APIs listed only | added JPQL/Criteria/native decision, projections, keyset/count, bulk-DML context behavior |
| 07 | advanced items listed only | added batch proof limitations, optimistic SQL, pessimistic locks, caches, equality, bulk decision table |
| 08 | absent | added layered test strategy, Hibernate statistics, SQL budgets, incident diagnostic sequence |
| 09 | absent | added 7 live interviews, 20 rapid answered questions, cumulative assessment and rubric |

## Executable additions

- `code/HibernateJpaInterviewCompanion.java`: lifecycle, identity map, dirty snapshots, association synchronization, optimistic versioning, batch chunking.
- `labs/maven-demo`: real Hibernate ORM/Jakarta Persistence fixture with H2 and seven behavior tests.
- `labs/validate_hibernate_jpa_labs.sh`: Java 21 strict compile/smoke and Maven tests.

## Accuracy corrections and safeguards

- `merge` copies into and returns a managed instance; the argument remains detached.
- Flush executes/synchronizes SQL but is not commit.
- Lazy is a loading plan mechanism, not a guarantee of no SQL or a complete N+1 solution.
- `mappedBy` and foreign-key ownership are separated from in-memory bidirectional consistency.
- ORM cascade and database referential cascade are different mechanisms.
- Bulk DML bypasses ordinary managed-state synchronization and per-entity behavior.
- Hibernate statistics do not by themselves prove wire-level JDBC batching.
- Pessimistic lock SQL/scope and target-engine behavior are not claimed portable.
