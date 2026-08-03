# Hash Tables from First Principles

Using `HashMap` well requires more than memorizing `put`, `get`, and `containsKey`. You should be able to explain how a key reaches a bucket, why two different keys may share that bucket, why resizing must rehash entries, and why changing a key after insertion can make an entry appear to vanish.

The goal is not to replace the JDK implementation. The goal is to build a small table once so the production API stops feeling magical. The complete educational implementation and executable checks live in `HashingInterviewChecks.java`.

## The lookup pipeline

For a key `k`, a hash table performs a pipeline like this:

```text
key
 |
 v
hashCode()          32-bit integer; different keys may produce the same value
 |
 v
hash spreading      mix high bits into low bits
 |
 v
bucket index        spread & (capacity - 1), when capacity is a power of two
 |
 v
collision chain     compare candidate keys with equals()
```

Hashing narrows the search; `equals` establishes identity within the bucket. A hash match alone never proves that keys are equal.

The companion uses separate chaining. Each bucket points to a linked sequence of nodes containing `key`, `value`, and `next`:

```text
buckets[0] -> null
buckets[1] -> (A,10) -> (Q,20) -> null
buckets[2] -> (B,30) -> null
buckets[3] -> null
```

`A` and `Q` colliding is correct behavior, not an exceptional condition.

## `put`: update or prepend

For `put(key, value)`:

1. Compute the current bucket index.
2. Walk that chain and compare keys with `Objects.equals`.
3. If the key exists, replace its value and return the previous value.
4. Otherwise, prepend a new node and increment `size`.
5. If the load threshold is exceeded, resize and rehash.

The important invariant is:

> Every stored node is reachable from the bucket computed from its key under the table's current capacity.

That last phrase explains both resizing and the mutable-key bug.

## Collision behavior and complexity

With a healthy distribution and controlled load, chains remain short and lookup is expected `O(1)`. In the worst case, many keys land in one bucket and a chained lookup is `O(n)`.

Do not say “`HashMap` is guaranteed `O(1)`.” A defensible interview answer is:

> Lookup and update are expected constant time under a reasonable hash distribution. Collision structure, resizing, key behavior, and implementation details affect the worst case.

The educational table intentionally keeps linked chains so degradation is visible. Modern JDK implementations have additional collision defenses, but those details are version-specific and belong in the Collections Internals material.

## Load factor and resizing

Load factor is `size / bucketCount`. The companion grows after `size > capacity * 0.75`.

Suppose capacity grows from 4 to 8:

```text
old index = spread & 0b0011
new index = spread & 0b0111
```

The extra mask bit can move an entry. Copying old bucket heads to the same indexes would violate the reachability invariant. Resizing therefore visits every node, recomputes its bucket under the new capacity, and links it into the new table.

One resize is `O(n)`, but geometric growth spreads that work across many inserts, giving expected amortized `O(1)` insertion. Pre-sizing can reduce resize pauses when the entry count is known.

## `equals` and `hashCode`: the key contract

For keys used in a hash table:

- if `a.equals(b)` is true, `a.hashCode()` and `b.hashCode()` must be equal;
- unequal keys may share a hash code;
- equality and hash results must remain consistent while the key is stored; and
- equality should be reflexive, symmetric, transitive, and consistent.

A Java `record` is often a good immutable value key because generated equality and hashing use its components. A custom class must deliberately choose which fields define logical identity.

### Broken contract example

If equality compares only `employeeId` but hashing includes `name`, two logically equal keys can enter different buckets. A lookup with the equal key searches the wrong chain and fails.

Correct both methods from the same stable identity fields:

```java
@Override
public boolean equals(Object other) {
    return other instanceof EmployeeKey key && employeeId == key.employeeId;
}

@Override
public int hashCode() {
    return Integer.hashCode(employeeId);
}
```

## The mutable-key disappearance

This bug surprises candidates because the node has not been deleted.

```text
1. key.id = 1; hash -> bucket 1
2. map.put(key, value) stores the node in bucket 1
3. key.id = 2; hash -> bucket 2
4. map.get(key) searches bucket 2 and finds nothing
5. size is still 1; the node is stranded in bucket 1
```

The same object reference is being used, but its lookup coordinates changed. A resize can make the behavior even less intuitive because the table rehashes the mutated key.

Use immutable keys, or at least fields that do not change while the key is stored. Returning a mutable collection from a key's identity is equally risky.

