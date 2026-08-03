# Masks, Core Techniques, and Safe Shortcuts

## The rule behind every mask

A mask marks positions that an operation should affect. The operator decides what happens to those positions:

- AND with a mask keeps selected positions;
- AND with an inverted mask clears selected positions;
- OR with a mask sets selected positions; and
- XOR with a mask toggles selected positions.

If you can derive those four actions from the truth tables, you do not need to memorize disconnected formulas.

## Learning objectives

After this chapter, you should be able to:

- test, set, clear, toggle, and replace bits safely;
- build single-bit, low-bit, high-bit, and range masks;
- count, isolate, remove, and inspect set bits;
- recognize powers of two and four under precise contracts;
- calculate Hamming distance and required bit flips;
- reverse bits and compute per-value bit counts;
- choose Java library methods when they communicate intent better; and
- explain why each shortcut works and where it fails.

## 2.1 One-bit operations

Let `index` be a validated `int` position and let `mask = 1 << index`.

| Goal | Expression | Reason |
|---|---|---|
| Test | `(value & mask) != 0` | AND retains the selected bit |
| Set | `value | mask` | OR forces the selected bit to one |
| Clear | `value & ~mask` | inverted mask has zero at the selected bit |
| Toggle | `value ^ mask` | XOR with one flips a bit |

Safe `long` helpers:

```java
static long oneBit(int index) {
    if (index < 0 || index >= Long.SIZE) {
        throw new IllegalArgumentException("index must be in [0, 63]");
    }
    return 1L << index;
}

static boolean isSet(long value, int index) {
    return (value & oneBit(index)) != 0;
}

static long set(long value, int index) {
    return value | oneBit(index);
}

static long clear(long value, int index) {
    return value & ~oneBit(index);
}

static long toggle(long value, int index) {
    return value ^ oneBit(index);
}
```

### Dry run: clear bit 3 from 13

```text
value         = 1101
mask          = 1000
inverted mask = ...0111
result        = 0101 = 5
```

Only bit 3 changed. All other positions were ANDed with one and preserved.

## 2.2 Set a bit to a requested boolean value

A clear implementation is often best:

```java
static long update(long value, int index, boolean enabled) {
    return enabled ? set(value, index) : clear(value, index);
}
```

An interviewer may ask for a branch-free version. Convert the boolean to either all zeros or all ones:

```java
static long updateBranchFree(long value, int index, boolean enabled) {
    long mask = oneBit(index);
    long requested = enabled ? -1L : 0L;
    return (value & ~mask) | (requested & mask);
}
```

The branch-free form is not automatically faster in Java and is less obvious. Present the readable version first unless the constraint specifically rewards branch removal.

## 2.3 Build a mask for the lowest width bits

For widths from 1 through 63:

```java
long mask = (1L << width) - 1;
```

Example for width 5:

```text
1L << 5       = 100000
(1L << 5) - 1 = 011111
```

Width 64 is a special case because Java masks the shift distance and `1L << 64` becomes `1L << 0`.

```java
static long lowBitsMask(int width) {
    if (width < 0 || width > Long.SIZE) {
        throw new IllegalArgumentException("width must be in [0, 64]");
    }
    if (width == 0) {
        return 0L;
    }
    if (width == Long.SIZE) {
        return -1L;
    }
    return (1L << width) - 1;
}
```

## 2.4 Build and use a contiguous range mask

Suppose a field begins at `offset` and occupies `width` bits.

```java
static long rangeMask(int offset, int width) {
    if (offset < 0 || width < 0 || offset > Long.SIZE - width) {
        throw new IllegalArgumentException("invalid range");
    }
    return lowBitsMask(width) << offset;
}
```

Extract an unsigned field:

```java
static long extractField(long word, int offset, int width) {
    if (width == Long.SIZE) {
        if (offset != 0) {
            throw new IllegalArgumentException("invalid full-width field");
        }
        return word;
    }
    return (word >>> offset) & lowBitsMask(width);
}
```

Replace a field:

```java
static long replaceField(long word, int offset, int width, long newValue) {
    long unshiftedMask = lowBitsMask(width);
    if ((newValue & ~unshiftedMask) != 0) {
        throw new IllegalArgumentException("new value does not fit");
    }
    long mask = unshiftedMask << offset;
    return (word & ~mask) | (newValue << offset);
}
```

The two phases are visible: clear the old field, then insert the new field. Validation prevents silent truncation.

## 2.5 The lowest set bit: `x & -x`

For nonzero `x`, `x & -x` isolates its lowest one bit.

```text
x             = 10110000
-x            = 01010000  (same illustrative width)
x & -x        = 00010000
```

Why it works:

