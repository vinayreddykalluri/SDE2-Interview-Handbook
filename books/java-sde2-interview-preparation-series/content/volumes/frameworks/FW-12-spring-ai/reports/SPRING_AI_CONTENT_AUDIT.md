# Spring AI Content Audit

## Scope and previous condition

SD11 previously contained one 224-word roadmap. It named clients, prompts, RAG, tools, memory, evaluation, operations, and safety but did not teach the model/runtime path, deterministic safety shell, tenant isolation, edge cases, measurable release gates, exercises, solutions, or realistic interviewer dialogue.

## Critical findings resolved

| Previous gap | Risk | Resolution |
|---|---|---|
| Model call resembled ordinary API integration | Nondeterminism and behavior drift hidden | Seven contracts and deterministic shell around probabilistic core |
| No version boundary | Pre-2.0 APIs could be copied as current | Java 21 / Boot 4.1 / Spring AI 2.0 GA target and upgrade warnings |
| Structured output not decomposed | Valid JSON could execute unsafe values | Schema, Java, business, authorization/effect layers |
| RAG only named | Tenant leakage and poor retrieval hidden by fluent answers | Separate ingestion/query flows, metadata scope, migration, retrieval evaluation |
| Tool calls lacked an authority model | Prompt injection could reach side effects | Proposal/validation/authz/approval/idempotency loop and MCP trust boundary |
| Memory lacked ownership/privacy lifecycle | Cross-user leaks and indefinite retention | Composite scoped key, token limits, poisoning, concurrency, deletion inventory |
| No quality gate | Demo quality could be called production-ready | Stage metrics, held-out cases, deterministic checks, safety and release bundle |
| No incident/operations path | Cost, 429, drift, and leakage response unclear | Total deadline, retry/fallback/cache rules, safe telemetry, incident matrix |
| No live interview answers | Reader could not rehearse SDE-2 follow-ups | 12 dialogue chains, 12 follow-ups, and scoring rubric |

## Final content inventory

- 9 sequential chapters and approximately 8,636 words.
- 12 realistic interview dialogue chains plus 12 interviewer follow-ups.
- 28 labeled chapter/practice tasks and 8 reasoned end-of-book solutions.
- Internal diagrams for ChatClient/advisors/model, RAG ingestion/query, tool loops, and memory.
- More than 45 concrete failure/edge cases plus an incident-response matrix.
- Dependency-free Java companion proving seven deterministic-boundary suites.
- Official Spring AI 2.0 reference/upgrade links and version-aware provider guidance.

## Remaining publication boundary

No live provider, vector database, or Spring AI dependency was added. Such a lab requires credentials/cost choices and must not become the only validation. The root pass must add sources to the manifest, build the PDF/web output, and inspect layout.

## Recommendation

Content is ready for enhanced candidate publication after root integration. Production adoption still requires an organization-specific dataset, privacy/security review, provider contract tests, and measured quality gates.
