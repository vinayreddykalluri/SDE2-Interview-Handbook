# Chapter 36: Executors, Futures, CompletableFuture, and Work Scheduling

## Learning objectives

- Separate task submission, execution policy, completion, and shutdown.
- Configure thread pools with intentional sizing, bounded queues, and saturation behavior.
- Use `Future` timeouts and cancellation without confusing them with task termination.
- Compose `CompletableFuture` stages and handle failures precisely.
- Avoid common-pool blocking, unbounded admission, hidden exceptions, and orphaned work.

## Why this matters at SDE-2

Executors turn a concurrency problem into a capacity system. An unbounded queue can preserve acceptance while latency and memory grow until failure. An oversized blocking pool can overwhelm a database. A `Future.get` timeout can return while the task continues consuming resources. A `CompletableFuture` callback can run on a request thread, pool worker, or common pool depending on completion timing and method choice.

An SDE-2 must design ownership and overload behavior, not just call `submit`. Interviews expect differences among `execute`, `submit`, `thenApply`, `thenCompose`, `handle`, and `exceptionally`. Production expects queue depth, rejection, task age, and shutdown behavior to be observable.

## First-principles model

An executor separates **what** should run from **where and when** it runs:

```text
producer -> admission -> work queue -> workers -> completion
                |                         |
             reject/backpressure       result/failure
```

Capacity is finite even when a data structure is unbounded. If arrival rate exceeds service rate for long enough, queued work, latency, and memory grow. A correct policy bounds one or more of: accepted concurrency, queue length, wait time, work per request, or upstream arrival.

A completion object represents a state machine:

```text
incomplete -> completed normally(value)
           -> completed exceptionally(cause)
           -> cancelled(cancellation outcome)
```

Composition transforms state without blocking a worker merely to wait for another stage.

## Core terminology

- **Executor:** Interface accepting tasks for execution.
- **ExecutorService:** Executor with lifecycle, submission, futures, and shutdown operations.
- **ThreadPoolExecutor:** Configurable platform-thread pool with core/max sizes, queue, factory, and rejection policy.
- **Future:** Handle for completion, result retrieval, cancellation request, and status.
- **Saturation policy:** Behavior when an executor cannot accept more work.
- **Backpressure:** Propagating limited capacity to producers rather than buffering without bound.
- **Work stealing:** Workers take tasks from other workers' queues to balance computation.
- **Completion stage:** Dependent asynchronous computation triggered by another completion.
- **Fan-out/fan-in:** Start multiple tasks and combine their completions.
- **Deadline:** Absolute remaining-time budget propagated across operations.

## Detailed mechanics

`execute(Runnable)` submits work with no result handle. If the task throws, uncaught-exception behavior may expose it through the worker/thread mechanism. `submit` returns a `Future`; thrown exceptions are captured and later wrapped by `Future.get` in `ExecutionException`. Ignoring the returned future can hide failure. This distinction often explains why changing `execute` to `submit` made logs disappear.

`ThreadPoolExecutor` admission is frequently misunderstood. In the typical policy:

1. If fewer than core threads exist, create one for the task.
2. Otherwise offer to the work queue.
3. If queue offer fails and thread count is below maximum, add a worker.
4. Otherwise reject.

With an unbounded queue, step 2 nearly always succeeds, so `maximumPoolSize` may never affect load handling. A bounded `ArrayBlockingQueue` makes overload explicit. Rejection choices include aborting with `RejectedExecutionException`, running in the caller, discarding, or discarding oldest. Silent discard is dangerous unless loss is deliberate and measured. `CallerRunsPolicy` can slow producers but may also run expensive work on an event loop or latency-sensitive request thread, so it is not universally safe.

Pool sizing begins with workload, not a magic number. For CPU-bound independent work, a starting point near available processors avoids excessive runnable threads. For blocking work, the rough formula `threads = cores / (1 - blockingFraction)` can guide an experiment, but external capacity and memory usually impose stricter limits. A database with 40 connections cannot benefit from 400 concurrent queries. Measure service time, blocking fraction, utilization, queue delay, downstream limits, and tail latency.

