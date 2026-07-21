// SPDX-License-Identifier: MIT
package io.github.vinayreddykalluri.interviewhandbook.volume19;

public class DpPatterns {
    public static int climbStairs(int n) {
        if (n < 0) {
            throw new IllegalArgumentException("n must be non-negative");
        }
        if (n <= 1) return 1;
        int prev2 = 1, prev1 = 1;
        for (int i = 2; i <= n; i++) {
            int cur = Math.addExact(prev1, prev2);
            prev2 = prev1;
            prev1 = cur;
        }
        return prev1;
    }
}
