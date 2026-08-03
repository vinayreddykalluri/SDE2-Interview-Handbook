# Outbound HTTP Clients, Timeouts, and Resilience Boundaries

Calling another service is not a method call with latency. It crosses a network boundary where requests may be delayed, duplicated, partially processed, rejected, or answered after the caller gives up.

## Use configured builders

Boot supplies configured builders for supported clients. In a blocking MVC service, `RestClient` is a clear default:

```java
@Bean
PaymentClient paymentClient(RestClient.Builder builder,
                            PaymentProperties properties) {
    RestClient client = builder
            .baseUrl(properties.baseUrl().toString())
            .build();
    return new PaymentClient(client);
}
```

```java
final class PaymentClient {
    private final RestClient client;

    PaymentClient(RestClient client) {
        this.client = client;
    }

    PaymentResult authorize(PaymentRequest request) {
        return client.post()
                .uri("/authorizations")
                .body(request)
                .retrieve()
                .body(PaymentResult.class);
    }
}
```

Boot 4.1 also supports focused client starters and HTTP service interfaces. A declarative interface reduces repetitive mapping but does not remove timeout, retry, authentication, error, or observability decisions.

## Timeout taxonomy

| Timeout | Bounds |
|---|---|
| connection acquisition | waiting for a pool connection |
| connect | establishing TCP/TLS connection |
| response/read | waiting for response data |
| overall request/deadline | total operation budget |

An infinite default is not resilience. Choose budgets from the caller's service-level objective and leave time for fallback/error translation.

## Retry decision

Retry only when all are true:

1. failure may be transient;
2. operation is idempotent or protected by an idempotency key;
3. enough deadline remains;
4. attempt count/backoff/jitter are bounded;
5. retry load will not amplify an outage.

Do not retry validation errors, authentication failures, or deterministic business rejection. Treat 429/503 according to contract and `Retry-After` where applicable.

## Circuit breaker and bulkhead

A circuit breaker prevents repeated calls when failure evidence crosses a threshold; it is not a replacement for timeouts. A bulkhead limits concurrent work so one slow dependency cannot consume every request thread or connection.

Boot does not make an arbitrary resilience library correct automatically. Metrics must distinguish original requests, attempts, timeouts, rejected bulkhead calls, open-circuit calls, and final outcomes.

## Error translation

Translate transport details at the adapter boundary:

```text
404 payment account -> domain not-found if contract says so
409 provider conflict -> stable application conflict
429/503 -> temporary dependency failure with retry metadata
malformed response -> integration contract failure
timeout -> outcome unknown, not automatically failed
```

Never return a provider's raw body to public clients.

## Transaction interaction

Avoid holding a database transaction while waiting for a remote response. It increases lock and pool duration and still cannot make the remote call atomic with the database commit. Use explicit workflow states, outbox/inbox, compensation, or a saga where needed.

## Common mistakes

- Creating a new HTTP client per request.
- Setting only a connect timeout.
- Retrying POST without idempotency.
- Retrying at gateway, service, client, and library layers simultaneously.
- Catching every exception and returning an empty success.
- Logging authorization headers or full sensitive bodies.
- Treating a timeout as proof the provider did nothing.

## Interview angle

**Interviewer:** Payment timed out. Should the order retry immediately?

**Strong answer:** A timeout makes the outcome unknown. I query by the same idempotency key or use a provider status API before creating a second authorization. Any retry is bounded by the end-to-end deadline and protected against duplication. I keep the database transaction short and persist an explicit pending state for recovery.

## Quick check

1. Name four timeout boundaries.
2. When is retry safe?
3. What problem does a bulkhead solve?
4. Why is timeout an unknown outcome?
5. Why avoid remote calls inside database transactions?

## Practice

- **Foundation:** Configure explicit connect and response timeouts.
- **Interview Core:** Classify HTTP failures as retryable or final.
- **Interview Core:** Design metrics for requests versus attempts.
- **SDE-2 Follow-up:** Build a recovery workflow for unknown payment outcomes.