A `Future` result becomes available through `get`. Actions in the asynchronous computation happen-before actions after another thread successfully retrieves the result through `Future.get`, per the API memory-consistency contract. `get(timeout)` bounds how long the caller waits; `TimeoutException` does not cancel the task. `cancel(true)` requests interruption if running, but success means the Future transitioned to cancelled, not proof that arbitrary target code stopped. `cancel(false)` prevents execution when possible or records cancellation without interruption depending on state.

Correct executor shutdown is two-phase. `shutdown()` rejects new tasks and allows accepted work to finish. Await for a bounded period. If time expires, `shutdownNow()` requests interruption and returns tasks that never commenced. Await again and report remaining nontermination. If the shutdown thread is interrupted, request immediate shutdown, restore status, and return/propagate according to the boundary.

Scheduled executors distinguish fixed rate from fixed delay. Fixed-rate scheduling targets a cadence relative to the initial schedule and can run successive executions close together when behind, but does not concurrently overlap executions of the same periodic task through that scheduling contract. Fixed delay waits a delay after one run completes. If a periodic task throws an uncaught exception, subsequent executions are suppressed. Wrap/report failures deliberately.

`CompletableFuture` combines a writable completion with `CompletionStage` transformations:

- `thenApply`: map one result synchronously when the stage completes.
- `thenCompose`: map to another stage and flatten it, avoiding nested futures.
- `thenCombine`: combine two independent successful results.
- `allOf`: complete when all supplied stages complete; it does not collect typed results.
- `exceptionally`: recover from failure with a value.
- `handle`: transform both success and failure into a result.
- `whenComplete`: observe success/failure while generally preserving the original outcome.

Non-`Async` dependent actions can run in the thread that completes the previous stage or the thread attaching the action if already complete. `Async` variants without an executor use the default asynchronous facility, commonly the common `ForkJoinPool`. Supply an explicit executor for isolation, capacity, naming, and blocking behavior.

`thenApply` is wrong when the function returns a future: it creates `CompletableFuture<CompletableFuture<T>>`. Use `thenCompose`. A dependent success stage is skipped if its prerequisite fails. `join()` throws unchecked `CompletionException`; `get()` uses checked `ExecutionException`/`InterruptedException`. Unwrap deliberately without discarding context.

`CompletableFuture.cancel` completes the future exceptionally with `CancellationException`; the `mayInterruptIfRunning` parameter has no interrupt effect for its asynchronous processing according to this class's contract. Cancellation must be designed into the underlying task/resource, not assumed from the graph handle.

> **Specification boundary:** `java.util.concurrent` specifies lifecycle, completion, cancellation requests, and memory-consistency effects. It does not guarantee task start order unless an executor says so, fair worker scheduling, task termination after cancel, or one thread per task.

## Worked Java example

```java
import java.time.Duration;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

public final class PricePipeline implements AutoCloseable {
    private final ThreadPoolExecutor io = new ThreadPoolExecutor(
            8, 8, 0, TimeUnit.MILLISECONDS,
            new ArrayBlockingQueue<>(100),
            new ThreadPoolExecutor.AbortPolicy());

    CompletableFuture<Integer> basePrice(String sku) {
        return CompletableFuture.supplyAsync(() -> loadPrice(sku), io);
    }

    CompletableFuture<Integer> discount(int base) {
        return CompletableFuture.supplyAsync(() -> loadDiscount(base), io);
    }

    CompletableFuture<Integer> finalPrice(String sku) {
        return basePrice(sku)
                .thenCompose(this::discount)
                .thenApply(value -> Math.max(0, value))
                .orTimeout(500, TimeUnit.MILLISECONDS)
                .exceptionally(failure -> 0);
    }

    private int loadPrice(String sku) { return 100; }
    private int loadDiscount(int base) { return base - 10; }

    @Override
    public void close() {
        shutdown(io, Duration.ofSeconds(5));
    }

    static void shutdown(ThreadPoolExecutor executor, Duration timeout) {
        executor.shutdown();
        try {
            if (!executor.awaitTermination(timeout.toMillis(), TimeUnit.MILLISECONDS)) {
                executor.shutdownNow();
                if (!executor.awaitTermination(timeout.toMillis(), TimeUnit.MILLISECONDS)) {
                    System.err.println("executor did not terminate");
                }
            }
        } catch (InterruptedException interrupted) {
            executor.shutdownNow();
            Thread.currentThread().interrupt();
        }
    }
}
```

