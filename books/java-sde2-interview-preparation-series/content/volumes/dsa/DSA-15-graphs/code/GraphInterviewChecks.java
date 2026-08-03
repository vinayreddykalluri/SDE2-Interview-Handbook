import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.Deque;
import java.util.HashSet;
import java.util.List;
import java.util.PriorityQueue;
import java.util.Random;
import java.util.Set;

public final class GraphInterviewChecks {
    static final long INFINITY = Long.MAX_VALUE / 4;

    record DirectedEdge(int from, int to, long weight) {}
    record UndirectedEdge(int id, int first, int second, long weight) {}
    record BellmanFordResult(long[] distances, boolean[] affectedByNegativeCycle) {
        boolean hasReachableNegativeCycle() {
            for (boolean affected : affectedByNegativeCycle) {
                if (affected) {
                    return true;
                }
            }
            return false;
        }
    }
    record FloydWarshallResult(long[][] distances, boolean hasNegativeCycle) {}
    record MstResult(List<UndirectedEdge> edges, long totalWeight, boolean spansAllVertices) {}
    record CriticalResult(Set<Integer> bridgeEdgeIds, Set<Integer> articulationVertices) {}

    private GraphInterviewChecks() {}

    static int[] courseOrder(int courses, int[][] prerequisites) {
        List<List<Integer>> graph = new ArrayList<>(courses);
        for (int i = 0; i < courses; i++) graph.add(new ArrayList<>());
        int[] indegree = new int[courses];
        for (int[] pair : prerequisites) {
            graph.get(pair[1]).add(pair[0]);
            indegree[pair[0]]++;
        }
        Deque<Integer> ready = new ArrayDeque<>();
        for (int course = 0; course < courses; course++) {
            if (indegree[course] == 0) ready.addLast(course);
        }
        int[] order = new int[courses];
        int size = 0;
        while (!ready.isEmpty()) {
            int course = ready.removeFirst();
            order[size++] = course;
            for (int next : graph.get(course)) {
                if (--indegree[next] == 0) ready.addLast(next);
            }
        }
        return size == courses ? order : new int[0];
    }

    static int countIslands(char[][] grid) {
        int count = 0;
        int[][] directions = {{1, 0}, {-1, 0}, {0, 1}, {0, -1}};
        for (int row = 0; row < grid.length; row++) {
            for (int column = 0; column < grid[row].length; column++) {
                if (grid[row][column] != '1') continue;
                count++;
                Deque<int[]> queue = new ArrayDeque<>();
                grid[row][column] = '0';
                queue.addLast(new int[] {row, column});
                while (!queue.isEmpty()) {
                    int[] cell = queue.removeFirst();
                    for (int[] direction : directions) {
                        int nextRow = cell[0] + direction[0];
                        int nextColumn = cell[1] + direction[1];
                        if (nextRow >= 0 && nextRow < grid.length && nextColumn >= 0
                                && nextColumn < grid[nextRow].length
                                && grid[nextRow][nextColumn] == '1') {
                            grid[nextRow][nextColumn] = '0';
                            queue.addLast(new int[] {nextRow, nextColumn});
                        }
                    }
                }
            }
        }
        return count;
    }

    /** Shortest paths in a DAG; negative edges are valid because no cycle exists. */
    static long[] dagShortestPaths(int vertices, List<DirectedEdge> edges, int source) {
        requireVertex(source, vertices);
        List<List<DirectedEdge>> graph = directedGraph(vertices, edges);
        int[] indegree = new int[vertices];
        for (DirectedEdge edge : edges) {
            indegree[edge.to()]++;
        }
        Deque<Integer> ready = new ArrayDeque<>();
        for (int vertex = 0; vertex < vertices; vertex++) {
            if (indegree[vertex] == 0) {
                ready.addLast(vertex);
            }
        }
        int[] order = new int[vertices];
        int orderSize = 0;
        while (!ready.isEmpty()) {
            int vertex = ready.removeFirst();
            order[orderSize++] = vertex;
            for (DirectedEdge edge : graph.get(vertex)) {
                if (--indegree[edge.to()] == 0) {
                    ready.addLast(edge.to());
                }
            }
        }
        if (orderSize != vertices) {
            throw new IllegalArgumentException("DAG shortest path requires an acyclic graph");
        }
        long[] distance = new long[vertices];
        Arrays.fill(distance, INFINITY);
        distance[source] = 0;
        for (int position = 0; position < order.length; position++) {
            int vertex = order[position];
            if (distance[vertex] == INFINITY) {
                continue;
            }
            for (DirectedEdge edge : graph.get(vertex)) {
                long candidate = finiteAdd(distance[vertex], edge.weight());
                if (candidate < distance[edge.to()]) {
                    distance[edge.to()] = candidate;
                }
            }
        }
        return distance;
    }

