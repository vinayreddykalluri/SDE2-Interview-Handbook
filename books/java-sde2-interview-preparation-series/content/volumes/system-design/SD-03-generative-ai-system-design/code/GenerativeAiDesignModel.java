package sd03;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Executable model of the design decisions in this volume.
 *
 * Every claim in the chapters that can be made arithmetic or algorithmic is
 * implemented here so it can be run, changed, and argued with. There are no
 * dependencies and no network calls: the point is the reasoning, not a client
 * library. Run it with:
 *
 *     javac --release 21 GenerativeAiDesignModel.java && java sd03.GenerativeAiDesignModel
 *
 * Sections map to chapters:
 *   1. Token and cost estimation, latency budgeting        (chapter 1)
 *   2. Reciprocal rank fusion for hybrid retrieval          (chapter 2)
 *   3. Citation validation                                  (chapter 2)
 *   4. Agent loop bounds and idempotency keys               (chapter 3)
 *   5. Eval scoring and the regression gate                 (chapter 4)
 */
public final class GenerativeAiDesignModel {

    private GenerativeAiDesignModel() {
    }

    // ---------------------------------------------------------------- 1 ----
    // Chapter 1: estimate before designing.

    /** Provider pricing, expressed per million tokens. Vendor numbers expire; measure yours. */
    record Pricing(double inputPerMillion, double outputPerMillion) {
        double cost(long inputTokens, long outputTokens) {
            return (inputTokens * inputPerMillion + outputTokens * outputPerMillion) / 1_000_000.0;
        }
    }

    /** The per-request token budget. Everything competes for one context window. */
    record TokenBudget(int systemPrompt, int retrievedContext, int history, int question, int maxOutput) {
        int input() {
            return systemPrompt + retrievedContext + history + question;
        }

        int total() {
            return input() + maxOutput;
        }

        void requireFits(int contextWindow) {
            if (total() > contextWindow) {
                throw new IllegalStateException(
                        "Budget of " + total() + " tokens exceeds the " + contextWindow
                                + " token window. Trim retrieved context or cap output.");
            }
        }
    }

    record Traffic(long dailyActiveUsers, double adoptionRate, int turnsPerSession) {
        long requestsPerDay() {
            return Math.round(dailyActiveUsers * adoptionRate * turnsPerSession);
        }

        double averageRps() {
            return requestsPerDay() / 86_400.0;
        }

        double peakRps(double peakMultiplier) {
            return averageRps() * peakMultiplier;
        }
    }

    record CostEstimate(long requestsPerDay, long inputTokensPerDay, long outputTokensPerDay,
                        double inputCostPerDay, double outputCostPerDay) {
        double totalPerDay() {
            return inputCostPerDay + outputCostPerDay;
        }

        double totalPerMonth() {
            return totalPerDay() * 30;
        }

        /**
         * Output is a minority of tokens and a majority of cost. This is why capping
         * max_tokens is the single highest-leverage cost control available.
         */
        double outputCostShare() {
            return outputCostPerDay / totalPerDay();
        }
    }

    static CostEstimate estimate(Traffic traffic, TokenBudget budget, Pricing pricing) {
        long requests = traffic.requestsPerDay();
        long inputTokens = requests * budget.input();
        long outputTokens = requests * budget.maxOutput();
        return new CostEstimate(
                requests,
                inputTokens,
                outputTokens,
                pricing.cost(inputTokens, 0),
                pricing.cost(0, outputTokens));
    }

    /**
     * Total latency is dominated by decode, which is proportional to output length.
     * Streaming makes the user-visible number timeToFirstToken rather than total.
     */
    record LatencyBudget(int embedMs, int searchMs, int rerankMs, int modelTtftMs,
                         int outputTokens, double tokensPerSecond) {
        int timeToFirstToken() {
            return embedMs + searchMs + rerankMs + modelTtftMs;
        }

        int totalMs() {
            return timeToFirstToken() + (int) Math.round(outputTokens / tokensPerSecond * 1000);
        }

        /** Dropping the reranker is the usual lever when TTFT is over budget. */
        LatencyBudget withoutReranker() {
            return new LatencyBudget(embedMs, searchMs, 0, modelTtftMs, outputTokens, tokensPerSecond);
        }
    }

