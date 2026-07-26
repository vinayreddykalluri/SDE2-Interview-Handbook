# Appendix F - Glossary

## F.1 Reading the glossary

Definitions describe the Java 21 platform unless a version is named. "HotSpot" identifies common OpenJDK implementation behavior rather than a Java specification guarantee. Chapter references point to the principal treatment; many concepts recur throughout the book.

## A

- **Abstract class:** A class declared `abstract` that cannot be instantiated directly and may combine implemented methods, state, constructors, and abstract methods for subclasses. See Chapter 18.
- **Abstraction:** A model exposing behavior that clients need while hiding representation or mechanism. Good abstractions state invariants, ownership, failure, and performance boundaries. See Chapter 49.
- **Access modifier:** `public`, `protected`, or `private`. Where the declaration rules permit it, omitting an access modifier gives package access; interface members and implicitly declared constructors have additional rules. Modifiers control source-level member/type accessibility, not network authorization or runtime data secrecy. See Chapter 16.
- **ACID:** Atomicity, consistency, isolation, and durability: goals used to reason about database transactions. Exact isolation and durability depend on the database and configuration. See Chapter 50.
- **Active use:** An action that can trigger class or interface initialization, such as selected static field access, static method invocation, instance creation, or reflective request. See Chapter 5.
- **Adapter:** A design pattern translating one contract into another, commonly isolating a vendor API from domain policy. A good adapter prevents vendor types from leaking inward. See Chapter 49.
- **Allocation rate:** Bytes or objects allocated per time. High allocation can drive GC work without implying a retention leak. Correlate it with live-set and latency evidence. See Chapters 39 and 41.
- **Amortized analysis:** Averaging total cost over an operation sequence. Dynamic-array append is amortized constant time even though an individual resize copies many references. See Chapters 26 and 42.
- **Annotation:** Metadata attached to declarations, types, or other supported locations. Retention and target control where it is available; behavior comes from compilers, runtimes, or frameworks. See Chapter 21.
- **API contract:** Observable promises of a type or operation: valid inputs, results, failures, side effects, ordering, mutation, blocking, ownership, and thread safety. See Chapter 49.
- **Array covariance:** Java permits a `Sub[]` where a `Super[]` is expected. Runtime store checks preserve type safety and can throw `ArrayStoreException`; generics are invariant. See Chapters 15 and 22.
- **Atomic operation:** An operation observed as indivisible under its stated concurrency model. Atomicity of one method does not make a multi-call invariant atomic. See Chapters 35 and 37.
- **Atomicity:** All-or-nothing behavior within a defined boundary. CPU atomics, Java atomic classes, and database transactions have different scopes and guarantees. See Chapters 35 and 50.
- **Autoboxing:** Compiler conversion from a primitive to its wrapper, such as `int` to `Integer`. It can add allocation, null, identity, and overload-selection surprises. See Chapter 12.

## B

- **Backpressure:** A policy that slows, rejects, blocks, sheds, or durably spills production when consumers lack capacity. It prevents an unbounded queue from hiding overload. See Chapters 36 and 52.
- **Balanced tree:** A search tree with structural constraints keeping height logarithmic. Java's API need not prescribe the exact balancing algorithm of every ordered collection. See Chapters 28 and 45.
- **Behavioral subtyping:** Requirement that a subtype preserve the supertype's preconditions, postconditions, invariants, failures, and relevant timing/side-effect behavior. This is central to LSP. See Chapter 49.
- **Big-O notation:** An asymptotic upper-bound language that suppresses constant factors and lower-order terms. Always state input variable, operation, and assumptions. See Chapter 42.
- **Binary heap:** A complete tree, usually array-backed, satisfying a parent-child priority invariant. It exposes an extremum efficiently but is not globally sorted. See Chapters 29 and 45.
- **Binary search tree:** A tree whose left and right subtrees are ordered relative to each node under a comparator. Without balancing, height can become linear. See Chapters 28 and 45.
- **Blocking:** Waiting while a thread cannot make current progress, for example on I/O, a lock, or a queue. Blocking semantics need timeout, interruption, and capacity policy. See Chapters 33 and 37.
- **Boxing:** Explicit or implicit creation/use of a wrapper representation for a primitive value. Primitive streams and specialized collections/arrays can avoid some boxing overhead. See Chapters 12 and 31.

