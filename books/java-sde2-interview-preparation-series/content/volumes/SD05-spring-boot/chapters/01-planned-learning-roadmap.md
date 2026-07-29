# Spring Boot for Production Java Services - Planned Learning Roadmap

> **Publication status:** roadmap edition. Runnable services, configuration matrices, failure drills, and interview questions will be added incrementally.

Spring Boot packages Spring applications around opinionated defaults, auto-configuration, dependency management, executable deployment, and production features. The book will build on Spring Framework rather than presenting Boot annotations without their underlying contracts.

## Planned sequence

1. Starters, dependency management, auto-configuration, conditions, and back-off rules.
2. Application startup, configuration properties, profiles, precedence, and secrets.
3. Web applications, JSON boundaries, validation, errors, pagination, and idempotency.
4. Actuator health, readiness, liveness, metrics, tracing, logging, and diagnostics.
5. Data access and transaction integration without leaking persistence into API contracts.
6. Testing with unit, slice, integration, container, and end-to-end evidence.
7. Packaging, layered images, graceful shutdown, resource limits, and deployment behavior.
8. Version upgrades, dependency risk, native-image awareness, and operational checklists.

## Interview focus

The expanded edition will ask candidates to explain why an auto-configuration activates, how to override it safely, how configuration precedence works, what readiness must prove, and how a service behaves during shutdown or dependency failure.

## Completion gate

A reader is ready for Spring Data and specialist Spring modules when they can create a minimal service, explain its startup and configuration, expose safe operational signals, select appropriate test boundaries, and diagnose behavior from conditions and logs rather than guessing.
