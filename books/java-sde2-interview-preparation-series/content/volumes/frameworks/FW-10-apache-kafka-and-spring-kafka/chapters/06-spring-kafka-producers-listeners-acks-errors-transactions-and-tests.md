# Spring Kafka: Producers, Listeners, Acknowledgements, Errors, Transactions, and Tests

Spring Kafka wraps Kafka clients with `KafkaTemplate`, listener containers, conversion, error handlers, transactions, and test utilities. Start from the client contract, then configure the abstraction explicitly.

## Producer boundary

```java
CompletableFuture<SendResult<String, OrderPaid>> result =
        kafkaTemplate.send("order-events", orderId, event);

result.whenComplete((sent, failure) -> {
    if (failure != null) {
        // record classified failure; outbox remains pending
    }
});
```

Do not fire-and-forget without observing the future. The callback may run on client/framework threads; do not block it with slow I/O. An outbox publisher marks sent only after success and still tolerates duplicate publish after a crash.

## Listener boundary

```java
@KafkaListener(topics = "order-events", groupId = "billing-v2")
public void onOrderPaid(ConsumerRecord<String, OrderPaid> record) {
    billingService.applyIdempotently(
            record.value().eventId(),
            record.key(),
            record.value());
}
```

Container acknowledgement modes determine when offsets are committed relative to records/batches/manual acknowledgement. Version-specific names/semantics should be verified in the Spring Kafka 4.1 reference. Select a mode only after the database side effect and error behavior are defined.

Manual acknowledgement does not create exactly once; acknowledging before async work is durable loses records. With asynchronous processing, track contiguous completed offsets per partition and pause/resume safely.

## Deserialization failures

A normal listener may never receive a record that fails deserialization. Configure error-handling deserializers or byte-level/quarantine paths so metadata and raw safe payload can be diagnosed. Bound payload logging and protect PII.

## `DefaultErrorHandler`

Classify retryable and nonretryable exceptions, configure bounded backoff, and use `DeadLetterPublishingRecoverer` when appropriate. Validate whether recovered offsets are committed and what happens when DLT publication fails.

```java
DeadLetterPublishingRecoverer recoverer =
        new DeadLetterPublishingRecoverer(template,
                (record, exception) ->
                        new TopicPartition(record.topic() + ".DLT", record.partition()));

DefaultErrorHandler handler =
        new DefaultErrorHandler(recoverer, new FixedBackOff(1_000L, 2L));
```

Two retries means three total attempts in this example. Infinite retry is rarely acceptable.

## Non-blocking retry topics

`@RetryableTopic`/retry-topic configuration forwards failed records through delayed retry topics. It improves main-topic progress for longer failures but loses the original topic’s ordering relationship and, in current Spring Kafka documentation, does not combine with container transactions. Batch-listener support also differs. Label and test the selected version.

## Transactions

Spring can configure transactional `KafkaTemplate` producers and transactional listener containers. For read-process-write Kafka flows, container transactions coordinate output sends and offsets. If the listener throws, transaction rollback and after-rollback processing determine seeks/recovery.

A `@Transactional` database method plus Kafka transaction manager is not automatically a distributed atomic transaction. State commit order and crash window, or use outbox/inbox.

## Testing

- unit test event mapping/idempotency/error classification;
- `MockProducer`/`MockConsumer` for client decisions;
- embedded KRaft broker for real protocol/topic/group behavior;
- target cluster for security, quotas, multi-broker replication/failover;
- fault injection for rebalance, DLT failure, DB commit/offset crash.

Assert key/partition, headers/schema, event ID, offset commit point, duplicate result, retry count/backoff, DLT metadata, and lag recovery—not just “listener method called.”

## Spring edge matrix

| Mistake | Failure |
|---|---|
| ignore send future | silent/late publish failure |
| DLT every exception | transient outages become data quarantine |
| async handler + early ack | data loss |
| retry topic for ordered workflow | later event overtakes earlier |
| DB + Kafka annotations assumed atomic | dual-commit crash window |
| raw payload logging | security/PII exposure |

## Practice

- **Foundation:** Explain the `KafkaTemplate` future and broker acknowledgement.
- **Interview Core:** Configure classified bounded retry plus DLT ownership.
- **SDE-2 Follow-up:** Write a test that crashes after DB commit but before offset commit and proves inbox deduplication.
