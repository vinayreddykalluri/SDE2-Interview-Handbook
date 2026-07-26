# 14. Methods, Overloading, Varargs, and Pass-by-Value

## Learning objectives

By the end of this chapter, you should be able to:

- explain method declarations, signatures, return contracts, and invocation;
- predict compile-time overload selection and runtime overriding separately;
- use varargs without ambiguity, mutation leaks, or heap-pollution surprises;
- demonstrate Java's pass-by-value rule for primitives and references; and
- design methods with cohesive responsibilities and stable API boundaries.

## Why this matters at SDE-2

Methods are Java's primary unit of behavior and API design. Subtle errors arise when overloaded APIs accept `null`, generic and varargs methods interact, or a developer expects a callee to replace the caller's reference. These issues appear in framework APIs, backward-compatible library evolution, and interviews about dispatch.

At SDE-2 level, a correct method is not enough. Its contract must communicate nullability, mutation, failure, ownership, cost, and concurrency expectations. Overload sets must remain predictable as the codebase evolves.

## First-principles model

A method declaration gives code a name, parameter list, return type, modifiers, optional type parameters, and declared checked exceptions. On invocation, Java evaluates the receiver and arguments, creates a new invocation context, and initializes each parameter with a copy of its corresponding argument value.

For a primitive argument, the copied value is the primitive. For a reference argument, the copied value is the reference. Caller and callee can therefore refer to the same mutable object, but assigning the callee's parameter cannot replace the caller's variable. Java has no pass-by-reference parameter mode.

Invocation has two major decisions. The compiler selects an applicable declaration using the compile-time types and overload rules. If the chosen method is an overridable instance method, runtime dynamic dispatch selects the most specific override for the receiver's actual class.

> **Specification boundary:** Java is always pass-by-value. References are values that can be copied. The JVM's physical calling convention may use registers, stack locations, or optimized inlining, but these do not create source-language pass-by-reference semantics.

## Core terminology

- **Formal parameter:** variable declared by a method.
- **Argument:** expression supplied at a call site.
- **Signature:** method name plus type parameters after adaptation and formal parameter types; return type is not part of overload identity.
- **Overloading:** same method name with different parameter lists, resolved at compile time.
- **Overriding:** subclass instance method supplies behavior for an inherited method, dispatched at runtime.
- **Arity:** number of arguments or parameters.
- **Varargs:** variable-arity final parameter declared with `T...` and represented as an array.
- **Applicable method:** overload that can accept the arguments in a particular invocation phase.
- **Covariant return:** override narrows a reference return type.
- **Bridge method:** compiler-generated adapter that preserves polymorphism after type erasure.

## Detailed mechanics

### Declaration and contracts

```java
public static <T> T require(T value, String message) {
    if (value == null) {
        throw new IllegalArgumentException(message);
    }
    return value;
}
```

`public static` are modifiers, `<T>` declares a method type parameter, the next `T` is the return type, and two formal parameters follow. A non-`void` method must not complete normally without returning a compatible value. Returning a reference does not copy the object.

Parameter names are generally not part of binary method identity. Overloading cannot differ only by return type, parameter names, `throws` clauses, or generic type arguments erased to the same signature.

### Overload resolution phases

Conceptually, Java searches in phases to preserve compatibility:

1. fixed-arity methods applicable without boxing or unboxing;
2. fixed-arity methods allowing boxing and unboxing; then
3. variable-arity methods.

Within a phase, it chooses a most-specific method. Widening a primitive is preferred over boxing because it succeeds in an earlier phase.

```java
static String choose(long value) { return "long"; }
static String choose(Integer value) { return "Integer"; }
static String choose(int... values) { return "varargs"; }

System.out.println(choose(5)); // long
```

The `int` literal widens to `long` during strict fixed-arity invocation. Boxing and varargs are considered only if no earlier-phase candidate works.

`null` can make an overload ambiguous when unrelated reference parameter types are equally specific:

```java
static void send(String value) {}
static void send(StringBuilder value) {}
// send(null); // ambiguous
```

Casting documents the intended overload, but a public API that repeatedly needs such casts probably has a poor overload set.

### Static selection and dynamic dispatch

