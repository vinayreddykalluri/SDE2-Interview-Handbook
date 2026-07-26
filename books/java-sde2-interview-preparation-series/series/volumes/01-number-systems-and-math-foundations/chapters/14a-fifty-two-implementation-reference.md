# Chapter 14A: Fifty-Two Implementation Reference

Part A taught the ideas. Chapter 14 turned thirty high-frequency ideas into reusable patterns. This chapter closes the implementation gap: it indexes all fifty-two required solutions, explains the newly added techniques, and points to the compiling Java 21 companion.

The canonical implementations are in `code/NumberSystemsAlgorithms.java`; the boundary suite is in `code/NumberSystemsAlgorithmsTest.java`. Do not memorize all fifty-two methods. Memorize the small invariants they share: traverse a digit, accumulate a base, pair factors, reduce with GCD, normalize a remainder, halve an exponent, and check before an operation can overflow.

## 14A.1 Reading route

Use this chapter in three passes:

1. **Foundation:** implement digit statistics, factorial, base validation, and factor sums.
2. **Interview Core:** implement strict reversal, numeric-string arithmetic, sieve, array GCD/LCM, and bit length.
3. **SDE-2 Follow-up:** implement factorial counting formulas and modular inverse, then explain when each contract fails.

If an invariant is unfamiliar, return to the prerequisite chapter named in the index. If the invariant is clear but the code is slow or fragile, stay here and compare boundary policies.

## 14A.2 Complete implementation index

The four smaller tables are intentional: they remain readable on paper and prevent one oversized catalog from splitting unpredictably across pages.

### Implementations 1-13

| # | Required implementation | Compiling method | Level |
|---:|---|---|---|
| 1 | Count digits | `countDigits` | Foundation |
| 2 | Sum of digits | `sumDigits` | Foundation |
| 3 | Product of digits | `productDigits` | Foundation |
| 4 | Minimum digit | `minimumDigit` | Foundation |
| 5 | Maximum digit | `maximumDigit` | Foundation |
| 6 | Count occurrence of a digit | `countDigitOccurrences` | Foundation |
| 7 | Reverse integer | `reverseInt` | Interview Core |
| 8 | Strict overflow-safe reverse | `reverseIntStrict` | Interview Core |
| 9 | Palindrome number | `isPalindromeNumber` | Interview Core |
| 10 | Armstrong number | `isArmstrongNumber` | Foundation |
| 11 | Strong number | `isStrongNumber` | Foundation |
| 12 | Perfect number | `isPerfectNumber` | Optional Advanced |
| 13 | Factorial | `factorialExact` | Foundation |

### Implementations 14-26

| # | Required implementation | Compiling method | Level |
|---:|---|---|---|
| 14 | Decimal to binary | `decimalToBinary` | Foundation |
| 15 | Binary to decimal | `binaryStringToLong` | Interview Core |
| 16 | Decimal to generic base | `longToBase` | Interview Core |
| 17 | Generic base to decimal | `baseToLong` | Interview Core |
| 18 | Validate a number in a base | `isValidNumberInBase` | Interview Core |
| 19 | Hexadecimal to decimal | `hexadecimalToLong` | Foundation |
| 20 | Decimal to hexadecimal | `decimalToHexadecimal` | Foundation |
| 21 | Compare huge numeric strings | `compareNumericStrings` | Interview Core |
| 22 | Add huge numeric strings | `addNumericStrings` | Interview Core |
| 23 | Subtract huge numeric strings | `subtractNumericStrings` | SDE-2 Follow-up |
| 24 | Huge-number modulo | `largeNumberModulo` | Interview Core |
| 25 | Huge-number divisibility by 9 | `isDivisibleBy9` | Interview Core |
| 26 | Huge-number divisibility by 11 | `isDivisibleBy11` | Interview Core |

### Implementations 27-39

