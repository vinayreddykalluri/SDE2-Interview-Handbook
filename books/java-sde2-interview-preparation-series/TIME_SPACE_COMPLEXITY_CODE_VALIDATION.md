# Time and Space Complexity Code Validation

Validation date: 2026-07-26  
Java target: Java 21  
Canonical companion: `series/volumes/02-time-and-space-complexity/code/ComplexityExamples.java`

## Strategy

The existing `code_companion` publishing mechanism embeds one standalone dependency-free Java file in the PDF. Each named check demonstrates behavior or a count that supports a complexity derivation. Wall-clock timing is intentionally excluded because measurements do not prove asymptotic growth.

## Results

| Metric | Result |
|---|---:|
| Standalone Java sources | 1 |
| Named executable checks | 24 |
| Successfully compiled | 1 of 1 |
| Compilation failures | 0 |
| Compiler warnings with `-Xlint:all -Werror` | 0 |
| Executed checks | 24 of 24 |
| Assertion failures | 0 |
| Expected output | `PASS 24 complexity examples` |
| Actual output | `PASS 24 complexity examples` |
| Output mismatches | 0 |

## Coverage

| Range | Checks |
|---|---|
| 01-05 | indexed access, linear scan, early exit, consecutive loops, independent nesting |
| 06-10 | triangular pairs, repeated halving, logarithmic levels, n-log-n shape, two pointers |
| 11-15 | geometric inner work, rectangular and jagged matrices, StringBuilder, defensive copy |
| 16-20 | linear/log recursion depth, output-sensitive result, ArrayList access, HashSet membership |
| 21-24 | HashMap frequency, TreeSet navigation, ArrayDeque queue, PriorityQueue removal order |

## Command

Run from `/Users/vinayreddykalluri/Documents/Java SDE 2 Interview Book`:

```bash
mkdir -p /private/tmp/complexity-validate-final
javac -Xlint:all -Werror \
  -d /private/tmp/complexity-validate-final \
  series/volumes/02-time-and-space-complexity/code/ComplexityExamples.java
java -cp /private/tmp/complexity-validate-final ComplexityExamples
```

Observed output:

```text
PASS 24 complexity examples
```

## Validation limits

- The checks validate behavior and illustrative operation counts, not nanosecond timing.
- Complexity claims about library implementations remain qualified in prose by concrete type, case, Java version, and contract.
- Intentionally incomplete teaching snippets are not compiled as standalone programs; their behavior is represented by the companion where appropriate.
