import java.io.IOException;
import java.lang.management.ManagementFactory;
import java.lang.management.MemoryMXBean;
import java.lang.management.MemoryUsage;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import jdk.jfr.Event;
import jdk.jfr.Label;
import jdk.jfr.Name;
import jdk.jfr.Recording;
import jdk.jfr.consumer.RecordedEvent;
import jdk.jfr.consumer.RecordingFile;

/** Safe, deterministic checks for an evidence-first JVM diagnostic workflow. */
public final class PerformanceEvidenceChecks {
    private PerformanceEvidenceChecks() {}

    public static void main(String[] args) throws Exception {
        verifyPercentileCalculation();
        verifyMemorySnapshot();
        verifyThreadSnapshot();
        verifyJfrRecordingRoundTrip();
        verifyBoundedRetentionPolicy();
        System.out.println("PASS 5 performance evidence suites");
    }

    private static void verifyPercentileCalculation() {
        long[] latencyMicros = {90, 10, 50, 20, 40, 30, 80, 70, 60, 100};
        check(percentileNearestRank(latencyMicros, 0.50) == 50,
                "nearest-rank p50");
        check(percentileNearestRank(latencyMicros, 0.95) == 100,
                "nearest-rank p95");
        check(latencyMicros[0] == 90, "percentile calculation preserves input");
    }

    static long percentileNearestRank(long[] observations, double percentile) {
        if (observations.length == 0) {
            throw new IllegalArgumentException("observations must not be empty");
        }
        if (!(percentile > 0.0 && percentile <= 1.0)) {
            throw new IllegalArgumentException("percentile must be in (0, 1]");
        }
        long[] sorted = observations.clone();
        Arrays.sort(sorted);
        int rank = (int) Math.ceil(percentile * sorted.length);
        return sorted[rank - 1];
    }

    private static void verifyMemorySnapshot() {
        MemoryMXBean memory = ManagementFactory.getMemoryMXBean();
        MemoryUsage heap = memory.getHeapMemoryUsage();
        MemoryUsage nonHeap = memory.getNonHeapMemoryUsage();
        check(heap.getUsed() >= 0 && heap.getCommitted() >= heap.getUsed(),
                "heap snapshot values");
        check(nonHeap.getUsed() >= 0, "non-heap snapshot values");
    }

    private static void verifyThreadSnapshot() throws InterruptedException {
        CountDownLatch waiting = new CountDownLatch(1);
        CountDownLatch release = new CountDownLatch(1);
        Thread worker = Thread.ofPlatform().name("evidence-waiter").start(() -> {
            waiting.countDown();
            try {
                release.await();
            } catch (InterruptedException interruption) {
                Thread.currentThread().interrupt();
            }
        });

        try {
            check(waiting.await(5, TimeUnit.SECONDS), "worker entered wait");
            Thread.State state = awaitBlockingState(worker, 5, TimeUnit.SECONDS);
            Map<Thread, StackTraceElement[]> snapshot = Thread.getAllStackTraces();
            check(snapshot.containsKey(worker), "thread snapshot contains worker");
            check(state == Thread.State.WAITING || state == Thread.State.TIMED_WAITING,
                    "worker reached a blocking wait state");
        } finally {
            release.countDown();
            worker.join(5_000);
        }
        check(!worker.isAlive(), "diagnostic worker terminated");
    }

    private static Thread.State awaitBlockingState(Thread thread, long timeout,
                                                    TimeUnit unit) {
        long deadline = System.nanoTime() + unit.toNanos(timeout);
        while (System.nanoTime() < deadline) {
            Thread.State state = thread.getState();
            if (state == Thread.State.WAITING || state == Thread.State.TIMED_WAITING) {
                return state;
            }
            if (state == Thread.State.TERMINATED) {
                throw new AssertionError("worker terminated before blocking");
            }
            Thread.onSpinWait();
        }
        throw new AssertionError("worker did not reach blocking state");
    }

    private static void verifyJfrRecordingRoundTrip() throws IOException {
        Path recordingPath = Files.createTempFile("java-wave2-evidence-", ".jfr");
        try {
            try (Recording recording = new Recording()) {
                recording.enable(DiagnosticEvent.class)
                        .withThreshold(Duration.ZERO);
                recording.start();

                DiagnosticEvent event = new DiagnosticEvent();
                event.message = "interview-evidence";
                event.commit();

                recording.stop();
                recording.dump(recordingPath);
            }

            boolean found = false;
            try (RecordingFile recording = new RecordingFile(recordingPath)) {
                while (recording.hasMoreEvents()) {
                    RecordedEvent event = recording.readEvent();
                    if (event.getEventType().getName().equals("book.DiagnosticEvent")
                            && event.getString("message").equals("interview-evidence")) {
                        found = true;
                    }
                }
            }
            check(Files.size(recordingPath) > 0 && found,
                    "JFR recording contains committed custom event");
        } finally {
            Files.deleteIfExists(recordingPath);
        }
    }

    private static void verifyBoundedRetentionPolicy() {
        BoundedCache<Integer, byte[]> cache = new BoundedCache<>(3);
        cache.put(1, new byte[16]);
        cache.put(2, new byte[16]);
        cache.put(3, new byte[16]);
        cache.get(1);
        cache.put(4, new byte[16]);

        check(cache.size() == 3, "cache remains bounded");
        check(!cache.containsKey(2) && cache.containsKey(1),
                "access-order eviction policy");
    }

    private static void check(boolean condition, String message) {
        if (!condition) {
            throw new AssertionError(message);
        }
    }

    @Name("book.DiagnosticEvent")
    @Label("Book Diagnostic Event")
    static final class DiagnosticEvent extends Event {
        @Label("Message")
        String message;
    }

    private static final class BoundedCache<K, V> extends LinkedHashMap<K, V> {
        private static final long serialVersionUID = 1L;

        private final int maximumEntries;

        BoundedCache(int maximumEntries) {
            super(16, 0.75f, true);
            if (maximumEntries <= 0) {
                throw new IllegalArgumentException("maximumEntries must be positive");
            }
            this.maximumEntries = maximumEntries;
        }

        @Override
        protected boolean removeEldestEntry(Map.Entry<K, V> eldest) {
            return size() > maximumEntries;
        }
    }
}
