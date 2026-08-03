# Hibernate and JPA for Java Backend Interviews

## Learning Path: SQL Before ORM

Jakarta Persistence defines portable object-persistence contracts. Hibernate ORM implements those contracts and adds provider-specific capabilities. Neither changes the fundamental database work: rows are read, constraints arbitrate races, transactions commit, and SQL plans consume resources.

> **From Vinay:** Before approving an entity mapping, write the SQL you expect it to produce. If you cannot predict the query count, join shape, lock scope, and commit point, the annotation has hidden too much. This book makes that hidden work visible one layer at a time.

## Prerequisite bridge from MySQL

Given:

```sql
CREATE TABLE purchase_order (
    order_id BIGINT PRIMARY KEY,
    customer_id BIGINT NOT NULL,
    status VARCHAR(20) NOT NULL,
    total_cents BIGINT NOT NULL,
    version BIGINT NOT NULL,
    created_at TIMESTAMP NOT NULL
);
```

JDBC can issue an explicit optimistic update:

```sql
UPDATE purchase_order
SET status = ?, version = version + 1
WHERE order_id = ? AND version = ?;
```

JPA expresses the same intent with a managed entity and `@Version`; Hibernate eventually generates a version-checked `UPDATE`. The abstraction removes repetitive mapping, not the concurrency requirement.

## Dependency order

```text
SQL + JDBC + transaction boundary
  -> JPA contract and Hibernate implementation
  -> entity identity and mapping
  -> persistence context and lifecycle
  -> dirty checking and flush
  -> associations and ownership
  -> proxies, fetching, and N+1
  -> JPQL/Criteria/native/projection/pagination
  -> batching, locks, caches, bulk work
  -> tests and production diagnosis
```

## The SQL prediction card

Before a repository or `EntityManager` call, answer:

1. Does this call execute SQL now, or only change persistence-context state?
2. Which tables, columns, and predicates will appear?
3. How many statements can this object traversal trigger?
4. When will a constraint or optimistic conflict be observed?
5. Which transaction and connection own the work?
6. Is returned state managed, detached, projected, or proxied?

## Contract boundaries

| Layer | Owns | Does not guarantee |
|---|---|---|
| JPA specification | portable lifecycle, mapping, query, locking contracts | Hibernate-specific SQL shape or every optimization |
| Hibernate | implementation, dialect, batching, fetch/caching extensions | database plan or network reliability |
| JDBC driver/pool | protocol, binding, connections, timeout mechanisms | ORM graph correctness |
| database | constraints, SQL execution, isolation/durability per configuration | application idempotency or external side effects |

## First failure timeline

```text
find entity -> mutate field -> call remote API -> flush -> constraint fails
```

The remote side effect already happened even though the database transaction rolls back. Place remote work outside the transaction or use an outbox/state-machine design. Calling `save()` earlier does not make two resource managers atomic.

## Quick check

1. Which part of optimistic locking is still performed by SQL?
2. Why can a field assignment appear successful before a constraint failure?
3. What does JPA standardize that Hibernate implements?

## Practice

- **Foundation:** Translate an entity lookup and update into expected SQL.
- **Interview Core:** Mark which operations need a transaction and which merely read.
- **SDE-2 Follow-up:** Redesign the failure timeline so the external effect is recoverable.

Continue when you can explain the difference between Java state change, SQL flush, and transaction commit.
