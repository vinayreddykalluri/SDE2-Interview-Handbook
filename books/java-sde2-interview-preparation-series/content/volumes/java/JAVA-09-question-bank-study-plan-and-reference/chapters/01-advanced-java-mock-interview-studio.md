# Advanced Java Mock Interview Studio

Reading an answer and producing an answer under pressure are different skills. This studio turns the Advanced Java books into spoken reasoning, code prediction, debugging, and production diagnosis.

It does not reteach Java Fundamentals or collection implementations. When a gap is exposed, return to the owning chapter, repair the model, and rerun the card from memory.

## The answer ladder

For a deep Java question, build the response in layers:

1. **Contract:** the rule the caller/runtime can rely on.
2. **Mechanism:** the minimum internals that explain the rule.
3. **Edge:** one case that breaks the common shortcut.
4. **Decision:** when you would choose or avoid the technique.
5. **Evidence:** how you would validate the production claim.

Example question: “What does volatile do?”

Weak: “It writes to main memory and other threads see it.”

Strong 45-second answer:

> A volatile write happens-before a subsequent volatile read of the same variable, so prior writes by the writer become visible to code after the reader's read through program order and transitivity. Each volatile access is atomic, but a compound operation such as `count++` is not. I use volatile for an independent flag or immutable snapshot reference; I use an atomic operation or lock for read-modify-write or multi-field invariants.

Two-minute follow-up:

> I would draw the exact happens-before chain. I would not explain it as “every read goes to RAM,” because the Java Memory Model defines observable ordering rather than one hardware cache protocol. In production I would also define cancellation/ownership and test the protocol with latches or a memory-model harness, not sleeps.

The longer answer adds value; it does not replace clarity with vocabulary.

## Mock scoring rubric

Score each dimension from 0 to 2:

| Dimension | 0 | 1 | 2 |
|---|---|---|---|
| correctness | incorrect/material omission | mostly right but qualified poorly | precise contract and version boundary |
| mechanism | slogan only | partial mechanism | clear state transition/selection/HB path |
| edge case | none | named without consequence | concrete failure and correction |
| engineering judgment | feature preference | one trade-off | scope, ownership, compatibility, capacity |
| evidence | guesses/tool list | one relevant signal | discriminating, bounded, reproducible evidence |
| communication | unstructured | understandable | layered answer with direct conclusion |

Ten or more out of twelve is strong. Any zero in correctness is a repair item even if the total is high.

## Card 1: loading is not initialization

**Interviewer:** Does accessing a static member always initialize the declaring class?

**Worked answer:** No. Loading, linking, and initialization are distinct. Active use such as invoking a static method, creating an instance, or reading a non-constant static field triggers initialization under the language rules. A compile-time constant variable can be inlined into the client and read without initializing the declaring class. I can confirm the distinction by inspecting caller bytecode and a side-effecting initializer or class-initialization log.

**Follow-up:** The constant changed in a library, but one service prints the old value. Why?

**Worked answer:** The service may have been separately compiled against the old constant and contains the old embedded value. Replacing only the library artifact does not rewrite the client bytecode; recompile/redeploy the client and avoid using compile-time constants for values intended to change independently.

**Red flag:** “The class loads when the JVM starts.” Loading can be lazy and loader-specific.

## Card 2: lookup failure versus failed definition

**Interviewer:** Compare `ClassNotFoundException` and `NoClassDefFoundError`.

**Worked answer:** `ClassNotFoundException` is a checked failure reported by explicit lookup APIs when a loader cannot find a requested class. `NoClassDefFoundError` is an unchecked linkage/runtime failure when executing code requires a definition that cannot be provided, and it can also follow a previous class-initialization failure. I inspect the full cause, binary name, artifact set, and initiating/defining loader rather than treating both as “classpath missing.”

**Follow-up:** Why can two identical class files fail assignment compatibility?

**Worked answer:** Runtime type identity includes binary name and defining loader. Independent definitions by two loaders are distinct types. Shared parent-loaded contracts are the normal plugin bridge.

## Card 3: “stack versus heap” under optimization

**Interviewer:** Are Java objects always on the heap?

