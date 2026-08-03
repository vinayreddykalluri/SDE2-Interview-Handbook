import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.math.BigInteger;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import java.util.OptionalInt;

/** Dependency-free boundary and behavior tests for NumberSystemsAlgorithms. */
public final class NumberSystemsAlgorithmsTest {
    private static int assertions;

    private NumberSystemsAlgorithmsTest() {}

    public static void main(String[] args) {
        testDecimalDigits();
        testBaseConversion();
        testLargeDecimalStrings();
        testFactorsAndPrimes();
        testPowersRootsAndExactArithmetic();
        testModularArithmetic();
        System.out.println(
                "All NumberSystemsAlgorithms tests passed ("
                        + assertions
                        + " assertions)");
    }

    private static void testDecimalDigits() {
        assertEquals(1, NumberSystemsAlgorithms.countDigits(0), "zero digit count");
        assertEquals(3, NumberSystemsAlgorithms.countDigits(-123), "negative digit count");
        assertEquals(19, NumberSystemsAlgorithms.countDigits(Long.MIN_VALUE),
                "Long.MIN_VALUE digit count");
        assertEquals(0, NumberSystemsAlgorithms.sumDigits(0), "zero digit sum");
        assertEquals(15, NumberSystemsAlgorithms.sumDigits(-12345), "negative digit sum");
        assertEquals(89, NumberSystemsAlgorithms.sumDigits(Long.MIN_VALUE),
                "Long.MIN_VALUE digit sum");
        assertEquals(0, NumberSystemsAlgorithms.productDigits(105), "embedded zero product");
        assertEquals(729, NumberSystemsAlgorithms.productDigits(-999), "negative digit product");
        assertEquals(0, NumberSystemsAlgorithms.minimumDigit(0), "minimum digit of zero");
        assertEquals(0, NumberSystemsAlgorithms.minimumDigit(-91_028),
                "minimum digit ignores sign");
        assertEquals(9, NumberSystemsAlgorithms.maximumDigit(909),
                "maximum digit");
        assertEquals(3, NumberSystemsAlgorithms.countDigitOccurrences(-10_010, 0),
                "digit occurrence count");
        expectThrows(IllegalArgumentException.class,
                () -> NumberSystemsAlgorithms.countDigitOccurrences(123, 10),
                "invalid target digit");

        assertEquals(OptionalInt.of(0), NumberSystemsAlgorithms.reverseInt(0),
                "reverse zero");
        assertEquals(OptionalInt.of(-3021), NumberSystemsAlgorithms.reverseInt(-1203),
                "reverse negative");
        assertEquals(OptionalInt.empty(), NumberSystemsAlgorithms.reverseInt(1_534_236_469),
                "reverse overflow");
        assertEquals(OptionalInt.empty(), NumberSystemsAlgorithms.reverseInt(Integer.MIN_VALUE),
                "reverse Integer.MIN_VALUE overflow");
        assertEquals(OptionalInt.of(-3021), NumberSystemsAlgorithms.reverseIntStrict(-1203),
                "strict reverse negative");
        assertEquals(OptionalInt.of(1_463_847_412),
                NumberSystemsAlgorithms.reverseIntStrict(2_147_483_641),
                "strict reverse positive boundary");
        assertEquals(OptionalInt.empty(),
                NumberSystemsAlgorithms.reverseIntStrict(1_534_236_469),
                "strict reverse overflow");

        assertTrue(NumberSystemsAlgorithms.isPalindromeNumber(0), "zero palindrome");
        assertTrue(NumberSystemsAlgorithms.isPalindromeNumber(123_454_321),
                "odd palindrome");
        assertTrue(NumberSystemsAlgorithms.isPalindromeNumber(1_221), "even palindrome");
        assertFalse(NumberSystemsAlgorithms.isPalindromeNumber(-121),
                "negative is not palindrome");
        assertFalse(NumberSystemsAlgorithms.isPalindromeNumber(10),
                "trailing zero is not palindrome");

        for (int value : new int[] {0, 1, 153, 370, 371, 407, 9_474}) {
            assertTrue(NumberSystemsAlgorithms.isArmstrongNumber(value),
                    "Armstrong number " + value);
        }
        assertFalse(NumberSystemsAlgorithms.isArmstrongNumber(-153),
                "negative Armstrong input");
        assertFalse(NumberSystemsAlgorithms.isArmstrongNumber(9_475),
                "non-Armstrong value");

        for (int value : new int[] {1, 2, 145, 40_585}) {
            assertTrue(NumberSystemsAlgorithms.isStrongNumber(value),
                    "strong number " + value);
        }
        assertFalse(NumberSystemsAlgorithms.isStrongNumber(0), "zero is not strong");
        assertFalse(NumberSystemsAlgorithms.isStrongNumber(123), "non-strong value");
        assertFalse(NumberSystemsAlgorithms.isStrongNumber(-145), "negative strong input");
        assertEquals(1, NumberSystemsAlgorithms.factorialExact(0), "zero factorial");
        assertEquals(3_628_800, NumberSystemsAlgorithms.factorialExact(10),
                "ten factorial");
        assertEquals(2_432_902_008_176_640_000L,
                NumberSystemsAlgorithms.factorialExact(20), "largest long factorial");
        expectThrows(IllegalArgumentException.class,
                () -> NumberSystemsAlgorithms.factorialExact(21),
                "factorial outside long range");
    }

