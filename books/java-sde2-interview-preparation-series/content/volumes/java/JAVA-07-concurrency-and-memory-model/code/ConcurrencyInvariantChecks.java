import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

/** Deterministic checks for concurrency invariants and lifecycle policy. */
public final class ConcurrencyInvariantChecks {
    private ConcurrencyInvariantChecks() {}

    public static void main(String[] args)
            throws InterruptedException, ExecutionException {
        verifySynchronizedTransition();
        verifyAtomicTransition();
        verifyVolatilePublication();
        verifyInterruptionProtocol();
        verifyBoundedExecutorAdmission();
        verifyVirtualThreadTask();
        System.out.println("PASS 6 concurrency invariant suites");
    }

    private static void verifySynchronizedTransition() throws InterruptedException {
        LockedCounter counter = new LockedCounter();
        runConcurrently(4, 2_500, counter::increment);
        check(counter.value() == 10_000, "synchronized counter transition");
    }

    private static void verifyAtomicTransition() throws InterruptedException {
        AtomicInteger counter = new AtomicInteger();
        runConcurrently(4, 2_500, counter::incrementAndGet);
        check(counter.get() == 10_000, "atomic counter transition");
    }

    private static void runConcurrently(int threadCount, int iterations,
                                        Runnable operation) throws InterruptedException {
        CountDownLatch start = new CountDownLatch(1);
        List<Thread> workers = new ArrayList<>();
        for (int worker = 0; worker < threadCount; worker++) {
            Thread thread = Thread.ofPlatform().unstarted(() -> {
                awaitUninterruptiblyForTest(start);
                for (int iteration = 0; iteration < iterations; iteration++) {
                    operation.run();
                }
            });
            workers.add(thread);
            thread.start();
        }
        start.countDown();
        for (Thread worker : workers) {
            worker.join();
        }
    }

    private static void verifyVolatilePublication() throws InterruptedException {
        Publication publication = new Publication();
        AtomicInteger observed = new AtomicInteger(-1);
        CountDownLatch readerStarted = new CountDownLatch(1);

        Thread reader = Thread.ofPlatform().start(() -> {
            readerStarted.countDown();
            while (!publication.ready) {
                Thread.onSpinWait();
            }
            observed.set(publication.answer);
        });

        check(readerStarted.await(5, TimeUnit.SECONDS), "reader started");
        publication.answer = 42;
        publication.ready = true;
        reader.join(5_000);

        check(!reader.isAlive() && observed.get() == 42,
                "volatile flag publishes prior ordinary write");
    }

    private static void verifyInterruptionProtocol() throws InterruptedException {
        CountDownLatch waiting = new CountDownLatch(1);
        CountDownLatch neverReleased = new CountDownLatch(1);
        AtomicBoolean restoredStatus = new AtomicBoolean();

        Thread worker = Thread.ofPlatform().start(() -> {
            try {
                waiting.countDown();
                neverReleased.await();
            } catch (InterruptedException interruption) {
                Thread.currentThread().interrupt();
                restoredStatus.set(Thread.currentThread().isInterrupted());
            }
        });

        check(waiting.await(5, TimeUnit.SECONDS), "worker reached blocking call");
        worker.interrupt();
        worker.join(5_000);
        check(!worker.isAlive() && restoredStatus.get(),
                "interruption was handled and status restored");
    }

    private static void verifyBoundedExecutorAdmission() throws InterruptedException {
        CountDownLatch firstTaskRunning = new CountDownLatch(1);
        CountDownLatch releaseFirstTask = new CountDownLatch(1);
        ThreadPoolExecutor executor = new ThreadPoolExecutor(
                1,
                1,
                0L,
                TimeUnit.MILLISECONDS,
                new ArrayBlockingQueue<>(1),
                new ThreadPoolExecutor.AbortPolicy());

        try {
            executor.execute(() -> {
                firstTaskRunning.countDown();
                awaitUninterruptiblyForTest(releaseFirstTask);
            });
            check(firstTaskRunning.await(5, TimeUnit.SECONDS), "first task running");
            executor.execute(() -> { });

            boolean rejected = false;
            try {
                executor.execute(() -> { });
            } catch (RejectedExecutionException expected) {
                rejected = true;
            }
            check(rejected, "third task rejected at explicit capacity boundary");
        } finally {
            releaseFirstTask.countDown();
            executor.shutdown();
            check(executor.awaitTermination(5, TimeUnit.SECONDS),
                    "bounded executor termination");
        }
    }

    private static void verifyVirtualThreadTask()
            throws InterruptedException, ExecutionException {
        try (ExecutorService executor = Executors.newVirtualThreadPerTaskExecutor()) {
            Future<Boolean> virtual = executor.submit(
                    () -> Thread.currentThread().isVirtual());
            check(virtual.get(), "task ran on a virtual thread");
        }
    }

    private static void awaitUninterruptiblyForTest(CountDownLatch latch) {
        boolean interrupted = false;
        while (true) {
            try {
                latch.await();
                break;
            } catch (InterruptedException interruption) {
                interrupted = true;
            }
        }
        if (interrupted) {
            Thread.currentThread().interrupt();
        }
    }

    private static void check(boolean condition, String message) {
        if (!condition) {
            throw new AssertionError(message);
        }
    }

    private static final class LockedCounter {
        private int value;

        synchronized void increment() {
            value++;
        }

        synchronized int value() {
            return value;
        }
    }

    private static final class Publication {
        private int answer;
        private volatile boolean ready;
    }
}
