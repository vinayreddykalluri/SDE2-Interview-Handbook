import java.io.PrintStream;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.OptionalInt;
import java.util.StringJoiner;

/**
 * Dependency-free Java 21 reference implementations for the Number Systems
 * and Math Foundations interview volume.
 *
 * <p>Methods reject inputs outside their documented contracts instead of
 * silently changing bases, signs, or ranges. Operations whose mathematical
 * result may exceed {@code long} either throw {@link ArithmeticException} or
 * provide a {@link BigInteger} alternative.</p>
 */
public final class NumberSystemsAlgorithms {
    private static final int[] DIGIT_FACTORIALS = {
            1, 1, 2, 6, 24, 120, 720, 5_040, 40_320, 362_880
    };
    private static final long MAX_LONG_SQUARE_ROOT = 3_037_000_499L;

    private NumberSystemsAlgorithms() {}

    // ---------------------------------------------------------------------
    // Decimal digit algorithms
    // ---------------------------------------------------------------------

    /** Returns the decimal digit count of any long. Zero has one digit. */
    public static int countDigits(long value) {
        if (value == 0) return 1;

        int count = 0;
        long remaining = value;
        while (remaining != 0) {
            remaining /= 10;
            count++;
        }
        return count;
    }

    /** Returns the sum of decimal digit magnitudes, ignoring the sign. */
    public static int sumDigits(long value) {
        if (value == 0) return 0;

        long remaining = value > 0 ? -value : value;
        int sum = 0;
        while (remaining != 0) {
            int digit = (int) -(remaining % 10);
            sum += digit;
            remaining /= 10;
        }
        return sum;
    }

    /** Returns the product of decimal digit magnitudes, ignoring the sign. */
    public static long productDigits(long value) {
        if (value == 0) return 0;

        long remaining = value > 0 ? -value : value;
        long product = 1;
        while (remaining != 0) {
            int digit = (int) -(remaining % 10);
            product = Math.multiplyExact(product, digit);
            remaining /= 10;
        }
        return product;
    }

    /** Returns the smallest decimal digit magnitude. Zero returns zero. */
    public static int minimumDigit(long value) {
        long remaining = value > 0 ? -value : value;
        int minimum = 9;
        do {
            minimum = Math.min(minimum, (int) -(remaining % 10));
            remaining /= 10;
        } while (remaining != 0);
        return minimum;
    }

    /** Returns the largest decimal digit magnitude. Zero returns zero. */
    public static int maximumDigit(long value) {
        long remaining = value > 0 ? -value : value;
        int maximum = 0;
        do {
            maximum = Math.max(maximum, (int) -(remaining % 10));
            remaining /= 10;
        } while (remaining != 0);
        return maximum;
    }

    /** Counts occurrences of targetDigit in the decimal magnitude. */
    public static int countDigitOccurrences(long value, int targetDigit) {
        if (targetDigit < 0 || targetDigit > 9) {
            throw new IllegalArgumentException("targetDigit must be 0 through 9");
        }
        long remaining = value > 0 ? -value : value;
        int count = 0;
        do {
            if ((int) -(remaining % 10) == targetDigit) count++;
            remaining /= 10;
        } while (remaining != 0);
        return count;
    }

    /**
     * Reverses an int in decimal. Returns empty when the reversed value does
     * not fit in int.
     */
    public static OptionalInt reverseInt(int value) {
        int remaining = value;
        long reversed = 0;
        while (remaining != 0) {
            int digit = remaining % 10;
            reversed = reversed * 10 + digit;
            if (reversed < Integer.MIN_VALUE || reversed > Integer.MAX_VALUE) {
                return OptionalInt.empty();
            }
            remaining /= 10;
        }
        return OptionalInt.of((int) reversed);
    }