    // ---------------------------------------------------------------- 2 ----
    // Chapter 2: hybrid retrieval. Lexical and semantic retrievers fail in
    // opposite directions, and their scores are not on a comparable scale.
    // Reciprocal rank fusion merges ranked lists without needing them to be.

    record Chunk(String id, String documentTitle, String sectionPath, String text, Set<String> allowedRoles) {
        /**
         * Chapter 2: embedding a bare paragraph loses the context that makes it
         * interpretable. Prefixing the document and section path is cheap and
         * usually beats switching embedding models.
         */
        String embeddingText() {
            return documentTitle + " > " + sectionPath + ": " + text;
        }

        boolean visibleTo(Set<String> userRoles) {
            return allowedRoles.stream().anyMatch(userRoles::contains);
        }
    }

    static final double RRF_K = 60.0;

    /**
     * Fuse any number of ranked lists. Score depends only on rank, so retrievers
     * with wildly different scoring schemes combine without normalization.
     */
    static List<String> reciprocalRankFusion(List<List<String>> rankedLists, int limit) {
        Map<String, Double> fused = new HashMap<>();
        for (List<String> list : rankedLists) {
            for (int rank = 0; rank < list.size(); rank++) {
                fused.merge(list.get(rank), 1.0 / (RRF_K + rank + 1), Double::sum);
            }
        }
        return fused.entrySet().stream()
                .sorted(Map.Entry.<String, Double>comparingByValue().reversed()
                        .thenComparing(Map.Entry.<String, Double>comparingByKey()))
                .limit(limit)
                .map(Map.Entry::getKey)
                .toList();
    }

    /**
     * Authorization is applied as a predicate over the candidate set, standing in
     * for a predicate pushed into the index. Filtering AFTER ranking is the bug
     * this method exists to make impossible: it silently shrinks the result set
     * and can leak a document into the prompt.
     */
    static List<Chunk> retrieveAuthorized(List<Chunk> corpus, List<String> rankedIds,
                                          Set<String> userRoles, int topK) {
        Map<String, Chunk> byId = new HashMap<>();
        for (Chunk c : corpus) {
            byId.put(c.id(), c);
        }
        List<Chunk> out = new ArrayList<>();
        for (String id : rankedIds) {
            Chunk chunk = byId.get(id);
            if (chunk != null && chunk.visibleTo(userRoles)) {
                out.add(chunk);
                if (out.size() == topK) {
                    break;
                }
            }
        }
        return out;
    }

    // ---------------------------------------------------------------- 3 ----
    // Chapter 2: a model that cites an id you never supplied has fabricated its
    // own evidence. That is cheap to detect and almost nobody checks.

    static final Pattern CITATION = Pattern.compile("\\[(doc_[A-Za-z0-9_-]+)]");

    record CitationReport(Set<String> cited, Set<String> supplied, Set<String> fabricated) {
        boolean valid() {
            return fabricated.isEmpty();
        }

        /** Chapter 4 tracks this as an SLI; below ~98% is an incident. */
        double validityRatio() {
            return cited.isEmpty() ? 1.0 : (cited.size() - fabricated.size()) / (double) cited.size();
        }
    }

    static CitationReport validateCitations(String answer, List<Chunk> supplied) {
        Set<String> suppliedIds = new HashSet<>();
        for (Chunk c : supplied) {
            suppliedIds.add(c.id());
        }
        Set<String> cited = new LinkedHashSet<>();
        Matcher matcher = CITATION.matcher(answer);
        while (matcher.find()) {
            cited.add(matcher.group(1));
        }
        Set<String> fabricated = new LinkedHashSet<>(cited);
        fabricated.removeAll(suppliedIds);
        return new CitationReport(cited, suppliedIds, fabricated);
    }

    // ---------------------------------------------------------------- 4 ----
    // Chapter 3: an agent loop terminates because you make it, not because the
    // model decides to stop. Four independent bounds catch four failures.

    record AgentBounds(int maxIterations, int maxTokens, long maxWallClockMs, double maxCostUsd) {
        static AgentBounds defaults() {
            return new AgentBounds(10, 50_000, 60_000, 0.50);
        }
    }

