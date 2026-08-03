# Sharding, Change Streams, Migrations, Operations, and Security

## Sharding partitions data by key ranges or hashed space

A sharded cluster routes operations through `mongos` to shards according to collection metadata. The shard key affects distribution, query targeting, migration cost, and the ability to enforce some uniqueness constraints.

## Shard-key worksheet

Evaluate candidates by:

- cardinality and frequency distribution;
- monotonicity and write hot spots;
- query routing prefixes;
- ability to add zones/locality if needed;
- document growth and chunk movement;
- immutable/stable availability in every operation;
- uniqueness requirements;
- future resharding cost.

`customerId` can co-locate a customer’s orders and target customer queries, but a celebrity tenant may create a hot shard. A hashed key spreads writes but loses natural range locality. A compound key may balance both; test the actual workload.

## Targeted versus scatter-gather

If a query includes the shard key or a usable prefix/range, the router can target fewer shards. Without it, the query may scatter, merge, sort, and limit results across shards. A fast single-shard plan does not predict cluster-wide behavior.

## Chunk migration and balancing

Balancing moves data to distribute load/storage. Migration consumes network, I/O, and coordination and can interact with long-running work. Observe shard imbalance, jumbo/large ranges, migration duration, and tail latency. Do not schedule major index builds, backfills, and rebalancing together without capacity evidence.

## Change streams

Change streams expose committed changes from the oplog through resumable cursors. They are useful for projections, cache invalidation, and integrations, but consumers must handle:

- duplicate delivery after resume/retry;
- resume-token persistence after durable processing;
- retention gaps/invalidation requiring rebuild;
- ordering scope rather than assuming global order;
- schema evolution and large-event handling;
- idempotent destination writes.

Persisting the resume token before the side effect can lose work; after the side effect can repeat it. Use idempotent writes or an atomic destination checkpoint when possible.

## Schema migration in a flexible store

Flexible documents make gradual rollout possible, not effortless:

1. deploy readers compatible with old/new shape;
2. write the new shape and stable version marker;
3. backfill in bounded restartable ranges;
4. verify counts/domain invariants and index usage;
5. tighten collection validation;
6. remove old compatibility only after observation.

Avoid rewriting every document merely to add a value that can be interpreted as a default on read—unless queries/indexes/validation require materialization.

## Backup and restore

Replication is not backup. Define RPO/RTO, use consistent snapshots plus oplog/point-in-time mechanisms appropriate to the deployment, encrypt and isolate backups, and restore-test the application invariants. Sharded recovery needs cluster-consistent planning, not independent ad hoc shard copies.

## Observability

Monitor:

- driver pool wait and server-selection time;
- command latency/errors by normalized shape;
- documents/keys examined versus returned;
- replication lag and election frequency;
- cache pressure, tickets/queues, disk and journal latency;
- locks/transactions and oldest active work;
- shard targeting, imbalance, migrations, chunk growth;
- change-stream lag/resume failures;
- backup age and restore-test results.

## Security baseline

Authenticate services, use least-privilege database roles, restrict networks, enable TLS, rotate secrets, encrypt backups, audit privileged operations, redact query values, and validate user-controlled operators. Do not deserialize arbitrary client JSON directly into a MongoDB query; operator injection can bypass the intended filter.

## Operational edge matrix

| Incident | Evidence | First correction |
|---|---|---|
| one shard at 95% CPU | shard-key distribution and targeting | remove scatter/hot-key cause before adding nodes |
| change consumer missed history | resume token and oplog retention | rebuild projection then resume from safe point |
| backfill hurts writes | batch size/yield/index use | throttle and checkpoint |
| repeated elections | member/network/disk health | fix infrastructure and acknowledgement assumptions |
| backup “green,” restore unusable | restore logs/domain checks | automated restore drills |

## Practice

- **Interview Core:** Choose a shard key for multi-tenant orders and name the hot-tenant failure.
- **SDE-2 Follow-up:** Build a cache-invalidation consumer that survives duplicate events and expired resume tokens.
