import java.util.Arrays;
import java.util.Comparator;

public final class GreedyInterviewChecks {
    private GreedyInterviewChecks() {}

    static int minimumRemovals(int[][] input) {
        int[][] intervals = Arrays.stream(input).map(int[]::clone).toArray(int[][]::new);
        Arrays.sort(intervals, Comparator.comparingInt(interval -> interval[1]));
        int kept = 0;
        int lastEnd = Integer.MIN_VALUE;
        for (int[] interval : intervals) {
            if (interval[0] >= lastEnd) {
                kept++;
                lastEnd = interval[1];
            }
        }
        return intervals.length - kept;
    }

    static int startStation(int[] gas, int[] cost) {
        if (gas.length != cost.length || gas.length == 0) {
            throw new IllegalArgumentException("invalid arrays");
        }
        long total = 0L;
        long tank = 0L;
        int start = 0;
        for (int i = 0; i < gas.length; i++) {
            long net = (long) gas[i] - cost[i];
            total += net;
            tank += net;
            if (tank < 0L) {
                start = i + 1;
                tank = 0L;
            }
        }
        return total >= 0L ? start % gas.length : -1;
    }

    private static void check(boolean condition, String message) {
        if (!condition) throw new AssertionError(message);
    }

    public static void main(String[] args) {
        check(minimumRemovals(new int[][] {{1, 2}, {2, 3}, {3, 4}, {1, 3}}) == 1,
                "interval removals");
        check(startStation(new int[] {1, 2, 3, 4, 5}, new int[] {3, 4, 5, 1, 2}) == 3,
                "gas station");
        check(startStation(new int[] {2}, new int[] {3}) == -1, "impossible");
        System.out.println("PASS 3 greedy checks");
    }
}
