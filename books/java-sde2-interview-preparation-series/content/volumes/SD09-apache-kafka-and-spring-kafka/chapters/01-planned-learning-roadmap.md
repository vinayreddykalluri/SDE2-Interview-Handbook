# Apache Kafka and Spring Kafka - Planned Learning Roadmap

> **Publication status:** roadmap edition. Runnable brokers, producer and consumer labs, rebalance drills, and delivery-semantics exercises will follow.

Apache Kafka is a distributed log. Spring Kafka integrates Kafka clients with Spring configuration, listener containers, transactions, retries, and testing support. The book will teach Kafka contracts first and Spring integration second.

## Planned sequence

1. Topics, partitions, records, offsets, retention, compaction, and ordering scope.
2. Brokers, leaders, replicas, in-sync replicas, acknowledgments, and durability.
3. Producers, keys, partitioning, batching, compression, retries, and idempotence.
4. Consumers, groups, assignments, polling, commits, lag, and rebalances.
5. At-most-once, at-least-once, transactional processing, and practical deduplication.
6. Schema contracts, compatibility, poison records, retry topics, and dead-letter handling.
7. Spring Kafka factories, templates, listeners, error handlers, transactions, and tests.
8. Capacity, partition counts, hot keys, observability, upgrades, and incident recovery.

## Interview focus

Readers will learn to define the ordering key, choose acknowledgment and commit boundaries, explain replay, handle duplicates, diagnose lag, and defend a retry and dead-letter policy. Framework annotations will always be tied back to native client behavior.

## Completion gate

A reader is ready to design a Kafka workflow when they can state partitioning, delivery, replay, schema, failure, and observability contracts and can explain exactly where Spring Kafka starts and where Kafka client semantics still control correctness.
