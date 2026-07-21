# Advanced Review: Low-Level Design

## Advanced model

| Area | Required depth |
| --- | --- |
| Domain-driven design | Entities, values, aggregates, services, bounded contexts |
| Architecture boundaries | Ports/adapters, dependency inversion, infrastructure isolation |
| Invariants | Aggregate ownership, transaction boundary, invalid-state prevention |
| Concurrency | Ownership, locks, optimistic versions, idempotent commands |
| Persistence | Repository contracts, mapping, unit of work, migrations |
| Extensibility | Stable core, explicit variation points, compatibility |
| Testing | State, interaction, property-based, and contract tests |
| Evolution | New requirements without speculative abstraction |

## Reconstruction exercise

Choose one case study and recreate four views: domain model, class diagram, sequence diagram, and state machine. Mark transaction boundaries, external ports, concurrency ownership, and each invariant.

## Drill ladder

- **Foundation:** model one happy path with semantic classes and tests.
- **SDE-2:** add a failure path, persistence adapter, and policy variation.
- **Advanced:** support concurrent commands and consistency behavior.
- **Evolution:** add a requirement that invalidates the first abstraction.

## Retrieval prompts

1. What makes an aggregate too large or too small?
2. When is a domain service preferable to an entity method?
3. Which dependency points inward in hexagonal architecture?
4. How do State and Strategy differ in intent?
5. Where should command idempotency be enforced?
6. How do optimistic versions change the contract?
7. Which tests protect behavior without freezing structure?
8. When is a modular monolith better than services?

## Exit criteria

Deliver one runnable case study with diagrams, tests, persistence boundary, concurrent-update strategy, and a ten-minute extension without redesigning the entire model.
