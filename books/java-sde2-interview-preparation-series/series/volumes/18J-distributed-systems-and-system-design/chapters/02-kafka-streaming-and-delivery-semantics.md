# Kafka Partitions, Consumer Groups, and Delivery Semantics

## Learning objectives

After this chapter, you should be able to:

- model a Kafka topic as ordered partition logs with replicated durability configured at broker/topic/producer layers;
- select a message key from ordering, parallelism, skew, and evolution needs;
- explain consumer groups, assignments, polling, offset position, committed offsets, and rebalances;
- place offset commits relative to effects to obtain an explicit at-most-once or at-least-once failure window;
- explain what producer idempotence and Kafka transactions do and do not guarantee;
- evolve event schemas under producer/consumer compatibility rules;
- design retry, quarantine/DLQ, replay, and idempotent handling as operational protocols; and
- use Spring for Apache Kafka without hiding broker semantics behind annotations.

## 1. The log and partition model

A topic has one or more partitions. Each partition is an append-only ordered sequence addressed by offsets:

```text
partition 0: [offset 0][1][2][3]...
partition 1: [offset 0][1][2]...
```

Order is defined *within a partition*, not globally across a topic. An offset identifies a position, not a message identity and not a timestamp. Retention can remove old records even though offsets remain conceptual positions. Compaction, when configured, retains records according to key/tombstone and broker policy; it is not a full audit log guarantee.

Partitions provide parallelism and the unit of replicated leadership/assignment. More partitions can increase parallel consumption but also add metadata, files, replication/recovery work, rebalance scope, and ordering fragmentation. Partition count changes can alter key-to-partition mapping under common partitioners, so a key ordering/history contract needs a migration plan.

### Producer send model

A producer serializes a key and value, selects a partition, batches/compresses records, sends to the partition leader, and receives an acknowledgement under configured durability semantics. Correctness depends on the combination of:

- topic replication factor and in-sync replica policy;
- producer acknowledgement configuration;
- retry and delivery timeout;
- idempotence/transactions;
- broker failure and leader election policy;
- error handling in application code.

Never say “Kafka write succeeded” without defining which acknowledgement completed and what durability/failure domain that implies for the deployed cluster.

## 2. Keys, ordering, and hot partitions

### Key decision rule

Choose the smallest business identity whose events require relative order. For order lifecycle events, `orderId` is a natural key:

```text
OrderPlaced(O-7) -> PaymentAuthorized(O-7) -> OrderConfirmed(O-7)
```

Different orders can process in parallel. Keying by `customerId` would serialize all of one customer's orders and can create hotspots. A null key may be spread by the producer but offers no stable per-entity routing contract.

Per-key order is preserved only along a compatible path:

- all relevant records select the same partition;
- producer retries/configuration preserve documented ordering behavior;
- consumers do not apply records concurrently out of order within the key;
- retries do not divert one failed record while later same-key records commit incorrectly;
- repartitioning or topic migration preserves the contract.

### Hot-key walkthrough

A global tenant configuration event uses `tenantId`; one tenant generates half the traffic. Its partition is saturated while others are idle. Random salting spreads load but destroys simple per-tenant order. Alternatives:

- subdivide by a business subkey and make consumers order only within subkey;
- isolate the heavy tenant/topic;
- aggregate or rate-limit producer traffic;
- use more partitions only if there are enough independent keys;
- design sequence/version conflict handling so out-of-order across buckets is acceptable.

Partition skew is measured in bytes, records, processing time, and lag—not key count alone.

## 3. Consumer groups and offsets

### Group model

Within one consumer group, each partition is assigned to at most one active group member at a time under the group protocol. A member can own multiple partitions; if members exceed partitions, some are idle. Separate groups consume independently.

The consumer has concepts that are often conflated:

- **current position:** next offset the consumer will return locally;
- **committed offset:** stored recovery position for the group;
- **high watermark/end position:** broker-side boundary used to define lag under the implementation's metrics;
- **processed effect:** application's external state, which Kafka does not infer from method return.

Committing an offset means “on recovery, the group may resume from here” under the API protocol. It does not prove a database transaction committed unless the application coordinated those actions through an explicit design.

### Poll loop contract

Simplified native client sketch — dependency-requiring:

```java
while (running) {
    ConsumerRecords<String, OrderEvent> batch = consumer.poll(pollTimeout);
    for (TopicPartition partition : batch.partitions()) {
        List<ConsumerRecord<String, OrderEvent>> records =
                batch.records(partition);
        for (ConsumerRecord<String, OrderEvent> record : records) {
            handler.applyIdempotently(record.key(), record.value());
        }
        long next = records.get(records.size() - 1).offset() + 1;
        consumer.commitSync(Map.of(
                partition, new OffsetAndMetadata(next)));
    }
}
```

