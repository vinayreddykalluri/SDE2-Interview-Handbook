# Chapter 3: Binary Number System

Binary is a positional base-2 system. It uses only digits `0` and `1`, and each position represents a power of two. Binary matters in interviews because Java integers have fixed widths, bit operations act on those representations, and powers of two appear throughout complexity, trees, memory sizing, and hashing.

This chapter builds the representation model. It does not attempt to cover the full set of bit-manipulation tricks.

## Learning objectives

By the end of this chapter, you should be able to:

- evaluate binary place values;
- convert nonnegative values between decimal and binary manually;
- validate and accumulate a binary string with overflow checks;
- explain leading zeros and Java's fixed-width signed integers; and
- describe how this foundation supports later bit manipulation.

## Bits and binary place values

![Binary place value: each bit contributes zero or one times a power of two.](content/volumes/01-number-systems-and-math-foundations/assets/02-binary-place-value.png)

A bit is one binary digit. Starting at the rightmost position, place values are `2^0`, `2^1`, `2^2`, and so on.

```text
binary:       1    1    0    1
place value:  8    4    2    1
contribution: 8 +  4 +  0 +  1 = 13
```

The same positional rule works in every base. Reading digits from left to right, the accumulated value can be updated as:

```text
next = current * 2 + bit
```

This Horner-style accumulation avoids calculating each power separately and generalizes directly to other bases.

## Recognition signals

Binary knowledge is relevant when the prompt includes:

- binary strings or conversion;
- powers of two;
- flags, subsets, masks, or packed fields;
- signed shifts or unsigned shifts;
- highest or lowest set bit;
- memory widths or integer boundaries; or
- a later bit-manipulation operation that is difficult to explain in decimal.

First clarify whether the input is a nonnegative mathematical value, a signed Java `int`, or a raw fixed-width bit pattern. The same characters can be interpreted differently under different contracts.

## Binary to decimal by positional accumulation

The following method accepts a nonempty string of `0` and `1` characters and returns a nonnegative `long`. It rejects invalid digits and values larger than `Long.MAX_VALUE`.

```java
public final class BinaryConversions {
    private BinaryConversions() {}

    public static long binaryToLong(String binary) {
        if (binary == null || binary.isEmpty()) {
            throw new IllegalArgumentException("binary must be nonempty");
        }

        long value = 0;
        for (int index = 0; index < binary.length(); index++) {
            char current = binary.charAt(index);
            if (current != '0' && current != '1') {
                throw new IllegalArgumentException(
                        "invalid binary digit at index " + index);
            }
            int bit = current - '0';
            if (value > (Long.MAX_VALUE - bit) / 2) {
                throw new ArithmeticException("binary value exceeds long");
            }
            value = value * 2 + bit;
        }
        return value;
    }

    public static String nonnegativeLongToBinary(long value) {
        if (value < 0) {
            throw new IllegalArgumentException("value must be nonnegative");
        }
        if (value == 0) return "0";

        StringBuilder reversed = new StringBuilder();
        long remaining = value;
        while (remaining > 0) {
            reversed.append((char) ('0' + remaining % 2));
            remaining /= 2;
        }
        return reversed.reverse().toString();
    }

    public static void main(String[] args) {
        System.out.println(binaryToLong("001101"));       // 13
        System.out.println(nonnegativeLongToBinary(13));  // 1101
    }
}
```

The overflow check is performed before multiplication. For a next bit `b`, accumulation is safe only when `value <= (Long.MAX_VALUE - b) / 2`.

Time is O(k) for k input digits. `binaryToLong` uses O(1) auxiliary space. Decimal-to-binary conversion creates O(log value) output characters, which are required output rather than avoidable working space.

### Dry run: binary `001101`

| Character | Previous value | `previous * 2 + bit` | New value |
|---|---:|---:|---:|
| 0 | 0 | 0 | 0 |
| 0 | 0 | 0 | 0 |
| 1 | 0 | 1 | 1 |
| 1 | 1 | 3 | 3 |
| 0 | 3 | 6 | 6 |
| 1 | 6 | 13 | 13 |

Leading zeros do not change the mathematical value. They may still be meaningful when a contract requires a fixed-width bit string.

## Decimal to binary by repeated division

For a nonnegative value, repeated division by two produces remainders from least significant to most significant. For decimal 13:

| Remaining | Remainder | Next remaining |
|---:|---:|---:|
| 13 | 1 | 6 |
| 6 | 0 | 3 |
| 3 | 1 | 1 |
| 1 | 1 | 0 |

The remainders arrive as `1, 0, 1, 1`; reversing them produces `1101`.

The zero case must return `"0"` before the loop. Without it, the result would be an empty string.

## Java built-ins

Java provides conversion APIs when manual conversion is not the point of the interview:

```java
String binary = Long.toString(13L, 2);       // "1101"
long value = Long.parseLong("1101", 2);     // 13
String intBits = Integer.toBinaryString(-5); // 32-bit pattern
```