**Worked answer:** Java semantics define object identity, fields, references, and reachability; the JVM model supports reasoning about frames and heap state. An optimizing runtime can eliminate or scalar-replace a non-escaping allocation while preserving behavior, so I do not promise a universal physical location from source alone. Object layout, compressed references, and headers are runtime configuration details that I would measure on the target VM if they matter.

**Follow-up:** Does setting a local to null free the object?

**Worked answer:** It removes one reference value. The object remains reachable if any other strong path exists, and becoming unreachable does not guarantee immediate collection. `System.gc()` is not a deterministic per-object reclamation contract.

## Card 4: overload, override, static hiding

**Interviewer:** A parent reference points to a child object. Which method runs?

**Worked answer:** First the compiler selects an overload signature using declared receiver and argument types. Then an overridable instance signature dispatches using the runtime receiver. A child-only overload is not selected through a parent expression. Static methods and fields are resolved by the qualifying/declared type and are hidden, not dynamically overridden.

**Follow-up:** Can return type alone overload a method?

**Worked answer:** No. The invocation would be ambiguous before assignment context could choose it. An override may use a covariant return type because the parameter signature already identifies the inherited method.

## Card 5: record versus immutable aggregate

**Interviewer:** Is a record immutable?

**Worked answer:** Its component fields are final and the generated API is value-oriented, but a component can refer to mutable state. If a caller passes a mutable list, both caller and record can observe later mutations unless the constructor defensively copies. `List.copyOf` prevents membership mutation through the record but does not deep-copy mutable elements. I define the required depth and ownership explicitly.

**Follow-up:** When is a record the wrong choice?

**Worked answer:** When lifecycle identity, framework construction/proxy requirements, complex encapsulated mutation, or an intentionally extensible class hierarchy is central. Record is a semantic commitment to a transparent component-based value shape, not merely less boilerplate.

## Card 6: variance and heap pollution

**Interviewer:** Why is `List<Integer>` not a subtype of `List<Number>`?

**Worked answer:** If it were, code holding the `List<Number>` view could add a `Double`, violating the original integer list. Java generics are ordinarily invariant. For a method that only reads numbers I can accept `List<? extends Number>`; for a destination that receives integers I can use `List<? super Integer>`.

**Follow-up:** Why can raw-type heap pollution fail later?

**Worked answer:** A raw alias can perform an unchecked write that the parameterized API forbids. Retrieval through `List<String>` can later execute a compiler-inserted cast and throw `ClassCastException`. I trace the bug back to the unchecked boundary, not the read that detected it.

## Card 7: two failures in try-with-resources

**Interviewer:** The body and `close()` both throw. Which failure escapes?

**Worked answer:** The body failure remains primary and the close failure is attached as suppressed. Multiple resources close in reverse declaration order, with their close failures suppressed in that order after a primary body/construction failure as defined by the translation. I preserve the whole throwable, including causes and suppressed failures, at diagnostic boundaries.

**Follow-up:** Should a helper close a reader passed by its caller?

**Worked answer:** Only if the contract transfers ownership. Normally the creator owns closing; silently closing caller-owned resources creates distant failures and makes composition difficult.

## Card 8: volatile is not a compound transition

**Interviewer:** Is `volatile int count; count++;` safe?

**Worked answer:** Individual volatile reads/writes have visibility and ordering guarantees, but increment is read, add, write. Threads can read the same old value and lose an update. For one exact counter use an atomic increment or lock; for a multi-field invariant use one lock or one immutable state behind an atomic reference.

**Follow-up:** When is `LongAdder` better?

**Worked answer:** For high-contention statistics where a momentarily non-atomic sum is acceptable. It is not a linearizable exact balance or sequence generator.

## Card 9: virtual threads and capacity

**Interviewer:** Can virtual threads replace a bounded executor and database pool?

**Worked answer:** They reduce the cost of thread-per-task blocking and can simplify code, but they do not increase database connections, downstream throughput, CPU, or rate limits. I keep an explicit dependency admission bound tied to deadlines and overload behavior. I also state the target JDK because pinning behavior and diagnostics evolved after Java 21.

**Follow-up:** Why can thread locals be risky?

