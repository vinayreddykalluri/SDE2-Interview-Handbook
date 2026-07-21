# Code Library

The code library is the implementation companion to the handbook. Canonical Java files live under `examples/java/src/main/java` in the GitHub repository, not inside the documentation tree. This keeps the prose readable and allows every source file to pass an independent compiler check.

[Open all Java source](https://github.com/vinayreddykalluri/SDE2-Interview-Handbook/tree/master/examples/java/src/main/java/io/github/vinayreddykalluri/interviewhandbook){ .md-button .md-button--primary }
[Read local run instructions](https://github.com/vinayreddykalluri/SDE2-Interview-Handbook/blob/master/examples/java/README.md){ .md-button }

## How to learn from an example

1. Read the corresponding chapter before opening the implementation.
2. Write down the input contract and invariant.
3. Predict behavior for empty, singleton, duplicate, negative, and maximum-size inputs.
4. Trace pointer, queue, stack, or state changes by hand.
5. Reimplement the method without copying.
6. Compare complexity and failure behavior.
7. Run `make validate-code` from a local clone.

## Source map

| Volume | Main code area | What to inspect |
| --- | --- | --- |
| 01 Java Fundamentals | [volume01](https://github.com/vinayreddykalluri/SDE2-Interview-Handbook/tree/master/examples/java/src/main/java/io/github/vinayreddykalluri/interviewhandbook/volume01) | execution, memory, types, collections, and warm-up solutions |
| 02 Complexity | [ComplexityDemo.java](https://github.com/vinayreddykalluri/SDE2-Interview-Handbook/blob/master/examples/java/src/main/java/io/github/vinayreddykalluri/interviewhandbook/volume02/ComplexityDemo.java) | operation growth and nested-loop accounting |
| 03 Math | [MathUtils.java](https://github.com/vinayreddykalluri/SDE2-Interview-Handbook/blob/master/examples/java/src/main/java/io/github/vinayreddykalluri/interviewhandbook/volume03/MathUtils.java) | Euclid's algorithm and combinations |
| 04 Loops | [LoopPatterns.java](https://github.com/vinayreddykalluri/SDE2-Interview-Handbook/blob/master/examples/java/src/main/java/io/github/vinayreddykalluri/interviewhandbook/volume04/LoopPatterns.java) | termination and early exit |
| 05 Indices | [Indexing.java](https://github.com/vinayreddykalluri/SDE2-Interview-Handbook/blob/master/examples/java/src/main/java/io/github/vinayreddykalluri/interviewhandbook/volume05/Indexing.java) | half-open ranges |
| 06 Bits | [BitOps.java](https://github.com/vinayreddykalluri/SDE2-Interview-Handbook/blob/master/examples/java/src/main/java/io/github/vinayreddykalluri/interviewhandbook/volume06/BitOps.java) | masks, toggles, and power-of-two checks |
| 07 Arrays | [ArrayOps.java](https://github.com/vinayreddykalluri/SDE2-Interview-Handbook/blob/master/examples/java/src/main/java/io/github/vinayreddykalluri/interviewhandbook/volume07/ArrayOps.java) | stable in-place compaction |
| 08 Strings | [StringPatterns.java](https://github.com/vinayreddykalluri/SDE2-Interview-Handbook/blob/master/examples/java/src/main/java/io/github/vinayreddykalluri/interviewhandbook/volume08/StringPatterns.java) | tokenization and linear reconstruction |
| 09 Hashing | [HashingDemo.java](https://github.com/vinayreddykalluri/SDE2-Interview-Handbook/blob/master/examples/java/src/main/java/io/github/vinayreddykalluri/interviewhandbook/volume09/HashingDemo.java) | frequency maps and deterministic selection |
| 10 Sliding Window | [SlidingWindow.java](https://github.com/vinayreddykalluri/SDE2-Interview-Handbook/blob/master/examples/java/src/main/java/io/github/vinayreddykalluri/interviewhandbook/volume10/SlidingWindow.java) | expand-shrink invariants |
| 11 Two Pointers | [TwoPointers.java](https://github.com/vinayreddykalluri/SDE2-Interview-Handbook/blob/master/examples/java/src/main/java/io/github/vinayreddykalluri/interviewhandbook/volume11/TwoPointers.java) | sorted-pair elimination |
| 12 Prefix Sum | [PrefixSum.java](https://github.com/vinayreddykalluri/SDE2-Interview-Handbook/blob/master/examples/java/src/main/java/io/github/vinayreddykalluri/interviewhandbook/volume12/PrefixSum.java) | preprocessing and range boundaries |
| 13 Binary Search | [BinarySearch.java](https://github.com/vinayreddykalluri/SDE2-Interview-Handbook/blob/master/examples/java/src/main/java/io/github/vinayreddykalluri/interviewhandbook/volume13/BinarySearch.java) | lower-bound loop invariants |
| 14 Stack | [StackPatterns.java](https://github.com/vinayreddykalluri/SDE2-Interview-Handbook/blob/master/examples/java/src/main/java/io/github/vinayreddykalluri/interviewhandbook/volume14/StackPatterns.java) | delimiter matching |
| 15 Queue | [QueuePatterns.java](https://github.com/vinayreddykalluri/SDE2-Interview-Handbook/blob/master/examples/java/src/main/java/io/github/vinayreddykalluri/interviewhandbook/volume15/QueuePatterns.java) | monotonic deque maintenance |
| 16 Linked List | [ListPatterns.java](https://github.com/vinayreddykalluri/SDE2-Interview-Handbook/blob/master/examples/java/src/main/java/io/github/vinayreddykalluri/interviewhandbook/volume16/ListPatterns.java) | pointer reversal |
| 17 Trees | [TreePatterns.java](https://github.com/vinayreddykalluri/SDE2-Interview-Handbook/blob/master/examples/java/src/main/java/io/github/vinayreddykalluri/interviewhandbook/volume17/TreePatterns.java) | recursive tree contracts |
| 18 Graphs | [GraphPatterns.java](https://github.com/vinayreddykalluri/SDE2-Interview-Handbook/blob/master/examples/java/src/main/java/io/github/vinayreddykalluri/interviewhandbook/volume18/GraphPatterns.java) | adjacency construction and BFS |
| 19 Dynamic Programming | [DpPatterns.java](https://github.com/vinayreddykalluri/SDE2-Interview-Handbook/blob/master/examples/java/src/main/java/io/github/vinayreddykalluri/interviewhandbook/volume19/DpPatterns.java) | state compression |

## Quality contract

- Java 17 is the minimum compilation target.
- Every source has a semantic class and filename.
- Large examples are canonical in one place and linked from chapters.
- The standard validation command compiles all main and smoke-test sources.
- The smoke suite exercises representative algorithms across the full volume sequence.
