# Essential Advanced Binary-Search Pattern Clinics

Most binary-search questions search an index or a monotone answer. Two advanced variants search a partition or an ordered numeric domain that is not materialized as an array.

## Clinic 1: median of two sorted arrays

### Partition instead of merge

Partition both arrays so the combined left side contains half the elements. Binary-search only the shorter array. If its cut is `cutA`, the other cut is determined:

```text
cutB = (lengthA + lengthB + 1) / 2 - cutA
```

The partition is valid when:

```text
leftA <= rightB and leftB <= rightA
```

If `leftA > rightB`, cut A too far right. Otherwise `leftB > rightA`, so cut A too far left. Sentinels represent missing values at array boundaries.

For odd total length, the median is the maximum left value. For even length, average the maximum left and minimum right values using `long` before addition.

### Why the shorter array matters

Searching the shorter array keeps `cutB` within valid bounds and gives O(log(min(m,n))) time. The input arrays are not modified, and auxiliary space is O(1).

## Clinic 2: kth smallest in a row-and-column sorted matrix

The matrix values define an ordered answer domain even though flattening it would destroy the desired memory bound. Binary-search value `x` using this monotone predicate:

```text
count(values <= x) >= k
```

Count in O(rows + columns) by starting at the bottom-left. If the current value is at most `x`, every value above it in that column also qualifies; add `row + 1` and move right. Otherwise move up.

Duplicates are handled naturally because the predicate counts occurrences. The total complexity is O((rows + columns) log(valueRange)), which should be compared with a heap solution when the numeric range is very wide or k is small.

## Runnable Java 21 clinic

```java
import java.util.Objects;

public final class BinarySearchCoverageClinic {
    private BinarySearchCoverageClinic() {
    }

    public static double medianOfTwoSortedArrays(int[] first, int[] second) {
        Objects.requireNonNull(first, "first");
        Objects.requireNonNull(second, "second");
        if (first.length + second.length == 0) {
            throw new IllegalArgumentException("at least one value is required");
        }
        if (first.length > second.length) {
            return medianOfTwoSortedArrays(second, first);
        }

        int low = 0;
        int high = first.length;
        while (low <= high) {
            int cutFirst = low + (high - low) / 2;
            int cutSecond = (first.length + second.length + 1) / 2 - cutFirst;

            int leftFirst = cutFirst == 0 ? Integer.MIN_VALUE : first[cutFirst - 1];
            int rightFirst = cutFirst == first.length
                    ? Integer.MAX_VALUE : first[cutFirst];
            int leftSecond = cutSecond == 0 ? Integer.MIN_VALUE : second[cutSecond - 1];
            int rightSecond = cutSecond == second.length
                    ? Integer.MAX_VALUE : second[cutSecond];

            if (leftFirst <= rightSecond && leftSecond <= rightFirst) {
                int bestLeft = Math.max(leftFirst, leftSecond);
                if ((first.length + second.length) % 2 == 1) {
                    return bestLeft;
                }
                int bestRight = Math.min(rightFirst, rightSecond);
                return ((long) bestLeft + bestRight) / 2.0;
            }
            if (leftFirst > rightSecond) {
                high = cutFirst - 1;
            } else {
                low = cutFirst + 1;
            }
        }
        throw new IllegalArgumentException("inputs must be sorted");
    }

    public static int kthSmallestInSortedMatrix(int[][] matrix, int k) {
        int columns = validateRectangular(matrix);
        long total = (long) matrix.length * columns;
        if (k < 1 || k > total) {
            throw new IllegalArgumentException("k is outside the matrix");
        }

        long low = matrix[0][0];
        long high = matrix[matrix.length - 1][columns - 1];
        while (low < high) {
            long middle = low + (high - low) / 2;
            if (countAtMost(matrix, columns, middle) >= k) {
                high = middle;
            } else {
                low = middle + 1;
            }
        }
        return (int) low;
    }

    private static long countAtMost(int[][] matrix, int columns, long target) {
        int row = matrix.length - 1;
        int column = 0;
        long count = 0;
        while (row >= 0 && column < columns) {
            if (matrix[row][column] <= target) {
                count += row + 1L;
                column++;
            } else {
                row--;
            }
        }
        return count;
    }

    private static int validateRectangular(int[][] matrix) {
        if (matrix == null || matrix.length == 0 || matrix[0] == null
                || matrix[0].length == 0) {
            throw new IllegalArgumentException("matrix must be nonempty");
        }
        int columns = matrix[0].length;
        for (int[] row : matrix) {
            if (row == null || row.length != columns) {
                throw new IllegalArgumentException("matrix must be rectangular");
            }
        }
        return columns;
    }

    public static void main(String[] args) {
        assert medianOfTwoSortedArrays(new int[] {1, 3}, new int[] {2}) == 2.0;
        assert medianOfTwoSortedArrays(new int[] {1, 2}, new int[] {3, 4}) == 2.5;
        int[][] matrix = {{1, 5, 9}, {10, 11, 13}, {12, 13, 15}};
        assert kthSmallestInSortedMatrix(matrix, 8) == 13;
        System.out.println("PASS essential binary-search clinics");
    }
}
```

Expected output with assertions enabled:

```text
PASS essential binary-search clinics
```

## Interviewer follow-up chain with model answers

**Interviewer:** Why is merging not the target solution for the median problem?

**Candidate:** Merge is correct and a good baseline, but it costs O(m+n) time. The sorted boundary condition lets us search a partition in O(log(min(m,n))) time and O(1) space.

**Interviewer:** Does the matrix algorithm require all values to be distinct?

**Candidate:** No. It counts occurrences less than or equal to the candidate, so duplicate values occupy multiple ranks correctly.
