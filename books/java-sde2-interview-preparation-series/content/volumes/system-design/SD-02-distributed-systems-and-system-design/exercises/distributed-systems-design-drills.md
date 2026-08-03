# Distributed Systems Design Drills

Answer each drill with assumptions, invariant, flow, failure behavior, and evidence. Avoid responding with only product names.

## 1. Unit-bearing capacity — Foundation

A service receives 120 million requests/day, reads a database on 25% of requests, has a 12× peak factor, and the database call takes 30 ms at peak. Estimate average RPS, peak RPS, and concurrent database calls. State at least four omitted factors that could make the estimate wrong.

## 2. Idempotent write — Interview Core

Design the states and stored fields for a payment-intent idempotency record. Cover same-payload duplicates, changed payload, an in-progress attempt, completed success, deterministic rejection, and unknown external-provider outcome.

## 3. Cache contract — Interview Core

A user updates a shipping address and immediately reads it through another region. The replica can lag 10 seconds and the cache TTL is 60 seconds. Give two defensible read-after-write designs and compare availability, latency, and complexity.

## 4. Stream completion — Interview Core

One partition delivers offsets 20–24. Processing completes in order 22, 20, 24, 21, 23. After each completion, state the next safe commit offset and the remembered gaps. Explain the crash consequence of committing the greatest completed offset.

## 5. Poison record — Foundation

A consumer retries a malformed event every second and blocks all later records. Design classification, quarantine, alerting, replay, retention, and ownership. What information must the dead-letter record contain, and what sensitive information should it not contain?

## 6. Hot key — SDE-2 Follow-up

One product ID receives 35% of catalog reads during a launch. The cache expires at the same moment in every instance. Build a mitigation ladder from simplest to most invasive. State which controls protect the cache, origin, and fairness for unrelated keys.

## 7. Retry budget — Interview Core

The endpoint deadline is 800 ms. Local work/reserve needs 100 ms. A dependency attempt has a 200 ms timeout. Propose a bounded retry schedule and explain why three independent 200 ms attempts plus fixed sleeps can still violate the deadline. Include jitter and overload behavior.

## 8. Shard expansion — SDE-2 Follow-up

You must expand from four to six storage shards without stopping writes. Describe routing versioning, copy, change capture/dual-write, validation, cutover, rollback, and retirement. Identify at least three ways data can be lost or duplicated.

## 9. Region failover — SDE-2 Follow-up

The primary region loses connectivity to the control plane but remains connected to some clients and the database. A secondary region is promoted. Explain split-brain risk and design a fencing mechanism. State what happens if storage does not enforce the fencing token.

## 10. Full design defense — SDE-2 Follow-up

Design a multi-tenant job-submission service with these constraints:

- 10k submissions/s peak;
- jobs may run for 30 seconds to 2 hours;
- at-least-once worker delivery;
- no tenant may consume more than 20% of workers;
- callers need status and cancellation;
- a submitted job must not disappear after acknowledgement.

Cover API and identity, durable state, queue/partitioning, scheduling fairness, worker lease/heartbeat, duplicate execution, cancellation race, overload, recovery, and SLIs.
