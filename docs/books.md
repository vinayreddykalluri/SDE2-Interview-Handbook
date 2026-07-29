# Published Java SDE-2 Book Series

The repository includes a free, open-source series of individually navigable Java and DSA books for SDE-2 interview preparation. The books use a beginner-first route and include runnable Java 21 examples, diagrams, dry runs, exercises, solutions, interview traps, and technical validation reports.

[Download the latest book release](https://github.com/vinayreddykalluri/SDE2-Interview-Handbook/releases/latest){ .md-button .md-button--primary }
[Open the interactive web + PDF library](https://vinayreddykalluri.github.io/SDE2-Interview-Handbook/#books){ .md-button }
[Browse the canonical book sources](https://github.com/vinayreddykalluri/SDE2-Interview-Handbook/tree/master/books/java-sde2-interview-preparation-series){ .md-button }
[Open the individual-PDF reading order](https://github.com/vinayreddykalluri/SDE2-Interview-Handbook/blob/master/books/java-sde2-interview-preparation-series/dist/00-START-HERE.md){ .md-button }

## Choose the right reading surface

| Need | Use | Why |
|---|---|---|
| Learn or revise quickly | Web lessons | Searchable navigation, copyable code, cross-links, and a concise learning path |
| Study a topic to publication depth | Focused PDF | Full explanations, diagrams, dry runs, exercises, solutions, and offline reading |
| Correct or extend the material | Canonical Markdown | Review the exact editable source and submit a focused contribution |

The interactive library uses the publishing manifest for book order and release links, then reads the canonical Markdown sources for chapter counts and previews. A newly added book chapter therefore becomes visible in the catalog after `make sync-book-catalog`; the website does not maintain a hand-written shadow catalog.

## Start here: the foundation route

Do not jump directly into pattern-heavy DSA if Java mechanics, loop boundaries, or cost analysis are uncertain. Use the first seven steps in order.

| Step | Topic | Read on the web | Publication-depth PDF |
|---:|---|---|---|
| 1 | Java Foundations | [Start the Java track](coding-foundations/01-java-runtime/index.md) | [Download PDF](https://github.com/vinayreddykalluri/SDE2-Interview-Handbook/raw/refs/heads/master/books/java-sde2-interview-preparation-series/dist/Java-SDE2-DSA-03-Java-Foundations-for-Problem-Solving.pdf) |
| 2 | Time and Space Complexity | [Read complexity lessons](coding-foundations/02-complexity/index.md) | [Download PDF](https://github.com/vinayreddykalluri/SDE2-Interview-Handbook/raw/refs/heads/master/books/java-sde2-interview-preparation-series/dist/Java-SDE2-DSA-02-Time-and-Space-Complexity.pdf) |
| 3 | Number Systems and Math | [Read math foundations](coding-foundations/03-math/index.md) | [Main book](https://github.com/vinayreddykalluri/SDE2-Interview-Handbook/raw/refs/heads/master/books/java-sde2-interview-preparation-series/dist/Java-SDE2-DSA-01-Number-Systems-and-Math-Foundations.pdf) · [Workbook](https://github.com/vinayreddykalluri/SDE2-Interview-Handbook/raw/refs/heads/master/books/java-sde2-interview-preparation-series/dist/Java-SDE2-DSA-01B-Number-Systems-Interview-Workbook.pdf) |
| 4 | Bit Manipulation | [Read bit lessons](coding-foundations/06-bit-manipulation/index.md) | [Download PDF](https://github.com/vinayreddykalluri/SDE2-Interview-Handbook/raw/refs/heads/master/books/java-sde2-interview-preparation-series/dist/Java-SDE2-DSA-04-Bit-Manipulation-in-Java.pdf) |
| 5 | Loops and Index Calculations | [Loop reasoning](coding-foundations/04-loop-reasoning/index.md) · [Index safety](coding-foundations/05-indexing/index.md) | [Download PDF](https://github.com/vinayreddykalluri/SDE2-Interview-Handbook/raw/refs/heads/master/books/java-sde2-interview-preparation-series/dist/Java-SDE2-DSA-05-Loop-Mastery-and-Index-Calculations.pdf) |
| 6 | Arrays and Array Patterns | [Read array lessons](coding-foundations/07-arrays/index.md) | [Download PDF](https://github.com/vinayreddykalluri/SDE2-Interview-Handbook/raw/refs/heads/master/books/java-sde2-interview-preparation-series/dist/Java-SDE2-DSA-06-Arrays-and-Array-Patterns.pdf) |
| 7 | Strings and String Patterns | [Read string lessons](coding-foundations/08-strings/index.md) | [Download PDF](https://github.com/vinayreddykalluri/SDE2-Interview-Handbook/raw/refs/heads/master/books/java-sde2-interview-preparation-series/dist/Java-SDE2-DSA-07-Strings-and-String-Patterns.pdf) |

After step seven, use the interactive library's DSA filter to continue with Hashing, Recursion, Linked Lists, Stacks and Queues, Binary Search, Trees, Heaps, Graphs, Greedy Algorithms, and Dynamic Programming.

## Recommended order

1. Java Foundations for Problem Solving
2. Time and Space Complexity for Java Interviews
3. Number Systems and Math Foundations, then its interview workbook
4. Bit Manipulation in Java
5. Loop Mastery, Patterns, and Index Calculations
6. Arrays and Array Problem-Solving Patterns
7. Strings and String Problem-Solving Patterns
8. Hashing, Recursion, Linked Lists, Stacks/Queues, Binary Search, Trees, Heaps, and Graphs
9. Greedy Algorithms and Dynamic Programming
10. Advanced Java and backend engineering volumes A-J

Stable volume numbers remain in filenames, while the books' internal learning-step labels show the recommended route. The reader index groups every individual PDF into Foundations, Core DSA, Algorithm Strategies, and Advanced Java/Backend sections. Contributors can also generate actual step-prefixed local folders with `scripts/organize_pdf_library.py`.

## What is published

- 28 focused topic books and one series index, totaling 1,963 pages
- one complete 616-page master book
- 30 individual PDFs and 2,579 reviewed pages across the complete library
- canonical Markdown and diagram sources
- Java 21 companion programs and boundary tests
- exercises with separated solutions
- content audits, coverage matrices, change logs, validation reports, and build reports
- a SHA-256 artifact manifest

## Editorial leadership and contribution

Vinay Reddy Kalluri is the project creator, founding author, Editor-in-Chief, and Chief Auditor. Accepted contributors receive individual credit through the repository author registry, Git history, and pull requests.

Use the repository's content-improvement issue form to report a confusing explanation or technical defect. See the contributor guide before proposing a large new chapter or changing the learning path.

To help expand the remaining concise DSA volumes, open the [book contribution backlog](community/book-contribution-backlog.md). Strings is now the publication-depth reference implementation; scoped issues for Hashing through Dynamic Programming include canonical paths, acceptance criteria, and validation commands. Partial contributions are welcome.

Book prose, exercises, diagrams, and PDFs use CC BY 4.0. Source code and publishing tools use MIT.
