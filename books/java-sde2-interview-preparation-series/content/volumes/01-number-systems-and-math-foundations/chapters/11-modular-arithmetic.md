# Chapter 11: Modular Arithmetic for DSA

Modular arithmetic keeps only a remainder class. In interviews it appears in cyclic indexes, large answer counts, prefix-state problems, hashing, powers, and divisibility of numeric strings. The mathematics is simple, but Java adds two practical hazards: `%` can return a negative value, and the arithmetic performed before `%` can overflow.

## Java remainder versus mathematical modulo

Java's `%` operator returns a remainder whose sign follows the dividend:

```java
System.out.println(14 % 5);  // 4
System.out.println(-14 % 5); // -4
```

Many algorithms instead need a canonical mathematical modulo in `[0, mod)`. For a positive modulus, the familiar normalization is:

```java
int normalized = ((value % mod) + mod) % mod;
```

This pattern is correct when the addition cannot overflow, as with common small moduli. For arbitrary positive `int mod`, `(value % mod) + mod` can exceed `Integer.MAX_VALUE`. A robust Java implementation uses `Math.floorMod` or widens the addition:

```java
int normalized = Math.floorMod(value, mod);
```

Always validate `mod > 0` when your API promises a canonical nonnegative result. `% 0` throws `ArithmeticException`, and a negative modulus makes the desired range ambiguous.

## The cycle model

![Modulo as a clock: normalized values map every integer to one position in the cycle.](content/volumes/01-number-systems-and-math-foundations/assets/08-modular-clock.png)

Modulo `m` places integers into `m` repeating positions:

```text
... -2 -1  0  1  2  3  4  5  6  7 ...
     3  4  0  1  2  3  4  0  1  2     modulo 5
```

Values with the same normalized remainder are equivalent for addition, subtraction, and multiplication under the modulus. This is why an algorithm can reduce intermediate values after every step instead of storing a huge exact result.

## Addition, subtraction, and multiplication under a modulus

For positive `m`:

```text
(a + b) mod m = ((a mod m) + (b mod m)) mod m
(a - b) mod m = ((a mod m) - (b mod m)) mod m
(a * b) mod m = ((a mod m) * (b mod m)) mod m
```

Reduction controls magnitude, but evaluate intermediate arithmetic in a wide enough type. For `int` operands, the required multiplication pattern is:

```java
long result = (a * 1L * b) % mod;
```

The `1L` forces 64-bit multiplication before the remainder. Any `int * int` product fits in `long`. The result can still be negative when one operand is negative, so normalize when the contract requires `[0, mod)`.

For arbitrary `long` operands, `long * long` can overflow before `%`. Reducing both values helps only if `(mod - 1)^2` fits in `long`. With a very large `long` modulus, use `BigInteger`, a proven repeated-doubling method, or a specialized algorithm.

## A safe Java toolkit for `int` inputs

```java
public final class ModularArithmetic {
    private ModularArithmetic() {}

    public static int normalize(int value, int modulus) {
        requirePositiveModulus(modulus);
        return Math.floorMod(value, modulus);
    }

    public static int addMod(int left, int right, int modulus) {
        requirePositiveModulus(modulus);
        long a = Math.floorMod(left, modulus);
        long b = Math.floorMod(right, modulus);
        return (int) ((a + b) % modulus);
    }

    public static int subtractMod(int left, int right, int modulus) {
        requirePositiveModulus(modulus);
        long a = Math.floorMod(left, modulus);
        long b = Math.floorMod(right, modulus);
        return (int) Math.floorMod(a - b, (long) modulus);
    }

    public static int multiplyMod(int left, int right, int modulus) {
        requirePositiveModulus(modulus);
        long result = (left * 1L * right) % modulus;
        return (int) Math.floorMod(result, (long) modulus);
    }

    public static int powerMod(int base, long exponent, int modulus) {
        requirePositiveModulus(modulus);
        if (exponent < 0) {
            throw new IllegalArgumentException("exponent must be nonnegative");
        }

        long result = 1L % modulus;
        long factor = Math.floorMod(base, modulus);
        long remainingExponent = exponent;

        while (remainingExponent > 0) {
            if ((remainingExponent & 1L) != 0) {
                result = (result * factor) % modulus;
            }
            factor = (factor * factor) % modulus;
            remainingExponent >>>= 1;
        }
        return (int) result;
    }

    public static int decimalStringMod(String value, int modulus) {
        requirePositiveModulus(modulus);
        if (value == null || value.isEmpty()) {
            throw new IllegalArgumentException("value must contain digits");
        }

        int index = 0;
        boolean negative = false;
        if (value.charAt(0) == '+' || value.charAt(0) == '-') {
            negative = value.charAt(0) == '-';
            index = 1;
        }
        if (index == value.length()) {
            throw new IllegalArgumentException("sign requires digits");
        }

        int remainder = 0;
        for (; index < value.length(); index++) {
            char character = value.charAt(index);
            if (character < '0' || character > '9') {
                throw new IllegalArgumentException("invalid decimal digit");
            }
            int digit = character - '0';
            remainder = (int) ((remainder * 10L + digit) % modulus);
        }
        return negative && remainder != 0 ? modulus - remainder : remainder;
    }

    private static void requirePositiveModulus(int modulus) {
        if (modulus <= 0) {
            throw new IllegalArgumentException("modulus must be positive");
        }
    }

    public static void main(String[] args) {
        System.out.println(normalize(-14, 5)); // 1
        System.out.println(addMod(Integer.MAX_VALUE, Integer.MAX_VALUE, 97));
        System.out.println(subtractMod(2, 7, 5)); // 0
        System.out.println(multiplyMod(100_000, 100_000, 1_000_000_007));
        System.out.println(powerMod(2, 10, 1_000)); // 24
        System.out.println(decimalStringMod("-123456789123456789", 97));
    }
}
```

