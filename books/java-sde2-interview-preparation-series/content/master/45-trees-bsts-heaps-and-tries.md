# 45. Trees, BSTs, Heaps, and Tries

## Learning objectives

By the end of this chapter, you should be able to:

- choose preorder, inorder, postorder, or level order from data dependencies;
- write recursive tree contracts and translate them to explicit-stack traversals;
- validate and query binary search trees using inherited bounds;
- use heaps for top-k and frontier selection with correct comparator semantics;
- design tries around an explicit alphabet and terminal-state model; and
- analyze time and space in terms of node count, height, branching, and key length.

## Why this matters at SDE-2

Trees force candidates to reason about hierarchical dependencies. The best traversal is determined by when a node's result depends on ancestors, children, or peers. Heaps test whether only an extreme candidate is needed rather than global ordering. Tries expose representation trade-offs between query speed and memory.

Production systems use related ideas in syntax trees, indexes, schedulers, routing tables, autocomplete, dependency models, and hierarchical configuration. The textbook structure is simplified, but its invariants transfer. An SDE-2 answer should separate a logical binary search tree from a database index or a Java library implementation.

## First-principles model

A rooted tree is a connected acyclic hierarchy. Every node except the root has one parent, and each child roots a disjoint subtree. This self-similarity makes recursion natural: solve each subtree under a clear contract, then combine results.

A binary search tree adds an ordering invariant over whole subtrees, not just direct children. A heap adds only an extreme-at-root invariant and a complete-tree shape; it does not sort all elements. A trie organizes keys by prefixes, making each edge represent part of a key rather than a comparison against a whole key.

Algorithm choice follows the question:

- information flowing from ancestors suggests preorder or parameters carrying context;
- information flowing from descendants suggests postorder;
- sorted BST output suggests inorder; and
- minimum unweighted depth or per-level output suggests BFS.

> **Specification boundary:** Java does not provide a built-in general tree node contract. `PriorityQueue` guarantees heap-based queue behavior and head ordering according to natural order or a comparator; it does not expose a sorted iteration order or stable ordering among ties.

## Core terminology

- **Root/leaf:** top node and node with no children.
- **Depth:** edges from root to a node under the common convention.
- **Height:** longest downward edge path from a node to a leaf; conventions for empty trees must be stated.
- **Subtree:** a node and all descendants.
- **Preorder/inorder/postorder:** node-before, node-between, or node-after child traversal for binary trees.
- **Level order:** breadth-first traversal by depth.
- **BST invariant:** all keys in one subtree precede the node and all in the other follow it under a duplicate policy.
- **Heap:** complete tree with parent-child priority invariant.
- **Top-k:** retain only k best candidates seen so far.
- **Trie:** prefix tree with edges labeled by key units.
- **Terminal marker:** indicates that a trie path forms a complete key.

## Detailed mechanics

### Recursive contracts and traversal selection

Before writing recursion, finish this sentence: "For node x, this function returns..." Examples include subtree height, whether the subtree is balanced, maximum downward path, or a pair of summaries. Then define the null result and show how child results combine.

For height in nodes, one possible contract is:

```text
height(null) = 0
height(node) = 1 + max(height(node.left), height(node.right))
```

For balance, a naive method recomputes heights at each node and can become O(n squared) on a chain. A postorder method can return height for a balanced subtree or a sentinel such as -1 for imbalance, computing both properties in one O(n) pass.

Traversal choice table:

| Requirement | Natural traversal | Reason |
|---|---|---|
| Copy tree, emit prefix notation | Preorder | Process node before descendants |
| Sorted keys from a valid BST | Inorder | Left, node, right follows ordering |
| Delete tree, height, balance, diameter | Postorder | Need child results before parent |
| Minimum depth, nearest target, levels | BFS | Processes increasing edge distance |
| Root-to-leaf path constraints | DFS with carried state | Context follows one active path |

A recursive DFS uses O(h) call-stack space for height h, which is O(log n) for a balanced tree and O(n) for a skewed tree. Java does not guarantee tail-call elimination. An iterative traversal uses an explicit stack with the same asymptotic worst-case depth but avoids thread-stack overflow and can store explicit frame state.

### State ownership in DFS

Distinguish three categories:

- **path state:** belongs only to the active root-to-current path and must be undone when backtracking;
- **subtree result:** returned upward and summarizes descendants; and
- **global result:** best answer across all visited nodes.

Tree diameter illustrates the split. Each call returns the longest downward height usable by its parent. A separate maximum records a path that may connect the left and right heights through the current node. Returning diameter instead of height would violate the parent's needed contract.

