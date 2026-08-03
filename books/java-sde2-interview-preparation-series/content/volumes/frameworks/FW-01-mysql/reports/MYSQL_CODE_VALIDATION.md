# MySQL Code Validation — Backend Wave 2

## Results

- Java baseline: release 21.
- Dependency-free companions discovered: 1.
- Companions compiled with `-Xlint:all -Werror`: **1/1**.
- Companion smoke executions: **1/1 passed**.
- Maven/JUnit tests: **7 passed, 0 failed, 0 errors, 0 skipped**.
- Intentionally invalid snippets: 0 in the lab; conceptual broken SQL is labeled in prose.
- Output mismatches: 0.

Command:

```bash
bash content/volumes/frameworks/FW-01-mysql/labs/validate_mysql_labs.sh
```

Observed companion output:

```text
MySqlInterviewCompanion checks passed
```

## What the tests prove

1. unique/check constraints arbitrate invalid writes;
2. left-join predicate placement retains zero-child rows;
3. CTE/window ranking keeps detail and selects a deterministic winner;
4. rollback removes all work in the unit;
5. an expected-version predicate detects stale updates through affected rows;
6. descending keyset continuation uses both time and ID;
7. bound JDBC batches persist every row.

## What remains target-engine specific

The H2 fixture does not validate InnoDB clustered/secondary layout, `EXPLAIN` plans, collations, gap/next-key locks, MySQL error codes, online DDL, binary logs, replication, or recovery. Those need the exact target MySQL release and production-like data.

## Required root manifest mapping

Replace the single removed roadmap source with these paths in this exact order; set each `series_native` to `true`:

```text
content/volumes/frameworks/FW-01-mysql/chapters/00-learning-path-and-relational-first-principles.md
content/volumes/frameworks/FW-01-mysql/chapters/01-relational-modeling-keys-constraints-and-normalization.md
content/volumes/frameworks/FW-01-mysql/chapters/02-types-null-collation-temporal-and-schema-safety.md
content/volumes/frameworks/FW-01-mysql/chapters/03-sql-joins-grouping-cte-and-window-reasoning.md
content/volumes/frameworks/FW-01-mysql/chapters/04-innodb-bplus-tree-buffer-pool-redo-undo-and-mvcc.md
content/volumes/frameworks/FW-01-mysql/chapters/05-index-design-explain-and-query-performance.md
content/volumes/frameworks/FW-01-mysql/chapters/06-transactions-isolation-locks-deadlocks-and-retries.md
content/volumes/frameworks/FW-01-mysql/chapters/07-jdbc-pooling-batching-timeouts-and-pagination.md
content/volumes/frameworks/FW-01-mysql/chapters/08-migrations-replication-backup-observability-and-security.md
content/volumes/frameworks/FW-01-mysql/chapters/09-live-interviews-rapid-qa-practice-and-sources.md
```

Add:

```json
"code_companion": {
  "path": "content/volumes/frameworks/FW-01-mysql/code/MySqlInterviewCompanion.java",
  "title": "Java 21 MySQL Interview Reasoning Companion",
  "description": "Executable models for SQL truth, composite-index prefixes, MVCC visibility, optimistic updates, keyset boundaries, and transaction retry classification."
}
```

Change `publication_status` to `published`, `volume_label` to `Publication Edition`, and expand page bounds from roadmap values to approximately `min_pages: 24`, `max_pages: 90` before building.

## Root validation hooks

After manifest integration run:

```bash
python3 scripts/validate_series.py --source-only
python3 scripts/build_series.py --volume MYSQL
python3 scripts/validate_series.py
```

The central focused-Java validator skips `planned` volumes; publication status and companion mapping are therefore both required for it to compile/run this companion. Root must then inspect the generated PDF and book web pages.
