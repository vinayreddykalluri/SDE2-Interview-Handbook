# 52. Secure and Reliable Java

## Learning objectives

By the end of this chapter, you should be able to:

- threat-model Java service assets, actors, trust boundaries, and abuse cases;
- validate input and prevent injection, unsafe deserialization, path, XML, SSRF, and resource-exhaustion defects;
- apply authentication, authorization, secrets, cryptography, and logging at appropriate boundaries;
- design deadlines, bounded retries, idempotency, bulkheads, backpressure, and graceful degradation;
- connect security and reliability through least privilege, bounded work, safe failure, and recovery; and
- present a practical secure-development and incident-readiness checklist for Java 17 and Java 21 services.

## Why this matters at SDE-2

Security and reliability fail at assumptions: a field was "internal," a timeout meant rollback, a queue would stay small, a URL was trusted after parsing, or a dependency was safe because it was popular. Attackers deliberately exercise edge cases that normal traffic rarely reaches, and outages turn ordinary retries and logs into amplifiers.

SDE-2 engineers should build controls into API and system design rather than bolt them onto controllers. They must understand that validation is context-specific, authorization belongs on every protected action, cryptography requires lifecycle and key management, and graceful degradation cannot violate money, privacy, or integrity rules.

## First-principles model

Threat modeling begins with a data-flow view:

```text
external actor -> network boundary -> parser -> application policy
               -> database/cache -> message -> downstream service
```

At each crossing ask:

1. What assets and invariants matter?
2. Who can supply or observe data?
3. How can identity, confidentiality, integrity, availability, or auditability fail?
4. Which prevention, detection, response, and recovery controls apply?
5. What residual risk and operational signal remain?

Reliability uses a similar model. Work consumes finite CPU, memory, threads, connections, file descriptors, queue slots, and downstream capacity. Every admission path needs a bound and every remote effect needs an outcome model.

```text
safe service = validated authority + bounded work + explicit deadlines
             + idempotent recovery + observable state + tested rollback
```

> **Specification boundary:** Java supplies type safety, memory safety for ordinary managed code, cryptographic and networking APIs, and runtime checks. It does not make application authorization, parsers, native code, reflection, dependencies, secrets, SQL, files, or distributed protocols automatically safe. Provider algorithms and TLS/JCA defaults vary by JDK/vendor/version.

## Core terminology

- **Asset:** Data, capability, availability, money, identity, or reputation requiring protection.
- **Trust boundary:** Point where data or authority crosses between different trust assumptions.
- **Authentication:** Establishing a principal's identity.
- **Authorization:** Deciding whether that principal may perform one action on one resource.
- **Least privilege:** Granting only capabilities required for a limited purpose and duration.
- **Defense in depth:** Independent controls that limit failure of one layer.
- **Injection:** Untrusted data interpreted as commands or structure in another language.
- **SSRF:** Server-side request forgery that makes a service access unintended network resources.
- **Deserialization:** Converting external representation into in-memory values or objects.
- **Deadline:** Absolute latest completion time propagated across calls.
- **Backpressure:** Slowing, rejecting, or shedding producers when capacity is exhausted.
- **Bulkhead:** Resource partition that limits one workload's blast radius.
- **Circuit breaker:** State machine that temporarily rejects calls after evidence of dependency failure.

## Detailed mechanics

### Validate by context and bound work

Decode using an explicit charset and media type, enforce total bytes and nesting before expensive allocation, then validate type, range, length, format, and cross-field domain rules. Prefer allow-lists and typed parsers. A string safe as display text is not automatically safe in SQL, HTML, shell, LDAP, a file path, a regular expression, or a log.

Parameterize SQL values. Avoid executing operating-system commands; when unavoidable, pass an argument list to a fixed executable rather than building a shell string, validate every argument, set a working directory/environment deliberately, and impose timeout/output limits.

Regular expressions can consume superlinear CPU, so bound input and choose safe patterns. Compressed uploads need entry, expanded-byte, nesting, path, and ratio limits. `Content-Length` is not a sufficient bound.

### Paths, URLs, XML, and serialization

