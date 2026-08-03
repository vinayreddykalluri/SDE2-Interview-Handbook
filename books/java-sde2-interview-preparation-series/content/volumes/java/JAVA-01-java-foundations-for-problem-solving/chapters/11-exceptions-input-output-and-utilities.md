# Exceptions, Input, Output, and Everyday Utilities

Interview code still crosses boundaries: text becomes numbers, files or consoles provide input, and library methods can reject invalid state. This chapter starts with a small rule: use exceptions to report an operation that cannot complete its contract; do not use them as a substitute for ordinary conditions.

## A practical exception hierarchy

At the level needed for this book:

```text
Throwable
|- Error
|  `- serious runtime or environment failure
`- Exception
   |- checked exception
   `- RuntimeException
      `- unchecked exception
```

- A **checked exception** must be caught or declared. `IOException` is the common example.
- An **unchecked exception** is a `RuntimeException` subtype. It often reports a broken precondition, invalid state, or programming error.
- An **Error** is not an exception. Code normally does not recover locally from failures such as `OutOfMemoryError` or `StackOverflowError`.

The hierarchy is a classification, not a quality ranking. Checked exceptions are not always better, and unchecked exceptions are not automatically bugs.

## Catch, throw, and declare

`throw` creates or propagates a specific failure:

```java
static int requirePositive(int value) {
    if (value <= 0) {
        throw new IllegalArgumentException("value must be positive");
    }
    return value;
}
```

`throws` declares that a method may let a checked exception cross its boundary:

```java
static String readFirstLine(BufferedReader reader) throws IOException {
    return reader.readLine();
}
```

`try` and `catch` handle a failure when the method has enough context to respond:

```java
static Integer parseOrNull(String text) {
    try {
        return Integer.valueOf(text);
    } catch (NumberFormatException exception) {
        return null;
    }
}
```

Returning `null` is acceptable only when the caller's contract defines it clearly. At many boundaries, returning an explicit result type or throwing a contextual exception is clearer.

## Catch narrowly and preserve the cause

A catch block should know what recovery means. This hides too much:

```java
try {
    processRequest();
} catch (Exception exception) {
    // ignored
}
```

It can suppress a programming defect and leave the caller believing work succeeded. Catch the failure you can handle. If you translate it, retain the original cause:

```java
try {
    return Integer.parseInt(text);
} catch (NumberFormatException exception) {
    throw new IllegalArgumentException("invalid age: " + text, exception);
}
```

Catch blocks run top to bottom. A more specific subtype must appear before its superclass; reversing them is a compile-time error because the specific block becomes unreachable.

## `finally` and its limits

`finally` normally runs as control leaves the `try` or `catch`, whether the path returns or throws. It is useful for cleanup that cannot use try-with-resources.

Do not say that `finally` *always* runs. Abrupt process termination, JVM failure, or a non-terminating operation can prevent it. Also avoid returning from `finally`; that can replace an earlier return value or suppress an exception.

## Try-with-resources owns cleanup

A resource implementing `AutoCloseable` can be declared in the `try` header:

```java
static String firstLine(String source) throws IOException {
    try (BufferedReader reader = new BufferedReader(new StringReader(source))) {
        return reader.readLine();
    }
}
```

Resources close in reverse declaration order. If the body and `close()` both fail, the body failure is primary and close failures are available through `getSuppressed()`.

The ownership question matters: the code that creates a resource usually closes it. A method should not silently close a caller-owned stream unless its contract says so.

## Common interview failures

| Failure | Typical cause | Better prevention or response |
|---|---|---|
| `NullPointerException` | dereferencing `null` or unboxing it | validate the boundary; avoid ambiguous null contracts |
| `ArrayIndexOutOfBoundsException` | index outside `[0, length)` | state and preserve the index invariant |
| `NumberFormatException` | malformed or out-of-range numeric text | validate/translate at the parsing boundary |
| `ArithmeticException` | integer divide by zero or exact-arithmetic overflow | validate divisor; choose an explicit overflow policy |
| `ClassCastException` | runtime object is not the forced subtype | avoid the cast or verify the runtime type |
| `IllegalArgumentException` | caller violates a method precondition | document and check the precondition |
| `ConcurrentModificationException` | unsupported structural mutation during iteration | use iterator removal or a two-phase update |
| `StackOverflowError` | recursion has no reachable base case or is too deep | prove termination; consider iteration |

An exception stack trace is read from the first relevant application frame outward. The top line gives the type and message; the `Caused by` chain often preserves the boundary where the original failure began.

## Input choices

### `Scanner`: convenient and readable

```java
try (Scanner scanner = new Scanner("3 10 20 30")) {
    int size = scanner.nextInt();
    int[] values = new int[size];
    for (int index = 0; index < size; index++) {
        values[index] = scanner.nextInt();
    }
}
```

`Scanner` is convenient but does more parsing work. Be careful when mixing token reads such as `nextInt()` with `nextLine()`: the line separator after the token is still waiting to be consumed.

### `BufferedReader`: explicit parsing and lower overhead

```java
try (BufferedReader reader = new BufferedReader(new StringReader("10 20 30"))) {
    String[] tokens = reader.readLine().trim().split("\\s+");
    int first = Integer.parseInt(tokens[0]);
}
```

For high-volume competitive input, a buffered token parser may avoid repeated string splitting. In interviews, many platforms call your method directly, so do not spend coding time on console input unless asked.

`StringTokenizer` is a lightweight legacy tokenizer still seen in contest code. Know how to read it, but prefer the clearest parser for the environment.

### Output

`System.out.println` is enough for small examples. `PrintWriter` can buffer many writes:

```java
PrintWriter output = new PrintWriter(System.out);
output.println("ready");
output.flush();
```

