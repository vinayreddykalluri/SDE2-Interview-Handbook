package interview.mysql;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class MySqlPortableBehaviorTest {
    private String url;

    @BeforeEach
    void createSchema() throws SQLException {
        url = "jdbc:h2:mem:" + UUID.randomUUID()
                + ";MODE=MySQL;DB_CLOSE_DELAY=-1;DATABASE_TO_LOWER=TRUE";
        try (Connection connection = DriverManager.getConnection(url);
             Statement statement = connection.createStatement()) {
            statement.execute("""
                CREATE TABLE customer (
                    customer_id BIGINT PRIMARY KEY,
                    email VARCHAR(320) NOT NULL UNIQUE
                )
                """);
            statement.execute("""
                CREATE TABLE purchase_order (
                    order_id BIGINT PRIMARY KEY,
                    customer_id BIGINT NOT NULL,
                    request_key VARCHAR(64) NOT NULL,
                    status VARCHAR(20) NOT NULL,
                    total_cents BIGINT NOT NULL CHECK (total_cents >= 0),
                    version BIGINT NOT NULL DEFAULT 0,
                    created_at TIMESTAMP NOT NULL,
                    UNIQUE (customer_id, request_key),
                    FOREIGN KEY (customer_id) REFERENCES customer(customer_id)
                )
                """);
            statement.execute("INSERT INTO customer VALUES (1, 'one@example.com'), (2, 'two@example.com')");
        }
    }

    @Test
    void constraintsArbitrateDuplicateRequests() throws SQLException {
        insertOrder(10, 1, "request-a", "CREATED", 500, Instant.parse("2026-08-01T10:00:00Z"));
        assertThrows(SQLException.class,
                () -> insertOrder(11, 1, "request-a", "CREATED", 700,
                        Instant.parse("2026-08-01T11:00:00Z")));
        assertThrows(SQLException.class,
                () -> insertOrder(12, 1, "request-b", "CREATED", -1,
                        Instant.parse("2026-08-01T12:00:00Z")));
    }

    @Test
    void leftJoinRetainsCustomersWithZeroOrders() throws SQLException {
        insertOrder(10, 1, "a", "PAID", 500, Instant.parse("2026-08-01T10:00:00Z"));
        try (Connection connection = DriverManager.getConnection(url);
             Statement statement = connection.createStatement();
             ResultSet rows = statement.executeQuery("""
                 SELECT c.customer_id, COUNT(o.order_id) AS paid_count
                 FROM customer c
                 LEFT JOIN purchase_order o
                   ON o.customer_id = c.customer_id AND o.status = 'PAID'
                 GROUP BY c.customer_id
                 ORDER BY c.customer_id
                 """)) {
            List<Long> counts = new ArrayList<>();
            while (rows.next()) {
                counts.add(rows.getLong("paid_count"));
            }
            assertEquals(List.of(1L, 0L), counts);
        }
    }

    @Test
    void cteAndWindowRankRowsWithoutCollapsingThem() throws SQLException {
        insertOrder(10, 1, "a", "PAID", 500, Instant.parse("2026-08-01T10:00:00Z"));
        insertOrder(11, 1, "b", "PAID", 700, Instant.parse("2026-08-02T10:00:00Z"));
        try (Connection connection = DriverManager.getConnection(url);
             Statement statement = connection.createStatement();
             ResultSet rows = statement.executeQuery("""
                 WITH ranked AS (
                     SELECT order_id,
                            ROW_NUMBER() OVER (
                                PARTITION BY customer_id
                                ORDER BY created_at DESC, order_id DESC
                            ) AS rn
                     FROM purchase_order
                 )
                 SELECT order_id FROM ranked WHERE rn = 1
                 """)) {
            rows.next();
            assertEquals(11L, rows.getLong(1));
        }
    }

    @Test
    void rollbackRestoresTheUnitOfWork() throws SQLException {
        try (Connection connection = DriverManager.getConnection(url)) {
            connection.setAutoCommit(false);
            try (Statement statement = connection.createStatement()) {
                statement.execute("INSERT INTO customer VALUES (3, 'rollback@example.com')");
            }
            connection.rollback();
        }
        assertEquals(0L, scalarLong("SELECT COUNT(*) FROM customer WHERE customer_id = 3"));
    }

    @Test
    void versionPredicateDetectsAStaleUpdate() throws SQLException {
        insertOrder(10, 1, "a", "CREATED", 500, Instant.parse("2026-08-01T10:00:00Z"));
        String sql = """
            UPDATE purchase_order
            SET status = 'PAID', version = version + 1
            WHERE order_id = ? AND version = ?
            """;
        assertEquals(1, executeUpdate(sql, 10L, 0L));
        assertEquals(0, executeUpdate(sql, 10L, 0L));
    }

    @Test
    void keysetPredicateUsesTheCompleteDescendingBoundary() throws SQLException {
        Instant tied = Instant.parse("2026-08-02T10:00:00Z");
        insertOrder(10, 1, "a", "PAID", 500, tied);
        insertOrder(11, 1, "b", "PAID", 600, tied);
        insertOrder(9, 1, "c", "PAID", 400, Instant.parse("2026-08-01T10:00:00Z"));

        String sql = """
            SELECT order_id FROM purchase_order
            WHERE created_at < ? OR (created_at = ? AND order_id < ?)
            ORDER BY created_at DESC, order_id DESC
            """;
        try (Connection connection = DriverManager.getConnection(url);
             PreparedStatement statement = connection.prepareStatement(sql)) {
            Timestamp boundary = Timestamp.from(tied);
            statement.setTimestamp(1, boundary);
            statement.setTimestamp(2, boundary);
            statement.setLong(3, 11L);
            try (ResultSet rows = statement.executeQuery()) {
                List<Long> ids = new ArrayList<>();
                while (rows.next()) {
                    ids.add(rows.getLong(1));
                }
                assertEquals(List.of(10L, 9L), ids);
            }
        }
    }

    @Test
    void jdbcBatchPersistsEveryBoundRow() throws SQLException {
        String sql = "INSERT INTO customer(customer_id, email) VALUES (?, ?)";
        try (Connection connection = DriverManager.getConnection(url);
             PreparedStatement statement = connection.prepareStatement(sql)) {
            for (long id = 3; id <= 5; id++) {
                statement.setLong(1, id);
                statement.setString(2, "customer-" + id + "@example.com");
                statement.addBatch();
            }
            assertEquals(3, statement.executeBatch().length);
        }
        assertEquals(5L, scalarLong("SELECT COUNT(*) FROM customer"));
    }

    private void insertOrder(
            long id, long customerId, String requestKey, String status,
            long totalCents, Instant createdAt) throws SQLException {
        String sql = """
            INSERT INTO purchase_order(
                order_id, customer_id, request_key, status, total_cents, created_at)
            VALUES (?, ?, ?, ?, ?, ?)
            """;
        try (Connection connection = DriverManager.getConnection(url);
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setLong(1, id);
            statement.setLong(2, customerId);
            statement.setString(3, requestKey);
            statement.setString(4, status);
            statement.setLong(5, totalCents);
            statement.setTimestamp(6, Timestamp.from(createdAt));
            statement.executeUpdate();
        }
    }

    private int executeUpdate(String sql, long id, long version) throws SQLException {
        try (Connection connection = DriverManager.getConnection(url);
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setLong(1, id);
            statement.setLong(2, version);
            return statement.executeUpdate();
        }
    }

    private long scalarLong(String sql) throws SQLException {
        try (Connection connection = DriverManager.getConnection(url);
             Statement statement = connection.createStatement();
             ResultSet rows = statement.executeQuery(sql)) {
            rows.next();
            return rows.getLong(1);
        }
    }
}
