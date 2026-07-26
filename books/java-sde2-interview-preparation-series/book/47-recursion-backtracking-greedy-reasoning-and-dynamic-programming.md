# 47. Recursion, Backtracking, Greedy Reasoning, and Dynamic Programming

## Learning objectives

By the end of this chapter, you should be able to:

- define recursive contracts with base cases, progress, and combination logic;
- implement backtracking with explicit choose, explore, and unchoose phases;
- prune duplicates and infeasible branches without removing valid solutions;
- justify a greedy choice with an exchange, staying-ahead, or cut argument;
- specify dynamic programming by state, transition, base, order, and answer; and
- choose among brute force, memoization, tabulation, greedy, and space compression.

## Why this matters at SDE-2

These techniques address large search spaces. Recursion mirrors decomposable structure. Backtracking explores alternatives while reusing one mutable path. Greedy algorithms commit early when a proof says the choice cannot hurt. Dynamic programming stores repeated subproblem results when no safe local commitment exists.

The senior-level signal is not recognizing "this is DP." It is defining enough state to make the future independent of the past, proving a transition covers all choices, and explaining why a proposed greedy shortcut is valid or offering a counterexample. Production relevance includes bounded search, scheduling, resource allocation, parsing, and configuration exploration.

## First-principles model

Imagine a directed acyclic state graph. A state summarizes everything needed to solve the remaining problem. Transitions represent legal choices. Plain recursion may visit the same state repeatedly. Backtracking traverses a decision tree while maintaining a current partial solution. Memoization caches state results. Tabulation evaluates states in dependency order. A greedy algorithm follows one outgoing transition and discards the rest only when a proof makes them unnecessary.

The central question is information sufficiency: if two histories map to the same state, do they truly have identical legal futures and objective consequences? If not, the state is under-specified and caching it is incorrect.

> **Specification boundary:** Java uses ordinary method invocation for recursion and does not guarantee tail-call elimination. Maximum safe recursion depth is not specified and depends on frame shape, JVM, options, and thread stack. Algorithmic proofs must not rely on recursive calls being converted to loops.

## Core terminology

- **Recursive contract:** meaning of one call for its parameters.
- **Base case:** smallest state solved directly.
- **Progress:** argument moves toward a base case.
- **Decision tree:** nodes are partial choices; edges are candidate decisions.
- **Backtracking:** depth-first exploration that restores mutable path state.
- **Pruning:** skip a branch proven infeasible, duplicate, or unable to improve the answer.
- **Greedy choice property:** an optimal solution exists that begins with the selected local choice.
- **Optimal substructure:** optimal solution can be composed from optimal subproblem solutions.
- **Overlapping subproblems:** same state is reached by multiple histories.
- **Memoization/tabulation:** top-down cached recursion and bottom-up ordered evaluation.
- **State compression:** discard table dimensions or history not needed by future transitions.

## Detailed mechanics

### Recursion from a contract

Write a recursive method in four steps:

1. Define exactly what `solve(state)` returns.
2. Handle all minimal or invalid states.
3. Generate strictly smaller or closer states.
4. Combine recursive results into the promised result.

For binary-tree height: `height(node)` returns the number of nodes on the longest downward path in that subtree. Null returns zero. Non-null returns one plus the maximum child height. The structure guarantees progress because calls move to proper subtrees.

For index recursion, prove the index increases or remaining amount decreases. Beware zero-value choices: a recursive coin call with unchanged amount never reaches the base case.

Recursive time is number of distinct calls only when memoized. Without caching, count all nodes in the recursion tree. Fibonacci-style branching can be exponential even though there are only O(n) unique argument values.

Recursive space is maximum active depth times frame state, not total calls. A branching tree may execute exponentially many calls but hold only O(depth) simultaneously.

### Backtracking discipline

Backtracking is DFS over choices:

```text
search(state, path):
    if state is complete:
        record a copy of path
        return
    for each legal candidate:
        choose candidate
        search(next state, path)
        undo candidate
```

The path invariant is that it represents exactly the decisions from the root to the current state. The unchoose step restores the caller's path before trying its next candidate. If results store the mutable path reference instead of a copy, all output entries can later appear identical.

Candidate-generation strategy prevents duplicate output:

- subsets/combinations use a start index so order does not create permutations;
- permutations use a `used[]` set or in-place swaps;
- sorted candidates allow same-depth duplicate skipping;
- repeated reuse keeps the same candidate index, while single use advances it.

For sorted duplicate permutation input, the rule `i > 0 && values[i] == values[i-1] && !used[i-1]` skips choosing the later equal value before its identical predecessor at the same decision level. It removes symmetric branches, not distinct value sequences.

