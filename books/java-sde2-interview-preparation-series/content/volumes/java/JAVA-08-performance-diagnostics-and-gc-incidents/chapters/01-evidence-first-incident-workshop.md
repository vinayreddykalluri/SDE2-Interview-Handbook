# Evidence-First Performance and GC Incident Workshop

Performance work begins when a vague complaint becomes a measured symptom. “The JVM is slow” is not a hypothesis. “Checkout p99 rose from 280 ms to 1.8 s at the same request rate after release R, while CPU stayed at 45% and executor queue age increased” is something a team can investigate.

Use this loop:

```text
scope symptom -> preserve timeline -> form competing hypotheses
              -> collect discriminating evidence
              -> mitigate reversibly -> verify -> prevent recurrence
```

The goal is not to use every JVM tool. It is to spend the smallest diagnostic cost that can change the next decision.

## 1. Define the performance contract

Name the metric and population:

- latency: which endpoint/operation, client-side or server-side, and which percentile?
- throughput: accepted, completed, successful, or useful operations per interval?
- CPU: process, container quota, host, or per-thread?
- allocation: bytes per request, bytes per second, or retained bytes?
- GC: pause time, concurrent CPU, frequency, post-GC occupancy, or allocation stalls?
- memory: heap used, committed, RSS, native memory, metaspace, direct buffers, or page cache?

Always add a comparison window and workload context. A p99 from 100 requests and a p99 from ten million requests do not carry the same stability.

### Percentiles need a declared method

The companion uses nearest-rank percentiles for a small exact sample:

```text
sort n observations
rank = ceil(p * n)
answer = sorted[rank - 1]
```

For ten observations, p95 selects rank 10. Production monitoring often uses histograms, sketches, or time-window aggregations with approximation and merge rules. State the method before comparing two systems.

Do not average percentiles from separate hosts. Merge the underlying distributions or compatible histogram/sketch state.

## 2. Preserve a timeline before changing the system

Capture:

- first bad timestamp and duration;
- deploy/config/JDK/traffic/dependency changes;
- request rate, error rate, latency distribution;
- CPU, runnable threads, queue depth/age, saturation;
- allocation, heap occupancy after collection, pause and concurrent-GC time;
- container limits, throttling, host pressure, restarts;
- downstream latency and pool usage.

Changing heap size, collector flags, and thread count before capturing this evidence can erase the differentiating signal.

## 3. Build competing hypotheses

For a latency spike with moderate CPU, plausible hypotheses include:

1. downstream I/O slowed and request threads wait;
2. a lock serializes a hot path;
3. executor queueing increased;
4. connection-pool admission is saturated;
5. safepoint/GC pauses increased;
6. container CPU throttling hides host-side demand;
7. retries multiply work.

Write a discriminating prediction for each:

| Hypothesis | Prediction if true | Evidence |
|---|---|---|
| downstream wait | many similar socket/client frames; dependency latency rises | JFR/thread samples + client metrics |
| lock contention | blocked/parked threads share monitor/lock owner | repeated dumps/JFR lock events |
| queue saturation | queue age/depth rises before latency | executor metrics and task timestamps |
| GC pause | latency aligns with pause/safepoint interval | GC/JFR timeline |
| CPU hot loop | process CPU and one/few stacks dominate | profile/JFR execution samples |
| retry amplification | attempts per request and downstream load rise | tracing/counters |

One observation can support several hypotheses. The next collection should separate them.

## 4. Choose a low-risk tool ladder

Start with ambient metrics and logs, then increase diagnostic cost deliberately.

```text
service metrics/traces/logs
        |
        v
jcmd inventory + short thread snapshots
        |
        v
bounded JFR recording / targeted profile
        |
        v
class histogram / native memory evidence
        |
        v
heap dump or invasive capture with storage/privacy plan
```

