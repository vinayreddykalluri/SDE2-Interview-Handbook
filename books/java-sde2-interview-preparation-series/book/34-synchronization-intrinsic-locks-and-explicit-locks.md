# Chapter 34: Synchronization, Intrinsic Locks, and Explicit Locks

## Learning objectives

- Explain mutual exclusion and memory visibility provided by Java monitors.
- Choose the correct lock object and scope for a shared invariant.
- Use `wait`, `notifyAll`, `ReentrantLock`, and `Condition` with condition predicates.
- Handle interruption and exceptions without leaking locks.
- Compare intrinsic locks, explicit locks, read-write strategies, and lock-free alternatives.

## Why this matters at SDE-2

Most lock bugs are design bugs, not syntax bugs. Synchronizing a setter while leaving a reader unguarded does not protect an invariant. Calling external code while holding a lock can turn one slow dependency into global request serialization. A missed signal or `if` around `wait` can hang only under rare timing. An explicit lock without `finally` can permanently block a subsystem after one exception.

An SDE-2 should state what data a lock guards, which operations must be atomic together, how waiting threads are signaled, and how shutdown interrupts those waits. The goal is not "make the method thread-safe" but preserve a named invariant under all interleavings.

## First-principles model

A lock establishes ownership of a critical section. Mutual exclusion ensures at most one holder executes code guarded by that lock. The Java Memory Model also orders state:

```text
Thread A                              Thread B
lock L                               lock L (later)
  mutate guarded state
unlock L -------- happens-before ---> read guarded state
```

Condition waiting adds another idea. A thread cannot make progress until a predicate over guarded state becomes true. It must atomically release the lock and wait, then reacquire before rechecking:

```text
lock
while (!predicate) {
    await: release lock + wait + reacquire lock
}
change guarded state
unlock
```

Signals are hints that state may have changed. The predicate, not the notification, determines permission to proceed.

## Core terminology

- **Monitor/intrinsic lock:** Lock and wait-set behavior associated with every Java object.
- **Critical section:** Code that accesses a shared invariant under its guarding lock.
- **Reentrancy:** A thread holding a lock can acquire it again, with a hold count.
- **Contention:** Multiple threads compete for the same lock.
- **Condition predicate:** Boolean expression over guarded state required for progress.
- **Wait set:** Threads waiting through `Object.wait` on a monitor.
- **Condition:** Explicit wait queue associated with a `Lock`.
- **Fair lock:** Lock configured to favor longer-waiting contenders under documented limitations.
- **Read-write lock:** Lock with shared read and exclusive write modes.
- **Optimistic read:** Read attempted without exclusive ownership, followed by validation.

## Detailed mechanics

Every object can serve as a monitor. Entering a `synchronized (lock)` statement acquires that monitor; normal or abrupt exit releases it. An instance `synchronized` method locks `this`. A static synchronized method locks the corresponding `Class` object. These are different locks, so mixing instance and static synchronization does not serialize access unless that is the intended design.

Intrinsic locks are reentrant. If method A holds `this` and calls synchronized method B on the same object, the thread proceeds and increments its logical hold count. It must exit the matching number of acquisitions before another thread can enter.

Monitor exit happens-before a later monitor entry on the same object. This supplies visibility for all earlier actions, not just fields declared inside the synchronized block. The discipline still requires every conflicting access to participate through the same lock or another valid ordering mechanism.

`Object.wait()` requires the caller to own that object's monitor. It releases that monitor and waits; upon wakeup it must reacquire before returning. It can return due to notification, interruption, timeout, or spurious wakeup. Therefore test the predicate in a loop. `notify()` wakes one arbitrary waiter; using it when different predicates share a wait set can wake an ineligible thread and strand the eligible one. `notifyAll()` is safer for correctness, though potentially more expensive.

`sleep()` does not release owned monitors. `yield()` is only a scheduling hint. Neither creates a happens-before relationship suitable for publication.

`ReentrantLock` provides monitor-like exclusion and JMM effects with additional operations:

- `tryLock()` avoids indefinite acquisition and supports timed attempts.
- `lockInterruptibly()` lets cancellation abort lock acquisition.
- multiple `Condition` instances separate wait sets for different predicates;
- optional fairness influences admission policy;
- lock state and queue inspection support diagnostics, with snapshot limitations.

Always unlock in `finally` after a successful acquisition:

```java
lock.lock();
try {
    changeState();
} finally {
    lock.unlock();
}
```

Do not place `lock()` inside the `try` if acquisition can fail/interrupt and code might then unlock a lock it never acquired. With timed `tryLock`, branch on the boolean.

