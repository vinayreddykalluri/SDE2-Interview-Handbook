import java.sql.SQLException;
import java.time.Instant;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;

/** Dependency-free executable models for the MySQL interview volume. */
public final class MySqlInterviewCompanion {
    private MySqlInterviewCompanion() {}

    enum SqlTruth {
        TRUE, FALSE, UNKNOWN;

        SqlTruth and(SqlTruth other) {
            Objects.requireNonNull(other, "other");
            if (this == FALSE || other == FALSE) {
                return FALSE;
            }
            return this == UNKNOWN || other == UNKNOWN ? UNKNOWN : TRUE;
        }
    }

    record CompositeIndex(List<String> columns) {
        CompositeIndex {
            columns = List.copyOf(columns);
            if (columns.isEmpty()) {
                throw new IllegalArgumentException("an index needs a column");
            }
        }

        boolean canSeekByEqualityPrefix(List<String> equalityColumns) {
            if (equalityColumns.size() > columns.size()) {
                return false;
            }
            return columns.subList(0, equalityColumns.size()).equals(equalityColumns);
        }
    }

    record RowVersion(String value, long committedAtSequence, boolean deleted) {}

    static String visibleValue(List<RowVersion> newestFirst, long readSequence) {
        return newestFirst.stream()
                .filter(version -> version.committedAtSequence() <= readSequence)
                .findFirst()
                .filter(version -> !version.deleted())
                .map(RowVersion::value)
                .orElse(null);
    }

    record VersionedOrder(long id, String status, long version) {
        VersionedOrder updateIfVersion(String nextStatus, long expectedVersion) {
            Objects.requireNonNull(nextStatus, "nextStatus");
            return version == expectedVersion
                    ? new VersionedOrder(id, nextStatus, version + 1)
                    : this;
        }
    }

    record Cursor(Instant createdAt, long id) {
        Cursor {
            Objects.requireNonNull(createdAt, "createdAt");
        }
    }

    static boolean comesAfterInDescendingFeed(Cursor candidate, Cursor boundary) {
        int timeComparison = candidate.createdAt().compareTo(boundary.createdAt());
        return timeComparison < 0 || (timeComparison == 0 && candidate.id() < boundary.id());
    }

    static final Comparator<Cursor> NEWEST_FIRST = Comparator
            .comparing(Cursor::createdAt, Comparator.reverseOrder())
            .thenComparing(Comparator.comparingLong(Cursor::id).reversed());

    static boolean isRetryableTransactionFailure(SQLException failure) {
        Objects.requireNonNull(failure, "failure");
        // SQLState 40001 is serialization/deadlock class. MySQL error codes 1213
        // (deadlock) and 1205 (lock wait timeout) require policy-specific retries.
        return "40001".equals(failure.getSQLState())
                || failure.getErrorCode() == 1213
                || failure.getErrorCode() == 1205;
    }

    public static void main(String[] args) {
        assert SqlTruth.FALSE.and(SqlTruth.UNKNOWN) == SqlTruth.FALSE;
        assert SqlTruth.TRUE.and(SqlTruth.UNKNOWN) == SqlTruth.UNKNOWN;

        CompositeIndex index = new CompositeIndex(
                List.of("customer_id", "status", "created_at", "order_id"));
        assert index.canSeekByEqualityPrefix(List.of("customer_id", "status"));
        assert !index.canSeekByEqualityPrefix(List.of("status"));

        List<RowVersion> versions = List.of(
                new RowVersion("PAID", 12, false),
                new RowVersion("CREATED", 8, false));
        assert "CREATED".equals(visibleValue(versions, 10));
        assert "PAID".equals(visibleValue(versions, 12));

        VersionedOrder original = new VersionedOrder(7, "CREATED", 3);
        assert original.updateIfVersion("PAID", 2).equals(original);
        assert original.updateIfVersion("PAID", 3).version() == 4;

        Instant sameTime = Instant.parse("2026-08-02T10:00:00Z");
        Cursor boundary = new Cursor(sameTime, 50);
        assert comesAfterInDescendingFeed(new Cursor(sameTime, 49), boundary);
        assert !comesAfterInDescendingFeed(new Cursor(sameTime, 51), boundary);

        SQLException deadlock = new SQLException("deadlock", "40001", 1213);
        assert isRetryableTransactionFailure(deadlock);
        assert !isRetryableTransactionFailure(new SQLException("syntax", "42000", 1064));

        System.out.println("MySqlInterviewCompanion checks passed");
    }
}
