# Chapter 9: Garbage Collection

## Learning objectives

- Explain reachability, GC roots, tracing, reclamation, and relocation.
- Use strong, soft, weak, and phantom references with accurate expectations.
- Compare marking, sweeping, compaction, copying, and generational collection.
- Describe Serial, Parallel, G1, ZGC, and Shenandoah at an engineering level.
- Read basic GC evidence, explain leaks in managed languages, and tune from goals and measurements.

## Why this matters at SDE-2

Garbage collection removes manual deallocation but not capacity engineering. An SDE-2 must distinguish allocation pressure from retention, throughput from tail latency, a young pause from a full-heap event, and Java heap exhaustion from native or metadata exhaustion. The same knowledge prevents harmful fixes such as forcing `System.gc()`, caching with soft references, or increasing heap until container OOM kills replace useful Java errors.

GC is an interview favorite because it connects graphs, runtime architecture, performance, references, and operations. Strong answers describe goals and trade-offs without presenting collector folklore as specification.

## First-principles model

Memory can be reclaimed when no future legal execution can observe an object. Tracing collectors approximate this condition through reachability from roots known to the runtime.

```text
GC roots
  +-- active thread frames ------> Request -> byte[]
  +-- static/class state --------> Cache -> Entry
  +-- JNI/runtime handles -------> NativePeer

unreachable cycle:
  Node A -> Node B -> Node A       no path from any root, collectible
```

Reference counting alone cannot collect the unreachable cycle. A tracing collector starts at roots, follows references, marks or copies what is reachable, and treats the rest as reclaimable, subject to special reference processing and finalization-related rules.

> **Specification boundary:** Java specifies reachability categories, reference APIs, finalization semantics where retained for compatibility, and observable requirements such as not reclaiming strongly reachable objects. It does not require generations, Eden, survivor spaces, specific pause names, or a particular collector algorithm.

## Core terminology

- **GC root:** Runtime starting point for reachability analysis, such as selected thread/frame, class, JNI, and VM references.
- **Live set:** Objects considered live under the collector/reference-processing rules at a point in time.
- **Strong reference:** Ordinary reference that keeps an object strongly reachable.
- **Soft reference:** Reference intended for memory-sensitive caches, cleared at GC's discretion under specified policy constraints.
- **Weak reference:** Does not keep a weakly reachable object alive; commonly used with a `ReferenceQueue`.
- **Phantom reference:** Post-mortem notification/cleanup coordination mechanism, always observed through `get()` as `null`.
- **Mark-sweep:** Mark reachable objects, then reclaim unmarked storage.
- **Compaction:** Move live objects to reduce fragmentation and update references.
- **Copying:** Move live objects from one region to another, reclaiming the source wholesale.
- **Generational hypothesis:** Most newly allocated objects die young, while survivors are more likely to remain.
- **Pause:** Interval when selected application threads cannot execute.

## Detailed mechanics

Root discovery uses runtime knowledge of where references reside. In compiled code and frames, precise maps can identify reference locations. Static state associated with live classes, active thread locals, synchronization/runtime structures, and JNI handles can participate. A local that appears in source may cease to be a root after its last use under allowed optimization.

Strong references are the default. Weak references support canonical maps and metadata associations when combined with careful cleanup. `WeakHashMap` weakens keys, not values; if a value strongly references its key, the entry can remain reachable through the map value path. Soft references are not deterministic cache eviction and can cause unstable latency. Bounded caches with explicit policy are usually better. Phantom references plus a `ReferenceQueue` and `Cleaner`-style mechanisms can coordinate cleanup of non-memory resources, but explicit `AutoCloseable` ownership is primary.

Mark-sweep avoids moving live objects but can fragment free memory. Mark-compact pays relocation cost to create contiguous free space. Copying cost is proportional mainly to survivors and gives cheap bulk reclamation, which suits young regions with high mortality. Concurrent collectors perform substantial tracing or relocation while application threads run, using barriers and additional metadata to preserve correctness.

Generational collectors classify storage by object age or regions serving young/old roles. A common young layout has Eden for new allocation and survivor spaces for copied survivors. Objects surviving enough collections, or meeting other conditions, move to an old region. References from old to young require remembered sets/card tables so a young collection need not scan the entire old population.

"Minor," "major," and "full" GC are widely used but not rigorously portable terms. Minor commonly means young-only. Major sometimes means old-generation work, but log/tool usage varies. Full commonly means a broad stop-the-world collection of much or all heap, often with compaction, but exact causes and algorithms are collector-specific. Use actual event names and scope from the deployed JVM.