    /**
     * Reverses an int without using a wider accumulator. Returns empty before
     * the multiply-add step would cross an int boundary.
     */
    public static OptionalInt reverseIntStrict(int value) {
        int remaining = value;
        int reversed = 0;
        while (remaining != 0) {
            int digit = remaining % 10;
            if (reversed > Integer.MAX_VALUE / 10
                    || (reversed == Integer.MAX_VALUE / 10 && digit > 7)
                    || reversed < Integer.MIN_VALUE / 10
                    || (reversed == Integer.MIN_VALUE / 10 && digit < -8)) {
                return OptionalInt.empty();
            }
            reversed = reversed * 10 + digit;
            remaining /= 10;
        }
        return OptionalInt.of(reversed);
    }

    /** Returns true when a nonnegative long reads the same in both directions. */
    public static boolean isPalindromeNumber(long value) {
        if (value < 0) return false;
        if (value != 0 && value % 10 == 0) return false;

        long remaining = value;
        long reversedHalf = 0;
        while (remaining > reversedHalf) {
            reversedHalf = reversedHalf * 10 + remaining % 10;
            remaining /= 10;
        }
        return remaining == reversedHalf || remaining == reversedHalf / 10;
    }

    /** Returns true when value equals the sum of each digit raised to digit count. */
    public static boolean isArmstrongNumber(int value) {
        if (value < 0) return false;

        int digits = countDigits(value);
        int remaining = value;
        long sum = 0;
        do {
            int digit = remaining % 10;
            sum = Math.addExact(sum, powerOfDigit(digit, digits));
            remaining /= 10;
        } while (remaining != 0);
        return sum == value;
    }

    /** Returns true when value equals the sum of the factorials of its digits. */
    public static boolean isStrongNumber(int value) {
        if (value < 0) return false;

        int remaining = value;
        long sum = 0;
        do {
            sum += DIGIT_FACTORIALS[remaining % 10];
            remaining /= 10;
        } while (remaining != 0);
        return sum == value;
    }

    /** Returns n! exactly for 0 through 20; larger values do not fit in long. */
    public static long factorialExact(int value) {
        if (value < 0 || value > 20) {
            throw new IllegalArgumentException("value must be 0 through 20");
        }
        long result = 1;
        for (int factor = 2; factor <= value; factor++) {
            result = Math.multiplyExact(result, factor);
        }
        return result;
    }

    private static long powerOfDigit(int digit, int exponent) {
        long result = 1;
        for (int count = 0; count < exponent; count++) {
            result = Math.multiplyExact(result, digit);
        }
        return result;
    }

    // ---------------------------------------------------------------------
    // Base conversion
    // ---------------------------------------------------------------------

    /** Parses a signed binary string into long with digit and overflow checks. */
    public static long binaryStringToLong(String binary) {
        return baseToLong(binary, 2);
    }

    /** Formats a signed long as a mathematical base-2 value. */
    public static String decimalToBinary(long value) {
        return longToBase(value, 2);
    }

    /** Parses a signed ASCII value in a base from 2 through 36. */
    public static long baseToLong(String text, int base) {
        validateBase(base);
        if (text == null || text.isEmpty()) {
            throw new IllegalArgumentException("number must be nonempty");
        }

        int index = 0;
        boolean negative = false;
        char first = text.charAt(0);
        if (first == '+' || first == '-') {
            negative = first == '-';
            index++;
        }
        if (index == text.length()) {
            throw new IllegalArgumentException("sign requires digits");
        }

        long limit = negative ? Long.MIN_VALUE : -Long.MAX_VALUE;
        long multiplyLimit = limit / base;
        long result = 0;

        while (index < text.length()) {
            int digit = asciiDigit(text.charAt(index));
            if (digit < 0 || digit >= base) {
                throw new IllegalArgumentException(
                        "invalid base-" + base + " digit at index " + index);
            }
            if (result < multiplyLimit) {
                throw new ArithmeticException("value exceeds long");
            }
            result *= base;
            if (result < limit + digit) {
                throw new ArithmeticException("value exceeds long");
            }
            result -= digit;
            index++;
        }
        return negative ? result : -result;
    }

