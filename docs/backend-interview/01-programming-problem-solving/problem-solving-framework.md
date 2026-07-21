# Problem-Solving Framework

## Interview sequence

1. Restate inputs, outputs, invalid states, mutability, ordering, duplicates, and scale.
2. Construct one normal example, one smallest example, and one adversarial example.
3. State the direct solution first. It proves understanding and provides a correctness baseline.
4. Identify the repeated work or expensive operation that optimization must remove.
5. Select a data structure or pattern and state its invariant before coding.
6. Implement in small semantic units with names that expose the reasoning.
7. Trace the adversarial example and test empty, singleton, duplicate, overflow, and boundary cases.
8. Derive time and space costs, then discuss the alternative you rejected.

## Correctness explanation

- **Initialization:** the invariant holds before the first iteration or recursive call.
- **Maintenance:** each transition preserves the invariant.
- **Termination:** when execution stops, the invariant implies the required result.

## Drill set

- Change sorted input to unsorted and explain which assumptions break.
- Change offline processing to streaming and reconsider memory.
- Add concurrency or very large input and identify new bottlenecks.
- Replace exact answers with approximate answers and discuss the trade-off.

## Expansion contract

Future examples should include a prompt, clarifying dialogue, brute force, optimized derivation, invariant proof, Java implementation, trace table, tests, complexity, production analogy, and interviewer follow-ups.
