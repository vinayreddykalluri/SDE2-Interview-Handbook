import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Dependency-free Java 21 executable models for optimistic updates, keyset
 * pagination, and a version-aware cache-aside read path.
 */
public final class PersistencePatterns {
    private PersistencePatterns() {
    }

    public record Order(String id, long tenantId, Instant createdAt,
                        String status, long version, String note) {
        public Order {
            requireText(id, "id");
            Objects.requireNonNull(createdAt, "createdAt");
            requireText(status, "status");
            note = note == null ? "" : note;
            if (version < 0) {
                throw new IllegalArgumentException("version must be nonnegative");
            }
        }

        Order withNote(String newNote) {
            return new Order(id, tenantId, createdAt, status,
                    Math.addExact(version, 1), newNote);
        }
    }

    public record Cursor(Instant createdAt, String id) {
        public Cursor {
            Objects.requireNonNull(createdAt, "createdAt");
            requireText(id, "id");
        }
    }

    public sealed interface UpdateResult
            permits Updated, Conflict, NotFound {
    }

    public record Updated(Order order) implements UpdateResult {
    }

    public record Conflict(long currentVersion) implements UpdateResult {
    }

    public record NotFound() implements UpdateResult {
    }

    /** In-memory stand-in for one atomic database UPDATE predicate. */
    public static final class OptimisticOrderStore {
        private final Map<String, Order> rows = new HashMap<>();

        public synchronized void insert(Order order) {
            Objects.requireNonNull(order, "order");
            if (rows.putIfAbsent(order.id(), order) != null) {
                throw new IllegalStateException("duplicate order id");
            }
        }

        public synchronized Optional<Order> find(long tenantId, String id) {
            Order order = rows.get(id);
            return order != null && order.tenantId() == tenantId
                    ? Optional.of(order) : Optional.empty();
        }

        public synchronized UpdateResult updateNote(
                long tenantId, String id, long expectedVersion, String note) {
            if (note == null || note.length() > 200) {
                throw new IllegalArgumentException("invalid note");
            }
            Order current = rows.get(id);
            if (current == null || current.tenantId() != tenantId) {
                return new NotFound();
            }
            if (current.version() != expectedVersion) {
                return new Conflict(current.version());
            }
            Order changed = current.withNote(note);
            rows.put(id, changed);
            return new Updated(changed);
        }

        public synchronized List<Order> page(
                long tenantId, Cursor after, int limit) {
            if (limit < 1 || limit > 100) {
                throw new IllegalArgumentException("limit must be 1..100");
            }
            Comparator<Order> newestFirst = Comparator
                    .comparing(Order::createdAt).reversed()
                    .thenComparing(Order::id, Comparator.reverseOrder());
            return rows.values().stream()
                    .filter(order -> order.tenantId() == tenantId)
                    .filter(order -> after == null
                            || order.createdAt().isBefore(after.createdAt())
                            || (order.createdAt().equals(after.createdAt())
                                && order.id().compareTo(after.id()) < 0))
                    .sorted(newestFirst)
                    .limit(limit)
                    .toList();
        }
    }

    public record ProductView(String id, long sourceVersion,
                              String description, long priceMinor) {
        public ProductView {
            requireText(id, "id");
            requireText(description, "description");
            if (sourceVersion < 0 || priceMinor < 0) {
                throw new IllegalArgumentException("negative version/price");
            }
        }
    }

    public interface ProductSource {
        ProductView require(String id);
    }

    private record CacheEntry(ProductView value, Instant expiresAt) {
    }

    /**
     * One-process cache model. Production Redis calls require strict deadlines,
     * serialization versions, source protection, and distributed failure policy.
     */
    public static final class VersionAwareCache {
        private final Map<String, CacheEntry> values = new HashMap<>();
        private final Clock clock;

        public VersionAwareCache(Clock clock) {
            this.clock = Objects.requireNonNull(clock, "clock");
        }

        public synchronized Optional<ProductView> get(String key) {
            CacheEntry entry = values.get(key);
            if (entry == null) {
                return Optional.empty();
            }
            if (!entry.expiresAt().isAfter(clock.instant())) {
                values.remove(key);
                return Optional.empty();
            }
            return Optional.of(entry.value());
        }

