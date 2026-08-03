import java.time.Duration;
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
        validatesPaginationModels();
        validatesLockRetryPolicy();
        validatesStoreBoundaryChoice();
        validatesObservabilitySignals();
        System.out.println("SpringDataInterviewCompanion checks passed");
    }

    private static void validatesMethodContract() {
        RepositoryContract create = new RepositoryContract(
                "save", "Order", true, false, true, false);
        RepositoryContract find = new RepositoryContract(
                "findByStatusOrderByCreatedAtDescIdDesc", "Order",
                true, true, false, true);
        RepositoryContract delete = new RepositoryContract(
                "deleteById", "Order", false, false, true, false);

        require(create.pureCommand(), "save must be a command-oriented method");
        require(find.declaresOrdering(), "query name must declare its ordering intent");
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
        Comparator<RecordRow> newestFirst = Comparator
                .comparingInt(RecordRow::createdAt)
                .reversed()
                .thenComparing(RecordRow::id, Comparator.reverseOrder());
        withTieBreaker.sort(newestFirst);

        require(
                byCreatedAtDesc.getFirst().createdAt() == 101,
                "createdAt order still must place largest first");
        require(withTieBreaker.get(0).id() == 9L,
                "primary descending timestamp must remain primary");
        require(withTieBreaker.get(1).id() == 7L,
                "descending id must break equal-timestamp ties");
    }

    private static void validatesPaginationModels() {
        OffsetPage firstOffsetPage = new OffsetPage(0, 20);
        OffsetPage secondOffsetPage = firstOffsetPage.next();
        require(secondOffsetPage.offset() == 20,
                "offset pagination advances by page size");

        CursorPage firstCursorPage = CursorPage.first(20);
        CursorPage nextCursorPage = firstCursorPage.after(100, 3L);
        require(nextCursorPage.afterCreatedAt().orElseThrow() == 100,
                "cursor retains the ordered timestamp");
        require(nextCursorPage.afterId().orElseThrow() == 3L,
                "cursor retains the unique tie-breaker");

        require(!OffsetPage.unbounded().pagingSafe(),
                "unbounded offset work is not safe");
        require(!CursorPage.first(Integer.MAX_VALUE).pagingSafe(),
                "unbounded cursor work is not safe");
    }

    private static void validatesLockRetryPolicy() {
        OptimisticWriteAttempt first = OptimisticWriteAttempt.initial();
        OptimisticWriteAttempt second = first.afterFailure(
                new StaleWriteConflict("version mismatch"));
        OptimisticWriteAttempt third = second.afterFailure(
                new StaleWriteConflict("version mismatch"));
        OptimisticWriteAttempt fourth = third.afterFailure(
                new StaleWriteConflict("version mismatch"));
        OptimisticWriteAttempt permanent = first.afterFailure(
                new PermanentStoreFailure("invalid query"));

        require(
                second.outcome() == AttemptOutcome.RETRYING,
                "first conflict should require refresh and retry");
        require(
                third.outcome() == AttemptOutcome.RETRYING,
                "second conflict should also require retry policy");
        require(
                fourth.outcome() == AttemptOutcome.FAIL_FAST,
                "too many retries should fail fast");
        require(permanent.outcome() == AttemptOutcome.FAIL_FAST,
                "unclassified failures must never be retried blindly");
    }

    private static void validatesStoreBoundaryChoice() {
        StoreDecision sql = chooseStore(new Workload(
                true, true, false, false, false));
        StoreDecision mongo = chooseStore(new Workload(
                true, false, true, true, false));
        StoreDecision redis = chooseStore(new Workload(
                false, false, false, false, true));

        require(sql.choice() == StoreChoice.SQL,
                "relational joins and multi-row invariants point to SQL");
        require(mongo.choice() == StoreChoice.MONGODB,
                "bounded document locality can point to MongoDB");
        require(mongo.caveat().contains("transactions"),
                "MongoDB transaction capability must not be denied categorically");
        require(redis.choice() == StoreChoice.REDIS,
                "derived expiring state can point to Redis");
    }

    private static void validatesObservabilitySignals() {
        var signals = Map.of(
                "queryCount", "4",
                "p50LatencyMs", "45",
                "lockWaitMs", "12",
                "retryCount", "1");

        require(!signals.containsKey("error"), "successful path must not emit error markers");
        require(
                Integer.parseInt(signals.get("queryCount")) <= 6,
                "query count should stay bounded for endpoint contract");

        DiagnosticTimeline timeline = new DiagnosticTimeline(signals);
        List<String> keys = timeline.sortedKeys();
        require(
                keys.get(0).equals("lockWaitMs"),
                "sorted signal output keeps deterministic trace order");
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

        boolean declaresOrdering() {
            String normalized = method.toLowerCase(Locale.ROOT);
            return normalized.contains("find") && normalized.contains("orderby");
        }

        boolean implicitFilter() {
            return normalizedName().contains("by")
                    && !method.contains("All")
                    && mayReturnMultiple;
        }

        boolean pagingSafe() {
            if (!hasPagingIntent) {
                return false;
            }
            return normalizedName().contains("orderbycreatedatdesc")
                    && normalizedName().contains("id");
        }

        private String normalizedName() {
            return method.toLowerCase(Locale.ROOT);
        }
    }

    record RecordRow(long id, String status, int createdAt) {
    }

    record OffsetPage(int offset, int size) {
        OffsetPage {
            if (offset < 0) {
                throw new IllegalArgumentException("offset must be nonnegative");
            }
            if (size <= 0) {
                throw new IllegalArgumentException("size must be positive");
            }
        }

        static OffsetPage unbounded() {
            return new OffsetPage(0, Integer.MAX_VALUE);
        }

        OffsetPage next() {
            return new OffsetPage(Math.addExact(offset, size), size);
        }

        boolean pagingSafe() {
            return size < Integer.MAX_VALUE;
        }
    }

    record CursorPage(int size, Optional<Integer> afterCreatedAt,
                      Optional<Long> afterId) {
        CursorPage {
            if (size <= 0) {
                throw new IllegalArgumentException("size must be positive");
            }
            Objects.requireNonNull(afterCreatedAt, "afterCreatedAt");
            Objects.requireNonNull(afterId, "afterId");
            if (afterCreatedAt.isPresent() != afterId.isPresent()) {
                throw new IllegalArgumentException(
                        "cursor timestamp and tie-breaker must appear together");
            }
        }

        static CursorPage first(int size) {
            return new CursorPage(size, Optional.empty(), Optional.empty());
        }

        CursorPage after(int createdAt, long id) {
            return new CursorPage(size, Optional.of(createdAt), Optional.of(id));
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

    static final class PermanentStoreFailure extends RuntimeException {
        private static final long serialVersionUID = 1L;

        PermanentStoreFailure(String message) {
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

        static OptimisticWriteAttempt initial() {
            return new OptimisticWriteAttempt(0, AttemptOutcome.SUCCESS);
        }

        OptimisticWriteAttempt afterFailure(Throwable cause) {
            Objects.requireNonNull(cause, "cause");
            int failedAttempts = attempts + 1;
            if (!(cause instanceof StaleWriteConflict) || failedAttempts >= 3) {
                return new OptimisticWriteAttempt(failedAttempts,
                        AttemptOutcome.FAIL_FAST);
            }
            return new OptimisticWriteAttempt(failedAttempts,
                    AttemptOutcome.RETRYING);
        }

        AttemptOutcome outcome() {
            return outcome;
        }
    }

    enum StoreChoice {
        SQL,
        MONGODB,
        REDIS,
        NEEDS_MORE_EVIDENCE
    }

    record Workload(boolean sourceOfTruth,
                    boolean relationalJoins,
                    boolean documentLocality,
                    boolean boundedDocument,
                    boolean expiringDerivedState) {
    }

    record StoreDecision(StoreChoice choice, String reason, String caveat) {
    }

    static StoreDecision chooseStore(Workload workload) {
        Objects.requireNonNull(workload, "workload");
        if (workload.sourceOfTruth() && workload.relationalJoins()) {
            return new StoreDecision(StoreChoice.SQL,
                    "relational constraints and join-shaped access",
                    "verify isolation, indexes, and engine behavior");
        }
        if (workload.sourceOfTruth() && workload.documentLocality()
                && workload.boundedDocument()) {
            return new StoreDecision(StoreChoice.MONGODB,
                    "bounded aggregate reads and document locality",
                    "multi-document transactions exist on supported deployments, "
                            + "but frequent cross-document invariants may signal a poor model");
        }
        if (!workload.sourceOfTruth() && workload.expiringDerivedState()) {
            return new StoreDecision(StoreChoice.REDIS,
                    "derived state with an explicit expiration contract",
                    "define source of truth, eviction, failover, and stale-data policy");
        }
        return new StoreDecision(StoreChoice.NEEDS_MORE_EVIDENCE,
                "requirements do not select a store",
                "measure query shape, consistency, growth, and operations");
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
