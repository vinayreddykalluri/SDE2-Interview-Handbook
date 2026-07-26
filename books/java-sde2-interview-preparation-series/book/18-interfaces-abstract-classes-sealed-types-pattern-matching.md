# 18. Interfaces, Abstract Classes, Sealed Types, and Pattern Matching

## Learning objectives

By the end of this chapter, you should be able to:

- choose among interfaces, abstract classes, sealed hierarchies, and composition;
- resolve default-method inheritance and understand interface member rules;
- design and enforce a closed subtype family with `sealed`, `non-sealed`, and `final`;
- use Java 21 type patterns, record patterns, and pattern switch exhaustively; and
- identify which pattern features were preview versus permanent in Java 17 and 21.

## Why this matters at SDE-2

Backend systems repeatedly model variants: payment results, commands, events, failures, and protocol messages. An open interface supports third-party implementations; a sealed algebra supports a known set of cases. Choosing incorrectly either blocks extension or makes exhaustive reasoning impossible.

Java 21 pattern matching makes closed models concise, but concision is not the primary benefit. The real gain is aligning domain completeness with compiler checks. SDE-2 interviews increasingly test this modern type-system reasoning while still expecting knowledge of default methods and abstract base classes.

## First-principles model

An interface specifies a reference type whose instances are objects of implementing classes. It supports multiple inheritance of type and behavior but not per-instance interface state. An abstract class is a partially implemented class with constructors and instance fields, and a subclass can extend only one class.

A sealed type restricts which classes or interfaces may directly extend or implement it. Each permitted direct subtype must explicitly continue the policy: `final` closes it, `sealed` declares another restricted level, or `non-sealed` reopens it.

A pattern combines a test with conditional variable extraction. Pattern matching does not bypass the type system; it moves a cast and its proof into one construct. Exhaustive pattern switch is especially valuable when the selector is a closed hierarchy.

> **Specification boundary:** Exhaustiveness, dominance, permitted subclasses, and pattern-variable scope are compile-time language rules. The JVM also records permitted subclasses in class-file metadata and rejects unauthorized direct extension; it does not guarantee that a sealed hierarchy has only a fixed number of runtime object instances.

## Core terminology

- **Interface:** contract type supporting multiple implementation inheritance.
- **Default method:** interface instance method with an implementation.
- **Abstract class:** non-instantiable class that may mix abstract behavior, state, and implementation.
- **Sealed type:** type with an enumerated set of permitted direct subtypes.
- **Non-sealed type:** permitted subtype that reopens unrestricted inheritance.
- **Pattern:** test that conditionally binds variables from a value.
- **Type pattern:** tests a runtime type and binds the narrowed value.
- **Record pattern:** deconstructs record components recursively.
- **Dominance:** an earlier case makes a later case unreachable.
- **Exhaustiveness:** cases cover every possible selector value allowed by the type rules.

## Detailed mechanics

### Interface members and evolution

Interface fields are implicitly `public static final` and must have initializers. Interface methods can be:

- implicitly `public abstract` when declared without a body;
- `public default` instance methods with bodies;
- `public static` methods; or
- `private` instance or static helpers, available since Java 9.

Implementations cannot reduce the accessibility of an interface's public method. Static interface methods are called through the interface and are not inherited as instance behavior. An interface has no constructor and cannot hold per-object instance fields.

Default methods allow compatible interface evolution, but conflicts follow rules:

1. a concrete class method wins over an interface default;
2. a more specific subinterface default wins over a less specific one; and
3. unrelated inherited defaults with the same signature require an explicit override.

```java
interface JsonForm { default String format() { return "json"; } }
interface TextForm { default String format() { return "text"; } }

final class Output implements JsonForm, TextForm {
    @Override
    public String format() {
        return JsonForm.super.format();
    }
}
```

`InterfaceName.super.method()` resolves a direct superinterface default. An abstract declaration in a more specific interface can also remove an inherited default and force implementers to provide behavior.

### Abstract classes

An abstract class may declare abstract methods and concrete members. It can have constructors even though it cannot be instantiated directly; subclass constructors use them to initialize the base portion. A class containing an abstract method must be abstract.

Use an abstract class when implementations share protected invariants, instance state, or a controlled template algorithm. Keep extension contracts explicit and avoid protected mutable fields. Use an interface when the important concept is a role that unrelated classes can implement or when multiple roles must compose.

```java
abstract class BatchJob {
    public final void run() {
        validate();
        execute();
    }

    protected void validate() {}
    protected abstract void execute();
}
```

The final template fixes sequencing while hooks provide limited variation. It still inherits constructor and fragile-base-class risks, so document whether hooks may throw, block, or mutate shared state.

### Sealed hierarchies

Sealed classes and interfaces became permanent in Java 17. A declaration names permitted direct subtypes with `permits`, unless the compiler can infer them from declarations in the same source file.

