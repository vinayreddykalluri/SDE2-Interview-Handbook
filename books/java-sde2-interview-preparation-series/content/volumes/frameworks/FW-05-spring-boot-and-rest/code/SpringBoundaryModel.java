import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HexFormat;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

/**
 * Dependency-free Java 21 models for API-boundary interview exercises.
 *
 * <p>This is not a distributed idempotency store. It makes the state and
 * invariants executable; production code must use durable atomic storage.
 */
public final class SpringBoundaryModel {
    private SpringBoundaryModel() {
    }

    public record ScopedKey(String tenant, String operation, String key) {
        public ScopedKey {
            requireText(tenant, "tenant");
            requireText(operation, "operation");
            requireText(key, "key");
            if (key.length() > 128) {
                throw new IllegalArgumentException("key is too long");
            }
        }
    }

    public record Request(String customerId, List<Line> lines, String note) {
        public Request {
            requireText(customerId, "customerId");
            lines = List.copyOf(Objects.requireNonNull(lines, "lines"));
            if (lines.isEmpty() || lines.size() > 100) {
                throw new IllegalArgumentException("line count must be 1..100");
            }
            note = note == null ? "" : note;
            if (note.length() > 200) {
                throw new IllegalArgumentException("note is too long");
            }
        }

        public String canonicalForm() {
            var sorted = new ArrayList<>(lines);
            sorted.sort(Comparator.comparing(Line::sku)
                    .thenComparingInt(Line::quantity));
            var builder = new StringBuilder(customerId).append('|');
            for (Line line : sorted) {
                appendLengthPrefixed(builder, line.sku());
                builder.append(':').append(line.quantity()).append('|');
            }
            appendLengthPrefixed(builder, note);
            return builder.toString();
        }
    }

    public record Line(String sku, int quantity) {
        public Line {
            requireText(sku, "sku");
            if (quantity < 1 || quantity > 10_000) {
                throw new IllegalArgumentException("quantity must be 1..10000");
            }
        }
    }

    public sealed interface ReservationResult
            permits Owner, Replay, InProgress, PayloadConflict {
    }

    public record Owner(long attemptToken) implements ReservationResult {
    }

    public record Replay(String resourceId) implements ReservationResult {
        public Replay {
            requireText(resourceId, "resourceId");
        }
    }

    public record InProgress(Instant retryAfter) implements ReservationResult {
        public InProgress {
            Objects.requireNonNull(retryAfter, "retryAfter");
        }
    }

    public record PayloadConflict(String expectedFingerprint)
            implements ReservationResult {
    }

    private enum State { IN_PROGRESS, SUCCEEDED }

    private static final class Entry {
        private final String fingerprint;
        private final long attemptToken;
        private final Instant leaseUntil;
        private final Instant expiresAt;
        private State state = State.IN_PROGRESS;
        private String resourceId;

        private Entry(String fingerprint, long attemptToken,
                Instant leaseUntil, Instant expiresAt) {
            this.fingerprint = fingerprint;
            this.attemptToken = attemptToken;
            this.leaseUntil = leaseUntil;
            this.expiresAt = expiresAt;
        }
    }

    /** Thread-safe only within one process; used to exercise the contract. */
    public static final class IdempotencyRegistry {
        private final Map<ScopedKey, Entry> entries = new HashMap<>();
        private final Clock clock;
        private final Duration lease;
        private final Duration retention;
        private long nextAttemptToken = 1;

        public IdempotencyRegistry(
                Clock clock, Duration lease, Duration retention) {
            this.clock = Objects.requireNonNull(clock, "clock");
            this.lease = requirePositive(lease, "lease");
            this.retention = requirePositive(retention, "retention");
            if (retention.compareTo(lease) < 0) {
                throw new IllegalArgumentException(
                        "retention must not be shorter than lease");
            }
        }

        public synchronized ReservationResult reserve(
                ScopedKey key, String fingerprint) {
            Objects.requireNonNull(key, "key");
            requireText(fingerprint, "fingerprint");
            Instant now = clock.instant();
            Entry current = entries.get(key);
            if (current != null && !current.expiresAt.isAfter(now)) {
                entries.remove(key);
                current = null;
            }
            if (current == null) {
                long token = nextAttemptToken++;
                entries.put(key, new Entry(fingerprint, token,
                        now.plus(lease), now.plus(retention)));
                return new Owner(token);
            }
            if (!MessageDigest.isEqual(
                    current.fingerprint.getBytes(StandardCharsets.UTF_8),
                    fingerprint.getBytes(StandardCharsets.UTF_8))) {
                return new PayloadConflict(current.fingerprint);
            }
            if (current.state == State.SUCCEEDED) {
                return new Replay(current.resourceId);
            }
            return new InProgress(current.leaseUntil);
        }

        public synchronized void complete(
                ScopedKey key, long attemptToken, String resourceId) {
            Entry current = entries.get(Objects.requireNonNull(key, "key"));
            if (current == null || current.attemptToken != attemptToken
                    || current.state != State.IN_PROGRESS) {
                throw new IllegalStateException("attempt does not own reservation");
            }
            requireText(resourceId, "resourceId");
            current.resourceId = resourceId;
            current.state = State.SUCCEEDED;
        }

        public synchronized int size() {
            return entries.size();
        }
    }

    public record OrderView(String id, Instant createdAt) {
        public OrderView {
            requireText(id, "id");
            Objects.requireNonNull(createdAt, "createdAt");
        }
    }

    public record Cursor(Instant createdAt, String id) {
        public Cursor {
            Objects.requireNonNull(createdAt, "createdAt");
            requireText(id, "id");
        }
    }

