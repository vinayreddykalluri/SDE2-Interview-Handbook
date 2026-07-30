# Your First Java Build, Side by Side

This chapter builds the same small Java application with Maven and Gradle. The goal is not to memorize both files. It is to map the same responsibilities: identity, Java version, source layout, compilation, tests, and packaging.

## Shared project

Use this structure for both versions:

```text
pricing-cli/
  src/main/java/com/example/pricing/PriceCalculator.java
  src/test/java/com/example/pricing/PriceCalculatorTest.java
  pom.xml                         # Maven version
  settings.gradle.kts            # Gradle version
  build.gradle.kts               # Gradle version
```

Maven and Gradle's Java plugins both understand the conventional `src/main/java` and `src/test/java` directories. Conventions remove configuration only when the project follows them.

```java
package com.example.pricing;

public final class PriceCalculator {
    public long total(long unitPriceInCents, int quantity) {
        if (unitPriceInCents < 0 || quantity < 0) {
            throw new IllegalArgumentException("values must be nonnegative");
        }
        return Math.multiplyExact(unitPriceInCents, quantity);
    }
}
```

`Math.multiplyExact` makes overflow an explicit failure instead of silently wrapping. Build examples should still contain production-quality Java.

## Maven version

`pom.xml` declares the project model:

```xml
<project xmlns="http://maven.apache.org/POM/4.0.0"
         xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
         xsi:schemaLocation="http://maven.apache.org/POM/4.0.0
                             https://maven.apache.org/xsd/maven-4.0.0.xsd">
  <modelVersion>4.0.0</modelVersion>
  <groupId>com.example</groupId>
  <artifactId>pricing-cli</artifactId>
  <version>1.0.0-SNAPSHOT</version>

  <properties>
    <maven.compiler.release>21</maven.compiler.release>
    <project.build.sourceEncoding>UTF-8</project.build.sourceEncoding>
  </properties>

  <build>
    <plugins>
      <plugin>
        <groupId>org.apache.maven.plugins</groupId>
        <artifactId>maven-compiler-plugin</artifactId>
        <version>3.13.0</version>
      </plugin>
    </plugins>
  </build>
</project>
```

Build with the wrapper when the repository provides it:

```bash
./mvnw verify
jar tf target/pricing-cli-1.0.0-SNAPSHOT.jar
```

`verify` is a lifecycle phase. Maven reaches it by running earlier phases such as compile, test, and package. The JAR appears under `target/` by convention.

## Gradle version

`settings.gradle.kts` gives the build and root project a stable name:

```kotlin
rootProject.name = "pricing-cli"
```

`build.gradle.kts` applies Java behavior and configures compilation:

```kotlin
plugins {
    java
}

group = "com.example"
version = "1.0.0-SNAPSHOT"

java {
    toolchain {
        languageVersion = JavaLanguageVersion.of(21)
    }
}

tasks.withType<JavaCompile>().configureEach {
    options.release = 21
    options.encoding = "UTF-8"
}
```

Build with the wrapper:

```bash
./gradlew build
jar tf build/libs/pricing-cli-1.0.0-SNAPSHOT.jar
```

`build` is a task, not a lifecycle phase. The Java plugin contributes tasks and relationships. Outputs appear under `build/` by convention.

## Responsibility mapping

| Responsibility | Maven | Gradle Kotlin DSL |
|---|---|---|
| project identity | POM coordinates | `group`, project name, `version` |
| Java behavior | compiler/plugin properties | Java plugin and toolchain |
| compile request | `mvn compile` | `./gradlew classes` |
| test request | `mvn test` | `./gradlew test` |
| full verification | `mvn verify` | `./gradlew check` or `build` |
| packaged JAR | `target/*.jar` | `build/libs/*.jar` |
| clean outputs | `mvn clean` | `./gradlew clean` |

`build` depends on both checking and assembly in the conventional Gradle Java model. `check` focuses on verification. Prefer the narrowest command that matches the desired outcome.

## Add JUnit Jupiter

Maven declares a test-scoped dependency and configures Surefire:

```xml
<dependency>
  <groupId>org.junit.jupiter</groupId>
  <artifactId>junit-jupiter</artifactId>
  <version>5.11.4</version>
  <scope>test</scope>
</dependency>
```

Gradle declares it on the test implementation configuration:

```kotlin
dependencies {
    testImplementation("org.junit.jupiter:junit-jupiter:5.11.4")
}

tasks.test {
    useJUnitPlatform()
}
```

The exact version is an example, not a promise of "latest." Production repositories should centralize and update versions under review.

## What actually happened

On a first build, the tool may download plugins, metadata, and dependencies. It then compiles main source, compiles tests against a different classpath, runs tests, and creates a JAR. A second build may reuse local artifacts or skip work, but only when the tool can justify reuse.

```text
main source -> main classes -----------------> JAR
                  |                            |
test source ------+-> test classes -> tests --+-> verification result
```

## Predict the output

If `PriceCalculatorTest` fails, will a normal `mvn package` or `./gradlew build` still publish a successful JAR result?

No. The JAR task may have produced a file before a later failure in some graphs, but the overall build fails. A file existing in an output directory is not proof that the build completed successfully or that it is safe to publish.

## Common mistakes

- Running `mvn install` when only `verify` is needed; `install` mutates the local repository.
- Assuming Maven's `package` and Gradle's `build` are exact synonyms.
- Mixing an installed Gradle version with a checked-in wrapper.
- Setting only an IDE language level.
- Adding JUnit to the production runtime classpath.
- Treating generated directories as source-controlled inputs.

## Interview drill

**Question:** What is the difference between `mvn package`, `mvn verify`, and `mvn install`?

**Strong answer:** All are phases in Maven's default lifecycle. Selecting a later phase runs earlier phases. `package` creates the distributable artifact, `verify` also completes verification checks such as integration-test result checks when configured, and `install` writes the artifact and POM to the local Maven repository for other local builds. I use `verify` for a CI quality gate unless local installation is intentionally required.

## Practice

1. **Foundation:** Create both build files for the sample and identify every shared responsibility.
2. **Predict:** What directories are recreated after `clean`?
3. **Debugging:** The IDE compiles Java 21 syntax but CI rejects it. List the build files and JVMs to inspect.
4. **Interview Core:** Explain why `target/` or `build/` should usually be ignored by Git.
5. **SDE-2 Follow-up:** Define which command your pull-request CI should run and defend it.
