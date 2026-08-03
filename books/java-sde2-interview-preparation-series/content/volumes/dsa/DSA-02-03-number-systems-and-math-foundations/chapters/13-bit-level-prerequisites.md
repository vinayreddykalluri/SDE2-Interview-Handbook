# Chapter 13: Bit-Level Prerequisites

Bit manipulation is easiest when the number-system model is already stable. This chapter builds only the bridge required for mainstream DSA interviews: binary representation, signed Java integers, parity, shifts, small masks, and the power-of-two test. It deliberately stops before advanced bit tricks, subset masks, bitwise tries, range XOR, and bit dynamic programming. Those belong in the dedicated Bit Manipulation mini-book later in the series.

## 13.1 Learning objectives

After this chapter, you should be able to:

- read a binary representation as powers of two;
- distinguish a mathematical integer from its fixed-width Java representation;
- explain two's complement at an interview-ready level;
- test even and odd values with the low bit;
- explain left shift, signed right shift, and unsigned right shift;
- predict Java's shift-distance masking;
- build and apply a one-bit mask;
- test whether a positive integer is a power of two;
- identify overflow and sign-extension risks.

## 13.2 Binary representation revisited

An unsigned binary value assigns a power of two to each position:

~~~text
bit index:      5   4   3   2   1   0
place value:   32  16   8   4   2   1
bits:           1   0   1   1   0   1

value = 32 + 8 + 4 + 1 = 45
~~~

The rightmost bit is bit 0, also called the least significant bit. Moving one position left doubles the place value. That is why shifts and powers of two are connected.

Java's byte, short, int, and long types are signed fixed-width two's-complement values:

| Type | Width | Shift operations actually use |
|---|---:|---|
| byte | 8 bits | Promoted to int before shifting |
| short | 16 bits | Promoted to int before shifting |
| int | 32 bits | 32-bit int |
| long | 64 bits | 64-bit long |

char is an unsigned 16-bit code unit, but arithmetic and shifts promote it to int as well.

## 13.3 Two's complement intuition