**Worked answer:** A small per-thread value multiplied by a very large task/thread count can become significant, and cleanup/ownership can be obscure. Explicit context or scoped designs may be clearer; any alternative must match the target Java version and stability level.

## Card 10: deadlock incident

**Interviewer:** Production requests freeze intermittently. How do you prove deadlock?

**Worked answer:** Capture multiple thread dumps during the symptom and construct a wait-for graph from lock owners and waiters. A cycle with no progress across snapshots supports deadlock; a long but eventually moving owner may be contention or downstream blocking. I correlate lock/park events, request/queue age, and owner stacks before changing pool size.

**Follow-up:** How do you prevent two-account transfer deadlock?

**Worked answer:** Acquire both account locks in one global key order independent of transfer direction, handle equal keys, and release in reverse in `finally`. A transaction or single-owner partition may be a cleaner boundary. A timeout can bound waiting but is not the primary correctness proof.

## Card 11: benchmark credibility

**Interviewer:** Parser A is 30% faster in a loop timed with `nanoTime`. Ship it?

**Worked answer:** Not yet. I define the production decision and representative inputs, validate equal output, and use JMH with forks, warmup, measurement, state scope, and result consumption. I control JDK/hardware and report variance plus allocation if relevant. A one-process cold loop can measure startup, constant folding, dead-code elimination, or noise rather than parser performance.

**Follow-up:** Does JMH guarantee relevance?

**Worked answer:** No. It addresses many harness mechanics. It cannot make unrealistic payloads, wrong concurrency, or a meaningless metric represent production.

## Card 12: leak, churn, or native footprint

**Interviewer:** Memory rises every hour. Is it a heap leak?

**Worked answer:** I separate heap used/post-GC live set from process RSS and allocation rate. High allocation with stable post-GC occupancy is churn. Rising post-GC occupancy suggests retention. Flat heap with rising RSS points toward direct/native buffers, threads/stacks, metaspace/class loaders, code cache, JNI, or allocator behavior. I identify retained owners or native categories before increasing heap.

**Follow-up:** Why can a bigger heap be harmful?

**Worked answer:** It can delay failure and change GC frequency/pause/recovery/footprint without fixing unbounded retention. It may be a temporary capacity mitigation only with mechanism, headroom analysis, and rollback.

## Full mock: from code to production

Run this as a 35-minute interview.

### Prompt

A Java 21 service lazily creates an inventory component, reads handler annotations at startup, reserves stock concurrently, and performs downstream work on virtual threads. After a release, startup is slower and p99 rises under load.

### Expected clarification

Ask:

- what exactly marks startup readiness and when did it regress?
- request rate, p50/p95/p99/error change and baseline;
- inventory scope: one JVM, cluster, or database authority?
- downstream concurrency/connection limits and deadlines;
- JDK build/flags/artifact/config changes;
- whether stock correctness or only latency is failing.

### Design answer

- Use initialization-on-demand only for cheap deterministic in-process construction. Move external I/O out of static initialization into explicit lifecycle with timeout and observability.
- Cache reflection metadata with class-loader-aware ownership; define annotation retention, bridge/synthetic filtering, and module access.
- Store availability and version in one immutable state behind one atomic reference, or use a lock. External inventory requires transactional/distributed authority; a JVM atomic is not enough.
- Virtual threads can model blocking requests but retain a bounded downstream admission policy and cancellation deadline.
- Instrument queue/admission wait, dependency duration, atomic retry/contention, and startup phases.

### Evidence answer

Capture a startup JFR/class-loading timeline to separate reflection/class generation/static initialization from I/O. During load, correlate p99 with downstream pool utilization, permit wait, JFR socket/park/monitor events, CPU, allocation, and GC. Change one mechanism at a time—such as deferring metadata scan or bounding fan-out—and verify the predicted signal.

### SDE-2 follow-up

If two pods each hold an atomic inventory of five, both can reserve five. The local invariant is correct while the system invariant is wrong. Move authority to a database conditional update, partition owner, or other coordinated store and design idempotency/retry semantics.

## Executable readiness assessment