1. `-x` is `~x + 1`.
2. Bits below the lowest one in `x` are zero.
3. The lowest one remains one in both `x` and `-x`.
4. Higher positions differ or are irrelevant to the AND result.

Use cases:

- partitioning two exceptional XOR values;
- Fenwick tree index movement;
- iterating selected positions; and
- converting a set bit into its position with `numberOfTrailingZeros`.

```java
int lowestMask = value & -value;
int index = Integer.numberOfTrailingZeros(value); // 32 when value is zero
```

`Integer.numberOfTrailingZeros(0)` returning 32 is a defined sentinel, not a valid `int` bit index. Check zero when the contract requires an actual selected bit.

## 2.6 Remove the lowest set bit: `x & (x - 1)`

```text
x       = 10110000
x - 1   = 10101111
AND     = 10100000
```

Subtracting one clears the lowest one and changes lower zeros to ones. AND removes that lowest one and clears the changed suffix.

### Brian Kernighan's set-bit count

```java
static int countSetBits(int value) {
    int count = 0;
    while (value != 0) {
        value &= value - 1;
        count++;
    }
    return count;
}
```

Loop invariant: `count` equals the number of one bits removed from the original value, and `value` contains exactly the remaining one bits.

Time is `O(p)`, where `p` is the number of set bits. Since an `int` has 32 positions, this is bounded by 32 iterations. Stating `O(p)` explains the progress measure; stating constant time under a fixed-width machine model explains the bound.

### Iterate only the set positions

```java
for (int remaining = value; remaining != 0; remaining &= remaining - 1) {
    int index = Integer.numberOfTrailingZeros(remaining);
    // process index
}
```

This visits selected positions without scanning all 32. It is useful when the mask is sparse.

## 2.7 Power-of-two recognition

A positive power of two has exactly one set bit.

```java
static boolean isPowerOfTwo(int value) {
    return value > 0 && (value & (value - 1)) == 0;
}
```

The positivity check is mandatory. Without it:

- zero passes because `0 & -1` is zero; and
- `Integer.MIN_VALUE` passes the one-bit test even though the usual interview contract asks for a positive power of two.

Java already provides `Integer.bitCount(value) == 1`, but it needs the same positive-value contract.

### Power of four

A positive power of four has one set bit in an even index: 0, 2, 4, and so on.

```java
static boolean isPowerOfFour(int value) {
    return value > 0
            && (value & (value - 1)) == 0
            && (value & 0x5555_5555) != 0;
}
```

`0x5555_5555` is the repeating pattern `0101...0101`, selecting even positions. An arithmetic alternative is `value % 3 == 1` after the power-of-two check, because powers of four are `1 mod 3`; the mask version reveals the bit position directly.

## 2.8 Hamming distance and minimum flips

The Hamming distance between two fixed-width integers is the number of positions on which they differ.

```java
static int hammingDistance(int first, int second) {
    return Integer.bitCount(first ^ second);
}
```

XOR creates one exactly where the operands differ; bit count finishes the task.

### Flips needed so `(a | b) == target`

For each position:

- if target is zero, every one in `a` or `b` must be cleared;
- if target is one and both inputs are zero, one input must be set; and
- otherwise no change is needed.

```java
static int flipsForOr(int a, int b, int target) {
    int flips = 0;
    for (int bit = 0; bit < Integer.SIZE; bit++) {
        int aBit = (a >>> bit) & 1;
        int bBit = (b >>> bit) & 1;
        int targetBit = (target >>> bit) & 1;
        if (targetBit == 0) {
            flips += aBit + bBit;
        } else if (aBit == 0 && bBit == 0) {
            flips++;
        }
    }
    return flips;
}
```

The per-bit truth table is clearer than attempting a memorized combined expression during an interview.

## 2.9 Reverse a 32-bit representation

Build the result from left to right while consuming the input from right to left.

```java
static int reverseBits(int value) {
    int reversed = 0;
    for (int bit = 0; bit < Integer.SIZE; bit++) {
        reversed = (reversed << 1) | (value & 1);
        value >>>= 1;
    }
    return reversed;
}
```

Use `>>>` so a negative input eventually shifts in zeros. The loop must run exactly 32 times because leading zeros in the input become trailing zeros in the result and still belong to the fixed-width contract.

Java provides `Integer.reverse(value)` and `Long.reverse(value)`. Know the derivation; use the library when the interview allows it.

## 2.10 Count bits for every value from zero through n

Removing the lowest set bit gives a dynamic-programming relation:

```text
bits[x] = bits[x & (x - 1)] + 1
```

The predecessor is smaller than `x`, so it has already been computed.

