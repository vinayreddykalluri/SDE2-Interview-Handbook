# Chapter 9: Divisibility Rules Needed for Coding Interviews

Divisibility rules are small shortcuts with large interview value. They help you avoid parsing an enormous numeric string, recognize modular patterns, and design constant-memory scans. You do not need a catalog of obscure tricks. You need a short set of reliable rules and the ability to derive or implement them.

## The useful rules

| Divisor | Rule in base 10 | Typical interview use |
|---:|---|---|
| 2 | last digit is even | parity, even grouping |
| 3 | digit sum is divisible by 3 | large numeric strings |
| 4 | last two digits are divisible by 4 | suffix validation |
| 5 | last digit is 0 or 5 | decimal cycle reasoning |
| 6 | number is divisible by both 2 and 3 | combined constraints |
| 8 | last three digits are divisible by 8 | suffix validation |
| 9 | digit sum is divisible by 9 | checksums, large strings |
| 10 | last digit is 0 | decimal trailing-zero logic |
| 11 | alternating digit sum is divisible by 11 | large-string interview problem |

Zero is divisible by every nonzero integer because `0 = divisor * 0`. Divisibility by zero is undefined. A negative sign does not change divisibility.

## Why the suffix rules work

Every decimal integer can be separated into a high prefix and a short suffix. For divisor 4:

```text
number = prefix * 100 + lastTwoDigits
```

Because 100 is divisible by 4, the prefix term contributes no remainder. Only the final two digits matter. The same reasoning uses 1000 for divisor 8. Divisors 2, 5, and 10 depend only on the last digit because 10 is divisible by each of them.

This is also a reusable recognition pattern: if a power of the representation base is divisible by `d`, a fixed-length suffix is enough to test divisibility by `d`.

## Why digit sums work for 3 and 9

In remainder arithmetic, 10 leaves remainder 1 when divided by 3 or 9. Therefore 10, 100, 1000, and every higher power of 10 behave like 1 for those divisors.

For decimal digits `a`, `b`, and `c`:

```text
100a + 10b + c
```

has the same remainder modulo 3 or 9 as:

```text
a + b + c
```

The digit sum can still grow for a huge string, so a robust implementation stores the running remainder rather than an unbounded sum.

## Why the alternating sum works for 11

Ten leaves remainder -1 modulo 11. Its powers alternate between 1 and -1. Therefore a decimal number has the same divisibility-by-11 result as an alternating sum of its digits.

For `918082`:

```text
9 - 1 + 8 - 0 + 8 - 2 = 22
```

Since 22 is divisible by 11, so is 918082. Starting with plus or minus does not matter for the final zero test; it only negates the alternating sum.

## Integer implementations without absolute-value traps

For a normal `int`, the direct remainder test is the safest implementation:

```java
static boolean isDivisible(int value, int divisor) {
    if (divisor == 0) {
        throw new IllegalArgumentException("divisor must be nonzero");
    }
    return value % divisor == 0;
}
```

This works for negative values and `Integer.MIN_VALUE`; no `Math.abs` call is needed. The decimal rules are valuable when the number is supplied as text, when parsing is prohibited, or when the interviewer explicitly asks you to implement the rule.

For suffix rules on a signed `int`, `Math.floorMod(value, 100)` or `Math.floorMod(value, 1000)` provides a nonnegative suffix without calling `Math.abs`:

```java
boolean by4 = Math.floorMod(value, 100) % 4 == 0;
boolean by8 = Math.floorMod(value, 1000) % 8 == 0;
```

## Large-number Java implementation

The next class accepts an optional leading sign followed by at least one ASCII digit. Leading zeros are valid. It provides a universal streaming test and optimized rule-based tests for 9 and 11.

```java
public final class DivisibilityRules {
    private DivisibilityRules() {}

    public static boolean isDivisibleBy(String value, int divisor) {
        if (divisor <= 0) {
            throw new IllegalArgumentException("divisor must be positive");
        }
        int index = firstDigitIndex(value);
        int remainder = 0;

        for (; index < value.length(); index++) {
            int digit = asciiDigit(value.charAt(index));
            remainder = (int) ((remainder * 10L + digit) % divisor);
        }
        return remainder == 0;
    }

    public static boolean isDivisibleBy9(String value) {
        int index = firstDigitIndex(value);
        int remainder = 0;

        for (; index < value.length(); index++) {
            int digit = asciiDigit(value.charAt(index));
            remainder = (remainder + digit) % 9;
        }
        return remainder == 0;
    }

    public static boolean isDivisibleBy11(String value) {
        int index = firstDigitIndex(value);
        int alternatingRemainder = 0;
        boolean add = true;

        for (; index < value.length(); index++) {
            int digit = asciiDigit(value.charAt(index));
            int signedDigit = add ? digit : -digit;
            alternatingRemainder = Math.floorMod(
                    alternatingRemainder + signedDigit, 11);
            add = !add;
        }
        return alternatingRemainder == 0;
    }

    private static int firstDigitIndex(String value) {
        if (value == null || value.isEmpty()) {
            throw new IllegalArgumentException("value must contain digits");
        }
        int index = value.charAt(0) == '+' || value.charAt(0) == '-' ? 1 : 0;
        if (index == value.length()) {
            throw new IllegalArgumentException("sign requires digits");
        }
        return index;
    }

    private static int asciiDigit(char character) {
        if (character < '0' || character > '9') {
            throw new IllegalArgumentException("invalid decimal digit");
        }
        return character - '0';
    }

    public static void main(String[] args) {
        System.out.println(isDivisibleBy("-000120", 8)); // true
        System.out.println(isDivisibleBy9("999999999999999999")); // true
        System.out.println(isDivisibleBy11("918082")); // true
    }
}
```

