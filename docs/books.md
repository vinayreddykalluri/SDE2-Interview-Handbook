# Published Java SDE-2 Book Series

The repository includes a free, open-source series of individually navigable Java and DSA books for SDE-2 interview preparation. The books use a beginner-first route and include runnable Java 21 examples, diagrams, dry runs, exercises, solutions, interview traps, and technical validation reports.

[Download the latest book release](https://github.com/vinayreddykalluri/SDE2-Interview-Handbook/releases/latest){ .md-button .md-button--primary }
[Read the complete web book library](https://vinayreddykalluri.github.io/SDE2-Interview-Handbook/books/){ .md-button }
[Open the interactive web + PDF library](https://vinayreddykalluri.github.io/SDE2-Interview-Handbook/#books){ .md-button }
[Browse the canonical book sources](https://github.com/vinayreddykalluri/SDE2-Interview-Handbook/tree/master/books/java-sde2-interview-preparation-series){ .md-button }
[Open the individual-PDF reading order](https://github.com/vinayreddykalluri/SDE2-Interview-Handbook/blob/master/books/java-sde2-interview-preparation-series/dist/00-START-HERE.md){ .md-button }

## Choose the right reading surface

| Need | Use | Why |
|---|---|---|
| Study a complete book online | Full web book | Every canonical chapter, exercise, solution, searchable contents, and per-book code index |
| Learn or revise quickly | Quick lessons | Concise navigation, copyable code, cross-links, and a guided module path |
| Study a topic to publication depth | Focused PDF | Full explanations, diagrams, dry runs, exercises, solutions, and offline reading |
| Correct or extend the material | Canonical Markdown | Review the exact editable source and submit a focused contribution |

The interactive library uses the publishing manifest for segment order and release links, then reads the canonical Markdown and declared Java companions for routes, chapter previews, document counts, word counts, and code counts. The build renders all 275 current source documents into 40 searchable web books. A newly added canonical chapter therefore reaches the catalog and full reader through `make sync-book-catalog` and `make build-site`; the website does not maintain a hand-written shadow copy of book prose.

## Choose a segment

Do not combine every subject into one overwhelming route. Select the interview skill you need now, start with that segment's Book 01, and continue in order.

| Segment | Start | Books | What it develops |
|---|---|---:|---|
| Java Engineering | [JAVA 01 - Java Foundations](https://vinayreddykalluri.github.io/SDE2-Interview-Handbook/books/01-java-foundations-for-problem-solving/) | 9 | Java language, Git, Maven/Gradle, core libraries, JVM, concurrency, performance, and revision |
| Data Structures and Algorithms | [DSA 01 - Time and Space Complexity](https://vinayreddykalluri.github.io/SDE2-Interview-Handbook/books/02-time-and-space-complexity/) | 17 | Complexity, number systems, bit operations, implementation patterns, data structures, algorithms, and problem solving |
| System Design and Backend | [SD 01 - Backend and Design Foundations](https://vinayreddykalluri.github.io/SDE2-Interview-Handbook/books/18f-design-backend-testing-and-security/) | 14 | MySQL, Hibernate/JPA, Spring, MongoDB, Redis, Kafka, AI integration, persistence, and distributed-system design |

Books marked **Roadmap edition** are real downloadable web/PDF artifacts that define scope, prerequisites, chapter order, interview outcomes, and the completion gate. They do not claim publication-depth coverage yet; their full chapters, examples, exercises, and validation will be added one book at a time.

Stable technical IDs remain in existing filenames and URLs. Reader-facing segment codes—JAVA 01-09, DSA 01-17, and SD 01-14—appear on the web, PDF covers, index, and generated folders. Contributors can create the segment-organized local library with `scripts/organize_pdf_library.py`.

## What is published

- 40 focused books (2,581 pages) and one 17-page series index
- one complete 616-page master book
- 42 individual PDFs and 3,214 pages across the complete library
- canonical Markdown and diagram sources
- Java 21 companion programs and boundary tests
- exercises with separated solutions
- content audits, coverage matrices, change logs, validation reports, and build reports
- a SHA-256 artifact manifest

SD 04 - Spring Framework is now a 122-page publication edition with 21 canonical web chapters, 24 answered interview rounds, 170 structured practice/debug tasks, a Java 21 reasoning companion, and six real Spring Framework 7 behavior tests.

## Editorial leadership and contribution

Vinay Reddy Kalluri is the project creator, founding author, Editor-in-Chief, and Chief Auditor. Accepted contributors receive individual credit through the repository author registry, Git history, and pull requests.

Use the repository's content-improvement issue form to report a confusing explanation or technical defect. See the contributor guide before proposing a large new chapter or changing the learning path.

To help expand the remaining concise DSA volumes, open the [book contribution backlog](community/book-contribution-backlog.md). Strings is now the publication-depth reference implementation; scoped issues for Hashing through Dynamic Programming include canonical paths, acceptance criteria, and validation commands. Partial contributions are welcome.

Book prose, exercises, diagrams, and PDFs use CC BY 4.0. Source code and publishing tools use MIT.
