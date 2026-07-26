# Spring Container, Configuration, Proxies, and Transaction Boundaries

## Learning objectives

After this chapter, you should be able to:

- explain inversion of control as a graph-construction and lifecycle problem rather than as annotation magic;
- choose constructor injection, qualifiers, scopes, and lifecycle hooks from explicit ownership rules;
- read Spring Boot auto-configuration as conditional configuration that backs away when user configuration is present;
- predict when a Spring AOP proxy can and cannot intercept a call;
- place a transaction around a business invariant and recognize proxy, rollback, and propagation traps; and
- test container wiring without turning every unit test into a full application startup.

## 1. The container is a managed object graph

### Intuition and formal model

In ordinary Java, one object constructs another:

```java
var repository = new JdbcOrderRepository(dataSource);
var service = new OrderService(repository, clock);
```

Dependency injection changes *who assembles the graph*, not the semantics of the objects. A Spring `ApplicationContext` reads bean definitions, resolves dependencies, creates instances, applies container extension points, and owns destruction callbacks. Let the bean-definition graph be `G = (V, E)`, where each vertex is a bean definition and an edge `A -> B` means A requires B during construction. Eager singleton creation needs an acyclic resolvable subgraph. A constructor cycle is a design signal because neither instance can be completed first.

Inversion of control is therefore an ownership contract:

1. application code declares required capabilities;
2. configuration maps each capability to a concrete bean;
3. the container constructs and decorates the graph;
4. callers use the resulting objects without locating dependencies globally.

This is different from the service-locator pattern. Calling `applicationContext.getBean(...)` throughout business code hides required dependencies and couples the domain to a container. Injection keeps requirements visible in constructors and makes plain-Java testing possible.

### Recognition and decision rules

| Situation | Preferred decision | Why |
|---|---|---|
| dependency is required for every valid instance | constructor parameter | construction proves completeness and supports `final` fields |
| dependency is one of several implementations | qualifier or explicit `@Bean` wiring | selection becomes configuration, not ordering accident |
| dependency is optional policy | inject an explicit no-op/default implementation | avoids nullable or container-aware domain logic |
| two services depend on each other | redesign the responsibility boundary | lazy injection hides the cycle but rarely fixes cohesion |
| third-party class must be managed | `@Bean` factory method | classpath scanning cannot annotate code you do not own |
| many fields are requested by one service | examine cohesion before adding injection tricks | a large constructor often reveals multiple responsibilities |

### Concrete Spring example — dependency-requiring

The following snippet requires Spring Framework. It deliberately injects a small interface rather than a concrete transport:

```java
public interface PaymentGateway {
    Authorization authorize(String accountId, long amountCents);
}

@Service
final class CheckoutService {
    private final PaymentGateway gateway;
    private final OrderRepository orders;
    private final Clock clock;

    public CheckoutService(
            @Qualifier("primaryGateway") PaymentGateway gateway,
            OrderRepository orders,
            Clock clock) {
        this.gateway = Objects.requireNonNull(gateway);
        this.orders = Objects.requireNonNull(orders);
        this.clock = Objects.requireNonNull(clock);
    }
}

@Configuration(proxyBeanMethods = false)
class CheckoutConfiguration {
    @Bean
    Clock applicationClock() {
        return Clock.systemUTC();
    }
}
```

The important guarantee is visible without Spring: a `CheckoutService` cannot exist without its collaborators. `@Qualifier` makes selection explicit. The meaning of `proxyBeanMethods` is framework-specific; do not infer container behavior from the Java syntax alone.

### Execution walkthrough

For an eager singleton service, a simplified startup sequence is:

1. configuration and component scanning contribute bean definitions;
2. post-processors may transform those definitions;
3. the container resolves the `CheckoutService` constructor;
4. it creates or obtains `PaymentGateway`, `OrderRepository`, and `Clock` beans;
5. it invokes the constructor;
6. it performs documented initialization callbacks and bean post-processing;
7. a post-processor may return a proxy that wraps the target;
8. the context publishes the fully initialized bean reference;
9. at orderly shutdown, destruction callbacks run for beans whose lifecycle the context owns.

The exact internal call sequence is not an application guarantee. The engineering contract is that extension points and lifecycle callbacks follow Spring's documented ordering rules. If ordering matters, test it against the supported framework version.

## 2. Lifecycle, scopes, and safe state

