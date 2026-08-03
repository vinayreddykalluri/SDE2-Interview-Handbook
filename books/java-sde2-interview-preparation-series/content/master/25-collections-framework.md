# 25. Collections Framework Architecture

## Start with a bug

A service returns the list of backends a router is allowed to use:

```java
final class RoutingTable {
    private final List<String> backends = new ArrayList<>();

    List<String> backends() {
        return backends;          // looks harmless
    }
}
```

Six months later, a caller three layers away writes `table.backends().remove(0)` to skip a backend for one request. It compiles. It runs. It permanently removes a backend from the router for the lifetime of the process, and nothing in the type system objected.

That is the shape of most real collection defects. Not "I picked a slow data structure" - almost nobody loses a day to that. It is "I handed out a reference and lost control of who can change what," or "I asked for a snapshot and got a live view," or "I put an object in a map and later it could not be found." This chapter is about the boundaries that prevent those, and the vocabulary interviewers use to probe them.

## Choosing by guarantee, not by habit

Every collection interface is a promise about behaviour. Pick the one whose promise matches the requirement, then pick an implementation.

![Figure 25.1 - What each collection contract promises](assets/diagrams/13-collection-contracts.png)

- **`List`** - position is meaningful and duplicates are allowed. "The first three results, in rank order."
- **`Set`** - membership is meaningful and duplicates are not. "The set of user IDs that opted in."
- **`Queue` / `Deque`** - the *removal order* is the point. "Process the oldest pending job." A `Deque` also serves as the correct stack.
- **`Map`** - a unique key identifies a value. It is not a `Collection`, because "add one element" has no meaning for a key-value pair.

There is a trap in this that interviewers use deliberately. The interface tells you the semantics; it tells you nothing about cost:

| Call | What the interface promises | What it actually costs |
|---|---|---|
| `List.get(i)` | element at position `i` | `ArrayList` `O(1)`; `LinkedList` `O(n)` |
| `Set.contains(x)` | membership | `HashSet` expected `O(1)`; `TreeSet` `O(log n)` |
| `Map.get(k)` | value for the key | `HashMap` expected `O(1)`; `TreeMap` `O(log n)` |

So "`List.get` is constant time" is wrong as stated. "`ArrayList.get` is constant time" is right. Naming the concrete class before quoting a complexity is a small habit that separates a precise answer from a memorised one.

> **Specification boundary:** interface contracts specify observable behaviour. They do not promise array storage, linked nodes, a hashing strategy, tree shape, amortized cost, or fail-fast detection. Complexity belongs to the documentation of a concrete implementation.

## Three ways to hand out data, and they behave differently

Return to the routing table. There are three plausible fixes, and they are not interchangeable. Suppose the source list holds `A, B, C`, we wrap it three ways, and *then* someone appends `D` to the source.

![Figure 25.2 - View, copy, and unmodifiable are three different things](assets/diagrams/14-view-copy-unmodifiable.png)

```java
List<String> source = new ArrayList<>(List.of("A", "B", "C"));

List<String> view    = Collections.unmodifiableList(source);
List<String> frozen  = List.copyOf(source);
List<String> mutable = new ArrayList<>(source);

source.add("D");
```

- `view` now contains four elements. It is a **live view**: it rejects mutation *through that reference*, but it reflects every change made through `source`.
- `frozen` still contains three. `List.copyOf` took an **independent snapshot**. It also rejects null elements, which is useful only if that matches your domain.
- `mutable` still contains three, and callers may modify it freely without touching `source`.

None of the three makes the *elements* immutable. If `A`, `B`, and `C` were mutable objects, every one of these can still observe them change. Unmodifiable is a statement about the reference, not about the data reachable through it.

For the routing table, the fix is to copy on the way in and stop exposing the collection at all:

```java
final class RoutingTable {
    private final List<String> backends;

    RoutingTable(Collection<String> backends) {
        this.backends = List.copyOf(backends);   // independent, unmodifiable
    }

    String chooseBackend(int requestHash) {      // a domain operation
        return backends.get(Math.floorMod(requestHash, backends.size()));
    }
}
```

`chooseBackend` protects an invariant that `backends().remove(0)` cannot. Prefer exposing behaviour to exposing storage.

## Views are everywhere, and that is mostly good

`map.keySet()`, `map.values()`, and `map.entrySet()` are views onto the same map. So is `list.subList(from, to)`. They exist because copying would be wasteful, and they are genuinely useful - but they are aliases, and aliases surprise people:

```java
Map<String, Integer> counts = new HashMap<>(Map.of("a", 1, "b", 2));
counts.keySet().remove("a");
// counts is now {b=2} - removing from the key set removed the mapping
```

That is the documented contract, not an accident. The rule to carry: **if a method returns something derived from a collection, find out whether it is a view or a copy before you store it, return it, or hand it across a layer.**

## Modifying while iterating