## C

- **Cache locality:** Performance benefit from accessing nearby or recently used memory. Array-backed collections generally have better locality than node-heavy linked structures. See Chapters 26 and 39.
- **Capacity:** Allocated storage or admitted workload limit, distinct from current logical size. Explicit capacity is also a reliability boundary for queues, pools, and caches. See Chapters 26, 29, and 52.
- **CAS:** Compare-and-set, an atomic conditional update that succeeds only if the observed value still equals an expected value. Algorithms must handle failure and possible ABA concerns. See Chapter 35.
- **Checked exception:** A `Throwable` subtype that is neither a `RuntimeException` subtype nor an `Error` subtype. Java's compile-time checking generally requires callers to catch or declare checked exceptions. They should represent meaningful recoverable boundaries. See Chapter 20.
- **Class:** A Java reference-type declaration defining members, constructors, inheritance, and implementation. At runtime a class is associated with its defining loader. See Chapters 16 and 5.
- **Class file:** The binary format containing JVM instructions, constants, metadata, and verification information for a class or interface. It is specified by the JVMS. See Chapter 3.
- **Class initialization:** Execution of static field initializers and static blocks under the language's initialization procedure. Successful initialization has important inter-thread visibility guarantees. See Chapter 5.
- **Class loader:** Runtime component that creates a `Class` from binary data and establishes a namespace. The same binary name under different defining loaders denotes different runtime types. See Chapter 5.
- **Class path:** Ordered set of locations searched by class-loading tools in class-path mode. Duplicate classes and ordering can make resolution fragile; modules provide another model. See Chapters 2 and 51.
- **Class variable:** A `static` field associated with a class rather than each instance. Shared mutability requires an explicit concurrency and lifecycle policy. See Chapters 16 and 33.
- **Class-loader leak:** Retention of an otherwise obsolete loader through threads, statics, registries, drivers, or callbacks, thereby retaining all classes and related metadata it defines. See Chapter 41.
- **Closure:** Function-like behavior together with captured lexical values. A Java lambda can capture only final or effectively final local variables, though captured objects may be mutable. See Chapter 23.
- **Code cache:** HotSpot native memory storing generated machine code and related metadata. Its organization and tuning are implementation-specific. See Chapters 10 and 40.
- **Cohesion:** Degree to which a component's responsibilities belong together and change for related reasons. High cohesion makes invariants and ownership easier to locate. See Chapter 49.
- **Collision:** In hashing, distinct unequal keys mapping to the same hash region. Correct tables resolve collisions using equality; excessive collisions degrade performance. See Chapter 27.
- **Comparable:** Interface defining a type's natural ordering through `compareTo`. Natural order should normally be consistent with `equals`, especially for sorted collection use. See Chapter 30.
- **Comparator:** Object defining an external ordering through `compare`. It must satisfy sign, transitivity, and zero-consistency rules; comparison zero defines uniqueness in tree collections. See Chapter 30.
- **Composition:** Building behavior by owning and delegating to collaborators. It usually exposes fewer extension hazards than inheritance and supports Strategy or Decorator designs. See Chapter 49.
- **Concurrency:** Multiple tasks making progress during overlapping periods, whether or not they execute simultaneously. Correctness requires ordering, ownership, cancellation, and capacity reasoning. See Part V.
- **Concurrent collection:** Collection designed with specified thread-safe operations and traversal semantics. Its individual method safety does not automatically make a compound workflow atomic. See Chapter 37.
- **Constant pool:** See runtime constant pool. A class file also contains a per-class-file constant pool used to encode symbolic and literal information. See Chapters 3 and 4.
- **Constructor:** Special class member that initializes a newly created instance after allocation and superclass construction steps. It is not inherited and has no return type. See Chapter 16.
- **Contention:** Multiple threads competing for a limited resource such as a lock, CPU, connection, or cache line. It can reduce throughput and increase tail latency. See Chapters 34 and 39.
- **Covariance:** Type relationship preserving direction, such as `? extends T` for a producer view. Java arrays are covariant; ordinary generic types are invariant. See Chapter 22.
- **Critical section:** Code region whose shared-state invariant requires controlled access, commonly through a lock or atomic protocol. Keep it correct first, then minimize held time. See Chapter 34.

