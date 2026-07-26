# Solutions

These are reasoning models, not merely answer keys. Compare the invariant and contract before comparing syntax. Complete executable implementations are also available in `LoopMasteryExamples.java`.

## Solutions A — knowledge checks

1. Initialization once; condition; body if true; update; condition again. A false condition exits.
2. `n + 1` times after normal completion: once for each body entry and one final false test.
3. `while` tests before the body and can run zero times; `do-while` tests afterward and runs at least once.
4. `[0,n)`, meaning `0 <= index < n`.
5. `n` names the position after all elements, useful for empty ranges and insertion points; no element exists there.
6. It skips the remaining body, executes the update clause, then retests.
7. A `while` has no automatic update clause; `continue` can skip the only progress statement.
8. The loop variable receives a copy of each primitive value. Reassigning the copy does not write the slot.
9. When position, neighbors, replacement, reverse order, or a custom step is required.
10. From its declaration through the `for` statement and body; it is unavailable after the loop.
11. `[left,right)` includes left and excludes right; its length is `right - left`.
12. `[left,right]` includes both ends; it is empty when `left > right`.
13. Show the invariant true before iteration, preserved by one iteration, and strong enough at exit to imply the answer.
14. `n - index`, a nonnegative quantity that decreases by one.
15. Sorted order proves that a too-small left value cannot pair with any smaller right candidate, and symmetrically for a too-large right value.
16. `[0,write)` is retained output; `[0,read)` is inspected input. The invariant equates the first with retained values from the second.
17. The method only promises a compacted prefix. Old or overwritten values may remain beyond `write`.
18. Contiguous candidates, heavy overlap, and incrementally updateable state.
19. Each item enters once and leaves at most once; initialization plus sliding is proportional to `n`.
20. Monotonicity: removing from the left must move invalid state predictably toward validity.
21. Removing a negative can increase a sum and adding a negative can decrease it, invalidating safe one-way discard.
22. Count total movement. If each pointer advances at most `n` times, all inner executions total `O(n)`.
23. `values.length`, the insertion point after all elements.
24. `mid` may be the first qualifying index, so it must remain a candidate in the new half-open interval.
25. It is an array of independently allocated row references; rows need not share a length and may be null.
26. `row * cols` executes as `int` and can overflow before assignment. Cast an operand before multiplication or use checked `long` arithmetic.
27. Many iterators detect structural modification on a best-effort basis and throw. This is not thread safety or a guaranteed detector.
28. Its iterator exposes heap storage order, not repeated-priority-removal order. Use `poll()` on an owned/copy queue for sorted priority output.
29. Loop counters and boundaries are working space; the list of requested results is output space. Report both.
30. Deadlines, cancellation, output limits, memory, ownership, concurrency, backpressure, observability, and failure/partial-result contracts.

## Solutions B — predicted output

1. `012`. Values 0 through 2 satisfy `i < 3`.
2. `2 1 0 `. The comparison uses the old value; postfix decrement occurs before the body prints the new value.
3. `5`. The body executes once before the false condition.
4. `0 2 4 `. The update adds two.
5. `3 2 0 `. `continue` skips printing 1, but the `for` update still decrements.
6. `[1, 2]`. Primitive loop variables are copies.
7. `6`. The inner body runs three times for each of two rows.
8. It does not terminate. At `i == 1`, `continue` skips `i++` forever.
9. `5`. Reverse traversal starts at the only valid index, zero.
10. `0`. The half-open interval `[2,2)` is empty.
11. `2 [1, 2, 2]`. The valid prefix is `[1,2]`; the stale suffix is not output.
12. `1410065408 10000000000`. The first multiplication overflows as `int`; the second is `long`.
13. `11`. Initial sum 5; add 10 and remove 4.
14. `1 2 2 2 `. After shrinking, the active window never exceeds length two.
15. `2,1`. Integer division and remainder unflatten index 9 in four columns.
16. `6`. Ragged row lengths are 2, 1, and 3.
17. `3`. The half-open interval converges to the first boundary not below 3.
18. It normally throws `ConcurrentModificationException` during iterator advancement. Fail-fast is best effort, so code must be classified as invalid even if a particular rearrangement appears to finish.
19. `4999950000`. The cast occurs before multiplication.
20. `3`. The guards prevent a one-row rectangle from being traversed again as a bottom edge.

