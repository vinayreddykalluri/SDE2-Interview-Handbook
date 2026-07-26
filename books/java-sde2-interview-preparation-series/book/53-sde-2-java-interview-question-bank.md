# Chapter 53: SDE-2 Java Interview Question Bank

## Learning objectives

- Answer Java questions with definition, mechanism, guarantee, trade-off, and production relevance.
- Recognize follow-up probes that distinguish memorization from working knowledge.
- Detect misconception traps involving specifications, implementation details, complexity, and concurrency.
- Practice across JVM internals, language engineering, collections, concurrency, performance, DSA, backend design, testing, and security.
- Convert weak answers into short, precise SDE-2 model answers.

## Why this matters at SDE-2

An SDE-2 interview rarely stops at a definition. "HashMap is O(1)" invites collision, resizing, equality, and mutability probes. "Volatile gives visibility" invites a lost-update example. "The object is on the heap" invites escape analysis and the distinction between Java semantics and HotSpot optimization.

The interviewer is evaluating reasoning under uncertainty. A strong candidate states the contract first, labels implementation-specific details, tests assumptions, and connects the answer to a production decision. A weak candidate recites internal trivia without knowing whether it is guaranteed.

This bank is organized for active recall. Cover the model answer, speak for 60 to 120 seconds, then answer the follow-up before reading. Mark a question green only if the explanation is correct, bounded, and usable without prompting.

## First-principles model

Use a five-part answer shape:

```text
1. Definition: What is it?
2. Contract: What does Java guarantee?
3. Mechanism: How is it commonly implemented?
4. Trade-off: When does it help or hurt?
5. Evidence/example: How would I prove, test, or diagnose it?
```

Do not force all five parts into the opening sentence. Lead with the direct answer, then expand according to the probe. For comparison questions, establish dimensions before choosing: correctness, complexity, contention, ordering, memory, startup, tail latency, maintainability, and operational evidence.

For coding questions, use a parallel loop:

```text
clarify contract -> examples -> baseline -> invariant -> algorithm
-> complexity -> implement -> test edges -> discuss alternatives
```

> **Specification boundary:** Interview answers should distinguish JLS/JVMS and Java API guarantees from HotSpot, OpenJDK library, operating-system, and hardware behavior. An implementation detail is useful only when labeled with version and purpose.

## Core terminology

- **Opening answer:** Direct 20-to-40-second response before deeper probes.
- **Follow-up probe:** Question testing mechanism, edge cases, alternatives, or production application.
- **Misconception trap:** Plausible but false simplification, such as "Java passes objects by reference."
- **Invariant:** Property that must remain true throughout an algorithm or state transition.
- **Linearization point:** Instant at which a concurrent operation appears to take effect.
- **Compatibility:** Source, binary, class-file, runtime, or behavioral compatibility, depending on context.
- **Complexity qualification:** Expected, amortized, worst-case, or implementation-sensitive cost.
- **Readiness evidence:** Timed performance, explanation quality, test coverage, and corrected-error history rather than confidence alone.

## Detailed mechanics

The following bank includes an opening model answer, one likely follow-up, and a trap. The answers are intentionally concise; a mock interviewer should ask for examples and counterexamples.

### JVM and execution

**1. What is the difference among JDK, JRE, and JVM?**

Model answer: The JVM loads and executes class files. A runtime combines a JVM with standard modules, native support, and resources. A JDK contains a runtime plus tools such as `javac`, `jar`, `javadoc`, and diagnostics. Modern deployments may use a custom `jlink` runtime instead of a separately packaged historical JRE.

Follow-up: Why can code compiled on JDK 21 fail on Java 17? Trap: Saying a JRE always exists as a separate vendor download.

**2. Trace Java source to CPU execution.**

Model answer: `javac` lexes, parses, resolves names, type-checks, and emits class files. At runtime, loaders define classes; the JVM verifies, prepares, resolves, and initializes them. An execution engine interprets, JIT-compiles, ahead-of-time executes, or combines strategies before native instructions run.

