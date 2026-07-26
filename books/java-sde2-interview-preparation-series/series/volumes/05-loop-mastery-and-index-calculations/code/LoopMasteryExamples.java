import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/** Executable Java 21 checks for Loop Mastery, Patterns, and Index Calculations. */
public final class LoopMasteryExamples {
    private static int checks;

    private LoopMasteryExamples() {
    }

    public record Cell(int row, int col) {
    }

    public static long forwardSum(int[] values) {
        long total = 0;
        for (int value : values) {
            total += value;
        }
        return total;
    }

    public static int[] reversedCopy(int[] values) {
        int[] reversed = new int[values.length];
        for (int index = 0; index < values.length; index++) {
            reversed[index] = values[values.length - 1 - index];
        }
        return reversed;
    }

    public static int countEvenIndexes(int[] values) {
        int count = 0;
        for (int index = 0; index < values.length; index += 2) {
            count++;
        }
        return count;
    }

    public static int firstIndex(int[] values, int target) {
        for (int index = 0; index < values.length; index++) {
            if (values[index] == target) {
                return index;
            }
        }
        return -1;
    }

    public static boolean isNondecreasing(int[] values) {
        for (int index = 1; index < values.length; index++) {
            if (values[index] < values[index - 1]) {
                return false;
            }
        }
        return true;
    }

    public static int doWhileExecutions(int start) {
        int value = start;
        int executions = 0;
        do {
            executions++;
            value--;
        } while (value > 0);
        return executions;
    }

    public static List<Integer> forContinueValues(int limit, int skipped) {
        List<Integer> result = new ArrayList<>();
        for (int index = 0; index < limit; index++) {
            if (index == skipped) {
                continue;
            }
            result.add(index);
        }
        return result;
    }

    public static long raggedSum(int[][] grid) {
        if (grid == null) {
            throw new IllegalArgumentException("grid must not be null");
        }
        long total = 0;
        for (int[] row : grid) {
            if (row != null) {
                for (int value : row) {
                    total += value;
                }
            }
        }
        return total;
    }

    public static int lowerBound(int[] values, int target) {
        int low = 0;
        int high = values.length;
        while (low < high) {
            int mid = low + (high - low) / 2;
            if (values[mid] < target) {
                low = mid + 1;
            } else {
                high = mid;
            }
        }
        return low;
    }

    public static int upperBound(int[] values, int target) {
        int low = 0;
        int high = values.length;
        while (low < high) {
            int mid = low + (high - low) / 2;
            if (values[mid] <= target) {
                low = mid + 1;
            } else {
                high = mid;
            }
        }
        return low;
    }

