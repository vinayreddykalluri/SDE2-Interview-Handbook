# Concurrency Invariants, Cancellation, and Capacity Lab

Concurrency code is not correct because it uses a class from `java.util.concurrent`. It is correct when every shared invariant has an ownership rule, every cross-thread observation has a happens-before path, every task has a completion/cancellation policy, and admitted work fits a resource budget.

Use this review order:

```text
state -> invariant -> owners -> transition -> happens-before edge
      -> progress/cancellation -> admission bound -> evidence
```

If you start by asking “volatile or synchronized?”, you are choosing a mechanism before defining the problem.

## Step 1: classify the shared state

### Thread-confined

One thread owns the mutable value and does not publish it. No synchronization is needed for that state. Confinement can be lexical, request-scoped, actor/event-loop ownership, or task ownership.

### Immutable and safely published

Construct a complete value, validate it, and publish one reference through a happens-before edge. Readers take one snapshot and use it for the whole operation.

```java
record Config(String endpoint, int timeoutMillis) {}

private volatile Config current;

void replace(Config next) {
    current = validate(next);
}
```

Two separate volatile fields for endpoint and timeout would allow mixed versions when a refresh happens between reads.

### Shared mutable with one invariant

Examples include balance plus version, queue plus capacity, or lifecycle state plus owner. Protect the complete transition with one lock or represent it as one immutable state behind one atomic reference.

### Independent statistics

Metrics that tolerate a non-atomic aggregate can use striped accumulators such as `LongAdder`. Do not transfer that choice to an exact balance, sequence, or inventory count.

## Step 2: write the invariant as a sentence

Examples:

- `available >= 0` and version increases exactly once per successful reservation;
- queued plus running work never exceeds the configured admission boundary;
- once shutdown starts, no new externally owned task is accepted;
- a published snapshot's endpoint and timeout come from the same version;
- locks are acquired only in global order `(accountId ascending)`.

An invariant should be checkable at the transition point. “Thread-safe” is a conclusion, not an invariant.

## Step 3: prove visibility with happens-before

Happens-before is not wall-clock order. It is the model's guarantee that one action's effects are visible and ordered before another action.

Common edges:

- actions before `Thread.start()` happen-before actions in the started thread;
- actions in a thread happen-before another thread successfully returns from `join()`;
- an unlock happens-before a later lock of the same monitor/lock;
- a write to a volatile variable happens-before a subsequent read of that variable in synchronization order;
- library synchronizers such as latches, futures, queues, and executors define memory-consistency effects in their contracts.

### Publication proof

```text
writer program order:
answer = 42
ready = true   (volatile write)
       |
       | happens-before
       v
reader reads ready == true (volatile read)
observed = answer
```

The ordinary `answer` write is published through the volatile edge and transitivity. The volatile flag does not make `answer++` atomic, and it does not protect a larger invariant if later writes mutate the published object unsafely.

## Step 4: choose a transition mechanism

### `synchronized` or a lock

Use a lock when a transition spans multiple fields/structures, must wait on a condition, or benefits from one obvious critical section.

```java
synchronized void transfer(Account target, long cents) {
    // One monitor is insufficient if target has independent ownership.
}
```

For two independently locked accounts, define a global acquisition order. Locking source then target can deadlock when another thread transfers in the opposite direction.

Intrinsic locks release automatically on exceptional exit. `ReentrantLock` requires `unlock()` in `finally`. Its additional capabilities—interruptible acquisition, timed try, conditions, optional fairness—are useful only when the protocol needs them.

### Atomic read-modify-write

`AtomicInteger.incrementAndGet()` is one atomic transition on one location. `volatile int count; count++` is still read, add, write and can lose updates.

A CAS loop:

```text
read observed state
derive side-effect-free proposal
CAS(observed, proposal)
  success -> linearization point
  failure -> reread and recompute
```

The proposal function may run repeatedly. Do not charge a card, send a message, or append to an external log inside it.

Atomics do not combine independent locations. If `used` and `limit` form one invariant, store one immutable state in one atomic reference or use a lock.

### Concurrent collection compound operations

A concurrent collection makes its documented individual operations safe; a sequence of operations is not automatically atomic.

Weak:

```java
if (!map.containsKey(key)) {
    map.put(key, createValue());
}
```

Use the collection's compound operation when its semantics fit:

```java
map.computeIfAbsent(key, this::createValue);
```

The mapping function must obey that implementation's contract. Avoid slow external side effects or recursive updates, and do not assume it executes exactly once under every failure/retry scenario unless the API promises that.

## Interruption is a request carried by state

`Thread.interrupt()` does not forcibly kill arbitrary Java code. It sets interrupt status and causes certain blocking methods to throw `InterruptedException`, often clearing status as they do so.

At a boundary, choose one policy:

1. propagate `InterruptedException` when the method contract allows it;
2. translate to a domain cancellation failure while restoring status;
3. if this layer owns the task, clean up and terminate promptly.

Do not swallow:

```java
try {
    queue.take();
} catch (InterruptedException ignored) {
    // broken: caller cancellation was lost
}
```

When translating:

```java
} catch (InterruptedException interruption) {
    Thread.currentThread().interrupt();
    throw new TaskCancelledException("cancelled", interruption);
}
```

Restoring status is not ritual. Do it when higher layers still need to observe the request; do not restore and then continue a long loop that ignores it.

Cancellation must propagate through the whole call graph. Cancelling a `Future` cannot reliably stop an operation that ignores interruption or blocks in a non-interruptible external API.

## Executor configuration is an admission policy

A pool has three separate questions:

- how many tasks may execute concurrently?
- how many may wait?
- what happens when both capacities are full?

```text
submit
  |
  +-> idle/core worker? -> run
  |
  +-> queue has room?   -> wait
  |
  +-> grow allowed?     -> new worker
  |
  `-> reject policy     -> explicit overload response
```

An unbounded queue can keep rejection out of logs while moving failure into latency and memory. A bounded queue makes overload visible. The bound should follow arrival bursts, service time, deadline, memory per task, and acceptable queue delay—not a copied number.

`CallerRunsPolicy` is not universal backpressure. It makes the submitting thread execute the task, which can slow producers, but it can also block an event loop, request thread, or lock holder. Evaluate the submission context.

### Deterministic saturation example

With one worker and one queue slot:

1. task A occupies the worker and waits on a latch;
2. task B occupies the queue slot;
3. task C reaches the rejection policy;
4. release A, drain B, shut down, await termination.

This is a reliable test. Sleeping for “long enough” and hoping the pool is full is not.

## Virtual threads increase concurrency, not downstream capacity

Virtual threads make thread-per-task style practical for many blocking workloads. They do not make CPU work faster, increase a database connection pool, or remove an external rate limit.

```text
100,000 virtual-thread tasks
        |
        v
database permits = 50
```

The application still needs a semaphore, bounded connection pool, rate limiter, queue, or other admission policy around the scarce dependency.

In the Java 21 baseline, some blocking while holding an intrinsic monitor and some native/foreign calls can pin a virtual thread's carrier. Later JDKs changed important pinning behavior, including monitor-related improvements. State the target JDK and confirm with JFR/runtime evidence instead of repeating one version's rule forever.

Thread-local values work with virtual threads but can multiply retained state when task counts are huge. Prefer explicit context where ownership and cleanup matter.

## CompletableFuture: separate execution, completion, and failure

For every stage, ask:

- which executor runs it?
- does it transform a value (`thenApply`) or compose another stage (`thenCompose`)?
- how does failure propagate?
- who owns timeout/cancellation?
- can blocking inside the stage starve the chosen executor?

`exceptionally` recovers from failure with a value. `handle` observes both success and failure. `whenComplete` is usually for observation/side effects and does not inherently turn failure into success.

A timeout on a wrapper stage does not necessarily abort the underlying remote operation. Production cancellation needs a protocol at the I/O/client boundary.

## Failure-mode matrix

| Failure | Required conditions | Evidence | Design response |
|---|---|---|---|
| lost update | non-atomic read-modify-write | invariant counters, stress test | lock/atomic compound operation |
| stale read | data race/no publication edge | code-level HB analysis; controlled test | safe publication/synchronization |
| deadlock | cyclic wait-for graph | multiple thread dumps/lock owners | ordering, try-lock protocol, reduce nested ownership |
| starvation | one task repeatedly denied progress | per-task latency/queue/lock evidence | fairness/partitioning/shorter critical section |
| livelock | threads act but state never advances | CPU profile plus repeated state transitions | randomized/backoff/coordination redesign |
| executor overload | arrival exceeds service/admission | active, queue, rejection, deadline metrics | bound and shed/degrade/scale |
| cancellation leak | task ignores or cannot propagate stop | task age, outstanding futures, thread traces | explicit cancellation protocol |
| virtual-thread pinning | target-version pinning condition | JFR pinned-thread evidence | shorten monitor/native block or upgrade/redesign |

## Testing concurrency without pretending to prove absence of races

One passing run does not prove thread safety. Use layers:

1. **Deterministic protocol tests:** latches/barriers place operations at important states.
2. **Invariant stress tests:** many randomized schedules check postconditions repeatedly.
3. **Specialized tools:** jcstress-style tests for memory-model outcomes; static analysis where useful.
4. **Production evidence:** queue depth, rejection, task age, lock/park events, CPU, cancellation, and deadline metrics.

Avoid `Thread.sleep` as the synchronization mechanism in a correctness test. A sleep can make a race less likely on the fastest machine and more likely under CI load without proving the desired order.

## Executable invariant companion

`ConcurrencyInvariantChecks.java` runs six deterministic suites:

- synchronized and atomic exact counters under concurrent start;
- volatile publication of an ordinary write;
- interruption of a blocking wait with restored status;
- bounded executor saturation and explicit rejection;
- Java 21 virtual-thread execution.

```bash
out=$(mktemp -d)
javac --release 21 -Xlint:all -Werror -d "$out" \
  content/volumes/java/JAVA-07-concurrency-and-memory-model/code/ConcurrencyInvariantChecks.java
