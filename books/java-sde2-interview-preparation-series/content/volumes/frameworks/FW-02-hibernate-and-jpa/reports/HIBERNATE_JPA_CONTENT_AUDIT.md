# Hibernate/JPA Content Audit — Backend Wave 2

## Before improvement

The canonical volume contained one 241-word roadmap preview. It mentioned entity lifecycle, mappings, fetching, locking, and batching, but taught none of them. There were no entities, SQL traces, broken mappings, runnable provider tests, interview answers, or clear boundary between JPA, Hibernate, JDBC, and MySQL.

## Final chapter inventory

| # | Chapter | Learning role |
|---:|---|---|
| 0 | SQL-before-ORM learning path | makes database work visible before annotations |
| 1 | JPA contract, Hibernate provider, bootstrap | establishes API/provider/resource boundaries |
| 2 | Mapping, identity, values, schema boundaries | IDs, access, values, enums, converters, inheritance, equality |
| 3 | Lifecycle, persistence context, dirty checking, flush | core unit-of-work mechanics and failure timing |
| 4 | Associations, ownership, cascades, orphans | foreign-key control and aggregate lifecycle |
| 5 | Proxies, fetch plans, N+1, graph control | loading mechanics and query-count discipline |
| 6 | JPQL, Criteria, native, projections, pagination | query choices and bulk-DML staleness |
| 7 | Batching, locks, caches, equality, bulk work | concurrency/performance internals and trade-offs |
| 8 | Testing, SQL traces, production diagnosis | evidence and regression strategy |
| 9 | Live interviews, rapid Q&A, assessment, sources | synthesis and SDE-2 readiness |

## Quality matrix

| Topic | Previous state | Final state | Evidence |
|---|---|---|---|
| SQL/JDBC foundation | implied prerequisite | strong | explicit SQL translation and prediction card |
| JPA vs Hibernate | one sentence | strong | contract/provider capability table |
| identity/mapping | named | strong | access, IDs, values, enums, inheritance, equality traps |
| lifecycle/context | named | strong | state diagram, identity map, merge/clear/refresh cases |
| dirty checking/flush | named | strong | SQL trace, AUTO query failure timeline, flush vs commit |
| associations | named | strong | owning side, helper methods, cascade vs DB action, orphans |
| fetch/N+1 | named | strong | proxy timeline, query count, fetch alternatives, paging failure |
| queries | named | strong | JPQL, Criteria, native, projections, cursor/count/bulk behavior |
| locks/batches/caches | named | strong | SQL version check, lock boundary, batch proof limits, cache policy |
| testing/diagnosis | missing | strong | test pyramid, statistics, symptom/evidence matrix |
| interviews | missing | strong | 7 live dialogues, 20 answered rapid questions, assessment |
| executable proof | missing | strong | companion plus 7 real Hibernate/JPA tests |

## Critical findings resolved

- The reader can now distinguish JPA specification guarantees from Hibernate/provider/database behavior.
- `merge`, `flush`, lazy loading, owning side, bulk DML, and optimistic locking are explained with their real failure timing.
- H2 is explicitly limited to portable ORM mechanics; it is not used to make MySQL claims.
- Entity equality avoids mutable association/hash and proxy traps.
- ORM convenience is never presented as a replacement for transactions, constraints, migrations, or idempotency.

## Remaining publication work outside this wave

- Root integration must update the manifest, regenerate PDF/web, and visually inspect code/tables/diagrams.
- A later target-MySQL suite should validate dialect SQL, plans, collations, pessimistic lock scope, deadlock/timeout behavior, and actual driver batching.
- Provider-specific examples should be rechecked when upgrading Hibernate because generated SQL and tuning APIs can evolve.

## Source baseline

Portable behavior was checked against the [Jakarta Persistence 3.2 specification](https://jakarta.ee/specifications/persistence/3.2/jakarta-persistence-spec-3.2) and provider behavior against the [Hibernate ORM User Guide](https://docs.jboss.org/hibernate/orm/current/userguide/html_single/Hibernate_User_Guide.html). The fixture observed Hibernate ORM 7.4.1.Final.
