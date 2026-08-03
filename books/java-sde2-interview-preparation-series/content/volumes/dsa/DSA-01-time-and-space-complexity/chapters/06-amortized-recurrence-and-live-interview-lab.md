# Amortized Analysis, Recurrence Trees, and Live Interview Reasoning

> **A note from Vinay:** Complexity becomes useful when it changes a decision. Do not begin by guessing a Big-O label. Write down what changes, how often it can change, and what work one change causes. The notation should be the last line of the explanation, not the first.

## 1. The three questions behind a complexity answer

When an interviewer asks for complexity, answer three different questions:

1. What is the input-size variable?
2. How many times can each important operation happen?
3. What additional memory grows with that input?

For a graph, `n` is usually not enough. State vertices `V` and edges `E`. For a matrix, use rows `R` and columns `C`. For a sliding window, separate loop nesting from total pointer movement.

## 2. Recurrence trees without magic

Consider merge sort on `n` elements:

```text
T(n) = 2T(n / 2) + n
```

The two recursive calls divide the input. The `+ n` is the merge work performed after those calls.

```text
Level 0:                 n                         total n
Level 1:            n/2     n/2                    total n
Level 2:         n/4 n/4 n/4 n/4                  total n
...
Level log2(n):       n leaves of size 1            total n
```

There are `log2(n) + 1` levels and each non-leaf level performs `n` merge work, so the total is `O(n log n)`.

The useful habit is to calculate work per level and number of levels separately.

### Unequal branches

For:

```text
T(n) = T(n - 1) + O(1)
```

the problem shrinks by one, so there are `n` levels and constant work per level: `O(n)`.

For naive Fibonacci:

```text
T(n) = T(n - 1) + T(n - 2) + O(1)
```

the tree branches and recomputes the same arguments. A safe interview statement is exponential time, commonly bounded as `O(2^n)`. Do not claim that bound is exact.

## 3. A usable Master-Theorem boundary

The familiar recurrence is:

```text
T(n) = aT(n / b) + f(n)
```

It describes `a` equal-sized recursive subproblems of size `n / b`, plus non-recursive work `f(n)`.

Use the theorem only when the recurrence actually has that shape. It does not directly solve:

- `T(n) = T(n - 1) + n`;
- unequal splits such as `T(n) = T(n / 3) + T(2n / 3) + n`;
- a recurrence whose subproblem size depends on data rather than a fixed ratio.

In those cases, use expansion, a recursion tree, substitution, or a justified upper bound.

## 4. Amortized dynamic-array growth

An append to a dynamic array is usually constant work. Occasionally the backing array is full, so the implementation allocates a larger array and copies existing elements.

Suppose capacity doubles:

```text
capacity copied
1        1
2        2
4        4
8        8
...
```

Before reaching size `n`, the number of copied elements is less than:

```text
1 + 2 + 4 + ... + largest power below n < 2n
```

The `n` ordinary writes plus fewer than `2n` copied writes give fewer than `3n` element writes across `n` appends. The average, or amortized, cost per append is therefore `O(1)`.

This does not mean every append is `O(1)`. One resize can still cost `O(n)`. It also does not promise that every Java collection grows by exactly two; the proof must use the implementation's actual geometric-growth rule or a qualified abstract model.

## 5. Aggregate analysis defeats misleading nesting

```java
int right = 0;
for (int left = 0; left < values.length; left++) {
    while (right < values.length && canInclude(values[right])) {
        right++;
    }
}
```

The `while` loop is nested syntactically, but `right` never moves backward. Across the whole execution, `left` moves at most `n` times and `right` moves at most `n` times. Total pointer movement is at most `2n`, so the traversal is `O(n)`, assuming `canInclude` is constant time.

The same argument explains stack-based algorithms in which every item is pushed once and popped at most once.

## 6. Space: live memory, not total allocations

A program may allocate many short-lived objects while keeping only a small number live at once. Auxiliary-space analysis asks for peak additional live memory, not every allocation ever performed.

For recursive code, include the call stack. A linear recursion can use `O(n)` stack depth even when it allocates no collection. A balanced divide-and-conquer recursion commonly has `O(log n)` active depth, although its total work may be much larger.

## 7. Complete Java evidence

The companion `ComplexityDeepDiveExamples.java` measures three claims without pretending a measurement is a proof:

- doubling-array copy count remains below `2n`;
- a monotonic right pointer moves at most `n` times;
- naive Fibonacci repeats calls while memoization evaluates each state once.

Measurements illustrate the model. The recurrence or aggregate argument establishes the bound.

## 8. Edge and failure matrix

| Situation | Incorrect shortcut | Correct treatment |
|---|---|---|
| Empty input | “The loop is O(n), so it runs once” | Zero iterations; asymptotic bound still describes growth |
| Nested monotonic pointers | Multiply loop bounds automatically | Count total movement of each pointer |
| Hash lookup | “Guaranteed O(1)” | State expected behavior and key/distribution assumptions |
| Recursive helper | Ignore stack space | Include maximum active call depth |
| Integer operation counter | Let the counter overflow | Use `long`, exact arithmetic, or a mathematical bound |
| String concatenation in a loop | Count `+` as constant | Include copied characters and temporary strings |
| `subList` or a view | Assume an independent copy | Check the API contract and backing relationship |
| Parallel work | Divide sequential time by thread count | Include scheduling, coordination, imbalance, and work/span |

