# Chapter 14: Thirty Core Interview Problem Patterns

Interview problems involving numbers become much easier when they are grouped by invariant instead of memorized as unrelated tricks. Digit problems repeatedly remove a base-10 digit. Base-conversion problems repeatedly multiply and add, or repeatedly divide and collect remainders. Factor problems search only through a square-root boundary. Large-number-string problems carry a small state while scanning characters. Overflow-safe problems validate an operation before trusting its fixed-width result.

This chapter is a catalog of thirty reusable core patterns and four closely related extensions. Chapter 14A expands the compiling companion to all fifty-two required implementations while preserving this shorter high-frequency learning route.

## 14.1 How to use the catalog

For each numbered pattern, rehearse this sequence:

1. State the input contract.
2. Name the recognition signal.
3. Describe the natural brute-force idea.
4. Improve it by identifying the invariant.
5. Write the optimal interview implementation.
6. Dry-run one normal case and one boundary.
7. State time and space complexity.
8. Name the failure modes before the interviewer asks.
9. Offer a realistic follow-up.

The implementations use Java 21 syntax but avoid distracting language features. They throw IllegalArgumentException for malformed input unless the common coding-platform contract calls for a sentinel, such as returning zero when a reversed int overflows.

## 14.2 Coverage map

| ID | Mandatory problem | Primary method |
|---:|---|---|
| 1 | Count digits | DigitPatterns.countDigits |
| 2 | Sum of digits | DigitPatterns.sumDigits |
| 3 | Reverse an integer safely | DigitPatterns.reverseIntOrZero |
| 4 | Palindrome number | DigitPatterns.isPalindrome |
| 5 | Armstrong number | DigitPatterns.isArmstrong |
| 6 | Strong number | DigitPatterns.isStrong |
| 7 | Binary string to decimal | BaseConversionPatterns.binaryToLong |
| 8 | Decimal to binary | BaseConversionPatterns.toBinary |
| 9 | Generic base to decimal | BaseConversionPatterns.baseToLong |
| 10 | Decimal to generic base | BaseConversionPatterns.longToBase |
| 11 | Large number divisible by 9 | LargeNumberPatterns.isDivisibleBy9 |
| 12 | Large number divisible by 11 | LargeNumberPatterns.isDivisibleBy11 |
| 13 | Prime check | NumericPropertyPatterns.isPrime |
| 14 | Print factors | NumericPropertyPatterns.factors |
| 15 | Count factors | NumericPropertyPatterns.factorCount |
| 16 | Prime factorization | NumericPropertyPatterns.primeFactors |
| 17 | GCD | NumericPropertyPatterns.gcd |
| 18 | LCM | NumericPropertyPatterns.safeLcm |
| 19 | Perfect square | NumericPropertyPatterns.isPerfectSquare |
| 20 | Integer square root | NumericPropertyPatterns.floorSquareRoot |
| 21 | Power of two | NumericPropertyPatterns.isPowerOfTwo |
| 22 | Fast exponentiation | NumericPropertyPatterns.safePower |
| 23 | Large numeric string modulo | LargeNumberPatterns.modulo |
| 24 | Add two numeric strings | LargeNumberPatterns.add |
| 25 | Compare two numeric strings | LargeNumberPatterns.compare |
| 26 | Safe multiplication | OverflowPatterns.safeMultiply |
| 27 | Overflow-safe midpoint | OverflowPatterns.safeMidpoint |
| 28 | Normalize negative modulo | NumericPropertyPatterns.normalizeModulo |
| 29 | Hexadecimal string to decimal | BaseConversionPatterns.hexToLong |
| 30 | Decimal to hexadecimal | BaseConversionPatterns.toHex |

## 14.3 Shared contracts

The catalog makes these contracts explicit:

- A decimal digit problem treats zero as a one-digit value.
- Digit aggregation uses the magnitude of a signed int, promoted to long before absolute value.
- Base-conversion strings represent nonnegative values and use bases 2 through 36.
- Generic digits use 0-9 and A-Z; input accepts either letter case.
- Large-number strings contain decimal digits only. Leading zeros are valid.
- Factor and LCM methods accept nonnegative values as documented.
- A modulus must be positive.
- A failed exact fixed-width operation is represented with OptionalLong.
- Java code never assumes that assigning an overflowed int expression to long repairs it.

## 14.4 Digit-traversal invariant

For a nonnegative decimal value n:

~~~text
last digit       = n % 10
remaining prefix = n / 10
~~~

Every iteration removes one digit, so a d-digit number takes O(d) time, which is O(log10(n)) for positive n. Interviewers often accept O(number of digits), which is the clearer statement.

### Pattern 1: Count digits

**Recognition signal:** The task asks for decimal length without converting to a string, or a later rule needs the number of digits.

**Brute force:** Convert to a string and count characters, adjusting for a sign. This is easy but allocates a string.

**Better and optimal approach:** Promote to long, take the magnitude, divide by 10 until zero, and define zero as one digit.

**Java solution:** DigitPatterns.countDigits.

**Complexity:** O(d) time and O(1) space.

**Dry run:** 40,502 becomes 4,050, 405, 40, 4, 0, so the count is 5.

**Edge cases:** Zero, negative input, and Integer.MIN_VALUE.

**Common mistake:** Math.abs(Integer.MIN_VALUE) remains negative. Promote before taking the magnitude.

**Follow-up:** Count digits in an arbitrary base b by repeatedly dividing by b.

### Pattern 2: Sum of digits

**Recognition signal:** Divisibility rules, repeated digit transformation, digital-root questions, or a checksum based on decimal digits.

**Brute force:** Convert to text and parse each one-character substring.

**Better and optimal approach:** Repeatedly add magnitude % 10 and divide the magnitude by 10.

**Java solution:** DigitPatterns.sumDigits.

**Complexity:** O(d) time and O(1) space.

**Dry run:** 5,074 produces 4 + 7 + 0 + 5 = 16.

**Edge cases:** The sum of digits of zero is zero; the sign is not a digit.

**Common mistake:** Letting a negative remainder contaminate the sum.

**Follow-up:** Repeatedly sum until one digit remains, then derive the digital-root formula.

### Pattern 3: Reverse an integer safely

**Recognition signal:** Digits must be reconstructed in reverse order under a fixed-width return type.

**Brute force:** Convert to a string, reverse it, parse it, and catch parsing overflow.

**Better approach:** Rebuild numerically with reversed = reversed * 10 + digit using a wider long.

**Optimal interview approach:** For an int input, a long accumulator is sufficient because the reversed magnitude has at most ten decimal digits. Apply the sign and range-check before narrowing. A stricter variant can guard before every multiply-add.

**Java solution:** DigitPatterns.reverseIntOrZero.

**Complexity:** O(d) time and O(1) space.

**Dry run:** -120 becomes digits 0, 2, 1; the magnitude rebuilds as 21 and the result is -21.

**Edge cases:** Zero, trailing zeros, Integer.MIN_VALUE, and a reversed value outside the int range.

**Common mistake:** Reversing in int and checking after overflow has already happened.

**Follow-up:** Return OptionalInt instead of the coding-platform sentinel zero.

### Pattern 4: Palindrome number

