# Learning Path and Spring Boot First Principles

Spring Boot turns a Spring application into a runnable, configurable, observable service with fewer explicit setup decisions. It does this by contributing configuration based on the classpath, existing beans, properties, and application type. It does not replace Spring Framework, remove the need for architecture, or make production behavior automatic.

This publication targets **Java 21 and Spring Boot 4.1.0**. Spring Boot 4.1 requires at least Java 17 and Spring Framework 7.0.8. Version-sensitive features are labeled, while the core mental models also apply to maintained Spring Boot 3.5 and 4.0 services.

## The one model to keep in your head

```text
build dependencies + application code + external configuration
                         |
                         v
                  SpringApplication
           prepares Environment and context
                         |
                         v
              user configuration first
                         |
                         v
       conditional auto-configuration fills gaps
                         |
                         v
        runnable application + operational signals
```

Boot is an **application assembly layer**. Spring Framework still owns the container, dependency injection, proxying, transactions, MVC, validation, and events. Boot supplies conventions, dependency management, auto-configurations, executable packaging, configuration loading, Actuator integration, and test support.

## Start from plain Spring

Without Boot, a servlet application may explicitly register MVC infrastructure, configure a server, create a JSON mapper, establish property sources, and package for deployment. Boot observes that the application is servlet-based and that relevant libraries are present, then registers sensible defaults if the application has not supplied replacements.

That sentence contains four interview-critical ideas:

1. **Classpath:** dependencies are inputs to behavior.
2. **Conditions:** auto-configuration is selected, not unconditional.
3. **Back-off:** user beans can replace specific defaults.
4. **Evidence:** the condition evaluation report explains the decision.

## Boot versus Framework versus platform

| Boundary | Responsibility |
|---|---|
| Java | objects, threads, exceptions, memory, language rules |
| Spring Framework | container, AOP, transactions, MVC, validation |
| Spring Boot | application startup, conditional assembly, configuration conventions, packaging, operations |
| Deployment platform | processes, containers, networking, secrets, probes, scaling |

If a Kubernetes pod restarts, Boot did not scale the service. If a method transaction rolls back, Boot did not invent transaction semantics. If an actuator endpoint is exposed publicly, the deployment and security configuration still own that risk.

## The running service

Examples use an order API:

```text
HTTP client
    |
    v
OrderController -> OrderService -> OrderRepository -> database
                         |
                         +-> PaymentClient -> remote provider
                         +-> metrics and logs
```

The core invariant is: one idempotency key creates at most one order. The service must also start deterministically, reject invalid configuration, expose safe health signals, shut down without dropping in-flight requests, and produce evidence when it fails.

## The learning sequence

```text
first application -> build and startup -> package structure
        |
        v
auto-configuration -> external configuration -> typed properties
        |
        v
REST boundaries -> outbound calls -> data and migrations
        |
        v
Actuator -> observability -> availability and shutdown
        |
        v
testing -> packaging -> performance/upgrades -> incidents/interviews
```

Do not memorize annotations before understanding what they import. Do not discuss native images before you can diagnose a failed context. Do not add every Actuator endpoint to HTTP exposure before deciding who may read it.

## A seven-question Boot trace

For every feature, ask:

1. Which dependency put the feature on the classpath?
2. Which user configuration registered first?
3. Which auto-configuration condition matched?
4. Which condition caused another candidate to back off?
5. Which external value won, and from which property source?
6. Which runtime boundary handles the request, thread, and failure?
7. Which test, endpoint, log, metric, or trace proves the answer?

## Common myths corrected

- Boot is not a code generator and does not replace Spring Framework.
- A starter is a dependency descriptor, not a runtime container.
- Auto-configuration is conditional and can be inspected.
- `application.yml` is not always the highest-precedence configuration source.
- A healthy process is not necessarily ready for traffic.
- `@SpringBootTest` is not the correct default for every test.
- An executable jar is not automatically a production-safe container image.

## Quick check

1. What responsibilities does Boot add to Framework?
2. What four inputs determine common auto-configuration decisions?
3. Why does a user bean make an auto-configuration back off?
4. How are liveness and readiness different?
5. What evidence should replace guessing about startup?

## Practice

- **Foundation:** Draw the four runtime boundaries for a small REST service.
- **Foundation:** Explain starter, auto-configuration, and embedded server in separate sentences.
- **Interview Core:** Trace how adding a JDBC driver can change startup behavior.
- **Interview Core:** List three reasons a property in `application.yml` may not win.
- **SDE-2 Follow-up:** Define the evidence needed to approve a new Boot starter in a production service.

## Readiness checkpoint

Continue when you can describe Boot as conditional application assembly, not magic, and can separate Spring behavior from deployment-platform behavior.
