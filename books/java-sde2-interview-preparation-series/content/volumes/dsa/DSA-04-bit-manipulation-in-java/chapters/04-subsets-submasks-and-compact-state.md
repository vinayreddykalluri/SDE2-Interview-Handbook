# Subsets, Submasks, and Compact State

## Why masks represent subsets

For `n` indexed items, an `n`-bit value can record whether each item is absent or present. Bit `i` corresponds to item `i`.

For items `[A, B, C]`:

| Mask | Selected items |
|:---:|---|
| `000` | none |
| `001` | A |
| `010` | B |
| `011` | A, B |
| `100` | C |
| `101` | A, C |
| `110` | B, C |
| `111` | A, B, C |

This is not merely a storage trick. It turns membership into integer state, enabling fast checks, compact memoization keys, subset enumeration, and submask transitions.

## Learning objectives

After this chapter, you should be able to:

- map indexed items to bit positions;
- enumerate all subsets without hiding exponential output;
- distinguish index subsets from distinct value subsets;
- enumerate all submasks of one mask;
- derive the `O(3^n)` total mask-submask bound;
- generate Gray-code order and identify its limited benefit;
- use masks as small-state keys for search or dynamic programming; and
- reject mask representations that do not fit the constraints.

## 4.1 Enumerate every subset

For `n` items, masks range from zero through `2^n - 1`.

```java
static List<List<Integer>> subsets(int[] values) {
    if (values == null || values.length > 20) {
        throw new IllegalArgumentException("0..20 items required");
    }
    int total = 1 << values.length;
    List<List<Integer>> result = new ArrayList<>(total);
    for (int mask = 0; mask < total; mask++) {
        List<Integer> subset = new ArrayList<>();
        for (int index = 0; index < values.length; index++) {
            if ((mask & (1 << index)) != 0) {
                subset.add(values[index]);
            }
        }
        result.add(List.copyOf(subset));
    }
    return List.copyOf(result);
}
```

The limit of 20 is an API decision for materialized output, not a universal threshold. At `n = 20`, there are already 1,048,576 subsets.

### Dry run for `[10, 20, 30]`

For mask `101`:

```text
bit 0 is 1 -> include 10
bit 1 is 0 -> skip 20
bit 2 is 1 -> include 30
result: [10, 30]
```

### Complexity

There are `2^n` masks and the baseline checks `n` positions per mask:

```text
time = Theta(n * 2^n)
```

If all subsets are materialized, the output itself contains `Theta(n * 2^n)` element occurrences across all lists. Calling this `O(2^n)` ignores the cost of producing each subset.

Auxiliary working space can be `O(n)` when subsets are streamed to a consumer. Stored result space is output-sized.

## 4.2 Index subsets versus distinct value subsets

If the input is `[2, 2]`, masks `01` and `10` select different indexes but produce the same value list `[2]`.

Bitmask enumeration naturally generates index subsets. If the problem requires unique value combinations, you need an additional strategy:

- sort and skip equal choices during backtracking;
- store generated subsets in a set, usually less efficient; or
- compress value frequencies and enumerate counts.

Do not claim the bitmask loop removes duplicates. Define what a unique subset means in the problem contract.

## 4.3 Stream instead of materialize

When the task only evaluates each subset, avoid storing all of them.

```java
static long maximumSubsetSumAtMost(int[] values, long limit) {
    if (values == null || values.length > 25) {
        throw new IllegalArgumentException("0..25 items required");
    }
    long best = 0;
    long total = 1L << values.length;
    for (long mask = 0; mask < total; mask++) {
        long sum = 0;
        for (int index = 0; index < values.length; index++) {
            if ((mask & (1L << index)) != 0) {
                sum += values[index];
            }
        }
        if (sum <= limit) {
            best = Math.max(best, sum);
        }
    }
    return best;
}
```

This changes storage from output-sized to `O(1)` working state, but time remains exponential. It is useful only for small `n`, and negative values may change pruning or initialization choices.

