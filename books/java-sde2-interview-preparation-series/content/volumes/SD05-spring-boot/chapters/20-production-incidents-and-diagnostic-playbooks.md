# Production Incidents and Diagnostic Playbooks

Strong incident response moves from symptom to evidence to the smallest safe change. Boot provides condition reports, failure analyzers, Actuator endpoints, metrics, thread/heap dumps, startup steps, and configuration origins; use each within its security boundary.

## Universal diagnostic loop

```text
1. Define user impact and start time.
2. Compare recent deploy/config/dependency/platform changes.
3. Classify startup, request, dependency, resource, or shutdown phase.
4. Gather bounded evidence before restarting or changing flags.
5. Form competing hypotheses.
6. Run the cheapest discriminating check.
7. Mitigate user impact.
8. Correct and add a regression signal.
```

## Startup: missing bean

Evidence order:

1. full failure analyzer message and cause chain;
2. primary configuration and scan/import boundary;
3. active profiles and property origins;
4. positive/negative condition matches;
5. dependency graph differences;
6. user bean type/qualifier and back-off rules.

Do not add component scanning to the company root as a quick fix.

## Startup: configuration binding

Record canonical property, supplied name, source origin, raw format without secret value, target type, conversion, and validation failure. Check renamed/removed properties after upgrades. Fix ownership rather than adding a silent default.

## Runtime: sudden 404

Separate:

- route not registered (`mappings` evidence);
- wrong context path/base path;
- proxy/gateway rewrite;
- security hiding or rejecting path;
- wrong HTTP method/content negotiation;
- instance running a different version.

## Runtime: latency and pool exhaustion

Correlate request percentiles, active requests, executor queues, database-pool pending count, transaction duration, query/lock time, outbound client pools, retry attempts, GC, and CPU throttling. Increasing all pools can move overload downstream.

## Runtime: memory termination

Distinguish Java `OutOfMemoryError`, container OOM kill, native allocation failure, and deliberate platform eviction. Capture heap/native/thread evidence when safe. Compare heap, thread count, direct buffers, class growth, request sizes, caches, and recent feature toggles.

## Readiness flapping

Check which health contributor changes status, its timeout and cache, whether every instance shares the dependency, and what the platform does when all become unready. A noisy optional dependency should not repeatedly remove healthy core capacity.

## Deployment: requests dropped on shutdown

Compare readiness refusal time, load-balancer convergence, `preStop`, termination signal, grace period, server drain timeout, request deadlines, async executors, and message-consumer behavior. Reproduce with a slow but bounded request during rollout.

## Safe operational endpoints

- `health`: state and groups;
- `metrics`/Prometheus: rates, latency, saturation;
- `conditions`: auto-configuration decisions;
- `configprops`/`env`: origins and sanitized configuration;
- `mappings`: registered routes;
- `threaddump`/`heapdump`: high-sensitivity diagnostics;
- `startup`: recorded startup steps.

Expose only what operators need, authorize access, and audit high-risk retrieval.

## Common mistakes

- Restarting before collecting evidence.
- Enabling debug logging globally during peak load.
- Printing secrets to compare configuration.
- Increasing readiness timeouts without finding the slow phase.
- Treating one stack trace as the root cause.
- Making three configuration changes in one mitigation.
- Keeping temporary diagnostic exposure after the incident.

## Interview angle

**Interviewer:** Local succeeds; production fails during context startup. What is your sequence?

**Strong answer:** I compare artifact checksum, Java/Boot version, resolved dependencies, launch arguments, profiles and property origins, then inspect the failure analyzer and condition report. I reproduce with the production-like packaged artifact and sanitized configuration contract. I do not assume environment difference means “network” or expose all config.

## Quick check

1. What is the first incident question?
2. Which endpoint proves route registration?
3. Why can larger pools worsen latency?
4. Container OOM kill versus Java OOME?
5. What evidence is needed for readiness flapping?

## Practice

- **Foundation:** Classify ten symptoms by lifecycle phase.
- **Interview Core:** Write a missing-bean decision tree.
- **Interview Core:** Diagnose readiness flapping without restarts.
- **SDE-2 Follow-up:** Lead a latency incident with three competing hypotheses and discriminating checks.
