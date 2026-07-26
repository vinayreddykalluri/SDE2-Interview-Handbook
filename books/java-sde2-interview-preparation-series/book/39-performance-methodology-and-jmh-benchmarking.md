# 39. Performance Methodology and JMH Benchmarking

## Learning objectives

By the end of this chapter, you should be able to:

- turn a vague performance concern into a falsifiable question and a measurable objective;
- distinguish latency distributions, throughput, utilization, allocation, and asymptotic cost;
- explain warmup, tiered compilation, dead-code elimination, constant folding, and benchmark contamination;
- build a disciplined JMH benchmark with state, forks, warmup, measurement, and parameters;
- interpret measurements with uncertainty instead of selecting one attractive number; and
- connect microbenchmark evidence to profiling, load tests, production traces, and business constraints.

## Why this matters at SDE-2

Performance work at SDE-2 is decision work. The engineer must identify which user-visible objective is failing, collect evidence at the correct layer, and make the smallest change that improves the limiting resource without violating correctness. "This collection is faster" or "the JVM will optimize it" is not an engineering argument.

Java makes naive measurement particularly misleading. Code begins interpreted or lightly compiled, hot methods may be recompiled, unused work can disappear, object allocations can be scalar-replaced, garbage collection can interrupt samples, and the operating system can move the process between CPUs. JMH handles many mechanics, but it cannot repair a meaningless workload or prove that a micro-level improvement changes end-to-end behavior.

## First-principles model

Performance is work completed per constrained resource and time. Begin with an observable target:

```text
Question: Why did checkout p99 latency rise from 180 ms to 420 ms?
Scope: production requests in region A after release R
Constraints: error rate <= 0.1%, same durability and validation
Evidence: latency histogram, traces, CPU, allocation, GC, downstream timings
Hypothesis: repeated JSON materialization increased allocation and GC pauses
Experiment: remove one copy, compare controlled canary and allocation profile
```

The feedback loop is:

```text
define -> measure baseline -> form hypothesis -> design experiment
       -> change one variable -> validate -> deploy safely -> observe
```

Latency is a distribution, not one scalar. Throughput and latency interact through queueing: as utilization approaches capacity, waiting time can rise sharply. A faster isolated operation may not improve the system if a database, lock, network call, or admission limit remains the bottleneck.

> **Specification boundary:** Java SE does not specify JIT compilation thresholds, escape analysis outcomes, code layout, garbage collector ergonomics, or JMH behavior. These are implementation and tool concerns. Preserve Java semantics, but measure optimization behavior on the actual JDK, collector, hardware, and configuration.

## Core terminology

- **Throughput:** Completed operations per unit time.
- **Latency:** Time for one operation; report percentiles and the measurement window.
- **Tail latency:** High-percentile latency such as p95, p99, or p99.9.
- **Utilization:** Fraction of a resource's available capacity in use.
- **Service time:** Active processing time excluding some or all queue waiting, depending on definition.
- **Warmup:** Execution before measurement so runtime state approaches the intended regime.
- **Fork:** Fresh JVM process used as an independent benchmark run.
- **Steady state:** Period whose relevant runtime behavior is sufficiently stable for the question.
- **Dead-code elimination:** Removal of computations whose results cannot affect observable behavior.
- **Constant folding:** Compile-time or JIT-time evaluation of expressions known to be constant.
- **Escape analysis:** Reasoning that can enable allocation elimination or synchronization optimization.
- **Benchmark state:** Data whose lifecycle and sharing are controlled by the harness.
- **Confidence interval/error estimate:** Range describing measurement uncertainty under a statistical model.
- **Coordinated omission:** Missing slow observations because the load generator waits before issuing the next request.

## Detailed mechanics

### Define the metric before the experiment

State the operation, population, units, percentile, traffic shape, input distribution, and correctness constraints. "Average endpoint time" can hide a damaging tail. CPU time excludes blocking. Wall time includes scheduling and waiting. Allocation rate does not directly equal retained heap. Pick metrics tied to the hypothesis.

