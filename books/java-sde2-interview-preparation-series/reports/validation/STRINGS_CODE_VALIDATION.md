# Strings and String Patterns Code Validation

## Validation scope

Volume 07 contains one dependency-free complete Java 21 companion and contextual Java fences across seven teaching chapters, one practice lab, and one solution guide. Contextual snippets include complete methods, focused fragments, output questions, and intentionally flawed debugging scenarios; they are not concatenated into an artificial compilation unit.

## Discovery summary

| Measure | Result |
|---|---:|
| Focused native Markdown files | 9 |
| Focused native words | 15,657 |
| Java code fences in focused Markdown | 81 |
| Standalone complete Java companions | 1 |
| Numbered intentionally flawed debugging scenarios | 12 |
| Standalone examples compiled | 1 |
| Successful standalone compilations | 1 |
| Failed standalone compilations | 0 |
| Standalone examples executed | 1 |
| Executable behavioral checks | 50 |
| Behavioral-check failures | 0 |
| Output mismatches | 0 |

## Canonical companion

Source:

`content/volumes/07-strings-and-string-patterns/code/StringPatternsExamples.java`

Compilation command:

```bash
javac --release 21 -Xlint:all -Werror \
  content/volumes/07-strings-and-string-patterns/code/StringPatternsExamples.java
```

Execution command from the compiled output directory:

```bash
java StringPatternsExamples
```

Observed output:

```text
PASS 50 Strings checks
```

The companion validates core indexing, immutable transformations, code-point reversal, strict parser range handling, exact and phrase palindromes, one-deletion palindrome, anagrams, signatures, code-point frequencies, common prefixes, run encoding, fixed windows, unique windows, at-most and exactly-K counts, anagram starts, minimum covering window, replacement budget, naive search, KMP prefix/search/all matches, verified rolling hash, split trailing fields, and 2,000 fixed-seed naive-versus-KMP comparisons.

## Contextual-snippet policy

- Core complete methods have equivalent compiled companion coverage.
- Predicted-output snippets remain isolated so repeated local names do not cause false failures.
- Debugging exercises are intentionally wrong and excluded from standalone compilation.
- Null, regex, overflow, index-unit, hash-collision, and Unicode limitation examples are labeled in surrounding text.
- Solutions explain the violated contract, invariant, boundary, text unit, or ownership rule before the repair.
- Version-specific syntax is limited to Java 21-compatible APIs and labeled where first introduced.

## Commands already executed

```bash
tmp_classes=$(mktemp -d)
javac --release 21 -Xlint:all -Werror -d "$tmp_classes" \
  content/volumes/07-strings-and-string-patterns/code/StringPatternsExamples.java
java -cp "$tmp_classes" StringPatternsExamples
```

Direct result: one successful compilation, zero failures, one execution, 50 passed checks, and zero output mismatches.

## Repository-native validation

```bash
python3 scripts/validate_series.py --source-only
```

Observed result: 150 unique mapped Markdown files, 18 Number Systems chapters, 17 Number Systems diagrams, 24 standalone Number Systems blocks, and 19 declared series-native Java classes validated successfully. Volume 07 introduced zero focused-companion compilation failures and zero output mismatches.

## Result

All valid standalone Strings code compiles with warnings treated as errors. All fifty deterministic checks pass, including the seeded differential search suite, and documented companion output matches execution. Repository-native and PDF validation both passed.

## PDF validation result

| PDF check | Result |
|---|---|
| Final Volume 07 pages | 106 |
| Metadata title/author | passed |
| Link annotations | 71 |
| LinkedIn/GitHub links | present |
| Manifest size/hash | passed |
| Semantic pagination | 0 errors, 0 warnings |
| Latest rendered pages inspected | all 106 through contact sheets; 16 full-page samples |
| Clipping, overlap, broken tables/figures | none observed |
