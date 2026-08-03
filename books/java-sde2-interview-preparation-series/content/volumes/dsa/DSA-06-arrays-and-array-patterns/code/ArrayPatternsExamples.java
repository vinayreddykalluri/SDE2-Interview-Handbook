import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.PriorityQueue;
import java.util.Random;

/** Dependency-free executable checks for the Arrays and Array Patterns volume. */
public final class ArrayPatternsExamples {
    private static int checks;

    private ArrayPatternsExamples() {}

    record Subarray(long sum, int start, int endExclusive) {}
    record RangeAdd(int left, int rightInclusive, int delta) {}
    record StableItem(int key, String label) {}

    public static void main(String[] args) {
        check(Arrays.equals(reverseCopy(new int[] {1, 2, 3}), new int[] {3, 2, 1}));
        check(Arrays.equals(reverseCopy(new int[0]), new int[0]));
        check(Arrays.equals(rotateRight(new int[] {1, 2, 3, 4}, 1), new int[] {4, 1, 2, 3}));
        check(Arrays.equals(rotateRight(new int[] {1, 2, 3, 4}, 6), new int[] {3, 4, 1, 2}));
        check(Arrays.equals(rotateRight(new int[] {1, 2, 3}, -1), new int[] {2, 3, 1}));

        int[][] matrix = {{1, 2}, {3}};
        int[][] copied = deepCopy(matrix);
        copied[0][0] = 9;
        check(matrix[0][0] == 1);
        check(copied != matrix && copied[0] != matrix[0]);
        check(deepCopy(new int[][] {null})[0] == null);

        int[] unique = {1, 1, 2, 2, 3};
        check(compactUniqueSorted(unique) == 3);
        check(Arrays.equals(Arrays.copyOf(unique, 3), new int[] {1, 2, 3}));
        int[] zeros = {0, 1, 0, 3, 12};
        moveZerosToEnd(zeros);
        check(Arrays.equals(zeros, new int[] {1, 3, 12, 0, 0}));
        check(moveZerosToEnd(new int[0]) == 0);

        check(Arrays.equals(sortedTwoSum(new int[] {1, 3, 5, 8}, 11), new int[] {1, 3}));
        check(sortedTwoSum(new int[] {1, 2}, 10).length == 0);
        check(Arrays.equals(sortedTwoSum(
                new int[] {Integer.MAX_VALUE - 1, Integer.MAX_VALUE},
                4_294_967_293L), new int[] {0, 1}));
        check(Arrays.equals(mergeSorted(new int[] {1, 4}, new int[] {2, 3}),
                new int[] {1, 2, 3, 4}));
        check(Arrays.equals(mergeSorted(new int[0], new int[] {2}), new int[] {2}));

        int[] colors = {2, 0, 2, 1, 1, 0};
        sortThreeValues(colors);
        check(Arrays.equals(colors, new int[] {0, 0, 1, 1, 2, 2}));
        int[] allOne = {1, 1};
        sortThreeValues(allOne);
        check(Arrays.equals(allOne, new int[] {1, 1}));
        check(trappedWater(new int[] {0, 1, 0, 2, 1, 0, 1, 3, 2, 1, 2, 1}) == 6);
        check(trappedWater(new int[] {1, 2, 3}) == 0);

        check(maximumFixedWindowSum(new int[] {2, 1, 5, 1, 3, 2}, 3) == 9);
        check(maximumFixedWindowSum(new int[] {-5, -2}, 1) == -2);
        check(minimumLengthAtLeast(new int[] {2, 3, 1, 2, 4, 3}, 7) == 2);
        check(minimumLengthAtLeast(new int[] {1, 1}, 5) == 0);
        check(maximumSubarray(new int[] {-2, 1, -3, 4, -1, 2, 1, -5, 4})
                .equals(new Subarray(6, 3, 7)));
        check(maximumSubarray(new int[] {-4, -1, -7}).equals(new Subarray(-1, 1, 2)));
        check(maximumProductSubarray(new int[] {2, 3, -2, 4}) == 6);
        check(maximumProductSubarray(new int[] {-2, 0, -1}) == 0);
        check(countSubarraysWithSum(new int[] {1, 1, 1}, 2) == 2);
        check(countSubarraysWithSum(new int[] {1, -1, 0}, 0) == 3);

        long[] prefix = buildPrefixSums(new int[] {3, -1, 4, 2});
        check(Arrays.equals(prefix, new long[] {0, 3, 2, 6, 8}));
        check(rangeSum(prefix, 1, 4) == 5);
        check(rangeSum(prefix, 0, 0) == 0);
        check(Arrays.equals(productExceptSelf(new int[] {1, 2, 3, 4}),
                new long[] {24, 12, 8, 6}));
        check(Arrays.equals(productExceptSelf(new int[] {0, 2, 3}),
                new long[] {6, 0, 0}));
        check(Arrays.equals(applyRangeAdds(5, List.of(
                new RangeAdd(1, 3, 2), new RangeAdd(2, 4, -1))),
                new long[] {0, 2, 1, 1, -1}));

        long[][] prefix2D = buildPrefix2D(new int[][] {{1, 2}, {3, 4}});
        check(rectangleSum(prefix2D, 0, 0, 2, 2) == 10);
        check(rectangleSum(prefix2D, 0, 1, 2, 2) == 6);

        check(Arrays.deepEquals(mergeIntervals(new int[][] {{1, 3}, {2, 4}, {8, 9}}),
                new int[][] {{1, 4}, {8, 9}}));
        int[][] originalIntervals = {{3, 4}, {1, 2}};
        mergeIntervals(originalIntervals);
        check(originalIntervals[0][0] == 3);
        check(firstMissingPositive(new int[] {3, 4, -1, 1}) == 2);
        check(firstMissingPositive(new int[] {1, 2, 0}) == 3);
        check(findDuplicates(new int[] {4, 3, 2, 7, 8, 2, 3, 1})
                .equals(List.of(2, 3)));

        int[][] square = {{1, 2, 3}, {4, 5, 6}, {7, 8, 9}};
        rotateClockwise(square);
        check(Arrays.deepEquals(square, new int[][] {{7, 4, 1}, {8, 5, 2}, {9, 6, 3}}));
        int[][] one = {{7}};
        rotateClockwise(one);
        check(one[0][0] == 7);

        PriorityQueue<Integer> minimumHeap = new PriorityQueue<>();
        minimumHeap.addAll(List.of(4, 1, 3));
        check(minimumHeap.remove() == 1);
        check(Integer.compare(Integer.MIN_VALUE, Integer.MAX_VALUE) < 0);
        check(Arrays.equals(Arrays.copyOf(new int[] {1, 2, 3}, 5),
                new int[] {1, 2, 3, 0, 0}));
        check(Arrays.deepEquals(deepCopy(new int[][] {{1}, {2, 3}}),
                new int[][] {{1}, {2, 3}}));

        int[] insertion = {5, -1, 5, 0, Integer.MIN_VALUE};
        insertionSort(insertion);
        check(Arrays.equals(insertion,
                new int[] {Integer.MIN_VALUE, -1, 0, 5, 5}));

        StableItem[] stable = {
            new StableItem(2, "first-two"),
            new StableItem(1, "one"),
            new StableItem(2, "second-two")
        };
        stableMergeSort(stable);
        check(Arrays.equals(stable, new StableItem[] {
            new StableItem(1, "one"),
            new StableItem(2, "first-two"),
            new StableItem(2, "second-two")
        }));

        int[] quick = {3, 1, 2, 3, 0, 3, -1};
        threeWayQuickSort(quick);
        check(Arrays.equals(quick, new int[] {-1, 0, 1, 2, 3, 3, 3}));
        check(Arrays.equals(countingSort(new int[] {3, -1, 2, -1}, -1, 3),
                new int[] {-1, -1, 2, 3}));

        int[] selected = {9, 1, 7, 3, 3, 8};
        check(quickselectKthSmallest(selected, 2, 42L) == 3);
        check(Arrays.equals(selected, new int[] {9, 1, 7, 3, 3, 8}));
        check(sortAlgorithmsMatchJdkOnRandomInputs());
        check(quickselectMatchesSortingOnRandomInputs());
        checkThrows(() -> countingSort(new int[] {1}, 2, 1));
        checkThrows(() -> quickselectKthSmallest(new int[0], 0, 1L));
        check(checks == 60);

        System.out.println("PASS 60 Arrays checks");
    }