Record a baseline and a control. Change one primary variable. Preserve source data, JVM arguments, dependency versions, CPU limits, and test duration when possible. Run enough independent trials to see variance. If a result changes sign across forks, the honest conclusion is uncertainty, not the best-looking fork.

### Why stopwatch loops fail

`System.nanoTime` is suitable for elapsed intervals but does not create a correct harness. A hand-written loop often measures compilation transitions, loop machinery, a constant expression, timer calls, or shared setup. It can also omit the computed result, allowing the optimizer to remove the work.

The JIT optimizes the program it sees. If benchmark inputs are constants, it may precompute results. If an allocated object does not escape, it may never become a heap object. These are valid production optimizations, but a benchmark intended to measure allocation or general inputs must prevent accidental specialization without preventing realistic optimization.

### JMH execution model

JMH, the Java Microbenchmark Harness, is a separate tool rather than a Java SE API. Its generated harness controls invocation, warmup, measurement, result consumption, and process forks.

Important annotations include:

- `@Benchmark` marks measured methods.
- `@BenchmarkMode` selects throughput, average time, sample time, or single-shot behavior.
- `@OutputTimeUnit` chooses presentation units.
- `@Warmup` and `@Measurement` configure iteration phases.
- `@Fork` requests fresh JVM processes.
- `@State` defines state sharing: per thread, benchmark instance, or group.
- `@Param` expands controlled input cases.
- `@Setup` and `@TearDown` manage state outside timed invocations at trial, iteration, or invocation level.

Use multiple forks for JVM-level independence. Warmup is not "always five iterations"; it must be long enough for the workload and question. Inspect per-iteration results for drift. Single-shot mode is appropriate for cold or one-off operations only when that is explicitly the target.

### Preventing invalid optimization without disabling the JVM

Return the result from the benchmark or pass it to JMH's `Blackhole`. Do not add arbitrary logging, volatile writes, or synchronization merely to keep work alive; they can dominate the measurement. Supply varied state through parameters or setup when constants are unrealistic.

Move construction out of timed code unless construction is the operation under test. If each invocation mutates state, reset at a deliberate lifecycle level and include or exclude reset cost according to the real question. Invocation-level setup can be expensive enough to distort very small operations.

### Benchmark modes and profilers

Throughput mode answers operations per time. Average time reports mean operation time under the harness model. Sample time records a sample distribution; it is not a replacement for a production latency histogram. Single-shot measures individual invocations and is sensitive to noise.

JMH profilers can add allocation, GC, stack, compiler, or platform-counter evidence, but profilers perturb execution. Compare profiled runs with unprofiled runs. Hardware counters and assembly views depend on OS permissions, architecture, installed tools, and JVM support.

> **HotSpot note:** Tiered compilation, inlining budgets, on-stack replacement, escape analysis, and code-cache behavior are HotSpot implementation details and version-sensitive. A benchmark result from Java 17 is evidence for that setup, not a promise for Java 21 or another JVM.

### From micro to macro

A microbenchmark isolates mechanism. A component benchmark includes serialization, pools, and local dependencies. A load test checks queueing and capacity. Production observation validates actual inputs, contention, and downstream behavior. Use the lowest layer that can answer the question, then validate upward.

Do not extrapolate nanoseconds saved per isolated call by multiplying by request count unless the operation is actually on the critical path at that frequency. Profiling should confirm contribution to CPU or latency. Optimization can shift cost to memory, startup, code size, or maintainability.

## Worked Java example

This JMH benchmark compares miss lookup in a list and a set. It isolates lookup by constructing state once per trial:

