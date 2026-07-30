# Testing, Integration Suites, and Quality Gates

A build should make different test purposes visible. Unit tests, component tests, integration tests, architecture checks, and end-to-end tests have different cost, isolation, and failure ownership.

## Test pyramid as build topology

```text
many fast unit tests -------------------- pull request
fewer component/integration tests ------- verify/check
small end-to-end or environment tests --- delivery pipeline
```

The exact shape varies, but every gate needs a clear trigger, timeout, report, and owner.

## Maven: Surefire and Failsafe

Surefire runs unit tests in the `test` phase. Failsafe is designed for integration tests across `integration-test` and `verify`, allowing `post-integration-test` cleanup before final failure reporting.

```xml
<plugin>
  <groupId>org.apache.maven.plugins</groupId>
  <artifactId>maven-surefire-plugin</artifactId>
  <version>3.5.2</version>
  <configuration>
    <useModulePath>false</useModulePath>
  </configuration>
</plugin>
<plugin>
  <groupId>org.apache.maven.plugins</groupId>
  <artifactId>maven-failsafe-plugin</artifactId>
  <version>3.5.2</version>
  <executions>
    <execution>
      <goals>
        <goal>integration-test</goal>
        <goal>verify</goal>
      </goals>
    </execution>
  </executions>
</plugin>
```

Naming conventions are configurable. Make them obvious, for example `*Test` for unit tests and `*IT` for integration tests. Run `./mvnw verify`, not only `integration-test`.

Useful distinctions:

- `-DskipTests` commonly skips running tests but may still compile them;
- `-Dmaven.test.skip=true` can skip both compilation and execution;
- exact behavior can be plugin-specific, so a release script should not hide skip flags.

## Gradle unit tests

```kotlin
dependencies {
    testImplementation("org.junit.jupiter:junit-jupiter:5.11.4")
}

tasks.test {
    useJUnitPlatform()
    failFast = false
    reports.junitXml.required = true
}
```

The Java plugin's `test` task contributes to `check`. An additional integration suite can be modeled explicitly. Gradle's JVM Test Suite API is documented as incubating in current releases, so label that boundary and consider a custom source set when API stability is required.

```kotlin
testing {
    suites {
        val integrationTest by registering(JvmTestSuite::class) {
            useJUnitJupiter()
            dependencies {
                implementation(project())
            }
            targets.all {
                testTask.configure {
                    shouldRunAfter(tasks.test)
                }
            }
        }
    }
}

tasks.check {
    dependsOn(testing.suites.named("integrationTest"))
}
```

`shouldRunAfter` communicates order when both tasks are scheduled; `check` depending on the suite makes execution part of the gate.

## Forking and parallelism

Parallel tests can reduce latency only when tests are isolated. Shared ports, static state, fixed database rows, wall-clock assumptions, and global files create nondeterminism.

Before increasing forks:

1. measure test duration and CPU/memory utilization;
2. classify tests by resource needs;
3. eliminate shared mutable state;
4. cap forks for CI container limits;
5. preserve per-test diagnostics;
6. compare failure and retry rates.

A retry plugin can reduce noise but can also normalize flaky behavior. Track the first-attempt failure rate and assign ownership.

## Coverage and static analysis

Coverage is evidence of execution, not correctness. Quality gates can include formatting, compiler warnings, static analysis, forbidden APIs, architecture tests, mutation testing, dependency checks, and coverage thresholds.

Avoid one giant opaque "quality" task. Developers should be able to reproduce each failing gate locally and understand the report path.

## Test environment inputs

Declare test inputs such as locale, timezone, encoding, environment variables, ports, and service endpoints. A test that reads undeclared machine state may pass locally, fail in CI, and poison caches.

```text
test result = code + test code + classpath + JVM + declared environment
```

Use temporary directories, random available ports, deterministic clocks, and isolated data. Do not log secrets while diagnosing test configuration.

## Failure scenarios

### Tests pass locally but not in CI

Compare wrapper version, launcher JDK, test JDK, locale, timezone, file-system case sensitivity, CPU/memory, parallelism, service availability, and active profiles/properties.

### Integration environment is left running

Check whether Maven stopped at `integration-test` instead of `verify`, or whether Gradle cleanup was modeled only after successful task action rather than as a finalizer or external pipeline cleanup.

### No tests executed

Verify discovery patterns, selected engine, JUnit Platform configuration, task filters, skip properties, and reports. A green task with zero discovered tests may need an explicit fail-on-no-tests policy.

## Interview drill

**Question:** Why does Maven have both Surefire and Failsafe?

**Strong answer:** Surefire runs unit tests in the `test` phase and can fail immediately. Failsafe models integration tests across `integration-test` and `verify`; it preserves the lifecycle opportunity for `post-integration-test` teardown before final verification fails. This is why the normal endpoint is `mvn verify`.

## Practice

1. **Foundation:** Assign unit and integration suites to both tools.
2. **Predict:** Does Gradle `shouldRunAfter` schedule the other task?
3. **Debugging:** A build reports success with zero tests. Create an evidence checklist.
4. **Interview Core:** Compare skip-test flags and explain release risk.
5. **SDE-2 Follow-up:** Design a 15-minute PR gate and a 60-minute post-merge gate.

## Readiness check

- [ ] I can explain Surefire/Failsafe lifecycle behavior.
- [ ] I model Gradle suite execution as part of `check`.
- [ ] I measure flakiness instead of hiding it with retries.
