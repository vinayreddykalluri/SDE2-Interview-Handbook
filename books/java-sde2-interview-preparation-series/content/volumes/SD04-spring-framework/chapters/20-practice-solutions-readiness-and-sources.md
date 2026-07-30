# Practice, Solutions, Readiness, and Sources

Attempt each group before reading the solution notes. The goal is executable reasoning, not memorized definitions.

## Practice bank: 60 prompts

### Foundation: 1-20

1. Define bean, bean definition, context, IoC, and DI.
2. Draw a plain Java graph for controller, service, and repository.
3. Register that graph with explicit `@Bean` methods.
4. Explain `BeanFactory` versus `ApplicationContext`.
5. Trace context refresh from definitions to exposed objects.
6. Compare constructor, setter, and field injection.
7. Resolve two implementations with a semantic qualifier.
8. Inject all validation strategies and define their order.
9. Explain why a missing component annotation is not always the registration bug.
10. Compare component scanning and explicit imports.
11. Build and validate a typed settings record.
12. Read a classpath resource packaged inside a JAR.
13. Explain singleton scope precisely.
14. Demonstrate prototype lookup twice.
15. Explain prototype injection into a singleton.
16. List lifecycle stages through destruction.
17. Compare definition and instance post-processors.
18. Publish one immutable application event.
19. Convert a string into a domain identifier.
20. Separate structural, domain, authorization, and database validation.

### Interview Core: 21-45

21. Remove a constructor circular dependency without `@Lazy`.
22. Explain full and lite `@Configuration` behavior.
23. Prove one factory method parameter receives the managed singleton.
24. Decide whether a profile or typed property should select an adapter.
25. Diagnose a resource that works from the IDE but not a JAR.
26. Show why a mutable singleton counter races.
27. Inject a request-scoped collaborator into a singleton safely.
28. Explain why request context does not follow an async task.
29. Compare command, application event, and durable message.
30. Design an after-commit reaction and name its crash window.
31. Define join point, pointcut, advice, aspect, target, and proxy.
32. Test an aspect match, non-match, and exception path.
33. Order authorization, retry, transaction, and metrics advice.
34. Explain JDK versus subclass proxies.
35. Refactor an advised self-invocation.
36. Diagnose advice on a final or private method.
37. Place one transaction around order and inventory writes.
38. Predict rollback for checked and unchecked exceptions.
39. Explain rollback-only and unexpected rollback.
40. Compare `REQUIRED`, `REQUIRES_NEW`, and `NESTED`.
41. Explain why `readOnly` is not authorization.
42. Move an HTTP call out of a transaction.
43. Map a Spring MVC request from filter to response.
44. Design safe validation and problem responses.
45. Write a focused proxy-backed rollback integration test.

### SDE-2 Follow-up: 46-60

46. Classify deadlock, lock timeout, duplicate key, and uncertain commit for retry.
47. Order retry and transaction advice and prove the order.
48. Design request idempotency with a payload fingerprint.
49. Model unknown remote payment outcome and reconciliation.
50. Design an outbox relay with lease, retry, idempotency, and metrics.
51. Capacity-plan `REQUIRES_NEW` under concurrent outer transactions.
52. Design a bounded executor and overload policy from downstream capacity.
53. Coordinate one scheduled logical job across ten replicas.
54. Diagnose a growing async queue with idle application CPU.
55. Reduce a test suite with hundreds of context cache keys.
56. Prove after-commit listener behavior in a test.
57. Diagnose an unproxied transactional bean from runtime evidence.
58. Recover from a wrong-profile/wrong-adapter production deployment.
59. Design a privacy-safe cross-cutting telemetry aspect.
60. Review a custom container extension for lifecycle and AOT risks.

## Debugging exercises: 15 code reviews

1. A service field is annotated `@Autowired` and cannot be constructed in a unit test. Refactor it.
2. Two `Clock` beans exist and an unqualified constructor parameter fails startup. State two valid repairs.
3. A broad scan registers both fake and production payment gateways. Narrow the graph.
4. A singleton stores `currentCustomerId` in a field. Explain the race and redesign it.
5. A prototype connection wrapper expects `@PreDestroy`. Define ownership.
6. A lite configuration calls another `@Bean` method directly. Remove duplicate construction.
7. A listener sends email before the transaction later rolls back. Select an appropriate reaction design.
8. Around advice does not call `proceed()`. Predict the result.
9. `this.reserve()` has `@Transactional(REQUIRES_NEW)` but joins the caller. Explain why.
10. A checked import exception commits partial rows. Repair rollback semantics.
11. An inner `REQUIRED` failure is caught and outer commit throws unexpectedly. Trace it.
12. An async method accepts a managed entity and later throws lazy-loading errors. Redesign the command.
13. Six replicas run the same cleanup schedule. Add coordination.
14. A transactional test passes because lazy state remains open. Reproduce the production boundary.
15. An error aspect logs passwords and tokens. Replace its telemetry contract.

## Solution notes

### Foundation solutions

1-5 should distinguish metadata, instance, container, and proxy; use the context as the composition root; and describe refresh without claiming every lazy/prototype bean is created. 6-10 should make required dependencies explicit, qualify semantics rather than names, treat order as a contract, and constrain scanning. 11-15 should convert properties once, stream resources, define singleton per bean/container, and explain prototype resolution and cleanup. 16-20 should separate lifecycle phases, use events as in-process facts, convert before validation, and preserve database constraints for concurrent invariants.

### Interview Core solutions

