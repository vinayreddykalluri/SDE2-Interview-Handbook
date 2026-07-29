import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public final class RecursionInterviewChecks {
    private RecursionInterviewChecks() {}

    static long sum(int[] values, int length) {
        if (length < 0 || length > values.length) {
            throw new IllegalArgumentException("invalid length");
        }
        return length == 0 ? 0L : sum(values, length - 1) + values[length - 1];
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
        System.out.println("PASS 4 recursion checks");
    }
}
