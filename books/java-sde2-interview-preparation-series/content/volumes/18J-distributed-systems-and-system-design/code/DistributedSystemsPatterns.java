import java.time.Duration;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.OptionalLong;
import java.util.Set;
import java.util.SplittableRandom;
import java.util.concurrent.TimeUnit;

/**
 * Dependency-free Java 21 executable models for capacity, token-bucket
 * admission, retry budgets, Kafka-style contiguous offsets, and saga state.
 */
public final class DistributedSystemsPatterns {
    private DistributedSystemsPatterns() {
    }

    public record CapacityEstimate(
            double averageRps,
            double peakRps,
            double concurrentDependencyCalls,
            double gibPerDay,
            double replicatedTibPerYear) {
    }

    public static CapacityEstimate estimate(
            long requestsPerDay,
            double peakFactor,
            double dependencyFraction,
            Duration dependencyLatency,
            long eventsPerDay,
            long bytesPerEvent,
            int replicationFactor) {
        if (requestsPerDay < 0 || eventsPerDay < 0 || bytesPerEvent < 0) {
            throw new IllegalArgumentException("counts must be nonnegative");
        }
        requirePositiveFinite(peakFactor, "peakFactor");
        if (dependencyFraction < 0 || dependencyFraction > 1) {
            throw new IllegalArgumentException(
                    "dependencyFraction must be in [0,1]");
        }
        Objects.requireNonNull(dependencyLatency, "dependencyLatency");
        if (dependencyLatency.isNegative()) {
            throw new IllegalArgumentException("negative dependency latency");
        }
        if (replicationFactor < 1) {
            throw new IllegalArgumentException("replicationFactor must be >= 1");
        }

        double averageRps = requestsPerDay / 86_400.0;
        double peakRps = averageRps * peakFactor;
        double seconds = dependencyLatency.toNanos() / 1_000_000_000.0;
        double concurrent = peakRps * dependencyFraction * seconds;
        double bytesPerDay = Math.multiplyExact(eventsPerDay, bytesPerEvent);
        double gibPerDay = bytesPerDay / Math.pow(1024.0, 3);
        double replicatedTibPerYear = bytesPerDay * 365.0
                * replicationFactor / Math.pow(1024.0, 4);
        return new CapacityEstimate(averageRps, peakRps, concurrent,
                gibPerDay, replicatedTibPerYear);
    }

    /** Monotonic-time token bucket supporting fractional refill. */
    public static final class TokenBucket {
        private final double capacity;
        private final double refillPerNano;
        private double tokens;
        private long lastNanos;

        public TokenBucket(double capacity, double refillPerSecond,
                long initialNanos) {
            requirePositiveFinite(capacity, "capacity");
            requirePositiveFinite(refillPerSecond, "refillPerSecond");
            this.capacity = capacity;
            this.refillPerNano = refillPerSecond / 1_000_000_000.0;
            this.tokens = capacity;
            this.lastNanos = initialNanos;
        }

        public synchronized boolean tryAcquire(double cost, long nowNanos) {
            requirePositiveFinite(cost, "cost");
            if (cost > capacity) {
                return false;
            }
            refill(nowNanos);
            if (tokens + 1e-12 < cost) {
                return false;
            }
            tokens -= cost;
            return true;
        }

        public synchronized double available(long nowNanos) {
            refill(nowNanos);
            return tokens;
        }

        private void refill(long nowNanos) {
            if (nowNanos < lastNanos) {
                throw new IllegalArgumentException("monotonic time moved backward");
            }
            long elapsed = nowNanos - lastNanos;
            tokens = Math.min(capacity, tokens + elapsed * refillPerNano);
            lastNanos = nowNanos;
        }
    }

    public record RetryAttempt(int number, Duration delay,
                               Duration remainingAfterDelay) {
    }

    /**
     * Builds bounded full-jitter delays. Processing time is charged before
     * each optional retry; no delay is returned once the deadline is spent.
     */
    public static List<RetryAttempt> retryPlan(
            int maxAttempts,
            Duration totalBudget,
            Duration perAttemptCost,
            Duration baseBackoff,
            Duration maxBackoff,
            long randomSeed) {
        if (maxAttempts < 1) {
            throw new IllegalArgumentException("maxAttempts must be >= 1");
        }
        requirePositive(totalBudget, "totalBudget");
        requirePositive(perAttemptCost, "perAttemptCost");
        requirePositive(baseBackoff, "baseBackoff");
        requirePositive(maxBackoff, "maxBackoff");

        long remaining = totalBudget.toNanos();
        long attemptCost = perAttemptCost.toNanos();
        long base = baseBackoff.toNanos();
        long max = maxBackoff.toNanos();
        var random = new SplittableRandom(randomSeed);
        var plan = new ArrayList<RetryAttempt>();

        for (int attempt = 1; attempt <= maxAttempts; attempt++) {
            if (remaining < attemptCost) {
                break;
            }
            remaining -= attemptCost;
            if (attempt == maxAttempts) {
                break;
            }
            int shift = Math.min(attempt - 1, 62);
            long exponential;
            try {
                exponential = Math.multiplyExact(base, 1L << shift);
            } catch (ArithmeticException overflow) {
                exponential = Long.MAX_VALUE;
            }
            long cap = Math.min(max, exponential);
            long delay = cap == Long.MAX_VALUE
                    ? random.nextLong(Long.MAX_VALUE)
                    : random.nextLong(cap + 1);
            if (delay >= remaining) {
                break;
            }
            remaining -= delay;
            plan.add(new RetryAttempt(attempt + 1,
                    Duration.ofNanos(delay), Duration.ofNanos(remaining)));
        }
        return List.copyOf(plan);
    }

