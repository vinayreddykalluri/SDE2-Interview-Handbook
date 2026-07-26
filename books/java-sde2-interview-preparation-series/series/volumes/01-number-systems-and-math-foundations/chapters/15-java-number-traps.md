# Chapter 15: Java Interview Traps Related to Numbers

Many Java number questions are not mathematics questions. They test whether the candidate tracks compile-time types, promotions, boxing, fixed-width overflow, parsing contracts, and floating-point semantics. The dangerous answers are often plausible because they would be correct in mathematical arithmetic or in a different programming language.

The defense is a repeatable evaluation order:

1. Determine the compile-time type of each operand.
2. Apply unboxing and numeric promotion.
3. Evaluate the operation in that promoted type.
4. Account for overflow, truncation, or floating-point rounding.
5. Apply assignment conversion or an explicit cast last.

## 15.1 Learning objectives

After this chapter, you should be able to:

- choose equals rather than identity for boxed numeric values;
- explain the limited boxing identity guarantee without relying on cache accidents;
- identify hidden unboxing and null failures;
- predict integer division and remainder;
- compare floating-point results under an explicit policy;
- place casts or long operands before an operation;
- detect overflow that occurs before assignment;
- handle MIN_VALUE absolute-value traps;
- distinguish remainder from floor modulus;
- predict shift promotion and distance masking;
- validate parsing signs, zeros, digits, and range;
- avoid comparator subtraction overflow.

## 15.2 Boxed identity versus numeric equality

The operator == compares primitive numeric values when both operands are primitive. When both operands are references such as Integer, it compares object identity.

~~~java
Integer first = 1_000;
Integer second = 1_000;

System.out.println(first == second);      // do not rely on this
System.out.println(first.equals(second)); // true
~~~

Boxing selected constant values has an identity guarantee. In particular, boxed int constants from -128 through 127 are required to share identity in the situations described by the language specification. Implementations may cache additional values. Neither fact makes identity the right value-comparison operation.

~~~java
Integer smallFirst = 127;
Integer smallSecond = 127;
System.out.println(smallFirst == smallSecond); // true
~~~

The interview rule is simple:

- use == for primitive numeric comparison;
- use equals for same-wrapper value equality when null handling is explicit;
- use Objects.equals when either wrapper may be null;
- use a numeric conversion or comparator when wrapper types differ.

Wrapper equals methods are type-sensitive:

~~~java
Integer integer = 1;
Long longValue = 1L;
System.out.println(integer.equals(longValue)); // false
~~~

Both objects represent the mathematical value one, but Integer.equals requires another Integer.

### Common candidate mistake

Explaining a large boxed comparison as "always false." Identity outside the required cache range is not a stable value contract. The correct conclusion is that the program should not depend on it.

## 15.3 Autoboxing, unboxing, and null

Autoboxing converts a primitive to its wrapper when the context requires a reference. Unboxing extracts a primitive when an arithmetic or primitive context requires it.

~~~java
Integer boxed = 40;      // boxing
int result = boxed + 2;  // unboxing, then int addition
~~~

If boxed is null, unboxing throws NullPointerException:

~~~java
Integer missing = null;
int value = missing; // NullPointerException
~~~

The exception occurs at the unboxing point, not when null was assigned.

Hidden unboxing can occur in:

- arithmetic;
- comparison with a primitive;
- switch expressions or statements;
- method invocation requiring a primitive;
- compound assignments;
- the conditional operator, depending on operand types.

Prefer primitives for required numeric values. If absence is meaningful, validate or model it before arithmetic:

~~~java
static int requireValue(Integer value) {
    if (value == null) {
        throw new IllegalArgumentException("value is required");
    }
    return value;
}
~~~

### Overload awareness

Overload selection considers primitive widening, boxing, and varargs under ordered rules. Do not guess from runtime wrapper identity. Determine the declared argument type and applicable methods at compile time.

## 15.4 Integer division

When both operands are integral, Java performs integral division and discards the fractional part. The result rounds toward zero.

~~~java
System.out.println(7 / 2);   // 3
System.out.println(-7 / 2);  // -3
System.out.println(7 / -2);  // -3
~~~

A later assignment to double does not recover the fraction:

~~~java
double wrong = 7 / 2;        // 3.0
double correct = 7.0 / 2;    // 3.5
double alsoCorrect = (double) 7 / 2;
~~~

At least one operand must become floating point before division.

Integer division by zero throws ArithmeticException. Floating-point division by zero follows IEEE 754 behavior and may produce infinity or NaN:

~~~java
System.out.println(1.0 / 0.0); // Infinity
System.out.println(0.0 / 0.0); // NaN
~~~

