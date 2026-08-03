# 27. HashMap, HashSet, and Hashing Internals

## The entry that vanished

```java
Map<CartKey, Cart> carts = new HashMap<>();
CartKey key = new CartKey("cart-91", "OPEN");
carts.put(key, cart);

key.setStatus("PAID");        // somewhere else, later

carts.get(key);               // null
carts.remove(key);            // does nothing
carts.size();                 // still 1
for (var e : carts.entrySet()) { ... }   // the entry is right there
```

The entry was never deleted. `get` is looking in the wrong place.

`CartKey.hashCode` reads both fields. Changing `status` changed the hash, and the hash decides *which bucket to search*. The entry sits in the bucket computed from `"OPEN"`; the lookup searches the bucket computed from `"PAID"`. This chapter is about that machinery - because once you can compute the bucket yourself, most `HashMap` interview questions stop being memorisation.

## Key to bucket, with real numbers

Three steps, and only the first is a language contract.

![Figure 27.1 - From key to bucket, with real numbers](assets/diagrams/18-hash-to-bucket.png)

1. **`hashCode()`** - for `String` the API fixes this exactly: `s[0]*31^(n-1) + ... + s[n-1]`. `"alice"` gives `92903040`. You can compute it by hand.
2. **Spread: `h ^ (h >>> 16)`** - OpenJDK, not a contract. `92903040` becomes `92902153`.
3. **Mask: `hash & (capacity - 1)`** - capacity is always a power of two, so this is a cheap bitwise substitute for modulo. At capacity 16, `"alice"` lands in bucket 9.

Step 2 exists because step 3 throws away every high bit. Two keys differing only above bit 4 would collide every single time. XOR-ing the top half down lets those bits influence the index.

Once inside the bucket, `hashCode` is finished and **`equals` decides**. The hash narrows the search; equality determines identity. That division of labour is the whole contract:

> If `a.equals(b)`, then `a.hashCode() == b.hashCode()`.
> The converse is *not* required - unequal objects may share a hash. That is a collision, and it is normal, not an error.
> And repeated `hashCode` calls must agree while equality-relevant state is unchanged.

> **Specification boundary:** only step 1 is a contract, and only for types that document their
> `hashCode`. The spreading function, the power-of-two capacity, the mask, the load factor, tree bins,
> and the resize split are all OpenJDK implementation. The API guarantees no bound on `get` at all -
> "expected `O(1)`" is a property of the implementation plus your keys, not of `Map`.

A class that overrides `equals` without `hashCode` breaks the first rule, and the symptom is exactly the vanished entry: two objects that are equal land in different buckets and never meet.

## Collisions, and why "O(1)" needs a qualifier

Real keys collide. Of six names at capacity 16, `frank` and `mallory` both land in bucket 0, and `grace` and `heidi` both land in bucket 8. A bucket therefore holds a small chain, and a lookup walks it comparing hashes first and then `equals`.

So the honest statement is **expected `O(1)`**, conditional on:

- a hash that spreads the actual key population, and
- keys whose hash does not change while stored.

When a bucket grows past a threshold *and* the table is large enough, OpenJDK converts that bucket from a list to a red-black tree, so a pathological bucket degrades to `O(log n)` instead of `O(n)`. Bins can convert back. This is resilience against badly distributed or deliberately crafted keys - not permission to write a poor `hashCode`.

> **HotSpot note:** treeification and untreeification thresholds, the minimum table capacity required before treeifying, the tie-break used when keys are not `Comparable`, and node layouts are all version-sensitive. The Java API guarantees none of it.

## What resizing actually does

When size crosses `capacityxloadFactor` (0.75 by default), the table doubles. The common mental model - "everything is rehashed and redistributed" - is wrong, and the real answer is more useful.

![Figure 27.2 - What resizing actually does to a bucket](assets/diagrams/19-hashmap-resize-split.png)

