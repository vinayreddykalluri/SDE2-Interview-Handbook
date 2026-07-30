# Hashing Foundations: From Arrays to Maps and Sets

Hashing becomes useful only after the reader can answer three questions: what information must be remembered, what identifies that information, and what should happen when the same key appears again? This chapter builds those ideas before any advanced prefix-state pattern.

## Start with the problem, not `HashMap`

Suppose the input is `[4, 7, 4, 9]` and the interviewer asks how often each value occurs. A nested-loop solution repeatedly searches the array. A direct-address array works only when the key range is small and known. A map expresses the real requirement:

```text
key   -> stored information
4     -> 2 occurrences
7     -> 1 occurrence
9     -> 1 occurrence
```

A `Set` stores only membership. A `Map` stores a value for each key.

| Requirement | Java abstraction | Typical state |
|---|---|---|
| Have I seen this value? | `Set<T>` | membership |
| How often did it occur? | `Map<T, Integer>` | frequency |
| Where was it first seen? | `Map<T, Integer>` | earliest index |
| Which items belong together? | `Map<K, List<V>>` | grouping |
| Have I seen this prefix state? | `Map<Long, Integer>` | count or earliest index |

## The minimum Java API

```java
Map<String, Integer> counts = new HashMap<>();
counts.put("java", 1);
counts.put("java", counts.get("java") + 1);

boolean present = counts.containsKey("java");
int frequency = counts.getOrDefault("missing", 0);
counts.remove("java");
```

`put` replaces the previous value associated with an equal key. `containsKey` answers a membership question even if a map implementation permits a stored `null` value. `getOrDefault` is convenient for counting, but it does not insert the default.

The standard frequency update is:

```java
counts.merge(word, 1, Integer::sum);
```

For an interview, use the form you can explain without hesitation. `merge` is concise; `getOrDefault` makes the read-modify-write sequence more visible.

## What hashing does conceptually

A hash table uses a key's hash code to choose a candidate region, often called a bucket. It then uses equality to identify the matching key inside that region.

```text
key -> hash code -> candidate bucket -> equality checks -> entry
```

Different keys can produce the same hash code. That is a collision, not a correctness failure. Correctness depends on resolving collisions and checking equality. Performance depends on keys being distributed well enough that candidate regions remain small.

This is why the complexity statement is **expected O(1)** for common `HashMap` lookup and update operations, not a universal guarantee. State the input size, key behavior, and implementation boundary when an interviewer asks for a stronger claim.

## Equality is the identity policy

For Java objects used as hash keys:

1. Equal objects must produce equal hash codes.
2. Equality and the hash-relevant fields must remain stable while the key is stored.
3. Fields that define logical identity should participate consistently in both `equals` and `hashCode`.

A record is a good interview key when all components are immutable values:

```java
record Cell(int row, int column) {}

Set<Cell> visited = new HashSet<>();
visited.add(new Cell(2, 5));
System.out.println(visited.contains(new Cell(2, 5))); // true
```

Do not mutate a key field after insertion. The entry may remain in the bucket selected by the old hash while future lookups search using the new hash.

## Build the three basic templates

### Membership

```java
static boolean containsDuplicate(int[] values) {
    Set<Integer> seen = new HashSet<>();
    for (int value : values) {
        if (!seen.add(value)) {
            return true;
        }
    }
    return false;
}
```

`Set.add` returns `false` when an equal value is already present. The invariant is: before processing index `i`, `seen` contains exactly the values from indexes `[0, i)`.

### Frequency

```java
static Map<Integer, Integer> frequencies(int[] values) {
    Map<Integer, Integer> counts = new HashMap<>();
    for (int value : values) {
        counts.merge(value, 1, Integer::sum);
    }
    return counts;
}
```

### Earliest index

```java
static int firstRepeatedDistance(int[] values) {
    Map<Integer, Integer> firstIndex = new HashMap<>();
    int best = Integer.MAX_VALUE;
    for (int i = 0; i < values.length; i++) {
        Integer first = firstIndex.putIfAbsent(values[i], i);
        if (first != null) {
            best = Math.min(best, i - first);
        }
    }
    return best == Integer.MAX_VALUE ? -1 : best;
}
```

The choice between replacing, counting, and preserving the earliest index is part of the algorithm. It is not a cosmetic API decision.

## Prefix state from first principles

For an array, define `prefix[i]` as the sum of elements before index `i`. Then the sum of `[left, right]` is:

```text
prefix[right + 1] - prefix[left]
```

If the desired subarray sum is `target`, then at the current prefix `current` we need an earlier prefix equal to `current - target`.

Example for `[1, 2, 1]`, target `3`:

| Step | Value | Current prefix | Needed prefix | Earlier count | Added answers |
|---:|---:|---:|---:|---:|---:|
| seed | - | 0 | - | 1 | 0 |
| 0 | 1 | 1 | -2 | 0 | 0 |
| 1 | 2 | 3 | 0 | 1 | 1 |
| 2 | 1 | 4 | 1 | 1 | 1 |

The seed `{0 -> 1}` represents the empty prefix. Without it, subarrays that begin at index zero disappear from the count.

Use `long` for a prefix sum when the sum of valid `int` values can exceed the `int` range. The map key type must then be `Long`.

## Complexity and memory language

For `n` processed items and `u` distinct stored states:

- traversal work is expected O(n);
- map/set auxiliary space is O(u), worst-case O(n);
- boxing an `int` into `Integer` creates wrapper-level representation overhead that a primitive-specialized library could avoid, but standard Java interview solutions normally use the JDK collections;
- output collections are output space, not auxiliary space, when the output itself must contain those elements.

Never say that hashing makes a problem O(1). A single lookup may be expected O(1); processing `n` inputs is still expected O(n).

## Beginner failure clinic

- Using `map.get(key) == null` as the only presence test when stored nulls are permitted.
- Calling `get` before a count exists.
- Storing the current index when the algorithm needs the earliest index.
- Using a frequency map where a set is sufficient, or a set where counts are required.
- Forgetting that arrays use identity equality as keys unless wrapped in a value object.
- Mutating a key after insertion.
- Using `int` for a potentially overflowing prefix or answer count.
- Modifying a map structurally during enhanced-for iteration over its views.

## Foundation checkpoint

1. Why does a hash table still need equality after calculating a hash code?
2. When should `containsKey` be preferred over `get(key) != null`?
3. What state would you store for longest subarray length: count or earliest index?
4. Why is `{0 -> 1}` seeded for prefix-frequency counting?
5. What is the honest complexity statement for a `HashMap`-based scan?

If any answer feels memorized, trace one small input by hand before continuing to the pattern chapter.
