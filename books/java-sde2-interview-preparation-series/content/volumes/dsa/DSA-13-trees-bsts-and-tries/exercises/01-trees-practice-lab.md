# Trees, BSTs, and Tries Practice Lab

1. Define root, leaf, depth, height, ancestor, and subtree on one example.
2. Write preorder, inorder, postorder, and level order for a seven-node tree.
3. Explain why inorder is sorted only for a valid BST.
4. Debug a height method whose null base returns 1 while height counts nodes.
5. Debug BST validation that checks only direct children.
6. Implement iterative preorder and postorder.
7. Return whether a tree is height-balanced in O(n), not O(n squared).
8. Compute tree diameter while defining edges versus nodes.
9. Serialize and deserialize a general binary tree with null markers.
10. Return the kth smallest BST value without materializing all values.
11. Implement trie deletion without removing nodes required by another word.
12. Return a right-side view using DFS or BFS.
13. Explain recursion risk for a million-node chain.
14. Compare a BST, `TreeMap`, and trie for prefix queries.
15. Design LCA queries when the tree is static and queries are frequent.
16. **Interview Core:** Return the maximum path sum when a path may start and end anywhere. Separate the value returned to the parent from the complete through-node candidate.
17. **Interview Core:** Find a BST node's inorder successor without parent pointers. State a distinct-key policy and the missing-target behavior.

## Range-query and balancing lab

18. **Foundation:** For internal Fenwick index 12 (`1100`), compute its low bit and the interval summarized by that cell.
19. **Interview Core:** Implement a zero-based Fenwick API with point delta, half-open prefix, and half-open range sum. Test endpoint zero, endpoint `n`, and an empty range.
20. **Interview Core:** Implement an iterative point-replacement/range-sum segment tree using a power-of-two leaf base and `long` summaries.
21. **Interview Core:** Draw and execute LL, RR, LR, and RL AVL insertions. For each rotation, identify the transferred subtree and height-update order.
22. **SDE-2 Follow-up:** Differential-test Fenwick and segment trees against a plain array across randomized replacements and queries.
23. **SDE-2 Follow-up:** Differential-test an AVL set against `TreeSet` after every insertion while independently verifying inorder order, stored height, and balance factors.
