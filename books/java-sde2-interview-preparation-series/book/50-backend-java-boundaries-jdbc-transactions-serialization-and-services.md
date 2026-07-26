# 50. Backend Java Boundaries: JDBC, Transactions, Serialization, and Services

## Learning objectives

By the end of this chapter, you should be able to:

- use JDBC resources and prepared statements with explicit ownership and transaction control;
- choose transaction boundaries from business invariants rather than repository method boundaries;
- explain isolation anomalies and why database implementations must be verified;
- avoid dual-write inconsistency with transactional outbox and idempotent processing patterns;
- design versioned, validated serialization DTOs independent from persistence entities; and
- specify timeouts, retries, idempotency, backpressure, and observability at service boundaries.

## Why this matters at SDE-2

Backend correctness lives between components. A Java method can be locally correct while its transaction commits half an invariant, its JSON change breaks an older consumer, or its retry charges a customer twice. JDBC, messaging, HTTP, and serialization are not plumbing details; they define consistency and failure semantics.

At SDE-2, you are expected to place a transaction around a use case, explain what it cannot include atomically, and make remote effects retry-safe. You should know that closing a pooled connection usually returns it rather than closing a socket, that `PreparedStatement` protects values rather than arbitrary SQL identifiers, and that a timeout does not prove the remote operation did not happen.

## First-principles model

A boundary converts one model and failure domain into another:

```text
HTTP bytes -> validated request DTO -> application command
            -> domain transition -> SQL transaction
            -> outbox record -> broker message -> remote consumer
```

Each arrow needs a contract: encoding, validation, identity, timeout, atomicity, retry, ordering, ownership, and version compatibility.

A local database transaction groups statements under ACID goals:

- atomicity: all transaction effects commit or none do;
- consistency: constraints and application rules move valid state to valid state;
- isolation: concurrent transactions observe behavior defined by an isolation level;
- durability: committed changes survive according to database guarantees and configuration.

The database transaction does not automatically include an HTTP call, email, cache, or message broker. Crossing failure domains requires protocols and recoverable state, not a larger Java `try` block.

> **Specification boundary:** JDBC standardizes interfaces and broad transaction operations. SQL syntax, type mappings, generated keys, isolation behavior, lock semantics, timeout enforcement, batch behavior, and connection-pool reset are database, driver, and pool specific. Test the exact versions in use.

## Core terminology

- **DataSource:** Factory for database connections; often backed by a pool.
- **Connection:** JDBC session and transaction context.
- **Prepared statement:** Precompiled/parameterized statement separating SQL structure from values.
- **Transaction boundary:** Scope whose database effects commit or roll back together.
- **Isolation anomaly:** Concurrent outcome such as dirty read, nonrepeatable read, phantom, lost update, or write skew.
- **Optimistic concurrency:** Detect conflict using a version or compare-and-set condition.
- **Pessimistic locking:** Acquire database locks before a conflicting transition.
- **Idempotency:** Repeating an operation with the same identity has the intended single logical effect.
- **Outbox:** Database table storing events in the same local transaction as domain changes.
- **Inbox/deduplication:** Consumer record of processed message identities.
- **DTO:** Boundary-specific data transfer object.
- **Schema evolution:** Changing a wire or storage representation while preserving compatibility policy.
- **Retry budget:** Bound on attempts, elapsed time, and load amplification.

## Detailed mechanics

### JDBC resource ownership

Acquire a connection as late as practical and release it promptly. `Connection`, `Statement`, and `ResultSet` are `AutoCloseable`; nested try-with-resources closes them in reverse order. Closing a connection from a pool normally returns it to the pool, but that behavior belongs to the configured `DataSource`.

Do not hold a database connection while performing slow remote I/O. It extends lock time and consumes scarce pool capacity. A pool is a concurrency bound, not a throughput generator. Monitor active, idle, wait time, timeouts, and leaked borrows.

Use prepared statements for values:

```java
try (var statement = connection.prepareStatement(
        "select id, status from orders where customer_id = ?")) {
    statement.setString(1, customerId);
    try (var rows = statement.executeQuery()) {
        while (rows.next()) {
            // Map explicitly by stable column labels.
        }
    }
}
```

Placeholders do not represent table names, column names, sort direction, or arbitrary SQL fragments. Dynamic identifiers require a fixed allow-list mapped to known SQL. Never concatenate untrusted input.

Set query, lock, transaction, and network timeouts according to driver capabilities and an end-to-end deadline. `Statement.setQueryTimeout` units and enforcement have driver limits. A canceled query may continue server-side briefly; monitor the database as well as the client.

