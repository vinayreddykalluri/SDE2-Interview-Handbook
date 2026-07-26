# Chapter 33: Threads, Lifecycle, Interruption, and Cancellation

## Learning objectives

- Explain Java thread states as observable lifecycle categories rather than scheduler commands.
- Distinguish `start`, `run`, sleep, waiting, blocking, joining, daemon status, and termination.
- Use interruption as a cooperative cancellation protocol.
- Propagate or restore `InterruptedException` correctly.
- Design thread-owning components with explicit startup, bounded shutdown, and failure reporting.

## Why this matters at SDE-2

Services fail during shutdown as often as during steady state. A worker that swallows interruption can delay deployment, lose a partition lease, or leave a process to be killed after its grace period. A thread created without ownership can outlive the component that created it. A daemon thread can disappear before flushing critical work. An uncaught exception can silently remove capacity if no one observes it.

At SDE-2, "interrupt stops a thread" is not acceptable. Interruption requests cooperation. The target decides when and how to stop, blocking methods define how they react, and ownership code must wait for termination or report that it could not. These rules apply later to executors, futures, virtual threads, and structured concurrency designs.

## First-principles model

A thread is an independently scheduled sequence of Java actions sharing process resources with other threads. Creating a `Thread` object does not begin concurrent execution. Calling `start()` asks the runtime to schedule a new execution that invokes `run()` once.

```text
NEW --start()--> RUNNABLE --------------------------> TERMINATED
                  |   ^                                  ^
                  |   | monitor acquired / event         |
                  v   |                                  | run returns
               BLOCKED                                  | or throws
                  |                                      |
                  +--> WAITING / TIMED_WAITING -----------+
                       notify, unpark, timeout, interrupt
```

`RUNNABLE` includes threads actually executing and threads ready but waiting for CPU or certain native operations. Java's state enum is a diagnostic snapshot. It is not a complete OS scheduler model, and code should not coordinate correctness by polling another thread's state.

Cancellation is a protocol:

```text
owner requests cancellation
  -> publish cancellation state and/or interrupt
  -> worker notices at a cancellation point
  -> worker preserves invariants and releases resources
  -> worker terminates
  -> owner joins or otherwise observes completion
```

## Core terminology

- **Platform thread:** A traditional Java thread commonly mapped closely to an operating-system thread.
- **Virtual thread:** A lightweight Java thread scheduled by the runtime; covered deeply in Chapter 37.
- **Interrupt status:** Per-thread boolean cancellation signal manipulated by `interrupt`, `isInterrupted`, and `interrupted`.
- **Cancellation point:** A place where code checks status or calls an interruptible blocking API.
- **Daemon thread:** Thread that does not by itself keep the JVM alive.
- **Join:** Waiting for another thread to terminate.
- **Uncaught exception handler:** Last-resort observer invoked when a thread exits because `run` threw an uncaught exception.
- **Cooperative cancellation:** Target code participates in stopping at safe boundaries.
- **Thread confinement:** Mutable state accessed by only one thread.

## Detailed mechanics

`Thread.start()` has two important properties. It creates a new concurrent execution of `run`, and actions before `start` happen-before actions in the started thread. Calling `run()` directly is an ordinary synchronous method call on the current thread. A `Thread` instance can be started at most once; a second `start()` throws `IllegalThreadStateException` even after termination.

The six `Thread.State` values are:

- `NEW`: constructed but not started.
- `RUNNABLE`: executing or eligible to execute in the JVM/OS view.
- `BLOCKED`: waiting to enter a `synchronized` monitor.
- `WAITING`: waiting indefinitely through operations such as untimed `join`, `wait`, or `park`.
- `TIMED_WAITING`: waiting with a deadline, including `sleep`, timed `join`, timed `wait`, or timed park.
- `TERMINATED`: `run` completed normally or abruptly.

State can change immediately after observation. It is useful for thread dumps, not for writing `if (thread.getState() == WAITING) ...` protocols.

Calling `interrupt()` normally sets the target's interrupt status. If the target is in an interruptible blocking method such as `sleep`, `wait`, `join`, or many `java.util.concurrent` waits, that method usually throws `InterruptedException` and clears the status. The exception is not an error to log and ignore. It is a request arriving through the method's control flow.

There are two standard handling choices:

1. **Propagate:** A method that can abandon its work declares `throws InterruptedException`, performs required local cleanup, and lets its caller choose policy.
2. **Restore and stop/return:** Code at a boundary that cannot throw restores status with `Thread.currentThread().interrupt()` and exits or reports cancellation.

Swallowing the exception loses the request. Restoring status and blindly continuing the same interruptible loop can cause immediate repeated failures, so restoration must accompany a deliberate boundary decision.

`Thread.interrupted()` returns and clears the current thread's status. `currentThread().isInterrupted()` observes without clearing. This naming difference causes bugs. A CPU-bound loop should check status at a reasonable frequency:

```java
while (!Thread.currentThread().isInterrupted()) {
    computeOneChunk();
}
```

Not every block responds to interruption. Monitor entry for `synchronized` is not interruptible. Some legacy or native I/O may require closing a resource or using an API-specific cancellation mechanism. `interrupt()` cannot make arbitrary code safe to stop.

`join()` waits for termination and establishes a memory edge: all actions in the terminated thread happen-before another thread successfully returns from `join`. A timeout only bounds the wait; it does not terminate the target. Code must check `isAlive()` or use a higher-level completion result after a timed join.

Daemon status must be set before start. The JVM can exit when only daemon threads remain; it does not promise daemon `finally` blocks complete. Critical persistence, commits, lease release, and telemetry flushes need explicit lifecycle coordination rather than daemon reliance.

An uncaught exception terminates that thread. The JVM consults the thread's handler, thread group behavior, or default handler. Logging is useful, but a robust component also converts worker loss into health state or supervisor action. Restarting blindly can create a crash loop or duplicate unsafe work.

> **Specification boundary:** The JLS and Java APIs define thread actions, lifecycle methods, interruption status, and happens-before rules. They do not promise fair scheduling, immediate execution after `start`, immediate response to interruption, an OS thread mapping, or a maximum shutdown time.

## Worked Java example

```java
import java.time.Duration;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

public final class OwnedWorker implements AutoCloseable {
    private final BlockingQueue<String> jobs = new LinkedBlockingQueue<>();
    private final Thread thread = new Thread(this::runLoop, "owned-worker");
    private volatile boolean stopping;
    private boolean started;

    public synchronized void start() {
        if (started) throw new IllegalStateException("already started");
        if (stopping) throw new IllegalStateException("already stopped");
        started = true;
        thread.start();
    }

    public synchronized void submit(String job) {
        if (stopping) throw new IllegalStateException("stopping");
        jobs.add(job);
    }

    private void runLoop() {
        try {
            while (!stopping) {
                String job = jobs.poll(1, TimeUnit.SECONDS);
                if (job != null) process(job);
            }
        } catch (InterruptedException interrupted) {
            Thread.currentThread().interrupt();
        } finally {
            releaseWorkerResources();
        }
    }

    private void process(String job) {
        System.out.println(job);
    }

    private void releaseWorkerResources() {}

    public void stop(Duration timeout) throws InterruptedException {
        if (timeout.isNegative() || timeout.isZero()) {
            throw new IllegalArgumentException("positive timeout required");
        }
        synchronized (this) {
            stopping = true;
        }
        thread.interrupt();
        thread.join(Math.max(1, timeout.toMillis()));
        if (thread.isAlive()) {
            throw new IllegalStateException("worker did not stop in " + timeout);
        }
    }

    @Override
    public void close() throws InterruptedException {
        stop(Duration.ofSeconds(5));
    }
}
```

The object has explicit ownership. Construction does not leak `this` by starting a thread. `start()` is one-shot. Shutdown publishes `stopping`, interrupts the queue wait, joins with a bound, and reports failure instead of pretending the worker died.

## Execution or memory walkthrough

1. The owner constructs `OwnedWorker`. The internal thread is `NEW`; no concurrent access begins.
2. Synchronized `start` marks the component started and calls `Thread.start`. Prior construction actions are visible to the worker through the start happens-before edge.
3. `runLoop` polls the thread-safe queue. With no work, the worker is generally `TIMED_WAITING` in the queue implementation.
4. `submit("A")` publishes the element through `BlockingQueue`'s memory-consistency contract. The worker obtains it and calls `process`.
5. The owner calls `stop`. The volatile write `stopping=true` becomes visible to later volatile reads, and `interrupt` wakes an interruptible poll promptly.
6. `poll` throws `InterruptedException`, clearing status. The catch restores it because this boundary is terminating rather than propagating.
7. `finally` releases resources. `runLoop` returns, and the thread becomes `TERMINATED`.
8. Successful `join` lets the owner observe all worker actions. If timeout expires first, `isAlive()` causes a visible shutdown failure.

There is a deliberate policy question: queued jobs may remain unprocessed. A drain-before-stop service would reject new jobs, process the existing queue, then interrupt only after a grace deadline. Cancellation semantics must be documented rather than accidental.

## Complexity and performance

Thread creation and platform-thread stacks consume nontrivial native/runtime resources. Starting one platform thread per tiny task can spend more time scheduling than executing. A blocking queue offers O(1) expected enqueue/dequeue for typical linked implementations, but contention and wakeups dominate constants.

Polling every second is not needed for visibility because `interrupt` wakes the wait; an untimed `take()` could reduce periodic wakeups. The timed poll in the example also provides a natural health/cancellation checkpoint. A busy loop would offer lower theoretical response latency but waste CPU.

