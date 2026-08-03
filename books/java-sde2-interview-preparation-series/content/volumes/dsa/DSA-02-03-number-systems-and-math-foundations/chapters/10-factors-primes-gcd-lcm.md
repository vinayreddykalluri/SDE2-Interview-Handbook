# Chapter 10: Factors, Primes, GCD, and LCM

Factor and divisor problems appear in array grouping, fraction reduction, cycle alignment, repeated schedules, and number-property questions. The required mathematics is small. The interview challenge is writing bounded loops, defining behavior for zero and negatives, and preventing overflow in conditions and products.

## Core definitions

- A **factor** or **divisor** of `n` divides `n` with remainder zero.
- A **multiple** of `n` has the form `n * k` for an integer `k`.
- A **prime** is an integer greater than one with exactly two positive factors: 1 and itself.
- A **composite** is an integer greater than one that is not prime.
- The **greatest common divisor**, GCD, is the greatest nonnegative integer dividing both values.
- The **least common multiple**, LCM, is the least nonnegative integer that is a multiple of both values.

One is neither prime nor composite. Negative integers can have signed factors, but interview algorithms usually discuss positive factors of a positive magnitude. State that contract instead of letting sign behavior emerge accidentally.

## Prime checking up to the square root

If `n = a * b` and both factors were greater than `sqrt(n)`, their product would exceed `n`. Therefore every composite positive integer has at least one factor no greater than its square root.

This loop is risky for large `long` values:

```java
for (long divisor = 2; divisor * divisor <= n; divisor++) {
    // divisor * divisor can overflow
}
```

Use division instead:

```java
for (long divisor = 2; divisor <= n / divisor; divisor++) {
    // safe when n is positive
}
```

After checking 2, skip all other even candidates. This improves constants without changing O(sqrt(n)) time.

## Factor pairs

![Factor pairs meet at the square root, so the scan covers every factor without traversing to n.](content/volumes/dsa/DSA-02-03-number-systems-and-math-foundations/assets/14-factor-pairs.png)

Factors arrive in pairs. If `divisor` divides `n`, then `n / divisor` is the matching factor. Scanning only to the square root finds every pair. When `n` is a perfect square, the two factors are equal and must be counted once.

To print factors in sorted order without sorting all results, collect small factors during the ascending scan and collect their large partners separately. Append the large partners in reverse order.

The same scan can count or sum factors. For a non-square divisor, add both `divisor` and `n / divisor`; for a perfect-square root, add it once. The O(n) baseline checks every candidate. Pair enumeration reduces the scan to O(sqrt(n)) time, with O(1) state for a count or sum and O(number of factors) output space for a list.

## Repeated prime queries: the sieve

![The Sieve of Eratosthenes marks composite values and retains primes through a chosen limit.](content/volumes/dsa/DSA-02-03-number-systems-and-math-foundations/assets/15-sieve-process.png)

A single primality query is well served by an O(sqrt(n)) check. Many queries over the same bounded range justify preprocessing. The Sieve of Eratosthenes keeps a boolean composite array, visits each unmarked prime `p`, and marks multiples starting at `p * p` because smaller multiples already have a smaller prime factor.

Use `p <= limit / p` as the outer guard. Then `p * p` is representable for an `int limit`. The sieve costs O(n log log n) time and O(n) space, after which primality lookup is O(1). Counting or listing primes is a linear pass over the completed array. Segmented sieve is an optional advanced technique and is outside this book's core path.

## Euclid's GCD algorithm

