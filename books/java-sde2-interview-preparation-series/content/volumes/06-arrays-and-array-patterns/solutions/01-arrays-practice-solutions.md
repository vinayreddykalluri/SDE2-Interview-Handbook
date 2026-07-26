# Arrays Practice Solutions and Reasoning

Use these as review notes after attempting the lab. Equivalent implementations are valid when their contracts and complexity are equally clear.

## A. Knowledge-Check Answers

1. An `int[]` slot stores an `int` value; a `Student[]` slot stores a reference value or `null`.
2. The defaults are `0`, `false`, and `null`. These defaults apply to array elements, not uninitialized local variables.
3. Element assignments change contents, but the array object's length cannot change. Resizing means allocating a different array and copying.
4. Valid indexes are `[0, length)`: zero is included, `length` is excluded.
5. Physical capacity is the number of slots. Logical size is an application-level count of meaningful elements.
6. The enhanced loop variable receives a copy of each element value. Reassigning that local does not address the original slot.
7. Java passes a copy of the array reference value. The method can mutate the reached array but reassigning its parameter does not reassign the caller's variable.
8. Assignment can copy the same reference value into two variables; both then reach one mutable object.
9. Assignment aliases. `clone` and `Arrays.copyOf` allocate a new one-dimensional array; `copyOf` can change length. `System.arraycopy` copies a selected region into an existing destination. Nested references remain shared unless copied recursively.
10. The new outer array contains the same row references as the original.
11. `equals` compares one-dimensional element values; for nested arrays those elements are row references. `deepEquals` recursively compares nested array contents.
12. Before each iteration, `[0, write)` contains exactly the accepted values from the processed input prefix, in stable order.
13. Sorted order tells which pointer movement can discard impossible pairs.
14. A variable window needs a monotonic relationship between expansion/shrinking and the condition—positivity provides it for sum thresholds.
15. Zero silently represents an empty subarray and defeats all-negative inputs.
16. `prefix[i]` summarizes `[0, i)`; with sums, `prefix[0]` is zero.
17. It becomes stale after the source values change, unless the structure is updated too.
18. For inclusive `[left, right]`, the stop marker is `right + 1`, which can equal `length`.
19. Values must map into a bounded set of destination indexes; mutation or a working copy must be allowed.
20. Sign marking borrows slot signs and therefore needs a suitable positive bounded domain and mutable storage.
21. Subtraction can overflow and reverse comparator ordering. Use `Integer.compare` or `Long.compare`.
22. Auxiliary space supports computation; output space holds required results. Report both when it clarifies the trade-off.
23. Each pointer moves in only one direction and at most `n` positions, so total movement is linear.
24. `int[]` avoids boxing, has fixed dense storage, and gives direct index access. `ArrayList<Integer>` is useful when logical size changes and collection APIs matter.

## B. Predicted Outputs

1. `[1, 9, 3]` — both variables reference the same array.
2. `1` — the parameter was reassigned, not the caller's variable.
3. `8 false` — outer arrays differ, but their row references are shared.
4. `false true` — identities differ while contents match.
5. `1410065408 10000000000` — the first multiplication overflows as `int`; the cast promotes before multiplication.
6. `[1, 2, 3]` — the loop variable is only a local copy.
7. `6` — the query is `[1, 4)`, or `-1 + 5 + 2` for the underlying values.
8. `-1` — non-empty Kadane initialization handles all-negative input.
9. `3 3` — three rows; the second row has length three.
10. `1 3` — the list contains one element, which is the complete primitive array.
11. `2` then `-3` — `6` is at index two; `5` would be inserted at index two, encoded as `-(2)-1`.
12. `1 15` — postfix increment uses index zero, then increments it.

## C. Debugging Repairs

```java
// C1
for (int index = 0; index < values.length; index++) { }

// C2
System.out.println(java.util.Arrays.toString(values));

// C3
return input.clone();

// C4
for (int index = values.length - 1; index >= 0; index--) { }

// C5
long sum = (long) values[left] + values[right];

// C7: right is the entering index; right - k is leaving
windowSum += values[right] - values[right - k];

// C8
long ending = values[0];
long best = values[0];

// C9
difference[right + 1] -= delta;

// C10
if (value >= 1 && value <= values.length
        && values[value - 1] != value) {
    swap(values, index, value - 1);
} else {
    index++;
}

// C11
java.util.Arrays.sort(intervals,
        (first, second) -> Integer.compare(first[0], second[0]));
```

For C6, increment `write` only after accepting and assigning a value. For C12, make a complete validation pass first—check every row is non-null and has length `matrix.length`—then begin the transpose. Validation after mutation can leave a partially changed input.

## D. Coding Reference Points

### D2 — Reverse a Half-Open Range

```java
static void reverse(int[] values, int left, int rightExclusive) {
    if (left < 0 || left > rightExclusive || rightExclusive > values.length) {
        throw new IndexOutOfBoundsException();
    }
    for (int right = rightExclusive - 1; left < right; left++, right--) {
        int temporary = values[left];
        values[left] = values[right];
        values[right] = temporary;
    }
}
```

