package com.interviewbook.examples;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

public final class VirtualThreadsDemo {
    private VirtualThreadsDemo() {}

    public static List<Integer> runTasks(List<? extends Callable<Integer>> tasks) throws Exception {
        try (var executor = Executors.newVirtualThreadPerTaskExecutor()) {
            List<Future<Integer>> futures = new ArrayList<>();
            for (Callable<Integer> task : tasks) {
                futures.add(executor.submit(task));
            }
            List<Integer> results = new ArrayList<>(futures.size());
            for (Future<Integer> future : futures) {
                results.add(future.get());
            }
            return List.copyOf(results);
        }
    }

    public static void verify() throws Exception {
        List<Integer> values = runTasks(List.of(() -> 1, () -> 2, () -> 3));
        if (!values.equals(List.of(1, 2, 3))) {
            throw new AssertionError(values);
        }
    }
}
