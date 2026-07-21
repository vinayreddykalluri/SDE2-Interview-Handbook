# HLD Framework and Capacity Estimation

## Interview framework

1. Define users, core use cases, data lifecycle, and exclusions.
2. Rank latency, availability, durability, consistency, throughput, privacy, and cost.
3. Estimate average and peak requests, payloads, retained data, bandwidth, and concurrency.
4. Define external APIs and internal events before drawing components.
5. Model data access patterns, then select storage.
6. Draw the simplest architecture that meets the stated scale.
7. Deep-dive the dominant bottleneck and highest-risk failure.
8. Close with observability, security, rollout, and future evolution.

## Estimation worksheet

```text
Daily active users:
Actions per active user per day:
Average QPS = daily actions / 86,400:
Peak multiplier and peak QPS:
Average request/response bytes:
Daily ingress/egress:
Records per day and retention:
Replication factor:
Concurrent requests = QPS * latency seconds:
Cache working set and expected hit rate:
```

Round aggressively and state assumptions. The objective is architectural order of magnitude, not false precision.

## Pressure tests

- Traffic increases by `10x` in one region.
- One dependency is slow rather than unavailable.
- A hot tenant dominates a partition.
- Consistency changes from eventual to read-your-writes.
- A schema or protocol must migrate without downtime.
