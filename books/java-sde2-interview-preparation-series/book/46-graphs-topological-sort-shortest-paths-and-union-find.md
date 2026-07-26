# 46. Graphs, Topological Sort, Shortest Paths, and Union-Find

## Learning objectives

By the end of this chapter, you should be able to:

- model directed, undirected, weighted, and state-space graphs explicitly;
- implement BFS and DFS with correct visited timing and disconnected coverage;
- detect cycles and produce a topological order for a directed acyclic graph;
- choose a shortest-path algorithm from edge-weight constraints;
- implement Dijkstra with stale-entry handling and safe distance types; and
- use union-find with path compression and union by size for incremental connectivity.

## Why this matters at SDE-2

Many backend problems are graphs even when no vertex is named: service dependencies, build order, account relationships, workflows, routes, permissions, and state transitions. The hard part is often the model. Is an edge directed? Can there be parallel edges? Is cost uniform? Is the state only a location, or location plus remaining budget?

At SDE-2, "use BFS" is incomplete. You must state why BFS order proves minimum distance, when a node becomes visited, what happens in disconnected components, and which resource bounds make the representation viable. Weighted paths and dependency cycles are common follow-ups.

## First-principles model

A graph is a set of vertices and edges. An edge can be directed or undirected and may carry weight or metadata. Traversal maintains a frontier of discovered but unfinished states. BFS and DFS differ mainly in frontier policy: FIFO versus LIFO.

The visited state records what future work is redundant. Sometimes it is one boolean per vertex. Sometimes the true vertex is a compound state such as `(cell, keysHeld)`, `(service, retriesUsed)`, or `(airport, stops)`. Marking only the visible location would merge states that have different legal futures.

A topological order linearizes precedence constraints. It exists exactly when a directed graph is acyclic. A shortest-path algorithm repeatedly establishes that certain distances cannot later improve, but the proof depends on edge weights. Union-find answers a different question: whether undirected elements are already in the same connected component as edges are added.

> **Specification boundary:** Java collections supply list, deque, map, and priority-queue behavior; they do not define a graph abstraction or algorithm. `PriorityQueue` does not guarantee stable tie order or sorted iteration, so graph correctness must rely only on head priority.

## Core terminology

- **Vertex/edge:** graph entity and relationship.
- **Directed/undirected:** edge has one direction or is traversable both ways.
- **Weighted/unweighted:** edge costs differ or each edge counts equally.
- **Adjacency list:** outgoing neighbors stored per vertex.
- **Adjacency matrix:** V by V edge table.
- **Frontier:** discovered states waiting to be expanded.
- **Visited/finalized:** discovered state or state whose optimal result is proven, depending on algorithm.
- **DAG:** directed acyclic graph.
- **Indegree:** number of incoming directed edges.
- **Relaxation:** improve a tentative distance through an edge.
- **Connected component:** maximal mutually connected set in an undirected graph.
- **Disjoint-set union (DSU):** union-find representation of evolving components.

## Detailed mechanics

### Model before traversal

Ask these questions first:

1. What uniquely identifies a vertex?
2. Is each relationship directed, undirected, or asymmetric in cost?
3. Are self-loops or parallel edges legal?
4. Are vertices dense integers or arbitrary domain keys?
5. Is the graph static, streaming, or generated on demand?
6. What state affects future legal moves?
7. Is the output reachability, a path, cost, ordering, components, or all solutions?

For dense vertex IDs `0..V-1`, `List<List<Edge>>` or primitive arrays are convenient. Arbitrary keys may use a map from key to integer ID, retaining a reverse list for output. This often improves memory and makes `boolean[]`, `int[]`, and DSU possible.

Representation trade-offs:

| Representation | Space | Edge lookup | Neighbor iteration |
|---|---:|---:|---:|
| Adjacency list | O(V + E) | O(outdegree) typical | O(outdegree) |
| Adjacency matrix | O(V^2) | O(1) | O(V) |
| Edge list | O(E) | O(E) | O(E) unless indexed |
| Generated neighbors | State-dependent | Computed | No stored edge graph |