    /** Formats any signed long in a base from 2 through 36. */
    public static String longToBase(long value, int base) {
        validateBase(base);
        if (value == 0) return "0";

        boolean negative = value < 0;
        long remaining = negative ? value : -value;
        StringBuilder reversed = new StringBuilder();
        while (remaining != 0) {
            int digit = (int) -(remaining % base);
            reversed.append(Character.forDigit(digit, base));
            remaining /= base;
        }
        if (negative) reversed.append('-');
        return reversed.reverse().toString();
    }

    /** Converts an arbitrary-precision signed value between bases 2 through 36. */
    public static String convertArbitraryPrecision(
            String text, int sourceBase, int targetBase) {
        validateBase(sourceBase);
        validateBase(targetBase);
        if (text == null || text.isEmpty()) {
            throw new IllegalArgumentException("number must be nonempty");
        }
        try {
            return new BigInteger(text, sourceBase).toString(targetBase);
        } catch (NumberFormatException exception) {
            throw new IllegalArgumentException("invalid source number", exception);
        }
    }

    public static long hexadecimalToLong(String hexadecimal) {
        return baseToLong(hexadecimal, 16);
    }

    public static String decimalToHexadecimal(long value) {
        return longToBase(value, 16);
    }

    /** Returns whether text is a signed, nonempty ASCII number in the base. */
    public static boolean isValidNumberInBase(String text, int base) {
        validateBase(base);
        if (text == null || text.isEmpty()) return false;
        int index = text.charAt(0) == '+' || text.charAt(0) == '-' ? 1 : 0;
        if (index == text.length()) return false;
        while (index < text.length()) {
            int digit = asciiDigit(text.charAt(index++));
            if (digit < 0 || digit >= base) return false;
        }
        return true;
    }

    private static int asciiDigit(char value) {
        if (value >= '0' && value <= '9') return value - '0';
        if (value >= 'a' && value <= 'z') return value - 'a' + 10;
        if (value >= 'A' && value <= 'Z') return value - 'A' + 10;
        return -1;
    }

    private static void validateBase(int base) {
        if (base < Character.MIN_RADIX || base > Character.MAX_RADIX) {
            throw new IllegalArgumentException("base must be 2 through 36");
        }
    }

    // ---------------------------------------------------------------------
    // Very large decimal strings
    // ---------------------------------------------------------------------

    /**
     * Returns the mathematical remainder in [0, modulus) for an arbitrarily
     * long signed decimal string. Uses O(1) auxiliary numeric state.
     */
    public static long largeNumberModulo(String decimal, long modulus) {
        requirePositiveModulus(modulus);
        int index = signedDecimalStart(decimal);
        boolean negative = decimal.charAt(0) == '-';
        long remainder = 0;

        while (index < decimal.length()) {
            char current = decimal.charAt(index);
            if (current < '0' || current > '9') {
                throw new IllegalArgumentException(
                        "invalid decimal digit at index " + index);
            }
            int digit = current - '0';
            remainder = addModuloNormalized(
                    multiplyModulo(remainder, 10, modulus),
                    digit % modulus,
                    modulus);
            index++;
        }
        if (negative && remainder != 0) {
            return modulus - remainder;
        }
        return remainder;
    }

    public static boolean isDivisibleBy9(String decimal) {
        return largeNumberModulo(decimal, 9) == 0;
    }

    /** Applies the alternating-decimal-digit rule without parsing the value. */
    public static boolean isDivisibleBy11(String decimal) {
        int index = signedDecimalStart(decimal);
        int alternatingRemainder = 0;
        boolean add = true;
        while (index < decimal.length()) {
            char current = decimal.charAt(index);
            if (current < '0' || current > '9') {
                throw new IllegalArgumentException(
                        "invalid decimal digit at index " + index);
            }
            int digit = current - '0';
            alternatingRemainder = Math.floorMod(
                    alternatingRemainder + (add ? digit : -digit), 11);
            add = !add;
            index++;
        }
        return alternatingRemainder == 0;
    }

