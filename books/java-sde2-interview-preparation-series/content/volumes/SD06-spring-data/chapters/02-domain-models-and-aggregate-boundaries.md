# Domain Models, Aggregate Boundaries, and Repository Interfaces

Before writing repository methods, define what your aggregate owns and what it guarantees. A repository should protect the aggregate root contract, not leak transport details.

## Core model

A domain aggregate in an interview-safe model has:

- **Identity:** immutable or validated key type.
- **Invariant checks:** invariants enforced in behavior.
- **Boundary methods:** explicit state transitions.
- **Persistence intent:** what may be read, written, and in which transaction scope.

## Repository shape that supports invariants

Prefer focused methods:

- `save(Order order)`
- `findByOrderIdWithLines(Long orderId)` (if needed by boundary)
- `existsByReferenceNumber(String referenceNumber)`

Avoid exposing broad internals with generic methods:

- `findAll()` with no filters,
- `updateBy...` bypassing aggregate behavior,
- `saveAll` without validation.

## Example with behavior contract

```java
interface OrderRepository {
    Order getByOrderId(UUID id);         // throws if not found
    Optional<Order> findByOrderId(UUID id);
    boolean isReferenceNumberTaken(String referenceNumber);
    void reserveForAccount(UUID accountId, UUID productId, int quantity);
}
```

`reserveForAccount` is a domain command, not a data dump method.

## Why this helps interviews

Interviewers often ask:

- How do you prevent partial state changes?
- Why not expose only one generic save method?
- How does persistence stay aligned to invariants?

These are answered by showing domain boundaries, not just SQL knowledge.

## Quick check

1. What is an aggregate boundary in plain terms?
2. Why is a command-oriented repository method safer than a generic update helper?
3. How do repository contracts prevent repeated missing validation?

## Debugging exercise

Given `OrderRepository.save(Order)` and `OrderService.markShipped(Order)`:

- Find the issue if `markShipped` directly toggles status in multiple places.
- Add one rule that makes this method resilient to duplicate transitions.

Expected answer: centralize transition checks before state write and keep repository methods narrow.

## Practice

- **Foundation:** Write three repository method names for one `Product` aggregate.
- **Interview Core:** Classify one method as query, command, and validation boundary.
- **SDE-2 Follow-up:** Explain why a single generic repository method can become a bug hotspot.
