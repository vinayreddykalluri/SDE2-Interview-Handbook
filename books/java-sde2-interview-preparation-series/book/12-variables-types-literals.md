# 12. Variables, Primitive Types, Literals, and Numeric Semantics

## Learning objectives

By the end of this chapter, you should be able to:

- distinguish a variable, a value, a type, and an object reference;
- choose an appropriate primitive type and write unambiguous literals;
- predict integer promotion, floating-point behavior, narrowing, and overflow;
- explain local-variable initialization, `final`, `var`, boxing, and unboxing; and
- design numeric code whose domain assumptions are explicit and testable.

## Why this matters at SDE-2

Many production failures are not algorithm failures. They are representation failures: an order total overflows an `int`, a nullable `Integer` is unboxed, a monetary comparison uses `double`, or a shift silently masks its distance. An SDE-2 engineer must see these risks during review, not after an incident.

Interviewers also use short numeric expressions to test whether a candidate reasons from Java rules instead of intuition borrowed from another language. The goal is not memorizing puzzles. It is building a reliable model for conversions and evaluation.

> **Learning-path note:** Build the Java language model in this chapter first. After Java Foundations and Time and Space Complexity, continue to Number Systems Volume 01 for digit algorithms, base conversion, overflow-safe arithmetic, large-number strings, GCD/LCM, modular arithmetic, powers, roots, and number-oriented practice.

## First-principles model

A Java variable is a named storage location with a compile-time type. A primitive variable contains a primitive value. A reference variable contains either `null` or a reference to an object or array; it does not contain the object itself.

Java has eight primitive types: `boolean`, `byte`, `short`, `char`, `int`, `long`, `float`, and `double`. Except for `boolean`, these participate in numeric operations. Integral types use fixed-width two's-complement representations in modern Java's specified numeric model. Floating-point types follow IEEE 754 binary floating-point semantics.

A literal is source syntax for a value. Its spelling affects its type: `1` is an `int`, `1L` is a `long`, `1.0` is a `double`, and `1.0F` is a `float`. A conversion may preserve the mathematical value, approximate it, wrap it, or be rejected.

> **Specification boundary:** Java specifies primitive ranges, conversions, and expression results. It does not expose a source-level memory address for a reference or guarantee that a local primitive occupies a particular number of physical bytes in a stack frame.

## Core terminology

- **Declaration:** introduces a variable and its type, such as `long count;`.
- **Initialization:** supplies its first value.
- **Assignment conversion:** conversion allowed when storing an expression into a variable.
- **Widening conversion:** moves to a type with a broader representable domain, although `long` to `float` can lose precision.
- **Narrowing conversion:** explicitly converts to a smaller or different domain and may discard information.
- **Numeric promotion:** converts operands before unary or binary arithmetic.
- **Overflow:** a result outside an integral type's range; ordinary Java integer arithmetic wraps modulo the type's width.
- **Boxing/unboxing:** conversion between a primitive and its wrapper, such as `int` and `Integer`.
- **Compile-time constant:** a restricted expression of primitive or `String` type whose value the compiler can determine.

## Detailed mechanics

### Types, ranges, and defaults

`byte` is signed 8-bit, `short` signed 16-bit, `int` signed 32-bit, and `long` signed 64-bit. `char` is an unsigned 16-bit UTF-16 code unit, not necessarily a complete Unicode character. `float` is 32-bit binary floating point and `double` is 64-bit.

Fields and array elements receive default values: numeric zero, `false`, or `null`. Local variables do not. Java's definite-assignment analysis rejects a path that might read an uninitialized local.

```java
public class InitializationDemo {
    private int field;              // default 0

    public int choose(boolean useField) {
        int local;
        if (useField) {
            local = field;
        } else {
            local = 10;
        }
        return local;               // assigned on every reachable path
    }
}
```

`final` means a variable can be assigned only once. For a reference, it freezes the reference, not the referenced object's state. A blank `final` field must be assigned by every constructor.

`var`, available for local variables since Java 10, asks the compiler to infer a static type from the initializer. It is not dynamic typing, cannot be used for fields or method parameters, and requires an initializer. Prefer it when the inferred type remains obvious.

### Literals

Integral literals can be decimal, hexadecimal (`0xFF`), binary (`0b1111`), or octal (`017`). Underscores may group digits but cannot occur at the literal's beginning, end, or next to a radix prefix or decimal point. An integer literal without `L` has type `int` and must satisfy the `int` literal range rules; use uppercase `L` for `long` because lowercase `l` resembles `1`.

