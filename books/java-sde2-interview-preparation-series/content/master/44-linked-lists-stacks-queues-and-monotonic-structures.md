# 44. Linked Lists, Stacks, Queues, and Monotonic Structures

## Learning objectives

By the end of this chapter, you should be able to:

- mutate singly linked lists without losing nodes or creating accidental cycles;
- use sentinels, fast/slow pointers, and merge as reusable list techniques;
- choose stack, queue, or deque semantics from dependency order;
- derive monotonic stack and deque algorithms from domination rules; and
- prove linear time using aggregate pushes, pops, and pointer movement.

## Why this matters at SDE-2

These structures test whether a candidate can control mutable state. Linked-list bugs are often one incorrect assignment; monotonic algorithms require explaining why removed candidates can never matter again. Stack and queue choices reveal whether traversal order is understood rather than memorized.

Production Java rarely uses a hand-written linked list for ordinary storage, but the pointer discipline transfers to trees, caches, lock-free structures, and intrusive queues. Deques power schedulers, buffers, parsers, graph traversals, and rolling analytics. SDE-2 answers should connect the abstract behavior to memory locality, capacity, concurrency, and ownership.

## First-principles model

A singly linked node stores a value and a reference to the next node. The list is reachable from a head reference; changing one link can make an entire suffix unreachable or can introduce a cycle. Safe mutation therefore preserves a reference to future work before redirecting the current link.

A stack exposes the most recently added item first. A queue exposes the earliest added item first. A deque supports both ends. These are ordering contracts, not particular storage layouts.

A monotonic structure keeps candidates ordered by value while positions arrive in sequence. When a new candidate dominates an older one for every possible future query, the older one is removed permanently. This deletion rule is what makes the structure useful and what gives an amortized linear bound.

> **Specification boundary:** Java references are not source-level memory pointers, and Java does not expose pointer arithmetic. `Deque` defines endpoint behavior; a particular implementation's internal array or node layout is not part of that contract. `ArrayDeque` rejects null elements.

## Core terminology

- **Head/tail:** first and last reachable list nodes.
- **Successor:** node referenced by `next`.
- **Sentinel/dummy:** extra node that unifies boundary mutations.
- **Fast/slow pointers:** references advancing at different rates or from offset positions.
- **LIFO:** last in, first out stack order.
- **FIFO:** first in, first out queue order.
- **Frontier:** discovered work waiting for processing.
- **Monotonic stack:** stack whose relevant values are maintained increasing or decreasing.
- **Monotonic deque:** deque retaining ordered candidates, often with expiry at the front.
- **Domination:** proof that one candidate can never beat another in a future answer.
- **Amortized analysis:** bounds total operations across a sequence rather than one step.

## Detailed mechanics

### Linked-list mutation discipline

For iterative reversal, before changing `current.next`:

1. save `next = current.next`;
2. redirect `current.next = previous`;
3. advance `previous = current`; and
4. advance `current = next`.

The loop invariant is:

- `previous` heads a correctly reversed prefix;
- `current` heads the original-order unprocessed suffix; and
- every original node is reachable from exactly one of those two roots.

Initialization uses an empty prefix (`previous = null`) and the full suffix. One step moves exactly one node from suffix to prefix. At termination the suffix is empty and `previous` heads the complete reversal.

Assignment order matters. Writing `current.next = previous` before saving the old successor loses access to the remaining suffix. Drawing three boxes is not childish; it externalizes an aliasing problem that is difficult to repair after code is written.

### Sentinels and boundary normalization

Insertion or deletion at the head often needs a special branch because there is no predecessor. A sentinel node placed before the real head makes every real node have a predecessor. For "remove the nth node from the end," a sentinel also permits removal of the original head using the same `previous.next = previous.next.next` operation.

The sentinel is algorithmic state and is not returned as data. Its value is irrelevant. In generic production code, prefer a dedicated node shape or explicit absence over inventing a value that might collide with valid input.

### Fast and slow pointers

Fast/slow pointers compress distance information:

- move fast two and slow one to detect a cycle;
- after a meeting, reset one pointer to head and move both one to find cycle entry;
- start fast k nodes ahead to locate the kth node from the end;
- move fast two and slow one to find a midpoint.

Floyd cycle detection works because, inside a cycle, the relative distance changes by one modulo the cycle length each iteration, so the pointers must meet. It uses O(1) space. A visited-node set is easier to generalize and can report more information but costs O(n) space.

