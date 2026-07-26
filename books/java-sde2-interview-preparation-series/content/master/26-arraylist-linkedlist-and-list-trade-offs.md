# 26. ArrayList, LinkedList, and List Trade-offs

## Learning objectives

By the end of this chapter, you should be able to:

- explain the `List` contract and its positional model;
- derive the invariants of dynamic arrays and doubly linked lists;
- analyze access, insertion, removal, iteration, and memory costs;
- explain amortized growth without claiming a fixed growth formula;
- use list iterators, sublists, immutable factories, and array conversions safely; and
- select a list implementation from workload and locality, not folklore.

## Why this matters at SDE-2

Lists look simple, which makes them good interview probes. A candidate who says "linked lists are better for insertion" without discussing how the insertion position is found misses the important cost. Backend systems usually favor `ArrayList` because indexed storage, cache locality, low allocation count, and predictable iteration dominate. `LinkedList` remains useful for learning pointer invariants and can serve as a deque, but it is rarely the best default list.

At SDE-2 you should reason from operation distribution and data size. A batch assembled once and scanned many times has different needs from a cursor that repeatedly inserts at a known position. You should also recognize that a list can be modifiable, fixed-size, unmodifiable, or a backed range view despite sharing the same `List` type.

## First-principles model

A `List<E>` represents an ordered sequence indexed from `0` through `size - 1`. It permits duplicates and, depending on implementation, may permit null. Equality compares corresponding elements in sequence.

An array-backed list maintains conceptually:

```text
elements: [e0, e1, e2, e3, null, null, null, null]
size:      4
capacity:  8

Invariant: 0 <= size <= capacity
Valid logical elements occupy indexes [0, size).
```

A doubly linked list maintains nodes:

```text
null <- [prev|A|next] <-> [prev|B|next] <-> [prev|C|next] -> null
         first                                      last

Invariant: first.prev == null, last.next == null,
and adjacent next/prev links agree.
```

The dynamic array makes location computation cheap: address is derived from base plus index. The linked representation must follow references to find an index. Once a linked node is already known, relinking neighbors is constant work, but the public `List` API usually supplies an index or search value, not a node handle.

> **Specification boundary:** `List` promises positional behavior, not storage representation or complexity. `ArrayList` documents constant-time positional access and amortized constant-time append. Its exact capacity-growth policy is not a public contract.

## Core terminology

- **Logical size:** Number of elements visible through the list.
- **Capacity:** Number of slots currently available in an array-backed representation.
- **Amortized cost:** Average cost per operation over a sequence, including occasional expensive operations.
- **Random access:** Efficient access by arbitrary index. `RandomAccess` is a marker used by some algorithms to adapt behavior.
- **Locality:** Tendency for nearby data to be located near each other in memory and used together.
- **Node:** A linked-list record containing an element and neighbor references.
- **Cursor:** A position between elements, represented by `ListIterator`.
- **Range view:** A live portion of a list, typically from `subList`.
- **Fixed-size list:** Element replacement is allowed, but size-changing operations are rejected.

## Detailed mechanics

### ArrayList operations

`get(i)` and `set(i, value)` validate the index and access one slot. Appending writes into the next slot if capacity remains. When full, the implementation allocates a larger array and copies references. Inserting at index `i` shifts the suffix `[i, size)` one slot right. Removing at `i` shifts the later suffix left and clears the obsolete final slot so the removed object is not retained through that array reference.

Suppose size equals capacity and append triggers a copy of `n` references. One append is `O(n)`, but if capacity grows geometrically, many preceding appends were cheap. Across a long append sequence, total copied references form a geometric series bounded by a constant multiple of `n`, yielding amortized `O(1)` append.

`ensureCapacity` can reduce repeated resizing when a reasonable size estimate exists. `trimToSize` can reduce spare storage but may cost `O(n)` and cause future regrowth. Neither should be called reflexively.

> **HotSpot note:** OpenJDK has historically grown `ArrayList` capacity by roughly one-half of the old capacity in common cases, with boundary handling. This is version-sensitive implementation detail. Never write logic that depends on a particular capacity sequence.

### LinkedList operations

Each element is stored in a node with references to predecessor and successor. Adding at either end updates a small number of links and endpoints. Finding index `i` walks from the nearer end in common implementations, still `O(n)` asymptotically. Removing by value also searches before unlinking.

`LinkedList` implements both `List` and `Deque`. Its strongest use is often end-based queue behavior, but `ArrayDeque` usually offers better locality and lower allocation overhead for that role. A linked list does not reserve unused array capacity, yet it usually uses much more memory per element because every element has a node object and two link fields.

### Iteration and ListIterator

For both structures, one full iterator traversal is `O(n)`. Indexed traversal can be disastrous for a linked list:

```java
for (int i = 0; i < list.size(); i++) {
    consume(list.get(i));
}
```

If `list` is a `LinkedList`, repeated `get` makes the loop `O(n^2)`. An enhanced `for` loop or iterator is `O(n)`.

