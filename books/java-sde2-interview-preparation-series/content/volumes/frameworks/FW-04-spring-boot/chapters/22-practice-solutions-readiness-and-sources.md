# Practice, Solutions, Readiness, and Sources

Complete each task before reading its solution sketch. The goal is to predict runtime behavior, produce evidence, and communicate trade-offs.

## Cumulative assessment 1 - Application assembly

1. **Foundation:** Build and package a one-endpoint MVC service.
2. **Foundation:** Draw the startup phases through readiness.
3. **Interview Core:** Explain starter, BOM, plugin, and auto-configuration separately.
4. **Interview Core:** Diagnose a component outside the scan root.
5. **SDE-2 Follow-up:** Review a pull request that adds six unrelated starters.

### Solution sketch

Prove the packaged jar starts and serves the endpoint. The application root defines discovery. A starter changes the classpath; the BOM aligns versions; the plugin packages; auto-configuration registers conditional definitions. Unused starters expand attack, startup, and behavior surface and need removal or explicit justification.

## Cumulative assessment 2 - Conditions and configuration

1. Predict a missing-bean back-off result.
2. Identify the winner among packaged YAML, external profile YAML, environment, and CLI.
3. Convert scattered timeout strings into typed properties.
4. Debug a critical optional config import.
5. Design a six-case custom auto-configuration test matrix.

### Solution sketch

User definitions register before auto-configuration, so a matching user bean makes the default back off. Determine values from actual ordered property sources and origins. Use typed `Duration`/`DataSize` with explicit units and validation. Required imports must fail fast. Test default, disabled, missing prerequisite, user override, invalid binding, and applicable/non-applicable application type.

## Cumulative assessment 3 - HTTP and dependencies

1. Define structural and business validation for order creation.
2. Design one stable problem response.
3. Create a `(createdAt,id)` cursor.
4. Specify idempotency-key states.
5. Allocate a 2-second request deadline across database and payment calls.

### Solution sketch

Bind to a DTO, structurally validate it, authorize, enforce state in the service, and use a database constraint for concurrency. Error shape includes stable code/status/title/correlation without internals. Cursor ordering includes a unique tie-breaker. Idempotency distinguishes new/in-progress/completed/conflicting fingerprints. Timeout allocation reserves response/error budget and never lets nested retries exceed the caller deadline.

## Cumulative assessment 4 - Operations and testing

1. Separate liveness, readiness, and diagnostics for three dependencies.
2. Allowlist Actuator endpoints and roles.
3. Define four low-cardinality metrics.
4. Select tests for domain, binding, controller, database locking, and packaged server.
5. Diagnose a readiness-flapping deployment.

### Solution sketch

Liveness describes process repairability; readiness controls traffic; detailed dependency health remains diagnostic. Expose only required operator endpoints. Metrics use controlled route/outcome/dependency dimensions. Use unit, context runner, web slice, target container, and random-port/smoke respectively. For flapping, identify the contributor and timeout, correlate dependency evidence, and decide whether it belongs in readiness before tuning thresholds.

## Cumulative assessment 5 - Delivery and incidents

1. Explain a layered executable jar.
2. Budget heap and non-heap memory for a 1 GiB container.
3. Design expand-contract migration for a column rename.
4. Investigate a 50-second startup regression.
5. Lead a pool-exhaustion incident.

### Solution sketch

Layers separate stable dependencies from changing application code for image reuse. Leave measured room for metaspace, stacks, direct/native memory, and spikes. Expand, dual-write/backfill, switch reads, stop old writes, then contract after compatibility evidence. Decompose startup steps. For pools, measure acquisition, active/pending, transaction/query/lock duration, remote calls, and leaks before resizing.

## Predict the outcome

1. A user bean matches `@ConditionalOnMissingBean`: default backs off.
2. `--server.port=9090` and packaged `server.port=8080`: CLI wins unless command-line properties were disabled.
3. Health endpoint exists but is not included in web exposure: no remote route through that technology.
4. Readiness depends on one shared database and it fails: all instances may leave traffic, depending on platform behavior.
5. `@SpringBootTest` contains a mock variant per class: context-cache fragmentation grows.
6. A payment POST times out: remote outcome is unknown.
7. Container limit is 512 MiB and heap max is 512 MiB: native/non-heap use can trigger kill.
8. Global lazy initialization is enabled: some bean errors move from startup to first use.
9. A property was removed in an upgrade: context may still start while old value has no effect.
10. A separate management port is healthy: main-port health is not proven.

## Debugging exercises

1. **Missing controller:** application root is below the API package.
2. **Two clients:** missing-bean condition checks an implementation while user exposes an interface.
3. **Wrong timeout:** bare number interpreted in an unexpected default unit.
4. **Secret leak:** properties record is logged in a startup failure.
5. **400 becomes 500:** no handler for binding/validation contract.
6. **Duplicate orders:** idempotency stored only in an instance map.
7. **Retry storm:** gateway, service, and client each retry three times.
8. **Probe storm:** liveness calls a shared database.
9. **Slow tests:** every class changes profiles and mocks.
10. **Dropped rollout traffic:** platform grace is shorter than application drain.
11. **Migration race:** every pod runs a long non-transactional backfill.
12. **Public Actuator:** custom filter chain omitted management rules.
13. **Cardinality explosion:** metric tag contains raw order ID.
14. **Local-only success:** H2 is on the local/test classpath.
15. **Native failure:** reflection/resource behavior has no AOT hint.