Collector overview for modern HotSpot deployments:

| Collector | Primary orientation | Important trade-off |
|---|---|---|
| Serial GC | Simple single-worker collection, small heaps | Longer pauses as heap/work grows |
| Parallel GC | Throughput using parallel stop-the-world workers | Pause-time predictability is secondary |
| G1 GC | Region-based generational collector with pause targets | Remembered-set/concurrent-cycle overhead; targets are goals |
| ZGC | Very low pauses through concurrent work and barriers | Additional concurrent CPU/metadata; release capabilities vary |
| Shenandoah | Low pauses through concurrent evacuation/compaction | Availability and generational mode depend on distribution/release |

G1 divides the heap into regions rather than fixed contiguous young/old blocks. Young collections evacuate live objects; concurrent marking identifies old live data; mixed collections can include selected old regions. Humongous objects receive special region handling. A pause target guides heuristics but is not a deadline guarantee.

ZGC uses colored/metadata-bearing reference techniques, load barriers, and concurrent phases to keep pauses largely independent of heap size. Modern releases have evolved, including generational capabilities in relevant versions. Shenandoah also performs concurrent evacuation with barriers. Do not transfer flags or mental models between releases without checking documentation and logs.

## Worked Java example

```java
import java.lang.ref.ReferenceQueue;
import java.lang.ref.WeakReference;

public final class ReferenceDemo {
    static final class TrackedReference extends WeakReference<Object> {
        final String label;

        TrackedReference(Object value, ReferenceQueue<Object> queue, String label) {
            super(value, queue);
            this.label = label;
        }
    }

    public static void main(String[] args) throws Exception {
        ReferenceQueue<Object> queue = new ReferenceQueue<>();
        Object strong = new byte[1_000_000];
        TrackedReference weak = new TrackedReference(strong, queue, "payload");

        System.out.println(weak.get() != null); // true here
        strong = null;

        for (int i = 0; i < 100 && queue.poll() == null; i++) {
            System.gc();
            Thread.sleep(10);
        }
        System.out.println(weak.get());
    }
}
```

The first line is predictably `true` because `strong` still holds the object. The final line is not a portable timing test: `System.gc()` is only a request, collection need not happen immediately, and weak-reference clearing/enqueueing timing is nondeterministic.

## Execution or memory walkthrough

Initially:

```text
main frame --strong----------------------> byte[]
          --weak--> TrackedReference -weak-> byte[]
          --queue--> ReferenceQueue
```

After `strong = null`, the byte array has only the weak edge. At an appropriate GC cycle:

1. Roots are scanned and the `TrackedReference` plus queue are strongly reachable.
2. The byte array is not strongly or softly reachable through the shown graph.
3. Weak-reference processing clears the referent according to the reference rules.
4. The reference is registered/enqueued for consumer observation, with timing depending on processing.
5. The byte-array storage can eventually be reclaimed or its region reused.

The `TrackedReference` itself is not automatically removed; the program must drain the queue and clean associated metadata. A reference object and its referent are distinct objects.

For ordinary young allocation, a common HotSpot generational dry run is:

```text
allocate in Eden -> Eden fills -> STW young collection
  dead objects: not copied
  survivors: copied to survivor/old regions, age updated
  references: adjusted
resume application
```

This is a collector model, not a universal Java execution requirement.

## Complexity and performance

Tracing cost relates to roots, live graph, remembered-set work, and changed-reference tracking, not simply maximum heap. Copying cost is driven by survivors. Sweep cost can involve address-space/region metadata. Concurrent work consumes CPU and memory bandwidth while the application runs.

Key workload measurements are allocation rate, promotion rate, live-set size after a sufficiently complete collection, pause distribution, concurrent-cycle duration, GC CPU, and headroom. A larger heap can reduce collection frequency but increase footprint and sometimes cycle/pause work. A smaller heap may improve locality but collect too often or fail under bursts.

Latency and throughput are different goals. Parallel GC can maximize application throughput with larger pauses. Low-pause collectors shift work concurrent with the application and use barriers, often buying tail-latency control with CPU and complexity. Collector choice must follow an SLO and measured workload.

## Edge cases and common mistakes

