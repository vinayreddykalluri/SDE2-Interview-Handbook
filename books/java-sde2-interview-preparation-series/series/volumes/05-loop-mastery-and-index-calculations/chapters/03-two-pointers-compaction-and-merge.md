# Two Pointers, Compaction, and Merge

“Two pointers” is not one algorithm. It is a state design: two indexes summarize a larger search or transformation. Each movement must be justified by an invariant such as sorted order, a retained prefix, or two already-sorted inputs.

## 3.1 Recognize the families

| Family | Typical start | Movement | Common jobs |
|---|---|---|---|
| opposing | `left = 0`, `right = n - 1` | inward | sorted pair, palindrome, partition |
| same direction | `read = 0`, `write = 0` | forward | remove, deduplicate, compact |
| two inputs | one pointer per input | advance smaller/chosen | merge, intersection |
| slow/fast | both near start | different speeds | cycle and midpoint; later volume |

Do not select the pattern because a problem contains an array. Select it because pointer movement safely eliminates work or maintains a meaningful region.

## 3.2 Opposing pointers on sorted data

Problem: in a sorted array, find two distinct values whose sum equals `target`.

```java
static int[] sortedTwoSum(int[] values, int target) {
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
```

![Sorted two-pointer elimination for target sum 8](series/volumes/05-loop-mastery-and-index-calculations/assets/03-opposing-two-pointers.png)

### Why movement is valid

Suppose `values[left] + values[right]` is too small. Pairing the same left value with any index below `right` cannot produce a larger sum. Therefore `left` cannot participate in a solution inside the current range; incrementing `left` eliminates an entire row of candidate pairs.

If the sum is too large, pairing `values[right]` with any index above `left` cannot make it smaller, so decrement `right`.

The proof depends on sorted order. On unsorted input these eliminations are invalid.

### Invariant and progress

- Any solution not yet eliminated lies within the current closed range `[left, right]`.
- `left < right` guarantees two distinct positions.
- Every unsuccessful iteration moves exactly one boundary inward.
- The width `right - left` strictly decreases, so time is `O(n)` and termination is guaranteed.

### Overflow safety

Cast before addition. `(long) (values[left] + values[right])` widens only after an `int` overflow. The correct expression is `(long) values[left] + values[right]`.

## 3.3 When sorting changes the contract

Sorting an unsorted array enables opposing pointers but may:

- cost `O(n log n)` time;
- mutate caller-owned input;
- destroy original indexes;
- reorder equal values.

If original indexes are required, a hash map may be a better solution, or sort `(value, originalIndex)` pairs. State the trade-off. “Use two pointers” is incomplete without the sortedness and ownership contract.

## 3.4 Palindrome comparison

```java
static boolean isPalindrome(char[] text) {
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
```

Invariant: every pair outside `[left, right]` has already matched. The remaining work shrinks by two positions per successful iteration. For a single middle character, `left == right`; it needs no comparison.

For Java `String`, decide whether the contract is UTF-16 code units, Unicode code points, normalized text, or user-perceived characters. The Strings volume develops that distinction.

## 3.5 Same-direction read/write compaction

Problem: remove all occurrences of a value in place and return the length of the retained prefix.

```java
static int compactRemoving(int[] values, int removedValue) {
    int write = 0;
    for (int read = 0; read < values.length; read++) {
        if (values[read] != removedValue) {
            values[write] = values[read];
            write++;
        }
    }
    return write;
}
```

![Read and write pointers preserving a compacted prefix](series/volumes/05-loop-mastery-and-index-calculations/assets/04-read-write-compaction.png)

### Pointer meanings

- `read` is the next input position to inspect.
- `write` is the next output position for a retained value.
- `[0, write)` contains exactly the retained values from `[0, read)`, in original order.

Because `write <= read`, writing never destroys unread data. After the loop, only `[0, write)` is valid output. Values in `[write, n)` are stale and unspecified.

### Dry run: remove 2 from `[2, 1, 2, 3, 2]`

| `read` | value | action | `write` after | valid prefix |
|---:|---:|---|---:|---|
| 0 | 2 | skip | 0 | `[]` |
| 1 | 1 | write at 0 | 1 | `[1]` |
| 2 | 2 | skip | 1 | `[1]` |
| 3 | 3 | write at 1 | 2 | `[1,3]` |
| 4 | 2 | skip | 2 | `[1,3]` |

