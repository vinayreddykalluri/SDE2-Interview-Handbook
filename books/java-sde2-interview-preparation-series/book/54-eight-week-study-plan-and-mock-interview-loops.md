# Chapter 54: Eight-Week Study Plan and Mock Interview Loops

## Learning objectives

- Convert the book into an eight-week schedule with daily outputs and weekly readiness gates.
- Balance Java depth, DSA execution, backend design, diagnostics, and behavioral stories.
- Run realistic mock loops and score them consistently.
- Use spaced retrieval and an error log instead of passive rereading.
- Recover from missed days, weak mocks, plateaus, and burnout without abandoning the plan.

## Why this matters at SDE-2

Interview preparation fails when study feels productive but never tests retrieval under time. Reading concurrency twice is not the same as proving a happens-before edge aloud. Solving a problem after seeing its pattern is not the same as clarifying, deriving, coding, and testing in 40 minutes. Completing 200 random problems can coexist with weak Java depth and poor communication.

This plan treats preparation as an engineering system. Each week has inputs, outputs, measurements, and a gate. Mocks are feedback, not final exams. The schedule assumes an experienced backend engineer with work obligations and about 15 focused hours per week. It can be compressed or extended without changing the order of learning and verification.

## First-principles model

Use a closed feedback loop:

```text
learn a model
    -> retrieve without notes
    -> implement or diagnose
    -> explain under a time limit
    -> receive evidence-based feedback
    -> record the smallest corrected rule
    -> retrieve again after spacing
```

Every study block must create an artifact: solved code, a spoken answer, a diagram from memory, a scored mock, an incident timeline, or a corrected error card. Highlighting pages is input, not evidence of mastery.

The plan has four parallel tracks:

| Track | Outcome |
|---|---|
| Java/JVM | Precise contracts, internals, trade-offs, and diagnostics |
| Coding/DSA | Correct medium problems in 35-45 minutes with explanation/tests |
| Design/backend | APIs, data, concurrency, reliability, and scaling decisions |
| Communication/behavioral | Structured answers, ownership stories, and collaborative reasoning |

One track must not consume every hour. An SDE-2 loop often mixes coding, Java/concurrency, design, and behavioral evidence.

## Core terminology

- **Active recall:** Producing an answer without looking at notes.
- **Spaced retrieval:** Recalling material after increasing delays, such as 1, 3, 7, and 14 days.
- **Interleaving:** Mixing related problem types so pattern selection must be inferred.
- **Error log:** Short record of an incorrect decision, root cause, corrected rule, and retest date.
- **Mock loop:** Timed simulation followed by scoring and a correction cycle.
- **Readiness gate:** Objective threshold required before increasing interview intensity.
- **Red flag:** Correctness-critical weakness that cannot be offset by a high average.
- **Maintenance set:** Small recurring review set preventing mastered topics from decaying.
- **Recovery plan:** Predefined response to schedule or performance disruption.

## Detailed mechanics

### Before Day 1: establish the baseline

Reserve 90 minutes and do not study first:

1. Answer 20 mixed questions from Chapter 53, two minutes each.
2. Solve one unseen medium array/hash problem in 40 minutes.
3. Explain one production incident in five minutes.
4. Draw source-to-CPU and JVM memory from memory.
5. Score with the rubrics later in this chapter.

Create an error log with columns: date, domain, prompt, observed error, root cause, corrected rule, smallest counterexample, retest dates, and status. Keep answers short. "Review concurrency" is not actionable; "I claimed volatile increment is atomic; retest with a two-thread lost-update trace on D+1/D+3/D+7" is.

Choose one Java 21 JDK distribution and record its exact version. Build a small scratch project with tests, compiler warnings, and commands for `javap`, JFR, and thread dumps. Interviews may use an online editor, so practice both IDE and plain-editor execution.

> **Specification boundary:** Treat finalized Java 21 language/API behavior separately from preview features, vendor tools, and interview-platform restrictions. Preview APIs require explicit enablement and can change; a study schedule or hiring rubric is an editorial policy, not a Java platform guarantee.

### Standard daily schedule

The working-engineer schedule is about 15 hours per week:

**Monday through Friday, 2 hours:**

