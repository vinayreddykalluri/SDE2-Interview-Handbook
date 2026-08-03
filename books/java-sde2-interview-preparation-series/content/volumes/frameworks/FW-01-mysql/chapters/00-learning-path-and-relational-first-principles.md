# MySQL for Java Backend Interviews

## Learning Path and Relational First Principles

MySQL is not merely the place where a Java service stores objects. It is a concurrent system that enforces constraints, chooses physical access paths, writes recovery records, and coordinates transactions that may disagree about the same data.

> **From Vinay:** In interviews, start with the business invariant and the SQL that preserves it. Add Hibernate or Spring Data only after you can explain what the database must do. That order keeps convenient APIs from hiding correctness.

## What you will be able to do

By the end of this book, you should be able to:

- turn requirements into tables, keys, constraints, and query shapes;
- write SQL using joins, grouping, CTEs, and window functions;
- explain an InnoDB lookup from B+tree root to clustered row;
- choose an index from evidence instead of folklore;
- reason about MVCC, isolation, locks, deadlocks, and retries;
- build a safe JDBC boundary with pooling, batching, timeouts, and pagination; and
- discuss migrations, replication, recovery, security, and observability at SDE-2 depth.

## The dependency order

```text
requirement
   -> invariant and relational model
   -> types, NULL rules, keys, and constraints
   -> SQL result correctness
   -> physical access path and EXPLAIN evidence
   -> transaction and concurrency behavior
   -> Java/JDBC resource boundary
   -> production change, recovery, and operations
```

Do not jump to index tuning before proving that the query returns the right rows. Do not discuss transaction retries before identifying which statements are safe to repeat. Do not call a replica a backup before defining the recovery point you can restore.

## One running domain

Most examples use a small ordering domain:

```sql
CREATE TABLE customer (
    customer_id BIGINT PRIMARY KEY,
    email VARCHAR(320) NOT NULL,
    status VARCHAR(20) NOT NULL,
    created_at TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    CONSTRAINT uq_customer_email UNIQUE (email),
    CONSTRAINT chk_customer_status CHECK (status IN ('ACTIVE', 'BLOCKED'))
);

CREATE TABLE purchase_order (
    order_id BIGINT PRIMARY KEY,
    customer_id BIGINT NOT NULL,
    request_key VARCHAR(64) NOT NULL,
    status VARCHAR(20) NOT NULL,
    total_cents BIGINT NOT NULL,
    version BIGINT NOT NULL DEFAULT 0,
    created_at TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    CONSTRAINT uq_order_request UNIQUE (customer_id, request_key),
    CONSTRAINT fk_order_customer FOREIGN KEY (customer_id)
        REFERENCES customer(customer_id),
    CONSTRAINT chk_order_total CHECK (total_cents >= 0)
);
```

The unique request key is not cosmetic. It turns “create this order once” into a database-enforced invariant even when a caller retries after a timeout.

## The SDE-2 answer frame

For every database scenario, answer in this order:

1. **Correctness:** What invariant must always hold?
2. **Query:** Which rows and columns must be read or changed?
3. **Access path:** Which index can serve that query, and what evidence proves it?
4. **Concurrency:** What if two transactions do it at once?
5. **Failure:** What if the connection fails before, during, or after commit?
6. **Operations:** How will we migrate, observe, recover, and roll back?

## First failure matrix

| Failure | What the caller knows | Safe response |
|---|---|---|
| Validation fails before SQL | Nothing was written | Correct input; do not retry blindly |
| Constraint rejects statement | Transaction may be usable or marked for rollback by framework policy | Interpret the named invariant; roll back the unit of work |
| Deadlock victim | Current transaction was rolled back | Retry the whole idempotent transaction with a bound and jitter |
| Socket timeout during commit | Outcome may be unknown | Reconcile through idempotency key or read-after-reconnect |
| Replica is behind | Read may omit a committed write | Route consistency-sensitive read to source or use a causal strategy |

## Quick check

1. Why should a schema model an invariant even if Java validates it?
2. Why is “the API timed out” not proof that a transaction rolled back?
3. Which part of the answer frame comes before index design?

## Practice

- **Foundation:** Write three invariants for an order-creation endpoint.
- **Interview Core:** Decide which invariant belongs in Java, which in SQL, and which in both.
- **SDE-2 Follow-up:** Describe how you would reconcile an unknown commit result without creating a duplicate order.

## Readiness checkpoint

Continue when you can explain why a unique constraint is a concurrency tool, not just a validation convenience.
