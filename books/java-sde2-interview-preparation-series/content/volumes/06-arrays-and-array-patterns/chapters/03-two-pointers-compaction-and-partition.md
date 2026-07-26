# Two Pointers, Compaction, and Partition

"Two pointers" is not one algorithm. It is a family of proofs in which two indexes describe different roles or boundaries. Use it when movement can safely eliminate candidates, when a filtered result can reuse the input prefix, or when values must be classified into regions.

## 3.1 Recognition before code

| Signal | Pointer roles | Required reason |
|---|---|---|
| sorted pair or comparison | opposing `left` and `right` | ordering proves which side can be discarded |
| filtering or deduplication | `read` and `write` | `write <= read` protects unread input |
| merging sorted inputs | one pointer per input | smaller head is the next output |
| three categories | `low`, `scan`, `high` | every position belongs to a defined region |

Two indexes alone do not guarantee linear time. State whether each pointer only moves forward or inward and how often it can move over the complete run.

## 3.2 Opposing pointers on sorted data

Given a nondecreasing array and a target, compare the smallest and largest remaining candidates:

```java
static int[] twoSumSorted(int[] values, int target) {
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

![Sorted two-pointer elimination removes one impossible boundary at a time](content/volumes/06-arrays-and-array-patterns/assets/03-opposing-two-pointer-elimination.png)

### Why movement is safe

If `values[left] + values[right]` is smaller than the target, pairing the same smallest value with any index no larger than `right` cannot increase the sum enough. Therefore the current `left` cannot participate in a solution inside the remaining interval and can be discarded. The symmetric argument justifies decrementing `right` when the sum is too large.

The cast occurs before addition. Assigning an already-overflowed `int` sum to `long` would be too late.

### Contract choice

Sorting an unsorted array first changes complexity to `O(n log n)`, mutates unless copied, and loses original indexes unless values are decorated with positions. A hash map can solve unsorted two sum in expected `O(n)` time with `O(n)` storage. Choose from the required output and ownership contract, not from pattern familiarity.

## 3.3 Read/write compaction

For filtering, `read` visits every original element while `write` marks the next retained slot:

```java
static int removeValue(int[] values, int removed) {
    int write = 0;
    for (int read = 0; read < values.length; read++) {
        if (values[read] != removed) {
            values[write++] = values[read];
        }
    }
    return write;
}
```

![Read scans the input while write grows the valid output prefix](content/volumes/06-arrays-and-array-patterns/assets/04-read-write-compaction.png)

Invariant before each read:

- `[0, write)` contains exactly the retained values from `[0, read)`, in original order;
- `[read, n)` has not been classified;
- `write <= read`, so writing cannot destroy an unread value.

At termination, `[0, write)` is the logical output. The suffix `[write, n)` is unspecified. Returning the new length is part of the contract; returning the whole array as though every slot were valid is a common error.

### Dry run: remove 2

Input `[2,1,2,3,2]`:

| read/value | action | write | valid prefix |
|---:|---|---:|---|
| 0/2 | skip | 0 | `[]` |
| 1/1 | write at 0 | 1 | `[1]` |
| 2/2 | skip | 1 | `[1]` |
| 3/3 | write at 1 | 2 | `[1,3]` |
| 4/2 | skip | 2 | `[1,3]` |

## 3.4 Stable move-zeroes

First compact nonzero values, then fill the suffix:

```java
static void moveZeroes(int[] values) {
    int write = 0;
    for (int value : values) {
        if (value != 0) {
            values[write++] = value;
        }
    }
    while (write < values.length) {
        values[write++] = 0;
    }
}
```

This preserves nonzero order and writes each slot at most a small constant number of times. A swapping variant also works but may perform unnecessary writes when most values are already nonzero. SDE-2 analysis can discuss write amplification when arrays represent persistent or off-heap storage.

## 3.5 Deduplicate a sorted array

Sortedness makes equal values adjacent:

```java
static int compactSortedUnique(int[] values) {
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

For `[1,1,2,2,2,5]`, the valid result prefix becomes `[1,2,5]`. Running the same method on unsorted input does not remove nonadjacent duplicates. The precondition is part of correctness.

To keep at most `k` copies in a sorted array, compare the current value with the element `k` places before the write boundary. This generalizes the same prefix invariant.

## 3.6 Merge two sorted arrays

When the output is separate, advance the input whose head is smaller. On ties, taking from the first input creates a stable merge relative to input order.

```java
static int[] mergeSorted(int[] first, int[] second) {
    int[] result = new int[first.length + second.length];
    int left = 0;
    int right = 0;
    int write = 0;
    while (left < first.length && right < second.length) {
        if (first[left] <= second[right]) {
            result[write++] = first[left++];
        } else {
            result[write++] = second[right++];
        }
    }
    while (left < first.length) result[write++] = first[left++];
    while (right < second.length) result[write++] = second[right++];
    return result;
}
```

Time is `O(n + m)`. The result is required output, not a hidden `O(1)` solution. In-place merge into a first array with spare capacity should fill from the back; filling from the front would overwrite unread values.

## 3.7 Three-way partition

For values `0`, `1`, and `2`, maintain four regions:

```text
[0, low)       settled 0s
[low, scan)    settled 1s
[scan, high]   unknown
(high, n)      settled 2s
```

```java
static void dutchFlag(int[] values) {
    int low = 0;
    int scan = 0;
    int high = values.length - 1;
    while (scan <= high) {
        switch (values[scan]) {
            case 0 -> swap(values, low++, scan++);
            case 1 -> scan++;
            case 2 -> swap(values, scan, high--);
            default -> throw new IllegalArgumentException("expected 0, 1, or 2");
        }
    }
}
```

![Dutch national flag partitions settled regions around one unknown interval](content/volumes/06-arrays-and-array-patterns/assets/05-three-way-partition-regions.png)

After swapping a `2` with `values[high]`, do not increment `scan`. The incoming value has not been classified. Every iteration either advances `scan` or decrements `high`, so the unknown region shrinks and the loop terminates in `O(n)` time.

Partitioning is not sorting. It guarantees category regions, not order within a category. If stability is required, constant-space three-way partitioning becomes substantially harder; an output buffer may be the better trade-off.

## 3.8 Trapping rain water: a derived opposing-pointer pattern

At each boundary, the smaller of the left and right maximums determines the side whose trapped water is already knowable. If `leftMax <= rightMax`, any future right boundary is at least `rightMax`, so water at `left` depends only on `leftMax`. Process left and advance it; otherwise process right.

This is a useful SDE-2 example because pointer motion is justified by information dominance, not by sorted input. The proof obligation changes even though the code shape resembles sorted two sum.

## 3.9 Complexity by total movement

All methods in this chapter are linear because each pointer moves monotonically across at most one array length. Dutch partition may swap an unseen value back to `scan`, but `high` still moves inward; the unknown region cannot grow.

Do not say "two pointers means `O(n)`." Say "`left` advances at most `n` times and `right` retreats at most `n` times, with constant work per movement, so aggregate work is `O(n)`."

## 3.10 Common failures

- applying opposing pointers without the ordering or dominance proof;
- sorting when original indexes or input order must be preserved;
- adding two `int` values before widening;
- forgetting that compacted output is only a prefix;
- incrementing `scan` after swapping with an unknown high boundary;
- claiming stable partition when swaps reorder equal-category values;
- using a fixed array as though removal changed its length; and
- counting a returned merged array as zero space.

## 3.11 Interview checkpoint

1. What proof permits one boundary to be discarded in sorted two sum?
2. Why does `write <= read` matter during compaction?
3. What part of a compacted array is valid after the method returns?
4. Why does the high-category Dutch-flag swap not advance `scan`?
5. When is sorting plus two pointers worse than hashing?
6. How would stability change the partition design?

**Foundation:** Implement stable odd-value compaction and return the logical length.

**Interview Core:** Merge two sorted arrays, deduplicate the result, and state output-space cost.

**SDE-2 Follow-up:** Compare trapping-rain-water prefix arrays with the constant-state two-pointer solution. Explain proof, memory, and readability trade-offs.

## Chapter summary

Two-pointer mastery comes from pointer roles and region invariants. Ordering can eliminate an opposing boundary, `read` and `write` can preserve a compacted prefix, and category boundaries can shrink one unknown region. The implementation becomes reliable when every pointer movement has a proof and every returned region has a contract.
