# 25. Collections Framework Architecture

## Learning objectives

By the end of this chapter, you should be able to:

- distinguish collection interfaces, implementations, algorithms, and views;
- choose between `List`, `Set`, `Queue`, `Deque`, and `Map` from behavioral requirements;
- explain optional operations, iteration order, mutability, and fail-fast iteration;
- reason about structural modification, backed views, equality, and bulk operations;
- state complexity as an implementation property, not an interface promise; and
- design collection-facing APIs that preserve invariants and ownership boundaries.

## Why this matters at SDE-2

Collections sit in nearly every backend path: request aggregation, caches, indexes, batching, deduplication, scheduling, and persistence mapping. At SDE-2, naming a familiar class is not enough. You are expected to identify the required semantics first, defend complexity claims, understand aliasing, and notice when an innocent view or mutable key creates a correctness defect.

An interview question such as "Which collection would you use?" is usually testing a decision process. Does order matter? Are duplicates allowed? Is lookup by key required? Are nulls valid? Is mutation concurrent? Is a sorted range query needed? The framework gives a vocabulary for those constraints.

## First-principles model

A collection is an object that owns or exposes a group of references. The Java Collections Framework separates four ideas:

1. Interfaces define behavioral contracts, such as uniqueness or positional access.
2. Implementations choose a data structure, memory layout, and performance profile.
3. Algorithms operate through interfaces, such as sorting or binary search.
4. Views expose a live projection of another object, such as a map's key set.

The principal hierarchy is:

```text
Iterable
  Collection
    List
    Set
      SortedSet
        NavigableSet
    Queue
      Deque

Map                         (not a Collection)
  SortedMap
    NavigableMap
```

`Map` is separate because it models key-value associations rather than individual elements. Its `keySet()`, `values()`, and `entrySet()` methods bridge into the `Collection` hierarchy through views.

> **Specification boundary:** Interface contracts specify observable behavior. They generally do not promise array storage, linked nodes, hashing strategy, tree shape, amortized cost, or fail-fast detection. Complexity claims belong to the documentation of a concrete implementation.

## Core terminology

- **Element:** A reference stored by a collection. Collections do not contain primitive values directly; boxing creates wrapper objects when needed.
- **Structural modification:** A change that can alter iteration structure, usually adding or removing elements. Replacing a value may or may not be structural for a particular implementation.
- **Encounter order:** The order in which an iteration mechanism presents elements. Some collections define it; others do not.
- **Natural ordering:** Ordering defined by `Comparable`.
- **Optional operation:** An interface method that an implementation may reject with `UnsupportedOperationException`.
- **View:** A projection backed by another object. Changes may be visible in both directions.
- **Snapshot:** An independent representation of state at a point in time.
- **Unmodifiable:** Mutation through that reference is rejected. It does not necessarily mean the underlying data or elements are immutable.
- **Immutable:** State cannot change after construction, including through aliases permitted by the abstraction.
- **Fail-fast iterator:** An iterator that attempts to detect unsupported concurrent structural modification and throw `ConcurrentModificationException`.

## Detailed mechanics

### Selecting the abstraction

Use a `List` when position, encounter order, or duplicates are meaningful. Use a `Set` for membership and uniqueness. Use a `Queue` when processing order matters and elements enter and leave through queue operations. Use a `Deque` for both ends or stack behavior. Use a `Map` when a unique key identifies a value.

Program parameters and return types to the narrowest useful interface. A method that only iterates can accept `Iterable<T>` or `Collection<T>` rather than `ArrayList<T>`. Do not hide an important semantic property, however: accepting `Set<T>` communicates uniqueness, while accepting `Collection<T>` does not.

### Optional operations and capability

`Collection` includes methods such as `add`, `remove`, and `clear`, but implementations can reject them. `List.of(...)` produces an unmodifiable list. `Arrays.asList(array)` is fixed-size: `set` works, but size-changing methods do not. This design lets algorithms use a common interface, but it means the static type alone does not prove mutability.

Java has no standard type-level distinction between mutable and read-only collections. Document ownership and capability explicitly. Prefer immutable copies at public boundaries when callers should not mutate state:

```java
final class RoutingTable {
    private final List<String> backends;

    RoutingTable(Collection<String> backends) {
        this.backends = List.copyOf(backends);
    }

    List<String> backends() {
        return backends;
    }
}
```

`List.copyOf` also rejects null elements. That is useful only if null rejection matches the domain contract.

### Iterators and structural modification

An `Iterator` is a cursor with `hasNext`, `next`, and optional `remove`. Enhanced `for` normally uses it. Removing directly from a typical collection during iteration can invalidate cursor state:

```java
for (String id : ids) {
    if (id.isBlank()) {
        ids.remove(id); // usually wrong
    }
}
```

Use `Iterator.remove`, `removeIf`, or a separate result collection. The iterator's own removal operation updates both the structure and its bookkeeping.

