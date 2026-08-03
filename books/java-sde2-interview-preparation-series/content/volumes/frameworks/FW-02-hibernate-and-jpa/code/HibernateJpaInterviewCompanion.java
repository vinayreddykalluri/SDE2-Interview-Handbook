import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/** Dependency-free executable models for the Hibernate/JPA interview volume. */
public final class HibernateJpaInterviewCompanion {
    private HibernateJpaInterviewCompanion() {}

    enum Lifecycle {
        NEW, MANAGED, DETACHED, REMOVED
    }

    static Lifecycle persist(Lifecycle state) {
        if (state != Lifecycle.NEW) {
            throw new IllegalStateException("persist expects a new instance");
        }
        return Lifecycle.MANAGED;
    }

    static Lifecycle detach(Lifecycle state) {
        return state == Lifecycle.MANAGED || state == Lifecycle.REMOVED
                ? Lifecycle.DETACHED : state;
    }

    record EntityKey(Class<?> type, long id) {
        EntityKey {
            Objects.requireNonNull(type, "type");
        }
    }

    static final class IdentityMap {
        private final Map<EntityKey, Object> managed = new HashMap<>();

        <T> T manage(EntityKey key, T entity) {
            Object existing = managed.putIfAbsent(key, entity);
            @SuppressWarnings("unchecked")
            T canonical = (T) (existing == null ? entity : existing);
            return canonical;
        }

        void clear() {
            managed.clear();
        }
    }

    record Snapshot(String status, long totalCents) {}

    static boolean isDirty(Snapshot loaded, Snapshot current) {
        return !Objects.equals(loaded, current);
    }

    static final class Order {
        private final long id;
        private final List<Line> lines = new ArrayList<>();

        Order(long id) {
            this.id = id;
        }

        void add(Line line) {
            Objects.requireNonNull(line, "line");
            lines.add(line);
            line.order = this;
        }

        void remove(Line line) {
            if (lines.remove(line)) {
                line.order = null;
            }
        }
    }

    static final class Line {
        private Order order;
    }

    record VersionedState(String status, long version) {
        VersionedState update(String nextStatus, long expectedVersion) {
            return version == expectedVersion
                    ? new VersionedState(Objects.requireNonNull(nextStatus), version + 1)
                    : this;
        }
    }

    static <T> List<List<T>> batches(List<T> input, int batchSize) {
        if (batchSize <= 0) {
            throw new IllegalArgumentException("batchSize must be positive");
        }
        List<List<T>> result = new ArrayList<>();
        for (int start = 0; start < input.size(); start += batchSize) {
            result.add(List.copyOf(input.subList(start, Math.min(start + batchSize, input.size()))));
        }
        return List.copyOf(result);
    }

    public static void main(String[] args) {
        assert persist(Lifecycle.NEW) == Lifecycle.MANAGED;
        assert detach(Lifecycle.MANAGED) == Lifecycle.DETACHED;

        IdentityMap context = new IdentityMap();
        EntityKey key = new EntityKey(Order.class, 7);
        Order first = context.manage(key, new Order(7));
        Order second = context.manage(key, new Order(7));
        assert first == second;

        assert !isDirty(new Snapshot("CREATED", 500), new Snapshot("CREATED", 500));
        assert isDirty(new Snapshot("CREATED", 500), new Snapshot("PAID", 500));

        Line line = new Line();
        first.add(line);
        assert line.order == first && first.lines.size() == 1;
        first.remove(line);
        assert line.order == null && first.lines.isEmpty();

        VersionedState state = new VersionedState("CREATED", 2);
        assert state.update("PAID", 1).equals(state);
        assert state.update("PAID", 2).version() == 3;

        assert batches(List.of(1, 2, 3, 4, 5), 2).equals(
                List.of(List.of(1, 2), List.of(3, 4), List.of(5)));

        context.clear();
        System.out.println("HibernateJpaInterviewCompanion checks passed");
    }
}
