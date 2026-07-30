# Spring Boot Scenario Validation

## Validation scope

| Asset | Count / result |
|---|---:|
| canonical Markdown chapters | 23 |
| source words before publisher normalization | 15,824 |
| fenced teaching blocks | 85 |
| dependency-free Java companion | 1 |
| companion source lines | 357 |
| Maven fixture Java files | 1 |
| Maven fixture source lines | 133 |
| real Spring Boot tests | 6 |
| realistic interview rounds | 28 |
| cumulative assessments | 5 |
| debugging exercises in final bank | 15 |

## Command

```bash
bash content/volumes/SD05-spring-boot/labs/validate_spring_boot_labs.sh
```

## Dependency-free companion

`SpringBootInterviewCompanion.java` compiled under Java 21 with `-Xlint:all -Werror`, ran with assertions enabled, and passed. It validates:

- property precedence while retaining winning origin;
- class/property conditions and user-bean back-off;
- valid and invalid availability transitions;
- child operation budgets within an end-to-end deadline;
- new, in-progress, replay, and conflict idempotency outcomes.

Compilation failures: 0. Compiler warnings: 0. Assertion failures: 0. Output mismatches: 0.

## Real Spring Boot fixture

The Maven fixture resolved Spring Boot 4.1.0 and executed six JUnit tests:

1. command-line value overrides default configuration in a real non-web `SpringApplication`;
2. successful startup reports `CORRECT` liveness and `ACCEPTING_TRAFFIC` readiness;
3. matching auto-configuration creates the default client;
4. a feature property disables the default;
5. a user bean makes the missing-bean default back off;
6. invalid typed properties fail startup and the condition report records the auto-configuration source.

Tests passed: 6. Tests failed: 0. Test errors: 0. Skipped: 0. Maven compilation failures: 0. Compiler warnings: 0.

## Version basis

- Book/example Java target: 21.
- Spring Boot: 4.1.0, the current stable documentation line at audit time.
- Spring Boot 4.1 minimum Java: 17; compatibility is documented through Java 26.
- Spring Framework baseline: 7.0.8 or later.

## Boundary statement

The fixture proves Boot assembly and configuration mechanics. Target MySQL locking/SQL, JPA provider behavior, servlet load, security-provider integration, container resource limits, and native-image behavior require their own target environments and remain cross-book or deployment validation boundaries.

## Series and publication validation

- Full series validation discovered 286 unique mapped Markdown sources and compiled and ran 44 series-native Java classes.
- The Spring Boot-specific validator compiled the companion and passed all six Boot integration tests.
- Full artifact validation passed across 41 focused/index PDFs and 2,701 pages.
- The publisher generated a 113-page PDF with the expected local table of contents, bookmarks, and sibling links.
- The web catalog exposes 23 Spring Boot documents, 15,374 words, and 37 code entries at `books/19f-spring-boot/`.
- Semantic layout QA reported zero errors. Its single warning is the intentional repeated series-roadmap table header on pages 110-111.
- Every PDF page was rendered and reviewed through six contact sheets. Pages 99 and 110-113 received individual high-resolution inspection.
- Blank-page candidates: 0. Edge-clipping candidates: 0. Dark-page candidates: 0. Output mismatches: 0.

Repository validation passed layout, structure, links, 81 Java examples plus one smoke-test source, web, and deployment checks. The first run hit macOS iCloud placeholder read timeouts in unrelated existing examples; after materialization, the complete command passed unchanged. Exact artifact measurements and the resolved environmental warning are recorded in `SPRING_BOOT_BUILD_REPORT.md`.
