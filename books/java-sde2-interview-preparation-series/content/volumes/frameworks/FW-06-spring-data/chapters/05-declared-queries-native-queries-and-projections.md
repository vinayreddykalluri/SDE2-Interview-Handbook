# Declared Queries, Native SQL, and Projections

Declared queries keep behavior readable while enabling fine control.

## Three query channels

1. **Derived methods:** generated from method names.
2. **Declared queries (`@Query`):** explicit, readable, often portable.
3. **Native queries:** full control, vendor-specific power, stronger responsibility.

A fourth channel is a **custom repository fragment** implemented with `EntityManager`, `JdbcTemplate`, or another store template. It is often clearer than forcing a complex use case through annotations.

## One query at three levels

Start with the result and database work:

```sql
select id, status, created_at
from customer_order
where tenant_id = ? and status = ?
order by created_at desc, id desc
limit ?;
```

An explicit JPA adapter keeps the query visible:

```java
List<OrderSummary> findLatest(long tenantId, String status, int limit) {
    return entityManager.createQuery("""
            select new example.OrderSummary(o.id, o.status, o.createdAt)
            from OrderEntity o
            where o.tenantId = :tenantId and o.status = :status
            order by o.createdAt desc, o.id desc
            """, OrderSummary.class)
        .setParameter("tenantId", tenantId)
        .setParameter("status", status)
        .setMaxResults(limit)
        .getResultList();
}
```

The repository form removes adapter plumbing:

```java
@Query("""
       select new example.OrderSummary(o.id, o.status, o.createdAt)
       from OrderEntity o
       where o.tenantId = :tenantId and o.status = :status
       order by o.createdAt desc, o.id desc
       """)
Slice<OrderSummary> findLatest(
        long tenantId, String status, Pageable page);
```

These are not automatically identical. A `Page` may add a count query; a `Slice` commonly requests enough rows to determine continuation. Parameter binding, SQL generation, null handling, and limit syntax depend on provider and dialect.

## Choosing the right channel

Use declared/native query when:

- you need explicit joins,
- you need aggregation or windowed sorting,
- return shape needs fields not present in entity graph,
- optimizer behavior must be controlled.

Use derived when:

- query stays inside method-level intent,
- team clarity is more important than SQL novelty,
- execution remains bounded.

## Projection pattern

Projection reduces payload and accidental update risk.

```text
Entity table (wide)
    |
    +---> projection interface/class
         (selected columns only)
```

If projection includes computed fields, confirm if they are DB-native or application computed.

Projection choices include interface projections, DTO/record constructor projections, and dynamic projections. Closed projections can select a narrow shape; open expression-based projections can require more source state and obscure work. Never serialize a managed entity merely because it already has getters.

## Common trap

- Native query changes with each DB version.
- Declared query may still load extra columns unless projection is correct.
- Projections can hide expensive joins behind cheap-looking method names.
- A native paged query may need an explicit correct `countQuery`; a guessed count can disagree with grouping or joins.
- Bulk JPQL/native mutation bypasses normal managed-entity dirty checking and can leave the persistence context stale until it is cleared or refreshed.

## Edge and failure matrix

| Choice | Failure mode | Evidence |
|---|---|---|
| DTO projection | constructor/type mismatch | context or query integration test |
| interface projection | getter alias mismatch | representative result mapping |
| native query | dialect/schema drift | target-engine test and migration compatibility |
| paged join | inflated count or duplicate roots | result/count assertions with one-to-many data |
| bulk update | managed objects retain old state | clear/refresh test in same transaction |
| dynamic sort | unsafe property or unsupported expression | allow-list and repository test |

## Quick check

1. Why does a projection not automatically mean performance is good?
2. Which layer owns SQL portability checks for native query?
3. When should you avoid native query in interviews?

## Debugging exercise

Two queries fetch the same fields, one derived and one declared.

Latency jumps 4x in production.

Explain the 4 checks you run first.

Expected checks:

- compare execution plans,
- verify indexes,
- validate sort + filter order,
- confirm projection columns and cardinality.

## Practice

- **Foundation:** Write one declared query equivalent to a derived method.
- **Interview Core:** Name why native queries should include test data with vendor versions.
- **SDE-2 Follow-up:** Explain projection risks with entity-to-DTO mapping.

## Interviewer question and model answer

**Interviewer:** When would you choose `@Query`, a `Specification`, `EntityManager`, or JDBC?

**Model answer:** I use `@Query` for a stable explicit JPQL query, a `Specification` when a bounded set of optional filters must compose, `EntityManager` for JPA behavior that does not fit a repository annotation cleanly, and JDBC for SQL-first reads, bulk work, window functions, or vendor features where ORM mapping adds little. I keep parameters bound, use a narrow projection, make ordering and limits explicit, and verify the generated or written SQL on the target database.
