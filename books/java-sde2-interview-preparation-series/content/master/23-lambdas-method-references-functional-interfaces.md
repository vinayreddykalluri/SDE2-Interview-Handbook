# 23. Lambdas, Method References, and Functional Interfaces

## Learning objectives

By the end of this chapter, you should be able to:

- define and recognize functional interfaces, including generic primitive variants;
- explain lambda target typing, capture, scope, and exception compatibility;
- distinguish the four principal method-reference forms;
- compose behavior while controlling side effects and concurrency assumptions; and
- design functional APIs that remain readable, testable, and allocation-aware.

## Why this matters at SDE-2

Modern Java APIs use functions for streams, asynchronous stages, retries, transaction callbacks, and configuration. A lambda can make variation concise, but it can also hide blocking work, capture mutable state, or make exception handling opaque.

SDE-2 interviews test syntax less often than semantics: whether a lambda is an object, which overload it targets, how `this` behaves, and why a captured counter will not compile. Production reviews add ownership, threading, observability, and idempotency.

## First-principles model

A lambda expression is source syntax for supplying an implementation of a functional interface's single abstract method. It has no standalone type. Its target type comes from assignment, invocation, cast, or return context, and that context determines parameter and return compatibility.

A method reference is another compact implementation form that delegates the functional method to an existing static method, bound receiver, unbound receiver, or constructor. Neither syntax promises a particular generated class, allocation, or object identity.

Captured local values must be final or effectively final. The lambda captures their values, not mutable stack slots. A captured reference may still reach mutable state, so type legality does not imply thread safety.

> **Specification boundary:** Java specifies lambda behavior through target functional interfaces, including capture and `this` semantics. The runtime representation, caching, generated class shape, and use of `invokedynamic` linkage are implementation details and must not be observed through identity assumptions.

## Core terminology

- **Functional interface:** interface with one abstract method after inheritance and override-equivalence rules.
- **SAM:** single abstract method and its function type.
- **Target typing:** surrounding context determines a lambda or method-reference type.
- **Capture:** lambda retains values from its lexical scope.
- **Effectively final:** local assigned once even without `final` modifier.
- **Bound method reference:** receiver object is captured, such as `printer::print`.
- **Unbound method reference:** receiver becomes the first function parameter, such as `String::length`.
- **Constructor reference:** creates through `Type::new` or an array constructor.
- **Higher-order function:** accepts or returns behavior.
- **Non-interference:** operation does not modify the source or shared state in a way that invalidates processing.

## Detailed mechanics

### Functional interface rules

`@FunctionalInterface` makes the compiler validate intent. The annotation is optional but useful. Default, static, and private methods do not count as abstract methods. Public methods corresponding to `Object` methods do not prevent functional status in the relevant interface rules.

```java
@FunctionalInterface
interface CheckedSupplier<T> {
    T get() throws Exception;

    default T getOrElse(T fallback) {
        try {
            return get();
        } catch (Exception ex) {
            return fallback;
        }
    }
}
```

Generic interface methods can prevent lambda targeting in cases where no single concrete function type can be inferred. Keep custom interfaces focused and reuse `java.util.function` when its naming and exception contract fit.

Common interfaces include:

- `Predicate<T>`: T to boolean;
- `Function<T,R>`: T to R;
- `UnaryOperator<T>`: T to T;
- `BinaryOperator<T>`: two Ts to T;
- `Consumer<T>`: T to void;
- `Supplier<T>`: no arguments to T; and
- primitive specializations such as `IntPredicate`, `ToLongFunction<T>`, and `LongSupplier` to reduce boxing.

### Lambda syntax and target typing

```java
java.util.function.Predicate<String> nonBlank = s -> !s.isBlank();
java.util.function.BinaryOperator<Integer> add = (left, right) -> left + right;
java.util.function.Supplier<String> empty = () -> "";
```

One inferred parameter can omit parentheses. Multiple or zero parameters require them. A block body uses statements and must return on every normal path when the functional result is non-void. Parameter types must be all inferred, all `var`, or all explicit; annotations can be placed on `var` parameters.

The same lambda text can target different interfaces. Overloads taking unrelated functional interfaces with identical-looking shapes can therefore be ambiguous:

```java
static void use(java.util.function.Function<String, Integer> f) {}
static void use(java.util.function.ToIntFunction<String> f) {}
// use(s -> s.length()); // ambiguous
use((java.util.function.ToIntFunction<String>) String::length);
```

