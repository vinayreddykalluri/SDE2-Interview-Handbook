# 16. Classes, Objects, Access Control, and Packages

## Learning objectives

By the end of this chapter, you should be able to:

- separate class definitions, objects, references, identity, and state;
- predict field, initializer, and constructor execution order;
- apply `public`, `protected`, package, and `private` access precisely;
- use static and instance members without confusing ownership; and
- design cohesive classes and package boundaries that preserve invariants.

## Why this matters at SDE-2

Backend design lives in classes and package boundaries. Weak encapsulation lets invalid states spread, couples services to implementation details, and makes concurrent or evolutionary changes dangerous. Strong modeling converts business rules into construction and method contracts.

Java access control also contains interview-worthy subtleties, especially `protected` across packages and the difference between class visibility and member visibility. Experienced engineers should reason about these rules rather than repeatedly widening access until code compiles.

## First-principles model

A class declares a reference type and defines members: fields, methods, constructors, initializers, and nested types. An object is a runtime class instance with identity and field state. A reference value can designate that object or be `null`. Multiple references may designate one object.

Instance members belong conceptually to each object; static members belong to the class's shared namespace and initialization lifecycle. Encapsulation means an object controls how its valid state is created and changed. Access modifiers are compiler- and runtime-enforced visibility rules, not a complete security boundary.

A package groups types and contributes to access control and naming. `com.example.api` and `com.example.api.internal` are distinct packages; package names do not create inheritance of access.

> **Specification boundary:** The Java language specifies initialization order and access checks. It does not promise a field's physical offset, an object's header format, or one object allocation per `new` expression after optimization, provided observable behavior is preserved.

## Core terminology

- **Class:** declaration of a reference type and its members.
- **Object:** runtime instance of a class.
- **Identity:** distinction between object instances even when their content is equal.
- **State:** values reachable through an object's instance fields.
- **Invariant:** condition that valid instances maintain.
- **Constructor:** special declaration that initializes a newly allocated instance.
- **Initializer:** static or instance block executed during class or object initialization.
- **Receiver:** object denoted by `this` for an instance method call.
- **Package-private:** access when no modifier is written.
- **Qualified name:** package plus type name, such as `java.time.Instant`.

## Detailed mechanics

### Fields, methods, and receivers

```java
package example.account;

public final class Counter {
    private static int instances;
    private int value;

    public Counter(int initialValue) {
        this.value = initialValue;
        instances++;
    }

    public int increment() {
        return ++value;
    }

    public static int instancesCreated() {
        return instances;
    }
}
```

Each `Counter` has a separate `value`, while all calls observe one static `instances` field per defining class loader. `this` is the current receiver and is unavailable in a static context. A static method should be called through the class name; calling it through an expression is legal in some forms but misleading because dispatch is not based on that object's runtime class.

The example's shared count is not thread-safe. `++` is a read-modify-write sequence, and `private` says nothing about concurrency.

### Construction and initialization order

Object creation conceptually allocates storage, initializes all instance fields to defaults, invokes a selected constructor, and returns a reference if construction completes. Before a constructor body runs, it invokes another constructor in the same class with `this(...)` or a superclass constructor with `super(...)`; if neither is explicit, the compiler inserts an accessible no-argument `super()`.

For each class in the hierarchy, instance field initializers and instance initializer blocks run in textual order after the superclass portion and before that class's constructor body.

```java
class Base {
    Base() { System.out.println("Base body"); }
}

class Report extends Base {
    private final String title = initializeTitle();
    { System.out.println("Report initializer"); }

    Report() {
        System.out.println("Report body");
    }

    private String initializeTitle() {
        System.out.println("title initializer");
        return "daily";
    }
}
```

Creating `Report` prints Base body, title initializer, Report initializer, then Report body. Calling an overridable instance method from a constructor is dangerous: dynamic dispatch can reach subclass code before subclass fields have been initialized.

Constructors have no return type and are not inherited. If a class declares no constructor, the compiler supplies a default constructor whose accessibility matches the class. Once any constructor is declared, no default is generated.

### Static initialization

