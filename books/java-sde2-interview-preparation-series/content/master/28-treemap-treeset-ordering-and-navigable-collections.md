# 28. TreeMap, TreeSet, Ordering, and Navigable Collections

## Two elements go in. One comes out.

```java
Set<BigDecimal> hashed = new HashSet<>();
hashed.add(new BigDecimal("1.0"));
hashed.add(new BigDecimal("1.00"));
hashed.size();                      // 2

Set<BigDecimal> sorted = new TreeSet<>();
sorted.add(new BigDecimal("1.0"));
sorted.add(new BigDecimal("1.00"));
sorted.size();                      // 1
```

Neither set is broken. They are answering different questions.

![Figure 28.1 - A sorted set does not use equals](assets/diagrams/22-compareto-vs-equals.png)

`HashSet` asks `equals`, and `BigDecimal.equals` compares unscaled value *and* scale, so `1.0` and `1.00` are different objects. `TreeSet` asks `compareTo`, which compares numeric value only, so the second `add` is a duplicate and is silently dropped.

**Inside a tree collection, "the same" means "compares to zero."** `equals` is never consulted. The Javadoc says so explicitly, describing `SortedSet` and `SortedMap` as behaving inconsistently with `Set` and `Map` when the ordering is not consistent with equals. It is a documented consequence of having two notions of sameness, not a defect.

The production version of this is worse than `BigDecimal`, because it is silent:

```java
Comparator<Person> byAge = Comparator.comparingInt(Person::age);
TreeSet<Person> people = new TreeSet<>(byAge);
// every 34-year-old after the first is discarded
```

> **Specification boundary:** that a sorted collection uses comparison rather than `equals` is a
> documented contract, not an implementation detail - `SortedSet` and `SortedMap` explicitly describe
> themselves as behaving inconsistently with `Set` and `Map` when the ordering disagrees with equals.
> The red-black tree underneath is implementation; the `O(log n)` bounds are documented by `TreeMap`.

The fix is a rule, not a special case: **end every comparator used by a sorted collection on something unique.**

```java
Comparator<Person> byAgeThenId = Comparator
        .comparingInt(Person::age)
        .thenComparing(Person::id);
```

The tie-break must reflect the identity you mean, and must stay stable while the element is stored - the same constraint a `HashMap` key has, for the same reason.

## What you buy for the log factor

A `HashMap` answers exactly one question: is this key present? It has no cheap way to tell you the *nearest* key, the smallest key, or every key in a range. A `TreeMap` answers all of those on the same structure.

![Figure 28.2 - One tree, six navigation questions](assets/diagrams/21-navigable-map.png)

```java
NavigableMap<Integer, String> m = new TreeMap<>(Map.of(
        10, "a", 20, "b", 30, "c", 40, "d", 50, "e", 60, "f", 70, "g"));

m.floorKey(35)      // 30 - greatest key <= 35
m.ceilingKey(35)    // 40 - least key >= 35
m.lowerKey(30)      // 20 - strictly less
m.higherKey(30)     // 40 - strictly greater
m.headMap(40)       // {10, 20, 30}
m.subMap(20, 50)    // {20, 30, 40} - half-open, like everything else
m.firstEntry()      // 10=a
m.pollLastEntry()   // 70=g, and removes it
```

The naming is worth internalising because it is uniform: **`floor`/`ceiling` include the key; `lower`/`higher` are strict.** These methods exist to delete the off-by-one code people otherwise write by hand, and that is most of their value.

If you need any of these, the `O(log n)` is not a cost you are paying - it is the feature you are buying.

## Range views are live, and bounded

`subMap`, `headMap`, and `tailMap` return views backed by the map, not copies:

```java
map.subMap(from, true, to, false);   // [from, to) - explicit endpoints
map.subMap(from, to);                // same thing, implicit
```

Two behaviours follow:

- Mutating the view mutates the map. `map.subMap(a, b).clear()` deletes that whole range.
- Inserting a key *outside* the view's range throws `IllegalArgumentException`. The view knows its bounds.

`descendingMap` and `descendingSet` are also views - reverse-order windows, not reversed copies. Reversing twice gives you a view equivalent to the original.

For a stable snapshot you must copy: into another `TreeMap` if you need to keep the comparator and the navigation, or into a `List` if you only need the current ordered sequence.

## Inside: a balanced tree, and why balance matters

Lookup starts at the root and descends: negative goes left, positive goes right, zero is a hit. Insertion follows the same path and attaches a node - and then rebalances, because an unbalanced tree is a linked list with extra steps.

```text
      C                 B
     /                 / \
    B       ->        A   C
   /
  A
```

