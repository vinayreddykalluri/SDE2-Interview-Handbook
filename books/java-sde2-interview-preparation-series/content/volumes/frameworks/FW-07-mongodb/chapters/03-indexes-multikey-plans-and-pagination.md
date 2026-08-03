# Indexes, Multikey Behavior, Query Plans, and Pagination

## Design from the query shape

```javascript
db.orders.find({
  customerId: "c-42",
  status: "PAID",
  createdAt: { $lt: boundaryTime }
}).sort({ createdAt: -1, _id: -1 }).limit(50)
```

A candidate:

```javascript
db.orders.createIndex(
  { customerId: 1, status: 1, createdAt: -1, _id: -1 },
  { name: "ix_order_customer_status_cursor" }
)
```

Equality fields, sort, and range are a useful starting heuristic. Actual key order depends on selectivity, sort direction, query variants, and write/index cost. Validate with representative data and `explain("executionStats")`.

## Compound-prefix reasoning

The order of fields defines usable prefixes. An index beginning with `customerId` cannot generally seek a query only by `status` as though status were first. Later fields can support filtering/coverage even when they no longer narrow the primary scan; read the winning plan.

## Multikey indexes

Indexing an array creates multikey index entries. One document can contribute many keys, increasing size/work. Compound multikey restrictions and correlation matter.

For an array of documents:

```javascript
{ lines: [ { sku: "A", quantity: 1 }, { sku: "B", quantity: 10 } ] }
```

These independent predicates may match different elements:

```javascript
{ "lines.sku": "A", "lines.quantity": { $gte: 10 } }
```

Use `$elemMatch` when both must hold for the same element:

```javascript
{ lines: { $elemMatch: { sku: "A", quantity: { $gte: 10 } } } }
```

## Specialized index decisions

- **Unique:** database invariant; understand missing/null and sharded constraints.
- **Partial:** indexes documents matching an expression; query must be compatible for use.
- **Sparse:** omits documents without the field; not identical to partial semantics.
- **TTL:** background expiration for date fields; deletion is not immediate or an exact scheduler.
- **Text/wildcard/geospatial:** solve specific query shapes with limits and operational cost.

TTL should not enforce a security deadline by itself. Check expiry in application authorization because the cleanup monitor can run later.

## Reading an explain plan

Ask:

- Did the winning plan use `IXSCAN` or `COLLSCAN`?
- Which bounds were applied to each key?
- How do keys/documents examined compare with rows returned?
- Was an in-memory/blocking sort required?
- Were documents fetched after index scan, or was the query covered?
- Did a shard router target one shard or scatter?

An index scan is not automatically efficient; scanning 10 million keys to return 10 rows is evidence of mismatch.

## Internal request path

```text
Java filter + options
  -> wire command
  -> parser/planner + plan cache
  -> index scan / collection scan
  -> WiredTiger cache pages and storage
  -> fetch/filter/sort/limit
  -> BSON batches over cursor
  -> Java decoding/mapping
```

Latency can arise in pool/server selection, network, plan/execution, storage cache, cursor batches, or decoding. Do not add an index until you locate the time and work.

## Cursor pagination

For descending `(createdAt, _id)`:

```javascript
{
  customerId: "c-42",
  status: "PAID",
  $or: [
    { createdAt: { $lt: boundaryTime } },
    { createdAt: boundaryTime, _id: { $lt: boundaryId } }
  ]
}
```

Use an opaque cursor containing every ordering component. Deep `skip()` still performs/discards earlier work and shifts under concurrent writes.

## Edge cases and practice

| Trap | Result |
|---|---|
| index every field | write amplification/cache pressure |
| TTL as exact timer | late cleanup and stale authorization |
| array predicates without `$elemMatch` | matches different elements |
| unstable sort | duplicate/missing cursor rows |
| plan tested on tiny uniform data | hides skew/cardinality |

- **Foundation:** Explain why an index prefix matters.
- **Interview Core:** Correct the array query using `$elemMatch`.
- **SDE-2 Follow-up:** Diagnose a plan with 2M keys and 20 returned documents.
