# Broker Log, Partitions, Replication, and Storage Flow

## Topic and partition

A topic is divided into partitions. Each partition is an ordered sequence of records with monotonically increasing offsets. The offset identifies a position in one partition, not a global event sequence and not a permanent business ID.

```text
orders-0: [offset 0 A] [1 C] [2 A]
orders-1: [offset 0 B] [1 D]
```

Consumers can process partitions in parallel. Within one group, a partition is assigned to at most one consumer at a time; extra consumers beyond the available partitions are idle for that topic assignment.

## Key-to-partition behavior

A producer partitioner uses topic metadata and record key/value. Same key normally maps to the same partition while the partition count/partitioner contract remains compatible. Increasing partitions can change the key mapping for future records, so per-key ordering across the change needs a migration strategy.

Null keys may be distributed using batching/sticky behavior rather than deterministic entity ordering. Never require order and omit the key.

## Replication

Each partition has a leader and replicas. Producers/consumers interact with leaders; followers fetch from leaders. An in-sync replica set tracks replicas sufficiently caught up under broker rules. Controller metadata/elections are coordinated by KRaft in current Kafka deployments.

```text
producer -> partition leader append
                    |-> follower fetch/apply
                    |-> follower fetch/apply
              high watermark advances
consumer fetches committed/available records per isolation
```

Replication factor, `acks`, `min.insync.replicas`, and unclean-election policy jointly affect durability and availability. `acks=all` means all required in-sync acknowledgements under current ISR/minimum settings—not every configured replica regardless of health.

## Append and storage intuition

Partitions are stored as log segments with index structures. Records are appended, batched, compressed, served through sequential I/O/page cache, and removed/compacted by policy. Kafka does not keep every message as an individual file or deserialize application payloads to route them.

Large batches improve throughput/compression but increase memory and latency while filling. Large records stress broker/client buffers, replication, recovery, and consumers. Prefer object storage plus a referenced immutable blob for very large payloads when appropriate.

## High watermark and log end

The leader’s log end can include records not yet safely replicated/visible under a consumer’s isolation. The high watermark bounds records considered replicated enough for normal consumption. Transactional aborted/uncommitted records add another visibility rule for `read_committed` consumers.

## Retention and deletion

Time/size retention removes old log segments; it is not per-record exact deletion. Consumer lag beyond retained history can make offsets invalid and force reset/rebuild. Retention must exceed the longest supported outage/replay window plus safety margin, subject to storage capacity.

## Capacity worksheet

Estimate:

```text
ingress bytes/sec = events/sec * average encoded bytes
replicated network ~= ingress * replication factor (rough planning input)
retained bytes ~= ingress * retention seconds * replication factor
consumer egress = ingress * independent consuming groups
```

Then account for compression, peaks, headers, indexes, compaction, replication catch-up, reassignments, and disk/headroom. Measure rather than treating the formula as exact.

## Edge matrix

| Trap | Consequence | Repair |
|---|---|---|
| global-order assumption | events interleave across partitions | key/one partition or downstream versioning |
| add partitions casually | future key mapping changes | migration/versioned routing |
| RF 3 means no loss | acknowledgement/ISR/election policy still matters | test failure contract |
| retention too short | lagging group cannot replay | size from outage/recovery objective |
| giant records | broker/client/recovery pressure | compact event or external blob |

## Practice

- **Foundation:** Explain why offset 10 on partition 0 is unrelated to offset 10 on partition 1.
- **Interview Core:** Choose partitions for 60 consumer instances and justify peak throughput/key skew.
- **SDE-2 Follow-up:** Plan a partition-count increase while preserving aggregate ordering.
