# Observability, SLOs, Testing, and Delivery

## Service-level objectives

Start from a user journey. Define an SLI as a good-event ratio or latency distribution, choose a window and target, then create burn-rate alerts for actionable budget consumption.

## Telemetry contract

- Metrics: rate, errors, duration, saturation, queue lag, dependency health, and business outcomes.
- Logs: structured event, timestamp, severity, service, operation, tenant-safe context, error code, and correlation ID.
- Traces: end-to-end context, meaningful spans, sampled errors/slow paths, and async links.
- Dashboards: user outcome first, then service and resource drill-down.

## Testing strategy

Cover unit tests, component tests, integration tests for real boundaries, contract tests, end-to-end critical journeys, load tests, fault tests, and migration tests.

## Delivery safety

Explain immutable artifacts, automated checks, canary or blue/green rollout, feature flags, compatible schema changes, health gates, rollback versus roll-forward, and verification.

## Drill

Create an SLO and release plan for checkout. Include an alert, dashboard, canary decision, failed migration response, and customer-impact communication.
