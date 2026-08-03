# Low-Level Collections Implementation Lab

The JDK collections are the right default for real applications and most interviews. Manual implementations belong here for a different reason: when you can build the small version, resizing, pointer rewiring, collision handling, heap repair, and comparator failures stop feeling like library magic.

This lab moves in four passes for each structure:

1. start with the public behavior;
2. draw the representation;
3. state the invariant;
4. trace the edge cases that break a plausible implementation.

The executable companion, `CollectionsImplementationChecks.java`, contains `MiniDynamicArray`, `MiniLinkedList`, `MiniHashMap`, `BinaryHeap`, and a comparator/sorting edge harness. They are educational implementations, not replacements for `ArrayList`, `LinkedList`, `HashMap`, or `PriorityQueue`.

## First decision: library or manual implementation?

| Situation | Best default | What to say |
|---|---|---|
| ordinary interview algorithm | JDK collection | name required semantics and cost assumptions |
| interviewer asks for internals | implement the requested core operations | state simplified scope before coding |
| production application | JDK or proven specialized library | correctness, interoperability, and maintenance dominate |
| learning the mechanics | small executable implementation | expose invariants and adversarial tests |

“Without the library” is not automatically the best production solution. It is the best *learning instrument* when the interviewer wants to see the mechanism.

## 1. Dynamic array: contiguous slots plus a logical size

A dynamic array stores elements in a fixed-length backing array and replaces that backing array when more capacity is needed.

```text
logical size = 3, capacity = 5

index       0       1       2       3       4
          +-------+-------+-------+-------+-------+
backing   |  "A"  |  "B"  |  "C"  | null  | null  |
          +-------+-------+-------+-------+-------+
            used    used    used    spare   spare
```

Do not confuse:

- `size`: the number of elements in the collection;
- `capacity`: the number of slots in the backing array.

### Invariants

For `MiniDynamicArray<E>`:

- `0 <= size <= elements.length`;
- valid element indexes are `[0, size)`;
- a valid insertion position is `[0, size]`;
- the live sequence is stored contiguously in `elements[0..size)`;
- unused slots do not contribute to logical contents.

That final distinction explains why `get(capacity - 1)` may be invalid even though a physical slot exists.

### Append with growth

The lab grows by roughly 1.5 times, with enough room for the requested minimum:

```java
private void ensureCapacity(int minimum) {
    if (minimum <= elements.length) {
        return;
    }
    int grown = elements.length + Math.max(1, elements.length / 2);
    elements = Arrays.copyOf(elements, Math.max(minimum, grown));
}
```

The exact JDK growth policy is an implementation detail and may differ. The reasoning does not depend on a sacred factor: growth must create spare capacity so every append does not copy the entire prefix.

### Inserting in the middle

Insert `B` at index 1 into `[A, C]`:

```text
before: [A, C, _, _]    size = 2
shift : [A, C, C, _]
write : [A, B, C, _]    size = 3
```

The moved count is `size - index`. The shift must move from the end toward the insertion point; `System.arraycopy` handles overlapping regions correctly.

### Removing and releasing the stale reference

Remove index 1 from `[A, B, C]`:

```text
shift left: [A, C, C, _]
clear tail: [A, C, _, _]    size = 2
```

Clearing the old tail slot matters for reference elements. Leaving `C` there would not change logical behavior, but the backing array would retain an object that the collection no longer owns.

### Cost model

| Operation | Cost | Reason |
|---|---:|---|
| `get`/`set` by valid index | `O(1)` | direct array address calculation |
| append without resize | `O(1)` | one write |
| one resizing append | `O(n)` | copy live prefix |
| append across a sequence | amortized `O(1)` | geometric growth spreads occasional copies |
| insert/remove near front | `O(n)` | shift suffix |

Amortized `O(1)` does not mean every append is constant time. It means the average charged cost across a long operation sequence remains bounded.

### Dynamic-array edge cases

| Edge | Tempting bug | Correct behavior |
|---|---|---|
| zero requested capacity | growth remains zero | keep a positive minimum backing capacity |
| append at `index == size` | reject as out of bounds | valid insertion position |
| read at `index == size` | return spare slot | reject; valid element indexes stop before size |
| middle insertion | overwrite one element | shift the suffix before writing |
| removal | leave stale tail reference | clear the vacated live slot |
| generic backing array | create `new E[]` | store in `Object[]` with one controlled cast |

## 2. Doubly linked list: nodes and four boundary links

A doubly linked node holds a value plus links in both directions:

```text
null <- [prev| A |next] <-> [prev| B |next] <-> [prev| C |next] -> null
          ^                                                   ^
        first                                                last
```

The collection stores `first`, `last`, and `size`. There is no contiguous backing array.

### Invariants

- an empty list has `first == null`, `last == null`, and `size == 0`;
- a non-empty list has `first.previous == null` and `last.next == null`;
- for adjacent nodes `x` and `y`, `x.next == y` and `y.previous == x`;
- walking from first reaches exactly `size` nodes and ends at last.

The executable check traverses the links after mutation. Testing returned values alone can miss a corrupted backward link.

### Insert between two nodes

To insert `B` between `A` and `C`:

```text
before: A <-> C

1. B.previous = A
2. B.next     = C
3. A.next     = B
4. C.previous = B

after:  A <-> B <-> C
```

Keep local references to predecessor and successor before rewiring. Updating one side and forgetting the reciprocal link creates a list that works in one direction and fails in the other.

### Remove a node

For the middle node `B`:

```java
predecessor.next = successor;
successor.previous = predecessor;
```

The endpoints need separate rules: removing first changes `first`; removing last changes `last`; removing the only node changes both. The lab's `unlink` method centralizes those branches.

### “Linked-list insertion is O(1)” needs a condition

Unlinking or inserting is constant time **when the node or iterator position is already available**. Finding index `i` is `O(n)`. A method that receives only an index must pay traversal cost first.

The lab walks from the closer end, but the asymptotic bound remains linear. Node allocation and weaker cache locality also matter in practice. This is why `LinkedList` is not automatically faster than `ArrayList` for insertion-heavy workloads.

### Linked-list edge cases

| Edge | Invariant at risk | Check |
|---|---|---|
| first insertion | both endpoints | first and last designate the same node |
| remove only node | both endpoints | both become null and size becomes zero |
| insert at front/back | outer null link | first.prev and last.next remain null |
| middle unlink | reciprocal links | predecessor.next and successor.prev agree |
| `clear` | retained node chain | release both links while walking |
| indexed access | hidden traversal | include reach cost in complexity |

## 3. Hash map: hash, bucket, equality, collision, resize

A hash map is not “an array indexed by the key.” It transforms a key's hash into a bucket index, then resolves possible collisions inside that bucket.

The lab uses power-of-two capacity and separate chaining:

```text
buckets
  0 -> null
  1 -> [key=C, value=3] -> [key=A, value=1] -> null
  2 -> [key=B, value=2] -> null
  3 -> null
```

Different keys may have the same hash and the same bucket. A collision is normal, not proof of a broken hash map.

### The lookup pipeline

