# SDE-2 Array Problem-Solving Patterns

## Why arrays are the interview default

An array is a fixed-size, indexed region of homogeneous references or primitive values. That simple model supports a large fraction of interview problems because position, order, and contiguous ranges are explicit. The hard part is rarely the loop itself. The hard part is recognizing which information can be maintained incrementally, whether mutation is allowed, and which invariant makes the result trustworthy.

At SDE-2 level, a candidate should move beyond "use two pointers" and explain why that pattern applies, which region of the array has a settled meaning, how overflow and aliasing are handled, and what would change in a production API.

## Learning objectives

After completing this chapter, you should be able to:

- select an array pattern from ordering, contiguity, value-domain, and mutation signals;
- compact, partition, rotate, and place values in situ with a stated invariant;
- derive Kadane's algorithm and return both the maximum sum and its range;
- merge intervals without mutating caller-owned nested arrays;
- use cyclic placement or sign marking only when the value domain proves indexes are safe;
- build prefix and difference arrays with correct half-open boundaries;
- traverse and transform matrices while validating rectangular or square shape;
- distinguish auxiliary space from output space and input mutation; and
- discuss ownership, defensive copying, concurrency, and memory locality.

## Array pattern decision map

Ask these questions in order.

| Question about the problem | Likely pattern | Critical precondition |
|---|---|---|
| Is the output a filtered form of the input? | read/write compaction | mutation and output-prefix contract are allowed |
| Is the value domain a few categories? | Dutch national flag partition | each value maps to a known category |
| Is the answer one contiguous range? | window, prefix state, or Kadane | operation and constraints determine which one |
| Are intervals overlapping after ordering? | sort and sweep | endpoint semantics are defined |
| Are values drawn from `1..n`? | cyclic placement or sign marking | mutation is allowed and validation is safe |
| Is the operation a cyclic shift? | reversal rotation | normalize distance and define empty behavior |
| Are there many immutable range queries? | prefix array | extra `O(n)` memory is acceptable |
| Are there many offline range updates? | difference array | final values can be materialized later |
| Is the input a grid? | row/column/diagonal/boundary traversal | rectangular versus ragged contract is explicit |

Two signals can coexist. "Maximum sum after many range additions" might use a difference array to materialize values and Kadane afterward. Pattern selection is composition guided by invariants, not a one-label classification exercise.

## Java array semantics and ownership

An array variable stores a reference. Assignment aliases the same array:

```java
int[] original = {1, 2, 3};
int[] alias = original;
alias[0] = 99;                 // original[0] is now 99
int[] copy = original.clone(); // independent primitive array
```

For `int[][]`, `clone()` copies only the outer array. The row arrays remain shared. A defensive deep copy must clone every row. This matters for interval inputs represented as `int[][]`: sorting the outer array reorders caller-visible row references, and editing an endpoint mutates a shared row. A production-quality helper either documents destructive behavior or copies first.

Arrays expose mutable state without synchronization. A method cannot safely scan an array while another thread modifies it unless the caller provides coordination. Declaring a reference `final` prevents rebinding, not element mutation.

Use `Arrays.copyOf` or `clone` deliberately. Copying costs `O(n)` time and space but creates an ownership boundary. In interview answers, state whether auxiliary-space analysis includes a defensive copy and why the API chooses it.

## Pattern 1: stable compaction

To retain selected elements in original order, maintain a read index and a write boundary. For deduplicating a sorted array, retain the first value and write a value only when it differs from the last retained value.

Invariant before reading index `read`: `[0, write)` contains exactly one representative of each run found in `[0, read)`, in sorted order. Since `write <= read`, overwriting `values[write]` cannot destroy unread data. At termination, the logical result is `[0, write)`; the suffix is unspecified.

### Dry run

Input: `[1, 1, 2, 2, 2, 5]`.

| read value | action | logical prefix |
|---:|---|---|
| first 1 | initialize | `[1]` |
| 1 | duplicate, skip | `[1]` |
| 2 | write at 1 | `[1,2]` |
| 2 | duplicate, skip | `[1,2]` |
| 2 | duplicate, skip | `[1,2]` |
| 5 | write at 2 | `[1,2,5]` |

