# Chapter 4: JVM Architecture

## Learning objectives

- Describe the major JVM subsystems and how they cooperate.
- Distinguish specified JVM runtime concepts from one implementation's process layout.
- Explain the roles of class loading, runtime data areas, execution, garbage collection, JNI, and native libraries.
- Place application, compiler, GC, service, and operating-system threads in one process model.
- Explain safepoints and stop-the-world operations without claiming that every pause is a GC pause.

## Why this matters at SDE-2

A production Java process is not simply "the heap plus application threads." Startup failures, allocation stalls, code-cache exhaustion, native memory growth, class-loader leaks, JNI bugs, and safepoint pauses live in different subsystems. A useful incident hypothesis begins by locating evidence in the correct region.

Architecture is also a common interview gateway. The interviewer is testing whether you can connect components rather than recite a box diagram: a class loader supplies verified code and metadata; stack frames execute bytecodes that reference heap objects; the JIT places machine code in a code cache; the collector coordinates with all threads to discover and update references; JNI crosses into native code with additional safety obligations.

## First-principles model

The JVMS describes an abstract machine. A concrete JVM is a native process that realizes that model while integrating with a host OS and CPU. Its high-level data and control flow is:

```text
                  class/module path, generated classes
                                  |
                                  v
+---------------------- JVM process --------------------------------+
| +---------------- Class-loader subsystem -----------------------+ |
| | locate -> define -> verify -> prepare -> resolve -> initialize | |
| +------------------------------+--------------------------------+ |
|                                v                                  |
| +---------------------- Runtime data ---------------------------+ |
| | Shared: heap, class/method metadata, runtime constant pools   | |
| | Per thread: pc, Java stack/frames, native execution state     | |
| +-----------+----------------------+----------------------------+ |
|             |                      |                              |
| +-----------v-------+    +---------v----------+   +-------------+ |
| | Execution engine  |    | Memory management |   | JNI/native  | |
| | interpreter/JIT   |<-->| allocation + GC   |   | libraries   | |
| | compiled code     |    | reference update  |   | OS services | |
| +---------+---------+    +---------+----------+   +------+------+ |
|           |                        |                     |        |
|   code cache/profiles       safepoint coordination       |        |
+-----------+------------------------+---------------------+--------+
            v                        v                     v
                      CPU, virtual memory, OS
```

The arrows matter. Execution triggers class loading and allocation. GC needs precise knowledge of references in frames and compiled code. Class unloading depends on reachability of defining loaders. Native calls can retain references and block coordination. No subsystem is operationally isolated.

## Core terminology

- **Class-loader subsystem:** Finds class-file bytes and creates runtime class definitions under loader-specific namespaces.
- **Runtime data areas:** Memory regions described by the JVMS, some shared and some per thread.
- **Execution engine:** The implementation that performs method semantics, possibly via interpretation and compilation.
- **Code cache:** Implementation memory holding generated native code and related data.
- **Garbage collector:** Reclaims storage for objects that can no longer affect execution.
- **JNI:** Java Native Interface, a standard native interoperability boundary.
- **Safepoint:** An implementation state where selected runtime invariants allow a global VM operation to inspect or modify managed state.
- **Stop the world (STW):** A period in which relevant Java application threads are paused for a VM operation.
- **VM thread:** An implementation thread coordinating certain JVM-wide operations.

## Detailed mechanics

The class-loader subsystem receives a binary name request, delegates or searches according to loader policy, obtains bytes, and asks the JVM to define a class. The JVM protects type safety through class-file validation and linking. Runtime class identity includes both binary name and defining loader. Initialization executes static initialization logic before specified active uses.

The runtime data areas provide execution state. Each Java thread has a program counter and JVM stack with a frame per active method. Frames hold local-variable slots, an operand stack, and linkage/return information. Objects and arrays usually inhabit a shared heap. Per-class runtime constant pools and method/class structures represent loaded definitions. Native method execution may require a native stack or equivalent host mechanism.

The execution engine obeys bytecode semantics. An implementation can interpret, compile, or mix both. Compiled code still cooperates with the runtime: it contains metadata identifying reference locations, points where a thread can stop, exception mappings, and paths back to less optimized execution.

Memory management handles allocation and reclamation. Allocation often has a fast path, but a failed fast path can request a new allocation region or trigger collection. The collector identifies roots in threads, class metadata, JNI handles, and runtime structures, then traces references. Depending on algorithm, it marks, copies, compacts, or reclaims regions and updates references.

JNI permits native functions to receive Java values and controlled object handles. Native code can call Java methods and use platform libraries. It must respect JNI reference lifetimes and cannot treat managed object addresses as permanently stable. Critical native sections and long native calls can interfere with collection or safepoint coordination depending on implementation.

Threads in a JVM process can include:

- application platform threads mapped to OS threads;
- carrier threads used to run virtual threads;
- GC workers and concurrent marking/relocation workers;
- JIT compiler threads;
- signal, attach, service, timer, and reference-processing threads;
- VM coordination threads;
- threads created by native libraries.

> **Specification boundary:** The JVM specification defines abstract runtime data areas and execution behavior. It does not require HotSpot's Metaspace, named compiler tiers, TLABs, a particular collector, one OS thread per platform thread, or a particular internal thread inventory.

Safepoints support operations that require a stable view, such as some GC phases, deoptimization, class redefinition, biased-lock cleanup in historical releases, or certain diagnostic operations. Threads do not necessarily stop at the exact instant a request is made. They reach a safe state through polling or transitions, and the delay until all required threads arrive is distinct from the duration of the operation itself.

Stop-the-world is a coordination property, not a synonym for full GC. A young collection may pause the world; a mostly concurrent collector still has short pause phases; deoptimization or a diagnostic command may also stop threads. Conversely, much GC work can run concurrently with application threads.

