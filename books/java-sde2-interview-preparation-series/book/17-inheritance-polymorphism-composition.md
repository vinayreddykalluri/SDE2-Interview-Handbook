# 17. Inheritance, Polymorphism, and Composition

## Learning objectives

By the end of this chapter, you should be able to:

- distinguish subtype polymorphism from code reuse and object containment;
- predict overriding, hiding, field access, casts, and constructor behavior;
- apply substitutability when designing and reviewing hierarchies;
- choose composition or inheritance based on semantic ownership; and
- evolve polymorphic APIs without fragile downcasts or superclass coupling.

## Why this matters at SDE-2

SDE-2 design discussions often reduce to one question: where should variation live? A hierarchy can make variation explicit and open to new behavior, but a weak hierarchy spreads superclass assumptions throughout the system. Composition localizes collaboration and runtime configuration, but excessive forwarding can obscure the domain.

Interviewers expect more than definitions of "is-a" and "has-a." They look for dispatch reasoning, substitutability, constructor hazards, and pragmatic trade-offs in service and library design.

## First-principles model

Class inheritance creates a subtype relationship and allows a subclass object to contain a superclass portion. A variable of a supertype can hold a reference to a subtype object. When an overridable instance method is invoked, the object's runtime class chooses the implementation. This is subtype polymorphism.

Inheritance is also a reuse mechanism, but reuse alone is not sufficient justification. A subtype promises that code written against its supertype remains correct. It must preserve the supertype's stated preconditions, postconditions, invariants, and behavioral expectations.

Composition means one object holds or receives another object and delegates part of its work. It creates collaboration without claiming substitutability. Dependencies can often be replaced, combined, or decorated at runtime.

> **Specification boundary:** Java specifies single class inheritance, multiple interface inheritance, method overriding, and dynamic dispatch. Object layout, virtual method tables, inline caches, and devirtualization are JVM implementation strategies, not Java language guarantees.

## Core terminology

- **Superclass/subclass:** class being extended and class that extends it.
- **Subtype:** type whose values can be used where a supertype is expected.
- **Upcast:** safe conversion from subtype reference to supertype.
- **Downcast:** checked conversion from supertype reference to a more specific type.
- **Override:** instance method implementation replacing inherited behavior for dispatch.
- **Hide:** static method or field declaration shadows an inherited same-named member.
- **Dynamic dispatch:** runtime selection of an overriding instance method.
- **Substitutability:** subtype preserves contracts expected through the supertype.
- **Composition:** object delegates to contained or injected collaborators.
- **Fragile base class:** superclass changes unexpectedly affect subclasses coupled to internals.

## Detailed mechanics

### Extending classes

Every class except `Object` has one direct superclass. If `extends` is omitted, it extends `Object`. Constructors are not inherited, and subclass construction must invoke an accessible superclass constructor first.

Inherited accessible members become part of the subclass's behavior. Private superclass state remains present but can be manipulated only through accessible superclass operations. A class declared `final` cannot be subclassed. A method declared `final` cannot be overridden.

### Overriding rules

An overriding method has the same subsignature, a compatible covariant reference return, no more restrictive access, and no broader checked exception declaration. `@Override` asks the compiler to verify intent and should be used consistently.

```java
class Message {
    CharSequence render() { return "message"; }
}

class Alert extends Message {
    @Override
    String render() { return "alert"; } // covariant return
}
```

Private methods are not overridden. Static methods are hidden and selected by compile-time type. Fields are hidden rather than polymorphic. Instance method calls dispatch dynamically even when called from a superclass method, except for private, static, constructor, or explicit `super` behavior.

```java
class Base {
    String name = "base field";
    static String kind() { return "base static"; }
    String describe() { return "base virtual"; }
}

class Derived extends Base {
    String name = "derived field";
    static String kind() { return "derived static"; }
    @Override String describe() { return "derived virtual"; }
}

Base value = new Derived();
System.out.println(value.name);       // base field
System.out.println(Base.kind());     // base static
System.out.println(value.describe());// derived virtual
```

Avoid same-named fields and static hiding because they invite mistaken dispatch assumptions.

### `super` and construction