    private static void testBaseConversion() {
        assertEquals(13, NumberSystemsAlgorithms.binaryStringToLong("001101"),
                "binary with leading zeros");
        assertEquals(Long.MAX_VALUE,
                NumberSystemsAlgorithms.binaryStringToLong("1".repeat(63)),
                "maximum positive binary long");
        assertEquals(Long.MIN_VALUE,
                NumberSystemsAlgorithms.binaryStringToLong("-1" + "0".repeat(63)),
                "minimum binary long");
        expectThrows(ArithmeticException.class,
                () -> NumberSystemsAlgorithms.binaryStringToLong("1".repeat(64)),
                "binary overflow");
        expectThrows(IllegalArgumentException.class,
                () -> NumberSystemsAlgorithms.binaryStringToLong("10201"),
                "invalid binary digit");

        long[] values = {
                Long.MIN_VALUE, -1_000_000_000_000L, -255, -1, 0, 1, 255,
                1_000_000_000_000L, Long.MAX_VALUE
        };
        for (int base = 2; base <= 36; base++) {
            for (long value : values) {
                String encoded = NumberSystemsAlgorithms.longToBase(value, base);
                assertEquals(Long.toString(value, base), encoded,
                        "format base " + base + " value " + value);
                assertEquals(value, NumberSystemsAlgorithms.baseToLong(encoded, base),
                        "round trip base " + base + " value " + value);
            }
        }

        assertEquals("-1000000000000000000000000000000000000000000000000000000000000000",
                NumberSystemsAlgorithms.decimalToBinary(Long.MIN_VALUE),
                "decimal to signed binary");
        assertEquals(255, NumberSystemsAlgorithms.hexadecimalToLong("+00fF"),
                "hexadecimal parsing");
        assertEquals("-ff", NumberSystemsAlgorithms.decimalToHexadecimal(-255),
                "hexadecimal formatting");
        assertEquals("1111111111111111111111111111111111111111111111111111111111111111",
                NumberSystemsAlgorithms.convertArbitraryPrecision(
                        "ffffffffffffffff", 16, 2),
                "arbitrary precision conversion");
        assertTrue(NumberSystemsAlgorithms.isValidNumberInBase("-7f", 16),
                "valid signed hexadecimal");
        assertTrue(NumberSystemsAlgorithms.isValidNumberInBase("000101", 2),
                "valid binary with leading zeros");
        assertFalse(NumberSystemsAlgorithms.isValidNumberInBase("102", 2),
                "invalid binary digit");
        assertFalse(NumberSystemsAlgorithms.isValidNumberInBase("+", 10),
                "sign is not a number");

        expectThrows(IllegalArgumentException.class,
                () -> NumberSystemsAlgorithms.baseToLong("10", 1), "base below range");
        expectThrows(IllegalArgumentException.class,
                () -> NumberSystemsAlgorithms.longToBase(10, 37), "base above range");
        expectThrows(IllegalArgumentException.class,
                () -> NumberSystemsAlgorithms.baseToLong("+", 10), "sign without digits");
        expectThrows(IllegalArgumentException.class,
                () -> NumberSystemsAlgorithms.baseToLong(" 10", 10), "whitespace rejection");
        expectThrows(IllegalArgumentException.class,
                () -> NumberSystemsAlgorithms.baseToLong("19", 8), "digit outside radix");
        expectThrows(ArithmeticException.class,
                () -> NumberSystemsAlgorithms.baseToLong("9223372036854775808", 10),
                "positive long overflow");
        expectThrows(ArithmeticException.class,
                () -> NumberSystemsAlgorithms.baseToLong("-9223372036854775809", 10),
                "negative long overflow");
    }

