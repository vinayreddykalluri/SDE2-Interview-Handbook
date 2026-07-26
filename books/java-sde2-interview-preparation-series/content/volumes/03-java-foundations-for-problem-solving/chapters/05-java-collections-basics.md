# Java Collections Basics for SDE-2 Interviews

## Learning objectives

By the end of this chapter, you can create and use the collection types expected in Java coding interviews, choose one from required behavior, iterate and modify safely, explain basic mutability and ordering contracts, and avoid the API traps that commonly break otherwise correct algorithms.

This is the usage chapter. The later Collections Architecture chapter explains views, ownership, optional operations, and design trade-offs more deeply. Hash-table internals, tree balancing, heap mechanics, and concurrent collections remain in their dedicated books.

## Start with the problem, not the class name

An array has fixed length. A collection manages a changing group of reference values through an interface. Generic type arguments state what may be stored.

```java
List<String> names = new ArrayList<>();
names.add("Ada");
names.add("Grace");
```

Read the declaration as: `names` follows the `List<String>` contract, and the current implementation is `ArrayList<String>`. Program to the interface unless a concrete implementation exposes behavior the method genuinely needs.

Use this first decision:

| Requirement | Start with | Common implementation |
|---|---|---|
| ordered sequence, duplicates allowed | `List<E>` | `ArrayList<E>` |
| uniqueness or membership | `Set<E>` | `HashSet<E>` |
| key-to-value association | `Map<K,V>` | `HashMap<K,V>` |
| first-in, first-out work | `Queue<E>` or `Deque<E>` | `ArrayDeque<E>` |
| last-in, first-out work | `Deque<E>` | `ArrayDeque<E>` |
| repeatedly remove best priority | `PriorityQueue<E>` | `PriorityQueue<E>` |
| retain insertion order | List, `LinkedHashSet`, `LinkedHashMap` | matching type |
| keep keys/elements sorted | `TreeMap`, `TreeSet` | matching type |

Do not select `LinkedList` because an interview slogan says insertion is fast. Insertion is only useful after reaching the position, and node-based storage has real locality and memory costs.

## List basics

A list preserves encounter order, permits duplicates, and supports positional access.

```java
List<Integer> scores = new ArrayList<>();
scores.add(70);                 // [70]
scores.add(90);                 // [70, 90]
scores.add(1, 80);              // [70, 80, 90]
int first = scores.get(0);      // 70
scores.set(0, 75);              // [75, 80, 90]
boolean hasNinety = scores.contains(90);
int size = scores.size();
```

Indexes are zero-based and form `[0, size())`. `get`, `set`, and indexed insertion throw `IndexOutOfBoundsException` for an invalid position.

The overloaded `remove` methods are a classic trap:

```java
List<Integer> values = new ArrayList<>(List.of(10, 20, 30));
values.remove(1);                    // removes index 1 -> value 20
values.remove(Integer.valueOf(30));  // removes the element 30
```

`ArrayList` is the default general-purpose list for interviews. `LinkedList` also implements `List` and `Deque`, but indexed access is linear. Use it only when its exact deque or iterator-position behavior is justified; `ArrayDeque` is normally clearer for queue/stack work.

## Set basics

A set stores no duplicate elements according to equality or ordering.

```java
Set<Integer> seen = new HashSet<>();
System.out.println(seen.add(4)); // true
System.out.println(seen.add(4)); // false
System.out.println(seen.contains(4)); // true
seen.remove(4);
```

`add` returning `false` is often useful while detecting duplicates.

```java
static boolean hasDuplicate(int[] numbers) {
    Set<Integer> seen = new HashSet<>();
    for (int number : numbers) {
        if (!seen.add(number)) return true;
    }
    return false;
}
```

- `HashSet` does not promise iteration order.
- `LinkedHashSet` preserves insertion encounter order.
- `TreeSet` keeps elements sorted according to natural order or a comparator and supports operations such as `floor`, `ceiling`, `lower`, and `higher`.

Set behavior depends on stable equality/hash or comparison. Mutating a stored element's key fields can make membership behavior incorrect.

## Map basics

A map associates unique keys with values. It is not a subtype of `Collection`.

```java
Map<String, Integer> attempts = new HashMap<>();
attempts.put("java", 1);
attempts.put("java", 2); // replaces the value for the same key

int value = attempts.get("java");
int missingAsZero = attempts.getOrDefault("dsa", 0);
boolean hasKey = attempts.containsKey("java");
attempts.remove("java");
```

