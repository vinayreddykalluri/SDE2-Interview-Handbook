# Chapter 11: The Java Memory Model from First Principles

## Learning objectives

- Separate atomicity, visibility, and ordering in concurrent Java.
- Use happens-before to prove which writes a read is guaranteed to observe.
- Explain monitor, volatile, thread start/join, task synchronization, default initialization, and final-field rules.
- Understand data races, safe publication, immutable objects, and broken double-checked locking.
- Avoid reasoning only from source interleavings or a particular CPU cache story.

## Why this matters at SDE-2

Concurrency bugs often pass tests and fail under production load, another architecture, or a new JIT. Locks are not merely mutual exclusion. They, volatile operations, and lifecycle APIs establish ordering that makes writes visible. Without such relationships, "thread A ran first" is not a proof.

An SDE-2 should be able to review a publication pattern, explain why `volatile` does not make `count++` atomic, select a synchronization mechanism, and state the proof in happens-before terms. This skill underlies concurrent collections, executors, atomics, virtual threads, and reliable service shutdown.

## First-principles model

Each thread executes actions governed by its program order, but compilers and processors may transform implementation order when a single thread cannot tell the difference. Multiple threads can observe effects only within constraints imposed by the Java Memory Model (JMM).

The JMM is not a literal diagram of each core copying values to "main memory." It is a formal contract defining legal observations. The central proof relation is happens-before (HB): if action A happens-before action B, then A's effects are ordered before B, and conflicting memory observations must respect that order.

```text
writer thread                         reader thread
data = 42;                            if (ready) {
ready = true;  // volatile W ----HB----> volatile R of true
                                      use(data); // guaranteed to see 42
                                  }
```

The volatile write of `ready` synchronizes-with a subsequent volatile read that observes the relevant write in the volatile order. Program-order edges place `data=42` before that write and the `data` read after the volatile read. HB transitivity orders the data write before the data read.

## Core terminology

- **Memory action:** Read/write of a variable, volatile action, lock/unlock, thread lifecycle action, and other JMM-relevant operation.
- **Program order:** Per-thread order consistent with intra-thread semantics.
- **Synchronization action:** Operation participating in the global synchronization order, such as volatile access or monitor lock/unlock.
- **Synchronizes-with:** Specified cross-thread edge created by matching synchronization actions.
- **Happens-before:** Transitive ordering built from program order and synchronizes-with plus other specified rules.
- **Data race:** Two conflicting accesses to the same variable, at least one a write, not ordered by happens-before.
- **Sequential consistency (SC):** Behavior explainable by one total interleaving preserving each thread's program order.
- **Safe publication:** Making an object reachable by other threads through an ordering mechanism that exposes required initialized state.
- **Visibility:** Guarantee about which writes a read can observe.
- **Atomicity:** Indivisibility of an operation relative to other threads.
- **Ordering:** Constraints preventing observable reordering across actions.

## Detailed mechanics

Three properties must be separated:

- **Atomicity:** A read or write may be indivisible, yet a read-modify-write expression is multiple actions. `count++` reads, adds, and writes; two threads can lose an update.
- **Visibility:** One thread may not be guaranteed to observe another thread's ordinary write without HB, even if wall-clock time passed.
- **Ordering:** Operations can be implemented in a different order when allowed, and another thread with a data race can observe results not predicted by a naive source interleaving.

Reads and writes of references and most primitive values are atomic under JMM rules. The specification still permits a non-volatile `long` or `double` access to be implemented as two 32-bit accesses, so a racing read could theoretically tear; volatile `long` and `double` accesses are guaranteed atomic. Mainstream 64-bit HotSpot platforms commonly perform aligned 64-bit accesses atomically, but correctness must not rely on that implementation fact. Atomic individual access still does not make compound invariants atomic. Array elements and fields are distinct variables, and implementations must prevent a write to one from tearing into an adjacent variable.

Happens-before includes these crucial rules:

1. Every action in a thread happens-before each later action in that thread's program order.
2. An unlock of a monitor happens-before every subsequent lock of that same monitor.
3. A write to a volatile variable happens-before every subsequent read of that variable in the synchronization order.
4. A call to `Thread.start()` happens-before actions in the started thread.
5. All actions in a thread happen-before another thread successfully returns from `join()` on it.
6. Default initialization of an object happens-before any other actions, providing zero/default state safety.
7. HB is transitive.

Library specifications add edges. For example, executor submission, `Future.get`, concurrent collections, latches, and queues state memory-consistency effects. Use those documented contracts; do not assume that "it uses threads" provides publication.

Monitors provide both mutual exclusion and ordering. Exiting a `synchronized(lock)` block unlocks; later entering a block on the same monitor locks. Synchronizing on different objects creates no matching edge. Reentrancy lets a thread reacquire a monitor it owns but does not weaken cross-thread rules.

Volatile provides ordering/visibility for a variable without making arbitrary multi-step invariants mutually exclusive. A volatile read sees some write consistent with volatile synchronization order; if it sees a publication flag's write, earlier writer effects are visible after that read. A volatile write has release-like effects and a volatile read acquire-like effects as an intuition, while the formal Java rule remains synchronizes-with/HB.

Volatile is suitable for independent state such as a shutdown flag or immutable snapshot reference. It is insufficient for `if (stock > 0) stock--`, `counter++`, or updates across multiple fields that must be observed consistently. Use a lock, an atomic read-modify-write primitive, or an immutable state transition through CAS.

Data-race-free programs whose synchronization is correctly used receive a powerful guarantee often summarized as DRF-SC: executions appear sequentially consistent. This is why synchronization should define the program, not merely add barriers after a bug appears. A data race does not mean "the latest value eventually appears"; allowed observations can be unintuitive while still constrained by causality and safety rules.

Final fields have special initialization safety. If an object's constructor completes and the reference does not escape during construction, another thread that obtains the reference, even through some racy forms, receives specified guarantees for correctly constructed final fields and reachable state covered by the final-field semantics. This is not a license for racy publication: non-final fields, later mutations, compound object graphs, and the reference itself still demand robust publication. Use final fields plus safe publication.

Safe publication idioms include:

- constructing before storing into a `volatile` reference;
- storing while holding a monitor that readers also acquire;
- static initialization/class initialization;
- placing into a concurrent collection or synchronization-aware queue;
- passing state before `Thread.start()`;
- completing a `Future`, promise-like API, or latch under its documented memory effects.

Thread confinement avoids sharing entirely. Immutability reduces proof surface because state cannot change after construction. Neither helps if mutable components leak or an object's constructor publishes `this` prematurely.

Double-checked locking is correct only with a volatile publication field in its standard modern form:

```java
private static volatile Service instance;

static Service instance() {
    Service local = instance;
    if (local == null) {
        synchronized (Service.class) {
            local = instance;
            if (local == null) {
                local = new Service();
                instance = local;
            }
        }
    }
    return local;
}
```

Without volatile, publication can race with construction effects, and the unsynchronized read has no required HB connection. Simpler static-holder initialization is often preferable.

> **Specification boundary:** The JMM is part of Java language semantics. It defines allowed observations through actions and ordering relations, not cache-flush instructions, CPU fences, or HotSpot compiler phases. A correct proof should remain valid across processors and conforming JVMs.

## Worked Java example

```java
public final class Publication {
    private int payload;
    private volatile boolean ready;

    void publish(int value) {
        payload = value;
        ready = true;
    }

    int await() {
        while (!ready) {
            Thread.onSpinWait();
        }
        return payload;
    }

    public static void main(String[] args) throws InterruptedException {
        Publication publication = new Publication();
        Thread reader = new Thread(() ->
                System.out.println(publication.await()));
        reader.start();
        publication.publish(42);
        reader.join();
    }
}
```

If `await` terminates after reading `ready == true`, it must return 42. The volatile edge publishes the earlier ordinary write. `Thread.onSpinWait()` is only a hint and supplies no visibility guarantee; `volatile` supplies the ordering.

