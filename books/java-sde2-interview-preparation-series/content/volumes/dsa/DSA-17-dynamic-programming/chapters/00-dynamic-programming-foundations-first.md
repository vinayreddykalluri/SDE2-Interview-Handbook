# Dynamic Programming Foundations: Derive State Before Writing a Table

Dynamic programming is a way to evaluate repeated subproblems once. It is not a collection of unrelated formulas. A correct DP starts with a precise state sentence.

## See repetition in a recursive tree

For Fibonacci:

```text
fib(5)
  fib(4)
    fib(3)
    fib(2)
  fib(3)       repeated
```

The recursion has optimal substructure - larger answers combine smaller answers - and overlapping subproblems - the same arguments recur.

```java
static long fibonacciMemo(int n, long[] memo) {
    if (n <= 1) return n;
    if (memo[n] != -1L) return memo[n];
    memo[n] = fibonacciMemo(n - 1, memo) + fibonacciMemo(n - 2, memo);
    return memo[n];
}
```

Memoization preserves demand-driven recursion but still uses call-stack depth. Tabulation chooses an explicit evaluation order:

```java
static long fibonacci(int n) {
    if (n < 0) throw new IllegalArgumentException("negative n");
    if (n <= 1) return n;
    long previous = 0L;
    long current = 1L;
    for (int index = 2; index <= n; index++) {
        long next = Math.addExact(previous, current);
        previous = current;
        current = next;
    }
    return current;
}
```

Space compression is safe because each state needs only the previous two states. Numeric overflow remains a separate correctness boundary.

## The six questions for every DP

1. **State:** What does `dp[...]` mean in one complete sentence?
2. **Choices:** What can the current decision do?
3. **Transition:** Which smaller states produce this state?
4. **Base:** Which states are known without recurrence?
5. **Order:** Are dependencies computed before use?
6. **Answer:** Which state or aggregate is returned?

Add two more for SDE-2:

7. **Reconstruction:** What parent/choice evidence recovers an actual solution?
8. **Compression:** Which older states are provably dead?

## Example: House Robber from a state sentence

State:

```text
dp[i] = maximum amount obtainable from houses in indexes [0, i)
```

At boundary `i`, either skip house `i-1` and keep `dp[i-1]`, or take it and add its value to `dp[i-2]`.

```text
dp[i] = max(dp[i-1], dp[i-2] + value[i-1])
dp[0] = 0
dp[1] = max(0, value[0]) under an allow-skip-all contract
```

This formulation exposes boundary indexes and handles empty input naturally.

## Memoization versus tabulation

| Question | Memoization | Tabulation |
|---|---|---|
| Evaluation | only reached states | chosen order, often all states |
| Stack | recursive depth | usually none |
| Sparse state | can avoid unreachable states | may need explicit sparse map |
| Ordering | follows recursion | must be derived |
| Debugging | mirrors recurrence | table can be inspected |

Neither is automatically faster. Compare reachable-state count, call overhead, iteration locality, and stack safety.

## One-dimensional and two-dimensional state

Add a dimension only when the future answer depends on that information. For 0/1 knapsack:

```text
dp[item][capacity] = best value using first item items within capacity
```

The item dimension distinguishes which choices remain. The capacity dimension distinguishes remaining resource. The transition compares skipping and taking the current item.

## Evaluation order is correctness

When compressing 0/1 knapsack to one dimension, capacities must iterate downward. Upward iteration would reuse the current item multiple times, silently changing the problem to unbounded knapsack.

```java
for each item:
    for capacity from limit down to itemWeight:
        dp[capacity] = max(dp[capacity], dp[capacity - itemWeight] + itemValue)
```

For unlimited reuse, upward iteration is often intentional. Loop direction encodes the reuse contract.

## Counting, feasibility, and optimization differ

The same input shape can ask:

- feasibility: can a sum be formed? use boolean state;
- count: how many ways? use additive state and define order/combinations;
- minimum: fewest items? use infinity sentinel and min transition;
- maximum: best value? use max transition;
- reconstruction: which choices? store parents or retain enough table state.

Do not transplant a recurrence without restating the state.

## Complexity from state space

```text
time = number of reachable states * transition work per state
space = stored states + recursion depth + reconstruction/output
```

For `n * capacity` knapsack states with O(1) transition work, time is O(n * capacity). This is pseudo-polynomial because capacity is a numeric value, not its bit-length.

## Compression caution

Compression can destroy information needed to reconstruct choices, process dependencies in the wrong order, or obscure correctness. Derive a full correct table first, draw dependency arrows, then retain only live layers.

## Foundation checkpoint

1. State House Robber DP in one sentence.
2. Why does memoization reduce repeated work but not recursion depth?
3. Why does 0/1 knapsack iterate capacity downward after compression?
4. What distinguishes combination count from permutation count?
5. How do you derive complexity without memorizing it?
