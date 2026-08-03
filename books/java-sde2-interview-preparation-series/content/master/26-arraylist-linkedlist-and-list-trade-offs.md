# 26. ArrayList, LinkedList, and List Trade-offs

## A loop that looks linear and is not

```java
for (int i = 0; i < list.size(); i++) {
    consume(list.get(i));
}
```

One pass, one `get` per element. It is `O(n)` - as long as `list` is an `ArrayList`.

If it is a `LinkedList`, `get(i)` has to *walk there*. The implementation is smart enough to start from whichever end is nearer, so reaching index `i` costs `min(i, n-1-i)` link hops. Summed over a full pass that is `floor((n-1)^2 / 4)` hops:

| n | indexed loop | for-each loop | ratio |
|---:|---:|---:|---:|
| 100 | 2,450 hops | 100 | 24x |
| 1,000 | 249,500 hops | 1,000 | 250x |
| 10,000 | 24,995,000 hops | 10,000 | 2,500x |

Nothing about the code changed. The declared type was `List`, so the compiler had no objection, and at n = 100 in a unit test it is imperceptible. This is why the second most useful thing to know about a list is which implementation is underneath, and the most useful thing is that the interface will not tell you.

## Two representations of the same four elements

![Figure 26.1 - The same four elements, two representations](assets/diagrams/16-list-memory-layout.png)

An `ArrayList` is **one object holding a reference array**. Element `i` lives at a computed offset, so `get` is arithmetic. A CPU cache line pulls in several neighbouring references at once, so traversal is fast in a way Big-O does not express.

A `LinkedList` is **one node object per element**, each with a payload reference and two link references. Nothing is computed; everything is followed. Each hop is a potential cache miss, and every element costs an allocation the garbage collector will later have to trace.

Both iterate in `O(n)`. They do not iterate at the same speed, and they do not cost the same to hold.

> **Specification boundary:** `List` promises positional semantics - order, indexing, duplicates, and
> equality by corresponding elements. It promises nothing about storage or cost. `ArrayList` separately
> documents constant-time positional access and amortized constant-time append; its capacity growth
> policy is not a public contract at all.

```text
ArrayList invariant:   0 <= size <= capacity
                       valid elements occupy indexes [0, size)

LinkedList invariant:  first.prev == null, last.next == null,
                       and adjacent next/prev links agree
```

## Size, capacity, and the append that is secretly O(n)

`size` is what you can see. `capacity` is what was allocated. When they are equal and you append, there is nowhere to write, so the implementation allocates a larger array and copies every existing reference.

![Figure 26.2 - Why appending is O(n) and amortized O(1) at the same time](assets/diagrams/15-arraylist-growth.png)

That one append is `O(n)`. Yet appending `n` elements in total is `O(n)`, not `O(n^2)`, because capacity grows *multiplicatively*: the copies form a geometric series bounded by a constant multiple of `n`. Growing by a fixed increment instead would need `n/c` resizes copying an average of `n/2` each - genuinely quadratic. At n = 10,000 that is the difference between about 16,000 copied references and about 5,000,000.

Two things follow, and interviewers ask for both:

- **Amortized `O(1)` is a statement about a sequence, not about latency.** A single `add` can still stall while a large array is copied. In a tail-latency-sensitive path that matters, and `ensureCapacity` with a known size removes it.
- **The growth factor is not a contract.** Never write logic that depends on a particular capacity sequence.

> **HotSpot note:** OpenJDK has historically grown `ArrayList` capacity by roughly one half in common cases, with special handling at the boundaries. This is version-sensitive implementation detail.

## Where the "linked lists are better for insertion" claim goes wrong

Relinking a node *is* `O(1)`. The claim omits how you reached the node.

```java
list.add(50_000, value);   // LinkedList: walk 50,000 links, THEN relink
```

`add(index, value)` must find the position first, so it is `O(n)` overall. The constant-time case is real but narrow: you are already sitting at the position, holding a `ListIterator`, and you insert or remove through it.

| Operation | `ArrayList` | `LinkedList` |
|---|---|---|
| `get`/`set` by index | `O(1)` | `O(n)` |
| append | amortized `O(1)` | `O(1)` |
| add/remove at front | `O(n)` shift | `O(1)` |
| add/remove at arbitrary index | `O(n)` shift | `O(n)` walk + `O(1)` relink |
| add/remove through a positioned `ListIterator` | `O(n)` shift | **`O(1)`** |
| search by value | `O(n)` | `O(n)` |
| memory per element | one array slot | node object + two links |

Even the one column `LinkedList` wins is usually better served by `ArrayDeque`, which gives constant-time operations at both ends with array locality and no per-element allocation. In practice: **default to `ArrayList`; use `ArrayDeque` for queue and stack behaviour; reach for `LinkedList` only when a measurement says so.**

