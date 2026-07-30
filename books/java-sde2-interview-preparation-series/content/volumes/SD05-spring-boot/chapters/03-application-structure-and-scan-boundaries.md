# Application Structure and Scan Boundaries

Package structure is executable configuration in a Boot application. It affects component scanning, default entity/repository discovery, test configuration search, and accidental coupling.

## A feature-oriented structure

```text
com.example.orders
  OrderApplication.java
  order/
    api/
    application/
    domain/
    persistence/
  payment/
    application/
    client/
  shared/
    configuration/
```

Feature-oriented packages keep one workflow discoverable. Layers can still exist inside each feature. A global `controller/service/repository` split often scatters a change across the repository and encourages dependencies between unrelated features.

## What `@SpringBootApplication` combines

Conceptually:

```java
@SpringBootConfiguration
@EnableAutoConfiguration
@ComponentScan
public @interface SpringBootApplication { }
```

`@SpringBootConfiguration` identifies primary Boot configuration. `@EnableAutoConfiguration` imports candidates conditionally. `@ComponentScan` finds application components from the annotated class's package by default.

## Keep the root narrow

```java
package com.example.orders;

@SpringBootApplication
public class OrderApplication { }
```

Do not put the class in `com.example`; that can scan every company library beneath the package. Do not put it in `com.example.orders.bootstrap` unless component scanning is configured deliberately.

Use explicit imports at module boundaries:

```java
@Configuration(proxyBeanMethods = false)
@Import({OrderModuleConfiguration.class, PaymentClientConfiguration.class})
class ApplicationModules { }
```

This makes ownership reviewable and reduces accidental registration.

## Configuration classes should stay thin

Configuration describes assembly:

```java
@Configuration(proxyBeanMethods = false)
class PaymentClientConfiguration {
    @Bean
    PaymentClient paymentClient(PaymentProperties properties,
                                RestClient.Builder builder) {
        return new PaymentClient(builder.baseUrl(properties.baseUrl()).build());
    }
}
```

Business decisions belong in application/domain classes. A large configuration class with branching business logic becomes difficult to test and reuse.

## Default package and unnamed package

Never use Java's unnamed/default package. Boot cannot establish a safe scan boundary and may inspect every class from every jar. Explicit packages also make tests and modularization predictable.

## Architecture checks

A mature service can test dependency direction with architecture tests or simple package rules:

```text
api -> application -> domain
               |
               v
       ports/interfaces
               ^
               |
     persistence/client adapters
```

Boot is used at the outer composition boundary. Domain objects should not require a running Boot application to enforce invariants.

## Common mistakes

- Broad `scanBasePackages` strings that silently grow.
- A second `@SpringBootApplication` in test sources that changes discovery.
- Component-scanning third-party library packages.
- Placing entities or repositories outside expected roots without explicit configuration.
- Using package-private framework components across unrelated packages accidentally.

## Interview angle

**Interviewer:** A component exists but is not injected. What do you verify?

**Strong answer:** I confirm it is registered, not merely annotated; check the primary configuration package and explicit scans/imports; verify profiles and conditions; look for duplicate test configuration; then inspect candidate type and qualifiers. I prefer correcting the module boundary over widening the scan to the company root.

## Quick check

1. Which three concerns are combined by `@SpringBootApplication`?
2. Why is the application package a runtime boundary?
3. Why prefer feature-oriented packages?
4. When is explicit `@Import` clearer than scanning?
5. Why keep the domain independent of Boot?

## Practice

- **Foundation:** Reorganize a controller/service/repository sample by feature.
- **Foundation:** Predict which packages a root application class scans.
- **Interview Core:** Diagnose a test that finds a different primary configuration.
- **SDE-2 Follow-up:** Define module dependency rules for orders, payments, and notifications.
