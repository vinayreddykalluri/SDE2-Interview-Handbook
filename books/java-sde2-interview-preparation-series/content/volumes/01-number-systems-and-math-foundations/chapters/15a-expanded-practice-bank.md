# Chapter 15A: Expanded Practice Bank and SDE-2 Follow-Ups

This bank completes the requested assessment depth without mixing answers into the question flow. It extends Chapter 16 to forty conceptual questions, at least thirty Java-specific questions, twenty-five code-output questions, twenty-five debugging tasks, twenty algorithmic follow-ups, and ten interviewer discussion chains across Part B.

Use labels as priorities, not as judgments about difficulty. Attempt Foundation items first, then Interview Core, then SDE-2 Follow-up. Optional recreational number properties remain low priority.

## 15A.1 Ten additional conceptual questions

These extend Chapter 16's C01-C30.

### C31

What is the difference between a number, a digit, a sign, and a textual representation?

### C32

Why can leading zeros matter in a `String` even though they do not change an integer value?

### C33

Why does half-reversal make a numeric palindrome check safer than full reversal?

### C34

Why does a repeated-prime-query workload justify a sieve instead of repeated square-root checks?

### C35

Why can factor enumeration stop at the square root, and how is a perfect square handled?

### C36

Why is zero an identity for GCD reduction but an absorbing value for LCM reduction?

### C37

Why do trailing zeros in a factorial count factors of five?

### C38

How can logarithms count factorial digits without constructing the factorial?

### C39

What condition determines whether a modular inverse exists?

### C40

Why is a binary length calculation different from counting set bits?

## 15A.2 Thirty Java-specific retrieval questions

Answer aloud in one or two sentences before writing code.

1. Why is `long result = 100000 * 100000;` already wrong?
2. Which literal suffix makes an integer operand `long`?
3. Why does `(long) (a * b)` cast too late?
4. What type evaluates `byte + byte`?
5. When should `Math.toIntExact` replace a cast?
6. What does `Math.multiplyExact` do on overflow?
7. Why can `Math.abs(Integer.MIN_VALUE)` remain negative?
8. What is the matching `Long.MIN_VALUE` trap?
9. What sign can Java's `%` result have?
10. What range does `Math.floorMod(value, positiveModulus)` return?
11. Why can `(a, b) -> a - b` violate comparator ordering?
12. Why is `left + (right - left) / 2` contract-dependent?
13. Which method parses a `long` in a supplied radix?
14. What happens when a digit is invalid for `Integer.parseInt(text, radix)`?
15. What does `Character.digit('F', 16)` return?
16. What does `Character.forDigit(15, 16)` return?
17. Why is `char` numeric but not a general integer-storage choice?
18. Why is `0.1 + 0.2 == 0.3` unreliable?
19. Why is `BigDecimal` usually unnecessary in integer DSA problems?
20. When is `BigInteger` the correct representation rather than a workaround?
21. What is the difference between `==` and `.equals` for boxed integers?
22. What can null unboxing throw?
23. Why is `Double.NaN == Double.NaN` false?
24. How are `int` shift distances masked?
25. How are `long` shift distances masked?
26. What is the difference between `>>` and `>>>`?
27. Why does `1 << 31` not produce a positive mathematical 2^31?
28. What does `Long.numberOfLeadingZeros` help compute?
29. Why should numeric-string validation and arithmetic use the same character contract?
30. Why can a `long` product overflow even when both operands were safely normalized modulo a very large modulus?

## 15A.3 Five additional code-output questions

These extend Chapter 16's O01-O20. Predict the output or exception without running the code.

### O21

```java
System.out.println(100_000 * 100_000);
System.out.println(100_000L * 100_000);
```

### O22

```java
System.out.println(Math.floorMod(-17, 5));
System.out.println(-17 % 5);
```

### O23

```java
System.out.println(Long.SIZE - Long.numberOfLeadingZeros(1024));
System.out.println(Long.bitCount(1024));
```

### O24

```java
System.out.println(Character.digit('g', 16));
System.out.println(Character.forDigit(15, 16));
```

### O25

```java
System.out.println(Math.multiplyExact(Long.MAX_VALUE, 2));
```

## 15A.4 Five additional debugging tasks

These extend Chapter 16's D01-D20. For each: identify the bug, explain it, correct it, and state affected edge cases.

### D21: Sieve-bound overflow

```java
for (int prime = 2; prime * prime <= limit; prime++) {
    // mark multiples
}
```

### D22: Numeric-string subtraction sign bug

```java
String subtract(String left, String right) {
    return subtractMagnitudes(left, right); // assumes left >= right
}
```

### D23: Trailing-zero undercount

```java
long trailingZeros(int n) {
    return n / 5;
}
```

### D24: Empty array reduction

```java
long gcdArray(long[] values) {
    long result = values[0];
    for (long value : values) result = gcd(result, value);
    return result;
}
```

### D25: Modular inverse assumption

```java
long inverse(long value, long modulus) {
    return powerModulo(value, modulus - 2, modulus);
}
```