```java
long nanos = 1_000_000_000L;
int permissions = 0b110_100_100;
int mask = 0xFF;
double ratio = 6.25e-2;             // 0.0625
float sample = 3.5F;
char newline = '\n';
char omegaCodeUnit = '\u03A9';
```

A special rule lets a constant `int` expression be assigned to `byte`, `short`, or `char` if the value fits. The same value held in a non-constant variable requires a cast.

```java
byte a = 100;             // constant fits
final int fixed = 100;
byte b = fixed;           // constant variable fits
int runtime = 100;
// byte c = runtime;      // compile-time error
byte c = (byte) runtime;
```

### Promotion and evaluation

Unary numeric promotion changes `byte`, `short`, and `char` to `int`. Binary numeric promotion normally chooses `double`, then `float`, then `long`, otherwise `int`. Thus two `byte` operands add as `int`.

```java
byte x = 10;
byte y = 20;
int sum = x + y;
// byte bad = x + y;
x += y;                   // compound assignment includes an implicit cast
```

Compound assignment `E1 op= E2` behaves roughly like `E1 = (T)(E1 op E2)`, while evaluating the left side once. That implicit narrowing can hide wraparound.

Integer division truncates toward zero. The remainder has the dividend's sign. Division by integral zero throws `ArithmeticException`, but floating-point division by zero produces infinity or NaN under IEEE 754 rules.

```java
System.out.println(-7 / 3);       // -2
System.out.println(-7 % 3);       // -1
System.out.println(1.0 / 0.0);    // Infinity
System.out.println(0.0 / 0.0);    // NaN
```

### Overflow and exact arithmetic

Ordinary integer overflow does not throw. `Integer.MAX_VALUE + 1` becomes `Integer.MIN_VALUE`. Use `Math.addExact`, `subtractExact`, `multiplyExact`, `incrementExact`, or conversion helpers such as `Math.toIntExact` when overflow is an invalid domain event.

Floating-point values model a finite subset of binary fractions. Decimal `0.1` is not exactly representable, so repeated addition can accumulate visible error. `BigDecimal` is appropriate for decimal business arithmetic, but construction matters: prefer a string or `BigDecimal.valueOf(double)` over `new BigDecimal(0.1)`.

NaN is unordered: every relational comparison with NaN is false, and `NaN == NaN` is false. Positive and negative zero compare equal with `==`, although some library operations distinguish their bit patterns.

### Boxing and unboxing

Wrapper objects enable primitives in generic APIs. Boxing may allocate, but the language does not promise object identity. `Integer` commonly caches a small range, so `==` can appear to work and then fail. Compare wrapper values with `equals` or `Objects.equals`.

Unboxing `null` throws `NullPointerException` at the conversion site. A conditional expression or overloaded call can trigger unboxing less visibly than a direct assignment.

## Worked Java example

This invoice example treats cents as exact integral units and rejects overflow.

```java
import java.math.BigDecimal;

public class InvoiceMath {
    static long lineTotalCents(long unitCents, int quantity) {
        if (unitCents < 0 || quantity < 0) {
            throw new IllegalArgumentException("negative price or quantity");
        }
        return Math.multiplyExact(unitCents, (long) quantity);
    }

    static BigDecimal dollars(long cents) {
        return BigDecimal.valueOf(cents, 2);
    }

    public static void main(String[] args) {
        long first = lineTotalCents(1_299L, 3);
        long second = lineTotalCents(250L, 4);
        long total = Math.addExact(first, second);
        System.out.println(total);           // 4897
        System.out.println(dollars(total));  // 48.97
    }
}
```

The public unit is explicit in the names. Multiplication promotes quantity to `long` before evaluation, and exact helpers turn an impossible invoice into an exception rather than corrupted data.

## Execution or memory walkthrough

For `lineTotalCents(1_299L, 3)`, the literal `1_299L` is already `long`; `3` is `int`. The method receives copies of both primitive values. The range checks evaluate without conversion surprises. Casting `quantity` to `long` ensures 64-bit multiplication, producing `3_897L`.

The second line produces `1_000L`. `Math.addExact` returns `4_897L`. `BigDecimal.valueOf(4897, 2)` represents an unscaled integer of 4897 with scale 2, so its decimal value is exactly 48.97.

If multiplication exceeded `long`, `multiplyExact` would throw before a wrapped value reached persistence. No wrapper is needed on this hot path. The values may be held in registers or stack-frame slots, but that placement is a JVM implementation decision.

## Complexity and performance

Primitive arithmetic is constant time for fixed-width types. Exact arithmetic helpers are also O(1), with a small overflow check. `BigInteger` and `BigDecimal` costs grow with precision; their operations allocate immutable result objects and are unsuitable as automatic replacements for every primitive.

