# What Spring Data Is and When Not To Use It

Spring Data gives you repository patterns plus query abstraction. It does not replace relational design, transaction semantics, or indexing.

## Core idea

Think of Spring Data as a **convenience adapter**:

- A repository method is still backed by SQL, Mongo queries, or Redis operations.
- You must still choose transaction boundaries and consistency trade-offs.
- You must still read execution behavior under load.

Spring Data Commons supplies shared repository concepts. A store module such as Spring Data JPA, MongoDB, or Redis implements only the contracts that make sense for that store. Similar Java method names do **not** make SQL rows, MongoDB documents, and Redis values share transaction, query, or durability semantics.

## See the abstraction being added

Without a repository, a focused JPA adapter is ordinary Java:

```java
final class JpaOrderLookup {
    private final EntityManager entityManager;

    JpaOrderLookup(EntityManager entityManager) {
        this.entityManager = entityManager;
    }

    Optional<OrderEntity> find(long id) {
        return Optional.ofNullable(entityManager.find(OrderEntity.class, id));
    }
}
```

Spring Data can generate the common adapter:

```java
interface OrderRepository extends Repository<OrderEntity, Long> {
    Optional<OrderEntity> findById(Long id);
}
```

Both eventually use the persistence provider and driver. The repository saves boilerplate; it does not change entity lifecycle, SQL isolation, indexes, or commit semantics.

## Decision first

Use a repository when:

- Access is CRUD-heavy and bounded.
- Domain operations are simple and stable.
- Query language can stay close to model language.

Use a lower-level `JdbcTemplate`, `EntityManager`, Mongo `MongoTemplate`, or Redis API when:

- You need non-standard joins, windowed updates, bulk writes, or vendor-specific operators.
- Performance reasoning is dominated by generated query shape.
- You need deterministic lock and retry behavior for each step.

Also choose a custom repository fragment when most queries are simple but one use case needs lower-level control. The decision is per operation, not one framework choice for the entire application.

## Failure and edge matrix

| Situation | Hidden risk | Safer decision |
|---|---|---|
| method returns thousands of entities | heap growth and long persistence context | projection plus a bounded `Slice`, cursor, or stream lifecycle |
| query name contains several `Or`/`And` parts | parser precedence hides business grouping | declared query or `Specification` with tests |
| write calls a remote provider inside transaction | locks and connections remain held; outcome can be unknown | short local transaction plus durable intent/outbox |
| ORM-generated SQL is the bottleneck | Java method hides joins and row multiplication | inspect SQL/plan, then projection, custom query, or JDBC |
| multiple stores are updated | no repository creates cross-store atomicity | explicit workflow, idempotency, reconciliation, or saga |

## Interview-sized model

```text
Method contract + module style + domain semantics
                 |
                 v
            Repository method
                 |
           store query + plan
                 |
       row/document/cache side effect
```

## Common myth corrected

- **Myth:** "Repository methods are enough for all query quality."

  **Reality:** Repository methods are only a mapping point. Cost, locks, and visibility still come from the store.

- **Myth:** "If repository returns a `List`, all rows are deterministic."

  **Reality:** Sorting and tie-breakers are your responsibility unless explicitly specified.

## Debugging exercise

1. Repository method `findByStatus(String)` runs fast for one developer dataset.
2. In production, API latency spikes with status cardinality change.
3. What questions do you ask before adding more derived methods?

Expected investigation:

- query shape and index usage,
- sort/order determinism,
- pagination or cursor strategy,
- transaction and cache interactions.

## Quick check

1. What does repository abstraction remove from your code?
2. What does it not remove?
3. Why is ordering an explicit API concern?

## Practice

- **Foundation:** Choose repository vs template for one simple read API.
- **Interview Core:** Explain an example where derived methods would be harmful.
- **SDE-2 Follow-up:** State 3 reasons to keep a custom query for read-heavy endpoints.

## Interviewer question and model answer

**Interviewer:** If Spring Data removes repository implementations, why would an SDE-2 engineer ever use `EntityManager` or `JdbcTemplate`?

**Model answer:** Spring Data removes recurring adapter code, not the need to control a query. I keep repositories for bounded CRUD and clear predicates. I use a custom fragment, `EntityManager`, or JDBC when the use case needs vendor SQL, bulk mutation, window functions, deterministic locking, or a projection whose cost must stay visible. I choose per operation, keep the transaction in the application service, and prove the choice with generated SQL, the target database plan, and integration tests.
