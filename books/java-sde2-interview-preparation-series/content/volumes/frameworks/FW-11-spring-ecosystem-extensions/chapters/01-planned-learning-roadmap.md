# Spring Ecosystem Extensions: Start with the Problem, Not the Starter

Spring has a module for almost every backend concern. That is useful, but it creates a common interview mistake: naming a starter before explaining the problem. An SDE-2 answer should travel in the opposite direction:

```text
business invariant
      |
      v
runtime and failure boundary
      |
      v
smallest useful abstraction
      |
      v
Spring module + configuration + proof
```

This book follows that order. It assumes that you already understand plain Java, dependency injection, Spring proxies, transactions, Spring Boot configuration, HTTP, and basic persistence. It does **not** assume that you know Spring Security, Cloud, WebFlux, Batch, or Integration.

## Version boundary

The examples target the Spring Boot 4.1 generation and Java 21. At publication time, that line aligns with Spring Framework 7, Spring Security 7, Spring Batch 6, and the Spring Cloud 2025.1 release train. Spring projects ship independently, so treat exact annotations, defaults, and artifact names as version-sensitive.

The durable ideas are older than any release:

- authentication establishes an identity; authorization decides what that identity may do;
- a request is transformed by an ordered chain before application code runs;
- reactive code needs an end-to-end non-blocking path to gain its scalability model;
- a restartable batch job must record progress at a safe boundary;
- integration flows move messages through explicit channels and endpoints;
- retries are safe only when the operation and the failure are classified;
- health, logs, metrics, and traces answer different questions.

Use a Bill of Materials rather than choosing Spring Cloud component versions independently. For Boot 4.0/4.1, the Spring project compatibility table places applications on the 2025.1 release train; re-check the official table before an upgrade.

## What belongs in this volume

| Requirement | First module to investigate | What it does not solve by itself |
|---|---|---|
| Identity, request protection, permissions | Spring Security | User lifecycle, business authorization policy, secret storage |
| Health, metrics, traces, operational controls | Boot Actuator + Micrometer | A useful SLO, incident response, safe endpoint exposure |
| External configuration, gateway, service clients | Spring Cloud projects | Distributed consistency, correct timeout budgets, good service boundaries |
| High-concurrency non-blocking I/O | Spring WebFlux + Reactor | Slow blocking dependencies, CPU capacity, simpler MVC workloads |
| Restartable finite data processing | Spring Batch | Continuous event processing, idempotent business semantics |
| Enterprise application integration flows | Spring Integration | Broker durability unless the selected channel/adapter provides it |
| Focused framework proof | Spring Test + provider fixtures | Production-equivalent behavior without realistic dependencies |

Do not add all of these to a service “for future use.” Every module increases configuration surface, upgrade work, test combinations, and operational knowledge.

## One running system

The chapters use an order platform:

```text
browser / API client
        |
        v
gateway -> order API -> database
                    |-> payment API
                    |-> audit event

nightly settlement file -> batch job -> reconciliation table
partner SFTP -> integration flow -> validated message -> order API
```

The important questions are concrete:

1. Who authenticated the caller, and where is that identity stored?
2. Which component authorized `order:refund` for this order and tenant?
3. Which timeout expires first when payment is slow?
4. Does a retry repeat a side effect?
5. Which thread executes each stage, and can it block?
6. What state lets a failed import resume without duplicating rows?
7. Which signal tells an operator that the system is wrong rather than merely busy?

## Learning sequence

```text
module selection and prerequisites
             |
             v
Security request path -> authentication -> authorization -> exploit defenses
             |
             v
Actuator, metrics, traces, and focused tests
             |
             v
Cloud configuration, clients, gateway, and resilience
             |
             v
WebFlux demand, event loops, and blocking boundaries
             |
             v
Batch checkpoints + Integration message flows
             |
             v
failure matrix -> live interview rounds -> exercises and solutions
```

Security comes first because every later module must preserve identity and tenant context. Observability comes before distributed resilience because you cannot tune what you cannot see. WebFlux follows ordinary request flow so “reactive” has something familiar to compare against.

## The boundary tracing habit

For any extension, draw five labels:

```text
caller -> framework boundary -> application boundary -> external resource
             |                      |
          context                invariant
             |
          thread / transaction / retry attempt
```

Then ask:

- What enters and leaves the boundary?
- Which state is thread-local, subscriber-context, persisted, or reconstructed?
- Which order is guaranteed and which order is incidental?
- What happens on timeout, cancellation, duplicate delivery, restart, and shutdown?
- Which test or telemetry proves the behavior?

## A small decision example

**Requirement:** Import a 20 GB settlement file once per night, resume after a crash, and skip already committed records.

Weak answer: “Use WebFlux because it handles a lot of data.”

Stronger reasoning:

1. The workload is finite and restartability matters more than keeping many sockets open.
2. Records can be read in bounded chunks.
3. A durable job repository can record which step and checkpoint committed.
4. The writer needs an idempotency key because a crash can occur after an external effect but before metadata advances.
5. Spring Batch is the first module to evaluate. WebFlux is not the natural abstraction for job identity or restart state.

## Quick check

1. Why is a Spring starter not a design justification?
2. When is WebFlux a poor choice even if throughput is important?
3. What is the difference between batch restartability and business idempotency?
4. Why must a Spring Cloud release train be aligned with Boot?
5. What context must survive an asynchronous boundary?

## Practice

- **Foundation:** Match each running-system requirement to Security, Actuator, Cloud, WebFlux, Batch, Integration, or no extra module.
- **Interview Core:** Draw the five labels for an authenticated call from gateway to order service to payment service.
- **Interview Core:** Explain why adding retries without an idempotency design can make availability worse.
- **SDE-2 Follow-up:** Choose between MVC, WebFlux, Batch, and messaging for three workloads and state the evidence that could change your choice.

## Readiness checkpoint

Continue when you can describe each extension by the runtime problem it solves, one cost it introduces, and one failure it cannot solve on its own.
