import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

/** Dependency-free executable models for backend request boundaries. */
public final class BackendBoundaryPatterns {
    private static int checks;

    private BackendBoundaryPatterns() {}

    enum BeginStatus { STARTED, IN_PROGRESS, REPLAY }

    record BeginResult(BeginStatus status, String completedResponse) {}

    /**
     * Idempotency is scoped by caller-provided key and payload fingerprint.
     * A completed response is replayed; a different payload is a conflict.
     */
    static final class IdempotencyRegistry {
        private final Map<String, Entry> entries = new HashMap<>();

        synchronized BeginResult begin(String key, String fingerprint) {
            requireText(key, "key");
            requireText(fingerprint, "fingerprint");
            Entry entry = entries.get(key);
            if (entry == null) {
                entries.put(key, new Entry(fingerprint));
                return new BeginResult(BeginStatus.STARTED, null);
            }
            if (!entry.fingerprint.equals(fingerprint)) {
                throw new IllegalStateException("idempotency key reused with different payload");
            }
            return entry.completedResponse == null
                    ? new BeginResult(BeginStatus.IN_PROGRESS, null)
                    : new BeginResult(BeginStatus.REPLAY, entry.completedResponse);
        }

        synchronized void complete(String key, String fingerprint, String response) {
            Entry entry = matchingEntry(key, fingerprint);
            Objects.requireNonNull(response, "response");
            if (entry.completedResponse != null && !entry.completedResponse.equals(response)) {
                throw new IllegalStateException("completed response cannot be replaced");
            }
            entry.completedResponse = response;
        }

        synchronized void abandonBeforeSideEffect(String key, String fingerprint) {
            Entry entry = matchingEntry(key, fingerprint);
            if (entry.completedResponse != null) {
                throw new IllegalStateException("completed operation cannot be abandoned");
            }
            entries.remove(key);
        }

        private Entry matchingEntry(String key, String fingerprint) {
            Entry entry = entries.get(key);
            if (entry == null || !entry.fingerprint.equals(fingerprint)) {
                throw new IllegalStateException("unknown idempotency operation");
            }
            return entry;
        }

        private static final class Entry {
            private final String fingerprint;
            private String completedResponse;

            private Entry(String fingerprint) {
                this.fingerprint = fingerprint;
            }
        }
    }

    record Order(long id, String commandId, String tenantId, long amountCents) {}
    record OutboxEvent(long id, long orderId, String type) {}

    /** Models one database transaction writing domain state and an outbox row. */
    static final class OrderStore {
        private final Map<Long, Order> orders = new HashMap<>();
        private final Map<String, Long> orderByCommand = new HashMap<>();
        private final List<OutboxEvent> outbox = new ArrayList<>();
        private final Set<Long> publishedEventIds = new HashSet<>();
        private long nextOrderId = 1;
        private long nextEventId = 1;

        synchronized Order create(
                String commandId,
                String tenantId,
                long amountCents,
                boolean failBeforeCommit) {
            requireText(commandId, "commandId");
            requireText(tenantId, "tenantId");
            if (amountCents <= 0) {
                throw new IllegalArgumentException("amount must be positive");
            }
            Long existingId = orderByCommand.get(commandId);
            if (existingId != null) {
                return orders.get(existingId);
            }

            long orderId = nextOrderId;
            long eventId = nextEventId;
            Order order = new Order(orderId, commandId, tenantId, amountCents);
            OutboxEvent event = new OutboxEvent(eventId, orderId, "OrderCreated");
            if (failBeforeCommit) {
                throw new IllegalStateException("simulated transaction rollback");
            }

            orders.put(orderId, order);
            orderByCommand.put(commandId, orderId);
            outbox.add(event);
            nextOrderId++;
            nextEventId++;
            return order;
        }

        synchronized List<OutboxEvent> pendingEvents(int limit) {
            if (limit < 0) {
                throw new IllegalArgumentException("limit cannot be negative");
            }
            List<OutboxEvent> pending = new ArrayList<>();
            for (OutboxEvent event : outbox) {
                if (!publishedEventIds.contains(event.id())) {
                    pending.add(event);
                    if (pending.size() == limit) {
                        break;
                    }
                }
            }
            return List.copyOf(pending);
        }

        synchronized void markPublished(long eventId) {
            boolean known = outbox.stream().anyMatch(event -> event.id() == eventId);
            if (!known) {
                throw new IllegalArgumentException("unknown outbox event");
            }
            publishedEventIds.add(eventId);
        }

        synchronized int orderCount() {
            return orders.size();
        }
    }

    enum Role { USER, TENANT_ADMIN, PLATFORM_ADMIN }
    record Principal(String userId, String tenantId, Role role) {}
    record Resource(String ownerId, String tenantId) {}

    static boolean mayRead(Principal principal, Resource resource) {
        Objects.requireNonNull(principal, "principal");
        Objects.requireNonNull(resource, "resource");
        if (principal.role() == Role.PLATFORM_ADMIN) {
            return true;
        }
        if (!principal.tenantId().equals(resource.tenantId())) {
            return false;
        }
        return principal.role() == Role.TENANT_ADMIN
                || principal.userId().equals(resource.ownerId());
    }

