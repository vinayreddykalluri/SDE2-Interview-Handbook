# 5. The GenAI Design Interview: Method, Worked Cases, and Question Bank

## Learning objectives

By the end of this chapter, you should be able to:

- run a 45-minute generative-AI design interview with a repeatable structure;
- open with constraints and estimates rather than a component diagram;
- work three canonical prompts end to end;
- answer the follow-ups that separate a passing answer from a strong one; and
- recognize which parts of a traditional design answer still carry the interview.

## Why this matters at SDE-2

The failure mode in this round is not ignorance. It is disorganization. Candidates who know every term still lose the round by drawing boxes for thirty minutes and never stating a latency budget, a cost estimate, or a failure mode.

The structure below is the same constraint-first method used for traditional system design in SD-02, with three additions that are specific to a model in the request path: a token budget, a quality-measurement plan, and a degraded mode. If you already interview well on distributed systems, you are adding three moves rather than learning a new game.

## The method

### Minutes 0 to 8 - constraints before components

Do not draw anything yet. Establish:

- **Who asks, how often, and how tolerant are they?** An internal documentation assistant and a customer-facing support bot have different accuracy bars and different blast radii when wrong.
- **What corpus, how large, how fresh?** 50,000 static documents and a stream of support tickets are different problems.
- **What does wrong cost?** A wrong documentation answer wastes a minute. A wrong refund costs money. This single question drives guardrails, human approval, and how conservative retrieval should be.
- **Latency expectation.** Is streaming acceptable? Almost always yes, and it changes the budget from total generation to TTFT.
- **Budget.** If the interviewer has no number, propose one. It shows you know inference is metered.

Then estimate out loud, as in chapter 1: requests per day, tokens per request, cost per day, index size. Two minutes of arithmetic. It is the strongest opening available and most candidates skip it.

### Minutes 8 to 25 - the pipeline

Draw the two pipelines, ingestion and query, and walk them. At each stage name the decision and its alternative:

```text
INGEST:  source -> extract -> chunk -> embed -> index (+metadata, +ACL)
QUERY:   question -> embed -> hybrid search (ACL pushed down)
                  -> rerank -> assemble -> generate -> validate -> stream
```

State a choice and a reason at every hop. "Chunking at 600 tokens on section boundaries with headers prefixed, because the corpus is structured documentation and a bare paragraph is not self-interpretable." Not "we chunk the documents."

Bring up access control unprompted. It is the highest-severity issue in the design and interviewers notice who raises it first.

### Minutes 25 to 35 - failure and quality

This is where strong candidates separate. Cover, without being asked:

- **Degraded mode.** Provider outage, rate limiting, slow responses. Name the non-generative fallback.
- **Wrong answers.** The five-stage diagnosis from chapter 2.
- **Evaluation.** Eval set, metrics, regression gate, shadow evaluation.
- **Guardrails.** Input and output, as controls rather than prompt instructions.

### Minutes 35 to 45 - depth and trade-offs

The interviewer will push on one area. Have a position and its cost:

- "Why not fine-tune instead?" Because the corpus changes daily, fine-tuning cannot enforce per-user authorization, and RAG gives citations. Fine-tuning is for format and tone, not for facts.
- "How would you cut cost by half?" Cap output tokens, trim retrieved chunks, restructure the prompt for prefix caching, route easy queries to a small model, add a semantic cache. In that order, because that is roughly descending leverage per unit of risk.
- "What if the corpus were a thousand times larger?" Now index choice, sharding, and ANN recall tuning matter. Say what changes and what does not.

## Worked case 1: internal documentation assistant

*"Design an assistant that answers employee questions over internal documentation."*

**Constraints.** 20,000 employees, 15% weekly usage, 4 questions each. 50,000 documents, updated continuously, with per-team access restrictions. Wrong answers waste time but are recoverable. Streaming acceptable.

**Estimates.** ~1,700 questions/day, ~2 rps peak. 3,100 input and 500 output tokens gives roughly $840/month. 400,000 chunks at 1,536 dimensions is about 2.4 GB - fits in memory, so no distributed vector store.

