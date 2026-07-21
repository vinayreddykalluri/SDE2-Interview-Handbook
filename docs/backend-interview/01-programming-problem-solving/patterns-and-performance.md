# Coding Patterns and Performance

Patterns are retrieval aids, not substitutes for reasoning. Choose one because the constraints and invariant fit, not because a keyword appeared in the prompt.

## Pattern catalog

| Signal | Candidate patterns | Invariant to explain |
| --- | --- | --- |
| Contiguous range | Sliding window, prefix sum | Window or prefix represents the processed range |
| Sorted data | Binary search, two pointers | Discarded region cannot hold a better answer |
| Fast membership/grouping | Hashing | Map/set reflects all processed elements |
| Nested structure | Stack, recursion | Stack represents unfinished work or path |
| Hierarchical data | DFS, BFS | Visited/frontier prevents duplicate work |
| Dependencies | Topological sort | Removed nodes have no unresolved prerequisites |
| Shortest path | BFS, Dijkstra | Finalized distance cannot improve under assumptions |
| Overlapping subproblems | Memoization, tabulation | State answers a precise subproblem |
| Top or bottom K | Heap, selection | Structure retains only competitive candidates |
| Intervals | Sort and sweep | Processed intervals are merged or ordered |

## Performance beyond Big-O

Mention allocation rate, object overhead, cache locality, boxing, recursion depth, contention, I/O, serialization, and worst-case latency when relevant. `O(n)` over primitive arrays can behave differently from `O(n)` over pointer-heavy objects.

## Concurrency prompts

- What state is shared, and who owns mutation?
- Is atomicity required across one field or multiple fields?
- Can immutability, partitioning, or message passing remove locking?
- How are cancellation, timeout, backpressure, and partial failure represented?

## Ready when

For an unfamiliar problem, name two plausible patterns, reject one using constraints, define the winning invariant, and explain asymptotic and practical performance.
