# Spring Boot and REST Capstone Audit

## Audit decision

The original four chapters were already strong at individual Spring concepts: container behavior, proxies and transactions, MVC contracts, idempotency, operations, testing, and an order API. The highest-value remaining weakness was integration. Readers could answer isolated questions but did not yet have one explicit model connecting the network request, framework dispatch, transaction boundary, database commit, response loss, retry, resource budgets, and deployment shutdown.

## Changes made

- Added `05-request-runtime-failure-and-interview-capstone.md` as the final synthesis chapter.
- Traced one request from socket acceptance through filters, security, `DispatcherServlet`, conversion, validation, controller, service proxy, repository, database commit, and response serialization.
- Separated flush, commit, response delivery, and asynchronous propagation failure windows.
- Added deadline budgeting, retry admission, readiness, graceful shutdown, and concurrency edge cases.
- Added seven realistic interviewer chains with complete answer structures.
- Extended the Java companion with lost-response recovery and deadline-aware retry decisions.
- Preserved the dedicated-book boundaries for security, persistence internals, messaging, and distributed systems.

## Quality evidence

| Measure | Result |
|---|---:|
| Canonical chapters audited | 5 |
| Approximate chapter words | 14,039 |
| New capstone words | 2,035 |
| New worked live-interview chains | 7 |
| Dependency-free Java companions | 1 |
| Strict compilation target | Java 21 |

## Remaining boundary

This volume intentionally explains how Spring participates in the path without duplicating the deeper books on Spring Security, JPA/Hibernate, Kafka, database storage, or distributed system design. Production integration tests still require a real Spring application and infrastructure; the local companion validates the decision invariants that can be tested deterministically without those dependencies.
