# Chapter 5: Class Loading, Linking, and Initialization

## Learning objectives

- Explain loading, verification, preparation, resolution, and initialization.
- Describe bootstrap, platform, and application class loaders in modern Java.
- Reason about parent delegation, custom loaders, namespaces, and class identity.
- Predict static initialization order, including failure and circularity cases.
- Diagnose `ClassNotFoundException`, `NoClassDefFoundError`, and common linkage errors.

## Why this matters at SDE-2

Class loading enables plugin systems, application servers, agents, hot deployment, JDBC drivers, and framework discovery. It also creates difficult failures: a class can exist in a JAR but be invisible to a particular loader; two classes with the same name can be different types; an initialization failure can poison later use; and retained class loaders can leak entire application generations.

At SDE-2, "check the classpath" is only the start. You should identify the initiating and defining loader, distinguish lookup failure from definition/linkage failure, and explain why changing delegation order affects both isolation and security.

## First-principles model

A class name is not sufficient to create a runtime type. The JVM needs class-file bytes and a defining class loader. A useful pipeline is:

```text
binary-name request
      |
      v
loading: find bytes and create Class representation
      |
      v
linking:
  verification -> preparation -> resolution
      |
      v
initialization: execute class initialization method once
      |
      v
active runtime use
```

These phases have ordering constraints but can be lazy. A class may be loaded without being initialized. Resolution of some symbolic references can be delayed until first use.

> **Specification boundary:** The JLS defines when class or interface initialization is required and its synchronization/failure behavior. The JVMS defines loading, linking, class-file verification, and runtime constraints. The names and exact search behavior of built-in loaders are platform API/implementation matters rather than language semantics.

## Core terminology

- **Binary name:** Runtime name such as `com.example.OrderService`.
- **Initiating loader:** A loader through which loading of a class was initiated.
- **Defining loader:** The loader that calls into the JVM to define that class.
- **Loader namespace:** The set of type definitions visible under a loader's delegation relationships.
- **Parent delegation:** Asking a parent loader before attempting a local definition.
- **Preparation:** Allocating static field storage and assigning default values.
- **Initialization:** Executing static field initializers and static blocks in textual order through the generated class initialization method.
- **Active use:** An operation that triggers initialization, such as certain static field access, static method invocation, construction, or reflective requests.
- **Linkage error:** An `Error` indicating incompatible or unsatisfied class relationships at runtime.

## Detailed mechanics

Loading begins from a request for a binary name. A loader may return a class it already loaded, delegate, read a class from a JAR/network/generated source, transform bytes, then define it. The JVM associates the definition with that loader. Arrays are created specially by the JVM, with their component type influencing loader identity.

Verification rejects malformed or type-unsafe class files. Checks include structural validity, valid instruction operands and control flow, legal stack states, access constraints, and correct object initialization patterns. Verification protects the JVM even when bytes came from a compiler other than `javac` or were transformed by an agent.

Preparation creates static field storage with default values. It is important not to confuse this with executing Java initializers. Given `static int size = 42`, preparation establishes `size` as 0; initialization later assigns 42. Compile-time constant variables can be represented through a `ConstantValue` attribute and have special initialization/use behavior.

Resolution turns symbolic references from runtime constant pools into concrete classes, fields, methods, or interfaces, checking access and compatibility. Implementations may resolve lazily. Therefore an incompatible method can remain unnoticed until a path first executes its invocation.

Initialization executes a class's generated `<clinit>` logic, if present. Before a class initializes, its superclass initializes. Static field initializers and static blocks execute in source order. Initialization is synchronized per class so only one thread performs it while other threads wait, subject to specified recursive handling.

Merely referring to `SomeType.class` does not necessarily initialize `SomeType`. Reading a compile-time constant through a class name can inline the value into the client and may not initialize the declaring class. Creating an instance, invoking a static method, or reading a non-constant static field triggers initialization.

Modern built-in loaders are commonly described as:

- **Bootstrap loader:** Loads foundational runtime classes, represented as `null` by some reflection APIs.
- **Platform class loader:** Loads selected platform modules/classes not loaded by bootstrap.
- **Application (system) class loader:** Loads application classes from the configured class path/module path.

The application loader is not guaranteed to be a `URLClassLoader` in modern Java. Code that casts it based on Java 8 behavior is fragile.

Parent-first delegation prevents an application from casually substituting foundational platform definitions and promotes a single shared definition. Some containers use carefully designed child-first or selective policies for application isolation. A custom loader should normally call `loadClass` through the established locking/delegation protocol and implement `findClass` for local lookup.

Class identity is approximately `(binary name, defining loader)`. If loader A and loader B independently define bytes named `plugin.Message`, instances are not assignment compatible, even if byte-for-byte identical. A shared parent-loaded interface can bridge plugin implementations.

## Worked Java example

```java
public final class InitializationOrder {
    static int first = trace("first", 10);
    static int second = first + trace("second", 20);

    static {
        System.out.println("block: first=" + first + ", second=" + second);
    }

    static int trace(String label, int value) {
        System.out.println(label + " -> " + value);
        return value;
    }

    public static void main(String[] args) {
        System.out.println("main: " + second);
    }
}
```

Expected output:

```text
first -> 10
second -> 20
block: first=10, second=30
main: 30
```

Preparation first gives `first` and `second` the value 0. Initialization then follows textual order: assign 10, evaluate `10 + 20`, run the block, and finally invoke `main`.

## Execution or memory walkthrough

