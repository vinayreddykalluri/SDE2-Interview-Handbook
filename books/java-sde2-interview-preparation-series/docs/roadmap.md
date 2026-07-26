# Java SDE-2 Interview Preparation Series Roadmap

## Purpose

This series turns the comprehensive Java master guide into a sequence that is easier to finish, print, revise, and revisit. Stable volume numbers identify files; the recommended learning steps now begin with Java Fundamentals, continue to Time and Space Complexity, and then establish Number Systems before the DSA pattern books.

The learning roadmap has 18 steps. Number Systems is packaged as a core foundation plus an interview workbook, and the advanced step is packaged as ten physical PDFs so the material remains focused, including three backend specialist volumes. The complete release therefore contains 28 topic PDFs plus one series-index PDF.

## Ordered learning path

| Learning step | Focus | Stable physical PDF |
|---:|---|---|
| 1 | Java Foundations for Problem Solving | `Java-SDE2-DSA-03-Java-Foundations-for-Problem-Solving.pdf` |
| 2 | Time and Space Complexity | `Java-SDE2-DSA-02-Time-and-Space-Complexity.pdf` |
| 3A | Number Systems and Math Foundations for DSA Interviews | `Java-SDE2-DSA-01-Number-Systems-and-Math-Foundations.pdf` |
| 3B | Number Systems Interview Patterns and Rapid Revision | `Java-SDE2-DSA-01B-Number-Systems-Interview-Workbook.pdf` |
| 4 | Bit Manipulation in Java | `Java-SDE2-DSA-04-Bit-Manipulation-in-Java.pdf` |
| 5 | Loop Mastery, Patterns, and Index Calculations | `Java-SDE2-DSA-05-Loop-Mastery-and-Index-Calculations.pdf` |
| 6 | Arrays and Array Problem-Solving Patterns | `Java-SDE2-DSA-06-Arrays-and-Array-Patterns.pdf` |
| 7 | Strings and String Problem-Solving Patterns | `Java-SDE2-DSA-07-Strings-and-String-Patterns.pdf` |
| 8 | Hashing: Maps, Sets, Frequency, and Prefix State | `Java-SDE2-DSA-08-Hashing-Maps-Sets-and-Prefix-State.pdf` |
| 9 | Recursion and Backtracking | `Java-SDE2-DSA-09-Recursion-and-Backtracking.pdf` |
| 10 | Linked Lists | `Java-SDE2-DSA-10-Linked-Lists.pdf` |
| 11 | Stacks, Queues, Deques, and Monotonic Patterns | `Java-SDE2-DSA-11-Stacks-Queues-Deques-and-Monotonic-Patterns.pdf` |
| 12 | Binary Search | `Java-SDE2-DSA-12-Binary-Search.pdf` |
| 13 | Trees, Binary Search Trees, and Tries | `Java-SDE2-DSA-13-Trees-BSTs-and-Tries.pdf` |
| 14 | Heaps, Priority Queues, Selection, and Top-K | `Java-SDE2-DSA-14-Heaps-Priority-Queues-and-Top-K.pdf` |
| 15 | Graphs | `Java-SDE2-DSA-15-Graphs.pdf` |
| 16 | Greedy Algorithms | `Java-SDE2-DSA-16-Greedy-Algorithms.pdf` |
| 17 | Dynamic Programming | `Java-SDE2-DSA-17-Dynamic-Programming.pdf` |
| 18A | JVM and Execution | `Java-SDE2-ADV-18A-JVM-and-Execution.pdf` |
| 18B | Language, OOP, and Modern Java | `Java-SDE2-ADV-18B-Language-OOP-and-Modern-Java.pdf` |
| 18C | Collections, Streams, and I/O | `Java-SDE2-ADV-18C-Collections-Streams-and-IO.pdf` |
| 18D | Concurrency and the Memory Model | `Java-SDE2-ADV-18D-Concurrency-and-Memory-Model.pdf` |
| 18E | Performance, Diagnostics, and GC Incidents | `Java-SDE2-ADV-18E-Performance-Diagnostics-and-GC.pdf` |
| 18F | Design, Backend, Testing, and Security | `Java-SDE2-ADV-18F-Design-Backend-Testing-and-Security.pdf` |
| 18G | Question Bank, Study Plan, and Reference | `Java-SDE2-ADV-18G-Question-Bank-Study-Plan-and-Reference.pdf` |
| 18H | Spring Boot and REST APIs | `Java-SDE2-ADV-18H-Spring-Boot-and-REST.pdf` |
| 18I | Persistence, SQL, JPA, and Caching | `Java-SDE2-ADV-18I-Persistence-SQL-and-Caching.pdf` |
| 18J | Distributed Systems and System Design | `Java-SDE2-ADV-18J-Distributed-Systems-and-System-Design.pdf` |

## How to choose a starting point

When rebuilding from basics, start with Java Foundations Volume 03, continue to Time and Space Complexity Volume 02, then complete both Number Systems PDFs (01 and 01B). The volume numbers remain stable identifiers rather than reading positions. For targeted revision, scan each volume's recognition signals and completion check, then enter at the first learning step where the answers are not yet automatic. Complete Number Systems before Bit Manipulation when signed binary, powers of two, numeric limits, or overflow still cause hesitation.

Stage 18 is a capstone rather than a prerequisite for every DSA problem. Use 18A-18D for Java-depth interviews, 18E for performance-oriented roles, 18F for framework-neutral backend design, and 18G for core mixed revision. Use 18H-18J as the backend specialist track for Spring REST services, SQL/JPA/caching, Kafka, distributed systems, resilience, observability, and system design; then revisit the 18G mock loops with those deeper cases.

## Navigation contract

- Keep the PDFs together in `dist/` so relative links can work in compatible viewers.
- Use `Java-SDE2-Interview-Preparation-Series-Index.pdf` as the durable entry point.
- Each topic PDF repeats the complete 18-step roadmap and highlights its current stable stage.
- Each topic PDF has local bookmarks and a local table of contents.
- Previous and next filenames are printed even when a viewer blocks local-file links.
- `dist/manifest.json` records page counts and SHA-256 hashes for artifact verification.

## Build

From the independent book folder:

```bash
python3 scripts/build_series.py
```

No Git operation is part of the authoring, build, validation, or release workflow for this series.
