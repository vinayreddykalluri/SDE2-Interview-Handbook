# Chapter 2: JDK, JRE, JVM, Editions, and Distributions

## Learning objectives

- Distinguish the JVM, runtime image, JRE concept, and JDK.
- Explain Java SE, Jakarta EE, OpenJDK, Oracle JDK, and vendor distributions.
- Reason about source, binary, class-file, runtime, and behavioral compatibility.
- Summarize the practical significance of Java 8, 11, 17, and 21.
- Select and use core JDK command-line tools during development and incidents.

## Why this matters at SDE-2

Production failures often hide behind imprecise statements such as "the Java version is the same." The build may use JDK 21 while emitting Java 17 class files; the container may use a different vendor build; an agent may depend on internal APIs; or a library may be binary compatible but behaviorally different. An SDE-2 should capture the complete runtime contract and use built-in tools before guessing.

The JDK/JRE/JVM question is common in interviews because it tests whether the candidate sees a layered platform. The senior answer also covers modern modular runtime images, class-file versions, LTS policy, distribution support, and diagnostic tooling.

## First-principles model

The JVM is the abstract execution target and its concrete implementation. A runtime needs that JVM plus core libraries and supporting files. A development kit adds compilers, packagers, documentation generators, debuggers, and diagnostics.

```text
JDK
+-- Java compiler and development/diagnostic tools
+-- Java runtime image
    +-- JVM implementation
    +-- standard modules and native support
```

Historically, vendors shipped a separately installable JRE. Since Java 9 introduced the module system and custom runtime images, production deployments frequently use a full JDK or an application-specific image produced by `jlink`. "JRE" remains a useful conceptual term, but one should not assume a separate official JRE download exists for every modern distribution.

## Core terminology

- **JVM:** Loads and executes class files according to the JVMS.
- **JRE:** The runtime concept: JVM plus libraries and resources needed to run applications.
- **JDK:** A development and diagnostic distribution containing a runtime and tools.
- **Java SE:** The standard language and platform APIs forming the base Java platform.
- **Jakarta EE:** Enterprise specifications layered on Java SE and implemented by compatible servers/frameworks; it is not another JVM.
- **OpenJDK:** The open-source reference implementation project and code base used by many builds.
- **Distribution:** A tested vendor build of OpenJDK, potentially with different packaging, support, patches, and optional components.
- **LTS:** A release for which a vendor offers a longer support window; the duration and terms are vendor policy.
- **Class-file version:** A version marker constraining which JVM releases can load a class.

## Detailed mechanics

Java SE includes the language, class-file/JVM contracts, and APIs such as collections, I/O, concurrency, networking, JDBC, and cryptography. Jakarta EE defines additional APIs for enterprise concerns such as persistence, transactions, dependency injection, REST, and messaging. A Jakarta EE specification requires an implementation. A Spring Boot service may use selected Jakarta APIs without running a full Jakarta EE application server.

OpenJDK is not synonymous with one downloadable binary. Oracle, Eclipse Temurin, Amazon Corretto, Azul Zulu, Microsoft Build of OpenJDK, Red Hat builds, BellSoft Liberica, and others distribute JDKs. GraalVM distributions add technologies such as a high-performance compiler and Native Image. IBM Semeru can use OpenJ9 in some editions rather than HotSpot. Selection factors include supported platforms, update cadence, cryptographic requirements, container images, support contracts, and operational validation.

Oracle JDK and OpenJDK builds share substantial code and conformance goals, but license and support terms must be evaluated for the chosen version and use. Do not infer licensing from the words "Java" or "OpenJDK" alone.

LTS is not a language property. Java 8, 11, 17, and 21 have been treated as LTS releases by major vendors, but update availability differs. Feature releases occur on the Java release cadence, and teams can choose to upgrade every feature release or remain on supported LTS lines.

Version compatibility has several dimensions:

- **Source compatibility:** Whether old source still compiles. New keywords, removed APIs, and stricter checks can matter.
- **Binary compatibility:** Whether previously compiled clients can link against a changed library. Removing a method or changing a superclass can break it even when source could be adjusted.
- **Class-file compatibility:** Whether the runtime supports the emitted class-file major version.
- **Runtime compatibility:** Whether required APIs, modules, native libraries, flags, and environment exist.
- **Behavioral compatibility:** Whether results and operational behavior remain acceptable despite implementation or library changes.

> **Specification boundary:** A JVM must reject an unsupported class-file version. The Java platform defines extensive compatibility rules, but it does not promise that every internal API, removed module, command-line flag, timing characteristic, or vendor extension remains available.

A compact release map:

| Release | Interview and production significance |
|---|---|
| Java 8 | Lambdas, streams, default interface methods, `java.time`; still a major historical baseline |
| Java 11 | Standard HTTP client, single-file source launch, post-Java-8 module-era baseline |
| Java 17 | Records, sealed classes, pattern matching for `instanceof`; common modern LTS baseline |
| Java 21 | Virtual threads, record patterns, pattern matching for `switch`, sequenced collections; modern LTS baseline |

Features arrived across intermediate releases. For example, records became final before Java 17; a release table is a migration summary, not a claim that every listed feature originated in that exact LTS release.

The essential tools form a pipeline:

- `javac`: compile source to class files; `--release 17` targets the documented Java 17 API and class-file level.
- `java`: launch a class, JAR, module, or source file in supported modes.
- `jar`: create, inspect, and update JAR archives.
- `javap`: inspect class signatures and bytecode; it is not a Java decompiler that reconstructs exact source.
- `javadoc`: generate API documentation from source comments and declarations.
- `jcmd`: send supported diagnostic commands to a running JVM.
- `jstack`: print Java thread stacks; `jcmd <pid> Thread.print` is often preferred in modern operations.
- `jmap`: inspect heap configuration or request heap information/dumps; collection impact must be considered.
- `jfr`: manage and inspect Java Flight Recorder files, depending on JDK version and command usage.

## Worked Java example

```java
public final class RuntimeIdentity {
    public static void main(String[] args) {
        System.out.println("runtime=" + Runtime.version());
        System.out.println("vm=" + System.getProperty("java.vm.name"));
        System.out.println("vendor=" + System.getProperty("java.vendor"));
        System.out.println("home=" + System.getProperty("java.home"));
    }
}
```

Compile for a Java 17 deployment while using a newer JDK:

```bash
javac --release 17 RuntimeIdentity.java
javap -verbose RuntimeIdentity.class
java RuntimeIdentity
```

`--release 17` is stronger than merely using `-source 17 -target 17`: it also constrains compilation against the documented Java 17 platform API signature set. It cannot validate every third-party library or environmental dependency.

## Execution or memory walkthrough

Suppose CI runs JDK 21 with `--release 17` and production runs a Java 17 runtime.

1. The compiler accepts Java 17 language constructs and documented Java 17 APIs.
2. It emits the class-file version associated with Java 17.
3. `javap -verbose` exposes that version and constant-pool metadata.
4. A Java 17 JVM can load the class if dependencies are compatible.
5. At launch, the host JDK supplies its own JVM implementation, core modules, native libraries, default properties, and security providers.
6. `Runtime.version()` reports the executing runtime, not the compiler that produced the class.

The JVM process has heap and non-heap state independent of the tool binaries on disk. Running `jcmd` attaches or communicates with that process through implementation-supported mechanisms; it is not executing inside application source semantics.

## Complexity and performance

Choosing a distribution should rarely depend on a single microbenchmark. Compare representative startup, steady-state throughput, tail latency, memory, collector behavior, and diagnostics under the exact release and flags. Vendor patches can change performance without changing language behavior.

A custom `jlink` image can reduce installed modules and distribution size, which may improve image transfer and attack surface. It also creates ownership: the exact module graph and update process become part of deployment. A full JDK consumes more disk space but keeps diagnostic tools nearby. Some organizations use a slim runtime image and an approved sidecar or matching diagnostic image.