    static int[] reverseCopy(int[] input) {
        int[] result = input.clone();
        reverse(result, 0, result.length);
        return result;
    }

    static void reverse(int[] values, int left, int rightExclusive) {
        for (int right = rightExclusive - 1; left < right; left++, right--) {
            int temporary = values[left];
            values[left] = values[right];
            values[right] = temporary;
        }
    }

    static int[] rotateRight(int[] input, int distance) {
        int[] result = input.clone();
        if (result.length == 0) {
            return result;
        }
        int normalized = Math.floorMod(distance, result.length);
        reverse(result, 0, result.length);
        reverse(result, 0, normalized);
        reverse(result, normalized, result.length);
        return result;
    }

    static int[][] deepCopy(int[][] matrix) {
        int[][] copy = new int[matrix.length][];
        for (int row = 0; row < matrix.length; row++) {
            copy[row] = matrix[row] == null ? null : matrix[row].clone();
        }
        return copy;
    }

    static int compactUniqueSorted(int[] sorted) {
        int write = 0;
        for (int value : sorted) {
            if (write == 0 || value != sorted[write - 1]) {
                sorted[write++] = value;
            }
        }
        return write;
    }

    static int moveZerosToEnd(int[] values) {
        int write = 0;
        for (int value : values) {
            if (value != 0) {
                values[write++] = value;
            }
        }
        int nonZeroCount = write;
        while (write < values.length) {
            values[write++] = 0;
        }
        return nonZeroCount;
    }