**Recognition signal:** Decimal digits must read identically from both ends.

**Brute force:** Convert to a string and compare mirrored characters.

**Better approach:** Reverse the entire number numerically and compare, but that can overflow.

**Optimal interview approach:** Reverse only the lower half of the digits. Stop when the original prefix is no larger than the reversed suffix.

**Java solution:** DigitPatterns.isPalindrome.

**Complexity:** O(d) time and O(1) space.

**Dry run:** 12,321 evolves from prefix 12,321 and suffix 0 to prefix 12 and suffix 123. For an odd digit count, remove suffix's middle digit: 12 == 123 / 10.

**Edge cases:** Negative values are false. Zero is true. A positive value ending in zero is false unless the value is zero.

**Common mistake:** Reversing the full int and introducing overflow.

**Follow-up:** Test a palindrome in base b.

### Pattern 5: Armstrong number

An Armstrong number equals the sum of each decimal digit raised to the number of digits. For example, 153 = 1^3 + 5^3 + 3^3.

**Recognition signal:** The property combines digit count with per-digit exponentiation.

**Brute force:** Convert to a string, use Math.pow on each character, and cast from double.

**Better and optimal approach:** Count digits once, traverse digits, and compute each small integer power with integer multiplication.

**Java solution:** DigitPatterns.isArmstrong.

**Complexity:** O(d^2) under the simple repeated-multiplication helper, or O(d log d) with exponentiation by squaring. Since int has at most ten digits, either is bounded and clear.

**Dry run:** 153 has three digits; 1 + 125 + 27 = 153.

**Edge cases:** This implementation defines the property only for nonnegative ints. Zero is an Armstrong number.

**Common mistake:** Trusting floating-point Math.pow for an exact integer equality.

**Follow-up:** Generalize to base b and state how the digit count changes.

### Pattern 6: Strong number

A Strong number equals the sum of the factorials of its decimal digits. For example, 145 = 1! + 4! + 5!.

**Recognition signal:** Each digit maps to a small reusable value from 0! through 9!.

**Brute force:** Recompute each factorial for every occurrence.

**Better and optimal approach:** Precompute the ten factorials once and perform a digit traversal.

**Java solution:** DigitPatterns.isStrong.

**Complexity:** O(d) time and O(1) extra space because the table has fixed size ten.

**Dry run:** 145 produces 1 + 24 + 120 = 145.

**Edge cases:** 0! is 1, so zero itself is not Strong under this definition. Negative values are rejected as false.

**Common mistake:** Initializing 0! to zero.

**Follow-up:** Find every Strong number in a range without recomputing factorials.

## 14.5 Compiling digit-pattern implementation

~~~java
public final class DigitPatterns {
    private static final int[] DIGIT_FACTORIAL = {
        1, 1, 2, 6, 24, 120, 720, 5_040, 40_320, 362_880
    };

    private DigitPatterns() {
    }

    public static int countDigits(int value) {
        long remaining = Math.abs((long) value);
        int count = 1;
        while (remaining >= 10) {
            remaining /= 10;
            count++;
        }
        return count;
    }

    public static int sumDigits(int value) {
        long remaining = Math.abs((long) value);
        int sum = 0;
        do {
            sum += (int) (remaining % 10);
            remaining /= 10;
        } while (remaining > 0);
        return sum;
    }

    public static long productDigits(int value) {
        long remaining = Math.abs((long) value);
        long product = 1;
        do {
            product *= remaining % 10;
            remaining /= 10;
        } while (remaining > 0);
        return product;
    }

    public static int reverseIntOrZero(int value) {
        long remaining = Math.abs((long) value);
        long reversed = 0;
        do {
            reversed = reversed * 10 + remaining % 10;
            remaining /= 10;
        } while (remaining > 0);

        long signed = value < 0 ? -reversed : reversed;
        if (signed < Integer.MIN_VALUE || signed > Integer.MAX_VALUE) {
            return 0;
        }
        return (int) signed;
    }

    public static boolean isPalindrome(int value) {
        if (value < 0 || value != 0 && value % 10 == 0) {
            return false;
        }

        int prefix = value;
        int reversedSuffix = 0;
        while (prefix > reversedSuffix) {
            reversedSuffix = reversedSuffix * 10 + prefix % 10;
            prefix /= 10;
        }
        return prefix == reversedSuffix
                || prefix == reversedSuffix / 10;
    }

    public static boolean isArmstrong(int value) {
        if (value < 0) {
            return false;
        }
        int digits = countDigits(value);
        int remaining = value;
        long sum = 0;
        do {
            int digit = remaining % 10;
            sum += integerPower(digit, digits);
            remaining /= 10;
        } while (remaining > 0);
        return sum == value;
    }

    public static boolean isStrong(int value) {
        if (value < 0) {
            return false;
        }
        int remaining = value;
        int sum = 0;
        do {
            sum += DIGIT_FACTORIAL[remaining % 10];
            remaining /= 10;
        } while (remaining > 0);
        return sum == value;
    }

    private static long integerPower(int base, int exponent) {
        long result = 1;
        for (int i = 0; i < exponent; i++) {
            result *= base;
        }
        return result;
    }

    public static void main(String[] args) {
        System.out.println(countDigits(Integer.MIN_VALUE)); // 10
        System.out.println(sumDigits(-5_074));              // 16
        System.out.println(productDigits(1_234));           // 24
        System.out.println(reverseIntOrZero(-120));         // -21
        System.out.println(isPalindrome(12_321));           // true
        System.out.println(isArmstrong(153));               // true
        System.out.println(isStrong(145));                  // true
    }
}
~~~

### Extension: Product of digits

Product of digits uses the same traversal. Zero needs deliberate handling: the number zero has one digit, zero, so its digit product is zero. Any number containing an internal zero also has product zero.

**Recognition signal:** A property multiplies independent decimal digits.

**Complexity:** O(d) time and O(1) space.

**Follow-up:** Decide how to detect overflow if the input is a very long decimal string rather than an int.

## 14.6 Base-conversion invariants

Converting from a base-b string to decimal uses Horner accumulation:

~~~text
result = result * base + nextDigit
~~~

Converting a nonnegative decimal value to base b uses repeated division:

~~~text
digit     = value % base
remaining = value / base
~~~

Remainders arrive from least significant to most significant, so they are reversed at the end.

### Pattern 7: Binary string to decimal

**Recognition signal:** A sequence of characters '0' and '1' represents a numeric value, not a decimal text.

**Brute force:** For each one bit, call Math.pow(2, position) and add a cast result.

**Better approach:** Scan right to left with a power variable, checking overflow.

**Optimal interview approach:** Scan left to right and apply result = result * 2 + bit, checking before each update.

**Java solution:** BaseConversionPatterns.binaryToLong.

**Complexity:** O(d) time and O(1) space.

**Dry run:** "1011" accumulates 1, 2, 5, 11.

**Edge cases:** Empty input, nonbinary characters, leading zeros, and a value larger than Long.MAX_VALUE.

**Common mistake:** Parsing through int first when a long result was promised.

**Follow-up:** Return the value modulo m without requiring the full value to fit.

### Pattern 8: Decimal to binary

**Recognition signal:** A nonnegative integer must be represented in base 2 without a library formatter.

