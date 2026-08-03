# Arrays Practice Lab

Attempt these without the solutions open. For every coding task, write the contract, one invariant, time and auxiliary-space complexity, and at least four tests.

## A. Knowledge Checks

1. **Foundation:** What is stored in an `int[]` slot? What is stored in a `Student[]` slot?
2. **Foundation:** Which default values appear in newly created `int[]`, `boolean[]`, and `String[]` arrays?
3. **Foundation:** Why is `new int[5]` fixed-length even though its elements are mutable?
4. **Foundation:** Explain valid indexes using a half-open range.
5. **Foundation:** What differs between `numbers.length` and the logical number of used elements?
6. **Foundation:** Why can an enhanced `for` loop read values but not conveniently replace array slots?
7. **Foundation:** What does Java pass when an array is supplied to a method?
8. **Foundation:** Why can two variables observe the same array mutation?
9. **Interview Core:** Compare assignment, `clone`, `Arrays.copyOf`, and `System.arraycopy`.
10. **Interview Core:** Why is `matrix.clone()` generally a shallow copy?
11. **Interview Core:** What is the difference between `Arrays.equals` and `Arrays.deepEquals`?
12. **Interview Core:** State the read/write compaction invariant.
13. **Interview Core:** What property permits two opposing pointers in sorted two-sum?
14. **Interview Core:** What property permits a variable sliding window?
15. **Interview Core:** Why must Kadane's algorithm initialize from an element for a non-empty answer?
16. **Interview Core:** What does a sentinel prefix sum store at index `i`?
17. **Interview Core:** When does a prefix-sum snapshot become invalid?
18. **Interview Core:** Why does a difference array need one extra slot?
19. **Interview Core:** What input constraints enable cyclic placement?
20. **Interview Core:** Why is sign marking not a general duplicate-detection method?
21. **Interview Core:** Why can comparator subtraction be incorrect?
22. **SDE-2 Follow-up:** Distinguish auxiliary space from output space.
23. **SDE-2 Follow-up:** Explain amortized linear two-pointer work even when one pointer sometimes moves many positions.
24. **SDE-2 Follow-up:** When would `int[]` be a better interview choice than `ArrayList<Integer>`?

## B. Predict the Output

### B1 — Aliasing

```java
int[] first = {1, 2, 3};
int[] second = first;
second[1] = 9;
System.out.println(java.util.Arrays.toString(first));
```

### B2 — Reassignment

```java
static void replace(int[] values) {
    values = new int[] {7, 8};
}
int[] values = {1, 2};
replace(values);
System.out.println(values[0]);
```

### B3 — Shallow Matrix Copy

```java
int[][] first = {{1}, {2}};
int[][] second = first.clone();
second[0][0] = 8;
System.out.println(first[0][0] + " " + (first == second));
```

### B4 — Array Equality

```java
int[] a = {1, 2};
int[] b = {1, 2};
System.out.println((a == b) + " " + java.util.Arrays.equals(a, b));
```

### B5 — Overflow Before Assignment

```java
int value = 100_000;
long wrong = value * value;
long right = (long) value * value;
System.out.println(wrong + " " + right);
```

### B6 — Enhanced For

```java
int[] values = {1, 2, 3};
for (int value : values) {
    value *= 10;
}
System.out.println(java.util.Arrays.toString(values));
```

### B7 — Prefix Query

```java
long[] prefix = {0, 4, 3, 8, 10};
System.out.println(prefix[4] - prefix[1]);
```

### B8 — Kadane on Negatives

```java
int[] values = {-4, -1, -7};
long best = values[0];
long ending = values[0];
for (int i = 1; i < values.length; i++) {
    ending = Math.max(values[i], ending + values[i]);
    best = Math.max(best, ending);
}
System.out.println(best);
```

### B9 — Jagged Array

```java
int[][] rows = new int[3][];
rows[0] = new int[1];
rows[1] = new int[3];
rows[2] = new int[0];
System.out.println(rows.length + " " + rows[1].length);
```

### B10 — Primitive Array with `asList`

```java
int[] values = {1, 2, 3};
java.util.List<int[]> list = java.util.Arrays.asList(values);
System.out.println(list.size() + " " + list.get(0).length);
```

### B11 — Binary Search Contract

```java
int[] values = {2, 4, 6, 8};
System.out.println(java.util.Arrays.binarySearch(values, 6));
System.out.println(java.util.Arrays.binarySearch(values, 5));
```

### B12 — Postfix Indexing

```java
int[] values = {10, 20, 30};
int index = 0;
values[index++] += 5;
System.out.println(index + " " + values[0]);
```

## C. Debug the Code

