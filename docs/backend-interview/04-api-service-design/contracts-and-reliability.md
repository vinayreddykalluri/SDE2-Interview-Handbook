# API Contracts and Reliability Semantics

## Contract checklist

- Model business resources or commands, not database tables.
- Define authentication, authorization, tenant context, and data classification.
- Specify required fields, validation, defaults, limits, and canonical formats.
- Use a stable error envelope with machine-readable codes and correlation IDs.
- Define timeout budgets, retryability, rate limits, and overload behavior.
- Choose cursor pagination for changing large collections; document ordering.
- Define backward-compatible evolution and a deprecation window.

## Idempotency

For retryable writes, define the key scope, retention, request fingerprint, concurrent duplicate behavior, and stored response. Distinguish transport retry from business-level duplicate intent.

## Protocol decision

| Context | Typical fit |
| --- | --- |
| Public resource-oriented API | HTTP/JSON REST |
| Internal low-latency typed calls | gRPC |
| Decoupled domain notification | Event or stream |
| Long-running workflow | Async command plus status resource |
| Large offline transfer | Batch/object-storage manifest |

## Drills

Design create-payment, list-orders, bulk-import, and webhook-delivery contracts. Explain duplicates, partial success, ordering, authorization, observability, and compatibility.