`super(...)` invokes a direct superclass constructor and must be the first constructor invocation statement, unless another same-class constructor is invoked with `this(...)`. `super.method()` invokes the superclass implementation directly for the current object; it is not a call on a separate superclass object.

During base construction, virtual calls can dispatch to the subclass before subclass initializers run. The result may observe null or zero state and violates the expectation that methods see a fully formed object. Constructors should call private or final helpers when they need internal decomposition.

### Casts and `instanceof`

An upcast needs no explicit syntax and never fails for a non-null compatible reference. A downcast asks the runtime whether the object is an instance of the target type; otherwise it throws `ClassCastException`. Casting null succeeds and produces null.

```java
Object candidate = "java";
if (candidate instanceof String text) {
    System.out.println(text.length());
}
```

Pattern variables are in scope where the compiler proves the match succeeded. A design requiring frequent type tests may be missing a polymorphic operation, although boundary adapters and closed algebraic models legitimately inspect variants.

### Substitutability

Suppose a base method accepts any integer and promises a nonnegative result. An override cannot reject negative inputs or return negative results without breaking callers. More generally, a subtype should not strengthen preconditions, weaken postconditions, violate invariants, or change important side effects and timing without the contract allowing it.

The classic rectangle/square modeling problem demonstrates that mathematical subset relationships do not automatically make good mutable object subtypes. If a mutable rectangle allows width and height to change independently, a square cannot preserve both that behavior and its equal-sides invariant.

### Composition and delegation

Composition represents roles explicitly:

```java
interface DiscountPolicy {
    long discountCents(long subtotalCents);
}

final class Checkout {
    private final DiscountPolicy policy;

    Checkout(DiscountPolicy policy) {
        this.policy = java.util.Objects.requireNonNull(policy);
    }

    long totalCents(long subtotalCents) {
        long discount = policy.discountCents(subtotalCents);
        if (discount < 0 || discount > subtotalCents) {
            throw new IllegalStateException("invalid discount");
        }
        return subtotalCents - discount;
    }
}
```

`Checkout` does not claim to be a discount policy. It owns orchestration and validates the collaborator's result. Different policies can be injected without subclasses inheriting checkout internals.

## Worked Java example

This example combines subtype polymorphism at a narrow interface with composition in the service.

```java
import java.util.List;
import java.util.Objects;

public class NotificationDemo {
    interface Channel {
        Delivery send(String recipient, String body);
    }

    record Delivery(boolean accepted, String providerId) {}

    static final class EmailChannel implements Channel {
        @Override
        public Delivery send(String recipient, String body) {
            return new Delivery(true, "email-42");
        }
    }

    static final class Notifier {
        private final Channel channel;

        Notifier(Channel channel) {
            this.channel = Objects.requireNonNull(channel);
        }

        Delivery notify(String recipient, List<String> lines) {
            String body = String.join(System.lineSeparator(), lines);
            if (body.isBlank()) {
                throw new IllegalArgumentException("empty body");
            }
            return channel.send(recipient, body);
        }
    }

    public static void main(String[] args) {
        Channel channel = new EmailChannel();
        Notifier notifier = new Notifier(channel);
        System.out.println(notifier.notify("a@example.test", List.of("Hello")));
    }
}
```

`Notifier` depends on the role it needs, not the concrete provider. Its validation and message construction stay independent from channel implementations.

## Execution or memory walkthrough

`new EmailChannel()` creates an object referenced through the `Channel` interface. This upcast loses no object information; it narrows which operations are visible at compile time. `Notifier` stores a copied reference to the same channel object.

During `notify`, `String.join` builds the body. The service validates it, then invokes the interface signature `Channel.send`. Runtime dispatch selects `EmailChannel.send`. That method creates and returns a `Delivery` record. No downcast is required anywhere.

If another implementation is injected, the service bytecode still targets the interface operation. Only runtime dispatch changes. The interface contract must therefore specify acceptance meaning, null behavior, failures, and whether retries are safe.

## Complexity and performance

Inheritance and composition do not determine algorithmic complexity. Dynamic dispatch and delegation each add conceptually constant overhead. In the example, joining lines takes O(n) time in total character content and O(n) output space; channel work dominates the remaining cost.

Deep inheritance raises cognitive cost even when runtime cost is small. Each override may require understanding implicit superclass state and hooks. Composition adds objects and calls but makes dependency graphs and test seams explicit.