**Design.** Structural chunking on headings at 600 tokens with headers prefixed. Hybrid BM25 plus dense retrieval fused with RRF, with the team-permission predicate pushed into the query. Cross-encoder rerank from 50 to 5, affordable inside the 1s TTFT target. Prompt with strongest chunks at the edges, enforced citations, code-side citation validation. Stream over SSE.

**Freshness.** Webhook on document change re-embeds affected chunks; nightly reconciliation catches missed deletions.

**Degraded mode.** Model unavailable: return the ranked passages with highlights. The product degrades to search.

**Quality.** Eval set seeded from the top 100 real support questions, plus absent-answer and injection cases. Gate on golden set and aggregate faithfulness. Alert on refusal rate as a band.

**The likely follow-up: "an employee saw a document they should not have."** This is an incident, not a bug. Answer: verify whether the ACL predicate was pushed into the query or applied after; check whether permissions in the index were stale relative to the source system; add the adversarial authorization test to CI; and consider re-checking permissions at answer time against the live source rather than trusting indexed metadata, accepting the added latency.

## Worked case 2: customer support agent with actions

*"Design a support agent that can look up orders and issue refunds."*

**Open by narrowing the scope.** Refunds follow a fixed policy, so the control flow belongs in code. Use the model for intent classification, entity extraction, and drafting the reply. That answer alone is a strong signal - if the interviewer wants the agentic version, they will say so, and you have already shown judgment.

**If agentic.** Narrow tools: `get_order`, `get_policy`, `issue_refund`, `escalate_to_human`. No general query tool.

**Authorization.** Every tool authorizes on the *customer's* identity, not the agent's. Enforced in the tool, never in the prompt.

**Idempotency.** `issue_refund` takes a key derived from run id and canonical arguments. Calling twice returns the original result.

**Ordering.** Validate, check policy, check amount, then refund last - after everything that can fail has already succeeded. Minimizes compensation.

**Human approval** above a threshold amount, and for any refund on an account flagged for review.

**Bounds.** 10 iterations, 50,000 tokens, 60 seconds, $0.50, plus repetition detection.

**Injection.** Ticket bodies are attacker-controlled. The agent has no email tool, so the classic exfiltration path does not exist. Escalation writes to an internal queue rather than sending outbound mail.

**The likely follow-up: "it issued two refunds."** Non-idempotent tool plus a retry - either the model re-called it or the network failed after the effect. Fix at the tool boundary with the idempotency key, and add the trajectory to the eval set as a regression case.

## Worked case 3: semantic search over a large corpus

*"Design semantic search over 100 million documents."*

**Recognize the shape.** This may not need generation at all. Ask whether users want passages or a synthesized answer. If passages, this is a retrieval problem and the model is optional - saying so is a strong answer.

**Scale changes the calculus.** 100M documents at 8 chunks each is 800M vectors. At 1,536 dimensions that is roughly 4.9 TB of raw vectors. Now index choice matters:

- Dimensionality reduction or quantization to cut memory materially
- Sharding by tenant or corpus partition, with scatter-gather
- IVF with tuned `nprobe`, or HNSW if memory allows
- Measured recall@k per shard, because ANN recall is a tunable and not a guarantee

**Two-stage retrieval** becomes essential: cheap ANN to a few hundred candidates, then rerank. The reranker cost is now the dominant latency term and caps how many candidates you can afford.

**Freshness at this scale** is its own subsystem: streaming ingestion, index build pipeline, and periodic full rebuilds, with the read path served from an immutable snapshot so queries are not racing writes.

**The likely follow-up: "recall dropped after re-indexing."** Compare recall@k on a labelled set before and after; check whether ANN parameters changed; check whether the embedding model version changed, which invalidates comparisons entirely; and check shard balance, since a hot shard with an over-tight `nprobe` can degrade recall in one partition while the aggregate looks acceptable.

## Question bank

**Design prompts**

1. A ChatGPT-style assistant over a company's Confluence and Google Drive.
2. An LLM-backed code review assistant on pull requests.
3. A RAG system over legal contracts where citation accuracy is a compliance requirement.
4. A multi-agent travel planner with flight, hotel, and calendar tools.
5. Semantic search across 100 million product listings.
6. An assistant that drafts customer emails, with human approval before sending.
7. A meeting summarizer producing action items into a task tracker.
8. An on-call assistant that reads runbooks and proposes remediation.

