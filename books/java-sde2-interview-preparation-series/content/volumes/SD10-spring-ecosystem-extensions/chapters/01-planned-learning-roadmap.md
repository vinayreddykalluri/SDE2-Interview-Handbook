# Spring Ecosystem Extensions - Planned Learning Roadmap

> **Publication status:** roadmap edition. This volume reserves the major Spring topics that deserve focused expansion after the Framework and Boot foundations are stable.

The Spring ecosystem is broad. This book groups the most important specialist modules initially so the curriculum stays navigable. A topic can become its own focused volume later when its examples, exercises, and production guidance reach publication depth.

## Planned sequence

1. Spring Security: filter chains, authentication, authorization, method security, OAuth 2.0, and testing.
2. Spring Cloud: configuration, discovery, gateways, clients, resilience integration, and distributed concerns.
3. Spring Boot Actuator and observability: health groups, metrics, tracing, audit events, and safe exposure.
4. Spring WebFlux: reactive contracts, backpressure, event loops, blocking boundaries, and testing.
5. Spring Batch: jobs, steps, readers, processors, writers, restartability, and partitioning.
6. Spring Integration: messages, channels, adapters, routing, retries, and idempotency.
7. Spring testing: context caching, slices, security tests, containers, and contract evidence.
8. Version alignment, release trains, dependency management, and module selection.

## Scope rule

This volume will not imply that every service needs every module. Each section will begin with the problem the module solves, prerequisites, operational costs, alternatives, and a decision checklist.

## Completion gate

A reader is ready to select an extension when they can identify the actual requirement, explain the runtime model, define failure and security boundaries, choose a focused test strategy, and justify why the module is simpler than a smaller direct implementation.