> **HotSpot note:** Common OpenJDK collections maintain a modification counter and compare it with an iterator's expected value. This is implementation detail and detection is best-effort. `ConcurrentModificationException` is a bug signal, not a synchronization mechanism. Exact fields and detection points are version-sensitive.

### Views and aliasing

`map.keySet()`, `map.values()`, and `map.entrySet()` are normally backed views. Removing a key from the key set removes the mapping. An entry's `setValue` can update the map when supported. `list.subList(from, to)` is also a backed view. A structural change to the parent outside the sublist can make later sublist operations fail.

Wrappers from `Collections.unmodifiableList(source)` are read-only views, not copies. If another alias mutates `source`, the wrapper reflects it. By contrast, `List.copyOf(source)` returns an unmodifiable list whose membership is not backed by later source changes. Neither operation freezes mutable element objects.

### Equality and bulk operations

`List.equals` is order-sensitive. `Set.equals` compares membership independent of order. `Map.equals` compares mappings. These contracts enable equality across implementations: an `ArrayList` can equal a `LinkedList` with the same sequence.

Bulk methods include `addAll`, `removeAll`, `retainAll`, `containsAll`, `removeIf`, and `replaceAll`. Their apparent single-call form does not imply constant time. Cost depends on both receiver and argument. For example, removing every element found in an `ArrayList` argument can be quadratic when membership checks are linear. Converting the lookup side to a `HashSet` can improve the expected bound.

### Null policy and element validity

Null support varies. Some general-purpose collections allow null; many queues, concurrent collections, sorted structures with natural ordering, and factory-created immutable collections reject it. A robust API validates domain values before selecting an implementation rather than relying on an incidental null policy.

## Worked Java example

The following method groups unique order IDs by customer while preserving first-seen customer order and first-seen order ID order:

```java
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

record Order(String id, String customerId) {}

final class OrderIndex {
    static Map<String, List<String>> build(Collection<Order> orders) {
        Map<String, Set<String>> working = new LinkedHashMap<>();

        for (Order order : orders) {
            if (order == null || order.id() == null || order.customerId() == null) {
                throw new IllegalArgumentException("order fields must be non-null");
            }
            working.computeIfAbsent(
                    order.customerId(), ignored -> new LinkedHashSet<>())
                    .add(order.id());
        }

        Map<String, List<String>> result = new LinkedHashMap<>();
        working.forEach((customer, ids) ->
                result.put(customer, List.copyOf(ids)));
        return Map.copyOf(result);
    }

    public static void main(String[] args) {
        List<Order> input = new ArrayList<>();
        input.add(new Order("o-1", "c-7"));
        input.add(new Order("o-2", "c-8"));
        input.add(new Order("o-1", "c-7"));
        input.add(new Order("o-3", "c-7"));

        System.out.println(build(input));
    }
}
```

One subtlety: `Map.copyOf` guarantees an unmodifiable map, but its iteration order is not specified to preserve the insertion order of the supplied `LinkedHashMap`. If public iteration order is part of the result contract, return an unmodifiable copy with an order-preserving implementation, for example `Collections.unmodifiableMap(new LinkedHashMap<>(result))`, and document the guarantee.

## Execution or memory walkthrough

For the four inputs above:

1. `o-1/c-7` creates the first map entry and a set containing `o-1`.
2. `o-2/c-8` creates a second entry. The map's encounter order is now `c-7`, `c-8`.
3. The duplicate `o-1/c-7` reaches the existing set. `Set.add` returns `false`; membership remains unchanged.
4. `o-3/c-7` appends a second unique ID to the first customer's insertion-ordered set.
5. The conversion creates new lists, so clients cannot add or remove IDs through the result.

The working representation uses a map object, map entries, set objects, set entries, and references to existing strings. The result adds list storage and map entries. Peak memory includes both representations until the method returns and the working map becomes unreachable. The code copies collection structure, not `String` content; strings are immutable, so sharing them is safe.

## Complexity and performance

Let `n` be the number of orders and `u` the number of unique customer-order pairs. With typical `LinkedHashMap` and `LinkedHashSet` behavior, building has expected `O(n)` time and `O(u)` space; producing the result costs `O(u)`. Hash collisions, resizing, and key methods affect constants and worst cases.

Interface-only complexity should be stated cautiously:

| Operation | Interface guarantee | Common implementation example |
|---|---|---|
| `List.get(i)` | No bound | `ArrayList`: `O(1)`; `LinkedList`: `O(n)` |
| `Set.contains(x)` | No bound | `HashSet`: expected `O(1)`; `TreeSet`: `O(log n)` |
| `Map.get(k)` | No bound | `HashMap`: expected `O(1)`; `TreeMap`: `O(log n)` |
| iteration | Semantic order may vary | Usually `O(n)`, but hash-table capacity can affect traversal |
| `containsAll` | No bound | Depends on sizes and membership cost |

Memory is often as important as asymptotic time. Node-based structures add per-element objects and pointers. Array-based structures may reserve unused capacity but provide locality. Measure representative workloads rather than extrapolating from Big-O alone.

