# Chapter 16: Interview Questions and Rapid Revision

This chapter is a retrieval and reasoning test, not a second pass through the explanations. Attempt the questions without running code first. Write down assumptions, predict behavior, and state complexity aloud. Then use a compiler and small tests to challenge your reasoning. The answer material begins only after every question, debugging task, coding exercise, and follow-up chain.

## 16.1 How to use this chapter

Use three passes:

1. **Closed-book pass:** Answer from memory. Mark uncertain items instead of guessing silently.
2. **Evidence pass:** Run snippets, write boundary tests, and compare the observed result with the language or algorithm contract.
3. **Interview pass:** Re-answer aloud in two minutes or less, including constraints, invariant, complexity, and one edge case.

Suggested scoring:

- 2 points: correct answer with a clear reason;
- 1 point: correct answer without a stable reason, or a nearly correct solution with one repair;
- 0 points: incorrect, unsafe, or unable to explain.

Do not use the answer key until all six assessment parts are complete.

## 16.2 Part I - 30 conceptual interview questions

### C01

How many decimal digits does zero have in a coding-problem contract, and why does a naive while (value > 0) loop often get this wrong?

### C02

Why must an int be promoted before Math.abs when every possible int input is allowed?

### C03

Why do signed two's-complement types have one more negative value than positive value?

### C04

Explain why assigning an arithmetic result to long does not guarantee that the arithmetic occurred in long.

### C05

What is the difference between the character '7', the int 7, and the string "7"?

### C06

State the loop invariant for left-to-right base parsing with result = result * base + digit.

### C07

What must be validated before accepting a digit in base b?

### C08

When should leading zeros be ignored, and when might they be semantically important?

### C09

Why can an arbitrarily large decimal string be tested for divisibility by nine without constructing the full integer?

### C10

Explain the alternating-sum divisibility rule for eleven and why the starting sign does not affect the yes-or-no result.

### C11

State Euclid's GCD invariant and its termination condition.

### C12

Why is (a / gcd(a, b)) * b safer than a * b / gcd(a, b), and why is it still not automatically safe?

### C13

Why can a prime test stop at sqrt(n), and how should the loop boundary be written to avoid overflow?

### C14

How do factor pairs prevent duplicate work, and what special case occurs for a perfect square?

### C15

Distinguish floor(log2(n)), ceil(log2(n)), and the bit length of a positive integer.

### C16

Derive binary-search complexity from the size of the remaining search space.

### C17

Why is mid <= n / mid preferable to mid * mid <= n in an integer square-root search?

### C18

When is Math.sqrt useful for an exact integer problem, and what verification is required?

### C19

Why must (value & (value - 1)) == 0 be combined with value > 0 for a power-of-two test?

### C20

Why do 1 << 40 and 1L << 40 mean different operations?

### C21

Contrast >> and >>> for a negative int.

### C22

What shift distance does Java use for int and long, and what surprising behavior follows?

### C23

Why can Java's % result be negative?

### C24

When should Math.floorMod be preferred over %?

### C25

Why is == unsafe as a general value comparison for Integer references even though some small boxed values share identity?

### C26

Name four contexts that can unbox a nullable wrapper and throw NullPointerException.

### C27

Why does casting the result of integer division differ from casting one operand before division?

### C28

Why is a single universal epsilon not a sound policy for every double comparison?

### C29

How do you compare two nonnegative decimal strings numerically without parsing them?

### C30

When should a large-number problem use BigInteger, and when is a streaming remainder or carry state better?

## 16.3 Part II - 20 code-output questions

Assume each fragment appears inside a valid main method with any required java.lang types available. Predict the exact output or exception before running it.

### O01

~~~java
int value = Integer.MAX_VALUE;
System.out.println(value + 1);
~~~

### O02

~~~java
long value = Integer.MAX_VALUE + 1;
System.out.println(value);
~~~

### O03

~~~java
long value = (long) Integer.MAX_VALUE + 1;
System.out.println(value);
~~~

### O04

~~~java
System.out.println(7 / 2 * 2.0);
~~~

### O05

~~~java
System.out.println((double) (7 / 2));
~~~

### O06

~~~java
System.out.println(Math.abs(Integer.MIN_VALUE));
~~~

### O07

~~~java
System.out.println(-13 % 5);
System.out.println(Math.floorMod(-13, 5));
~~~

### O08

~~~java
System.out.println(1 << 32);
System.out.println(1L << 32);
~~~

### O09

~~~java
System.out.println(-1 >>> 1);
~~~

### O10

~~~java
System.out.println(-3 >> 1);
System.out.println(-3 / 2);
~~~

### O11

~~~java
Integer first = 127;
Integer second = 127;
System.out.println(first == second);
~~~

### O12

~~~java
Integer first = 1_000;
Integer second = 1_000;
System.out.println(first.equals(second));
~~~

### O13

~~~java
Integer value = null;
System.out.println(value + 1);
~~~

### O14

~~~java
System.out.println(Integer.parseInt("+007"));
~~~

### O15

~~~java
System.out.println(Character.digit('F', 16));
~~~

### O16

~~~java
byte value = 127;
value += 1;
System.out.println(value);
~~~

### O17

~~~java
double value = 0.0 / 0.0;
System.out.println(value == value);
~~~

### O18

~~~java
System.out.println(Double.compare(-0.0, 0.0));
~~~

### O19

~~~java
int value = 0xFFFF_FFFF;
System.out.println(value);
~~~

### O20

~~~java
System.out.println("9".compareTo("10"));
~~~

## 16.4 Part III - 20 debugging questions

For each method or fragment, identify the violated contract, give a failing input, and describe a repair. Do not restrict your answer to syntax.

### D01: Zero-digit bug

~~~java
static int countDigits(int value) {
    int count = 0;
    while (value > 0) {
        count++;
        value /= 10;
    }
    return count;
}
~~~

### D02: Absolute-value bug

~~~java
static int sumDigits(int value) {
    int remaining = Math.abs(value);
    int sum = 0;
    while (remaining > 0) {
        sum += remaining % 10;
        remaining /= 10;
    }
    return sum;
}
~~~

### D03: Reverse-overflow bug

~~~java
static int reverse(int value) {
    int result = 0;
    while (value != 0) {
        result = result * 10 + value % 10;
        value /= 10;
    }
    return result;
}
~~~

### D04: Binary-parser bug

~~~java
static int binaryToInt(String text) {
    int result = 0;
    for (int i = 0; i < text.length(); i++) {
        result = result * 2 + text.charAt(i) - '0';
    }
    return result;
}
~~~

### D05: Generic-base bug

~~~java
static long parseBase(String text, int base) {
    long result = 0;
    for (int i = 0; i < text.length(); i++) {
        int digit = Character.digit(
                text.charAt(text.length() - 1 - i), base);
        result += digit * Math.pow(base, i);
    }
    return result;
}
~~~

