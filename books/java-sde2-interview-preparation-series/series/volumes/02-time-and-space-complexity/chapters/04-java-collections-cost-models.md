# Java Collections Cost Models for Interviews

## Learning objectives

This chapter connects the collection APIs learned in Java Fundamentals to interview complexity. You will choose a concrete implementation, qualify worst-case versus expected or amortized costs, include iteration and conversion work, and avoid common “all map operations are O(1)” mistakes.

## Start with behavior, then cost

Do not choose a collection from a complexity table alone. Ask:

1. Do duplicates matter?
2. Must insertion order or sorted order be preserved?
3. Do you need lookup by numeric index, by key, or by priority?
4. Where do insertions and removals occur?
5. Is a worst-case guarantee required, or is expected performance acceptable?
6. What equality, ordering, and mutation rules apply to elements or keys?

Only then compare costs.

## Cost vocabulary

- **Worst-case:** an upper bound for any valid input/state under the stated model.
- **Expected:** an average over a stated source of randomness or distribution. Hash-based collections are commonly described with expected O(1) lookup under a sound hash distribution, not a universal guarantee.
- **Amortized:** average cost per operation across a sequence, even if an occasional individual operation is expensive. Array growth is the standard example.
- **Output-sensitive:** includes the number `k` of returned or visited results.

## ArrayList: contiguous indexed sequence

Use `ArrayList` as the default general-purpose `List` when you need indexed access and append-heavy use.

| Operation | Typical asymptotic cost | Reason |
|---|---:|---|
| `get(i)`, `set(i, x)` | O(1) | direct indexed slot |
| append `add(x)` | amortized O(1) | occasional backing-array growth copies elements |
| insert/remove at index | O(n) worst case | later references shift |
| `contains`, `indexOf` | O(n) | linear equality checks |
| iterate | O(n) | visit n elements |
| copy into a new list | O(n) | n memberships copied |

```java
static void insertAtFront(List<Integer> values, int value) {
    values.add(0, value);
}
```

If `values` is an `ArrayList` containing `n` elements, the insertion shifts `n` references: O(n). The method signature accepts any `List`, so a production API cannot promise one implementation's performance unless the contract restricts the implementation.

### Why append is amortized O(1)

Most appends fill the next free slot. Occasionally capacity is exhausted and the implementation allocates a larger backing array and copies existing references.

For a growth sequence such as capacities 1, 2, 4, 8, ..., the total number of copied slots before reaching `n` is less than `2n`. Spread over `n` appends, average copying per append is constant. This does not mean every append is O(1); one resize can be O(n).

Pre-sizing with `new ArrayList<>(expectedSize)` can reduce resizing when a trustworthy bound is known. It does not change the asymptotic O(n) storage needed for n entries.

## LinkedList: nodes plus traversal

`LinkedList` implements both `List` and `Deque`, but it is not a universal faster replacement for `ArrayList`.

| Operation | Typical asymptotic cost |
|---|---:|
| `get(i)` | O(n) |
| find a value | O(n) |
| add/remove at a known end | O(1) |
| add/remove through a positioned iterator | O(1) after reaching position |
| reach a position by index/value | O(n) |
| iterate | O(n) |

The phrase “linked-list insertion is O(1)” omits how the insertion point is obtained. If a method first walks to index `i`, total work is O(n).

For interview queues and stacks, `ArrayDeque` is usually the clearer default. `LinkedList` also carries one node object per element and often has poorer cache locality; exact performance remains workload- and JVM-dependent.

## HashMap and HashSet: expected constant-time lookup

`HashMap<K,V>` stores key-value mappings; `HashSet<E>` uses the same hashing idea for unique elements.

Under a sound `hashCode` distribution and ordinary resizing assumptions:

- `put`, `get`, `containsKey`, `remove`: expected O(1);
- insert or lookup can degrade with collisions and implementation state;
- building from `n` entries is expected O(n);
- storing `u` distinct entries uses O(u) logical space.

Do not say “HashMap is guaranteed O(1).” Also do not invent one universal worst-case without naming the Java version, comparable-key conditions, table state, and collision behavior. Expected O(1), with a correctness requirement on `equals` and `hashCode`, is the safe fundamentals answer.

### Frequency map cost

```java
static Map<Integer, Integer> frequencies(int[] values) {
    Map<Integer, Integer> counts = new HashMap<>();
    for (int value : values) {
        counts.merge(value, 1, Integer::sum);
    }
    return counts;
}
```

