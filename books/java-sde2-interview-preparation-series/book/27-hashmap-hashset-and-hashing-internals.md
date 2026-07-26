# 27. HashMap, HashSet, and Hashing Internals

## Learning objectives

By the end of this chapter, you should be able to:

- derive hash-table lookup from equality and bucket selection;
- state the `equals` and `hashCode` contract and key-stability invariant;
- explain collisions, load factor, resizing, and expected complexity;
- describe a typical OpenJDK `HashMap` without presenting it as a platform guarantee;
- use `compute`, `merge`, views, and iteration safely; and
- identify security, concurrency, memory, and API-design risks around hash tables.

## Why this matters at SDE-2

Hash maps support indexes, caches, joins, deduplication, frequency counts, and graph representations. They are also a rich interview subject because correctness spans language-level equality, data-structure invariants, amortized analysis, and mutation discipline.

At SDE-2, "lookup is O(1)" is incomplete. The useful answer is expected `O(1)` under a suitable hash distribution and stable key behavior, with resizing and collision caveats. You should be able to diagnose a missing entry caused by a mutated key, distinguish absence from a null value, and choose an ordered, identity-based, weak-key, concurrent, or sorted alternative when semantics demand it.

## First-principles model

A hash table transforms a key into a candidate region rather than comparing it with every stored key. Conceptually:

```text
hashCode(key) -> mixed hash -> bucket index -> inspect candidates -> equals
```

The hash narrows the search; `equals` determines logical key identity. Different keys can have the same hash and must coexist as a collision. Equal keys must be directed to the same search region.

The central invariant is:

```text
For every stored entry (k, v), lookup using a key equal to k
must search the bucket containing that entry.
```

That requires equal objects to have equal hash codes and requires fields used by equality and hashing to remain stable while the key is stored.

`HashSet<E>` uses the same membership idea without an application value. A typical implementation is backed by a hash map whose keys are set elements and whose values are a private marker.

> **Specification boundary:** `Map` specifies unique keys and mapping behavior. `HashMap` specifies null support and general performance expectations in its API documentation, but bucket array shape, hash mixing, resize thresholds, tree bins, and constants are implementation details.

## Core terminology

- **Hash code:** An `int` computed for an object and used to group possible matches.
- **Collision:** Distinct, non-equal keys assigned to the same bucket.
- **Bucket/bin:** A table position containing zero or more entries.
- **Capacity:** Number of table buckets in a typical hash-table representation.
- **Load factor:** Ratio controlling how full the table may become before resizing.
- **Threshold:** Entry count that triggers resizing, often capacity times load factor.
- **Rehash/resizing:** Allocating a larger table and redistributing entries.
- **Hash flooding:** Many keys deliberately or accidentally colliding.
- **Tree bin:** A collision bucket represented by a balanced tree in some implementations.
- **Key stability:** Requirement that equality and hash behavior not change while a key is stored.
- **Canonical key:** Stable value representation used as a map key, such as a normalized immutable identifier.

## Detailed mechanics

### Equality and hashing contract

`Object` establishes these practical rules:

1. If `a.equals(b)` is true, `a.hashCode() == b.hashCode()` must be true.
2. Unequal objects may share a hash code.
3. Repeated hash calls during one execution should be consistent while equality-relevant state is unchanged.

A record automatically derives value-based `equals` and `hashCode` from components, making records useful keys when components are themselves stable:

```java
record TenantUser(String tenantId, String userId) {}
```

If a mutable class uses `tenantId` and `userId` for hashing, changing either after insertion can strand the entry. The object remains physically in its old bucket while lookup computes a new bucket.

### Lookup, insertion, and collision resolution

A typical `get(key)` computes a hash, maps it to an index, then checks candidate entries. It first compares hash values and then key equality. Correct comparisons account for identical references and null according to implementation policy.

On `put(key, value)`, a matching key replaces its value and returns the old value. If no matching key exists, a new entry is linked or otherwise inserted into the bucket. Map size changes only for a new key. A `HashSet.add` similarly returns `false` when an equal element is already present.

