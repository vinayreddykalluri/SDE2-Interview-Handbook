# Kafka and Spring Kafka Content Audit — Backend Wave 3

## Before improvement

The canonical book was a 221-word roadmap. It contained no partition log, broker flow, producer acknowledgement, consumer commit timeline, schema example, outbox/inbox, Spring configuration, incident reasoning, executable broker test, or answered interview.

## Final inventory

| # | Chapter | Final evidence |
|---:|---|---|
| 00 | event/log learning path | end-to-end flow, delivery windows, answer frame |
| 01 | broker log, partitions, replication/storage | leader/follower/high-watermark model, retention/capacity |
| 02 | producer | key/batch path, acks/ISR, idempotence, transactions, failures |
| 03 | consumer | groups, poll/offset timelines, rebalances, replay, poison records |
| 04 | contracts and retention/compaction | compatibility, tombstones, snapshots, sensitive data |
| 05 | outbox/inbox/retries/DLT/EOS | dual writes, idempotency, unknown effects, retry/DLT lifecycle |
| 06 | Spring Kafka | template/listener/ack/error/retry-topic/transaction/test boundaries |
| 07 | operations | capacity, metrics, lag/rebalance/disk incidents, security/drills |
| 08 | interviews/readiness | 7 live chains, 20 rapid answers, cumulative assessment |

Final chapter content is 5,281 words plus executable code. It teaches Apache Kafka semantics before Spring abstractions and maintains the partition/offset/side-effect timeline throughout.

## Critical corrections and boundaries

- Ordering is limited to one partition; offsets are partition-local and not business IDs.
- `acks=all` is explained with current ISR/minimum-ISR settings, not “every replica forever.”
- Producer idempotence does not deduplicate external requests or consumer side effects.
- Commit-before/after effects explicitly show loss/duplicate windows.
- Auto/manual acknowledgement is not called exactly once.
- Kafka transactions cover supported Kafka records/offsets, not arbitrary MySQL/HTTP/payment effects.
- Retry topics explicitly sacrifice original ordering and current Spring transaction compatibility is version-labeled.
- DLT is treated as owned repair work, not successful processing.
- Compaction is asynchronous and tombstones have cleanup timing.

## Remaining target-cluster work

The embedded broker is one JVM-local KRaft node. A multi-broker environment remains required for RF/ISR/minimum ISR, controller failover, transactional recovery under broker loss, quotas, TLS/SASL/ACLs, reassignments, and capacity. PDF/web build and visual QA remain root-owned.

## Primary references

Claims were cross-checked against [Apache Kafka documentation](https://kafka.apache.org/documentation/) and the current [Spring Kafka reference](https://docs.spring.io/spring-kafka/reference/kafka.html), including error handling and retry-topic limitations.