    /**
     * Bellman-Ford distinguishes an unreachable negative cycle from one reachable
     * from the source and marks every vertex whose answer is then undefined.
     */
    static BellmanFordResult bellmanFord(
            int vertices, List<DirectedEdge> edges, int source) {
        requireVertex(source, vertices);
        List<List<DirectedEdge>> graph = directedGraph(vertices, edges);
        long[] distance = new long[vertices];
        Arrays.fill(distance, INFINITY);
        distance[source] = 0;
        for (int pass = 1; pass < vertices; pass++) {
            long[] nextDistance = distance.clone();
            boolean changed = false;
            for (DirectedEdge edge : edges) {
                if (distance[edge.from()] == INFINITY) {
                    continue;
                }
                long candidate = finiteAdd(distance[edge.from()], edge.weight());
                if (candidate < nextDistance[edge.to()]) {
                    nextDistance[edge.to()] = candidate;
                    changed = true;
                }
            }
            distance = nextDistance;
            if (!changed) {
                break;
            }
        }

        boolean[] affected = new boolean[vertices];
        Deque<Integer> queue = new ArrayDeque<>();
        for (DirectedEdge edge : edges) {
            if (distance[edge.from()] != INFINITY
                    && finiteAdd(distance[edge.from()], edge.weight()) < distance[edge.to()]
                    && !affected[edge.to()]) {
                affected[edge.to()] = true;
                queue.addLast(edge.to());
            }
        }
        while (!queue.isEmpty()) {
            int vertex = queue.removeFirst();
            for (DirectedEdge edge : graph.get(vertex)) {
                if (!affected[edge.to()]) {
                    affected[edge.to()] = true;
                    queue.addLast(edge.to());
                }
            }
        }
        return new BellmanFordResult(distance, affected);
    }

    /** All-pairs shortest paths with an explicit sentinel and saturated addition. */
    static FloydWarshallResult floydWarshall(
            int vertices, List<DirectedEdge> edges) {
        directedGraph(vertices, edges); // validates vertices and supported weights
        long[][] distance = new long[vertices][vertices];
        for (long[] row : distance) {
            Arrays.fill(row, INFINITY);
        }
        for (int vertex = 0; vertex < vertices; vertex++) {
            distance[vertex][vertex] = 0;
        }
        for (DirectedEdge edge : edges) {
            distance[edge.from()][edge.to()] =
                    Math.min(distance[edge.from()][edge.to()], edge.weight());
        }
        for (int through = 0; through < vertices; through++) {
            for (int from = 0; from < vertices; from++) {
                if (distance[from][through] == INFINITY) {
                    continue;
                }
                for (int to = 0; to < vertices; to++) {
                    if (distance[through][to] == INFINITY) {
                        continue;
                    }
                    long candidate = finiteAdd(
                            distance[from][through], distance[through][to]);
                    if (candidate < distance[from][to]) {
                        distance[from][to] = candidate;
                    }
                }
            }
        }
        boolean negativeCycle = false;
        for (int vertex = 0; vertex < vertices; vertex++) {
            negativeCycle |= distance[vertex][vertex] < 0;
        }
        return new FloydWarshallResult(distance, negativeCycle);
    }

    /** Prim returns the chosen original edges, not only the total weight. */
    static MstResult primMst(
            int vertices, List<UndirectedEdge> edges, int start) {
        requireVertex(start, vertices);
        List<List<UndirectedEdge>> graph = undirectedGraph(vertices, edges);
        boolean[] visited = new boolean[vertices];
        PriorityQueue<UndirectedEdge> frontier = new PriorityQueue<>(
                Comparator.comparingLong(UndirectedEdge::weight)
                        .thenComparingInt(UndirectedEdge::id));
        List<UndirectedEdge> chosen = new ArrayList<>();
        long total = 0;
        visited[start] = true;
        frontier.addAll(graph.get(start));
        while (!frontier.isEmpty() && chosen.size() < vertices - 1) {
            UndirectedEdge edge = frontier.remove();
            boolean firstVisited = visited[edge.first()];
            boolean secondVisited = visited[edge.second()];
            if (firstVisited == secondVisited) {
                continue;
            }
            int next = firstVisited ? edge.second() : edge.first();
            visited[next] = true;
            chosen.add(edge);
            total = finiteAdd(total, edge.weight());
            for (UndirectedEdge candidate : graph.get(next)) {
                int other = candidate.first() == next
                        ? candidate.second() : candidate.first();
                if (!visited[other]) {
                    frontier.add(candidate);
                }
            }
        }
        boolean spans = chosen.size() == Math.max(0, vertices - 1);
        return new MstResult(List.copyOf(chosen), total, spans);
    }

