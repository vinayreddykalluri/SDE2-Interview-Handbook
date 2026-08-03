# Aggregation Pipelines, Memory, and Result Grain

An aggregation pipeline transforms a stream of documents stage by stage. As in SQL, define the result grain before writing stages.

## A readable pipeline

```javascript
db.orders.aggregate([
  { $match: {
      status: "PAID",
      createdAt: { $gte: ISODate("2026-08-01T00:00:00Z") }
  }},
  { $group: {
      _id: "$customerId",
      paidCents: { $sum: "$totals.subtotalCents" },
      orderCount: { $sum: 1 }
  }},
  { $match: { paidCents: { $gte: NumberLong(100000) } } },
  { $sort: { paidCents: -1, _id: 1 } },
  { $limit: 100 }
])
```

The first `$match` can use an index and reduces the stream before grouping. The second filters groups. The optimizer can reorder/coalesce some stages, but write for correct semantics and verify the explain plan.

## Core stages

- `$match`: filter documents; early selective filters reduce work.
- `$project`/`$set`: shape or compute fields; avoid discarding a field needed later.
- `$unwind`: one output per array element; can multiply rows dramatically.
- `$group`: collapse by key and accumulate.
- `$sort`/`$limit`: order and bound; index support depends on preceding stages.
- `$lookup`: join-like access to another collection; watch fan-out and shard behavior.
- `$facet`: multiple subpipelines over the same input; memory/work can multiply.
- `$setWindowFields`: window computations while retaining row/document grain.

## `$unwind` multiplication

An order with 5 lines becomes 5 pipeline documents. If a later `$lookup` returns 3 promotions per line, the stream can become 15 records for one order. Aggregating money at that point may triple count it. Track grain after every expanding stage.

## `$lookup` is not free embedding

`$lookup` can be appropriate for reporting or bounded joins with indexed foreign fields. It does not make document boundaries irrelevant. Ask how many local documents, how many matches per local row, whether the foreign predicate is indexed, and what transfers between shards/nodes.

## Memory and spilling

Blocking stages such as sort/group may require memory and can spill to disk depending on limits/options/version. Disk allowance prevents some failures but does not make an unbounded pipeline cheap. Bound the time range, filter early, index entry stages, precompute repairable summaries, or run analytical work elsewhere.

## Result materialization

`$merge` and `$out` can materialize results. Define idempotency, key choice, replacement/merge behavior, concurrent runs, and partial failure. A derived collection needs lineage, freshness, reconciliation, and rebuild procedures.

## Window example: latest order per customer

```javascript
[
  { $setWindowFields: {
      partitionBy: "$customerId",
      sortBy: { createdAt: -1, _id: -1 },
      output: { rank: { $documentNumber: {} } }
  }},
  { $match: { rank: 1 } }
]
```

Compare with sort-then-group patterns under your version and indexes. The best expression is the one whose semantics and plan are proven.

## Failure/edge matrix

| Symptom | Likely cause | Repair |
|---|---|---|
| totals multiplied | unwind/lookup changed grain | regroup at intended identity |
| memory error/spill | unbounded sort/group/facet | filter, index, partition, precompute |
| lookup slow | fan-out/missing foreign index | model/index/bound relation |
| report stale | materialized projection lag | freshness contract and reconciliation |
| timeout but server continues | cancellation/outcome unclear | `maxTimeMS`, operation tracking, idempotent writes |

## Practice with solutions

- **Foundation:** Sum paid totals by customer. `$match`, `$group`, deterministic `$sort`.
- **Interview Core:** Find top SKU without double-counting order totals. Unwind lines and sum line-level extended price, not order total.
- **SDE-2 Follow-up:** Design a daily revenue projection. Use a stable day/customer key, idempotent `$merge` policy, watermark, late-data correction, and source reconciliation.
