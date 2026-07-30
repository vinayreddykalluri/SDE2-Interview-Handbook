# Gradle: Model, Lifecycle, Tasks, and Lazy Configuration

Gradle is easiest to reason about when you distinguish build scripts from task execution. A build script configures a model; it is not a shell script that executes top to bottom as the build's work.

## Files and responsibilities

| File | Typical responsibility |
|---|---|
| `settings.gradle.kts` | build identity, projects, plugin and dependency repositories |
| `build.gradle.kts` | project plugins, dependencies, tasks, publications |
| `gradle.properties` | project or Gradle behavior without secrets in VCS |
| `gradle/libs.versions.toml` | dependency and plugin aliases and requested versions |
| `gradle/wrapper/*` | selected Gradle distribution contract |
| `build-logic/` | reusable convention plugins in a larger build |

This book uses Kotlin DSL because it offers typed IDE support. You should still be able to recognize Groovy DSL in existing repositories.

## Three lifecycle phases

Gradle's broad lifecycle is:

```text
INITIALIZATION              CONFIGURATION                EXECUTION
discover builds/projects -> register/configure model -> run scheduled tasks
settings.gradle(.kts)       build.gradle(.kts)           task actions
```

Initialization determines participating projects and included builds. Configuration applies plugins and creates the task graph for requested work. Execution runs scheduled tasks in dependency order.

A common mistake is performing network calls or expensive file scans during configuration. They run even when the selected task does not need the result and can prevent configuration-cache reuse.

## Tasks and relationships

Tasks form a directed acyclic graph. Relationships have different meanings:

```kotlin
val generateSchema by tasks.registering {
    outputs.file(layout.buildDirectory.file("schema/schema.json"))
    doLast {
        // Generate the declared output.
    }
}

tasks.named("classes") {
    dependsOn(generateSchema)
}
```

- `dependsOn` expresses a required predecessor.
- `mustRunAfter` constrains order only when both tasks are scheduled.
- `shouldRunAfter` is a softer ordering preference.
- `finalizedBy` schedules cleanup or follow-up after another task.

Do not use ordering where a true data dependency exists. If task B consumes task A's output, model the input/output relationship or dependency explicitly.

Inspect before changing:

```bash
./gradlew projects
./gradlew tasks --all
./gradlew help --task test
./gradlew build --dry-run
./gradlew :app:dependencies
```

## Plugin contribution

Applying `java-library` adds source sets, configurations, tasks, and outgoing variants. It is not just a macro:

```kotlin
plugins {
    `java-library`
}

java {
    toolchain {
        languageVersion = JavaLanguageVersion.of(21)
    }
}
```

Prefer plugin IDs and published convention plugins over copy-pasted `allprojects` or `subprojects` blocks in large builds. Shared build logic should have tests, ownership, and versioning decisions like production code.

## Task registration versus eager creation

Prefer lazy registration and named configuration:

```kotlin
val verifyGeneratedFiles by tasks.registering {
    group = "verification"
    description = "Checks generated files without eager work"
}

tasks.named("check") {
    dependsOn(verifyGeneratedFiles)
}
```

Avoid eagerly traversing all tasks or calling `.get()` early without need. Lazy APIs let Gradle configure less work and support scalable builds.

## Providers and declared inputs

Environment or file values used by tasks should be modeled lazily and declared:

```kotlin
abstract class WriteBuildInfo : DefaultTask() {
    @get:Input
    abstract val revision: Property<String>

    @get:OutputFile
    abstract val outputFile: RegularFileProperty

    @TaskAction
    fun write() {
        outputFile.get().asFile.writeText(revision.get())
    }
}
```

Undeclared inputs cause stale up-to-date or cache hits. Undeclared outputs can collide across tasks. A fast wrong build is worse than a slow correct build.

## `doFirst`, `doLast`, and configuration confusion

This code prints during configuration:

```kotlin
println("configuring project")
```

This registers execution-time behavior:

```kotlin
tasks.register("explain") {
    doLast {
        println("executing task")
    }
}
```

Interviewers often ask why code in a Gradle script runs even when a different task is requested. The answer is the configuration phase.

## Build, configuration, and dependency caches

Do not merge these concepts:

- up-to-date checks reuse outputs already present in the current workspace;
- the build cache can restore task outputs from earlier compatible executions;
- the configuration cache can reuse the configured task graph;
- the dependency cache stores resolved artifacts and metadata.

Each has different keys, invalidation, and security boundaries.

## Debugging sequence

```bash
./gradlew build --stacktrace
./gradlew build --info
./gradlew build --scan        # only under approved data policy
./gradlew dependencies
./gradlew dependencyInsight \
  --dependency jackson-databind \
  --configuration runtimeClasspath
```

Start with `--stacktrace`, then `--info`. `--debug` can expose credentials and is rarely the first move. Build scans can be powerful but may upload metadata to an external service; follow organizational policy.

## Interview traps

- A Gradle build script is configuration code, not the task execution sequence.
- `build` is a task; `clean` is another task.
- `mustRunAfter` does not create a dependency.
- A version catalog does not force conflict-resolution outcomes by itself.
- `UP-TO-DATE` is not the same as `FROM-CACHE`.
- The daemon JVM and Java compilation toolchain can differ.
- Kotlin DSL does not make arbitrary custom build logic automatically safe or lazy.

## Practice

1. **Foundation:** Draw initialization, configuration, and execution for `./gradlew test`.
2. **Predict:** Will `mustRunAfter` cause an otherwise unrequested task to execute?
3. **Debugging:** A task reuses stale output. List the missing input/output declarations to investigate.
4. **Interview Core:** Compare `UP-TO-DATE`, `FROM-CACHE`, and `SKIPPED`.
5. **SDE-2 Follow-up:** Decide when shared logic belongs in a convention plugin rather than the root script.

## Readiness check

- [ ] I can explain why configuration code can run without task execution.
- [ ] I can distinguish task dependencies from ordering constraints.
- [ ] I treat providers, inputs, outputs, and lazy registration as correctness tools.
