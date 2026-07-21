# Advanced Review: Data Systems

## Advanced model

| Area | Required depth |
| --- | --- |
| Storage internals | WAL, pages, buffer cache, B-tree, LSM, compaction |
| Concurrency control | MVCC, locks, snapshots, optimistic checks, deadlocks |
| Query planning | Cardinality, joins, statistics, plan regression |
| Distributed data | Quorums, lag, conflicts, partition repair |
| Transactions | Isolation anomalies, 2PC limits, saga alternatives |
| CDC | Ordering, snapshots, schema, replay, backpressure |
| Migration | Expand/contract, dual write, backfill, verification |
| Data quality | Constraints, reconciliation, lineage, audit, repair |

## Reconstruction exercise

Draw a write and read through a relational engine, then an LSM-based store. Mark persistence, cache, index, compaction, replication, and failures. Explain which workload each favors.

## Drill ladder

- **Foundation:** model data and indexes from access patterns.
- **SDE-2:** select isolation and diagnose an incorrect or slow query.
- **Advanced:** partition and replicate with required session guarantees.
- **Migration:** move live data with backfill, verification, and rollback.

## Retrieval prompts

1. Why can MVCC reduce blocking while increasing cleanup?
2. How do B-tree and LSM writes differ?
3. When does a composite index stop helping?
4. Why do cardinality estimates select bad plans?
5. How can replica lag violate a user journey?
6. Why does 2PC harm availability?
7. How do you verify dual-written datasets?
8. What makes a partition key unsafe?

## Exit criteria

Defend a storage design through engine internals, isolation, indexes, cache, partitioning, replication, migration, backup, and repair.
