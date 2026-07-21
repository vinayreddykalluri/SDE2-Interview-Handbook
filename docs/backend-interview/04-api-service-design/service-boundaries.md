# Service Boundaries and Evolution

## Boundary heuristics

- Align ownership with a cohesive business capability and its invariants.
- Keep strongly consistent changes inside one boundary where possible.
- Avoid splitting services around CRUD entities that always change together.
- Use APIs or events across boundaries, never shared tables.
- Treat team autonomy and operational load as architecture constraints.

## Coupling review

Evaluate temporal, data, deployment, failure, and organizational coupling. A network boundary increases latency and failure modes; it should buy meaningful autonomy or scaling isolation.

## Evolution strategies

- Modular monolith before premature extraction.
- Strangler migration for incremental replacement.
- Anti-corruption layer between domain models.
- Expand/contract schema and protocol changes.
- Outbox plus consumers for reliable integration.
- Consumer-driven contract tests for compatibility.

## Required exercise

Start with an order-management monolith. Draw modular boundaries, identify one justified extraction, define ownership and APIs/events, plan data migration, preserve idempotency, and explain rollback. Include why the remaining modules should not become services yet.
