# SDE-2 Hashing, Maps, Sets, and Prefix-State Patterns

## Why hashing is more than a lookup trick

Hashing turns a value or derived state into a key that can be found without scanning everything seen so far. That enables pair lookup, frequency counting, grouping, sequence starts, and prefix-state matching. In Java, however, algorithmic correctness depends on the object equality contract, key immutability, collision behavior, and memory cost.

An SDE-2 answer should never stop at "HashMap is O(1)." It should say expected or amortized time, describe the key and value meanings, prove why the stored state is sufficient, and explain what happens with duplicates, overflow, mutable keys, adversarial cardinality, and production concurrency.

## Learning objectives

After completing this chapter, you should be able to:

- choose membership, frequency, value-to-index, grouping, or prefix-state hashing from problem signals;
- implement unsorted two-sum with a hash map and preserve the distinct-index contract;
- build immutable anagram signatures with correct `equals` and `hashCode`;
- derive longest-consecutive-sequence scanning from sequence starts;
- count target-sum subarrays from equalities between prefix sums;
- canonicalize prefix remainders to count divisible-sum subarrays with negative values;
- design immutable custom keys and explain why mutable keys become unreachable;
- state expected, amortized, and worst-case complexity honestly; and
- discuss capacity, cardinality, memory, concurrency, caching, and untrusted input.

## Recognition and decision map

| Signal in the problem | Map or set state | Typical value stored |
|---|---|---|
| have I seen this value? | membership set | no separate value |
| how often has it appeared? | frequency map | count |
| which earlier index completes a relation? | value-to-index map | earliest or latest index |
| collect items with the same signature | grouping map | list or aggregate |
| does a sequence begin here? | membership set | no separate value |
| how many earlier prefixes have a required value? | prefix-state frequency map | number of occurrences |
| what was the earliest position of this state? | prefix-state index map | earliest index |
| cache result by multiple fields | composite key map | computed result/future |

The stored value must match the question. Counting solutions requires frequencies; finding a longest range often requires the earliest index; returning any pair may need only one index. Using a set when multiplicity matters loses information.

## Hash-map model and complexity language

A hash map computes a hash, chooses a bucket, and then uses equality to distinguish keys in that bucket. Equal keys must have equal hash codes. Unequal keys may share a hash code, so collision handling is part of normal operation, not an exceptional failure.

For a well-distributed hash function and controlled load factor, lookup, insertion, and removal are expected `O(1)`. Resizing occasionally costs `O(n)`, but a sequence of insertions is amortized expected `O(1)` per insertion. Worst-case behavior can be worse because collisions and key comparison still exist. Modern Java implementations contain collision defenses, but portable algorithm analysis should not depend on an undocumented bucket layout or claim a universal hard constant-time guarantee.

A map also has substantial memory overhead: table slots, nodes or internal entries, object headers, references, boxed primitive values, and unused capacity. `HashMap<Integer,Integer>` can use far more memory than two primitive arrays. For a small dense alphabet, an array is often the better data structure. For a sparse or large key space, hashing buys flexibility.

Java `HashMap` allows one null key and null values, while `ConcurrentHashMap` does not. Iteration order is not a stable API guarantee for `HashMap`. Use `LinkedHashMap` when insertion order is an explicit result requirement, or sort results before returning.

## Equality, hash codes, and stable keys

The essential Java contracts are:

- `equals` is reflexive, symmetric, transitive, consistent, and false for null;
- if `a.equals(b)`, then `a.hashCode() == b.hashCode()`; and
- fields used by equality and hashing must not change while the object is a key.

The final rule is easy to violate. Suppose an `ArrayList<Integer>` is inserted as a key. Its equality and hash code depend on its elements. If an element changes, a later lookup computes a different bucket position, so even `map.get(theSameListReference)` may fail to find the entry through normal lookup. The entry has not vanished from memory; its key no longer leads to the bucket where it was stored.

Prefer records composed of immutable fields, immutable value objects, strings, enums, and primitive wrappers. A record containing an array is not deeply immutable: the generated `equals` compares that array by reference, and callers might mutate it. A custom array-backed key must make a defensive copy, use content equality, and never expose the array.

Cache a hash code only when every equality-relevant field is immutable. A cached hash with mutable contents makes the inconsistency even harder to diagnose.

## Pattern 1: unsorted two-sum with a hash map