## 15.5 Floating-point precision

Most finite decimal fractions do not have an exact finite binary floating-point representation. Therefore:

~~~java
double computed = 0.1 + 0.2;
System.out.println(computed == 0.3); // false
~~~

This is not random error. Each operand is rounded to a representable binary value, and the arithmetic result is rounded again.

### Choose a comparison policy

There is no universal epsilon. A useful policy depends on the scale, units, accumulated operations, and domain.

For many interview examples, combine absolute and relative tolerance:

~~~java
static boolean nearlyEqual(
        double first,
        double second,
        double absoluteTolerance,
        double relativeTolerance) {
    if (!Double.isFinite(absoluteTolerance)
            || !Double.isFinite(relativeTolerance)
            || absoluteTolerance < 0
            || relativeTolerance < 0) {
        throw new IllegalArgumentException(
                "tolerances must be finite and nonnegative");
    }
    if (Double.isNaN(first) || Double.isNaN(second)) {
        return false;
    }
    if (first == second) {
        return true;
    }
    if (!Double.isFinite(first) || !Double.isFinite(second)) {
        return false;
    }
    double difference = Math.abs(first - second);
    double scale = Math.max(Math.abs(first), Math.abs(second));
    return difference <= Math.max(
            absoluteTolerance,
            relativeTolerance * scale);
}
~~~

The `first == second` check deliberately accepts equal infinities and both signed zeros. NaN is handled first because NaN is unequal to every value, including itself. After the exact-equality case, any remaining infinity is rejected; otherwise both the difference and relative threshold could become infinity and create a false match. Tolerances must themselves be finite and nonnegative.

For ordering, Double.compare provides a defined total ordering suitable for comparators. Approximate equality is generally not a valid comparator equivalence because it may violate transitivity.

### Decimal business values

When exact decimal arithmetic is required, BigDecimal may be appropriate:

~~~java
import java.math.BigDecimal;

BigDecimal tenth = new BigDecimal("0.1");
BigDecimal twoTenths = new BigDecimal("0.2");
System.out.println(tenth.add(twoTenths)); // 0.3
~~~

Construct from a decimal string or use BigDecimal.valueOf when starting from a double. new BigDecimal(0.1) preserves the exact binary double value, which is usually not the intended decimal 0.1.

## 15.6 Casting order

A cast changes the value and type at the point where it appears. Casting the result is different from casting an operand.

~~~java
int completed = 3;
int total = 4;

double wrong = (double) (completed / total); // 0.0
double right = (double) completed / total;   // 0.75
~~~

In wrong, integer division happens inside the parentheses before the cast. In right, completed becomes double first, so binary numeric promotion makes the division floating point.

Narrowing casts can discard high bits:

~~~java
int value = 130;
byte narrowed = (byte) value; // -126
~~~

The cast does not clamp to Byte.MAX_VALUE and does not throw.

## 15.7 Overflow before assignment

An expression is evaluated according to operand types, not the destination type.

~~~java
long wrong = Integer.MAX_VALUE * 2;
long right = Integer.MAX_VALUE * 2L;
~~~

In wrong, both operands are int, so multiplication overflows in int and the wrapped result is widened to long. In right, 2L promotes the other operand and multiplication occurs in long.

The same issue appears with constants and variables:

~~~java
long seconds = 30 * 24 * 60 * 60; // fits here, but fragile
long safer = 30L * 24 * 60 * 60;
~~~

Put the long operand early enough that every subsequent multiplication is long.

Exact APIs make the policy explicit:

~~~java
long product = Math.multiplyExact(first, second);
long sum = Math.addExact(first, second);
int incremented = Math.incrementExact(value);
~~~

They throw ArithmeticException when the exact mathematical result does not fit.

## 15.8 Math.abs and MIN_VALUE

Two's-complement signed types have one more negative value than positive value. Therefore:

~~~java
System.out.println(Math.abs(Integer.MIN_VALUE));
// -2147483648
~~~

No positive int can represent 2,147,483,648.

For an int magnitude, promote first:

~~~java
long magnitude = Math.abs((long) value);
~~~

For Long.MIN_VALUE, no wider primitive integer exists. Choose among:

- reject or special-case it;
- use BigInteger;
- use unsigned magnitude logic;
- structure the algorithm to avoid absolute value;
- accumulate in the negative range, as robust signed parsers do.

Negating MIN_VALUE has the same trap:

~~~java
int stillMinimum = -Integer.MIN_VALUE;
~~~

## 15.9 Remainder versus mathematical modulus

Java's % is remainder, and its sign follows the dividend:

~~~java
System.out.println(-13 % 5); // -3
System.out.println(13 % -5); // 3
~~~

For circular indices with positive modulus, use Math.floorMod:

~~~java
System.out.println(Math.floorMod(-13, 5)); // 2
~~~

The common normalization expression:

~~~java
((value % modulus) + modulus) % modulus
~~~

works under suitable bounds and a positive modulus, but Math.floorMod states the intent and handles fixed-width details clearly.

## 15.10 Shift behavior

Shift traps combine promotion, sign handling, overflow, and distance masking.

### Operand promotion

byte, short, and char are promoted to int before shifting:

~~~java
byte one = 1;
int shifted = one << 8; // int 256
~~~

### Width comes from the left operand

~~~java
long wrong = 1 << 40;  // int shift; distance becomes 8
long right = 1L << 40; // long shift
~~~

### Distance is masked

- int uses distance & 31;
- long uses distance & 63.

~~~java
System.out.println(1 << 32);  // 1
System.out.println(1L << 64); // 1
~~~

### Signed and unsigned right shift

~~~java
System.out.println(-1 >> 1);  // -1
System.out.println(-1 >>> 1); // 2147483647
~~~

Do not replace signed division with right shift without checking rounding for negative odd values.

## 15.11 Parsing contracts

Parsing can fail because of syntax or range:

~~~java
Integer.parseInt("+42");         // 42
Integer.parseInt("-42");         // -42
Integer.parseInt("00042");       // 42, decimal
Integer.parseInt(" 42 ");        // NumberFormatException
Integer.parseInt("2147483648");  // NumberFormatException
~~~

Integer.parseInt does not trim whitespace automatically. It accepts one leading plus or minus sign but not a sign by itself.

Leading zeros do not make parseInt(String) octal. Radix-sensitive APIs have separate contracts:

~~~java
Integer.parseInt("10", 2);  // 2
Integer.parseInt("FF", 16); // 255
~~~

Integer.decode recognizes prefixes such as 0x and # and has different leading-zero behavior. Do not substitute it for a clearly specified decimal parser.

### Parsing unsigned text

Integer.parseUnsignedInt can parse a nonnegative 32-bit value that does not fit in positive int, but the returned int may have a negative signed interpretation. Use Integer.toUnsignedLong or unsigned comparison/formatting APIs when needed.

### Exception policy

For trusted configuration, allowing NumberFormatException to identify invalid input may be fine. At a public boundary, validate length, signs, radix, and error reporting according to the API contract. Do not catch every exception and silently return zero; zero may be a valid input.

## 15.12 Leading signs and zeros

A robust parser must state:

- whether one leading + or - is accepted;
- whether the sign may be followed by separators or whitespace;
- whether leading zeros are allowed;
- whether "-0" canonicalizes to "0";
- whether a prefix such as 0x is accepted;
- what happens on a sign-only string;
- whether non-ASCII digits are accepted.

For interview code, a narrow explicit contract is better than accidental flexibility.

Leading zeros matter differently by task:

- numeric comparison should ignore them after validation;
- string identity should preserve them;
- fixed-width identifiers may treat them as significant;
- decimal parsing treats them as ordinary decimal zeros;
- Integer.decode can interpret prefixes under its own rules.

## 15.13 Character-to-digit conversion

Subtracting '0' is clear for validated ASCII decimal digits:

~~~java
static int asciiDecimalDigit(char value) {
    if (value < '0' || value > '9') {
        throw new IllegalArgumentException("not an ASCII digit");
    }
    return value - '0';
}
~~~

Do not subtract '0' before validation. For hexadecimal and other radices, use Character.digit:

~~~java
static int digitInBase(char value, int base) {
    int digit = Character.digit(value, base);
    if (digit < 0) {
        throw new IllegalArgumentException("digit not valid in base");
    }
    return digit;
}
~~~

Character.digit handles letter case and more Unicode characters than an ASCII-only contract. If the input must be ASCII, add an explicit character-range policy rather than assuming the method enforces one.

## 15.14 Comparator overflow

A comparator's sign communicates order. Subtraction can wrap and report the wrong sign:

~~~java
// Unsafe
Comparator<Integer> wrong =
        (first, second) -> first - second;

// Safe
Comparator<Integer> right = Integer::compare;
~~~

Consider first = Integer.MIN_VALUE and second = 1. The mathematical difference is below Integer.MIN_VALUE, so int subtraction wraps positive. The unsafe comparator claims the minimum value is greater than one.

For long use Long.compare, for double use Double.compare, and for object keys use Comparator.comparingInt, comparingLong, or comparing.

Comparator correctness requires antisymmetry, transitivity, and consistent zero-equivalence. Approximate floating-point equality usually should not define comparator equality.

