# 30. Comparable, Comparator, Sorting, and Selection

## Learning objectives

By the end of this chapter, you should be able to:

- define natural and external orderings with valid comparison contracts;
- compose null-safe, deterministic comparators without overflow;
- explain stable sorting, in-place sorting, and comparison lower bounds;
- choose among object sort, primitive sort, parallel sort, heap selection, and quickselect;
- use binary search only under its ordering precondition; and
- discuss current OpenJDK algorithms without confusing them with API guarantees.

## Why this matters at SDE-2

Sorting is often the boundary between raw data and useful output: ranked results, merge pipelines, pagination, reconciliation, and deduplication all depend on ordering. Comparison defects can be intermittent and severe. A non-transitive comparator may make sorting fail at runtime, corrupt a tree collection's model, or create unstable pagination.

At SDE-2, you should translate a product rule into a total order, handle ties explicitly, and select an algorithm based on whether all values or only the top `k` are needed. You should distinguish specification promises, such as stability for an object sort, from OpenJDK's current TimSort or primitive-array algorithms.

## First-principles model

An ordering comparator maps two values to a negative number, zero, or a positive number:

```text
compare(a, b) < 0  means a precedes b
compare(a, b) = 0  means a and b are ordering-equivalent
compare(a, b) > 0  means a follows b
```

The magnitude is irrelevant. A comparator must behave like a total order over the accepted domain. Important laws are:

- sign symmetry: `sign(compare(a,b)) == -sign(compare(b,a))`;
- transitivity: if `a > b` and `b > c`, then `a > c`;
- zero consistency: if `a` compares equal to `b`, comparisons of each against `c` have the same sign; and
- repeatability while participating fields remain unchanged.

Comparison sorting uses pairwise questions to distinguish possible permutations. There are `n!` permutations, so a decision tree requires height `Omega(log(n!))`, which is `Omega(n log n)`. General comparison sorting cannot asymptotically beat that bound. Algorithms using restricted keys, such as counting sort, operate under different assumptions.

> **Specification boundary:** `Comparable` and `Comparator` define ordering contracts. Sorting APIs document properties such as stability and permitted mutation. Their exact algorithm, run detection, pivot selection, temporary storage, and thresholds can vary by JDK implementation and version.

## Core terminology

- **Natural order:** A type's canonical order implemented by `Comparable`.
- **External order:** A purpose-specific `Comparator` supplied by a client.
- **Stable sort:** Equal-order elements retain input relative order.
- **In-place:** Uses only bounded or logarithmic auxiliary storage under a stated model; common library documentation may use looser wording.
- **Adaptive sort:** Exploits existing order or runs in the input.
- **Total order:** Consistent ordering for every accepted pair.
- **Partial selection:** Find a rank or top subset without fully sorting.
- **Quickselect:** Partition-based expected linear-time selection.
- **Partition:** Rearrange elements around a pivot by comparison.
- **Tie-breaker:** Additional field distinguishing otherwise equal rankings.
- **Schwartzian transform/decorate-sort-undecorate:** Precompute expensive sort keys, sort decorated values, then extract originals.

## Detailed mechanics

### Comparable versus Comparator

Implement `Comparable<T>` when a type has one unsurprising natural order used broadly:

```java
record Version(int major, int minor, int patch)
        implements Comparable<Version> {
    @Override
    public int compareTo(Version other) {
        int result = Integer.compare(major, other.major);
        if (result != 0) return result;
        result = Integer.compare(minor, other.minor);
        if (result != 0) return result;
        return Integer.compare(patch, other.patch);
    }
}
```

Use comparators for alternative views: orders by creation time, priority, customer, or amount. A natural order becomes part of a public type's long-lived meaning, so do not add one merely for a single screen.

Natural ordering should usually be consistent with `equals`, especially when values enter sorted sets or maps. `BigDecimal` is a notable counterexample: values such as `1.0` and `1.00` compare as zero but are not equal because scale affects `equals`. A `TreeSet<BigDecimal>` can therefore have different uniqueness behavior from a `HashSet<BigDecimal>`.