A rotation relinks parent and child without changing in-order traversal, which is exactly why it is safe. Removal has three cases: a leaf is detached; a node with one child is replaced by that child; a node with two children is logically replaced by its successor, reducing the problem to one of the simpler cases. Balance is repaired afterwards.

> **HotSpot note:** current OpenJDK `TreeMap` uses a red-black tree with parent-linked entry nodes. The deletion strategy, colour representation, and helper methods are version-sensitive. `TreeSet` is backed by a `NavigableMap` holding a marker value - a familiar design, not a requirement of the specification.

## Natural order, nulls, and mixed types

Natural ordering needs keys that implement mutually compatible `Comparable`. Two consequences catch people:

- A natural-order `TreeMap` **rejects null keys** - null cannot be compared. (`HashMap` allows one.) A custom comparator may define null placement via `Comparator.nullsFirst` or `nullsLast`, though a null key usually signals a modelling problem.
- Mixing unrelated key types throws `ClassCastException` at insert time, not at compile time.

And never write a subtraction comparator:

```java
(a, b) -> a.score() - b.score()      // wrong: overflow reverses the sign
Comparator.comparingInt(Player::score)   // correct
```

Chapter 30 measures how often that overflow actually bites. The short version: at 25% of random `int` pairs, it is not a corner case.

## Worked example: which configuration was in effect?

"What was the effective config at time *t*" is a `floorEntry` query, and it is the canonical reason to reach for a `TreeMap`.

```java
import java.time.Instant;
import java.util.*;

final class ConfigHistory {
    private final NavigableMap<Instant, Config> byEffectiveFrom = new TreeMap<>();

    void publish(Instant effectiveFrom, Config config) {
        byEffectiveFrom.put(Objects.requireNonNull(effectiveFrom),
                            Objects.requireNonNull(config));
    }

    Optional<Config> effectiveAt(Instant when) {
        Map.Entry<Instant, Config> entry = byEffectiveFrom.floorEntry(when);
        return Optional.ofNullable(entry).map(Map.Entry::getValue);
    }

    List<Config> publishedBetween(Instant fromInclusive, Instant toExclusive) {
        return List.copyOf(
                byEffectiveFrom.subMap(fromInclusive, true, toExclusive, false)
                               .values());
    }
}
```

The alternative with a `HashMap` is to keep a separate sorted list of timestamps and binary-search it, then look up the map - two structures to keep consistent, and a class of bug that does not exist here.

Note `List.copyOf` on the range: `values()` of a `subMap` is a view of a view. Returning it would hand callers something that changes under them and retains the entire history.

Also note that `floorEntry` returns a **snapshot** entry. Unlike an entry obtained from `entrySet()` iteration, calling `setValue` on it is not a route into the map.

## Trace

`byEffectiveFrom` holds `09:00`, `12:00`, `18:00`. Query `effectiveAt(14:30)`:

```text
root 12:00     14:30 > 12:00  -> go right
node 18:00     14:30 < 18:00  -> go left, remember 12:00 as best-so-far
null           stop; answer = 12:00
```

Three comparisons, no scan. The "remember the last left-turn ancestor" step is the whole of `floor` - it is worth being able to describe, because interviewers ask how `floorKey` is implemented rather than what it returns.

## Complexity

| Operation | `TreeMap` / `TreeSet` | `HashMap` / `HashSet` |
|---|---|---|
| `get`, `put`, `remove`, `contains` | `O(log n)` | expected `O(1)` |
| `first`, `last`, `floor`, `ceiling`, `higher`, `lower` | `O(log n)` | not supported |
| range view construction | `O(1)` - it is a view | not supported |
| iterating a range of size `k` | `O(log n + k)` | not supported |
| full iteration | `O(n)`, **in order** | `O(capacity + size)`, unordered |

Memory is a node per entry with key, value, colour, and three links. Comparison cost is part of every operation, so an expensive comparator multiplies the whole structure's cost - comparing long strings that share a prefix is the usual culprit.

## Edge cases and common mistakes

- Assuming `TreeSet` and `TreeMap` use `equals`. They use comparison.
- A comparator without a unique final tie-break, silently discarding elements.
- `BigDecimal` in a `TreeSet` when scale differences are meaningful.
- Putting a null key into a natural-order tree collection.
- Mixing key types and discovering it as a runtime `ClassCastException`.
- Subtraction comparators.
- Inserting outside a range view's bounds and being surprised by `IllegalArgumentException`.
- Returning a range view or `descendingMap` as if it were an independent result - it is live and retains the parent.
- Mutating a field the comparator reads while the element is stored.
- Expecting `firstEntry` to remove; that is `pollFirstEntry`.
- Calling `setValue` on a navigation-returned entry and expecting the map to change.
- Reaching for a tree when a `HashMap` plus one sort at the end would do.

