# JDBC, Connection Pools, Batching, Timeouts, and Pagination

JDBC is where database guarantees meet Java resource management. A correct SQL statement can still fail operationally through pool exhaustion, transaction leakage, unsafe retries, or type conversion.

## A safe transaction skeleton

```java
static boolean markPaid(
        DataSource dataSource,
        long orderId,
        long expectedVersion) throws SQLException {
    String sql = """
        UPDATE purchase_order
        SET status = 'PAID', version = version + 1
        WHERE order_id = ? AND status = 'CREATED' AND version = ?
        """;

    try (Connection connection = dataSource.getConnection()) {
        boolean originalAutoCommit = connection.getAutoCommit();
        try {
            connection.setAutoCommit(false);
            try (PreparedStatement statement = connection.prepareStatement(sql)) {
                statement.setLong(1, orderId);
                statement.setLong(2, expectedVersion);
                int changed = statement.executeUpdate();
                if (changed != 1) {
                    connection.rollback();
                    return false;
                }
            }
            connection.commit();
            return true;
        } catch (SQLException failure) {
            connection.rollback();
            throw failure;
        } finally {
            connection.setAutoCommit(originalAutoCommit);
        }
    }
}
```

Production pools usually reset connection state on return, but application code should not depend on leaking altered autocommit, isolation, read-only flags, or session variables. Framework transaction managers centralize this boundary; understand the equivalent behavior before using them.

## Prepared statements

Prepared statements separate SQL structure from data, prevent ordinary parameter injection, and provide typed binding. They do not parameterize table names, column names, or arbitrary sort expressions; allow-list those structural choices.

Use `setNull(index, sqlType)` when needed. Check JDBC driver behavior for temporal types and unsigned/narrowing conversions. Avoid constructing `IN (...)` by concatenating untrusted text; generate placeholders or use a supported collection strategy.

## Pool sizing is a queueing decision

A connection is a scarce concurrent resource. A larger pool can increase database contention rather than throughput.

Estimate from measured service time and arrival rate, database connection budget, number of application instances, and acceptable wait. Monitor separately:

- pool acquisition wait;
- active/idle/pending connections;
- query execution time;
- transaction duration;
- timeout/cancel result.

Set acquisition, connect, socket, statement, and transaction deadlines deliberately. A timeout exception does not always prove server-side work stopped; cancellation and outcome reconciliation are part of the contract.

## Batch writes

```java
try (PreparedStatement statement = connection.prepareStatement(
        "INSERT INTO order_item(order_id, product_id, quantity) VALUES (?, ?, ?)")) {
    for (OrderItem item : items) {
        statement.setLong(1, item.orderId());
        statement.setLong(2, item.productId());
        statement.setInt(3, item.quantity());
        statement.addBatch();
    }
    int[] counts = statement.executeBatch();
}
```

Batching reduces round trips, but huge transactions expand locks, logs, memory, and failure replay. Chunk by measured size. Driver rewrite settings may affect whether a JDBC batch becomes a multi-value statement. Generated keys and partial batch failures require explicit tests.

## Streaming and fetch behavior

Reading a million rows into a `List` is not streaming. Configure fetch behavior supported by the driver/version, process incrementally, hold the connection only as long as necessary, and define transaction consistency. A streaming result ties up a connection; backpressure must reach the caller.

## Cursor pagination in Java

```java
record OrderCursor(Instant createdAt, long orderId) {}

String sql = """
    SELECT order_id, total_cents, created_at
    FROM purchase_order
    WHERE customer_id = ?
      AND (created_at < ? OR (created_at = ? AND order_id < ?))
    ORDER BY created_at DESC, order_id DESC
    LIMIT ?
    """;
```

Fetch `pageSize + 1` to determine whether another page exists, return only `pageSize`, and encode the last row’s complete ordering key. Decide how inserts/deletes between requests affect the product experience.

## Resource and failure matrix

| Mistake | Result | Repair |
|---|---|---|
| no try-with-resources | pool leak | close result, statement, connection deterministically |
| catch without rollback | ambiguous transaction state | rollback then propagate/translate |
| retry one statement | broken unit-of-work invariant | retry whole idempotent transaction |
| log bind values indiscriminately | secrets/PII exposure | structured redacted diagnostics |
| pool much larger than DB capacity | queue moves into database | budget globally and load test |
| deep `OFFSET` | increasing discarded work | keyset when navigation permits |

## Quick check and practice

1. Which connection settings can leak across pool borrowers?
2. Why is a query timeout not proof of rollback?
3. What makes a batch too large?
4. Why must a cursor contain a tie-breaker?

- **Foundation:** Implement a query using try-with-resources and typed mapping.
- **Interview Core:** Add bounded deadlock retry around the whole transaction.
- **SDE-2 Follow-up:** Diagnose an endpoint whose SQL is 20 ms but pool acquisition is 900 ms.