An undirected edge must normally be added in both adjacency directions. Parallel edges can affect indegree, shortest paths, and output; deduplicate only when semantics allow it.

### BFS

BFS for an unweighted graph:

```text
mark source visited and enqueue it
while queue not empty:
    remove front vertex
    for every neighbor:
        if unseen:
            mark visited
            record distance/parent
            enqueue
```

Mark on enqueue. If marking waits until dequeue, several parents can enqueue the same vertex, increasing memory and work. FIFO order processes all distance-d vertices before distance d + 1, so the first discovery gives a minimum number of edges from the source.

Store `parent[neighbor] = current` to reconstruct a path. Distance alone cannot recover the route. For multi-source BFS, enqueue all sources with distance zero before traversal; it computes distance to the nearest source.

Grid problems are implicit graphs. Bounds, blocked cells, movement directions, and whether diagonal moves exist define the edges. Mutating a grid as visited can save space only when input ownership permits it.

### DFS and cycle state

DFS explores one path deeply using recursion or an explicit stack. It is useful for components, reachability, postorder, backtracking, and cycle detection. A single visited boolean detects redundant exploration but not every directed cycle. Use three colors or two booleans:

- unseen;
- active on the current recursion path; and
- complete.

An edge to an active vertex is a directed back edge and proves a cycle. An edge to a complete vertex does not. In an undirected graph, the immediate parent edge is expected and must be excluded; an edge to another visited vertex proves a cycle.

For a disconnected graph, wrap traversal in an outer loop over all vertices. Starting once answers only the source component.

Recursive DFS can overflow the Java stack on a long chain. An iterative postorder needs a frame phase or a `(vertex, exiting)` marker; simply pushing neighbors does not automatically reproduce recursive finish order.

### Topological sort

Kahn's algorithm uses indegrees:

1. count each vertex's incoming edges;
2. enqueue all zero-indegree vertices;
3. remove one, append it to the order, and decrement its outgoing neighbors;
4. enqueue a neighbor when its indegree reaches zero; and
5. if fewer than V vertices are emitted, a directed cycle exists.

The invariant is that emitted vertices have all prerequisites emitted, and current indegree counts incoming edges from the un-emitted subgraph. Removing a zero-indegree vertex cannot violate precedence.

Topological orders are generally not unique. If lexicographically smallest order is required, use a priority queue for zero-indegree choices, changing complexity to O((V + E) log V) in the broad bound. If all orders are required, the output may be exponential.

DFS finish-time reversal also yields a topological order if cycle detection succeeds. Kahn's algorithm often makes cycle detection and scheduling layers easier to explain.

### Shortest-path decision table

| Edge model | Algorithm | Typical complexity |
|---|---|---:|
| Unweighted/equal weight | BFS | O(V + E) |
| Weights 0 or 1 | 0-1 BFS with deque | O(V + E) |
| Nonnegative weights | Dijkstra with binary heap | O((V + E) log V) in the standard simple-graph model |
| Negative edges, no reachable negative cycle | Bellman-Ford | O(VE) |
| DAG, any weights | Topological relaxation | O(V + E) |
| All pairs, dense/small | Floyd-Warshall | O(V^3), O(V^2) space |

Clarify whether a negative cycle reachable from the source makes the shortest value undefined because cost can decrease without bound.

### Dijkstra's algorithm

Dijkstra maintains tentative distances and repeatedly expands the vertex with minimum tentative distance. With nonnegative edges, when a minimum-distance entry is current, no later route through unprocessed vertices can produce a smaller value: any extension adds a nonnegative amount.

Java's `PriorityQueue` has no efficient decrease-key operation. Insert a new state whenever a distance improves. Old entries become stale. When polling `(vertex, distance)`, skip it if `distance != distances[vertex]`. Each successful relaxation can add an entry, producing O(E) queue entries and O((V + E) log E) time in the broad multigraph analysis. For a simple graph, `E <= V^2`, so this is conventionally written O((V + E) log V).

Use `long` distances. Do not add to a sentinel `Long.MAX_VALUE`; only expand finite queued states. Reject negative weights before relying on the proof. If a predecessor is needed, update it in the same successful relaxation.

