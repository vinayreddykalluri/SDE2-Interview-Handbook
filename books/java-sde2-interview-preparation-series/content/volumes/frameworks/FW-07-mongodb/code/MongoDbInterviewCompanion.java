import java.time.Instant;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;

/** Dependency-free executable reasoning models for the MongoDB volume. */
public final class MongoDbInterviewCompanion {
    private MongoDbInterviewCompanion() {}

    enum Boundary { EMBED, REFERENCE }

    record Relationship(boolean owned, boolean bounded, boolean readTogether,
                        boolean independentlyQueried) {}

    static Boundary chooseBoundary(Relationship relationship) {
        Objects.requireNonNull(relationship, "relationship");
        return relationship.owned()
                && relationship.bounded()
                && relationship.readTogether()
                && !relationship.independentlyQueried()
                ? Boundary.EMBED : Boundary.REFERENCE;
    }

    record VersionedDocument(String status, long version) {
        VersionedDocument transition(String requiredStatus, long expectedVersion, String nextStatus) {
            return status.equals(requiredStatus) && version == expectedVersion
                    ? new VersionedDocument(Objects.requireNonNull(nextStatus), version + 1)
                    : this;
        }
    }

    record Cursor(Instant createdAt, String objectIdHex) {
        Cursor {
            Objects.requireNonNull(createdAt, "createdAt");
            Objects.requireNonNull(objectIdHex, "objectIdHex");
        }
    }

    static final Comparator<Cursor> NEWEST_FIRST = Comparator
            .comparing(Cursor::createdAt, Comparator.reverseOrder())
            .thenComparing(Cursor::objectIdHex, Comparator.reverseOrder());

    static boolean comesAfter(Cursor candidate, Cursor boundary) {
        return NEWEST_FIRST.compare(candidate, boundary) > 0;
    }

    enum RetryAction { DO_NOT_RETRY, RETRY_TRANSACTION, RETRY_COMMIT, RECONCILE }

    static RetryAction classify(Set<String> errorLabels, boolean writeOutcomeKnown) {
        Set<String> labels = Set.copyOf(errorLabels);
        if (labels.contains("UnknownTransactionCommitResult")) {
            return RetryAction.RETRY_COMMIT;
        }
        if (labels.contains("TransientTransactionError")) {
            return RetryAction.RETRY_TRANSACTION;
        }
        return writeOutcomeKnown ? RetryAction.DO_NOT_RETRY : RetryAction.RECONCILE;
    }

    static final class IdempotentProjection {
        private final Set<String> appliedEventIds = new HashSet<>();
        private long totalCents;

        boolean apply(String eventId, long deltaCents) {
            Objects.requireNonNull(eventId, "eventId");
            if (!appliedEventIds.add(eventId)) {
                return false;
            }
            totalCents = Math.addExact(totalCents, deltaCents);
            return true;
        }
    }

    public static void main(String[] args) {
        assert chooseBoundary(new Relationship(true, true, true, false)) == Boundary.EMBED;
        assert chooseBoundary(new Relationship(true, false, true, false)) == Boundary.REFERENCE;

        VersionedDocument document = new VersionedDocument("CREATED", 3);
        assert document.transition("CREATED", 2, "PAID").equals(document);
        assert document.transition("CREATED", 3, "PAID").version() == 4;

        Instant tied = Instant.parse("2026-08-02T10:00:00Z");
        Cursor boundary = new Cursor(tied, "0000000000000000000000ff");
        assert comesAfter(new Cursor(tied, "0000000000000000000000fe"), boundary);

        assert classify(Set.of("TransientTransactionError"), true)
                == RetryAction.RETRY_TRANSACTION;
        assert classify(Set.of("UnknownTransactionCommitResult"), false)
                == RetryAction.RETRY_COMMIT;
        assert classify(Set.of(), false) == RetryAction.RECONCILE;

        IdempotentProjection projection = new IdempotentProjection();
        assert projection.apply("event-1", 500);
        assert !projection.apply("event-1", 500);
        assert projection.totalCents == 500;

        System.out.println("MongoDbInterviewCompanion checks passed");
    }
}