    private static void testLargeDecimalStrings() {
        String huge = "12345678901234567890123456789012345678901234567890";
        long modulus = Long.MAX_VALUE;
        long expected = new BigInteger(huge)
                .mod(BigInteger.valueOf(modulus))
                .longValueExact();
        assertEquals(expected, NumberSystemsAlgorithms.largeNumberModulo(huge, modulus),
                "huge decimal modulo");
        assertEquals(2, NumberSystemsAlgorithms.largeNumberModulo("-10", 3),
                "negative mathematical modulo");
        assertEquals(0, NumberSystemsAlgorithms.largeNumberModulo("+0000", 17),
                "signed zero modulo");

        assertTrue(NumberSystemsAlgorithms.isDivisibleBy9("-00000018"),
                "large signed divisibility by 9");
        assertFalse(NumberSystemsAlgorithms.isDivisibleBy9("100000000000000000000000000000"),
                "large non-divisibility by 9");
        assertTrue(NumberSystemsAlgorithms.isDivisibleBy11("121"),
                "divisibility by 11");
        assertTrue(NumberSystemsAlgorithms.isDivisibleBy11("-001001"),
                "signed divisibility by 11");
        assertFalse(NumberSystemsAlgorithms.isDivisibleBy11("12345"),
                "non-divisibility by 11");

        assertEquals("1000", NumberSystemsAlgorithms.addNumericStrings("999", "1"),
                "carry across all digits");
        assertEquals("-998", NumberSystemsAlgorithms.addNumericStrings("-999", "1"),
                "opposite signs negative result");
        assertEquals("-1", NumberSystemsAlgorithms.addNumericStrings("999", "-1000"),
                "opposite signs second magnitude larger");
        assertEquals("0", NumberSystemsAlgorithms.addNumericStrings("-999", "+0999"),
                "opposite signs cancel");
        assertEquals("20", NumberSystemsAlgorithms.addNumericStrings("+00012", "0008"),
                "leading zeros in addition");
        assertEquals("1000000000000000000000000000000",
                NumberSystemsAlgorithms.addNumericStrings(
                        "999999999999999999999999999999", "1"),
                "arbitrary precision carry");
        assertEquals("999999999999999999999999999999",
                NumberSystemsAlgorithms.subtractNumericStrings(
                        "1000000000000000000000000000000", "1"),
                "arbitrary precision borrow");
        assertEquals("-1001", NumberSystemsAlgorithms.subtractNumericStrings("-999", "2"),
                "signed numeric-string subtraction");
        assertEquals("0", NumberSystemsAlgorithms.subtractNumericStrings("-0", "+000"),
                "numeric-string subtraction zero");
        assertEquals("8999999999999999999999999999991",
                NumberSystemsAlgorithms.multiplyNumericStringByDigit(
                        "999999999999999999999999999999", 9),
                "numeric string times digit");
        assertEquals("-84", NumberSystemsAlgorithms.multiplyNumericStringByDigit("-0012", 7),
                "signed numeric string times digit");
        assertEquals("0", NumberSystemsAlgorithms.multiplyNumericStringByDigit("123", 0),
                "numeric string times zero");

        assertEquals(0, Integer.signum(NumberSystemsAlgorithms.compareNumericStrings(
                "0010", "+10")), "equal normalized strings");
        assertEquals(0, Integer.signum(NumberSystemsAlgorithms.compareNumericStrings(
                "-0", "000")), "signed zero comparison");
        assertEquals(-1, Integer.signum(NumberSystemsAlgorithms.compareNumericStrings(
                "-11", "-2")), "negative magnitude comparison");
        assertEquals(1, Integer.signum(NumberSystemsAlgorithms.compareNumericStrings(
                "100000000000000000000", "99999999999999999999")),
                "large magnitude comparison");
        assertEquals("-12", NumberSystemsAlgorithms.normalizeDecimalString("-00012"),
                "normalize negative decimal");
        assertEquals("0", NumberSystemsAlgorithms.normalizeDecimalString("-000"),
                "normalize signed zero");

        expectThrows(IllegalArgumentException.class,
                () -> NumberSystemsAlgorithms.largeNumberModulo("", 7),
                "empty large number");
        expectThrows(IllegalArgumentException.class,
                () -> NumberSystemsAlgorithms.largeNumberModulo("12x", 7),
                "invalid large number digit");
        expectThrows(IllegalArgumentException.class,
                () -> NumberSystemsAlgorithms.addNumericStrings("+", "1"),
                "sign-only addend");
        expectThrows(IllegalArgumentException.class,
                () -> NumberSystemsAlgorithms.compareNumericStrings("1.0", "1"),
                "non-integer comparison");
    }

