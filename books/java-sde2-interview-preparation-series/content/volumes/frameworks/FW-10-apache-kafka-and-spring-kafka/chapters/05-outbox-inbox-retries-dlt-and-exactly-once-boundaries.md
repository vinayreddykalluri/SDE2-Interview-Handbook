# Outbox, Inbox, Retries, DLT, and Exactly-Once Boundaries

## Database-to-Kafka dual-write problem

```text
commit database -> crash before Kafka send = missing event
send Kafka -> crash/DB rollback       = event for nonexistent state
```

There is no safe ordering of two independent commits by hope alone.

## Transactional outbox

Write domain state and an outbox row in one database transaction:

```sql
BEGIN;
UPDATE purchase_order SET status='PAID', version=version+1
WHERE order_id=? AND version=?;
INSERT INTO outbox(event_id, aggregate_id, aggregate_version,
                   event_type, payload, status)
VALUES (?, ?, ?, 'OrderPaid', ?, 'NEW');
COMMIT;
```

A publisher polls with locking/claiming or uses CDC, publishes with stable key/event ID, and marks progress. Crash after Kafka acknowledgement but before marking sent creates a duplicate publish; consumers still need idempotency.

Outbox retention, poison payloads, ordering per aggregate, multiple publishers, monitoring, and replay are production requirements—not optional cleanup.

## Kafka-to-database inbox

Store `(consumer, eventId)` with the business update in one DB transaction. Duplicate delivery becomes a unique conflict/prior success. Commit Kafka offset only after that transaction commits.

If events can arrive out of order despite same-key design (topic migration, multiple sources, manual replay), compare aggregate version and decide buffer, ignore older, or reconcile from source.

## Retry taxonomy

| Failure | Example | Response |
|---|---|---|
| transient dependency | DB connection/lock timeout | bounded exponential backoff with jitter |
| throttling/overload | downstream 429 | honor budget, pause/bulkhead |
| permanent payload | invalid schema/domain | quarantine/DLT |
| code bug | null pointer for valid event | stop/alert or bounded retry then DLT with ownership |
| unknown side-effect result | HTTP timeout after request | idempotency lookup/reconciliation |

Blocking retry preserves partition order but stops later records. Non-blocking retry topics free the main partition but change timing/order and create extra topics/consumers. Choose from ordering requirement and failure duration.

## DLT lifecycle

A dead-letter topic is a controlled failure queue. Include original metadata and safe diagnostics. Define:

- alerts and owner/SLA;
- retention and PII access;
- classification and repair tool;
- replay key/order/destination;
- deduplication and maximum replay attempts;
- success verification and audit trail.

Publishing to DLT can fail. Decide whether to stop, retry, or retain the original offset; never silently commit and lose evidence.

## Kafka exactly-once processing

For Kafka read → transform → Kafka write, a transaction can atomically publish outputs and consumed offsets. Requirements include idempotent/transactional producer, stable transactional IDs, transactional listener/container integration, and downstream `read_committed` where aborted records must be hidden.

It does not make a database update atomic. Some frameworks coordinate transaction managers in an order, but without distributed atomic commit a crash window remains. Use outbox/inbox/compensation and explain the window.

## Side-effect state machine

For a payment request:

```text
RECEIVED -> CLAIMED -> PROVIDER_UNKNOWN -> SUCCEEDED/FAILED
```

Persist provider idempotency key and response/reference. A timeout moves to unknown, then a reconciliation worker queries provider status. Retrying with a new key is forbidden.

## Practice and solutions

- **Foundation:** Draw the two DB/Kafka dual-write failure orders.
- **Interview Core:** Implement outbox publisher idempotency and lag metric.
- **SDE-2 Follow-up:** Compare blocking retry and retry topic for account events. Preserve per-account order with blocking/pause or an ordered state machine; retry topics explicitly sacrifice immediate order and need version reconciliation.
