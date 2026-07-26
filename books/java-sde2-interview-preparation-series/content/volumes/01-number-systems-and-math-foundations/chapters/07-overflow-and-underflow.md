# Chapter 7: Overflow and Underflow in Interviews

An algorithm can be logically correct over mathematical integers and still fail in Java because `int` and `long` have finite ranges. Java ordinarily wraps integral results instead of throwing. Interviewers use this fact to test whether you reason about intermediate values, not only final values.

## Overflow, underflow, and wraparound

![Signed integer overflow wraps across the fixed-width boundary unless code checks the operation.](content/volumes/01-number-systems-and-math-foundations/assets/06-integer-overflow-wraparound.png)

Integral overflow occurs when a mathematical result is greater than a type's maximum. Integral underflow is the corresponding result below its minimum. Java does not treat these as separate runtime events for ordinary `+`, `-`, `*`, unary negation, or increment. The low bits are retained according to fixed-width two's-complement arithmetic.

```java
System.out.println(Integer.MAX_VALUE + 1); // -2147483648
System.out.println(Integer.MIN_VALUE - 1); // 2147483647
```

No exception is thrown. That behavior can corrupt a count, turn a positive length negative, reverse a comparator result, or make a loop terminate incorrectly.

Floating-point underflow is different: a tiny nonzero magnitude can become a subnormal value or zero. That is an approximate-numeric concern and is not the usual meaning of an integer-overflow interview question.

## Recognizing risky operations

Check arithmetic when any of these signals appear:

- multiplication of two unconstrained values;
- accumulated sums over many elements;
- negation or absolute value at a minimum boundary;
- decimal digit reconstruction such as `result * 10 + digit`;
- subtraction inside a comparator;
- midpoint, capacity, or buffer-size formulas;
- conversion from `long` to `int`; or
- a sentinel near `Integer.MAX_VALUE` that is later incremented.

Read constraints before choosing a repair. Widening from `int` to `long` is enough only when the widest possible mathematical result fits in `long`.

## Exact arithmetic helpers

Java provides checked operations:

```java
long total = Math.addExact(left, right);
long difference = Math.subtractExact(left, right);
long product = Math.multiplyExact(left, right);
int narrowed = Math.toIntExact(total);
```

These methods return the ordinary result when it fits and throw `ArithmeticException` otherwise. Related helpers include `incrementExact`, `decrementExact`, `negateExact`, and `absExact`.

Exact helpers are useful when overflow means invalid data or a violated invariant. They are less appropriate when an interview method must return a specific sentinel, when arbitrary precision is required, or when the contract intentionally uses wraparound.

## Detecting overflow before addition and multiplication

For manual checks on `long`, compare against the boundary before evaluating the risky operation:

```java
public final class CheckedLongMath {
    private CheckedLongMath() {}

    static long addChecked(long left, long right) {
        if (right > 0 && left > Long.MAX_VALUE - right) {
            throw new ArithmeticException("long addition overflow");
        }
        if (right < 0 && left < Long.MIN_VALUE - right) {
            throw new ArithmeticException("long addition underflow");
        }
        return left + right;
    }

    static boolean multiplicationWouldOverflow(long left, long right) {
        if (left == 0 || right == 0) return false;

        if (left > 0) {
            if (right > 0) return left > Long.MAX_VALUE / right;
            return right < Long.MIN_VALUE / left;
        }

        if (right > 0) return left < Long.MIN_VALUE / right;
        return right < Long.MAX_VALUE / left;
    }

    static long multiplyChecked(long left, long right) {
        if (multiplicationWouldOverflow(left, right)) {
            throw new ArithmeticException("long multiplication overflow");
        }
        return left * right;
    }

    public static void main(String[] args) {
        System.out.println(addChecked(40, 2));
        System.out.println(multiplyChecked(-7, 6));
        System.out.println(multiplicationWouldOverflow(Long.MIN_VALUE, -1));
    }
}
```

The division expressions are arranged so that the checks do not rely on the unrepresentable mathematical quotient of `Long.MIN_VALUE / -1`. Java returns `Long.MIN_VALUE` for that division because positive 2^63 does not fit in `long`. Each method uses O(1) time and O(1) auxiliary space.

In production code, `Math.addExact` and `Math.multiplyExact` are usually clearer. Manual checks remain valuable in interviews because they expose boundary reasoning and support contracts that return a boolean or sentinel instead of throwing.

## Problem: reverse an integer safely

**Problem.** Reverse the decimal digits of a signed 32-bit integer. Return zero if the reversed value would be outside the `int` range.

**Interview relevance.** This tests digit extraction, Java's negative remainder behavior, and checking overflow before multiplication.

**Initial approach.** Build `reversed = reversed * 10 + digit`, then inspect the result.

**Problem with the initial approach.** Once `int` arithmetic wraps, comparing the damaged result with the boundary cannot reliably recover the mathematical value. Widening to `long` works for this exact problem, but the interviewer may ask for an `int`-only solution.

**Optimal approach.** Before multiplying by ten, compare the current result with `MAX_VALUE / 10` and `MIN_VALUE / 10`. At the equal boundary, compare the final digit with 7 or -8.