A `Condition` belongs to one lock. `await` releases that lock and reacquires before returning, with interruptible, uninterruptible, and timed variants. `signal`/`signalAll` require lock ownership. As with monitors, use predicate loops.

`ReentrantReadWriteLock` can improve throughput when read sections are long, writes rare, and contention material. For tiny state, bookkeeping and writer starvation concerns can make a normal lock faster. Upgrade from read to write is hazardous; release/read then acquire/write and revalidate, or use a documented conversion mechanism.

`StampedLock` offers write, read, and optimistic-read modes but is not reentrant. Optimistic reads must validate and must not expose inconsistent intermediate data before validation. It has different interruption and ownership semantics from `Lock`, so it is an advanced measured optimization, not a default.

> **Specification boundary:** Java APIs and the JMM define monitor/lock exclusion, waiting contracts, and memory-consistency effects. They do not promise fair scheduling for intrinsic locks, a particular queue algorithm, adaptive spinning, or an exact mapping to OS mutexes.

## Worked Java example

```java
import java.util.ArrayDeque;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.ReentrantLock;

public final class BoundedBuffer<E> {
    private final ArrayDeque<E> elements = new ArrayDeque<>();
    private final int capacity;
    private final ReentrantLock lock = new ReentrantLock();
    private final Condition notEmpty = lock.newCondition();
    private final Condition notFull = lock.newCondition();

    public BoundedBuffer(int capacity) {
        if (capacity <= 0) throw new IllegalArgumentException("capacity");
        this.capacity = capacity;
    }

    public void put(E element) throws InterruptedException {
        if (element == null) throw new NullPointerException("element");
        lock.lockInterruptibly();
        try {
            while (elements.size() == capacity) {
                notFull.await();
            }
            elements.addLast(element);
            notEmpty.signal();
        } finally {
            lock.unlock();
        }
    }

    public E take() throws InterruptedException {
        lock.lockInterruptibly();
        try {
            while (elements.isEmpty()) {
                notEmpty.await();
            }
            E result = elements.removeFirst();
            notFull.signal();
            return result;
        } finally {
            lock.unlock();
        }
    }

    public int size() {
        lock.lock();
        try {
            return elements.size();
        } finally {
            lock.unlock();
        }
    }
}
```

Two conditions avoid waking producers when only consumers can proceed and vice versa. The queue, its size, and capacity invariant are always accessed under one lock.

## Execution or memory walkthrough

Assume capacity one and an empty buffer:

1. Consumer C calls `take`, acquires `lock`, observes empty, and calls `notEmpty.await`.
2. `await` atomically places C in the condition wait protocol and releases `lock`. C is not consuming CPU.
3. Producer P acquires `lock`, observes space, appends A, and calls `notEmpty.signal`.
4. Signal makes one waiter eligible to transfer/reacquire; it does not hand the lock directly to C. P still owns the lock.
5. P exits `finally` and unlocks. Its state changes happen-before C's successful later acquisition through lock semantics.
6. C reacquires, returns from `await`, and rechecks `elements.isEmpty()` in the loop. It removes A and signals `notFull`.
7. If another consumer acquired first and removed A, C would find empty again and await. This is why `if` is incorrect even without a spurious wakeup.

If C is interrupted during `await`, the call throws after reacquiring/processing according to the condition contract. `finally` releases the lock, and `InterruptedException` propagates. The queue invariant remains intact.

The memory shape is simple: the `BoundedBuffer` owns one deque and lock. Waiting threads are managed through condition/lock implementation nodes outside the domain deque; they do not need application-level polling flags.

## Complexity and performance

Deque insertion/removal is amortized O(1); lock acquisition is O(1) algorithmically but can wait without a finite bound under contention. A condition wait releases CPU resources but park/unpark and scheduling add latency. `size` is exact at the linearization point under the lock and can be stale immediately after return.

Lock performance depends on critical-section duration, arrival rate, core count, preemption, and fairness. The utilization principle matters: as time inside a serialized region approaches capacity, queueing delay rises sharply. Reduce shared state and critical-section work before replacing the lock primitive.

Measure both acquisition wait and hold duration; either can dominate tail latency.

Fair `ReentrantLock` can reduce barging/starvation in some workloads but often lowers throughput and does not guarantee fair thread scheduling. `tryLock()` can barge even on a fair lock under documented behavior. Measure the workload whose property matters.

> **HotSpot note:** HotSpot has evolved monitor representations, fast paths, spinning, and inflation/deflation policies across JDK releases. Source-level performance should not rely on a particular object-header lock encoding. Uncontended `synchronized` can be highly optimized.

## Edge cases and common mistakes