Given an unsorted array and target, scan from left to right. Before storing the current value, compute the complement and ask whether an earlier index produced it. If so, return the earlier and current indexes. Otherwise store the current value and its index.

The order of lookup and insertion enforces distinct indexes: the current element cannot match itself unless an equal value occurred earlier. The invariant before processing index `i` is that the map contains an index for each stored value in prefix `[0, i)`. If a solution ending at `i` exists, its complement is in that prefix and the lookup finds it.

### Dry run

For `[3, 2, 4, 3]` and target 6:

| i/value | needed | prior map | action |
|---:|---:|---|---|
| 0/3 | 3 | `{}` | store `3 -> 0` |
| 1/2 | 4 | `{3=0}` | store `2 -> 1` |
| 2/4 | 2 | `{3=0,2=1}` | return `(1,2)` |

The pair uses values 2 and 4. If the problem instead asks for every pair, storing one index is insufficient; duplicate policy and output-size complexity become central.

Compute `needed` in `long`. With `int` target and values, mathematical subtraction can leave the `int` range. A complement outside that range cannot appear in an `int[]`, so skip the lookup. The runnable method uses a real `HashMap`; it does not sort or silently replace this required pattern with two pointers.

Expected time is `O(n)`, space is `O(n)`. A sort-and-two-pointer alternative takes `O(n log n)` and may lose original indexes unless pairs are retained.

## Pattern 2: frequency and membership

A frequency map summarizes a multiset. After processing prefix `[0, i)`, invariant `frequency[x]` equals the number of occurrences of `x` in that prefix. This supports duplicate detection, majority candidates with verification, intersection with multiplicity, and top-frequency selection.

Use `merge(key, 1, Integer::sum)` for concise counting when counts fit `int`. Use `long` counts for streams or aggregated datasets that can exceed that range. Removing zero counts keeps maps canonical in sliding-window algorithms and makes `map.size()` equal the number of active distinct keys.

A set answers membership only. For deduplication that must preserve encounter order, use `LinkedHashSet` or maintain a result list plus a `HashSet` of seen values. For sorted results, use sorting or `TreeSet`, accepting `O(log n)` operations.

Production code should cap distinct-key cardinality for untrusted or effectively unbounded input. A linear-time algorithm can still exhaust heap if every token is distinct.

## Pattern 3: grouping anagrams by immutable signature

For lowercase ASCII words, the 26 letter counts form a canonical signature. Words are anagrams exactly when their signature vectors are equal. Map each signature to the list of words having it.

The custom `LowercaseSignature` in the runnable class owns a private count array, computes content-based equality with `Arrays.equals`, and uses `Arrays.hashCode`. Callers never receive the backing array. This repairs two common Java errors: using a raw array as a map key, which compares by identity, and reusing a mutable counting buffer across insertions.

### Dry run

Words `eat`, `tea`, `tan`, `ate`, `nat`, `bat` produce three logical keys:

```text
counts(a=1,e=1,t=1) -> [eat, tea, ate]
counts(a=1,n=1,t=1) -> [tan, nat]
counts(a=1,b=1,t=1) -> [bat]
```

A `LinkedHashMap` preserves first-group encounter order in the reference implementation, making tests deterministic.

Let `C` be the total number of characters and `g` the number of distinct signatures. Building groups takes expected `O(C)` time because the alphabet width is fixed, plus output storage. Signature storage costs `O(26g)`, abbreviated to `O(g)` for a fixed alphabet.

For Unicode text, first define normalization and case rules. Options include sorting code points, which costs `O(m log m)` per word, or building a sparse code-point frequency signature. Do not reuse the 26-letter solution without validating every code point.

## Pattern 4: longest consecutive sequence

Insert every number into a set. A value begins a consecutive sequence only if its predecessor is absent. From each such start, walk upward while successors exist. Values inside a sequence are never used as starts, so each value participates in at most one forward walk.

Invariant during a walk: every integer from `start` through `current` is in the set, and the recorded length is exactly that range size. When the successor is absent, the sequence is maximal on the right; predecessor absence made it maximal on the left.

### Dry run

For `[100,4,200,1,3,2]`, the set contains all six values. `100` starts a length-1 sequence; `200` starts another. `4`, `3`, and `2` do not start because their predecessors exist. `1` has no predecessor, then walks through `2,3,4`, producing best length 4.

Expected time is `O(n)`, not `O(n^2)`: forward walks across all starts visit each distinct value once. Space is `O(n)`.

