# Practice, Reasoned Solutions, and Readiness

Attempt the exercises before reading the solutions. Each solution explains the decision boundary; production details still depend on the selected provider and deployment platform.

## Exercises

### 1. Chain selection — Foundation

Three chains are declared in this order:

```text
A: /**             -> permitAll
B: /api/**         -> authenticated
C: /api/admin/**   -> has ADMIN
```

Predict the policy for `GET /api/admin/users`. Repair the design.

### 2. Resource authorization — Interview Core

A caller has `order:refund` but belongs to tenant B. The order belongs to tenant A. Design the two authorization checks and state where each belongs.

### 3. Timeout arithmetic — Interview Core

An endpoint has a 1,000 ms deadline. Local work uses 120 ms and response reserve is 130 ms. Payment policy proposes three attempts with a 400 ms timeout and 50/100 ms backoff. Is it valid? Propose a bounded alternative.

### 4. Reactive diagnosis — Interview Core

This handler stalls under concurrency:

```java
Mono<Order> handle(String id) {
    return Mono.just(jdbcRepository.findById(id).orElseThrow());
}
```

Explain when the JDBC call runs and repair or replace the design.

### 5. Batch restart — SDE-2 Follow-up

A chunk writes 100 database rows, calls a partner once per row, and then commits batch metadata. The process dies after 70 partner calls. Design a safe restart strategy.

### 6. Integration error — Interview Core

An executor channel accepts a message; a later transformer throws. The HTTP caller already received 202. Define the error contract and evidence.

### 7. Observability labels — Foundation

Choose which values may be metric labels: HTTP method, normalized route, status family, order ID, raw exception message, dependency name, tenant ID.

### 8. Module selection — SDE-2 Follow-up

Choose the smallest approach for:

1. a 5,000-row nightly restartable import;
2. a blocking CRUD API with 40 requests/second;
3. a route that proxies 50,000 simultaneous slow streaming connections;
4. an SFTP-to-HTTP bridge with validation and dead-letter handling.

## Solutions

### Solution 1

The first matching chain wins, so A permits the admin request. Put narrow matchers before broad ones and avoid overlapping catch-all policies where possible:

```text
C: /api/admin/** -> ADMIN
B: /api/**       -> authenticated
A: explicitly public paths only -> permitAll
```

Test allowed and denied cases for each boundary; do not rely only on bean order inspection.

### Solution 2

The request/method boundary may check the coarse `order:refund` authority. The application policy must then load the order and require `actor.tenantId == order.tenantId`, plus state/amount rules. A gateway cannot make the resource decision because it does not own authoritative order state. Test the policy in plain Java and one actual method/filter integration path.

### Solution 3

The dependency budget is `1,000 - 120 - 130 = 750 ms`. Three 400 ms attempts plus 150 ms of backoff cannot fit. A valid policy might permit one 450 ms attempt and one retry capped by the remaining deadline, with 50 ms jittered backoff and reserve. Exact values need latency evidence. The operation also needs idempotency if it has side effects.

### Solution 4

Java evaluates method arguments before calling `Mono.just`, so JDBC executes immediately on the caller thread. The code is not lazy or non-blocking. The clearest option is MVC for a blocking persistence stack. As a bounded migration bridge:

```java
return Mono.fromCallable(() -> jdbcRepository.findById(id).orElseThrow())
        .subscribeOn(Schedulers.boundedElastic());
```

This moves blocking work; it does not create more database connections or make JDBC asynchronous. Measure scheduler queue and database pool limits.

### Solution 5

Database rows can share the chunk transaction only if the repository and metadata are correctly coordinated, but partner calls cannot. Give each logical row a stable operation key. Prefer committing an outbox/staging record with database state and let a separate idempotent dispatcher call the partner. On restart, already committed keys return the original result; pending keys resume. Reconcile partner responses with local status. Chunk size alone cannot close the crash window.

### Solution 6

HTTP 202 must mean accepted for asynchronous processing, not completed. Persist or durably enqueue before returning if loss is unacceptable. Route transform failures to a correlated error/dead-letter store with retry classification, attempt cap, and alert. Measure accepted, completed, failed, retried, queue age, and rejections. An in-memory executor queue alone cannot survive process loss.

### Solution 7

Good bounded labels: HTTP method, normalized route, status family, and a bounded dependency name. Order ID, raw exception text, and tenant ID can have unbounded cardinality and may be sensitive; put them in protected logs or sampled traces. If tenants are a tiny contractual set, a reviewed tenant tier—not raw tenant ID—may be a metric dimension.

### Solution 8

1. Spring Batch if restart/checkpoint evidence matters; a simple scheduled transaction may suffice if all rows are one small atomic unit.
2. MVC with the blocking stack; WebFlux adds little.
3. WebFlux is a candidate if the entire I/O path is non-blocking and demand/cancellation are modeled.
4. Spring Integration is a candidate for adapters, routing, error flow, and dead-letter semantics; Batch may own processing if each file is a restartable finite job.

## Final readiness assessment

You are ready to use these modules at SDE-2 level when you can:

- trace a servlet security request through the selected filter chain;
- separate authentication, coarse authorization, and resource policy;
- defend CSRF/CORS choices from the credential model;
- allocate one end-to-end deadline and keep retries idempotent;
- explain demand, subscription, scheduler, blocking, error, and cancellation behavior;
- distinguish job instance, execution, checkpoint, and idempotency;
- distinguish direct/pollable/executor channel failure semantics;
- design low-cardinality telemetry and useful probes;
- choose a smaller design when an extension adds no useful boundary.

## Official sources and version checks

- [Spring Security servlet architecture](https://docs.spring.io/spring-security/reference/7.0/servlet/architecture.html)
- [Spring Security authorization](https://docs.spring.io/spring-security/reference/servlet/authorization/index.html)
- [Spring Cloud release-train compatibility](https://spring.io/projects/spring-cloud/)
- [Spring Framework WebFlux reference](https://docs.spring.io/spring-framework/reference/web/webflux.html)
- [Project Reactor reference](https://projectreactor.io/docs/core/release/reference/)
- [Spring Batch reference](https://docs.spring.io/spring-batch/reference/index.html)
- [Spring Integration reference](https://docs.spring.io/spring-integration/reference/index.html)
- [Spring Boot Actuator reference](https://docs.spring.io/spring-boot/reference/actuator/index.html)

Before copying an API, confirm the exact reference documentation for the Boot-managed versions in the project. Concepts in this book are stable; names and defaults can move at major-version boundaries.
