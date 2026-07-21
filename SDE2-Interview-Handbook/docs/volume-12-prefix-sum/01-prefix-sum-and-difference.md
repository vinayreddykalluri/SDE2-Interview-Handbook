# Chapter 1: Prefix Sum and Difference Array

## Why This Matters

Many range-based interview questions ask for repeated query speed. Prefix sums provide a reusable memoized accumulation pattern.

## Learning Objectives

- Derive prefix formula for range sums.
- Use difference array for range increment updates.
- Analyze preprocessing and query complexity trade-offs.
- Handle mutable updates safely.

## Core Concept

Given array `A`, prefix `P` where `P[i+1] = P[i] + A[i]`, then:

`sum(l, r) = P[r+1] - P[l]`

Difference arrays allow range updates in O(1) update and O(n) final reconstruction.

## Internal Working

1. Build base prefix once.
2. For each range query, compute subtraction directly.
3. For range update, increment `diff[l]`, decrement `diff[r+1]`.
4. Reconstruct final values by cumulative sum of diff.

## Architecture or Memory Diagram

```mermaid
flowchart LR
    A[Original Array] --> B[Prefix Build]
    B --> C[Range Query]
    C --> D[O(1) answer]
    E[Range Updates] --> F[Difference Array]
    F --> G[Reconstruct]
```

## Code Example

```java
public class PrefixSum {
    public static int[] buildPrefix(int[] nums) {
        int[] pref = new int[nums.length + 1];
        for (int i = 0; i < nums.length; i++) {
            pref[i + 1] = pref[i] + nums[i];
        }
        return pref;
    }

    public static int rangeSum(int[] pref, int l, int r) {
        return pref[r + 1] - pref[l];
    }
}
```

## Step-by-Step Execution

1. Construct prefix with O(n).
2. Convert each query `(l, r)` to constant-time subtraction.
3. For update-heavy workloads, use difference for deferred accumulation.

## Interviewer Perspective

Common pressure: "How do you justify correctness of subtraction formula?" and "What is preprocessing cost?".

## Common Mistakes

- Using inclusive-exclusive indexing wrong.
- Rebuilding prefix after each query instead of using memoized structure.
- Forgetting integer overflow for large values.

## Production Perspective

Prefix arrays are common in analytics and telemetry where many range queries dominate.

## Must Know for DSA

This pattern reduces complexity from O(n*queries) to O(n + queries) after build.

## Interview Questions and Answers

- **Q: Why one extra prefix slot?**
  - **Answer:** simplifies `sum(0, r)` with `pref[r+1]`.
- **Q: Why difference updates are O(1)?**
  - **Answer:** boundaries encode start/end deltas; reconstruction accumulates later.
- **Q: Can we do dynamic updates online?**
  - **Answer:** use Fenwick/segment tree for fully dynamic point/range query mix.

## Practice Exercises

1. Build prefix and answer 10 random range queries.
2. Apply three range increments using difference approach.
3. Convert to `long` to avoid overflow and compare behavior.

## Revision Checklist

- [ ] Memorize range sum equation.
- [ ] Explain update/query complexity separately.
- [ ] Use correct indexing conventions in all examples.
- [ ] Mention offline vs online update trade-offs.

## One-Page Summary

Prefix sums are a high-impact preprocessing idea that trades setup time for fast query performance with predictable correctness.
