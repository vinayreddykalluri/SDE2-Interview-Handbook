// SPDX-License-Identifier: MIT
package io.github.vinayreddykalluri.interviewhandbook.problemsolving;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.PriorityQueue;

/** Size-k heap example with a deterministic tie rule. */
public final class TopKFrequentElements {
    private TopKFrequentElements() {
    }

    private record Frequency(int value, int count) {
    }

    /**
     * Returns at most {@code k} values ordered by frequency descending, then value ascending.
     * Expected time is O(n + d log k), where d is the distinct-value count; space is O(d + k).
     */
    public static int[] select(int[] values, int k) {
        Objects.requireNonNull(values, "values");
        if (k < 0) {
            throw new IllegalArgumentException("k must be non-negative");
        }
        if (k == 0 || values.length == 0) {
            return new int[0];
        }

        Map<Integer, Integer> counts = new HashMap<>();
        for (int value : values) {
            counts.merge(value, 1, Integer::sum);
        }

        PriorityQueue<Frequency> competitive = new PriorityQueue<>((first, second) -> {
            int byCount = Integer.compare(first.count(), second.count());
            return byCount != 0 ? byCount : Integer.compare(second.value(), first.value());
        });
        for (Map.Entry<Integer, Integer> entry : counts.entrySet()) {
            competitive.offer(new Frequency(entry.getKey(), entry.getValue()));
            if (competitive.size() > k) {
                competitive.poll();
            }
        }

        List<Frequency> selected = new ArrayList<>(competitive);
        selected.sort((first, second) -> {
            int byCount = Integer.compare(second.count(), first.count());
            return byCount != 0 ? byCount : Integer.compare(first.value(), second.value());
        });
        return selected.stream().mapToInt(Frequency::value).toArray();
    }
}
