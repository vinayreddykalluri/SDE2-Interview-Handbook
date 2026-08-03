# Greedy Proof and Counterexample Workshop

A greedy algorithm commits to a locally attractive choice without revisiting it. The loop is often short; the proof is the real solution. At SDE-2 level, “it seems optimal” is not enough. Name the choice, show why an optimal solution can include it, and state the invariant after committing.

The executable interval, jump, gas-station, and refueling implementations are in `GreedyInterviewChecks.java`.

## Recognition: choice plus irreversible safety

A problem is a greedy candidate when:

- one decision can be ordered by a useful criterion;
- committing to the best current criterion leaves an equivalent smaller problem; and
- an exchange, cut, dominance, or frontier argument proves no optimum is lost.

Greedy is suspicious when choices consume resources in ways that depend on future combinations. Coin change with arbitrary denominations is the standard warning: choosing the largest coin first fails for coins `[1,3,4]`, amount `6` (`4+1+1` versus optimal `3+3`).

## Interval scheduling: earliest finish leaves most room

For maximum number of compatible half-open intervals `[start,end)`, sort by end and select an interval when `start >= lastEnd`.

Exchange proof:

1. Let greedy choose interval `g` with earliest finish.
2. Let an optimal schedule begin with `o`.
3. Since `g.end <= o.end`, replace `o` with `g`.
4. Every interval compatible after `o` is still compatible after `g`.
5. An optimum exists containing the greedy choice; repeat on the suffix.

The companion returns the selected original records and IDs, not only a count. Deterministic tie-breaks make the result auditable.

Sorting by earliest start is not safe: a very long early interval can block many short ones. A counterexample is part of explaining the correct criterion.

## Minimum jumps as BFS layers without a queue

For each index, `farthest` is the greatest position reachable using one more jump from any index in the current layer. `currentLayerEnd` marks the end of positions reachable with the current number of jumps.

```text
values: [2,3,1,1,4]
layer 0 indexes: [0]       farthest becomes 2
layer 1 indexes: [1,2]     farthest becomes 4
destination reached in 2 jumps
```

When scanning reaches `currentLayerEnd`, commit one jump and extend the layer to `farthest`. If the scan index moves beyond `farthest`, the destination is unreachable.

The proof is the same as unweighted BFS: all indexes in a layer are reachable with the same minimum jump count, and the next layer is their union of outgoing ranges.

## Gas station: discard an impossible prefix

Let `net[i] = gas[i] - cost[i]`. If total net is negative, no start can complete the circle. While scanning a candidate start, if running tank becomes negative at `i`, no station between that candidate and `i` can be a valid start: each would begin with no more accumulated surplus yet face the same failing suffix. Set candidate to `i+1` and reset the local tank.

Use `long` for total and tank. Validate nonnegative gas/cost and equal nonempty arrays. The proof assumes every segment is traversed in fixed circular order.

## Refueling: defer the choice until necessary

As current fuel makes stations reachable, add their fuel amounts to a max-heap but do not stop immediately. When the target is not reachable, retroactively choose the largest fuel among all passed stations.

Why this is safe: every passed station was reachable before the current failure. Replacing any smaller chosen refill with the largest passed refill cannot reduce reach and uses the same one stop.

```text
reachable frontier -> collect station fuels
frontier cannot advance -> take largest collected fuel
repeat or report impossible
```

This produces the minimum number of stops, not minimum fuel cost. Adding prices, tank capacity, mandatory stops, or time windows changes the problem and can invalidate the greedy rule.

## Test a greedy algorithm against an oracle

Proof is primary, but exhaustive small cases catch implementation bugs and false intuitions.

The companion:

- enumerates every interval subset for small random instances and compares maximum cardinality; and
- computes minimum jumps with a small dynamic-programming oracle and compares the greedy frontier result.

An oracle disagreement does not automatically prove which implementation is wrong, but it gives a reproducible counterexample for reasoning.

## Greedy versus dynamic programming

| Signal | Greedy | Dynamic programming |
|---|---|---|
| decision can be safely exchanged into an optimum | strong candidate | may be unnecessary |
| choice affects several future resource dimensions | risky | model dimensions as state |
| need only current frontier summary | often | sometimes space-optimized DP |
| arbitrary coin denominations | generally fails | standard DP |
| interval maximum count | earliest-finish proof | possible but excessive |
| weighted interval profit | earliest finish no longer enough | DP after predecessor search |

Do not choose DP merely because it feels safer; do not choose greedy merely because it is shorter.

## Edge-case matrix

| Case | Correct handling | Common failure |
|---|---|---|
| touching intervals | define half-open/closed semantics | inconsistent `>=` versus `>` |
| invalid interval | reject before sorting/selection | negative-duration reasoning |
| equal finish times | deterministic safe tie policy | nondeterministic outputs |
| empty interval list | empty selected list | sentinel counted as interval |
| empty/single jump array | zero jumps by stated contract | indexing position zero |
| unreachable jump prefix | return explicit failure | increment jumps forever |
| huge jump length | compute reach in `long`, clamp | index addition overflow |
| total gas negative | return impossible | returning reset candidate |
| target already fueled | zero stops | consuming a station anyway |
| zero-fuel station | harmless but cannot advance | infinite refuel loop |
| unsorted stations | reject or sort a copy | missing reachable stations |
| accumulated fuel overflow | clamp at target | wrapped negative reach |

## Six live interview Q&A chains

### 1. Interval criterion

**Interviewer:** Why not choose the shortest interval?

**Candidate:** Duration alone does not determine how much future timeline remains. Earliest finish has an exchange proof: replacing an optimal first interval with it cannot block a later compatible interval.

### 2. Weighted intervals

**Interviewer:** What if intervals have profit?

**Candidate:** Maximum count's earliest-finish proof no longer optimizes profit. I would sort by finish, binary-search the predecessor, and use DP over take/skip profit.

### 3. Jump frontier

**Interviewer:** Why increment jumps only at the layer end?

**Candidate:** Every index up to that boundary is reachable with the same minimum jump count. I first aggregate all their next reach, then one committed jump moves to that complete next layer.

### 4. Gas reset

**Interviewer:** Why can you discard every start between the candidate and failure?

**Candidate:** Starting later removes a prefix whose cumulative tank was nonnegative before eventual failure; it cannot give more tank at the failing edge than the original candidate. None can cross it.

### 5. Refueling choice

**Interviewer:** Are you pretending we stopped at an earlier station after passing it?

**Candidate:** The heap is an equivalent deferred-decision model. All stored stations were reachable. When a stop becomes necessary, choosing the largest one among them can be exchanged with any smaller prior choice without increasing stop count.

### 6. Proof confidence

**Interviewer:** Your randomized oracle passed. Is the greedy algorithm proven?

**Candidate:** No. Tests sample finite cases and catch bugs; the exchange/frontier argument establishes general correctness. I use both: proof for the algorithm, oracle for the implementation.

## Run the companion

```bash
javac --release 21 -Xlint:all -Werror GreedyInterviewChecks.java
java GreedyInterviewChecks
```

Expected final line: `PASS 15 greedy checks`.
