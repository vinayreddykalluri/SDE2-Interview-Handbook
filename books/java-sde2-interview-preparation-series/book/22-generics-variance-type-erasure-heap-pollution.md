# 22. Generics, Variance, Type Erasure, and Heap Pollution

## Learning objectives

By the end of this chapter, you should be able to:

- design generic classes and methods with useful bounds;
- explain invariance and apply upper- and lower-bounded wildcards;
- reason about wildcard capture and type inference;
- describe erasure, bridge methods, reifiability, and generic restrictions; and
- prevent raw-type leaks, unsafe generic arrays, and heap pollution.

## Why this matters at SDE-2

Generics make collection and framework APIs reusable while preserving type safety. Weak generic design forces casts onto callers; overly clever signatures become unreadable. SDE-2 engineers must balance precision and usability, especially in repositories, event buses, serializers, and reusable algorithms.

Interview questions often focus on why `List<Integer>` is not a `List<Number>`, what `? super T` permits, and how erasure causes bridge methods or heap pollution. The production version is harder: warnings appear far from the eventual `ClassCastException`.

## First-principles model

A type parameter is a compile-time variable ranging over reference types. A parameterized type such as `List<String>` states that operations observe a consistent element type. Java implements generics primarily through erasure: most type arguments do not exist as independently testable runtime types.

Generic classes are invariant by default. Even though `Integer` is a subtype of `Number`, `List<Integer>` is not a subtype of `List<Number>`, because the latter would allow insertion of a `Double`. Wildcards express use-site variance: `? extends Number` is an unknown specific subtype useful for producing numbers, and `? super Integer` is an unknown specific supertype useful for consuming integers.

Heap pollution occurs when a variable of a parameterized type refers to an object that does not satisfy that parameterization. It usually begins with an unchecked operation and fails later when a compiler-inserted cast encounters the wrong value.

> **Specification boundary:** Java specifies generic typing, erasure translation, casts, and bridge behavior. It does not preserve arbitrary type arguments for runtime `instanceof`, and reflective generic signatures are metadata rather than a fully reified runtime type system.

## Core terminology

- **Type parameter:** declared type variable such as `<T>`.
- **Type argument:** supplied type such as `String` in `List<String>`.
- **Parameterized type:** generic declaration instantiated with arguments.
- **Bound:** restriction such as `<T extends Comparable<? super T>>`.
- **Invariance:** no subtype relation follows between parameterizations merely from their arguments.
- **Wildcard:** unknown type argument written `?` with optional upper or lower bound.
- **Capture conversion:** compiler treats a wildcard as a fresh unknown type.
- **Erasure:** mapping generic types to non-generic runtime representations with casts as needed.
- **Reifiable type:** type whose runtime representation retains enough information for checks.
- **Heap pollution:** runtime value violates a parameterized variable's promised type.

## Detailed mechanics

### Generic declarations and methods

```java
final class Box<T> {
    private final T value;

    Box(T value) { this.value = value; }
    T value() { return value; }
}

static <T> T first(java.util.List<T> values) {
    if (values.isEmpty()) throw new IllegalArgumentException("empty");
    return values.get(0);
}
```

`T` is in scope for instance members of `Box`, but static members cannot use the class's `T` because static state is shared across all parameterizations. A static method may declare its own `<T>` before the return type.

Primitive types cannot be type arguments, so use wrappers or primitive-specialized APIs. `Box<String>` and `Box<Integer>` share the same raw runtime class object under erasure.

Type inference uses argument context, target type, and bounds. Explicit type witnesses such as `Collections.<String>emptyList()` are occasionally useful when inference lacks context, but modern Java resolves most ordinary calls.

### Bounds

An upper bound exposes operations supported by the bound:

```java
static <T extends Number> double total(java.util.List<T> values) {
    double sum = 0;
    for (T value : values) sum += value.doubleValue();
    return sum;
}
```

Multiple bounds use one class first, followed by interfaces: `<T extends Base & Comparable<T> & Serializable>`. Erasure of a type variable is its leftmost bound, which makes bound order relevant to generated binary signatures.

Recursive or F-bounds describe operations in terms of the implementing type. A common ordering bound is `<T extends Comparable<? super T>>`, which accepts a type comparable to itself or one of its supertypes. This is more flexible than `Comparable<T>` for inherited comparison definitions.

### Invariance and wildcards

Given `List<Integer> integers`, assignment to `List<Number>` is illegal. Both can, however, be viewed through `List<? extends Number>`:

```java
java.util.List<? extends Number> source = java.util.List.of(1, 2, 3);
Number number = source.get(0);
// source.add(4); // rejected; exact captured subtype is unknown
```

You cannot add any non-null value because the actual list might be `List<Double>`. Null is type-compatible but adding it is rarely useful.

For a consumer:

```java
java.util.List<? super Integer> target = new java.util.ArrayList<Number>();
target.add(10);
Object value = target.get(0);
```

