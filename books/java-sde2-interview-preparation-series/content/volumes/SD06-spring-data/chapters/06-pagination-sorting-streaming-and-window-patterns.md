# Pagination, Sorting, Streaming, and Window Patterns

Pagination prevents loading everything. It also adds complexity in ordering and cursor correctness.

## Base model

For interview-safe pagination, define:

- page size,
- sort fields,
- deterministic tie-breaker,
- no accidental duplicate overlap,
- bounded response contract.

## Offset vs cursor

Offset pagination is easier but can be expensive and unstable with deletes/inserts.
Cursor pagination is better for deep scrolling and real-time data.

```text
Offset pagination:
SELECT ... LIMIT 20 OFFSET 40

Cursor pagination:
WHERE (created_at, id) > (:cursorCreatedAt, :cursorId)
ORDER BY created_at, id
LIMIT 20
```

## Practical rules

- Always use deterministic secondary sort (for example `createdAt DESC, id DESC`).
- Keep sort keys aligned with supporting indexes.
- Return explicit page metadata (`hasNext`, `nextCursor`) from the service contract.

## Quick check

1. Why is `ORDER BY created_at DESC` alone often insufficient?
2. Where do offset-based pages become unreliable?
3. How does cursor pagination change resume semantics after deletes?

## Debugging exercise

An API uses `findTop20ByOrderByCreatedAtDesc`. At page 2, users sometimes see duplicates after new inserts.

Why this happens and what fix you apply first.

Expected answer: deterministic tie-breakers and cursor strategy aligned with stable order.

## Practice

- **Foundation:** Draw offset vs cursor for 12 records and one insert between page reads.
- **Interview Core:** Write a safe page contract for a `GET /orders` endpoint.
- **SDE-2 Follow-up:** Define failure cases for streaming repository calls in high concurrency.