| # | Required implementation | Compiling method | Level |
|---:|---|---|---|
| 27 | List factors | `listFactors` | Interview Core |
| 28 | Count factors | `countFactors` | Interview Core |
| 29 | Sum factors | `sumFactors` | Interview Core |
| 30 | Prime check | `isPrime` | Interview Core |
| 31 | Prime factorization | `primeFactorization` | Interview Core |
| 32 | Sieve of Eratosthenes | `sievePrimes` | Interview Core |
| 33 | GCD | `gcd`, `gcdMagnitude` | Interview Core |
| 34 | LCM | `lcm`, `lcmMagnitude` | Interview Core |
| 35 | GCD of an array | `gcdOfArray` | Interview Core |
| 36 | LCM of an array | `lcmOfArray` | SDE-2 Follow-up |
| 37 | Normalize modulo | `normalizeModulo` | Interview Core |
| 38 | Modular addition | `addModulo` | Interview Core |
| 39 | Modular subtraction | `subtractModulo` | Interview Core |

### Implementations 40-52

| # | Required implementation | Compiling method | Level |
|---:|---|---|---|
| 40 | Modular multiplication | `multiplyModulo` | SDE-2 Follow-up |
| 41 | Fast exponentiation | `fastPowerExact` | Interview Core |
| 42 | Modular exponentiation | `powerModulo` | Interview Core |
| 43 | Perfect-square check | `isPerfectSquare` | Interview Core |
| 44 | Integer square root | `integerSquareRoot` | Interview Core |
| 45 | Power-of-two check | `isPowerOfTwo` | Foundation |
| 46 | Count number of bits | `countBits` | Interview Core |
| 47 | Trailing zeros in factorial | `trailingZerosInFactorial` | Interview Core |
| 48 | Number of digits in factorial | `digitsInFactorial` | SDE-2 Follow-up |
| 49 | Safe binary-search midpoint | `safeIndexMidpoint`, `signedMidpoint` | Interview Core |
| 50 | Overflow-safe comparator | `compareInts` | Interview Core |
| 51 | Safe integer multiplication | `safeMultiply` | Interview Core |
| 52 | Modular inverse | `modularInverse` | SDE-2 Follow-up |

Numeric-string multiplication by one digit is also included as `multiplyNumericStringByDigit`; it supports implementation 23 and is a useful bridge to full grade-school multiplication.

## 14A.3 Digit statistics and strict reversal

**Problem statement.** Find minimum/maximum digits, count a target digit, or reverse an `int` under a strict no-wider-accumulator policy.

**Why interviewers ask it.** These tasks expose whether you can separate the digit-traversal invariant from zero, sign, and overflow policies.

**Prerequisites.** Chapter 2's `% 10` and `/ 10` loop and Chapter 7's pre-operation boundary checks.

**Natural approach and limitation.** Converting to text is simple but changes the problem to character processing. A `long` accumulator makes reversal safe for `int`, but it does not demonstrate how to protect a fixed-width multiply-add.

**Optimal interview approach.** Keep the working value nonpositive when a magnitude might be `MIN_VALUE`. For strict reversal, reject the next digit before evaluating `reversed * 10 + digit`.

```java
static int minimumDigit(long value) {
    long remaining = value > 0 ? -value : value;
    int minimum = 9;
    do {
        minimum = Math.min(minimum, (int) -(remaining % 10));
        remaining /= 10;
    } while (remaining != 0);
    return minimum;
}

static int countDigitOccurrences(long value, int target) {
    if (target < 0 || target > 9) throw new IllegalArgumentException();
    long remaining = value > 0 ? -value : value;
    int count = 0;
    do {
        if ((int) -(remaining % 10) == target) count++;
        remaining /= 10;
    } while (remaining != 0);
    return count;
}
```

For strict positive overflow, `reversed > MAX_VALUE / 10` is already too large. At equality, the incoming digit must not exceed 7. The mirrored negative boundary permits -8 because `Integer.MIN_VALUE` ends in 8.

