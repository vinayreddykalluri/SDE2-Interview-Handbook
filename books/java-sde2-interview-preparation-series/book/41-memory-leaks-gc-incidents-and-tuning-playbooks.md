# 41. Memory Leaks, GC Incidents, and Tuning Playbooks

## Learning objectives

By the end of this chapter, you should be able to:

- distinguish high allocation, intended live data, heap retention leaks, native growth, and resource leaks;
- read heap occupancy, allocation, pause, CPU, promotion, and process-memory signals together;
- investigate a retention path from GC root to dominated objects;
- execute a staged memory or GC incident playbook without destroying evidence;
- tune only after defining latency, throughput, footprint, and headroom goals; and
- prevent common leaks through bounded ownership, lifecycle APIs, and observability.

## Why this matters at SDE-2

An out-of-memory failure is an endpoint, not a root cause. A growing cache, an overloaded queue, a class-loader leak, direct-buffer growth, too many threads, or an undersized container can all end in memory failure and require different fixes. Increasing `-Xmx` can buy recovery time, hide an unbounded invariant, or make pauses and heap dumps larger.

SDE-2 engineers are expected to stabilize service, classify the memory domain, preserve evidence, and separate allocation pressure from unwanted retention. They must also resist tuning folklore. A collector flag is not a substitute for controlling cardinality, closing resources, removing listeners, bounding concurrency, and respecting the process memory limit.

## First-principles model

Garbage collection reclaims objects that are not reachable from GC roots. A heap leak in a managed runtime is therefore unwanted reachability, not forgotten manual deallocation:

```text
GC root -> long-lived owner -> collection -> entry -> large object graph
```

The key quantities are:

```text
allocation rate = bytes allocated per unit time
live set        = bytes still reachable after an effective collection
headroom        = usable memory above live set for allocation and GC work
```

High allocation with a stable post-collection baseline is pressure, not necessarily a leak. A rising post-collection baseline under comparable load suggests increasing live data. Stable Java heap with rising process resident memory suggests a non-heap or native domain.

The investigation loop is:

```text
stabilize -> classify memory domain -> preserve timeline
          -> compare repeated evidence -> find owner/path
          -> fix lifecycle or capacity -> load-test -> canary -> monitor
```

> **Specification boundary:** Java specifies reachability concepts and reference types, not a collector algorithm, generation layout, region size, pause target, object age threshold, or out-of-memory message. Collector behavior and diagnostic counters are JVM/vendor/version specific.

## Core terminology

- **GC root:** Starting point for reachability, such as selected thread, static, class-loader, JNI, or VM references.
- **Live set:** Objects that remain reachable after collection under the chosen observation.
- **Shallow size:** Memory occupied directly by one object.
- **Retained size:** Memory that could become reclaimable if an object were removed, under dominator analysis.
- **Dominator:** Object through which every root path to another object passes.
- **Allocation pressure:** High creation rate that forces frequent collection even if objects die quickly.
- **Promotion:** Movement or classification of surviving objects into longer-lived storage in a generational collector.
- **Fragmentation:** Free memory exists but not in a form suitable for requested allocation or collector progress.
- **Humongous/large object:** Collector-specific category for unusually large allocation.
- **Metaspace:** HotSpot native memory commonly used for class metadata.
- **Direct memory:** Native storage used by direct byte buffers and related I/O.
- **Leak slope:** Rate at which a stable-comparison memory baseline grows.
- **Backlog:** Work retained because production exceeds consumption.

## Detailed mechanics

### Classify before collecting expensive evidence

Start with container or host memory, Java heap used/committed/max, post-GC occupancy, allocation rate, GC pause and CPU, thread count, loaded classes, direct-buffer pool metrics, file descriptors, and queue/cache cardinality. Align them with traffic and deployment changes.

Useful patterns include:

- high allocation, frequent collections, stable baseline: allocation pressure;
- rising post-collection old/live occupancy: growing retained set;
- heap near max, low reclaim, repeated long collections: exhaustion or oversized live set;
- stable heap, rising RSS: direct/native/thread/metaspace/mapping hypothesis;
- rising loaded classes and metaspace after redeploy-like activity: class-loader retention hypothesis;
- queue depth and memory rising together: downstream throughput or backpressure failure.

A single sawtooth graph cannot identify an owner. Compare the same metric under comparable load and collector phase. Concurrent collectors can report occupancy at phases that do not correspond to a stop-the-world full collection.

### Allocation pressure versus retention