    /** Adds two signed decimal strings and returns a canonical decimal string. */
    public static String addNumericStrings(String left, String right) {
        DecimalParts a = parseDecimalParts(left);
        DecimalParts b = parseDecimalParts(right);

        if (a.negative() == b.negative()) {
            String magnitude = addMagnitudes(a.digits(), b.digits());
            return a.negative() && !magnitude.equals("0")
                    ? "-" + magnitude
                    : magnitude;
        }

        int magnitudeComparison = compareMagnitudes(a.digits(), b.digits());
        if (magnitudeComparison == 0) return "0";
        if (magnitudeComparison > 0) {
            String magnitude = subtractMagnitudes(a.digits(), b.digits());
            return a.negative() ? "-" + magnitude : magnitude;
        }
        String magnitude = subtractMagnitudes(b.digits(), a.digits());
        return b.negative() ? "-" + magnitude : magnitude;
    }

    /** Subtracts two signed decimal strings and returns a canonical result. */
    public static String subtractNumericStrings(String left, String right) {
        DecimalParts subtrahend = parseDecimalParts(right);
        String negated = subtrahend.digits().equals("0")
                ? "0"
                : subtrahend.negative()
                        ? subtrahend.digits()
                        : "-" + subtrahend.digits();
        return addNumericStrings(left, negated);
    }

    /** Multiplies a signed decimal string by one digit from 0 through 9. */
    public static String multiplyNumericStringByDigit(String decimal, int digit) {
        if (digit < 0 || digit > 9) {
            throw new IllegalArgumentException("digit must be 0 through 9");
        }
        DecimalParts parts = parseDecimalParts(decimal);
        if (digit == 0 || parts.digits().equals("0")) return "0";

        int carry = 0;
        StringBuilder reversed = new StringBuilder(parts.digits().length() + 1);
        for (int index = parts.digits().length() - 1; index >= 0; index--) {
            int product = (parts.digits().charAt(index) - '0') * digit + carry;
            reversed.append((char) ('0' + product % 10));
            carry = product / 10;
        }
        if (carry != 0) reversed.append(carry);
        if (parts.negative()) reversed.append('-');
        return reversed.reverse().toString();
    }

    /** Compares arbitrarily long signed decimal strings numerically. */
    public static int compareNumericStrings(String left, String right) {
        DecimalParts a = parseDecimalParts(left);
        DecimalParts b = parseDecimalParts(right);
        if (a.negative() != b.negative()) return a.negative() ? -1 : 1;

        int magnitudeComparison = compareMagnitudes(a.digits(), b.digits());
        return a.negative() ? -magnitudeComparison : magnitudeComparison;
    }

    /** Removes redundant leading zeros and a redundant positive sign. */
    public static String normalizeDecimalString(String decimal) {
        DecimalParts parts = parseDecimalParts(decimal);
        return parts.negative() ? "-" + parts.digits() : parts.digits();
    }

    private static int signedDecimalStart(String decimal) {
        if (decimal == null || decimal.isEmpty()) {
            throw new IllegalArgumentException("decimal must be nonempty");
        }
        int index = 0;
        char first = decimal.charAt(0);
        if (first == '+' || first == '-') index++;
        if (index == decimal.length()) {
            throw new IllegalArgumentException("sign requires digits");
        }
        return index;
    }

    private static DecimalParts parseDecimalParts(String decimal) {
        int start = signedDecimalStart(decimal);
        boolean negative = decimal.charAt(0) == '-';
        int firstNonzero = -1;
        for (int index = start; index < decimal.length(); index++) {
            char current = decimal.charAt(index);
            if (current < '0' || current > '9') {
                throw new IllegalArgumentException(
                        "invalid decimal digit at index " + index);
            }
            if (firstNonzero < 0 && current != '0') firstNonzero = index;
        }
        if (firstNonzero < 0) return new DecimalParts(false, "0");
        return new DecimalParts(negative, decimal.substring(firstNonzero));
    }

