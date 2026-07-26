import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Deque;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.PriorityQueue;
import java.util.Set;
import java.util.TreeSet;

/**
 * Executable examples for Time and Space Complexity for Java Interviews.
 *
 * <p>These checks validate behavior and operation counts. They deliberately do
 * not use wall-clock timing as proof of asymptotic complexity.</p>
 */
public final class ComplexityExamples {
    private static int passed;

    private ComplexityExamples() {}

    public static void main(String[] args) {
        run("01 constant indexed access", ComplexityExamples::constantAccess);
        run("02 linear scan", ComplexityExamples::linearScan);
        run("03 early exit", ComplexityExamples::earlyExit);
        run("04 consecutive loops", ComplexityExamples::consecutiveLoops);
        run("05 independent nested loops", ComplexityExamples::independentNestedLoops);
        run("06 triangular loop", ComplexityExamples::triangularLoop);
        run("07 repeated halving", ComplexityExamples::repeatedHalving);
        run("08 logarithmic outer loop", ComplexityExamples::logarithmicOuterLoop);
        run("09 linearithmic work shape", ComplexityExamples::linearithmicShape);
        run("10 forward-only two pointers", ComplexityExamples::twoPointers);
        run("11 geometric inner work", ComplexityExamples::geometricInnerWork);
        run("12 rectangular matrix", ComplexityExamples::rectangularMatrix);
        run("13 jagged matrix", ComplexityExamples::jaggedMatrix);
        run("14 StringBuilder growth", ComplexityExamples::stringBuilder);
        run("15 defensive array copy", ComplexityExamples::defensiveCopy);
        run("16 linear recursion depth", ComplexityExamples::linearRecursionDepth);
        run("17 logarithmic recursion depth", ComplexityExamples::logRecursionDepth);
        run("18 output-sensitive result", ComplexityExamples::outputSensitive);
        run("19 ArrayList indexed access", ComplexityExamples::arrayListAccess);
        run("20 HashSet membership", ComplexityExamples::hashSetMembership);
        run("21 HashMap frequency", ComplexityExamples::hashMapFrequency);
        run("22 TreeSet navigation", ComplexityExamples::treeSetNavigation);
        run("23 ArrayDeque queue", ComplexityExamples::arrayDequeQueue);
        run("24 PriorityQueue order", ComplexityExamples::priorityQueueOrder);

        System.out.println("PASS " + passed + " complexity examples");
    }

    private static void constantAccess() {
        int[] values = {5, 8, 13, 21};
        check(values[2] == 13, "indexed read");
    }

    private static void linearScan() {
        int[] values = {2, 4, 6, 8};
        ScanResult result = find(values, 99);
        check(result.index() == -1 && result.inspections() == values.length,
                "absent target inspects n values");
    }

    private static void earlyExit() {
        int[] values = {2, 4, 6, 8};
        ScanResult result = find(values, 2);
        check(result.index() == 0 && result.inspections() == 1,
                "first target inspects once");
    }

    private static ScanResult find(int[] values, int target) {
        int inspections = 0;
        for (int index = 0; index < values.length; index++) {
            inspections++;
            if (values[index] == target) return new ScanResult(index, inspections);
        }
        return new ScanResult(-1, inspections);
    }

    private static void consecutiveLoops() {
        int n = 7;
        int operations = 0;
        for (int index = 0; index < n; index++) operations++;
        for (int index = 0; index < n; index++) operations++;
        check(operations == 2 * n, "n + n");
    }

    private static void independentNestedLoops() {
        int rows = 3;
        int columns = 5;
        int operations = 0;
        for (int row = 0; row < rows; row++) {
            for (int column = 0; column < columns; column++) operations++;
        }
        check(operations == rows * columns, "rows times columns");
    }

    private static void triangularLoop() {
        int n = 6;
        int operations = 0;
        for (int left = 0; left < n; left++) {
            for (int right = left + 1; right < n; right++) operations++;
        }
        check(operations == n * (n - 1) / 2, "pair count");
    }

    private static void repeatedHalving() {
        int value = 32;
        int steps = 0;
        while (value > 1) {
            value /= 2;
            steps++;
        }
        check(steps == 5, "log2(32)");
    }

    private static void logarithmicOuterLoop() {
        int n = 16;
        int levels = 0;
        for (int size = 1; size < n; size *= 2) levels++;
        check(levels == 4, "1, 2, 4, 8");
    }

