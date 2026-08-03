# Starters, Dependency Management, and Build Plugins

Boot build support solves three different problems: choosing capabilities, aligning versions, and packaging an application. Candidates often call all three a starter; that loses important failure modes.

## Starters choose capabilities

```xml
<dependency>
  <groupId>org.springframework.boot</groupId>
  <artifactId>spring-boot-starter-webmvc</artifactId>
</dependency>
<dependency>
  <groupId>org.springframework.boot</groupId>
  <artifactId>spring-boot-starter-actuator</artifactId>
</dependency>
```

A starter is a curated dependency descriptor. It usually has little or no code. Its transitive libraries become classpath signals for auto-configuration.

## The BOM aligns versions

The `spring-boot-dependencies` bill of materials manages compatible versions for Spring modules and many third-party libraries. The Maven parent imports it and adds plugin defaults. A project that cannot inherit the parent can import the BOM in `dependencyManagement`.

Gradle can use the Boot plugin with native platform support:

```groovy
plugins {
    id 'java'
    id 'org.springframework.boot' version '4.1.0'
}

dependencies {
    implementation platform(org.springframework.boot.gradle.plugin.SpringBootPlugin.BOM_COORDINATES)
    implementation 'org.springframework.boot:spring-boot-starter-webmvc'
    testImplementation 'org.springframework.boot:spring-boot-starter-test'
}
```

Do not also pin `spring-core`, Jackson, Tomcat, or logging versions casually. A local override can defeat a security fix or create binary incompatibility.

## Plugins package and inspect

The Maven and Gradle Boot plugins can:

- create executable jars or wars;
- run the application during development;
- build OCI images through buildpacks;
- process AOT metadata;
- add build information for Actuator.

The plugin is not the dependency-management BOM, and the BOM does not produce an executable jar.

## Read the graph before changing it

```bash
./mvnw dependency:tree -Dverbose
./mvnw help:effective-pom
./gradlew dependencies
./gradlew dependencyInsight --dependency jackson-databind
```

Ask who introduced a library, which version constraint won, and whether it is used at compile, runtime, or test time.

## Exclusions and substitutions

To replace the default logging implementation, exclude deliberately and add one replacement. To switch the embedded server, use the focused runtime starter supported by the selected Boot line. Test startup, shutdown, access logs, TLS, compression, forwarded headers, and error behavior after the swap.

An exclusion is not a security policy. The excluded class may arrive through another path; verify the resolved graph and software bill of materials.

## Version strategy

1. Pick a supported Boot line.
2. Let Boot manage the dependency set.
3. Record justified overrides with owner and removal condition.
4. Use dependency and vulnerability automation.
5. Upgrade maintenance releases continuously.
6. Treat major/minor upgrades as behavior changes with migration tests.

## Common mistakes

- Adding every starter “just in case,” increasing startup surface and CVEs.
- Depending on both blocking and reactive stacks accidentally.
- Confusing a managed version with a direct dependency.
- Using `-DskipTests` as a universal CI shortcut without understanding other build phases.
- Producing a thin jar locally but deploying an executable-jar command.
- Overriding Spring Framework independently of Boot.

## Interview angle

**Interviewer:** A CVE scanner reports a transitive library. What do you do?

**Strong answer:** I identify the introducing path and resolved version, check whether the vulnerable code is reachable, prefer a supported Boot maintenance release that aligns the dependency set, and use a temporary override only with compatibility tests and a removal plan. I verify the packaged artifact and SBOM, not only the source build file.

## Quick check

1. Starter versus BOM versus plugin?
2. What does an effective POM reveal?
3. Why can one version override be risky?
4. What should be tested after swapping the server?
5. Why inspect the packaged dependency graph?

## Practice

- **Foundation:** Explain each transitive dependency in a minimal web service.
- **Interview Core:** Remove an unnecessary starter and compare the condition report.
- **Interview Core:** Diagnose a version conflict with `dependencyInsight`.
- **SDE-2 Follow-up:** Write an emergency-override policy for a critical transitive CVE.
