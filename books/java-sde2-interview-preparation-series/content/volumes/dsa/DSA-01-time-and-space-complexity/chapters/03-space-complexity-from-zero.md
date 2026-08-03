# Space Complexity from Zero

## Learning objectives

By the end of this chapter, you can separate input, output, and auxiliary space; identify peak live storage; include recursion stacks and hidden copies; and explain when an in-place algorithm is worth mutating its input.

## Start with three different questions

Space analysis becomes confusing when three quantities are mixed together.

1. **Input space:** storage already occupied by the caller's input.
2. **Auxiliary space:** extra storage the algorithm uses while working.
3. **Output space:** storage required for the result that the contract asks the method to produce.

Interviewers usually mean auxiliary space when they ask, “What is the space complexity?” State that convention instead of making them guess.

```java
static int maximum(int[] numbers) {
    if (numbers.length == 0) {
        throw new IllegalArgumentException("numbers must not be empty");
    }

    int best = numbers[0];
    for (int number : numbers) {
        best = Math.max(best, number);
    }
    return best;
}
```

For `n = numbers.length`:

- the input array occupies O(n) input space;
- the method adds only `best`, `number`, and loop state, so auxiliary space is O(1);
- the returned integer is O(1) output space.

Calling the method “O(n) space” without a qualifier hides the useful fact that it does not allocate storage proportional to the input.

## Constant versus growing auxiliary storage

### A fixed number of variables is O(1)

```java
static void reverseInPlace(int[] values) {
    int left = 0;
    int right = values.length - 1;

    while (left < right) {
        int temporary = values[left];
        values[left] = values[right];
        values[right] = temporary;
        left++;
        right--;
    }
}
```

The method uses a fixed number of variables regardless of `n`, so auxiliary space is O(1). The time is O(n). It mutates the caller's array, which is part of the method contract—not a detail to hide behind the word “optimal.”

### A copy grows with the input

```java
static int[] reversedCopy(int[] values) {
    int[] result = new int[values.length];
    for (int index = 0; index < values.length; index++) {
        result[values.length - 1 - index] = values[index];
    }
    return result;
}
```

The returned array contains `n` integers. If output is excluded, auxiliary working space is O(1); if all newly allocated result storage is reported, space is O(n). Say which convention you use. In practice, the output allocation still matters to memory capacity.

## Peak live storage, not total allocations over history

Space complexity describes the maximum storage simultaneously needed during an execution.

```java
static void processInBatches(int[] values, int batchSize) {
    for (int start = 0; start < values.length; start += batchSize) {
        int end = Math.min(values.length, start + batchSize);
        int[] batch = Arrays.copyOfRange(values, start, end);
        consume(batch);
    }
}
```

If each completed `batch` becomes unreachable before the next allocation and `consume` does not retain it, peak additional array storage is O(min(n, `batchSize`)), even though many arrays may be allocated over the full run. Allocation rate can affect garbage collection, but it is different from peak auxiliary space.

If `consume` stores every batch, the retained space can grow to O(n). Ownership and lifetime change the answer.

## Arrays, matrices, and collections

- `new int[n]` adds O(n) elements of storage.
- `new boolean[rows][columns]` adds O(rows times columns) elements for a rectangular matrix.
- A jagged `int[][]` should be described by the sum of row lengths, not automatically O(rows times maximumColumns).
- A map or set holding up to `u` distinct keys uses O(u) logical entries; implementation overhead exists, but exact bytes are JVM- and implementation-dependent.
- `new ArrayList<>(existingList)` copies `n` references: O(n) additional logical slots, not `n` deep copies of the referenced objects.

Avoid claiming exact object sizes unless the environment, JVM options, object layout, and measurement method are specified. Asymptotic interview analysis normally counts logical elements or frames.

## Aliasing can avoid a copy—but changes the contract

```java
static int[] identity(int[] values) {
    return values;
}
```

The returned reference points to the same array; there is no element copy. Auxiliary space is O(1). But callers can now observe shared mutation.

```java
static int[] defensiveCopy(int[] values) {
    return Arrays.copyOf(values, values.length);
}
```

This takes O(n) time and O(n) result storage, but protects the original array from mutation through the returned reference. Space is not merely a score to minimize; it can buy ownership safety.

## Recursion uses call-stack space

Each unfinished recursive call needs a frame holding return state and local information. Exact bytes are JVM-dependent, but the number of simultaneously active calls gives an asymptotic bound.

### Linear recursion depth

```java
static long factorial(int n) {
    if (n < 0) throw new IllegalArgumentException("n must be non-negative");
    if (n <= 1) return 1L;
    return n * factorial(n - 1);
}
```

For `n = 4`, the active chain reaches `factorial(4)`, `factorial(3)`, `factorial(2)`, `factorial(1)`. Depth is O(n), so auxiliary stack space is O(n). Java does not guarantee tail-call optimization, so a tail-recursive rewrite does not justify O(1) stack space.

### Logarithmic recursion depth

