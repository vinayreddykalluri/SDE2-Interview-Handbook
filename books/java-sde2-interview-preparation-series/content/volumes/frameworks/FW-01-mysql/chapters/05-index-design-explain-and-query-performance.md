# Index Design, `EXPLAIN`, and Query Performance

An index is an ordered data structure maintained on every relevant write. Design it from a real query shape—not from a list of frequently mentioned columns.

## The query-shape worksheet

For each important query, record:

```text
FROM/JOIN tables:
equality predicates:
range predicates:
required order:
projected columns:
expected matches and skew:
latency/throughput target:
write rate and index budget:
```

Example:

```sql
SELECT order_id, total_cents, created_at
FROM purchase_order
WHERE customer_id = ?
  AND status = 'PAID'
  AND created_at < ?
ORDER BY created_at DESC, order_id DESC
LIMIT 50;
```

A candidate is:

```sql
CREATE INDEX ix_order_customer_status_created_id
    ON purchase_order(customer_id, status, created_at DESC, order_id DESC);
```

The equality columns lead, followed by the range/order columns. This is a useful heuristic, not a law. Low-cardinality status can still be valuable after customer because the combined prefix is selective and matches the query. Measure alternate orders under real data distribution.

## Leftmost-prefix behavior

For `(customer_id, status, created_at)`, the ordering can efficiently support predicates beginning with `customer_id`, and often with both `customer_id` and `status`. A predicate only on `status` cannot generally seek through the first dimension of this B+tree.

After a range on one key part, later parts may still help filtering or coverage but frequently cannot narrow the same contiguous search interval. Read the actual plan for your version and query.

## Sargability

Keep the indexed column available for direct comparison.

```sql
-- Often prevents a simple range seek on created_at
WHERE DATE(created_at) = '2026-08-02'

-- Half-open range preserves all times and index ordering
WHERE created_at >= '2026-08-02 00:00:00'
  AND created_at <  '2026-08-03 00:00:00'
```

Other common problems: mismatched types, leading-wildcard patterns, arithmetic on the indexed column, and collation conversions.

## Reading `EXPLAIN` as evidence

Important signals include:

- chosen and candidate keys;
- access type and key parts used;
- estimated rows and filtering;
- additional work such as temporary tables or filesort;
- join order;
- actual timing and rows from `EXPLAIN ANALYZE` where safe.

Do not rank a plan by one field alone. “Using filesort” is not automatically bad, and “uses an index” can still mean scanning most of it.

### Estimate-versus-actual reasoning

If the optimizer estimates 10 rows but observes 500,000, investigate stale/inadequate statistics, skew, correlated columns, parameter distribution, and predicate conversions. An index may be structurally correct yet rejected because the estimate says it is expensive.

## Covering and redundant indexes

Coverage avoids clustered-row lookup, but adding every selected column creates a large write-heavy index. Review whether `(a)`, `(a,b)`, and `(a,b,c)` are all needed; longer indexes sometimes subsume shorter ones, but uniqueness, sort direction, size, and workload can justify both.

## Pagination plans

Deep offset pagination performs and discards earlier work:

```sql
... ORDER BY created_at DESC, order_id DESC
LIMIT 50 OFFSET 500000;
```

Keyset pagination continues after the last key:

```sql
WHERE (created_at < ?)
   OR (created_at = ? AND order_id < ?)
ORDER BY created_at DESC, order_id DESC
LIMIT 50;
```

MySQL row constructors may express an equivalent tuple comparison where types/order align. The cursor must contain every tie-breaker and be opaque/tamper-resistant if exposed to clients.

Offset still fits small admin screens and random page-number navigation. Keyset fits stable forward/backward traversal at scale.

## Performance investigation loop

1. Confirm user-visible symptom and time window.
2. Capture normalized query, bind-value distribution, call rate, and rows returned.
3. Inspect plan and actual work on representative data.
4. Check waits: CPU, storage, locks, connection pool, network.
5. Change one hypothesis—query, index, stats, data model, or workload.
6. Re-measure p50/p95/p99 and write-side cost.

## Edge cases

| Symptom | Plausible cause | Counterexample to avoid |
|---|---|---|
| index ignored | query returns large fraction | “optimizer is broken” |
| query slow only for one tenant | data skew | average cardinality assumption |
| plan changed after deploy | statistics/data/query shape changed | blaming application version only |
| fast query, slow endpoint | pool wait/network/serialization | adding an index |
| composite index not useful | missing leading prefix/type mismatch | adding each column separately |

## Interview drill

**Question:** “Index every foreign key and every `WHERE` column?”

**Answer:** Foreign-key lookup and parent-delete checks often benefit, but indexes must follow workload. Several single-column indexes do not automatically replace one ordered composite index. Each index has write, memory, storage, and maintenance cost.

## Practice

- **Foundation:** Explain why `(last_name, first_name)` does not efficiently seek by first name alone.
- **Interview Core:** Design and defend an index for the sample cursor query.
- **SDE-2 Follow-up:** An index makes reads 40% faster but doubles insert p99. Define the decision evidence.

## Readiness checkpoint

You are ready when you can say which rows an index narrows, which ordering it provides, whether it covers, and what it costs to maintain.