**Depth follow-ups**

9. Why RAG rather than fine-tuning? When is fine-tuning correct?
10. Cut inference cost by half without materially hurting quality.
11. Enforce per-document access control in retrieval, and prove it.
12. Your assistant confidently answers questions the corpus does not cover. Fix it.
13. Users say quality dropped last week. Nothing was deployed. Investigate.
14. Design the eval set and regression gate for a change to the system prompt.
15. Prevent indirect prompt injection through retrieved content.
16. Migrate to a new embedding model with no quality regression.
17. Choose between exact and approximate search, with the numbers.
18. The agent loops until it hits the token cap. Diagnose and bound it.
19. Guarantee valid JSON output for a downstream service.
20. The provider has a two-hour outage. What do users see?

## Failure modes and common mistakes

- Drawing components before establishing constraints.
- Never stating a token budget or cost estimate.
- Not mentioning access control until asked.
- Presenting an agent where a fixed workflow is obviously correct.
- No degraded mode, leaving a third-party API as a hard dependency.
- No evaluation plan, so quality is unfalsifiable.
- Guardrails described as prompt instructions rather than controls.
- Reaching for a distributed vector database without computing the index size.
- Treating LLM-as-judge as ground truth.
- Proposing multi-agent architectures for problems a single agent handles.
- Ignoring cost entirely, the most common single omission.
- Forgetting that changing the embedding model requires a full re-index.

## Interview questions and model answers

**Where do you start in a GenAI design round?**

Constraints and arithmetic, before any diagram. Who asks, how often, what corpus, what a wrong answer costs, latency expectation, budget. Then estimate requests per day, tokens per request, cost per day, and index size out loud. That establishes which parts of the design are actually load-bearing - and often shows the problem is smaller than it sounds.

**Why RAG rather than fine-tuning?**

Fine-tuning bakes knowledge into weights: expensive to update, impossible to authorize per user, and it produces no citations. RAG retrieves at request time, so the corpus can change continuously, permissions can be enforced in the query, and every claim can be traced to a source. Fine-tuning is right for format, tone, and domain style - not for facts.

**What single thing most improves a struggling RAG system?**

Usually retrieval, not the prompt or the model. Specifically: hybrid retrieval with RRF, and prefixing chunks with their document and section context. Both are cheap and reliably move recall. But measure first with the five-stage diagnosis, because if the document was never ingested, no retrieval change helps.

**What makes this different from a traditional design interview?**

Three additions. A token budget, because cost and latency both scale with payload. A quality-measurement plan, because correctness is a distribution rather than an assertion. And a degraded mode, because the model is a third-party dependency with rate limits you do not control. Everything else - capacity, sharding, caching, failure domains, observability - is the design interview you already know.

## Chapter summary

The round is won on structure, not vocabulary. Spend the first eight minutes on constraints and arithmetic; state requests per day, tokens per request, cost, and index size before drawing anything. Walk both pipelines with a stated reason at every hop, and raise access control before you are asked, because it is the highest-severity failure available. Reserve a third of the time for degraded mode, wrong-answer diagnosis, evaluation, and guardrails, since that is where candidates separate. Have a position on the standard follow-ups - fine-tuning versus retrieval, halving cost, scaling the corpus - and state the cost of each position. The method is the constraint-first approach from traditional system design plus exactly three moves: a token budget, a quality-measurement plan, and a fallback for when the model is unavailable.

## Revision checklist

- [ ] I open with constraints and arithmetic, not components.
- [ ] I can estimate tokens, cost, and index size in under two minutes.
- [ ] I raise access control before being asked.
- [ ] I state a decision and its alternative at every pipeline hop.
- [ ] I reserve time for degraded mode, evaluation, and guardrails.
- [ ] I can argue RAG versus fine-tuning in both directions.
- [ ] I can list cost reductions in descending order of leverage.
- [ ] I know what changes when the corpus grows a thousandfold, and what does not.
- [ ] I question whether an agent is needed before designing one.
- [ ] I can run all three worked cases end to end from memory.
