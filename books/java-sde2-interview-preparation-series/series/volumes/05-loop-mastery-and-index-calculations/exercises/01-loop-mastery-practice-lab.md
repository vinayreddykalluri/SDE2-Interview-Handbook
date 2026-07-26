# Practice Lab

Use paper first. For code-output questions, write the state at the top of each iteration. For debugging, name the violated invariant before changing code. Complete solutions appear after the entire lab.

Difficulty labels:

- **Foundation** — make syntax, order, and bounds automatic.
- **Interview Core** — derive common patterns under a clear contract.
- **SDE-2 Follow-up** — defend correctness, performance, ownership, and failure behavior.

## Part A — 30 knowledge checks

1. **Foundation:** In what order are the four parts of a `for` loop evaluated?
2. **Foundation:** How many times is `index < n` evaluated when the body completes `n` times normally?
3. **Foundation:** What distinguishes `while` from `do-while`?
4. **Foundation:** What exact index range is valid for an array of length `n`?
5. **Foundation:** Why is `n` sometimes a valid boundary but never a valid element index?
6. **Foundation:** What does `continue` do in a `for` loop?
7. **Foundation:** Why can the same `continue` placement cause an infinite `while` loop?
8. **Foundation:** Why does assigning an enhanced-for primitive variable not update the array?
9. **Foundation:** When should you prefer an index loop over enhanced-for?
10. **Foundation:** What scope does a variable declared in the `for` initializer have?
11. **Interview Core:** Define a half-open range and derive its length.
12. **Interview Core:** Define a closed range and its empty state.
13. **Interview Core:** What are initialization, maintenance, and termination in an invariant proof?
14. **Interview Core:** Give a progress measure for forward traversal.
15. **Interview Core:** Why does a sorted two-sum pointer movement eliminate candidates safely?
16. **Interview Core:** In compaction, what do `[0, write)` and `[0, read)` mean?
17. **Interview Core:** Why is the suffix after an in-place compaction not part of the result?
18. **Interview Core:** What three signals suggest a sliding window?
19. **Interview Core:** Why is a fixed rolling sum `O(n)` rather than `O(nk)`?
20. **Interview Core:** What property permits a variable window to discard left endpoints?
21. **Interview Core:** Why may arbitrary negative values break a sum window?
22. **Interview Core:** Why can a nested pointer `while` still be linear?
23. **Interview Core:** What does lower bound return when all values are smaller than target?
24. **Interview Core:** Why does half-open lower bound use `high = mid`, not `mid - 1`?
25. **Interview Core:** Why can a Java `int[][]` be ragged?
26. **SDE-2 Follow-up:** Why is `long flat = row * cols + col` potentially wrong?
27. **SDE-2 Follow-up:** What is fail-fast iteration, and what does it not guarantee?
28. **SDE-2 Follow-up:** Why is `PriorityQueue` iteration order unsafe as sorted output?
29. **SDE-2 Follow-up:** Distinguish working space from output space for a traversal returning a list.
30. **SDE-2 Follow-up:** What production concerns can change an otherwise correct long loop?

## Part B — 20 predict-the-output questions

Assume Java 21. If code does not terminate or throws, state that instead of inventing output.

### B1 — Foundation

```java
for (int i = 0; i < 3; i++) System.out.print(i);
```

### B2 — Foundation

```java
int i = 3;
while (i-- > 0) System.out.print(i + " ");
```

### B3 — Foundation

```java
int i = 5;
do { System.out.print(i); } while (i < 0);
```

### B4 — Foundation

```java
for (int i = 0; i < 5; i += 2) System.out.print(i + " ");
```

### B5 — Foundation

```java
for (int i = 3; i >= 0; i--) {
    if (i == 1) continue;
    System.out.print(i + " ");
}
```

### B6 — Foundation

```java
int[] a = {1, 2};
for (int value : a) value += 10;
System.out.println(java.util.Arrays.toString(a));
```

### B7 — Foundation

```java
int count = 0;
for (int row = 0; row < 2; row++)
    for (int col = 0; col < 3; col++) count++;
System.out.println(count);
```

### B8 — Interview Core

```java
int i = 0;
while (i < 3) {
    if (i == 1) continue;
    i++;
}
System.out.println(i);
```

### B9 — Interview Core

```java
int[] a = {5};
for (int i = a.length - 1; i >= 0; i--) System.out.print(a[i]);
```

### B10 — Interview Core

```java
int low = 2, high = 2;
System.out.println(high - low);
```

### B11 — Interview Core

