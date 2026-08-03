# Migrations, Replication, Recovery, Observability, and Security

Production database work is a lifecycle: safely change a live schema, route traffic, detect trouble, and recover data. A migration that is syntactically valid can still block writes or break old application instances.

## Expand, migrate, contract

For a risky rename or type change:

1. **Expand:** add the new compatible column/table/index.
2. **Deploy compatibility:** write both forms when necessary; read with a safe fallback.
3. **Backfill:** bounded, restartable batches with checkpoints and rate limits.
4. **Verify:** counts, nulls, checksums/domain invariants, and query plans.
5. **Switch:** move reads to the new representation.
6. **Contract later:** stop old writes, observe, then remove obsolete schema.

DDL algorithms, lock modes, and online behavior vary by MySQL version and operation. Test on production-like size; “online” can still consume resources or wait on metadata locks.

## Index rollout

Before adding an index, capture the query and baseline. During rollout monitor DDL progress, replication lag, I/O, and write latency. After rollout confirm the plan and business latency. Have a rollback/removal criterion—but remember removing a large index is also an operation.

## Replication is asynchronous unless you prove otherwise

MySQL replication uses the binary log to transmit committed changes. A replica may lag. Therefore read-after-write from a replica can return older data.

Choose per endpoint:

- source read for immediate consistency;
- session/causal marker with wait/routing logic;
- tolerate stale results and communicate the product behavior;
- cache/replica only for data whose staleness budget permits it.

Replication improves availability/read capacity; it also reproduces accidental deletes. **A replica is not a backup.**

## Recovery objectives

- **RPO:** maximum acceptable data loss measured in time or transactions.
- **RTO:** maximum acceptable time to restore service.

Meet them with a tested combination of full/incremental backups, binary logs for point-in-time recovery, retention, encryption, separate failure domains, and documented restore procedures. A backup is not proven until a restore test validates the application’s data invariants.

## Failover correctness

Promotion can expose lost/unapplied writes depending on topology and acknowledgement policy. Clients need connection discovery/routing, retry discipline, and idempotency. Fencing prevents an old source from accepting writes after a new one is promoted.

## What to observe

```text
application: request rate, errors, p95/p99, pool wait, transaction duration
query: normalized digest, calls, total/mean/tail time, rows examined/returned
database: CPU, I/O, buffer pool, redo/checkpoint, locks/deadlocks, temp work
replication: lag/apply state/errors, source position
capacity: data/index size, connections, growth, backup duration
```

Slow-query evidence is a starting point, not a verdict. Correlate with trace/request identifiers without logging credentials or full sensitive bind values.

## Security baseline

- separate application, migration, and administrative identities;
- least privilege and narrow network access;
- TLS in transit and managed encryption/keys at rest;
- parameterized SQL plus allow-listed structural input;
- secret rotation without source-control exposure;
- audited privileged operations;
- data classification, minimization, retention, and deletion verification;
- backups protected to the same or stronger standard as the source.

Encryption does not fix SQL injection; parameter binding does not fix excessive privileges; masking a log does not fix an overbroad database export. Use layered controls.

## Operational failure table

| Event | First concern | Immediate evidence | Durable fix |
|---|---|---|---|
| migration stalls writes | metadata/row locks | process/lock waits, DDL phase | safer rollout and rehearsal |
| replica returns missing order | lag/read routing | replay position, request route | causal/source read policy |
| backups succeed but restore fails | false recoverability | restore logs/invariant checks | automated restore drills |
| connection storm | retry amplification | pool pending, connection rate | backoff, budgets, admission control |
| credentials leaked | scope and dwell time | audit/connect logs | rotate, revoke, reduce privilege |

## Practice

- **Foundation:** Define RPO/RTO for a shopping cart and completed payments.
- **Interview Core:** Plan a non-null column addition with old and new app versions live.
- **Interview Core:** Explain why a read replica can violate read-after-write.
- **SDE-2 Follow-up:** Design an evidence-driven failover drill with rollback and success criteria.
