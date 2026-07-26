# 21. Nested Types, Enums, Annotations, and Reflection

## Learning objectives

By the end of this chapter, you should be able to:

- distinguish static nested, inner, local, and anonymous classes and their capture rules;
- design enums with behavior, stable external representation, and collection support;
- define annotations with appropriate targets, retention, defaults, and repeatability;
- inspect and invoke types reflectively while respecting access and module boundaries; and
- decide when metadata-driven runtime behavior is worth its safety and complexity costs.

## Why this matters at SDE-2

Framework-heavy Java uses all four topics. Builders and implementation details use nested types, domain state uses enums, dependency injection and serialization use annotations, and frameworks connect metadata to behavior through reflection. An SDE-2 engineer must understand what the framework is doing when scanning fails, an enum evolves, or reflective access is denied.

These features are also design tools. Used carefully, they localize helper types and declarative metadata. Used casually, they create hidden control flow, memory retention, brittle names, and startup surprises.

## First-principles model

A nested type is declared inside another declaration or block. A static nested class has no implicit enclosing object. A non-static member inner class is associated with an enclosing instance. Local and anonymous classes live inside executable code and can capture final or effectively final local values.

An enum is a special class with a fixed declared set of named instances. Each constant is constructed during enum class initialization. An annotation is structured metadata attached to supported program elements or type uses. Its retention policy determines whether it reaches source tools, class files, or runtime reflection.

Reflection treats types and members as data through `Class`, `Method`, `Field`, `Constructor`, and related APIs. It can discover and invoke behavior dynamically, but it shifts checks from compilation to runtime and remains constrained by access control and modules.

> **Specification boundary:** Java defines nested-class semantics, enum identity and initialization, annotation metadata, and reflection APIs. Compiler-generated class names, synthetic capture-field names, framework scan order, and reflective inflation or accessor implementation are not stable language contracts.

## Core terminology

- **Static nested class:** nested class with no implicit outer instance.
- **Inner class:** non-static nested class associated with an enclosing instance.
- **Local class:** named class declared inside a block.
- **Anonymous class:** unnamed class expression that extends or implements one type.
- **Capture:** retaining values from an enclosing lexical scope.
- **Enum constant:** canonical instance declared in an enum body.
- **Annotation element:** parameterless metadata member with a restricted return type.
- **Retention:** source-only, class-file, or runtime availability.
- **Type-use annotation:** annotation applicable wherever a type is used.
- **Reflective access:** discovering or operating on program structure at runtime.

## Detailed mechanics

### Member nested and inner classes

```java
class Cache {
    private final String region;

    Cache(String region) { this.region = region; }

    static final class Key {
        private final String value;
        Key(String value) { this.value = value; }
    }

    final class Entry {
        String description() { return region + " entry"; }
    }
}
```

`Cache.Key` can be constructed without a `Cache`: `new Cache.Key("k")`. It cannot directly access an instance field because it has no implicit receiver. `Entry` requires an enclosing instance: `cache.new Entry()`. Its methods can access outer private state.

Use a static nested class unless the semantic relationship truly requires an outer instance. An inner instance retains its enclosing instance, which can unintentionally keep a large object graph alive. Static declarations inside inner classes have become more permissive in modern Java, but an explicit outer relationship is still the defining semantic distinction.

Nested types follow access control and can access private members of their nest under language rules. At the JVM level, modern class files use nestmate metadata to support this without exposing those members publicly.

### Local and anonymous classes

A local class is visible only in its declaring block. An anonymous class combines allocation with an unnamed subclass or interface implementation:

```java
Runnable task = new Runnable() {
    @Override
    public void run() {
        System.out.println("running");
    }
};
```

Anonymous classes can declare fields and methods but no explicit constructor, and their unique runtime type is awkward to name. Lambdas are usually clearer for functional interfaces, but anonymous classes differ: `this` refers to the anonymous object, they can hold extra state, and they create an actual class instance with class-style semantics.

Local variables captured by local/anonymous classes or lambdas must be final or effectively final. The code captures a value, not a mutable local variable slot. Mutable objects referenced by the captured value can still change.

### Enums

Every enum implicitly extends `Enum<E>`, cannot extend another class, and may implement interfaces. Constants are public static final instances. Enum constructors are never public or protected and run during class initialization.

```java
enum Severity {
    INFO(1), WARNING(2), CRITICAL(3);

    private final int rank;

    Severity(int rank) { this.rank = rank; }
    int rank() { return rank; }
}
```

Compare enum constants with `==`; identity is the intended semantics and null-safe when the known constant is on the left. `name()` is the declared source name, `ordinal()` is its position, and `toString()` may be overridden. Never persist ordinal because reordering or inserting constants changes it. Persist an explicit stable code if external compatibility matters.

