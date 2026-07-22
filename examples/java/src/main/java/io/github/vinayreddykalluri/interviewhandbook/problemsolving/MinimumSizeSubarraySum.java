// SPDX-License-Identifier: MIT
package io.github.vinayreddykalluri.interviewhandbook.problemsolving;

import java.util.Objects;

/** Variable sliding window for positive values. */
public final class MinimumSizeSubarraySum {
    private MinimumSizeSubarraySum() {
    }

    /**
     * Returns the shortest contiguous length whose sum is at least {@code target}, or zero.
     *
     * <p>Invariant: {@code sum} equals exactly the values in {@code [left, right]}. Positivity
     * makes discarded starts permanently irrelevant. Time is O(n); auxiliary space is O(1).
     */
    public static int minLengthAtLeast(int[] values, long target) {
        Objects.requireNonNull(values, "values");
        if (target <= 0) {
            throw new IllegalArgumentException("target must be positive");
        }

        int left = 0;
        int best = values.length + 1;
        long sum = 0;
        for (int right = 0; right < values.length; right++) {
            if (values[right] <= 0) {
                throw new IllegalArgumentException("every value must be positive");
            }
            sum = Math.addExact(sum, values[right]);
            while (sum >= target) {
                best = Math.min(best, right - left + 1);
                sum -= values[left++];
            }
        }
        return best == values.length + 1 ? 0 : best;
    }
}