This commits per-partition after processing. Real code must handle empty batches, wakeup/shutdown, retriable/fatal broker errors, partition revocation, partial failures, time budgets, and poison records. Long processing must remain within group liveness/max-poll protocol or use supported pause/worker/handoff design carefully.

Do not process a partition concurrently without a sequencing and commit-watermark algorithm. If offsets 10 and 11 run in parallel and 11 finishes first, committing 12 before 10's effect is durable can lose 10 on crash.

## 4. Rebalances

Group membership or subscription/metadata changes can reassign partitions. Depending on client/broker protocol and configuration, rebalancing may revoke broad assignments or transfer incrementally/cooperatively. Exact protocol support is version-sensitive.

An assignment lifecycle needs:

1. stop accepting new work for revoked partitions;
2. complete/cancel/drain in-flight work under a deadline;
3. durably record safe processed offsets if application-managed;
4. release partition-owned state/resources;
5. initialize state for newly assigned partitions;
6. resume polling without exceeding liveness constraints.

### Rebalance failure walkthrough

Consumer A processes partition 2 offset 100 slowly. A rebalance revokes it and assigns partition 2 to B. If A continues and writes after B processes the same/newer key, stale effects can overwrite new state. Idempotency by event ID prevents duplicates but not necessarily stale ordering. Use source sequence/version conditions or fencing ownership when effects require it, and stop revoked work promptly.

Rebalance storms can come from slow polls, unstable instances, aggressive timeouts, deployments, partition changes, or coordinator/broker issues. Observe rebalance count/duration, assignment churn, processing time, poll interval, and lag rather than blindly increasing timeouts.

## 5. Delivery semantics and failure windows

### At-most-once

Commit/advance recovery position before applying the effect:

```text
receive -> commit offset -> effect
```

Crash after commit but before effect loses the message. Suitable only when occasional loss is acceptable or another replay source exists.

### At-least-once

Apply effect, then commit:

```text
receive -> effect -> commit offset
```

Crash after effect before commit repeats the record. This is common and requires an idempotent effect or deduplication. For a database consumer, insert an inbox/event ID and domain transition in one local transaction:

```sql
begin;
insert into consumer_inbox(consumer_name, event_id) values (?, ?)
    on conflict do nothing;
-- Only if inserted: apply the state transition with source version.
commit;
```

Then commit Kafka offset. Duplicate delivery repeats the DB transaction, sees inbox, and becomes a no-op. If DB commits and offset commit fails, replay is safe.

### Producer idempotence

Kafka's idempotent producer protocol prevents duplicate writes caused by supported producer retries within its producer-session/partition semantics and preserves relevant ordering under documented configuration. It does not deduplicate two independent business calls that produce the same logical event. Use a stable event ID/outbox for business idempotency.

### Kafka transactions/exactly-once

Kafka transactions can atomically write records to Kafka partitions and commit consumed offsets for consume-process-produce pipelines when producers/consumers use the required transaction and isolation settings. The practical guarantee is scoped to Kafka resources and the configured clients. It does not atomically include an arbitrary relational database or HTTP call.

State precisely:

- which inputs/outputs are Kafka records;
- whether consumers use committed-only isolation;
- transactional identity/epoch behavior across restarts;
- what external side effects exist;
- how duplicates/retries at the business/API layer are handled.

“Exactly once” is not a magic absence of retries. It is a protocol that makes selected writes/offsets visible atomically and filters aborted work for participating consumers.

## 6. Event schema evolution

### Event contract

An event is an immutable statement about something that happened, with:

```text
eventId, eventType, schemaVersion, aggregateId/key,
occurredAt, producer, trace/correlation metadata, payload
```

Avoid copying request DTOs or database entities onto the wire. Define field meaning, units, timezone, optionality, enum/unknown handling, identity, and privacy. `occurredAt` is domain/event time; broker append time and processing time are different.

Compatibility depends on serializer/schema system, but general rules include:

- add optional/defaultable fields when old consumers can ignore them;
- do not reuse a field name/number with a new meaning;
- do not silently change units or enum semantics;
- preserve readers during rolling producer/consumer deployment;
- use new event type/version or dual publishing when meaning is incompatible;
- retain raw input/replay ability only under privacy and retention policy;
- test representative old/new readers and writers.

A schema registry can enforce syntactic compatibility rules, but cannot prove semantic compatibility. Renaming `amountCents` to `amount` while changing units can pass some schemas and still break money.

## 7. Retry topics, DLQ, and replay

### Classify failures

- transient dependency failure: retry with bounded exponential backoff/jitter;
- rate/capacity failure: pause/backpressure; retries alone amplify load;
- validation/schema poison record: quarantine with reason and payload reference under security policy;
- authorization/business rejection: terminal domain outcome, not infrastructure retry;
- code defect: halt/contain and deploy fix; repeated retry burns capacity.

