# Advanced Review: Algorithms and Performance

## Advanced model

| Area | Strong SDE-2 explanation |
| --- | --- |
| Correctness | Loop invariant, induction, exchange argument, or contradiction |
| Amortized analysis | Aggregate, accounting, or potential-method reasoning |
| Randomization | Expected cost, probability of failure, adversarial input |
| Streaming | Memory bounds, approximation, sketches, sampling |
| Optimization | Monotonic predicate, pruning, branch and bound |
| Dynamic programming | Minimal state, transition proof, ordering, compression |
| Concurrency | Linearization point, races, ownership, progress guarantees |
| Practical performance | Allocation, locality, boxing, I/O, contention, tail cost |

## Reconstruction exercise

Rebuild a decision tree from input shape and constraints to hashing, window, pointers, binary search, traversal, heap, greedy, backtracking, or DP. For each leaf, write the invariant and one counterexample where it does not apply.

## Drill ladder

- **Foundation:** solve one problem and prove initialization, maintenance, and termination.
- **SDE-2:** solve the same contract in batch and streaming forms.
- **Advanced:** add concurrent updates, skewed data, or approximation.
- **Production:** benchmark two Java implementations and explain beyond Big-O.

## Retrieval prompts

1. When does a sliding window fail despite a contiguous range?
2. How do you prove a greedy choice is safe?
3. What distinguishes amortized cost from average-case cost?
4. How do recursion depth and allocation alter Java performance?
5. Where is the linearization point in a concurrent operation?
6. When is approximation operationally better than an exact answer?
7. How do you detect hidden quadratic behavior?
8. Which DP state can be removed without breaking dependencies?

## Exit criteria

Complete two unseen medium problems in 70 minutes, prove both, adapt one to a changed constraint, and explain one measured Java performance difference.
