# JVM Execution Evidence Workshop

The earlier chapters explain the pieces of the runtime. This workshop teaches the interview skill that connects them: begin with a Java guarantee, predict an observable consequence, collect the smallest useful evidence, and stop before an implementation detail becomes a universal claim.

> **From Vinay's review desk:** “Objects are on the heap and methods are on the stack” may pass a first screening, but it cannot diagnose a production incident. At SDE-2, say what the language guarantees, what the JVM specification models, what one runtime currently does, and which observation would distinguish your hypotheses.

## The three-layer answer

Use three layers in this order:

1. **Java language contract:** source-level rules such as initialization triggers, final-field semantics, exceptions, and reachability categories exposed by APIs.
2. **JVM contract:** class files, runtime constant pools, frames, operand stacks, verification, linking, and runtime data areas.
3. **Runtime observation:** HotSpot/OpenJDK flags, compilation tiers, object layout, collector regions, inlining decisions, and operating-system behavior for the exact build in front of you.

For example:

> The language requires a class to initialize before the first active use listed by the JLS. The JVM represents its initializer as `<clinit>` when one is needed. On this OpenJDK build I can confirm the trigger with class-initialization logging or a side-effecting initializer. I would not claim that merely loading a class runs `<clinit>`.

That answer is precise without pretending the interviewer wants a bytecode encyclopedia.

## One path from source to evidence

```text
Source.java
   | javac --release 21
   v
Source.class
   | javap -c -v
   v
class-file evidence: descriptors, constant pool, bytecode, attributes
   | java + runtime observation
   v
loading -> linking -> initialization -> interpreted/compiled execution
   | jcmd / JFR / logs / dumps
   v
evidence for a concrete runtime hypothesis
```

Each tool answers a different question:

| Question | Useful first evidence | What it cannot prove alone |
|---|---|---|
| what did `javac` emit? | `javap -c -v` | what the JIT ultimately executes |
| which class definition was used? | class-load log, code source, defining loader | that another environment uses the same artifact |
| did initialization run? | side effect or initialization logging | why startup is slow without timing/profile evidence |
| where are threads blocked? | thread dump/JFR monitor events | long-term frequency from one snapshot |
| is allocation high? | JFR allocation events/profile | that retained memory is a leak |
| did a method compile or deoptimize? | compiler logs/JFR/compiler commands | source-level performance on every JVM |

The mistake to avoid is collecting a heap dump because “the JVM is slow.” Start from a falsifiable symptom.

## Class life cycle: predict before running

Consider this sequence:

```java
static final class Lazy {
    static final int CONSTANT = 7;
    static int runtimeValue = record("Lazy.<clinit>", 42);
}

int first = Lazy.CONSTANT;
int second = Lazy.runtimeValue;
int third = Lazy.runtimeValue;
```

### Prediction

1. `CONSTANT` is a compile-time constant variable, so client bytecode can contain the value directly. Reading it does not require initialization of `Lazy`.
2. Reading non-constant `runtimeValue` is an active use. Before the read completes, `Lazy` initializes.
3. Static initializers execute in textual order under the class-initialization protocol.
4. Successful initialization is not repeated for the third read.

### What `javap` can show

The caller can use an integer constant instruction or constant-pool value for `7`, while the runtime field read uses `getstatic`. `Lazy` can have a `<clinit>` method containing the call to `record` and the `putstatic` for `runtimeValue`.

Do not memorize the exact instruction chosen for every small integer. The useful distinction is inlined constant versus runtime field access.

### Failure transition

If a static initializer throws an ordinary unchecked exception, first active use typically receives `ExceptionInInitializerError`. The class becomes erroneous for that defining loader. A later active use can receive `NoClassDefFoundError` indicating that initialization could not complete.

This differs from `ClassNotFoundException`, which is a checked lookup failure from an explicit loading API. Reading the causal chain matters more than matching one exception name.

## Frames, references, and objects without the cartoon

A JVM frame is associated with one method invocation in one thread. The class-file method declares limits and instructions that operate on local-variable slots and an operand stack. An implementation can optimize aggressively while preserving observable Java behavior.

Use this model for a call:

```text
thread T

frame: caller
  local slot -> reference value R ------------------+
  operand stack                                     |
                                                     v
frame: callee                                  object state
  parameter slot -> copied reference value R -> {value=9}
  operand stack
```

Two slots can hold copies of the same reference value. Reassigning one slot does not destroy the object or change the other slot. Mutating through either reference can affect the shared object.

Avoid three overclaims:

- A Java source local is not guaranteed to remain in a physical stack-memory slot after optimization.
- An object is not required by the language to have one universal header size or field layout.
- Escape analysis may remove an allocation or scalar-replace an object, but the optimization is not part of Java semantics and can change by runtime/version/path.

