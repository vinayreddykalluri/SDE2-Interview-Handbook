# 30. Comparable, Comparator, Sorting, and Selection

## A sort that does nothing, quietly

Someone writes a rule that sounds reasonable: *scores within 10 points of each other count as tied.*

```java
Comparator<Integer> fuzzy = (a, b) ->
        Math.abs(a - b) <= 10 ? 0 : Integer.compare(a, b);

List<Integer> scores = new ArrayList<>(List.of(15, 5, 0));
scores.sort(fuzzy);
System.out.println(scores);      // [15, 5, 0]
```

Nothing moved. The list came out in exactly the order it went in - which happens to be exactly backwards.

![Figure 30.1 - An inconsistent comparator does not throw. It just lies.](assets/diagrams/27-intransitive-comparator.png)

`compare(15, 5)` returns 0 and `compare(5, 0)` returns 0, so the sort sees two ties and no reason to move anything. But `compare(15, 0)` returns `+1` - 15 is supposed to come *after* 0. The comparator contradicts itself, and nothing threw.

There is a lesson inside the lesson. When I first checked this, I compared adjacent pairs of the output and found zero violations, which would have cleared the comparator entirely. The defect only shows up across *non-adjacent* pairs: over 20,000 random 12-element lists, 10,454 outputs contained at least one pair in the wrong order. **The right test for a comparator is all pairs, not neighbours.**

## What a comparator has to promise

Three properties, and the rule people break is almost always the third:

1. **Antisymmetry** - `sgn(compare(a, b)) == -sgn(compare(b, a))`.
2. **Transitivity of order** - if `a < b` and `b < c`, then `a < c`.
3. **Transitivity of equivalence** - if `compare(a, b) == 0`, then for any `c`, `sgn(compare(a, c)) == sgn(compare(b, c))`.

"Within 10" satisfies the first two and fails the third. Whenever you find yourself writing a comparator that treats *approximately* similar things as equal, you are breaking rule 3.

Java also *recommends*, without requiring, that `compare(a, b) == 0` agree with `a.equals(b)`. Chapter 28 shows what a sorted collection does when it does not: it discards elements.

> **Specification boundary:** `List.sort` and `Arrays.sort(Object[])` are specified as **stable**. Primitive overloads are not required to be, because equal primitives have no separately observable identity. `Arrays.sort` may throw `IllegalArgumentException` with "Comparison method violates its general contract!" for an inconsistent comparator - but detection is opportunistic. A sort that completes proves nothing.

## Never write `a - b`

```java
(a, b) -> a.score() - b.score()          // wrong
Comparator.comparingInt(Player::score)   // right
```

The subtraction overflows:

| a | b | `a - b` as `int` | says | truth |
|---:|---:|---:|---|---|
| 2,000,000,000 | -2,000,000,000 | -294,967,296 | a < b | **a > b** |
| 2,147,483,647 | -1 | -2,147,483,648 | a < b | **a > b** |

This is not a corner case. Over 200,000 random `int` pairs, subtraction produced the wrong sign **25.0% of the time**. It only looks safe because most codebases compare small non-negative numbers, right up until one of them is a timestamp delta or a hash.

The same applies to `long` subtraction cast to `int`, and to `(int) (a - b)` in any form. Use `Integer.compare`, `Long.compare`, `Double.compare`, or the `comparingInt` / `comparingLong` / `comparingDouble` factories.

## Chains are cascades

![Figure 30.2 - A comparator chain is a cascade, not a formula](assets/diagrams/26-comparator-chain.png)

```java
static final Comparator<Order> ORDER =
        Comparator.comparingInt(Order::priority)
                  .thenComparing(Order::dueDate)
                  .thenComparing(Order::id);           // unique - ends the chain
```

Each link runs **only if everything before it returned zero**. That makes the last link the one that decides your ties, and it is the one people leave off.

If the chain can still return zero for two different objects:

- their relative order in a sort is arbitrary;
- a `TreeSet` treats them as one element and drops one (Chapter 28);
- a `PriorityQueue` orders them unpredictably (Chapter 29);
- paginated output can repeat or skip rows between pages.

That last one is the expensive one, because it looks like a database bug.

For descending order and nulls, use the combinators rather than hand-rolling:

```java
Comparator.comparing(Order::dueDate).reversed()
Comparator.comparing(Order::assignee, Comparator.nullsLast(String::compareTo))
```

Note that `reversed()` applies to *everything before it* in the chain. `a.thenComparing(b).reversed()` is not `a.thenComparing(b.reversed())`.

## `Comparable` or `Comparator`?

Implement `Comparable` when a type has one obvious, permanent, domain-wide ordering - `String`, `Integer`, `Instant`, an enum. Use a `Comparator` for everything else, which in business code is most things: an `Order` has no single natural ordering, it has a dozen contextual ones.

Two practical rules: define each comparator once as a named constant rather than scattering equivalent lambdas, and keep comparators side-effect-free - parallel sorts will call them from multiple threads.

## Stability, and what it does not give you

A stable sort preserves the input order of elements that compare equal. That lets you sort in passes: sort by secondary key, then stably by primary. A single chain is usually clearer and does less work.

