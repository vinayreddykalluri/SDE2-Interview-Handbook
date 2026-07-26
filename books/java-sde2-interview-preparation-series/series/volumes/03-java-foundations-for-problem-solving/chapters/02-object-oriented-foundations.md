# Classes, Objects, and Object-Oriented Foundations

## Learning objectives

This chapter builds the minimum object model needed for Java interviews: classes, objects, constructors, `this`, `static`, access control, packages, encapsulation, inheritance, polymorphism, abstraction, interfaces, composition, equality, and immutability.

## Class, object, state, and behavior

A class defines a type. An object is an instance of that class. Fields hold object state; methods expose behavior. A reference variable holds a reference value that may designate an object or be `null`; it does not contain the entire object.

```java
final class Student {
    private final int id;
    private String name;

    Student(int id, String name) {
        if (id <= 0) {
            throw new IllegalArgumentException("id must be positive");
        }
        this.id = id;
        this.name = java.util.Objects.requireNonNull(name);
    }

    int id() {
        return id;
    }

    String name() {
        return name;
    }

    void rename(String name) {
        this.name = java.util.Objects.requireNonNull(name);
    }
}
```

`Student first = new Student(1, "Ada");` creates one object and stores a reference value in `first`. `Student alias = first;` copies that reference value; both variables designate the same object. `alias.rename("Grace")` is therefore visible through `first`.

```text
first ----+
          +----> Student{id=1, name="Grace"}
alias ----+
```

Java still passes arguments by value. Passing `first` copies its reference value. A method can mutate the shared object through its copy, but assigning its parameter to a new object does not change the caller's variable.

## Constructors and `this`

A constructor initializes a new object. Its name matches the class and it has no return type, not even `void`.

```java
final class Window {
    private final int width;
    private final int height;

    Window() {
        this(800, 600);          // must be the first statement
    }

    Window(int width, int height) {
        if (width <= 0 || height <= 0) {
            throw new IllegalArgumentException("positive dimensions required");
        }
        this.width = width;      // field = parameter
        this.height = height;
    }
}
```

`this` is the current receiver. It disambiguates a field from a parameter and calls another constructor with `this(...)`. A constructor may call `this(...)` or `super(...)` first, never both explicitly. If a class declares no constructor, Java may supply a no-argument default constructor. The moment any constructor is declared, that automatic constructor is not supplied.

Avoid calling overridable methods from constructors: dynamic dispatch can reach subclass code before subclass initialization is complete.

## `static`: class state and behavior

An instance member belongs to an object. A static member belongs to the class.

```java
final class IdGenerator {
    private static int nextId = 1;

    private IdGenerator() {}

    static int next() {
        return nextId++;
    }
}
```

Call it as `IdGenerator.next()`. A static method has no `this` and cannot directly read an instance field. Static constants such as `private static final int MAX_ATTEMPTS = 3;` are appropriate. Mutable static state is shared across tests, calls, and threads; it makes reasoning and isolation harder and should be used deliberately.

Static methods are hidden, not overridden. Instance methods can be overridden and dynamically dispatched. Fields are also selected by the declared reference type, not polymorphically.

## Access modifiers and packages

| Modifier | Same class | Same package | Subclass in another package | Unrelated class elsewhere |
|---|---:|---:|---:|---:|
| `private` | yes | no | no | no |
| package-private | yes | yes | no | no |
| `protected` | yes | yes | yes, through inheritance rules | no |
| `public` | yes | yes | yes | yes |

Across packages, a subclass's `protected` access is tied to inheritance; it cannot use a subclass reference to reach the protected member through an arbitrary sibling/base instance. Treat `protected` as an extension API, not "public to subclasses everywhere."

The package declaration is first, apart from comments:

```java
package interview.model;

import java.util.List;
import static java.util.Objects.requireNonNull;
```

An import lets source use a short name. It does not copy the class, change its package, or load it at runtime. Use a fully qualified name to resolve conflicts. Avoid the default package in maintainable multi-file code because named packages cannot import from it cleanly.

## Encapsulation and invariants

Encapsulation means keeping representation decisions and invariants behind a small API. It is not the mechanical presence of getters and setters.

```java
final class BankAccount {
    private long balanceCents;

    BankAccount(long openingBalanceCents) {
        if (openingBalanceCents < 0) throw new IllegalArgumentException();
        balanceCents = openingBalanceCents;
    }

    void withdraw(long cents) {
        if (cents <= 0 || cents > balanceCents) {
            throw new IllegalArgumentException("invalid withdrawal");
        }
        balanceCents -= cents;
    }

    long balanceCents() {
        return balanceCents;
    }
}
```

A public setter for `balanceCents` would destroy the invariant. Expose the domain operation instead.

## Inheritance and runtime polymorphism

Inheritance expresses an IS-A relationship and a subtype promise.

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

