# Publishing, Versioning, and Private Repositories

Publishing turns a local artifact into a component contract for other builds. It requires stable identity, correct metadata, credentials, immutability policy, and consumer validation.

## Coordinates and version policy

Maven-compatible coordinates commonly contain:

```text
groupId : artifactId : version : packaging : classifier
```

Choose names that remain meaningful after teams reorganize. Avoid republishing different bytes under the same release coordinate. Mutable snapshots can support integration, but consumers must understand cache and reproducibility effects.

Semantic versioning can communicate compatibility intent, but it is not automatic proof. Java binary, source, behavior, serialization, schema, and configuration compatibility can change differently.

## Maven install versus deploy

- `install` writes artifact and POM to the local repository;
- `deploy` uploads to the configured remote repository;
- `distributionManagement` declares deployment targets;
- credentials belong in settings under a matching server ID.

```xml
<distributionManagement>
  <snapshotRepository>
    <id>internal-snapshots</id>
    <url>https://packages.example/snapshots</url>
  </snapshotRepository>
  <repository>
    <id>internal-releases</id>
    <url>https://packages.example/releases</url>
  </repository>
</distributionManagement>
```

```xml
<!-- ~/.m2/settings.xml, supplied securely -->
<server>
  <id>internal-releases</id>
  <username>${env.REPOSITORY_USER}</username>
  <password>${env.REPOSITORY_TOKEN}</password>
</server>
```

Do not print the effective settings in public logs without redaction.

## Gradle Maven publication

```kotlin
plugins {
    `java-library`
    `maven-publish`
}

publishing {
    publications {
        create<MavenPublication>("library") {
            from(components["java"])
            pom {
                name = "Pricing Contracts"
                description = "Stable pricing API contracts"
            }
        }
    }
    repositories {
        maven {
            name = "internal"
            url = uri("https://packages.example/releases")
            credentials {
                username = providers.environmentVariable("REPOSITORY_USER").orNull
                password = providers.environmentVariable("REPOSITORY_TOKEN").orNull
            }
        }
    }
}
```

Provider-based environment access delays evaluation. Still prevent credentials from entering configuration-cache entries, logs, or committed properties.

`publishToMavenLocal` is useful for a narrow local experiment, but local publication can mask missing remote metadata or stale components. A composite build may be better for source co-development, while a temporary isolated repository is better for testing publication behavior.

## Library publication checklist

- main JAR, sources JAR, and Javadoc JAR as policy requires;
- correct POM scope and Gradle variant metadata;
- license, SCM, developer, and project metadata for public publication;
- checksums/signatures under the repository policy;
- dependency constraints that do not over-constrain consumers;
- no internal repositories or credentials leaked into published metadata;
- reproducible artifact evidence;
- consumer smoke tests using the published form;
- immutable release coordinate.

## BOMs and platforms as products

A Maven BOM or Gradle Java platform publishes a compatibility policy, not executable code. It should have release notes, tests for supported combinations, ownership, and upgrade strategy.

Publishing every internal library version through one giant BOM can create lockstep coupling. Group components that are genuinely tested together.

## Snapshot and release incident

Symptom: CI passes against `1.4.0-SNAPSHOT`, but production artifact built later behaves differently from the tested candidate.

Cause: the snapshot coordinate was mutable, so dependency bytes changed between verification and release.

Correction:

1. identify exact hashes resolved in each build;
2. stop promoting the unverified artifact;
3. release immutable component versions;
4. rebuild the consumer from pinned inputs;
5. promote the already verified consumer artifact rather than rebuilding per environment.

## Interview drill

**Question:** What is the difference between a source version and a published artifact version?

**Strong answer:** A source version identifies repository state such as a Git commit. A component version is part of artifact coordinates and its compatibility contract. A release process connects them with build instructions, toolchain, dependency graph, hashes, and provenance. Neither identifier alone proves which bytes reached production.

## Practice

1. **Foundation:** Explain install, deploy, publish, and promote.
2. **Predict:** Can `publishToMavenLocal` prove remote consumers will resolve correctly?
3. **Debugging:** A release POM exposes an internal implementation dependency. Find the declaration error.
4. **Interview Core:** Explain why release coordinates should be immutable.
5. **SDE-2 Follow-up:** Design a library release candidate and consumer-validation flow.

## Readiness check

- [ ] I separate local installation from remote publication.
- [ ] I test published metadata, not only project compilation.
- [ ] I can connect commit, artifact coordinate, hash, and deployment.