### Bean lifecycle without folklore

A useful lifecycle model is:

```text
definition -> dependency resolution -> construction -> population
           -> aware callbacks / post-processors -> initialization
           -> ready reference (possibly proxy) -> destruction
```

Prefer constructors for establishing invariants. Use `@PostConstruct` only for initialization that needs injected collaborators and cannot be done in a constructor. It must not start unbounded background work that the application cannot stop. Pair acquired resources with `@PreDestroy` or another explicit lifecycle mechanism. A factory method that returns an object does not automatically give the container knowledge of every resource the object creates later.

Spring's default application scope is singleton *per container and bean definition*, not one instance per JVM and not automatically thread-safe. A singleton controller can serve concurrent requests. Keep it immutable or protect mutable state with a deliberate concurrency design. Do not store request-specific data in fields.

Common scopes have different ownership:

| Scope | Identity and lifetime | Typical use | Primary risk |
|---|---|---|---|
| singleton | one managed instance per definition in a context | stateless service, repository, configuration | shared mutable state and unsafe publication assumptions |
| prototype | new instance each time the container supplies it | uncommon stateful helper | container does not manage the full destruction lifecycle in the same way as singletons |
| request | one instance for an HTTP request | request metadata or aggregation | access outside an active request; accidental capture by long-lived tasks |
| session | one instance per HTTP session | limited user-session state | memory growth, clustering/serialization, concurrent requests in one session |
| application | servlet-context lifetime | servlet application state | confusion with Spring singleton boundaries |

Injecting a shorter-lived bean directly into a singleton needs an indirection such as a scoped proxy or provider. The decision rule is ownership: the long-lived object must not capture one short-lived instance forever. Prefer passing request data as method arguments when that is clearer.

### Failure walkthrough: mutable singleton controller

```java
@RestController
final class UnsafeSequenceController {
    private long next = 1; // shared by concurrent requests

    @PostMapping("/numbers")
    long allocate() {
        return next++;
    }
}
```

Two request threads can read the same value and lose an increment. Even replacing `long` with `AtomicLong` would only make this local counter atomic; it would not make it durable, globally unique across instances, or transactionally connected to database work. The correct design begins with the required identity contract, often a database sequence or an external ID strategy.

## 3. Configuration and Boot auto-configuration

### Configuration is precedence, conditions, and binding

Spring configuration combines bean definitions and an `Environment` of property sources. Spring Boot adds conventions and conditional auto-configuration. The correct mental model is not “Boot guesses.” It is:

```text
explicit application configuration
        + discovered auto-configuration candidates
        + conditions (classes, beans, properties, web type, resources)
        = effective bean graph
```

Auto-configuration commonly activates when required classes are present and expected user beans are absent. User configuration can make a default back away. Conditions and ordering belong to the supported Boot contract; the contents of auto-configuration classes are not an application API unless documentation says otherwise.

Use typed configuration properties for a cohesive group:

```java
// Dependency-requiring: Spring Boot configuration binding.
@ConfigurationProperties("payments")
public record PaymentProperties(
        URI baseUrl,
        Duration connectTimeout,
        Duration responseTimeout,
        int maxAttempts) {

    public PaymentProperties {
        Objects.requireNonNull(baseUrl);
        Objects.requireNonNull(connectTimeout);
        Objects.requireNonNull(responseTimeout);
        if (maxAttempts < 1 || maxAttempts > 5) {
            throw new IllegalArgumentException("maxAttempts must be 1..5");
        }
    }
}
```

Binding provides values; validation protects the contract. Secrets should enter through an approved secret mechanism, should never be rendered by an endpoint, and should not be copied into exception messages. Configuration changes that require restart must be treated differently from dynamic control-plane changes.

### Diagnosing auto-configuration

When a bean is missing or duplicated, do not add annotations randomly. Inspect:

1. the dependency graph and active profiles;
2. the configuration-properties prefix and actual value source;
3. conditional evaluation or startup report;
4. bean names, types, qualifiers, and primary selection;
5. component-scan boundaries;
6. test-slice exclusions;
7. framework and BOM compatibility.

A condition report is evidence, not part of business logic. A robust application does not depend on auto-configuration implementation classes that official documentation labels internal.

## 4. Proxies and AOP: reason from the call path

### Formal interception model