Null checks must match the dereference: before `fast.next.next`, require both `fast != null` and `fast.next != null`. Decide which middle to return for even length; initial positions and loop condition control the answer.

### Merge as a primitive

Merging two sorted lists uses a sentinel tail. At each step, attach the smaller current node, advance that source, and advance the output tail. The invariant says the output prefix is sorted and contains exactly the consumed input nodes. Once one input ends, the other suffix is already sorted and can be attached.

This O(n + m) primitive appears in merge sort, k-way merging with a heap, and interval or stream processing. Clarify whether nodes may be relinked or a new list is required. Relinking is O(1) auxiliary space but mutates the inputs and changes ownership.

### Stack semantics

Use a stack when unresolved work must finish in reverse order:

- matching nested delimiters;
- evaluating expressions;
- iterative depth-first traversal;
- simulating recursive frames; and
- finding next/previous greater or smaller elements.

In Java, prefer `ArrayDeque` through the `Deque` interface over legacy `Stack`. Use `push`, `pop`, and `peek` consistently for stack intent. Decide whether malformed input returns a result or throws; `pop` and `removeFirst` throw on emptiness, while `poll` forms return null, which `ArrayDeque` can use as an absence signal because it disallows null elements.

Delimiter matching needs both kind and order. A count of openings is insufficient for `([)]`; the most recent unmatched opening must match the current closing delimiter.

### Queue and deque semantics

Use a queue when work must be processed in discovery or arrival order. BFS relies on FIFO order to process all states at distance d before distance d + 1 in an unweighted graph. Mark a state visited when enqueuing, not when dequeuing, to avoid repeated queue entries.

Level-order processing can store `(node, distance)` pairs, use a fixed `levelSize = queue.size()` before a level loop, or place a sentinel marker. Size-based boundaries avoid null markers and make the invariant explicit.

A circular buffer represents a bounded FIFO with an array, head index, tail index, and size or one deliberately unused slot to distinguish full from empty. Every representation needs one unambiguous convention. Bounded capacity is a production feature: it establishes backpressure instead of allowing memory to grow without limit.

### Monotonic stacks

For next greater value to the right, scan from right to left. Maintain a decreasing stack of values that could answer a future position. Before answering current x, pop every value less than or equal to x; those values are both closer to x than earlier positions and no larger than x, so x dominates them for all future elements to the left. The remaining top, if present, is the nearest greater value. Then push x.

Many variants need indexes instead of values:

- distances to next greater temperature;
- histogram rectangle widths;
- previous smaller boundaries; and
- span calculations.

Choose strict versus non-strict popping from duplicate semantics. `<=` and `<` produce different boundaries. State the desired relation before coding.

### Monotonic deques

For maximum in every size-k window, keep indexes in decreasing value order. For each index i:

1. remove front indexes `<= i - k` because they expired;
2. remove back indexes whose values are `<= values[i]` because the new value is newer and at least as good;
3. append i; and
4. once a full window exists, the front is its maximum.

The deque maintains two dimensions: indexes increase from front to back, while values decrease. Expiry happens only at the front; domination happens at the back.

Although one iteration can pop many entries, each index is appended once and removed at most once from each relevant end. Total deque work is O(n), an aggregate proof that is more informative than inspecting the nested while loops.

## Worked Java example

This Java 21 class contains canonical linked-list and monotonic templates.

```java
import java.util.ArrayDeque;
import java.util.Arrays;
import java.util.Deque;

public final class LinearStructures {
    static final class Node {
        final int value;
        Node next;

        Node(int value) {
            this.value = value;
        }
    }

    static Node reverse(Node head) {
        Node previous = null;
        Node current = head;
        while (current != null) {
            Node next = current.next;
            current.next = previous;
            previous = current;
            current = next;
        }
        return previous;
    }

    static boolean hasCycle(Node head) {
        Node slow = head;
        Node fast = head;
        while (fast != null && fast.next != null) {
            slow = slow.next;
            fast = fast.next.next;
            if (slow == fast) return true;
        }
        return false;
    }
```

The monotonic-stack and monotonic-deque methods continue the same `LinearStructures` class:

```java

    static int[] nextGreater(int[] values) {
        int[] answer = new int[values.length];
        Deque<Integer> stack = new ArrayDeque<>();
        for (int i = values.length - 1; i >= 0; i--) {
            while (!stack.isEmpty() && stack.peek() <= values[i]) {
                stack.pop();
            }
            answer[i] = stack.isEmpty() ? -1 : stack.peek();
            stack.push(values[i]);
        }
        return answer;
    }

    static int[] slidingWindowMaximum(int[] values, int k) {
        if (k < 1 || k > values.length) {
            throw new IllegalArgumentException("invalid window size");
        }
        int[] answer = new int[values.length - k + 1];
        Deque<Integer> deque = new ArrayDeque<>();

        for (int i = 0; i < values.length; i++) {
            while (!deque.isEmpty() && deque.peekFirst() <= i - k) {
                deque.removeFirst();
            }
            while (!deque.isEmpty()
                    && values[deque.peekLast()] <= values[i]) {
                deque.removeLast();
            }
            deque.addLast(i);
            if (i >= k - 1) answer[i - k + 1] = values[deque.peekFirst()];
        }
        return answer;
    }

    public static void main(String[] args) {
        Node one = new Node(1);
        one.next = new Node(2);
        one.next.next = new Node(3);
        Node reversed = reverse(one);
        System.out.println(reversed.value + " " + reversed.next.value); // 3 2
        System.out.println(hasCycle(reversed));                         // false
        System.out.println(Arrays.toString(nextGreater(
                new int[] {2, 1, 2, 4, 3})));                          // [4,2,4,-1,-1]
        System.out.println(Arrays.toString(slidingWindowMaximum(
                new int[] {1, 3, -1, -3, 5, 3, 6, 7}, 3)));           // [3,3,5,5,6,7]
    }
}
```

The list methods compare node references by identity. Defining logical node equality is unnecessary and could make cycle logic incorrect if equal values appeared in distinct nodes.

## Execution or memory walkthrough

For reversal of `1 -> 2 -> 3`, state evolves as follows:

| Step | `previous` reversed prefix | `current` suffix |
|---|---|---|
| Start | null | `1 -> 2 -> 3` |
| Move 1 | `1 -> null` | `2 -> 3` |
| Move 2 | `2 -> 1` | `3` |
| Move 3 | `3 -> 2 -> 1` | null |

The saved `next` reference preserves the suffix before redirection. No new nodes are allocated.

For window maxima over `[1, 3, -1, -3, 5]` with k = 3, deque indexes evolve:

| i | Value | Deque after expiry/domination | Output |
|---:|---:|---|---|
| 0 | 1 | `[0]` | - |
| 1 | 3 | `[1]` | - |
| 2 | -1 | `[1,2]` | 3 |
| 3 | -3 | `[1,2,3]` | 3 |
| 4 | 5 | `[4]` | 5 |

At i = 4, index 1 first expires, then value 5 dominates indexes 3 and 2. The deque front is always inside the window and holds its maximum.

## Complexity and performance

| Operation or algorithm | Time | Auxiliary space |
|---|---:|---:|
| Access kth singly linked node | O(k) | O(1) |
| Insert after known node | O(1) | O(1) |
| Reverse list | O(n) | O(1) iterative |
| Cycle detection | O(n) | O(1) |
| Merge two sorted lists | O(n + m) | O(1) if relinking |
| Stack delimiter scan | O(n) | O(n) worst case |
| BFS frontier traversal | O(V + E) | O(V) |
| Next greater monotonic stack | O(n) amortized | O(n) |
| Sliding maximum deque | O(n) amortized | O(k) |

The worked algorithms are linear because each node or index is processed a bounded number of times. `ArrayDeque` endpoint operations are amortized constant time under its documented collection behavior, while an individual growth can copy internal storage.

Linked nodes have poor spatial locality and per-node object overhead. An array-based representation often outperforms a linked structure even when both have the same asymptotic scan cost. Conversely, relinking known nodes can avoid moving large payloads.

> **HotSpot note:** HotSpot may scalar-replace short-lived iterator or node-like objects that do not escape, but linked structures generally involve pointer chasing and allocations. Exact object layout and `ArrayDeque` growth strategy are implementation details.

## Edge cases and common mistakes

