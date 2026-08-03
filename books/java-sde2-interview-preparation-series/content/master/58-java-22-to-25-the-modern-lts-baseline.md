# 58. Java 22 to 25: The Modern LTS Baseline

## Learning objectives

By the end of this chapter, you should be able to:

- state what changed between the Java 21 and Java 25 long-term-support baselines;
- use stream gatherers, the Class-File API, and the Foreign Function and Memory API where they replace older approaches;
- explain why virtual threads behave differently under `synchronized` from Java 24 onward;
- distinguish a finalized feature from a preview feature and justify a production policy for each; and
- answer "what version should we target" with reasoning rather than a number.

## Why this matters at SDE-2

This book's baseline is Java 21, the LTS release most teams standardized on. Java 25, released 16 September 2025, is the next LTS, and the four releases between them changed several things an SDE-2 is expected to reason about - most consequentially, the pinning behavior of virtual threads under `synchronized`.

Interviewers do not test JEP numbers. They test whether you can say what a feature is *for*, whether you know the difference between "previewed" and "shipped," and whether you have an opinion about upgrade cadence. An engineer who says "we're on 21 and we'll move to 25 once our agent vendors certify it, mainly for the `synchronized` pinning fix" is demonstrating exactly the judgment the level requires.

## First-principles model

Since Java 9, releases ship every six months on a fixed date. Features are not held for a release; a release takes whatever is ready. Roughly every two years a release is designated **long-term support**, which is a *vendor* commitment to backported security and bug fixes, not a technical property of the JDK. Java 8, 11, 17, 21, and 25 are the LTS line.

Language and API changes arrive through a staged process:

- **Incubator** - an API in a separate `jdk.incubator` module, expected to change.
- **Preview** - a fully specified feature, disabled by default, requiring `--enable-preview` at compile *and* run time. Preview class files are tied to the exact JDK version that produced them.
- **Final** - permanent, subject to normal compatibility rules.

The staging matters commercially. Preview features can be revised or withdrawn: String Templates previewed in Java 21 and 22, then were removed entirely before Java 23 because the design was not right. Anyone who had shipped code depending on them had to rewrite it. That is the argument for the policy stated later in this chapter.

> **Specification boundary:** Java specifies the language and API changes in each release, and the preview mechanism that gates them. It does not specify support duration, backport policy, or which vendor's build you run. "LTS" is a distribution commitment from Oracle, Temurin, Amazon, Red Hat, and others - their support windows differ, and so do their patch levels.

## Core terminology

- **LTS:** a release vendors commit to supporting for years; 8, 11, 17, 21, 25.
- **Preview feature:** fully specified but provisional; requires `--enable-preview`.
- **Incubator module:** provisional API under `jdk.incubator.*`.
- **Pinning:** a virtual thread that cannot unmount, blocking its carrier platform thread.
- **Gatherer:** a user-defined intermediate stream operation.
- **Arena:** the lifetime scope controlling native memory in the FFM API.
- **Compact object headers:** 8-byte object headers instead of 12 or 16.
- **AOT cache:** ahead-of-time class loading and linking state captured from a training run.

## Detailed mechanics

### Java 22 - Foreign Function and Memory API (JEP 454, final)

The FFM API reached final status in Java 22, giving Java a supported way to call native code and access off-heap memory without JNI.

```java
import java.lang.foreign.*;
import java.lang.invoke.MethodHandle;

try (Arena arena = Arena.ofConfined()) {
    MemorySegment name = arena.allocateFrom("world");

    Linker linker = Linker.nativeLinker();
    MethodHandle strlen = linker.downcallHandle(
            linker.defaultLookup().find("strlen").orElseThrow(),
            FunctionDescriptor.of(ValueLayout.JAVA_LONG, ValueLayout.ADDRESS));

    long length = (long) strlen.invoke(name);   // 5
}
```

Two things make this a genuine replacement for JNI rather than a variation on it. There is no C shim to compile and ship - the binding is expressed in Java. And memory lifetime is explicit: an `Arena` is a scope, closing it frees every segment allocated from it, and touching a freed segment throws `IllegalStateException` rather than corrupting the heap. `Arena.ofConfined()` further restricts access to the creating thread, so use-after-free and cross-thread races become exceptions instead of undefined behavior.

This is also the intended replacement for the parts of `sun.misc.Unsafe` that deal with memory access, which were deprecated for removal in Java 23 and now warn at runtime.

**Unnamed variables and patterns (JEP 456, final)** let `_` stand for a binding you must declare but will not use:

```java
try {
    process(order);
} catch (IOException _) {          // named but unused
    metrics.increment("io.failure");
}

for (var _ : items) { count++; }

if (obj instanceof Point(int x, _)) { ... }   // ignore the second component
```

Small, but it removes a category of misleading names and lets the compiler verify that an ignored binding really is ignored.

### Java 23 - Markdown in Javadoc (JEP 467, final)

Documentation comments may be written in Markdown using `///`:

```java
/// Returns the customer's outstanding balance.
///
/// - Never negative.
/// - Excludes pending authorizations.
///
/// @param id the customer identifier
public Money balance(CustomerId id) { ... }
```

The practical effect is that documentation gets written, because the friction of HTML tags in comments was real.

**ZGC became generational by default**, and the non-generational mode was removed in Java 24. If you tuned ZGC flags on 21, revisit them.

### Java 24 - the release that matters most operationally

**Synchronize virtual threads without pinning (JEP 491)** is the single most important change between the two LTS releases.

In Java 21, a virtual thread that blocked inside a `synchronized` block or method could not unmount from its carrier platform thread. The carrier stayed pinned for the duration. With a default scheduler parallelism equal to the core count, a handful of virtual threads blocking inside `synchronized` on a database call could stall every other virtual thread in the JVM. The recommended workaround was to replace `synchronized` with `ReentrantLock` in any code path a virtual thread might block on - a real, invasive migration cost.

From Java 24, virtual threads unmount correctly while holding a monitor. The workaround becomes unnecessary, and the most common reason for virtual-thread adoption to disappoint goes away.

```java
// On Java 21 this pins the carrier for the duration of the call.
// From Java 24 the virtual thread unmounts and the carrier is reused.
synchronized (lock) {
    result = jdbcTemplate.query(...);
}
```

Pinning still occurs inside native frames and class initializers. But if your team evaluated virtual threads on 21, hit carrier starvation, and shelved them, the evaluation is worth repeating on 25.

**Stream gatherers (JEP 485, final)** finally make intermediate stream operations extensible. Before this, you could write any terminal operation you liked via `Collector`, but the intermediate stage was a closed set.

```java
// Sliding window of 3 - impossible to express before gatherers
List<List<Integer>> windows = Stream.of(1, 2, 3, 4, 5)
        .gather(Gatherers.windowSliding(3))
        .toList();
// [[1, 2, 3], [2, 3, 4], [3, 4, 5]]

// Fixed batches, useful for chunked writes
List<List<String>> batches = ids.stream()
        .gather(Gatherers.windowFixed(500))
        .toList();
```

Built-in gatherers cover `fold`, `scan`, `mapConcurrent`, `windowFixed`, and `windowSliding`. `Gatherer.of` builds custom ones with an initializer, an integrator, an optional combiner for parallel streams, and a finisher. `mapConcurrent` deserves particular attention: it runs a mapping function on virtual threads with a concurrency limit, which is a clean way to fan out I/O without hand-managing an executor.

**The Class-File API (JEP 484, final)** gives the JDK a supported library for parsing and generating class files, replacing the widespread dependency on ASM. If you work on agents, instrumentation, or build tooling, this removes a dependency that had to be upgraded in lockstep with every JDK.

**Ahead-of-time class loading and linking (JEP 483)** records the loaded and linked state of classes from a training run and replays it at startup, cutting JVM startup time substantially for large applications. Java 25 adds command-line ergonomics (JEP 514) and method profiling (JEP 515) on top.

**The Security Manager was permanently disabled** (JEP 486). It had been deprecated since Java 17; code still calling `System.setSecurityManager` now fails.

### Java 25 - the new LTS

**Scoped values (JEP 506, final)** provide immutable, inheritable, bounded data sharing - the replacement for `ThreadLocal` in a virtual-thread world.

```java
private static final ScopedValue<RequestContext> CONTEXT = ScopedValue.newInstance();

ScopedValue.where(CONTEXT, new RequestContext(traceId, tenantId))
           .run(() -> handleRequest());   // CONTEXT.get() visible to everything called here

// Outside the run block, CONTEXT.get() throws NoSuchElementException
```

`ThreadLocal` was designed for a few hundred pooled platform threads. It is mutable, unbounded in lifetime, inherited by copying, and a well-known leak source. With millions of virtual threads, copying an inheritable `ThreadLocal` map per thread is untenable. A `ScopedValue` is immutable, its lifetime is exactly the dynamic extent of the `run` call, and child threads in a structured scope share the binding rather than copying it.

