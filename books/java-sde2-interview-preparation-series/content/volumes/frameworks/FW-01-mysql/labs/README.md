# MySQL Volume Labs

The Maven fixture executes portable relational SQL and JDBC behavior on H2 in MySQL compatibility mode. It validates constraints, null-aware query construction, joins, CTE/window functions, transactions, optimistic updates, batching, and keyset pagination.

It intentionally does **not** claim to validate MySQL-specific behavior such as InnoDB clustered and secondary index layout, collations, optimizer plans, record/gap/next-key locks, deadlock codes, online DDL, binary logs, or replication. Run those scenarios against the exact target MySQL release in a controlled integration environment.

```bash
bash content/volumes/frameworks/FW-01-mysql/labs/validate_mysql_labs.sh
```
