# Dynamic Programming Practice Lab Solutions

1. `fib(n-2)` appears directly and again inside `fib(n-1)`; the recurrence tree repeats many argument values.
2. `dp[i]` is the maximum amount from houses `[0,i)`. Transition is max of skipping house `i-1` and taking it plus `dp[i-2]`.
3. Memoization can avoid unreachable states but retains recursion overhead/depth. Tabulation has explicit order and good locality but may evaluate every table cell.
4. Use a separate visited array, nullable wrappers, or a sentinel outside the valid answer range. Do not confuse a legitimate zero with missing state.
5. Iterate capacity downward so each item reads only states from the previous conceptual item layer. Upward iteration permits reuse.
6. `dp[row][column]` counts paths to that cell; obstacles contribute zero, and other cells add top plus left. Use long, exact arithmetic, modulo, or big integers according to the contract.
7. O(n squared) DP stores predecessor index when a better length ends at i, then follows predecessors from the best endpoint. O(n log n) reconstruction tracks tail indexes and predecessors.
8. Coins outer and amounts upward count combinations without order. Amount outer and coins inner count ordered sequences under the corresponding recurrence.
9. Walk backward from `(m,n)`: a diagonal match emits nothing, diagonal mismatch emits replace, upward emits delete, and left emits insert. Tie-breaking defines which optimal script is returned.
10. `dp[day][holding]` is best profit from a day under current holding status. Transitions compare rest with buy/sell, extended for transaction count, fee, or cooldown.
11. State over interval `[left,right]`; try every final split/burst and combine independent subinterval answers plus boundary cost. Evaluate by increasing interval length.
12. Each node returns two values: best when node is taken and when skipped. Taking forces children skipped; skipping allows the better child state independently.
13. O(n * amount) is polynomial in numeric amount but exponential in the number of bits required to encode a potentially huge amount.
14. Compress only after dependency analysis. Keep the full table or parent choices when an actual solution path is required and memory permits.
15. DP evaluates a DAG of states, greedy commits using a proof, and shortest path optimizes edge-cost accumulation. Equivalent state graphs can reveal alternative algorithms, but edge weights and acyclicity determine validity.
16. Let `reachable[end]` mean prefix `[0,end)` can be segmented, with `reachable[0]=true`. From every reachable start, try dictionary words or endings up to the maximum word length; record the predecessor start on the first successful transition to each end. Reconstruct backward from n and reverse. The abstract transition count can be O(nL), but Java substring creation and hashing add character/allocation cost; a trie or region comparison can avoid temporary strings.
