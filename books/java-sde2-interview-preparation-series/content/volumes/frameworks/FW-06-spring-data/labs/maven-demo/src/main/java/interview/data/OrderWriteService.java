package interview.data;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.Objects;

@Service
public class OrderWriteService {
    private final OrderRepository orders;

    public OrderWriteService(OrderRepository orders) {
        this.orders = Objects.requireNonNull(orders, "orders");
    }

    @Transactional
    public long create(String requestKey, String status, Instant createdAt,
                       String note) {
        OrderEntity saved = orders.saveAndFlush(
                new OrderEntity(requestKey, status, createdAt, note));
        return saved.getId();
    }

    @Transactional
    public void replaceNote(long id, String note) {
        OrderEntity order = orders.findById(id).orElseThrow();
        order.replaceNote(note);
    }

    @Transactional
    public void replaceNoteThenFail(long id, String note) {
        replaceNote(id, note);
        throw new IllegalStateException("force rollback");
    }

    @Transactional
    public void saveDetachedWithNote(OrderEntity detached, String note) {
        detached.replaceNote(note);
        orders.saveAndFlush(detached);
    }
}