### Debugging corrections

Narrow and correct the scan/import boundary; align conditional types; add explicit units; redact secret-bearing types; centralize stable problem translation; persist idempotency durably with uniqueness; own one bounded retry layer; remove shared dependencies from liveness; reduce context variants; align readiness/drain/grace; coordinate migrations; explicitly secure management; replace IDs with bounded tags; test the target database; and add supported runtime hints or remove unnecessary dynamic behavior.

## Small coding and design tasks

1. Create an immutable validated properties record.
2. Write an `ApplicationContextRunner` back-off test.
3. Return a problem detail for missing order.
4. Validate nested request lines.
5. Implement a deterministic cursor codec with signature.
6. Model idempotency states as an enum and transition table.
7. Configure a `RestClient` from typed properties.
8. Classify provider errors without leaking bodies.
9. Create a bounded health indicator.
10. Record accepted/rejected order counters with safe tags.
11. Add trace correlation to structured logs.
12. Write a readiness policy for required/optional dependencies.
13. Test graceful shutdown with one slow request.
14. Build a random-port endpoint test.
15. Add a target-database migration smoke test.
16. Build and inspect an OCI image.
17. Generate build information and expose only safe fields.
18. Compare startup steps before/after removing a starter.
19. Create a major-upgrade checklist.
20. Write a production 404 diagnostic script/runbook.

## Interview follow-ups

1. Which Boot defaults would you never rely on without verification?
2. When should startup fail rather than mark readiness false?
3. How does back-off preserve application control?
4. Which configuration values may safely have defaults?
5. How would you rotate credentials at runtime?
6. When is a custom starter excessive abstraction?
7. How do you prevent JSON changes from breaking clients?
8. What does an idempotency key not solve?
9. Where should retry ownership live?
10. Which dependencies belong in readiness?
11. How do you secure heap-dump access?
12. What makes a metric tag unsafe?
13. How do you prove graceful shutdown?
14. When is a full-context test mandatory?
15. How do target containers still differ from production?
16. Why can a larger pool reduce throughput?
17. When should migrations run outside the app?
18. What justifies native image adoption?
19. How do you canary a framework upgrade?
20. Which evidence do you collect before restart?

## Final readiness assessment

You are ready when you can, without notes:

- build and explain a packaged Boot service;
- trace startup and one condition report;
- predict configuration precedence from origins;
- design typed validated properties without leaking secrets;
- define stable HTTP, error, pagination, and idempotency contracts;
- budget outbound timeouts and retries;
- separate liveness, readiness, diagnostics, and graceful shutdown;
- select the smallest useful test boundary;
- explain artifact/image/resource behavior;
- diagnose startup, latency, memory, and rollout incidents from evidence.

If two areas remain weak, repeat their chapter exercises and one cumulative assessment before moving to Spring Data.

## Cross-book boundaries

- Container, proxies, AOP, transactions, MVC internals -> **SD 04 - Spring Framework**.
- Maven/Gradle dependency and plugin depth -> **JAVA 03 - Maven and Gradle**.
- MySQL plans, locks, isolation, indexes -> **SD 02 - MySQL**.
- JPA mappings, persistence context, fetching -> **SD 03 - Hibernate and JPA**.
- Spring Security depth -> **SD 10 - Spring Ecosystem Extensions** or the security track.
- Spring Data repositories -> **SD 06 - Spring Data**.
- Kafka workflows -> **SD 09 - Apache Kafka and Spring Kafka**.
- JVM memory/GC/diagnostics -> **JAVA 06/JAVA 08**.
- Distributed consistency and system design -> **SD 14**.

## Primary sources

- Spring Boot Reference Documentation: `https://docs.spring.io/spring-boot/reference/`
- System Requirements: `https://docs.spring.io/spring-boot/system-requirements.html`
- Build Systems and Starters: `https://docs.spring.io/spring-boot/reference/using/build-systems.html`
- Auto-configuration: `https://docs.spring.io/spring-boot/reference/using/auto-configuration.html`
- Custom Auto-configuration: `https://docs.spring.io/spring-boot/reference/features/developing-auto-configuration.html`
- Externalized Configuration: `https://docs.spring.io/spring-boot/reference/features/external-config.html`
- Testing: `https://docs.spring.io/spring-boot/reference/testing/`
- Actuator Endpoints and Probes: `https://docs.spring.io/spring-boot/reference/actuator/endpoints.html`
- Container Images: `https://docs.spring.io/spring-boot/reference/packaging/container-images/`
- Spring Boot 4.1 Release Notes: `https://github.com/spring-projects/spring-boot/wiki/Spring-Boot-4.1-Release-Notes`

Validate release-specific behavior against the documentation for the Boot line deployed by your organization.