**Compact object headers (JEP 519)** move from experimental to production-ready, reducing object headers from 12 or 16 bytes to 8. For allocation-heavy applications with many small objects this is a real heap reduction - commonly reported in the 10-20% range - and it is enabled with `-XX:+UseCompactObjectHeaders`.

**Module import declarations (JEP 511, final)** import every package a module exports:

```java
import module java.base;   // java.util, java.io, java.nio.file, ... all visible

List<String> names = new ArrayList<>();
Path path = Path.of("orders.csv");
```

Useful in scripts and teaching material. In a production codebase, explicit imports still document dependencies better.

**Compact source files and instance main methods (JEP 512, final)** finish the four-release effort to shorten the first program a learner writes:

```java
void main() {
    IO.println("Hello");
}
```

No class declaration, no `static`, no `String[] args`, no `System.out`. Combined with `java Program.java` and multi-file source launching from Java 22, Java is now genuinely scriptable.

**Flexible constructor bodies (JEP 513, final)** allow statements before `super(...)` or `this(...)`, provided they do not reference the instance under construction:

```java
public Order(List<Item> items) {
    if (items.isEmpty()) {                    // validate BEFORE the super call
        throw new IllegalArgumentException("order requires at least one item");
    }
    super(computeTotal(items));
    this.items = List.copyOf(items);
}
```

Previously this validation had to be smuggled into a static helper inside the `super` argument list. The change also lets you avoid the classic bug where a superclass constructor calls an overridable method before the subclass has initialized its fields.

**Still preview in Java 25:** structured concurrency (fifth preview), stable values, primitive types in patterns, PEM encodings. Structured concurrency in particular has been through five previews with API changes each time - a reminder that "preview" is a real warning, not a formality.

### Java 26 and beyond

Java 26 reached general availability on 17 March 2026 with a frozen feature set including HTTP/3 support in the HTTP client (JEP 517), removal of the Applet API (JEP 504), ahead-of-time object caching with any GC (JEP 516), and "prepare to make final mean final" (JEP 500) - which begins closing the loophole allowing reflective mutation of `final` fields. It is not an LTS release. The next LTS is expected to be Java 29 in September 2027.

## Worked Java example

A batching pipeline that would have needed hand-written iterator code before Java 24, combining gatherers with `mapConcurrent`.

```java
import java.util.List;
import java.util.stream.Gatherers;
import java.util.stream.Stream;

public final class BulkEnricher {

    record Customer(String id, String name) {}

    /**
     * Fetches customers in batches of 100, enriching up to 8 batches
     * concurrently on virtual threads, and flattens the result.
     */
    public List<Customer> enrichAll(List<String> ids) {
        return ids.stream()
                .gather(Gatherers.windowFixed(100))          // List<String> batches
                .gather(Gatherers.mapConcurrent(8, this::fetchBatch))
                .flatMap(List::stream)
                .toList();
    }

    private List<Customer> fetchBatch(List<String> batch) {
        // One network round trip per batch; blocking is fine on a virtual thread.
        return batch.stream().map(id -> new Customer(id, "name-" + id)).toList();
    }

    public static void main(String[] args) {
        List<String> ids = Stream.iterate(1, i -> i + 1).limit(250)
                .map(i -> "c-" + i).toList();
        List<Customer> all = new BulkEnricher().enrichAll(ids);
        System.out.println(all.size());        // 250
        System.out.println(all.getFirst());    // Customer[id=c-1, name=name-c-1]
    }
}
```

Three batches of 100, 100, and 50 are produced by `windowFixed`; `mapConcurrent` runs up to eight of them at a time on virtual threads while preserving encounter order in the output. Before gatherers, expressing "batch then fan out with a concurrency limit" as a stream was not possible - you dropped to a loop with an `ExecutorService` and reassembled the ordering yourself.

## Execution or memory walkthrough

`windowFixed(100)` is a stateful gatherer. Its integrator accumulates elements into a private buffer, and when the buffer reaches 100 it pushes the list downstream and starts a new one. The finisher pushes the trailing partial window - the 50-element remainder. Because it holds mutable state, it declares itself unsuitable for parallel splitting; the stream stays sequential regardless of `parallel()`.

`mapConcurrent(8, fn)` maintains a bounded set of in-flight tasks, each on its own virtual thread, and a queue that preserves encounter order. When the ninth batch arrives, the gatherer blocks the calling thread until a slot frees. Blocking here is cheap precisely because the workers are virtual threads: each unmounts from its carrier while waiting on I/O, so eight concurrent network calls consume eight heap-allocated stacks rather than eight OS threads.