```java
class Parent {
    String label(Object value) { return "Parent:Object"; }
}
class Child extends Parent {
    @Override String label(Object value) { return "Child:Object"; }
    String label(String value) { return "Child:String"; }
}

Parent p = new Child();
System.out.println(p.label("x")); // Child:Object
```

The compile-time type of `p` exposes only `Parent.label(Object)`, so overload selection chooses that signature. Runtime dispatch then finds `Child`'s override of `label(Object)`. It does not restart overload selection to discover `label(String)`.

Static methods are hidden, not overridden. Private methods are not inherited as overridable members. Constructors are not methods and are not inherited. `final` instance methods cannot be overridden.

### Varargs

`void log(String... messages)` is compiled using a `String[]` parameter. A call such as `log("a", "b")` causes the call site to create an array, while `log(existingArray)` can pass the existing array directly. This creates an ownership risk if the callee mutates it.

Only the final formal parameter may be varargs. A method is treated as fixed arity first, which is why passing an existing array does not require another wrapper array.

Generic varargs can create non-reifiable array types. The compiler warns because the runtime array cannot fully check its element type. `@SafeVarargs` is a promise by the author that an eligible method does not perform unsafe operations on or expose its varargs array. It suppresses warnings; it does not make unsafe code safe.

```java
@SafeVarargs
static <T> java.util.List<T> immutableList(T... values) {
    return java.util.List.copyOf(java.util.Arrays.asList(values));
}
```

This method reads the array and returns an independent unmodifiable list. It does not store incompatible values or expose the array.

### Pass-by-value and mutation

```java
static void change(StringBuilder builder, int number) {
    builder.append("!");
    builder = new StringBuilder("replacement");
    number = 99;
}

StringBuilder text = new StringBuilder("hello");
int count = 1;
change(text, count);
System.out.println(text);  // hello!
System.out.println(count); // 1
```

The copied reference lets `change` mutate the original builder. Reassigning the parameter changes only the callee's local copy. The copied integer is similarly independent.

## Worked Java example

This API avoids an ambiguous overload set and defensively handles its variable-arity input.

```java
import java.time.Instant;
import java.util.List;
import java.util.Objects;

public final class AuditEvent {
    private final String action;
    private final Instant occurredAt;
    private final List<String> tags;

    private AuditEvent(String action, Instant occurredAt, List<String> tags) {
        this.action = Objects.requireNonNull(action);
        this.occurredAt = Objects.requireNonNull(occurredAt);
        this.tags = List.copyOf(tags);
    }

    public static AuditEvent now(String action, String... tags) {
        Objects.requireNonNull(tags, "tags");
        return at(action, Instant.now(), List.of(tags.clone()));
    }

    public static AuditEvent at(
            String action, Instant occurredAt, List<String> tags) {
        return new AuditEvent(action, occurredAt, tags);
    }

    public List<String> tags() {
        return tags;
    }
}
```

Named factories distinguish convenience construction from deterministic construction. The full method takes a collection rather than adding many same-name overloads. `List.copyOf` validates elements and creates an unmodifiable snapshot when necessary.

## Execution or memory walkthrough

For `AuditEvent.now("login", "security", "user")`, the call site constructs a `String[]` containing two references. `now` receives a copy of the array reference. It checks that reference, clones the array, and creates a list view over the clone. The clone prevents a caller-supplied array from being affected by internal processing.

`at` receives copies of three references. The constructor receives another set of copied references. `List.copyOf` rejects null elements and returns an unmodifiable list whose contents cannot be changed through the original list. The final fields receive references once; immutability also depends on the referenced values. `String` and `Instant` are immutable, and the tag list is unmodifiable with immutable string elements.

The JVM may inline these calls and eliminate some intermediate allocations, but semantically every parameter is initialized from a copied value.

## Complexity and performance

A direct method invocation is O(1) before considering method work. Virtual dispatch is also constant time conceptually. Varargs array creation is O(n) time and space in the argument count; cloning adds another O(n). `List.copyOf` may copy in O(n), depending on its input.

Tiny methods can be inlined by a JIT, so architectural clarity usually matters more than avoiding calls. Varargs on a high-frequency logging or serialization path can allocate even when the callee does little. Offer collection, builder, or fixed-arity forms only when measurement and API clarity support them.

