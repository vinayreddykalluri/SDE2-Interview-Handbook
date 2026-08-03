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

Choose a fetch plan for the use case when:

- relationship is always needed,
- row explosion is bounded,
- paging strategy prevents huge memory spikes.

Avoid making every association statically eager. One endpoint may need lines while another needs only order summaries. Prefer a bounded fetch join/entity graph, controlled batching, or explicit projection/query by relationship IDs according to result cardinality.

## SQL consequences

```text
20 orders + lazy lines accessed in a loop
  -> 1 order SELECT + up to 20 line SELECTs

20 orders + one collection fetch join
  -> 1 SQL statement, but one row per order-line pair

20 order summaries projected from order table
  -> 1 narrow SQL statement, no managed child graph
```

A collection fetch join can create a Cartesian product when several to-many associations are joined, and pagination over a collection fetch may be applied in memory or rejected depending on provider/version. Inspect SQL, rows, statements, heap, and response shape—not only query count.

## Edge matrix

| Strategy | Useful when | Main edge |
|---|---|---|
| entity graph/fetch join | one bounded graph is required | row multiplication and paging |
| batch fetching | several lazy associations are needed | extra round trips and batch tuning |
| DTO projection | read endpoint needs a stable narrow shape | explicit mapping and duplicate/group logic |
| two-step IDs then graph | page roots plus to-many data | preserve root order and bound ID list |

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

## Interviewer question and model answer

**Interviewer:** Should I fix N+1 by marking every association eager?

**Model answer:** No. Static eager loading shifts the surprise to queries that do not need the relationship and can create row or object-graph explosions. I identify the endpoint's result shape, capture SQL and query count, then choose a bounded entity graph/fetch join, batch fetch, or DTO projection. For a pageable root with a to-many relationship, I often page root IDs first and fetch the bounded graph second, then test order, statements, rows, and memory.