## Edge cases and common mistakes

- Choosing a concrete type before defining uniqueness, ordering, and access semantics.
- Returning a mutable internal list and unintentionally granting write access.
- Assuming an unmodifiable wrapper is a snapshot or that immutable membership makes elements immutable.
- Depending on `HashMap` or `HashSet` iteration order.
- Modifying a collection directly while iterating over it.
- Treating `ConcurrentModificationException` as guaranteed detection of a race.
- Forgetting that `subList` and map collection views are backed by their owner.
- Passing a collection to itself in a bulk operation whose behavior is undefined or surprising.
- Using mutable objects whose `equals`, `hashCode`, or ordering fields change while stored.
- Assuming every implementation permits null or mutation.
- Calling `size()` repeatedly on a nonstandard collection without checking whether it is cheap.
- Confusing `Collection` with `Collections`; the latter is an algorithm and wrapper utility class.

## Production engineering notes

Define collection contracts in API documentation: order, duplicates, null policy, mutability, snapshot versus live view, thread safety, and expected scale. A return type of `List<T>` communicates sequence but not ownership.

Defensively copy inputs when the component must own stable membership. For very large inputs, copying can be costly; an explicit ownership-transfer convention or immutable persistent data structure may be more appropriate. Never expose a lazily changing view across layers unless that behavior is deliberate.

Avoid sharing ordinary mutable collections across threads without a synchronization policy. A synchronized wrapper makes individual calls mutually exclusive, but compound actions such as "check then add" still need one lock scope. Concurrent collections have different iteration and atomicity contracts and should be selected explicitly.

Prefer domain operations over raw collection exposure. `routingTable.chooseBackend()` protects invariants better than `routingTable.backends().remove(0)`. Validate capacity and input size where untrusted requests can cause large allocations. Instrument unusually large collection sizes and expensive conversions in latency-sensitive paths.

## Interview questions and model answers

**Why is `Map` not a subtype of `Collection`?**

A collection models individual elements, while a map models key-value mappings with key uniqueness. Map exposes collection views for keys, values, and entries, but operations such as adding one arbitrary element do not fit its contract.

**What does fail-fast mean?**

It means an iterator may detect an unsupported structural modification and throw `ConcurrentModificationException` promptly. It is best-effort behavior in common implementations, not a thread-safety guarantee or a correctness mechanism.

**What is the difference between an unmodifiable view and an immutable copy?**

An unmodifiable view rejects mutation through that reference but can reflect mutations through another alias. An immutable copy has independent, unchangeable membership. In both cases, referenced element objects can still be mutable unless separately constrained.

**Why accept an interface rather than `ArrayList`?**

It minimizes coupling and admits any implementation satisfying the required behavior. The chosen interface should still express semantics: `Set` is preferable to `Collection` when uniqueness is required.

**Can interface type tell you complexity?**

Usually no. `List.get` can be constant or linear depending on implementation. State the concrete type and assumptions whenever making a complexity claim.

**How do backed views affect correctness?**

They create aliases. Mutating the owner can change the view, and supported mutations through the view can change the owner. They are useful for efficient projections but dangerous if callers assume snapshots.

## Exercises

1. Design a method signature for returning active feature flags in deterministic order without allowing callers to mutate membership. State every contract not captured by the type.
2. Predict the result of removing an entry through `map.entrySet().iterator().remove()` and explain why it differs from mutating the map directly during iteration.
3. Compare `Collections.unmodifiableList(source)`, `List.copyOf(source)`, and `new ArrayList<>(source)` for mutability, null handling, and aliasing.
4. Rewrite a quadratic `removeAll` workflow so membership tests use a set. Give time and space bounds.
5. Create a small program that demonstrates a `subList` backed view. Then structurally modify the parent outside the view and record the observed behavior without treating it as a portable synchronization technique.

## Chapter summary

The Collections Framework separates behavioral interfaces from data-structure implementations, reusable algorithms, and backed views. A sound choice begins with semantics: order, uniqueness, lookup, mutation, concurrency, and ownership. Complexity belongs to concrete implementations. Iterators, views, and optional operations make the framework flexible, but they also create important capability and aliasing boundaries. Production APIs should state those boundaries explicitly and copy or wrap data according to an intentional ownership model.

## Revision checklist

- [ ] I can sketch the `Collection` hierarchy and explain why `Map` is separate.
- [ ] I can choose among `List`, `Set`, `Queue`, `Deque`, and `Map` from requirements.
- [ ] I distinguish interface contracts from implementation complexity.
- [ ] I understand optional operations and common fixed-size or unmodifiable collections.
- [ ] I can explain structural modification and correct iterator removal.
- [ ] I can distinguish a view, an unmodifiable wrapper, a shallow copy, and deep immutability.
- [ ] I know the equality semantics of lists, sets, and maps.
- [ ] I document order, nulls, ownership, thread safety, and expected scale in APIs.