Time is `O(n)`, auxiliary space is `O(1)`, and order is stable. The sortedness precondition is essential; non-adjacent duplicates in an unsorted array would survive.

## Pattern 2: Dutch national flag partition

When values belong to three categories, maintain four regions:

```text
[0, low)       settled low category
[low, scan)    settled middle category
[scan, high]   unknown
(high, n)      settled high category
```

For values `0`, `1`, and `2`:

- if `values[scan] == 0`, swap it with `values[low]`, then increment both;
- if it is `1`, increment `scan`;
- if it is `2`, swap it with `values[high]` and decrement `high`, but do not increment `scan` because the incoming value is unclassified.

The unknown region shrinks every iteration, proving termination. Each element is classified in `O(1)` work, so time is `O(n)` and auxiliary space is `O(1)`.

For `[2,0,2,1,1,0]`, the first swap brings `0` from the end to index 0 but leaves an unclassified `0` at `scan`, which must be inspected next. Incrementing `scan` after the high-category swap is the classic bug.

In a production API, use an enum or classifier when categories have domain meaning. Magic integers make validation and evolution harder.

## Pattern 3: Kadane's maximum subarray

For each index `i`, define `endingHere` as the best sum of a nonempty subarray ending exactly at `i`. Such a subarray either starts at `i` or extends the best subarray ending at `i - 1`:

```text
endingHere(i) = max(values[i], endingHere(i - 1) + values[i])
```

The global answer is the maximum `endingHere` seen. This is dynamic programming compressed to constant state.

To return a range, track the start of the current candidate. If extending would be worse than starting fresh, set the candidate start to `i`. Update the best range only when the sum improves; a defined tie policy can prefer the earliest or shortest range.

### Dry run

For `[-2, 1, -3, 4, -1, 2, 1, -5, 4]`:

| i/value | best ending here | candidate range | global best |
|---:|---:|---|---:|
| 0/-2 | -2 | `[0,1)` | -2 |
| 1/1 | 1 | `[1,2)` | 1 |
| 2/-3 | -2 | `[1,3)` | 1 |
| 3/4 | 4 | `[3,4)` | 4 |
| 4/-1 | 3 | `[3,5)` | 4 |
| 5/2 | 5 | `[3,6)` | 5 |
| 6/1 | 6 | `[3,7)` | 6 |
| 7/-5 | 1 | `[3,8)` | 6 |
| 8/4 | 5 | `[3,9)` | 6 |

The answer is sum 6 over `[3,7)`, values `[4,-1,2,1]`. Initializing the best sum to zero would incorrectly allow an empty subarray for all-negative input. Use `long` for accumulated sums.

## Pattern 4: sort and merge intervals

An interval contract must say whether endpoints are closed, open, or half-open. The implementation below treats intervals as closed `[start, end]`, so `[1,4]` and `[4,7]` overlap. For half-open intervals, touching at 4 would not overlap unless the product requirement chooses coalescing.

Algorithm:

1. validate each interval has two endpoints and `start <= end`;
2. copy the intervals to avoid mutating the caller;
3. sort by start, then end using comparison methods that cannot overflow;
4. keep one current merged interval;
5. extend it when the next start is at most the current end; otherwise emit it and begin another.

Invariant: emitted intervals are sorted, pairwise disjoint, and exactly cover all processed intervals except the current merged interval. Time is `O(n log n)` due to sorting; the sweep is `O(n)`. Output and defensive-copy space are `O(n)`.

### Dry run

`[[1,3],[2,6],[8,10],[10,12]]` becomes `[[1,6],[8,12]]`. The first two overlap. The third starts a new component. The final interval touches 10 and merges under the closed-interval contract.

## Pattern 5: cyclic placement

