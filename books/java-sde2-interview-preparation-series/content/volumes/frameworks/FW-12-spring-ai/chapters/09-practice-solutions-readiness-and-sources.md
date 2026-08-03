# Practice, Reasoned Solutions, and Readiness

## Exercises

### 1. Seven-contract rewrite — Foundation

Turn “answer employee HR questions” into task, data, quality, safety, latency, cost, and fallback contracts.

### 2. Context budget — Interview Core

A model supports 16,000 tokens. Reserve 2,000 for output, 1,200 for system/tool definitions, 3,000 for current conversation, and 600 for the user request. How much remains for retrieval? What happens when evidence exceeds it?

### 3. Structured output — Interview Core

Design schema validation, business validation, and authorization for:

```java
record TicketRoute(String team, int priority, String reason) {}
```

### 4. Tenant-safe RAG — Interview Core

The client sends `tenantId` and `conversationId`. Design the trusted query and two negative tests.

### 5. Tool approval — SDE-2 Follow-up

Design an approval record for `refundOrder(orderId, amount)` that remains safe if the order changes before execution.

### 6. Evaluation set — Interview Core

Create six cases: common, exact identifier, missing evidence, conflicting evidence, cross-tenant attack, and prompt injection. Name retrieval and answer metrics.

### 7. Provider outage — SDE-2 Follow-up

Define retry, concurrency, fallback, user response, telemetry, and rollback criteria for a provider 429 incident.

### 8. Memory deletion — Interview Core

List every location that may contain one conversation and define proof of deletion.

## Solutions

### Solution 1

One acceptable contract is: answer HR policy questions for the authenticated employee using only current, employee-visible policy documents; cite supporting sections; never infer individual eligibility or expose another region’s documents; abstain/escalate when evidence is absent/conflicting; p95 complete response under four seconds; enforce a per-user and daily token budget; fall back to filtered document search plus an HR contact. Exact thresholds need organizational evidence.

### Solution 2

Retrieval receives `16,000 - 2,000 - 1,200 - 3,000 - 600 = 9,200` tokens. Do not silently truncate arbitrary text. Rank and deduplicate authorized chunks, preserve the highest-quality evidence within 9,200, and abstain or use an evaluated summarize/second-stage strategy if required evidence cannot fit. Count actual model tokens rather than characters.

### Solution 3

Schema checks ensure `team`, `priority`, and `reason` exist with allowed primitive shapes. Business checks restrict team to the current routing catalog, priority to a defined range, and reason length/content. Authorization checks whether the actor may create or reroute the ticket and whether its tenant/queue is allowed. Persist the original user request and deterministic routing decision evidence; do not save arbitrary model text into privileged fields.

### Solution 4

Ignore client tenant as authority. Derive `(tenantId, userId)` from authentication; verify the server-issued conversation belongs to that scope; query the vector store with mandatory tenant/ACL/active-version filters. Negative tests: a tenant A actor cannot retrieve a known tenant B nearest neighbor; a tenant A actor cannot load tenant B’s conversation even with its valid ID. Also test cache keys.

### Solution 5

Store approval ID, actor/tenant, normalized tool name, order ID, amount/currency, order version, policy version, idempotency key, preview hash, creation/expiry, and approver. At execution, reload order, recheck ownership/state/limits and version, then execute through the stable key. A changed order invalidates or requires renewed approval.

### Solution 6

For each case record expected source IDs or no-answer, required/forbidden claims, tool expectation, and risk. Retrieval: recall@k, first relevant rank, ACL violations. Generation: required-fact coverage, unsupported claims, citation correctness, abstention. Run deterministic isolation/policy checks on every build and semantic review on versioned model/prompt/index configurations.

### Solution 7

Bound concurrent provider calls and queues first; retry only classified 429s with jitter and the remaining deadline. Route to an already evaluated smaller provider/model or retrieval-only response if compliant; otherwise return an explicit temporary-unavailable/escalation result. Measure logical requests, attempts, queue age, rate limits, latency, tokens/cost, and fallback outcome. Roll back a recent model/config change if correlated; do not enable an untested provider during the incident.

### Solution 8

Inventory primary transcript rows, summaries, vector memories, caches, search indexes, tool/audit payloads, logs/traces, evaluation samples, analytics exports, provider retention, replicas, and backups. Delete or tombstone according to policy, prevent re-index/replay, verify primary/index/cache absence under the scoped key, record completion and known backup expiry lag, and test that future retrieval cannot surface the content.

## Final readiness assessment

You are ready for SDE-2 Spring AI discussions when you can:

- treat the model as uncertain and versioned;
- trace `ChatClient`, advisors, model/provider, validation, and streaming boundaries;
- budget context and total deadline;
- evaluate ingestion, retrieval, generation, and outcomes separately;
- enforce tenant filters before retrieval;
- keep tool authorization and idempotency in application code;
- design memory ownership, retention, and deletion;
- define offline gates, canaries, drift response, cost and safe fallback;
- explain an incident without responding only “change the prompt.”

## Official sources

- [Spring AI 2.0 reference](https://docs.spring.io/spring-ai/reference/)
- [Spring AI API overview](https://docs.spring.io/spring-ai/reference/api/)
- [ChatClient and advisors](https://docs.spring.io/spring-ai/reference/api/chatclient.html)
- [Structured output](https://docs.spring.io/spring-ai/reference/api/structured-output-converter.html)
- [Tool calling](https://docs.spring.io/spring-ai/reference/api/tools.html)
- [Vector stores](https://docs.spring.io/spring-ai/reference/api/vectordbs.html)
- [Retrieval-augmented generation](https://docs.spring.io/spring-ai/reference/api/retrieval-augmented-generation.html)
- [Chat memory](https://docs.spring.io/spring-ai/reference/api/chat-memory.html)
- [Observability](https://docs.spring.io/spring-ai/reference/observability/index.html)
- [Spring AI upgrade notes](https://docs.spring.io/spring-ai/reference/upgrade-notes.html)

Recheck provider capability tables and managed artifact versions before implementation; model APIs and limitations move faster than the core engineering principles in this book.
