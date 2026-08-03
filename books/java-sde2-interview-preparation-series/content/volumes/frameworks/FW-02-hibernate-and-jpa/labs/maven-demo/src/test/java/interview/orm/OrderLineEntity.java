package interview.orm;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.SequenceGenerator;
import jakarta.persistence.Table;
import java.util.Objects;

@Entity
@Table(name = "order_line")
public class OrderLineEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "line_sequence")
    @SequenceGenerator(name = "line_sequence", sequenceName = "line_sequence", allocationSize = 20)
    private Long id;

    @Column(name = "sku", nullable = false, length = 40)
    private String sku;

    @Column(nullable = false)
    private int quantity;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "order_id", nullable = false)
    private PurchaseOrderEntity order;

    protected OrderLineEntity() {}

    public OrderLineEntity(String sku, int quantity) {
        this.sku = Objects.requireNonNull(sku, "sku");
        if (quantity <= 0) {
            throw new IllegalArgumentException("quantity must be positive");
        }
        this.quantity = quantity;
    }

    void attachTo(PurchaseOrderEntity order) {
        this.order = Objects.requireNonNull(order, "order");
    }

    void detachFrom(PurchaseOrderEntity expectedOrder) {
        if (order == expectedOrder) {
            order = null;
        }
    }

    public Long getId() {
        return id;
    }

    public String getSku() {
        return sku;
    }

    public PurchaseOrderEntity getOrder() {
        return order;
    }
}
