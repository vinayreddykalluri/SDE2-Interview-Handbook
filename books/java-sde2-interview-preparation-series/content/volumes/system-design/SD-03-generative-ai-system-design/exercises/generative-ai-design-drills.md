# Generative AI Design Drills

Work these in order. Each drill states the artifact you should produce, because "think about it" is not practice. Solutions follow in the companion file; write your own answer first, then compare - the gap between the two is the actual lesson.

## Foundation: constraints and arithmetic

**D1. Size a feature end to end.**
A retail company adds an LLM-backed shopping assistant. 3 million monthly active users, 12% use the assistant monthly, 4 turns per session. Retrieval supplies 6 chunks of 350 tokens. System prompt is 500 tokens, history averages 700, questions average 80. Output is capped at 400.
*Produce:* requests/day, peak rps at 8x, input and output tokens/day, cost/day and cost/month at $3 and $15 per million, and the output share of total cost. State every assumption.

**D2. Bring the bill down.**
The result of D1 is over budget by half.
*Produce:* an ordered list of five cost reductions, each with its expected saving and the quality or latency risk it carries. Order by leverage per unit of risk and defend the ordering.

**D3. Budget the latency.**
Target p95 TTFT under 800ms. Measured stages: embed 35ms, vector search 25ms, rerank 90ms, model TTFT 450ms.
*Produce:* the TTFT total, whether it fits, and two options if it does not. State what each option costs in quality.

**D4. Design the timeout policy.**
*Produce:* separate TTFT and total-duration timeouts with justified values, the client-disconnect behaviour, and the retry policy. Explain why one timeout is insufficient.

## Retrieval

**D5. Chunk three corpora.**
Sources: OpenAPI reference docs, incident runbooks, and Slack support threads.
*Produce:* a chunking strategy per source with size, overlap, boundary rule, and what goes in the contextual header. Justify why they differ.

**D6. Choose an index.**
Corpus A is 400,000 chunks. Corpus B is 800 million chunks. Embeddings are 1,536 dimensions, 4-byte floats.
*Produce:* raw vector size for each, and a decision between exact search, in-memory HNSW, and a sharded ANN deployment. Name the parameter you would tune and what it trades.

**D7. Enforce access control.**
A document is visible only to the `finance` role.
*Produce:* the retrieval query design showing where the predicate is applied, plus an executable adversarial test asserting the document never reaches the results, the prompt, or the answer. State where the test runs in CI.

**D8. Implement and beat a single retriever.**
*Produce:* a working reciprocal rank fusion implementation, plus one query where the fused ranking beats both BM25 alone and dense retrieval alone. Explain why each single retriever failed.

**D9. Plan an embedding migration.**
A live system must move to a new embedding model with no quality regression.
*Produce:* the migration plan, the comparison metric, the labelled set you need, the cutover mechanism, and the rollback trigger. Explain what breaks if you migrate in place.

## Generation and grounding

**D10. Run the five-stage diagnosis.**
A user reports a wrong answer. Logs show the retrieved chunk ids and scores, and the assembled prompt.
*Produce:* the five stages in order, the evidence you would check at each, and the fix at each. State which stage is the rarest cause.

**D11. Build a citation validator.**
*Produce:* code that parses cited ids from a response, checks them against the supplied context, computes a validity ratio, and decides what to do on a mismatch. Decide whether a fabricated citation should fail the request or degrade it, and justify.

**D12. Decline instead of guessing.**
*Produce:* the rule that decides when retrieval scores are too low to call the model at all, how you would set the threshold empirically, and what the user sees instead.

## Agents

**D13. Do you need an agent?**
An order-management assistant must look up orders, check warranty status, issue replacements, and email confirmations.
*Produce:* a step-by-step decision for each capability - model, code, or model-inside-code - and an argument for the least agentic design that satisfies the requirement.

**D14. Design the tool surface.**
*Produce:* schemas for four tools including argument types, validation rules, the authorization check and whose identity it uses, the idempotency key derivation, and the return payload trimmed to what the model actually needs.

**D15. Bound the loop.**
*Produce:* the enforcement logic for all four bounds plus repetition detection, and the partial-result response returned when each one trips. Explain what each bound catches that the others do not.

**D16. Attack your own agent.**
An agent has `search_tickets` and `send_email`.
*Produce:* a working indirect prompt injection embedded in a ticket body, then three architectural mitigations, and for each an explanation of why it blocks the attack. Explain why a prompt instruction does not.

**D17. Order for compensation.**
A five-step workflow contains two irreversible actions.
*Produce:* the ordering that minimizes compensation, the compensating action for each remaining reversible step, and the one effect that cannot be compensated.

## Evaluation and operations

**D18. Build an eval set.**
*Produce:* 20 cases for a documentation assistant across happy path, edge, absent-answer, adversarial, and regression. At least four must have "I do not know" as the correct answer. State the scoring rule for each category.

**D19. Calibrate a judge.**
*Produce:* an LLM-as-judge rubric for faithfulness, a hand-grade of 10 outputs, the agreement rate between you and the judge, and what you would change if agreement is poor. Name the three biases and the control for each.

**D20. Set the gate empirically.**
*Produce:* the procedure for deriving a regression threshold from measured run-to-run variance rather than intuition, and the resulting gate specification separating absolute golden-set failures from tolerance-based aggregate movement.

**D21. Define SLIs and SLOs.**
*Produce:* six SLIs with objectives for a customer-facing RAG assistant. Justify refusal rate as a two-sided band and name the incident each direction represents.

**D22. Shadow a change.**
*Produce:* the shadow evaluation pipeline - traffic capture, candidate execution, comparison metric, promotion criteria, and how you avoid double-charging for inference.

## Challenge: full design rounds

**D23.** Design a RAG system over legal contracts where a wrong citation is a compliance breach. Run the full 45-minute method. Then state what you changed relative to a documentation assistant, and why.

**D24.** Design semantic search over 100 million product listings. Decide first whether generation is needed at all, and defend the decision.

**D25.** Design an on-call assistant that reads runbooks and proposes remediation. It may not execute anything without approval. Specify the approval boundary precisely, and say what happens during a provider outage - given that an incident is exactly when you need it.
