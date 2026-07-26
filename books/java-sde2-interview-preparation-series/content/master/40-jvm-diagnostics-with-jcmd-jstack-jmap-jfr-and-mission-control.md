# 40. JVM Diagnostics with jcmd, jstack, jmap, JFR, and Mission Control

## Learning objectives

By the end of this chapter, you should be able to:

- build a timestamped incident timeline before changing JVM state;
- select low-risk diagnostics for CPU, latency, deadlock, allocation, heap, and native-memory symptoms;
- use `jcmd`, thread dumps, Java Flight Recorder, and heap tools with explicit cost and data-handling controls;
- interpret thread states, repeated stacks, JFR events, histograms, and heap dumps without overclaiming causality;
- explain when `jstack`, `jmap`, and Mission Control are appropriate; and
- write a safe evidence-capture runbook for Java 17 and Java 21 deployments.

## Why this matters at SDE-2

During an incident, diagnostic commands are production changes. Some briefly stop application threads, some walk the heap, some write files as large as the heap, and many capture secrets. An SDE-2 engineer must gather enough evidence to distinguish CPU saturation, lock contention, garbage collection, downstream waiting, native-memory growth, and deadlock while avoiding a second outage caused by diagnosis.

The strongest incident report correlates sources. A thread dump is a moment, a JFR recording is a timeline, service metrics show impact, and a heap dump shows one graph. None alone proves root cause. Preserve timestamps, process identity, container identity, release version, JVM configuration, host pressure, and command results so observations can be aligned.

## First-principles model

Diagnostics answer three questions:

```text
What is the symptom?
Which resource or dependency is limiting progress?
What evidence would disprove each plausible cause?
```

Use an escalation ladder:

```text
existing metrics/logs/traces
    -> process identity and JVM metadata
    -> short JFR or repeated thread dumps
    -> targeted histograms/native-memory summaries
    -> heap dump or invasive tooling only with capacity and approval
```

Start with evidence already being collected because it adds no incident-time perturbation. Prefer time-bounded, reversible capture. Do not tune, restart, force GC, or clear caches before obtaining a baseline unless immediate recovery takes priority over diagnosis. If recovery is necessary, say which evidence was lost.

> **Specification boundary:** These tools are JDK and vendor facilities, not Java Language Specification guarantees. Command names, accepted arguments, attach behavior, event fields, virtual-thread representation, and overhead can change between JDK builds. Inspect `jcmd <pid> help` on the target runtime and test runbooks on the exact distribution.

## Core terminology

- **Attach:** Mechanism by which a diagnostic process connects to a running JVM.
- **Safepoint:** Runtime state where selected VM operations can safely inspect or modify global state.
- **Thread dump:** Snapshot of Java threads, states, stacks, and optionally owned synchronizers.
- **JFR:** Java Flight Recorder, an event recording facility integrated with supported JDKs.
- **JMC:** Java Mission Control, a graphical analysis application commonly used with JFR recordings.
- **Class histogram:** Counts and shallow bytes grouped by class.
- **Heap dump:** Snapshot of heap objects and references, commonly in HPROF form.
- **Native memory:** Process memory outside ordinary Java heap, including metaspace, code, threads, direct buffers, and native libraries.
- **NMT:** HotSpot Native Memory Tracking.
- **Deadlock:** Cycle in which participants wait indefinitely for resources held by one another.
- **Perturbation:** Change in application behavior caused by measurement.
- **Evidence custody:** Secure storage, access control, retention, and deletion of diagnostic artifacts.

## Detailed mechanics

### Establish identity and context

Confirm the operating-system PID inside the relevant PID namespace. In containers, a host PID and container PID can differ. Record current time with timezone, pod or host, deployment revision, traffic, and whether the symptom is active. Avoid relying solely on `jps`; not every Java process is discoverable in every namespace or permission setup.

`jcmd <pid> VM.version`, `VM.command_line`, and `VM.flags` can capture runtime identity and configuration. System properties and command lines may contain credentials, tokens, paths, or customer identifiers, so collect them only into restricted storage. `jcmd <pid> help` lists commands actually available in that JVM.

### Thread dumps as repeated snapshots