Handle numeric boundaries explicitly. Testing `value - 1` when `value == Integer.MIN_VALUE` wraps to `Integer.MAX_VALUE`; incrementing `Integer.MAX_VALUE` wraps to the minimum. The toolkit guards both operations so separate boundary values are not joined into a false cyclic sequence.

## Prefix state: turn a subarray into two prefixes

Define half-open prefix sum:

```text
prefix[0] = 0
prefix[i + 1] = values[0] + ... + values[i]
```

Then the sum of subarray `[left, right)` is `prefix[right] - prefix[left]`. Hashing earlier prefix states lets the current right boundary find every left boundary satisfying a relation.

This is more general than a sliding window for arrays containing negative values. Sliding a left pointer based on sum is not monotone when adding or removing a negative value can move the sum in either direction. Prefix equalities remain valid.

The seed state for the empty prefix is essential. Store prefix sum zero with frequency one before scanning; this counts subarrays beginning at index zero.

## Pattern 5: count subarrays with a target sum

At current prefix `P`, a prior prefix `Q` creates target sum `target` when:

```text
P - Q = target
Q = P - target
```

Therefore add the frequency of `P - target`, then increment the frequency of `P`. Query before insertion when zero-length subarrays are not part of the answer.

Invariant before each input value: the map counts every prefix ending before the current right boundary. `answer` counts every target-sum subarray whose right boundary has already been processed.

### Dry run

For `[1,2,1,2]`, target 3:

| value | prefix P | seek P-3 | prior count | answer | updated map entry |
|---:|---:|---:|---:|---:|---|
| 1 | 1 | -2 | 0 | 0 | `1 -> 1` |
| 2 | 3 | 0 | 1 | 1 | `3 -> 1` |
| 1 | 4 | 1 | 1 | 2 | `4 -> 1` |
| 2 | 6 | 3 | 1 | 3 | `6 -> 1` |

The three subarrays are indexes `[0,2)`, `[1,3)`, and `[2,4)`. Use `long` for prefix and answer; the number of subarrays can reach `n * (n + 1) / 2`.

Expected time is `O(n)` and space is `O(n)`. If all inputs are nonnegative and only a longest or shortest range is needed, a window may use less state, but it is a different proof.

## Pattern 6: count subarrays divisible by K

A subarray sum is divisible by positive divisor `k` when two prefixes have the same remainder modulo `k`:

```text
(P - Q) mod k = 0
P mod k = Q mod k
```

Maintain frequencies of prior canonical remainders. For each prefix, compute `Math.floorMod(prefix, divisor)` so negative sums map into `[0, k)`. Add the previous frequency of that remainder, then increment it. Seed remainder zero with frequency one.

### Dry run

For `[4,5,0,-2,-3,1]` and `k = 5`, canonical prefix remainders are `4,4,4,2,4,0`. Before updates their prior frequencies contribute `0,1,2,0,3,1`, totaling 7 divisible-sum subarrays.

Using Java `%` directly can produce negative remainders; prefixes that are congruent mathematically may then appear under different keys. Canonicalization fixes that. Reject divisor zero. The toolkit requires a positive divisor, avoiding the `abs(Long.MIN_VALUE)` trap.

When `k` is small and trusted, a `long[]` frequency table may be faster and more memory-efficient than a map. When `k` is huge or sparse, allocating an array of size `k` is unsafe, so a map is appropriate.

## Earliest index versus frequency

The same prefix state can answer a different question by changing the map value.

- **Count subarrays:** store frequency of each state.
- **Longest subarray:** store the earliest index of each state and never overwrite it.
- **Shortest subarray under a direct equality:** store the latest index, if the proof supports it.
- **Existence:** store a set or return immediately.

For longest zero-sum subarray, when prefix `P` repeats at boundaries `j` and `i`, subarray `[j, i)` sums to zero. The earliest `j` gives the greatest length for a fixed `i`. Using a frequency map here would not provide the boundary needed for the range.

## Runnable Java 21 reference implementation

The class below compiles as written. Run checkpoints with `java -ea HashingPatternToolkit`.