Because the mask is `hash & (capacity - 1)` and capacity doubles, exactly one additional bit becomes significant. So an entry at index `i` either **stays at `i`** or **moves to `i + oldCapacity`**, decided by a single bit test:

```text
(hash & oldCapacity) == 0  ->  stay at i
otherwise                  ->  move to i + oldCapacity
```

Nothing is rehashed. Relative order within a bucket is preserved - which is why the modern algorithm cannot produce the infinite lookup loop the pre-Java-8 implementation could when a `HashMap` was mutated concurrently.

Note what the figure shows. `frank` and `mallory` shared bucket 0 and separated cleanly; `grace` and `heidi` shared bucket 8 and *both* moved to 24. Their hashes agree on more than the one bit the resize tests. **Resizing relieves collisions statistically; it does not guarantee to break any particular one.**

Sizing up front avoids the churn. For an expected `n` entries, construct with `new HashMap<>((int) (n / 0.75f) + 1)` - the argument is capacity, not entry count, so passing `n` still resizes.

A low load factor spends memory to shorten chains; a high one does the reverse. 0.75 is a general compromise, not an optimum for your workload.

## Back to the vanished entry

![Figure 27.3 - The entry is still there. The lookup is looking elsewhere.](assets/diagrams/20-hashmap-mutated-key.png)

With `Objects.hash("cart-91", status)`, the `"OPEN"` key masks to bucket 0 and the `"PAID"` key masks to bucket 14. The entry is stranded: unreachable by key, perfectly visible by iteration.

The part that makes this expensive to diagnose is that **it does not fail every time.** Over 200,000 random two-field keys at capacity 16, changing one field moved the bucket in 93.7% of cases - almost exactly the 93.8% chance-level expectation. So roughly one lookup in sixteen still succeeds. A bug that always fails is found in an afternoon. A bug that works 6% of the time gets blamed on the network.

The rule is unconditional: **any field read by `hashCode` or `equals` must not change while the object is in use as a key.** Records make that structural, which is why they are the best default key type. The same argument rules out mutable collections as keys - their hash changes as their contents do.

## Null, absence, and defaults

`HashMap` permits one null key and any number of null values. That makes `get` ambiguous:

```java
map.get(k)                    // null: absent, or present-with-null-value?
map.containsKey(k)            // the only way to distinguish
map.getOrDefault(k, fallback) // returns the STORED null if the mapping exists
```

`getOrDefault` uses the default only when there is no mapping at all. `putIfAbsent` treats a null mapping as absent. Read each contract rather than assuming a family resemblance - and better, do not store nulls. If absence is meaningful, model it.

## `compute`, `merge`, and `computeIfAbsent`

These replace get-check-put, and they do it in a single lookup:

```java
// three lookups, and racy even on a concurrent map
Integer n = counts.get(word);
counts.put(word, n == null ? 1 : n + 1);

// one lookup, and atomic per key on ConcurrentHashMap
counts.merge(word, 1, Integer::sum);
```

`computeIfAbsent` is the idiomatic multimap builder:

```java
index.computeIfAbsent(customerId, ignored -> new ArrayList<>()).add(order);
```

Two behaviours to know. Mapping a key to `null` through `compute` or `merge` **removes** the entry rather than storing null. And the mapping function must not structurally modify the same map - doing so can corrupt the table, and modern JDKs will usually throw `ConcurrentModificationException` rather than let you.

These methods make a compound update concise. They do not make an ordinary `HashMap` thread-safe; only the concurrent implementations define per-key atomicity.

## Views and iteration

`keySet`, `values`, and `entrySet` are live views - removing through one removes the mapping. Iterate `entrySet` when you need both halves; a loop over `keySet` calling `map.get(key)` pays for a second lookup per element.

There is no stable iteration order. Even when a run looks consistent, a resize, a JDK upgrade, or different input can change it. And `Map.Entry` objects yielded by iteration may be tied to the map - use `Map.entry(k, v)` or a domain record if you need to keep a pair.

