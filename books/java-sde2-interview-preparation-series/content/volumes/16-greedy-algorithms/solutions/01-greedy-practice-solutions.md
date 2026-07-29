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
