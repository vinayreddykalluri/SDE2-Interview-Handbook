# Essential Graph Frontier and Weight Clinics

Graph traversal is not one algorithm with different containers. The frontier policy must match edge weights and source semantics. Multi-source BFS and 0-1 BFS are high-value transitions between ordinary BFS and general weighted shortest paths.

## Clinic 1: multi-source BFS

When distance is measured from the nearest member of a source set, enqueue every source at distance zero before traversal begins. The initial queue is level zero, so ordinary BFS discovers each remaining vertex at its minimum distance to any source.

Common examples include distance to the nearest gate, rotting oranges, nearest zero, fire spread, and simultaneous contagion. Running a separate BFS from every source repeats work and can cost O(S(V+E)); one combined traversal is O(V+E).

Mark distance when enqueuing. That makes `distance != -1` both the discovered flag and the proof that the cell enters the queue once.

## Clinic 2: 0-1 BFS

For edge weights restricted to zero and one, a deque preserves Dijkstra's priority order without a heap:

- a zero-weight improvement goes to the front;
- a one-weight improvement goes to the back.

The deque contains vertices in nondecreasing tentative-distance bands. Relax an edge only when the new distance is smaller. The algorithm is O(V+E) for adjacency-list input.

### Shortest-path selection boundary

| Edge contract | Preferred starting point |
|---|---|
| unweighted or equal nonnegative weights | BFS |
| weights only 0 and 1 | 0-1 BFS |
| arbitrary nonnegative weights | Dijkstra |
| negative edges, no reachable negative cycle | Bellman-Ford or DAG relaxation when acyclic |
| all-pairs, moderate dense graph | Floyd-Warshall |

Bellman-Ford performs up to V-1 relaxation passes and uses one more pass to detect a reachable negative cycle. Dijkstra is not repaired by merely allowing a negative number into its edge type; its finalized-minimum proof no longer holds.

## Runnable Java 21 clinic

```java
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Deque;
import java.util.List;

public final class GraphCoverageClinic {
    private static final int[][] DIRECTIONS = {{1, 0}, {-1, 0}, {0, 1}, {0, -1}};

    private GraphCoverageClinic() {
    }

    private record Edge(int to, int weight) {
    }

    public static int[][] distanceToNearestSource(int[][] grid) {
        int columns = validateRectangular(grid);
        int[][] distance = new int[grid.length][columns];
        Deque<int[]> queue = new ArrayDeque<>();
        for (int row = 0; row < grid.length; row++) {
            Arrays.fill(distance[row], -1);
            for (int column = 0; column < columns; column++) {
                if (grid[row][column] == 1) {
                    distance[row][column] = 0;
                    queue.addLast(new int[] {row, column});
                }
            }
        }

        while (!queue.isEmpty()) {
            int[] cell = queue.removeFirst();
            for (int[] direction : DIRECTIONS) {
                int nextRow = cell[0] + direction[0];
                int nextColumn = cell[1] + direction[1];
                if (nextRow < 0 || nextRow >= grid.length || nextColumn < 0
                        || nextColumn >= columns || grid[nextRow][nextColumn] == -1
                        || distance[nextRow][nextColumn] != -1) {
                    continue;
                }
                distance[nextRow][nextColumn] = distance[cell[0]][cell[1]] + 1;
                queue.addLast(new int[] {nextRow, nextColumn});
            }
        }
        return distance;
    }

    public static int[] zeroOneShortestPaths(int vertices, int[][] edges, int source) {
        if (vertices <= 0 || source < 0 || source >= vertices) {
            throw new IllegalArgumentException("invalid vertex contract");
        }
        List<List<Edge>> graph = new ArrayList<>();
        for (int vertex = 0; vertex < vertices; vertex++) {
            graph.add(new ArrayList<>());
        }
        for (int[] edge : edges) {
            if (edge == null || edge.length != 3 || edge[0] < 0 || edge[0] >= vertices
                    || edge[1] < 0 || edge[1] >= vertices
                    || (edge[2] != 0 && edge[2] != 1)) {
                throw new IllegalArgumentException("edges must be (from,to,0-or-1)");
            }
            graph.get(edge[0]).add(new Edge(edge[1], edge[2]));
        }

        int unreachable = Integer.MAX_VALUE;
        int[] distance = new int[vertices];
        Arrays.fill(distance, unreachable);
        distance[source] = 0;
        Deque<Integer> deque = new ArrayDeque<>();
        deque.addFirst(source);

        while (!deque.isEmpty()) {
            int vertex = deque.removeFirst();
            for (Edge edge : graph.get(vertex)) {
                int candidate = distance[vertex] + edge.weight();
                if (candidate >= distance[edge.to()]) {
                    continue;
                }
                distance[edge.to()] = candidate;
                if (edge.weight() == 0) {
                    deque.addFirst(edge.to());
                } else {
                    deque.addLast(edge.to());
                }
            }
        }
        return distance;
    }

    private static int validateRectangular(int[][] grid) {
        if (grid == null || grid.length == 0 || grid[0] == null
                || grid[0].length == 0) {
            throw new IllegalArgumentException("grid must be nonempty");
        }
        int columns = grid[0].length;
        for (int[] row : grid) {
            if (row == null || row.length != columns) {
                throw new IllegalArgumentException("grid must be rectangular");
            }
        }
        return columns;
    }

    public static void main(String[] args) {
        int[][] distance = distanceToNearestSource(new int[][] {
            {1, 0, 0}, {0, -1, 0}, {0, 0, 1}
        });
        assert distance[0][2] == 2 && distance[2][0] == 2;

        int[] shortest = zeroOneShortestPaths(4, new int[][] {
            {0, 1, 1}, {0, 2, 0}, {2, 1, 0}, {1, 3, 1}, {2, 3, 1}
        }, 0);
        assert shortest[1] == 0 && shortest[3] == 1;
        System.out.println("PASS essential graph clinics");
    }
}
```

Expected output with assertions enabled:

```text
PASS essential graph clinics
```

## Interviewer follow-up chain with model answers

**Interviewer:** Why is multi-source BFS not the same as connecting a synthetic source with weighted edges?

**Candidate:** It is equivalent to a synthetic source with zero-weight edges to every real source. Initializing all sources at distance zero avoids storing that extra vertex and preserves ordinary BFS levels.

**Interviewer:** Why not use a normal queue for zero-one weights?

**Candidate:** A zero-cost edge must be processed before vertices one distance level farther away. A FIFO queue cannot guarantee that; adding zero-cost relaxations to the front restores the required priority.

**Interviewer:** How would you return a path?

**Candidate:** Record `parent[to] = vertex` whenever relaxation improves the distance. If equal-distance deterministic paths matter, define and implement a tie policy rather than overwriting parents accidentally.
