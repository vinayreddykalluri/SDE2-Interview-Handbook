# Realistic Spring Ecosystem Interview Rounds

These are dialogue chains, not flash cards. Pause after each interviewer line and answer aloud. A strong SDE-2 answer clarifies the invariant, traces the runtime, names the failure window, makes a trade-off, and proposes proof.

## 1. Two security chains

**Interviewer:** `/api/admin/report` is unexpectedly accessible without authentication. Two `SecurityFilterChain` beans exist. Diagnose it.

**Candidate:** I would first log or inspect which chain matched that exact method and path. `FilterChainProxy` selects the first matching chain; it does not merge both. A broad `/**` chain ordered before `/api/admin/**` can shadow the admin chain. I would make matchers non-overlapping where possible, put the narrow chain first, and add tests for public, authenticated, and denied admin requests.

**Interviewer:** Would adding `@PreAuthorize` fix it?

**Candidate:** It gives defense in depth at the service boundary if method security is enabled and the call crosses the proxy, but it does not make the public request-chain mistake acceptable. I would fix both boundaries and keep the domain policy independent of the annotation.

## 2. Valid JWT, still 401

**Interviewer:** The token’s signature is valid. Why reject it?

**Candidate:** Signature verifies integrity under a key, not suitability for this API. I still validate trusted issuer, intended audience, expiry and not-before with controlled clock skew, accepted algorithms, and any required tenant/client claims. I also inspect key rotation and authority mapping. I would not paste the raw production token into shared logs.

**Interviewer:** What is the 403 follow-up?

**Candidate:** If authentication succeeded but the principal lacks route or resource permission, authorization should deny it. I distinguish authentication entry-point behavior from access-denied handling and test both.

## 3. CSRF on a “REST API”

**Interviewer:** Can we disable CSRF because responses are JSON?

**Candidate:** Response format is irrelevant. I ask how the browser authenticates. If it automatically attaches a session cookie, cross-site requests can carry that credential and state changes need CSRF protection. If the API accepts only an explicit bearer header and has no cookie authentication, the CSRF threat differs. CORS is separate and is not authentication.

**Interviewer:** What if the SPA stores its bearer token in local storage?

**Candidate:** That avoids automatic cookie attachment but increases token theft impact under XSS. I would evaluate the complete browser threat model rather than trading one slogan for another.

## 4. Database outage restarts every pod

**Interviewer:** Kubernetes keeps restarting healthy Java processes when MySQL is down.

**Candidate:** The liveness probe likely includes database health. Liveness should answer whether the process is irrecoverably stuck, not whether every dependency is available. I would keep liveness local, use readiness or route-specific health to stop traffic that requires MySQL, keep diagnostic detail secured, and verify probe timeout/frequency so probes do not amplify the outage.

**Interviewer:** Should readiness always fail on database loss?

**Candidate:** Only if the instance cannot serve its promised routes. A service that can safely serve cached reads may remain ready for those paths, but a single platform readiness bit may force a conservative decision or require separated deployments/routes.

## 5. Retry a timed-out payment

**Interviewer:** Payment returned no response after 400 ms. Retry?

**Candidate:** Timeout is ambiguous: the provider may have committed. I retry only with a stable idempotency key and a provider contract that returns the original result, while respecting the remaining end-to-end deadline. Otherwise I query/reconcile before repeating the side effect. I record logical requests separately from attempts.

**Interviewer:** Add a circuit breaker too?

**Candidate:** Possibly, but only around classified dependency failure. It protects local and downstream capacity; it does not resolve the ambiguous operation. I would avoid multiplied gateway/client/mesh retries and make one layer own attempts.

## 6. WebFlux service is slower than MVC

**Interviewer:** We migrated controllers to `Mono`, but p99 became worse.

**Candidate:** A reactive return type does not make dependencies non-blocking. I would inspect event-loop thread dumps and latency spans for JDBC, filesystem, or blocking SDK calls. If the hot path remains blocking, MVC—possibly with a suitable execution model—may be simpler. If one legacy call must remain, I isolate it on a bounded scheduler and measure queueing; I do not scatter `subscribeOn` randomly.