Prefer returning a small record such as `record Result(boolean balanced, int height) {}` over mutable global state when it keeps the contract local and supports reentrant calls.

### Binary search tree reasoning

The BST invariant is transitive. Checking only `node.left.value < node.value < node.right.value` misses a deep value that violates an ancestor. Carry an allowed range into each recursive call:

```text
validate(node, lowerExclusive, upperExclusive)
left  gets (lowerExclusive, node.value)
right gets (node.value, upperExclusive)
```

Use `long` bounds for `int` keys or nullable bounds so `Integer.MIN_VALUE` and `MAX_VALUE` remain valid. Define duplicates: forbidden, always left, always right, or counted in a node. The inequality must match that contract.

BST search, insertion, and deletion cost O(h). They are O(log n) only when height is logarithmic; an unbalanced insertion order can create a chain and O(n) operations. Self-balancing trees enforce height bounds, but implementing rotations is usually outside a basic interview unless requested.

Inorder traversal of a valid BST yields sorted keys. The kth smallest element can stop after k visits, use an explicit stack, or use stored subtree sizes for order-statistic queries. Augmented metadata must be updated on every mutation.

### Lowest common ancestor and path problems

For a general binary tree, a postorder LCA contract can return whether a target was found and an ancestor candidate. The familiar method that returns a node when it equals p or q, then returns the current node when left and right both return non-null, assumes both targets exist unless additional found-state is tracked.

For a BST with distinct known keys, ordering permits directed descent: if both are smaller go left; if both larger go right; otherwise the current node splits them and is the LCA. This is O(h) and avoids exploring both subtrees.

Root-to-leaf sums use path state. Arbitrary paths may require combining child summaries. Always define whether an empty path is legal, whether endpoints must be leaves, and whether node or edge weights are summed.

### Heaps and priority queues

A binary heap uses a complete-tree shape, commonly stored in an array. For zero-based index i:

```text
parent = (i - 1) / 2
left   = 2 * i + 1
right  = 2 * i + 2
```

Insertion appends and sifts up; removal swaps in the last value and sifts down. Both are O(log n). Peeking at the minimum in Java's default `PriorityQueue` is O(1). Building a heap from n values with bottom-up heapify is O(n), not O(n log n), because most nodes are near the leaves and move little.

For k largest values, retain a min-heap of size k. The root is the weakest retained candidate. For each new value, insert until size k; afterward replace the root only if the new value is larger. The invariant is that the heap contains the k largest values of the processed prefix, and its root is the kth largest among them.

For k smallest values, use a max-heap of size k. Avoid `b - a` comparators because subtraction can overflow; use `Comparator.reverseOrder()` or `Integer.compare(b, a)`. A priority queue does not support efficient arbitrary removal or membership; combine it with a map and lazy deletion when updates are frequent.

### Tries

A trie node stores outgoing edges and whether the path ending there is a complete key. Search cost is O(L) for key length L, independent of the number of stored keys under the chosen edge-access model. Prefix search reaches the prefix node and then enumerates descendants; total cost must include output.

Representation depends on alphabet:

- fixed array: fast and simple for a small dense alphabet, but allocates many null slots;
- hash map: sparse and flexible, with hashing and object overhead;
- sorted map or compact edge vector: supports ordered enumeration at additional lookup cost;
- compressed/radix trie: stores strings on edges to collapse single-child chains.

The terminal flag distinguishes `"app"` from the prefix of `"apple"`. Deletion clears a terminal marker and prunes nodes only when they are nonterminal and childless. Normalize case or Unicode only if the domain requires it, and make normalization consistent between insertion and lookup.

## Worked Java example

This Java 21 program demonstrates global-bound BST validation, heap-based top-k selection, and an ASCII lowercase trie.

```java
import java.util.Arrays;
import java.util.PriorityQueue;

public final class TreeToolkit {
    static final class Node {
        final int value;
        Node left;
        Node right;

        Node(int value) {
            this.value = value;
        }
    }

    static boolean isValidBst(Node root) {
        return validate(root, Long.MIN_VALUE, Long.MAX_VALUE);
    }

    private static boolean validate(Node node, long lower, long upper) {
        if (node == null) return true;
        if (node.value <= lower || node.value >= upper) return false;
        return validate(node.left, lower, node.value)
                && validate(node.right, node.value, upper);
    }

    static int[] largestK(int[] values, int k) {
        if (k < 0 || k > values.length) {
            throw new IllegalArgumentException("invalid k");
        }
        PriorityQueue<Integer> selected = new PriorityQueue<>();
        for (int value : values) {
            if (selected.size() < k) selected.add(value);
            else if (k > 0 && value > selected.peek()) {
                selected.remove();
                selected.add(value);
            }
        }
        int[] answer = new int[k];
        for (int i = 0; i < k; i++) answer[i] = selected.remove();
        return answer;
    }
```

