# Java Wave 2 Content Audit and Changelog

Date: 2026-08-02

Scope: Advanced Java volumes `18A`, `18B`, `18D`, `18E`, and `18G`

Status: five supplemental synthesis chapters and five executable companions complete; manifest/PDF/web integration intentionally deferred

## Executive finding

The five advanced volumes already contain substantial, technically strong master chapters. Their weakness was not missing definitions or insufficient page count. Each topic was taught in a chapter-local template, but the books needed an integrative place where a reader must connect contracts, low-level mechanics, edge cases, production evidence, and an interview answer under pressure.

Wave 2 therefore **supplements rather than replaces** every existing master source. No master chapter should be removed. Each new native chapter is a capstone/workshop that assumes the existing prerequisites and adds deterministic Java 21 evidence, worked incident reasoning, realistic interviewer follow-ups, exercises, and fully explained solutions.

## Baseline audited

| Volume | Existing sources | Existing PDF | Baseline pages | Baseline size | Main gap addressed |
|---|---:|---|---:|---:|---|
| 18A JVM and Execution | 10 master chapters | `dist/01-java/Java-SDE2-JAVA-06-JVM-and-Execution.pdf` | 85 | 4,481,860 B | cross-layer evidence workshop and runnable initialization/failure checks |
| 18B Language/OOP/Modern Java | 9 master chapters + feature appendix | `dist/01-java/Java-SDE2-JAVA-04-Language-OOP-and-Modern-Java.pdf` | 94 | 3,913,421 B | contract interaction, API-evolution matrix, executable edge suite |
| 18D Concurrency/JMM | 7 master chapters | `dist/01-java/Java-SDE2-JAVA-07-Concurrency-and-Memory-Model.pdf` | 70 | 4,144,563 B | one invariant/capacity/cancellation protocol with deterministic tests |
| 18E Performance/Diagnostics/GC | 3 master chapters + tool appendix | `dist/01-java/Java-SDE2-JAVA-08-Performance-Diagnostics-and-GC-Incidents.pdf` | 48 | 3,767,598 B | end-to-end incident workflow and safe public-API evidence companion |
| 18G Question Bank/Study Plan | playbook, 54-question bank, plan, solutions/reference appendices | `dist/01-java/Java-SDE2-JAVA-09-Question-Bank-Study-Plan-and-Reference.pdf` | 96 | 4,017,320 B | layered mock answer studio, full mixed scenario, executable assessment |

Baseline PDFs were inspected only for metadata. They do not yet contain Wave 2 content.

## Audit findings

### What was already strong

- Master chapters distinguish Java/JLS/JVMS contracts from HotSpot/OpenJDK implementation details.
- Class loading, runtime areas, GC, JIT, language design, generics, exceptions, JMM, executors, virtual threads, JMH, diagnostics, and incident response all had meaningful standalone depth.
- The existing question bank already provides 54 concise model answers, follow-ups, and traps.
- Existing PDFs are within configured page ranges.

### High-value gaps

1. There was no focused executable companion for any of the five volumes.
2. Runtime chapters needed one evidence ladder connecting `javac`/`javap`, loading, initialization, logical frames, reachability, JIT claims, and production diagnosis.
3. Language chapters needed one call-site-to-compatibility workflow connecting overload/dispatch, substitutability, equality, erasure, sealed types, reflection, and suppressed failures.
4. Concurrency chapters needed one deterministic lab proving transitions and admission policy without sleep-based timing.
5. Performance chapters needed safe runnable evidence using management/JFR APIs and a single symptom-to-mitigation playbook.
6. The revision volume needed a full mixed mock that connects local JVM correctness to process/system boundaries, not another list of isolated definitions.

## New content inventory

| Volume | New chapter | Words | Interview cards/questions | Exercises | Worked solutions | Companion result |
|---|---|---:|---:|---:|---:|---|
| 18A | `01-execution-evidence-workshop.md` | 2,510 | 6 | 6 | 6 | PASS 5 |
| 18B | `01-language-contracts-and-evolution-lab.md` | 2,667 | 8 | 7 | 7 | PASS 6 |
| 18D | `01-concurrency-invariants-and-capacity-lab.md` | 2,537 | 8 | 7 | 7 | PASS 6 |
| 18E | `01-evidence-first-incident-workshop.md` | 2,463 | 8 | 7 | 7 | PASS 5 |
| 18G | `01-advanced-java-mock-interview-studio.md` | 2,733 | 12 + full mock | 6 | 6 | PASS 6 |
| **Total** | **five substantial chapters** | **12,910** | **42 + full mock** | **33** | **33** | **28 suites** |

Each question includes a worked model answer; most include a follow-up answer and an explicit misconception or production boundary.

## Volume 18A changes

### Added learning value

