# Chapter 6: Java Integer Types and Numeric Limits

Java arithmetic is predictable only after you know the type of every operand. Many interview bugs are not caused by a wrong algorithm; they are caused by evaluating a correct formula in a type that is too small. This chapter builds the numeric model needed for array indexes, counters, binary search, comparators, products, and large results.

## What an interviewer is testing

You should be able to:

- choose between `int`, `long`, floating-point types, and `BigInteger`;
- state the fixed ranges of Java integral types;
- trace widening, narrowing, and integer promotion;
- explain why assignment to `long` does not retroactively widen an `int` calculation;
- prevent overflow in multiplication and midpoint calculations; and
- recognize when no primitive type can represent the required answer.

## Numeric types at a glance

![Java primitive integer ranges and the promotion rules that matter in interviews.](content/volumes/01-number-systems-and-math-foundations/assets/05-java-primitive-ranges.png)

| Type | Model | Range or precision | Interview use |
|---|---|---|---|
| `byte` | signed 8-bit integer | -128 through 127 | compact storage and byte protocols, rarely arithmetic |
| `short` | signed 16-bit integer | -32,768 through 32,767 | compact storage, rarely arithmetic |
| `int` | signed 32-bit integer | -2^31 through 2^31 - 1 | indexes, counts, most coding-platform inputs |
| `long` | signed 64-bit integer | -2^63 through 2^63 - 1 | sums, products, timestamps, larger counters |
| `char` | unsigned 16-bit code unit | 0 through 65,535 | UTF-16 code units, not general numeric storage |
| `float` | IEEE 754 binary32 | about 7 decimal digits | uncommon in interviews |
| `double` | IEEE 754 binary64 | about 15 to 16 decimal digits | measurements and approximate arithmetic |

`byte`, `short`, `int`, and `long` use fixed-width signed two's-complement arithmetic. `char` is integral but unsigned. `float` and `double` are approximate binary floating-point types; they are not wider integer types. A large integer converted to `double` can lose low-order bits even when it is inside the numeric range of `double`.

`BigInteger` stores integers beyond `long` and provides arithmetic, parsing, base conversion, GCD, and modular operations. It is the right tool when arbitrary precision is part of the contract, but it does not replace the manual digit-by-digit algorithms interviewers ask you to derive. `BigDecimal` stores decimal values with explicit scale and rounding; know why it exists, but ordinary integer DSA problems rarely need it.

Binary floating-point cannot represent every decimal fraction exactly. For example, `0.1 + 0.2` is not exactly `0.3` as a `double`. Do not use exact `==` for approximate measurements; choose a tolerance policy appropriate to the domain. For integer predicates such as perfect square, prefer integer arithmetic or verify any `Math.sqrt` estimate with an overflow-safe integer comparison.

Integer literals are `int` by default when they fit. Add `L` to force a `long` operand before evaluation. Floating literals are `double` by default; add `F` only when a `float` is truly required.

Useful named boundaries include `Integer.MIN_VALUE`, `Integer.MAX_VALUE`, `Long.MIN_VALUE`, and `Long.MAX_VALUE`. Prefer these constants over handwritten literals.

## Defaults and local variables

Fields and array elements receive default values. Numeric fields default to zero, `char` defaults to the zero code unit, and reference fields default to `null`. Local variables do not receive a usable default; Java requires definite assignment before a local is read.

This distinction occasionally appears in code-output questions, but its practical value is larger: a local result must be assigned on every reachable path, while a forgotten field assignment can silently leave a zero that looks valid.

## Widening, narrowing, and promotion

A widening conversion moves to a type whose range generally contains the integral source, such as `int` to `long`. It is implicit:

```java
int count = 2_000_000;
long copiedCount = count;
```

A narrowing conversion can discard high bits or change the value, so it requires a cast:

```java
int original = 130;
byte narrowed = (byte) original; // -126, not 130
```

The cast says, "perform this conversion." It does not say, "throw if information is lost." Use `Math.toIntExact(longValue)` when a value must fit in `int`; it throws `ArithmeticException` otherwise.

Arithmetic on `byte`, `short`, and `char` normally promotes operands to `int` first:

```java
byte left = 10;
byte right = 20;
int sum = left + right;
```

Binary numeric promotion selects a common type before calculation. If either operand is `double`, calculation is in `double`; otherwise `float`, then `long`, otherwise `int`. The important phrase is **before calculation**.

## Overflow happens before assignment

Consider this common mistake:

```java
int a = 1_500_000_000;
int b = 2;
long wrong = a * b;
```

Both operands are `int`, so `a * b` is evaluated in 32 bits and wraps. Only the already-wrong result is widened to `long`.

Widen an operand first:

```java
long result = (long) a * b;
long equivalent = 1L * a * b;
```

The required interview patterns are:

```java
long result = (long) a * b;
long value = 1L * a * b;
int mid = left + (right - left) / 2;
```

The midpoint expression assumes the ordinary binary-search contract `0 <= left <= right <= Integer.MAX_VALUE`. Under that contract, `right - left` cannot overflow. For arbitrary signed endpoints, calculate in `long` and decide how rounding should work.

## A compiling numeric-safety example