### D06: Signed-GCD bug

~~~java
static long gcd(long first, long second) {
    long a = Math.abs(first);
    long b = Math.abs(second);
    while (b != 0) {
        long remainder = a % b;
        a = b;
        b = remainder;
    }
    return a;
}
~~~

### D07: LCM-overflow bug

~~~java
static long lcm(long first, long second) {
    return first * second / gcd(first, second);
}
~~~

### D08: Prime-boundary bug

~~~java
static boolean isPrime(int value) {
    for (int divisor = 2;
            divisor * divisor < value;
            divisor++) {
        if (value % divisor == 0) {
            return false;
        }
    }
    return true;
}
~~~

### D09: Duplicate-factor bug

~~~java
static List<Integer> factors(int value) {
    List<Integer> result = new ArrayList<>();
    for (int divisor = 1;
            divisor * divisor <= value;
            divisor++) {
        if (value % divisor == 0) {
            result.add(divisor);
            result.add(value / divisor);
        }
    }
    return result;
}
~~~

### D10: Square-overflow bug

~~~java
static boolean isPerfectSquare(int value) {
    int low = 0;
    int high = value;
    while (low <= high) {
        int mid = (low + high) / 2;
        int square = mid * mid;
        if (square == value) {
            return true;
        }
        if (square < value) {
            low = mid + 1;
        } else {
            high = mid - 1;
        }
    }
    return false;
}
~~~

### D11: Power-of-two bug

~~~java
static boolean isPowerOfTwo(int value) {
    return (value & (value - 1)) == 0;
}
~~~

### D12: Huge-number bug

~~~java
static long modulo(String digits, int modulus) {
    return Long.parseLong(digits) % modulus;
}
~~~

### D13: Final-carry bug

~~~java
static String add(String first, String second) {
    int i = first.length() - 1;
    int j = second.length() - 1;
    int carry = 0;
    StringBuilder reversed = new StringBuilder();
    while (i >= 0 || j >= 0) {
        int a = i >= 0 ? first.charAt(i--) - '0' : 0;
        int b = j >= 0 ? second.charAt(j--) - '0' : 0;
        int sum = a + b + carry;
        reversed.append(sum % 10);
        carry = sum / 10;
    }
    return reversed.reverse().toString();
}
~~~

### D14: Numeric-string comparison bug

~~~java
static int compareNumbers(String first, String second) {
    return first.compareTo(second);
}
~~~

### D15: Midpoint-overflow bug

~~~java
static int midpoint(int left, int right) {
    return (left + right) / 2;
}
~~~

### D16: Modulo-normalization bug

~~~java
static int normalize(int value, int modulus) {
    return (value % modulus + modulus) % modulus;
}
~~~

### D17: Comparator-overflow bug

~~~java
values.sort((first, second) -> first - second);
~~~

### D18: Ratio-truncation bug

~~~java
static double completionRatio(int completed, int total) {
    return completed / total;
}
~~~

### D19: Boxed-equality bug

~~~java
static boolean sameCount(Integer first, Integer second) {
    return first == second;
}
~~~

### D20: Null-unboxing bug

~~~java
static void increment(Map<String, Integer> counts, String key) {
    counts.put(key, counts.get(key) + 1);
}
~~~

## 16.5 Part IV - 20 short coding exercises

Write compilable Java methods. For every answer, state the numeric type, time complexity, space complexity, and at least three boundary tests.

### S01 - Foundation: Count decimal digits

Implement int countDigits(int value). Zero has one digit, and the sign is not a digit. Every int input is valid.

### S02 - Foundation: Digit statistics

Return the decimal digit count, sum, and product for any int in one traversal. Define the product for zero correctly.

### S03 - Interview Core: Safe reverse

Implement OptionalInt reverse(int value). Return empty when the reversed mathematical value does not fit in int.

### S04 - Interview Core: Numeric palindrome

Test whether a nonnegative int is a decimal palindrome without string conversion and without reversing the entire value.

### S05 - Foundation: Binary validation

Implement boolean isValidBinary(String text). Require a non-null, nonempty sequence containing only ASCII '0' and '1'.

### S06 - Interview Core: Binary to long

Parse a validated nonnegative binary string into long. Throw ArithmeticException when the value exceeds Long.MAX_VALUE.

### S07 - Interview Core: Decimal to base

Convert a nonnegative long to a string in base 2 through 36 using uppercase letters.

### S08 - Foundation: GCD

Compute the nonnegative GCD of two nonnegative long values with Euclid's algorithm.

### S09 - Interview Core: Safe LCM

Return OptionalLong for the LCM of two nonnegative long values. Zero with any value has LCM zero.

### S10 - Interview Core: Prime check

Test a long for primality without allowing a square-boundary multiplication to overflow.

### S11 - Interview Core: Factor count

Count the positive factors of a positive long without listing all candidates through n.

### S12 - Interview Core: Integer square root

Return floor(sqrt(value)) for any nonnegative long without using floating point.

### S13 - Foundation: Perfect-square test

Use the integer-square-root result to test whether a long is a perfect square.

### S14 - Foundation: Power of two

Test whether a signed long is a positive mathematical power of two.

### S15 - Interview Core: Exact fast power

Compute base^exponent for a long base and nonnegative int exponent. Return empty on long overflow.

### S16 - Interview Core: Huge decimal modulo

Compute a nonnegative decimal string modulo a positive int without parsing the full value.

### S17 - Interview Core: Huge divisibility by eleven

Test an arbitrarily long validated decimal string with the alternating-sum rule.

### S18 - Interview Core: Add numeric strings

Add two validated nonnegative decimal strings. Return canonical output without unnecessary leading zeros.

### S19 - Foundation: Compare numeric strings

Compare two validated nonnegative decimal strings numerically while allowing leading zeros.

### S20 - SDE-2 Follow-up: Circular index

Implement int circularIndex(int index, long delta, int length). Require 0 <= index < length and length > 0. The signed delta may be any long.

## 16.6 Part V - 10 medium coding problems

These problems require multiple invariants or a deliberate API policy.

### M01 - Signed generic parser

Parse a string in base 2 through 36 into long. Accept one optional leading sign, reject a sign-only string, validate every digit, and support Long.MIN_VALUE exactly. Do not use Long.parseLong or BigInteger.

### M02 - Signed numeric-string addition

Add two canonical or noncanonical signed decimal strings. Accept leading zeros, normalize "-0" to "0", validate syntax, and avoid BigInteger.

### M03 - Numeric-string multiplication

Multiply two nonnegative decimal strings using grade-school multiplication. Validate input, canonicalize output, and analyze the O(a * b) digit work.

### M04 - Integer kth root