Avoid public overload sets distinguished only by similar function types. Named methods or differently named operations are easier for callers.

### Scope, capture, and `this`

```java
int limit = 10;
java.util.function.IntPredicate small = value -> value < limit;
// limit = 20; // would make limit not effectively final
```

The value 10 is captured. Instance lambdas can access fields, whose values may change because the lambda captures `this`, not a snapshot of each field. Local variables cannot be redeclared with the same name inside a lambda parameter or body when lexical scope forbids shadowing.

Lambda `this` and `super` are lexically scoped: they refer to the enclosing instance. In an anonymous class, `this` refers to the anonymous object. This distinction matters for listeners and recursion.

Capture can extend object lifetimes. Storing a lambda that references a large service or request object retains that graph. A stateless lambda may be reused by an implementation, but code must not depend on reuse or synchronize on a lambda object.

### Method references

Four common forms are:

```java
java.util.function.IntBinaryOperator max = Math::max;       // static
var prefix = "id:";
java.util.function.Function<String, String> add = prefix::concat; // bound
java.util.function.ToIntFunction<String> length = String::length;  // unbound
java.util.function.Supplier<java.util.ArrayList<String>> list =
        java.util.ArrayList::new;                            // constructor
```

For `String::length`, the function argument is the receiver. An unbound reference can also have additional parameters, as `String::indexOf` matching `(String receiver, String search) -> int`, subject to overload resolution.

A bound receiver expression is evaluated when the method reference is created, not each time it is invoked. If that expression is null, creation can throw `NullPointerException`. A lambda `x -> receiver.method(x)` evaluates its captured receiver according to its own expression semantics and may fail later, so the forms are not always timing-equivalent.

Array constructor references such as `String[]::new` match `IntFunction<String[]>` and preserve a reified component type.

### Composition

Standard interfaces provide combinators. Predicates support `and`, `or`, and `negate`; functions support `compose` and `andThen`; consumers support sequential `andThen`.

```java
java.util.function.Function<String, String> strip = String::strip;
java.util.function.Function<String, String> lowercase =
        text -> text.toLowerCase(java.util.Locale.ROOT);
var normalize = strip.andThen(lowercase);
```

`strip.andThen(lowercase)` applies strip first. `strip.compose(lowercase)` would apply lowercase first. Composition preserves thrown runtime exceptions and does not introduce rollback; if the first consumer has a side effect and the second fails, the first effect remains.

Standard function interfaces do not declare checked exceptions. Options are to handle within the lambda, adapt from a custom checked interface at a boundary, or redesign the surrounding API. A generic "sneaky throw" erodes declared contracts and should not be a default strategy.

### Behavior and concurrency

A callback's contract must say whether it may run zero, one, or many times; synchronously or asynchronously; sequentially or concurrently; and on which thread or executor. Lambdas do not make captured mutable objects safe. Side effects in parallel streams or retry callbacks can race or repeat.

Do not assume function instances have meaningful `equals`, `hashCode`, serialization, or stable class names. If behavior needs persistent identity, represent it as explicit data or a named strategy type.

## Worked Java example

This validator composes named predicates and returns useful failures rather than a bare boolean.

```java
import java.util.List;
import java.util.function.Predicate;

public class ValidationDemo {
    record Rule<T>(String message, Predicate<? super T> test) {}

    static <T> List<String> validate(T value, List<Rule<T>> rules) {
        return rules.stream()
                .filter(rule -> !rule.test().test(value))
                .map(Rule::message)
                .toList();
    }

    public static void main(String[] args) {
        List<Rule<String>> rules = List.of(
                new Rule<>("must not be blank", text -> !text.isBlank()),
                new Rule<>("must be at most 12 characters", text -> text.length() <= 12));

        System.out.println(validate("a very long value", rules));
    }
}
```

`Predicate<? super T>` lets a rule consume T using a predicate defined for T or a supertype. Each rule also carries durable descriptive data rather than requiring introspection of the lambda.

## Execution or memory walkthrough

The two lambda expressions are target-typed as `Predicate<? super String>` through record construction and list inference. The list stores references to two immutable rule records, each of which holds a message and predicate reference.

`validate` creates a lazy stream pipeline. At terminal `toList`, each rule enters the filter. The blank rule passes its test, so negation makes the filter false and its message is excluded. The length rule returns false; negation includes it. `Rule::message` is an unbound instance reference whose input is a Rule receiver. `toList` returns an unmodifiable result list containing the one failure message.

The implementation may allocate or reuse lambda objects, but no correctness depends on their identity. The captured lambdas here are stateless.

