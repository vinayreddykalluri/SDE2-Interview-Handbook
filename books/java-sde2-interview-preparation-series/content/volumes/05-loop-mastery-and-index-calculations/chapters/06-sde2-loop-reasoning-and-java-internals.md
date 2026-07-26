# SDE-2 Loop Reasoning and Java Internals

At SDE-2 level, producing the right output is the starting point. You should also explain why the loop terminates, why it cannot skip an answer, which Java behaviors matter, how work aggregates, what the API mutates, and how the solution behaves under production constraints.

## 6.1 A repeatable design process

Before typing the loop body, write this six-line contract:

```text
Input contract:
Index/range convention:
State meaning:
Invariant:
Progress measure:
Exit meaning:
```

For lower bound:

```text
Input: sorted int array, non-null
Range: searchable [low, high)
State: indexes below low are too small; high is a candidate boundary
Invariant: insertion point is in [low, high]
Progress: high - low strictly decreases
Exit: low == high is first index with value >= target
```

This costs seconds and prevents minutes of patching boundaries.

## 6.2 What “internal implementation” means here

This book explains observable Java mechanics needed to reason about loops:

- evaluation order of initialization, condition, body, update, and exit;
- enhanced-for translation over arrays and `Iterable` values;
- array null and bounds checks;
- iterator state and mutation rules;
- primitive copies versus copied object references;
- numeric promotion and overflow in index expressions;
- JIT optimization only where it affects how to interpret performance claims.

It does not require memorizing bytecode instructions, compiler phases, or processor branch predictors. Those topics belong in JVM and performance-engineering material.

## 6.3 Array loops and bounds-check elimination

Java requires every array access to reject null references and invalid indexes. A JVM may prove that a simple loop index remains within bounds and remove redundant checks from optimized machine code:

```java
for (int index = 0; index < values.length; index++) {
    sum += values[index];
}
```

This does not change Java semantics. Do not manually cache unsafe bounds, use exceptions as loop termination, or write obscure code in hope of reducing checks. Clear canonical loops are easier for humans and optimizers to understand. Use a profiler before making low-level performance claims.

## 6.4 Enhanced-for over collections

Conceptually:

```java
for (String item : items) {
    use(item);
}
```

behaves like:

```java
for (java.util.Iterator<String> iterator = items.iterator();
        iterator.hasNext();) {
    String item = iterator.next();
    use(item);
}
```

The iterator owns a cursor and may track structural modification state. Many standard mutable collections are fail-fast on a best-effort basis: structural modification outside the iterator can produce `ConcurrentModificationException`. It is a bug detector, not a concurrency guarantee.

### Incorrect structural mutation

```java
for (String item : items) {
    if (item.isBlank()) {
        items.remove(item); // unsafe during this iteration
    }
}
```

### Correct iterator removal

```java
for (java.util.Iterator<String> iterator = items.iterator();
        iterator.hasNext();) {
    if (iterator.next().isBlank()) {
        iterator.remove();
    }
}
```

Or use `items.removeIf(String::isBlank)` when its contract matches the need. Different collection implementations have different iteration and concurrency semantics; consult the Collections volume before generalizing.

## 6.5 Iteration order is part of correctness

`ArrayList` iteration follows list order. `LinkedHashMap` follows its configured encounter order. `TreeSet` follows sorted order. `HashMap` and `HashSet` do not promise a stable logical order. `PriorityQueue` iteration is not sorted order even though repeated `poll()` returns priority order.

If output order matters, name it in the contract and choose a structure that guarantees it. A test passing under one HashMap layout is not proof.

## 6.6 Mutation during index traversal

Arrays have fixed length, so replacing an element does not invalidate later indexes. Lists can change size and shift positions:

```java
for (int index = 0; index < list.size(); index++) {
    if (shouldRemove(list.get(index))) {
        list.remove(index);
        index--; // otherwise the shifted element is skipped
    }
}
```

This can be correct but is easy to misuse and may be quadratic for an ArrayList because every removal shifts a suffix. Prefer `removeIf`, an iterator, reverse index deletion, or read/write compaction depending on order and ownership requirements.

## 6.7 Numeric promotion inside loop expressions

An assignment target does not control the arithmetic width:

```java
long total = rows * columns;          // int multiplication first
long safe = (long) rows * columns;    // long multiplication
```

The same applies to sums, differences, counts, and midpoint formulas. Candidate code commonly fails at extreme constraints because it widens after overflow.

Also remember compound assignment includes an implicit cast:

```java
short position = 0;
position += 1;       // behaves like position = (short) (position + 1)
```

Do not use narrow loop counters for cleverness. `int` is conventional for Java array indexes; use `long` for counts or abstract coordinate products that can exceed `int`.

## 6.8 Complexity from movement, visits, and API calls

Analyze a loop in this order:

1. count how often each pointer can move;
2. count how many data items or states are visited;
3. include the cost of calls inside the body;
4. separate one-time setup from repeated work;
5. separate auxiliary space from returned output;
6. qualify expected, amortized, or implementation-dependent costs.

Examples:

- two monotone pointers over one array: `O(n)` total movement;
- two pointers over arrays of lengths `n` and `m`: `O(n + m)`;
- every pair `(i,j)`: `O(n^2)` visits even if the body is constant;
- `substring` or copying inside a loop: include copied length under the selected Java contract;
- HashMap access: usually expected constant time, not an unconditional universal guarantee;
- outputting every subarray: output itself can be `Theta(n^2)` or larger.

The Time and Space Complexity volume develops the formal notation and Java collection cost models.

## 6.9 Correctness review by region

Draw the array as named regions:

```text
[ processed ][ active ][ unknown ]
0           left     right          n
```