Follow-up: Where can `NoSuchMethodError` occur? Trap: Calling Java only interpreted.

**3. What is bytecode?**

Model answer: Bytecode is the JVM's platform-neutral instruction representation stored in class-file method structures. It uses an operand-stack model and symbolic constant-pool references. It is not CPU machine code, and one source construct need not map to one bytecode sequence.

Follow-up: Use `javap -c` to explain `invokevirtual`. Trap: Calling the class-file constant pool the same as the string pool.

**4. What happens during class loading?**

Model answer: Loading finds bytes and creates a loader-scoped definition. Linking verifies safety, prepares static storage with defaults, and resolves symbolic references, possibly lazily. Initialization later executes static initializers on first required active use under a synchronized once-only protocol.

Follow-up: Does `SomeType.class` initialize the class? Trap: Saying explicit static values are assigned during preparation.

**5. Why can two classes with the same name fail a cast?**

Model answer: Runtime class identity includes the binary name and defining class loader. Two loaders that independently define `com.example.Message` produce different types. Plugin systems normally place shared contracts in a parent loader and implementations in isolated children.

Follow-up: Describe a class-loader leak. Trap: Assuming byte-for-byte identical definitions are assignment compatible.

**6. Explain JVM runtime data areas.**

Model answer: The JVM model has a shared heap for objects, shared per-class/method structures and runtime constant pools, and per-thread PCs, JVM stacks, frames, and native execution support. HotSpot structures such as Metaspace, TLABs, and code cache implement or extend this model but are not JVMS region names.

Follow-up: Why can process memory exceed `-Xmx`? Trap: Putting every static-referenced object in Metaspace.

**7. What does a JIT compiler optimize?**

Model answer: An adaptive JIT uses profiles to compile hot paths, inline calls, specialize receiver types, eliminate checks, analyze escape, and optimize loops. Guarded assumptions remain correct through fallback and deoptimization. Compilation consumes CPU and code-cache memory.

Follow-up: What is on-stack replacement? Trap: Assuming a source method call always has a physical frame.

**8. What is a safepoint?**

Model answer: It is an implementation safe state used for VM operations needing coordinated managed state. Relevant threads reach polls or transitions, then the operation runs. Time to safepoint and operation duration are separate; stop-the-world does not automatically mean full GC.

Follow-up: Name a non-GC safepoint operation. Trap: Calling safepoints a JLS feature.

### Memory and garbage collection

**9. What happens when `new` executes?**

Model answer: The class must be ready, storage and identity are obtained, fields receive mandatory defaults, and constructor invocation runs superclass construction, instance initializers, and the body. TLAB allocation, header words, field offsets, and compressed references are HotSpot details.

Follow-up: Can allocation be eliminated? Trap: Claiming Java guarantees stack allocation.

**10. Shallow size versus retained size?**

Model answer: Shallow size is the storage directly occupied by one object. Retained size is the storage that becomes unreachable if that object is removed, determined by GC-root paths and dominance. Referenced objects do not automatically belong to a parent's retained set.

Follow-up: Why can a small cache entry retain megabytes? Trap: Summing declared field widths as retained size.

**11. How does GC identify garbage?**

Model answer: A tracing collector starts from VM-known roots and follows strong and other applicable reachability edges. Objects with no relevant root path become reclamation candidates. Unreachable cycles are collectible, unlike naive reference counting.

Follow-up: Name common GC roots. Trap: Saying an object is garbage when a local is assigned null regardless of other paths.

**12. Compare strong, soft, weak, and phantom references.**

Model answer: Strong references retain normally. Soft references are discretionary memory-sensitive references and poor deterministic cache bounds. Weak references do not retain weakly reachable referents. Phantom references always return null and support post-mortem cleanup coordination through a `ReferenceQueue`.

