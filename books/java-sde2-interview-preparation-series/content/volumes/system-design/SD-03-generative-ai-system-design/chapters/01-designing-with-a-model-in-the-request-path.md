# 1. Designing With a Model in the Request Path

## Learning objectives

By the end of this chapter, you should be able to:

- treat inference latency, token cost, and non-determinism as first-class capacity constraints rather than implementation details;
- estimate the cost and latency envelope of an LLM-backed feature before writing any code;
- explain time-to-first-token, inter-token latency, and why streaming changes the perceived latency budget;
- choose between model sizes, routing tiers, and caching layers with a stated reason; and
- design the degraded mode that runs when the model is slow, wrong, or unavailable.

## Why this matters at SDE-2

System design interviews used to have one shape. They now have three: the traditional prompt, the ML prompt, and the generative-AI prompt. The third moved from niche to routine in under two years, and it is the one most engineers have not rehearsed.

The trap is treating the model as an ordinary downstream service. It is not. A database call has a latency distribution you can bound and a cost that does not vary with the *content* of the response. An LLM call has a latency proportional to the number of tokens it decides to generate, a cost that scales with both input and output length, and an output that is correct in a probabilistic rather than a deterministic sense. An engineer who says "we call the model, cache the response, and add a circuit breaker" has described the plumbing and missed every constraint that makes the problem interesting.

The signal an interviewer is looking for is whether you can reason about a component whose latency, cost, and correctness are all variable, and still produce a system with a defensible service-level objective.

## First-principles model

An LLM in a request path is a **remote, stateless, non-deterministic, token-metered function** with an unusually wide latency distribution.

Four properties follow, and each one breaks an assumption that ordinary backend design relies on:

**Latency is proportional to output length.** A response is generated one token at a time. Total latency is roughly `TTFT + (output_tokens x inter_token_latency)`. A 50-token answer and a 2,000-token answer to the same prompt differ by an order of magnitude in wall time. You cannot bound latency without bounding output length, which means `max_tokens` is a capacity control, not a formatting preference.

**Cost is metered on both directions, asymmetrically.** Providers bill input and output tokens separately, and output is typically several times more expensive. A design that stuffs 30,000 tokens of context into every request has made a capacity decision whether or not anyone wrote it down.

**Context is finite and shared.** Everything - system prompt, retrieved documents, conversation history, tool definitions, the user's question - competes for one window. Growth in any of them silently evicts the others unless you budget explicitly.

**Output is probabilistic.** The same input can produce different output. This is not a bug to be engineered away; it is the operating characteristic. Design accordingly: validate structure, constrain format, and never place unvalidated model output into a privileged position.

> **Specification boundary:** Nothing here is specified by a standard. Token pricing, context limits, rate limits, and latency characteristics are vendor product decisions that change on the vendor's schedule, sometimes without a version bump. Treat every number in a design as a measured input with an expiry date, and build the system so a model swap is a configuration change rather than a rewrite.

## Core terminology

- **Token:** the unit of text the model consumes and produces; roughly 3 to 4 characters of English.
- **Context window:** the maximum combined input and output tokens for one request.
- **TTFT (time to first token):** latency until the first output token arrives.
- **Inter-token latency:** time between successive output tokens; determines streaming speed.
- **Prefill:** the phase that processes the input prompt; cost scales with input length.
- **Decode:** the phase that generates output one token at a time; dominates wall time.
- **KV cache:** attention state retained during decode; the reason prefix caching works.
- **Prompt prefix caching:** provider-side reuse of an identical leading prompt segment, billed at a discount.
- **Semantic cache:** a cache keyed by embedding similarity rather than exact string match.
- **Model routing:** dispatching a request to a cheaper or costlier model based on assessed difficulty.
- **Guardrail:** a check applied to input or output independent of the model.
- **Eval:** an offline test set with scored expectations, the regression suite for a probabilistic system.

## Detailed mechanics

### Estimating before designing