The target is known to accept `Integer`, but reading yields only `Object` because its exact element type might be `Object` or `Number`. The mnemonic PECS is useful: producer extends, consumer super. It describes the role of a parameter, not an absolute law. A mutable parameter used for both reading and writing often needs an exact `List<T>`.

An unbounded `List<?>` means a list of some one unknown type, not a list that can contain any types. It is safer than raw `List`: reads yield `Object`, and arbitrary writes are rejected.

### API variance examples

```java
static <T> void copy(
        java.util.List<? extends T> source,
        java.util.List<? super T> destination) {
    for (T element : source) {
        destination.add(element);
    }
}
```

This accepts `List<Integer>` into `List<Number>` or `List<Object>`. If both parameters were `List<T>`, invariant inference would reject useful combinations.

Return types usually should not expose wildcards when the method can state a concrete parameterization. Returning `List<? extends Animal>` makes callers handle an unknown type; `List<Animal>` or a method type variable often gives a clearer ownership contract.

### Wildcard capture

This seemingly simple method fails if written directly because two calls to `list.get` and `list.set` must agree on the captured unknown type. A helper captures it:

```java
static void reverseFirstTwo(java.util.List<?> list) {
    reverseFirstTwoCaptured(list);
}

private static <T> void reverseFirstTwoCaptured(java.util.List<T> list) {
    if (list.size() >= 2) {
        T first = list.get(0);
        list.set(0, list.get(1));
        list.set(1, first);
    }
}
```

The public API correctly accepts a list of any single element type. The private method gives that captured type a name so read values can be written back safely.

### Erasure and bridge methods

Erasure maps an unbounded type variable to `Object`, a bounded variable to its leftmost bound, and a parameterized type to its raw declaration. The compiler inserts casts where a caller expects a more specific result.

```java
interface Provider<T> { T get(); }
final class TextProvider implements Provider<String> {
    public String get() { return "text"; }
}
```

After erasure, `Provider.get` returns `Object`, while the implementation returns `String`. The compiler emits a synthetic bridge method equivalent in effect to `Object get()` delegating to `String get()`, preserving polymorphism. Reflection and stack traces can expose bridge methods; tools should check `Method.isBridge()` and `isSynthetic()` when appropriate.

Two overloads whose signatures erase identically cannot coexist, such as `process(List<String>)` and `process(List<Integer>)`. Neither can code test `value instanceof List<String>` because runtime sees only `List`; `value instanceof List<?>` is legal.

### Reifiability and arrays

Primitive types, non-generic classes, raw types, unbounded-wildcard parameterizations, and certain arrays of reifiable types are reifiable. `List<String>` is not. Arrays are reified and covariant, which conflicts with erased invariant generics. Therefore `new T[10]` and `new List<String>[10]` are illegal.

A generic data structure may allocate `Object[]` internally and cast at a controlled boundary, accept an array constructor such as `IntFunction<T[]>`, or prefer collections. Any unchecked cast must be locally justified and representation-safe.

### Raw types and heap pollution

Raw types exist for backward compatibility and erase parameter checks:

```java
java.util.List<String> names = new java.util.ArrayList<>();
java.util.List raw = names;
raw.add(42);                       // unchecked warning
// String first = names.get(0);   // ClassCastException here
```

The list object now violates the promise `List<String>`. The failure appears at the later compiler-inserted cast, not at the unsafe write. Treat unchecked warnings as defects unless a narrow adapter proves safety.

Generic varargs add an array boundary. A `T...` parameter has an array runtime representation that cannot fully represent non-reifiable `T`. `@SafeVarargs` may be applied to eligible methods and constructors only when the body neither corrupts nor unsafely exposes the array.

## Worked Java example

This generic maximum works for types comparable to a supertype and accepts any producing collection.

```java
import java.util.Collection;
import java.util.List;
import java.util.NoSuchElementException;

public class GenericMaximum {
    static <T extends Comparable<? super T>> T max(
            Collection<? extends T> values) {
        var iterator = values.iterator();
        if (!iterator.hasNext()) {
            throw new NoSuchElementException("empty input");
        }
        T best = iterator.next();
        while (iterator.hasNext()) {
            T candidate = iterator.next();
            if (candidate.compareTo(best) > 0) {
                best = candidate;
            }
        }
        return best;
    }

    public static void main(String[] args) {
        List<Integer> values = List.of(4, 9, 2);
        Number result = max(values);
        System.out.println(result); // 9
    }
}
```

`Collection<? extends T>` permits a collection of a subtype of the inferred result type. The comparison bound ensures every candidate can compare itself with a compatible supertype.

## Execution or memory walkthrough

For `max(values)`, inference can choose `Integer` for `T`; the target assignment to `Number` also accepts the returned Integer. The iterator has type compatible with the captured producer, and each `next` result can be widened to `T`.

`best` starts as 4. Candidate 9 compares greater and replaces it; candidate 2 does not. The returned reference designates the same immutable Integer object held by the list, not a copy of an object.

