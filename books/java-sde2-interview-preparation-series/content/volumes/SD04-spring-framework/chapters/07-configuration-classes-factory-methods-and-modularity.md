# Configuration Classes, Factory Methods, and Modularity

Java configuration is executable code, but Spring may enhance a full `@Configuration` class so inter-bean method calls preserve container semantics. This subtle mechanism appears often in senior interviews.

## Full configuration mode

```java
@Configuration
class ApplicationConfiguration {
    @Bean
    Repository repository() {
        return new JdbcRepository();
    }

    @Bean
    OrderService orderService() {
        return new OrderService(repository());
    }
}
```

In the default full mode, Spring creates a subclass-based enhancement of the configuration class. The call to `repository()` is intercepted and returns the container-managed singleton rather than constructing an extra repository.

That behavior is not normal Java. If you manually instantiate the configuration class, normal method calls apply and new objects can be created.

## Parameter injection is clearer

```java
@Bean
OrderService orderService(Repository repository) {
    return new OrderService(repository);
}
```

This expresses the dependency directly, works in full or lite configuration, and avoids relying on inter-bean method interception. Prefer it.

## Lite configuration mode

An `@Bean` method declared in a non-`@Configuration` component, or an `@Configuration(proxyBeanMethods = false)` class, uses lite semantics. Direct Java calls between factory methods are not intercepted.

```java
@Configuration(proxyBeanMethods = false)
class FastConfiguration {
    @Bean
    Repository repository() {
        return new JdbcRepository();
    }

    @Bean
    OrderService orderService(Repository repository) {
        return new OrderService(repository);
    }
}
```

This is safe because dependencies arrive as parameters. Lite mode can reduce configuration enhancement and restrictions, but correctness comes first.

## Factory method parameters are injection points

Factory methods can receive beans, configuration values, and infrastructure types:

```java
@Bean
PaymentClient paymentClient(
        HttpTransport transport,
        PaymentClientSettings settings) {
    return new PaymentClient(transport, settings.baseUri(), settings.timeout());
}
```

Keep factory methods deterministic and quick. They should not perform unbounded retries, migrations, or production data reads.

## Configuration class constraints

Full configuration relies on subclass enhancement. Do not make full configuration classes or relevant methods final. Prefer a simple, package-visible configuration class with focused imports. If you need final classes, use `proxyBeanMethods = false` and parameter injection.

## Modular assembly

```text
RootConfiguration
  |
  +-- OrdersConfiguration
  |     +-- domain services
  |     +-- repository port adapter
  |
  +-- PaymentsConfiguration
  |     +-- client settings
  |     +-- payment adapter
  |
  +-- RuntimeConfiguration
        +-- clock / executor / transaction manager
```

Each module should expose a small public configuration surface. Keep package internals unscanned from unrelated modules. Avoid a configuration class per individual bean; modularity should reflect a coherent capability.

## Conditional registration boundary

Plain Framework offers profiles and programmable conditions. Spring Boot adds a larger conditional auto-configuration model. In core application configuration, prefer deterministic choices based on validated settings. If you implement a custom `Condition`, keep it free of side effects and produce diagnostic evidence about why it matched.

## Bean method return types

Declare the narrow service interface when consumers should not depend on an implementation. For infrastructure extension points, declare a sufficiently precise type so Spring can detect their role without instantiating them early.

```java
@Bean
static BeanFactoryPostProcessor propertyGuard() { ... }
```

## Common mistakes

- Assuming every call to an `@Bean` method is container-intercepted.
- Using `proxyBeanMethods = false` while calling another factory method directly.
- Manually constructing a full configuration class in application code.
- Marking enhanced configuration final.
- Doing remote I/O in factory methods without bounded startup semantics.
- Splitting configuration so finely that no module boundary is visible.

## Interview angle

**Interviewer:** Why might calling one `@Bean` method from another return the singleton?

**Strong answer:** In full `@Configuration` mode, Spring enhances the configuration class with a subclass that intercepts factory-method calls and routes them through the container. Lite mode does not. I normally use factory method parameters, which state dependencies directly and avoid relying on that interception.

## Quick check

1. What makes full configuration behavior different from normal Java?
2. What does `proxyBeanMethods = false` change?
3. Why are factory method parameters clearer?
4. Which configuration mode supports final configuration classes more naturally?
5. What makes a useful configuration module?

## Predict and debug

**Predict:** In lite mode, `return new OrderService(repository())` invokes ordinary Java and can create a repository not owned as the registered singleton.

**Debug:** Two connection pools exist despite one pool bean definition. Search for direct factory-method calls, manual `new Configuration()`, and adapters constructed outside the context; switch to parameter injection.

## Practice

- **Foundation:** Rewrite an inter-bean call as a method parameter.
- **Foundation:** Explain full and lite mode in one diagram.
- **Interview Core:** Split a configuration into orders, payments, and runtime modules.
- **Interview Core:** Write a test proving one repository instance reaches two services.
- **SDE-2 Follow-up:** Diagnose duplicate expensive clients after a `proxyBeanMethods = false` optimization.

## Readiness checkpoint

Continue when you can predict an `@Bean` call in full configuration, lite configuration, and a manually constructed configuration object.
