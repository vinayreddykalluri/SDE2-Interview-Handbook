# Linked-List Foundations: References, Reachability, and Safe Mutation

Linked-list interviews are reference-reasoning interviews. The syntax is small; the challenge is preserving access to every node while references change.

## Build the mental model

```java
static final class Node {
    int value;
    Node next;

    Node(int value) {
        this.value = value;
    }
}
```

For `4 -> 7 -> 9 -> null`, `head` stores a reference value that identifies the first node. Each node stores a value and another reference. The nodes need not be adjacent in memory.

```text
head
 |
 v
[4 | next] -> [7 | next] -> [9 | null]
```

Assignment copies a reference value:

```java
Node cursor = head;
```

It does not copy the list. `cursor` and `head` now refer to the same first node. Moving `cursor = cursor.next` does not move `head`; mutating `cursor.value` changes the shared node.

## The first safe traversal

```java
static int length(Node head) {
    int count = 0;
    for (Node current = head; current != null; current = current.next) {
        count++;
    }
    return count;
}
```

Invariant: `count` equals the number of nodes strictly before `current`. The loop terminates only for an acyclic, null-terminated list. A cycle requires different logic.

## Insert and delete from first principles

Insert `6` after a known node `current`:

```java
Node inserted = new Node(6);
inserted.next = current.next;
current.next = inserted;
```

The order matters. If `current.next` is overwritten before being saved in `inserted.next`, the remainder can become unreachable.

Delete the node after `current`:

```java
if (current != null && current.next != null) {
    current.next = current.next.next;
}
```

Java garbage collection can reclaim an unreferenced node later; deletion means removing reachability from the data structure, not explicitly freeing memory.

## Head changes and sentinels

Inserting or deleting at the head has no predecessor node. A sentinel gives every real node a predecessor:

```text
sentinel -> head -> ...
```

```java
Node sentinel = new Node(0);
sentinel.next = head;
// mutate through sentinel
return sentinel.next;
```

The sentinel's value is not data. It normalizes boundary logic and reduces special-case branches.

## Reversal: save before overwrite

```java
static Node reverse(Node head) {
    Node previous = null;
    Node current = head;
    while (current != null) {
        Node next = current.next; // preserve remainder
        current.next = previous;  // reverse one edge
        previous = current;       // grow reversed prefix
        current = next;           // continue with saved remainder
    }
    return previous;
}
```

State after processing `4` in `4 -> 7 -> 9`:

```text
reversed prefix        unprocessed suffix
null <- 4              7 -> 9 -> null
       previous        current
```

Invariant: `previous` heads the reversed processed prefix; `current` heads the untouched suffix; together they contain exactly the original nodes.

## Slow and fast pointers

When `slow` advances one edge and `fast` advances two:

- `slow` reaches the middle when `fast` reaches the end;
- in a cycle, their relative distance changes modulo the cycle length, so they meet;
- a fixed gap can locate a node relative to the tail.

Be explicit about even-length midpoint policy. Starting both at `head` and looping while `fast != null && fast.next != null` returns the second middle for an even-length list. Some splits need the first middle instead.

## Identity versus value

Two nodes with equal values are not necessarily the same node. Intersection and cycle problems compare references with `==`. Deduplication by logical payload may compare values. State which identity the problem asks about.

## Mutation and ownership contract

Before coding, clarify:

- May the input nodes be relinked?
- Must the original order be restored after a temporary reversal?
- Can two input lists share nodes?
- Can cycles occur?
- Is the caller allowed to retain references to interior nodes?
- Should the method allocate new nodes or reuse existing ones?

Merging overlapping lists destructively can create a cycle or duplicate ownership. Production APIs need stronger preconditions than many coding-platform prompts.

## Complexity language

Traversing `n` nodes costs O(n) time. Relinking in place uses O(1) auxiliary references, but recursion uses O(n) stack frames. Java's `LinkedList` does not make arbitrary indexed access O(1); locating index `i` still traverses from an end.

## Foundation checkpoint

1. Why must reversal save `current.next` before overwriting it?
2. What problem does a sentinel remove?
3. Which midpoint does your chosen fast/slow loop return for even length?
4. Why does intersection compare node identity rather than value?
5. When is O(1) auxiliary space not the only important mutation concern?
