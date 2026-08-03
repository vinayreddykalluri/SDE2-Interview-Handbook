# Capacity, Observability, Security, and Incident Response

## Capacity starts with partitions and bytes

Partitions provide parallelism but consume broker/controller/client resources. Too few constrain throughput; too many increase metadata, open files, leader-election/reassignment time, and operational cost. Size from measured per-partition throughput and consumer parallelism, then include growth/recovery headroom.

Test skew: average events/sec can hide one key driving one partition to saturation.

## Producer signals

- record/send rate and encoded size;
- batch size, compression ratio, requests;
- buffer available/wait and queue time;
- acknowledgement latency p50/p95/p99;
- retries, errors by class, timeouts;
- metadata age and partition skew;
- transactional abort/fencing.

## Consumer signals

- lag by group/topic/partition and lag age where derivable;
- processing rate/latency/error by event type;
- time between polls, batch size, pause duration;
- rebalances, assignments/revocations, commit failures;
- retry attempts/topic lag and DLT rate/age;
- idempotency duplicate count;
- oldest unprocessed business event time.

Lag is a symptom. Diagnose whether ingress rose, handler slowed, one partition is hot, dependencies fail, rebalances loop, deserialization blocks, or consumer capacity disappeared.

## Broker/cluster signals

- under-replicated/offline partitions and ISR changes;
- produce/fetch request latency and queues;
- disk throughput/space, log directory health, page-cache pressure;
- network saturation and request size;
- controller/metadata health, leader imbalance;
- replica fetcher lag, reassignments;
- quota throttling and authentication/authorization failures.

## Incident: lag grows on one partition

1. Identify hot partition/key and oldest event.
2. Compare its ingress/record size/handler latency with peers.
3. Check consumer assignment, dependency errors, retries, and poison record.
4. Protect downstream with backpressure; do not add consumers beyond partition count expecting relief.
5. Mitigate hot aggregate/key only if ordering can be preserved or downstream versions reconcile.
6. Reprocess/DLT with audit and add a regression/load test.

## Incident: rebalance storm

Check deployment churn, poll interval violations, session/connectivity, GC pauses, overloaded coordinators, and unstable subscriptions. Reduce work per poll, pause safely, tune only with evidence, use cooperative/static membership where appropriate, and avoid synchronized rolling restarts.

## Incident: disk filling

Check retention overrides, unexpected record size/rate, replication factor, compaction backlog, stuck consumers (which do not themselves stop deletion retention but may create business pressure), and failed log cleanup. Do not delete topic data manually. Adjust retention/capacity through controlled changes and preserve required replay/RPO.

## Security

- TLS and authenticated SASL mechanism appropriate to environment;
- topic/group/transactional-ID ACLs with least privilege;
- quotas to control noisy producers/consumers;
- secrets rotation and client reconnect testing;
- schema registry and Connect security too;
- PII minimization, retention, DLT/log redaction;
- audit topic/config/ACL changes;
- network segmentation and broker listener separation.

Keys/headers can contain sensitive IDs and appear in logs/metrics. Hash/redact carefully while retaining diagnostic correlation.

## Recovery drills

Test broker loss under required ISR, producer unknown outcomes, consumer restart after side effect, offset reset, DLT replay, schema rollback, outbox backlog, certificate rotation, partition reassignment, and restoration/rebuild from retained/archived data.

## Failure matrix

| Symptom | Bad reflex | Evidence-driven action |
|---|---|---|
| lag high | add consumers | check partitions/skew/dependency |
| producer timeout | retry forever | delivery budget, idempotence, ISR/latency |
| DLT rising | ignore because main flow moves | classify/own/repair/replay |
| under-replicated partitions | lower min ISR immediately | fix disk/network/broker capacity first |
| auth failures during rotation | roll back blindly | verify credential overlap/client refresh |

## Practice

- **Interview Core:** Define alerts that distinguish total lag from one hot partition.
- **SDE-2 Follow-up:** Create a runbook for a 10× lag spike during a database outage, including backpressure, retries, DLT policy, recovery rate, and correctness verification.
