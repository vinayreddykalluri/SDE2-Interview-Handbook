# Bean Definitions, Java Configuration, and Component Scanning

The container works from **bean definitions**: recipes describing how an object is created, named, scoped, initialized, and connected. An annotation is one way to contribute a recipe; it is not the bean itself.

## Three registration styles

### Explicit Java configuration

```java
@Configuration
class OrderConfiguration {
    @Bean
    Clock clock() {
        return Clock.systemUTC();
    }

    @Bean
    OrderService orderService(OrderRepository repository, Clock clock) {
        return new OrderService(repository, clock);
    }
}
```

Advantages: construction is visible, third-party classes are easy to register, and a configuration class forms a reviewable module boundary.

### Component scanning

```java
@Repository
final class JdbcOrderRepository implements OrderRepository {
    // ...
}

@Service
final class OrderService {
    private final OrderRepository repository;

    OrderService(OrderRepository repository) {
        this.repository = repository;
    }
}

@Configuration
@ComponentScan(basePackageClasses = OrderService.class)
class ApplicationConfiguration {
}
```

`@Component`, `@Service`, `@Repository`, and `@Controller` are stereotype annotations. The specialized forms communicate architectural role. `@Repository` also participates in persistence-exception translation when the relevant infrastructure is configured.

### XML or programmatic registration

Legacy and integration-heavy systems may use XML. Framework libraries may register definitions through lower-level APIs. SDE-2 engineers should be able to read XML, but new examples in this book prefer explicit Java configuration and focused scanning.

## Scanning is a search boundary

Avoid a scan rooted at a broad company package merely to make startup pass. It can register tests, duplicate adapters, or internal configuration unintentionally.

```java
@ComponentScan(basePackages = "com.example") // very broad
```

Prefer a marker type or a narrow package:

```java
@ComponentScan(basePackageClasses = OrdersModule.class)
```

This survives package renames better than a string and states the intended module.

## Bean names and aliases

An `@Bean` method defaults to its method name. A scanned component defaults to a decapitalized class-based name unless explicitly named.

```java
@Bean("primaryClock")
Clock applicationClock() {
    return Clock.systemUTC();
}
```

Type-based injection should not depend on incidental names. Use explicit names or qualifiers when multiple semantic implementations exist.

## `@Import` creates visible modules

```java
@Configuration
@Import({OrderConfiguration.class, MessagingConfiguration.class})
class RootConfiguration {
}
```

`@Import` is often easier to reason about than a large scan. A useful rule is: use scanning for stable application components inside a bounded package; use explicit imports and `@Bean` methods at infrastructure and module boundaries.

## Third-party objects

You cannot annotate a class you do not own, but you can still manage it:

```java
@Bean
PaymentClient paymentClient(PaymentClientSettings settings) {
    return new PaymentClient(settings.baseUrl(), settings.timeout());
}
```

The factory method is also the right place to translate validated configuration into a library-specific object.

## Definition versus instance

```text
BeanDefinition "orderService"
  class/factory: OrderConfiguration.orderService(...)
  scope: singleton
  lazy: false
  dependencies: OrderRepository, Clock
                  |
                  v refresh
OrderService instance (possibly wrapped by a proxy)
```

Definition-level tools run before most application beans exist. Instance-level tools run around construction and initialization. Confusing these stages causes many extension-point bugs.

## Duplicate registration and overriding

If two configurations define the same name, behavior depends on the context and override policy. Do not rely on whichever definition happens to win. Give semantically distinct beans distinct names or eliminate the duplicate. A production graph should be deterministic under package ordering changes.

## Configuration design example

Weak:

```java
@Configuration
@ComponentScan("com.company")
class EverythingConfiguration {
}
```

Stronger:

```java
@Configuration
@Import({OrdersConfiguration.class, PaymentsConfiguration.class})
class BackendConfiguration {
}

@Configuration
@ComponentScan(basePackageClasses = OrdersModule.class)
class OrdersConfiguration {
    @Bean
    Clock orderClock() {
        return Clock.systemUTC();
    }
}
```

The stronger design exposes application modules and keeps infrastructure decisions reviewable.

## Common mistakes

- Believing `@Service` changes Java call semantics by itself.
- Scanning the default package or the whole classpath.
- Registering one component both through scanning and an `@Bean` method.
- Using bean names as hidden business routing.
- Placing environment-specific branching throughout business configuration.
- Assuming imported classes move into the current Java package.

## Interview angle

**Interviewer:** Component scanning or explicit `@Bean` methods?

**Strong answer:** Both produce bean definitions. I use focused scanning for application-owned components with stable stereotypes and explicit configuration for third-party clients, environment-bound objects, and module assembly. I avoid broad scans because they hide registration and increase startup surprises. The decision is about graph visibility and ownership, not annotation preference.

## Quick check

1. What information belongs in a bean definition?
2. How does an `@Bean` name default?
3. Why can a marker class make scanning safer?
4. When is `@Bean` preferable to `@Component`?
5. What is the difference between definition-level and instance-level processing?

## Predict and debug

**Predict:** If `OrderService` is annotated but its package is not scanned or imported, is it a bean? No.

**Debug:** Startup finds two `PaymentGateway` beans. Do not widen scanning further. Name the intended semantics, use a qualifier or primary only when one default is real, and make the configuration boundary explicit.

## Practice

- **Foundation:** Register `Clock`, `TaxPolicy`, and `CheckoutService` with `@Bean`.
- **Foundation:** Convert only application-owned services to scanned components.
- **Interview Core:** Draw definitions and instances for a three-bean context.
- **Interview Core:** Find the duplicate-registration risk in a scan-plus-import configuration.
- **SDE-2 Follow-up:** Split a 40-bean configuration into reviewable modules without creating child contexts.

## Readiness checkpoint

Continue when you can locate the exact registration path for every bean and defend the scan or explicit configuration boundary.
