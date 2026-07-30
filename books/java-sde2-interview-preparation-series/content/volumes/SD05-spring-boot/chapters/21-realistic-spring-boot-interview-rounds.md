# Realistic Spring Boot Interview Rounds

Answer each prompt aloud before reading the model response. A strong answer identifies dependency, condition, property origin, application/context boundary, runtime failure semantics, and evidence.

## Answer control loop

```text
1. Clarify the user-visible invariant and environment.
2. Name the Boot capability and underlying Spring mechanism.
3. Trace dependency -> condition -> bean -> property origin.
4. Mark HTTP/thread/transaction/process/deployment boundaries.
5. Predict normal, failure, retry, and shutdown behavior.
6. Select the smallest safe configuration or design.
7. Prove it with focused tests and operational signals.
```

## 1. Framework versus Boot

**Interviewer:** What exactly does Boot add?

**Strong answer:** Framework supplies the container, AOP, transactions, MVC, validation, and testing foundations. Boot adds application bootstrap, conditional auto-configuration, curated dependencies/starters, external-configuration conventions, executable packaging, Actuator integration, and test support. Boot contributes ordinary Spring definitions based on evidence; it does not replace Framework behavior.

## 2. Starter versus auto-configuration

**Interviewer:** Is a starter an auto-configuration?

**Strong answer:** No. A starter is a dependency descriptor. Its libraries may place classes and auto-configuration modules on the classpath. Auto-configuration is runtime configuration selected by conditions. The BOM manages versions and the build plugin packages/runs; those are separate responsibilities.

## 3. `@SpringBootApplication`

**Interviewer:** Explain it and one placement failure.

**Strong answer:** It identifies Boot configuration, enables auto-configuration, and component-scans from its package. Putting it in a leaf package can exclude components; putting it too high can scan unintended code. I keep it at the application root and use explicit imports for module boundaries.

## 4. Startup sequence

**Interviewer:** What happens after `SpringApplication.run`?

**Strong answer:** Boot prepares listeners/initializers and the environment, determines application type, creates and loads the context, applies user and conditional auto-configuration, refreshes beans, starts the embedded server if needed, runs startup runners, then publishes readiness. I classify a failure by the last completed phase.

## 5. Auto-configuration back-off

**Interviewer:** Why did defining one bean change startup?

**Strong answer:** A missing-bean condition in an auto-configuration no longer matched, so Boot withheld its default. Back-off is local to the condition; related infrastructure may remain. I inspect the condition report and public bean types rather than excluding the whole subsystem.

## 6. Local DataSource only

**Interviewer:** Local creates a DataSource, CI does not.

**Strong answer:** An embedded driver may be present locally or test scope may differ. I compare resolved classpaths, profiles, external URL/driver configuration, user beans, and condition report. I make the target dependency explicit and test the intended database contract.

## 7. Property precedence

**Interviewer:** YAML says 8080, process listens on 9090.

**Strong answer:** Command-line, system, environment, external/profile-specific configuration, or test overrides may have higher precedence. I inspect the winning property source and origin, active profiles, imports, and relaxed binding. Packaged YAML is not the universal winner.

## 8. Typed configuration

**Interviewer:** Why not ten `@Value` fields?

**Strong answer:** A cohesive `@ConfigurationProperties` type gives one namespace, typed conversion, explicit units, metadata, validation, immutable construction, and focused binding tests. I keep secrets redacted and model refresh separately if values rotate.

## 9. Profiles

**Interviewer:** Would you create profiles for every customer?

**Strong answer:** No. Profiles select coherent configuration groups, not tenant data or unlimited feature combinations. Customer values belong in data or typed external configuration; feature delivery needs a governed feature-flag mechanism. Profile explosion makes effective behavior hard to predict.

## 10. Custom starter

**Interviewer:** When is a company starter justified?

**Strong answer:** When many applications need one stable integration contract and defaults. I separate core API from Boot assembly, register auto-configuration through imports metadata, back off for user beans, validate typed properties, test the condition matrix, and publish version compatibility/migration guidance.

## 11. Controller DTOs

**Interviewer:** Why not expose the entity?

**Strong answer:** It couples API and persistence, risks over-posting and lazy loads, and makes compatibility difficult. I use request/response DTOs, perform structural validation at binding, enforce state-dependent invariants in the application service, and preserve concurrent invariants in the database.

## 12. Error handling

**Interviewer:** How do you build consistent errors?

**Strong answer:** A central advice translates known application exceptions to stable problem details and codes. Unknown defects stay 500 and are logged once with correlation. I never expose stack traces, SQL, credentials, or provider bodies, and I contract-test both status and shape.

## 13. Duplicate POST

**Interviewer:** A client retries after timeout.

**Strong answer:** The client supplies an idempotency key. The service atomically claims it with a canonical request fingerprint and persists outcome under a uniqueness constraint. Same key and request returns the original result; same key and different request conflicts. In-progress/crash recovery and retention are explicit.

## 14. Outbound timeout

