# Appendix C - JVM Tools, Flags, and Incident Commands

This appendix is a field checklist for OpenJDK HotSpot-style deployments. Tool availability, exact output, attach permissions, overhead, container behavior, and flags vary by release and distribution. Treat every command as version-specific and rehearse it in a safe environment. Prefer evidence collection with understood overhead over speculative tuning.

## Identify the runtime before interpreting it

Capture the executable, release, vendor, process arguments, and container limits:

```text
java -version
java -XshowSettings:vm -version
jcmd <pid> VM.version
jcmd <pid> VM.command_line
jcmd <pid> VM.flags
jcmd <pid> VM.system_properties
jcmd <pid> VM.info
```

Also record:

- wall-clock time, timezone, host/pod identity, deployment version, and incident interval;
- CPU quota and throttling, memory limit, resident memory, swap policy, and OOM-kill evidence;
- request rate, concurrency, error rate, latency percentiles, dependency health, and recent changes;
- heap sizing, collector, thread count, file descriptors, connection-pool limits, and native-library versions.

Without runtime identity and workload context, two similar-looking tool outputs can imply different mechanisms.

## Evidence-first incident sequence

A conservative sequence is:

1. Establish user-visible symptoms and exact time bounds.
2. Correlate application and infrastructure metrics before attaching a tool.
3. Record runtime identity, arguments, limits, and recent deployments.
4. Collect several lightweight thread and heap summaries rather than one isolated sample.
5. Start a bounded JFR recording if its overhead is acceptable and existing continuous recordings are unavailable.
6. Escalate to heap dumps or more invasive diagnostics only with disk, pause, privacy, and operational risks understood.
7. Preserve raw artifacts and command lines; analyze copies.
8. Change one causal control at a time, define success/rollback criteria, and keep monitoring guardrails.

Restarting may restore service but destroys in-process evidence. When safety permits, collect the smallest decisive artifact first.

## jcmd command map

List local JVMs and supported diagnostic commands:

```text
jcmd -l
jcmd <pid> help
jcmd <pid> help <command>
```

Common commands:

| Goal | Representative command | Interpretation note |
|---|---|---|
| thread dump | `jcmd <pid> Thread.print -l` | take several samples; one blocked stack is not a trend |
| class histogram | `jcmd <pid> GC.class_histogram` | count/bytes are shallow; command may affect the process |
| heap overview | `jcmd <pid> GC.heap_info` | collector-specific summary, not a leak proof |
| heap dump | `jcmd <pid> GC.heap_dump filename=/safe/path/heap.hprof` | large and potentially sensitive; check command help/version |
| class-loader stats | `jcmd <pid> VM.classloader_stats` | useful for loader/metaspace growth |
| native memory | `jcmd <pid> VM.native_memory summary` | requires NMT enabled at startup |
| compiler state | `jcmd <pid> Compiler.codecache` | exact command set varies |
| system counters | `jcmd <pid> PerfCounter.print` | implementation counters need release context |
| start JFR | `jcmd <pid> JFR.start name=incident settings=profile duration=5m filename=/safe/path/incident.jfr` | quote/adjust syntax for shell and release |
| inspect recordings | `jcmd <pid> JFR.check` | lists active recordings |
| dump JFR | `jcmd <pid> JFR.dump name=incident filename=/safe/path/snapshot.jfr` | preserves current data |
| stop JFR | `jcmd <pid> JFR.stop name=incident` | optionally specify output per command help |

Some diagnostic commands are marked with impact levels in `jcmd help`. Read them. A production-safe action depends on heap size, allocation rate, storage, pause objectives, and incident severity.

## Thread diagnosis

Collect repeated dumps several seconds apart:

```text
jcmd <pid> Thread.print -l > threads-01.txt
# wait while preserving the incident interval
jcmd <pid> Thread.print -l > threads-02.txt
jcmd <pid> Thread.print -l > threads-03.txt
```

`jstack -l <pid>` is a familiar alternative when available; `jcmd` is generally the preferred HotSpot diagnostic entry point.

Look for:

- the same runnable stacks consuming samples, correlated with process/thread CPU;
- many threads waiting for one monitor, lock, connection pool, queue, or future;
- deadlock reports and an actual resource cycle;
- executor queues growing while workers block downstream;
- repeated socket/database waits aligned with dependency latency;
- virtual-thread pinning or carrier scarcity only after confirming release-specific evidence;
- shutdown threads waiting on tasks that ignore interruption.

Thread state names are snapshots. `RUNNABLE` can include native I/O or code not currently executing on a CPU. `WAITING` is not automatically a problem. Ask whether the state persists and whether it explains the service symptom.

