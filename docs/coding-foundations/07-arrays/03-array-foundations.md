# Array Foundations: From First Principles to Production

Arrays are the base representation behind strings, matrices, heaps, hash-table buckets, dynamic arrays, sorting, and many graph structures. Interview success depends on understanding both the physical model and the invariants that make in-place algorithms safe.

This chapter starts with storage and indexing, then builds toward mutation, resizing, matrices, correctness proofs, Java APIs, and production concerns.

## Learning contract

After this chapter, you should be able to:

- explain why indexed access is constant time;
- distinguish an array object, an element, and a reference stored in an array;
- use half-open ranges consistently;
- analyze access, search, insertion, deletion, copying, and resizing;
- mutate arrays without losing unread data;
- reason about aliases and defensive copies;
- work with rectangular and jagged matrices;
- describe edge cases and invariants before coding.

## 1. What an array represents

An array is a fixed-length, indexed sequence whose slots have one element type. Conceptually, the runtime locates slot <code>i</code> from a base address and an element stride.

~~~mermaid
flowchart LR
    A["Array reference"] --> B["Array object"]
    B --> I0["index 0"]
    B --> I1["index 1"]
    B --> I2["index 2"]
    B --> IX["..."]
    B --> IN["index n - 1"]
    Q["Address of slot i"] --> F["base + i * element stride"]
~~~

This address calculation explains expected <code>O(1)</code> indexed access. Cache behavior, bounds checks, memory locality, and element type still affect real latency.

### Java's array model

~~~java
int[] scores = new int[4];          // primitive values, initially 0
String[] names = new String[4];     // references, initially null
int length = scores.length;         // fixed after allocation
~~~

Important distinctions:

- <code>int[]</code> stores primitive values directly in slots.
- <code>String[]</code> stores references; the String objects live separately.
- Array length is fixed. A variable can later point to a different array.
- Java checks every index and throws for an invalid access.
- Elements receive default values: numeric zero, false, the zero character, or null.
- Java passes an array reference by value. Caller and callee can still identify the same mutable array.

## 2. Index and range discipline

For length <code>n</code>, valid indices satisfy:

~~~text
0 <= index < n
~~~

Prefer half-open ranges <code>[left, right)</code>.

~~~java
for (int i = left; i < right; i++) {
    process(values[i]);
}
~~~

Half-open ranges provide simple algebra:

~~~text
range length = right - left
empty range  = left == right
full array   = [0, n)
adjacent     = [a, b) followed by [b, c)
~~~

Inclusive ranges are valid when used consistently, but their length is <code>right - left + 1</code> and emptiness needs a separate convention.

### Boundary checklist

Before a loop, state:

- whether the right endpoint is included;
- whether empty input and empty ranges are valid;
- whether caller-controlled indices can be negative;
- whether index arithmetic can overflow;
- which data is already finalized;
- whether the algorithm reads a value before overwriting it.

## 3. Core operation complexity

Let <code>n</code> be the array length.

| Operation | Time | Extra space | Reason |
|---|---:|---:|---|
| Read or write index | <code>O(1)</code> | <code>O(1)</code> | Direct index calculation |
| Read length | <code>O(1)</code> | <code>O(1)</code> | Stored with the array |
| Linear search | <code>O(n)</code> | <code>O(1)</code> | May inspect every element |
| Binary search on sorted data | <code>O(log n)</code> | <code>O(1)</code> iterative | Discards half each step |
| Insert into fixed array | Unsupported directly | - | Length cannot change |
| Insert using a new array | <code>O(n)</code> | <code>O(n)</code> | Allocate and shift |
| Delete using a new array | <code>O(n)</code> | <code>O(n)</code> | Allocate and shift |
| Reverse in place | <code>O(n)</code> | <code>O(1)</code> | Swap symmetric pairs |
| Copy | <code>O(n)</code> | <code>O(n)</code> | Every slot is copied |

An <code>ArrayList</code> is a resizable-array abstraction. Indexed access is expected <code>O(1)</code>, append is amortized <code>O(1)</code>, and front insertion remains <code>O(n)</code> because elements shift.

## 4. Fundamental traversal templates

### 4.1 Read-only aggregation

Maintain a summary of the processed prefix.

~~~java
static long sum(int[] values) {
    long total = 0;
    for (int value : values) {
        total += value;
    }
    return total;
}
~~~

**Invariant:** before the next element, <code>total</code> equals the sum of the processed prefix.

Use a wider accumulator when the aggregate can exceed the element type.

### 4.2 Search with an early return

~~~java
static int firstIndexOf(int[] values, int target) {
    for (int i = 0; i < values.length; i++) {
        if (values[i] == target) return i;
    }
    return -1;
}
~~~

A negative sentinel is convenient because no valid index is negative. A public API may use <code>OptionalInt</code> when absence should be explicit.

### 4.3 In-place reversal