- Empty list, one node, and a cycle beginning at the head.
- Losing the suffix during reversal by redirecting before saving `next`.
- Returning the sentinel rather than `sentinel.next`.
- Using value equality when an algorithm depends on node identity.
- Advancing fast without checking both fast and `fast.next`.
- Failing to define which middle an even-length list returns.
- Relinking caller-owned lists without declaring mutation.
- Using legacy `Stack` or mixing queue and stack method names on a deque.
- Popping an empty structure because malformed input was not validated.
- Marking BFS visited when dequeued, creating duplicate work.
- Storing monotonic values when the answer requires distance or expiry indexes.
- Using `<` where duplicates require `<=`, or the reverse.
- Expiring after reading the maximum, allowing an out-of-window answer.
- Calling a monotonic structure O(n) without the push/pop aggregate proof.
- Treating a concurrent queue as a complete atomic workflow.

## Production engineering notes

Prefer `ArrayList` or arrays for most sequential data; use `LinkedList` only when its semantics and measured access pattern justify node overhead. For custom linked structures, define ownership, whether nodes may be shared, and whether mutation must be synchronized. Never expose partially relinked state to concurrent readers.

Queues need capacity and overload behavior. An unbounded producer-consumer queue converts load spikes into memory pressure and latency. Specify whether producers block, fail, shed, or persist. Queue thread safety does not make a multi-step check-then-act protocol atomic.

Monotonic deques are valuable for rolling telemetry, but real time windows may include out-of-order events, late data, and expiry based on timestamps rather than indexes. Extend the invariant accordingly. For parser stacks, place limits on nesting depth to resist malicious input.

## Interview questions and model answers

**How do you prove iterative list reversal?**

Maintain a reversed processed prefix headed by `previous` and an untouched suffix headed by `current`. Save the successor, move one node to the prefix, and preserve reachability. When the suffix is empty, the prefix is the whole reversed list.

**Why does Floyd's cycle algorithm use constant space?**

It stores two references. Once both are inside a cycle, their relative offset advances modulo cycle length and must become zero. No visited set is needed.

**Why prefer a sentinel node?**

It gives the original head a predecessor, so insertion or deletion at the head uses the same link update as an interior position. This reduces branches and proof cases.

**Why is a monotonic stack linear despite nested loops?**

Every index is pushed once and, once popped, never returns. Across the full algorithm there are at most n pushes and n pops, so total stack operations are O(n).

**Why store indexes in a monotonic deque?**

Indexes identify when candidates leave the window and allow distance results. Values alone cannot distinguish duplicate occurrences or expiry.

**Does BFS always find a shortest path?**

It finds a minimum-edge path in an unweighted graph, or when all edges have equal cost, because FIFO processing explores by distance layers. Arbitrary positive weights require a weighted shortest-path algorithm.

## Exercises

1. Reverse nodes between positions left and right using a sentinel and state the local invariant.
2. Find a cycle entry with Floyd's algorithm and derive why the reset step works.
3. Merge k sorted linked lists using a heap; analyze n total nodes and k lists.
4. Implement a delimiter validator with `ArrayDeque<Character>` and report the first error index.
5. Build a bounded circular queue using an array and a `size` field; test wraparound.
6. Return days until a warmer temperature using a monotonic stack of indexes.
7. Solve largest rectangle in a histogram, specifying duplicate-height behavior.
8. Compare the deque maximum template against a heap with lazy expiry on random inputs.

## Chapter summary

Linked-list correctness depends on preserving reachability while changing links. Sentinels normalize boundaries, fast/slow pointers encode relative position, and merge is a reusable sorted primitive. Stacks and queues express reverse-dependency and discovery order. Monotonic stacks and deques retain only candidates that can still win; expiry and domination rules define their invariants. Their linear bounds come from counting each push and permanent removal across the whole run.

## Revision checklist

- [ ] I save a successor before redirecting a list link.
- [ ] I can prove reversal with prefix and suffix reachability.
- [ ] I use sentinels to remove head special cases.
- [ ] I understand fast/slow cycle, midpoint, and offset patterns.
- [ ] I choose LIFO, FIFO, or double-ended semantics intentionally.
- [ ] I use `ArrayDeque` consistently and handle empty operations.
- [ ] I state monotonic order, expiry rule, and domination rule.
- [ ] I store indexes when distance, duplicate identity, or expiry matters.
- [ ] I prove linear time by aggregate pushes, pops, and pointer advances.
- [ ] I discuss locality, ownership, capacity, and concurrency in production.
