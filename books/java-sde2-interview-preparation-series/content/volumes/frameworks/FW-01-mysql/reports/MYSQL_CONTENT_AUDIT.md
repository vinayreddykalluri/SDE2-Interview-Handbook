# MySQL Content Audit — Backend Wave 2

## Before improvement

The canonical volume contained one 232-word roadmap preview. It named future topics but did not teach relational reasoning, contain executable SQL/Java, explain InnoDB internals, provide failure cases, or answer interviews. The PDF metadata still marks the volume `planned` and points at the removed roadmap filename.

## Final chapter inventory

| # | Chapter | Depth and dependency |
|---:|---|---|
| 0 | Learning path and relational first principles | entry point; invariant-first answer frame |
| 1 | Modeling, keys, constraints, normalization | builds the logical model before queries |
| 2 | Types, nulls, collation, temporal safety | defines value semantics and edge cases |
| 3 | Joins, grouping, CTEs, windows | result-grain reasoning before tuning |
| 4 | InnoDB B+trees, buffer pool, redo/undo, MVCC | physical and recovery intuition |
| 5 | Index design and `EXPLAIN` | query-shape evidence after correct SQL |
| 6 | Transactions, isolation, locks, deadlocks | concurrent correctness and failure recovery |
| 7 | JDBC, pools, batching, timeouts, pagination | Java resource and protocol boundary |
| 8 | Migrations, replication, backup, observability, security | production lifecycle |
| 9 | Live interviews, rapid Q&A, assessment, sources | synthesis and readiness |

## Content-quality matrix

| Topic | Previous quality | Final quality | Evidence |
|---|---|---|---|
| relational invariants | too shallow | strong | keys, constraints, normalization, race examples |
| types and `NULL` | missing | strong | three-valued logic, `NOT IN`, collation/time cases |
| SQL reasoning | missing examples | strong | joins, result grain, grouping, CTE, windows |
| InnoDB internals | missing | strong | clustered/secondary lookup diagram, buffer/redo/undo/MVCC |
| indexes/plans | named only | strong | query-shape worksheet, sargability, estimate/actual loop |
| transactions | named only | strong | anomaly/lock/failure matrices and unknown commit outcome |
| Java integration | missing | strong | safe JDBC boundary, pool, batch, cursor examples |
| operations | named only | strong | expand-contract, replica lag, restore, telemetry, security |
| interviews | missing | strong | 7 live dialogues, 18 answered rapid questions, assessment |
| executable proof | missing | strong | companion plus 7-test H2 SQL/JDBC fixture |

## Priority findings

### Critical, resolved

- The source was a roadmap rather than a book and could not prepare a reader for SQL or concurrency interviews.
- It had no distinction between portable relational behavior and MySQL/InnoDB-specific behavior.
- It had no code validation, expected outcomes, failure timelines, or unknown-commit/idempotency treatment.

### High value, resolved

- Added basics-first dependency order and repeated correctness → access path → concurrency → failure framework.
- Added low-level InnoDB mechanics without unsupported claims about exact page/object layout.
- Replaced universal performance slogans with measurement, skew, plan, and write-cost trade-offs.
- Added daily-language author guidance, debugging exercises, edge-case tables, and production scenarios.

### Remaining publication work outside this wave

- Root integration must update `publishing/series.json`, rebuild the PDF/web, and visually inspect tables/code/diagrams.
- Target-MySQL Testcontainers or managed integration tests should later validate lock ranges, collations, plans, replication, and online DDL. H2 cannot prove them.

## Source baseline

Claims were cross-checked against the [MySQL 8.4 InnoDB documentation](https://dev.mysql.com/doc/refman/8.4/en/innodb-introduction.html), [MySQL backup and recovery guide](https://dev.mysql.com/doc/refman/8.4/en/backup-and-recovery.html), and Java JDBC contracts. Version-specific behavior is labeled and the reader is told to verify the target server/driver.
