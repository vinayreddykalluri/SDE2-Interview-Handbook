# Essential Hashing Pattern Clinics

The core chapter develops membership, frequency, grouping, and prefix-sum state. Two additional transformations appear often enough in interviews that they should be learned before the practice lab: prefix XOR and exactly-K counting.

## Clinic 1: prefix XOR plus a frequency map

### Recognition

Use this pattern when the question asks for the number of subarrays whose XOR equals a target. A sliding window is not valid because XOR does not become predictably larger or smaller when an endpoint moves.

Let `prefix` be the XOR of all values seen through the current index. If an earlier prefix was `previous`, then the XOR of the subarray between them is:

```text
previous XOR prefix
```

We want that result to equal `target`. XOR is its own inverse, so:

```text
previous = prefix XOR target
```

The map therefore stores how many times each earlier prefix XOR has occurred. Seed `0 -> 1` so a valid subarray beginning at index zero is counted.

### Dry run

For `[4, 2, 2, 6, 4]` and target `6`, the running prefixes are `4, 6, 4, 2, 6`. At each step, look up `prefix XOR 6` before recording the new prefix. Four compatible earlier states are found, so the answer is four.

### Common mistakes

- Using a sum formula such as `prefix - target`; XOR requires `prefix XOR target`.
- Forgetting the empty prefix.
- Storing only existence when the contract asks for a count.
- Returning `int` when the number of subarrays can approach `n(n+1)/2`.

## Clinic 2: exactly K distinct values

Counting subarrays with exactly K distinct values looks difficult because removing one left value can change the distinct count discontinuously. Transform it into two monotone window counts:

```text
exactly(K) = atMost(K) - atMost(K - 1)
```

For an at-most-K window, expand the right endpoint, shrink while the distinct count is too large, then add `right - left + 1`. That quantity counts every valid subarray ending at `right`.

For `[1, 2, 1, 2, 3]`, the at-most-two count is 12 and the at-most-one count is 5. Their difference is 7.

### Invariant

After shrinking, `[left, right]` contains at most K distinct values, and every suffix ending at `right` whose start lies between `left` and `right` is also valid.

### When not to use it

This transformation works because "at most K distinct" is monotone under removing values from the left. Do not mechanically apply the formula to a property whose at-most form cannot be maintained by a window.

## Runnable Java 21 clinic

```java
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

public final class HashingCoverageClinic {
    private HashingCoverageClinic() {
    }

    public static long countSubarraysWithXor(int[] values, int target) {
        Objects.requireNonNull(values, "values");
        Map<Integer, Long> prefixFrequency = new HashMap<>();
        prefixFrequency.put(0, 1L);

        int prefix = 0;
        long count = 0;
        for (int value : values) {
            prefix ^= value;
            count += prefixFrequency.getOrDefault(prefix ^ target, 0L);
            prefixFrequency.merge(prefix, 1L, Long::sum);
        }
        return count;
    }

    public static long subarraysWithExactlyKDistinct(int[] values, int k) {
        Objects.requireNonNull(values, "values");
        if (k <= 0) {
            return 0;
        }
        return subarraysWithAtMostKDistinct(values, k)
                - subarraysWithAtMostKDistinct(values, k - 1);
    }

    private static long subarraysWithAtMostKDistinct(int[] values, int k) {
        if (k < 0) {
            return 0;
        }
        Map<Integer, Integer> frequency = new HashMap<>();
        int left = 0;
        long count = 0;

        for (int right = 0; right < values.length; right++) {
            frequency.merge(values[right], 1, Integer::sum);
            while (frequency.size() > k) {
                int outgoing = values[left++];
                int remaining = frequency.get(outgoing) - 1;
                if (remaining == 0) {
                    frequency.remove(outgoing);
                } else {
                    frequency.put(outgoing, remaining);
                }
            }
            count += right - left + 1L;
        }
        return count;
    }

    public static void main(String[] args) {
        assert countSubarraysWithXor(new int[] {4, 2, 2, 6, 4}, 6) == 4;
        assert subarraysWithExactlyKDistinct(new int[] {1, 2, 1, 2, 3}, 2) == 7;
        assert subarraysWithExactlyKDistinct(new int[0], 1) == 0;
        System.out.println("PASS essential hashing clinics");
    }
}
```

Expected output with assertions enabled:

```text
PASS essential hashing clinics
```

## Interviewer follow-up chain with model answers

**Interviewer:** Why does prefix XOR use a frequency rather than an earliest index?

**Candidate:** The requested output is a count, so every compatible earlier prefix contributes. An earliest index would be correct for a longest-length contract, not a count contract.

**Interviewer:** Could exactly-K distinct be solved with one window?

**Candidate:** A single ordinary window does not directly count all starts with exactly K distinct values. Two at-most counts are simpler and provably complete. A specialized one-pass method can track additional boundary state, but it is easier to get wrong and does not improve the asymptotic bound.