## D

- **Data race:** Conflicting accesses to shared memory by different threads, at least one a write, without sufficient happens-before ordering. Race-free design is prerequisite to simple visibility reasoning. See Chapter 11.
- **Deadlock:** Cycle of waiting in which each participant requires a resource held by another and none can progress. Prevention can impose lock ordering or avoid hold-and-wait. See Chapter 38.
- **Defensive copy:** Independent copy made to prevent caller or callee aliases from mutating owned collection/array structure. A shallow copy still shares element objects. See Chapters 19 and 49.
- **Dependency injection:** Supplying collaborators from outside a component rather than constructing global/concrete dependencies internally. It improves explicitness and test seams when boundaries are meaningful. See Chapter 49.
- **Dependency inversion:** High-level policy depends on an abstraction it needs; low-level mechanisms implement that abstraction. An interface mirroring one vendor SDK does not necessarily invert policy. See Chapter 49.
- **Deserialization:** Reconstruction of data or objects from an external representation. It is a trust boundary requiring schema, type, size, depth, and code-execution controls. See Chapters 32 and 52.
- **Direct buffer:** NIO buffer whose storage is outside the ordinary heap-array model and intended for efficient native I/O. Allocation, cleanup, and accounting are JVM/platform-sensitive. See Chapter 32.
- **Dominator:** In heap-graph analysis, an object through which every root path to another object passes. Dominators help locate retention owners. See Chapter 41.
- **Double-checked locking:** Lazy-initialization pattern checking before and after acquiring a lock. In Java, the published field must be `volatile` and the implementation must preserve the protocol. See Chapter 35.
- **Downstream collector:** Collector applied inside another collector, such as counting values inside each `groupingBy` bucket. It enables declarative multi-level reductions. See Chapter 31.
- **DTO:** Data transfer object designed for a boundary such as HTTP, events, or persistence mapping. It should carry an explicit version/validation contract rather than expose internal entities. See Chapter 50.
- **Durability:** Degree to which committed state survives failures under a storage system's contract. Java `flush` or object reachability does not itself imply durable storage. See Chapters 32 and 50.
- **Dynamic dispatch:** Runtime selection of an overridden instance method based on the receiver's runtime class. Static, private, and constructor behavior follows different rules. See Chapter 17.
- **Dynamic programming:** Algorithm technique storing solutions to overlapping subproblems under a recurrence and state definition. Correct state and transition derivation precede table optimization. See Chapter 47.

## E

- **Encapsulation:** Protecting representation and invariants through controlled operations. Private fields alone are insufficient if mutable aliases or invalid setters escape. See Chapters 16 and 49.
- **Encounter order:** Logical order in which a collection or stream presents elements. It can be defined, absent, or deliberately relaxed; it is distinct from sorted order. See Chapters 25 and 31.
- **Enum:** Special class with a fixed set of named instances. Enums can have fields, methods, and per-constant behavior and work efficiently with `EnumSet`/`EnumMap`. See Chapter 21.
- **Equality contract:** `equals` must be reflexive, symmetric, transitive, consistent, and false for null. Equal objects must have equal hash codes. See Chapter 19.
- **Erasure:** See type erasure.
- **Escape analysis:** JVM optimization analysis determining whether an object/reference escapes a scope, potentially enabling allocation or synchronization elimination. Results are HotSpot/version-dependent. See Chapters 7 and 39.
- **Exception:** Object representing abrupt completion. Checked/unchecked classification, causal chains, suppression, and recovery scope are API-design concerns. See Chapter 20.
- **Executor:** Abstraction separating task submission from scheduling/execution policy. Executor services also define lifecycle, rejection, queue, and shutdown behavior. See Chapter 36.

## F

