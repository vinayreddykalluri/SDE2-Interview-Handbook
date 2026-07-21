# LLD Case-Study Catalog

Use each case study to practice a different design pressure. Do not reuse one generic class model.

| Case study | Primary pressure | Required deep dive |
| --- | --- | --- |
| Parking lot | Allocation and pricing policies | Compatibility and concurrent entry |
| Elevator | State transitions and scheduling | Request selection and failure state |
| Vending machine | Explicit state machine | Inventory, payment, refund |
| Notification service | Strategy and adapters | Retry, templates, preferences |
| In-memory cache | Data structure and policy | LRU/LFU, TTL, thread safety |
| Rate limiter | Algorithm strategy | Token bucket and distributed ownership |
| Job scheduler | Commands and lifecycle | Dependencies, retries, cancellation |
| Split expense | Domain invariants | Rounding, settlement, split policies |
| Library system | Search and reservation | Copies, holds, fines |
| Board game | Rules and extensibility | Turns, moves, win conditions |

## Deliverable template

Clarify scope, list use cases, define entities and value objects, draw class/sequence/state diagrams, implement the critical path, test invariants, add one new requirement, and explain what changes.

## Follow-up bank

- Which object owns concurrency control?
- Where does persistence enter without contaminating domain logic?
- Which operation must be idempotent?
- How would a new policy or provider be added?
- Which assumptions stop working if this becomes distributed?
