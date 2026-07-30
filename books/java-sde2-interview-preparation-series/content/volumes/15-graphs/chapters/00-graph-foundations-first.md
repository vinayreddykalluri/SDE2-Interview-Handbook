# Graph Foundations: Model the Relationships Before Traversal

A graph contains vertices and edges. The hardest early step is often deciding what each represents.

- Social network: person is a vertex, friendship is an undirected edge.
- Build system: task is a vertex, prerequisite is a directed edge.
- Road network: location is a vertex, road is a weighted edge.
- Grid: valid cell is a vertex, allowed move is an implicit edge.

## Vocabulary that changes the algorithm

| Property | Meaning | Consequence |
|---|---|---|
| Directed | `u -> v` need not imply `v -> u` | cycle and reachability logic changes |
| Undirected | edge connects both ways | parent edge must not look like a cycle |
| Weighted | edges carry cost | BFS is insufficient for arbitrary weights |
| Connected | every vertex reaches every other in undirected graph | otherwise loop over components |
| DAG | directed and acyclic | topological order exists |

A path is a sequence of adjacent vertices. A cycle returns to an earlier vertex. A component is a maximal connected region under the relevant direction semantics.

## Adjacency-list representation

For vertices numbered `0..n-1`:

```java
static List<List<Integer>> undirectedGraph(int vertices, int[][] edges) {
    List<List<Integer>> graph = new ArrayList<>(vertices);
    for (int i = 0; i < vertices; i++) graph.add(new ArrayList<>());
    for (int[] edge : edges) {
        int from = edge[0];
        int to = edge[1];
        graph.get(from).add(to);
        graph.get(to).add(from);
    }
    return graph;
}
```

An adjacency list uses O(V + E) space for a directed graph and stores two adjacency entries per undirected edge. An adjacency matrix uses O(V squared) space and provides O(1) edge-existence checks.

Validate vertex ranges and clarify parallel edges and self-loops. Those details affect indegrees, cycle rules, and costs.

## BFS from zero

Breadth-first search explores vertices in increasing unweighted edge distance from a source.

```java
static int[] distances(List<List<Integer>> graph, int source) {
    int[] distance = new int[graph.size()];
    Arrays.fill(distance, -1);
    Deque<Integer> queue = new ArrayDeque<>();
    distance[source] = 0;
    queue.addLast(source);
    while (!queue.isEmpty()) {
        int node = queue.removeFirst();
        for (int neighbor : graph.get(node)) {
            if (distance[neighbor] == -1) {
                distance[neighbor] = distance[node] + 1;
                queue.addLast(neighbor);
            }
        }
    }
    return distance;
}
```

Setting distance when enqueuing also marks visited. Each vertex enters the queue once. For unweighted graphs, the first discovery is along a shortest path.

## DFS from zero

Depth-first search completes one branch before trying the next. It supports reachability, components, cycle state, entry/exit timing, and postorder.

```java
static void dfs(List<List<Integer>> graph, int node, boolean[] visited) {
    visited[node] = true;
    for (int neighbor : graph.get(node)) {
        if (!visited[neighbor]) dfs(graph, neighbor, visited);
    }
}
```

Recursive depth can reach O(V), unsafe for a deep graph in Java. An explicit `ArrayDeque` avoids call-stack failure.

## Disconnected graphs

One traversal from vertex 0 sees only its reachable component. To count components:

```java
for each vertex:
    if unseen:
        components++
        traverse from it
```

This outer loop is not extra O(VE) work. Across all traversals, each vertex and adjacency entry is processed a bounded number of times: O(V + E).

## Grid as an implicit graph

Do not allocate adjacency lists for a grid unless needed. Generate up/down/left/right neighbors from coordinates. Decide whether diagonal movement, wrapping, blocked cells, and jagged rows are valid.

Use a direction table:

```java
int[][] directions = {{1, 0}, {-1, 0}, {0, 1}, {0, -1}};
```

Mark before enqueue/recurse to prevent duplicate work. Mutating the grid as visited saves a separate array only when the input-mutation contract allows it.

## Choose shortest-path logic from weights

| Edge weights | Typical algorithm |
|---|---|
| all equal/unweighted | BFS |
| only 0 and 1 | 0-1 BFS with deque |
| nonnegative | Dijkstra with min-heap |
| negative edges, no reachable negative cycle | Bellman-Ford |
| all-pairs, dense/moderate V | Floyd-Warshall |
| DAG | topological relaxation |

Dijkstra is not correct with negative edges. A visited set alone is not enough to choose an algorithm; weight constraints drive the choice.

## Topological ordering

In a directed graph, an edge `u -> v` can mean `u` must happen before `v`. Kahn's algorithm queues all zero-indegree vertices, removes them, and decrements neighbors. If fewer than V vertices are emitted, a directed cycle prevents a complete order.

## Union-Find boundary

Disjoint-set union maintains evolving undirected connectivity under edge additions. It does not provide paths and does not directly handle deletions. Path compression and union by rank/size give near-constant amortized operations.

## Complexity language

For adjacency lists, traversal is O(V + E), not simply O(n). Memory includes the graph representation plus O(V) visited/frontier state. For a grid with rows and columns, V is valid cells and E is proportional to allowed neighbor relationships.

## Foundation checkpoint

1. What are vertices and edges in the problem domain?
2. Is the graph directed, weighted, disconnected, or implicit?
3. Why should BFS mark at enqueue time?
4. Why does one DFS not necessarily visit every vertex?
5. Which weight contract makes Dijkstra valid?