### Transaction boundaries and failure handling

Disable auto-commit before a multi-statement transaction, commit once after all invariant checks and writes, and roll back on failure. If rollback throws, preserve it as a suppressed exception rather than losing the primary cause. Never return a connection with an open transaction or altered session state; use a pool that reliably resets documented state and keep application behavior disciplined.

The boundary belongs at the application use case. Two repository methods called separately with independent transactions cannot enforce a cross-table invariant. Conversely, a transaction spanning user think time or a remote request holds resources and locks too long.

Database constraints are the final concurrent guard. Use primary keys, unique constraints, foreign keys, checks, and atomic update predicates. A Java "check then insert" can race. Map constraint violations to domain outcomes carefully using driver/database error information rather than brittle message parsing.

### Isolation and concurrency control

Isolation levels are commonly described by prohibited phenomena, but real databases use locks or multiversion concurrency control with vendor-specific behavior. "Repeatable read" does not mean identical semantics everywhere. Serializable execution can abort a transaction and require retry.

For an update such as reserving inventory, useful approaches include:

- atomic conditional SQL: update where available quantity is sufficient, then inspect update count;
- optimistic versioning: update where `version = expected`, increment version, reject count zero;
- pessimistic lock: lock the row, inspect, then update within one short transaction;
- serializable transaction: let the database detect conflicting executions and retry safe failures.

Choose from contention, invariant shape, latency, and database behavior. Retrying a transaction reruns its code, so no non-idempotent external side effect should occur inside it.

> **Vendor boundary:** Lock clauses, deadlock detection, snapshot rules, serialization failures, timestamp precision, UUID/JSON types, and DDL transaction behavior differ. Treat database documentation and integration tests as part of the contract.

### The dual-write problem

Suppose the service commits an order and then publishes `OrderCreated`. A crash between those actions loses the event. Publishing first can expose an event for an order that later rolls back. Ordinary local transactions cannot atomically cover both systems.

The transactional outbox writes the order and an outbox row in one database transaction. A separate relay reads committed rows, publishes messages, and marks progress. Publication may occur more than once around failures, so consumers use stable event IDs and idempotent inbox or natural-key effects. This provides recoverable at-least-once processing, not magical exactly-once behavior across arbitrary side effects.

An outbox needs cleanup, lag metrics, partition/order policy, claim/lease semantics, and poison-message handling. Change-data-capture can relay rows but remains an operational component with its own offsets and failure modes.

### Serialization boundaries

Do not serialize persistence entities or rich domain objects directly by default. Lazy-loading proxies, bidirectional graphs, internal fields, and refactors make unstable contracts. Define request/response/event DTOs and map explicitly at the boundary.

Specify charset, media type, field names, required/optional/default behavior, numeric range and precision, timestamps and timezone, enum evolution, unknown fields, null versus absent, collection limits, and maximum nesting/bytes. Validate syntactic shape at decoding and domain rules in the application/domain layer.

Backward compatibility usually means new producers avoid removing or changing fields older consumers require, while consumers tolerate documented additions. Adding an enum value can break exhaustive older consumers. Version semantics, not merely a `/v2` path, must be planned.

Native Java serialization is unsuitable for untrusted service boundaries. Prefer an explicit schema and safe parser configuration. No format is automatically safe: JSON and binary parsers still need size, depth, duplicate-field, polymorphism, and resource limits.

### Remote service boundaries

Every call needs an end-to-end deadline propagated through connection acquisition, connect, request, and response phases. Layered timeouts should leave callers time to recover. Cancellation and interruption should release resources.

Retry only failures that are transient and operations that are idempotent or protected by an idempotency key. Use exponential backoff with jitter and a strict attempt/deadline budget. Retries amplify load during an outage; combine them with admission control, circuit breaking, concurrency limits, and bounded queues.

A timeout is an unknown outcome. The server may have committed after the client stopped waiting. Reconcile using an idempotency key and status lookup rather than issuing a semantically new request. Record request IDs and trace context, but do not place secrets or unbounded tenant IDs in metric labels.

### Framework transaction caveats

Framework annotations often implement transactions through proxies or interception. Method visibility, self-invocation, exception type, propagation, asynchronous boundaries, and reactive execution can affect whether a transaction exists. These are framework/version rules, not Java or JDBC guarantees. Verify with integration tests that inspect committed outcomes, not only mocked repository calls.

## Worked Java example

This service writes an order and outbox record atomically using standard JDBC:

```java
import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.Clock;
import java.time.Instant;
import java.util.UUID;
import javax.sql.DataSource;

record PlaceOrder(String customerId, BigDecimal total, String currency) {}

public final class OrderApplicationService {
    private final DataSource dataSource;
    private final Clock clock;

    public OrderApplicationService(DataSource dataSource, Clock clock) {
        this.dataSource = java.util.Objects.requireNonNull(dataSource);
        this.clock = java.util.Objects.requireNonNull(clock);
    }

    public String place(PlaceOrder command) throws SQLException {
        validate(command);
        String orderId = UUID.randomUUID().toString();
        String eventId = UUID.randomUUID().toString();
        Instant now = clock.instant();

        try (Connection connection = dataSource.getConnection()) {
            connection.setAutoCommit(false);
            try {
                insertOrder(connection, orderId, command, now);
                insertOutbox(connection, eventId, orderId, now);
                connection.commit();
                return orderId;
            } catch (SQLException | RuntimeException failure) {
                try {
                    connection.rollback();
                } catch (SQLException rollbackFailure) {
                    failure.addSuppressed(rollbackFailure);
                }
                throw failure;
            }
        }
    }
```

The insert helpers and validation complete the same `OrderApplicationService` class:

```java

    private static void insertOrder(Connection connection, String orderId,
            PlaceOrder command, Instant now) throws SQLException {
        String sql = "insert into orders "
                + "(id, customer_id, total, currency, created_at) "
                + "values (?, ?, ?, ?, ?)";
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, orderId);
            statement.setString(2, command.customerId());
            statement.setBigDecimal(3, command.total());
            statement.setString(4, command.currency());
            statement.setTimestamp(5, Timestamp.from(now));
            if (statement.executeUpdate() != 1) {
                throw new SQLException("order insert affected unexpected row count");
            }
        }
    }

    private static void insertOutbox(Connection connection, String eventId,
            String orderId, Instant now) throws SQLException {
        String sql = "insert into outbox "
                + "(event_id, event_type, aggregate_id, payload, created_at) "
                + "values (?, ?, ?, ?, ?)";
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, eventId);
            statement.setString(2, "OrderCreated");
            statement.setString(3, orderId);
            statement.setString(4, "order-created:" + orderId);
            statement.setTimestamp(5, Timestamp.from(now));
            if (statement.executeUpdate() != 1) {
                throw new SQLException("outbox insert affected unexpected row count");
            }
        }
    }

    private static void validate(PlaceOrder command) {
        if (command == null || command.customerId() == null
                || command.total() == null || command.total().signum() < 0
                || command.currency() == null) {
            throw new IllegalArgumentException("invalid order command");
        }
    }
}
```

The textual payload is deliberately minimal; a real event uses a versioned serializer and schema. SQL types and timestamp mappings must be integration-tested with the selected driver.

## Execution or memory walkthrough

1. Validation runs before borrowing a scarce connection.
2. IDs and time are created once, making retry identity a design decision. As written, a caller retry invokes `place` again and creates a new order; a public idempotency key should be supplied for retry-safe API behavior.
3. Auto-commit is disabled. The order insert and outbox insert share one connection and transaction.
4. If both affect one row, commit makes both visible under database semantics.
5. If the second insert fails, rollback removes the first insert. A rollback failure is preserved as suppressed evidence.
6. A commit or close exception can leave the caller uncertain whether commit succeeded. With a pool, close should return and reset the connection according to pool configuration; public retry safety still requires a stable idempotency key.
7. Later, a relay publishes the event. A crash after broker acceptance but before marking progress can cause duplicate delivery; consumers deduplicate by `eventId`.

The method retains only command and small JDBC objects. The database, not the Java heap, owns durable state. Holding the transaction open while calling the broker would increase lock and pool occupancy without creating cross-system atomicity.

## Complexity and performance

Application-side computation is `O(1)` for fixed-size fields, but database cost depends on indexes, constraints, logging, contention, and network latency. An indexed primary-key insert is often described as `O(log n)` for a B-tree, yet storage engines batch, cache, split pages, and write logs; measure the actual database.

Important capacity relationships are:

```text
concurrent database work <= connection-pool capacity
queueing wait + query time + commit time <= request deadline
retry traffic <= remaining downstream capacity
outbox production rate <= sustainable relay rate over time
```

Batching can reduce round trips but changes failure attribution and memory. Large transactions retain locks/versions/log records longer and make retries more expensive. N+1 queries turn one logical operation into `O(n)` round trips; fetch or batch deliberately while avoiding explosive joins.

## Edge cases and common mistakes

