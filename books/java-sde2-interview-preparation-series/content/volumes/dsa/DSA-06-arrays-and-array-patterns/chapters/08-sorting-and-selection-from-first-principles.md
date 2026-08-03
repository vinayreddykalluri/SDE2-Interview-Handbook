# Sorting and Selection from First Principles

Sorting is not one trick. It is a family of trade-offs: extra memory versus in-place work, stable ordering versus arbitrary ordering, guaranteed bounds versus expected bounds, and sorting everything versus selecting one rank. In an interview, the first good sentence is often not code. It is: “What must the output preserve, and what do the constraints allow?”

This chapter starts with mechanics you can see by hand, then builds toward the choices expected of an SDE-2 candidate. The complete, executable implementations are in `ArrayPatternsExamples.java`.

## Choose the contract before the algorithm

Ask these questions:

1. May the input be modified?
2. Must equal keys keep their original relative order?
3. Is the value range small compared with the number of values?
4. Is worst-case latency important, or is expected performance acceptable?
5. Do we need every value sorted, or only the kth value?
6. Can the input contain duplicates, negatives, and integer extremes?

| Need | Useful starting point | Important boundary |
|---|---|---|
| tiny or nearly sorted input | insertion sort | quadratic on reverse order |
| stable, predictable general sort | merge sort | `O(n)` auxiliary storage |
| in-place general sort with many duplicates | three-way quicksort | recursive worst case is still possible |
| dense, explicitly bounded integer range | counting sort | memory is proportional to range width |
| one rank, not complete order | randomized quickselect | expected linear, worst-case quadratic |
| production sort with no implementation requirement | `Arrays.sort` | know its contract; do not pretend you implemented it |

“Best” is meaningless until the contract is stated.

## Insertion sort: the baseline worth knowing

At the start of iteration `i`, positions `[0, i)` are sorted. Save `values[i]`, shift every larger prefix value one slot right, and insert the saved value into the hole.

```text
input:       [5, 2, 4, 2]
insert 2:    [2, 5, 4, 2]
insert 4:    [2, 4, 5, 2]
insert 2:    [2, 2, 4, 5]
                    ^ shift values greater than 2, not values equal to 2
```

The strict `>` comparison is what preserves stability. Changing it to `>=` can reverse equal-key records. Time is `O(n^2)` in the worst case, but `O(n)` when already sorted; space is `O(1)`.

### Interview explanation

> I maintain a sorted prefix. The current value is saved before shifting, so it cannot be overwritten. After the shifts, every prefix value to the left is at most the saved value and every shifted value to the right is greater. That re-establishes the invariant for the next iteration.

## Stable merge sort: equality is a design decision

Merge sort recursively sorts two halves and merges them into a buffer. The merge invariant is:

> Before writing position `w`, the buffer range already written contains the smallest consumed values in sorted order; the next result must be the smaller of the two unconsumed heads.

For records with equal keys, take the left record first:

```text
left:   [(2, first), (5, left)]
right:  [(2, second), (4, right)]

merge:  (2, first) before (2, second)
        because equality chooses the left half
```

The recurrence is `T(n) = 2T(n/2) + O(n)`, giving `O(n log n)` time. The implementation uses one reusable `O(n)` buffer instead of allocating a new array at every merge.

### Failure clinic

- Choosing the right item on equality silently loses stability.
- Copying only part of the merged range leaves stale values.
- Computing the midpoint as `(left + right) / 2` can overflow for theoretical large indexes; use `left + (right - left) / 2`.
- A merge sort over objects must define what happens with `null` elements. The companion rejects a null array and assumes non-null records.

## Three-way quicksort: make duplicates a settled region

A two-way partition can repeatedly revisit values equal to the pivot. Three-way partitioning maintains four regions:

```text
[  < pivot  |  == pivot  |  unknown  |  > pivot  ]
   left..L-1   L..scan-1    scan..U     U+1..right
```

For each unknown value:

- less than pivot: swap with `L`, then advance `L` and `scan`;
- equal to pivot: advance `scan`;
- greater than pivot: swap with `U`, decrement `U`, and inspect the swapped-in value before advancing.

When `scan > U`, every equal value is final. Recurse only on the less-than and greater-than regions.

Dry run around pivot `3`:

```text
[3, 1, 3, 5, 3, 2]
 -> [1, 2 | 3, 3, 3 | 5]
      recurse          recurse
```

Average time is `O(n log n)`, extra array space is `O(1)`, and recursion uses expected `O(log n)` stack space. A consistently poor pivot can still produce `O(n^2)` time and `O(n)` call depth. For adversarial input or strict latency, prefer a guaranteed algorithm or an introspective library sort.

## Counting sort: fast only when the range earns it

If values lie in the declared interval `[minimum, maximum]`, allocate one frequency slot per possible value. A negative value maps through `value - minimum`.

```text
values:       [3, -1, 2, -1]
range:        [-1, 3]
frequencies:  [2, 0, 0, 1, 1]
output:       [-1, -1, 2, 3]
```

