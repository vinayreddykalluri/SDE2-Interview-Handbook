# Resolution Conflicts, BOMs, Platforms, and Locks

Declaring dependencies creates requests. Resolution turns those requests and transitive metadata into an actual graph. Interviews test whether you can explain why a particular version won and how you would verify it.

## A conflict graph

```text
application
  +-- client-a:1.0 ----> json-core:2.1
  +-- client-b:1.0 ----> json-core:2.6
```

The build needs one compatible runtime result unless isolation is explicitly designed. "The latest wins" is not a universal answer.

## Maven mediation

Maven commonly chooses the nearest definition in the dependency tree. A version reached through fewer dependency edges is nearer. At equal depth, declaration order can decide. A direct declaration is nearest, but scattering overrides across modules makes the graph hard to govern.

Inspect the selected and omitted paths:

```bash
./mvnw dependency:tree
./mvnw dependency:tree -Dverbose
./mvnw dependency:tree \
  -Dincludes=com.fasterxml.jackson.core:jackson-databind
./mvnw dependency:analyze
```

`dependencyManagement` centralizes versions when a dependency is encountered:

```xml
<dependencyManagement>
  <dependencies>
    <dependency>
      <groupId>com.fasterxml.jackson</groupId>
      <artifactId>jackson-bom</artifactId>
      <version>${jackson.version}</version>
      <type>pom</type>
      <scope>import</scope>
    </dependency>
  </dependencies>
</dependencyManagement>
```

A BOM aligns a family of component versions. It does not prove the family is secure or mutually compatible with every other library in the application.

The Enforcer plugin can make convergence or banned-version rules explicit. Decide whether strict convergence is appropriate; forcing every path to one version can expose incompatible consumers rather than magically fixing them.

## Gradle resolution

Gradle builds a graph, selects components, chooses compatible variants using attributes and capabilities, and then resolves artifacts. A simple version conflict often selects a newer candidate, but constraints, platforms, strict versions, rejects, forced rules, locks, and metadata can change the outcome.

Use evidence:

```bash
./gradlew :app:dependencies --configuration runtimeClasspath
./gradlew :app:dependencyInsight \
  --dependency jackson-databind \
  --configuration runtimeClasspath
```

`dependencyInsight` explains selection reasons and paths; it is more useful than reading only the declaration.

## Catalogs, platforms, constraints, and locks

These mechanisms solve different problems:

| Mechanism | Primary job |
|---|---|
| version catalog | central names and requested versions for build authors |
| platform / BOM | align or constrain a related version set |
| dependency constraint | express acceptable or preferred versions in a graph |
| strict version | reject outcomes outside an exact constraint |
| lock file | record resolved versions for repeatable future resolution |
| verification metadata | authenticate expected dependency bytes or signatures |

A catalog is not a lock. A lock is not a checksum. A checksum is not a vulnerability assessment.

Gradle platform use:

```kotlin
dependencies {
    implementation(platform("com.fasterxml.jackson:jackson-bom:2.18.2"))
    implementation("com.fasterxml.jackson.core:jackson-databind")
}
```

Locking example:

```kotlin
dependencyLocking {
    lockAllConfigurations()
}
```

```bash
./gradlew dependencies --write-locks
./gradlew build
```

Review lock changes like code. A generated lock based on an already-compromised repository records the compromise faithfully.

## Dynamic and changing versions

Selectors such as `1.+`, version ranges, and snapshots can change resolution without a source change. They may be useful for controlled compatibility testing, but release builds need a deliberate reproducibility policy.

Maven `SNAPSHOT` and Gradle changing modules also interact with metadata caches. `--refresh-dependencies` or deleting a cache is not a root-cause analysis; first determine what should have been immutable and which metadata expired.

## Version skew failure walkthrough

Symptom: a service compiles but throws `NoSuchMethodError` after deployment.

Reasoning:

1. `NoSuchMethodError` usually means the runtime loaded a class version whose binary API differs from the one used at compile time.
2. Capture the exact class and method descriptor.
3. Inspect the resolved runtime graph and packaged contents.
4. Find duplicate JARs, container libraries, shaded copies, or dependency mediation.
5. Align versions or isolate incompatible components.
6. Add a packaging/runtime smoke test.

Do not "fix" this by adding the newest version blindly. The newest version may break a different path.

## Interview drill

**Question:** Maven and Gradle resolve two versions of the same dependency. How do you know which one is used?

**Strong answer:** I do not infer solely from declaration order. In Maven I inspect `dependency:tree`, then apply nearest-definition and managed-version rules to the shown paths. In Gradle I inspect the target configuration with `dependencies` and `dependencyInsight`, because conflict selection, constraints, platforms, variants, and locks can affect the result. I verify the runtime classpath and packaged artifact when diagnosing a runtime linkage error.

## Practice

1. **Foundation:** Distinguish requested, selected, and downloaded versions.
2. **Predict:** Can importing a BOM add every BOM component to the application?
3. **Debugging:** Trace a `NoSuchMethodError` from runtime symptom to dependency path.
4. **Interview Core:** Compare Maven nearest mediation with Gradle selection evidence.
5. **SDE-2 Follow-up:** Design a controlled dependency-upgrade and lock-update process.

## Readiness check

- [ ] I can obtain selection evidence in both tools.
- [ ] I distinguish catalogs, BOMs/platforms, locks, and verification.
- [ ] I do not equate newest with compatible.
