# Web and PDF Numbering and Navigation Audit

Date: 2026-07-29  
Scope: 28 focused web books, 28 focused PDFs, the series-index PDF, and the complete master PDF

## Outcome

The curriculum now has one reader-facing numbering contract everywhere:

`01, 02, 03A, 03B, 04-17, 18A-18J`

`Study Step` identifies the curriculum position. `Book n of 28` identifies physical progress. Local web chapters use `StudyStep.Chapter`, such as `01.01` and `03A.07`.

Stable PDF filenames retain their historical technical IDs so repository links, release assets, and external bookmarks do not break. Those IDs are no longer presented as the learning order. Legacy web routes for Java Foundations and the two Number Systems books redirect to their canonical Study Step routes.

## Findings before correction

| Surface | Previous behavior | Reader impact | Resolution |
|---|---|---|---|
| PDF covers | Java Foundations said Learning Step 1 but Volume 3 of 18 | The first book appeared to be both 1 and 3 | Cover now says Study Step 01 of 18 |
| Number Systems covers | Study Step 3 appeared as Volume 1 / 01B | The route looked as if it moved backward | Books now use 03A and 03B |
| Advanced covers | Ten books all appeared as Volume 18 | Their internal order was ambiguous | Books now use 18A through 18J |
| PDF roadmap | Eighteen conceptual stages collapsed 28 physical books | Readers could not see every downloadable book | Roadmap now lists all 28 books on one readable page |
| PDF local navigation | Previous/current/next labels used technical IDs | The sequence contradicted the cover | Navigation now uses Study Step codes |
| Web book routes | Java began under `/books/03-.../` | The first web book looked like the third | Canonical route is `/books/01-.../` with a compatibility redirect |
| Web sidebar | Top-level ordinal, stage number, and source chapter number competed | Labels such as `01.02 · 12. Variables` appeared | Sidebar now uses Study Step plus local chapter only |
| Web chapters | No persistent book/chapter progress | Readers lacked a study-session cue | Added progress panel, study loop, and previous/next chapter controls |
| Catalog cards | Step and PDF-page labels omitted physical position | Splits such as 03A/03B were hard to interpret | Cards show Study Step plus Book n of 28 |
| Prerequisites | Metadata still said Volumes 1-8 or Stage 18B | Entry guidance reintroduced obsolete numbers | Prerequisites now use Study Step codes |
| Organized PDF folders | Files used ordinal 01-28 prefixes | A fourth independent numbering scheme appeared | Generated files now use canonical Study Step prefixes |

## Canonical order

| Book | Study Step | Title | Canonical web route | Stable PDF filename |
|---:|---:|---|---|---|
| 1 | 01 | Java Foundations for Problem Solving | `books/01-java-foundations-for-problem-solving/` | `Java-SDE2-JAVA-01-Java-Foundations-for-Problem-Solving.pdf` |
| 2 | 02 | Time and Space Complexity | `books/02-time-and-space-complexity/` | `Java-SDE2-DSA-01-Time-and-Space-Complexity.pdf` |
| 3 | 03A | Number Systems and Math Foundations | `books/03a-number-systems-and-math-foundations/` | `Java-SDE2-DSA-02-Number-Systems-and-Math-Foundations.pdf` |
| 4 | 03B | Number Systems Interview Workbook | `books/03b-number-systems-interview-workbook/` | `Java-SDE2-DSA-03-Number-Systems-Interview-Workbook.pdf` |
| 5 | 04 | Bit Manipulation | `books/04-bit-manipulation-in-java/` | `Java-SDE2-DSA-04-Bit-Manipulation-in-Java.pdf` |
| 6 | 05 | Loop Mastery and Index Calculations | `books/05-loop-mastery-and-index-calculations/` | `Java-SDE2-DSA-05-Loop-Mastery-and-Index-Calculations.pdf` |
| 7 | 06 | Arrays and Array Patterns | `books/06-arrays-and-array-patterns/` | `Java-SDE2-DSA-06-Arrays-and-Array-Patterns.pdf` |
| 8 | 07 | Strings and String Patterns | `books/07-strings-and-string-patterns/` | `Java-SDE2-DSA-07-Strings-and-String-Patterns.pdf` |
| 9 | 08 | Hashing and Prefix State | `books/08-hashing-maps-sets-and-prefix-state/` | `Java-SDE2-DSA-08-Hashing-Maps-Sets-and-Prefix-State.pdf` |
| 10 | 09 | Recursion and Backtracking | `books/09-recursion-and-backtracking/` | `Java-SDE2-DSA-09-Recursion-and-Backtracking.pdf` |
| 11 | 10 | Linked Lists | `books/10-linked-lists/` | `Java-SDE2-DSA-10-Linked-Lists.pdf` |
| 12 | 11 | Stacks, Queues, and Deques | `books/11-stacks-queues-deques-and-monotonic-patterns/` | `Java-SDE2-DSA-11-Stacks-Queues-Deques-and-Monotonic-Patterns.pdf` |
| 13 | 12 | Binary Search | `books/12-binary-search/` | `Java-SDE2-DSA-12-Binary-Search.pdf` |
| 14 | 13 | Trees, BSTs, and Tries | `books/13-trees-bsts-and-tries/` | `Java-SDE2-DSA-13-Trees-BSTs-and-Tries.pdf` |
| 15 | 14 | Heaps and Priority Queues | `books/14-heaps-priority-queues-and-top-k/` | `Java-SDE2-DSA-14-Heaps-Priority-Queues-and-Top-K.pdf` |
| 16 | 15 | Graphs | `books/15-graphs/` | `Java-SDE2-DSA-15-Graphs.pdf` |
| 17 | 16 | Greedy Algorithms | `books/16-greedy-algorithms/` | `Java-SDE2-DSA-16-Greedy-Algorithms.pdf` |
| 18 | 17 | Dynamic Programming | `books/17-dynamic-programming/` | `Java-SDE2-DSA-17-Dynamic-Programming.pdf` |
| 19 | 18A | JVM and Execution | `books/18a-jvm-and-execution/` | `Java-SDE2-JAVA-06-JVM-and-Execution.pdf` |
| 20 | 18B | Language, OOP, and Modern Java | `books/18b-language-oop-and-modern-java/` | `Java-SDE2-JAVA-04-Language-OOP-and-Modern-Java.pdf` |
| 21 | 18C | Collections, Streams, and I/O | `books/18c-collections-streams-and-io/` | `Java-SDE2-JAVA-05-Collections-Streams-and-IO.pdf` |
| 22 | 18D | Concurrency and Memory Model | `books/18d-concurrency-and-memory-model/` | `Java-SDE2-JAVA-07-Concurrency-and-Memory-Model.pdf` |
| 23 | 18E | Performance, Diagnostics, and GC | `books/18e-performance-diagnostics-and-gc-incidents/` | `Java-SDE2-JAVA-08-Performance-Diagnostics-and-GC-Incidents.pdf` |
| 24 | 18F | Design, Backend, Testing, and Security | `books/18f-design-backend-testing-and-security/` | `Java-SDE2-SD-01-Design-Backend-Testing-and-Security.pdf` |
| 25 | 18G | Question Bank and Study Plan | `books/18g-question-bank-study-plan-and-reference/` | `Java-SDE2-JAVA-09-Question-Bank-Study-Plan-and-Reference.pdf` |
| 26 | 18H | Spring Boot and REST | `books/18h-spring-boot-and-rest/` | `Java-SDE2-FW-05-Spring-Boot-and-REST.pdf` |
| 27 | 18I | Persistence, SQL, JPA, and Caching | `books/18i-persistence-sql-and-caching/` | `Java-SDE2-FW-09-Persistence-SQL-and-Caching.pdf` |
| 28 | 18J | Distributed Systems and System Design | `books/18j-distributed-systems-and-system-design/` | `Java-SDE2-SD-02-Distributed-Systems-and-System-Design.pdf` |