```java
int[] a = {1, 1, 2};
int write = 1;
for (int read = 1; read < a.length; read++)
    if (a[read] != a[write - 1]) a[write++] = a[read];
System.out.println(write + " " + java.util.Arrays.toString(a));
```

### B12 — Interview Core

```java
long x = 100_000 * 100_000;
long y = 100_000L * 100_000;
System.out.println(x + " " + y);
```

### B13 — Interview Core

```java
int[] a = {4, -1, 2, 10};
long sum = 0;
for (int i = 0; i < 3; i++) sum += a[i];
sum += a[3] - a[0];
System.out.println(sum);
```

### B14 — Interview Core

```java
int left = 0;
for (int right = 0; right < 4; right++) {
    while (right - left > 1) left++;
    System.out.print((right - left + 1) + " ");
}
```

### B15 — Interview Core

```java
int rows = 3, cols = 4, flat = 9;
System.out.println((flat / cols) + "," + (flat % cols));
```

### B16 — Interview Core

```java
int[][] grid = {{1, 2}, {3}, {4, 5, 6}};
int count = 0;
for (int[] row : grid) count += row.length;
System.out.println(count);
```

### B17 — SDE-2 Follow-up

```java
int low = 0, high = 5;
while (low < high) {
    int mid = low + (high - low) / 2;
    if (mid < 3) low = mid + 1;
    else high = mid;
}
System.out.println(low);
```

### B18 — SDE-2 Follow-up

```java
java.util.List<Integer> a = new java.util.ArrayList<>(java.util.List.of(1, 2, 3));
for (int value : a) if (value == 2) a.remove(Integer.valueOf(value));
System.out.println(a);
```

### B19 — SDE-2 Follow-up

```java
int n = 100_000;
long pairs = (long) n * (n - 1) / 2;
System.out.println(pairs);
```

### B20 — SDE-2 Follow-up

```java
int top = 0, bottom = 0, left = 0, right = 2, visits = 0;
while (top <= bottom && left <= right) {
    for (int c = left; c <= right; c++) visits++;
    top++;
    for (int r = top; r <= bottom; r++) visits++;
    right--;
    if (top <= bottom) for (int c = right; c >= left; c--) visits++;
    if (left <= right) for (int r = bottom; r >= top; r--) visits++;
}
System.out.println(visits);
```

## Part C — 20 debugging drills

For each, describe the failure, state the violated range/invariant, and provide a correction.

1. **Foundation:** `for (int i = 0; i <= values.length; i++) sum += values[i];`
2. **Foundation:** `for (int i = values.length; i >= 0; i--) use(values[i]);`
3. **Foundation:** `while (remaining > 0) { if (skip) continue; remaining--; }`
4. **Foundation:** an enhanced-for loop assigns `value = 0` but expects the array to clear.
5. **Foundation:** `do-while` is used when zero executions are required for invalid input.
6. **Interview Core:** a closed interval is initialized with `right = n`.
7. **Interview Core:** half-open binary search updates `high = mid - 1`.
8. **Interview Core:** binary search updates `low = mid`, allowing a two-element interval not to shrink.
9. **Interview Core:** sorted two-sum adds two `int` values and then casts the result to `long`.
10. **Interview Core:** compaction returns the original array without its valid prefix length.
11. **Interview Core:** sorted deduplication starts `write = 1` without handling empty input.
12. **Interview Core:** a fixed window subtracts `values[right - width + 1]`.
13. **Interview Core:** a distinct-value window decrements counts but leaves zero-count keys.
14. **Interview Core:** shortest-sum window is applied to arbitrary negative values.
15. **Interview Core:** pair count is stored in `int`.
16. **Interview Core:** rectangular traversal uses `matrix[0].length` before checking emptiness.
17. **Interview Core:** spiral traversal always emits the bottom and left edges without guards.
18. **SDE-2 Follow-up:** flattening returns `(long) (row * cols + col)`.
19. **SDE-2 Follow-up:** an enhanced-for loop removes from an `ArrayList` directly.
20. **SDE-2 Follow-up:** a service loop logs the complete payload on every iteration.

## Part D — 24 focused coding tasks

