package io.github.vinayreddykalluri.interviewhandbook.codingfoundations.arrays;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Advanced array problems that combine multiple foundational patterns.
 */
public final class AdvancedArrayProblems {

    private AdvancedArrayProblems() {
    }

    public static long[] productExceptSelf(int[] values) {
        requireArray(values);

        long[] answer = new long[values.length];
        long prefix = 1;
        for (int i = 0; i < values.length; i++) {
            answer[i] = prefix;
            prefix *= values[i];
        }

        long suffix = 1;
        for (int i = values.length - 1; i >= 0; i--) {
            answer[i] *= suffix;
            suffix *= values[i];
        }
        return answer;
    }

    public static long trappedRainWater(int[] heights) {
        requireArray(heights);
        for (int height : heights) {
            if (height < 0) {
                throw new IllegalArgumentException("heights must be nonnegative");
            }
        }

        int left = 0;
        int right = heights.length - 1;
        int leftMaximum = 0;
        int rightMaximum = 0;
        long water = 0;

        while (left <= right) {
            if (leftMaximum <= rightMaximum) {
                leftMaximum = Math.max(leftMaximum, heights[left]);
                water += leftMaximum - heights[left];
                left++;
            } else {
                rightMaximum = Math.max(rightMaximum, heights[right]);
                water += rightMaximum - heights[right];
                right--;
            }
        }
        return water;
    }

    /**
     * Searches rotated sorted input with distinct values.
     */
    public static int searchRotatedDistinct(int[] values, int target) {
        requireArray(values);

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
            } else {
                if (values[middle] < target && target <= values[right]) {
                    left = middle + 1;
                } else {
                    right = middle - 1;
                }
            }
        }
        return -1;
    }

    /**
     * Returns the one-based kth largest value. Mutates input.
     * Expected O(n), worst-case O(n^2) with this pivot policy.
     */
    public static int kthLargest(int[] values, int k) {
        requireArray(values);
        if (k <= 0 || k > values.length) {
            throw new IllegalArgumentException("k must be in [1, length]");
        }

        int targetIndex = values.length - k;
        int left = 0;
        int right = values.length - 1;

        while (left <= right) {
            int pivotIndex = partition(values, left, right);
            if (pivotIndex == targetIndex) {
                return values[pivotIndex];
            }
            if (pivotIndex < targetIndex) {
                left = pivotIndex + 1;
            } else {
                right = pivotIndex - 1;
            }
        }
        throw new IllegalStateException("target rank was not found");
    }

    public static void rotateSquareClockwise(int[][] matrix) {
        int size = requireSquare(matrix);

        for (int row = 0; row < size; row++) {
            for (int column = row + 1; column < size; column++) {
                int temporary = matrix[row][column];
                matrix[row][column] = matrix[column][row];
                matrix[column][row] = temporary;
            }
        }

        for (int[] row : matrix) {
            reverse(row);
        }
    }

    public static List<Integer> spiralOrder(int[][] matrix) {
        int columns = requireRectangular(matrix);
        List<Integer> order = new ArrayList<>();
        if (matrix.length == 0 || columns == 0) {
            return order;
        }

        int top = 0;
        int bottom = matrix.length - 1;
        int left = 0;
        int right = columns - 1;

        while (top <= bottom && left <= right) {
            for (int column = left; column <= right; column++) {
                order.add(matrix[top][column]);
            }
            top++;

            for (int row = top; row <= bottom; row++) {
                order.add(matrix[row][right]);
            }
            right--;

            if (top <= bottom) {
                for (int column = right; column >= left; column--) {
                    order.add(matrix[bottom][column]);
                }
                bottom--;
            }

            if (left <= right) {
                for (int row = bottom; row >= top; row--) {
                    order.add(matrix[row][left]);
                }
                left++;
            }
        }
        return order;
    }

    public static long maximumCircularSubarraySum(int[] values) {
        requireNonempty(values);

        long total = values[0];
        long maximumEnding = values[0];
        long maximum = values[0];
        long minimumEnding = values[0];
        long minimum = values[0];

        for (int i = 1; i < values.length; i++) {
            total += values[i];
            maximumEnding = Math.max(values[i], maximumEnding + values[i]);
            maximum = Math.max(maximum, maximumEnding);
            minimumEnding = Math.min(values[i], minimumEnding + values[i]);
            minimum = Math.min(minimum, minimumEnding);
        }

        if (maximum < 0) {
            return maximum;
        }
        return Math.max(maximum, total - minimum);
    }

    public static int longestConsecutiveSequence(int[] values) {
        requireArray(values);

        Set<Integer> present = new HashSet<>();
        for (int value : values) {
            present.add(value);
        }

        int best = 0;
        for (int value : present) {
            if (value != Integer.MIN_VALUE && present.contains(value - 1)) {
                continue;
            }

            int length = 1;
            int current = value;
            while (current != Integer.MAX_VALUE && present.contains(current + 1)) {
                current++;
                length++;
            }
            best = Math.max(best, length);
        }
        return best;
    }

    private static int partition(int[] values, int left, int right) {
        int pivotIndex = left + (right - left) / 2;
        swap(values, pivotIndex, right);
        int write = left;

        for (int read = left; read < right; read++) {
            if (values[read] < values[right]) {
                swap(values, write++, read);
            }
        }
        swap(values, write, right);
        return write;
    }

    private static int requireSquare(int[][] matrix) {
        int columns = requireRectangular(matrix);
        if (matrix.length != columns) {
            throw new IllegalArgumentException("matrix must be square");
        }
        return matrix.length;
    }

    private static int requireRectangular(int[][] matrix) {
        if (matrix == null) {
            throw new IllegalArgumentException("matrix must not be null");
        }
        if (matrix.length == 0) {
            return 0;
        }
        if (matrix[0] == null) {
            throw new IllegalArgumentException("matrix rows must not be null");
        }

        int columns = matrix[0].length;
        for (int row = 1; row < matrix.length; row++) {
            if (matrix[row] == null || matrix[row].length != columns) {
                throw new IllegalArgumentException("matrix must be rectangular");
            }
        }
        return columns;
    }

    private static void reverse(int[] values) {
        int left = 0;
        int right = values.length - 1;
        while (left < right) {
            swap(values, left++, right--);
        }
    }

    private static void swap(int[] values, int left, int right) {
        int temporary = values[left];
        values[left] = values[right];
        values[right] = temporary;
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

    public static void main(String[] args) {
        System.out.println(Arrays.toString(productExceptSelf(new int[] {1, 2, 3, 4})));
        System.out.println(trappedRainWater(new int[] {0, 1, 0, 2, 1, 0, 1, 3, 2, 1, 2, 1}));
        System.out.println(kthLargest(new int[] {3, 2, 1, 5, 6, 4}, 2));

        int[][] matrix = {
            {1, 2, 3},
            {4, 5, 6},
            {7, 8, 9}
        };
        rotateSquareClockwise(matrix);
        System.out.println(Arrays.deepToString(matrix));
    }
}