21-25 should redesign cycles, use parameter injection across configuration modes, keep environment selection coarse, and avoid filesystem assumptions. 26-30 should identify shared mutable state, use scoped indirection, treat thread context explicitly, and use outbox for durable delivery. 31-36 should draw proxy dispatch, make pointcuts and order testable, and move policy boundaries to externally invoked visible methods. 37-42 should center the business invariant, state rollback rules, distinguish logical and physical transactions, and shorten resource hold time. 43-45 should keep DTO/status/error contracts explicit and verify durable rollback through the injected proxy.

### SDE-2 solutions

46-50 require categorized failures, fresh-transaction bounded retries, request/message identifiers, uncertain-outcome state, and a crash-recoverable outbox. 51-55 require measured resource demand, bounded queues, distributed job ownership, bottleneck evidence, and context-key consolidation. 56-60 should deliberately commit when testing after-commit behavior, inspect actual advisors/call paths, validate deployment configuration, redact high-risk data, and document extension lifecycle/order/compatibility.

### Debugging solutions

1-5 favor constructor injection, explicit selection, narrow scanning, immutable method arguments, and caller-owned prototype cleanup. 6-10 use factory parameters, transaction-aware event phases or outbox, exactly one `proceed`, external proxy crossings, and type-safe rollback rules. 11-15 preserve rollback-only truth, pass immutable IDs to async work, coordinate cluster jobs, test closed transaction boundaries, and allowlist telemetry fields.

## Five cumulative assessments

### Assessment 1: object graph

Build an order context with explicit modules, two gateway candidates, typed configuration, lifecycle, and a pure Java service test.

**Pass:** every bean has one registration path; no field injection, hidden service lookup, broad scan, ambiguous candidate, or secret default.

### Assessment 2: proxy reasoning

Add timing and audit advice, choose proxy mode, demonstrate one self-invocation failure, refactor it, and prove advisor order.

**Pass:** match/non-match, exception, method visibility, runtime type, and external call path are all explained.

### Assessment 3: transaction and delivery

Place an order and inventory reservation, add idempotency, publish an outbox record, and handle deadlock plus uncertain payment outcome.

**Pass:** local invariant is atomic, retries start fresh transactions, remote calls do not hold locks, and duplicates are safe.

### Assessment 4: HTTP and concurrency

Design create/find endpoints, validation, problem responses, async notification, and a clustered cleanup job.

**Pass:** DTOs are narrow, statuses precise, executor bounded, context propagation explicit, and scheduling coordinated.

### Assessment 5: test and incident round

Write pure unit, context, proxy, transaction-commit, MVC, and external-resource tests; diagnose a wrong adapter plus pool exhaustion.

**Pass:** each test proves a named risk, committed state is observed, evidence precedes capacity changes, and no secret is exposed.

## Final readiness assessment

You are ready for **SD 05 - Spring Boot** when you can, without notes:

- construct and explain a context from plain Java through managed beans;
- predict candidate resolution, lifecycle, scope, and configuration mode;
- draw JDK/subclass proxy and self-invocation paths;
- place transactions from invariants and trace rollback/propagation;
- distinguish in-process events, async tasks, and durable messaging;
- map MVC requests through validation, service, and safe response;
- choose a test level and prove committed/failed outcomes;
- diagnose startup, proxy, pool, and scheduled-work incidents from evidence.

## Official sources and version baseline

The prose and examples were audited against primary Spring documentation. These links are also the upgrade path when behavior changes:

- Spring Framework reference: <https://docs.spring.io/spring-framework/reference/>
- IoC container and beans: <https://docs.spring.io/spring-framework/reference/core/beans.html>
- Bean scopes: <https://docs.spring.io/spring-framework/reference/core/beans/factory-scopes.html>
- Container extension points: <https://docs.spring.io/spring-framework/reference/core/beans/factory-extension.html>
- Spring AOP proxy mechanics: <https://docs.spring.io/spring-framework/reference/core/aop/proxying.html>
- Transaction management: <https://docs.spring.io/spring-framework/reference/data-access/transaction.html>
- Transaction propagation: <https://docs.spring.io/spring-framework/reference/data-access/transaction/declarative/tx-propagation.html>
- Task execution and scheduling: <https://docs.spring.io/spring-framework/reference/integration/scheduling.html>
- Spring MVC reference: <https://docs.spring.io/spring-framework/reference/web/webmvc.html>
- TestContext framework: <https://docs.spring.io/spring-framework/reference/testing/testcontext-framework.html>
- Supported Spring Framework lines: <https://github.com/spring-projects/spring-framework/wiki/Spring-Framework-Versions>
- Spring Framework 7.0 release notes: <https://github.com/spring-projects/spring-framework/wiki/Spring-Framework-7.0-Release-Notes>

Version baseline for executable labs: Java 21, Spring Framework 7.0.8, JUnit Jupiter 5.11.4, and H2 2.3.232. H2 proves local Spring transaction mechanics only; it does not validate MySQL-specific locking or query behavior.

## Cross-book boundaries

- Spring Boot auto-configuration, Actuator, packaging, and Boot testing: **SD 05**.
- Repository derivation and Spring Data abstractions: **SD 06**.
- MySQL isolation, indexes, locks, and recovery: **SD 02**.
- Hibernate/JPA lifecycle and fetch behavior: **SD 03**.
- Spring Security, WebFlux, Batch, Integration, and specialized testing: **SD 10**.
- Kafka delivery and Spring Kafka: **SD 09**.

The core rule remains simple: first draw the Java objects and runtime boundary; then choose the Spring mechanism that makes that boundary explicit and testable.