`remainder * 10L` is evaluated in `long`. This matters when the caller supplies a divisor near `Integer.MAX_VALUE`. The product remains safely below about 21.5 billion. The optimized 9 and 11 methods keep their state bounded after every digit, so the running state cannot overflow even for the longest possible Java string.

## Implementing each common rule on a string

For one-off rule implementations, use these states:

- **2:** inspect whether the final digit is one of 0, 2, 4, 6, or 8.
- **3:** accumulate digit sum modulo 3.
- **4:** build at most the final two digits, then test `% 4 == 0`.
- **5:** inspect whether the final digit is 0 or 5.
- **6:** combine the tests for 2 and 3.
- **8:** build at most the final three digits, then test `% 8 == 0`.
- **9:** accumulate digit sum modulo 9.
- **10:** inspect whether the final digit is 0.
- **11:** alternate addition and subtraction, reducing modulo 11.

The generic streaming-remainder method is often simpler and supports every positive divisor. A specialized rule is most useful when the interviewer requests it, when it provides a clearer proof, or when only a tiny suffix must be inspected.

## Dry run: large divisibility by 11

For `"918082"`, the implementation reduces after each digit:

| Digit | Operation | Normalized remainder mod 11 |
|---:|---|---:|
| 9 | `0 + 9` | 9 |
| 1 | `9 - 1` | 8 |
| 8 | `8 + 8` | 5 |
| 0 | `5 - 0` | 5 |
| 8 | `5 + 8` | 2 |
| 2 | `2 - 2` | 0 |

The zero remainder proves divisibility. The method never parses the six-digit value, so the same trace pattern works for six million digits.

## Complexity

For n digits, the generic, by-9, and by-11 scans take O(n) time and O(1) auxiliary space. A suffix test for 2, 4, 5, 8, or 10 can take O(1) arithmetic time after validation, but full validation still costs O(n) when input is untrusted. Be explicit about whether validation is part of the method's responsibility.

## Edge cases and common mistakes

- Zero is divisible by every nonzero divisor.
- Negative values have the same divisibility as their magnitudes.
- Never test divisibility by zero.
- Do not call `Math.abs(Integer.MIN_VALUE)` to handle signs.
- A sign alone is not a number.
- Leading zeros do not change divisibility.
- State whether only ASCII digits are accepted. `Character.digit` supports a wider set.
- Do not let an unbounded digit sum overflow; reduce modulo the divisor as you scan.
- Divisible by 6 means divisible by both 2 and 3, not either one.
- For 4 and 8, fewer than two or three digits are simply the whole number.

## Interview follow-ups

**Why use a rule instead of `%`?** If the number already fits a primitive, `%` is the clearest test. Rules matter for numeric strings, manual reasoning, and deriving streaming state.

**Can the same streaming method work in another base?** Yes. Replace 10 with the source base: `remainder = (remainder * base + digit) % divisor`, with safe intermediate arithmetic.

**Can processing be parallelized?** Chunks can be combined using their lengths and powers of ten modulo the divisor, but the added coordination is unnecessary for ordinary interview inputs. The sequential O(n) scan is usually best.

## Quick Check

1. **[Foundation]** Which decimal suffix is sufficient for divisibility by 8?
2. **[Foundation]** Why does a negative sign not affect divisibility?
3. **[Interview Core]** Why is digit sum valid for both 3 and 9?
4. **[Interview Core]** What property of 10 produces the alternating-sum rule for 11?
5. **[SDE-2 Follow-up]** Why can input validation change an apparent O(1) suffix test to O(n)?

## Coding Practice

1. **[Foundation]** Implement the rules for 2, 5, and 10 on a validated string.
2. **[Interview Core]** Test a huge number for divisibility by 4 and 8 without full parsing.
3. **[Interview Core]** Implement divisibility by 9 for an optional signed numeric string.
4. **[Interview Core]** Implement divisibility by 11 using bounded running state.
5. **[SDE-2 Follow-up]** Generalize the streaming test to bases 2 through 36.

## Debugging Task

**[Interview Core]** This implementation eventually fails on sufficiently long input. Explain why and repair it without using `BigInteger`.

```java
static boolean by9(String value) {
    int sum = 0;
    for (int index = 0; index < value.length(); index++) {
        sum += value.charAt(index) - '0';
    }
    return sum % 9 == 0;
}
```

Also add null, empty, sign, and invalid-character behavior to the contract.

## Interview Extension

**[SDE-2 Follow-up]** Design a streaming validation component for a large uploaded numeric file. Define size limits, character encoding, optional sign handling, divisibility checks computed in one pass, error reporting, cancellation, and memory bounds.
