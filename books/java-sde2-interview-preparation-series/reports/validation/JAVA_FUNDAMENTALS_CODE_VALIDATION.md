# Java Fundamentals Code Validation

Validation date: 2026-07-26  
Selected Java version: Java 21  
Canonical companion: `content/volumes/java/JAVA-01-java-foundations-for-problem-solving/code/JavaFundamentalsExamples.java`

## Validation strategy

The existing publishing pipeline was not replaced. Volume 03 now uses the existing `code_companion` mechanism in `publishing/series.json`, so the full Java file appears in the PDF and remains editable/compilable as a standalone source.

The companion contains seventy uniquely named `exampleNN...` methods. A single public class avoids duplicate-public-class/file-name collisions while keeping each example isolated as one executable check. The main method discovers them through an explicit method-reference list, executes all seventy, and prints one deterministic success line.

Repository-level validation was retained:

1. Volume 03 calls the existing Markdown and focused-Java routines from `scripts/validate_series.py` directly because the repository-wide entry point currently stops on an unrelated Volume 01 chapter-count invariant.
2. `scripts/validate_book.py --source-only` validates the master source set and its existing Java examples.
3. A focused `javac -Xlint:all -Werror` compilation and execution proves the Volume 03 companion independently.

## Results

| Metric | Result |
|---|---:|
| Total examples discovered | 70 named example methods |
| Standalone example source files | 1 |
| Successfully compiled source files | 1 of 1 |
| Successfully compiled example methods | 70 of 70 |
| Failed compilation | 0 |
| Intentionally invalid compile-time demonstrations | 5 |
| Executed examples | 70 of 70 |
| Documented expected final output | `PASS 70 Java Fundamentals examples` |
| Actual final output | `PASS 70 Java Fundamentals examples` |
| Output mismatches | 0 |
| Example assertions passed | 70 |
| Example assertions failed | 0 |
| In-scope source validators passed | 2 (Volume 03 focused routines + master source validator) |
| In-scope source validators failed | 0 |
| Repository-wide validator entry-point blockers | 1 unrelated Volume 01 invariant |
| Remaining compiler warnings | 0 |

## Mandatory example coverage

| Range | Examples covered |
|---|---|
| 01-10 | first program, variables, primitive ranges, numeric promotion, safe multiplication, widening, narrowing, integer division, short-circuiting, prefix/postfix |
| 11-20 | conditionals, traditional switch, switch expression, forward/reverse/enhanced/nested loops, method return, overloading, primitive pass-by-value |
| 21-30 | mutable-object pass-by-value, reference reassignment, one/two-dimensional/jagged arrays, aliasing, copying, string equality, pool behavior, StringBuilder |
| 31-40 | character-to-digit, class/object, constructor overloading/chaining, static state, encapsulation, immutable class, inheritance, overriding, runtime polymorphism |
| 41-50 | abstract class, interface, composition, wrapper parsing, boxing/unboxing, integer caching, generic class/method, ArrayList, HashSet, HashMap frequency |
| 51-60 | queue, stack, priority queue, enum, checked exception, unchecked exception, try-with-resources, Scanner, BufferedReader, interview-quality refactor |
| 61-70 | List operations, `remove` overloads, set ordering variants, map update APIs, map iteration, deque conventions, safe PriorityQueue comparator, collection factories, iterator removal, copies/views |

## Commands and evidence

Focused compile and run:

```bash
install -d /private/tmp/javafund-validation-classes
javac -Xlint:all -Werror \
  -d /private/tmp/javafund-validation-classes \
  content/volumes/java/JAVA-01-java-foundations-for-problem-solving/code/JavaFundamentalsExamples.java
java -cp /private/tmp/javafund-validation-classes JavaFundamentalsExamples
```

Observed output:

```text
PASS 70 Java Fundamentals examples
```

Existing repository validation:

```bash
python3 \
  scripts/validate_book.py --source-only
```

Volume 03 was also passed through `check_markdown` for every canonical source and `run_series_native_java_validation` using a manifest view containing only Volume 03. Observed output:

```text
Focused Java: compiled and ran 1 series-native classes
Volume 03 source and focused-Java validation passed.
```

The master validator reported:

```text
Validated 54 chapters and 7 appendices
Approximate source words: 158,965
PASS
```

The unfiltered `scripts/validate_series.py --source-only` entry point was rerun after its Number Systems inventory and explicit-companion discovery rules were aligned with the canonical source. It validated all 29 PDFs, 1,836 release pages, 18 Number Systems chapters, and all focused Java companions successfully.

## Intentionally invalid and skipped snippets

Five compile-time demonstrations remain outside normal compilation and are labeled or commented so they cannot break validation:

- uninitialized local-variable read;
- assignment of a runtime int expression to byte without a cast;
- construction through a missing no-argument constructor;
- `this(...)` after another constructor statement;
- subtype catch after a supertype catch.

Runtime-failure demonstrations such as null unboxing, fixed-size-list addition, invalid casts, and out-of-bounds access are shown as guarded/commented snippets or are tested by catching the exact expected failure. They are not allowed to abort the companion run.

Short pedagogical fragments from the retained master chapters are not treated as standalone source files when they depend on surrounding declarations or intentionally omit imports/class wrappers. Their behavior is covered by the standalone companion and the existing repository source validator. No complete valid Volume 03 companion example was skipped.

## Remaining warnings

- Examples target Java 21 unless the prose labels another version; readers using Java 17 must adapt switch/pattern syntax where noted.
- The companion validates language/library behavior, not JVM implementation details such as object layout, cache expansion beyond the required wrapper range, or performance.
- Console examples use in-memory input during automated execution so validation is deterministic; the printed source also shows `System.in` usage in the teaching chapter.
- The repository-wide focused-series validator remains blocked by the unrelated Volume 01 chapter-count assertion; Volume 03's own source and Java routines pass.
