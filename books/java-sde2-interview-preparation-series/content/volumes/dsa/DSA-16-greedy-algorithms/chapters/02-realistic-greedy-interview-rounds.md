# Realistic Greedy Interview Rounds

## Round 1: remove the fewest overlapping intervals

### Prompt

Return the minimum number of half-open intervals to remove so the rest do not overlap.

### Candidate derivation

Minimizing removals is equivalent to maximizing retained compatible intervals. Sort by end, retain each interval whose start is at least the last retained end, and remove the rest.

```java
static int minimumRemovals(int[][] input) {
    int[][] intervals = Arrays.stream(input).map(int[]::clone).toArray(int[][]::new);
    Arrays.sort(intervals, Comparator.comparingInt(interval -> interval[1]));
    int kept = 0;
    int lastEnd = Integer.MIN_VALUE;
    for (int[] interval : intervals) {
        if (interval[0] >= lastEnd) {
            kept++;
            lastEnd = interval[1];
        }
    }
    return intervals.length - kept;
}
```

The exchange proof for activity selection applies. Time is O(n log n), auxiliary space depends on whether cloning/preserving input is required.

### Follow-up

**Weighted intervals?** Earliest finish no longer maximizes value. Sort by end, locate previous compatible intervals, and use dynamic programming.

**Streaming intervals?** Without future knowledge, an irrevocable optimal offline selection is generally not available. State the online objective or approximation.

## Round 2: gas station circuit

### Prompt

Given gas gained and travel cost at each station, return a start index that completes the circular route, or -1.

### Candidate answer

If total net gas is negative, no start works. While scanning, if the current candidate's running tank becomes negative at index `i`, no station between the candidate and `i` can succeed, so the next candidate is `i + 1`.

```java
static int startStation(int[] gas, int[] cost) {
    if (gas.length != cost.length || gas.length == 0) {
        throw new IllegalArgumentException("invalid arrays");
    }
    long total = 0L;
    long tank = 0L;
    int start = 0;
    for (int i = 0; i < gas.length; i++) {
        long net = (long) gas[i] - cost[i];
        total += net;
        tank += net;
        if (tank < 0L) {
            start = i + 1;
            tank = 0L;
        }
    }
    return total >= 0L ? start % gas.length : -1;
}
```

### Follow-up answers

**Why can skipped starts not work?** They begin with no more accumulated surplus than the failed candidate had before reaching the same failing point, so each also runs negative by then.

**Is the answer unique?** Not necessarily. The method returns one valid start under the standard contract.

## Round 3: minimum jumps to reach the end

### Prompt

Each nonnegative array value is maximum jump length. Return the minimum number of jumps to reach the last index; return -1 if unreachable.

### Candidate explanation

Interpret reachable indexes as BFS layers without storing a queue. `currentEnd` is the boundary reachable with the current number of jumps; `farthest` is the boundary reachable with one additional jump from the current layer.

```java
static int minimumJumps(int[] jumps) {
    if (jumps.length <= 1) return 0;
    int jumpsUsed = 0;
    int currentEnd = 0;
    int farthest = 0;
    for (int index = 0; index < jumps.length - 1; index++) {
        if (index > farthest) return -1;
        long reach = (long) index + jumps[index];
        farthest = (int) Math.min(jumps.length - 1L, Math.max(farthest, reach));
        if (index == currentEnd) {
            jumpsUsed++;
            currentEnd = farthest;
            if (currentEnd >= jumps.length - 1) return jumpsUsed;
        }
    }
    return -1;
}
```

### Follow-up answers

**Proof intuition?** All indexes through currentEnd form one BFS layer. Expanding every index in that layer finds the farthest boundary of the next layer. Advancing a layer once preserves minimum edge count.

**Why not choose the largest immediate jump?** The landing position's future reach matters. The layer method evaluates all positions reachable with the same jump count.

## Closing answer pattern

State the local choice, rejected tempting choices, exchange/stays-ahead proof, endpoint and mutation contracts, sorting cost, numeric safety, and a nearby problem where greedy fails.