Do not close `System.in` or `System.out` inside reusable interview methods.

## Utility APIs worth knowing

### `Math`

- `min`, `max`, `abs`, `sqrt`, `floor`, and `ceil` express common numeric operations.
- `addExact`, `subtractExact`, and `multiplyExact` throw `ArithmeticException` instead of silently wrapping.
- `Math.abs(Integer.MIN_VALUE)` is still negative because the positive magnitude is not representable as an `int`. The same rule applies to `Long.MIN_VALUE`.
- `Math.pow` returns `double`; it is not a general exact-integer exponentiation function.

### `Arrays`

```java
int[] values = {3, 1, 2};
Arrays.sort(values);                     // [1, 2, 3]
int position = Arrays.binarySearch(values, 2); // 1
int[] copy = Arrays.copyOf(values, values.length);
```

`binarySearch` requires a compatible sorted order. A negative result encodes an insertion point; it is not merely `-1`.

`Arrays.asList` deserves two warnings:

```java
List<String> words = Arrays.asList("a", "b"); // fixed-size view
List<int[]> oneElement = Arrays.asList(new int[] {1, 2});
```

The first list permits `set` but not structural `add` or `remove`. The primitive array in the second example is one element; it is not boxed into `List<Integer>`.

### `Collections`, `Objects`, wrappers, and `Character`

- `Collections.sort`, `reverse`, `min`, `max`, `frequency`, and `binarySearch` work with lists and their stated preconditions.
- `List.of`, `Set.of`, and `Map.of` create unmodifiable collections and reject null. Unmodifiable does not make mutable elements immutable.
- `Objects.equals(left, right)` compares safely when either reference may be null.
- `Objects.requireNonNull(value, "name")` makes a null boundary explicit.
- `Integer.parseInt`, `Long.parseLong`, and wrapper `compare` methods avoid hand-written parsing and subtraction comparators.
- `Character.isDigit`, `isLetter`, `isWhitespace`, and case-conversion methods are clearer than ASCII-only arithmetic when broader text is allowed.

## Complete example

File: `ExceptionsIoUtilitiesExample.java`

```java
import java.io.BufferedReader;
import java.io.IOException;
import java.io.StringReader;
import java.util.Arrays;

public final class ExceptionsIoUtilitiesExample {
    static int[] parseLine(String line) {
        if (line == null || line.isBlank()) {
            return new int[0];
        }

        String[] tokens = line.trim().split("\\s+");
        int[] values = new int[tokens.length];
        for (int index = 0; index < tokens.length; index++) {
            try {
                values[index] = Integer.parseInt(tokens[index]);
            } catch (NumberFormatException exception) {
                throw new IllegalArgumentException(
                        "invalid integer at token " + index + ": " + tokens[index],
                        exception);
            }
        }
        return values;
    }

    static int[] readValues(String source) throws IOException {
        try (BufferedReader reader =
                     new BufferedReader(new StringReader(source))) {
            return parseLine(reader.readLine());
        }
    }

    public static void main(String[] args) throws IOException {
        int[] values = readValues("30 10 20");
        Arrays.sort(values);
        System.out.println(Arrays.toString(values));

        try {
            readValues("4 nope 6");
        } catch (IllegalArgumentException exception) {
            System.out.println(exception.getMessage());
            System.out.println(exception.getCause().getClass().getSimpleName());
        }
    }
}
```

Expected output:

```text
[10, 20, 30]
invalid integer at token 1: nope
NumberFormatException
```

The parsing boundary adds token position while preserving the original cause. The resource is closed automatically, and sorting occurs only after input is valid.

## Edge-case matrix

| Case | Weak assumption | Deliberate choice |
|---|---|---|
| empty input line | token zero exists | define empty-input behavior |
| extra whitespace | split on one literal space | trim and split on `\\s+`, or tokenize |
| numeric overflow text | parse always succeeds | handle `NumberFormatException` |
| integer divide by zero | result is infinity | validate; integer division throws |
| `Math.abs(MIN_VALUE)` | always non-negative | widen or special-case the minimum |
| broad catch | every failure is recoverable | catch the failure you understand |
| `finally` return | harmless cleanup | can suppress an earlier result/failure |
| caller-owned reader | helper may close it silently | document ownership explicitly |
| binary search | array may be unsorted | sort or prove compatible ordering first |

## Interview room

**Interviewer:** When should you catch an exception instead of declaring it?

**Model answer:** I catch it where I can recover, translate it into a boundary-specific failure, or add useful context. If this method cannot make a correct decision, I let the failure propagate according to the API contract. I avoid catching broadly only to log and continue.

**Follow-up:** Does `finally` always execute?

**Model answer:** It normally executes as the `try` or `catch` completes, including return and exception paths, but not under every possible process or JVM termination. I also avoid control flow such as `return` inside `finally` because it can replace earlier outcomes.

## Practice

1. **Foundation:** Parse a line of integers with a defined blank-line policy.
2. **Predict:** Determine the result of `Math.abs(Integer.MIN_VALUE)` and explain it.
3. **Debugging:** Repair catch blocks ordered from `Exception` to `IOException`.
4. **Debugging:** Fix a `Scanner` program that loses a line after `nextInt()`.
5. **Interview Core:** Translate a parsing failure while preserving its cause and token index.
6. **Interview Core:** Explain ownership for a method that accepts a caller-created reader.
7. **SDE-2 Follow-up:** Describe primary and suppressed exceptions in try-with-resources.

## Chapter takeaway

Treat failures and I/O as explicit boundaries. Catch only when you can make a correct decision, preserve causes when translating, assign resource ownership, and learn utility preconditions instead of treating library calls as magic.
