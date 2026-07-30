# Spring Framework Scenario Validation

## Validation scope

| Asset | Count / result |
|---|---|
| canonical Markdown chapters | 21 |
| source words | 19,544 |
| catalog-indexed words after publisher normalization | 18,577 |
| fenced teaching blocks | 107 |
| Java teaching fences | 75 |
| dependency-free Java companions | 1 |
| companion source lines | 230 |
| Maven fixture Java files | 1 |
| Maven fixture Java source lines | 393 |
| real Spring tests | 6 |
| distributed chapter practice tasks | 95 |
| final practice-bank tasks | 60 |
| debugging exercises | 15 |
| cumulative assessments | 5 |
| final realistic interview scenarios | 24 |

## Commands

```bash
bash content/volumes/SD04-spring-framework/labs/validate_spring_framework_labs.sh
python3 scripts/validate_series.py --source-only
```

## Dependency-free companion

`SpringFrameworkInterviewCompanion.java` compiled under Java 21 with `-Xlint:all -Werror`, executed with assertions, and passed. It validates:

- deterministic dependency-first bean creation order;
- missing dependency and cycle rejection;
- qualifier, primary, absent, and ambiguous candidate outcomes;
- external proxy, self-invocation, JDK interface, subclass, final, and private method interception rules;
- default unchecked rollback, default checked commit, and explicit checked rollback.

Compilation failures: 0. Warnings: 0. Assertion failures: 0.

## Real Spring Framework fixture

The Maven fixture resolved Spring Framework 7.0.8 and executed six JUnit tests:

1. constructor injection and fresh prototype resolution through `ObjectProvider`;
2. initialization and destruction callbacks around context close;
3. same-thread default events and after-commit-only transactional events;
4. AOP proxy interception for external calls and bypass for self-invocation;
5. unchecked rollback versus checked commit under default rules;
6. explicit checked-exception rollback rule.

Tests passed: 6. Tests failed: 0. Test errors: 0. Maven compilation failures: 0. Compiler warnings: 0.

H2 proves local Spring JDBC transaction mechanics only. MySQL-specific isolation, lock, SQL, and execution-plan behavior require target-MySQL evidence and remain outside this fixture and book.

## Version basis

- Java 21 lab/runtime target.
- Spring Framework 7.0.8 current stable documentation line at audit time.
- Spring Framework 7 retains Java 17 as its baseline; examples avoid depending on Java 21-only framework contracts.
- JUnit Jupiter 5.11.4, AspectJ Weaver 1.9.24, and H2 2.3.232 are pinned for reproducibility.

## Series and publication validation

- Focused source validation discovered 264 unique mapped Markdown sources and compiled and ran 43 series-native Java classes.
- The Spring-specific validator compiled the companion and passed the six real Spring integration tests.
- Full series artifact validation passed across 41 focused/index PDFs and 2,598 pages.
- Repository validation passed layout, structure, links, 81 Java examples plus one smoke-test source, web, and deployment checks.
- The publisher generated a 122-page PDF with the expected table of contents and bookmarks.
- The web catalog exposes 21 Spring Framework documents, 18,577 words, and 76 code entries at `books/19e-spring-framework/`.
- Semantic layout QA reported zero errors. Its single warning is the intentional repeated series-roadmap table header on pages 119-120.
- Every PDF page was rendered and reviewed through seven contact sheets. Pages 111 and 119-122 received individual high-resolution inspection.
- Blank-page candidates: 0. Edge-clipping candidates: 0. Dark-page candidates: 0. Output mismatches: 0.

The exact artifact measurements and repository-wide totals are recorded in `SPRING_FRAMEWORK_BUILD_REPORT.md`.
