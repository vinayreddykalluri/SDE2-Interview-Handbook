# Array Foundations from Zero

Arrays are the first data structure to make index reasoning, mutation, ownership, and complexity visible at the same time. This chapter starts with the object Java creates, not with an interview trick. Later chapters will add patterns only after the storage and range rules are automatic.

## 1.1 What an array is

An array is a fixed-length Java object whose slots all have one component type. An `int[]` stores primitive `int` values. A `Student[]` stores references that may point to `Student` objects or be `null`.

```java
int[] scores = new int[4];
String[] names = new String[3];
```

Java creates both array objects on the heap under the normal object-allocation model. `scores` and `names` are local reference variables in this example. The language guarantees array behavior, component type, length, default values, and bounds checks. It does not promise a universal byte count for the complete object; headers, alignment, and reference width depend on the JVM.

![Primitive values and object references inside Java array slots](content/volumes/06-arrays-and-array-patterns/assets/01-array-storage-and-references.png)

The slots are default-initialized:

| Component | Default slot value |
|---|---|
| integer primitives | `0` |
| floating-point primitives | `0.0` |
| `char` | `\u0000` |
| `boolean` | `false` |
| reference type | `null` |

This rule applies to array elements and fields. A local variable such as `int count;` still has no automatic value and must be definitely assigned before use.

## 1.2 Declaration, creation, and initialization

Keep brackets with the type so mixed declarations remain readable:

```java
int[] first;                    // declaration only
first = new int[3];             // creation and assignment
int[] second = {8, 3, 5};       // initializer syntax
int[] third = new int[] {2, 4}; // explicit array expression
```

The array length is fixed at construction. `first.length` is `3` and cannot be changed. The elements can change:

```java
first[0] = 10;
first[1] = first[0] + 5;
```

To grow a sequence, create a larger array and copy, or use a dynamic collection such as `ArrayList`. A Java array never silently resizes.

## 1.3 Indexes and the valid range

For length `n`, valid indexes form the half-open range `[0, n)`:

```text
first valid index       0
last valid index        n - 1, when n > 0
number of valid indexes n
```

```java
static long sum(int[] values) {
    long total = 0;
    for (int index = 0; index < values.length; index++) {
        total += values[index];
    }
    return total;
}
```

### Dry run

For `[4, -1, 7]`, the loop reads indexes `0`, `1`, and `2`. After the update from `2` to `3`, the condition `3 < 3` is false. Index `3` is never accessed.

| `index` before test | condition | action | `total` after body |
|---:|---|---|---:|
| 0 | true | add 4 | 4 |
| 1 | true | add -1 | 3 |
| 2 | true | add 7 | 10 |
| 3 | false | exit | 10 |

An empty array has length zero. The same loop performs zero iterations without a special case. Code that immediately reads `values[0]` needs a nonempty precondition or an explicit empty policy.

## 1.4 What happens during indexed access

At the language level, evaluating `values[index]` requires a non-null array reference and an index between zero and `length - 1`. A null array causes `NullPointerException`; an invalid index causes `ArrayIndexOutOfBoundsException`.

The useful constant-time intuition is that the runtime can compute the location from the array base and the index rather than walking from the first element. This explains `O(1)` indexed access. It does not mean access is literally free: loads, checks, cache behavior, and element processing still matter. A JIT compiler may remove redundant bounds checks when it can prove a loop safe, but correctness must never depend on that optimization.

## 1.5 Primitive slots versus reference slots

```java
int[] numbers = {10, 20};
String[] words = {"red", "blue"};
```

`numbers[0]` is the value `10`. `words[0]` is a reference to a `String`. Reassigning one reference slot does not change another object:

```java
words[0] = "green";
```

If two slots refer to the same mutable object, a mutation through either reference is shared:

```java
StringBuilder shared = new StringBuilder("A");
StringBuilder[] builders = {shared, shared};
builders[0].append("B");
System.out.println(builders[1]); // AB
```

The array stores two copies of one reference value, not two builders.

## 1.6 Array variables, aliasing, and pass-by-value

Assignment copies the reference value:

```java
int[] original = {1, 2, 3};
int[] alias = original;
alias[0] = 99;
System.out.println(original[0]); // 99
```

Java is still pass-by-value. Passing an array copies its reference value into the parameter:

