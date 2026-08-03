# Trees, BSTs, and Tries Practice Lab Solutions

1. Apply the definitions consistently and state whether height counts edges or nodes; a leaf has height zero under edge height and one under node height.
2. Preorder processes before children, inorder between children, postorder after children, and BFS by depth. Trace actual node references rather than memorizing labels.
3. The BST global ordering invariant places all left-subtree keys before the node and all right-subtree keys after it.
4. With node-count height, null must return 0 and a leaf returns 1. Returning 1 makes a leaf height 2.
5. Carry ancestor bounds or verify strict inorder increase. Immediate children do not represent all descendants.
6. Preorder uses a stack and pushes right before left. Postorder can use two stacks, a `(node, expanded)` frame, or reverse a root-right-left sequence.
7. Return subtree height, but return a sentinel such as -1 when a child is unbalanced. Each node is processed once.
8. A postorder call returns height while updating a maximum of left height plus right height. State whether that sum is edges or adjust for node count.
9. Preorder plus explicit null markers uniquely preserves shape. Parsing consumes tokens in the same recursive contract; validate malformed/trailing input.
10. Iterative inorder counts visited nodes and stops at k. O(h + k) time and O(h) auxiliary space.
11. Recursively clear terminal at the word end, then remove a child only if it is nonterminal and has no children. Preserve nodes shared by longer or sibling words.
12. BFS takes the last node per level; right-first DFS records the first node seen at each depth.
13. Recursive depth becomes O(n) and can overflow the Java stack. Use an explicit deque or constrain input depth.
14. A hand-built BST gives ordering but not prefix structure; `TreeMap` is balanced and can support range queries; a trie follows prefixes directly but may use much more memory.
15. Options include parent pointers plus depth alignment, binary lifting, Euler tour plus RMQ, or offline algorithms. Choose from update frequency, memory, and query latency.
16. A child returns only its best nonnegative downward contribution because a parent can extend one branch. At the current node, update a global best with node value plus both nonnegative child contributions. Initialize below every node value so an all-negative tree selects its least-negative node. Time is O(n), with O(h) call depth.
17. Search from the root while recording the current node whenever moving left; it is the best greater ancestor seen so far. If the target has a right subtree, return that subtree's leftmost node. Return empty when no successor exists and reject or explicitly return empty for a missing target according to the API contract.

## Range-query and balancing solutions

18. `12` is binary `1100`; `12 & -12` is `0100`, or 4. Fenwick cell 12 summarizes the four one-based positions `[9,12]`. In general it covers `[i-lowbit(i)+1, i]`.

19. Allocate `long[length+1]`. For public point index `p`, update internal indexes starting at `p+1` and repeatedly add `i & -i`. For a prefix endpoint `r`, start at internal `r` and repeatedly subtract the low bit. Range `[l,r)` is `prefix(r)-prefix(l)`. Validate `p` in `[0,n)` and endpoints in `[0,n]`; `[i,i)` returns zero. The complete code is `FenwickTree` in the companion.

20. Choose `leafBase` as the smallest power of two at least `max(1,n)`. Copy values to `tree[leafBase+i]`, build parents downward, and recompute ancestors after a point replacement. For `[l,r)`, translate both endpoints to leaves, consume an odd left node and the node before an odd right endpoint, then shift to parents. Each update/query touches `O(log n)` nodes; build is `O(n)`.

21. LL uses `rotateRight`, RR uses `rotateLeft`, LR rotates the left child left then the node right, and RL mirrors it. In a right rotation, the promoted left child's right subtree transfers to the demoted node's left because its keys lie between them. Update the demoted node's height before the promoted node. `AvlTree.rotationTrace()` makes each chosen case observable.

22. Keep a `long[]` oracle. On replacement, compute `delta = replacement - old`, apply Fenwick `add(index,delta)`, segment `set(index,replacement)`, and update the oracle. On a random half-open query, scan the oracle and compare both tree results. Generate empty ranges, negatives, endpoints zero/`n`, and repeated updates with a fixed seed.

23. Insert the same random value into the AVL and `TreeSet`. After every operation, compare inorder list and size, then recursively recompute child heights. Reject if a stored height differs, `abs(leftHeight-rightHeight)>1`, or inorder is not strictly increasing. Include duplicates to verify the selected set policy. The companion performs this check after each of one thousand insertions.