    /** Kosaraju returns one list per strongly connected component. */
    static List<List<Integer>> stronglyConnectedComponents(
            int vertices, List<DirectedEdge> edges) {
        List<List<DirectedEdge>> graph = directedGraph(vertices, edges);
        List<List<Integer>> reverse = emptyAdjacency(vertices);
        for (DirectedEdge edge : edges) {
            reverse.get(edge.to()).add(edge.from());
        }
        boolean[] visited = new boolean[vertices];
        List<Integer> finishOrder = new ArrayList<>(vertices);
        for (int vertex = 0; vertex < vertices; vertex++) {
            if (!visited[vertex]) {
                finishDfs(vertex, graph, visited, finishOrder);
            }
        }
        Arrays.fill(visited, false);
        List<List<Integer>> components = new ArrayList<>();
        for (int position = finishOrder.size() - 1; position >= 0; position--) {
            int vertex = finishOrder.get(position);
            if (!visited[vertex]) {
                List<Integer> component = new ArrayList<>();
                collectDfs(vertex, reverse, visited, component);
                component.sort(Integer::compare);
                components.add(component);
            }
        }
        components.sort(Comparator.comparingInt(component -> component.get(0)));
        return components;
    }

    /** Tarjan low-link logic skips only the parent edge ID, so parallel edges work. */
    static CriticalResult bridgesAndArticulationPoints(
            int vertices, List<UndirectedEdge> edges) {
        List<List<AdjacentEdge>> graph = emptyAdjacentEdges(vertices);
        Set<Integer> ids = new HashSet<>();
        for (UndirectedEdge edge : edges) {
            requireVertex(edge.first(), vertices);
            requireVertex(edge.second(), vertices);
            if (!ids.add(edge.id())) {
                throw new IllegalArgumentException("edge IDs must be unique");
            }
            graph.get(edge.first()).add(new AdjacentEdge(edge.second(), edge.id()));
            graph.get(edge.second()).add(new AdjacentEdge(edge.first(), edge.id()));
        }
        int[] discovery = new int[vertices];
        int[] low = new int[vertices];
        Arrays.fill(discovery, -1);
        int[] clock = {0};
        Set<Integer> bridges = new HashSet<>();
        Set<Integer> articulation = new HashSet<>();
        for (int vertex = 0; vertex < vertices; vertex++) {
            if (discovery[vertex] == -1) {
                criticalDfs(vertex, -1, graph, discovery, low, clock,
                        bridges, articulation);
            }
        }
        return new CriticalResult(Set.copyOf(bridges), Set.copyOf(articulation));
    }

    record AdjacentEdge(int to, int id) {}

    private static void criticalDfs(
            int vertex,
            int parentEdgeId,
            List<List<AdjacentEdge>> graph,
            int[] discovery,
            int[] low,
            int[] clock,
            Set<Integer> bridges,
            Set<Integer> articulation) {
        discovery[vertex] = low[vertex] = clock[0]++;
        int childCount = 0;
        for (AdjacentEdge edge : graph.get(vertex)) {
            if (edge.id() == parentEdgeId) {
                continue;
            }
            if (discovery[edge.to()] != -1) {
                low[vertex] = Math.min(low[vertex], discovery[edge.to()]);
                continue;
            }
            childCount++;
            criticalDfs(edge.to(), edge.id(), graph, discovery, low, clock,
                    bridges, articulation);
            low[vertex] = Math.min(low[vertex], low[edge.to()]);
            if (low[edge.to()] > discovery[vertex]) {
                bridges.add(edge.id());
            }
            if (parentEdgeId != -1 && low[edge.to()] >= discovery[vertex]) {
                articulation.add(vertex);
            }
        }
        if (parentEdgeId == -1 && childCount > 1) {
            articulation.add(vertex);
        }
    }

    private static void finishDfs(
            int vertex,
            List<List<DirectedEdge>> graph,
            boolean[] visited,
            List<Integer> order) {
        visited[vertex] = true;
        for (DirectedEdge edge : graph.get(vertex)) {
            if (!visited[edge.to()]) {
                finishDfs(edge.to(), graph, visited, order);
            }
        }
        order.add(vertex);
    }

