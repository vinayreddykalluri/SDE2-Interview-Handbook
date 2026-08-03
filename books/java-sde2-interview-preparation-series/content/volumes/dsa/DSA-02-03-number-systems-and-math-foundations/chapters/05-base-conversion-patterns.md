# Chapter 5: Base Conversion Patterns

Base conversion has two directions: interpret text by multiplying the accumulator by its base and adding a digit; produce text by repeated division and remainders.

The interview challenge is not only the loop. It is validating the radix and digits, defining sign and leading-zero behavior, and detecting overflow before it corrupts the result.

## Learning objectives

By the end of this chapter, you should be able to:

- convert a signed `long` to bases 2 through 36 manually;
- parse bases 2 through 36 with digit and overflow validation;
- compare manual conversion with Java library methods;
- use `Character.digit`, `Character.forDigit`, and `BigInteger` deliberately; and
- explain when an interviewer expects an algorithm rather than a built-in call.

## The two conversion directions

![Base conversion map: parsing turns text into a value; formatting turns a value into text.](content/volumes/dsa/DSA-02-03-number-systems-and-math-foundations/assets/10-base-conversion-map.png)

### Base text to a value

![Positional accumulation validates each digit before multiplying the current value by the base.](content/volumes/dsa/DSA-02-03-number-systems-and-math-foundations/assets/04-base-to-decimal-positional-accumulation.png)

For digits read from left to right:

```text
value = value * base + digit
```

For hexadecimal `2F`:

```text
0 * 16 + 2  = 2
2 * 16 + 15 = 47
```

This is positional notation written as an incremental algorithm. It runs in O(k) time for k input digits and uses O(1) auxiliary space when the result is a fixed-width primitive.

### A value to base text

![Repeated division records remainders from least significant to most significant.](content/volumes/dsa/DSA-02-03-number-systems-and-math-foundations/assets/03-decimal-to-binary-repeated-division.png)

Repeatedly divide the magnitude by the base:

```text
digit         = value % base
next value    = value / base
```

The first remainder is the least significant output digit, so the collected sequence must be reversed. The number of output digits is O(log base of magnitude), and those characters are required output.

## Recognition signals

Use these patterns when the prompt asks you to:

- convert without built-in methods;
- parse a binary, octal, hexadecimal, or custom-radix string;
- validate that every symbol is legal for its base;
- support bases up to 36;
- process a value too large for a primitive; or
- explain why a conversion overflows despite containing valid digits.

Clarify the contract before coding: allowed base range, optional sign, whitespace, prefixes, letter case, leading zeros, empty input, invalid characters, and result type.

## A robust manual implementation

The following Java 21 class supports bases 2 through 36, accepts an optional leading `+` or `-`, allows leading zeros, rejects whitespace and prefixes, and detects `long` overflow. Output letters are lowercase.

