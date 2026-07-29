import java.util.Arrays;

public final class DynamicProgrammingInterviewChecks {
    private DynamicProgrammingInterviewChecks() {}

    static int minimumCoins(int[] coins, int amount) {
        if (amount < 0) throw new IllegalArgumentException("negative amount");
        int unreachable = amount + 1;
        int[] dp = new int[amount + 1];
        Arrays.fill(dp, unreachable);
        dp[0] = 0;
        for (int current = 1; current <= amount; current++) {
            for (int coin : coins) {
                if (coin <= 0) throw new IllegalArgumentException("positive coins required");
                if (coin <= current && dp[current - coin] != unreachable) {
                    dp[current] = Math.min(dp[current], dp[current - coin] + 1);
                }
            }
        }
        return dp[amount] == unreachable ? -1 : dp[amount];
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

    private static void check(boolean condition, String message) {
        if (!condition) throw new AssertionError(message);
    }

    public static void main(String[] args) {
        check(minimumCoins(new int[] {1, 2, 5}, 11) == 3, "minimum coins");
        check(minimumCoins(new int[] {2}, 3) == -1, "unreachable");
        check(lcsLength("abcde", "ace") == 3, "LCS");
        check(canPartition(new int[] {1, 5, 11, 5}), "partition true");
        check(!canPartition(new int[] {1, 2, 3, 5}), "partition false");
        System.out.println("PASS 5 dynamic-programming checks");
    }
}
