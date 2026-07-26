# Bits and Java Operators from Zero

## Where this chapter fits

This is the first Bit Manipulation chapter. It assumes that you can read a Java loop, but it does not assume that bit expressions feel natural yet. Do not rush to XOR tricks. First learn to see the value, the fixed width, and the operator result.

Use this sequence:

1. write the value in binary;
2. label bit positions from right to left starting at zero;
3. align operands to the same width;
4. apply the operator one column at a time; and
5. interpret the result under the problem's signed or unsigned contract.

The Number Systems volume explains base conversion and two's complement in greater depth. This chapter reviews only the part needed to operate on bits safely in Java.

## Learning objectives

After this chapter, you should be able to:

- explain what a bit and a bit position mean;
- convert small nonnegative values between decimal and binary;
- distinguish a mathematical integer from its fixed-width Java representation;
- predict `&`, `|`, `^`, `~`, `<<`, `>>`, and `>>>` results;
- explain why `byte`, `short`, and `char` expressions become `int`;
- print a padded 32-bit or 64-bit view for debugging; and
- identify the sign, shift-distance, and literal-width traps interviewers use.

## 1.1 A bit is one binary decision

A bit has two possible values: zero or one. In a nonnegative binary number, position `i` contributes `2^i` when that bit is one.

```text
bit positions:  5 4 3 2 1 0
powers of two: 32 16 8 4 2 1
value:           1 0 1 1 0 1
```

The value `101101` is:

```text
32 + 0 + 8 + 4 + 0 + 1 = 45
```

The rightmost bit is the least significant bit, or LSB. The leftmost bit inside the chosen width is the most significant bit, or MSB. In a signed Java integer, the MSB also participates in the sign under two's-complement interpretation.

### Quick conversion: decimal to binary

For a small nonnegative number, repeatedly select powers of two.

Convert 26:

```text
largest power <= 26 is 16 -> bit 4 is 1, remainder 10
largest power <= 10 is  8 -> bit 3 is 1, remainder  2
largest power <=  2 is  2 -> bit 1 is 1, remainder  0

26 = 16 + 8 + 2 = 11010
```

For a coding interview, you rarely need to perform long manual conversions. You do need to recognize powers of two, align short patterns, and explain what each selected position represents.

### Hexadecimal is a compact bit view

One hexadecimal digit represents four bits.

| Hex | Binary | Hex | Binary |
|---:|:---:|---:|:---:|
| `0` | `0000` | `8` | `1000` |
| `1` | `0001` | `9` | `1001` |
| `2` | `0010` | `A` | `1010` |
| `3` | `0011` | `B` | `1011` |
| `4` | `0100` | `C` | `1100` |
| `5` | `0101` | `D` | `1101` |
| `6` | `0110` | `E` | `1110` |
| `7` | `0111` | `F` | `1111` |

Therefore `0x2D` is `0010 1101`, which is decimal 45. Hexadecimal makes long masks readable: `0xFF` means eight low one bits, and `0xFFFFL` means sixteen low one bits in a `long` expression.

## 1.2 Java gives the representation a fixed width

Bitwise operators work on integral primitive values:

| Type | Width | Bitwise expression behavior |
|---|---:|---|
| `byte` | 8 bits | promoted to `int` before the operation |
| `short` | 16 bits | promoted to `int` before the operation |
| `char` | 16 bits, unsigned value range | promoted to `int` before the operation |
| `int` | 32 bits | result is normally a 32-bit `int` |
| `long` | 64 bits | result is 64-bit when an operand is `long` |

`boolean` supports logical `&`, `|`, and `^`, but a boolean is not an integer bit field. Floating-point values do not support bitwise arithmetic directly.

### The representation and the interpretation are different questions

The 32-bit pattern below can be viewed as a signed Java `int` or as an unsigned magnitude:

```text
11111111 11111111 11111111 11111111
```

- As a signed `int`, it is `-1`.
- As an unsigned 32-bit value, it is `4_294_967_295`.

The bits did not change. Only the interpretation changed. Java's ordinary `int` comparisons are signed. When a problem defines unsigned ordering, use APIs such as `Integer.compareUnsigned`, `Integer.toUnsignedLong`, and `Integer.divideUnsigned` rather than pretending the sign bit is absent.

## 1.3 Two's complement without mystery

For a fixed width, negate a value by inverting all bits and adding one.

Using an illustrative 8-bit width:

```text
  5 = 0000 0101
 ~5 = 1111 1010
 +1 = 1111 1011 = -5 in 8-bit two's complement
```

