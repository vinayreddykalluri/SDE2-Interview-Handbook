# Batching, Locking, Caches, Equality, and Bulk Work

## JDBC batching through Hibernate

Configure a bounded `hibernate.jdbc.batch_size`, order compatible inserts/updates when appropriate, and flush/clear in chunks:

```java
for (int index = 0; index < commands.size(); index++) {
    entityManager.persist(toEntity(commands.get(index)));
    if ((index + 1) % batchSize == 0) {
        entityManager.flush();
        entityManager.clear();
    }
}
```

Batching can be limited by identity ID generation, mixed statement shapes, early flushes, generated-key needs, or driver settings. Hibernate statistics showing insert count does not prove network batches; use datasource/driver tracing or an integration proxy when batch transport matters.

## Optimistic locking

```java
@Version
private long version;
```

Hibernate reads the version and generates a predicate like:

```sql
update purchase_order
set status=?, version=?
where order_id=? and version=?
```

Zero updated rows becomes an optimistic-lock failure and the transaction rolls back. Retry only if you can replay the command against fresh state without duplicating external effects. Often returning conflict to the caller is more honest than silently overwriting.

Every concurrently changed entity that needs protection should be versioned. Versioning only the root does not automatically detect direct changes to every child unless the mapping/update changes the root version as intended.

## Pessimistic locking

```java
PurchaseOrder order = entityManager.find(
        PurchaseOrder.class, id, LockModeType.PESSIMISTIC_WRITE);
```

JPA requires a transaction for pessimistic lock modes. The provider translates to database locks, but exact SQL, scope, timeout, and range behavior are database-specific. Keep transactions short, lock in stable order, and handle deadlocks/timeouts.

## First- and second-level caches

- First-level persistence-context identity is mandatory and transaction/context local.
- Hibernate second-level cache is optional and shared by factory scope for configured entity/collection data.
- Query cache stores result identities/scalars and relies on invalidation metadata; it is not an automatic performance switch.

Cache only data with a clear freshness, invalidation, memory, and multi-node strategy. Database bulk/native changes can bypass assumptions. Measure hit rate, miss cost, invalidations, stale tolerance, and tail latency.

## Equality revisited

Never generate Lombok-style equality over every entity field and association. It can initialize lazy graphs, recurse, and change hash codes. Keep entities out of hash collections until identity semantics are stable, or use an immutable natural key with database uniqueness and a proxy-compatible implementation.

## Bulk versus entity-oriented work

Entity loops provide lifecycle callbacks, cascades, dirty checking, and version checks but can be memory/statement heavy. Bulk SQL/JPQL is efficient for set-based changes but bypasses per-entity behavior and tracked state. Choose deliberately:

| Requirement | Better starting point |
|---|---|
| change one aggregate with invariants | managed entity |
| expire 5 million independent rows | bounded bulk SQL/JPQL |
| callback/outbox per row | set-based update plus set-based outbox design, or bounded entity chunks |
| database-specific upsert | native SQL/JDBC with explicit contract |

## Failure matrix

| Failure | Cause | Response |
|---|---|---|
| batch still sends single statements | ID strategy/driver/shape | instrument actual JDBC transport |
| stale update silently wins | no `@Version` or bulk bypass | version/conditional SQL |
| pessimistic endpoint times out | long lock scope or missing index | inspect lock/query evidence |
| cache returns stale entity | invalidation/bulk/external writer | explicit cache policy/eviction |
| entity disappears from `HashSet` | mutable hash field | stable equality |

## Practice

- **Foundation:** Explain what `@Version` adds to `UPDATE`.
- **Interview Core:** Choose optimistic or pessimistic control for scarce-ticket checkout.
- **Interview Core:** Prove whether 10,000 inserts are actually batched.
- **SDE-2 Follow-up:** Safely expire sessions with bulk DML, cache eviction, and observability.
