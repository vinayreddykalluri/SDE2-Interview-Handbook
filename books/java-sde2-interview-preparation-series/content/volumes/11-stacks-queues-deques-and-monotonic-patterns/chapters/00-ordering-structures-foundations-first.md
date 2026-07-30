# Stack, Queue, and Deque Foundations Before Monotonic Patterns

These structures are not distinguished by what they store. They are distinguished by which stored item is accessible next.

## Three access policies

```text
Stack: last in, first out       push -> [newest ... oldest] -> pop
Queue: first in, first out      add  -> [oldest ... newest] -> remove
Deque: both ends are available  front <-> [items] <-> back
```

A stack fits nested work and most-recent unresolved state. A queue fits arrival order and breadth-first layers. A deque fits work at both boundaries and can implement either policy.

## Use `ArrayDeque` deliberately

For new interview code, prefer `ArrayDeque` over the legacy `Stack` class.

| Role | Insert | Inspect | Remove |
|---|---|---|---|
| Stack at front | `push(x)` | `peek()` | `pop()` |
| Queue | `addLast(x)` | `peekFirst()` | `removeFirst()` |
| Deque front | `addFirst(x)` | `peekFirst()` | `removeFirst()` |
| Deque back | `addLast(x)` | `peekLast()` | `removeLast()` |

The throwing forms (`removeFirst`, `pop`) fail on empty input. The nullable forms (`pollFirst`, `peek`) return null. `ArrayDeque` does not permit null elements, which keeps null available as an empty-result signal.

```java
Deque<Integer> stack = new ArrayDeque<>();
stack.push(10);
stack.push(20);
System.out.println(stack.pop()); // 20

Deque<Integer> queue = new ArrayDeque<>();
queue.addLast(10);
queue.addLast(20);
System.out.println(queue.removeFirst()); // 10
```

Do not mix ends without naming the policy. `addLast` followed by `removeLast` is a stack; `addLast` followed by `removeFirst` is a queue.

## Stack invariant: unresolved nested state

For delimiter validation, the stack holds opening delimiters that have not yet found a matching close. The top must match the next closing delimiter.

```java
static boolean balanced(String text) {
    Deque<Character> openings = new ArrayDeque<>();
    for (char token : text.toCharArray()) {
        if (token == '(' || token == '[' || token == '{') {
            openings.push(token);
        } else if (token == ')' || token == ']' || token == '}') {
            if (openings.isEmpty() || !matches(openings.pop(), token)) {
                return false;
            }
        }
    }
    return openings.isEmpty();
}
```

The final emptiness check matters: `"(("` never produces a mismatched closer, but remains incomplete.

## Queue invariant: discovered but not processed

In breadth-first search, the queue contains discovered nodes whose outgoing relationships have not yet been processed. Mark a node visited when enqueuing, not when dequeuing, to prevent multiple parents from adding the same node repeatedly.

## Circular queue mechanics

An array-backed bounded queue needs:

- `head`: index of the next element to remove;
- `size`: current number of elements;
- capacity: array length;
- insertion index: `(head + size) % capacity`.

Tracking size separates empty from full without sacrificing one slot. Guard capacity zero before modulo.

## From ordinary stack to monotonic stack

A monotonic stack discards candidates that can never answer a future query better than a newer candidate.

For next greater value, store indexes whose answer is unresolved. When `values[i]` is greater than the value at the top index, it resolves that index.

```text
values: 2  1  4
i=0: stack [0]
i=1: 1 is not greater than 2 -> [0,1]
i=2: 4 resolves index 1, then index 0 -> []
```

Every index is pushed once and popped at most once. That aggregate argument proves O(n) time even though a single iteration may pop many indexes.

Store indexes when the answer needs distance, boundaries, or access to both value and position. Store values only when position is irrelevant.

## Monotonic deque for a window maximum

The deque stores indexes in decreasing value order:

1. remove front indexes outside the current window;
2. remove back indexes whose values are less than or equal to the new value;
3. append the new index;
4. the front is the maximum index.

Removing equal older values is safe when only the maximum value is required because the newer equal value expires later. If stable earliest-index behavior is required, adjust the comparison.

## Boundary with `PriorityQueue`

A priority queue returns the globally best priority, not FIFO order and not arbitrary removal from both ends. Sliding-window maximum with lazy deletion can use heaps, but a monotonic deque is linear and keeps only undominated window candidates. Heaps are developed in the next dedicated volume.

## Foundation failure clinic

- Popping before checking emptiness.
- Mixing front/back methods so the policy silently changes.
- Using `LinkedList` or `Stack` by habit without a needed contract.
- Marking BFS nodes visited too late.
- Storing values when indexes are required.
- Expiring window indexes after reporting the answer.
- Claiming a monotonic structure is sorted output; it stores only a useful candidate frontier.

## Foundation checkpoint

1. Which ends implement a FIFO queue in `ArrayDeque`?
2. Why is `null` unavailable as an `ArrayDeque` element?
3. Why is next-greater processing O(n) rather than O(n squared)?
4. What exactly does a BFS queue contain?
5. When should equal values be removed from a monotonic deque?