![An 8-bit example visualizes two's-complement negation and the asymmetric signed range.](content/volumes/dsa/DSA-02-03-number-systems-and-math-foundations/assets/13-twos-complement-intuition.png)

For an int, bit 31 is the sign position. Nonnegative values use the familiar binary representation. A negative value can be understood by starting from its positive magnitude, inverting all bits, and adding one.

Using eight bits only as a small illustration:

~~~text
 5  = 0000 0101
invert
     1111 1010
add 1
-5  = 1111 1011
~~~

The actual Java int representation uses 32 bits, not eight.

Two's complement has one more negative value than positive value:

~~~text
int range: -2^31 through 2^31 - 1
~~~

That asymmetry explains several interview traps:

- -Integer.MIN_VALUE is still Integer.MIN_VALUE because the positive result does not fit;
- Math.abs(Integer.MIN_VALUE) is negative;
- shifting into the sign bit can turn a positive int into a negative one;
- a power-of-two test must reject nonpositive values.

You do not need to perform manual inversion for ordinary code. The model is useful for predicting fixed-width behavior.

## 13.4 Even and odd values

Every even integer is divisible by two, so its 2^0 bit is zero. Every odd integer has that bit set.

~~~java
static boolean isEven(int value) {
    return (value & 1) == 0;
}

static boolean isOdd(int value) {
    return (value & 1) != 0;
}
~~~

This works for negative values too. For example, -3 has a low bit of one in two's complement, while -4 has a low bit of zero.

Modulo is equally clear:

~~~java
static boolean isEvenWithModulo(int value) {
    return value % 2 == 0;
}
~~~

In an interview, prefer the version that makes the surrounding algorithm easiest to read. The bit test is especially natural when the problem already uses masks or powers of two. Do not claim that it is meaningfully faster without measurement and context.

## 13.5 Left shift

For a value whose mathematical result fits, shifting left by k positions multiplies by 2^k:

~~~text
3       = 0000 0011
3 << 2  = 0000 1100 = 12
~~~

In Java:

~~~java
int result = 3 << 2; // 12
~~~

The important qualification is "whose result fits." Bits shifted beyond the fixed width are discarded. No overflow exception is thrown.

~~~java
int wrapped = 1 << 31; // Integer.MIN_VALUE
long wide = 1L << 31;  // 2147483648
~~~

The suffix L changes both the operation width and the result type. Assigning an already-overflowed int expression to long does not repair it.

### Shift distance masking

Java does not reject every large shift distance. For int, only the low five bits of the distance are used, equivalent to distance & 31. For long, only the low six bits are used, equivalent to distance & 63.

~~~java
int sameAsOne = 1 << 32;   // actually 1 << 0
long sameAsOneLong = 1L << 64; // actually 1L << 0
~~~

A negative shift count is masked too:

~~~java
int signBit = 1 << -1; // distance -1 & 31 is 31
~~~

This is defined Java behavior, not a request to write obscure code. Validate user-provided bit indices explicitly.

## 13.6 Signed right shift

The operator >> shifts bits right and copies the sign bit into newly opened high positions. This is called sign extension.

~~~text
 12 >> 2 = 3
-12 >> 2 = -3
~~~

For nonnegative values, right shifting by k is equivalent to integer division by 2^k. For negative odd values, be careful: signed right shift rounds toward negative infinity, while Java integer division rounds toward zero.

~~~java
System.out.println(-3 >> 1); // -2
System.out.println(-3 / 2);  // -1
~~~

Therefore, do not replace division with shifting in signed arithmetic unless the domain and rounding semantics are correct.

## 13.7 Unsigned right shift

The operator >>> shifts right and fills new high positions with zeros. It treats the bit pattern as if it were unsigned for the shift operation.

~~~java
System.out.println(-1 >> 1);  // -1
System.out.println(-1 >>> 1); // 2147483647
~~~

The result type remains int. Java has unsigned helper methods such as Integer.compareUnsigned and Integer.toUnsignedString, but >>> does not change int into a wider mathematical type automatically.

Unsigned right shift is useful for:

- inspecting an int's raw bit pattern;
- extracting fields from packed binary data;
- implementing selected hashing and encoding operations;
- moving high bits without sign extension.

For a negative int x, Integer.toUnsignedLong(x) converts the same 32-bit pattern to its nonnegative long interpretation.

## 13.8 Basic one-bit masks

A mask selects one or more bit positions. To address bit k in an int:

~~~java
int mask = 1 << k;
~~~

Assume 0 <= k < 32.

### Test a bit

~~~java
boolean set = (value & mask) != 0;
~~~

AND keeps a one only where both operands have a one.

### Set a bit

~~~java
int changed = value | mask;
~~~

OR forces the selected position to one.

### Clear a bit

~~~java
int changed = value & ~mask;
~~~

The complement mask has zero at the selected position and ones elsewhere.

### Toggle a bit

~~~java
int changed = value ^ mask;
~~~

XOR flips the selected position.

These operations are small building blocks, not complete problem-solving patterns by themselves. Always state the bit-index contract and choose 1L << k for long masks.

## 13.9 Power-of-two test

A positive power of two contains exactly one set bit:

~~~text
1   = 0000 0001
2   = 0000 0010
4   = 0000 0100
8   = 0000 1000
16  = 0001 0000
~~~

Subtracting one clears that bit and makes every lower bit one:

~~~text
8       = 0000 1000
8 - 1   = 0000 0111
AND       0000 0000
~~~

Therefore:

~~~java
static boolean isPowerOfTwo(int value) {
    return value > 0 && (value & (value - 1)) == 0;
}
~~~

The value > 0 guard is mandatory. Zero would otherwise pass because 0 & -1 is 0. Integer.MIN_VALUE also has a single set bit, but it is not the positive mathematical power 2^31 representable as an int.

**Time complexity:** O(1) for fixed-width int.

**Space complexity:** O(1).

An alternative repeatedly divides a positive value by two. It is O(log value) and may be easier to derive before introducing the bit identity.

## 13.10 Runnable Java demonstration

~~~java
public final class BitPrerequisitesDemo {
    private BitPrerequisitesDemo() {
    }

    public static boolean isEven(int value) {
        return (value & 1) == 0;
    }

    public static boolean isPowerOfTwo(int value) {
        return value > 0 && (value & (value - 1)) == 0;
    }

    public static boolean isBitSet(int value, int bitIndex) {
        requireIntBitIndex(bitIndex);
        int mask = 1 << bitIndex;
        return (value & mask) != 0;
    }

    public static int setBit(int value, int bitIndex) {
        requireIntBitIndex(bitIndex);
        return value | (1 << bitIndex);
    }

    public static int clearBit(int value, int bitIndex) {
        requireIntBitIndex(bitIndex);
        return value & ~(1 << bitIndex);
    }

    public static int toggleBit(int value, int bitIndex) {
        requireIntBitIndex(bitIndex);
        return value ^ (1 << bitIndex);
    }

    private static void requireIntBitIndex(int bitIndex) {
        if (bitIndex < 0 || bitIndex >= Integer.SIZE) {
            throw new IllegalArgumentException(
                    "bitIndex must be between 0 and 31");
        }
    }

    public static void main(String[] args) {
        int value = 10; // binary 1010
        System.out.println(isEven(value));          // true
        System.out.println(isBitSet(value, 1));     // true
        System.out.println(setBit(value, 0));       // 11
        System.out.println(clearBit(value, 3));     // 2
        System.out.println(toggleBit(value, 2));    // 14
        System.out.println(isPowerOfTwo(1_024));    // true
        System.out.println(-1 >>> 1);               // 2147483647
    }
}
~~~

Every method uses int intentionally. A long version must use 1L as the mask seed and validate bit indices from 0 through 63.

## 13.11 Dry runs

### Toggle bit 2 of 10

~~~text
value       1010
mask        0100
XOR         1110 = 14
~~~

### Clear bit 3 of 10

~~~text
value       1010
mask        1000
~mask   ...0111
AND         0010 = 2
~~~

### Test 12 as a power of two

~~~text
12      1100
11      1011
AND     1000, not zero
~~~

Twelve is not a power of two.

## 13.12 Edge cases and common mistakes

- Omitting value > 0 from the power-of-two test.
- Using 1 << k when a long mask is needed.
- Assuming a left shift throws on overflow.
- Treating >> as division for every negative value.
- Forgetting that >>> still returns an int or long of the original width.
- Assuming a shift distance of 32 for int clears the value. It acts like a shift by zero.
- Accepting an arbitrary bit index without validation.
- Forgetting byte and short promotion to int.
- Confusing a binary string such as "1010" with the int whose bit pattern represents decimal 1010.
- Applying a clever mask when a readable arithmetic test better communicates intent.

## 13.13 Interview questions

1. Why does value & 1 identify parity?
2. Why must a power-of-two check reject zero?
3. What is the difference between >> and >>>?
4. Why are -3 >> 1 and -3 / 2 different?
5. What does 1 << 31 produce, and why?
6. What shift distance does 1 << 32 use?
7. How do you set, clear, test, and toggle bit k?
8. Why is Integer.MIN_VALUE a trap for "one set bit" reasoning?
9. What happens to a byte before a shift operation?
10. When should a problem be deferred to a fuller bit-manipulation toolkit?

## 13.14 Practice set

Do not read the delayed notes before attempting the problems.

### Quick check

1. Write the six-bit representation of 37.
2. Is -8 even under the low-bit test?
3. Predict -8 >> 2 and -8 >>> 2 for a 32-bit int.
4. Predict 1 << 33.
5. Explain why 0 passes the expression (value & (value - 1)) == 0.

### Coding practice

1. **Foundation:** Implement isOdd(long value).
2. **Foundation:** Implement setLongBit(long value, int bitIndex).
3. **Interview Core:** Count the set bits of a nonnegative int by repeatedly applying value &= value - 1.
4. **Interview Core:** Return the index of the only set bit in a positive power of two.
5. **SDE-2 Follow-up:** Design a method that extracts an unsigned field of width w beginning at bit offset k from a long, with validated arguments.

### Debugging task

~~~java
static boolean isPowerOfTwo(int value) {
    return (value & value - 1) == 0;
}
~~~

Find the precedence problem and the missing domain check. Then explain whether Integer.MIN_VALUE should be accepted.

### Interview extension

An input contains every integer twice except one. Explain how XOR can isolate the unique value, which algebraic properties make the solution work, and what changes if every repeated value appears three times.

## 13.15 Delayed answer notes

### Quick-check answers

1. 37 is 100101.
2. Yes. Its least significant bit is zero.
3. -8 >> 2 is -2. -8 >>> 2 is 1,073,741,822.
4. The int shift distance is 33 & 31, so the result is 1 << 1, which is 2.
5. Zero AND -1 is zero. That is why the positive-domain guard is required.

### Coding guidance

- isOdd(long value) can test (value & 1L) != 0.
- A long mask starts with 1L, and bitIndex must be from 0 through 63.
- value &= value - 1 clears the lowest set bit. The loop count equals the number of set bits.
- For a positive power of two, the set-bit index equals the number of unsigned right shifts needed to reach 1.
- Field extraction requires validating width, offset, and offset + width without overflow. A width of 64 needs special treatment because 1L << 64 acts like 1L << 0.

### Debugging resolution

Parenthesize the subtraction and the AND:

~~~java
static boolean isPowerOfTwo(int value) {
    return value > 0 && (value & (value - 1)) == 0;
}
~~~

The method deliberately rejects Integer.MIN_VALUE because it is negative, even though its bit pattern has one set bit.

### Interview-extension answer

XOR is associative and commutative, x ^ x is zero, and x ^ 0 is x. Pairs cancel regardless of order, leaving the unique value. Values repeated three times do not cancel under ordinary XOR, so a per-bit count modulo three or a two-mask state machine is required. That fuller pattern belongs in the Bit Manipulation mini-book.

## 13.16 What comes in the Bit Manipulation mini-book

The later focused book develops:

- set-bit counting variants;
- XOR cancellation families;
- single-number variants;
- subset generation with masks;
- bitwise range operations;
- prefix XOR;
- trie-based maximum XOR;
- state compression;
- bit-field design;
- overflow-aware shift identities;
- interview problems that combine masks with graphs or dynamic programming.

This chapter supplies the representation and operator semantics needed to approach those topics safely.

## 13.17 Chapter summary

- Binary positions represent powers of two.
- Java signed integers use fixed-width two's-complement representations.
- The low bit identifies parity for positive and negative values.
- Left shift resembles multiplication by powers of two only while the result fits.
- Signed right shift preserves the sign; unsigned right shift fills with zeros.
- Java masks shift distances to five bits for int and six bits for long.
- One-bit masks can test, set, clear, or toggle a position.
- A positive power of two satisfies (value & (value - 1)) == 0.

## 13.18 Revision checklist

- [ ] I can explain two's complement without claiming that Java stores a separate sign character.
- [ ] I can predict parity for negative values.
- [ ] I know the difference between >> and >>>.
- [ ] I remember int and long shift-distance masking.
- [ ] I use 1L for long masks.
- [ ] I validate bit indices.
- [ ] I include the positive guard in the power-of-two test.
- [ ] I know which advanced topics belong in the later bit-focused volume.