```java
static void updateFirst(int[] values) {
    values[0] = 50;       // mutates the shared array object
}

static void replace(int[] values) {
    values = new int[] {7, 8}; // reassigns only the local parameter
}
```

`updateFirst` is visible to the caller because both references reach the same array. `replace` does not change the caller's variable because the parameter holds its own copied reference value.

## 1.7 One-dimensional, nested, and jagged arrays

Java has arrays of arrays, not a special contiguous matrix type:

```java
int[][] rectangular = new int[2][3];
int[][] jagged = {
        {1, 2, 3},
        {4},
        null
};
```

Each row is an independent reference. Rows may have different lengths or be null. A rectangular algorithm must validate that contract before using `matrix[0].length` for every row.

```java
static long sumJagged(int[][] matrix) {
    if (matrix == null) {
        throw new IllegalArgumentException("matrix must not be null");
    }
    long sum = 0;
    for (int[] row : matrix) {
        if (row == null) {
            continue;
        }
        for (int value : row) {
            sum += value;
        }
    }
    return sum;
}
```

Time is proportional to the number of visited elements, not automatically `rows * firstRowLength`.

## 1.8 Runtime component types and covariance

Arrays retain their runtime component type. Java permits reference-array covariance:

```java
Number[] numbers = new Integer[2];
numbers[0] = 10;
// numbers[1] = 2.5; // throws ArrayStoreException at runtime
```

The variable type permits `Number`, but the actual array object accepts only `Integer`. Generic collections normally reject the unsafe assignment at compile time. In interview code, prefer precise array types and avoid using covariance as an API convenience.

## 1.9 Small `Arrays` toolkit

```java
import java.util.Arrays;

int[] values = {4, 1, 3};
System.out.println(Arrays.toString(values));

int[] copy = Arrays.copyOf(values, values.length);
Arrays.fill(copy, 7);

System.out.println(Arrays.equals(values, copy));
Arrays.sort(values);
int position = Arrays.binarySearch(values, 3); // array must use same order
```

Use `Arrays.deepToString` and `Arrays.deepEquals` for nested content. Calling `values.toString()` or comparing arrays with `==` observes object identity, not element content. Full binary-search patterns belong in Study Step 12; here the API appears only to establish its sorted-input precondition.

## 1.10 Complexity baseline

| Operation | Time | Important qualification |
|---|---:|---|
| read or replace one known index | `O(1)` | index must be valid |
| scan all `n` slots | `O(n)` | includes element-processing cost |
| copy `n` slots | `O(n)` | object elements copy references |
| compare contents | `O(n)` worst case | can stop at first mismatch |
| insert/delete in a fixed array | `O(n)` movement worst case | logical size must be managed separately |
| allocate array of length `n` | `O(n)` initialization work | every slot receives a default value |

## 1.11 Common first-week mistakes

- using `index <= values.length` instead of `<`;
- reading index zero without defining empty behavior;
- confusing `length`, `length()`, and `size()`;
- believing assignment copies the elements;
- saying Java passes the array by reference;
- assuming `int[][]` is rectangular or one flat block;
- printing an array directly;
- using `==` when content comparison is required;
- accumulating an `int` sum that can overflow; and
- writing to a covariant array through an overly broad variable type.

## 1.12 Quick check

1. Why does `new String[3]` contain null references rather than empty strings?
2. Which indexes are valid for an array of length zero? For length one?
3. Why can a method mutate an array but not replace the caller's variable?
4. What does `clone()` copy for `int[]`? What changes for `int[][]`?
5. Why is indexed access `O(1)` while inserting at the front is usually `O(n)`?
6. What exception separates a null array from a bad index?

## 1.13 Foundation practice

**Foundation:** Write `min`, `max`, `sum`, and `contains` methods that define null and empty behavior. Use `long` for the sum.

**Interview Core:** Write a deep-copy method for a jagged `int[][]` that preserves null rows, then prove that changing a copied row cannot affect the original.

**SDE-2 Follow-up:** Design a class that accepts an array in its constructor and exposes a snapshot. Decide where defensive copies occur and explain the time, space, and concurrency consequences.

## Chapter summary

A Java array is a fixed-length, reified object. Its variable stores a reference, its slots store primitives or references, and valid indexes form `[0, length)`. Assignment and parameter passing copy reference values, so aliasing and mutation must be explicit. Nested arrays are arrays of row references and may be jagged. These rules are the foundation for every pattern that follows.