Distinguish a missing key from a key mapped to null when the implementation permits null values: `get` returns null for both, while `containsKey` separates them. Prefer a domain contract that avoids ambiguous null when practical.

Frequency counting:

```java
static Map<String, Integer> frequencies(List<String> words) {
    Map<String, Integer> count = new HashMap<>();
    for (String word : words) {
        count.merge(word, 1, Integer::sum);
    }
    return count;
}
```

Useful update APIs:

- `putIfAbsent(key, value)` installs only when absent.
- `computeIfAbsent(key, function)` lazily creates a value, useful for grouping.
- `merge(key, value, function)` combines a new value with an existing one.
- `replace` changes only an existing mapping.

Grouping example:

```java
Map<Character, List<String>> groups = new HashMap<>();
for (String word : List.of("array", "api", "map")) {
    groups.computeIfAbsent(word.charAt(0), ignored -> new ArrayList<>())
          .add(word);
}
```

Iteration choices:

```java
for (Map.Entry<String, Integer> entry : attempts.entrySet()) {
    String key = entry.getKey();
    int count = entry.getValue();
}
```

Use `entrySet` when both key and value are needed; it avoids a second lookup and communicates the mapping traversal directly.

- `HashMap` does not promise iteration order.
- `LinkedHashMap` preserves insertion order, or access order when configured for specialized use.
- `TreeMap` keeps keys sorted and supports range/nearest-key navigation.

## Queue and deque basics

A queue models FIFO work. A deque supports both ends and can model both a queue and a stack.

Prefer the non-throwing queue method family in interview algorithms:

| Operation | Special-value form | Throwing form |
|---|---|---|
| insert | `offer` | `add` |
| remove head | `poll` | `remove` |
| inspect head | `peek` | `element` |

FIFO with `ArrayDeque`:

```java
Deque<String> queue = new ArrayDeque<>();
queue.offerLast("first");
queue.offerLast("second");
System.out.println(queue.pollFirst()); // first
```

LIFO with `ArrayDeque`:

```java
Deque<Integer> stack = new ArrayDeque<>();
stack.push(10);
stack.push(20);
System.out.println(stack.pop()); // 20
```

Use consistent ends. Mixing `push`, `offerLast`, `pollFirst`, and `removeLast` without a written convention makes traces difficult. `ArrayDeque` rejects null, which keeps null available as the empty-result signal for `poll` and `peek`.

Avoid legacy `Stack` for new interview code. `ArrayDeque` provides the needed stack contract without inheriting the old synchronized `Vector` API.

## PriorityQueue basics

`PriorityQueue` exposes the element with the smallest natural order by default.

```java
PriorityQueue<Integer> minimums = new PriorityQueue<>();
minimums.offer(9);
minimums.offer(3);
minimums.offer(5);

System.out.println(minimums.peek()); // 3
System.out.println(minimums.poll()); // 3
```

A max-heap reverses the comparator:

```java
PriorityQueue<Integer> maximums =
        new PriorityQueue<>(Comparator.reverseOrder());
```

For a custom type, compare without subtraction:

```java
record Candidate(String name, int score) {}

Comparator<Candidate> byHighestScore =
        Comparator.comparingInt(Candidate::score)
                  .reversed()
                  .thenComparing(Candidate::name);
```

The queue promises only the head after each operation. Iterating the priority queue is not sorted traversal. Poll a defensive copy when sorted output is required and the original must remain intact.

## Iteration and safe removal

Enhanced-for is best when no index or structural mutation is needed:

```java
for (String name : names) {
    System.out.println(name);
}
```

Do not structurally modify an ordinary collection directly inside enhanced-for. Use one of these contracts:

```java
values.removeIf(value -> value < 0);

for (Iterator<Integer> iterator = values.iterator(); iterator.hasNext();) {
    if (iterator.next() < 0) iterator.remove();
}
```

`ConcurrentModificationException` is a best-effort bug signal, not a thread-safety mechanism.

## Factories, copies, and mutability

These declarations do not mean the same thing:

```java
List<String> mutable = new ArrayList<>(List.of("a", "b"));
List<String> unmodifiable = List.of("a", "b");
List<String> fixedSize = Arrays.asList("a", "b");
List<String> snapshot = List.copyOf(mutable);
List<String> readOnlyView = Collections.unmodifiableList(mutable);
```

- `mutable` permits structural and element replacement.
- `List.of` is unmodifiable and rejects null.
- `Arrays.asList` is fixed-size; `set` works but `add` and `remove` do not.
- `List.copyOf` creates unmodifiable membership not backed by later source changes.
- `unmodifiableList` is a view; mutation through another alias remains visible.

None of these operations makes mutable element objects deeply immutable.

## Sorting and conversion basics

```java
List<Integer> numbers = new ArrayList<>(List.of(4, 1, 3));
numbers.sort(Integer::compare);
Collections.reverse(numbers);

int[] primitiveArray = numbers.stream().mapToInt(Integer::intValue).toArray();
List<Integer> boxed = Arrays.stream(primitiveArray).boxed().toList();
```

`toList()` returns an unmodifiable list in current Java. Use `new ArrayList<>(...)` when a mutable result is required. `Arrays.asList(primitiveArray)` produces a one-element `List<int[]>`, not a list of boxed integers.

## Complexity intuition, not folklore

Interfaces do not promise one complexity. State the implementation and qualifier.

| Operation | Common implementation | Interview-level cost |
|---|---|---:|
| indexed get/set | `ArrayList` | O(1) |
| append | `ArrayList` | amortized O(1) |
| insert/remove at front | `ArrayList` | O(n) shifting |
| indexed get | `LinkedList` | O(n) |
| membership | `HashSet` | expected O(1) |
| key lookup | `HashMap` | expected O(1) |
| ordered lookup | `TreeSet`/`TreeMap` | O(log n) |
| deque end operation | `ArrayDeque` | amortized O(1) |
| heap offer/poll | `PriorityQueue` | O(log n) |
| heap peek | `PriorityQueue` | O(1) |

The Time and Space Complexity volume explains how to derive and qualify these costs. The collection-internals volume explains resizing, hashing, trees, node overhead, and implementation behavior.

## Interview checklist

Before choosing a collection, state:

1. Are duplicates allowed?
2. Does encounter or sorted order matter?
3. Is the operation positional, membership, key lookup, FIFO, LIFO, or priority?
4. Can inputs or returned data be mutated?
5. Are nulls valid?
6. Which operation dominates and at what scale?
7. Does equality or comparator state remain stable?

## Common mistakes

- Declaring `ArrayList` everywhere instead of the useful interface.
- Forgetting `Map` is not a `Collection`.
- Confusing `remove(index)` and `remove(value)` for `List<Integer>`.
- Depending on HashMap/HashSet iteration order.
- Assuming LinkedList is automatically faster for insertion.
- Using Stack instead of ArrayDeque.
- Treating PriorityQueue iteration as sorted.
- Writing comparator subtraction that can overflow.
- Modifying a collection directly in enhanced-for.
- Confusing fixed-size, unmodifiable, snapshot, view, and deeply immutable.
- Using mutable equality/hash fields for set elements or map keys.
- Claiming HashMap operations are guaranteed O(1).

## Practice

1. **Foundation:** Use ArrayList to insert, replace, remove by index, and remove by value.
2. **Foundation:** Deduplicate input while preserving first-seen order.
3. **Interview Core:** Build a frequency map, then return the most frequent key with a deterministic tie-breaker.
4. **Interview Core:** Implement FIFO and LIFO behavior with two separate ArrayDeque instances.
5. **Interview Core:** Maintain the largest `k` values with a size-`k` min-heap.
6. **Interview Core:** Demonstrate the difference among List.of, Arrays.asList, List.copyOf, and an unmodifiable view.
7. **SDE-2 Follow-up:** Define an API returning ordered results without exposing mutable internal membership.
8. **SDE-2 Follow-up:** Compare ArrayList, HashSet, and TreeSet for repeated membership queries, including build cost and ordering.

## Chapter summary

Choose collections from semantics: sequence, uniqueness, association, FIFO/LIFO, or priority. Use ArrayList, HashSet, HashMap, ArrayDeque, and PriorityQueue as the normal interview starting points, then select linked or sorted variants only for a stated contract. Know the everyday APIs, ownership/mutability differences, safe iteration rules, null and equality boundaries, and qualified costs before studying internals.