1. **Foundation:** Fix a forward traversal that uses `index <= values.length`.
2. **Foundation:** Fix `System.out.println(values)` so it displays one-dimensional contents.
3. **Foundation:** A method returns `input` as a “copy.” Make it independent.
4. **Interview Core:** A reverse loop is `for (int i = values.length; i >= 0; i--)`. Correct both boundaries.
5. **Interview Core:** A sorted two-sum method calculates `int sum = values[left] + values[right]` for unrestricted integers. Repair numeric safety.
6. **Interview Core:** A compaction loop increments `write` even when the value is rejected. Restore the invariant.
7. **Interview Core:** A fixed window subtracts `values[right - k + 1]`. Fix the outgoing index.
8. **Interview Core:** Kadane returns `0` for `[-8, -3]`. Correct its initialization.
9. **Interview Core:** A difference update for inclusive `[left, right]` subtracts at `difference[right]`. Correct the stop boundary.
10. **Interview Core:** Cyclic placement hangs on `[1, 1]`. Add the duplicate guard.
11. **Interview Core:** `Arrays.sort(intervals, (x, y) -> x[0] - y[0])` fails for extreme starts. Correct the comparator.
12. **SDE-2 Follow-up:** A square-matrix rotation accepts jagged rows and later fails halfway through mutation. Validate before modifying any cell.

## D. Focused Coding Tasks

1. **Foundation:** Return the minimum and maximum of a non-empty array in one traversal.
2. **Foundation:** Reverse a half-open subrange `[left, right)` in place.
3. **Foundation:** Return an independent deep copy of an `int[][]`, preserving `null` rows.
4. **Foundation:** Insert a value into a partially filled array with logical size and capacity checks.
5. **Interview Core:** Remove every occurrence of a target in place and return the new logical size.
6. **Interview Core:** Deduplicate a sorted array, allowing each value at most twice.
7. **Interview Core:** Return original indexes for two-sum on an unsorted array.
8. **Interview Core:** Compute maximum water container area without overflow.
9. **Interview Core:** Return the minimum-length subarray whose sum reaches a target for positive values.
10. **Interview Core:** Return maximum-subarray sum and half-open indexes, preferring the shortest range on ties.
11. **Interview Core:** Count subarrays with sum equal to a target when negatives are allowed.
12. **Interview Core:** Build an immutable range-sum query object.
13. **Interview Core:** Apply offline inclusive range additions.
14. **Interview Core:** Return product-except-self without division.
15. **Interview Core:** Merge closed intervals without modifying input rows.
16. **Interview Core:** Rotate a square matrix clockwise in place.
17. **SDE-2 Follow-up:** Find the first missing positive while documenting mutation and space trade-offs.
18. **SDE-2 Follow-up:** Implement a randomized test that compares optimized maximum-subarray sum with the quadratic baseline.

## E. Interview Follow-Ups

1. The input is sorted but must not be modified. How does that affect two-sum choices?
2. The input is streamed and cannot be retained. Which array patterns remain possible?
3. The same immutable array receives one million range-sum queries. What API would you expose?
4. Range updates and queries are interleaved. Why is a difference array insufficient?
5. Values may be negative. Which sliding-window assumption fails?
6. The method may run concurrently. Which kinds of state should remain local or immutable?
7. The array has 500 million integers. Discuss memory, locality, copying, and boxing.
8. The interviewer asks for “in place.” Which questions must you ask before claiming `O(1)` space?

## F. Cumulative Assessments

### Assessment 1 — Foundations to Compaction

Design `stableRemove(int[] values, IntPredicate remove)` and return the new logical size. Explain aliasing, mutation, stability, boxed versus primitive alternatives, and test cases.

### Assessment 2 — Range Analytics

Given daily signed balance changes, support immutable range-sum queries, count ranges equal to a target, and report the best non-empty range. Choose separate structures or algorithms and justify each.

### Assessment 3 — Constraint-Driven Choice

For each requirement—sorted pair, bounded missing value, interval consolidation, nearest greater value, and online range updates—select a pattern and state its proof obligation.

## Final Readiness Assessment

Implement and explain a method that receives possibly overlapping closed intervals and a query window. It must preserve inputs, merge coverage, clip it to the query window, return total covered length using `long`, and define whether touching intervals merge. Provide a baseline, optimized complexity, invariants, overflow analysis, and a failure-focused test matrix.

## G. Sorting and Selection Implementation Lab

19. **Foundation:** Dry-run insertion sort on `[4, 2, 2, 3]`. Identify the comparison that preserves the two equal values' relative order.
20. **Interview Core:** Implement stable merge sort for records `(score, originalPosition)`. Your tests must prove stability, not merely sorted keys.
21. **Interview Core:** Implement three-way quicksort and label the four partition regions before and after each branch. Test all-equal and duplicate-heavy inputs.
22. **Interview Core:** Implement counting sort for signed integers only when the caller supplies a safe inclusive range. Reject an overflowing or excessive range before allocation.
23. **Interview Core:** Implement iterative randomized quickselect with a zero-based kth-smallest contract. Preserve caller input and reject an invalid rank.
24. **SDE-2 Follow-up:** Build deterministic randomized differential tests: compare both comparison sorts with `Arrays.sort`, and every quickselect rank with a sorted clone. Explain why a fixed seed helps diagnosis but does not prove correctness.