## 4.4 Enumerate the selected positions efficiently

Instead of scanning all `n` positions, iterate only the set bits:

```java
for (int remaining = mask; remaining != 0; remaining &= remaining - 1) {
    int index = Integer.numberOfTrailingZeros(remaining);
    // process item at index
}
```

For one subset with `k` selected items, this performs `O(k)` iterations. Across all subsets, the total number of selected-item visits is exactly `n * 2^(n - 1)`, because each item appears in half of all subsets. The overall output-sensitive bound remains `Theta(n * 2^n)`.

## 4.5 Enumerate every submask of one mask

A submask contains only positions selected by the original mask.

```java
static List<Integer> nonzeroSubmasks(int mask) {
    List<Integer> result = new ArrayList<>();
    for (int sub = mask; sub != 0; sub = (sub - 1) & mask) {
        result.add(sub);
    }
    return result;
}
```

For `mask = 10110`:

```text
10110 -> 10100 -> 10010 -> 10000
      -> 00110 -> 00100 -> 00010 -> stop
```

If zero is valid, handle it after the loop:

```java
// process zero submask here
```

### Why `(sub - 1) & mask` works

`sub - 1` moves to a lower pattern, possibly turning on positions outside the original mask. AND removes those forbidden positions. Repeating produces the next lower allowed pattern.

For a mask with `k` selected positions, there are `2^k` submasks including zero.

### The zero-loop trap

This form never terminates:

```java
for (int sub = mask; ; sub = (sub - 1) & mask) {
    // after sub becomes zero, the update returns mask again
}
```

Use `sub != 0` and process zero separately, or break explicitly after processing zero.

## 4.6 Why all masks with all submasks cost `O(3^n)`

Consider:

```java
for (int mask = 0; mask < (1 << n); mask++) {
    for (int sub = mask; sub != 0; sub = (sub - 1) & mask) {
        // transition from sub to mask
    }
}
```

Each bit position has three roles:

1. outside `mask`;
2. inside `mask` but outside `sub`; or
3. inside both `mask` and `sub`.

That gives `3^n` mask-submask relationships, excluding or including a lower-order zero case depending on the loop. The bound is not `4^n`; `sub` cannot contain a position outside `mask`.

This derivation is a common SDE-2 follow-up because it tests combinatorial reasoning, not syntax.

## 4.7 Complement within an n-bit universe

`~mask` flips all 32 positions, not only the `n` logical positions.

Wrong for an `n`-item universe:

```java
int complement = ~mask;
```

Correct:

```java
int universe = (1 << n) - 1;
int complement = universe ^ mask;
```

For `n == 32`, the expression `1 << n` fails because the distance is masked. A robust API either uses a `long` for up to 31 or 32 logical items, or handles the full-width case explicitly.

## 4.8 Gray code: change one selected item at a time

The Gray code for sequence number `i` is:

```text
gray(i) = i ^ (i >>> 1)
```

Consecutive Gray codes differ in exactly one bit.

```java
static int grayCode(int index) {
    if (index < 0) {
        throw new IllegalArgumentException("index must be nonnegative");
    }
    return index ^ (index >>> 1);
}
```

First eight values:

```text
i:       000 001 010 011 100 101 110 111
gray(i): 000 001 011 010 110 111 101 100
```

To find the changed position between consecutive masks:

```java
int changed = previous ^ current;
int index = Integer.numberOfTrailingZeros(changed);
```

Gray order helps only if an aggregate can be updated from the one changed item. If each subset still requires a full scan, Gray code does not improve the asymptotic time and may reduce clarity.

## 4.9 Mask as visited-state key

For a small bounded set of tasks, cities, keys, or workers, a mask can encode which items have been used.

Example: assign one distinct job to each worker. `mask` records assigned jobs; `worker = bitCount(mask)` identifies the next worker.