Given nonnegative long value and int k >= 1, return floor(value^(1/k)). Use binary search and an overflow-safe predicate that determines whether candidate^k <= value.

### M05 - Next power-of-two capacity

Return the smallest positive power of two greater than or equal to a positive long. Return OptionalLong.empty if no positive long answer exists.

### M06 - Batch prime factorization

For many queries with values from 2 through a known limit, precompute the smallest prime factor and return each query's prime factorization efficiently. Analyze preprocessing and per-query work.

### M07 - Overflow-safe modular multiplication

Compute (a * b) % modulus for 0 <= a, b < modulus and positive long modulus, even when a * b overflows long. Do not use BigInteger.

### M08 - Normalized rational value

Design an immutable Rational type backed by long numerator and denominator. Normalize signs and GCD, reject denominator zero, define zero canonically, and state an overflow policy for addition and comparison.

### M09 - Streaming multi-modulus scan

Read decimal digits in chunks and compute remainders for a caller-provided list of positive int moduli. Report the absolute position of an invalid character without retaining the entire input.

### M10 - Binary search on a numeric answer

Workers complete tasks at known positive rates. Find the minimum whole time needed to finish at least target tasks. The feasibility sum must not overflow, and the search bound must be justified.

## 16.7 Part VI - Five interviewer follow-up chains

Answer each first question before revealing the next one. The chains model how an interviewer increases constraints.

### F01 - Reverse integer chain

1. Reverse a positive int.
2. Add zero and negative inputs.
3. Detect int overflow.
4. Avoid a wider primitive accumulator.
5. Return a domain result that distinguishes overflow from a legitimate zero.

### F02 - Base conversion chain

1. Convert a binary string to decimal.
2. Reject invalid characters.
3. Detect long overflow.
4. Generalize to bases 2 through 36.
5. Add an optional sign and support Long.MIN_VALUE.

### F03 - GCD and scheduling chain

1. Implement GCD for positive ints.
2. Add zero inputs.
3. Derive LCM.
4. Detect LCM overflow.
5. Find when several recurring schedules next align and stop if the answer exceeds a deadline.

### F04 - Huge-number chain

1. Compute a decimal string modulo an int.
2. Process the digits as streamed chunks.
3. Validate and report the exact invalid position.
4. Compute several moduli in one pass.
5. Parallelize chunk processing while preserving order-sensitive remainder composition.

### F05 - Root and monotonic-search chain

1. Test whether an int is a perfect square.
2. Remove multiplication overflow.
3. Return floor square root for long.
4. Generalize to kth root.
5. Explain the monotonic predicate and prove the binary-search boundary updates.

## 16.8 Stop point

Do not continue until you have attempted:

- all 30 conceptual questions;
- all 20 code-output predictions;
- all 20 debugging diagnoses;
- at least 15 short exercises;
- at least 5 medium problems;
- all 5 follow-up chains aloud.

The remaining sections contain answers and solution guidance.

## 16.9 Conceptual model answers

### C01

Zero has one decimal digit: 0. A loop that runs only while value > 0 executes zero times for zero, so initialize the count to one or use a do-while traversal.

### C02

Math.abs(Integer.MIN_VALUE) cannot produce a positive int because 2^31 is outside the positive int range. Convert to long first and then take the absolute value.

### C03

With w bits, two's complement represents values from -2^(w-1) through 2^(w-1) - 1. Zero occupies one nonnegative pattern, leaving one extra negative magnitude.

### C04

Binary numeric promotion uses operand types to select the operation type. If both operands are int, overflow occurs in int; assignment conversion to long happens only after that result exists.

### C05

'7' is a char code unit, 7 is an int numeric value, and "7" is a String containing one character. Converting among them requires validation and an explicit numeric or textual rule.

### C06

Before each iteration, result equals the numeric value represented by the processed prefix. Multiplying by base shifts that prefix one digit left, and adding the new digit extends the invariant.

### C07

Validate that the base is supported, the text is nonempty under the sign policy, and Character.digit(character, base) returns a nonnegative value. Also validate overflow before multiplying and adding.

### C08

Ignore leading zeros when comparing or canonicalizing numeric values. Preserve them when the data is an identifier, fixed-width field, account code, or textual representation whose formatting is part of the contract.

### C09

Congruence is preserved by decimal extension. If two prefixes have the same remainder modulo nine, multiplying both by ten and adding the same digit preserves equivalent remainders, so only the current remainder is needed.

### C10

A decimal value is divisible by eleven when the alternating signed digit sum is divisible by eleven. Reversing every sign negates the sum, and a value is divisible by eleven exactly when its negation is too.

### C11

gcd(a, b) equals gcd(b, a % b). Repeated replacement reduces the second nonnegative operand until it is zero; the remaining first operand is the GCD.

### C12

Dividing by the GCD first reduces an operand before multiplication, avoiding some unnecessary overflow. The reduced product can still exceed long, so compare against Long.MAX_VALUE / otherOperand or use Math.multiplyExact.

### C13

If n = a * b and a is greater than sqrt(n), then b is less than sqrt(n), so one factor would already have been found. Use divisor <= n / divisor rather than divisor * divisor <= n.

### C14

Each divisor d produces paired divisor n / d. At an exact square root, the two members are equal and must be counted or emitted once.

### C15

floor(log2(n)) is the highest set-bit index for positive n. ceil(log2(n)) is the number of doublings from one needed to reach at least n. Bit length is floor(log2(n)) + 1.

### C16

After k valid binary-search decisions, at most about n / 2^k candidates remain. Requiring that quantity to reach one gives k in O(log2(n)).

### C17

mid * mid can overflow even when mid and n are valid long values. For positive mid, mid <= n / mid expresses the same ordering without multiplication.

### C18

Math.sqrt is a useful constant-time estimate or initial candidate. For exact long behavior, adjust and verify the candidate with division and remainder, or use an integer binary search for a proof independent of floating-point rounding.

### C19

Zero passes the bit expression because 0 & -1 is zero. Negative values such as Long.MIN_VALUE can also contain one set bit, so require a positive mathematical value.

### C20

The left operand determines the promoted width. The first expression shifts a 32-bit int with distance masked to five bits; the second shifts a 64-bit long with distance masked to six bits.

### C21

Signed right shift >> fills new high bits with the sign bit. Unsigned right shift >>> fills them with zero, so a negative int becomes a nonnegative bit-pattern interpretation after enough shifting.

### C22

int uses the low five bits of the distance, equivalent to distance & 31. long uses the low six bits, equivalent to distance & 63. Therefore shifting int by 32 or long by 64 acts like shifting by zero.

### C23

Java defines % as remainder after division that truncates toward zero. The remainder has the dividend's sign or is zero, so a negative dividend can produce a negative remainder.

### C24

Use Math.floorMod when the domain needs a result in [0, modulus) for a positive modulus, such as circular indices, clock arithmetic, and normalized hash buckets.