Notification message = new EmailNotification();
System.out.println(message.format()); // email
```

The declared reference type is `Notification`; the runtime object type is `EmailNotification`. The selected overridable instance method is dispatched to the object type. This is runtime polymorphism. Overloading is compile-time selection among different parameter lists; return type alone cannot create an overload.

An upcast to a base type is safe. A downcast can fail:

```java
if (message instanceof EmailNotification email) {
    System.out.println(email.format());
}
```

The pattern form is final in Java 16+. In earlier Java, test with `instanceof` and then cast. Do not cast merely to access behavior that should be declared on the base contract.

`super(...)` invokes a superclass constructor and must be first when explicit. Superclass construction occurs before the subclass constructor body. Java supports multiple interface implementation, not multiple class inheritance.

## Abstraction and interfaces

Abstraction exposes what a type does while hiding unnecessary implementation detail. An abstract class can have fields, constructors, concrete methods, and abstract methods.

```java
abstract class Parser {
    Parser() {}
    final int parseNonNegative(String text) {
        int value = parse(text);
        if (value < 0) throw new IllegalArgumentException();
        return value;
    }
    protected abstract int parse(String text);
}
```

An interface defines a role or contract that unrelated classes may implement.

```java
interface Formatter {
    String format(int value);

    default String label(int value) {
        return "value=" + format(value);
    }

    static Formatter decimal() {
        return Integer::toString;
    }
}
```

Interfaces may contain abstract methods, default methods, static methods, and, since Java 9, private helper methods. They have no constructors or per-instance fields. Interface fields are constants. A **functional interface** has one abstract method and can be a lambda target; lambdas are introduced here only so library examples remain readable.

| Choose | When |
|---|---|
| Interface | a role, multiple implementations, an open contract, or test seam |
| Abstract class | closely related types share state, construction, or a template |
| Concrete class | one complete implementation is the useful concept |
| Composition | one object uses a replaceable collaborator or HAS-A relationship |

## Composition before forced inheritance

Composition delegates work to a collaborator.

```java
interface Clock {
    long nowMillis();
}

final class ExpiringToken {
    private final Clock clock;
    private final long expiresAt;

    ExpiringToken(Clock clock, long expiresAt) {
        this.clock = java.util.Objects.requireNonNull(clock);
        this.expiresAt = expiresAt;
    }

    boolean isExpired() {
        return clock.nowMillis() >= expiresAt;
    }
}
```

`ExpiringToken` HAS-A `Clock`; it is not a clock. A test can pass a deterministic clock. Composition keeps the token invariant local and permits replacement without inventing a false subtype.

## Equality, hash code, and immutability

`==` compares object identity. `equals` represents semantic equality when a class defines it. Equal objects must have equal hash codes; mutable fields that participate in equality make unsafe hash-map keys because mutation can move the logical key without moving its bucket entry.

An immutable class prevents observable state changes after construction. `final` on a reference prevents reassignment; it does not freeze the referenced object.

```java
final class Coordinate {
    private final int row;
    private final int column;

    Coordinate(int row, int column) {
        this.row = row;
        this.column = column;
    }

    @Override public boolean equals(Object other) {
        if (this == other) return true;
        if (!(other instanceof Coordinate that)) return false;
        return row == that.row && column == that.column;
    }

    @Override public int hashCode() {
        return java.util.Objects.hash(row, column);
    }
}
```

Records can express simple immutable data carriers in Java 16+, but ordinary classes remain essential interview knowledge.

## Common mistakes and interview angles

- Confusing a reference with the object itself.
- Calling Java pass-by-reference.
- Assuming a declared constructor leaves the compiler-provided no-argument constructor in place.
- Adding `void` to a constructor, accidentally creating a method.
- Calling static members through instances.
- Using inheritance only to reuse code.
- Treating fields or static methods as dynamically dispatched.
- Thinking abstract classes cannot have constructors or interfaces cannot have implemented methods.
- Saying `final` makes an object immutable.
- Overriding `equals` without a consistent `hashCode`.

## Quick check and practice

1. What is copied by `Student alias = first`?
2. When does Java provide a default constructor?
3. Why can a static method not use `this`?
4. What is selected from the declared reference type, and what is dispatched from the runtime object type?
5. When is composition more truthful than inheritance?

**Foundation:** Model a `Rectangle` with validated dimensions and an `area()` method.

**Interview Core:** Refactor a class with public mutable fields into an invariant-preserving API.

**SDE-2 Follow-up:** Explain why a mutable object used as a `HashMap` key can become unreachable by lookup.

## Cross-book boundary

This chapter teaches the object model required by DSA code. Continue to Low-Level Design for SOLID, patterns, modeling exercises, and larger collaborations; to Advanced Java for sealed types, records, reflection, and generic variance; and to the Hashing volume for collection-key contracts in depth.

## Chapter summary

Classes define state and behavior; references designate objects and may alias. Constructors establish invariants, instance methods use receivers, static members belong to the class, and access control shapes APIs. Runtime dispatch applies to overridden instance methods, while composition is often safer than forced inheritance. Equality, hashing, and immutability are behavioral contracts, not keywords to memorize.
