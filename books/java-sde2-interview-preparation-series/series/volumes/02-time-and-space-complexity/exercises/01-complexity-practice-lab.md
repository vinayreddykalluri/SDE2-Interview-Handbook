# Complexity Practice Lab

## How to use this lab

Attempt each item before reading the solutions chapter. For every analysis answer, write:

1. input dimensions;
2. time bound and case;
3. auxiliary/output space;
4. assumptions about APIs or data;
5. one sentence of proof.

The difficulty labels describe the reasoning expected, not the amount of code.

## A. Knowledge check

1. **Foundation:** What does Big-O describe, and what does it not describe?
2. **Foundation:** Why must two independent array lengths be named `n` and `m`?
3. **Foundation:** Distinguish input space, auxiliary space, and output space.
4. **Foundation:** Why are two consecutive O(n) loops still O(n)?
5. **Foundation:** What code shape commonly creates O(log n) work?
6. **Foundation:** Why is array indexing O(1)?
7. **Foundation:** When does a pair-enumeration loop become O(n squared)?
8. **Foundation:** What does “worst case” mean for linear search?
9. **Foundation:** Why should constants be dropped only after deriving a count?
10. **Interview Core:** Distinguish expected and amortized complexity.
11. **Interview Core:** What makes a complexity bound output-sensitive?
12. **Interview Core:** Why can a nested-looking two-pointer loop be O(n)?
13. **Interview Core:** Why is recursive Fibonacci exponential time but linear stack space?
14. **Interview Core:** What is the most accurate stack-space dimension for tree DFS?
15. **Interview Core:** Why is `HashMap.get` not guaranteed O(1) in every circumstance?
16. **Interview Core:** Why can LinkedList insertion still require O(n) total time?
17. **Interview Core:** What makes ArrayList append amortized O(1)?
18. **Interview Core:** Why is PriorityQueue iteration not a sorted traversal?
19. **SDE-2 Follow-up:** When is an O(n log n) solution preferable to expected O(n)?
20. **SDE-2 Follow-up:** Why can the same asymptotic complexity have very different production behavior?

## B. Predict the count and derive the bound

For each snippet, give an exact body count when practical and then simplify.

### B1. Fixed access

```java
int answer = values[0] + values[values.length - 1];
```

### B2. Sequential phases

```java
for (int value : first) consume(value);
for (int value : second) consume(value);
```

### B3. Square grid

```java
for (int row = 0; row < n; row++)
    for (int column = 0; column < n; column++)
        visit(row, column);
```

### B4. Triangular pairs

```java
for (int left = 0; left < n; left++)
    for (int right = left + 1; right < n; right++)
        compare(left, right);
```

### B5. Halving

```java
for (int value = n; value > 1; value /= 2) work();
```

### B6. Doubling plus full scan

```java
for (int size = 1; size < n; size *= 2)
    for (int index = 0; index < n; index++)
        work();
```

### B7. Geometric inner work

```java
for (int size = 1; size <= n; size *= 2)
    for (int index = 0; index < size; index++)
        work();
```

### B8. Forward-only pointers

```java
int left = 0;
for (int right = 0; right < n; right++) {
    while (left < right && tooLarge(left, right)) left++;
}
```

Assume `tooLarge` is O(1).

### B9. ArrayList membership

```java
for (int value : values) {
    if (list.contains(value)) matches++;
}
```

Both `values` and `list` contain `n` elements and `list` is an ArrayList.

### B10. HashSet membership

Repeat B9 when `list` is replaced by a soundly hashed HashSet.

### B11. Sorting and scanning

```java
Arrays.sort(values);
for (int value : values) consume(value);
```

State the ordinary primitive-array interview model and qualify it.

### B12. Repeated concatenation

```java
String result = "";
for (char character : characters) result += character;
```

Let the final string length be `n`.

### B13. Jagged matrix

```java
for (int[] row : matrix)
    for (int value : row)
        consume(value);
```

Use `r` rows and `c_i` for each row's length.

### B14. Recursive countdown

```java
static void countDown(int n) {
    if (n == 0) return;
    countDown(n - 1);
}
```

### B15. Binary recursion

```java
static void enumerate(int remaining) {
    if (remaining == 0) return;
    enumerate(remaining - 1);
    enumerate(remaining - 1);
}
```

### B16. Output matches

```java
List<Integer> result = new ArrayList<>();
for (int value : values)
    if (accept(value)) result.add(value);
```

Assume `accept` is O(1), input length `n`, and `k` matches.

### B17. Queue drain

```java
while (!queue.isEmpty()) consume(queue.removeFirst());
```

The queue is an ArrayDeque initially containing `n` entries.

### B18. Heap drain

```java
while (!heap.isEmpty()) consume(heap.remove());
```

The PriorityQueue initially contains `n` entries.

### B19. Tree range

```java
for (int value : treeSet.subSet(low, true, high, true)) consume(value);
```

Let `k` elements lie in the range.

### B20. Many test cases

Test case `i` contains `n_i` values and is scanned once. Express total work for `t` test cases without assuming every case has the same size.