Useful `jcmd` categories on supported HotSpot builds include command inventory, VM/version/flags, thread print, class histogram, native-memory tracking when enabled, and JFR control. Exact commands and availability depend on JDK/version/permissions; run `jcmd <pid> help` against the target rather than copying flags blindly.

### Thread dump discipline

One dump is a snapshot. Capture several at a known interval during the symptom. Compare:

- same threads in same frames;
- lock owners and waiters;
- runnable threads repeatedly in one computation;
- pool worker utilization and queue evidence;
- request correlation identifiers if available.

A thread in `RUNNABLE` can be executing Java, native work, or blocked in certain OS operations depending on observation. Thread state names are clues, not final diagnoses.

### JFR discipline

Define duration, settings, disk repository, max size/age, and data-handling policy. Correlate recording time with service clocks. Useful event families include execution samples, allocation, GC, thread park, monitor contention, socket/file I/O, class loading, and virtual-thread events when supported.

JFR can have low overhead with appropriate settings, but “always zero overhead” is false. Measure and bound the chosen configuration.

### Heap-dump discipline

A heap dump can pause or stress a process, create a large sensitive artifact, and consume disk. Before capture, confirm:

- enough local/remote storage;
- access controls and retention/deletion plan;
- whether a representative replica can be removed from traffic;
- whether a histogram/JFR allocation profile can answer first;
- the exact question the dump will answer.

## 5. Benchmark the question, not the implementation you hope wins

JMH handles warmup, measurement iterations, forks, state scope, result consumption, and many compiler interactions. It cannot rescue a benchmark whose workload does not represent the decision.

Write the decision first:

> For payload sizes 32, 1,024, and 65,536 under the target JDK and CPU, compare throughput and allocation of parser A and B using representative valid/invalid distributions. Confirm that outputs are equivalent.

Then control:

- JDK build and flags;
- forks and warmup;
- input generation outside the timed path where appropriate;
- state sharing and thread count;
- result consumption;
- constant inputs and dead-code elimination;
- GC/allocation profilers when relevant;
- confidence/variance, not one best score.

Do not place network, disk, logging, random setup, and parsing in one microbenchmark and call the result parser speed.

### Coordinated omission and load tests

If a load generator waits for each response before scheduling the next request, it can stop sending work during a stall and under-report the latency users would see at the intended arrival rate. Use an arrival model appropriate to the service and record queueing/end-to-end latency.

## 6. Memory: allocation, retention, and native footprint

Use a boundary table:

| Observation | Likely next question |
|---|---|
| high allocation, stable post-GC heap | which paths create short-lived objects? |
| rising post-GC old occupancy | which types/owners retain more each cycle? |
| stable heap, rising RSS | direct/native buffers, thread stacks, code cache, metaspace, allocator fragmentation? |
| rising class count/metaspace after redeploy | which class loader remains reachable? |
| humongous/large-object pressure | payload size/distribution and collector region behavior? |

### Dominator reasoning

A large shallow object is not necessarily the owner. Retained size asks what would become unreachable if an object were removed from the graph. A dominator tree and paths to GC roots help identify ownership.

Common retaining owners:

- unbounded maps/caches;
- listeners/registries never removed;
- thread locals on long-lived workers;
- queued tasks/futures retaining request graphs;
- class loaders retained by parent state;
- direct buffers or native handles with delayed cleanup;
- metrics labels with unbounded cardinality.

The fix is usually an ownership/lifecycle policy: bound, expire, remove, close, cancel, or stop. Increasing heap can lengthen time to failure without removing the owner.

### Bounded cache is a contract, not only a data structure

Define:

- maximum entries or bytes;
- eviction order;
- TTL/refresh semantics;
- behavior under loader/fetch failure;
- concurrency policy;
- metrics with bounded labels;
- whether values hold external resources.

The executable companion uses a tiny access-order `LinkedHashMap` only to verify an explicit maximum and eviction rule. A production cache needs a proven library and a richer policy.

