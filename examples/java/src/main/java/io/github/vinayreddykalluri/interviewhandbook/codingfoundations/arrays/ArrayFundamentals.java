package io.github.vinayreddykalluri.interviewhandbook.codingfoundations.arrays;

import java.util.Arrays;
import java.util.NoSuchElementException;

/**
 * Runnable implementations for the Array Foundations chapter.
 */
public final class ArrayFundamentals {

    private ArrayFundamentals() {
    }

    public static long sum(int[] values) {
        requireArray(values);
        long total = 0;
        for (int value : values) {
            total += value;
        }
        return total;
    }

    /**
     * Returns a two-element array containing minimum and maximum.
     */
    public static int[] minAndMax(int[] values) {
        requireArray(values);
        if (values.length == 0) {
            throw new NoSuchElementException("minimum and maximum require nonempty input");
        }

        int minimum = values[0];
        int maximum = values[0];
        for (int i = 1; i < values.length; i++) {
            minimum = Math.min(minimum, values[i]);
            maximum = Math.max(maximum, values[i]);
        }
        return new int[] {minimum, maximum};
    }

    public static int firstIndexOf(int[] values, int target) {
        requireArray(values);
        for (int i = 0; i < values.length; i++) {
            if (values[i] == target) {
                return i;
            }
        }
        return -1;
    }

    public static void reverse(int[] values) {
        requireArray(values);
        reverseRange(values, 0, values.length);
    }

    /**
     * Reverses the half-open range [left, right).
     */
    public static void reverseRange(int[] values, int left, int right) {
        requireArray(values);
        requireRange(left, right, values.length);

        int low = left;
        int high = right - 1;
        while (low < high) {
            swap(values, low++, high--);
        }
    }

    /**
     * Returns a new array containing value inserted at index.
     */
    public static int[] inserted(int[] values, int index, int value) {
        requireArray(values);
        if (index < 0 || index > values.length) {
            throw new IndexOutOfBoundsException("index: " + index);
        }

        int[] result = new int[values.length + 1];
        System.arraycopy(values, 0, result, 0, index);
        result[index] = value;
        System.arraycopy(values, index, result, index + 1, values.length - index);
        return result;
    }

    /**
     * Returns a new array with the element at index removed.
     */
    public static int[] deleted(int[] values, int index) {
        requireArray(values);
        if (index < 0 || index >= values.length) {
            throw new IndexOutOfBoundsException("index: " + index);
        }

        int[] result = new int[values.length - 1];
        System.arraycopy(values, 0, result, 0, index);
        System.arraycopy(values, index + 1, result, index, values.length - index - 1);
        return result;
    }

    /**
     * Removes target values in place and returns the logical result length.
     */
    public static int compactRemoving(int[] values, int target) {
        requireArray(values);

        int write = 0;
        for (int read = 0; read < values.length; read++) {
            if (values[read] != target) {
                values[write++] = values[read];
            }
        }
        return write;
    }

    /**
     * Rotates right. Negative steps rotate left.
     */
    public static void rotateRight(int[] values, int steps) {
        requireArray(values);
        if (values.length == 0) {
            return;
        }

        int normalized = ((steps % values.length) + values.length) % values.length;
        if (normalized == 0) {
            return;
        }

        reverseRange(values, 0, values.length);
        reverseRange(values, 0, normalized);
        reverseRange(values, normalized, values.length);
    }

    /**
     * Builds an exclusive long prefix-sum array of length n + 1.
     */
    public static long[] exclusivePrefixSums(int[] values) {
        requireArray(values);

        long[] prefix = new long[values.length + 1];
        for (int i = 0; i < values.length; i++) {
            prefix[i + 1] = prefix[i] + values[i];
        }
        return prefix;
    }

    public static long rangeSum(long[] prefix, int left, int right) {
        if (prefix == null || prefix.length == 0) {
            throw new IllegalArgumentException("prefix must contain its zero sentinel");
        }

        int originalLength = prefix.length - 1;
        requireRange(left, right, originalLength);
        return prefix[right] - prefix[left];
    }

    /**
     * Returns a new transposed matrix and rejects null or jagged rows.
     */
    public static int[][] transposeRectangular(int[][] matrix) {
        int columns = requireRectangular(matrix);
        int rows = matrix.length;
        int[][] result = new int[columns][rows];

        for (int row = 0; row < rows; row++) {
            for (int column = 0; column < columns; column++) {
                result[column][row] = matrix[row][column];
            }
        }
        return result;
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

    private static void requireArray(int[] values) {
        if (values == null) {
            throw new IllegalArgumentException("array must not be null");
        }
    }

    private static void requireRange(int left, int right, int length) {
        if (left < 0 || right < left || right > length) {
            throw new IndexOutOfBoundsException(
                    "invalid half-open range [" + left + ", " + right + ") for length " + length);
        }
    }

    private static void swap(int[] values, int left, int right) {
        int temporary = values[left];
        values[left] = values[right];
        values[right] = temporary;
    }

    public static void main(String[] args) {
        int[] values = {1, 2, 3, 4, 5};
        rotateRight(values, 2);

        long[] prefix = exclusivePrefixSums(values);
        System.out.println("rotated = " + Arrays.toString(values));
        System.out.println("sum [1, 4) = " + rangeSum(prefix, 1, 4));
        System.out.println("inserted = " + Arrays.toString(inserted(values, 2, 99)));
    }
}
