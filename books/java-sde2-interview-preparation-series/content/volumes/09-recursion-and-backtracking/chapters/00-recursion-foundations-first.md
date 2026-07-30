# Recursion Foundations: Contracts Before Search Trees

Recursion is not "a method calling itself until it stops." A reliable recursive method states what one call promises, handles the smallest complete cases, and reduces every other case toward them.

## Translate a loop into a smaller problem

To sum the first `n` array elements, define this contract:

```text
sum(values, n) returns values[0] + ... + values[n - 1]
```

The smallest complete instance is `n == 0`, whose sum is zero. For `n > 0`, the answer is the sum of the first `n - 1` elements plus `values[n - 1]`.

```java
static long sum(int[] values, int n) {
    if (n == 0) {
        return 0L;
    }
    return sum(values, n - 1) + values[n - 1];
}
```

For `[4, 7, 2]`:

```text
sum(a, 3)
  waits for sum(a, 2) + 2
    waits for sum(a, 1) + 7
      waits for sum(a, 0) + 4
        returns 0
      returns 4
    returns 11
  returns 13
```

Each active call owns its parameter values, local variables, and return location. Java does not guarantee tail-call optimization, so recursion depth is real auxiliary stack usage.

## The four correctness questions

Before coding, answer:

1. **Contract:** What exactly does one call return or accomplish?
2. **Base case:** Which smallest inputs can be answered directly?
3. **Progress:** Why does every recursive branch move toward a base case?
4. **Combination:** How do smaller answers form the current answer?

A missing base case can cause `StackOverflowError`. A base case that is never reached is equally broken. A recursive call on the same state does not make progress.

## Return-value recursion versus mutable-state recursion

Return-value recursion computes an answer from child answers, as in height or sum. Mutable-state recursion updates shared or caller-owned state, as in building a list of paths. State ownership must be explicit.

```java
static void collectCountdown(int n, List<Integer> output) {
    if (n == 0) {
        return;
    }
    output.add(n);
    collectCountdown(n - 1, output);
}
```

The reference value is passed by value, but both caller and callee refer to the same list object. Mutations remain visible after the call returns.

## From recursion to a decision tree

For subsets, each index offers two choices: exclude the value or include it.

```text
                         [] at index 0
                    /                     \
             exclude 1                  include 1
               []                          [1]
           /        \                  /        \
      exclude 2   include 2       exclude 2   include 2
         []          [2]              [1]        [1,2]
```

The path is the partial candidate. The index is the next decision. A leaf is a complete set of decisions.

## Backtracking is reversible mutation

The standard transaction is:

```text
choose -> explore -> unchoose
```

```java
static void subsets(int[] values, int index,
                    List<Integer> path,
                    List<List<Integer>> output) {
    if (index == values.length) {
        output.add(new ArrayList<>(path));
        return;
    }

    subsets(values, index + 1, path, output);

    path.add(values[index]);
    subsets(values, index + 1, path, output);
    path.remove(path.size() - 1);
}
```

The copy at the leaf is essential. Adding `path` directly would store repeated references to one mutable list. The removal is equally essential: on return, the caller must see the path exactly as it was before the choice.

## State ownership options

| State | Typical owner | Restoration rule |
|---|---|---|
| `index`, remaining target | current call | automatic; values are local |
| `path` list | shared along one DFS path | undo the last choice |
| `used[]` for permutations | shared constraint state | reset the chosen position |
| board marker | shared input or work buffer | restore before return |
| output list | whole search | append completed snapshots; do not undo output |

Use `try/finally` when restoration must occur even if deeper code can throw:

```java
char saved = board[row][column];
board[row][column] = '#';
try {
    return searchNeighbor(board, row, column);
} finally {
    board[row][column] = saved;
}
```

## Pruning must be proved safe

Pruning removes branches that cannot produce a valid or better answer. Examples:

- stop a fixed-size combination when too few elements remain;
- stop a positive-number combination when the remaining target is negative;
- skip a value already used at the same depth when generating unique permutations;
- stop optimization search when an admissible lower bound is no better than the best result.

Do not copy a pruning rule across contracts. `remaining < 0` is unsafe when later values can be negative.

## Complexity without hand-waving

Count three separate quantities:

- number of decision-tree nodes visited;
- work performed per node, including copying a path;
- maximum call depth and mutable state size.

Generating all subsets has `2^n` leaves and must output `n * 2^(n-1)` element occurrences across all subsets. The algorithm is output-sensitive; saying only O(2^n) hides copying cost.

## When not to recurse

- Depth can be proportional to a large untrusted input.
- An iterative loop expresses a linear scan more clearly.
- A graph traversal may exceed the Java stack; an explicit deque gives controlled heap-backed storage.
- Repeated subproblems indicate memoization or dynamic programming, not plain backtracking.

## Foundation checkpoint

1. State the contract and base case for recursive binary-tree height.
2. Why must `path` be copied at a solution leaf?
3. What state is restored in a permutation search?
4. Why is recursion depth part of auxiliary space?
5. What proof is required before adding a prune?
