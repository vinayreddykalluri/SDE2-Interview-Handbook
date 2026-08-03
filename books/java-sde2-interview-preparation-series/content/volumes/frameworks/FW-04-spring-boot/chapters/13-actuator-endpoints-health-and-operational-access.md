# Actuator Endpoints, Health, and Operational Access

Actuator exposes management capabilities through technology-neutral endpoints adapted to HTTP or JMX. Adding the starter is only the first step; availability, exposure, access, details, and network reachability are separate decisions.

## Add the capability

```xml
<dependency>
  <groupId>org.springframework.boot</groupId>
  <artifactId>spring-boot-starter-actuator</artifactId>
</dependency>
```

By default, only a limited endpoint set is exposed remotely. Do not expose `env`, `configprops`, `beans`, `heapdump`, `loggers`, or `shutdown` broadly.

## Four gates

```text
endpoint bean exists
      |
      v
endpoint access permits operation
      |
      v
technology exposure includes it
      |
      v
network/security policy permits caller
```

An endpoint removed from the context by access policy is different from an endpoint present but not exposed over HTTP.

## Explicit exposure

```yaml
management:
  endpoints:
    web:
      exposure:
        include: health,info,metrics,prometheus
  endpoint:
    health:
      show-details: when-authorized
```

Treat the management surface as an administrative API. Use authentication/authorization and network segmentation. A separate management port can isolate traffic, but probes on that port may succeed even when the main server cannot accept connections.

## Health contributors

```java
@Component
final class PaymentHealthIndicator implements HealthIndicator {
    @Override
    public Health health() {
        return Health.up()
                .withDetail("mode", "degraded-capable")
                .build();
    }
}
```

This trivial example should not make a live remote call per probe. Health checks need bounded time, safe detail, and clear semantics. A dependency can be degraded without making the process dead.

## Health groups

Group indicators by operational decision:

- **liveness:** should the platform restart this process?
- **readiness:** should new traffic be sent here?
- **diagnostic:** what dependencies or subsystems are degraded?

Do not put a shared database or provider in liveness; an outage could restart every replica and amplify failure. Include external dependencies in readiness only after deciding the consequences of removing every instance from service.

## Custom endpoints

Use application APIs for business operations. A custom Actuator endpoint is suitable for operator diagnostics or carefully controlled actions:

```java
@Endpoint(id = "orderBacklog")
final class OrderBacklogEndpoint {
    @ReadOperation
    BacklogSnapshot snapshot() {
        return new BacklogSnapshot(queuedCount, oldestAge);
    }
}
```

Keep payloads bounded and sanitized. Mutating operations need strict access and audit.

## Common mistakes

- Exposing every endpoint with `*`.
- Returning credentials or full configuration in details.
- Calling a slow remote system synchronously on every health request.
- Using health as a high-cardinality diagnostics dump.
- Assuming a separate management port proves the main port works.
- Enabling runtime log-level changes without authorization/audit.

## Interview angle

**Interviewer:** Should database failure make readiness fail?

**Strong answer:** It depends on whether the instance can serve meaningful traffic without the database and whether all replicas share it. Removing all instances may turn a dependency outage into total unreachability. I keep liveness independent, define a readiness policy from traffic behavior, expose detailed dependency diagnostics separately, and make checks bounded.

## Quick check

1. Availability versus exposure versus access?
2. Why is a management port not sufficient security?
3. What operational decision should liveness drive?
4. Why keep health payloads bounded?
5. When is a custom endpoint appropriate?

## Practice

- **Foundation:** Expose only health and info.
- **Interview Core:** Design liveness/readiness for a database-backed API.
- **Interview Core:** Threat-model the management surface.
- **SDE-2 Follow-up:** Create a health-group policy for required and optional dependencies.