Prune only with proof. A partial sum exceeding target is safe to prune when all remaining values are nonnegative. With negative values, the branch might later recover. A branch-and-bound estimate must be optimistic enough that a pruned branch truly cannot beat the incumbent.

### Greedy reasoning

A greedy algorithm makes an irrevocable local choice. Fast code is not evidence of correctness. Common proof styles are:

- **Exchange:** transform an optimal solution to use the greedy first choice without worsening it.
- **Staying ahead:** after every prefix, greedy is at least as good as any competitor under a useful measure.
- **Cut property:** the lightest or safest edge crossing a partition can belong to an optimum.
- **Invariant:** processed choices can be extended to some optimal solution.

Interval scheduling by earliest finish is a classic exchange proof. Let g be the compatible interval finishing first. Take any optimal schedule whose first interval is o. Replacing o with g cannot reduce remaining room because g ends no later. Therefore an optimum exists beginning with g; repeat on intervals starting after g.

Choosing earliest start, shortest duration, or greatest value is not equivalent. Construct a counterexample before trusting a heuristic.

Greedy coin selection also depends on denomination structure. For coins `[1,3,4]` and amount 6, largest-first chooses `4+1+1` (three coins), while `3+3` uses two. The local choice lacks a general exchange proof, so dynamic programming is appropriate.

### Dynamic programming specification

A rigorous DP has five parts:

1. **State:** what does `dp[...]` mean?
2. **Transition:** which final or next choice connects states?
3. **Base:** which states are known directly?
4. **Order:** are dependencies computed before use?
5. **Answer:** which state or aggregate is returned?

For minimum coins, `dp[a]` is the minimum number of coins needed to total exactly a, or impossible. Base `dp[0] = 0`. For every amount a and usable coin c:

```text
dp[a] = min(dp[a], dp[a - c] + 1)
```

Ascending amounts permit unlimited reuse. For 0/1 selection with a one-dimensional table, amounts often iterate downward so the current item is not reused in the same iteration. Loop direction is part of the state dependency proof, not a micro-optimization.

### Memoization versus tabulation

Memoization follows only reachable states and often mirrors a recurrence. It carries recursion depth and cache lookup overhead. Tabulation avoids recursion and controls iteration order, but may fill unreachable states. Under the same state graph, both are roughly O(number of states times transitions per state); actual constants and reachability differ.

Use a distinct marker for uncomputed versus computed-impossible. `null`, a parallel boolean array, or a value outside the result domain can work. A numeric zero is often a valid answer and cannot double as "not computed."

### Designing state dimensions

Common dimensions include index, remaining budget, last choice, mask of used items, transaction count, and whether an action is currently allowed. Every extra dimension multiplies state count. State `(index, remaining)` with n indexes and budget B has O(nB) states before transitions.

If a memo key includes a mutable list or unordered representation, equality and hashing can be expensive or unstable. Prefer dense integer indexes, records with immutable components, or bit masks for small sets.

Pseudo-polynomial DP such as O(nB) is polynomial in numeric value B, not in the number of bits needed to encode B. It can be infeasible when B is one billion even if n is small.

### Space compression and reconstruction

Compress only after identifying dependencies. If row i uses only row i-1, two rows may suffice. If each cell uses the updated value to its left and the previous-row value above, update direction determines which versions remain available.

Compression can destroy information needed to reconstruct chosen actions. To return the solution, retain parent decisions, recompute portions, or use a divide-and-conquer reconstruction. State whether the problem asks only for optimal value or also the actual choices.

Use safe sentinels. Adding one to `Integer.MAX_VALUE` overflows. Either skip impossible predecessors or choose a bounded sentinel whose arithmetic cannot overflow under constraints.

## Worked Java example

This Java 21 program contrasts a proven greedy interval algorithm, dynamic programming for coin change, and duplicate-aware permutation backtracking.

```java
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;

public final class OptimizationPatterns {
    record Interval(int start, int end) {
        Interval {
            if (end < start) throw new IllegalArgumentException("negative interval");
        }
    }

    static int maxNonOverlapping(Interval[] input) {
        Interval[] intervals = input.clone();
        Arrays.sort(intervals, Comparator
                .comparingInt(Interval::end)
                .thenComparingInt(Interval::start));

        int selected = 0;
        int previousEnd = Integer.MIN_VALUE;
        for (Interval interval : intervals) {
            if (interval.start() >= previousEnd) {
                selected++;
                previousEnd = interval.end();
            }
        }
        return selected;
    }

    static int minCoins(int[] coins, int amount) {
        if (amount < 0) throw new IllegalArgumentException("negative amount");
        for (int coin : coins) {
            if (coin <= 0) throw new IllegalArgumentException("nonpositive coin");
        }

        int impossible = Integer.MAX_VALUE / 4;
        int[] dp = new int[amount + 1];
        Arrays.fill(dp, impossible);
        dp[0] = 0;

        for (int subtotal = 1; subtotal <= amount; subtotal++) {
            for (int coin : coins) {
                if (coin <= subtotal && dp[subtotal - coin] != impossible) {
                    dp[subtotal] = Math.min(
                            dp[subtotal], dp[subtotal - coin] + 1);
                }
            }
        }
        return dp[amount] == impossible ? -1 : dp[amount];
    }
```

