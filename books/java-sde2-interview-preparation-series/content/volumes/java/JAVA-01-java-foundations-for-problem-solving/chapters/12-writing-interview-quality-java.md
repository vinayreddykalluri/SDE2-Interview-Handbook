# Writing Interview-Quality Java

An interview solution is evaluated as code and as reasoning. The strongest answer is usually not the shortest one. It makes its contract, invariant, failure behavior, complexity, and tests easy to inspect.

## Begin with a contract

Before typing the loop, clarify:

- Can the input be null or empty?
- Are values sorted, unique, non-negative, or bounded?
- May the method mutate the input?
- What should happen when no answer exists?
- Can arithmetic exceed `int`?
- Is any output order required?

Do not invent expensive defensive behavior for constraints the interviewer has already guaranteed. State the assumption, then code to it.

## Choose types from the largest intermediate value

The input element type does not determine the safe accumulator type:

```java
static long calculateSum(int[] numbers) {
    long sum = 0L;
    for (int number : numbers) {
        sum += number;
    }
    return sum;
}
```

Widen before an operation that can overflow:

```java
long area = (long) width * height;
```

A cast after the multiplication is too late. For an exact-overflow contract, use `Math.addExact` or `Math.multiplyExact` and explain the thrown failure.

## Make the control flow readable

Guard clauses keep the main path visible:

```java
static int firstPositive(int[] numbers) {
    if (numbers == null) {
        throw new IllegalArgumentException("numbers must not be null");
    }
    for (int number : numbers) {
        if (number > 0) {
            return number;
        }
    }
    return -1;
}
```

Returning `-1` is safe here only if the contract says it means “not found.” Sometimes an empty optional, result object, or exception is clearer. Pick one contract rather than mixing signals.

## Name the meaning, not the syntax

Weak:

```java
int f(int[] a) {
    int x = 0;
    for (int i = 0; i < a.length; i++) {
        x += a[i];
    }
    return x;
}
```

Improved:

```java
long calculateSum(int[] numbers) {
    long sum = 0L;
    for (int number : numbers) {
        sum += number;
    }
    return sum;
}
```

`i` and `j` are conventional for short index loops. `left`, `right`, `mid`, `row`, and `column` are also meaningful domain names. Avoid long names that merely repeat the type.

## Use helper methods when they expose an idea

A helper is useful when it:

- names a reusable predicate or transition;
- isolates parsing or validation;
- removes duplicated logic;
- makes the main algorithm read like its explanation;
- is independently testable.

Do not create a class hierarchy or ten one-line methods to make a small interview solution look “enterprise.” Appropriate abstraction is proportional to the problem.

## Mutability and ownership

If sorting or overwriting the input simplifies the solution, ask whether mutation is allowed. When it is not, copy deliberately:

```java
int[] sorted = Arrays.copyOf(numbers, numbers.length);
Arrays.sort(sorted);
```

Copying changes space complexity. Say so. A shallow copy isolates the container but not mutable referenced elements.

## Standard libraries are tools, not substitutes for reasoning

Use a library operation when you know its contract and can explain why it fits. Interviewers may ask you to implement a structure manually to test mechanics, but production-quality code usually favors tested standard libraries.

Useful examples:

- `HashMap` for lookup state, with a stated average-case expectation rather than a universal guarantee;
- `ArrayDeque` for queue or stack behavior;
- `PriorityQueue` when only the next best element must be exposed;
- `Arrays.sort` when sorting is allowed and its cost fits;
- `Integer.compare` instead of subtraction in a comparator.

## State the invariant while coding

For a one-pass lookup solution, a useful invariant might be:

> Before processing index `i`, the map contains each value from indexes `[0, i)` paired with the index chosen by the contract.

The invariant answers three questions:

1. What does the state mean?
2. Why does the next operation preserve it?
3. Why does termination produce a correct result?

This is more persuasive than saying “the hash map makes it fast.”

## A complete refactoring: two sum

Suppose the contract is:

- input is non-null;
- return the first pair discovered while scanning left to right;
- the two indexes must be different;
- return an empty array when no pair exists;
- values and target are `int`, so calculate the complement in `long` before deciding whether it is representable.

File: `InterviewQualityExample.java`