Allocation pressure is often caused by repeated parsing, boxing, temporary collections, copying, oversized buffers, logging arguments, or retry amplification. JFR allocation samples and an allocation profiler identify hot allocation stacks. Optimize only allocations that materially affect GC CPU, pause, or capacity.

Retention analysis asks why objects remain reachable. Class histograms taken at comparable lifecycle points show class growth. A heap dump enables dominator trees and paths to roots. Look for the first unexpected long-lived owner, not merely the largest byte array. A million value objects may be correctly owned by one accidental map.

Histogram growth is a lead. Class counts can rise because traffic or legitimate state rises. Heap-dump retained sizes depend on the snapshot and tool model. Confirm the suspected owner in code and with a reproducible lifecycle test.

### Common heap-retention patterns

- unbounded maps, caches, deduplication sets, queues, and retry histories;
- entries with expiration timestamps but no active eviction;
- `ThreadLocal` values not removed on pooled threads;
- listeners, callbacks, observers, and scheduled tasks never deregistered;
- static registries retaining application or class-loader objects;
- keys whose equality mutation prevents normal removal;
- a small view or lambda retaining a large backing object graph;
- completed futures or request contexts retained by diagnostics or metrics labels;
- class loaders retained by threads, drivers, caches, or shutdown hooks;
- weak-reference designs whose values retain keys indirectly.

Weak references are not a general cache policy. Their reclamation follows reachability and collector timing, not a service-level capacity contract. Prefer explicit maximum size, weight, expiry, admission, and observability.

### Non-heap and native growth

Each platform thread consumes native stack and VM metadata; thread explosion can exhaust address space or native allocation before heap fills. Direct buffers use native memory and can be retained by Java references even though their bytes are outside the heap. Metaspace grows with classes and class loaders. JIT code cache, JNI libraries, memory maps, TLS/native libraries, and allocator fragmentation also contribute to RSS.

Correlate thread counts, direct-buffer pools, class loading, HotSpot NMT categories when pre-enabled, OS mappings, and library metrics. NMT does not cover every native allocation and RSS includes shared/resident accounting that does not match NMT totals.

### OutOfMemoryError is a family of symptoms

Messages can point toward Java heap, GC overhead, metaspace, direct-buffer reservation, native-thread creation, or array-size limits. Message wording and triggering policy are implementation-specific. Preserve the exact exception, JVM logs, process limit, heap/native metrics, thread count, and allocation context.

Do not assume catch-and-continue is safe. The process may lack memory to log, allocate a response, or restore invariants. Keep emergency handling minimal, avoid large allocations, shed traffic, and use supervisor restart/failover policy. `-XX:+HeapDumpOnOutOfMemoryError` is a HotSpot option that may aid analysis but needs secure disk capacity and may fail.

### Collector-neutral GC diagnosis

Ask whether GC is the cause of impact or reacting to allocation/load. Correlate pause windows with request latency, GC CPU with application CPU, allocation with traffic, and post-cycle live set with heap capacity. A pause at the same time as latency is evidence; its duration and affected requests establish contribution.

Current HotSpot distributions commonly offer throughput-oriented, region-based, and low-pause collectors. Defaults and availability depend on JDK/vendor/platform. Collector selection changes trade-offs among throughput, pause, footprint, CPU, and headroom. It cannot collect reachable objects or make an unbounded queue bounded.

> **HotSpot note:** G1 is a common default in mainstream 64-bit HotSpot server configurations for Java 17 and 21, but verify `VM.flags` or startup logs. ZGC, Generational ZGC modes, Shenandoah availability, region/humongous thresholds, and logging fields are release and vendor sensitive.

### A safe incident playbook

1. Protect users: shed load, stop nonessential producers, fail over, or restart a replica according to policy.
2. Record timeline, release, traffic, limits, JVM/collector flags, exact OOM or pause symptoms.
3. Classify heap versus native using existing metrics.
4. Capture bounded JFR and GC logs if already available; take repeated histograms at comparable points.
5. Take a heap dump only after disk, pause, replica, permissions, and sensitive-data checks.
6. Analyze dominators and shortest/meaningful paths to roots; map the owner to code and lifecycle.
7. Reproduce with a bounded load test and assert cardinality or baseline recovery.
8. Fix ownership/capacity before tuning, then canary with rollback criteria.

Never invoke full GC solely to make a graph look clean without recording pre-GC evidence. `System.gc()` is a request whose treatment depends on JVM flags and collector; it is not a portable diagnostic barrier.

