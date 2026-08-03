# Advanced Graph Algorithms, Implemented and Defended

Advanced graph interviews are usually lost before the code: the candidate applies a familiar algorithm to the wrong graph model. Start by stating direction, weight meaning, negative-edge policy, connectivity, parallel-edge policy, and the exact requested output. Then choose the invariant that matches those facts.

This chapter implements the advanced families that the earlier graph chapters introduced: DAG shortest paths, Bellman-Ford, Floyd-Warshall, Prim, strongly connected components, bridges, and articulation points. The complete Java code and executable checks live in `GraphInterviewChecks.java`.

## Decision map

| Question | Preconditions | Algorithm | Core bound |
|---|---|---|---:|
| one-source shortest paths in DAG | directed, acyclic; weights may be negative | topological relaxation | `O(V + E)` |
| one-source paths with negative edges | no assumption of nonnegative weight | Bellman-Ford | `O(VE)` |
| all-pairs paths on moderate graph | matrix-sized memory is acceptable | Floyd-Warshall | `O(V^3)` |
| minimum total connection | undirected weighted graph | Prim or Kruskal | commonly `O(E log V)` |
| mutually reachable groups | directed graph | Kosaraju/Tarjan SCC | `O(V + E)` |
| single points/edges of failure | undirected graph | low-link DFS | `O(V + E)` |

Shortest path, minimum spanning tree, and reachability decomposition answer different questions. Similar use of a priority queue or DFS does not make them interchangeable.

## DAG shortest paths: order makes relaxation final

A directed acyclic graph has a topological order in which every edge goes forward. Initialize the source to zero and all other distances to infinity, then relax outgoing edges in that order.

```text
edges: 0->1 (4), 0->2 (2), 2->1 (-3), 1->3 (2), 2->3 (5)
topological order: 0, 2, 1, 3, 4

after 0: dist = [0, 4, 2, INF, INF]
after 2: dist = [0,-1, 2,   7, INF]
after 1: dist = [0,-1, 2,   1, INF]
```

Negative edges are safe because no path can return to a processed vertex. Invariant:

> When vertex `u` is processed, every possible predecessor of `u` appears earlier, so `dist[u]` is final.

The companion derives the order with Kahn's indegrees and rejects a cycle. Silently running “DAG shortest path” on a cyclic graph invalidates the proof even if a particular sample happens to work.

## Bellman-Ford: reachable negative cycles

Any simple path has at most `V - 1` edges. Bellman-Ford relaxes every edge for up to `V - 1` passes. If a reachable edge can still improve afterward, a reachable negative cycle participates in arbitrarily cheaper walks.

There are three different states an API must not collapse:

```text
finite distance       source reaches vertex; no relevant negative cycle
INF                   source cannot reach vertex
undefined / affected  source reaches a negative cycle that can reach vertex
```

An unrelated negative cycle in a disconnected component does not invalidate this source's distances.

### Propagating undefined answers

The extra relaxation identifies vertices immediately improvable after `V - 1` passes. Those vertices are seeds. Traverse outward from every seed and mark all reachable descendants affected:

```text
source -> A -> B -> C -> D
             ^    |
             | -4 |
             +----+

B and C lie on a negative cycle; D is not on the cycle but its shortest
distance is also undefined because the cycle can be repeated before reaching D.
```

The companion returns both `distances` and `affectedByNegativeCycle[]`. Callers must inspect the affected flag before treating the corresponding numeric distance as meaningful.

Early stopping after a full pass with no change is safe: no later pass can discover an improvement without an earlier improvement feeding it.

## Floyd-Warshall: define infinity before adding

Floyd-Warshall gradually allows vertices as intermediates. At phase `k`:

> `distance[i][j]` is the best path from `i` to `j` whose intermediate vertices come only from the processed set.

Transition:

```text
distance[i][j] = min(
    distance[i][j],
    distance[i][k] + distance[k][j]
)
```