In `powerMod`, `result` and `factor` are always below an `int` modulus, so their product is below `(2^31 - 1)^2` and fits in `long`. The exponent is shifted unsigned because it is already validated as nonnegative.

## Dry run: normalize and multiply

For `normalize(-14, 5)`, Java gives `-14 % 5 = -4`. Moving to the equivalent nonnegative position gives 1. `Math.floorMod(-14, 5)` returns that value directly.

For `multiplyMod(100_000, 100_000, 1_000_000_007)`:

1. `1L` forces the product to be computed as `10_000_000_000L`.
2. The remainder is `999_999_937`.
3. It is already nonnegative, so normalization leaves it unchanged.

An `int` product would overflow before the `%` operation.

## Fast exponentiation

Repeated multiplication takes O(exponent) steps. Exponentiation by squaring uses the binary representation of the exponent:

- if the current exponent bit is 1, multiply the result by the current factor;
- square the factor; and
- discard the processed exponent bit.

For `2^10`, binary 10 is `1010`. Only the factor values corresponding to set bits contribute to the result. The method takes O(log exponent) time and O(1) auxiliary space.

Negative exponents require modular inverses and additional preconditions. They are outside this chapter's mainstream interview scope; do not pretend ordinary integer division solves them.

## Why `1_000_000_007` appears so often

Counting problems can produce exponentially large answers. Returning the answer modulo `1_000_000_007` keeps values bounded and makes cross-language judging deterministic. This modulus is positive, fits in `int`, and is prime, which supports some advanced operations. Primality is not needed for ordinary modular addition and multiplication.

Do not apply a modulus unless the problem asks for it. A reduced answer cannot later recover the exact count, ordering, or magnitude.

## Prefix sums with modulo

Let `prefix[i]` be the sum of the first `i` array values. A subarray from `left` through `right` has sum divisible by `k` when the two boundary prefix sums have the same normalized remainder modulo `k`:

```text
prefix[right + 1] - prefix[left] is divisible by k
```

Store frequencies or earliest indexes by normalized remainder. Normalize after adding each possibly negative value. If raw Java remainders are used, mathematically equal states such as -1 and `k - 1` land in different map keys and valid subarrays are missed.

## Cyclic indexing and hashing

Circular buffers and rotating arrays commonly need:

```java
int wrappedIndex = Math.floorMod(index, length);
```

The expression `index % length` is wrong for a negative index. Validate `length > 0` first.

Polynomial rolling hashes also repeatedly multiply and add under a modulus. Hash equality is only a candidate match because collisions exist; verify actual content when correctness requires certainty.

## Complexity

Normalization, modular addition, subtraction, and `int` multiplication take O(1) time and space. Fast exponentiation takes O(log exponent) time and O(1) space. A decimal-string remainder takes O(n) time for n digits and O(1) auxiliary space. Prefix-remainder scans are typically O(n) expected time with a hash map and O(min(n, k)) stored states.

## Edge cases and common mistakes

- Modulus zero is invalid; this API also rejects negative moduli.
- Java `%` is a signed remainder, not always a canonical modulo.
- `(a + b) % mod` can overflow before reduction.
- `(a * b) % mod` can overflow even when the final remainder is small.
- The `1L` multiplication pattern protects `int` operands, not arbitrary `long` operands.
- Normalize negative prefix sums before using them as keys.
- `modulus == 1` makes every normalized result zero.
- Modular division is not ordinary integer division.
- Reducing values loses the exact original magnitude.
- A hash collision is possible even with a large prime modulus.

## Interview follow-ups

**Why reduce after every operation?** The equivalence laws preserve the final remainder while keeping intermediate values bounded. The chosen representation must still be wide enough for one multiply or add before reduction.

**When is `Math.floorMod` preferable?** It expresses the nonnegative-result contract directly and avoids overflow in the double-remainder normalization pattern.

**How would you multiply under a very large `long` modulus?** Use `BigInteger`, a carefully proven add-and-double algorithm, or platform support for wider multiplication. The simple `long` product is not safe.

## Quick Check

1. **[Foundation]** What does `-14 % 5` return in Java, and what is its normalized modulo?
2. **[Interview Core]** Why must multiplication be widened before `%`?
3. **[Interview Core]** Why do equal prefix remainders identify a divisible subarray?
4. **[Interview Core]** What happens when the modulus is one?
5. **[SDE-2 Follow-up]** Why is modular hash equality insufficient proof of string equality?

## Coding Practice

1. **[Foundation]** Normalize a negative remainder for a positive modulus.
2. **[Interview Core]** Implement safe modular addition, subtraction, and multiplication for `int` operands.
3. **[Interview Core]** Compute a huge decimal string modulo an `int`.
4. **[Interview Core]** Implement fast modular exponentiation for a nonnegative exponent.
5. **[Interview Core]** Count subarrays whose sum is divisible by `k`.
6. **[SDE-2 Follow-up]** Implement a circular-array lookup that supports negative logical indexes.

## Debugging Task

**[Interview Core]** Identify both defects and repair them under a positive-modulus contract:

```java
static int multiply(int left, int right, int modulus) {
    return (left * right) % modulus;
}
```

Test large positive operands, one negative operand, modulus one, and invalid modulus values.

## Interview Extension

**[SDE-2 Follow-up]** Design a prefix-remainder component for a stream of signed values. Define update, query, reset, overflow, modulus, concurrency, memory-bound, and checkpoint behavior. Explain what can and cannot be reconstructed from stored remainders.