Follow-up: Why drain a reference queue? Trap: Relying on finalization or phantom references to close resources promptly.

**13. Can Java have memory leaks?**

Model answer: Yes. GC removes unreachable objects, not reachable objects the application no longer needs. Unbounded caches, listeners, queues, thread locals, and class loaders can retain useless graphs. Compare allocation rate with post-collection live-set and dominator evidence.

Follow-up: Heap leak versus native leak? Trap: Calling every high allocation rate a leak.

**14. Compare G1, ZGC, and Parallel GC.**

Model answer: Parallel GC prioritizes throughput with parallel stop-the-world work. G1 uses regions, evacuation, concurrent marking, and pause goals for a balance. ZGC performs most work concurrently for very low pauses. Exact capabilities, including generations, depend on JDK version.

Follow-up: Which would you choose for a 99.9 latency SLO? Trap: Selecting from heap size alone without workload evidence.

### Java language and API design

**15. Is Java pass-by-reference?**

Model answer: No. Every argument value is copied into a parameter. For objects, that copied value is a reference, so caller and callee can mutate the same object. Reassigning the parameter cannot reassign the caller's variable.

Follow-up: Demonstrate with an array. Trap: Saying "objects are pass-by-reference" as shorthand.

**16. Overloading versus overriding?**

Model answer: Overloading selects among methods at compile time using declared types and conversions. Overriding supplies runtime instance dispatch based on the receiver's actual class. Static methods are hidden, not overridden; constructors are neither inherited nor overridden.

Follow-up: Which overload receives `null`? Trap: Treating return type alone as an overload distinction.

**17. Interface versus abstract class?**

Model answer: An interface defines a multiple-inheritance contract and can have default/static/private behavior but no per-instance constructor state. An abstract class shares state, protected implementation, and construction under single class inheritance. Choose by semantic relationship and evolution needs, not method count.

Follow-up: How do default-method conflicts resolve? Trap: Saying interfaces contain no implementation.

**18. Composition versus inheritance?**

Model answer: Inheritance creates a substitutability commitment and exposes superclass evolution. Composition delegates to a collaborator and keeps behavior replaceable. Prefer composition unless the subtype preserves the parent's behavioral contract and shared protected implementation is genuinely valuable.

Follow-up: Give a Liskov-substitution violation. Trap: Using inheritance only for code reuse.

**19. What makes a class immutable?**

Model answer: Its observable state cannot change after construction. Use private final fields, validate construction, prevent `this` escape, make defensive copies of mutable inputs/outputs, and ensure referenced components are immutable or encapsulated. A final reference alone is insufficient.

Follow-up: Are records deeply immutable? Trap: Equating `final` or record with deep immutability.

**20. Explain `equals` and `hashCode`.**

Model answer: Equal objects must produce the same hash code; unequal objects may collide. Equality should be reflexive, symmetric, transitive, consistent, and false for null. Keys must not change equality-relevant state while in hash collections.

Follow-up: Why can subclass equality break symmetry? Trap: Using database-generated IDs inconsistently across entity lifecycle.

**21. Checked versus unchecked exceptions?**

Model answer: Checked exceptions must be caught or declared and can express recoverable alternatives callers are expected to handle. Unchecked exceptions often represent violated contracts or failures not locally recoverable. The choice is API design, not severity; preserve causes and avoid broad catch-and-ignore.

Follow-up: When should an exception be translated? Trap: Logging and rethrowing at every layer.

**22. What does try-with-resources guarantee?**

Model answer: It closes initialized resources in reverse order on normal or abrupt completion. If body and close both throw, the body exception remains primary and close failures are suppressed. Resource ownership must still be clear.

Follow-up: How do you inspect suppressed exceptions? Trap: Assuming close exceptions replace the primary failure.

**23. Explain generic type erasure.**

Model answer: Java generics are mainly enforced at compile time; type variables are translated to bounds/Object with casts and bridge methods where needed. Parameterized types generally do not have distinct runtime classes, so `new T()` and `instanceof List<String>` are unavailable.

