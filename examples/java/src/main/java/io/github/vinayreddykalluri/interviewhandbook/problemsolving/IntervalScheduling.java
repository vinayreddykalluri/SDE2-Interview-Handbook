// SPDX-License-Identifier: MIT
package io.github.vinayreddykalluri.interviewhandbook.problemsolving;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;

/** Greedy maximum-cardinality scheduling for half-open intervals. */
public final class IntervalScheduling {
    private IntervalScheduling() {
    }

    public record Interval(int startInclusive, int endExclusive) {
        public Interval {
            if (startInclusive > endExclusive) {
                throw new IllegalArgumentException("interval start must not exceed its end");
            }
        }
    }

    /**
     * Returns a maximum-size non-overlapping subset ordered by finish time.
     * The earliest-finish exchange argument proves the greedy choice safe. Time is O(n log n).
     */
    public static List<Interval> selectMaximum(List<Interval> intervals) {
        Objects.requireNonNull(intervals, "intervals");
        List<Interval> ordered = new ArrayList<>(intervals.size());
        for (Interval interval : intervals) {
            ordered.add(Objects.requireNonNull(interval, "interval"));
        }
        ordered.sort(
                Comparator.comparingInt(Interval::endExclusive)
                        .thenComparingInt(Interval::startInclusive)
        );

        List<Interval> selected = new ArrayList<>();
        int availableFrom = Integer.MIN_VALUE;
        for (Interval interval : ordered) {
            if (interval.startInclusive() >= availableFrom) {
                selected.add(interval);
                availableFrom = interval.endExclusive();
            }
        }
        return List.copyOf(selected);
    }
}