| State | `remaining` | incoming digit | decision |
|---|---:|---:|---|
| Safe prefix | 214,748,364 | 1 | append; result fits |
| Positive boundary | 214,748,364 | 8 | reject; exceeds MAX_VALUE |
| Negative boundary | -214,748,364 | -8 | append; equals MIN_VALUE |

**Complexity.** O(d) time and O(1) auxiliary space for d decimal digits.

**Edge cases.** Zero is one digit; signs are ignored for statistics; trailing zeros disappear during numeric reversal; `MIN_VALUE` must not be passed through same-type `abs`.

**Common mistakes.** A `while` loop skips zero, initialization of minimum to zero makes every positive input wrong, and checking overflow after the `int` operation is too late.

**Follow-ups.** Return all digit statistics in one traversal; reconstruct only half a palindrome; generalize the loop to another base.

## 14A.4 Factorial and its counting follow-ups

**Problem statement.** Compute `n!` when it fits, count trailing zeros of `n!`, or find its decimal digit count without constructing the value.

**Why interviewers ask it.** The three variants distinguish direct computation, factor counting, and logarithmic transformation.

**Prerequisites.** Multiplication, integer division, logarithm intuition, and explicit range contracts.

The natural factorial loop is optimal for a small exact result. `20!` fits in `long`; `21!` does not. The contract therefore accepts only 0 through 20 and begins at the identity `0! = 1`.

```java
static long factorialExact(int n) {
    if (n < 0 || n > 20) throw new IllegalArgumentException();
    long result = 1;
    for (int factor = 2; factor <= n; factor++) {
        result = Math.multiplyExact(result, factor);
    }
    return result;
}

static long trailingZeros(int n) {
    if (n < 0) throw new IllegalArgumentException();
    long zeros = 0;
    for (long divisor = 5; divisor <= n; divisor *= 5) {
        zeros += n / divisor;
        if (divisor > n / 5) break;
    }
    return zeros;
}
```

For `100!`, multiples of 5 contribute 20 factors of five and multiples of 25 contribute four additional factors, so the answer is 24. Factors of two are more plentiful and do not limit trailing zeros.

For digit count, use `floor(sum(log10(k), k=2..n)) + 1`. This is O(n) time and O(1) space. It is a counting technique, not a substitute when exact factorial digits are required.

**Edge cases.** `0!` and `1!` both have one digit and zero trailing zeros. Negative factorial is outside the integer interview contract.

**Common mistakes.** Returning zero for `0!`, using `int`, counting only `n / 5`, or constructing a huge `BigInteger` merely to call `toString().length()`.

**Follow-ups.** Compute the last nonzero digit, use a prefix table for repeated small queries, or discuss Stirling's approximation as optional theory.

## 14A.5 Base validation and huge-string arithmetic

**Problem statement.** Validate a signed representation in base 2-36, subtract two signed decimal strings, or multiply a signed decimal string by one digit.

**Why interviewers ask it.** These tasks test parsing contracts, carry/borrow state, leading-zero normalization, and the ability to avoid unsupported primitive parsing.

**Prerequisites.** Chapters 5 and 8.

Validation must happen before accumulation. An optional sign is allowed only at index zero and must be followed by at least one digit. `Character.digit(character, base)` is convenient for general Java text; the companion intentionally uses an explicit ASCII contract so accepted symbols are predictable.

For subtraction, normalize signs and magnitudes first. Equal signs become magnitude subtraction; different signs become magnitude addition. The companion reuses signed addition by negating the second canonical operand, which prevents a second, inconsistent sign engine.

```java
static String multiplyByDigit(String digits, int multiplier) {
    if (multiplier < 0 || multiplier > 9) throw new IllegalArgumentException();
    if (digits.equals("0") || multiplier == 0) return "0";
    int carry = 0;
    StringBuilder reversed = new StringBuilder(digits.length() + 1);
    for (int index = digits.length() - 1; index >= 0; index--) {
        int product = (digits.charAt(index) - '0') * multiplier + carry;
        reversed.append((char) ('0' + product % 10));
        carry = product / 10;
    }
    if (carry != 0) reversed.append(carry);
    return reversed.reverse().toString();
}
```

