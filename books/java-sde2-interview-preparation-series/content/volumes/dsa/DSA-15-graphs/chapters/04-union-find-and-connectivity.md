# 4. Union-Find and Incremental Connectivity

## Why this chapter exists

The foundations chapter named Union-Find and deferred it. This chapter develops it, because it is the one graph structure that answers a question BFS and DFS cannot answer efficiently: **connectivity that changes over time**.

BFS answers "are `a` and `b` connected?" in O(V + E) per query. If edges arrive one at a time and you must answer after each arrival, that becomes O(Q * (V + E)) - quadratic and usually too slow. Union-Find answers each query in effectively constant time, and the reason it can is worth understanding rather than memorizing.

It also carries a specific interview signal. Candidates who reach for Union-Find on "number of provinces" when a DFS would do are pattern-matching rather than reasoning; candidates who cannot reach for it on "redundant connection" or "accounts merge" are missing a tool. Knowing which problems genuinely need it is the point.

## First-principles model

Union-Find maintains a **partition of elements into disjoint sets** under two operations:

```text
find(x)      -> the representative of x's set
union(a, b)  -> merge the two sets containing a and b
```

Connectivity is then `find(a) == find(b)`. That is the whole interface.

The representation is a forest. Each element points at a parent; a root points at itself and is the set's representative. `find` walks to the root. `union` attaches one root to the other.

```text
initial:   0   1   2   3   4        every element its own root

union(0,1): 0       2   3   4
            |
            1

union(2,3):     0       2   4
                |       |
                1       3

union(1,3): find(1)=0, find(3)=2, attach 2 under 0

                0
               / \
              1   2
                  |
                  3
```

Naively this degenerates. Attaching roots arbitrarily can build a chain of length `n`, making `find` O(n) and the whole structure no better than a linked list. Two independent optimizations fix that, and both are needed to reach the stated bound.

> **Specification boundary:** the near-constant bound is *amortized*, not worst case. A single `find` can still be O(log n) before compression flattens the path. Union-Find also supports no efficient *undo* - it is an incremental structure, and removing an edge requires either rebuilding or a substantially more complex design. If a prompt deletes edges, Union-Find is the wrong tool and saying so is the correct answer.

## The two optimizations

**Union by size or rank.** Always attach the smaller tree under the larger root. This alone bounds depth at O(log n), because an element's depth only increases when its tree is merged into one at least as large, which can happen at most log n times.

**Path compression.** During `find`, repoint every node on the path directly at the root. Subsequent finds on those nodes are O(1).

![Path compression repoints every node on the find path directly at the root](content/volumes/dsa/DSA-15-graphs/assets/01-union-find-path-compression.png)

Applied together, the amortized cost per operation is **O(alpha(n))**, where alpha is the inverse Ackermann function. For every `n` that fits in the observable universe, alpha(n) < 5. It is constant in practice and it is worth saying "effectively constant, formally inverse Ackermann" rather than claiming O(1) outright - interviewers notice the difference.

Either optimization alone gives O(log n). Both give O(alpha(n)). Knowing that they are independent, and that the bound requires both, is a standard follow-up.

## Implementation

```java
public final class UnionFind {
    private final int[] parent;
    private final int[] size;
    private int componentCount;

    public UnionFind(int elements) {
        if (elements < 0) {
            throw new IllegalArgumentException("element count must be nonnegative");
        }
        parent = new int[elements];
        size = new int[elements];
        for (int i = 0; i < elements; i++) {
            parent[i] = i;          // every element starts as its own root
            size[i] = 1;
        }
        componentCount = elements;
    }

    /** Representative of x's set, compressing the path on the way out. */
    public int find(int x) {
        int root = x;
        while (parent[root] != root) {
            root = parent[root];
        }
        while (parent[x] != root) {  // second pass repoints the whole path
            int next = parent[x];
            parent[x] = root;
            x = next;
        }
        return root;
    }

    /** Merge two sets. Returns false when they were already joined. */
    public boolean union(int a, int b) {
        int rootA = find(a);
        int rootB = find(b);
        if (rootA == rootB) {
            return false;            // the edge is redundant
        }
        if (size[rootA] < size[rootB]) {
            int swap = rootA;
            rootA = rootB;
            rootB = swap;            // rootA is now the larger tree
        }
        parent[rootB] = rootA;
        size[rootA] += size[rootB];
        componentCount--;
        return true;
    }

    public boolean connected(int a, int b) {
        return find(a) == find(b);
    }

    public int componentCount() {
        return componentCount;
    }

    /** Size of the component containing x. */
    public int componentSize(int x) {
        return size[find(x)];
    }
}
```

Three details carry most of the value:

**`union` returns a boolean.** Whether the two elements were *already* connected is the answer to several problems by itself - redundant connection, cycle detection, and Kruskal's edge-acceptance test all hinge on it. Returning `void` throws that information away.