## 15A.5 Ordered implementation ladder

The following inventory gives the complete requested volume of practice. Some items also appear as worked examples in earlier chapters; here they are ordered for independent retrieval.

### Thirty Foundation problems

1. **Foundation:** Count decimal digits, including zero.
2. **Foundation:** Sum decimal digits while ignoring sign.
3. **Foundation:** Multiply digits and preserve embedded-zero behavior.
4. **Foundation:** Return the minimum digit.
5. **Foundation:** Return the maximum digit.
6. **Foundation:** Count even digits.
7. **Foundation:** Count odd digits.
8. **Foundation:** Count occurrences of a target digit.
9. **Foundation:** Compute repeated digit sum.
10. **Foundation:** Reverse an integer under a documented fit assumption.
11. **Foundation:** Check a numeric palindrome by full reversal.
12. **Foundation:** Identify an Armstrong number.
13. **Foundation:** Identify a Strong number.
14. **Foundation:** Identify a Perfect number.
15. **Foundation:** Compute factorial through 20.
16. **Foundation:** Convert nonnegative decimal to binary.
17. **Foundation:** Parse a valid binary string.
18. **Foundation:** Validate binary digits.
19. **Foundation:** Convert decimal to hexadecimal.
20. **Foundation:** Parse hexadecimal.
21. **Foundation:** Apply divisibility rules for 2, 5, and 10.
22. **Foundation:** Apply divisibility rules for 3 and 9.
23. **Foundation:** Apply the alternating-sum rule for 11.
24. **Foundation:** List factor pairs.
25. **Foundation:** Count factors.
26. **Foundation:** Sum factors.
27. **Foundation:** Check primality.
28. **Foundation:** Compute GCD iteratively.
29. **Foundation:** Compute LCM with a zero contract.
30. **Foundation:** Check a positive power of two.

### Thirty Interview Core problems

1. **Interview Core:** Reverse `int` with strict pre-operation checks.
2. **Interview Core:** Check palindrome by reversing only half.
3. **Interview Core:** Parse binary to `long` with overflow detection.
4. **Interview Core:** Parse huge binary with `BigInteger`.
5. **Interview Core:** Format a signed value in base 2-36.
6. **Interview Core:** Parse a signed base-2-36 value safely.
7. **Interview Core:** Validate signs and digits for a supplied base.
8. **Interview Core:** Convert arbitrary precision between bases.
9. **Interview Core:** Normalize a signed decimal string.
10. **Interview Core:** Compare signed huge decimal strings.
11. **Interview Core:** Add signed huge decimal strings.
12. **Interview Core:** Subtract signed huge decimal strings.
13. **Interview Core:** Multiply a numeric string by one digit.
14. **Interview Core:** Stream a huge-number remainder.
15. **Interview Core:** Test huge-number divisibility by 9.
16. **Interview Core:** Test huge-number divisibility by 11.
17. **Interview Core:** Return sorted factors in O(sqrt(n)).
18. **Interview Core:** Prime-factorize by trial division.
19. **Interview Core:** Generate primes with a sieve.
20. **Interview Core:** Build prefix prime counts.
21. **Interview Core:** Reduce GCD across an array.
22. **Interview Core:** Reduce LCM across an array with exact checks.
23. **Interview Core:** Normalize negative modulo.
24. **Interview Core:** Add and subtract under modulo.
25. **Interview Core:** Compute fast exact power.
26. **Interview Core:** Compute modular power.
27. **Interview Core:** Check a perfect square without overflow.
28. **Interview Core:** Compute floor square root.
29. **Interview Core:** Count binary length.
30. **Interview Core:** Count factorial trailing zeros.

### Twenty SDE-2 Follow-up problems

1. **SDE-2 Follow-up:** Multiply unrestricted `long` values under modulo without overflow.
2. **SDE-2 Follow-up:** Compute a modular inverse with extended Euclid.
3. **SDE-2 Follow-up:** Explain inverse failure for non-coprime inputs.
4. **SDE-2 Follow-up:** Return factorial digit count without constructing the factorial.
5. **SDE-2 Follow-up:** Answer repeated factor-count queries from prime exponents.
6. **SDE-2 Follow-up:** Answer prime-range queries with prefix counts.
7. **SDE-2 Follow-up:** Reduce a signed fraction to canonical form.
8. **SDE-2 Follow-up:** Align repeated schedules with array LCM.
9. **SDE-2 Follow-up:** Group array values using a shared GCD.
10. **SDE-2 Follow-up:** Detect LCM overflow and return `BigInteger` when required.
11. **SDE-2 Follow-up:** Stream several moduli across one huge input.
12. **SDE-2 Follow-up:** Multiply two numeric strings.
13. **SDE-2 Follow-up:** Divide a numeric string by a small positive integer.
14. **SDE-2 Follow-up:** Convert a huge signed value between arbitrary bases.
15. **SDE-2 Follow-up:** Find the highest power of two not exceeding a capacity.
16. **SDE-2 Follow-up:** Find the next representable power-of-two capacity.
17. **SDE-2 Follow-up:** Compute an integer kth root with overflow-safe comparisons.
18. **SDE-2 Follow-up:** Compare two rational values without cross-product overflow.
19. **SDE-2 Follow-up:** Design a numeric parsing API with explicit error categories.
20. **SDE-2 Follow-up:** Defend when manual arithmetic, exact primitives, or `BigInteger` is the best contract.

