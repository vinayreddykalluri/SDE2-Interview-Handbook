# Spring Cloud: Configuration, Clients, Gateway, and Resilience Budgets

Spring Cloud is a family of projects, not a distributed-systems cure. Use it when a deployment genuinely needs shared configuration, service discovery, a gateway, managed service clients, or integration with a platform’s distributed capabilities.

## Start with topology

Before choosing a module, draw the call path:

```text
mobile client
    |
edge / load balancer
    |
API gateway
    |
order-service --HTTP--> payment-service
    |                       |
database               provider API
```

Mark who owns TLS termination, authentication, routing, service discovery, timeout, retry, load balancing, and observability. Duplicate ownership is a common production defect: a platform mesh retries, the gateway retries, and the client retries the same call.

## Release trains and dependency management

Spring Cloud components are released as a tested train. Import the official `spring-cloud-dependencies` BOM compatible with the chosen Spring Boot generation. Do not select individual transitive versions because one library appears newer.

For the Boot 4.0/4.1 generation, the official compatibility table maps to Spring Cloud 2025.1. Exact service releases change; verify the table and use the latest supported service release for your line.

## External configuration flow

```text
configuration source
      |
      v
config client bootstrap/import
      |
PropertySources ordered by precedence
      |
binding + validation
      |
immutable configuration object
```

Important edge cases:

- A remote source can be unavailable during startup. Decide whether the service must fail closed or may start from safe local defaults.
- Refreshable configuration can change behavior while requests are running. Not every property is safe to refresh.
- Precedence can let an environment variable override a remote value—or the reverse—depending on configuration.
- Secrets need a secret manager and access controls, not a public config repository.
- Configuration must be validated as a coherent set. A new timeout with an old retry count may violate the total request budget.

Prefer immutable, validated configuration properties over scattered string lookups.

## Discovery and load balancing

Discovery maps a logical service name to eligible instances. Client-side load balancing chooses one instance; platform DNS or a service mesh may already perform this role.

Ask:

1. How quickly does a new healthy instance appear?
2. How quickly is a failed instance removed?
3. Is the view stale or eventually consistent?
4. Does the client retry the same instance or choose another?
5. Does session affinity exist, and is the service truly stateless?

Discovery says where a service might be. It does not say the chosen instance will complete within the caller’s deadline.

## Gateway request flow

```text
request
  -> route predicate match
  -> ordered pre-filters
  -> downstream HTTP exchange
  -> ordered post-filters
  -> response
```

A gateway is useful for coarse cross-cutting concerns: routing, TLS, authentication handoff, rate limits, request-size limits, and consistent telemetry. It should not become a hidden business workflow engine.

Filter order matters. For example, authentication must establish identity before a tenant rate limiter uses it. Body-caching filters can consume significant memory. A fallback route can turn a clear 503 into stale or incorrect business data.

## Declarative clients do not remove HTTP

An interface-based client still performs DNS lookup, connection acquisition, TLS, serialization, network I/O, status mapping, and response-body consumption.

```text
service method
  -> client proxy
  -> request encoder/interceptors
  -> HTTP client connection pool
  -> network
  -> response decoder/error mapping
  -> return or exception
```

Define explicitly:

- connect timeout;
- connection-acquisition timeout;
- response/read timeout;
- maximum response/body size;
- error mapping;
- retry classification;
- propagation of trace and minimum safe identity context.

## One end-to-end timeout budget

Suppose the API must respond within 800 ms:

```text
gateway overhead             50 ms
order service local work    100 ms
payment call budget         500 ms
response reserve            150 ms
                           -------
total                        800 ms
```

Three payment attempts of 500 ms cannot fit. A retry policy must consume the remaining deadline, not reset a fresh timeout per attempt. Add backoff and jitter so replicas do not synchronize.

## Retry, circuit breaker, bulkhead, rate limit

| Mechanism | Protects against | Does not guarantee |
|---|---|---|
| Timeout/deadline | Waiting forever | Operation did not complete remotely |
| Retry | A classified transient failure | Side effect was not duplicated |
| Circuit breaker | Repeated calls to a failing dependency | Dependency recovery or data correctness |
| Bulkhead/concurrency limit | One dependency consuming all local capacity | Fairness or downstream capacity |
| Rate limit | Excess request rate at a boundary | Backend latency or per-key business correctness |
| Fallback | A deliberately degraded response | Fresh or semantically equivalent data |

### The ambiguous timeout

The client times out after sending a payment request. The server may have:

1. received nothing;
2. committed payment and lost the response;
3. still been processing when the connection closed.

Blind retry is unsafe. Send a stable idempotency key and design the payment API to return the original result for a duplicate key. If the provider lacks idempotency, reconcile before repeating the effect.

## Failure and edge-case matrix

| Scenario | Failure mode | Design response |
|---|---|---|
| Config server unavailable | Startup fleet outage | Explicit fail-fast/fallback policy and cached-safe config only if justified |
| Partial config refresh | Incoherent timeout/retry values | Bind and validate a versioned configuration set |
| Stale discovery entry | Connection failure | Short bounded retry to another instance if operation is safe |
| Gateway and client both retry | Multiplicative attempts | Assign one retry owner and emit attempt counts |
| Circuit opens on caller errors | Healthy dependency hidden | Classify only relevant failures/latency |
| Fallback returns empty list | Outage appears as “no data” | Mark degradation or fail explicitly |
| Bulkhead queue unbounded | Memory and latency collapse | Bound concurrency and queue; reject early |
| Token forwarded unchanged | Excess privilege downstream | Audience-bound token exchange or minimal trusted claims |
| Client reads huge error body | Memory pressure | Bound body and map status safely |

## Quick check

1. Why can nested retries make a three-attempt policy produce nine calls?
2. What does service discovery not guarantee?
3. What state makes a payment retry safe after a timeout?
4. Why should fallback data be visibly degraded?
5. How do release trains reduce upgrade risk?

## Practice

- **Foundation:** Draw one client call and label every timeout.
- **Interview Core:** Allocate a 600 ms request budget across two sequential dependencies.
- **Interview Core:** Classify connect reset, 400, 429, 503, and read timeout for retry.
- **SDE-2 Follow-up:** Design gateway and client ownership so auth, rate limiting, retry, and tracing do not execute twice.
