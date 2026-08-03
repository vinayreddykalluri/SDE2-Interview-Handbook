# 43. Arrays, Strings, Hashing, Two Pointers, Sliding Windows, and Prefix Sums

## Learning objectives

By the end of this chapter, you should be able to:

- select a sequence pattern from constraints and eliminated work;
- maintain correct two-pointer and sliding-window invariants;
- transform subarray sums into prefix-state lookups;
- use hashing with explicit equality, range, and complexity assumptions;
- apply binary search to sorted data or a monotone answer predicate; and
- implement Java 21 solutions that handle overflow, duplicates, empty input, and Unicode boundaries deliberately.

## Why this matters at SDE-2

Arrays and strings dominate coding screens because they expose reasoning without requiring much setup. The same small set of ideas solves a wide range of tasks: deduplication, rate-window analysis, contiguous aggregates, text scanning, sorted pair search, and event correlation.

At SDE-2, naming "sliding window" is not sufficient. You must explain why shrinking restores validity, why no candidate is skipped, whether negative values break monotonicity, and whether a map stores the earliest index, latest index, frequency, or count of prefix states. Those details are the algorithm.

> **Focused prerequisites:** Number Systems (`DSA-02`) establishes numeric boundaries, prefix sums under modulo, and safe index arithmetic. Continue through `DSA-05` to `DSA-08` for loops and indices, arrays, strings, and hashing. Use the series index PDF for the ordered filenames.

## First-principles model

An array offers indexed, fixed-length storage. A Java `String` is an immutable sequence of UTF-16 code units. Both make a sequence available in order. Most interview patterns compress information about a processed prefix so the algorithm does not rescan it.

Two pointers replace repeated pair or range exploration when order lets one pointer movement eliminate a set of candidates. A sliding window maintains state for a contiguous interval. Prefix sums answer a range query by subtracting two summaries. Hashing turns a previously seen value or state into an expected constant-time lookup.

The common proof shape is a partition:

- a processed region whose answer-relevant information is summarized;
- a current boundary or window with a precisely stated meaning; and
- an unprocessed region that will be considered exactly once or safely skipped.

> **Specification boundary:** Java guarantees array indexing and `String` UTF-16 semantics. It does not guarantee worst-case O(1) for every `HashMap` operation, a particular hash-table layout, or one Unicode code point per `char`.

## Core terminology

- **Subarray/substring:** contiguous interval.
- **Subsequence:** elements retain relative order but need not be contiguous.
- **Frequency map:** key to occurrence count.
- **Index map:** key to an earliest, latest, or otherwise selected position.
- **Opposing pointers:** boundaries move inward, commonly on sorted data.
- **Same-direction pointers:** read/write or left/right positions move forward.
- **Fixed window:** interval length is constant.
- **Variable window:** boundary moves to preserve a validity predicate.
- **Prefix sum:** sum of elements strictly before an index in the common `prefix[0] = 0` convention.
- **Difference array:** boundary updates later reconstructed by a prefix sum.
- **Monotone predicate:** once true (or false), remains so across an ordered search domain.

## Detailed mechanics

### Pattern-selection map

| Signal | Candidate technique | Required proof |
|---|---|---|
| Need membership, duplicates, complement, or frequency | Hash set/map | Stored state answers future query |
| Sorted pairs or symmetric comparison | Opposing pointers | Pointer move discards impossible pairs |
| In-place filtering or compaction | Read/write pointers | Prefix contains exactly accepted elements |
| Best contiguous range with monotone validity | Variable window | Shrink restores validity and no start is revisited |
| Aggregate for many ranges | Prefix sum | Range equals difference of prefix states |
| Count subarrays with exact aggregate | Prefix plus hash frequencies | Earlier required prefix is counted |
| Sorted boundary or monotone feasibility | Binary search | Answer remains in candidate interval |

Do not select only from a noun in the prompt. "Subarray" suggests a window, but exact sums with negative values often require prefix hashing because adding a value does not move the sum monotonically.

### Arrays and strings as state spaces

Indexes are half-open boundaries whenever possible. Interval `[left, right)` has length `right - left`, is empty when equal, and composes cleanly. Java APIs vary, so translate carefully: `Arrays.copyOfRange` is half-open, while some problem statements use inclusive endpoints.

For read/write compaction, maintain: `values[0..write)` contains exactly the accepted elements from `values[0..read)` in stable order. When the read value qualifies, write it and increment `write`. Every input is examined once, giving O(n) time and O(1) auxiliary space.