- 15 minutes: spaced recall cards and one diagram.
- 35 minutes: focused chapter study.
- 45 minutes: implementation, diagnosis, or one coding problem.
- 15 minutes: speak one model answer without notes.
- 10 minutes: tests, error log, and next retrieval dates.

**Saturday, 3 hours:** 60-minute mock, 30-minute break/debrief, 75-minute targeted repair, 15-minute plan update.

**Sunday, 2 hours:** weekly retrieval test, story rehearsal, maintenance problems, and deliberate rest. Stop early if the gate is met and fatigue is high.

For a 90-minute weekday, keep 10 minutes recall, 30 minutes concept, 35 minutes code, and 15 minutes explanation/log. Do not remove retrieval or explanation; reduce scope. For a full-time search, add a second independent block after a long break, not a five-hour continuous session.

### Week 1: computing model and JVM foundations

Goal: explain how Java executes and where memory/diagnostic evidence belongs.

| Day | Primary work | Required output |
|---|---|---|
| 1 | Baseline assessment and environment | Scores, error log, exact JDK/runtime record |
| 2 | Why Java, JDK/JVM, compatibility, tools | 90-second source-to-runtime answer |
| 3 | Compilation, bytecode, class files | Annotated `javap -c` for one class |
| 4 | JVM architecture and runtime data areas | Memory diagram from memory |
| 5 | Class loading and initialization | Failure table: CNFE, NCDFE, linkage/init errors |
| 6 | Object layout, calls, recursion | Coding mock plus reference/frame dry run |
| 7 | GC and JIT overview | Collector comparison and weekly retrieval score |

Implementation tasks: compile with `--release 17`, inspect bytecode, create a static-initialization failure safely, capture a thread dump, and use a small JFR recording. Do not tune flags. The purpose is evidence literacy.

Gate 1: score at least 3/4 on source-to-CPU, class lifecycle, runtime areas, object creation, and GC reachability. Solve one medium array/hash problem in 50 minutes with correct complexity. If the JVM diagram still mixes Metaspace with heap guarantees, repeat the diagram on Days 8 and 10.

### Week 2: Java language engineering

Goal: write and explain precise Java 17/21 code, equality, exceptions, generics, and object design.

| Day | Primary work | Required output |
|---|---|---|
| 8 | Primitives, numeric semantics, operators | Overflow/promotion prediction test |
| 9 | Methods, overloading, varargs, pass-by-value | Five call-resolution dry runs |
| 10 | Arrays, strings, Unicode, text blocks | String/array memory and complexity explanation |
| 11 | Classes, access, inheritance, composition | Refactor inheritance to composition |
| 12 | Interfaces, sealed types, records, equality | Immutable value type with tests |
| 13 | Exceptions, resources, nested types, reflection | Suppressed-exception and reflection exercise |
| 14 | Generics, erasure, lambdas, Java 21 features | Language deep-dive mock and error repair |

Complete two DSA problems this week involving strings/two pointers. For every solution, test empty input, one element, duplicates, boundary indices, and overflow where applicable. Explain when a record is inappropriate, why a final collection is still mutable, and one heap-pollution example.

Gate 2: average 3/4 across language mock dimensions with no red flags on pass-by-value, equality/hash contract, exception resource handling, or generic variance. Implement a correct immutable class in 25 minutes. If overload/generic reasoning is weak, use ten small compile-prediction snippets rather than rereading entire chapters.

### Week 3: collections, streams, and I/O

Goal: select a data structure from operations and explain implementation-sensitive costs.

| Day | Primary work | Required output |
|---|---|---|
| 15 | Collection hierarchy and List trade-offs | Selection table for six workloads |
| 16 | HashMap/HashSet internals | Hash/equality dry run and mutable-key failure |
| 17 | TreeMap/TreeSet and ordering | Three valid comparators with tests |
| 18 | Queue, deque, priority queue, heaps | Top-K implementation and complexity |
| 19 | Sorting, selection, Comparable/Comparator | Comparator contract review |
| 20 | Streams, collectors, Optional | Sequential pipeline plus safe collector |
| 21 | NIO.2, buffers, serialization boundaries | File-copy/buffer state exercise and mock |

Coding maintenance: one sliding-window problem, one heap/top-K problem, and one interval/sorting problem. Repeat one older problem from memory after seven days; do not count immediate re-solves as mastery.

