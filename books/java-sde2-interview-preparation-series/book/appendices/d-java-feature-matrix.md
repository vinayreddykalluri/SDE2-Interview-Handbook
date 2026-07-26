# Appendix D - Java 17 and Java 21 Feature Matrix

This matrix emphasizes features that influence SDE-2 interviews and backend engineering. It uses Java 21 as the executable baseline and distinguishes final features from preview or incubating work. A feature can be introduced across several releases before becoming final; the final release is the portable baseline used here.

## LTS orientation

| Release | Status in this book | Selected significance |
|---|---|---|
| Java 8 | historical baseline | lambdas, streams, default interface methods, `java.time`, `CompletableFuture` |
| Java 11 | migration baseline | standard HTTP client, single-file launch, module-era runtime, removed bundled technologies |
| Java 17 | supported modern baseline | records, sealed classes, pattern matching for `instanceof`, strong encapsulation context |
| Java 21 | main code baseline | virtual threads, record patterns, pattern switch, sequenced collections, generational ZGC option |
| Java 25 | later ecosystem context | LTS after the book's Java 21 code baseline; do not assume its APIs in examples |

Long-term-support labels and commercial support schedules are vendor policy, not Java language semantics. Verify the distribution and support contract used by an employer.

## Language feature timeline

| Feature | Final release | Java 17 | Java 21 | Interview implication |
|---|---:|---:|---:|---|
| local variable type inference (`var`) | 10 | yes | yes | local static type remains fixed; not dynamic typing |
| switch expressions | 14 | yes | yes | value-producing, exhaustive form with arrow labels |
| text blocks | 15 | yes | yes | multi-line literals with incidental indentation rules |
| records | 16 | yes | yes | nominal data carriers with generated members and shallow immutability |
| pattern matching for `instanceof` | 16 | yes | yes | flow-scoped pattern variable |
| sealed classes/interfaces | 17 | yes | yes | closed direct-subtype set; supports exhaustive reasoning |
| record patterns | 21 | no | yes | nested deconstruction patterns |
| pattern matching for switch | 21 | preview | final | type/record patterns, null handling, dominance and exhaustiveness rules |

### `var`

```java
var names = new java.util.ArrayList<String>();
```

`var` is permitted for qualifying local variables, loop variables, and lambda parameters in specified forms. It cannot declare fields, method parameters, or return types. The initializer determines a compile-time type; `names` is not dynamically typed. Use it when the initializer makes the type clear, not to hide a meaningful abstraction.

### Switch expressions

```java
int priority = switch (severity) {
    case LOW -> 1;
    case MEDIUM -> 2;
    case HIGH -> 3;
};
```

The compiler checks that a switch expression produces a value for every possible input covered by the type rules. In a block arm, use `yield` to produce the value. The older colon statement form remains and can fall through.

### Text blocks

```java
String json = """
        {
          "enabled": true
        }
        """;
```

Text blocks reduce escaping but still produce an ordinary `String`. Their indentation and trailing-newline behavior are defined transformations; use `stripIndent` or explicit escapes only when the contract requires it.

### Records

```java
record Point(int x, int y) {}
```

A record is implicitly final and extends `java.lang.Record`. It receives private final component fields, accessors named after components, a canonical constructor, and value-oriented `equals`, `hashCode`, and `toString`. It may implement interfaces and define members, but cannot declare extra instance fields. Component objects can still be mutable.

### Sealed hierarchies

```java
sealed interface Command permits Create, Delete {}
record Create(String id) implements Command {}
record Delete(String id) implements Command {}
```

The permits relationship is enforced at compile time and in class metadata. It is useful when the domain genuinely owns a closed alternative set. `non-sealed` deliberately reopens a branch.

### Record patterns and pattern switch

```java
static String describe(Object value) {
    return switch (value) {
        case null -> "null";
        case Point(int x, int y) when x == y -> "diagonal " + x;
        case Point(int x, int y) -> x + "," + y;
        default -> value.toString();
    };
}
```

