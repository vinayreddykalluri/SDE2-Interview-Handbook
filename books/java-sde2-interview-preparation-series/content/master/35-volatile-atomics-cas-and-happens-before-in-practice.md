# Chapter 35: Volatile, Atomics, CAS, and Happens-Before in Practice

## Learning objectives

- Prove publication and visibility using happens-before rather than timing intuition.
- Identify when volatile is sufficient and when an invariant needs an atomic transition or lock.
- Explain compare-and-set, retry loops, linearization points, and progress properties.
- Recognize ABA and choose versioning, stamped references, or a different design.
- Use atomic counters, immutable state snapshots, and contention-aware accumulators correctly.

## Why this matters at SDE-2

Lock-free code can be wrong while looking sophisticated. Two atomic fields do not make a two-field invariant atomic. A volatile flag can publish prior writes but cannot prevent duplicate check-then-act. A CAS loop can burn a core under contention. A `LongAdder` can improve metrics throughput but is unsuitable for an exact bank balance or sequence number.

SDE-2 interviews expect both correctness and judgment. You should locate the linearization point, name the happens-before edge, state whether the operation is lock-free or wait-free, and explain why a simpler lock might be better. Production design additionally needs overflow, observability, retry, and shutdown behavior.

## First-principles model

Shared-state correctness has two layers:

1. **Ordering/visibility:** Which writes must a reader observe? Use happens-before edges.
2. **Atomic state transition:** Which read-decision-write sequence must appear indivisible? Use a lock, CAS, or a higher-level concurrent operation.

Volatile solves the first layer for a variable and surrounding actions:

```text
writer                               reader
snapshot = fullyBuilt;
volatile current = snapshot; --HB--> read volatile current
                                     use fully initialized snapshot
```

CAS can solve a single-location transition:

```text
loop:
  observed = state
  proposed = f(observed)
  if compareAndSet(observed, proposed): success
  else: another transition won; retry from new state
```

The successful CAS is the linearization point: the instant the operation appears to take effect.

## Core terminology

- **Volatile variable:** Field whose accesses participate in the JMM volatile synchronization order and visibility rules.
- **Atomic variable:** Library wrapper such as `AtomicInteger` or `AtomicReference` supporting indivisible operations on one location.
- **CAS:** Compare-and-set; update only if the current value equals an expected value under the operation's comparison semantics.
- **Linearization point:** Single conceptual instant at which a concurrent operation takes effect.
- **Lock-free:** System-wide progress is guaranteed under continued scheduling, though one thread may starve.
- **Wait-free:** Each operation completes in a bounded number of its own steps.
- **Obstruction-free:** A thread completes if it eventually runs alone long enough.
- **ABA problem:** A location changes A to B and back to A, making equality alone hide intervening history.
- **Contention:** Concurrent attempts compete to update the same state.
- **False sharing:** Independent hot values trigger cache-coherence traffic because of physical proximity.

## Detailed mechanics

A volatile write happens-before every subsequent read of that volatile in synchronization order. Program order and transitivity publish ordinary writes before the volatile write to code after the volatile read. Volatile access is individually atomic, including volatile `long` and `double`.

Volatile does not combine operations. `volatile int count; count++;` remains a read, calculation, and write. Two threads can read 5 and both write 6. Similarly, this is unsafe:

```java
if (!initialized) {
    initialized = true;
    initialize();
}
```

Even if `initialized` is volatile, two threads can both read false before either writes true, and setting it before initialization can expose the wrong protocol. Use class initialization, a lock, a future, or correctly designed CAS state.

Volatile is a good match for an independent cancellation flag, one-writer status, or reference to an immutable snapshot. Updating an immutable `Config` and publishing its single reference makes all fields move together. Publishing separate volatile host and port fields permits mixed-version snapshots.

Atomic classes offer operations such as `getAndIncrement`, `compareAndSet`, `getAndUpdate`, and `accumulateAndGet`. These are more than volatile wrappers because read-modify-write is indivisible for that atomic location. Atomic object methods have documented memory effects broadly aligned with their access modes. Newer APIs such as `VarHandle` expose a range from plain/opaque/acquire/release to volatile access; use weaker modes only with a written proof and measured need.

A CAS loop must recompute from the newly observed value after failure. The update function can execute multiple times, so it must be side-effect free. Do not charge a card, append to an external log, or increment a separate metric inside a function passed to `updateAndGet`; retries can duplicate side effects.

CAS is optimistic. Under low contention it avoids parking and often performs well. Under high contention many threads calculate losing updates, retry, and generate cache-line traffic. Lock-free means some operation makes progress; it does not mean every caller has bounded latency or that no scheduling support is involved.

