# Advanced Review: Cloud and Platform

## Advanced model

| Area | Required depth |
| --- | --- |
| Container runtime | Layers, process model, cgroups, namespaces, signals |
| Orchestration | Scheduling, probes, controllers, disruption, rollout |
| Networking | DNS, load balancing, CNI, ingress/egress, policy, TLS |
| Storage | Ephemeral/persistent volumes, CSI, topology, recovery |
| Autoscaling | Signals, startup lag, downstream limits, stability |
| Identity | Workload identity, IAM, secrets, least privilege |
| Multi-region | Traffic steering, locality, failover, data plane |
| Cost | Requests/limits, reservations, transfer, headroom |

## Reconstruction exercise

Trace a request from DNS through edge, load balancer, service, workload, queue, and database. Add identities, network boundaries, zones, telemetry, deployment controller, and timeouts.

## Drill ladder

- **Foundation:** containerize and deploy a stateless service.
- **SDE-2:** set resources, probes, disruption budget, and autoscaling.
- **Advanced:** survive a zone failure without overloading storage.
- **Regional:** define traffic and data recovery for region loss.

## Retrieval prompts

1. Why can a liveness probe cause an outage loop?
2. How do requests and limits affect scheduling?
3. When is CPU a poor autoscaling signal?
4. How does pooling interact with pod scaling?
5. What complexity does a service mesh add?
6. Which dependencies prevent regional failover?
7. How do disruption budgets conflict with rollout?
8. Which metric reveals overprovisioning?

## Exit criteria

Defend a secure multi-zone deployment, autoscaling policy, rollout, zone failure response, regional recovery path, and cost model.
