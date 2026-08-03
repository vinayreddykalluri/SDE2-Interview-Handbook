package interview.data;

import jakarta.persistence.LockModeType;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

public interface OrderRepository extends JpaRepository<OrderEntity, Long> {
    Optional<OrderEntity> findByRequestKey(String requestKey);

    boolean existsByRequestKey(String requestKey);

    Slice<OrderSummary> findByStatusOrderByCreatedAtDescIdDesc(
            String status, Pageable pageable);

    @Query("""
            select new interview.data.OrderSummary(o.id, o.status, o.createdAt)
            from OrderEntity o
            where o.status = :status
              and (:afterCreatedAt is null
                   or o.createdAt < :afterCreatedAt
                   or (o.createdAt = :afterCreatedAt and o.id < :afterId))
            order by o.createdAt desc, o.id desc
            """)
    List<OrderSummary> findNextWindow(
            @Param("status") String status,
            @Param("afterCreatedAt") Instant afterCreatedAt,
            @Param("afterId") Long afterId,
            Pageable pageable);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select o from OrderEntity o where o.id = :id")
    Optional<OrderEntity> findForUpdateById(@Param("id") long id);
}