**Interviewer:** Why not call `block()` once?

**Candidate:** On an event loop it consumes scarce progress capacity and can deadlock. Composition keeps the request asynchronous and preserves cancellation/context.

## 7. `flatMap` changed result order

**Interviewer:** A reactive export returns orders out of input order.

**Candidate:** `flatMap` subscribes to inner publishers concurrently and emits as they complete. If order is a requirement I use sequential `concatMap` or an ordered-concurrency strategy, with a bounded concurrency value. If order is not required, I change the test and API contract rather than paying for ordering accidentally.

**Interviewer:** Can concurrency be `Integer.MAX_VALUE`?

**Candidate:** That moves the queue into memory or the downstream. I size it against connection pools, downstream limits, response budget, and observed latency.

## 8. Batch restarts and duplicates

**Interviewer:** A failed job resumed from its checkpoint but duplicated 17 partner calls.

**Candidate:** The external side effects were not atomic with the batch metadata commit. Those calls likely succeeded before the chunk rolled back or the process died. I use a stable business key at the partner, an outbox/staging workflow, or reconciliation. I also test a crash exactly after the side effect and before checkpoint commit.

**Interviewer:** Does chunk size fix it?

**Candidate:** It changes the failure window and repeated work, not the atomicity across systems. Idempotency remains required.

## 9. Skip policy turns bad schema green

**Interviewer:** The job completed with 12,000 skipped rows after a vendor changed its file format.

**Candidate:** A schema change is systemic, not independent bad data. It should fail fast after header/schema validation or a low classified threshold. I reconcile input, committed, rejected, and quarantined counts and alert on deviation. Skip is acceptable only for explicitly tolerated records with an auditable disposition.

**Interviewer:** What would you expose operationally?

**Candidate:** Job/step identity, input checksum/version, counts by outcome/reason, duration, checkpoint/restart count, and the location of a protected reject artifact—without leaking sensitive rows into labels.

## 10. Integration flow loses errors

**Interviewer:** The sender got success, but an executor-backed handler failed later.

**Candidate:** The async channel moved failure to another thread, so it could not propagate normally to the sender. I define an error channel or adapter-specific retry/dead-letter path, correlate errors with the original message, bound executor and queue capacity, and state whether acceptance means “queued” or “completed.”

**Interviewer:** Would a direct channel solve it?

**Candidate:** It restores synchronous error propagation but also makes the sender pay handler latency and failure. I choose it only if that coupling matches the contract.

## 11. Config refresh breaks latency

**Interviewer:** A config refresh changed read timeout from 500 ms to 2 s while retry stayed at three. What went wrong?

**Candidate:** Independently refreshable fields violated the total deadline. I bind a coherent, versioned resilience policy and validate attempts, per-attempt caps, backoff, and total budget together. Some client resources may not safely rebuild live, so I may require controlled rollout instead of refresh.

**Interviewer:** Roll back the property?

**Candidate:** Yes as incident mitigation, then add configuration validation, provenance/version telemetry, and a canary test so the unsafe combination cannot reach the fleet again.

## 12. Which Spring module?

**Interviewer:** We receive one 50 MB partner file each hour, transform it, and upload a result. Batch or Integration?

**Candidate:** I clarify restart, record count, checkpoint, protocols, and latency. If each file is a finite job requiring durable restart and item-level skip/retry, Batch should own processing. Integration can own SFTP polling, validation, and launching the job. If the file is small and one atomic transform is sufficient, a simpler scheduled service may be clearer.

**Interviewer:** Why not WebFlux?

**Candidate:** Non-blocking I/O can help a streaming transport, but it does not provide job identity, checkpoint, or restart semantics. I would not use it as a substitute for those requirements.

## Answer rubric

Score each response from 0 to 2:

- identifies the invariant and asks one clarifying question;
- traces the actual framework/runtime boundary;
- names timeout, duplicate, context, restart, or ordering edge cases;
- chooses a bounded mechanism rather than “enable everything”;
- proposes focused test and operational proof.

A score of 8/10 is a strong signal. Repeat any answer that starts only with an annotation name.
