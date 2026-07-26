# Chapter 38: Concurrency Failure Modes, Testing, and Design Patterns

## Learning objectives

- Classify races, deadlocks, livelocks, starvation, missed signals, leaks, and performance collapse.
- Diagnose blocking and progress failures from timelines, thread dumps, JFR, and capacity metrics.
- Test concurrent code with coordinated starts, invariants, histories, stress harnesses, and bounded completion.
- Apply immutability, confinement, message passing, lock ordering, open calls, and bulkheads.
- Separate safety, liveness, and performance claims in design and interviews.

## Why this matters at SDE-2

Concurrent failures are nondeterministic only from the observer's perspective. They follow a protocol and a schedule. Adding sleeps may hide the schedule; adding logging may alter it. A test that passed one million times increases confidence but does not prove correctness. A thread dump showing every worker waiting may represent healthy idleness, a deadlock, downstream saturation, or same-pool starvation.

SDE-2 engineers turn symptoms into a graph: who owns which resource, who waits for whom, what state transition was expected, which happens-before edge is missing, and what progress guarantee was intended. They also prefer designs whose proof is short.

## First-principles model

Evaluate concurrency across three independent properties:

- **Safety:** Nothing bad happens. Examples: no negative balance, no duplicate lease owner, linearizable queue operations.
- **Liveness:** Something good eventually happens. Examples: every accepted task completes or cancels; locks are eventually acquired under stated assumptions.
- **Performance:** It happens within resource and latency goals.

A design can be safe but deadlocked, live but too slow, or fast in a benchmark but racy.

Wait-for graphs make deadlock concrete:

```text
Thread T1 owns Lock A and waits for Lock B
Thread T2 owns Lock B and waits for Lock A

T1 -> B -> T2 -> A -> T1     cycle means no participant can proceed
```

Removing one necessary condition prevents classic lock deadlock: mutual exclusion, hold-and-wait, no preemption, or circular wait. Lock ordering targets circular wait.

## Core terminology

- **Race condition:** Result depends incorrectly on relative timing/interleaving.
- **Data race:** Conflicting accesses not ordered by happens-before.
- **Lost update:** Two read-modify-write operations overwrite one effect.
- **Deadlock:** Participants wait in a cycle or condition that cannot be satisfied.
- **Livelock:** Participants keep changing/retrying but make no useful progress.
- **Starvation:** One participant is indefinitely denied progress while others proceed.
- **Priority inversion:** Higher-priority work waits behind lower-priority work holding a needed resource.
- **Thread starvation deadlock:** All workers wait for tasks queued to the same exhausted executor.
- **Linearizability:** Concurrent history behaves as if each operation took effect atomically between call and response.
- **Stress test:** Repeated/concurrent exploration intended to expose allowed schedules.

## Detailed mechanics

Data races create visibility and ordering uncertainty. Race conditions are broader: two correctly atomic operations can still violate a check-then-act protocol. Lost updates, duplicate initialization, stale snapshots, and unsafe publication belong here. The fix is to make the intended transition atomic and ordered, not merely mark every field volatile.

Missed-signal bugs occur when condition state and notification are not protected by one protocol. If a producer signals before a consumer enters an unrelated wait, the consumer can sleep forever despite available work. A correct condition variable changes/checks the predicate under one lock and always waits in a loop. Semaphores, latches, futures, and blocking queues retain state and are often safer than hand-written notifications.

Classic lock deadlock often meets four conditions:

1. Resources have exclusive ownership.
2. A participant holds one while waiting for another.
3. Ownership cannot be forcibly taken safely.
4. The wait-for graph contains a cycle.

Prevent it with a global lock order, one coarser lock, atomic database operation, avoiding nested ownership, or timed/interruptible acquisition plus rollback. Timeouts detect/escape some waits but do not make a protocol correct: repeated partial work can create livelock or data inconsistency.

