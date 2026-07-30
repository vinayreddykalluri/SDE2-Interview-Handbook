# Practice Workbook, Solutions, Command Reference, and Sources

Complete the tasks in a disposable repository. Predict the graph or output before running a command. Do not use a production artifact repository for practice.

## Foundation exercises

1. Draw inputs, work, outputs, and publications for a Java library.
2. Create the same Java 21 project with Maven and Gradle Kotlin DSL.
3. Explain POM coordinates and Gradle project identity.
4. Compare Maven phase, goal, plugin, and execution.
5. Compare Gradle initialization, configuration, and execution.
6. Map main compile, main runtime, test compile, and test runtime classpaths.
7. Place JUnit and a database driver in appropriate scopes/configurations.
8. Inspect Maven effective POM and active profiles.
9. Inspect Gradle projects, tasks, and a dry-run task graph.
10. Build a plain JAR and inspect its manifest and contents.
11. Explain wrapper files to a new teammate.
12. Compare launcher JDK, toolchain JDK, release target, and runtime JDK.
13. Add a unit test and locate its XML report.
14. Explain why generated output directories are normally ignored by Git.
15. State when `clean` is diagnostically useful and when it wastes feedback time.

## Interview-core exercises

16. Draw a transitive version conflict and predict Maven mediation.
17. Use Maven dependency tree to confirm the selected path.
18. Use Gradle dependency insight on one runtime configuration.
19. Compare BOM, platform, catalog, constraint, lock, and checksum.
20. Explain Maven `dependencyManagement` without saying it adds dependencies.
21. Explain Gradle `api` versus `implementation` from a consumer classpath.
22. Configure Maven Surefire and Failsafe and explain the verify endpoint.
23. Configure a Gradle integration suite and wire it into `check`.
24. Diagnose a green build that discovered zero tests.
25. Explain `NoClassDefFoundError` versus `NoSuchMethodError` build causes.
26. Split a service into three acyclic modules.
27. Select one Maven reactor module plus required upstream modules.
28. Run one qualified Gradle subproject task.
29. Explain parent inheritance versus aggregation.
30. Explain multi-project versus composite Gradle builds.
31. Compare plain JAR, application distribution, and fat JAR.
32. Create a packaged runtime smoke-test plan.
33. Explain `install`, `deploy`, `publish`, and promote.
34. Review a generated publication POM for API/runtime mistakes.
35. Explain why a local publication can hide repository defects.

## SDE-2 exercises

36. Design a wrapper and JDK upgrade rollout across 50 repositories.
37. Define an affected-module CI algorithm for a parent/BOM change.
38. Create a cache trust model with CI-only writers.
39. Diagnose a Gradle task with stale cache hits.
40. Diagnose a Maven build that works only after local `install`.
41. Compare clean, warm, and incremental performance scenarios.
42. Design a dependency-confusion defense for internal coordinates.
43. Respond to a compromised build plugin.
44. Respond to a token printed in debug logs.
45. Define SBOM, checksum, signature, provenance, and reproducibility evidence.
46. Design immutable snapshot-to-release promotion.
47. Build a Maven-to-Gradle parity matrix.
48. Define rollback criteria for a build-tool migration.
49. Diagnose different archive hashes across operating systems.
50. Write an incident update for a production runtime linkage error.

## Cumulative assessments

### Assessment A: New service

Design a two-module Java service with Java 21, unit tests, integration tests, wrapper entry point, dependency version policy, packaged smoke test, and PR CI. Provide Maven and Gradle command paths.

### Assessment B: Dependency incident

A private `com.example:payments-api` resolves from a public repository on one CI runner. Provide containment, evidence, correction, validation, and prevention.

### Assessment C: Slow monorepo

A 180-module build takes 40 minutes. Developers propose path-only module selection and maximum parallelism. Produce a measurement and safe-optimization plan.

### Assessment D: Library release

A Gradle-built library works for Gradle consumers but Maven consumers miss a type from a public signature. Diagnose metadata and publish a safe corrected release.

### Assessment E: Migration

Plan Maven-to-Gradle migration for a service using generated OpenAPI source, unit and integration tests, shading, an internal BOM, and artifact signing.

## Final readiness assessment

In 45 minutes, explain and sketch:

1. the shared build model;
2. Maven lifecycle and effective-model mechanics;
3. Gradle lifecycle, task graph, providers, and caches;
4. dependency selection and runtime linkage diagnosis;
5. tests, modules, artifacts, wrappers, toolchains, CI, publication, and security;
6. one production incident with containment and recovery;
7. a constraint-based Maven-versus-Gradle recommendation.

## Solution sketches

### Exercises 16-20

