# Async Execution, Scheduling, and Context Boundaries

Asynchronous execution moves work to an executor. Scheduling decides when work becomes eligible to run. Neither feature supplies durability, capacity, ordering, or context propagation automatically.

## Enable only what you use

```java
@Configuration
@EnableAsync
@EnableScheduling
class ExecutionConfiguration {
    @Bean("emailExecutor")
    Executor emailExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(4);
        executor.setMaxPoolSize(8);
        executor.setQueueCapacity(200);
        executor.setThreadNamePrefix("email-");
        executor.initialize();
        return executor;
    }
}
```

The numbers are examples, not recommendations. Size concurrency from downstream capacity, service time, memory per task, and latency target. An unbounded queue converts overload into growing latency and memory pressure.

## `@Async` call flow

```java
@Service
final class EmailService {
    @Async("emailExecutor")
    public CompletableFuture<DeliveryResult> send(EmailCommand command) {
        return CompletableFuture.completedFuture(deliver(command));
    }
}
```

```text
caller -> async proxy -> submit task -> caller receives Future
                         |
                         v
                    executor thread -> target method
```

The default annotation mode is proxy-based, so self-invocation bypasses `@Async`. The returned future represents task completion, not durable acceptance. Process crash can lose queued memory tasks.

## Failure semantics

For `Future`/`CompletableFuture`, failures complete the future exceptionally and must be observed. For `void` async methods, exceptions cannot return to the caller; configure an `AsyncUncaughtExceptionHandler` and telemetry. Prefer a result-bearing type or durable queue for important work.

## Thread-local and transaction boundaries

Imperative transaction state and many request/security/logging contexts are thread-bound. Moving to an executor does not copy them automatically.

```text
request thread: transaction + request context
        |
        +-- submit immutable command
                 |
                 v
executor thread: new context; start its own transaction if needed
```

Capture stable IDs and immutable data, not an open `EntityManager`, request-scoped bean, mutable entity, or thread-local assumption. If context propagation is needed, use a deliberate supported mechanism and test cleanup to prevent one task's data from leaking into another.

## `@Scheduled`

```java
@Scheduled(fixedDelay = 30, timeUnit = TimeUnit.SECONDS)
public void relayOutbox() {
    // claim a bounded batch and process idempotently
}
```

- **fixed delay:** next run is measured after prior completion.
- **fixed rate:** attempts to maintain a rate measured from start times; overlap/backlog depends on scheduler configuration.
- **cron:** calendar schedule; specify timezone when business meaning depends on it.

Scheduled methods are commonly no-argument operations. A schedule is not a cluster-wide singleton. Ten service replicas can each run it unless coordination, sharding, or leader/lease ownership is designed.

## Durable scheduled work

A reliable job needs:

- bounded claim size and stable ordering;
- database lease/claim with expiration or an external scheduler;
- idempotent processing;
- checkpoint and restart behavior;
- poison-item policy;
- concurrency limit and backpressure;
- lag, duration, outcome, and oldest-item metrics.

Do not keep one transaction around an entire million-row job. Claim/process in bounded units and define whether each item or batch is atomic.

## Cancellation and shutdown

Interruption is cooperative. Code must honor cancellation when safe. Configure graceful executor shutdown and a bounded wait, but also design queued work to survive or be safely retried after process termination if it matters.

## Common mistakes

- Assuming `@Async` means durable messaging.
- Using an unbounded executor queue.
- Ignoring exceptional future completion.
- Sharing a transaction or persistence context across threads.
- Assuming one scheduled invocation across a cluster.
- Scheduling long work without leases, idempotency, or backlog metrics.

## Interview angle

**Interviewer:** What breaks when you add `@Async` to a transactional method call?

**Strong answer:** The call must first cross the async proxy. Work then runs on another thread, so the caller's imperative transaction and thread-local request context do not propagate automatically. The async method can start its own transaction, but that transaction is independent. I pass an immutable command/ID, define failure observation and executor capacity, and use a durable queue if losing in-memory work is unacceptable.

## Quick check

1. Why can self-invocation bypass `@Async`?
2. How are async exceptions observed?
3. Does transaction state follow an executor task?
4. What is fixed delay measured from?
5. Why is `@Scheduled` not cluster coordination?

## Predict and debug

**Predict:** A scheduled method runs in each of six replicas. Without coordination, the logical job executes six times.

**Debug:** Async email backlog grows while CPU is idle. Measure queue depth/age, downstream latency/rate limits, active threads, rejection, and connection pools; increasing threads may only overload the provider.

## Practice

- **Foundation:** Configure a named bounded executor and return a `CompletableFuture`.
- **Foundation:** Contrast fixed delay, fixed rate, and cron.
- **Interview Core:** Refactor an async method to accept an immutable ID rather than an entity.
- **Interview Core:** Design exceptional completion and safe telemetry.
- **SDE-2 Follow-up:** Build a multi-replica outbox scheduler using leases, idempotency, and backpressure.

## Readiness checkpoint

Continue when you can state executor, queue, thread, transaction, context, durability, cancellation, and overload behavior for each async or scheduled task.
