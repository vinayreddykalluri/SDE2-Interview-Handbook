import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.PriorityQueue;
import java.util.Random;

public final class GreedyInterviewChecks {
    record Interval(int id, int start, int end) {}

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
            if (gas[i] < 0 || cost[i] < 0) {
                throw new IllegalArgumentException("gas and cost must be nonnegative");
            }
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

    /** Maximum-cardinality compatible set for half-open intervals [start,end). */
    static List<Interval> selectMaximumNonOverlapping(List<Interval> input) {
        List<Interval> intervals = new ArrayList<>(input);
        for (Interval interval : intervals) {
            if (interval.start() > interval.end()) {
                throw new IllegalArgumentException("interval start exceeds end");
            }
        }
        intervals.sort(Comparator.comparingInt(Interval::end)
                .thenComparingInt(Interval::start)
                .thenComparingInt(Interval::id));
        List<Interval> selected = new ArrayList<>();
        int lastEnd = Integer.MIN_VALUE;
        for (Interval interval : intervals) {
            if (interval.start() >= lastEnd) {
                selected.add(interval);
                lastEnd = interval.end();
            }
        }
        return List.copyOf(selected);
    }

    static int minimumJumps(int[] maximumSteps) {
        for (int step : maximumSteps) {
            if (step < 0) {
                throw new IllegalArgumentException("jump lengths cannot be negative");
            }
        }
        if (maximumSteps.length <= 1) {
            return 0;
        }
        int jumps = 0;
        int currentLayerEnd = 0;
        int farthest = 0;
        for (int index = 0; index < maximumSteps.length - 1; index++) {
            if (index > farthest) {
                return -1;
            }
            long reach = index + (long) maximumSteps[index];
            farthest = (int) Math.min(maximumSteps.length - 1L,
                    Math.max(farthest, reach));
            if (index == currentLayerEnd) {
                jumps++;
                currentLayerEnd = farthest;
                if (currentLayerEnd >= maximumSteps.length - 1) {
                    return jumps;
                }
            }
        }
        return -1;
    }

    /** Stations are [position,fuel], sorted by position; fuel can be chosen once. */
    static int minimumRefuelStops(long target, long startFuel, long[][] stations) {
        if (target < 0 || startFuel < 0) {
            throw new IllegalArgumentException("target and fuel must be nonnegative");
        }
        long previousPosition = -1;
        for (long[] station : stations) {
            if (station.length != 2 || station[0] < previousPosition
                    || station[0] < 0 || station[0] > target || station[1] < 0) {
                throw new IllegalArgumentException("invalid station sequence");
            }
            previousPosition = station[0];
        }
        PriorityQueue<Long> passedFuel = new PriorityQueue<>(Comparator.reverseOrder());
        long reachable = startFuel;
        int stationIndex = 0;
        int stops = 0;
        while (reachable < target) {
            while (stationIndex < stations.length
                    && stations[stationIndex][0] <= reachable) {
                passedFuel.add(stations[stationIndex][1]);
                stationIndex++;
            }
            if (passedFuel.isEmpty()) {
                return -1;
            }
            long fuel = passedFuel.remove();
            reachable = fuel >= target - reachable ? target : reachable + fuel;
            stops++;
        }
        return stops;
    }

    private static boolean intervalGreedyMatchesBruteForce() {
        Random random = new Random(61L);
        for (int trial = 0; trial < 1_000; trial++) {
            int count = random.nextInt(11);
            List<Interval> intervals = new ArrayList<>();
            for (int id = 0; id < count; id++) {
                int start = random.nextInt(12);
                int end = start + 1 + random.nextInt(5);
                intervals.add(new Interval(id, start, end));
            }
            int greedy = selectMaximumNonOverlapping(intervals).size();
            int best = 0;
            for (int mask = 0; mask < 1 << count; mask++) {
                List<Interval> chosen = new ArrayList<>();
                for (int index = 0; index < count; index++) {
                    if ((mask & 1 << index) != 0) {
                        chosen.add(intervals.get(index));
                    }
                }
                chosen.sort(Comparator.comparingInt(Interval::start)
                        .thenComparingInt(Interval::end));
                boolean compatible = true;
                for (int index = 1; index < chosen.size(); index++) {
                    compatible &= chosen.get(index).start() >= chosen.get(index - 1).end();
                }
                if (compatible) {
                    best = Math.max(best, chosen.size());
                }
            }
            if (greedy != best) {
                return false;
            }
        }
        return true;
    }

