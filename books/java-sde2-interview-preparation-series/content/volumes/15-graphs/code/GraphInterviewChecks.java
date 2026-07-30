import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Deque;
import java.util.List;

public final class GraphInterviewChecks {
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
        System.out.println("PASS 4 graph checks");
    }
}
