# Graph Practice Lab Solutions

1. A task/service step is a vertex; an edge points from prerequisite to dependent if topological processing should flow forward. State whether duplicate dependencies are meaningful.
2. Lists use O(V + E) storage and traverse neighbors efficiently for sparse graphs. Matrices use O(V squared), make edge checks O(1), and may suit dense moderate graphs.
3. Marking when enqueued makes frontier membership equivalent to discovered state and prevents duplicate enqueues.
4. Carry the parent vertex in DFS/BFS. An already visited neighbor is a cycle only when it is not the edge back to the parent; parallel edges require an edge-identity policy.
5. Dijkstra's finalized-minimum argument fails with negative edges. Use Bellman-Ford, DAG relaxation, or another algorithm based on constraints.
6. Loop over every vertex; start a traversal from each unseen one and increment the count. Total O(V + E).
7. BFS stores `parent[neighbor] = node` on first discovery. Walk backward from target and reverse. If undiscovered, no path exists.
8. Assign alternating colors during BFS/DFS. Check every component and fail on an edge whose endpoints share a color.
9. Use a deque. Relax weight-0 edges to the front and weight-1 edges to the back; stale-distance checks keep processing sound.
10. `find` compresses paths; `union` attaches smaller root under larger. Operations are near constant amortized and support connectivity, not paths.
11. Kruskal sorts edges and unions components; Prim grows from a start with a heap. A spanning tree requires exactly V-1 accepted edges for nonempty connected input.
12. Topological sort is an ordering/cycle problem in DAGs. Shortest path optimizes cost; a DAG can use topological order as its evaluation schedule.
13. DSU handles undirected additions and connectivity efficiently. Deletions or directed reachability require different structures/offline techniques.
14. A chain can create O(V) call depth and `StackOverflowError`; use an explicit deque for untrusted depth.
15. Partition vertices/edges, stream adjacency, use compressed IDs, external storage, frontier batching, and explicit consistency/failure contracts. Complexity alone does not solve distribution.
16. Enqueue all sources at distance zero and mark them discovered before traversal. Ordinary BFS then processes distance layers from the complete source set, equivalent to adding a synthetic source with zero-cost edges. Each traversable cell enters once, so time and output storage are O(rows*columns).
17. Use BFS for equal weights, deque-based 0-1 BFS for weights zero or one, Dijkstra for arbitrary nonnegative weights, topological relaxation for a DAG, Bellman-Ford when negative edges may exist, and Floyd-Warshall for suitable moderate all-pairs cases. Bellman-Ford relaxes all edges up to V-1 times; any further reachable improvement proves a reachable negative cycle.

## Advanced graph implementation solutions

18. Compute a topological order and reject if it emits fewer than `V` vertices. Initialize source zero and others infinity; skip unreachable vertices and relax outgoing edges in order. Negative edges are valid because every predecessor appears before its destination. The companion's `dagShortestPaths` returns `[0,-1,2,1,INF]` on its signed-edge fixture and preserves the unreachable vertex.

19. Relax all edges up to `V-1` passes, skipping unreachable `from` vertices and stopping after an unchanged pass. On one extra scan, seed every still-improvable reachable destination, then BFS/DFS outward to mark all vertices whose distances are undefined. Do not seed an edge from infinity. Return both numeric distances and flags because unreachable, finite, and negative-cycle-affected are three distinct states.

20. Initialize diagonal zero, other cells to a separated infinity sentinel, and parallel edges with their minimum weight. At phase `k`, combine only when both halves are reachable and use guarded `long` addition. A negative diagonal reports a negative cycle. Global cycle existence does not alone say every source-target pair is affected; pair-level invalidation also requires reachability through a negative-diagonal vertex.

21. Mark the start included and push its incident edges. Repeatedly remove the lightest edge; skip it when both endpoints share visited state, otherwise add the excluded endpoint, record the original edge, add weight in `long`, and push new crossing edges. Success requires exactly `V-1` chosen edges for nonempty input. Returning edge IDs makes the result auditable; `spansAllVertices=false` distinguishes a component tree from an MST.

22. Kosaraju first records original-graph finish order, reverses edges, then explores in decreasing finish time to collect SCCs. If the condensation contained a directed cycle, all meta-vertices on that cycle would be mutually reachable and should have been one SCC, a contradiction. Thus condensation is a DAG. Include isolated vertices as singleton components.

23. Store `discovery` and `low`. DFS carries the exact incoming edge ID, not only parent vertex. Skip that one edge; a second parallel edge to the parent lowers `low` as a back edge. Add tree edge ID when `low[child] > discovery[parent]`. A nonroot parent is articulation when `low[child] >= discovery[parent]`; a root needs more than one DFS child. Reject duplicate IDs because result identity would be ambiguous.

24. Generate edges only from lower to higher indexes, guaranteeing a DAG while permitting signed weights. For every source, compare topological distances, Bellman-Ford distances, and the corresponding Floyd-Warshall row; assert no negative-cycle flag. Use a small signed range for duplicates and alternate routes, and a fixed seed for replay. This triangulation is stronger than comparing two algorithms that might share the same defect; fixed targeted cycle and infinity tests remain necessary.
