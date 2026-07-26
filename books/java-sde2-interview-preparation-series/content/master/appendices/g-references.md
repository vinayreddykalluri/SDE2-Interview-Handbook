# Appendix G - Primary References and Further Reading

This appendix is a map to primary sources, not a list of links to memorize. Use it when a statement in this book needs a precise boundary: whether code is legal Java, what bytecode must mean, what an API promises, what a particular JDK implements, or how a build and test tool behaves. Unless a link explicitly says otherwise, the Java links below target Java SE or JDK 21, the baseline used by this book.

## G.1 A Source Hierarchy for Interview-Quality Answers

Java documentation is published in several forms with different authority. Treating all of them as interchangeable leads to confident but inaccurate answers.

1. **Normative specifications** define the language and virtual machine. The Java Language Specification (JLS) answers questions such as whether an expression compiles, which overload is selected, and what synchronization orders memory actions. The Java Virtual Machine Specification (JVMS) defines class-file structure, loading, linking, initialization, instructions, and runtime data areas.
2. **Platform API specifications** define public contracts for Java SE types and methods. They are the first source for collection behavior, exception conditions, thread guarantees, stream requirements, and other library semantics. Read the entire class and method contract, including implementation requirements and API notes, while remembering that an implementation note is not necessarily a portable guarantee.
3. **OpenJDK JEPs** record the motivation, goals, alternatives, and delivery plan for JDK changes. A JEP is excellent design context, but it is not the final language or API specification. For a shipped feature, confirm the resulting contract in the JLS, JVMS, or Java SE API.
4. **JDK implementation and vendor documentation** describes tools and behavior of a distribution, commonly Oracle JDK or OpenJDK HotSpot. It is authoritative for that documented tool or implementation, not automatically for every Java implementation.
5. **Project documentation** is authoritative for Maven, Gradle, JUnit, JMH, and similar projects. Such documentation evolves independently of JDK 21. Pin the tool version used by a real repository and consult that version's manual.
6. **Source code** can explain an implementation, but it is not a substitute for a public contract. An interview answer should label implementation observations as such and avoid relying on internals unless the question explicitly asks about them.

A defensible answer often takes this form: "The API guarantees X; OpenJDK 21 commonly implements it using Y; code should rely on X." This separates portable reasoning from useful implementation knowledge.

## G.2 Java SE 21 Specification Entry Points