    /**
     * Returns a descending keyset page. Input is sorted defensively so the
     * executable model does not hide the required total ordering.
     */
    public static List<OrderView> pageAfter(
            List<OrderView> source, Cursor after, int limit) {
        if (limit < 1 || limit > 100) {
            throw new IllegalArgumentException("limit must be 1..100");
        }
        Comparator<OrderView> newestFirst = Comparator
                .comparing(OrderView::createdAt).reversed()
                .thenComparing(OrderView::id, Comparator.reverseOrder());
        return source.stream()
                .sorted(newestFirst)
                .filter(order -> after == null
                        || order.createdAt().isBefore(after.createdAt())
                        || (order.createdAt().equals(after.createdAt())
                            && order.id().compareTo(after.id()) < 0))
                .limit(limit)
                .toList();
    }

    public static String strongEtag(String resourceId, long version) {
        requireText(resourceId, "resourceId");
        if (version < 0) {
            throw new IllegalArgumentException("version must be nonnegative");
        }
        String token = resourceId + "-v" + version;
        String safe = Base64.getUrlEncoder().withoutPadding()
                .encodeToString(token.getBytes(StandardCharsets.UTF_8));
        return '"' + safe + '"';
    }

    public static String fingerprint(Request request) {
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256")
                    .digest(request.canonicalForm()
                            .getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(digest);
        } catch (NoSuchAlgorithmException impossible) {
            throw new AssertionError("Java requires SHA-256", impossible);
        }
    }

    public enum WriteOutcome {
        NOT_STARTED,
        COMMITTED,
        UNKNOWN
    }

    public enum RecoveryAction {
        RETRY_SAME_KEY,
        REPLAY_RECORDED_RESULT,
        RECONCILE_SAME_KEY
    }

    /**
     * A transport failure never authorizes a fresh logical request. The same
     * scoped idempotency key is used for retry, replay, or reconciliation.
     */
    public static RecoveryAction recoveryAction(WriteOutcome outcome) {
        Objects.requireNonNull(outcome, "outcome");
        return switch (outcome) {
            case NOT_STARTED -> RecoveryAction.RETRY_SAME_KEY;
            case COMMITTED -> RecoveryAction.REPLAY_RECORDED_RESULT;
            case UNKNOWN -> RecoveryAction.RECONCILE_SAME_KEY;
        };
    }

    public record Deadline(Instant expiresAt) {
        public Deadline {
            Objects.requireNonNull(expiresAt, "expiresAt");
        }

        public Duration remainingAt(Instant now) {
            Objects.requireNonNull(now, "now");
            if (!now.isBefore(expiresAt)) {
                return Duration.ZERO;
            }
            return Duration.between(now, expiresAt);
        }

        public boolean canStart(Instant now, Duration worstCaseAttempt) {
            requirePositive(worstCaseAttempt, "worstCaseAttempt");
            return remainingAt(now).compareTo(worstCaseAttempt) >= 0;
        }
    }

    public static void main(String[] args) {
        Clock fixed = Clock.fixed(
                Instant.parse("2026-01-01T00:00:00Z"), ZoneOffset.UTC);
        var registry = new IdempotencyRegistry(
                fixed, Duration.ofSeconds(30), Duration.ofHours(24));
        var key = new ScopedKey("tenant-1", "create-order", "request-7");
        var request = new Request("customer-1",
                List.of(new Line("BOOK-21", 2)), "reception");
        String hash = fingerprint(request);

        ReservationResult first = registry.reserve(key, hash);
        assert first instanceof Owner;
        long token = ((Owner) first).attemptToken();

        assert registry.reserve(key, hash) instanceof InProgress;
        assert registry.reserve(key, fingerprint(new Request("customer-1",
                List.of(new Line("BOOK-21", 3)), "reception")))
                instanceof PayloadConflict;

        registry.complete(key, token, "order-91");
        ReservationResult replay = registry.reserve(key, hash);
        assert replay.equals(new Replay("order-91"));
        assert registry.size() == 1;

        Instant t = Instant.parse("2026-01-01T10:00:00Z");
        List<OrderView> orders = List.of(
                new OrderView("O-3", t),
                new OrderView("O-2", t),
                new OrderView("O-1", t.minusSeconds(1)));
        assert pageAfter(orders, null, 2)
                .equals(List.of(orders.get(0), orders.get(1)));
        assert pageAfter(orders, new Cursor(t, "O-2"), 2)
                .equals(List.of(orders.get(2)));

        assert strongEtag("O-91", 8).startsWith("\"");
        assert strongEtag("O-91", 8).endsWith("\"");

        assert recoveryAction(WriteOutcome.NOT_STARTED)
                == RecoveryAction.RETRY_SAME_KEY;
        assert recoveryAction(WriteOutcome.COMMITTED)
                == RecoveryAction.REPLAY_RECORDED_RESULT;
        assert recoveryAction(WriteOutcome.UNKNOWN)
                == RecoveryAction.RECONCILE_SAME_KEY;

        Instant now = Instant.parse("2026-01-01T00:00:00Z");
        Deadline deadline = new Deadline(now.plusSeconds(2));
        assert deadline.remainingAt(now).equals(Duration.ofSeconds(2));
        assert deadline.canStart(now, Duration.ofMillis(1_500));
        assert !deadline.canStart(now.plusMillis(1_600),
                Duration.ofMillis(500));
        System.out.println("SpringBoundaryModel assertions passed");
    }

    private static void appendLengthPrefixed(
            StringBuilder builder, String value) {
        builder.append(value.length()).append(':').append(value);
    }

    private static void requireText(String value, String name) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(name + " must not be blank");
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