`AdvancedJavaReadinessAssessment.java` turns six advanced concepts into runnable checks:

- initialization-on-demand holder behavior;
- runtime annotation retention and discovery;
- exhaustive sealed-domain dispatch;
- one-location atomic immutable-state invariant;
- explicit asynchronous failure recovery;
- Java 21 virtual-thread task execution.

```bash
out=$(mktemp -d)
javac --release 21 -Xlint:all -Werror -d "$out" \
  content/volumes/java/JAVA-09-question-bank-study-plan-and-reference/code/AdvancedJavaReadinessAssessment.java
java -ea -cp "$out" AdvancedJavaReadinessAssessment
```

Expected output:

```text
PASS 6 advanced Java readiness scenarios
```

Passing the program validates the examples, not your interview readiness. Read each method, predict the transition and failure mode aloud, then explain where the in-process guarantee ends.

## Error log that changes the next study session

Use one row per meaningful miss:

| Date | Prompt | Failure type | Wrong model | Correct rule | New edge test | Retest date |
|---|---|---|---|---|---|---|
| Aug 2 | volatile counter | mechanism | volatile makes `++` atomic | volatile orders accesses; increment is compound | four-thread exact count | Aug 4 |

Failure types:

- recall: term/rule unavailable;
- model: incorrect mechanism;
- boundary: missed version/scope/ownership;
- implementation: code violates stated model;
- communication: answer correct but unstructured;
- evidence: production claim cannot be tested.

Do not reread the whole book for one miss. Route it:

```text
miss -> owning chapter -> one worked example -> one changed edge case
     -> 24-hour closed-book retest -> one-week mixed retest
```

## Exercises

1. **Rapid recall:** Answer Cards 1–12 in 45 seconds each without notes.
2. **Mechanism:** Draw selection/dispatch for Card 4 and happens-before for Card 8.
3. **Debugging:** Modify the executable assessment so one raw/mutable/concurrent boundary fails; identify the earliest unsafe action.
4. **Production:** Run the full mock and produce three competing p99 hypotheses with discriminating evidence.
5. **SDE-2 Follow-up:** Move the inventory invariant from one JVM to a multi-instance service design.
6. **Communication:** Turn a five-minute JIT answer into a correct 45-second answer plus optional depth.

## Worked solutions

1. A strong response reaches contract, mechanism, edge, and decision within 45 seconds; evidence can be the follow-up. Mark any answer that depends on “always,” “main memory,” or a tool list without a hypothesis.
2. Card 4 has compile-time overload selection followed only then by runtime override dispatch. Card 8 needs program order -> volatile write -> subsequent volatile read -> program order; it still lacks one atomic `++` transition.
3. Example: mutate a list held by a record and observe shallow mutability, or split atomic inventory into two independent atomics and detect a mixed invariant. The earliest unsafe action is the alias mutation or split transition, not the later read that reveals it.
4. Downstream saturation predicts permit/pool wait and dependency duration; lock contention predicts repeated common owners; GC predicts timeline-aligned pause/allocation/live-set evidence. Preserve baseline and change one suspected mechanism.
5. Use a single authoritative conditional update/transaction or partition owner, include a request id for idempotency, define retryable conflicts versus terminal insufficiency, and reconcile ambiguous timeout outcomes. Local atomics may protect only local caches/metrics.
6. Lead with: “The runtime may interpret first, profile hot paths, compile and inline speculatively, and deoptimize if assumptions fail; Java behavior is unchanged and exact tiers are VM-specific.” Add benchmark/JFR evidence only if asked.

## Readiness decision

You are ready for a mixed SDE-2 Java loop when you can:

- score at least 10/12 on two different card sets one week apart;
- predict companion outputs before running them;
- repair a failed invariant without changing the contract mid-answer;
- separate Java guarantees from JVM/HotSpot observations;
- move from symptom to competing hypotheses and evidence;
- state process/JVM boundaries in every concurrency or caching claim;
- admit uncertainty precisely and name how you would verify it.

If you miss only one domain, do not restart the entire Java shelf. Return to the owning `JAVA-*` book, solve one unseen edge case, then rerun a mixed mock so recognition is not tied to chapter order.
