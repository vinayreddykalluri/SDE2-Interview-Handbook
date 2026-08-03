# State Traces and Live Loop Interviews

> **A note from Vinay:** When a loop feels confusing, stop reading it as syntax. Write one sentence that must be true before every iteration, then trace only the variables that can change. Most off-by-one bugs become visible immediately.

## 1. A loop proof has four parts

For an interview-quality loop, state:

1. **Initialization:** why the invariant is true before the first iteration.
2. **Preservation:** why one iteration keeps it true.
3. **Progress:** which quantity moves toward termination.
4. **Exit meaning:** what the invariant tells us when the condition becomes false.

This is more useful than saying “the loop looks correct.”

## 2. Compaction trace

Remove zeros in place while keeping nonzero values in order:

```text
input: [4, 0, 2, 0, 7]

read  value  write-before  action                array-after
0     4      0             copy to index 0       [4,0,2,0,7]
1     0      1             skip                  [4,0,2,0,7]
2     2      1             copy to index 1       [4,2,2,0,7]
3     0      2             skip                  [4,2,2,0,7]
4     7      2             copy to index 2       [4,2,7,0,7]
```

Invariant before processing `read`: `[0, write)` contains exactly the nonzero values from `[0, read)`, in original order. At exit, `write` is the new logical length.

## 3. Sliding-window trace

For nonnegative values, find the longest range whose sum is at most `5`:

```text
values = [2, 1, 3, 1]

right  add  sum-before-shrink  left-after  legal window  best
0      2    2                  0           [0,0]         1
1      1    3                  0           [0,1]         2
2      3    6                  1           [1,2]         2
3      1    5                  1           [1,3]         3
```

Why the nonnegative contract matters: removing values from the left cannot increase the sum. With negative values, the same shrink rule can discard a prefix that would later become useful.

## 4. Cyclic index normalization without a library shortcut

Java remainder may be negative. Normalize an index into `[0, length)` with:

```java
static int normalizeIndex(int index, int length) {
    if (length <= 0) {
        throw new IllegalArgumentException("length must be positive");
    }
    int remainder = index % length;
    return remainder < 0 ? remainder + length : remainder;
}
```

`Math.floorMod(index, length)` is the standard-library equivalent for a positive length. The manual version shows the sign rule; the library version is preferable when normalization itself is not the interview question.

## 5. Flattening and unflattening safely

For a rectangular grid with `columns` columns:

```text
flat = row * columns + column
row = flat / columns
column = flat % columns
```

Use `long` for the multiplication when dimensions may approach `int` limits. Validate jagged arrays separately because a single global column count does not describe them.

## 6. Edge and failure matrix

| Case | Typical bug | Repair |
|---|---|---|
| Empty array | initialize from element zero | define and handle the empty contract first |
| Inclusive upper bound | use `index <= length` | prefer half-open `[0, length)` ranges |
| Reverse loop | unsigned or non-progressing index | use signed `int` and prove decrement |
| Cyclic negative index | use raw `%` result | normalize or call `floorMod` |
| Sliding sum | use `int` for large totals | widen before addition and define overflow policy |
| Negative window values | reuse nonnegative shrink rule | choose prefix state or another valid invariant |
| Jagged matrix | assume `grid[0].length` for every row | use each row's actual length |
| Binary-search midpoint | `(left + right) / 2` overflow | use `left + (right - left) / 2` or widened arithmetic |
| Nested pointers | reset a monotonic pointer | preserve direction if the linear aggregate proof requires it |
| Mutation during enhanced-for | assume iteration variable writes back | use indexes or an iterator with the correct mutation API |

## 7. Six live interview rounds

### Round 1 - Remove duplicates from sorted input

**Candidate opening:** I will keep `[0, write)` as the unique compacted prefix and scan with `read`.

**Model answer:** When `values[read]` differs from the last kept value, copy it to `values[write]` and increment `write`. Each index is visited once, so time is `O(n)` and auxiliary space is `O(1)`.

**Follow-up:** Empty input must return logical length zero before reading `values[0]`.

### Round 2 - Pair sum in sorted input

**Candidate opening:** The sum comparison tells me which endpoint can be eliminated.

**Model answer:** If the sum is too small, increasing `left` is the only move that can help; if too large, decreasing `right` is the only move that can help. This elimination proof depends on sorted order. Widen to `long` before adding two `int` values.

### Round 3 - Minimum-length window

**Interviewer:** Why does the usual shrinking window require nonnegative values?

**Model answer:** With nonnegative values, expanding cannot decrease the sum and shrinking cannot increase it, so pointer movement is monotonic. Negative values break that relationship, so the same invariant no longer proves correctness.

### Round 4 - Spiral traversal

**Model answer:** Maintain unprocessed boundaries `top`, `bottom`, `left`, and `right`. After traversing the top and right edges, re-check whether a bottom row or left column still exists before traversing them. This prevents duplicates in a single row or column.

### Round 5 - Circular buffer index

**Interviewer:** Why can `(index + delta) % length` be wrong?

**Model answer:** Java remainder keeps the dividend's sign, so a negative sum produces a negative index. Normalize the remainder into `[0, length)` and reject nonpositive length. Widen the addition if `index + delta` may overflow.

### Round 6 - Debug nontermination

**Interviewer:** A binary-search loop sometimes hangs. What do you inspect first?

**Model answer:** I write the interval convention and verify every branch strictly reduces its size. Assigning `left = mid` in a closed interval can fail when `mid == left`; use `left = mid + 1` when `mid` is eliminated, or use a carefully derived half-open template.

## 8. Rapid interviewer questions

1. **Why prefer half-open ranges?** Length is `end - start`, and empty ranges use `start == end`.
2. **Can nested loops be linear?** Yes when aggregate pointer movement is bounded linearly.
3. **When is enhanced-for appropriate?** Read-only traversal when the index is irrelevant.
4. **What does `continue` do?** Skips the remainder of the current iteration and proceeds with the loop's next update/condition step.
5. **What does `break` do?** Exits the nearest targeted loop; a label can target an outer loop but should remain readable.
6. **How do you prove termination?** Identify a bounded measure that changes strictly toward the exit condition.
7. **Why widen before multiplication?** Assignment to `long` occurs after an `int * int` expression has already overflowed.
8. **What is an off-by-one test set?** Empty, one item, two items, first/last match, no match, and full-range match.
9. **How do you traverse a jagged matrix?** Use `grid[row].length` for each row.
10. **Why can direct collection removal fail in enhanced-for?** The iterator detects unsupported structural modification; use its removal contract or another traversal.
11. **When should two pointers move in the same direction?** Compaction, merge, windows, and partition scans whose invariant preserves processed prefixes.
12. **What should you say before coding?** The interval or processed-region invariant and the progress rule.

## 9. Executable evidence

`LoopInvariantChecks.java` validates compaction, cyclic normalization, flattened indexes, nonnegative sliding windows, and aggregate movement. Its boundary tests are intentionally small enough to reproduce by hand.