Gate 3: choose ArrayList/HashMap/TreeMap/heap/deque correctly from a new scenario, state qualified complexity, and solve two collection-heavy medium problems within 45 minutes each on separate days. Explain why fail-fast is not thread safety and why parallel streams are not a universal speed switch.

### Week 4: concurrency and the Java Memory Model

Goal: prove visibility/atomicity and design cancellation, locking, scheduling, and virtual-thread capacity.

| Day | Primary work | Required output |
|---|---|---|
| 22 | Threads, states, interruption, shutdown | Owned-worker shutdown protocol |
| 23 | Happens-before, volatile, safe publication | HB graph for two publication patterns |
| 24 | Intrinsic locks, wait/notify | Predicate-loop bounded buffer |
| 25 | ReentrantLock/Condition, deadlock | Lock-order transfer and wait-for graph |
| 26 | Atomics, CAS, ABA, accumulators | CAS dry run and linearization point |
| 27 | Executors, bounded queues, futures | Executor capacity plan and shutdown code |
| 28 | Concurrent collections, CompletableFuture, virtual threads | Full concurrency mock and incident diagnosis |

Run one stress exercise, but pair it with a proof: a lost-update counter, safe volatile publication, or condition wait. Capture thread dumps from a deliberate safe deadlock and a healthy idle executor; learn the difference. On Java 21, create virtual threads and demonstrate a semaphore protecting a simulated downstream.

Gate 4: no score below 3 on interruption, happens-before, lock/condition loops, executor admission, and virtual-thread capacity. You must explain why `volatile count++` fails, why `Future.get(timeout)` does not cancel, and why a virtual-thread migration can overload JDBC. This is a hard gate; concurrency misconceptions should delay interviews involving deep Java.

### Week 5: DSA foundations in Java

Goal: make problem-solving communication and core linear structures automatic.

| Day | Primary work | Required output |
|---|---|---|
| 29 | Complexity and interview method | Verbal template plus brute-force comparison |
| 30 | Arrays, hashing, prefix sums | Two mediums, one timed |
| 31 | Two pointers and sliding windows | Invariant written before implementation |
| 32 | Linked lists | Reverse, cycle, and merge dry runs |
| 33 | Stacks, queues, monotonic structures | Next-greater/interval exercise |
| 34 | Trees and recursive reasoning | Traversal plus depth-risk discussion |
| 35 | BSTs and heaps | 60-minute coding mock and repair |

Problem selection should be representative, not random. For each pattern, solve one learning problem with notes, one related problem without notes after a day, and one mixed problem where the pattern is not named. Maintain Java hygiene: use `ArrayDeque` for stack/queue, avoid subtraction comparators, handle integer overflow, and choose meaningful helper methods.

Gate 5: in three unseen mediums, achieve two correct solutions within 45 minutes and one viable solution with minor correction. Every solution must include examples, invariant, complexity, and tests. If recognition is good but implementation fails, reduce new problems and do line-by-line dry runs plus typed reimplementation from a blank file.

### Week 6: advanced DSA and low-level design

Goal: handle graph/state-space problems and translate requirements into maintainable Java objects.

| Day | Primary work | Required output |
|---|---|---|
| 36 | Graph representation, BFS/DFS | Component/shortest-path comparison |
| 37 | Topological sort and union-find | Dependency ordering implementation |
| 38 | Weighted shortest paths | Dijkstra assumptions and stale-entry trace |
| 39 | Backtracking and pruning | State-choice-undo invariant |
| 40 | Greedy reasoning and counterexamples | Exchange argument or rejected greedy |
| 41 | Dynamic programming | State/transition/base/order derivation |
| 42 | SOLID, API design, patterns | 60-minute LLD mock plus coding maintenance |

Use one graph, one backtracking, and two DP problems; quality beats volume. For LLD, practice a rate limiter, parking lot, task scheduler, or notification service. State scope, concurrency, extension points, failure behavior, and what belongs outside the process.

Gate 6: solve one unseen graph medium and one DP medium with correct state/complexity; reach 3/4 on an LLD rubric for requirements, model, APIs, invariants, extensibility, and testability. A memorized pattern without a correctness explanation does not pass.

### Week 7: backend, performance, reliability, and full loops

Goal: connect Java mechanics to service design and production evidence.