    private static void linearithmicShape() {
        int n = 16;
        int operations = 0;
        for (int size = 1; size < n; size *= 2) {
            for (int index = 0; index < n; index++) operations++;
        }
        check(operations == 64, "n times log2(n)");
    }

    private static void twoPointers() {
        int n = 9;
        int left = 0;
        int movements = 0;
        for (int right = 0; right < n; right++) {
            movements++;
            while (left < right && right - left > 2) {
                left++;
                movements++;
            }
        }
        check(movements <= 2 * n, "each pointer advances at most n times");
    }

    private static void geometricInnerWork() {
        int n = 16;
        int operations = 0;
        for (int size = 1; size <= n; size *= 2) {
            for (int index = 0; index < size; index++) operations++;
        }
        check(operations == 31 && operations < 2 * n, "1 + 2 + ... + n");
    }

    private static void rectangularMatrix() {
        int[][] matrix = new int[2][5];
        int visits = 0;
        for (int[] row : matrix) {
            for (int ignored : row) visits++;
        }
        check(visits == 10, "rows times columns");
    }

    private static void jaggedMatrix() {
        int[][] matrix = {{1}, {2, 3, 4}, {}, {5, 6}};
        int visits = 0;
        for (int[] row : matrix) {
            for (int ignored : row) visits++;
        }
        check(visits == 6, "sum of row lengths");
    }

    private static void stringBuilder() {
        List<String> parts = List.of("time", "-", "space");
        StringBuilder result = new StringBuilder();
        for (String part : parts) result.append(part);
        check(result.toString().equals("time-space"), "append characters");
    }

    private static void defensiveCopy() {
        int[] source = {3, 1, 2};
        int[] copy = Arrays.copyOf(source, source.length);
        copy[0] = 99;
        check(source[0] == 3 && copy[0] == 99, "independent arrays");
    }

    private static void linearRecursionDepth() {
        check(countDownDepth(7) == 8, "frames include base call");
    }

    private static int countDownDepth(int n) {
        if (n == 0) return 1;
        return 1 + countDownDepth(n - 1);
    }

    private static void logRecursionDepth() {
        check(halvingDepth(32) == 6, "32, 16, 8, 4, 2, 1");
    }

    private static int halvingDepth(int n) {
        if (n <= 1) return 1;
        return 1 + halvingDepth(n / 2);
    }

    private static void outputSensitive() {
        int[] values = {4, 1, 4, 2, 4};
        List<Integer> positions = new ArrayList<>();
        for (int index = 0; index < values.length; index++) {
            if (values[index] == 4) positions.add(index);
        }
        check(positions.equals(List.of(0, 2, 4)), "output has k matches");
    }

    private static void arrayListAccess() {
        List<Integer> values = new ArrayList<>(List.of(10, 20, 30));
        check(values.get(1) == 20, "indexed access");
    }

    private static void hashSetMembership() {
        Set<Integer> seen = new HashSet<>();
        check(seen.add(7) && !seen.add(7), "add reports duplicate");
    }

    private static void hashMapFrequency() {
        Map<Integer, Integer> counts = new HashMap<>();
        for (int value : new int[] {2, 1, 2, 2}) {
            counts.merge(value, 1, Integer::sum);
        }
        check(counts.equals(Map.of(1, 1, 2, 3)), "frequency map");
    }

    private static void treeSetNavigation() {
        TreeSet<Integer> sorted = new TreeSet<>(List.of(8, 2, 5, 11));
        check(sorted.ceiling(6) == 8 && sorted.floor(6) == 5, "ordered neighbors");
    }

    private static void arrayDequeQueue() {
        Deque<Integer> queue = new ArrayDeque<>();
        queue.offerLast(10);
        queue.offerLast(20);
        check(queue.removeFirst() == 10 && queue.removeFirst() == 20, "FIFO");
    }

    private static void priorityQueueOrder() {
        PriorityQueue<Integer> heap = new PriorityQueue<>();
        heap.addAll(List.of(9, 3, 7, 1));
        List<Integer> drained = new ArrayList<>();
        while (!heap.isEmpty()) drained.add(heap.remove());
        check(drained.equals(List.of(1, 3, 7, 9)), "poll order");
    }

    private static void run(String name, Runnable example) {
        try {
            example.run();
            passed++;
        } catch (RuntimeException | AssertionError error) {
            throw new AssertionError("FAILED " + name, error);
        }
    }

    private static void check(boolean condition, String message) {
        if (!condition) throw new AssertionError(message);
    }

    private record ScanResult(int index, int inspections) {}
}
