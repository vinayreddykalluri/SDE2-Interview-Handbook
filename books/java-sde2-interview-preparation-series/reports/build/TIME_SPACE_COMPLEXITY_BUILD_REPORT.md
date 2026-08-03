# Time and Space Complexity Build Report

Build date: 2026-07-26  
Volume: 02 - Time and Space Complexity for Java Interviews

## Result

The canonical Volume 02 was rebuilt successfully with the existing publishing system. It now progresses from zero-assumption operation counting through space analysis, Java collection cost models, and SDE-2 optimization reasoning. The JMH chapter was removed from this fundamentals volume and remains available in Advanced Java E.

| Item | Before | Final |
|---|---:|---:|
| PDF pages | 33 | 79 |
| PDF size | 218,294 bytes | 3,837,739 bytes |
| Canonical teaching/reference/practice sources | 3 | 8 |
| Generated executable companion chapters | 0 | 1 |
| Total generated content chapters | 3 | 9 |
| Volume-specific executable checks | 0 | 24 |
| Structured practice/assessment items | approximately 6 | 85 |

The larger file size is primarily the shared high-resolution cover artwork now embedded in every independently distributable PDF.

## Canonical sources

1. `content/volumes/02-time-and-space-complexity/chapters/01-complexity-from-zero.md`
2. `content/volumes/02-time-and-space-complexity/chapters/02-reading-complexity-from-java-code.md`
3. `content/volumes/02-time-and-space-complexity/chapters/03-space-complexity-from-zero.md`
4. `content/volumes/02-time-and-space-complexity/chapters/04-java-collections-cost-models.md`
5. `content/volumes/02-time-and-space-complexity/chapters/05-sde2-complexity-reasoning.md`
6. `content/master/appendices/b-collection-complexity.md`
7. `content/volumes/02-time-and-space-complexity/exercises/01-complexity-practice-lab.md`
8. `content/volumes/02-time-and-space-complexity/solutions/01-complexity-practice-solutions.md`
9. `content/volumes/02-time-and-space-complexity/code/ComplexityExamples.java` (builder-injected companion)

Manifest and shared publisher:

- `publishing/series.json`
- `scripts/build_series.py`
- `publishing/assets/modern-series-cover-background-v2.png`

Generated Markdown/PDF files are not canonical when editable sources exist.

## Chapters substantially improved

- Added a count-first time-complexity foundation using array access, scans, halving, sorting-level work, pairs, and output generation.
- Added a repeatable code-reading method for sequential phases, branches, independent/dependent nesting, two pointers, helper APIs, strings, copies, jagged arrays, test cases, and output-sensitive work.
- Added full beginner-to-interview space reasoning: input/auxiliary/output, peak live storage, recursion, tree height/width, shallow references, boxing, views, and mutation/ownership.
- Added a usage-aware collection cost chapter for ArrayList, LinkedList, hash/linked/tree collections, ArrayDeque, PriorityQueue, sorting, binary search, conversions, equality, order, and amortization.
- Preserved the accurate SDE-2 invariant/optimization chapter after its prerequisites and retitled it for the focused sequence.
- Added a mixed practice lab, separated reasoned solutions, cumulative assessments, final readiness criteria, and a 24-check Java companion.

## Accuracy strengthening

- Big-O is described as a growth bound, not elapsed time or exact instruction count.
- Independent input dimensions are preserved rather than collapsed into one `n`.
- Nested syntax is not automatically multiplied; aggregate pointer motion and geometric sums are derived.
- Output size and required result storage are reported explicitly.
- Recursion time and maximum active stack depth are separated.
- Java does not guarantee tail-call optimization.
- HashMap/HashSet costs are qualified as expected under sound hashing, not guaranteed O(1).
- ArrayList append is labeled amortized O(1), not universally O(1).
- LinkedList insertion includes the cost of reaching the position.
- PriorityQueue iteration is not sorted order.
- Comparator subtraction, mutable hash keys, boxing, copies, and backed views are treated as correctness/performance concerns.
- Sorting and auxiliary-space claims are qualified by Java overload/type.

## Practice totals

- Knowledge checks: **20**
- Predict/count/analyze snippets: **20**
- Debug-the-analysis exercises: **15**
- Small coding and analysis tasks: **12**
- Interview follow-ups: **12**
- Cumulative assessments: **5**
- Final readiness assessment: **1**
- Total structured items: **85**

## Java validation

`ComplexityExamples.java` compiled with Java 21 using `-Xlint:all -Werror` and executed successfully:

```text
PASS 24 complexity examples
```

Compilation failures: **0**  
Warnings: **0**  
Runtime/output mismatches: **0**

See `TIME_SPACE_COMPLEXITY_CODE_VALIDATION.md` for details.

## Build commands

Targeted Volume 02 build, run from `.`:

```bash
python3 \
  scripts/build_series.py --volume 02 --skip-index
```

Full series build used to propagate shared cover, author, and learning-order changes:

```bash
python3 \
  scripts/build_series.py
```

## Final PDF

- Path: `./dist/Java-SDE2-DSA-01-Time-and-Space-Complexity.pdf`
- Page count: **79**
- File size: **3,837,739 bytes**
- SHA-256: `b371cc5e3cd29a4eb09b6ecca2a655e7513cebc662cd5c9b770e4b2682048aed`
- Page size: US Letter, 612 x 792 points
- Metadata title: `Time and Space Complexity for Java Interviews`
- Metadata subject: `From First Operation Counts to SDE-2 Trade-off Reasoning`

## PDF verification

- Contents page lists all nine chapters and their start pages.
- Cover artwork, title hierarchy, code blocks, tables, headers, footers, and page numbers render correctly.
- No near-empty body pages were detected across the final 29-PDF series audit.
- Author metadata is correct, and the résumé-based author page includes LinkedIn and GitHub links.
- The series index orders Java Foundations as Learning Step 1, Complexity as Step 2, and Number Systems as Step 3 while preserving stable filenames.
- All 29 PDFs passed page-bound, title/author metadata, cover-image, résumé-bio marker, LinkedIn/GitHub, manifest hash, and extracted-text checks.

Content-affected pages rendered with Poppler and visually inspected:

`1, 3, 18, 25, 51, 59, 68, 78, 79`

The first solutions-page render exposed excess whitespace. A “How to review a solution” guide was added, the PDF was rebuilt, and page 59 was re-rendered successfully.

## Remaining deliberate gaps

- Full recurrence-solving catalogs belong with Recursion and Dynamic Programming.
- Graph-specific and tree-specific complexity patterns remain in their dedicated volumes.
- JMH, warmup, JIT, GC, and diagnostic measurement remain in Advanced Java E.
- Exact JVM object byte sizes are not asserted because layout depends on implementation and runtime configuration.
- Number Systems teaching content remains unchanged; only the shared cover, author profile, and navigation pages were regenerated.
- This historical build was produced in the independent publishing workspace. The canonical source and artifact are now included in the consolidated handbook repository under its governance and licensing files.