`jcmd <pid> Thread.print -l` is the preferred general HotSpot route in many modern deployments. `jstack -l <pid>` is a useful fallback when available and supported. Capture several dumps separated by a meaningful interval. A repeated identical stack on an on-CPU method is stronger evidence than one appearance.

Common thread states require context:

- `RUNNABLE` means eligible/running from the JVM perspective; it can include native I/O and does not prove CPU consumption.
- `BLOCKED` means waiting to enter an intrinsic monitor.
- `WAITING` can be normal parking in a pool, queue, or `join`.
- `TIMED_WAITING` includes timed sleep, park, and wait.
- terminated threads do not appear as live work.

Look for deadlock reports, many threads blocked behind the same owner, pool threads waiting on downstream calls, and stacks that remain active across captures. Correlate native thread IDs with OS CPU tools where supported. Thread names are operational API: name pools and critical workers meaningfully.

Virtual threads change scale. Dump formats and commands capable of representing large virtual-thread sets have evolved across JDK releases. Do not assume one platform-thread-style dump will list or interpret every virtual-thread task identically; validate Java 21 tooling and prefer JFR events or supported virtual-thread dumps for the target build.

### Java Flight Recorder

JFR records timestamped JVM and application events with configurable settings. Useful event families include execution samples, allocation samples, garbage collection, monitor contention, thread parking, file/socket I/O, exceptions, class loading, and JVM configuration. Availability and fields are version-dependent.

A continuously running, size- and age-bounded recording is often more valuable than starting after an event. It preserves the lead-up. If continuous recording is not configured, `jcmd` can start a named, duration-bounded recording and write it to a restricted path. The `default` configuration targets lower overhead; `profile` commonly collects more detail at higher cost. Validate both overhead and disk behavior under load.

Typical command shapes are:

```bash
jcmd 12345 JFR.check
jcmd 12345 JFR.start name=incident settings=default duration=120s filename=/restricted/incident.jfr
jcmd 12345 JFR.dump name=incident filename=/restricted/snapshot.jfr
jcmd 12345 JFR.stop name=incident
```

Do not paste these blindly. Confirm PID, available help, output directory, free space, permissions, recording name, and the JDK's syntax. JFR can contain class names, paths, stack traces, thread names, URLs, and application event fields.

Mission Control opens recordings for timeline analysis, event browsing, automated rules, flame-like views, lock analysis, and GC inspection. It is an analyzer, not an oracle. Automated warnings are leads to correlate with service impact. JMC versions are distributed separately from some JDKs and should be compatible with the recording format in use.

### Class histograms and heap dumps

A class histogram answers "which classes account for many live or heap-visible objects and shallow bytes?" It does not show why they are retained. `jcmd <pid> GC.class_histogram` and `jmap -histo` variants can be costly because implementations may require a safepoint and heap traversal; live-only variants can trigger additional GC work. Syntax and impact vary.

A heap dump preserves objects and reference edges for dominator and path-to-root analysis. It can pause the process, perform substantial I/O, consume disk near heap size or more, expose secrets, and worsen a memory-starved host. Verify disk headroom, storage performance, encryption/access controls, and whether a replica can be removed from traffic. Prefer `jcmd` heap-dump facilities over older `jmap` paths when the target JDK recommends them.

Configure `-XX:+HeapDumpOnOutOfMemoryError` and a controlled dump path only after validating disk, permissions, retention, and restart behavior. The JVM may be too impaired to complete a dump. Never make heap-dump success the only OOM evidence plan.

### Native memory and process evidence

Resident-set growth with stable heap may come from thread stacks, direct buffers, metaspace, code cache, JNI, memory mapping, allocators, or kernel accounting. Process metrics and OS maps provide evidence. On HotSpot, NMT can categorize tracked native allocations, but it must be enabled at process startup and adds overhead. Summary tracking is normally less costly than detail tracking.

With NMT enabled, `jcmd <pid> VM.native_memory baseline` followed later by `summary.diff` or the target version's equivalent can expose category growth. NMT does not track every native allocation and its totals need not equal RSS. Treat categories as a model to correlate, not exact process accounting.

> **HotSpot note:** Attach implementation, safepoint behavior, NMT categories, JFR event sets, compiler names, and diagnostic-command side effects are HotSpot and build specific. Container security policies can disable attach even when commands are present.

