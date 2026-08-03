# Arrays from First Principles

Arrays are the bridge between Java syntax and most early DSA problems. Before learning two pointers or sliding windows, become comfortable with length, indexes, mutation, copying, and row boundaries.

## Declaration, creation, and initialization

```java
int[] numbers;              // declares a reference variable
numbers = new int[4];       // creates an array with four zero values

int[] scores = {70, 85, 90};
```

An array is an object with a fixed length. `numbers.length` is 4 and valid indexes are 0 through 3.

Default element values are zero for numeric primitives, `false` for boolean, the zero code unit for `char`, and `null` for references.

## Read and write by index

```java
int first = scores[0];
scores[1] = 88;
```

Java checks every reached array access. An invalid index throws `ArrayIndexOutOfBoundsException`. A null array reference throws `NullPointerException` before an element can be accessed.

## Traverse safely

```java
for (int index = 0; index < scores.length; index++) {
    System.out.println(index + ": " + scores[index]);
}
```

Use enhanced `for` when only values are needed:

```java
for (int score : scores) {
    System.out.println(score);
}
```

Changing the enhanced-for variable does not replace the array element because the variable receives a copied value.

## Arrays passed to methods

```java
static void fillWithZero(int[] values) {
    for (int index = 0; index < values.length; index++) {
        values[index] = 0;
    }
}
```

The copied reference still designates the caller's array, so element mutation is visible. Reassigning `values` inside the method would not replace the caller's variable.

## Aliasing versus copying

```java
int[] original = {1, 2, 3};
int[] alias = original;
alias[0] = 9;
System.out.println(original[0]); // 9
```

Assignment copies the reference. To create new array storage:

```java
int[] copyA = original.clone();
int[] copyB = java.util.Arrays.copyOf(original, original.length);
int[] copyC = new int[original.length];
System.arraycopy(original, 0, copyC, 0, original.length);
```

For a primitive array, these copy primitive values. For an object array, they copy references, so element objects remain shared. That is a shallow copy.

## Comparing and printing

```java
int[] left = {1, 2};
int[] right = {1, 2};

System.out.println(left == right);                 // false
System.out.println(java.util.Arrays.equals(left, right)); // true
System.out.println(java.util.Arrays.toString(left));      // [1, 2]
```

Array `==` checks identity. Use `Arrays.equals` for one-dimensional content and `Arrays.deepEquals` for nested content when that is the intended contract.

## Two-dimensional and jagged arrays

Java's `int[][]` is an array whose elements are `int[]` references:

```java
int[][] matrix = {
        {1, 2, 3},
        {4, 5, 6}
};

int[][] jagged = {
        {1},
        {2, 3},
        null,
        {4, 5, 6}
};
```

Rows can have different lengths and can be null. Traverse using the current row, not `matrix[0].length`.

## Deep-copy a jagged primitive matrix

Manual first-principles implementation:

```java
static int[][] deepCopy(int[][] source) {
    if (source == null) {
        return null;
    }
    int[][] result = new int[source.length][];
    for (int row = 0; row < source.length; row++) {
        result[row] = source[row] == null ? null : source[row].clone();
    }
    return result;
}
```

This copies the outer array and every non-null primitive row. For arbitrary object graphs, "deep copy" requires a domain-specific ownership definition.

## Useful library operations

- `Arrays.fill(array, value)` writes every element.
- `Arrays.sort(array)` sorts in place.
- `Arrays.binarySearch(array, key)` assumes sorted input.
- `Arrays.copyOf(array, newLength)` copies and can resize the result.
- `System.arraycopy` copies a range and handles overlapping ranges according to its contract.

These APIs do not replace understanding. In an interview, state mutation, preconditions, and complexity before using them.

## Complete example

File: `ArraysExample.java`

```java
import java.util.Arrays;

public final class ArraysExample {
    static int sum(int[] values) {
        int total = 0;
        for (int value : values) {
            total += value;
        }
        return total;
    }

    public static void main(String[] args) {
        int[] values = {4, 1, 3};
        int[] copy = values.clone();
        Arrays.sort(copy);

        System.out.println(sum(values));
        System.out.println(Arrays.toString(values));
        System.out.println(Arrays.toString(copy));
    }
}
```

Expected output:

```text
8
[4, 1, 3]
[1, 3, 4]
```

Sorting the copy preserves the caller's original order.

## Edge-case matrix

| Input or operation | Risk | Required thought |
|---|---|---|
| null array | dereference failure | reject, accept, or map to an empty result explicitly |
| empty array | first/last index does not exist | loops should naturally perform zero work |
| index equals length | out of bounds | valid interval is `[0, length)` |
| assignment to another variable | aliasing | choose alias or copy intentionally |
| nested `clone()` | rows remain shared | copy each row when independent rows are required |
| null jagged row | row dereference fails | define skip/reject/preserve behavior |
| `Arrays.binarySearch` on unsorted input | result has no useful guarantee | sort or prove sorted precondition |
| direct `println(array)` | identity-style text | use `Arrays.toString` or `deepToString` |

## Interview room

**Interviewer:** What does `int[][] copy = original.clone()` copy?

**Model answer:** It creates a new outer array and copies each row reference. The row arrays remain shared, so modifying `copy[0][0]` changes the same row seen through `original`. To copy a jagged primitive matrix independently, clone each non-null row.

**Follow-up:** Is passing an array to a method pass-by-reference?

**Model answer:** No. The array reference value is copied into the parameter. Both references designate the same array, which allows shared mutation, but parameter reassignment is local.

## Practice

1. **Foundation:** Find minimum and maximum without sorting.
2. **Foundation:** Reverse an array in place and then return a reversed copy.
3. **Predict:** Trace aliasing through two array variables and a method call.
4. **Debugging:** Repair a rectangular-only traversal for jagged input.
5. **Interview Core:** Deep-copy an `int[][]` while preserving null rows.
6. **SDE-2 Follow-up:** Design an API that sorts values without surprising its caller about mutation.

## Chapter takeaway

An array is fixed-length mutable storage reached through a reference. Correct array code makes the valid index range, mutation policy, aliasing, and copy depth explicit.