**Brute force:** Find the largest power of two and test every position.

**Better and optimal approach:** Repeatedly append value % 2, divide by 2, and reverse.

**Java solution:** BaseConversionPatterns.toBinary.

**Complexity:** O(log2(value)) time and O(log2(value)) output space.

**Dry run:** 13 produces remainders 1, 0, 1, 1; reversing gives "1101".

**Edge cases:** Zero must return "0". This contract accepts nonnegative long values only.

**Common mistake:** Returning an empty string for zero.

**Follow-up:** Emit exactly 64 bits, including leading zeros.

### Pattern 9: Generic base to decimal

**Recognition signal:** The input provides digits and a radix, often from 2 through 36.

**Brute force:** Multiply each digit by Math.pow(base, position).

**Better approach:** Maintain an integer positional power from right to left.

**Optimal interview approach:** Horner accumulation needs one multiply-add per digit and no separate power.

**Java solution:** BaseConversionPatterns.baseToLong.

**Complexity:** O(d) time and O(1) space.

**Dry run:** "2A" in base 16 accumulates 2, then 2 * 16 + 10 = 42.

**Edge cases:** Invalid base, invalid digit, mixed letter case, leading zeros, empty input, and overflow.

**Common mistake:** Accepting Character.digit(ch, base) == -1 as a real digit.

**Follow-up:** Support an optional sign without allowing a sign-only string.

### Pattern 10: Decimal to generic base

**Recognition signal:** A nonnegative fixed-width value must be formatted in a requested radix.

**Brute force:** Repeatedly search for the largest power of the base.

**Better and optimal approach:** Collect repeated remainders and reverse them.

**Java solution:** BaseConversionPatterns.longToBase.

**Complexity:** O(log_base(value)) time and the same amount of output space.

**Dry run:** 42 in base 16 produces remainders 10 and 2, which map to A and 2; reverse to "2A".

**Edge cases:** Zero, invalid bases, and the chosen nonnegative-input contract.

**Common mistake:** Mapping remainder 10 to the two characters "10" instead of digit A.

**Follow-up:** Support negative values, including Long.MIN_VALUE, without calling Math.abs on it.

### Pattern 29: Hexadecimal string to decimal

**Recognition signal:** Digits may include A-F and represent powers of sixteen.

**Brute force:** Use a position-by-position power formula with Math.pow.

**Better and optimal approach:** Reuse generic Horner accumulation with base 16.

**Java solution:** BaseConversionPatterns.hexToLong.

**Complexity:** O(d) time and O(1) extra space.

**Dry run:** "7F" accumulates 7, then 7 * 16 + 15 = 127.

**Edge cases:** Letter case, optional prefix policy, invalid G-Z digits, and overflow. This method deliberately does not accept a 0x prefix.

**Common mistake:** Subtracting '0' from a letter digit.

**Follow-up:** Accept and validate an optional 0x or 0X prefix.

### Pattern 30: Decimal to hexadecimal

**Recognition signal:** A nonnegative value must be rendered compactly in base 16.

**Brute force:** Repeatedly compare against a hard-coded table of powers of sixteen.

**Better and optimal approach:** Reuse generic repeated division with base 16.

**Java solution:** BaseConversionPatterns.toHex.

**Complexity:** O(log16(value)) time and output space.

**Dry run:** 255 produces remainders 15 and 15, giving "FF".

**Edge cases:** Zero and the selected uppercase/lowercase convention.

**Common mistake:** Forgetting that the returned representation is text, not a decimal integer containing hexadecimal digits.

**Follow-up:** Produce Java-style fixed-width two's-complement output for a negative int.

### Extension: Validate a binary string

Validation is part of parsing, not an optional cleanup step. A valid nonnegative binary string is non-null, nonempty, and contains only '0' or '1'. Decide separately whether whitespace, signs, separators, or a 0b prefix are allowed. Silently deleting invalid characters changes the input and is rarely an acceptable interview contract.

## 14.7 Compiling base-conversion implementation

~~~java
public final class BaseConversionPatterns {
    private static final char[] DIGITS =
            "0123456789ABCDEFGHIJKLMNOPQRSTUVWXYZ".toCharArray();

    private BaseConversionPatterns() {
    }

    public static boolean isValidBinary(String text) {
        if (text == null || text.isEmpty()) {
            return false;
        }
        for (int i = 0; i < text.length(); i++) {
            char current = text.charAt(i);
            if (current != '0' && current != '1') {
                return false;
            }
        }
        return true;
    }

    public static long binaryToLong(String binary) {
        if (!isValidBinary(binary)) {
            throw new IllegalArgumentException("invalid binary string");
        }
        return baseToLong(binary, 2);
    }

    public static String toBinary(long value) {
        return longToBase(value, 2);
    }

    public static long hexToLong(String hexadecimal) {
        return baseToLong(hexadecimal, 16);
    }

    public static String toHex(long value) {
        return longToBase(value, 16);
    }

    public static long baseToLong(String text, int base) {
        requireBase(base);
        if (text == null || text.isEmpty()) {
            throw new IllegalArgumentException("digits must not be empty");
        }

        long result = 0;
        for (int i = 0; i < text.length(); i++) {
            int digit = asciiDigit(text.charAt(i));
            if (digit < 0 || digit >= base) {
                throw new IllegalArgumentException(
                        "invalid digit at index " + i);
            }
            if (result > (Long.MAX_VALUE - digit) / base) {
                throw new ArithmeticException("value exceeds long range");
            }
            result = result * base + digit;
        }
        return result;
    }

    public static String longToBase(long value, int base) {
        requireBase(base);
        if (value < 0) {
            throw new IllegalArgumentException(
                    "value must be nonnegative");
        }
        if (value == 0) {
            return "0";
        }

        StringBuilder reversed = new StringBuilder();
        long remaining = value;
        while (remaining > 0) {
            int digit = (int) (remaining % base);
            reversed.append(DIGITS[digit]);
            remaining /= base;
        }
        return reversed.reverse().toString();
    }

    private static void requireBase(int base) {
        if (base < Character.MIN_RADIX
                || base > Character.MAX_RADIX) {
            throw new IllegalArgumentException(
                    "base must be between 2 and 36");
        }
    }

    private static int asciiDigit(char value) {
        if (value >= '0' && value <= '9') {
            return value - '0';
        }
        if (value >= 'A' && value <= 'Z') {
            return value - 'A' + 10;
        }
        if (value >= 'a' && value <= 'z') {
            return value - 'a' + 10;
        }
        return -1;
    }

    public static void main(String[] args) {
        System.out.println(binaryToLong("001011")); // 11
        System.out.println(toBinary(13));           // 1101
        System.out.println(baseToLong("2A", 16));   // 42
        System.out.println(longToBase(42, 16));     // 2A
        System.out.println(hexToLong("7f"));        // 127
        System.out.println(toHex(255));             // FF
    }
}
~~~

The parser checks result > (Long.MAX_VALUE - digit) / base before multiplying. This guard proves that the next multiply-add fits. Checking result after overflow would be too late.

## 14.8 Numeric-property patterns

Many numeric properties become tractable when a search stops at sqrt(n). If n has a factor greater than its square root, the paired factor is smaller than the square root and has already been discovered.

