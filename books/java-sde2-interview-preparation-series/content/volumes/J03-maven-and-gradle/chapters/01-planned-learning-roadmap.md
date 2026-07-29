# Maven and Gradle Together - Planned Learning Roadmap

> **Publication status:** roadmap edition. The current book fixes the prerequisite order and comparison boundaries before detailed labs and interview drills are added.

Java engineers need to understand build contracts, not merely copy a `pom.xml` or `build.gradle`. Maven emphasizes a declarative lifecycle and conventions. Gradle exposes a programmable task graph with rich incremental-build behavior. The completed book will teach both around the same Java project so readers can compare equivalent responsibilities.

## Planned sequence

1. Build inputs, outputs, source sets, artifacts, coordinates, and repositories.
2. Maven project structure, lifecycle phases, goals, plugins, scopes, and profiles.
3. Gradle projects, tasks, plugins, configurations, Kotlin DSL, and wrappers.
4. Dependency graphs, transitive dependencies, conflict resolution, exclusions, and locks.
5. Compile, test, package, integration-test, quality, and publication workflows.
6. Multi-module builds, dependency boundaries, build caching, and reproducibility.
7. Maven Wrapper and Gradle Wrapper as repository-owned toolchain contracts.
8. CI integration, credentials, private registries, SBOMs, and dependency security.

## Comparison contract

Every major concept will be shown in Maven and Gradle: declaring Java 21, adding JUnit, separating unit and integration tests, building an executable artifact, creating modules, pinning dependencies, and diagnosing a resolution failure. The book will distinguish portable build concepts from tool-specific syntax.

## Completion gate

A reader is ready to continue when they can explain the build lifecycle, inspect the effective dependency graph, reproduce a clean build through the wrapper, isolate a failing plugin or dependency, and justify Maven or Gradle for a team rather than selecting by popularity alone.
