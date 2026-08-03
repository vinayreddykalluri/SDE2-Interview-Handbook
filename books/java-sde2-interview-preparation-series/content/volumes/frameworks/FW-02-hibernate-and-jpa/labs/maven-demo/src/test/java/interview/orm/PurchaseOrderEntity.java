package interview.orm;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OrderBy;
import jakarta.persistence.SequenceGenerator;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

@Entity
@Table(name = "purchase_order")
public class PurchaseOrderEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "order_sequence")
    @SequenceGenerator(name = "order_sequence", sequenceName = "order_sequence", allocationSize = 20)
    private Long id;

    @Column(nullable = false, length = 20)
    private String status;

    @Column(name = "total_cents", nullable = false)
    private long totalCents;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Version
    private long version;

    @OneToMany(mappedBy = "order", fetch = FetchType.LAZY,
            cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("id ASC")
    private List<OrderLineEntity> lines = new ArrayList<>();

    protected PurchaseOrderEntity() {}

    public PurchaseOrderEntity(String status, long totalCents, Instant createdAt) {
        this.status = Objects.requireNonNull(status, "status");
        this.totalCents = totalCents;
        this.createdAt = Objects.requireNonNull(createdAt, "createdAt");
    }

    public void addLine(OrderLineEntity line) {
        lines.add(Objects.requireNonNull(line, "line"));
        line.attachTo(this);
    }

    public void removeLine(OrderLineEntity line) {
        if (lines.remove(line)) {
            line.detachFrom(this);
        }
    }

    public void markPaid() {
        status = "PAID";
    }

    public Long getId() {
        return id;
    }

    public String getStatus() {
        return status;
    }

    public long getVersion() {
        return version;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public List<OrderLineEntity> getLines() {
        return lines;
    }
}
