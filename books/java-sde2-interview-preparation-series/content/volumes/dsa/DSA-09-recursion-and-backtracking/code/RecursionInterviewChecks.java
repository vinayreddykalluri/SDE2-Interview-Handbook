import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Random;
import java.util.Set;

public final class RecursionInterviewChecks {
    private RecursionInterviewChecks() {}

    static long sum(int[] values, int length) {
        if (length < 0 || length > values.length) {
            throw new IllegalArgumentException("invalid length");
        }
        return length == 0 ? 0L : sum(values, length - 1) + values[length - 1];
    }

    static List<String> sumFrameTrace(int[] values) {
        List<String> trace = new ArrayList<>();
        tracedSum(values, values.length, trace);
        return trace;
    }

    private static long tracedSum(int[] values, int length, List<String> trace) {
        trace.add("enter length=" + length);
        if (length == 0) {
            trace.add("return length=0 value=0");
            return 0;
        }
        long result = tracedSum(values, length - 1, trace) + values[length - 1];
        trace.add("return length=" + length + " value=" + result);
        return result;
    }

    static List<List<Integer>> uniqueSubsets(int[] input) {
        int[] values = input.clone();
        Arrays.sort(values);
        List<List<Integer>> result = new ArrayList<>();
        build(values, 0, new ArrayList<>(), result);
        return result;
    }

    private static void build(int[] values, int start, List<Integer> path,
                              List<List<Integer>> result) {
        result.add(new ArrayList<>(path));
        for (int i = start; i < values.length; i++) {
            if (i > start && values[i] == values[i - 1]) {
                continue;
            }
            path.add(values[i]);
            build(values, i + 1, path, result);
            path.remove(path.size() - 1);
        }
    }

    static List<String> balancedParentheses(int pairs) {
        if (pairs < 0) {
            throw new IllegalArgumentException("negative pairs");
        }
        List<String> result = new ArrayList<>();
        buildParentheses(pairs, 0, 0, new StringBuilder(), result);
        return result;
    }

    private static void buildParentheses(int pairs, int open, int close,
                                         StringBuilder path, List<String> result) {
        if (path.length() == pairs * 2) {
            result.add(path.toString());
            return;
        }
        if (open < pairs) {
            path.append('(');
            buildParentheses(pairs, open + 1, close, path, result);
            path.deleteCharAt(path.length() - 1);
        }
        if (close < open) {
            path.append(')');
            buildParentheses(pairs, open, close + 1, path, result);
            path.deleteCharAt(path.length() - 1);
        }
    }

    static List<List<Integer>> uniquePermutations(int[] input) {
        int[] values = input.clone();
        Arrays.sort(values);
        List<List<Integer>> result = new ArrayList<>();
        permute(values, new boolean[values.length], new ArrayList<>(), result);
        return result;
    }

    private static void permute(
            int[] values,
            boolean[] used,
            List<Integer> path,
            List<List<Integer>> result) {
        if (path.size() == values.length) {
            result.add(new ArrayList<>(path));
            return;
        }
        for (int index = 0; index < values.length; index++) {
            if (used[index]) {
                continue;
            }
            if (index > 0 && values[index] == values[index - 1] && !used[index - 1]) {
                continue;
            }
            used[index] = true;
            path.add(values[index]);
            permute(values, used, path, result);
            path.remove(path.size() - 1);
            used[index] = false;
        }
    }

    static List<List<Integer>> combinationSum(int[] input, int target) {
        if (target < 0) {
            throw new IllegalArgumentException("target cannot be negative");
        }
        int[] candidates = input.clone();
        Arrays.sort(candidates);
        for (int candidate : candidates) {
            if (candidate <= 0) {
                throw new IllegalArgumentException("positive candidates required");
            }
        }
        List<List<Integer>> result = new ArrayList<>();
        combine(candidates, target, 0, new ArrayList<>(), result);
        return result;
    }

    private static void combine(
            int[] candidates,
            int remaining,
            int start,
            List<Integer> path,
            List<List<Integer>> result) {
        if (remaining == 0) {
            result.add(new ArrayList<>(path));
            return;
        }
        for (int index = start; index < candidates.length; index++) {
            if (index > start && candidates[index] == candidates[index - 1]) {
                continue;
            }
            int candidate = candidates[index];
            if (candidate > remaining) {
                break;
            }
            path.add(candidate);
            combine(candidates, remaining - candidate, index, path, result);
            path.remove(path.size() - 1);
        }
    }

    static int countNQueens(int size) {
        if (size < 0 || size > 15) {
            throw new IllegalArgumentException("size must be in [0,15]");
        }
        return placeQueen(0, size, new boolean[size],
                new boolean[Math.max(0, size * 2 - 1)],
                new boolean[Math.max(0, size * 2 - 1)]);
    }

