# Spring Boot Content Audit

## Previous condition

SD 05 contained one roadmap chapter and a 10-page roadmap PDF. It named starters, configuration, web, Actuator, data, testing, packaging, and upgrades but did not teach the mechanics, provide runnable applications, trace failures, answer realistic interviews, or validate Boot behavior.

## Existing chapter inventory

| Chapter | Previous concepts | Depth | Examples | Exercises | Dependency |
|---|---|---|---|---|---|
| Planned learning roadmap | eight planned topic groups | Too shallow | none | none | SD 04 and JAVA 03 named only |

The canonical source was `content/volumes/SD05-spring-boot/chapters/01-planned-learning-roadmap.md`. The publisher registration in `publishing/series.json` marked BOOT as planned with a 7-30 page range.

## Content-quality matrix

| Topic | Previous quality | Beginner clarity | Interview relevance | Accuracy/example quality | Recommended action |
|---|---|---|---|---|---|
| Boot versus Framework | Too shallow | Low | Medium | no runtime model | Complete rewrite |
| first application/project anatomy | Missing | None | Core | no compiling example | Add first |
| startup lifecycle | Missing | None | High | no phase model | Add timeline and failures |
| package/scan boundaries | Missing | None | High | no example | Add before conditions |
| starters/BOM/plugins | Confusing boundary | Low | High | no dependency evidence | Separate responsibilities |
| auto-configuration/back-off | Missing mechanics | Low | Critical | no condition example | Deep rewrite |
| custom starter | Missing | None | SDE-2 | no test matrix | Add focused chapter |
| external configuration | Too shallow | Low | Critical | no precedence/origin | Deep rewrite |
| typed properties/secrets | Missing | None | High | no validation | Add compiling examples |
| REST API/error handling | Too shallow | Low | High | no DTO/problem example | Expand |
| HTTP compatibility/idempotency | Missing | None | Critical | no concurrency model | Add |
| outbound HTTP/resilience | Missing | None | Critical | no deadline model | Add |
| data/transactions/migrations | Too shallow | Low | High | misleading risk if omitted | Add boundaries |
| Actuator/security | Too shallow | Low | High | no exposure/access model | Deep rewrite |
| observability/cardinality | Missing | None | Critical | no metrics example | Add |
| liveness/readiness/shutdown | Missing mechanics | None | Critical | no lifecycle model | Add |
| testing ladder | Too shallow | Low | Critical | no real fixture | Deep rewrite |
| packaging/containers/resources | Missing mechanics | None | High | no production example | Add |
| AOT/native/upgrades | Roadmap name only | Low | High | no trade-offs | Add after fundamentals |
| security/production boundary | Missing | None | High | no threat model | Add scoped chapter |
| incident diagnosis | Missing | None | Critical | no playbooks | Add |
| realistic interviews | Missing | None | Critical | no answers | Add 28 rounds |
| practice/solutions | Missing | None | High | no reinforcement | Add distributed/final |

## Critical findings

- The roadmap could not take a beginner from Spring Framework into a runnable Boot service.
- Auto-configuration was named without explaining classpath, conditions, user configuration, or back-off.
- Configuration precedence and property origins were absent, creating a major production/interview gap.
- There was no distinction between liveness, readiness, diagnostic health, and graceful shutdown.
- No code proved Boot behavior; no test boundary guidance existed.
- REST, idempotency, outbound timeouts, migration safety, metrics cardinality, resource limits, and incident response were missing.

## High-value findings

- Build-system responsibilities needed separation into starter, BOM, and plugin.
- Advanced topics appeared in the roadmap before first-application and startup foundations.
- Exercises and model answers were absent.
- Framework and specialist-book boundaries needed explicit cross-references.

## Final learning sequence

The replacement edition now progresses through:

```text
Boot mental model -> first application -> startup -> structure/build
-> conditions/custom auto-configuration -> configuration/binding
-> HTTP/outbound/data -> Actuator/observability/availability
-> testing -> packaging/performance/security -> incidents/interviews
```

This sequence preserves Spring Framework SD 04 as the prerequisite and prevents annotations, AOT, native images, or deployment concerns from appearing before application assembly and configuration are understood.
