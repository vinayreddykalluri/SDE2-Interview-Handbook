# Advanced Arrays: Interview Playbook

Advanced array interviews combine patterns, force explicit trade-offs, and test whether a candidate can move from brute force to a proven implementation.

## 1. SDE2 interview workflow

~~~mermaid
flowchart LR
    A["Clarify contract"] --> B["Derive baseline"]
    B --> C["Locate repeated work"]
    C --> D["Select pattern"]
    D --> E["State invariant"]
    E --> F["Code by regions"]
    F --> G["Trace edge cases"]
    G --> H["Analyze production trade-offs"]
~~~

### Clarify

Ask about empty input, negative values, duplicates, numeric limits, output shape, mutation, order preservation, constraints, and endpoint semantics.

### Establish a baseline

A brute-force solution gives a correctness reference and exposes repeated work. Explain it precisely, then optimize before spending interview time on unacceptable code.

### Name eliminated work

- windows avoid rescanning overlapping ranges;
- prefix sums avoid repeated range aggregation;
- hashing avoids searching all earlier states;
- sorting avoids comparing every pair;
- monotonic structures remove dominated candidates;
- cyclic placement avoids a presence set;
- binary search avoids testing every feasible answer.

### State an invariant

Describe exact finalized, active, and unknown regions. If this is unclear, pointer updates are not ready.

### Trace adversarial cases

Use empty, one-element, all-equal, sorted, reverse-sorted, all-negative, duplicate-heavy, no-answer, boundary-answer, and overflow cases.

## 2. Worked problem: first missing positive

For length <code>n</code>, the answer is in <code>[1, n + 1]</code>. Values outside <code>[1, n]</code> are irrelevant. Map value <code>v</code> to index <code>v - 1</code>.

~~~java
for (int i = 0; i < values.length; i++) {
    while (values[i] >= 1
            && values[i] <= values.length
            && values[values[i] - 1] != values[i]) {
        int target = values[i] - 1;
        swap(values, i, target);
    }
}
~~~

Scan for the first index where <code>values[i] != i + 1</code>.

**Correctness:** every successful swap places an in-range value in its canonical slot. Duplicate guards guarantee termination. The first canonical mismatch is the smallest absent positive.

**Complexity:** <code>O(n)</code> time, <code>O(1)</code> extra space, input mutated.

## 3. Worked problem: product except self

For index <code>i</code>:

~~~text
answer[i] = product before i * product after i
~~~

Write left products into output, then multiply by a running right product.

~~~java
long prefix = 1;
for (int i = 0; i < values.length; i++) {
    answer[i] = prefix;
    prefix *= values[i];
}

long suffix = 1;
for (int i = values.length - 1; i >= 0; i--) {
    answer[i] *= suffix;
    suffix *= values[i];
}
~~~

No division means zero values work naturally. Time is <code>O(n)</code>; auxiliary space beyond output is <code>O(1)</code>. Product overflow still needs a contract.

## 4. Worked problem: count subarrays with sum k

Negative values invalidate ordinary variable-window reasoning.

Let current prefix be <code>p</code>:

~~~text
current prefix - earlier prefix = target
earlier prefix = current prefix - target
~~~

~~~java
Map<Long, Integer> frequency = new HashMap<>();
frequency.put(0L, 1);

long prefix = 0;
long count = 0;
for (int value : values) {
    prefix += value;
    count += frequency.getOrDefault(prefix - target, 0);
    frequency.merge(prefix, 1, Integer::sum);
}
~~~

**Invariant:** before insertion, the map counts every earlier prefix. Query first so an empty subarray is not accidentally counted.

**Complexity:** expected <code>O(n)</code> time and <code>O(n)</code> space.

## 5. Worked problem: trapping rainwater

Water at an index is bounded by the smaller of maximum heights to its left and right.

If <code>leftMax <= rightMax</code>, the current left result is final because sufficient right support is known. Otherwise process the right.

~~~java
while (left <= right) {
    if (leftMax <= rightMax) {
        leftMax = Math.max(leftMax, heights[left]);
        water += leftMax - heights[left++];
    } else {
        rightMax = Math.max(rightMax, heights[right]);
        water += rightMax - heights[right--];
    }
}
~~~

**Complexity:** <code>O(n)</code> time and <code>O(1)</code> space.

## 6. Worked problem: search rotated sorted data

A rotated distinct sorted array contains two sorted portions. At every iteration, at least one half is sorted.

- Identify the sorted half.
- Test whether target lies within its ordered bounds.
- Keep that half if it can contain target; otherwise discard it.

**Invariant:** if target exists, it remains in the inclusive active interval.