The unprocessed region is `[left, right]`; positions outside it already contain their final mirrored values. Time is proportional to the range length and auxiliary space is constant.

### D3 — Jagged Deep Copy

```java
static int[][] deepCopy(int[][] matrix) {
    int[][] copy = new int[matrix.length][];
    for (int row = 0; row < matrix.length; row++) {
        copy[row] = matrix[row] == null ? null : matrix[row].clone();
    }
    return copy;
}
```

### D5 — Stable Removal

```java
static int remove(int[] values, int target) {
    int write = 0;
    for (int value : values) {
        if (value != target) {
            values[write++] = value;
        }
    }
    return write;
}
```

Only `[0, write)` is meaningful afterward. Clearing the unused suffix is optional for primitives; for object arrays, clearing may release references sooner.

### D6 — At Most Two Duplicates

```java
static int keepAtMostTwo(int[] sorted) {
    int write = 0;
    for (int value : sorted) {
        if (write < 2 || value != sorted[write - 2]) {
            sorted[write++] = value;
        }
    }
    return write;
}
```

### D7 — Unsorted Two-Sum Indexes

```java
static int[] twoSum(int[] values, long target) {
    java.util.Map<Long, Integer> indexByValue = new java.util.HashMap<>();
    for (int index = 0; index < values.length; index++) {
        long needed = target - values[index];
        Integer other = indexByValue.get(needed);
        if (other != null) {
            return new int[] {other, index};
        }
        indexByValue.putIfAbsent((long) values[index], index);
    }
    return new int[0];
}
```

Lookup is expected constant time under ordinary hash behavior, not an unconditional worst-case guarantee. Space is linear.

### D12 — Immutable Range Query

```java
final class RangeSumQuery {
    private final long[] prefix;

    RangeSumQuery(int[] values) {
        prefix = new long[values.length + 1];
        for (int index = 0; index < values.length; index++) {
            prefix[index + 1] = prefix[index] + values[index];
        }
    }

    long sum(int left, int rightExclusive) {
        if (left < 0 || left > rightExclusive || rightExclusive >= prefix.length) {
            throw new IndexOutOfBoundsException();
        }
        return prefix[rightExclusive] - prefix[left];
    }
}
```

The object stores derived state, not the caller's mutable array. Construction is linear; queries are constant time.

### D18 — Differential Test Shape

Generate small arrays, compute the quadratic baseline and optimized answer, and fail with the complete input if answers differ. Use a fixed random seed for reproducibility and include all-negative, empty-policy, and extreme-value cases separately. Random testing supplements proofs and targeted tests; it does not replace them.

## E. Follow-Up Guidance

1. Sorted immutable input permits two pointers without mutation. If original indexes from a differently ordered input are required, preserve index metadata.
2. Streaming permits summaries such as Kadane state and some fixed windows if only the current window must be retained. Arbitrary backward access and sorting do not.
3. Expose an immutable prefix-backed object with validated half-open queries and document construction versus query cost.
4. Difference arrays defer materialization and cannot answer interleaved current-state queries efficiently; consider Fenwick or segment trees.
5. Negative values break the monotonic link between removing a left value and decreasing a sum.
6. Keep per-call arrays, indexes, maps, and accumulators local; share only immutable snapshots or deliberately synchronized state.
7. Five hundred million `int` values require roughly two billion bytes for primitive payload alone, before headers and other memory. Avoid boxing and unnecessary clones; process sequentially for locality or use chunked/external strategies.
8. Clarify whether input mutation is permitted, whether output storage counts, whether recursion or library sorting allocates, and whether a shallow copy is acceptable.

## F. Assessment Rubrics

### Assessment 1

A strong solution uses stable read/write compaction, returns logical size, states that the original array is mutated, and uses `IntPredicate` without boxing each element. It tests remove-none, remove-all, alternating, empty, and repeated calls. The invariant describes the accepted processed prefix.

### Assessment 2

Use immutable prefix sums for range queries, prefix-frequency hashing for target counts with signed values, and Kadane for the best non-empty range. Explain why one “range technique” does not replace all three: their query requirements and reusable state differ.

### Assessment 3

Expected starting points are opposing pointers, cyclic placement or marking, sort-and-scan, monotonic stack, and Fenwick/segment tree. Full credit requires the property that makes each movement or state update valid.

## Final Readiness Assessment Rubric

A publishable solution first defines closed intervals and touching behavior, validates each row, clones input rows, sorts with `Integer.compare`, merges in one scan, clips using `max(start, queryStart)` and `min(end, queryEnd)`, and adds covered length in `long`. It distinguishes inclusive integer-point counts from continuous interval lengths—these are not the same contract. It reports `O(n log n)` time and `O(n)` space because preservation requires copied rows and output. Tests cover no intervals, disjoint/touching/nested intervals, reversed invalid intervals, extreme endpoints, complete clipping, and no overlap.
