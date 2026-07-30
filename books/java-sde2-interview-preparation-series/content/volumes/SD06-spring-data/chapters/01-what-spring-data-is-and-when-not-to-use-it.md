# What Spring Data Is and When Not To Use It

Spring Data gives you repository patterns plus query abstraction. It does not replace relational design, transaction semantics, or indexing.

## Core idea

Think of Spring Data as a **convenience adapter**:

- A repository method is still backed by SQL, Mongo queries, or Redis operations.
- You must still choose transaction boundaries and consistency trade-offs.
- You must still read execution behavior under load.

## Decision first

Use a repository when:

- Access is CRUD-heavy and bounded.
- Domain operations are simple and stable.
- Query language can stay close to model language.

Use a lower-level `JdbcTemplate`, `EntityManager`, Mongo `MongoTemplate`, or Redis API when:

- You need non-standard joins, windowed updates, bulk writes, or vendor-specific operators.
- Performance reasoning is dominated by generated query shape.
- You need deterministic lock and retry behavior for each step.

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