With duplicates, equal left, middle, and right values may hide the sorted side. Shrinking boundaries preserves correctness but can degrade to <code>O(n)</code>.

## 7. Worked problem: merge intervals

Define closed versus half-open endpoints and whether touching intervals merge.

1. Sort by start, then end.
2. Maintain one active interval.
3. Extend it on overlap.
4. Otherwise emit it and start a new active interval.

**Invariant:** emitted intervals are final, sorted, disjoint, and cover the processed input before the active group.

**Complexity:** <code>O(n log n)</code> for sorting plus <code>O(n)</code> scanning.

## 8. Worked problem: rotate a square matrix

Clockwise coordinate mapping:

~~~text
(row, column) -> (column, n - 1 - row)
~~~

An in-place decomposition:

1. transpose across the main diagonal;
2. reverse every row.

Validate square shape. Time is <code>O(n^2)</code>; extra space is <code>O(1)</code>.

## 9. Advanced pattern combinations

### Maximum rectangle in a binary matrix

Convert each row into histogram heights and solve largest histogram rectangle with a monotonic stack. Total time is <code>O(rows * columns)</code>.

### Shortest qualifying subarray with negative values

Build prefix sums and maintain increasing candidate prefixes in a monotonic deque. Pop the front when the target is achieved and the back when a newer prefix dominates. Total time is <code>O(n)</code>.

### Median of two sorted arrays

Binary-search a partition in the smaller array so all left-partition values are at most all right-partition values. Time is <code>O(log min(m, n))</code>.

### Count range sums

Convert subarray sums to prefix differences. Count earlier prefixes in a numeric range using merge-sort counting, a Fenwick tree with coordinate compression, or an ordered multiset. Typical time is <code>O(n log n)</code>.

### Dynamic range queries

Use a Fenwick tree for prefix aggregates and point updates, a segment tree for configurable range operations, or a sparse table for immutable idempotent queries.

## 10. Trade-off table

| Goal | Approach | Time | Extra space | Precondition |
|---|---|---:|---:|---|
| Pair sum, unsorted | Hash map | Expected <code>O(n)</code> | <code>O(n)</code> | Hashing allowed |
| Pair sum, sorted | Two pointers | <code>O(n)</code> | <code>O(1)</code> | Sorted |
| Immutable range sums | Prefix sum | Build <code>O(n)</code>, query <code>O(1)</code> | <code>O(n)</code> | Static values |
| Offline range additions | Difference array | <code>O(n + updates)</code> | <code>O(n)</code> | Additive updates |
| Every window maximum | Monotonic deque | <code>O(n)</code> | <code>O(k)</code> | Fixed windows |
| Streaming top k | Heap | <code>O(n log k)</code> | <code>O(k)</code> | Comparator |
| Offline kth value | Quickselect | Expected <code>O(n)</code> | Usually <code>O(1)</code> | Mutation allowed |
| Missing value in domain | Cyclic placement | <code>O(n)</code> | <code>O(1)</code> | Mutation allowed |

## 11. Concept interview questions and answers

### Q1. Subarray versus subsequence?

A subarray is contiguous and described by boundaries. A subsequence preserves order but may skip values. Window and prefix-difference methods naturally target subarrays.

### Q2. Why can nested loops be linear?

Each pointer may only advance, or each element may enter and leave a structure once. Count aggregate transitions, not indentation.

### Q3. When is in-place not constant space?

When recursion consumes growing stack depth, slices are copied, output is hidden, or a library operation allocates auxiliary storage.

### Q4. Why use long prefix sums?

Many int values can sum beyond 32-bit range. Overflow corrupts equality, ordering, and hash keys.

### Q5. What breaks variable windows?

The validity predicate must move monotonically with pointer changes. Arbitrary negatives commonly violate that assumption.

### Q6. How do duplicates change binary search?

Define any, first, last, lower bound, or upper bound. In rotated data, duplicates can also hide which half is sorted.

### Q7. Why store monotonic indices?

Indices provide expiration, distance, span, and identity for equal values.

### Q8. Stable versus unstable partition?

Stable partition preserves relative order and often uses more memory or movement. Unstable partition can swap in constant space.

### Q9. When should input be sorted in place?

Only when mutation is allowed and original order is unnecessary. Otherwise copy or sort value-index records.

### Q10. What is output-sensitive complexity?

If output contains <code>r</code> items, writing it costs at least <code>Omega(r)</code>. Reporting all pairs cannot always be bounded only by input size.

### Q11. How do you avoid comparator overflow?

Use <code>Integer.compare</code> or <code>Long.compare</code>, not subtraction.

### Q12. How do you prove a pointer move is safe?

Show that every candidate discarded by the move cannot satisfy the goal, using order, monotonicity, or a boundary fact.