**The iterative two-pass `find`.** The recursive one-liner is shorter but recurses to the depth of the tree, which can be O(n) before the first compression and will overflow the stack on a large adversarial input. The iterative version has no such failure mode.

**`size` is only meaningful at a root.** After a union, `size[rootB]` is stale. `componentSize` therefore calls `find` first. Reading `size[x]` directly for a non-root is a silent wrong answer, and it is a common bug.

## What it solves, and what it does not

| Problem | Union-Find? | Why |
|---|---|---|
| Count connected components, static graph | Either | DFS is simpler; use it unless edges stream in |
| Connectivity queries interleaved with edge additions | **Yes** | The case DFS cannot do efficiently |
| Redundant connection - find the edge closing a cycle | **Yes** | `union` returning false names the edge |
| Kruskal's minimum spanning tree | **Yes** | Accept an edge exactly when it joins two components |
| Accounts merge / entity resolution | **Yes** | Repeated merging by shared keys |
| Number of islands, single pass | Either | DFS or BFS is usually clearer |
| Number of islands II - islands appear one at a time | **Yes** | Incremental, which is the whole point |
| Detect a cycle in a **directed** graph | **No** | Union-Find has no notion of direction; use DFS colours or Kahn |
| Shortest path | **No** | It knows connectivity, not distance |
| Anything that removes edges | **No** | Not efficiently undoable |

The directed-cycle row is the one interviewers probe. Union-Find treats every edge as symmetric, so it cannot distinguish `a -> b` from `b -> a` and cannot detect a directed cycle. Reaching for it there is a correctness error, not a performance one.

## Kruskal's minimum spanning tree

Union-Find makes Kruskal almost trivial, which is why the two are usually taught together.

```java
record Edge(int from, int to, int weight) {}

static List<Edge> minimumSpanningTree(int vertices, List<Edge> edges) {
    List<Edge> sorted = new ArrayList<>(edges);
    sorted.sort(Comparator.comparingInt(Edge::weight));

    UnionFind sets = new UnionFind(vertices);
    List<Edge> chosen = new ArrayList<>();
    for (Edge edge : sorted) {
        if (sets.union(edge.from(), edge.to())) {   // joins two components
            chosen.add(edge);
            if (chosen.size() == vertices - 1) {
                break;                              // a tree is complete
            }
        }
    }
    return chosen;
}
```

The correctness argument is the cut property: the lightest edge crossing any cut belongs to some minimum spanning tree. Processing edges in weight order and accepting exactly those that join two components realizes that greedily.

Complexity is O(E log E) for the sort, which dominates the O(E * alpha(V)) of the union operations. **The sort is the bottleneck, not the Union-Find** - worth stating, because candidates often quote the alpha term as though it mattered.

If the graph is disconnected, `chosen` ends with fewer than `V - 1` edges and you have a spanning *forest*. Whether that is an answer or an error is a requirement to clarify, not to assume.

### Kruskal versus Prim

| | Kruskal | Prim |
|---|---|---|
| Structure | Union-Find over sorted edges | Priority queue over frontier |
| Complexity | O(E log E) | O(E log V) with a binary heap |
| Best when | Sparse graphs, edges already sorted | Dense graphs, adjacency matrix |
| Produces | Forest on a disconnected graph | Tree of one component only |

The disconnected-graph difference is the practical one. Prim explores from a start vertex and silently returns a tree covering only that component; Kruskal covers everything it can. If the prompt does not promise connectivity, that distinction decides which is correct.

## Worked example: redundant connection

*Given a graph that began as a tree and had exactly one extra edge added, return that edge.*

The extra edge is precisely the first one whose endpoints are already connected.

```java
static int[] findRedundantConnection(int[][] edges) {
    UnionFind sets = new UnionFind(edges.length + 1);   // 1-indexed vertices
    for (int[] edge : edges) {
        if (!sets.union(edge[0], edge[1])) {
            return edge;                                 // already connected
        }
    }
    throw new IllegalArgumentException("no redundant edge; input was a tree");
}
```

The entire solution is the boolean that `union` returns. This is why the interface choice earlier mattered: with a `void` union you would need a separate `connected` call before every merge, which is the same work written less clearly.

Note the `edges.length + 1` sizing. The problem states vertices are labelled `1..n` and a tree with one extra edge has exactly `n` edges, so `n` vertices need indices `0..n` when 1-indexed. Off-by-one here produces an `ArrayIndexOutOfBoundsException` on the last vertex, and it is the most common bug in this problem.

## Edge cases and common mistakes

- Using Union-Find to detect a cycle in a **directed** graph. It cannot; it has no direction.
- Attempting edge deletion. The structure is incremental and not efficiently undoable.
- Omitting union by size, so a chain forms and `find` degrades to O(n).
- Omitting path compression, leaving O(log n) rather than O(alpha(n)).
- Recursive `find` on a deep tree, overflowing the stack before compression can help.
- Reading `size[x]` without calling `find(x)` first; the value is only current at a root.
- Returning `void` from `union`, discarding the already-connected answer several problems depend on.
- Claiming O(1) rather than amortized O(alpha(n)). The distinction is small and interviewers notice.
- Off-by-one sizing for 1-indexed vertex labels.
- Quoting alpha(n) as Kruskal's bottleneck when the edge sort dominates at O(E log E).
- Assuming Kruskal returns a spanning tree on a disconnected graph. It returns a forest.
- Using Union-Find on a static connectivity count where a single DFS pass is simpler and equally fast.