## Worked Java example

```java
import java.util.ArrayList;
import java.util.List;

public final class ArchitectureTour {
    private static final List<byte[]> RETAINED = new ArrayList<>();

    static int compute(int value) {
        return value * 31 + 7;
    }

    public static void main(String[] args) throws Exception {
        for (int i = 0; i < 10_000; i++) {
            compute(i);
        }
        RETAINED.add(new byte[1024 * 1024]);
        Thread worker = new Thread(() -> System.out.println(compute(5)), "worker");
        worker.start();
        worker.join();
    }
}
```

This small program touches every main box: classes are loaded, calls create frames, an array is allocated, a static collection retains it, repeated execution can create JIT profiles, a new application thread appears, and `join` coordinates completion.

## Execution or memory walkthrough

1. The launcher creates a JVM process and establishes the initial thread and runtime services.
2. `ArchitectureTour` and referenced platform classes are loaded and linked. Static initialization creates `RETAINED`.
3. Each `compute` invocation has method state. The optimizing runtime may later inline the calculation into the loop, eliminating a physical call frame in compiled execution.
4. `new byte[...]` requests heap storage. The local reference is passed to `ArrayList.add` and stored in the shared list's backing array.
5. Because `RETAINED` is reachable through a static field of a live class, the byte array remains reachable after `main`'s local expression finishes.
6. `worker.start()` asks the runtime and OS scheduling layer to arrange concurrent execution. Its lambda invokes `compute` and prints.
7. `join()` blocks the main thread until the worker terminates and also establishes a Java Memory Model ordering relationship.
8. At a collection pause, reference maps for frames and compiled code allow the collector to find live references. The exact internal algorithm depends on the chosen collector.

## Complexity and performance

The loop is O(n) time and O(1) Java-level auxiliary space. The retained allocation is O(m) space for an `m`-byte array. Architecture adds non-algorithmic costs: loading and verification, compilation CPU, metadata, thread stacks, heap reservation/commit, and GC work.

JVM process memory is broader than maximum heap:

```text
resident/process memory
  = committed heap portions
  + class metadata
  + code cache
  + thread stacks
  + direct/native buffers
  + GC/compiler/runtime native structures
  + loaded native libraries and allocator overhead
```

This is a conceptual accounting identity, not a promise that OS tools categorize every byte cleanly. Thread count can dominate native stack reservation, and class generation can dominate metadata even when the heap appears stable.

## Edge cases and common mistakes

- Drawing Metaspace inside the heap and calling that a JVMS requirement.
- Claiming every method call creates a visible frame after JIT inlining.
- Assuming an empty Java heap explains low process memory.
- Calling every application pause a GC pause.
- Assuming a concurrent collector never stops the world.
- Treating JNI as ordinary safe Java; native code can corrupt memory or crash the process.
- Forgetting static fields store references, not the referenced objects "inside the class."
- Assuming all JVM threads correspond to business request threads.

## Production engineering notes

Measure the whole process. Heap metrics, native memory tracking where available, thread counts, class counts, code-cache state, direct-buffer usage, GC telemetry, and OS RSS answer different questions. A container limit must cover non-heap memory and transient native needs, not just `-Xmx`.

For long pauses, separate time-to-safepoint from VM-operation time and inspect CPU starvation, native transitions, and thread states. For high CPU, distinguish application, compiler, GC, and native threads using profiles. For class growth, group by class loader. For native crashes, preserve `hs_err`-style error logs and native library versions.

> **HotSpot note:** HotSpot exposes architecture details through JFR, unified logging, `jcmd`, serviceability agents, Native Memory Tracking, and many `-XX` options. Names and availability change by release; they must be validated against the deployed build.

## Interview questions and model answers

**What are the main JVM components?**

I group them as class loading/linking, runtime data areas, execution, memory management, and native integration. Then I explain their interactions: execution uses frames and heap objects; the JIT emits code plus GC metadata; the collector coordinates with threads; loaders define type identity; JNI crosses into host libraries.

**What is a safepoint?**

It is an implementation-defined safe execution state used for global VM operations that need consistent managed state. A safepoint request may pause relevant Java threads, and reaching the safepoint can itself take time. The JLS/JVMS do not mandate HotSpot's safepoint mechanism.

**Does stop-the-world mean full GC?**

No. STW describes thread suspension. Young GC and short phases of concurrent collectors can be STW, and non-GC VM operations can also require it. A full GC describes collection scope or a collector-specific event, not every pause.

**Why can process memory exceed `-Xmx`?**

`-Xmx` limits the Java heap, not class metadata, code cache, thread stacks, native buffers, JVM structures, libraries, or allocator overhead.

## Exercises

1. Redraw the architecture diagram and add an arrow for every allocation and native call.
2. Run the example, capture a thread dump, and classify visible threads as application or runtime support.
3. Explain why the retained byte array survives collection.
4. Build a memory budget for a 1 GiB container with a 600 MiB heap.
5. Give three STW operations, at least one not caused by GC.

## Chapter summary

A JVM is an interacting runtime, not a single bytecode loop. Class loading establishes definitions and identity; runtime data areas hold shared and per-thread state; the execution engine interprets or compiles; memory management allocates and reclaims; JNI connects native facilities. Safepoints coordinate operations requiring stable managed state. The abstract specification and a concrete HotSpot process must be discussed at different levels.

## Revision checklist

- [ ] I can explain the full JVM architecture as a connected system.
- [ ] I can separate shared runtime state from per-thread state.
- [ ] I can identify non-heap contributors to process memory.
- [ ] I can distinguish a safepoint, an STW pause, and a full GC.
- [ ] I understand why JIT code and GC require shared metadata.
- [ ] I label HotSpot-specific structures as implementation details.

