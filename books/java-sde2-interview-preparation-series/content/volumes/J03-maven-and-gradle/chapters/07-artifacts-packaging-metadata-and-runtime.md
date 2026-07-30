# Artifacts, Packaging, Metadata, and Runtime

Compilation produces class files. Packaging turns outputs into distributable components and metadata. Delivery still has to prove that the runtime starts with the intended classpath and configuration.

## Common Java artifacts

- plain JAR containing project classes and resources;
- executable JAR with a main-class manifest and possibly bundled dependencies;
- WAR for a compatible servlet container;
- sources and Javadoc JARs for library consumers;
- POM and Gradle Module Metadata describing dependencies and variants;
- checksums, signatures, SBOMs, provenance, and test reports.

A plain JAR does not normally contain its dependency JARs. `java -jar app.jar` therefore needs an executable packaging strategy or a launcher/classpath arrangement.

## Inspect before assuming

```bash
jar tf target/pricing.jar
unzip -p target/pricing.jar META-INF/MANIFEST.MF
javap -classpath target/pricing.jar \
  com.example.pricing.PriceCalculator
```

For Gradle, substitute `build/libs/...`. Inspect duplicate files, service descriptors, manifest entries, module descriptors, and dependency contents when diagnosing packaging.

## Maven packaging and plugins

`jar` packaging contributes default goals for compilation, tests, JAR creation, installation, and deployment. Plugins can attach additional artifacts:

```xml
<plugin>
  <groupId>org.apache.maven.plugins</groupId>
  <artifactId>maven-source-plugin</artifactId>
  <version>3.3.1</version>
  <executions>
    <execution>
      <id>attach-sources</id>
      <goals><goal>jar-no-fork</goal></goals>
    </execution>
  </executions>
</plugin>
```

Understand whether a plugin goal forks a lifecycle, attaches an artifact, replaces the main artifact, or merely writes an untracked file. Duplicate plugin declarations are invalid design and Maven 4 is stricter about them.

## Gradle archives and application distribution

```kotlin
plugins {
    application
}

application {
    mainClass = "com.example.pricing.Main"
}

tasks.jar {
    manifest {
        attributes["Main-Class"] = application.mainClass.get()
    }
}
```

The Application plugin can create start scripts and distributions with runtime libraries. A manifest main class alone does not copy dependencies into the JAR.

For libraries:

```kotlin
java {
    withSourcesJar()
    withJavadocJar()
}
```

## Fat JAR and shading trade-offs

Bundling dependencies can simplify deployment but creates new obligations:

- merge service-provider files correctly;
- handle duplicate resources and licenses;
- avoid accidentally bundling signatures that no longer verify;
- decide whether packages must be relocated to prevent conflicts;
- record the true component inventory;
- verify startup and reflection-heavy paths.

Do not assume the first JAR task that includes `runtimeClasspath` is a correct production assembly.

## Reproducible output

Bit-for-bit reproduction can be broken by timestamps, file order, permissions, generated paths, hostnames, usernames, locale, or differing JDK/plugin versions.

Maven plugins can honor `project.build.outputTimestamp`:

```xml
<properties>
  <project.build.outputTimestamp>
    2026-01-01T00:00:00Z
  </project.build.outputTimestamp>
</properties>
```

Gradle archive tasks expose reproducibility controls, and modern defaults improve ordering and timestamp behavior. Still verify the actual artifact twice in isolated environments; configuration intent is not proof.

```bash
shasum -a 256 build/libs/*.jar
```

The same hash proves identical bytes, not that the bytes are safe or correct.

## Runtime smoke testing

A useful release gate starts the packaged artifact, not just classes from the IDE or test classpath. Check:

- main class and launch command;
- required dependencies and native libraries;
- Java runtime compatibility;
- configuration and secret injection;
- health or readiness behavior;
- graceful failure on missing configuration;
- artifact identity reported in logs or metadata.

## Interview drill

**Question:** Why can a build pass tests but the JAR fail at startup?

**Strong answer:** Tests may run against the tool-managed test runtime classpath, while the deployed JAR is a plain artifact without dependencies or has different packaging. The manifest may be wrong, a runtime-only dependency may be absent, duplicate resources may be merged incorrectly, or the runtime JDK may be incompatible. I inspect the artifact and run a packaged smoke test with the deployment-style command.

## Practice

1. **Foundation:** Explain classes, plain JAR, executable distribution, and publication metadata.
2. **Predict:** Does adding `Main-Class` automatically embed dependency JARs?
3. **Debugging:** A service provider works in tests but disappears from a fat JAR. What do you inspect?
4. **Interview Core:** Separate reproducibility, integrity, and vulnerability evidence.
5. **SDE-2 Follow-up:** Define release checks for a reusable Java library.

## Readiness check

- [ ] I inspect packaged contents rather than inferring them.
- [ ] I understand executable packaging and shading trade-offs.
- [ ] I include deployment-style smoke tests in release reasoning.