    static int[] sortedTwoSum(int[] sorted, long target) {
        int left = 0;
        int right = sorted.length - 1;
        while (left < right) {
            long sum = (long) sorted[left] + sorted[right];
            if (sum == target) {
                return new int[] {left, right};
            }
            if (sum < target) {
                left++;
            } else {
                right--;
            }
        }
        return new int[0];
    }

    static int[] mergeSorted(int[] first, int[] second) {
        int[] result = new int[first.length + second.length];
        int left = 0;
        int right = 0;
        int write = 0;
        while (left < first.length || right < second.length) {
            if (right == second.length
                    || (left < first.length && first[left] <= second[right])) {
                result[write++] = first[left++];
            } else {
                result[write++] = second[right++];
            }
        }
        return result;
    }

    static void sortThreeValues(int[] values) {
        int low = 0;
        int current = 0;
        int high = values.length - 1;
        while (current <= high) {
            if (values[current] == 0) {
                swap(values, low++, current++);
            } else if (values[current] == 1) {
                current++;
            } else if (values[current] == 2) {
                swap(values, current, high--);
            } else {
                throw new IllegalArgumentException("only 0, 1, and 2 are supported");
            }
        }
    }

    static long trappedWater(int[] heights) {
        int left = 0;
        int right = heights.length - 1;
        int leftMaximum = 0;
        int rightMaximum = 0;
        long water = 0;
        while (left < right) {
            if (heights[left] <= heights[right]) {
                leftMaximum = Math.max(leftMaximum, heights[left]);
                water += leftMaximum - heights[left++];
            } else {
                rightMaximum = Math.max(rightMaximum, heights[right]);
                water += rightMaximum - heights[right--];
            }
        }
        return water;
    }

    static long maximumFixedWindowSum(int[] values, int size) {
        if (size <= 0 || size > values.length) {
            throw new IllegalArgumentException("invalid window");
        }
        long sum = 0;
        for (int index = 0; index < size; index++) {
            sum += values[index];
        }
        long best = sum;
        for (int right = size; right < values.length; right++) {
            sum += values[right] - (long) values[right - size];
            best = Math.max(best, sum);
        }
        return best;
    }