    private static String addMagnitudes(String left, String right) {
        int leftIndex = left.length() - 1;
        int rightIndex = right.length() - 1;
        int carry = 0;
        StringBuilder reversed = new StringBuilder(
                Math.max(left.length(), right.length()) + 1);

        while (leftIndex >= 0 || rightIndex >= 0 || carry != 0) {
            int leftDigit = leftIndex >= 0 ? left.charAt(leftIndex--) - '0' : 0;
            int rightDigit = rightIndex >= 0 ? right.charAt(rightIndex--) - '0' : 0;
            int sum = leftDigit + rightDigit + carry;
            reversed.append((char) ('0' + sum % 10));
            carry = sum / 10;
        }
        return reversed.reverse().toString();
    }

    /** Subtracts right from left where left magnitude is at least right. */
    private static String subtractMagnitudes(String left, String right) {
        int leftIndex = left.length() - 1;
        int rightIndex = right.length() - 1;
        int borrow = 0;
        StringBuilder reversed = new StringBuilder(left.length());

        while (leftIndex >= 0) {
            int leftDigit = left.charAt(leftIndex--) - '0' - borrow;
            int rightDigit = rightIndex >= 0 ? right.charAt(rightIndex--) - '0' : 0;
            if (leftDigit < rightDigit) {
                leftDigit += 10;
                borrow = 1;
            } else {
                borrow = 0;
            }
            reversed.append((char) ('0' + leftDigit - rightDigit));
        }

        while (reversed.length() > 1
                && reversed.charAt(reversed.length() - 1) == '0') {
            reversed.setLength(reversed.length() - 1);
        }
        return reversed.reverse().toString();
    }

    private static int compareMagnitudes(String left, String right) {
        if (left.length() != right.length()) {
            return Integer.compare(left.length(), right.length());
        }
        return left.compareTo(right);
    }

    private record DecimalParts(boolean negative, String digits) {}

    // ---------------------------------------------------------------------
    // Factors, primes, GCD, and LCM
    // ---------------------------------------------------------------------

    public static boolean isPrime(long value) {
        if (value < 2) return false;
        if (value % 2 == 0) return value == 2;
        for (long factor = 3; factor <= value / factor; factor += 2) {
            if (value % factor == 0) return false;
        }
        return true;
    }

    /** Returns true when value equals the sum of its positive proper factors. */
    public static boolean isPerfectNumber(long value) {
        if (value <= 1) return false;

        long sum = 1;
        for (long factor = 2; factor <= value / factor; factor++) {
            if (value % factor != 0) continue;
            if (sum > value - factor) return false;
            sum += factor;

            long partner = value / factor;
            if (partner != factor) {
                if (sum > value - partner) return false;
                sum += partner;
            }
        }
        return sum == value;
    }

    /** Returns all positive factors in ascending order. Requires value > 0. */
    public static List<Long> listFactors(long value) {
        requirePositive(value, "value");
        List<Long> lower = new ArrayList<>();
        List<Long> upper = new ArrayList<>();
        for (long factor = 1; factor <= value / factor; factor++) {
            if (value % factor == 0) {
                lower.add(factor);
                long partner = value / factor;
                if (partner != factor) upper.add(partner);
            }
        }
        Collections.reverse(upper);
        lower.addAll(upper);
        return List.copyOf(lower);
    }

    /** Prints positive factors as one ascending, space-separated line. */
    public static void printFactors(long value, PrintStream output) {
        Objects.requireNonNull(output, "output");
        StringJoiner joiner = new StringJoiner(" ");
        for (long factor : listFactors(value)) {
            joiner.add(Long.toString(factor));
        }
        output.println(joiner);
    }

    public static long countFactors(long value) {
        requirePositive(value, "value");
        long count = 0;
        for (long factor = 1; factor <= value / factor; factor++) {
            if (value % factor == 0) {
                count += factor == value / factor ? 1 : 2;
            }
        }
        return count;
    }

