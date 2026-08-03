# The Recursion Engine and Backtracking State

Recursion is a way to let each call own one smaller piece of unfinished work. Backtracking adds reversible choices: choose, explore, undo, then try the next choice. If those two ideas are clear, subsets, permutations, combination search, board search, and N-Queens stop looking like unrelated templates.

The complete Java 21 examples are in `RecursionInterviewChecks.java`.

## What one call actually owns

For `sum(values, length)`, one call promises: “return the sum of the first `length` values.”

```text
sum([4,7], 2) waits for sum(...,1), then adds 7
  sum([4,7], 1) waits for sum(...,0), then adds 4
    sum([4,7], 0) returns 0
  returns 4
returns 11
```

Each active frame has its own `length` and suspended addition. Local variables are not shared merely because the method is the same. The base case answers the smallest valid problem without another call. The recursive case must move strictly toward it.

The companion records entry and return events so the unwind order is executable, not imaginary.

## Three proof questions

Before writing a recursive method, answer:

1. **Contract:** what does one call return or produce?
2. **Progress:** which measure becomes smaller?
3. **Base:** which smallest state can be answered directly?

For a tree, progress may be moving to a child. For backtracking, it may be increasing the decision depth. “It probably reaches the base” is not a termination argument.

## Backtracking is controlled mutation

A standard path search has this lifecycle:

```text
for each legal choice:
    apply choice
    recurse on the smaller remaining decision space
    undo exactly that choice
```

The invariant is:

> On entry at depth `d`, `path` represents exactly the choices made at depths `[0,d)`, and all auxiliary used-state agrees with that path.

Store `new ArrayList<>(path)` when recording an answer. Storing the mutable `path` reference records many aliases to the same object; after undo, every “answer” can appear empty.

## Subsets versus permutations

Subsets choose increasing source indexes. After choosing index `i`, recurse from `i + 1`. Order is not part of the result.

Permutations choose any unused index at every depth. A `used[]` array is state, and its mark must be undone after recursion. Order is the result.

For sorted duplicate values, unique permutations use this rule:

```text
skip values[i] when it equals values[i-1]
and the earlier equal index has not been used in this branch
```

That chooses a canonical order among indistinguishable siblings. Skipping every repeated value globally would wrongly prevent using both copies in one permutation.

## Reuse versus consume once

In combination sum, recursing with the same candidate index permits reuse. Recursing with `index + 1` consumes a candidate once. This one parameter changes the problem contract.

The companion sorts positive candidates, removes duplicate sibling branches, stops when a candidate exceeds the remaining target, and rejects zero/negative candidates. Without positivity, repeatedly choosing zero never makes progress and negative values invalidate the simple pruning rule.

## Board search and state restoration

Word search uses `(row, column, wordIndex)` as logical state and a visited matrix as reversible branch state.

```text
match cell
mark used
explore four neighbors
unmark used before returning
```

The companion does not overwrite board characters with a sentinel, because any chosen sentinel might be valid input. It supports jagged rows by validating bounds against the current row, and it proves the board is unchanged after both successful and failed searches.

Short-circuit `||` is safe only because every explored call restores its own state before returning. Restoration must not be placed after an early `return true` that bypasses cleanup.

## N-Queens: prune with maintained constraints

Placing one queen per row removes the need to track used rows. Three boolean arrays track columns, descending diagonals, and ascending diagonals:

```text
column key:              column
descending diagonal:    row - column + n - 1
ascending diagonal:     row + column
```

Choose a safe column, mark all three, recurse to the next row, and unmark. This prunes invalid partial boards immediately. `n=0` has one empty arrangement—the combinatorial identity useful to recursive composition. The teaching implementation caps `n` because solution count and runtime grow rapidly.

## Complexity: include the output

Generating `n!` permutations cannot be `O(n)` because the output itself contains `n! * n` values when materialized. State:

- recursion depth;
- branching factor;
- number and size of outputs;
- auxiliary state such as `used[]`; and
- whether copying each answer costs `O(path length)`.

Pruning improves visited state in practice but does not automatically change the worst-case family.

## Edge-case matrix

| Case | Correct contract | Common failure |
|---|---|---|
| empty input | one empty subset/permutation | returning no combinatorial identity |
| missing base case | reject in review | infinite recursion/stack overflow |
| huge linear depth | iterative alternative or explicit limit | assuming heap memory prevents stack failure |
| duplicate values | sort and skip duplicate siblings | duplicate outputs or lost valid copies |
| mutable path answer | snapshot it | every result aliases the final path |
| early successful board path | still restore branch state | board/visited state leaks |
| zero combination candidate | reject under reuse contract | no progress |
| negative target | reject or define a different bounded problem | invalid `candidate > remaining` pruning |
| `n=0` queens | one empty arrangement | inconsistent recursion identity |
| Unicode string search | define code unit/code point model | treating every `char` as a full character |

## Six live interview Q&A chains

### 1. Base case and progress

**Interviewer:** Why does your recursion terminate?

**Candidate:** One call owns a nonnegative `length`. The recursive branch calls `length - 1`, so the measure strictly decreases, and length zero returns without another call.

**Interviewer:** What if the caller passes `-1`?

**Candidate:** That is outside the contract. I validate at the boundary instead of hoping it reaches zero.

### 2. Result aliasing

**Interviewer:** Why copy `path` into the result?

**Candidate:** `path` is reused and undone. Adding the same reference would make all results observe later mutations. A snapshot freezes the answer at that leaf.

**Interviewer:** What is the cost?

**Candidate:** `O(path length)` per recorded result, which belongs in output-sensitive complexity.

### 3. Duplicate permutations

**Interviewer:** Why is `values[i] == values[i-1]` not enough to skip a duplicate?

**Candidate:** Two equal copies may both appear in one permutation. I skip the later copy only when its earlier twin is unused at this depth, enforcing sibling order without banning reuse of distinct indexes.

### 4. Combination candidates

**Interviewer:** Can your combination-sum code accept zero?

**Candidate:** Not with unlimited reuse. Choosing zero leaves the remaining target unchanged, so recursion does not progress. I reject nonpositive candidates; a different input contract needs different bounds/state.

### 5. Board mutation

**Interviewer:** Why use a visited matrix instead of writing `'#'` into the board?

**Candidate:** It avoids a sentinel collision and makes preservation explicit. It costs `O(rows*columns)` state. If the domain excludes a sentinel and mutation is permitted, temporary marking can reduce auxiliary space, but restoration is mandatory.

### 6. Recursion versus iteration

**Interviewer:** Is recursion always cleaner?

**Candidate:** It often mirrors a bounded-depth search proof, but a million-node chain can overflow Java's call stack. For untrusted depth I use an explicit stack of frames containing the same state and next-choice position. Recursion changes syntax, not the amount of unfinished work.

## Run the companion

```bash
javac --release 21 -Xlint:all -Werror RecursionInterviewChecks.java
java RecursionInterviewChecks
```

Expected final line: `PASS 17 recursion checks`.