    enum StopReason { COMPLETED, MAX_ITERATIONS, MAX_TOKENS, TIMEOUT, BUDGET_EXCEEDED, REPETITION }

    record ToolCall(String tool, String canonicalArgs) {
    }

    /**
     * Tracks a run against its bounds. Repetition detection matters because a
     * model calling the same tool with the same arguments three times is stuck,
     * and spending the remaining budget will not unstick it.
     */
    static final class AgentRun {
        private final AgentBounds bounds;
        private final long startedAtMs;
        private final Deque<ToolCall> recentCalls = new ArrayDeque<>();
        private int iterations;
        private int tokensUsed;
        private double costUsd;

        AgentRun(AgentBounds bounds, long startedAtMs) {
            this.bounds = bounds;
            this.startedAtMs = startedAtMs;
        }

        Optional<StopReason> recordStep(ToolCall call, int tokens, double cost, long nowMs) {
            iterations++;
            tokensUsed += tokens;
            costUsd += cost;

            recentCalls.addLast(call);
            if (recentCalls.size() > 3) {
                recentCalls.removeFirst();
            }
            if (recentCalls.size() == 3 && recentCalls.stream().distinct().count() == 1) {
                return Optional.of(StopReason.REPETITION);
            }
            if (iterations >= bounds.maxIterations()) {
                return Optional.of(StopReason.MAX_ITERATIONS);
            }
            if (tokensUsed >= bounds.maxTokens()) {
                return Optional.of(StopReason.MAX_TOKENS);
            }
            if (nowMs - startedAtMs >= bounds.maxWallClockMs()) {
                return Optional.of(StopReason.TIMEOUT);
            }
            if (costUsd >= bounds.maxCostUsd()) {
                return Optional.of(StopReason.BUDGET_EXCEEDED);
            }
            return Optional.empty();
        }

        int iterations() {
            return iterations;
        }

        double costUsd() {
            return costUsd;
        }
    }

    /**
     * Chapter 3: agents retry, and networks fail after the effect but before the
     * response. The key must be deterministic across argument orderings, which is
     * why the arguments are canonicalized before hashing.
     */
    static String idempotencyKey(String runId, String toolName, Map<String, String> args) {
        String canonical = args.entrySet().stream()
                .sorted(Map.Entry.comparingByKey())
                .map(e -> e.getKey() + "=" + e.getValue())
                .reduce((a, b) -> a + "&" + b)
                .orElse("");
        return Integer.toHexString(Objects.hash(runId, toolName, canonical));
    }

    // ---------------------------------------------------------------- 5 ----
    // Chapter 4: correctness is a distribution, so testing becomes measurement.
    // The gate compares against a baseline instead of asserting equality.

    record EvalCase(String id, String category, boolean expectRefusal) {
    }

    record EvalResult(EvalCase evalCase, boolean refused, double faithfulness, boolean citationsValid) {
        /** An absent-answer case is correct only if the system declined. */
        boolean passed() {
            if (evalCase.expectRefusal()) {
                return refused;
            }
            return !refused && faithfulness >= 0.8 && citationsValid;
        }
    }

    record EvalSummary(int total, int passed, double meanFaithfulness, double refusalRate) {
        double passRate() {
            return total == 0 ? 0 : passed / (double) total;
        }
    }

    static EvalSummary summarize(List<EvalResult> results) {
        int passed = 0;
        int refused = 0;
        double faithfulnessSum = 0;
        for (EvalResult r : results) {
            if (r.passed()) {
                passed++;
            }
            if (r.refused()) {
                refused++;
            }
            faithfulnessSum += r.faithfulness();
        }
        int n = results.size();
        return new EvalSummary(n, passed, n == 0 ? 0 : faithfulnessSum / n,
                n == 0 ? 0 : refused / (double) n);
    }