- **Fail-fast iterator:** Iterator that attempts to detect unsupported structural modification and throw `ConcurrentModificationException`. Detection is best-effort and is not synchronization. See Chapter 25.
- **Fairness:** Policy controlling which waiter obtains a resource. Fair locks/queues can reduce starvation but often add coordination cost and do not guarantee application-level fairness. See Chapter 34.
- **False sharing:** Performance interference when independent frequently written variables occupy the same hardware cache line. It is hardware/layout-sensitive and should be confirmed by measurement. See Chapter 39.
- **FIFO:** First in, first out removal policy. Queue ordering can still be affected by priorities, multiple consumers, retry insertion, or implementation semantics. See Chapter 29.
- **Final field:** Field assigned once by constructor/initializer rules. Correct construction gives special visibility guarantees for final state, but referenced mutable objects can still change. See Chapters 11 and 19.
- **ForkJoinPool:** Executor based on work-stealing queues, suited to recursively divisible CPU work. Blocking and use of the common pool require capacity awareness. See Chapter 36.
- **Functional interface:** A non-sealed interface whose inherited abstract methods, after excluding signatures matching public methods of `Object`, induce one function type under the JLS rules. It can be a target for lambdas and method references; default and static methods do not add abstract requirements. See Chapter 23.

## G

- **G1:** Garbage-First collector, a region-based HotSpot collector and common default in mainstream Java 17/21 server configurations. It is an implementation choice, not Java semantics. See Chapter 9.
- **Garbage collection:** Automatic reclamation of storage for unreachable objects. Java does not mandate one collector, generation scheme, pause target, or immediate return of memory to the OS. See Chapter 9.
- **GC root:** Starting reference used in reachability analysis, such as selected thread, static, class-loader, JNI, or VM references. Root paths explain retention. See Chapter 41.
- **Generics:** Compile-time parameterization of types and methods. Java generics largely use erasure and are invariant unless wildcards express use-site variance. See Chapter 22.

## H

- **Happens-before:** JMM ordering relation guaranteeing visibility and ordering between actions. Program order, locks, volatile, thread start/join, class initialization, and transitivity create important edges. See Chapter 11.
- **Hash code:** Integer used to choose candidate hash regions. Equal objects must have equal hash codes; unequal objects may collide. Key-relevant state must remain stable while stored. See Chapter 27.
- **Hash flooding:** Accidental or adversarial concentration of keys into collisions, increasing CPU cost. Current implementations may add resilience, but input bounds and sound hashes remain necessary. See Chapter 27.
- **HashMap:** General-purpose hash-table `Map` allowing one null key and null values, with expected constant-time basic operations under suitable hashing. It has no encounter-order guarantee. See Chapter 27.
- **Heap (data structure):** Priority structure satisfying a heap-order invariant between parents and descendants. A complete, array-backed binary heap is the common representation in Java priority-queue and top-k discussions. See binary heap and Chapters 29 and 45.
- **Heap (JVM):** Runtime data area from which class instances and arrays are allocated conceptually. Physical representation and collector layout are implementation-specific. See Chapter 6.
- **Heap pollution:** A parameterized variable refers to an object not compatible with its parameterized type, often through raw types, unchecked casts, arrays, or unsafe varargs. See Chapter 22.
- **HotSpot:** OpenJDK's widely used JVM implementation. Its JIT tiers, collectors, object layout, flags, and diagnostics are useful engineering details but not Java specification guarantees.

## I

- **Idempotency:** Property that repeated execution under one logical identity produces the intended single logical effect. It is essential when timeouts leave remote outcomes unknown. See Chapters 50 and 52.
- **Identity:** Distinction between the same object reference and merely equal values. `==` tests reference identity for references; `equals` can define logical value equality. See Chapter 19.
- **Immutability:** Inability to change an object's observable state after construction. Final fields and unmodifiable collections help, but deep immutable graphs require controlling mutable referenced objects. See Chapter 19.
- **Inheritance:** Mechanism by which a class extends another class and inherits accessible members/behavior. Correct use requires a genuine behavioral subtype, not merely code reuse. See Chapter 17.
- **Interface:** Reference type declaring a contract and possibly default/static/private methods and constants. Interfaces support multiple capability inheritance but do not automatically guarantee substitutability. See Chapter 18.
- **Interrupt:** Cooperative thread cancellation/status mechanism. Blocking methods may throw `InterruptedException`; code should propagate or restore status unless it deliberately consumes cancellation. See Chapter 33.
- **Invariant:** Condition that remains true for every valid observable state of an object, algorithm, or system. It is the center of correctness proofs and API design. See Chapters 42 and 49.
- **Isolation:** Transaction property controlling observable interference among concurrent transactions. Named levels and anomalies have database-specific realization and must be integration-tested. See Chapter 50.
- **Iterator:** Stateful traversal cursor with `hasNext` and `next`, plus optional removal. Iterator ordering, mutation, consistency, and thread behavior come from the source contract. See Chapter 25.