## Java Flight Recorder

JFR records time-correlated JVM and application events with configurable detail. Prefer a continuous, bounded recording established before the incident when organizational policy allows it.

Command-line startup examples:

```text
java -XX:StartFlightRecording=filename=service.jfr,dumponexit=true,maxsize=512m,maxage=2h ...
java -XX:FlightRecorderOptions=stackdepth=128 ...
```

Attach example:

```text
jcmd <pid> JFR.start name=incident settings=profile delay=10s duration=5m filename=/safe/path/incident.jfr
```

Inspect in JDK Mission Control or with the `jfr` command where available:

```text
jfr summary incident.jfr
jfr print --events jdk.CPULoad,jdk.GarbageCollection incident.jfr
```

Correlate events instead of ranking isolated hot methods. Useful relationships include allocation pressure -> GC work -> pause or concurrent CPU -> request latency; monitor contention -> blocked duration -> endpoint; socket reads -> remote host -> pool occupancy; and compilation/deoptimization -> phase change -> latency.

Recording settings and event names evolve. A profile recording can be more expensive than the default configuration. Measure overhead for the selected settings and workload.

## GC and safepoint logging

Unified logging is the modern mechanism:

```text
-Xlog:gc*:file=/safe/path/gc.log:time,uptime,level,tags:filecount=10,filesize=50m
-Xlog:safepoint:file=/safe/path/safepoint.log:time,uptime,level,tags
-Xlog:gc+heap=debug,gc+age=trace:file=/safe/path/gc-detail.log:time,uptime,tags
```

Validate selectors for the exact JDK:

```text
java -Xlog:help
```

Questions to answer from GC evidence:

- Is allocation rate rising, or is the retained live set rising?
- Does old occupancy return to a stable baseline after a complete relevant cycle?
- Are pauses caused by evacuation work, reference processing, class unloading, humongous allocations, or another phase?
- Is concurrent collector work receiving enough CPU under the container quota?
- Is the heap undersized for the live set and allocation burst, or oversized in a way that harms objectives?
- Do latency spikes align with GC at all?

Never conclude "memory leak" merely because used heap rises between collections. A leak is unintended retained reachability. Use a time series and a retention analysis.

## Heap histograms and dumps

A class histogram is a triage view:

```text
jcmd <pid> GC.class_histogram
```

Compare multiple histograms under similar collection conditions. Shallow bytes do not reveal who retains objects. A large legitimate cache can rank first without being a leak, while a small retaining root can keep a huge graph alive.

Heap dumps:

```text
jcmd <pid> GC.heap_dump filename=/safe/path/heap.hprof
```

Before dumping, verify:

- enough disk for approximately heap-scale output plus safety margin;
- write path and container persistence;
- pause and I/O impact for this release/heap;
- secrets, personal data, credentials, payloads, and retention policy;
- secure transfer and access controls;
- whether an existing dump-on-OOME policy is more appropriate.

Analyze dominator trees, retained size, paths to GC roots, duplicate values, and class-loader ownership. Compare a suspect population to expected workload state and lifecycle.

`jmap` offers historical heap commands, but prefer supported `jcmd` commands for the deployed JDK. Avoid force/SA attachment unless standard attachment is impossible, the risks are understood, and the incident justifies it.

## Native memory and process RSS

The Java heap is only part of process memory. Other consumers include metaspace, code cache, thread stacks, direct/mapped buffers, GC/compiler structures, JNI/native libraries, allocator fragmentation, and observability agents.

Native Memory Tracking must be enabled at startup:

```text
-XX:NativeMemoryTracking=summary
```

Then:

```text
jcmd <pid> VM.native_memory baseline
jcmd <pid> VM.native_memory summary.diff scale=MB
jcmd <pid> VM.native_memory detail scale=MB
```

NMT has overhead, does not account for every native allocation, and its category names are implementation details. Correlate it with OS/container RSS, mapped memory, thread count, and native-library metrics.

A container OOM kill with no Java `OutOfMemoryError` often means the total process crossed the cgroup limit before the JVM could report a heap failure. Compare committed heap, live heap, native categories, stacks, direct buffers, sidecars, and limit/headroom.

## Class loading and metaspace

Useful evidence:

```text
jcmd <pid> VM.classloader_stats
jcmd <pid> VM.classloaders
jcmd <pid> GC.class_histogram
-Xlog:class+load=info,class+unload=info
```