    private static void testFactorsAndPrimes() {
        assertFalse(NumberSystemsAlgorithms.isPrime(Long.MIN_VALUE), "negative prime input");
        assertFalse(NumberSystemsAlgorithms.isPrime(0), "zero is not prime");
        assertFalse(NumberSystemsAlgorithms.isPrime(1), "one is not prime");
        assertTrue(NumberSystemsAlgorithms.isPrime(2), "two is prime");
        assertTrue(NumberSystemsAlgorithms.isPrime(3), "three is prime");
        assertFalse(NumberSystemsAlgorithms.isPrime(4), "four is composite");
        assertTrue(NumberSystemsAlgorithms.isPrime(2_147_483_647L),
                "large int prime");
        assertTrue(NumberSystemsAlgorithms.isPerfectNumber(6), "first perfect number");
        assertTrue(NumberSystemsAlgorithms.isPerfectNumber(28), "second perfect number");
        assertTrue(NumberSystemsAlgorithms.isPerfectNumber(496), "third perfect number");
        assertFalse(NumberSystemsAlgorithms.isPerfectNumber(1), "one is not perfect");
        assertFalse(NumberSystemsAlgorithms.isPerfectNumber(12), "abundant is not perfect");

        assertEquals(List.of(1L), NumberSystemsAlgorithms.listFactors(1),
                "factors of one");
        assertEquals(List.of(1L, 2L, 3L, 4L, 6L, 9L, 12L, 18L, 36L),
                NumberSystemsAlgorithms.listFactors(36), "factors of 36");
        assertEquals(9, NumberSystemsAlgorithms.countFactors(36),
                "factor count for square");
        assertEquals(91, NumberSystemsAlgorithms.sumFactors(36),
                "factor sum for square");
        assertEquals(2, NumberSystemsAlgorithms.countFactors(2_147_483_647L),
                "factor count for prime");

        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        try (PrintStream output = new PrintStream(bytes, true, StandardCharsets.UTF_8)) {
            NumberSystemsAlgorithms.printFactors(12, output);
        }
        assertEquals("1 2 3 4 6 12", bytes.toString(StandardCharsets.UTF_8).trim(),
                "print factors");

        assertEquals(Map.of(2L, 3, 3L, 2, 5L, 1),
                NumberSystemsAlgorithms.primeFactorization(360),
                "prime factorization");
        assertEquals(Map.of(), NumberSystemsAlgorithms.primeFactorization(1),
                "factorization of one");
        assertEquals(List.of(2, 3, 5, 7, 11, 13, 17, 19, 23, 29),
                NumberSystemsAlgorithms.sievePrimes(30), "sieve through thirty");
        assertEquals(List.of(), NumberSystemsAlgorithms.sievePrimes(1),
                "sieve below first prime");
        expectThrows(IllegalArgumentException.class,
                () -> NumberSystemsAlgorithms.listFactors(0),
                "factor listing requires positive input");

        assertEquals(6, NumberSystemsAlgorithms.gcd(54, 24), "Euclidean GCD");
        assertEquals(6, NumberSystemsAlgorithms.gcd(-54, 24), "signed GCD");
        assertEquals(5, NumberSystemsAlgorithms.gcd(0, -5), "GCD with zero");
        assertEquals(0, NumberSystemsAlgorithms.gcd(0, 0), "GCD of two zeros");
        assertEquals(BigInteger.ONE.shiftLeft(63),
                NumberSystemsAlgorithms.gcdMagnitude(Long.MIN_VALUE, 0),
                "exact GCD magnitude at 2^63");
        expectThrows(ArithmeticException.class,
                () -> NumberSystemsAlgorithms.gcd(Long.MIN_VALUE, 0),
                "GCD magnitude outside long");

        assertEquals(42, NumberSystemsAlgorithms.lcm(21, 6), "LCM");
        assertEquals(42, NumberSystemsAlgorithms.lcm(-21, 6), "signed LCM");
        assertEquals(0, NumberSystemsAlgorithms.lcm(Long.MIN_VALUE, 0),
                "LCM with zero");
        assertEquals(BigInteger.ONE.shiftLeft(63),
                NumberSystemsAlgorithms.lcmMagnitude(Long.MIN_VALUE, 1),
                "exact LCM magnitude at 2^63");
        expectThrows(ArithmeticException.class,
                () -> NumberSystemsAlgorithms.lcm(Long.MAX_VALUE, 2),
                "LCM multiplication overflow");
        expectThrows(ArithmeticException.class,
                () -> NumberSystemsAlgorithms.lcm(Long.MIN_VALUE, 1),
                "LCM absolute-value overflow");
        assertEquals(6, NumberSystemsAlgorithms.gcdOfArray(new long[] {0, -54, 24, 30}),
                "GCD of array");
        assertEquals(60, NumberSystemsAlgorithms.lcmOfArray(new long[] {3, 4, 5}),
                "LCM of array");
        assertEquals(0, NumberSystemsAlgorithms.lcmOfArray(new long[] {3, 0, 5}),
                "LCM array containing zero");
        expectThrows(IllegalArgumentException.class,
                () -> NumberSystemsAlgorithms.gcdOfArray(new long[0]),
                "empty GCD array");
    }