Draw every dependency path and distinguish requested from selected versions. Maven evidence comes from `dependency:tree`; Gradle evidence is configuration-specific and comes from `dependencies` plus `dependencyInsight`. A BOM/platform aligns versions, a catalog centralizes declarations, a lock records resolution, and a checksum verifies bytes.

### Exercises 22-24

Surefire owns unit-test execution in Maven's test phase. Failsafe spans integration-test and verify so teardown can run before final failure. In Gradle, an integration suite must be scheduled by `check`; ordering alone is insufficient. Zero discovered tests require engine, naming/filter, source set, and report inspection.

### Exercises 25 and 40

`NoClassDefFoundError` often signals missing runtime content. `NoSuchMethodError` signals binary mismatch between compile and runtime definitions. A build that needs local `install` likely has an undeclared project/artifact relationship; reproduce with an isolated local repository and declare it.

### Exercises 37-39

Affected selection follows project dependencies, dependents, shared build logic, parents, platforms/catalogs, generated contracts, and integration edges. Cache correctness requires complete declared inputs/outputs and trusted writers. Preserve a stale-hit reproduction before invalidating caches.

### Exercises 42-45

Restrict internal namespaces to an approved private repository, verify artifacts, and separate read/write identities. Rotate exposed secrets first. SBOM inventory, checksum identity, signature identity, provenance, reproducibility, and vulnerability evidence answer distinct questions.

### Exercises 47-49

Migration parity covers graphs, test inventory, generated work, resources, artifacts, metadata, runtime, and performance. Roll back on correctness or publication mismatch. Different hashes require structural archive comparison for timestamps, order, paths, permissions, encodings, and platform-specific files.

## Maven command reference

```bash
./mvnw --version
./mvnw test
./mvnw verify
./mvnw dependency:tree
./mvnw dependency:analyze
./mvnw help:effective-pom -Dverbose
./mvnw help:active-profiles
./mvnw -pl :module -am verify
./mvnw -pl :module -amd test
./mvnw -rf :module verify
```

## Gradle command reference

```bash
./gradlew --version
./gradlew projects
./gradlew tasks --all
./gradlew test
./gradlew check
./gradlew build
./gradlew :module:build
./gradlew dependencies --configuration runtimeClasspath
./gradlew dependencyInsight --dependency name \
  --configuration runtimeClasspath
./gradlew build --console=verbose
./gradlew build --configuration-cache --build-cache
```

## Official sources

Version-sensitive statements were checked on 2026-07-29. Consult current official documentation before changing production configuration.

### Apache Maven

- Maven lifecycle: https://maven.apache.org/guides/introduction/introduction-to-the-lifecycle.html
- POM model: https://maven.apache.org/pom.html
- Dependency mechanism: https://maven.apache.org/guides/introduction/introduction-to-dependency-mechanism.html
- Standard layout: https://maven.apache.org/guides/introduction/introduction-to-the-standard-directory-layout.html
- Multiple modules: https://maven.apache.org/guides/mini/guide-multiple-modules.html
- Toolchains: https://maven.apache.org/guides/mini/guide-using-toolchains
- Reproducible builds: https://maven.apache.org/guides/mini/guide-reproducible-builds.html
- Surefire: https://maven.apache.org/surefire/maven-surefire-plugin/
- Failsafe: https://maven.apache.org/surefire/maven-failsafe-plugin/
- Effective POM: https://maven.apache.org/plugins/maven-help-plugin/effective-pom-mojo.html

### Gradle

- Build lifecycle: https://docs.gradle.org/current/userguide/build_lifecycle.html
- Wrapper: https://docs.gradle.org/current/userguide/gradle_wrapper.html
- Java and Java Library plugins: https://docs.gradle.org/current/userguide/java_library_plugin.html
- Dependency management: https://docs.gradle.org/current/userguide/core_dependency_management.html
- Dependency locking: https://docs.gradle.org/current/userguide/dependency_locking.html
- Dependency verification: https://docs.gradle.org/current/userguide/dependency_verification.html
- JVM test suites: https://docs.gradle.org/current/userguide/jvm_test_suite_plugin.html
- Multi-project builds: https://docs.gradle.org/current/userguide/intro_multi_project_builds.html
- Composite builds: https://docs.gradle.org/current/userguide/composite_builds.html
- Build cache: https://docs.gradle.org/current/userguide/build_cache.html
- Configuration cache: https://docs.gradle.org/current/userguide/configuration_cache.html
- Maven publishing: https://docs.gradle.org/current/userguide/publishing_maven.html

## Final checklist

- [ ] I can explain the concept before showing syntax.
- [ ] I can inspect effective configuration and resolved graphs.
- [ ] I can separate compile, test, package, publish, and deploy outcomes.
- [ ] I can diagnose across source, toolchain, graph, artifact, and runtime.
- [ ] I can defend build security, reproducibility, caching, and migration choices.