ABA matters when equality is used as evidence that no relevant change occurred. In a lock-free stack, thread T1 reads head A and next C, pauses, while T2 pops A, changes the stack, and later pushes the same A object back. T1's CAS from A to C can succeed even though history changed and C may no longer represent the intended successor. Solutions include immutable/non-reused nodes under GC-specific reasoning, version counters, `AtomicStampedReference`, `AtomicMarkableReference`, hazard/epoch schemes in native memory, or a lock.

Java garbage collection prevents a freed Java object address from being manually reallocated behind a live reference in ordinary code, removing one classic native ABA route. It does not eliminate logical ABA when the same reference/value is deliberately restored.

`AtomicStampedReference` atomically compares reference plus integer stamp. Stamp overflow is theoretically possible, so the stamp width and operation lifetime matter. A monotonic version embedded in an immutable state object often makes domain history clearer.

`LongAdder` distributes updates across internal cells under contention, then sums them. It is excellent for high-rate statistics where a momentarily non-atomic aggregate is acceptable. `sum()` is not an atomic snapshot relative to concurrent updates, and `sumThenReset()` can be inappropriate when exact accounting is required. Use `AtomicLong` or locking for exact sequences/balances.

> **Specification boundary:** The JMM and `java.util.concurrent.atomic` APIs define visibility and atomic-operation contracts. They do not require one CPU instruction, a particular cache protocol, wait-free progress for all atomic methods, or a fixed physical layout.

## Worked Java example

```java
import java.util.concurrent.atomic.AtomicReference;

public final class Inventory {
    private record State(int available, long version) {
        State {
            if (available < 0) throw new IllegalArgumentException("negative");
        }
    }

    private final AtomicReference<State> state;

    public Inventory(int initial) {
        state = new AtomicReference<>(new State(initial, 0));
    }

    public boolean reserve(int units) {
        if (units <= 0) throw new IllegalArgumentException("units");
        while (true) {
            State observed = state.get();
            if (observed.available() < units) return false;
            State proposed = new State(
                    observed.available() - units,
                    observed.version() + 1);
            if (state.compareAndSet(observed, proposed)) return true;
            Thread.onSpinWait();
        }
    }

    public int available() {
        return state.get().available();
    }
}
```

Availability and version move in one immutable object referenced by one atomic location. No observer can see new availability with an old version. The successful CAS linearizes each reservation.

This in-memory reservation is not a substitute for a database transaction across processes. Its scope is one JVM object instance.

## Execution or memory walkthrough

Start with `State(5, 0)`. Threads A and B both call `reserve(4)`:

1. A reads reference S0 -> `(5,0)`.
2. B reads the same reference S0.
3. A allocates proposed S1 -> `(1,1)`.
4. B allocates proposed S2 -> `(1,1)`; equal content does not make it the same object reference.
5. A performs CAS expected S0, update S1. It succeeds and atomically changes the location.
6. B performs CAS expected S0, update S2. Current reference is S1, so it fails.
7. B retries and reads S1. Available 1 is less than 4, so B returns false.

Exactly one reservation succeeds; availability never becomes negative. A lock-free system-progress claim applies because a failed CAS indicates another update won. B itself was not guaranteed to win.

The immutable objects are ordinary heap objects. Failed proposed states become unreachable and add allocation/GC pressure. A packed `AtomicLong` could encode fields without allocation, but packing introduces bit widths, overflow, readability, and migration constraints. Optimize only after evidence.

The atomic read that observes S1 also gives the documented visibility needed to read that fully constructed record. Final record fields further support immutable construction, but publication still uses the atomic reference.

## Complexity and performance

In the absence of contention, `reserve` is O(1) time and allocates one small state. Under contention, one call can retry an unbounded number of times, so it is not wait-free. Total useful updates still progress if threads continue running and the atomic implementation provides the expected progress behavior.

CAS loops work best for small, fast, independent transformations. If validation is expensive, contention high, or the invariant spans several mutable structures, a lock can reduce wasted work and produce better tail latency. Backoff may reduce contention but complicates fairness and tuning.

Atomics create hot memory locations. An `AtomicLong` incremented by every request can become a coherence bottleneck. `LongAdder` trades exact instantaneous state and memory for scalable distributed updates. Sharding domain state can also improve locality, but requires aggregation semantics.

`Thread.onSpinWait()` is only a performance hint and does not add ordering or guarantee progress. A long spin should yield to a blocking design or explicit backoff to avoid wasting CPU.