## Solutions C — debugging drills

1. `i == length` is not an element. Use `i < values.length`; invariant processed `[0,i)`.
2. `values.length` is the after-end boundary. Start at `length - 1`.
3. `continue` skips progress. Move decrement before the branch or restructure so every branch advances/exits.
4. Enhanced-for assignment changes only a copy. Use an index and write `values[i] = 0`.
5. `do-while` guarantees one execution. Validate first or use `while` when zero is required.
6. A full closed element range ends at `n - 1`. Alternatively use half-open `[0,n)` consistently.
7. `mid` may qualify and cannot be discarded. Use `high = mid` in the half-open lower-bound template.
8. With `mid == low`, `low = mid` makes no progress. Use `low = mid + 1` when mid is ruled out.
9. Overflow happens before the cast. Use `(long) a[left] + a[right]`.
10. Only `[0,write)` is valid. Return `write`, a slice/copy, or a result object containing both array and length.
11. Index zero does not exist for empty input. Return zero before `write = 1`.
12. The departing old element is `right - width`; `+1` points inside the new window.
13. Map size no longer equals distinct count. Remove the key exactly when its count reaches zero.
14. The validity rule is not monotone. Use a technique appropriate to arbitrary negatives, such as prefix state with an ordered structure for the specific problem.
15. Pair count may approach `n^2 / 2`. Use `long` and cast before multiplication.
16. Empty matrices have no row zero. Return/handle zero before accessing the first row.
17. One remaining row/column is duplicated. Recheck `top <= bottom` and `left <= right` before opposite edges.
18. The cast follows overflowing `int` arithmetic. Use `Math.addExact(Math.multiplyExact((long) row, cols), col)`.
19. Enhanced-for uses an iterator; external structural mutation invalidates it. Use iterator removal or `removeIf`.
20. Per-item payload logging causes cost, leakage, and noise. Emit bounded aggregate metrics and sampled/redacted diagnostics.

## Solutions D — coding tasks

### D1–D6 foundations

```java
static long sum(int[] values) {
    long total = 0;
    for (int value : values) total += value;
    return total;
}

static int[] reversedCopy(int[] values) {
    int[] copy = new int[values.length];
    for (int i = 0; i < values.length; i++)
        copy[i] = values[values.length - 1 - i];
    return copy;
}

static int countEvenIndexes(int[] values) {
    int count = 0;
    for (int i = 0; i < values.length; i += 2) count++;
    return count;
}

static int firstIndex(int[] values, int target) {
    for (int i = 0; i < values.length; i++)
        if (values[i] == target) return i;
    return -1;
}

static boolean isNondecreasing(int[] values) {
    for (int i = 1; i < values.length; i++)
        if (values[i] < values[i - 1]) return false;
    return true;
}

static long raggedSum(int[][] grid) {
    long total = 0;
    for (int[] row : grid)
        if (row != null) for (int value : row) total += value;
    return total;
}
```

The reverse formula maps output index `i` to input `n - 1 - i`. Nondecreasing comparison starts at 1 because every element is compared with its predecessor.

### D7–D8 bounds

Use the chapter's half-open `lowerBound` and `upperBound`. The duplicate count is `upperBound(values,target) - lowerBound(values,target)`. Both return boundaries in `[0,n]`, so absence produces zero without special scanning.

### D9–D15 pointer patterns

Use `sortedTwoSum`, palindrome, `compactRemoving`, `deduplicateSorted`, `keepAtMostTwo`, merge, and intersection from Chapter 3 and the executable companion. Required reasoning:

- sortedness justifies opposing-pointer elimination;
- `[0,write)` is the valid output prefix;
- merge output `[0,write)` remains sorted;
- every input pointer moves only forward;
- tie choice defines stability;
- all sum arithmetic that may exceed `int` is widened first.

### D16 fixed window

Initialize exactly `width` elements. For each new `right`, add `values[right]`, subtract `values[right - width]`, then update best. Reject width outside `[1,n]`. Use `long` for sum.

### D17 at most K distinct

Maintain a frequency map for `[left,right]`. Add right, shrink while map size exceeds `k`, delete zero-count entries, then maximize `right - left + 1`. For `k == 0`, every restored window is empty and answer is zero.

### D18 exactly K distinct