## J

- **JAR:** ZIP-based Java archive format containing classes, resources, and metadata. A JAR is packaging, not an isolation or dependency-resolution mechanism. See Chapters 2 and 51.
- **javac:** Reference Java compiler included with the JDK. It translates source according to selected language/release options; its diagnostics and optimization choices are tool-specific. See Chapter 3.
- **JDK:** Java Development Kit: a Java runtime plus development, packaging, documentation, and diagnostic tools. Distribution and included components vary by vendor. See Chapter 2.
- **JFR:** Java Flight Recorder, a JDK event-recording facility for timestamped runtime/application evidence. Event configuration and fields are version-sensitive. See Chapter 40.
- **JIT:** Just-in-time compilation of hot runtime code to machine code. Java permits but does not require a particular compiler, threshold, tier, or optimization. See Chapter 10.
- **JLS:** Java Language Specification, the normative definition of Java syntax, typing, evaluation, initialization, exceptions, and memory-model rules for a release.
- **JMC:** JDK Mission Control, a separately distributed analysis and monitoring application commonly used to inspect JFR recordings. Tool versions should match supported recording formats. See Chapter 40.
- **JNI:** Java Native Interface, a boundary for calling native code and interacting with JVM objects. It expands memory-safety, lifecycle, portability, and crash risk. See Chapters 4 and 52.
- **JVM:** Abstract Java Virtual Machine plus, colloquially, an implementation executing class files. Separate JVMS guarantees from HotSpot behavior. See Chapter 4.
- **JVMS:** Java Virtual Machine Specification, the normative definition of class-file format, runtime data areas, loading/linking/initialization, instructions, and verification for a release.

## L

- **Lambda expression:** Java syntax producing an instance of a functional-interface target type. Capture and runtime translation do not imply a specified anonymous-class allocation. See Chapter 23.
- **Latency:** Elapsed time for an operation. It is a distribution; p50, p95, p99, maximum, errors, and load context convey more than an average. See Chapter 39.
- **Lazy evaluation:** Deferring work until a result is demanded. Stream intermediate operations are generally lazy, enabling fusion and short-circuiting but allowing stateful buffering. See Chapter 31.
- **Linearizability:** Concurrent-object correctness condition in which each completed operation appears to take effect at one instant between invocation and response, respecting real-time order. See Chapter 37.
- **Linking:** JVM process of verification, preparation, and optionally resolution after loading. Verification and preparation precede initialization, while symbolic-reference resolution may continue after initialization. See Chapter 5.
- **Live set:** Memory occupied by objects that remain reachable after an effective collection/observation point. A growing comparable live set suggests increasing intended or unwanted retention. See Chapter 41.
- **Livelock:** Threads continue taking actions in response to one another but make no useful progress. Randomized/backoff coordination or protocol redesign may resolve it. See Chapter 38.
- **Load factor:** Hash-table fullness parameter influencing resize threshold, empty buckets, and collision depth. Exact capacity policy belongs to the concrete implementation. See Chapter 27.
- **Lock:** Synchronization mechanism granting controlled access and creating memory-ordering edges. Intrinsic monitors and `Lock` implementations differ in API features. See Chapter 34.
- **LSP:** Liskov Substitution Principle: implementations must remain behaviorally usable wherever the abstraction is expected. See behavioral subtyping and Chapter 49.

## M