```java
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

public final class HashingPatternToolkit {
    private HashingPatternToolkit() {
    }

    public record Point(int x, int y) {
    }

    public static final class LowercaseSignature {
        private final int[] counts;
        private final int hash;

        private LowercaseSignature(String word) {
            if (word == null) {
                throw new IllegalArgumentException("word must not be null");
            }
            this.counts = new int[26];
            for (int i = 0; i < word.length(); i++) {
                char current = word.charAt(i);
                if (current < 'a' || current > 'z') {
                    throw new IllegalArgumentException("expected lowercase ASCII word");
                }
                counts[current - 'a']++;
            }
            this.hash = Arrays.hashCode(counts);
        }

        @Override
        public boolean equals(Object other) {
            return this == other
                    || other instanceof LowercaseSignature that
                    && Arrays.equals(this.counts, that.counts);
        }

        @Override
        public int hashCode() {
            return hash;
        }
    }

    public static int[] twoSumUnsorted(int[] values, int target) {
        requireArray(values);
        Map<Integer, Integer> indexByValue = new HashMap<>();
        for (int i = 0; i < values.length; i++) {
            long needed = (long) target - values[i];
            if (needed >= Integer.MIN_VALUE && needed <= Integer.MAX_VALUE) {
                Integer earlier = indexByValue.get((int) needed);
                if (earlier != null) {
                    return new int[] {earlier, i};
                }
            }
            indexByValue.putIfAbsent(values[i], i);
        }
        return new int[] {-1, -1};
    }

    public static Map<Integer, Long> frequencies(int[] values) {
        requireArray(values);
        Map<Integer, Long> result = new HashMap<>();
        for (int value : values) {
            result.merge(value, 1L, Long::sum);
        }
        return result;
    }

    public static List<List<String>> groupLowercaseAnagrams(List<String> words) {
        if (words == null) {
            throw new IllegalArgumentException("words must not be null");
        }
        Map<LowercaseSignature, List<String>> groups = new LinkedHashMap<>();
        for (String word : words) {
            groups.computeIfAbsent(new LowercaseSignature(word), ignored -> new ArrayList<>())
                    .add(word);
        }
        List<List<String>> result = new ArrayList<>();
        for (List<String> group : groups.values()) {
            result.add(List.copyOf(group));
        }
        return List.copyOf(result);
    }

    public static int longestConsecutive(int[] values) {
        requireArray(values);
        Set<Integer> members = new HashSet<>();
        for (int value : values) {
            members.add(value);
        }
        int best = 0;
        for (int value : members) {
            boolean hasPredecessor = value != Integer.MIN_VALUE
                    && members.contains(value - 1);
            if (hasPredecessor) {
                continue;
            }
            int length = 1;
            int current = value;
            while (current != Integer.MAX_VALUE && members.contains(current + 1)) {
                current++;
                length++;
            }
            best = Math.max(best, length);
        }
        return best;
    }

    public static long countSubarraysWithSum(int[] values, long target) {
        requireArray(values);
        Map<Long, Long> prefixFrequency = new HashMap<>();
        prefixFrequency.put(0L, 1L);
        long prefix = 0;
        long answer = 0;
        for (int value : values) {
            prefix += value;
            answer += prefixFrequency.getOrDefault(prefix - target, 0L);
            prefixFrequency.merge(prefix, 1L, Long::sum);
        }
        return answer;
    }

    public static long countSubarraysDivisibleBy(int[] values, long divisor) {
        requireArray(values);
        if (divisor <= 0) {
            throw new IllegalArgumentException("divisor must be positive");
        }
        Map<Long, Long> remainderFrequency = new HashMap<>();
        remainderFrequency.put(0L, 1L);
        long prefix = 0;
        long answer = 0;
        for (int value : values) {
            prefix += value;
            long remainder = Math.floorMod(prefix, divisor);
            answer += remainderFrequency.getOrDefault(remainder, 0L);
            remainderFrequency.merge(remainder, 1L, Long::sum);
        }
        return answer;
    }

    public static Map<Point, Long> countPointVisits(List<Point> points) {
        if (points == null) {
            throw new IllegalArgumentException("points must not be null");
        }
        Map<Point, Long> visits = new HashMap<>();
        for (Point point : points) {
            if (point == null) {
                throw new IllegalArgumentException("points must not contain null");
            }
            visits.merge(point, 1L, Long::sum);
        }
        return Map.copyOf(visits);
    }

    private static void requireArray(int[] values) {
        if (values == null) {
            throw new IllegalArgumentException("values must not be null");
        }
    }

    public static void main(String[] args) {
        assert Arrays.equals(twoSumUnsorted(new int[] {3, 2, 4, 3}, 6),
                new int[] {1, 2});
        assert Arrays.equals(twoSumUnsorted(new int[] {3, 3}, 6),
                new int[] {0, 1});
        assert frequencies(new int[] {4, 4, 2}).equals(Map.of(4, 2L, 2, 1L));

        List<List<String>> groups = groupLowercaseAnagrams(
                List.of("eat", "tea", "tan", "ate", "nat", "bat"));
        assert groups.equals(List.of(
                List.of("eat", "tea", "ate"),
                List.of("tan", "nat"),
                List.of("bat")));

        assert longestConsecutive(new int[] {100, 4, 200, 1, 3, 2}) == 4;
        assert longestConsecutive(new int[] {Integer.MIN_VALUE, Integer.MAX_VALUE}) == 1;
        assert countSubarraysWithSum(new int[] {1, 2, 1, 2}, 3) == 3;
        assert countSubarraysWithSum(new int[] {0, 0, 0}, 0) == 6;
        assert countSubarraysDivisibleBy(new int[] {4, 5, 0, -2, -3, 1}, 5) == 7;

        Map<Point, Long> visits = countPointVisits(
                List.of(new Point(1, 2), new Point(1, 2), new Point(2, 1)));
        assert visits.get(new Point(1, 2)) == 2L;
        assert visits.get(new Point(2, 1)) == 1L;
    }
}
```

