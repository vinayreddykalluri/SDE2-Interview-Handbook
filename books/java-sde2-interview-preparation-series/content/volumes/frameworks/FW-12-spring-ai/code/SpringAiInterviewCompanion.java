import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;

/**
 * Dependency-free Java 21 checks for Spring AI application invariants.
 *
 * <p>No model is called. The suite makes the deterministic shell executable:
 * context budgeting, tenant-filtered retrieval, structured business validation,
 * authorized/idempotent tools, conversation ownership, evaluation, and bounded
 * retry planning.
 */
public final class SpringAiInterviewCompanion {
    private SpringAiInterviewCompanion() {
    }

    public static void main(String[] args) {
        validatesContextBudget();
        validatesTenantFilteredRetrieval();
        validatesStructuredBusinessRules();
        validatesAuthorizedIdempotentTool();
        validatesConversationOwnership();
        validatesEvaluationMetrics();
        validatesBoundedRetryPlan();
        System.out.println("PASS 7 Spring AI deterministic-boundary suites");
    }

    private static void validatesContextBudget() {
        ContextBudget budget = new ContextBudget(16_000, 2_000, 1_200, 3_000, 600);
        require(budget.retrievalTokens() == 9_200,
                "retrieval budget should preserve output and fixed input reserves");
        expectFailure(() -> new ContextBudget(4_000, 2_000, 1_200, 1_000, 600)
                .retrievalTokens(), "exceeds context");
    }

    private static void validatesTenantFilteredRetrieval() {
        List<Chunk> corpus = List.of(
                new Chunk("a-current", "tenant-a", true, new double[]{1.0, 0.0},
                        "Tenant A refund policy"),
                new Chunk("a-old", "tenant-a", false, new double[]{0.99, 0.01},
                        "Obsolete Tenant A policy"),
                new Chunk("b-secret", "tenant-b", true, new double[]{1.0, 0.0},
                        "Tenant B confidential policy"),
                new Chunk("a-other", "tenant-a", true, new double[]{0.0, 1.0},
                        "Tenant A shipping policy"));

        List<RetrievedChunk> results = retrieve(
                corpus, new double[]{1.0, 0.0}, "tenant-a", 3);
        require(results.stream().map(result -> result.chunk().id()).toList()
                        .equals(List.of("a-current", "a-other")),
                "retrieval must filter tenant and active version before ranking");
        require(results.stream().noneMatch(result -> result.chunk().tenantId().equals("tenant-b")),
                "cross-tenant chunk must never enter results");
    }

    private static void validatesStructuredBusinessRules() {
        Set<String> teams = Set.of("billing", "shipping", "account-security");
        TicketRoute valid = new TicketRoute("billing", 2, "duplicate charge");
        require(validateRoute(valid, teams).isEmpty(), "valid route should pass");

        TicketRoute invented = new TicketRoute("super-admin", 99, "x");
        List<String> failures = validateRoute(invented, teams);
        require(failures.contains("unknown team"), "invented team must fail");
        require(failures.contains("priority outside 1..5"), "priority must be bounded");
        require(failures.contains("reason too short"), "reason must meet business rule");
    }

    private static void validatesAuthorizedIdempotentTool() {
        RefundTool tool = new RefundTool();
        Actor operator = new Actor("user-1", "tenant-a", Set.of("order:refund"));
        Order order = new Order("order-1", "tenant-a", 4L, OrderState.PAID, 10_000);
        Approval approval = new Approval(
                "approval-1", "tenant-a", "user-1", "order-1", 2_000,
                4L, "operation-7", Instant.parse("2026-08-01T01:00:00Z"));
        Instant now = Instant.parse("2026-08-01T00:30:00Z");

        RefundReceipt first = tool.execute(operator, order, approval, now);
        RefundReceipt duplicate = tool.execute(operator, order, approval, now.plusSeconds(5));
        require(first.equals(duplicate), "same operation key must return original result");
        require(tool.physicalEffects() == 1, "duplicate request must not repeat physical effect");

        Actor foreign = new Actor("user-2", "tenant-b", Set.of("order:refund"));
        expectFailure(() -> tool.execute(foreign, order, approval, now), "tenant");
        Order changed = new Order("order-1", "tenant-a", 5L, OrderState.SHIPPED, 10_000);
        expectFailure(() -> tool.execute(operator, changed, approval, now), "version");
    }

    private static void validatesConversationOwnership() {
        ConversationStore store = new ConversationStore();
        ConversationKey key = new ConversationKey("tenant-a", "user-1", "conversation-9");
        store.append(key, "message-1", "hello");
        store.append(key, "message-2", "refund policy?");

        require(store.read(key).size() == 2, "owner should read ordered conversation");
        ConversationKey attacker = new ConversationKey("tenant-b", "user-2", "conversation-9");
        require(store.read(attacker).isEmpty(),
                "same public conversation id under another scope must reveal nothing");
        expectFailure(() -> store.append(key, "message-2", "overwrite"),
                "duplicate message");
    }

