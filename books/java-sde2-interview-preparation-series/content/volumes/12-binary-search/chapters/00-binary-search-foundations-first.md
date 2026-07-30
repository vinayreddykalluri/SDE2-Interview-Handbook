# Binary Search Foundations: Search Space, Invariant, and Progress

Binary search is safe only when a comparison or predicate lets you discard one contiguous part of the remaining search space. Sorted data is the familiar case; a monotone yes/no predicate is the general case.

## Begin with exact search

For sorted `[2, 5, 8, 12, 16]`, searching for 12 compares the middle value 8. Because every value left of 8 is also too small, the entire left half can be discarded.

Use a closed interval when both endpoints are candidates:

```java
static int indexOf(int[] sorted, int target) {
    int left = 0;
    int right = sorted.length - 1;
    while (left <= right) {
        int middle = left + (right - left) / 2;
        if (sorted[middle] == target) {
            return middle;
        }
        if (sorted[middle] < target) {
            left = middle + 1;
        } else {
            right = middle - 1;
        }
    }
    return -1;
}
```

The invariant is: if the target exists, an occurrence remains inside `[left, right]`. The updates exclude `middle`, which has already been proven unequal, so the candidate interval strictly shrinks.

## Why the midpoint formula matters

```java
int middle = left + (right - left) / 2;
```

For nonnegative valid indexes, `left + right` can overflow while `right - left` cannot. Parentheses also make the intended arithmetic explicit.

## Closed versus half-open

| Convention | Candidates | Loop | Empty when |
|---|---|---|---|
| Closed | `[left, right]` | `left <= right` | `left > right` |
| Half-open | `[left, right)` | `left < right` | `left == right` |

Do not combine a closed-loop condition with half-open updates from memory. Write what each boundary means.

## Lower bound from first principles

The lower bound is the first index whose value is at least the target. It is also the insertion position before existing equal values.

```java
static int lowerBound(int[] sorted, int target) {
    int left = 0;
    int right = sorted.length;
    while (left < right) {
        int middle = left + (right - left) / 2;
        if (sorted[middle] < target) {
            left = middle + 1;
        } else {
            right = middle;
        }
    }
    return left;
}
```

Invariant:

- all indexes before `left` are known to contain values less than target;
- all indexes at or after `right` are known to contain values at least target;
- `[left, right)` remains unknown.

For `[1, 2, 2, 2, 5]`, target 2, the result is 1. For target 4, the result is 4. For target 9, the result is `length`.

Upper bound changes the comparison to discard values less than or equal to target. The count of target in sorted data is `upperBound - lowerBound`.

## First true: the general template

Suppose predicate results across ordered candidates are:

```text
false false false true true true
```

Lower bound is exactly "first true" for predicate `value >= target`. Many SDE-2 answer-search problems have the same shape: capacity, speed, days, distance, or threshold.

Before searching an answer, prove:

1. a bounded candidate domain;
2. a monotone predicate;
3. which boundary is requested;
4. a predicate cost that makes the total worthwhile.

## Termination clinic

The common infinite loop is retaining `middle` without changing interval size:

```java
// Broken for some two-element intervals
left = middle;
```

If `middle == left`, no progress occurs. Use `left = middle + 1` when middle is excluded, or choose an upper-biased midpoint for a last-true template that intentionally retains middle.

## Duplicates change the question

Exact search may return any matching index. First occurrence, last occurrence, first greater-or-equal, and first strictly greater are distinct contracts. Say the boundary aloud before code.

## Library semantics

`Arrays.binarySearch` returns an index when found. Otherwise it returns `-(insertionPoint) - 1`. Recover the insertion point with `-result - 1`. With duplicates, the returned matching index is not promised to be the first.

## Complexity

After `k` comparisons, at most roughly `n / 2^k` candidates remain, so exact and boundary searches use O(log n) comparisons and O(1) auxiliary space. Searching an answer with a predicate costing O(f(n)) costs O(f(n) log R), where `R` is the integer candidate-range size.

## Foundation checkpoint

1. What does each boundary mean in your exact-search template?
2. Why must an unsuccessful lower bound be allowed to return `length`?
3. How do duplicates change exact search versus first occurrence?
4. What proves an answer predicate is searchable?
5. Show that every update strictly reduces your interval.