| Day | Primary work | Required output |
|---|---|---|
| 43 | JDBC, connection pools, transactions | Transaction/isolation failure walkthrough |
| 44 | Serialization, APIs, idempotency | Idempotent create state machine |
| 45 | Performance method and JMH | Valid benchmark plan, no premature tuning |
| 46 | jcmd, JFR, dumps, GC incidents | Investigation playbook for p99/OOM/high CPU |
| 47 | Testing, build tools, dependencies | Test pyramid and reproducible build record |
| 48 | Security and reliability | Threat review for one API |
| 49 | Full coding + Java + design loop | Scores, recording, top-three repair plan |

Prepare a story bank: difficult bug, performance improvement, conflict/trade-off, ownership beyond role, failed decision and learning, ambiguous project, and mentoring/collaboration. Each story should be five minutes with a 30-second summary. Include metrics but avoid confidential detail.

Gate 7: complete a full loop without a correctness red flag. Diagnose one incident using a timeline and tools, design idempotency/transactions correctly, and score at least 3 on testing/security. If system design is broad but shallow, practice one subsystem deeply rather than adding more architecture boxes.

### Week 8: simulation, consolidation, and interview taper

Goal: produce stable performance, not learn an entirely new curriculum.

| Day | Primary work | Required output |
|---|---|---|
| 50 | Full mock loop 1 | Independent scores and error priorities |
| 51 | Repair top correctness weakness | Counterexample, implementation, retest |
| 52 | Full mock loop 2 | Compare score trend and fatigue |
| 53 | Repair communication/timing | 30/90-second answer set |
| 54 | Company-shaped mixed loop | Coding plus likely Java/design emphasis |
| 55 | Light maintenance and logistics | Environment, questions, story cards, sleep plan |
| 56 | Retrieval only and rest | No heavy new problems; final readiness decision |

Schedule real interviews only after a gate if possible, with lower-priority companies before top choices but without treating any interviewer disrespectfully. Leave recovery time between loops. The final 48 hours should reduce volume, preserve sleep, and rehearse opening frameworks, not attempt a new advanced topic.

### Mock loop formats

**Coding mock, 60 minutes:** 5 minutes clarification/examples, 5 minutes baseline/invariant, 35 minutes implementation, 10 minutes tests/complexity, 5 minutes feedback. The interviewer should not rescue pattern recognition immediately.

**Java deep dive, 45 minutes:** six rapid questions, two deep follow-up trees, one code/concurrency dry run, and five-minute summary feedback.

**Low-level design, 60 minutes:** requirements/scope 10, model/API 15, core flows/invariants 15, concurrency/failure/scaling 10, trade-offs/testing 10.

**Backend/system design, 60 minutes:** requirements/SLOs 10, estimates/interfaces 10, data/workflows 15, scaling/reliability 15, deep dive/trade-offs 10.

**Behavioral, 45 minutes:** four stories with follow-ups on ownership, conflict, failure, data, and learning. Feedback must identify missing context, personal action, trade-off, and outcome.

Debrief within 30 minutes: score first without notes, compare interviewer score, identify the earliest wrong decision, write one corrected rule, and schedule a retest. Do not immediately redo the same prompt from memory and call it fixed; use a transfer problem first.

## Worked Java example

This small Java 21-compatible tracker encodes a readiness gate instead of relying on mood:

```java
import java.util.List;

public final class ReadinessTracker {
    public record MockScore(
            int correctness,
            int communication,
            int complexity,
            int testing,
            int javaDepth,
            int design) {

        public MockScore {
            int[] values = {
                    correctness, communication, complexity,
                    testing, javaDepth, design
            };
            for (int value : values) {
                if (value < 0 || value > 4) {
                    throw new IllegalArgumentException("score must be 0..4");
                }
            }
        }

        boolean clearsCriticalGate() {
            return correctness >= 3
                    && communication >= 3
                    && javaDepth >= 3
                    && testing >= 2;
        }
    }

    static double weighted(MockScore score) {
        return score.correctness() * 0.25
                + score.communication() * 0.15
                + score.complexity() * 0.15
                + score.testing() * 0.10
                + score.javaDepth() * 0.20
                + score.design() * 0.15;
    }

    static boolean ready(List<MockScore> scores) {
        if (scores.size() < 3) return false;
        List<MockScore> recent = scores.subList(scores.size() - 3, scores.size());
        return recent.stream().allMatch(MockScore::clearsCriticalGate)
                && recent.stream().mapToDouble(ReadinessTracker::weighted)
                        .average().orElse(0.0) >= 3.2;
    }
}
```