If an array of length `n` contains values in the domain `1..n`, value `v` has a natural home at index `v - 1`. While a value is in range and not already at home, swap it into its home. Each successful swap settles at least one value, so there are at most `O(n)` successful swaps even though a `while` appears inside a `for`.

For first missing positive, values outside `1..n` are irrelevant. After placement, the first index `i` whose value is not `i + 1` identifies the missing positive; if all positions match, the answer is `n + 1`.

Input `[3,4,-1,1]` evolves as follows:

- at index 0, place 3 at index 2: `[-1,4,3,1]`;
- index 1 holds 4, place it at index 3: `[-1,1,3,4]`;
- index 1 now holds 1, place it at index 0: `[1,-1,3,4]`;
- index 1 is the first mismatch, so the answer is 2.

Time is `O(n)`, auxiliary space is `O(1)`, and the input is destroyed. Duplicates require the `values[home] != values[i]` guard; without it, equal values swap forever.

## Pattern 6: in-place sign marking

For values guaranteed to lie in `1..n`, index `abs(v) - 1` can record whether value `v` has appeared by making that cell negative. If the cell is already negative, `v` is a duplicate.

This is useful but contract-heavy:

- zero and out-of-range values produce invalid indexes;
- repeated reporting policy must be defined when a value appears more than twice;
- input signs are destroyed unless a restoration pass runs;
- `Math.abs(Integer.MIN_VALUE)` remains negative, though the domain precondition excludes it; and
- a negative-zero marker does not exist, which is why the domain begins at 1.

Cyclic placement is often easier to defend for first-missing-positive. Sign marking is concise for presence or duplicate detection when mutation is explicitly permitted.

## Pattern 7: rotation by reversal

To rotate an array right by `distance`:

1. normalize `k = floorMod(distance, n)`;
2. reverse the whole array;
3. reverse `[0, k)`;
4. reverse `[k, n)`.

For `[1,2,3,4,5,6,7]` and `k = 3`:

```text
whole reverse: [7,6,5,4,3,2,1]
first part:    [5,6,7,4,3,2,1]
second part:   [5,6,7,1,2,3,4]
```

The result preserves the relative order inside both cyclic segments. Time is `O(n)` and auxiliary space is `O(1)`. Handle an empty array before taking a modulus by zero. `Math.floorMod` gives intentional behavior for negative distances.

## Pattern 8: prefix arrays

For immutable range-sum queries, define a `long` prefix array of length `n + 1`:

```text
prefix[0] = 0
prefix[i + 1] = prefix[i] + values[i]
```

The sum over half-open range `[left, right)` is `prefix[right] - prefix[left]`. Everything before `left` cancels. Construction takes `O(n)` time and `O(n)` space; each query takes `O(1)`.

For `[3,-2,5,1]`, the prefix is `[0,3,1,6,7]`. Range `[1,4)` sums to `7 - 3 = 4`, matching `-2 + 5 + 1`.

Prefix state is a snapshot. If the underlying array changes, the prefix is stale. A system with frequent point updates may need a Fenwick tree or segment tree rather than rebuilding.

## Pattern 9: difference arrays for offline range updates

A difference array stores changes between adjacent positions. To add `delta` to half-open range `[left, right)`:

```text
difference[left] += delta
difference[right] -= delta   // if right is within the sentinel array
```

Taking a running sum materializes all values. Allocate `n + 1` slots so the subtraction at `right == n` is safe. After `q` updates, total time is `O(q + n)` instead of `O(qn)`. Use `long` because updates accumulate.

Invariant while materializing: the running sum equals the net delta applying at the current index. This technique is offline; querying arbitrary points before materialization or interleaving updates and queries calls for a different data structure.

## Pattern 10: matrix traversal and transformation

Java matrices may be ragged. A rectangular algorithm must validate row lengths. Row-major traversal normally has better locality because each Java row is a contiguous array. Column-major traversal still takes `O(rows * cols)` time but jumps among row objects.

For an in-place 90-degree clockwise rotation, require a square matrix. Transpose across the main diagonal, then reverse every row:

```text
transpose: matrix[row][col] <-> matrix[col][row] for col > row
reverse:   each row from both ends
```

After transpose, the original column becomes a row; reversal gives the clockwise orientation. Time is `O(n^2)` and auxiliary space is `O(1)`. Rectangular rotation changes dimensions and cannot be done through the same `int[][]` shape without allocating a new matrix.

## Runnable Java 21 reference implementation

The class below compiles as written. Run its checkpoints with `java -ea ArrayPatternToolkit`.

```java
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;

public final class ArrayPatternToolkit {
    private ArrayPatternToolkit() {
    }

    public record Subarray(long sum, int start, int endExclusive) {
    }

    public static int compactSortedUnique(int[] values) {
        requireArray(values);
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

    public static void dutchFlag(int[] values) {
        requireArray(values);
        int low = 0;
        int scan = 0;
        int high = values.length - 1;
        while (scan <= high) {
            switch (values[scan]) {
                case 0 -> swap(values, low++, scan++);
                case 1 -> scan++;
                case 2 -> swap(values, scan, high--);
                default -> throw new IllegalArgumentException("expected only 0, 1, and 2");
            }
        }
    }

    public static Subarray maxSubarray(int[] values) {
        requireArray(values);
        if (values.length == 0) {
            throw new IllegalArgumentException("a nonempty array is required");
        }
        long endingHere = values[0];
        long best = values[0];
        int candidateStart = 0;
        int bestStart = 0;
        int bestEnd = 1;

        for (int i = 1; i < values.length; i++) {
            long extended = endingHere + values[i];
            if (values[i] > extended) {
                endingHere = values[i];
                candidateStart = i;
            } else {
                endingHere = extended;
            }
            if (endingHere > best) {
                best = endingHere;
                bestStart = candidateStart;
                bestEnd = i + 1;
            }
        }
        return new Subarray(best, bestStart, bestEnd);
    }

    public static int[][] mergeClosedIntervals(int[][] intervals) {
        if (intervals == null) {
            throw new IllegalArgumentException("intervals must not be null");
        }
        int[][] copy = new int[intervals.length][2];
        for (int i = 0; i < intervals.length; i++) {
            if (intervals[i] == null || intervals[i].length != 2
                    || intervals[i][0] > intervals[i][1]) {
                throw new IllegalArgumentException("invalid interval at index " + i);
            }
            copy[i] = intervals[i].clone();
        }
        Arrays.sort(copy, Comparator.comparingInt((int[] a) -> a[0])
                .thenComparingInt(a -> a[1]));
        List<int[]> merged = new ArrayList<>();
        for (int[] interval : copy) {
            if (merged.isEmpty() || interval[0] > merged.get(merged.size() - 1)[1]) {
                merged.add(interval.clone());
            } else {
                int[] last = merged.get(merged.size() - 1);
                last[1] = Math.max(last[1], interval[1]);
            }
        }
        return merged.toArray(int[][]::new);
    }

    public static int firstMissingPositive(int[] values) {
        requireArray(values);
        for (int i = 0; i < values.length; i++) {
            while (values[i] >= 1 && values[i] <= values.length) {
                int home = values[i] - 1;
                if (values[home] == values[i]) {
                    break;
                }
                swap(values, i, home);
            }
        }
        for (int i = 0; i < values.length; i++) {
            if (values[i] != i + 1) {
                return i + 1;
            }
        }
        return values.length + 1;
    }

    public static List<Integer> duplicatesBySignMarking(int[] values) {
        requireArray(values);
        for (int value : values) {
            if (value < 1 || value > values.length) {
                throw new IllegalArgumentException("values must be in 1..n");
            }
        }
        List<Integer> duplicates = new ArrayList<>();
        for (int i = 0; i < values.length; i++) {
            int value = (int) Math.abs((long) values[i]);
            int marker = value - 1;
            if (values[marker] < 0) {
                duplicates.add(value);
            } else {
                values[marker] = -values[marker];
            }
        }
        for (int i = 0; i < values.length; i++) {
            values[i] = (int) Math.abs((long) values[i]);
        }
        return duplicates;
    }

    public static void rotateRight(int[] values, int distance) {
        requireArray(values);
        if (values.length == 0) {
            return;
        }
        int k = Math.floorMod(distance, values.length);
        reverse(values, 0, values.length);
        reverse(values, 0, k);
        reverse(values, k, values.length);
    }

    public static long[] prefixSums(int[] values) {
        requireArray(values);
        long[] prefix = new long[values.length + 1];
        for (int i = 0; i < values.length; i++) {
            prefix[i + 1] = prefix[i] + values[i];
        }
        return prefix;
    }

    public static long rangeSum(long[] prefix, int left, int rightExclusive) {
        if (prefix == null || left < 0 || left > rightExclusive
                || rightExclusive >= prefix.length) {
            throw new IllegalArgumentException("invalid prefix range");
        }
        return prefix[rightExclusive] - prefix[left];
    }

    public static long[] applyRangeAdds(int length, int[][] updates) {
        if (length < 0 || updates == null) {
            throw new IllegalArgumentException("invalid length or updates");
        }
        long[] difference = new long[length + 1];
        for (int i = 0; i < updates.length; i++) {
            int[] update = updates[i];
            if (update == null || update.length != 3) {
                throw new IllegalArgumentException("update must be [left,rightExclusive,delta]");
            }
            int left = update[0];
            int right = update[1];
            int delta = update[2];
            if (left < 0 || left > right || right > length) {
                throw new IllegalArgumentException("invalid update range");
            }
            difference[left] += delta;
            difference[right] -= delta;
        }
        long[] result = new long[length];
        long running = 0;
        for (int i = 0; i < length; i++) {
            running += difference[i];
            result[i] = running;
        }
        return result;
    }

    public static void rotateSquareClockwise(int[][] matrix) {
        int n = validateSquare(matrix);
        for (int row = 0; row < n; row++) {
            for (int col = row + 1; col < n; col++) {
                int temporary = matrix[row][col];
                matrix[row][col] = matrix[col][row];
                matrix[col][row] = temporary;
            }
        }
        for (int[] row : matrix) {
            reverse(row, 0, row.length);
        }
    }

    private static int validateSquare(int[][] matrix) {
        if (matrix == null) {
            throw new IllegalArgumentException("matrix must not be null");
        }
        int n = matrix.length;
        for (int[] row : matrix) {
            if (row == null || row.length != n) {
                throw new IllegalArgumentException("matrix must be square");
            }
        }
        return n;
    }

    private static void reverse(int[] values, int from, int toExclusive) {
        for (int left = from, right = toExclusive - 1; left < right; left++, right--) {
            swap(values, left, right);
        }
    }

    private static void swap(int[] values, int first, int second) {
        int temporary = values[first];
        values[first] = values[second];
        values[second] = temporary;
    }

    private static void requireArray(int[] values) {
        if (values == null) {
            throw new IllegalArgumentException("values must not be null");
        }
    }

    public static void main(String[] args) {
        int[] unique = {1, 1, 2, 2, 2, 5};
        int uniqueLength = compactSortedUnique(unique);
        assert Arrays.equals(Arrays.copyOf(unique, uniqueLength), new int[] {1, 2, 5});

        int[] colors = {2, 0, 2, 1, 1, 0};
        dutchFlag(colors);
        assert Arrays.equals(colors, new int[] {0, 0, 1, 1, 2, 2});

        assert maxSubarray(new int[] {-2, 1, -3, 4, -1, 2, 1, -5, 4})
                .equals(new Subarray(6, 3, 7));
        assert maxSubarray(new int[] {-8, -3, -6}).sum() == -3;

        int[][] input = {{1, 3}, {2, 6}, {8, 10}, {10, 12}};
        int[][] merged = mergeClosedIntervals(input);
        assert Arrays.deepEquals(merged, new int[][] {{1, 6}, {8, 12}});
        assert Arrays.deepEquals(input, new int[][] {{1, 3}, {2, 6}, {8, 10}, {10, 12}});

        assert firstMissingPositive(new int[] {3, 4, -1, 1}) == 2;
        int[] marked = {4, 3, 2, 7, 8, 2, 3, 1};
        assert duplicatesBySignMarking(marked).equals(List.of(2, 3));
        assert Arrays.equals(marked, new int[] {4, 3, 2, 7, 8, 2, 3, 1});

        int[] rotated = {1, 2, 3, 4, 5, 6, 7};
        rotateRight(rotated, 3);
        assert Arrays.equals(rotated, new int[] {5, 6, 7, 1, 2, 3, 4});

        long[] prefix = prefixSums(new int[] {3, -2, 5, 1});
        assert Arrays.equals(prefix, new long[] {0, 3, 1, 6, 7});
        assert rangeSum(prefix, 1, 4) == 4;
        assert Arrays.equals(applyRangeAdds(5,
                        new int[][] {{1, 4, 3}, {0, 2, -1}}),
                new long[] {-1, 2, 3, 3, 0});

        int[][] matrix = {{1, 2, 3}, {4, 5, 6}, {7, 8, 9}};
        rotateSquareClockwise(matrix);
        assert Arrays.deepEquals(matrix,
                new int[][] {{7, 4, 1}, {8, 5, 2}, {9, 6, 3}});
    }
}
```