        /** An older invalidation must not remove a newer filled value. */
        public synchronized boolean invalidateIfNotNewer(
                String key, long sourceVersion) {
            CacheEntry current = values.get(key);
            if (current == null
                    || current.value().sourceVersion() <= sourceVersion) {
                values.remove(key);
                return current != null;
            }
            return false;
        }

        /** An out-of-order older fill must not replace a newer value. */
        public synchronized boolean putIfNewerOrEqual(
                String key, ProductView value, Duration ttl) {
            Objects.requireNonNull(value, "value");
            requirePositive(ttl, "ttl");
            CacheEntry current = values.get(key);
            if (current != null
                    && current.value().sourceVersion() > value.sourceVersion()) {
                return false;
            }
            values.put(key, new CacheEntry(value, clock.instant().plus(ttl)));
            return true;
        }
    }

    public static final class CachedProducts {
        private final ProductSource source;
        private final VersionAwareCache cache;
        private final Duration ttl;

        public CachedProducts(
                ProductSource source, VersionAwareCache cache, Duration ttl) {
            this.source = Objects.requireNonNull(source, "source");
            this.cache = Objects.requireNonNull(cache, "cache");
            this.ttl = requirePositive(ttl, "ttl");
        }

        public ProductView get(String id) {
            String key = cacheKey(id);
            Optional<ProductView> hit = cache.get(key);
            if (hit.isPresent()) {
                return hit.get();
            }
            ProductView loaded = source.require(id);
            cache.putIfNewerOrEqual(key, loaded, ttl);
            return cache.get(key).orElse(loaded);
        }

        public void onProductChanged(String id, long sourceVersion) {
            cache.invalidateIfNotNewer(cacheKey(id), sourceVersion);
        }

        private static String cacheKey(String id) {
            requireText(id, "id");
            return "product:v1:" + id;
        }
    }

    public record OutboxClaim(String eventId, long claimToken) {
        public OutboxClaim {
            requireText(eventId, "eventId");
            if (claimToken <= 0) {
                throw new IllegalArgumentException(
                        "claimToken must be positive");
            }
        }
    }

    private enum OutboxState { NEW, CLAIMED, PUBLISHED }

    private static final class OutboxEntry {
        private final long aggregateVersion;
        private OutboxState state = OutboxState.NEW;
        private long claimToken;

        private OutboxEntry(long aggregateVersion) {
            this.aggregateVersion = aggregateVersion;
        }
    }

    /**
     * In-memory model of durable outbox claim fencing. A stale relay attempt
     * cannot mark a row published after another attempt reclaims it.
     */
    public static final class OutboxStore {
        private final Map<String, OutboxEntry> events = new HashMap<>();
        private long nextClaimToken = 1;

        public synchronized boolean insert(
                String eventId, long aggregateVersion) {
            requireText(eventId, "eventId");
            if (aggregateVersion < 0) {
                throw new IllegalArgumentException(
                        "aggregateVersion must be nonnegative");
            }
            return events.putIfAbsent(
                    eventId, new OutboxEntry(aggregateVersion)) == null;
        }

        public synchronized Optional<OutboxClaim> claim(String eventId) {
            OutboxEntry entry = events.get(eventId);
            if (entry == null || entry.state != OutboxState.NEW) {
                return Optional.empty();
            }
            entry.state = OutboxState.CLAIMED;
            entry.claimToken = nextClaimToken++;
            return Optional.of(new OutboxClaim(eventId, entry.claimToken));
        }

        public synchronized boolean release(OutboxClaim claim) {
            OutboxEntry entry = events.get(claim.eventId());
            if (!owns(entry, claim)) {
                return false;
            }
            entry.state = OutboxState.NEW;
            return true;
        }

        public synchronized boolean markPublished(OutboxClaim claim) {
            OutboxEntry entry = events.get(claim.eventId());
            if (!owns(entry, claim)) {
                return false;
            }
            entry.state = OutboxState.PUBLISHED;
            return true;
        }

