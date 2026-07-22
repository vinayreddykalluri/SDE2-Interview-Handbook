# Chapter 1: Queues, Deques, and Circular Buffers

## Why This Matters

Queues enforce FIFO while deques provide both-end flexibility. They are core to shortest-path traversal and sliding-window variants.

## Learning Objectives

- Differentiate FIFO queue, deque, and priority queue use cases.
- Build ring buffer implementation details.
- Analyze enqueue/dequeue complexity.
- Avoid full/empty ambiguities in fixed-size buffers.

## Core Concept

- **Queue**: first in, first out.
- **Deque**: add/remove at both ends.
- **Circular array**: reuse index space with modulo arithmetic and O(1) operations.

## Internal Working

For ring buffer with fixed capacity:

1. Maintain head and tail indices.
2. Increment indices using modulo by capacity.
3. Track size to separate full from empty states.

## Architecture or Memory Diagram

```mermaid
flowchart LR
    A[Array[cap]] --> B[head]
    A --> C[tail]
    B --> D[dequeue front]
    C --> E[enqueue back]
    D --> F[advance head]
    E --> G[advance tail]
    F --> H[Size--]
    G --> H
    H --> I[Full / Empty check]
```

## Code Example

[Code Example 1 in detail (external file)](https://github.com/vinayreddykalluri/SDE2-Interview-Handbook/blob/master/examples/java/src/main/java/io/github/vinayreddykalluri/interviewhandbook/codingfoundations/queues/QueuePatterns.java)


## Step-by-Step Execution

1. Pop smaller values from back while maintaining decreasing values by index.
2. Remove indices outside window from front.
3. Front always keeps max index for current window.

## Interviewer Perspective

They ask: "Why use deque instead of heap?" and "How to enforce O(n)?".

## Common Mistakes

- Keeping stale indices.
- Forgetting size checks for empty queue.
- Using incorrect modulo in fixed-buffer version.

## Production Perspective

FIFO and double-ended containers are used in scheduling and buffering systems where deterministic operations matter.

## Must Know for DSA

Deque patterns are high-frequency in window maximum/minimum and monotonic optimization tasks.

## Interview Questions and Answers

- **Q: Why track indices, not values?**
  - **Answer:** avoids eviction ambiguity for duplicate values.
- **Q: Why `pollFirst` with stale condition?**
  - **Answer:** ensures only in-window candidates remain.
- **Q: O(1) guarantee?**
  - **Answer:** amortized constant per push/pop due to single insertion/removal.

## Practice Exercises

1. Implement circular queue with array and size tracking.
2. Use deque for max in each window with O(n).
3. Compare deque and heap for window max in constraints.

## Revision Checklist

- [ ] Distinguish queue vs deque vs priority queue behavior.
- [ ] Explain ring buffer empty/full logic.
- [ ] Derive amortized O(1) deque operations.
- [ ] Use indices when duplicates exist.

## One-Page Summary

FIFO and double-ended operations become powerful when candidate validity is time- or index-based and must be maintained in O(n).