Interviewers reward a candidate who produces a capacity envelope early, exactly as they would for a traditional design. The arithmetic is simple and almost nobody does it.

Take a support-assistant feature: 500,000 daily active users, 8% use the assistant, 3 turns per session.

```text
requests/day   = 500,000 x 0.08 x 3          = 120,000
requests/sec   = 120,000 / 86,400            ~ 1.4 average
peak (10x)                                   ~ 14 rps
```

Now the token budget per request:

```text
system prompt              400 tokens
retrieved context (5 x 400)  2,000 tokens
conversation history       600 tokens
user question              100 tokens
-------------------------------------------
input                    ~ 3,100 tokens
output (capped)            500 tokens
```

Cost follows directly. At an illustrative $3 per million input tokens and $15 per million output tokens:

```text
input   120,000 x 3,100 = 372M tokens/day  -> 372 x $3   = $1,116/day
output  120,000 x   500 =  60M tokens/day  ->  60 x $15  =   $900/day
                                              total     ~ $2,016/day
                                                        ~ $736K/year
```

That number changes the conversation. It justifies the caching layer, the routing tier, and the argument for trimming retrieved context from five chunks to three. Stating it unprompted is a strong senior signal; it is the generative-AI equivalent of estimating storage before choosing a database.

And note which term dominates: output is 16% of the tokens but 45% of the cost. Capping `max_tokens` is the highest-leverage cost control available, and it is one line of configuration.

### Latency budgeting and streaming

A 500-token response at 30 tokens/sec takes about 17 seconds to complete. No product tolerates a 17-second blocking call, which is why streaming is architectural rather than cosmetic.

```text
non-streaming: user waits 17s, sees everything at once
streaming:     user sees first token at ~400ms, reads as it generates
```

The perceived latency becomes TTFT, not total generation time. This has consequences that propagate through the whole stack:

- The transport must stream. Server-Sent Events is the common choice; a JSON response body cannot deliver partial results.
- Every hop in between must stream. A load balancer, gateway, or service-mesh proxy that buffers the full response destroys the benefit while leaving the code looking correct.
- Timeouts must distinguish TTFT from total duration. A single 30-second timeout cannot tell a healthy long answer from a stalled connection. Bound TTFT tightly (2 to 3 seconds) and total generation loosely.
- Cancellation must propagate. A user who closes the tab should stop the generation, because you are billed for tokens produced after they left.

Retrieval sits in front of all of this and adds to TTFT:

```text
embed query        ~  30ms
vector search      ~  20ms
rerank (optional)  ~  80ms
model TTFT         ~ 400ms
--------------------------------
first token        ~ 530ms
```

Reranking nearly triples pre-model latency. That is a real trade-off against answer quality, and being able to name it is the point.

### Caching, and why the obvious cache does not work

An exact-match response cache has a poor hit rate here. "How do I reset my password?" and "how can I change my password" are the same question and different cache keys.

Three caching layers exist, and they are not alternatives - a mature design uses all three:

**Exact-match cache.** Hash the full normalized prompt. Cheap, safe, low hit rate. Worth having; do not expect much.

**Prompt prefix caching.** Providers can cache the KV state of an identical leading prompt segment and bill it at a large discount. This is free money if you structure prompts correctly: put the stable content first (system prompt, tool definitions, few-shot examples) and the variable content last (retrieved chunks, user question). Teams routinely lose this by interpolating a timestamp near the top of the system prompt, which invalidates the prefix on every request.

**Semantic cache.** Embed the query, search previous queries by cosine similarity, and return the stored answer above a threshold. Much higher hit rate, and genuinely risky: too low a threshold and you confidently answer a question the user did not ask. Never share a semantic cache across users or tenants without the identity in the key, and never cache anything personalized.

### Model routing

Not every request needs the largest model. Routing sends easy work to a small, fast, cheap model and escalates only when needed.