Duplicate-aware permutation generation continues inside the same `OptimizationPatterns` class:

```java

    static List<List<Integer>> uniquePermutations(int[] input) {
        int[] values = input.clone();
        Arrays.sort(values);
        List<List<Integer>> answer = new ArrayList<>();
        backtrack(values, new boolean[values.length],
                new ArrayList<>(), answer);
        return List.copyOf(answer);
    }

    private static void backtrack(int[] values, boolean[] used,
            List<Integer> path, List<List<Integer>> answer) {
        if (path.size() == values.length) {
            answer.add(List.copyOf(path));
            return;
        }

        for (int i = 0; i < values.length; i++) {
            if (used[i]) continue;
            if (i > 0 && values[i] == values[i - 1] && !used[i - 1]) continue;

            used[i] = true;
            path.add(values[i]);
            backtrack(values, used, path, answer);
            path.remove(path.size() - 1);
            used[i] = false;
        }
    }

    public static void main(String[] args) {
        Interval[] meetings = {
                new Interval(1, 4), new Interval(2, 3),
                new Interval(3, 5), new Interval(5, 7)};
        System.out.println(maxNonOverlapping(meetings)); // 3
        System.out.println(minCoins(new int[] {1, 3, 4}, 6)); // 2
        System.out.println(uniquePermutations(new int[] {1, 1, 2}));
    }
}
```

The interval method treats touching half-open intervals as compatible (`start >= previousEnd`). If endpoints are closed and sharing an endpoint conflicts, the comparison must be strict.

## Execution or memory walkthrough

For interval scheduling, sorting by end gives `(2,3), (1,4), (3,5), (5,7)`. Select `(2,3)`. Reject `(1,4)` because it starts before 3. Select `(3,5)` and `(5,7)`, producing three. The exchange proof says choosing `(2,3)` cannot leave less room than any other first selection.

For coins `[1,3,4]`, amount 6, the table becomes:

| Amount | 0 | 1 | 2 | 3 | 4 | 5 | 6 |
|---|---:|---:|---:|---:|---:|---:|---:|
| Minimum coins | 0 | 1 | 2 | 1 | 1 | 2 | 2 |

At amount 6, coin 3 uses `dp[3] + 1 = 2`; coin 4 uses `dp[2] + 1 = 3`. The optimal recurrence considers every possible final coin, so the better `3+3` result is retained.

For permutations of `[1,1,2]`, sorting makes equal choices adjacent. At an empty path, index 1 is skipped while identical index 0 is unused. This prevents duplicate root branches. Once index 0 is used, index 1 can be chosen later, so valid sequences with both ones remain. Each recorded path is copied before undo.

## Complexity and performance

| Technique | Time | Auxiliary space | Output consideration |
|---|---:|---:|---|
| Linear recursion | O(n) | O(n) call stack | None |
| Binary choice enumeration | O(2^n) | O(n) path | Up to O(2^n) outputs |
| Permutations | O(n * n!) | O(n) excluding output | n! lists of length n |
| Interval scheduling | O(n log n) | Sort-dependent | O(1) for count |
| Memoized states S, transitions T | O(S*T) | O(S) plus stack | Reachable states only |
| Tabulated states S, transitions T | O(S*T) | O(S) | Iteration order required |
| Coin DP | O(amount * coinCount) | O(amount) | Pseudo-polynomial |

The backtracking output itself dominates: for distinct values there are n! permutations, each copied in O(n). No algorithm materializing all permutations can be sublinear in that output.

The interval method clones input before sorting, using O(n) extra references and O(n log n) time. `Arrays.sort` details vary by element type; the algorithmic decision is sort then scan. Coin DP allocates amount + 1 integers and can fail due to memory long before integer indexing reaches its theoretical maximum.

> **HotSpot note:** HotSpot may inline recursive calls but does not promise tail-call elimination. Deep recursion still consumes thread stack. Boxing path integers and allocating output lists remain material costs even when recursive control is optimized.

## Edge cases and common mistakes

