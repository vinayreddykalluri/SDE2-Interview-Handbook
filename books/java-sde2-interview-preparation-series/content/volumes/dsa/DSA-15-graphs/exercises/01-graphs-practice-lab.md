# Graph Practice Lab

1. Model a dependency service as vertices and directed edges.
2. Compare adjacency list and matrix for sparse and dense graphs.
3. Explain enqueue-time visited marking in BFS.
4. Debug undirected cycle detection that treats the parent edge as a cycle.
5. Debug Dijkstra used on a graph containing a negative edge.
6. Count connected components in an undirected graph.
7. Reconstruct one shortest unweighted path with parent pointers.
8. Test whether a graph is bipartite across all components.
9. Implement 0-1 BFS.
10. Implement disjoint-set union with size and path compression.
11. Return edges of a minimum spanning tree and detect disconnection.
12. Compare topological sort with shortest path.
13. Choose an algorithm for dynamic edge additions and connectivity queries.
14. Explain recursion depth risk in graph DFS.
15. Design memory boundaries for a graph too large for one process.
16. **Interview Core:** Compute every grid cell's distance to the nearest source with one multi-source BFS. Explain the synthetic-source equivalence.
17. **SDE-2 Follow-up:** Give the shortest-path decision rule for unweighted, 0/1, nonnegative, negative-edge, DAG, and all-pairs inputs. Include reachable negative-cycle detection.

## Advanced graph implementation lab

18. **Interview Core:** Implement DAG shortest paths with topological relaxation, negative edges, unreachable vertices, and explicit rejection of a cycle.
19. **Interview Core:** Implement Bellman-Ford returning finite distances plus an `affectedByNegativeCycle` flag for every vertex. Include reachable and unreachable negative cycles.
20. **Interview Core:** Implement Floyd-Warshall with a documented infinity sentinel, guarded addition, parallel-edge minimum, and negative-diagonal detection.
21. **Interview Core:** Implement Prim returning selected edge IDs, total `long` weight, and a non-spanning result for a disconnected graph.
22. **Interview Core:** Implement SCC decomposition and explain why the condensation graph is acyclic.
23. **SDE-2 Follow-up:** Return bridges and articulation points in an undirected multigraph. Carry parent edge ID so a parallel edge to the parent remains a back edge.
24. **SDE-2 Follow-up:** Differential-test DAG relaxation and Bellman-Ford from every source against Floyd-Warshall rows on randomly generated DAGs with signed weights.
