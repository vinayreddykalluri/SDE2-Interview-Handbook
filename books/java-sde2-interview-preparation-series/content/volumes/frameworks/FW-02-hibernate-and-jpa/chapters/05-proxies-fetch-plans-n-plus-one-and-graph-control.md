# Proxies, Fetch Plans, N+1, and Graph Control

Fetching is a use-case decision: which state must this transaction load, in how many statements, with what row multiplication and memory cost?

## Lazy does not mean “never loaded”

A lazy association is represented by a proxy or persistent collection wrapper and initialized when accessed while a usable persistence context exists.

```text
load order -> proxy customer / uninitialized lines
access order.getLines().size()
     -> SQL if context is open
     -> LazyInitializationException if detached and not initialized
```

Do not repair a detached lazy failure by making everything eager or keeping a persistence context open through web rendering. Define a transactional query that returns the data the application use case needs.

## The N+1 shape

```java
List<PurchaseOrder> orders = entityManager
        .createQuery("select o from PurchaseOrder o", PurchaseOrder.class)
        .getResultList();                         // 1 query

for (PurchaseOrder order : orders) {
    System.out.println(order.getLines().size()); // up to N queries
}
```

Trace:

```sql
select ... from purchase_order;                 -- 1
select ... from order_line where order_id=?;    -- repeated per order
```

The code has no explicit query in the loop, which is exactly why query-count tests and SQL traces matter.

## Fetch join

```jpql
select distinct o
from PurchaseOrder o
left join fetch o.lines
where o.id = :id
```

This is effective for one aggregate or a bounded set. Collection fetch joins multiply result rows. Fetching two bag-like collections can create a Cartesian explosion or provider limitation. `distinct` can deduplicate root entities in Java/SQL semantics; it does not make transferred child combinations free.

## Entity graphs

An entity graph describes attributes to fetch without embedding fetch joins into every JPQL statement. It improves use-case-level plans, but still inspect generated SQL and provider semantics. A graph is not a guarantee of one query in every provider/mapping combination.

## Batch and subselect fetching

Hibernate batch fetching can initialize several lazy proxies/collections with `IN (...)` queries, reducing N+1 without joining a wide Cartesian result. Subselect fetching can load related collections for roots from a prior query. These are provider-specific tuning options: measure query count, parameter limits, row count, and memory.

## Pagination and collection fetch joins

Applying offset/limit to a collection fetch join is dangerous: SQL rows represent root-child pairs, not distinct roots. Providers may paginate in memory or reject/warn, producing incorrect performance assumptions.

Use two steps:

1. page only root IDs with deterministic order;
2. fetch roots and required children for those IDs;
3. restore requested order if the second query does not preserve it.

Or use projections when the response does not require entities.

## DTO boundary

Serializing entities directly can trigger lazy SQL, recursion, expose internal columns, and accept unsafe detached updates. Map to a response DTO inside the transaction with an explicit fetch plan or query projection.

## Fetch decision table

| Use case | Technique | Risk to test |
|---|---|---|
| one aggregate detail | fetch join/entity graph | row multiplication |
| page of summaries | DTO projection | constructor/alias drift |
| page roots then children | two-step ID page + fetch | ordering and duplicate IDs |
| many roots, occasional child access | lazy + batch fetching | query count/parameter size |
| detached API response | assemble DTO in transaction | accidentally accessed field |

## Interview drill

**Question:** “Why not set every association to lazy?”

**Answer:** Lazy is a safe default for avoiding unconditional work, not a complete fetch plan. Each use case still needs explicit loading. Otherwise access patterns create N+1 or detached failures.

## Practice

- **Foundation:** Predict query count for ten orders whose lines are accessed.
- **Interview Core:** Repair it with both a fetch join and projection; compare trade-offs.
- **SDE-2 Follow-up:** Page customers with two collections without Cartesian multiplication.
