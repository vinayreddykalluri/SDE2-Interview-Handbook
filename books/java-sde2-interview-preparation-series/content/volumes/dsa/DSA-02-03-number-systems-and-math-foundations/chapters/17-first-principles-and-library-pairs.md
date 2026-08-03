# First-Principles and Java Library Pairs

> **A note from Vinay:** The strongest interview answer is not "never use a library." It is "I can derive the behavior, and I know when the standard API is the clearer production choice." This chapter keeps those two skills side by side.

## 1. The comparison contract

For each pair:

1. define the accepted domain;
2. derive the manual rule;
3. test its boundaries;
4. compare it with the JDK operation over the shared domain;
5. state which version you would present and why.

The JDK implementation is an oracle in differential tests, not a substitute for the derivation.

## 2. Checked addition and subtraction

For addition `left + right`:

```text
right > 0 and left > MAX - right  -> positive overflow
right < 0 and left < MIN - right  -> negative overflow
```

For subtraction `left - right`:

```text
right > 0 and left < MIN + right  -> negative overflow
right < 0 and left > MAX + right  -> positive overflow
```

The manual methods throw `ArithmeticException`, matching the important behavior of `Math.addExact` and `Math.subtractExact`.

## 3. Checked multiplication

A simple manual check multiplies and then verifies reversibility:

```java
long result = left * right;
if (left != 0 && result / left != right) {
    throw new ArithmeticException("long overflow");
}
```

One boundary needs explicit handling: `-1 * Long.MIN_VALUE` wraps to `Long.MIN_VALUE`, and dividing that wrapped value by `-1` also overflows. The complete implementation checks that pair before multiplication.

Use `Math.multiplyExact` in production code when the manual rule is not the interview target.

## 4. Modulo normalization

For positive modulus `m`, Java remainder lies in `(-m, m)`. Normalize once:

```java
long remainder = value % m;
return remainder < 0 ? remainder + m : remainder;
```

This matches `Math.floorMod(value, m)` for a positive modulus. Reject zero or negative modulus if the algorithm promises the canonical range `[0, m)`.

## 5. Bit length

For a nonnegative number, repeatedly shift unsigned until zero. The number of shifts is the bit length. For zero, choose and document either mathematical bit length zero or display width one. This book's display-oriented contract uses one bit for zero.

The library comparison is:

```java
value == 0 ? 1 : Long.SIZE - Long.numberOfLeadingZeros(value)
```

## 6. GCD without losing `MIN_VALUE`

Negating `Long.MIN_VALUE` overflows. A manual primitive Euclidean algorithm can keep both operands nonpositive, because the negative domain has one extra representable magnitude. If the final magnitude is exactly `2^63`, it cannot be returned as a positive `long`; the primitive method must throw or use a wider representation.

The existing `gcdMagnitude` uses `BigInteger` when the full nonnegative magnitude is required. That is a contract decision, not a shortcut to hide Euclid.

## 7. Modular inverse

Extended Euclid maintains coefficients satisfying:

```text
oldS * value + oldT * modulus = oldR
```

When the final gcd is one, `oldS` is an inverse coefficient. Normalize it into `[0, modulus)`. The manual companion restricts modulus to positive `int` range so its coefficient arithmetic remains safely representable in `long`; the `BigInteger` version supports a wider contract.

## 8. Exact factorial digits without logarithms

The logarithmic method efficiently counts digits but depends on floating-point accumulation. To show exact first principles, store the factorial as decimal digits in little-endian order and multiply the digit array by every factor.

```text
5! construction
1 -> 2 -> 6 -> 24 -> 120
digits stored least-significant first: [0, 2, 1]
```

The exact construction costs far more time and memory than the logarithmic count. Use it to teach representation or when exact digits are needed; use the logarithmic approach when only the count is required and its numeric-accuracy contract is acceptable.

## 9. Boundary matrix

| Operation | Required boundaries |
|---|---|
| checked add/subtract | zero, opposite signs, `MIN_VALUE`, `MAX_VALUE`, one-step overflow |
| checked multiply | zero, one, negative one, mixed signs, `MIN_VALUE`, square near square-root limit |
| narrowing | `Integer.MIN_VALUE/MAX_VALUE` and one outside each boundary |
| modulo | negative value, exact multiple, zero value, modulus one, invalid modulus |
| bit length | zero, powers of two, one below/above a power, `Long.MAX_VALUE` |
| GCD | zeros, signs, equal values, coprime values, `MIN_VALUE` |
| modular inverse | value zero, negative value, coprime pair, non-coprime pair, modulus one |
| factorial digits | `0!`, `1!`, power-of-ten transitions, larger differential checks |

## 10. Interviewer questions with model answers

**Why not always use `Math.addExact` in an interview?** If overflow detection is the concept being tested, derive the boundary first and then mention the API. If overflow is incidental, the API is clearer and less error-prone.

**Is the handwritten version faster?** Do not assume so. The JDK may intrinsify exact arithmetic, and performance is not the reason to reimplement it here.

**Why not use `Math.abs` before GCD?** `Math.abs(Long.MIN_VALUE)` remains negative because its positive magnitude is not representable as a `long`.

**Is a logarithmic factorial digit count exact for every possible input?** It uses floating-point sums, so its supported precision and input range need testing. Exact digit construction avoids that approximation but is much more expensive.

**When is `BigInteger` the right answer?** When the required result does not fit a primitive or the contract explicitly permits arbitrary precision. Explain the algorithm it supports rather than presenting the type name as the reasoning.

**What proves two implementations agree?** A proof establishes the manual rule. Differential tests across boundaries and deterministic randomized values catch implementation mistakes over their shared domain.

## 11. Executable comparison

`NumberSystemsManualLibraryChecks.java` tests manual checked arithmetic, narrowing, modulo normalization, bit length, primitive GCD, modular inverse, and exact factorial digit construction against the corresponding JDK or existing-library result wherever their contracts overlap.
