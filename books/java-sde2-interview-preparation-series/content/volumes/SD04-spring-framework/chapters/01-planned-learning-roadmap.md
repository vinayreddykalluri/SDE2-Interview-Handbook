# Spring Framework Foundations - Planned Learning Roadmap

> **Publication status:** roadmap edition. Later revisions will add runnable container examples, proxy traces, test cases, and interview exercises.

Spring Framework is the foundation beneath Spring Boot and the wider Spring ecosystem. This book will teach the container, dependency injection, application context, proxy model, transactions, events, validation, and web foundations before convenience auto-configuration is introduced.

## Planned sequence

1. Inversion of control, dependency injection, object graphs, and explicit boundaries.
2. Bean definitions, component scanning, configuration classes, scopes, and lifecycle.
3. Application contexts, environment properties, profiles, resources, and events.
4. Aspect-oriented programming, proxies, advice, self-invocation, and visibility limits.
5. Declarative transactions, propagation, isolation, rollback rules, and proxy boundaries.
6. Validation, conversion, formatting, data binding, and message resolution.
7. Spring MVC request flow, controllers, exception handling, and HTTP contracts.
8. Testing the container with focused slices and explicit integration boundaries.

## Interview focus

Readers will learn to explain what the container creates, which calls pass through a proxy, why a transaction annotation may not take effect, how bean scope changes ownership, and when constructor injection improves correctness. The book will keep interface contracts separate from framework magic.

## Completion gate

A reader is ready for Spring Boot when they can construct and test a small Spring application context, reason about bean lifecycle and proxy interception, define transaction boundaries, and diagnose configuration without relying on trial-and-error annotations.
