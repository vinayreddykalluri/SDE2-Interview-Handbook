# Chapter 1: Hashing Fundamentals and Collision Strategy

## Why This Matters

Any problem with fast lookup, deduplication, counting, or set intersection often reduces to hashing.

## Learning Objectives

- Explain hash map internals and average-case complexity.
- Handle collisions with chain/table resize ideas.
- Use hashing for frequency and membership checks.
- Distinguish good hash keys from weak ones.

## Core Concept

A hash function maps a key to a bucket index. Proper hashing plus load-factor management gives average O(1) insert/lookup.

Collision strategy in Java maps:
- Separate chaining (linked list or tree bins in high-occupancy cases).
- Resize/rehash when threshold exceeded.

## Internal Working

1. Compute hash and bucket index.
2. If empty, place node.
3. If occupied, compare keys and resolve.
4. Resize when load-factor indicates dense buckets.

## Architecture or Memory Diagram

```mermaid
flowchart TD
    A[Key] --> B[hashCode()]
    B --> C[bucket = hash % capacity]
    C --> D[Bucket Chain / Tree]
    D --> E[Lookup or Insert]
    E --> F[Resize if needed]
```

## Code Example

[Code Example 1 in detail (external file)](https://github.com/vinayreddykalluri/SDE2-Interview-Handbook/blob/master/examples/java/src/main/java/io/github/vinayreddykalluri/interviewhandbook/volume09/HashingDemo.java)


## Step-by-Step Execution

1. Iterate each value and increment frequency in map.
2. Iterate map entries for maximum count.
3. Return the key with max frequency.

## Interviewer Perspective

They will ask:
- "What is worst-case hash lookup?" (O(n) when pathological collisions)
- "How do you avoid this?" (good hashing, resize, treeification)
- "Can duplicate keys be merged safely?" (yes with update path)

## Common Mistakes

- Mutating keys used in hash structures.
- Forgetting null handling and `equals/hashCode` contract for custom objects.
- Assuming O(1) in all cases regardless of adversarial inputs.

## Production Perspective

Hash structures are central in caches, dedup stores, and frequency counters; key design matters for memory and performance.

## Must Know for DSA

Most O(1)-expected algorithm shortcuts in interviews rely on hash maps/sets.

## Interview Questions and Answers

- **Q: Why can hash be worst-case O(n)?**
  - **Answer:** collisions can collapse many keys into one bucket.
- **Q: What is load factor trade-off?**
  - **Answer:** lower load = more memory but faster lookups.
- **Q: When to pre-size map?**
  - **Answer:** when input count is known to avoid rehashing.

## Practice Exercises

1. Find first unique character in a string with map.
2. Detect anagram equivalence by count arrays vs maps.
3. Build custom class with proper `equals/hashCode` and explain contract.

## Revision Checklist

- [ ] Explain average vs worst-case behavior.
- [ ] Define hash bucket lifecycle.
- [ ] State safe mutable-key rules.
- [ ] Evaluate when sorting+scan beats hash.

## One-Page Summary

Hashing is a practical speed tool when key identity is strong and bucket behavior remains controlled.