This is an educational pattern, not a recommended blocking design. Busy waiting consumes a carrier/CPU resource. A latch, queue, future, or higher-level synchronizer usually expresses waiting more efficiently.

## Execution or memory walkthrough

Name the actions:

```text
Main/writer                          Reader
S: reader.start()
P: payload = 42
W: volatile ready = true
                                     R: volatile read ready == true
                                     Q: read payload
                                     T: reader terminates
J: reader.join() returns
```

Proof:

1. `S` happens-before actions in the started reader, though this alone does not publish the later write P because P occurs after start.
2. Writer program order gives `P HB W`.
3. The volatile rule gives `W HB R` for the subsequent read of the published true write.
4. Reader program order gives `R HB Q`.
5. Transitivity gives `P HB Q`, so Q must observe payload=42 or a later HB-consistent write, and none exists here.
6. All reader actions happen-before `J`, so after `join`, the main thread can safely observe reader effects as defined.

If `ready` is ordinary, P and W are not connected to reader R/Q by synchronization. Source order and physical elapsed time are insufficient. The loop could fail to terminate, or the reader could observe an allowed stale/independently ordered state.

If two writers call `publish` concurrently, volatile does not make the pair `(payload, ready)` an atomic transaction. The design assumes a single publication event. For repeated messages, use a queue, lock, sequence protocol, or immutable snapshot held in one volatile reference.

## Complexity and performance

Each volatile read/write is O(1) algorithmically, but its constant cost includes compiler and hardware ordering constraints and cache-coherence traffic. An uncontended monitor can also be fast, while contention introduces queueing, park/unpark, scheduler delay, and convoy effects. Big-O alone is not useful for synchronization choices.

The spin loop performs O(k) reads until publication and consumes CPU while waiting. With no publication, time is unbounded. Blocking synchronization trades wake-up latency for released CPU. Hybrid spin-then-park strategies belong in well-tested concurrency libraries, not ad hoc business code.

False sharing is an implementation/hardware performance issue where independent frequently written variables share cache-coherence granularity. It does not change JMM correctness. Padding or special annotations are JVM-specific optimization techniques requiring measurement.

> **HotSpot note:** HotSpot maps JMM requirements to compiler barriers, machine instructions, cache-coherence protocols, monitor implementations, and runtime stubs appropriate to the target CPU. x86-64 and AArch64 can require different instruction sequences. Those mappings are not portable source-level explanations.

## Edge cases and common mistakes

- Saying volatile makes a variable "thread-safe" without defining the invariant.
- Using `volatile int count` and expecting `count++` to be atomic.
- Synchronizing writers but allowing readers to access the same fields without the same lock or another HB edge.
- Locking on different objects, mutable lock references, boxed values, or publicly accessible strings.
- Assuming `sleep`, logging, a debugger, or elapsed time flushes memory.
- Treating `ConcurrentHashMap` safety as automatically making mutable values safely updated under arbitrary operations.
- Starting a thread from a constructor and leaking partially constructed `this`.
- Assuming final reference means deeply immutable graph.
- Using double-checked locking without volatile.
- Believing data races merely return "old or new" values in every compound scenario.
- Reasoning from one observed run as proof.

Publication and mutation are separate. Putting a mutable `ArrayList` into a concurrent map can safely publish the list reference according to the map contract, but unsynchronized mutations of that list can still race. Similarly, `Collections.synchronizedList` requires its documented locking discipline during compound iteration.

Deadlock freedom and memory visibility are separate properties. A correct HB proof can still deadlock; a lock-free algorithm can still be logically wrong. Progress properties such as obstruction freedom, lock freedom, wait freedom, fairness, and starvation need separate analysis.

## Production engineering notes

Prefer ownership and higher-level primitives:

- immutable request/config snapshots published atomically;
- executor task boundaries and futures;
- blocking queues for producer-consumer transfer;
- concurrent maps with atomic methods such as `compute` for key-scoped transitions;
- atomics for small independent state machines;
- locks for multi-field invariants;
- structured lifecycle with interruption and join/close semantics.

Document invariants next to guarded fields, for example "guarded by `lock`" or "immutable snapshot published through volatile `current`." Code review should identify every conflicting access and the HB path ordering it, not only verify that some `synchronized` appears.

