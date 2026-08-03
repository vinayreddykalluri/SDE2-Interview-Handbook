# Realistic Graph Interview Rounds

## Round 1: number of islands

### Prompt

Count connected groups of `'1'` cells in a rectangular grid using four-direction adjacency. The input may be modified.

### Candidate answer

Each land cell is a vertex; adjacency is implicit. Scan every cell. On unvisited land, increment the component count and flood-fill, marking cells as water.

```java
static int countIslands(char[][] grid) {
    int islands = 0;
    for (int row = 0; row < grid.length; row++) {
        for (int column = 0; column < grid[row].length; column++) {
            if (grid[row][column] == '1') {
                islands++;
                eraseIsland(grid, row, column);
            }
        }
    }
    return islands;
}

static void eraseIsland(char[][] grid, int startRow, int startColumn) {
    Deque<int[]> queue = new ArrayDeque<>();
    grid[startRow][startColumn] = '0';
    queue.addLast(new int[] {startRow, startColumn});
    int[][] directions = {{1, 0}, {-1, 0}, {0, 1}, {0, -1}};
    while (!queue.isEmpty()) {
        int[] cell = queue.removeFirst();
        for (int[] direction : directions) {
            int row = cell[0] + direction[0];
            int column = cell[1] + direction[1];
            if (row >= 0 && row < grid.length && column >= 0
                    && column < grid[row].length && grid[row][column] == '1') {
                grid[row][column] = '0';
                queue.addLast(new int[] {row, column});
            }
        }
    }
}
```

**Complexity:** O(RC) for a rectangular grid because each cell is marked once; queue can hold O(RC) cells. Input mutation is part of the contract.

**Follow-up:** If the grid cannot be modified, use a boolean visited structure. For huge sparse coordinates, store land positions in a hash set instead of allocating the bounding rectangle.

## Round 2: course schedule and one valid order

### Prompt

Courses are numbered `0..n-1`; pair `[course, prerequisite]` means prerequisite must come first. Return one valid order or an empty array if impossible.

### Model answer

Create edge `prerequisite -> course`, compute indegrees, and apply Kahn's algorithm.

```java
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
```

### Follow-up answers

**Why does incomplete output imply a cycle?** Remaining vertices all have unresolved incoming edges from the remaining subgraph, so none can start; a finite directed graph with that property contains a cycle.

**Duplicate prerequisite pairs?** If duplicates are not meaningful, deduplicate edges or they inflate indegrees and adjacency work.

**Deterministic smallest order?** Replace the ready deque with a min-heap, increasing time to O((V + E) log V) in the broad bound.

## Round 3: network delay with nonnegative weights

### Prompt

Given directed travel times, return the time for a signal from source to reach every vertex, or -1 if some vertex is unreachable.

### Candidate plan

This is single-source shortest path with nonnegative weights: Dijkstra. Store long distances, push improved candidates, and skip stale heap entries.

```java
record Edge(int to, int weight) {}
record State(int node, long distance) {}

static long networkDelay(List<List<Edge>> graph, int source) {
    long[] distance = new long[graph.size()];
    Arrays.fill(distance, Long.MAX_VALUE);
    PriorityQueue<State> queue = new PriorityQueue<>(
            Comparator.comparingLong(State::distance));
    distance[source] = 0L;
    queue.add(new State(source, 0L));
    while (!queue.isEmpty()) {
        State state = queue.poll();
        if (state.distance() != distance[state.node()]) continue;
        for (Edge edge : graph.get(state.node())) {
            if (edge.weight() < 0) throw new IllegalArgumentException("negative edge");
            long candidate = state.distance() + edge.weight();
            if (candidate < distance[edge.to()]) {
                distance[edge.to()] = candidate;
                queue.add(new State(edge.to(), candidate));
            }
        }
    }
    long answer = 0L;
    for (long value : distance) {
        if (value == Long.MAX_VALUE) return -1L;
        answer = Math.max(answer, value);
    }
    return answer;
}
```

### Follow-up answers

**Why stale entries?** Java `PriorityQueue` has no efficient decrease-key. Add a new improved state and ignore older states when polled.

**Overflow?** Relax only from finite distances and validate weights. If domain totals could exceed long, the numeric contract needs redesign.

**Complexity?** With adjacency lists and duplicate heap entries, O((V + E) log V) is a common bound under ordinary formulations; be ready to state O(E log E) for the literal maximum heap-entry count. Space is O(V + E).

## Closing answer pattern

Model vertices/edges, state direction and weights, choose representation, define visited/distance state timing, cover disconnected input, justify the algorithm from constraints, and express complexity in V and E.