## 15.15 Additional promotion trap: compound assignment

Compound assignment includes an implicit cast to the left-hand type:

~~~java
byte value = 127;
value += 1;
System.out.println(value); // -128
~~~

The expression behaves roughly like:

~~~java
value = (byte) (value + 1);
~~~

By contrast, value = value + 1 does not compile without a cast because value + 1 is int.

Unary and binary numeric promotion also mean:

~~~java
byte small = 10;
int negated = -small;
int added = small + small;
~~~

Both results are int.

## 15.16 Runnable trap laboratory

~~~java
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;

public final class JavaNumberTraps {
    private JavaNumberTraps() {
    }

    public static boolean nearlyEqual(
            double first,
            double second,
            double absoluteTolerance,
            double relativeTolerance) {
        if (!Double.isFinite(absoluteTolerance)
                || !Double.isFinite(relativeTolerance)
                || absoluteTolerance < 0
                || relativeTolerance < 0) {
            throw new IllegalArgumentException(
                    "tolerances must be finite and nonnegative");
        }
        if (Double.isNaN(first) || Double.isNaN(second)) {
            return false;
        }
        if (first == second) {
            return true;
        }
        if (!Double.isFinite(first) || !Double.isFinite(second)) {
            return false;
        }
        double difference = Math.abs(first - second);
        double scale = Math.max(
                Math.abs(first), Math.abs(second));
        return difference <= Math.max(
                absoluteTolerance,
                relativeTolerance * scale);
    }

    public static int asciiDecimalDigit(char value) {
        if (value < '0' || value > '9') {
            throw new IllegalArgumentException(
                    "not an ASCII decimal digit");
        }
        return value - '0';
    }

    public static int digitInBase(char value, int base) {
        int digit = Character.digit(value, base);
        if (digit < 0) {
            throw new IllegalArgumentException(
                    "digit is not valid in base " + base);
        }
        return digit;
    }

    public static List<Integer> sortedValues(
            List<Integer> input) {
        List<Integer> result = new ArrayList<>(input);
        result.sort(Comparator.naturalOrder());
        return List.copyOf(result);
    }

    public static void main(String[] args) {
        Integer smallFirst = 127;
        Integer smallSecond = 127;
        Integer largeFirst = 1_000;
        Integer largeSecond = 1_000;

        System.out.println(smallFirst == smallSecond);
        System.out.println(largeFirst.equals(largeSecond));
        System.out.println(Objects.equals(null, null));

        double truncated = 7 / 2;
        double divided = 7.0 / 2;
        System.out.println(truncated); // 3.0
        System.out.println(divided);   // 3.5

        System.out.println(nearlyEqual(
                0.1 + 0.2, 0.3, 1e-12, 1e-12));

        long overflowedBeforeWidening =
                Integer.MAX_VALUE * 2;
        long multipliedAsLong =
                Integer.MAX_VALUE * 2L;
        System.out.println(overflowedBeforeWidening);
        System.out.println(multipliedAsLong);

        System.out.println(Math.abs(Integer.MIN_VALUE));
        System.out.println(
                Math.abs((long) Integer.MIN_VALUE));
        System.out.println(-13 % 5);
        System.out.println(Math.floorMod(-13, 5));
        System.out.println(1 << 32);
        System.out.println(1L << 40);

        System.out.println(Integer.parseInt("+00042"));
        System.out.println(asciiDecimalDigit('7'));
        System.out.println(digitInBase('F', 16));

        BigDecimal exact = new BigDecimal("0.1")
                .add(new BigDecimal("0.2"));
        System.out.println(exact);

        System.out.println(sortedValues(List.of(
                Integer.MAX_VALUE,
                0,
                Integer.MIN_VALUE)));
    }
}
~~~

Every arithmetic example uses a type chosen to expose or prevent the intended behavior. The class does not use boxed identity as a correctness decision.

## 15.17 Interview questions

1. Why can two equal Integer values fail an == comparison?
2. What boxing identity is guaranteed, and why should code still use equals?
3. Why does Integer.valueOf(1).equals(Long.valueOf(1)) return false?
4. Where can null unboxing occur implicitly?
5. Why is double ratio = 1 / 2 equal to 0.0?
6. Why is 0.1 + 0.2 usually not equal to 0.3 by ==?
7. What should determine a floating-point tolerance?
8. Why does casting after integer division not restore the fraction?
9. Why can an int multiplication overflow before assignment to long?
10. Why can Math.abs return a negative int?
11. How do % and Math.floorMod differ for negative values?
12. Why is 1 << 40 not a 64-bit shift?
13. What shift does 1 << 32 perform?
14. Which sign forms does Integer.parseInt accept?
15. How should an API distinguish invalid input from a valid zero?
16. When is character - '0' appropriate?
17. Why is subtraction unsafe in a comparator?
18. Why does byteValue += 1 compile while byteValue = byteValue + 1 does not?

