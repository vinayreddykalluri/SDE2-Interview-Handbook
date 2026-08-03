# Kafka and Spring Kafka Code Validation — Backend Wave 3

## Results

- Dependency-free companion: **1/1 strict Java 21 compile and execution pass**.
- Spring Kafka/Kafka tests: **7 passed, 0 failed/errors/skipped**.
- Observed output: `KafkaInterviewCompanion checks passed`.
- Runtime resolved Spring Kafka 4.1 APIs and Kafka clients/test broker 4.2.1; embedded KRaft metadata formatted and started successfully.

```bash
bash content/volumes/frameworks/FW-10-apache-kafka-and-spring-kafka/labs/validate_kafka_labs.sh
```

Tests prove two same-key records retain one partition and increasing offsets through `KafkaTemplate`, mock transaction commit, identity headers, explicit Spring acknowledgement mode, two-retry backoff semantics, contiguous offset commits, and inbox deduplication. SLF4J used its no-provider test fallback; this is a logging warning, not failure.

## Exact root source array

Use `series_native: true` in this order:

```text
content/volumes/frameworks/FW-10-apache-kafka-and-spring-kafka/chapters/00-learning-path-event-log-and-delivery-first-principles.md
content/volumes/frameworks/FW-10-apache-kafka-and-spring-kafka/chapters/01-broker-log-partitions-replication-and-storage-flow.md
content/volumes/frameworks/FW-10-apache-kafka-and-spring-kafka/chapters/02-producer-keys-batching-acks-idempotence-and-transactions.md
content/volumes/frameworks/FW-10-apache-kafka-and-spring-kafka/chapters/03-consumers-groups-offsets-polling-rebalances-and-delivery.md
content/volumes/frameworks/FW-10-apache-kafka-and-spring-kafka/chapters/04-event-contracts-schema-evolution-retention-compaction-and-tombstones.md
content/volumes/frameworks/FW-10-apache-kafka-and-spring-kafka/chapters/05-outbox-inbox-retries-dlt-and-exactly-once-boundaries.md
content/volumes/frameworks/FW-10-apache-kafka-and-spring-kafka/chapters/06-spring-kafka-producers-listeners-acks-errors-transactions-and-tests.md
content/volumes/frameworks/FW-10-apache-kafka-and-spring-kafka/chapters/07-capacity-observability-security-and-incident-response.md
content/volumes/frameworks/FW-10-apache-kafka-and-spring-kafka/chapters/08-live-interviews-rapid-qa-practice-solutions-and-sources.md
```

```json
"code_companion": {
  "path": "content/volumes/frameworks/FW-10-apache-kafka-and-spring-kafka/code/KafkaInterviewCompanion.java",
  "title": "Java 21 Kafka Delivery Reasoning Companion",
  "description": "Executable models for record identity, contiguous offset commits, idempotent inbox effects, failure classification, consumer parallelism, and aggregate ordering."
}
```

Set `publication_status: "published"`, `volume_label: "Publication Edition"`, `min_pages: 22`, and `max_pages: 90`.

After root integration:

```bash
python3 scripts/validate_series.py --source-only
python3 scripts/build_series.py --volume KAFKA
python3 scripts/validate_series.py
```

The central validator skips planned volumes, so status and companion mapping are both required. Root must visually inspect the rebuilt PDF/web output.
