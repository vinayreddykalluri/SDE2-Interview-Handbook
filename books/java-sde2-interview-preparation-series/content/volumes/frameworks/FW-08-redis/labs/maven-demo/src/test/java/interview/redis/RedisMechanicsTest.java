package interview.redis;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.charset.StandardCharsets;
import java.util.List;
import org.junit.jupiter.api.Test;

class RedisMechanicsTest {
    @Test
    void respEncoderUsesUtf8ByteLengthRatherThanCharacterCount() {
        String encoded = new String(
                RedisMechanics.encodeResp2Command(List.of("SET", "currency", "₹")),
                StandardCharsets.UTF_8);
        assertEquals("*3\r\n$3\r\nSET\r\n$8\r\ncurrency\r\n$3\r\n₹\r\n", encoded);
    }

    @Test
    void ttlExpiresAtTheBoundary() {
        RedisMechanics.ManualClock clock = new RedisMechanics.ManualClock(1_000);
        RedisMechanics.ExpiringStore store = new RedisMechanics.ExpiringStore(clock);
        assertTrue(store.setNxPx("session", "value", 500));
        clock.advance(499);
        assertEquals("value", store.get("session"));
        clock.advance(1);
        assertNull(store.get("session"));
    }

    @Test
    void nxRejectsOverwriteUntilLeaseExpires() {
        RedisMechanics.ManualClock clock = new RedisMechanics.ManualClock(0);
        RedisMechanics.ExpiringStore store = new RedisMechanics.ExpiringStore(clock);
        assertTrue(store.setNxPx("lock", "owner-a", 10));
        assertFalse(store.setNxPx("lock", "owner-b", 10));
        clock.advance(10);
        assertTrue(store.setNxPx("lock", "owner-b", 10));
    }

    @Test
    void compareAndDeleteCannotReleaseAnotherOwnersLease() {
        RedisMechanics.ManualClock clock = new RedisMechanics.ManualClock(0);
        RedisMechanics.ExpiringStore store = new RedisMechanics.ExpiringStore(clock);
        store.setNxPx("lock", "owner-a", 10);
        clock.advance(10);
        store.setNxPx("lock", "owner-b", 10);

        assertFalse(store.compareAndDelete("lock", "owner-a"));
        assertEquals("owner-b", store.get("lock"));
        assertTrue(store.compareAndDelete("lock", "owner-b"));
    }

    @Test
    void slidingWindowRejectsFourthRequestAndReleasesBoundary() {
        RedisMechanics.SlidingWindowLimiter limiter = new RedisMechanics.SlidingWindowLimiter(3, 1_000);
        assertTrue(limiter.allow("tenant", 0));
        assertTrue(limiter.allow("tenant", 100));
        assertTrue(limiter.allow("tenant", 200));
        assertFalse(limiter.allow("tenant", 999));
        assertTrue(limiter.allow("tenant", 1_000));
    }

    @Test
    void rateLimitsAreScopedByKey() {
        RedisMechanics.SlidingWindowLimiter limiter = new RedisMechanics.SlidingWindowLimiter(1, 1_000);
        assertTrue(limiter.allow("tenant-a", 0));
        assertFalse(limiter.allow("tenant-a", 1));
        assertTrue(limiter.allow("tenant-b", 1));
    }

    @Test
    void fencingRejectsAStaleResumedOwner() {
        RedisMechanics.FencedResource resource = new RedisMechanics.FencedResource();
        long oldToken = resource.nextFencingToken();
        long newToken = resource.nextFencingToken();
        assertTrue(resource.write(newToken, "new-owner"));
        assertFalse(resource.write(oldToken, "stale-owner"));
        assertEquals("new-owner", resource.value());
    }
}