```java
public final class BaseConversions {
    private BaseConversions() {}

    public static String toBase(long value, int base) {
        validateBase(base);
        if (value == 0) return "0";

        boolean negative = value < 0;
        long remaining = negative ? value : -value;
        StringBuilder reversed = new StringBuilder();

        while (remaining != 0) {
            int digit = (int) -(remaining % base);
            reversed.append(Character.forDigit(digit, base));
            remaining /= base;
        }
        if (negative) reversed.append('-');
        return reversed.reverse().toString();
    }

    public static long fromBase(String text, int base) {
        validateBase(base);
        if (text == null || text.isEmpty()) {
            throw new IllegalArgumentException("number must be nonempty");
        }

        int index = 0;
        boolean negative = false;
        char first = text.charAt(0);
        if (first == '+' || first == '-') {
            negative = first == '-';
            index++;
        }
        if (index == text.length()) {
            throw new IllegalArgumentException("sign requires digits");
        }

        long limit = negative ? Long.MIN_VALUE : -Long.MAX_VALUE;
        long multiplyLimit = limit / base;
        long result = 0;

        while (index < text.length()) {
            char current = text.charAt(index);
            int digit = asciiDigit(current);
            if (digit < 0 || digit >= base) {
                throw new IllegalArgumentException(
                        "invalid base-" + base + " digit at index " + index);
            }
            if (result < multiplyLimit) {
                throw new ArithmeticException("value exceeds long");
            }
            result *= base;
            if (result < limit + digit) {
                throw new ArithmeticException("value exceeds long");
            }
            result -= digit;
            index++;
        }
        return negative ? result : -result;
    }

    private static int asciiDigit(char value) {
        if (value >= '0' && value <= '9') return value - '0';
        if (value >= 'a' && value <= 'z') return value - 'a' + 10;
        if (value >= 'A' && value <= 'Z') return value - 'A' + 10;
        return -1;
    }

    private static void validateBase(int base) {
        if (base < Character.MIN_RADIX || base > Character.MAX_RADIX) {
            throw new IllegalArgumentException("base must be 2 through 36");
        }
    }

    public static void main(String[] args) {
        System.out.println(toBase(255, 16));          // ff
        System.out.println(fromBase("FF", 16));      // 255
        System.out.println(toBase(Long.MIN_VALUE, 2));
        System.out.println(fromBase("-8000000000000000", 16));
    }
}
```

## Why the implementation keeps values negative

`Long.MIN_VALUE` is `-2^63`; its positive magnitude is not representable by `long`. Calling `Math.abs(Long.MIN_VALUE)` does not solve the problem.

The formatter converts positive input into a negative working value and leaves negative input unchanged. Every `long` magnitude can be represented in that nonpositive domain. A negative remainder has magnitude less than the base, so negating only the small remainder safely produces the next digit.

The parser uses the same idea. It accumulates a nonpositive result and subtracts each digit. Its `limit` is `Long.MIN_VALUE` for negative input and `-Long.MAX_VALUE` for positive input. This allows it to accept both endpoints without requiring an impossible positive magnitude for `Long.MIN_VALUE`.

## Overflow checks, step by step

Before `result *= base`, require `result >= limit / base`. Because the values are nonpositive, a smaller result is closer to negative overflow.

After multiplication but before subtracting digit `d`, require:

```text
result >= limit + d
```

Otherwise `result - d` would fall below the allowed limit. Checking after the arithmetic would be too late because ordinary Java integer overflow silently wraps.

### Dry run: parse hexadecimal `-1F`

The sign selects a limit of `Long.MIN_VALUE`; accumulation starts at zero.

| Digit | Previous result | Multiply by 16 | Subtract digit | Result |
|---|---:|---:|---:|---:|
| 1 | 0 | 0 | `0 - 1` | -1 |
| F = 15 | -1 | -16 | `-16 - 15` | -31 |

Because the input is negative, return the accumulated `-31` directly.

### Dry run: format decimal 31 in base 16

The positive input is represented internally as `-31`.

| Remaining | Negative remainder | Output digit | Next remaining |
|---:|---:|---|---:|
| -31 | -15 | f | -1 |
| -1 | -1 | 1 | 0 |

Collected order is `f1`; reversing produces `1f`.

## Java library methods

Use the standard library when manual conversion is not the skill being tested.

| Operation | Java API |
|---|---|
| Parse `int` | `Integer.parseInt(text, base)` |
| Parse `long` | `Long.parseLong(text, base)` |
| Format `int` | `Integer.toString(value, base)` |
| Format `long` | `Long.toString(value, base)` |
| Read a digit | `Character.digit(character, base)` |
| Produce a digit | `Character.forDigit(digit, base)` |
| Parse beyond `long` | `new BigInteger(text, base)` |
| Format beyond `long` | `bigInteger.toString(base)` |

`Character.digit` conveniently accepts uppercase and lowercase letters and also recognizes some non-ASCII numeric characters. The manual class deliberately enforces ASCII digits because its input contract is a coding-platform number. If Unicode digit acceptance is intended, use `Character.digit` and test that contract explicitly.

`Character.forDigit` returns lowercase letters. Validate base and digit before calling it; an invalid input can produce the null character.