```java
sealed interface Command permits Create, Cancel {}
record Create(String id) implements Command {}
record Cancel(String id) implements Command {}
```

Records are implicitly final, so they satisfy the continuation requirement. A permitted ordinary class must declare `final`, `sealed`, or `non-sealed`.

Permitted direct subtypes must be accessible and compiled in the same named module as the sealed type. In an unnamed module, they must be in the same package. Sealing controls direct inheritance, not visibility: a permitted subtype can itself expose behavior according to its modifiers.

Sealed types are ideal for closed domain alternatives maintained together. They are a poor public extension point when independent consumers must add implementations. `non-sealed` deliberately creates an open branch, which can reduce switch exhaustiveness over deeper runtime variants even though the direct branch is known.

### Type patterns

Pattern matching for `instanceof` became permanent in Java 16 and is therefore permanent in both Java 17 and 21.

```java
static int length(Object value) {
    if (value instanceof String text && !text.isEmpty()) {
        return text.length();
    }
    return 0;
}
```

The pattern variable is in scope only where flow analysis proves the match. With `&&`, it is available on the right. With a negated test followed by an abrupt exit, it can be available afterward:

```java
if (!(value instanceof String text)) {
    throw new IllegalArgumentException();
}
System.out.println(text.length());
```

Patterns do not match null. A type pattern that would be unconditionally true from the static type is generally rejected as pointless, while casts have a different compatibility history.

### Pattern switch in Java 21

Pattern matching for `switch` was preview in Java 17 under JEP 406 and required `--enable-preview`. It evolved across releases and became permanent in Java 21 under JEP 441. Java 17 preview syntax and semantics should not be treated as a stable Java 21 source contract.

```java
static String describe(Object value) {
    return switch (value) {
        case null -> "missing";
        case String text when text.isBlank() -> "blank text";
        case String text -> "text of length " + text.length();
        case Integer number -> "integer " + number;
        default -> "other";
    };
}
```

Java 21 uses `when` guards. Cases are tried in source order subject to compiler dominance rules. `case Object ignored` dominates subtype cases after it and must therefore appear last. A guarded pattern generally does not dominate the same unguarded pattern unless its guard is a constant true expression.

Pattern switch supports explicit `case null`; without a matching null label, switching on null throws `NullPointerException`. Type and record patterns do not match null, so null handling must be explicit when it is part of the domain.

An exhaustive switch expression over a sealed selector can omit `default` when all permitted alternatives are covered. Omitting a broad default is often desirable: when the hierarchy changes and code is recompiled, the compiler points to every incomplete switch. The compiler still emits a defensive failure path for incompatible binary evolution.

### Record patterns in Java 21

Record patterns became permanent in Java 21 under JEP 440. They deconstruct records and can nest:

```java
record Point(int x, int y) {}
record Segment(Point start, Point end) {}

static int horizontalLength(Object value) {
    return switch (value) {
        case Segment(Point(int x1, int y1), Point(int x2, int y2))
                when y1 == y2 -> Math.abs(x2 - x1);
        default -> 0;
    };
}
```

Record patterns were not part of Java 17. Java 21 also permits `var` in record component patterns where inference is useful. A record pattern checks and extracts component values; accessor invocation and nested matching can still fail only according to ordinary execution and pattern rules, so keep record accessors pure.

## Worked Java example

This closed result type turns success, rejection, and transient failure into explicit cases.

```java
public class PaymentResultDemo {
    sealed interface PaymentResult
            permits Approved, Rejected, RetryLater {}

    record Approved(String authorizationId) implements PaymentResult {}
    record Rejected(String reason) implements PaymentResult {}
    record RetryLater(long delayMillis) implements PaymentResult {}

    static String clientMessage(PaymentResult result) {
        return switch (result) {
            case Approved(String id) -> "approved: " + id;
            case Rejected(String reason) -> "rejected: " + reason;
            case RetryLater(long delay) when delay <= 1_000 -> "retry soon";
            case RetryLater(long delay) -> "retry in " + delay + " ms";
        };
    }

    public static void main(String[] args) {
        System.out.println(clientMessage(new Approved("auth-7")));
        System.out.println(clientMessage(new RetryLater(500)));
    }
}
```

This source requires Java 21 because it uses record patterns and a guarded pattern switch. It needs no preview flag in Java 21.

## Execution or memory walkthrough

`new RetryLater(500)` creates a final record instance referenced as `PaymentResult`. `clientMessage` first checks alternatives according to the selector's runtime class. The two unrelated record types do not match. The `RetryLater(long delay)` pattern matches, extracts the primitive component into `delay`, and evaluates the guard.

Because 500 is at most 1000, the guarded arm produces `"retry soon"`; the unguarded RetryLater arm is not evaluated. Exhaustiveness follows from all three direct permitted record implementations, with the two RetryLater arms collectively covering that variant.

If a fourth permitted subtype is added and this code is recompiled, the compiler rejects the incomplete switch. If separately compiled binaries become inconsistent, a generated defensive path prevents silently treating the new value as an old case.

