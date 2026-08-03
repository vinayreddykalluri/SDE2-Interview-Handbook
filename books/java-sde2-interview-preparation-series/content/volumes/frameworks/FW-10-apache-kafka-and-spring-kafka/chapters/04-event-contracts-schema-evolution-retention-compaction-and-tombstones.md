# Event Contracts, Schema Evolution, Retention, Compaction, and Tombstones

## Event versus command

- **Event:** immutable fact in past tense (`OrderPaid`). Consumers choose reactions.
- **Command:** request for an owner to act (`CapturePayment`). It may be rejected and often has one logical handler.

Avoid ambiguous “OrderUpdate” payloads. Include stable identity, type, occurrence time, aggregate/version, tenant, and domain data. Transport metadata (topic, partition, offset) is not a business event ID.

## Schema formats

JSON is readable but needs external validation/compatibility discipline. Avro, Protobuf, and JSON Schema plus a registry can enforce typed evolution and schema IDs. The best format depends on ecosystem, performance, language, governance, and evolution needs—not interview fashion.

## Compatibility

Backward compatibility asks whether new consumers can read old data; forward compatibility asks whether old consumers can read new data. Full/transitive modes tighten combinations across history.

Common safe changes under many schemas:

- add optional/defaulted field;
- stop writing a field only after all readers tolerate absence;
- introduce new event type rather than change meaning;
- preserve numeric range and enum unknown handling.

Renaming is remove-plus-add unless aliases/migration are supported. Changing semantic meaning while retaining a field name is the most dangerous “compatible” change because syntax checks pass.

## Consumer robustness

Consumers should ignore unknown fields when the contract permits, preserve unknown enum behavior, validate required business invariants, and quarantine truly unreadable events. Do not silently default a missing currency or tenant if that changes money/security meaning.

## Deletion retention

Delete retention removes old segments by time/size. It supports replay only within the retained window. Topic storage is not an archival strategy unless retention/capacity/recovery are designed for it.

## Log compaction

Compaction retains at least the latest known value per key over time while preserving record order/offset structure for retained records. Cleanup is asynchronous; old versions can remain for a while. Compaction is not a real-time unique-key table lookup and does not eliminate consumer duplicate handling.

A keyed null value (tombstone) represents deletion for compacted state. Tombstones themselves have retention/cleanup timing. Consumers building state must interpret deletion explicitly.

Never use null key for compacted state that needs per-entity latest value; the key is the identity.

## Snapshot plus changelog

A consumer rebuilding state can restore a snapshot and replay from a recorded offset, or consume a compacted changelog from the beginning. Define atomicity between snapshot contents and offset, schema compatibility, tombstones, and validation/checksum.

## Sensitive data

Kafka retention and replication spread event data. Minimize PII/secrets, encrypt in transit/at rest, enforce ACLs, redact logs/DLTs, and plan deletion requirements. Encrypting a field complicates filtering/debugging/key rotation; avoid publishing unnecessary sensitive data first.

## Contract edge matrix

| Change | Hidden risk | Safer rollout |
|---|---|---|
| add required field | old records/producers lack it | optional/default then migrate |
| reuse enum value | semantic corruption | new value/type and explicit mapping |
| rename topic immediately | mixed deploy loses traffic | dual publish/bridge with reconciliation |
| compact with null keys | no per-entity collapse | stable business key |
| DLT raw payload | PII retention leak | access/retention/redaction policy |

## Practice and solutions

- **Foundation:** Add an optional delivery note compatibly.
- **Interview Core:** Evolve amount from `int` to larger range without breaking readers; introduce long-compatible schema/version and test every reader before values exceed old range.
- **SDE-2 Follow-up:** Design GDPR deletion for a compacted customer projection; minimize original event data, emit keyed tombstones, control backups/DLTs, and verify downstream deletion rather than promising immediate compaction.