## Complexity and contract table

| Pattern | Time | Auxiliary space | Mutates input? |
|---|---:|---:|---|
| sorted unique compaction | `O(n)` | `O(1)` | yes |
| Dutch partition | `O(n)` | `O(1)` | yes |
| Kadane with range | `O(n)` | `O(1)` | no |
| merge intervals | `O(n log n)` | `O(n)` including defensive copy/output | no |
| cyclic placement | `O(n)` aggregate | `O(1)` | yes |
| sign marking | `O(n)` | `O(1)` excluding duplicate output | temporarily, then restores |
| reversal rotation | `O(n)` | `O(1)` | yes |
| prefix construction/query | `O(n)` / `O(1)` | `O(n)` | no |
| difference updates/materialization | `O(q + n)` | `O(n)` | no |
| square matrix rotation | `O(n^2)` | `O(1)` | yes |

## Edge cases and common mistakes

1. **Returning a false empty answer.** Kadane for a nonempty subarray must initialize from the first value, not zero.
2. **Comparator overflow.** Use `Comparator.comparingInt`, not `(a, b) -> a[0] - b[0]`.
3. **Shallow copies.** Cloning `int[][]` does not clone its rows.
4. **Ambiguous interval endpoints.** Decide whether touching intervals merge.
5. **Cyclic-placement infinite loops.** Check whether the destination already contains the same value.
6. **Unvalidated sign indexes.** Sign marking is valid only for its promised domain.
7. **Modulus by zero.** Return early for an empty rotation input.
8. **Prefix off by one.** A prefix for `n` values has `n + 1` entries and range `[left, right)` uses `prefix[right] - prefix[left]`.
9. **Difference sentinel omitted.** Allocate the extra cell or guard `right == n`.
10. **Integer accumulation.** Array values may be `int` while sums, counts, and products need `long`.
11. **Ragged matrix assumptions.** Validate shape before using a shared column bound.
12. **Hidden destructive behavior.** Name in-place methods clearly and document the valid output region.