1. **Foundation:** return the sum of all array values using a forward index loop and `long`.
2. **Foundation:** copy an array in reverse order without mutating input.
3. **Foundation:** count values at even indexes, not even values.
4. **Foundation:** return the first index of target or `-1`.
5. **Foundation:** verify that an array is nondecreasing.
6. **Foundation:** traverse every non-null row of a ragged matrix.
7. **Interview Core:** implement lower bound.
8. **Interview Core:** implement upper bound and count duplicates using two bounds.
9. **Interview Core:** find a target pair in a sorted array with opposing pointers.
10. **Interview Core:** test a `char[]` palindrome with opposing pointers.
11. **Interview Core:** remove a selected value in place and return valid length.
12. **Interview Core:** deduplicate a sorted array in place.
13. **Interview Core:** keep at most two copies of each sorted value.
14. **Interview Core:** merge two sorted arrays stably.
15. **Interview Core:** return the distinct intersection of two sorted arrays.
16. **Interview Core:** compute the maximum sum of a fixed-width window.
17. **Interview Core:** return longest subarray with at most K distinct values.
18. **Interview Core:** count subarrays with exactly K distinct values.
19. **Interview Core:** find the shortest positive-value window reaching a target sum.
20. **Interview Core:** count sorted pairs with distance at most a limit.
21. **Interview Core:** flatten and unflatten a rectangular index with checked `long` arithmetic.
22. **Interview Core:** return row-major and column-major values for a validated rectangle.
23. **Interview Core:** return a spiral traversal without duplicate final edges.
24. **SDE-2 Follow-up:** design a cancellation-aware visitor for a huge rectangular grid without materializing output.

## Part E — 15 interview follow-up chains

1. **Sorted pair:** What breaks if input is unsorted? What if original indexes are required? What if sums overflow?
2. **Palindrome:** Is comparison by `char`, code point, normalized text, or grapheme? Is case ignored? Who owns normalization?
3. **Compaction:** Must order be stable? May input mutate? Should stale suffix slots be cleared for object retention?
4. **Deduplication:** What if input is not sorted? What if at most `m` copies are allowed? What does the returned length mean?
5. **Merge:** Is tie handling stable? Can output alias an input? What if total length overflows allocation limits?
6. **Fixed window:** Can width be zero? Can input stream? What state is required to remove the departing value?
7. **Variable window:** Prove monotonicity. What changes with negatives? What bounds the map?
8. **Window count:** Why is the answer `long`? Derive exactly K. How is `k = 0` defined?
9. **Lower bound:** What invariant do you use? Why may result equal `n`? How do duplicates behave?
10. **Pair aggregate:** Why is nested syntax linear? Is HashMap cost expected or guaranteed? Can preprocessing dominate?
11. **Matrix:** Rectangular or ragged? Empty behavior? What is the output-size limit?
12. **Flattening:** Which layout order? What metadata must be persisted? How is overflow reported?
13. **Spiral:** Prove every cell exactly once. Test 1xN and Nx1. Can results be streamed?
14. **Collections:** What iteration order is promised? Which mutations are legal? Is iteration thread-safe?
15. **Production loop:** How are cancellation, deadlines, telemetry, partial results, and backpressure handled?

## Part F — cumulative assessments

### Assessment 1 — Foundations, 35 minutes

Without notes:

1. Trace `for`, `while`, `do-while`, and enhanced-for examples.
2. Repair five boundary bugs from Part C.
3. Implement forward sum, reverse copy, and first index.
4. Explain array bounds checking and enhanced-for assignment.
5. Test empty, singleton, first, and last positions.

**Pass standard:** all code compiles; no invalid array access; every loop has a named progress measure.

### Assessment 2 — Pointers and windows, 55 minutes

1. Implement sorted two-sum, compaction, deduplication, fixed window, and at-most-K distinct.
2. Dry-run one pointer method and one window method in tables.
3. State the invariant and total movement for each.
4. Explain overflow and mutation contracts.
5. Give a counterexample to a window assumption.

**Pass standard:** correct endpoint tests, `long` where required, and no unsupported monotonicity claim.

### Assessment 3 — Search and grids, 55 minutes

1. Implement lower and upper bound.
2. Implement checked flatten/unflatten.
3. Implement spiral traversal for a rectangle.
4. Reject or explicitly support ragged and null rows.
5. Prove interval shrinkage and exactly-once cell visitation.

**Pass standard:** passes empty, duplicates, insertion at `n`, 1xN, Nx1, rectangular, and overflow-oriented tests.

## Final readiness interview — 90 minutes

Build one Java class containing:

1. lower bound;
2. stable in-place compaction;
3. longest at-most-K distinct;
4. count pairs within sorted distance;
5. checked flatten/unflatten;
6. spiral traversal;
7. at least 20 deterministic tests.

Then deliver a ten-minute explanation covering input contracts, ranges, invariants, progress, termination, aggregate complexity, Java numeric promotion, collection iteration behavior, mutation ownership, and production limits.

**Ready to advance** means the implementation compiles without warnings, all tests pass, explanations do not rely on “because it is the template,” and you can repair one changed requirement without discarding the state model.