## Complexity and performance

Creating or invoking functional behavior is conceptually constant overhead; body cost dominates. Validation is O(r) predicate invocations and O(f) result space for r rules and f failures. Predicate order can short-circuit when explicitly composed, while this implementation intentionally evaluates every rule to collect all messages.

Generic standard interfaces can box primitive arguments and results. Primitive specializations reduce that cost on numeric hot paths. Deep chains may affect stack traces and debugging clarity even if the JIT inlines them.

> **HotSpot note:** Java compilers commonly emit `invokedynamic` lambda factories, and HotSpot can reuse stateless instances, inline bodies, and remove allocations. No identity, singleton, or allocation guarantee follows from those optimizations.

## Edge cases and common mistakes

- A lambda has no standalone type; it needs a target functional interface.
- Similar functional-interface overloads can be ambiguous.
- Captured locals must be effectively final; captured objects may still mutate.
- Lambda `this` is the enclosing `this`, unlike an anonymous class.
- A bound method-reference receiver is evaluated immediately.
- Method references can become unclear when overloads or parameter reordering are involved; use a lambda for clarity.
- Standard function interfaces do not declare checked exceptions.
- Side-effecting callbacks may be invoked multiple times by retries or stream behavior.
- Parallel callbacks can race on captured collections or counters.
- Function-object identity, class names, and serialization are not portable contracts.
- `Consumer.andThen` does not run the second action if the first throws and does not undo completed effects.
- Using `Function<T, Boolean>` instead of `Predicate<T>` introduces boxing and weakens intent.

## Production engineering notes

Name nontrivial behavior. A local variable such as `isEligible` or a small strategy class gives stack traces and reviews more meaning than a large inline lambda. Put blocking, transactional, and retry semantics in callback documentation.

Pass immutable captured data where possible. When callbacks run concurrently, use thread-safe collaborators or confine state per invocation. Do not capture request-scoped objects in application-lifetime registries.

Functional interfaces are public APIs. Consider checked failures, variance, invocation count, ordering, nullability, thread, cancellation, and reentrancy. If callers need to log, persist, or configure a policy, pair executable behavior with explicit metadata or use a named domain type.

## Interview questions and model answers

**Is a lambda an anonymous inner class?**

No. Both can implement a functional role, but lambda `this` is lexical, capture and typing rules differ, and the runtime representation is unspecified. An anonymous class introduces a distinct class body and its own `this`.

**What is target typing?**

The assignment, parameter, cast, or return context supplies the functional interface whose single abstract method determines lambda parameter and result types. Without a target, a lambda expression cannot stand alone.

**Why must captured locals be effectively final?**

The lambda captures their values and may outlive the method frame. Preventing reassignment preserves clear value capture. It does not freeze objects reached through captured references.

**How does `String::length` work as a function?**

It is an unbound instance method reference. The function's first and only String argument becomes the receiver, and the result is that receiver's length.

**Are lambdas thread-safe?**

Not inherently. A stateless lambda can be safely invoked concurrently if its called operations are safe. A lambda capturing mutable state has the same synchronization and race concerns as any other code.

## Exercises

1. Write examples of static, bound, unbound, constructor, and array-constructor references.
2. Demonstrate the different meaning of `this` in a lambda and anonymous class.
3. Refactor ambiguous functional overloads into unambiguous method names.
4. Adapt a checked `IOException`-throwing supplier to a domain result without losing cause.
5. Find a lambda capture that retains a request object in a singleton registry and redesign it.
6. Add fail-fast and collect-all modes to `ValidationDemo`, documenting invocation counts.

## Chapter summary

Lambdas and method references implement target functional interfaces; they are not standalone dynamically typed functions. Capture copies effectively final local values, while object mutability and thread safety remain ordinary concerns. Method references cover static, bound, unbound, and constructor forms, with bound receivers evaluated at creation. Functional API contracts must define exceptions, invocation count, ordering, threading, side effects, and ownership, not only parameter types.

## Revision checklist

- [ ] I can determine whether an interface is functional.
- [ ] I understand lambda target typing and overload ambiguity.
- [ ] I can explain local capture and lexical `this`.
- [ ] I know all principal method-reference forms.
- [ ] I use standard and primitive functional interfaces appropriately.
- [ ] I handle checked failures at an explicit boundary.
- [ ] I do not depend on lambda identity, class shape, or serialization.
- [ ] I document callback side effects, repetition, threading, and cancellation.
