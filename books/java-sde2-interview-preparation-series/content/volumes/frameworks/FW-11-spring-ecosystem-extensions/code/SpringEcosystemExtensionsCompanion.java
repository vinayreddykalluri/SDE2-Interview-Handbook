import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.function.Predicate;

/**
 * Dependency-free Java 21 checks for Spring ecosystem runtime reasoning.
 *
 * <p>This is deliberately not a replacement for Spring Security, Reactor,
 * Spring Cloud, Batch, or Integration. It makes the invariants hidden behind
 * those abstractions executable: first-match chain selection, resource-aware
 * authorization, remaining-deadline retries, demand, restart, idempotency, and
 * bounded correlation state.
 */
public final class SpringEcosystemExtensionsCompanion {
    private SpringEcosystemExtensionsCompanion() {
    }

    public static void main(String[] args) {
        validatesFirstMatchingSecurityChain();
        validatesResourceAwareAuthorization();
        validatesRemainingDeadlineRetryPlan();
        validatesDemandAccounting();
        validatesBatchRestartAndIdempotency();
        validatesCorrelationExpiry();
        System.out.println("PASS 6 Spring ecosystem extension invariant suites");
    }

    private static void validatesFirstMatchingSecurityChain() {
        List<SecurityChain> unsafeOrder = List.of(
                new SecurityChain("public-catch-all", request -> true, Decision.PERMIT),
                new SecurityChain("admin", request -> request.path().startsWith("/api/admin/"),
                        Decision.REQUIRE_ADMIN));

        HttpRequest admin = new HttpRequest("GET", "/api/admin/users");
        require(select(unsafeOrder, admin).decision() == Decision.PERMIT,
                "first broad matcher must demonstrate the shadowing defect");

        List<SecurityChain> safeOrder = List.of(
                new SecurityChain("admin", request -> request.path().startsWith("/api/admin/"),
                        Decision.REQUIRE_ADMIN),
                new SecurityChain("api", request -> request.path().startsWith("/api/"),
                        Decision.AUTHENTICATED),
                new SecurityChain("public", request -> request.path().equals("/health"),
                        Decision.PERMIT));

        require(select(safeOrder, admin).decision() == Decision.REQUIRE_ADMIN,
                "narrow admin chain must win");
        require(select(safeOrder, new HttpRequest("GET", "/api/orders/1")).decision()
                        == Decision.AUTHENTICATED,
                "ordinary API path must require authentication");
        expectFailure(() -> select(safeOrder, new HttpRequest("GET", "/unknown")),
                "no security chain");
    }

    private static void validatesResourceAwareAuthorization() {
        Actor refundOperator = new Actor("user-7", "tenant-a", Set.of("order:refund"));
        Order owned = new Order("order-1", "tenant-a", OrderState.PAID, 9_000);
        Order foreign = new Order("order-2", "tenant-b", OrderState.PAID, 9_000);
        Order shipped = new Order("order-3", "tenant-a", OrderState.SHIPPED, 9_000);

        require(refundAllowed(refundOperator, owned, 2_000),
                "authorized actor should refund an eligible owned order");
        require(!refundAllowed(refundOperator, foreign, 2_000),
                "authority must not cross tenant boundary");
        require(!refundAllowed(refundOperator, shipped, 2_000),
                "resource state must be part of authorization");
        require(!refundAllowed(refundOperator, owned, 10_000),
                "refund cannot exceed paid amount");
    }

    private static void validatesRemainingDeadlineRetryPlan() {
        RetryPolicy policy = new RetryPolicy(
                Duration.ofMillis(750),
                Duration.ofMillis(400),
                List.of(Duration.ofMillis(50), Duration.ofMillis(100)));

        List<AttemptBudget> plan = policy.plan();
        require(plan.size() == 2, "only two useful attempts fit in total budget");
        require(plan.get(0).timeout().equals(Duration.ofMillis(400)),
                "first attempt should receive configured cap");
        require(plan.get(1).timeout().equals(Duration.ofMillis(300)),
                "second attempt must use the remaining budget");
        require(plan.stream().map(AttemptBudget::timeout).reduce(Duration.ZERO, Duration::plus)
                        .plus(Duration.ofMillis(50)).compareTo(Duration.ofMillis(750)) <= 0,
                "planned attempts and backoff must fit total deadline");

        expectFailure(() -> new RetryPolicy(
                Duration.ZERO, Duration.ofMillis(10), List.of()).plan(),
                "total budget");
    }

