# Fetch Plans, the N+1 Pattern, and Performance Badges

If your data model reads a parent with many children, N+1 can hide in any abstraction.

## N+1 in simple words

1 query for parent rows + N queries for each parent’s child collection.

This is a classic interview failure because method naming may hide this behavior.

## Prevention layers

- **Join fetch / fetch join** where result shape is stable.
- **Batch fetch / chunked secondary query** with controlled window.
- **Projection DTO** for endpoint-only payload.

```text
parent query ----> children query per parent (bad)
parent + fetch join --> complete in bounded single query (good for bounded graphs)
```

## Decision rule

Use eager loading only when:

- relationship is always needed,
- row explosion is bounded,
- paging strategy prevents huge memory spikes.

Otherwise prefer controlled batching or explicit query by relationship IDs.

## Quick check

1. How does projection reduce impact of fetch joins?
2. Why is N+1 still possible with repository methods?
3. What is a safe first diagnostic query for suspected N+1?

## Debugging exercise

Endpoint returns one order page with nested items. QA reports 250 SQL statements for 20 rows.

What do you do in order?

Expected:

- inspect query log,
- check repository method and transaction boundary,
- add graph strategy,
- add assertion test for query count.

## Practice

- **Foundation:** Explain N+1 for `Order` and `OrderLine` in one paragraph.
- **Interview Core:** Propose one fix without changing API shape.
- **SDE-2 Follow-up:** Compare fetch join vs batch fetch for high-cardinality relationships.
