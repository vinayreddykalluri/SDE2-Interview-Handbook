# Chapter 2: Decimal Number System for Coding Problems

Decimal is a positional base-10 system. Each position contributes a digit multiplied by a power of ten. In `4,207`, the digits contribute `4 * 1000`, `2 * 100`, `0 * 10`, and `7 * 1`.

Coding interviews rarely ask for that definition alone. They ask you to extract, transform, compare, or reconstruct digits while handling zero, negative values, and overflow.

## Learning objectives

By the end of this chapter, you should be able to:

- extract and remove decimal digits with integer arithmetic;
- count, sum, and reverse digits without converting to text;
- explain what happens to zero, negative values, and leading zeros;
- reconstruct a value while checking its range; and
- recognize when a string representation is the better contract.

## Number, digit, sign, and magnitude

A **number** is the complete mathematical value; a **digit** is one symbol used to write it. `305` is one number written with the digits `3`, `0`, and `5`. The digit `3` contributes three hundreds because its position supplies the weight. In expanded form:

```text
305  = 3 x 100 + 0 x 10 + 5 x 1
1005 = 1 x 1000 + 0 x 100 + 0 x 10 + 5 x 1
-482 = -(4 x 100 + 8 x 10 + 2 x 1)
```

The minus sign is not a digit. It changes the sign of the whole magnitude. The absolute value is the nonnegative magnitude, but Java cannot represent `abs(MIN_VALUE)` in the same signed primitive type; later algorithms therefore avoid blindly calling `Math.abs`.

Leading zeros usually have no numeric effect: `000123` and `123` denote the same integer. They do have textual meaning when width, identifiers, protocol fields, or formatting must be preserved. Once text is parsed into an integer, those leading zeros cannot be recovered.

## The decimal decomposition pattern

![Decimal place value: each digit contributes its value times a power of ten.](series/volumes/01-number-systems-and-math-foundations/assets/01-decimal-place-value.png)

![A dry run of the digit loop shows the last digit and remaining prefix changing at every step.](series/volumes/01-number-systems-and-math-foundations/assets/12-digit-extraction-loop.png)

For a nonnegative integer `n`:

```text
last digit       = n % 10
remaining prefix = n / 10
```

Java integer division truncates toward zero. Repeated division therefore removes the last decimal digit. Repeated remainder extraction visits digits from right to left.

For `n = 5,382`:

| Step | `n` | `n % 10` | `n / 10` |
|---:|---:|---:|---:|
| 1 | 5382 | 2 | 538 |
| 2 | 538 | 8 | 53 |
| 3 | 53 | 3 | 5 |
| 4 | 5 | 5 | 0 |

This loop is the foundation for digit sum, digit product, reversal, palindrome checks, Armstrong numbers, and divisibility calculations.

The smallest safe template handles zero explicitly and preserves the caller's input:

```java
long original = number;
long remaining = number;
do {
    int digit = (int) Math.abs(remaining % 10);
    // Process digit here.
    remaining /= 10;
} while (remaining != 0);
```

Using `do-while` lets zero contribute its single digit. Taking the absolute value of the one-digit remainder is safe because it is only -9 through 9, including when `number` is `Long.MIN_VALUE`.

## Recognition signals

Consider arithmetic digit traversal when the prompt mentions:

- last digit, first digit, or every digit;
- sum or product of digits;
- reverse or palindrome integer;
- replace, remove, or count digits;
- repeated decimal construction; or
- a rule based on decimal positions.

Use a `String` instead when the input may contain more digits than `long`, leading zeros are meaningful, invalid characters must be reported, or the task is primarily textual editing.

## Zero is a one-digit number

A loop written as `while (value != 0)` executes zero times for input zero. That is correct for some accumulations, such as digit sum, but not for digit count. Decimal zero has one digit.

```java
public static int countDigits(int value) {
    if (value == 0) return 1;

    int count = 0;
    int remaining = value;
    while (remaining != 0) {
        remaining /= 10;
        count++;
    }
    return count;
}
```

This method works for positive values, negative values, and `Integer.MIN_VALUE`. It never calls `Math.abs`, and division toward zero eventually reaches zero.

Time is O(d), where d is the number of decimal digits. Auxiliary space is O(1).

## Handling negative values safely

Java gives a negative remainder when the dividend is negative. For example, `-123 % 10` is `-3`. A common but unsafe response is `Math.abs(value)`: `Math.abs(Integer.MIN_VALUE)` is still negative because the positive magnitude does not fit in `int`.

One safe technique keeps the working value nonpositive. Every positive `int` can be negated, and `Integer.MIN_VALUE` is already representable:

```java
public static int sumDigits(int value) {
    if (value == 0) return 0;

    int remaining = value > 0 ? -value : value;
    int sum = 0;
    while (remaining != 0) {
        int digit = -(remaining % 10);
        sum += digit;
        remaining /= 10;
    }
    return sum;
}
```

`remaining % 10` is between `-9` and `0`, so negating that one digit is safe. The maximum digit sum of any `int` is small, so the `int sum` cannot overflow.

An alternative is to promote to `long` before taking an absolute value:

```java
long remaining = Math.abs((long) value);
```

Both approaches are valid. The negative-domain technique becomes especially useful when parsing a signed value up to `Long.MIN_VALUE`, whose positive magnitude does not fit in `long`.

## Reconstructing and reversing a number

To append digit `d` to an accumulated decimal value:

```text
next = current * 10 + d
```