Normalize paths to reject simple traversal, but remember symlinks and validation-use races. Store user content under generated names and least-privilege roots. Archive extraction validates every final destination and refuses links or special files unless explicitly supported.

For outbound URLs, strictly allow schemes, hosts, ports, and paths; account for private/link-local/loopback addresses, redirects, and DNS rebinding. Network egress controls provide an independent layer. Blocking one textual hostname is not SSRF prevention.

Disable XML external entities and external DTD/schema access unless narrowly required. Defaults vary, so test the actual parser with malicious fixtures.

Never use native Java deserialization for untrusted data. Isolate unavoidable legacy use with allow-list and graph limits. Explicit schemas still need polymorphism, size, duplicate-field, and version policies.

### Authentication and authorization

Authenticate at a verified boundary, then authorize each operation and resource. Resolve tenant and ownership from trusted server-side state, not solely from request fields.

Use short-lived, narrowly scoped credentials and validate signature, issuer, audience, time, and key rotation through the protocol library. Enforce application-level authorization on user, administrative, and background paths, not only in a UI or gateway.

### Secrets and cryptography

Keep secrets out of source, build logs, command lines, heap dumps, URLs, and ordinary metrics. Obtain them from an approved secret system, grant least privilege, rotate, audit access, and design clients to refresh without full outages. Environment variables can be exposed by process diagnostics and are not a universal secure store.

Use maintained protocols and approved JCA/JCE libraries. Do not invent encryption, nonce, password-hashing, signature, or certificate rules. Passwords need a salted, work-factor KDF selected by current policy. Random tokens need `SecureRandom`; UUID uniqueness does not automatically provide secret unpredictability.

Authenticated encryption requires unique nonces per key and verified tags before plaintext use. Keys need rotation, revocation, and access separation. Never install a "trust all" TLS manager or disable hostname verification in production.

> **Vendor boundary:** Available JCA providers, algorithm names, key-store support, TLS versions/cipher defaults, certificate revocation, and hardware acceleration differ by distribution and configuration. Pin policy and test handshakes on every supported runtime.

### Logging, errors, and audit

Log events and identifiers needed for diagnosis, not raw request bodies, access tokens, passwords, card data, session cookies, encryption keys, or broad object `toString` output. Sanitize control characters to prevent log forging. Bound message length and metric-label cardinality.

Return stable public errors with minimal detail and preserve restricted causal context internally. Keep access-controlled audit events separate from debug logs when required.

### Deadlines and bounded retry

Propagate an absolute deadline so each layer sees remaining time. Configure connection acquisition, connect, read/write, database statement, and queue waits within it. Cancellation must release resources. A timeout is an unknown remote outcome, not proof of rollback.

Wall clocks can jump and hosts can disagree. Propagate an absolute deadline for cross-process policy, account for clock skew, and use a monotonic elapsed-time source for local budget enforcement when precision matters.

Retry only classified transient failures and idempotent operations, or reuse a stable idempotency key. Bound attempts and elapsed time, use exponential backoff with jitter, and honor server retry hints only within policy. Retrying at multiple stack layers multiplies attempts. Centralize ownership of retries and measure attempts versus logical requests.

Circuit breakers reduce repeated calls to a failing dependency but add a state machine and can reject recovery probes. Bulkheads bound concurrency per dependency or tenant. Rate limits and admission control reject before expensive work. Bounded queues force a documented response: block within a deadline, reject, spill durably, shed lower priority, or degrade safely.

### Idempotency and state transitions

An idempotency key identifies one logical command, is scoped to principal/operation, and stores a request fingerprint plus completed or in-progress outcome. Reuse with a different payload must be rejected. Expiry must exceed plausible client retry windows or the old effect can be repeated later.

Database unique constraints or compare-and-set updates enforce concurrency. Consumers record message identity in the same transaction as their business effect. Exactly-once transport wording does not make an email, payment, or arbitrary database side effect exactly once.

### Safe degradation and recovery

Degrade only features whose loss does not violate authorization, integrity, billing, or durability. Recommendations can disappear; authorization normally fails closed.

Liveness should answer whether the process should be restarted; readiness should answer whether it should receive new traffic. Making liveness depend on every downstream can cause restart storms. Exact probe semantics belong to the orchestrator and service design.

