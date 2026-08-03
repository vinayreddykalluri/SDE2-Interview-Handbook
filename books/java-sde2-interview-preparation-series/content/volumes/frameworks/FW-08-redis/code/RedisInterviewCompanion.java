import java.nio.charset.StandardCharsets;
import java.util.Objects;

/** Dependency-free executable reasoning models for the Redis volume. */
public final class RedisInterviewCompanion {
    private RedisInterviewCompanion() {}

    enum Structure { STRING, HASH, SET, SORTED_SET, STREAM }

    record Requirement(boolean uniqueMembers, boolean scoreOrdered,
                       boolean fieldUpdates, boolean replayableLog) {}

    static Structure chooseStructure(Requirement requirement) {
        Objects.requireNonNull(requirement, "requirement");
        if (requirement.replayableLog()) {
            return Structure.STREAM;
        }
        if (requirement.scoreOrdered()) {
            return Structure.SORTED_SET;
        }
        if (requirement.uniqueMembers()) {
            return Structure.SET;
        }
        return requirement.fieldUpdates() ? Structure.HASH : Structure.STRING;
    }

    static long deterministicTtlSeconds(String key, long base, long jitter) {
        Objects.requireNonNull(key, "key");
        if (base <= 0 || jitter < 0) {
            throw new IllegalArgumentException("invalid TTL");
        }
        long offset = jitter == 0 ? 0 : Math.floorMod(key.hashCode(), jitter + 1);
        return Math.addExact(base, offset);
    }

    record CacheValue(String payload, long sourceVersion, long logicalExpiresAtMillis) {
        CacheValue {
            Objects.requireNonNull(payload, "payload");
        }

        boolean freshAt(long nowMillis) {
            return nowMillis < logicalExpiresAtMillis;
        }
    }

    static String clusterHashTag(String key) {
        Objects.requireNonNull(key, "key");
        int open = key.indexOf('{');
        if (open < 0) {
            return key;
        }
        int close = key.indexOf('}', open + 1);
        return close > open + 1 ? key.substring(open + 1, close) : key;
    }

    record Lease(String ownerToken, long expiresAtMillis, long fencingToken) {
        Lease {
            Objects.requireNonNull(ownerToken, "ownerToken");
        }

        boolean ownedBy(String token, long nowMillis) {
            return ownerToken.equals(token) && nowMillis < expiresAtMillis;
        }
    }

    static int encodedUtf8Length(String value) {
        return value.getBytes(StandardCharsets.UTF_8).length;
    }

    public static void main(String[] args) {
        assert chooseStructure(new Requirement(true, false, false, false)) == Structure.SET;
        assert chooseStructure(new Requirement(false, true, false, false)) == Structure.SORTED_SET;
        assert chooseStructure(new Requirement(false, false, false, true)) == Structure.STREAM;

        long ttl = deterministicTtlSeconds("order:42", 300, 30);
        assert ttl >= 300 && ttl <= 330;

        CacheValue value = new CacheValue("paid", 4, 1_000);
        assert value.freshAt(999);
        assert !value.freshAt(1_000);

        assert clusterHashTag("order:{42}:items").equals("42");
        assert clusterHashTag("ordinary-key").equals("ordinary-key");

        Lease lease = new Lease("owner-a", 500, 7);
        assert lease.ownedBy("owner-a", 499);
        assert !lease.ownedBy("owner-b", 499);
        assert !lease.ownedBy("owner-a", 500);
        assert encodedUtf8Length("\u20B9") == 3;

        System.out.println("RedisInterviewCompanion checks passed");
    }
}
