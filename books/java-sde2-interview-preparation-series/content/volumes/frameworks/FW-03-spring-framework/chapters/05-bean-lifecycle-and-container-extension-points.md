# Bean Lifecycle and Container Extension Points

The bean lifecycle is a pipeline. Knowing the order explains why a dependency is null, why a callback ran twice, or why one bean missed proxying.

## Lifecycle of a normal singleton

```text
bean definition registered
        |
        v
instantiate object
        |
        v
populate dependencies and properties
        |
        v
Aware callbacks (when implemented)
        |
        v
BeanPostProcessor before initialization
        |
        v
initialization callbacks
        |
        v
BeanPostProcessor after initialization
        |       (a proxy may be returned here)
        v
ready bean exposed to callers
        |
        v
context close -> destruction callbacks for eligible scopes
```

This is a learning model, not every internal callback. The key distinction is between changing **bean definitions before instances exist** and processing **bean instances around initialization**.

## Initialization and destruction

Prefer a plain method named in configuration when practical:

```java
final class ConnectionRegistry {
    void initialize() {
        // Validate local state; do not perform unbounded remote work.
    }

    void close() {
        // Release resources owned by this bean.
    }
}

@Bean(initMethod = "initialize", destroyMethod = "close")
ConnectionRegistry connectionRegistry() {
    return new ConnectionRegistry();
}
```

`@PostConstruct` and `@PreDestroy` are also common and use `jakarta.annotation` in current Jakarta-based applications. `InitializingBean` and `DisposableBean` work but couple the class to Spring. Pick one lifecycle style for a component; combining all styles creates order questions and duplicate work.

## Constructors should establish local validity

A constructor should validate required arguments and establish invariants. Avoid long network calls, thread creation, or database migrations in it. Heavy startup work:

- increases failure ambiguity;
- runs before the full context is ready;
- complicates tests and shutdown;
- may be repeated during test context creation.

If the application must not accept traffic until a dependency is ready, use an explicit readiness component with bounded timeouts and observable failure rather than a hidden constructor call.

## Definition-level extension

`BeanFactoryPostProcessor` can inspect or change bean definitions before ordinary beans are instantiated. A familiar implementation resolves external property tokens. Custom implementations are infrastructure code and should avoid creating application beans prematurely.

```java
@Bean
static BeanFactoryPostProcessor requireNamedBean() {
    return factory -> {
        if (!factory.containsBeanDefinition("orderService")) {
            throw new IllegalStateException("orderService definition is required");
        }
    };
}
```

The factory method is `static` so the post-processor can be registered without early instantiation of its configuration class.

## Instance-level extension

`BeanPostProcessor` sees bean instances before and after initialization. Framework features use post-processors for annotation handling and proxy creation.

```java
final class TimingMarkerPostProcessor implements BeanPostProcessor {
    @Override
    public Object postProcessAfterInitialization(Object bean, String name) {
        if (bean instanceof TimedComponent) {
            System.out.println("timed bean: " + name);
        }
        return bean;
    }
}
```

Returning a different object is legal; that is one route to a proxy. A post-processor must return the bean, even when unchanged. Returning null prematurely stops subsequent processing for that phase.

## Aware callbacks and framework coupling

Interfaces such as `BeanNameAware` and `ApplicationContextAware` inject container infrastructure. They are appropriate in framework adapters but are usually a smell in business services. If a service needs a clock, publisher, or strategy, inject that focused interface rather than the entire context.

## Lifecycle ordering

`@DependsOn` can force initialization order, but dependency injection is clearer when one bean truly depends on another. `SmartLifecycle` coordinates start/stop phases for active components such as listeners. It is not a substitute for a well-defined dependency graph.

On shutdown, give in-flight work a bounded drain period, stop accepting new work, and release resources. Do not assume a destruction callback runs after abrupt process termination.

## Prototype lifecycle limit

Spring creates and initializes prototype beans but does not automatically call their destruction callbacks. The recipient owns cleanup. This is one reason prototype beans should not casually own files, threads, or connections.

## Common mistakes

- Calling application beans from a factory post-processor and causing early creation.
- Returning null from a post-processor accidentally.
- Starting non-daemon threads without a shutdown contract.
- Expecting destruction callbacks after `kill -9` or a crash.
- Implementing `ApplicationContextAware` as a service locator.
- Registering a post-processor with a factory method whose return type hides its post-processor nature.

## Interview angle

**Interviewer:** Difference between `BeanFactoryPostProcessor` and `BeanPostProcessor`?

**Strong answer:** The first works on bean-definition metadata before normal instances are created. The second works on instances around initialization and may return wrappers such as proxies. I avoid resolving application beans from either too early because that can skip later processing or create incomplete infrastructure.

## Quick check

1. At what stage are dependencies populated?
2. Why may the final exposed bean differ from the constructed target?
3. Which extension point changes definition metadata?
4. Who destroys prototype instances?
5. Why should business code avoid `ApplicationContextAware`?

## Predict and debug

**Predict:** An `@Bean` method returns a class with `close()`. Spring may infer and call a public close/shutdown destroy method for a singleton, but explicit lifecycle configuration is clearer for interview reasoning.

**Debug:** A bean is not advised because it was fetched while an infrastructure post-processor was being constructed. Inspect early creation warnings, simplify the post-processor dependencies, and ensure infrastructure return types are declared precisely.

## Practice

- **Foundation:** Add init and destroy methods to a managed resource and close the context.
- **Foundation:** Write the lifecycle stages in order from definition to shutdown.
- **Interview Core:** Build a post-processor that records bean names without changing them.
- **Interview Core:** Explain why a slow constructor damages startup diagnosis.
- **SDE-2 Follow-up:** Design start, readiness, drain, stop, and forced-termination behavior for a message listener.

## Readiness checkpoint

Continue when you can locate an action at definition time, instance initialization, post-processing, runtime, or shutdown and name who owns cleanup.
