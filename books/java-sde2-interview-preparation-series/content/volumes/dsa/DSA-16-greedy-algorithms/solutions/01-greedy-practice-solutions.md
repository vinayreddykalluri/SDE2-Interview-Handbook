# Greedy Algorithms Practice Lab Solutions

1. Choose the compatible interval with earliest finish. Replace the first interval of an optimal schedule with it; later intervals remain feasible, so an optimal schedule using the choice exists.
2. Place a short interval across the boundary between two longer but mutually compatible intervals; choosing it blocks both and yields one instead of two.
3. `[a,b)` and `[b,c)` do not overlap, so `next.start >= previous.end`. Closed intervals share point b and may require strict comparison.
4. Use `Comparator.comparingInt(Interval::end).thenComparingInt(Interval::start)`; subtraction can overflow.
5. Fractions let density choices be partially taken. Indivisible items can make one high-density item block a better combination; 0/1 knapsack needs DP or another exact method.
6. Sort by start and extend the current end on overlap. It computes union coverage groups, not a maximum cardinality compatible subset.
7. Record the last index of each symbol. Scan while extending the current partition end; close a partition when the current index reaches that end.
8. Standard denominations may be canonical, but arbitrary systems fail. For coins `{1,3,4}` and amount 6, greedy gives `4+1+1` while optimum is `3+3`.
9. Repeatedly combine the two smallest weights, add their sum to total, and reinsert. The exchange/coding-tree argument makes smallest-first optimal.
10. Sort by increasing deadline. An interchange argument shows swapping an inverted pair does not increase maximum lateness.
11. Reachability tracks one farthest reachable boundary. Minimum jumps tracks current and next BFS-layer boundaries and increments only when completing a layer.
12. The lightest safe edge crossing a cut can be included in some MST; replacing a heavier crossing edge preserves connectivity without increasing cost.
13. Identify the information needed to compare alternative histories, such as best value up to item/index/capacity, then define transitions instead of committing irrevocably.
14. Add a secondary key only among choices equivalent under the proof's primary criterion. Confirm it does not change feasibility or objective.
15. Offline algorithms see all requests and can sort. Online algorithms need competitive objectives, buffering, preemption, or approximation; exact offline optimality may be impossible with irrevocable decisions.
16. Initialize one candy each. A left-to-right pass raises a child's count above a lower-rated left neighbor; a right-to-left pass raises it above a lower-rated right neighbor, taking the maximum with the existing count. Each direction establishes a necessary lower bound, and their pointwise maximum satisfies both without excess. Time is O(n), auxiliary space O(n), and the total should use `long` for large n.

## Proof and counterexample solutions

17. Replace an optimal schedule's first interval with greedy's earliest-finishing interval; it ends no later, so every later compatible interval remains feasible. Repeat. Earliest-start fails for `[0,100)` together with `[1,2),[2,3),[3,4)`: it selects one instead of three.
18. Sort copied records by end, then start, then ID. Select when `start>=lastEnd` under half-open semantics and return original records/IDs. Equal-end tie choice does not reduce maximum count, while deterministic ordering makes tests and downstream explanation stable.
19. `currentLayerEnd` bounds indexes reachable in current jump count; scanning them accumulates `farthest` for the next layer. At layer end increment jumps and advance boundary. If scan index exceeds farthest, return -1. Compute `index+steps[index]` in long and clamp to destination.
20. Track total net and current candidate tank. When tank becomes negative at `i`, no start from the candidate through `i` can cross that failing edge with more fuel, so reset to `i+1`. Total negative proves impossibility; otherwise the final candidate works. Accumulate in long.
21. Add every station at/before reachable frontier to a max-heap. When target is still beyond reach, choose largest passed fuel; exchanging it for any smaller chosen refill cannot reduce reach and uses one stop. If heap is empty, fail. Clamp reach at target to prevent overflow and validate sorted station positions.
22. Enumerate all subsets for at most ten intervals, sort chosen intervals, and compute maximum compatible count. For jumps, use `O(n^2)` DP/BFS distance as oracle. Compare seeded random cases. These tests find counterexamples and coding errors but do not replace the exchange/frontier proof over all inputs.
