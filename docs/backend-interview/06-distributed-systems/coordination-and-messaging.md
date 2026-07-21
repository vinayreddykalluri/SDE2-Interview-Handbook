# Coordination, Consistency, and Messaging

## Foundation topics

- Partial failure, unreliable clocks, network partitions, and unbounded delay.
- Happens-before, logical clocks, causality, and conflict resolution.
- Quorums, leader election, leases, fencing tokens, and split-brain prevention.
- Consensus purpose and limits; when a database or coordinator provides it.
- Strong, causal, session, and eventual consistency models.

## Messaging decisions

Define producer acknowledgement, broker durability, partition key, ordering scope, consumer groups, delivery guarantee, retry schedule, dead-letter policy, replay, schema evolution, and lag monitoring.

## Reliable side effects

- Idempotency key or natural business key.
- Inbox/deduplication record for consumers.
- Transactional outbox for database state plus event publication.
- Saga orchestration or choreography for multi-service workflows.
- Reconciliation for states that still diverge.

## Drills

Design order-to-payment processing, notification fan-out, change-data capture, distributed locking, and a scheduled-job lease. Identify duplicate, reorder, stale-owner, and poison-message behavior.