## Production engineering notes

Choose `TreeMap` when the *queries* are ordered: nearest-match, ranges, "latest before", top-of-range, or in-order iteration. Choose `HashMap` when they are exact-match. Do not pay `O(log n)` on every operation for an ordering you only need once at the end - sort then.

Define the comparator once, as a named constant, and give it a documented final tie-break. A comparator scattered as an inline lambda across a codebase will diverge.

Range views are excellent for bounded deletion - expiring everything before a cutoff is `map.headMap(cutoff).clear()`, which is one call and no iteration by hand.

Tree collections are not thread-safe and their iterators are fail-fast on a best-effort basis. For concurrent ordered access use `ConcurrentSkipListMap`, which provides the same navigation contract with concurrent semantics - and which exists precisely because a concurrent balanced tree is much harder to build than a concurrent skip list.

## Interview questions and model answers

**How does `TreeSet` decide two elements are duplicates?**

By comparison returning zero, not by `equals`. That is why `new TreeSet<BigDecimal>()` treats `1.0` and `1.00` as one element while `HashSet` keeps both, and why a comparator that only compares one field silently deduplicates.

**When would you choose `TreeMap` over `HashMap`?**

When the questions are ordered: nearest key, range, first or last, or in-order traversal. `HashMap` cannot answer any of those cheaply. If I only need order once at the end, I use `HashMap` and sort.

**What is the difference between `floor` and `lower`?**

`floor` is inclusive - the greatest key less than *or equal to* the argument. `lower` is strict. Same relationship between `ceiling` and `higher`.

**Is `subMap` a copy?**

No, it is a live view with bounds. Mutating it mutates the map, and inserting outside its range throws `IllegalArgumentException`. It also retains the whole parent map.

**How is `floorKey` implemented?**

Descend from the root; on each right turn, record the current node as the best candidate so far, because everything on a right turn is a smaller key that is still a candidate. When you fall off the tree, the recorded candidate is the answer. `O(log n)`.

**Why does a natural-order `TreeMap` reject null keys when `HashMap` accepts one?**

Because it must compare every key it stores, and null cannot be compared. A `HashMap` only has to hash, and it special-cases null.

## Exercises

1. Reproduce the `BigDecimal` result in both `HashSet` and `TreeSet`. Then explain which of the two matches the intent of a money-deduplication task, and defend it.
2. Build a `TreeSet<Person>` with an age-only comparator and insert three people aged 34. Report the size, then fix it with a tie-break and report it again.
3. For `{10, 20, 30}`, state `lower(20)`, `floor(20)`, `ceiling(20)`, and `higher(20)` from memory. Then check.
4. Implement `floorKey` yourself against a plain BST and verify it against `TreeMap` over a few hundred random trees and queries.
5. Delete every entry before a cutoff using a `headMap` view, then do it with an explicit iterator. Compare the code and say which you would review more carefully.
6. Attempt to insert a key outside a `subMap`'s bounds. Record the exception and explain why it is preferable to silently widening the view.
7. Replace a `TreeMap` in a piece of code with a `HashMap` plus a final sort. Say which workloads that improves and which it makes worse.

## Chapter summary

Inside a sorted collection, identity is comparison: two keys are the same when `compareTo` or the comparator returns zero, and `equals` is never asked. That is why a `TreeSet` keeps one `BigDecimal` where a `HashSet` keeps two, and why an age-only comparator quietly discards people - so every comparator handed to a sorted collection needs a unique, stable final tie-break. What you buy in exchange for `O(log n)` is the family of questions a hash table cannot answer at all: `floor` and `ceiling` (inclusive), `lower` and `higher` (strict), first and last, and half-open ranges. `subMap`, `headMap`, `tailMap`, and `descendingMap` are live bounded views, so they write through, reject out-of-range inserts, and retain their parent. Natural ordering rejects nulls and throws on mixed types at runtime, comparison cost multiplies through every operation, and for concurrent ordered access the answer is `ConcurrentSkipListMap` rather than a lock around a `TreeMap`.

## Revision checklist

- [ ] I know tree collections decide duplicates by comparison, never by `equals`.
- [ ] Every comparator I hand a sorted collection ends on something unique and stable.
- [ ] I can state `floor`, `ceiling`, `lower`, and `higher` without hesitating over inclusivity.
- [ ] I can describe how `floorKey` is implemented, not just what it returns.
- [ ] I know range and descending views are live, bounded, and retain the parent.
- [ ] I know a natural-order tree rejects null keys and why `HashMap` does not.
- [ ] I never write a subtraction comparator.
- [ ] I can say when a `HashMap` plus one sort beats a `TreeMap`, and when it does not.
