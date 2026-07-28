# Strings and String Patterns Build Report

## Outcome

Volume 07 was rebuilt successfully as a 106-page focused PDF. The enhancement preserved the existing ReportLab publishing pipeline, stable output filename, modern series cover, fonts, margins, navigation, metadata, licensing, and expanded author profile. Work concentrated on prerequisite-first content, diagrams, practice, solutions, executable validation, and reader-facing PDF organization.

## Canonical sources

- Manifest/specification: `publishing/series.json`, volume `07`
- Focused chapters: `content/volumes/07-strings-and-string-patterns/chapters/01-*.md` through `07-*.md`
- Practice: `content/volumes/07-strings-and-string-patterns/exercises/01-strings-practice-lab.md`
- Solutions: `content/volumes/07-strings-and-string-patterns/solutions/01-strings-practice-solutions.md`
- Companion: `content/volumes/07-strings-and-string-patterns/code/StringPatternsExamples.java`
- Figures: ten PNGs in `content/volumes/07-strings-and-string-patterns/assets/`
- Figure generator: `scripts/generate_string_diagrams.py`
- Reader order: `dist/00-START-HERE.md`
- Grouped-library generator: `scripts/organize_pdf_library.py`

The former `chapters/01-sde2-string-patterns.md` was replaced, not retained as a duplicate. The two mapped master selections were removed from this focused volume because the new native chapters teach String and charset behavior in the correct prerequisite sequence. Their canonical master sources remain unchanged for the master book.

## Content changes

| Measure | Result |
|---|---:|
| Previous PDF pages | 32 |
| Final PDF pages | 106 |
| Native teaching chapters | 7 |
| Practice/solution chapters | 2 |
| Executable companion chapters | 1 |
| Focused native Markdown files | 9 |
| Focused native words | 15,657 |
| Focused Java code fences | 81 |
| Reproducible diagrams | 10 |
| Standalone companion checks | 50 |
| Seeded differential search cases | 2,000 |
| Dedicated lab items | 78 |

Substantially rewritten or expanded topics include references and immutability, literals and pooling, equality and ordering, null/empty/blank, core String APIs, UTF-16 and code points, grapheme boundaries, normalization and locale, explicit charsets, StringBuilder, delimiter construction, regex split, strict overflow-safe parsing, grammar validation, palindrome variants, anagram baselines and signatures, longest common prefix, run encoding, fixed windows, longest unique substring, at-most/exactly-K state, anagram windows, minimum cover, replacement budgets, naive search, LPS derivation, KMP first/all matches, rolling hash, Z intuition, testing, complexity, and production text boundaries.

Accuracy hardening includes:

- `==` compares String identity; `.equals()` compares value.
- String literals may share pooled instances, but equal values do not universally share identity.
- String parameters still demonstrate Java pass-by-value.
- `length()` and String indexes use UTF-16 units, not visible-character counts.
- Code-point positions, byte offsets, grapheme positions, and UTF-16 indexes are not interchangeable.
- Case, locale, normalization, and accepted alphabet are explicit contracts.
- Repeated growing concatenation is separated from one readable concatenation expression.
- `split` is treated as regex and trailing-empty behavior is documented.
- Integer overflow is detected before multiplication/subtraction.
- Hash-map costs are qualified as expected and rolling-hash candidates are verified.
- KMP preserves overlapping matches and never rewinds the text index.
- `String.indexOf` is not assigned an undocumented algorithmic guarantee.
- Complexity includes preprocessing, normalization/conversion, output, and allocation where relevant.

## Practice inventory

The separated lab contains 24 knowledge checks, 12 predicted-output exercises, 12 debugging repairs, 18 focused coding tasks, 8 interview follow-ups, 3 cumulative assessments, and 1 final readiness assessment: 78 items total. Each teaching chapter also ends with local checks and Foundation, Interview Core, or SDE-2 Follow-up practice. Solutions explain contracts, text units, invariants, boundaries, numeric width, ownership, and trade-offs rather than only presenting code.

## Code validation

Compilation:

```bash
javac --release 21 -Xlint:all -Werror \
  content/volumes/07-strings-and-string-patterns/code/StringPatternsExamples.java
```