This is the single most common collection mistake, and the enhanced `for` loop hides why:

```java
for (String id : ids) {
    if (id.isBlank()) {
        ids.remove(id);        // usually throws ConcurrentModificationException
    }
}
```

The loop is really an `Iterator`, and the iterator keeps its own bookkeeping about the structure. Removing behind its back leaves that bookkeeping stale. The three correct forms:

```java
ids.removeIf(String::isBlank);                       // clearest

Iterator<String> it = ids.iterator();                // when you need more control
while (it.hasNext()) {
    if (it.next().isBlank()) {
        it.remove();                                 // the iterator updates itself
    }
}

List<String> kept = ids.stream()                     // when the source is shared
        .filter(id -> !id.isBlank()).toList();
```

> **HotSpot note:** common OpenJDK collections keep a modification counter and compare it against the iterator's expected value. Detection is best-effort. `ConcurrentModificationException` is a *bug signal*, not a synchronisation mechanism - a program that relies on catching it is relying on an implementation detail, and a genuinely concurrent modification may go undetected entirely.

## Optional operations: the type does not prove mutability

`Collection` declares `add`, `remove`, and `clear`, but an implementation is permitted to reject them:

| Expression | Mutable? | Resizable? | Nulls? |
|---|---|---|---|
| `new ArrayList<>()` | yes | yes | yes |
| `Arrays.asList(array)` | `set` only | **no** | yes |
| `List.of(...)` | no | no | **rejected** |
| `List.copyOf(source)` | no | no | **rejected** |
| `Collections.unmodifiableList(source)` | no (through this ref) | no | inherits source |
| `stream.toList()` | no | no | allowed |

Java has no type-level distinction between a mutable and a read-only collection. `List<String>` tells a caller nothing about whether they may modify it. That is why capability belongs in the documentation and, better, in the API shape.

## Bulk operations hide their cost

`addAll`, `removeAll`, `retainAll`, and `containsAll` look like single operations. Their cost depends on *both* sides:

```java
List<String> all = ...;        // 100,000 entries
List<String> banned = ...;     //   1,000 entries
all.removeAll(banned);         // ~100,000 x 1,000 comparisons
```

`removeAll` asks the argument `contains` for each element of the receiver. With a `List` argument that is a linear scan every time, so the whole call is quadratic. Converting the lookup side costs one pass and changes the class of the operation:

```java
Set<String> bannedSet = new HashSet<>(banned);
all.removeIf(bannedSet::contains);          // ~100,000 expected-constant lookups
```

## Worked example

Group unique order IDs by customer, preserving first-seen order on both levels:

```java
import java.util.*;

record Order(String id, String customerId) {}

final class OrderIndex {
    static Map<String, List<String>> build(Collection<Order> orders) {
        Map<String, Set<String>> working = new LinkedHashMap<>();

        for (Order order : orders) {
            Objects.requireNonNull(order, "order");
            working.computeIfAbsent(order.customerId(),
                            ignored -> new LinkedHashSet<>())
                   .add(order.id());
        }

        Map<String, List<String>> result = new LinkedHashMap<>();
        working.forEach((customer, ids) -> result.put(customer, List.copyOf(ids)));
        return Collections.unmodifiableMap(result);
    }
}
```

Three decisions carry the contract:

1. `LinkedHashMap` and `LinkedHashSet`, not `HashMap` and `HashSet`, because the result promises encounter order. A plain `HashMap` would give an order that looks stable in testing and is not guaranteed.
2. `computeIfAbsent` creates the inner set exactly once, replacing the `get`-check-`put` sequence.
3. The return is wrapped in `Collections.unmodifiableMap(new LinkedHashMap<>(...))` rather than `Map.copyOf`. `Map.copyOf` returns an unmodifiable map but **does not promise to preserve iteration order**, so it would silently break the order guarantee the method advertises.

Point 3 is the kind of detail that only shows up in production, when a report starts rendering customers in a different order after an unrelated upgrade.

For inputs `o-1/c-7`, `o-2/c-8`, `o-1/c-7`, `o-3/c-7`: the duplicate `o-1` reaches the existing `LinkedHashSet`, `add` returns `false`, and membership is unchanged - deduplication happens without a single explicit check.

## Complexity and memory

Building the index is expected `O(n)` time and `O(u)` space for `u` unique pairs. But memory is often the constraint that bites first:

- Node-based structures (`LinkedList`, `TreeMap`, `HashMap` bins) allocate one object per element, with headers and pointers on top of your data.
- Array-based structures (`ArrayList`, `ArrayDeque`) reserve spare capacity, but store references contiguously and traverse far faster than the asymptotics suggest.
- A view keeps its whole parent reachable. A three-element `subList` of a million-element list retains all million.

## Edge cases and common mistakes

