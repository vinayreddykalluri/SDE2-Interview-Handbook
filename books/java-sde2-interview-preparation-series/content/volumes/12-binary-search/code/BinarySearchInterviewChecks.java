import java.util.Arrays;

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
        System.out.println("PASS 5 binary-search checks");
    }
}