    static int minimumLengthAtLeast(int[] positiveValues, long target) {
        int best = Integer.MAX_VALUE;
        int left = 0;
        long sum = 0;
        for (int right = 0; right < positiveValues.length; right++) {
            sum += positiveValues[right];
            while (sum >= target) {
                best = Math.min(best, right - left + 1);
                sum -= positiveValues[left++];
            }
        }
        return best == Integer.MAX_VALUE ? 0 : best;
    }

    static Subarray maximumSubarray(int[] values) {
        if (values.length == 0) {
            throw new IllegalArgumentException("non-empty input required");
        }
        long ending = values[0];
        long best = values[0];
        int candidateStart = 0;
        int bestStart = 0;
        int bestEnd = 1;
        for (int index = 1; index < values.length; index++) {
            if (values[index] > ending + values[index]) {
                ending = values[index];
                candidateStart = index;
            } else {
                ending += values[index];
            }
            if (ending > best) {
                best = ending;
                bestStart = candidateStart;
                bestEnd = index + 1;
            }
        }
        return new Subarray(best, bestStart, bestEnd);
    }

    static long maximumProductSubarray(int[] values) {
        long maximum = values[0];
        long minimum = values[0];
        long best = values[0];
        for (int index = 1; index < values.length; index++) {
            long value = values[index];
            if (value < 0) {
                long temporary = maximum;
                maximum = minimum;
                minimum = temporary;
            }
            maximum = Math.max(value, maximum * value);
            minimum = Math.min(value, minimum * value);
            best = Math.max(best, maximum);
        }
        return best;
    }

    static long countSubarraysWithSum(int[] values, long target) {
        Map<Long, Integer> frequencies = new HashMap<>();
        frequencies.put(0L, 1);
        long prefix = 0;
        long count = 0;
        for (int value : values) {
            prefix += value;
            count += frequencies.getOrDefault(prefix - target, 0);
            frequencies.merge(prefix, 1, Integer::sum);
        }
        return count;
    }

    static long[] buildPrefixSums(int[] values) {
        long[] prefix = new long[values.length + 1];
        for (int index = 0; index < values.length; index++) {
            prefix[index + 1] = prefix[index] + values[index];
        }
        return prefix;
    }

    static long rangeSum(long[] prefix, int left, int rightExclusive) {
        return prefix[rightExclusive] - prefix[left];
    }

    static long[] productExceptSelf(int[] values) {
        long[] result = new long[values.length];
        long prefix = 1;
        for (int index = 0; index < values.length; index++) {
            result[index] = prefix;
            prefix *= values[index];
        }
        long suffix = 1;
        for (int index = values.length - 1; index >= 0; index--) {
            result[index] *= suffix;
            suffix *= values[index];
        }
        return result;
    }

    static long[] applyRangeAdds(int length, List<RangeAdd> updates) {
        long[] difference = new long[length + 1];
        for (RangeAdd update : updates) {
            difference[update.left()] += update.delta();
            difference[update.rightInclusive() + 1] -= update.delta();
        }
        long[] result = new long[length];
        long active = 0;
        for (int index = 0; index < length; index++) {
            active += difference[index];
            result[index] = active;
        }
        return result;
    }

    static long[][] buildPrefix2D(int[][] matrix) {
        int columns = matrix.length == 0 ? 0 : matrix[0].length;
        long[][] prefix = new long[matrix.length + 1][columns + 1];
        for (int row = 0; row < matrix.length; row++) {
            for (int column = 0; column < columns; column++) {
                prefix[row + 1][column + 1] = matrix[row][column]
                        + prefix[row][column + 1]
                        + prefix[row + 1][column]
                        - prefix[row][column];
            }
        }
        return prefix;
    }

    static long rectangleSum(long[][] prefix, int top, int left, int bottom, int right) {
        return prefix[bottom][right] - prefix[top][right]
                - prefix[bottom][left] + prefix[top][left];
    }

