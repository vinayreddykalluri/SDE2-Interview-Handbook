# Classes, Objects, and Constructors

Java interview solutions often use only a few classes, but you still need a clear object model. A class defines a type; an object is one runtime instance; a reference is the value used to reach that object.

## State and behavior

```java
final class Student {
    private final int id;
    private String name;

    Student(int id, String name) {
        this.id = id;
        this.name = name;
    }

    void rename(String name) {
        this.name = name;
    }

    String name() {
        return name;
    }
}
```

Fields hold state. Methods expose behavior. `new Student(1, "Ada")` creates an object and produces a reference to it.

```java
Student first = new Student(1, "Ada");
Student alias = first;
alias.rename("Grace");
System.out.println(first.name()); // Grace
```

The assignment copies a reference value. It does not copy the object.

## Constructors establish valid starting state

A constructor has the class name and no return type, not even `void`.

```java
final class Rectangle {
    private final int width;
    private final int height;

    Rectangle(int width, int height) {
        if (width <= 0 || height <= 0) {
            throw new IllegalArgumentException("positive dimensions required");
        }
        this.width = width;
        this.height = height;
    }
}
```

Java supplies a default no-argument constructor only when the class declares no constructor. Once any constructor is declared, create every desired form explicitly.

## Constructor overloading and chaining

```java
final class Window {
    private final int width;
    private final int height;

    Window() {
        this(800, 600);
    }

    Window(int width, int height) {
        if (width <= 0 || height <= 0) {
            throw new IllegalArgumentException("positive dimensions required");
        }
        this.width = width;
        this.height = height;
    }
}
```

`this(...)` calls another constructor in the same class and must be the first statement. `super(...)` calls a superclass constructor and follows the same first-statement rule. A constructor cannot explicitly call both.

## `this` means the current object

In `this.name = name`, `this.name` is the field and `name` is the parameter. `this` can also be passed to another method or returned, but exposing an incompletely constructed `this` is dangerous and belongs in the advanced book.

## Instance and static members

An instance member belongs to an object. A static member belongs to the class:

```java
final class Identifier {
    private static int nextValue = 1;

    private Identifier() {}

    static int next() {
        return nextValue++;
    }
}
```

Call it as `Identifier.next()`. A static method has no `this` and cannot directly use instance fields.

Constants often use `static final`:

```java
private static final int MAX_ATTEMPTS = 3;
```

Mutable static state is shared across calls, tests, and threads. Use it only with an explicit lifecycle and concurrency policy.

## Access modifiers

| Modifier | Same class | Same package | Subclass elsewhere | Unrelated elsewhere |
|---|---:|---:|---:|---:|
| `private` | yes | no | no | no |
| package-private | yes | yes | no | no |
| `protected` | yes | yes | through inheritance rules | no |
| `public` | yes | yes | yes | yes |

Package-private is the absence of an access modifier. Across packages, protected access is tied to the subclass's inherited view; it is not simply public access for every subclass reference.

## Packages and imports

```java
package interview.model;

import java.util.List;
```

The package declaration normally comes first. Imports shorten source names; they do not copy a class into the current package or load it at runtime. Avoid the default package for maintainable multi-file projects.

## Encapsulation is about invariants

Encapsulation does not mean adding setters for every field. It means callers use operations that keep the object valid.

```java
final class BankAccount {
    private long balanceCents;

    BankAccount(long openingBalanceCents) {
        if (openingBalanceCents < 0) {
            throw new IllegalArgumentException("negative balance");
        }
        balanceCents = openingBalanceCents;
    }

    void withdraw(long cents) {
        if (cents <= 0 || cents > balanceCents) {
            throw new IllegalArgumentException("invalid withdrawal");
        }
        balanceCents -= cents;
    }
}
```

A raw `setBalance` would allow invalid state. The domain operation is the safer interface.

## Complete example

File: `ClassesAndConstructorsExample.java`

```java
public final class ClassesAndConstructorsExample {
    static final class Counter {
        private static int created;
        private int value;

        Counter() {
            this(0);
        }

        Counter(int initialValue) {
            if (initialValue < 0) {
                throw new IllegalArgumentException("negative initial value");
            }
            value = initialValue;
            created++;
        }

        void increment() {
            value++;
        }

        int value() {
            return value;
        }

        static int created() {
            return created;
        }
    }

    public static void main(String[] args) {
        Counter first = new Counter();
        Counter second = new Counter(4);
        second.increment();

        System.out.println(first.value());
        System.out.println(second.value());
        System.out.println(Counter.created());
    }
}
```

Expected output:

```text
0
5
2
```

## Edge-case matrix

| Case | Mistake | Correct rule |
|---|---|---|
| constructor declares `void` | creates an ordinary method | constructors have no return type |
| explicit constructor exists | expects generated no-arg constructor | declare it yourself if needed |
| two references reach object | assumes independent objects | assignment copies the reference |
| `final List` field | calls object immutable | final prevents reference reassignment only |
| static method reads instance field | no receiver exists | pass an object or use an instance method |
| public setter | breaks invariant | expose a meaningful validated operation |
| import statement | thinks package changes | import only shortens a compile-time name |

## Interview room

**Interviewer:** When does Java provide a default constructor?

**Model answer:** Only when the class declares no constructor. It is a no-argument constructor that invokes an accessible superclass constructor. Declaring any constructor means Java does not add another automatically.

**Follow-up:** Does `final` make an object immutable?

**Model answer:** No. A final variable cannot be reassigned after initialization. If it refers to a mutable object, that object's state may still change. Immutability depends on the complete class design and reachable state.

## Practice

1. **Foundation:** Create a validated `Rectangle` and its `area()` method.
2. **Foundation:** Add no-arg and parameterized constructors using `this(...)`.
3. **Predict:** Trace two references that designate the same mutable object.
4. **Debugging:** Repair a constructor accidentally written with `void`.
5. **Interview Core:** Refactor public mutable fields into an invariant-preserving class.
6. **SDE-2 Follow-up:** Explain why mutable static state complicates testing and concurrency.

## Chapter takeaway

Constructors establish valid objects, methods protect their invariants, references can alias, static state is shared, and access control limits who can depend on an implementation detail.
