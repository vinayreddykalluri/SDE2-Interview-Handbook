# Structured Logs, Metrics, Traces, and SLO Engineering

## Learning objectives

After this chapter, you should be able to:

- design telemetry from questions and failure modes rather than logging everything;
- produce structured, correlated, redacted logs with bounded event volume;
- choose metric types and low-cardinality dimensions;
- explain Micrometer as an instrumentation facade/observation API and OpenTelemetry as a telemetry model, API/SDK, context, and protocol ecosystem;
- use RED for request-driven services and USE for resources;
- define an SLI with a precise eligible-event population, good-event rule, window, and source;
- calculate error budget and multi-window burn-rate intuition; and
- lead an incident from user impact through hypotheses, evidence, mitigation, validation, and prevention.

## 1. Observability is question-answering capacity

Telemetry is useful when it can answer:

- Are users failing or slow?
- Which route, tenant tier, region, version, or dependency changed?
- Is the bottleneck demand, CPU, memory, queue, connection, lock, network, storage, or downstream?
- Which request/event followed the failing path?
- Did mitigation restore the SLI?
- Can we reproduce and prevent the condition?

Logs, metrics, and traces are complementary:

- **metrics:** bounded-dimensional aggregates; cheap trend/alert foundation;
- **logs:** discrete structured events and diagnostic context;
- **traces:** causally related spans for sampled/distributed executions;
- **profiles/dumps:** code/runtime resource evidence when CPU, allocation, locks, or threads require depth.

Instrument semantic boundaries—request, queue, use case, database, broker, remote call—not every method. Telemetry consumes CPU, allocation, network, storage, and human attention; it needs budgets and failure behavior.

## 2. Structured logging

### Event contract

A structured log record can contain:

```json
{
  "timestamp": "2026-01-01T12:00:00Z",
  "level": "WARN",
  "service": "orders",
  "environment": "prod",
  "event": "payment_outcome_unknown",
  "traceId": "...",
  "orderId": "O-91",
  "provider": "primary",
  "elapsedMs": 287,
  "outcome": "TIMEOUT",
  "attempt": 1
}
```

Use stable field names/types and an event code. Message prose can change; automation should use structured outcome fields. Include IDs only when policy permits; avoid tokens, credentials, card data, full request/response, personal notes, SQL parameter dumps, and secrets in exception messages.

### Correlation and context

Trace context should propagate through supported HTTP/messaging mechanisms. A business operation ID/event ID can correlate retries across traces. Do not use trace ID as idempotency identity: trace sampling/lifetime semantics differ.

Thread-local logging context does not automatically cross executors/reactive pipelines/virtual-thread task boundaries correctly. Use framework-supported context propagation and clear scopes. Verify that asynchronous callbacks and Kafka records preserve only necessary context.

### Logging mistakes

- logging the same exception at repository, service, controller, and gateway;
- dynamic message templates that defeat grouping;
- metric-like high-volume success logs instead of counters;
- logging every retry as error when terminal operation succeeds;
- unbounded stack traces for expected validation;
- using raw URL/query strings containing IDs as route labels;
- swallowing a failure after logging without returning/recording an outcome;
- synchronous remote log append on the critical path without backpressure policy.

Log once at the boundary that owns classification, with enough causal context. Sampling must preserve rare critical errors according to policy.

## 3. Metrics and cardinality

### Metric types

- **counter:** monotonically increasing event count; derive rate over time;
- **gauge:** sampled current value such as queue depth; can jump and vanish with process;
- **histogram/distribution:** counts observations in buckets, enabling aggregate percentiles and SLO thresholds;
- **timer:** duration distribution plus count, represented by the instrumentation backend;
- **long-task timer/up-down counter:** active duration/in-flight work under supported model.

Never average precomputed percentiles across instances. Histograms can aggregate when bucket boundaries/model align. Client-side quantiles have different aggregation properties. Choose buckets around SLO and diagnostic thresholds.

### Cardinality budget

Time-series count roughly multiplies distinct label values:

```text
routes(40) * methods(5) * outcomes(8) * regions(4)
= 6,400 potential series before instances/status/etc.
```

Adding `customerId` with one million values explodes cost. Good dimensions are bounded: route template, operation, method, outcome class, dependency, region, version. Put high-cardinality request/order/event IDs in logs or sampled traces.

Metric names and units are an API. `request.duration` must define seconds/milliseconds through the telemetry convention, start/stop boundary, error/cancellation classification, and route fallback. Missing labels and renamed routes can break dashboards silently.

## 4. RED and USE

### RED for request/event-driven components