Weights are a planning policy, not a universal hiring rubric. Adjust them for the target role, but never let a high design score average away incorrect/racy code. Keep red-flag gates.

## Execution or memory walkthrough

Suppose the last three mock scores are:

```text
M1 = correctness 3, communication 3, complexity 3,
     testing 2, javaDepth 3, design 3       weighted 2.90

M2 = correctness 4, communication 3, complexity 3,
     testing 3, javaDepth 3, design 3       weighted 3.25

M3 = correctness 4, communication 4, complexity 3,
     testing 3, javaDepth 4, design 3       weighted 3.60
```

All three clear the critical minima, but the average is 3.25, so `ready` returns true. If M3 had Java depth 2, the average might remain acceptable, but `clearsCriticalGate` would fail and readiness would be false.

The method examines only the latest three mocks so old baseline failures do not dominate forever. That also means mock selection must be representative. Three easy array problems cannot establish system-design or concurrency readiness. Store domain-specific scorecards alongside the aggregate.

At runtime, record instances and list elements are ordinary objects; no concurrency is implied. If multiple evaluators update a shared tracker, publish immutable score-list snapshots or use an appropriate thread-safe store. The example calculates a decision; it does not persist evidence.

## Complexity and performance

`weighted` is O(1). `ready` examines three recent records, effectively O(1), with O(1) auxiliary space aside from stream machinery. A generalized window of `k` mocks would be O(k).

The standard schedule invests about 15 hours/week, or 120 hours over eight weeks. Protect those hours from context switching. Two uninterrupted 60-minute blocks usually produce more evidence than four scattered half-hours. Track completed outputs and scores, not chair time.

Spaced retrieval should be selective. A new error is recalled after approximately 1, 3, 7, and 14 days; mastered cards move to weekly maintenance. If review consumes more than 25 percent of study time, retire duplicates and keep rules with counterexamples rather than accumulating trivia.

Problem count is a weak throughput metric. A better weekly dashboard includes unseen timed correctness, median time to viable approach, implementation defect count, Java question score, mock trend, sleep/fatigue, and number of unresolved red flags.

## Edge cases and common mistakes

- Reading the entire book sequentially without retrieval or implementation.
- Solving only familiar tagged problems and overestimating pattern recognition.
- Counting a solution read as a problem solved.
- Doing mocks without written scores or repeating mocks without repairing root causes.
- Cramming missed work into one exhausted weekend.
- Letting DSA consume all Java, design, or behavioral time.
- Changing resources every few days instead of completing one feedback loop.
- Memorizing HotSpot internals without contracts or production evidence.
- Scheduling top-choice interviews before concurrency/correctness red flags clear.
- Treating a weighted average as permission to ignore a score of 1 in security or correctness.
- Sacrificing sleep; fatigue directly harms working memory, communication, and coding accuracy.
- Studying company trivia instead of role requirements and reusable fundamentals.

## Production engineering notes

Treat preparation artifacts like an engineering repository: dated scorecards, runnable code, failing/passing tests, diagrams, and a changelog of corrected rules. Keep personal/employer data out of examples. Use synthetic incidents when a real story is confidential.

Before each interview, verify JDK/editor assumptions, meeting link, time zone, backup network/audio, allowed references, and whether coding occurs in a shared editor. Prepare concise questions about team ownership, reliability expectations, deployment, on-call, technical debt, mentorship, and success in the first six months.

> **HotSpot note:** Practice implementation-specific diagnostics on the same Java 21 runtime you recorded, but rehearse answers at the portable contract level first. Tool output and defaults can differ in the interviewer's JDK.

### Recovery plans

**Missed one or two days:** Do not double the next day. Preserve recall, the week's mock, and the gate-critical topic. Drop optional new problems and move one deep dive to Sunday.

**Lost a full week:** Extend the calendar by a week if possible. If the interview date cannot move, retain Weeks 1-4 correctness foundations, core DSA, one design loop, and two final mocks; reduce breadth, not verification.

