# Loop Execution from Zero

Loops are controlled repetition. That definition is simple; reliable loop code is not. A correct loop must answer four questions:

1. **State:** what values describe the work completed so far?
2. **Condition:** when is another iteration allowed?
3. **Progress:** what changes so the loop moves toward completion?
4. **Exit meaning:** what is guaranteed when the loop stops?

This chapter begins with Java syntax and then opens the syntax so you can see the execution order underneath it. Do not memorize advanced patterns yet. First make one counter completely predictable.

## 1.1 The first complete loop

```java
for (int index = 0; index < 4; index++) {
    System.out.println(index);
}
```

**Expected output**

```text
0
1
2
3
```

The loop has four parts:

| Part | Code | Meaning |
|---|---|---|
| initialization | `int index = 0` | create the loop variable once |
| condition | `index < 4` | enter only while this is true |
| body | `println(index)` | perform one unit of work |
| update | `index++` | move toward termination |

The exact execution order is initialization, condition, body, update, condition again. Initialization does **not** repeat. The final failed condition is still evaluated.

![The Java for-loop lifecycle, including continue and exit behavior](series/volumes/05-loop-mastery-and-index-calculations/assets/01-loop-execution-lifecycle.png)

### Dry run

| Event | `index` | `index < 4` | Action |
|---|---:|---|---|
| initialize | 0 | not tested yet | create `index` |
| test 1 | 0 | true | print 0, then update |
| test 2 | 1 | true | print 1, then update |
| test 3 | 2 | true | print 2, then update |
| test 4 | 3 | true | print 3, then update |
| test 5 | 4 | false | exit |

The body runs four times, but the condition runs five times. This distinction matters when the condition is expensive or has side effects. Interview code should normally keep conditions side-effect free.

## 1.2 `for` is structured `while`

The first loop can be written as:

```java
int index = 0;
while (index < 4) {
    System.out.println(index);
    index++;
}
```

Both versions print the same values. Use `for` when initialization, condition, and update naturally describe a traversal. Use `while` when continuation depends on state discovered inside the body, such as reading until end-of-input or moving pointers by different amounts.

### Smallest correct `while`

```java
int remaining = 3;
while (remaining > 0) {
    System.out.println("work");
    remaining--;
}
```

The progress measure is `remaining`. It begins nonnegative and strictly decreases. Once it reaches zero, the condition is false. Naming this measure is a lightweight termination proof.

### Common infinite-loop failure

```java
int remaining = 3;
while (remaining > 0) {
    System.out.println("work");
    // BUG: remaining never changes
}
```

The condition remains true forever. In a `while` loop, inspect every branch: it must return, throw, break, or change state toward exit.

## 1.3 `do-while`: body first, condition second

```java
int attempts = 0;
do {
    attempts++;
} while (attempts < 3);
System.out.println(attempts);
```

**Output:** `3`

A `do-while` executes its body at least once because the first condition check happens after the body. It is useful for menus, retry flows, and parsers that must consume one item before deciding whether more work remains.

```java
int value = 10;
do {
    System.out.println(value);
} while (value < 0);
```

This prints `10` once. The equivalent `while (value < 0)` would print nothing.

## 1.4 Forward, reverse, and step traversals

### Forward over every array element

```java
int[] values = {8, 3, 5};
for (int index = 0; index < values.length; index++) {
    System.out.println(values[index]);
}
```

Valid indexes are `0`, `1`, and `2`. `values.length` is 3, so the condition must be `index < values.length`, not `<=`.

### Reverse over every element

```java
for (int index = values.length - 1; index >= 0; index--) {
    System.out.println(values[index]);
}
```

For an empty array, `values.length - 1` is `-1`; `-1 >= 0` is false, so the body is skipped safely.

### Visit every second element

```java
for (int index = 0; index < values.length; index += 2) {
    System.out.println(values[index]);
}
```

Do not write `index++` and place another `index++` inside the body. Keeping the movement in the update clause makes the traversal easier to audit.

## 1.5 Enhanced `for`: values without indexes

```java
int sum = 0;
for (int value : values) {
    sum += value;
}
```

Use enhanced `for` when you need each value and do not need its position, a neighboring element, or in-place replacement.

![How enhanced-for is implemented for arrays and Iterable values](series/volumes/05-loop-mastery-and-index-calculations/assets/10-enhanced-for-desugaring.png)

Conceptually, Java treats the array form like an index loop with a hidden index:

```java
for (int hidden = 0; hidden < values.length; hidden++) {
    int value = values[hidden];
    sum += value;
}
```

For an `Iterable`, Java obtains an `Iterator`, calls `hasNext()`, and then `next()`. This is a language-level explanation of observable behavior; an implementation may optimize generated machine code while preserving the same result.

### Why assigning the loop variable does not replace an array element

```java
int[] numbers = {1, 2, 3};
for (int number : numbers) {
    number *= 10;
}
System.out.println(java.util.Arrays.toString(numbers));
```

