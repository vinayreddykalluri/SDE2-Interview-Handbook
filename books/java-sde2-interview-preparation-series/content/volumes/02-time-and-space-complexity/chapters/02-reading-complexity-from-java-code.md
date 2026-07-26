# Reading Time Complexity from Java Code

## Learning objectives

This chapter teaches a repeatable method for analyzing Java statements, consecutive phases, branches, nested and dependent loops, helper calls, early exits, sorting, strings, and output-producing code.

## The five-step reading method

For every snippet:

1. Name each independent input dimension.
2. Identify the operation whose count grows.
3. Write a count or sum before simplifying.
4. Include called methods and library APIs.
5. State the case and assumptions.

## Consecutive phases add

```java
static int summarize(int[] values) {
    int negatives = 0;
    for (int value : values) {
        if (value < 0) negatives++;
    }

    int positives = 0;
    for (int value : values) {
        if (value > 0) positives++;
    }
    return negatives + positives;
}
```

The loops run `n` and `n` times: O(n + n) = O(n). They are not nested, so they do not multiply.

If the phases use independent arrays of lengths `n` and `m`, the result is O(n + m), not O(max(n,m)) unless you explicitly use an equivalent bound and preserve the meaning of both inputs.

## Branches usually take the maximum reached cost

```java
if (alreadySorted(values)) {       // O(n)
    return copy(values);           // O(n)
}
Arrays.sort(values);               // O(n log n)
return values;
```

Worst-case time is O(n log n), because only one branch path executes after the condition and sorting dominates. The sorted branch is O(n), but worst-case reporting takes the most expensive reachable path. Include the condition cost.

## Independent nested loops multiply

```java
for (int row = 0; row < rows; row++) {
    for (int column = 0; column < columns; column++) {
        visit(row, column);
    }
}
```

The body runs `rows * columns` times: O(rc). For a square `n x n` matrix this becomes O(n squared), but do not assume square shape unless the contract says so.

## Triangular loops are still quadratic

```java
for (int left = 0; left < n; left++) {
    for (int right = left + 1; right < n; right++) {
        compare(left, right);
    }
}
```

Counts are `(n - 1) + (n - 2) + ... + 1`, approximately `n^2 / 2`. Dropping the constant still gives O(n squared).

## Nested-looking loops can be linear

```java
int left = 0;
for (int right = 0; right < values.length; right++) {
    while (left <= right && windowTooLarge(left, right)) {
        left++;
    }
}
```

Do not multiply `n` by `n` automatically. `right` moves forward at most n times and `left` also moves forward at most n times across the entire method. Total pointer movements are at most 2n, so the control movement is O(n), assuming `windowTooLarge` is O(1).

The proof uses an aggregate count: each pointer never moves backward.

## Dependent inner bounds require a sum

```java
for (int size = 1; size <= n; size *= 2) {
    for (int index = 0; index < size; index++) {
        work();
    }
}
```

The outer loop has log n iterations, but the inner work is `1 + 2 + 4 + ... + n`, a geometric series bounded by 2n. Total is O(n), not O(n log n).

Contrast:

```java
for (int size = 1; size <= n; size *= 2) {
    for (int index = 0; index < n; index++) {
        work();
    }
}
```

Now each of log n outer iterations performs n work, so total is O(n log n).

## Early exits change best case, not necessarily worst case

```java
for (int index = 0; index < values.length; index++) {
    if (values[index] == target) return index;
}
```

Best case O(1), worst case O(n). If an interviewer asks for typical behavior, define a distribution instead of saying "average O(n/2), therefore O(n)" without assumptions.

## Include helper and API costs

```java
for (String word : words) {
    if (word.contains(pattern)) matches++;
}
```

The loop is not simply O(n). If there are n words, average word length `L`, and pattern length `p`, substring-search cost must be included according to the API/algorithm model. A safe interview bound might be O(total text examined times pattern-related cost), with a simpler O(nL p) naive model when explicitly assumed.

Likewise:

```java
for (int value : values) {
    list.contains(value);
}
```

For an ArrayList of size growing to n, each `contains` can be O(n), making the loop O(n squared). Replacing the lookup side with HashSet can make membership expected O(1) and total expected O(n), using O(n) extra space.

## Hidden work in convenient syntax

### String concatenation

```java
String result = "";
for (String word : words) {
    result += word;
}
```

String is immutable. Repeatedly copying a growing result can make total work quadratic in the final character count. Use `StringBuilder` for repeated construction:

```java
StringBuilder builder = new StringBuilder();
for (String word : words) builder.append(word);
String result = builder.toString();
```

This is linear in the characters appended, excluding domain-specific formatting work.

### Copies and conversions

`Arrays.copyOf(array, n)`, `new ArrayList<>(collection)`, `List.copyOf(collection)`, and `stream().toList()` traverse/copy membership. A single method call can still be O(n) and allocate O(n) storage.

### Sorting before a scan

```java
Arrays.sort(values);       // O(n log n)
for (int value : values) { // O(n)
    process(value);
}
```

Total is O(n log n + n) = O(n log n). Do not discard the scan before showing the sum; it may matter for concrete work or if sorting assumptions change.

## Matrix and jagged-array analysis

For a rectangular `r x c` matrix, visiting every cell is O(rc).

For a jagged array, use total cells:

```java
int cells = 0;
for (int[] row : matrix) {
    if (row == null) continue;
    for (int value : row) {
        consume(value);
        cells++;
    }
}
```

Time is O(total cells), often written O(sum of row lengths). `rows * maximumColumns` is a valid upper bound but can be loose.

## Multiple test cases

If there are `t` test cases each of size `n`, O(tn) may be appropriate. If sizes vary, a tighter expression is O(n1 + n2 + ... + nt). Online judges often constrain the sum of input sizes, which can make this distinction important.

## Output-sensitive time

```java
static List<int[]> allMatchingPairs(int[] values) {
    List<int[]> result = new ArrayList<>();
    for (int left = 0; left < values.length; left++) {
        for (int right = left + 1; right < values.length; right++) {
            if (matches(values[left], values[right])) {
                result.add(new int[] {left, right});
            }
        }
    }
    return result;
}
```

Search work is O(n squared). Creating and returning `k` pairs adds O(k) time and output space. When an algorithm otherwise performs O(n) work plus emits k results, report O(n + k).

## Dry-run table: four patterns

| Code shape | Count before simplification | Result |
|---|---:|---:|
| two consecutive n loops | n + n | O(n) |
| independent n by m loops | nm | O(nm) |
| two forward-only pointers | at most n + n moves | O(n) |
| doubling level, full n scan | n log n | O(n log n) |

## Common mistakes

- Multiplying consecutive loops.
- Failing to multiply genuinely independent nested loops.
- Ignoring a helper/API cost.
- Treating an early return as proof of O(1) worst case.
- Calling a two-pointer loop quadratic without aggregate reasoning.
- Calling every doubling-loop combination O(n log n) without summing dependent work.
- Using rows times first-row length for a jagged matrix.
- Ignoring repeated copies or immutable-string growth.
- Dropping multiple independent input variables.

## Practice

1. **Foundation:** Analyze three consecutive loops with lengths n, n, and m.
2. **Foundation:** Derive the exact number of pairs in a triangular nested loop.
3. **Interview Core:** Prove a forward-only sliding window is O(n) by counting pointer moves.
4. **Interview Core:** Compare a list-membership nested pattern with a HashSet alternative.
5. **Interview Core:** Analyze repeated string concatenation by final output length.
6. **SDE-2 Follow-up:** Express the cost of processing variable-sized test cases using a sum rather than one maximum.

## Chapter summary

Read Java complexity by execution: phases add, independent nesting multiplies, dependent bounds require sums, early exits need case labels, and helper/API work must be included. Aggregate movement, total cells, total text, and output size often provide a more accurate dimension than the visible number of loops.
