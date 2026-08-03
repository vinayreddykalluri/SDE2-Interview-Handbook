# Bean Scopes, State, and Thread Safety

Scope answers: **for one bean definition, when is a new instance supplied and who owns its lifecycle?** It does not automatically make a class thread-safe.

## Core scopes

| Scope | Instance boundary | Lifecycle note |
|---|---|---|
| `singleton` | one instance per bean definition per container | full lifecycle managed on normal context close |
| `prototype` | a new instance when the container resolves or requests it | destruction is not managed automatically |
| `request` | one instance per HTTP request | web-aware context required |
| `session` | one instance per HTTP session | web-aware context required; distributed session concerns remain |
| `application` | one per `ServletContext` | not identical to Spring singleton semantics |
| `websocket` | one per WebSocket session | web context required |

Spring singleton is not the GoF singleton pattern and not one per JVM. Two contexts can each own a singleton from the same definition class. Two bean definitions can each create one instance of the same Java class.

## Stateless singleton as the default

```java
@Service
final class PriceService {
    Money calculate(Cart cart, PricingPolicy policy) {
        return policy.price(cart);
    }
}
```

Method-local values are naturally isolated between calls. By contrast:

```java
@Service
final class UnsafeSequenceService {
    private long next = 1;

    long nextValue() {
        return next++;
    }
}
```

Concurrent calls race. Singleton scope did not serialize access. Use a database sequence, atomic primitive when semantics fit, or another coordination design. Never store request-specific user data in singleton fields.

## Prototype injection surprise

```java
@Component
@Scope(ConfigurableBeanFactory.SCOPE_PROTOTYPE)
final class WorkBuffer { }

@Service
final class ExportService {
    private final WorkBuffer buffer;

    ExportService(WorkBuffer buffer) {
        this.buffer = buffer;
    }
}
```

`ExportService` is a singleton, so its constructor runs once. It receives one prototype at that time and reuses it. Prototype means new on resolution, not automatically new on every method call.

For a fresh instance per operation:

```java
final class ExportService {
    private final ObjectProvider<WorkBuffer> buffers;

    ExportService(ObjectProvider<WorkBuffer> buffers) {
        this.buffers = buffers;
    }

    void export() {
        WorkBuffer buffer = buffers.getObject();
        // use and explicitly clean up owned resources
    }
}
```

Often a plain `new WorkBuffer()` in application code is simpler if the buffer has no container-provided dependencies.

## Scoped proxies

A singleton controller/service cannot hold the actual request-scoped object at startup because no request exists. A scoped proxy can be injected instead:

```text
singleton service -> stable request-scope proxy
                              |
                    each call resolves target
                              |
                              v
                    current request instance
```

The proxy does not copy request data into other threads. An `@Async` task has no automatic guarantee that request scope or thread-local context remains available. Capture only the small immutable values the task needs.

## Session scope caution

Session beans can increase memory, serialization, concurrency, and failover complexity. Multiple requests for one session may execute concurrently. Session scope does not serialize them. Keep authoritative business state in durable storage and session state minimal.

## Scope versus ownership

Ask four questions:

1. Who creates the instance?
2. How many callers can access it concurrently?
3. Who releases resources?
4. Can a longer-lived object retain it past its intended boundary?

A request-scoped object leaked into a static field defeats the scope. A prototype owning a thread without explicit shutdown leaks resources even though instances are short-lived.

## Common mistakes

- Saying Spring singleton is one instance for the whole JVM.
- Keeping mutable request state in singleton fields.
- Expecting prototype destruction callbacks.
- Injecting prototype directly into a singleton and expecting one per call.
- Passing request-scoped proxies into background threads.
- Using session scope as a database.

## Interview angle

**Interviewer:** Are singleton beans thread-safe?

**Strong answer:** Scope and thread safety are separate. A Spring singleton is shared per bean definition per container, so concurrent callers can execute it. Stateless services are usually safe; shared mutable fields require an explicit concurrency design. I also inspect collaborators and the scope of any proxy rather than declaring every singleton safe.

## Quick check

1. How does Spring singleton differ from GoF singleton?
2. When is a prototype created?
3. Who destroys a prototype?
4. Why is a scoped proxy useful?
5. Does session scope serialize requests?

## Predict and debug

**Predict:** A singleton receives a prototype in its constructor. How many prototype instances does that singleton normally retain? One.

**Debug:** Tenant ID stored in a singleton field intermittently crosses requests. Remove request state from the shared bean; pass tenant context as an immutable argument or use a correctly bounded request context with strict async handling.

## Practice

- **Foundation:** Prove two singleton lookups return one object and two prototype lookups return two.
- **Foundation:** Identify mutable fields in three service classes.
- **Interview Core:** Use `ObjectProvider` for one fresh operation object.
- **Interview Core:** Explain request proxy target resolution on two requests.
- **SDE-2 Follow-up:** Diagnose a cross-tenant leak involving singleton state and an executor.

## Readiness checkpoint

Continue when you can separate scope, thread safety, and resource ownership and can predict prototype behavior inside a singleton.