`StackWalker` and stack traces expose logical frames according to their APIs. They are evidence about call paths, not raw native stack-memory maps.

## Reachability is not lexical scope

An object becomes eligible for collection when it is no longer strongly reachable according to the collector's root traversal and the relevant reference-processing rules. Leaving a block does not itself prove collection eligibility, and eligibility does not promise immediate reclamation.

```java
Payload first = new Payload(9);
Payload alias = first;
first = null;
```

The object remains strongly reachable through `alias`. Calling `System.gc()` is only a request and cannot make a deterministic unit-test assertion that a particular object is reclaimed.

For a suspected memory leak, separate:

```text
high allocation rate -> many objects created per unit time
high retained size   -> objects remain reachable after useful lifetime
heap exhaustion      -> runtime cannot satisfy an allocation under current conditions
```

These can coexist, but they are not synonyms.

## JIT compilation: optimize the path, preserve the program

The execution engine can begin with interpreted execution, collect profiles, compile hot methods, inline callees, speculate about receiver types, and later deoptimize when an assumption no longer holds. Exact tiers and thresholds are HotSpot details.

An interview-quality explanation:

> A hot call site may become monomorphic in its profile, allowing an implementation to inline the observed target and optimize across the boundary. The runtime must retain a path to correct behavior if a new receiver type invalidates that speculation, often through guards and deoptimization. I would confirm the behavior on the target runtime rather than promise that this call is always inlined.

Consequences for measurement:

- one cold invocation includes startup, loading, initialization, and compilation effects;
- a loop can be optimized away if its result has no observable use;
- constant inputs can create a benchmark of constant folding rather than the intended work;
- GC, safepoints, CPU frequency, scheduling, and co-tenancy affect timing;
- JMH exists to handle many harness hazards, not to make a bad question meaningful.

## Safepoints are not “the JVM stops after every instruction”

A safepoint is a runtime coordination mechanism used for operations that require threads to reach a known state. Some VM operations can pause many or all Java threads, but not every safepoint request implies a long stop, and not every application stall is GC.

When latency spikes, distinguish:

- time waiting to reach a safepoint;
- time in the VM operation after coordination;
- GC pause versus class redefinition, deoptimization, biased-lock legacy behavior on old runtimes, or another operation;
- application lock wait, queueing, I/O, CPU saturation, and downstream latency.

Use timestamp-correlated evidence rather than attributing any gap to “stop the world.”

## Failure and evidence matrix

| Symptom or error | Mechanism to consider | First useful evidence | Common wrong conclusion |
|---|---|---|---|
| `UnsupportedClassVersionError` | class file newer than runtime supports | class major version and `java -version` | source syntax is wrong |
| `VerifyError` | invalid/incompatible bytecode | full error, transformed artifact, `javap -v` | GC corruption |
| `ClassNotFoundException` | explicit loader lookup failed | requested name, loader, class/module path | class never existed anywhere |
| `NoClassDefFoundError` | required definition unavailable or initialization already failed | cause chain and earlier logs | same as checked lookup failure |
| `ExceptionInInitializerError` | first class initialization failed | root cause and initializer path | constructor failed |
| `StackOverflowError` | invocation depth exceeds available stack/resource | stack trace shape and input | every recursion is invalid |
| `OutOfMemoryError: Java heap space` | allocation cannot be satisfied | heap/GC evidence, allocation and retention | increase heap immediately |
| Metaspace-related OOME | class metadata/loader retention or limit | class counts, loader graph, unload evidence | object cache is necessarily the cause |
| high CPU after warmup | hot code, spin, compilation, GC, native work | profile/JFR plus runtime metrics | method with most calls is hottest |

## Production scenario: startup regression

**Report:** deployment startup increased from 8 seconds to 45 seconds after adding a plugin.

Weak response: “The JVM needs more warmup.”

Evidence-first plan:

1. Define start and ready timestamps consistently.
2. Compare artifact, JDK, flags, environment, and plugin set.
3. Record class-loading counts/timing and a startup JFR.
4. Look for static initializers performing network I/O, schema scans, reflection, generated classes, or lock contention.
5. Confirm whether the added time is CPU, blocked I/O, compilation, or GC.
6. Remove or defer one suspected initialization path and repeat under the same conditions.

If a plugin's static initializer performs network discovery, the design repair is usually explicit lifecycle initialization with timeout/cancellation and observability—not a collector flag.

## Production scenario: redeploy memory growth

**Report:** each hot redeploy adds non-heap memory and old application versions never disappear.

Hypothesis: an old defining class loader remains reachable.

Useful retention paths include:

- live threads and their context class loaders;
- static registries in parent-loaded code;
- thread locals;
- JDBC drivers, logging appenders, MBeans, callbacks, or executor tasks;
- generated proxy/class caches.

