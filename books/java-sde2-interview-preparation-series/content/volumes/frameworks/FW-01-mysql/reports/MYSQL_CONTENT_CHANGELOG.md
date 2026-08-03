# MySQL Content Changelog — Backend Wave 2

| Chapter | Original weakness | Change made |
|---|---|---|
| 00 | no teaching entry point | added prerequisite map, running schema, SDE-2 answer frame, failure matrix |
| 01 | modeling listed only | added keys, constraints as race protection, normalization anomalies, denormalization test |
| 02 | absent | added numeric/text/time/JSON decisions, SQL three-valued logic, null and collation edge cases |
| 03 | absent | added result-grain method, join placement, multiplication, CTE/window reasoning, corrected queries |
| 04 | internals named only | added clustered/secondary B+tree path, coverage, buffer pool, redo/undo/binlog separation, MVCC visibility |
| 05 | no plan examples | added composite prefix, sargability, `EXPLAIN` evidence loop, estimate/actual mismatch, cursor indexing |
| 06 | no concurrency cases | added atomic writes, anomalies, MVCC/locks, deadlocks, retries, unknown commit, outbox/reservation case |
| 07 | no Java boundary | added prepared statements, transaction skeleton, pool queues, batching, streaming, cursor code |
| 08 | operational list only | added expand-migrate-contract, rollout, lag, RPO/RTO, failover, telemetry, security |
| 09 | no interview practice | added 7 live interviews, 18 rapid answered questions, cumulative assessment and rubric |

## Executable additions

- `code/MySqlInterviewCompanion.java`: three-valued logic, composite-index prefix, MVCC version visibility, conditional version update, descending cursor boundary, retry classification.
- `labs/maven-demo`: 7 portable H2/JDBC tests for constraints, left joins, CTE/windows, rollback, optimistic update, keyset pagination, and JDBC batching.
- `labs/validate_mysql_labs.sh`: Java 21 compile/smoke plus Maven tests.

## Accuracy boundaries added

- Secondary InnoDB leaves carry primary-key values; the clustered leaf is the row.
- Flush/commit/data-page persistence and redo/undo/binlog now have distinct roles.
- MVCC does not mean lock-free execution.
- A CTE, index, foreign key, larger pool, or key type is never called universally faster/better.
- Replication is not a backup and an API timeout is not proof of rollback.
- H2 behavior is never presented as proof of MySQL optimizer, lock, collation, or replication behavior.