## Worked example: a frequency index with a safe key

```java
import java.util.*;

record TenantKey(String tenantId, String region) { }   // immutable by construction

final class RequestCounter {
    private final Map<TenantKey, Long> counts = new HashMap<>();

    void record(String tenantId, String region) {
        counts.merge(new TenantKey(tenantId, region), 1L, Long::sum);
    }

    Map<TenantKey, Long> snapshot() {
        return Map.copyOf(counts);
    }

    List<Map.Entry<TenantKey, Long>> top(int k) {
        return counts.entrySet().stream()
                .sorted(Map.Entry.<TenantKey, Long>comparingByValue().reversed()
                        .thenComparing(entry -> entry.getKey().tenantId()))
                .limit(k)
                .map(entry -> Map.entry(entry.getKey(), entry.getValue()))
                .toList();
    }
}
```

Why each choice:

- `record` gives value-based `equals` and `hashCode` and no setters, so the key cannot mutate underneath the map.
- `merge` does one hash and one bucket walk instead of three.
- `top` adds a tie-break on `tenantId`. Without it, two tenants with equal counts order arbitrarily, and a paginated report can repeat or skip rows between pages.
- `Map.entry(...)` detaches the pairs from the map before they escape.
- `Map.copyOf` is safe here precisely because `snapshot` promises nothing about iteration order - the opposite of the situation in Chapter 25.

## Complexity and memory

| Operation | Expected | Worst case | Notes |
|---|---|---|---|
| `get`, `put`, `remove`, `containsKey` | `O(1)` | `O(log n)` with treeified bins | `O(n)` if all keys collide and are not `Comparable` |
| iteration | `O(capacity + size)` | same | a large sparse table costs more to iterate than it holds |
| resize | `O(n)`, amortised over the inserts that triggered it | - | one bit test per entry, no rehashing |

That iteration cost is a real trap: a map sized for a million entries but holding a hundred still walks a million-slot table. `HashSet` is a `HashMap` with a constant value, so every statement above applies to it unchanged - including that `add` returns `false` for an element already present.

## Edge cases and common mistakes

- Overriding `equals` without `hashCode`, or the reverse.
- Mutating a field that `hashCode` or `equals` reads while the object is a key.
- Depending on iteration order.
- Sizing with `new HashMap<>(n)` where `n` is the expected entry count rather than the capacity.
- Using `get` to test presence when null values are stored.
- Structurally modifying the map inside a `computeIfAbsent` mapping function.
- Reading a `null` result from `merge` or `compute` as "stored" when it means "removed".
- Assuming tree bins make a bad `hashCode` free; they cap the damage, they do not remove it.
- Iterating `keySet` and calling `get` instead of iterating `entrySet`.
- Retaining an iteration-supplied `Map.Entry` as if it were an independent record.
- Sharing a plain `HashMap` across threads - concurrent mutation can corrupt it.
- Assuming hash codes are stable across JVM runs; `Object.hashCode` is not.
- Using a mutable collection as a key.

## Production engineering notes

Prefer immutable keys; records and enums are the strongest defaults. Size the map when the workload is known. Choose an alternative deliberately when semantics demand it: `LinkedHashMap` for encounter or access order - and it is the natural base for an LRU cache; `TreeMap` for range and neighbour queries; `EnumMap` for enum keys; `IdentityHashMap` when reference identity is genuinely what you mean; `WeakHashMap` for keys that must not prevent collection; `ConcurrentHashMap` for shared mutation.

Where keys derive from untrusted input, remember that collisions can be induced deliberately to force worst-case behaviour. Tree bins mitigate that; bounding the number of distinct keys you accept mitigates it further.

Instrument unusually large maps. A cache without an eviction policy is an out-of-memory incident with a delay on it.

## Interview questions and model answers

**Is `HashMap.get` O(1)?**

Expected `O(1)`, given a hash that distributes the actual keys and keys whose hash does not change while stored. A degenerate bucket is `O(log n)` once OpenJDK treeifies it, and `O(n)` if the keys are not `Comparable`.

