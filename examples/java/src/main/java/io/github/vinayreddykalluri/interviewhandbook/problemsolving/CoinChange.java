// SPDX-License-Identifier: MIT
package io.github.vinayreddykalluri.interviewhandbook.problemsolving;

import java.util.Arrays;
import java.util.Objects;

/** Bottom-up dynamic programming for the unbounded minimum-coin problem. */
public final class CoinChange {
    private CoinChange() {
    }

    /**
     * Returns the minimum number of coins needed for {@code amount}, or -1 when impossible.
     * Time is O(amount * coinCount); auxiliary space is O(amount).
     */
    public static int minCoins(int[] coins, int amount) {
        Objects.requireNonNull(coins, "coins");
        if (amount < 0) {
            throw new IllegalArgumentException("amount must be non-negative");
        }
        for (int coin : coins) {
            if (coin <= 0) {
                throw new IllegalArgumentException("coin values must be positive");
            }
        }

        int impossible = Integer.MAX_VALUE / 2;
        int[] best = new int[amount + 1];
        Arrays.fill(best, impossible);
        best[0] = 0;

        for (int current = 1; current <= amount; current++) {
            for (int coin : coins) {
                if (coin <= current) {
                    best[current] = Math.min(best[current], best[current - coin] + 1);
                }
            }
        }
        return best[amount] == impossible ? -1 : best[amount];
    }
}