## Complexity and state table

| Pattern | Time | Space | Map meaning |
|---|---:|---:|---|
| unsorted two-sum | expected `O(n)` | `O(n)` | value to earlier index |
| frequency counting | expected `O(n)` | `O(distinct)` | value to count |
| lowercase anagram grouping | expected `O(C)` | `O(C + groups)` including output | signature to words |
| longest consecutive | expected `O(n)` | `O(n)` | membership only |
| target-sum subarray count | expected `O(n)` | `O(n)` | prefix sum to frequency |
| divisible-sum subarray count | expected `O(n)` | `O(min(n,k))` states | remainder to frequency |

These are expected hash-table bounds. Returned groups and maps are output space; do not hide them inside or outside auxiliary-space notation inconsistently.

## Edge cases and common mistakes

1. **Sorting in a required hash solution.** It changes the demonstrated pattern and may lose original indexes.
2. **Inserting before complement lookup.** The same element can match itself when target is twice its value.
3. **Subtraction overflow.** Compute complements and prefix states in `long`.
4. **Wrong map value.** A count problem needs frequencies; a longest-range problem often needs earliest indexes.
5. **Missing empty-prefix seed.** Subarrays starting at zero disappear.
6. **Query after update.** For nonempty subarrays, count prior prefixes before inserting the current one.
7. **Negative remainder split.** Use `floorMod` or another canonical remainder formula.
8. **Boundary wraparound in sequences.** `MIN_VALUE - 1` and `MAX_VALUE + 1` wrap.
9. **Array as key.** Java arrays use identity equality and identity-based hash codes unless wrapped in a content-based key.
10. **Mutable key.** Changing equality-relevant fields after insertion breaks lookup.
11. **Hash code without equality.** Overriding only one side of the contract yields incorrect behavior.
12. **Assuming iteration order.** `HashMap` order is not a result contract.
13. **Unbounded cardinality.** Expected linear time does not prevent memory exhaustion.
14. **Concurrent unsynchronized mutation.** `HashMap` is not a thread-safe shared mutable cache.
15. **Hash as proof of equality.** A hash collision must be resolved with `equals` or exact verification.

## SDE-2 production follow-ups

- **Capacity planning:** if a trustworthy size estimate exists, pre-size a map to reduce resizing, but do not allocate from an unbounded attacker-provided count. Measure because capacity formulas and implementation details can change.
- **Primitive specialization:** high-volume integer maps may justify primitive collections to avoid boxing. Introduce a dependency only after profiling and with operational familiarity.
- **Concurrency:** use method-local maps for algorithms. Shared caches need `ConcurrentHashMap` or an external cache plus atomic loading, eviction, expiry, and failure policy.
- **Cache semantics:** a hash map is not a complete cache. Bound entries, account for value weight, avoid caching failures forever, and prevent stampedes for expensive keys.
- **Skew and hot keys:** one group or frequency key may dominate memory or contention. Track maximum group size and distinct cardinality, not only total input.
- **Untrusted keys:** cap lengths and counts, use stable library key types, and prefer deterministic algorithms where collision-sensitive work can be abused.
- **Persistence:** Java hash codes are not durable cross-language identifiers. Persist canonical fields, not in-memory bucket hashes.
- **Data privacy:** map keys can contain customer identifiers or text. Avoid raw-key logging and sanitize metrics to prevent high-cardinality telemetry.
- **Result determinism:** sort keys or use a defined ordered map when API consumers, snapshots, or tests require stable ordering.
- **Null policy:** reject nulls at the boundary or specify them. Mixing "absent" with "present and mapped to null" complicates `get`-based logic.
- **Equality evolution:** adding an equality field to a key changes grouping and cache identity. Treat key schema as an API and migration concern.