The trap: **stability preserves an order; it does not create one.** If your input came from a `HashMap` or a parallel stream, the encounter order was unspecified to begin with, and stable sorting faithfully preserves an unspecified order. For reproducible pagination and reproducible tests, you need a unique tie-break, not stability.

> **HotSpot note:** current OpenJDK uses TimSort-derived logic for object arrays and lists, dual-pivot quicksort with specialised paths for primitives, and parallel merge strategies above a threshold. TimSort is adaptive - it finds existing ascending and descending runs and merges them, so nearly-sorted input is much faster than the `O(n log n)` bound suggests. Algorithms and thresholds are version-sensitive.

## Do not sort when you only need part of the answer

"Find the 100 largest of ten million" does not require ordering ten million things.

![Figure 30.3 - Three ways to get the top k, and when each one wins](assets/diagrams/28-top-k-strategies.png)

| Approach | Time | Extra space | Use when |
|---|---|---|---|
| sort, then take `k` | `O(n log n)` | `O(n)` | you need the full order anyway |
| bounded heap of size `k` | `O(n log k)` | `O(k)` | `k` is small, or the input streams |
| quickselect | `O(n)` expected, `O(n^2)` worst | `O(1)` extra | you need the `k`-th element, order irrelevant |
| counting / bucket | `O(n + range)` | `O(range)` | keys are integers over a small bounded range |

At n = 10,000,000 and k = 100 that is roughly 24 comparisons per element versus about 7 - and 100 elements resident instead of ten million.

Two traps in the heap version. First, **the comparator is inverted**: to keep the largest `k` you maintain a *min*-heap and evict the smallest of the current best. Second, quickselect *reorders its input*; if the caller still needs the original order, that is a copy you did not budget for.

## Binary search has a precondition and an encoding

`Collections.binarySearch` and `Arrays.binarySearch` require data already sorted **by the same ordering** you pass in. Violate that and the result is meaningless rather than merely wrong - no exception.

```java
int found = Collections.binarySearch(events, probe, ORDER);
int insertionPoint = found >= 0 ? found : -found - 1;
```

The negative encoding exists so a caller can tell "found at index 0" from "not found, belongs at index 0". And with duplicates, nothing promises *which* equal element you get - for ranges of duplicates, write explicit lower-bound and upper-bound searches.

## Worked example: top-k with bounded memory

```java
import java.util.*;

record Candidate(String id, int score, long submittedAt) { }

final class Leaderboard {
    private static final Comparator<Candidate> BEST_FIRST =
            Comparator.comparingInt(Candidate::score).reversed()
                      .thenComparingLong(Candidate::submittedAt)
                      .thenComparing(Candidate::id);        // unique tie-break

    static List<Candidate> top(Collection<Candidate> all, int k) {
        if (k <= 0) {
            return List.of();
        }
        // min-heap under BEST_FIRST: its head is the WORST of the current best k
        PriorityQueue<Candidate> best = new PriorityQueue<>(BEST_FIRST.reversed());
        for (Candidate candidate : all) {
            best.offer(candidate);
            if (best.size() > k) {
                best.poll();                 // evict the current worst
            }
        }
        List<Candidate> result = new ArrayList<>(best);
        result.sort(BEST_FIRST);             // the heap is not sorted (Chapter 29)
        return List.copyOf(result);
    }
}
```

Every line of the comparator earns its place: `score` descending is the business rule, `submittedAt` breaks score ties in favour of the earlier submission, and `id` guarantees a total order so that two identical scores at the identical millisecond still paginate reproducibly.

The final `result.sort(...)` is not redundant. Draining a heap yields sorted output, but *copying* one into a list yields array order - the exact confusion Chapter 29 opens with.

## Complexity

| Operation | Cost | Notes |
|---|---|---|
| `List.sort` / `Arrays.sort(Object[])` | `O(n log n)`, stable | needs temporary storage for merging |
| `Arrays.sort(int[])` | `O(n log n)` typical, not stable | no boxing, far less memory |
| `Arrays.parallelSort` | `O(n log n)` work | uses the common pool; overhead can dominate small arrays |
| binary search | `O(log n)` | **requires** matching sorted order |
| bounded heap top-k | `O(n log k)` | `O(k)` space |
| quickselect | `O(n)` expected | `O(n^2)` worst without a good pivot; mutates input |

Comparison cost multiplies through all of it. A chain of four comparators on long strings sharing a prefix is a real cost that no complexity column shows.

## Edge cases and common mistakes

- A comparator that treats "close enough" as equal, breaking transitivity of equivalence.
- Testing comparator correctness on adjacent pairs only.
- `a - b` in any form, including `(int) (longA - longB)`.
- Leaving off the final unique tie-break.
- `a.thenComparing(b).reversed()` when you meant to reverse only `b`.
- Relying on stability to produce determinism from an unordered source.
- Assuming a sort that completed proves the comparator is consistent.
- Binary searching with an ordering different from the one used to sort.
- Reading a negative `binarySearch` result as an index.
- Assuming which duplicate `binarySearch` returns.
- Sorting everything when only `k` items are needed.
- Forgetting to invert the comparator in a bounded-heap top-k.
- Forgetting that quickselect mutates the input.
- Sorting an unmodifiable list.
- Comparators with side effects, then reaching for `parallelSort`.