`ListIterator` supports forward and backward traversal, reports neighboring indexes, and can add, remove, or replace at its cursor when supported. Its state rules matter: `remove` or `set` must follow a successful `next` or `previous`, and cannot immediately follow `add` or another `remove`.

Typical general-purpose list iterators are fail-fast when they detect structural mutation outside the iterator. Detection is best-effort and must not be used for synchronization or program control. Use the iterator's mutation methods or establish exclusive ownership.

### List construction variants

- `new ArrayList<>()` is mutable and resizable.
- `new ArrayList<>(source)` is a shallow mutable copy.
- `Arrays.asList(array)` is a fixed-size view backed by the array. `set` changes the array.
- `List.of(elements...)` is unmodifiable and rejects null.
- `List.copyOf(source)` is unmodifiable and rejects null; it may reuse an already suitable immutable instance.
- `Collections.unmodifiableList(source)` is an unmodifiable view backed by `source`.
- `stream.toList()` returns an unmodifiable list, but code should not assume a concrete implementation.

### Sublists

`list.subList(from, to)` uses a half-open range: `from` is included and `to` is excluded. It is normally backed by the parent. Clearing a sublist can efficiently remove a range from the parent. Structural modification of the backing list outside the view invalidates assumptions; subsequent behavior is generally not useful to depend upon and commonly produces `ConcurrentModificationException`.

If a stable independent range is needed, use `new ArrayList<>(list.subList(from, to))`.

### Conversion to arrays

`toArray()` returns `Object[]`. `toArray(new String[0])` and `toArray(String[]::new)` produce a typed array. Modern JVMs optimize common zero-length-array idioms, so prefer clarity and measure if this conversion is actually hot. Conversion copies references and costs `O(n)` space and time.

## Worked Java example

This example keeps a sorted timeline in an `ArrayList` and inserts a new event using binary search:

```java
import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

record Event(Instant at, String id) {}

final class Timeline {
    private static final Comparator<Event> ORDER =
            Comparator.comparing(Event::at).thenComparing(Event::id);

    private final ArrayList<Event> events = new ArrayList<>();

    void add(Event event) {
        int result = java.util.Collections.binarySearch(events, event, ORDER);
        int insertionPoint = result >= 0 ? result : -result - 1;
        events.add(insertionPoint, event);
    }

    List<Event> between(Instant startInclusive, Instant endExclusive) {
        ArrayList<Event> result = new ArrayList<>();
        for (Event event : events) {
            if (event.at().isBefore(startInclusive)) {
                continue;
            }
            if (!event.at().isBefore(endExclusive)) {
                break;
            }
            result.add(event);
        }
        return List.copyOf(result);
    }

    List<Event> snapshot() {
        return List.copyOf(events);
    }
}
```

The list invariant is that `events` is sorted by `ORDER`. The class, not callers, owns mutation. A real system with frequent arbitrary insertion may choose a tree index or append plus periodic sort; the example illustrates how search cost and movement cost differ.

## Execution or memory walkthrough

Assume the list contains events at `(10:00, a)`, `(10:05, b)`, and `(10:20, c)`. Adding `(10:12, d)` performs binary search:

```text
low=0 high=2, mid=1 -> 10:05 is smaller, low=2
low=2 high=2, mid=2 -> 10:20 is larger, high=1
search ends; insertion point=2

before: [10:00/a, 10:05/b, 10:20/c, empty]
shift:  [10:00/a, 10:05/b, 10:20/c, 10:20/c]
write:  [10:00/a, 10:05/b, 10:12/d, 10:20/c]
```

Binary search uses `O(log n)` comparisons, but insertion shifts `O(n)` references in the worst case. Overall insertion remains `O(n)`. This distinction is a common interview trap.

The array holds references, not inline `Event` record payloads. `snapshot()` creates another list storage object containing the same immutable `Event` references. Because `Event` contains immutable components, shallow sharing is safe. If elements were mutable, the snapshot would protect membership only.

## Complexity and performance

| Operation | `ArrayList` | `LinkedList` |
|---|---:|---:|
| `get` or `set` by index | `O(1)` | `O(n)` |
| append | amortized `O(1)` | `O(1)` |
| add or remove at front | `O(n)` | `O(1)` |
| insert/remove at arbitrary index | `O(n)` shift | `O(n)` search plus `O(1)` relink |
| insert/remove through positioned iterator | `O(n)` shift | `O(1)` relink |
| search by value | `O(n)` | `O(n)` |
| full iteration | `O(n)` | `O(n)` |
| auxiliary structure per element | array slot | node plus links |

The table hides hardware effects. `ArrayList` traversal is usually faster because reference slots are contiguous, copying can use optimized bulk operations, and there are fewer allocations and indirections. `LinkedList` nodes increase garbage collection work and can miss CPU caches.

For small lists, constants dominate. For very large lists, the `int` index and array-size limits matter. Neither implementation should be treated as a disk-scale container. Benchmark representative data with the intended JDK and hardware.

