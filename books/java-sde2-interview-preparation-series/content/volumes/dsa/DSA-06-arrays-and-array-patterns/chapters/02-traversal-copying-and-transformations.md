# Traversal, Copying, and Basic Transformations

Before learning named patterns, become fluent with the operations from which those patterns are built: visit, search, copy, shift, swap, reverse, and rotate. The central question is not merely "does this loop run?" but "what region already has a reliable meaning?"

## 2.1 Choose traversal from the information you need

Use enhanced-for for a read-only scan that does not need an index:

```java
static long sum(int[] values) {
    long total = 0;
    for (int value : values) {
        total += value;
    }
    return total;
}
```

Use an index when position, neighbors, mutation, or a subrange matters:

```java
static int firstIndexOf(int[] values, int target) {
    for (int index = 0; index < values.length; index++) {
        if (values[index] == target) {
            return index;
        }
    }
    return -1;
}
```

Use reverse traversal when later state must be protected or when the problem asks for the last occurrence:

```java
static int lastIndexOf(int[] values, int target) {
    for (int index = values.length - 1; index >= 0; index--) {
        if (values[index] == target) {
            return index;
        }
    }
    return -1;
}
```

For an empty array, `values.length - 1` is `-1`, so the reverse loop exits safely. Do not use an unsigned index or a condition that prevents the loop from reaching `-1`.

## 2.2 Half-open helper ranges

Reusable array helpers should state their range convention. This book uses `[from, toExclusive)`:

```java
static void reverse(int[] values, int from, int toExclusive) {
    if (values == null || from < 0 || from > toExclusive
            || toExclusive > values.length) {
        throw new IllegalArgumentException("invalid range");
    }
    for (int left = from, right = toExclusive - 1;
            left < right;
            left++, right--) {
        int temporary = values[left];
        values[left] = values[right];
        values[right] = temporary;
    }
}
```

The empty range `[k, k)` is valid and needs no special case. The length is always `toExclusive - from`. Adjacent ranges `[a, b)` and `[b, c)` meet without overlap.

## 2.3 Logical size inside a fixed physical array

Interview questions sometimes simulate a resizable sequence inside an array. Keep two concepts separate:

```text
physical capacity = storage.length
logical size      = number of meaningful elements
valid data        = storage[0 .. size)
spare capacity    = storage[size .. storage.length)
```

![Insertion and deletion shift a suffix while logical size changes](content/volumes/dsa/DSA-06-arrays-and-array-patterns/assets/02-logical-size-and-shifts.png)

Insertion at `index` first shifts the suffix right, starting from the end so unread values are not overwritten:

```java
static int insert(int[] storage, int size, int index, int value) {
    if (storage == null || size < 0 || size > storage.length
            || index < 0 || index > size || size == storage.length) {
        throw new IllegalArgumentException("invalid insertion");
    }
    for (int current = size; current > index; current--) {
        storage[current] = storage[current - 1];
    }
    storage[index] = value;
    return size + 1;
}
```

Deletion shifts left, then optionally clears the stale primitive slot for readable debugging:

```java
static int delete(int[] storage, int size, int index) {
    if (storage == null || size < 0 || size > storage.length
            || index < 0 || index >= size) {
        throw new IllegalArgumentException("invalid deletion");
    }
    for (int current = index; current + 1 < size; current++) {
        storage[current] = storage[current + 1];
    }
    storage[size - 1] = 0;
    return size - 1;
}
```

Both operations move `O(n)` elements in the worst case. A faster binary search for the position does not remove the shifting cost.

## 2.4 Copying APIs and overlap

| API | Result | Key use |
|---|---|---|
| `values.clone()` | same-length top-level copy | simplest exact copy |
| `Arrays.copyOf(values, newLength)` | copy, truncate, or pad | resize into a new array |
| `Arrays.copyOfRange(values, from, to)` | new half-open slice | independent range |
| `System.arraycopy(source, s, target, t, count)` | copy into existing array | bulk movement, including overlap |

`System.arraycopy` handles overlapping ranges as if the relevant values were protected before destructive overwrite. This makes it suitable for shifts, though an interview loop is often clearer when explaining the invariant.

All these operations are shallow for reference components:

```java
StringBuilder[] original = {new StringBuilder("A")};
StringBuilder[] copy = original.clone();
copy[0].append("B");
System.out.println(original[0]); // AB
```

The array objects are distinct; the builder is shared.

## 2.5 Deep-copy a jagged array

```java
static int[][] deepCopy(int[][] matrix) {
    if (matrix == null) {
        return null;
    }
    int[][] copy = new int[matrix.length][];
    for (int row = 0; row < matrix.length; row++) {
        copy[row] = matrix[row] == null ? null : matrix[row].clone();
    }
    return copy;
}
```