Java performs this over 32 bits for `int` and 64 bits for `long`. The most negative value has no positive counterpart in the same signed type:

```text
Integer.MIN_VALUE = 10000000 00000000 00000000 00000000
```

Consequences:

- `-Integer.MIN_VALUE` is still `Integer.MIN_VALUE` because ordinary integer arithmetic wraps.
- `Math.abs(Integer.MIN_VALUE)` is still negative.
- the isolated lowest bit of `Integer.MIN_VALUE` is the sign-bit mask, which is also negative as an `int`.

Treat a mask as a pattern. A mask does not need to be a positive mathematical number to be valid.

## 1.4 The four column-wise operators

### AND: keep a bit only when both inputs contain it

| `a` | `b` | `a & b` |
|:---:|:---:|:---:|
| 0 | 0 | 0 |
| 0 | 1 | 0 |
| 1 | 0 | 0 |
| 1 | 1 | 1 |

```text
12 = 1100
10 = 1010
&    ----
 8 = 1000
```

Think: filter or retain. AND is used to test bits, clear selected positions, and extract fields.

### OR: keep a bit when either input contains it

| `a` | `b` | `a | b` |
|:---:|:---:|:---:|
| 0 | 0 | 0 |
| 0 | 1 | 1 |
| 1 | 0 | 1 |
| 1 | 1 | 1 |

```text
12 = 1100
10 = 1010
|    ----
14 = 1110
```

Think: combine or enable. OR is used to set bits and merge independent flags.

### XOR: keep a bit when the inputs differ

| `a` | `b` | `a ^ b` |
|:---:|:---:|:---:|
| 0 | 0 | 0 |
| 0 | 1 | 1 |
| 1 | 0 | 1 |
| 1 | 1 | 0 |

```text
12 = 1100
10 = 1010
^    ----
 6 = 0110
```

Think: difference or toggle. XOR is used for parity, bit flips, cancellation under exact duplicate contracts, and reversible deltas.

### NOT: invert every bit in the width

```text
illustrative 8-bit view
 5 = 0000 0101
~5 = 1111 1010 = -6
```

In two's complement, `~x == -x - 1`. The result is not just an inversion of the visible significant bits; Java inverts all 32 or all 64 positions.

## 1.5 Shift operators

### Left shift `<<`

`value << distance` moves bits left, discards high bits that leave the width, and fills low positions with zeros.

```text
  6      = 0000 0110
  6 << 1 = 0000 1100 = 12
```

For a nonnegative value whose shifted result fits, shifting left by `k` is equivalent to multiplying by `2^k`. It is not an overflow-safe multiplication technique.

```java
int wrapped = 1 << 31;       // Integer.MIN_VALUE, not positive 2^31
long correct = 1L << 31;     // positive 2^31 in a long
```

### Arithmetic right shift `>>`

`>>` preserves the sign interpretation by copying the sign bit into new high positions.

```text
illustrative 8-bit view
  40      = 0010 1000
  40 >> 2 = 0000 1010 = 10

 -40      = 1101 1000
 -40 >> 2 = 1111 0110 = -10
```

For negative odd values, `x >> 1` rounds toward negative infinity, while Java integer division `x / 2` truncates toward zero.

```java
System.out.println(-3 >> 1);  // -2
System.out.println(-3 / 2);   // -1
```

Do not substitute shifts for division without checking the negative-input contract.

### Logical right shift `>>>`

`>>>` fills high positions with zeros, regardless of the sign bit.

```java
System.out.println(-1 >> 1);   // -1
System.out.println(-1 >>> 1);  // 2147483647
```

Use `>>>` when moving through the raw representation, extracting an unsigned field, reversing bits, or scanning all positions without copying the sign.

### Java masks the shift distance

An `int` uses only the low 5 bits of the shift distance. A `long` uses only the low 6.

```java
System.out.println(1 << 32);    // 1, because 32 becomes 0
System.out.println(1L << 64);   // 1, because 64 becomes 0
System.out.println(1 << -1);    // same effective distance as 31
```

This is specified Java behavior, not validation. If a parameter represents a bit index, reject values outside `0..31` or `0..63` before shifting.

## 1.6 Promotion changes small-type expressions

`byte`, `short`, and `char` are promoted to `int` before unary and most binary numeric operations.

```java
byte flags = 0b0000_0101;
int inverted = ~flags;
System.out.println(inverted);   // -6, a 32-bit int result
```

If the intended result is an unsigned 8-bit pattern, mask the promoted result:

```java
int invertedByte = (~flags) & 0xFF;
System.out.println(invertedByte); // 250
```