```java
static int minimumAssignmentCost(int[][] cost) {
    int n = cost.length;
    if (n == 0 || n > 20) {
        throw new IllegalArgumentException("1..20 workers required");
    }
    for (int[] row : cost) {
        if (row == null || row.length != n) {
            throw new IllegalArgumentException("cost must be square");
        }
    }

    int total = 1 << n;
    long[] best = new long[total];
    Arrays.fill(best, Long.MAX_VALUE);
    best[0] = 0;

    for (int mask = 0; mask < total; mask++) {
        if (best[mask] == Long.MAX_VALUE) {
            continue;
        }
        int worker = Integer.bitCount(mask);
        if (worker == n) {
            continue;
        }
        for (int job = 0; job < n; job++) {
            int jobBit = 1 << job;
            if ((mask & jobBit) == 0) {
                int next = mask | jobBit;
                best[next] = Math.min(best[next], best[mask] + cost[worker][job]);
            }
        }
    }
    return Math.toIntExact(best[total - 1]);
}
```

State definition: `best[mask]` is the minimum cost to assign the first `bitCount(mask)` workers to exactly the jobs selected in `mask`.

Time is `O(n * 2^n)` and space is `O(2^n)`. This is feasible only for small `n`. The Dynamic Programming volume develops state design, reconstruction, and compression more deeply; here the goal is to understand why a mask can replace a set in the state key.

## 4.10 Recognition versus overuse

Choose a primitive mask when:

- the universe is small and fixed;
- membership is boolean;
- indexes are stable and dense;
- state copying or hashing would otherwise be frequent; or
- a subset-state algorithm already has exponential constraints.

Do not choose a primitive mask when:

- identifiers are sparse or unbounded;
- more than 64 logical positions are required and no segmented design exists;
- readability of named values matters more than compactness;
- the mapping from items to positions is unstable; or
- the underlying search is infeasible even with compact state.

A mask improves representation cost. It does not make an exponential state space polynomial.

## 4.11 Common interview traps

- `1 << n` overflows or wraps before subset enumeration.
- A `long` mask does not make `2^60` work feasible.
- Input duplicates create duplicate value subsets even when masks are unique.
- `~mask` includes bits outside the logical universe.
- Submask enumeration can cycle after zero.
- Materialized-output space is incorrectly reported as `O(1)`.
- The DP state omits information needed for future choices.
- `bitCount(mask)` is used as a step number when not every transition adds exactly one item.
- A mask is used for external identifiers whose numeric values are not dense bit indexes.
- Gray code is introduced without an incremental update that benefits from it.

## 4.12 Interview follow-ups

1. Derive the `Theta(n * 2^n)` total size of all subsets.
2. Why does the mask-submask pair count become `3^n`?
3. How would you generate unique subsets from duplicate input values?
4. When is backtracking clearer than integer-mask enumeration?
5. How would you represent 100 possible flags?
6. Can a mask be used as a `HashMap` key safely? What makes primitive wrapper keys immutable?
7. When does meet-in-the-middle reduce a `2^n` search to roughly `2^(n/2)` work per side?

The final question is an advanced direction, not a default solution. Meet-in-the-middle is covered with array and search techniques later in the series.

## Chapter summary

- An `n`-bit mask naturally represents one subset of `n` indexed items.
- Full subset generation has unavoidable exponential output.
- Index uniqueness and value uniqueness are different contracts.
- `(sub - 1) & mask` enumerates allowed submasks; zero needs deliberate handling.
- All mask-submask relationships across `n` positions total `3^n`.
- Gray code changes one position at a time but helps only with incremental state.
- Bitmask state can replace a small immutable set in search or DP.
- Compact representation does not rescue an infeasible state space.

## Readiness checkpoint

Implement subset and submask enumeration, explain their output-sensitive complexity, diagnose the zero-loop trap, and define one valid `dp[mask]` invariant before continuing to advanced patterns.