`Enum.valueOf` matches the exact declared name and throws for unknown text. `values()` returns a new array each call. `EnumSet` and `EnumMap` are specialized, type-safe collections and are preferable to manual bit masks or general-purpose maps for enum domains.

Constants may have constant-specific class bodies that override behavior, but a large behavioral enum can become a closed service locator. Keep behavior cohesive and consider strategies when implementations need independent deployment.

Switches over enums should account for evolution. An exhaustive switch expression without default helps recompilation reveal a new constant; separately compiled old code may still encounter a defensive runtime failure with a newer enum.

### Declaring annotations

```java
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.METHOD)
public @interface Operation {
    String value();
    boolean idempotent() default false;
}
```

Annotation elements may return primitives, `String`, `Class`, enum, annotation, or one-dimensional arrays of these. They have no parameters or throws clauses. Values must be compile-time-compatible annotation values; null is not allowed. Defaults belong to the annotation method declaration and are applied when read, so changing a default can change behavior of already compiled annotated code at runtime.

Important meta-annotations include:

- `@Target` for legal declaration or type-use sites;
- `@Retention` with SOURCE, CLASS, or RUNTIME;
- `@Documented` for API documentation inclusion;
- `@Inherited`, which applies only to class-level annotation lookup through superclass inheritance, not interfaces, members, or general meta-annotation composition; and
- `@Repeatable`, whose value names a container annotation.

Runtime processors need RUNTIME retention. Compile-time annotation processors usually work from source/model APIs and do not require runtime retention. TYPE_USE supports nullness and other type-system qualifiers, while TYPE targets a type declaration.

### Reflection

Obtain a `Class<?>` from a literal (`Order.class`), object (`value.getClass()`), primitive literal (`int.class`), array class, or `Class.forName`. A class literal normally does not initialize the class; certain reflective uses and `Class.forName(String)` default behavior can initialize it.

`getMethods()` returns public methods including inherited ones. `getDeclaredMethods()` returns methods declared directly regardless of access, excluding inherited methods. Similar distinctions apply to fields and constructors.

```java
Operation metadata = method.getAnnotation(Operation.class);
Object result = method.invoke(target, argument);
```

Reflective invocation wraps an exception thrown by the target in `InvocationTargetException`; inspect its cause. Argument mismatch and access failures are runtime exceptions or checked reflective failures rather than compile-time errors.

Calling `setAccessible(true)` or `trySetAccessible()` does not grant unlimited authority. In Java 17 and 21, strong module encapsulation can deny deep reflection when the declaring package is not opened to the caller's module. Public access may require package export; deep reflective access usually requires `opens`, command-line configuration, or an API designed for it.

Method handles in `java.lang.invoke` offer a typed, composable alternative and often integrate better with JVM optimization, but lookup access rules still apply. Reflection is appropriate for frameworks and tools, not as a replacement for ordinary polymorphism when types are known.

## Worked Java example

This small registry discovers annotated methods on an explicitly supplied handler. It avoids uncontrolled classpath scanning.

```java
import java.lang.annotation.*;
import java.lang.reflect.*;
import java.util.*;

public class OperationRegistry {
    @Retention(RetentionPolicy.RUNTIME)
    @Target(ElementType.METHOD)
    @interface Operation {
        String value();
    }

    static final class Handler {
        @Operation("health")
        public String health() { return "ok"; }
    }

    static Map<String, Method> discover(Class<?> type) {
        Map<String, Method> result = new HashMap<>();
        for (Method method : type.getDeclaredMethods()) {
            Operation operation = method.getAnnotation(Operation.class);
            if (operation == null) continue;
            if (method.getParameterCount() != 0 || method.getReturnType() != String.class) {
                throw new IllegalArgumentException("invalid operation method: " + method);
            }
            if (result.putIfAbsent(operation.value(), method) != null) {
                throw new IllegalArgumentException("duplicate operation: " + operation.value());
            }
        }
        return Map.copyOf(result);
    }

    public static void main(String[] args) throws ReflectiveOperationException {
        Handler handler = new Handler();
        Method method = discover(Handler.class).get("health");
        System.out.println(method.invoke(handler));
    }
}
```

The nested annotation and handler are scoped to the example. Production registries should validate public accessibility, inherited-method policy, duplicate names, return contracts, and target exceptions deliberately.

## Execution or memory walkthrough

Loading `OperationRegistry` makes its nested class metadata available. `Handler.class` supplies a class object without constructing a handler. `getDeclaredMethods` creates reflection descriptors for directly declared methods; their order is unspecified, so the registry must not depend on iteration order.

`getAnnotation` returns a runtime annotation representation because retention is RUNTIME. The registry validates the signature and stores the `Method` under `health`, then creates an unmodifiable map. `method.invoke(handler)` checks receiver and arguments and dispatches to `health`, which returns `"ok"`.