## SDE-2 production follow-ups

- **Ownership and immutability:** expose an immutable result or copy when inputs cross trust boundaries. Internal hot paths may use ownership transfer to avoid copies, but the contract must be explicit.
- **Large inputs:** check proposed result sizes before allocation. An array still uses `int` indexes in Java, and object headers plus alignment affect real memory beyond element counts.
- **Primitive versus boxed data:** `int[]` avoids per-element boxing and normally has better locality than `List<Integer>`. A collection may be preferable when size changes or abstraction matters more than raw layout.
- **Concurrency:** copying can provide a stable snapshot. Without it, callers must not mutate while the algorithm reads.
- **Observability:** record input length, category counts, and elapsed time rather than logging entire arrays that may contain sensitive data.
- **External memory:** data larger than heap may require chunking, memory-mapped files, a database operation, or a streaming algorithm. Not every array solution scales by increasing the heap.
- **Vectorization and libraries:** use `Arrays.sort`, `Arrays.fill`, `System.arraycopy`, and measured library primitives in production. Hand-written loops are justified by custom invariants, not by assuming they are faster.
- **Versioned semantics:** interval endpoints, rotation direction, and duplicate policy belong in API documentation and tests because changing them silently changes results.

## Exercises with model checkpoints

### Exercise 1: move zeros stably