> **HotSpot note:** HotSpot often inlines interface and virtual calls when profiling shows one or a few receiver classes. It can deoptimize if assumptions change. Do not mark classes final solely for speculative call speed without measurement.

## Edge cases and common mistakes

- Overloading is not overriding; different parameter types create a new overload.
- Fields and static methods do not dispatch on runtime type.
- A downcast changes the reference's static view, not the object, and may fail.
- `instanceof` returns false for null.
- An override cannot reduce accessibility or broaden checked exceptions.
- Calling overridable methods during construction can reach uninitialized subclass state.
- Extending a concrete collection merely to reuse methods often exposes a larger, misleading contract.
- Inheriting from a class with overridable internal hooks creates fragile coupling to call order.
- A base class with `equals` rules that subclasses cannot preserve can break symmetry or transitivity.
- Composition still needs ownership decisions: whether collaborators are thread-safe, mutable, shared, or lifecycle-managed.
- A decorator must preserve the wrapped contract, including exceptions and identity expectations, not only method names.

## Production engineering notes

Use inheritance for a genuine, stable subtype relationship with a documented extension contract. Prefer narrow interfaces for roles and composition for configurable behavior, infrastructure clients, and policies. If subclasses are expected, document which methods are hooks, when they run, and what state is valid.

Keep base classes small and protect invariants with private state and final template methods. Avoid exposing protected mutable fields. A protected operation can offer controlled extension without handing subclasses the representation.

Downcasts at system boundaries should fail with useful diagnostics. Within core domain logic, repeated `instanceof` chains often indicate behavior living in the wrong place. For a fixed set of variants, sealed types and pattern matching may make inspection intentional and exhaustive.

## Interview questions and model answers

**What is runtime polymorphism in Java?**

A call to an overridable instance method is dispatched according to the receiver object's runtime class. The compiler has already selected the method signature using static types; runtime chooses the most specific override of that signature.

**Why prefer composition over inheritance?**

Composition avoids claiming substitutability, limits coupling to a collaborator's public contract, allows runtime replacement, and does not expose superclass hooks or state. Inheritance remains appropriate when values truly are subtypes and the base is designed for extension.

**What is the difference between overriding and hiding?**

Instance methods override and dispatch dynamically. Static methods and fields can be hidden; access is determined from the compile-time qualifying type. Same-named fields are separate state.

**What makes a subtype substitutable?**

It honors the supertype's behavioral contract: it accepts at least the promised inputs, returns results meeting the promised postconditions, maintains invariants, and preserves documented failure and side-effect expectations.

**Why are constructor calls to overridable methods unsafe?**

Dynamic dispatch reaches the subclass implementation before the subclass's field initializers and constructor body have run. The method can observe default state or leak the incomplete object.

## Exercises

1. Predict field, static method, and instance method outputs through base and derived references.
2. Design a subclass that violates a base precondition contract, then refactor it into composition.
3. Replace a three-branch `instanceof` chain with a polymorphic operation.
4. Implement a timing decorator for `Channel` that preserves exceptions and return values.
5. Analyze whether `Stack extends ArrayList` would be substitutable and identify inherited operations that violate stack intent.
6. Write a base constructor that accidentally dispatches to subclass state, demonstrate the failure, and repair it with a private helper.

## Chapter summary

Inheritance creates a subtype promise, not merely a reuse opportunity. Compile-time member selection and runtime overriding must be considered separately: fields and static methods hide, while overridable instance methods dispatch. Subtypes must preserve contracts. Composition expresses collaboration without exposing superclass internals and is usually safer for configurable behavior. Choose the relationship that tells the domain truth and keeps invariants local.

## Revision checklist

- [ ] I distinguish subtype polymorphism from implementation reuse.
- [ ] I can trace overriding, field hiding, and static hiding.
- [ ] I understand upcasts, downcasts, and pattern `instanceof`.
- [ ] I can state substitutability in terms of behavioral contracts.
- [ ] I avoid virtual calls from constructors.
- [ ] I recognize fragile base-class coupling.
- [ ] I choose composition for roles and runtime-configurable policies.
- [ ] I document extension points and collaborator ownership explicitly.
