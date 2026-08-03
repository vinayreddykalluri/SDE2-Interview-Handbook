package interview.orm;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;

import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;
import jakarta.persistence.Persistence;
import jakarta.persistence.RollbackException;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import org.hibernate.SessionFactory;
import org.hibernate.stat.Statistics;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class HibernateJpaBehaviorTest {
    private static EntityManagerFactory factory;
    private static Statistics statistics;

    @BeforeAll
    static void startProvider() {
        factory = Persistence.createEntityManagerFactory("book-lab", Map.of(
                "jakarta.persistence.jdbc.url",
                "jdbc:h2:mem:hibernate-book;MODE=MySQL;DB_CLOSE_DELAY=-1;DATABASE_TO_LOWER=TRUE"));
        statistics = factory.unwrap(SessionFactory.class).getStatistics();
        statistics.setStatisticsEnabled(true);
    }

    @AfterAll
    static void stopProvider() {
        factory.close();
    }

    @BeforeEach
    void clearRowsAndStatistics() {
        inTransaction(entityManager -> {
            entityManager.createQuery("delete from OrderLineEntity").executeUpdate();
            entityManager.createQuery("delete from PurchaseOrderEntity").executeUpdate();
        });
        statistics.clear();
    }

    @Test
    void persistenceContextReturnsOneManagedInstancePerIdentity() {
        long id = createOrder("CREATED", 500, Instant.parse("2026-08-01T10:00:00Z"));
        inTransaction(entityManager -> {
            PurchaseOrderEntity first = entityManager.find(PurchaseOrderEntity.class, id);
            PurchaseOrderEntity second = entityManager.find(PurchaseOrderEntity.class, id);
            assertSame(first, second);
        });
    }

    @Test
    void dirtyCheckingUpdatesAtFlushAndIncrementsVersion() {
        long id = createOrder("CREATED", 500, Instant.parse("2026-08-01T10:00:00Z"));
        statistics.clear();

        inTransaction(entityManager ->
                entityManager.find(PurchaseOrderEntity.class, id).markPaid());

        assertEquals(1L, statistics.getEntityUpdateCount());
        inTransaction(entityManager -> {
            PurchaseOrderEntity reloaded = entityManager.find(PurchaseOrderEntity.class, id);
            assertEquals("PAID", reloaded.getStatus());
            assertEquals(1L, reloaded.getVersion());
        });
    }

    @Test
    void orphanRemovalDeletesThePrivatelyOwnedChild() {
        long id = createOrderWithLine("sku-one");
        inTransaction(entityManager -> {
            PurchaseOrderEntity order = entityManager.find(PurchaseOrderEntity.class, id);
            OrderLineEntity line = order.getLines().getFirst();
            order.removeLine(line);
        });

        inTransaction(entityManager -> assertEquals(
                0L,
                entityManager.createQuery("select count(l) from OrderLineEntity l", Long.class)
                        .getSingleResult()));
    }

    @Test
    void fetchJoinUsesOneStatementWhereLazyTraversalUsesNPlusOne() {
        createOrderWithLine("sku-one");
        createOrderWithLine("sku-two");
        statistics.clear();

        try (EntityManager entityManager = factory.createEntityManager()) {
            List<PurchaseOrderEntity> orders = entityManager
                    .createQuery("select o from PurchaseOrderEntity o order by o.id", PurchaseOrderEntity.class)
                    .getResultList();
            orders.forEach(order -> assertEquals(1, order.getLines().size()));
        }
        assertEquals(3L, statistics.getPrepareStatementCount());

        statistics.clear();
        try (EntityManager entityManager = factory.createEntityManager()) {
            List<PurchaseOrderEntity> orders = entityManager.createQuery("""
                    select distinct o
                    from PurchaseOrderEntity o
                    left join fetch o.lines
                    order by o.id
                    """, PurchaseOrderEntity.class).getResultList();
            orders.forEach(order -> assertEquals(1, order.getLines().size()));
        }
        assertEquals(1L, statistics.getPrepareStatementCount());
    }

    @Test
    void versionCheckRejectsAStalePersistenceContext() {
        long id = createOrder("CREATED", 500, Instant.parse("2026-08-01T10:00:00Z"));

        try (EntityManager firstManager = factory.createEntityManager();
             EntityManager staleManager = factory.createEntityManager()) {
            firstManager.getTransaction().begin();
            staleManager.getTransaction().begin();
            PurchaseOrderEntity first = firstManager.find(PurchaseOrderEntity.class, id);
            PurchaseOrderEntity stale = staleManager.find(PurchaseOrderEntity.class, id);

            first.markPaid();
            firstManager.getTransaction().commit();

            stale.markPaid();
            assertThrows(RollbackException.class, staleManager.getTransaction()::commit);
        }
    }

    @Test
    void bulkDmlLeavesManagedStateStaleUntilClear() {
        long id = createOrder("CREATED", 500, Instant.parse("2026-08-01T10:00:00Z"));

        inTransaction(entityManager -> {
            PurchaseOrderEntity managed = entityManager.find(PurchaseOrderEntity.class, id);
            int changed = entityManager.createQuery("""
                    update PurchaseOrderEntity o
                    set o.status = 'EXPIRED'
                    where o.id = :id
                    """).setParameter("id", id).executeUpdate();
            assertEquals(1, changed);
            assertEquals("CREATED", managed.getStatus());

            entityManager.clear();
            assertEquals("EXPIRED",
                    entityManager.find(PurchaseOrderEntity.class, id).getStatus());
        });
    }

    @Test
    void dtoLikeProjectionAndLimitAvoidManagedGraphLoading() {
        createOrder("CREATED", 100, Instant.parse("2026-08-01T10:00:00Z"));
        long newest = createOrder("PAID", 200, Instant.parse("2026-08-03T10:00:00Z"));
        createOrder("PAID", 150, Instant.parse("2026-08-02T10:00:00Z"));
        statistics.clear();

        try (EntityManager entityManager = factory.createEntityManager()) {
            List<Object[]> rows = entityManager.createQuery("""
                    select o.id, o.status
                    from PurchaseOrderEntity o
                    order by o.createdAt desc, o.id desc
                    """, Object[].class).setMaxResults(2).getResultList();
            assertEquals(2, rows.size());
            assertEquals(newest, rows.getFirst()[0]);
            assertEquals(0L, statistics.getEntityLoadCount());
        }
    }

    private static long createOrder(String status, long totalCents, Instant createdAt) {
        long[] id = new long[1];
        inTransaction(entityManager -> {
            PurchaseOrderEntity order = new PurchaseOrderEntity(status, totalCents, createdAt);
            entityManager.persist(order);
            entityManager.flush();
            id[0] = order.getId();
        });
        return id[0];
    }

    private static long createOrderWithLine(String sku) {
        long[] id = new long[1];
        inTransaction(entityManager -> {
            PurchaseOrderEntity order = new PurchaseOrderEntity(
                    "CREATED", 500, Instant.parse("2026-08-01T10:00:00Z"));
            order.addLine(new OrderLineEntity(sku, 1));
            entityManager.persist(order);
            entityManager.flush();
            id[0] = order.getId();
        });
        return id[0];
    }

    private static void inTransaction(EntityWork work) {
        try (EntityManager entityManager = factory.createEntityManager()) {
            entityManager.getTransaction().begin();
            try {
                work.accept(entityManager);
                entityManager.getTransaction().commit();
            } catch (RuntimeException failure) {
                if (entityManager.getTransaction().isActive()) {
                    entityManager.getTransaction().rollback();
                }
                throw failure;
            }
        }
    }

    @FunctionalInterface
    private interface EntityWork {
        void accept(EntityManager entityManager);
    }
}