    /** Returns the exact sum of all positive factors or throws on long overflow. */
    public static long sumFactors(long value) {
        requirePositive(value, "value");
        long sum = 0;
        for (long factor = 1; factor <= value / factor; factor++) {
            if (value % factor != 0) continue;
            sum = Math.addExact(sum, factor);
            long partner = value / factor;
            if (partner != factor) sum = Math.addExact(sum, partner);
        }
        return sum;
    }

    /** Returns prime factors and exponents in ascending factor order. */
    public static Map<Long, Integer> primeFactorization(long value) {
        requirePositive(value, "value");
        Map<Long, Integer> factors = new LinkedHashMap<>();
        long remaining = value;

        while (remaining % 2 == 0) {
            factors.merge(2L, 1, Integer::sum);
            remaining /= 2;
        }
        for (long factor = 3;
                factor <= remaining / factor;
                factor += 2) {
            while (remaining % factor == 0) {
                factors.merge(factor, 1, Integer::sum);
                remaining /= factor;
            }
        }
        if (remaining > 1) factors.merge(remaining, 1, Integer::sum);
        return Collections.unmodifiableMap(new LinkedHashMap<>(factors));
    }

    /** Returns every prime not greater than limit using Eratosthenes' sieve. */
    public static List<Integer> sievePrimes(int limit) {
        if (limit < 0) {
            throw new IllegalArgumentException("limit must be nonnegative");
        }
        if (limit < 2) return List.of();

        boolean[] composite = new boolean[limit + 1];
        for (int prime = 2; prime <= limit / prime; prime++) {
            if (composite[prime]) continue;
            for (int multiple = prime * prime; multiple <= limit; multiple += prime) {
                composite[multiple] = true;
            }
        }
        List<Integer> primes = new ArrayList<>();
        for (int value = 2; value <= limit; value++) {
            if (!composite[value]) primes.add(value);
        }
        return List.copyOf(primes);
    }

    /**
     * Returns the nonnegative GCD as long. Throws when the magnitude is 2^63,
     * which cannot be represented by a nonnegative long.
     */
    public static long gcd(long left, long right) {
        return gcdMagnitude(left, right).longValueExact();
    }

    /** Euclidean GCD that represents every long input magnitude exactly. */
    public static BigInteger gcdMagnitude(long left, long right) {
        BigInteger a = BigInteger.valueOf(left).abs();
        BigInteger b = BigInteger.valueOf(right).abs();
        while (!b.equals(BigInteger.ZERO)) {
            BigInteger remainder = a.mod(b);
            a = b;
            b = remainder;
        }
        return a;
    }

    /** Returns a nonnegative LCM as long or throws when it does not fit. */
    public static long lcm(long left, long right) {
        if (left == 0 || right == 0) return 0;
        long divisor = gcd(left, right);
        long product = Math.multiplyExact(left / divisor, right);
        return Math.absExact(product);
    }

    /** Returns the exact nonnegative LCM magnitude for every long input pair. */
    public static BigInteger lcmMagnitude(long left, long right) {
        if (left == 0 || right == 0) return BigInteger.ZERO;
        BigInteger a = BigInteger.valueOf(left);
        BigInteger b = BigInteger.valueOf(right);
        return a.divide(a.gcd(b)).multiply(b).abs();
    }

    /** Returns the GCD of a nonempty array under the pairwise GCD contract. */
    public static long gcdOfArray(long[] values) {
        Objects.requireNonNull(values, "values");
        if (values.length == 0) {
            throw new IllegalArgumentException("values must be nonempty");
        }
        long result = 0;
        for (long value : values) result = gcd(result, value);
        return result;
    }

    /** Returns the exact-in-long LCM of a nonempty array; any zero yields zero. */
    public static long lcmOfArray(long[] values) {
        Objects.requireNonNull(values, "values");
        if (values.length == 0) {
            throw new IllegalArgumentException("values must be nonempty");
        }
        long result = 1;
        for (long value : values) result = lcm(result, value);
        return result;
    }

    // ---------------------------------------------------------------------
    // Powers, roots, and exact arithmetic
    // ---------------------------------------------------------------------

