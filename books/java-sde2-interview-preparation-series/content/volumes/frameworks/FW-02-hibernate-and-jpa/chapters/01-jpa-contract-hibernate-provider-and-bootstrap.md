# JPA Contract, Hibernate Provider, and Bootstrap

## The names matter

- **Jakarta Persistence (JPA)** is the specification and API: annotations such as `@Entity`, `EntityManager`, JPQL, lifecycle, and portable locking rules.
- **Hibernate ORM** is a provider that implements JPA and offers native APIs/extensions.
- **Spring Data JPA** builds repository abstractions over JPA; it is covered in the next volume.

Saying “JPA is an ORM” is imprecise. It is the contract used by ORM providers.

## Minimal entity

```java
@Entity
@Table(name = "purchase_order")
public class PurchaseOrder {
    @Id
    @Column(name = "order_id")
    private Long id;

    @Column(nullable = false, length = 20)
    private String status;

    protected PurchaseOrder() {}

    public PurchaseOrder(Long id, String status) {
        this.id = Objects.requireNonNull(id);
        this.status = Objects.requireNonNull(status);
    }
}
```

An entity needs a no-argument constructor that is public or protected under the portable contract. Avoid final entity classes and final persistent accessors when proxying/lazy behavior may require subclassing.

## Java SE lifecycle

```java
EntityManagerFactory factory =
        Persistence.createEntityManagerFactory("orders");

try (EntityManager entityManager = factory.createEntityManager()) {
    EntityTransaction transaction = entityManager.getTransaction();
    transaction.begin();
    try {
        entityManager.persist(new PurchaseOrder(1L, "CREATED"));
        transaction.commit();
    } catch (RuntimeException failure) {
        if (transaction.isActive()) {
            transaction.rollback();
        }
        throw failure;
    }
}
factory.close();
```

`EntityManagerFactory` is expensive and normally application-scoped. `EntityManager` represents a persistence-context/unit-of-work boundary and is not shared concurrently across requests.

Spring usually manages these resources and transactions, but the underlying scope still matters.

## Configuration and schema ownership

`persistence.xml`, programmatic configuration, or framework properties define provider, datasource, mappings, dialect, schema validation, logging, batching, and cache behavior.

For production, prefer versioned database migrations. ORM schema generation is useful for disposable tests and prototypes. A startup `update` strategy is not a substitute for reviewed, reversible migrations against production-scale data.

## Exceptions and transaction state

Persistence failures often surface at flush or commit. After a runtime persistence exception, assume the current transaction must roll back. Do not catch a constraint exception and continue to write unrelated managed entities in the same unit of work.

Exception types may be JPA-standard (`OptimisticLockException`, `EntityNotFoundException`, `TransactionRequiredException`) or provider/driver-specific beneath them. Translate at a stable application boundary while preserving diagnostic cause and constraint identity.

## When direct JDBC is clearer

Use SQL/JDBC for bulk updates, highly tuned reporting, database-specific features, or hot paths where entity tracking adds no value. Use JPA for domain-oriented transactional changes where identity, associations, dirty checking, and optimistic locking reduce repeated mapping. Mixed use requires awareness that direct SQL can make the persistence context stale.

## Debug exercise

**Broken:** create one static `EntityManager` and use it from every request.

**Why:** it is not a thread-safe global cache; it mixes units of work, grows managed state, and creates concurrency hazards.

**Repair:** inject/create a transaction-scoped manager per unit of work and keep the factory/pool shared.

## Quick check and practice

1. Who generates the SQL: the JPA specification or provider?
2. Which object is expensive and shared, and which is unit-of-work scoped?
3. Why should production DDL use a migration system?

- **Foundation:** Bootstrap the lab and persist one entity.
- **Interview Core:** Compare direct JDBC with a managed entity update.
- **SDE-2 Follow-up:** Define an exception translation policy that preserves retry classification.
