# Java Book Workspaces

This directory contains the volume-specific source, exercises, labs, solutions, and executable companions for the public Java learning path.

| Code | Workspace | Focus |
|---|---|---|
| JAVA 01 | `JAVA-01-java-foundations-for-problem-solving/` | Java fundamentals for DSA and interview-quality coding |
| JAVA 02 | `JAVA-02-git-and-github/` | Git and GitHub workflows for Java engineers |
| JAVA 03 | `JAVA-03-maven-and-gradle/` | Maven and Gradle build engineering |
| JAVA 04 | `JAVA-04-language-oop-and-modern-java/` | Advanced language contracts, OOP, and modern Java |
| JAVA 05 | `JAVA-05-collections-streams-and-io/` | Collections, streams, ordering, and I/O |
| JAVA 06 | `JAVA-06-jvm-and-execution/` | JVM execution, loading, runtime memory, GC, and JIT |
| JAVA 07 | `JAVA-07-concurrency-and-memory-model/` | Java Memory Model, concurrency, executors, and virtual threads |
| JAVA 08 | `JAVA-08-performance-diagnostics-and-gc-incidents/` | Performance measurement, JVM diagnostics, and GC incidents |
| JAVA 09 | `JAVA-09-question-bank-study-plan-and-reference/` | Advanced Java mock interviews, revision, and readiness |

Shared, reusable chapters remain in [`../../master/`](../../master/). The publishing manifest composes those master chapters with the volume-specific workshops and code stored here. Keeping shared material in `content/master` avoids copying the same canonical chapter into several books.

When adding material:

1. place book-specific chapters, labs, exercises, solutions, and companions in the matching `JAVA-*` workspace;
2. keep genuinely shared canonical chapters in `content/master`;
3. update publishing paths without changing established reader order accidentally;
4. compile Java companions with the repository's Java 21 validation settings before publishing.