The bounded queue makes overload observable through rejection. `thenCompose` starts the dependent asynchronous discount only after obtaining the base. The fallback of zero is intentionally visible but would be a dangerous business default unless the domain defines it; production code should classify timeout, rejection, and data failures separately.

## Execution or memory walkthrough

Successful path:

1. Caller invokes `finalPrice("A")`. `basePrice` submits a task and returns an incomplete future F1.
2. The task enters a worker or bounded queue. If both worker capacity and queue are unavailable, submission throws rejection before a usable pipeline is returned unless handled at that boundary.
3. A worker loads 100 and completes F1. Its completion triggers the `thenCompose` function, which calls `discount(100)` and returns F2.
4. `thenCompose` links rather than nests F2. Another IO task computes 90 and completes F2.
5. `thenApply` maps 90 to 90. The timeout stage races normal completion against its timer semantics; normal completion wins here.
6. `exceptionally` sees no failure and passes the value. The returned future completes with 90.

Failure path: if `loadDiscount` throws, F2 completes exceptionally. `thenApply` is skipped. `orTimeout` does not replace an already completed failure. `exceptionally` receives a completion-wrapped cause and returns 0, converting the final stage to normal success. Metrics and logs must occur before or inside recovery if the failure must remain observable.

Timeout path: the pipeline future completes exceptionally after the configured duration, but underlying I/O may continue. `orTimeout` is not resource cancellation. The I/O client also needs connect/read/request deadlines, and cancellation hooks where supported.

## Complexity and performance

Submission is generally O(1), while queueing delay is unbounded in time unless capacity and deadlines constrain it. A queue of capacity `q` uses O(q) references plus retained task graphs. Each task can retain request payloads, traces, and closures, so a "small" queue can hold significant heap.

Little's Law relates average concurrency `L`, arrival rate `lambda`, and average time `W`: `L = lambda * W` in a stable system. If 500 requests/second each occupy an IO operation for 200 ms, average in-flight demand is about 100 before bursts. Downstream limits and desired utilization determine admission below saturation.

Blocking on one future from a task in the same small executor can create thread starvation deadlock: every worker waits for subtasks queued behind them. Compose asynchronously or use separate capacity domains. Work-stealing pools excel at many nonblocking fork/join tasks; unmanaged blocking can reduce parallelism and starve unrelated common-pool users.

## Edge cases and common mistakes

- Using an unbounded queue and assuming `maximumPoolSize` controls overload.
- Creating an executor per request or forgetting to close an owned executor.
- Ignoring a `Future` returned by `submit`, hiding task exceptions.
- Treating `get(timeout)` as cancellation.
- Assuming `cancel(true)` proves target termination.
- Blocking common-pool workers on slow I/O.
- Using non-`Async` completion actions while assuming a dedicated thread.
- Using `thenApply` for a future-returning function and creating nested futures.
- Recovering every failure to a default and losing incident visibility.
- Scheduling a periodic task whose first exception silently suppresses future runs.
- Letting a queue hold work past caller deadlines.
- Using `CallerRunsPolicy` on an event loop or lock-holding producer without analysis.

## Production engineering notes

Treat each executor as a named resource pool with an owner and SLO. Export active workers, pool size, queue size/capacity, oldest task age if feasible, completed count, rejection count, execution duration, queue delay, cancellation, and shutdown duration. Thread names should identify the capacity domain.

