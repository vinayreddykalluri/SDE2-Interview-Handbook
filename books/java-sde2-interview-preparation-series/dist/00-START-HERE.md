# Start Here: Individual PDF Reading Order

Every PDF remains an individual download. Read the books by the numbered learning steps below, not by alphabetical filename order. Stable physical volume IDs remain in filenames so existing links and releases do not break.

## Start and complete references

- [Series index](Java-SDE2-Interview-Preparation-Series-Index.pdf)
- [Complete 616-page master book](java-sde2-interview-book.pdf)

## Foundations - learning steps 1 to 5

1. [Java Foundations for Problem Solving](Java-SDE2-DSA-03-Java-Foundations-for-Problem-Solving.pdf)
2. [Time and Space Complexity for Java Interviews](Java-SDE2-DSA-02-Time-and-Space-Complexity.pdf)
3. [Number Systems and Math Foundations](Java-SDE2-DSA-01-Number-Systems-and-Math-Foundations.pdf)
4. [Number Systems Interview Workbook](Java-SDE2-DSA-01B-Number-Systems-Interview-Workbook.pdf)
5. [Bit Manipulation in Java](Java-SDE2-DSA-04-Bit-Manipulation-in-Java.pdf)
6. [Loop Mastery, Patterns, and Index Calculations](Java-SDE2-DSA-05-Loop-Mastery-and-Index-Calculations.pdf)

## Core DSA - learning steps 6 to 15

7. [Arrays and Array Problem-Solving Patterns](Java-SDE2-DSA-06-Arrays-and-Array-Patterns.pdf)
8. [Strings and String Problem-Solving Patterns](Java-SDE2-DSA-07-Strings-and-String-Patterns.pdf)
9. [Hashing: Maps, Sets, Frequency, and Prefix State](Java-SDE2-DSA-08-Hashing-Maps-Sets-and-Prefix-State.pdf)
10. [Recursion and Backtracking in Java](Java-SDE2-DSA-09-Recursion-and-Backtracking.pdf)
11. [Linked Lists: Pointer Reasoning and Mutation](Java-SDE2-DSA-10-Linked-Lists.pdf)
12. [Stacks, Queues, Deques, and Monotonic Patterns](Java-SDE2-DSA-11-Stacks-Queues-Deques-and-Monotonic-Patterns.pdf)
13. [Binary Search: Bounds, Answers, and Invariants](Java-SDE2-DSA-12-Binary-Search.pdf)
14. [Trees, Binary Search Trees, and Tries](Java-SDE2-DSA-13-Trees-BSTs-and-Tries.pdf)
15. [Heaps, Priority Queues, Selection, and Top-K](Java-SDE2-DSA-14-Heaps-Priority-Queues-and-Top-K.pdf)
16. [Graphs: Traversal, Ordering, Paths, and Union-Find](Java-SDE2-DSA-15-Graphs.pdf)

## Algorithm strategies - learning steps 16 and 17

17. [Greedy Algorithms: Recognition, Proofs, and Scheduling](Java-SDE2-DSA-16-Greedy-Algorithms.pdf)
18. [Dynamic Programming: State, Transitions, and Optimization](Java-SDE2-DSA-17-Dynamic-Programming.pdf)

## Advanced Java and backend engineering - learning step 18

19. [Advanced Java A: JVM and Execution](Java-SDE2-ADV-18A-JVM-and-Execution.pdf)
20. [Advanced Java B: Language Design](Java-SDE2-ADV-18B-Language-OOP-and-Modern-Java.pdf)
21. [Advanced Java C: Collections, Generics, and Functional Java](Java-SDE2-ADV-18C-Collections-Streams-and-IO.pdf)
22. [Advanced Java D: Concurrency](Java-SDE2-ADV-18D-Concurrency-and-Memory-Model.pdf)
23. [Advanced Java E: Performance and Diagnostics](Java-SDE2-ADV-18E-Performance-Diagnostics-and-GC.pdf)
24. [Advanced Java F: Design, Backend, Testing, and Security](Java-SDE2-ADV-18F-Design-Backend-Testing-and-Security.pdf)
25. [Advanced Java G: Question Bank, Study Plan, and Reference](Java-SDE2-ADV-18G-Question-Bank-Study-Plan-and-Reference.pdf)
26. [Advanced Java H: Spring Boot and REST APIs](Java-SDE2-ADV-18H-Spring-Boot-and-REST.pdf)
27. [Advanced Java I: Persistence, SQL, JPA, and Caching](Java-SDE2-ADV-18I-Persistence-SQL-and-Caching.pdf)
28. [Advanced Java J: Distributed Systems and System Design](Java-SDE2-ADV-18J-Distributed-Systems-and-System-Design.pdf)

## Prefer real folders locally?

From the book workspace, run:

```bash
python3 scripts/organize_pdf_library.py
```

This creates `output/reader-library/` with step-prefixed PDFs grouped under foundations, core DSA, algorithm strategies, and advanced Java. Canonical `dist/` files remain unchanged.
