# Realistic Linked-List Interview Rounds

## Round 1: reverse a sublist in one pass

### Prompt

Reverse positions `left` through `right`, using one-based positions, and return the possibly new head.

### Clarification

> Are the positions guaranteed valid? May I relink the original nodes? Is an empty list valid?

Assume valid positions, in-place relinking, and `head` may be null only when no range is requested.

### Model answer

A sentinel handles `left == 1`. Move `before` to the node preceding the range. Repeatedly detach the node after the range head and insert it immediately after `before`.

```java
static Node reverseBetween(Node head, int left, int right) {
    Node sentinel = new Node(0);
    sentinel.next = head;
    Node before = sentinel;
    for (int position = 1; position < left; position++) {
        before = before.next;
    }

    Node rangeHead = before.next;
    for (int move = 0; move < right - left; move++) {
        Node extracted = rangeHead.next;
        rangeHead.next = extracted.next;
        extracted.next = before.next;
        before.next = extracted;
    }
    return sentinel.next;
}
```

For `1 -> 2 -> 3 -> 4 -> 5`, range 2..4:

```text
before=1, rangeHead=2
move 3: 1 -> 3 -> 2 -> 4 -> 5
move 4: 1 -> 4 -> 3 -> 2 -> 5
```

Time is O(n) including reaching the range; auxiliary space is O(1).

### Follow-up

**How do you make invalid ranges safe?** Validate `left >= 1`, `right >= left`, and enough nodes before mutation, or document a fail-fast contract. Avoid partially mutating before discovering invalid input.

## Round 2: find the entry of a cycle

### Prompt

Return the node where a cycle begins, or null when no cycle exists. Do not allocate a set.

### Candidate explanation

First, move slow by one and fast by two until they meet. If fast reaches null, there is no cycle. Then move one pointer to head and advance both by one; their next meeting is the cycle entry.

```java
static Node cycleEntry(Node head) {
    Node slow = head;
    Node fast = head;
    do {
        if (fast == null || fast.next == null) {
            return null;
        }
        slow = slow.next;
        fast = fast.next.next;
    } while (slow != fast);

    Node fromHead = head;
    while (fromHead != slow) {
        fromHead = fromHead.next;
        slow = slow.next;
    }
    return fromHead;
}
```

If the distance from head to entry is `a`, and the first meeting is `b` steps into a cycle of length `c`, the fast pointer has traveled twice as far. The resulting modular relation makes the distance from meeting to entry match `a` modulo `c`.

### Follow-up

**Could a visited set be clearer?** Yes: O(n) auxiliary space and simpler reasoning. Floyd's method meets an O(1)-space constraint and requires the proof above.

**What if the list structure changes concurrently?** The result is not meaningful without synchronization or immutability; pointer algorithms assume a stable structure during traversal.

## Round 3: determine whether a list is a palindrome and restore it

### Strong plan

Find the end of the first half, reverse the second half, compare corresponding values, and reverse the second half again before returning.

```java
static boolean isPalindrome(Node head) {
    if (head == null || head.next == null) {
        return true;
    }
    Node slow = head;
    Node fast = head;
    while (fast.next != null && fast.next.next != null) {
        slow = slow.next;
        fast = fast.next.next;
    }

    Node reversed = reverse(slow.next);
    boolean equal = true;
    Node left = head;
    Node right = reversed;
    while (right != null) {
        if (left.value != right.value) {
            equal = false;
            break;
        }
        left = left.next;
        right = right.next;
    }
    slow.next = reverse(reversed);
    return equal;
}
```

### Follow-up answers

**Why restore?** A predicate named `isPalindrome` should not normally leave caller-owned data reordered. Restoration makes the side-effect contract unsurprising.

**What if comparison throws?** Use `try/finally` around comparison so restoration still occurs.

**Complexity?** O(n) time and O(1) auxiliary references. The input is temporarily mutated.

## Interview closing checklist

Draw the nodes, name each reference, state the reachability invariant, identify every edge that changes, clarify head/tail behavior, test one and two nodes, and state whether the original structure is preserved.