java -ea -cp "$out" ConcurrencyInvariantChecks
```

Expected output:

```text
PASS 6 concurrency invariant suites
```

Timeouts in the harness are failure bounds, not ordering mechanisms. Latches establish the intended states.

## Interview room: worked answers

### Does volatile make an object thread-safe?

**Model answer:** A volatile reference safely orders replacement/publication of that reference. It does not make later mutations to the referenced object atomic or safe. I either publish an immutable snapshot and never mutate it, or protect the object's mutable invariant separately.

### When do you prefer a lock over CAS?

**Model answer:** When the invariant spans multiple locations, the transformation is expensive, waiting/conditions are needed, or contention makes retries wasteful. CAS is attractive for a small one-location transition with a side-effect-free proposal. I compare correctness proof and tail latency, not “lock-free sounds faster.”

### What is the linearization point of a CAS reservation?

**Model answer:** The successful compare-and-set that replaces observed immutable state with proposed state. Failed proposals have no external effect and must be recomputed from a new observation.

### How do you prevent transfer deadlock?

**Model answer:** Acquire account locks in one global order independent of transfer direction, release in reverse in `finally`, and define the equal-key case. Alternatively centralize ownership or perform a transactional conditional update. Timeouts detect or bound waiting but do not alone prove correctness.

### How do you size a thread pool?

**Model answer:** I begin with workload type, measured service time, arrival rate/bursts, downstream concurrency, task memory, and deadline. I bound queueing and define rejection/degradation. A formula can seed a test, but production metrics validate the policy.

### Are virtual threads a replacement for a connection pool?

**Model answer:** No. They reduce the cost of blocked thread-per-task code. The database still has finite connections and throughput, so I keep an explicit downstream admission bound.

### Should an `InterruptedException` always be rethrown?

**Model answer:** It should not be silently lost. A boundary can propagate it, translate after restoring status, or consume it when that layer owns termination and actually stops. The policy must preserve cancellation through the call graph.

### Why can a timeout leave work running?

**Model answer:** A timeout can complete a waiting stage exceptionally without cancelling the underlying operation, or cancellation can rely on interruption that the operation ignores. I design timeout and cancellation at the resource/client boundary and observe outstanding work after callers leave.

## Exercises

1. **Foundation:** Draw the happens-before chain for a configuration snapshot published through one volatile reference.
2. **Debugging:** Repair `volatile int count; count++` for an exact counter and state the linearization point.
3. **Interview Core:** Protect a balance/version invariant with both a lock and an atomic immutable state; compare proofs.
4. **Debugging:** Replace sleep-based executor saturation with latches and assert rejection deterministically.
5. **SDE-2 Follow-up:** Design cancellation through controller, service, executor task, and HTTP client.
6. **SDE-2 Follow-up:** Bound 20,000 virtual-thread requests against 50 database connections and define overload behavior.
7. **Production:** Diagnose intermittent request stalls from three thread dumps and queue/lock metrics.

## Worked solutions

1. Writer constructs and validates the complete immutable config, performs a volatile reference write; reader performs a subsequent volatile read and uses that one local reference. Program order plus volatile synchronization plus transitivity publishes all constructor writes.
2. Use `AtomicInteger.incrementAndGet()` for one exact counter or a lock when it belongs to a larger invariant. The atomic operation or locked critical-section transition is the linearization point; volatile alone does not combine read and write.
3. A lock protects both fields in one critical section and can validate/transition in place. An atomic reference stores both in one immutable value and linearizes at successful CAS; the proposal must be side-effect free and can allocate/retry. Choose by contention and surrounding operations.
4. Occupy the only worker with task A blocked on a latch, wait until A signals running, enqueue B into the one-slot queue, submit C and assert the rejection policy, then release and terminate. No timing guess is needed.
5. Define one request cancellation token/deadline, cancel owned futures, propagate interruption, configure client request/connect/read deadlines, and ensure response timeout does not orphan remote work. Record cancellation reason and outstanding-task age.
6. Keep virtual thread per request if it simplifies code, but acquire a 50-permit dependency admission guard with bounded wait tied to the request deadline. Reject or degrade when permits cannot be obtained; expose wait and rejection metrics.
7. Build a wait-for picture from repeated dumps. If the same lock owner is blocked on a downstream call while many request threads queue behind it, shorten/move I/O outside the critical section. Correlate with queue age and downstream latency before changing pool size.

## Final checklist

- I define ownership and invariants before choosing primitives.
- I can draw the exact happens-before path for publication.
- I distinguish visibility, atomicity, ordering, and progress.
- I preserve interruption/cancellation policy across boundaries.
- I treat executors and virtual threads as admission decisions.
- I use deterministic scheduling controls in tests.
- I diagnose from waits, queues, owners, and timelines rather than thread count alone.
