# InnoDB Internals: B+trees, Buffer Pool, Redo, Undo, and MVCC

This chapter gives enough implementation intuition to explain performance and correctness. It does not ask you to memorize page-file internals that vary by version.

## The row lookup path

InnoDB organizes the primary-key index as a clustered B+tree. Leaf records contain the row data. A secondary-index leaf contains the secondary key plus the primary-key value, so a noncovering secondary lookup often needs a second B+tree traversal.

```text
SELECT total_cents
FROM purchase_order
WHERE customer_id = 42;

secondary B+tree (customer_id, primary key)
       root -> branch -> leaf: (42, order_id=9001)
                               |
                               v
clustered primary-key B+tree
       root -> branch -> leaf: full order row
```

If the secondary index also contains the projected column, the plan may be covering and avoid the clustered lookup:

```sql
CREATE INDEX ix_order_customer_total
    ON purchase_order(customer_id, total_cents);
```

Coverage is query-specific and not free: wider indexes consume memory, add write amplification, and reduce entries per page.

## Why primary-key shape matters

Every secondary entry carries the primary key. A wide or random primary key can enlarge all secondary indexes and reduce insertion locality. That does not make random identifiers “wrong”; it makes the trade-off measurable.

InnoDB creates a hidden clustered key when no suitable primary/unique key exists. Relying on it gives the application no stable explicit identity and is poor schema design.

## The buffer pool

The buffer pool caches data and index pages. A logical read may be served from memory; a miss requires storage I/O. A benchmark immediately after restart and one after warm-up answer different questions.

Dirty pages contain committed or uncommitted changes not yet written to their data-file locations. Commit durability does **not** require every dirty data page to be flushed immediately.

## Redo versus undo

```text
UPDATE row
  |-- undo information: previous version / rollback + MVCC history
  |-- buffer-pool page becomes dirty
  |-- redo record: enough physical/logical change information for recovery
  `-- commit durability depends on log-flush configuration
```

- **Redo log** supports crash recovery by replaying durable changes whose data pages were not yet persisted.
- **Undo records** support rollback and older row versions used by consistent reads.
- **Binary log** is a server-level change stream used for replication and point-in-time recovery; it is not the same as InnoDB redo.

Do not say “undo always contains a full copy” or “commit writes the table row to disk.” Those oversimplifications produce incorrect incident reasoning.

## MVCC visibility intuition

A consistent read uses a read view to decide which row version is visible. A transaction may therefore read an older committed version while a newer version exists.

```text
row v3 (created by active T9) -> undo points to v2 (committed before read view)
                                  ^ visible version for this read
```

Long-running transactions can keep old versions relevant and delay purge, increasing history and storage pressure. “Readers do not block writers” is only a partial statement: locking reads, writes, metadata operations, and resource pressure still interact.

## Page split and locality intuition

When insertion targets a full B+tree page, the tree may split or reorganize. Sequential keys usually append with better locality; random keys distribute writes but may cause more scattered page work. The real decision also includes hot-spotting, distributed ID generation, privacy, and secondary-index width.

## Internal-to-symptom map

| Internal behavior | Observable symptom | Evidence to collect |
|---|---|---|
| poor buffer-pool locality | storage reads and latency after cache misses | buffer-pool metrics, I/O, plan, working set |
| noncovering range lookup | many clustered lookups | `EXPLAIN ANALYZE`, rows examined, projection |
| long read view | growing undo/history and purge lag | transaction age, history metrics |
| random wide primary key | larger indexes and write cost | index sizes, page behavior, write workload |
| checkpoint/log pressure | periodic write stalls | redo/checkpoint and fsync latency metrics |

## Interview question

**“Does an index store pointers to table rows?”**

Answer precisely for InnoDB: the clustered primary-key leaf is the row. A secondary leaf stores its key and the primary-key value; the engine uses that key for a clustered lookup unless the secondary index covers the query. Avoid applying heap-table terminology to every engine.

## Quick check and practice

1. Why can a secondary lookup require two B+tree traversals?
2. How can commit be durable before a data page reaches its final file location?
3. What does undo provide besides rollback?
4. Why can a long-running reader create operational pressure?

- **Foundation:** Draw the lookup for `WHERE email = ?` with a unique secondary index.
- **Interview Core:** Explain why adding projection columns can help and hurt.
- **SDE-2 Follow-up:** Given a latency regression only after deployment restart, distinguish cold-cache behavior from a bad plan.
