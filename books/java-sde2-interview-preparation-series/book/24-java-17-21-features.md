# 24. Java 17 and Java 21 Language and Platform Features

## Learning objectives

By the end of this chapter, you should be able to:

- identify the high-impact permanent features introduced in Java 17 and Java 21;
- distinguish permanent, preview, and incubator status as of each release;
- compile and run preview or incubator code with appropriate controls;
- evaluate virtual threads, sequenced collections, pattern matching, and platform changes; and
- plan a Java 17 to Java 21 migration without confusing language compatibility with operational readiness.

## Why this matters at SDE-2

Java 17 and Java 21 are common long-term-support deployment baselines, although support duration is a vendor distribution policy rather than a Java language property. SDE-2 engineers are expected to know which newer constructs improve a service and which are experimental in a given release.

A migration is not complete because source compiles. Strong encapsulation can break reflection, virtual threads can change workload shape, and preview class files require runtime flags.

## First-principles model

Java evolves through language, library, JVM, tooling, security, and platform changes. A feature has a status within a specific release:

- **Permanent:** standard in that release and does not require preview flags.
- **Preview:** fully specified for feedback but not permanent; source and class files require opt-in and may change or disappear.
- **Incubator:** non-final API delivered in a `jdk.incubator.*` module for experimentation; compatibility is not promised.

Preview in one release does not imply the same syntax or API in another. Code must be judged against its exact source release. A feature finalized in Java 21 is ordinary Java 21 even if its ancestors were preview in earlier releases.

> **Specification boundary:** "LTS" is not a Java Language Specification status. Vendors decide support offerings and timelines. Permanent, preview, and incubator status is release-specific; never describe a preview API as standardized merely because a production JDK contains it.

## Core terminology

- **Feature release:** numbered Java platform release with its own source/class-file level.
- **LTS:** vendor commitment to extended maintenance for selected releases.
- **Preview feature:** opt-in language or VM feature intended for real-world feedback.
- **Incubator module:** non-final API module shipped for experimentation.
- **Strong encapsulation:** enforcement of module boundaries around JDK internals.
- **Virtual thread:** lightweight Java thread scheduled by the runtime rather than permanently bound one-to-one to an OS thread.
- **Sequenced collection:** collection with defined encounter order and first/last/reversed operations.
- **Pattern matching:** combined testing and extraction based on value shape or type.
- **Generational ZGC:** Z Garbage Collector mode exploiting object-age behavior.
- **Migration baseline:** source, target bytecode, runtime, dependencies, tools, and operations agreed for deployment.

## Detailed mechanics

### Java 17 permanent features

Java 17 made sealed classes and interfaces permanent. They use `sealed`, `permits`, `final`, and `non-sealed` to define controlled direct inheritance. Pattern matching for `instanceof`, records, text blocks, and switch expressions were already permanent before 17 and are part of a practical Java 17 language baseline.

Java 17 restored always-strict floating-point evaluation. The older `strictfp` distinction became unnecessary for ordinary source semantics: floating-point expressions use consistently strict behavior. Existing `strictfp` code remains source-compatible but the modifier is no longer needed for this purpose.

Java 17 strongly encapsulated JDK internals, with limited critical exceptions. Applications depending on `sun.*` members or illegal deep reflection may fail and should migrate to supported APIs. `--add-opens` can be a temporary operational bridge, not a durable application API.

### Java 17 preview and incubator status

As of Java 17:

- Pattern matching for `switch` was **preview**. It required preview flags, and its guarded-pattern syntax and null semantics evolved before finalization.
- The Foreign Function and Memory API was **incubator**, in `jdk.incubator.foreign`.
- The Vector API was in its **second incubator** round, in `jdk.incubator.vector`.

Do not write Java 21 `when` guards and call them Java 17 syntax. Do not assume Java 17 incubator foreign-memory source migrates unchanged to the later `java.lang.foreign` API.

