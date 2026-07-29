# Realistic Hashing Interview Rounds with Model Answers

These are original interview-room simulations. Each round shows what a strong candidate says and does: clarify the contract, expose a baseline, derive the state, code, test, and answer follow-ups.

## Round 1: return the two indices whose values sum to a target

### Interviewer prompt

Given an integer array and a target, return the indexes of two distinct elements whose sum equals the target. Assume exactly one answer exists.

### Candidate clarification

> May I return indexes in any order? Can values be negative? Can the same value appear more than once? Is the array allowed to be modified?

The interviewer answers: any order, negatives and duplicates are allowed, and preserve the input.

### Baseline answer

Try every pair. That is O(n squared) time and O(1) auxiliary space. It is correct but does not reuse information from earlier comparisons.

### Optimized reasoning

While scanning index `i`, the needed partner is `target - values[i]`. Store indexes for values already processed. Check before inserting so the same element is never paired with itself.

```java
static int[] twoSum(int[] values, int target) {
    Map<Integer, Integer> indexByValue = new HashMap<>();
    for (int i = 0; i < values.length; i++) {
        int needed = target - values[i];
        Integer partner = indexByValue.get(needed);
        if (partner != null) {
            return new int[] {partner, i};
        }
        indexByValue.put(values[i], i);
    }
    throw new IllegalArgumentException("no pair");
}
```

Invariant: before index `i`, the map contains an index for every value in `[0, i)`. If `needed` is present, the returned indexes are distinct and their values sum to the target.

For `[3, 3]`, target `6`, index 0 is inserted first. Index 1 finds the stored 3 and returns `[0, 1]`.

### Follow-up questions and answers

**What if there can be no answer?** Return an empty optional-like result or a documented sentinel instead of throwing; the API contract decides. In a coding platform, `new int[0]` may be expected.

**What if arithmetic can overflow?** Compute the complement using `long` and use `Map<Long, Integer>` if the mathematical target is wider than `int`.

**What if I need every unique value pair?** Sort and use two pointers, or use a set of canonical value pairs. Index-pair output can itself be quadratic when many duplicates exist.

**Complexity?** Expected O(n) time, O(n) auxiliary space. The claim depends on ordinary hash behavior.

## Round 2: count subarrays whose sum equals K

### Interviewer prompt

Return the number of contiguous subarrays whose sum equals `target`. Values may be negative.

### Candidate clarification

> Should the count fit in `int`? Are empty subarrays included? May values be negative?

Assume non-empty subarrays, negatives are allowed, and return `long` because the answer can approach `n(n+1)/2`.

### Why the obvious window is wrong

A variable sliding window relies on monotone movement. Negative values can make a larger window have a smaller sum, so shrinking on `sum > target` is not sound.

### Derivation

At current prefix `p`, each earlier prefix `p - target` creates one valid subarray ending here. Store how many times every prefix has appeared.

```java
static long countSubarrays(int[] values, long target) {
    Map<Long, Long> frequency = new HashMap<>();
    frequency.put(0L, 1L);
    long prefix = 0L;
    long answer = 0L;
    for (int value : values) {
        prefix += value;
        answer += frequency.getOrDefault(prefix - target, 0L);
        frequency.merge(prefix, 1L, Long::sum);
    }
    return answer;
}
```

Dry run for `[1, -1, 1]`, target `1`:

| Value | Prefix | Need | Earlier matches | Running answer |
|---:|---:|---:|---:|---:|
| seed | 0 | - | one empty prefix | 0 |
| 1 | 1 | 0 | 1 | 1 |
| -1 | 0 | -1 | 0 | 1 |
| 1 | 1 | 0 | 2 | 3 |

The valid subarrays are indexes `[0,0]`, `[0,2]`, and `[2,2]`.

### Follow-up questions and answers

**Why frequency rather than earliest index?** The question asks how many subarrays exist. Every earlier matching prefix contributes a different start boundary.

**How would longest length differ?** Store the earliest index of each prefix and never overwrite it. For a current index, subtract that earliest matching index.

**Can this be streaming?** Yes. Process each value once while retaining the prefix-frequency map. Memory still grows with the number of distinct prefixes.

**How would you handle unbounded input?** Exact arbitrary-range counts require retaining all relevant prefix states. A bounded-memory version needs constraints, approximation, or a bounded time window with carefully expired state.

## Round 3: longest consecutive sequence without sorting

### Interviewer prompt

Given unsorted integers, return the length of the longest run of consecutive values in expected linear time.

### Candidate reasoning

Insert all distinct values into a set. Start a walk only at a value with no predecessor. That rule prevents revisiting the interior of a run from every element.

```java
static int longestConsecutive(int[] values) {
    Set<Integer> present = new HashSet<>();
    for (int value : values) {
        present.add(value);
    }

    int best = 0;
    for (int value : present) {
        if (value != Integer.MIN_VALUE && present.contains(value - 1)) {
            continue;
        }
        int length = 1;
        int current = value;
        while (current != Integer.MAX_VALUE && present.contains(current + 1)) {
            current++;
            length++;
        }
        best = Math.max(best, length);
    }
    return best;
}
```

### Correctness explanation

Every consecutive run has exactly one smallest value. Only that value lacks a predecessor and begins a walk. The walk counts every value in the run, so the maximum counted length is the answer.

### Follow-up questions and answers

**Why is the nested while loop still expected O(n)?** Across all starts, each distinct value belongs to one run and is advanced through at most once. This is aggregate analysis, not O(n) work per outer iteration.

**Why guard integer boundaries?** `Integer.MIN_VALUE - 1` and `Integer.MAX_VALUE + 1` wrap in Java and could create false adjacency.

**Would sorting be acceptable?** Sorting gives O(n log n) time and can use less additional object-heavy memory depending on whether mutation is allowed. State that trade-off instead of calling hashing universally superior.

## A realistic debugging exchange

The interviewer shows this code:

```java
Map<List<Integer>, String> cache = new HashMap<>();
List<Integer> key = new ArrayList<>(List.of(1, 2));
cache.put(key, "stored");
key.add(3);
System.out.println(cache.get(key));
```

**Strong answer:** The list's equality and hash code depend on its elements. Mutating it after insertion changes the lookup hash. The entry is still stored according to the old hash position, so lookup behavior is no longer reliable. Use an immutable key such as `List.copyOf(key)` or a record composed of immutable fields.

## Closing answer pattern

For a hashing problem, finish aloud with:

1. the exact key and stored value;
2. what the state represents before each iteration;
3. whether duplicates require counts, earliest indexes, or membership;
4. overflow and mutable-key boundaries;
5. expected time and worst-case/space qualifications;
6. tests for empty input, duplicates, negatives, and numeric extremes.
