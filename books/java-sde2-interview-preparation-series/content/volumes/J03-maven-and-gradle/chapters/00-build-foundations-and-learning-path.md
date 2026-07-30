# Build Foundations and the Learning Path

A Java build is a repeatable transformation from reviewed source code to verified artifacts. Maven and Gradle automate that transformation, but the tool is not the model. Before learning XML or Kotlin DSL, learn the questions every build must answer.

## Learning objectives

By the end of this chapter, you should be able to:

- distinguish source, dependency, task, output, artifact, and publication;
- explain why an IDE build is not a team build contract;
- draw a build as a dependency graph rather than a command list;
- identify the responsibilities shared by Maven and Gradle;
- follow the book from first build through SDE-2 incident diagnosis.

## The build contract

Consider a pricing service. Its repository contains Java source, tests, resources, and build configuration. A useful build contract states:

1. which tool and JDK execute the build;
2. which source files and generated inputs participate;
3. which external components are resolved;
4. which checks must pass;
5. which artifacts are produced;
6. which environment inputs are permitted;
7. how another machine reproduces the result.

```text
SOURCE + CONFIGURATION + TOOLCHAIN + DEPENDENCIES
                       |
                       v
             ordered, verified work
                       |
          +------------+-------------+
          |            |             |
          v            v             v
       classes       reports      JAR / metadata
```

If a developer can build only by clicking an undocumented IDE action, the process is not yet a reliable team contract. IDEs should import and delegate to the repository-owned build when practical.

## Shared vocabulary

| Term | Meaning |
|---|---|
| project | one buildable unit with configuration and outputs |
| module | a separately modeled component inside or beside a larger build |
| task / goal | executable unit of build work |
| lifecycle | ordered stages with defined meanings |
| dependency | an input produced elsewhere or by another module |
| repository | service or directory that stores dependency artifacts and metadata |
| artifact | a produced file such as a JAR, sources JAR, POM, or module metadata |
| coordinate | stable artifact identity, commonly group, name, and version |
| plugin | extension that contributes build behavior |
| toolchain | selected JDK or other tool used by build work |

The word repository is overloaded. A Git repository stores source history. An artifact repository stores packages such as JARs and their metadata. Maven's local repository is also not a source-control repository.

## Maven and Gradle at a glance

Maven starts from a Project Object Model, conventions, packaging, lifecycles, phases, and plugin goals. A command such as `mvn verify` requests a lifecycle phase, so Maven runs preceding phases and their bound goals.

Gradle starts from settings, projects, plugins, a model of tasks, and task dependencies. A command such as `./gradlew build` selects a task; Gradle initializes the build, configures the required model, creates a task graph, and executes scheduled work.

```text
Maven                            Gradle
POM + packaging                  settings + build scripts
       |                                  |
       v                                  v
lifecycle phases                 projects and task graph
       |                                  |
       v                                  v
plugin goals                     task actions
       +---------------+------------------+
                       v
               verified artifacts
```

Neither tool is universally superior. Maven often gives a predictable convention-heavy model. Gradle offers richer modeling and customization, which can improve large builds but also permits more accidental complexity.

## What an SDE-2 engineer is expected to know

An entry-level answer may show how to add a dependency. An SDE-2 answer should also explain:

- which compile, runtime, and test classpaths change;
- how transitive version conflicts are resolved;
- why the same build can differ across machines;
- how unit and integration checks are separated;
- how module boundaries affect compilation and delivery;
- why cache reuse is correct or unsafe;
- how credentials and private repositories are isolated;
- how to diagnose the first meaningful cause in a long failure log.

## The study route

```text
FOUNDATION
inputs -> work -> outputs -> artifacts -> repositories
   |
   v
TOOL BASICS
Maven POM/lifecycle <-> Gradle scripts/task graph
   |
   v
DEPENDENCIES AND TESTS
classpaths -> conflicts -> tests -> packaging
   |
   v
SCALE AND DELIVERY
modules -> toolchains -> CI -> cache -> publish -> security
   |
   v
SDE-2 JUDGMENT
diagnosis -> migration -> incident response -> interview defense
```

Read in order the first time. Every later chapter assumes that a build is a graph with explicit inputs and outputs, not a magic command.

## First inspection habit

When joining a Java repository, do not begin by globally upgrading plugins or deleting caches. Inspect:

```bash
git status --short --branch
find . -maxdepth 2 -name pom.xml -o -name settings.gradle.kts \
  -o -name settings.gradle -o -name build.gradle.kts \
  -o -name build.gradle
java --version
```

Then look for `mvnw` or `gradlew`. A checked-in wrapper is the project's declared entry point. Read its properties and repository policy before executing downloaded code.

## Interview drill

**Question:** Why do we need Maven or Gradle if `javac` and `jar` already exist?

**Strong answer:** `javac` and `jar` perform important individual operations. A build tool models the repeatable graph around them: source discovery, dependency resolution, generated inputs, multiple classpaths, tests, packaging, metadata, publication, incremental work, and team-wide configuration. The value is the declared and verifiable process, not merely fewer command-line characters.

## Common mistakes

- Saying Maven is only dependency management or Gradle is only a faster Maven.
- Treating `clean` as a correctness requirement for every build.
- Assuming a successful compile proves runtime dependencies are present.
- Depending on an IDE's private configuration.
- Using the globally installed tool when a wrapper is available.
- Calling an artifact cache a source of truth.

## Practice

1. **Foundation:** Draw inputs, work, and outputs for a one-class Java application.
2. **Foundation:** Explain the two meanings of repository used in this chapter.
3. **Interview Core:** Name three reasons a build can work locally and fail in CI.
4. **Debugging:** A teammate says, "Deleting everything fixed it." List the evidence still missing.
5. **SDE-2 Follow-up:** Define a minimal build contract for a library consumed by ten services.

## Readiness check

- [ ] I can explain the build without naming either tool.
- [ ] I distinguish Git, local artifact, and remote artifact repositories.
- [ ] I know why wrappers, JDK selection, and dependency graphs belong to correctness.
