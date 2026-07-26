# Chapter 37: Concurrent Collections and Virtual Threads

## Learning objectives

- Choose a concurrent collection by access pattern, ordering, consistency, and capacity needs.
- Use atomic map operations instead of unsafe compound check-then-act sequences.
- Explain weakly consistent and snapshot iteration without expecting fail-fast behavior.
- Design Java 21 virtual-thread-per-task services without pooling virtual threads.
- Recognize pinning and bound scarce downstream resources independently of thread count.

## Why this matters at SDE-2

Replacing `HashMap` with `ConcurrentHashMap` prevents structural corruption but does not automatically preserve a business invariant across several calls. An unbounded "concurrent" queue can still exhaust memory. Snapshot iteration can be perfect for configuration listeners and disastrous for a write-heavy list.

Virtual threads change the cost of blocking concurrency, not the capacity of databases, remote APIs, CPU cores, or memory. A service can move from 200 pooled platform threads to 20,000 virtual requests and overload its database in seconds. SDE-2 design therefore separates task representation from resource admission.

## First-principles model

A concurrent collection defines synchronization at the operation boundary. The key question is which compound action is one operation:

```text
unsafe composition                      atomic collection operation
if (!map.containsKey(k)) {              map.computeIfAbsent(k, loader)
    map.put(k, load(k));
}
```

Thread safety of each call does not make the left pair atomic. Another thread can change the map between calls.

Virtual threads address a different boundary. They let one Java thread represent one task while the runtime schedules many such threads over fewer carrier platform threads:

```text
virtual tasks:  V1 V2 V3 V4 V5 ... V10000
                  \ | /     \ | /
carrier threads:   P1   P2   P3   ...
                         |
                       CPU cores
```

When supported blocking operations park a virtual thread, its continuation can be unmounted so the carrier runs another. The application keeps straightforward blocking code without dedicating one platform stack/thread to every wait.

## Core terminology

- **Linearizable operation:** Appears to take effect at one point between invocation and response.
- **Weakly consistent iterator:** Tolerates concurrent updates, does not throw `ConcurrentModificationException`, and may reflect some updates under its contract.
- **Snapshot iterator:** Iterates an immutable array/version captured when the iterator was created.
- **Blocking queue:** Queue whose operations can wait for capacity or elements.
- **Nonblocking collection:** Collection whose core operations use progress techniques without ordinary mutual-exclusion blocking.
- **Virtual thread:** Lightweight `Thread` implementation finalized in Java 21 for high-throughput blocking tasks.
- **Carrier:** Platform thread on which a virtual thread is mounted for execution.
- **Mount/unmount:** Runtime scheduling of virtual-thread execution state onto/off a carrier.
- **Pinning:** Condition in which a blocked virtual thread cannot unmount from its carrier in the relevant JDK.
- **Bulkhead:** Independent capacity limit preventing one workload/resource from consuming all concurrency.

## Detailed mechanics

`ConcurrentHashMap` supports concurrent retrievals and high update concurrency. It rejects null keys and values, which avoids ambiguity between absent and mapped-to-null in concurrent reads. Individual operations such as `putIfAbsent`, conditional `remove`, `replace`, `compute`, `computeIfAbsent`, and `merge` provide atomic compound behavior under their documented contracts.

Use the operation that matches the invariant. A cache loader usually needs `computeIfAbsent`; a state machine may need `compute`; compare-and-remove can use `remove(key, expectedValue)`. Do not split an atomic transition into `get`, decide, `put`. Mapping/remapping functions should be short, nonblocking where possible, and must not recursively update the same map in ways forbidden by the API. Other map keys can change while one key is computed, so map-wide invariants still need another design.

ConcurrentHashMap iterators, spliterators, and enumerations are weakly consistent. They do not freeze the map, do not throw fail-fast exceptions, and may observe updates occurring during traversal according to the documented consistency. Aggregate methods such as `size`, `isEmpty`, and bulk reductions can be transient under concurrent mutation; use them for monitoring or estimates, not authorization decisions requiring a global snapshot.