Cancellation check frequency trades overhead for response time. Check between bounded chunks, not once per trivial arithmetic operation and not only after an unbounded task. Blocking operations should have deadlines when external systems can hang independently of interruption.

> **HotSpot note:** Platform-thread stack reservation, native mapping, scheduling transitions, safepoint interaction, and diagnostic state details are HotSpot/OS dependent. Thread priorities are hints mapped differently across operating systems and should not enforce correctness.

## Edge cases and common mistakes

- Calling `run()` when concurrency was intended.
- Starting a thread in a constructor, allowing `this` to escape before construction completes.
- Reusing a terminated `Thread` object.
- Assuming `interrupt()` kills the target or closes every I/O operation.
- Catching `InterruptedException`, logging it, and continuing.
- Restoring interrupt status but continuing an interruptible call in a tight loop.
- Using `Thread.stop`, `suspend`, or `resume`; these unsafe/deprecated mechanisms can expose broken invariants or deadlock.
- Depending on thread state polling for coordination.
- Marking critical workers daemon and expecting guaranteed cleanup.
- Waiting forever during shutdown without a deadline or diagnostics.
- Treating an uncaught-exception log as sufficient supervision.

`sleep` does not release monitors a thread owns. `wait` releases the particular monitor as part of its condition-wait protocol. Confusing them can create long lock stalls.

## Production engineering notes

Every thread needs an owner, name, failure policy, cancellation mechanism, and termination observation. Prefer executors or structured task scopes where available over scattered raw threads, but the same ownership questions remain. Name threads with subsystem purpose rather than per-request secrets.

A shutdown sequence should normally:

1. Mark the component not ready and stop admitting work.
2. Request cooperative cancellation or graceful draining.
3. Close resources that unblock non-interruptible operations.
4. Wait with a deadline derived from the platform's termination grace period.
5. Capture thread stacks and report remaining work if the deadline expires.
6. Let process-level orchestration escalate only after evidence and policy.

Production incident example: a consumer deployment takes the full 30-second grace period. A thread dump shows the worker blocked in `BlockingQueue.take`; shutdown set a plain `boolean` but never interrupted. Because no item arrives, the loop cannot recheck the flag. Fix the protocol by safe publication plus interrupt/queue signal, then join and expose shutdown duration.

Do not use interruption as a generic business error. Preserve its meaning as cancellation. If a library translates it, include the original cause and restore status unless the API contract has transferred cancellation through another explicit result.

## Interview questions and model answers

**What is the difference between `start()` and `run()`?**

`start()` arranges a new thread of execution that invokes `run` once and creates a happens-before edge from earlier actions. Calling `run()` directly is a normal synchronous call on the current thread.

**How does interruption work?**

It is a cooperative request represented by interrupt status. Interruptible blocking APIs often throw `InterruptedException` and clear status. Code should propagate the exception or restore status and exit at a boundary. It cannot safely force arbitrary code to stop.

**Why restore interrupt status?**

If a method cannot propagate `InterruptedException`, restoring preserves the cancellation signal for higher-level code. Restoration should be paired with return, termination, or another explicit policy, not swallowed by continued work.

**What does `join()` guarantee?**

It waits for termination. When it returns successfully because the target terminated, all actions in that thread happen-before actions after the join in the waiting thread. A timed join can return while the target remains alive.

**Daemon versus non-daemon?**

Non-daemon threads keep the JVM alive. The JVM may exit when only daemon threads remain, without guaranteeing daemon cleanup. Daemon status is therefore unsuitable as the sole lifecycle policy for critical work.

## Exercises

1. Change the worker to graceful drain semantics and define what happens at the deadline.
2. Write a CPU-bound search that responds to interruption every bounded chunk.
3. Demonstrate `Thread.interrupted()` clearing status and `isInterrupted()` preserving it.
4. Add an uncaught-exception handler that marks component health unhealthy.
5. Diagnose a shutdown where a worker is blocked in socket I/O that ignores interruption.
6. Explain the happens-before paths created by `start`, queue handoff, volatile stop, and `join`.

## Chapter summary

Java threads move through diagnostic lifecycle states, but correctness comes from explicit synchronization and ownership, not state polling. Interruption is a cooperative cancellation request. Interruptible waits convert it to `InterruptedException`, which must be propagated or restored with a deliberate exit policy. Reliable components stop admission, request cancellation, unblock waits, release resources, join with a deadline, and report nontermination.

## Revision checklist

- [ ] I can explain all `Thread.State` values without treating them as scheduler guarantees.
- [ ] I know why `start` differs from `run` and cannot be repeated.
- [ ] I can propagate or restore interruption correctly.
- [ ] I can identify APIs that may require cancellation beyond interruption.
- [ ] I can design owned startup and bounded shutdown.
- [ ] I understand daemon and uncaught-exception risks.
- [ ] I can state the `start` and `join` happens-before rules.