```java
public final class NumericLimitsDemo {
    private NumericLimitsDemo() {}

    static long multiplyInts(int left, int right) {
        return (long) left * right;
    }

    static int midpoint(int left, int right) {
        if (left < 0 || right < left) {
            throw new IllegalArgumentException("invalid index range");
        }
        return left + (right - left) / 2;
    }

    static long multiplyLongsExactly(long left, long right) {
        return Math.multiplyExact(left, right);
    }

    static int narrowExactly(long value) {
        return Math.toIntExact(value);
    }

    public static void main(String[] args) {
        int left = 1_500_000_000;
        int right = 2;

        System.out.println(multiplyInts(left, right)); // 3000000000
        System.out.println(midpoint(1_900_000_000, 2_000_000_000));
        System.out.println(multiplyLongsExactly(3_000_000L, 4_000_000L));
        System.out.println(narrowExactly(42L));
    }
}
```

`multiplyInts` is safe because the largest possible magnitude of an `int * int` product fits in `long`. The largest product is at most 2^62 in magnitude. `multiplyLongsExactly` needs an explicit overflow check because a `long * long` product may require up to 128 bits.

All four methods use O(1) time and O(1) auxiliary space.

## Dry run: safe multiplication and midpoint

For `multiplyInts(1_500_000_000, 2)`:

1. The cast converts the left operand to `long`.
2. Binary numeric promotion converts the right operand to `long`.
3. Multiplication occurs in 64 bits and produces `3_000_000_000L`.
4. The method returns that exact value.

For `midpoint(1_900_000_000, 2_000_000_000)`:

1. `right - left` is `100_000_000`.
2. Dividing by two gives `50_000_000`.
3. Adding to `left` gives `1_950_000_000`.

The naive expression `(left + right) / 2` would overflow while adding the two indexes.

## When `int`, `long`, and `BigInteger` are appropriate

Use `int` when constraints prove that indexes, values, and intermediate results fit. Java arrays are indexed by `int`, so using `long` for every loop variable often creates noisy casts without adding safety.

Use `long` for sums and products that can exceed `int`. Read the constraints before coding. If `n <= 100_000` and each value is at most `1_000_000_000`, a sum can reach 10^14, so `long` is required.

Use `BigInteger` when the mathematical result can exceed `long`, such as an exact factorial, a very large parsed integer, or unrestricted integer multiplication:

```java
java.math.BigInteger product = java.math.BigInteger.valueOf(leftLong)
        .multiply(java.math.BigInteger.valueOf(rightLong));
```

`BigInteger` is immutable. Its operations allocate results, and their cost grows with the number of machine words in the value. Do not call them O(1) merely because one method call appears in source. Also do not use `BigInteger` automatically when a proven `long` bound is simpler and faster.

## Edge cases and common mistakes

- `Math.abs(Integer.MIN_VALUE)` is still negative because positive 2^31 does not fit in `int`.
- `Math.abs(Long.MIN_VALUE)` has the same problem for `long`.
- Casting after multiplication is too late: `(long) (a * b)` preserves the wrapped `int` result.
- `byte` and `short` operands calculate as `int`.
- Compound assignment includes an implicit narrowing conversion; `byteValue += 1` can wrap.
- Integer division truncates toward zero. Cast before division if a fractional result is required.
- Widening `long` to `double` can lose integer precision.
- `char` arithmetic promotes to `int`, and one `char` is not always one Unicode character.
- The safe index-midpoint formula needs an ordered, nonnegative index interval.
- A comparator must use `Integer.compare(a, b)`, not `a - b`, because subtraction can overflow.

## Interview follow-ups

**Why not use `long` everywhere?** It does not prevent `long` overflow, cannot index arrays without conversion, consumes more storage in large primitive arrays, and can hide the need to prove constraints. Choose the smallest type that safely contains all required intermediate values.

**Can every `long` be represented exactly by `double`?** No. `double` has 53 bits of integer precision, including the hidden significand bit. Large adjacent `long` values can map to the same `double`.

**What should an API do on overflow?** The domain decides. It may reject with an exception, return an explicit failure result, saturate by documented policy, or use arbitrary precision. Silent wraparound is correct only when the contract deliberately uses fixed-width modular arithmetic.

## Quick Check

1. **[Foundation]** Why does `byte + byte` produce an `int`?
2. **[Foundation]** What is the difference between a cast and `Math.toIntExact`?
3. **[Interview Core]** Why can `long total = intA * intB;` be wrong?
4. **[Interview Core]** Under what precondition is `left + (right - left) / 2` overflow-safe?
5. **[SDE-2 Follow-up]** Give a realistic case where `BigInteger` is required and a case where it is unnecessary.

## Coding Practice

1. **[Foundation]** Print the minimum and maximum values of every integral type.
2. **[Foundation]** Write `long sum(int[] values)` with a justified result type.
3. **[Interview Core]** Implement a checked conversion from `long` to `int` without calling `Math.toIntExact`.
4. **[Interview Core]** Compute the product of two `int` values without intermediate overflow.
5. **[SDE-2 Follow-up]** Given constraints for a matrix problem, document the safe types for indexes, cell values, prefix sums, and the final answer.

## Debugging Task

**[Interview Core]** Find every numeric defect. Do not replace all types blindly.

```java
static long rectangleScore(int width, int height, int multiplier) {
    long area = width * height;
    return area * multiplier;
}
```

State the input constraints under which your repair is safe, and specify behavior if the mathematical result exceeds `long`.

## Interview Extension

**[SDE-2 Follow-up]** Design a `Money` value type whose internal representation is scaled `long`. Define currency, scale, rounding, overflow, comparison, serialization, and conversion policies. Explain when `BigDecimal` would be a better representation.
