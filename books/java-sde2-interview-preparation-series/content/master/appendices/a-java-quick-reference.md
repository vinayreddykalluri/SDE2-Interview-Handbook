# Appendix A - Java Syntax and Language Quick Reference

This appendix is a recall sheet, not a substitute for the language chapters. Every compact rule hides edge cases. When correctness depends on an exact compile-time rule, use the Java Language Specification linked in Appendix G.

After Java Foundations, continue to `Java-SDE2-DSA-02-Time-and-Space-Complexity.pdf`, then use `Java-SDE2-DSA-01-Number-Systems-and-Math-Foundations.pdf` for a printable numeric quick-reference with worked algorithms. For the full focused sequence, open `dist/Java-SDE2-Interview-Preparation-Series-Index.pdf`.

## Source files, packages, and launch

A conventional source file begins with an optional package declaration, followed by imports and type declarations:

```java
package com.example.orders;

import java.time.Instant;
import java.util.List;

public final class OrderBatch {
    private final List<String> ids;
    private final Instant createdAt;

    public OrderBatch(List<String> ids, Instant createdAt) {
        this.ids = List.copyOf(ids);
        this.createdAt = createdAt;
    }

    public static void main(String[] args) {
        var batch = new OrderBatch(List.of("demo"), Instant.now());
        System.out.println("orders=" + batch.ids.size());
    }
}
```

At most one top-level public type appears in a source file, and its name normally matches the file name. A package name maps to a directory layout by convention and by most build tools. The unnamed package is unsuitable for maintained applications because named packages cannot import from it.

Common commands:

```text
javac --release 21 -d out src/com/example/orders/OrderBatch.java
java -cp out com.example.orders.OrderBatch
java --enable-preview --source 21 PreviewExample.java
javap -classpath out -c -v com.example.orders.OrderBatch
```

`--release 21` selects a supported language level and matching documented Java SE APIs. It is safer for cross-compilation than setting only `-source` and `-target`. Java 21 features covered in this book do not require preview mode when they were final in that release.

## Primitive types, references, and default values

Java has eight primitive types:

| Type | Width or model | Representative range or values | Default field value |
|---|---:|---|---|
| `byte` | 8-bit signed | -128 through 127 | `0` |
| `short` | 16-bit signed | -32,768 through 32,767 | `0` |
| `int` | 32-bit signed | -2^31 through 2^31 - 1 | `0` |
| `long` | 64-bit signed | -2^63 through 2^63 - 1 | `0L` |
| `char` | 16-bit unsigned UTF-16 code unit | `\u0000` through `\uffff` | `\u0000` |
| `float` | IEEE 754 binary32 | finite values, infinities, NaN | `0.0f` |
| `double` | IEEE 754 binary64 | finite values, infinities, NaN | `0.0d` |
| `boolean` | logical value | `true`, `false` | `false` |

Reference variables hold reference values or `null`; they do not contain an object inline as a language guarantee. Fields and array elements receive defaults. Local variables must be definitely assigned before use.

Integer arithmetic on `byte`, `short`, and `char` is normally promoted to `int`. Integer overflow wraps using two's-complement arithmetic; it does not throw. Use `Math.addExact`, `subtractExact`, and related methods when overflow must be rejected. Floating-point `NaN` is unequal to every value, including itself; use `Double.isNaN` rather than equality.

Useful literals:

```java
int decimal = 1_000_000;
int hexadecimal = 0xFF;
int binary = 0b1010_0110;
long longValue = 9_000_000_000L;
float ratio = 0.25F;
char newline = '\n';
String textBlock = """
        line one
        line two
        """;
```

## Conversions, boxing, and numeric comparison

Widening primitive conversions such as `int` to `long` are implicit, although a large integer can lose precision when converted to floating point. Narrowing conversions require a cast and can discard high bits or truncate a fractional component.

Boxing converts a primitive to a wrapper, and unboxing extracts the primitive. Unboxing `null` throws `NullPointerException`. Wrapper identity is not a numeric comparison:

```java
Integer a = 1_000;
Integer b = 1_000;
boolean wrongQuestion = a == b;       // compares references
boolean valueEquality = a.equals(b);  // compares int values
```

Do not rely on wrapper caches. Prefer primitives in hot numeric loops unless nullability, generic APIs, or object semantics are required.

For money, binary floating point is usually the wrong domain model. Use an integer minor-unit representation when scale is fixed, or use `BigDecimal` with an explicit scale and rounding policy. `new BigDecimal("0.1")` is predictable; `new BigDecimal(0.1)` captures the binary floating-point approximation.

## Operators and precedence

From high to low, a practical precedence summary is:

| Family | Operators |
|---|---|
| postfix | `expr++`, `expr--` |
| unary | `++`, `--`, `+`, `-`, `~`, `!`, cast |
| multiplicative | `*`, `/`, `%` |
| additive | `+`, `-` |
| shift | `<<`, `>>`, `>>>` |
| relational | `<`, `<=`, `>`, `>=`, `instanceof` |
| equality | `==`, `!=` |
| bitwise | `&`, then `^`, then `|` |
| conditional logical | `&&`, then `||` |
| conditional | `condition ? yes : no` |
| assignment | `=`, compound assignments |