- a three-layer answer model: language contract, JVM contract, runtime observation;
- a source -> class file -> runtime -> diagnostic evidence ladder;
- constant inlining versus active-use initialization with bytecode implications;
- erroneous-class transition from first initializer failure to later use;
- frame/reference/object diagrams without universal physical-placement claims;
- reachability versus lexical scope and allocation versus retention;
- JIT speculation/deoptimization and safepoint caveats;
- failure/evidence matrix for version, verification, loading, initialization, stack, heap, and metaspace errors;
- worked startup-regression and class-loader-retention incidents.

### Companion

`JvmExecutionEvidenceLab.java` deterministically checks initialization triggers, loader API boundaries, `StackWalker` logical frames, failed initialization state, and aliasing. It deliberately does not assert GC timing, object layout, JIT decisions, or physical stack placement.

## Volume 18B changes

### Added learning value

- one two-stage model for compile-time overload selection and runtime override dispatch;
- behavioral substitutability beyond legal signatures;
- equality design across inheritance and record shallow mutability;
- operation-first wildcard design, erasure, compiler casts, heap pollution, reifiable boundaries, and bridge methods;
- sealed-hierarchy exhaustiveness as an API-evolution choice;
- lambda identity/capture warnings and reflection/module/loader boundaries;
- primary versus suppressed resource failures;
- a source/binary/behavior/reflection/data compatibility matrix.

### Companion

`LanguageContractChecks.java` validates dispatch, record semantics, variance, controlled raw-type pollution, Java 21 sealed pattern switch, and suppressed exceptions under `-Werror`.

## Volume 18D changes

### Added learning value

- state -> invariant -> owner -> transition -> happens-before -> progress -> capacity -> evidence review order;
- classification of confined, immutable-published, invariant-bearing, and statistical state;
- exact happens-before publication proof;
- locks versus atomics versus concurrent compound operations;
- interruption translation/restoration and cancellation ownership;
- executors as admission policy with deterministic saturation;
- virtual threads separated from downstream capacity, with Java-version pinning caveat;
- failure/evidence matrix for races, deadlock, starvation, livelock, overload, cancellation, and pinning;
- layered concurrency testing without claiming tests prove race absence.

### Companion

`ConcurrencyInvariantChecks.java` validates synchronized/atomic transitions, volatile publication, interruption, bounded executor rejection, and virtual-thread execution. Latches establish state; timeouts only bound failure.

## Volume 18E changes

### Added learning value

- metric/population/baseline/time-window framing;
- declared percentile calculation and aggregation cautions;
- competing hypotheses with discriminating predictions;
- a low-risk tool ladder from ambient telemetry to dumps;
- thread dump, JFR, heap-dump, and benchmark evidence boundaries;
- coordinated-omission warning;
- allocation, retention, live set, RSS, native memory, and class-loader separation;
- collector reasoning from workload/live set/headroom rather than flag slogans;
- reversible mitigation template with expected signal and rollback.

### Companion

`PerformanceEvidenceChecks.java` validates exact percentile calculation, management memory snapshots, a deterministic waiting-thread snapshot, JFR custom-event write/read/delete, and bounded retention policy. It makes no environment-specific timing, size, GC, or compilation assertion.

## Volume 18G changes

### Added learning value

- six-dimension answer scoring for correctness, mechanism, edge, judgment, evidence, and communication;
- 12 advanced mock cards with short answer, follow-up depth, and misconception control;
- one 35-minute cross-domain service scenario covering startup, reflection, atomic inventory, virtual threads, process/system scope, and p99 evidence;
- error-log routing from a miss to owning chapter, changed edge, 24-hour retest, and one-week mixed retest;
- an evidence-based readiness decision instead of rereading the entire series.

### Companion

`AdvancedJavaReadinessAssessment.java` checks lazy-holder initialization, runtime annotation discovery, sealed-domain exhaustiveness, atomic immutable-state transition, asynchronous failure recovery, and virtual-thread execution. The chapter explicitly says that a passing executable validates examples, not spoken interview readiness.

## Accuracy boundaries reinforced

- Loading is not initialization; compile-time constants can be inlined.
- Class identity includes defining loader.
- Source variables do not imply permanent physical stack slots; object layout and allocation elimination are runtime details.
- GC eligibility does not promise immediate reclamation and `System.gc()` is not a deterministic test primitive.
- Overload selection precedes runtime override dispatch; static methods/fields are not polymorphic.
- Records are not deeply immutable by default.
- Heap pollution originates at an unchecked boundary and may fail at a later compiler-inserted cast.
- `@SafeVarargs` is an audited promise, not compiler proof.
- Volatile orders accesses but does not make compound transitions atomic.
- A concurrent collection does not make an arbitrary multi-operation invariant atomic.
- Virtual threads do not increase downstream capacity; pinning behavior must be version-labeled.
- A timeout can leave underlying work alive without an end-to-end cancellation protocol.
- Rising heap before GC is not sufficient evidence of a leak.
- Low pauses alone do not prove healthy collector throughput/headroom.
- JMH improves harness mechanics but cannot make an irrelevant workload representative.