Follow-up: What is heap pollution? Trap: Saying all generic information disappears from metadata and reflection.

**24. Explain PECS.**

Model answer: For a parameterized structure that produces `T`, use `? extends T`; for one that consumes `T`, use `? super T`. It guides flexible APIs but does not mean a structure cannot both read and write in all designs.

Follow-up: Why can you add null to `List<? extends Number>` but not an Integer? Trap: Treating `List<Integer>` as a subtype of `List<Number>`.

**25. What Java 17 and 21 features matter most?**

Model answer: Java 17 provides a modern LTS baseline with records, sealed types, and finalized pattern matching for `instanceof`. Java 21 adds virtual threads, record patterns, switch pattern matching, and sequenced collections. Preview features must be labeled separately.

Follow-up: When not to use a record? Trap: Listing preview structured concurrency as final Java 21.

### Collections, streams, and I/O

**26. ArrayList versus LinkedList?**

Model answer: ArrayList gives O(1) indexed access, amortized O(1) append, compact storage, and cache locality. LinkedList gives O(1) insertion only when an existing node position is already represented by an iterator, but lookup is O(n) and each node costs memory. ArrayList is the usual default.

Follow-up: Removing from the front? Trap: Choosing LinkedList for arbitrary middle insertion while ignoring traversal.

**27. How does HashMap work?**

Model answer: It spreads a key hash to choose a table bin, then uses `equals` within collisions. Expected lookup is O(1) under good distribution; resize is O(n), and collision behavior can use trees in current implementations. Capacity and load factor trade memory against collision/resizing.

Follow-up: Why are mutable keys dangerous? Trap: Claiming `hashCode` must be unique.

**28. TreeMap versus HashMap?**

Model answer: TreeMap maintains key order and navigation with O(log n) operations. HashMap offers expected O(1) lookup without ordering. TreeMap equality for keys is effectively based on comparator/compare-to zero, so ordering consistency with `equals` matters.

Follow-up: How do range queries work? Trap: Using subtraction in a comparator and risking overflow.

**29. What does fail-fast mean?**

Model answer: Many ordinary collection iterators detect some unsupported structural modifications and may throw `ConcurrentModificationException` on a best-effort basis. It is a bug detector, not a synchronization guarantee. Concurrent collections define weak or snapshot iteration instead.

Follow-up: Is iteration safe if no exception occurred? Trap: Catching the exception and retrying as concurrency control.

**30. Comparable versus Comparator?**

Model answer: Comparable defines a type's natural order. Comparator is an external strategy, allowing multiple orderings and composition. Both must satisfy ordering contracts; sorted sets/maps treat comparison zero as equivalent for membership/key uniqueness.

Follow-up: Handle nulls? Trap: Comparator inconsistent/transitivity violations.

**31. Stream versus collection?**

Model answer: A collection stores elements; a stream is a single-use computation pipeline over a source. Intermediate operations are lazy, terminal operations trigger traversal, and side-effect-free stateless operations compose safely. Parallel streams need splittable work, sufficient size, associative reduction, and execution-context care.

Follow-up: `map` versus `flatMap`? Trap: Reusing a consumed stream.

**32. Why must reduction be associative?**

Model answer: Parallel partitioning can group operands differently. An associative accumulator/combiner produces the same logical result under regrouping. Floating-point addition is not mathematically associative due to rounding, so exact reproducibility may require sequential or specialized algorithms.

Follow-up: Identity requirements? Trap: Mutating one non-thread-safe container in `forEach` on a parallel stream.

**33. Optional best practices?**

Model answer: Optional is primarily a return type for possibly absent values. Use `map`, `flatMap`, `orElseGet`, and explicit absence semantics. Avoid Optional fields/parameters in ordinary domain models unless a framework/contract justifies them; never use Optional to hide errors.

