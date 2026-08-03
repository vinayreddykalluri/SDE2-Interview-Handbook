import java.util.Arrays;

public final class LoopInvariantChecks {
    private LoopInvariantChecks() {
    }

    static int compactNonZero(int[] values) {
        int write = 0;
        for (int read = 0; read < values.length; read++) {
            if (values[read] != 0) {
                values[write++] = values[read];
            }
        }
        return write;
    }

    static int normalizeIndex(int index, int length) {
        if (length <= 0) {
            throw new IllegalArgumentException("length must be positive");
        }
        int remainder = index % length;
        return remainder < 0 ? remainder + length : remainder;
    }

    static long flatten(int row, int column, int columns) {
        if (row < 0 || column < 0 || columns <= 0 || column >= columns) {
            throw new IllegalArgumentException("invalid rectangular-grid coordinate");
        }
        return Math.addExact(Math.multiplyExact((long) row, columns), column);
    }

    static int longestWindowAtMost(int[] values, long limit) {
        if (limit < 0) {
            return 0;
        }
        int left = 0;
        int best = 0;
        long sum = 0;
        for (int right = 0; right < values.length; right++) {
            if (values[right] < 0) {
                throw new IllegalArgumentException("window requires nonnegative values");
            }
            sum = Math.addExact(sum, values[right]);
            while (sum > limit) {
                sum -= values[left++];
            }
            best = Math.max(best, right - left + 1);
        }
        return best;
    }

    private static void require(boolean condition, String message) {
        if (!condition) {
            throw new AssertionError(message);
        }
    }

    private static void expectIllegalArgument(Runnable action) {
        try {
            action.run();
            throw new AssertionError("expected IllegalArgumentException");
        } catch (IllegalArgumentException expected) {
            // Expected failure contract.
        }
    }

    public static void main(String[] args) {
        int[] values = {4, 0, 2, 0, 7};
        int length = compactNonZero(values);
        require(length == 3, "wrong compacted length");
        require(Arrays.equals(Arrays.copyOf(values, length), new int[] {4, 2, 7}),
                "compaction must preserve order");

        for (int index = -20; index <= 20; index++) {
            require(normalizeIndex(index, 7) == Math.floorMod(index, 7),
                    "normalization mismatch at " + index);
        }
        require(flatten(50_000, 40_000, 50_000) == 2_500_040_000L,
                "flattening must widen before multiplication");
        require(longestWindowAtMost(new int[] {2, 1, 3, 1}, 5) == 3,
                "wrong nonnegative window length");
        require(longestWindowAtMost(new int[0], 5) == 0, "empty window must be zero");

        expectIllegalArgument(() -> normalizeIndex(1, 0));
        expectIllegalArgument(() -> longestWindowAtMost(new int[] {1, -1, 2}, 3));

        System.out.println("PASS loop invariant and boundary checks");
    }
}
