# Apache Kafka and Spring Kafka

## Learning Path: Event Log Before Listener Annotations

Kafka stores ordered records in partitioned logs. Producers append; consumers fetch by offset; consumer groups coordinate partition ownership. Spring Kafka automates clients and listener containers, but it cannot choose the correct key, commit point, retry effect, or schema contract for you.

> **From Vinay:** In an interview, draw the partition and write the offset beside each record. Then mark where the business side effect happens and where the offset is committed. Most “exactly once” confusion disappears on that timeline.

## Dependency order

```text
event contract and log
 -> topic + partitions + replication
 -> producer key/batch/ack/idempotence
 -> consumer fetch/group/offset/rebalance
 -> delivery and failure windows
 -> schema evolution + retention/compaction
 -> outbox/inbox/retry/DLT/transactions
 -> Spring Kafka containers and handlers
 -> capacity, security, and incidents
```

## One running event

```json
{
  "eventId": "evt-7f3",
  "eventType": "OrderPaid",
  "schemaVersion": 2,
  "occurredAt": "2026-08-02T10:00:00Z",
  "aggregateId": "order-42",
  "aggregateVersion": 4,
  "tenantId": "tenant-9",
  "data": { "amountCents": 5000, "currency": "USD" }
}
```

Use `order-42` as the record key when per-order order matters. Events for that key map to one partition under stable partitioning metadata. Ordering is not global across partitions.

## End-to-end flow

```text
database transaction writes order + outbox row
  -> publisher reads outbox
  -> producer serializes/key-partitions/batches
  -> broker leader appends and replicas copy
  -> acknowledgement reaches producer
  -> consumer group member fetches partition
  -> deserializer validates schema
  -> handler applies idempotent database effect
  -> offset/checkpoint is committed
```

At every arrow ask: can it retry, duplicate, reorder, block, or lose acknowledgement?

## Delivery terms

- **At-most-once:** commit/advance before processing; failure can lose work, duplicates reduced.
- **At-least-once:** process before commit; crash can repeat work, so effects must be idempotent.
- **Exactly-once Kafka processing:** transactions/idempotence can make supported Kafka reads/writes/offsets atomic for read-process-write flows. It does not automatically make MySQL, email, or HTTP side effects exactly once.

## First failure timeline

```text
consume offset 25
charge database successfully
process crashes before committing offset 26
record 25 is fetched again
```

The correct default is not “avoid duplicate delivery.” It is “make the charge/application effect detect the duplicate event ID or business command.”

## The SDE-2 answer frame

1. Event versus command: what fact/intent is represented?
2. Key and partition: what ordering and parallelism are required?
3. Producer: what acknowledgement, retry, idempotence, and timeout?
4. Consumer: when is the side effect durable and when is the next offset committed?
5. Poison/transient failure: blocking retry, retry topic, DLT, or stop?
6. Schema: how do old/new producers and consumers coexist?
7. Operations: retention, capacity, lag, rebalances, security, replay, recovery?

## Practice

- **Foundation:** Draw two partitions and assign six keyed order events.
- **Interview Core:** Mark duplicate/loss windows around a database write and offset commit.
- **SDE-2 Follow-up:** Explain why a transactional Kafka producer cannot roll back a payment-provider call.