- **Map:** Association from unique keys to values. `Map` is not a subtype of `Collection`; its key, value, and entry views bridge into collection APIs. See Chapter 25.
- **Memory leak:** Unwanted retention or unreleased resource that grows footprint or exhausts capacity. In Java heap analysis, the root issue is an unintended reachability path. See Chapter 41.
- **Memory model:** Rules describing legal inter-thread observations, ordering, data races, final fields, volatile, locks, and happens-before. Java's model is specified in JLS Chapter 17. See Chapter 11.
- **Metaspace:** HotSpot native-memory area commonly holding class metadata. It can grow through class loading and class-loader retention and is distinct from ordinary Java heap. See Chapters 6 and 41.
- **Method area:** JVMS shared runtime area storing per-class structures such as runtime constants and method/field data. Physical placement is not specified. See Chapter 6.
- **Method overloading:** Multiple methods share a name but have different parameter signatures; compile-time resolution selects among applicable methods. Return type alone cannot overload. See Chapter 14.
- **Method overriding:** Subclass or implementation supplies an instance method matching an inherited method contract, enabling dynamic dispatch. Visibility, return, and checked-exception rules constrain it. See Chapter 17.
- **Module:** Named unit in the Java Platform Module System declaring dependencies and exported/open packages. Modules improve reliable configuration and encapsulation but are not security sandboxes. See Chapter 2.
- **Monitor:** Intrinsic synchronization object associated conceptually with each object/class, used by `synchronized`, `wait`, `notify`, and `notifyAll`. See Chapter 34.
- **Mutable reduction:** Accumulation into a mutable container using a supplier, accumulator, combiner, and optional finisher, as modeled by stream collectors. See Chapter 31.

## N

- **N+1 query:** One initial query followed by one query per result/item, causing linear database round trips. Batch/fetch design should preserve cardinality and memory bounds. See Chapter 50.
- **Natural ordering:** Canonical ordering defined by `Comparable`. Sorted maps/sets use it when no comparator is supplied; comparison zero controls their key/element uniqueness. See Chapters 28 and 30.
- **NIO:** Java APIs centered on buffers, channels, selectors, charsets, and modern filesystem paths. NIO does not mean every operation is nonblocking. See Chapter 32.
- **Non-interference:** Stream requirement that behavioral functions not improperly modify or depend on mutation of the source during pipeline execution. See Chapter 31.
- **Null:** The sole value of the null type, assignable to reference types but not primitive types. API null support is contract-specific; dereference throws `NullPointerException`. See Chapter 12.

## O

- **Object:** Class instance or array at runtime. A reference variable holds a reference value, not the object inline as a language guarantee. See Chapters 7 and 16.
- **Object header:** HotSpot-specific metadata commonly associated with an object, such as class and locking/GC information. Exact bits and size depend on JVM/configuration. See Chapter 7.
- **Object monitor:** See monitor.
- **Optional:** Value-based container representing one non-null value or absence. Best used as a maybe-one return type, not universally as a field, parameter, or collection wrapper. See Chapter 31.
- **Outbox:** Durable table/record written atomically with domain changes and later relayed to messaging, addressing the database-plus-broker dual-write gap. See Chapter 50.
- **Overloading:** See method overloading.
- **Overriding:** See method overriding.

## P

- **Parallel stream:** Stream pipeline eligible for partitioned execution, commonly using fork/join infrastructure. Correctness needs associative/stateless behavior; speed depends on source splitting and workload. See Chapter 31.
- **Parameterized type:** Generic type instantiated with type arguments, such as `List<String>`. It provides compile-time constraints but is mostly erased at runtime. See Chapter 22.
- **Pass-by-value:** Java argument passing copies the evaluated argument value into a parameter. For objects, the copied value is a reference; parameter reassignment does not affect caller variables. See Chapter 14.
- **Pattern matching:** Language features that test structure/type and introduce variables when a pattern matches, including `instanceof`, record patterns, and pattern `switch`. See Chapters 18 and 24.
- **Platform thread:** `Thread` typically mapped one-to-one to an operating-system thread for its lifetime. It is heavier and scarcer than a virtual thread. See Chapters 33 and 37.
- **Polymorphism:** One abstraction supports values with differing implementations, with behavior selected through overriding, interfaces, or patterns. Substitutability remains a contract requirement. See Chapter 17.
- **Prepared statement:** JDBC statement separating SQL structure from bound values. It mitigates value injection but cannot parameterize arbitrary identifiers or SQL fragments. See Chapter 50.
- **Primitive type:** One of Java's boolean or numeric non-reference types. Primitive values have defined ranges/semantics and cannot be null. See Chapter 12.
- **Priority queue:** Queue whose head is selected by ordering rather than arrival time. Java's `PriorityQueue` is not stable and its iterator is not sorted. See Chapter 29.
- **Promotion:** Collector-specific movement/classification of surviving objects into longer-lived storage. Promotion pressure can indicate survivor behavior or live-set growth, not automatically a leak. See Chapters 9 and 41.

