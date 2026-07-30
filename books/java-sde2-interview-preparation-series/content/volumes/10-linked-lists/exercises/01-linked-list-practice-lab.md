# Linked-List Practice Lab

## Knowledge and prediction

1. Why does `Node copy = head` not copy a list?
2. What does a sentinel represent, and should its value be returned as data?
3. For `1 -> 2 -> 3 -> 4`, which middle does your fast/slow loop return?
4. Why can recursive reversal use O(n) auxiliary space despite allocating no nodes?

## Debugging

5. A reversal assigns `current.next = previous` and then `current = current.next`. Explain the lost-suffix bug.
6. A remove-Nth-from-end method advances `fast` without checking range validity. Define a safe contract and repair approach.
7. A palindrome check reverses half the list but returns on first mismatch. What side effect remains?

## Coding tasks

8. Merge two sorted lists by reusing nodes.
9. Partition a list around a pivot while preserving relative order.
10. Find the intersection of two acyclic lists by node identity.
11. Copy a list whose nodes have `next` and `random` references.
12. Perform bottom-up merge sort without recursive stack use.

## SDE-2 follow-ups

13. Explain the risk of destructively merging two lists that may overlap.
14. Design an immutable persistent list and compare update cost.
15. Explain why Java's `LinkedList` is rarely the default choice for interview queues.

## Essential clinic task

16. **SDE-2 Follow-up:** Implement a fixed-capacity LRU cache with a hash map and sentinel-based doubly linked list. Write the representation invariants and explain why `get` is a mutation.
