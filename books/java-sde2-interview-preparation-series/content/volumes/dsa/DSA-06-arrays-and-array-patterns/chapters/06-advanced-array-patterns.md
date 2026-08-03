# Advanced Array Patterns from Constraints

“Advanced” should not mean a bag of tricks. Each pattern in this chapter follows from an input constraint or an invariant. In an interview, explain that reason before writing the optimized loop.

## Sort, Then Scan: Merging Intervals

Unordered intervals are difficult because any later interval might overlap the current one. Sorting by start time creates a useful invariant: after sorting, if the next start is beyond the current end, no still-later interval can overlap the current interval.

```java
static int[][] mergeIntervals(int[][] intervals) {
    if (intervals.length == 0) {
        return new int[0][];
    }

    int[][] sorted = java.util.Arrays.stream(intervals)
            .map(int[]::clone)
            .toArray(int[][]::new);
    java.util.Arrays.sort(sorted,
            java.util.Comparator.comparingInt(interval -> interval[0]));

    java.util.List<int[]> merged = new java.util.ArrayList<>();
    int start = sorted[0][0];
    int end = sorted[0][1];

    for (int index = 1; index < sorted.length; index++) {
        int[] next = sorted[index];
        if (next[0] <= end) { // closed intervals; touching endpoints overlap
            end = Math.max(end, next[1]);
        } else {
            merged.add(new int[] {start, end});
            start = next[0];
            end = next[1];
        }
    }
    merged.add(new int[] {start, end});
    return merged.toArray(int[][]::new);
}
```

Copying before sorting preserves caller-owned order. Ask whether intervals are closed, half-open, or open; `next[0] <= end` is correct only for the stated closed-interval contract.

## Cyclic Placement: The Index Is the Destination

If an array of length `n` contains values from `1` through `n`, value `v` has a natural home at index `v - 1`. Swap until the current value is either at home or a duplicate blocks it.

![Cyclic placement moves each bounded value toward its natural index](content/volumes/dsa/DSA-06-arrays-and-array-patterns/assets/08-cyclic-placement.png)

```java
static int firstMissingPositive(int[] input) {
    int[] numbers = input.clone();

    for (int index = 0; index < numbers.length;) {
        int value = numbers[index];
        int destination = value - 1;

        if (value > 0 && value <= numbers.length
                && numbers[destination] != value) {
            int temporary = numbers[index];
            numbers[index] = numbers[destination];
            numbers[destination] = temporary;
        } else {
            index++;
        }
    }

    for (int index = 0; index < numbers.length; index++) {
        if (numbers[index] != index + 1) {
            return index + 1;
        }
    }
    return numbers.length + 1;
}
```

Why is the loop linear despite its nested-looking swaps? A successful swap places at least one value into its final slot. There are at most `n` such placements. The duplicate check prevents an infinite swap between equal values.

This method mutates its working array. The clone makes the public behavior non-mutating at `O(n)` extra space. If the interviewer requires constant auxiliary space, ask permission to mutate the input.

## Sign Marking: Borrowing a Bit of State

With values restricted to `1..n`, the sign of slot `abs(value) - 1` can mark whether a value was seen.

```java
static java.util.List<Integer> findDuplicates(int[] input) {
    int[] numbers = input.clone();
    java.util.List<Integer> duplicates = new java.util.ArrayList<>();

    for (int value : numbers) {
        int index = Math.abs(value) - 1;
        if (numbers[index] < 0) {
            duplicates.add(Math.abs(value));
        } else {
            numbers[index] = -numbers[index];
        }
    }
    return duplicates;
}
```

This is not universally safe. It needs a bounded positive domain, mutable working storage, and values for which `Math.abs` is valid. `Math.abs(Integer.MIN_VALUE)` is still negative. State the preconditions or validate them.

## Matrix Rotation: Transpose, Then Reverse Rows

