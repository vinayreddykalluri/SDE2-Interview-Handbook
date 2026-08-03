package interview.data;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Version;

import java.time.Instant;
import java.util.Objects;

@Entity
@Table(name = "book_order")
public class OrderEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "request_key", nullable = false, unique = true, length = 80)
    private String requestKey;

    @Column(nullable = false, length = 24)
    private String status;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(nullable = false, length = 200)
    private String note;

    @Version
    private long version;

    protected OrderEntity() {
    }

    public OrderEntity(String requestKey, String status, Instant createdAt,
                       String note) {
        this.requestKey = requireText(requestKey, "requestKey");
        this.status = requireText(status, "status");
        this.createdAt = Objects.requireNonNull(createdAt, "createdAt");
        this.note = requireNote(note);
    }

    public Long getId() {
        return id;
    }

    public String getRequestKey() {
        return requestKey;
    }

    public String getStatus() {
        return status;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public String getNote() {
        return note;
    }

    public long getVersion() {
        return version;
    }

    public void replaceNote(String replacement) {
        note = requireNote(replacement);
    }

    private static String requireText(String value, String name) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(name + " must not be blank");
        }
        return value;
    }

    private static String requireNote(String value) {
        Objects.requireNonNull(value, "note");
        if (value.length() > 200) {
            throw new IllegalArgumentException("note is too long");
        }
        return value;
    }
}
