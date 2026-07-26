# Chapter 10: Execution Engine, JIT Compilation, and Safepoints

## Learning objectives

- Explain how bytecode execution can combine interpretation and multiple compilation tiers.
- Describe profiling, inlining, speculative optimization, on-stack replacement, and deoptimization.
- Distinguish the code cache and compilation work from Java heap behavior.
- Explain safepoints, safepoint polling, time-to-safepoint, and stop-the-world coordination.
- Investigate warm-up, compilation, and safepoint issues using evidence rather than folklore.

## Why this matters at SDE-2

Java service performance changes over time. Cold startup can be dominated by loading and interpretation; a warm service can run highly optimized native code; a traffic-shape change can invalidate profiles; compiler CPU can compete with requests; and a global VM operation can pause threads even when GC work is small. These effects matter for autoscaling, canary comparison, benchmark design, and tail-latency incidents.

The SDE-2 standard is not memorizing C1 and C2. It is explaining why adaptive optimization exists, which assumptions it can make, how correctness survives invalidation, and how safepoint coordination interacts with all execution forms.

## First-principles model

Bytecode is a portable semantic program, not the only runtime representation. An execution engine may trade immediate startup for later optimization:

```text
class-file bytecode
        |
        +--> execute quickly with low setup cost
        |       interpreter or low-tier compiled code
        |                    |
        |                    v
        |             collect runtime profiles
        |        calls, branches, receiver types, traps
        |                    |
        +--------------------v
                    optimize hot paths
                native code in code cache
                    |
             assumption invalidated?
                    |
                    v
                deoptimize and possibly recompile
```

The JVM can make an optimization that is valid under a guarded assumption, retain metadata to recover a legal execution state, and abandon the optimized form if the assumption becomes false. This feedback loop is the foundation of adaptive JIT compilation.

## Core terminology

- **Interpreter:** Executes bytecode with relatively low compilation delay and rich profiling opportunities.
- **Hot method/loop:** Code selected for compilation based on implementation counters and policy.
- **Tiered compilation:** Movement through execution/compilation levels balancing profiling speed and optimization quality.
- **Inlining:** Substitution of callee operations at a call site, often enabling further optimization.
- **Profile-guided optimization:** Use of observed receiver types, branches, and call frequencies.
- **On-stack replacement (OSR):** Replacement of an executing loop/method state with compiled code before invocation returns.
- **Speculative optimization:** Optimization based on a guarded, currently valid runtime assumption.
- **Uncommon trap:** Transfer out of optimized code when a rare path or failed assumption occurs.
- **Deoptimization:** Reconstruction of less optimized/interpreted state from compiled execution metadata.
- **Code cache:** Native memory used for generated machine code and associated runtime structures.
- **Safepoint poll:** A check through which a thread cooperates with a safepoint request.
- **Time to safepoint (TTSP):** Delay between requesting a global safe state and all relevant threads reaching it.

## Detailed mechanics

Interpretation gives fast availability: the runtime can start executing a method without spending time on aggressive optimization. It also observes behavior. A JIT compiler spends CPU and memory to make repeated execution cheaper. Tiering attempts to optimize total time by using quick compilation for moderately hot code and expensive optimization for code likely to repay it.

Inlining is the gateway optimization. Once a callee is merged into a caller, the compiler can propagate constants, eliminate dead branches, remove redundant null/bounds checks, specialize virtual calls, combine arithmetic, and analyze allocations across the old call boundary. Inlining decisions consider hotness, call-site profile, method size, recursion depth, and compilation budget.

Dynamic dispatch does not prevent optimization. If a call site has observed one receiver class, compiled code can guard that class and call or inline the known target. Class-hierarchy analysis can prove that only one target currently exists. A later class load can invalidate such a dependency. The runtime makes affected code non-entrant, transfers active execution safely when needed, and can recompile with a broader dispatch path.

Escape analysis can prove that a newly created object is confined to a compilation scope. Scalar replacement then represents its fields as independent values and removes allocation. Lock elimination can remove synchronization on a proven non-escaping object. These transformations require inlining and analysis visibility; small source changes can alter the outcome.

Loops deserve special treatment because a single method invocation can run for a long time. Waiting for it to return before using compiled code loses the benefit. OSR compiles and enters a loop at a supported bytecode position using the live state of the current invocation.

