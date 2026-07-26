# Sliding Windows and Aggregate Work

A window is a contiguous index range whose state can be updated as its boundaries move. The central optimization is reuse: remove what leaves, add what enters, and avoid recomputing the whole range.

## 4.1 Recognizing a window problem

Ask whether the problem contains all three signals:

1. the answer concerns a **contiguous** subarray or substring;
2. neighboring candidates overlap heavily;
3. the needed state can be updated when one endpoint moves.

If the selection is not contiguous, a window may be the wrong model. If deleting the left item cannot update the state efficiently, a different technique may be needed.

## 4.2 Fixed-size window

Problem: maximum sum of exactly `width` consecutive elements.

```java
static long maxFixedWindowSum(int[] values, int width) {
    if (width <= 0 || width > values.length) {
        throw new IllegalArgumentException("invalid width");
    }
    long windowSum = 0;
    for (int index = 0; index < width; index++) {
        windowSum += values[index];
    }
    long best = windowSum;
    for (int right = width; right < values.length; right++) {
        windowSum += values[right];
        windowSum -= values[right - width];
        best = Math.max(best, windowSum);
    }
    return best;
}
```

![Fixed and variable sliding-window state transitions](content/volumes/05-loop-mastery-and-index-calculations/assets/05-sliding-window-state.png)

### Window meaning

After processing `right`, the active inclusive window is `[right - width + 1, right]`. The departing index is `right - width`. A half-open description of the same active range is `[right - width + 1, right + 1)`.

### Dry run

For `[4, -1, 2, 10, -3]`, width 3:

| active values | update | sum | best |
|---|---|---:|---:|
| `[4,-1,2]` | initialize | 5 | 5 |
| `[-1,2,10]` | `+10 -4` | 11 | 11 |
| `[2,10,-3]` | `-3 -(-1)` | 9 | 11 |

Time is `O(n)`, not `O(n * width)`. Each value is added once and, after initialization, removed at most once.

### Frequent off-by-one error

Subtracting `values[right - width + 1]` removes the new window's first element rather than the old departing element. Draw the old and new ranges before writing the index expression.

## 4.3 Variable-size window

Variable windows expand a right boundary and shrink a left boundary until a validity rule is restored.

Problem: longest subarray with at most `k` distinct values.

```java
static int longestAtMostKDistinct(int[] values, int k) {
    if (k < 0) {
        throw new IllegalArgumentException("k must be nonnegative");
    }
    java.util.Map<Integer, Integer> frequency = new java.util.HashMap<>();
    int left = 0;
    int best = 0;
    for (int right = 0; right < values.length; right++) {
        frequency.merge(values[right], 1, Integer::sum);
        while (frequency.size() > k) {
            int departing = values[left++];
            int remaining = frequency.get(departing) - 1;
            if (remaining == 0) {
                frequency.remove(departing);
            } else {
                frequency.put(departing, remaining);
            }
        }
        best = Math.max(best, right - left + 1);
    }
    return best;
}
```

### Internal state

- `[left, right]` is the active window after shrinking.
- `frequency` describes exactly that window.
- `frequency.size() <= k` is the validity condition.
- zero-frequency keys must be removed or `size()` stops representing distinct values.

### Dry run for `[1,2,1,2,3]`, `k = 2`

| right | add | window before/after shrink | frequency | best |
|---:|---:|---|---|---:|
| 0 | 1 | `[0,0]` | `{1=1}` | 1 |
| 1 | 2 | `[0,1]` | `{1=1,2=1}` | 2 |
| 2 | 1 | `[0,2]` | `{1=2,2=1}` | 3 |
| 3 | 2 | `[0,3]` | `{1=2,2=2}` | 4 |
| 4 | 3 | shrink to `[3,4]` | `{2=1,3=1}` | 4 |

## 4.4 Why the nested loop is still linear

The `while` loop is nested syntactically, but `left` never moves backward. Across the whole call:

- `right` advances at most `n` times;
- `left` advances at most `n` times;
- each map increment/decrement corresponds to one endpoint movement.

Expected time is `O(n)` under the usual HashMap model, with `O(min(n, distinct values))` state. This is aggregate analysis.

![Counting total pointer movement instead of multiplying visible loop bounds](content/volumes/05-loop-mastery-and-index-calculations/assets/06-aggregate-pointer-movement.png)

Do not say “nested loops are `O(n^2)`” without counting how often the inner body can execute across the complete method.

## 4.5 Monotonicity: the permission to shrink

A standard variable window needs a monotone relationship: when the window is invalid, moving `left` forward must move state predictably toward validity; once a start is discarded for the current right endpoint, it should not become needed again before right advances.

For “at most K distinct,” removing items cannot increase the number of distinct values. The rule is monotone.

For “sum at least target” with **positive** values, adding on the right never decreases sum and removing on the left never increases it. The rule is monotone.

With arbitrary negative values, those statements fail. A standard sum window can skip the optimum.

### Counterexample with negatives

For target 3 and `[2, -1, 2]`, expansion and shrinking are not governed by a monotone sum. More dramatic cases can require prefix sums with an ordered structure or monotonic deque. The important interview habit is to state the positivity assumption before using the template.

