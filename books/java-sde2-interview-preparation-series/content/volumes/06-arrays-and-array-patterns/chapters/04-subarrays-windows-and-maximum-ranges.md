# Subarrays, Sliding Windows, and Maximum Ranges

Array interviews often stop being about a single element and start asking about a **contiguous range**. The most important first step is vocabulary:

- A **subarray** is contiguous: `[4, 1]` is a subarray of `[7, 4, 1, 9]`.
- A **subsequence** preserves order but may skip elements: `[7, 1, 9]` is a subsequence.
- A **subset** has no position or continuity requirement.

If a problem says “consecutive,” “continuous,” “window,” or gives endpoints, test whether it is a subarray problem.

## Build the Baseline Before Choosing a Pattern

For every start index, we can try every end index. Recomputing each sum from scratch takes cubic time. Carrying the running sum as the end advances removes one loop.

```java
static long maximumSubarrayBruteForce(int[] numbers) {
    if (numbers == null || numbers.length == 0) {
        throw new IllegalArgumentException("numbers must be non-empty");
    }

    long best = Long.MIN_VALUE;
    for (int start = 0; start < numbers.length; start++) {
        long current = 0;
        for (int end = start; end < numbers.length; end++) {
            current += numbers[end];
            best = Math.max(best, current);
        }
    }
    return best;
}
```

There are `n(n + 1) / 2` subarrays, so enumerating every subarray already requires quadratic work. That observation helps us ask the right follow-up: does the problem require every range, or only one optimum?

## Fixed-Size Sliding Window

Suppose we need the maximum sum of exactly `k` adjacent values. Neighboring windows overlap in `k - 1` positions. Reusing that overlap changes an `O(nk)` solution into `O(n)`.

```java
static long maximumFixedWindowSum(int[] numbers, int windowSize) {
    if (numbers == null || windowSize <= 0 || windowSize > numbers.length) {
        throw new IllegalArgumentException("invalid window");
    }

    long windowSum = 0;
    for (int index = 0; index < windowSize; index++) {
        windowSum += numbers[index];
    }

    long best = windowSum;
    for (int right = windowSize; right < numbers.length; right++) {
        windowSum += numbers[right];
        windowSum -= numbers[right - windowSize];
        best = Math.max(best, windowSum);
    }
    return best;
}
```

Dry run for `[2, 1, 5, 1, 3, 2]`, `k = 3`:

| Window | Update | Sum | Best |
|---|---|---:|---:|
| `[2, 1, 5]` | initial | 8 | 8 |
| `[1, 5, 1]` | `+1 -2` | 7 | 8 |
| `[5, 1, 3]` | `+3 -1` | 9 | 9 |
| `[1, 3, 2]` | `+2 -5` | 6 | 9 |

![A fixed-size sliding window removes one value and adds one value](content/volumes/06-arrays-and-array-patterns/assets/06-sliding-window-state.png)

**Invariant:** before each comparison, `windowSum` equals the sum of the current `windowSize` positions.

## Variable-Size Window: Prove Monotonicity First

For a positive-integer array, increasing the right boundary never decreases the sum. Once the sum reaches a target, moving the left boundary is the only useful way to find a shorter valid window.

```java
static int minimumLengthAtLeast(int[] positiveNumbers, long target) {
    int best = Integer.MAX_VALUE;
    int left = 0;
    long sum = 0;

    for (int right = 0; right < positiveNumbers.length; right++) {
        sum += positiveNumbers[right];

        while (sum >= target) {
            best = Math.min(best, right - left + 1);
            sum -= positiveNumbers[left++];
        }
    }
    return best == Integer.MAX_VALUE ? 0 : best;
}
```

The positivity requirement is not decoration. With negative values, removing the leftmost value may increase the sum; the grow/shrink decisions are no longer monotonic. For arbitrary integers, consider prefix sums plus a map, a deque, or another pattern justified by the exact question.

> **Interview checkpoint:** Do not say “sliding window works for subarrays.” State the property that makes boundary movement safe.

## Kadane's Algorithm: Best Sum Ending Here

For the maximum-sum subarray, the reusable state is smaller than the whole window. At index `i`, the best subarray ending at `i` either:

1. starts at `i`, or
2. extends the best subarray ending at `i - 1`.

