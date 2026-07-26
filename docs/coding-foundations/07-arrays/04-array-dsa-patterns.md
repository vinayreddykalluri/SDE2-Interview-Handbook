# Array DSA Pattern Catalog

Strong candidates map constraints and requested output to reusable patterns, then state the invariant that makes the selected pattern correct. This catalog covers the primary patterns used with arrays, intervals, and matrices.

## Pattern-selection map

~~~mermaid
flowchart TD
    A["Array problem"] --> B{"Contiguous range?"}
    B -- Yes --> C{"Fixed size?"}
    C -- Yes --> FW["Fixed sliding window"]
    C -- No --> D{"Validity changes monotonically?"}
    D -- Yes --> VW["Variable sliding window"]
    D -- No --> PS["Prefix state and map"]
    B -- No --> E{"Sorted or sortable?"}
    E -- Yes --> F{"Pair or partition relation?"}
    F -- Yes --> TP["Two pointers or sort and scan"]
    F -- No --> BS["Binary search or ordered scan"]
    E -- No --> G{"Values map to indices?"}
    G -- Yes --> CP["Cyclic placement or counting"]
    G -- No --> H{"Nearest greater or window extreme?"}
    H -- Yes --> MD["Monotonic stack or deque"]
    H -- No --> RW["Linear scan, hashing, or compaction"]
~~~

## Before choosing a pattern

Extract:

- length and value bounds;
- sorted, partially sorted, or unsorted input;
- contiguous subarray versus subsequence;
- one answer versus all answers;
- duplicates and stability;
- negative values;
- online versus offline input;
- whether mutation is allowed;
- expected time, space, and output size.

A pattern is valid because of a property. If the property is absent, the pattern can return a plausible but incorrect answer.

## 1. Linear scan with compressed state

**Use when:** the processed prefix can be summarized by a fixed number of values.

**Examples:** extrema, one stock trade, majority candidate, maximum subarray, running balance.

Maximum subarray recurrence:

~~~java
long ending = values[0];
long best = values[0];
for (int i = 1; i < values.length; i++) {
    ending = Math.max(values[i], ending + values[i]);
    best = Math.max(best, ending);
}
~~~

**Invariant:** <code>ending</code> is the best nonempty subarray ending at the current index; <code>best</code> is the best seen anywhere.

**Complexity:** <code>O(n)</code> time and <code>O(1)</code> space.

**Failure mode:** initializing to zero when a nonempty all-negative answer is required.

## 2. Read-write compaction

**Use when:** filtering, deduplicating, moving values, or preserving a stable retained order in place.

~~~text
[ final output | processed discardable area | unread input ]
0              write                      read
~~~

**Invariant:** <code>[0, write)</code> is exactly the correct output derived from <code>[0, read)</code>.

**Examples:** remove value, deduplicate sorted input, move zeroes, retain valid records.

**Complexity:** <code>O(n)</code> time and <code>O(1)</code> extra space.

## 3. Opposing two pointers

**Use when:** sorted order lets a comparison discard every pair using one endpoint.

~~~java
int left = 0;
int right = values.length - 1;
while (left < right) {
    long sum = (long) values[left] + values[right];
    if (sum == target) return new int[] {left, right};
    if (sum < target) left++;
    else right--;
}
~~~

**Invariant:** no discarded pair can satisfy the target.

**Examples:** pair sum, three sum after fixing one value, palindrome checks, container area, trapping rainwater.

**Failure mode:** applying sorted-pointer reasoning to unsorted data.

## 4. Same-direction fast and slow pointers

**Use when:** one pointer discovers input and another owns the output frontier or maintains a fixed gap.

**Examples:** stable filtering, duplicate limits, kth from a boundary, merging into spare suffix capacity.

When merging into spare capacity, work from the end so unread values are not overwritten.

## 5. Fixed-size sliding window

**Use when:** every candidate is a contiguous range of exactly <code>k</code> elements.

~~~java
long window = 0;
for (int i = 0; i < k; i++) window += values[i];
long best = window;

for (int right = k; right < values.length; right++) {
    window += values[right];
    window -= values[right - k];
    best = Math.max(best, window);
}
~~~

**Invariant:** after each update, the summary describes exactly the current size-<code>k</code> window.

**Complexity:** <code>O(n)</code>. Validate <code>1 <= k <= n</code>.

## 6. Variable-size sliding window

**Use when:** the range is contiguous and advancing the left boundary can monotonically restore validity.

~~~java
int left = 0;
long sum = 0;
for (int right = 0; right < values.length; right++) {
    sum += values[right];
    while (sum >= target) {
        record(left, right);
        sum -= values[left++];
    }
}
~~~

**Examples:** minimum positive-sum window, unique-character range, at-most-k frequency constraints.

