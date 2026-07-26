# Bit Manipulation Code Validation

## Validation scope

Volume 04 contains one dependency-free complete Java 21 companion and 124 contextual Java snippets across the teaching, practice, and solution sources. The contextual snippets include focused methods, output questions, API fragments, and twenty intentionally flawed debugging examples. They are not concatenated into one compilation unit.

## Discovery summary

| Measure | Result |
|---|---:|
| Java code fences in Markdown sources | 125 |
| Standalone complete Java companions | 1 |
| Contextual or method-level snippets | 124 |
| Intentionally flawed debugging snippets | 20 |
| Standalone examples compiled | 1 |
| Successful standalone compilations | 1 |
| Failed standalone compilations | 0 |
| Standalone examples executed | 1 |
| Executable behavioral checks | 40 |
| Behavioral-check failures | 0 |
| Output mismatches | 0 |

## Canonical companion

Source:

`content/volumes/04-bit-manipulation-in-java/code/BitManipulationExamples.java`

Compilation command:

```bash
javac --release 21 -Xlint:all -Werror \
  content/volumes/04-bit-manipulation-in-java/code/BitManipulationExamples.java
```

Execution command:

```bash
java BitManipulationExamples
```

Observed output:

```text
PASS 40 Bit Manipulation checks
```

The companion covers:

- padded fixed-width representation;
- validated one-bit operations;
- low-width masks and field extraction/replacement;
- set-bit count, powers of two/four, Hamming distance, and reversal;
- count-bits dynamic programming;
- one/two/triple XOR families and missing value;
- prefix/range XOR and target-XOR subarray counting;
- submask count and Gray code;
- maximum-XOR trie;
- range AND and total set-bit recurrence;
- significant complement and minimum XOR pair;
- distinct subarray OR;
- atomic flag update; and
- addition with XOR and carry.

## Repository-native validation

The repository's own `run_series_native_java_validation` routine was invoked with the manifest filtered to Volume 04. Result:

```text
Focused Java: compiled and ran 1 series-native classes
```

This routine compiles with:

```text
javac --release 21 -Xlint:all -Werror
```

and executes the public companion with assertions enabled.

## Contextual-snippet policy

- Method-level examples are reviewed in the context of their named imports and surrounding explanation.
- Output snippets are executed indirectly by equivalent assertions in the companion where they represent core behavior.
- Debugging snippets are deliberately incorrect and are isolated in the practice chapter.
- Each debugging prompt asks for a failing input and repair; the solution chapter explains the defect.
- No intentionally invalid snippet is included in the standalone compilation unit.

## PDF validation

| Check | Result |
|---|---|
| Page count reopened with `pypdf` | 109 |
| Required chapter/content markers | 11 of 11 |
| PDF metadata title/subject/author | passed |
| External link annotations | 45 |
| LinkedIn annotation | present |
| GitHub annotation | present |
| Near-empty body pages | 0 |
| Manifest page/size/hash match | passed (`bb0581058184...`) |
| Visual render inspection | passed after one title-length repair |

## Repository-wide validation

The full `scripts/validate_series.py --source-only` command now recognizes the canonical 18-chapter Number Systems inventory and each volume's explicit Java companion. It validates every focused volume, PDF, link, role-credit marker, licensing marker, and manifest entry successfully.

## Final result

Volume 04 valid code compiles with warnings treated as errors, all forty behavioral checks pass, documented companion output matches execution, and the rebuilt PDF passes content and navigation checks.