    private static void validatesDemandAccounting() {
        DemandWindow demand = new DemandWindow();
        demand.request(3);
        require(demand.tryEmit("a"), "first demanded item should emit");
        require(demand.tryEmit("b"), "second demanded item should emit");
        require(demand.tryEmit("c"), "third demanded item should emit");
        require(!demand.tryEmit("overflow"), "publisher must not exceed demand");
        demand.request(1);
        demand.cancel();
        require(!demand.tryEmit("cancelled"), "cancelled subscriber must receive no item");
        require(demand.delivered().equals(List.of("a", "b", "c")),
                "delivery history should be deterministic");
    }

    private static void validatesBatchRestartAndIdempotency() {
        IdempotentPartner partner = new IdempotentPartner();
        BatchCheckpoint checkpoint = new BatchCheckpoint();
        List<String> itemKeys = List.of("settlement-1", "settlement-2", "settlement-3");

        processUntilCrash(itemKeys, checkpoint, partner, 2);
        require(checkpoint.lastCommittedIndex() == 0,
                "crash before chunk commit leaves checkpoint unchanged");
        require(partner.physicalEffects() == 2,
                "two remote effects happened before crash");

        processAndCommit(itemKeys, checkpoint, partner);
        require(checkpoint.lastCommittedIndex() == 3,
                "restart must advance checkpoint after successful chunk");
        require(partner.physicalEffects() == 3,
                "stable keys must suppress duplicate physical effects");
        require(partner.requests() == 5,
                "restart can repeat requests even when effects are deduplicated");
    }

    private static void validatesCorrelationExpiry() {
        Instant start = Instant.parse("2026-08-01T00:00:00Z");
        CorrelationStore store = new CorrelationStore(Duration.ofMinutes(5));
        store.add("group-a", "part-1", start);
        store.add("group-b", "part-1", start.plusSeconds(60));
        store.add("group-a", "part-2", start.plusSeconds(120));

        require(store.parts("group-a").equals(List.of("part-1", "part-2")),
                "correlation group must preserve insertion order");
        int removed = store.expireBefore(start.plusSeconds(330));
        require(removed == 1, "only stale group-a should expire");
        require(store.parts("group-a").isEmpty(), "expired group must be removed");
        require(store.parts("group-b").equals(List.of("part-1")),
                "fresh group must remain");
    }