### C25

When both operands are references, == compares identity. Small boxed constants have selected identity guarantees, but value correctness must not depend on allocation or cache behavior; use equals with a null policy.

### C26

Arithmetic, comparison with a primitive, assignment to a primitive, and method invocation requiring a primitive can unbox. switch and selected conditional-expression contexts can also unbox.

### C27

Casting the result occurs after integer division has already discarded the fraction. Casting one operand first makes binary numeric promotion select floating-point division.

### C28

An acceptable error depends on scale, units, algorithm, and accumulated operations. A fixed absolute epsilon may be too large near zero or too small for large magnitudes; often a documented combination of absolute and relative tolerance is needed.

### C29

Validate and remove unnecessary leading zeros. Compare canonical lengths first; if lengths match, compare digit characters lexicographically.

### C30

Use BigInteger when the exact entire value must be retained and arbitrary-precision operations are part of the solution. Use a streaming state when the question needs only a remainder, checksum, comparison summary, or carry, because it is simpler and uses bounded extra space.

## 16.10 Code-output answer key

### O01

Output: -2147483648. The int addition wraps from Integer.MAX_VALUE to Integer.MIN_VALUE.

### O02

Output: -2147483648. Both operands are int, so the overflow occurs before widening to long.

### O03

Output: 2147483648. Casting the first operand makes the addition occur in long.

### O04

Output: 6.0. The subexpression 7 / 2 evaluates to int 3, and 3 * 2.0 is double 6.0.

### O05

Output: 3.0. Integer division completes before the cast.

### O06

Output: -2147483648. The positive magnitude does not fit in int.

### O07

Output:

~~~text
-3
2
~~~

Remainder follows the negative dividend; floorMod normalizes for positive modulus.

### O08

Output:

~~~text
1
4294967296
~~~

The int shift distance 32 is masked to zero. The long expression represents 2^32.

### O09

Output: 2147483647. Unsigned right shift fills the high bit with zero.

### O10

Output:

~~~text
-2
-1
~~~

Signed right shift extends the sign and behaves like floor division by two here. Integer division truncates toward zero.

### O11

Output: true. Boxing the constant int 127 is within the required small-value identity range.

### O12

Output: true. Integer.equals compares same-wrapper numeric value; identity is irrelevant.

### O13

Result: NullPointerException before println can print a value. Addition requires unboxing null.

### O14

Output: 7. parseInt accepts one leading plus sign and decimal leading zeros.

### O15

Output: 15.

### O16

Output: -128. Compound assignment includes an implicit narrowing cast back to byte after int addition.

### O17

Output: false. NaN is unequal to every value, including itself.

### O18

Output: -1. Double.compare defines negative zero as ordered before positive zero.

### O19

Output: -1. The hexadecimal literal denotes the 32-bit int pattern with every bit set.

### O20

Output: 8, the character-code difference between '9' and '1'. Raw lexicographic order does not equal numeric order for different digit lengths.

## 16.11 Debugging answer guide

### D01

Failing inputs include 0 and every negative value. Zero returns zero digits, while negatives never enter the loop. Promote to long, take the magnitude, and use a do-while or initialize the count for zero.

### D02

Integer.MIN_VALUE remains negative after Math.abs, and zero never enters the loop. Use long remaining = Math.abs((long) value) and a do-while traversal.

### D03

An input such as 1,534,236,469 reverses beyond the int range and wraps during reconstruction. Accumulate an int input in long and range-check before narrowing, or guard each multiply-add before it occurs.

### D04

The method accepts characters other than '0' and '1', rejects neither null nor empty input deliberately, and can overflow int. Validate the string and check result > (Integer.MAX_VALUE - bit) / 2 before updating.

### D05

Character.digit can return -1, the base can be invalid, Math.pow returns double, casting loses exactness, and the sum can overflow. Scan left to right with checked Horner accumulation after validating base and every digit.

### D06

Math.abs(Long.MIN_VALUE) remains negative, so the claimed nonnegative normalization fails. Either require nonnegative inputs, use BigInteger for every signed long pair, or return a result type capable of representing the unsigned magnitude 2^63.

### D07

first * second can overflow before division, and gcd(0, 0) may lead to division by zero. Handle zero first, divide one input by GCD, then validate or exactly perform the remaining multiplication.

### D08

The method returns true for values below two. The strict boundary misses exact square divisors, such as 3 for 9, and divisor * divisor can overflow. Reject values below two and loop while divisor <= value / divisor.

### D09

A perfect square adds the square root twice. The method also lacks a positive-value contract, can overflow divisor * divisor, and does not produce sorted order. Add the paired factor only when paired != divisor and use divisor <= value / divisor.

### D10

Negative values create an invalid range. `low + high` and `mid * mid` can overflow, causing wrong direction changes. Reject negative input, return `true` immediately for zero and one, then search from `low = 1` with `low + (high - low) / 2` and compare `mid <= value / mid`. Handling zero before division prevents a `0 / 0` failure.

### D11

Zero passes because 0 & -1 is zero, and Integer.MIN_VALUE also has one set bit despite being negative. Require value > 0.

### D12

Long.parseLong fails for the exact inputs that motivate a large-number-string method. It also lacks digit and modulus validation. Scan characters and carry remainder = (remainder * 10 + digit) % modulus.

### D13

"999" + "1" ends with carry one after both indices are exhausted, but the loop stops and returns "000". Continue while i >= 0 || j >= 0 || carry != 0, and validate digits before arithmetic.

### D14

"9".compareTo("10") is positive even though nine is numerically smaller. Validate and remove leading zeros, compare lengths, then compare equal-length canonical strings.

### D15

Large positive indices can overflow left + right. Under the array-index contract 0 <= left <= right, return left + (right - left) / 2.

### D16

The method does not require a positive modulus, division by zero is possible, and value % modulus + modulus can overflow int for a very large modulus. Validate modulus > 0 and use Math.floorMod(value, modulus).

### D17

For Integer.MIN_VALUE and a positive value, subtraction can overflow and return a sign that reverses the order. Use Integer.compare(first, second) or Comparator.naturalOrder().

### D18

Both operands are int, so fractional information is discarded. Validate total according to the zero policy and cast an operand before division: (double) completed / total.

### D19

The method compares wrapper identity and can also produce false when equal values are distinct objects. Choose Objects.equals for nullable same-wrapper equality, or validate non-null values and compare their unboxed ints.

### D20

counts.get(key) returns null for an absent key, and addition unboxes it. Use counts.merge(key, 1, Integer::sum), getOrDefault, or an explicit initialization policy. Consider Math.addExact if count overflow matters.

## 16.12 Short-exercise reference solutions

The following class supplies one compiling reference implementation for S01 through S20. Method names correspond to the exercise order. Alternative solutions are valid when their contracts and boundary behavior are equally explicit.

