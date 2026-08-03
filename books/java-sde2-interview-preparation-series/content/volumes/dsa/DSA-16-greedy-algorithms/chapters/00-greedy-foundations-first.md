# Greedy Foundations: A Local Choice Needs a Global Proof

A greedy algorithm commits to one locally attractive choice and does not revisit it. Short code is not evidence of correctness. A greedy solution needs structure showing that an optimal solution can begin with the chosen decision.

## Start with a choice sequence

For interval scheduling, the task is to select as many non-overlapping intervals as possible. Candidate rules include earliest start, shortest duration, fewest conflicts, and earliest finish. Only earliest finish has the standard proof for the unweighted objective.

Why earliest finish? It leaves the largest remaining timeline for all later intervals.

```java
record Interval(int start, int end) {}

static int maximumCompatible(List<Interval> input) {
    List<Interval> intervals = new ArrayList<>(input);
    intervals.sort(Comparator.comparingInt(Interval::end)
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
```

The endpoint comparison depends on the contract. For half-open meetings `[start, end)`, a meeting starting exactly at previous end is compatible. Closed intervals may require `start > previousEnd`.

## Exchange argument from zero

Let `g` be the earliest-finishing available interval. Consider an optimal schedule whose first interval is `o`. Because `g` finishes no later than `o`, replacing `o` with `g` cannot invalidate any later selected interval. Therefore some optimal solution begins with `g`. After choosing it, the remaining problem has the same form.

That proof supplies both:

- greedy-choice property: an optimal solution can start with the greedy choice;
- optimal substructure: the remaining choices form the same problem after the chosen finish.

## Counterexamples are a design tool

To reject "shortest interval first," construct one short interval placed so it blocks two compatible intervals. To reject "highest value first" for 0/1 knapsack, choose one valuable heavy item versus multiple lighter items with a better combined value.

A strong interview answer actively searches for a counterexample before committing to a greedy rule.

## Common proof styles

### Exchange

Transform an optimal solution to use the greedy choice without making it worse.

### Stays ahead

After each prefix of decisions, prove the greedy partial solution is at least as good on a progress measure as any competitor. Minimum-jump frontier reasoning often uses this style.

### Cut property

Show the lightest safe edge across a partition can belong to an optimal spanning tree.

### Greedy choice plus induction

Prove a greedy first choice is safe, then apply the same argument to the smaller remaining problem.

## Sort key is part of the proof

Many greedy algorithms appear as "sort, then scan." The sort key encodes the theorem. Changing it changes the algorithm. Tie behavior may affect determinism or even correctness when multiple fields control feasibility.

Use comparator composition rather than subtraction:

```java
Comparator<Job> order = Comparator.comparingInt(Job::deadline)
        .thenComparingInt(Job::duration)
        .thenComparing(Job::id);
```

## Greedy versus dynamic programming

Ask:

1. Does a locally best choice remain safe for every valid suffix?
2. Can I exchange it into an optimal solution?
3. Does the future need only a small summary of the past, or does it need to compare competing states?
4. Can I produce a small counterexample?

Unweighted activity selection is greedy. Weighted interval scheduling generally needs DP because accepting an interval trades its value against many alternative compatible schedules. Fractional knapsack is greedy by value density; 0/1 knapsack is not.

## Reachability greedy

In Jump Game, maintain the farthest reachable index while scanning only positions already reachable.

```java
static boolean canReachEnd(int[] jumps) {
    int farthest = 0;
    for (int index = 0; index < jumps.length && index <= farthest; index++) {
        farthest = Math.max(farthest, index + jumps[index]);
        if (farthest >= jumps.length - 1) return true;
    }
    return jumps.length == 0;
}
```

Invariant: every index through `farthest` is reachable using decisions from the processed reachable prefix. Use long or clamp arithmetic if index plus jump can overflow under the input contract.

## Foundation failure clinic

- Choosing the most intuitive rule without proof.
- Giving only examples that work instead of a proof.
- Confusing interval merge with maximum compatible selection.
- Using value/weight density for indivisible 0/1 choices.
- Sorting caller-owned input without permission.
- Ignoring endpoint semantics and comparator overflow.
- Saying O(n) while hiding O(n log n) sorting.

## Foundation checkpoint

1. State the greedy choice for maximum unweighted interval selection.
2. Give the exchange step in one sentence.
3. Why does a successful example not prove greedy correctness?
4. Name a similar-looking problem where the same rule fails.
5. What part of a sort-and-scan solution carries the proof?