**Walk me from a key to a bucket.**

`hashCode()` - fixed by the API for `String`. Then OpenJDK's spread, `h ^ (h >>> 16)`, which mixes high bits down because the next step discards them. Then `hash & (capacity - 1)`, a mask rather than a modulo because capacity is a power of two. Inside the bucket, `equals` decides.

**What happens on resize?**

Capacity doubles, so one more hash bit becomes significant. Each entry either stays at index `i` or moves to `i + oldCapacity`, decided by `hash & oldCapacity`. Nothing is rehashed, and intra-bucket order is preserved - which is what makes it safe against the old concurrent-resize loop.

**Why must `equals` and `hashCode` agree?**

The hash chooses where to look; equality decides what matches. If two equal objects hash differently they land in different buckets, can never be compared, and the map behaves as if the entry is absent.

**What breaks if a key mutates after insertion?**

Its computed bucket changes, so the entry becomes unreachable by key while still counting in `size` and appearing in iteration. Measured on two-field keys at capacity 16, one field change moved the bucket about 94% of the time - so it fails intermittently, which is worse than failing always.

**`get` returned null. What do you know?**

Nothing conclusive: the key may be absent, or present with a null value. Only `containsKey` distinguishes them, which is one good reason not to store nulls.

## Exercises

1. Compute `"bob".hashCode()` by hand from the specified formula, then its spread value and its index at capacity 16. Validate your method against `"alice"`: `92903040->92902153->9`.
2. Take six keys of your own. Compute their capacity-16 indexes, list the collisions, then compute the capacity-32 indexes and identify which collisions survived the resize and why.
3. Write a class with a mutable field used by `hashCode`. Put it in a map, mutate it, then record what `get`, `remove`, `size`, and iteration each report.
4. Rewrite a get-check-put counter using `merge`. State what changes on a `ConcurrentHashMap` and what does not on a `HashMap`.
5. Build a `HashMap` with capacity 1,048,576 holding ten entries. Time a full iteration against a properly sized map and explain the result from the iteration cost formula.
6. Show that `Map.copyOf` does not promise iteration order, and name a method contract it would therefore be wrong to implement with.
7. Explain why `HashSet` needs no separate analysis in this chapter.

## Chapter summary

A hash map turns a key into a *region* to search rather than scanning every key, and the three steps are worth being able to perform by hand: `hashCode` (a contract for `String`), OpenJDK's `h ^ (h >>> 16)` spread, and a power-of-two mask. Inside the bucket, `equals` decides - which is why the two must agree, and why overriding one alone produces entries that are present but unreachable. Collisions are normal, so complexity is *expected* `O(1)`, with treeified bins capping the bad case at `O(log n)` for `Comparable` keys. Resizing rehashes nothing: doubling makes one more bit significant, so each entry stays at `i` or moves to `i + oldCapacity`, and collisions agreeing on more than that bit survive it. The failure mode to internalise is the mutated key - changing any field `hashCode` reads relocates the entry's computed bucket while leaving the entry itself in place, and it fails roughly fifteen times in sixteen rather than always, which is exactly what makes it expensive to find.

## Revision checklist

- [ ] I can take a `String` key and compute its bucket index by hand.
- [ ] I can explain why the spread step exists in terms of what the mask discards.
- [ ] I can state the `equals`/`hashCode` contract in both directions and say which one collisions do not violate.
- [ ] I say "expected `O(1)`" and can name both conditions it rests on.
- [ ] I can describe resize as a one-bit split rather than a rehash.
- [ ] I know why a mutated key fails intermittently rather than always.
- [ ] I can distinguish absent from present-with-null, and know what `getOrDefault` really does.
- [ ] I know when to reach for `LinkedHashMap`, `TreeMap`, `EnumMap`, `IdentityHashMap`, `WeakHashMap`, or `ConcurrentHashMap` instead.