The method is stable because it preserves retained order. It is `O(n)` time and `O(1)` auxiliary space.

## 3.6 Deduplicate a sorted array

```java
static int deduplicateSorted(int[] values) {
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
```

Here `[0, write)` contains unique sorted values. Compare against `values[write - 1]`, the last retained value, not blindly against a stale position. The empty-array guard is required before treating index 0 as retained.

For `[1,1,2,2,2,5]`, the returned length is 3 and the valid prefix is `[1,2,5]`.

### Keep at most two duplicates

The invariant can be generalized:

```java
static int keepAtMostTwo(int[] values) {
    int write = 0;
    for (int value : values) {
        if (write < 2 || value != values[write - 2]) {
            values[write++] = value;
        }
    }
    return write;
}
```

Sortedness makes `values[write - 2]` the value that would be violated by a third retained copy.

## 3.7 Stable partition with a write pointer

Move nonnegative values to a retained prefix while preserving their order:

```java
static int retainNonnegative(int[] values) {
    int write = 0;
    for (int read = 0; read < values.length; read++) {
        if (values[read] >= 0) {
            values[write++] = values[read];
        }
    }
    return write;
}
```

If both retained and rejected groups must remain in the array, stable partition generally needs additional storage or more expensive movement. An unstable opposing-pointer partition can be in place, but it may change order. State which property matters.

## 3.8 Merging two sorted arrays

```java
static int[] mergeSorted(int[] first, int[] second) {
    int[] merged = new int[first.length + second.length];
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
```

Invariant: `[0, write)` is the sorted merge of `first[0..left)` and `second[0..right)`. Choosing the smaller next value preserves sorted order. The remaining tail of either input is already sorted and can be copied directly.

Time is `O(first.length + second.length)`. Auxiliary space is `O(first.length + second.length)` because the returned array is newly allocated; if distinguishing output from auxiliary space, call it `O(1)` working state plus `O(n + m)` output.

### Stability

Using `<=` chooses from `first` when values tie, preserving the relative source preference. Changing it to `<` changes tie behavior. For plain integers this may not matter; for records with equal keys it can.

## 3.9 Intersection of sorted arrays

```java
static java.util.List<Integer> distinctIntersection(int[] a, int[] b) {
    java.util.List<Integer> result = new java.util.ArrayList<>();
    int i = 0;
    int j = 0;
    while (i < a.length && j < b.length) {
        if (a[i] < b[j]) {
            i++;
        } else if (a[i] > b[j]) {
            j++;
        } else {
            int value = a[i];
            result.add(value);
            while (i < a.length && a[i] == value) i++;
            while (j < b.length && b[j] == value) j++;
        }
    }
    return result;
}
```

Each pointer only moves forward, so total work is `O(n + m)` even though duplicate-skipping `while` loops are nested inside the main loop.

## 3.10 Three-way partition intuition

The Dutch National Flag pattern maintains regions such as:

```text
[0, low)       < pivot
[low, scan)    == pivot
[scan, high]   unknown
(high, n)      > pivot
```

Each branch classifies `values[scan]`. Swapping an unknown value from `high` into `scan` means `scan` must not advance until the incoming value is classified. This is a classic interview trap: pointer movement is determined by what the swap proved.

Full partition and sorting algorithms belong in the Arrays volume; here the lesson is to name every region.

## 3.11 Common failures

1. Applying opposing pointers to unsorted data without a proof.
2. Using `left <= right` when the solution needs two distinct positions.
3. Adding as `int` and casting after overflow.
4. Returning the whole compacted array instead of its valid prefix length.
5. Reading from overwritten data because `write > read` was allowed.
6. Forgetting empty input before initializing `write = 1`.
7. Advancing both merge pointers when only one value was consumed.
8. Claiming stable partition when swaps changed order.
9. Mutating caller input without documenting ownership.
10. Treating nested duplicate-skipping loops as automatically quadratic.

## 3.12 Interview explanation template

For any two-pointer solution, say:

1. what each pointer means;
2. what regions are processed, retained, or unknown;
3. which property justifies each movement;
4. why movement cannot skip a solution;
5. why every pointer has bounded total movement;
6. what mutation, stability, and overflow contracts apply.

The next chapter extends the same discipline to contiguous windows that maintain reusable state.