Never add an infinity sentinel as though it were a real distance. `Long.MAX_VALUE + positiveWeight` overflows negative and can look like an excellent path.

The companion uses:

```text
INFINITY = Long.MAX_VALUE / 4
```

It skips unreachable halves, rejects an edge magnitude that cannot be accumulated safely across a vertex-length simple path under this sentinel, and uses saturated finite addition at the supported numeric boundary. Parallel directed edges initialize the matrix with the minimum weight. A negative diagonal after processing indicates a negative cycle somewhere in the graph.

Floyd-Warshall's negative-cycle flag is global. To decide which `(source,target)` pairs are invalidated, additionally check whether the source can reach a negative-diagonal vertex and that vertex can reach the target.

## Prim: return the selected edges

An MST API returning only a weight is often too weak. Real callers may need the chosen links to build a network or explain a plan.

Prim maintains a cut:

```text
included vertices | excluded vertices
                  ^ frontier contains crossing edges
```

Choose the lightest frontier edge whose endpoints lie on opposite sides, add the excluded endpoint, and push its outgoing crossing edges. The cut property makes that chosen edge safe for an MST.

The companion returns:

- the original edge records, preserving edge IDs;
- total weight accumulated in `long`; and
- `spansAllVertices`, which is false when the start component cannot reach all vertices.

Negative weights are valid for MST. The nonnegative restriction belongs to Dijkstra. Self-loops are ignored by the crossing-edge test. Parallel edges are valid; the cheaper useful one can be selected.

For a disconnected graph, decide whether the contract wants failure, one component's tree, or a minimum spanning forest. The companion grows from one start and reports that it did not span rather than pretending it produced an MST.

## Strongly connected components with Kosaraju

Inside a strongly connected component (SCC), every vertex reaches every other. Contracting each SCC into one meta-vertex produces a DAG.

Kosaraju uses two traversals:

1. DFS the original graph and append a vertex after all outgoing work finishes.
2. Reverse every edge.
3. Visit vertices in decreasing original finish time; each reverse-graph traversal yields one SCC.

Why finish time matters: a source SCC in the original condensation DAG finishes after SCCs reachable from it. Reversing edges turns it into a sink boundary, so the second traversal cannot leak into an uncollected component.

```text
original SCC DAG:  {0,1} -> {2,3} -> {4}    {5}
reported groups:   [0,1],   [2,3],   [4],   [5]
```

The companion sorts members and components only to make tests and output deterministic; sorting is not part of Kosaraju's linear core. Its recursive teaching DFS can overflow on a pathological long chain. For untrusted production depth, convert both passes to explicit frame stacks.

## Bridges and articulation points: low-link meaning

For undirected DFS:

- `discovery[u]` is when `u` was first reached;
- `low[u]` is the earliest discovery reachable from `u`'s DFS subtree using tree edges and at most one back edge.

After returning from child `v`:

- edge `(u,v)` is a bridge when `low[v] > discovery[u]`;
- nonroot `u` is an articulation point when `low[v] >= discovery[u]`; and
- a DFS root is an articulation point only when it has more than one DFS child.

### Why parent edge ID matters

In a multigraph, two parallel edges may connect `u` and `v`:

```text
u ===== v
  edge 21
  edge 22
```

The DFS enters `v` through edge 21. It must skip that exact edge on the way back, but edge 22 is a legitimate back edge. Skipping every edge whose neighbor equals the parent would incorrectly label edge 21 a bridge.

The companion assigns and preserves edge IDs, carries `parentEdgeId`, and skips only that ID. It also rejects duplicate IDs because ambiguous identity would corrupt the result contract.

## Numeric and representation policy

Graph code should state these policies before the algorithm:

- vertices are dense integers in `[0,V)`;
- missing adjacency is represented by absence, not a magic weight;
- weight magnitude is bounded from `V` so a vertex-length simple path stays strictly between `-INFINITY` and `INFINITY`;
- distance arithmetic uses `long` and guarded addition;
- directed and undirected edge records are separate types;
- undirected edge IDs are unique when identity matters; and
- recursion is educational unless depth constraints make it safe.

External IDs should be mapped to dense indexes at the boundary. Do not allocate a billion-slot array merely because one customer ID is large.

## Edge-case matrix

| Case | Correct handling | Frequent failure |
|---|---|---|
| DAG contains a cycle | reject the precondition | process a partial order as complete |
| negative DAG edge | valid | unnecessarily rejecting it like Dijkstra |
| unreachable negative cycle | Bellman-Ford source result remains valid | reporting every graph cycle as source-relevant |
| descendant of reachable negative cycle | mark undefined too | marking only cycle vertices |
| unreachable Floyd pair | keep `INFINITY`; do not add it | overflow creates a fake negative path |
| multiple directed edges | initialize with minimum edge | last input edge wins arbitrarily |
| disconnected Prim input | return/report non-spanning result | calling a component tree an MST |
| negative MST edge | valid | importing Dijkstra's restriction |
| isolated SCC vertex | one singleton SCC | omitting vertices with no edges |
| undirected parallel edges | skip parent edge ID only | false bridge from parent-vertex skip |
| DFS root articulation | requires more than one DFS child | applying nonroot rule to root |
| self-loop | state semantics; not a bridge | treating all visited-neighbor edges identically |
| deep DFS chain | iterative frames or explicit limit | Java `StackOverflowError` |
| invalid endpoint | reject before indexing | accidental `ArrayIndexOutOfBoundsException` |

## Real interview follow-up round

**Interviewer:** Why can DAG shortest path handle a negative edge while Dijkstra cannot?

**Candidate:** Topological order guarantees every predecessor is processed before a vertex and no edge returns to a finalized prefix. Dijkstra instead relies on nonnegative weights to prove the smallest tentative distance cannot later decrease.

**Interviewer:** Bellman-Ford found a negative cycle in the graph. Are all source distances invalid?

**Candidate:** Only if the cycle is reachable from this source, and only vertices reachable from that cycle have undefined shortest distance. Unreachable vertices remain unreachable, while reachable vertices outside the cycle's downstream region can retain finite answers.

**Interviewer:** Why not use `Long.MAX_VALUE` as infinity?

**Candidate:** Adding any positive weight overflows. I use a separated sentinel, skip unreachable additions, constrain supported weights, and guard finite addition. The numeric policy is part of the API, not an implementation afterthought.

**Interviewer:** Prim and Dijkstra both use a priority queue. What differs?

**Candidate:** Dijkstra prioritizes total source-to-vertex distance and builds shortest paths. Prim prioritizes the weight of a crossing edge and minimizes total tree weight. Their queue syntax looks similar, but their invariants and outputs are different.

**Interviewer:** Why does the bridge condition use `>` but articulation uses `>=`?

**Candidate:** If a child's subtree can reach the parent itself, `low[child] == discovery[parent]`, the tree edge has an alternate route and is not a bridge. But removing the parent still separates that child subtree from the parent's ancestors, so the nonroot parent is an articulation point under equality.

**Interviewer:** How would you validate shortest-path implementations?

**Candidate:** On random DAGs, I can compare topological relaxation and Bellman-Ford from every source with Floyd-Warshall's corresponding row, including negative edges but no cycles. I also need directed fixed cases for reachable and unreachable negative cycles, plus infinity boundaries. The companion runs that differential test with a deterministic seed.

## Run the verified companion

```bash
javac -Xlint:all -Werror GraphInterviewChecks.java
java GraphInterviewChecks
```

Expected final line:

```text
PASS 16 graph checks
```

For an interview, implement only the algorithm the contract needs, but be ready to defend why its invariant applies. A shorter correct choice beats a memorized advanced algorithm attached to the wrong graph.
