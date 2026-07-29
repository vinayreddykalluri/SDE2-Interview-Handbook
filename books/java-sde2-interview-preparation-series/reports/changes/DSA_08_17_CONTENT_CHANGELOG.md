# DSA 08-17 Content Changelog

| Book | Original weakness | Change made | Interview additions | Accuracy emphasis |
|---|---|---|---|---|
| 08 Hashing | native path began after master excerpts | added native foundations, lab, solutions, and companion | 3 full rounds | expected complexity, stable keys, long prefix state |
| 09 Recursion | patterns preceded a full contract model | added contract/stack/restoration foundation | 3 full rounds | output-sensitive work, safe pruning, Java depth |
| 10 Linked Lists | pointer basics were compressed | added reference/reachability/sentinel foundation | 3 full rounds | identity, saved-next, restoration, ownership |
| 11 Ordering Structures | API ends and policy easy to confuse | added stack/queue/deque and monotonic transition | 3 full rounds | enqueue-time visited, aggregate analysis, no null |
| 12 Binary Search | boundary templates needed derivation | added interval/progress/lower-bound foundation | 3 full rounds | overflow, duplicates, monotonicity, termination |
| 13 Trees | vocabulary and global invariants compressed | added subtree-contract and traversal foundation | 3 full rounds | O(h) stack, ancestor bounds, Unicode trie boundary |
| 14 Heaps | heap shape and partial order too brief | added heap mechanics and Java API foundation | 3 full rounds | comparator safety, iteration order, mutable priority |
| 15 Graphs | modeling assumptions were easy to skip | added representation and algorithm-selection foundation | 3 full rounds | weight constraints, disconnected state, V/E cost |
| 16 Greedy | proof obligation needed beginner practice | added exchange/counterexample/proof foundation | 3 full rounds | endpoint semantics, hidden sort cost, DP boundary |
| 17 Dynamic Programming | state derivation needed a repeatable protocol | added eight-question DP foundation | 3 full rounds | evaluation order, pseudo-polynomial cost, safe compression |

All physical PDF names, cover conventions, segment numbering, author credit, and build tools were preserved.

## Second publication audit

| Book | Gap promoted from exercise/boundary | Taught additions | Practice additions |
|---|---|---|---:|
| 08 Hashing | XOR and exact-cardinality state | prefix-XOR frequency; exactly-K via at-most subtraction | 2 |
| 09 Recursion | constrained generation and dense constraint state | balanced parentheses; validated restoring Sudoku solver | 1 |
| 10 Linked Lists | bidirectional ownership | doubly linked invariants; complete LRU cache | 1 |
| 11 Ordering Structures | popped-boundary proof | histogram area; trapped rain water | 1 |
| 12 Binary Search | partition and implicit rank | median of two arrays; kth matrix occurrence | 2 |
| 13 Trees | returned state versus global answer | maximum path sum; inorder successor | 2 |
| 14 Heaps | bounded and synchronized frontiers | k closest; smallest range covering k lists | 2 |
| 15 Graphs | frontier policy by source/weight | multi-source BFS; 0-1 BFS and weighted routing | 2 |
| 16 Greedy | lower-bound and closing-obligation proofs | candy; partition labels | 1 |
| 17 Dynamic Programming | missing prefix state and shallow interval boundary | word break; matrix-chain multiplication | 1 |

The V2 pass also expanded validation to compile complete Java classes embedded in mapped chapters even when a separate companion exists. Visual QA found and removed three single-answer tail pages created by the new content.