### Q13. Prefix versus suffix state?

Prefix state summarizes values before a boundary; suffix state summarizes values after it. Combining them avoids repeated scans.

### Q14. Why does cyclic placement terminate?

Every successful swap finalizes an in-range value, and duplicate guards prevent swapping equal values forever.

### Q15. Why is quickselect expected linear?

A representative pivot produces a geometric series of partition work while only one side continues. Adversarial pivots can cause quadratic time.

### Q16. Why is merge intervals primarily a sort-and-scan pattern?

Sorting creates ordered starts; one active coverage range then summarizes the processed suffix.

### Q17. What if input cannot fit in memory?

Consider streaming summaries, external sorting, chunking, compressed storage, or working sets bounded by <code>k</code>.

### Q18. How do you return original indices after sorting?

Sort value-index records or use a hash-based approach. Sorting raw values destroys identity.

### Q19. How do you handle concurrent mutation?

Prefer exclusive ownership or immutable snapshots. Otherwise design one coherent synchronization protocol for the full operation.

### Q20. What should an SDE2 candidate discuss after solving?

Contracts, numeric bounds, mutation, worst-case behavior, adversarial inputs, memory, API shape, observability, streaming, and concurrency.

## 12. Edge-case matrix

| Category | Cases |
|---|---|
| Size | empty, one, two, very large |
| Values | equal, zero, negative, mixed |
| Ordering | sorted, reverse, rotated, duplicate-heavy |
| Answer | beginning, middle, end, absent |
| Numeric | minimum int, maximum int, aggregate overflow |
| Ranges | empty, full, invalid endpoints |
| Windows | one, full length, zero, larger than input |
| Matrices | empty, one cell, one row, one column, jagged |
| Mutation | aliased input, immutable expectation, repeated call |

## 13. Preparation roadmap

### Stage 1: mechanics

Implement linear search, reverse, rotate, remove, merge sorted arrays, and matrix traversal. State an invariant for each.

### Stage 2: core recognition

Practice two sum, maximum subarray, fixed/variable windows, prefix queries, subarray sum k, sort colors, binary bounds, and merge intervals.

### Stage 3: advanced patterns

Practice first missing positive, product except self, rainwater, next greater, histogram rectangle, window maximum, rotated search, quickselect, and matrix rotation.

### Stage 4: SDE2 combinations

Practice maximum rectangle in a matrix, shortest subarray with negatives, count range sums, median of sorted arrays, online top k, dynamic range queries, and capacity sweep lines.

## 14. Mock interview rubric

| Area | Weak | Developing | Strong |
|---|---|---|---|
| Contract | Assumes behavior | Clarifies some cases | Defines inputs, outputs, mutation, bounds |
| Baseline | No correct path | Correct but vague | Correct with precise complexity |
| Pattern | Guesses | Needs hints | Derives from constraints |
| Invariant | Missing | Informal | Exact regions and state |
| Code | Major defects | Minor boundary issues | Clean and consistent |
| Trace | Happy path only | Some edges | Adversarial cases |
| Analysis | Incorrect | Big-O only | Time, space, output, worst case |
| SDE2 depth | None | One trade-off | API, scale, alternatives |

## 15. Seven-day revision loop

| Day | Focus | Deliverable |
|---|---|---|
| 1 | Storage, ranges, scans | Fundamentals from memory |
| 2 | Two pointers and windows | Four variants with validity rules |
| 3 | Prefix and difference | Equations before code |
| 4 | Sorting, partition, placement | Region diagrams |
| 5 | Binary and monotonic | Discard-safety proofs |
| 6 | Intervals and matrices | Two combined patterns |
| 7 | Timed mock | Explain, code, trace, review |

Track the failed reasoning step, not only the problem name.

## Runnable references

- [ArrayDsaPatterns.java](https://github.com/vinayreddykalluri/SDE2-Interview-Handbook/blob/master/examples/java/src/main/java/io/github/vinayreddykalluri/interviewhandbook/codingfoundations/arrays/ArrayDsaPatterns.java)
- [AdvancedArrayProblems.java](https://github.com/vinayreddykalluri/SDE2-Interview-Handbook/blob/master/examples/java/src/main/java/io/github/vinayreddykalluri/interviewhandbook/codingfoundations/arrays/AdvancedArrayProblems.java)

## Final revision sheet

- Clarify contiguity, order, duplicates, mutation, output, and bounds.
- Derive a baseline and name repeated work.
- Select patterns from properties.
- Draw finalized and unknown regions.
- State the invariant before coding.
- Use wider arithmetic for aggregates.
- Count sorting, copying, recursion, and output.
- Trace adversarial inputs.
- Discuss ownership, worst case, and scale.