## 4.6 Shortest positive-sum window

```java
static int shortestAtLeastTarget(int[] positive, long target) {
    int left = 0;
    long sum = 0;
    int best = Integer.MAX_VALUE;
    for (int right = 0; right < positive.length; right++) {
        if (positive[right] <= 0) {
            throw new IllegalArgumentException("values must be positive");
        }
        sum += positive[right];
        while (sum >= target) {
            best = Math.min(best, right - left + 1);
            sum -= positive[left++];
        }
    }
    return best == Integer.MAX_VALUE ? 0 : best;
}
```

Update `best` **before** removing the left element, while the window is still known to be valid. Use `long` because many `int` values can overflow an `int` sum.

Define the target contract. If `target <= 0`, is the empty window allowed? This implementation should reject such a target or explicitly return zero; interviewers care that you noticed the ambiguity.

## 4.7 Counting valid windows

After shrinking to the smallest valid left boundary for an “at most” rule, every start in `[left, right]` produces a valid subarray ending at `right`. There are `right - left + 1` such starts.

```java
static long countAtMostKDistinct(int[] values, int k) {
    if (k < 0) return 0;
    java.util.Map<Integer, Integer> frequency = new java.util.HashMap<>();
    int left = 0;
    long count = 0;
    for (int right = 0; right < values.length; right++) {
        frequency.merge(values[right], 1, Integer::sum);
        while (frequency.size() > k) {
            int value = values[left++];
            int next = frequency.get(value) - 1;
            if (next == 0) frequency.remove(value);
            else frequency.put(value, next);
        }
        count += right - left + 1L;
    }
    return count;
}
```

Use `long`: an array has `n * (n + 1) / 2` subarrays, which exceeds `int` for sufficiently large `n`.

Exactly `k` distinct can be derived as:

```text
exactly(k) = atMost(k) - atMost(k - 1)
```

Define `atMost(-1) = 0` so `k = 0` behaves consistently.

## 4.8 Window state beyond sums

The updateable state may be:

- a running sum;
- a frequency map;
- a fixed alphabet count array;
- the number of violated constraints;
- a deque of candidates for a minimum or maximum;
- a bit mask for a small set of properties.

The window pattern stays the same, but the data structure must support entry and departure. A simple `HashSet` is insufficient when duplicate counts matter: removing one duplicate would incorrectly remove the value from the set.

## 4.9 Pair counting with aggregate movement

For a sorted array, count pairs whose distance is at most `limit`:

```java
static long countPairsWithinDistance(int[] sorted, int limit) {
    if (limit < 0) return 0;
    int left = 0;
    long pairs = 0;
    for (int right = 0; right < sorted.length; right++) {
        while ((long) sorted[right] - sorted[left] > limit) {
            left++;
        }
        pairs += right - left;
    }
    return pairs;
}
```

After shrinking, every `i` in `[left, right)` forms a valid pair `(i, right)`, hence `right - left` new pairs. Sortedness and nonnegative distance justify monotone movement.

For `[1,2,4,7]`, limit 3, the contributions are `0,1,2,1`, totaling 4.

## 4.10 Fixed window versus prefix sum

Both can answer range-sum questions, but their contracts differ:

| Need | Better fit |
|---|---|
| scan every window of one width once | rolling fixed window |
| many arbitrary range-sum queries | prefix sums |
| streaming last `k` values | rolling window plus queue/ring buffer |
| updates between queries | Fenwick/segment structure; later material |

Do not force sliding windows onto independent queries that do not arrive in traversal order.

## 4.11 Common failures

1. Using a window for noncontiguous selection.
2. Subtracting the wrong departing index.
3. Updating the answer before restoring validity in a maximum-valid-window problem.
4. Updating the minimum after invalidating a previously valid window.
5. Forgetting to delete zero-frequency keys.
6. Applying positive-sum logic to arbitrary negatives.
7. Recomputing the window aggregate inside every iteration.
8. Returning `int` when the count can be quadratic.
9. Claiming worst-case `O(n)` HashMap behavior without qualification.
10. Ignoring storage required to know which streaming item departs.

## 4.12 SDE-2 follow-ups

- **Streaming:** a fixed window must retain enough history to remove the departing item; state is often `O(k)`, not `O(1)`.
- **Unbounded cardinality:** an at-most-K map is bounded after restoration, but transient state and the active window still matter.
- **Cancellation:** a service traversal may periodically check interruption. Choose a cadence that bounds cancellation latency without checking on every trivial operation.
- **Ownership:** an API may stream results rather than allocate all windows.
- **Concurrency:** a stateful window is sequential across its boundary. Partitioning requires reconciliation logic.
- **Telemetry:** log sizes, counts, and duration, not full sensitive payloads.

## 4.13 Interview checkpoint

You should be able to derive the departing index in a fixed window, state the exact active interval, justify monotonicity for a variable window, count total endpoint movements, explain why negative values can invalidate a sum window, and choose `long` for aggregate counts. The next chapter applies the same range discipline to two-dimensional data.