## Exact manifest mappings

All five additions are supplements. Keep every current source entry unchanged and add one `series_native` chapter plus one companion.

### Volume `18A`

Add after `content/master/10-execution-engine-jit-compilation-and-safepoints.md`:

```json
{
  "path": "content/volumes/java/JAVA-06-jvm-and-execution/chapters/01-execution-evidence-workshop.md",
  "series_native": true
}
```

Add:

```json
"code_companion": {
  "path": "content/volumes/java/JAVA-06-jvm-and-execution/code/JvmExecutionEvidenceLab.java",
  "title": "JVM Execution Evidence Lab",
  "description": "Deterministic Java 21 checks for initialization triggers, loader boundaries, logical stack frames, erroneous class initialization, and reference aliasing. A successful run prints exactly PASS 5 JVM execution evidence checks."
}
```

### Volume `18B`

Add after `content/master/24-java-17-21-features.md` and before the feature-matrix appendix:

```json
{
  "path": "content/volumes/java/JAVA-04-language-oop-and-modern-java/chapters/01-language-contracts-and-evolution-lab.md",
  "series_native": true
}
```

Add:

```json
"code_companion": {
  "path": "content/volumes/java/JAVA-04-language-oop-and-modern-java/code/LanguageContractChecks.java",
  "title": "Advanced Language Contract Checks",
  "description": "Executable Java 21 checks for overload/override selection, record semantics, generic variance and heap pollution, sealed pattern dispatch, and suppressed resource failures. A successful run prints exactly PASS 6 advanced language contract suites."
}
```

### Volume `18D`

Add after `content/master/38-concurrency-failure-modes-testing-and-design-patterns.md`:

```json
{
  "path": "content/volumes/java/JAVA-07-concurrency-and-memory-model/chapters/01-concurrency-invariants-and-capacity-lab.md",
  "series_native": true
}
```

Add:

```json
"code_companion": {
  "path": "content/volumes/java/JAVA-07-concurrency-and-memory-model/code/ConcurrencyInvariantChecks.java",
  "title": "Concurrency Invariant Checks",
  "description": "Deterministic Java 21 checks for synchronized and atomic transitions, volatile publication, interruption, bounded executor rejection, and virtual-thread execution. A successful run prints exactly PASS 6 concurrency invariant suites."
}
```

### Volume `18E`

Add after `content/master/41-memory-leaks-gc-incidents-and-tuning-playbooks.md` and before the tools appendix:

```json
{
  "path": "content/volumes/java/JAVA-08-performance-diagnostics-and-gc-incidents/chapters/01-evidence-first-incident-workshop.md",
  "series_native": true
}
```

Add:

```json
"code_companion": {
  "path": "content/volumes/java/JAVA-08-performance-diagnostics-and-gc-incidents/code/PerformanceEvidenceChecks.java",
  "title": "Performance Evidence Checks",
  "description": "Safe Java 21 checks for percentile math, JVM memory and thread snapshots, JFR custom-event round trips, and bounded retention policy. A successful run prints exactly PASS 5 performance evidence suites."
}
```

### Volume `18G`

Add after `content/master/53-sde-2-java-interview-question-bank.md` and before `content/master/54-eight-week-study-plan-and-mock-interview-loops.md`:

```json
{
  "path": "content/volumes/java/JAVA-09-question-bank-study-plan-and-reference/chapters/01-advanced-java-mock-interview-studio.md",
  "series_native": true
}
```

Add:

```json
"code_companion": {
  "path": "content/volumes/java/JAVA-09-question-bank-study-plan-and-reference/code/AdvancedJavaReadinessAssessment.java",
  "title": "Advanced Java Readiness Assessment",
  "description": "Mixed Java 21 checks for lazy initialization, runtime annotations, sealed domains, atomic state, asynchronous failure handling, and virtual threads. A successful run prints exactly PASS 6 advanced Java readiness scenarios."
}
```

## No replacements or moves

- Replace/remove existing master sources: **none**.
- Edit master sources: **none**.
- Move existing content: **none in Wave 2**.
- Manifest, scripts, web, README, PDF, and generated files edited: **none**.

The new directories should remain in place until the coordinating repository reorganization maps them as a unit.

## Integration acceptance criteria

1. Each manifest volume retains all current master sources and gains exactly one native chapter and one companion.
2. Focused-series validation compiles/runs five new companions with `--release 21 -Xlint:all -Werror`.
3. PDFs stay within existing configured page bounds.
4. Tables, code blocks, ASCII diagrams, and interview cards do not clip or split badly.
5. Table of contents places each workshop at the recommended prerequisite point.
6. Web routes show the new chapter and companion download without duplicating Fundamentals or Collections navigation.