~~~java
import java.util.OptionalInt;
import java.util.OptionalLong;

public final class RapidRevisionSolutions {
    private static final char[] BASE_DIGITS =
            "0123456789ABCDEFGHIJKLMNOPQRSTUVWXYZ".toCharArray();

    private RapidRevisionSolutions() {
    }

    public record DigitStats(
            int count, int sum, long product) {
    }

    public static int countDigits(int value) {
        long remaining = Math.abs((long) value);
        int count = 0;
        do {
            count++;
            remaining /= 10;
        } while (remaining > 0);
        return count;
    }

    public static DigitStats digitStats(int value) {
        long remaining = Math.abs((long) value);
        int count = 0;
        int sum = 0;
        long product = 1;
        do {
            int digit = (int) (remaining % 10);
            count++;
            sum += digit;
            product *= digit;
            remaining /= 10;
        } while (remaining > 0);
        return new DigitStats(count, sum, product);
    }

    public static OptionalInt reverse(int value) {
        long remaining = Math.abs((long) value);
        long reversed = 0;
        do {
            reversed = reversed * 10 + remaining % 10;
            remaining /= 10;
        } while (remaining > 0);
        long signed = value < 0 ? -reversed : reversed;
        if (signed < Integer.MIN_VALUE
                || signed > Integer.MAX_VALUE) {
            return OptionalInt.empty();
        }
        return OptionalInt.of((int) signed);
    }