- **[Java Language Specification, Java SE 21](https://docs.oracle.com/javase/specs/jls/se21/html/)** - Normative language specification. Use it for syntax, types, conversions, declarations, generics, overload resolution, expressions, exceptions, definite assignment, records, patterns, and the Java Memory Model.
- **[Java Virtual Machine Specification, Java SE 21](https://docs.oracle.com/javase/specs/jvms/se21/html/)** - Normative JVM specification. Use it for class files, runtime data areas, loading, linking, initialization, verification, bytecode instructions, and execution semantics.
- **[Java SE 21 specifications hub](https://docs.oracle.com/en/java/javase/21/docs/specs/)** - Official index for Java SE specifications plus JDK-specific specifications and tool manuals. Check the classification shown on the page rather than assuming every linked document is part of the language specification.
- **[Java SE 21 API specification](https://docs.oracle.com/en/java/javase/21/docs/api/)** - Public platform API contracts. Start here whenever the question is about an interface, class, method, exception, ordering guarantee, null policy, thread-safety statement, or performance characteristic promised by an API.
- **[New API list in Java SE 21](https://docs.oracle.com/en/java/javase/21/docs/api/new-list.html)** - Version-specific inventory of added APIs. It is useful when separating Java 21 capabilities from APIs introduced earlier or later.
- **[Deprecated API list in Java SE 21](https://docs.oracle.com/en/java/javase/21/docs/api/deprecated-list.html)** - Official deprecation inventory and replacement guidance. Deprecation does not by itself mean immediate removal.
- **[JDK 21 tool specifications](https://docs.oracle.com/en/java/javase/21/docs/specs/man/index.html)** - Oracle JDK tool manuals for commands such as `java`, `javac`, `jcmd`, and `jfr`. These describe the JDK distribution and its tools; they are not the JLS.
- **[`javac` command specification](https://docs.oracle.com/en/java/javase/21/docs/specs/man/javac.html)** - Compiler options, source compatibility, class paths, module paths, annotation processing, and `--release`. Use it to make build claims precise.
- **[Java Object Serialization Specification for Java SE 21](https://docs.oracle.com/en/java/javase/21/docs/specs/serialization/)** - Serialization stream format, object graph rules, versioning, hooks, and security considerations. Prefer explicit formats for many new systems, but consult this specification when legacy Java serialization is actually in scope.

## G.3 Language Topics in the JLS

The JLS is dense. The following direct chapter links make it practical during study and review.

- **[Chapter 4: Types, Values, and Variables](https://docs.oracle.com/javase/specs/jls/se21/html/jls-4.html)** - Primitive and reference types, type variables, parameterized types, reifiable types, intersections, arrays, and the relationship between compile-time types and runtime classes.
- **[Chapter 5: Conversions and Contexts](https://docs.oracle.com/javase/specs/jls/se21/html/jls-5.html)** - Widening, narrowing, boxing, unboxing, capture conversion, assignment conversion, invocation contexts, casting, and numeric promotion. Use it to resolve "does this compile?" questions rather than relying on intuition.
- **[Chapter 6: Names](https://docs.oracle.com/javase/specs/jls/se21/html/jls-6.html)** - Scope, shadowing, hiding, name classification, accessibility, and qualified names.
- **[Chapter 8: Classes](https://docs.oracle.com/javase/specs/jls/se21/html/jls-8.html)** - Fields, methods, constructors, inheritance, overriding, initialization order, nested classes, enum classes, and record classes.
- **[Chapter 9: Interfaces](https://docs.oracle.com/javase/specs/jls/se21/html/jls-9.html)** - Interface inheritance, default methods, functional interfaces, annotations, and annotation interfaces.
- **[Chapter 10: Arrays](https://docs.oracle.com/javase/specs/jls/se21/html/jls-10.html)** - Array covariance, runtime component types, creation, initialization, and access.
- **[Chapter 11: Exceptions](https://docs.oracle.com/javase/specs/jls/se21/html/jls-11.html)** - Checked and unchecked exceptions, exception analysis, abrupt completion, and precise rethrow.
- **[Chapter 12: Execution](https://docs.oracle.com/javase/specs/jls/se21/html/jls-12.html)** - Loading, linking, initialization, object creation, finalization terminology, and program exit. Pair it with JVMS Chapter 5 for runtime details.
- **[Chapter 14: Blocks, Statements, and Patterns](https://docs.oracle.com/javase/specs/jls/se21/html/jls-14.html)** - Control flow, switch statements and expressions, pattern variables, reachability, and definite assignment interactions.
- **[Chapter 15: Expressions](https://docs.oracle.com/javase/specs/jls/se21/html/jls-15.html)** - Evaluation order, method invocation, overload resolution, lambdas, method references, operators, casts, switch expressions, and pattern-related expressions.
- **[Chapter 17: Threads and Locks](https://docs.oracle.com/javase/specs/jls/se21/html/jls-17.html)** - Synchronization actions, happens-before, volatile variables, final-field semantics, wait sets, and the formal Java Memory Model. This is the primary source for visibility and ordering claims.
- **[Chapter 18: Type Inference](https://docs.oracle.com/javase/specs/jls/se21/html/jls-18.html)** - Constraint formulas and inference variables behind generic method calls, lambdas, method references, and diamond inference. Use it after first explaining the practical result in plain language.

For interview preparation, read the introductory portions and the exact subsection relevant to an example. Do not quote a nearby rule without checking its exceptions and defined terms.

## G.4 JVM Topics in the JVMS

- **[Chapter 2: JVM Structure](https://docs.oracle.com/javase/specs/jvms/se21/html/jvms-2.html)** - Runtime data areas, frames, stacks, heaps, method areas, run-time constant pools, native stacks, and the JVM's abstract architecture. It deliberately leaves many implementation choices open.
- **[Chapter 3: Compiling for the JVM](https://docs.oracle.com/javase/specs/jvms/se21/html/jvms-3.html)** - Illustrative mappings from Java constructs to bytecode. It helps connect source-level reasoning to operand-stack execution, but does not mandate one compiler strategy for every construct.
- **[Chapter 4: Class File Format](https://docs.oracle.com/javase/specs/jvms/se21/html/jvms-4.html)** - Class-file layout, constant pools, descriptors, attributes, stack-map frames, verification constraints, and binary representation.
- **[Chapter 5: Loading, Linking, and Initializing](https://docs.oracle.com/javase/specs/jvms/se21/html/jvms-5.html)** - Class and interface loading, verification, preparation, resolution, class-loader constraints, and initialization. Use it for class-loader identity and initialization-trigger questions.
- **[Chapter 6: JVM Instruction Set](https://docs.oracle.com/javase/specs/jvms/se21/html/jvms-6.html)** - Normative definitions of bytecode instructions, their operands, stack effects, and exceptional behavior.
- **[Chapter 7: Opcode Mnemonics](https://docs.oracle.com/javase/specs/jvms/se21/html/jvms-7.html)** - Compact opcode reference useful while reading `javap` output.
- **[Oracle `javap` manual for JDK 21](https://docs.oracle.com/en/java/javase/21/docs/specs/man/javap.html)** - Tool documentation for disassembling class files. Use `javap -c -v` as an observation aid, then interpret output using the JVMS.

The JVMS describes an abstract machine, not HotSpot's exact memory layout or JIT algorithms. Statements about TLABs, compressed object pointers, tiered compilation, or a collector's regions belong to implementation documentation and should be labeled accordingly.

## G.5 Java 21 Feature Design Records

These OpenJDK JEP pages are primary design records. They explain goals and tradeoffs well, but the final JLS and API remain the contract for shipped behavior.

- **[JEP 431: Sequenced Collections](https://openjdk.org/jeps/431)** - Motivation and design for uniform first/last/reversed operations across ordered collections. Follow with the Java SE 21 `SequencedCollection`, `SequencedSet`, and `SequencedMap` API contracts.
- **[JEP 439: Generational ZGC](https://openjdk.org/jeps/439)** - Rationale and goals for adding generations to ZGC in JDK 21. Use the JDK's GC documentation and measurements from the target workload before recommending a collector.
- **[JEP 440: Record Patterns](https://openjdk.org/jeps/440)** - Final design for record-pattern deconstruction in Java 21. Confirm typing, applicability, and match behavior in the JLS.
- **[JEP 441: Pattern Matching for `switch`](https://openjdk.org/jeps/441)** - Final design for type patterns, guarded cases, null handling, dominance, and enhanced exhaustiveness in Java 21. Use JLS Chapters 14 and 15 for normative rules.
- **[JEP 444: Virtual Threads](https://openjdk.org/jeps/444)** - Goals, scheduling model, observability, and migration guidance for virtual threads finalized in JDK 21. Pair it with the `Thread` API and Oracle's virtual-thread guide.

Earlier feature records often referenced by SDE-2 interviews include **[JEP 361: Switch Expressions](https://openjdk.org/jeps/361)**, **[JEP 394: Pattern Matching for `instanceof`](https://openjdk.org/jeps/394)**, **[JEP 395: Records](https://openjdk.org/jeps/395)**, and **[JEP 409: Sealed Classes](https://openjdk.org/jeps/409)**. Their historical examples clarify design intent; use the Java 21 JLS for the consolidated rules.

## G.6 Core Library API Maps

- **[`java.util` package specification](https://docs.oracle.com/en/java/javase/21/docs/api/java.base/java/util/package-summary.html)** - Collections, comparators, optionals, random generation, utilities, and common value types. Read the collection-interface contracts before relying on a concrete implementation.
- **[`SequencedCollection` API](https://docs.oracle.com/en/java/javase/21/docs/api/java.base/java/util/SequencedCollection.html)**, **[`SequencedSet` API](https://docs.oracle.com/en/java/javase/21/docs/api/java.base/java/util/SequencedSet.html)**, and **[`SequencedMap` API](https://docs.oracle.com/en/java/javase/21/docs/api/java.base/java/util/SequencedMap.html)** - Java 21 contracts for encounter-ordered collection operations and reversed views.
- **[`java.util.stream` package specification](https://docs.oracle.com/en/java/javase/21/docs/api/java.base/java/util/stream/package-summary.html)** - Stream pipelines, laziness, non-interference, statelessness, ordering, reduction, collectors, and parallel execution. It is the primary source for behavioral constraints on stream lambdas.
- **[`java.util.concurrent` package specification](https://docs.oracle.com/en/java/javase/21/docs/api/java.base/java/util/concurrent/package-summary.html)** - Executors, queues, synchronizers, concurrent collections, atomics, futures, and package-level memory-consistency guarantees.
- **[`Thread` API for Java 21](https://docs.oracle.com/en/java/javase/21/docs/api/java.base/java/lang/Thread.html)** - Platform and virtual thread creation, lifecycle, interruption, joining, uncaught exceptions, and method contracts.
- **[`Executors` API](https://docs.oracle.com/en/java/javase/21/docs/api/java.base/java/util/concurrent/Executors.html)** - Executor factory contracts, including the virtual-thread-per-task executor. Factory convenience does not remove the need to reason about admission control and downstream capacity.
- **[`CompletableFuture` API](https://docs.oracle.com/en/java/javase/21/docs/api/java.base/java/util/concurrent/CompletableFuture.html)** - Completion-stage composition, execution policies, cancellation behavior, and exceptional completion.
- **[`java.nio` package specification](https://docs.oracle.com/en/java/javase/21/docs/api/java.base/java/nio/package-summary.html)** - Buffers, channels, charsets, non-blocking I/O foundations, and related contracts.
- **[`Files` API](https://docs.oracle.com/en/java/javase/21/docs/api/java.base/java/nio/file/Files.html)** - File operations, streams over directories and lines, copy/move behavior, attributes, and resource-closing requirements.
- **[`ByteBuffer` API](https://docs.oracle.com/en/java/javase/21/docs/api/java.base/java/nio/ByteBuffer.html)** - Position, limit, capacity, slicing, byte order, heap versus direct buffers, and view semantics.
- **[`java.io` package specification](https://docs.oracle.com/en/java/javase/21/docs/api/java.base/java/io/package-summary.html)** - Byte and character streams, readers, writers, serialization APIs, and closeable resources.
- **[`java.time` package specification](https://docs.oracle.com/en/java/javase/21/docs/api/java.base/java/time/package-summary.html)** - Immutable date-time types, time zones, clocks, durations, periods, parsing, and thread-safety guidance.
- **[`java.sql` package specification](https://docs.oracle.com/en/java/javase/21/docs/api/java.sql/java/sql/package-summary.html)** and **[`javax.sql` package specification](https://docs.oracle.com/en/java/javase/21/docs/api/java.sql/javax/sql/package-summary.html)** - JDBC contracts, data sources, connections, statements, result sets, transactions, and row sets. Database isolation and locking behavior also require the target database's documentation.

When an API page states average or expected complexity, null handling, optional operations, encounter order, or concurrency guarantees, cite that statement. Otherwise, present complexity as an implementation-specific expectation and name the implementation.

## G.7 Virtual Threads and Concurrency Guidance

- **[Oracle Java 21 virtual threads guide](https://docs.oracle.com/en/java/javase/21/core/virtual-threads.html)** - JDK 21 guidance on creating virtual threads, representing tasks, scheduling, pinning, thread-local usage, and observability. This is Oracle JDK guidance, while JEP 444 and the API describe design and contract.
- **[Java Memory Model in JLS 17.4](https://docs.oracle.com/javase/specs/jls/se21/html/jls-17.html#jls-17.4)** - Normative model for inter-thread actions, synchronization order, happens-before, data races, and correctly synchronized programs.
- **[`Lock` API](https://docs.oracle.com/en/java/javase/21/docs/api/java.base/java/util/concurrent/locks/Lock.html)** - Explicit-lock semantics and memory synchronization effects. Read each implementation's fairness, interruptibility, and condition behavior separately.
- **[`AtomicInteger` API](https://docs.oracle.com/en/java/javase/21/docs/api/java.base/java/util/concurrent/atomic/AtomicInteger.html)** and **[`VarHandle` API](https://docs.oracle.com/en/java/javase/21/docs/api/java.base/java/lang/invoke/VarHandle.html)** - Atomic operations and explicit memory-ordering modes. Prefer the simplest ordering that is demonstrably correct; do not infer compound-operation atomicity from individual atomic calls.

For concurrency questions, give the invariant first, identify the synchronization edge that protects it, and then cite the applicable API or JLS rule. A schedule that "usually works" is not a correctness proof.

## G.8 HotSpot, Garbage Collection, and Runtime Tuning

- **[Java 21 HotSpot Virtual Machine guide](https://docs.oracle.com/en/java/javase/21/vm/index.html)** - Oracle documentation for the HotSpot implementation shipped with JDK 21. Use it for runtime implementation topics, not as a universal JVM specification.
- **[Java 21 HotSpot Garbage Collection Tuning Guide](https://docs.oracle.com/en/java/javase/21/gctuning/)** - Collector selection, ergonomics, generations, pause goals, throughput, footprint, and tuning workflow for HotSpot. Treat flags and defaults as JDK-version-specific.
- **[G1 Garbage Collector tuning](https://docs.oracle.com/en/java/javase/21/gctuning/garbage-first-garbage-collector-tuning.html)** - Oracle guidance for diagnosing and tuning G1. Begin with evidence from GC logs and workload goals before changing flags.
- **[JEP 439: Generational ZGC](https://openjdk.org/jeps/439)** - JDK 21 design context for generational ZGC. A JEP's goals are not a latency guarantee for a specific service.
- **[OpenJDK 21 GA source tree](https://github.com/openjdk/jdk/tree/jdk-21-ga)** - Primary implementation source for OpenJDK 21. It can answer implementation questions after the specification boundary is clear. Internal classes and algorithms may change without preserving source-level compatibility.

A tuning claim should name the JDK build, collector, heap constraints, allocation rate, traffic profile, and measured outcome. Avoid universal prescriptions such as "collector X is always faster" or "larger heaps always reduce latency."

## G.9 Diagnostics, JFR, JMC, and `jcmd`

- **[Java 21 troubleshooting diagnostic tools](https://docs.oracle.com/en/java/javase/21/troubleshoot/diagnostic-tools.html)** - Oracle's overview of command-line and graphical diagnostic facilities. It is a good starting map during an incident.
- **[`jcmd` manual for JDK 21](https://docs.oracle.com/en/java/javase/21/docs/specs/man/jcmd.html)** - Authoritative Oracle JDK command documentation for sending diagnostic requests to a running JVM. Obtain the commands supported by the target process rather than assuming every build exposes the same set.
- **[`jfr` command manual for JDK 21](https://docs.oracle.com/en/java/javase/21/docs/specs/man/jfr.html)** - Recording-file inspection, summary, printing, assembly, disassembly, and metadata commands.
- **[JDK Flight Recorder API Programmer's Guide](https://docs.oracle.com/en/java/javase/21/jfapi/)** - Oracle guide to consuming recordings and creating custom JFR events using the `jdk.jfr` API.
- **[JFR recording configurations](https://docs.oracle.com/en/java/javase/21/jfapi/flight-recorder-configurations.html)** - Guidance on predefined and custom recording settings. Choose settings based on diagnostic goals and validate overhead in the target environment.
- **[JDK Mission Control documentation](https://docs.oracle.com/en/java/java-components/jdk-mission-control/index.html)** - Oracle documentation and releases for JMC, a separately distributed tool for inspecting JFR data and JVM behavior. It is not part of the Java language specification and may have its own compatibility matrix.
- **[`jstack` manual for JDK 21](https://docs.oracle.com/en/java/javase/21/docs/specs/man/jstack.html)** and **[`jmap` manual for JDK 21](https://docs.oracle.com/en/java/javase/21/docs/specs/man/jmap.html)** - JDK tool references for thread and memory diagnostics. Their manuals label these tools experimental and unsupported; prefer supported diagnostic paths such as appropriate `jcmd` operations where available.

During diagnosis, capture evidence before tuning: timestamps, service symptoms, JDK/build, command used, recording duration, load conditions, and relevant configuration. Correlate JFR, GC, application, and infrastructure signals instead of drawing a conclusion from one isolated sample.

## G.10 Microbenchmarking with JMH

- **[OpenJDK JMH project](https://openjdk.org/projects/code-tools/jmh/)** - Official project page for the Java Microbenchmark Harness. JMH is an OpenJDK Code Tools project, not a Java SE API.
- **[Official JMH repository and README](https://github.com/openjdk/jmh)** - Build instructions, supported modes, annotations, and project guidance. Use the generated harness and the recommended build setup.
- **[Official JMH samples](https://github.com/openjdk/jmh/tree/master/jmh-samples/src/main/java/org/openjdk/jmh/samples)** - Executable examples covering dead-code elimination, constant folding, state scope, setup, parameters, profilers, and other benchmark traps.

JMH handles much of the measurement machinery, but it cannot decide whether a benchmark represents a production workload. State the hypothesis, isolate the operation carefully, consume results, parameterize realistic inputs, inspect allocation and generated behavior where relevant, use forks, report uncertainty, and retain the full environment. Benchmark results are evidence for the tested configuration, not timeless truths about a Java construct.

## G.11 Security and Serialization

- **[Java SE 21 security developer guide](https://docs.oracle.com/en/java/javase/21/security/)** - Oracle's versioned guide to Java security APIs and mechanisms, including cryptography, providers, secure communication, authentication, and related topics.
- **[Java security overview for JDK 21](https://docs.oracle.com/en/java/javase/21/security/java-security-overview1.html)** - Architecture and terminology for security providers, cryptographic services, public-key infrastructure, secure communication, and access control.
- **[Oracle Secure Coding Guidelines for Java SE](https://www.oracle.com/java/technologies/javase/seccodeguide.html)** - Vendor-maintained secure coding guidance. It is useful for threat-aware design but is updated independently of the JLS and should be checked for its applicable platform scope.
- **[Java serialization filters guide for JDK 21](https://docs.oracle.com/en/java/javase/21/core/java-serialization-filters.html)** - Oracle guidance for allowlists, reject lists, resource limits, and filter factories when native serialization cannot be avoided.
- **[Java Object Serialization Specification](https://docs.oracle.com/en/java/javase/21/docs/specs/serialization/)** - The detailed stream and object-graph contract. Use it alongside, not instead of, a threat model for untrusted input.

Security behavior changes with JDK updates, library versions, deployment configuration, and threat intelligence. For a real system, also consult supported-version advisories and the primary documentation for the framework, container, operating system, identity provider, and protocol in use.

## G.12 Maven Primary Documentation

- **[Maven build lifecycle guide](https://maven.apache.org/guides/introduction/introduction-to-the-lifecycle.html)** - Default, clean, and site lifecycles; phases; goals; packaging; and safe command selection.
- **[Maven dependency mechanism guide](https://maven.apache.org/guides/introduction/introduction-to-dependency-mechanism.html)** - Transitive dependencies, mediation, scopes, management, optional dependencies, and exclusions.
- **[Maven reproducible builds guide](https://maven.apache.org/guides/mini/guide-reproducible-builds.html)** - Project configuration and verification practices for reproducible artifacts.
- **[Maven Wrapper documentation](https://maven.apache.org/wrapper/)** - Running a project with a declared Maven distribution rather than depending on an arbitrary machine-wide installation.

Maven's online documentation can describe the current Maven line rather than the version in an older repository. Confirm the wrapper or CI version, plugin versions, effective POM, active profiles, and repository settings before diagnosing resolution or lifecycle behavior.

## G.13 Gradle Primary Documentation

- **[Gradle User Manual](https://docs.gradle.org/current/userguide/)** - Official entry point for builds, tasks, plugins, dependency management, performance, authoring, and reference material.
- **[Gradle dependency management](https://docs.gradle.org/current/userguide/core_dependency_management.html)** - Configurations, variants, metadata, repositories, constraints, platforms, and dependency resolution.
- **[Gradle dependency locking](https://docs.gradle.org/current/userguide/dependency_locking.html)** - Lock-state generation, update, use, and limitations for repeatable resolution.
- **[Gradle dependency verification](https://docs.gradle.org/current/userguide/dependency_verification.html)** - Checksums, signatures, trust configuration, and verification metadata for supply-chain protection.
- **[Gradle Java toolchains](https://docs.gradle.org/current/userguide/toolchains.html)** - Declaring, discovering, selecting, and provisioning Java installations for compilation, testing, and execution.
- **[Gradle security guidance](https://docs.gradle.org/current/userguide/security.html)** - Official guidance on wrapper integrity, dependency verification, credentials, and other build-security concerns.

The `current` URLs follow Gradle's current release. For a repository, use its wrapper version and switch the documentation selector to that exact version before relying on DSL behavior or defaults.

## G.14 JUnit Primary Documentation

- **[JUnit official site](https://junit.org/)** - Project entry point for releases, documentation, community information, and related modules.
- **[JUnit User Guide](https://docs.junit.org/current/user-guide/)** - Official guide for the JUnit Platform, Jupiter programming and extension models, parameterized tests, suites, build integration, IDE support, parallel execution, and migration topics.

The `current` guide moves with JUnit releases. Pin the version used by the build and open the matching versioned guide for exact annotations, engines, launcher APIs, and configuration parameters. In an interview, distinguish the JUnit Platform, a test engine such as Jupiter, and the assertions or mocking libraries selected by the project.

## G.15 Suggested Reading Routes

Use a route based on the question rather than reading every source front to back.

- **Language puzzle:** Compile a minimal example with JDK 21, then consult JLS Chapters 4, 5, 8, 14, 15, or 18. Explain the rule and one practical implication.
- **Memory visibility or race:** State the shared invariant, identify synchronization actions, use JLS 17.4 and the relevant `java.util.concurrent` contract, and test only as supporting evidence.
- **Collection behavior:** Start at the interface contract, then the concrete API, then JEP 431 if design motivation matters. Do not infer a guarantee merely from current source code.
- **Class loading or bytecode:** Use JVMS Chapters 4 through 6. Use `javap` to inspect an example and label OpenJDK-specific observations.
- **Java 21 feature:** Read the relevant JEP for motivation, then the JLS or API for final semantics, and finally a JDK guide for operational advice.
- **GC or latency incident:** Read the HotSpot VM and GC guides, collect JFR and GC evidence with documented tools, and evaluate the exact deployed JDK under representative load.
- **Microbenchmark:** Start with JMH's README and samples. Pre-register the question, preserve benchmark code and environment, and report distributions or uncertainty rather than only the best score.
- **Build failure:** Identify the wrapper and runtime versions, then use the exact Maven or Gradle documentation. Capture dependency insight, effective configuration, repositories, and toolchain selection.
- **Test design:** Use the version-matched JUnit guide, separate unit from integration boundaries, and make assertions about externally meaningful behavior.
- **Security decision:** Start with a threat model and current supported-version guidance. Use the Java security and serialization sources for platform behavior, then consult the primary documentation of every external component involved.

The final habit is version discipline. A precise source from the wrong JDK, collector, database, build tool, or test framework can still produce the wrong answer. Record the relevant versions, distinguish guarantees from observations, and prefer the smallest authoritative source that directly answers the question.