Then ask:

- Which regions may be read?
- Which may be overwritten?
- Is the active range closed or half-open?
- Does a swap import an unknown value that needs reprocessing?
- At exit, which region is the result?

This method exposes compaction, partition, binary-search, and window errors faster than staring at individual assignments.

## 6.10 Debugging with traces

Use a compact state table rather than printing the whole object graph:

```text
iteration | left | right | state summary | decision
```

For a variable window, include entering value, departing value, aggregate before/after shrinking, and current answer. For a matrix, log `(row,col)` and boundaries. Turn the trace off after diagnosis; production hot loops should not emit per-item logs.

### Assertions for development

```java
assert 0 <= left && left <= right && right <= values.length;
```

Assertions are disabled unless enabled with `-ea`, so do not use them for public input validation. They are useful for checking internal invariants during testing.

## 6.11 Testing loop behavior systematically

Build tests from dimensions:

- size: empty, one, two, typical, large;
- position: first, middle, last, absent;
- value shape: all equal, sorted, reverse, duplicates, alternating;
- numeric boundary: zero, negative where allowed, MIN/MAX;
- matrix shape: 0x0, 1xN, Nx1, square, wide, tall, ragged, null row;
- contract: invalid width, invalid `k`, null input, ownership/mutation.

Property-based tests can assert general truths:

- lower bound result is in `[0,n]`;
- every index below it is too small;
- compacted prefix contains no removed value;
- flatten then unflatten returns the original valid cell;
- spiral output count equals `rows * columns` and contains every unique cell once.

## 6.12 Production loop concerns

### Cancellation and deadlines

Long-running traversals should cooperate with cancellation when used in services or jobs. Checking on every element can add overhead; checking every fixed batch bounds latency. Document partial-result behavior.

### Memory and output limits

A linear traversal can still allocate enormous output. Validate requested result size, use iterators/streams/callbacks where appropriate, and distinguish “working memory” from “all results retained.”

### Ownership and aliasing

An in-place method must say that it mutates input and which prefix or region remains valid. If the caller retains aliases, mutation is visible through all of them. A defensive copy improves isolation but changes space and latency.

### Concurrency

Do not mutate ordinary collections concurrently without a designed synchronization or concurrent-collection contract. Fail-fast exceptions do not make unsynchronized iteration safe. Stateful windows and pointer algorithms usually have sequential dependencies; parallelism needs a partition-and-reconcile design.

### Observability

Record input sizes, cardinalities, rejection reasons, duration, and cancellation. Avoid logging full customer data. Metrics should help distinguish large input from an algorithmic regression.

## 6.13 Weak versus strong interview communication

Weak:

> I use two pointers, so it is O(n).

Strong:

> The input is sorted. `left` and `right` bound the only remaining candidate pairs. If the sum is small, no pair using the current left value can reach the target, so I increment left; the symmetric argument holds for a large sum. Each pointer moves inward at most n times, so total time is O(n), with O(1) working space. I widen before addition and return original indexes because I do not sort inside the method.

The strong explanation states prerequisite, invariant, elimination proof, aggregate complexity, overflow behavior, and contract.

## 6.14 Pattern selection map

| Signal | First pattern to consider | Required proof |
|---|---|---|
| all values in order | one forward/reverse loop | processed prefix/suffix |
| sorted pair or mirrored comparison | opposing pointers | safe elimination |
| remove or retain in place | read/write compaction | valid retained prefix |
| merge sorted inputs | one pointer per input | sorted produced prefix |
| exact contiguous width | fixed window | aggregate matches active range |
| variable valid contiguous range | variable window | monotonic restoration |
| first boundary in sorted predicate | lower/upper bound | candidate interval shrinks |
| rectangular cells | nested/flat traversal | every cell once |
| perimeter or layers | shrinking boundaries | processed outside rectangle |

## 6.15 Rapid revision sheet

```text
Array indexes:             [0, n)
Half-open length:          right - left
Closed length:             right - left + 1
Reverse start:             n - 1
Safe midpoint:             low + (high - low) / 2
Inclusive window length:   right - left + 1
Fixed departing index:     right - width
Row-major flatten:         (long) row * columns + col
Unflatten row/col:         flat / columns, flat % columns
All subarrays:             (long) n * (n + 1) / 2
All unordered pairs:       (long) n * (n - 1) / 2
```

## 6.16 Final loop review checklist

- [ ] Input validity and mutation ownership are explicit.
- [ ] Every index and boundary has one meaning.
- [ ] The range convention is written.
- [ ] The invariant is true initially and preserved.
- [ ] Every branch makes progress or exits.
- [ ] Exit state implies the result.
- [ ] Arithmetic widens before it can overflow.
- [ ] Complexity counts total movement and body-call costs.
- [ ] Output space is separated from working space.
- [ ] Empty, singleton, endpoints, duplicates, and extremes are tested.
- [ ] Java iteration order and mutation semantics are not assumed incorrectly.
- [ ] Production limits, cancellation, and observability are considered where relevant.

## 6.17 Where to continue

- Array transformations, prefix sums, difference arrays, rotations, and partitions: **Arrays and Array Problem-Solving Patterns**.
- Unicode-aware windows and text traversal: **Strings and String Problem-Solving Patterns**.
- Frequency maps and prefix-state counting: **Hashing**.
- Answer-space and rotated searches: **Binary Search**.
- Slow/fast pointer mutation on nodes: **Linked Lists**.
- Loop-growth proofs and cost models: **Time and Space Complexity**.

Use the practice lab next. Do not read solutions immediately: predict, trace, implement, compile, test boundaries, and only then compare reasoning.