- Saying reference cycles leak automatically. Tracing collectors collect unreachable cycles.
- Treating any high allocation rate as a leak.
- Assuming GC means resources such as sockets are closed promptly.
- Using finalization for correctness; finalization is deprecated for removal and nondeterministic.
- Assuming `System.gc()` forces an immediate full collection.
- Using soft references as the only cache bound.
- Forgetting thread locals, listeners, static maps, queues, and class loaders are common retention paths.
- Calling every old-generation event a full GC.
- Choosing a collector from generic benchmark claims without application SLO evidence.
- Ignoring non-heap OOME causes and container-level kills.

Memory leaks exist whenever reachable objects are no longer useful. Examples include an unbounded cache, completed requests retained in a queue, forgotten listener registrations, stale thread-local values on pool threads, and class loaders retained after redeployment.

`OutOfMemoryError` is a family of failures. Messages can indicate Java heap, GC overhead, array-size limits, Metaspace, direct buffer memory, or native thread creation. The JVM may be severely resource constrained; error-handling code requiring more allocation may fail too.

## Production engineering notes

Start with service goals and evidence, not flags:

1. Confirm the exact JDK, collector, heap/container sizing, and recent changes.
2. Capture GC logs/JFR and OS/container memory/CPU data.
3. Determine whether the problem is allocation rate, retained live set, fragmentation, insufficient headroom, concurrent-cycle lateness, or non-heap memory.
4. Fix retention or allocation behavior when code is the cause.
5. Adjust heap/collector/limited options one variable at a time under representative load.
6. Validate throughput, p95/p99/p999 latency, CPU, memory, and failure recovery.

Unified GC logging in HotSpot can expose event type, phases, causes, region occupancy, and pause time. Use a tested rotation/retention plan because logs are incident evidence. A heap dump may contain secrets and can require substantial disk and pause/CPU; secure it and rehearse acquisition.

> **HotSpot note:** Collector defaults, supported collectors, flags, log tags, reference-processing policy, and `System.gc()` handling vary by JDK distribution and release. Confirm the running process with supported commands instead of trusting launch documentation alone.

## Interview questions and model answers

**How does GC decide an object is garbage?**

A tracing collector starts from JVM-known roots and follows references under Java's reachability rules. Objects with no applicable root path are candidates for reclamation, with special processing for soft, weak, phantom, and legacy finalization states.

**Can Java have memory leaks?**

Yes. GC removes unreachable objects, not reachable objects the application no longer needs. Unbounded caches, thread locals, listeners, queues, and class loaders can retain useless graphs.

**Compare G1 and ZGC.**

G1 is a region-based generational collector balancing throughput and pause goals with evacuation and concurrent marking. ZGC moves much more work, including relocation, concurrently to target very low pauses using barriers and metadata techniques. Exact generational capabilities and trade-offs depend on JDK version; I would choose using SLOs and load tests.

**What is stop-the-world?**

It means relevant application threads are paused for a VM operation. Young collections and short phases of concurrent collectors can be STW. It does not necessarily mean full GC, and not every STW operation is GC.

**How would you investigate frequent GC?**

First classify events and inspect allocation rate, live set, promotion, pause and concurrent-cycle timing, heap occupancy, and CPU. Then determine whether traffic, object churn, retention, heap sizing, or collector inability to keep up is causal before tuning.

## Exercises

1. Draw root paths for a static cache, a thread-local leak, and an unreachable cycle.
2. Implement weak-key metadata cleanup using `ReferenceQueue`; explain why polling is required.
3. Given GC-log excerpts, classify young pauses, concurrent phases, and broad full-heap events using their actual labels.
4. Design a load test comparing throughput and p99 latency under two collectors.
5. Explain why a 2 GiB heap in a 2 GiB container is unsafe.
6. Create a bounded local heap experiment, capture a histogram, and distinguish allocation from retention.

## Chapter summary

Garbage collection traces from roots and reclaims objects that can no longer participate under Java's reference rules. Marking, sweeping, copying, compaction, generations, regions, and concurrency are implementation strategies with different CPU, memory, throughput, and pause trade-offs. Managed memory still leaks when useless objects remain reachable. Successful operations begin with workload goals, GC evidence, whole-process memory, and code-level causes before flags.

## Revision checklist

- [ ] I can explain tracing reachability and why cycles can be collected.
- [ ] I know strong, soft, weak, and phantom reference roles and limitations.
- [ ] I can compare sweep, compact, copy, and concurrent work.
- [ ] I can explain generations, remembered sets, and promotion.
- [ ] I can compare Serial, Parallel, G1, ZGC, and Shenandoah without overclaiming.
- [ ] I can distinguish allocation pressure, retention leaks, and non-heap failures.
- [ ] I can propose an evidence-driven GC investigation and tuning loop.

