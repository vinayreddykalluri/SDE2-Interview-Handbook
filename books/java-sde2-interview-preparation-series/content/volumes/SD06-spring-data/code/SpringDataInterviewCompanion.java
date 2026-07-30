import java.time.Duration;
import java.time.Instant;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Queue;
import java.util.Set;

/**
 * Dependency-free Java 21 companion for Spring Data interview reasoning.
 *
 * <p>
 * The model keeps Spring Data discussion precise:
 * repository contracts, deterministic pagination, fetch strategy,
 * transactional intent, and consistency recovery semantics.
 */
public final class SpringDataInterviewCompanion {
    private SpringDataInterviewCompanion() {
    }

    public static void main(String[] args) {
        validatesMethodContract();
        validatesDeterministicOrdering();
        validatesPaginationWindow();
        validatesLockRetryPolicy();
        validatesStoreBoundaryChoice();
        validatesObservabilitySignals();
        System.out.println("SpringDataInterviewCompanion checks passed");
    }

    private static void validatesMethodContract() {
        RepositoryContract create = new RepositoryContract(
                "save", "Order", true, false, true, false);
        RepositoryContract find = new RepositoryContract(
                "findByStatusOrderByCreatedAtDesc", "Order", true, true, false, true);
        RepositoryContract delete = new RepositoryContract(
                "deleteById", "Order", false, false, true, false);

        require(create.pureCommand(), "save must be a command-oriented method");
        require(find.needsDeterministicSort(), "derived methods with paging must define stable sort");
        require(!delete.implicitFilter(), "deleteById is scoped by explicit identity filter");
        require(find.pagingSafe(), "querying latest style should be paging-safe");
    }

    private static void validatesDeterministicOrdering() {
        List<RecordRow> rows = List.of(
                new RecordRow(7L, "OPEN", 100),
                new RecordRow(5L, "OPEN", 100),
                new RecordRow(9L, "OPEN", 101));

        List<RecordRow> byCreatedAtDesc = rows.stream()
                .sorted(Comparator.comparingInt(RecordRow::createdAt).reversed())
                .toList();

        List<RecordRow> withTieBreaker = new ArrayList<>(rows);
        withTieBreaker.sort(
                Comparator.comparingInt(RecordRow::createdAt)
                        .reversed()
                        .thenComparing(RecordRow::id).reversed());

        require(byCreatedAtDesc.getFirst().createdAt() == 101, "createdAt order still must place largest first");
        require(withTieBreaker.get(0).id() == 7L, "tie-breaker keeps a deterministic top row");
    }

    private static void validatesPaginationWindow() {
        PagingWindow first = new PagingWindow(0, 20, "cursor");
        PagingWindow next = first.advance(3L);
        require(next.offset() == 20, "next offset advances by page size");
        require("cursor".equals(next.strategy()), "cursor strategy stays explicit");
        require(next.cursorId().orElse(0L) == 3L, "cursor token is preserved across pages");

        PagingWindow bad = PagingWindow.unbounded("offset");
        require(!bad.pagingSafe(), "unbounded paging is not safe by default");
    }

    private static void validatesLockRetryPolicy() {
        OptimisticWriteAttempt first = new OptimisticWriteAttempt(false);
        OptimisticWriteAttempt second = first.retry(new StaleWriteConflict("version mismatch"));
        OptimisticWriteAttempt third = second.retry(new StaleWriteConflict("version mismatch"));
        OptimisticWriteAttempt fourth = third.retry(new StaleWriteConflict("version mismatch"));

        require(second.outcome() == AttemptOutcome.RETRYING, "first conflict should require refresh and retry");
        require(third.outcome() == AttemptOutcome.RETRYING, "second conflict should also require retry policy");
        require(fourth.outcome() == AttemptOutcome.FAIL_FAST, "too many retries should fail fast");
    }

    private static void validatesStoreBoundaryChoice() {
        StorePlan sql = new StorePlan("SQL", "transactional consistency", true, true);
        StorePlan mongo = new StorePlan("MongoDB", "document locality", false, true);
        StorePlan redis = new StorePlan("Redis", "short-lived coordination", false, false);

        require(sql.fitForAggregates(), "SQL is preferred for transactional aggregates");
        require(mongo.fitForNestedDocuments(), "MongoDB fits nested read-optimized reads");
        require(redis.fitForCachingOnly(), "Redis is for cache/state, not system of record by default");
    }

    private static void validatesObservabilitySignals() {
        var signals = Map.of(
                "queryCount", "4",
                "p50LatencyMs", "45",
                "lockWaitMs", "12",
                "retryCount", "1");

        require(!signals.containsKey("error"), "successful path must not emit error markers");
        require(Integer.parseInt(signals.get("queryCount")) <= 6, "query count should stay bounded for endpoint contract");

        DiagnosticTimeline timeline = new DiagnosticTimeline(signals);
        List<String> keys = timeline.sortedKeys();
        require(keys.get(0).equals("lockWaitMs"), "sorted signal output keeps deterministic trace order");
        require(timeline.p99LatencyUs() >= 0, "latency math should be non-negative");
    }

