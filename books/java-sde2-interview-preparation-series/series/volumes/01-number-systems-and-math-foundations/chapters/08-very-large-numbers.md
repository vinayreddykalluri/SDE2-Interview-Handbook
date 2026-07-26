# Chapter 8: Working with Very Large Numbers

Some interview inputs contain thousands or millions of decimal digits. Parsing such an input into `long` is not an optimization; it is a correctness bug because most valid inputs do not fit. The central technique is to process the string one digit at a time while retaining only the state the problem requires.

## Recognition signals

Use digit-by-digit processing when:

- the input is explicitly described as a numeric string;
- its length is larger than 18 or 19 decimal digits;
- the task asks only for a remainder or divisibility result;
- addition or comparison must work without arbitrary-precision libraries; or
- the interviewer wants the manual algorithm rather than `BigInteger`.

Before coding, define the input contract. Decide whether an empty string, leading sign, leading zeros, whitespace, or non-ASCII digits are valid. Do not silently choose different rules in validation and calculation.

![Streaming a huge numeric string keeps only the bounded state required by the problem.](series/volumes/01-number-systems-and-math-foundations/assets/17-large-numeric-string-traversal.png)

Use a manual algorithm when the interviewer asks for the invariant or forbids arbitrary-precision libraries. Use `BigInteger` when the application contract genuinely requires general arithmetic and the library is allowed. Comparison, addition, subtraction, multiplication by one digit, and modulo all have linear digit-by-digit solutions; none requires parsing the entire text into a primitive.

## Why primitive parsing fails

`Long.parseLong` accepts only values in the signed 64-bit range. A longer string throws `NumberFormatException` even if every character is a valid decimal digit.

```java
String input = "999999999999999999999999999999";
// long value = Long.parseLong(input); // NumberFormatException
```

Catching the exception and returning a default value loses the distinction between invalid input and a valid zero. Truncating digits changes the number. Converting to `double` loses exact integer precision. The representation must match the contract.

## Streaming remainder

Suppose the remainder of a processed prefix is `r`. Appending decimal digit `d` forms a new value `prefix * 10 + d`, so the new remainder is:

```text
newRemainder = (r * 10 + d) % modulus
```

Only the remainder is retained. If `modulus` is a positive `int`, using `long` for the multiply-add is safe because `r < modulus <= Integer.MAX_VALUE`, so `r * 10 + d` is below about 21.5 billion.

## A robust large-number toolkit

The following complete class uses two explicit contracts:

- divisibility and remainder accept one optional leading `+` or `-`;
- comparison and addition accept nonnegative ASCII decimal strings only.

```java
public final class LargeNumberAlgorithms {
    private LargeNumberAlgorithms() {}

    public static boolean isDivisibleBy9(String value) {
        return decimalRemainder(value, 9) == 0;
    }

    public static int decimalRemainder(String value, int modulus) {
        if (value == null) {
            throw new IllegalArgumentException("value must not be null");
        }
        if (modulus <= 0) {
            throw new IllegalArgumentException("modulus must be positive");
        }
        if (value.isEmpty()) {
            throw new IllegalArgumentException("value must not be empty");
        }

        int index = 0;
        boolean negative = false;
        char first = value.charAt(0);
        if (first == '+' || first == '-') {
            negative = first == '-';
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

        if (negative && remainder != 0) {
            return modulus - remainder;
        }
        return remainder;
    }

    public static int compareNonNegative(String left, String right) {
        String normalizedLeft = canonicalNonNegative(left);
        String normalizedRight = canonicalNonNegative(right);

        if (normalizedLeft.length() != normalizedRight.length()) {
            return Integer.compare(normalizedLeft.length(), normalizedRight.length());
        }
        return normalizedLeft.compareTo(normalizedRight);
    }

    public static String addNonNegative(String left, String right) {
        String normalizedLeft = canonicalNonNegative(left);
        String normalizedRight = canonicalNonNegative(right);

        int leftIndex = normalizedLeft.length() - 1;
        int rightIndex = normalizedRight.length() - 1;
        int carry = 0;
        StringBuilder reversed = new StringBuilder(
                Math.max(normalizedLeft.length(), normalizedRight.length()) + 1);

        while (leftIndex >= 0 || rightIndex >= 0 || carry != 0) {
            int leftDigit = leftIndex >= 0
                    ? normalizedLeft.charAt(leftIndex--) - '0' : 0;
            int rightDigit = rightIndex >= 0
                    ? normalizedRight.charAt(rightIndex--) - '0' : 0;
            int sum = leftDigit + rightDigit + carry;
            reversed.append((char) ('0' + sum % 10));
            carry = sum / 10;
        }
        return reversed.reverse().toString();
    }

    private static String canonicalNonNegative(String value) {
        if (value == null || value.isEmpty()) {
            throw new IllegalArgumentException("digits must not be null or empty");
        }
        for (int index = 0; index < value.length(); index++) {
            char character = value.charAt(index);
            if (character < '0' || character > '9') {
                throw new IllegalArgumentException("expected only ASCII digits");
            }
        }

        int firstNonZero = 0;
        while (firstNonZero < value.length() - 1
                && value.charAt(firstNonZero) == '0') {
            firstNonZero++;
        }
        return value.substring(firstNonZero);
    }

    public static void main(String[] args) {
        System.out.println(isDivisibleBy9("-000999999999999999999"));
        System.out.println(decimalRemainder("-14", 5)); // 1
        System.out.println(compareNonNegative("00042", "42")); // 0
        System.out.println(addNonNegative("999999999999", "1"));
    }
}
```

The returned remainder is normalized to the range `[0, modulus)`. For example, mathematical `-14 mod 5` is 1. Java's signed remainder `-14 % 5` is -4; the method deliberately exposes a mathematical-modulo contract.