Follow-up: `orElse` versus `orElseGet`? Trap: Calling `get()` without proving presence.

**34. Buffered I/O versus NIO?**

Model answer: Buffering amortizes system calls for stream/channel I/O. NIO.2 adds paths, file operations, channels, buffers, selectors, and asynchronous facilities. A `ByteBuffer` has capacity, position, and limit; `flip` changes from writing into the buffer to reading from it.

Follow-up: Direct buffer trade-offs? Trap: Assuming one `read` or `write` transfers the entire requested amount.

### Concurrency

**35. What is happens-before?**

Model answer: It is the JMM relation ordering memory effects. It comes from program order, volatile write/read, monitor unlock/lock, thread start/join, class initialization, and concurrent-library contracts plus transitivity. Wall-clock ordering or sleep is not enough.

Follow-up: Prove safe publication with volatile. Trap: Explaining only CPU caches.

**36. What does synchronized guarantee?**

Model answer: Mutual exclusion for the same monitor and memory ordering: unlock happens-before a later lock. It is reentrant and releases on block exit, including exceptions. All conflicting accesses must follow the same guard or another valid protocol.

Follow-up: `wait` versus `sleep`? Trap: Synchronizing readers and writers on different objects.

**37. What does volatile guarantee?**

Model answer: Volatile access is atomic and globally ordered for that variable; a write happens-before subsequent reads, publishing prior actions. It does not make read-modify-write or multi-field invariants atomic. It fits flags and immutable snapshot references.

Follow-up: Why is `volatile count++` unsafe? Trap: Calling volatile a lightweight universal lock.

**38. How does interruption work?**

Model answer: Interruption is cooperative cancellation through status. Interruptible waits often throw `InterruptedException` and clear status. Propagate it, or restore status and exit at a boundary. It cannot safely force arbitrary code or every I/O call to stop.

Follow-up: Why restore status? Trap: Catch, log, and continue.

**39. ReentrantLock versus synchronized?**

Model answer: Both provide exclusion and visibility. ReentrantLock adds timed/interruptible acquisition, multiple conditions, and optional fairness but requires `finally` unlock. Use synchronized for simple scoped locking; choose explicit features from a requirement, not presumed speed.

Follow-up: Why await in a loop? Trap: Unlocking outside finally.

**40. Explain CAS and ABA.**

Model answer: CAS installs an update only if current state matches expected; a successful CAS is a linearization point. Retry loops must recompute without side effects. ABA occurs when state changes A-B-A and equality hides history; use a version/stamp or different design.

Follow-up: Is lock-free starvation-free? Trap: Assuming GC eliminates logical ABA.

**41. How do you size an executor?**

Model answer: Start near cores for CPU-bound work. For blocking work, estimate blocking fraction but cap by downstream capacity, memory, and latency. Use bounded queues, explicit rejection/backpressure, deadlines, metrics, and load tests. An unbounded queue can make maximum threads irrelevant.

Follow-up: Explain same-pool starvation deadlock. Trap: Maximizing threads without considering database connections.

**42. `thenApply` versus `thenCompose`?**

Model answer: `thenApply` maps `T` to `U`; `thenCompose` maps `T` to a completion stage and flattens it. Non-Async callbacks may run on the completing/attaching thread. Explicit executors isolate blocking and capacity. Recovery stages can accidentally hide failures.

Follow-up: `handle` versus `whenComplete`? Trap: Assuming CompletableFuture cancellation interrupts work.

**43. Why virtual threads?**

Model answer: Java 21 virtual threads make thread-per-task blocking code scalable by unmounting blocked tasks from carrier platform threads. They do not add CPU cores or downstream capacity. Do not pool them; limit scarce resources directly. In Java 21, blocking while pinned in synchronized/native code can occupy carriers.

Follow-up: Migration incident with JDBC? Trap: Removing all concurrency limits.

**44. ConcurrentHashMap guarantees?**

