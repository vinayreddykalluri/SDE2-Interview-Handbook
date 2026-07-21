# 03. High-Level System Design

HLD evaluates structured decision-making under ambiguity. A strong answer is a traceable chain from requirements and scale to interfaces, data, topology, bottlenecks, failure behavior, and operations.

```mermaid
flowchart LR
    R["Functional and quality requirements"] --> N["Numbers and constraints"]
    N --> A["APIs and events"]
    A --> D["Data model"]
    D --> H["High-level architecture"]
    H --> F["Failure and scale deep dives"]
    F --> O["Observability and evolution"]
```

## Coverage

- [Design framework and capacity estimation](design-framework-and-estimation.md)
- [Architecture patterns and case studies](architecture-patterns.md)

## Required artifacts

- Requirements table with explicit out-of-scope decisions.
- Traffic, storage, bandwidth, and concurrency estimates.
- API/event contracts and logical data model.
- Architecture diagram with synchronous and asynchronous boundaries.
- Failure-mode table and at least two alternatives.

## Ready when

You can lead a 45-minute design, quantify important decisions, adapt to follow-ups, and discuss consistency, reliability, security, observability, cost, and migration.
