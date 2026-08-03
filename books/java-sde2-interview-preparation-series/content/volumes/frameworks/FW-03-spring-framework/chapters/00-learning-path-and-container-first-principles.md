# Learning Path and Container First Principles

Spring Framework helps an application create objects, connect their dependencies, apply cross-cutting behavior, manage transactions, publish events, validate data, and serve web requests. It does not replace Java. The strongest Spring engineers can first write the object graph in plain Java, then explain exactly which responsibility the container takes over.

This publication targets **Java 21 and Spring Framework 7.0.8**. Spring Framework 7 retains a Java 17 baseline. Version-sensitive features are labeled; the core mental models also apply to supported Spring 6.2 applications.

## The one model to keep in your head

```text
configuration metadata
  @Configuration / @Bean / @Component / XML / programmatic registration
                           |
                           v
                  ApplicationContext
               reads bean definitions
                           |
          +----------------+----------------+
          |                |                |
          v                v                v
       creates          injects          enhances
       objects       dependencies     lifecycle/proxies
          |                |                |
          +----------------+----------------+
                           |
                           v
                    application beans
```

A **bean** is an object whose creation and lifecycle are managed by a Spring container. It is not a special Java language construct. An `OrderService` remains an ordinary Java class; becoming a bean changes who creates and wires it.

## IoC and dependency injection without slogans

Suppose `OrderService` needs an `OrderRepository`.

```java
OrderRepository repository = new JdbcOrderRepository(dataSource);
OrderService service = new OrderService(repository);
```

The application entry point controls construction. With dependency injection, `OrderService` still declares what it needs, but another component supplies it:

```java
final class OrderService {
    private final OrderRepository repository;

    OrderService(OrderRepository repository) {
        this.repository = repository;
    }
}
```

Spring can become that object-graph assembler. This is **inversion of control**: creation and connection move from business objects to a container. DI is the concrete mechanism: dependencies arrive through constructors, factory methods, or properties instead of being looked up or created inside the object.

### Why this matters

- Dependencies become visible in the class API.
- Tests can supply small fakes without starting Spring.
- Configuration can choose an implementation at an application boundary.
- Framework services such as transactions can wrap calls consistently.

DI does not automatically create good design. A class with fifteen constructor arguments is still telling you that its responsibility may be too large.

## Spring Framework versus Spring Boot

| Spring Framework | Spring Boot |
|---|---|
| Container, DI, AOP, transactions, events, MVC, validation, testing | Opinionated application setup built on Framework |
| You can construct a context explicitly | Usually creates and configures the context for you |
| Teaches the mechanisms | Adds auto-configuration, starters, executable packaging, and operations conventions |

This book does not use Boot to hide Framework mechanics. Boot is covered in **SD 05 - Spring Boot** after this volume.

## Three boundaries to label in interviews

1. **Java boundary:** constructors, interfaces, objects, threads, exceptions.
2. **Spring container boundary:** bean definitions, dependency resolution, lifecycle, proxies.
3. **External resource boundary:** database transaction, network call, message broker, filesystem.

An annotation is metadata. It has an effect only when a configured Spring component discovers it and the call crosses the required runtime boundary. For example, `@Transactional` normally needs transaction infrastructure and a call through a Spring proxy.

## The learning sequence

```text
plain Java object graph
        |
        v
first context -> bean definitions -> injection -> configuration data
        |
        v
lifecycle -> scopes -> configuration mechanics -> events/validation
        |
        v
AOP -> proxies -> transactions -> web flow -> async work
        |
        v
testing -> production diagnosis -> realistic interviews
```

Do not jump to transaction propagation before you can identify the proxy. Do not add a custom post-processor before you understand the normal lifecycle. Do not start a full application context when a pure Java unit test proves the behavior.

## The running domain

Examples use a small order application:

```text
OrderController -> OrderService -> OrderRepository -> database
                        |
                        +-> PaymentGateway (remote system)
                        +-> application event
```

The business invariant is simple: one request key creates at most one order. The design questions are not simple: which dependencies are beans, where does the transaction begin, when should an event be observed, and what happens when a remote call fails?

## A six-question runtime trace

For every Spring example, ask:

1. Which class registered the bean definition?
2. Which container owns the bean instance?
3. How was each dependency selected?
4. Did the caller receive a target or a proxy?
5. Which thread and transaction execute the method?
6. How will a test or metric prove the answer?

## Common myths corrected

- Spring is not the same as Spring Boot.
- `@Autowired` does not make a dependency globally available; the container resolves an injection point.
- Spring singleton means one instance per bean definition per container, not one object per JVM.
- A proxy cannot intercept every possible Java call.
- `@Transactional` does not create a distributed transaction across remote services.
- Constructor injection improves visibility, but it does not repair circular or oversized designs.

## Quick check

1. What makes an ordinary Java object a Spring bean?
2. How is DI a form of IoC?
3. Why can an annotation be present but ineffective?
4. What is the difference between Framework and Boot?
5. What six questions should you ask when tracing runtime behavior?

## Practice

- **Foundation:** Draw a three-object graph in plain Java and mark who constructs each object.
- **Foundation:** Rewrite a class that calls `new JdbcOrderRepository()` internally to use constructor injection.
- **Interview Core:** Explain why a bean with twelve dependencies may indicate a design problem.
- **Interview Core:** Classify configuration, proxying, and database commit by boundary.
- **SDE-2 Follow-up:** Given an ineffective annotation, describe the evidence you would collect before changing configuration.

## Readiness checkpoint

Continue when you can define bean, IoC, and DI without saying "Spring magic," and can separate a Java object, its container registration, and any proxy surrounding it.
