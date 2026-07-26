# Index Ranges, Invariants, and Search

Most loop bugs are not syntax bugs. They are disagreements about what an index means. Does `right` name the last included position, or the first excluded position? Has `index` been processed, or is it next? Write those meanings before writing the body.

## 2.1 Indexes are positions, not values

For an array of length `n`, valid element indexes form the half-open range `[0, n)`:

- `0` is included;
- `n` is excluded;
- the number of positions is `n - 0 = n`.

An index can also be a legal **boundary** without naming an element. `n` is the boundary immediately after the last element. This is why insertion points and empty suffixes can validly equal `n`, even though `array[n]` is invalid.

## 2.2 Half-open ranges

`[left, right)` means `left <= index < right`. Its length is always `right - left`. It is empty exactly when `left == right`.

![Processed and remaining regions under a half-open loop invariant](series/volumes/05-loop-mastery-and-index-calculations/assets/02-half-open-range-invariant.png)

For a forward traversal:

```java
for (int index = 0; index < values.length; index++) {
    process(values[index]);
}
```

At the top of every iteration:

- `[0, index)` has already been processed;
- `[index, values.length)` remains;
- `0 <= index <= values.length`.

This is a loop invariant: a statement true before the first iteration and preserved by every iteration.

### Why half-open ranges compose cleanly

Splitting `[0, n)` at `mid` produces `[0, mid)` and `[mid, n)`. The ranges do not overlap and leave no gap. Their lengths add:

```text
mid - 0 + n - mid = n
```

Java libraries use this convention frequently: `String.substring(begin, end)`, `Arrays.copyOfRange`, streams with `range(start, end)`, and list `subList(from, to)`.

## 2.3 Closed ranges

`[left, right]` includes both endpoints. Its length is `right - left + 1`, and it is empty when `left > right`.

Closed ranges can be useful for two pointers that begin on actual elements:

```java
int left = 0;
int right = values.length - 1;
while (left <= right) {
    // [left, right] is the unprocessed region
}
```

Neither convention is universally superior. The bug appears when initialization follows one convention and the condition follows another.

| Convention | Initial full range | Nonempty test | Length | Empty state |
|---|---|---|---:|---|
| half-open | `[0, n)` | `left < right` | `right - left` | `left == right` |
| closed | `[0, n - 1]` | `left <= right` | `right - left + 1` | `left > right` |

Write the range in a comment for binary search, windows, and partition code.

## 2.4 The three-part invariant proof

An invariant becomes useful when you test it in three places:

1. **Initialization:** is it true before the first iteration?
2. **Maintenance:** assuming it is true at the top, does one iteration preserve it?
3. **Termination:** when the condition becomes false, what result follows from the invariant?

Example: summing `[0, n)`.

```java
long sum = 0;
for (int index = 0; index < values.length; index++) {
    sum += values[index];
}
```

Invariant: `sum` equals the sum of values in `[0, index)`.

- Initialization: at `index = 0`, the range is empty and `sum = 0`.
- Maintenance: add `values[index]`, then the update advances `index`; the larger processed range is summarized.
- Termination: `index == n`, so `sum` represents `[0, n)`, the entire array.

## 2.5 Progress measures

An invariant explains correctness; a progress measure explains termination. Common measures include:

- `n - index` for forward traversal;
- `index + 1` for reverse traversal;
- `right - left` for a half-open interval;
- `right - left + 1` for a nonempty closed interval;
- remaining unvisited cells in a matrix;
- total pointers that can still advance.

A good measure is bounded below and strictly decreases, or is bounded above and strictly increases. If a branch leaves it unchanged, examine that branch for an infinite loop.

## 2.6 Fencepost reasoning

If posts stand at positions 0 through 4, there are 5 posts but only 4 gaps. Algorithms frequently confuse **items** with **boundaries**.

For `n` elements:

- valid indexes: `0` through `n - 1`;
- boundaries: `0` through `n`;
- adjacent pairs: `max(0, n - 1)`;
- all unordered pairs: `n * (n - 1) / 2`;
- a window `[left, right]`: `right - left + 1` elements;
- a window `[left, right)`: `right - left` elements.

Use `long` for pair counts:

```java
long pairs = (long) n * (n - 1) / 2;
```

The cast must occur before multiplication.

## 2.7 Reverse-loop traps

Correct reverse index traversal:

```java
for (int index = values.length - 1; index >= 0; index--) {
    process(values[index]);
}
```

Avoid unsigned-style thinking. Java `int` is signed, so `index` can become `-1` and stop. A `char` loop variable is dangerous because `char` is unsigned and wraps instead of becoming negative.

Also avoid this overflow-prone countdown when `start` might be `Integer.MIN_VALUE`:

```java
for (int value = start; value >= end; value--) { ... }
```

After processing `Integer.MIN_VALUE`, decrement wraps to `Integer.MAX_VALUE`. State a supported numeric domain or use a wider type and a safe exit arrangement.

## 2.8 Midpoint calculations

The naive midpoint can overflow:

```java
int mid = (low + high) / 2;
```

Prefer:

```java
int mid = low + (high - low) / 2;
```

This is safe when `0 <= low <= high <= Integer.MAX_VALUE`. It does not magically fix invalid or arbitrarily signed bounds. The proof depends on the range contract.

For a half-open interval `[low, high)`, `mid` satisfies `low <= mid < high` whenever the interval is nonempty.

## 2.9 Lower bound: first value at least target

Lower bound returns the first index `i` whose sorted value is at least `target`. If no such element exists, it returns `values.length`. The result is an insertion boundary, not necessarily an existing element.

```java
static int lowerBound(int[] values, int target) {
    int low = 0;
    int high = values.length;
    while (low < high) {
        int mid = low + (high - low) / 2;
        if (values[mid] < target) {
            low = mid + 1;
        } else {
            high = mid;
        }
    }
    return low;
}
```

![A complete half-open lower-bound trace](series/volumes/05-loop-mastery-and-index-calculations/assets/09-lower-bound-half-open-search.png)

### Invariant

- Every index below `low` contains a value less than target.
- The answer remains somewhere in `[low, high]`, where `high == n` represents the insertion position after the array.
- The searchable half-open interval is `[low, high)`.

If `values[mid] < target`, `mid` cannot be the answer, so discard through `mid` using `low = mid + 1`. Otherwise `mid` may be the first qualifying index, so preserve it with `high = mid`.

### Why `high = mid - 1` is wrong here

That update belongs to a different, closed-interval template. In this half-open search, `mid` may be the answer and must not be discarded.

### Boundary examples

For `[1, 3, 5, 7, 9]`:

| target | result | meaning |
|---:|---:|---|
| 0 | 0 | insert before all values |
| 1 | 0 | first value at least 1 |
| 6 | 3 | value 7 is first at least 6 |
| 9 | 4 | exact last position |
| 10 | 5 | insert after all values |

## 2.10 Upper bound and duplicate ranges

Upper bound returns the first index with value greater than target:

```java
static int upperBound(int[] values, int target) {
    int low = 0;
    int high = values.length;
    while (low < high) {
        int mid = low + (high - low) / 2;
        if (values[mid] <= target) {
            low = mid + 1;
        } else {
            high = mid;
        }
    }
    return low;
}
```

In a sorted array, all occurrences of `target` occupy `[lowerBound(target), upperBound(target))`. The count is `upper - lower`. This remains correct when the target is absent because the interval is empty.

The dedicated Binary Search volume develops answer-space search, rotated arrays, and proof templates. Here the goal is to master range meaning before using those variants.

## 2.11 Index arithmetic and overflow

These two lines are not equivalent:

```java
long wrong = row * columns + col;
long correct = (long) row * columns + col;
```

In `wrong`, both operands of multiplication are `int`; overflow can occur before assignment widens the already-wrong result. In `correct`, the cast promotes the multiplication to `long`.

Use checked arithmetic when overflow must become an explicit failure:

```java
long base = Math.multiplyExact((long) row, columns);
long flat = Math.addExact(base, col);
```

## 2.12 A boundary test matrix

For every index algorithm, test by category rather than by random examples alone:

| Category | Example | What it exposes |
|---|---|---|
| empty | `[]` | invalid first/last assumptions |
| singleton | `[5]` | `left < right` versus `<=` |
| first position | target at 0 | skipped initialization |
| last position | target at `n - 1` | premature termination |
| absent below | target smaller than all | boundary zero |
| absent above | target larger than all | boundary `n` |
| duplicates | `[2,2,2]` | first versus any occurrence |
| extreme integers | MIN/MAX | overflow in arithmetic |

## 2.13 Debugging index failures

When an exception or wrong answer appears:

1. Write the valid element range.
2. Write the meaning of each boundary.
3. Record state at the top of the loop, not only after the body.
4. Check which update discards the current position.
5. Confirm the interval becomes strictly smaller.
6. Test empty and singleton inputs before large inputs.
7. Widen before arithmetic, not after.

## 2.14 Interview checkpoint

You should now be able to explain why `[left, right)` has length `right - left`, prove a summation loop using an invariant, identify a progress measure, implement lower bound without mixing templates, and distinguish an element index from an insertion boundary. The next chapter uses these foundations to move two indexes for a reason rather than by guesswork.
