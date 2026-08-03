# Evaluation: Turn “Looks Good” into a Release Gate

Deterministic unit tests remain necessary for prompt assembly, filters, schemas, authorization, and tool adapters. They are not enough to evaluate probabilistic answer quality.

## Build an evaluation set

Each case should contain enough reviewable evidence:

```text
case ID
task/input and actor/tenant scope
allowed source IDs or expected no-answer
required facts and forbidden claims
expected tool/no-tool and allowed arguments
risk category
scoring rubric
```

Use representative production-like cases with protected/synthetic data:

- ordinary frequent questions;
- rare but important policies;
- missing evidence and conflicting evidence;
- exact identifiers/numbers;
- multilingual or noisy inputs where in scope;
- prompt injection and data-exfiltration attempts;
- unauthorized tenant/tool cases;
- long context and provider timeout;
- regression cases from real incidents.

Do not tune only on the final test set. Maintain development and held-out gates, and review leakage when evaluation examples enter prompts or retrieval indexes.

## Separate stage metrics

```text
ingestion -> retrieval -> generation -> validation -> user outcome
```

| Stage | Useful measures |
|---|---|
| Ingestion | coverage, failures, freshness lag, duplicate/stale versions |
| Retrieval | recall@k, precision@k, first relevant rank, ACL violations |
| Generation | grounded claims, citation correctness, completeness, abstention |
| Structured output | schema-valid rate, repair attempts, business-valid rate |
| Tools | correct selection, valid args, denied unsafe calls, effect success/duplicate |
| Operations | latency distribution, timeouts, rate limits, tokens/cost, fallback |
| User outcome | resolution, correction/escalation, satisfaction with bias controls |

One aggregate “accuracy” number hides the failure stage.

## Deterministic checks first

Examples:

- cited IDs are a subset of retrieved IDs;
- a required field is present and within range;
- no tool executes without authenticated actor context;
- cross-tenant retrieval count is zero;
- total attempts and tool iterations stay within caps;
- response carries an abstention flag when evidence list is empty.

Then use human or model-assisted rubrics for semantic qualities such as groundedness and completeness. A model-as-judge is another probabilistic dependency: calibrate it against human labels, detect position/style bias, pin versions/options, and sample disagreements for review.

## Thresholds and trade-offs

For a support answer:

- false confident answer may be worse than abstention;
- too much abstention harms usefulness;
- stricter retrieval threshold can improve precision and reduce recall;
- larger top-k can increase evidence recall but add distractors and cost.

Choose thresholds from the task’s harm model. Publish both failure rates and sample sizes, with confidence ranges where decisions are sensitive.

## Online evaluation and feedback

Production feedback is not automatically ground truth. A thumbs-up can reflect tone rather than correctness; only unhappy users may respond. Use:

- outcome-based metrics where causal interpretation is reasonable;
- explicit correction/escalation reasons;
- sampled expert review;
- shadow/canary comparison;
- rollback thresholds;
- privacy-reviewed trace replay;
- drift monitoring by task segment.

Never train directly on all feedback without abuse, privacy, quality, and provenance controls.

## Safety testing

Threat categories include:

- direct and indirect prompt injection;
- sensitive-data extraction;
- cross-tenant retrieval;
- tool privilege escalation;
- harmful/illegal content relevant to domain policy;
- insecure code or fabricated commands;
- denial of wallet/service through large prompts or loops;
- model/provider outage and degraded fallback;
- supply-chain changes in model, prompt, tool, retriever, or MCP server.

Safety filters are one layer. Application authorization and data isolation remain mandatory.

## Release gate

Version the complete behavior bundle:

```text
application code
prompt/template
model + provider options
advisor order
embedding model + index version
retrieval/reranker settings
tool schemas/policies
evaluation dataset + scorer versions
```

Run the gate on any material change. A provider model alias that moves behind the same name is a behavior change; use pinned versions where available and monitor drift.

Example gate:

```text
0 cross-tenant retrieval/tool violations
100% deterministic policy/schema checks
>= 92% required-fact coverage on held-out core cases
<= 3% unsupported-claim rate on high-risk cases
>= 85% correct abstention on unanswerable cases
p95 latency <= 4 s and cost/request <= agreed budget
no regression > agreed margin in any protected segment
```

Numbers here are illustrative; derive real thresholds from risk and baseline evidence.

## Failure and edge-case matrix

| Evaluation mistake | Why it misleads | Repair |
|---|---|---|
| Ten hand-picked demos | Selection bias | Representative versioned dataset |
| Exact-string expected answer | Penalizes valid wording | Facts/rubric plus deterministic structure |
| Judge model agrees with itself | Shared bias | Human calibration and diverse checks |
| Only answer quality measured | Broken retrieval hidden | Stage-specific metrics |
| Average score only | High-risk tail hidden | Segment and severity gates |
| Test set used during tuning | Optimistic result | Held-out gate and leakage controls |
| No no-answer cases | Hallucination not tested | Abstention calibration set |
| Latency measured without retries | Tail/cost understated | Logical request and attempt-level metrics |
| Model alias silently changes | Drift without code deploy | Version/response metadata and canary |

## Quick check

1. Why are exact-string assertions weak for generated prose?
2. What does recall@k measure before generation?
3. Why is model-as-judge not ground truth?
4. What should be versioned with a release?
5. How can a higher retrieval threshold harm recall?

## Practice

- **Foundation:** Write five gold cases including one no-answer case.
- **Interview Core:** Separate retrieval and generation metrics for those cases.
- **Interview Core:** Define deterministic checks for cited sources and tool arguments.
- **SDE-2 Follow-up:** Create a canary/rollback plan for a model upgrade with protected-segment gates.
