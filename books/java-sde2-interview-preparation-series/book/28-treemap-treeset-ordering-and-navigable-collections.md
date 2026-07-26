# 28. TreeMap, TreeSet, Ordering, and Navigable Collections

## Learning objectives

By the end of this chapter, you should be able to:

- distinguish sorted, encounter-ordered, and unordered collections;
- explain binary-search-tree and red-black-tree invariants;
- use `TreeMap`, `TreeSet`, `NavigableMap`, and `NavigableSet` operations correctly;
- reason about comparator consistency, mutable keys, ranges, and backed views;
- analyze logarithmic operations and ordered traversal; and
- choose a tree-based collection when navigation is required, not merely for deterministic output.

## Why this matters at SDE-2

Ordered maps and sets support time-window indexes, floor and ceiling lookup, leaderboards, routing ranges, and nearest-neighbor decisions. Interviews often ask for "the next event," "all keys in a range," or "the largest value no greater than x." A hash table cannot answer those efficiently without an additional scan or sort.

The difficult part is not recalling `TreeMap`. It is preserving an ordering that is total, stable, and consistent with the application's notion of identity. A comparator that treats two distinct keys as equal silently changes map membership. A range view that escapes its intended lifetime introduces aliasing. An SDE-2 engineer recognizes these as correctness contracts.

## First-principles model

A binary search tree stores one entry per node and maintains:

```text
all keys in left subtree  < node key
all keys in right subtree > node key
```

Here `<` is defined by a comparator or natural ordering. An ordinary binary search tree can become a chain under sorted insertion, producing linear operations. A balanced tree adds structural invariants that keep height logarithmic.

A red-black tree conceptually enforces:

1. Every node has a color, red or black.
2. The root is black.
3. Missing leaf sentinels are black.
4. A red node has no red child.
5. Every path from a node to a missing leaf contains the same number of black nodes.

These rules limit the longest root-to-leaf path to at most about twice the shortest, so height is `O(log n)`. Insertions and removals restore invariants using recoloring and rotations.

> **Specification boundary:** `TreeMap` and `TreeSet` promise sorted and navigable behavior with documented logarithmic basic operations. The API does not require a red-black tree specifically. OpenJDK's representation, node fields, and balancing code are implementation details.

## Core terminology

- **Natural ordering:** Ordering provided by a type's `Comparable.compareTo`.
- **Comparator:** Object defining an external ordering through `compare(a, b)`.
- **Total order:** Every pair can be compared consistently, with transitivity and antisymmetry-like sign rules.
- **Ordering-equivalent:** `compare(a, b) == 0`.
- **Consistent with equals:** Ordering-equivalent exactly when `a.equals(b)` for relevant values.
- **Floor:** Greatest element less than or equal to a target.
- **Lower:** Greatest element strictly less than a target.
- **Ceiling:** Least element greater than or equal to a target.
- **Higher:** Least element strictly greater than a target.
- **Range view:** Backed portion bounded by keys or elements.
- **Rotation:** Local tree transformation preserving in-order sequence while changing shape.

## Detailed mechanics

### Ordering is identity inside a tree collection

For `TreeMap`, two keys are treated as the same map key when comparison returns zero. `equals` need not be consulted. For `TreeSet`, comparison zero means duplicate membership. Therefore a comparator inconsistent with equals can make the collection violate the general expectations of `Map` or `Set` equality, even while operating according to its own ordering.

This comparator collapses all people with the same age:

```java
Comparator<Person> byAgeOnly = Comparator.comparingInt(Person::age);
```

If distinct people of one age must coexist, add a stable tie-breaker:

```java
Comparator<Person> byAgeThenId = Comparator
        .comparingInt(Person::age)
        .thenComparing(Person::id);
```

The tie-breaker must reflect the intended identity and remain stable while stored.

### Search and insertion

Lookup starts at the root. A negative comparison descends left, positive descends right, and zero finds the key. Insertion follows the same path, attaches a new node, and restores balance. A rotation changes parent-child links without changing in-order traversal:

```text
      C                 B
     /                 / \
    B       ->        A   C
   /
  A
```

Removal has extra cases. A leaf can be detached. A node with one child can be replaced by that child. A node with two children is logically replaced with its successor or predecessor, after which a simpler node is removed. Balanced implementations then repair color or height invariants.

> **HotSpot note:** Current OpenJDK `TreeMap` uses a red-black tree and parent-linked entry nodes. Exact deletion strategy, color representation, and helper methods are version-sensitive.

### Navigational operations

`NavigableMap` offers `lowerEntry`, `floorEntry`, `ceilingEntry`, and `higherEntry`, along with key-returning forms. Entry-returning navigation methods produce snapshots of mappings rather than entries supporting `setValue`. `firstEntry` and `lastEntry` inspect endpoints; `pollFirstEntry` and `pollLastEntry` remove endpoints.

`NavigableSet` supplies equivalent element operations. These methods avoid the off-by-one logic that often appears in hand-written range code.