On Java 21 the same code would work but any `synchronized` block inside `fetchBatch` - including one inside a JDBC driver - could pin a carrier thread. On Java 25 the virtual thread unmounts and the carrier is returned to the pool.

`flatMap(List::stream).toList()` allocates the final list once, sized from the spliterator estimate where available.

## Complexity and performance

Gatherers add no asymptotic cost: `windowFixed` is O(n) time and O(w) space for window size w; `windowSliding` is O(n-w) in output volume, which is inherent to producing overlapping windows. `mapConcurrent` bounds memory by its concurrency limit rather than the input size - the reason to prefer it over submitting every element to an executor.

FFM downcalls cost roughly what a JNI call costs for simple signatures and less for many, because there is no JNI shim and the linker can specialize the handle. Hoist `MethodHandle` construction to a `static final` field; building it per call dominates everything else.

Compact object headers reduce heap by 4 or 8 bytes per object. For a workload dominated by small objects - nodes, boxed values, short strings - this is a double-digit percentage of live heap, and it improves cache density as well as footprint.

AOT class loading and linking mainly moves work out of startup. It does not change steady-state throughput; it changes time-to-first-request, which is what matters for scale-to-zero and serverless deployments.

> **HotSpot note:** the virtual-thread pinning fix in JEP 491 is a JVM change, not a library change. Recompilation is not required - running existing Java 21 bytecode on a Java 25 JVM gets the improved behavior. This is the general shape of runtime improvements: upgrading the JVM often delivers more than adopting new syntax.

## Edge cases and common mistakes

- Treating a preview feature as stable. String Templates previewed twice and were withdrawn; anyone who shipped on them rewrote that code.
- Forgetting that `--enable-preview` is required at both compile time and run time, and that preview class files are pinned to the exact JDK version.
- Assuming "LTS" is a JDK property. It is a vendor support commitment, and windows differ by vendor.
- Concluding virtual threads are unusable after testing on Java 21 and hitting `synchronized` pinning that Java 24 fixed.
- Believing Java 24 removed pinning entirely. Native frames and class initializers still pin.
- Using `ThreadLocal` for request context with virtual threads. Prefer `ScopedValue` in Java 25.
- Using a stateful gatherer and expecting `parallel()` to help. Stateful gatherers suppress splitting.
- Building an FFM `MethodHandle` inside the hot path instead of hoisting it.
- Letting an `Arena` escape its try-with-resources scope, then touching a freed `MemorySegment`.
- Using `Arena.ofShared()` when `ofConfined()` suffices, giving up the thread-confinement checks.
- Still calling `System.setSecurityManager`, which fails from Java 24.
- Tuning non-generational ZGC flags that no longer exist after Java 24.
- Enabling `-XX:+UseCompactObjectHeaders` without measuring; benefits depend heavily on object size distribution.
- Assuming an agent, profiler, or bytecode-manipulation library works across a major upgrade. These break most often, and they are usually what actually blocks an upgrade.

## Production engineering notes

Adopt finalized features freely and preview features never - in production code. Preview is genuinely useful for evaluation and for giving feedback to the JDK team, and that work belongs in a spike branch, not in a service you are on call for.

Separate the JVM upgrade from the language-level upgrade. Running Java 21 bytecode on a Java 25 JVM delivers the pinning fix, generational ZGC, compact object headers, and the AOT startup work with no source changes. Bumping `--release` to 25 is a second, independent decision. Doing them together makes a regression twice as hard to attribute.

Inventory your agents first. Profilers, APM agents, mocking frameworks, and anything using ASM or reaching into `sun.misc.Unsafe` are the usual blockers. The Class-File API existing does not mean your dependencies have migrated to it.

Expect `sun.misc.Unsafe` memory-access warnings on 23 and later. They are deprecation notices pointing at FFM and `VarHandle` replacements. Find which dependency triggers them before removal becomes mandatory rather than after.

Re-evaluate virtual threads on 25 if you shelved them on 21. Also re-run any performance baseline: generational ZGC, compact object headers, and improved G1 synchronization all shift the numbers your capacity model was built on.

## Interview questions and model answers

**What changed between Java 21 and Java 25 that would affect an existing service?**