Use parentheses when a reviewer would otherwise have to recall the table. `&&` and `||` short-circuit; `&` and `|` evaluate both operands even for booleans. Compound assignment includes an implicit conversion: `byteValue += 1` compiles where `byteValue = byteValue + 1` needs a cast.

`==` compares primitive values or reference identity. Semantic object equality is normally expressed by `equals`. Strings must not be compared with `==`.

## Control flow and pattern matching

Basic forms:

```java
if (ready) {
    start();
} else {
    defer();
}

for (int i = 0; i < values.length; i++) {
    consume(values[i]);
}

for (String value : values) {
    consume(value);
}

while (condition()) {
    step();
}

do {
    step();
} while (condition());
```

A switch expression returns a value and should be exhaustive:

```java
enum State { NEW, RUNNING, DONE }

String label = switch (state) {
    case NEW -> "queued";
    case RUNNING -> "active";
    case DONE -> "complete";
};
```

The colon form retains fall-through semantics. Prefer arrow labels unless fall-through is deliberate and obvious.

Pattern variables are flow-scoped:

```java
if (candidate instanceof String text && !text.isBlank()) {
    System.out.println(text.length());
}
```

Record patterns and pattern matching for switch are final in Java 21. A sealed hierarchy lets the compiler check exhaustiveness when all permitted direct subtypes are known in the relevant compilation context.

## Methods, overloads, and parameter passing

A method signature for overloading consists of the method name and parameter types. Return type alone cannot distinguish overloads. Resolution occurs at compile time using the declared argument types, applicability, conversions, and most-specific rules. Overriding is dynamic dispatch based on the runtime receiver type.

```java
static long sum(int left, long right) {
    return left + right;
}

static int sum(int... values) {
    int total = 0;
    for (int value : values) total += value;
    return total;
}
```

Varargs is an array parameter with call-site convenience. It can allocate an array and can create overload ambiguity. A caller may pass `null` as the entire array, so a public varargs method still needs a null policy.

Java is always pass-by-value. For an object argument, the copied value is a reference. The callee can use that copied reference to mutate the same mutable object, but assigning a different object to the parameter does not change the caller's variable.

## Arrays, strings, and text

Arrays are reified, covariant objects with a fixed length:

```java
String[] names = new String[3];
int[][] matrix = new int[4][5];
int[] copy = java.util.Arrays.copyOf(values, values.length);
```

Covariance can fail at runtime: assigning a `String[]` to `Object[]` is legal, but storing a non-string throws `ArrayStoreException`. Generic collections are invariant and catch the analogous mismatch at compile time.

`String` is immutable. Concatenation in one expression is compiled using platform mechanisms selected by the compiler; repeated concatenation in a loop should normally use `StringBuilder`. `String.length()` counts UTF-16 code units, not user-perceived characters. A supplementary Unicode code point uses a surrogate pair. Use `codePoints()` when code points are the needed abstraction and a text library when grapheme clusters matter.

Common equality and emptiness checks:

```java
boolean same = java.util.Objects.equals(left, right);
boolean empty = text.isEmpty();
boolean blank = text.isBlank();
```

## Classes, records, interfaces, and sealed hierarchies

Class skeleton:

```java
public final class Account {
    private final String id;
    private long balance;

    public Account(String id) {
        this.id = java.util.Objects.requireNonNull(id);
    }

    public synchronized void credit(long cents) {
        balance = Math.addExact(balance, cents);
    }
}
```

Access levels:

| Modifier | Same class | Same package | Subclass in another package | Everywhere |
|---|---:|---:|---:|---:|
| `private` | yes | no | no | no |
| package-private | yes | yes | no | no |
| `protected` | yes | yes | qualified rules apply | no |
| `public` | yes | yes | yes | yes |

Constructors are not inherited. A subclass constructor begins with an explicit or implicit invocation of another constructor. Static methods are hidden, not overridden. Private methods are not overridden. An overriding method may use a covariant return type and cannot broaden checked exceptions.

A record declares a nominal data carrier:

```java
public record Range(int start, int end) {
    public Range {
        if (start > end) throw new IllegalArgumentException("reversed");
    }
}
```

Records are shallowly immutable: component fields are final, but a referenced component can still be mutable. Make defensive copies when value semantics require them.

An interface defines a contract and can contain abstract, default, static, and private methods. An abstract class can hold instance state and protected implementation machinery. Prefer composition when the relationship is behavioral reuse rather than a stable subtype relationship.

Sealed types restrict direct subtypes:

```java
sealed interface Result permits Success, Failure {}
record Success(String value) implements Result {}
record Failure(String message) implements Result {}
```

Each permitted subtype must be `final`, `sealed`, or `non-sealed` unless its form, such as a record, is implicitly final.

## Equality, hashing, and ordering

The `equals` contract requires reflexivity, symmetry, transitivity, consistency, and a false result for null. Equal objects must have equal hash codes. Unequal objects may collide. Fields used by `equals` and `hashCode` should not change while the object is a key in a hash-based collection.