## Exercises with model checkpoints

### Exercise 1: all unique two-sum value pairs

Return every distinct value pair summing to target from an unsorted array.

**Model checkpoints:** distinguish value pairs from index pairs; use a seen set and a canonical pair key to avoid duplicates; widen complement arithmetic; output can be `O(n)`; define ordering for deterministic results.

### Exercise 2: longest zero-sum subarray

Return the boundaries of the longest contiguous range summing to zero.

**Model checkpoints:** map each prefix sum to its earliest boundary index; seed `0 -> 0` when prefixes are indexed by element count; on repetition at boundary `right`, candidate is `[earliest, right)`; never overwrite earliest; use `long` prefix.

### Exercise 3: exactly K distinct subarrays

Count subarrays with exactly `k` distinct values.

**Model checkpoints:** derive `exactly(k) = atMost(k) - atMost(k - 1)`; each at-most helper uses a frequency map and monotone window; define `k <= 0`; use `long` result; remove keys at zero.

### Exercise 4: top K frequent values

Return the `k` most frequent integers.

**Model checkpoints:** frequency map plus size-`k` min-heap gives expected `O(n + d log k)` for `d` distinct values; define tie order; validate `k`; bucket frequency can be linear but allocates by input length; output order is a contract.

### Exercise 5: subarrays with equal zeroes and ones

Count ranges containing equal numbers of zero and one.

**Model checkpoints:** transform zero to -1 and one to +1; equal counts become zero-sum; reuse prefix-frequency proof; reject other input values or define them; seed empty prefix.

### Exercise 6: Unicode anagram key

Design an immutable signature for normalized Unicode strings.

**Model checkpoints:** choose normalization and case policy; sorted code points give an immutable sequence but cost `O(m log m)`; sparse counts need a stable canonical representation; copy mutable inputs; test canonically equivalent spellings and supplementary code points.

### Exercise 7: bounded frequency service

Count events by key in a long-running service with a strict memory budget.

**Model checkpoints:** an exact unbounded map cannot satisfy the budget for unbounded keys; define time windows, eviction, aggregation, or approximate sketches; identify heavy hitters; specify concurrency and persistence; expose dropped/evicted-state metrics.

### Exercise 8: custom composite key review

Review a key class containing `String tenant`, `byte[] digest`, and mutable `List<String> tags`.

**Model checkpoints:** copy the byte array and tags into immutable representations; define order sensitivity; implement equality and hash from the same fields; validate nulls; avoid exposing internals; consider whether every field truly belongs to identity.

## Interview answer checklist

- [ ] I named exactly what the map key and value represent.
- [ ] I used a set only when multiplicity and indexes are unnecessary.
- [ ] I stated expected/amortized complexity rather than promising universal `O(1)`.
- [ ] I proved distinct-index behavior in two-sum.
- [ ] I seeded the empty prefix when ranges may begin at zero.
- [ ] I chose frequency versus earliest index based on the requested output.
- [ ] I canonicalized negative remainders.
- [ ] I guarded integer boundary wraparound.
- [ ] My custom keys are immutable and implement consistent equality and hashing.
- [ ] I defined result ordering, null policy, and duplicate behavior.
- [ ] I can discuss cardinality, memory, concurrency, and hostile input.

## Summary

Hashing is a way to retain exactly the past state a future element needs. Unsorted two-sum maps values to earlier indexes. Frequency maps preserve multiplicity; grouping maps canonical signatures to outputs; a set lets longest-consecutive scanning begin only at maximal sequence starts. Prefix-state hashing turns a subarray equation into a lookup for an earlier sum or remainder. All of these rely on stable equality, collision resolution, correct numeric width, and the right map value. The SDE-2 standard includes not only the expected linear algorithm, but also immutable keys, honest complexity, bounded cardinality, deterministic API behavior, and production ownership and concurrency decisions.