    /** Returns floor(sqrt(value)) for every nonnegative long. */
    public static long integerSquareRoot(long value) {
        if (value < 0) {
            throw new IllegalArgumentException("value must be nonnegative");
        }
        long low = 0;
        long high = Math.min(value, MAX_LONG_SQUARE_ROOT);
        long answer = 0;

        while (low <= high) {
            long mid = low + (high - low) / 2;
            if (mid == 0 || mid <= value / mid) {
                answer = mid;
                low = mid + 1;
            } else {
                high = mid - 1;
            }
        }
        return answer;
    }

    public static boolean isPerfectSquare(long value) {
        if (value < 0) return false;
        long root = integerSquareRoot(value);
        return root * root == value;
    }

    public static boolean isPowerOfTwo(long value) {
        return value > 0 && (value & (value - 1)) == 0;
    }

    /** Returns the unsigned binary length of a nonnegative long; zero uses one bit. */
    public static int countBits(long value) {
        if (value < 0) {
            throw new IllegalArgumentException("value must be nonnegative");
        }
        return value == 0 ? 1 : Long.SIZE - Long.numberOfLeadingZeros(value);
    }

    /** Counts factors of five in n!, which equals the trailing-zero count. */
    public static long trailingZerosInFactorial(int value) {
        if (value < 0) {
            throw new IllegalArgumentException("value must be nonnegative");
        }
        long zeros = 0;
        for (long divisor = 5; divisor <= value; divisor *= 5) {
            zeros += value / divisor;
            if (divisor > value / 5) break;
        }
        return zeros;
    }

    /** Returns the decimal digit count of n! without constructing the factorial. */
    public static long digitsInFactorial(int value) {
        if (value < 0) {
            throw new IllegalArgumentException("value must be nonnegative");
        }
        if (value < 2) return 1;
        double logarithm = 0.0;
        for (int factor = 2; factor <= value; factor++) {
            logarithm += Math.log10(factor);
        }
        return (long) Math.floor(logarithm) + 1;
    }

    /** Fast exponentiation with exact long multiplication and exponent >= 0. */
    public static long fastPowerExact(long base, long exponent) {
        if (exponent < 0) {
            throw new IllegalArgumentException("exponent must be nonnegative");
        }
        long result = 1;
        long factor = base;
        long remaining = exponent;
        while (remaining > 0) {
            if ((remaining & 1L) != 0) {
                result = Math.multiplyExact(result, factor);
            }
            remaining >>>= 1;
            if (remaining > 0) {
                factor = Math.multiplyExact(factor, factor);
            }
        }
        return result;
    }

    public static long safeMultiply(long left, long right) {
        return Math.multiplyExact(left, right);
    }

    public static long safeAdd(long left, long right) {
        return Math.addExact(left, right);
    }

    public static long safeSubtract(long left, long right) {
        return Math.subtractExact(left, right);
    }

    public static int safeToInt(long value) {
        return Math.toIntExact(value);
    }

    public static int compareInts(int left, int right) {
        return Integer.compare(left, right);
    }

    /** Safe midpoint for a nonnegative inclusive or half-open int interval. */
    public static int safeIndexMidpoint(int left, int right) {
        if (left < 0 || left > right) {
            throw new IllegalArgumentException("invalid nonnegative interval");
        }
        return left + (right - left) / 2;
    }

    /** Overflow-free floor average for arbitrary ordered signed long endpoints. */
    public static long signedMidpoint(long left, long right) {
        if (left > right) {
            throw new IllegalArgumentException("left must not exceed right");
        }
        return (left & right) + ((left ^ right) >> 1);
    }

    // ---------------------------------------------------------------------
    // Modular arithmetic
    // ---------------------------------------------------------------------

    public static int normalizeModulo(int value, int modulus) {
        if (modulus <= 0) {
            throw new IllegalArgumentException("modulus must be positive");
        }
        return Math.floorMod(value, modulus);
    }

