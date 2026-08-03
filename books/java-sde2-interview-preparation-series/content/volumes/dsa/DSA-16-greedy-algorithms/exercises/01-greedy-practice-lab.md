# Greedy Algorithms Practice Lab

1. State the greedy rule and exchange proof for activity selection.
2. Build a counterexample to shortest-interval-first.
3. Explain half-open versus closed interval compatibility.
4. Debug a comparator that subtracts interval endpoints.
5. Debug fractional-knapsack logic applied to 0/1 items.
6. Merge overlapping intervals and explain why this is not activity selection.
7. Partition a string so each symbol appears in at most one part.
8. Prove or refute a greedy rule for coin change on arbitrary denominations.
9. Build Huffman merge cost with a min-heap.
10. Schedule jobs to minimize maximum lateness.
11. Compare Jump Game reachability and minimum jumps invariants.
12. Explain the cut property used by minimum spanning tree algorithms.
13. Convert a failed greedy formulation into a DP state.
14. Design deterministic tie-breaking without changing optimality.
15. Discuss online versus offline scheduling limitations.
16. **Interview Core:** Derive the minimum-candy allocation with two directional passes and prove why taking the maximum of the two lower bounds is minimal.

## Proof and counterexample lab

17. **Foundation:** Give an exchange proof for earliest-finish interval scheduling and a counterexample to earliest-start.
18. **Interview Core:** Return the chosen original interval IDs under half-open semantics and deterministic tie-breaking.
19. **Interview Core:** Implement minimum jumps as BFS layers without a queue and return explicit failure for an unreachable suffix.
20. **Interview Core:** Prove the gas-station prefix reset and use `long` for cumulative net fuel.
21. **Interview Core:** Implement minimum refueling stops with deferred max-heap choices and overflow-safe reach clamping.
22. **SDE-2 Follow-up:** Compare interval greedy with exhaustive subsets and jump greedy with a DP oracle on small random inputs; preserve the proof separately.