![Euclid's algorithm preserves the common divisors while replacing a pair with its remainder.](content/volumes/dsa/DSA-02-03-number-systems-and-math-foundations/assets/07-euclidean-gcd-process.png)

For nonnegative `a` and `b`, replacing `(a, b)` with `(b, a % b)` preserves the common divisors. Repeat until the second value is zero. The first value is then the GCD.

```text
gcd(48, 18)
gcd(18, 12)
gcd(12, 6)
gcd(6, 0) = 6
```

The algorithm takes O(log(min(a, b))) iterations in the usual fixed-width analysis and O(1) auxiliary space.

## LCM through GCD

For nonzero integers:

```text
abs(a * b) = gcd(a, b) * lcm(a, b)
```

Do not multiply first. Divide by the GCD before multiplying to reduce overflow risk:

```java
long lcm = Math.abs((a / gcd) * b);
```

That expression is safe only if its evaluated types and final mathematical LCM fit. Widen operands before multiplication and use exact arithmetic when the input type does not provide a proven bound.

## Compiling Java toolkit

This implementation uses `long` for positive primality and factor operations. Factor listing requires `n > 0`; prime factorization requires `n >= 2`. GCD and LCM accept any `int`, including `Integer.MIN_VALUE`, by widening before taking magnitudes.

```java
import java.util.ArrayList;
import java.util.List;

public final class NumberTheoryBasics {
    private NumberTheoryBasics() {}

    public static boolean isPrime(long value) {
        if (value < 2) return false;
        if (value == 2) return true;
        if (value % 2 == 0) return false;

        for (long divisor = 3; divisor <= value / divisor; divisor += 2) {
            if (value % divisor == 0) return false;
        }
        return true;
    }

    public static List<Long> factors(long value) {
        if (value <= 0) {
            throw new IllegalArgumentException("value must be positive");
        }
        List<Long> smaller = new ArrayList<>();
        List<Long> larger = new ArrayList<>();

        for (long divisor = 1; divisor <= value / divisor; divisor++) {
            if (value % divisor != 0) continue;
            smaller.add(divisor);
            long partner = value / divisor;
            if (partner != divisor) larger.add(partner);
        }

        for (int index = larger.size() - 1; index >= 0; index--) {
            smaller.add(larger.get(index));
        }
        return List.copyOf(smaller);
    }

    public static long countFactors(long value) {
        if (value <= 0) {
            throw new IllegalArgumentException("value must be positive");
        }
        long count = 0;
        for (long divisor = 1; divisor <= value / divisor; divisor++) {
            if (value % divisor == 0) {
                count += divisor == value / divisor ? 1 : 2;
            }
        }
        return count;
    }

    public static List<Long> primeFactorization(long value) {
        if (value < 2) {
            throw new IllegalArgumentException("value must be at least two");
        }
        List<Long> factors = new ArrayList<>();
        long remaining = value;

        while (remaining % 2 == 0) {
            factors.add(2L);
            remaining /= 2;
        }
        for (long divisor = 3;
                divisor <= remaining / divisor;
                divisor += 2) {
            while (remaining % divisor == 0) {
                factors.add(divisor);
                remaining /= divisor;
            }
        }
        if (remaining > 1) factors.add(remaining);
        return List.copyOf(factors);
    }

    public static long gcd(int left, int right) {
        long a = Math.abs((long) left);
        long b = Math.abs((long) right);
        while (b != 0) {
            long remainder = a % b;
            a = b;
            b = remainder;
        }
        return a;
    }

    public static long lcm(int left, int right) {
        if (left == 0 || right == 0) return 0;
        long divisor = gcd(left, right);
        long dividedFirst = (long) left / divisor;
        return Math.abs(dividedFirst * (long) right);
    }

    public static void main(String[] args) {
        System.out.println(isPrime(2_147_483_647L));
        System.out.println(factors(36));
        System.out.println(countFactors(36));
        System.out.println(primeFactorization(756));
        System.out.println(gcd(48, -18));
        System.out.println(lcm(Integer.MIN_VALUE, 2));
    }
}
```

The `int`-input LCM product is safe in `long`: the largest possible product of two `int` magnitudes after division is below `Long.MAX_VALUE`. If the method accepted two `long` values, neither that argument nor `Math.abs` would be sufficient.

## Dry runs

### Prime check for 97

97 is at least two and is not even. Test odd divisors while `divisor <= 97 / divisor`:

- 3 does not divide 97.
- 5 does not divide 97.
- 7 does not divide 97.
- The next divisor, 9, is greater than `97 / 9`, so the loop stops.

No factor at or below the square root exists, so 97 is prime.

### Factors of 36

The scan finds pairs `(1, 36)`, `(2, 18)`, `(3, 12)`, `(4, 9)`, and `(6, 6)`. The square-root factor 6 is added once. Reversing the stored larger partners yields:

```text
[1, 2, 3, 4, 6, 9, 12, 18, 36]
```

### Prime factorization of 756

Repeated division produces `2, 2, 3, 3, 3, 7`. Their product is 756. Reducing `remaining` as factors are removed also reduces the upper bound for later trial divisors.

## Complexity

| Operation | Time | Auxiliary space |
|---|---:|---:|
| Prime check | O(sqrt(n)) | O(1) |
| List or count factors | O(sqrt(n)) | O(k) output or O(1) for count |
| Trial-division factorization | O(sqrt(n)) worst case | O(k) output |
| Euclidean GCD | O(log(min(a, b))) | O(1) |
| LCM after GCD | O(log(min(a, b))) | O(1) |

Here `k` is the number of returned factors. Fixed-width division is treated as O(1) in normal interview analysis.

## Zero, negatives, and unrepresentable magnitudes

This book uses `gcd(0, 0) = 0` as a programming convention, `gcd(a, 0) = abs(a)`, and `lcm(a, 0) = 0`. State these choices because some mathematical contexts leave `gcd(0, 0)` undefined.

A nonnegative `long` cannot represent the magnitude of `Long.MIN_VALUE`, which is 2^63. Therefore a method returning nonnegative `long` cannot support every pair of `long` inputs. Options are to reject that boundary, return `BigInteger`, or use a different result contract:

```java
java.math.BigInteger gcd = java.math.BigInteger.valueOf(leftLong).abs()
        .gcd(java.math.BigInteger.valueOf(rightLong).abs());
```

## Edge cases and common mistakes

- Values below two are not prime.
- One is neither prime nor composite.
- Use `divisor <= value / divisor`, not an overflowing square.
- Count a perfect-square factor pair once.
- Do not scan all candidates to `n - 1` for primality.
- Skip zero before dividing in LCM.
- Divide by GCD before multiplying for LCM.
- Widen an `int` before `Math.abs` to handle `Integer.MIN_VALUE`.
- Define whether factor methods accept negatives and what they return.
- Trial division is appropriate here; it is not a high-performance factorization method for huge arbitrary-precision values.

## Interview follow-ups

**Why does Euclid's algorithm terminate?** The nonnegative second argument becomes the remainder, which is strictly smaller than the previous divisor whenever it is nonzero.

**Can prime checking be faster?** Sieves are better when answering many bounded primality queries. Probabilistic or advanced tests matter for much larger values, but they are outside ordinary DSA interview scope.

**How do you count factors from prime exponents?** If `n = p^a * q^b`, each divisor chooses exponents independently, so the positive factor count is `(a + 1)(b + 1)`. This is useful after factorization.

## Quick Check

1. **[Foundation]** Why is one not prime?
2. **[Interview Core]** Why is scanning through the square root sufficient for factor discovery?
3. **[Interview Core]** Why can `divisor * divisor <= value` be unsafe?
4. **[Interview Core]** Why should LCM divide before multiplying?
5. **[SDE-2 Follow-up]** Why can no nonnegative-`long` GCD API cover every pair of `long` inputs?

## Coding Practice

1. **[Foundation]** Check whether a positive `long` is prime.
2. **[Interview Core]** Print positive factors in sorted order without sorting the full result.
3. **[Interview Core]** Count positive factors without materializing them.
4. **[Interview Core]** Return prime factors with multiplicity.
5. **[Interview Core]** Implement GCD and overflow-aware LCM for `int` inputs.
6. **[SDE-2 Follow-up]** Answer many prime queries with a sieve under a documented upper bound.

## Debugging Task

**[Interview Core]** Find the boundary and performance defects:

```java
static boolean prime(int value) {
    for (int divisor = 2; divisor * divisor <= value; divisor++) {
        if (value % divisor == 0) return false;
    }
    return true;
}
```

Repair behavior for negative values, zero, one, two, large primes, and multiplication overflow.

## Interview Extension

**[SDE-2 Follow-up]** Design a service method that aligns two recurring schedules using LCM. Define units, zero/negative input behavior, overflow policy, maximum horizon, time-zone boundaries, and what happens when the next alignment cannot be represented.