## Q

- **Queue:** Collection modeling a head chosen for inspection/removal under a policy. `offer/poll/peek` use special results, while `add/remove/element` can throw. See Chapter 29.

## R

- **Race condition:** Correctness depends on uncontrolled relative timing. A race can exist without meeting the JMM's narrower definition of data race. See Chapter 38.
- **Record:** Final data-carrier class with declared components and generated accessors, constructor, equality, hash, and string form. Component references are only shallowly immutable. See Chapters 19 and 24.
- **Recursion:** Method/problem definition invoking itself on smaller subproblems with a base case. Stack depth and repeated work must be analyzed. See Chapters 8 and 47.
- **Reference type:** Class, interface, array, or type-variable category whose values are references or null. Representation of references is not fixed by the language. See Chapter 12.
- **Reflection:** Runtime inspection/invocation through metadata APIs. It can cross design boundaries, add access/configuration complexity, and should not be assumed to have ordinary call performance. See Chapter 21.
- **Region:** Collector-specific heap subdivision used by region-based collectors such as G1. Region size, roles, and humongous thresholds are implementation/configuration details. See Chapter 9.
- **Reification:** Runtime preservation of type information. Arrays are reified enough for store checks; most generic arguments are erased. See Chapter 22.
- **Retained size:** Heap-analysis estimate of memory that becomes unreachable if an object is removed, based on domination. It differs from direct shallow size. See Chapter 41.
- **Retry budget:** Explicit bound on attempts and elapsed time, ideally within one propagated deadline. It prevents retries from multiplying an outage. See Chapter 52.
- **Runtime constant pool:** Per-class/interface JVM runtime representation derived from the class-file constant pool, containing literals and symbolic references used in linking/execution. See Chapters 4 and 6.

## S

- **Safepoint:** HotSpot mechanism/state permitting selected global VM operations when relevant threads are at safe locations. Reasons, polling, and pause behavior are implementation-specific. See Chapter 10.
- **Sealed class/interface:** Type restricting permitted direct subclasses/implementations, enabling controlled hierarchies and exhaustive reasoning. Permitted types must satisfy language/module/package rules. See Chapters 18 and 24.
- **Serialization:** Encoding data/object state for storage or transfer. Wire schema, compatibility, limits, validation, and trust matter more than convenient object mapping. See Chapters 32 and 50.
- **Shallow copy:** New outer object/collection whose referenced elements are shared. It protects outer structure but not mutable nested objects. See Chapter 19.
- **Shallow size:** Memory directly occupied by one heap object, excluding referenced objects. Large retained graphs can have a small shallow owner. See Chapter 41.
- **Short-circuiting:** Operation that can complete without consuming all input, such as `findFirst`, `anyMatch`, or Boolean `&&`. State/order can constrain savings. See Chapters 13 and 31.
- **SOLID:** Five object-design heuristics: single responsibility, open/closed, Liskov substitution, interface segregation, and dependency inversion. They guide trade-offs, not class-count targets. See Chapter 49.
- **Stack frame:** Per-invocation JVM frame containing local variables, operand stack, and linkage information conceptually. Frame layout is implementation-specific. See Chapters 6 and 8.
- **Starvation:** A task remains unable to acquire service/resource while others progress. Fairness, partitioning, priority aging, or capacity policy may mitigate it. See Chapter 38.
- **Static binding:** Compile-time selection not based on receiver overriding, as with static method hiding and overload resolution. Contrast with dynamic instance-method dispatch. See Chapters 14 and 17.
- **Stream:** Single-use lazy traversal pipeline, not a data container. Intermediate operations describe transformations; a terminal operation drives consumption. See Chapter 31.
- **Synchronization:** Coordination that protects invariants and establishes memory ordering. It includes locks, volatile, atomics, thread lifecycle edges, and higher-level concurrent structures. See Part V.
- **Synchronized:** Java keyword for intrinsic monitor acquisition/release around a block or method. Successful unlock happens-before a later successful lock of the same monitor. See Chapter 34.
- **System class loader:** Application class loader returned by `ClassLoader.getSystemClassLoader` in ordinary launches. Its exact implementation and hierarchy can vary. See Chapter 5.

## T