**Complexity:** <code>O(n)</code> because each pointer advances at most <code>n</code> times.

**Failure mode:** arbitrary negative values can destroy monotonic validity. Use prefix-state methods instead.

## 7. Prefix sums

**Use when:** many immutable range sums or range properties can be expressed as prefix differences.

~~~text
prefix[0] = 0
prefix[i + 1] = prefix[i] + values[i]
sum(left, right) = prefix[right] - prefix[left]
~~~

Build in <code>O(n)</code>, query a half-open range in <code>O(1)</code>. Prefer <code>long[]</code> for numeric safety.

## 8. Prefix state plus hashing

**Use when:** an earlier prefix determines a current exact-sum, balance, or longest-range answer.

~~~text
prefix[j] - prefix[i] = target
prefix[i] = prefix[j] - target
~~~

Store earlier prefix frequencies or earliest positions. Seed zero prefix state so ranges beginning at index zero are recognized.

**Examples:** subarray sum k, equal zero/one count, longest target-sum range.

**Complexity:** expected <code>O(n)</code> time and <code>O(n)</code> space.

## 9. Prefix and suffix summaries

**Use when:** the answer at index <code>i</code> depends on everything before and after it.

**Examples:** product except self, left/right maxima, partition points, per-index boundaries.

Product except self can write prefix products into the output, then multiply by one running suffix product. This uses <code>O(1)</code> auxiliary space beyond output and naturally handles zero without division.

## 10. Difference arrays

**Use when:** many additive range updates are applied before final values are needed.

For inclusive update <code>[left, right]</code>:

~~~text
difference[left] += delta
difference[right + 1] -= delta, when right + 1 is valid
~~~

One prefix pass reconstructs final values.

**Complexity:** <code>O(updates + n)</code>, replacing worst-case <code>O(updates * n)</code> work.

The two-dimensional version marks four rectangle corners and reconstructs with 2D prefix accumulation.

## 11. Sorting plus scanning

**Use when:** order need not be preserved and sorting exposes duplicates, adjacency, overlap, or nearest relationships.

**Examples:** merge intervals, three sum, minimum difference, meeting conflicts, global deduplication.

**Complexity:** usually <code>O(n log n)</code> including sort.

State whether input is mutated. Preserve original indices by sorting value-index records.

## 12. Partitioning and Dutch national flag

**Use when:** values must be grouped around predicates or a pivot and internal order is unimportant.

~~~text
[ less | equal | unknown | greater ]
0       low     middle    high      n
~~~

Rules:

- less: swap low and middle, advance both;
- equal: advance middle;
- greater: swap middle and high, decrement high, and reprocess the incoming unknown value.

**Examples:** sort colors, quicksort partition, category grouping.

**Complexity:** <code>O(n)</code> time and <code>O(1)</code> space.

## 13. Cyclic placement and index-as-bucket

**Use when:** length is <code>n</code>, values occupy a compact domain such as <code>[1, n]</code>, and mutation is allowed.

Map value <code>v</code> to index <code>v - 1</code>. Swap until each in-range value is at its canonical index or its duplicate is already there.

**Examples:** first missing positive, disappeared numbers, misplaced permutation values.

**Proof:** every successful swap finalizes at least one value, so total swaps are linear.

**Failure mode:** omit the duplicate guard and loop forever.

## 14. Binary search

**Use when:** sorted values or a monotonic predicate let one contiguous side be discarded.

Lower-bound invariant:

~~~text
indices before left are definitely too small
indices from right onward are possible answers
active interval is [left, right)
~~~

~~~java
int left = 0;
int right = values.length;
while (left < right) {
    int middle = left + (right - left) / 2;
    if (values[middle] < target) left = middle + 1;
    else right = middle;
}
return left;
~~~

Binary search on the answer also needs known bounds and a monotonic feasibility predicate. Include predicate cost in total complexity.

## 15. Monotonic stack

**Use when:** finding nearest greater/smaller boundaries or spans where a new value permanently invalidates weaker candidates.

**Examples:** next greater, daily temperatures, histogram rectangle, stock span, subarray minimum contributions.

Store indices for distances, expiration, and duplicate ownership.

**Complexity:** <code>O(n)</code> because each index is pushed once and popped at most once.

## 16. Monotonic deque

**Use when:** every fixed window needs its maximum or minimum.

For window maximum, maintain:

- increasing indices;
- decreasing corresponding values;
- only indices inside the current window;
- the current maximum at the front.

Expired indices leave the front; dominated values leave the back.

**Complexity:** <code>O(n)</code> total.

## 17. Intervals and sweep lines

Intervals require endpoint semantics before coding.

### Merge intervals