### Tool failure is evidence, not permission to escalate blindly

Attach can fail because of permissions, namespaces, disabled mechanisms, a hung JVM, or incompatible tools. Use tools from the same JDK family as the target. Do not reach immediately for forced serviceability agents or debuggers on production; they can suspend or destabilize the process. Follow an approved runbook and decide whether replica failover, core dump, or restart is safer.

## Worked Java example

This bounded program creates observable intrinsic-lock contention without deadlocking forever:

```java
import java.util.ArrayList;
import java.util.List;

public final class ContentionDemo {
    private static final Object LOCK = new Object();

    private static void work() {
        for (int i = 0; i < 100; i++) {
            synchronized (LOCK) {
                try {
                    Thread.sleep(20);
                } catch (InterruptedException interrupted) {
                    Thread.currentThread().interrupt();
                    return;
                }
            }
        }
    }

    public static void main(String[] args) throws InterruptedException {
        List<Thread> workers = new ArrayList<>();
        for (int i = 0; i < 4; i++) {
            Thread worker = new Thread(ContentionDemo::work, "worker-" + i);
            workers.add(worker);
            worker.start();
        }
        for (Thread worker : workers) {
            worker.join();
        }
    }
}
```

In a disposable local environment, run the process, find its PID safely, and capture two thread dumps while it is active. A short JFR recording can then show monitor-blocked events. Do not run an artificial contention program on a shared production host.

## Execution or memory walkthrough

At most one worker owns `LOCK`. That worker calls `sleep` while still inside the synchronized block, so it appears `TIMED_WAITING` while retaining the monitor. Other workers attempting entry appear `BLOCKED`, and the dump identifies the monitor owner when lock detail is available.

Twenty milliseconds later, the owner wakes and exits. Scheduling and monitor acquisition determine the next owner; intrinsic locks do not promise strict FIFO fairness. Across several dumps, different workers may own the monitor, but the structural pattern remains one sleeper and multiple blocked contenders.

A JFR timeline adds duration: it can reveal how long monitor entry was blocked and which stack requested it. OS CPU may remain low because most threads wait. This combination disproves the hypothesis that high latency necessarily means CPU saturation.

No deadlock exists because the owner eventually releases one lock and there is no dependency cycle. A single dump with blocked threads is therefore not a deadlock diagnosis.

## Complexity and performance

Diagnostic cost scales with captured scope:

| Evidence | Typical relative impact | Scaling concern |
|---|---|---|
| existing metrics/logs | already paid | cardinality and logging volume |
| thread dump | usually brief | number of threads and stack depth |
| low-overhead JFR | designed for continuous use | event rate, stack traces, disk limits |
| profile JFR | higher | sampling/event configuration and workload |
| class histogram | moderate to high | heap object count and safepoint work |
| heap dump | high | live/used heap, pause, disk and I/O bandwidth |
| detailed NMT | startup-enabled overhead | native allocation rate and tracking detail |

These are not universal rankings. A JVM with millions of virtual threads, a nearly full disk, or a stalled safepoint can change risk. Estimate artifact size and latency, test on a realistic replica, and define an abort condition.

Analysis complexity also matters. A histogram is small enough for quick comparison but lacks edges. A heap dump provides retention paths but can take significant offline processing memory. JFR duration and event settings trade history against file size and detail.

## Edge cases and common mistakes

- Attaching to the wrong PID or wrong container namespace.
- Collecting credentials from command lines, properties, heap, or JFR into world-readable files.
- Taking one thread dump and labeling every waiting thread a problem.
- Treating `RUNNABLE` as proof that a thread consumes CPU.
- Calling blocked threads a deadlock without a wait cycle.
- Forcing GC before preserving the heap behavior under investigation.
- Requesting a heap dump on a nearly full filesystem or latency-critical sole instance.
- Assuming a histogram's shallow bytes identify the retaining owner.
- Comparing NMT totals directly with RSS and declaring the difference a leak.
- Starting a high-detail recording without duration and size controls.
- Using diagnostic tools from an incompatible JDK distribution or version.
- Restarting before recording JVM arguments, timeline, and at least low-cost evidence.
- Uploading diagnostic artifacts to unapproved analysis services.
- Treating a Mission Control rule as proven causality.