Case labels are checked for dominance. An earlier unconditional supertype pattern can make a later subtype case unreachable. Guards refine a matching case. Exhaustiveness rules depend on the selector type; enum and sealed hierarchies are common interview examples.

## Library and platform matrix

| Feature | Release | Core idea | Boundary to remember |
|---|---:|---|---|
| module system | 9 | reliable configuration and strong encapsulation | modules complement, not replace, packages/JARs |
| collection factories | 9/10 | `List.of`, `Set.of`, `Map.of`, `copyOf` | unmodifiable and null-rejecting, not necessarily a new object |
| standard HTTP client | 11 | synchronous/asynchronous HTTP/2-capable client | application still owns timeouts, body limits, retries, and executors |
| helpful NPE messages | 14 | more precise dereference context | diagnostic behavior, not a null-safety type system |
| strong encapsulation progression | 9-17 | restrict unsupported JDK internals | migration should remove reliance on internals, not add permanent opens |
| random generator interfaces | 17 | named/splittable/jumpable algorithms | choose by statistical/concurrency need, not habit |
| simple web server | 18 | minimal local HTTP serving API | not a full production service framework |
| UTF-8 default charset | 18 | standardized default charset | explicit charsets remain best at protocol/file boundaries |
| sequenced collections | 21 | uniform first/last/reversed encounter-order APIs | only meaningful for collections with a defined encounter order |
| virtual threads | 21 | cheap JVM-managed thread-per-task | do not pool to limit threads; bound external resources |
| generational ZGC | 21 | generational mode option for ZGC | collector flags/production maturity are release-specific |

## Sequenced collections

Java 21 adds `SequencedCollection`, `SequencedSet`, and `SequencedMap` to express defined encounter order and operations at both ends.

Representative methods:

```java
list.getFirst();
list.getLast();
list.addFirst(value);
list.reversed();

map.firstEntry();
map.lastEntry();
map.sequencedKeySet();
map.reversed();
```

Support and mutation behavior still depend on the concrete collection. A reversed view is generally a view, not an independent copy. An unmodifiable collection remains unmodifiable through its reversed view.

## Virtual threads

Creation patterns:

```java
Thread thread = Thread.ofVirtual().start(task);

try (var executor = java.util.concurrent.Executors.newVirtualThreadPerTaskExecutor()) {
    var future = executor.submit(task);
    future.get();
}
```

Virtual threads preserve the `Thread` programming model and are most compelling for large numbers of mostly blocking tasks. They improve scalability by allowing a virtual thread to unmount from a carrier during many blocking operations. They do not make CPU-bound work cheaper, change happens-before rules, or make shared mutable state safe.

Operational considerations:

- Replace thread-pool size as admission control with explicit semaphores, connection pools, rate limits, and bounded queues at constrained resources.
- Thread-local values multiplied across very many tasks can retain surprising state; keep them deliberate and small.
- Some blocking while holding a monitor or executing native/foreign code can pin a virtual thread to a carrier in Java 21. Diagnose with release-appropriate JFR/tooling rather than guessing.
- Preserve cancellation and structured lifecycle even when threads are inexpensive.

Structured concurrency and scoped values were preview APIs in Java 21 and are not used as final Java 21 APIs in this book.

## API removals, encapsulation, and migration

Modernizing from Java 8/11 to 17/21 is more than changing a compiler level:

1. Inventory runtime distribution, build plugins, bytecode agents, annotation processors, JNI libraries, and reflective frameworks.
2. Run `jdeps` and tests to find internal JDK dependencies and removed modules.
3. Update dependencies before using broad `--add-opens` workarounds.
4. Check default charset, TLS/security defaults, GC defaults, container sizing, and logging.
5. Compile with `--release` and treat illegal reflective access or deprecation warnings as migration work.
6. Benchmark representative startup, steady-state, allocation, memory, and tail latency.
7. Roll out with canaries and explicit rollback criteria.

`--add-opens` can be a temporary compatibility bridge. It should name a specific need and have an owner/removal plan, because it weakens encapsulation and can hide unsupported coupling.

## Preview and incubator discipline