    static int[][] mergeIntervals(int[][] intervals) {
        if (intervals.length == 0) {
            return new int[0][];
        }
        int[][] sorted = deepCopy(intervals);
        Arrays.sort(sorted, (first, second) -> Integer.compare(first[0], second[0]));
        List<int[]> merged = new ArrayList<>();
        int start = sorted[0][0];
        int end = sorted[0][1];
        for (int index = 1; index < sorted.length; index++) {
            if (sorted[index][0] <= end) {
                end = Math.max(end, sorted[index][1]);
            } else {
                merged.add(new int[] {start, end});
                start = sorted[index][0];
                end = sorted[index][1];
            }
        }
        merged.add(new int[] {start, end});
        return merged.toArray(int[][]::new);
    }

    static int firstMissingPositive(int[] input) {
        int[] values = input.clone();
        for (int index = 0; index < values.length;) {
            int value = values[index];
            if (value > 0 && value <= values.length && values[value - 1] != value) {
                swap(values, index, value - 1);
            } else {
                index++;
            }
        }
        for (int index = 0; index < values.length; index++) {
            if (values[index] != index + 1) {
                return index + 1;
            }
        }
        return values.length + 1;
    }

    static List<Integer> findDuplicates(int[] input) {
        int[] values = input.clone();
        List<Integer> duplicates = new ArrayList<>();
        for (int value : values) {
            int index = Math.abs(value) - 1;
            if (values[index] < 0) {
                duplicates.add(Math.abs(value));
            } else {
                values[index] = -values[index];
            }
        }
        return duplicates;
    }

    static void rotateClockwise(int[][] matrix) {
        int size = matrix.length;
        for (int[] row : matrix) {
            if (row.length != size) {
                throw new IllegalArgumentException("square matrix required");
            }
        }
        for (int row = 0; row < size; row++) {
            for (int column = row + 1; column < size; column++) {
                int temporary = matrix[row][column];
                matrix[row][column] = matrix[column][row];
                matrix[column][row] = temporary;
            }
        }
        for (int[] row : matrix) {
            reverse(row, 0, row.length);
        }
    }

    /** Stable O(n^2) baseline that is excellent for tiny or nearly sorted inputs. */
    static void insertionSort(int[] values) {
        requireArray(values);
        for (int index = 1; index < values.length; index++) {
            int value = values[index];
            int position = index;
            while (position > 0 && values[position - 1] > value) {
                values[position] = values[position - 1];
                position--;
            }
            values[position] = value;
        }
    }

    /** Stable merge sort; equality deliberately takes from the left half first. */
    static void stableMergeSort(StableItem[] values) {
        if (values == null) {
            throw new IllegalArgumentException("values is null");
        }
        StableItem[] buffer = new StableItem[values.length];
        stableMergeSort(values, buffer, 0, values.length);
    }

    private static void stableMergeSort(
            StableItem[] values, StableItem[] buffer, int left, int right) {
        if (right - left < 2) {
            return;
        }
        int middle = left + (right - left) / 2;
        stableMergeSort(values, buffer, left, middle);
        stableMergeSort(values, buffer, middle, right);
        int first = left;
        int second = middle;
        int write = left;
        while (first < middle || second < right) {
            if (second == right
                    || first < middle && values[first].key() <= values[second].key()) {
                buffer[write++] = values[first++];
            } else {
                buffer[write++] = values[second++];
            }
        }
        System.arraycopy(buffer, left, values, left, right - left);
    }

    /** In-place three-way quicksort; equal keys form one settled middle region. */
    static void threeWayQuickSort(int[] values) {
        requireArray(values);
        threeWayQuickSort(values, 0, values.length - 1);
    }

    private static void threeWayQuickSort(int[] values, int left, int right) {
        if (left >= right) {
            return;
        }
        int pivot = values[left + (right - left) / 2];
        int lower = left;
        int scan = left;
        int upper = right;
        while (scan <= upper) {
            if (values[scan] < pivot) {
                swap(values, lower++, scan++);
            } else if (values[scan] > pivot) {
                swap(values, scan, upper--);
            } else {
                scan++;
            }
        }
        threeWayQuickSort(values, left, lower - 1);
        threeWayQuickSort(values, upper + 1, right);
    }

