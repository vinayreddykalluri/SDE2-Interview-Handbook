import java.util.Arrays;
import java.util.Random;
import java.util.function.LongPredicate;

public final class BinarySearchInterviewChecks {
    private BinarySearchInterviewChecks() {}

    static int lowerBound(int[] sorted, int target) {
        int left = 0;
        int right = sorted.length;
        while (left < right) {
            int middle = left + (right - left) / 2;
            if (sorted[middle] < target) {
                left = middle + 1;
            } else {
                right = middle;
            }
        }
        return left;
    }

    static int upperBound(int[] sorted, int target) {
        int left = 0;
        int right = sorted.length;
        while (left < right) {
            int middle = left + (right - left) / 2;
            if (sorted[middle] <= target) {
                left = middle + 1;
            } else {
                right = middle;
            }
        }
        return left;
    }

    static int[] equalRange(int[] sorted, int target) {
        int first = lowerBound(sorted, target);
        if (first == sorted.length || sorted[first] != target) {
            return new int[] {-1, -1};
        }
        return new int[] {first, upperBound(sorted, target) - 1};
    }

    /** Finds the first true value in a closed domain whose high endpoint is true. */
    static long firstTrue(long low, long high, LongPredicate predicate) {
        if (low > high || !predicate.test(high)) {
            throw new IllegalArgumentException("nonempty domain with true high required");
        }
        while (low < high) {
            long middle = (low & high) + ((low ^ high) >> 1);
            if (predicate.test(middle)) {
                high = middle;
            } else {
                low = middle + 1L;
            }
        }
        return low;
    }

    static long minimumCapacity(int[] weights, int days) {
        if (weights.length == 0 || days <= 0) {
            throw new IllegalArgumentException("invalid input");
        }
        long left = 0L;
        long right = 0L;
        for (int weight : weights) {
            if (weight <= 0) {
                throw new IllegalArgumentException("positive weights required");
            }
            left = Math.max(left, weight);
            right += weight;
        }
        while (left < right) {
            long middle = left + (right - left) / 2L;
            if (canShip(weights, days, middle)) {
                right = middle;
            } else {
                left = middle + 1L;
            }
        }
        return left;
    }

    private static boolean canShip(int[] weights, int days, long capacity) {
        int usedDays = 1;
        long load = 0L;
        for (int weight : weights) {
            if (load + weight > capacity) {
                usedDays++;
                load = 0L;
            }
            load += weight;
        }
        return usedDays <= days;
    }

    static long integerSquareRoot(long value) {
        if (value < 0) {
            throw new IllegalArgumentException("value cannot be negative");
        }
        long left = 0;
        long right = Math.min(value, 3_037_000_499L);
        long answer = 0;
        while (left <= right) {
            long middle = left + (right - left) / 2L;
            if (middle == 0 || middle <= value / middle) {
                answer = middle;
                left = middle + 1L;
            } else {
                right = middle - 1L;
            }
        }
        return answer;
    }

    static int searchRotatedDistinct(int[] values, int target) {
        int left = 0;
        int right = values.length - 1;
        while (left <= right) {
            int middle = left + (right - left) / 2;
            if (values[middle] == target) {
                return middle;
            }
            if (values[left] <= values[middle]) {
                if (values[left] <= target && target < values[middle]) {
                    right = middle - 1;
                } else {
                    left = middle + 1;
                }
            } else if (values[middle] < target && target <= values[right]) {
                left = middle + 1;
            } else {
                right = middle - 1;
            }
        }
        return -1;
    }

    static int minimumEatingSpeed(int[] piles, int hours) {
        if (piles.length == 0 || hours < piles.length) {
            throw new IllegalArgumentException("at least one hour per nonempty pile required");
        }
        int maximum = 0;
        for (int pile : piles) {
            if (pile <= 0) {
                throw new IllegalArgumentException("positive piles required");
            }
            maximum = Math.max(maximum, pile);
        }
        long answer = firstTrue(1, maximum, speed -> canEat(piles, hours, speed));
        return Math.toIntExact(answer);
    }