> **HotSpot note:** Mainstream JDK implementations use CAS, bins, selective locking, resizing cooperation, and tree-shaped bins under collision for `ConcurrentHashMap`. These structures and thresholds are implementation details, not the Map or concurrency specification.

`CopyOnWriteArrayList` stores an array snapshot. Reads and traversal avoid mutation interference; an iterator sees the array version from iterator creation and does not support mutating iterator operations. Every structural write copies the backing array, making writes O(n) and producing garbage. It fits small, read-mostly listener/config lists with rare changes, not event queues or frequently updated registries.

`ConcurrentLinkedQueue` is an unbounded nonblocking FIFO queue. `offer` and `poll` are efficient under concurrency, while `size()` typically traverses and can be expensive/inexact as a control signal under mutation. Because it is unbounded, it supplies no backpressure.

Blocking queues combine storage with wait/signal protocols:

- `ArrayBlockingQueue`: fixed capacity array, optional fairness, predictable bound.
- `LinkedBlockingQueue`: optionally bounded; default effectively large capacity is an overload risk.
- `SynchronousQueue`: no element capacity; each handoff pairs producer and consumer.
- `PriorityBlockingQueue`: priority ordering but logically unbounded capacity.
- `DelayQueue`: elements become available after delay expiration and is logically unbounded.

Methods express overload policy: `add` throws on full, `offer` reports immediately, timed `offer` waits to a deadline, and `put` waits interruptibly. Consumers similarly choose `poll`, timed `poll`, or `take`. Queue memory-consistency effects publish actions before insertion to a thread that subsequently removes/accesses the element through the specified handoff.

`ConcurrentSkipListMap` and Set provide sorted concurrent navigation with expected O(log n) search/update. They are useful when range queries and order are requirements; a hash-based structure is usually simpler/faster for unordered exact lookup. `ConcurrentSkipListMap` also disallows null.

Virtual threads are ordinary Java `Thread` instances in API semantics: they have names/IDs, interruption, thread locals, stack traces, and happens-before lifecycle rules. Java 21 creation includes:

```java
Thread.startVirtualThread(task);
Thread.ofVirtual().name("fetch-", 0).start(task);
Executors.newVirtualThreadPerTaskExecutor();
```

The executor creates a new virtual thread for each task; it is not a fixed pool of reusable virtual threads. Pooling cheap task threads reintroduces queueing and thread-local leakage without protecting a scarce resource. Bound the scarce resource directly with a connection pool, semaphore, bounded queue, rate limiter, or downstream concurrency control.

Virtual threads improve scalability for workloads with many blocking waits and a natural thread-per-request style. They do not increase CPU parallelism. CPU-bound work still competes for cores and may need an executor sized for CPU scheduling or a semaphore limiting expensive sections.

In Java 21, a virtual thread can be pinned to its carrier while executing inside a `synchronized` block/method or native/foreign call. If it blocks while pinned, the carrier cannot run another virtual thread. Brief uncontended synchronized sections are not inherently a problem; long/blocking operations while holding a monitor are. `ReentrantLock` waits integrate with parking and can be an alternative after measurement, but mechanical replacement can introduce bugs.

