# Multi-Module and Multi-Project Build Design

Splitting a repository into modules can enforce ownership and dependency direction, enable reuse, and reduce the affected build graph. It can also create ceremony, cyclic pressure, and accidental coupling. Module count is not architecture quality.

## Example architecture

```text
pricing-parent
  +-- pricing-domain       pure domain types and rules
  +-- pricing-storage      persistence adapter -> domain
  +-- pricing-service      application -> domain + storage
```

Desired direction:

```text
service ---> storage ---> domain
    +---------------------> domain
```

A cycle such as domain depending on storage signals a boundary problem. Build tools can reject the cycle, but architecture must resolve it.

## Maven reactor

An aggregator POM collects modules:

```xml
<packaging>pom</packaging>
<modules>
  <module>pricing-domain</module>
  <module>pricing-storage</module>
  <module>pricing-service</module>
</modules>
```

The reactor collects projects, determines build order from instantiated relationships, selects requested projects, and builds them. Declaration order is not a substitute for dependency declarations.

Useful selection flags:

```bash
./mvnw -pl pricing-service -am verify
./mvnw -pl pricing-storage -amd test
./mvnw -rf :pricing-storage verify
```

- `-pl` selects projects;
- `-am` also builds required reactor dependencies;
- `-amd` also builds reactor dependents;
- `-rf` resumes from a project after a failure.

Understand the trade-off: resuming after a source change or changed environment may reuse earlier outputs that should be rebuilt. Reproduce cleanly before release.

### Parent and BOM structure

A parent POM can centralize plugin and dependency management. A BOM POM communicates dependency versions. Keeping these roles distinct may improve reuse, but a small repository may reasonably combine parent and aggregator roles.

Avoid a parent that activates every expensive plugin in every child. Put default versions under management and activate behavior through clear module conventions.

## Gradle multi-project build

`settings.gradle.kts` includes projects:

```kotlin
rootProject.name = "pricing-platform"
include("pricing-domain", "pricing-storage", "pricing-service")
```

A module dependency is explicit:

```kotlin
dependencies {
    implementation(project(":pricing-domain"))
    implementation(project(":pricing-storage"))
}
```

Run a qualified task:

```bash
./gradlew :pricing-service:build
./gradlew :pricing-domain:test
```

Gradle schedules required producer tasks based on project dependencies. Do not implement module ordering with manual task-order hacks.

## Shared Gradle build logic

Small builds can use a root script carefully. Larger builds benefit from convention plugins that define the organization's Java service or library contract.

```text
build-logic/                       included build
  src/main/kotlin/
    company.java-library.gradle.kts
pricing-domain/build.gradle.kts    applies convention plugin
```

`buildSrc` is convenient but globally affects the build when changed. An explicit included `build-logic` build is more flexible and scales better. Avoid cross-project configuration that eagerly reaches into every child.

## Composite builds

A multi-project build contains subprojects in one build. A composite build includes independent builds and can substitute a published dependency with local source during co-development:

```kotlin
includeBuild("../shared-observability")
```

This is powerful for testing a library and consumer together, but publication metadata can differ from source substitution. Always validate the released coordinates too.

## Selective build correctness

Path-based CI that builds only "changed modules" can miss:

- downstream consumers;
- shared build logic;
- parent/BOM/catalog changes;
- generated schemas;
- runtime-only wiring;
- integration tests across modules.

The safe affected set is graph-based, not merely directory-based.

```text
changed module + transitive dependents + shared contract consumers
```

Use periodic full builds as a backstop, but do not let them become the only place integration failures appear.

## Interview drill

**Question:** What decides module build order?

**Strong answer:** Real dependency relationships should decide it. Maven's reactor sorts collected projects using instantiated relationships such as project dependencies and build plugins, not simply the `<modules>` order. Gradle constructs task dependencies across project dependencies. Manual ordering is a warning that the build graph is not modeled correctly.

## Practice

1. **Foundation:** Split a Java service into domain, adapter, and application modules.
2. **Predict:** Does Maven `dependencyManagement` create reactor build order by itself?
3. **Debugging:** A selective build misses a consumer failure after a BOM change. Explain why.
4. **Interview Core:** Compare parent, aggregator, multi-project, and composite build.
5. **SDE-2 Follow-up:** Design affected-module CI with a periodic full-build backstop.

## Readiness check

- [ ] I model module direction explicitly.
- [ ] I can use reactor and qualified-task selection safely.
- [ ] I know why source substitution does not replace publication testing.