    private static void validatesEvaluationMetrics() {
        Set<String> expected = Set.of("source-1", "source-2");
        List<List<String>> retrieved = List.of(
                List.of("source-9", "source-1"),
                List.of("source-8", "source-7"),
                List.of("source-2", "source-1"));
        RetrievalMetrics metrics = evaluateRetrieval(expected, retrieved);
        require(close(metrics.recallAtK(), 1.0), "both expected sources were retrieved");
        require(close(metrics.caseHitRate(), 2.0 / 3.0), "two of three cases contain evidence");
        require(metrics.authorizationViolations() == 0,
                "fixture should have zero authorization violations");

        require(citationsAreSupplied(Set.of("source-1"), Set.of("source-1", "source-9")),
                "supplied citation should pass");
        require(!citationsAreSupplied(Set.of("invented"), Set.of("source-1")),
                "invented citation must fail deterministically");
    }

    private static void validatesBoundedRetryPlan() {
        List<Attempt> plan = retryPlan(
                Duration.ofMillis(1_000),
                Duration.ofMillis(450),
                List.of(Duration.ofMillis(75), Duration.ofMillis(150)));
        require(plan.size() == 2, "only two attempts should fit");
        require(plan.get(1).timeout().equals(Duration.ofMillis(450)),
                "second attempt remains within cap and remaining deadline");
        require(plan.get(1).startsAfter().equals(Duration.ofMillis(525)),
                "second attempt starts after first cap plus backoff");
    }

    static List<RetrievedChunk> retrieve(
            List<Chunk> corpus, double[] query, String trustedTenantId, int limit) {
        Objects.requireNonNull(corpus, "corpus");
        Objects.requireNonNull(query, "query");
        Objects.requireNonNull(trustedTenantId, "trustedTenantId");
        if (limit < 1) {
            throw new IllegalArgumentException("limit must be positive");
        }
        return corpus.stream()
                .filter(Chunk::active)
                .filter(chunk -> chunk.tenantId().equals(trustedTenantId))
                .map(chunk -> new RetrievedChunk(chunk, cosine(query, chunk.embedding())))
                .sorted(Comparator.comparingDouble(RetrievedChunk::score).reversed()
                        .thenComparing(result -> result.chunk().id()))
                .limit(limit)
                .toList();
    }

    static double cosine(double[] first, double[] second) {
        if (first.length == 0 || first.length != second.length) {
            throw new IllegalArgumentException("vectors require equal positive dimensions");
        }
        double dot = 0.0;
        double firstSquared = 0.0;
        double secondSquared = 0.0;
        for (int index = 0; index < first.length; index++) {
            dot += first[index] * second[index];
            firstSquared += first[index] * first[index];
            secondSquared += second[index] * second[index];
        }
        if (firstSquared == 0.0 || secondSquared == 0.0) {
            throw new IllegalArgumentException("zero vector has undefined cosine");
        }
        return dot / (Math.sqrt(firstSquared) * Math.sqrt(secondSquared));
    }

    static List<String> validateRoute(TicketRoute route, Set<String> allowedTeams) {
        Objects.requireNonNull(route, "route");
        List<String> failures = new ArrayList<>();
        if (!allowedTeams.contains(route.team())) {
            failures.add("unknown team");
        }
        if (route.priority() < 1 || route.priority() > 5) {
            failures.add("priority outside 1..5");
        }
        if (route.reason().trim().length() < 3) {
            failures.add("reason too short");
        }
        return List.copyOf(failures);
    }

    static RetrievalMetrics evaluateRetrieval(
            Set<String> expectedSourceIds, List<List<String>> cases) {
        Set<String> found = new HashSet<>();
        int casesWithHit = 0;
        for (List<String> oneCase : cases) {
            boolean hit = oneCase.stream().anyMatch(expectedSourceIds::contains);
            if (hit) {
                casesWithHit++;
            }
            oneCase.stream().filter(expectedSourceIds::contains).forEach(found::add);
        }
        double recall = expectedSourceIds.isEmpty()
                ? 1.0 : (double) found.size() / expectedSourceIds.size();
        double hitRate = cases.isEmpty() ? 1.0 : (double) casesWithHit / cases.size();
        return new RetrievalMetrics(recall, hitRate, 0);
    }

    static boolean citationsAreSupplied(Set<String> citations, Set<String> supplied) {
        return supplied.containsAll(citations);
    }

    static List<Attempt> retryPlan(
            Duration total, Duration perAttempt, List<Duration> backoffs) {
        if (total.isZero() || total.isNegative()
                || perAttempt.isZero() || perAttempt.isNegative()) {
            throw new IllegalArgumentException("durations must be positive");
        }
        List<Attempt> attempts = new ArrayList<>();
        Duration elapsed = Duration.ZERO;
        int number = 1;
        while (elapsed.compareTo(total) < 0) {
            Duration remaining = total.minus(elapsed);
            Duration timeout = remaining.compareTo(perAttempt) < 0 ? remaining : perAttempt;
            attempts.add(new Attempt(number, elapsed, timeout));
            int backoffIndex = number - 1;
            if (backoffIndex >= backoffs.size()) {
                break;
            }
            elapsed = elapsed.plus(timeout).plus(backoffs.get(backoffIndex));
            number++;
        }
        return List.copyOf(attempts);
    }

