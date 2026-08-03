# Spring AI for Java Engineers: Begin with an Uncertain Dependency

A language model call looks like an HTTP client call, but the engineering contract is different. The same input can produce different wording, incorrect facts, malformed structure, an unsafe tool request, or a refusal. The provider can change model behavior without a Java deployment. Production design begins by admitting that uncertainty.

```text
ordinary API: request -> deterministic program contract -> response/error

AI feature: request + instructions + context + model/version
                 |
                 v
        probabilistic generation
                 |
        validate / ground / evaluate / authorize
                 |
                 v
           useful response or controlled failure
```

Spring AI provides Spring-style abstractions for models, `ChatClient`, prompts, advisors, structured output, tools, embeddings, vector stores, retrieval, memory, MCP integration, and observability. It reduces provider-specific plumbing. It does not make model output true, safe, deterministic, authorized, or cheap.

## Version boundary

This book targets **Java 21, Spring Boot 4.1, and Spring AI 2.0 GA**. Spring AI moved quickly before 2.0; examples written for 1.0/1.1 can have different artifact names, advisor contracts, structured-output behavior, and tool execution paths.

Version-sensitive examples are labeled. The durable engineering concepts are:

- models have bounded context and probabilistic output;
- a prompt is untrusted data plus application instructions, not a security boundary;
- structured output still requires schema and business validation;
- retrieval quality depends on ingestion, filtering, ranking, and evidence—not only embeddings;
- a model may propose a tool call; the application authorizes and executes it;
- memory is application-owned state with tenancy, retention, and privacy rules;
- evaluation needs a representative dataset and thresholds;
- deadlines, rate limits, cost, and fallbacks are explicit operational contracts.

## Prerequisites and non-prerequisites

You should understand:

- Spring Boot configuration and dependency injection;
- HTTP client timeouts, retries, and observability;
- JSON serialization and validation;
- database and tenant boundaries;
- basic vector intuition from dot product/cosine similarity (the formula is introduced when needed).

You do **not** need to train a neural network or derive transformer mathematics. This is a Java application-engineering book, not an ML research text.

## The running feature

We will design an internal support assistant:

```text
employee question
      |
      v
Spring MVC/WebFlux endpoint
      |
policy + tenant/user context
      |
retrieval over approved support documents
      |
ChatClient/advisor pipeline
      |
model may answer OR request a read-only support tool
      |
schema + citation + policy validation
      |
answer, abstention, or human escalation
```

It must never reveal another tenant’s documents, invent a refund, execute a privileged action solely because the model requested it, or claim confidence without evidence.

## Seven contracts before code

| Contract | Question |
|---|---|
| Task | What exact user outcome is allowed? |
| Data | Which sources may enter prompts, retrieval, logs, and training? |
| Quality | What does a correct, grounded, useful answer mean? |
| Safety | Which content/action must be blocked or escalated? |
| Latency | What is the end-to-end deadline and streaming expectation? |
| Cost | What token/request budget and model-routing policy apply? |
| Fallback | What does the user receive when quality or provider availability is insufficient? |

“Helpful chatbot” is not a testable contract. “Answer support-policy questions from documents visible to the authenticated tenant, cite supporting chunks, abstain below evidence threshold, p95 under four seconds” is closer.

## Learning sequence

```text
model basics and uncertainty
       |
ChatModel -> ChatClient -> prompt -> advisor chain -> structured output
       |
ingestion -> embedding -> vector store -> filtered retrieval -> grounded prompt
       |
tool proposal -> validation -> authorization -> idempotent execution -> result loop
       |
memory ownership and tenant/privacy boundaries
       |
evaluation datasets, quality gates, safety, operations, and incidents
       |
live interview rounds and reasoned exercises
```

Do not jump to agents and tools before you can validate one model response. Do not add RAG before you can measure whether retrieval finds the right evidence. Do not store conversation memory before defining ownership and deletion.

## Deterministic shell around a probabilistic core

```text
deterministic code:
authenticate -> authorize -> sanitize/limit -> retrieve/filter -> build request
                                                           |
                                                  probabilistic model
                                                           |
deterministic code:
validate schema -> validate business rules -> cite/abstain -> record safe evidence
```

The safest architecture keeps policy, permissions, money, irreversible actions, and source-of-truth state in deterministic application code.

## Quick check

1. What does Spring AI abstract, and what does it not guarantee?
2. Why is valid JSON not necessarily a valid business result?
3. Who authorizes a tool call?
4. Why must retrieval be tenant-filtered before context reaches the model?
5. What makes an AI quality requirement testable?

## Practice

- **Foundation:** Rewrite “build a smart support bot” as seven contracts.
- **Foundation:** Mark deterministic and probabilistic stages in the running feature.
- **Interview Core:** Define a safe abstention response for missing evidence.
- **SDE-2 Follow-up:** Identify three behavior changes that can happen without a Java code deployment and the monitoring needed for each.

## Readiness checkpoint

Continue when you can explain why a model is an uncertain external dependency and can state the allowed task, evidence, action, and fallback boundaries before naming a provider.