- **Rate:** requests/records per second;
- **Errors:** failures under the operation contract, separated from expected client outcomes;
- **Duration:** latency distribution, including queue time as defined.

Add in-flight and rejection because rate alone does not reveal queue/saturation. For Kafka, use input rate, processing failure, processing/end-to-end age, consumer lag, retry/DLQ, and rebalance.

### USE for resources

- **Utilization:** fraction/time resource is busy;
- **Saturation:** queued/waiting work beyond immediate service capacity;
- **Errors:** resource failures.

Examples:

| Resource | Utilization | Saturation | Errors |
|---|---|---|---|
| CPU | busy time | runnable queue/throttling | machine/runtime counters |
| DB pool | active/max | acquisition waiters/time | timeout/validation failures |
| disk | busy/throughput | queue latency/depth | I/O errors |
| executor | active capacity | queue depth/oldest age | rejection/task failure |
| heap/GC | occupancy/allocation context | allocation pressure/pause symptoms | OOM/allocation failure |

Interpret OS/JVM/container metrics using platform documentation. CPU “100%” may mean one core or quota depending on metric. Heap occupancy alone is not a memory leak proof.

## 5. Micrometer and OpenTelemetry mental model

### Micrometer

Micrometer provides instrumentation abstractions for meters and observations, with registries/handlers exporting to monitoring systems through supported modules. Spring Boot can auto-configure integrations. Application code should record semantic operation metrics, not depend on a vendor query language.

Dependency-requiring example:

```java
final class InventoryClient {
    private final MeterRegistry registry;

    InventoryResult reserve(Command command) {
        Timer.Sample sample = Timer.start(registry);
        String outcome = "success";
        try {
            return call(command);
        } catch (RuntimeException failure) {
            outcome = classify(failure); // bounded vocabulary
            throw failure;
        } finally {
            sample.stop(Timer.builder("inventory.request")
                    .tag("operation", "reserve")
                    .tag("outcome", outcome)
                    .register(registry));
        }
    }
}
```

Avoid registering meters with an unbounded tag on every call. Prefer framework observation conventions when they already capture request/client boundaries, and customize bounded fields.

### OpenTelemetry

OpenTelemetry defines APIs/SDK concepts for traces, metrics, logs, context propagation, semantic conventions, sampling, and OTLP export. A trace contains spans with parent/link relationships, attributes, events, status, and timing. Messaging may use span links when causal work is not a strict synchronous child.

Key boundaries:

- instrumentation API records telemetry without choosing backend;
- SDK/processors/samplers/exporters control collection/export;
- context propagators encode/decode cross-process context;
- collector can receive, process, sample, route, and export;
- backend stores/queries/visualizes.

Do not put secrets/PII or high-cardinality payloads into span attributes. Head sampling decides near trace start and may miss rare failures; tail sampling can use completed trace signals but requires collector buffering/capacity. Sampling changes diagnostic coverage; metrics remain the primary aggregate SLI source.

Instrumentation must fail safely. A slow exporter must not block the request indefinitely; bounded queues can drop telemetry under overload. Measure dropped spans/logs/metrics so “no evidence” is not mistaken for health.

## 6. SLI, SLO, and error budget

### Formal definitions

An SLI is a measured ratio or distribution tied to user experience. For request availability:

```text
SLI = good eligible requests / total eligible requests
```

Define:

- service and user journey;
- eligible population and exclusions;
- good-event rule (status plus latency threshold);
- measurement point (load balancer, service, synthetic, client);
- rolling/calendar window and data completeness;
- treatment of retries, cancellations, rate-limited abuse, planned maintenance, and missing telemetry.

Example: “Over a rolling 30 days, 99.9% of eligible `CreateOrder` requests accepted by the service boundary complete within 750 ms without a server-attributable terminal failure.” This still needs exact outcome mapping.

For target `S` over event count `N`, error budget is `(1-S)N`. For time-style intuition, 99.9% over 30 days corresponds to about 43.2 minutes, but request-based SLO budget depends on traffic and is usually more faithful for request services.

### Burn rate

Burn rate compares observed bad-event fraction to allowed bad fraction:

```text
burn = observedErrorRate / (1 - SLO)
```

For 99.9%, allowed fraction is 0.001. Observed 1% bad gives burn 10. At sustained burn 10, a 30-day budget is consumed in about 3 days. Multi-window alerts combine a fast window with a longer confirmation window: high burn catches severe incidents quickly; lower sustained burn catches slow erosion. Exact thresholds/windows follow organizational policy and traffic volume.

Avoid alerting directly on CPU absent user risk. Page on SLO burn or a condition likely to become imminent user impact; create tickets/dashboard signals for lower urgency. Every alert needs owner, runbook, and tested routing.

