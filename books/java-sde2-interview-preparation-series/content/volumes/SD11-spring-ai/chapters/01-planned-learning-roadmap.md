# Spring AI for Java Engineers - Planned Learning Roadmap

> **Publication status:** roadmap edition. Provider-backed examples, retrieval labs, evaluations, and safety exercises will be added after the core Spring service path is complete.

Spring AI provides abstractions and integrations for model clients, prompts, structured output, tools, embeddings, vector stores, retrieval, and observability. This book will focus on engineering contracts around AI features rather than presenting model calls as ordinary deterministic APIs.

## Planned sequence

1. Model capabilities, provider differences, tokens, context, latency, cost, and nondeterminism.
2. Spring AI configuration, model clients, prompts, advisors, and structured output.
3. Embeddings, chunking, vector stores, metadata, retrieval, and grounding.
4. Tool calling, permission boundaries, validation, timeouts, and side-effect control.
5. Conversation memory, state ownership, privacy, retention, and multi-tenant isolation.
6. Evaluation datasets, quality metrics, regression tests, and human review.
7. Resilience, rate limits, fallbacks, caching, observability, and cost controls.
8. Prompt injection, data leakage, unsafe output, supply-chain risk, and governance.

## Interview focus

The expanded edition will ask readers to design a retrieval-backed Java service, define measurable quality, protect tool execution, handle provider failure, and explain why deterministic unit tests alone cannot validate an AI feature.

## Completion gate

A reader is ready to add Spring AI to a system when they can state the data, quality, safety, latency, cost, and fallback contracts; evaluate the feature against representative evidence; and keep privileged actions behind explicit application-controlled authorization.