> **HotSpot note:** HotSpot intrinsifies many atomic operations into CPU-specific instructions and barriers when possible. CAS instruction costs, spurious failure behavior of weak variants, padding, and fence mappings differ across x86-64, AArch64, and JVM versions.

## Edge cases and common mistakes

- Believing volatile makes `++`, check-then-act, or multi-field invariants atomic.
- Updating two atomics separately and calling the pair transactionally consistent.
- Performing side effects inside a retryable atomic update function.
- Ignoring counter overflow or stamp wraparound.
- Treating `LongAdder.sum()` as an exact linearizable balance.
- Assuming lock-free means starvation-free, bounded latency, or always faster.
- Retrying CAS without rereading and recomputing proposed state.
- Using reference CAS when logical equality, not identity, was intended, or vice versa.
- Dismissing ABA merely because Java has GC.
- Publishing a mutable object through volatile and then mutating it without synchronization.
- Using several volatile fields when one immutable snapshot is the actual invariant.

A volatile collection reference does not make collection operations thread-safe. Volatile orders replacement of the reference; concurrent mutation still requires a concurrent collection or other synchronization.

## Production engineering notes

Write down each operation's linearization point and scope. `Inventory.reserve` is atomic only inside one process and instance; a replicated service requires a database conditional update, consensus-backed store, partition ownership, or idempotent distributed workflow.

Monitor CAS failure/retry rates, CPU, allocation, and p99 latency. A regression after traffic growth may be coherence contention rather than application computation. Profiles can show atomic intrinsics and spin hot spots; JFR/OS tools can correlate CPU saturation.

Incident example: a metrics endpoint occasionally reports fewer completed requests than the previous scrape. The code uses `LongAdder.sumThenReset()` while updates continue, assuming an exact interval transfer. Some updates race with cell summation/reset. Fix by accepting approximate monotonic cumulative counters, using a metrics library's supported model, or serializing exact interval accounting if the business truly requires it.

For configuration, construct an immutable snapshot, validate it completely, then publish one volatile/atomic reference. Keep the previous snapshot on validation failure. Readers perform one read and use that local snapshot for the whole operation, avoiding mixed versions if a refresh occurs midway.

## Interview questions and model answers

**What does volatile provide?**

Volatile access is atomic and ordered in the synchronization order. A volatile write happens-before subsequent reads of that variable, publishing prior actions to code after the read. It does not make compound operations or a multi-variable invariant atomic.

**How does CAS work?**

It atomically compares the current value with an expected value and installs an update only on match. A CAS loop reads state, computes a side-effect-free proposal, attempts CAS, and recomputes after failure. The successful CAS is the linearization point.

**What does lock-free mean?**

Under continued execution, the system as a whole makes progress; individual threads may repeatedly lose and starve. It does not mean no coordination, no blocking anywhere in the runtime, or better latency under all contention.

**Explain ABA.**

A location changes from A to B and back to A, so a comparison with A succeeds despite relevant intermediate history. Use a version/stamp, a design that prevents reuse/restoration, a safe reclamation scheme, or a lock when history matters.

**AtomicLong versus LongAdder?**

`AtomicLong` gives linearizable exact single-location updates and reads. `LongAdder` spreads writes for throughput but its aggregate is not one atomic snapshot. Use it for statistics, not exact balances or IDs.

## Exercises

1. Add `restock` to `Inventory`, including overflow handling and a linearization-point explanation.
2. Implement the inventory with `ReentrantLock` and compare correctness proof and contention behavior.
3. Show a two-atomic invariant that produces a mixed snapshot; replace it with one immutable atomic state.
4. Construct a logical ABA trace for a lock-free stack and add a stamp.
5. Benchmark `AtomicLong` and `LongAdder` across thread counts, then state what correctness each benchmark ignores.
6. Review an `updateAndGet` lambda containing a log or external call and redesign it.

## Chapter summary

Volatile supplies ordered visibility, not compound atomicity. Atomic classes make one-location transitions linearizable, and CAS loops turn failed competition into retry. Correct lock-free design requires side-effect-free proposals, explicit progress expectations, ABA analysis, overflow handling, and contention measurement. Immutable snapshots published through one volatile or atomic reference often provide the clearest practical design.

## Revision checklist

- [ ] I can prove volatile publication using happens-before.
- [ ] I know which invariants volatile cannot protect.
- [ ] I can identify a CAS loop's linearization point.
- [ ] I distinguish lock-free, wait-free, and starvation-free behavior.
- [ ] I can explain logical ABA and versioned solutions.
- [ ] I choose `AtomicLong` versus `LongAdder` by correctness needs.
- [ ] I avoid side effects in retryable update functions.