    private static boolean jumpGreedyMatchesDynamicProgramming() {
        Random random = new Random(67L);
        for (int trial = 0; trial < 2_000; trial++) {
            int[] steps = new int[random.nextInt(15)];
            for (int index = 0; index < steps.length; index++) {
                steps[index] = random.nextInt(6);
            }
            int[] best = new int[steps.length];
            Arrays.fill(best, Integer.MAX_VALUE);
            if (steps.length > 0) {
                best[0] = 0;
            }
            for (int index = 0; index < steps.length; index++) {
                if (best[index] == Integer.MAX_VALUE) {
                    continue;
                }
                int limit = (int) Math.min(steps.length - 1L, index + (long) steps[index]);
                for (int next = index + 1; next <= limit; next++) {
                    best[next] = Math.min(best[next], best[index] + 1);
                }
            }
            int expected = steps.length <= 1 ? 0
                    : best[steps.length - 1] == Integer.MAX_VALUE
                    ? -1 : best[steps.length - 1];
            if (minimumJumps(steps) != expected) {
                return false;
            }
        }
        return true;
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
        if (!condition) throw new AssertionError(message);
    }

    public static void main(String[] args) {
        check(minimumRemovals(new int[][] {{1, 2}, {2, 3}, {3, 4}, {1, 3}}) == 1,
                "interval removals");
        check(startStation(new int[] {1, 2, 3, 4, 5}, new int[] {3, 4, 5, 1, 2}) == 3,
                "gas station");
        check(startStation(new int[] {2}, new int[] {3}) == -1, "impossible");
        List<Interval> selected = selectMaximumNonOverlapping(List.of(
                new Interval(1, 1, 4), new Interval(2, 3, 5),
                new Interval(3, 0, 6), new Interval(4, 5, 7),
                new Interval(5, 8, 9), new Interval(6, 5, 9)));
        check(selected.stream().map(Interval::id).toList().equals(List.of(1, 4, 5)),
                "interval schedule returns auditable choices");
        check(selectMaximumNonOverlapping(List.of(
                new Interval(1, 1, 2), new Interval(2, 2, 3))).size() == 2,
                "touching half-open intervals are compatible");
        expectFailure(() -> selectMaximumNonOverlapping(
                List.of(new Interval(1, 3, 2))));

        check(minimumJumps(new int[] {2, 3, 1, 1, 4}) == 2, "minimum jumps");
        check(minimumJumps(new int[] {3, 2, 1, 0, 4}) == -1, "unreachable jump");
        check(minimumJumps(new int[0]) == 0, "empty jump contract");

        long[][] stations = {{10, 60}, {20, 30}, {30, 30}, {60, 40}};
        check(minimumRefuelStops(100, 10, stations) == 2, "deferred refuel choices");
        check(minimumRefuelStops(100, 1, stations) == -1, "refuel impossible");
        check(minimumRefuelStops(50, 50, new long[0][]) == 0, "already reachable");
        expectFailure(() -> minimumRefuelStops(100, 10,
                new long[][] {{20, 1}, {10, 2}}));
        check(intervalGreedyMatchesBruteForce(), "interval exhaustive oracle");
        check(jumpGreedyMatchesDynamicProgramming(), "jump DP oracle");
        System.out.println("PASS 15 greedy checks");
    }
}