    public static boolean isPalindrome(int value) {
        if (value < 0 || value != 0 && value % 10 == 0) {
            return false;
        }
        int prefix = value;
        int suffix = 0;
        while (prefix > suffix) {
            suffix = suffix * 10 + prefix % 10;
            prefix /= 10;
        }
        return prefix == suffix || prefix == suffix / 10;
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

    public static long binaryToLong(String text) {
        if (!isValidBinary(text)) {
            throw new IllegalArgumentException(
                    "invalid binary text");
        }
        long result = 0;
        for (int i = 0; i < text.length(); i++) {
            int bit = text.charAt(i) - '0';
            if (result > (Long.MAX_VALUE - bit) / 2) {
                throw new ArithmeticException(
                        "binary value exceeds long");
            }
            result = result * 2 + bit;
        }
        return result;
    }

    public static String toBase(long value, int base) {
        if (value < 0) {
            throw new IllegalArgumentException(
                    "value must be nonnegative");
        }
        if (base < 2 || base > 36) {
            throw new IllegalArgumentException(
                    "base must be between 2 and 36");
        }
        if (value == 0) {
            return "0";
        }
        StringBuilder reversed = new StringBuilder();
        long remaining = value;
        while (remaining > 0) {
            int digit = (int) (remaining % base);
            reversed.append(BASE_DIGITS[digit]);
            remaining /= base;
        }
        return reversed.reverse().toString();
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

    public static OptionalLong safeLcm(
            long first, long second) {
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

    public static int factorCount(long value) {
        if (value <= 0) {
            throw new IllegalArgumentException(
                    "value must be positive");
        }
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

    public static long floorSquareRoot(long value) {
        requireNonnegative(value, "value");
        if (value < 2) {
            return value;
        }
        long low = 1;
        long high = Math.min(
                value / 2 + 1, 3_037_000_499L);
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
                : value / root == root
                        && value % root == 0;
    }

    public static boolean isPowerOfTwo(long value) {
        return value > 0 && (value & (value - 1)) == 0;
    }

    public static OptionalLong safePower(
            long base, int exponent) {
        if (exponent < 0) {
            throw new IllegalArgumentException(
                    "exponent must be nonnegative");
        }
        long result = 1;
        long factor = base;
        int remaining = exponent;
        while (remaining > 0) {
            if ((remaining & 1) != 0) {
                OptionalLong product =
                        exactProduct(result, factor);
                if (product.isEmpty()) {
                    return OptionalLong.empty();
                }
                result = product.getAsLong();
            }
            remaining >>>= 1;
            if (remaining > 0) {
                OptionalLong square =
                        exactProduct(factor, factor);
                if (square.isEmpty()) {
                    return OptionalLong.empty();
                }
                factor = square.getAsLong();
            }
        }
        return OptionalLong.of(result);
    }

    public static int largeModulo(
            String digits, int modulus) {
        requireDecimalDigits(digits);
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

    public static boolean isDivisibleBy11(String digits) {
        requireDecimalDigits(digits);
        int alternating = 0;
        int sign = 1;
        for (int i = 0; i < digits.length(); i++) {
            int digit = digits.charAt(i) - '0';
            alternating = (alternating + sign * digit) % 11;
            sign = -sign;
        }
        return alternating == 0;
    }

    public static String add(
            String first, String second) {
        requireDecimalDigits(first);
        requireDecimalDigits(second);
        int i = first.length() - 1;
        int j = second.length() - 1;
        int carry = 0;
        StringBuilder reversed = new StringBuilder();
        while (i >= 0 || j >= 0 || carry != 0) {
            int a = i >= 0 ? first.charAt(i--) - '0' : 0;
            int b = j >= 0 ? second.charAt(j--) - '0' : 0;
            int sum = a + b + carry;
            reversed.append((char) ('0' + sum % 10));
            carry = sum / 10;
        }
        return canonical(reversed.reverse().toString());
    }

    public static int compare(
            String first, String second) {
        String a = canonical(first);
        String b = canonical(second);
        if (a.length() != b.length()) {
            return Integer.compare(a.length(), b.length());
        }
        return a.compareTo(b);
    }

    public static int circularIndex(
            int index, long delta, int length) {
        if (length <= 0 || index < 0 || index >= length) {
            throw new IllegalArgumentException(
                    "require length > 0 and valid index");
        }
        long normalizedDelta = Math.floorMod(
                delta, (long) length);
        return (int) ((index + normalizedDelta) % length);
    }

    private static OptionalLong exactProduct(
            long first, long second) {
        try {
            return OptionalLong.of(
                    Math.multiplyExact(first, second));
        } catch (ArithmeticException overflow) {
            return OptionalLong.empty();
        }
    }

    private static String canonical(String digits) {
        requireDecimalDigits(digits);
        int firstNonzero = 0;
        while (firstNonzero < digits.length() - 1
                && digits.charAt(firstNonzero) == '0') {
            firstNonzero++;
        }
        return digits.substring(firstNonzero);
    }

    private static void requireDecimalDigits(String digits) {
        if (digits == null || digits.isEmpty()) {
            throw new IllegalArgumentException(
                    "digits must not be empty");
        }
        for (int i = 0; i < digits.length(); i++) {
            char current = digits.charAt(i);
            if (current < '0' || current > '9') {
                throw new IllegalArgumentException(
                        "invalid digit at index " + i);
            }
        }
    }

    private static void requireNonnegative(
            long value, String name) {
        if (value < 0) {
            throw new IllegalArgumentException(
                    name + " must be nonnegative");
        }
    }

    public static void main(String[] args) {
        System.out.println(countDigits(Integer.MIN_VALUE));
        System.out.println(digitStats(1_203));
        System.out.println(reverse(-120));
        System.out.println(isPalindrome(12_321));
        System.out.println(binaryToLong("1011"));
        System.out.println(toBase(255, 16));
        System.out.println(gcd(48, 18));
        System.out.println(safeLcm(21, 6));
        System.out.println(isPrime(29));
        System.out.println(factorCount(36));
        System.out.println(floorSquareRoot(27));
        System.out.println(isPerfectSquare(81));
        System.out.println(isPowerOfTwo(1_024));
        System.out.println(safePower(3, 5));
        System.out.println(largeModulo("1234", 7));
        System.out.println(isDivisibleBy11("121"));
        System.out.println(add("999", "1"));
        System.out.println(compare("0009", "10"));
        System.out.println(circularIndex(1, Long.MIN_VALUE, 7));
    }
}
~~~

### Complexity and boundary table

| Exercises | Time | Extra space excluding output | Critical tests |
|---|---:|---:|---|
| S01-S04 | O(decimal digits) | O(1) | 0, negative, MIN_VALUE, overflow |
| S05 | O(d) | O(1) | null, empty, "0", invalid char |
| S06 | O(d) | O(1) | leading zeros, MAX_VALUE, one-bit overflow |
| S07 | O(log_base n) | O(output) | zero, base 2, base 36 |
| S08-S09 | O(log min(a,b)) | O(1) | both zero, one zero, LCM overflow |
| S10-S11 | O(sqrt(n)) | O(1) | below two, square, large prime |
| S12-S13 | O(log n) | O(1) | 0, 1, MAX_VALUE |
| S14 | O(1) | O(1) | negative, zero, highest positive power |
| S15 | O(log exponent) | O(1) | exponent 0, negative base, overflow |
| S16-S17 | O(d) | O(1) | huge text, zeros, invalid digit |
| S18-S19 | O(max(a,b)) | O(output) | leading zeros, carry, equal values |
| S20 | O(1) | O(1) | negative delta, MIN_VALUE, length 1 |

## 16.13 Medium-problem solution guide

### M01 solution: Signed generic parser

Accumulate the result as a negative long. The negative range can represent Long.MIN_VALUE directly, while the positive range cannot represent its magnitude. After processing an optional sign, set:

~~~text
limit = Long.MIN_VALUE       for a negative input
limit = -Long.MAX_VALUE      for a positive input
~~~

Before multiplying, require result >= limit / base. Before subtracting the next digit, require multipliedResult >= limit + digit. Return the negative result directly for a negative input and negate it for a positive input.

~~~java
public final class SignedRadixParser {
    private SignedRadixParser() {
    }

    public static long parse(String text, int base) {
        if (base < 2 || base > 36) {
            throw new IllegalArgumentException("invalid base");
        }
        if (text == null || text.isEmpty()) {
            throw new IllegalArgumentException("empty input");
        }

        int index = 0;
        boolean negative = false;
        char first = text.charAt(0);
        if (first == '+' || first == '-') {
            negative = first == '-';
            index++;
        }
        if (index == text.length()) {
            throw new IllegalArgumentException("sign without digits");
        }

        long limit = negative
                ? Long.MIN_VALUE
                : -Long.MAX_VALUE;
        long multiplicationLimit = limit / base;
        long result = 0;

        while (index < text.length()) {
            int digit = Character.digit(
                    text.charAt(index), base);
            if (digit < 0) {
                throw new IllegalArgumentException(
                        "invalid digit at index " + index);
            }
            if (result < multiplicationLimit) {
                throw new ArithmeticException("long overflow");
            }
            result *= base;
            if (result < limit + digit) {
                throw new ArithmeticException("long overflow");
            }
            result -= digit;
            index++;
        }
        return negative ? result : -result;
    }

    public static void main(String[] args) {
        System.out.println(parse(
                "-8000000000000000", 16));
        System.out.println(parse(
                "7FFFFFFFFFFFFFFF", 16));
    }
}
~~~

**Complexity:** O(d) time and O(1) space.

**Critical tests:** "+0", "-0", sign only, invalid digit for base, Long.MAX_VALUE, Long.MIN_VALUE, and one step beyond each bound.

### M02 solution: Signed numeric-string addition

Parse each input into sign and canonical magnitude. If signs match, add magnitudes and retain the sign. If signs differ, compare magnitudes, subtract the smaller from the larger, and use the larger magnitude's sign. Canonicalize every zero result to "0".

Magnitude addition is the carry scan from Chapter 14. Magnitude subtraction scans right to left with a borrow and requires firstMagnitude >= secondMagnitude.

**Complexity:** O(max(a, b)) time and output space.

**Critical invariant:** Before each subtraction position, borrow is zero or one, and all less-significant output digits are final.

### M03 solution: Numeric-string multiplication

Validate and canonicalize both inputs. If either is zero, return "0". Allocate an int array of a.length + b.length. For each pair of digits from right to left, add their product to the lower result position, store sum % 10 there, and carry sum / 10 into the next position to the left.

~~~java
static String multiply(String first, String second) {
    String a = canonicalDigits(first);
    String b = canonicalDigits(second);
    if (a.equals("0") || b.equals("0")) {
        return "0";
    }

    int[] result = new int[a.length() + b.length()];
    for (int i = a.length() - 1; i >= 0; i--) {
        int leftDigit = a.charAt(i) - '0';
        for (int j = b.length() - 1; j >= 0; j--) {
            int rightDigit = b.charAt(j) - '0';
            int low = i + j + 1;
            int high = i + j;
            int sum = leftDigit * rightDigit + result[low];
            result[low] = sum % 10;
            result[high] += sum / 10;
        }
    }

    StringBuilder output = new StringBuilder(result.length);
    int index = result[0] == 0 ? 1 : 0;
    while (index < result.length) {
        output.append((char) ('0' + result[index++]));
    }
    return output.toString();
}
~~~

The helper canonicalDigits must validate a nonempty ASCII decimal string and remove unnecessary leading zeros.

**Complexity:** O(a * b) time and O(a + b) space.

### M04 solution: Integer kth root

Binary-search candidate from zero through min(value, a safe upper bound). The monotonic predicate is candidate^k <= value. Evaluate it with an accumulator and stop before multiplication when accumulator > value / candidate.

For candidate zero, the predicate is true for nonnegative value. For k = 1, the answer is value.

**Complexity:** O(k log value) with repeated multiplication in each predicate. Exponentiation by squaring can reduce each predicate to O(log k), but needs equally careful overflow capping.

### M05 solution: Next power-of-two capacity

Start at one and double while capacity < value. Before doubling, check capacity > Long.MAX_VALUE / 2. The largest positive power of two representable in long is 2^62.

**Complexity:** O(log value) time and O(1) space.

**Critical tests:** 1, an existing power, one above a power, 2^62, and 2^62 + 1.

### M06 solution: Batch prime factorization

Build an int array smallestPrimeFactor from zero through limit. When an unmarked i is found, set its own factor to i and mark still-unmarked multiples beginning at i * i. Use i <= limit / i to avoid square overflow.

To factor query q, repeatedly emit smallestPrimeFactor[q] and divide q by it until q becomes one.

**Complexity:** About O(limit log log limit) preprocessing, O(limit) memory, and O(number of prime factors with repetition) per query.

### M07 solution: Overflow-safe modular multiplication

Use binary decomposition of b. Maintain result and factor in [0, modulus). Modular addition avoids x + y overflow by comparing x with modulus - y.

~~~java
public final class ModularMultiplication {
    private ModularMultiplication() {
    }

    public static long multiplyMod(
            long first, long second, long modulus) {
        if (modulus <= 0
                || first < 0 || first >= modulus
                || second < 0 || second >= modulus) {
            throw new IllegalArgumentException(
                    "require 0 <= operands < positive modulus");
        }
        long result = 0;
        long factor = first;
        long multiplier = second;
        while (multiplier > 0) {
            if ((multiplier & 1) != 0) {
                result = addMod(result, factor, modulus);
            }
            multiplier >>>= 1;
            if (multiplier > 0) {
                factor = addMod(factor, factor, modulus);
            }
        }
        return result;
    }

    private static long addMod(
            long first, long second, long modulus) {
        return first >= modulus - second
                ? first - (modulus - second)
                : first + second;
    }

    public static void main(String[] args) {
        long modulus = Long.MAX_VALUE - 58;
        System.out.println(multiplyMod(
                modulus - 2, modulus - 3, modulus));
    }
}
~~~

**Complexity:** O(log second) time and O(1) space.

### M08 solution: Normalized rational value

Keep final fields long numerator and long denominator, with denominator strictly positive and gcd equal to one. Canonical zero is 0/1.

A robust overflow policy is:

- perform constructor sign normalization and GCD in BigInteger;
- convert normalized fields with longValueExact, rejecting a rational whose canonical long fields do not fit;
- perform addition and comparison cross-products in BigInteger;
- normalize the addition result back through the constructor.

This preserves long-backed storage while making overflow a checked API outcome rather than silent wraparound.

**Complexity:** Depends on operand bit length; for fixed-width inputs, treat the number of machine words as bounded but still state that BigInteger allocates.

### M09 solution: Streaming multi-modulus scan

Copy and validate the moduli once. Keep one long remainder per modulus and a long absolutePosition. For each ASCII digit, update every remainder with (remainder * 10 + digit) % modulus. Because each modulus is int, the intermediate fits in long.

At chunk boundaries, retain the remainders and position; no digit history is needed.

**Complexity:** O(d * m) time and O(m) space for d digits and m moduli.

### M10 solution: Binary search on a numeric answer

First clarify units. A common form supplies each worker's positive duration per task. At time t, that worker finishes t / duration tasks.

The feasibility predicate sums contributions but returns true as soon as total >= target. To avoid sum overflow, add only up to the remaining target or use:

~~~java
if (completed >= target - contribution) {
    return true;
}
completed += contribution;
~~~

Find an upper bound by doubling time until feasible, checking overflow. Then binary-search the first feasible time with low + (high - low) / 2.

**Complexity:** O(workers * log answer) time and O(1) extra space.

**Critical invariant:** All times below low are infeasible, and high is feasible.

## 16.14 Follow-up-chain model checkpoints

### F01 checkpoints: Reverse integer

1. Repeatedly take digit = value % 10 and remove it with value /= 10.
2. Decide whether to process signed remainders directly or promote the magnitude to long. Zero must produce zero.
3. A long accumulator is sufficient for int input; apply the sign and range-check before narrowing.
4. Without a wider accumulator, check the current result against Integer.MAX_VALUE / 10 and Integer.MIN_VALUE / 10 before result * 10 + digit. At equal boundaries, the final positive digit may be at most 7 and the final negative digit at least -8.
5. OptionalInt, a sealed domain result, or an exception distinguishes overflow from a valid reversed zero. State which policy the caller needs.

### F02 checkpoints: Base conversion

1. Horner accumulation processes each bit with result = result * 2 + bit.
2. Validate null, empty input, and every character before using its numeric value.
3. Before an update, require result <= (Long.MAX_VALUE - digit) / base.
4. Validate base 2 through 36 and use Character.digit.
5. Accumulate negatively under a sign-specific limit so Long.MIN_VALUE is representable. Reject a sign-only input.

### F03 checkpoints: GCD and scheduling

1. Use Euclid's replacement (a, b) = (b, a % b).
2. gcd(a, 0) is a, and this contract defines gcd(0, 0) as zero.
3. For nonzero inputs, lcm = (a / gcd(a, b)) * b.
4. Check the final product or use Math.multiplyExact. Handle zero before dividing.
5. Fold schedules one at a time with safe LCM. Stop immediately if the partial alignment exceeds the deadline or becomes unrepresentable.

### F04 checkpoints: Huge-number processing

1. Carry only the prefix remainder.
2. Persist remainder and absolute position across chunks.
3. Validate each character before arithmetic and report position + localIndex.
4. Keep one remainder per modulus; time becomes O(d * m).
5. For ordered chunk composition, summarize each chunk by its remainder and digit length. Combine left and right as (leftRemainder * 10^rightLength + rightRemainder) % modulus using overflow-safe modular arithmetic. The reduction tree must preserve chunk order.

### F05 checkpoints: Root search

1. Reject negative values and verify an integer candidate.
2. Compare candidate <= value / candidate.
3. Binary-search the last true candidate in a nonnegative long range.
4. Define predicate powerAtMost(candidate, k, value), stopping before a multiply that would exceed value.
5. The predicate is monotonic: once candidate^k exceeds value, every larger nonnegative candidate also exceeds it. Preserve the last true answer and move low or high without discarding a possible boundary.

## 16.15 Final revision cheat sheet

### Java integer ranges

| Type | Width | Minimum | Maximum |
|---|---:|---:|---:|
| byte | 8 | -2^7 | 2^7 - 1 |
| short | 16 | -2^15 | 2^15 - 1 |
| int | 32 | -2^31 | 2^31 - 1 |
| long | 64 | -2^63 | 2^63 - 1 |

Remember:

- promote an int before Math.abs when MIN_VALUE is possible;
- 1L, not 1, selects a long shift;
- int arithmetic can overflow before long assignment;
- exact APIs throw rather than wrap.

### Powers of two

| Power | Decimal |
|---:|---:|
| 2^10 | 1,024 |
| 2^20 | 1,048,576 |
| 2^30 | 1,073,741,824 |
| 2^31 | 2,147,483,648 |
| 2^32 | 4,294,967,296 |
| 2^62 | 4,611,686,018,427,387,904 |
| 2^63 | 9,223,372,036,854,775,808 |

2^30 is the largest positive power of two that fits in int. 2^62 is the largest positive power of two that fits in long.

### Core arithmetic identities

~~~text
last decimal digit        n % 10
remove last digit         n / 10
Horner parse              result * base + digit
GCD step                  gcd(a, b) = gcd(b, a % b)
safe LCM shape            (a / gcd(a, b)) * b
factor boundary           divisor <= n / divisor
safe square boundary      root <= n / root
safe index midpoint       left + (right - left) / 2
power of two              n > 0 && (n & (n - 1)) == 0
large decimal remainder   (remainder * 10 + digit) % modulus
~~~

### Logarithm intuition

| Repeated change | Rounds |
|---|---:|
| Remove one item | O(n) |
| Halve remaining work | O(log n) |
| Process n items at each halving level | O(n log n) |
| Make two choices for each item | O(2^n) |

For positive n:

~~~text
highest set-bit index = floor(log2(n))
bit length            = floor(log2(n)) + 1
doublings to reach n  = ceil(log2(n))
~~~

### Shift rules

| Expression | Behavior |
|---|---|
| value << k | Shift left; discarded high bits do not throw |
| value >> k | Signed right shift with sign extension |
| value >>> k | Unsigned right shift with zero fill |
| int distance | k & 31 |
| long distance | k & 63 |

byte, short, and char promote to int before shifting.

### Parsing and character rules

- Integer.parseInt accepts one leading + or - and no automatic trimming.
- Leading zeros remain decimal for parseInt(String).
- Validate Character.digit(character, base) >= 0.
- character - '0' is for validated ASCII decimal digits.
- Detect parse overflow before multiply-add.
- Decide whether signs, prefixes, whitespace, separators, and Unicode digits are allowed.

### Equality and comparison

| Need | Use |
|---|---|
| Primitive int equality | == |
| Same-wrapper value equality | equals with null policy |
| Nullable wrapper equality | Objects.equals |
| int ordering | Integer.compare |
| long ordering | Long.compare |
| double ordering | Double.compare |
| Approximate measurement equality | documented absolute/relative tolerance |

Do not sort fixed-width integers by subtraction.

### Modulo rules

~~~text
-13 % 5                = -3
Math.floorMod(-13, 5)  = 2
~~~

Use floorMod for a nonnegative state under a positive modulus. For huge decimal strings, carry only the remainder.

### Problem-recognition map

| Signal | First pattern to consider |
|---|---|
| Repeated decimal digits | quotient/remainder by 10 |
| Base-b text | Horner accumulation |
| Output in base b | repeated division and reverse |
| Divisors or prime | factor pairs through sqrt(n) |
| Repeating cycles | GCD or safe LCM |
| Huge number, only divisibility | streaming remainder |
| Huge number addition | right-to-left carry |
| Huge number comparison | canonical length then lexicographic |
| Exact primitive arithmetic | pre-check or Math.*Exact |
| Sorted range or monotonic answer | safe binary search |

### Required boundary tests

- zero and one;
- negative values where allowed;
- Integer.MIN_VALUE and Integer.MAX_VALUE;
- Long.MIN_VALUE and Long.MAX_VALUE;
- empty, null, sign-only, invalid digit, and leading zeros;
- exact square and values immediately around it;
- exact power of two and neighbors;
- one operation that fits and one that overflows;
- modulus one, negative dividend, and invalid modulus;
- equal numeric strings with different leading zeros.

## 16.16 Readiness assessment

Score the completed work out of 100:

| Area | Points | Full-credit condition |
|---|---:|---|
| Conceptual C01-C30 | 30 | Correct reason, not only conclusion |
| Output O01-O20 | 20 | Exact output or exception plus type reasoning |
| Debugging D01-D20 | 20 | Failing input, violated contract, and repair |
| Short coding S01-S20 | 20 | Compiles, correct boundaries, complexity stated |
| Medium M01-M10 | 10 | Correct invariant and implementable design |

### Interpretation

- **90-100: Interview ready for this volume.** Begin timed mixed practice and the next series volume.
- **75-89: Nearly ready.** Rework every missed boundary and repeat the assessment within three days.
- **60-74: Partial foundation.** Revisit the relevant chapters, then implement the short set again without notes.
- **Below 60: Rebuild before adding topics.** Focus on types, overflow, digit traversal, base conversion, and GCD/root invariants.

### Mandatory quality gates

A high score is not sufficient if any of these statements is false:

- [ ] I state the input contract before coding.
- [ ] I can explain every numeric type in my solution.
- [ ] I test zero, signs, and fixed-width boundaries.
- [ ] I detect overflow before trusting a result.
- [ ] I state time and space complexity with the correct input variable.
- [ ] I can dry-run the invariant aloud.
- [ ] I do not use BigInteger merely to hide a simpler streaming invariant.
- [ ] I do not avoid BigInteger when the exact arbitrary-precision value is genuinely required.
- [ ] I can complete all five follow-up chains without losing the original contract.

### Five-minute oral readiness drill

Without notes, explain:

1. safe reverse integer;
2. generic base parsing with overflow;
3. prime checking through a division boundary;
4. safe GCD and LCM;
5. huge decimal string modulo;
6. integer square root;
7. Java overflow before widening;
8. negative remainder and floorMod;
9. boxed equality and null unboxing;
10. comparator overflow.

If any explanation takes more than 30 seconds to reach the invariant, mark it for one more review cycle.

## 16.17 Chapter summary

- Interview readiness depends on retrieval, implementation, debugging, and explanation.
- Numeric correctness begins with operand types and input contracts.
- The same small set of invariants solves a large family of number problems.
- Delayed answers reveal whether knowledge was recalled or merely recognized.
- Boundary tests are part of the solution, not optional polish.
- Follow-up chains test whether the invariant survives changing constraints.

## 16.18 Final revision checklist

- [ ] I attempted all 30 conceptual questions before reading answers.
- [ ] I predicted all 20 code outputs before running them.
- [ ] I diagnosed all 20 debugging tasks with failing inputs.
- [ ] I implemented all 20 short exercises.
- [ ] I designed all 10 medium solutions.
- [ ] I rehearsed all 5 follow-up chains.
- [ ] I scored the readiness assessment honestly.
- [ ] I can use the cheat sheet as a prompt, not as a substitute for reasoning.
