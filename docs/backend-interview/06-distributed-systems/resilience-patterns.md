# Resilience Patterns and Failure Analysis

## Pattern map

| Failure pressure | Mechanism | Misuse to avoid |
| --- | --- | --- |
| Transient dependency failure | Bounded retry with jitter | Retrying non-idempotent work or overload |
| Slow dependency | Timeout budget | Timeouts that exceed caller deadline |
| Repeated failure | Circuit breaker | Treating it as root-cause recovery |
| Resource exhaustion | Bulkhead | Sharing the saturated pool underneath |
| Excess demand | Backpressure/load shedding | Unbounded queues that hide failure |
| Optional dependency loss | Graceful degradation | Silently incorrect core data |
| Multi-step failure | Compensation/reconciliation | Assuming rollback is instantaneous |

## Failure-analysis sequence

1. Name the component and failure mode: crash, timeout, corruption, overload, or partition.
2. Trace propagation through threads, pools, queues, caches, and callers.
3. Define user-visible behavior and consistency impact.
4. Identify detection signals and alert thresholds.
5. Contain blast radius and preserve the most important function.
6. Recover, reconcile, and verify correctness.

## Ready when

Given an architecture diagram, select three credible failures, explain propagation, and add bounded mechanisms with measurable recovery behavior.