Many hash tables use separate chaining: each bucket holds multiple entry nodes. Other hash tables use open addressing, but ordinary OpenJDK `HashMap` has traditionally used bucket chains with tree conversion under certain conditions.

### Capacity, load factor, and resizing

A low load factor uses more buckets and generally shortens collision searches. A high load factor conserves table space but increases collisions. The default is intended as a general compromise, not a universal optimum.

When size crosses a threshold, a typical implementation allocates a larger bucket array and relocates or splits entries. The resize is `O(n)`, but geometric growth makes a long sequence of inserts expected amortized `O(1)` each. Pre-sizing can avoid repeated growth when an estimate is reliable. Excessive pre-sizing wastes memory and can slow full iteration because iteration may scan buckets as well as entries.

> **HotSpot note:** In current OpenJDK implementations, capacities are generally powers of two, index selection uses masked hash bits, and resizing often doubles capacity. Hash spreading mixes high bits into low bits. Details and helper methods can change by JDK release.

### Tree bins

Long collision chains degrade lookup toward `O(n)`. Modern OpenJDK versions can transform a sufficiently populated bin into a red-black tree when the table is also large enough, giving `O(log n)` comparison depth for that bin under suitable conditions. Bins can later return to list form.

> **HotSpot note:** Treeification and untreeification thresholds, minimum table capacity, tie-breaking, and node layouts are version-sensitive. The Java API does not guarantee tree bins or worst-case logarithmic lookup. Treat them as resilience in a particular implementation, not as permission to use poor hashes.

### Null, absence, and defaulting

`HashMap` permits one null key and multiple null values. Therefore `get(key) == null` can mean no mapping or a mapping to null. Use `containsKey` when the distinction matters. Prefer avoiding null values in domain maps when absence has a clear meaning.

`getOrDefault` returns the mapped value even if that value is null; it uses the default only when no mapping exists. `putIfAbsent` treats a null mapping as absent for its operation. Read each method's contract carefully.

### Compute and merge methods

`computeIfAbsent` is ideal for multimap assembly:

```java
index.computeIfAbsent(customerId, ignored -> new ArrayList<>()).add(order);
```

The mapping function should be short and should not structurally mutate the same map recursively. If it returns null, no mapping is recorded. `computeIfPresent` runs only for a present non-null mapping. `compute` considers both presence and absence; a null result removes the mapping. `merge(k, incoming, remapper)` inserts `incoming` when absent or combines it with the current non-null value; a null remapping result removes the key.

These methods make a compound update concise on an ordinary map but do not make that map thread-safe. Concurrent map implementations define stronger per-key atomic behavior for their overrides.

### Views and iteration

`keySet`, `values`, and `entrySet` are backed views. Iterate `entrySet` when both key and value are needed; repeated `map.get(key)` is redundant. Removing through a view removes mappings. A `HashMap` defines no stable iteration order. Even if a run appears consistent, a resize, JDK change, input change, or hash change can alter it.

Typical iterators are fail-fast under detected unsupported structural modification. Replacing the value for an existing key is often nonstructural, but this is not a general concurrency contract. `Map.Entry` objects from iteration may be tied to the map and should not be retained as permanent independent records; use `Map.entry(k, v)` or a domain record for a snapshot pair.

## Worked Java example

This frequency index uses an immutable composite key and `merge`:

```java
import java.util.HashMap;
import java.util.List;
import java.util.Map;

record Endpoint(String method, String path) {
    public Endpoint {
        if (method == null || path == null) {
            throw new IllegalArgumentException("endpoint fields are required");
        }
        method = method.toUpperCase(java.util.Locale.ROOT);
    }
}

record Request(String method, String path) {}

final class RequestCounter {
    static Map<Endpoint, Long> count(List<Request> requests) {
        Map<Endpoint, Long> counts = new HashMap<>();
        for (Request request : requests) {
            Endpoint key = new Endpoint(request.method(), request.path());
            counts.merge(key, 1L, Long::sum);
        }
        return Map.copyOf(counts);
    }

    public static void main(String[] args) {
        var requests = List.of(
                new Request("get", "/health"),
                new Request("GET", "/orders"),
                new Request("GET", "/health"));
        System.out.println(count(requests));
    }
}
```