    private static void testPowersRootsAndExactArithmetic() {
        assertEquals(0, NumberSystemsAlgorithms.integerSquareRoot(0), "sqrt zero");
        assertEquals(1, NumberSystemsAlgorithms.integerSquareRoot(1), "sqrt one");
        assertEquals(3, NumberSystemsAlgorithms.integerSquareRoot(15), "floor sqrt");
        assertEquals(4, NumberSystemsAlgorithms.integerSquareRoot(16), "exact sqrt");
        assertEquals(3_037_000_499L,
                NumberSystemsAlgorithms.integerSquareRoot(Long.MAX_VALUE),
                "sqrt Long.MAX_VALUE");
        long largestSquare = 3_037_000_499L * 3_037_000_499L;
        assertTrue(NumberSystemsAlgorithms.isPerfectSquare(largestSquare),
                "large perfect square");
        assertFalse(NumberSystemsAlgorithms.isPerfectSquare(Long.MAX_VALUE),
                "Long.MAX_VALUE is not square");
        assertFalse(NumberSystemsAlgorithms.isPerfectSquare(-1),
                "negative is not square");
        expectThrows(IllegalArgumentException.class,
                () -> NumberSystemsAlgorithms.integerSquareRoot(-1),
                "negative integer square root");

        assertTrue(NumberSystemsAlgorithms.isPowerOfTwo(1), "one is power of two");
        assertTrue(NumberSystemsAlgorithms.isPowerOfTwo(1L << 62),
                "largest positive long power of two");
        assertFalse(NumberSystemsAlgorithms.isPowerOfTwo(0), "zero is not power of two");
        assertFalse(NumberSystemsAlgorithms.isPowerOfTwo(Long.MIN_VALUE),
                "negative bit pattern is not positive power of two");
        assertEquals(1, NumberSystemsAlgorithms.countBits(0), "zero binary length");
        assertEquals(1, NumberSystemsAlgorithms.countBits(1), "one binary length");
        assertEquals(10, NumberSystemsAlgorithms.countBits(1_023),
                "binary length below power of two");
        assertEquals(11, NumberSystemsAlgorithms.countBits(1_024),
                "binary length at power of two");
        expectThrows(IllegalArgumentException.class,
                () -> NumberSystemsAlgorithms.countBits(-1), "negative binary length");

        assertEquals(0, NumberSystemsAlgorithms.trailingZerosInFactorial(4),
                "factorial without trailing zero");
        assertEquals(24, NumberSystemsAlgorithms.trailingZerosInFactorial(100),
                "trailing zeros in one hundred factorial");
        assertEquals(1, NumberSystemsAlgorithms.digitsInFactorial(0),
                "digits in zero factorial");
        assertEquals(7, NumberSystemsAlgorithms.digitsInFactorial(10),
                "digits in ten factorial");
        assertEquals(158, NumberSystemsAlgorithms.digitsInFactorial(100),
                "digits in one hundred factorial");

        assertEquals(1, NumberSystemsAlgorithms.fastPowerExact(0, 0),
                "zero exponent convention");
        assertEquals(1_024, NumberSystemsAlgorithms.fastPowerExact(2, 10),
                "fast exponentiation");
        assertEquals(-8, NumberSystemsAlgorithms.fastPowerExact(-2, 3),
                "negative base exponentiation");
        expectThrows(ArithmeticException.class,
                () -> NumberSystemsAlgorithms.fastPowerExact(2, 63),
                "power overflow");
        expectThrows(IllegalArgumentException.class,
                () -> NumberSystemsAlgorithms.fastPowerExact(2, -1),
                "negative exponent");

        assertEquals(Long.MAX_VALUE,
                NumberSystemsAlgorithms.safeMultiply(Long.MAX_VALUE, 1),
                "safe multiplication boundary");
        expectThrows(ArithmeticException.class,
                () -> NumberSystemsAlgorithms.safeMultiply(Long.MAX_VALUE, 2),
                "safe multiplication overflow");
        expectThrows(ArithmeticException.class,
                () -> NumberSystemsAlgorithms.safeAdd(Long.MAX_VALUE, 1),
                "safe addition overflow");
        expectThrows(ArithmeticException.class,
                () -> NumberSystemsAlgorithms.safeSubtract(Long.MIN_VALUE, 1),
                "safe subtraction overflow");
        assertEquals(Integer.MAX_VALUE,
                NumberSystemsAlgorithms.safeToInt(Integer.MAX_VALUE),
                "safe long-to-int boundary");
        expectThrows(ArithmeticException.class,
                () -> NumberSystemsAlgorithms.safeToInt((long) Integer.MAX_VALUE + 1),
                "safe long-to-int overflow");
        assertEquals(-1,
                NumberSystemsAlgorithms.compareInts(Integer.MIN_VALUE, Integer.MAX_VALUE),
                "comparison without subtraction overflow");

        assertEquals(1_073_741_823,
                NumberSystemsAlgorithms.safeIndexMidpoint(0, Integer.MAX_VALUE),
                "index midpoint");
        assertEquals(-1,
                NumberSystemsAlgorithms.signedMidpoint(Long.MIN_VALUE, Long.MAX_VALUE),
                "full signed midpoint");
        assertEquals(-1, NumberSystemsAlgorithms.signedMidpoint(-5, 4),
                "signed midpoint floors exact average");
        expectThrows(IllegalArgumentException.class,
                () -> NumberSystemsAlgorithms.safeIndexMidpoint(-1, 10),
                "negative index midpoint");
        expectThrows(IllegalArgumentException.class,
                () -> NumberSystemsAlgorithms.signedMidpoint(1, 0),
                "reversed midpoint endpoints");
    }

