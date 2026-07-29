# Module 08: Strings and String Patterns

Strings appear simple, but interview failures often come from Java behavior rather than the main algorithm: identity instead of value equality, immutable concatenation inside loops, UTF-16 boundary mistakes, regex surprises in `split`, an incorrect sliding-window invariant, or an unstated null and normalization contract.

This module begins with SDE-1 mechanics and only then moves to SDE-2 pattern selection. Use the web lessons for focused reading and revision. Use the 106-page book when you want the complete diagrams, dry runs, practice lab, separated solutions, and readiness assessment.

[Start the web lessons](01-string-internals-and-patterns.md){ .md-button .md-button--primary }
[Download the current 106-page Strings PDF](https://github.com/vinayreddykalluri/SDE2-Interview-Handbook/raw/refs/heads/master/books/java-sde2-interview-preparation-series/dist/Java-SDE2-DSA-07-Strings-and-String-Patterns.pdf){ .md-button }
[Inspect the canonical Markdown](https://github.com/vinayreddykalluri/SDE2-Interview-Handbook/tree/master/books/java-sde2-interview-preparation-series/content/volumes/07-strings-and-string-patterns){ .md-button }

## Prerequisite check

Before using window or pattern-matching templates, confirm that you can:

- explain `==` versus `.equals()` for strings;
- trace half-open ranges such as `[left, right)`;
- use `StringBuilder` for repeated construction;
- distinguish a `char` index from a Unicode code-point count;
- use arrays or maps for frequency state; and
- explain why two monotonic pointers can perform aggregate linear work.

If any item is uncertain, revisit [Strings in Java](../01-java-runtime/13-strings.md), [Loop Reasoning](../04-loop-reasoning/index.md), [Index Calculations](../05-indexing/index.md), and [Arrays](../07-arrays/index.md) first.

## Web learning path

1. [String Internals and Processing Patterns](01-string-internals-and-patterns.md) — values, immutability, equality, builders, text representation, traversal, and baseline processing.
2. [Strings: Interview Deep Dive](02-interview-deep-dive.md) — decision rules, invariants, correctness arguments, worked traces, complexity, trade-offs, and progressive practice.
3. Reimplement the linked Java companion without copying it.
4. Use the publication-depth chapter map below to close remaining gaps.

## What the publication-depth book adds

The PDF and its canonical Markdown contain seven ordered chapters plus separate exercise and solution sources:

1. **String Foundations from Zero** — creation, equality, immutability, core APIs, null, empty, and blank contracts.
2. **Traversal, Unicode, and Text Boundaries** — UTF-16 units, code points, graphemes, bytes, index contracts, and locale-aware boundaries.
3. **Building, Parsing, and Conversion** — `StringBuilder`, tokenization, numeric parsing, delimiters, formatting, and failure behavior.
4. **Two Pointers, Frequency, and Core String Patterns** — palindrome checks, anagrams, prefix reasoning, and bounded-alphabet state.
5. **Sliding Windows and Substring State** — fixed windows, longest-valid windows, minimum cover, replacement budgets, and invariant repair.
6. **Pattern Matching and Search** — naive matching, KMP derivation, prefix tables, rolling-hash boundaries, Z-function context, and API selection.
7. **SDE-2 String Interview Playbook** — recognition signals, contracts, proofs, production text boundaries, testing, and follow-up chains.

The [canonical source directory](https://github.com/vinayreddykalluri/SDE2-Interview-Handbook/tree/master/books/java-sde2-interview-preparation-series/content/volumes/07-strings-and-string-patterns) is the editable source of truth. The portal reads its Markdown headings when generating book cards, so chapter previews stay aligned with the book.

## Readiness gate

Move to Hashing only when you can solve and defend all five of these without a template:

1. content equality with an explicit null contract;
2. a two-pointer palindrome scan;
3. an anagram or frequency-signature comparison;
4. a variable sliding window with a stated invariant; and
5. exact substring search with a justified choice between the standard API, a baseline scan, and KMP.

For each solution, state the text unit, output-index unit, time cost, auxiliary space, mutation behavior, and at least three failure-focused tests.

## Runnable reference

The interview deep dive links the matching Java implementation under `examples/java/src/main/java/`. The publication book also includes a standalone Java 21 companion with deterministic checks and differential KMP validation.