Livelock example: two polite transfer workers detect conflict, both release, sleep for the same fixed delay, and retry in sync forever. Randomized/exponential backoff can reduce synchronization, but a deterministic owner/order is preferable when possible. Starvation can arise from unfair locks, endless high-priority tasks, read-heavy read/write locks, CAS losers, or a scheduler/resource admission policy.

Thread starvation deadlock does not require two locks. In a two-thread executor, two parent tasks submit child tasks to the same executor and block on their futures. Both workers are occupied by parents; children remain queued. Compose without blocking, run child work directly, allocate a separately justified executor, or redesign ownership.

Cancellation failures are liveness/resource bugs. A timeout on a caller does not necessarily stop downstream work. Orphan tasks can later mutate state, retain memory, hold connections, or consume the result of an expired request. Define cancellation propagation, idempotency, deadlines, and what commits are still legal after caller departure.

Thread leakage occurs when threads/executors are created repeatedly and never terminated, or when blocked tasks never receive cancellation. Thread-local leakage occurs when pooled threads retain per-request data after completion. Always remove manually managed thread locals in `finally`; prefer explicit context passing.

Testing must control setup without pretending to control the runtime schedule. Use `CountDownLatch`, `CyclicBarrier`, or `Phaser` to align starts and increase overlap. Use timeouts so a deadlock fails the test rather than hangs the suite. Avoid sleeps as correctness synchronization; a sleep can be a workload delay but should not prove that another thread finished.

Assert invariants and histories, not just final happy values. For a transfer system, total balance and nonnegative balances matter after every legal transition. For a concurrent object, record invocation/response times and results, then check whether a legal sequential ordering exists between them. This is linearizability testing; exhaustive checking can be expensive, so histories are bounded.

The OpenJDK jcstress project embodies JVM concurrency stress-testing concepts: actors perform operations concurrently, an arbiter observes outcomes, and results are classified as acceptable, interesting, or forbidden. It is especially useful for JMM litmus tests. Repeating a JUnit method manually is less rigorous because compiler warm-up, actor placement, outcome collection, and fork isolation matter. Stress tools find counterexamples; they do not prove all large programs correct.

Model checking or deterministic scheduler frameworks can explore controlled interleavings for abstractions they support. Static analysis can find inconsistent locking and unsafe publication patterns. Code review remains essential: list shared mutable variables, all accesses, guards, linearization points, cancellation points, and progress assumptions.

Design patterns reduce states:

- **Immutability plus atomic publication:** replace one validated snapshot.
- **Thread confinement/single writer:** one owner mutates state; others send messages.
- **Producer-consumer:** bounded queue separates producers and workers with backpressure.
- **Bulkhead:** independent concurrency limit per dependency/workload.
- **Lock ordering:** acquire multiple locks by a stable global key.
- **Open call:** copy/decide under lock, invoke unknown or slow code after releasing it.
- **Split phase:** begin asynchronous work, release resources, resume on completion.
- **Idempotent command:** retries/cancellation races do not duplicate business effects.
- **Structured lifetime:** child work does not silently outlive its owning operation.

Structured concurrency is a preview API in Java 21, not a finalized Java 21 feature. Its design idea remains useful: task lifetimes form a tree, sibling failure/cancellation is coordinated, and the owner joins before leaving scope.

> **Specification boundary:** Java defines monitor, volatile, atomic, thread lifecycle, and concurrent-library contracts. It does not guarantee fair scheduling or that stress tests explore every legal execution. Deadlock freedom and linearizability are properties the program must establish.

## Worked Java example

