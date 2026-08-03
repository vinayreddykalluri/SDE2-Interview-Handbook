# Kafka and Spring Kafka Content Changelog — Backend Wave 3

| Area | Change |
|---|---|
| sequence | replaced roadmap with nine log/partition-first chapters |
| broker internals | added append/batch/segment/page-cache intuition, leader/follower/high-watermark flow |
| producer | added serialization/partition/accumulator/request path, ack/ISR, idempotence, transactions |
| consumer | added group ownership, next-offset rule, loss/duplicate timelines, poll/rebalance/replay |
| contracts | added event/command distinction, schema compatibility, compaction/tombstones/privacy |
| reliability | added outbox/inbox, retry taxonomy, DLT lifecycle, external exactly-once boundary |
| Spring Kafka | added `KafkaTemplate`, listeners, ack modes, deserialization, handlers, retry topics, transactions |
| operations | added byte/partition sizing, metrics and lag/rebalance/disk/security incidents |
| interviews | added 7 live chains, 20 rapid answered questions, practice solutions, assessment |

## Executable additions

- `code/KafkaInterviewCompanion.java`: record identity, contiguous commits, inbox idempotency, failure classification, group capacity, aggregate versions.
- `labs/maven-demo`: real Spring Kafka 4.1/Kafka 4.2 JVM-local KRaft test plus client transaction, ack mode, retry and offset mechanics.
- `labs/validate_kafka_labs.sh`: strict Java 21 compile/smoke plus Maven tests.
