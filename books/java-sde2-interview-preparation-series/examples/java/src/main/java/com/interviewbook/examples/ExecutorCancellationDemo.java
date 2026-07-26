package com.interviewbook.examples;

import java.time.Duration;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

public final class ExecutorCancellationDemo {
    private ExecutorCancellationDemo() {}

    public static void verify() throws Exception {
        ExecutorService pool = Executors.newSingleThreadExecutor();
        try {
            Future<?> task = pool.submit(() -> {
                try {
                    while (!Thread.currentThread().isInterrupted()) {
                        Thread.sleep(Duration.ofSeconds(1));
                    }
                } catch (InterruptedException interrupted) {
                    Thread.currentThread().interrupt();
                }
            });
            task.cancel(true);
            if (!task.isCancelled()) {
                throw new AssertionError("cancellation state not visible");
            }
        } finally {
            pool.shutdownNow();
            if (!pool.awaitTermination(2, TimeUnit.SECONDS)) {
                throw new AssertionError("executor failed to terminate");
            }
        }
    }
}
