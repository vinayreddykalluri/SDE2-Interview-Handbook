# Pagination, Sorting, Streaming, and Window Patterns

Pagination prevents loading everything. It also adds complexity in ordering and cursor correctness.

## Base model

For interview-safe pagination, define:

- page size,
- sort fields,
- deterministic tie-breaker,
- no accidental duplicate overlap,
- bounded response contract.

## Choose the return contract deliberately

| Contract | What it promises | Hidden/extra work |
|---|---|---|
| `List<T>` with `Pageable` | one bounded list | no total/continuation metadata unless application adds it |
| `Slice<T>` | content plus whether another slice exists | commonly fetches enough rows to detect continuation; no total count promise |
| `Page<T>` | content, page metadata, and total | can execute a separate count query |
| `Window<T>` | a scroll window and continuation position | requires stable scroll semantics and supported repository method shape |
| `Stream<T>` | incremental consumption | transaction, connection, and stream must remain open and be closed deterministically |

Use a `Page` only when the product needs an exact total. On a large filtered relation, counting can cost more than fetching one result window.

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

These are separate models:

```text
offset state = page number or numeric offset
cursor state = last ordered key tuple, for example (createdAt, id)
```

Do not advance both an offset and a cursor. A cursor is bound to normalized filters, sort direction, tenant/authorization scope, and version. If the client changes those inputs, reject or restart the traversal.

For descending `(created_at, id)` order, continuation is:

```sql
where created_at < :last_created_at
   or (created_at = :last_created_at and id < :last_id)
order by created_at desc, id desc
limit :limit_plus_one;
```

Mixed ascending/descending fields need a carefully derived lexicographic predicate; do not copy a tuple comparison blindly.

## Practical rules

- Always use deterministic secondary sort (for example `createdAt DESC, id DESC`).
- Keep sort keys aligned with supporting indexes.
- Return explicit page metadata (`hasNext`, `nextCursor`) from the service contract.
- Cap page size before constructing `PageRequest`.
- Allow-list externally supplied sort properties; do not expose arbitrary entity paths.
- Treat a cursor as opaque and integrity-protected when tampering changes scope or cost.

## Failure matrix

| Failure | Cause | Correction |
|---|---|---|
| duplicate/missing rows across offset pages | inserts/deletes move positions | keyset cursor or documented snapshot |
| duplicate/missing cursor rows | non-total or mutable ordering key | unique immutable tie-breaker and explicit live semantics |
| slow deep page | database still skips many rows | aligned keyset predicate/index |
| expensive `Page` | count query scans/joins large result | `Slice`, cached/approximate total, or separate product action |
| connection exhaustion | stream escapes transaction or is not closed | try-with-resources and bounded processing |
| cross-tenant cursor reuse | cursor does not bind scope | derive authorization server-side and sign/bind cursor |

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

## Interviewer question and model answer

**Interviewer:** Why is cursor pagination not simply `PageRequest.of(page + 1, size)`?

**Model answer:** `PageRequest` describes offset/page-number navigation. Cursor pagination continues from the last total-order key, such as `(createdAt,id)`, using a range predicate aligned to the sort. I do not combine the two. I bind the cursor to filters and scope, cap the limit, fetch `limit + 1`, and document behavior when ordering keys change. Cursor traversal improves deep-page work and live stability but does not create a database snapshot.