    /**
     * Tracks completed offsets and exposes the next safe commit offset: the
     * first offset not yet completed. Useful when processing finishes out of
     * order while commits must advance only across a contiguous prefix.
     */
    public static final class ContiguousOffsetTracker {
        private long nextCommitOffset;
        private final Set<Long> completedOutOfOrder = new HashSet<>();

        public ContiguousOffsetTracker(long startingOffset) {
            if (startingOffset < 0) {
                throw new IllegalArgumentException("negative starting offset");
            }
            this.nextCommitOffset = startingOffset;
        }

        public synchronized OptionalLong complete(long offset) {
            if (offset < nextCommitOffset) {
                return OptionalLong.empty(); // duplicate completion
            }
            completedOutOfOrder.add(offset);
            long before = nextCommitOffset;
            while (completedOutOfOrder.remove(nextCommitOffset)) {
                nextCommitOffset++;
            }
            return nextCommitOffset == before
                    ? OptionalLong.empty()
                    : OptionalLong.of(nextCommitOffset);
        }

        public synchronized long nextCommitOffset() {
            return nextCommitOffset;
        }

        public synchronized Set<Long> gapsSnapshot() {
            return Collections.unmodifiableSet(
                    new HashSet<>(completedOutOfOrder));
        }
    }

    public enum SagaState {
        PENDING,
        INVENTORY_RESERVED,
        PAYMENT_UNKNOWN,
        PAYMENT_AUTHORIZED,
        CONFIRMED,
        COMPENSATING,
        CANCELLED,
        FAILED_RECONCILIATION
    }

    public enum SagaEvent {
        INVENTORY_RESERVED,
        PAYMENT_TIMED_OUT,
        PAYMENT_CONFIRMED,
        CONFIRM_ORDER,
        CANCEL_REQUESTED,
        COMPENSATION_COMPLETED,
        COMPENSATION_FAILED
    }

    public static SagaState transition(SagaState state, SagaEvent event) {
        Objects.requireNonNull(state, "state");
        Objects.requireNonNull(event, "event");
        return switch (state) {
            case PENDING -> switch (event) {
                case INVENTORY_RESERVED -> SagaState.INVENTORY_RESERVED;
                case CANCEL_REQUESTED -> SagaState.CANCELLED;
                default -> invalid(state, event);
            };
            case INVENTORY_RESERVED -> switch (event) {
                case PAYMENT_TIMED_OUT -> SagaState.PAYMENT_UNKNOWN;
                case PAYMENT_CONFIRMED -> SagaState.PAYMENT_AUTHORIZED;
                case CANCEL_REQUESTED -> SagaState.COMPENSATING;
                default -> invalid(state, event);
            };
            case PAYMENT_UNKNOWN -> switch (event) {
                case PAYMENT_CONFIRMED -> SagaState.PAYMENT_AUTHORIZED;
                case CANCEL_REQUESTED -> SagaState.COMPENSATING;
                default -> invalid(state, event);
            };
            case PAYMENT_AUTHORIZED -> switch (event) {
                case CONFIRM_ORDER -> SagaState.CONFIRMED;
                case CANCEL_REQUESTED -> SagaState.COMPENSATING;
                default -> invalid(state, event);
            };
            case COMPENSATING -> switch (event) {
                case COMPENSATION_COMPLETED -> SagaState.CANCELLED;
                case COMPENSATION_FAILED -> SagaState.FAILED_RECONCILIATION;
                default -> invalid(state, event);
            };
            case CONFIRMED, CANCELLED, FAILED_RECONCILIATION ->
                    invalid(state, event);
        };
    }

    public static boolean quorumSetsMustIntersect(int replicaCount,
            int firstAcknowledgements, int secondAcknowledgements) {
        if (replicaCount < 1
                || firstAcknowledgements < 0
                || secondAcknowledgements < 0
                || firstAcknowledgements > replicaCount
                || secondAcknowledgements > replicaCount) {
            throw new IllegalArgumentException("invalid quorum sizes");
        }
        return firstAcknowledgements + secondAcknowledgements > replicaCount;
    }

    public static double errorBudgetEvents(
            long eligibleEvents, double sloTarget) {
        if (eligibleEvents < 0 || !Double.isFinite(sloTarget)
                || sloTarget < 0 || sloTarget > 1) {
            throw new IllegalArgumentException("invalid SLO inputs");
        }
        return eligibleEvents * (1.0 - sloTarget);
    }