    /**
     * The release gate. The golden set is absolute; aggregate metrics get a
     * tolerance derived from measured run-to-run variance, not intuition.
     * Refusal rate is a band because both directions are incidents: too low
     * means answering things it should not, too high means retrieval broke.
     */
    static List<String> regressionGate(EvalSummary candidate, EvalSummary baseline,
                                       List<EvalResult> goldenResults, double tolerance) {
        List<String> failures = new ArrayList<>();
        for (EvalResult r : goldenResults) {
            if (!r.passed()) {
                failures.add("golden case failed: " + r.evalCase().id());
            }
        }
        if (candidate.passRate() < baseline.passRate() - tolerance) {
            failures.add(String.format("pass rate regressed: %.3f -> %.3f (tolerance %.3f)",
                    baseline.passRate(), candidate.passRate(), tolerance));
        }
        if (candidate.meanFaithfulness() < baseline.meanFaithfulness() - tolerance) {
            failures.add(String.format("faithfulness regressed: %.3f -> %.3f",
                    baseline.meanFaithfulness(), candidate.meanFaithfulness()));
        }
        // The band is only meaningful once the set is large enough for the rate to
        // mean anything. On a handful of cases it is noise, and a gate that cries
        // wolf on every run is a gate the team learns to ignore.
        if (candidate.total() >= 20) {
            if (candidate.refusalRate() < 0.02) {
                failures.add(String.format("refusal rate %.3f below band - answering what it should not",
                        candidate.refusalRate()));
            }
            if (candidate.refusalRate() > 0.08) {
                failures.add(String.format("refusal rate %.3f above band - retrieval or a guardrail broke",
                        candidate.refusalRate()));
            }
        }
        return failures;
    }

    // -------------------------------------------------------------- demo ----