### Pattern 13: Prime check

**Recognition signal:** Determine whether a value has exactly two positive factors.

**Brute force:** Test every divisor from 2 through n - 1.

**Better approach:** Test only through sqrt(n).

**Optimal interview approach:** Reject values below two, handle two and even values, then test odd divisors while divisor <= n / divisor. Division avoids divisor * divisor overflow.

**Java solution:** NumericPropertyPatterns.isPrime.

**Complexity:** O(sqrt(n)) time and O(1) space.

**Dry run:** For 29, test 3 and 5; neither divides, and the next odd divisor 7 exceeds 29 / 7.

**Edge cases:** Negative values, 0, 1, 2, and large values near Long.MAX_VALUE.

**Common mistake:** Treating 1 as prime or writing divisor * divisor <= n in long.

**Follow-up:** Generate all primes through n with the Sieve of Eratosthenes.

### Pattern 14: Print factors

**Recognition signal:** Enumerate all positive divisors, usually in sorted order.

**Brute force:** Test every candidate from 1 through n.

**Better approach:** Discover factor pairs through sqrt(n).

**Optimal interview approach:** Store small factors in forward order and paired large factors in reverse order, avoiding a duplicate when divisor == n / divisor.

**Java solution:** NumericPropertyPatterns.factors.

**Complexity:** O(sqrt(n)) search time, O(f) output space for f factors, and O(f) reversal time.

**Dry run:** For 36, pairs are (1,36), (2,18), (3,12), (4,9), and (6,6). Emit 6 only once.

**Edge cases:** This implementation requires n > 0. The factors of 1 are [1].

**Common mistake:** Printing both square-root partners and duplicating the root.

**Follow-up:** Return factors in sorted order without sorting the entire result.

### Pattern 15: Count factors

**Recognition signal:** Only the number of divisors matters, not the list.

**Brute force:** Test 1 through n.

**Better and optimal approach:** Count two for each factor pair and one for a square-root pair.

**Java solution:** NumericPropertyPatterns.factorCount.

**Complexity:** O(sqrt(n)) time and O(1) space.

**Dry run:** 36 has four distinct pairs plus the pair (6,6), so the total is 2 + 2 + 2 + 2 + 1 = 9.

**Edge cases:** n must be positive; factorCount(1) is 1.

**Common mistake:** Adding two for a perfect-square root.

**Follow-up:** Use prime exponents: if n = p^a q^b, the count is (a + 1)(b + 1).

### Pattern 16: Prime factorization

**Recognition signal:** Decompose a value into prime powers, often to derive divisor counts, GCDs, or multiplicative properties.

**Brute force:** Generate primes separately and repeatedly search the whole range.

**Better approach:** Trial-divide from two upward and remove each discovered factor completely.

**Optimal interview approach for a single long:** Remove factor two, test odd factors only while factor <= remaining / factor, then record any remaining value greater than one as prime.

**Java solution:** NumericPropertyPatterns.primeFactors.

**Complexity:** O(sqrt(n)) worst-case time and O(k) output space for k distinct primes.

**Dry run:** 84 removes 2 twice, then 3 once, leaving 7. The factorization is 2^2 * 3 * 7.

**Edge cases:** This method requires n > 0 and returns an empty map for 1.

**Common mistake:** Incrementing the divisor immediately after one division and missing repeated powers.

**Follow-up:** Factor many values by precomputing the smallest prime factor for every integer through a limit.

### Pattern 17: Greatest common divisor

**Recognition signal:** Simplifying ratios, synchronizing cycles, reducing fractions, or finding a largest shared unit.

**Brute force:** Scan downward from min(a, b) until a common divisor appears.

**Better and optimal approach:** Euclid's algorithm repeatedly replaces (a, b) with (b, a % b) until b is zero.

**Java solution:** NumericPropertyPatterns.gcd.

**Complexity:** O(log min(a, b)) time and O(1) space.

**Dry run:** gcd(48, 18) moves through (18,12), (12,6), and (6,0), so the answer is 6.

**Edge cases:** gcd(0, b) is b; gcd(0, 0) is defined here as 0. Inputs must be nonnegative.

**Common mistake:** Applying Math.abs to Long.MIN_VALUE and assuming it became positive.

**Follow-up:** Return coefficients x and y satisfying ax + by = gcd(a, b).

### Pattern 18: Least common multiple

**Recognition signal:** Find the first time cycles align or the smallest positive multiple shared by two values.

**Brute force:** Step through multiples of the larger input until one is shared.

**Better approach:** Use a * b / gcd(a, b), but multiplication may overflow before division.

**Optimal interview approach:** Divide first: (a / gcd(a, b)) * b, and verify the final multiplication.

**Java solution:** NumericPropertyPatterns.safeLcm.

**Complexity:** O(log min(a, b)) time for GCD and O(1) space.

**Dry run:** lcm(21, 6) uses gcd 3, reduced value 7, then 7 * 6 = 42.

**Edge cases:** If either input is zero, the result is zero. Inputs are nonnegative. An unrepresentable long result returns OptionalLong.empty.

**Common mistake:** Multiplying before dividing or ignoring the zero case.

**Follow-up:** Compute the LCM of an array while stopping on overflow.

### Pattern 19: Perfect square

**Recognition signal:** Determine whether n equals k^2 for an integer k.

**Brute force:** Test every k from zero through n.

**Better approach:** Estimate with Math.sqrt and verify.

**Optimal proof-friendly approach:** Binary-search the integer root and compare with division rather than mid * mid.

**Java solution:** NumericPropertyPatterns.isPerfectSquare.

**Complexity:** O(log n) time and O(1) space.

**Dry run:** floorSquareRoot(80) returns 8; 80 / 8 is 10, so 80 is not 8 squared.

**Edge cases:** Negative values are false; zero and one are true.

**Common mistake:** Comparing Math.sqrt(n) % 1 == 0 and assuming floating-point output is exact for every long.

**Follow-up:** Determine whether n is a perfect cube without multiplication overflow.

### Pattern 20: Integer square root

**Recognition signal:** Return floor(sqrt(n)) without a floating-point answer.

**Brute force:** Increment a candidate until its square is too large.

**Better and optimal approach:** Binary-search the monotonic predicate candidate <= n / candidate.

**Java solution:** NumericPropertyPatterns.floorSquareRoot.

**Complexity:** O(log n) time and O(1) space.

**Dry run:** For 27, candidates 7, 3, 5, and 6 leave answer 5.

**Edge cases:** Negative input is rejected. Zero and one return themselves.

**Common mistake:** Allowing a midpoint or square multiplication to overflow.

**Follow-up:** Search for the kth root or return an answer within a decimal tolerance.

### Pattern 21: Power of two

**Recognition signal:** A positive value should contain exactly one set bit.

**Brute force:** Generate powers of two until reaching or passing n.

**Better approach:** Repeatedly divide by two and reject an odd intermediate.

**Optimal interview approach:** Require n > 0 and test (n & (n - 1)) == 0.

**Java solution:** NumericPropertyPatterns.isPowerOfTwo.

**Complexity:** O(1) time and O(1) space for a fixed-width long.

