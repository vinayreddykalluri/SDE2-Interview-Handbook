package com.interviewbook.examples;

public final class DynamicProgramming {
    private DynamicProgramming() {}

    public static int minimumCoins(int[] coins, int amount) {
        if (amount < 0) {
            throw new IllegalArgumentException("negative amount");
        }
        int unreachable = amount + 1;
        int[] best = new int[amount + 1];
        java.util.Arrays.fill(best, unreachable);
        best[0] = 0;
        for (int value = 1; value <= amount; value++) {
            for (int coin : coins) {
                if (coin <= 0) {
                    throw new IllegalArgumentException("coin must be positive");
                }
                if (coin <= value && best[value - coin] != unreachable) {
                    best[value] = Math.min(best[value], best[value - coin] + 1);
                }
            }
        }
        return best[amount] == unreachable ? -1 : best[amount];
    }

    public static void verify() {
        if (minimumCoins(new int[] {1, 3, 4}, 6) != 2) {
            throw new AssertionError();
        }
        if (minimumCoins(new int[] {2}, 3) != -1) {
            throw new AssertionError();
        }
    }
}
