import java.util.HashSet;
import java.util.Objects;
import java.util.Set;
import java.util.TreeSet;

/** Dependency-free executable reasoning models for Kafka and Spring Kafka. */
public final class KafkaInterviewCompanion {
    private KafkaInterviewCompanion() {}

    record RecordId(String topic, int partition, long offset) {
        RecordId {
            Objects.requireNonNull(topic, "topic");
            if (partition < 0 || offset < 0) {
                throw new IllegalArgumentException("negative partition or offset");
            }
        }
    }

    static final class ContiguousCommitTracker {
        private final TreeSet<Long> completed = new TreeSet<>();
        private long nextOffset;

        ContiguousCommitTracker(long firstOffset) {
            nextOffset = firstOffset;
        }

        long complete(long offset) {
            if (offset >= nextOffset) {
                completed.add(offset);
            }
            while (completed.remove(nextOffset)) {
                nextOffset++;
            }
            return nextOffset;
        }
    }

    static final class Inbox {
        private final Set<String> processedEventIds = new HashSet<>();
        private long totalCents;

        boolean apply(String eventId, long deltaCents) {
            Objects.requireNonNull(eventId, "eventId");
            if (!processedEventIds.add(eventId)) {
                return false;
            }
            totalCents = Math.addExact(totalCents, deltaCents);
            return true;
        }
    }

    enum FailureKind { TRANSIENT, PERMANENT_DATA, CODE_BUG, UNKNOWN_SIDE_EFFECT }
    enum Handling { RETRY_WITH_BACKOFF, QUARANTINE, STOP_AND_ALERT, RECONCILE }

    static Handling handling(FailureKind failure) {
        return switch (failure) {
            case TRANSIENT -> Handling.RETRY_WITH_BACKOFF;
            case PERMANENT_DATA -> Handling.QUARANTINE;
            case CODE_BUG -> Handling.STOP_AND_ALERT;
            case UNKNOWN_SIDE_EFFECT -> Handling.RECONCILE;
        };
    }

    static int requiredConsumers(int partitions, int desiredConcurrency) {
        if (partitions <= 0 || desiredConcurrency <= 0) {
            throw new IllegalArgumentException("positive values required");
        }
        return Math.min(partitions, desiredConcurrency);
    }

    record AggregateEvent(String aggregateId, long aggregateVersion) {
        AggregateEvent {
            Objects.requireNonNull(aggregateId, "aggregateId");
        }

        boolean isNextAfter(long appliedVersion) {
            return aggregateVersion == appliedVersion + 1;
        }
    }

    public static void main(String[] args) {
        ContiguousCommitTracker tracker = new ContiguousCommitTracker(10);
        assert tracker.complete(11) == 10;
        assert tracker.complete(10) == 12;
        assert tracker.complete(13) == 12;
        assert tracker.complete(12) == 14;

        Inbox inbox = new Inbox();
        assert inbox.apply("event-1", 500);
        assert !inbox.apply("event-1", 500);
        assert inbox.totalCents == 500;

        assert handling(FailureKind.TRANSIENT) == Handling.RETRY_WITH_BACKOFF;
        assert handling(FailureKind.UNKNOWN_SIDE_EFFECT) == Handling.RECONCILE;
        assert requiredConsumers(6, 10) == 6;
        assert new AggregateEvent("order-42", 4).isNextAfter(3);

        RecordId record = new RecordId("orders", 2, 25);
        assert record.offset() + 1 == 26;
        System.out.println("KafkaInterviewCompanion checks passed");
    }
}