    private static void require(boolean condition, String message) {
        if (!condition) {
            throw new AssertionError(message);
        }
    }

    record RepositoryContract(
            String method,
            String aggregate,
            boolean returnsEntity,
            boolean mayReturnMultiple,
            boolean mutates,
            boolean hasPagingIntent) {

        boolean pureCommand() {
            return mutates && returnsEntity && !mayReturnMultiple;
        }

        boolean needsDeterministicSort() {
            String normalized = method.toLowerCase(Locale.ROOT);
            return normalized.contains("find") && (normalized.contains("orderby") || hasPagingIntent);
        }

        boolean implicitFilter() {
            return normalizedName().contains("by") && !method.contains("All") && mayReturnMultiple;
        }

        boolean pagingSafe() {
            if (!hasPagingIntent) {
                return false;
            }
            return normalizedName().contains("order") && normalizedName().contains("orderby");
        }

        private String normalizedName() {
            return method.toLowerCase(Locale.ROOT);
        }
    }

    record RecordRow(long id, String status, int createdAt) {
    }

    record PagingWindow(int offset, int size, String strategy, Optional<Long> cursorId) {
        PagingWindow(int offset, int size, String strategy) {
            this(offset, size, strategy, Optional.empty());
            if (size <= 0) {
                throw new IllegalArgumentException("size must be positive");
            }
            Objects.requireNonNull(strategy, "strategy");
        }

        static PagingWindow unbounded(String strategy) {
            return new PagingWindow(0, Integer.MAX_VALUE, strategy, Optional.empty());
        }

        PagingWindow advance(long nextCursor) {
            return new PagingWindow(offset + size, size, strategy, Optional.of(nextCursor));
        }

        boolean pagingSafe() {
            return size < Integer.MAX_VALUE;
        }
    }

    enum AttemptOutcome {
        SUCCESS,
        RETRYING,
        FAIL_FAST
    }

    static final class StaleWriteConflict extends RuntimeException {
        private static final long serialVersionUID = 1L;

        StaleWriteConflict(String message) {
            super(message);
        }
    }

    static final class OptimisticWriteAttempt {
        private final int attempts;
        private final AttemptOutcome outcome;

        OptimisticWriteAttempt(int attempts, AttemptOutcome outcome) {
            this.attempts = attempts;
            this.outcome = outcome;
        }

        OptimisticWriteAttempt(boolean succeed) {
            this(1, succeed ? AttemptOutcome.SUCCESS : AttemptOutcome.RETRYING);
        }

        OptimisticWriteAttempt retry(Throwable cause) {
            if (attempts >= 3) {
                return new OptimisticWriteAttempt(attempts + 1, AttemptOutcome.FAIL_FAST);
            }
            if (cause instanceof StaleWriteConflict) {
                return new OptimisticWriteAttempt(attempts + 1, AttemptOutcome.RETRYING);
            }
            return new OptimisticWriteAttempt(attempts + 1, AttemptOutcome.RETRYING);
        }

        AttemptOutcome outcome() {
            return outcome;
        }
    }

    record StorePlan(String store, String strength, boolean transactional, boolean consistentIndex) {
        boolean fitForAggregates() {
            return "SQL".equals(store) && transactional;
        }

        boolean fitForNestedDocuments() {
            return "MongoDB".equals(store) && !transactional;
        }

        boolean fitForCachingOnly() {
            return "Redis".equals(store) && !transactional;
        }
    }

    static final class DiagnosticTimeline {
        private final Map<String, String> values;

        DiagnosticTimeline(Map<String, String> values) {
            this.values = new LinkedHashMap<>(values);
        }

        List<String> sortedKeys() {
            return new ArrayList<>(values.keySet())
                    .stream()
                    .sorted()
                    .toList();
        }

        int p99LatencyUs() {
            return sortedKeys().stream()
                    .filter(name -> name.toLowerCase(Locale.ROOT).contains("latency"))
                    .findFirst()
                    .map(values::get)
                    .map(Integer::parseInt)
                    .map(ms -> ms * 1000)
                    .orElse(0);
        }
    }

    // Minimal realistic examples used by interview explanation examples
    static final class Backoff {
        private final Queue<Duration> delays = new ArrayDeque<>();

        Backoff() {
            delays.add(Duration.ofMillis(10));
            delays.add(Duration.ofMillis(20));
            delays.add(Duration.ofMillis(30));
        }

        List<Long> drain() {
            List<Long> values = new ArrayList<>();
            while (!delays.isEmpty()) {
                values.add(delays.remove().toMillis());
            }
            return values;
        }

        boolean monotonic() {
            List<Long> values = drain();
            for (int i = 1; i < values.size(); i++) {
                if (values.get(i) < values.get(i - 1)) {
                    return false;
                }
            }
            return true;
        }
    }
}