On shutdown, stop admission, mark unready, allow bounded in-flight completion, checkpoint or return leased work, close clients/executors, and exit before the platform's hard deadline. Test forced termination and duplicate processing.

Backups are not recovery until restores are tested. Define recovery objectives, key recovery, schema repair, incident authority, evidence preservation, and credential rotation.

### Java-specific attack surface

Reflection, agents, annotation processors, JNI, native libraries, and dynamic loading expand capability. Minimize them and treat processors as executable dependencies. The Security Manager is deprecated for removal in Java 17/21; isolate hostile code at process/container and OS boundaries.

## Worked Java example

This retrier makes limits, classification, deadline, sleeping, and jitter explicit:

```java
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.concurrent.TimeoutException;
import java.util.function.LongSupplier;

record RetryPolicy(int maxAttempts, Duration initialBackoff,
        Duration maxBackoff) {
    public RetryPolicy {
        if (maxAttempts < 1 || initialBackoff == null || maxBackoff == null
                || initialBackoff.isNegative() || initialBackoff.isZero()
                || maxBackoff.compareTo(initialBackoff) < 0) {
            throw new IllegalArgumentException("invalid retry policy");
        }
    }
}

@FunctionalInterface interface CheckedOperation<T> {
    T run(Instant deadline) throws Exception;
}

@FunctionalInterface interface RetryClassifier {
    boolean isRetryable(Exception failure);
}

@FunctionalInterface interface Sleeper {
    void sleep(Duration duration) throws InterruptedException;
}

@FunctionalInterface interface Jitter {
    Duration apply(Duration upperBound);
}

final class BoundedRetrier {
    private final Clock clock;
    private final LongSupplier nanoTime;
    private final Sleeper sleeper;
    private final Jitter jitter;

    BoundedRetrier(Clock clock, LongSupplier nanoTime,
            Sleeper sleeper, Jitter jitter) {
        this.clock = java.util.Objects.requireNonNull(clock);
        this.nanoTime = java.util.Objects.requireNonNull(nanoTime);
        this.sleeper = java.util.Objects.requireNonNull(sleeper);
        this.jitter = java.util.Objects.requireNonNull(jitter);
    }
```

The bounded retry loop continues inside the same `BoundedRetrier` class:

```java

    <T> T execute(Instant deadline, RetryPolicy policy,
            RetryClassifier classifier, CheckedOperation<T> operation)
            throws Exception {
        Duration backoff = policy.initialBackoff();
        Duration initialBudget = Duration.between(clock.instant(), deadline);
        long startedAtNanos = nanoTime.getAsLong();
        Exception lastFailure = null;

        for (int attempt = 1; attempt <= policy.maxAttempts(); attempt++) {
            Duration remaining = remainingBudget(
                    deadline, initialBudget, startedAtNanos);
            if (remaining.isNegative() || remaining.isZero()) {
                TimeoutException timeout = new TimeoutException("deadline exceeded");
                if (lastFailure != null) timeout.addSuppressed(lastFailure);
                throw timeout;
            }
            try {
                return operation.run(deadline);
            } catch (InterruptedException interrupted) {
                Thread.currentThread().interrupt();
                throw interrupted;
            } catch (Exception failure) {
                lastFailure = failure;
                if (!classifier.isRetryable(failure)
                        || attempt == policy.maxAttempts()) {
                    throw failure;
                }
            }

            remaining = remainingBudget(deadline, initialBudget, startedAtNanos);
            Duration cap = min(backoff, remaining);
            if (cap.isNegative() || cap.isZero()) {
                TimeoutException timeout = new TimeoutException("no retry budget");
                timeout.addSuppressed(lastFailure);
                throw timeout;
            }
            Duration delay = jitter.apply(cap);
            if (delay == null || delay.isNegative() || delay.compareTo(cap) > 0) {
                throw new IllegalStateException("jitter violated its contract");
            }
            try {
                sleeper.sleep(delay);
            } catch (InterruptedException interrupted) {
                Thread.currentThread().interrupt();
                throw interrupted;
            }
            backoff = doubleCapped(backoff, policy.maxBackoff());
        }
        throw new AssertionError("unreachable");
    }
```