Dijkstra does not work with a negative edge because a vertex considered final can later improve through that edge. This is a proof failure, not merely a library limitation.

### Union-find

DSU stores each element's parent; roots represent components. `find(x)` follows parents to a root. `union(a,b)` connects roots. Two optimizations make sequences nearly constant time:

- **path compression:** make nodes visited by `find` point closer or directly to the root;
- **union by size/rank:** attach the smaller or shallower tree beneath the larger.

Over m operations on n elements, the cost is O(m alpha(n)), where alpha is the inverse Ackermann function and is tiny for practical n. This is an amortized bound.

DSU is excellent for incremental undirected connectivity, redundant-edge detection, Kruskal's minimum spanning tree, and grouping equivalences. It does not naturally support deletions, shortest paths, directed reachability, or listing a route.

The invariant is that every element reaches one root, and two elements have the same root exactly when processed unions connect them.

## Worked Java example

This Java 21 class includes Kahn topological sort, Dijkstra, and an array-backed DSU.

```java
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.Deque;
import java.util.List;
import java.util.PriorityQueue;

public final class GraphAlgorithms {
    record Edge(int to, int weight) {}
    record State(int vertex, long distance) {}

    static List<Integer> topologicalOrder(int vertices, int[][] edges) {
        List<List<Integer>> graph = new ArrayList<>();
        for (int i = 0; i < vertices; i++) graph.add(new ArrayList<>());
        int[] indegree = new int[vertices];
        for (int[] edge : edges) {
            int from = edge[0];
            int to = edge[1];
            graph.get(from).add(to);
            indegree[to]++;
        }

        Deque<Integer> ready = new ArrayDeque<>();
        for (int v = 0; v < vertices; v++) {
            if (indegree[v] == 0) ready.addLast(v);
        }

        List<Integer> order = new ArrayList<>(vertices);
        while (!ready.isEmpty()) {
            int current = ready.removeFirst();
            order.add(current);
            for (int next : graph.get(current)) {
                if (--indegree[next] == 0) ready.addLast(next);
            }
        }
        return order.size() == vertices ? List.copyOf(order) : List.of();
    }
```

Dijkstra's algorithm continues inside the same `GraphAlgorithms` class:

```java

    static long[] dijkstra(List<List<Edge>> graph, int source) {
        long[] distance = new long[graph.size()];
        Arrays.fill(distance, Long.MAX_VALUE);
        distance[source] = 0;

        PriorityQueue<State> frontier = new PriorityQueue<>(
                Comparator.comparingLong(State::distance));
        frontier.add(new State(source, 0));

        while (!frontier.isEmpty()) {
            State current = frontier.remove();
            if (current.distance() != distance[current.vertex()]) continue;

            for (Edge edge : graph.get(current.vertex())) {
                if (edge.weight() < 0) {
                    throw new IllegalArgumentException("negative edge");
                }
                long candidate = current.distance() + edge.weight();
                if (candidate < distance[edge.to()]) {
                    distance[edge.to()] = candidate;
                    frontier.add(new State(edge.to(), candidate));
                }
            }
        }
        return distance;
    }
```

The disjoint-set implementation is the next member of `GraphAlgorithms`:

```java

    static final class DisjointSet {
        private final int[] parent;
        private final int[] size;

        DisjointSet(int count) {
            parent = new int[count];
            size = new int[count];
            for (int i = 0; i < count; i++) {
                parent[i] = i;
                size[i] = 1;
            }
        }

        int find(int value) {
            int root = value;
            while (root != parent[root]) root = parent[root];
            while (value != root) {
                int next = parent[value];
                parent[value] = root;
                value = next;
            }
            return root;
        }

        boolean union(int left, int right) {
            int rootLeft = find(left);
            int rootRight = find(right);
            if (rootLeft == rootRight) return false;
            if (size[rootLeft] < size[rootRight]) {
                int temporary = rootLeft;
                rootLeft = rootRight;
                rootRight = temporary;
            }
            parent[rootRight] = rootLeft;
            size[rootLeft] += size[rootRight];
            return true;
        }
    }
```

