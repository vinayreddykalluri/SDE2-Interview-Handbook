# Kafka and Spring Kafka Volume Labs

This Java 21 fixture combines deterministic offset/idempotency models, Kafka client mocks, Spring listener configuration, and a JVM-local single-broker KRaft integration test. It validates keyed ordering within one partition, Spring `KafkaTemplate` send acknowledgement, explicit acknowledgement mode, transaction behavior in `MockProducer`, bounded backoff, and contiguous offset tracking.

It cannot validate multi-broker replication, ISR/minimum ISR, failover, quotas, security, transactions across broker loss, partition reassignment, or production throughput. Those require a target multi-broker cluster.

```bash
bash content/volumes/frameworks/FW-10-apache-kafka-and-spring-kafka/labs/validate_kafka_labs.sh
```