## Complexity and performance

Interface calls, virtual calls, type tests, component extraction, and ordinary switch selection are conceptually O(1), excluding method bodies. Nested patterns add work proportional to the number of tested components and guards. Sealed metadata can help tools and optimizers reason about possible subtypes, but its primary benefit is semantic completeness.

An abstract template can avoid repeated orchestration code; an interface-plus-composition design may add delegation. These constant costs are typically irrelevant beside I/O. Optimize the model for comprehensibility and profile before specializing dispatch.

> **HotSpot note:** HotSpot may devirtualize interface calls and optimize type tests using class profiling. Sealing can provide additional hierarchy facts, but no source-level latency or allocation guarantee follows from declaring a type sealed.

## Edge cases and common mistakes

- Interface fields are static constants, not per-implementation state.
- A class method wins over an interface default, even if inherited from a superclass.
- Unrelated same-signature defaults require an explicit resolution.
- Abstract classes may have constructors; interfaces do not.
- A sealed type's permitted direct subclasses must satisfy module or package placement rules.
- Every permitted direct subtype must be final, sealed, or non-sealed.
- `non-sealed` is a deliberate reopening, not the absence of a decision.
- Patterns do not behave like unchecked destructuring; type compatibility and dominance are enforced.
- A broad case before a narrow case can make the latter dominated and illegal.
- Null handling in pattern switch must be intentional.
- Java 17 pattern switch was preview and changed before finalization; compile Java 21 examples as Java 21 source.
- Record patterns are final in Java 21 but unavailable in Java 17.
- An exhaustive switch without default is often better for closed models, but binary evolution still needs operational compatibility planning.

## Production engineering notes

Use open interfaces for provider ecosystems and test seams. Use sealed types for centrally owned outcomes, commands, or syntax trees whose variants must be exhaustively understood. Avoid sealing across teams when deployments and extension ownership are independent.

Keep default methods small and contract-preserving. Adding a default method can still conflict with another interface a consumer already implements. Library evolution requires downstream compatibility testing, not only successful compilation of the library.

Pattern switches should translate or interpret variants at clear architectural boundaries. If every consumer repeats a large switch, behavior may belong on the variants or in a visitor-like service. Conversely, putting every external concern into domain variants can create coupling; exhaustive external interpreters are often appropriate.

## Interview questions and model answers

**When should you use an interface instead of an abstract class?**

Use an interface for a role that unrelated classes can implement, multiple inheritance of type, or an open provider contract. Use an abstract class when closely related implementations share state, construction rules, and a controlled implementation skeleton. Composition can combine either.

**How are conflicting default methods resolved?**

A class implementation wins over interface defaults. A more specific subinterface wins over its ancestor. Unrelated defaults require the implementing class to override and optionally delegate with `InterfaceName.super.method()`.

**What does sealing guarantee?**

Only listed permitted types may directly extend or implement the sealed type, subject to module or package rules. Each permitted subtype states whether its own branch closes, stays sealed, or reopens. It does not imply immutability or a fixed object count.

**Which pattern features are final in Java 17?**

Type patterns for `instanceof` are final, and sealed classes are final features in Java 17. Pattern matching for switch is preview in Java 17. Record patterns are not available there.

**Which features became final in Java 21?**

Pattern matching for switch and record patterns are permanent in Java 21. Java 21 guarded case labels use `when`. Neither feature requires `--enable-preview` in Java 21.

## Exercises

1. Resolve a diamond of two unrelated default methods while calling both implementations deliberately.
2. Model a sealed file-operation result and write an exhaustive Java 21 switch without default.
3. Add a non-sealed branch and explain what remains known to the compiler.
4. Convert nested casts for two records into a nested record pattern.
5. Write a pattern switch whose cases are initially dominated, then reorder or guard them correctly.
6. Decide whether a plugin API should be sealed; document extension ownership, module boundaries, and compatibility consequences.

## Chapter summary

Interfaces model roles and support multiple inheritance of contract and default behavior. Abstract classes provide single-inheritance state and controlled templates. Sealed types make a directly permitted family explicit, while each branch chooses whether to close or reopen. Java 21's final pattern switch and record patterns let code test, extract, guard, and exhaustively handle those models. Java 17 supports final sealed types and `instanceof` patterns, but its pattern switch is preview.

## Revision checklist

- [ ] I can choose between an interface, abstract class, sealed type, and composition.
- [ ] I know interface field, static, default, and private-method rules.
- [ ] I can resolve default-method conflicts.
- [ ] I understand permits placement and final/sealed/non-sealed continuation.
- [ ] I use flow-scoped `instanceof` pattern variables safely.
- [ ] I can order pattern switch cases without dominance errors.
- [ ] I handle null and exhaustiveness deliberately.
- [ ] I can label Java 17 preview and Java 21 permanent pattern features accurately.
