# Heaps and Priority Queues Practice Lab

1. Draw the array indexes for a seven-node complete binary tree.
2. State the min-heap invariant and explain why index 0 is minimal.
3. Predict whether iterating a `PriorityQueue` constructed from several values would produce sorted order.
4. Debug comparator `(a, b) -> b.score - a.score`.
5. Debug a heap of mutable jobs whose priority changes after insertion.
6. Implement sift-down and bottom-up heapify.
7. Return the k smallest values without sorting all input.
8. Return top-k frequent values with deterministic tie-breaking.
9. Merge k sorted arrays while preserving source positions.
10. Schedule meeting rooms and return the minimum room count.
11. Implement a running median with overflow-safe averaging.
12. Compare sorting, bounded heap, and quickselect for kth-largest selection.
13. Explain lazy deletion and its memory-cleanup obligation.
14. Design a stable priority scheduler with equal priorities.
15. Explain why arbitrary deletion is a boundary for Java `PriorityQueue`.
16. **Interview Core:** Return the k closest points with a bounded max-heap, overflow-safe squared distance, and deterministic tie-breaking.
17. **SDE-2 Follow-up:** Find the smallest range containing at least one value from each of k sorted lists. Explain why only the list supplying the current minimum advances.

## Heap and selection internals lab

18. **Foundation:** Map every parent and child index in heap array `[1, 3, 2, 8, 5, 7]`. Explain why the array is not globally sorted.
19. **Interview Core:** Implement a resizable primitive min-heap with bottom-up heapify, offer, peek, poll, and an invariant checker.
20. **Interview Core:** Prove bottom-up heapify is `O(n)` by grouping nodes by height; do not multiply every node by maximum height.
21. **Interview Core:** Implement iterative randomized three-way quickselect for a one-based kth-largest contract. Test duplicates, `k=1`, and `k=n`.
22. **SDE-2 Follow-up:** Compare custom heap behavior with `PriorityQueue` after every operation in a deterministic randomized sequence, then drain both.
23. **SDE-2 Follow-up:** Design a top-k stream contract when `k` may increase after values were discarded. State what history must be retained or why the request cannot be answered exactly.
