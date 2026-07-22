// SPDX-License-Identifier: MIT
package io.github.vinayreddykalluri.interviewhandbook.problemsolving;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

/** Prefix-sum plus frequency-map example that remains valid for signed values. */
public final class SubarraySumCount {
    private SubarraySumCount() {
    }

    /**
     * Counts contiguous subarrays whose sum equals {@code target}.
     *
     * <p>Before each value, the map contains frequencies of all prior prefix sums. A prior
     * prefix {@code prefix - target} identifies one valid start. Expected time is O(n), and
     * auxiliary space is O(n).
     */
    public static long count(int[] values, long target) {
        Objects.requireNonNull(values, "values");
        Map<Long, Long> frequencies = new HashMap<>();
        frequencies.put(0L, 1L);

        long prefix = 0;
        long result = 0;
        for (int value : values) {
            prefix = Math.addExact(prefix, value);
            result = Math.addExact(result, frequencies.getOrDefault(prefix - target, 0L));
            frequencies.merge(prefix, 1L, Math::addExact);
        }
        return result;
    }
}