## Edge cases and common mistakes

- Calling Jakarta EE a JDK or JVM edition. It is a set of specifications layered on Java SE.
- Assuming all OpenJDK distributions have identical support, packaging, certificates, fonts, or native integrations.
- Believing LTS means free updates forever. Support is vendor-specific.
- Compiling on JDK 21 with `-target 17` while accidentally referencing Java 21 APIs.
- Assuming a lower class-file target makes every dependency compatible.
- Parsing `java -version` manually when deployment metadata could record an exact immutable image digest.
- Shipping a JRE-only image and discovering incident tools are unavailable.
- Running heap-dump commands without checking disk space, data sensitivity, and pause impact.

## Production engineering notes

Record at least distribution, full version/build, architecture, base image digest, enabled modules, JVM flags, and collector for each service release. Reproduce incidents with the same build where possible. Patch releases contain security and correctness fixes, so "LTS" should support an update policy, not justify freezing forever.

Useful first-response commands include:

```bash
java -version
jcmd -l
jcmd 12345 VM.version
jcmd 12345 VM.flags
jcmd 12345 VM.system_properties
jcmd 12345 Thread.print
jcmd 12345 GC.heap_info
jfr print recording.jfr
```

Commands vary by release and JVM implementation. Confirm with `jcmd <pid> help`, test operational cost, protect diagnostic outputs, and avoid making write-heavy dumps during an already capacity-constrained incident unless justified.

> **HotSpot note:** Tools such as `jcmd`, HotSpot-specific commands, many `-XX` flags, and details of JFR integration are implementation/version specific. Their presence is not guaranteed by the JLS or JVMS.

## Interview questions and model answers

**What is the difference among JDK, JRE, and JVM?**

The JVM executes class files. A runtime combines a JVM with platform libraries and support files. A JDK contains a runtime plus development and diagnostic tools. In modern modular Java, a runtime may be a custom image rather than a separately branded JRE installation.

**Is OpenJDK different from Oracle JDK?**

OpenJDK is the open-source project/code base; Oracle JDK is a distribution. They share substantial implementation, but packaging, licensing, support, update schedules, and optional components must be assessed for the exact releases.

**Why can an `UnsupportedClassVersionError` occur?**

The class file was produced for a newer JVM class-file level than the runtime supports. The durable fix is to run a compatible newer runtime or compile the application and all dependencies for the intended older release.

**What does binary compatibility mean?**

It asks whether already compiled client code can link and run against a new library version without recompilation. It differs from source compatibility and does not guarantee identical behavior.

## Exercises

1. Install or inspect two JDK distributions and compare `java -version` output and available modules.
2. Compile a class with `--release 17`, inspect it with `javap -verbose`, and identify its major version.
3. Produce a JAR with a main class and run it using `java -jar`.
4. Design a runtime-version inventory record for a fleet of services.
5. Explain when a slim `jlink` image is worth the operational trade-off.
6. On a safe local process, use `jcmd` to inspect flags and thread stacks.

## Chapter summary

The JVM is the execution layer, a runtime combines it with required libraries, and the JDK adds engineering tools. Java SE defines the base platform; Jakarta EE adds enterprise specifications. OpenJDK underlies many distributions whose packaging and support still differ. Compatibility must be discussed across source, binary, class-file, runtime, and behavior dimensions. Exact version and distribution metadata are production requirements, not trivia.

## Revision checklist

- [ ] I can explain JDK, runtime/JRE, and JVM without relying on old packaging assumptions.
- [ ] I can distinguish Java SE, Jakarta EE, OpenJDK, and a distribution.
- [ ] I know why LTS is a vendor support concept.
- [ ] I can explain source, binary, class-file, runtime, and behavioral compatibility.
- [ ] I can summarize Java 8, 11, 17, and 21 accurately.
- [ ] I can select the right basic JDK diagnostic tool and state its risk.