```text
classify difficulty (small model or heuristic)
  |
  +-- simple lookup, FAQ  -> small model   ($, ~200ms TTFT)
  +-- reasoning, synthesis -> large model  ($$$, ~600ms TTFT)
  +-- low confidence       -> escalate, or hand off to a human
```

The honest trade-off: routing adds a classification hop to every request and introduces a new failure mode where the router itself is wrong. It pays off when the traffic mix is genuinely bimodal, which for support and search workloads it usually is. Measure the mix before assuming it.

### Degraded mode

This is the question that separates candidates. **What does your system do when the model is down, rate-limited, or slow?**

An LLM provider is a third-party dependency with rate limits you do not control and outages you cannot escalate. The design must answer:

- **Rate limited (429).** Retry with backoff *and jitter*, and shed load rather than queue unboundedly. A queue in front of a rate limit converts a fast failure into a slow one.
- **Provider outage.** Fail over to a secondary provider if prompts are portable, or degrade to non-generative behavior - return the retrieved documents directly, surface a search results list, route to a human.
- **Slow.** Enforce the TTFT timeout and fall back rather than holding the connection.
- **Wrong.** Guardrails and structural validation, covered in chapter 4.

The strongest answer names the non-generative fallback explicitly. A retrieval-augmented assistant that loses its model can still show the retrieved passages. That is a materially useful product, and designing for it means the model becomes an enhancement rather than a single point of failure.

## Worked example: sizing a documentation assistant

Requirements: answer questions over 50,000 internal documents, under 1 second to first token at p95, budget under $2,000 per month.

**Step 1 - traffic.** 20,000 employees, 15% weekly usage, 4 questions each:

```text
12,000 questions/week -> ~1,700/day -> ~0.02 rps average, ~2 rps peak
```

Low. This system is not throughput-constrained, which immediately rules out a lot of complexity and is worth saying out loud.

**Step 2 - budget check.** At 3,100 input and 500 output tokens:

```text
1,700 x (3,100 x $3 + 500 x $15) / 1,000,000 = ~$28/day = ~$840/month
```

Inside budget with room. If it had not been, the levers in order of leverage are: cap output tokens, retrieve 3 chunks instead of 5, add a semantic cache, route easy questions to a small model.

**Step 3 - latency.** Target p95 TTFT under 1s against the ~530ms pipeline above leaves ~470ms of headroom. Reranking fits, but only just. Decide deliberately: keep it and measure, or drop it and accept lower retrieval precision.

**Step 4 - storage.** 50,000 documents at ~8 chunks each is 400,000 vectors. At 1,536 dimensions and 4 bytes per float, that is about 2.4 GB of raw vectors plus index overhead. This fits in memory on one machine. A distributed vector database is not justified, and saying so is a stronger answer than reaching for one.

**Step 5 - degraded mode.** Model unavailable: return the top retrieved passages with highlighted matches and a banner. The assistant becomes a search engine, which is what it was before anyone added a model.

## Failure modes and common mistakes

- Treating the model as a normal service with a fixed latency budget.
- Leaving `max_tokens` unbounded, so a single request can run for minutes and cost dollars.
- Buffering the stream at a proxy, silently discarding the entire benefit of streaming.
- One timeout for TTFT and total duration, unable to distinguish stalled from long.
- Not cancelling generation when the client disconnects, and paying for unread tokens.
- Putting variable content early in the prompt, defeating provider prefix caching.
- Sharing a semantic cache across users or tenants, leaking one user's answer to another.
- Setting a semantic-cache similarity threshold by intuition and never measuring the false-hit rate.
- Sizing capacity in requests per second while ignoring tokens per second, which is the real limit.
- Queueing in front of a provider rate limit, converting fast failures into timeouts.
- No degraded mode, making a third-party API a hard dependency for a core product surface.
- Retrying a non-idempotent agentic request and executing its side effects twice.
- Assuming vendor latency and pricing are stable enough to hardcode.

## Interview questions and model answers