The trie implementation continues inside the same `TreeToolkit` class:

```java

    static final class Trie {
        private static final class TrieNode {
            final TrieNode[] children = new TrieNode[26];
            boolean terminal;
        }

        private final TrieNode root = new TrieNode();

        void insert(String word) {
            TrieNode node = root;
            for (int i = 0; i < word.length(); i++) {
                int edge = edge(word.charAt(i));
                if (node.children[edge] == null) {
                    node.children[edge] = new TrieNode();
                }
                node = node.children[edge];
            }
            node.terminal = true;
        }

        boolean contains(String word) {
            TrieNode node = find(word);
            return node != null && node.terminal;
        }

        boolean hasPrefix(String prefix) {
            return find(prefix) != null;
        }

        private TrieNode find(String text) {
            TrieNode node = root;
            for (int i = 0; i < text.length(); i++) {
                node = node.children[edge(text.charAt(i))];
                if (node == null) return null;
            }
            return node;
        }

        private static int edge(char value) {
            if (value < 'a' || value > 'z') {
                throw new IllegalArgumentException("only a-z is supported");
            }
            return value - 'a';
        }
    }
```

The entry point completes `TreeToolkit` and exercises all three structures:

```java

    public static void main(String[] args) {
        Node root = new Node(8);
        root.left = new Node(3);
        root.right = new Node(10);
        root.left.right = new Node(6);
        System.out.println(isValidBst(root)); // true

        System.out.println(Arrays.toString(largestK(
                new int[] {5, 1, 9, 3, 7, 8}, 3))); // [7, 8, 9]

        Trie trie = new Trie();
        trie.insert("app");
        trie.insert("apple");
        System.out.println(trie.contains("app"));    // true
        System.out.println(trie.contains("ap"));     // false
        System.out.println(trie.hasPrefix("ap"));    // true
    }
}
```

`largestK` returns the selected values in ascending order because repeated removal from a min-heap yields its head order. The top-k problem itself may not require sorted output; if it does not, draining the heap is still a simple deterministic choice.

## Execution or memory walkthrough

BST validation begins at 8 with allowed range `(Long.MIN_VALUE, Long.MAX_VALUE)`. Node 3 receives the upper bound 8. Its right child 6 receives `(3, 8)`, so it passes. Node 10 receives `(8, Long.MAX_VALUE)`. A hidden node 9 under the left subtree of 3 would fail because it inherits upper bound 8 even if it is greater than its immediate parent.

For `largestK([5,1,9,3,7,8], 3)`, the heap first becomes `[1,5,9]` conceptually, with 1 at the root. Value 3 replaces 1; 7 replaces 3; 8 replaces 5. The final heap contains `{7,8,9}`. Every removed root is proven unable to belong to the largest three of the processed prefix.

Trie insertion of `app` creates edges a, p, p and marks the last node terminal. Inserting `apple` reuses that path, then creates l and e and marks a different terminal. Thus `ap` is a reachable prefix but not a stored key.

## Complexity and performance

| Operation | Time | Auxiliary space |
|---|---:|---:|
| DFS traversal | O(n) | O(h) stack |
| BFS traversal | O(n) | O(w) frontier, w max width |
| BST search/update | O(h) | O(1) iterative |
| BST validation | O(n) | O(h) |
| Heap peek | O(1) | O(1) |
| Heap insert/remove head | O(log n) | O(1) beyond heap |
| Largest k of n | O(n log k) | O(k) |
| Trie insert/search | O(L) | O(L) new nodes for insert |
| Enumerate prefix results | O(P + output) | Traversal-dependent |

The worked BST validator is O(n) time and O(h) call-stack space. Top-k is O(n log k) time and O(k) space, plus O(k log k) to drain in sorted order. Trie lookup is O(L), assuming fixed-array edge access, but the constant memory per node is 26 references.

For small k, a heap beats sorting all n values asymptotically. For k near n and sorted output, sorting can be simpler and competitive. A trie can outperform repeated full-key comparison for prefix workloads but may consume far more memory than a sorted array of strings.

> **HotSpot note:** Tree node layout, reference compression, recursion compilation, and `PriorityQueue`'s backing-array details are HotSpot/library implementation concerns. Pointer-heavy trees can have poor cache locality even when asymptotic bounds match array-based structures.