```java
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.stream.IntStream;
import org.openjdk.jmh.annotations.Benchmark;
import org.openjdk.jmh.annotations.BenchmarkMode;
import org.openjdk.jmh.annotations.Fork;
import org.openjdk.jmh.annotations.Level;
import org.openjdk.jmh.annotations.Measurement;
import org.openjdk.jmh.annotations.Mode;
import org.openjdk.jmh.annotations.OutputTimeUnit;
import org.openjdk.jmh.annotations.Param;
import org.openjdk.jmh.annotations.Scope;
import org.openjdk.jmh.annotations.Setup;
import org.openjdk.jmh.annotations.State;
import org.openjdk.jmh.annotations.Warmup;

@BenchmarkMode(Mode.AverageTime)
@OutputTimeUnit(TimeUnit.NANOSECONDS)
@Warmup(iterations = 5, time = 1)
@Measurement(iterations = 7, time = 1)
@Fork(3)
public class MembershipBenchmark {
    @State(Scope.Thread)
    public static class Data {
        @Param({"16", "1024", "65536"})
        int size;

        List<Integer> list;
        Set<Integer> set;
        Integer missing;

        @Setup(Level.Trial)
        public void setup() {
            ArrayList<Integer> values = IntStream.range(0, size)
                    .boxed()
                    .collect(java.util.stream.Collectors.toCollection(ArrayList::new));
            list = values;
            set = new HashSet<>(values);
            missing = -1;
        }
    }

    @Benchmark
    public boolean listMiss(Data data) {
        return data.list.contains(data.missing);
    }

    @Benchmark
    public boolean setMiss(Data data) {
        return data.set.contains(data.missing);
    }
}
```

The JMH annotation processor and runtime must be configured in the build. Pin their version rather than copying an unversioned command from another project. A typical generated benchmark artifact can be filtered by benchmark name and optionally run with a GC profiler, but exact command-line options belong to the selected JMH version.

## Execution or memory walkthrough

For each fork, JMH starts a new JVM. For each parameter value it creates thread-scoped `Data`, runs trial setup, then executes warmup iterations whose results are not reported as measurements. Compilation and profile information evolve during warmup. Measurement iterations follow and JMH aggregates fork-level observations.

For `size = 16`, `list.contains(-1)` checks 16 boxed integer references/equality operations. The hash set computes a hash, selects a bucket, and usually confirms absence after a small amount of work, but it has a larger structure and less locality. At tiny sizes, constants may make the list competitive. At 65,536 elements, the list miss is linear while expected set lookup remains constant under good hashing.

The benchmark returns a Boolean result to the harness, preventing trivial removal. It does not time collection construction, iteration, memory footprint, cache warmness across production requests, successful lookups at varied positions, or concurrent access. Those are separate experiments.

## Complexity and performance

The underlying algorithm predicts trends:

| Operation | List | Hash set, typical expectation |
|---|---:|---:|
| miss lookup | `O(n)` | expected `O(1)` |
| build from `n` values | `O(n)` | expected `O(n)` |
| storage | `O(n)` references | `O(n + capacity)` plus entry overhead |
| ordered iteration | insertion sequence for list | no general hash-set order |

Big-O does not predict the crossover size. Hash computation, equality, allocation, cache locality, table load, JVM optimization, and hardware determine constants. The benchmark measures one point in this design space.

Measurement cost also matters. More forks and longer iterations improve confidence but consume time. Prefer enough independent evidence to make the decision, not maximal duration by ritual. Report configuration, sample counts, central estimates, uncertainty, outliers, and all tested parameter values. Never report more precision than noise supports.

## Edge cases and common mistakes

- Optimizing without a baseline, SLO, profile, or explicit hypothesis.
- Reporting only averages and hiding tail behavior or errors.
- Timing startup while claiming steady-state throughput, or warming up while claiming cold startup.
- Leaving benchmark results unused and measuring code eliminated by the optimizer.
- Putting data generation, logging, assertions, or random seeding in the timed operation unintentionally.
- Sharing mutable benchmark state across threads without matching production semantics.
- Running one fork and treating one score as universal.
- Comparing runs with different CPU quotas, power modes, thermal states, JVM flags, or background load.
- Using unrealistic constant inputs that enable specialization.
- Treating a profiler run as unperturbed timing evidence.
- Assuming a statistically significant micro improvement is operationally meaningful.
- Running load generators that suffer coordinated omission and under-report queue delay.
- Claiming causality from correlation in one production graph.

