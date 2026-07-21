# Chapter 1: Graph Representations and Traversals

## Why This Matters

Many hard interview problems reduce to graph modeling. If representation is wrong, algorithms fail even with correct traversal logic.

## Learning Objectives

- Choose adjacency list vs matrix based on density.
- Implement BFS and DFS with clear visited semantics.
- Detect cycles/disconnected components.
- Compare complexity of traversal strategies.

## Core Concept

Graph representation controls performance and memory:

- **Adjacency list** for sparse graphs.
- **Adjacency matrix** for dense graphs and fast edge existence.

Traversal:
- BFS for shortest hops in unweighted graphs.
- DFS for connectivity and component enumeration.

## Internal Working

1. Build graph from edges.
2. Mark visited.
3. Traverse from source, visiting neighbors not yet seen.
4. Count components/order and detect cycles if required.

## Architecture or Memory Diagram

```mermaid
flowchart LR
    N1((1)) --> N2((2))
    N1 --> N3((3))
    N2 --> N4((4))
    N3 --> N4
    N4 --> N1
    D[Adjacency List] --> V[visit[]]
    V --> BFS[BFS Queue]
    V --> DFS[BFS Stack/Recursion]
```

## Code Example

[Code Example 1 in detail (external file)](https://github.com/vinayreddykalluri/SDE2-Interview-Handbook/blob/master/examples/java/src/main/java/io/github/vinayreddykalluri/interviewhandbook/volume18/GraphPatterns.java)


## Step-by-Step Execution

1. Build adjacency list from edges.
2. Initialize start vertex and queue.
3. Pop, append, and enqueue unseen neighbors.
4. Repeat until queue empty.

## Interviewer Perspective

Follow-ups include:
- "Why choose list over matrix here?"
- "What is memory and time complexity?"
- "How do you reconstruct parent path?"

## Common Mistakes

- Forgetting to initialize visited and revisiting nodes.
- Using matrix for sparse graph at scale.
- Assuming connectedness before checking components.

## Production Perspective

Graph traversals are central in dependency resolution and social/transport network analysis services.

## Must Know for DSA

Interviewers value clean graph modeling and traversal contracts above optimized micro-implementation details.

## Interview Questions and Answers

- **Q: Why BFS for shortest path in unweighted graph?**
  - **Answer:** BFS visits by nondecreasing hop distance.
- **Q: How to handle disconnected graphs?**
  - **Answer:** loop over all vertices and BFS/DFS if unvisited.
- **Q: Why adjacency list for sparse graphs?**
  - **Answer:** proportional storage to edges, not n^2.

## Practice Exercises

1. Detect cycle in undirected graph.
2. Count components in graph with n nodes.
3. Convert directed edges and add topological feasibility check.

## Revision Checklist

- [ ] Choose structure based on graph density.
- [ ] State visited semantics at push/enqueue.
- [ ] Explain BFS complexity using vertices/edges.
- [ ] Discuss disconnected-case handling.

## One-Page Summary

Graphs require a deliberate model, then a disciplined traversal. Representation first, traversal second.