JDK 24 delivered [JEP 491](https://openjdk.org/jeps/491), which removes nearly all pinning caused by `synchronized` monitors. Selected native/foreign-call and class-loading or class-initialization situations can still retain a carrier. Code, diagnosis, and interview answers must therefore name the deployed release. Pinning affects scalability, not monitor correctness.

Thread locals work with virtual threads but can multiply memory by enormous thread counts. Large per-thread caches and pool-era assumptions should be removed. Scoped values and structured concurrency appear as preview APIs in Java 21 and must be compiled/run with preview enabled; they should not be presented as final Java 21 contracts.

> **Specification boundary:** Java 21 specifies virtual-thread API behavior and concurrent collection contracts. Carrier scheduling, continuation representation, default scheduler parallelism, ConcurrentHashMap layout, and specific pinning diagnostics are implementation details.

## Worked Java example

```java
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.Semaphore;

public final class VirtualThreadQueries {
    private final Semaphore databaseSlots = new Semaphore(20);

    String query(int id) throws InterruptedException {
        databaseSlots.acquire();
        try {
            Thread.sleep(25); // Simulated interruptible blocking database call.
            return "row-" + id;
        } finally {
            databaseSlots.release();
        }
    }

    List<String> loadAll(int count) throws InterruptedException, ExecutionException {
        try (ExecutorService executor = Executors.newVirtualThreadPerTaskExecutor()) {
            List<Future<String>> futures = new ArrayList<>();
            for (int i = 0; i < count; i++) {
                int id = i;
                futures.add(executor.submit(() -> query(id)));
            }

            List<String> results = new ArrayList<>(count);
            for (Future<String> future : futures) {
                results.add(future.get());
            }
            return results;
        }
    }
}
```

The executor can represent hundreds or thousands of waiting tasks without an equally large platform-thread pool. The semaphore preserves the real database concurrency budget at 20. In production, the connection pool may itself enforce a bound, but an earlier application bulkhead can bound waiters and deadline handling more intentionally.

## Execution or memory walkthrough

For `loadAll(100)`:

1. The loop creates 100 task submissions; the executor starts one virtual thread per task rather than queuing behind a fixed virtual-thread pool.
2. The first 20 acquire semaphore permits. The remaining 80 park waiting for permits. Parking through the concurrency framework normally allows their carriers to run other virtual threads.
3. Each admitted thread calls the simulated blocking operation. `Thread.sleep` unmounts a non-pinned virtual thread in the Java 21 runtime, freeing its carrier.
4. When one sleep completes, the virtual thread is made runnable, remounts on an available carrier, returns its row, and releases a permit in `finally`.
5. A waiting virtual thread acquires that permit and proceeds. At no point does the simulated database have more than 20 operations.
6. `Future.get` consumes results in submission order. Later futures can already be complete, but one slow early future delays result assembly. A completion-order design could reduce head-of-line waiting if ordering is unnecessary.
7. If `get` is interrupted, try-with-resources closes the owned executor during unwinding under its API behavior, but production code still needs clear cancellation/deadline policy for outstanding tasks and external calls.

Each virtual thread has stack/task state, so 100 is not free. It is substantially lighter than reserving 100 large platform stacks, but captured request objects, thread locals, futures, and queue nodes still consume heap.

## Complexity and performance

Submitting and collecting `n` tasks uses O(n) futures/result references in the example. Each semaphore operation is O(1) algorithmically but can wait. Overall elapsed time is approximately `ceil(n/20) * serviceTime` under the simplified equal-duration model, plus scheduling overhead.

Collection choices:

| Structure | Typical read | Typical update | Iteration model | Capacity |
|---|---:|---:|---|---|
| `ConcurrentHashMap` | expected O(1) | expected O(1) | weakly consistent | unbounded by API |
| `ConcurrentSkipListMap` | expected O(log n) | expected O(log n) | weakly consistent, ordered | unbounded |
| `CopyOnWriteArrayList` | O(1) index | O(n) copy | snapshot | unbounded |
| `ConcurrentLinkedQueue` | O(1) offer/poll | O(1) expected | weakly consistent | unbounded |
| `ArrayBlockingQueue` | O(1) | O(1) | weakly consistent | fixed |

Virtual-thread throughput benefits appear when tasks spend substantial time blocking. For pure computation, more runnable virtual threads increase scheduling overhead without adding cores. Pinning or synchronized native libraries can reduce carrier availability; JFR and load tests should verify.

## Edge cases and common mistakes

- Composing individually thread-safe map calls into a racy compound operation.
- Expecting a concurrent iterator to produce an atomic whole-map snapshot.
- Calling `size()` repeatedly on `ConcurrentLinkedQueue` for admission.
- Using `CopyOnWriteArrayList` for frequent writes or large lists.
- Treating default `LinkedBlockingQueue` or priority/delay queues as safely bounded.
- Performing slow recursive work inside `ConcurrentHashMap.compute`.
- Pooling virtual threads instead of limiting the real resource.
- Assuming virtual threads make database connections or CPU unlimited.
- Holding a Java 21 monitor across slow blocking I/O and ignoring pinning.
- Carrying large thread-local caches into millions of virtual threads.
- Assuming virtual threads remove the need for interruption, deadlines, or shutdown.
- Using preview structured-concurrency/scoped-value APIs without labeling and enabling preview.

## Production engineering notes

Choose a collection with an explicit consistency statement. If a dashboard may tolerate a weakly consistent traversal, document it. If billing requires a snapshot, build an immutable version, take a lock, use a database snapshot, or design an event log; do not infer snapshot semantics from "concurrent."

Incident example: a service migrates from a 100-thread JDBC executor to virtual-thread-per-request. Throughput rises briefly, then database latency and connection wait explode because 8,000 requests concurrently reach the 100-connection pool. Virtual threads made waiting cheap but did not make the database faster. Add admission aligned with connection/query capacity, propagate request deadlines, shed overload, and measure semaphore/connection wait separately from query time.

For Java 21 pinning, JFR can record virtual-thread pinning events, and HotSpot offers version-specific diagnostics such as pinned-thread tracing properties. Capture stack context and duration; prioritize blocking while pinned, not every short monitor. JDK 24's JEP 491 removes nearly all monitor pinning and changes the diagnostic picture, so confirm the exact JDK before applying a Java 21 runbook.

Monitor virtual-thread count, runnable/parked distribution, carrier utilization, task latency, downstream permit/connection waits, rejections, memory per task, and thread-local growth. A massive thread dump is hard to read; JFR and aggregate tooling are designed to group virtual-thread behavior.

## Interview questions and model answers

**Is `ConcurrentHashMap` enough for `containsKey` then `put`?**

No. Each call is safe but the pair is not atomic. Use `putIfAbsent`, `computeIfAbsent`, `compute`, or another operation matching the transition. A multi-key invariant may still require locking or a different state model.

**What does weakly consistent iteration mean?**

Iteration tolerates concurrent modification and does not fail fast. It traverses elements according to the collection's documented consistency and may reflect some concurrent updates, but it is not an atomic snapshot.

**Why are virtual threads useful?**

They make thread-per-task blocking designs scalable by allowing blocked virtual threads to unmount from carrier platform threads. They improve concurrency for waiting-heavy workloads, not CPU parallelism or downstream capacity.

**Should virtual threads be pooled?**

Usually no. They are intended to be cheap per-task threads. Pooling them creates an artificial queue. Bound scarce resources such as connections, CPU-heavy sections, or partner calls directly.

**What is pinning in Java 21?**

A virtual thread can remain attached to a carrier while inside a monitor or native/foreign call. If it blocks then, the carrier cannot run another virtual thread, reducing scalability. Correctness remains, and later JDK behavior must be checked separately.

## Exercises

1. Replace a `get`/`put` cache race with `computeIfAbsent`; specify loader failure behavior.
2. Compare weakly consistent and copy-on-write snapshot iteration in a listener registry.
3. Choose a blocking queue and overload method for a lossless worker, best-effort telemetry, and direct handoff.
4. Add a deadline-aware semaphore acquisition to the worked example.
5. Produce completion-order results instead of submission-order waits.
6. Create a Java 21 pinning demonstration safely, record it with JFR, then shorten the monitor scope.

## Chapter summary

Concurrent collections make documented individual and compound operations safe; they do not grant transactions across arbitrary call sequences. Iteration consistency and capacity are first-class selection criteria. Java 21 virtual threads let blocking tasks use a thread-per-task style by multiplexing execution over carriers, but CPU and downstream resources remain finite. Do not pool virtual threads; bound the scarce resource, monitor task memory, and diagnose Java 21 pinning where blocking occurs under monitors or native calls.

## Revision checklist

- [ ] I can replace racy map compositions with an atomic operation.
- [ ] I distinguish weakly consistent and snapshot iteration.
- [ ] I know capacity/backpressure properties of major concurrent queues.
- [ ] I can explain virtual threads, carriers, mounting, and blocking.
- [ ] I do not equate concurrency with parallelism or downstream capacity.
- [ ] I understand Java 21 pinning and version dependence.
- [ ] I can design a virtual-thread service with explicit bulkheads and deadlines.