Preview language/platform features require `--enable-preview` for compilation and execution with a matching release. Class files record preview usage. Incubator modules normally require `--add-modules` and can change or disappear.

Java 21's non-final features included:

| Feature | Java 21 status | JEP |
|---|---|---:|
| String Templates | preview | 430 |
| Unnamed Patterns and Variables | preview | 443 |
| Unnamed Classes and Instance Main Methods | preview | 445 |
| Foreign Function and Memory API | third preview | 442 |
| Scoped Values | preview | 446 |
| Structured Concurrency | preview | 453 |
| Vector API | sixth incubator | 448 |

The iteration label matters: a later preview or incubator round is still not a final Java SE contract. Verify the exact JDK release before compiling examples or making a production compatibility promise.

## Later-release delta: JDK 24 and JDK 25

This is context for interviews held after the Java 21 baseline. It does not change the source level of the book's runnable examples.

| Later change | Delivered status | What a Java 21 answer should add |
|---|---|---|
| virtual-thread monitor pinning | [JEP 491](https://openjdk.org/jeps/491), delivered in JDK 24 | Java 21 can pin a virtual thread during blocking inside `synchronized`; JDK 24 removes nearly all monitor-related pinning, while selected native and class-initialization cases can remain |
| scoped values | [JEP 506](https://openjdk.org/jeps/506), final in JDK 25 | do not use the Java 21 preview API as a stable contract; describe the final JDK 25 API separately |
| module import declarations | [JEP 511](https://openjdk.org/jeps/511), final in JDK 25 | this is source convenience and does not replace module readability, exports, or reliable configuration |
| compact source files and instance main methods | [JEP 512](https://openjdk.org/jeps/512), final in JDK 25 | useful for small programs and learning; ordinary class-based source remains valid |
| flexible constructor bodies | [JEP 513](https://openjdk.org/jeps/513), final in JDK 25 | statements may precede an explicit constructor invocation under the new rules; Java 21 retains the older first-statement restriction |
| compact object headers | [JEP 519](https://openjdk.org/jeps/519), delivered in JDK 25 | object-header width is a VM/version/flag detail, so never present one layout as a Java guarantee |
| string templates | JEP 465 withdrawn | Java 21's preview syntax did not become a final feature; do not write production guidance as if `STR` were a current standard API |
| structured concurrency | fifth preview in JDK 25 | the concept is useful, but preview signatures and lifecycle rules still require exact-release verification |

The [OpenJDK JDK 25 release page](https://openjdk.org/projects/jdk/25/) records the complete delivered feature set and its General Availability date. Long-term-support availability remains a distribution/vendor support decision even when a release is widely described as LTS.

This delta illustrates a durable interview habit: name the requested baseline first, then give a short later-release update. Do not silently answer a Java 21 question with JDK 25 behavior, and do not keep repeating a Java 21 limitation after the deployed runtime has removed it.

Interview answer pattern:

- State whether the feature is final, preview, or incubating in the named JDK.
- Describe the final Java 21 baseline separately from later evolution.
- Do not present a preview API signature as a stable production contract.
- Explain why an organization might experiment, and how it limits migration and support risk.

## Feature selection checklist

| Need | Relevant modern feature | Question before adoption |
|---|---|---|
| compact immutable-looking value carrier | record | are component values themselves safely immutable/copyable? |
| closed domain alternatives | sealed hierarchy | does the domain truly own and control all direct variants? |
| exhaustive type-based branching | pattern switch | are null, dominance, and future evolution handled? |
| deconstruct nested values | record pattern | is pattern logic clearer than behavior on the domain type? |
| readable embedded multi-line text | text block | are indentation, escaping, and untrusted input handled? |
| thread-per-request blocking service | virtual threads | which downstream resources still need explicit bounds? |
| uniform first/last/reverse access | sequenced collections | does the concrete collection define encounter order and supported mutation? |
| safer fixed collection boundary | factory/copy methods | is shallow immutability enough and are nulls prohibited? |

Modern syntax is valuable when it sharpens the model. An SDE-2 answer also names compatibility, operability, lifecycle, and team-readability costs.
