# The SDE-2 Array Interview Playbook

An interview-quality solution is more than a correct loop. It makes the contract, invariant, ownership, complexity, and evidence visible. This chapter turns the patterns into a repeatable conversation.

## Step 1: Clarify the Contract

Before coding, resolve the questions that change the algorithm:

- Can the input be `null` or empty?
- Are values positive, signed, unique, sorted, or bounded?
- Can arithmetic exceed `int` or `long`?
- Is mutation permitted? Must input order be preserved?
- Is the result a value, indexes, elements, or every matching range?
- How should ties be resolved?
- Are intervals closed or half-open?
- What are `n`, value range, memory limits, and expected call frequency?

Do not invent defensive behavior. If a platform guarantees non-null input, say you are relying on that contract. In production code, validate at the appropriate boundary.

## Step 2: State a Trustworthy Baseline

A baseline reveals understanding and provides a correctness reference. For two-sum, checking every pair is `O(n^2)` time and `O(1)` auxiliary space. Then improve it based on input properties:

- sorted input → two pointers;
- unsorted input, extra memory allowed → hash map;
- mutation allowed, original indexes irrelevant → sort and scan.

The “best” approach is conditional, not universal.

## Step 3: Name the Invariant Before the Loop

Useful array invariants include:

- `[0, write)` contains the compacted valid values;
- everything outside `[left, right]` has already been eliminated;
- `windowSum` equals the current half-open window;
- `prefix[i]` summarizes `[0, i)`;
- `[0, low)` and `(high, n)` are finalized partitions;
- unvisited matrix cells lie inside four shrinking boundaries.

An invariant gives you a debugging question: “Which statement stopped being true?”

## Step 4: Separate Ownership from Mechanics

Java arrays are mutable reference types. A locally correct algorithm may still violate its caller's expectations.

```java
static int[] sortedCopy(int[] input) {
    int[] copy = input.clone();
    java.util.Arrays.sort(copy);
    return copy;
}
```

For `int[][]`, `clone()` copies only the outer array. Clone each row for an independent rectangular or jagged copy. If in-place mutation is essential for the space target, expose it in the method name or documentation and discuss the trade-off.

## Step 5: Choose Numeric Types Deliberately

If `n` values can each be near `10^9`, a sum can exceed `int`. Write promotion before multiplication:

```java
long area = (long) height * width;
```

This is still bounded by `long`. For comparator order, use `Integer.compare(first, second)` instead of `first - second`.

## Step 6: Test by Failure Category

| Category | Representative cases |
|---|---|
| Size | empty, one element, two elements |
| Position | answer at start, middle, end, entire array |
| Values | zero, negative, duplicate, min/max integer |
| Shape | sorted, reverse-sorted, all equal, alternating |
| Range | invalid `k`, boundary endpoints, no valid answer |
| Matrix | empty, one row, one column, square, jagged |
| Ownership | verify whether input changed |

Randomized differential tests are powerful: compare an optimized method with a small brute-force method over thousands of tiny arrays.

## Step 7: Explain Complexity Precisely

Say what `n` means and distinguish:

- time complexity;
- auxiliary space;
- output space;
- mutation of input;
- preprocessing versus per-query cost;
- average versus worst case for hash-based structures.

“O(1) space” is false if you cloned an `n`-element input. Returning `k` results necessarily uses `O(k)` output space even when auxiliary space is constant.

## Weak and Strong Interview Communication

**Weak:** “I will use sliding window because this is a subarray.”

**Strong:** “The values are positive. Expanding right cannot decrease the sum, and after reaching the threshold, moving left is the only way to shorten the valid range. That monotonicity permits a linear variable-size window. If negatives were allowed, I would reconsider.”

**Weak:** “This is in place.”

**Strong:** “The algorithm uses constant auxiliary state but mutates the input. If caller ownership requires preservation, I will clone it, making auxiliary space linear.”

## Java Choices That Matter

- Prefer `int[]` over `List<Integer>` for dense primitive data when resizing is unnecessary; it avoids boxing and makes index semantics explicit.
- Prefer an enhanced `for` loop for read-only value traversal, but an index loop when positions, neighbors, or mutation matter.
- Use `Arrays.equals` for one-dimensional content equality and `Arrays.deepEquals` for nested content.
- Use `ArrayDeque<Integer>` for index stacks or queues; never assume its boxed values are free.
- Keep helper methods cohesive. Excess abstraction can hide the invariant during a timed interview.
- Avoid shared static mutable arrays. Local state is easier to reason about and naturally safe across concurrent calls.

## Forty-Second Recognition Checklist

1. Is continuity required?
2. Is the array sorted or may I sort it?
3. Do values map to bounded indexes?
4. Are all values positive, enabling monotonic windows?
5. Are there many range queries or offline updates?
6. Does the answer depend on nearest greater/smaller boundaries?
7. Is mutation permitted?
8. What can overflow?
9. What is my invariant?
10. Which edge case is most likely to break this approach?

## Mandatory Traps to Recognize

| Trap | Correct mental model |
|---|---|
| `copy = original` | Copies a reference; both names reach the same array |
| `matrix.clone()` | Copies only the outer reference array |
| `array1 == array2` | Compares identities, not contents |
| `System.out.println(array)` | Does not print elements meaningfully; use `Arrays.toString` |
| `Arrays.asList(intArray)` | Produces a one-element list containing the whole `int[]` |
| `long sum = a * b` | `int` multiplication can overflow before assignment |
| `Math.abs(Integer.MIN_VALUE)` | Remains negative because positive counterpart is unrepresentable |
| `Arrays.binarySearch` on unsorted input | Result is not meaningful under the API contract |
| `PriorityQueue` iteration | Not sorted-order traversal |
| comparator subtraction | Can overflow and violate ordering |

## Readiness Gate

You are ready to move beyond this volume when you can:

- derive half-open boundaries without guessing;
- trace aliasing and shallow copying;
- implement and justify two pointers, fixed and variable windows, Kadane, prefix sums, and interval merging;
- identify when a popular pattern does **not** apply;
- state mutation and space trade-offs honestly;
- use Java arrays and utility APIs without equality, printing, bounds, or overflow traps;
- communicate an invariant, proof idea, and test matrix while coding.

The practice lab and solutions that follow are deliberately separated. Attempt each exercise, record your invariant and complexity, then compare reasoning—not only final code.
