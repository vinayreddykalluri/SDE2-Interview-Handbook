# Maven: Model, Lifecycle, Plugins, and Effective Build

Maven is predictable when you separate four ideas: the POM describes a project, packaging contributes default lifecycle bindings, phases name build stages, and plugin goals perform work.

## The POM is input to an effective model

The `pom.xml` you read is not always the complete configuration Maven executes. Maven combines defaults, parent inheritance, dependency and plugin management, active profiles, user or installation settings, and command-line properties into an effective model.

```text
current POM + parent POMs + defaults + active profiles + settings
                              |
                              v
                       effective POM
                              |
                              v
                  lifecycle and plugin execution
```

Inspect rather than guessing:

```bash
./mvnw help:effective-pom -Dverbose -Doutput=effective-pom.xml
./mvnw help:effective-settings -Doutput=effective-settings.xml
./mvnw help:active-profiles
./mvnw help:describe -Dplugin=compiler -Ddetail
```

Effective files may expose repository locations or other environment details. Do not commit diagnostic output blindly.

## Coordinates and packaging

The common identity is `groupId:artifactId:version`. Packaging defaults to `jar`; `pom` packaging is common for parents and aggregators. A coordinate names a component, while a classifier distinguishes an additional artifact such as `sources` under the same base coordinate.

Avoid encoding environments into artifact identity unless the release model truly requires different components. The same tested artifact should normally move across environments with runtime configuration supplied separately.

## Lifecycles and phases

Maven has built-in `default`, `clean`, and `site` lifecycles. The most important default phases are:

```text
validate -> compile -> test -> package -> verify -> install -> deploy
```

When you run `mvn verify`, Maven does not start at `verify`; it traverses earlier phases. A phase can have zero or more goals bound to it. A goal is plugin work such as `compiler:compile` or `surefire:test`.

Direct goal execution is valid for diagnosis and one-off operations:

```bash
./mvnw dependency:tree
./mvnw help:effective-pom
```

For normal delivery, prefer a lifecycle phase that expresses the desired outcome.

## Why `mvn integration-test` is usually the wrong endpoint

Integration-test infrastructure may be started in `pre-integration-test`, exercised in `integration-test`, stopped in `post-integration-test`, and checked in `verify`. Stopping at `integration-test` can bypass cleanup and final result checking. Invoke `verify` when this lifecycle is configured.

## Plugins, executions, and configuration

A plugin contains goals. An execution can bind goals to phases and supply configuration:

```xml
<plugin>
  <groupId>org.apache.maven.plugins</groupId>
  <artifactId>maven-enforcer-plugin</artifactId>
  <version>3.5.0</version>
  <executions>
    <execution>
      <id>enforce-build-contract</id>
      <phase>validate</phase>
      <goals><goal>enforce</goal></goals>
      <configuration>
        <rules>
          <requireJavaVersion><version>[21,22)</version></requireJavaVersion>
          <requireMavenVersion><version>[3.9,4)</version></requireMavenVersion>
        </rules>
      </configuration>
    </execution>
  </executions>
</plugin>
```

Pin build-plugin versions. A build whose source dependencies are fixed but whose plugin versions drift is not fully controlled.

## `plugins` versus `pluginManagement`

`build/plugins` activates or configures a plugin for the current project and inheriting children, subject to inheritance rules. `pluginManagement` centralizes defaults for a plugin but normally does not activate it by itself; a child still references the plugin under `plugins`.

The dependency equivalent is similar but not identical: `dependencyManagement` supplies versions and other managed values when a dependency is encountered. It does not automatically add the dependency to each module.

## Parent versus aggregator

A parent relationship uses `<parent>` and inheritance. Aggregation uses `<modules>` in Maven 3 terminology. One POM can perform both roles, but the concepts differ:

- inheritance shares configuration down a parent chain;
- aggregation collects projects for one reactor build;
- a module can inherit from a parent that is not its aggregator;
- an aggregator can build a module that inherits elsewhere.

This distinction is a frequent interview question because large repositories often blur it.

## Properties and profiles

Properties reduce duplication but can obscure ownership when overused. Profiles conditionally modify the model. Profile activation may depend on a property, JDK, operating system, file, or explicit flag.

Use profiles for genuine build variation, not for hiding environment-specific application configuration inside artifacts. Verify active profiles in incident diagnosis:

```bash
./mvnw help:active-profiles
./mvnw verify -Pquality-gates
```

## Settings, mirrors, and credentials

Project-wide static configuration belongs in the repository or a shared parent. User and environment configuration belongs in `settings.xml`, including mirrors, proxies, and server credentials. A `<server>` ID must match the repository or deployment ID that requests credentials.

Never place repository passwords in `pom.xml`. Treat the local Maven repository as a cache plus locally installed artifacts, not a canonical deployment target.

## Debugging sequence

When a Maven build surprises you:

1. reproduce with `./mvnw -version` and record the JDK;
2. run the narrow failing phase without unrelated flags;
3. inspect the first meaningful `Caused by` section, not only the last summary;
4. inspect active profiles and effective POM;
5. inspect dependency or plugin resolution separately;
6. use `-X` only after narrowing the problem because it is noisy and may reveal data;
7. compare local and CI settings, mirrors, toolchains, and environment inputs.

## Interview traps

- A phase is not a plugin goal.
- `clean` belongs to a separate lifecycle; `mvn clean verify` requests two lifecycles in sequence.
- `install` does not deploy to a shared remote repository.
- `dependencyManagement` does not automatically add a dependency.
- `pluginManagement` does not normally execute the plugin.
- The child POM alone is not the effective build.
- Maven profiles are not Spring profiles.

## Practice

1. **Foundation:** Explain phase, goal, plugin, and execution with one example each.
2. **Predict:** What earlier work runs for `mvn install`?
3. **Debugging:** A plugin executes only in CI. Which effective inputs do you compare?
4. **Interview Core:** Compare parent inheritance and reactor aggregation.
5. **SDE-2 Follow-up:** Design a company parent POM without forcing every plugin on every service.

## Readiness check

- [ ] I can trace a phase to bound goals.
- [ ] I can obtain and interpret the effective POM.
- [ ] I know where project configuration, machine settings, and credentials belong.