## `remove(1)` does not mean what it looks like

```java
List<Integer> ids = new ArrayList<>(List.of(10, 20, 30));

ids.remove(1);                      // removes INDEX 1 -> [10, 30]
ids.remove(Integer.valueOf(1));     // removes the VALUE 1 -> no change here
```

`List` declares both `remove(int)` and `remove(Object)`. With a `List<Integer>` the literal `1` is an `int`, so overload resolution picks the index form without a widening or boxing conversion. There is no warning. The fix is to be explicit - `Integer.valueOf(1)`, or `removeIf(v -> v == 1)`.

## `subList` is a window, not a slice

![Figure 26.3 - subList is a window, not a slice](assets/diagrams/17-sublist-view.png)

`list.subList(2, 5)` does not copy anything. It stores a reference to the parent, an offset, and a size. Half-open, as everywhere in Java: `from` included, `to` excluded.

```java
List<String> window = list.subList(2, 5);
window.set(0, "x");        // writes parent index 2
window.clear();            // removes that whole range from the parent
```

`clear()` on a sublist is genuinely useful - it is the cheapest way to delete a range. What breaks it is structurally modifying the *parent* directly while the view is alive: the view's recorded size goes stale, and the next view operation typically throws `ConcurrentModificationException`. The specification calls the result undefined, so do not build on the exception either.

Two consequences worth carrying:

- If you need an independent range, say so: `new ArrayList<>(list.subList(2, 5))`.
- A view retains its parent. Holding a three-element window of a million-element list keeps all million reachable.

## Which construction do you actually want?

```java
new ArrayList<>()               // mutable, resizable
new ArrayList<>(source)         // independent mutable shallow copy
new ArrayList<>(1_000)          // same, with capacity pre-reserved
Arrays.asList(array)            // FIXED SIZE view over the array; set writes through
List.of(a, b, c)                // unmodifiable, rejects null
List.copyOf(source)             // unmodifiable snapshot, rejects null
Collections.unmodifiableList(source)  // unmodifiable LIVE VIEW of source
stream.toList()                 // unmodifiable, allows null
```

`Arrays.asList` catches people twice: it is fixed-size (so `add` throws), and `set` writes through to the backing array. Also, `Arrays.asList(intArray)` on a `int[]` produces a one-element `List<int[]>`, not a list of numbers - a primitive array is a single object.

## Worked example: a list with an invariant

A timeline kept sorted by insertion rather than re-sorted on read:

```java
import java.time.Instant;
import java.util.*;

record Event(Instant at, String id) {}

final class Timeline {
    private static final Comparator<Event> ORDER =
            Comparator.comparing(Event::at).thenComparing(Event::id);

    private final List<Event> events = new ArrayList<>();

    void add(Event event) {
        int found = Collections.binarySearch(events, event, ORDER);
        int insertionPoint = found >= 0 ? found : -found - 1;
        events.add(insertionPoint, event);
    }

    List<Event> snapshot() {
        return List.copyOf(events);      // callers cannot break the invariant
    }
}
```

Trace `add` of `(10:12, d)` into `[(10:00,a), (10:05,b), (10:20,c)]`:

```text
binarySearch: low=0 high=2 mid=1 -> 10:05 < 10:12, low=2
              low=2 high=2 mid=2 -> 10:20 > 10:12, high=1
              not found, returns -(2)-1 = -3, so insertionPoint = 2

before: [10:00/a, 10:05/b, 10:20/c, _      ]
shift:  [10:00/a, 10:05/b, 10:20/c, 10:20/c]
write:  [10:00/a, 10:05/b, 10:12/d, 10:20/c]
```

The search is `O(log n)`; the shift is `O(n)`. **Insertion is `O(n)` overall** - finding the spot cheaply does not make putting it there cheap. That gap is a favourite follow-up question.

Note also the `-found - 1` encoding: `binarySearch` returns a negative value whose complement is the insertion point, precisely so a caller can distinguish "found at index 0" from "not found, belongs at index 0".

## Complexity and performance in practice

The table above hides hardware. `ArrayList` traversal wins on contiguity, bulk copies use optimised intrinsics, and there are `n` fewer objects for the collector to trace. For small lists the constants dominate everything else. For very large ones, remember that indexes are `int`.

If insertion into the middle dominates your workload, the answer is usually neither list: it is a different structure (a tree index, a map), or a different shape of work - append everything, sort once, publish an immutable snapshot.

## Edge cases and common mistakes