**Dry run:** 16 is 10000 in binary and 15 is 01111; AND is zero.

**Edge cases:** Zero and negative values must be false.

**Common mistake:** Omitting the positive guard.

**Follow-up:** Return the next representable power of two without overflow.

### Pattern 22: Fast exponentiation

**Recognition signal:** Compute base^exponent for a large nonnegative exponent.

**Brute force:** Multiply the result by base exponent times.

**Better and optimal approach:** Exponentiation by squaring uses the exponent's binary representation. Square the base each round and multiply it into the result only when the current exponent bit is one.

**Java solution:** NumericPropertyPatterns.safePower.

**Complexity:** O(log exponent) multiplications and O(1) space.

**Dry run:** 3^5 uses exponent bits 101: result becomes 3, base squares to 9, then base squares to 81 and result becomes 243.

**Edge cases:** Exponent zero returns one, including 0^0 under this programming contract. Negative exponents are rejected. Overflow returns OptionalLong.empty.

**Common mistake:** Squaring one extra time after the exponent becomes zero and reporting irrelevant overflow.

**Follow-up:** Compute base^exponent modulo m while avoiding multiplication overflow.

### Pattern 28: Normalize negative modulo

**Recognition signal:** An index, clock position, or modular state must be in [0, modulus) even when the input is negative.

**Brute force:** Repeatedly add modulus until the value becomes nonnegative.

**Better approach:** Use ((value % modulus) + modulus) % modulus, but the addition deserves range reasoning.

**Optimal Java approach:** Use Math.floorMod(value, modulus) with a positive modulus.

**Java solution:** NumericPropertyPatterns.normalizeModulo.

**Complexity:** O(1) time and O(1) space.

**Dry run:** -13 with modulus 5 normalizes to 2.

**Edge cases:** The modulus must be positive.

**Common mistake:** Assuming Java's % always returns a nonnegative value.

**Follow-up:** Normalize a circular array index after an arbitrary signed jump.

### Extension: Perfect number

A positive integer is perfect when the sum of its positive proper divisors equals the value. Six is perfect because 1 + 2 + 3 = 6.

**Recognition signal:** Sum factor pairs while excluding n itself.

**Optimal approach:** Start with sum 1 for n > 1, inspect divisors through sqrt(n), and add both members of each pair without duplicating a square root.

**Complexity:** O(sqrt(n)) time and O(1) space.

**Common mistake:** Including n as its own proper divisor.

## 14.9 Compiling numeric-property implementation

~~~java
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.OptionalLong;

public final class NumericPropertyPatterns {
    private NumericPropertyPatterns() {
    }

    public static boolean isPrime(long value) {
        if (value < 2) {
            return false;
        }
        if (value == 2) {
            return true;
        }
        if (value % 2 == 0) {
            return false;
        }
        for (long divisor = 3;
                divisor <= value / divisor;
                divisor += 2) {
            if (value % divisor == 0) {
                return false;
            }
        }
        return true;
    }

    public static List<Long> factors(long value) {
        requirePositive(value);
        List<Long> lower = new ArrayList<>();
        List<Long> upper = new ArrayList<>();
        for (long divisor = 1;
                divisor <= value / divisor;
                divisor++) {
            if (value % divisor == 0) {
                lower.add(divisor);
                long paired = value / divisor;
                if (paired != divisor) {
                    upper.add(paired);
                }
            }
        }
        Collections.reverse(upper);
        lower.addAll(upper);
        return List.copyOf(lower);
    }

    public static int factorCount(long value) {
        requirePositive(value);
        int count = 0;
        for (long divisor = 1;
                divisor <= value / divisor;
                divisor++) {
            if (value % divisor == 0) {
                count += divisor == value / divisor ? 1 : 2;
            }
        }
        return count;
    }

    public static Map<Long, Integer> primeFactors(long value) {
        requirePositive(value);
        Map<Long, Integer> result = new LinkedHashMap<>();
        long remaining = value;

        while (remaining % 2 == 0 && remaining > 1) {
            result.merge(2L, 1, Integer::sum);
            remaining /= 2;
        }
        for (long factor = 3;
                factor <= remaining / factor;
                factor += 2) {
            while (remaining % factor == 0) {
                result.merge(factor, 1, Integer::sum);
                remaining /= factor;
            }
        }
        if (remaining > 1) {
            result.merge(remaining, 1, Integer::sum);
        }
        return Map.copyOf(result);
    }

    public static long gcd(long first, long second) {
        requireNonnegative(first, "first");
        requireNonnegative(second, "second");
        long a = first;
        long b = second;
        while (b != 0) {
            long remainder = a % b;
            a = b;
            b = remainder;
        }
        return a;
    }

    public static OptionalLong safeLcm(long first, long second) {
        requireNonnegative(first, "first");
        requireNonnegative(second, "second");
        if (first == 0 || second == 0) {
            return OptionalLong.of(0);
        }
        long reduced = first / gcd(first, second);
        if (reduced > Long.MAX_VALUE / second) {
            return OptionalLong.empty();
        }
        return OptionalLong.of(reduced * second);
    }

    public static long floorSquareRoot(long value) {
        requireNonnegative(value, "value");
        if (value < 2) {
            return value;
        }
        long low = 1;
        long high = Math.min(value / 2 + 1, 3_037_000_499L);
        long answer = 1;
        while (low <= high) {
            long mid = low + (high - low) / 2;
            if (mid <= value / mid) {
                answer = mid;
                low = mid + 1;
            } else {
                high = mid - 1;
            }
        }
        return answer;
    }

    public static boolean isPerfectSquare(long value) {
        if (value < 0) {
            return false;
        }
        long root = floorSquareRoot(value);
        return root == 0
                ? value == 0
                : value / root == root && value % root == 0;
    }

    public static boolean isPowerOfTwo(long value) {
        return value > 0 && (value & (value - 1)) == 0;
    }

    public static OptionalLong safePower(long base, int exponent) {
        if (exponent < 0) {
            throw new IllegalArgumentException(
                    "exponent must be nonnegative");
        }
        long result = 1;
        long factor = base;
        int remaining = exponent;

        while (remaining > 0) {
            if ((remaining & 1) != 0) {
                OptionalLong product = safeMultiply(result, factor);
                if (product.isEmpty()) {
                    return OptionalLong.empty();
                }
                result = product.getAsLong();
            }
            remaining >>>= 1;
            if (remaining > 0) {
                OptionalLong square = safeMultiply(factor, factor);
                if (square.isEmpty()) {
                    return OptionalLong.empty();
                }
                factor = square.getAsLong();
            }
        }
        return OptionalLong.of(result);
    }

    public static long normalizeModulo(long value, long modulus) {
        if (modulus <= 0) {
            throw new IllegalArgumentException(
                    "modulus must be positive");
        }
        return Math.floorMod(value, modulus);
    }

    public static boolean isPerfectNumber(long value) {
        if (value <= 1) {
            return false;
        }
        long sum = 1;
        for (long divisor = 2;
                divisor <= value / divisor;
                divisor++) {
            if (value % divisor == 0) {
                long paired = value / divisor;
                if (sum > value - divisor) {
                    return false;
                }
                sum += divisor;
                if (paired != divisor) {
                    if (sum > value - paired) {
                        return false;
                    }
                    sum += paired;
                }
            }
        }
        return sum == value;
    }