String indexing needs a declared unit. A `char` solution counts UTF-16 code units and may split supplementary code points. If the task means Unicode code points, convert with `text.codePoints().toArray()` or advance using `codePointAt` and `Character.charCount`. If it means user-perceived graphemes, code-point processing is still insufficient and a Unicode boundary implementation is needed.

### Hashing patterns

Hashing stores the minimum past state necessary for future decisions:

- **set:** has this key appeared?
- **frequency map:** how many times has it appeared?
- **index map:** which position should be used?
- **grouping map:** which values share a canonical signature?
- **prefix-frequency map:** how many earlier prefixes enable the current range?

For two-sum on unsorted data, before inserting `x`, look for `target - x`. This prevents pairing an element with itself unless an earlier equal value exists. If original indices matter, sorting loses them unless pairs of value and index are retained.

Map update order and duplicate policy matter. Longest-substring algorithms often need the latest occurrence so `left` jumps forward. Longest-subarray prefix algorithms often need the earliest index to maximize length. Counting algorithms need every occurrence as a frequency, not one index.

Use `long` for sums and complements when n and element magnitudes can overflow `int`. Boxed `Long` and `Integer` keys allocate or reuse wrappers according to implementation; the asymptotic map model remains expected O(1), but memory can be significant.

### Opposing and same-direction pointers

For a sorted two-sum search, compare `values[left] + values[right]` with target. If the sum is too small, every pair using the current left with an index no larger than right is also too small, so incrementing left is safe. The symmetric argument justifies decrementing right when the sum is too large.

This elimination proof depends on sorted order. Without it, moving a pointer has no such implication. Sorting first costs O(n log n), may mutate input, and complicates original-index output. A hash map offers expected O(n) time and O(n) space instead.

Same-direction fast/slow pointers appear in compaction and linked-list cycle detection. In arrays, name them `read` and `write` when that reflects meaning; semantic names make invariants easier to state.

### Sliding windows

A fixed-size window updates incrementally: add the entering element, remove the leaving element, and evaluate when length reaches k. This replaces O(k) work for each start with O(1) state updates, producing O(n) total time.

A variable window uses a validity condition:

```text
for each right:
    include right in state
    while window is invalid:
        remove left from state
        advance left
    evaluate the valid window
```

The invariant is that state describes exactly `[left, right]` and the window is valid after shrinking. Whether evaluating the current window finds an optimum depends on the problem. For "longest window with at most k distinct values," after restoring validity, the current left is the earliest valid start for that right, so it gives the longest valid window ending there.

Variable sum windows usually require nonnegative elements. With negatives, extending can reduce a sum and shrinking can increase it, so the needed monotonicity disappears. Prefix hashing, a monotonic deque over prefix sums, or another technique may apply instead.

### Prefix sums and difference arrays

Define `prefix[i]` as the sum of the first i elements:

```text
prefix[0] = 0
prefix[i + 1] = prefix[i] + values[i]
sum(left, rightExclusive) = prefix[rightExclusive] - prefix[left]
```

The leading zero removes special cases for ranges beginning at index zero. Building costs O(n); each range-sum query is O(1).

For an exact target subarray ending at current index with prefix `p`, an earlier prefix must equal `p - target`. Store counts of earlier prefixes and seed frequency zero with one, representing the empty prefix. Query before incrementing the current prefix frequency if zero-length ranges are not allowed.

A difference array reverses the idea. To add delta to inclusive range `[left, right]`, apply `difference[left] += delta` and, if it exists, `difference[right + 1] -= delta`. One final prefix pass materializes all values. This is ideal for many offline range updates, not arbitrary online queries.

### Binary search in sequence problems

Use binary search for a sorted boundary, rotated-array partition with careful duplicate handling, or a monotone answer such as minimum feasible capacity. Do not binary-search a condition that can flip repeatedly. State whether the interval is closed or half-open and whether you seek any match, first true, last false, lower bound, or upper bound.

## Worked Java example

This Java 21 class provides three complementary templates: sorted two pointers, a variable character window, and prefix sum plus hashing.