    private static void collectDfs(
            int vertex,
            List<List<Integer>> graph,
            boolean[] visited,
            List<Integer> component) {
        visited[vertex] = true;
        component.add(vertex);
        for (int next : graph.get(vertex)) {
            if (!visited[next]) {
                collectDfs(next, graph, visited, component);
            }
        }
    }

    private static List<List<DirectedEdge>> directedGraph(
            int vertices, List<DirectedEdge> edges) {
        if (vertices < 0) {
            throw new IllegalArgumentException("vertex count cannot be negative");
        }
        List<List<DirectedEdge>> graph = new ArrayList<>(vertices);
        for (int vertex = 0; vertex < vertices; vertex++) {
            graph.add(new ArrayList<>());
        }
        for (DirectedEdge edge : edges) {
            requireVertex(edge.from(), vertices);
            requireVertex(edge.to(), vertices);
            requireSupportedWeight(edge.weight(), vertices);
            graph.get(edge.from()).add(edge);
        }
        return graph;
    }

    private static List<List<UndirectedEdge>> undirectedGraph(
            int vertices, List<UndirectedEdge> edges) {
        if (vertices < 0) {
            throw new IllegalArgumentException("vertex count cannot be negative");
        }
        List<List<UndirectedEdge>> graph = new ArrayList<>(vertices);
        for (int vertex = 0; vertex < vertices; vertex++) {
            graph.add(new ArrayList<>());
        }
        for (UndirectedEdge edge : edges) {
            requireVertex(edge.first(), vertices);
            requireVertex(edge.second(), vertices);
            requireSupportedWeight(edge.weight(), vertices);
            graph.get(edge.first()).add(edge);
            graph.get(edge.second()).add(edge);
        }
        return graph;
    }

    private static List<List<Integer>> emptyAdjacency(int vertices) {
        List<List<Integer>> graph = new ArrayList<>(vertices);
        for (int vertex = 0; vertex < vertices; vertex++) {
            graph.add(new ArrayList<>());
        }
        return graph;
    }

    private static List<List<AdjacentEdge>> emptyAdjacentEdges(int vertices) {
        List<List<AdjacentEdge>> graph = new ArrayList<>(vertices);
        for (int vertex = 0; vertex < vertices; vertex++) {
            graph.add(new ArrayList<>());
        }
        return graph;
    }

    private static void requireVertex(int vertex, int vertices) {
        if (vertices < 0 || vertex < 0 || vertex >= vertices) {
            throw new IllegalArgumentException("vertex is outside [0,V)");
        }
    }

    private static void requireSupportedWeight(long weight, int vertices) {
        long pathLengthGuard = Math.max(1L, (long) vertices + 1L);
        long maximumMagnitude = (INFINITY - 1L) / pathLengthGuard;
        if (weight < -maximumMagnitude || weight > maximumMagnitude) {
            throw new IllegalArgumentException(
                    "edge weight is too large for the graph's infinity policy");
        }
    }

    private static long finiteAdd(long first, long second) {
        if (first == INFINITY || second == INFINITY) {
            return INFINITY;
        }
        if (first == -INFINITY || second == -INFINITY) {
            return -INFINITY;
        }
        if (second > 0 && first > INFINITY - second) {
            return INFINITY;
        }
        if (second < 0 && first < -INFINITY - second) {
            return -INFINITY;
        }
        return first + second;
    }

    private static boolean shortestPathAlgorithmsAgreeOnRandomDags() {
        Random random = new Random(43L);
        for (int trial = 0; trial < 200; trial++) {
            int vertices = 2 + random.nextInt(8);
            List<DirectedEdge> edges = new ArrayList<>();
            for (int from = 0; from < vertices; from++) {
                for (int to = from + 1; to < vertices; to++) {
                    if (random.nextInt(4) == 0) {
                        edges.add(new DirectedEdge(from, to, random.nextInt(21) - 10));
                    }
                }
            }
            FloydWarshallResult allPairs = floydWarshall(vertices, edges);
            for (int source = 0; source < vertices; source++) {
                long[] dag = dagShortestPaths(vertices, edges, source);
                BellmanFordResult bellman = bellmanFord(vertices, edges, source);
                if (!Arrays.equals(dag, bellman.distances())
                        || !Arrays.equals(dag, allPairs.distances()[source])
                        || bellman.hasReachableNegativeCycle()) {
                    return false;
                }
            }
        }
        return true;
    }

    private static void check(boolean condition, String message) {
        if (!condition) throw new AssertionError(message);
    }

