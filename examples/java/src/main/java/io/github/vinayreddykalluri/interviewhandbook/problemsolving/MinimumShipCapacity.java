// SPDX-License-Identifier: MIT
package io.github.vinayreddykalluri.interviewhandbook.problemsolving;

import java.util.Objects;

/** Binary search over a monotone capacity predicate. */
public final class MinimumShipCapacity {
    private MinimumShipCapacity() {
    }

    /**
     * Returns the minimum daily capacity that ships ordered positive weights within {@code days}.
     * Time is O(n log(sum(weights))); auxiliary space is O(1).
     */
    public static long minimumCapacity(int[] weights, int days) {
        Objects.requireNonNull(weights, "weights");
        if (weights.length == 0) {
            throw new IllegalArgumentException("weights must not be empty");
        }
        if (days <= 0) {
            throw new IllegalArgumentException("days must be positive");
        }

        long low = 0;
        long total = 0;
        for (int weight : weights) {
            if (weight <= 0) {
                throw new IllegalArgumentException("weights must be positive");
            }
            low = Math.max(low, weight);
            total = Math.addExact(total, weight);
        }

        long high = Math.addExact(total, 1);
        while (low < high) {
            long candidate = low + (high - low) / 2;
            if (canShip(weights, days, candidate)) {
                high = candidate;
            } else {
                low = candidate + 1;
            }
        }
        return low;
    }

    private static boolean canShip(int[] weights, int allowedDays, long capacity) {
        int usedDays = 1;
        long currentLoad = 0;
        for (int weight : weights) {
            if (currentLoad + weight > capacity) {
                usedDays++;
                currentLoad = 0;
                if (usedDays > allowedDays) {
                    return false;
                }
            }
            currentLoad += weight;
        }
        return true;
    }
}