Evidence should identify the retained loader and a path from a GC root. “Metaspace leak” names the symptom; the root path identifies the owner that must release it.

## Executable evidence companion

`JvmExecutionEvidenceLab.java` deliberately checks only behavior that can be observed safely from Java:

- compile-time constant versus active-use initialization;
- bootstrap-loader API representation versus application loader;
- logical frames through `StackWalker`;
- first and later failures of an erroneous class initializer;
- reference aliasing after one variable is reassigned.

Compile and run from the series root:

```bash
out=$(mktemp -d)
javac --release 21 -Xlint:all -Werror -d "$out" \
  content/volumes/java/JAVA-06-jvm-and-execution/code/JvmExecutionEvidenceLab.java
java -ea -cp "$out" JvmExecutionEvidenceLab
```

Expected output:

```text
PASS 5 JVM execution evidence checks
```

The lab does not assert object layout, exact GC time, JIT decisions, or physical stack placement because those are not deterministic Java contracts.

## Interview room: worked answers

### Does reading any static field initialize its class?

**Model answer:** No. Active use of a non-constant static field triggers initialization, but a compile-time constant variable can be inlined into the client and read without initializing the declaring class. Loading and linking can also occur without initialization. I would inspect caller bytecode and a side effect or initialization log for the exact case.

**Follow-up:** What risk does inlining create across separate compilation?

**Answer:** A client compiled against the old constant can retain the old embedded value even if the library field changes, until the client is recompiled.

### Are all objects allocated on the heap?

**Model answer:** The Java/JVM model lets me reason about object identity and reachability, but a conforming optimizing runtime can eliminate or scalar-replace an allocation when behavior is unchanged. I do not use a source-level “always physically on heap” claim to predict exact memory layout.

### Can `System.gc()` prove an object is collectible?

**Model answer:** No. It is a request, collector behavior is not an immediate per-object promise, and reference processing has its own rules. I prove whether a strong path remains, then use appropriate heap evidence when diagnosing retention.

### What is the difference between interpreted and compiled execution?

**Model answer:** An implementation can execute bytecode through an interpreter and compile selected hot paths to native code using runtime profiles. Compilation may speculate and later deoptimize. Java semantics stay the same; thresholds, tiers, and generated code are runtime-specific.

### Why might two classes with the same binary name fail a cast?

**Model answer:** Runtime type identity includes the defining class loader. If two loaders independently define the same binary name, they create distinct types. A shared parent-loaded interface is a common plugin boundary.

### How do you distinguish a leak from allocation pressure?

**Model answer:** Allocation pressure is creation rate; a leak is unintended retention beyond useful lifetime. I correlate allocation profiles, post-GC occupancy/retained graphs, request rate, and GC behavior. Reducing allocation can help throughput without fixing a retained-owner path.

## Exercises

1. **Foundation:** Compile a class with a constant and a non-constant static field. Locate the caller instructions and `<clinit>` with `javap -c -v`.
2. **Interview Core:** Predict the first and second failures after a static initializer throws.
3. **Interview Core:** Draw frames and object references for a method that mutates an argument, then reassigns its parameter.
4. **Debugging:** A team claims `System.gc()` proves its weak-cache test. Replace the assertion with a deterministic contract test.
5. **SDE-2 Follow-up:** Create an evidence plan for a startup regression without changing JVM flags first.
6. **SDE-2 Follow-up:** Explain a class-loader retention path across hot redeploy and identify the owner of cleanup.

## Worked solutions

1. The constant read should not require `getstatic`; the runtime field should. `<clinit>` contains side-effecting static initialization. Report the actual instructions rather than assuming one small-constant opcode.
2. First active use observes the initializer failure, commonly wrapped in `ExceptionInInitializerError`; later use sees that the class is erroneous, commonly through `NoClassDefFoundError`. Preserve and read causes.
3. The caller and callee each have a slot containing a copied reference value. Mutation follows either copy to shared state; parameter reassignment changes only the callee slot.
4. Test cache membership and explicit cleanup policy deterministically. Treat eventual reference clearing as an integration/observability experiment with a bounded wait and no guarantee that `System.gc()` complies.
5. Fix timestamps and environment, collect startup JFR/class-loading evidence, split CPU from wait time, identify expensive initialization, change one suspected path, and repeat. A flag change without a mechanism is not a test.
6. Show `GC root -> parent registry/thread/thread-local -> plugin object/class -> defining loader`. Cleanup belongs to the component that registered or started the long-lived owner; unloading cannot occur while the loader remains reachable.

## Final checklist

- I separate language, JVM, and implementation claims.
- I can predict a class-initialization trigger and failure transition.
- I reason about copied references without asserting universal physical placement.
- I distinguish allocation, retention, and exhaustion.
- I explain speculative optimization and deoptimization without promising inlining.
- I select evidence from a falsifiable runtime hypothesis.