    public static int[] sortedTwoSum(int[] values, int target) {
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

    public static boolean isPalindrome(char[] text) {
        int left = 0;
        int right = text.length - 1;
        while (left < right) {
            if (text[left] != text[right]) {
                return false;
            }
            left++;
            right--;
        }
        return true;
    }

    public static int compactRemoving(int[] values, int removedValue) {
        int write = 0;
        for (int read = 0; read < values.length; read++) {
            if (values[read] != removedValue) {
                values[write++] = values[read];
            }
        }
        return write;
    }

    public static int deduplicateSorted(int[] values) {
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

    public static int keepAtMostTwo(int[] values) {
        int write = 0;
        for (int value : values) {
            if (write < 2 || value != values[write - 2]) {
                values[write++] = value;
            }
        }
        return write;
    }

    public static int[] mergeSorted(int[] first, int[] second) {
        int[] merged = new int[Math.addExact(first.length, second.length)];
        int left = 0;
        int right = 0;
        int write = 0;
        while (left < first.length && right < second.length) {
            if (first[left] <= second[right]) {
                merged[write++] = first[left++];
            } else {
                merged[write++] = second[right++];
            }
        }
        while (left < first.length) {
            merged[write++] = first[left++];
        }
        while (right < second.length) {
            merged[write++] = second[right++];
        }
        return merged;
    }

    public static List<Integer> distinctIntersection(int[] first, int[] second) {
        List<Integer> result = new ArrayList<>();
        int left = 0;
        int right = 0;
        while (left < first.length && right < second.length) {
            if (first[left] < second[right]) {
                left++;
            } else if (first[left] > second[right]) {
                right++;
            } else {
                int value = first[left];
                result.add(value);
                while (left < first.length && first[left] == value) {
                    left++;
                }
                while (right < second.length && second[right] == value) {
                    right++;
                }
            }
        }
        return result;
    }

    public static long maxFixedWindowSum(int[] values, int width) {
        if (width <= 0 || width > values.length) {
            throw new IllegalArgumentException("invalid width");
        }
        long sum = 0;
        for (int index = 0; index < width; index++) {
            sum += values[index];
        }
        long best = sum;
        for (int right = width; right < values.length; right++) {
            sum += values[right];
            sum -= values[right - width];
            best = Math.max(best, sum);
        }
        return best;
    }

    public static int longestAtMostKDistinct(int[] values, int k) {
        if (k < 0) {
            throw new IllegalArgumentException("k must be nonnegative");
        }
        Map<Integer, Integer> frequency = new HashMap<>();
        int left = 0;
        int best = 0;
        for (int right = 0; right < values.length; right++) {
            frequency.merge(values[right], 1, Integer::sum);
            while (frequency.size() > k) {
                decrement(frequency, values[left++]);
            }
            best = Math.max(best, right - left + 1);
        }
        return best;
    }

    public static long countAtMostKDistinct(int[] values, int k) {
        if (k < 0) {
            return 0;
        }
        Map<Integer, Integer> frequency = new HashMap<>();
        int left = 0;
        long count = 0;
        for (int right = 0; right < values.length; right++) {
            frequency.merge(values[right], 1, Integer::sum);
            while (frequency.size() > k) {
                decrement(frequency, values[left++]);
            }
            count += right - left + 1L;
        }
        return count;
    }

    public static long countExactlyKDistinct(int[] values, int k) {
        if (k <= 0) {
            return 0;
        }
        return countAtMostKDistinct(values, k)
                - countAtMostKDistinct(values, k - 1);
    }

    private static void decrement(Map<Integer, Integer> frequency, int value) {
        int remaining = frequency.get(value) - 1;
        if (remaining == 0) {
            frequency.remove(value);
        } else {
            frequency.put(value, remaining);
        }
    }

    public static int shortestAtLeastTarget(int[] positive, long target) {
        if (target <= 0) {
            throw new IllegalArgumentException("target must be positive");
        }
        int left = 0;
        int best = Integer.MAX_VALUE;
        long sum = 0;
        for (int right = 0; right < positive.length; right++) {
            if (positive[right] <= 0) {
                throw new IllegalArgumentException("values must be positive");
            }
            sum += positive[right];
            while (sum >= target) {
                best = Math.min(best, right - left + 1);
                sum -= positive[left++];
            }
        }
        return best == Integer.MAX_VALUE ? 0 : best;
    }

    public static long countPairsWithinDistance(int[] sorted, int limit) {
        if (limit < 0) {
            return 0;
        }
        int left = 0;
        long pairs = 0;
        for (int right = 0; right < sorted.length; right++) {
            while ((long) sorted[right] - sorted[left] > limit) {
                left++;
            }
            pairs += right - left;
        }
        return pairs;
    }

    public static long flatten(int row, int col, int rows, int columns) {
        validateDimensions(rows, columns);
        if (row < 0 || row >= rows || col < 0 || col >= columns) {
            throw new IndexOutOfBoundsException("cell outside grid");
        }
        return Math.addExact(Math.multiplyExact((long) row, columns), col);
    }

    public static Cell unflatten(long flat, int rows, int columns) {
        validateDimensions(rows, columns);
        long capacity = Math.multiplyExact((long) rows, columns);
        if (flat < 0 || flat >= capacity) {
            throw new IndexOutOfBoundsException("index outside grid");
        }
        return new Cell((int) (flat / columns), (int) (flat % columns));
    }

    private static void validateDimensions(int rows, int columns) {
        if (rows <= 0 || columns <= 0) {
            throw new IllegalArgumentException("dimensions must be positive");
        }
        Math.multiplyExact((long) rows, columns);
    }

    public static List<Integer> rowMajor(int[][] matrix) {
        int columns = requireRectangular(matrix);
        List<Integer> result = new ArrayList<>();
        for (int row = 0; row < matrix.length; row++) {
            for (int col = 0; col < columns; col++) {
                result.add(matrix[row][col]);
            }
        }
        return result;
    }

    public static List<Integer> columnMajor(int[][] matrix) {
        int columns = requireRectangular(matrix);
        List<Integer> result = new ArrayList<>();
        for (int col = 0; col < columns; col++) {
            for (int row = 0; row < matrix.length; row++) {
                result.add(matrix[row][col]);
            }
        }
        return result;
    }

    public static List<Integer> mainDiagonal(int[][] matrix) {
        int columns = requireRectangular(matrix);
        List<Integer> result = new ArrayList<>();
        for (int index = 0; index < Math.min(matrix.length, columns); index++) {
            result.add(matrix[index][index]);
        }
        return result;
    }

    public static List<Integer> antiDiagonal(int[][] matrix) {
        int columns = requireRectangular(matrix);
        List<Integer> result = new ArrayList<>();
        for (int index = 0; index < Math.min(matrix.length, columns); index++) {
            result.add(matrix[index][columns - 1 - index]);
        }
        return result;
    }

    public static List<Integer> neighbors4(int[][] matrix, int row, int col) {
        int columns = requireRectangular(matrix);
        if (row < 0 || row >= matrix.length || col < 0 || col >= columns) {
            throw new IndexOutOfBoundsException("cell outside matrix");
        }
        int[] deltaRow = {-1, 0, 1, 0};
        int[] deltaCol = {0, 1, 0, -1};
        List<Integer> result = new ArrayList<>();
        for (int direction = 0; direction < deltaRow.length; direction++) {
            int nextRow = row + deltaRow[direction];
            int nextCol = col + deltaCol[direction];
            if (nextRow >= 0 && nextRow < matrix.length
                    && nextCol >= 0 && nextCol < columns) {
                result.add(matrix[nextRow][nextCol]);
            }
        }
        return result;
    }

    public static List<Integer> spiral(int[][] matrix) {
        int columns = requireRectangular(matrix);
        List<Integer> result = new ArrayList<>();
        if (matrix.length == 0 || columns == 0) {
            return result;
        }
        int top = 0;
        int bottom = matrix.length - 1;
        int left = 0;
        int right = columns - 1;
        while (top <= bottom && left <= right) {
            for (int col = left; col <= right; col++) {
                result.add(matrix[top][col]);
            }
            top++;
            for (int row = top; row <= bottom; row++) {
                result.add(matrix[row][right]);
            }
            right--;
            if (top <= bottom) {
                for (int col = right; col >= left; col--) {
                    result.add(matrix[bottom][col]);
                }
                bottom--;
            }
            if (left <= right) {
                for (int row = bottom; row >= top; row--) {
                    result.add(matrix[row][left]);
                }
                left++;
            }
        }
        return result;
    }

    public static int requireRectangular(int[][] matrix) {
        if (matrix == null) {
            throw new IllegalArgumentException("matrix must not be null");
        }
        if (matrix.length == 0) {
            return 0;
        }
        if (matrix[0] == null) {
            throw new IllegalArgumentException("rows must not be null");
        }
        int columns = matrix[0].length;
        for (int[] row : matrix) {
            if (row == null || row.length != columns) {
                throw new IllegalArgumentException("matrix must be rectangular");
            }
        }
        return columns;
    }

    private static void check(boolean condition) {
        if (!condition) {
            throw new AssertionError("check " + (checks + 1) + " failed");
        }
        checks++;
    }

    private static void expectIllegalArgument(Runnable action) {
        try {
            action.run();
            throw new AssertionError("expected IllegalArgumentException");
        } catch (IllegalArgumentException expected) {
            checks++;
        }
    }

    public static void main(String[] args) {
        check(forwardSum(new int[] {1, 2, 3}) == 6);                              // 1
        check(forwardSum(new int[0]) == 0);                                        // 2
        check(Arrays.equals(reversedCopy(new int[] {1, 2, 3}), new int[] {3, 2, 1})); // 3
        check(countEvenIndexes(new int[5]) == 3);                                  // 4
        check(firstIndex(new int[] {7, 4, 7}, 7) == 0);                            // 5
        check(isNondecreasing(new int[] {1, 1, 3}));                               // 6
        check(!isNondecreasing(new int[] {2, 1}));                                 // 7
        check(doWhileExecutions(0) == 1);                                          // 8
        check(forContinueValues(5, 2).equals(List.of(0, 1, 3, 4)));                // 9
        check(raggedSum(new int[][] {{1, 2}, null, {3}}) == 6);                    // 10

        int[] sorted = {1, 3, 5, 7, 9};
        check(lowerBound(sorted, 6) == 3);                                         // 11
        check(lowerBound(sorted, 0) == 0);                                         // 12
        check(lowerBound(sorted, 10) == 5);                                        // 13
        check(upperBound(new int[] {1, 2, 2, 2, 4}, 2) == 4);                      // 14
        check(upperBound(new int[] {1, 2, 2, 2, 4}, 2)
                - lowerBound(new int[] {1, 2, 2, 2, 4}, 2) == 3);                  // 15
        check(Arrays.equals(sortedTwoSum(new int[] {1, 3, 4, 7, 10}, 8),
                new int[] {0, 3}));                                                // 16
        check(Arrays.equals(sortedTwoSum(new int[] {1, 2}, 8),
                new int[] {-1, -1}));                                              // 17
        check(isPalindrome(new char[] {'l', 'e', 'v', 'e', 'l'}));                 // 18
        check(!isPalindrome(new char[] {'j', 'a', 'v', 'a'}));                     // 19

        int[] compacted = {2, 1, 2, 3, 2};
        int compactLength = compactRemoving(compacted, 2);
        check(compactLength == 2
                && Arrays.equals(Arrays.copyOf(compacted, compactLength),
                        new int[] {1, 3}));                                        // 20
        int[] duplicates = {1, 1, 2, 2, 2, 5};
        int uniqueLength = deduplicateSorted(duplicates);
        check(uniqueLength == 3
                && Arrays.equals(Arrays.copyOf(duplicates, uniqueLength),
                        new int[] {1, 2, 5}));                                     // 21
        int[] manyDuplicates = {1, 1, 1, 2, 2, 3};
        int atMostTwoLength = keepAtMostTwo(manyDuplicates);
        check(Arrays.equals(Arrays.copyOf(manyDuplicates, atMostTwoLength),
                new int[] {1, 1, 2, 2, 3}));                                      // 22
        check(Arrays.equals(mergeSorted(new int[] {1, 4}, new int[] {2, 3}),
                new int[] {1, 2, 3, 4}));                                         // 23
        check(distinctIntersection(new int[] {1, 1, 2, 4},
                new int[] {1, 3, 4, 4}).equals(List.of(1, 4)));                    // 24

        check(maxFixedWindowSum(new int[] {4, -1, 2, 10, -3}, 3) == 11);           // 25
        check(longestAtMostKDistinct(new int[] {1, 2, 1, 2, 3}, 2) == 4);          // 26
        check(longestAtMostKDistinct(new int[] {1, 2}, 0) == 0);                   // 27
        check(countAtMostKDistinct(new int[] {1, 2, 1}, 2) == 6);                  // 28
        check(countExactlyKDistinct(new int[] {1, 2, 1}, 2) == 3);                 // 29
        check(shortestAtLeastTarget(new int[] {2, 3, 1, 2, 4, 3}, 7) == 2);        // 30
        check(countPairsWithinDistance(new int[] {1, 2, 4, 7}, 3) == 4);           // 31

        check(flatten(2, 1, 3, 4) == 9);                                           // 32
        check(unflatten(9, 3, 4).equals(new Cell(2, 1)));                           // 33
        int[][] matrix = {{1, 2, 3}, {4, 5, 6}, {7, 8, 9}};
        check(rowMajor(matrix).equals(List.of(1, 2, 3, 4, 5, 6, 7, 8, 9)));        // 34
        check(columnMajor(matrix).equals(List.of(1, 4, 7, 2, 5, 8, 3, 6, 9)));    // 35
        check(mainDiagonal(matrix).equals(List.of(1, 5, 9)));                      // 36
        check(antiDiagonal(matrix).equals(List.of(3, 5, 7)));                      // 37
        check(neighbors4(matrix, 1, 1).equals(List.of(2, 6, 8, 4)));               // 38
        check(spiral(matrix).equals(List.of(1, 2, 3, 6, 9, 8, 7, 4, 5)));          // 39
        expectIllegalArgument(() -> requireRectangular(new int[][] {{1}, {2, 3}})); // 40

        if (checks != 40) {
            throw new AssertionError("expected 40 checks but found " + checks);
        }
        System.out.println("PASS 40 Loop Mastery checks");
    }
}
