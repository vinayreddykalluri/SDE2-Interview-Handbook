# Focused Series Build Report

## Release summary

- Author: Vinay Reddy Kalluri
- Public stages: 18
- Physical topic PDFs: 28
- Series index PDFs: 1
- Total release PDFs: 29
- Topic pages: 1,823
- Index pages: 13
- Total release pages: 1,836
- Umbrella master PDF pages: 616
- Total pages represented across the release and 616-page master: 2,452
- Page format: US Letter
- Java baseline: Java 21
- Release date: 2026-07-25

Stable volume numbers are preserved, while the recommended path now begins with Java Foundations (03), continues to Time and Space Complexity (02), then uses both Number Systems PDFs (01/01B). The advanced step is split into Parts A-J, with Parts H-J providing the Spring/backend, persistence, and distributed-systems specialist track.

## Artifact inventory

| ID | PDF | Pages |
|---|---|---:|
| 01 | `Java-SDE2-DSA-01-Number-Systems-and-Math-Foundations.pdf` | 125 |
| 01B | `Java-SDE2-DSA-01B-Number-Systems-Interview-Workbook.pdf` | 137 |
| 02 | `Java-SDE2-DSA-02-Time-and-Space-Complexity.pdf` | 79 |
| 03 | `Java-SDE2-DSA-03-Java-Foundations-for-Problem-Solving.pdf` | 198 |
| 04 | `Java-SDE2-DSA-04-Bit-Manipulation-in-Java.pdf` | 109 |
| 05 | `Java-SDE2-DSA-05-Loop-Mastery-and-Index-Calculations.pdf` | 95 |
| 06 | `Java-SDE2-DSA-06-Arrays-and-Array-Patterns.pdf` | 40 |
| 07 | `Java-SDE2-DSA-07-Strings-and-String-Patterns.pdf` | 32 |
| 08 | `Java-SDE2-DSA-08-Hashing-Maps-Sets-and-Prefix-State.pdf` | 43 |
| 09 | `Java-SDE2-DSA-09-Recursion-and-Backtracking.pdf` | 23 |
| 10 | `Java-SDE2-DSA-10-Linked-Lists.pdf` | 24 |
| 11 | `Java-SDE2-DSA-11-Stacks-Queues-Deques-and-Monotonic-Patterns.pdf` | 23 |
| 12 | `Java-SDE2-DSA-12-Binary-Search.pdf` | 22 |
| 13 | `Java-SDE2-DSA-13-Trees-BSTs-and-Tries.pdf` | 25 |
| 14 | `Java-SDE2-DSA-14-Heaps-Priority-Queues-and-Top-K.pdf` | 24 |
| 15 | `Java-SDE2-DSA-15-Graphs.pdf` | 32 |
| 16 | `Java-SDE2-DSA-16-Greedy-Algorithms.pdf` | 27 |
| 17 | `Java-SDE2-DSA-17-Dynamic-Programming.pdf` | 31 |
| 18A | `Java-SDE2-ADV-18A-JVM-and-Execution.pdf` | 84 |
| 18B | `Java-SDE2-ADV-18B-Language-OOP-and-Modern-Java.pdf` | 93 |
| 18C | `Java-SDE2-ADV-18C-Collections-Streams-and-IO.pdf` | 95 |
| 18D | `Java-SDE2-ADV-18D-Concurrency-and-Memory-Model.pdf` | 69 |
| 18E | `Java-SDE2-ADV-18E-Performance-Diagnostics-and-GC.pdf` | 47 |
| 18F | `Java-SDE2-ADV-18F-Design-Backend-Testing-and-Security.pdf` | 49 |
| 18G | `Java-SDE2-ADV-18G-Question-Bank-Study-Plan-and-Reference.pdf` | 95 |
| 18H | `Java-SDE2-ADV-18H-Spring-Boot-and-REST.pdf` | 67 |
| 18I | `Java-SDE2-ADV-18I-Persistence-SQL-and-Caching.pdf` | 64 |
| 18J | `Java-SDE2-ADV-18J-Distributed-Systems-and-System-Design.pdf` | 71 |
| INDEX | `Java-SDE2-Interview-Preparation-Series-Index.pdf` | 13 |

Exact byte counts and SHA-256 hashes are recorded in `dist/manifest.json`.

## Navigation and presentation

Every topic PDF includes:

- a modern navy, teal, gold, and off-white cover with a strict uninterrupted central text-safe field and decoration confined to side margins and the bottom edge;
- a one-page `Start Here` readiness gate with prerequisites, recognition signals, an exit target, and a first action;
- local previous/current/next navigation and a page-numbered contents page before the learning material;
- core learning beginning on page 4 in every focused topic PDF;
- Vinay Reddy Kalluri as author on the cover and in PDF metadata;
- a selective résumé-based About the Author page featuring production-scale Java/Kafka work, reliability engineering, dual master's degrees and gold medal, independent products, clickable LinkedIn/GitHub links, and publishing notes;
- the complete 18-step roadmap with Java Foundations first, Complexity second, Number Systems third, and current-stage highlighting;
- current-section running headers, footers, verified page numbers, PDF bookmarks, and sibling-file links;
- Charter body text, Avenir Next headings, Menlo code, accessible semantic colors, and restored bold/italic emphasis;
- protected short tables, whole-row long tables with repeated headers, heading/lead-in/payload grouping, and code labels bound to balanced continuation panels;
- semantic reading cues: `CHOOSE IT`, `WHY IT WORKS`, `TRACE IT`, `WATCH OUT`, `TRY IT`, and `RECAP`;
- prerequisite, recognition-signal, outcome, practice, and SDE-2 follow-up guidance;
- previous/next navigation with printed filename fallbacks;
- exercises, checkpoints, and a completion handoff.

## Validation

The current release passed the following enhancement-pass checks:

- The unchanged full-series publisher generated 28 topic PDFs and the index successfully.
- The final full-release structural audit checked all 29 PDFs and 1,836 pages for page bounds, metadata, content markers, Editor-in-Chief and Chief Auditor credit, CC BY 4.0 and MIT licensing references, LinkedIn/GitHub annotations, manifest hashes, and near-empty body pages; every check passed.
- Java Fundamentals compiled with `javac -Xlint:all -Werror` and printed `PASS 70 Java Fundamentals examples`.
- Time and Space Complexity compiled with `javac -Xlint:all -Werror` and printed `PASS 24 complexity examples`.
- Loop Mastery compiled with `javac --release 21 -Xlint:all -Werror` and printed `PASS 40 Loop Mastery checks`.
- Poppler renders were visually inspected for the series index, Java Fundamentals, Complexity, Bit Manipulation, Arrays, Graphs, and Advanced Java J, covering short and long titles, contents, roadmap, teaching, collections, practice, solutions, companion code, author pages, and publishing notes.
- The first Complexity solutions render exposed excess whitespace; reader guidance was added, Volume 02 was rebuilt, and the affected page was re-rendered successfully.

Focused evidence is recorded in the Java Fundamentals, Time/Space, Bit Manipulation, and Loop Mastery audit, coverage, validation, changelog, and build reports. Machine-readable release metadata is in `dist/manifest.json`.

## Publishing model

The books retain an independent publishing workspace inside the consolidated `SDE2-Interview-Handbook` repository. Canonical source, code, diagrams, PDFs, reports, and the artifact manifest are versioned under `books/java-sde2-interview-preparation-series/`; regenerable build and visual-QA workspaces are excluded. The build commands themselves never stage, commit, push, merge, or open pull requests.
