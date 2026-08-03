package interview.redis;

import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayDeque;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.LongSupplier;

public final class RedisMechanics {
    private RedisMechanics() {}

    public static byte[] encodeResp2Command(List<String> arguments) {
        if (arguments.isEmpty()) {
            throw new IllegalArgumentException("a command needs arguments");
        }
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        writeAscii(output, "*" + arguments.size() + "\r\n");
        for (String argument : arguments) {
            byte[] bytes = Objects.requireNonNull(argument, "argument")
                    .getBytes(StandardCharsets.UTF_8);
            writeAscii(output, "$" + bytes.length + "\r\n");
            output.writeBytes(bytes);
            writeAscii(output, "\r\n");
        }
        return output.toByteArray();
    }

    private static void writeAscii(ByteArrayOutputStream output, String value) {
        output.writeBytes(value.getBytes(StandardCharsets.US_ASCII));
    }

    public static final class ManualClock implements LongSupplier {
        private long millis;

        public ManualClock(long initialMillis) {
            millis = initialMillis;
        }

        public void advance(long deltaMillis) {
            if (deltaMillis < 0) {
                throw new IllegalArgumentException("time cannot move backward");
            }
            millis = Math.addExact(millis, deltaMillis);
        }

        @Override
        public long getAsLong() {
            return millis;
        }
    }

    public static final class ExpiringStore {
        private record Entry(String value, long expiresAtMillis) {}

        private final LongSupplier clock;
        private final Map<String, Entry> entries = new HashMap<>();

        public ExpiringStore(LongSupplier clock) {
            this.clock = Objects.requireNonNull(clock, "clock");
        }

        public synchronized boolean setNxPx(String key, String value, long ttlMillis) {
            if (ttlMillis <= 0) {
                throw new IllegalArgumentException("TTL must be positive");
            }
            expire(key);
            if (entries.containsKey(key)) {
                return false;
            }
            entries.put(key, new Entry(value, Math.addExact(clock.getAsLong(), ttlMillis)));
            return true;
        }

        public synchronized String get(String key) {
            expire(key);
            Entry entry = entries.get(key);
            return entry == null ? null : entry.value();
        }

        public synchronized boolean compareAndDelete(String key, String expectedValue) {
            expire(key);
            Entry entry = entries.get(key);
            if (entry == null || !entry.value().equals(expectedValue)) {
                return false;
            }
            entries.remove(key);
            return true;
        }

        private void expire(String key) {
            Entry entry = entries.get(key);
            if (entry != null && clock.getAsLong() >= entry.expiresAtMillis()) {
                entries.remove(key);
            }
        }
    }

    public static final class SlidingWindowLimiter {
        private final int limit;
        private final long windowMillis;
        private final Map<String, ArrayDeque<Long>> accepted = new HashMap<>();

        public SlidingWindowLimiter(int limit, long windowMillis) {
            if (limit <= 0 || windowMillis <= 0) {
                throw new IllegalArgumentException("limit and window must be positive");
            }
            this.limit = limit;
            this.windowMillis = windowMillis;
        }

        public synchronized boolean allow(String key, long nowMillis) {
            ArrayDeque<Long> timestamps = accepted.computeIfAbsent(key, ignored -> new ArrayDeque<>());
            long cutoff = nowMillis - windowMillis;
            while (!timestamps.isEmpty() && timestamps.getFirst() <= cutoff) {
                timestamps.removeFirst();
            }
            if (timestamps.size() >= limit) {
                return false;
            }
            timestamps.addLast(nowMillis);
            return true;
        }
    }

    public static final class FencedResource {
        private final AtomicLong nextToken = new AtomicLong();
        private long lastAppliedToken;
        private String value;

        public long nextFencingToken() {
            return nextToken.incrementAndGet();
        }

        public synchronized boolean write(long token, String nextValue) {
            if (token <= lastAppliedToken) {
                return false;
            }
            lastAppliedToken = token;
            value = Objects.requireNonNull(nextValue, "nextValue");
            return true;
        }

        public synchronized String value() {
            return value;
        }
    }
}