Preview compilation and execution typically use matching JDKs:

```bash
javac --enable-preview --release 17 Example.java
java --enable-preview Example
```

Incubator modules require explicit module resolution, for example:

```bash
javac --add-modules jdk.incubator.vector VectorExample.java
java --add-modules jdk.incubator.vector VectorExample
```

Build tools, tests, IDEs, packaging, and runtime launchers all need consistent configuration. Preview class files are marked so a runtime does not silently run them without preview opt-in.

### Java 21 permanent language and collection features

Java 21 finalized pattern matching for switch. Type patterns, explicit null cases, exhaustive sealed-hierarchy coverage, and `when` guards allow declarative variant handling. It also finalized record patterns, including nested deconstruction. Neither requires `--enable-preview` on Java 21.

```java
sealed interface Event permits Login, Logout {}
record Login(String user) implements Event {}
record Logout(String user) implements Event {}

static String user(Event event) {
    return switch (event) {
        case Login(String name) -> name;
        case Logout(String name) -> name;
    };
}
```

Java 21 introduced sequenced collection interfaces: `SequencedCollection`, `SequencedSet`, and `SequencedMap`. They provide uniform first, last, and reversed operations for ordered collections. Existing types such as lists, deques, linked hash sets, sorted sets, linked hash maps, and sorted maps participate according to their contracts.

```java
java.util.List<String> queue = new java.util.ArrayList<>();
queue.addLast("second");
queue.addFirst("first");
System.out.println(queue.getFirst());       // first
System.out.println(queue.reversed());       // [second, first]
```

`reversed()` is generally a reverse-ordered view, not an independent deep copy. Mutability and write-through behavior depend on the backing collection's contract.

### Virtual threads in Java 21

Virtual threads became permanent in Java 21 after previews in Java 19 and 20. They implement the `Thread` API and are well suited to high-concurrency workloads that spend substantial time blocking on I/O. They preserve a straightforward thread-per-task programming model.

Virtual threads are not automatically faster for CPU-bound work and do not remove downstream capacity limits. Database pools, HTTP peers, file descriptors, memory, and rate limits still need bounds. Do not pool virtual threads merely to reduce thread count; use one per task, then bound the scarce resource itself.

As of Java 21, blocking while holding a monitor in certain operations or executing native/foreign calls can pin a virtual thread to its carrier, reducing scalability. Pinning is a performance condition, not a correctness failure. Measure with Java Flight Recorder and relevant diagnostics, keep synchronized regions short, and avoid holding locks across blocking I/O.

Thread-local values work, but creating millions of virtual threads can make large or mutable thread-local state expensive. Prefer explicit context propagation where practical; scoped values were preview in Java 21 as a structured alternative.

### Java 21 JVM and operational changes

Generational ZGC became available in Java 21. It separates young and old generations to exploit the observation that most objects die young. In Java 21 it is selected with ZGC plus the generational option; collector defaults and flag evolution are release-specific, so validate exact deployment commands against the target JDK.

Virtual-thread observability requires tools that understand large thread populations. Do not assume one platform thread per request in dashboards or capacity formulas.

### Java 21 preview and incubator status

As of Java 21, the following were **preview**, not permanent:

- String Templates, first preview;
- Unnamed Patterns and Variables, first preview;
- Unnamed Classes and Instance Main Methods, first preview;
- Scoped Values, preview;
- Structured Concurrency, preview; and
- Foreign Function and Memory API, third preview.

The Vector API was in its **sixth incubator** round, not preview or permanent.

String templates combined literal text, expressions, and a processor; preview syntax such as `STR."Hello \{name}"` did not make SQL safe automatically. Unnamed patterns use `_` for intentionally unused bindings. Scoped values provide bounded context, while structured concurrency groups related subtasks and their cancellation. FFM moved to the `java.lang.foreign` API family in Java 21 and was not source-compatible with Java 17's incubator API. All remain subject to their status above.