    private static void testModularArithmetic() {
        assertEquals(3, NumberSystemsAlgorithms.normalizeModulo(-7, 5),
                "normalized int modulo");
        assertEquals(Integer.MAX_VALUE - 1L,
                NumberSystemsAlgorithms.normalizeModulo(
                        Integer.MIN_VALUE, Integer.MAX_VALUE),
                "int minimum normalization");
        assertEquals(2, NumberSystemsAlgorithms.normalizeModulo(-10L, 3L),
                "normalized long modulo");
        assertEquals(1, NumberSystemsAlgorithms.addModulo(-1, 2, Long.MAX_VALUE),
                "signed modular addition");
        assertEquals(Long.MAX_VALUE - 2,
                NumberSystemsAlgorithms.addModulo(
                        Long.MAX_VALUE - 1, Long.MAX_VALUE - 1, Long.MAX_VALUE),
                "overflow-free modular addition");
        assertEquals(4, NumberSystemsAlgorithms.subtractModulo(1, 2, 5),
                "modular subtraction");

        long left = Long.MIN_VALUE;
        long right = Long.MAX_VALUE - 123;
        long modulus = Long.MAX_VALUE - 17;
        BigInteger bigModulus = BigInteger.valueOf(modulus);
        long expectedProduct = BigInteger.valueOf(left)
                .mod(bigModulus)
                .multiply(BigInteger.valueOf(right).mod(bigModulus))
                .mod(bigModulus)
                .longValueExact();
        assertEquals(expectedProduct,
                NumberSystemsAlgorithms.multiplyModulo(left, right, modulus),
                "overflow-free modular multiplication");

        long exponent = 12_345;
        long expectedPower = BigInteger.valueOf(left)
                .mod(bigModulus)
                .modPow(BigInteger.valueOf(exponent), bigModulus)
                .longValueExact();
        assertEquals(expectedPower,
                NumberSystemsAlgorithms.powerModulo(left, exponent, modulus),
                "modular fast exponentiation");
        assertEquals(0, NumberSystemsAlgorithms.powerModulo(999, 0, 1),
                "modulus one");
        assertEquals(4, NumberSystemsAlgorithms.modularInverse(3, 11),
                "modular inverse");
        assertEquals(7, NumberSystemsAlgorithms.modularInverse(-3, 11),
                "negative modular inverse");
        assertEquals(1, NumberSystemsAlgorithms.modularInverse(1, Long.MAX_VALUE),
                "large-modulus inverse");
        expectThrows(ArithmeticException.class,
                () -> NumberSystemsAlgorithms.modularInverse(6, 9),
                "non-coprime modular inverse");

        expectThrows(IllegalArgumentException.class,
                () -> NumberSystemsAlgorithms.normalizeModulo(1, 0),
                "zero modulus");
        expectThrows(IllegalArgumentException.class,
                () -> NumberSystemsAlgorithms.multiplyModulo(1, 2, -3),
                "negative modulus");
        expectThrows(IllegalArgumentException.class,
                () -> NumberSystemsAlgorithms.powerModulo(2, -1, 5),
                "negative modular exponent");
    }

    private static void assertTrue(boolean condition, String message) {
        assertions++;
        if (!condition) throw new AssertionError(message);
    }

    private static void assertFalse(boolean condition, String message) {
        assertTrue(!condition, message);
    }

    private static void assertEquals(long expected, long actual, String message) {
        assertions++;
        if (expected != actual) {
            throw new AssertionError(
                    message + ": expected " + expected + ", got " + actual);
        }
    }

    private static void assertEquals(Object expected, Object actual, String message) {
        assertions++;
        if (!expected.equals(actual)) {
            throw new AssertionError(
                    message + ": expected " + expected + ", got " + actual);
        }
    }

    private static void expectThrows(
            Class<? extends Throwable> expected,
            Runnable operation,
            String message) {
        assertions++;
        try {
            operation.run();
        } catch (Throwable thrown) {
            if (expected.isInstance(thrown)) return;
            throw new AssertionError(
                    message + ": expected " + expected.getSimpleName()
                            + ", got " + thrown,
                    thrown);
        }
        throw new AssertionError(
                message + ": expected " + expected.getSimpleName());
    }
}