That gives `endingHere = max(value, endingHere + value)`.

```java
record SubarrayResult(long sum, int start, int endExclusive) {}

static SubarrayResult maximumSubarray(int[] numbers) {
    if (numbers == null || numbers.length == 0) {
        throw new IllegalArgumentException("numbers must be non-empty");
    }

    long endingHere = numbers[0];
    long best = numbers[0];
    int candidateStart = 0;
    int bestStart = 0;
    int bestEndExclusive = 1;

    for (int index = 1; index < numbers.length; index++) {
        if (numbers[index] > endingHere + numbers[index]) {
            endingHere = numbers[index];
            candidateStart = index;
        } else {
            endingHere += numbers[index];
        }

        if (endingHere > best) {
            best = endingHere;
            bestStart = candidateStart;
            bestEndExclusive = index + 1;
        }
    }
    return new SubarrayResult(best, bestStart, bestEndExclusive);
}
```

Starting with `0` is a common bug because it incorrectly permits an empty range. On `[-5, -2, -8]`, the correct non-empty answer is `-2`, not `0`. `long` protects the accumulated sum from ordinary `int` overflow, provided each value is promoted before arithmetic is lost.

## Maximum Product Needs Two States

A negative value can turn the smallest product into the largest. Track both extremes ending at the current index.

```java
static long maximumProductSubarray(int[] numbers) {
    long maximumEnding = numbers[0];
    long minimumEnding = numbers[0];
    long answer = numbers[0];

    for (int index = 1; index < numbers.length; index++) {
        long value = numbers[index];
        if (value < 0) {
            long temporary = maximumEnding;
            maximumEnding = minimumEnding;
            minimumEnding = temporary;
        }
        maximumEnding = Math.max(value, maximumEnding * value);
        minimumEnding = Math.min(value, minimumEnding * value);
        answer = Math.max(answer, maximumEnding);
    }
    return answer;
}
```

This code may still overflow `long` for unconstrained inputs. In an interview, ask for bounds before claiming numeric safety.

## Prefix Sum Plus Frequency Map

For “count subarrays whose sum equals `target`” with arbitrary positive, zero, and negative values, let `prefix` be the sum through the current index. A previous prefix of `prefix - target` identifies a valid subarray.

```java
static long countSubarraysWithSum(int[] numbers, long target) {
    java.util.Map<Long, Integer> frequencies = new java.util.HashMap<>();
    frequencies.put(0L, 1); // a range beginning at index 0

    long prefix = 0;
    long count = 0;
    for (int number : numbers) {
        prefix += number;
        count += frequencies.getOrDefault(prefix - target, 0);
        frequencies.merge(prefix, 1, Integer::sum);
    }
    return count;
}
```

The map is not merely a lookup optimization; it represents all earlier prefix states. Deep hash-table behavior belongs in the Hashing and Java Collections Internals volumes.

## Common Failures

| Failure | Why it breaks | Better habit |
|---|---|---|
| Treating subsequences as subarrays | Changes the search space | Write the continuity requirement explicitly |
| Using a variable window with negatives | Boundary decisions lose monotonicity | Prove why grow/shrink moves are safe |
| Initializing Kadane's answer to zero | Allows an empty result silently | Initialize from the first value |
| Storing sums in `int` | Valid inputs may overflow | Use `long` after checking constraints |
| Returning only a sum when indexes are requested | Loses reconstruction data | Track candidate and best boundaries |

## Quick Check and Practice

1. **Foundation:** How many non-empty subarrays does an array of length `n` contain?
2. **Foundation:** Why is `[1, 4]` not necessarily a subarray of `[1, 2, 3, 4]`?
3. **Interview Core:** State the fixed-window invariant in one sentence.
4. **Interview Core:** Give an input where a positive-only variable window fails after a negative number is introduced.
5. **Interview Core:** Modify Kadane's algorithm to prefer the shortest range when sums tie.
6. **SDE-2 Follow-up:** Return the maximum circular subarray sum without mutating the input. Explain the all-negative case.
7. **SDE-2 Follow-up:** Which requirements would make you reject an `O(n)` hash-map solution in favor of sorting or a different data structure?

## Transition

Sliding windows optimize a range that changes continuously. When a problem asks many range queries or many offline range updates, precomputed prefix, suffix, and difference state is usually a better model.