```java
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

public final class InterviewQualityExample {
    static int[] findTwoSumIndices(int[] numbers, int target) {
        if (numbers == null) {
            throw new IllegalArgumentException("numbers must not be null");
        }

        Map<Integer, Integer> earliestIndex = new HashMap<>();
        for (int index = 0; index < numbers.length; index++) {
            long complement = (long) target - numbers[index];
            if (complement >= Integer.MIN_VALUE && complement <= Integer.MAX_VALUE) {
                Integer earlier = earliestIndex.get((int) complement);
                if (earlier != null) {
                    return new int[] {earlier, index};
                }
            }
            earliestIndex.putIfAbsent(numbers[index], index);
        }
        return new int[0];
    }

    public static void main(String[] args) {
        System.out.println(Arrays.toString(
                findTwoSumIndices(new int[] {3, 2, 4}, 6)));
        System.out.println(Arrays.toString(
                findTwoSumIndices(new int[] {3, 3}, 6)));
        System.out.println(Arrays.toString(
                findTwoSumIndices(new int[] {1, 2}, 9)));
    }
}
```

Expected output:

```text
[1, 2]
[0, 1]
[]
```

### Dry run

For `[3, 2, 4]` and target `6`:

| index | value | needed | map before check | action |
|---:|---:|---:|---|---|
| 0 | 3 | 3 | `{}` | store `3 -> 0` |
| 1 | 2 | 4 | `{3=0}` | store `2 -> 1` |
| 2 | 4 | 2 | `{3=0, 2=1}` | return `[1, 2]` |

The lookup happens before insertion, so one array element cannot pair with itself. `putIfAbsent` preserves the earliest index for duplicate values. The `long` subtraction avoids a wrapped complement.

### Complexity statement

The method performs one scan, so expected time is `O(n)` with a well-behaved hash map. Extra space is `O(n)` in the worst case for stored entries. Java does not promise every `HashMap` operation is universally `O(1)` under every key distribution and implementation path.

## Test a small but discriminating set

Do not stop at the happy path. For the contract above, test:

1. ordinary solution in the middle;
2. duplicate values forming a pair;
3. no solution;
4. one or zero elements;
5. negative values;
6. integer-boundary values;
7. null, if the method owns that boundary.

A useful test earns its place by separating a correct implementation from a plausible bug.

## Communicate while coding

A concise interview narration:

1. Restate the contract and assumptions.
2. Give the simple baseline.
3. Identify the bottleneck.
4. State the optimized representation and invariant.
5. Implement in small compiling steps.
6. Dry-run a representative and an edge case.
7. State time and space costs, including mutation or copying.
8. Name one trade-off or alternative.

Do not recite a memorized speech while the code contradicts it. Let variable names and state transitions match the explanation.

## Common review comments

| Review finding | Why it matters | Repair |
|---|---|---|
| accumulator is `int` | intermediate overflow | choose type from bounds |
| comparator subtracts | overflow can reverse order | use `Integer.compare` or comparator builders |
| helper returns null silently | ambiguous missing/error state | define an explicit contract |
| input sorted in place | caller data changed | ask permission or copy |
| map iteration assumed ordered | result can vary | use an ordered structure or sort output |
| nested conditions dominate | hard to prove main path | introduce guard clauses |
| one giant method | state has no names | extract meaningful transitions |
| many decorative classes | obscures the algorithm | keep abstraction proportional |
| complexity says only `O(n)` | hides space/assumptions | state both resources and operation model |

## Interview room

**Interviewer:** Why did you look up the complement before inserting the current value?

**Model answer:** The contract requires two distinct indexes. Checking the state from earlier indexes first enforces that directly. After the check, inserting the current value restores the invariant for the next iteration.

**Follow-up:** Why did you compute the complement as `long`?

**Model answer:** Subtracting two int values can overflow before a map lookup. Computing in long preserves the mathematical result. I only cast back after verifying that the complement is representable as an int key.

## Practice

1. **Foundation:** Refactor a sum method with clear names and a safe accumulator.
2. **Predict:** Trace the two-sum state for `[3, 3]` and target `6`.
3. **Debugging:** Fix a solution that inserts before lookup and reuses one index.
4. **Debugging:** Replace a subtraction comparator and test integer extremes.
5. **Interview Core:** Add an explicit contract for null and missing-result behavior.
6. **Interview Core:** Produce a mutation-free solution and account for its copied storage.
7. **SDE-2 Follow-up:** Defend the hash-map complexity statement without claiming a hard constant-time guarantee.

## Readiness check

You are ready for the Time and Space Complexity volume when you can:

- predict type promotion and overflow before running code;
- distinguish a primitive value, a reference value, and an object;
- explain Java pass-by-value using mutation and reassignment;
- traverse arrays and strings without off-by-one errors;
- choose a basic list, set, map, deque, or priority queue from required semantics;
- define method contracts and failure behavior;
- dry-run state against an invariant;
- state time, extra space, mutation, and important assumptions.

## Chapter takeaway

Interview-quality Java makes reasoning visible. Clarify the contract, choose safe types, name the state, preserve an invariant, test discriminating cases, and state the real resource trade-offs.