### Tuning in the right order

First reduce unwanted retention and overload. Next set a realistic process/container budget that includes heap, metaspace, direct/native memory, thread stacks, code cache, agents, and safety margin. Then choose heap size and collector from measured objectives. Finally adjust a small number of documented flags, one hypothesis at a time.

`-Xms`, `-Xmx`, percentage-based container sizing, pause targets, and collector flags are HotSpot/vendor options. Large heaps provide burst headroom but increase footprint and some diagnostic costs. Small heaps collect more frequently. Setting minimum equal to maximum can improve predictability but commits a capacity choice and is not universally best.

## Worked Java example

This event bus makes listener lifetime explicit:

```java
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

record Event(String name) {}

final class RequestContext {
    private final byte[] scratch = new byte[1_000_000];
    private int eventsSeen;

    void onEvent(Event event) {
        scratch[Math.floorMod(eventsSeen++, scratch.length)] =
                (byte) event.name().length();
    }
}

final class EventBus {
    interface Registration extends AutoCloseable {
        @Override void close();
    }

    private final List<Consumer<Event>> listeners = new ArrayList<>();

    Registration subscribe(Consumer<Event> listener) {
        java.util.Objects.requireNonNull(listener);
        listeners.add(listener);
        return new Registration() {
            private boolean closed;

            @Override
            public void close() {
                if (!closed) {
                    listeners.remove(listener);
                    closed = true;
                }
            }
        };
    }

    void publish(Event event) {
        List.copyOf(listeners).forEach(listener -> listener.accept(event));
    }
}

public final class ListenerLifecycleDemo {
    public static void main(String[] args) {
        EventBus bus = new EventBus();
        RequestContext context = new RequestContext();
        try (EventBus.Registration ignored = bus.subscribe(context::onEvent)) {
            bus.publish(new Event("request-complete"));
        }
    }
}
```

The example is single-threaded. A production bus needs a concurrency policy, exception isolation, and documented behavior when a listener removes itself during publication. The lifecycle idea remains: subscription returns an idempotent handle that owners must close.

## Execution or memory walkthrough

During subscription, the method reference refers to `context`. The event bus stores that listener in its list:

```text
live bus -> listeners -> method-reference object -> RequestContext -> 1 MB byte[]
```

If the bus is application-scoped and registration is never removed, the request context remains reachable after the request ends. Repeating this pattern creates an approximately linear retained-set slope. The byte array is not the root cause; the unexpected listener ownership is.

The try-with-resources block closes the registration. `close` removes the same listener reference, breaking the path. After `main` no longer uses `context` and a future collection occurs, the context and byte array are eligible for reclamation. Eligibility does not promise immediate collection or memory return to the operating system.

`List.copyOf` in `publish` creates a snapshot of listener references so a callback cannot directly disrupt that iteration through the original list. It adds allocation and is not thread-safety. Alternative policies have different costs.

## Complexity and performance

For `n` listeners, publication is `O(n)` time and the snapshot is `O(n)` temporary references. Registration append is amortized `O(1)`; removal from an array list is `O(n)`. If registration churn or listener count is high, a different representation may be justified, but it must preserve lifecycle and concurrency semantics.

Memory incident costs scale differently:

| Action | Time/space tendency | Primary risk |
|---|---|---|
| allocation sample | sampled event cost | attribution precision |
| histogram | heap-object traversal/aggregation | pause and shallow-only view |
| heap dump | `O(heap objects + data)` | pause, disk, secrets |
| offline dominator analysis | roughly graph-scale, tool-dependent | analyzer memory and time |
| increasing heap | more headroom | larger footprint, delayed failure |
| bounding a queue/cache | bounded steady storage | eviction/rejection semantics |

An unbounded collection is `O(n)` space where `n` may be lifetime traffic. A capacity limit turns it into `O(capacity)` but forces an explicit behavior at the boundary: block, reject, evict, spill, or degrade.

## Edge cases and common mistakes

- Calling any high memory usage a leak without comparing post-collection baselines.
- Looking only at heap while process RSS or native threads grow.
- Treating a large class histogram row as the retaining owner.
- Taking one heap dump after traffic changed and inferring a slope.
- Increasing heap repeatedly while leaving cache or queue cardinality unbounded.
- Forcing full GC during the incident before preserving allocation and occupancy evidence.
- Assuming weak keys solve retention when values reference keys or cleanup lags.
- Forgetting `ThreadLocal.remove` on pooled threads.
- Adding eviction without defining what happens to correctness on eviction.
- Tuning a pause target without measuring throughput, CPU, and headroom effects.
- Using old collector flags that are ignored, deprecated, or removed on the target JDK.
- Ignoring diagnostic artifact sensitivity and disk consumption.
- Expecting memory to return immediately to the OS after objects become unreachable.
- Catching `OutOfMemoryError` and continuing normal request processing.