```java
class Solution {
    public int reverse(int value) {
        int remaining = value;
        int reversed = 0;

        while (remaining != 0) {
            int digit = remaining % 10;
            remaining /= 10;

            if (reversed > Integer.MAX_VALUE / 10
                    || (reversed == Integer.MAX_VALUE / 10 && digit > 7)) {
                return 0;
            }
            if (reversed < Integer.MIN_VALUE / 10
                    || (reversed == Integer.MIN_VALUE / 10 && digit < -8)) {
                return 0;
            }

            reversed = reversed * 10 + digit;
        }
        return reversed;
    }
}
```

The method never calls `Math.abs`, so `Integer.MIN_VALUE` is handled without a special magnitude conversion. Java division truncates toward zero, and `% 10` gives a negative digit for a negative dividend. The same boundary logic therefore works on both signs.

**Dry run: `value = -120`.**

| Iteration | Remaining before | Digit | Reversed after |
|---:|---:|---:|---:|
| 1 | -120 | 0 | 0 |
| 2 | -12 | -2 | -2 |
| 3 | -1 | -1 | -21 |

The answer is `-21`; leading zeros do not exist in an integer result.

For a ten-digit input whose reverse exceeds the boundary, the method returns zero before the unsafe multiply-add. Time is O(d), where d is the number of decimal digits, and auxiliary space is O(1).

## Safe midpoint and comparator patterns

For a nonnegative ordered binary-search interval, use:

```java
int mid = left + (right - left) / 2;
```

Do not use `(left + right) / 2`, because the sum can overflow even when both indexes are valid.

For ordering, this comparator is incorrect:

```java
(a, b) -> a - b
```

If `a` is large positive and `b` is large negative, subtraction can wrap and report the wrong sign. Use:

```java
(a, b) -> Integer.compare(a, b)
```

For object fields, use `Comparator.comparingInt`, `comparingLong`, and explicit tie-breakers.

## Absolute-value traps

The positive magnitude of `Integer.MIN_VALUE` is 2^31, which does not fit in `int`. Therefore:

```java
System.out.println(Math.abs(Integer.MIN_VALUE)); // still negative
```

Widen first when an `int` magnitude may reach 2^31:

```java
long magnitude = Math.abs((long) intValue);
```

There is no wider primitive to rescue `Long.MIN_VALUE` in the same way. Use `Math.absExact` to reject it or `BigInteger.valueOf(longValue).abs()` to represent its magnitude exactly.

## Complexity and numeric safety

Boundary checks do not change the asymptotic complexity of a fixed-width arithmetic operation: checked addition and multiplication are O(1). `BigInteger` operations are not O(1) with respect to the number of stored bits. For a digit-processing loop, overflow checks inside each iteration preserve O(d) time and O(1) auxiliary space.

## Edge cases and common mistakes

- Checking after an overflowing operation is often too late.
- Casting the result instead of an operand does not prevent narrow evaluation.
- `-Integer.MIN_VALUE` and `Math.abs(Integer.MIN_VALUE)` overflow.
- Using a maximum sentinel and then adding one can wrap. Skip impossible states or use a bounded sentinel.
- `Math.addExact` throws; it does not return a special value.
- A safe `int * int -> long` pattern does not make `long * long -> long` safe.
- Comparator subtraction can violate antisymmetry and transitivity.
- Midpoint formulas need a documented interval and rounding convention.
- Returning zero on overflow is valid only if the problem contract says so.

## Interview follow-ups

**Can division detect multiplication overflow after the fact?** For most nonzero pairs, checking `product / right == left` works, but multiplication has already wrapped and `MIN_VALUE / -1` cannot represent its mathematical quotient. A precheck or `Math.multiplyExact` is clearer.

**Should a service crash on `ArithmeticException`?** Usually the exception should be translated at the correct boundary into a domain failure, rejected request, or alert. Catching it and continuing with a fabricated value hides corruption.

**When is wraparound intentional?** Hash mixing, checksums, and some low-level algorithms intentionally use fixed-width modular arithmetic. State that contract explicitly so reviewers do not mistake a feature for a bug.

## Quick Check

1. **[Foundation]** What happens when ordinary `int` addition exceeds `Integer.MAX_VALUE`?
2. **[Interview Core]** Why must overflow be checked before `reversed * 10 + digit`?
3. **[Interview Core]** Why is `a - b` unsafe in a comparator?
4. **[Interview Core]** Why does `Math.abs(Integer.MIN_VALUE)` remain negative?
5. **[SDE-2 Follow-up]** Name two domain policies other than throwing when a result cannot be represented.

## Coding Practice

1. **[Foundation]** Demonstrate positive overflow and negative underflow for `byte`, `int`, and `long`.
2. **[Interview Core]** Implement checked `long` subtraction with boundary tests.
3. **[Interview Core]** Reverse an integer and return `OptionalInt.empty()` on overflow.
4. **[Interview Core]** Repair a subtraction-based comparator and add boundary tests.
5. **[SDE-2 Follow-up]** Implement saturating addition with a clearly documented contract.
6. **[Challenge]** Compute the average of two arbitrary `long` values without overflowing their sum and document the rounding rule.

## Debugging Task

**[Interview Core]** Explain why this test can pass for small values and fail in production:

```java
static int compareByScore(int leftScore, int rightScore) {
    return rightScore - leftScore;
}
```

Repair it, preserve descending order, and add a deterministic tie-breaker for a candidate ID.

## Interview Extension

**[SDE-2 Follow-up]** Design an API that multiplies two user-supplied quantities. Compare four contracts: wrapped `long`, checked `long`, `OptionalLong`, and `BigInteger`. Discuss observability, caller misuse, serialization, and performance.