    private static OptionalLong safeMultiply(long first, long second) {
        try {
            return OptionalLong.of(Math.multiplyExact(first, second));
        } catch (ArithmeticException overflow) {
            return OptionalLong.empty();
        }
    }

    private static void requirePositive(long value) {
        if (value <= 0) {
            throw new IllegalArgumentException(
                    "value must be positive");
        }
    }

    private static void requireNonnegative(
            long value, String parameterName) {
        if (value < 0) {
            throw new IllegalArgumentException(
                    parameterName + " must be nonnegative");
        }
    }

    public static void main(String[] args) {
        System.out.println(isPrime(29));             // true
        System.out.println(factors(36));             // nine factors
        System.out.println(primeFactors(84));        // 2^2, 3, 7
        System.out.println(gcd(48, 18));             // 6
        System.out.println(safeLcm(21, 6));          // 42
        System.out.println(floorSquareRoot(27));     // 5
        System.out.println(isPerfectSquare(81));     // true
        System.out.println(isPowerOfTwo(1_024));     // true
        System.out.println(safePower(3, 5));         // 243
        System.out.println(normalizeModulo(-13, 5)); // 2
        System.out.println(isPerfectNumber(28));     // true
    }
}
~~~

The numeric-type choices are deliberate. Factor search uses long and a division boundary. LCM divides before multiplication. Fast exponentiation reports overflow instead of wrapping. The factor-count result fits comfortably in int for any positive long input, while factors themselves remain long.

## 14.10 Large-number-string invariant

When a decimal value may contain thousands or millions of digits, it cannot be parsed into long. The solution should carry only the state needed by the question:

- a remainder for divisibility or modulo;
- a carry for addition;
- a canonical length and lexicographic comparison for ordering.

This is streaming reasoning. The full mathematical integer never needs to exist in memory as a primitive.

### Pattern 11: Divisibility by 9 for an arbitrarily large number

**Recognition signal:** The input is a decimal string too large for primitive parsing, and only divisibility by nine matters.

**Brute force:** Parse with long and use value % 9, which fails outside the long range.

**Better approach:** Sum all digits, then test sum % 9.

**Optimal interview approach:** Maintain the sum modulo nine while scanning so the state remains small for arbitrarily long input.

**Java solution:** LargeNumberPatterns.isDivisibleBy9.

**Complexity:** O(d) time and O(1) extra space.

**Dry run:** "729" carries remainders 7, 0, 0 and is divisible by nine.

**Edge cases:** "0" and strings of zeros are divisible by nine. Invalid characters are rejected.

**Common mistake:** Parsing first, or allowing an int digit sum to overflow for an unbounded string.

**Follow-up:** Generalize the scan to compute a remainder modulo any positive int.

### Pattern 12: Divisibility by 11 for an arbitrarily large number

**Recognition signal:** A decimal string must be tested with the alternating-sum rule.

**Brute force:** Parse the entire value.

**Better approach:** Build separate sums for alternating positions and compare their difference modulo eleven.

**Optimal interview approach:** Carry the alternating signed remainder modulo eleven in one scan.

**Java solution:** LargeNumberPatterns.isDivisibleBy11.

**Complexity:** O(d) time and O(1) extra space.

**Dry run:** "121" has alternating sum 1 - 2 + 1 = 0, so it is divisible by eleven.

**Edge cases:** Leading zeros do not change the value. Starting with either alternating sign gives a result that differs only by sign, so divisibility is unchanged.

**Common mistake:** Applying the digit rule without validating that every character is decimal.

**Follow-up:** Return the actual remainder of the huge decimal string modulo eleven.

### Pattern 23: Modulo of a large numeric string

**Recognition signal:** A decimal string is too large to parse, but only value % modulus is needed.

**Brute force:** Use BigInteger or attempt primitive parsing.

**Better and optimal approach:** Apply Horner accumulation to the remainder:

~~~text
remainder = (remainder * 10 + digit) % modulus
~~~

**Java solution:** LargeNumberPatterns.modulo.

**Complexity:** O(d) time and O(1) space.

**Dry run:** "1234" modulo 7 carries 1, 5, 4, 2.

**Edge cases:** Modulus must be positive. This implementation uses an int modulus, so remainder * 10 + digit fits safely in long.

**Common mistake:** Storing the growing prefix rather than only its remainder.

**Follow-up:** Process characters from a Reader so the full string is not retained.

### Pattern 24: Add two numeric strings

**Recognition signal:** Two nonnegative decimal values may exceed every primitive type.

**Brute force:** Parse both values and add them.

**Better and optimal approach:** Walk from right to left, add two digits and a carry, append the result digit, and reverse once.

**Java solution:** LargeNumberPatterns.add.

**Complexity:** O(max(a, b)) time and O(max(a, b)) output space.

**Dry run:** "999" + "1" produces digits 0, 0, 0 with carries, then a final 1, yielding "1000".

**Edge cases:** Different lengths, leading zeros, zero plus zero, and a final carry.

**Common mistake:** Forgetting to include the remaining carry after both inputs are exhausted.

**Follow-up:** Add signed numeric strings with canonical output.

### Pattern 25: Compare two numeric strings

**Recognition signal:** Order two nonnegative decimal values without parsing them.

**Brute force:** Parse into long.

**Better approach:** Remove leading zeros, compare lengths, then compare characters.

**Optimal interview approach:** The better approach is already optimal because every relevant digit may need inspection.

**Java solution:** LargeNumberPatterns.compare.

**Complexity:** O(a + b) time to validate and canonicalize, and O(a + b) space in this clear string-producing implementation. An index-only variant uses O(1) extra space.

**Dry run:** "00098" and "101" canonicalize to "98" and "101"; shorter length means the first is smaller.

**Edge cases:** All-zero strings canonicalize to "0".

**Common mistake:** Lexicographically comparing raw strings before handling length and leading zeros.

**Follow-up:** Compare signed numeric strings while treating "-0" as zero.

## 14.11 Compiling large-number-string implementation

~~~java
public final class LargeNumberPatterns {
    private LargeNumberPatterns() {
    }

    public static boolean isDivisibleBy9(String digits) {
        requireDigits(digits);
        int remainder = 0;
        for (int i = 0; i < digits.length(); i++) {
            int digit = digits.charAt(i) - '0';
            remainder = (remainder + digit) % 9;
        }
        return remainder == 0;
    }

    public static boolean isDivisibleBy11(String digits) {
        requireDigits(digits);
        int alternating = 0;
        int sign = 1;
        for (int i = 0; i < digits.length(); i++) {
            int digit = digits.charAt(i) - '0';
            alternating = (alternating + sign * digit) % 11;
            sign = -sign;
        }
        return alternating == 0;
    }

    public static int modulo(String digits, int modulus) {
        requireDigits(digits);
        if (modulus <= 0) {
            throw new IllegalArgumentException(
                    "modulus must be positive");
        }
        long remainder = 0;
        for (int i = 0; i < digits.length(); i++) {
            int digit = digits.charAt(i) - '0';
            remainder = (remainder * 10 + digit) % modulus;
        }
        return (int) remainder;
    }

