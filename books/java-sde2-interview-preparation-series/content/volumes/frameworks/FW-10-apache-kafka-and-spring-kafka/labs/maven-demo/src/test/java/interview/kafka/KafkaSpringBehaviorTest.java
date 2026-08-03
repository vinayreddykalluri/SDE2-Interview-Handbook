package interview.kafka;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import org.apache.kafka.clients.consumer.Consumer;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.clients.producer.MockProducer;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.common.header.internals.RecordHeader;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.apache.kafka.common.serialization.StringSerializer;
import org.junit.jupiter.api.Test;
import org.springframework.kafka.core.DefaultKafkaProducerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.listener.ContainerProperties;
import org.springframework.kafka.support.SendResult;
import org.springframework.kafka.test.EmbeddedKafkaBroker;
import org.springframework.kafka.test.context.EmbeddedKafka;
import org.springframework.kafka.test.utils.KafkaTestUtils;
import org.springframework.util.backoff.BackOffExecution;
import org.springframework.util.backoff.FixedBackOff;

@EmbeddedKafka(partitions = 2)
class KafkaSpringBehaviorTest {
    @Test
    void kafkaTemplatePreservesKeyAndPartitionOrder(EmbeddedKafkaBroker broker) throws Exception {
        String topic = "orders-" + UUID.randomUUID();
        broker.addTopics(topic);

        Map<String, Object> producerProperties = KafkaTestUtils.producerProps(broker);
        DefaultKafkaProducerFactory<String, String> producerFactory =
                new DefaultKafkaProducerFactory<>(
                        producerProperties, new StringSerializer(), new StringSerializer());
        KafkaTemplate<String, String> template = new KafkaTemplate<>(producerFactory);

        SendResult<String, String> first = template.send(topic, "order-42", "v1")
                .get(10, TimeUnit.SECONDS);
        SendResult<String, String> second = template.send(topic, "order-42", "v2")
                .get(10, TimeUnit.SECONDS);
        assertEquals(first.getRecordMetadata().partition(), second.getRecordMetadata().partition());
        assertTrue(second.getRecordMetadata().offset() > first.getRecordMetadata().offset());

        Map<String, Object> consumerProperties = new HashMap<>(
                KafkaTestUtils.consumerProps("group-" + UUID.randomUUID(), "false", broker));
        consumerProperties.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest");
        List<ConsumerRecord<String, String>> received = new ArrayList<>();
        try (Consumer<String, String> consumer = new KafkaConsumer<>(
                consumerProperties, new StringDeserializer(), new StringDeserializer())) {
            broker.consumeFromAnEmbeddedTopic(consumer, topic);
            long deadline = System.nanoTime() + Duration.ofSeconds(10).toNanos();
            while (received.size() < 2 && System.nanoTime() < deadline) {
                ConsumerRecords<String, String> records = consumer.poll(Duration.ofMillis(200));
                records.forEach(received::add);
            }
        } finally {
            template.destroy();
            producerFactory.destroy();
        }

        assertEquals(List.of("v1", "v2"), received.stream().map(ConsumerRecord::value).toList());
        assertEquals(1, received.stream().map(ConsumerRecord::partition).distinct().count());
    }

    @Test
    void mockProducerTransactionCommitsTwoRecords() {
        MockProducer<String, String> producer =
                new MockProducer<>(
                        true, null, new StringSerializer(), new StringSerializer());
        producer.initTransactions();
        producer.beginTransaction();
        producer.send(new ProducerRecord<>("out", 0, "order-42", "paid"));
        producer.send(new ProducerRecord<>("audit", 0, "order-42", "recorded"));
        producer.commitTransaction();

        assertTrue(producer.transactionCommitted());
        assertEquals(2, producer.history().size());
        producer.close(Duration.ofSeconds(1));
    }

    @Test
    void producerRecordCarriesStableBusinessIdentityHeaders() {
        ProducerRecord<String, String> record = new ProducerRecord<>(
                "orders", null, null, "order-42", "payload",
                List.of(new RecordHeader("eventId", "evt-7f3".getBytes(StandardCharsets.UTF_8)),
                        new RecordHeader("schemaVersion", "2".getBytes(StandardCharsets.UTF_8))));
        assertEquals("order-42", record.key());
        assertEquals("evt-7f3", new String(
                record.headers().lastHeader("eventId").value(), StandardCharsets.UTF_8));
        assertEquals("2", new String(
                record.headers().lastHeader("schemaVersion").value(), StandardCharsets.UTF_8));
    }

    @Test
    void springContainerAckModeIsExplicit() {
        ContainerProperties properties = new ContainerProperties("orders");
        properties.setAckMode(ContainerProperties.AckMode.MANUAL_IMMEDIATE);
        assertEquals(ContainerProperties.AckMode.MANUAL_IMMEDIATE, properties.getAckMode());
    }

    @Test
    void fixedBackoffCountsRetriesNotTotalAttempts() {
        BackOffExecution execution = new FixedBackOff(1_000, 2).start();
        assertEquals(1_000L, execution.nextBackOff());
        assertEquals(1_000L, execution.nextBackOff());
        assertEquals(BackOffExecution.STOP, execution.nextBackOff());
    }

    @Test
    void contiguousTrackerNeverCommitsPastAnIncompleteOffset() {
        KafkaMechanics.ContiguousOffsetTracker tracker =
                new KafkaMechanics.ContiguousOffsetTracker(10);
        assertEquals(10L, tracker.complete(11));
        assertEquals(12L, tracker.complete(10));
        assertEquals(12L, tracker.complete(13));
        assertEquals(14L, tracker.complete(12));
    }

    @Test
    void inboxRejectsDuplicateEventIds() {
        KafkaMechanics.IdempotentInbox inbox = new KafkaMechanics.IdempotentInbox();
        assertTrue(inbox.claim("evt-1"));
        assertFalse(inbox.claim("evt-1"));
        assertTrue(inbox.claim("evt-2"));
    }
}