`Long.parseLong` interprets a signed mathematical value in the supplied radix and throws `NumberFormatException` for invalid text or overflow. `Integer.toBinaryString` is different for negative values: it returns the unsigned textual form of the 32-bit two's-complement representation, with no leading sign.

Use built-ins in production unless manual processing provides required validation, streaming, or behavior. In an interview, state the library option, then implement manually when the interviewer is testing the algorithm.

## Fixed width and leading zeros

The mathematical binary form of 5 is `101`. A byte-sized display could be `00000101`; an `int`-sized display uses 32 bits. Leading zeros change formatting, not value.

```java
public static String fixedWidthIntBits(int value) {
    String raw = Integer.toBinaryString(value);
    return "0".repeat(32 - raw.length()) + raw;
}
```

For nonnegative values, `raw` may be shorter than 32 characters and is padded. For negative values, `Integer.toBinaryString` already returns 32 characters.

A leading-zero contract must state width and what happens when the value needs more bits. Never silently truncate high bits unless the problem explicitly models a fixed-width wrap.

## Signed integer intuition and two's complement

Java `int` has 32 bits and `long` has 64 bits. The highest bit contributes to the signed interpretation. Two's complement gives one zero and makes ordinary fixed-width addition behave consistently across positive and negative values.

To form the 8-bit pattern for `-5` as intuition:

```text
+5         00000101
invert     11111010
add one    11111011
```

The same low-bit idea applies to Java's 32-bit `int`; the example uses eight bits only for readability. Do not interpret `11111011` as Java `int -5` without specifying that it is an 8-bit signed pattern.

The range asymmetry follows from the representation. A 32-bit `int` ranges from `-2^31` through `2^31 - 1`. There is no positive `2^31`, which is why `Math.abs(Integer.MIN_VALUE)` cannot return a positive `int`.

## Bridge to bit manipulation

Bit operators act on fixed-width binary representations:

- `&` keeps positions set in both operands.
- `|` keeps positions set in either operand.
- `^` keeps positions that differ.
- `~` flips every bit in the fixed width.
- `<<` shifts left and fills low positions with zero.
- `>>` shifts right while extending the sign bit.
- `>>>` shifts right while filling high positions with zero.

These definitions are enough to understand later examples, but masks, bit counting, subset enumeration, and bitwise problem patterns belong in the dedicated Bit Manipulation volume.

## Common candidate mistakes

- Accepting characters other than `0` and `1`.
- Treating an empty string as zero without a stated contract.
- Calculating powers with floating point instead of accumulating exactly.
- Checking overflow after a `long` multiplication has already wrapped.
- Forgetting the special decimal-to-binary case for zero.
- Assuming `Integer.toBinaryString(-5)` returns `"-101"`.
- Confusing a mathematical binary string with a signed fixed-width pattern.
- Discarding leading zeros when the problem requires a fixed width.

## Interview follow-up questions

Be prepared to support inputs longer than 63 value bits, return the result modulo a constant, validate separators, parse a signed `-101`, preserve a requested width, or convert a raw 32-bit string into an `int`. Each variation changes the contract before it changes the loop.

## Chapter summary

Binary uses powers of two. Accumulate a binary string with `value = value * 2 + bit`, checking range before arithmetic. Convert a nonnegative value by collecting repeated remainders and reversing them. Distinguish minimal mathematical notation from a fixed-width signed Java representation. This model is the prerequisite for bit manipulation, not a replacement for that later topic.

## Quick Check

1. What value does binary `10110` represent?
2. Why does left-to-right accumulation avoid explicit powers of two?
3. What does `Integer.toBinaryString` return for a negative value conceptually?
4. Why is `Integer.MIN_VALUE` one magnitude larger than `Integer.MAX_VALUE`?
5. What is the difference between `101` and `00000101`?

## Coding Practice

1. **Foundation:** Convert a validated nonnegative binary string to `long`.
2. **Foundation:** Convert a nonnegative `long` to binary without built-in conversion methods.
3. **Interview Core:** Return a fixed-width binary string and reject values that do not fit the requested unsigned width.
4. **Interview Core:** Validate a binary string containing optional underscore separators.
5. **SDE-2 Follow-up:** Compute a huge binary string modulo a positive `int` without parsing the whole value.
6. **Challenge:** Parse exactly 32 raw bits into the corresponding signed Java `int`.

## Debugging Task

**Interview Core:** Find the invalid-input, zero, ordering, and range defects in this method.

```java
static String toBinary(int value) {
    StringBuilder result = new StringBuilder();
    while (value > 0) {
        result.append(value % 2);
        value /= 2;
    }
    return result.toString();
}
```

## Interview Extension

**SDE-2 Follow-up:** Design an API that distinguishes three operations: formatting a signed `int` mathematically, displaying its exact 32-bit pattern, and parsing a raw 32-bit pattern. State how each operation handles a sign, leading zeros, invalid characters, and overflow.