    public static String add(String first, String second) {
        requireDigits(first);
        requireDigits(second);
        int left = first.length() - 1;
        int right = second.length() - 1;
        int carry = 0;
        StringBuilder reversed = new StringBuilder(
                Math.max(first.length(), second.length()) + 1);

        while (left >= 0 || right >= 0 || carry != 0) {
            int firstDigit = left >= 0
                    ? first.charAt(left--) - '0'
                    : 0;
            int secondDigit = right >= 0
                    ? second.charAt(right--) - '0'
                    : 0;
            int sum = firstDigit + secondDigit + carry;
            reversed.append((char) ('0' + sum % 10));
            carry = sum / 10;
        }
        return canonical(reversed.reverse().toString());
    }

    public static int compare(String first, String second) {
        String left = canonical(first);
        String right = canonical(second);
        if (left.length() != right.length()) {
            return Integer.compare(left.length(), right.length());
        }
        return left.compareTo(right);
    }

    public static String canonical(String digits) {
        requireDigits(digits);
        int firstNonzero = 0;
        while (firstNonzero < digits.length() - 1
                && digits.charAt(firstNonzero) == '0') {
            firstNonzero++;
        }
        return digits.substring(firstNonzero);
    }

    private static void requireDigits(String digits) {
        if (digits == null || digits.isEmpty()) {
            throw new IllegalArgumentException(
                    "digits must not be empty");
        }
        for (int i = 0; i < digits.length(); i++) {
            char current = digits.charAt(i);
            if (current < '0' || current > '9') {
                throw new IllegalArgumentException(
                        "invalid decimal digit at index " + i);
            }
        }
    }

    public static void main(String[] args) {
        System.out.println(isDivisibleBy9("729"));       // true
        System.out.println(isDivisibleBy11("121"));     // true
        System.out.println(modulo("1234", 7));          // 2
        System.out.println(add("000999", "1"));         // 1000
        System.out.println(compare("00098", "101"));    // negative
    }
}
~~~

## 14.12 Overflow-aware patterns

Overflow safety is not a cleanup step. It is part of the algorithm's contract. A wrapped result cannot reliably reveal whether overflow occurred because the same bit pattern may also be a legitimate result from different operands.

### Pattern 26: Safe multiplication

**Recognition signal:** Two long operands must be multiplied only if the exact mathematical result fits.

**Brute force:** Multiply and inspect whether the result "looks wrong." This is not reliable.

**Better approach:** Write sign-sensitive division guards, carefully handling Long.MIN_VALUE and -1.

**Optimal Java approach:** Use Math.multiplyExact and convert ArithmeticException into the method's documented result type.

**Java solution:** OverflowPatterns.safeMultiply.

**Complexity:** O(1) time and O(1) space.

**Dry run:** Long.MAX_VALUE * 2 triggers overflow and returns OptionalLong.empty.

**Edge cases:** Zero, one, negative operands, Long.MIN_VALUE * -1, and two negative values.

**Common mistake:** Checking result / first == second after multiplication; overflow and zero cases make this fragile.

**Follow-up:** Implement a saturating multiplication policy instead of an optional result.

### Pattern 27: Overflow-safe binary-search midpoint

**Recognition signal:** A midpoint is computed between two valid nonnegative indices.

**Brute force:** (left + right) / 2, whose addition can overflow.

**Better and optimal approach for indices:** left + (right - left) / 2 after validating 0 <= left <= right. The difference then fits int.

**Java solution:** OverflowPatterns.safeMidpoint.

**Complexity:** O(1) time and O(1) space.

**Dry run:** left = 1,500,000,000 and right = 2,000,000,000 produce difference 500,000,000 and midpoint 1,750,000,000.

**Edge cases:** Equal bounds and invalid negative or reversed bounds.

**Common mistake:** Reusing this proof for arbitrary signed endpoints where right - left itself may overflow.

**Follow-up:** Compute the average of arbitrary signed long values without overflow and define rounding for negative odd sums.

### Related pattern: Safe LCM

Pattern 18 is also an overflow problem. Dividing by GCD before multiplying reduces the intermediate, but it does not prove that the remaining product fits. The final guard is still required.

### Related pattern: Safe integer reversal

Pattern 3 uses a long accumulator for an int input. If the input and output were both long, a wider primitive would not exist; the loop would need a pre-update bound check or Math.multiplyExact and Math.addExact.

### Extension: Comparator overflow

Sorting by subtraction is unsafe:

~~~java
// Wrong: subtraction can overflow and reverse the ordering.
values.sort((first, second) -> first - second);
~~~

Use the comparison API:

~~~java
values.sort(Integer::compare);
~~~

**Recognition signal:** A comparator orders fixed-width numbers.

**Complexity:** O(1) per comparison.

**Edge cases:** Integer.MIN_VALUE compared with a positive value.

**Follow-up:** Chain multiple keys with Comparator.comparingInt and thenComparing.

## 14.13 Compiling overflow implementation

~~~java
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.OptionalLong;

public final class OverflowPatterns {
    private OverflowPatterns() {
    }

    public static OptionalLong safeMultiply(
            long first, long second) {
        try {
            return OptionalLong.of(
                    Math.multiplyExact(first, second));
        } catch (ArithmeticException overflow) {
            return OptionalLong.empty();
        }
    }

    public static int safeMidpoint(int left, int right) {
        if (left < 0 || left > right) {
            throw new IllegalArgumentException(
                    "require 0 <= left <= right");
        }
        return left + (right - left) / 2;
    }

    public static List<Integer> sortedCopy(
            List<Integer> input) {
        List<Integer> result = new ArrayList<>(input);
        result.sort(Comparator.naturalOrder());
        return List.copyOf(result);
    }

    public static void main(String[] args) {
        System.out.println(safeMultiply(12, 13)); // 156
        System.out.println(
                safeMultiply(Long.MAX_VALUE, 2)); // empty
        System.out.println(
                safeMidpoint(1_500_000_000, 2_000_000_000));
        System.out.println(
                sortedCopy(List.of(
                        Integer.MAX_VALUE,
                        0,
                        Integer.MIN_VALUE)));
    }
}
~~~

The exception in safeMultiply does not escape; it is translated into the explicit OptionalLong contract. In production code, an API may instead propagate ArithmeticException, return a domain result object, use BigInteger, or reject the operation before it begins.

## 14.14 Pattern-recognition summary

| Problem wording | Carry this state | Usually avoid |
|---|---|---|
| "Each decimal digit" | quotient and remainder by 10 | String parsing unless text is the real input |
| "Value in base b" | multiply-add or quotient/remainder by b | Math.pow for exact conversion |
| "Factors or prime" | divisor through n / divisor | scanning through n |
| "Cycles align" | GCD, divide, checked multiply | a * b before division |
| "Huge decimal string modulo m" | current remainder only | primitive parsing |
| "Add huge numbers" | indices and carry | converting to double |
| "Compare huge numbers" | canonical length then text | raw lexicographic order |
| "Exact fixed-width operation" | precondition or exact API | checking after wraparound |
| "Circular nonnegative index" | floor modulus | assuming % is nonnegative |
| "Positive power of two" | one-set-bit invariant | accepting zero |

