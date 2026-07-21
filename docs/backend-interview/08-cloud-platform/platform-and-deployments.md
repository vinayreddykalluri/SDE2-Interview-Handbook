# Platform, Containers, Scaling, and Deployments

## Runtime topics

- Images, processes, namespaces, cgroups, filesystems, signals, and graceful shutdown.
- Service discovery, load balancing, ingress/egress, DNS, policy, and TLS identity.
- Scheduler requests/limits, health probes, disruption budgets, and placement.
- Configuration and secret delivery, rotation, and environment parity.

## Scaling decisions

Scale on the signal closest to the bottleneck: concurrency, CPU, queue lag, request rate, or business load. Account for startup, warm-up, downstream capacity, connection pools, and scale-down safety.

## Deployment strategies

Compare rolling, canary, blue/green, shadow, and feature-flag releases. Define health gates, traffic progression, observability, schema compatibility, rollback, and cleanup.

## Failure drills

- One availability zone disappears.
- DNS or certificate renewal fails.
- Autoscaling overloads an already saturated database.
- A bad image passes liveness but corrupts responses.
- A regional dependency is unavailable during failover.

For each, describe detection, traffic behavior, capacity, correctness, recovery, and customer impact.