Static fields first receive defaults, then static field initializers and static initializer blocks execute in textual order when the class is initialized. Initialization occurs at most once per class or interface per defining class loader, synchronized by the JVM. A failure can leave the class erroneous for subsequent use.

Compile-time constant static fields may be inlined into clients and accessed without triggering initialization of the declaring class. This matters for side effects and binary evolution.

### Access control

At top level, a class may be `public` or package-private. A public top-level class is conventionally, and under ordinary file-based compilation, stored in a same-named source file.

Member access proceeds from narrowest to broadest:

- `private`: within the top-level nest that declares it, including its nested nestmates under language access rules;
- package-private: within the same package;
- `protected`: same-package access plus subclass access under an additional cross-package receiver rule;
- `public`: wherever the declaring type and module/package exposure permit.

The cross-package `protected` rule is commonly misunderstood. Subclass code may access the inherited protected member through `this`, `super`, or a reference whose qualifying type is the subclass or its subclass, not through an arbitrary base-class instance.

```java
// package library;
public class Base { protected int code; }

// package app;
class Derived extends library.Base {
    void ok(Derived other) { other.code = 1; }
    // void bad(library.Base other) { other.code = 1; }
}
```

`private` members are not inherited as directly accessible members, although an object still contains superclass state and superclass methods can use it. Modern Java nest-based access allows nested classes in the same nest to access one another's private members according to language rules without treating private as public.

### Packages and imports

A package declaration must precede imports and type declarations, aside from comments and whitespace. Imports shorten names at compile time; they do not load classes or add runtime dependencies by themselves. `java.lang` is implicitly imported, as are types in the current package. Wildcard imports import types from one package, not subpackages, and do not harm runtime performance.

Static imports can shorten constants or utility calls but may obscure origin. Avoid the unnamed package for production code because named-package code cannot import from it and tooling conventions assume named packages.

Java Platform Module System exports can further restrict whether a public package is accessible to another module. Reflection may also require an `opens` directive; public at the language level is not necessarily globally reflectable in a modular runtime.

## Worked Java example

This class establishes a nonnegative balance invariant and returns new immutable values instead of exposing mutation.

```java
package example.money;

import java.util.Objects;

public final class Wallet {
    private final String ownerId;
    private final long cents;

    private Wallet(String ownerId, long cents) {
        this.ownerId = Objects.requireNonNull(ownerId, "ownerId");
        if (ownerId.isBlank()) {
            throw new IllegalArgumentException("blank ownerId");
        }
        if (cents < 0) {
            throw new IllegalArgumentException("negative balance");
        }
        this.cents = cents;
    }

    public static Wallet empty(String ownerId) {
        return new Wallet(ownerId, 0);
    }

    public Wallet deposit(long amountCents) {
        if (amountCents <= 0) {
            throw new IllegalArgumentException("deposit must be positive");
        }
        return new Wallet(ownerId, Math.addExact(cents, amountCents));
    }

    public long balanceCents() {
        return cents;
    }
}
```

The constructor is private so all construction flows remain under the class's control. A factory names the initial state. Final fields plus immutable field values make each instance safe to share after proper construction.

## Execution or memory walkthrough

`Wallet.empty("u-7")` invokes a static method; no receiver object is required. `new Wallet` allocates a new instance whose reference fields initially hold null and whose `long` field holds zero. The constructor validates the copied `ownerId` reference, then assigns both final fields. It returns normally and the factory returns the reference.

Calling `wallet.deposit(500)` supplies the original wallet as receiver and copies `500L` into the parameter. The existing object is not modified. `addExact` computes the new balance and construction validates a new wallet. The caller may retain both versions without aliasing mutable state.

If validation throws, no reference to the incompletely constructed object is returned by the expression. Publishing `this` from a constructor, however, could allow another component to observe incomplete state and must be avoided.

## Complexity and performance

Field access and ordinary method dispatch are O(1), excluding method work. Object construction is generally proportional to initialization and reachable defensive copies, not merely the number of fields. The wallet operations are O(1) time and allocate one new object per successful state change.