Let `n` be array length and `u` the number of distinct values. Expected time is O(n); result space is O(u). The code boxes primitive values/counts. If value range is small and known, an `int[]` frequency table may use less overhead and give worst-case constant indexed updates.

### Equality and mutable keys

Hash collections locate a key using its hash and then equality. Equal keys must have equal hash codes. If fields used by `equals`/`hashCode` change after insertion, lookup and removal may fail because the object no longer maps to the expected bucket. Prefer immutable key state.

### Iteration is work too

Iterating a map's entries is at least O(n) in the number of mappings and can depend on implementation capacity. Do not label a method O(k) merely because it returns `k` selected entries if it first scans the whole map.

## LinkedHashMap and LinkedHashSet: encounter order

These structures preserve a defined encounter order while retaining hash-based lookup characteristics. Basic lookup remains expected O(1); iteration visits entries in encounter order; linked bookkeeping adds constant-factor memory and update work.

Use them when deterministic encounter order is part of the contract, not as a reflex for every map/set.

## TreeMap and TreeSet: sorted order

Tree-based sorted collections maintain elements/keys according to natural order or a comparator.

| Operation | Cost |
|---|---:|
| add/put/get/contains/remove | O(log n) |
| first/last/floor/ceiling style navigation | O(log n) |
| iterate all entries in order | O(n) |
| report a range of k entries | O(log n + k) as a useful model |

```java
static Integer smallestAtLeast(TreeSet<Integer> values, int target) {
    return values.ceiling(target);
}
```

This query is O(log n). A `HashSet` cannot answer it directly because it provides membership, not sorted navigation.

Comparator consistency matters: if comparison returns zero for values that are not intended to be the same set key, one may replace/suppress the other from the sorted collection's perspective.

## ArrayDeque: queue and stack ends

`ArrayDeque` supports adding/removing at both ends, with end operations amortized O(1).

```java
Deque<Integer> queue = new ArrayDeque<>();
queue.offerLast(10);
queue.offerLast(20);
int first = queue.removeFirst();

Deque<Integer> stack = new ArrayDeque<>();
stack.push(10);
stack.push(20);
int top = stack.pop();
```

A sequence of `n` enqueue/dequeue or push/pop operations is O(n) total. Searching for an arbitrary value is O(n). ArrayDeque does not permit `null`; that keeps `null` available as an empty-result signal for methods such as `poll` and `peek`.

Avoid legacy `Stack` for new interview code; its historical inheritance and synchronization rarely match the requested abstraction.

## PriorityQueue: access the next priority, not a sorted list

Java's default `PriorityQueue` is a min-priority queue.

| Operation | Cost |
|---|---:|
| `peek` | O(1) |
| `offer` | O(log n) |
| `poll` | O(log n) |
| find/remove an arbitrary value | O(n) |
| drain all values by repeated `poll` | O(n log n) |

```java
PriorityQueue<Integer> maximumFirst =
        new PriorityQueue<>(Comparator.reverseOrder());
```

For custom objects, use `Integer.compare`, `Long.compare`, or comparator composition rather than subtraction that can overflow.

```java
record Job(int priority, long sequence) {}

PriorityQueue<Job> jobs = new PriorityQueue<>(
        Comparator.comparingInt(Job::priority)
                  .thenComparingLong(Job::sequence));
```

Iteration over a `PriorityQueue` is **not** sorted order. Only the head is guaranteed. Poll repeatedly if ordered removal is required, accepting O(n log n) time and mutation of the queue (or copy first).

## Sorting, binary search, and conversion

### Sorting

- Sorting `n` values is commonly O(n log n), but exact worst-case and auxiliary-space guarantees depend on the Java overload and element type.
- `Collections.sort(list)` and `list.sort(comparator)` mutate the list.
- Copy before sorting when the caller's order must remain unchanged; the copy adds O(n) time and space.

### Binary search

`Arrays.binarySearch` or `Collections.binarySearch` is meaningful only when data is sorted under a compatible order. Search is O(log n) on an array or random-access list. On a sequential-access list, traversal can dominate even if comparison count is logarithmic. State the representation.

### Conversions and views

- `new HashSet<>(list)` is expected O(n) build time and O(u) entries.
- `new ArrayList<>(set)` is O(n) time and O(n) slots.
- `Arrays.asList(objectArray)` creates a fixed-size list backed by the array; it does not copy elements into an independent resizable list.
- `List.copyOf(source)` creates an unmodifiable list snapshot of the memberships and is O(n) in the ordinary analysis.
- `subList` is a view; changing the backing list structurally can invalidate it.