## Interview questions and model answers

**What does Union-Find give you that BFS does not?**

Efficient connectivity under *incremental* edge additions. BFS answers a connectivity query in O(V + E), so interleaving Q queries with edge insertions costs O(Q * (V + E)). Union-Find answers each in amortized O(alpha(n)). On a static graph where you just need components once, DFS is simpler and I would use it.

**Why is the complexity alpha(n) and what are the two optimizations?**

Union by size keeps trees shallow by attaching the smaller under the larger, giving O(log n) alone. Path compression flattens the path to the root during `find`, also giving O(log n) alone. Together they give amortized O(alpha(n)), inverse Ackermann, which is under 5 for any practical n. Both are required for the combined bound.

**Can you detect a cycle in a directed graph with Union-Find?**

No. It models undirected connectivity and has no notion of edge direction, so it cannot distinguish `a -> b` from `b -> a`. For directed cycles use DFS with white/grey/black colouring, or Kahn's algorithm and check whether the topological order covers every vertex.

**Implement Kruskal and state the bottleneck.**

Sort edges by weight, then accept an edge exactly when `union` reports it joined two distinct components, stopping at `V - 1` edges. The bottleneck is the sort at O(E log E); the union operations are O(E * alpha(V)) and are not the dominant term. On a disconnected graph this yields a spanning forest, which may or may not be the intended answer.

**Your Union-Find is slow on a large input. What would you check?**

Whether both optimizations are present - a missing union-by-size lets a chain form, and a missing path compression leaves logarithmic finds. Then whether `find` is recursive, which risks stack depth on a tall tree. Then whether component sizes are being read at non-roots, which is a correctness bug rather than a performance one.

**When would you refuse Union-Find?**

When edges are removed as well as added, since the structure is incremental with no efficient undo. When the question needs distances rather than connectivity. And when the graph is directed and the question is about reachability or cycles.

## Exercises

1. **Foundation:** Draw the forest after `union(0,1)`, `union(2,3)`, `union(1,2)` with union by size, then show the effect of `find(3)` with path compression.
2. **Foundation:** Implement Union-Find without union by size and construct an input where `find` degrades to O(n).
3. **Interview Core:** Implement the structure above with both optimizations, then count total pointer traversals across a million operations with and without compression.
4. **Interview Core:** Solve redundant connection. Then change the vertex labelling to 0-indexed and fix the sizing.
5. **Interview Core:** Implement Kruskal returning the chosen edges. Run it on a disconnected graph and state what you got back.
6. **Interview Core:** Solve "number of provinces" twice - once with DFS, once with Union-Find - and argue which you would present first in an interview.
7. **SDE-2 Follow-up:** Solve accounts merge, where accounts sharing any email belong to one person. Explain the mapping from emails to element indices.
8. **SDE-2 Follow-up:** Attempt directed cycle detection with Union-Find and construct the graph where it gives the wrong answer.
9. **SDE-2 Follow-up:** Extend the structure to report the size of the largest component in O(1) after every union.
10. **Challenge:** Islands II - land cells appear one at a time on a grid; after each addition report the island count. Explain why an incremental structure is required.

## Chapter summary

Union-Find maintains a partition under `find` and `union`, and it exists to answer connectivity that changes as edges arrive - the case where repeated BFS becomes quadratic. Union by size and path compression are independent, each giving O(log n) alone and together giving amortized O(alpha(n)), which is effectively but not literally constant. Have `union` return whether it actually merged: that boolean is the entire answer to redundant connection, undirected cycle detection, and Kruskal's acceptance test. Kruskal is then a short function whose bottleneck is the edge sort, not the union operations, and which yields a forest rather than a tree on a disconnected graph. The structure's limits are as important as its strengths: it has no direction, so it cannot detect directed cycles; it has no distances; and it has no efficient undo, so any prompt that deletes edges is telling you to use something else.

## Revision checklist

- [ ] I can state the two operations and how connectivity is tested.
- [ ] I can explain why the naive forest degenerates and what each optimization fixes.
- [ ] I know both are needed for O(alpha(n)) and that either alone gives O(log n).
- [ ] I implement `find` iteratively and can say why.
- [ ] My `union` returns whether a merge happened, and I know three problems that need it.
- [ ] I read component size only after `find`.
- [ ] I can implement Kruskal and name the sort as the bottleneck.
- [ ] I know Kruskal yields a forest on a disconnected graph and Prim does not.
- [ ] I can explain why Union-Find cannot detect a directed cycle.
- [ ] I recognize edge deletion as a signal to use a different structure.
