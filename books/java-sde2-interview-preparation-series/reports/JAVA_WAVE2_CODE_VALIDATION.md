# Java Wave 2 Code Validation

Date: 2026-08-02

Target: Java 21

Compiler: OpenJDK `javac 24.0.1` with `--release 21`

## Final result

PASS. All five dependency-free companion sources compile together with every lint category enabled and warnings promoted to errors. All five programs execute with assertions enabled, all documented outputs match exactly, and repeated concurrency/evidence runs did not expose a timing flake after the thread-state check was made deterministic.

## Inventory and results

| Volume | Source | Suites | Compile | Run | Documented output |
|---|---|---:|---|---|---|
| 18A | `JvmExecutionEvidenceLab.java` | 5 | PASS | PASS | exact match |
| 18B | `LanguageContractChecks.java` | 6 | PASS | PASS | exact match |
| 18D | `ConcurrencyInvariantChecks.java` | 6 | PASS | PASS | exact match |
| 18E | `PerformanceEvidenceChecks.java` | 5 | PASS | PASS | exact match |
| 18G | `AdvancedJavaReadinessAssessment.java` | 6 | PASS | PASS | exact match |
| **Total** | **5 standalone programs** | **28 suites** | **5/5** | **5/5** | **5/5** |

Compilation failures: 0

Runtime failures: 0

Compiler warnings: 0

Output mismatches: 0

Skipped valid programs: 0

Intentionally invalid standalone programs: 0

## Combined compile command

From the repository root:

```bash
wave2_out=$(mktemp -d /tmp/java-wave2-all.XXXXXX)
javac --release 21 -Xlint:all -Werror -d "$wave2_out" \
  books/java-sde2-interview-preparation-series/content/volumes/java/JAVA-06-jvm-and-execution/code/JvmExecutionEvidenceLab.java \
  books/java-sde2-interview-preparation-series/content/volumes/java/JAVA-04-language-oop-and-modern-java/code/LanguageContractChecks.java \
  books/java-sde2-interview-preparation-series/content/volumes/java/JAVA-07-concurrency-and-memory-model/code/ConcurrencyInvariantChecks.java \
  books/java-sde2-interview-preparation-series/content/volumes/java/JAVA-08-performance-diagnostics-and-gc-incidents/code/PerformanceEvidenceChecks.java \
  books/java-sde2-interview-preparation-series/content/volumes/java/JAVA-09-question-bank-study-plan-and-reference/code/AdvancedJavaReadinessAssessment.java
```

Execution:

```bash
for class_name in \
  JvmExecutionEvidenceLab \
  LanguageContractChecks \
  ConcurrencyInvariantChecks \
  PerformanceEvidenceChecks \
  AdvancedJavaReadinessAssessment; do
  java -ea -cp "$wave2_out" "$class_name"
done
```

Observed output:

```text
PASS 5 JVM execution evidence checks
PASS 6 advanced language contract suites
PASS 6 concurrency invariant suites
PASS 5 performance evidence suites
PASS 6 advanced Java readiness scenarios
```

## Determinism audit

### Class initialization

- Uses a compile-time constant and a side-effecting non-constant field.
- Verifies successful initialization once.
- Verifies first `ExceptionInInitializerError` and later `NoClassDefFoundError` without relying on log text.
- Makes no collector/JIT/layout timing assertion.

### Language contracts

- The raw-type pollution demonstration is isolated under a narrow `@SuppressWarnings({"rawtypes", "unchecked"})` annotation.
- Full source still passes `-Xlint:all -Werror`.
- Record, sealed switch, and try-with-resources behavior are deterministic Java 21 contracts.

### Concurrency

- Latches coordinate start, wait, saturation, and release states.
- Timeouts bound a failed test; sleeps are not used to establish ordering.
- Exact synchronized/atomic counts use joined platform threads.
- The volatile publication test checks a documented happens-before path.
- Executor rejection is forced with one occupied worker and one occupied queue slot.
- Virtual-thread execution uses Java 21's per-task executor and checks `isVirtual()`.

### Performance evidence

- Percentile results use declared nearest-rank math on a copied array.
- Memory snapshot assertions check only portable non-negative/committed relationships.
- The worker's blocking state is polled to a bounded state transition before the stack snapshot; it does not race immediately after a signal.
- JFR creates a custom event, dumps to a temporary file, reads the exact event/message, closes resources, and deletes the file in `finally`.
- No temporary `.jfr` files remained after validation.
- No exact memory size, GC count, pause, compilation, or timing target is asserted.

### Readiness assessment

- Lazy-holder initialization, runtime annotation retention, sealed dispatch, atomic state, failed-future recovery, and virtual-thread execution are self-contained.
- The inventory assertion is explicitly one JVM-instance scope; the reader chapter supplies the multi-instance correction.

## Repeated-run validation

The two suites most exposed to scheduling/runtime state were compiled once and each executed 25 times:

```text
ConcurrencyInvariantChecks: 25/25 PASS
PerformanceEvidenceChecks: 25/25 PASS
```

An initial thread-snapshot version could observe the worker between its latch signal and blocking call. Validation caught that flake. The final code explicitly waits for `WAITING`/`TIMED_WAITING` with a bounded spin before snapshotting, then releases and joins the worker.

## Markdown/source validation

- Five reader chapters contain balanced fenced code blocks.
- Every companion filename/class name is unique.
- Every chapter's expected-output block was diffed against the corresponding process output: 5 exact matches.
- Compilation output was isolated under `/tmp`; no `.class` files were written into the repository.
- JFR output was temporary and removed.
- No external dependency, build plugin, or framework was added.

## Remaining integration checks

Wave 2 files are not yet listed in `publishing/series.json` by design. After the coordinating manifest edit:

1. run the existing focused-series validator;
2. confirm each advanced volume recognizes exactly one code companion;
3. rebuild PDFs for 18A, 18B, 18D, 18E, and 18G;
4. inspect new tables, ASCII diagrams, command blocks, interview cards, exercises, and solutions;
5. confirm final page counts stay within configured ranges;
6. update web chapter order/download routes through the existing publishing system.

These are publishing integration tasks, not remaining Java code defects.