    public static void main(String[] args) {
        System.out.println("== 1. Estimate before designing ==");
        Traffic traffic = new Traffic(500_000, 0.08, 3);
        TokenBudget budget = new TokenBudget(400, 2_000, 600, 100, 500);
        budget.requireFits(128_000);
        Pricing pricing = new Pricing(3.00, 15.00);
        CostEstimate cost = estimate(traffic, budget, pricing);

        System.out.printf("  requests/day      %,d%n", cost.requestsPerDay());
        System.out.printf("  peak rps (10x)    %.1f%n", traffic.peakRps(10));
        System.out.printf("  input tokens/day  %,d%n", cost.inputTokensPerDay());
        System.out.printf("  output tokens/day %,d%n", cost.outputTokensPerDay());
        System.out.printf("  cost/day          $%,.2f%n", cost.totalPerDay());
        System.out.printf("  cost/month        $%,.2f%n", cost.totalPerMonth());
        System.out.printf("  output share of cost %.0f%% from %.0f%% of tokens%n",
                cost.outputCostShare() * 100,
                100.0 * cost.outputTokensPerDay()
                        / (cost.inputTokensPerDay() + cost.outputTokensPerDay()));

        System.out.println();
        System.out.println("== Latency: streaming changes what the user experiences ==");
        LatencyBudget latency = new LatencyBudget(30, 20, 80, 400, 500, 30);
        System.out.printf("  with reranker    TTFT %dms, total %dms%n",
                latency.timeToFirstToken(), latency.totalMs());
        System.out.printf("  without reranker TTFT %dms, total %dms%n",
                latency.withoutReranker().timeToFirstToken(), latency.withoutReranker().totalMs());

        System.out.println();
        System.out.println("== 2. Hybrid retrieval with RRF, and ACL enforcement ==");
        List<Chunk> corpus = List.of(
                new Chunk("doc_1", "API Reference", "Rate Limits > Free Tier",
                        "Free tier accounts are limited to 500 requests per hour.", Set.of("everyone")),
                new Chunk("doc_2", "API Reference", "Errors",
                        "ERR_5521 indicates the hourly quota was exhausted.", Set.of("everyone")),
                new Chunk("doc_3", "Billing Internal", "Margins",
                        "Enterprise discount floor is 32 percent.", Set.of("finance")));

        List<String> bm25 = List.of("doc_2", "doc_3", "doc_1");   // exact term match wins on ERR_5521
        List<String> dense = List.of("doc_1", "doc_2", "doc_3");  // paraphrase match wins on "quota"
        List<String> fused = reciprocalRankFusion(List.of(bm25, dense), 3);
        System.out.println("  fused ranking     " + fused);

        List<Chunk> forEmployee = retrieveAuthorized(corpus, fused, Set.of("everyone"), 3);
        List<Chunk> forFinance = retrieveAuthorized(corpus, fused, Set.of("everyone", "finance"), 3);
        System.out.println("  employee sees     " + forEmployee.stream().map(Chunk::id).toList());
        System.out.println("  finance sees      " + forFinance.stream().map(Chunk::id).toList());
        if (forEmployee.stream().anyMatch(c -> c.id().equals("doc_3"))) {
            throw new AssertionError("ACL leak: restricted chunk reached an unauthorized user");
        }
        System.out.println("  adversarial ACL assertion passed");

        System.out.println();
        System.out.println("== 3. Citation validation ==");
        String good = "Free tier allows 500 requests per hour [doc_1], and ERR_5521 means "
                + "the quota is exhausted [doc_2].";
        String bad = "Free tier allows 500 requests per hour [doc_1], per the pricing table [doc_31].";
        System.out.println("  grounded answer   valid=" + validateCitations(good, forEmployee).valid());
        CitationReport report = validateCitations(bad, forEmployee);
        System.out.println("  fabricated cite   valid=" + report.valid()
                + " fabricated=" + report.fabricated()
                + String.format(" validity=%.2f", report.validityRatio()));

        System.out.println();
        System.out.println("== 4. Agent bounds and idempotency ==");
        AgentRun run = new AgentRun(AgentBounds.defaults(), 0);
        ToolCall stuck = new ToolCall("get_order", "id=A-991");
        StopReason reason = StopReason.COMPLETED;
        for (int step = 1; step <= 5; step++) {
            Optional<StopReason> stop = run.recordStep(stuck, 1_200, 0.01, step * 500L);
            if (stop.isPresent()) {
                reason = stop.get();
                break;
            }
        }
        System.out.printf("  stopped after %d steps: %s (cost $%.2f)%n",
                run.iterations(), reason, run.costUsd());

        Map<String, String> argsA = new LinkedHashMap<>();
        argsA.put("order_id", "A-991");
        argsA.put("amount", "50.00");
        Map<String, String> argsB = new LinkedHashMap<>();
        argsB.put("amount", "50.00");
        argsB.put("order_id", "A-991");
        String keyA = idempotencyKey("run-7", "issue_refund", argsA);
        String keyB = idempotencyKey("run-7", "issue_refund", argsB);
        System.out.println("  idempotency key stable across argument order: " + keyA.equals(keyB));

        System.out.println();
        System.out.println("== 5. Eval summary and regression gate ==");
        List<EvalResult> baselineResults = List.of(
                new EvalResult(new EvalCase("e1", "happy", false), false, 0.94, true),
                new EvalResult(new EvalCase("e2", "edge", false), false, 0.88, true),
                new EvalResult(new EvalCase("e3", "absent", true), true, 1.00, true),
                new EvalResult(new EvalCase("e4", "adversarial", true), true, 1.00, true));
        List<EvalResult> candidateResults = List.of(
                new EvalResult(new EvalCase("e1", "happy", false), false, 0.93, true),
                new EvalResult(new EvalCase("e2", "edge", false), false, 0.61, true),
                new EvalResult(new EvalCase("e3", "absent", true), false, 0.40, true),
                new EvalResult(new EvalCase("e4", "adversarial", true), true, 1.00, true));

        EvalSummary baseline = summarize(baselineResults);
        EvalSummary candidate = summarize(candidateResults);
        System.out.printf("  baseline  pass %.2f  faithfulness %.2f  refusal %.2f%n",
                baseline.passRate(), baseline.meanFaithfulness(), baseline.refusalRate());
        System.out.printf("  candidate pass %.2f  faithfulness %.2f  refusal %.2f%n",
                candidate.passRate(), candidate.meanFaithfulness(), candidate.refusalRate());

        List<EvalResult> golden = candidateResults.stream()
                .filter(r -> r.evalCase().category().equals("absent"))
                .toList();
        List<String> failures = regressionGate(candidate, baseline, golden, 0.05);
        System.out.println("  gate decision: " + (failures.isEmpty() ? "PASS" : "BLOCK"));
        failures.forEach(f -> System.out.println("    - " + f));
    }
}