Normalization is part of key construction, so equality semantics match the domain rule that HTTP method case is irrelevant here. The path is intentionally not normalized; whether `/orders/` equals `/orders` is an application decision, not a hash-table concern.

`Map.copyOf` prevents result membership changes and rejects null keys or values. It does not promise the iteration order of the source hash map, so output formatting and tests must not depend on order.

## Execution or memory walkthrough

For the sample input:

1. `Endpoint("get", "/health")` becomes `("GET", "/health")`. Its hash selects a bucket. No equal key exists, so `merge` inserts count `1`.
2. `("GET", "/orders")` selects some bucket and is inserted with `1`.
3. A new `Endpoint("GET", "/health")` is a different reference but is record-equal to the first key and has the same hash. Lookup reaches the same bucket, `equals` succeeds, and `Long::sum` produces `2`.

Conceptually, with capacity eight:

```text
bucket 0: empty
bucket 1: [Endpoint(GET,/orders) -> 1]
bucket 2: empty
bucket 3: [Endpoint(GET,/health) -> 2]
bucket 4: empty
...
```

Actual indexes are deliberately omitted because hash spreading and table state are implementation-sensitive. The map stores one entry per unique endpoint, not one per request. Temporary equal key objects created for repeated endpoints become unreachable after lookup. The boxed `Long` values are replaced as counts grow because `Long` is immutable.

Now consider a broken mutable key:

```java
final class MutableKey {
    String id;
    MutableKey(String id) { this.id = id; }
    public boolean equals(Object other) {
        return other instanceof MutableKey k && id.equals(k.id);
    }
    public int hashCode() { return id.hashCode(); }
}
```

After `map.put(key, value)`, changing `key.id` changes its lookup hash. `map.get(key)` can return null even though iteration still reveals the same reference. This violates the table's key-stability assumption, not the map's implementation.

## Complexity and performance

For `n` entries and a reasonable hash distribution:

| Operation | Expected | Collision-degraded | Notes |
|---|---:|---:|---|
| `get`, `put`, `remove` | `O(1)` | up to `O(n)` abstractly | tree bins may improve a particular implementation |
| `containsKey` | `O(1)` | up to `O(n)` | invokes hash/equality work |
| full iteration | `O(n + capacity)` typical | same form | oversized tables can hurt |
| resize | `O(n)` | `O(n)` | amortized across insertions |
| `HashSet` membership | same as backing hash table | same caveat | value is only a marker |

Hash computation itself may not be constant in key size. `String.hashCode` is proportional to string length on first computation in many implementations, though caching and compact-string representation are implementation concerns. Expensive `equals` also increases collision cost.

Space is `O(n + capacity)` plus entry/node overhead. Load factor changes the trade-off between empty buckets and collision depth. `LinkedHashMap` adds ordering links. `IdentityHashMap` uses reference identity and a specialized representation. `EnumMap` uses enum ordinals and is typically compact. Choose semantics first, then measure.

## Edge cases and common mistakes

- Overriding `equals` without a consistent `hashCode`.
- Mutating equality-relevant key state after insertion.
- Assuming unequal keys require different hash codes.
- Assuming iteration order or using printed `HashMap` order in golden tests.
- Using `get(k) == null` to prove absence when null values are permitted.
- Calling `containsValue` as though it were a hashed lookup; it generally scans values.
- Performing expensive or recursive side effects in `computeIfAbsent`.
- Assuming ordinary `HashMap.compute` is atomic across threads.
- Pre-sizing from an untrusted claimed count and allocating excessive memory.
- Using arrays as value keys without content-based wrappers; arrays inherit identity equality.
- Returning mutable lists stored as map values and calling the outer map immutable.
- Expecting tree bins to fix adversarial equality or expensive hash functions.
- Sharing a `HashMap` for concurrent mutation without synchronization.
- Forgetting that `HashSet` uniqueness is exactly the equality definition of its elements.

## Production engineering notes

Prefer immutable, small, domain-specific keys. Normalize once at the boundary and make normalization rules explicit. Avoid concatenated string keys when components can be represented by a record; concatenation risks ambiguity and repeated allocation.