### Migration and compilation boundaries

`--release 17` compiles against the documented Java 17 API and emits compatible class files even when the compiler itself is newer. Merely using `-source 17 -target 17` can accidentally compile against newer boot APIs in some workflows; prefer `--release` when targeting an older platform.

Upgrade in layers:

1. inventory JDK distributions, CPU/OS support, containers, agents, flags, build tools, and libraries;
2. run the existing Java 17 source and bytecode on Java 21 in staging;
3. remove illegal internal API/reflection dependencies and obsolete JVM flags;
4. update CI, static analysis, test frameworks, profilers, and runtime images;
5. establish behavioral and performance baselines; then
6. adopt Java 21 source features deliberately.

Runtime and source-language upgrades need not occur in one deployment.

## Worked Java example

This Java 21 program combines permanent record patterns, pattern switch, and virtual threads. It uses no preview feature.

```java
import java.util.List;
import java.util.concurrent.Executors;

public class Java21Demo {
    sealed interface Request permits Read, Write {}
    record Read(String key) implements Request {}
    record Write(String key, String value) implements Request {}

    static String execute(Request request) {
        return switch (request) {
            case Read(String key) -> "read " + key;
            case Write(String key, String value) when value.isBlank() ->
                    "reject blank write to " + key;
            case Write(String key, String value) -> "write " + key + "=" + value;
        };
    }

    public static void main(String[] args) throws Exception {
        List<Request> requests = List.of(new Read("a"), new Write("b", "2"));
        try (var executor = Executors.newVirtualThreadPerTaskExecutor()) {
            var futures = requests.stream()
                    .map(request -> executor.submit(() -> execute(request)))
                    .toList();
            for (var future : futures) {
                System.out.println(future.get());
            }
        }
    }
}
```

Compile it with `javac --release 21 Java21Demo.java`; no `--enable-preview` is needed.

## Execution or memory walkthrough

The two records are final permitted implementations. The request list preserves encounter order. Each submitted callable is assigned its own virtual thread by the executor. The runtime schedules virtual threads onto carrier platform threads and can unmount them around supported blocking operations.

The Read task matches and deconstructs its record arm. The Write task first tests the guarded blank arm; the guard is false, so it matches the unguarded Write arm. The main thread obtains futures in list order, so printing is deterministic here even if task completion order differs.

Closing the executor waits for submitted tasks under its `ExecutorService` close contract. A production workflow still needs deadlines, cancellation, partial-failure policy, and downstream concurrency bounds.

## Complexity and performance

New syntax does not change underlying algorithmic complexity. Pattern matching replaces explicit tests and casts with compiler-checked forms. Sequenced view operations are typically O(1), but exact operation complexity follows each collection implementation.

Virtual threads reduce the resource cost of large numbers of blocking threads; they do not reduce CPU work or remote latency. Total memory still scales with live task state. Generational ZGC trades CPU, throughput, footprint, and pause characteristics differently from other collectors and must be load-tested with the service's allocation profile.

> **HotSpot note:** Carrier scheduling, virtual-thread continuation representation, JIT decisions, ZGC implementation, and diagnostic event details are HotSpot behavior. Code should rely on Thread and API contracts, while capacity plans should measure the exact JDK build and flags deployed.

## Edge cases and common mistakes

- LTS is a vendor support designation, not a different language edition.
- Preview source and runtime must use matching release configuration.
- Java 17 pattern switch is preview; Java 21 pattern switch is permanent and uses final `when` guard syntax.
- Record patterns are permanent in 21 and unavailable in 17.
- String templates, scoped values, structured concurrency, unnamed features, and FFM are preview in 21.
- The Vector API is incubator in both releases discussed, at different iterations.
- Virtual threads do not justify unbounded database or network load.
- Long blocking operations while holding monitors can pin carriers in Java 21.
- `reversed()` commonly returns a view, so backing mutations can be visible.
- Strong encapsulation means a successful `--add-opens` workaround is migration debt, not a public contract.
- New JDK execution does not guarantee old agents, flags, or profilers remain compatible.