    /**
     * Counting sort for a caller-declared dense range. The returned array is new,
     * and an excessive or inconsistent range is rejected before allocation.
     */
    static int[] countingSort(int[] values, int minimum, int maximum) {
        requireArray(values);
        if (minimum > maximum) {
            throw new IllegalArgumentException("minimum exceeds maximum");
        }
        long width = (long) maximum - minimum + 1L;
        if (width > 1_000_000L) {
            throw new IllegalArgumentException("declared range is too wide");
        }
        int[] frequencies = new int[(int) width];
        for (int value : values) {
            if (value < minimum || value > maximum) {
                throw new IllegalArgumentException("value outside declared range");
            }
            frequencies[value - minimum]++;
        }
        int[] sorted = new int[values.length];
        int write = 0;
        for (int offset = 0; offset < frequencies.length; offset++) {
            for (int count = frequencies[offset]; count > 0; count--) {
                sorted[write++] = minimum + offset;
            }
        }
        return sorted;
    }

    /** Returns the zero-based kth-smallest value without mutating caller input. */
    static int quickselectKthSmallest(int[] input, int k, long seed) {
        requireArray(input);
        if (k < 0 || k >= input.length) {
            throw new IllegalArgumentException("k must be in [0,n)");
        }
        int[] values = input.clone();
        Random random = new Random(seed);
        int left = 0;
        int right = values.length - 1;
        while (left <= right) {
            int pivotIndex = left + random.nextInt(right - left + 1);
            int settled = partitionAroundPivot(values, left, right, pivotIndex);
            if (settled == k) {
                return values[settled];
            }
            if (settled < k) {
                left = settled + 1;
            } else {
                right = settled - 1;
            }
        }
        throw new AssertionError("valid rank was not found");
    }

    private static int partitionAroundPivot(
            int[] values, int left, int right, int pivotIndex) {
        int pivot = values[pivotIndex];
        swap(values, pivotIndex, right);
        int write = left;
        for (int scan = left; scan < right; scan++) {
            if (values[scan] < pivot) {
                swap(values, write++, scan);
            }
        }
        swap(values, write, right);
        return write;
    }

    private static boolean sortAlgorithmsMatchJdkOnRandomInputs() {
        Random random = new Random(7L);
        for (int trial = 0; trial < 500; trial++) {
            int[] input = new int[random.nextInt(30)];
            for (int index = 0; index < input.length; index++) {
                input[index] = random.nextInt(21) - 10;
            }
            int[] expected = input.clone();
            Arrays.sort(expected);
            int[] insertion = input.clone();
            insertionSort(insertion);
            int[] quick = input.clone();
            threeWayQuickSort(quick);
            if (!Arrays.equals(expected, insertion) || !Arrays.equals(expected, quick)) {
                return false;
            }
        }
        return true;
    }

    private static boolean quickselectMatchesSortingOnRandomInputs() {
        Random random = new Random(11L);
        for (int trial = 0; trial < 500; trial++) {
            int length = 1 + random.nextInt(30);
            int[] input = new int[length];
            for (int index = 0; index < length; index++) {
                input[index] = random.nextInt(31) - 15;
            }
            int[] expected = input.clone();
            Arrays.sort(expected);
            for (int k = 0; k < length; k++) {
                if (quickselectKthSmallest(input, k, trial * 31L + k) != expected[k]) {
                    return false;
                }
            }
        }
        return true;
    }

    private static void requireArray(int[] values) {
        if (values == null) {
            throw new IllegalArgumentException("values is null");
        }
    }

    private static void checkThrows(Runnable action) {
        try {
            action.run();
        } catch (IllegalArgumentException expected) {
            check(true);
            return;
        }
        throw new AssertionError("expected IllegalArgumentException");
    }

    static void swap(int[] values, int first, int second) {
        int temporary = values[first];
        values[first] = values[second];
        values[second] = temporary;
    }

    static void check(boolean condition) {
        if (!condition) {
            throw new AssertionError("check " + (checks + 1) + " failed");
        }
        checks++;
    }
}