    private static boolean close(double first, double second) {
        return Math.abs(first - second) < 0.000_001;
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

    record ContextBudget(
            int modelLimit,
            int outputReserve,
            int instructionAndToolTokens,
            int conversationTokens,
            int userTokens) {
        int retrievalTokens() {
            int fixed = Math.addExact(outputReserve,
                    Math.addExact(instructionAndToolTokens,
                            Math.addExact(conversationTokens, userTokens)));
            int result = modelLimit - fixed;
            if (modelLimit <= 0 || result < 0) {
                throw new IllegalArgumentException("reserved tokens exceeds context limit");
            }
            return result;
        }
    }

    record Chunk(
            String id,
            String tenantId,
            boolean active,
            double[] embedding,
            String text) {
        Chunk {
            Objects.requireNonNull(id, "id");
            Objects.requireNonNull(tenantId, "tenantId");
            embedding = embedding.clone();
            Objects.requireNonNull(text, "text");
        }

        @Override
        public double[] embedding() {
            return embedding.clone();
        }
    }

    record RetrievedChunk(Chunk chunk, double score) {
    }

    record TicketRoute(String team, int priority, String reason) {
        TicketRoute {
            Objects.requireNonNull(team, "team");
            Objects.requireNonNull(reason, "reason");
        }
    }

    record Actor(String id, String tenantId, Set<String> authorities) {
        Actor {
            Objects.requireNonNull(id, "id");
            Objects.requireNonNull(tenantId, "tenantId");
            authorities = Set.copyOf(authorities);
        }
    }

    record Order(String id, String tenantId, long version, OrderState state, long paidInCents) {
        Order {
            Objects.requireNonNull(id, "id");
            Objects.requireNonNull(tenantId, "tenantId");
            Objects.requireNonNull(state, "state");
        }
    }

    enum OrderState {
        PAID,
        SHIPPED
    }

    record Approval(
            String id,
            String tenantId,
            String actorId,
            String orderId,
            long amountInCents,
            long orderVersion,
            String operationKey,
            Instant expiresAt) {
    }

    record RefundReceipt(String operationKey, String orderId, long amountInCents) {
    }

    static final class RefundTool {
        private final Map<String, RefundReceipt> receipts = new HashMap<>();
        private int physicalEffects;

        RefundReceipt execute(Actor actor, Order order, Approval approval, Instant now) {
            Objects.requireNonNull(actor, "actor");
            Objects.requireNonNull(order, "order");
            Objects.requireNonNull(approval, "approval");
            Objects.requireNonNull(now, "now");
            if (!actor.tenantId().equals(order.tenantId())
                    || !actor.tenantId().equals(approval.tenantId())) {
                throw new IllegalArgumentException("tenant authorization failed");
            }
            if (!actor.id().equals(approval.actorId())
                    || !actor.authorities().contains("order:refund")) {
                throw new IllegalArgumentException("actor authorization failed");
            }
            if (!order.id().equals(approval.orderId())) {
                throw new IllegalArgumentException("order target differs from approval");
            }
            if (order.version() != approval.orderVersion()) {
                throw new IllegalArgumentException("order version changed after approval");
            }
            if (order.state() != OrderState.PAID
                    || approval.amountInCents() <= 0
                    || approval.amountInCents() > order.paidInCents()) {
                throw new IllegalArgumentException("refund business rule failed");
            }
            if (!now.isBefore(approval.expiresAt())) {
                throw new IllegalArgumentException("approval expired");
            }
            return receipts.computeIfAbsent(approval.operationKey(), key -> {
                physicalEffects++;
                return new RefundReceipt(key, order.id(), approval.amountInCents());
            });
        }

        int physicalEffects() {
            return physicalEffects;
        }
    }

    record ConversationKey(String tenantId, String userId, String conversationId) {
    }

    record Message(String id, String text) {
    }

    static final class ConversationStore {
        private final Map<ConversationKey, List<Message>> messages = new HashMap<>();

        void append(ConversationKey key, String messageId, String text) {
            Objects.requireNonNull(key, "key");
            List<Message> conversation = messages.computeIfAbsent(key,
                    ignored -> new ArrayList<>());
            if (conversation.stream().anyMatch(message -> message.id().equals(messageId))) {
                throw new IllegalArgumentException("duplicate message id");
            }
            conversation.add(new Message(messageId, text));
        }

        List<Message> read(ConversationKey key) {
            return Optional.ofNullable(messages.get(key))
                    .map(List::copyOf)
                    .orElseGet(List::of);
        }
    }

    record RetrievalMetrics(double recallAtK, double caseHitRate, int authorizationViolations) {
    }

    record Attempt(int number, Duration startsAfter, Duration timeout) {
    }
}