Immutability can increase allocation but enables safe sharing, caching, and simpler concurrency. A JIT may eliminate non-escaping allocations. Do not replace clear value semantics with mutable pooling unless profiling demonstrates a meaningful bottleneck and ownership remains tractable.

> **HotSpot note:** HotSpot object headers, compressed references, field layout, escape analysis, and allocation in thread-local buffers are implementation choices. Use measurement tools when physical footprint matters; do not derive it only from field widths.

## Edge cases and common mistakes

- A reference declaration does not create an object.
- `final` on a reference prevents reassignment, not mutation of the object.
- The generated default constructor disappears after any explicit constructor is declared.
- A superclass must be initialized before subclass field initializers run.
- Overridable calls from constructors can observe default-valued subclass fields.
- Leaking `this` during construction can violate invariants and safe publication.
- `private` does not imply thread-safe, immutable, or inaccessible through all reflection configurations.
- Package hierarchy is naming convention, not access inheritance.
- `protected` is not simply package-or-world-visible-to-subclasses; the cross-package qualifying reference matters.
- A public member is unusable if its declaring type is inaccessible.
- Static mutable state is global per class loader, complicates tests, and needs concurrency control.
- Static initialization cycles can expose default values and produce fragile startup failures.

## Production engineering notes

Organize packages around stable domain capabilities, not arbitrary technical buckets alone. Expose a small public surface and keep implementation types package-private. If tests need broad access, prefer testing through behavior or narrowly scoped test support over making internals public.

Construct valid objects atomically. Validate required state, use factories or builders when construction has meaningful variants, and avoid setters that create transiently invalid combinations. Keep dependency injection and serialization frameworks from bypassing invariants without explicit adapters.

Treat static state as process-wide infrastructure. Make it immutable where possible; otherwise define lifecycle, synchronization, reset behavior, and class-loader implications. Static constants that may change across versions should not be compile-time constants if clients must observe updates without recompilation.

## Interview questions and model answers

**What is the difference between a class, an object, and a reference?**

A class declares a type and behavior. An object is a runtime instance with identity and state. A reference is a value that designates an object or is null; several references can designate one object.

**What is the object initialization order?**

Fields receive defaults first. Constructor chaining initializes the superclass portion. For the current class, instance field initializers and instance initializer blocks run in textual order, then its constructor body runs. This repeats down the hierarchy.

**How does protected access work across packages?**

A subclass may access the protected member as part of its inherited view, but through a qualifying expression whose type is that subclass or a subtype, not an arbitrary instance of the superclass. Same-package code has ordinary package access to it.

**Is a private constructor only for singletons?**

No. It can enforce factories, canonical instances, validated creation, utility classes, or controlled subclassing. A singleton also needs lifecycle, concurrency, serialization, and testability decisions.

**Does importing a type load it?**

No. An import is a compile-time name-resolution convenience. Loading and initialization follow runtime use rules, not the presence of an import.

## Exercises

1. Predict the print order for a three-level hierarchy with static fields, instance fields, initializer blocks, and constructor chaining.
2. Build a class that cannot represent an invalid date range and has no setters.
3. Create two packages demonstrating legal and illegal protected access through differently typed references.
4. Refactor a public implementation class into a package-private type behind a public interface.
5. Identify all ways `this` could escape from a constructor through callbacks, collections, or threads.
6. Make the `Counter` instance count thread-safe, then discuss whether the global metric belongs in the domain class.

## Chapter summary

Classes define types; objects carry identity and state; references designate objects. Construction initializes superclass state before subclass initializers and bodies. Access control combines member modifiers, declaring-type accessibility, packages, subclass rules, and possibly modules. Good class design centralizes invariants, limits public surface, avoids construction leaks, and treats static mutable state as an explicit architectural concern.

## Revision checklist

- [ ] I distinguish a class, object, reference, and receiver.
- [ ] I can trace static and instance initialization order.
- [ ] I know when the compiler supplies a default constructor.
- [ ] I apply all four member access levels precisely.
- [ ] I can explain the cross-package protected receiver rule.
- [ ] I know that packages and subpackages have no special access relationship.
- [ ] I avoid overridable calls and `this` escape during construction.
- [ ] I design classes whose constructors and methods preserve invariants.