For key `20` in `{10, 20, 30}`:

```text
lower(20)   = 10
floor(20)   = 20
ceiling(20) = 20
higher(20)  = 30
```

### Range and descending views

`subMap`, `headMap`, and `tailMap` expose backed views. Navigable overloads make endpoints explicit:

```java
map.subMap(from, true, to, false) // [from, to)
```

Mutating a range view mutates the backing map. Inserting a key outside the range throws `IllegalArgumentException`. `descendingMap` and `descendingSet` are reverse-order views, not full copied reversals. Reversing again produces an equivalent view of the original order.

Iterators over ordinary tree maps, sets, and their views commonly detect unsupported structural mutation and fail fast. Detection is best-effort, not a concurrency guarantee; synchronize access or use a collection designed for concurrent navigation.

A stable snapshot requires copying. Copy into another `TreeMap` if the comparator and sorted navigation must be retained, or into a list if only the current ordered sequence is needed.

### Natural order, null, and heterogeneous keys

Natural ordering requires keys implementing mutually compatible `Comparable`. Mixing unrelated types can cause `ClassCastException`. Natural-order `TreeMap` rejects null keys because they cannot be compared. A custom comparator could define null placement, although null keys usually make domain modeling weaker.

Construct comparator chains carefully. `Comparator.comparing` can accept a key comparator, including `nullsFirst` or `nullsLast`. Do not implement numeric comparison with subtraction:

```java
// Wrong: overflow can reverse the sign.
(a, b) -> a.score() - b.score()

// Correct:
Comparator.comparingInt(Player::score)
```

### TreeSet relationship

A typical `TreeSet` is backed by a `NavigableMap` and stores elements as keys with a marker value. Its comparator, range behavior, navigation, and logarithmic costs follow the ordered map. The set API exposes only elements, preserving uniqueness under comparison.

> **HotSpot note:** Backing `TreeSet` with a `TreeMap` is the familiar OpenJDK design, not a requirement that every Java implementation use the same fields.

## Worked Java example

The following index finds the configuration effective at a given time:

```java
import java.time.Instant;
import java.util.NavigableMap;
import java.util.TreeMap;

record Config(String revision, int timeoutMillis) {
    public Config {
        if (timeoutMillis <= 0) {
            throw new IllegalArgumentException("timeout must be positive");
        }
    }
}

final class ConfigTimeline {
    private final NavigableMap<Instant, Config> byStart = new TreeMap<>();

    void publish(Instant startsAt, Config config) {
        if (startsAt == null || config == null) {
            throw new IllegalArgumentException("arguments must be non-null");
        }
        Config previous = byStart.putIfAbsent(startsAt, config);
        if (previous != null) {
            throw new IllegalStateException("a revision already starts at " + startsAt);
        }
    }

    Config effectiveAt(Instant instant) {
        var entry = byStart.floorEntry(instant);
        if (entry == null) {
            throw new IllegalStateException("no configuration yet");
        }
        return entry.getValue();
    }

    NavigableMap<Instant, Config> changes(
            Instant fromInclusive, Instant toExclusive) {
        return java.util.Collections.unmodifiableNavigableMap(
                new TreeMap<>(byStart.subMap(
                        fromInclusive, true, toExclusive, false)));
    }
}
```

The copy inside `changes` is deliberate. Returning only an unmodifiable wrapper around `subMap` would still expose later updates from the backing timeline. Here callers receive a membership snapshot that retains sorted navigation.

## Execution or memory walkthrough

Suppose revisions start at 09:00, 12:00, and 18:00. Searching at 15:30 asks for `floorEntry(15:30)`. The tree follows comparisons until it either finds the key or reaches a missing child. During descent it remembers the best key seen that is below the target. The result is the 12:00 revision.

For a possible balanced shape:

```text
           12:00(B)
          /        \
     09:00(R)    18:00(R)
```

Adding 20:00 may initially attach under 18:00. If red-black rules are violated, recoloring or rotations restore them while preserving this in-order sequence:

```text
09:00, 12:00, 18:00, 20:00
```

The exact resulting shape and colors are not API-visible. Code should observe only ordering and mappings.

Each map entry typically carries key, value, left, right, parent, and balancing metadata. That is more per-entry memory than many hash-table entries and much more than flat arrays. A copied range allocates new tree entries but shares immutable `Instant` and `Config` references.

## Complexity and performance

For `n` entries in a balanced ordered tree:

| Operation | Time | Additional space |
|---|---:|---:|
| `get`, `put`, `remove` | `O(log n)` | `O(1)` excluding inserted node |
| floor/ceiling/lower/higher | `O(log n)` | `O(1)` |
| first/last | `O(log n)` typical/documented basic bound | `O(1)` |
| ordered traversal | `O(n)` | iterator state usually `O(1)` with parent links |
| range traversal returning `k` entries | `O(log n + k)` conceptual | view `O(1)`, copy `O(k)` nodes |

Comparator cost multiplies tree depth. Comparing long strings or extracting expensive sort keys can dominate. Precompute immutable comparison fields when justified.