The mask `0xFF` keeps only the low eight positions.

### Literal width determines shift width

```java
long wrong = 1 << 40;    // 32-bit shift; effective distance is 8
long right = 1L << 40;   // 64-bit shift
```

Adding `L` after the computation is too late. The left operand must already be a `long`.

## 1.7 A reliable way to display bits

`Integer.toBinaryString` omits leading zeros for nonnegative values. Pad it when width matters:

```java
static String bits32(int value) {
    return String.format("%32s", Integer.toBinaryString(value))
            .replace(' ', '0');
}

static String bits64(long value) {
    return String.format("%64s", Long.toBinaryString(value))
            .replace(' ', '0');
}
```

Example:

```java
System.out.println(bits32(10));
System.out.println(bits32(-1));
```

Expected output:

```text
00000000000000000000000000001010
11111111111111111111111111111111
```

Use this helper while learning. In an interview, draw only the positions relevant to the invariant unless the sign bit matters.

## 1.8 Operator reasoning shortcuts that are safe to remember

These are algebraic facts, not problem solutions by themselves:

```text
x & 0 = 0          x | 0 = x          x ^ 0 = x
x & x = x          x | x = x          x ^ x = 0
x & -1 = x         x | -1 = -1        x ^ -1 = ~x
x & ~x = 0         x | ~x = -1
```

AND, OR, and XOR are associative and commutative. That means grouping and order do not affect a reduction. Only XOR provides pair cancellation with `x ^ x = 0`.

Do not infer more than the identity proves. XORing an array does not validate that every ordinary value appears exactly twice.

## 1.9 Worked dry run: decode a packed status byte

Suppose the low byte stores:

```text
bit 0: active
bit 1: verified
bit 2: premium
bit 3: suspended
```

The value is `0b0000_0101`.

```text
position: 3 2 1 0
value:    0 1 0 1
```

- active is true because bit 0 is one;
- verified is false because bit 1 is zero;
- premium is true because bit 2 is one; and
- suspended is false because bit 3 is zero.

Testing premium:

```text
value = 0101
mask  = 0100
AND   = 0100, which is nonzero
```

The correct test is `(value & mask) != 0`. Comparing with `== 1` works only for bit zero.

## 1.10 Common beginner mistakes

- Numbering positions from one instead of zero.
- Reading binary digits from left to right without labeling positions.
- Assuming an `int` has only the displayed significant bits.
- Using `1 << index` when the destination is a `long`.
- Using `>>` to inspect a raw negative bit pattern when `>>>` is needed.
- Treating `x << k` as safe multiplication for every input.
- Forgetting promotion when applying `~` to a `byte` or `short`.
- Comparing `(value & mask) == 1` instead of checking for nonzero.
- Memorizing a bit trick without its input contract.
- Calling a fixed 32-step loop `O(log n)` without explaining whether complexity is measured by numeric value or representation width.

## 1.11 Interview checks

### Predict the output

```java
System.out.println(12 & 10);
System.out.println(12 | 10);
System.out.println(12 ^ 10);
System.out.println(~5);
```

Expected output:

```text
8
14
6
-6
```

### Explain, do not only calculate

1. Why is `~0` equal to `-1` for an `int`?
2. Why can `1 << 31` be a valid mask even though it is negative?
3. When does `x >> 1` differ from `x / 2`?
4. Why is `1L << index` required for high `long` positions?
5. What contract would make an unsigned comparison necessary?

### Debug the code

```java
static boolean isBitSet(long value, int index) {
    return (value & (1 << index)) == 1;
}
```

There are two defects: the mask is computed as an `int`, and comparing with one only recognizes bit zero. The corrected method also validates the index:

```java
static boolean isBitSet(long value, int index) {
    if (index < 0 || index >= Long.SIZE) {
        throw new IllegalArgumentException("index must be in [0, 63]");
    }
    return (value & (1L << index)) != 0;
}
```

## Chapter summary

- Bit positions start at zero from the right.
- Java bit operations occur over a fixed 32-bit or 64-bit representation.
- AND filters, OR sets or combines, XOR differs or toggles, and NOT inverts the whole width.
- `>>` extends the sign; `>>>` fills with zero.
- Java masks shift distances, so APIs must validate logical bit indexes.
- Small integral types promote to `int`.
- A suffix such as `L` must affect the operand before the shift.
- Always separate the bit pattern from its signed or unsigned interpretation.

## Readiness checkpoint

Continue only when you can draw and explain all seven operators for small values, print a padded Java representation, and diagnose the `1 << 40` and `(value & mask) == 1` defects without notes.