## Web study experience

- The portal, complete web library, sidebar, overview metadata, search entries, and book cards use the same Study Step codes.
- Each book overview shows Study Step, Book n of 28, PDF pages, web-document count, word count, and code count.
- Every chapter shows Study Step, Book n of 28, Chapter n of N, a progress bar, and the `READ · TRACE · PRACTICE · EXPLAIN` loop.
- Every chapter ends with labeled previous and next destinations.
- Reused master-book chapter numbers are removed from web headings and navigation; source filenames remain unchanged.
- The first route presents eight foundation books: 01, 02, 03A, 03B, 04, 05, 06, and 07.

## PDF audit

- All 28 focused PDFs were rebuilt from canonical sources.
- All 28 covers contain their exact public Study Step code and no `Volume n of 18` label.
- All local chapter headings are sequential and validated against their source order.
- The roadmap lists all 28 focused books and highlights the current book, not every book sharing a conceptual stage.
- The series index is 36 pages and includes a direct linked section for every focused book.
- Page size, metadata, bookmarks, sibling links, required publication text, blank-page checks, page-count bounds, hashes, and artifact-manifest records passed.
- Representative covers inspected: 01, 03A, 03B, 18J, and the series index.
- Representative roadmap pages inspected: Java Foundations page 196 and index page 34.

## Validation evidence

Commands run:

```bash
python3 books/java-sde2-interview-preparation-series/scripts/build_series.py
python3 books/java-sde2-interview-preparation-series/scripts/build_book.py
python3 books/java-sde2-interview-preparation-series/scripts/validate_series.py
python3 books/java-sde2-interview-preparation-series/scripts/organize_pdf_library.py --check
python3 tooling/automation/sync_book_catalog.py
python3 tooling/automation/build_site.py
python3 tooling/automation/validate_web.py
```

Results:

- Canonical numbering: passed for all 28 books.
- Focused Java: 19 series-native classes compiled and ran.
- Number Systems: 820 assertions passed; 24 standalone Java blocks compiled.
- Focused PDFs plus index: 29 artifacts, 1,986 pages.
- Complete master: 616 pages.
- Complete library: 30 PDFs, 2,602 pages.
- Web: 28 books, 161 canonical documents, and 860 indexed code entries.
- Broken or duplicate public Study Step codes: 0.
- PDF compilation failures: 0.
- Web validation failures: 0.

## Maintainer rule

Add or move a book only by updating `learning_order` and `path_labels` in `publishing/series.json`. Never derive public order from a stable filename ID. Validation intentionally fails when a Study Step is missing, duplicated, out of order, inconsistent with its cover, or inconsistent between the catalog and generated artifacts.