- **Tail latency:** High-percentile response time, such as p99, reflecting slow requests hidden by averages. Always state traffic, window, errors, and sampling method. See Chapter 39.
- **Thread:** Java execution abstraction represented by `Thread`, with lifecycle, interruption, and memory-ordering rules. It may be a platform or virtual thread in Java 21. See Chapter 33.
- **Thread confinement:** Keeping mutable state accessible to only one thread at a time, eliminating shared-memory races for that state. Transfer still needs safe publication. See Chapter 38.
- **Thread local:** Per-thread value associated with a `ThreadLocal` key. Pooled-thread values require cleanup; massive virtual-thread use requires footprint awareness. See Chapters 33 and 41.
- **Thread pool:** Reuses a bounded/scaled set of worker threads to execute tasks, usually with a queue and rejection policy. Pool sizing follows workload and downstream capacity. See Chapter 36.
- **Thread safety:** Property that a component's documented invariants hold under allowed concurrent use. It must specify atomicity, traversal, visibility, and compound-operation boundaries. See Chapter 38.
- **Throughput:** Completed useful operations per unit time under stated load and correctness. Higher throughput can coexist with worse latency or resource cost. See Chapter 39.
- **Transaction:** Group of database operations committed or rolled back under a database's atomicity/isolation/durability contract. It does not automatically include remote services. See Chapter 50.
- **Transitive dependency:** Library pulled into a build through another dependency. Its version, license, vulnerabilities, and runtime presence remain application supply-chain concerns. See Chapter 51.
- **Treeification:** OpenJDK `HashMap` optimization that can convert a sufficiently collided bucket into a tree under thresholds. It is version-sensitive, not a `Map` contract. See Chapter 27.
- **Try-with-resources:** Statement managing `AutoCloseable` resources in reverse declaration order. Work failure remains primary and close failures become suppressed. See Chapter 20.
- **Type erasure:** Translation model removing most generic type arguments from runtime representation while adding casts/bridges as needed. It explains raw types and heap pollution. See Chapter 22.
- **Type inference:** Compiler derivation of omitted generic/lambda/local types from constraints and context. Inferred type follows language rules, not runtime value changes. See Chapters 22 and 24.

## U

- **Unboxing:** Conversion from wrapper reference to primitive. Unboxing null throws `NullPointerException`; numeric overload and equality behavior can change across boxing boundaries. See Chapter 12.
- **Unicode code point:** Integer identifying a Unicode character value. Java `String` indexes UTF-16 code units, so one supplementary code point occupies two `char` values. See Chapter 15.
- **Unmodifiable view:** Wrapper rejecting mutation through that reference while often reflecting mutations through a backing alias. It is not necessarily an immutable snapshot. See Chapter 25.

## V

- **Value-based class:** API-design designation for classes whose instances should be treated by value and whose identity should not be used for synchronization or identity-sensitive operations. See Chapter 19.
- **Variance:** Relationship between parameterized types when type arguments are related. Java uses invariant generic classes with `extends`/`super` wildcards for use-site covariance/contravariance. See Chapter 22.
- **Virtual thread:** Lightweight `Thread` scheduled by the Java runtime, suited to high-concurrency blocking workloads. It improves scale, not CPU speed, and does not remove downstream limits. See Chapter 37.
- **Volatile:** Field modifier providing defined visibility/order and atomic reads/writes for the field's value. Compound actions like increment remain non-atomic. See Chapters 11 and 35.

## W

- **Wait set:** Conceptual monitor-associated set of threads that called `wait`. Notification permits competition to reacquire the monitor; condition loops must handle wakeups and recheck predicates. See Chapter 34.
- **Weak reference:** Reference that does not keep its referent strongly reachable and is cleared under GC reachability rules. It is not a deterministic cache eviction policy. See Chapters 9 and 41.
- **Work stealing:** Scheduling approach where idle workers take tasks from other workers' deques, used by fork/join execution. Blocking and task granularity affect efficiency. See Chapter 36.
- **Write skew:** Transaction anomaly where concurrent transactions read overlapping state and write disjoint rows, jointly violating an invariant under some snapshot isolation schemes. See Chapter 50.

## Z

- **ZGC:** HotSpot low-latency concurrent collector. Generational mode was introduced in JDK 21; selection, flags, defaults, and behavior are release/vendor-specific. See Chapters 9 and 41.
