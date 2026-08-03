# MySQL Live Interviews, Answered Questions, Practice, and Sources

The questions below are written as interview dialogue. Read the prompt, pause, answer aloud, then compare with the reasoning. Strong answers state assumptions and failure behavior; they do not recite feature lists.

## Live interview 1: duplicate payments

**Interviewer:** “A client times out and retries `POST /payments`. How do you prevent two charges?”

**Candidate:** “I would require a client-scoped idempotency key and persist it under a unique constraint with the payment intent. The transaction claims that key and writes an outbox event. If commit acknowledgement is lost, the retry reads the existing row instead of issuing a second charge. The payment provider also receives a stable idempotency key. I would define how failed and expired attempts reuse or reject that key.”

**Follow-up:** Why not `SELECT` before `INSERT`?

**Answer:** Competing requests can both observe absence. The unique constraint arbitrates the race.

## Live interview 2: index design

**Interviewer:** “Fetch the newest 50 paid orders for one customer. What index?”

**Candidate:** “First I need the exact predicate and deterministic order. For `customer_id = ?`, `status = 'PAID'`, ordered by `(created_at DESC, order_id DESC)`, I would test `(customer_id, status, created_at DESC, order_id DESC)`. It narrows equality first and serves the cursor order. I would compare row estimates and actual work under skew, and assess write/index-size cost. I would not add projected values for coverage until evidence justifies the width.”

## Live interview 3: overselling inventory

**Interviewer:** “Two buyers see one item. Stop overselling.”

**Candidate:** “For a simple counter I prefer one atomic conditional update: decrement where available is positive, then require one affected row. If allocation spans rows or rules, I use a short explicit transaction, stable lock order, and `SELECT ... FOR UPDATE`/conditional writes. I keep payment calls outside the lock window and make reservation expiry a state transition.”

## Live interview 4: deadlocks after feature launch

**Interviewer:** “Deadlocks rose 20×. What do you do?”

**Candidate:** “Deadlocks are expected signals, not solved by increasing a timeout. I capture deadlock graphs plus query digests and business IDs, identify inconsistent resource order or widened scans, and inspect indexes. I make resource order deterministic, shorten the transaction, and add bounded classified retry for idempotent units. I verify retry amplification and tail latency after the change.”

## Live interview 5: replica lag

**Interviewer:** “The order confirmation page sometimes says ‘not found’.”

**Candidate:** “I would trace whether the write went to the source and the immediate read went to an asynchronous replica. For this flow I would use source/causal read-after-write or carry a consistency token and wait within a budget. I would measure lag and define fallback; making every read primary is simple but has capacity cost.”

## Live interview 6: zero-downtime schema change

**Interviewer:** “Rename a heavily used column.”

**Candidate:** “I would avoid a one-step rename while mixed application versions run. Add the new column, deploy compatible dual-write/backfill logic, migrate in restartable batches, validate equality and query plans, switch reads, observe, then remove old compatibility later. I would rehearse DDL locks and replication impact on production-like data.”

## Live interview 7: intermittent slow query

**Interviewer:** “The same SQL is usually 10 ms but sometimes 4 seconds.”

**Candidate:** “I separate pool wait from server time, preserve bind distribution, and correlate slow instances with lock waits, storage/cache misses, plan changes, temp work, and tenant skew. An average `EXPLAIN` without the slow bind is insufficient. I form one hypothesis and compare actual rows and wait evidence before changing an index.”

## Rapid answered questions

1. **Primary key versus unique key?** Both enforce uniqueness; one chosen primary key drives InnoDB clustering and cannot be null. Other candidate keys remain unique constraints.
2. **Does `COUNT(column)` count nulls?** No; it counts non-null values. `COUNT(*)` counts rows.
3. **`WHERE` versus `HAVING`?** `WHERE` filters input rows; `HAVING` filters grouped results.
4. **Why can `NOT IN` surprise?** A null in the compared set can make comparisons unknown. Use a null-safe design or correlated `NOT EXISTS`.
5. **Is a CTE materialized?** Not as a universal rule; the optimizer may merge or materialize it.
6. **Is an index always faster?** No. Low selectivity, random I/O, maintenance, and poor estimates can make a scan cheaper.
7. **Why include a tie-breaker in sorting?** Equal primary sort values otherwise produce unstable order and broken cursors.
8. **What is a covering index?** One containing all columns required for that query’s predicates/order/projection, avoiding clustered lookup.
9. **MVCC means no locks?** No. Consistent reads use versions; writes and locking reads acquire locks.
10. **Deadlock versus lock timeout?** A deadlock is a cycle the engine breaks; a timeout is excessive waiting without necessarily a cycle.
11. **Can every deadlock be retried?** Only retry the whole transaction when operations are idempotent/reconcilable, with bounds and jitter.
12. **What is an unknown outcome?** The client lost acknowledgement near commit and cannot infer whether work committed.
13. **Why is a replica not a backup?** It can reproduce corruption/deletion and does not itself provide historical recovery points.
14. **Offset versus keyset?** Offset supports page numbers but deep pages discard work; keyset continues from ordered values but lacks arbitrary jumps.
15. **Why can a bigger pool hurt?** It moves queuing into MySQL and increases contention, memory, and context switching.
16. **Does commit flush every data page?** No; durability uses logging/configuration while dirty pages can be written later.
17. **Why not log all SQL values?** They can expose secrets/PII and generate cost; use redacted structured diagnostics.
18. **When use a foreign key?** When the database owns both sides and referential integrity matters; discuss measured cost and lifecycle, not folklore.

## Cumulative assessment

Design an order service that supports idempotent creation, paid-order cursor listing, stock reservation, and read replicas. Deliver:

1. tables, keys, constraints, and data types;
2. the four most important SQL statements;
3. indexes justified by those statements;
4. concurrency timeline for two buyers;
5. JDBC transaction/resource boundary;
6. migration and backfill plan;
7. dashboards and recovery test;
8. answers for deadlock, timeout-at-commit, and replica-lag failures.

### Assessment rubric

| Level | Evidence |
|---|---|
| Foundation | valid model and SQL; explains nulls and joins |
| Interview Core | deliberate indexes, atomic writes, transaction failure handling |
| SDE-2 ready | reconciles unknown outcomes, plans operations, measures trade-offs |

## Selected authoritative references

- MySQL 8.4 Reference Manual: InnoDB architecture, indexes, transactions, locking, optimizer, replication, backup, and Performance Schema.
- MySQL Connector/J Developer Guide: JDBC URL, properties, batching, timeouts, and type mapping.
- Java SE 21 JDBC API: `DataSource`, `Connection`, `PreparedStatement`, `ResultSet`, and `SQLException` contracts.

Verify examples against the database and driver version used by your target company. H2 examples in the companion lab validate portable SQL/JDBC behavior; they do not prove MySQL collation, InnoDB plan, lock-range, replication, or DDL semantics.