**How do you bound the latency of an LLM-backed endpoint?**

Cap `max_tokens`, because total latency scales with output length. Stream, so perceived latency becomes TTFT rather than total generation. Set separate timeouts for TTFT and total duration, since one timeout cannot distinguish a stalled connection from a long answer. Verify nothing in the path buffers the stream.

**Your inference bill is triple the forecast. Where do you look?**

Token volume per request first, since that is usually the cause: unbounded output, oversized retrieved context, or full conversation history resent every turn. Then cache effectiveness, particularly whether prompt prefix caching is being defeated by variable content early in the prompt. Then the traffic mix, to see whether requests that could be served by a small model are hitting the large one. Cost per request is more actionable than total cost.

**How do you cache responses when semantically identical questions have different wording?**

Three layers. Exact-match on a normalized prompt for the cheap wins. Provider prefix caching, which requires stable content first in the prompt. And a semantic cache keyed on query embedding similarity - which needs a measured threshold, per-user or per-tenant key scoping, and an exclusion for anything personalized, because a false hit answers a question the user did not ask.

**What happens when the model provider has an outage?**

Degrade rather than fail. For a retrieval-augmented system, return the retrieved passages directly - the product becomes search, which is still useful. Optionally fail over to a second provider if the prompts are portable. Shed load rather than queue on rate limits. The design goal is that the model is an enhancement on top of a system that works without it.

**How is designing this different from designing a normal read-heavy service?**

Three constraints do not exist in ordinary services: latency scales with output length rather than being roughly fixed; cost is metered per token in both directions, so payload size is a budget line; and output is non-deterministic, so correctness is a distribution rather than an assertion. That last one is why evaluation replaces unit testing as the primary quality gate.

## Exercises

1. Estimate daily cost and peak tokens/sec for a customer-facing chat feature with 2 million MAU, 20% monthly usage, 5 turns per session. State every assumption.
2. Take a system prompt containing a current timestamp and restructure it to preserve prefix caching. Explain which segments must be stable.
3. Design the timeout and cancellation policy for a streaming endpoint, distinguishing TTFT from total duration and specifying client-disconnect behavior.
4. Propose a semantic cache for a multi-tenant assistant. Define the key, the threshold, the exclusions, and how you would measure the false-hit rate.
5. Write the degraded-mode specification for a documentation assistant covering rate limiting, provider outage, and slow responses.
6. A design retrieves 20 chunks of 800 tokens each. Compute the cost impact of reducing to 5 chunks, and state what you would measure to know whether quality regressed.

## Chapter summary

An LLM in a request path is a remote, non-deterministic, token-metered function whose latency scales with output length. That single fact drives the design: cap output tokens because they bound both latency and cost, stream so perceived latency collapses to TTFT, and verify no hop buffers the response. Estimate the token budget and monthly cost before designing anything, because the number decides whether caching, routing, and context trimming are necessary. Cache in three layers - exact match, provider prefix, semantic - and understand that a semantic cache trades a higher hit rate for the risk of confidently answering the wrong question. Above all, specify the degraded mode: a retrieval-augmented system whose model is unavailable can still return its retrieved passages, and designing for that turns a hard third-party dependency into an enhancement.

## Revision checklist

- [ ] I can estimate tokens, cost, and peak throughput for a described feature.
- [ ] I know why output tokens dominate cost despite being a minority of volume.
- [ ] I can explain TTFT, inter-token latency, and why streaming is architectural.
- [ ] I know that a buffering proxy silently negates streaming.
- [ ] I can set separate TTFT and total-duration timeouts and justify both.
- [ ] I can describe all three caching layers and the risk each carries.
- [ ] I can structure a prompt to preserve provider prefix caching.
- [ ] I can argue for or against model routing using the traffic mix.
- [ ] I can specify degraded behavior for rate limiting, outage, and slowness.
- [ ] I can state which vendor numbers in my design will expire and need remeasuring.