`HashMap` usually provides lower expected point-lookup cost and lower comparison overhead. `TreeMap` earns its cost when sorted traversal, bounded ranges, or neighbor queries are first-class. If data changes rarely and is read often, a sorted array or list plus binary search may offer better locality and memory use.

## Edge cases and common mistakes

- Supplying a comparator that is not transitive, leading to unpredictable placement and lookup.
- Returning zero for distinct keys that must coexist.
- Mutating a field used by comparison while an element is stored.
- Using subtraction in integer comparators and overflowing.
- Confusing `lower` with `floor` or `higher` with `ceiling`.
- Forgetting that range bounds can be inclusive or exclusive.
- Returning a mutable backed range to an untrusted caller.
- Assuming a descending view is an independent reversed copy.
- Expecting null keys to work with natural ordering.
- Comparing heterogeneous keys without a comparator capable of handling both.
- Using `TreeMap` solely to print deterministic test output when sorting once would be simpler.
- Assuming the precise red-black tree shape or choosing behavior based on it.

## Production engineering notes

Centralize comparators as named, tested constants. Test sign symmetry, transitivity, zero behavior, and tie-breakers with generated data. Use stable immutable identifiers as final tie-breakers. When comparator equality intentionally differs from object equality, document that the tree collection defines uniqueness by ordering.

Range views are excellent within a short operation because they avoid copies. At service boundaries, prefer snapshots unless live coupling is a deliberate API feature. Treat `pollFirstEntry` and `pollLastEntry` as mutations and protect shared structures with an appropriate concurrency design; `TreeMap` is not thread-safe.

If a time index receives duplicate timestamps, decide whether one mapping, a list per timestamp, or a composite key is correct. Do not let an accidental comparator-zero replacement make that business decision. For very high read concurrency, consider publishing immutable sorted snapshots. For write-heavy concurrent navigation, evaluate purpose-built concurrent structures such as `ConcurrentSkipListMap` and its weaker snapshot semantics.

Measure retained memory and comparator CPU. Ordered indexes can duplicate data already held elsewhere. Decide which structure owns values, and remove stale entries consistently to avoid unbounded retention.

## Interview questions and model answers

**How does `TreeMap` differ from `HashMap`?**

`TreeMap` maintains keys in comparator or natural order and supports range and neighbor queries in `O(log n)`. `HashMap` provides expected `O(1)` point operations but no ordering contract. The right choice follows required semantics.

**What happens when a comparator returns zero?**

The tree treats the keys as the same key or set element. A map insertion replaces the value for that ordering-equivalent key. Therefore tie-breakers are necessary when distinct objects must coexist.

**Why are tree operations logarithmic?**

A balancing invariant keeps height `O(log n)`. Each search follows one root-to-leaf path, and rebalancing uses a bounded amount of local work per level.

**What is the difference between `floorKey` and `lowerKey`?**

`floorKey(x)` may return `x` itself and finds the greatest key `<= x`. `lowerKey(x)` requires a strictly smaller key.

**Is `subMap` a copy?**

No. It is a backed, bounded view. Changes are reflected in the owner, and out-of-range insertions are rejected. Copy it when independent lifetime is needed.

**Must `TreeMap` be a red-black tree?**

No. The public contract promises behavior and complexity, not a specific balancing algorithm. Red-black structure describes current common OpenJDK implementation.

## Exercises

1. Design a comparator for orders sorted by descending priority, then creation time, then immutable ID. Explain why every tie-breaker is needed.
2. For keys `{10, 20, 30}`, compute lower, floor, ceiling, and higher results for targets 5, 20, 25, and 35.
3. Implement an interval lookup where each key is a range start. State what invariant is needed to avoid overlapping ranges.
4. Demonstrate how a comparator by string length causes `TreeSet` to collapse distinct same-length strings. Repair it.
5. Compare a sorted `ArrayList` plus binary search with `TreeMap` for one bulk build followed by one million reads.
6. Return an immutable snapshot of a descending range while preserving its comparator.

## Chapter summary

Navigable tree collections maintain elements under a total ordering and support point, range, endpoint, and neighbor operations. Balanced-tree invariants keep height logarithmic; OpenJDK commonly uses red-black trees, but this is not the API contract. Comparison defines key identity inside the tree, so consistency, tie-breakers, and key stability are correctness requirements. Range and descending collections are backed views, making ownership choices as important as algorithmic complexity.

## Revision checklist

- [ ] I can state binary-search-tree and red-black-tree invariants.
- [ ] I understand that comparison zero defines uniqueness in tree collections.
- [ ] I can build safe comparator chains without subtraction overflow.
- [ ] I know lower, floor, ceiling, and higher semantics.
- [ ] I can use inclusive and exclusive range views correctly.
- [ ] I distinguish backed views from sorted snapshots.
- [ ] I can compare tree maps with hash maps and sorted arrays.
- [ ] I label red-black representation details as version-sensitive.