## 14.15 Interview communication template

For a numeric interview problem, a concise high-quality explanation sounds like this:

1. "I will define the input domain first: nonnegative decimal text, leading zeros allowed, invalid characters rejected."
2. "The entire value cannot fit in a primitive, but I only need its remainder."
3. "For each digit, the next prefix is oldPrefix * 10 + digit, so the next remainder is (oldRemainder * 10 + digit) % modulus."
4. "Because modulus is a positive int, the intermediate fits in long."
5. "The scan is O(d) time and O(1) extra space."
6. "I will test zero, leading zeros, invalid input, and the largest allowed modulus."

This explanation exposes the invariant, numeric safety, complexity, and tests before code.

## 14.16 Common mistakes across the catalog

- Leaving the input domain implicit.
- Treating zero as having no digits.
- Applying Math.abs before promoting Integer.MIN_VALUE.
- Parsing a large numeric string before applying a divisibility rule.
- Using Math.pow for an exact integer conversion.
- Checking overflow only after a fixed-width operation wraps.
- Multiplying before dividing in LCM.
- Writing divisor * divisor <= n near a type boundary.
- Forgetting to validate a digit against the selected base.
- Returning an empty representation for zero.
- Duplicating the square-root factor.
- Treating one as prime.
- Omitting the positive guard in a power-of-two test.
- Using raw lexicographic order for decimal strings of different lengths.
- Forgetting a final addition carry.
- Assuming Java's remainder is always nonnegative.
- Sorting integers by subtraction.
- Stating O(log n) without naming what shrinks geometrically.

## 14.17 Practice set

Solutions are intentionally separated into the delayed notes.

### Quick check

1. Why does countDigits need a special interpretation for zero?
2. What loop invariant makes Horner base parsing correct?
3. Why does a factor search stop at divisor <= n / divisor?
4. What state is sufficient to compute a huge decimal string modulo m?
5. Why is a / gcd(a, b) * b safer than a * b / gcd(a, b)?

### Coding practice

1. **Foundation:** Return the sum and product of the digits of an int in one traversal.
2. **Foundation:** Validate a base-8 string.
3. **Foundation:** Return all factor pairs of a positive long.
4. **Interview Core:** Parse a nonnegative base-b string to int with exact overflow detection.
5. **Interview Core:** Add one to a nonnegative numeric string in place in a char array when capacity permits.
6. **Interview Core:** Determine whether a huge decimal string is divisible by both 9 and 11 in one scan.
7. **Interview Core:** Return the next power of two in OptionalLong.
8. **SDE-2 Follow-up:** Compute a huge decimal string modulo several positive int moduli in one pass.

### Debugging tasks

1. Explain every failure in this prime loop:

~~~java
static boolean isPrime(int value) {
    for (int divisor = 2; divisor * divisor < value; divisor++) {
        if (value % divisor == 0) {
            return false;
        }
    }
    return true;
}
~~~

2. Explain why this comparator violates its contract for some inputs:

~~~java
values.sort((first, second) -> first - second);
~~~

3. Find the overflow path:

~~~java
static long lcm(long first, long second) {
    return first * second / gcd(first, second);
}
~~~

### Interview extensions

1. Modify baseToLong to accept an optional leading sign and still parse Long.MIN_VALUE exactly.
2. Add signed large-number strings, canonicalizing "-0" to "0".
3. Compute (a * b) % modulus when a and b are nonnegative long values and their product may overflow.
4. Design a streaming API that computes divisibility while reading a very large file of digits.

## 14.18 Delayed solution notes

### Quick-check answers

1. Decimal zero is represented by the single digit 0, so its digit count is one.
2. Before processing a character, result equals the numeric value of the processed prefix. Multiplying by the base shifts that prefix one position before the next digit is added.
3. Every factor above sqrt(n) is paired with a factor below sqrt(n), so no new pair begins beyond the boundary. Division expresses the boundary without square overflow.
4. The current prefix remainder is sufficient because congruent prefixes remain congruent after multiplying by ten and adding the same digit.
5. Dividing first reduces the factor before multiplication. A final bound check is still necessary.

### Coding guidance

1. Promote to the input's safe magnitude type once, then update both aggregates from the same digit. The product of digits for an int fits in long.
2. Map only ASCII `0` through `9` and letters explicitly, then require a value from zero through seven for every character. `Character.digit` is convenient only when a broader Unicode-digit contract is intentional.
3. Emit divisor and n / divisor for each exact division; avoid duplicating the square root.
4. Use the same bound result > (Integer.MAX_VALUE - digit) / base before each update.
5. Start from the rightmost character, propagate carry, and report whether a new leading character is required.
6. Carry two remainders: sum modulo nine and alternating sum modulo eleven.
7. Reject nonpositive input or define its contract, return the input if already a power of two, and check current > Long.MAX_VALUE / 2 before doubling.
8. Store one remainder per requested modulus. Time is O(d * m) for d digits and m moduli; space is O(m).

### Debugging resolutions

The prime method must reject values below two. Its boundary must include an exact square root, and divisor * divisor can overflow. A safe condition is divisor <= value / divisor.

Comparator subtraction can overflow, reverse the sign, and violate transitivity. Use Integer.compare(first, second) or Comparator.naturalOrder().

LCM multiplication may overflow before division, and gcd(0, 0) can create division by zero depending on the implementation. Handle zero, divide by GCD first, and check the remaining multiplication.

### Extension strategies

- A signed base parser can accumulate negatively, as Java's parsing implementations commonly do, because the negative range contains one extra magnitude. Validate the sign and compare against a negative limit before each update.
- Signed string addition first canonicalizes signs and magnitudes. Equal signs add magnitudes; different signs subtract the smaller magnitude from the larger.
- Overflow-safe modular multiplication can use repeated doubling with modular addition, BigInteger, or a carefully proven unsigned technique. State constraints before choosing.
- A streaming digit API carries remainder state across chunks and records an absolute character offset for validation errors.

## 14.19 Chapter summary

- Digit traversal removes one decimal digit per iteration.
- Horner accumulation is the central base-to-decimal invariant.
- Repeated division produces digits from least significant to most significant.
- Factor pairs reduce divisor search to O(sqrt(n)).
- Euclid's algorithm computes GCD in logarithmic time.
- Safe LCM divides before a checked multiply.
- Large-number strings often need only remainder, carry, or canonical length state.
- Exact arithmetic must detect overflow before trusting the result.
- Java exact-arithmetic and comparison APIs encode difficult boundary behavior clearly.

## 14.20 Revision checklist

- [ ] I can identify all 30 mandatory patterns in the coverage map.
- [ ] I can implement digit traversal for zero and Integer.MIN_VALUE.
- [ ] I can parse and format bases 2 through 36.
- [ ] I can explain the square-root factor boundary without multiplying.
- [ ] I can derive GCD and overflow-safe LCM.
- [ ] I can process a numeric string that does not fit in long.
- [ ] I can add and compare numeric strings with leading zeros.
- [ ] I can use Math.multiplyExact and a safe midpoint.
- [ ] I can normalize negative modulo with Math.floorMod.
- [ ] I can state complexity, numeric safety, and edge cases before coding.