```java
static int binarySearch(int[] sorted, int target, int low, int high) {
    if (low > high) return -1;
    int middle = low + (high - low) / 2;
    if (sorted[middle] == target) return middle;
    if (sorted[middle] < target) {
        return binarySearch(sorted, target, middle + 1, high);
    }
    return binarySearch(sorted, target, low, middle - 1);
}
```

The remaining range halves each time. Time is O(log n) and recursive stack space is O(log n). An iterative binary search can preserve O(log n) time with O(1) auxiliary space.

### Branching does not automatically mean exponential stack space

```java
static int fibonacci(int n) {
    if (n <= 1) return n;
    return fibonacci(n - 1) + fibonacci(n - 2);
}
```

This implementation makes exponentially many calls over time, but the deepest active chain is only O(n). Therefore time is O(2^n) as a simple upper bound, while stack space is O(n). Space counts simultaneous frames, not every call ever created.

### Trees use height, not always node count

A depth-first traversal of a tree has O(h) call-stack space, where `h` is height. A balanced tree has `h = O(log n)`; a skewed tree can have `h = O(n)`. Report O(h) first, then discuss shapes.

## Output-sensitive space

```java
static List<Integer> positionsOf(int[] values, int target) {
    List<Integer> positions = new ArrayList<>();
    for (int index = 0; index < values.length; index++) {
        if (values[index] == target) positions.add(index);
    }
    return positions;
}
```

Let `k` be the number of matches. Time is O(n); result space is O(k). The list's capacity policy and boxed `Integer` objects add concrete overhead, but O(k) is the useful asymptotic model. If the contract requires all positions, Omega(k) output space is unavoidable.

## Hidden space in Java code

### Boxing

`List<Integer>` stores references to wrapper objects, not primitive `int` elements. This is still O(n) logical space, but can be materially larger than `int[]`. Mention the representation when memory limits are tight.

### Substrings and conversions

Do not assume `substring`, `toCharArray`, `split`, streams, or collectors are zero-copy views. Model them according to the selected Java version and documented API behavior. In modern Java, a new substring has its own character storage rather than retaining a view of the entire original backing array.

### Sorting

Do not give one universal auxiliary-space claim for `Arrays.sort`. Primitive-array and object-array overloads use different algorithm families. State the overload/type or keep the answer qualified.

### Views versus copies

`list.subList(...)` is a backed view; `new ArrayList<>(list.subList(...))` creates a new list. A view can use little new storage while retaining and sharing a larger structure. Low allocation does not imply independent ownership.

## A repeatable space-analysis method

1. Name input dimensions.
2. Decide whether the question asks for auxiliary, output, or total storage.
3. List allocations whose size grows with input.
4. Count maximum simultaneously active recursion frames.
5. Include hidden library copies, boxing, and retained views where relevant.
6. Express space with the most accurate dimension: O(n), O(rows times columns), O(h), O(k), or a sum.
7. State mutation and ownership trade-offs.

## Dry run: breadth-first traversal frontier

Suppose a queue-based tree traversal processes one level at a time.

| Moment | Queue contents | Live queued nodes |
|---|---|---:|
| start | root | 1 |
| after root | its children | 2 |
| middle level | next frontier | up to width |
| finish | empty | 0 |

Time is O(n) because each node enters and leaves once. Auxiliary space is O(w), where `w` is maximum tree width—not automatically O(n), although O(n) is a valid worst-case upper bound.

## Common mistakes

- Saying every iterative method is O(1) space.
- Ignoring recursion frames.
- Counting every historical allocation instead of peak live storage.
- Treating returned output as free without stating the convention.
- Calling shallow copies deep copies.
- Assuming a reference copy duplicates an object.
- Claiming Java optimizes tail recursion.
- Reporting a tree traversal as O(log n) space without a balance guarantee.
- Calling mutation “better” without discussing caller expectations.

## Quick check

1. What is the difference between input space and auxiliary space?
2. Why does naive recursive Fibonacci use O(n), not O(2^n), stack space?
3. What space dimension describes a tree DFS most accurately?
4. When can a batch-processing loop use less peak space than total allocated bytes?
5. Does copying a list of object references copy the objects?
6. Why might O(n) boxed storage matter more than O(n) primitive storage?

## Practice

1. **Foundation:** Analyze the auxiliary and output space of an array-filter method.
2. **Foundation:** Compare `return values` with `return Arrays.copyOf(values, values.length)`.
3. **Interview Core:** Analyze recursive and iterative binary search space.
4. **Interview Core:** Give time and space bounds for a frequency map with `u` distinct keys.
5. **Interview Core:** Analyze a jagged matrix using total cell count.
6. **SDE-2 Follow-up:** Choose between an in-place sort and a defensive copy when an API promises not to mutate caller data.

## Chapter summary

Report space with a contract: input, auxiliary, and output are different. Count peak live storage, growing allocations, and active recursion depth. Then explain what memory buys—ownership, speed, simpler code, or required output—instead of treating O(1) space as automatically superior.