Sort by start. Maintain one active interval. Extend it on overlap; otherwise emit it and start a new active interval.

For closed ranges, <code>[1, 3]</code> and <code>[3, 5]</code> overlap. For half-open ranges they do not.

### Sweep line

Turn ranges into start/end events, sort them, and maintain active state.

**Examples:** concurrent meetings, capacity, covered length, skyline changes.

Tie ordering is part of correctness.

## 18. Matrix patterns

A matrix is often an implicit graph or coordinate-transformation problem.

Primary patterns:

- row/column scans;
- spiral boundary traversal;
- transpose plus row reversal for square rotation;
- top-right search in row-and-column sorted data;
- grid BFS/DFS;
- two-dimensional prefix sums;
- row/column dynamic programming.

Always validate rectangular or square shape when required.

## 19. Selection and top-k

| Method | Time | Space | Best use |
|---|---:|---:|---|
| Full sort | <code>O(n log n)</code> | Sort-dependent | Sorted output also useful |
| Heap of k | <code>O(n log k)</code> | <code>O(k)</code> | Streaming or immutable input |
| Quickselect | Expected <code>O(n)</code>, worst <code>O(n^2)</code> | Usually <code>O(1)</code> | One offline rank, mutation allowed |

State worst-case behavior and mutation rather than claiming quickselect is always best.

## 20. Pattern combinations

| Problem | Combined patterns |
|---|---|
| Three sum | Sort + fixed index + opposing pointers |
| Longest target-sum range | Prefix sum + earliest-index map |
| Sliding-window maximum | Fixed window + monotonic deque |
| First missing positive | Domain bound + cyclic placement |
| Merge intervals | Sort + active-range scan |
| Search rotated array | Binary search + partition reasoning |
| Product except self | Prefix + suffix products |
| Trapping rainwater | Opposing pointers + boundary maxima |
| Maximum rectangle in matrix | Row compression + histogram stack |
| Range additions | Difference array + prefix reconstruction |

## 21. Pattern decision Q&A

### Why not use sliding window for every subarray problem?

Variable windows need a monotonic validity relationship. Negative values commonly break it; prefix-state techniques retain the necessary history.

### When should input be sorted?

When order creates a useful invariant and <code>O(n log n)</code> fits. Discuss mutation and original-index preservation.

### Hashing or sorting for two sum?

Hashing gives expected <code>O(n)</code> time and <code>O(n)</code> space. Sorting plus pointers costs <code>O(n log n)</code>, may use less auxiliary space, and needs index preservation if indices are returned.

### Prefix sum or difference array?

Prefix sums accelerate range queries on mostly static values. Difference arrays accelerate a batch of range updates before reconstruction.

### Stack or deque?

A monotonic stack resolves one-direction nearest boundaries. A deque also expires old candidates from the front for moving windows.

### Why can nested loops be linear?

Each pointer may advance only <code>n</code> times, or each element may enter and leave a structure once. Count aggregate operations, not indentation.

## 22. Practice by pattern

| Pattern | Starter | Intermediate | Advanced |
|---|---|---|---|
| Scan | Maximum element | Best stock trade | Maximum product subarray |
| Read-write | Remove value | Deduplicate | Stable compaction |
| Two pointers | Reverse | Pair sum | Three sum |
| Fixed window | Max sum size k | Permutation windows | Window statistics |
| Variable window | Min positive sum | Longest unique range | Budgeted replacement |
| Prefix + map | Range sums | Subarray sum k | Longest balanced range |
| Difference | Range additions | Flight bookings | 2D updates |
| Partition | Move zeroes | Sort colors | Quickselect |
| Cyclic placement | Missing number | Disappeared values | First missing positive |
| Binary search | Exact search | First/last | Minimum feasible answer |
| Stack | Next greater | Temperatures | Histogram |
| Deque | Window maximum | Bounded difference | Constrained prefixes |
| Intervals | Merge | Insert | Capacity sweep |
| Matrix | Transpose | Spiral | Maximum rectangle |

## Runnable reference

See [ArrayDsaPatterns.java](https://github.com/vinayreddykalluri/SDE2-Interview-Handbook/blob/master/examples/java/src/main/java/io/github/vinayreddykalluri/interviewhandbook/codingfoundations/arrays/ArrayDsaPatterns.java).

## 60-second revision

- Start from constraints, not a memorized problem name.
- Sliding windows need contiguous ranges and monotonic movement.
- Prefix states turn range properties into differences.
- Difference arrays batch range updates.
- Sorting creates order at an <code>O(n log n)</code> cost.
- Partition and cyclic placement use input as organized workspace.
- Binary search requires a monotonic decision.
- Monotonic structures discard candidates that cannot win.
- State duplicate, endpoint, mutation, and overflow rules.