Separate pools only when they represent different blocking behavior, priorities, or failure domains; too many pools make total concurrency uncontrollable. The strongest limit is often a semaphore or client pool aligned with a downstream resource, regardless of executor size.

Incident example: heap rises while CPU and database usage remain moderate. The service uses `newFixedThreadPool(32)`, whose factory commonly uses an unbounded queue. A dependency slows, incoming requests continue submitting closures that retain 200 KB payloads, and queue depth reaches 50,000. Fix with a bounded queue, rejection/backpressure, end-to-end deadlines, limited payload retention, and upstream shedding. Increasing heap only delays failure.

Propagate deadlines, not independent full timeouts at every layer. If an incoming request has 800 ms remaining, a downstream stage should not start a new 2-second timeout. When a stage times out, cancel or close the underlying client operation if its API supports it, and ensure late completion cannot commit unwanted side effects.

> **HotSpot note:** The common asynchronous facility normally uses `ForkJoinPool.commonPool()` in mainstream JDKs, with behavior influenced by system properties and environment. Worker implementation, work-stealing queues, compensation for managed blocking, and timer mechanics are library/runtime details, not language guarantees.

## Interview questions and model answers

**How does `ThreadPoolExecutor` use core size, maximum size, and queue?**

It normally creates workers up to core, then queues; only when queueing fails does it grow toward maximum, then reject. Therefore an unbounded queue often makes maximum size irrelevant. The queue and rejection policy are central overload decisions.

**How do you size a pool?**

Start near processor count for CPU-bound work. For blocking work, estimate blocking fraction and concurrency demand, but cap by downstream capacity, memory, and latency. Validate queue delay, utilization, and tail latency under representative load rather than trust a formula.

**`thenApply` versus `thenCompose`?**

`thenApply` maps `T` to `U`. If the function maps `T` to `CompletionStage<U>`, use `thenCompose` to flatten the asynchronous dependency rather than produce a nested stage.

**How do CompletableFuture exceptions work?**

A failed prerequisite skips normal dependent stages. `exceptionally` recovers failures, `handle` maps both outcomes, and `whenComplete` observes while preserving outcome in the ordinary case. `join` reports failure through `CompletionException`; `get` uses `ExecutionException` and is interruptible.

**What is correct shutdown?**

Stop admission with `shutdown`, await a bounded grace period, request interruption with `shutdownNow` if needed, await again, report survivors, and restore caller interrupt status if shutdown itself is interrupted.

## Exercises

1. Add explicit handling for rejection so `finalPrice` returns a failed future rather than throwing synchronously.
2. Replace the default fallback with typed classification for timeout, rejection, and domain failure.
3. Size a pool and queue for a stated arrival rate, service time, downstream connection limit, and burst budget.
4. Create and then fix a thread-starvation deadlock caused by waiting on same-pool subtasks.
5. Compare fixed-rate and fixed-delay execution when one run exceeds the period.
6. Implement fan-out to two independent futures and combine with `thenCombine` under one deadline.

## Chapter summary

Executors encapsulate scheduling and capacity; futures represent completion, not guaranteed task termination. Bounded queues and explicit rejection make overload controllable. Pool sizing follows computation, blocking, downstream limits, and measured queue latency. CompletableFuture composition avoids blocking when `thenCompose` and combination stages express dependencies, but execution context and exception recovery must be explicit. Every executor needs ownership and bounded shutdown.

## Revision checklist

- [ ] I understand executor admission order and unbounded-queue risk.
- [ ] I can size pools from workload and downstream constraints.
- [ ] I know `get(timeout)` and cancellation limitations.
- [ ] I can implement two-phase executor shutdown.
- [ ] I can choose `thenApply`, `thenCompose`, `thenCombine`, and error stages.
- [ ] I specify callback execution context rather than assume it.
- [ ] I can diagnose queue growth, hidden exceptions, and same-pool starvation.

