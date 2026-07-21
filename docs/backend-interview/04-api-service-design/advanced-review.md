# Advanced Review: API and Service Contracts

## Advanced model

| Area | Required depth |
| --- | --- |
| Contract evolution | Additive change, versions, deprecation, compatibility tests |
| Idempotency | Scope, fingerprint, concurrency, retention, response replay |
| Long-running work | Async command, status, cancellation, callback/event |
| Streaming | Flow control, ordering, resume, schema, backpressure |
| Webhooks | Signing, replay prevention, retry, deduplication, status |
| Authorization | Identity, delegation, resource policy, tenant boundary |
| Errors | Stable codes, retryability, partial success, correlation |
| Decomposition | Cohesion, coupling, ownership, migration, operational cost |

## Reconstruction exercise

Write contracts from memory for a retryable payment command and paginated order query. Include headers, validation, errors, idempotency, authorization, timeout, rate limit, and versioning.

## Drill ladder

- **Foundation:** design a resource-oriented CRUD contract.
- **SDE-2:** add retries, concurrency, pagination, and stable errors.
- **Advanced:** add bulk partial success, async completion, and webhooks.
- **Evolution:** migrate an endpoint without breaking clients.

## Retrieval prompts

1. Why is HTTP method idempotence insufficient for business operations?
2. What must be stored for reliable idempotency replay?
3. When can cursor pagination duplicate or omit results?
4. How does a client distinguish retryable failure?
5. What is backward compatible for request and response fields?
6. How do gRPC deadlines propagate downstream?
7. Which boundary owns object-level authorization?
8. When does a service boundary increase coupling?

## Exit criteria

Produce and defend one synchronous, one asynchronous, and one event contract, including reliability, security, compatibility, and migration.