## Production engineering notes

Prepare diagnostics before incidents. Bake compatible tools into an approved debug image or host path, configure secure artifact storage, define who may attach, and test commands against Java 17 and Java 21 services. Keep commands parameterized by verified PID and never use broad kill or file targets.

Enable bounded continuous JFR where overhead tests permit. Give recordings unique incident IDs and UTC timestamps. Apply least privilege, encryption, short retention, and an audit trail. Heap dumps usually require the strongest classification because they can contain entire request bodies and credentials.

Capture three synchronized views: application impact, JVM behavior, and host/container limits. CPU throttling, memory limits, file descriptors, DNS, storage, and downstream pools can imitate JVM problems. Preserve deployment events and feature-flag changes.

Separate recovery from root-cause analysis. Traffic shedding, failover, or restart may be the correct first action. Record that decision, collect evidence from another replica or continuous recording, and reproduce later. Never delay recovery solely to obtain a perfect dump.

## Interview questions and model answers

**What would you collect for a Java latency spike?**

First correlate request histograms, errors, traces, CPU/throttling, GC, allocation, pools, and downstream latency. Then verify process metadata and capture a short JFR or repeated thread dumps. Escalate to heap evidence only if the hypothesis requires it and impact is acceptable.

**Why take multiple thread dumps?**

One dump is a snapshot. Repeated dumps show whether the same stacks make no progress, which owners change, and whether contention or downstream waiting persists.

**What is the difference between a histogram and a heap dump?**

A histogram aggregates class counts and shallow bytes. A heap dump preserves individual objects and reference edges, enabling dominator and root-path analysis at much greater cost and sensitivity.

**How do JFR and JMC relate?**

JFR records timestamped events in the JVM. Mission Control analyzes recordings and presents timelines and rules. It helps form hypotheses but does not automatically prove root cause.

**Can NMT prove a native-memory leak?**

It can show growth in tracked HotSpot categories and guide a hypothesis. It omits some allocations and differs from RSS, so correlate it with process maps, direct-buffer metrics, thread counts, and repeated baselines.

**Why can a diagnostic command be dangerous?**

It can safepoint or pause the JVM, traverse a huge heap, consume disk and I/O, expose sensitive data, or fail under attach restrictions. Validate impact and storage before running it.

## Exercises

1. Given high CPU and stable GC, design a capture sequence using OS per-thread CPU, repeated dumps, and JFR samples.
2. Given rising RSS but flat committed heap, list evidence for thread stacks, direct buffers, metaspace, and native libraries.
3. Write a runbook precheck for heap dumping a 24 GB service. Include replica status, disk, access, pause, and deletion.
4. Run the contention example locally and explain `BLOCKED`, `TIMED_WAITING`, monitor ownership, and why it is not deadlock.
5. Design a continuous JFR retention policy for a service with strict customer-data controls.
6. Review a thread dump with 200 idle pool workers in `WAITING`. Explain what additional evidence is required before tuning the pool.

## Chapter summary

JVM diagnosis is controlled evidence collection. Begin with timelines and existing telemetry, verify process identity, and escalate from repeated thread snapshots or bounded JFR to histograms and heap dumps only when the hypothesis justifies their cost. Thread state needs temporal and OS context. JFR provides event history; Mission Control supports analysis; heap and native-memory tools answer narrower retention questions. Every artifact is sensitive, every command is version-dependent, and recovery may take priority over perfect evidence.

## Revision checklist

- [ ] I verify PID, namespace, JDK, timestamp, release, and symptom before attachment.
- [ ] I can interpret thread states without equating `RUNNABLE` with CPU or waiting with failure.
- [ ] I capture repeated dumps and correlate them with OS and request evidence.
- [ ] I can start, dump, and stop a bounded JFR recording after checking target help.
- [ ] I distinguish JFR recording from Mission Control analysis.
- [ ] I know the cost and information difference among histogram, heap dump, and NMT.
- [ ] I secure artifacts and check disk, replica capacity, and abort conditions.
- [ ] I label HotSpot, vendor, container, and JDK-version behavior explicitly.
