package com.interviewbook.examples;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;

public final class GenericAlgorithms {
    private GenericAlgorithms() {}

    public static <T> T max(Collection<? extends T> source,
                            Comparator<? super T> comparator) {
        if (source.isEmpty()) {
            throw new IllegalArgumentException("empty source");
        }
        T best = null;
        boolean first = true;
        for (T item : source) {
            if (first || comparator.compare(item, best) > 0) {
                best = item;
                first = false;
            }
        }
        return best;
    }

    public static <T> void copy(Collection<? extends T> source,
                                Collection<? super T> destination) {
        destination.addAll(source);
    }

    public static void verify() {
        List<Integer> numbers = List.of(4, 9, 2);
        if (max(numbers, Comparator.naturalOrder()) != 9) {
            throw new AssertionError();
        }
        List<Number> destination = new ArrayList<>();
        copy(numbers, destination);
        if (!destination.equals(numbers)) {
            throw new AssertionError();
        }
    }
}
