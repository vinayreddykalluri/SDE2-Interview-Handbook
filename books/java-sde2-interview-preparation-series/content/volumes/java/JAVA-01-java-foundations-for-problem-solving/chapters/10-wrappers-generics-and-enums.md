# Wrapper Classes, Basic Generics, and Enums

Collections store references, while early interview algorithms often work with primitive values. Wrapper classes and basic generics connect those two worlds safely. Enums give names and type safety to a fixed set of states.

## Wrapper classes

| Primitive | Wrapper |
|---|---|
| `byte` | `Byte` |
| `short` | `Short` |
| `int` | `Integer` |
| `long` | `Long` |
| `float` | `Float` |
| `double` | `Double` |
| `char` | `Character` |
| `boolean` | `Boolean` |

Wrappers are immutable and provide parsing, comparison, conversion, constants, and utilities:

```java
int count = Integer.parseInt("42");
long maximum = Long.MAX_VALUE;
int order = Integer.compare(10, 20);
```

Parsing invalid or out-of-range text throws `NumberFormatException`.

## Autoboxing and unboxing

```java
java.util.List<Integer> values = new java.util.ArrayList<>();
values.add(7);             // boxes int to Integer
int first = values.get(0); // unboxes Integer to int
```

Unboxing `null` throws `NullPointerException`:

```java
Integer missing = null;
// int value = missing; // runtime failure when reached
```

Wrapper `==` compares identity. Small cached values can make identity appear to work:

```java
Integer first = 127;
Integer second = 127;
System.out.println(first == second); // true for required cached constants
```

Values outside the required cached range may use different objects. Use `equals` or `Objects.equals`; never depend on wrapper identity for numeric meaning.

## Why generics matter

Without a type argument, a collection can defer a type mistake until runtime. Generics let the compiler enforce the intended element type:

```java
java.util.List<String> names = new java.util.ArrayList<>();
names.add("Ada");
String firstName = names.get(0);
```

The diamond `<>` asks the compiler to infer type arguments from context.

Primitive types cannot be generic arguments, so write `List<Integer>`, not `List<int>`.

## A small generic class and method

```java
final class Box<T> {
    private final T value;

    Box(T value) {
        this.value = value;
    }

    T value() {
        return value;
    }
}

static <T> T first(java.util.List<T> values) {
    if (values.isEmpty()) {
        throw new IllegalArgumentException("empty values");
    }
    return values.get(0);
}
```

`T` is a type parameter. `Box<String>` is one parameterization. The method declares its own `T` before the return type.

A light bound exposes an operation:

```java
static <T extends Comparable<? super T>> T smaller(T left, T right) {
    return left.compareTo(right) <= 0 ? left : right;
}
```

Wildcard variance, capture, erasure, and heap pollution belong in the Advanced Java Language book.

## Enums model finite state

```java
enum OrderStatus {
    CREATED,
    PAID,
    SHIPPED,
    DELIVERED,
    CANCELLED;

    boolean isTerminal() {
        return this == DELIVERED || this == CANCELLED;
    }
}
```

Enum constants are typed instances. Enums can have fields, constructors, and methods and work naturally with switch. They are safer than magic strings because the compiler checks names.

Do not persist `ordinal()` as a durable external value. Reordering constants would change it. Persist an explicit stable code when the domain needs one.

## Complete example

File: `WrappersGenericsEnumsExample.java`

```java
import java.util.ArrayList;
import java.util.List;

public final class WrappersGenericsEnumsExample {
    enum Difficulty { FOUNDATION, INTERVIEW_CORE, SDE2_FOLLOW_UP }

    record Exercise(String title, Difficulty difficulty) {}

    static <T> T first(List<T> values) {
        if (values.isEmpty()) {
            throw new IllegalArgumentException("empty values");
        }
        return values.get(0);
    }

    public static void main(String[] args) {
        List<Integer> scores = new ArrayList<>();
        scores.add(Integer.parseInt("90"));

        List<Exercise> exercises = List.of(
                new Exercise("Array traversal", Difficulty.FOUNDATION));

        System.out.println(first(scores));
        System.out.println(first(exercises).difficulty());
    }
}
```

Expected output:

```text
90
FOUNDATION
```

## Edge-case matrix

| Case | Failure | Correction |
|---|---|---|
| invalid numeric text | `NumberFormatException` | validate/catch at the input boundary |
| null wrapper arithmetic | unboxing `NullPointerException` | define null policy before arithmetic |
| wrapper comparison with `==` | identity-dependent result | use value equality or primitive comparison |
| raw collection | unchecked runtime type failure | use a specific generic type |
| `List<int>` | does not compile | use `List<Integer>` |
| empty generic helper | invalid first element | reject or return an explicit absence type |
| enum ordinal persisted | declaration reorder changes value | use an explicit stable external code |
| string status | typo survives compilation | model finite states with an enum |

## Interview room

**Interviewer:** Why can `Integer first = 127; Integer second = 127; first == second` be true?

**Model answer:** Boxing of certain constant values uses required cached instances, so both references can designate one object. `==` still means identity, not numeric equality. I use `equals` or unbox to compare values and never depend on cache identity.

**Follow-up:** Why can generics not use primitive type arguments?

**Model answer:** Java generics operate on reference types. Primitive values must use wrappers such as `Integer`, with boxing and unboxing at boundaries. Advanced details such as erasure belong in the generics chapter later.

## Practice

1. **Foundation:** Parse three integers and handle invalid input explicitly.
2. **Predict:** Compare boxed 127 and 128 values using both `==` and `equals`.
3. **Debugging:** Find the null-unboxing failure in a `Map<String, Boolean>` lookup.
4. **Interview Core:** Write a generic `last(List<T>)` with an empty-input contract.
5. **Interview Core:** Replace string order states with an enum and exhaustive switch.
6. **SDE-2 Follow-up:** Explain the difference between unmodifiable membership and immutable element state.

## Chapter takeaway

Wrappers let primitives participate in reference-based APIs but add null and identity traps. Generics move type errors to compilation. Enums model a finite state space more safely than strings.
