# Spring Data Content Changelog — Wave 1

| Area | Original weakness | Change made |
|---|---|---|
| Learning path | Repository abstraction appeared before native work was visible | Added JDBC/SQL, `EntityManager`, template, repository progression and full runtime flow |
| Repository APIs | Commons hierarchy and exact contracts absent | Added `Repository`, `CrudRepository`, `ListCrudRepository`, `PagingAndSortingRepository`, and `JpaRepository` decision table |
| Missing results | `getBy` incorrectly promised an exception | Explained wrapper/nullability/cardinality rules and JPA reference behavior |
| Existence | `countBy` recommended for existence/health | Separated existence, count, and readiness contracts with SQL consequences |
| Derived queries | Parser/grouping and SQL were vague | Added startup parsing, `Slice`, deterministic tie-breaker, SQL shape, and `And`/`Or` grouping correction |
| Declared/native queries | No lower-level comparison | Added one operation as SQL, `EntityManager`, and repository query plus projection/bulk-DML edges |
| Pagination | Offset and cursor semantics were mixed | Added `Page`, `Slice`, `Window`, stream table; separate models; signed/scoped cursor and failure matrix |
| Transactions | `REQUIRES_NEW`, flush, and retry were conflated | Added physical transaction flow, fresh-transaction retry, remote-call boundary, and recovery matrix |
| Fetch plans | One-query language was too simple | Added row multiplication, pageable collection risk, two-step reads, and evidence-based decision table |
| Locking | Vendor behavior was generalized | Added `@Version`, guarded SQL, `@Lock`, vendor boundary, and lock failure matrix |
| Specifications | Purely conceptual | Added `JpaSpecificationExecutor` style example, tenant predicate, input/sort/IN-list edges |
| Auditing | Audit metadata and compliance log were conflated | Distinguished Spring Data auditing from immutable audit evidence and expanded soft-delete decisions |
| MongoDB | Transaction capability was misstated | Added single-document atomicity, supported multi-document transactions, native update/version example, and failure labels |
| Redis | Cache flow was oversimplified | Added source version, stale-fill race, native atomic operation rule, outage/stampede/fencing matrix |
| Testing | No executable framework proof | Added Spring Boot 4.1/Spring Data JPA/Hibernate/H2 fixture with seven behavior tests |
| Interview preparation | Five unsolved prompts | Added twenty full model answers covering the highest-frequency Spring Data interview decisions |
| Companion | Comparator, pagination, retry, and store-choice defects | Corrected all four models and their executable assertions |