```java
static int[] countBitsThrough(int n) {
    if (n < 0) {
        throw new IllegalArgumentException("n must be nonnegative");
    }
    int[] bits = new int[n + 1];
    for (int value = 1; value <= n; value++) {
        bits[value] = bits[value & (value - 1)] + 1;
    }
    return bits;
}
```

Time is `O(n)` and output space is `O(n)`. The recurrence saves repeated per-value scanning; it does not reduce the requirement to produce `n + 1` answers.

An equally valid relation is:

```text
bits[x] = bits[x >>> 1] + (x & 1)
```

Choose the one you can explain most clearly.

## 2.11 Highest bit, bit length, and next capacity

Java exposes useful operations:

| Goal | Java API |
|---|---|
| count ones | `Integer.bitCount(x)` |
| lowest one-bit mask | `Integer.lowestOneBit(x)` |
| highest one-bit mask | `Integer.highestOneBit(x)` |
| leading zeros | `Integer.numberOfLeadingZeros(x)` |
| trailing zeros | `Integer.numberOfTrailingZeros(x)` |
| rotate | `Integer.rotateLeft(x, distance)` |
| reverse bits | `Integer.reverse(x)` |
| unsigned string | `Integer.toUnsignedString(x)` |

For positive `x`, its bit length is:

```java
int bitLength = Integer.SIZE - Integer.numberOfLeadingZeros(x);
```

For zero, define the bit length as zero explicitly.

A common capacity problem asks for the smallest power of two at least as large as a positive value. Derive it carefully and handle overflow rather than copying a compact formula. For an `int`, the next positive power of two cannot exceed `1 << 30`.

```java
static int ceilingPowerOfTwo(int value) {
    if (value <= 1) {
        return 1;
    }
    if (value > (1 << 30)) {
        throw new ArithmeticException("result does not fit positive int");
    }
    return 1 << (Integer.SIZE - Integer.numberOfLeadingZeros(value - 1));
}
```

Subtracting one makes an existing power of two remain unchanged after rounding.

## 2.12 A shortcut selection table

| Problem wording | Candidate technique | Contract to say aloud |
|---|---|---|
| Is this a positive power of two? | `x > 0 && (x & (x - 1)) == 0` | positive mathematical value |
| Count selected positions | Kernighan or `bitCount` | count over 32 or 64 representation bits |
| Find the lowest selected position | `x & -x` or trailing zeros | input must be nonzero for a valid position |
| Remove one selected position | `x &= x - 1` | removes the lowest one only |
| Compare bit patterns | `bitCount(a ^ b)` | fixed width and sign interpretation defined |
| Read an unsigned field | `>>>` then AND | offset and width validated |
| Force selected bits on | OR with mask | mask width matches value width |
| Force selected bits off | AND with inverted mask | inversion spans full width |
| Flip selected bits | XOR with mask | toggling twice restores original |

The table is a retrieval aid. During an interview, derive the expression from the desired per-position behavior and then state the precondition.

## 2.13 Common failures and corrections

### Failure: check whether the third bit is set

```java
return (value & (1 << 2)) == 1;
```

Correction:

```java
return (value & (1 << 2)) != 0;
```

### Failure: clear with XOR

```java
value ^= mask;
```

XOR toggles. If the bit was zero, this sets it. To clear unconditionally:

```java
value &= ~mask;
```

### Failure: power-of-two test accepts zero

```java
return (value & (value - 1)) == 0;
```

Correction:

```java
return value > 0 && (value & (value - 1)) == 0;
```

### Failure: build all-low-bits mask for width 64

```java
return (1L << width) - 1;
```

Handle width zero and width 64 explicitly.

## 2.14 Interview follow-ups

1. Why can `x & -x` be negative yet still be a correct mask?
2. Compare Kernighan's loop with scanning all 32 positions.
3. What does `Integer.bitCount(-1)` return, and why?
4. Why is a branch-free update not automatically faster on the JVM?
5. How would you generalize these operations to more than 64 flags?
6. Why must a packed-field replacement validate the new value before ORing it?
7. What is the time complexity of bit count if input size is measured in bits rather than as one machine word?

## Chapter summary

- A mask plus an operator gives a precise per-position update.
- `x & -x` isolates the lowest set bit; `x & (x - 1)` removes it.
- Power-of-two tests need an explicit positive-input condition.
- XOR plus bit count computes Hamming distance.
- Range masks need special handling at widths zero and 64.
- Java's named bit APIs are often the clearest production choice.
- Safe interview shortcuts include their derivation, fixed width, and input contract.

## Readiness checkpoint

From memory, implement test, set, clear, toggle, low-bit isolation, set-bit count, positive power-of-two detection, Hamming distance, and field extraction. For each, explain one failing edge case in the naive version.