The duration helpers complete `BoundedRetrier`:

```java

    private Duration remainingBudget(Instant deadline,
            Duration initialBudget, long startedAtNanos) {
        Duration wallRemaining = Duration.between(clock.instant(), deadline);
        long elapsedNanos = nanoTime.getAsLong() - startedAtNanos;
        if (elapsedNanos < 0) {
            return Duration.ZERO;
        }
        Duration monotonicRemaining = initialBudget.minusNanos(elapsedNanos);
        return min(wallRemaining, monotonicRemaining);
    }

    private static Duration min(Duration left, Duration right) {
        return left.compareTo(right) <= 0 ? left : right;
    }

    private static Duration doubleCapped(Duration value, Duration maximum) {
        return value.compareTo(maximum.dividedBy(2)) > 0
                ? maximum : value.multipliedBy(2);
    }
}
```

A production constructor can inject `System::nanoTime` as the monotonic ticker; tests inject a deterministic ticker. The wrapper stops when either the propagated wall-clock deadline or the original monotonic elapsed-time budget is exhausted, so a backward wall-clock adjustment cannot extend retries. A production jitter implementation might choose a random duration between zero and the cap; tests inject exact delays. The operation receives the absolute deadline and must apply its own per-call timeouts. The wrapper cannot interrupt an arbitrary blocking API merely because either clock reaches the deadline.

## Execution or memory walkthrough

Assume `maxAttempts = 3`, initial backoff 100 ms, maximum 1 second, and five seconds remain. The first transient failure is classified retryable. Jitter returns 60 ms, so the sleeper waits 60 ms. The next cap is 200 ms. If the second call succeeds, execution returns after two attempts.

If failure is an authentication rejection, the classifier returns false and no retry occurs. If the thread is interrupted during the operation or sleep, the retrier restores the interrupt flag and propagates interruption rather than treating cancellation as transient failure.

If only 40 ms remains after a failure under either clock, the backoff cap becomes 40 ms; jitter cannot exceed it. The next operation still receives the same absolute deadline. A client with a 500 ms socket timeout would violate that deadline, so the operation must derive its timeout from remaining time.

The retrier holds only one last exception, policy values, and local durations: `O(1)` state. The remote side still needs an idempotency key because the first timed-out attempt may have committed.

## Complexity and performance

With at most `a` attempts, local control work is `O(a)` and state is `O(1)`. Total worst-case delay is bounded by the deadline and attempt limit, not merely by the backoff sum. Without limits, retries can grow traffic by a multiplier at every layer:

```text
3 gateway attempts x 3 service attempts x 3 client attempts = 27 calls
```

Security bounds turn attacker-controlled dimensions into configured limits:

| Dimension | Unbounded risk | Control |
|---|---|---|
| request bytes/nesting | heap/CPU exhaustion | parser and transport limits |
| regex/input length | CPU exhaustion | safe pattern plus length bound |
| queue/concurrency | memory and downstream collapse | admission and bulkhead |
| decompression | disk/heap explosion | expanded-byte and ratio limits |
| retries | outage amplification | classifier, jitter, attempts, deadline |
| metric labels/logs | memory/storage/cardinality | allow-list and truncation |

Cryptographic cost is intentional. Password KDF parameters consume CPU/memory to resist guessing and must be capacity-tested. Do not lower them reactively without a security decision; protect login capacity with rate limiting and dedicated resource budgets.

## Edge cases and common mistakes

- Validating a string once and reusing it in a different output context.
- Trusting client-supplied tenant/resource IDs after authentication without authorization.
- Concatenating SQL, shell, path, URL, LDAP, or log structure from untrusted data.
- Allowing URL redirects or DNS resolution to bypass an SSRF host check.
- Enabling XML external entities or broad polymorphic deserialization.
- Treating encryption without authentication as integrity protection.
- Reusing an AEAD nonce or storing keys beside ciphertext with identical access.
- Logging tokens, passwords, request bodies, secrets, or diagnostic dumps without controls.
- Enforcing elapsed retry budgets only with an adjustable wall clock and ignoring clock skew.
- Retrying permanent failures, interruption, or non-idempotent commands.
- Implementing retries at several layers without a total budget.
- Using unbounded queues or cached thread pools to absorb overload.
- Failing open when authorization, billing, or integrity dependencies fail.
- Making liveness depend on a downstream service and causing restart storms.
- Running untrusted code under the deprecated Security Manager as a sandbox.
- Assuming a successful backup job proves restoration.

