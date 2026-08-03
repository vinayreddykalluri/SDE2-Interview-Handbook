package interview.data;

import java.time.Instant;

public record OrderSummary(Long id, String status, Instant createdAt) {
}