        public synchronized boolean isPublished(String eventId) {
            OutboxEntry entry = events.get(eventId);
            return entry != null && entry.state == OutboxState.PUBLISHED;
        }

        public synchronized long aggregateVersion(String eventId) {
            OutboxEntry entry = events.get(eventId);
            if (entry == null) {
                throw new IllegalArgumentException("unknown event");
            }
            return entry.aggregateVersion;
        }

        private static boolean owns(
                OutboxEntry entry, OutboxClaim claim) {
            return entry != null
                    && entry.state == OutboxState.CLAIMED
                    && entry.claimToken == claim.claimToken();
        }
    }

    public static void main(String[] args) {
        optimisticUpdateAssertions();
        keysetAssertions();
        cacheAssertions();
        outboxAssertions();
        System.out.println("PersistencePatterns assertions passed");
    }

    private static void optimisticUpdateAssertions() {
        var store = new OptimisticOrderStore();
        Instant now = Instant.parse("2026-01-01T00:00:00Z");
        store.insert(new Order("O-1", 7, now, "PENDING", 0, ""));

        UpdateResult first = store.updateNote(7, "O-1", 0, "door");
        assert first instanceof Updated;
        assert ((Updated) first).order().version() == 1;

        UpdateResult stale = store.updateNote(7, "O-1", 0, "stale");
        assert stale.equals(new Conflict(1));
        assert store.updateNote(8, "O-1", 1, "cross-tenant")
                instanceof NotFound;
    }

    private static void keysetAssertions() {
        var store = new OptimisticOrderStore();
        Instant t = Instant.parse("2026-01-01T10:00:00Z");
        Order newest = new Order("O-3", 7, t, "NEW", 0, "");
        Order tieSecond = new Order("O-2", 7, t, "NEW", 0, "");
        Order older = new Order("O-1", 7, t.minusSeconds(1), "NEW", 0, "");
        store.insert(older);
        store.insert(tieSecond);
        store.insert(newest);
        store.insert(new Order("OTHER", 8, t.plusSeconds(3), "NEW", 0, ""));

        assert store.page(7, null, 2).equals(List.of(newest, tieSecond));
        assert store.page(7, new Cursor(t, "O-2"), 2)
                .equals(List.of(older));
    }

    private static void cacheAssertions() {
        Clock fixed = Clock.fixed(
                Instant.parse("2026-01-01T00:00:00Z"), ZoneOffset.UTC);
        var cache = new VersionAwareCache(fixed);
        var calls = new AtomicInteger();
        ProductView version8 = new ProductView("P-1", 8, "Book", 1_200);
        ProductSource source = id -> {
            calls.incrementAndGet();
            assert id.equals("P-1");
            return version8;
        };
        var products = new CachedProducts(
                source, cache, Duration.ofSeconds(10));

        assert products.get("P-1").equals(version8);
        assert products.get("P-1").equals(version8);
        assert calls.get() == 1;

        products.onProductChanged("P-1", 7); // older event cannot remove v8
        assert products.get("P-1").equals(version8);
        assert calls.get() == 1;

        products.onProductChanged("P-1", 8);
        assert products.get("P-1").equals(version8);
        assert calls.get() == 2;

        boolean acceptedOlder = cache.putIfNewerOrEqual(
                "product:v1:P-1",
                new ProductView("P-1", 7, "Old", 1_000),
                Duration.ofSeconds(10));
        assert !acceptedOlder;
    }

    private static void outboxAssertions() {
        var outbox = new OutboxStore();
        assert outbox.insert("event-8", 8);
        assert !outbox.insert("event-8", 8);
        assert outbox.aggregateVersion("event-8") == 8;

        OutboxClaim first = outbox.claim("event-8").orElseThrow();
        assert outbox.claim("event-8").isEmpty();
        assert outbox.release(first);

        OutboxClaim second = outbox.claim("event-8").orElseThrow();
        assert !outbox.markPublished(first);
        assert outbox.markPublished(second);
        assert outbox.isPublished("event-8");
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
