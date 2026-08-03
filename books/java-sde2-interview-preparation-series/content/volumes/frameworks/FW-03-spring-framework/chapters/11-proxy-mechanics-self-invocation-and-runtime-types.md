# Proxy Mechanics, Self-Invocation, and Runtime Types

Most surprising Spring behavior is ordinary Java dispatch around an extra object. Draw the proxy before changing annotations.

## JDK and subclass proxies

Spring AOP can use:

- **JDK dynamic proxy:** implements interfaces exposed by the target.
- **Subclass proxy:** runtime-generated subclass of the target class, using repackaged CGLIB support.

```text
JDK proxy                         subclass proxy
implements OrderOperations       extends OrderService
          |                              |
          v                              v
       target                         target logic
```

With interfaces, core Spring AOP commonly chooses JDK proxies by default. Class-based proxying can be requested. Spring Boot may apply its own defaults; do not project Boot behavior onto every Framework context.

## The external-call requirement

```java
@Service
class BillingService {
    void billAll() {
        billOne();             // this.billOne(): target-to-target call
    }

    @AuditedOperation
    public void billOne() { }
}
```

When an external caller invokes `proxy.billOne()`, advice can run. After control reaches the target, `billAll()` calling `this.billOne()` does not re-enter the proxy. The advice on `billOne` is bypassed.

```text
external caller -> proxy -> target.billAll
                              |
                              +-> this.billOne  (no proxy crossing)
```

Preferred correction: extract the advised operation into a separate collaborator and call it through its injected proxy.

```java
@Service
final class SingleBillingService {
    @AuditedOperation
    public void billOne() { }
}

@Service
final class BatchBillingService {
    private final SingleBillingService single;

    BatchBillingService(SingleBillingService single) {
        this.single = single;
    }

    void billAll() {
        single.billOne();
    }
}
```

Self-injection and `AopContext.currentProxy()` exist but couple design to the proxy mechanism and are weaker defaults.

## Methods that cannot be advised reliably

For class-based proxies:

- final classes cannot be subclassed;
- final methods cannot be overridden;
- private methods cannot be overridden;
- methods not visible to the subclass may not be advised.

JDK proxies expose interface methods. A caller typed to a concrete class may fail or bypass the intended contract when the bean is a JDK proxy. Design application ports as interfaces when that abstraction is real; do not create meaningless interfaces solely to satisfy a myth that Spring requires them.

## Runtime class surprises

```java
Object bean = context.getBean("orderService");
System.out.println(bean.getClass());
```

The class may be a generated proxy, not exactly `OrderService.class`. Avoid exact-class equality for behavior decisions. Use interfaces, `instanceof` thoughtfully, and Spring utilities when infrastructure must resolve a target class. Domain code should not inspect proxy types.

## Annotations and interface placement

For consistent transaction discovery across proxy styles, place annotations on concrete implementation methods/classes as recommended by Spring documentation, unless your framework contract and tests explicitly support interface annotations. Meta-annotations and inheritance rules can become subtle; verify the actual method Spring resolves.

## Proxy chains and identity

One bean may have several advisors, usually in one proxy chain. Avoid manually wrapping Spring proxies in unrelated proxies without understanding equality, serialization, and type exposure. `equals` and `hashCode` should reflect domain/value semantics, not target/proxy identity assumptions.

## Diagnosing an annotation that does nothing

Use this checklist:

1. Is the object a Spring bean or created with `new`?
2. Is the feature enabled and its post-processor registered?
3. Did the caller receive the proxy?
4. Is this an external call or self-invocation?
5. Is the method visible and overridable for the proxy type?
6. Does the pointcut/annotation match the resolved method?
7. Is a later post-processor replacing the bean unexpectedly?
8. Does a focused test assert advice execution rather than only successful business output?

## Common mistakes

- Saying Spring modifies the target method body.
- Expecting private/final methods to be advised by subclass proxies.
- Calling an advised method from the same object.
- Casting a JDK proxy to its concrete implementation.
- Checking exact runtime class in business code.
- Assuming adding an annotation enables its infrastructure.

## Interview angle

**Interviewer:** Why does `@Transactional` work from a controller but not from another method in the same service?

**Strong answer:** In the normal proxy mode, the controller calls the Spring proxy, so transaction advice intercepts the call. A same-instance call uses `this` after control reached the target and bypasses the proxy. I move the transactional operation to a separate bean or place the transaction at the externally invoked orchestration method, then prove it with a rollback test.

## Quick check

1. What does a JDK proxy expose?
2. Why does self-invocation bypass advice?
3. Why can final methods be a problem for subclass proxies?
4. Should business code inspect generated proxy classes?
5. What is the first diagnostic question for an ineffective annotation?

## Predict and debug

**Predict:** A JDK proxy implements `OrderOperations`; casting it to `OrderService` can throw `ClassCastException`.

**Debug:** An audit annotation on a private helper never runs. Put the policy boundary on an externally invoked public operation or make the behavior explicit; do not expose internals solely for proxying.

## Practice

- **Foundation:** Draw external proxy call and self-invocation paths.
- **Foundation:** Inspect whether a context bean is an AOP proxy.
- **Interview Core:** Refactor one self-invoking service into two collaborators.
- **Interview Core:** Test interface-based and class-based proxy type exposure.
- **SDE-2 Follow-up:** Diagnose a production policy bypass introduced when a method became final.

## Readiness checkpoint

Continue when you can predict interception from object ownership, call path, proxy type, and method visibility without guessing from the annotation.
