# Advanced Review: High-Level System Design

## Advanced model

| Area | Required depth |
| --- | --- |
| Multi-region | Active/passive, active/active, locality, failover, conflicts |
| Control/data planes | Availability independence, propagation, stale configuration |
| Load management | Admission control, queueing, backpressure, shedding |
| Data movement | Backfill, dual write, CDC, validation, cutover, rollback |
| Tail latency | Fan-out, hedging, timeout budgets, percentiles |
| Cost | Storage tiers, transfer, managed services, overprovisioning |
| Tenant isolation | Noisy neighbors, quotas, partitioning, blast radius |
| Evolution | Protocol versions, schema migration, decomposition |

## Reconstruction exercise

Redraw a previous system in three layers: request/data flow, deployment/failure domains, and ownership/operational boundaries. Add estimates beside every capacity-sensitive edge.

## Drill ladder

- **Foundation:** produce a complete single-region baseline.
- **SDE-2:** identify bottlenecks and explicit failure behavior.
- **Advanced:** make it multi-region with a stated consistency model.
- **Evolution:** migrate a datastore or protocol with no downtime.

## Retrieval prompts

1. How does fan-out change tail latency and availability?
2. When should control and data planes fail independently?
3. How do RPO and RTO alter replication choices?
4. What prevents retries from multiplying traffic?
5. How do you validate a backfill before cutover?
6. Which metrics prove a cache or queue helps the SLO?
7. How do tenant isolation and cost conflict?
8. What changes at `100x` scale?

## Exit criteria

Lead an unseen 45-minute design through requirements, estimates, contracts, data, architecture, multi-region failure, security, observability, cost, and migration.