## 7. GC incident reasoning

Collector choice and flags come after workload evidence. Ask:

- Is allocation rate higher, live set larger, or both?
- Does post-GC occupancy return to a stable baseline?
- Are pauses caused by young, mixed/full, evacuation, remark, or allocation stalls on this collector?
- Is the heap too small for live set plus allocation headroom, or is retention unbounded?
- Is concurrent GC consuming CPU needed by application work?
- Are large objects/regions or remembered-set pressure involved?
- Did a JDK/collector/configuration change occur?

Avoid universal slogans such as “more heap always reduces pauses” or “low-pause collector fixes leaks.” A larger heap can reduce frequency but increase footprint/recovery time; a low-pause collector still requires CPU and headroom; no collector makes strongly reachable obsolete data collectible.

### Example: rising full-GC frequency

```text
Evidence A: allocation rate doubled; post-GC occupancy flat
  -> allocation pressure/workload change likely

Evidence B: allocation stable; post-GC occupancy climbs each hour
  -> retention/leak hypothesis stronger

Evidence C: heap stable; RSS climbs; direct buffer count rises
  -> investigate native/direct ownership
```

Each branch requests different evidence and mitigation.

## 8. Reversible mitigation

A safe incident action states:

- mechanism it targets;
- expected metric movement;
- blast radius;
- rollback trigger and command;
- owner and observation window.

Examples:

- shed an expensive optional endpoint to reduce queue age;
- disable a newly added high-cardinality metric;
- cap/clear a cache only if rebuild/load impact is understood;
- remove one unhealthy replica for dump capture;
- roll back the release that introduced retention;
- temporarily scale when demand exceeds proven capacity, while continuing root-cause work.

“Restart every hour” can restore service as an emergency containment, but it must remain labeled as containment and preserve enough pre-restart evidence.

## Executable evidence companion

`PerformanceEvidenceChecks.java` demonstrates safe public-API evidence:

1. exact nearest-rank percentile calculation without mutating input;
2. heap/non-heap snapshot sanity through `MemoryMXBean`;
3. a deterministic blocked-thread snapshot;
4. programmatic JFR custom-event recording and readback in a temporary file;
5. bounded access-order retention behavior.

```bash
out=$(mktemp -d)
javac --release 21 -Xlint:all -Werror -d "$out" \
  content/volumes/java/JAVA-08-performance-diagnostics-and-gc-incidents/code/PerformanceEvidenceChecks.java
java -ea -cp "$out" PerformanceEvidenceChecks
```

Expected output:

```text
PASS 5 performance evidence suites
```

The JFR file is created under the platform temporary directory and deleted in `finally`. The program does not assert exact memory sizes, GC counts, compilation decisions, or timing thresholds because those would be environment-dependent.

## Interview room: worked incident answers

### CPU is 100%. What do you do first?

**Model answer:** Confirm scope—process versus container quota/host—and timestamp. Check request rate and throttling. Capture a bounded CPU profile or JFR execution samples and thread snapshots during the symptom. Separate application hot code, spin/lock contention, GC/concurrent work, compilation, and native activity. I do not increase thread count before knowing whether work is CPU-bound.

### Heap usage rises. Is that a leak?

**Model answer:** Not by itself. Heap commonly rises between collections. I inspect post-GC occupancy/live-set trend, allocation rate, class histogram/retained owners, workload, and cache lifecycle. A leak means unintended retention beyond useful lifetime, not simply high used bytes before a collection.

### Why is one heap dump insufficient?

**Model answer:** It can show a retention graph at one time, but growth and causality often require comparison, allocation evidence, or known workload context. I still can identify dominant owners and GC-root paths, but I avoid inferring growth rate from one snapshot.

### When would you take a heap dump in production?

**Model answer:** When the retention question justifies the pause, I/O, disk, and sensitive-data risk; preferably on a representative replica removed from traffic. I confirm storage and access policy and try lower-cost histogram/JFR evidence first.