Optimized code must support exceptions, garbage collection, stack walking, debugging, and deoptimization. It therefore includes metadata mapping machine positions and registers to Java-level values, reference locations, and possible reconstructed frames. An inlined method can appear as a logical frame even though no independent physical call occurred.

The code cache is separate from the Java heap. Generated code has a life cycle: compilation installs it, dependencies can invalidate it, sweeper/runtime policy can reclaim dead forms, and finite capacity can fill. If effective compilation is disabled or constrained by code-cache pressure, throughput may degrade without a heap OOME.

> **Specification boundary:** The JLS and JVMS require correct language/JVM behavior. They do not require an interpreter, JIT, tiered compilation, C1/C2, OSR, a code cache, escape analysis, or deoptimization. A conforming implementation could use ahead-of-time compilation or another execution strategy.

Safepoints solve a coordination problem. Some runtime operations need every relevant thread at a state where object references and execution can be described reliably. Compilers place polling opportunities at selected transitions, returns, or loop paths, and runtime stubs/transitions cooperate. Once a safepoint is requested, threads eventually observe it and enter a safe state; the VM operation executes; threads resume.

TTSP and safepoint operation time are separate:

```text
request issued ------- all threads stopped -------- operation ends
       time to safepoint          safepoint operation time
```

A long TTSP can arise from CPU starvation, a thread in a difficult native/critical transition, or generated code with delayed polling under a particular implementation defect or path. A short TTSP followed by a long pause points to the operation itself. Some JVM operations can instead use thread-local handshakes, coordinating only selected threads, but this is implementation evolution rather than a Java guarantee.

> **HotSpot note:** Mainstream HotSpot uses tiered compilation with an interpreter, C1, and C2 in common configurations. It uses invocation/back-edge counters, OSR, speculative dependencies, uncommon traps, and safepoint polling. Exact levels, thresholds, compiler availability, and polling mechanisms vary by build and release.

## Worked Java example

```java
public final class AdaptiveExecution {
    interface PriceRule {
        long apply(long cents);
    }

    static final class Discount implements PriceRule {
        public long apply(long cents) {
            return cents - cents / 10;
        }
    }

    static long total(PriceRule rule, long[] prices) {
        long sum = 0;
        for (long price : prices) {
            sum += rule.apply(price);
        }
        return sum;
    }

    public static void main(String[] args) {
        long[] prices = {100, 200, 300, 400};
        PriceRule rule = new Discount();
        long checksum = 0;
        for (int i = 0; i < 1_000_000; i++) {
            checksum ^= total(rule, prices);
        }
        System.out.println(checksum);
    }
}
```

The repeated call site has one receiver type. An adaptive compiler may inline `Discount.apply` through the interface call, inline `total`, and optimize loop mechanics. "May" is essential: compilation policy, command-line modes, profiling, and program context determine the result.

## Execution or memory walkthrough

A plausible HotSpot timeline is:

1. Classes load and methods begin in interpreted or early-tier execution.
2. Invocation and loop back-edge counters grow. Profiles record that `rule` is always `Discount` at the call site.
3. `total` or `main` is queued for compilation on compiler threads. Application execution continues using an available form.
4. Compiled code is installed in the code cache. A future invocation enters it, or OSR transfers a currently executing hot loop.
5. The interface call is guarded/specialized. After inlining, `cents / 10` and summation appear in a larger optimization graph.
6. A safepoint request can stop the application at a poll. Metadata tells GC where `prices`, `rule`, and any other live references reside, even if not in source-like frame slots.
7. If another implementation of `PriceRule` later appears at that site or a hierarchy dependency changes, a guard can choose a fallback or an uncommon trap can deoptimize.
8. Deoptimization reconstructs logical Java frames and values, materializing objects if scalar replacement had removed them, then continues in a valid less-specialized form.

The JIT cannot delete the entire computation if the printed value depends on it, though algebraic simplification may still be possible. A benchmark whose result is unused is more vulnerable to dead-code elimination.

## Complexity and performance

For `p` prices and `n` repetitions, source algorithmic time is O(np), with O(1) auxiliary algorithmic space beyond the input. JIT compilation does not change Big-O here, but it can drastically change constants. Inlining can reduce dispatch overhead and expose vectorization or range-check elimination.

Optimization has a budget:

- interpretation/low tier reduces startup delay but executes slower;
- profiling consumes counters and some runtime work;
- compiler threads consume CPU and native memory;
- optimized code consumes code cache;
- speculation can pay off, but repeated deoptimization/recompilation wastes work;
- aggressive inlining increases code size and instruction-cache pressure.

