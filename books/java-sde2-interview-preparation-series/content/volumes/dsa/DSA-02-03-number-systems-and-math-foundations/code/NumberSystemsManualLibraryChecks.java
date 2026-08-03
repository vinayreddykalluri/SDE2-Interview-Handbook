import java.math.BigInteger;
import java.util.Random;

public final class NumberSystemsManualLibraryChecks {
    private NumberSystemsManualLibraryChecks() {
    }

    static long addExactManual(long left, long right) {
        if (right > 0 && left > Long.MAX_VALUE - right
                || right < 0 && left < Long.MIN_VALUE - right) {
            throw new ArithmeticException("long overflow");
        }
        return left + right;
    }

    static long subtractExactManual(long left, long right) {
        if (right > 0 && left < Long.MIN_VALUE + right
                || right < 0 && left > Long.MAX_VALUE + right) {
            throw new ArithmeticException("long overflow");
        }
        return left - right;
    }

    static long multiplyExactManual(long left, long right) {
        if ((left == -1 && right == Long.MIN_VALUE)
                || (right == -1 && left == Long.MIN_VALUE)) {
            throw new ArithmeticException("long overflow");
        }
        long result = left * right;
        if (left != 0 && result / left != right) {
            throw new ArithmeticException("long overflow");
        }
        return result;
    }

    static int toIntExactManual(long value) {
        if (value < Integer.MIN_VALUE || value > Integer.MAX_VALUE) {
            throw new ArithmeticException("integer overflow");
        }
        return (int) value;
    }

    static long normalizeModuloManual(long value, long modulus) {
        if (modulus <= 0) {
            throw new IllegalArgumentException("modulus must be positive");
        }
        long remainder = value % modulus;
        return remainder < 0 ? remainder + modulus : remainder;
    }

    static int bitLengthManual(long value) {
        if (value < 0) {
            throw new IllegalArgumentException("value must be nonnegative");
        }
        int length = 1;
        while ((value >>>= 1) != 0) {
            length++;
        }
        return length;
    }

    static long gcdManual(long left, long right) {
        long first = left > 0 ? -left : left;
        long second = right > 0 ? -right : right;
        while (second != 0) {
            long remainder = first % second;
            first = second;
            second = remainder;
        }
        if (first == Long.MIN_VALUE) {
            throw new ArithmeticException("GCD magnitude does not fit long");
        }
        return -first;
    }

    static int modularInverseManual(int value, int modulus) {
        if (modulus <= 1) {
            throw new IllegalArgumentException("modulus must exceed one");
        }
        long oldR = normalizeModuloManual(value, modulus);
        long remainder = modulus;
        long oldCoefficient = 1;
        long coefficient = 0;
        while (remainder != 0) {
            long quotient = oldR / remainder;
            long nextRemainder = oldR - quotient * remainder;
            oldR = remainder;
            remainder = nextRemainder;
            long nextCoefficient = oldCoefficient - quotient * coefficient;
            oldCoefficient = coefficient;
            coefficient = nextCoefficient;
        }
        if (oldR != 1) {
            throw new ArithmeticException("modular inverse does not exist");
        }
        return (int) normalizeModuloManual(oldCoefficient, modulus);
    }

    static int factorialDigitCountExact(int value) {
        if (value < 0) {
            throw new IllegalArgumentException("value must be nonnegative");
        }
        int[] digits = {1};
        int length = 1;
        for (int factor = 2; factor <= value; factor++) {
            long carry = 0;
            for (int index = 0; index < length; index++) {
                long product = (long) digits[index] * factor + carry;
                digits[index] = (int) (product % 10);
                carry = product / 10;
            }
            while (carry > 0) {
                if (length == digits.length) {
                    int[] grown = new int[digits.length * 2];
                    System.arraycopy(digits, 0, grown, 0, digits.length);
                    digits = grown;
                }
                digits[length++] = (int) (carry % 10);
                carry /= 10;
            }
        }
        return length;
    }

    private static void require(boolean condition, String message) {
        if (!condition) {
            throw new AssertionError(message);
        }
    }