## Edge cases and common mistakes

- Inconsistent height definitions for null, leaf, edges, and nodes.
- Recomputing subtree height at every node and accidentally creating O(n squared) work.
- Using recursion on an unbounded skewed tree and overflowing the Java stack.
- Validating a BST only against immediate children.
- Using `int` sentinels that reject legitimate minimum or maximum keys.
- Leaving duplicate-key placement unspecified.
- Assuming a BST is balanced because it is a BST.
- Assuming `PriorityQueue` iteration is sorted or tie order is stable.
- Reversing a comparator with subtraction and overflowing.
- Keeping k largest values in a max-heap, which exposes the wrong eviction candidate.
- Calling top-k O(n log k) without including required sorted-output work.
- Forgetting a trie terminal marker, making every prefix appear to be a word.
- Allocating a dense child array for a huge sparse alphabet.
- Applying ASCII `char` trie logic to arbitrary Unicode without a stated key unit.
- Mutating augmented tree metadata on some update paths but not others.

## Production engineering notes

Real database B-trees and LSM systems are designed around pages, storage hierarchy, concurrency, and durability; do not equate them with a pointer-based interview BST. Similarly, `TreeMap` is a balanced library map with a contract broader than a hand-written node exercise. Prefer these libraries unless implementing the structure is itself the task.

Use heaps for bounded candidate sets, timers, schedulers, and graph frontiers, but define stale-entry handling and tie breakers. A queue entry that references mutable priority can violate heap ordering; insert immutable snapshots or remove/reinsert through a controlled API.

Trie deployments need an alphabet, normalization, memory budget, update policy, and output cap. Autocomplete can return enormous subtrees, so cache or rank top suggestions and paginate. Concurrent readers and writers require immutability, copy-on-write, locking, or a specialized structure; ordinary node arrays are not thread-safe.

## Interview questions and model answers

**How do you choose a tree traversal?**

Follow dependency order. Process a node before children when passing ancestor state, after children when combining subtree results, between children for BST order, and by levels when distance from the root matters.

**Why is checking only BST children insufficient?**

Every descendant must satisfy all ancestor bounds. A value can be less than its parent yet still be greater than an ancestor whose left subtree contains it. Carry the valid range or verify strict inorder order.

**Why is bottom-up heap construction O(n)?**

Although one sift-down can cost O(log n), most nodes are near leaves and move zero or one levels. Summing nodes by height yields a convergent weighted series proportional to n.

**Which heap finds the k largest elements?**

A min-heap of size k. Its root is the smallest retained value and therefore the correct candidate to evict when a larger value arrives.

**What does trie search complexity omit?**

O(L) covers reaching the node for a length-L key under an edge lookup assumption. Returning all completions also costs at least the visited subtree and output size. Memory depends strongly on child representation and alphabet.

**Recursive or iterative DFS?**

Both are O(n) time and O(h) state. Recursion is concise and mirrors the proof, but Java stack depth is bounded and not tail-call optimized. Use an explicit stack for adversarial depth or when frame phases need direct control.

## Exercises

1. Return balance and height in one postorder pass without global mutable state.
2. Implement iterative inorder traversal and use it to find the kth smallest BST key.
3. Validate a BST under an "equal keys go right" duplicate policy.
4. Compute tree diameter while clearly separating returned downward state from global path state.
5. Merge k sorted iterators with a priority queue and a stable tie breaker.
6. Find the k closest points without comparator overflow and discuss output ordering.
7. Add deletion to the trie, pruning only safe nodes.
8. Replace the fixed trie child array with a map and compare memory and lookup trade-offs.

## Chapter summary

Trees turn hierarchy into recursive subproblems. Choose traversal from the direction of information flow, define the null result, and separate path, subtree, and global state. BST correctness requires inherited ordering bounds and has O(h), not automatically O(log n), operations. Heaps maintain only an extreme and support bounded top-k selection. Tries exchange memory for prefix-directed lookup and require explicit alphabet and terminal semantics.

## Revision checklist

- [ ] I define depth and height conventions before using them.
- [ ] I choose traversal from dependency order rather than habit.
- [ ] I state a recursive return contract and null base result.
- [ ] I account for O(h) recursion or explicit-stack space.
- [ ] I validate BSTs with global bounds and a duplicate policy.
- [ ] I never assume an arbitrary BST is balanced.
- [ ] I choose the heap whose root is the eviction candidate.
- [ ] I use safe comparators and do not assume priority-queue iteration order.
- [ ] I distinguish trie prefix reachability from terminal keys.
- [ ] I include output size and representation memory in trie analysis.
