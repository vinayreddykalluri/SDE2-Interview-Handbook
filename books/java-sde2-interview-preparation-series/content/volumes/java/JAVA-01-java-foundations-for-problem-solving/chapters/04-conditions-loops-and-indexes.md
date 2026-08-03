# Conditions, Loops, and Index Boundaries

Control flow decides which statements run and how often. The syntax is small; the real skill is expressing a boundary that is easy to verify.

## Conditions start with a boolean question

```java
if (temperature < 0) {
    System.out.println("freezing");
} else if (temperature < 20) {
    System.out.println("cool");
} else {
    System.out.println("warm");
}
```

Java does not treat arbitrary integers or references as truthy. The condition must be `boolean`, or a `Boolean` that can be unboxed safely.

Use a guard clause when it makes the normal path flatter:

```java
static int firstValue(int[] values) {
    if (values == null || values.length == 0) {
        throw new IllegalArgumentException("values must not be empty");
    }
    return values[0];
}
```

## Traditional switch and fall-through

```java
static String dayType(int day) {
    String result;
    switch (day) {
        case 6:
        case 7:
            result = "weekend";
            break;
        default:
            result = "weekday";
    }
    return result;
}
```

Colon-style cases can fall through. Sometimes that combines labels intentionally; missing `break` accidentally is a common interview trap.

Java 14 made switch expressions permanent:

```java
static String dayTypeModern(int day) {
    return switch (day) {
        case 6, 7 -> "weekend";
        default -> "weekday";
    };
}
```

Label version-specific syntax when the interview target is older than Java 14.

## Four loop forms

Use a `for` loop when initialization, continuation, and update naturally belong together:

```java
for (int index = 0; index < values.length; index++) {
    System.out.println(values[index]);
}
```

Use `while` when continuation is the central idea:

```java
while (node != null) {
    node = node.next;
}
```

Use `do-while` only when the body must run at least once. Use enhanced `for` when each value is needed and no index or structural mutation is required.

## Index ranges should be spoken aloud

For an array of length `n`, valid indexes are `[0, n)`. That half-open interval gives two useful facts:

- zero is the first valid index;
- `n` is the number of elements and the first invalid index.

Forward traversal:

```java
for (int index = 0; index < values.length; index++) {
    consume(values[index]);
}
```

Reverse traversal:

```java
for (int index = values.length - 1; index >= 0; index--) {
    consume(values[index]);
}
```

For an empty array, reverse traversal starts at `-1` and performs no iteration.

## Loop invariant and progress

An invariant is a statement that remains true before and after every completed iteration. Progress explains why the loop ends.

For a running sum:

```java
int sum = 0;
for (int index = 0; index < values.length; index++) {
    sum += values[index];
}
```

Invariant: before each iteration, `sum` equals the total of `values[0..index)`.

Progress: `index` increases by one and is bounded by `values.length`.

## `break`, `continue`, and `return`

- `break` exits the nearest loop or switch statement.
- `continue` skips to the next loop iteration.
- `return` exits the method.

```java
static int firstNegative(int[] values) {
    for (int index = 0; index < values.length; index++) {
        if (values[index] < 0) {
            return index;
        }
    }
    return -1;
}
```

Labeled break and continue exist, but extracting a named helper is often clearer than deeply nested labels.

## Two-dimensional traversal

Do not assume every row has the same length:

```java
for (int row = 0; row < matrix.length; row++) {
    if (matrix[row] == null) {
        continue;
    }
    for (int column = 0; column < matrix[row].length; column++) {
        System.out.println(matrix[row][column]);
    }
}
```

This works for rectangular and jagged arrays and deliberately skips null rows.

## Complete example

File: `ControlFlowExample.java`

```java
public final class ControlFlowExample {
    static int firstAtLeast(int[] values, int threshold) {
        for (int index = 0; index < values.length; index++) {
            if (values[index] >= threshold) {
                return index;
            }
        }
        return -1;
    }

    public static void main(String[] args) {
        int[] values = {3, 5, 8, 13};
        System.out.println(firstAtLeast(values, 8));
        System.out.println(firstAtLeast(values, 20));
    }
}
```

Expected output:

```text
2
-1
```

## Edge-case matrix

| Case | Typical bug | Reliable check |
|---|---|---|
| empty input | assumes index zero exists | loop should perform zero iterations |
| last element | uses `index < length - 1` | use the stated half-open range |
| reverse loop | uses `index > 0` | include index zero with `>= 0` |
| `continue` | skips a required update | keep progress in the loop update or prove every path |
| nested rows | uses `matrix[0].length` for every row | inspect the current row |
| colon switch | missing `break` | label intentional fall-through |
| search miss | returns a valid-looking index | choose and document a sentinel or richer result |

## Interview room

**Interviewer:** How do you prove an index loop is correct?

**Model answer:** I state the valid range, an invariant describing processed and unprocessed elements, and a progress measure. For forward array traversal, indexes in `[0, index)` are processed, `index` increases by one, and the loop stops when `index == length`, so every valid index is visited once.

**Follow-up:** When would you use an enhanced `for` loop?

**Model answer:** When I need each element and do not need its index or structural mutation. For two-pointer movement, index replacement, or position reporting, I use an indexed loop.

## Practice

1. **Foundation:** Print an array forward and backward.
2. **Foundation:** Sum only positive values using `continue`.
3. **Interview Core:** Traverse a jagged matrix that can contain null rows.
4. **Debugging:** Repair `for (int i = 0; i <= values.length; i++)`.
5. **SDE-2 Follow-up:** Write the invariant and termination proof for a loop that consumes paginated results.

## Chapter takeaway

Treat every loop as a range plus an invariant plus progress. This is more dependable than remembering isolated patterns, and it prepares you for complexity analysis in the next book.
