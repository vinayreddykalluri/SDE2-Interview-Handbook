# Dependencies, Classpaths, Scopes, and Configurations

Dependency declarations are not merely download instructions. They determine compile visibility, runtime contents, test isolation, transitive exposure, publication metadata, and sometimes consumer recompilation.

## Start with classpaths

A typical Java project has at least these conceptual classpaths:

```text
main compile:   main classes + compile-visible dependencies
main runtime:   main classes + runtime dependencies
test compile:   test classes + main output + test compile dependencies
test runtime:   test classes + main output + test runtime dependencies
```

A dependency can be unnecessary at compile time but required at runtime, such as a database driver behind a standard API. A test library belongs on test classpaths, not the published production API.

## Maven scopes

The common scopes are:

| Scope | Main compile | Main runtime | Test | Transitive to consumer |
|---|---:|---:|---:|---:|
| `compile` | yes | yes | yes | yes |
| `provided` | yes | no | yes | no |
| `runtime` | no | yes | yes | yes |
| `test` | no | no | yes | no |

Maven also defines `system`, which binds a build to a machine-local path and is strongly discouraged, and `import`, which is used only for POM dependencies in `dependencyManagement`.

```xml
<dependencies>
  <dependency>
    <groupId>org.slf4j</groupId>
    <artifactId>slf4j-api</artifactId>
    <version>${slf4j.version}</version>
  </dependency>
  <dependency>
    <groupId>org.junit.jupiter</groupId>
    <artifactId>junit-jupiter</artifactId>
    <version>${junit.version}</version>
    <scope>test</scope>
  </dependency>
</dependencies>
```

Declare libraries used directly by source code even if they currently arrive transitively. That documents the module contract and prevents an unrelated upstream upgrade from silently removing the classpath.

## Gradle configurations

Gradle configurations have roles: some accept dependency declarations, some are resolved as classpaths, and some expose variants to consumers. Java plugins create familiar declarable configurations:

- `implementation`: needed to implement the component but not exposed as its API;
- `api`: exposed to consumers by `java-library` when public signatures require it;
- `compileOnly`: compile-visible but absent at runtime;
- `runtimeOnly`: absent at compile time but present at runtime;
- `testImplementation` and `testRuntimeOnly`: test-specific inputs.

```kotlin
plugins {
    `java-library`
}

dependencies {
    api("org.slf4j:slf4j-api:2.0.16")
    implementation("com.fasterxml.jackson.core:jackson-databind:2.18.2")
    runtimeOnly("org.postgresql:postgresql:42.7.5")
    testImplementation("org.junit.jupiter:junit-jupiter:5.11.4")
}
```

Do not declare on resolvable internal classpaths merely because the DSL allows low-level access. Use plugin-defined buckets and let the plugin model the graph.

## API versus implementation

Suppose a library exposes `org.slf4j.Logger` in a public method signature. Consumers need SLF4J to compile against that API, so `api` is appropriate in Gradle. If Jackson is only used inside private implementation, use `implementation`.

```text
consumer compile classpath
       ^
       | exposed API dependency
library public surface ---- library implementation details
                                  |
                                  +-- hidden from consumer compile classpath
```

Overusing `api` expands consumer compile classpaths and increases recompilation. Hiding a required public type under `implementation` makes the published contract incomplete.

Maven's POM model does not express Gradle's API/implementation distinction with identical precision. Maven library authors must design published dependencies and public APIs carefully rather than assuming a one-to-one scope mapping.

## Repositories and metadata

Dependency resolution uses both artifact files and metadata such as POMs or Gradle Module Metadata. Repository order, content, authentication, and availability affect the result.

Avoid adding arbitrary repositories until a missing artifact resolves. Multiple unfiltered repositories can create reliability and dependency-confusion risks. Prefer organization-controlled repository policy, HTTPS, and content restrictions.

## Optional dependencies and exclusions

In Maven, an optional dependency is not automatically propagated to consumers. An exclusion removes a particular transitive path. In Gradle, exclusions and constraints can alter graph selection, while capabilities can model mutually exclusive implementations.

Use exclusions only with evidence. Removing a transitive dependency may compile and then fail at runtime. Add a deliberate replacement and a test that exercises the affected path.

## `NoClassDefFoundError` reasoning

If compilation succeeds but runtime fails with `NoClassDefFoundError`:

1. identify the missing binary name;
2. inspect the runtime classpath, not only declared dependencies;
3. find whether scope/configuration omitted the artifact;
4. check packaging or container-provided assumptions;
5. check duplicate or incompatible versions;
6. reproduce outside the IDE.

The source may have compiled against a provided dependency that the deployment environment did not actually provide.

## Interview drill

**Question:** Compare Maven `compile` scope with Gradle `implementation`.

**Strong answer:** They overlap for many application dependencies but are not exact synonyms. Maven `compile` is present on compile, runtime, and test classpaths and propagates to consumers. Gradle `implementation` is deliberately not exposed on a Java library consumer's compile classpath; `api` expresses that exposure. I compare the desired classpath and publication contract instead of memorizing a one-to-one mapping.

## Common mistakes

- Adding every dependency to the broadest scope.
- Assuming a transitive dependency is a stable direct contract.
- Confusing dependency repositories with plugin repositories.
- Excluding a library without a runtime test.
- Treating version-catalog aliases as resolution locks.
- Assuming a successful compilation proves the packaged runtime classpath.

## Practice

1. **Foundation:** Place JUnit, a JDBC API, a driver, and an internal JSON library on appropriate classpaths.
2. **Predict:** What happens when a `provided` dependency is absent in production?
3. **Debugging:** A public API exposes a type declared as Gradle `implementation`. Explain the consumer symptom.
4. **Interview Core:** Compare direct, transitive, optional, and excluded dependencies.
5. **SDE-2 Follow-up:** Design repository policy for public and private artifact namespaces.

## Readiness check

- [ ] I reason from classpaths before choosing syntax.
- [ ] I understand Maven scope and Gradle configuration differences.
- [ ] I can diagnose compile-success/runtime-failure dependency problems.