**Output:** `[1, 2, 3]`

Each primitive value is copied into `number`. Changing the copy does not change the array slot. Use an index when replacing elements:

```java
for (int index = 0; index < numbers.length; index++) {
    numbers[index] *= 10;
}
```

For objects, the enhanced-for variable receives a copy of a reference. Mutating the referred object is visible; assigning the loop variable to a different object does not replace the collection entry. Java remains pass-by-value.

## 1.6 `break`, `continue`, and `return`

These statements change normal flow. Their meanings are precise:

- `break` exits the nearest loop or switch.
- `continue` skips the rest of the current iteration.
- `return` exits the entire method, optionally with a value.

### `continue` in a `for`

```java
for (int index = 0; index < 5; index++) {
    if (index == 2) {
        continue;
    }
    System.out.print(index + " ");
}
```

**Output:** `0 1 3 4 `

The `for` update still runs after `continue`. In a `while`, `continue` jumps directly to the condition, so an update placed later in the body may be skipped:

```java
int index = 0;
while (index < 5) {
    if (index == 2) {
        continue; // infinite: index remains 2
    }
    index++;
}
```

Move progress before the possible `continue`, or restructure the condition.

### Early return expresses success or failure cleanly

```java
static boolean contains(int[] values, int target) {
    for (int value : values) {
        if (value == target) {
            return true;
        }
    }
    return false;
}
```

After the loop, every element has been checked and none matched. That is the exit meaning.

### Labeled control flow

```java
outer:
for (int row = 0; row < matrix.length; row++) {
    for (int col = 0; col < matrix[row].length; col++) {
        if (matrix[row][col] == target) {
            break outer;
        }
    }
}
```

Labeled `break` can exit nested loops. Use it sparingly; a helper method with early `return` often communicates intent better. A labeled `continue` starts the next iteration of the labeled loop.

## 1.7 Scope and the lifetime of loop variables

```java
for (int index = 0; index < 3; index++) {
    int doubled = index * 2;
}
// index and doubled are not in scope here
```

The `index` declared in the `for` initialization exists only in the loop statement and body. A variable declared in the body is recreated for each iteration at the Java-language level. The runtime may reuse storage, but your program cannot access a previous iteration's local variable after its scope ends.

If the final index is genuinely needed, declare it outside:

```java
int index = 0;
while (index < values.length && values[index] < target) {
    index++;
}
// index is the first unprocessed position
```

## 1.8 How array access is protected

Every Java array knows its length. An access such as `values[index]` must behave as follows:

1. evaluate the array reference;
2. throw `NullPointerException` if it is `null`;
3. evaluate the index;
4. verify `0 <= index < values.length`;
5. read or write the element, or throw `ArrayIndexOutOfBoundsException`.

The JVM can eliminate repeated bounds checks when it proves them redundant, but Java never permits an out-of-range access. Write obvious bounds; do not try to outsmart the optimizer.

## 1.9 Nested loops from first principles

```java
for (int row = 0; row < 2; row++) {
    for (int col = 0; col < 3; col++) {
        System.out.println(row + "," + col);
    }
}
```

For each one of 2 outer iterations, the inner loop completes 3 iterations: 6 visits. The inner `col` is initialized to zero again for every row. This is `2 * 3` work, and with an `r` by `c` grid it is `O(r * c)`.

Do not conclude that every nested loop is quadratic. Later chapters show nested syntax with only linear aggregate movement.

## 1.10 Choosing the loop form

| Situation | Natural choice | Reason |
|---|---|---|
| known index range | `for` | bounds and update stay together |
| value-only traversal | enhanced `for` | no manual index |
| continue until state changes | `while` | movement may depend on the body |
| body must run once | `do-while` | condition follows first body |
| two-dimensional positions | nested `for` | each dimension has explicit bounds |

Loop choice is about making state and progress visible, not saving keystrokes.

## 1.11 Beginner mistakes to eliminate now

1. Using `<= values.length` instead of `< values.length`.
2. Starting reverse traversal at `values.length` instead of `values.length - 1`.
3. Forgetting progress in a `while` branch.
4. Expecting enhanced-for assignment to replace an element.
5. Updating the same pointer in two distant places.
6. Hiding side effects in the condition.
7. Assuming `break` exits every nested loop.
8. Treating a null array like an empty array.
9. Using an index after its intended range changed.
10. Measuring only the body and forgetting condition evaluations or iterator work.

## 1.12 Interview checkpoint

Before moving on, you should be able to answer:

- In what order are a `for` loop's four parts executed?
- Why can `continue` create an infinite `while` loop?
- Why does `for (int value : array) value++` not mutate the array?
- What is the exact valid index range for an array of length `n`?
- What guarantees termination in a loop you just wrote?

The next chapter turns these answers into a reusable language of ranges, invariants, and search boundaries.