    public static double burnRate(
            long badEvents, long eligibleEvents, double sloTarget) {
        if (badEvents < 0 || eligibleEvents <= 0
                || badEvents > eligibleEvents
                || !Double.isFinite(sloTarget)
                || sloTarget < 0 || sloTarget >= 1) {
            throw new IllegalArgumentException("invalid burn-rate inputs");
        }
        double observedBad = badEvents / (double) eligibleEvents;
        return observedBad / (1.0 - sloTarget);
    }

    public static void main(String[] args) {
        capacityAssertions();
        tokenBucketAssertions();
        retryAssertions();
        offsetAssertions();
        sagaAssertions();
        quorumAndSloAssertions();
        System.out.println("DistributedSystemsPatterns assertions passed");
    }

    private static void capacityAssertions() {
        CapacityEstimate estimate = estimate(
                50_000_000, 8, 0.30, Duration.ofMillis(40),
                10_000_000, 1_536, 3);
        assert Math.abs(estimate.averageRps() - 578.7037) < 0.001;
        assert Math.abs(estimate.peakRps() - 4_629.6296) < 0.01;
        assert Math.abs(estimate.concurrentDependencyCalls() - 55.5555) < 0.01;
        assert estimate.replicatedTibPerYear() > 15;
    }

    private static void tokenBucketAssertions() {
        long start = 1_000_000_000L;
        var bucket = new TokenBucket(3, 2, start);
        assert bucket.tryAcquire(1, start);
        assert bucket.tryAcquire(2, start);
        assert !bucket.tryAcquire(1, start);
        long halfSecondLater = start + TimeUnit.MILLISECONDS.toNanos(500);
        assert bucket.tryAcquire(1, halfSecondLater);
        assert Math.abs(bucket.available(halfSecondLater)) < 1e-9;
        long farLater = start + TimeUnit.SECONDS.toNanos(10);
        assert Math.abs(bucket.available(farLater) - 3) < 1e-9;
    }

    private static void retryAssertions() {
        List<RetryAttempt> plan = retryPlan(
                4, Duration.ofMillis(500), Duration.ofMillis(80),
                Duration.ofMillis(20), Duration.ofMillis(100), 7);
        assert !plan.isEmpty();
        assert plan.size() <= 3;
        Duration previousRemaining = Duration.ofMillis(500);
        for (RetryAttempt retry : plan) {
            assert retry.number() >= 2 && retry.number() <= 4;
            assert !retry.delay().isNegative();
            assert retry.remainingAfterDelay().compareTo(previousRemaining) < 0;
            previousRemaining = retry.remainingAfterDelay();
        }
    }

    private static void offsetAssertions() {
        var tracker = new ContiguousOffsetTracker(10);
        assert tracker.complete(11).isEmpty();
        assert tracker.nextCommitOffset() == 10;
        assert tracker.gapsSnapshot().equals(Set.of(11L));
        assert tracker.complete(10).orElseThrow() == 12;
        assert tracker.complete(10).isEmpty();
        assert tracker.complete(13).isEmpty();
        assert tracker.complete(12).orElseThrow() == 14;
    }

    private static void sagaAssertions() {
        SagaState state = SagaState.PENDING;
        state = transition(state, SagaEvent.INVENTORY_RESERVED);
        state = transition(state, SagaEvent.PAYMENT_TIMED_OUT);
        assert state == SagaState.PAYMENT_UNKNOWN;
        state = transition(state, SagaEvent.PAYMENT_CONFIRMED);
        state = transition(state, SagaEvent.CANCEL_REQUESTED);
        state = transition(state, SagaEvent.COMPENSATION_COMPLETED);
        assert state == SagaState.CANCELLED;

        try {
            transition(SagaState.CONFIRMED, SagaEvent.CANCEL_REQUESTED);
            throw new AssertionError("terminal state accepted transition");
        } catch (IllegalStateException expected) {
            assert expected.getMessage().contains("CONFIRMED");
        }
    }

    private static void quorumAndSloAssertions() {
        assert quorumSetsMustIntersect(3, 2, 2);
        assert !quorumSetsMustIntersect(3, 1, 2);
        assert Math.abs(errorBudgetEvents(1_000_000, 0.999) - 1_000) < 1e-6;
        assert Math.abs(burnRate(1_000, 100_000, 0.999) - 10.0) < 1e-8;
    }

    private static SagaState invalid(SagaState state, SagaEvent event) {
        throw new IllegalStateException(
                "event " + event + " is invalid from " + state);
    }

    private static void requirePositiveFinite(double value, String name) {
        if (!Double.isFinite(value) || value <= 0) {
            throw new IllegalArgumentException(name + " must be positive");
        }
    }

    private static Duration requirePositive(Duration value, String name) {
        Objects.requireNonNull(value, name);
        if (value.isZero() || value.isNegative()) {
            throw new IllegalArgumentException(name + " must be positive");
        }
        return value;
    }
}