- Locking only writes while ordinary reads race.
- Guarding one invariant with multiple unrelated lock objects.
- Synchronizing on an externally accessible string, boxed value, collection, or replaceable field.
- Calling `wait`, `notify`, or `notifyAll` without owning the same object's monitor.
- Using `if` instead of `while` around a condition wait.
- Assuming a signal transfers lock ownership or proves the predicate true.
- Forgetting `unlock` in `finally`.
- Calling slow network/database/user callbacks while holding a shared lock.
- Returning a mutable guarded collection reference to callers.
- Choosing a fair/read-write/stamped lock without measured need.
- Acquiring locks in inconsistent order; analyzed deeply in Chapter 38.

An intrinsic lock acquisition cannot be interrupted or timed. If bounded lock wait is a requirement, an explicit lock may be necessary. Conversely, replacing a simple `synchronized` block with `ReentrantLock` only for perceived speed adds failure modes without guaranteed gain.

## Production engineering notes

Document each invariant and its guard:

```java
// Guarded by lock: elements.size() <= capacity and all deque access.
```

Keep critical sections small in elapsed time, not merely line count. Copy needed state under lock, release, then perform logging, serialization, callbacks, or remote I/O. If the operation must be atomic with an external system, a Java lock alone cannot provide distributed atomicity; use transactions, idempotency, or a workflow protocol.

Incident example: request p99 jumps from 50 ms to 8 seconds while CPU is low. Thread dumps show 180 threads `BLOCKED` entering a synchronized cache method; the owner is performing a remote refresh inside the monitor. One slow upstream serialized the service. Redesign with an immutable snapshot, single-flight refresh outside the publication lock, a timeout, and stale-value policy.

Thread dumps show monitor owner and contenders for intrinsic locks and often explicit-lock parking stacks. JFR lock events and profiles reveal duration and hot call sites. One dump is a snapshot; collect a short series and correlate with request/CPU timelines.

For shutdown, waiting methods should propagate interruption. A condition-based component can also set a closed predicate under lock and `signalAll` so every waiter can distinguish closure from temporary empty/full state.

## Interview questions and model answers

**What does `synchronized` guarantee?**

Mutual exclusion for code using the same monitor and memory ordering: an unlock happens-before a subsequent lock of that monitor. It is reentrant and releases on normal or abrupt block exit. It does not make unrelated unsynchronized accesses safe.

**Why call `wait` in a loop?**

Wakeups can be spurious, a notification can target a thread whose predicate is false, or another thread can consume the state before this waiter reacquires the lock. The condition predicate must be rechecked while holding the lock.

**When would you use `ReentrantLock` instead of `synchronized`?**

When requirements include timed or interruptible acquisition, multiple conditions, or a deliberately evaluated fairness/inspection feature. For simple scoped exclusion, `synchronized` is safer and concise.

**Does `notify` wake the most appropriate waiter?**

No such selection is guaranteed. With multiple predicates on one monitor, `notify` can wake an ineligible waiter. `notifyAll` plus predicate loops is safer, or separate explicit `Condition` queues.

**Read-write lock versus normal lock?**

A read-write lock can help with long, frequent reads and rare writes under real contention. It adds coordination, can complicate upgrades, and may reduce performance for short sections. Benchmark the actual read/write mix.

## Exercises

1. Add `close()` to `BoundedBuffer` so all waiting producers/consumers terminate with defined behavior.
2. Implement the same buffer with `synchronized`, `wait`, and `notifyAll`.
3. Demonstrate why replacing each `while` with `if` is incorrect using a two-consumer trace.
4. Refactor code that performs a callback under lock into snapshot-then-callback form.
5. Compare `lock`, `lockInterruptibly`, and timed `tryLock` cancellation behavior.
6. Write the invariant and lock-order documentation for a two-account transfer.

## Chapter summary

Locks protect named invariants by combining mutual exclusion with happens-before ordering. Intrinsic monitors are reentrant and support one wait set; explicit locks add timed/interruptible acquisition and multiple conditions. Condition waits always require a predicate loop, and every successful explicit acquisition requires `finally`-based release. Performance comes mainly from reducing contention and critical-section duration, not selecting a fashionable lock.

## Revision checklist

- [ ] I can identify the exact invariant and lock object.
- [ ] I can explain monitor unlock-to-lock visibility.
- [ ] I know why wait/await uses a loop and reacquires the lock.
- [ ] I can use `ReentrantLock` and `Condition` exception-safely.
- [ ] I can choose among intrinsic, explicit, read-write, and optimistic locking.
- [ ] I avoid external calls and mutable-state escape under a shared lock.
- [ ] I can investigate contention using dumps, JFR, and a timeline.