Many Spring services are exposed as proxies to apply transactions, method security, caching, metrics, or other advice. Model a proxied call as:

```text
caller -> proxy -> ordered advice chain -> target method
```

Advice can run only if the call passes through the relevant proxy. A call from one target method to another on `this` is an ordinary Java call and can bypass proxy advice. Private methods are not a reliable proxy interception boundary. Final classes/methods constrain subclass-proxy strategies. Interface-based and class-based proxies have different type surfaces. The exact strategy is configuration and framework-version sensitive.

Recognition rule: whenever an annotation appears to change method execution, draw the runtime reference and call path. Ask which object the caller holds. Never reason from the annotation alone.

### Self-invocation failure

```java
@Service
class ImportService {
    void importBatch(List<Row> rows) {
        for (Row row : rows) {
            importOne(row); // direct call on this
        }
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    void importOne(Row row) {
        // The expected separate transaction may not be created.
    }
}
```

The intended boundary is “one transaction per row,” but the call path never re-enters the proxy. Better designs include moving the per-row use case to a separate injected bean, using an explicit transaction template, or changing the unit of work. Injecting “self” is usually a warning sign because it teaches application code to accommodate interception mechanics.

### Advice ordering and failure semantics

If retry and transaction advice both apply, order changes behavior:

```text
retry outside transaction: each attempt may receive a fresh transaction
transaction outside retry: retries may occur inside one doomed transaction
```

The correct choice depends on the recoverable failure and transaction manager. Declare and test the intended ordering. Metrics advice must not accidentally transform exceptions. Security must occur before protected state changes. Caching around a mutating method requires explicit invalidation semantics.

## 5. Transaction boundaries and traps

### The business invariant defines the boundary

Suppose confirming an order must transition `PENDING -> CONFIRMED`, reserve inventory, and append an outbox event atomically in one database. That use case—not each repository call—is the natural local transaction boundary.

```java
// Dependency-requiring: Spring transaction support.
@Service
final class ConfirmOrderUseCase {
    private final OrderRepository orders;
    private final InventoryRepository inventory;
    private final OutboxRepository outbox;

    @Transactional
    public Confirmation confirm(OrderId id, long expectedVersion) {
        Order order = orders.require(id);
        order.confirm(expectedVersion);
        inventory.reserve(order.lines());
        outbox.append(OrderConfirmed.from(order));
        return Confirmation.from(order);
    }
}
```

The remote publication is not inside the database's atomic domain. An outbox relay publishes after commit and consumers deduplicate by event identity. Calling a broker or payment provider while holding locks expands latency and still cannot create atomicity across independent systems.

### Rollback is policy, not punctuation

Declarative transactions decide rollback using the configured transaction manager and rollback rules. Do not memorize a one-line rule and assume every exception hierarchy matches it. Define domain failures deliberately, inspect the supported Spring behavior, and add integration tests that verify committed database state.

Important traps:

- catching an exception inside the method and returning success can allow commit;
- a transaction may already be marked rollback-only, causing failure at commit even after a catch;
- an asynchronous continuation normally runs in another thread and does not inherit a thread-bound transaction;
- `readOnly = true` is a hint/optimization contract, not a universal database authorization mechanism;
- a timeout is not proof that remote or database work did not occur;
- `REQUIRES_NEW` can consume another pooled connection while the outer transaction holds one;
- a long transaction increases lock duration, version conflict probability, and pool occupancy;
- an isolation name does not guarantee identical vendor behavior.

### Propagation decision table

| Need | Candidate | Reasoning and caveat |
|---|---|---|
| join an existing use-case transaction, otherwise create one | required/default behavior | common service boundary |
| run audit work independently of caller outcome | new transaction may be considered | can persist even when business work fails; connection pressure and semantic surprise |
| prohibit accidental transaction | never-style policy | useful for remote calls or orchestration that must not hold database resources |
| savepoint-like nested work | nested behavior if supported | manager/resource dependent; do not equate with an independent transaction |
| explicit complex control flow | transaction template/API | makes begin/commit scope visible and avoids self-invocation ambiguity |

Propagation does not solve distributed transactions, idempotency, or event publication. It only specifies interaction with a transaction context understood by a manager.

## 6. Testing strategy

Use the smallest test that can falsify the claim:

- **plain unit test:** construct a service directly; test decisions and domain transitions;
- **container wiring test:** load a narrow configuration; assert bean selection and properties validation;
- **proxy behavior test:** use a real Spring context when interception is the subject;
- **database integration test:** use the production database engine/version when isolation, SQL, or locking matters;
- **end-to-end test:** verify request mapping, security, serialization, transaction, and persistent state for a few critical paths.

Mocking a repository cannot prove transaction rollback. Calling `new ConfirmOrderUseCase(...)` cannot prove proxy advice. Conversely, a full application test is a slow and opaque way to test a pure state transition.

## 7. Interview questions and model checkpoints

### Q1. Why is constructor injection preferred?

**Model checkpoint:** it exposes required dependencies, allows immutable fields, makes invalid partial construction difficult, and supports plain-Java tests. It does not by itself solve cycles or guarantee thread safety.

### Q2. Why might `@Transactional` appear to do nothing?

**Model checkpoint:** draw the call path. The target may not be a managed bean, the call may bypass the proxy through self-invocation, the method may not be interceptable under the configured proxy strategy, the wrong transaction manager may be selected, or the observed effect may reflect rollback rules rather than missing interception.

### Q3. Is a Spring singleton thread-safe?

**Model checkpoint:** scope determines identity and lifetime, not thread safety. A singleton is shared by request threads; immutable/stateless design or explicit synchronization is required.

### Q4. What does Boot auto-configuration guarantee?

**Model checkpoint:** it contributes conditional configuration based on documented conditions and normally backs away from specific user configuration. Application code should depend on public configuration contracts, not internal auto-configuration methods.

### SDE-2 follow-ups

1. A batch job needs one transaction per item and currently calls an annotated method on `this`. Refactor it and explain pool sizing.
2. A service must write a row and publish Kafka. Draw failure windows and design an outbox relay.
3. A request-scoped identity is needed in an asynchronous task. Explain why capturing a scoped proxy is risky and propose explicit context propagation.
4. A library auto-configuration unexpectedly overrides a test bean. Describe the evidence you would collect before changing precedence.

## 8. Exercises

1. Draw the bean graph for a checkout service with two gateways, a repository, a clock, and a fraud policy. Mark qualifiers and ownership.
2. Refactor a constructor cycle between `OrderService` and `InvoiceService` by extracting the invariant-owning use case.
3. Write a test matrix for a transaction that updates inventory and inserts an outbox record. Include a constraint violation and simulated relay failure.
4. Explain whether a prototype bean injected into a singleton produces a new instance on every method call. Propose two correct alternatives.
5. Design an experiment that proves whether retry advice wraps transaction advice or the reverse without depending on log order alone.

## 9. Summary checklist

- [ ] Required collaborators are constructor parameters.
- [ ] Scope matches ownership; singleton state is immutable or deliberately synchronized.
- [ ] Startup and shutdown resource ownership are paired.
- [ ] Auto-configuration is diagnosed through conditions and public configuration contracts.
- [ ] Every annotation-based cross-cutting behavior is explained through the proxy call path.
- [ ] Transactions wrap business invariants, not arbitrary repository methods.
- [ ] Remote effects are excluded from local atomicity and made retry-safe.
- [ ] Rollback, propagation, isolation, and pool behavior have integration tests.

## 10. Decision labs and model walkthroughs

### Lab A: configuration override without ambiguity

A service needs a production `FraudClient`, a local-development stub, and a deterministic test fake. A fragile design scans all three and hopes profile, primary, and bean-name precedence select the intended one. A stronger design defines one application port and one explicit bean per environment boundary:

```java
// Dependency-requiring Spring configuration; classes are package-private.
@Configuration(proxyBeanMethods = false)
class FraudConfiguration {
    @Bean
    @ConditionalOnMissingBean(FraudClient.class)
    FraudClient httpFraudClient(FraudProperties properties,
                                HttpClient client) {
        return new HttpFraudClient(properties, client);
    }
}

@TestConfiguration(proxyBeanMethods = false)
class FraudTestConfiguration {
    @Bean
    FraudClient deterministicFraudClient() {
        return command -> FraudDecision.accepted("test-policy");
    }
}
```

The test imports its configuration deliberately. The application asserts exactly one `FraudClient`. An auto-configuration condition can provide a default, but application code must not depend on the condition's internal class structure. Test the following matrix:

| Context | Expected bean | Evidence |
|---|---|---|
| production dependencies and properties | HTTP adapter | narrow context starts; properties validated |
| user supplies custom adapter | custom adapter | default condition backs away |
| test imports fake | deterministic fake | no network client constructed |
| required property absent | startup fails | actionable binding/validation message |
| two explicit user adapters | startup fails unless qualified | ambiguity is visible, not silently ordered |

This is the difference between convenient defaults and implicit behavior. Auto-configuration should reduce wiring, not hide which capability the application receives.

### Lab B: transaction boundary under proxy and pool pressure

Suppose `CheckoutService.checkout()` is transactional and invokes a payment provider while the transaction is open. Load testing shows:

```text
request rate:              300/s
payment p95:               800 ms
DB statement time:          25 ms
pool maximum:               60
pool acquisition p95:     2.4 s
```

The remote wait keeps about `300 * 0.8 = 240` concurrent operations wanting connections before counting other queries. A 60-connection pool cannot hide that mismatch. Enlarging it to 240 may overload the database and lengthen locks.

Refactor to a durable workflow:

1. short transaction creates `PAYMENT_PENDING` order, operation ID, and outbox command;
2. worker calls provider outside the database transaction under a deadline;
3. second short transaction accepts provider result only from the expected order version/state and appends the next event;
4. timeout stores `PAYMENT_UNKNOWN` and schedules reconciliation by operation ID;
5. duplicate provider results are idempotent;
6. cancellation races through optimistic state transitions.

The price is eventual completion and a richer state machine. The benefit is honest atomicity, bounded pool occupancy, crash recovery, and explicit unknown outcomes.

### Lab C: proxy advice matrix

For each call, predict interception before running a test:

| Call path | Transaction advice expected? | Reason |
|---|---:|---|
| controller holds proxied service and calls advised public method | yes under normal proxy configuration | call enters proxy |
| target method calls another advised method on `this` | commonly no | direct target call bypasses proxy |
| object constructed with `new` outside container | no | no container proxy reference |
| scheduled adapter calls injected proxied use case | yes if scheduling target and service wiring are managed | call crosses bean boundary |
| new thread invokes captured target rather than proxy | no/unsafe assumption | reference and transaction context differ |
| method returns a future and work continues later | only invocation boundary is certain | later work has its own thread/context semantics |

Then write one integration test that records transaction-active state or committed rows at each boundary. Avoid making tests depend on proxy class names; test the application-visible effect.

### Lab D: rollback and exception translation

Consider a unique order constraint. The persistence adapter should recognize the exact database/driver constraint signal and translate it to `DuplicateExternalOrder`; unexpected connection and syntax failures remain infrastructure errors. The use case maps a duplicate combined with the same idempotency fingerprint to replay, but a different fingerprint to conflict.

Do not translate every `DataIntegrityViolationException` to `409`; it can represent a programming error, null/check/foreign-key violation, or another constraint. Do not use exception message substrings as the only stable classifier. Integration tests assert:

- transaction leaves no partial rows;
- exact known constraint maps to stable domain outcome;
- unknown constraint is not mislabeled as client error;
- rollback occurs even if exception crosses a proxy boundary;
- replay authorization is re-evaluated.

### Decision-lab checkpoint

An SDE-2 answer should make the managed reference, advice path, transaction resource, pool slot, and durable state visible. If the explanation relies on “Spring handles it,” draw the call and commit sequence until each failure has an owner.

## Primary references

- Spring Framework Reference, “The IoC Container”: <https://docs.spring.io/spring-framework/reference/core/beans.html>
- Spring Framework Reference, “Aspect Oriented Programming with Spring”: <https://docs.spring.io/spring-framework/reference/core/aop.html>
- Spring Framework Reference, “Transaction Management”: <https://docs.spring.io/spring-framework/reference/data-access/transaction.html>
- Spring Boot Reference, “Auto-configuration”: <https://docs.spring.io/spring-boot/reference/using/auto-configuration.html>
- Spring Boot Reference, “Externalized Configuration”: <https://docs.spring.io/spring-boot/reference/features/external-config.html>

> **Version boundary:** examples use Java 21 syntax and Jakarta-era Spring APIs. Exact proxy constraints, callback ordering, condition names, defaults, and supported baselines can change between Spring Framework and Spring Boot release lines. Use the documentation selected by the application's dependency BOM. Later-JDK features are not assumed.
