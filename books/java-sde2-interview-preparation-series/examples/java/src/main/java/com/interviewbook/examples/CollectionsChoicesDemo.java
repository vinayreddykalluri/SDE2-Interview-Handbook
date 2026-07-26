package com.interviewbook.examples;

import java.util.ArrayDeque;
import java.util.HashMap;
import java.util.Map;
import java.util.PriorityQueue;

public final class CollectionsChoicesDemo {
    private CollectionsChoicesDemo() {}

    public static void verify() {
        ArrayDeque<String> work = new ArrayDeque<>();
        work.addLast("first");
        work.addLast("second");
        if (!"first".equals(work.removeFirst())) {
            throw new AssertionError("queue invariant broken");
        }

        Map<String, Integer> frequencies = new HashMap<>();
        for (String word : new String[] {"jvm", "java", "jvm"}) {
            frequencies.merge(word, 1, Integer::sum);
        }
        if (frequencies.get("jvm") != 2) {
            throw new AssertionError("frequency count broken");
        }

        PriorityQueue<Integer> minimums = new PriorityQueue<>();
        minimums.add(9);
        minimums.add(3);
        minimums.add(5);
        if (minimums.remove() != 3) {
            throw new AssertionError("heap head broken");
        }
    }
}