    private static boolean canEat(int[] piles, int hours, long speed) {
        long used = 0;
        for (int pile : piles) {
            used += (pile + speed - 1L) / speed;
            if (used > hours) {
                return false;
            }
        }
        return true;
    }

    static boolean searchRowMajorMatrix(int[][] matrix, int target) {
        if (matrix.length == 0) {
            return false;
        }
        int columns = matrix[0].length;
        for (int[] row : matrix) {
            if (row.length != columns) {
                throw new IllegalArgumentException("rectangular matrix required");
            }
        }
        if (columns == 0) {
            return false;
        }
        long left = 0;
        long right = (long) matrix.length * columns;
        while (left < right) {
            long middle = left + (right - left) / 2L;
            int row = (int) (middle / columns);
            int column = (int) (middle % columns);
            if (matrix[row][column] < target) {
                left = middle + 1L;
            } else {
                right = middle;
            }
        }
        if (left == (long) matrix.length * columns) {
            return false;
        }
        return matrix[(int) (left / columns)][(int) (left % columns)] == target;
    }

    private static boolean boundsMatchLinearOracle() {
        Random random = new Random(59L);
        for (int trial = 0; trial < 2_000; trial++) {
            int[] values = new int[random.nextInt(50)];
            for (int index = 0; index < values.length; index++) {
                values[index] = random.nextInt(31) - 15;
            }
            Arrays.sort(values);
            int target = random.nextInt(41) - 20;
            int expectedLower = 0;
            while (expectedLower < values.length && values[expectedLower] < target) {
                expectedLower++;
            }
            int expectedUpper = expectedLower;
            while (expectedUpper < values.length && values[expectedUpper] <= target) {
                expectedUpper++;
            }
            if (lowerBound(values, target) != expectedLower
                    || upperBound(values, target) != expectedUpper) {
                return false;
            }
        }
        return true;
    }

    private static void expectFailure(Runnable action) {
        try {
            action.run();
        } catch (IllegalArgumentException expected) {
            return;
        }
        throw new AssertionError("expected IllegalArgumentException");
    }

    private static void check(boolean condition, String message) {
        if (!condition) {
            throw new AssertionError(message);
        }
    }

    public static void main(String[] args) {
        int[] sorted = {1, 2, 2, 2, 5};
        check(lowerBound(sorted, 2) == 1, "lower bound");
        check(upperBound(sorted, 2) == 4, "upper bound");
        check(Arrays.equals(equalRange(sorted, 2), new int[] {1, 3}), "equal range");
        check(Arrays.equals(equalRange(sorted, 9), new int[] {-1, -1}), "missing");
        check(minimumCapacity(new int[] {1, 2, 3, 1, 1}, 4) == 3L, "capacity");
        check(firstTrue(0, 100, value -> value >= 37) == 37, "first true");
        expectFailure(() -> firstTrue(0, 10, value -> false));
        check(integerSquareRoot(0) == 0 && integerSquareRoot(15) == 3,
                "integer square root basics");
        check(integerSquareRoot(Long.MAX_VALUE) == 3_037_000_499L,
                "integer square root overflow boundary");
        expectFailure(() -> integerSquareRoot(-1));

        int[] rotated = {4, 5, 6, 7, 0, 1, 2};
        check(searchRotatedDistinct(rotated, 0) == 4, "rotated search");
        check(searchRotatedDistinct(rotated, 3) == -1, "rotated missing");
        check(minimumEatingSpeed(new int[] {3, 6, 7, 11}, 8) == 4,
                "binary search on answer");
        expectFailure(() -> minimumEatingSpeed(new int[] {3, 6}, 1));
        check(searchRowMajorMatrix(new int[][] {{1, 3, 5}, {7, 9, 11}}, 7),
                "flattened matrix search");
        check(!searchRowMajorMatrix(new int[][] {{1, 3, 5}, {7, 9, 11}}, 6),
                "matrix missing");
        expectFailure(() -> searchRowMajorMatrix(new int[][] {{1}, {2, 3}}, 2));
        check(boundsMatchLinearOracle(), "bound differential test");
        System.out.println("PASS 18 binary-search checks");
    }
}
