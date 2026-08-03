# AOP: Join Points, Pointcuts, and Advice

Aspect-oriented programming applies one policy across many method calls without copying policy code into every service. Spring AOP is primarily **proxy-based method interception** for Spring-managed beans.

## Vocabulary through one call

```text
caller -> proxy -> target method
           |
           +-- before advice
           +-- around advice (can proceed, replace, time, fail)
           +-- after returning / after throwing / finally-style advice
```

- **Join point:** a point where advice can run. In Spring AOP, think method execution reached through a proxy.
- **Pointcut:** a predicate selecting join points.
- **Advice:** behavior that runs at selected join points.
- **Aspect:** the pointcut and associated advice as one concern.
- **Target:** the underlying application object.
- **Proxy:** the object callers invoke so advice can run before delegating.

## A timing aspect

```java
@Aspect
@Component
final class TimingAspect {
    @Around("execution(* com.example.orders..*(..))")
    Object time(ProceedingJoinPoint call) throws Throwable {
        long start = System.nanoTime();
        try {
            return call.proceed();
        } finally {
            long elapsed = System.nanoTime() - start;
            System.out.println(call.getSignature() + " " + elapsed + "ns");
        }
    }
}
```

`call.proceed()` invokes the next interceptor or target. Forgetting it replaces the call. Calling it twice executes the target twice. Around advice therefore requires careful review.

Enable AspectJ annotation interpretation for Spring AOP:

```java
@Configuration
@EnableAspectJAutoProxy
class AopConfiguration { }
```

The AspectJ annotation style here configures Spring proxy AOP; it does not automatically mean compile-time or load-time AspectJ weaving.

## Pointcut design

Broad expression:

```java
execution(* com.example..*(..))
```

This may advise configuration, infrastructure, getters, or framework callbacks unintentionally. Prefer an architectural boundary or marker annotation:

```java
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
@interface AuditedOperation { }

@Around("@annotation(AuditedOperation)")
Object audit(ProceedingJoinPoint call) throws Throwable { ... }
```

Marker annotations make opt-in visible but also couple the method to that policy metadata. Package or interface pointcuts keep application code unaware but can be too broad. Choose deliberately and test matching.

## Advice ordering

Transactions, retries, authorization, metrics, and auditing may all wrap one call:

```text
authorization -> retry -> transaction -> metrics -> target
```

This order is semantic. Retrying outside the transaction can create a fresh transaction per attempt; retrying inside may repeat work in one already-failed transaction, which is often wrong. State and test the intended nesting with `@Order` or ordered advisors. Do not rely on incidental registration order.

## Good and poor AOP candidates

Good candidates are policies with a stable call boundary:

- transaction demarcation;
- authorization checks;
- metrics and tracing;
- bounded retries for classified failures;
- standardized auditing metadata.

Poor candidates hide domain workflow or change return values unexpectedly. If an aspect needs detailed knowledge of every method's business state, the concern may belong in explicit application code.

## Exception and argument handling

Log or audit only safe metadata. A generic aspect sees arguments from many domains, including tokens, passwords, and personal data. Avoid serializing all arguments and return values. Preserve the original throwable unless the boundary has an explicit translation contract.

## Testing an aspect

1. Unit-test pure policy helpers without Spring.
2. Start a focused context containing the target, aspect, and enablement.
3. Assert the bean is proxied.
4. Call through the context-provided reference.
5. Assert match and non-match cases, advice order, return value, and exception preservation.

## Common mistakes

- Equating AspectJ annotations with AspectJ weaving.
- Using a pointcut so broad that infrastructure beans are advised.
- Forgetting or duplicating `proceed()`.
- Logging all arguments and leaking secrets.
- Assuming advice order.
- Using AOP to hide core business steps.

## Interview angle

**Interviewer:** When would you use Spring AOP?

**Strong answer:** For a cross-cutting policy with a clear method boundary, such as transactions or metrics, when proxy interception is sufficient. I define a narrow, testable pointcut, document advice order, preserve failure semantics, and account for proxy limits such as self-invocation and non-advisable methods. I keep domain workflow explicit.

## Quick check

1. What does a pointcut select?
2. What is the consequence of omitting `proceed()`?
3. Why is advice order part of correctness?
4. Does `@Aspect` imply bytecode weaving?
5. Which data should a generic logging aspect avoid?

## Predict and debug

**Predict:** Around advice calls `proceed()` twice. The target can execute twice and duplicate side effects.

**Debug:** A metric appears on every configuration method. Narrow the package, interface, or annotation pointcut and add a non-match test.

## Practice

- **Foundation:** Label caller, proxy, advice, and target in a diagram.
- **Foundation:** Write around advice that always calls `proceed()` exactly once.
- **Interview Core:** Design match and non-match tests for an audit annotation.
- **Interview Core:** Choose ordering for authorization, retry, transaction, and metrics.
- **SDE-2 Follow-up:** Replace an aspect that logs payloads with a privacy-safe structured telemetry contract.

## Readiness checkpoint

Continue when you can explain what call the aspect intercepts, what it may change, how advisors are ordered, and which tests prove the boundary.
