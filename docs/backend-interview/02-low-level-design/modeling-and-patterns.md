# Object Modeling, SOLID, and Patterns

## Modeling sequence

1. Extract actors, commands, entities, value objects, policies, repositories, and external gateways.
2. Assign each invariant to one owner. If everyone validates it, no one owns it.
3. Prefer behavior-rich domain objects over public mutable data bags.
4. Make invalid states unrepresentable through constructors, factories, and value types where practical.
5. Depend on abstractions at volatile boundaries, not around every class.

## SOLID as diagnostic questions

- **SRP:** What single reason would make this unit change?
- **OCP:** Can expected variation be added without editing stable logic?
- **LSP:** Can every implementation honor the behavioral contract?
- **ISP:** Does each client depend only on operations it uses?
- **DIP:** Does policy depend on infrastructure details or on a boundary?

## Pattern map

| Change pressure | Useful patterns |
| --- | --- |
| Object creation varies | Factory, builder |
| Algorithm or policy varies | Strategy, template method |
| Behavior changes by state | State |
| Consumers react to events | Observer, event bus |
| External model differs | Adapter, anti-corruption layer |
| Cross-cutting access/control | Decorator, proxy |
| Multi-step action needs undo | Command |

Name the simpler alternative and the cost introduced by each pattern. Pattern vocabulary without a change pressure is overengineering.
