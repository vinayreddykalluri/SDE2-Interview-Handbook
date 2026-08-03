import java.util.Random;

public final class BitContractChecks {
    private BitContractChecks() {
    }

    static int manualBitCount(int value) {
        int count = 0;
        while (value != 0) {
            value &= value - 1;
            count++;
        }
        return count;
    }

    static int manualRotateRight(int value, int requestedDistance) {
        int distance = requestedDistance & 31;
        return (value >>> distance) | (value << ((32 - distance) & 31));
    }

    static int manualBitLength(int value) {
        if (value < 0) {
            throw new IllegalArgumentException("bit length requires a nonnegative value");
        }
        int length = 0;
        while (value != 0) {
            value >>>= 1;
            length++;
        }
        return length;
    }

    private static void require(boolean condition, String message) {
        if (!condition) {
            throw new AssertionError(message);
        }
    }

    private static void verify(int value, int distance) {
        require(manualBitCount(value) == Integer.bitCount(value),
                "bit-count mismatch for " + value);
        require(manualRotateRight(value, distance) == Integer.rotateRight(value, distance),
                "rotation mismatch for value=" + value + ", distance=" + distance);
        if (value >= 0) {
            int expectedLength = value == 0 ? 0 : Integer.SIZE - Integer.numberOfLeadingZeros(value);
            require(manualBitLength(value) == expectedLength,
                    "bit-length mismatch for " + value);
        }
    }

    public static void main(String[] args) {
        int[] boundaries = {
                0, 1, 2, 3, 7, 8, 31, 32,
                Integer.MAX_VALUE, Integer.MIN_VALUE, -1
        };
        int[] distances = {-65, -32, -1, 0, 1, 31, 32, 33, 64, 97};
        int checks = 0;
        for (int value : boundaries) {
            for (int distance : distances) {
                verify(value, distance);
                checks++;
            }
        }

        Random random = new Random(20260802L);
        for (int index = 0; index < 10_000; index++) {
            verify(random.nextInt(), random.nextInt());
            checks++;
        }

        require((1 << 32) == 1, "int shift distance must be masked to five bits");
        require((1L << 64) == 1L, "long shift distance must be masked to six bits");
        System.out.println("PASS " + checks + " manual/JDK bit contract checks");
    }
}