If `health` threw, `invoke` would throw `InvocationTargetException` with the application exception as cause. If the method were inaccessible across a module boundary, discovery might still see a declared descriptor, but invocation could fail unless access was legitimately available.

## Complexity and performance

Enum comparison and ordinal-based specialized collection operations are generally O(1). `values()` copies O(n) constants. Reflection discovery is O(m) in inspected members plus annotation and validation work. Cache validated descriptors rather than scanning on each request.

Reflective invocation adds access checks, argument adaptation, boxing, and indirection compared with a direct call. For startup wiring or administrative paths this is usually irrelevant. For a hot serializer loop, cache method handles or generated accessors and benchmark realistic data.

> **HotSpot note:** Reflection and method-handle implementations can optimize repeated access, and the JIT may inline adapted method-handle chains. Exact thresholds and generated accessor strategies are implementation-specific and version-dependent.

## Edge cases and common mistakes

- A non-static inner instance retains an enclosing instance.
- Captured locals must be effectively final, but referenced mutable state can still race.
- Anonymous-class `this` differs from lambda `this`.
- Persisting enum ordinal breaks when constants are reordered.
- Persisting `toString()` breaks when presentation changes; use an explicit stable code.
- `Enum.valueOf` is case-sensitive and rejects unknown future values.
- Runtime reflection cannot see annotations with SOURCE or CLASS retention through ordinary annotation APIs.
- `@Inherited` does not propagate method annotations or interface annotations.
- Reflection member order is unspecified.
- `getMethod` and `getDeclaredMethod` differ in inheritance and access scope.
- Target exceptions arrive wrapped by `InvocationTargetException`.
- Strong module encapsulation can reject deep reflection despite `setAccessible`.
- Annotation defaults can change runtime interpretation without recompiling clients.

## Production engineering notes

Prefer static nested types for builders and implementation helpers. Make inner ownership explicit and avoid registering long-lived callbacks that accidentally retain an outer service. Captured state used concurrently needs the same synchronization as any other shared state.

Give externally serialized enums stable codes and an unknown-value policy. Adding a constant is source-compatible in many locations but can break exhaustive consumers, database constraints, or generated clients. Roll out readers before writers when protocols evolve.

Treat annotations as an API. Specify valid placement, inheritance/merge behavior, duplicates, defaults, and validation timing. Fail fast at startup rather than on the first request. Limit reflection to known packages or explicit registrations, cache results, respect modules, and never invoke untrusted member names without authorization and validation.

## Interview questions and model answers

**What is the difference between a static nested class and an inner class?**

A static nested class has no implicit enclosing instance and cannot directly use outer instance state. A member inner class is associated with a particular outer object and can access it, which also means it retains that object.

**Why are captured local variables effectively final?**

The nested object may outlive the method activation, so it captures the local's value rather than sharing its stack variable slot. Reassignment would create ambiguous value semantics. The captured reference can still designate mutable data.

**Why should enum ordinal not be persisted?**

Ordinal is declaration position, not a stable domain code. Inserting or reordering constants changes it. Persist an explicit immutable code and map unknown values intentionally.

**What does runtime retention mean?**

The annotation is recorded in the class file and exposed through runtime reflection APIs. CLASS retention records it but ordinary runtime reflection does not expose it; SOURCE retention need not enter the class file.

**Can reflection access every private field?**

No. Access depends on the caller, language access, security context, and module openness. Strong encapsulation in Java 17 and 21 can prevent deep reflection unless the package is opened appropriately.

## Exercises

1. Demonstrate an inner callback retaining its outer object, then convert it to a static nested class with explicit state.
2. Define an enum with stable external codes and safe unknown-code parsing.
3. Create a repeatable runtime method annotation and inspect both repeated instances.
4. Extend the registry to unwrap `InvocationTargetException` while preserving its cause.
5. Compare `getMethods` and `getDeclaredMethods` on a subclass with public and private members.
6. Design a startup validation policy for annotated handlers covering duplicates, visibility, signatures, and module access.

## Chapter summary

Nested types control lexical scope and object relationships; only inner forms carry an implicit enclosing instance. Enums are canonical typed instances and need explicit external codes for evolution. Annotations provide constrained metadata whose target and retention define its reach. Reflection connects metadata to runtime behavior but moves checks later and remains subject to access and modules. Use explicit registration, validation, caching, and narrow authority to keep dynamic systems understandable.

## Revision checklist

- [ ] I distinguish static nested, inner, local, and anonymous classes.
- [ ] I understand capture, effectively final locals, and outer-instance retention.
- [ ] I use enum identity safely and never persist ordinal.
- [ ] I choose annotation targets, retention, defaults, and repeatability deliberately.
- [ ] I know the limits of `@Inherited`.
- [ ] I distinguish declared from inherited reflection queries.
- [ ] I unwrap target failures and respect module encapsulation.
- [ ] I validate and cache reflective metadata outside hot request paths.
