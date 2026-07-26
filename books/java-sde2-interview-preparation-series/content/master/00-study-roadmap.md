# Study Roadmap

## Focused 18-stage PDF path

The complete master book remains the umbrella reference. For a more finishable basics-to-advanced route, use the companion **Java SDE-2 Interview Preparation Series** in `dist/`. It contains 18 public stages: Number Systems, complexity, Java problem-solving foundations, bit manipulation, loops and indexing, arrays, strings, hashing, recursion and backtracking, linked lists, stacks and queues, binary search, trees, heaps, graphs, greedy algorithms, dynamic programming, and advanced Java engineering.

Start with `Java-SDE2-Interview-Preparation-Series-Index.pdf`. Stage 18 is deliberately packaged as Parts 18A through 18J so JVM execution, modern Java, libraries, concurrency, diagnostics, design and testing, final interview revision, Spring REST services, persistence, and distributed systems/system design remain individually printable. Parts 18H through 18J form the backend specialist track. Every focused PDF includes the complete roadmap plus previous/next navigation.

When rebuilding from basics, complete Stages 1 through 17 in order and then select the Stage 18 parts required by the role. When revising, enter at the first stage whose recognition signals or completion check expose a gap.

## Diagnostic baseline

Before choosing a schedule, answer these prompts aloud and write one code sample for each weak area:

1. Trace `javac` output from a class file through loading, verification, interpretation, and JIT compilation.
2. Distinguish heap, Java stack, metaspace, code cache, and native process memory.
3. Explain `equals` and `hashCode` as a joint contract, then predict a broken-key failure in `HashMap`.
4. Explain happens-before and show why `volatile int count++` is not an atomic counter.
5. Compare `ArrayList`, `LinkedList`, `ArrayDeque`, `HashMap`, `TreeMap`, and `ConcurrentHashMap` for a concrete workload.
6. Design cancellation for a task submitted to an executor.
7. Diagnose a service with rising old-generation occupancy and normal request volume.
8. Solve a medium array or graph problem while stating an invariant before coding.

Score each answer from zero to three:

- 0: cannot form a model;
- 1: recognizes terms but cannot explain mechanism;
- 2: gives a correct working explanation with minor gaps;
- 3: handles follow-ups, trade-offs, and production implications.

Prioritize zeros and ones. A balanced candidate is usually stronger than a candidate with one spectacular specialty and silent foundations.

## Twelve-week foundation plan

| Week | Core reading | Coding and evidence | Interview output |
|---|---|---|---|
| 1 | Chapters 1-3 | Compile, disassemble, and package a small program | Explain source-to-CPU execution in five minutes |
| 2 | Chapters 4-8 | Draw JVM memory and call-stack diagrams | Answer stack, heap, class-loader, and pass-by-value probes |
| 3 | Chapters 9-11 | Capture GC logs; write safe and unsafe publication examples | Explain reachability, collectors, JIT, and happens-before |
| 4 | Chapters 12-18 | Implement language edge-case exercises | Predict code output and defend specification claims |
| 5 | Chapters 19-24 | Build immutable values, generic APIs, and pattern switches | Compare records, classes, sealed types, and generics |
| 6 | Chapters 25-30 | Reimplement a small map and heap; benchmark only after reasoning | Select collections for five production workloads |
| 7 | Chapters 31-32 | Build stream and NIO pipelines with explicit resource handling | Explain laziness, collectors, back pressure boundaries, and I/O |
| 8 | Chapters 33-35 | Write cancellation, visibility, lock, and atomic examples | Solve race-condition and synchronization follow-ups |
| 9 | Chapters 36-38 | Build bounded executor and virtual-thread experiments | Design a concurrent component and failure strategy |
| 10 | Chapters 39-41 | Run JMH and JFR; analyze a synthetic memory leak | Present an evidence-first performance diagnosis |
| 11 | Chapters 42-48 | Two timed problems per day, rotating patterns | Complete two coding mocks |
| 12 | Chapters 49-54 | One low-level design and one backend scenario per day | Complete full mock loops and close weak topics |

## Daily practice block

A sustainable two-hour block is more valuable than an occasional eight-hour binge:

1. 25 minutes: recall yesterday's concepts without notes.
2. 35 minutes: read one focused section and annotate guarantees versus implementation.
3. 45 minutes: write, run, and test code.
4. 15 minutes: answer two questions aloud and record unclear phrases.

On coding-heavy days, use 20 minutes for problem framing, 35 minutes for implementation, 15 minutes for tests and complexity, and 10 minutes for a clean verbal recap.

## Spaced review cadence

Review a concept after one day, three days, seven days, and fourteen days. Each review should retrieve, not reread:

- draw the mechanism;
- state the contract;
- write the smallest example;
- answer one adversarial follow-up;
- update the evidence notebook.

If retrieval fails, shorten the next interval. If it succeeds cleanly twice, move the topic to weekly rotation.

## Mock loop design

A realistic SDE-2 loop should sample different evidence:

- 45 minutes: coding and complexity;
- 45 minutes: Java/JVM depth;
- 45 minutes: low-level design and concurrency;
- 45 minutes: backend design and operational reasoning;
- 30 to 45 minutes: behavioral examples with ownership and trade-offs.

After each mock, repair the first point where the answer lost precision before collecting more questions.

## Readiness gates

Do not use confidence as the only signal. Use observable gates:

- At least 80 percent of revision checklist items can be explained without notes.
- Medium coding problems are normally correct within 35 minutes, including tests and complexity.
- Collection and concurrency choices are justified by workload and correctness requirements.
- JVM answers label implementation details.
- Performance answers begin with measurement and preserve evidence.
- Design answers state invariants, failure modes, and observability before exotic optimizations.
- Behavioral stories show decisions, measurable impact, and learning; every answer should make reasoning inspectable and trustworthy.
