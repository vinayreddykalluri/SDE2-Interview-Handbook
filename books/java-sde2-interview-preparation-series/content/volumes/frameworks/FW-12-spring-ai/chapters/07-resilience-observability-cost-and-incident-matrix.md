# Resilience, Observability, Cost, and Incident Response

An AI request can fail before DNS, during connection acquisition, at a provider rate limit, after partial streaming, during tool execution, in schema conversion, or because the answer is confidently wrong. “Catch exception and retry” covers only a small part of reliability.

## End-to-end request budget

```text
authentication/policy        40 ms
retrieval + rerank          350 ms
model call                2,800 ms
validation/repair           450 ms
response reserve            360 ms
                          --------
total                      4,000 ms
```

A repair call, tool loop, or provider retry must use the remaining deadline. Do not give every internal call a fresh four seconds. For streaming, define both time-to-first-token and total completion limits.

## Retry classification

Potentially retryable within a bounded budget: connection reset before a known side effect, provider 429 with appropriate backoff, selected 5xx failures, or schema repair when the use case permits it.

Not blindly retryable: invalid credentials, policy rejection, oversized input, unsupported model option, an unsafe/malformed tool request, or a timed-out side-effecting tool without idempotency.

Use exponential backoff with jitter and concurrency limits. A retry storm can turn a small provider failure into a fleet-wide cost and latency incident.

## Fallbacks by meaning

| Fallback | Safe when | Dangerous when |
|---|---|---|
| Smaller/alternate model | Evaluated for this task and data region | Behavior/quality differs without gate |
| Retrieval-only source list | User can inspect evidence | Product claims it is a complete answer |
| Cached answer | Source/version/tenant key and staleness contract hold | Personalized or rapidly changing data |
| Deterministic FAQ/search | Query matches supported path | It silently answers a different task |
| Human escalation | Queue and response-time contract exist | “Escalated” disappears into nowhere |
| Explicit unavailable/abstain | Correctness matters more than completion | Product masks availability goals |

A fallback is product behavior, not an exception handler detail.

## Cache keys

AI response caching requires more than user text. A safe key may include:

- normalized task/input;
- tenant and authorization scope or public-data marker;
- prompt and policy version;
- model/options version;
- retrieval corpus/index version;
- locale;
- tool/data freshness dependencies.

Never share personalized responses across tenants. Semantic caching adds false-match risk and needs its own evaluation threshold.

## Observability without leaking prompts

Record low-cardinality operational fields:

- feature/route and model/provider identifier;
- prompt/policy/retriever/tool-schema version;
- request outcome: success, abstain, denied, validation-failed, timeout, rate-limited;
- latency by retrieval, model, tool, validation, and total;
- input/output token counts and estimated cost;
- retry/repair/tool iteration counts;
- retrieval hit count and bounded score summary;
- safety decision category;
- trace correlation to protected, access-controlled diagnostic data.

Do not put user text, document chunks, conversation IDs, user IDs, or tool arguments in metric labels. Prompt/response logging should be off or heavily controlled, redacted, sampled, encrypted, retained briefly, and access-audited.

## Capacity and cost controls

Bound request size, output tokens, concurrent provider calls, tool loops, queue depth, per-tenant rate, daily budget, and maximum expensive-model routing. Backpressure/rejection is safer than an unbounded queue whose work completes after users leave.

Track cost per successful **business outcome**, not only cost per token. A cheaper model that doubles corrections can be more expensive.

## Incident matrix

| Incident | Likely layer | Evidence | Immediate containment | Durable repair |
|---|---|---|---|---|
| Answers cite another tenant | Retrieval filter/cache/memory | Source IDs, scope, version | Disable feature/index; notify security | Server-derived scope, isolation tests, cache-key repair |
| Correct source not found | Ingestion/retrieval | Ingestion reconciliation, recall@k | Search fallback/escalate | Chunking/hybrid/index fix and regression case |
| JSON failures spike | Model/prompt/schema change | Finish reason, schema version, repair count | Pin/rollback model or buffer | Capability contract tests and bounded validation |
| Tool executes duplicate refund | Timeout/retry/idempotency | Operation key and provider receipts | Disable write tool/reconcile | Stable server key and approval-bound workflow |
| p99 and cost surge | retries, long context, tool loop | attempts/tokens/stage latency | concurrency/loop cap, route fallback | Budget policy and load evaluation |
| Streaming leaks unsafe partial text | late full-response safety check | chunk timeline | Stop streaming route | pre-check plus safe streaming protocol/buffer |
| Quality drops without deploy | provider alias/index drift | model response metadata/index freshness | pin/rollback/canary | versioned dependencies and drift gate |
| Provider unavailable | external dependency | status/timeout/rate-limit | evaluated fallback or explicit outage | multi-provider contract only if justified |
| Deletion complaint finds old data | privacy lifecycle gap | store inventory and lineage | restrict access/delete reachable copies | end-to-end deletion workflow and SLO |
| Prompt injection triggers tool attempts | content + broad tools | denied tool telemetry | remove write tools/feature | minimal exposure, authz, attack eval |

## Operational runbook questions

1. Can we disable one tool, provider, tenant, model route, or retrieval index independently?
2. Can we identify the exact behavior bundle for a request without storing raw content?
3. Can we roll back prompt/model/index versions?
4. Can we reconcile every side effect by operation key?
5. Does fallback preserve privacy, tenant, and regional data constraints?
6. Which quality failures require user notification or human review?

## Quick check

1. Why must schema repair consume the original deadline?
2. What belongs in a tenant-safe cache key?
3. Why can streaming expose content before a full-response check?
4. Which metrics reveal a tool loop?
5. Why is multi-provider fallback not automatic reliability?

## Practice

- **Foundation:** Classify eight failures as retry, fallback, abstain, deny, or escalate.
- **Interview Core:** Design a safe cache key and invalidation rule for public policy Q&A.
- **Interview Core:** Create a four-second stage budget and overload policy.
- **SDE-2 Follow-up:** Write the first 30 minutes of response to a cross-tenant citation incident.
