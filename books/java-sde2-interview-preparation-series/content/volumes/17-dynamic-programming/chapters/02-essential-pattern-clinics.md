# Essential Dynamic-Programming Pattern Clinics

The core chapter covers linear, grid, knapsack, sequence, edit, and stock state. Word break makes prefix feasibility concrete, while matrix-chain multiplication turns the interval-DP boundary into a complete derivation.

## Clinic 1: word break as prefix feasibility

Define:

```text
reachable[end] = text[0..end) can be segmented into dictionary words
```

Base case `reachable[0] = true` represents the empty prefix. For every reachable start, try word endings up to the maximum dictionary word length. A matching word makes that end reachable.

This is not plain greedy. Choosing the longest or shortest matching word can block a valid later segmentation. DP retains all reachable prefix boundaries without materializing every complete sentence.

If n is the text length and L is the maximum dictionary word length, the state/transition structure is O(nL), excluding the cost of substring creation and hashing. Java substring creates a new string in current mainstream JDKs, so a production implementation may use a trie, region comparison, or indexes to control allocation.

## Clinic 2: matrix-chain interval DP

Matrices are described by dimensions `d[0..n]`; matrix i has shape `d[i] x d[i+1]`. Multiplication order changes scalar work but not the final matrix.

State:

```text
cost[left][right] = minimum scalar multiplications for matrices left through right
```

Base: one matrix needs zero multiplications. For every split between `left` and `right`:

```text
cost[left][split]
+ cost[split + 1][right]
+ d[left] * d[split + 1] * d[right + 1]
```

Evaluate shorter intervals before longer intervals. There are O(n squared) states and O(n) split choices per state, so time is O(n cubed) and table space is O(n squared).

For dimensions `[40, 20, 30, 10, 30]`, the minimum cost is 26,000. Greedily multiplying the currently cheapest adjacent pair is not generally safe because each merge changes future dimensions.

## Runnable Java 21 clinic

```java
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;

public final class DynamicProgrammingCoverageClinic {
    private DynamicProgrammingCoverageClinic() {
    }

    public static boolean canSegment(String text, Set<String> dictionary) {
        Objects.requireNonNull(text, "text");
        Objects.requireNonNull(dictionary, "dictionary");
        Set<String> words = new HashSet<>();
        int maximumLength = 0;
        for (String word : dictionary) {
            if (word == null || word.isEmpty()) {
                throw new IllegalArgumentException("dictionary words must be nonempty");
            }
            words.add(word);
            maximumLength = Math.max(maximumLength, word.length());
        }

        boolean[] reachable = new boolean[text.length() + 1];
        reachable[0] = true;
        for (int start = 0; start < text.length(); start++) {
            if (!reachable[start]) {
                continue;
            }
            int lastEnd = Math.min(text.length(), start + maximumLength);
            for (int end = start + 1; end <= lastEnd; end++) {
                if (words.contains(text.substring(start, end))) {
                    reachable[end] = true;
                }
            }
        }
        return reachable[text.length()];
    }

    public static long minimumMatrixChainCost(int[] dimensions) {
        Objects.requireNonNull(dimensions, "dimensions");
        if (dimensions.length < 2) {
            throw new IllegalArgumentException("at least one matrix is required");
        }
        for (int dimension : dimensions) {
            if (dimension <= 0) {
                throw new IllegalArgumentException("dimensions must be positive");
            }
        }

        int matrices = dimensions.length - 1;
        long[][] cost = new long[matrices][matrices];
        for (int length = 2; length <= matrices; length++) {
            for (int left = 0; left + length <= matrices; left++) {
                int right = left + length - 1;
                cost[left][right] = Long.MAX_VALUE;
                for (int split = left; split < right; split++) {
                    long multiply = Math.multiplyExact(
                            Math.multiplyExact((long) dimensions[left],
                                    dimensions[split + 1]),
                            dimensions[right + 1]);
                    long candidate = Math.addExact(
                            Math.addExact(cost[left][split], cost[split + 1][right]),
                            multiply);
                    cost[left][right] = Math.min(cost[left][right], candidate);
                }
            }
        }
        return cost[0][matrices - 1];
    }

    public static void main(String[] args) {
        assert canSegment("leetcode", Set.of("leet", "code"));
        assert !canSegment("catsandog", Set.of("cats", "dog", "sand", "and", "cat"));
        assert minimumMatrixChainCost(new int[] {40, 20, 30, 10, 30}) == 26_000;
        System.out.println("PASS essential dynamic-programming clinics");
    }
}
```

Expected output with assertions enabled:

```text
PASS essential dynamic-programming clinics
```

## Interviewer follow-up chain with model answers

**Interviewer:** How would you return one word-break segmentation?

**Candidate:** Store the predecessor start whenever an end first becomes reachable. If the final index is reachable, walk predecessors backward and reverse the words. If a specific optimal segmentation is required, the state must encode that objective.

**Interviewer:** Why is matrix-chain DP evaluated by increasing interval length?

**Candidate:** Every transition reads two strict subintervals. Increasing length is a topological order of that dependency graph.

**Interviewer:** Can matrix-chain storage be compressed to one dimension?

**Candidate:** Not by the simple rolling technique used for linear DP. Many nonadjacent subinterval states remain live for larger intervals. If only the cost is needed, specialized optimizations require additional mathematical structure; they are not a generic compression rule.