Model answer: It supports thread-safe concurrent operations and atomic methods such as `putIfAbsent`, `compute`, and `merge`. Iteration is weakly consistent, not a whole-map snapshot. Multi-key invariants still require another protocol. Null keys/values are not allowed.

Follow-up: Why is `containsKey` then `put` racy? Trap: Using `size` for a precise admission decision during mutation.

### Performance, DSA, design, backend, testing, and security

**45. How do you benchmark Java code?**

Model answer: Define the decision and metric, use representative data, separate warm-up and measurement, consume results, fork JVMs, control environment, and report distributions. Use JMH for microbenchmarks because it addresses dead-code elimination, constant folding, and harness effects.

Follow-up: Why can `nanoTime` around one loop lie? Trap: Inferring production impact from a nanobenchmark.

**46. CPU is high. What do you inspect?**

Model answer: Confirm scope and timeline, then separate application, GC, JIT, native, and system CPU. Capture JFR or repeated profiles, correlate hot stacks with traffic and changes, and inspect runnable threads. Thread dumps alone sample stacks but not proportional CPU reliably.

Follow-up: What if CPU is low but latency high? Trap: Tuning GC without evidence.

**47. What is the SDE-2 DSA method?**

Model answer: Clarify input/output and constraints, run examples, state brute force, identify the invariant/data structure, derive complexity, implement incrementally, and test boundaries. Explain why the algorithm works, not only its pattern name.

Follow-up: How do you recover when stuck? Trap: Coding before clarifying duplicates, overflow, or mutation rules.

**48. BFS versus DFS?**

Model answer: BFS explores by distance layers and gives shortest path in unweighted graphs, using O(width) queue space. DFS explores depth, supports cycle/topological/component reasoning, and uses O(depth) stack/explicit storage. Both are O(V+E) with adjacency lists.

Follow-up: Why mark visited on enqueue? Trap: Recursive DFS on adversarial depth without stack analysis.

**49. What makes a good backend API?**

Model answer: Clear contract, validation, stable error model, idempotency where retries occur, pagination/bounds, authorization, observability, and compatibility. Keep transport DTOs separate from mutable persistence concerns and define timeout/cancellation behavior across dependencies.

Follow-up: Design idempotent payment creation. Trap: Treating HTTP retry as harmless without a key/state machine.

**50. Explain transaction isolation and Java locks.**

Model answer: A Java lock coordinates threads sharing one lock instance. A database transaction coordinates persisted operations across clients under an isolation level. In-process synchronization does not prevent another service instance from changing the row; use database constraints, locking/version checks, and idempotency.

Follow-up: Optimistic locking failure handling? Trap: Holding a Java lock across a remote transaction and calling it distributed consistency.

**51. How would you test concurrent code?**

Model answer: Prove invariants and happens-before/linearization points, then coordinate actor starts with barriers/latches, use bounded timeouts, record histories, and stress across forks/architectures. jcstress-style outcome tests can expose JMM behaviors. Passing tests do not prove absence of races.

Follow-up: How detect deadlock? Trap: Adding sleep to force order.

**52. Unit versus integration versus contract tests?**

Model answer: Unit tests isolate deterministic logic and run fast. Integration tests validate boundaries such as database, serialization, and framework wiring. Contract tests verify provider/consumer assumptions across service evolution. Use the cheapest layer that can catch the failure, with a smaller number of realistic end-to-end tests.

Follow-up: Test transaction rollback? Trap: Mocking the boundary whose semantics are under test.

**53. What security checks belong in a Java service?**

Model answer: Authenticate identity, authorize the specific resource/action, validate bounded input, parameterize SQL, avoid unsafe deserialization, protect secrets, constrain outbound requests, patch dependencies/JDK, and log security events without sensitive data. Apply least privilege and fail closed.

Follow-up: Prevent SSRF in a URL-fetch endpoint. Trap: Treating validation as only regex syntax.