```java
import java.util.concurrent.locks.ReentrantLock;

public final class BankTransfers {
    static final class Account {
        final long id;
        final ReentrantLock lock = new ReentrantLock();
        long cents;

        Account(long id, long cents) {
            if (cents < 0) throw new IllegalArgumentException("cents");
            this.id = id;
            this.cents = cents;
        }

        long balance() {
            lock.lock();
            try {
                return cents;
            } finally {
                lock.unlock();
            }
        }
    }

    static boolean transfer(Account from, Account to, long cents)
            throws InterruptedException {
        if (from == to) return true;
        if (cents <= 0) throw new IllegalArgumentException("cents");
        if (from.id == to.id) throw new IllegalArgumentException("duplicate id");

        Account first = from.id < to.id ? from : to;
        Account second = from.id < to.id ? to : from;

        first.lock.lockInterruptibly();
        try {
            second.lock.lockInterruptibly();
            try {
                if (from.cents < cents) return false;
                from.cents -= cents;
                to.cents += cents;
                return true;
            } finally {
                second.lock.unlock();
            }
        } finally {
            first.lock.unlock();
        }
    }
}
```

Every transfer acquires accounts by ascending stable ID regardless of direction, eliminating a two-account circular wait. Both balance changes occur while both locks are held, so the in-process invariant moves atomically relative to code following the same discipline.

## Execution or memory walkthrough

Let account A have ID 1 and B have ID 2. T1 transfers A to B while T2 transfers B to A:

1. Both compute `first=A`, `second=B` because ordering depends on IDs, not direction.
2. Suppose T1 acquires A. T2 waits interruptibly for A and owns no other account lock.
3. T1 acquires B, validates funds, subtracts/adds, releases B then A.
4. Lock release/acquisition orders the updated balances for T2.
5. T2 acquires A then B and performs its transition.

No state has T1 holding A waiting for B while T2 holds B waiting for A. The wait-for graph cannot contain that two-lock cycle if every operation respects the same total order.

If T1 is interrupted while waiting for B, `lockInterruptibly` throws. T1 never acquired B, so the inner `try/finally` did not begin. The outer finally releases A. No balances changed because mutation follows both acquisitions.

Overflow is not handled in this educational code. `to.cents += cents` should use exact arithmetic or validated limits in a financial implementation. Across multiple JVMs/database rows, these Java locks do not coordinate other processes; a database transaction or distributed protocol is required.

## Complexity and performance

Transfer does O(1) domain work but lock wait is unbounded without stronger scheduling assumptions. Contention is per account: unrelated account pairs can proceed concurrently. A global bank lock simplifies proof but serializes all transfers; fine-grained account locks increase parallelism and lock-order complexity.

Deadlock detection from a wait-for graph is O(V + E) for cycle detection, but production evidence can omit application-level resources such as database locks or message acknowledgments. Tail latency increases nonlinearly near resource saturation even without deadlock.

Stress testing cost grows with threads, operations, histories, and possible interleavings. Complete schedule exploration is generally exponential. Target small state machines, boundary races, and known weak points, then combine formal reasoning, static checks, unit tests, stress, integration load, and production observability.

## Edge cases and common mistakes

- Concluding "no race" because a test passed repeatedly.
- Fixing a race by adding sleep, logging, or `yield`.
- Taking one thread dump and declaring deadlock without an ownership cycle.
- Applying lock ordering in one method while another path violates it.
- Generating lock-order IDs that can collide or change.
- Timing out lock acquisition but leaving partial side effects before retry.
- Retrying conflicts with identical delay, creating livelock.
- Using fair locks as a complete starvation guarantee.
- Blocking executor workers on child tasks queued to the same executor.
- Letting timed-out work commit non-idempotent effects later.
- Forgetting to shut down executors or clear thread locals.
- Treating a synchronized in-memory invariant as distributed consistency.
- Testing only final count and missing illegal intermediate states.

Priority changes are not a portable correctness fix for priority inversion. OS/JVM mappings vary, and application priorities can worsen starvation. Reduce lock duration, separate workloads, and use explicit admission.

## Production engineering notes

Diagnose from a synchronized timeline:

1. Determine whether the symptom is wrong state, no progress, or slow progress.
2. Capture several thread dumps, not just one; include timestamps and CPU/load.
3. Identify RUNNABLE CPU consumers, monitor owners/contenders, parked explicit-lock waiters, executor queues, and downstream waits.
4. Use JVM deadlock detection where available, but add database lock graphs, connection pools, queue lag, and remote dependency state.
5. Correlate JFR lock/park events, CPU profiles, GC/safepoints, and request traces.
6. Preserve the triggering workload and deploy a protocol fix with a regression stress test.

