# Producers: Keys, Batching, Acknowledgements, Idempotence, and Transactions

## Producer request flow

```text
send(record)
 -> serialize key/value/headers
 -> fetch/use metadata
 -> partition selection
 -> per-partition accumulator batch
 -> sender transmits ProduceRequest
 -> leader append + replication requirement
 -> callback/future success or failure
```

`send` success normally means the configured broker acknowledgement was received, not that a consumer processed the event.

## Keys and partitioning

Choose a key for the ordering/ownership boundary: order ID, account ID, or customer ID. A key with extreme skew creates a hot partition; randomizing it sacrifices ordered processing. Sometimes shard a high-volume aggregate by a subkey and carry sequence/version for downstream reconciliation.

Custom partitioners become a long-lived compatibility contract. Rolling producers with different partition logic can split one key’s events.

## Batching and compression

Producer accumulators batch per partition. `batch.size`, `linger.ms`, compression, buffer memory, and in-flight requests trade latency, throughput, CPU, and memory. A low-traffic partition may not fill a batch. Compression works better across similar records; broker stores/transfers batches efficiently.

Never tune only events/sec. Observe record size, compression ratio, request rate/size, buffer wait, errors/retries, and p99 acknowledgement latency.

## Acknowledgements

- `acks=0`: producer does not wait for broker acknowledgement; loss visibility is weak.
- `acks=1`: leader acknowledges without waiting for all required replicas.
- `acks=all`: leader waits for the current required in-sync replicas; combine with suitable replication/min ISR.

Durability also depends on broker/topic configuration and failure mode. Stronger acknowledgement may reject writes during insufficient ISR—a deliberate availability trade-off.

## Retries and idempotent producer

Without idempotence, a response can be lost after append and retry can duplicate. Idempotent producer protocol uses producer identity/epoch and per-partition sequence numbers to deduplicate supported retries and preserve ordering under configured constraints.

It does not deduplicate a new application request with a new producer/session, nor a consumer’s database side effect. Keep event IDs/business idempotency.

Timeouts include delivery budget, request timeout, metadata/serialization/buffer wait. A future failure does not always mean the broker stored nothing; classify the exception and preserve the event ID/outbox state.

## Kafka transactions

A transactional producer can atomically publish records across partitions/topics and commit consumed offsets as part of a consume-transform-produce transaction. Consumers using `read_committed` hide aborted transactional records.

```text
begin transaction
  send output records
  send consumer offsets to transaction
commit transaction
```

Stable unique `transactional.id` ownership enables fencing of old producer instances. Mismanaging identity across replicas can fence healthy producers or allow conflicts.

Kafka transactions do not include arbitrary database writes. For DB-to-Kafka use a transactional outbox; for Kafka-to-DB use idempotent inbox/business writes and commit offsets after durable effect.

## Producer failure matrix

| Failure | Outcome | Response |
|---|---|---|
| serialization error | no broker send | fix/quarantine invalid event |
| buffer exhausted | backpressure/timeout | bound producers and tune/load shed |
| insufficient ISR | write rejected under durability policy | wait/retry within delivery budget, alert |
| ack lost after append | result unknown | idempotent retry/outbox reconciliation |
| producer fenced | transaction identity conflict/old instance | stop and fix ownership |
| oversized record | broker/client rejection | shrink/externalize payload |

## Practice

- **Foundation:** Explain `acks=all` with replication factor 3 and min ISR 2.
- **Interview Core:** Select a key for account balance events and handle one hot account.
- **SDE-2 Follow-up:** Design DB commit plus publish using an outbox, stable event ID, idempotent producer, and publisher checkpoint.