`Comparable<T>` defines a natural ordering. `Comparator<T>` defines an external ordering and supports composition:

```java
Comparator<Person> order = Comparator
        .comparing(Person::lastName)
        .thenComparing(Person::firstName)
        .thenComparingInt(Person::age);
```

An ordering inconsistent with equality is legal for some APIs but can make a sorted set or map treat unequal values as the same key. Document the choice.

## Exceptions and resource management

Checked exceptions are subclasses of `Exception` excluding `RuntimeException`; the compiler enforces catch-or-declare. Unchecked exceptions include `RuntimeException` and `Error`. Do not catch `Throwable` as routine application control flow.

```java
try (var input = java.nio.file.Files.newBufferedReader(path)) {
    return input.readLine();
} catch (java.io.IOException failure) {
    throw new UncheckedIOException("cannot read " + path, failure);
}
```

Try-with-resources closes initialized resources in reverse order. If both the body and close fail, the body failure is primary and close failures are suppressed. Inspect `getSuppressed()` when diagnosing layered resource failures.

Catch the narrowest useful type, preserve the cause when translating, and attach context without secrets. An interruption caught as `InterruptedException` should normally be propagated or restored with `Thread.currentThread().interrupt()`.

## Generics and variance

Generic types are invariant: `List<Integer>` is not a subtype of `List<Number>`. Use bounded wildcards at API boundaries:

```java
static double total(java.util.List<? extends Number> source) {
    return source.stream().mapToDouble(Number::doubleValue).sum();
}
static void addDefaults(java.util.List<? super Integer> sink) { /* write */ }
```

Mnemonic: producer extends, consumer super. A wildcard is not a declaration-site type parameter and may need capture by a helper method.

Type erasure means most type arguments are not reified at runtime. Consequently, Java prohibits `new T()`, `new T[]`, `instanceof List<String>`, and overloading methods whose signatures erase to the same form. Heap pollution occurs when a variable of a parameterized type refers to an incompatible value, often through raw types, unchecked casts, or generic varargs.

Prefer a generic method when the type relationship is local to one operation. Prefer a generic class when the type relationship belongs to the object's persistent state or contract.

## Lambdas, method references, and streams

A lambda implements a functional interface, an interface with one abstract method after inheritance rules:

```java
java.util.function.Predicate<String> useful =
        value -> value != null && !value.isBlank();
java.util.function.Function<String, Integer> length = String::length;
```

Captured local variables must be final or effectively final. Capturing mutable state does not make access thread-safe.

Streams describe a lazy traversal pipeline. Intermediate operations are lazy; a terminal operation drives evaluation:

```java
Map<String, Long> counts = words.stream()
        .filter(useful)
        .map(String::toLowerCase)
        .collect(java.util.stream.Collectors.groupingBy(
                java.util.function.Function.identity(),
                java.util.stream.Collectors.counting()));
```

Do not reuse a consumed stream. Avoid side effects in behavioral parameters. Parallel streams use shared runtime machinery and are not automatically faster; measure a representative, sufficiently large, CPU-oriented workload and account for blocking, ordering, splitting, and contention.

## Concurrency recall sheet

Key rules:

- Starting a thread happens-before actions in that thread.
- All actions in a thread happen-before another thread detects its termination, including an untimed `join()` that returns.
- An unlock on a monitor happens-before a later lock on the same monitor.
- A volatile write happens-before a later read of that same field in synchronization order.
- Final fields receive special visibility guarantees when construction is correct and `this` does not escape.
- `volatile` supplies visibility and ordering for the field; it does not turn `count++` into one atomic action.
- Higher-level concurrent collection and executor methods have their own documented memory-consistency effects.

Prefer ownership, immutability, message passing, and well-defined concurrent components over scattered low-level synchronization. Every blocking operation needs a cancellation and shutdown story.

Java 21 virtual threads make thread-per-task practical for many blocking workloads. They do not remove resource limits, make CPU work parallel beyond available processors, or repair data races. Bound downstream resources such as connections and request quotas rather than pooling virtual threads merely to limit their count.

## Common interview traps

- Java has no pass-by-reference parameter mode.
- `String` immutability does not make every object containing a string immutable.
- `final` freezes a variable binding or prevents overriding/inheritance in context; it is not a universal deep-immutability or thread-safety keyword.
- `volatile` is not mutual exclusion.
- `HashMap` has no general iteration-order contract.
- `PriorityQueue` exposes only its head ordering; iteration is not sorted.
- `Arrays.asList` returns a fixed-size list backed by the array.
- `List.subList` is a backed view whose structural validity depends on disciplined modification.
- `Stream.toList()` returns an unmodifiable list by contract; do not assume a concrete implementation.
- `Optional` is primarily a return-type tool, not a universal field, parameter, or serialization model.
- `Thread.sleep` does not release a held monitor.
- `wait` must be called while owning the monitor, releases that monitor while waiting, and belongs in a condition loop.
- A successful test run cannot prove a racy program correct.
- Big-O does not describe allocation, locality, constant costs, warm-up, contention, or tail latency.

Use this appendix for rapid recall, then return to the relevant chapter to recover the mechanism, proof obligation, trade-off, and production implication.