    record Profile(long id, String tenantId, String displayName, long version) {}

    static final class ProfileStore {
        private final Map<Long, Profile> profiles = new HashMap<>();

        synchronized void insert(Profile profile) {
            if (profiles.putIfAbsent(profile.id(), profile) != null) {
                throw new IllegalStateException("duplicate profile");
            }
        }

        synchronized Profile rename(
                long id, String tenantId, long expectedVersion, String displayName) {
            requireText(displayName, "displayName");
            Profile current = profiles.get(id);
            if (current == null || !current.tenantId().equals(tenantId)) {
                throw new IllegalStateException("profile not found");
            }
            if (current.version() != expectedVersion) {
                throw new IllegalStateException("optimistic version conflict");
            }
            Profile updated = new Profile(
                    id, tenantId, displayName, Math.addExact(expectedVersion, 1));
            profiles.put(id, updated);
            return updated;
        }
    }

    static boolean constantTimeTokenEquals(String supplied, String expected) {
        if (supplied == null || expected == null) {
            return false;
        }
        return MessageDigest.isEqual(
                supplied.getBytes(StandardCharsets.UTF_8),
                expected.getBytes(StandardCharsets.UTF_8));
    }

    public static void main(String[] args) {
        IdempotencyRegistry registry = new IdempotencyRegistry();
        check(registry.begin("key-1", "hash-A").status() == BeginStatus.STARTED,
                "first request owns execution");
        check(registry.begin("key-1", "hash-A").status() == BeginStatus.IN_PROGRESS,
                "concurrent duplicate does not execute");
        expectFailure(() -> registry.begin("key-1", "hash-B"), IllegalStateException.class);
        registry.complete("key-1", "hash-A", "201:order-7");
        BeginResult replay = registry.begin("key-1", "hash-A");
        check(replay.status() == BeginStatus.REPLAY
                && replay.completedResponse().equals("201:order-7"), "response replay");
        expectFailure(() -> registry.complete("key-1", "hash-A", "201:order-8"),
                IllegalStateException.class);
        check(registry.begin("key-2", "hash-C").status() == BeginStatus.STARTED,
                "second operation starts");
        registry.abandonBeforeSideEffect("key-2", "hash-C");
        check(registry.begin("key-2", "hash-C").status() == BeginStatus.STARTED,
                "safe pre-side-effect retry");

        OrderStore orders = new OrderStore();
        Order first = orders.create("cmd-1", "tenant-A", 1_250, false);
        check(first.id() == 1 && orders.orderCount() == 1, "order committed");
        check(orders.pendingEvents(10).equals(
                List.of(new OutboxEvent(1, 1, "OrderCreated"))), "outbox committed atomically");
        check(orders.create("cmd-1", "tenant-A", 1_250, false) == first,
                "duplicate command returns existing order");
        check(orders.orderCount() == 1 && orders.pendingEvents(10).size() == 1,
                "duplicate creates no second effects");
        expectFailure(() -> orders.create("cmd-2", "tenant-A", 500, true),
                IllegalStateException.class);
        check(orders.orderCount() == 1, "rollback exposes no order");
        orders.markPublished(1);
        check(orders.pendingEvents(10).isEmpty(), "published outbox event hidden");
        orders.markPublished(1);
        check(orders.pendingEvents(10).isEmpty(), "publish acknowledgement idempotent");

        Principal owner = new Principal("user-1", "tenant-A", Role.USER);
        Resource owned = new Resource("user-1", "tenant-A");
        check(mayRead(owner, owned), "owner read");
        check(!mayRead(new Principal("user-2", "tenant-A", Role.USER), owned),
                "different user denied");
        check(!mayRead(new Principal("user-1", "tenant-B", Role.TENANT_ADMIN), owned),
                "cross-tenant admin denied");
        check(mayRead(new Principal("ops", "platform", Role.PLATFORM_ADMIN), owned),
                "explicit platform admin");

        ProfileStore profiles = new ProfileStore();
        profiles.insert(new Profile(1, "tenant-A", "Before", 4));
        Profile renamed = profiles.rename(1, "tenant-A", 4, "After");
        check(renamed.version() == 5 && renamed.displayName().equals("After"),
                "optimistic update increments version");
        expectFailure(() -> profiles.rename(1, "tenant-A", 4, "Stale"),
                IllegalStateException.class);
        check(constantTimeTokenEquals("secret", "secret"), "equal token");
        check(!constantTimeTokenEquals("secret", "other"), "different token");

        check(checks == 23, "reported check count");
        System.out.println("PASS 24 backend-boundary checks");
    }

    private static void requireText(String value, String name) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(name + " is required");
        }
    }

    private static void expectFailure(Runnable action, Class<? extends Throwable> type) {
        try {
            action.run();
        } catch (Throwable failure) {
            if (type.isInstance(failure)) {
                check(true, "expected " + type.getSimpleName());
                return;
            }
            throw new AssertionError("wrong failure type", failure);
        }
        throw new AssertionError("expected " + type.getSimpleName());
    }

    private static void check(boolean condition, String message) {
        if (!condition) {
            throw new AssertionError("check " + (checks + 1) + " failed: " + message);
        }
        checks++;
    }
}