- Building SQL by concatenating values or untrusted sort identifiers.
- Forgetting to close a result set, statement, or connection on an exception path.
- Holding a connection across HTTP calls or user think time.
- Placing each repository call in an independent transaction when the invariant spans calls.
- Assuming application checks replace unique or check constraints.
- Retrying all SQL exceptions, including permanent constraints and unknown side effects.
- Assuming timeout means the database or remote service rolled back.
- Publishing an event after commit with no durable recovery record.
- Claiming exactly-once effects because a broker has an exactly-once feature.
- Serializing ORM entities, lazy proxies, or internal exception objects directly.
- Changing enum, timestamp, numeric, null, or unknown-field semantics without compatibility testing.
- Returning database error details or SQL in public responses.
- Using an annotation without verifying proxy, propagation, and rollback behavior.
- Making queues and retry attempts unbounded during a downstream outage.

## Production engineering notes

Size connection pools from database capacity and measured service time, not application thread count. Set acquisition deadlines and expose saturation. Validate connections and pool reset behavior. Keep transactions short, but never split an invariant merely to lower a timing metric.

Use migration tooling with ordered, reviewed, backward-compatible changes. Deploy expand/migrate/contract sequences when old and new application versions overlap. Index creation and DDL locking are database-specific operational events, not harmless startup steps.

Version events and APIs, retain contract fixtures, and run consumer compatibility tests. Redact logs, cap body size, reject invalid content types/charsets, and avoid generic polymorphic deserialization from untrusted input.

For every remote dependency, publish deadline, retry, idempotency, circuit, concurrency, and fallback policy. Measure attempts separately from logical requests, and measure outbox age, retries, poison records, deduplication conflicts, and end-to-end delivery latency.

## Interview questions and model answers

**Where should a transaction boundary be?**

Around the shortest database scope that enforces one business invariant or use case. It should include all required local reads and writes, but not remote I/O that the database cannot commit atomically.

**How do prepared statements prevent SQL injection?**

They separate SQL structure from bound values so values are not parsed as SQL syntax. They do not parameterize identifiers or arbitrary fragments; dynamic structure must come from an allow-list.

**What is the dual-write problem?**

Updating a database and publishing to another system cannot be made atomic by ordinary local transaction code. A crash between operations creates inconsistency. A transactional outbox stores publish intent with the domain update and supports recoverable at-least-once relay.

**How do you handle duplicate messages?**

Give each logical event a stable ID and make the consumer effect idempotent using an inbox/unique constraint or natural idempotent update in the same transaction as its business effect.

**What does a timeout tell you?**

Only that the caller stopped waiting within its policy. The remote operation may not have started, may still run, or may have committed. Use idempotency and status reconciliation.

**How do you choose an isolation level?**

Start from the invariant and concurrent anomalies that must be prevented, then choose an atomic statement, optimistic version, lock, or isolation level supported by the target database. Test contention and retry behavior.

## Exercises

1. Implement an optimistic update using `where id = ? and version = ?`; interpret update count zero.
2. Add a caller-provided idempotency key to the worked example with a unique database constraint and replayed-result behavior.
3. Design an outbox relay claim algorithm and state what happens after publish succeeds but progress update fails.
4. Specify a versioned `OrderCreated` schema, including time, money, enum, unknown field, and size behavior.
5. Analyze a request whose 500 ms deadline includes a 200 ms pool wait and two retries. Construct a valid remaining-time budget.
6. Write an integration test that proves a framework transaction rolls back both rows when the outbox insert fails.

## Chapter summary

Backend boundaries convert models and failure domains. JDBC code must own resources, parameterize values, keep transactions short, and rely on database constraints for concurrent truth. Isolation is an invariant decision with vendor-specific behavior. Database and broker effects require a recoverable outbox plus idempotent consumers, not a claim of universal exactly-once delivery. Serialization and remote APIs need explicit version, validation, timeout, retry, and unknown-outcome contracts.

## Revision checklist

- [ ] I close JDBC resources and avoid holding connections across remote I/O.
- [ ] I use prepared values and allow-list dynamic SQL structure.
- [ ] I place transactions around use-case invariants and preserve rollback failures.
- [ ] I can compare atomic updates, optimistic versions, locks, and isolation levels.
- [ ] I understand dual-write failure, outbox relay, and consumer idempotency.
- [ ] I define DTO schema, limits, timestamps, money, enum, null, and compatibility behavior.
- [ ] I treat timeout as an unknown outcome and retries as bounded load amplification.
- [ ] I verify database, driver, pool, and framework behavior with integration tests.