Boxing can create objects and increase garbage-collection pressure. Specialized primitives, primitive streams, or domain objects backed by `long` often help in high-volume paths. Measure before optimizing, because escape analysis and JIT compilation can remove some allocations.

> **HotSpot note:** HotSpot may keep values in registers, scalar-replace non-escaping wrappers, and use cached wrapper instances. None of these optimizations changes Java's observable value semantics or makes wrapper identity safe to depend on.

## Edge cases and common mistakes

- `long result = intA * intB;` performs `int` multiplication before widening. Cast an operand first.
- `Math.abs(Integer.MIN_VALUE)` is still negative because its positive magnitude does not fit. `Math.absExact` throws instead.
- A shift distance is masked: for `int`, only the low five bits are used; `x << 32` is effectively `x << 0`.
- `>>` sign-extends; `>>>` shifts zero bits into the high end.
- Narrowing a floating-point value truncates toward zero, clamps out-of-range finite values to the target endpoint, and converts NaN to zero.
- `0.1 + 0.2 == 0.3` is false in binary floating point.
- Autounboxing a nullable wrapper can fail during comparison, arithmetic, or overload selection.
- `char` arithmetic promotes to `int`; a surrogate pair requires two `char` values.
- Never use wrapper `==` for value comparison.

## Production engineering notes

Put units in names and APIs: `timeoutMillis`, `amountCents`, and `bytesPerSecond`. Validate at boundaries, use exact helpers where overflow violates the domain, and test minimum, maximum, zero, and just-outside-range cases.

For money, decide whether fixed minor units or `BigDecimal` best fits currency and rounding requirements. Specify scale and `RoundingMode`; do not let defaults leak into accounting behavior. For scientific measurements, `double` is often correct, but define tolerances and NaN handling.

Be cautious when database, JSON, and Java numeric domains differ. A JavaScript client cannot exactly represent every `long` as a number. Database `DECIMAL` scale may reject or round a `BigDecimal`. Treat conversion as part of the contract.

## Interview questions and model answers

**Why does `long n = 1_000_000 * 1_000_000;` not produce one trillion?**

Both operands are `int`, so multiplication occurs in 32 bits and overflows before assignment widens the wrapped result. Make either operand `long`, for example `1_000_000L * 1_000_000`.

**Is widening always lossless?**

No. Widening integral conversion preserves values except conversions to floating point can lose precision. A sufficiently large `long` cannot be represented exactly by `float` or even by `double`.

**What does `final List<String> names` guarantee?**

The variable cannot be reassigned after initialization. It does not make the list immutable; callers can still mutate the referenced list unless the object itself prevents mutation.

**Why can `Integer a = 100; Integer b = 100; a == b` be true?**

Boxing implementations reuse some wrapper instances, and the language requires caching for certain constant values. `==` asks about reference identity, not numeric equality, so code must not rely on that result outside the specified caching case. Use `a.equals(b)` or `Objects.equals`.

**When would you choose `BigDecimal` over scaled `long`?**

Choose `BigDecimal` when variable decimal scale, large magnitudes, or explicit decimal rounding are central. Scaled `long` is compact and fast when the unit and range are fixed. In either design, encode scale and rounding in the domain API.

## Exercises

1. Predict the type and value of `byte b = 127; b += 1;`. Rewrite it so overflow throws.
2. Implement `average(long a, long b)` without overflowing their sum. Consider negative values.
3. Explain every conversion in `double d = 3 + 4L / 2.0F`.
4. Write tests for converting an incoming decimal price into cents with a required scale of two.
5. Find and fix nullable unboxing in `boolean active = map.get(id);`.
6. Compare `new BigDecimal("0.10")` and `new BigDecimal("0.1")` using both `equals` and `compareTo`; explain the different contracts.

## Chapter summary

Java's numeric rules are deterministic: literals have types, operands are promoted before evaluation, narrowing may discard information, and ordinary integral overflow wraps. Primitive variables hold values while reference variables hold references. `final` restricts assignment, `var` preserves static typing, and wrappers introduce identity and nullability concerns. Robust systems make units, range, precision, scale, and overflow policy visible in their APIs.

## Revision checklist

- [ ] I can state all eight primitive types and their roles.
- [ ] I can distinguish default field values from definite assignment of locals.
- [ ] I can trace unary and binary numeric promotion.
- [ ] I cast before arithmetic when the operation needs a wider type.
- [ ] I know how integer division, remainder, shifts, and overflow behave.
- [ ] I can explain NaN, infinity, signed zero, and decimal approximation.
- [ ] I use exact arithmetic or `BigDecimal` when the domain requires it.
- [ ] I avoid wrapper identity comparisons and defend against null unboxing.