## Production engineering notes

Define comparators once, name them, and document the tie-break. `ORDER` as a `static final` constant beside the type it orders is reviewable; six inline lambdas are not.

Translate a product rule into a *total order* deliberately. "Most relevant first" is not an ordering until you have said what happens on a tie, and the answer must be something unique and stable.

Any endpoint with pagination needs a deterministic total order, or pages will overlap and drop rows. This is one of the most common production sorting bugs and it is invisible in a single-page test.

Sort primitives as primitives. `Arrays.sort(int[])` avoids `n` boxed objects, which usually matters more than the algorithm.

Measure before reaching for `parallelSort`: it uses the common fork-join pool, which you may be sharing with everything else in the process.

## Interview questions and model answers

**What contract does a comparator have to satisfy?**

Antisymmetry, transitivity of the ordering, and transitivity of equivalence - if two things compare equal, they must compare identically against everything else. The third is the one that "treat values within a tolerance as equal" rules break.

**What happens if a comparator is inconsistent?**

Undefined ordering. A sort may throw `IllegalArgumentException`, or may quietly return a wrong order - sorting `[15, 5, 0]` with a within-10 comparator returns it unchanged. Detection is opportunistic, so completing successfully proves nothing.

**Why not `a - b`?**

It overflows. `2147483647 - (-1)` is `-2147483648`, so the comparator claims the larger value is smaller. Measured across random `int` pairs, the sign is wrong 25% of the time. Use `Integer.compare`.

**What does a stable sort guarantee, and what does it not?**

It preserves the relative order of elements that compare equal. It does not manufacture determinism: if the input order was unspecified - from a `HashMap`, say - stability preserves an unspecified order. Determinism needs a unique tie-break.

**How would you find the top 100 of ten million?**

A min-heap of size 100 under the inverted comparator: offer each element, poll when size exceeds 100. `O(n log k)` and `O(k)` space instead of `O(n log n)` and `O(n)`. If I needed only the 100th value and not the order, quickselect in expected linear time - noting it reorders the input.

**`binarySearch` returned -4. What does that mean?**

Not found; the insertion point is `-(-4) - 1 = 3`. The encoding exists so "found at index 0" is distinguishable from "belongs at index 0".

**Why does pagination break without a unique tie-break?**

Equal-comparing rows may be ordered differently between two queries, so a row can appear on page one and page two, or on neither. Stability does not help - it preserves an order that was never determined.

## Exercises

1. Run the within-10 comparator on `[15, 5, 0]`. Confirm the output, then find the three values that prove the equivalence is not transitive.
2. Write a checker that verifies a sorted output over *all* pairs, not adjacent ones. Run it on the fuzzy comparator over random lists and report the violation rate.
3. Find two `int` values where `a - b` gives the wrong sign, then estimate the failure rate over random pairs by simulation.
4. Build a four-link comparator chain, then remove the final unique link. Sort the same data twice from two different input orders and diff the results.
5. Predict, then check, the difference between `a.thenComparing(b).reversed()` and `a.thenComparing(b.reversed())`.
6. Implement top-k with a bounded heap and with a full sort. Count comparisons at n = 1,000,000 for k = 10, 100, and 10,000, and find where the two cross over.
7. Implement quickselect and verify it against a full sort over a few thousand random arrays. Then show that it reordered its input.
8. Binary search a list sorted by a *different* comparator. Record the result and explain why no exception is thrown.

## Chapter summary

A comparator is a claim about a total order, and Java trusts it. Break transitivity of equivalence - which every "close enough counts as equal" rule does - and a sort will quietly return the wrong answer without throwing; `[15, 5, 0]` sorted by a within-10 comparator comes back unchanged. Test comparators over all pairs, because adjacent pairs will not reveal it. Never subtract: overflow gives the wrong sign for a quarter of random `int` pairs. Build orderings as named, reusable chains where each link runs only on a tie, and always end the chain on something unique - the missing final tie-break is what silently drops elements from a `TreeSet`, scrambles a `PriorityQueue`, and makes paginated endpoints repeat and skip rows. Stability preserves an order rather than creating one, so it cannot rescue determinism from an unordered source. And when you need only part of the answer, do not compute all of it: a bounded heap of size `k` is `O(n log k)` in `O(k)` space, and quickselect is expected linear if you only need the `k`-th element and can afford to reorder the input.

## Revision checklist

- [ ] I can state all three comparator properties and name which one tolerance rules break.
- [ ] I test comparators over all pairs, not adjacent ones.
- [ ] I never write `a - b`, in any width or cast.
- [ ] Every comparator I write ends on a unique, stable tie-break.
- [ ] I know `reversed()` applies to the whole chain before it.
- [ ] I know stability preserves an order and cannot create one.
- [ ] I can choose between full sort, bounded heap, quickselect, and counting - and justify it with `n` and `k`.
- [ ] I remember the bounded heap uses the inverted comparator.
- [ ] I can decode a negative `binarySearch` result and state its precondition.