- Missing base case or a recursive transition that does not make progress.
- Calculating recursion complexity from depth while ignoring branching.
- Treating total calls as simultaneous stack space.
- Recording the same mutable path reference instead of a snapshot.
- Forgetting the unchoose phase after a recursive call.
- Applying positive-value pruning when negative choices exist.
- Skipping duplicates globally instead of only at the relevant decision level.
- Claiming a greedy rule because it works on sample inputs without a proof.
- Using a canonical coin-system intuition for arbitrary denominations.
- Defining DP state without enough history to determine the future.
- Using zero as both a valid result and an uncomputed marker.
- Filling table states before their dependencies.
- Iterating a compressed 0/1 DP in the direction that reuses an item.
- Adding to an impossible sentinel and overflowing.
- Calling O(nB) polynomial without noting that B is numeric magnitude.
- Compressing state before considering path reconstruction.

## Production engineering notes

Exponential search requires explicit limits: maximum depth, candidates, outputs, deadline, and cancellation. Return an iterator or visitor when materializing all solutions is unnecessary. Order outputs deterministically when tests, caches, or users depend on it.

Recursion over untrusted depth can exhaust a worker thread. Convert to an explicit stack or reject excessive nesting. Memo tables need bounded keys and lifecycle; caching a high-dimensional state across requests can become a memory leak.

Greedy scheduling models must match operational constraints. Earliest finish is optimal for maximizing count of unweighted compatible intervals, not for weighted value, setup time, multiple machines, or fairness. Each added constraint can invalidate the exchange proof and require DP, flow, or approximation.

DP tables can expose a denial-of-service surface when dimensions come directly from client numeric values. Validate budgets before allocation. For monetary or large objectives, choose numeric types and infinity sentinels deliberately.

## Interview questions and model answers

**How do you derive a recursive solution?**

Define the exact return contract for one state, identify direct base states, express larger states through strictly progressing smaller states, and combine their results. Then count the recursion tree and maximum active depth separately.

**What is the backtracking invariant?**

The mutable path contains exactly the choices from the root to the current search state, and auxiliary used-state matches that path. After each recursive call, undo restores the caller's state before the next candidate.

**How do you prove a greedy algorithm?**

Show an optimal solution can adopt the greedy choice without becoming worse, show greedy stays ahead after every prefix, or use a structural cut property. Samples and intuition are not proofs; actively seek a counterexample.

**How do you recognize DP?**

There are alternative choices, optimal substructure, and repeated states whose future depends only on a compact summary. Define the state first. If a safe greedy proof exists, DP may be unnecessary; if states do not overlap, plain recursion or divide-and-conquer may suffice.

**Memoization or tabulation?**

Memoization is natural for sparse reachable state spaces and recursive recurrences but uses call stack. Tabulation controls order and avoids recursion but may fill unused states. With the same visited states and transitions their asymptotic work is similar.

**When is DP space compression safe?**

When future transitions depend only on a limited subset of already computed layers and the update order preserves the old values still needed. It may be unsafe when reconstruction or same-row dependencies require discarded information.

## Exercises

1. Generate all subsets, then all fixed-size combinations, and state how candidate indexing avoids duplicates.
2. Solve N-Queens with column and diagonal occupancy arrays; derive output-sensitive complexity.
3. Produce a counterexample for shortest-duration-first interval scheduling.
4. Extend interval scheduling to weighted intervals with binary search plus DP.
5. Implement top-down and bottom-up edit distance and compare state definitions.
6. Solve 0/1 knapsack with one row; explain why capacity iterates downward.
7. Reconstruct the actual minimum-coin selection rather than only its count.
8. Add a deadline and output cap to permutation generation without leaving mutable state corrupted.

## Chapter summary

Recursion begins with a return contract and progress toward base cases. Backtracking explores choices while preserving and restoring one path state. Greedy algorithms discard alternatives only when an exchange, staying-ahead, cut, or invariant proof permits it. Dynamic programming caches a sufficient state over overlapping subproblems; its specification includes state, transition, base, order, and answer. Space compression and pruning are correctness transformations that require proof, not automatic optimizations.

## Revision checklist

- [ ] I define a recursive contract, base cases, progress, and combination.
- [ ] I separate recursion-tree time from maximum stack depth.
- [ ] I implement choose, explore, and unchoose symmetrically.
- [ ] I copy mutable paths when recording results.
- [ ] I prove duplicate and feasibility pruning is safe.
- [ ] I demand a greedy proof and search for counterexamples.
- [ ] I specify DP state, transition, base, order, and answer.
- [ ] I distinguish uncomputed, impossible, and valid zero states.
- [ ] I explain pseudo-polynomial dimensions and sentinel safety.
- [ ] I compress DP space only after proving dependency and reconstruction needs.
