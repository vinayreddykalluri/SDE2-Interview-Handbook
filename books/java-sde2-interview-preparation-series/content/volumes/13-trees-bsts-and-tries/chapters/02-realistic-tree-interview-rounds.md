# Realistic Tree, BST, and Trie Interview Rounds

## Round 1: validate a binary search tree

### Prompt

Return whether a binary tree satisfies the strict BST invariant. Values are Java `int` values.

### Candidate clarification

> Are duplicate keys permitted, and if so, on which side?

The interviewer says duplicates are invalid.

### Correct answer

Carry the valid open interval from ancestors. Use `long` bounds so all `int` values are representable without special casing.

```java
static boolean isValidBst(TreeNode root) {
    return valid(root, Long.MIN_VALUE, Long.MAX_VALUE);
}

static boolean valid(TreeNode node, long lower, long upper) {
    if (node == null) {
        return true;
    }
    if (node.value <= lower || node.value >= upper) {
        return false;
    }
    return valid(node.left, lower, node.value)
            && valid(node.right, node.value, upper);
}
```

Invariant: every node reached by a call must lie strictly inside the interval implied by all ancestors. The left child receives a tighter upper bound; the right child receives a tighter lower bound.

**Complexity:** O(n) time; O(h) stack space.

**Failure mode:** checking `node.left.value < node.value` and `node.right.value > node.value` misses a value in the right subtree that is smaller than the root.

## Round 2: lowest common ancestor in a general binary tree

### Prompt

Return the lowest node whose subtree contains both target node references. Both targets are guaranteed present.

### Model answer

```java
static TreeNode lowestCommonAncestor(TreeNode node, TreeNode first, TreeNode second) {
    if (node == null || node == first || node == second) {
        return node;
    }
    TreeNode left = lowestCommonAncestor(node.left, first, second);
    TreeNode right = lowestCommonAncestor(node.right, first, second);
    if (left != null && right != null) {
        return node;
    }
    return left != null ? left : right;
}
```

Contract: a call returns a target found in its subtree, the LCA if both are found, or null if neither is found. When left and right are both non-null, the targets split across the child subtrees, so the current node is lowest.

### Follow-up answers

**What if a target may be absent?** The simple method can return the present target and falsely imply success. Return a result containing candidate plus found-count, and accept only count two.

**What if this is a BST?** Ordering can guide both targets left or right, giving O(h) search without exploring both subtrees.

## Round 3: design autocomplete with a trie

### Prompt

Support inserting words, testing complete words, testing prefixes, and returning up to `k` suggestions for a prefix.

### Candidate design

The basic trie stores a child map and terminal flag. To provide deterministic lexicographic suggestions, either use ordered children or sort child keys during collection. For high-query systems, cache top suggestions at nodes and define update consistency.

```java
static final class TrieNode {
    final Map<Character, TrieNode> children = new TreeMap<>();
    boolean terminal;
}

static void insert(TrieNode root, String word) {
    TrieNode current = root;
    for (int i = 0; i < word.length(); i++) {
        current = current.children.computeIfAbsent(word.charAt(i), ignored -> new TrieNode());
    }
    current.terminal = true;
}
```

### Follow-up answers

**Unicode?** `char` traverses UTF-16 code units, not complete user-perceived characters. Define normalization and symbol unit; code-point traversal may be required.

**Complexity?** Basic insert and prefix navigation are O(L) child lookups. Suggestion generation is output-sensitive and may visit a large subtree. `TreeMap` adds logarithmic child lookup; fixed alphabets can use arrays with different memory trade-offs.

**How do rankings work?** Store word frequencies and use cached top-k lists, a heap during subtree search, or a separate search index. Define freshness, memory, and update costs.

## Closing answer pattern

State node meaning, null base case, returned subtree contract, traversal order, height-dependent space, identity versus value, duplicate/order policy, and skewed-tree behavior.