~~~java
static void reverse(int[] values) {
    for (int left = 0, right = values.length - 1;
            left < right;
            left++, right--) {
        int temporary = values[left];
        values[left] = values[right];
        values[right] = temporary;
    }
}
~~~

**Invariant:** elements outside the active <code>[left, right]</code> region are already in final reversed positions.

### 4.4 Read-write compaction

A read index scans every element. A write index identifies the next retained slot.

~~~java
static int removeValue(int[] values, int target) {
    int write = 0;
    for (int read = 0; read < values.length; read++) {
        if (values[read] != target) {
            values[write++] = values[read];
        }
    }
    return write;
}
~~~

Only <code>[0, write)</code> belongs to the logical result. Later slots contain unspecified leftovers.

**Invariant:** <code>[0, write)</code> contains exactly the retained values from <code>[0, read)</code>, in original order.

## 5. Insertion and deletion

A fixed array cannot grow or shrink. A logical array with spare capacity inserts by shifting the suffix right.

~~~text
before: [10, 20, 30, _, _], size = 3
insert 15 at index 1
shift : [10, 20, 20, 30, _]
write : [10, 15, 20, 30, _], size = 4
~~~

Shift from right to left. Left-to-right shifting would overwrite unread values.

Deletion shifts the suffix left:

~~~text
before: [10, 15, 20, 30], size = 4
delete index 1
after : [10, 20, 30, _], size = 3
~~~

For reference arrays, clear the unused final slot so it does not retain an object unnecessarily.

## 6. Copying, aliasing, and ownership

Assignment copies the reference, not the array.

~~~java
int[] original = {1, 2, 3};
int[] alias = original;
alias[0] = 99;
// original[0] is also 99
~~~

Create independent slots with:

~~~java
int[] copyA = original.clone();
int[] copyB = Arrays.copyOf(original, original.length);
int[] copyC = new int[original.length];
System.arraycopy(original, 0, copyC, 0, original.length);
~~~

For an object array, these are shallow copies: they copy element references, not the objects. A deep copy requires an explicit element-copy policy.

Before exposing an array, decide:

- whether the method mutates caller-owned input;
- whether returned storage aliases internal state;
- whether a defensive copy is required;
- whether elements themselves are mutable;
- whether copying cost is acceptable.

## 7. Dynamic-array growth and amortized analysis

A dynamic array stores a backing array, logical size, and growth policy.

~~~mermaid
flowchart LR
    A["Append"] --> B{"size < capacity?"}
    B -- Yes --> C["Write at size"]
    B -- No --> D["Allocate larger array"]
    D --> E["Copy live elements"]
    E --> C
    C --> F["Increment size"]
~~~

One resize costs <code>O(n)</code>. Geometric growth makes a sequence of <code>n</code> appends cost <code>O(n)</code> total, so append is amortized <code>O(1)</code>.

Growing one slot at a time copies:

~~~text
1 + 2 + 3 + ... + n = O(n^2) elements
~~~

Amortized constant time does not promise constant latency for every append. Latency-sensitive systems may reserve capacity or use chunked storage.

## 8. Multidimensional and jagged arrays

Java models a matrix as an array of row references.

~~~java
int[][] rectangular = new int[3][4];
int[][] jagged = {
    {1, 2},
    {3, 4, 5},
    {}
};
~~~

Consequences:

- rows can have different lengths;
- a row can be null;
- <code>matrix.length</code> is row count;
- <code>matrix[row].length</code> is that row's column count;
- checking only the first row does not prove rectangular shape.

Use dimension names:

~~~text
rows = matrix.length
columns = matrix[row].length
full rectangular traversal = O(rows * columns)
~~~

Row-major traversal generally has better locality than repeatedly jumping across rows.

## 9. Worked example: rotate right in place

Rotate <code>[1, 2, 3, 4, 5, 6, 7]</code> right by three.

~~~text
reverse all      -> [7, 6, 5, 4, 3, 2, 1]
reverse [0, 3)   -> [5, 6, 7, 4, 3, 2, 1]
reverse [3, 7)   -> [5, 6, 7, 1, 2, 3, 4]
~~~

~~~java
static void rotateRight(int[] values, int steps) {
    if (values.length == 0) return;
    int k = ((steps % values.length) + values.length) % values.length;
    reverseRange(values, 0, values.length);
    reverseRange(values, 0, k);
    reverseRange(values, k, values.length);
}
~~~

Normalization supports negative and oversized step counts. Time is <code>O(n)</code>; extra space is <code>O(1)</code>.

## 10. Correctness framework for array loops

For every nontrivial loop, define:

1. **State:** indices and summaries that describe progress.
2. **Invariant:** what is true before and after every iteration.
3. **Progress:** why the unknown region shrinks.
4. **Termination:** why the invariant implies the answer when work ends.

A common region model is:

~~~text
[ finalized | processed summary | unknown ]
0           left                right
~~~

