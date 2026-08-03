# Java Fundamentals Wave 1: Complete Solution Studio

These solutions belong after the beginner-first chapters. Try each exercise before reading its solution. All eleven are executable in `code/fundamentals/FundamentalsSolutionChecks.java`; a successful run prints `PASS 11 Java Fundamentals solution checks`.

## 1. Average without `int` overflow

```java
static double safeAverage(int[] numbers) {
    Objects.requireNonNull(numbers, "numbers");
    if (numbers.length == 0) {
        throw new IllegalArgumentException("numbers must not be empty");
    }
    long sum = 0L;
    for (int number : numbers) {
        sum += number;
    }
    return (double) sum / numbers.length;
}
```

The accumulator is `long` from the first addition. The cast before division prevents integer truncation. Empty input has no defined arithmetic mean, so the contract rejects it explicitly.

## 2. Widen before multiplication

```java
static long safeMultiply(int left, int right) {
    return (long) left * right;
}
```

At least one operand is widened before `*`, so binary numeric promotion makes the multiplication a `long` operation. `(long) (left * right)` would preserve an already wrapped `int` result.

## 3. Explain mutation and reassignment under pass-by-value

```java
static void rename(Person person) {
    person.name = "Updated";
}

static void replace(Person person) {
    person = new Person("Replacement");
}
```

Both methods receive a copy of the caller's reference value. `rename` follows that copied reference to the shared object, so mutation is visible. `replace` changes only the method's local parameter variable, so the caller still refers to the original object.

## 4. Deep-copy a jagged primitive matrix

```java
static int[][] deepCopy(int[][] matrix) {
    Objects.requireNonNull(matrix, "matrix");
    int[][] copy = new int[matrix.length][];
    for (int row = 0; row < matrix.length; row++) {
        copy[row] = matrix[row] == null ? null : matrix[row].clone();
    }
    return copy;
}
```

Cloning only the outer array would still share every row. This solution allocates a new outer array and a new primitive array for each non-null row. It preserves jagged row lengths and the intentional null-row state.

## 5. Convert and validate a decimal character

```java
static int decimalDigit(char character) {
    int value = Character.digit(character, 10);
    if (value < 0) {
        throw new IllegalArgumentException("not a decimal digit: " + character);
    }
    return value;
}
```

`character - '0'` is compact when the contract guarantees ASCII `'0'` through `'9'`. `Character.digit` makes the validation explicit and supports decimal digits recognized by Java's Unicode data.

## 6. Preserve a class invariant

```java
final class BankAccount {
    private long balanceCents;

    BankAccount(long openingBalanceCents) {
        if (openingBalanceCents < 0) {
            throw new IllegalArgumentException("negative opening balance");
        }
        balanceCents = openingBalanceCents;
    }

    void withdraw(long cents) {
        if (cents <= 0 || cents > balanceCents) {
            throw new IllegalArgumentException("invalid withdrawal");
        }
        balanceCents -= cents;
    }
}
```

The invariant is `balanceCents >= 0`. Construction establishes it, and the only mutation shown preserves it. A public `setBalance` would expose invalid states instead of modeling a domain operation.

## 7. Write a type-safe generic `last`

```java
static <T> T last(List<T> values) {
    Objects.requireNonNull(values, "values");
    if (values.isEmpty()) {
        throw new IllegalArgumentException("values must not be empty");
    }
    return values.get(values.size() - 1);
}
```

The same method preserves the element type for `List<String>`, `List<Integer>`, or another reference type. The empty-input behavior is part of the contract instead of an accidental index failure.

## 8. Choose a deterministic most-frequent value

```java
static String mostFrequent(List<String> words) {
    if (words.isEmpty()) {
        throw new IllegalArgumentException("words must not be empty");
    }
    Map<String, Integer> frequencies = new HashMap<>();
    for (String word : words) {
        frequencies.merge(word, 1, Integer::sum);
    }

    String bestWord = null;
    int bestCount = -1;
    for (Map.Entry<String, Integer> entry : frequencies.entrySet()) {
        String word = entry.getKey();
        int count = entry.getValue();
        if (count > bestCount
                || (count == bestCount && word.compareTo(bestWord) < 0)) {
            bestWord = word;
            bestCount = count;
        }
    }
    return bestWord;
}
```

Counting alone does not define a tie. This solution returns the lexicographically smaller word when counts match, so it never depends on `HashMap` iteration order. The executable version also validates null inputs and null elements.

## 9. Add context to a parsing failure

```java
try {
    values[index] = Integer.parseInt(tokens[index]);
} catch (NumberFormatException exception) {
    throw new IllegalArgumentException(
            "invalid integer at token " + index + ": " + tokens[index],
            exception);
}
```

The boundary reports which token failed while retaining the original `NumberFormatException` as the cause. Catching `Exception` or discarding the cause would make diagnosis weaker.

## 10. Compare without arithmetic overflow

```java
record Candidate(String name, int score) {}

static final Comparator<Candidate> CANDIDATE_ORDER =
        Comparator.comparingInt(Candidate::score)
                .reversed()
                .thenComparing(Candidate::name);
```

Subtraction can overflow for extreme scores and can violate the comparator contract. Comparator builders perform a safe primitive comparison and make the name tie-break explicit.

## 11. Protect a complement calculation from overflow

```java
long complement = (long) target - numbers[index];
if (complement >= Integer.MIN_VALUE && complement <= Integer.MAX_VALUE) {
    Integer earlier = earliestIndex.get((int) complement);
    if (earlier != null) {
        return new int[] {earlier, index};
    }
}
```

Both inputs are `int`, so subtracting them directly would also be an `int` operation. Widening the target before subtraction preserves the mathematical difference. A cast back is safe only after the range check. Looking up before inserting the current value also guarantees two distinct indexes.

## Final dry-run prompt

For every solution, answer four questions aloud:

1. What is the input and failure contract?
2. What invariant is preserved?
3. Which edge case would break the most tempting incorrect version?
4. What time, extra space, and mutation behavior does the solution have?

If an answer is unclear, return to the corresponding chapter before moving to Time and Space Complexity.
