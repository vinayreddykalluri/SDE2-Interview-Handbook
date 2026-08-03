# Realistic Spring Framework Interview Rounds

Answer each prompt aloud before reading the model response. A strong answer identifies object ownership, registration, resolution, proxy crossing, thread/transaction boundary, failure semantics, and proof.

## Answer control loop

```text
1. Clarify the business invariant and caller.
2. Draw target, bean definition, context, and proxy.
3. Trace candidate resolution and lifecycle.
4. Mark thread, transaction manager, resource, and external calls.
5. Predict normal, failure, retry, and shutdown outcomes.
6. Choose the simplest explicit boundary.
7. Prove with focused tests and operational evidence.
```

## 1. Spring versus Spring Boot

**Interviewer:** What is the difference?

**Strong answer:** Spring Framework provides the IoC container, DI, AOP, transactions, events, validation, MVC, and testing foundations. Spring Boot builds on it with opinionated auto-configuration, starters, application bootstrap, external-configuration conventions, packaging, and operational integration. Boot does not replace Framework mechanics; it contributes bean definitions and configuration according to conditions.

## 2. IoC and DI

**Interviewer:** Explain IoC without saying "the framework controls everything."

**Strong answer:** A class declares collaborators through constructors or factory parameters, while a composition mechanism constructs and connects the object graph. Control of construction moves out of the business object. The class still owns its behavior and invariants. DI is the mechanism, and the Spring container is one implementation.

## 3. Bean creation trace

**Interviewer:** What happens when the context starts?

**Strong answer:** Configuration is parsed into bean definitions, definition processors run, eligible singletons are instantiated, dependencies are resolved and populated, lifecycle callbacks run through post-processors, post-processors may return proxies, and the context publishes refresh events. Lazy and non-singleton beans may be created later. Shutdown invokes eligible destruction callbacks.

## 4. Constructor or field injection

**Interviewer:** Why constructor injection?

**Strong answer:** It exposes required dependencies in the type contract, permits final references, fails incomplete graphs during startup, and enables pure Java tests. Field injection hides requirements and depends on reflective container mutation. Constructor injection does not make a class immutable or justify a constructor with fifteen dependencies.

## 5. Multiple implementations

**Interviewer:** Two gateways implement one interface. What happens?

**Strong answer:** An unqualified single injection point is ambiguous. If one is a real default I mark it primary; if selection is semantic I use a custom qualifier; if the use case needs all strategies I inject a collection and define ordering/routing explicitly. I do not rely on parameter names or add primary solely to silence startup.

## 6. Circular dependency

**Interviewer:** How do you fix `A -> B -> A`?

**Strong answer:** Constructor injection exposes the cycle. I look for shared logic to extract, orchestration to move above both services, a temporal event boundary, or responsibilities that should merge. `@Lazy` or self-injection can defer construction but usually hides the design issue, so I use them only with a justified lifecycle contract.

## 7. Singleton safety

**Interviewer:** Are Spring singleton beans thread-safe?

**Strong answer:** Not automatically. Singleton means one instance per bean definition per container, so request threads share it. Stateless services are typically safe; mutable fields require a concurrency design. I inspect collaborators and scoped proxies too. Request/session scope does not serialize access either.

## 8. Prototype in singleton

**Interviewer:** Why is the prototype reused?

**Strong answer:** The singleton's dependencies were resolved once during its construction, so it received one prototype instance. To request a new instance per operation I can inject `ObjectProvider`, use lookup/scoped proxy mechanisms, or simply construct a plain operation object if it has no container dependencies. I also own prototype cleanup.

## 9. `@Configuration` interception

**Interviewer:** Why does one `@Bean` method calling another return the singleton?

**Strong answer:** In full configuration mode, Spring subclass-enhances the configuration and intercepts factory calls through the container. Lite mode, including `proxyBeanMethods=false`, uses normal Java calls. I prefer factory method parameters, which make the dependency explicit and work in both modes.

## 10. Post-processor difference

**Interviewer:** `BeanFactoryPostProcessor` versus `BeanPostProcessor`?

**Strong answer:** The former changes or inspects bean-definition metadata before ordinary instances. The latter sees instances around initialization and can return a wrapper/proxy. Both are infrastructure; resolving application beans too early can make them miss later post-processing.

## 11. Event delivery

**Interviewer:** Is `publishEvent` asynchronous and durable?

**Strong answer:** Not by default. The default multicaster normally invokes listeners synchronously in the publishing thread, although it is configurable. Events are in-process and not durable. A transactional after-commit listener avoids pre-commit reaction but retains a crash window. Reliable cross-system delivery needs outbox/idempotency or a broker workflow.