The entry point completes the class and demonstrates each algorithm:

```java

    public static void main(String[] args) {
        System.out.println(topologicalOrder(4,
                new int[][] {{0, 1}, {0, 2}, {1, 3}, {2, 3}}));

        List<List<Edge>> graph = List.of(
                List.of(new Edge(1, 4), new Edge(2, 1)),
                List.of(new Edge(3, 1)),
                List.of(new Edge(1, 2), new Edge(3, 5)),
                List.of());
        System.out.println(Arrays.toString(dijkstra(graph, 0))); // [0,3,1,4]

        DisjointSet sets = new DisjointSet(4);
        sets.union(0, 1);
        sets.union(2, 3);
        System.out.println(sets.find(0) == sets.find(2)); // false
        sets.union(1, 2);
        System.out.println(sets.find(0) == sets.find(3)); // true
    }
}
```

The topological method returns an empty list for a cycle. That API is compact but ambiguous when the graph itself has zero vertices; a production API could return a record containing both status and order.

## Execution or memory walkthrough

For topological edges `0->1`, `0->2`, `1->3`, `2->3`, initial indegrees are `[0,1,1,2]`. Queue contains 0. Emitting 0 reduces 1 and 2 to zero and enqueues them. Emitting 1 reduces 3 to one; emitting 2 reduces it to zero; then 3 is emitted. Depending on neighbor and queue order, another valid DAG can have several correct outputs.

For Dijkstra, source 0 begins at distance 0. Expanding 0 proposes distance 4 to vertex 1 and 1 to vertex 2. Vertex 2 is next; it improves vertex 1 to 3 and proposes 6 to vertex 3. Vertex 1 at distance 3 improves vertex 3 to 4. The old queue states `(1,4)` and `(3,6)` later fail the equality check and are skipped. Final distances are `[0,3,1,4]`.

For DSU, unions create components `{0,1}` and `{2,3}`. Union of 1 and 2 attaches the smaller/equal root under the chosen larger root. Later finds compress traversed paths, so all four elements reach one root.

## Complexity and performance

| Algorithm | Time | Auxiliary space |
|---|---:|---:|
| BFS/DFS adjacency list | O(V + E) | O(V) |
| Kahn topological sort | O(V + E) | O(V) plus graph |
| Lazy Dijkstra, binary heap | O((V + E) log E), or O((V + E) log V) for simple graphs | O(V + E) queue worst case |
| Bellman-Ford | O(VE) | O(V) |
| DAG shortest paths | O(V + E) | O(V) |
| Floyd-Warshall | O(V^3) | O(V^2) |
| DSU operation sequence | O(m alpha(n)) | O(n) |

Building an adjacency list is itself O(V + E) time and space. If the caller already supplies one, say whether representation construction is included. BFS path output also costs O(path length).

The worked Dijkstra can hold stale entries, so queue memory may exceed V. An indexed heap with decrease-key can bound entries more tightly but is harder to implement correctly. For interviews, lazy stale-entry skipping is usually the right trade-off.

> **HotSpot note:** Object-heavy adjacency lists and record queue states create allocation and indirection that primitive graph libraries can reduce. HotSpot may optimize some short-lived objects, but no elimination is guaranteed; model large production graphs with measured bytes per vertex and edge.

## Edge cases and common mistakes

- Reversing a directed edge or adding only one direction for an undirected graph.
- Ignoring self-loops, duplicate edges, or vertices with no edges.
- Starting one traversal and missing disconnected components.
- Marking BFS visited on dequeue and enqueuing duplicates.
- Using one visited boolean for directed cycle detection instead of active/complete state.
- Treating the parent edge as an undirected cycle.
- Returning a partial topological order as if it were valid.
- Assuming a topological order is unique.
- Using BFS with arbitrary positive weights.
- Running Dijkstra with a negative edge.
- Omitting stale-entry checks from lazy Dijkstra.
- Storing distance in `int` and overflowing path sums.
- Confusing an unreachable sentinel with a very large reachable distance.
- Using DSU for directed reachability or shortest paths.
- Forgetting that DSU does not support arbitrary edge deletion naturally.
- Modeling visited only by location when budget or mode changes future transitions.