## Production engineering notes

Start with production-safe telemetry: request histograms, traces, error rate, CPU, allocation, GC, pool saturation, database timing, and queue depth. Align timestamps and release markers. Sampling profilers are usually safer than ad hoc high-frequency instrumentation, but every diagnostic has cost.

Benchmark on the same major JDK, collector, architecture, and container limits as production. Pin benchmark code and dependencies in source control. Capture JVM arguments and environment metadata with results. Run noisy-neighbor and concurrency tests when shared infrastructure is part of the question.

Optimize one confirmed hot path, add correctness and performance regression tests at the appropriate layer, and canary the change. A microbenchmark threshold in CI can be flaky; compare distributions on controlled runners and alert on substantial, repeated regressions rather than nanosecond drift.

Stop when the SLO and capacity target are met with margin. Further optimization spends complexity budget and can reduce reliability. Preserve a written experiment log so the next incident does not repeat disproven hypotheses.

## Interview questions and model answers

**Why is a loop around `System.nanoTime` not enough?**

It does not control warmup, forks, dead-code elimination, setup, or statistical independence. Timer and loop overhead can dominate. JMH supplies a generated harness, but the workload still needs a valid question and realistic inputs.

**Why does JMH use warmup and forks?**

Warmup lets runtime compilation and profiles approach the target regime. Forks create fresh JVM processes, reducing dependence on one process's compilation, heap, and code-cache history.

**How do you prevent dead-code elimination?**

Return the computed result or consume it through JMH's `Blackhole`. Also avoid fully constant inputs if the production operation receives varied data.

**When is a microbenchmark the wrong tool?**

When the question depends on I/O, queueing, thread-pool saturation, distributed calls, or user-visible tail latency. Use component, load, or production evidence for those effects.

**Can parallel code have higher throughput but worse latency?**

Yes. It can increase contention, coordination, queueing, allocation, and tail variation while completing more aggregate work. Measure both under the intended concurrency.

**What would you report with a benchmark result?**

Question, code revision, JDK and arguments, hardware and limits, JMH configuration, inputs, forks, score and uncertainty, profiler observations, correctness checks, and the scope of conclusions.

## Exercises

1. Design a JMH benchmark comparing string concatenation strategies for 10, 100, and 10,000 fragments. Decide whether construction belongs inside the timed method.
2. Find three ways a benchmark of `Math.log(42.0)` could measure constant folding rather than general logarithm cost.
3. Write separate experiments for cold startup and warmed request throughput. Explain why one configuration cannot answer both.
4. Given p50 20 ms, p99 900 ms, low CPU, and a saturated connection pool, form three hypotheses and choose evidence for each.
5. Add successful first, middle, and last list lookups to the worked example without introducing random-number generation into timed code.
6. Review a claimed 3 percent speedup whose fork error intervals overlap substantially. Write the appropriate conclusion and next experiment.

## Chapter summary

Performance engineering begins with a user-visible objective, a baseline, and a falsifiable hypothesis. Java runtime optimization makes naive timing unreliable. JMH controls microbenchmark lifecycle, result consumption, warmup, measurement, and forks, but it cannot supply realistic semantics. Use complexity to predict trends, profilers to locate mechanisms, higher-level tests to expose queueing and integration effects, and production canaries to validate impact. Report uncertainty and stop when measured objectives are met.

## Revision checklist

- [ ] I define the operation, distribution, metric, load, and correctness constraints before measuring.
- [ ] I distinguish throughput, latency percentiles, service time, utilization, allocation, and retention.
- [ ] I can explain warmup, tiered compilation, dead-code elimination, constant folding, and escape analysis.
- [ ] I use JMH state, setup, parameters, measurement iterations, and forks intentionally.
- [ ] I consume benchmark results and isolate setup according to the question.
- [ ] I report environment, uncertainty, outliers, and scope of conclusions.
- [ ] I validate microbenchmark findings with profiles and the appropriate higher-level experiment.
- [ ] I label HotSpot and tool behavior as version-sensitive.