- Indexed traversal of a `LinkedList`, turning a linear pass quadratic.
- Claiming linked insertion is `O(1)` without accounting for finding the position.
- `remove(1)` on a `List<Integer>` when you meant the value.
- Expecting `Arrays.asList` to be resizable, or forgetting that `set` writes through to the array.
- Returning a `subList` as if it were an independent result.
- Retaining a small `subList` of a huge parent and leaking the parent.
- Calling `ensureCapacity` with a size derived from untrusted input.
- Sorting a list, then mutating a field the ordering reads, and leaving the invariant broken.
- `List.of(primitiveArray)` producing a one-element list.
- Using `CopyOnWriteArrayList` for a write-heavy workload - it copies the whole array on every mutation.
- Relying on a specific capacity growth sequence.

## Production engineering notes

Default to `ArrayList`. Pre-size it when you have trustworthy batch metadata, and cap any estimate derived from a request. Prefer `ArrayDeque` for stack and queue use.

Keep invariants behind the API. If a list must stay sorted, do not return the mutable list. If duplicates are invalid, a `Set` models it better. If lookups by ID dominate, maintain a `Map` instead of scanning.

Prefer bulk construction to incremental middle insertion: append, validate, sort once, publish. And watch retention - a long-lived list holds every element graph it references, which is a common shape for a slow memory leak in a cache or buffer.

## Interview questions and model answers

**Why is `ArrayList` usually faster than `LinkedList` even when both are `O(n)`?**

Contiguous reference slots give cache locality, there is one allocation instead of `n`, there are no pointer indirections, and bulk copies use optimised intrinsics. The asymptotics are equal; the constants and the GC pressure are not.

**Is insertion into a `LinkedList` constant time?**

Relinking is, once you hold the position. `add(index, value)` must walk to the index first and is `O(n)`. The constant-time case requires an already-positioned `ListIterator`.

**How can append be `O(n)` and amortized `O(1)` at once?**

A resize copies every existing reference, so that one call is linear. Because capacity grows by a multiplicative factor, resizes are rare enough that total work over `n` appends is linear, making the average constant. It bounds the sequence, not any individual call.

**What does `subList` return, and when does it break?**

A live half-open window backed by the parent, storing an offset and size. Supported mutations write through. Structurally modifying the parent directly while the view is alive leaves it stale; the next view operation usually throws, and the specification calls the result undefined.

**`list.remove(1)` on a `List<Integer>` - what happens?**

It removes index 1, because `remove(int)` matches exactly and no boxing is needed. Use `remove(Integer.valueOf(1))` for value removal.

**When would you deliberately choose `LinkedList`?**

Rarely. Only where the workload is dominated by insertion or removal at a maintained iterator position, or at the ends, and a measurement supports it - and for the ends I would compare `ArrayDeque` first.

## Exercises

1. Dry-run `remove(2)` from a six-element `ArrayList`. Count moved references and say which slot must be nulled, and why leaving it costs memory.
2. Verify the `floor((n-1)^2/4)` formula for a full indexed `LinkedList` traversal by summing `min(i, n-1-i)` for n = 10 and n = 100.
3. Write a sorted-insert that places a duplicate *after* all equal elements, and explain why `binarySearch` alone does not tell you which equal element it found.
4. Demonstrate write-through aliasing between an array and `Arrays.asList(array)`. Contrast with `new ArrayList<>(Arrays.asList(array))`.
5. Predict, then check, the result of `List.of(new int[]{1,2,3}).size()`.
6. Design a benchmark comparing `ArrayList`, `LinkedList`, and `ArrayDeque` for removal from the front. State the warm-up, the allocation you expect, and what would make the result misleading.
7. Refactor a class that returns its mutable internal list into an ownership-safe API preserving deterministic order.

## Chapter summary

`List` fixes the positional semantics and nothing about cost, which is why the same loop is linear over an `ArrayList` and quadratic over a `LinkedList` - 24,995,000 link hops instead of 10,000 at n = 10,000. `ArrayList` stores references contiguously in one object: indexed access is arithmetic, append is amortized constant because capacity grows multiplicatively, and middle changes shift a suffix. `LinkedList` allocates a node per element: relinking is constant *once you are there*, but the public API almost always makes you walk there first. Amortized constant bounds the sequence, not the latency of any single call, and the growth factor is not a contract. Around the edges sit the traps that cost real time: `remove(int)` versus `remove(Object)`, the fixed-size `Arrays.asList`, and `subList` - a live window that writes through to its parent, breaks if the parent is modified behind it, and retains the whole parent for as long as you hold it.

## Revision checklist

- [ ] I never index-loop a list I did not choose the implementation of.
- [ ] I can state both list invariants from memory.
- [ ] I can derive amortized append without quoting a growth factor.
- [ ] I include the cost of *finding* the position when analysing linked insertion.
- [ ] I know the mutability, resizability, and null policy of all seven list constructions.
- [ ] I know `remove(1)` removes an index.
- [ ] I can explain what `subList` stores, what writes through, what invalidates it, and what it retains.
- [ ] I choose a list from access pattern, allocation, locality, and ownership - then measure.