### Comparator composition

Comparator factories make intent explicit:

```java
Comparator<Order> order = Comparator
        .comparingInt(Order::priority).reversed()
        .thenComparing(Order::createdAt)
        .thenComparing(Order::id);
```

Apply reversal carefully. Calling `reversed()` at the end reverses the entire chain. Calling it after the primary comparator reverses only that comparator before adding subsequent ascending tie-breakers.

Use primitive factories to avoid boxing in comparison: `comparingInt`, `comparingLong`, and `comparingDouble`. Use `Comparator.nullsFirst` or `nullsLast` only if null is valid domain data. Do not compare integers with `a - b`; overflow can violate the sign rule. Floats and doubles also require library comparison because NaN and signed zero need a consistent total order.

### Stability and tie-breaking

Stable sort preserves input order when comparator returns zero. This permits multi-pass sorting: stable-sort by a secondary key, then by primary key. A single comparator chain is usually clearer and performs fewer sorts.

Stability is not a substitute for a deterministic total presentation order. If input arrives from a hash map or concurrent source with unspecified encounter order, stable sorting equal ranks preserves an unspecified order. Add a unique immutable ID as a tie-breaker for reproducible pagination and tests.

### Library sorting APIs

`List.sort(comparator)` mutates the list and is specified as stable. `Collections.sort` delegates to the list's sorting facility in modern APIs. The list must support the required replacement operation; an unmodifiable list rejects sorting.

`Arrays.sort(Object[])` is stable. Primitive overloads are not required to be stable because primitive values have no separately observable identity when equal. Primitive sorts avoid boxing and are normally much more memory-efficient.

`Arrays.parallelSort` may use the common fork-join pool and temporary storage. Parallel overhead can outweigh gains for small arrays or contended services. Measure on the deployment shape, and remember that comparator work must be thread-safe and side-effect-free.

> **HotSpot note:** Current OpenJDK releases commonly use TimSort-derived logic for object arrays and lists, dual-pivot quicksort and specialized paths for several primitive types, and parallel merge/sort strategies above thresholds. Algorithms and thresholds are version-sensitive.

### TimSort intuition and comparator failures

TimSort is adaptive: it discovers ascending or descending runs, extends short runs, and merges runs while maintaining stack invariants. Nearly sorted input can therefore be efficient. It needs temporary storage for merging.

Inconsistent comparators can trigger exceptions such as "comparison method violates its general contract" in some sort paths, but detection is not guaranteed. A sort that appears to complete does not prove comparator correctness.

### Selection instead of full sorting

If only the smallest or largest `k` items are needed:

- sort all: `O(n log n)` time, straightforward, produces complete order;
- maintain a heap of size `k`: `O(n log k)` time and `O(k)` extra space;
- quickselect: expected `O(n)` to place the kth rank, then optionally sort the selected `k`; worst case `O(n^2)` without robust pivot strategy;
- counting/bucket techniques: potentially `O(n + range)` when integer key range is small and bounded.

Quickselect partitions around a pivot. After partition, if the pivot lands at rank `k`, selection is complete; otherwise recurse or iterate only on the side containing rank `k`. It mutates the array unless implemented on a copy.

### Binary search

`Collections.binarySearch` and `Arrays.binarySearch` require data sorted according to the same ordering. A nonnegative result is a matching index. A negative result encodes insertion point `-(result) - 1`. When duplicates exist, no promise should be inferred about which equal occurrence is returned unless the API states one. Use explicit lower-bound and upper-bound searches for ranges of duplicates.

## Worked Java example

This method returns the top `k` candidates while bounding intermediate storage:

```java
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.PriorityQueue;

record Candidate(String id, int score, long completedAtEpochMillis) {}

final class Ranking {
    // Best first: higher score, earlier completion, lexicographically smaller ID.
    private static final Comparator<Candidate> BEST_FIRST = Comparator
            .comparingInt(Candidate::score).reversed()
            .thenComparingLong(Candidate::completedAtEpochMillis)
            .thenComparing(Candidate::id);

    static List<Candidate> topK(Collection<Candidate> input, int k) {
        if (k < 0) throw new IllegalArgumentException("k must be non-negative");
        if (k == 0) return List.of();

        // Worst retained candidate is at the head.
        PriorityQueue<Candidate> retained =
                new PriorityQueue<>(k, BEST_FIRST.reversed());

        for (Candidate candidate : input) {
            if (candidate == null) throw new IllegalArgumentException("null candidate");
            if (retained.size() < k) {
                retained.offer(candidate);
            } else if (BEST_FIRST.compare(candidate, retained.peek()) < 0) {
                retained.poll();
                retained.offer(candidate);
            }
        }

        ArrayList<Candidate> result = new ArrayList<>(retained);
        result.sort(BEST_FIRST);
        return List.copyOf(result);
    }
}
```

The ID tie-breaker gives a deterministic total ranking. If IDs are unique, no two distinct candidates compare as zero. The heap uses reverse order so its head is the worst candidate currently retained; a better incoming candidate replaces it.

## Execution or memory walkthrough

For `k = 3` and candidates with scores `70/A`, `90/B`, `80/C`, `85/D`, and `80/E`, assume times and IDs do not change the score comparisons:

1. Retain A, B, C. The heap head is A, the worst of those three.
2. D outranks A, so poll A and add D. Retained set is B, C, D; C is now worst.
3. E ties C on score. Completion time and then ID decide whether E is better. Replace only if the full comparator ranks E before C.
4. Copy the heap. Its iteration order is not ranked.
5. Sort the copy best-first and publish an unmodifiable result.

At most `k` candidate references reside in the heap, plus the final list of at most `k` references. Candidate records are shared rather than copied. Comparator calls extract primitive fields without boxing for score and time.

Quickselect would store the entire mutable input or a copy but could reduce asymptotic selection time. The heap supports one-pass input and is appropriate when the collection is streamed or `k` is small.

## Complexity and performance

For `topK`, each of `n` inputs performs constant comparison plus at most one heap replacement costing `O(log k)`. Time is `O(n log k + k log k)` and auxiliary reference storage is `O(k)`. When `k >= n`, the behavior approaches `O(n log n)` and sorting all may be simpler and faster.

Common sorting bounds:

| Technique | Time | Extra space | Stable | Best use |
|---|---:|---:|---|---|
| comparison sort | `O(n log n)` typical/guaranteed by chosen algorithm | varies | API-dependent | full order |
| insertion sort | `O(n^2)`, near `O(n)` when nearly sorted | `O(1)` | yes when implemented conventionally | tiny/nearly sorted ranges |
| merge sort | `O(n log n)` | `O(n)` | yes | stable predictable sorting |
| heap sort | `O(n log n)` | `O(1)` array model | no | worst-case bound, low extra space |
| quicksort | average `O(n log n)`, worst `O(n^2)` | recursion/stack varies | usually no | fast primitive/in-place-style sorting |
| heap top-k | `O(n log k)` | `O(k)` | only with explicit tie order | small top subset |
| quickselect | expected `O(n)`, worst `O(n^2)` | often `O(1)` iterative | no | one rank or unsorted partition |

Comparator cost can dominate `n log n`. Avoid network calls, database access, mutable clocks, locale creation, or repeated expensive parsing inside comparison. Precompute keys when profiling justifies it.

## Edge cases and common mistakes

- Implementing compare with subtraction and overflowing.
- Omitting a stable tie-breaker for pagination or distributed merge results.
- Assuming stable sort makes unspecified input order deterministic.
- Using a comparator that changes based on mutable state, current time, or side effects.
- Mixing natural and external ordering between sort and binary search.
- Expecting binary search to return the first duplicate.
- Sorting an unmodifiable or fixed-capability list without understanding supported operations.
- Mutating comparator fields while objects reside in a sorted collection.
- Assuming primitive and object sort stability are identical.
- Parallelizing a small sort or using a non-thread-safe comparator with parallel sort.
- Fully sorting millions of items to return a tiny top-k result.
- Forgetting that `reversed()` placement can reverse an entire comparator chain.
- Treating current OpenJDK algorithm names or thresholds as portable guarantees.

