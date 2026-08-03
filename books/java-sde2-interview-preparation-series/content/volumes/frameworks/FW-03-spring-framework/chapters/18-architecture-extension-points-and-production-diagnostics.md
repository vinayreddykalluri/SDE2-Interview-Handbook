# Architecture, Extension Points, and Production Diagnostics

SDE-2 Spring skill is not measured by annotation count. It is the ability to keep the object graph understandable, extend the container safely, and diagnose runtime behavior from evidence.

## A healthy application shape

```text
HTTP / message / schedule adapters
             |
             v
application use-case services  <- transaction and authorization boundary
             |
             v
domain policies and state      <- ordinary Java
             |
             v
ports -> database / remote / broker adapters
```

Spring assembles these layers. Domain code need not depend on `ApplicationContext`, `@Transactional`, HTTP types, or persistence entities when the separation adds value. Do not turn this into ceremony: a small application can use fewer layers while preserving explicit boundaries.

## Container extension point selection

| Need | Extension point |
|---|---|
| change/register bean metadata before instances | `BeanFactoryPostProcessor` or registry-level processor |
| wrap/inspect instances around initialization | `BeanPostProcessor` |
| create an object through a specialized factory contract | `FactoryBean<T>` |
| alter import/registration from annotation metadata | registrar/selector infrastructure |
| customize scope | `Scope` implementation |
| react to context lifecycle | listener or lifecycle interface |

These are library and platform tools, not everyday application defaults. A custom extension must document phase, ordering, eligible beans, side effects, thread safety, native/AOT implications, and failure diagnostics.

## `FactoryBean` versus factory-method bean

`@Bean PaymentClient paymentClient()` is an ordinary factory method that registers its returned product. `FactoryBean<PaymentClient>` is itself a special bean whose normal lookup returns the product; prefixing the name with `&` addresses the factory. Many candidates confuse the two.

Use `FactoryBean` when reusable factory semantics require access to the container contract. For application configuration, an `@Bean` method is usually simpler.

## Avoid container reach-through

Weak:

```java
final class PricingService implements ApplicationContextAware {
    private ApplicationContext context;

    Money calculate(String strategyName, Cart cart) {
        return context.getBean(strategyName, PricingStrategy.class).price(cart);
    }
}
```

Stronger:

```java
final class PricingService {
    private final Map<PricingTier, PricingStrategy> strategies;

    PricingService(List<PricingStrategy> candidates) {
        this.strategies = candidates.stream().collect(Collectors.toUnmodifiableMap(
                PricingStrategy::tier, Function.identity()));
    }
}
```

The stronger class exposes its dependency and owns allowlisted business routing without bean-name coupling.

## Startup diagnostics

For a failed context:

1. Record the active profiles and non-secret configuration sources.
2. Read the deepest cause and identify the injection point or factory method.
3. Determine whether failure is definition, resolution, construction, initialization, or post-processing.
4. List matching candidate names/types and why each was included.
5. Inspect conditions/profiles/imports and accidental broad scans.
6. Check early-creation and proxy-eligibility warnings.
7. Reproduce with the smallest context.

Do not "fix" startup by making required beans lazy. That moves the same defect to the first request.

## Runtime proxy diagnostics

For missing transactions, metrics, authorization, caching, or async behavior:

- confirm managed bean and actual injected reference;
- inspect proxy type and advisors;
- verify external call versus self-invocation;
- check method visibility/finality and annotation resolution;
- confirm feature enablement and manager/executor selection;
- assert behavior with a failure path, not a proxy-class screenshot alone.

## Performance model

Spring method interception overhead is rarely the first production bottleneck compared with database, network, serialization, lock wait, or unbounded work. Measure before replacing abstractions. More common framework-related costs include:

- very large contexts and broad scanning;
- blocking remote calls in startup or request threads;
- long transactions and pool exhaustion;
- unbounded async queues;
- accidental request-time bean creation;
- high-cardinality logs/metrics from generic aspects;
- fragmented test context caches.

## Observability boundaries

For each use case record a stable operation name, duration, outcome category, trace correlation, and resource timings. Avoid raw customer/order IDs as metric labels. Logs should explain policy decisions without revealing secrets. Health and readiness should be bounded and should distinguish "process alive" from "safe to receive traffic." Full Spring Boot Actuator treatment is in **SD 05**.

## Security posture

- Minimize package scanning and reflection over untrusted types.
- Do not evaluate user input as SpEL.
- Do not expose bean names, environment dumps, or stack traces publicly.
- Treat configuration values and event payloads as sensitive by classification.
- Keep authorization at explicit use-case boundaries; AOP may enforce it but tests must prove coverage.
- Patch supported Spring lines and review dependency advisories through the build and release process.

Spring Security, reactive WebFlux, Spring Batch, and Spring Integration receive dedicated treatment in **SD 10 - Spring Ecosystem Extensions**. This volume introduces boundaries without duplicating those books.

## Incident scenario: wrong payment adapter

**Symptom:** production routes real payments to a sandbox endpoint after a deployment.

**Evidence plan:** inspect active profiles, effective non-secret endpoint, bean candidates, configuration import, and deployment change. Confirm the actual `PaymentGateway` bean class and settings. Do not dump tokens.

**Correction:** fail startup when the production deployment uses a non-approved host; replace broad profile combinations with one validated settings contract and explicit adapter selection; add deployment-policy and focused context tests.

## Common mistakes

- Building a service locator on `ApplicationContext`.
- Writing custom post-processors for ordinary dependency injection.
- Making required infrastructure lazy to hide startup failures.
- Optimizing proxy overhead before measuring external work.
- Logging effective configuration including secrets.
- Treating health checks as unlimited integration tests.

## Interview angle

**Interviewer:** How would you debug a Spring bean that exists but is not transactional?

**Strong answer:** I confirm the caller's reference is container-managed, inspect whether it is proxied and which advisors apply, verify the call crosses the proxy, check method visibility and resolved transaction metadata, confirm transaction management and the selected manager, then run a focused rollback test. I avoid toggling proxy mode blindly because the root cause may be object ownership, self-invocation, or exception rules.

## Quick check

1. When is a custom post-processor justified?
2. How does `FactoryBean` differ from an `@Bean` method?
3. Why is making a missing dependency lazy not a repair?
4. Which costs usually dominate proxy overhead?
5. What must a readiness check avoid?

## Practice

- **Foundation:** Place controller, use case, domain policy, port, and adapter in the architecture.
- **Foundation:** Match five needs to container extension points.
- **Interview Core:** Replace a context-based strategy lookup with explicit injection.
- **Interview Core:** Write a startup-failure diagnostic checklist for an ambiguous bean.
- **SDE-2 Follow-up:** Lead a wrong-adapter incident from containment through preventive controls.

## Readiness checkpoint

Continue when you can extend Spring only at the correct lifecycle phase and diagnose wiring, proxy, transaction, and capacity incidents from a small evidence plan.