## Null, missing values, and API contracts

The educational table supports one logical null key through `Objects.hashCode(null) == 0` and `Objects.equals`. That is a policy choice, not a universal map rule. Different map implementations have different null contracts.

Even when null keys are supported, `get(key) == null` is ambiguous: the key may be missing or mapped to `null`. Use `containsKey` when the distinction matters, or avoid null values in the application contract.

For interview frequency maps, prefer:

```java
frequency.merge(value, 1, Integer::sum);
```

For index maps, a missing index must not be confused with index zero. Use `containsKey`, a nullable boxed result, or an explicit sentinel outside the valid domain.

## Library use versus internal implementation

| Interview situation | Recommended response |
|---|---|
| solve two-sum or frequency counting | use `HashMap`; explain expected complexity and key assumptions |
| interviewer asks how collisions work | draw buckets and a chain; explain hash then equality |
| interviewer asks to build a map | implement a scoped contract such as chained put/get/remove/resize |
| production service code | use a JDK/concurrent implementation matching thread and null requirements |
| mutable domain object is proposed as key | extract an immutable identifier or immutable key record |

Do not bring a custom table into ordinary algorithm solutions. It obscures the problem and is less tested than the standard library.

## Edge-case matrix

| Case | Expected handling | Frequent failure |
|---|---|---|
| two keys share a hash | retain both; distinguish with `equals` | overwriting by hash alone |
| updating an existing key | replace value; size unchanged | appending a duplicate node |
| removing chain head/middle/tail | reconnect the correct predecessor | dropping the rest of the chain |
| resize threshold crossed | allocate larger table and rehash every node | copying buckets without re-indexing |
| null key | follow an explicit implementation policy | assuming all maps behave like `HashMap` |
| null value | use `containsKey` to distinguish absence | interpreting `get == null` as definitely absent |
| mutable key | prohibit or document; prefer immutable key | key becomes unreachable |
| poor hash distribution | correctness remains; speed degrades | promising guaranteed constant time |
| `Integer.MIN_VALUE` hash | use masking/spreading, not `Math.abs(hash)` | `Math.abs(MIN_VALUE)` stays negative |
| concurrent mutation | choose a concurrent design | treating `HashMap` as thread-safe |

## Real interview follow-up round

**Interviewer:** If two keys have the same hash code, are they equal?

**Candidate:** No. Equal keys must share a hash code, but the reverse is not required. The table uses the hash to choose a bucket and `equals` to find the logical key inside that bucket.

**Interviewer:** Why use a power-of-two capacity?

**Candidate:** It permits a fast mask instead of modulo. A spread step matters because the mask reads low bits. The invariant is that capacity remains a power of two every time the table grows.

**Interviewer:** Why does resizing require `O(n)` work?

**Candidate:** The bucket index depends on capacity. When the mask changes, an existing key may map elsewhere, so every node must be placed under the new capacity. Geometric growth makes repeated insertion amortized expected constant time.

**Interviewer:** Your map returns null from `put`. Was the key absent, or was its old value null?

**Candidate:** This simplified return is ambiguous, like some map APIs. If the distinction is required, I would return a result object with a `found` flag or use a prior `containsKey` check. The contract must say which semantics callers receive.

**Interviewer:** Can we repair a map after somebody mutates a key?

**Candidate:** The reliable fix is preventing the mutation by using immutable key identity. Removing with the mutated key may fail for the same reason lookup fails. Rebuilding the entire table can rehash current key state, but that treats the symptom and does not make future mutations safe.

**Interviewer:** How would you test your table?

**Candidate:** I would force collisions with a constant-hash key, update and remove nodes at different chain positions, cross the resize threshold, compare behavior with `HashMap` on randomized operations, cover null according to the stated policy, and demonstrate the mutable-key hazard. The companion covers the structural cases and its production-facing algorithms still use `HashMap`.

## Run the verified companion

```bash
javac -Xlint:all -Werror HashingInterviewChecks.java
java HashingInterviewChecks
```

Expected final line:

```text
PASS 16 hashing checks
```

Continue with prefix-state problems only after the table contract is clear. Prefix sums explain *what* key should be stored; hash-table mechanics explain *how* those states are retrieved. Deep JDK treeification, iterator, and concurrency internals belong in the Java Collections Internals and Java Concurrency books.
