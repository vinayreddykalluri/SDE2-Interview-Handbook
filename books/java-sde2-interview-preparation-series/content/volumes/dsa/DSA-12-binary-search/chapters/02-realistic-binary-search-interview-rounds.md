# Realistic Binary Search Interview Rounds

## Round 1: first and last position

### Prompt

In a sorted array, return the first and last index of `target`, or `[-1, -1]` when absent.

### Candidate answer

Derive both from boundaries instead of modifying an exact-search loop. `first = lowerBound(target)`. `after = upperBound(target)`. The target is absent when `first == length` or `sorted[first] != target`.

```java
static int[] equalRange(int[] sorted, int target) {
    int first = lowerBound(sorted, target);
    if (first == sorted.length || sorted[first] != target) {
        return new int[] {-1, -1};
    }
    return new int[] {first, upperBound(sorted, target) - 1};
}
```

**Why not search left and then scan?** Scanning can become O(n) when many duplicates exist. Two boundary searches remain O(log n).

**Overflow in `target + 1`?** Do not implement upper bound by lower-bounding `target + 1`; `Integer.MAX_VALUE + 1` wraps. Implement the strict comparison directly.

## Round 2: search a rotated sorted array

### Prompt

Search a rotated array of distinct integers in O(log n).

### Model reasoning

At least one half around the middle is normally sorted. Determine which half, then decide whether target lies inside its value range.

```java
static int searchRotated(int[] values, int target) {
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
```

### Follow-up answers

**What if duplicates are allowed?** When `values[left] == values[middle] == values[right]`, sorted-half identification can be ambiguous. Shrinking both ends preserves correctness but can degrade worst-case time to O(n).

**What invariant remains?** If target exists, it remains in the closed candidate interval. The sorted-half test justifies which contiguous side can be removed.

## Round 3: minimum ship capacity

### Prompt

Packages with positive integer weights must be shipped in order within `days`. Return the minimum daily capacity.

### Clarification and bounds

Capacity cannot be smaller than the heaviest package and never needs to exceed the total weight. Feasibility is monotone: if capacity `c` works, every larger capacity works.

```java
static long minimumCapacity(int[] weights, int days) {
    if (weights.length == 0 || days <= 0) {
        throw new IllegalArgumentException("invalid input");
    }
    long left = 0L;
    long right = 0L;
    for (int weight : weights) {
        if (weight <= 0) {
            throw new IllegalArgumentException("weights must be positive");
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

static boolean canShip(int[] weights, int days, long capacity) {
    int usedDays = 1;
    long load = 0L;
    for (int weight : weights) {
        if (load + weight > capacity) {
            usedDays++;
            load = 0L;
        }
        load += weight;
        if (usedDays > days) {
            return false;
        }
    }
    return true;
}
```

### Follow-up answers

**Why `long`?** The total of valid positive `int` weights can overflow `int`.

**Why greedily fill each day?** For fixed capacity and order, delaying a package cannot reduce the number of days. Filling until the next package no longer fits minimizes days for that capacity.

**Complexity?** O(n log S), where `S` is the candidate capacity range from maximum weight to total weight; O(1) auxiliary space.

## Closing answer pattern

State interval semantics, boundary meaning, monotonicity proof, midpoint bias, progress proof, empty/duplicate behavior, numeric width, and predicate cost.
