package interview.data;

import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;
import jakarta.persistence.LockModeType;
import org.hibernate.SessionFactory;
import org.hibernate.stat.Statistics;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest(properties = {
        "spring.datasource.url=jdbc:h2:mem:spring_data_book;DB_CLOSE_DELAY=-1",
        "spring.jpa.hibernate.ddl-auto=create-drop",
        "spring.jpa.open-in-view=false",
        "spring.jpa.properties.hibernate.generate_statistics=true"
})
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
class SpringDataJpaBehaviorTest {
    @Autowired
    private OrderRepository orders;

    @Autowired
    private OrderWriteService writes;

    @Autowired
    private EntityManager entityManager;

    @Autowired
    private EntityManagerFactory entityManagerFactory;

    @BeforeEach
    void clearRows() {
        orders.deleteAllInBatch();
    }

    @Test
    void sliceUsesDeterministicTieBreakerWithoutCountQuery() {
        Instant time = Instant.parse("2026-01-01T12:00:00Z");
        long older = writes.create("older", "OPEN", time.minusSeconds(1), "");
        long firstTie = writes.create("tie-1", "OPEN", time, "");
        long secondTie = writes.create("tie-2", "OPEN", time, "");

        Statistics statistics = entityManagerFactory
                .unwrap(SessionFactory.class).getStatistics();
        statistics.clear();

        var first = orders.findByStatusOrderByCreatedAtDescIdDesc(
                "OPEN", org.springframework.data.domain.PageRequest.of(0, 2));

        assertThat(first.getContent())
                .extracting(OrderSummary::id)
                .containsExactly(secondTie, firstTie);
        assertThat(first.hasNext()).isTrue();
        assertThat(first.getContent()).extracting(OrderSummary::id)
                .doesNotContain(older);
        assertThat(statistics.getPrepareStatementCount()).isEqualTo(1);
    }

    @Test
    void keysetWindowContinuesFromTimestampAndIdWithoutOffset() {
        Instant time = Instant.parse("2026-01-01T12:00:00Z");
        long older = writes.create("cursor-old", "OPEN", time.minusSeconds(1), "");
        long firstTie = writes.create("cursor-1", "OPEN", time, "");
        long secondTie = writes.create("cursor-2", "OPEN", time, "");

        var limit = org.springframework.data.domain.PageRequest.of(0, 2);
        List<OrderSummary> first = orders.findNextWindow(
                "OPEN", null, null, limit);
        OrderSummary cursor = first.getLast();
        List<OrderSummary> next = orders.findNextWindow(
                "OPEN", cursor.createdAt(), cursor.id(), limit);

        assertThat(first).extracting(OrderSummary::id)
                .containsExactly(secondTie, firstTie);
        assertThat(next).extracting(OrderSummary::id)
                .containsExactly(older);
    }

    @Test
    void serviceExceptionRollsBackManagedMutation() {
        long id = writes.create("rollback", "OPEN", Instant.now(), "before");

        assertThatThrownBy(() -> writes.replaceNoteThenFail(id, "after"))
                .isInstanceOf(IllegalStateException.class);

        assertThat(orders.findById(id).orElseThrow().getNote())
                .isEqualTo("before");
    }

    @Test
    void uniqueConstraintIsObservedAtFlushBoundary() {
        Instant now = Instant.now();
        writes.create("duplicate", "OPEN", now, "first");

        assertThatThrownBy(() -> writes.create(
                "duplicate", "OPEN", now, "second"))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void staleDetachedEntityRaisesOptimisticConflict() {
        long id = writes.create("optimistic", "OPEN", Instant.now(), "v0");
        OrderEntity stale = orders.findById(id).orElseThrow();

        writes.replaceNote(id, "v1");

        assertThatThrownBy(() -> writes.saveDetachedWithNote(stale, "stale"))
                .isInstanceOf(ObjectOptimisticLockingFailureException.class);
        assertThat(orders.findById(id).orElseThrow().getNote()).isEqualTo("v1");
    }

    @Test
    @Transactional
    void pessimisticRepositoryMethodAppliesJpaLockMode() {
        long id = writes.create("lock", "OPEN", Instant.now(), "");
        entityManager.clear();

        OrderEntity locked = orders.findForUpdateById(id).orElseThrow();

        assertThat(entityManager.getLockMode(locked))
                .isEqualTo(LockModeType.PESSIMISTIC_WRITE);
    }

    @Test
    void existenceAndCountRemainDifferentQuestions() {
        writes.create("exists", "OPEN", Instant.now(), "");

        assertThat(orders.existsByRequestKey("exists")).isTrue();
        assertThat(orders.existsByRequestKey("missing")).isFalse();
        assertThat(orders.count()).isEqualTo(1);
    }
}