## 9. Six live interview rounds

### Round 1 - The nested loop that is linear

**Interviewer:** Two pointers appear in nested loops. Is the algorithm quadratic?

**Candidate opening:** Not necessarily. I will count pointer movement rather than multiply the visible loop bounds.

**Model answer:** If both pointers only move forward, each crosses the array at most once. The inner loop may run many times for one outer iteration, but across all iterations it advances at most `n` times. The aggregate time is `O(n)` and the extra space is `O(1)`.

**Follow-up:** What breaks the argument? If the inner pointer is reset for every outer iteration, it may move `n` times for each of `n` starts, producing `O(n^2)` work.

### Round 2 - ArrayList append

**Interviewer:** Is `add` constant time?

**Candidate opening:** Appending is amortized constant time, but an individual growth operation copies the existing elements.

**Model answer:** Under geometric capacity growth, copies across `n` appends form a geometric series bounded by a constant multiple of `n`. Therefore total work is `O(n)` and amortized append is `O(1)`. The exact growth factor and maximum capacity are implementation details.

**Follow-up:** What latency might production code observe? A single resize can cause an `O(n)` pause and allocate a new backing array, so pre-sizing may matter for known large workloads.

### Round 3 - Recursive space

**Interviewer:** A recursive function uses no array or map. Is auxiliary space `O(1)`?

**Model answer:** No. Each active call has a stack frame. If the maximum recursion depth is `d`, auxiliary stack space is `O(d)`. For a linear recursion, `d` may be `n`; for balanced binary splitting, active depth is often `log n`.

**Follow-up:** Does Java guarantee tail-call elimination? No. Do not remove stack-space cost on that assumption.

### Round 4 - HashMap frequency counting

**Interviewer:** What is the complexity of counting frequencies?

**Model answer:** With `n` input items and ordinary key distribution, `n` expected constant-time map updates give expected `O(n)` time and `O(u)` space for `u` distinct keys. I would not call the operations universally guaranteed `O(1)`.

**Follow-up:** What else affects performance in Java? Hash computation, equality cost, collisions, resizing, boxing, object allocation, and memory locality.

### Round 5 - Recurrence selection

**Interviewer:** Solve `T(n) = 2T(n/2) + n`.

**Model answer:** Each level performs `n` non-recursive work, and halving produces `log n` levels, so the total is `O(n log n)`. The recursion has `O(log n)` active depth if the two calls execute sequentially.

**Follow-up:** Why is total recursion-tree node count not the space bound? Most nodes are not active simultaneously; space follows the deepest active path plus other live state.

### Round 6 - SDE-2 optimization discussion

**Interviewer:** You replaced an `O(n log n)` sort with an expected `O(n)` hash solution. Is it automatically better?

**Model answer:** No. I would compare constraints and contracts: hash state costs `O(u)` additional memory, has expected rather than universal bounds, may lose ordering, and can increase allocation. Sorting may mutate input but offers predictable ordering and often good locality. The best choice follows the required output, memory budget, key behavior, and input size.

## 10. Rapid interviewer questions with model answers

1. **Can constants matter?** Yes in real measurements, but Big-O intentionally describes growth after constants are removed.
2. **Is `O(n + n)` different from `O(n)`?** No asymptotically; both simplify to `O(n)`.
3. **Is `O(n/2)` different from `O(n)`?** No asymptotically.
4. **Can an algorithm be both `O(n)` and `O(n^2)`?** Yes, because Big-O is an upper bound; use the tightest useful bound.
5. **What is output-sensitive complexity?** A bound that includes output size, such as generating all subsets in `Theta(2^n)` outputs.
6. **What is pseudo-polynomial time?** Polynomial in a numeric value but not in the number of bits needed to encode it.
7. **What is amortized versus average-case?** Amortized bounds a sequence of operations without a probability distribution; average-case assumes one.
8. **Why can binary search still be wrong with `O(log n)`?** Complexity does not prove interval updates, ordering assumptions, or boundary correctness.
9. **What is an in-place algorithm?** Usually one using `O(1)` auxiliary space, but state the convention and whether recursion stack/output are excluded.
10. **Why is repeated immutable-string concatenation risky?** Each result may copy the accumulated characters, producing quadratic total character work.
11. **What is work versus span?** Work is total operations; span is the longest dependent chain and limits possible parallel speedup.
12. **When should complexity be measured?** After deriving it, benchmark when constants, allocation, cache behavior, JIT compilation, or I/O can change the engineering decision.

## 11. Readiness check

You are ready to continue when you can derive, without memorized labels:

- one recursion-tree bound;
- one aggregate two-pointer bound;
- one amortized growth bound;
- one stack-space bound;
- one qualified collection bound;
- one case where the asymptotically faster choice is not the better engineering choice.