```java
import java.util.HashMap;
import java.util.Map;

public final class SequencePatterns {
    public static int[] twoSumSorted(int[] values, long target) {
        int left = 0;
        int right = values.length - 1;
        while (left < right) {
            long sum = (long) values[left] + values[right];
            if (sum == target) return new int[] {left, right};
            if (sum < target) left++;
            else right--;
        }
        return new int[] {-1, -1};
    }

    public static int longestAtMostKDistinct(String text, int k) {
        if (k <= 0 || text.isEmpty()) return 0;
        Map<Character, Integer> frequencies = new HashMap<>();
        int left = 0;
        int best = 0;

        for (int right = 0; right < text.length(); right++) {
            char added = text.charAt(right);
            frequencies.merge(added, 1, Integer::sum);

            while (frequencies.size() > k) {
                char removed = text.charAt(left++);
                int remaining = frequencies.get(removed) - 1;
                if (remaining == 0) frequencies.remove(removed);
                else frequencies.put(removed, remaining);
            }
            best = Math.max(best, right - left + 1);
        }
        return best;
    }

    public static long countSubarraysWithSum(int[] values, long target) {
        Map<Long, Integer> prefixFrequency = new HashMap<>();
        prefixFrequency.put(0L, 1);
        long prefix = 0;
        long count = 0;

        for (int value : values) {
            prefix += value;
            count += prefixFrequency.getOrDefault(prefix - target, 0);
            prefixFrequency.merge(prefix, 1, Integer::sum);
        }
        return count;
    }

    public static void main(String[] args) {
        int[] pair = twoSumSorted(new int[] {1, 2, 4, 7, 11}, 9);
        System.out.println(pair[0] + "," + pair[1]);       // 1,3
        System.out.println(longestAtMostKDistinct("eceba", 2)); // 3
        System.out.println(countSubarraysWithSum(
                new int[] {1, 2, 1, 2}, 3));               // 3
    }
}
```

The string method intentionally defines length in UTF-16 code units. For a code-point contract, operate on an `int[]` of code points and use `Map<Integer, Integer>`.

## Execution or memory walkthrough

For `longestAtMostKDistinct("eceba", 2)`:

| `right` | Added | Window after shrink | Frequencies | Best |
|---:|---|---|---|---:|
| 0 | e | `e` | e:1 | 1 |
| 1 | c | `ec` | e:1,c:1 | 2 |
| 2 | e | `ece` | e:2,c:1 | 3 |
| 3 | b | `eb` | e:1,b:1 | 3 |
| 4 | a | `ba` | b:1,a:1 | 3 |

At right 3, adding b creates three distinct characters. Shrinking removes e at index 0 but e remains, then removes c at index 1, restoring two distinct characters with left at 2. Each code unit enters once and leaves at most once.

For prefix counting on `[1, 2, 1, 2]` with target 3, prefix values are 1, 3, 4, 6. Required earlier prefixes are -2, 0, 1, 3. They occur zero, one, one, and one times, so the count becomes three: ranges `[0,2)`, `[1,3)`, and `[2,4)`. Seeding prefix zero is what counts the first range.

For sorted two-sum target 9, `(1,11)` is too large so right moves; `(1,7)` is too small so left moves; `(2,7)` matches. Every move eliminates all pairs involving the discarded boundary under sorted order.

## Complexity and performance

| Technique | Time | Auxiliary space | Preconditions or caveats |
|---|---:|---:|---|
| Linear scan | O(n) | O(1) | Constant summary state |
| Hash membership/frequency | Expected O(n) | O(n) | Equality and hash quality |
| Sorted opposing pointers | O(n) | O(1) | Input already sorted |
| Sort plus two pointers | O(n log n) | Sort-dependent | Mutation and original indices |
| Fixed/variable window | O(n) | O(k) or alphabet size | Incremental state; validity monotonicity |
| Prefix array plus q queries | O(n + q) | O(n) | Static aggregate |
| Prefix-frequency counting | Expected O(n) | O(n) | Use frequency, not one index |
| Binary search | O(log n) | O(1) | Sorted or monotone domain |

The worked two-pointer method is O(n) time and O(1) auxiliary space. The window is expected O(n) time because each pointer moves at most n and map operations are expected O(1); space is O(min(n, alphabet diversity)). Prefix counting is expected O(n) time and O(n) space.

Primitive arrays are compact and locality-friendly. Maps add nodes or table storage, wrapper objects, hashing, and indirection. If the key domain is small and dense, an `int[]` frequency table is usually simpler and faster than `HashMap<Character, Integer>`.

> **HotSpot note:** HotSpot can eliminate some array bounds checks and optimize tight primitive loops. It cannot make an asymptotically quadratic rescan linear, and it does not turn boxed hash-map state into a primitive array contract.

## Edge cases and common mistakes