Reversing repeatedly extracts a last digit and appends it to the result. The algorithm is simple; the range policy is the real interview question.

The following coding-platform-style solution returns zero when the reversed value does not fit in `int`:

```java
class Solution {
    public int reverse(int value) {
        int remaining = value;
        long reversed = 0;

        while (remaining != 0) {
            int digit = remaining % 10;
            reversed = reversed * 10 + digit;
            if (reversed < Integer.MIN_VALUE
                    || reversed > Integer.MAX_VALUE) {
                return 0;
            }
            remaining /= 10;
        }
        return (int) reversed;
    }
}
```

Why is `long` safe here? An `int` has at most ten decimal digits. The intermediate reversed magnitude is therefore bounded by a ten-digit value, well within `long`. The method checks the required `int` contract before narrowing.

In production code, returning zero may be ambiguous because zero is also a valid result. A method could instead return `OptionalInt` or throw `ArithmeticException`. Follow the problem contract and state the trade-off.

### Dry run: reverse `-1203`

| Step | `remaining` | Digit | `reversed` after append |
|---:|---:|---:|---:|
| Start | -1203 | - | 0 |
| 1 | -1203 | -3 | -3 |
| 2 | -120 | 0 | -30 |
| 3 | -12 | -2 | -302 |
| 4 | -1 | -1 | -3021 |

The sign follows naturally because Java's remainder has the dividend's sign. No separate sign restoration is needed.

## Leading zeros and lost information

An integer value does not remember how it was written. Decimal `1200` reversed numerically becomes `21`, not `0021`, because leading zeros have no place in an integer representation. Likewise, inputs `7`, `07`, and `0007` represent the same integer once parsed.

If leading zeros matter, preserve a `String`. Product codes, fixed-width identifiers, and formatted account numbers are text even when every character is a digit.

This distinction prevents a common modeling error: treating identifiers as quantities. Arithmetic is meaningful for a quantity; formatting and exact character preservation are meaningful for an identifier.

## Counting digits with logarithms

For a positive value, `floor(log10(n)) + 1` gives the decimal digit count mathematically. It is usually the wrong interview implementation for primitive integers because floating-point rounding around powers of ten requires care, zero is undefined for `log10`, and negative values need a policy.

Repeated division is O(d), which is at most ten iterations for `int` and nineteen for positive `long`. The simple integer loop is exact and fast enough. Use a string when the input is already text.

## Sum and product of digits

Digit sum starts at zero. Digit product needs an explicit zero policy:

- The product of the digits of `105` is zero because one digit is zero.
- The product for input zero is normally zero because its only digit is zero.
- Initializing product to one works only if zero is handled before a loop that would execute zero times.

For `int`, the largest possible product of ten digits is below `9^10`, which exceeds `int` but fits in `long`. Use `long` for a general digit product and document whether a leading negative sign affects the result.

## Common candidate mistakes

- Reporting zero digits for input zero.
- Calling `Math.abs` on `Integer.MIN_VALUE`.
- Forgetting that negative `% 10` produces a negative digit.
- Reversing into `int` and checking overflow only after it has already wrapped.
- Claiming leading zeros survive numeric reversal.
- Using floating point to count a handful of primitive digits.
- Returning an overflow sentinel without checking whether it conflicts with a valid result.
- Stating O(n) without defining n; here the meaningful size is digit count d.

## Interview follow-up questions

An interviewer may ask you to:

- preserve the sign or reject negative values;
- avoid `long` and check overflow before each `int` append;
- process a million-digit input supplied as a string;
- test whether a number is a palindrome without fully reversing it;
- reconstruct a number after transforming each digit; or
- return both the transformed value and whether information was lost.

The best answer begins by clarifying the contract, especially for zero, negatives, leading zeros, and overflow.

## Chapter summary

Repeated `% 10` and `/ 10` decompose a decimal integer from right to left. Handle zero before loops whose iteration count matters. Avoid `Math.abs` on the minimum signed value, and reconstruct into a wider type or check bounds before multiplication. Use strings when digit formatting or unbounded length is part of the problem.

## Quick Check

1. Why does a basic `while (value != 0)` digit counter fail for zero?
2. What are `-123 % 10` and `-123 / 10` in Java?
3. Why can `Math.abs(Integer.MIN_VALUE)` not produce a positive `int`?
4. What information is lost when a decimal string is parsed as an integer?
5. What is the time complexity of digit traversal in terms of digit count?

## Coding Practice

1. **Foundation:** Count the decimal digits of any `int`, including zero and `Integer.MIN_VALUE`.
2. **Foundation:** Return the sum and product of the digits as a small result record.
3. **Interview Core:** Reverse an integer with an explicit overflow policy.
4. **Interview Core:** Determine whether a nonnegative integer is a palindrome by reversing only half of its digits.
5. **Interview Core:** Reconstruct a number after replacing each digit `d` with `9 - d`.
6. **SDE-2 Follow-up:** Implement digit traversal for a decimal string with an optional sign and validation.

## Debugging Task

**Interview Core:** Explain every input that breaks this implementation, then redesign its contract.

```java
static int countDigits(int value) {
    value = Math.abs(value);
    int count = 0;
    while (value > 0) {
        value /= 10;
        count++;
    }
    return count;
}
```

## Interview Extension

**SDE-2 Follow-up:** Reverse a signed decimal number supplied as a string of up to one million characters. Preserve or normalize leading zeros according to a stated contract, validate the optional sign, and explain time and memory costs for both an in-place character-array approach and a streaming approach.
