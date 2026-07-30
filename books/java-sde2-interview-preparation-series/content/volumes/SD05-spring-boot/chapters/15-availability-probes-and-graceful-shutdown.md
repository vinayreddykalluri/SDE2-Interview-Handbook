# Availability, Probes, and Graceful Shutdown

Process running, application live, application ready, and dependency healthy are different statements. Platforms act on probes, so incorrect semantics can turn one dependency incident into a restart storm or total traffic outage.

## Availability states

```text
startup:
  liveness BROKEN -> CORRECT
  readiness REFUSING_TRAFFIC -> ACCEPTING_TRAFFIC

shutdown:
  readiness ACCEPTING_TRAFFIC -> REFUSING_TRAFFIC
  stop accepting new requests
  drain in-flight work within timeout
  close context and resources
```

Boot exposes Kubernetes-oriented health groups at:

```text
/actuator/health/liveness
/actuator/health/readiness
```

Additional main-port paths can reduce false confidence when management uses a separate port.

## Liveness rule

Liveness asks whether restarting this process is likely to repair it. It should normally exclude shared external systems. If a database fails and every replica reports dead, the platform restarts all of them while the database is still unavailable.

## Readiness rule

Readiness asks whether this instance should receive new traffic. Include only dependencies required for the served traffic and consider service-wide impact. An optional recommendation provider should degrade a feature, not remove the order API from service.

## Startup probe

A startup probe can protect a legitimately slow application from liveness kills. It should not normalize unbounded migrations or cache warmups. Measure startup distribution and make required startup work finite.

## Graceful shutdown

```properties
server.shutdown=graceful
spring.lifecycle.timeout-per-shutdown-phase=20s
```

During graceful shutdown the server rejects new requests and permits in-flight work to finish within the phase timeout. The orchestrator must provide a termination grace period longer than application drain plus safety margin.

Shutdown also requires:

- stop message intake or scheduler claims;
- stop accepting new application tasks;
- await bounded executors;
- release database and HTTP pools;
- complete or recover durable work;
- preserve termination reason and metrics.

## Long requests and async work

A 20-second shutdown budget cannot safely drain a 2-minute request. Bound request deadlines. For durable background work, lease/claim units so another instance can recover after lease expiry. In-memory queued work can be lost on termination.

## Deployment timeline

```text
new instance starts -> readiness passes -> receives traffic
old instance marked unready -> load balancer converges
preStop/drain window -> shutdown signal -> in-flight completion
force kill only after grace period
```

Account for load-balancer propagation; readiness changing in-process is not instantly observed everywhere.

## Common mistakes

- Using the same check for liveness and readiness.
- Including every shared dependency in liveness.
- Setting shutdown timeout longer than platform grace.
- Starting a long migration in every replica.
- Keeping an unbounded task executor.
- Assuming an in-memory queue survives rolling deployment.
- Returning ready before mandatory startup state is safe.

## Interview angle

**Interviewer:** Deployments drop requests despite graceful shutdown. Why?

**Strong answer:** I compare readiness transition, endpoint removal and load-balancer convergence, pre-stop delay, server drain, application request deadlines, executor work, and platform termination grace. Graceful shutdown cannot save requests still routed after the instance begins closing or work longer than the allowed budget.

## Quick check

1. What action does liveness trigger?
2. What action does readiness trigger?
3. When is a startup probe appropriate?
4. What must platform grace exceed?
5. How should durable background work recover?

## Practice

- **Foundation:** Classify five checks as live, ready, or diagnostic.
- **Interview Core:** Design probes for required database and optional cache.
- **Interview Core:** Calculate a shutdown budget from request deadlines.
- **SDE-2 Follow-up:** Diagnose a zero-downtime rollout that still returns connection resets.