```java
static long exactlyKDistinct(int[] values, int k) {
    if (k <= 0) return 0;
    return countAtMostKDistinct(values, k)
            - countAtMostKDistinct(values, k - 1);
}
```

The subtraction works because “at most k” partitions all subarrays by distinct count. Return `long`.

### D19 shortest positive window

Expand right and add. While sum is at least target, update the minimum before subtracting `positive[left++]`. Validate positivity because it supplies monotonicity.

### D20 sorted pair distance

For each right, advance left while `(long) sorted[right] - sorted[left] > limit`, then add `right - left`. Every valid earlier index in `[left,right)` pairs with right.

### D21 checked grid mapping

Validate positive dimensions and coordinates. Flatten with checked `long` multiply/add. For unflatten, validate `0 <= flat < rows * columns`, then use division and remainder. The `columns` metadata is part of the layout contract.

### D22 row and column order

Validate rectangularity once. Row-major nests columns inside rows; column-major nests rows inside columns. Both visit `rows * columns` cells; result lists are output space.

### D23 spiral

Maintain inclusive `top`, `bottom`, `left`, `right`. Consume top/right, then guard bottom/left. The invariant is that cells outside the current rectangle were emitted once.

### D24 cancellation-aware visitor

```java
interface CellVisitor {
    void visit(int row, int col, int value);
}

static boolean visitGrid(int[][] grid, CellVisitor visitor,
        java.util.function.BooleanSupplier cancelled) {
    int cols = requireRectangular(grid);
    long visited = 0;
    for (int row = 0; row < grid.length; row++) {
        for (int col = 0; col < cols; col++) {
            if ((visited & 1023L) == 0 && cancelled.getAsBoolean()) {
                return false;
            }
            visitor.visit(row, col, grid[row][col]);
            visited++;
        }
    }
    return true;
}
```

This streams visits and checks cancellation every 1024 cells. The API must document whether `false` means a partial side effect and whether the visitor may throw.

## Solutions E — follow-up chains

1. Unsortedness removes safe elimination; sort with index metadata or use hashing. Widen before addition.
2. Define the text unit and normalization/case contract before claiming correctness; `char` is only a UTF-16 code unit.
3. State stability and mutation. For object arrays, clearing the unused suffix can release references if the array is retained.
4. Unsorted deduplication needs hashing or sorting. Generalize the retained-prefix invariant to at most `m` copies.
5. `<=` chooses first on ties and makes that source preference stable. Validate allocation capacity and ownership.
6. Width must have a defined domain. Streaming needs the last `k` values, commonly a ring buffer or deque.
7. State the monotone validity rule explicitly. Negative sums often require a different method. Bound map state by the active constraint when possible.
8. Counts can be quadratic. Exactly K is the difference of two cumulative counts; define negative K as zero.
9. Use half-open candidates, preserve qualifying `mid`, and allow the after-end insertion boundary.
10. Sum each pointer's lifetime movement. Qualify collection operations as expected/amortized as appropriate; include sorting or map construction.
11. Reject or support raggedness explicitly. Avoid materializing unbounded output.
12. Record row-major/column-major, dimensions, and version. Use checked arithmetic and a specified exception/result.
13. Shrinking-rectangle guards prove uniqueness for degenerate shapes. A callback can stream visits.
14. Order depends on collection contract; mutate through supported APIs; choose synchronization/concurrent collections deliberately.
15. Establish checkpoints, deadline behavior, bounded logs, output/backpressure limits, and whether partial results are visible or rolled back.

## Solutions F — assessment rubric

A passing solution is not one that only matches sample output. It must compile, preserve its stated invariant, strictly progress, handle category-based boundary tests, and make correct Java claims.

- **Foundation pass:** 80% of traces correct and all six basic methods boundary-safe.
- **Pointer/window pass:** every movement is justified; no after-overflow cast; assumptions such as sortedness and positivity are explicit.
- **Search/grid pass:** lower/upper results remain within `[0,n]`; dimensions and raggedness are defined; spiral visits exactly the cell count.
- **Final readiness pass:** all deterministic tests pass under Java 21 with `-Xlint:all -Werror`; explanations cover correctness, complexity, mutation, and production behavior without relying on memorized templates.

When an answer fails, record the category—range, invariant, progress, numeric width, mutation, iteration semantics, or contract—then repeat one smaller task from that category before retaking the assessment.
