# Time Complexity from Zero

## Learning objectives

By the end of this chapter, you can explain what time complexity measures, choose the correct input size, count work in simple Java code, distinguish constant, logarithmic, linear, linearithmic, and quadratic growth, and use Big-O without treating it as elapsed time.

## Why complexity comes after Java basics

Before analyzing code, you must be able to read variables, loops, arrays, methods, and common collections. Complexity asks a second question: as the input grows, how does the amount of work or extra storage grow?

Do not start by memorizing a chart. Start with one concrete input and count what the program actually does.

```java
static int first(int[] numbers) {
    return numbers[0];
}
```

For an array of length 10 or 10 million, the method performs one indexed read. The work does not grow with the array length, so it is constant: O(1).

```java
static boolean contains(int[] numbers, int target) {
    for (int number : numbers) {
        if (number == target) return true;
    }
    return false;
}
```

In the best case the first element matches. In the worst case the target is last or absent and all `n` values are inspected. Worst-case time is O(n).

## Step 1: Name the input size

Complexity is meaningless until its dimensions are named.

- For `int[] numbers`, let `n = numbers.length`.
- For a string, `n` may mean UTF-16 code units unless the contract says code points.
- For a matrix, use `rows` and `columns`, or total cells when that is the true work.
- For two independent arrays, use `n` and `m` instead of pretending they are equal.
- For a graph, use `V` vertices and `E` edges.
- For returned matches, use `k` for output size when output affects work or memory.

Example with two independent inputs:

```java
static int countEqualPairs(int[] left, int[] right) {
    int count = 0;
    for (int first : left) {
        for (int second : right) {
            if (first == second) count++;
        }
    }
    return count;
}
```

If lengths are `n` and `m`, every pair is compared, so time is O(nm), not automatically O(n squared).

## Step 2: Choose a meaningful operation

Count the operation that grows: element visits, comparisons, map lookups, recursive calls, or generated results. Exact statement counts are useful for learning, but Big-O keeps the dominant growth.

```java
static int sum(int[] numbers) {
    int total = 0;                    // 1 initialization
    for (int number : numbers) {      // n visits
        total += number;              // n additions
    }
    return total;                     // 1 return
}
```

A simplified count is `2n + 2`. As `n` grows, the linear part dominates, so time is O(n). Big-O does not claim the method literally takes `n` nanoseconds.

## Step 3: See the common growth shapes

### O(1): constant growth

```java
int middle = numbers[numbers.length / 2];
```

One indexed read. The array still occupies O(n) input space, but this expression uses O(1) time and O(1) auxiliary space.

### O(log n): repeatedly shrink the remaining problem

```java
int value = n;
int steps = 0;
while (value > 1) {
    value /= 2;
    steps++;
}
```

For `n = 16`, values are 16, 8, 4, 2, 1: four divisions. Doubling `n` adds roughly one step. The loop is O(log n). Unless a base matters to the algorithm's meaning, logarithm bases differ only by a constant factor.

### O(n): visit each input item a constant number of times

```java
for (int number : numbers) {
    process(number);
}
```

This is O(n) only if `process` is O(1). A method call is not automatically constant; include the work inside it.

### O(n log n): linear work across logarithmic levels

Comparison sorting is a familiar example:

```java
Arrays.sort(values);
```

For ordinary interview analysis, sorting `n` primitive values is O(n log n) worst-case under the documented Java sorting algorithm family, while exact implementation details and auxiliary space depend on the overload/type. State the API and data type rather than repeating one universal sorting claim.

### O(n squared): compare many pairs

```java
for (int left = 0; left < numbers.length; left++) {
    for (int right = left + 1; right < numbers.length; right++) {
        compare(numbers[left], numbers[right]);
    }
}
```

The comparisons total `(n - 1) + (n - 2) + ... + 1 = n(n - 1)/2`, which grows quadratically: O(n^2).

### O(2^n) and O(n!): enumerate choices or arrangements

Generating every subset can produce `2^n` outputs. Generating every permutation can produce `n!` outputs. If the algorithm must materialize all of them, the output itself already requires that much work. These bounds are not automatically mistakes; they may be unavoidable for the requested output.

## Big-O, Big-Omega, and Big-Theta gently

- **O(g(n))** is an asymptotic upper bound: growth is no faster than a constant multiple of `g(n)` after some point.
- **Omega(g(n))** is a lower bound.
- **Theta(g(n))** gives matching upper and lower growth.

In interviews, "the complexity is O(n)" often informally means a tight worst-case bound. Be precise when the distinction matters. A full scan that always reads all `n` elements is Theta(n). A contains search is O(n) worst-case but can return in Theta(1) best-case.

## Drop constants and lower-order terms only after deriving

- O(3n + 10) becomes O(n).
- O(n^2 + 20n + 100) becomes O(n^2).
- O(n + m) does not become O(n) when `m` is independent.
- O(n log n + n) becomes O(n log n).

Dropping terms is the last step, not the first. First show where the terms came from so an interviewer can verify the reasoning.

## Best, average/expected, and worst cases

Say which case you are reporting.

For linear search:

- best case: target first, O(1);
- worst case: target last/absent, O(n);
- expected case: requires a stated distribution of target positions and presence.

"Average" is not a magic middle answer. Expected analysis needs a probability model. Interview and production capacity decisions usually begin with worst case, then add expected or amortized behavior when relevant.

## A first constraint-to-complexity map

These are heuristics, not promises; constants, language, time limit, and operations matter.

| Approximate input scale | Often worth considering |
|---:|---|
| `n <= 10` | factorial/backtracking may be possible |
| `n <= 20-25` | subset O(2^n) may be possible |
| `n <= 1,000` | O(n^2) may be possible |
| `n <= 100,000` | O(n log n) or O(n) is typical |
| `n` in millions | usually O(n) with careful memory/constants |

Never quote this table without considering the operation cost, number of test cases, memory, and environment.

## Common beginner mistakes

- Counting source lines instead of executed work.
- Calling array access O(n) because the array contains n elements.
- Calling every method O(1) without analyzing its body/API.
- Multiplying loops merely because one appears inside another.
- Replacing two independent sizes with one `n`.
- Reporting the best case without labeling it.
- Saying O(n) means exactly n operations or a fixed duration.
- Ignoring output work.
- Optimizing before a correct baseline exists.

## Quick check

1. What is the input size for a method receiving two arrays of different lengths?
2. Why is `numbers[index]` O(1)?
3. How many times can a repeated-halving loop run?
4. Why does `n(n - 1)/2` become O(n squared)?
5. What probability assumption supports an expected-case claim?

## Practice

1. **Foundation:** Count exact body executions for loops of length 0, 1, 4, and n.
2. **Foundation:** Label five snippets as O(1), O(log n), O(n), O(n log n), or O(n squared).
3. **Interview Core:** Analyze a method that scans arrays of lengths n and m sequentially.
4. **Interview Core:** Explain why generating all subsets needs at least Omega(2^n) output work.
5. **SDE-2 Follow-up:** Given n = 100,000, compare a quadratic baseline with a sorting-based solution and state what evidence is still missing.

## Chapter summary

Complexity starts with a named input size and a count of executed work. Constant access, repeated halving, full scans, sorting-level work, and pair enumeration create recognizable growth shapes, but each claim must follow the code and its contracts. Big-O compares growth; it is not a stopwatch.