1. obtain `hashCode` (`0` for the lab's supported null key);
2. spread high bits into lower bits;
3. calculate `hash & (capacity - 1)`;
4. traverse the selected chain;
5. match both stored hash and `Objects.equals(storedKey, requestedKey)`.

The hash narrows the search; equality identifies the key.

### Equality and hash contract

If `a.equals(b)` is true, `a.hashCode() == b.hashCode()` must also be true. The reverse is not required. Unequal keys may collide.

Do not mutate fields used by `equals` or `hashCode` while the key is stored. The entry remains in the bucket selected from the old hash; a later lookup using the new hash can search a different bucket.

### Collision dry run

The companion's `CollisionKey` deliberately returns hash `7` for every key.

```text
put key(1): bucket p -> [1]
put key(2): bucket p -> [2] -> [1]
get key(1): compare 2 (not equal), then 1 (equal)
```

This test proves that the map does not treat “same hash” as “same key.” A weak implementation that checks only hash codes silently overwrites unrelated entries.

### Update before resize

`put` first searches the current bucket for the key. If found, it replaces the value without changing size. Only a new key can cross the load threshold and trigger resize.

This order avoids resizing merely because an existing mapping was updated.

### Resize means re-bucketing, not only copying

When capacity doubles, the bucket index can change because the mask changes. Every node must be assigned using the new capacity:

```java
int newIndex = hash & (newCapacity - 1);
```

Copying each old bucket head to the same numeric index would lose entries during future lookup.

The educational implementation uses linked chains only. Modern JDK `HashMap` implementations can transform sufficiently large, eligible collision bins under particular conditions; those thresholds and implementation details must not be presented as the `Map` contract.

### Null and missing values

This mini-map deliberately supports one null key and null values. Because `get` returns null for both “missing” and “mapped to null,” use `containsKey` when that distinction matters. Other map implementations can have different null policies.

### Honest complexity answer

| Situation | Lookup/update |
|---|---:|
| well-distributed keys, maintained load | expected `O(1)` |
| linked collision chain of length `k` | `O(k)` |
| resize operation | `O(n)` |
| geometric resizing across many inserts | amortized expected constant insertion work, subject to hashing assumptions |

Do not say `HashMap` is guaranteed `O(1)`. Explain the key distribution, equality/hash stability, load, and collision representation behind the expectation.

### Hash-map edge cases

| Edge | Failure mode | Correct handling |
|---|---|---|
| two unequal equal-hash keys | overwrite by hash alone | verify equality inside bucket |
| existing key with null value | confused with missing key | use node search/`containsKey` |
| key update | size grows | replace value without adding entry |
| threshold crossed | old index reused | re-bucket every node |
| mutable key | later lookup fails | keep equality/hash state stable |
| poor hash distribution | long chains | state degraded cost honestly |
| capacity arithmetic | shift can overflow | production code guards a maximum capacity |

## 4. Binary min-heap: a partial order in an array

A min-heap keeps one promise: every parent is no greater than either child according to the comparator.

```text
array: [1, 3, 4, 7, 9]

            1 (index 0)
          /             \
     3 (index 1)      4 (index 2)
      /      \
 7 (3)      9 (4)
```

For zero-based index `i`:

```text
parent(i) = (i - 1) / 2, for i > 0
left(i)   = 2*i + 1
right(i)  = 2*i + 2
```

The heap is not globally sorted. Siblings can appear in either order.

### Offer: append, then sift up

1. append at the next array slot;
2. compare with its parent;
3. while it is smaller, move the parent down;
4. place the new value in the final hole.

The loop stops because the index moves strictly toward zero.

### Poll: move last to root, then sift down

1. save the root result;
2. remove the last element;
3. place it at root;
4. repeatedly choose the smaller child;
5. move that child up until the value fits.

Choosing an arbitrary child can preserve one edge while violating the other. For a min-heap, compare both children and choose the smaller one.

### Priority queue is not sorted iteration

`PriorityQueue.peek()` and repeated `poll()` respect priority. Its iterator does not promise sorted order. To produce ordered output without destroying the original queue, copy it and poll the copy.

### Heap cost model

| Operation | Cost | Reason |
|---|---:|---|
| `peek` | `O(1)` | root slot |
| `offer` | `O(log n)` worst case | path from leaf to root |
| `poll` | `O(log n)` worst case | path from root to leaf |
| search arbitrary value | `O(n)` | partial order cannot choose one search branch |
| poll every value | `O(n log n)` | repeated removal |

### Heap edge cases

| Edge | Tempting bug | Repair |
|---|---|---|
| empty poll | access slot zero | return the documented empty signal |
| one element | sift after size becomes zero | clear and stop |
| one-child parent | read missing right child | bounds-check before comparison |
| equal priorities | assume stable order | add an explicit sequence tie-break if needed |
| mutable priority | heap order becomes stale | remove/reinsert or keep priority stable |
| iteration | assume sorted | repeatedly poll when sorted order is required |

## 5. Comparator and sorting edge harness

A comparator must be consistent enough to define an ordering: the sign must reverse when arguments reverse, transitive comparisons must remain transitive, and comparing an item with itself must return zero.

### Subtraction is not comparison

This is broken for extreme values:

```java
Comparator<Integer> unsafe = (left, right) -> left - right;
```

`Integer.MIN_VALUE - Integer.MAX_VALUE` wraps to a positive value, so the comparator can claim the minimum is greater than the maximum.

Use:

```java
Comparator<Integer> safe = Integer::compare;
```

For objects, make every required tie-break explicit:

```java
Comparator<Candidate> byScoreThenName =
        Comparator.comparingInt(Candidate::score)
                .reversed()
                .thenComparing(Candidate::name);
```

A tie-break creates deterministic output and prevents distinct values from comparing as equal when the sorted structure needs to distinguish them.

`List.sort` is stable: elements comparing equal retain encounter order. `TreeSet` and `TreeMap`, however, use comparison equality to decide whether keys/elements are distinct. A comparator inconsistent with the intended identity can appear to “lose” entries.

## Executable validation

Compile and run the complete lab from the series root:

```bash
out=$(mktemp -d)
javac --release 21 -Xlint:all -Werror \
  -d "$out" \
  content/volumes/java/JAVA-05-collections-streams-and-io/code/CollectionsImplementationChecks.java
java -cp "$out" CollectionsImplementationChecks
```

Expected output:

```text
PASS 5 low-level collection implementation suites
```

The suites cover middle insertion and shifting, bidirectional link consistency, deliberate hash collisions, resize and re-bucketing, null policy, heap repair after every operation, integer comparator overflow, and deterministic tie-breaking.

## Interview room: realistic questions and model answers

### Why is `ArrayList.add` described as amortized `O(1)`?

**Model answer:** Most appends write one spare slot. Occasionally the backing array is full and append allocates a larger array and copies `n` elements. With geometric growth, those copies are sufficiently infrequent that total work across many appends is linear, giving amortized constant cost per append. One particular resizing append is still `O(n)`.

**Follow-up:** What changes if capacity grows by exactly one each time?

**Answer:** Appending `n` elements copies roughly `1 + 2 + ... + n`, which is quadratic total work. The amortized append bound becomes linear rather than constant.

### Is linked-list insertion always faster than array-list insertion?

**Model answer:** No. Rewiring a known node position is constant time, but finding an index is linear. Array shifting is linear, yet contiguous storage often has better locality and less allocation overhead. I choose from the access pattern and measured workload, not the slogan.

### What happens when two keys have the same hash code?

**Model answer:** They reach the same candidate bucket, then equality distinguishes them. A correct map retains both unequal keys. The hash narrows the search; it does not prove identity or equality.

**Follow-up:** Why should a key be stable while stored?

**Answer:** If fields used by hash code or equality change, lookup can select a different bucket or fail equality against the stored entry. The entry has not automatically moved to match the mutated key.

### Why does a hash-map resize rehash entries if stored hash codes remain the same?

**Model answer:** The bucket function also depends on capacity. With a power-of-two table, doubling adds a mask bit, so an entry can stay or move. Each entry must be assigned under the new bucket calculation.

### Why is priority-queue iteration not sorted?

**Model answer:** A heap maintains only parent-child order, enough to expose the root efficiently. The internal array is not a sorted sequence. Repeated polling performs the repairs needed to reveal successive priorities.

### How would you make equal priorities FIFO?

**Model answer:** Store priority plus a monotonically increasing sequence number and compare by priority first, sequence second. I would also define overflow and concurrency behavior for that sequence in production code.

### Why is `left.score - right.score` a dangerous comparator?

**Model answer:** Int subtraction can overflow and reverse the sign, violating ordering laws and causing incorrect sort or tree behavior. I use `Integer.compare` or comparator builders, then add a deterministic tie-break when required.

### Would you ship these mini implementations?

**Model answer:** No. They deliberately omit production concerns such as serialization, iterators and fail-fast policy, bulk APIs, spliterators, concurrency, capacity ceilings, JDK-compatible null/exception details, and extensive performance tuning. I use them to demonstrate invariants, then use the JDK collections for application code.

## Failure-injection checklist

Before calling an implementation complete, try inputs designed to attack its invariant:

- empty, one-element, and exactly-full dynamic arrays;
- insertion and removal at first, middle, last, and `size` boundaries;
- linked-list removal of the only node;
- equal-hash unequal keys and repeated updates to one key;
- a key mapped to null versus a missing key;
- a resize triggered inside one collision-heavy bucket;
- ascending, descending, equal, and extreme heap values;
- comparator inputs at `Integer.MIN_VALUE` and `Integer.MAX_VALUE`;
- equal primary sort keys with a required deterministic output.

## Practice ladder

1. **Foundation:** Hand-trace `MiniDynamicArray.add(1, value)` and state the moved count.
2. **Foundation:** Draw all links after removing the only linked-list node.
3. **Interview Core:** Implement `containsKey` so a mapped null is distinguishable from absence.
4. **Interview Core:** Add `remove` to the mini hash map without losing the rest of a collision chain.
5. **Interview Core:** Prove that the heap's `siftDown` loop terminates and preserves the untouched subtrees.
6. **Debugging:** Break the reciprocal linked-list update, then write a test that detects it.
7. **Debugging:** Replace re-bucketing with an array copy during resize and show the failing lookup.
8. **Debugging:** Sort integer extremes with subtraction, predict the wrong sign, and repair it.
9. **SDE-2 Follow-up:** Design an iterator modification policy and explain fail-fast limitations.
10. **SDE-2 Follow-up:** Add a stable priority tie-break while keeping the comparator contract.

## Chapter takeaway

The low-level skill is not memorizing source code. It is seeing the representation, naming the invariant, tracing the state change, and attacking the boundary where a shortcut fails. Once those mechanics are clear, the standard collections become easier to select, debug, and defend in an SDE-2 interview.
