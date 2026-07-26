# Prefix, Suffix, and Range State

A range problem often repeats work. Prefix and suffix techniques precompute information once so that later queries or combinations are cheap. The trade-off is deliberate: extra memory and preprocessing in exchange for faster repeated work.

## Prefix Sums with a Sentinel

For `numbers = [3, -1, 4, 2]`, define:

```text
prefix[0] = 0
prefix[1] = 3
prefix[2] = 2
prefix[3] = 6
prefix[4] = 8
```

`prefix[i]` stores the sum of the half-open range `[0, i)`. Therefore, the sum of `[left, right)` is `prefix[right] - prefix[left]`.

```java
static long[] buildPrefixSums(int[] numbers) {
    long[] prefix = new long[numbers.length + 1];
    for (int index = 0; index < numbers.length; index++) {
        prefix[index + 1] = prefix[index] + numbers[index];
    }
    return prefix;
}

static long rangeSum(long[] prefix, int left, int rightExclusive) {
    if (left < 0 || left > rightExclusive || rightExclusive >= prefix.length) {
        throw new IndexOutOfBoundsException("invalid half-open range");
    }
    return prefix[rightExclusive] - prefix[left];
}
```

The leading zero removes the special case for a range beginning at index `0`. Building costs `O(n)` time and `O(n)` space; each query costs `O(1)`.

![Prefix state and difference-state boundaries](content/volumes/06-arrays-and-array-patterns/assets/07-prefix-and-difference-state.png)

### Snapshot Semantics

A prefix array describes the input **at build time**. If the original array changes, the prefix sums become stale. Say this explicitly when designing an API. Frequent online updates suggest a Fenwick tree or segment tree, covered in later data-structure material.

## Prefix and Suffix Products

“Product of array except self” is a classic example of combining left and right state without division.

```java
static long[] productExceptSelf(int[] numbers) {
    long[] result = new long[numbers.length];

    long prefixProduct = 1;
    for (int index = 0; index < numbers.length; index++) {
        result[index] = prefixProduct;
        prefixProduct *= numbers[index];
    }

    long suffixProduct = 1;
    for (int index = numbers.length - 1; index >= 0; index--) {
        result[index] *= suffixProduct;
        suffixProduct *= numbers[index];
    }
    return result;
}
```

At the beginning of the second loop, `result[i]` is the product strictly to the left of `i`; `suffixProduct` is the product strictly to its right. Zeros need no special branch. Overflow still depends on constraints—using `long` increases the range but does not make multiplication unbounded.

## Difference Arrays for Offline Range Updates

Suppose several operations add a value to every index in an inclusive range `[left, right]`. Updating each cell costs work proportional to the range length. Instead, record only where the effect starts and stops.

```java
record RangeAdd(int left, int rightInclusive, int delta) {}

static long[] applyRangeAdds(int length, java.util.List<RangeAdd> updates) {
    long[] difference = new long[length + 1];

    for (RangeAdd update : updates) {
        if (update.left() < 0 || update.left() > update.rightInclusive()
                || update.rightInclusive() >= length) {
            throw new IndexOutOfBoundsException("invalid update range");
        }
        difference[update.left()] += update.delta();
        difference[update.rightInclusive() + 1] -= update.delta();
    }

    long[] result = new long[length];
    long activeDelta = 0;
    for (int index = 0; index < length; index++) {
        activeDelta += difference[index];
        result[index] = activeDelta;
    }
    return result;
}
```

For `m` updates over length `n`, the total cost is `O(m + n)`. This works when updates can be collected before final values are required. It does not support arbitrary online queries by itself.

## Two-Dimensional Prefix Sums

For a matrix, add a zero row and zero column. Let `prefix[row][column]` contain the sum of the rectangle from the origin through the cells before those boundaries.

```java
static long[][] buildPrefix2D(int[][] matrix) {
    if (matrix.length == 0) {
        return new long[1][1];
    }
    int columns = matrix[0].length;
    long[][] prefix = new long[matrix.length + 1][columns + 1];

    for (int row = 0; row < matrix.length; row++) {
        if (matrix[row].length != columns) {
            throw new IllegalArgumentException("rectangular matrix required");
        }
        for (int column = 0; column < columns; column++) {
            prefix[row + 1][column + 1] = matrix[row][column]
                    + prefix[row][column + 1]
                    + prefix[row + 1][column]
                    - prefix[row][column];
        }
    }
    return prefix;
}
```

For rectangle `[top, bottom) x [left, right)`, inclusion-exclusion gives:

```java
static long rectangleSum(
        long[][] prefix, int top, int left, int bottom, int right) {
    return prefix[bottom][right]
            - prefix[top][right]
            - prefix[bottom][left]
            + prefix[top][left];
}
```

Why add the overlap back? Subtracting the area above and the area left removes the top-left intersection twice.

## Prefix State Is More Than a Sum

The same structure can store other associative state:

- prefix XOR for range XOR queries;
- prefix counts for character or category frequencies;
- prefix minimum only for one-sided queries, because minimum has no inverse;
- prefix balance for equal-category or parentheses-style reasoning.

Do not mechanically use `rightState - leftState`. Subtraction works for sums because addition has an inverse. Each operator needs its own query rule.

## Choosing Between Range Techniques

| Workload | Useful starting point | Reason |
|---|---|---|
| One pass, one best range | Window or Kadane-style state | No need to store every prefix |
| Many immutable sum queries | Prefix sums | One preprocessing pass, constant-time query |
| Many offline range additions | Difference array | Store only update boundaries |
| Online point updates and range queries | Fenwick/segment tree | Prefix snapshot would become stale |
| Arbitrary signed subarray target | Prefix state + hash map | Earlier matching state is not adjacent |

## Common Failures

1. Mixing inclusive and exclusive endpoints in the same formula.
2. Allocating `prefix.length == numbers.length` and then adding special cases for index zero.
3. Building in `int` and assigning the already-overflowed result to `long`.
4. Forgetting the `right + 1` stop marker in a difference array.
5. Assuming every Java `int[][]` is rectangular.
6. Reusing a prefix snapshot after mutating the source.

## Quick Check and Practice

1. **Foundation:** What does `prefix[i]` mean in the sentinel design?
2. **Foundation:** Derive the formula for sum of `[left, right)`.
3. **Interview Core:** Why does product-except-self work with zeros?
4. **Interview Core:** Apply updates `(1, 3, +2)` and `(2, 4, -1)` to an array of five zeros by hand.
5. **Interview Core:** Explain the four terms in a two-dimensional rectangle query.
6. **SDE-2 Follow-up:** Design an immutable `RangeSumQuery` class whose constructor defensively copies its input.
7. **SDE-2 Follow-up:** The input receives 100,000 updates and 100,000 interleaved queries. Explain why a difference array is insufficient and name the next data structure to consider.

## Transition

Range state handles repeated aggregation. The next chapter studies patterns that use array indexes as structure: interval sweeps, cyclic placement, sign marking, and in-place matrix transformations.