Incident example: all 32 pool threads show `FutureTask.get`, CPU is near zero, and queue depth grows. Each task submitted an enrichment subtask to the same pool and waited. No Java monitor cycle appears, so automatic deadlock detection reports nothing. This is thread-starvation deadlock. Replace blocking nested submission with completion composition or reserve/avoid the dependency structure; adding workers only moves the threshold.

Livelock evidence looks different: CPU and retry metrics are high, state changes repeatedly, but completed operations remain near zero. Add correlation IDs and attempt counts with rate-limited logs, then establish ownership/order or randomized bounded backoff.

> **HotSpot note:** HotSpot thread dumps and `jcmd Thread.print` can identify owned/waited monitors and some ownable synchronizers; JFR can capture Java monitor enter/wait, park, virtual-thread, and scheduling-related evidence depending on JDK configuration. Tool visibility is not a proof that no application or external deadlock exists.

## Interview questions and model answers

**Deadlock versus livelock versus starvation?**

In deadlock, participants are blocked in a cycle or unsatisfiable wait. In livelock, they keep reacting/retrying but complete no useful work. In starvation, the system progresses while one participant is indefinitely denied resources. Diagnosis and remedies differ.

**How do you prevent deadlock?**

Remove a necessary condition: avoid nested locks, use one lock, acquire all locks in a stable global order, use timed/interruptible acquisition with safe rollback, or redesign around message passing/transactions. Then verify every acquisition path follows the protocol.

**How do you test concurrent code?**

First prove invariants and happens-before/linearization points. Use latches or barriers to align actors, bounded timeouts to catch hangs, record histories, and stress across many forks/architectures with a harness such as jcstress for JMM cases. Tests find counterexamples but do not replace proof.

**What is thread-starvation deadlock?**

All executor workers block waiting for tasks that are queued to the same executor, leaving no worker to run them. It may not appear as a monitor cycle. Avoid blocking same-pool dependency chains and compose work or redesign capacity.

**Which concurrency design patterns do you prefer?**

Start with immutability, confinement/single ownership, immutable snapshot publication, bounded message passing, and high-level concurrent operations. Use locks for explicit multi-field invariants, with stable ordering and open calls. The best design minimizes shared mutable states.

## Exercises

1. Write a two-lock deadlock, capture it safely, then impose a global order.
2. Build a same-executor parent/child starvation example and replace blocking gets with composition.
3. Design a barrier-based lost-update test with acceptable and forbidden outcomes.
4. Define a linearizable history checker for a tiny concurrent stack with at most four operations.
5. Refactor a callback-under-lock component using the open-call pattern.
6. Design cancellation propagation for an HTTP request, database query, and message publish with an idempotency key.
7. List safety, liveness, and performance requirements for a bounded work queue separately.

## Chapter summary

Concurrency correctness requires separate safety, liveness, and performance arguments. Races violate atomicity or ordering; deadlocks form unsatisfiable waits; livelocks spend work without progress; starvation denies one participant. Tests should coordinate actors, assert invariants/histories, use deadlines, and stress with purpose, while recognizing that absence of a failure is not proof. Immutability, confinement, bounded message passing, lock ordering, open calls, bulkheads, and structured lifetimes reduce the state space and operational risk.

## Revision checklist

- [ ] I can distinguish data races, race conditions, lost updates, and missed signals.
- [ ] I can draw a wait-for graph and apply a stable lock order.
- [ ] I can identify deadlock, livelock, starvation, and same-pool starvation evidence.
- [ ] I use barriers/latches and bounded timeouts rather than sleeps for tests.
- [ ] I understand linearizability histories and jcstress-style outcome testing.
- [ ] I can design cancellation and shutdown without orphan work.
- [ ] I prefer immutability, confinement, message passing, and explicit ownership.
- [ ] I separate in-process synchronization from distributed consistency.

