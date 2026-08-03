import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Random;

public final class DynamicProgrammingInterviewChecks {
    record KnapsackResult(long maximumValue, List<Integer> chosenIndices) {}

    private DynamicProgrammingInterviewChecks() {}

    static int minimumCoins(int[] coins, int amount) {
        validateCoinInput(coins, amount);
        int unreachable = amount + 1;
        int[] dp = new int[amount + 1];
        Arrays.fill(dp, unreachable);
        dp[0] = 0;
        for (int current = 1; current <= amount; current++) {
            for (int coin : coins) {
                if (coin <= current && dp[current - coin] != unreachable) {
                    dp[current] = Math.min(dp[current], dp[current - coin] + 1);
                }
            }
        }
        return dp[amount] == unreachable ? -1 : dp[amount];
    }

    static int minimumCoinsMemoized(int[] coins, int amount) {
        validateCoinInput(coins, amount);
        if (amount > 2_000) {
            throw new IllegalArgumentException("memoized teaching version limits call depth");
        }
        int[] memo = new int[amount + 1];
        Arrays.fill(memo, -2);
        memo[0] = 0;
        return minimumCoinsMemoized(coins, amount, memo);
    }

    private static int minimumCoinsMemoized(int[] coins, int amount, int[] memo) {
        if (memo[amount] != -2) {
            return memo[amount];
        }
        int best = Integer.MAX_VALUE;
        for (int coin : coins) {
            if (coin <= amount) {
                int previous = minimumCoinsMemoized(coins, amount - coin, memo);
                if (previous >= 0) {
                    best = Math.min(best, previous + 1);
                }
            }
        }
        memo[amount] = best == Integer.MAX_VALUE ? -1 : best;
        return memo[amount];
    }

    private static void validateCoinInput(int[] coins, int amount) {
        if (amount < 0 || amount > 1_000_000) {
            throw new IllegalArgumentException("amount must be in [0,1000000]");
        }
        for (int coin : coins) {
            if (coin <= 0) {
                throw new IllegalArgumentException("positive coins required");
            }
        }
    }

    static int lcsLength(String first, String second) {
        int[][] dp = new int[first.length() + 1][second.length() + 1];
        for (int i = 1; i <= first.length(); i++) {
            for (int j = 1; j <= second.length(); j++) {
                dp[i][j] = first.charAt(i - 1) == second.charAt(j - 1)
                        ? dp[i - 1][j - 1] + 1
                        : Math.max(dp[i - 1][j], dp[i][j - 1]);
            }
        }
        return dp[first.length()][second.length()];
    }

    static boolean canPartition(int[] values) {
        long total = 0L;
        for (int value : values) {
            if (value < 0) throw new IllegalArgumentException("nonnegative values required");
            total += value;
        }
        if ((total & 1L) != 0L || total / 2L > Integer.MAX_VALUE) return false;
        int target = (int) (total / 2L);
        boolean[] possible = new boolean[target + 1];
        possible[0] = true;
        for (int value : values) {
            for (int sum = target; sum >= value; sum--) {
                possible[sum] |= possible[sum - value];
            }
        }
        return possible[target];
    }

    static long fibonacciMemoized(int n) {
        if (n < 0 || n > 92) {
            throw new IllegalArgumentException("n must be in [0,92]");
        }
        long[] memo = new long[n + 1];
        Arrays.fill(memo, -1L);
        return fibonacciMemoized(n, memo);
    }

    private static long fibonacciMemoized(int n, long[] memo) {
        if (n < 2) {
            return n;
        }
        if (memo[n] != -1L) {
            return memo[n];
        }
        memo[n] = Math.addExact(
                fibonacciMemoized(n - 1, memo), fibonacciMemoized(n - 2, memo));
        return memo[n];
    }

    static long fibonacciConstantSpace(int n) {
        if (n < 0 || n > 92) {
            throw new IllegalArgumentException("n must be in [0,92]");
        }
        if (n == 0) {
            return 0;
        }
        long previous = 0;
        long current = 1;
        for (int index = 2; index <= n; index++) {
            long next = Math.addExact(previous, current);
            previous = current;
            current = next;
        }
        return current;
    }

    static KnapsackResult knapsack01(
            int[] weights, int[] values, int capacity) {
        if (weights.length != values.length || capacity < 0 || capacity > 100_000) {
            throw new IllegalArgumentException("invalid knapsack dimensions");
        }
        long cells = (weights.length + 1L) * (capacity + 1L);
        if (cells > 10_000_000L) {
            throw new IllegalArgumentException("knapsack table exceeds teaching memory budget");
        }
        for (int item = 0; item < weights.length; item++) {
            if (weights[item] <= 0 || values[item] < 0) {
                throw new IllegalArgumentException(
                        "positive weights and nonnegative values required");
            }
        }
        long[][] best = new long[weights.length + 1][capacity + 1];
        for (int item = 1; item <= weights.length; item++) {
            int weight = weights[item - 1];
            int value = values[item - 1];
            for (int room = 0; room <= capacity; room++) {
                best[item][room] = best[item - 1][room];
                if (weight <= room) {
                    best[item][room] = Math.max(best[item][room],
                            best[item - 1][room - weight] + value);
                }
            }
        }
        List<Integer> chosen = new ArrayList<>();
        int room = capacity;
        for (int item = weights.length; item > 0; item--) {
            if (best[item][room] != best[item - 1][room]) {
                chosen.add(item - 1);
                room -= weights[item - 1];
            }
        }
        Collections.reverse(chosen);
        return new KnapsackResult(best[weights.length][capacity], List.copyOf(chosen));
    }