- Empty input, one element, no solution, and all-equal data.
- Confusing a contiguous subarray with a subsequence.
- Overflowing an `int` sum before assigning it to `long`; cast an operand first.
- Sorting when input mutation or original indices are forbidden.
- Using `left <= right` when a pair must use two distinct elements.
- Moving a pointer on unsorted data without an elimination proof.
- Forgetting to decrement or remove a leaving window frequency.
- Updating the best window before restoring validity.
- Applying a sum window to negative values without monotonicity.
- Forgetting `prefixFrequency.put(0L, 1)`.
- Storing one prefix index when the question asks for a count.
- Using latest prefix index when longest range requires earliest.
- Mixing inclusive range endpoints with half-open prefix formulas.
- Treating `String.length()` as code-point or grapheme count.
- Claiming deterministic O(1) map operations.
- Binary-searching a predicate that is not monotone or failing to shrink the interval.

## Production engineering notes

Define the unit of text and the maximum input size at the API boundary. For ASCII protocol tokens, a fixed frequency array is appropriate. For human names, code points, normalization, locale, and grapheme behavior may matter. Do not silently apply interview-style `char` assumptions to security identifiers.

For streaming data, an unbounded prefix map or window can become a memory leak. Add time or count retention, eviction, and late-event policy. A sliding time window over out-of-order events is not the same as a pointer window over a sorted array.

Avoid hidden mutation. If sorting is chosen, copy when ownership requires it and account for O(n) space. For large counts, use `long` and define what happens if even `long` overflows. Hash keys influenced by untrusted input need robust platform defaults and resource limits.

In production code, favor standard collection operations and readable loops. In interviews, explain when a dense array can replace a map, when preprocessing is amortized over many queries, and when the output size dominates any possible algorithm.

## Interview questions and model answers

**When is a variable sliding window valid?**

When state can be updated as boundaries move and validity changes monotonically enough that shrinking safely restores it. For nonnegative sums, increasing right cannot lower the sum. Negative values break that property, so exact-sum problems often need prefix hashing instead.

**Why can a loop containing a while loop still be O(n)?**

Analyze pointer movement over the whole run. If right advances n times and left only advances, at most n times, the combined work is O(2n), which is O(n).

**Hashing or sorting for two-sum?**

Hashing offers expected O(n) time and O(n) space on unsorted data and preserves original indices. Sorting plus two pointers is O(n log n), may use less auxiliary space, and changes index handling. If data is already sorted, pointers give O(n) time and O(1) space.

**Why use a prefix-frequency map for subarray counts?**

For current prefix p, every earlier prefix p minus target defines a target-sum subarray. Multiple equal earlier prefixes produce different starts, so a frequency is required. Seed zero for ranges starting at index zero.

**What does a window invariant contain?**

It states the exact interval represented by the auxiliary state, the validity condition after shrinking, and why the retained boundary is optimal or sufficient for the current right endpoint.

**How do duplicates affect binary search?**

An arbitrary equality search can return any duplicate. If the requirement is first or last occurrence, search for a boundary using lower or upper bound and keep the half-open interval invariant explicit.

## Exercises

1. Implement stable removal of a chosen value in place and state the read/write invariant.
2. Find the longest substring without repeated Unicode code points, not `char` values.
3. Count subarrays whose sum is divisible by k using normalized prefix remainders; handle negative values.
4. Implement minimum-length subarray with sum at least target for positive values, then explain why negatives break it.
5. Apply a difference array to range increments and use `long` for reconstructed values.
6. Return original indices for two-sum using both hashing and sort-plus-pairs approaches.
7. Implement binary search on the minimum feasible capacity for partitioning workloads into at most d days.
8. Create property tests comparing optimized subarray counting against an O(n squared) baseline on small random arrays.

## Chapter summary

Sequence algorithms avoid rescanning by compressing processed state. Hash maps answer questions about earlier values, two pointers exploit ordering to eliminate candidates, windows maintain an incrementally updated interval, and prefix sums convert ranges into differences. Binary search applies when a boundary predicate is monotone. Every technique depends on a specific invariant and precondition; sortedness, nonnegative values, duplicate policy, overflow, and text unit must be explicit.

## Revision checklist

- [ ] I distinguish subarrays, substrings, and subsequences.
- [ ] I choose map contents based on membership, index, or frequency needs.
- [ ] I can prove every two-pointer movement eliminates only impossible candidates.
- [ ] I state the exact window interval and validity invariant.
- [ ] I know why negative values can invalidate sum windows.
- [ ] I use the `prefix[0] = 0` convention and seed prefix frequency correctly.
- [ ] I use `long` before arithmetic can overflow.
- [ ] I distinguish UTF-16 units, code points, and graphemes.
- [ ] I apply binary search only to sorted data or a monotone predicate.
- [ ] I can compare hashing, sorting, and dense-array state in Java.