## Production engineering notes

Put bounds and ownership on every long-lived collection, executor queue, cache, registry, and per-tenant metric label. Emit current size, maximum, eviction/rejection, oldest age, and key cardinality. Expiration needs an active maintenance policy and tests using controllable time.

Close registrations, scheduled tasks, streams, class-loader-owned executors, and thread-local state at lifecycle boundaries. Use load tests that repeat create/use/destroy cycles and assert that post-cycle cardinality or heap baselines stabilize. A one-request unit test will not reveal lifetime leaks.

Maintain memory margin below the container limit. Account for heap plus non-heap components and traffic bursts. Monitor both JVM and cgroup/host views. Configure OOM handling, secure diagnostic paths, restart policy, and traffic failover before failure.

Keep GC logging/JFR settings and parsers compatible with each supported JDK. Compare collector changes with the same workload and objectives. Roll out canaries and retain rollback capacity; a lower pause percentile may cost enough CPU to reduce total service capacity.

## Interview questions and model answers

**How can Java have a memory leak with garbage collection?**

The collector removes unreachable objects. A leak occurs when objects remain reachable through an unintended long-lived path, such as an unbounded cache, listener registry, thread local, or class loader.

**How do you distinguish high allocation from a leak?**

High allocation shows rapid object creation and frequent collection, but post-collection live occupancy stabilizes. A retention leak tends to show a rising comparable post-collection baseline and growing owners across repeated histograms or dumps.

**What would you inspect in a heap dump?**

Dominators, retained size, growing classes, and paths from suspect objects to GC roots. I seek the first unexpected owner and verify its lifecycle in code rather than blaming the largest leaf array.

**Why might RSS grow while Java heap is flat?**

Thread stacks, direct buffers, metaspace/class loaders, code cache, JNI or native libraries, mappings, or allocator behavior. Correlate NMT where enabled with OS maps and subsystem metrics.

**Should you increase `-Xmx` during an incident?**

It can provide temporary headroom if process limits allow, but may delay an unbounded failure and increase footprint or diagnostic cost. Preserve evidence, define the budget, and fix ownership or capacity when that is the cause.

**How do you tune GC?**

Define pause, throughput, footprint, and CPU goals; fix retention and overload; size the whole process; establish a baseline; then change one supported setting or collector and test representative load before canarying.

## Exercises

1. Draw the root path for a pooled thread retaining a `ThreadLocal` request buffer. Show the correct `try/finally` cleanup.
2. Given stable heap, growing RSS, and increasing thread count, design an evidence and mitigation plan.
3. Design bounds for a retry queue, including rejection, durability, metrics, and tenant fairness.
4. Extend the event bus with thread safety without holding a lock while callbacks execute. State snapshot and removal semantics.
5. Compare two histograms five minutes apart and list reasons class growth might be legitimate.
6. Build a canary plan for changing collectors, including latency, throughput, CPU, footprint, and rollback thresholds.

## Chapter summary

Managed-memory leaks are unwanted reachability. Diagnose them by distinguishing allocation rate, live-set growth, and non-heap process memory, then following retention paths to an unexpected owner. Stabilize service and preserve low-cost evidence before invasive dumps. Bounded collections and explicit lifecycle handles prevent more failures than collector flags. GC tuning begins only after workload, ownership, whole-process memory, and objectives are understood, and every result is specific to the tested JVM and deployment.

## Revision checklist

- [ ] I distinguish allocation pressure, intended live data, heap leak, native growth, and resource leak.
- [ ] I correlate post-collection occupancy, allocation, GC CPU/pause, RSS, threads, classes, and queues.
- [ ] I understand shallow size, retained size, dominators, roots, and comparable snapshots.
- [ ] I can execute a staged incident playbook with safe dump criteria.
- [ ] I can identify listener, thread-local, cache, queue, key, and class-loader retention patterns.
- [ ] I budget heap and native components below process/container limits.
- [ ] I tune one measured hypothesis at a time and validate with representative load.
- [ ] I treat collector behavior, flags, OOM messages, and counters as vendor/version specific.