Metaspace growth can follow legitimate dynamic class generation, redeployment leaks, proxy churn, scripting engines, or class loaders retained by threads, caches, logging frameworks, JDBC drivers, or static registries. A class can unload only when its defining loader and associated classes are unreachable and the JVM performs relevant collection work.

Track loaded/unloaded class counts and group suspect classes by defining loader. Fix the retaining lifecycle rather than simply raising a metaspace limit.

## Compiler and code-cache evidence

Warm-up, compilation, deoptimization, and code-cache pressure can create phase-dependent latency. Representative diagnostics include JFR compilation events, unified compiler logging selected for the release, and commands advertised by:

```text
jcmd <pid> help | grep -i compiler
```

Diagnostic flags such as detailed compilation output can be noisy and version-specific. Do not enable them globally in production without a test. A method that appears hot in interpreted execution may later be compiled; a microbenchmark that omits warm-up can measure compilation rather than steady-state work.

## Heap and collector flags

Common controls:

| Goal | Representative option | Caution |
|---|---|---|
| initial heap | `-Xms<size>` | committing behavior and container headroom matter |
| maximum heap | `-Xmx<size>` | leave room for native memory and sidecars |
| choose G1 | `-XX:+UseG1GC` | default on many modern HotSpot configurations, but verify |
| choose ZGC | `-XX:+UseZGC` | capabilities and generational modes vary by release |
| pause target | `-XX:MaxGCPauseMillis=<ms>` | a heuristic goal, not an SLA |
| dump on heap OOME | `-XX:+HeapDumpOnOutOfMemoryError` | secure and provision dump path |
| dump location | `-XX:HeapDumpPath=/safe/path` | ensure persistence and free space |
| exit on OOME | `-XX:+ExitOnOutOfMemoryError` | coordinate with orchestrator and evidence policy |
| error files | `-XX:ErrorFile=/safe/path/hs_err_pid%p.log` | ensure writable persistent location |

Avoid cargo-cult flag bundles. Defaults, flag names, diagnostic status, and collector behavior change. Begin with an objective and evidence, change the smallest relevant control, and validate under a workload with production-like allocation and latency sensitivity.

## Container checklist

- Confirm the JDK recognizes the actual CPU and memory limits.
- Compare `-Xmx` and total native headroom to the cgroup limit, not host RAM.
- Inspect throttled CPU time; concurrent GC and JIT work still need CPU.
- Bound direct buffers, threads, queues, caches, and pools according to the whole process budget.
- Persist crash logs, heap dumps, and JFR outside ephemeral storage when policy permits.
- Align orchestrator termination grace with Java shutdown and diagnostic time.
- Distinguish Java OOME, kernel/cgroup OOM kill, liveness restart, and operator eviction.

## Symptom-to-evidence map

| Symptom | First evidence | Common false lead |
|---|---|---|
| CPU saturation | process/thread CPU, repeated stacks, JFR execution samples | assuming any `RUNNABLE` thread is consuming CPU |
| long tail latency | request spans/metrics, JFR, GC/safepoint correlation, pool occupancy | blaming GC without time alignment |
| growing heap after GC | live-set series, histograms, heap dominators and roots | reading sawtooth growth as a leak |
| rising RSS with flat heap | NMT, thread/direct-buffer counts, mappings, native tools | increasing `-Xmx` |
| stuck shutdown | repeated thread dumps, executor/task cancellation paths | calling `System.exit` before preserving evidence |
| deadlock | JVM deadlock report plus resource graph | labeling ordinary lock contention deadlock |
| task starvation | executor active/queue metrics, worker stacks, dependency waits | adding unbounded threads |
| allocation storm | JFR allocation samples, GC log allocation rate, endpoint correlation | tuning pause targets before fixing churn |
| metaspace growth | class/loader counts, loader stats, unload logs, roots | only raising metaspace maximum |
| container OOM kill | cgroup event, RSS/native budget, limit history | expecting a heap dump automatically |

## Incident handoff template

A good incident explanation separates observation, inference, and decision. Example: "old occupancy remained above 82% after three mixed cycles" is an observation; "a retained cache is likely" is a hypothesis; "capture a bounded heap dump on one canary" is a decision with operational and privacy costs.

Record:

```text
User impact and interval:
Deployment/runtime identity:
Host or container limits:
Workload and dependency state:
Recent changes:
Thread evidence (timestamps and repetitions):
GC/safepoint evidence:
JFR recording and settings:
Heap/native evidence:
Leading hypotheses:
Evidence that supports/refutes each hypothesis:
Mitigation, risk, and rollback:
Follow-up experiment and owner:
Artifact locations and data classification:
```