### Execution intuition

The outer allocation creates new row-reference slots. Each non-null `clone()` creates a new primitive row. Null rows remain null. Mutating `copy[0][0]` cannot affect `matrix[0][0]` because no row object is shared.

For an object graph, "deep copy" needs a domain definition. Cloning only the row array still shares mutable element objects. Often immutable elements, records, or an ownership boundary are better than an elaborate universal copier.

## 2.6 Reverse in place

The reversal invariant is:

- positions before `left` already contain their final reversed values;
- positions after `right` already contain their final reversed values;
- `[left, right]` remains unresolved.

Each swap settles two positions. The unresolved interval shrinks, giving `O(n)` time and `O(1)` auxiliary space.

Frequent mistakes are swapping until `left <= right` without understanding the harmless middle self-swap, using an inclusive helper but calling it with an exclusive endpoint, or losing a value by assigning without a temporary.

## 2.7 Rotate by reversal

To rotate right by `distance`:

1. return immediately when the array is empty;
2. normalize `k = Math.floorMod(distance, values.length)`;
3. reverse the whole array;
4. reverse `[0, k)`;
5. reverse `[k, n)`.

```java
static void rotateRight(int[] values, int distance) {
    if (values == null) {
        throw new IllegalArgumentException("values must not be null");
    }
    if (values.length == 0) {
        return;
    }
    int k = Math.floorMod(distance, values.length);
    reverse(values, 0, values.length);
    reverse(values, 0, k);
    reverse(values, k, values.length);
}
```

For `[1,2,3,4,5,6,7]`, `k = 3`:

```text
reverse all     [7,6,5,4,3,2,1]
reverse first 3 [5,6,7,4,3,2,1]
reverse rest    [5,6,7,1,2,3,4]
```

`Math.floorMod` gives defined negative-distance behavior. Taking `% values.length` before the empty check throws `ArithmeticException`.

## 2.8 Transform into a new array or mutate?

An out-of-place transform is often easier to reason about:

```java
static int[] squares(int[] values) {
    int[] result = new int[values.length];
    for (int i = 0; i < values.length; i++) {
        result[i] = Math.multiplyExact(values[i], values[i]);
    }
    return result;
}
```

Mutation can reduce auxiliary storage but expands the contract. Ask:

- Does the caller still need the original ordering or values?
- Are aliases held elsewhere?
- Is partial mutation acceptable if validation fails midway?
- Does the method name communicate destructive behavior?
- Is the saved memory material at the input size?

For public APIs, validate the full input before mutating when a later failure would leave an unusable half-transformed array.

## 2.9 Complexity and space language

Be precise about output space. Returning a new array of length `n` requires `O(n)` output storage even if auxiliary workspace beyond the required result is `O(1)`. "In place" should not be used to hide recursion stacks, temporary copies, or caller-owned output.

| Transformation | Time | Auxiliary space | Input preserved? |
|---|---:|---:|---|
| exact copy | `O(n)` | `O(n)` copy | yes |
| reverse | `O(n)` | `O(1)` | no |
| reversal rotation | `O(n)` | `O(1)` | no |
| rotate into result | `O(n)` | `O(n)` output | yes |
| deep-copy primitive matrix | proportional to all elements | proportional to all elements | yes |
| insert/delete in logical array | `O(n)` worst case | `O(1)` | no |

## 2.10 Debugging checklist

- Write the range convention beside every helper.
- Check empty input before computing `n - 1` for a direct access or modulus by `n`.
- For right shifts, copy from high indexes to low indexes.
- For left shifts, copy from low indexes to high indexes.
- Decide whether the suffix beyond logical size is unspecified or cleared.
- Test `distance = 0`, `n`, `n + 1`, and a negative value for rotation.
- Verify copied nested rows do not alias.
- Count output storage honestly.

## 2.11 Quick check and practice

1. Why must insertion shift from right to left?
2. Which array-copy operations are shallow for object elements?
3. Why is an empty half-open range useful?
4. What is the difference between physical capacity and logical size?
5. Why can an in-place method be a worse production API despite lower auxiliary space?

**Foundation:** Implement independent left and right rotations using an output array first. Then implement reversal rotation and compare contracts.

**Interview Core:** Implement insert/delete helpers and test every legal boundary, full capacity, and empty logical size.

**SDE-2 Follow-up:** Design an immutable `IntSnapshot` that owns a defensive copy, returns safe ranges, and avoids copying on every internal read.

## Chapter summary

Array transformations are region-management problems. Traversal chooses the information available, half-open ranges make boundaries composable, shifts must protect unread values, and copy depth determines ownership. Reversal and rotation demonstrate how a small invariant turns mutation into a proof rather than a guess.