### How do you know a benchmark result is trustworthy?

**Model answer:** The benchmark models a stated decision, validates equivalent outputs, controls environment/state, uses forks/warmup/measurement appropriately, consumes results, reports variance, and is reproducible. JMH addresses harness mechanics, but representative inputs and interpretation remain my responsibility.

### Do low GC pauses mean GC is healthy?

**Model answer:** Not alone. Concurrent collector CPU, allocation stalls, throughput loss, post-GC occupancy, cycle frequency, and headroom also matter. A service can meet pause targets while losing throughput or approaching exhaustion.

### What is the first response to suspected native memory growth?

**Model answer:** Separate heap from RSS and identify likely native categories: metaspace/class loaders, direct buffers, thread stacks/count, code cache, JNI/native libraries, allocator behavior. Use native-memory tracking if it was enabled appropriately, OS maps/metrics, buffer/thread/class counts, and lifecycle evidence.

### Why not tune flags during the first ten minutes?

**Model answer:** A flag change can alter several signals and erase the baseline. I first stabilize impact and preserve enough evidence to identify the mechanism. Any tuning mitigation then has an expected outcome, controlled rollout, and rollback.

## Exercises

1. **Foundation:** Rewrite “the service is slow” as a metric, population, baseline, and time window.
2. **Interview Core:** Build three competing hypotheses for high p99 with moderate CPU and name one discriminating observation each.
3. **Debugging:** Review a benchmark with no forks, constant input, discarded result, and one 20-ms measurement.
4. **Interview Core:** Distinguish allocation pressure from retained-growth evidence using post-GC occupancy.
5. **SDE-2 Follow-up:** Plan a production heap dump with traffic, disk, privacy, and rollback controls.
6. **SDE-2 Follow-up:** Diagnose rising RSS with flat Java heap.
7. **Production:** Write a reversible mitigation for an executor queue-age incident.

## Worked solutions

1. Example: “For successful `POST /checkout` calls in region A, server-side p99 over five-minute windows rose from a 250–320 ms seven-day same-hour baseline to 1.8 s beginning 14:05 UTC; request rate remained 900–1,000/s.”
2. Downstream wait predicts socket/client frames plus dependency latency; lock contention predicts common owners/waiters; queueing predicts queue age rising before end-to-end latency. Collect the least costly timeline that distinguishes them.
3. Use JMH, multiple forks and warmup/measurement iterations, varied representative inputs in proper state scope, result consumption, output correctness checks, and variance. First state which real decision the benchmark models.
4. High allocation with stable post-GC occupancy suggests churn; climbing post-GC occupancy suggests growing live retention. Correlate with workload and retained-owner evidence before calling either a leak.
5. Prefer a replica removed from traffic, verify free disk and artifact size, restrict access, record JDK/process/timestamp, define encrypted transfer and deletion, capture lower-cost evidence first, and abort if pause/disk pressure crosses a bound.
6. Inspect direct buffers, thread count/stack reservation, metaspace/class count/loaders, code cache, JNI/native allocations, and container/OS accounting. Heap flags alone do not explain native footprint.
7. Target mechanism: queue exceeds deadline because arrival burst exceeds service. Temporarily shed low-priority work or reduce upstream admission; expect queue age/p99 to fall; roll back if error budget/critical throughput worsens; keep executor and downstream metrics to validate.

## Final checklist

- I turn symptoms into scoped metrics and timelines.
- I maintain competing hypotheses and collect discriminating evidence.
- I understand the cost and limits of thread dumps, JFR, histograms, and heap dumps.
- I distinguish allocation, retention, heap, and native footprint.
- I treat GC as workload plus live-set plus collector behavior, not a flag contest.
- I design benchmarks around a decision and validate outputs.
- Every mitigation has a mechanism, expected signal, blast radius, and rollback.