## 15A.6 Twenty algorithmic follow-ups

Use these as interviewer changes after a working core solution.

1. The digit input changes from `int` to `Long.MIN_VALUE`.
2. Leading zeros must be preserved in output.
3. Reversal may not use a wider type.
4. Palindrome checking may inspect only half the digits.
5. Base conversion expands from base 2 to bases 2-36.
6. The source value no longer fits in `long`.
7. Numeric-string inputs may have signs and redundant zeros.
8. Numeric-string multiplication expands from one digit to another long string.
9. One prime query becomes one million bounded prime queries.
10. A factor list becomes only a factor count.
11. Factor counts are requested for many values with known prime factorizations.
12. Pairwise GCD becomes GCD of an array.
13. Pairwise LCM becomes schedule alignment over an array.
14. Modulo operands can approach `Long.MAX_VALUE`.
15. The exponent is large but nonnegative.
16. Modular division is requested.
17. Square root becomes integer kth root.
18. Factorial value is impossible to construct, but its zeros are requested.
19. Factorial digits are requested instead of its value.
20. A safe comparator must order boundary values and tolerate boxing policies.

## 15A.7 Five additional interviewer discussion chains

These extend Chapter 16's F01-F05.

### F06: Sieve service chain

Start with one prime check. Change to repeated queries, then prime counts in ranges, then a memory-constrained upper bound. Compare trial division, sieve, prefix counts, and the boundary with segmented processing.

### F07: Factorial metrics chain

Start with exact factorial. Increase `n` beyond `long`, ask for trailing zeros, then ask only for digit count. Explain why each requested output changes the representation and algorithm.

### F08: Array reduction chain

Start with pairwise GCD. Generalize to an array, allow zeros and negatives, then request LCM and overflow reporting. State identities, early exits, and type policy.

### F09: Modular division chain

Start with normalized remainder. Add modular multiplication, power, and inverse. Then use a composite modulus and explain why the inverse may not exist.

### F10: Numeric-string API chain

Start with nonnegative comparison. Add signs, canonical zero, addition, subtraction, multiplication by a digit, and invalid-input diagnostics. Separate parsing policy from arithmetic invariants.

## 15A.8 Stop point

Do not continue until you have answered C31-C40, J01-J30, O21-O25, and D21-D25. For implementation problems, compare behavior on zero, signs, boundaries, malformed text, and overflow before consulting the checkpoints.

## Part II - Delayed Checkpoints

## 15A.9 Conceptual checkpoints

- C31-C32: a number is a value; digits and signs are representation symbols; leading zeros can encode width even when value is unchanged.
- C33: half reversal never builds the full reversed magnitude and stops after enough information exists.
- C34: a sieve pays O(n log log n) once so repeated bounded lookups become O(1).
- C35: factors pair around the square root; count an equal square-root pair once.
- C36: `gcd(0, x) = abs(x)`, while `lcm(0, x) = 0`.
- C37: tens require a two and a five; factorials contain more twos, so fives limit the count.
- C38: `log10(n!)` converts a product to a sum; floor plus one gives decimal digits.
- C39: an inverse exists exactly when `gcd(value, modulus) = 1`.
- C40: binary length counts positions through the highest one-bit; population count counts how many one-bits appear.

## 15A.10 Java and code-output checkpoints

The Java retrieval answers should mention expression type before destination type, exact arithmetic exceptions, remainder normalization, radix validation, boxed/reference semantics, floating approximation, and fixed-width shift rules.

- O21 prints `1410065408` and then `10000000000`.
- O22 prints `3` and then `-2`.
- O23 prints `11` and then `1`.
- O24 prints `-1` and then `f`.
- O25 throws `ArithmeticException` before printing a value.

## 15A.11 Debugging checkpoints

- D21: use `prime <= limit / prime`; the proven guard makes `prime * prime` safe for the inner start.
- D22: normalize signs and magnitudes, select addition or subtraction, and canonicalize zero.
- D23: add `n/5 + n/25 + n/125 + ...` until the divisor exceeds `n`.
- D24: reject an empty array explicitly and start a GCD fold at zero.
- D25: exponent `modulus - 2` is justified only under a suitable prime-modulus theorem; general inversion requires extended Euclid and GCD one.

## 15A.12 Readiness check

- **Needs foundation work:** fewer than 24 of the 40 conceptual questions can be explained without notes.
- **Foundation ready:** at least 30 conceptual answers and 20 Foundation implementations are reliable.
- **Interview ready:** all Foundation items plus at least 24 Interview Core implementations pass boundary tests.
- **Strong SDE-2 readiness:** the core set is reliable and at least 12 follow-ups can be adapted with explicit contracts, complexity, and overflow policy.
