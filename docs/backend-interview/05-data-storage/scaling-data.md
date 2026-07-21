# Indexes, Caching, Replication, and Partitioning

## Index design

Derive indexes from predicates, join keys, ordering, and selectivity. Explain composite prefix order, covering indexes, write amplification, storage cost, statistics, query plans, and removal.

## Cache design

Specify cache key, value, owner, TTL, invalidation, consistency budget, stampede protection, negative caching, size policy, and observability. Compare cache-aside, read-through, write-through, and write-behind.

## Replication

Discuss leader/follower, multi-leader, or leaderless operation; synchronous versus asynchronous acknowledgement; replica lag; read routing; failover; split brain; and RPO/RTO.

## Partitioning

Choose a key from cardinality, distribution, query locality, and growth. Address hot keys, fan-out, secondary indexes, rebalancing, tenant isolation, and cross-partition transactions.

## Required failure table

| Failure | User-visible effect | Detection | Mitigation |
| --- | --- | --- | --- |
| Cache unavailable |  |  |  |
| Replica lag |  |  |  |
| Hot partition |  |  |  |
| Failed failover |  |  |  |
| Partial migration |  |  |  |