Race testing is probabilistic. Stress tools, randomized scheduling, high iteration counts, and multiple architectures can expose bugs but cannot prove absence. Static analyzers and disciplined design help. Thread dumps reveal blocking/deadlock states but not all memory-order races. JFR and profiles can reveal lock contention; they do not replace a correctness proof.

When optimizing synchronization, first preserve a written invariant and establish a benchmark representing contention. Replacing a lock with multiple atomics can weaken snapshot consistency. A single volatile immutable state object is often both clearer and faster than coordinating several volatile fields.

## Interview questions and model answers

**What does happens-before mean?**

It is the JMM ordering relation used to guarantee visibility and constrain observations. It is built from per-thread program order, synchronizes-with edges such as monitor unlock-to-lock and volatile write-to-read, thread lifecycle rules, and transitivity. It is not simply wall-clock ordering.

**What does volatile guarantee?**

Individual volatile access is atomic and participates in a total synchronization order. A volatile write happens-before subsequent reads of that variable, publishing earlier writer actions to code after the read. Volatile does not make compound operations or multi-field invariants atomic.

**How does `synchronized` affect memory?**

It provides mutual exclusion for the same monitor. Unlocking that monitor happens-before a subsequent lock, so writes before unlock are visible to actions after the matching lock. Different monitors do not create that edge.

**Why is `count++` unsafe even if reads/writes of `int` are atomic?**

Increment is read, compute, write. Two threads can read the same old value and both write the same incremented value. Use `AtomicInteger.incrementAndGet`, a lock, or an appropriate aggregate concurrency design.

**How do you safely publish an object?**

Construct it without leaking `this`, preferably make state final/immutable, then publish through a specified edge: volatile write/read, matching monitor unlock/lock, class initialization, thread start, concurrent collection, queue, future, or another synchronizer's documented memory effect.

**Does final make an object thread-safe?**

No. Final prevents reassignment of that field after construction and supplies special initialization safety for correctly constructed objects. The referenced object may still be mutable, and later mutations require synchronization.

**Why can a racy loop fail to see a flag update?**

Without synchronization, there is no HB edge requiring the read to observe the other thread's write. Compiler optimizations and hardware execution may reuse or reorder values within JMM permissions. Volatile or a synchronizer establishes the required relation.

## Exercises

1. Draw the HB graph for the worked example and remove each edge in turn.
2. Implement a lost-update counter using plain `int`, then correct it with a lock and an atomic.
3. Review a mutable object placed in `ConcurrentHashMap`; identify which operations remain unsafe.
4. Implement lazy initialization using the static-holder idiom and compare its proof with volatile double-checked locking.
5. Design an immutable configuration snapshot with one volatile reference.
6. Explain the memory effects of submitting a task and obtaining its `Future` result from library documentation.
7. Find a `this`-escape pattern in a constructor and redesign it with a factory.
8. State correctness, liveness, and performance properties separately for a proposed concurrent queue use.

## Chapter summary

The JMM defines legal cross-thread observations through actions and ordering relations, not a literal cache architecture. Atomicity, visibility, and ordering are distinct. Happens-before proofs connect program order with volatile, monitors, thread lifecycle, class initialization, and synchronization-library contracts. Data-race-free designs gain sequentially consistent reasoning. Safe construction, safe publication, immutability, ownership, and high-level synchronizers are the practical path to reliable concurrency.

## Revision checklist

- [ ] I can distinguish atomicity, visibility, and ordering.
- [ ] I can define data race, synchronizes-with, and happens-before.
- [ ] I can list monitor, volatile, start, join, and initialization edges.
- [ ] I can prove the volatile publication example by transitivity.
- [ ] I know why `volatile count++` is unsafe.
- [ ] I can safely publish immutable and mutable state.
- [ ] I understand final-field guarantees and `this` escape limitations.
- [ ] I can explain double-checked locking and prefer simpler alternatives.
- [ ] I do not substitute CPU-cache folklore or tests for a JMM proof.
