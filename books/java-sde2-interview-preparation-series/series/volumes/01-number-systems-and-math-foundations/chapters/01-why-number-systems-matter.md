# Chapter 1: Why Number Systems Matter in DSA

Number systems are not a separate academic requirement that sits beside algorithms. They are the representation layer under many interview problems. A candidate may choose the correct data structure and still fail because a sum overflows, a negative remainder is used as an array index, a binary string is parsed incorrectly, or a midpoint is calculated with unsafe arithmetic.

This book develops only the mathematics that supports practical Java problem solving. The goal is to recognize a numeric requirement, select a safe representation, implement the needed operation, and explain the decision clearly.

## Learning objectives

By the end of this chapter, you should be able to:

- connect common interview signals to the numeric concept they require;
- distinguish a mathematical value from its Java representation;
- identify range, sign, base, and overflow questions before coding;
- use a small set of safe Java arithmetic patterns; and
- assess whether you are ready to study digit algorithms and base conversion.

![The Number Systems learning route orders representation, safe arithmetic, number tools, and interview applications by prerequisite.](series/volumes/01-number-systems-and-math-foundations/assets/11-topic-dependency-map.png)

## How to start without getting lost

Read Part A in order the first time. Chapters 1-2 establish the vocabulary and digit loop; Chapters 3-5 teach representations and conversion; Chapters 6-8 establish Java range and large-input safety; Chapters 9-13 add the mathematical tools and the bit bridge. Then use Part B as an implementation and retrieval workbook.

If a later problem feels opaque, follow its prerequisite rather than rereading everything. A base-conversion error routes to Chapters 3-5. An overflow bug routes to Chapters 6-7. A repeated-prime-query problem routes to Chapter 10 and the 52-implementation catalog. This dependency-first route is faster than choosing a page at random.

> **Prerequisite check:** You need basic Java expressions, loops, methods, arrays, and strings. You do not need prior confidence with place value, binary, modulo, primes, GCD, or logarithms.

> **Scope:** This book teaches numeric prerequisites and interview applications. Full bit tricks, generalized binary search, array/string patterns, hashing, recursion, and dynamic programming remain in their dedicated mini-books.

## One value, several representations

The mathematical value thirteen can be written as decimal `13`, binary `1101`, octal `15`, or hexadecimal `D`. The value does not change. The representation changes the symbols and place values used to describe it.

Java adds another concern: the type that stores the value. An `int` stores a signed 32-bit value. A `long` stores a signed 64-bit value. A `String` can hold thousands of decimal digits but does not automatically support arithmetic. `BigInteger` supports integers beyond `long`, with costs that grow with the number of digits.

An interview solution therefore answers two different questions:

1. What mathematical operation is required?
2. Which Java representation can perform it without losing information?

For example, a prefix sum may be mathematically simple addition, but `int` may be too small. A 100,000-digit account number cannot be parsed into `long`, but its remainder modulo 9 can be computed one digit at a time.

## Recognition signals

Use the words and constraints in the problem to expose the hidden numeric requirement.

| Problem signal | Numeric idea | Typical response |
|---|---|---|
| "digits," "reverse," or "palindrome number" | Decimal decomposition | Repeated `% 10` and `/ 10` |
| Binary string, flags, masks, or powers of two | Base 2 representation | Validate bits; connect positions to powers of two |
| Huge number supplied as text | Incremental accumulation | Process one character at a time or use `BigInteger` |
| Answer requested modulo a constant | Modular arithmetic | Normalize remainders and widen before multiplication |
| Sorted boundary or monotone feasibility | Repeated halving | Use an overflow-aware midpoint |
| Factors, divisibility, or periodic alignment | GCD, LCM, primes | Apply bounded integer algorithms |
| Count or sum over many elements | Range analysis | Estimate the maximum before selecting `int` or `long` |
| Comparator over numeric fields | Ordering without subtraction | Use `Integer.compare` or `Long.compare` |

The recognition signal suggests a tool, not a complete solution. Constraints still decide whether the tool is valid. A digit algorithm must define negative input. A multiplication under modulo must ensure the intermediate product fits its type. A binary search must use a monotone predicate.

## Numeric failures that break good algorithms

### Overflow before assignment

Java evaluates `int * int` as `int`, even when the result is assigned to `long`:

```java
int rows = 100_000;
int columns = 100_000;

long wrong = rows * columns;          // overflow occurs first
long correct = (long) rows * columns; // multiplication occurs as long
```

The type of the destination does not travel backward and change how the expression is evaluated. Promote an operand before the arithmetic.

### Unsafe midpoint arithmetic

For nonnegative ordered indexes, this midpoint avoids adding the two endpoints:

```java
int mid = left + (right - left) / 2;
```

The pattern is safe for normal array-index intervals because `0 <= left <= right`. It is not a universal formula for arbitrary endpoints spanning the full signed `int` range; in that broader contract, `right - left` can itself overflow and a `long` intermediate is required.

### Remainder is not always mathematical modulo

Java's `%` result has the dividend's sign:

```java
System.out.println(-7 % 5);       // -2
System.out.println(Math.floorMod(-7, 5)); // 3
```

This matters for circular indexes and remainder-frequency arrays. When the modulus is positive and negative values are allowed, `Math.floorMod` states the intent clearly.

### Subtraction is not a safe comparator

This comparator can overflow and reverse the apparent order:

```java
// Wrong for widely separated values.
(left, right) -> left - right

// Correct.
(left, right) -> Integer.compare(left, right)
```

The interviewer is not testing whether you remember a style rule. The test is whether your comparison preserves the ordering contract at numeric boundaries.

## A small Java readiness example

The following program combines three recurring foundations. It validates the contract, widens before multiplication, uses a midpoint appropriate for array indexes, and normalizes a circular index.

```java
public final class NumericReadiness {
    private NumericReadiness() {}

    public static long cellCount(int rows, int columns) {
        if (rows < 0 || columns < 0) {
            throw new IllegalArgumentException("dimensions must be nonnegative");
        }
        return (long) rows * columns;
    }

    public static int midpoint(int left, int right) {
        if (left < 0 || left > right) {
            throw new IllegalArgumentException("invalid index interval");
        }
        return left + (right - left) / 2;
    }

    public static int circularIndex(int index, int length) {
        if (length <= 0) {
            throw new IllegalArgumentException("length must be positive");
        }
        return Math.floorMod(index, length);
    }

    public static void main(String[] args) {
        System.out.println(cellCount(100_000, 100_000)); // 10000000000
        System.out.println(midpoint(2, 9));              // 5
        System.out.println(circularIndex(-1, 8));        // 7
    }
}
```

### Dry run

For `cellCount(100_000, 100_000)`, casting `rows` changes the multiplication to `long`. The result is `10,000,000,000`, which is larger than `Integer.MAX_VALUE` but fits in `long`.

For `midpoint(2, 9)`, the distance is `7`; integer division gives `3`; adding it to `2` returns `5`. Either middle convention can be valid for an even-sized interval, but the boundary updates must match the chosen convention.

For `circularIndex(-1, 8)`, Java `%` would produce `-1`. `floorMod` returns `7`, the valid last index in a cycle of length eight.

Each operation uses constant time and constant auxiliary space because Java primitive widths are fixed.

## How number systems connect to the DSA sequence

Number systems should come before the later pattern books:

- Complexity uses powers, roots, repeated doubling, and repeated halving.
- Bit manipulation assumes that binary place values and signed fixed-width integers are familiar.
- Arrays require safe indexes, counts, products of dimensions, and prefix sums.
- Binary search requires a correct midpoint and logarithmic reasoning.
- Hashing uses fixed-width hash codes, bucket calculations, and sometimes masks.
- Dynamic programming often returns counts under a modulus.
- Graph and tree problems use distances, weights, heights, and sentinel boundaries.

The objective is not to force mathematics into every solution. It is to remove representation mistakes so the algorithm remains the difficult part.

## Common candidate mistakes

- Selecting `int` because each input element fits in `int`, without bounding their sum or product.
- Calling `Math.abs` on `Integer.MIN_VALUE` and assuming the result is positive.
- Parsing an arbitrarily long numeric string into `long` before processing it.
- Treating Java `%` as always nonnegative.
- Using floating point for an operation that must be exact.
- Memorizing a conversion result without understanding positional accumulation.
- Applying a formula without stating its range and sign assumptions.
- Giving complexity without including the number of input digits.

## Readiness checklist

You are ready to continue when you can answer yes to most of these statements:

- I know the ranges of `int` and `long` conceptually and can look up their constants.
- I promote an operand before arithmetic that needs a wider type.
- I treat zero and negative inputs as explicit contract decisions.
- I understand that a numeric string may be larger than every primitive type.
- I can explain why repeated division extracts digits or base digits.
- I know that `%` can be negative in Java.
- I test minimum, maximum, and just-over-the-boundary values.
- I can state the representation unit: decimal digit, bit, byte, code unit, or whole value.

## Chapter summary

Interview mathematics begins with representation. Identify the value domain, select a Java type or string representation that can hold it, and make sign and overflow behavior part of the contract. Digit problems, binary representation, modulo, hashing, and logarithmic algorithms become easier when these foundations are explicit.

## Quick Check

1. Why can `long result = a * b;` still overflow when `a` and `b` are `int`?
2. What is the difference between a mathematical value and its base representation?
3. Why can `%` produce an invalid array index for negative input?
4. Under what precondition is `left + (right - left) / 2` safe for indexes?
5. Why is a very large numeric string sometimes processed without parsing the whole value?

## Coding Practice

1. **Foundation:** Write a method that returns the number of cells in a matrix using a safe result type.
2. **Foundation:** Normalize any integer index into a positive cycle length.
3. **Interview Core:** Implement an overflow-aware average of two nonnegative `int` values.
4. **Interview Core:** Given element count and maximum absolute value, decide whether a sum needs `int`, `long`, or a larger representation.
5. **SDE-2 Follow-up:** Design a small value object that records a numeric value together with its unit and rejects incompatible addition.

## Debugging Task

**Interview Core:** Find every contract and arithmetic defect in this method. Do not reveal the corrected code until you have written boundary tests.

```java
static int bucket(int value, int bucketCount) {
    int magnitude = Math.abs(value);
    return magnitude % bucketCount;
}
```

Test zero buckets, negative values, and `Integer.MIN_VALUE`.

## Interview Extension

**SDE-2 Follow-up:** A counter is expected to receive 50,000 updates per second for 90 days. Choose a Java representation, calculate whether it fits, define overflow behavior, and explain how your answer changes if several counters are multiplied to estimate pair combinations.