## 7. Operational diagnostic method

### Evidence loop

1. state user impact, scope, start time, and SLI change;
2. stabilize: freeze risky deploys, shed/degrade, rollback or isolate when evidence supports it;
3. compare recent changes: deployment, config, traffic, dependency, schema, partition, infrastructure;
4. use RED to locate affected operation/dependency;
5. use USE to test saturation hypotheses;
6. inspect traces/logs for representative failing paths;
7. collect JVM/database/broker evidence without destructive load;
8. change one reversible factor when possible;
9. verify SLI recovery and absence of displaced failure;
10. preserve timeline/evidence and define prevention with owner/date.

### Worked incident

Symptom: create-order p99 jumps from 300 ms to 4 s; error rate remains low, DB pool wait rises, CPU normal.

Hypotheses:

- query/lock duration increased;
- connections leaked/held during remote calls;
- pool/database capacity changed;
- traffic mix shifted to slow path.

Evidence:

- trace shows payment HTTP span occurs inside transaction span;
- pool active=max and wait p99 3 s;
- DB execution itself 40 ms; lock wait low;
- deploy introduced fraud call within transactional method.

Mitigation: roll back or move remote call outside local transaction using a durable workflow, then verify pool wait and request SLI. Prevention: transaction-duration metric, architecture test/review rule, integration load test, explicit workflow state.

Do not “fix” by only enlarging the pool; that can transfer saturation to the database and retain the flawed boundary.

## 8. Interview questions and model checkpoints

### Q1. Metrics versus logs versus traces?

**Model checkpoint:** metrics reveal aggregate rate/error/duration/saturation and alert; logs capture discrete structured events; traces connect sampled causal paths. Use together and respect cardinality/privacy/cost.

### Q2. What makes an SLI defensible?

**Model checkpoint:** user journey, eligible population, good rule, measurement point, window, exclusions, retry/cancellation policy, and data-quality monitoring.

### Q3. Why not tag metrics with order ID?

**Model checkpoint:** unbounded cardinality creates a series per value and cost/memory failure. Put IDs in controlled logs/traces; metrics use bounded route/outcome/dependency dimensions.

### Q4. RED versus USE?

**Model checkpoint:** RED starts from service operations (rate/errors/duration); USE starts from resources (utilization/saturation/errors). Correlate user impact with bottleneck evidence.

### SDE-2 follow-ups

1. Design telemetry for a saga spanning HTTP, outbox, Kafka, and three consumers.
2. A collector loses 40% of spans during overload. Explain how metrics still detect impact and how telemetry loss is surfaced.
3. Define a 99.95% latency/availability SLO with low-traffic statistical considerations.
4. Build a burn-rate alert policy and explain false positives from client errors/retries.

## 9. Exercises

1. Audit a metric namespace and calculate worst-case time-series cardinality.
2. Convert five prose logs into structured events and redact sensitive fields.
3. Write RED/USE dashboards for HTTP, DB pool, Kafka consumer, Redis, and JVM.
4. Define availability and freshness SLIs for a catalog served from cache.
5. Run a paper incident: p99 latency, pool saturation, Kafka lag, and normal CPU. Produce hypotheses and discriminating evidence.

## 10. Summary checklist

- [ ] Telemetry begins with user and operational questions.
- [ ] Logs are structured, correlated, redacted, and not duplicated at every layer.
- [ ] Metrics have correct types, units, boundaries, and bounded labels.
- [ ] Traces propagate context safely and sampling/export loss is observable.
- [ ] RED and USE connect impact to capacity evidence.
- [ ] SLI population, good rule, source, window, exclusions, and quality are explicit.
- [ ] Alerts use error-budget urgency and have owners/runbooks.
- [ ] Incident actions are evidence-based, reversible, and verified against the SLI.

## Primary references

- OpenTelemetry Specification: <https://opentelemetry.io/docs/specs/>
- OpenTelemetry Java Documentation: <https://opentelemetry.io/docs/languages/java/>
- Micrometer Documentation: <https://docs.micrometer.io/micrometer/reference/>
- Spring Boot Actuator Observability Reference: <https://docs.spring.io/spring-boot/reference/actuator/observability.html>
- Google SRE Workbook, “Alerting on SLOs”: <https://sre.google/workbook/alerting-on-slos/>

> **Version boundary:** semantic-convention names, Micrometer observation APIs, Spring auto-instrumentation, exporter behavior, and OpenTelemetry SDK defaults evolve. Pin and test the versions managed by the application. The conceptual SLI/SLO and cardinality contracts are version-independent; code baseline is Java 21.
