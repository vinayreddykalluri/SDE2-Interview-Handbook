# JPQL, Criteria, Native SQL, Projections, and Pagination

## JPQL queries the entity model

```java
List<OrderSummary> summaries = entityManager.createQuery("""
    select new interview.OrderSummary(o.id, o.totalCents, o.createdAt)
    from PurchaseOrder o
    where o.customerId = :customerId
      and o.status = :status
    order by o.createdAt desc, o.id desc
    """, OrderSummary.class)
    .setParameter("customerId", customerId)
    .setParameter("status", OrderStatus.PAID)
    .setMaxResults(50)
    .getResultList();
```

JPQL names entities and attributes, not tables and columns. Constructor projections return unmanaged DTOs and avoid tracking a graph that will not be updated.

## Joins and implicit navigation

Explicit joins make cardinality and join type visible. Navigating `o.customer.email` can create an implicit inner join and eliminate rows where the relationship is absent. Use explicit `left join` when null-preserving behavior is required.

For collection membership and existence, express the domain question instead of fetching a collection and filtering in Java.

## Criteria API

Criteria is useful for dynamic, type-oriented query composition:

```java
CriteriaBuilder cb = entityManager.getCriteriaBuilder();
CriteriaQuery<PurchaseOrder> query = cb.createQuery(PurchaseOrder.class);
Root<PurchaseOrder> order = query.from(PurchaseOrder.class);

List<Predicate> predicates = new ArrayList<>();
if (status != null) {
    predicates.add(cb.equal(order.get("status"), status));
}
query.where(predicates.toArray(Predicate[]::new));
```

It can become verbose and stringly typed without a metamodel. Keep query composition isolated and test its generated SQL/results. Do not choose Criteria merely because it looks more “enterprise.”

## Native SQL

Use native SQL for database-specific features, window/CTE/reporting shapes not expressed cleanly, optimizer hints only when evidence demands them, or carefully tuned hot paths.

Trade-offs include dialect coupling, manual result mapping, persistence-context staleness after writes, and migration coordination. Parameterize values; native does not mean concatenate.

## Projections

- **Entity:** managed identity and changes; load only when you need domain behavior.
- **Scalar/tuple:** compact but alias/index handling can be fragile.
- **DTO constructor:** explicit read contract and no dirty checking.
- **Interface/record via framework mapping:** convenient but implementation rules vary.

Selecting an entity plus one scalar still returns/maintains the entity; it is not automatically a lightweight projection.

## Pagination

JPA offset pagination uses `setFirstResult` and `setMaxResults`; it inherits deep-offset cost and concurrent-shift behavior.

Keyset JPQL for descending `(createdAt, id)`:

```jpql
where o.createdAt < :time
   or (o.createdAt = :time and o.id < :id)
order by o.createdAt desc, o.id desc
```

Use a complete stable ordering key, fetch one extra row, and return an opaque cursor. Seek support from provider-specific APIs may reduce boilerplate, but the relational predicate remains the contract.

## Count-query trap

A page often requires a separate count. A fetch join, `distinct`, grouping, or complex predicate can make automatically derived counts slow or wrong. A `Slice`/cursor response avoids total count when the product needs only “has next.” If total count is required, write and test its query intentionally.

## Bulk DML bypasses managed state

```java
int changed = entityManager.createQuery("""
    update PurchaseOrder o
    set o.status = :expired
    where o.status = :created and o.createdAt < :cutoff
    """)
    .setParameter("expired", EXPIRED)
    .setParameter("created", CREATED)
    .setParameter("cutoff", cutoff)
    .executeUpdate();
```

Bulk JPQL does not synchronize each managed entity or invoke ordinary entity lifecycle/version handling in the same way. Flush relevant pending changes first, then clear/refresh the context, and decide how version/concurrency should be preserved. Directly adding `version = version + 1` may be required by policy, but affected stale instances still need reconciliation.

## Practice

- **Foundation:** Convert one SQL filter into JPQL and name the generated joins.
- **Interview Core:** Build a projection and keyset continuation query.
- **SDE-2 Follow-up:** Design a bulk expiry job that does not leave managed or cached state stale.
