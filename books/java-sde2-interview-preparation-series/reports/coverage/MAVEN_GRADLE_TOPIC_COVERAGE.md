# Maven and Gradle Topic Coverage

| Topic | Required depth | Final chapter | Examples | Practice | Validation |
|---|---|---:|---|---|---|
| build inputs, work and outputs | Foundation | 00 | graph diagrams | 5 tasks | source review |
| first Maven and Gradle build | Foundation | 01 | POM and Kotlin DSL | 5 tasks | paired fixtures |
| Maven POM and effective model | Interview Core | 02 | help commands and plugin execution | 5 tasks | Maven fixture |
| Maven lifecycle and packaging | Interview Core | 02, 07 | phases and attached artifacts | 10 tasks | `mvn verify` |
| Gradle lifecycle and task graph | Interview Core | 03 | lazy tasks and providers | 5 tasks | Gradle fixture |
| Gradle work avoidance | SDE-2 | 03, 10 | cache and input models | 10 tasks | companion model |
| scopes and configurations | Interview Core | 04 | parallel declarations | 5 tasks | build fixtures |
| transitive conflicts | SDE-2 | 05 | Maven and Gradle evidence commands | 5 tasks | interview rounds |
| BOM/platform/catalog/lock | SDE-2 | 05 | comparison and configuration | 5 tasks | coverage audit |
| unit and integration tests | Interview Core | 06 | Surefire/Failsafe and suites | 5 tasks | command validation |
| artifacts and runtime | Interview Core | 07 | JAR inspection and smoke tests | 5 tasks | both JARs executed |
| reactor and multi-project | SDE-2 | 08 | three-module reasoning | 5 tasks | two-module fixtures |
| composite and build logic | SDE-2 | 08 | included-build design | 5 tasks | interview coverage |
| wrappers and toolchains | Interview Core | 09 | release and version roles | 5 tasks | environment audit |
| CI and performance | SDE-2 | 10 | critical-path and trust model | 5 tasks | site/source checks |
| publishing and metadata | SDE-2 | 11 | Maven deploy and Gradle publish | 5 tasks | consumer checklist |
| security and reproducibility | SDE-2 | 12 | verification and evidence matrix | 5 tasks | official-source audit |
| complex incidents | SDE-2 | 13 | 18 playbooks | 5 tasks | scenario review |
| selection and migration | SDE-2 | 14 | parity and metrics | 5 tasks | migration assessment |
| real interview Q&A | Readiness | 15 | 18 dialogue rounds | spoken rubric | editorial review |
| cumulative practice | Readiness | 16 | 50 tasks and six assessments | 56 tasks | solution sketches |

## Deliberate boundaries

- Writing custom Maven plugins and Gradle binary plugins is introduced through ownership principles but belongs in an advanced build-engineering book.
- Android-specific Gradle variants and Kotlin Multiplatform are outside this Java backend scope.
- Spring Boot packaging details belong in the Spring Boot book; this volume teaches the build contract they depend on.
- GitHub Actions governance is cross-referenced conceptually to the Git/GitHub book rather than duplicated.
- Organization-specific repository products, credentials, and compliance controls remain local policy.