## 12. Aspect does not run

**Interviewer:** An annotation is present, but advice is absent. Diagnose it.

**Strong answer:** I confirm the object came from the context, feature infrastructure is enabled, the caller has the proxy, the call is external rather than self-invocation, the method is visible/advisable for the proxy type, and the pointcut resolves against the actual method. Then I add a focused match/non-match test instead of changing annotations randomly.

## 13. JDK versus class proxy

**Interviewer:** Compare them.

**Strong answer:** A JDK proxy implements target interfaces and exposes those contracts. A class proxy subclasses the target and can expose class methods, but final classes/methods and private methods cannot be advised. Both are proxy boundaries; self-invocation remains a concern. I choose interfaces from design value, not solely for proxy mechanics.

## 14. Transaction placement

**Interviewer:** Controller, service, or repository?

**Strong answer:** Usually the externally invoked application-service method that owns one business invariant. Repository calls participate. The controller owns HTTP translation; repositories own persistence mechanics. I keep slow remote calls outside the database transaction and integration-test durable commit and rollback.

## 15. Checked exception

**Interviewer:** A checked exception leaves a transactional method. Rollback?

**Strong answer:** Under Spring's default declarative rules, ordinary checked exceptions do not trigger rollback, while unchecked exceptions and errors do. I configure `rollbackFor` when the checked exception means the unit failed, or translate the exception according to the boundary. I prove the result with committed-state tests.

## 16. Unexpected rollback

**Interviewer:** Why does the outer method get `UnexpectedRollbackException` after catching an inner error?

**Strong answer:** Both `REQUIRED` scopes joined one physical transaction. The inner failure marked it rollback-only. Catching the exception did not clear that status, so the outer commit could not succeed and Spring reported the unexpected rollback rather than returning false success.

## 17. `REQUIRES_NEW`

**Interviewer:** Use it for audit writes?

**Strong answer:** Only if audit durability must be independent of business rollback. It suspends the outer transaction, needs a separate resource, can exhaust the pool, and still requires a proxy crossing. Often an outbox or dedicated append-only audit path gives clearer durability and load behavior.

## 18. Async transaction

**Interviewer:** Does the caller's transaction reach `@Async` work?

**Strong answer:** Imperative transaction state is normally thread-bound, and async work executes on another executor thread. It does not inherit the caller transaction. The task can start a separate transaction. I pass immutable IDs/commands, observe exceptional completion, bound the executor, and use durable messaging if queued work cannot be lost.

## 19. Transactional test passed, production failed

**Interviewer:** Why?

**Strong answer:** A rollback-by-default test may keep the persistence context open, never prove commit-time constraints or after-commit listeners, and cause production code to join the test transaction. I add tests that invoke the proxy, commit where needed, and verify from a new transaction using the target database behavior.

## 20. Production pool exhaustion

**Interviewer:** Would you increase the pool?

**Strong answer:** First separate acquisition wait, transaction duration, database execution, lock wait, remote calls inside transactions, nested/new transactions, and leaks. A larger pool can overload the database. I shorten hold time and control concurrency, then size the pool from measured database capacity and service-level goals.

## 21. Validation and binding

**Interviewer:** Why not bind JSON directly to an entity?

**Strong answer:** It exposes writable fields, couples API to persistence, can trigger lazy behavior, and enables over-posting. I bind to a narrow request DTO, convert and structurally validate it, authorize, then load and mutate domain state explicitly. Database constraints still protect concurrent invariants.

## 22. Scheduled job in a cluster

**Interviewer:** How do you prevent duplicate work?

**Strong answer:** I assume every replica can trigger. I use a database lease/claim, partition ownership, leader scheduler, or external job system; process bounded idempotent units; make leases expire for recovery; and monitor lag, duplicates, and failed items. A local annotation is only a trigger.

## 23. Component scanning strategy

**Interviewer:** Why not scan the company root package?

**Strong answer:** It hides the registration boundary and can pull in unintended adapters, tests, or duplicate configuration. I use marker-type scans for application-owned components and explicit imports/bean methods for modules and infrastructure. The goal is a deterministic, reviewable graph.

## 24. Framework extension design

**Interviewer:** Your team wants a custom annotation that wraps every method. What do you ask?

**Strong answer:** I ask which policy and boundary it represents, exact pointcut, order relative to transactions/retries/security, self-invocation and proxy limits, allowed payload metadata, failure behavior, and how match/non-match paths are tested. If it hides domain workflow, I keep the behavior explicit instead.

## Readiness signal

You are ready for Spring Boot when these answers start from the object graph and runtime boundary, not from a list of annotations.