    private static int placeQueen(
            int row,
            int size,
            boolean[] columns,
            boolean[] descending,
            boolean[] ascending) {
        if (row == size) {
            return 1;
        }
        int solutions = 0;
        for (int column = 0; column < size; column++) {
            int downIndex = row - column + size - 1;
            int upIndex = row + column;
            if (columns[column] || descending[downIndex] || ascending[upIndex]) {
                continue;
            }
            columns[column] = true;
            descending[downIndex] = true;
            ascending[upIndex] = true;
            solutions += placeQueen(row + 1, size, columns, descending, ascending);
            columns[column] = false;
            descending[downIndex] = false;
            ascending[upIndex] = false;
        }
        return solutions;
    }

    static boolean wordExists(char[][] board, String word) {
        if (board == null || word == null) {
            throw new IllegalArgumentException("board and word are required");
        }
        if (word.isEmpty()) {
            return true;
        }
        boolean[][] used = new boolean[board.length][];
        for (int row = 0; row < board.length; row++) {
            if (board[row] == null) {
                throw new IllegalArgumentException("null row");
            }
            used[row] = new boolean[board[row].length];
        }
        for (int row = 0; row < board.length; row++) {
            for (int column = 0; column < board[row].length; column++) {
                if (findWord(board, word, 0, row, column, used)) {
                    return true;
                }
            }
        }
        return false;
    }

    private static boolean findWord(
            char[][] board,
            String word,
            int index,
            int row,
            int column,
            boolean[][] used) {
        if (row < 0 || row >= board.length || column < 0
                || column >= board[row].length || used[row][column]
                || board[row][column] != word.charAt(index)) {
            return false;
        }
        if (index + 1 == word.length()) {
            return true;
        }
        used[row][column] = true;
        boolean found = findWord(board, word, index + 1, row + 1, column, used)
                || findWord(board, word, index + 1, row - 1, column, used)
                || findWord(board, word, index + 1, row, column + 1, used)
                || findWord(board, word, index + 1, row, column - 1, used);
        used[row][column] = false;
        return found;
    }

    private static boolean permutationCountsMatchSetOracle() {
        Random random = new Random(47L);
        for (int trial = 0; trial < 200; trial++) {
            int[] values = new int[random.nextInt(8)];
            for (int index = 0; index < values.length; index++) {
                values[index] = random.nextInt(4);
            }
            List<List<Integer>> permutations = uniquePermutations(values);
            Set<List<Integer>> unique = new HashSet<>(permutations);
            if (unique.size() != permutations.size()) {
                return false;
            }
            long expected = factorial(values.length);
            int[] frequencies = new int[4];
            for (int value : values) {
                frequencies[value]++;
            }
            for (int frequency : frequencies) {
                expected /= factorial(frequency);
            }
            if (permutations.size() != expected) {
                return false;
            }
        }
        return true;
    }

    private static long factorial(int value) {
        long result = 1;
        for (int factor = 2; factor <= value; factor++) {
            result *= factor;
        }
        return result;
    }

    private static void expectFailure(Runnable action) {
        try {
            action.run();
        } catch (IllegalArgumentException expected) {
            return;
        }
        throw new AssertionError("expected IllegalArgumentException");
    }

    private static void check(boolean condition, String message) {
        if (!condition) {
            throw new AssertionError(message);
        }
    }

    public static void main(String[] args) {
        check(sum(new int[] {4, 7, 2}, 3) == 13L, "recursive sum");
        check(uniqueSubsets(new int[] {1, 2, 2}).size() == 6, "unique subsets");
        check(balancedParentheses(3).size() == 5, "parentheses");
        check(balancedParentheses(0).equals(List.of("")), "zero pairs");
        check(sumFrameTrace(new int[] {4, 7}).equals(List.of(
                "enter length=2", "enter length=1", "enter length=0",
                "return length=0 value=0", "return length=1 value=4",
                "return length=2 value=11")), "call-frame trace");
        expectFailure(() -> sum(new int[] {1}, 2));

        int[] permutationInput = {2, 1, 2};
        check(uniquePermutations(permutationInput).equals(List.of(
                List.of(1, 2, 2), List.of(2, 1, 2), List.of(2, 2, 1))),
                "unique permutations");
        check(Arrays.equals(permutationInput, new int[] {2, 1, 2}), "input preserved");
        check(combinationSum(new int[] {2, 3, 2, 7}, 7).equals(List.of(
                List.of(2, 2, 3), List.of(7))), "combination sum with duplicate candidates");
        expectFailure(() -> combinationSum(new int[] {0, 1}, 2));
        check(countNQueens(0) == 1, "empty board has one arrangement");
        check(countNQueens(4) == 2, "four queens");

        char[][] board = {{'A', 'B', 'C', 'E'}, {'S', 'F', 'C', 'S'}, {'A', 'D', 'E', 'E'}};
        check(wordExists(board, "ABCCED"), "word search true");
        check(!wordExists(board, "ABCB"), "cell cannot be reused");
        check(Arrays.deepEquals(board,
                new char[][] {{'A', 'B', 'C', 'E'}, {'S', 'F', 'C', 'S'}, {'A', 'D', 'E', 'E'}}),
                "word search preserves board");
        check(wordExists(new char[0][], ""), "empty word");
        check(permutationCountsMatchSetOracle(), "permutation count oracle");
        System.out.println("PASS 17 recursion checks");
    }
}