## Production engineering notes

Define one approved JDK distribution and patch policy per environment. Record runtime version, flags, container limits, collector, and agents in deploy metadata. Test upgrades with production-like traffic and compare latency percentiles, CPU, allocation, GC, thread pinning, connection pools, and error rates.

Use virtual threads where blocking code dominates and simpler thread-per-request structure helps. Keep concurrency controls at scarce resources, propagate cancellation, and test every dependency for blocking and thread-local assumptions. Avoid a wholesale asynchronous-to-virtual rewrite without comparative evidence.

Keep preview features behind experiments or internal modules unless the organization accepts upgrade churn and flags across the full toolchain. Never publish a reusable Java 21 library that silently requires preview class files. For permanent features, adopt style guidance so patterns and record deconstruction improve clarity rather than compressing complex business rules.

## Interview questions and model answers

**Which major language features are permanent in Java 17?**

Sealed classes became permanent in 17. Records, text blocks, switch expressions, and pattern matching for `instanceof` had finalized earlier and are available in the Java 17 baseline. Pattern switch itself is only preview in 17.

**What became permanent in Java 21?**

Pattern matching for switch, record patterns, virtual threads, and sequenced collections are permanent Java 21 features or APIs. They require no preview flag.

**Are virtual threads faster than platform threads?**

Not inherently. They improve scalability and simplify code for many concurrent blocking tasks by reducing per-thread resource cost. CPU-bound throughput remains bounded by cores, and downstream capacity still needs limits.

**What is the difference between preview and incubator?**

Preview features are platform language, VM, or API features that require explicit opt-in and may evolve. Incubator APIs live in `jdk.incubator` modules, require module resolution, and are also non-final with intentionally weak compatibility promises.

**How would you migrate from 17 to 21 safely?**

First run existing source on a Java 21 runtime after updating dependencies, tools, agents, flags, and encapsulation workarounds. Establish functional and performance baselines. Then adopt source features and virtual threads incrementally, with concurrency limits, diagnostics, and rollback.

## Exercises

1. Classify every feature in this chapter as permanent, preview, or incubator in Java 17 and Java 21.
2. Compile the Java 21 demo without preview flags and verify its class-file target through `javap -verbose`.
3. Convert an executor-based blocking service to virtual threads while retaining a semaphore around a 20-connection database pool.
4. Replace first/last list indexing utilities with sequenced operations and test empty behavior and reversed-view mutation.
5. Inventory a Java 17 service for `--add-opens`, internal JDK APIs, agents, removed flags, and old build plugins.
6. Design a benchmark and load-test plan comparing platform-thread pools, virtual threads, and existing async code for one real I/O workload.

## Chapter summary

Java 17 provides a mature baseline with permanent sealed types and strong encapsulation, while its pattern switch is preview and FFM and Vector APIs are incubating. Java 21 permanently adds pattern switch, record patterns, sequenced collections, and virtual threads, alongside operational advances such as generational ZGC. Several Java 21 features remain preview, and Vector remains incubator. Safe adoption separates runtime migration from source migration and validates dependencies, flags, concurrency, observability, and workload performance.

## Revision checklist

- [ ] I distinguish vendor LTS policy from Java feature status.
- [ ] I can name permanent Java 17 and Java 21 language features.
- [ ] I accurately label preview and incubator features in each release.
- [ ] I know the compile and runtime controls for preview and incubator code.
- [ ] I understand virtual-thread strengths, limits, pinning, and resource bounds.
- [ ] I understand sequenced collection views and Java 21 pattern syntax.
- [ ] I can explain strong encapsulation and migration debt from add-opens.
- [ ] I can plan a measured Java 17 to 21 rollout before adopting new syntax.