For an `n x n` matrix, a clockwise rotation maps `(row, column)` to `(column, n - 1 - row)`. Performing every destination assignment directly needs separate storage or careful cycle management. A clearer in-place decomposition is:

1. transpose across the main diagonal;
2. reverse each row.

![A clockwise matrix rotation decomposed into transpose and row reversal](content/volumes/dsa/DSA-06-arrays-and-array-patterns/assets/09-matrix-rotation.png)

```java
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
        for (int left = 0, right = row.length - 1; left < right; left++, right--) {
            int temporary = row[left];
            row[left] = row[right];
            row[right] = temporary;
        }
    }
}
```

The algorithm is `O(n^2)` time because every matrix cell matters and `O(1)` auxiliary space. It is invalid for a jagged or rectangular non-square array.

## Spiral Traversal: Shrinking Boundaries

Maintain `top`, `bottom`, `left`, and `right`. After traversing an edge, shrink its boundary. Before traversing the bottom or left edge, verify that the remaining region still exists; otherwise a single row or column is visited twice.

The Loop Mastery volume develops boundary tracing and index calculations in more detail. Here, remember the invariant: **unvisited cells are exactly the rectangle bounded by the four variables**.

## Pattern Composition

SDE-2 questions frequently combine techniques:

- sort intervals, then scan and merge;
- prefix sums plus a frequency map;
- binary search over an answer, with an array scan as the feasibility test;
- monotonic stack over indexes to find next greater boundaries;
- two pointers inside an outer fixed index for three-sum;
- coordinate compression followed by a Fenwick tree.

Composition is safe only when you can state the cost of each layer. Sorting plus a linear scan is `O(n log n)`, not `O(n)`. A linear feasibility check performed `O(log R)` times is `O(n log R)`.

## A Pattern Decision Map

![Questions that guide array pattern selection](content/volumes/dsa/DSA-06-arrays-and-array-patterns/assets/10-pattern-decision-map.png)

| Signal | First candidate | Proof obligation |
|---|---|---|
| Sorted data, pair/range | Two pointers or binary search | Which direction can be eliminated? |
| Fixed contiguous length | Fixed window | How is overlap reused? |
| Positive values, threshold range | Variable window | Why are boundary moves monotonic? |
| Many immutable range queries | Prefix state | What combines and can it be inverted? |
| Bounded values map to indexes | Cyclic/sign marking | Are mutation and domain constraints allowed? |
| Intervals | Sort and scan | What does overlap mean at endpoints? |
| Nearest greater/smaller | Monotonic stack | What does the stack preserve? |

## Common Failures

- Applying cyclic placement without checking the value range.
- Claiming “constant space” while silently cloning the input.
- Mutating caller-owned interval rows during sorting or merging.
- Using comparator subtraction, such as `a[0] - b[0]`, which can overflow.
- Rotating a non-square matrix with the square-matrix formula.
- Reporting only the inner scan cost and omitting sorting or binary-search iterations.

## Quick Check and Practice

1. **Foundation:** Why does sorting make interval merging possible with one scan?
2. **Interview Core:** What prevents cyclic placement from swapping duplicates forever?
3. **Interview Core:** List the preconditions required by sign marking.
4. **Interview Core:** Dry-run clockwise rotation of a `2 x 2` matrix.
5. **Interview Core:** Implement spiral traversal without duplicating a single row.
6. **SDE-2 Follow-up:** Merge intervals while preserving every original input array.
7. **SDE-2 Follow-up:** Compare cyclic placement, a boolean array, and a hash set for finding a missing bounded value.
8. **SDE-2 Follow-up:** Design tests that would expose comparator overflow and endpoint-semantics bugs.

## Cross-Book Boundaries

This volume teaches the array mechanics and recognition signals. Full binary-search templates belong in Binary Search; monotonic stacks in Stacks; frequency-map internals in Hashing and Collections Internals; loop geometry in Loop Mastery; and numeric overflow/base behavior in Number Systems.
