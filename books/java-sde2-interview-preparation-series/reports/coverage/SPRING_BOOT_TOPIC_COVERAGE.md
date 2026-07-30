# Spring Boot Topic Coverage

| Topic | Required depth | Previous state | Final state | Chapter | Examples/exercises | Cross-reference | Validation |
|---|---|---|---|---|---|---|---|
| Boot/Framework boundary | Core | roadmap sentence | layered runtime model | 00 | diagram + 10 checks/tasks | SD 04 | source QA |
| first application | Foundation | missing | Maven app, endpoint, jar commands | 01 | code + 5 tasks | JAVA 03 | Boot fixture |
| SpringApplication lifecycle | Core | missing | phases, events, runners, exits | 02 | timeline + incident task | JVM book | source QA |
| package/scan structure | Core | missing | feature layout and module boundaries | 03 | package diagrams + diagnosis | LLD | source QA |
| starters/BOM/plugins | Core | name only | responsibility and risk model | 04 | Maven/Gradle/CLI + tasks | JAVA 03 | repo build |
| auto-configuration | Critical | name only | conditions, back-off, report, exclusion | 05 | decision tree + debug | SD 04 | six Boot tests |
| custom starter | SDE-2 | missing | imports, metadata, matrix, compatibility | 06 | config and runner test | advanced Boot | Boot fixture |
| external config | Critical | name only | precedence, origin, profiles, imports | 07 | config examples + runbook | platform docs | companion + Boot test |
| configuration properties | Critical | missing | typed, validated, immutable, secrets | 08 | record + invalid cases | security | Boot fixture |
| REST boundary | Core | name only | DTOs, validation, errors/status | 09 | controller/advice + tasks | SD 04 MVC | source QA |
| HTTP/API design | SDE-2 | missing | methods, JSON, pagination, versioning, idempotency | 10 | state machine + design tasks | system design | companion |
| outbound HTTP | SDE-2 | missing | clients, four timeouts, retry/bulkhead | 11 | RestClient + recovery tasks | distributed systems | companion deadline model |
| data/migrations | Core | name only | DataSource, transactions, expand-contract | 12 | config/timeline/tasks | SD 02/03/04 | source QA |
| Actuator | Critical | name only | four gates, groups, security, custom endpoint | 13 | config + health example | security | Boot dependency |
| observability | Critical | missing | metrics, observations, traces, logging | 14 | code + incident tasks | JVM diagnostics | source QA |
| probes/shutdown | Critical | missing | availability lifecycle and drain | 15 | state diagram + budget | platform | companion + Boot test |
| testing ladder | Critical | name only | unit/runner/slice/full/container | 16 | code + portfolio task | MySQL/JPA | six Boot tests |
| packaging/containers | Core | name only | jar, layers, buildpacks, resources, SBOM | 17 | commands/Dockerfile/tasks | JAVA 03/08 | publisher build |
| startup/AOT/native/upgrades | SDE-2 | name only | measurement-first trade-offs | 18 | upgrade matrix | JVM | source QA |
| security/production | Core | missing | filter back-off, CORS/CSRF, proxy trust | 19 | config/threat-model | Security book | source QA |
| incident diagnosis | SDE-2 | missing | seven playbooks and safe evidence | 20 | control loop/tasks | JVM/DB | source QA |
| interview rounds | Critical | missing | 28 realistic Q&A | 21 | model answers | all boundaries | editorial audit |
| practice/solutions | Critical | missing | five assessments, output/debug/design/follow-up | 22 | 85+ final tasks | all books | editorial audit |

## Scope boundary

The edition is complete for Spring Boot foundations and SDE-2 application/operations interviews. It intentionally cross-references deep Spring Security, WebFlux/Reactor, Spring Data internals, Kafka, MySQL, Hibernate/JPA, JVM diagnostics, and distributed-system design.