| Step for `999 x 7` | digit product + carry | output digit | next carry |
|---:|---:|---:|---:|
| rightmost 9 | 63 | 3 | 6 |
| middle 9 | 69 | 9 | 6 |
| leftmost 9 | 69 | 9 | 6 |
| final carry | - | 6 | 0 |

The result is `6993`. Time is O(n) and output space is O(n). Per-position arithmetic is bounded because `9 * 9 + 8` is at most 89.

**Edge cases.** Empty or sign-only text is invalid; canonical zero has no negative sign; leading zeros are removed from numeric results; subtraction must define which sign wins.

**Common mistakes.** Parsing to `long`, comparing unnormalized lengths, dropping the final carry, retaining `-0`, or allowing a digit equal to the base.

**Follow-ups.** Multiply two numeric strings, convert a huge binary string with `BigInteger`, or stream several remainders in one pass.

## 14A.6 Factor sums, sieve, and array GCD/LCM

**Problem statement.** Sum every factor, preprocess all primes to a limit, or reduce GCD/LCM across an array.

**Why interviewers ask it.** These variations test whether a pairwise invariant can become enumeration, preprocessing, or reduction.

**Prerequisites.** Chapter 10's factor pairs and Euclidean algorithm.

Factor sum mirrors factor count: for each divisor through the square root, add the divisor and its partner, but add a square root once. Use `Math.addExact` when the return contract is `long`.

For the sieve, mark `prime * prime`, `prime * prime + prime`, and so on. The outer condition `prime <= limit / prime` proves that the starting product fits. Preprocessing costs O(n log log n) time and O(n) space; each later primality query is O(1).

Array GCD is a fold:

```java
long result = 0;
for (long value : values) {
    result = gcd(result, value);
}
```

Zero is the identity because `gcd(0, x) = abs(x)`. Array LCM begins at one, but any zero makes the result zero. Each pair must divide by GCD before exact multiplication. Stop early after the result becomes zero.

**Complexity.** Factor sum is O(sqrt(n)); sieve is O(n log log n) time and O(n) space; array GCD is O(k log M) for k values bounded by magnitude M. Array LCM has the same number of reductions but may overflow much sooner.

**Edge cases.** Factor operations require positive input; sieve limits below two return an empty list; array contracts reject null and empty arrays; a GCD magnitude of 2^63 needs `BigInteger`.

**Common mistakes.** Double-counting the square root, starting sieve marks at `2 * p`, evaluating `p * p` before proving it fits, and multiplying before dividing in LCM.

**Follow-ups.** Prefix prime counts, divisor count from prime exponents, fraction reduction, or schedule alignment by array LCM.

## 14A.7 Bit length and modular inverse

### Count bits

**Problem statement.** Return the number of binary positions required to represent a nonnegative integer. This means binary length, not population count.

For zero, this book returns one because textual binary zero is `0`. For positive `value`, repeated unsigned right shift takes O(log value) time. Java's `Long.numberOfLeadingZeros` gives the same answer in a constant number of machine-level operations:

```java
static int countBits(long value) {
    if (value < 0) throw new IllegalArgumentException();
    return value == 0 ? 1 : Long.SIZE - Long.numberOfLeadingZeros(value);
}
```

Do not confuse bit length with `Long.bitCount`, which counts one-bits. Full bit-count patterns remain in the Bit Manipulation mini-book.

### Modular inverse

**Problem statement.** Find `x` such that `(a * x) mod m = 1`.

**Why interviewers ask it.** It tests whether the candidate knows that division under modulo is not ordinary integer division and that an inverse exists only under a GCD condition.

**Prerequisites.** GCD, normalized modulo, and the identity produced by extended Euclid.

The natural search checks every `x` from 1 to `m - 1`, which is O(m). Extended Euclid finds coefficients `x` and `y` satisfying:

```text
a*x + m*y = gcd(a, m)
```

When the GCD is one, reducing `x` modulo `m` gives the inverse. When the GCD is not one, no inverse exists.