## Worked comparison: duplicate detection

### Baseline

```java
static boolean hasDuplicateQuadratic(int[] values) {
    for (int left = 0; left < values.length; left++) {
        for (int right = left + 1; right < values.length; right++) {
            if (values[left] == values[right]) return true;
        }
    }
    return false;
}
```

Worst-case time is O(n squared); auxiliary space is O(1).

### Hashing trade-off

```java
static boolean hasDuplicateExpectedLinear(int[] values) {
    Set<Integer> seen = new HashSet<>();
    for (int value : values) {
        if (!seen.add(value)) return true;
    }
    return false;
}
```

Expected time is O(n); auxiliary space is O(u), up to O(n). The solution also boxes integers. This is not universally “better”: tight memory, adversarial behavior, a tiny `n`, or a constrained value range can change the choice.

### Sorting trade-off

```java
static boolean hasDuplicateBySorting(int[] values) {
    int[] copy = Arrays.copyOf(values, values.length);
    Arrays.sort(copy);
    for (int index = 1; index < copy.length; index++) {
        if (copy[index - 1] == copy[index]) return true;
    }
    return false;
}
```

Time is dominated by sorting, commonly O(n log n). The copy makes the original safe and adds O(n) space. Sorting the input directly can reduce additional result storage but mutates caller data.

## Collection selection table

| Requirement | Starting choice | Important qualification |
|---|---|---|
| indexed sequence, append-heavy | `ArrayList` | middle shifts are O(n) |
| unique membership | `HashSet` | expected O(1), equality/hash contract |
| unique + insertion order | `LinkedHashSet` | extra linked bookkeeping |
| unique + sorted navigation | `TreeSet` | O(log n), comparator semantics |
| key/value lookup | `HashMap` | expected O(1), mutable keys unsafe |
| key/value + encounter order | `LinkedHashMap` | order is part of contract |
| sorted keys/range queries | `TreeMap` | O(log n), comparator semantics |
| FIFO or LIFO ends | `ArrayDeque` | no `null`; arbitrary search O(n) |
| repeatedly remove min/max | `PriorityQueue` | iteration is not sorted |

## Common mistakes

- Claiming all collection operations are O(1).
- Choosing by interface name without naming the concrete implementation.
- Saying linked lists make insertion O(1) while ignoring traversal.
- Forgetting `ArrayList.contains` is linear.
- Reporting HashMap worst-case behavior as a universal O(1) guarantee.
- Ignoring equality, hashing, ordering, boxing, or mutation contracts.
- Assuming PriorityQueue iteration is sorted.
- Using subtraction in a comparator.
- Calling `Arrays.asList(intArray)` a list of integers; it is a one-element list whose element is the whole primitive array.
- Forgetting that a copy or conversion is O(n).

## Quick check

1. Why is ArrayList append amortized rather than worst-case O(1)?
2. Why can LinkedList insertion still take O(n)?
3. Which qualifier belongs before O(1) HashMap lookup?
4. What is the difference between TreeSet membership and HashSet membership?
5. Why is PriorityQueue iteration not a sorted traversal?
6. When does a range query naturally use `k` in its bound?
7. What correctness requirement makes a mutable HashMap key risky?

## Practice

1. **Foundation:** Choose implementations for a resizable list, unique tags, FIFO tasks, and smallest-priority task.
2. **Foundation:** Analyze `for (int x : list) if (list.contains(x)) ...` for an ArrayList.
3. **Interview Core:** Compare three duplicate-detection solutions by time, space, mutation, and guarantees.
4. **Interview Core:** Design a frequency counter that preserves first-seen key order.
5. **Interview Core:** Analyze top-k selection using a heap of size `k`.
6. **Interview Core:** Explain the cost of copying and sorting a list before binary search.
7. **SDE-2 Follow-up:** Choose between HashMap and TreeMap for an API supporting point lookup plus inclusive key ranges.
8. **SDE-2 Follow-up:** Explain what evidence would justify pre-sizing a high-volume HashMap.

## Chapter summary

Choose collections by semantics first and complexity second. Name the concrete implementation, qualify expected and amortized costs, include traversal and copying, and state equality, order, mutation, and memory assumptions. That is the level of defensible reasoning expected in an SDE-2 interview.