Blocking a partition preserves same-partition order but one poison record can stop unrelated keys. Moving to retry topics improves throughput but later same-key events can overtake. Options:

- keep ordered retries for workflows that require it;
- version-check state so stale events are rejected;
- partition retry stream by same key and coordinate delay;
- separate independent workloads/keys;
- quarantine with an explicit manual repair that accounts for later events.

### DLQ is a workflow

A useful quarantine record includes original topic/partition/offset, key, event ID/schema, failure code, first/last failure times, attempt count, handler version, and a secure reference or redacted payload. Operations require:

- alert/ownership and age/count SLO;
- investigation tooling with authorization/audit;
- fix-forward, discard, or replay decision;
- replay idempotency and rate control;
- prevention tracking;
- retention/privacy deletion.

Blindly replaying a DLQ into the original topic can recreate the incident and violate order. Use a controlled replay producer and observe effects.

## 8. Spring Kafka boundary — dependency-requiring

```java
@KafkaListener(topics = "order-events", groupId = "fulfillment-v3")
void onOrderEvent(ConsumerRecord<String, OrderEvent> record,
                  Acknowledgment acknowledgment) {
    handler.applyInDatabaseTransaction(
            record.key(), record.value(), record.topic(),
            record.partition(), record.offset());
    acknowledgment.acknowledge();
}
```

The annotation does not define delivery semantics by itself. Container acknowledgement mode, listener transaction integration, error handler/retry policy, concurrency, assignment, deserializer behavior, and broker/client settings determine execution. Acknowledge only after the durable effect according to the chosen protocol. Integration-test crash windows with the actual framework/client versions.

## 9. Interview questions and model checkpoints

### Q1. How many consumers can one group use effectively?

**Model checkpoint:** up to the number of partitions for direct partition assignment at a time; extra consumers are idle. Processing concurrency can be designed inside a member but must preserve per-partition/key order and safe offset watermark.

### Q2. Why might a record be processed twice?

**Model checkpoint:** effect succeeded and offset commit failed/crash occurred, producer/business duplicated, rebalance repeated uncommitted work, or retry policy redelivered. Use event identity/source version and atomic inbox+effect.

### Q3. Does idempotent producer make a create-order API idempotent?

**Model checkpoint:** no. It addresses producer retry duplicates within Kafka protocol scope. API request identity/outbox/business event ID addresses repeated business commands.

### Q4. What is the ordering guarantee?

**Model checkpoint:** records in a partition have broker log order; same-key producer partitioning commonly co-locates records. End-to-end effects need serial handling/versioning and retry/rebalance design. No topic-global order.

### SDE-2 follow-ups

1. Process one partition with a worker pool while committing only the highest contiguous completed offset.
2. Increase partition count without breaking per-customer workflow semantics.
3. Combine a relational outbox with Kafka producer idempotence and consumer inbox; enumerate all duplicate windows.
4. Design a schema-breaking event migration with old consumers running for two weeks.

## 10. Exercises

1. Draw crash points for commit-before, commit-after, and Kafka transactional consume-transform-produce.
2. Design a per-key ordering policy across main, retry, and quarantine topics.
3. Implement a database inbox schema with retention, tenant scope, and source-version checks.
4. Write a rebalance callback plan that drains work without exceeding the poll/liveness protocol.
5. Define dashboards for partition lag, processing latency, rebalance, producer errors, under-replication, and DLQ age.

## 11. Summary checklist

- [ ] Partition count and key balance order, skew, and parallelism.
- [ ] Producer durability is specified through replication and acknowledgement settings.
- [ ] Offset position, commit, and durable business effect are not conflated.
- [ ] Rebalances stop/fence revoked work and preserve safe recovery positions.
- [ ] At-most/at-least/exactly-once claims enumerate failure windows and scope.
- [ ] External effects are idempotent or deduplicated in a local atomic boundary.
- [ ] Schema compatibility includes semantics, not only registry validation.
- [ ] Retry and DLQ preserve or deliberately relax ordering with operations ownership.

## Primary references

- Apache Kafka Documentation: <https://kafka.apache.org/documentation/>
- Apache Kafka Producer configuration: <https://kafka.apache.org/documentation/#producerconfigs>
- Apache Kafka Consumer configuration: <https://kafka.apache.org/documentation/#consumerconfigs>
- Apache Kafka Design: <https://kafka.apache.org/documentation/#design>
- Spring for Apache Kafka Reference: <https://docs.spring.io/spring-kafka/reference/>

> **Version boundary:** consumer group protocols, assignment strategies, broker defaults, idempotence defaults, transaction behavior, partitioner behavior, Spring listener APIs, and retry facilities evolve. This chapter states conceptual contracts; verify exact broker/client/framework versions from their official documentation. Application Java baseline is 21.