Execution output:

```text
PASS 50 Strings checks
```

Repository source validation:

```bash
python3 scripts/validate_series.py --source-only
```

Observed result: 150 unique mapped Markdown files, 18 Number Systems chapters, 17 Number Systems diagrams, 24 standalone Number Systems blocks, and 19 declared series-native Java classes validated successfully. Strings had zero companion compilation failures and zero output mismatches.

## PDF build and validation

Exact targeted build command, run from `books/java-sde2-interview-preparation-series/`:

```bash
python3 scripts/build_series.py --volume 07 --skip-index
```

Final artifact:

`dist/Java-SDE2-DSA-07-Strings-and-String-Patterns.pdf`

| Property | Final value |
|---|---|
| Pages | 106 |
| File size | 4,961,504 bytes |
| SHA-256 | `b0921117ba71bb1286855a40d94cb1fcac412a99ffa90c99fb59fc5c63dcfb97` |
| Metadata title | Strings and String Problem-Solving Patterns |
| Metadata author | Vinay Reddy Kalluri |
| Link annotations | 71 |
| LinkedIn annotations | present |
| GitHub annotations | present |
| Semantic pagination | 0 errors, 0 warnings |

Semantic QA command:

```bash
python3 scripts/qa_semantic_layout.py --include 07
```

The rebuilt PDF contains all seven teaching chapter titles, the practice lab, the solution guide, the complete executable companion, and the author profile. The manifest records the updated byte count, page count, and SHA-256 digest.

## Rendered-page inspection

Poppler rendered every page of the latest 106-page PDF at 120 DPI. Six contact sheets covering pages 1-106 and full-page renders for pages 1, 3, 5, 9, 13, 22, 30, 33, 41, 47, 53, 61, 70, 77, 84, and 105 were visually inspected.

The inspected pages cover:

- the modern cover and uninterrupted text-safe field;
- Start Here, contents, and learning-step navigation;
- all ten focused diagrams;
- every teaching chapter opening;
- dry-run and complexity tables;
- short and long Java code blocks;
- practice and solution chapter openings;
- the 19-panel executable companion; and
- the expanded About the Author page and profile links.

Observed result: no clipped text, overlapping cover content, split-row table defect, broken diagram, unreadable code, orphan heading, footer collision, near-empty generated page, or unexpected blank page. The automated render report recorded zero blank, edge-touching, or unusually dark pages for Volume 07.

## PDF organization and website alignment

The canonical 30 PDF files remain in `dist/` under stable names. `dist/00-START-HERE.md` now groups every individual PDF in prerequisite order. The command below validated all 28 focused assignments and both reference PDFs, then generated 30 grouped, step-prefixed copies under the ignored local output directory:

```bash
python3 scripts/organize_pdf_library.py
python3 scripts/organize_pdf_library.py --check
```

Observed organization result:

```text
PASS 28 focused PDFs assigned once in learning order
PASS 2 reference PDFs present
```

The generated folders are `00-start-here`, `01-foundations`, `02-core-dsa`, `03-algorithm-strategies`, and `04-advanced-java-backend`. This provides physical local organization without duplicating committed binaries or breaking release links.

`tooling/automation/sync_book_catalog.py` updated the portal catalog so Volume 07 reports 106 pages. The complete library now reports 28 focused books, 1,950 focused pages, a 13-page index, a 616-page master, 30 PDFs, and 2,579 reviewed pages. The repository README, book README, web books page, portal summary, comprehensive audit, series report, and web validation constant were aligned.

## Remaining warnings and boundaries

- No blocking warning remains for Volume 07.
- The PDF is intentionally untagged because the preserved builder does not currently emit tagged PDF structure.
- Full regex-engine behavior, locale collation, grapheme segmentation, tries, suffix structures, approximate matching, edit-distance DP, streaming decoder internals, and search-system design remain in dedicated or later volumes.
- The generated grouped PDF library is intentionally ignored; `dist/` remains the single committed artifact store.
- Company-specific timed question sets should be added separately rather than expanding this prerequisite book indiscriminately.