## Edge cases and common mistakes

- Using `LinkedList` for frequent indexed reads, creating accidental quadratic behavior.
- Claiming linked insertion is `O(1)` while ignoring the `O(n)` search for a position.
- Using `remove(1)` on `List<Integer>` when intending to remove the value `1`; it removes index 1. Use `remove(Integer.valueOf(1))` for value removal.
- Assuming `Arrays.asList` is a normal resizable `ArrayList`.
- Exposing `subList` as a durable independent result.
- Forgetting half-open range boundaries and accepting `from > to` or invalid indexes.
- Retaining a tiny sublist backed by a very large custom or older representation; make an independent copy when retention is uncertain.
- Sorting a list and later mutating fields used by ordering without restoring the invariant.
- Treating `List.of(array)` as a list of array elements when a primitive array is passed; a primitive array is one reference element.
- Forgetting null rejection in immutable factories.
- Relying on exact `ArrayList` capacity growth.
- Using copy-on-write lists for write-heavy workloads; that concurrent implementation copies on mutation.

## Production engineering notes

Default to `ArrayList` for general mutable sequence storage. Provide an expected capacity when reliable batch metadata exists, but cap estimates derived from untrusted input to avoid oversized allocation. Prefer `ArrayDeque` for stack and queue operations. Use `LinkedList` only when its measured workload and cursor or end operations justify its overhead.

Preserve invariants behind an API. If a list must remain sorted, do not return the mutable list. If duplicates are invalid, a `Set` may express the model better. If lookups by ID dominate, maintain a map rather than repeatedly scanning a list.

Bulk construction is frequently better than incremental middle insertion: append, validate, sort once, and publish an immutable snapshot. Keep lists local to a request when possible. Shared mutation requires synchronization or a concurrent design; an unmodifiable wrapper does not make concurrent reads safe while another alias mutates the backing list.

Watch memory retention in queues or batch buffers. Clear references when custom array structures remove elements. Standard implementations do this, but application-level lists can still retain large element graphs simply because a long-lived owner retains the list.

## Interview questions and model answers

**Why is `ArrayList` usually faster than `LinkedList` for iteration?**

Its reference slots are contiguous, so it uses fewer objects and indirections and has better cache locality. Both are `O(n)`, but their constants and memory behavior differ greatly.

**Is insertion into `LinkedList` constant time?**

Relinking is constant time once the node or iterator position is known. `add(index, value)` must locate the index and is therefore `O(n)` overall.

**How can append be both `O(n)` and amortized `O(1)`?**

An append that triggers resize copies existing references and costs `O(n)`. Geometric capacity growth makes resizes infrequent, so total work across many appends is linear and average cost per append is constant.

**What does `subList` return?**

A backed range view, not an independent copy. Supported mutations affect the parent, and external structural changes to the parent can invalidate the view.

**What is the difference between size and capacity?**

Size is the number of logical elements. Capacity is allocated slot count in an array-backed implementation. Capacity is not part of the `List` contract.

**When would you deliberately choose `LinkedList`?**

Only when a workload relies on constant-time end operations or insert/remove at a maintained iterator position and measurement shows it is appropriate. Even for deque use, I would compare `ArrayDeque` first.

## Exercises

1. Dry-run removal at index 2 from an array list of six elements. Count moved references and identify the slot that must be cleared.
2. Prove that indexed traversal of a linked list is `O(n^2)` by summing traversal distances.
3. Implement a sorted list insertion method that places a duplicate after all equal values. State why binary search alone does not promise which equal item it finds.
4. Demonstrate aliasing between an array and `Arrays.asList(array)`. Contrast it with `new ArrayList<>(...)`.
5. Design a benchmark hypothesis comparing `ArrayList`, `LinkedList`, and `ArrayDeque` for removing from the front. Include allocation and warmup considerations.
6. Refactor a class that returns its mutable internal list into an ownership-safe API while preserving deterministic order.

## Chapter summary

`List` defines ordered positional semantics, while implementations determine representation and cost. `ArrayList` uses a resizable reference array, offering constant-time indexed access, amortized constant-time append, efficient traversal, and suffix movement for middle changes. `LinkedList` uses doubly linked nodes, offering constant-time relinking at known positions but linear positional access and high allocation overhead. Views, factories, and iterators introduce mutability and aliasing differences that must be part of API design.

## Revision checklist

- [ ] I can state dynamic-array and doubly linked-list invariants.
- [ ] I can derive amortized append cost without relying on a growth factor.
- [ ] I include position-finding cost when analyzing linked insertion.
- [ ] I avoid indexed traversal for sequential-access lists.
- [ ] I know the behavior of `Arrays.asList`, `List.of`, `List.copyOf`, and unmodifiable wrappers.
- [ ] I can use `ListIterator` statefully and safely.
- [ ] I understand that `subList` is a backed half-open range.
- [ ] I can choose a list using access pattern, locality, allocation, and ownership.