## C. Debug the analysis

For each claim, explain the error and write a corrected claim.

1. **Foundation:** “This method has three loops, so it is O(n cubed).” The loops are consecutive.
2. **Foundation:** “Array access is O(n) because the array has n elements.”
3. **Foundation:** “The loop always returns early, so worst-case time is O(1).”
4. **Foundation:** “O(n) means exactly n machine instructions.”
5. **Foundation:** “The two arrays have total complexity O(n)” even though lengths are independent.
6. **Interview Core:** “Every nested loop is O(n squared).” Both pointers only move forward.
7. **Interview Core:** “HashMap lookup is guaranteed O(1).”
8. **Interview Core:** “LinkedList insertion is O(1), so insert at index n/2 is O(1).”
9. **Interview Core:** “PriorityQueue iteration prints values in priority order.”
10. **Interview Core:** “A recursive method uses O(1) space because it declares no arrays.”
11. **Interview Core:** “Fibonacci makes O(2^n) calls, so stack space is O(2^n).”
12. **Interview Core:** “`new ArrayList<>(list)` is O(1) because it is one constructor call.”
13. **Interview Core:** “Returning a copied array uses O(1) space if output is excluded,” with no mention of result storage.
14. **SDE-2 Follow-up:** “O(n) is always faster than O(n log n).”
15. **SDE-2 Follow-up:** “A benchmark proves this method has O(n) complexity.”

## D. Small coding and analysis tasks

1. **Foundation:** Write linear search and return both the index and inspection count.
2. **Foundation:** Write a method that visits a rectangular matrix and reports total cells.
3. **Foundation:** Write a repeated-halving method that returns the number of steps.
4. **Interview Core:** Implement duplicate detection using nested loops and using HashSet. Compare contracts.
5. **Interview Core:** Implement a frequency map and state bounds using `n` and `u`.
6. **Interview Core:** Reverse an array in place, then implement a non-mutating version.
7. **Interview Core:** Implement a queue and stack using ArrayDeque with consistent method families.
8. **Interview Core:** Keep the largest `k` integers from a stream using a min-heap of size at most `k`.
9. **Interview Core:** Implement an O(n) two-pointer scan and prove the aggregate movement bound.
10. **Interview Core:** Implement a jagged-matrix sum and express cost using total cells.
11. **SDE-2 Follow-up:** Refactor repeated string concatenation to StringBuilder and define the input dimension as total characters.
12. **SDE-2 Follow-up:** Design a duplicate-check API that preserves caller input and offers deterministic worst-case behavior; discuss trade-offs.

## E. Interview follow-ups

1. Why might you accept O(n log n) sorting instead of expected O(n) hashing?
2. What changes if keys are adversarial or mutable?
3. How would you express graph traversal complexity, and why are both `V` and `E` needed?
4. A two-pointer method has a while loop inside a for loop. Prove or refute O(n squared).
5. How does returning all matches change a method that previously returned only the first match?
6. What is amortization, and does it guarantee every request is cheap?
7. When should a copied input be counted in space complexity?
8. How does recursion depth differ on balanced and skewed trees?
9. What assumptions support an expected-case claim?
10. What would a benchmark reveal that Big-O does not, and what would it fail to prove?
11. How do mutation, memory, and deterministic guarantees influence collection choice?
12. Given an O(n squared) baseline, how would you communicate the path to an O(n log n) or expected O(n) solution?

## F. Cumulative assessments

### Assessment 1: Foundations

Analyze five snippets: fixed access, full scan, repeated halving, two consecutive scans, and pair enumeration. Give one proof sentence each.

### Assessment 2: Java cost awareness

Compare ArrayList, HashSet, TreeSet, ArrayDeque, and PriorityQueue for lookup, order, end operations, and priority removal. Include qualifiers.

### Assessment 3: Space and ownership

Compare an in-place array transformation, a defensive copy, a recursive traversal, and an output list. Separate auxiliary and output space.

### Assessment 4: Hidden work

Find all non-constant work in a method that copies input, sorts it, repeatedly calls `contains` on a list, builds a string, and returns selected values. Rewrite it with clear assumptions.

### Assessment 5: SDE-2 explanation

In five minutes, explain a baseline, identify its bottleneck, propose a better data structure or invariant, prove time/space, and state one trade-off plus one test that could falsify your reasoning.

## Final readiness assessment

You are ready to continue to Number Systems and the DSA pattern books when you can do all of the following without a reference sheet:

- derive, not guess, bounds for common loop shapes;
- distinguish sequential, independent nested, dependent nested, and aggregate pointer movement;
- state best/worst/expected/amortized qualifiers correctly;
- separate input, auxiliary, output, and recursion-stack space;
- choose core Java collections by semantics and qualified cost;
- include sorting, copying, string construction, boxing, and output work;
- explain one optimization from baseline through proof and trade-off;
- avoid using wall-clock timing as proof of Big-O.

If two or more items are weak, revisit the relevant chapter and rerun `ComplexityExamples.java` before advancing.