**Repeated coding failure:** Classify: misunderstood contract, wrong pattern, broken invariant, Java implementation defect, or time management. Do two untimed derivations, one blank-file implementation, then one transfer problem timed. Avoid ten more random problems.

**Weak Java deep dive:** Build 30/90-second answers and draw mechanisms. For each wrong claim, attach a counterexample and specification/implementation label. Retest verbally with follow-ups.

**Mock anxiety:** Increase exposure gradually: record alone, peer with known questions, peer with unseen questions, then full loop. Keep the same opening framework so the first two minutes are automatic.

**Plateau:** Inspect the error distribution. If the same root cause repeats, change the drill. If errors are diverse, reduce cognitive load and improve rest. Seek external feedback because self-scoring may be miscalibrated.

**Burnout or sleep loss:** Take a full rest day, cut volume by one third for a week, preserve only recall and one mock, and restore sleep/exercise. Extending the plan is cheaper than reinforcing careless habits.

## Interview questions and model answers

**How do I know I am ready?**

Use three representative recent mocks with weighted average at least 3.2/4, no critical score below the gate, two unseen coding mediums solved correctly under time on different days, and one successful Java/concurrency plus design loop. Confidence is supporting information, not the gate.

**How many coding problems should I solve?**

Enough to demonstrate transfer across core patterns. Fifty deeply reviewed mixed problems can outperform 200 copied solutions. Track unseen timed correctness, explanation, tests, and delayed re-solves rather than a universal count.

**Should I postpone an interview after a failed mock?**

Look at failure type and timing. One difficult prompt is noise; a repeated correctness red flag in concurrency, coding, or design is signal. If rescheduling is feasible, repair and pass a transfer mock first. Do not wait for perfection.

**What if I have only four weeks?**

Run the baseline, combine Weeks 1-2, Weeks 3-4, Weeks 5-6, and Weeks 7-8. Keep one mock and debrief each week. Reduce problem breadth and optional internals, but retain JMM, collections, core DSA, backend correctness, and final simulations.

**How should mocks be scored?**

Use observable behavior: correct assumptions, invariant, implementation, tests, complexity, communication, depth, trade-offs, and recovery from hints. Score independently before discussion and record the earliest wrong decision, not only final outcome.

**What should I do the day before?**

Light retrieval, one easy confidence problem, logistics, story headlines, questions for interviewers, normal food/exercise, and sufficient sleep. No long mock, new advanced topic, or late-night cramming.

## Exercises

1. Run the baseline and create the first eight error cards with D+1/D+3/D+7 dates.
2. Customize the 56-day table to your interview format without deleting any critical gate.
3. Build scorecards for coding, Java deep dive, LLD, backend design, and behavioral rounds.
4. Implement `ReadinessTracker`, add tests for insufficient mocks, boundary scores, and one red flag.
5. Schedule the first four mocks now, including reviewer and debrief time.
6. Write recovery versions for 90 minutes/day, one missed week, and a fixed interview in 14 days.
7. Record one 90-second JVM answer and one five-minute incident story; score and repeat after three days.
8. At the end of each week, publish a private one-page review: evidence, red flags, repaired rules, next gate.

## Chapter summary

An effective eight-week plan alternates learning with retrieval, implementation, explanation, mocks, and correction. Each week produces artifacts and ends at an objective gate. Java/JVM foundations and concurrency correctness precede high interview volume; DSA, design, backend reliability, testing, security, and behavioral evidence remain parallel tracks. Scored mock trends and critical minima determine readiness, while predefined recovery plans keep one disruption from destroying the schedule.

## Revision checklist

- [ ] I completed a no-study baseline and created an actionable error log.
- [ ] My calendar contains daily outputs, weekly mocks, debriefs, and rest.
- [ ] I use 1/3/7/14-day retrieval for corrected rules.
- [ ] Every week has a measurable gate and recovery action.
- [ ] My coding practice includes unseen timed transfer problems.
- [ ] I score Java, coding, design, testing/security, and communication separately.
- [ ] I have at least three representative recent mocks with no critical red flag.
- [ ] My production stories include evidence, trade-offs, actions, and measured outcomes.
- [ ] I have a reduced-volume plan for missed time or burnout.
- [ ] The final 48 hours prioritize retrieval, logistics, and sleep.