Operationally, the largest change is JEP 491 in Java 24: virtual threads no longer pin their carrier thread when blocking inside `synchronized`. Beyond that, ZGC became generational by default, compact object headers became production-ready, the Security Manager was permanently disabled, and ahead-of-time class loading cut startup time. Most of these arrive by upgrading the JVM alone, without recompiling.

**What is a preview feature and how should a team treat it?**

A fully specified but provisional feature, disabled unless `--enable-preview` is passed at both compile and run time, with class files tied to the exact JDK version. Treat it as unusable in production: String Templates previewed in Java 21 and 22 and were then withdrawn entirely.

**What problem do stream gatherers solve?**

They make intermediate stream operations extensible. `Collector` always allowed custom terminal operations, but intermediate stages were a fixed set, so operations like sliding windows or fixed batching required dropping out of the stream. `Gatherers.windowFixed`, `windowSliding`, `fold`, `scan`, and `mapConcurrent` cover the common cases, and `Gatherer.of` handles the rest.

**When would you use `ScopedValue` instead of `ThreadLocal`?**

For request-scoped context, especially with virtual threads. `ScopedValue` is immutable, bounded to the dynamic extent of a `run` call, and shared rather than copied into child threads in a structured scope. `ThreadLocal` is mutable, unbounded in lifetime, a common leak source, and its inheritable form copies per thread, which does not scale to millions of virtual threads.

**Why is the Foreign Function and Memory API better than JNI?**

There is no C shim to write, compile, and ship per platform. Memory lifetime is explicit through `Arena`, so freeing is deterministic and use-after-free throws `IllegalStateException` instead of corrupting memory. Confined arenas add thread-confinement checks, converting a class of undefined behavior into exceptions.

**Should a team upgrade from Java 21 to Java 25?**

Usually yes, but as two decisions. Upgrade the JVM first for the pinning fix, GC improvements, and startup work with no code change; then decide separately about `--release 25`. The blocker is almost always agents and bytecode-manipulation libraries, so inventory those first.

## Exercises

1. Write a program using `synchronized` around a blocking call inside a virtual thread. Run it on Java 21 and Java 25 with `-Djdk.tracePinnedThreads` and compare.
2. Implement a sliding-window moving average with `Gatherers.windowSliding`, then reimplement it without gatherers and compare readability.
3. Build a custom `Gatherer` that emits an element only when it differs from the previous one, and explain why it cannot split for parallel streams.
4. Call a native function through the FFM API using a confined `Arena`. Then let a `MemorySegment` escape the scope and observe the exception.
5. Replace a `ThreadLocal`-based request context with `ScopedValue` and describe what changes for child threads.
6. Measure heap usage of a workload with many small objects before and after `-XX:+UseCompactObjectHeaders`.
7. Compile a class using a preview feature, then run it on a different JDK 25 build and explain the failure.
8. Rewrite a constructor that validates arguments through a static helper inside `super(...)` using flexible constructor bodies.

## Chapter summary

Java ships every six months and designates an LTS roughly every two years; Java 25 is the successor to the Java 21 baseline this book targets. Between them, Java 22 finalized the Foreign Function and Memory API and unnamed variables, Java 23 added Markdown Javadoc and made ZGC generational by default, Java 24 delivered the operationally decisive change - virtual threads no longer pin their carrier under `synchronized` - plus stream gatherers, the Class-File API, and AOT class loading, and Java 25 finalized scoped values, flexible constructor bodies, module imports, and compact source files while promoting compact object headers to production. Most of this value arrives from upgrading the JVM rather than the language level, which argues for separating those two decisions. Finalized features are safe; preview features are not, and String Templates' withdrawal after two previews is the evidence. The practical upgrade blocker is rarely the language - it is agents, profilers, and bytecode libraries.

## Revision checklist

- [ ] I can name the LTS releases and explain that LTS is a vendor commitment.
- [ ] I can explain the difference between incubator, preview, and final, and cite a withdrawn preview.
- [ ] I know that `--enable-preview` is required at compile and run time and pins the class file version.
- [ ] I can explain the Java 24 virtual-thread pinning fix and what still pins.
- [ ] I can use `windowFixed`, `windowSliding`, and `mapConcurrent`.
- [ ] I can justify `ScopedValue` over `ThreadLocal` for request context.
- [ ] I can describe FFM arenas and why they beat JNI.
- [ ] I know compact object headers, generational ZGC, and AOT class loading are JVM-level wins.
- [ ] I can argue for separating a JVM upgrade from a language-level upgrade.
- [ ] I know agents and bytecode libraries are the usual upgrade blocker.