**Interviewer:** Is a timeout a failure?

**Strong answer:** It is a caller-side deadline; the remote outcome may be unknown. I query/reconcile using the same idempotency key before repeating a non-idempotent operation. Client pools, connect/read/overall timeouts, retry budget, and observability are explicit.

## 15. Retry policy

**Interviewer:** Retry every 5xx three times?

**Strong answer:** No. I retry only transient classifications, with an idempotent operation/key, remaining end-to-end deadline, bounded exponential backoff and jitter, and protection against layered amplification. I respect 429/`Retry-After` contracts and expose attempts separately from requests.

## 16. Actuator exposure

**Interviewer:** Why is adding Actuator not enough?

**Strong answer:** Endpoint existence, access policy, HTTP/JMX exposure, network reachability, and authorization are separate gates. I allowlist endpoints, restrict management traffic, sanitize values, and test access. Heap/env/loggers/shutdown are especially sensitive.

## 17. Liveness versus readiness

**Interviewer:** Database is down. Should liveness fail?

**Strong answer:** Normally no; restarting every process cannot repair a shared database and may amplify load. Readiness depends on whether the instance can serve useful traffic and the effect of removing all replicas. I keep dependency diagnostics separate and checks bounded.

## 18. Graceful shutdown

**Interviewer:** Why are requests still dropped?

**Strong answer:** I trace readiness refusal, load-balancer convergence, pre-stop delay, server drain, request deadlines, executor/message work, and platform grace. Application shutdown timeout must fit inside platform grace, and durable work needs lease/recovery rather than an in-memory queue.

## 19. Test choice

**Interviewer:** When use `@SpringBootTest`?

**Strong answer:** For cross-bean application assembly or a real server boundary, not every method. Pure logic stays unit-tested; conditions/binding use `ApplicationContextRunner`; controllers use a web slice; database-specific behavior uses the target container. The claim selects the boundary.

## 20. Context tests are slow

**Interviewer:** How do you reduce suite time?

**Strong answer:** Count distinct contexts and identify property/profile/mock variations. Move logic down to unit tests, use focused slices/runners, standardize shared context configuration, remove unnecessary `@DirtiesContext`, and fix leaked mutable state/threads. Parallelism comes after isolation.

## 21. Migration safety

**Interviewer:** Rename a heavily used column without downtime.

**Strong answer:** Expand with the new representation, deploy code compatible with both, dual-write/backfill with bounded load and evidence, switch reads, stop old writes after old instances are gone, then contract/drop later. I coordinate DDL/locks and do not run a destructive change in every pod startup.

## 22. Pool exhaustion

**Interviewer:** Increase Hikari pool size?

**Strong answer:** First separate acquisition wait, active/idle/pending counts, transaction duration, slow queries, lock waits, remote calls inside transactions, and leaks. A larger pool can overload the database. I shorten hold time and bound concurrency, then size from database capacity.

## 23. OOM-killed container

**Interviewer:** Heap is below the limit, so why killed?

**Strong answer:** Container memory includes heap, metaspace, code cache, thread stacks, direct buffers, native libraries, and overhead. I distinguish Java OOME from platform kill, inspect native/thread evidence, and leave measured headroom rather than setting heap to the limit.

## 24. Startup regression

**Interviewer:** Startup grew from 8 to 60 seconds.

**Strong answer:** I compare build/dependency/config changes and structured startup steps, isolate context work, migrations, constructor I/O, cache warmup, and server time, then remove or defer the dominant optional work. Lazy/AOT/native comes only after the phase is measured.

## 25. Boot upgrade

**Interviewer:** How do you upgrade a major Boot line?

**Strong answer:** Move to the latest maintenance release of the current line, remove deprecations/overrides, read migration and dependency/configuration changelogs, compare resolved graphs, run compilation, behavior, target-database, packaging, startup, security, and smoke tests, then canary with rollback. I do not pin an incompatible Framework version.

## 26. Production 404 after deployment

**Interviewer:** Code contains the mapping, but clients get 404.

**Strong answer:** I verify artifact version, `mappings`, application/context path, gateway rewrite, HTTP method, security behavior, and instance routing. Source code presence does not prove registration or that traffic reached the expected instance.

## 27. Readiness flapping

**Interviewer:** Pods repeatedly enter and leave service.

**Strong answer:** I identify the exact health contributor, timeout, status aggregation, dependency sharing, and platform thresholds; correlate dependency latency and probe traffic; then choose whether the dependency belongs in readiness. I add hysteresis/cache only with clear stale-state bounds.

## 28. Observability design

**Interviewer:** What do you instrument first?

**Strong answer:** User-facing request rate, latency distribution, errors, and saturation; then dependency attempts/timeouts and core business outcomes with bounded dimensions. Traces explain paths and logs explain events. I ban unbounded IDs and secrets from tags.

## Readiness signal

You are ready to use Spring Data and specialist Spring modules when answers begin with dependency, condition, origin, boundary, and evidence rather than a list of annotations.