- Choosing a concrete class before deciding uniqueness, ordering, and mutation semantics.
- Returning a mutable internal collection and losing control of an invariant.
- Assuming an unmodifiable wrapper is a snapshot, or that unmodifiable membership makes elements immutable.
- Depending on `HashMap` or `HashSet` iteration order because it looked stable in a test.
- Removing from a collection directly while iterating it.
- Treating `ConcurrentModificationException` as reliable race detection.
- Forgetting that `subList` and the map views are backed by their owner - including for memory retention.
- Letting a bulk operation stay quadratic when converting one side to a `HashSet` would fix it.
- Storing an object as a key and then mutating a field its `equals` or `hashCode` reads.
- Confusing `Collection` (the interface) with `Collections` (the static utility class).

## Production engineering notes

Document six things for every collection an API exposes: **order, duplicates, null policy, mutability, live-view-versus-snapshot, and thread safety.** A declared type of `List<T>` communicates exactly one of those.

Copy at boundaries you own. Where copying is genuinely too expensive, make ownership transfer explicit in the method name and documentation rather than hoping.

Do not share an ordinary mutable collection across threads without a policy. `Collections.synchronizedList` makes each individual call atomic, which is not the same as making *your* operation atomic - a check-then-add still needs one lock scope covering both. Concurrent collections have different iteration and atomicity contracts and should be chosen deliberately, not as a reflex.

## Interview questions and model answers

**Why is `Map` not a `Collection`?**

A `Collection` models individual elements; a `Map` models key-to-value associations with unique keys. `Collection.add(E)` has no sensible meaning for a mapping. `Map` bridges into the hierarchy through its `keySet`, `values`, and `entrySet` views.

**What is the difference between an unmodifiable view and an immutable copy?**

A view rejects mutation through that reference but reflects changes made through another alias to the same data. A copy has independent membership that later source changes cannot affect. Neither makes the referenced element objects immutable.

**What does fail-fast mean, and what does it not mean?**

An iterator may detect an unsupported structural modification and throw `ConcurrentModificationException` promptly. It is best-effort in common implementations. It is not thread safety, not guaranteed detection, and not a control-flow mechanism.

**Can the interface type tell you the complexity?**

No. `List.get` is constant on `ArrayList` and linear on `LinkedList`, and both satisfy `List`. State the concrete type and your assumptions whenever you quote a bound.

**A method returns `List<String>`. What do you still not know?**

Whether you may modify it; whether it is a snapshot or a live view; whether it permits nulls; whether it is safe to hold across threads; and whether holding it retains a much larger structure. All five need documentation.

**How would you make a quadratic `removeAll` linear?**

Put the membership side in a `HashSet` and use `removeIf`. The receiver is still scanned once, but each membership test drops from a linear scan to an expected-constant lookup.

## Exercises

1. Write a class that exposes "the currently enabled feature flags" in deterministic order, such that no caller can change the set. State every contract your type does not express.
2. Run the three-way wrap experiment from this chapter and record which of `view`, `frozen`, and `mutable` observe the appended element. Predict first, then run.
3. Remove an entry through `map.entrySet().iterator().remove()` and explain why it succeeds where `map.remove(k)` inside a for-each fails.
4. Measure `removeAll` with a `List` argument and with a `HashSet` argument at n = 100,000. Report both times and the ratio.
5. Build a `subList` view, structurally modify the parent, then call a method on the view. Record what happens - and explain why you must not build behaviour on that observation.
6. Take the `OrderIndex` example, replace both linked implementations with `HashMap` and `HashSet`, and describe precisely which part of the method's contract you just broke.

## Chapter summary

The framework separates behavioural interfaces from implementations, and most real defects live at that seam rather than in complexity. Choose the interface by the guarantee you need - order, uniqueness, removal policy, key lookup - then choose an implementation, and only then quote a cost. Views, unmodifiable wrappers, and copies are three distinct things with three distinct aliasing behaviours, and none of them freezes the elements. Iterators keep bookkeeping that direct mutation invalidates, and the exception you get is a bug signal rather than a guarantee. Bulk operations hide costs that depend on both operands. Above all, a collection crossing an API boundary carries six contracts that its declared type expresses none of: order, duplicates, nulls, mutability, liveness, and thread safety - write them down.

## Revision checklist

- [ ] I choose the interface from the required guarantee, not from the class I know best.
- [ ] I name the concrete implementation before quoting any complexity.
- [ ] I can state the difference between a view, an unmodifiable wrapper, and a copy - and what none of them do.
- [ ] I know that `keySet`, `values`, `entrySet`, and `subList` are views onto their owner.
- [ ] I can remove elements during iteration three different correct ways.
- [ ] I treat `ConcurrentModificationException` as a bug signal, not a mechanism.
- [ ] I can spot a quadratic bulk operation and fix it with a `HashSet`.
- [ ] I document order, duplicates, nulls, mutability, liveness, and thread safety at every API boundary.
