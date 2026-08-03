# Object-Oriented Foundations in Interview-Sized Steps

Object-oriented programming is useful when it makes a model easier to change and reason about. For DSA interviews, keep the model small. For Java interviews, explain the relationship and dispatch rules precisely.

## Inheritance: a subtype promise

```java
class Notification {
    String format() {
        return "notification";
    }
}

final class EmailNotification extends Notification {
    @Override
    String format() {
        return "email";
    }
}
```

`EmailNotification` is a `Notification`. Inheritance should express a truthful IS-A relationship, not merely reuse a few lines of code.

Superclass construction occurs before the subclass constructor body. Use `super(...)` to select a superclass constructor. Java allows one direct superclass and multiple implemented interfaces.

## Overriding and runtime polymorphism

```java
Notification notification = new EmailNotification();
System.out.println(notification.format()); // email
```

The reference's declared type is `Notification`; the runtime object is `EmailNotification`. An overridden instance method is dynamically dispatched to the object type.

Overloading is different: the compiler chooses among parameter lists using compile-time types. Fields and static methods are not dynamically dispatched like overridden instance methods.

## Upcasting and downcasting

An upcast to a superclass or interface is safe:

```java
Notification notification = new EmailNotification();
```

A downcast can fail with `ClassCastException`. Test the runtime type when the design genuinely requires the subtype:

```java
if (notification instanceof EmailNotification email) {
    System.out.println(email.format());
}
```

Pattern matching for `instanceof` is permanent since Java 16. For older targets, test and cast separately.

## Abstract classes

An abstract class can have constructors, fields, concrete methods, and abstract methods:

```java
abstract class Parser {
    Parser() {}

    final int parsePositive(String text) {
        int value = parse(text);
        if (value <= 0) {
            throw new IllegalArgumentException("positive value required");
        }
        return value;
    }

    protected abstract int parse(String text);
}
```

Use an abstract class when closely related types share construction, state, or a protected implementation template.

## Interfaces

An interface defines a role that unrelated classes can implement:

```java
interface Formatter {
    String format(int value);

    default String label(int value) {
        return "value=" + format(value);
    }
}
```

Interfaces can declare abstract methods, default methods, static methods, and private helper methods on supported Java versions. They do not have constructors or per-instance fields. A class can implement multiple interfaces.

## Composition and delegation

Composition expresses HAS-A:

```java
interface Clock {
    long nowMillis();
}

final class ExpiringToken {
    private final Clock clock;
    private final long expiresAt;

    ExpiringToken(Clock clock, long expiresAt) {
        this.clock = clock;
        this.expiresAt = expiresAt;
    }

    boolean isExpired() {
        return clock.nowMillis() >= expiresAt;
    }
}
```

The token has a clock; it is not a clock. A test can supply a deterministic implementation. Composition avoids inventing a false subtype and makes collaborators replaceable.

## Decision table

| Choose | When it best describes the model |
|---|---|
| concrete class | one complete implementation is the useful concept |
| interface | a role, contract, or replacement seam |
| abstract class | related types share state or construction |
| inheritance | the subtype preserves the parent's behavioral promise |
| composition | one object uses another capability |

## Equality and hashing introduction

`==` compares object identity. A class can override `equals` for semantic equality. Equal objects must have equal hash codes.

```java
record Coordinate(int row, int column) {}
```

Records provide value-oriented generated members for simple data carriers on Java 16+. An ordinary class remains the right choice when construction, identity, mutation, or framework requirements differ.

Do not mutate fields used by equality and hash code while an object is stored in a hash-based set or map. The Collections and Hashing books develop this contract fully.

## Complete example

File: `ObjectOrientedFoundationsExample.java`

```java
public final class ObjectOrientedFoundationsExample {
    interface DiscountPolicy {
        int apply(int priceCents);
    }

    static final class FixedDiscount implements DiscountPolicy {
        private final int discountCents;

        FixedDiscount(int discountCents) {
            this.discountCents = discountCents;
        }

        @Override
        public int apply(int priceCents) {
            return Math.max(0, priceCents - discountCents);
        }
    }

    static final class Checkout {
        private final DiscountPolicy policy;

        Checkout(DiscountPolicy policy) {
            this.policy = policy;
        }

        int total(int priceCents) {
            return policy.apply(priceCents);
        }
    }

    public static void main(String[] args) {
        DiscountPolicy policy = new FixedDiscount(250);
        Checkout checkout = new Checkout(policy);
        System.out.println(checkout.total(1_000));
    }
}
```

Expected output:

```text
750
```

`Checkout` composes a policy. The interface reference supports runtime polymorphism without making checkout a subtype of a discount.

## Edge-case matrix

| Case | Wrong shortcut | Accurate explanation |
|---|---|---|
| overloaded method | calls it runtime polymorphism | overload selection is compile-time |
| overridden instance method | looks only at reference type | runtime object selects the override |
| static method with same signature | calls it overriding | static methods are hidden |
| field with same name | expects polymorphism | field access follows declared type |
| forced downcast | assumes actual subtype | verify or redesign the base contract |
| abstract class | says it cannot construct state | it can have constructors |
| interface | says all methods are abstract | default/static/private behavior exists |
| multiple inheritance | expects two superclasses | Java permits multiple interfaces, one class |
| inheritance for reuse | ignores substitutability | prefer composition without a true IS-A relation |

## Interview room

**Interviewer:** What is the difference between overloading and overriding?

**Model answer:** Overloading uses the same method name with different parameter lists and is selected at compile time from declared types. Overriding replaces an inherited instance-method implementation, and runtime dispatch chooses the override using the actual receiver class.

**Follow-up:** When do you prefer composition?

**Model answer:** When one object uses another capability but is not a true subtype, or when I need easy replacement and testing. Inheritance creates a behavioral substitutability commitment, so I use it only when that promise is valid.

## Practice

1. **Foundation:** Override one method in a truthful subtype.
2. **Predict:** Trace a base reference holding a subclass object.
3. **Debugging:** Identify an unsafe downcast and redesign the base contract.
4. **Interview Core:** Replace false inheritance with a composed collaborator.
5. **Interview Core:** Compare an interface, abstract class, and concrete class for a parser family.
6. **SDE-2 Follow-up:** Explain why equality across a mutable inheritance hierarchy is difficult.

## Chapter takeaway

Use inheritance for a real subtype, interfaces for roles, abstract classes for related shared structure, and composition for replaceable collaborators. Keep compile-time overloading separate from runtime overriding.