## Production engineering notes

Define ordering once and reuse it across database queries, in-memory merge, API pagination, and tests. Any mismatch can cause duplicates or gaps between pages. Cursor pagination needs a unique final tie-breaker included in both ordering and cursor encoding.

Comparator functions should be pure, cheap, null-explicit, and tested with property-style checks for symmetry and transitivity. Normalize text before sorting if case, Unicode, or locale rules matter. Human-language collation is not equivalent to `String` code-unit order and may depend on locale/version; isolate it from identity ordering.

Avoid sorting shared mutable lists in place. Copy, sort, and publish when readers need snapshots. For large results, push sorting and limiting toward an indexed data source when possible. Enforce input caps for user-controlled sorts to prevent CPU and memory abuse.

Measure sequential versus parallel sorting in realistic service contention. A common pool is shared process capacity, not free compute. Primitive arrays avoid wrapper allocation and should be preferred for numerical kernels when the surrounding design permits them.

## Interview questions and model answers

**When should a class implement `Comparable`?**

When it has one canonical, unsurprising natural order that is meaningful across uses. Purpose-specific orders belong in named comparators.

**What properties must a comparator satisfy?**

It must provide consistent sign symmetry, transitivity, and zero behavior over its accepted domain. It should be repeatable while compared state is unchanged and preferably consistent with equals for collection use.

**What does stable sort mean?**

Elements that compare as equal retain their original relative order. It does not create determinism if original encounter order is unspecified.

**How would you find the top 100 of ten million values?**

Use a size-100 min-heap while scanning: `O(n log 100)` time and `O(100)` memory, then sort the retained items for final output. Discuss database pushdown or distributed merge if data is external.

**Why can quickselect be faster than sorting?**

It explores only the partition containing the target rank, giving expected linear work. It does not fully order the data and has quadratic worst cases without pivot safeguards.

**Can I rely on TimSort for every Java object sort?**

Rely on the documented stability and behavior of the API, not an algorithm name. TimSort is a common current OpenJDK implementation and can change.

## Exercises

1. Write a comparator for nullable invoices ordered by status, descending amount, due date, and unique ID. State null policy.
2. Produce a three-value counterexample showing how a bad cyclic comparator violates transitivity.
3. Implement lower-bound binary search that returns the first index whose element is not less than a target.
4. Compare full sort, heap top-k, and quickselect for `n = 1,000,000` and `k = 10`, then for `k = 900,000`.
5. Repair a comparator written as `(a, b) -> a.timestamp() > b.timestamp() ? 1 : -1`.
6. Explain why stable sorting entries from a `HashMap` by value alone can still produce changing output.

## Chapter summary

Ordering is a correctness contract before it is an algorithm. `Comparable` defines a canonical natural order; `Comparator` defines external orders that can be composed with safe tie-breakers. Stable full sorting solves complete ordering in `O(n log n)`, while heaps and selection algorithms avoid unnecessary work for small subsets. Java APIs specify observable properties, whereas TimSort, primitive quicksort variants, and thresholds are current implementation choices. Production ordering must align across layers and remain deterministic, pure, and bounded.

## Revision checklist

- [ ] I can state comparator laws and consistency-with-equals concerns.
- [ ] I use safe primitive comparisons rather than subtraction.
- [ ] I understand reversal placement and comparator chaining.
- [ ] I distinguish stability from deterministic total ordering.
- [ ] I know object, primitive, and parallel sort trade-offs.
- [ ] I can choose full sort, heap top-k, or quickselect from requirements.
- [ ] I use binary search only with the same ordering used for sorting.
- [ ] I label OpenJDK sorting algorithms and thresholds as version-sensitive.
