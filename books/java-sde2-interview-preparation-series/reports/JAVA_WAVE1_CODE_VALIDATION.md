# Java Wave 1 Code Validation

Date: 2026-08-02

Target language level: Java 21

Compiler used: OpenJDK `javac 24.0.1` with `--release 21`

## Result

PASS. Every valid Java source added by Wave 1 compiled with all lint categories enabled and warnings treated as errors. Every program ran successfully. The output of all eleven reader-facing chapter examples matched the documented output exactly.

## Inventory

| Category | Discovered | Compiled | Executed | Failures | Output mismatches |
|---|---:|---:|---:|---:|---:|
| new chapter-scoped Fundamentals examples | 11 | 11 | 11 | 0 | 0 |
| new Fundamentals solution harness | 1 | 1 | 1 | 0 | 0 |
| new Collections implementation companion | 1 | 1 | 1 | 0 | 0 |
| existing Fundamentals 70-example companion | 1 | 1 | 1 | 0 | 0 |
| **Total Java source programs** | **14** | **14** | **14** | **0** | **0** |

Executable verification totals:

- 70 pre-existing Fundamentals checks passed;
- 11 new solution checks passed;
- 5 low-level collection suites passed;
- 11 chapter example output transcripts matched byte-for-byte after line normalization;
- 0 compiler warnings;
- 0 test failures;
- 0 unexplained skips.

## New Fundamentals source files

```text
code/fundamentals/ValuesAndTypesExample.java
code/fundamentals/OperatorsAndConversionsExample.java
code/fundamentals/ControlFlowExample.java
code/fundamentals/MethodsAndPassByValueExample.java
code/fundamentals/ArraysExample.java
code/fundamentals/StringsAndCharactersExample.java
code/fundamentals/ClassesAndConstructorsExample.java
code/fundamentals/ObjectOrientedFoundationsExample.java
code/fundamentals/WrappersGenericsEnumsExample.java
code/fundamentals/ExceptionsIoUtilitiesExample.java
code/fundamentals/InterviewQualityExample.java
code/fundamentals/FundamentalsSolutionChecks.java
```

The first eleven files are exact copies of the complete Java blocks printed in their chapters. A process-substitution diff was run for every chapter/file pair; all eleven matched.

## New Collections companion

```text
content/volumes/java/JAVA-05-collections-streams-and-io/code/CollectionsImplementationChecks.java
```

Its five suites validate:

1. dynamic-array resize, insertion shift, removal shift, capacity/size distinction, and upper bounds;
2. linked-list endpoints, middle unlink, and reciprocal link consistency;
3. deliberate hash collisions, lookup through chains, resize/re-bucketing, update size, removal, null key, and mapped-null distinction;
4. heap invariant after every offer and poll, sorted poll order, and empty poll;
5. subtraction-comparator overflow, safe extreme-value ordering, and deterministic object tie-breaking.

## Commands executed

From the repository root:

```bash
base=books/java-sde2-interview-preparation-series/content/volumes/java/JAVA-01-java-foundations-for-problem-solving
fund_out=$(mktemp -d /tmp/java-fundamentals-wave1.XXXXXX)
javac --release 21 -Xlint:all -Werror \
  -d "$fund_out" \
  "$base"/code/fundamentals/*.java
```

Each of the eleven `*Example` classes was then executed with assertions enabled, followed by:

```bash
java -ea -cp "$fund_out" FundamentalsSolutionChecks
```

Observed output:

```text
PASS 11 Java Fundamentals solution checks
```

The existing canonical companion was revalidated independently:

```bash
legacy_out=$(mktemp -d /tmp/java-fundamentals-legacy.XXXXXX)
javac --release 21 -Xlint:all -Werror \
  -d "$legacy_out" \
  "$base/code/JavaFundamentalsExamples.java"
java -ea -cp "$legacy_out" JavaFundamentalsExamples
```

Observed output:

```text
PASS 70 Java Fundamentals examples
```

The Collections companion was compiled and run independently:

```bash
collections_out=$(mktemp -d /tmp/java-collections-wave1.XXXXXX)
javac --release 21 -Xlint:all -Werror \
  -d "$collections_out" \
  books/java-sde2-interview-preparation-series/content/volumes/java/JAVA-05-collections-streams-and-io/code/CollectionsImplementationChecks.java
java -ea -cp "$collections_out" CollectionsImplementationChecks
```

Observed output:

```text
PASS 5 low-level collection implementation suites
```

## Expected-output validation

For each new Fundamentals chapter, validation:

1. read the filename declared after `File:`;
2. diffed the chapter's complete Java block against the corresponding source file;
3. executed the declared public class;
4. diffed process output against the chapter's `Expected output` text block.

Results: 11 exact source matches and 11 exact output matches.

## Intentionally invalid demonstrations

Invalid behavior is never compiled as a complete source:

- byte addition assigned directly to byte is commented and labeled a compile-time error;
- reading an uninitialized local is posed as a debugging explanation, not executable source;
- `List<int>` is shown only in an edge matrix and corrected to `List<Integer>`;
- null unboxing is commented and labeled as a runtime failure.

Skipped complete examples: 0. Intentionally invalid standalone files: 0.

## Markdown/content checks

- all newly added reader-facing Markdown files have balanced fenced code blocks;
- complete example class names are unique;
- no generated `.class` files were written into the repository;
- compilation output was isolated in `mktemp` directories under `/tmp`;
- no new dependency was introduced.

## Remaining integration validation

The new sources are not yet listed in `publishing/series.json` by design. After the coordinating manifest edit, run the existing focused-series validator and PDF build. Remaining required checks are:

1. focused-series extraction compiles and runs every public native example;
2. volume `18C` recognizes the new companion metadata;
3. Fundamentals and Collections PDFs rebuild successfully;
4. affected PDF pages show no clipped code, split row, or orphan heading;
5. web source routes and download links reflect the new chapter order.

These are integration checks, not unresolved Java compilation defects.
