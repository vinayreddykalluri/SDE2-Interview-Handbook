package io.github.vinayreddykalluri.interviewhandbook.codingfoundations.arrays;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Deque;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Reference implementations for the major array DSA patterns.
 */
public final class ArrayDsaPatterns {

    private ArrayDsaPatterns() {
    }

    /**
     * Kadane's algorithm for a nonempty subarray.
     */
    public static long maximumSubarraySum(int[] values) {
        requireNonempty(values);

        long bestEndingHere = values[0];
        long bestOverall = values[0];
        for (int i = 1; i < values.length; i++) {
            bestEndingHere = Math.max(values[i], bestEndingHere + values[i]);
            bestOverall = Math.max(bestOverall, bestEndingHere);
        }
        return bestOverall;
    }

    /**
     * Compacts sorted input to unique values and returns logical length.
     */
    public static int compactSortedUnique(int[] values) {
        requireArray(values);
        if (values.length == 0) {
            return 0;
        }

        int write = 1;
        for (int read = 1; read < values.length; read++) {
            if (values[read] != values[write - 1]) {
                values[write++] = values[read];
            }
        }
        return write;
    }

    /**
     * Returns indices of a pair in sorted input, or {-1, -1}.
     */
    public static int[] twoSumSorted(int[] values, long target) {
        requireArray(values);

        int left = 0;
        int right = values.length - 1;
        while (left < right) {
            long sum = (long) values[left] + values[right];
            if (sum == target) {
                return new int[] {left, right};
            }
            if (sum < target) {
                left++;
            } else {
                right--;
            }
        }
        return new int[] {-1, -1};
    }

    public static long maximumFixedWindowSum(int[] values, int windowSize) {
        requireArray(values);
        requireWindow(windowSize, values.length);

        long windowSum = 0;
        for (int i = 0; i < windowSize; i++) {
            windowSum += values[i];
        }

        long best = windowSum;
        for (int right = windowSize; right < values.length; right++) {
            windowSum += values[right];
            windowSum -= values[right - windowSize];
            best = Math.max(best, windowSum);
        }
        return best;
    }

    /**
     * Returns zero when no qualifying window exists. Values must be positive.
     */
    public static int minimumPositiveWindowAtLeast(int[] values, long target) {
        requireArray(values);
        if (target <= 0) {
            throw new IllegalArgumentException("target must be positive");
        }
        for (int value : values) {
            if (value <= 0) {
                throw new IllegalArgumentException("all values must be positive");
            }
        }

        int left = 0;
        int best = Integer.MAX_VALUE;
        long sum = 0;

        for (int right = 0; right < values.length; right++) {
            sum += values[right];
            while (sum >= target) {
                best = Math.min(best, right - left + 1);
                sum -= values[left++];
            }
        }
        return best == Integer.MAX_VALUE ? 0 : best;
    }

    public static long countSubarraysWithSum(int[] values, long target) {
        requireArray(values);

        Map<Long, Integer> prefixFrequency = new HashMap<>();
        prefixFrequency.put(0L, 1);

        long prefix = 0;
        long count = 0;
        for (int value : values) {
            prefix += value;
            count += prefixFrequency.getOrDefault(prefix - target, 0);
            prefixFrequency.merge(prefix, 1, Integer::sum);
        }
        return count;
    }

    /**
     * Applies updates {left, right, delta} to inclusive ranges.
     */
    public static long[] applyRangeAdditions(int length, int[][] updates) {
        if (length < 0) {
            throw new IllegalArgumentException("length must be nonnegative");
        }
        if (updates == null) {
            throw new IllegalArgumentException("updates must not be null");
        }

        long[] difference = new long[length + 1];
        for (int[] update : updates) {
            if (update == null || update.length != 3) {
                throw new IllegalArgumentException("each update must be {left, right, delta}");
            }

            int left = update[0];
            int right = update[1];
            int delta = update[2];
            if (left < 0 || right < left || right >= length) {
                throw new IndexOutOfBoundsException("invalid update range");
            }

            difference[left] += delta;
            difference[right + 1] -= delta;
        }

        long[] result = new long[length];
        long running = 0;
        for (int i = 0; i < length; i++) {
            running += difference[i];
            result[i] = running;
        }
        return result;
    }

    /**
     * Three-way partitions values containing only 0, 1, and 2.
     */
    public static void sortColors(int[] values) {
        requireArray(values);

        int low = 0;
        int middle = 0;
        int high = values.length - 1;

        while (middle <= high) {
            switch (values[middle]) {
                case 0:
                    swap(values, low++, middle++);
                    break;
                case 1:
                    middle++;
                    break;
                case 2:
                    swap(values, middle, high--);
                    break;
                default:
                    throw new IllegalArgumentException("values must contain only 0, 1, and 2");
            }
        }
    }