## Production engineering notes

Maintain threat models and owners for controls, alerts, keys, exceptions, and recovery. Review changes involving authorization, parsing, cryptography, file/network access, native code, and serialization.

Default to denial, bounded work, safe parsers, verified TLS, and no production debug surface. Make overrides narrow, audited, and temporary.

Measure logical requests, attempts, rejection, queue age, circuit state, timeout phase, authorization decisions, and dependency latency without leaking secrets or high-cardinality IDs.

Practice restores, outages, key/certificate rotation, duplicates, overload, and shutdown. Runbooks cover containment, evidence, credentials, communication, and rollback.

## Interview questions and model answers

**How do you secure a new endpoint?**

Define assets and actors, authenticate the principal, authorize the action on the resource, bound and validate parsing, parameterize downstream queries, set deadline and capacity, redact observability, make side effects idempotent, and test abuse and failure paths.

**What is the difference between authentication and authorization?**

Authentication establishes who the caller is. Authorization decides whether that caller can perform this action on this resource under current policy. A valid identity can still be unauthorized.

**Why are retries dangerous?**

They amplify load, increase latency, and can duplicate side effects. Retry only transient failures within one bounded owner and deadline, with jitter and idempotency or reconciliation.

**How would you prevent SSRF?**

Use strict URL parsing and allow-lists, validate scheme/host/port, control DNS/private addresses and redirects, enforce egress network policy, bound response/time, and avoid accepting arbitrary URLs when an identifier can be used.

**Should a service fail open or closed?**

It depends on the invariant. Authorization, integrity, and payment controls normally fail closed. Optional presentation features may degrade. Make the decision explicit and test dependency failure.

**What is defense in depth?**

Independent controls limit one another's failure: application URL policy plus network egress restrictions, prepared SQL plus least-privilege database credentials, and parser limits plus request admission.

**How do you handle an unknown outcome after timeout?**

Reuse a stable idempotency key and query status or retry the same logical operation safely. Do not create a new command identity and assume the first attempt rolled back.

## Exercises

1. Threat-model a file-upload endpoint: include parser, archive, path, malware, authorization, quotas, storage, and download behavior.
2. Design idempotency storage for a payment command, including payload fingerprint, in-progress state, response replay, and expiry.
3. Calculate worst-case calls when three layers each retry twice after the original attempt. Redesign with one retry owner.
4. Extend the retrier test design with a fake clock, sleeper, jitter, transient failure, deadline exhaustion, and interruption.
5. Review an outbound webhook feature for SSRF, secret signing, timeouts, retries, redirect policy, and tenant isolation.
6. Write graceful-shutdown steps for a message consumer that leases work and may receive duplicate delivery.

## Chapter summary

Secure and reliable Java systems make trust and capacity explicit. Validate data for its destination context, authorize every protected resource action, use approved cryptography and secret lifecycle, and restrict dangerous parsing and dynamic capabilities. Bound bytes, time, concurrency, queues, retries, and cardinality. Treat timeouts as unknown outcomes and use idempotency for recovery. Layer preventive controls with telemetry, graceful failure, least privilege, tested restore, and incident practice.

## Revision checklist

- [ ] I can identify assets, actors, trust boundaries, abuse cases, and residual risk.
- [ ] I validate input by context and bound bytes, depth, CPU, concurrency, and output.
- [ ] I can prevent SQL/shell/path/XML/deserialization and SSRF classes of defects.
- [ ] I separate authentication from resource-level authorization.
- [ ] I use approved secret, password, encryption, nonce, key, and TLS practices.
- [ ] I propagate deadlines and bound one retry owner with classification, jitter, and idempotency.
- [ ] I design bulkheads, backpressure, safe degradation, and graceful shutdown.
- [ ] I protect logs, metrics, diagnostics, dependencies, builds, backups, and incident evidence.
