# Kafka/Spring Kafka Live Interviews, Rapid Q&A, Practice, and Sources

## Live interview 1: ordering

**Interviewer:** “Guarantee order status events are processed in order.”

**Candidate:** “Key by order ID so one order maps to one partition, process that partition serially, and carry aggregate version to reject/reconcile stale events. Ordering is only per partition. I would control partition-count/partitioner migrations because they can remap keys.”

## Live interview 2: DB plus publish

**Interviewer:** “Update order and publish `OrderPaid` atomically.”

**Candidate:** “Write order and outbox in one MySQL transaction. A poller/CDC publisher sends stable event ID/key and marks progress. Crash after publish can duplicate; consumers use an inbox/idempotent effect. A Kafka transaction alone cannot include the database commit.”

## Live interview 3: duplicate charge

**Interviewer:** “Consumer charged twice.”

**Candidate:** “It likely crashed after provider/DB effect and before offset commit. Persist event ID/provider idempotency key and business update in one durable state machine/inbox. On unknown provider timeout reconcile with the same key. Offset commits after the durable decision.”

## Live interview 4: poison record

**Interviewer:** “One malformed record blocks a partition.”

**Candidate:** “Classify deserialization versus transient dependency versus code bug. Capture raw bytes safely before normal conversion, use bounded attempts, publish metadata to a DLT/quarantine with an owner, then commit only after recovery publication succeeds. Provide repair/replay tooling and preserve key/order implications.”

## Live interview 5: lag spike

**Interviewer:** “Lag is one million; add consumers?”

**Candidate:** “First inspect lag per partition, ingress/processing rates, key skew, retries, rebalances, and downstream latency. More consumers help only up to partition count and not one hot partition. I would protect dependencies, estimate drain time, and verify idempotency during accelerated catch-up.”

## Live interview 6: exactly once

**Interviewer:** “Kafka says exactly once. Are payments exactly once?”

**Candidate:** “Kafka transactions can atomically commit Kafka outputs and input offsets for supported read-process-write flows with `read_committed`. The payment API is outside that transaction. Payments need provider idempotency, durable state, reconciliation, and inbox/outbox handling.”

## Live interview 7: Spring retry topics

**Interviewer:** “Use `@RetryableTopic` for account events?”

**Candidate:** “Non-blocking retry frees the main partition but a later account event can overtake the failed one; current Spring Kafka also does not combine it with container transactions. If account order is invariant, use blocking retry/pause with bounded outage handling or a versioned state machine that safely reconciles reorder.”

## Rapid answered questions

1. **Ordering scope?** One partition, not the topic globally.
2. **Offset meaning?** Position in one partition; commit normally stores the next record to read.
3. **More consumers than partitions?** Extra members are idle for that assignment.
4. **Why key?** Stable routing/order boundary; null keys do not preserve entity order.
5. **Does RF 3 guarantee no loss?** No; acknowledgement, ISR, election, durability, and failures matter.
6. **`acks=all` means every replica?** It waits for required current in-sync replicas under configuration, not every configured replica regardless of health.
7. **Idempotent producer scope?** Deduplicates supported producer retries; not arbitrary app requests/consumer effects.
8. **At-least-once window?** Effect succeeds, crash before offset commit, record repeats.
9. **At-most-once window?** Commit first, crash before effect, record lost to that group.
10. **What is rebalance?** Partition ownership changes across group members.
11. **Why `max.poll.interval`?** Bounds time between polls/progress before membership is reconsidered.
12. **Outbox purpose?** Atomically records domain change and publication intent in one DB.
13. **Inbox purpose?** Deduplicates a consumer effect in the same DB transaction.
14. **DLT equals success?** No; it is observable quarantine requiring repair/replay ownership.
15. **Compaction keeps only one record immediately?** No; cleanup is asynchronous and old versions can remain.
16. **Tombstone?** Keyed null value representing deletion in compacted state.
17. **Can partitions be increased safely?** Capacity yes, but key mapping/order compatibility must be planned.
18. **Spring manual ack equals exactly once?** No; commit/effect timeline still determines loss/duplicates.
19. **Retry topic preserves order?** Not relative to later main-topic records.
20. **Consumer lag root cause?** It is a symptom; examine rate, skew, dependencies, errors, and membership.

## Cumulative assessment

Design an order-event platform: topic/partition/key/RF/retention, schema compatibility, outbox publisher, idempotent billing consumer, retry/DLT, Spring configuration, 10× load capacity, observability, security, replay, and broker/DB outage timelines.

**Strong solution:** quantifies bytes/partitions, preserves per-order versions, explains acknowledgement and offset windows, uses outbox/inbox, classifies failures, treats DLT as owned work, separates Kafka EOS from external effects, and validates multi-broker behavior.

## Authoritative references

- [Apache Kafka documentation](https://kafka.apache.org/documentation/): design, producer, consumer groups, replication, transactions, configuration, security, and operations.
- [Spring for Apache Kafka 4.1 reference](https://docs.spring.io/spring-kafka/reference/kafka.html): templates, containers, acknowledgement, transactions, error handlers, retry topics, DLT, testing, and observability.
- Schema-format/registry documentation selected by the deployment.

The JVM-local lab validates client and Spring configuration behavior. A target multi-broker/KRaft environment remains required for replication, ISR, controller, failover, security, quotas, and reassignment claims.