## Production engineering notes

Graph data is often larger than heap-friendly interview examples. Map external IDs to dense integers, choose primitive storage when memory dominates, stream edge lists when possible, and cap path/output materialization. Avoid recursively traversing untrusted dependency chains.

Dependency scheduling needs more than a topological order: failed prerequisites, optional edges, concurrency limits, retries, and versioned graph snapshots matter. Detect cycles with diagnostic paths, not only a boolean, so operators can repair configuration.

Routing weights can change while a path is computed. Define snapshot consistency and whether stale results are acceptable. For currency, latency, or risk, validate nonnegativity and overflow. Dijkstra's mathematical model does not include network calls made while expanding neighbors; cache or batch appropriately.

DSU is useful in offline batch grouping and incremental connectivity, but concurrent mutation requires synchronization or partitioning. Parent-array updates are not thread-safe merely because each entry is an `int`.

## Interview questions and model answers

**Adjacency list or matrix?**

Use a list for sparse graphs: O(V + E) space and efficient neighbor traversal. Use a matrix when the graph is dense or constant-time arbitrary edge lookup dominates and O(V squared) memory is acceptable.

**Why does BFS find a shortest unweighted path?**

FIFO order expands vertices by nondecreasing edge distance. Every neighbor first discovered from a distance-d vertex has distance d + 1, and no later discovery can use fewer edges.

**How does directed cycle detection differ from undirected?**

Directed DFS needs active-path state; an edge to active is a back edge. Undirected DFS ignores the edge to the immediate parent and treats another visited neighbor as a cycle. Kahn's emitted count is another directed-cycle test.

**Why does Dijkstra require nonnegative weights?**

Its finalization proof assumes extending any path cannot reduce cost. A negative edge can create a later cheaper route to a vertex already removed as minimum, invalidating the greedy choice.

**Why are duplicate priority-queue entries acceptable?**

Java's queue lacks decrease-key. Inserting improved states preserves correctness if a polled state is processed only when its distance equals the current best. Stale entries increase constants and memory but keep the implementation simple.

**When should union-find be chosen?**

For repeated undirected connectivity and component merging as edges are added, or for Kruskal's cycle checks. It is not a traversal, cannot provide shortest paths, and does not naturally handle deletions.

## Exercises

1. Implement BFS path reconstruction and distinguish unreachable from source-to-source paths.
2. Detect a directed cycle and return one concrete cycle, not only an empty topological order.
3. Produce lexicographically smallest topological order and update the complexity.
4. Implement 0-1 BFS and prove why deque front/back placement preserves distance order.
5. Extend Dijkstra to return predecessors and reconstruct the path; guard distance overflow.
6. Implement Bellman-Ford with reachable negative-cycle detection.
7. Use DSU to find the first redundant undirected edge and report component sizes.
8. Model a grid where state includes remaining obstacle eliminations; explain why cell-only visited state is wrong.

## Chapter summary

Graph algorithms begin with the correct vertex and edge model. BFS and DFS are frontier policies with different order guarantees; visited state must represent the full future-relevant state. Kahn's algorithm removes zero-indegree vertices and detects directed cycles by emitted count. Shortest-path choice follows weight constraints: BFS, 0-1 BFS, Dijkstra, Bellman-Ford, or DAG relaxation. Union-find efficiently maintains incremental undirected components through path compression and union by size.

## Revision checklist

- [ ] I define direction, weights, identity, duplicates, and state before traversal.
- [ ] I choose an adjacency representation from V, E, and query needs.
- [ ] I mark BFS states when enqueuing and can reconstruct paths.
- [ ] I cover disconnected graphs with an outer loop when required.
- [ ] I use active/complete state for directed DFS cycles.
- [ ] I can prove Kahn's indegree invariant and detect incomplete output.
- [ ] I select shortest-path algorithms from weight constraints.
- [ ] I implement Dijkstra with `long` distances and stale-entry skipping.
- [ ] I know DSU's invariant, amortized bound, and limitations.
- [ ] I include representation construction, output, and memory in complexity.