    public static long normalizeModulo(long value, long modulus) {
        requirePositiveModulus(modulus);
        return Math.floorMod(value, modulus);
    }

    /** Overflow-free modular addition for any long operands and positive modulus. */
    public static long addModulo(long left, long right, long modulus) {
        requirePositiveModulus(modulus);
        return addModuloNormalized(
                normalizeModulo(left, modulus),
                normalizeModulo(right, modulus),
                modulus);
    }

    public static long subtractModulo(long left, long right, long modulus) {
        requirePositiveModulus(modulus);
        long a = normalizeModulo(left, modulus);
        long b = normalizeModulo(right, modulus);
        return Math.floorMod(a - b, modulus);
    }

    /**
     * Overflow-free modular multiplication using repeated doubling. Runs in
     * O(log modulus) time and O(1) space.
     */
    public static long multiplyModulo(long left, long right, long modulus) {
        requirePositiveModulus(modulus);
        long a = normalizeModulo(left, modulus);
        long b = normalizeModulo(right, modulus);
        long result = 0;

        while (b > 0) {
            if ((b & 1L) != 0) {
                result = addModuloNormalized(result, a, modulus);
            }
            b >>>= 1;
            if (b > 0) a = addModuloNormalized(a, a, modulus);
        }
        return result;
    }

    public static long powerModulo(long base, long exponent, long modulus) {
        if (exponent < 0) {
            throw new IllegalArgumentException("exponent must be nonnegative");
        }
        requirePositiveModulus(modulus);
        long result = normalizeModulo(1, modulus);
        long factor = normalizeModulo(base, modulus);
        long remaining = exponent;

        while (remaining > 0) {
            if ((remaining & 1L) != 0) {
                result = multiplyModulo(result, factor, modulus);
            }
            remaining >>>= 1;
            if (remaining > 0) {
                factor = multiplyModulo(factor, factor, modulus);
            }
        }
        return result;
    }

    /**
     * Returns the modular inverse in [0, modulus), using extended Euclid with
     * arbitrary-precision coefficients. The inverse exists only when gcd is 1.
     */
    public static long modularInverse(long value, long modulus) {
        if (modulus <= 1) {
            throw new IllegalArgumentException("modulus must exceed one");
        }
        BigInteger m = BigInteger.valueOf(modulus);
        BigInteger a = BigInteger.valueOf(value).mod(m);
        ExtendedGcd result = extendedGcd(a, m);
        if (!result.gcd().equals(BigInteger.ONE)) {
            throw new ArithmeticException("modular inverse does not exist");
        }
        return result.x().mod(m).longValueExact();
    }

    private static ExtendedGcd extendedGcd(BigInteger left, BigInteger right) {
        BigInteger oldR = left;
        BigInteger remainder = right;
        BigInteger oldS = BigInteger.ONE;
        BigInteger coefficient = BigInteger.ZERO;
        while (!remainder.equals(BigInteger.ZERO)) {
            BigInteger quotient = oldR.divide(remainder);
            BigInteger nextRemainder = oldR.subtract(quotient.multiply(remainder));
            oldR = remainder;
            remainder = nextRemainder;
            BigInteger nextCoefficient = oldS.subtract(quotient.multiply(coefficient));
            oldS = coefficient;
            coefficient = nextCoefficient;
        }
        return new ExtendedGcd(oldR, oldS);
    }

    private record ExtendedGcd(BigInteger gcd, BigInteger x) {}

    /** Adds normalized values in [0, modulus) without overflowing. */
    private static long addModuloNormalized(
            long left, long right, long modulus) {
        if (left >= modulus - right) {
            return left - (modulus - right);
        }
        return left + right;
    }

    private static void requirePositiveModulus(long modulus) {
        if (modulus <= 0) {
            throw new IllegalArgumentException("modulus must be positive");
        }
    }

    private static void requirePositive(long value, String name) {
        if (value <= 0) {
            throw new IllegalArgumentException(name + " must be positive");
        }
    }
}
