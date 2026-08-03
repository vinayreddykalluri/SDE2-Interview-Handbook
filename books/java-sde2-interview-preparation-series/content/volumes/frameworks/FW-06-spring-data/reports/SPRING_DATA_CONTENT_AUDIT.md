# Spring Data Content Audit — Wave 1

## Scope

This audit covers the canonical SD06 Markdown, dependency-free Java companion, and executable JPA lab. It evaluates beginner progression, Java/Spring accuracy, runtime visibility, edge cases, interview answers, and proof through code.

## Before Wave 1

- 17 short chapters, approximately 5,097 words.
- One Java fence and no SQL fence across the chapters.
- No real Spring Data dependency, repository implementation, database fixture, or framework test.
- Five unsolved interview follow-up prompts despite a manifest promise of twenty solved scenarios.
- Spring Data Commons/JPA types such as `Repository`, `CrudRepository`, `JpaRepository`, `Page`, `Slice`, `Window`, `EntityManager`, `@Version`, and `@Lock` were missing or only implied.
- Several explanations were misleading, and the companion contained semantic defects.

## Critical findings and disposition

| Finding | Severity | Wave 1 disposition |
|---|---|---|
| `getBy...` described as a universal missing-row exception | Critical | Corrected; return type, nullability, cardinality, and `getReferenceById` are distinguished |
| `countBy` described as cheap existence/health behavior | Critical | Corrected with existence-versus-count SQL intent and health boundary |
| `REQUIRES_NEW` described as a retry path | Critical | Corrected as an independent transaction with connection/atomicity costs |
| flush framed as useful before a slow remote call | Critical | Corrected; early constraint detection is separated from remote workflow design |
| lock escalation implied as portable behavior | Critical | Corrected with explicit engine/version/statement boundary |
| comparator reversed the entire composed order | Critical | Fixed and asserted as timestamp DESC, ID DESC |
| cursor model also advanced an offset | Critical | Replaced by separate `OffsetPage` and `CursorPage` types |
| optimistic retry accepted unclassified failures | Critical | Retry now permits only classified stale conflicts and has a hard attempt cap |
| MongoDB modeled as non-transactional | Critical | Corrected; supported multi-document transactions and modeling cost are both stated |
| store selection reduced to categorical booleans | High | Replaced with workload evidence, reason, and caveat |
| no real Spring Data validation | High | Added Spring Boot 4.1/Spring Data JPA/Hibernate/H2 Maven fixture with seven tests |
| interview answers were prompts rather than models | High | Added twenty complete interviewer questions and model answers |

## After Wave 1

- Approximately 11,338 chapter words.
- Native SQL/JPA/Mongo/Redis examples precede repository abstractions where relevant.
- Runtime/data-flow diagrams trace service, repository proxy, provider/template, driver, flush, and commit.
- Edge/failure matrices cover CRUD, query channels, paging, transactions, fetch plans, locks, dynamic queries, auditing, MongoDB, Redis, and testing.
- Twenty solved interviewer follow-ups plus chapter-level model answers.
- A real Spring Data JPA lab proves deterministic derived ordering, count-free `Slice`, cursor continuation, rollback, uniqueness at flush, optimistic conflict, pessimistic lock intent, and existence semantics.

## Remaining content boundaries

- H2 does not prove MySQL execution plans, collation, gap/next-key locks, deadlocks, isolation, or online migration behavior. The MySQL/Hibernate volumes need a target MySQL Testcontainers suite.
- MongoDB and Redis remain bounded introductions. Complete native-driver, cluster/failover, and Spring integration depth belongs in SD07 and SD08.
- The PDF must be rebuilt and visually inspected by the root publishing pass because this wave intentionally did not edit the manifest or build pipeline.

## Publication recommendation

The content is suitable for an **enhanced candidate build** after the root pass connects the companion/lab to mandatory validation and rebuilds the PDF. Do not claim final publication until that validation is enforced centrally and the PDF is inspected.