## Dry run: divisibility by 9

For `"72918"`, track only the remainder modulo 9:

| Digit | Calculation | Remainder |
|---:|---|---:|
| 7 | `(0 * 10 + 7) % 9` | 7 |
| 2 | `(7 * 10 + 2) % 9` | 0 |
| 9 | `(0 * 10 + 9) % 9` | 0 |
| 1 | `(0 * 10 + 1) % 9` | 1 |
| 8 | `(1 * 10 + 8) % 9` | 0 |

The final remainder is zero, so the value is divisible by 9. Leading zeros do not change any remainder, and a leading minus sign does not change whether the remainder is zero.

## Dry run: adding numeric strings

Add `"999"` and `"27"` from right to left:

1. `9 + 7 = 16`: append 6 and carry 1.
2. `9 + 2 + 1 = 12`: append 2 and carry 1.
3. `9 + 0 + 1 = 10`: append 0 and carry 1.
4. No digits remain, so append the carry 1.
5. Reverse the accumulated characters to obtain `"1026"`.

Each intermediate digit sum is at most 19, so `int` is more than sufficient.

## Comparing large numeric strings

Lexicographic comparison alone is wrong before normalization: `"9"` is lexicographically greater than `"10"`, and `"0002"` has a greater length than `"10"` despite representing a smaller number. For nonnegative inputs:

1. validate and remove redundant leading zeros;
2. compare significant lengths;
3. if lengths match, compare lexicographically.

Signed comparison requires another layer: compare signs first, then magnitudes, and reverse the magnitude result when both numbers are negative. Keep that extension separate unless the contract requires it.

## When to use `BigInteger`

`BigInteger` is the right production or interview choice when arbitrary-precision values themselves are needed and libraries are allowed:

```java
java.math.BigInteger left = new java.math.BigInteger("999999999999999999999");
java.math.BigInteger right = java.math.BigInteger.ONE;
System.out.println(left.add(right));
```

Useful methods include `add`, `subtract`, `multiply`, `divideAndRemainder`, `compareTo`, `gcd`, and `mod`. `mod` requires a positive modulus and returns a nonnegative result; `remainder` follows signed remainder semantics.

Manual algorithms are still expected when the problem explicitly forbids parsing the full value, asks you to implement addition, or needs only a small remainder. Streaming also avoids retaining a second arbitrary-precision representation. For untrusted input, apply a length limit before expensive parsing or arithmetic; arbitrary precision does not mean unbounded resources are safe.

## Complexity

For n digits, divisibility and remainder take O(n) time and O(1) auxiliary space. Comparison takes O(n + m) time because both inputs are validated and normalized; substring creation may use O(n + m) additional character storage on modern Java implementations. Addition takes O(max(n, m)) time and O(max(n, m)) output space. `BigInteger` costs depend on the number of stored bits and the operation.

## Edge cases and common mistakes

- Reject `null`, empty input, and a sign with no digits.
- State whether whitespace is allowed. The sample methods reject it rather than trimming silently.
- Leading zeros are valid under this contract and normalize to one zero.
- Do not use `Character.isDigit` if the contract requires ASCII decimal digits; it recognizes many Unicode digits.
- Do not compare unnormalized numeric strings lexicographically.
- Do not prepend repeatedly to a `String`; append backward into `StringBuilder`, then reverse.
- Parsing into `double` is not exact large-integer arithmetic.
- A negative sign affects a normalized remainder but not divisibility.
- Validation is O(n); do not claim a comparison is O(1) merely because lengths differ after normalization.

## Interview follow-ups

**How would you process a million-digit value arriving as a stream?** Maintain the small remainder while reading chunks, validate each character, and enforce a byte/digit limit. Addition and exact comparison generally require retained digits or external storage.

**Why not always use `BigInteger`?** It may be forbidden, unnecessary for a remainder, more allocation-heavy, and less revealing of the requested algorithm. When arbitrary-precision results are the actual domain, it is often the clearest choice.

**How would signed string addition change the design?** Parse sign and canonical magnitude separately. Equal signs add magnitudes; different signs compare and subtract magnitudes. Canonicalize zero to a nonnegative representation.

## Quick Check

1. **[Foundation]** Why can a decimal string contain only valid digits yet fail `Long.parseLong`?
2. **[Interview Core]** What state is sufficient to compute divisibility by a small integer?
3. **[Interview Core]** Why must leading zeros be handled before comparing lengths?
4. **[Interview Core]** Why is each digit sum in manual addition safe in `int`?
5. **[SDE-2 Follow-up]** What resource controls belong around untrusted `BigInteger` input?

## Coding Practice

1. **[Foundation]** Remove redundant leading zeros while preserving one zero.
2. **[Interview Core]** Compute the remainder of an arbitrarily long decimal string.
3. **[Interview Core]** Add two nonnegative numeric strings.
4. **[Interview Core]** Compare two signed numeric strings canonically.
5. **[SDE-2 Follow-up]** Subtract two nonnegative numeric strings when the first is at least the second.
6. **[Challenge]** Multiply a numeric string by one decimal digit with validated input.

## Debugging Task

**[Interview Core]** Find the correctness and performance problems:

```java
static String add(String left, String right) {
    return String.valueOf(Long.parseLong(left) + Long.parseLong(right));
}
```

Define a precise input contract, repair the method without parsing the full values, and state its complexity.

## Interview Extension

**[SDE-2 Follow-up]** Design an API that accepts a large numeric identifier. Explain why arithmetic types may be the wrong representation, and define length, leading-zero, sign, normalization, storage, logging, and denial-of-service policies.
