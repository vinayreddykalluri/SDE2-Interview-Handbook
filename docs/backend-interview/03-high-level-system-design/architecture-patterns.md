# Architecture Patterns and HLD Case Studies

## Pattern catalog

| Need | Candidate patterns | Trade-off to discuss |
| --- | --- | --- |
| Horizontal request scale | Stateless services, load balancing | Session/state placement |
| Read-heavy workload | Cache-aside, replicas, materialized views | Staleness and invalidation |
| Write-heavy ingestion | Partitioning, batching, log ingestion | Ordering and hot partitions |
| Decoupled workflows | Queue, pub/sub, event stream | Delivery, duplicates, lag |
| Independent read/write models | CQRS | Complexity and consistency |
| Multi-step distributed change | Saga, outbox | Compensation and observability |
| Edge latency | CDN, regional replicas | Invalidation and locality |
| Failure containment | Bulkhead, circuit breaker, shedding | Reduced functionality |

## Case-study backlog

- URL shortener and redirect analytics
- News feed and timeline fan-out
- Chat and presence service
- File storage and multipart upload
- Video processing pipeline
- Search autocomplete
- Metrics and log ingestion
- E-commerce checkout and inventory
- Ride matching and location updates
- Notification delivery platform
- Distributed job scheduler
- Feature-flag configuration service

Each study must include a baseline, estimates, ownership, contracts, one failure deep dive, one scaling deep dive, security boundaries, SLOs, and an evolution path.