## BigInteger for unbounded inputs

`BigInteger` is appropriate when the full integer result is required and input can exceed `long`:

```java
import java.math.BigInteger;

BigInteger value = new BigInteger("123456789abcdef", 16);
String binary = value.toString(2);
```

Its range is limited by available memory, not 64 bits. Arithmetic cost grows with the number of machine words, and results are immutable objects. Do not replace every primitive with `BigInteger`; choose it when constraints require arbitrary precision.

If the problem asks only whether a huge decimal string is divisible by 9 or asks for its remainder, digit-by-digit modular accumulation is more direct and uses constant working space. The full `BigInteger` value is unnecessary.

## Manual algorithm or built-in?

Interviewers usually expect manual conversion when the question explicitly says "without built-ins," asks for validation or overflow reasoning, uses streaming input, or is designed to test positional accumulation. State the standard method first so the interviewer knows you understand Java, then explain why you are implementing the loop.

In production, prefer library parsing unless a different contract is necessary. Standard methods are reviewed, optimized, and familiar. Wrap `NumberFormatException` only when adding meaningful domain context, and preserve it as the cause.

## Common candidate mistakes

- Failing to validate that base is between 2 and 36.
- Accepting digit `8` in base 8 or letter `G` in base 16.
- Treating an optional sign as a digit or accepting a sign with no digits.
- Multiplying before checking overflow.
- Calling `Math.abs` on `Long.MIN_VALUE`.
- Forgetting to reverse remainders during formatting.
- Returning an empty string for zero.
- Assuming `parseLong(text, base)` accepts `0x` or `0b` prefixes.
- Trimming whitespace silently when the contract says invalid input.
- Using `BigInteger` when only a small remainder is required.

## Interview follow-up questions

Expect variations involving custom digit alphabets, bases above 36, unsigned 64-bit patterns, separators, a streaming reader, a huge input modulo m, fixed output width, or conversion directly between two non-decimal bases. State whether an intermediate primitive or `BigInteger` is allowed before adapting the algorithm.

## Chapter summary

Parsing uses `accumulator * base + digit`; formatting uses repeated division and reversed remainders. Robust Java code validates the radix, sign, digits, and range before arithmetic. Negative-domain accumulation handles `Long.MIN_VALUE` correctly. Prefer standard APIs for ordinary application code and manual algorithms when the interview tests the underlying reasoning.

## Quick Check

1. Why does base-to-value conversion multiply the current result before adding a digit?
2. Why are generated remainders reversed when formatting?
3. What failure does negative-domain accumulation avoid?
4. Does `Long.parseLong("0xff", 16)` accept the prefix?
5. When is `BigInteger` useful, and when is it unnecessary?

## Coding Practice

1. **Foundation:** Convert a nonnegative `int` to bases 2 through 16 manually.
2. **Foundation:** Parse a binary string with digit validation.
3. **Interview Core:** Parse bases 2 through 36 into `long` with overflow checks.
4. **Interview Core:** Convert any signed `long`, including `Long.MIN_VALUE`, to a requested base.
5. **Interview Core:** Convert a base-2 through base-36 string to another base using `BigInteger`.
6. **SDE-2 Follow-up:** Compute a million-digit base-b value modulo a positive `int` without constructing the full value.
7. **Challenge:** Support a caller-supplied unique ASCII alphabet containing between 2 and 62 symbols.

## Debugging Task

**Interview Core:** Identify the zero, sign, digit, radix, and overflow defects in this parser.

```java
static long parse(String text, int base) {
    long value = 0;
    for (int index = 0; index < text.length(); index++) {
        int digit = text.charAt(index) - '0';
        value = value * base + digit;
    }
    return value;
}
```

## Interview Extension

**SDE-2 Follow-up:** Design a streaming base-conversion service for inputs too large to fit in memory. Compare three contracts: returning the full value, returning the value modulo m, and translating to another base. Explain which contracts can operate in one pass with bounded memory and which require arbitrary-precision state or external storage.