Time is `O(n + k)` and space is `O(k)`, where `k = maximum - minimum + 1`. This is excellent when `k` is small and dangerous when values are sparse across a huge range. The companion:

- computes width in `long`, avoiding overflow in `maximum - minimum + 1`;
- rejects reversed and excessively wide ranges before allocating;
- rejects a value outside the caller-declared range; and
- returns a new array, leaving caller input unchanged.

The integer-only version does not need a stability promise because equal integers are indistinguishable. To stably sort records by an integer key, use cumulative counts and place records in output order.

## Iterative randomized quickselect

If the interviewer asks only for the kth-smallest value, fully sorting spends work establishing relations the answer does not need. Quickselect partitions, then continues only in the region containing rank `k`.

The companion contract uses a **zero-based** rank:

```text
sorted values: [1, 3, 3, 7, 8, 9]
k = 0 -> 1
k = 2 -> 3
k = 5 -> 9
```

It clones the input, chooses a pivot with a seeded `Random`, partitions in place, and updates `left` or `right` in a loop. Iteration avoids recursive stack growth. Expected time is `O(n)` because only one partition side continues; worst-case time remains `O(n^2)`. The seed makes tests reproducible, not the algorithm cryptographically random.

The heap volume revisits selection from the top-k perspective and supplies a three-way quickselect that settles duplicate pivots as a range.

## Library solution and first-principles solution

Both belong in a strong interview answer.

| Situation | First-principles answer | Java library answer |
|---|---|---|
| interviewer asks to implement sorting | explain and code the requested invariant | use `Arrays.sort` only as a test oracle |
| sorting is a small step inside a larger problem | mention sorting cost, then use the library | `Arrays.sort(values)` |
| stability of object records matters | stable merge implementation | `Arrays.sort(objects, comparator)` is stable by contract |
| one kth rank in unsorted data | quickselect | Java has no direct primitive-array quickselect API |
| tiny known integer range | guarded counting sort | a library comparison sort is safer if the range is uncertain |

Never hide a sort inside a helper and then claim the surrounding algorithm is linear.

## Edge-case matrix

| Case | Expected handling | Frequent mistake |
|---|---|---|
| empty array | sorting is a no-op; selection rejects it | reading index zero |
| one value | already sorted; only rank zero/one is valid | unnecessary special recursion |
| all equal | three-way partition settles the whole range | two-way partition degrades badly |
| negative values | comparison sorts work; counting uses an offset | indexing counts by the raw value |
| `Integer.MIN_VALUE` / `MAX_VALUE` | compare with `<`, `>`, or `Integer.compare` | subtraction comparator overflows |
| stable records | preserve relative order for equal keys | taking right record on equality |
| huge declared counting range | reject before allocation | memory exhaustion |
| invalid `k` | reject with a precise range contract | returning a sentinel that could be data |
| caller input must be preserved | clone before destructive partitioning | surprising mutation |
| adversarial quicksort/quickselect | state worst case and mitigation | presenting expected bounds as guaranteed |

## Real interview follow-up round

**Interviewer:** Why not always use merge sort?

**Candidate:** It gives a reliable `O(n log n)` bound and stability, but needs auxiliary memory for arrays. If mutation and expected bounds are acceptable, quicksort may use less extra array storage. For a single rank, quickselect avoids sorting everything.

**Interviewer:** Your merge compares equal values with `<`. Is it stable?

**Candidate:** No. With `<`, equality chooses the right half and can move a later record before an earlier one. I need `<=` when choosing from the left half.

**Interviewer:** Counting sort is linear, so is it always faster?

**Candidate:** Its bound is `O(n + k)`, not simply `O(n)`. If the key range is millions wide for a handful of values, allocation and scanning dominate. I would use it only with a trusted dense range.

**Interviewer:** Can you guarantee quickselect is linear?

**Candidate:** Randomized quickselect is expected linear, with quadratic worst case. A deterministic median-of-medians pivot can guarantee linear time but is more complex and rarely the right interview implementation unless worst-case bounds are explicitly required.

**Interviewer:** How would you test these implementations?

**Candidate:** I would cover empty, singleton, sorted, reverse, all-equal, duplicate-heavy, negative, and integer-extreme inputs. Then I would generate random arrays and compare every sorting result with `Arrays.sort`, and every selected rank with the corresponding sorted value. The companion does those differential checks with fixed seeds.

## Run the verified companion

```bash
javac -Xlint:all -Werror ArrayPatternsExamples.java
java ArrayPatternsExamples
```

Expected final line:

```text
PASS 60 Arrays checks
```

Next, use these mechanics inside array patterns rather than sorting by habit. For online range updates, continue to the range-query tree chapter in **DSA 13 — Trees, BSTs, and Tries**. For streaming top-k and heap-based selection, continue to **DSA 14 — Heaps, Priority Queues, and Top-K**.
