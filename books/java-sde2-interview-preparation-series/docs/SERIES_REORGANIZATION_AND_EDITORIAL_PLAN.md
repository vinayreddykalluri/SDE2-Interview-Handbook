# Series Reorganization and Editorial Enhancement Plan

Status: implemented; final release validation completed

Prepared: 2026-08-02

Scope: canonical Markdown, Java companions, focused PDFs, series index, web reader, downloads, and validation

## External contract benchmark

The depth audit is checked against authoritative behavior contracts, not the length of competing books:

- Java collection claims must remain consistent with the Java 21 API contracts for [`HashMap`](https://docs.oracle.com/en/java/javase/21/docs/api/java.base/java/util/HashMap.html) and [`PriorityQueue`](https://docs.oracle.com/en/java/javase/21/docs/api/java.base/java/util/PriorityQueue.html), including unspecified iteration order, conditional complexity, null policy, and best-effort fail-fast behavior.
- The DSA path must cover the core families represented in MIT's Design and Analysis of Algorithms curriculum—sorting, heaps, trees, graphs, shortest paths, greedy reasoning, and dynamic programming—while adding Java implementation contracts and interview communication.
- Spring transaction chapters must match the official distinction between logical and physical transactions, proxy-driven interception, thread-bound imperative contexts, rollback-only behavior, and the resource risk of `REQUIRES_NEW`.
- MySQL chapters must match the MySQL 8.4 InnoDB model: transactions, MVCC, clustered primary-key organization, row/record and range locking, and evidence-based query/index analysis.

These references define correctness boundaries. They do not replace first-principles explanations, executable examples, or interview practice.

## Outcome

The handbook will become four clearly separated, independently navigable book series:

1. Java Engineering
2. Data Structures and Algorithms
3. Frameworks, Data, and Messaging
4. System Design

The same series code and book number will appear in the source folder, PDF filename, cover, table of contents, web route, download link, roadmap, and validation manifest. Internal legacy IDs may remain temporarily for migration safety, but readers will never see conflicting public numbers.

Content will be revised around problem solving across **every series**, not only Number Systems. Important DSA chapters will derive the invariant, implement the relevant structure or algorithm, trace its internal state, test failure boundaries, and then compare the standard Java API where one exists. Java and backend chapters will expose the corresponding runtime, storage, framework, or operational flow instead of stopping at API usage. Every book will include answered interviewer-style scenarios and use a clear, conversational technical voice without repetitive branding or filler.

The goal is not to become the largest collection on the internet. The goal is to become the most dependable learning path in this repository: beginner-correct, technically exact, implementation-aware, solution-rich, and useful in a live SDE-2 interview.

## Pre-implementation audit snapshot

The following findings record the repository state that triggered this plan. They
are retained as the before-state; the implementation result is recorded at the
end of this document.

- 40 focused PDFs are declared.
- 42 PDFs are currently stored flat in `dist/`: 40 focused books, one series index, and one 616-page master reference.
- The 40 focused PDFs contain 2,725 pages.
- The manifest currently exposes only three segments: Java 9, DSA 17, and a combined backend/system-design segment 14.
- Java Fundamentals is publicly JAVA 01 but its filename still says DSA 03.
- Time and Space Complexity is publicly DSA 01 but its filename says DSA 02.
- Number Systems is publicly DSA 02 but its filename says DSA 01.
- Advanced Java and backend books retain legacy `ADV-18*` filenames.
- The web catalog is stale: SD06 is 52 pages and marked enhanced in the artifact manifest, but the site still shows a 10-page planned edition.
- Seven focused PDFs are only roadmap placeholders of about 10 pages: MySQL, Hibernate/JPA, MongoDB, Redis, Kafka/Spring Kafka, Spring Ecosystem Extensions, and Spring AI.
- SD06 Spring Data has more chapters, but its prose, code depth, and several behavioral claims still need a publication-quality audit.
- DSA 08-17 have stronger core prose than before, but most solution files are still short sketches rather than complete, runnable, explained solutions.
- Number Systems contains substantial first-principles work, but the manual-versus-library pattern is not applied consistently.
- The author page describes editorial titles more strongly than the reading experience expresses an identifiable human teaching voice.
- The earlier DSA 08-17 publication audit reports strong coverage, but its gate overweights topic presence, aggregate prompt counts, and the existence of compiling companion classes. In the canonical solution files, most answers remain prose outlines with no complete Java implementation.
- Across DSA 08-17, ten companion check classes total only 724 lines. They compile, but one small companion per book cannot by itself validate all important examples, exercise answers, edge cases, or stated outputs.
- A representative PDF review confirms the integration problem: some explanations and interview answers are concise checklist pages, while substantial Java is grouped into long continuation blocks away from the derivation and dry run. The code is valid but the reading experience is not yet consistently textbook-quality.
- Time and Space Complexity, Bit Manipulation, Loop Mastery, Arrays, and Strings contain much more inline Java than DSA 08-17, but explicit edge-case, failure-mode, and answered-interview coverage is uneven.
- Spring Data's current PDF demonstrates the opposite problem: clean layout, but some chapters are too sparse and make behavioral claims without enough runtime flow, SQL consequences, failure reproduction, or tests.

## External benchmark and evidence policy

The series will be compared with current primary sources and established public interview paths. This is a quality benchmark, not a license to copy questions or prose.

| Benchmark | What it demonstrates | Requirement adopted here |
|---|---|---|
| [HackerRank Interview Preparation Kit](https://www.hackerrank.com/interview/interview-preparation-kit) | Interview preparation repeatedly exercises arrays, hashing, sorting, strings, greedy, search, DP, stacks/queues, graphs, trees, linked lists, and recursion/backtracking | Preserve full DSA breadth and provide repeated, graduated application rather than one example per topic |
| [LeetCode Top Interview 150](https://leetcode.com/studyplan/top-interview-150/) | A serious plan uses a curated set, comprehensive topics, and detailed editorials over sustained practice | Give readers a bounded core problem set, complete explanations, review loops, and cumulative mocks |
| [Java Language Specification 21](https://docs.oracle.com/javase/specs/jls/se21/html/index.html) and [JVM Specification 21](https://docs.oracle.com/javase/specs/jvms/se21/html/index.html) | Java language behavior and VM behavior have different normative boundaries | Label language guarantees, JVM guarantees, and implementation-specific observations separately |
| [Java Collections Framework API](https://docs.oracle.com/en/java/javase/21/docs/api/java.base/java/util/package-summary.html) and [OpenJDK HashMap source](https://github.com/openjdk/jdk/blob/master/src/java.base/share/classes/java/util/HashMap.java) | API contracts and a particular JDK implementation are related but not interchangeable | Teach the contract first, a small educational implementation second, and selected OpenJDK mechanics only when they improve interview understanding |
| [Spring Framework reference](https://docs.spring.io/spring-framework/reference/index.html), [Spring Boot auto-configuration](https://docs.spring.io/spring-boot/reference/using/auto-configuration.html), and [Spring Data JPA reference](https://docs.spring.io/spring-data/jpa/reference/jpa.html) | Framework proficiency requires container, proxy, auto-configuration, persistence, and transaction behavior—not annotations alone | Trace request/container/repository flows, show generated consequences, reproduce failures, and test corrections |
| [MySQL InnoDB locking](https://dev.mysql.com/doc/refman/8.4/en/innodb-locking.html), [MongoDB transactions](https://www.mongodb.com/docs/manual/core/transactions/), [Redis data types](https://redis.io/docs/latest/develop/data-types/), and [Kafka documentation](https://kafka.apache.org/documentation/) | Backend interview answers depend on explicit consistency, locking, storage, and delivery contracts | Add internal state diagrams, failure matrices, operational trade-offs, and executable labs where practical |
| [AWS Well-Architected Framework](https://docs.aws.amazon.com/wellarchitected/latest/framework/welcome.html) and [Google SRE: Addressing Cascading Failures](https://sre.google/sre-book/addressing-cascading-failures/) | A system-design answer is incomplete without reliability, overload, observability, security, operations, and cost | Extend every major design case beyond components and happy paths |

External sources will be recorded per book in a compact source ledger. Claims about contracts, versions, internal implementation, performance, or failure behavior must be traceable to a primary source or a reproducible experiment. Interview questions will be original, representative scenarios synthesized from public themes; the project will not claim access to confidential or leaked company question banks.

### What “better than the internet” means here

The project will not make an unverifiable marketing claim that it is objectively the best resource online. It will instead publish evidence for these differentiators:

1. one prerequisite-correct Java-to-DSA-to-backend path;
2. first-principles and standard-library solutions shown together where useful;
3. internal state traces and implementation boundaries;
4. explicit edge cases, counterexamples, and failure recovery;
5. complete, compiling Java solutions adjacent to their reasoning;
6. interviewer questions with model answers and follow-up branches;
7. code, web, PDF, link, and visual validation for every release;
8. transparent per-book readiness scores and remaining gaps.

## Target series and order

### 01 - Java Engineering (`JAVA`)

1. JAVA 01 - Java Foundations for Problem Solving
2. JAVA 02 - Git and GitHub for Java Engineers
3. JAVA 03 - Maven and Gradle for Java Engineers
4. JAVA 04 - Language, OOP, and Modern Java
5. JAVA 05 - Collections, Streams, and I/O
6. JAVA 06 - JVM and Execution
7. JAVA 07 - Concurrency and the Java Memory Model
8. JAVA 08 - Performance, Diagnostics, and Garbage Collection
9. JAVA 09 - Java Interview Question Bank, Study Plan, and Reference

### 02 - Data Structures and Algorithms (`DSA`)

1. DSA 01 - Time and Space Complexity
2. DSA 02 - Number Systems and Math Foundations
3. DSA 03 - Number Systems Interview Workbook
4. DSA 04 - Bit Manipulation
5. DSA 05 - Loop Mastery and Index Calculations
6. DSA 06 - Arrays and Array Patterns
7. DSA 07 - Strings and String Patterns
8. DSA 08 - Hashing, Maps, Sets, and Prefix State
9. DSA 09 - Recursion and Backtracking
10. DSA 10 - Linked Lists
11. DSA 11 - Stacks, Queues, Deques, and Monotonic Patterns
12. DSA 12 - Binary Search
13. DSA 13 - Trees, BSTs, and Tries
14. DSA 14 - Heaps, Priority Queues, Selection, and Top-K
15. DSA 15 - Graphs
16. DSA 16 - Greedy Algorithms
17. DSA 17 - Dynamic Programming

### 03 - Frameworks, Data, and Messaging (`FW`)

1. FW 01 - MySQL for Java Backend Interviews
2. FW 02 - Hibernate and JPA
3. FW 03 - Spring Framework
4. FW 04 - Spring Boot
5. FW 05 - Spring Web and REST API Deep Dive
6. FW 06 - Spring Data
7. FW 07 - MongoDB
8. FW 08 - Redis
9. FW 09 - Persistence and Cache Consistency Deep Dive
10. FW 10 - Apache Kafka and Spring Kafka
11. FW 11 - Spring Ecosystem Extensions
12. FW 12 - Spring AI

`FW` is used instead of the older `SD` labels because these books teach technologies and integration contracts. MySQL, MongoDB, Redis, and Kafka are not falsely described as Spring frameworks; the full segment title makes the broader scope explicit.

### 04 - System Design (`SD`)

1. SD 01 - Backend Design, Testing, and Security Foundations
2. SD 02 - Distributed Systems and System Design

This short track will be shown honestly. Future system-design case-study books can be added here without mixing framework tutorials into the design series.

## Target repository layout

```text
books/java-sde2-interview-preparation-series/
|-- content/
|   |-- master/                         shared master-reference sources
|   `-- volumes/
|       |-- java/
|       |-- dsa/
|       |-- frameworks/
|       `-- system-design/
|-- dist/
|   |-- manifest.json
|   |-- 00-start-here/
|   |-- 01-java/
|   |-- 02-dsa/
|   |-- 03-frameworks/
|   `-- 04-system-design/
|-- publishing/
|-- scripts/
`-- reports/
```

`content/master/` remains a shared reference source during this migration. Moving its 61 shared files into focused-book directories would destabilize the existing 616-page master builder and encourage duplicated prose.

Example canonical PDF paths:

- `dist/01-java/Java-SDE2-JAVA-01-Java-Foundations.pdf`
- `dist/02-dsa/Java-SDE2-DSA-01-Time-and-Space-Complexity.pdf`
- `dist/02-dsa/Java-SDE2-DSA-02-Number-Systems-and-Math-Foundations.pdf`
- `dist/03-frameworks/Java-SDE2-FW-03-Spring-Framework.pdf`
- `dist/04-system-design/Java-SDE2-SD-01-Backend-Design-Foundations.pdf`

## Identity and navigation rules

- `publishing/series.json` is the only authority for segment, order, public code, source path, artifact path, and publication status.
- Public numbers are derived from segment position, not from legacy internal IDs.
- Each focused PDF has one canonical repository path.
- GitHub release assets may remain flat because release assets do not support folders; repository paths and release filenames are modeled separately.
- Historical release tags remain immutable.
- Old web routes receive generated redirects.
- A migration table records every old filename, new filename, old route, and new route.
- Previous and next links stay within a series.
- Cross-series prerequisites are explicit links, not accidental global ordering.
- Planned books appear in a separate "Coming soon" state and are never described as complete publications.

## Series-wide first-principles, internals, library, and edge-case policy

The earlier plan applied the manual-versus-library rule too narrowly to Number Systems. It now applies wherever an interview tests the underlying idea.

> Derive and implement the concept when the concept is under examination. Then show the production Java API when one exists and compare the contracts. Do not hide the algorithm behind the API, and do not pretend that handwritten code is automatically superior in production.

### Three levels of implementation explanation

1. **Contract** - what callers may rely on, including input, output, mutation, ordering, equality, error, and complexity assumptions.
2. **Educational implementation** - the smallest correct from-scratch implementation that reveals the invariant and state changes.
3. **Platform implementation** - the relevant JDK, framework, database, broker, or runtime behavior, clearly labeled when it is version- or vendor-specific.

“Low level” does not mean pasting thousands of lines from OpenJDK or a framework. It means showing enough structure and state to explain why the public behavior, performance, and failures occur.

### Required package for an important algorithm or structure

Every core DSA algorithm, data structure, or reusable pattern will include:

1. problem contract, constraints, and representative examples;
2. beginner mental model before compressed terminology;
3. brute-force or direct baseline when it teaches the bottleneck;
4. invariant or state definition;
5. first-principles Java implementation without an API that performs the tested idea;
6. state diagram or dry run tied to actual variable names;
7. termination and correctness reasoning;
8. time and auxiliary-space derivation;
9. edge-case and failure matrix;
10. complete executable tests, including empty, singleton, duplicate, extreme, invalid, and adversarial cases where relevant;
11. standard Java implementation or API comparison when one genuinely exists;
12. interviewer question, model candidate answer, and two or more follow-up branches;
13. a broken version to debug;
14. separated practice and complete reasoned solution.

Small syntax examples do not need this full package. The gate applies to concepts an interviewer can reasonably ask a candidate to derive, implement, debug, or defend.

### Number Systems remains a targeted repair, not the whole program

Number Systems already has the strongest first-principles base. It receives only focused repairs: manual parsing and base conversion beside `Integer.parseInt`/`BigInteger`, Euclidean GCD and safe LCM beside library alternatives, bit-length and overflow guards beside Java helpers, modulo normalization, extended Euclid, exact factorial construction, cross-tests, and corrected workbook labels. The broader implementation pass then moves immediately to the rest of DSA in study order.

## Re-audited DSA content plan

The current DSA set has broad topic coverage, but the books are not uniformly at the requested standard. Current DSA 01-07 PDFs are generally 80-138 pages and contain much richer inline Java; current DSA 09-17 are generally 41-50 pages with only 7-12 Java blocks each. Page count is not the verdict. The decisive evidence is that most DSA 09-17 solution files are only 18-19 lines and provide prose outlines instead of complete, tested answers. DSA 10-13 also contain no explicitly labeled dry runs.

| Target book | Current judgment | Required first-principles/internal implementation | Required edge, trace, and interview upgrade | Priority |
|---|---|---|---|---|
| DSA 01 Complexity | Strong sequence; inadequate visual derivation and incomplete coding answers | exact operation counting, recurrence tree, recursion-stack model, amortized dynamic-array growth, qualified Java collection cost models | Master-Theorem boundaries, nested aggregate work, overflow in counts, input-shape questions, six live rounds | P0 prerequisite |
| DSA 02 Number Systems | Strongest technical base; dual format is inconsistent | targeted manual checked arithmetic, modulo normalization, GCD/LCM extremes, factorial digit construction, modular inverse | cross-test manual/JDK domains; preserve overflow, sign, and minimum-value cases | Targeted repair |
| DSA 03 Number Systems Workbook | Strong implementation inventory; oversized chapters and uneven interview flow | apply the dual format consistently to all cataloged programs | split navigable units; add live interviewer conversations instead of more static prompts | Targeted repair |
| DSA 04 Bit Manipulation | Strong manual mechanics and code; Q&A is light | signed representation, shift behavior, masks, parity, popcount, submasks, prefix XOR; compare `Integer`, `Long`, and `BitSet` only after derivation | negative values, shift-distance masking, overflow, sign extension; six live rounds | P1 |
| DSA 05 Loop Mastery | Strong fundamentals; more state evidence needed | index algebra, cyclic normalization, matrix coordinates, two-pointer/window invariants, termination proofs | off-by-one, non-progress, `int` overflow, jagged grids; pointer/window trace tables and six live rounds | P1 |
| DSA 06 Arrays | Strong pattern coverage; missing core low-level structures and sorting implementations | array/reference memory model, alias/copy, minimal dynamic array, insertion sort, stable merge sort, three-way quicksort, counting sort, iterative quickselect | empty/single/duplicates/extremes, stability, comparator safety, partition boundaries, differential tests | P0 |
| DSA 07 Strings | Strong breadth; internals, Unicode contracts, and interview dialogue need strengthening | `char`/code point/grapheme distinction, immutability and builder state, manual parsing, KMP prefix table, rolling state, trie comparison | null/empty/blank, surrogate pairs, locale, normalization, delimiter and overflow failures; parsing/window/KMP/Unicode rounds | P1 |
| DSA 08 Hashing | Good pattern prose; no educational hash table and solutions are outlines | small separate-chaining map, hash spreading, bucket selection, insert/replace, collision chain, load threshold, resize/rehash | mutable keys, bad distribution, null/equality contracts, overflow in counts, adversarial input; collision and resize diagrams | P0 |
| DSA 09 Recursion | Good contracts; weak call-frame visualization and complete answers | explicit call frames, recursive-to-iterative conversion, choice/apply/recurse/undo, pruning and memo state | missing base case, state leakage, duplicate generation, stack overflow, exponential output; unwind and search-tree diagrams | P1 |
| DSA 10 Linked Lists | Good pointer reasoning; major mutations lack diagrams and full answers | node implementation, dummy/sentinel use, reverse, merge, cycle entry, intersection, random-pointer clone, LRU detach/attach | null/single/two-node/cycle/alias cases; before/after pointer diagrams for every mutation | P1 |
| DSA 11 Stacks/Queues/Deques | Good API and monotonic coverage; no full array/ring implementations | custom array stack and circular queue/deque with growth, wraparound, and invariant checks; compare `ArrayDeque` | overflow/underflow, wrapped head/tail, stale slots, duplicate monotonic values; state traces | P1 |
| DSA 12 Binary Search | Strong invariants; trace coverage is missing | closed and half-open templates, lower/upper bound, first-true/last-false, rotated search, answer-space search | midpoint overflow, non-progress, duplicates, impossible answer, long domains; trace every interval convention | P1 |
| DSA 13 Trees/BSTs/Tries | Good breadth; missing mutation diagrams and promised range structures | node/traversals, BST operations, trie, AVL rotations, Fenwick tree, segment tree; locate dynamic range-query ownership here | null/skew/depth/duplicates, deletion cases, Unicode trie keys, index boundaries; state diagrams and invariant tests | P0 |
| DSA 14 Heaps/Top-K | Sift concepts exist; implementation relies too quickly on `PriorityQueue` | resizable binary heap with `offer`/`peek`/`poll`, heapify and comparator policy; randomized iterative quickselect | empty heap, duplicate priorities, comparator overflow, adversarial partition, iteration-not-sorted trap; differential tests | P0 |
| DSA 15 Graphs | Strong core traversal; advanced algorithms are claimed but not implemented | representations, BFS/DFS, topo, union-find, DAG shortest path, Bellman-Ford, Floyd-Warshall, Prim, SCC, bridges/articulation | disconnected/cycle/negative edge/negative cycle/infinity overflow/parallel edge cases; visual frontier and relaxation traces | P0 |
| DSA 16 Greedy | Good proof vocabulary; insufficient complete counterexample practice | interval/scheduling/partition decisions, exchange and stays-ahead proofs, canonical counterexample construction | tie policy, overflow, input mutation, locally plausible failures; complete job-sequencing answers and proof rounds | P1 |
| DSA 17 Dynamic Programming | Strong progression; advanced families are mainly named | recursion to memo to tabulation to compression, reconstruction, tree independent set, selected bitmask DP, one bounded digit-DP case | invalid sentinels, overflow/modulo, dependency-order errors, memory limits; more state-table fills and reconstruction tests | P1 |

Sorting will be owned by DSA 06 rather than buried only in a Java comparator chapter. Fenwick and segment trees will be owned by DSA 13 because DSA 06 currently promises later coverage that does not exist. Graph and DP books will implement only the advanced families they claim to teach; recognition-only topics must be labeled honestly.

## Re-audited Java content plan

The Java series has enough pages. Its main problems are prerequisite order, duplicated master chapters, and inconsistent executable validation.

| Target book | Current judgment | Required implementation or evidence | Required interview and learning repair | Priority |
|---|---|---|---|---|
| JAVA 01 Fundamentals | 199 pages and 70 passing examples, but advanced concepts appear before prerequisites and four chapters duplicate later books | split the monolithic companion into chapter-scoped programs; retain focused arrays, strings, objects, equality, exceptions, and basic collections | remove advanced exception/collections/sorting/playbook chapters; replace with beginner introductions and cross-links; complete important coding/debugging solutions | P0 |
| JAVA 02 Git/GitHub | Strong first-principles model, 14 rounds, 7 validated labs | add index-stage/object-plumbing and merge-base/recovery labs | replace grouped workbook sketches with full command reasoning and recovery answers | P3 polish |
| JAVA 03 Maven/Gradle | Strong side-by-side model, 18 rounds, both labs pass | capture real effective POM, dependency tree/insight, task graph, cache, daemon, and multi-module evidence | expand concise chapters and complete scenario solutions | P2 |
| JAVA 04 Language/OOP/Modern Java | Strong mechanics, 45 model questions; overlaps Fundamentals and lacks dedicated validation | bytecode/desugaring and dispatch labs; chapter-scoped companions | remove duplication, label Java baselines, add fully answered code-output/debugging probes | P1 |
| JAVA 05 Collections/Streams/I/O | Excellent conceptual internals but no executable educational structures | `MiniDynamicArray`, `MiniLinkedList`, `MiniHashMap`, `BinaryHeap`, comparator/sorting harness, framed `ByteBuffer` decoder | pair manual structures with JDK contracts, edge matrices, and complete solutions | P0 |
| JAVA 06 JVM/Execution | Excellent specification-versus-HotSpot distinction; labs are missing | reproducible `javap`, class initialization/loading, loader identity, allocation, GC, and stack evidence | incident questions with commands, evidence interpretation, and model answers | P1 |
| JAVA 07 Concurrency/JMM | Strong theory; no deterministic companion or stress lab | lost-update, visibility, bounded buffer, double-checked locking, CAS state, cancellation, starvation and deadlock fixtures with timeouts | answer the exercise bank; add happens-before diagrams and failure-driven interview rounds | P0/P1 |
| JAVA 08 Performance/Diagnostics | Sound method but only 48 pages and no runnable investigation lab | JMH project, JFR recording, `jcmd`, thread dump, histogram, heap and GC-log cases with sample evidence | diagnosis rounds must move from symptom to hypothesis, measurement, correction, and verification | P0/P1 |
| JAVA 09 Interview Readiness | Useful 54-question bank, but only 13 selected worked solutions and unanswered follow-ups | become the nonduplicated cross-volume index and mock harness | add 30-second and two-minute answers, answered follow-ups, debugging/coding rounds, and scoring rubrics | P1 |

Fundamentals currently introduces `var`, boxing, `BigDecimal`, enums, interfaces, lambdas, `Duration`, bridge methods, generic varargs, covariance, graphemes, encoders, and text blocks too early. The rewrite will preserve those topics in the series but move each one after its prerequisite. The legacy ADV letter labels will be replaced by the visible JAVA 04-09 sequence.

## Re-audited Framework, Data, Messaging, and System Design plan

Seven roadmap books contain only about 220-244 words each inside a 10-page publication shell. They are placeholders, not unfinished full books, and must remain visibly planned until written. Spring Data has 17 short chapters and a 52-page PDF, but it is not publication-safe: several claims are misleading, its companion contains behavioral defects, and its current manifest flags allow the validator to skip it.

| Target book | Current judgment | Required low-level/native foundation | Required interview, failure, and lab upgrade | Priority |
|---|---|---|---|---|
| FW 01 MySQL | Roadmap placeholder | SQL before ORM; InnoDB B+ trees, clustered/secondary indexes, buffer pool, redo/undo, MVCC, locking; JDBC prepared statements, transactions and batching | query/index rounds, transaction schedules, deadlock, migration, replication-lag, backup/restore and observability labs | P0 backend spine |
| FW 02 Hibernate/JPA | Roadmap placeholder | JDBC/SQL consequence before JPA contract; persistence context, state transitions, dirty checking, flush order, proxies, fetch, batching, locking, cache | predict-the-SQL, N+1, merge/detach, stale context, equality, bulk DML, optimistic conflict scenarios with real tests | P0 backend spine |
| FW 03 Spring Framework | Strong, 122 pages and six real fixture tests; still lacks full application coverage | plain object graph before IoC; decorator before AOP; transaction boundary before annotation; proxy and bean lifecycle traces | add caching, rollback-only, self-invocation, `REQUIRES_NEW` pool pressure, async context, MVC/error and scheduler labs | P1 |
| FW 04 Spring Boot | Strong, 113 pages and six fixture tests; lacks end-to-end production application | explicit Spring configuration and property-source trace before auto-configuration | executable order service, HTTP/validation/error contracts, Actuator security, migrations, Testcontainers, shutdown and packaging | P1 |
| FW 05 Spring Web/REST | Strong capstone material but duplicated and no real Spring app | filter to dispatcher to handler to serialization/error flow; explicit idempotency and concurrency state | re-scope as the real integrated REST capstone with concurrent duplicate requests, ETags, security and failure injection | P1 |
| FW 06 Spring Data | Not publication-ready; 5,097 words, one Java fence, no SQL, no real Spring Data fixture | `EntityManager`/JDBC before repository proxy; query derivation, paging, scrolling, specifications, projections, graphs, locking and transaction flow | correct `getBy`, `countBy`, `REQUIRES_NEW`, flush, lock, comparator, paging and retry claims; add query-count and concurrency tests | P0 blocker |
| FW 07 MongoDB | Roadmap placeholder | BSON/document model, indexes/aggregation, replica and shard flow, read/write concern, sessions/transactions; shell and Java driver before Spring Data | schema/access-pattern, unbounded array, multikey index, shard hotspot, election and retry scenarios | P1 |
| FW 08 Redis | Roadmap placeholder | native commands/data structures, TTL, memory/eviction, persistence, replication, cluster slots, Streams PEL; Lua/client before Spring abstraction | stampede/hot-key, stale cache, failover/data loss, stream recovery, fencing and lock failure timelines | P1 |
| FW 09 Persistence/Cache Consistency | Strong theory; no database/ORM/cache fixture | cache-aside/write-through, versioned invalidation, outbox/CDC and crash windows implemented against real stores | pool exhaustion, stale reads, outbox recovery, stampede and cache race tests; engine-label SQL behavior | P2 capstone |
| FW 10 Kafka/Spring Kafka | Roadmap placeholder | native record/log/segment/partition/offset/group/poll/commit flow before listener containers | rebalance, lag, poison record, retry/DLT ordering, ISR, idempotence, transaction, outbox/inbox and crash matrix | P1 |
| FW 11 Spring Extensions | Roadmap scope is too broad | split Security, reactive, batch, integration and cloud runtime models, or label the book a survey | make Spring Security its own priority volume; do not claim mastery from a catalog chapter | Scope redesign |
| FW 12 Spring AI | Roadmap placeholder and fastest-changing API surface | deterministic model fake before `ChatModel`/`ChatClient`; advisors, structured output, tools, RAG, vector store, MCP | prompt/tool authorization, schema repair, tenant leakage, retrieval quality, evaluation, cost and provider-failure tests | Last framework batch |
| SD 01 Backend Design/Testing/Security | Useful 50-page reference; weak practical capstone | implement several complete LLD cases with state, concurrency, persistence boundary, tests and negative security cases | interviewer-driven change requests and model answers, not only pattern catalogues | P2 |
| SD 02 Distributed Systems/System Design | Strong theory, 72 pages; practical implementations and case breadth are limited | consistent/rendezvous hashing, fencing lease, circuit breaker, retry budget, offset tracking, saga and SLO calculations | feed/chat/storage/payment/rate-limiter cases; overload, regional partition, failure injection, observability and cost rounds | P1/P2 |

Spring Data will be returned to draft until its critical claims and companion defects are fixed and its code is actually compiled by the publication validator. Manifest promises such as “20 solved scenarios” will be verified against real, fully answered scenarios rather than retained as marketing text.

Known Spring Data corrections are already specific enough to block publication:

- `getBy...` does not by itself define one universal missing-result exception contract;
- `countBy...` should not be advertised as a cheap existence or health-check path;
- `REQUIRES_NEW` creates a separate transaction boundary, not an automatic retry mechanism;
- flushing before slow remote I/O must not encourage holding a database transaction across that call;
- lock escalation must not be generalized across vendors;
- the companion's composed comparator reverses the whole ordering and validates the wrong result;
- one model mixes cursor and offset pagination state;
- the retry classifier marks non-stale failures as retrying;
- the store-selection example is too categorical and incorrectly compresses MongoDB transaction capability;
- current `series_native` metadata permits the validator to skip the companion entirely.

## Publication scorecard

Every book will receive a transparent 100-point audit before it may be labeled enhanced or complete.

| Dimension | Weight | Passing evidence |
|---|---:|---|
| Prerequisite flow and beginner clarity | 10 | no concept used materially before explanation; explicit route from basics to SDE-2 |
| Technical accuracy and source boundaries | 10 | no critical defects; language/runtime/vendor claims qualified and sourced |
| First-principles implementation or native evidence | 15 | manual implementation for tested concepts, or reproducible lower-level lab for tools/frameworks |
| Internal state, dry runs, and diagrams | 10 | state changes are visible and tied to code/commands |
| Worked examples and complete solutions | 15 | core tasks have adjacent, runnable, reasoned answers |
| Edge cases, failures, and recovery | 10 | boundary/adversarial matrix plus broken and corrected cases |
| Interview questions and answered follow-ups | 10 | authentic dialogue, model answers, pressure branches and rubric |
| Practice design and solution mapping | 10 | graduated exercises map to solution and validation IDs |
| Executable validation | 5 | complete examples compile/run; outputs and intentional failures are isolated |
| PDF/web/navigation quality | 5 | same order and status; readable, linked, unclipped and downloadable |

A publication edition requires at least 85/100, no critical accuracy defect, no missing core prerequisite, no unvalidated central implementation, and no dimension below half its weight. Roadmap previews are exempt only because they remain plainly labeled previews.

## Series-wide editorial standard

Every substantial chapter will follow this learning arc:

1. **Why you are learning this** - one realistic interview or engineering situation
2. **Plain-language mental model** - daily language before terminology
3. **Mechanics and theory** - precise rules, invariants, and constraints
4. **Visual or dry run** - state changes that can be followed line by line
5. **Baseline solution** - the natural first attempt
6. **Bottleneck** - why the baseline is slow, unsafe, or incomplete
7. **Improved solution** - the reasoning step that changes the design
8. **Optimal interview solution** - complete compiling Java where appropriate
9. **Correctness argument** - why it works, not only what it prints
10. **Complexity** - derived from operations, not asserted from memory
11. **Edge cases and failure modes** - realistic tests and broken variants
12. **Interview conversation** - a real question, candidate answer, and follow-up
13. **Practice ladder** - Foundation, Interview Core, and SDE-2 Follow-up
14. **Solutions** - separated from the exercise and explained fully
15. **Chapter takeaway** - the reusable idea to remember

Low-value syntax topics remain shorter. More space goes to reasoning, state transitions, solution evolution, correctness, and trade-offs.

## Interview question-and-answer standard

The existing books frequently include questions, but a question count is not the same as interview preparation. A prompt qualifies as “answered” only when the reader receives the reasoning and not merely a hint or solution headline.

Every full book will contain at least:

- two Foundation/SDE-1 live rounds;
- two core-pattern or core-mechanics rounds;
- one SDE-2 escalation round;
- one debugging, incident, or incorrect-assumption round;
- 12-20 rapid interviewer questions with model answers;
- one cumulative mock and evaluation rubric.

Larger foundational books should exceed these minima only where the questions add distinct reasoning. Planned survey books do not receive artificial filler just to hit a number.

A live DSA round uses:

```text
Interviewer prompt
-> candidate clarifying questions
-> direct baseline
-> constraint pressure
-> derived invariant or state
-> complete Java implementation
-> dry run
-> correctness argument
-> edge tests
-> complexity
-> follow-up questions with model answers
```

A Java, framework, data, or system-design round uses:

```text
Interviewer prompt
-> 30-second opening answer
-> two-minute model answer
-> contract or runtime/data-flow diagram
-> code, command, query, or architecture evidence
-> failure or concurrency pressure
-> corrected design
-> observability and validation
-> follow-up questions with model answers
-> evaluation rubric
```

Each book must include at least one plausible but incorrect candidate answer and explain exactly why it fails. Questions will be original and representative; no book will describe synthesized prompts as leaked, proprietary, or guaranteed company questions.

## Author voice

The author presence will be human and useful, not title-heavy.

- Replace repeated institutional wording with one simple line: `Written by Vinay Reddy Kalluri`.
- Add a concise first-person preface explaining why the series exists and how Vinay recommends studying it.
- Add one unique "A note from Vinay" passage near the start of each book. It must explain a genuine learning difficulty or interview lesson for that topic, not repeat a template.
- Use natural transitions such as "Let us trace the state before optimizing it" and "Here is where candidates usually lose the invariant."
- Use first person sparingly. Technical evidence remains the center of the book.
- Retain the author bio, LinkedIn, GitHub, and contributor credits on the dedicated author page.
- Remove prominent repetitions of Editor-in-Chief and Chief Auditor from reader-facing pages while retaining governance roles in repository governance documents.

## Content ownership and duplication

Each deep concept will have one canonical owner.

- Fundamentals gives beginner context and the APIs needed for early DSA.
- Advanced Java owns internals and deep language behavior.
- DSA books own problem-solving patterns and algorithmic proofs.
- Framework books own technology usage and integration behavior.
- System Design owns architecture, trade-offs, capacity, reliability, and case studies.

When another book needs the idea, it receives a short, context-specific recap and a cross-reference. Entire advanced master chapters will no longer be copied into Fundamentals or DSA PDFs merely to increase depth.

Known duplicate areas to untangle include exceptions, collections, comparators, equality and hashing, ArrayList/list trade-offs, and the general interview playbook.

## Problem-solving and solution upgrade

DSA solutions must progress through:

```text
Problem contract -> examples -> baseline -> bottleneck -> invariant ->
complete Java solution -> dry run -> proof -> complexity -> edge tests -> follow-up
```

Current short solution sketches will not be counted as complete answers. Important exercises need runnable Java and reasoning. Variations can share a well-explained pattern instead of repeating near-identical code.

Every exercise that is labeled Interview Core or SDE-2 Follow-up will map to:

```text
exercise ID -> concept/pattern -> solution section -> Java method or lab ->
test/assertion -> documented result -> validation status
```

Optimized DSA implementations will be differential-tested against a simple baseline where practical. Data structures will expose invariant checks in tests. Overflow, mutation, equality, ordering, recursion depth, and invalid-input policy will be explicit rather than inferred.

The content pass follows reader order, while small independent book batches may be implemented in parallel:

1. JAVA 01 Fundamentals and DSA 01 Complexity
2. DSA 02-07 numeric, bit, loop, array, and string foundations
3. DSA 08-12 hashing, recursion, lists, ordering structures, and binary search
4. DSA 13-17 trees/range structures, heaps, graphs, greedy, and DP
5. JAVA 04-08 internals, collections, JVM, concurrency, and diagnostics
6. framework/data persistence spine and then messaging
7. System Design case-study expansion

This order prevents a user from meeting a custom heap, proxy, transaction, or distributed consistency model before the Java and algorithmic ideas needed to understand it.

## Framework and system-design quality gates

- “Manual plus library” means native/lower-level behavior before abstraction: SQL/JDBC before JPA, JPA/`EntityManager` before Spring Data, native Redis/Kafka/Mongo clients before their Spring integrations, a plain object graph before Spring IoC, and plain Spring configuration before Boot auto-configuration.
- Every important backend chapter includes a runtime or data-flow diagram, a happy-path trace, a failure/concurrency timeline, observability evidence, and an executable lab or reproducible command/query sequence.
- Spring Data returns to draft until its repository-return semantics, existence/count advice, transaction propagation framing, flush guidance, vendor-lock claims, comparator, pagination, retry classifier, and store-selection model are corrected.
- The validator must compile and run every non-planned companion; a `series_native: false` flag cannot silently exempt a published book.
- Keep the seven roadmap-only books under “Coming soon” until their chapter sets, examples, exercises, validation, and PDF QA pass.
- Consolidate any completed MySQL or Hibernate work from other branches before moving folders, so earlier authored material is not lost.
- Expand System Design as its own case-study and reliability program rather than padding it with framework books.

## Implementation phases

### Phase 1 - Stabilize current truth

- Reconcile `series.json`, `dist/manifest.json`, and the stale web catalog.
- Normalize publication statuses.
- Locate and consolidate any unmerged MySQL and Hibernate work.
- Record the current path and hash of every PDF before migration.

Exit criterion: source manifest, artifact manifest, and website report the same 40 books, statuses, page counts, and checksums.

### Phase 2 - Make builders path-aware

- Add four segment directories to `series.json`.
- Add canonical artifact paths and legacy-name metadata.
- Update PDF builders, validators, render QA, catalog generation, web reader generation, deployment checks, and download URL generation.
- Remove hard-coded assumptions about exactly three segments or flat `dist/*.pdf` discovery.

Exit criterion: validation passes with the old files still in place and a dry-run reports the complete new mapping.

### Phase 3 - Move sources and PDFs

- Move native volume folders under `java/`, `dsa/`, `frameworks/`, or `system-design/` using Git-aware renames.
- Update hard-coded diagram, asset, validation, report, and documentation paths.
- Split the Number Systems core and workbook sources into clearly owned folders while sharing only intentional support assets.
- Move and rename PDFs into the four ordered `dist/` folders.
- Move index and master reference artifacts into `00-start-here/`.
- Add web redirects and a legacy filename map.

Exit criterion: no canonical content or PDF remains in the old flat location, no volume is assigned twice, and no link points to an old mutable branch path.

### Phase 4 - Rebuild and align every surface

- Rebuild all 40 focused PDFs and the series index.
- Rebuild the master only if its roadmap or author front matter changes.
- Regenerate checksums and page counts.
- Regenerate the web catalog and complete Markdown reader.
- Update homepage, book discovery, downloads, README, roadmap, and contribution docs to four series.

Exit criterion: web, source folders, PDF covers, filenames, and navigation display identical codes and order.

### Phase 5 - Establish the honest content baseline

- Replace broad “strong” labels with the 100-point scorecard above.
- Mark the seven 10-page roadmap books as previews on the website, covers, downloads, manifest, README, and index.
- Return Spring Data to draft and record every critical correction.
- Add exercise-to-solution-to-test mapping files.
- Unify the existing editorial specification so its repeated headings guide authors without forcing identical prose.

Exit criterion: every book has an evidence-backed readiness score, owner, gap list, and publication state; no page count or aggregate prompt count is treated as proof of depth.

### Phase 6 - Repair the prerequisite spine

- Rewrite JAVA 01 in true prerequisite order and remove its copied advanced chapters.
- Split and validate its 70 examples by chapter.
- Upgrade DSA 01 Complexity with recurrence/amortized diagrams, complete answers, edge cases, and live rounds.
- Apply the focused Number Systems repairs without rewriting already strong material.
- Upgrade Bit Manipulation and Loop Mastery interview conversations and state traces.
- Add array internals, manual sorting, quickselect, and dynamic-array teaching to DSA 06.
- Add stronger string internals, Unicode contracts, pattern-matching traces, and live rounds to DSA 07.
- Apply author voice while each book is open rather than running a mechanical global rewrite later.

Exit criterion: a beginner can complete JAVA 01 and DSA 01-07 without prerequisite jumps, and every central implementation is adjacent to explanation, edge tests, and answered follow-ups.

### Phase 7 - Complete the DSA structures and strategy books

- Add the educational hash table to DSA 08.
- Add call-frame/backtracking diagrams and complete solutions to DSA 09.
- Add pointer-state diagrams and full mutation solutions to DSA 10.
- Add custom array stack/circular deque and monotonic traces to DSA 11.
- Add interval-convention and answer-search traces to DSA 12.
- Add AVL rotations, Fenwick tree, segment tree, and mutation diagrams to DSA 13.
- Add the custom binary heap and iterative quickselect to DSA 14.
- Implement the advanced graph algorithms already claimed by DSA 15.
- Complete Greedy proof/counterexample answers and selected advanced DP/reconstruction implementations.
- Add at least six live rounds and 12-20 rapid answered questions to each book, scaled down only where scope genuinely warrants it.

Exit criterion: every important Interview Core/SDE-2 exercise has complete Java, reasoning, tests, and validation; no claimed core algorithm exists only as prose.

### Phase 8 - Add executable depth to Java Engineering

- Add manual structures and paired JDK usage to JAVA 05.
- Add bytecode/desugaring, class-loading and initialization evidence to JAVA 04/JAVA 06.
- Add deterministic concurrency fixtures and failure diagrams to JAVA 07.
- Add a runnable JMH/JFR/`jcmd`/thread/heap/GC investigation laboratory to JAVA 08.
- Make JAVA 09 the nonduplicated readiness index with complete short/long answers and mock rubrics.
- Improve Git and build-tool books only through targeted missing labs and fuller solutions.

Exit criterion: every important advanced Java claim is supported by a compiled example, reproducible lab, specification reference, or explicitly labeled implementation observation.

### Phase 9 - Build the persistence spine

- Publish MySQL from SQL fundamentals through InnoDB internals and JDBC labs.
- Publish Hibernate/JPA against the same schema, showing SQL and persistence-context consequences.
- Rewrite Spring Data after those prerequisites and fix all known semantic and validation defects.
- Add one cross-layer use case shown as JDBC, JPA/Hibernate, and Spring Data.

Exit criterion: readers can explain what each abstraction adds and hides; real query, transaction, locking, pagination, and failure behavior is exercised and observed.

### Phase 10 - Complete frameworks, messaging, and design

- Expand Spring Framework and Boot labs, then re-scope Spring Web/REST as the integrated production capstone.
- Build MongoDB, Redis, and Kafka/Spring Kafka with native clients before Spring integrations.
- Expand the persistence/cache consistency capstone with real failure windows.
- Split Spring Security from the broad Spring Extensions survey or label the retained volume honestly as a survey.
- Build Spring AI last because its API surface changes fastest.
- Expand SD 01 with complete LLD cases and SD 02 with low-level reliability mechanisms and broader system-design cases.

Exit criterion: every published framework or design book traces runtime/data flow, reproduces failures, shows operational evidence, and answers realistic interviewer pressure questions.

### Phase 11 - Publication gate

- Compile all Java companions and runnable examples, including sources previously skipped as non-native.
- Run unit, snippet, lab, web, deployment, path, link, manifest, source-coverage, and repository-layout validation.
- Rebuild only the affected PDFs during each content batch; perform one full-series rebuild at the release candidate.
- Recursively inspect all PDFs for page count, metadata, bookmarks, clipping, broken tables, blank pages, missing images, and excessively detached code appendices.
- Render and visually inspect every cover and contents page, all changed pages, and representative unchanged pages from every volume.
- Publish updated scorecards, source ledgers, validation reports, README, contribution targets, and legacy mapping.

Exit criterion: zero unexplained compilation failures, output mismatches, broken links, stale catalog entries, duplicate assignments, clipped pages, unvalidated central examples, or planned books labeled complete.

## Commit strategy

Use separate reviewable commits:

1. Catalog reconciliation
2. Four-series manifest and path-aware tooling
3. Source-folder migration
4. PDF rebuild, manifest, and web synchronization
5. Content scorecards, status correction, and validation mapping
6. JAVA 01 and DSA 01 prerequisite spine
7. DSA upgrades by one book or a small dependency-safe group
8. Advanced Java executable-lab upgrades
9. MySQL, Hibernate/JPA, and Spring Data persistence spine
10. Framework, messaging, and System Design upgrades by book

Structural migration and broad prose changes must not be mixed into one commit.

## Definition of done

- Four source folders and four PDF folders exist and are canonical.
- All 40 focused books are assigned exactly once.
- Public codes and filenames follow series order.
- Website and PDFs expose the same four series and book order.
- Old web routes resolve to the new locations.
- Every important DSA concept teaches the invariant and first-principles implementation before comparing a library API where one exists.
- Java and backend books expose the relevant runtime/data/storage flow through reproducible evidence rather than annotation or API lists alone.
- Chapter voice is conversational, precise, and recognizably authored.
- Fundamentals stay beginner-first; advanced details appear only after prerequisites.
- Problem-solving chapters contain complete solutions, not answer sketches.
- Every full book includes realistic interviewer dialogues, rapid questions with model answers, answered follow-ups, and a cumulative mock.
- Every central example or lab maps to an executable validation result.
- Each published book scores at least 85/100 with no critical accuracy defect.
- Placeholder books remain clearly separated until completed.
- Every changed PDF passes automated and visual QA.

## Implementation result

Completed on 2026-08-02.

- Canonical sources now live under exactly four shelves: `java`, `dsa`,
  `frameworks`, and `system-design`.
- All 40 focused books have one canonical public code, ordered source workspace,
  nested PDF artifact, web route, and download target.
- The release contains 403 declared source documents (396 unique Markdown files),
  40 publication editions, and no placeholder-status focused books.
- The focused library contains 3,371 pages. The 18-page index and 616-page master
  bring the complete 42-PDF library to 4,005 pages.
- Beginner-first sequencing was restored in Java Fundamentals and the early DSA
  path. Important DSA, Java, persistence, framework, messaging, and system-design
  books now include lower-level mechanisms, boundary cases, complete solutions,
  and answered interviewer follow-ups appropriate to their scope.
- First-principles and standard-library approaches are paired where the comparison
  teaches an interview-relevant trade-off; the library is not presented as a
  substitute for understanding the underlying algorithm or data structure.
- Forty unique `A note from Vinay` passages give each book topic-specific editorial
  guidance. The cover byline remains intentionally simple.
- The web catalog is generated from the publishing manifest and contains all 403
  documents, 1,235 detected code examples, PDF downloads, four segment paths,
  book-local contents, and source links.
- Source, code, artifact, catalog, navigation, layout, deployment, and visual QA
  are recorded in `reports/build/SERIES_BUILD_REPORT.md`.

No framework migration or replacement PDF toolchain was introduced. The existing
publishing system was extended only where the four-folder artifact contract and
complete web synchronization required it.