Consider two threads concurrently invoking `InitializationOrder.main` through reflection:

1. The class can already be loaded, verified, and prepared, with static fields at defaults.
2. Thread T1 requests active use and obtains the class initialization lock.
3. T2 requests active use and waits.
4. T1 executes the initializers in order. References needed during execution live in its frames; static values belong to runtime class state.
5. T1 completes normally. The class is marked initialized.
6. T2 resumes and observes the completed initialization effects, then proceeds without rerunning `<clinit>`.

If T1's initializer throws an unchecked exception, first active use commonly observes `ExceptionInInitializerError` (unless the throwable already has special `Error` treatment). The class is marked erroneous. A later use can fail with `NoClassDefFoundError: Could not initialize class ...`.

Circular initialization is subtler than a dead loop. If class `A` initializes and reads `B.value`, `B` may initialize and read a not-yet-assigned `A.value`, observing its prepared default. Recursive initialization by the same thread is allowed to proceed rather than reentering the initializer. Cross-thread cycles can deadlock when two initialization locks and mutually dependent initializers interact.

## Complexity and performance

Loading cost scales with class-file input, verification, metadata construction, I/O, and dependency resolution. Initialization cost is arbitrary Java code and can include network calls, locks, or large allocations, although such behavior is usually a design smell. Many small generated classes increase metadata and startup work. Class lookup caches make repeated successful loads cheap, but custom loaders can defeat sharing.

Unloading is normally collective: a class can become unloadable only when its defining loader is unreachable and its classes/instances are no longer strongly reachable under implementation criteria. One retained loader can therefore retain class metadata, static fields, generated proxies, and related graphs.

## Edge cases and common mistakes

- Calling loading and initialization the same operation.
- Saying static fields receive explicit initializer values during preparation.
- Using `Class.forName(name)` without realizing common overloads initialize the class; `ClassLoader.loadClass` normally does not initialize it.
- Assuming same class name means same type across loaders.
- Casting the system loader to `URLClassLoader` on modern JDKs.
- Catching only `ClassNotFoundException` when failures include `NoClassDefFoundError`, `UnsupportedClassVersionError`, `VerifyError`, `NoSuchMethodError`, or `IncompatibleClassChangeError`.
- Performing slow external I/O in static initialization.
- Using a child-first loader without protecting platform/shared API packages.

`ClassNotFoundException` is checked and typically reported by an explicit lookup API that cannot find a requested definition. `NoClassDefFoundError` is unchecked and means the JVM or runtime tried to use a definition that cannot be provided, including cases where it existed during compilation or its initialization previously failed. The message and causal chain are essential evidence.

## Production engineering notes

For a class-loading incident, capture the exact runtime artifact set, module/class path, class name, exception chain, initiating context, and defining loader. Unified class-load logging can reveal sources and loaders. Dependency-tree output can expose version conflicts, but the effective packaged artifact is authoritative.

Plugin boundaries should place shared API types in a parent loader and implementations in isolated child loaders. On unload, close loaders/resources and remove listeners, thread locals, executor threads, JDBC drivers, logging contexts, and caches that point back to the plugin loader. Threads inherit context class loaders, which are a frequent retention path.

> **HotSpot note:** HotSpot stores class metadata in native Metaspace and can log class loading with unified logging such as class-related tags. Exact flags, log formats, and unloading policy depend on version and collector.

## Interview questions and model answers

**What are the class life-cycle phases?**

Loading finds bytes and creates a definition. Linking verifies it, prepares static state, and resolves symbolic references, potentially lazily. Initialization executes static initialization when required by active use. Loading does not imply initialization.

**How do `ClassNotFoundException` and `NoClassDefFoundError` differ?**

`ClassNotFoundException` is a checked lookup failure from APIs such as class-loader or reflection operations. `NoClassDefFoundError` is a linkage-time/runtime inability to provide a class definition required by executing code, and it can also follow failed class initialization. I would inspect the cause and loader context rather than classify only by name.

**Why can identical classes fail a cast?**

Runtime identity includes the defining loader. Two loaders that each define `com.example.Message` create distinct types. The solution is usually a shared parent-loaded contract, not a forced cast.

**What is parent delegation for?**

It promotes consistent shared definitions and protects foundational classes from accidental replacement. Isolation systems may alter the policy selectively, but then must manage type boundaries and security carefully.

## Exercises

1. Predict the output of the worked example, then inspect `<clinit>` using `javap -c`.
2. Add a `static final int CONSTANT = 7` and a side-effecting initializer. Test which references trigger initialization.
3. Create two `URLClassLoader` instances with no shared application parent for a sample class and demonstrate failed assignment compatibility.
4. Construct a safe example of failed static initialization and compare first and second errors.
5. Draw a class-loader leak path involving a thread context class loader.

## Chapter summary

Class loading turns named byte streams into loader-scoped runtime types. Linking verifies safety, prepares default static state, and resolves symbolic relationships. Initialization later runs Java code under a synchronized, once-only protocol. Loader identity enables isolation but makes namespaces and leaks important. Precise phase and loader reasoning turns vague classpath failures into diagnosable engineering problems.

## Revision checklist

- [ ] I can distinguish all loading, linking, and initialization phases.
- [ ] I know preparation assigns defaults before Java initializers run.
- [ ] I can explain built-in loaders without outdated inheritance assumptions.
- [ ] I can state class identity as name plus defining loader.
- [ ] I can explain normal, failed, and circular initialization.
- [ ] I can diagnose the major class-not-found and linkage error categories.