For `a = 3` and `m = 11`:

```text
1 = 4*3 - 1*11
therefore x = 4, and (3*4) mod 11 = 1
```

The companion uses iterative extended Euclid with `BigInteger` coefficients. This preserves correctness when intermediate coefficients exceed `long`, while returning a `long` inverse because the positive modulus itself is a `long`.

**Complexity.** O(log m) Euclidean iterations and O(log m)-sized coefficient arithmetic.

**Edge cases.** Require `m > 1`; normalize negative `a`; reject `gcd(a, m) != 1`; never assume a prime modulus unless the problem states it.

**Common mistakes.** Using integer division, applying Fermat's theorem to a composite modulus, returning a negative coefficient without normalization, or multiplying unrestricted `long` values before modulo.

**Follow-ups.** Inverse of every value under a prime modulus, fraction reduction before modular division, and safe modular multiplication for the final verification.

## 14A.8 Boundary matrix and recall

| Input shape | Required response |
|---|---|
| `0` in a digit algorithm | Process one digit when the task counts or classifies digits |
| `MIN_VALUE` | Avoid same-type absolute value and negation |
| Huge numeric string | Validate and traverse; never parse the entire value to a primitive |
| Factor or prime loop | Use `factor <= value / factor` |
| Array LCM | Handle zero, divide first, then multiply exactly |
| Negative remainder | Normalize with a positive modulus |
| No modular inverse | Report the GCD failure; do not invent a quotient |
| Floating estimate | Verify with integer arithmetic when the answer is discrete |

### Quick check

1. Why does strict reversal check before the multiply-add?
2. Why do trailing zeros count factors of five rather than ten?
3. Why can a sieve start marking at `p * p`?
4. What identity makes zero the starting value for an array GCD?
5. What condition decides whether a modular inverse exists?

### Guided practice

1. **Foundation:** Trace minimum, maximum, and zero count for `-900507`.
2. **Interview Core:** Trace `1000000 - 1` with right-to-left borrows.
3. **Interview Core:** Draw the sieve states through 40.
4. **SDE-2 Follow-up:** Derive the inverse of 17 modulo 43 with extended Euclid.

### Independent coding

1. **Foundation:** Return sum, product, minimum, maximum, and digit count in one immutable result.
2. **Interview Core:** Implement strict reversal without `long` and test both int boundaries.
3. **Interview Core:** Implement signed numeric-string subtraction with canonical zero.
4. **Interview Core:** Return prefix prime counts after one sieve.
5. **Interview Core:** Reduce the GCD of an array and stop early at one.
6. **SDE-2 Follow-up:** Reduce array LCM with exact overflow reporting.
7. **SDE-2 Follow-up:** Return the trailing-zero count for `Integer.MAX_VALUE` without overflow.
8. **Optional Advanced:** Return a modular inverse using only documented bounded `long` inputs.

### Debugging task

The following code can overflow before it decides to stop:

```java
for (int prime = 2; prime * prime <= limit; prime++) {
    if (!composite[prime]) {
        for (int multiple = prime * prime;
                multiple <= limit;
                multiple += prime) {
            composite[multiple] = true;
        }
    }
}
```

Identify the unsafe expression, replace the outer condition with a division guard, and state why the inner starting product is then representable.

### Interview follow-up

The modulus is close to `Long.MAX_VALUE`, and the interviewer asks you to verify `(value * inverse) mod modulus == 1`. Explain why ordinary multiplication is unsafe and route the verification through the overflow-free modular multiplication method.

## 14A.9 Chapter summary

The complete Number Systems stage now has compiling references for all fifty-two required implementations. The additions do not create twenty-two unrelated tricks. They reuse seven ideas: bounded digit traversal, pre-operation overflow checks, carry/borrow arithmetic, factor pairing, preprocessing, associative reduction, and Euclidean identities.

When revising, name the contract and invariant before the method. That is the difference between recalling code and being able to adapt it under an SDE-2 follow-up.
