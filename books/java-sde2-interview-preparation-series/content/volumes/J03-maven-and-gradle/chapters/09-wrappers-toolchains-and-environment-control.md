# Wrappers, Toolchains, and Environment Control

"Java version" can mean at least four things: the JDK launching the build tool, the JDK compiling source, the language/API release target, and the runtime executing tests or the application. Treat them as separate evidence.

## Repository-owned wrappers

The Maven Wrapper and Gradle Wrapper let a repository declare the build-tool distribution used by developers and CI.

```text
developer / CI
      |
      v
wrapper script -> wrapper properties -> verified distribution -> build
```

Commit the expected wrapper files. Review distribution URL, version, checksum capability, and wrapper code changes. A wrapper downloads and executes tooling, so it is part of the supply chain.

Common entry points:

```bash
./mvnw --version
./mvnw verify
./gradlew --version
./gradlew build
```

Do not silently fall back to globally installed `mvn` or `gradle` in CI when a wrapper fails.

## Maven toolchains

Maven itself runs on one JVM. Toolchain-aware plugins can select another JDK for compilation, tests, Javadoc, or signing. Machine installations can be mapped in `~/.m2/toolchains.xml`, while project configuration requests capabilities.

```xml
<plugin>
  <groupId>org.apache.maven.plugins</groupId>
  <artifactId>maven-toolchains-plugin</artifactId>
  <version>3.2.0</version>
  <executions>
    <execution>
      <goals><goal>select-jdk-toolchain</goal></goals>
    </execution>
  </executions>
  <configuration>
    <version>21</version>
  </configuration>
</plugin>
```

Verify which plugins are toolchain-aware. A plugin that ignores the selected toolchain may still use the Maven launcher JVM.

## Gradle toolchains

```kotlin
java {
    toolchain {
        languageVersion = JavaLanguageVersion.of(21)
    }
}
```

Toolchains can select or provision a compatible JDK according to configured resolver policy. The Gradle daemon or launcher JVM can differ from the compiler/test launcher selected by tasks.

Inspect:

```bash
./gradlew --version
./gradlew javaToolchains
./gradlew compileJava --info
```

## `--release` versus source and target

`javac --release 17` constrains language features, bytecode target, and supported Java SE API signatures for release 17. Setting only source and target can still compile against APIs from the newer JDK that will not exist on the older runtime.

Maven:

```xml
<maven.compiler.release>21</maven.compiler.release>
```

Gradle:

```kotlin
tasks.withType<JavaCompile>().configureEach {
    options.release = 21
}
```

A toolchain answers "which JDK performs work?" Release answers "which Java platform API and class-file target may this compilation use?" Many projects set both deliberately.

## Environment control

Potential inputs include:

- JDK vendor and patch version;
- build-tool and plugin versions;
- locale, timezone, and file encoding;
- operating system and architecture;
- environment variables and system properties;
- user Maven/Gradle configuration;
- repository mirrors and credentials;
- network and artifact-repository state.

Not every difference must be eliminated, but every correctness-relevant difference must be controlled, declared, or tested in a matrix.

## "Works on my machine" diagnostic

Capture side-by-side evidence:

```bash
./mvnw --version
./gradlew --version
java --version
locale
env | sort                 # redact secrets before sharing
```

Then compare wrapper properties, toolchains, active Maven profiles, Gradle properties, repository configuration, and clean-checkout behavior. Do not paste raw environment dumps into tickets; they can contain secrets.

## Compatibility matrix

A library that publishes Java 17 bytecode but tests only on Java 21 has not demonstrated Java 17 runtime compatibility. Conversely, running the build tool on Java 17 does not prove the application artifact targets Java 17.

Define separate jobs when needed:

```text
build launcher JDK 21 -> compile --release 17 -> test runtime 17 and 21
```

## Interview drill

**Question:** If Maven runs on JDK 21, does that mean the project produces Java 21 bytecode?

**Strong answer:** No. The launcher JVM, compiler JDK/toolchain, compiler release, and test/runtime JVM are separate. I inspect Maven version output, toolchain selection, compiler configuration, and the produced class-file target. With `--release`, I can constrain both bytecode and supported platform APIs.

## Practice

1. **Foundation:** Name the four Java-version roles.
2. **Predict:** Can a Gradle daemon on one JDK compile with another toolchain?
3. **Debugging:** CI reports "invalid target release." Build an evidence sequence.
4. **Interview Core:** Explain why wrappers belong in version control.
5. **SDE-2 Follow-up:** Design a compatibility matrix for a Java 17 library maintained on JDK 21.

## Readiness check

- [ ] I do not collapse launcher, toolchain, release, and runtime into one version.
- [ ] I treat wrapper changes as executable supply-chain changes.
- [ ] I can reproduce environment differences without leaking secrets.