At runtime, erasure represents much of this using `Comparable`, `Collection`, and casts inserted at use sites. The static proof prevents unrelated elements from entering through this API. No runtime object representing the type variable `T` is created.

## Complexity and performance

Generics do not change the algorithmic complexity of operations. `max` is O(n) time and O(1) additional space. Erasure normally avoids per-parameterization class generation, but wrapper types can introduce boxing and allocation when primitives pass through generic collections.

Bridge calls and casts are small constant costs and are often optimized. Wildcards are compile-time constructs and do not allocate. Prefer primitive-specialized structures only after data volume and profiling justify complexity.

> **HotSpot note:** HotSpot can inline generic erased code, eliminate casts proven redundant, and optimize some boxing. Java generics do not promise specialization for each type argument, and current optimization should not be treated as a no-boxing guarantee.

## Edge cases and common mistakes

- `List<Dog>` is not a subtype of `List<Animal>`.
- `? extends T` supports safe reads as T but not arbitrary writes.
- `? super T` supports writes of T but reads only as Object.
- `List<?>` is type-safe unknown, while raw `List` disables checks.
- Primitive types cannot be generic arguments.
- Static members cannot use a class's type parameter.
- Overloads cannot differ only by generic arguments with the same erasure.
- `instanceof List<String>` and `new T[]` are illegal because types are not reified.
- An unchecked cast can defer a failure far away from its source.
- `@SuppressWarnings("unchecked")` documents suppression, not proof; keep its scope tiny and comment the invariant.
- `@SafeVarargs` is unsafe if the method writes through an alias or returns its varargs array.
- A wildcard in every return type burdens callers and can indicate an over-general API.
- Reflection's `getGenericType` reads signatures but does not make runtime values generically reified.

## Production engineering notes

Expose variance at API boundaries and concrete type parameters within implementations. Accept the least restrictive producer or consumer shape the method truly supports. Keep public signatures readable; a named domain interface is sometimes better than a page of bounds.

Eliminate raw types in modern code. At legacy or reflection boundaries, validate runtime values once, copy into a correctly typed structure, and isolate a justified unchecked cast. Compile with unchecked warnings enabled and fail builds on unexplained new warnings.

Type tokens such as `Class<T>` reify only a raw class and cannot represent `List<String>`. Frameworks use richer type descriptors or anonymous-superclass capture for nested generic types. Understand whether their metadata survives proxies, serialization, and ahead-of-time compilation.

## Interview questions and model answers

**Why is List<Integer> not a subtype of List<Number>?**

If it were, a method receiving `List<Number>` could add a Double to an actual integer list. Invariance prevents that. Use `List<? extends Number>` for a producer view or `List<? super Integer>` for a consumer view.

**What does PECS mean?**

Producer extends, consumer super. An input that only produces T can use `? extends T`; one that only consumes T can use `? super T`. A structure both read and written as the same exact type often uses `T` directly.

**What is type erasure?**

The compiler checks generic types, then maps most parameterized uses to raw runtime representations, inserting casts and sometimes bridge methods. This supports binary compatibility but prevents runtime tests of arbitrary type arguments.

**What is heap pollution?**

A parameterized variable refers to data that violates its claimed type, typically because of a raw type, unchecked cast, or unsafe generic varargs operation. A later generated cast then throws, far from the unsafe operation.

**Why does the compiler generate bridge methods?**

Erasure can make an overriding method's erased descriptor differ from the generic supertype descriptor. A synthetic bridge with the erased signature delegates to the specific implementation, preserving runtime polymorphism.

## Exercises

1. Implement `copy` first with invariant lists, observe rejected calls, then add correct producer/consumer wildcards.
2. Write and invoke a helper that captures `List<?>` to swap two positions.
3. Explain the bound `<T extends Comparable<? super T>>` using a superclass that implements Comparable.
4. Create heap pollution through a raw list and locate the later compiler-inserted cast.
5. Inspect `TextProvider` reflection output and identify its bridge method.
6. Design a type-safe heterogeneous registry using `Class<T>` keys and isolate any checked cast.

## Chapter summary

Generics provide compile-time consistency over reference types. Parameterizations are invariant, while wildcards express producer and consumer variance. Bounds expose operations and capture helpers name unknown types. Erasure preserves a shared runtime representation, requiring casts and bridge methods and preventing tests of most type arguments. Raw types, unchecked casts, and unsafe generic varargs can pollute the heap; confine unavoidable unchecked boundaries and prove their invariants.

## Revision checklist

- [ ] I can declare generic classes, methods, and multiple bounds.
- [ ] I can explain invariance with the unsafe-insertion argument.
- [ ] I apply extends for producers and super for consumers.
- [ ] I understand unbounded wildcard versus raw type.
- [ ] I can use a helper to capture a wildcard.
- [ ] I know erasure, reifiability, and bridge-method consequences.
- [ ] I can identify every common source of heap pollution.
- [ ] I isolate and justify unavoidable unchecked operations.
