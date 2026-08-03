# Metrics, Observations, Traces, and Structured Logging

Observability is evidence for answering a question, not the number of signals emitted. Boot integrates Micrometer metrics and observations, tracing bridges, structured logging, and Actuator endpoints; the application still owns useful names, cardinality, sampling, and privacy.

## Signal roles

| Signal | Best for |
|---|---|
| metrics | rates, ratios, saturation, alerts, trends |
| traces | one request across components/services |
| logs | discrete events and rich diagnostic context |
| profiles/dumps | CPU, allocation, threads, memory incidents |

Do not use logs as a high-volume metric store or add every business ID as a metric tag.

## Low-cardinality metrics

```java
@Component
final class OrderMetrics {
    private final Counter accepted;

    OrderMetrics(MeterRegistry registry) {
        this.accepted = Counter.builder("orders.accepted")
                .description("Orders accepted by the application")
                .register(registry);
    }

    void recordAccepted() {
        accepted.increment();
    }
}
```

Good tags have bounded values: operation, outcome, region, dependency. Bad tags are order ID, user ID, raw URL, exception message, or timestamp. Cardinality consumes memory and backend cost.

## Observation wraps one operation

An observation can create metrics and tracing spans through configured handlers. Instrument around an application boundary:

```java
return Observation.createNotStarted("payment.authorize", registry)
        .lowCardinalityKeyValue("provider", providerName)
        .observe(() -> gateway.authorize(command));
```

Never put credentials, card data, request bodies, or unbounded identifiers into observation tags.

## Trace propagation

Trace context must cross supported HTTP or messaging instrumentation. New unmanaged threads, manual executors, and asynchronous boundaries can lose it. Verify propagation with an integration test and logs containing trace identifiers.

Sampling means not every request has a complete trace. Alerts and correctness must not depend on trace presence.

## Structured logs

```text
timestamp level service environment event outcome trace_id message
```

Use stable event names and fields. Avoid concatenated prose as the only representation. Log one failure at the layer that owns the response; repeated stack traces at every layer increase noise.

Redact authentication headers, tokens, cookies, secrets, personal data, and sensitive payloads. Hashing an identifier can still be personal data if it remains linkable.

## Service-level evidence

For an API, start with:

- request rate by normalized route and outcome;
- latency distribution, not only averages;
- error ratio by controlled category;
- executor and connection-pool saturation;
- dependency attempts, timeouts, and circuit state;
- readiness state and restart count;
- business acceptance/rejection counts with bounded dimensions.

## Common mistakes

- Tagging metrics with raw path IDs.
- Alerting on a single instance rather than user impact.
- Logging the same exception five times.
- Assuming trace context crosses every executor automatically.
- Recording high-cardinality exception messages.
- Publishing `prometheus` or log-management endpoints publicly.

## Interview angle

**Interviewer:** Latency rose. Which signal do you inspect first?

**Strong answer:** I confirm user-impacting latency by normalized route and percentile, then correlate saturation and dependency timing. Traces identify where representative requests spent time; logs explain discrete failures. I compare request rate, error rate, pool/executor queues, database latency, and deployment changes before attributing cause.

## Quick check

1. Why are averages weak for tail latency?
2. What is metric cardinality?
3. What does an observation produce?
4. Why can trace sampling not prove absence?
5. Which data must be redacted?

## Practice

- **Foundation:** Define four low-cardinality tags.
- **Interview Core:** Repair a metric tagged by customer ID.
- **Interview Core:** Correlate a timeout across metric, trace, and log evidence.
- **SDE-2 Follow-up:** Design alerts for latency, errors, and saturation without paging on harmless noise.