Size maps from credible estimates, accounting for load factor rather than setting initial capacity equal to expected entries and assuming no resize. Avoid massive speculative capacities. Monitor cache/index cardinality, eviction, and skew; an unbounded map is often a memory leak by design.

For user-controlled keys, use current supported JDKs and validate request sizes. Hash flooding can convert an expected constant-time endpoint into CPU exhaustion. Tree bins help in common OpenJDK versions but do not replace input limits, timeouts, and observability.

Use `LinkedHashMap` when encounter or access order is a contract, `TreeMap` for ordered range queries, `ConcurrentHashMap` for shared concurrent mutation, `EnumMap` for enum keys, and identity maps only when reference identity is truly the model. A synchronized wrapper still requires external locking around compound iteration or multi-call invariants.

When publishing maps, specify whether values are deeply immutable. `Map.copyOf` freezes mappings, not mutable objects reachable from values. Be cautious with cached negative or null results; explicit wrapper types often make state clearer.

## Interview questions and model answers

**How does `HashMap.get` work?**

It computes a key hash, derives a candidate bucket, then checks entries in that bucket using hash and equality. Expected lookup is constant with good distribution; collisions require additional comparisons. Exact bucket and tree mechanics are implementation-specific.

**Why must equal objects have equal hash codes?**

The table uses the hash to choose where to search. If equal keys could select different regions, lookup could miss a logically identical stored key.

**What happens when a key changes after insertion?**

If equality or hashing changes, lookup searches using the new hash while the entry remains placed according to the old hash. Retrieval and removal can fail. Use immutable keys or stable identity fields.

**What is a collision? Is it an error?**

A collision is two unequal keys sharing a bucket or hash result. It is normal and must be resolved by equality checks. Excessive collisions hurt performance.

**Why is `HashMap` lookup not simply guaranteed O(1)?**

The API does not guarantee collision distribution, hash cost, or a particular bucket representation. Expected `O(1)` assumes suitable hashing and load. Worst-case abstract lookup can be linear.

**How is `HashSet` commonly implemented?**

It is commonly backed by a `HashMap`, storing set elements as keys and a shared private marker as the map value. That is an OpenJDK-style implementation fact, while the set contract only guarantees uniqueness.

**When would you use `computeIfAbsent`?**

For lazy per-key initialization such as grouping values into lists. The function should be short, return the desired initial value, and avoid structurally modifying the same map.

## Exercises

1. Implement a correct immutable `Coordinate` key manually with `equals` and `hashCode`, then compare it with a record.
2. Dry-run three colliding keys through insert, replacement, lookup, and removal.
3. Write a program that demonstrates the mutable-key failure. Repair it by making the key immutable.
4. Build a word-frequency map using `merge`, then return entries sorted by frequency without relying on hash iteration order.
5. Given one million expected entries, explain how load factor, key size, entry overhead, and capacity affect memory. Identify what must be measured rather than guessed.
6. Compare an outer `Map.copyOf` containing mutable lists with a deeply unmodifiable result.

## Chapter summary

Hash tables narrow key search through a hash and confirm identity through `equals`. Their correctness depends on consistent equality and hashing plus stable keys. Collision handling, capacity, load factor, and resizing produce expected constant-time operations under ordinary assumptions, not an unconditional guarantee. OpenJDK's power-of-two tables and tree bins are useful implementation knowledge but are version-sensitive. Production design requires bounded cardinality, deliberate key semantics, explicit order and null policies, and a concurrency strategy.

## Revision checklist

- [ ] I can state the `equals` and `hashCode` contract.
- [ ] I can derive lookup, insertion, collision handling, and resizing.
- [ ] I explain expected and amortized costs with assumptions.
- [ ] I know why mutable keys become unreachable by normal lookup.
- [ ] I distinguish absence from a null mapping.
- [ ] I can use `merge` and compute methods with their null semantics.
- [ ] I do not depend on `HashMap` or `HashSet` iteration order.
- [ ] I label tree bins, thresholds, hash spreading, and capacity shape as implementation details.
- [ ] I can select ordered, concurrent, enum, identity, or sorted alternatives by semantics.