    private static boolean manualThrows(LongOperation operation, long left, long right) {
        try {
            operation.apply(left, right);
            return false;
        } catch (ArithmeticException expected) {
            return true;
        }
    }

    private static boolean jdkThrows(LongOperation operation, long left, long right) {
        return manualThrows(operation, left, right);
    }

    @FunctionalInterface
    private interface LongOperation {
        long apply(long left, long right);
    }

    private static void compareOperation(
            LongOperation manual, LongOperation jdk, long left, long right, String label) {
        boolean manualFailure = manualThrows(manual, left, right);
        boolean jdkFailure = jdkThrows(jdk, left, right);
        require(manualFailure == jdkFailure, label + " failure mismatch");
        if (!manualFailure) {
            require(manual.apply(left, right) == jdk.apply(left, right), label + " value mismatch");
        }
    }

    public static void main(String[] args) {
        long[] boundaries = {
                Long.MIN_VALUE, Long.MIN_VALUE + 1, -2, -1, 0, 1, 2,
                Long.MAX_VALUE - 1, Long.MAX_VALUE
        };
        for (long left : boundaries) {
            for (long right : boundaries) {
                compareOperation(NumberSystemsManualLibraryChecks::addExactManual,
                        Math::addExact, left, right, "add");
                compareOperation(NumberSystemsManualLibraryChecks::subtractExactManual,
                        Math::subtractExact, left, right, "subtract");
                compareOperation(NumberSystemsManualLibraryChecks::multiplyExactManual,
                        Math::multiplyExact, left, right, "multiply");
            }
        }

        Random random = new Random(20260802L);
        for (int index = 0; index < 20_000; index++) {
            long left = random.nextLong();
            long right = random.nextLong();
            compareOperation(NumberSystemsManualLibraryChecks::addExactManual,
                    Math::addExact, left, right, "random add");
            compareOperation(NumberSystemsManualLibraryChecks::subtractExactManual,
                    Math::subtractExact, left, right, "random subtract");
            compareOperation(NumberSystemsManualLibraryChecks::multiplyExactManual,
                    Math::multiplyExact, left, right, "random multiply");
        }

        for (long value : boundaries) {
            for (long modulus : new long[] {1, 2, 7, 97, Long.MAX_VALUE}) {
                require(normalizeModuloManual(value, modulus) == Math.floorMod(value, modulus),
                        "modulo mismatch");
            }
        }
        for (long value : new long[] {0, 1, 2, 3, 4, 7, 8, 9, Long.MAX_VALUE}) {
            int library = value == 0 ? 1 : Long.SIZE - Long.numberOfLeadingZeros(value);
            require(bitLengthManual(value) == library, "bit-length mismatch");
        }
        for (int value = -200; value <= 200; value++) {
            for (int modulus = 2; modulus <= 200; modulus++) {
                BigInteger normalized = BigInteger.valueOf(value).mod(BigInteger.valueOf(modulus));
                if (normalized.gcd(BigInteger.valueOf(modulus)).equals(BigInteger.ONE)) {
                    int expected = normalized.modInverse(BigInteger.valueOf(modulus)).intValueExact();
                    require(modularInverseManual(value, modulus) == expected,
                            "modular-inverse mismatch");
                }
            }
        }
        for (int value = 0; value <= 500; value++) {
            int expected = factorial(value).toString().length();
            require(factorialDigitCountExact(value) == expected,
                    "factorial digit mismatch at " + value);
        }

        require(gcdManual(-54, 24) == 6, "signed GCD mismatch");
        require(toIntExactManual(Integer.MAX_VALUE) == Integer.MAX_VALUE, "narrowing mismatch");
        require(factorialDigitCountExact(0) == 1, "0! must have one digit");
        System.out.println("PASS manual/JDK number-system contract checks");
    }

    private static BigInteger factorial(int value) {
        BigInteger result = BigInteger.ONE;
        for (int factor = 2; factor <= value; factor++) {
            result = result.multiply(BigInteger.valueOf(factor));
        }
        return result;
    }
}