Warm-up is workload-specific. A fixed number of iterations is not proof of steady state. Observe compilation and profile stability, use forked processes, consume results, and rely on JMH for microbenchmarks. For services, load testing should include startup, ramp, representative polymorphism, and traffic shifts.

## Edge cases and common mistakes

- Calling JIT compilation a Java language guarantee.
- Assuming hotness means elapsed time only; policy often uses invocation and loop counters plus tier state.
- Believing interface calls cannot inline.
- Equating source calls with physical native frames after inlining.
- Treating deoptimization as an error; it is a normal correctness mechanism.
- Benchmarking a constant or unused result that the compiler removes.
- Mixing cold and warm samples without labeling them.
- Assuming the code cache is part of heap or fixed by `-Xmx`.
- Calling every safepoint pause GC or every long pause TTSP.
- Disabling compilation or changing thresholds as a first response without evidence.

## Production engineering notes

Java Flight Recorder is a strong first choice for low-overhead visibility into compilation, code cache, execution sampling, allocation, locks, and safepoint/GC activity. Pair it with unified logging for targeted compiler/safepoint questions and OS CPU scheduling evidence. Diagnostic flag names and overhead must be tested on the deployed version.

For startup-sensitive services, consider class loading, framework initialization, profile maturity, and readiness policy together. Declaring readiness before representative code is warm can shift warm-up latency to users. Artificial warm-up can help only if it exercises realistic paths without unsafe external side effects.

For latency spikes, build a timeline:

1. Did application threads run, block, or stop?
2. Was a safepoint requested, and what operation ran?
3. How much was TTSP versus operation duration?
4. Was CPU saturated by GC, compilers, application, or neighbors?
5. Did deoptimization or a compilation storm coincide with a traffic/code-shape change?

Code-cache exhaustion, excessive dynamic class generation, megamorphic call sites, and unstable speculation are advanced hypotheses, not default explanations. Validate them with events and profiles.

## Interview questions and model answers

**Why use both an interpreter and JIT?**

Interpretation or low-tier execution starts quickly and gathers profiles. JIT compilation spends more work on hot paths, using runtime information unavailable to a static compiler. Tiering balances startup, profiling quality, compilation cost, and steady-state speed.

**What is speculative optimization?**

The runtime optimizes for an observed condition, such as one receiver type, while guarding the assumption. If it fails, execution takes a generic path or deoptimizes to reconstructed legal state. Correctness does not depend on the speculation remaining true.

**What is OSR?**

On-stack replacement transfers a currently running invocation, commonly at a hot loop back edge, into compiled code. It avoids waiting for a long-running method to return before benefiting from compilation.

**What is a safepoint?**

It is an implementation safe state used for VM operations needing coordinated managed state. Threads reach it through polls or transitions. Time to reach the safepoint and time spent performing the operation must be measured separately.

**Can JIT optimizations violate Java semantics?**

No. They can exploit all outcomes the JLS/JMM permits, including surprising outcomes in data-racy code, but must preserve specified behavior. Guards, metadata, and deoptimization maintain correctness when assumptions change.

## Exercises

1. Identify potential inlining and constant/range optimizations in the worked example.
2. Add a second `PriceRule` selected unpredictably and reason about monomorphic versus polymorphic profiles.
3. Explain how OSR helps one very long loop invocation.
4. Design a JMH benchmark that consumes results and separates warm-up from measurement.
5. Draw a pause timeline that distinguishes TTSP, VM operation, and application recovery.
6. Explain how an inlined, scalar-replaced object could reappear during deoptimization.

## Chapter summary

Adaptive execution moves code among low-setup and optimized forms using runtime profiles. Inlining unlocks specialization, escape analysis, and check elimination; speculation remains safe through guards and deoptimization. OSR accelerates long-running loops, while the code cache stores generated native code outside the heap. Safepoints coordinate runtime operations and must be analyzed as time-to-safepoint plus operation time. All of these are implementation strategies under Java's semantic contract.

## Revision checklist

- [ ] I can explain interpreter/JIT tiering as a cost trade-off.
- [ ] I can describe inlining, profiles, speculation, OSR, and deoptimization.
- [ ] I know why optimized frames and objects may be reconstructed.
- [ ] I can distinguish Java heap from code cache.
- [ ] I can explain safepoint polling and TTSP.
- [ ] I can propose evidence for warm-up, compilation, and pause investigations.