**54. How do you prevent injection?**

Model answer: Keep data separate from interpreter syntax: prepared SQL parameters, contextual output encoding, safe command APIs without shell concatenation, and allowlisted structure when identifiers cannot be parameterized. Validation helps policy but escaping alone is context-sensitive and fragile.

Follow-up: Can prepared statements parameterize table names? Trap: Building shell commands from quoted user strings.

## Worked Java example

An interviewer asks: "Implement a small bounded LRU cache and discuss concurrency." A focused baseline is:

```java
import java.util.LinkedHashMap;
import java.util.Map;

public final class LruCache<K, V> {
    private final int capacity;
    private final LinkedHashMap<K, V> entries;

    public LruCache(int capacity) {
        if (capacity <= 0) throw new IllegalArgumentException("capacity");
        this.capacity = capacity;
        this.entries = new LinkedHashMap<>(16, 0.75f, true) {
            @Override
            protected boolean removeEldestEntry(Map.Entry<K, V> eldest) {
                return size() > LruCache.this.capacity;
            }
        };
    }

    public synchronized V get(K key) {
        return entries.get(key);
    }

    public synchronized void put(K key, V value) {
        entries.put(key, value);
    }

    public synchronized int size() {
        return entries.size();
    }
}
```

`accessOrder=true` means a successful `get` mutates order, so even reads require synchronization. The intrinsic lock gives a simple linearization point per method. This is an in-process cache, not a distributed cache, and it deliberately omits loader coalescing, expiration, metrics, null policy, weight-based capacity, and external callback safety.

## Execution or memory walkthrough

For capacity two:

1. `put(A,1)` inserts A. Order is `[A]`.
2. `put(B,2)` inserts B. Order is `[A,B]`, least to most recently used.
3. `get(A)` returns 1 and moves A: `[B,A]`.
4. `put(C,3)` temporarily produces `[B,A,C]`.
5. `removeEldestEntry` observes size three and removes B, leaving `[A,C]`.

If T1 executes `get(A)` while T2 executes `put(C)`, the same monitor serializes the full LinkedHashMap operations. Without synchronization, a `get` can race because access-order maintenance changes linked structure. Returning mutable values still lets callers race on those values; cache thread safety does not make value objects immutable.

An excellent interview discussion adds that eviction callbacks must run after releasing the lock, loading should avoid duplicate work without holding the lock across I/O, and production caches need tested libraries unless the exercise explicitly requires implementation.

## Complexity and performance

Expected `get` and `put` are O(1); eviction is O(1) for one eldest entry. Space is O(capacity), excluding retained key/value graphs. The single lock caps parallel operations and can create contention, but it provides a short proof. Segmenting/sharding can increase concurrency at the cost of approximate global LRU.

For the question bank, track more than answer count. Score each response from 0 to 4:

- 0: no viable answer.
- 1: memorized fragment or materially incorrect.
- 2: correct opening but weak mechanism/edge cases.
- 3: correct, structured, one follow-up handled.
- 4: precise contract, trade-off, production example, and follow-ups.

Require at least 3 on correctness-critical areas: JMM, equality/hashing, exceptions/resources, complexity, transactions, and security. Fast wrong answers are worse than slower bounded reasoning.

## Edge cases and common mistakes

- Starting with five minutes of context instead of answering the question.
- Inventing a guarantee when uncertain; state the stable contract and label the uncertainty.
- Giving Big-O without expected/amortized/worst-case qualification.
- Describing HotSpot object headers or HashMap thresholds as language guarantees.
- Using a framework slogan instead of Java mechanics.
- Optimizing before defining the invariant and constraints.
- Ignoring null, empty, duplicate, overflow, cancellation, and adversarial inputs.
- Saying "thread-safe" without naming the operation/invariant.
- Presenting an incident with no measurement, decision, or outcome.
- Arguing that a data race is safe because it worked on x86.
- Hiding a weak area behind excessive terminology.

