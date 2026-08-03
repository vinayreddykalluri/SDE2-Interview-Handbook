# Spring Boot Build Report

## Publication result

SD 05 is now a publication edition. The existing publisher rebuilt the canonical source into a 113-page PDF, synchronized the web catalog, and passed executable, artifact, semantic, and visual QA.

| Measure | Result |
|---|---:|
| Previous roadmap PDF | 10 pages |
| Final publication PDF | 113 pages |
| Canonical chapters | 23 |
| Catalog-indexed words | 15,374 |
| Catalog-indexed code entries | 37 |
| Final PDF size | 3,922,488 bytes |
| SHA-256 | `e5eb2a607739e388ec5484571b162535406144de942ce35b0763c5b697d8f735` |

## Canonical sources

- 23 Markdown chapters under `content/volumes/frameworks/FW-04-spring-boot/chapters/`.
- One dependency-free Java 21 companion under `content/volumes/frameworks/FW-04-spring-boot/code/`.
- One Maven Spring Boot 4.1 behavior fixture under `content/volumes/frameworks/FW-04-spring-boot/labs/`.

## Content result

- Previous edition: one roadmap chapter and a 10-page PDF.
- Chapters rewritten/expanded: roadmap replaced by 23 prerequisite-ordered chapters.
- Source words: 15,824; catalog-indexed prose words after publisher normalization: 15,374.
- Fenced teaching blocks: 85.
- Practice: chapter checks/tasks, five cumulative assessments, 10 outcome predictions, 15 debugging exercises, 20 coding/design tasks, 20 interview follow-ups, and one final readiness assessment.
- Realistic interview rounds: 28 with direct model answers.
- Accuracy corrections: Boot versus Framework; starter versus BOM/plugin; application scan root; startup versus readiness; classpath/condition/back-off; property precedence/origin; profile limitations; secret handling; DTO/entity boundary; timeout outcome; retry safety; Actuator availability/exposure/access; liveness/readiness; context-test scope; H2/target database; heap/container memory; AOT/native distinction; CORS/CSRF; evidence-first incident response.

## Existing build command

```bash
cd books/java-sde2-interview-preparation-series
python3 scripts/build_series.py --volume BOOT
```

## Validation commands

```bash
bash content/volumes/frameworks/FW-04-spring-boot/labs/validate_spring_boot_labs.sh
python3 scripts/validate_series.py --source-only
python3 scripts/validate_series.py
make PYTHON=python3 sync-book-catalog
make PYTHON=python3 build-site
make PYTHON=python3 check-book-catalog validate
```

## Executable result

- Java 21 companion: compiled warning-free and passed all assertions.
- Maven Spring Boot 4.1.0 fixture: 6 passed, 0 failed, 0 errors, 0 skipped.
- Compilation failures: 0.
- Output mismatches: 0.

## Final artifact and QA

- Final PDF: `dist/Java-SDE2-FW-04-Spring-Boot.pdf`.
- Final page count: 113.
- Final file size: 3,922,488 bytes.
- Final SHA-256: `e5eb2a607739e388ec5484571b162535406144de942ce35b0763c5b697d8f735`.
- PDF bookmarks, local table of contents, sibling-book navigation, author links, and publication notes are present.
- Web route: `books/19f-spring-boot/` with 23 study documents, 15,374 words, 37 code entries, source links, and PDF download links.
- Web-library result: 40 books, 297 canonical study documents, and 1,076 indexed code entries.
- Series result: 32 publication editions, eight roadmap editions, 2,684 focused-PDF pages, and 3,317 pages including index and master editions.
- Full artifact validation: 41 focused/index PDFs and 2,701 pages passed checksum, metadata, bookmarks, page-size, chapter-heading, and sibling-link validation.
- Focused source validation: 286 mapped Markdown sources; 44 series-native Java classes compiled and ran; Spring Boot companion and fixture passed.
- Semantic PDF QA: 113 pages, zero errors, and one approved informational warning for the repeated series-roadmap table header across pages 110-111.
- Visual PDF QA: all 113 pages rendered at 110 DPI and inspected through six contact sheets; pages 99 and 110-113 also received individual high-resolution inspection.
- Visual candidates: zero blank, zero edge-clipping, and zero dark-page candidates.
- Repository validation passed layout, curriculum structure, 153 navigation/contributor documents, 81 Java examples plus one smoke-test source, web catalog, and deployment configuration. The first run encountered macOS iCloud placeholder read timeouts in unrelated existing examples; after materialization, the complete command passed unchanged.
- Content-induced layout defects remaining: none.
- Boot test failures, companion compilation failures, and output mismatches: none.
- Remaining environmental warning: keep the worktree materialized before large all-repository builds on iCloud-backed storage.

## Remaining scope boundaries

Deep Spring Security, WebFlux/Reactor, Spring Data internals, MySQL locking and execution plans, JPA provider behavior, Kafka, JVM memory diagnostics, and distributed consistency remain in their dedicated books. Native images and target deployment platforms require application-specific build/runtime evidence.

## Recommended next book

Continue with **SD 06 - Spring Data for Java Backend Engineers**, using SD 04 container/transaction mechanics and this edition's Boot configuration, testing, data-boundary, migration, and operations foundations.