    static SecurityChain select(List<SecurityChain> chains, HttpRequest request) {
        return chains.stream()
                .filter(chain -> chain.matches().test(request))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException(
                        "no security chain for " + request.path()));
    }

    static boolean refundAllowed(Actor actor, Order order, long amountInCents) {
        Objects.requireNonNull(actor, "actor");
        Objects.requireNonNull(order, "order");
        return actor.authorities().contains("order:refund")
                && actor.tenantId().equals(order.tenantId())
                && order.state() == OrderState.PAID
                && amountInCents > 0
                && amountInCents <= order.paidInCents();
    }

    private static void processUntilCrash(
            List<String> keys,
            BatchCheckpoint checkpoint,
            IdempotentPartner partner,
            int crashAfterRequests) {
        for (int index = checkpoint.lastCommittedIndex(); index < keys.size(); index++) {
            partner.apply(keys.get(index));
            if (partner.requests() == crashAfterRequests) {
                return;
            }
        }
    }

    private static void processAndCommit(
            List<String> keys,
            BatchCheckpoint checkpoint,
            IdempotentPartner partner) {
        for (int index = checkpoint.lastCommittedIndex(); index < keys.size(); index++) {
            partner.apply(keys.get(index));
        }
        checkpoint.commit(keys.size());
    }

    private static void expectFailure(Runnable action, String expectedText) {
        try {
            action.run();
            throw new AssertionError("expected failure containing: " + expectedText);
        } catch (IllegalArgumentException failure) {
            require(failure.getMessage().contains(expectedText),
                    "unexpected failure: " + failure.getMessage());
        }
    }

    private static void require(boolean condition, String message) {
        if (!condition) {
            throw new AssertionError(message);
        }
    }

    record HttpRequest(String method, String path) {
        HttpRequest {
            Objects.requireNonNull(method, "method");
            Objects.requireNonNull(path, "path");
        }
    }

    record SecurityChain(String name, Predicate<HttpRequest> matches, Decision decision) {
        SecurityChain {
            Objects.requireNonNull(name, "name");
            Objects.requireNonNull(matches, "matches");
            Objects.requireNonNull(decision, "decision");
        }
    }

    enum Decision {
        PERMIT,
        AUTHENTICATED,
        REQUIRE_ADMIN
    }

    record Actor(String id, String tenantId, Set<String> authorities) {
        Actor {
            Objects.requireNonNull(id, "id");
            Objects.requireNonNull(tenantId, "tenantId");
            authorities = Set.copyOf(authorities);
        }
    }

    record Order(String id, String tenantId, OrderState state, long paidInCents) {
        Order {
            Objects.requireNonNull(id, "id");
            Objects.requireNonNull(tenantId, "tenantId");
            Objects.requireNonNull(state, "state");
        }
    }

    enum OrderState {
        PAID,
        SHIPPED,
        CANCELLED
    }

    record AttemptBudget(int attempt, Duration timeout) {
        AttemptBudget {
            if (attempt < 1 || timeout.isNegative() || timeout.isZero()) {
                throw new IllegalArgumentException("invalid attempt budget");
            }
        }
    }

    record RetryPolicy(Duration totalBudget, Duration perAttemptCap, List<Duration> backoffs) {
        RetryPolicy {
            Objects.requireNonNull(totalBudget, "totalBudget");
            Objects.requireNonNull(perAttemptCap, "perAttemptCap");
            backoffs = List.copyOf(backoffs);
        }

        List<AttemptBudget> plan() {
            if (totalBudget.isNegative() || totalBudget.isZero()) {
                throw new IllegalArgumentException("total budget must be positive");
            }
            if (perAttemptCap.isNegative() || perAttemptCap.isZero()) {
                throw new IllegalArgumentException("per-attempt cap must be positive");
            }

            List<AttemptBudget> result = new ArrayList<>();
            Duration remaining = totalBudget;
            int attempt = 1;
            while (!remaining.isZero() && !remaining.isNegative()) {
                Duration timeout = remaining.compareTo(perAttemptCap) < 0
                        ? remaining : perAttemptCap;
                result.add(new AttemptBudget(attempt, timeout));
                int backoffIndex = attempt - 1;
                if (backoffIndex >= backoffs.size()) {
                    break;
                }
                Duration backoff = backoffs.get(backoffIndex);
                remaining = remaining.minus(timeout).minus(backoff);
                attempt++;
            }
            return List.copyOf(result);
        }
    }

    static final class DemandWindow {
        private long outstanding;
        private boolean cancelled;
        private final List<String> delivered = new ArrayList<>();

        void request(long count) {
            if (count <= 0) {
                throw new IllegalArgumentException("demand must be positive");
            }
            outstanding = Math.addExact(outstanding, count);
        }

        boolean tryEmit(String item) {
            Objects.requireNonNull(item, "item");
            if (cancelled || outstanding == 0) {
                return false;
            }
            outstanding--;
            delivered.add(item);
            return true;
        }

        void cancel() {
            cancelled = true;
        }

        List<String> delivered() {
            return List.copyOf(delivered);
        }
    }

    static final class BatchCheckpoint {
        private int lastCommittedIndex;

        int lastCommittedIndex() {
            return lastCommittedIndex;
        }

        void commit(int nextIndex) {
            if (nextIndex < lastCommittedIndex) {
                throw new IllegalArgumentException("checkpoint cannot move backward");
            }
            lastCommittedIndex = nextIndex;
        }
    }

    static final class IdempotentPartner {
        private final Set<String> completedKeys = new HashSet<>();
        private int requests;
        private int physicalEffects;

        String apply(String key) {
            Objects.requireNonNull(key, "key");
            requests++;
            if (completedKeys.add(key)) {
                physicalEffects++;
            }
            return "receipt:" + key;
        }

        int requests() {
            return requests;
        }

        int physicalEffects() {
            return physicalEffects;
        }
    }

    static final class CorrelationStore {
        private final Duration ttl;
        private final Map<String, Group> groups = new LinkedHashMap<>();

        CorrelationStore(Duration ttl) {
            if (ttl.isNegative() || ttl.isZero()) {
                throw new IllegalArgumentException("ttl must be positive");
            }
            this.ttl = ttl;
        }

        void add(String key, String part, Instant now) {
            Objects.requireNonNull(key, "key");
            Objects.requireNonNull(part, "part");
            Objects.requireNonNull(now, "now");
            Group group = groups.computeIfAbsent(key,
                    ignored -> new Group(now, new ArrayList<>()));
            group.parts().add(part);
        }

        int expireBefore(Instant now) {
            List<String> expired = groups.entrySet().stream()
                    .filter(entry -> !entry.getValue().createdAt().plus(ttl).isAfter(now))
                    .map(Map.Entry::getKey)
                    .toList();
            expired.forEach(groups::remove);
            return expired.size();
        }

        List<String> parts(String key) {
            return Optional.ofNullable(groups.get(key))
                    .map(group -> List.copyOf(group.parts()))
                    .orElseGet(List::of);
        }

        private record Group(Instant createdAt, List<String> parts) {
        }
    }
}