> **HotSpot note:** HotSpot uses profiling and class-hierarchy knowledge to inline or devirtualize calls and may scalar-replace short-lived varargs arrays. These are optimizations, not guarantees that allocation has no cost.

## Edge cases and common mistakes

- Return type alone cannot distinguish overloads.
- `null` can be ambiguous or select a surprising most-specific reference overload.
- Widening, boxing, and varargs do not have equal priority.
- Autounboxing during invocation can throw `NullPointerException`.
- Overloading a method in a subclass does not override a different parameter signature.
- A static call is selected using compile-time type, even when invoked through an instance expression.
- Passing an existing array to a varargs method may let the callee mutate caller-owned data.
- Calling a varargs method with plain `null` can mean a null array rather than one null element and may produce a warning. Cast explicitly if unavoidable.
- Generic varargs plus array writes can produce heap pollution and delayed `ClassCastException`.
- A method that returns an internal mutable collection leaks representation.
- Recursive methods need both a base case and progress toward it; large depth can exhaust the thread stack.

## Production engineering notes

Prefer cohesive methods named for business intent. Keep validation near boundaries and make ownership explicit: snapshot, view, mutable result, or caller-owned input. Public overloads should share one canonical implementation so fixes do not diverge.

Evolving an API requires source, binary, and behavioral compatibility analysis. Adding an overload can make existing source calls ambiguous or change which declaration recompilation selects. Changing a return type is not overloading; covariant changes are limited to overriding. Framework reflection may also observe bridge, synthetic, or parameter metadata differently.

Avoid boolean parameter clusters such as `send(data, true, false)`. An options object, enum, or separate named operation communicates more. Document nullability and checked failures; do not make callers reverse-engineer them from implementation.

## Interview questions and model answers

**Is Java pass-by-reference for objects?**

No. The argument value is a reference, and Java copies that value into the parameter. Callee and caller can use their copies to reach the same object, so mutation is visible. Parameter reassignment is not visible to the caller.

**What is selected at compile time versus runtime?**

Overload resolution and static method selection use compile-time types. For a selected overridable instance signature, runtime dispatch chooses the override associated with the actual receiver class.

**Why does primitive widening beat boxing?**

Invocation resolution first searches fixed-arity methods applicable without boxing. Only if none is suitable does it search with boxing, followed later by varargs. This phased design preserves older method-selection behavior.

**What does `@SafeVarargs` guarantee?**

It is an assertion by the method author that operations on the generic varargs parameter are type safe. The compiler trusts it for warnings; neither the annotation nor runtime verifies the implementation. Apply it only to eligible non-overridable methods and audit the body.

**Can an override throw broader checked exceptions?**

No. It may declare the same checked exceptions or narrower subclasses, and it may omit them. Unchecked exceptions are not restricted in the same way. This preserves the caller's compile-time exception contract.

## Exercises

1. Create overloads for `f(long)`, `f(Integer)`, and `f(int...)`; predict calls with `1`, `Integer.valueOf(1)`, and an `int[]`.
2. Trace a base-reference/subclass-object example containing one overload and one override.
3. Rewrite a `swap(Object a, Object b)` attempt and explain why it cannot swap caller variables. Design a return-value alternative.
4. Audit a generic varargs method for array writes, escaping references, and unsafe casts.
5. Refactor a method with four boolean parameters into a clear options type.
6. Design tests proving that a method does not retain or mutate a caller's input array.

## Chapter summary

Methods establish behavioral boundaries. Java initializes every parameter with a copied argument value, including copied references. Overload resolution is compile-time and phased; overriding is runtime dispatch of a previously selected instance signature. Varargs are arrays with allocation, ownership, and generic safety implications. Strong APIs minimize ambiguous overloads, document mutation and failure, and funnel convenience entry points into one canonical implementation.

## Revision checklist

- [ ] I can identify what is and is not part of a method signature.
- [ ] I understand the widening, boxing, and varargs resolution phases.
- [ ] I separate overload selection from override dispatch.
- [ ] I can demonstrate pass-by-value using a mutable object and reassignment.
- [ ] I treat varargs arrays as ordinary arrays with ownership concerns.
- [ ] I know why generic varargs can cause heap pollution.
- [ ] I design overload sets that remain clear with `null` and future evolution.
- [ ] I document method contracts for mutation, nullability, cost, and failure.