## Production engineering notes

Prepare six reusable incident stories: latency, correctness, memory/GC, concurrency, dependency failure, and security/reliability. Each should state scale, signal, hypothesis, evidence, action, trade-off, and measured result. Never disclose employer secrets; normalize identifiers and approximate scale where needed.

For JVM questions, mention the tool that would verify the claim: `javap` for class files, JFR/profile for CPU/allocation/locks, GC logs and heap analysis for memory, thread dumps for blocking, and build/runtime metadata for compatibility. Tool names without an investigation sequence are not enough.

> **HotSpot note:** Questions about TLABs, mark words, C1/C2, safepoint polls, collector defaults, and virtual-thread diagnostics are version-specific. A strong answer says "in mainstream HotSpot on the stated JDK" before explaining them.

## Interview questions and model answers

Use this twelve-question rapid loop after completing the bank:

1. **Explain source to CPU in 90 seconds.** Model: compiler, class file, loading/linking/init, adaptive execution, native code, specification boundary.
2. **Diagnose process OOM with stable heap.** Model: inspect container/RSS, stacks, Metaspace, direct buffers, code cache, native libraries, and NMT where available.
3. **Design an immutable key.** Model: stable equality/hash, final state, defensive copies, no mutable identity fields.
4. **Choose ArrayList or LinkedList.** Model: operation distribution and locality; ArrayList default.
5. **Prove volatile publication.** Model: program order, volatile synchronizes-with, transitive happens-before.
6. **Fix a shutdown hang.** Model: stop admission, interrupt/close blocking resource, propagate cancellation, join/await deadline, capture survivors.
7. **Bound an executor.** Model: downstream capacity, queue, rejection/backpressure, deadlines, metrics.
8. **Explain virtual-thread migration risk.** Model: cheap waiting increases concurrency; preserve DB/API bulkheads and diagnose Java 21 pinning.
9. **Solve longest unique substring.** Model: sliding window with last-seen indices, O(n) time and O(character-set) space.
10. **Design idempotent create.** Model: client key, atomic uniqueness, stored outcome/state machine, request-hash conflict policy.
11. **Investigate p99 regression.** Model: compare timeline/distributions, profile, queues/locks/GC/downstream, controlled experiment.
12. **Prevent unsafe deserialization.** Model: simple schema-bound formats, type allowlists, validation, dependency patches, least privilege, size/depth limits.

## Exercises

1. Record answers to all 54 questions; score them 0 to 4 and keep the first incorrect sentence for review.
2. For ten questions, produce a 30-second, 90-second, and five-minute answer.
3. Add two follow-ups and one misconception trap to every question scored below 3.
4. Implement and test the LRU cache, then redesign loader coalescing without holding a lock during I/O.
5. Run three paired mocks: JVM/concurrency, collections/DSA, and backend/performance/security.
6. Build a one-page error log containing the guarantee you confused, a counterexample, and the corrected rule.
7. Select five implementation-specific answers and attach an exact JDK/VM version label.

## Chapter summary

SDE-2 answers begin with a direct contract, then explain mechanism, trade-offs, and evidence. The bank spans the execution pipeline, memory, language, collections, concurrency, performance, DSA, backend design, testing, and security because interviews connect these domains. Active recall, follow-up probes, misconception traps, and scored corrections convert knowledge into interview performance.

## Revision checklist

- [ ] I can answer every bank question in under two minutes before follow-ups.
- [ ] I distinguish Java guarantees from HotSpot and library implementation details.
- [ ] I qualify complexity as expected, amortized, or worst-case.
- [ ] I can prove concurrency answers with happens-before or a linearization point.
- [ ] I connect JVM/performance answers to diagnostic evidence.
- [ ] I have production stories with measured outcomes and clear ownership.
- [ ] I handle coding constraints, edges, complexity, implementation, and tests aloud.
- [ ] No correctness-critical bank category scores below 3.