Move all zero values to the end while preserving nonzero order.

**Model checkpoints:** use read/write compaction; `[0, write)` contains the nonzero values processed so far; fill `[write, n)` with zeros; time `O(n)`, space `O(1)`; clarify whether negative zero is relevant for the chosen primitive type.

### Exercise 2: maximum product subarray

Return the maximum product of a nonempty contiguous range.

**Model checkpoints:** track both maximum and minimum products ending at each index because a negative value swaps their roles; define overflow behavior; include zeros and all-negative cases; returning indexes requires tracking candidate starts for both states.

### Exercise 3: insert and merge one interval

Given sorted, disjoint closed intervals and one new interval, merge in linear time.

**Model checkpoints:** emit intervals ending before the new start; merge all intervals starting at or before the current merged end; emit the rest; distinguish closed from half-open touching behavior; avoid mutating caller rows.

### Exercise 4: product except self

Return, without division, the product of all values except the current one.

**Model checkpoints:** output first stores prefix products, then multiply by a running suffix; output storage usually does not count as auxiliary space; discuss zero behavior and overflow; reject the claim that `O(1)` total space includes a required result array.

### Exercise 5: set matrix zeroes

If a cell is zero, zero its whole row and column using constant auxiliary space.

**Model checkpoints:** use first row and first column as markers; preserve separate flags for whether they originally contained a zero; mark before clearing; validate rectangularity; explain why clearing immediately destroys future information.

### Exercise 6: range update service

Turn the difference-array helper into a service supporting online updates and queries.

**Model checkpoints:** a plain difference array cannot answer online prefix values efficiently; consider a Fenwick tree for range-add/point-query or two Fenwick trees for range-add/range-sum; define overflow, synchronization, snapshot, and persistence semantics.

### Exercise 7: missing and duplicate pair

An array of length `n` should contain `1..n`, but one value is missing and another appears twice.

**Model checkpoints:** cyclic placement gives an intuitive destructive solution; XOR or sum equations require careful derivation and overflow handling; validate the promised contract if this is production data; return a named result rather than an anonymous array.

## Interview answer checklist

- [ ] I selected the pattern from a concrete recognition signal.
- [ ] I stated whether ordering, contiguity, or a bounded value domain is required.
- [ ] I identified every settled and unknown array region.
- [ ] I documented whether the method mutates the input and which output region is valid.
- [ ] I widened arithmetic before sums, differences, counts, or products can overflow.
- [ ] I defined empty, singleton, duplicate, and all-negative behavior.
- [ ] I specified interval endpoint semantics and comparator safety.
- [ ] I distinguished primitive arrays, nested-array copies, and collection alternatives.
- [ ] I counted defensive copies and returned output honestly in the space analysis.
- [ ] I can explain how updates, concurrency, and data larger than memory change the design.

## Summary

Array mastery is a map of invariants. Compaction separates a valid output prefix from unread input. Dutch partition shrinks an unknown region. Kadane carries the best range ending at one position. Interval merging turns sorted overlap into a sweep. Cyclic placement and sign marking borrow array cells as metadata only under strict value-domain and ownership contracts. Reversal implements rotation without extra storage. Prefix and difference arrays trade memory and update timing for constant-time range operations. Matrix transformations add shape and aliasing concerns. The SDE-2 standard is to connect each technique to its proof, boundaries, mutation policy, and production consequences.