Draw regions before coding. Many off-by-one defects begin as unclear region definitions.

## 11. Java array API reference

| API | Purpose | Important behavior |
|---|---|---|
| <code>Arrays.copyOf</code> | Resize or copy | Pads with defaults when larger |
| <code>Arrays.copyOfRange</code> | Copy a half-open range | Allocates |
| <code>System.arraycopy</code> | Copy ranges | Handles overlapping ranges |
| <code>Arrays.fill</code> | Initialize slots | Mutates selected range |
| <code>Arrays.sort</code> | Sort | Mutates input |
| <code>Arrays.binarySearch</code> | Search sorted data | Negative result encodes insertion point |
| <code>Arrays.equals</code> | Flat content equality | Uses element equality |
| <code>Arrays.deepEquals</code> | Nested equality | Recurses into nested arrays |
| <code>Arrays.toString</code> | Flat display | Nested arrays need deepToString |

Binary search requires input sorted with the same ordering used for the search.

## 12. Production engineering considerations

### Input contracts

Define behavior for null input, empty input, invalid indices, invalid windows, malformed matrices, and values outside a promised domain.

### Numeric safety

Use <code>long</code> for sums, products, prefix totals, and counts when bounds can exceed <code>int</code>. Use <code>Integer.compare</code> rather than subtraction in comparators.

### Memory and mutation

An auxiliary <code>O(n)</code> array may be too large. An in-place solution may be unacceptable if callers need original order. State the trade-off.

### Concurrency

Arrays are mutable and not inherently thread-safe. Safe publication of a reference does not make compound updates atomic. Prefer ownership, immutable snapshots, or an explicit synchronization protocol.

## 13. Interview questions and model answers

### Q1. Why is indexed access constant time?

The runtime calculates a slot from the array base, index, and fixed slot layout without traversing earlier values.

### Q2. Why is middle insertion linear?

The suffix may need to shift to preserve order. Allocation and copying are also linear when a new fixed array is needed.

### Q3. Is an array passed by reference in Java?

Java passes the reference value by value. A callee can mutate the shared array but cannot replace the caller's variable by reassigning its local parameter.

### Q4. Length versus capacity?

Array length is its fixed slot count. A dynamic array has a logical size and a potentially larger backing-array capacity.

### Q5. Shallow versus deep copy?

A shallow object-array copy duplicates references. A deep copy duplicates mutable element state using an explicit policy.

### Q6. Why prefer half-open intervals?

Length is <code>right - left</code>, empty ranges are natural, adjacent ranges compose, and Java range APIs commonly use them.

### Q7. What counts as auxiliary space?

Temporary storage beyond input and required output. Recursive frames, copied slices, helper arrays, and library allocation must be considered.

### Q8. Why are scans fast in practice?

Compact storage and locality let caches and hardware prefetching serve sequential access efficiently.

### Q9. Can binary search be used on any array?

No. It needs sorted order or another monotonic predicate that proves one side can be discarded.

### Q10. What does amortized append mean?

A long sequence has constant average cost even though an individual resize can be linear.

### Q11. Why use separate read and write indices?

The read index preserves complete discovery while the write index tracks the exact logical output frontier.

### Q12. What changes for jagged matrices?

Rows need independent validation and iteration because lengths may differ or rows may be null.

## 14. Foundations practice ladder

1. Implement sum, minimum, maximum, and search with explicit empty-input behavior.
2. Reverse an array and prove the finalized-region invariant.
3. Insert and delete using a logical size and spare capacity.
4. Compact retained values and return logical length.
5. Rotate with positive, negative, and oversized steps.
6. Build a minimal dynamic array and explain resize cost.
7. Transpose a rectangular matrix and reject malformed input.
8. Design an API that clearly communicates mutation and ownership.

## Runnable references

- [ArrayFundamentals.java](https://github.com/vinayreddykalluri/SDE2-Interview-Handbook/blob/master/examples/java/src/main/java/io/github/vinayreddykalluri/interviewhandbook/codingfoundations/arrays/ArrayFundamentals.java)
- [ArrayDsaPatterns.java](https://github.com/vinayreddykalluri/SDE2-Interview-Handbook/blob/master/examples/java/src/main/java/io/github/vinayreddykalluri/interviewhandbook/codingfoundations/arrays/ArrayDsaPatterns.java)

## 60-second revision

- Arrays provide fixed-length indexed storage.
- Valid indices are <code>[0, length)</code>.
- Access is <code>O(1)</code>; ordered insertion and deletion are <code>O(n)</code>.
- Assignment aliases; copying slots is not necessarily deep copying.
- Read-write pointers preserve a compact logical prefix.
- Geometric resizing gives amortized <code>O(1)</code> append.
- Java matrices are arrays of row references and may be jagged.
- State ranges, mutation, numeric bounds, and invariants explicitly.