    public static void main(String[] args) {
        int[] order = courseOrder(4, new int[][] {{1, 0}, {2, 0}, {3, 1}, {3, 2}});
        check(order.length == 4 && order[0] == 0 && order[3] == 3, "course order");
        check(courseOrder(2, new int[][] {{0, 1}, {1, 0}}).length == 0, "cycle");
        char[][] grid = {{'1', '1', '0'}, {'0', '1', '0'}, {'1', '0', '1'}};
        check(countIslands(grid) == 3, "islands");
        check(Arrays.stream(order).distinct().count() == 4L, "unique order");

        List<DirectedEdge> dag = List.of(
                new DirectedEdge(0, 1, 4),
                new DirectedEdge(0, 2, 2),
                new DirectedEdge(2, 1, -3),
                new DirectedEdge(1, 3, 2),
                new DirectedEdge(2, 3, 5));
        check(Arrays.equals(dagShortestPaths(5, dag, 0),
                new long[] {0, -1, 2, 1, INFINITY}), "DAG shortest paths");

        BellmanFordResult normal = bellmanFord(5, dag, 0);
        check(Arrays.equals(normal.distances(), new long[] {0, -1, 2, 1, INFINITY})
                && !normal.hasReachableNegativeCycle(), "Bellman-Ford finite answers");
        List<DirectedEdge> reachableCycle = List.of(
                new DirectedEdge(0, 1, 1),
                new DirectedEdge(1, 2, -2),
                new DirectedEdge(2, 1, -2),
                new DirectedEdge(2, 3, 1));
        BellmanFordResult cycleResult = bellmanFord(4, reachableCycle, 0);
        check(cycleResult.hasReachableNegativeCycle()
                && cycleResult.affectedByNegativeCycle()[1]
                && cycleResult.affectedByNegativeCycle()[2]
                && cycleResult.affectedByNegativeCycle()[3]
                && !cycleResult.affectedByNegativeCycle()[0], "reachable negative cycle propagation");
        BellmanFordResult unreachableCycle = bellmanFord(4, List.of(
                new DirectedEdge(0, 1, 1),
                new DirectedEdge(2, 3, -1),
                new DirectedEdge(3, 2, -1)), 0);
        check(!unreachableCycle.hasReachableNegativeCycle(), "unreachable cycle is irrelevant");

        FloydWarshallResult allPairs = floydWarshall(5, dag);
        check(allPairs.distances()[0][3] == 1
                && allPairs.distances()[4][0] == INFINITY
                && !allPairs.hasNegativeCycle(), "Floyd-Warshall infinity policy");
        check(floydWarshall(3, reachableCycle.subList(1, 3)).hasNegativeCycle(),
                "Floyd-Warshall negative-cycle diagonal");

        List<UndirectedEdge> weighted = List.of(
                new UndirectedEdge(10, 0, 1, 4),
                new UndirectedEdge(11, 0, 2, 1),
                new UndirectedEdge(12, 2, 1, 2),
                new UndirectedEdge(13, 1, 3, 1),
                new UndirectedEdge(14, 2, 3, 5));
        MstResult mst = primMst(4, weighted, 0);
        check(mst.spansAllVertices() && mst.totalWeight() == 4 && mst.edges().size() == 3,
                "Prim returns MST edges and weight");
        check(!primMst(3, List.of(new UndirectedEdge(1, 0, 1, 2)), 0)
                .spansAllVertices(), "Prim reports disconnected graph");

        List<List<Integer>> components = stronglyConnectedComponents(6, List.of(
                new DirectedEdge(0, 1, 1), new DirectedEdge(1, 0, 1),
                new DirectedEdge(1, 2, 1), new DirectedEdge(2, 3, 1),
                new DirectedEdge(3, 2, 1), new DirectedEdge(3, 4, 1)));
        check(components.equals(List.of(List.of(0, 1), List.of(2, 3), List.of(4), List.of(5))),
                "strongly connected components");

        CriticalResult critical = bridgesAndArticulationPoints(4, List.of(
                new UndirectedEdge(20, 0, 1, 0),
                new UndirectedEdge(21, 1, 2, 0),
                new UndirectedEdge(22, 1, 2, 0),
                new UndirectedEdge(23, 2, 3, 0)));
        check(critical.bridgeEdgeIds().equals(Set.of(20, 23)),
                "parallel edge is not a bridge");
        check(critical.articulationVertices().equals(Set.of(1, 2)),
                "articulation vertices");
        check(shortestPathAlgorithmsAgreeOnRandomDags(),
                "random shortest-path differential test");

        System.out.println("PASS 16 graph checks");
    }
}