## 15.18 Practice set

Answers are deliberately later in the chapter.

### Quick check

1. Predict whether Integer a = 127; Integer b = 127; a == b is true.
2. Predict double value = (double) (5 / 2).
3. Predict long value = Integer.MAX_VALUE + 1.
4. Predict Math.floorMod(-1, 8).
5. Predict 1L << 65.

### Coding practice

1. **Foundation:** Parse one validated ASCII decimal digit.
2. **Foundation:** Return the nonnegative magnitude of any int as long.
3. **Interview Core:** Implement safeRatio(int numerator, int denominator) with an explicit zero policy.
4. **Interview Core:** Compare two nullable Integer values with nulls last.
5. **Interview Core:** Parse an optional signed decimal int without trimming whitespace and without using Integer.parseInt.
6. **SDE-2 Follow-up:** Design a Money value object that prevents binary floating-point arithmetic.

### Debugging tasks

1. Repair long area = width * height when width and height are int.
2. Repair a circular index computed as (index + delta) % length.
3. Repair a comparator that returns first.priority - second.priority.
4. Repair a nullable Integer counter increment.
5. Repair a double loop that waits for value == target after repeated additions.

### Interview extension

Explain how you would review an unfamiliar numeric expression in production code. Include operand types, promotions, exceptional cases, overflow policy, rounding policy, and tests.

## 15.19 Delayed answer notes

### Quick-check answers

1. True under the boxing identity guarantee for the constant 127.
2. 2.0. Integer division occurs before the cast.
3. -2,147,483,648 widened to long. Both addition operands are int.
4. 7.
5. The long distance is 65 & 63, so it is 1L << 1, which is 2.

### Coding guidance

- Validate '0' <= character && character <= '9' before subtraction.
- Use Math.abs((long) value) for any int magnitude.
- Check a zero denominator before converting an operand to double.
- Comparator.nullsLast(Integer::compare) expresses nullable ordering.
- A manual parser should validate one optional sign, require at least one digit, accumulate with a range guard, and handle Integer.MIN_VALUE.
- A Money type commonly stores a scaled long under one currency and rounding contract or wraps BigDecimal with fixed scale and explicit rounding.

### Debugging resolutions

Promote before multiplication: long area = (long) width * height.

For a positive length, normalize with Math.floorMod(index + delta, length). If index + delta can overflow, widen before adding or normalize each operand.

Use Integer.compare(first.priority, second.priority), then add stable tie-breakers if the domain needs them.

Validate the nullable counter before unboxing, or choose a primitive/default policy explicitly.

Do not wait for repeated floating-point addition to hit an exact target. Use a bounded iteration count, compare under a justified tolerance, or model the step count with integer units.

### Review checklist answer

Read the declared operand types first. Mark every boxing, unboxing, and promotion. Determine the operation type before looking at the assignment target. Check division by zero, MIN_VALUE asymmetry, shift distance, parsing range, and null. Define overflow and rounding policies. Test zero, signs, bounds, invalid text, and values immediately around each threshold.

## 15.20 Chapter summary

- Boxed == is identity, not general numeric equality.
- Unboxing null throws at the unboxing point.
- Integral division truncates toward zero.
- A cast must occur before division to change its arithmetic type.
- Binary floating-point needs a domain-specific comparison policy.
- Expressions overflow according to operand types before assignment.
- MIN_VALUE has no positive counterpart in the same signed type.
- Java % can be negative; Math.floorMod supports nonnegative modular states.
- Shift width comes from the left operand, and distance is masked.
- Parsing must define signs, zeros, whitespace, radix, digits, and range.
- Character.digit and ASCII subtraction serve different contracts.
- Numeric comparators should use compare APIs, not subtraction.

## 15.21 Revision checklist

- [ ] I never use boxed identity as value equality.
- [ ] I can find hidden unboxing.
- [ ] I predict integer division before looking at the destination type.
- [ ] I place casts and long operands before the operation they must affect.
- [ ] I can explain floating-point tolerance as a policy.
- [ ] I handle Integer.MIN_VALUE and Long.MIN_VALUE explicitly.
- [ ] I distinguish remainder from floor modulus.
- [ ] I know shift promotion and distance masking.
- [ ] I state parsing syntax and range contracts.
- [ ] I use Integer.compare, Long.compare, and Double.compare appropriately.
