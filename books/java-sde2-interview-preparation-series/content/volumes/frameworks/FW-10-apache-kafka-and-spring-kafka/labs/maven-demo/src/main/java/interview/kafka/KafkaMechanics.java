package interview.kafka;

import java.util.HashSet;
import java.util.Set;
import java.util.TreeSet;

public final class KafkaMechanics {
    private KafkaMechanics() {}

    public static final class ContiguousOffsetTracker {
        private final TreeSet<Long> completed = new TreeSet<>();
        private long nextCommitOffset;

        public ContiguousOffsetTracker(long firstOffset) {
            if (firstOffset < 0) {
                throw new IllegalArgumentException("offset must be nonnegative");
            }
            nextCommitOffset = firstOffset;
        }

        public synchronized long complete(long processedOffset) {
            if (processedOffset >= nextCommitOffset) {
                completed.add(processedOffset);
            }
            while (completed.remove(nextCommitOffset)) {
                nextCommitOffset++;
            }
            return nextCommitOffset;
        }
    }

    public static final class IdempotentInbox {
        private final Set<String> ids = new HashSet<>();

        public synchronized boolean claim(String eventId) {
            return ids.add(eventId);
        }
    }
}