    /**
     * Cyclic placement. Mutates input.
     */
    public static int firstMissingPositive(int[] values) {
        requireArray(values);

        for (int i = 0; i < values.length; i++) {
            while (values[i] >= 1
                    && values[i] <= values.length
                    && values[values[i] - 1] != values[i]) {
                swap(values, i, values[i] - 1);
            }
        }

        for (int i = 0; i < values.length; i++) {
            if (values[i] != i + 1) {
                return i + 1;
            }
        }
        return values.length + 1;
    }

    public static int lowerBound(int[] values, int target) {
        requireArray(values);

        int left = 0;
        int right = values.length;
        while (left < right) {
            int middle = left + (right - left) / 2;
            if (values[middle] < target) {
                left = middle + 1;
            } else {
                right = middle;
            }
        }
        return left;
    }

    public static int[] nextGreaterValues(int[] values) {
        requireArray(values);

        int[] answer = new int[values.length];
        Arrays.fill(answer, -1);
        Deque<Integer> decreasing = new ArrayDeque<>();

        for (int i = 0; i < values.length; i++) {
            while (!decreasing.isEmpty() && values[decreasing.peek()] < values[i]) {
                answer[decreasing.pop()] = values[i];
            }
            decreasing.push(i);
        }
        return answer;
    }

    public static int[] maximumOfEveryWindow(int[] values, int windowSize) {
        requireArray(values);
        requireWindow(windowSize, values.length);

        int[] answer = new int[values.length - windowSize + 1];
        Deque<Integer> decreasing = new ArrayDeque<>();

        for (int right = 0; right < values.length; right++) {
            while (!decreasing.isEmpty() && decreasing.peekFirst() <= right - windowSize) {
                decreasing.removeFirst();
            }
            while (!decreasing.isEmpty()
                    && values[decreasing.peekLast()] <= values[right]) {
                decreasing.removeLast();
            }

            decreasing.addLast(right);
            if (right >= windowSize - 1) {
                answer[right - windowSize + 1] = values[decreasing.peekFirst()];
            }
        }
        return answer;
    }

    /**
     * Merges closed intervals. Touching endpoints overlap.
     */
    public static int[][] mergeClosedIntervals(int[][] intervals) {
        if (intervals == null) {
            throw new IllegalArgumentException("intervals must not be null");
        }
        if (intervals.length == 0) {
            return new int[0][0];
        }

        int[][] sorted = new int[intervals.length][2];
        for (int i = 0; i < intervals.length; i++) {
            if (intervals[i] == null || intervals[i].length != 2) {
                throw new IllegalArgumentException("each interval must contain start and end");
            }
            if (intervals[i][0] > intervals[i][1]) {
                throw new IllegalArgumentException("interval start must not exceed end");
            }
            sorted[i] = intervals[i].clone();
        }

        Arrays.sort(sorted, (left, right) -> {
            int byStart = Integer.compare(left[0], right[0]);
            return byStart != 0 ? byStart : Integer.compare(left[1], right[1]);
        });

        List<int[]> merged = new ArrayList<>();
        int activeStart = sorted[0][0];
        int activeEnd = sorted[0][1];

        for (int i = 1; i < sorted.length; i++) {
            if (sorted[i][0] <= activeEnd) {
                activeEnd = Math.max(activeEnd, sorted[i][1]);
            } else {
                merged.add(new int[] {activeStart, activeEnd});
                activeStart = sorted[i][0];
                activeEnd = sorted[i][1];
            }
        }
        merged.add(new int[] {activeStart, activeEnd});
        return merged.toArray(new int[merged.size()][]);
    }

    private static void requireArray(int[] values) {
        if (values == null) {
            throw new IllegalArgumentException("array must not be null");
        }
    }

    private static void requireNonempty(int[] values) {
        requireArray(values);
        if (values.length == 0) {
            throw new IllegalArgumentException("array must not be empty");
        }
    }

    private static void requireWindow(int windowSize, int length) {
        if (windowSize <= 0 || windowSize > length) {
            throw new IllegalArgumentException("window size must be in [1, length]");
        }
    }

    private static void swap(int[] values, int left, int right) {
        int temporary = values[left];
        values[left] = values[right];
        values[right] = temporary;
    }

    public static void main(String[] args) {
        int[] values = {1, 3, -1, -3, 5, 3, 6, 7};
        System.out.println(Arrays.toString(maximumOfEveryWindow(values, 3)));
        System.out.println(countSubarraysWithSum(new int[] {1, -1, 0}, 0));
        System.out.println(firstMissingPositive(new int[] {3, 4, -1, 1}));
    }
}