    static int editDistance(String first, String second) {
        if (second.length() > first.length()) {
            return editDistance(second, first);
        }
        int[] previous = new int[second.length() + 1];
        for (int column = 0; column <= second.length(); column++) {
            previous[column] = column;
        }
        for (int row = 1; row <= first.length(); row++) {
            int[] current = new int[second.length() + 1];
            current[0] = row;
            for (int column = 1; column <= second.length(); column++) {
                if (first.charAt(row - 1) == second.charAt(column - 1)) {
                    current[column] = previous[column - 1];
                } else {
                    current[column] = 1 + Math.min(previous[column - 1],
                            Math.min(previous[column], current[column - 1]));
                }
            }
            previous = current;
        }
        return previous[second.length()];
    }

    static long minimumPathSum(int[][] grid) {
        if (grid.length == 0 || grid[0].length == 0) {
            throw new IllegalArgumentException("nonempty grid required");
        }
        int columns = grid[0].length;
        long[] best = new long[columns];
        Arrays.fill(best, Long.MAX_VALUE);
        best[0] = 0;
        for (int[] row : grid) {
            if (row.length != columns) {
                throw new IllegalArgumentException("rectangular grid required");
            }
            for (int column = 0; column < columns; column++) {
                long fromAbove = best[column];
                long fromLeft = column == 0 ? Long.MAX_VALUE : best[column - 1];
                long previous = Math.min(fromAbove, fromLeft);
                best[column] = Math.addExact(previous, row[column]);
            }
        }
        return best[columns - 1];
    }

    static long maximumProfitAtMostTwoTransactions(int[] prices) {
        long buyFirst = Long.MIN_VALUE / 4;
        long sellFirst = 0;
        long buySecond = Long.MIN_VALUE / 4;
        long sellSecond = 0;
        for (int price : prices) {
            if (price < 0) {
                throw new IllegalArgumentException("prices cannot be negative");
            }
            buyFirst = Math.max(buyFirst, -(long) price);
            sellFirst = Math.max(sellFirst, buyFirst + price);
            buySecond = Math.max(buySecond, sellFirst - price);
            sellSecond = Math.max(sellSecond, buySecond + price);
        }
        return sellSecond;
    }

    private static boolean coinMethodsAgree() {
        Random random = new Random(71L);
        for (int trial = 0; trial < 2_000; trial++) {
            int[] coins = new int[1 + random.nextInt(5)];
            for (int index = 0; index < coins.length; index++) {
                coins[index] = 1 + random.nextInt(10);
            }
            int amount = random.nextInt(80);
            if (minimumCoins(coins, amount) != minimumCoinsMemoized(coins, amount)) {
                return false;
            }
        }
        return true;
    }

    private static boolean knapsackMatchesSubsetOracle() {
        Random random = new Random(73L);
        for (int trial = 0; trial < 1_000; trial++) {
            int count = random.nextInt(12);
            int[] weights = new int[count];
            int[] values = new int[count];
            for (int item = 0; item < count; item++) {
                weights[item] = 1 + random.nextInt(8);
                values[item] = random.nextInt(20);
            }
            int capacity = random.nextInt(25);
            long expected = 0;
            for (int mask = 0; mask < 1 << count; mask++) {
                int weight = 0;
                long value = 0;
                for (int item = 0; item < count; item++) {
                    if ((mask & 1 << item) != 0) {
                        weight += weights[item];
                        value += values[item];
                    }
                }
                if (weight <= capacity) {
                    expected = Math.max(expected, value);
                }
            }
            if (knapsack01(weights, values, capacity).maximumValue() != expected) {
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
        check(minimumCoins(new int[] {1, 2, 5}, 11) == 3, "minimum coins");
        check(minimumCoins(new int[] {2}, 3) == -1, "unreachable");
        check(lcsLength("abcde", "ace") == 3, "LCS");
        check(canPartition(new int[] {1, 5, 11, 5}), "partition true");
        check(!canPartition(new int[] {1, 2, 3, 5}), "partition false");
        check(minimumCoinsMemoized(new int[] {1, 2, 5}, 11) == 3,
                "memoized minimum coins");
        expectFailure(() -> minimumCoins(new int[] {0, 1}, 0));
        check(fibonacciMemoized(50) == 12_586_269_025L, "memoized Fibonacci");
        check(fibonacciConstantSpace(92) == 7_540_113_804_746_346_429L,
                "space-optimized Fibonacci boundary");
        expectFailure(() -> fibonacciMemoized(93));

        KnapsackResult knapsack = knapsack01(
                new int[] {2, 3, 4, 5}, new int[] {3, 4, 5, 8}, 8);
        check(knapsack.maximumValue() == 12L, "knapsack value");
        check(knapsack.chosenIndices().equals(List.of(1, 3)), "knapsack reconstruction");
        check(editDistance("horse", "ros") == 3, "edit distance");
        check(editDistance("", "abc") == 3, "edit empty boundary");
        check(minimumPathSum(new int[][] {{1, 3, 1}, {1, 5, 1}, {4, 2, 1}}) == 7L,
                "minimum path sum");
        expectFailure(() -> minimumPathSum(new int[][] {{1}, {2, 3}}));
        check(maximumProfitAtMostTwoTransactions(new int[] {3, 3, 5, 0, 0, 3, 1, 4}) == 6L,
                "two stock transactions");
        check(maximumProfitAtMostTwoTransactions(new int[0]) == 0L,
                "empty price series");
        check(coinMethodsAgree(), "memo/tabulation differential test");
        check(knapsackMatchesSubsetOracle(), "knapsack subset oracle");
        System.out.println("PASS 20 dynamic-programming checks");
    }
}
