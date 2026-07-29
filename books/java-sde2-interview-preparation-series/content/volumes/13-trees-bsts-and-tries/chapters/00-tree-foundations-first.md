# Tree Foundations: Vocabulary, Traversal, and Recursive Contracts

A tree is a connected acyclic structure. In interview code, a binary-tree node usually owns references to at most two children.

```java
static final class TreeNode {
    int value;
    TreeNode left;
    TreeNode right;

    TreeNode(int value) {
        this.value = value;
    }
}
```

## Vocabulary on one picture

```text
             8                 root
           /   \
          3     10             children of 8
         / \      \
        1   6      14          leaves include 1, 6, 14

path 8 -> 3 -> 6 has two edges
depth(6) = 2 when root depth is 0
height(3) = 1 when leaf height is 0
subtree rooted at 3 contains 3, 1, and 6
```

State whether height counts edges or nodes. Either convention can be correct, but formulas and empty-tree base cases differ.

## The recursive subtree contract

For node-counting, define:

```text
count(node) returns the number of nodes in the subtree rooted at node
```

```java
static int count(TreeNode node) {
    if (node == null) {
        return 0;
    }
    return 1 + count(node.left) + count(node.right);
}
```

The null reference represents an empty subtree. Each non-null call combines the answers of two strictly smaller subtrees.

For height measured in nodes:

```java
static int height(TreeNode node) {
    return node == null ? 0 : 1 + Math.max(height(node.left), height(node.right));
}
```

The recursion depth is O(h), where `h` is tree height. It is O(log n) only for a balanced tree and O(n) for a chain.

## Traversal orders answer different questions

For each node:

- preorder: process, left, right;
- inorder: left, process, right;
- postorder: left, right, process;
- level order: increasing distance from root using a queue.

```text
        2
       / \
      1   3

preorder:  2,1,3
inorder:   1,2,3
postorder: 1,3,2
level:     2,1,3
```

Postorder naturally fits information synthesized from children. Preorder fits carrying state from parent to child. Inorder reveals sorted order only for a valid binary search tree, not for every binary tree.

## Iterative DFS and explicit state

An iterative preorder stack is direct: push root, pop, process, push right then left so left is processed first.

Iterative inorder needs a cursor plus a stack of ancestors:

```java
static List<Integer> inorder(TreeNode root) {
    List<Integer> output = new ArrayList<>();
    Deque<TreeNode> stack = new ArrayDeque<>();
    TreeNode current = root;
    while (current != null || !stack.isEmpty()) {
        while (current != null) {
            stack.push(current);
            current = current.left;
        }
        current = stack.pop();
        output.add(current.value);
        current = current.right;
    }
    return output;
}
```

The stack contains ancestors whose left subtree is complete but whose node/right subtree still needs processing.

## Breadth-first traversal

```java
static List<List<Integer>> levels(TreeNode root) {
    if (root == null) {
        return List.of();
    }
    List<List<Integer>> result = new ArrayList<>();
    Deque<TreeNode> queue = new ArrayDeque<>();
    queue.addLast(root);
    while (!queue.isEmpty()) {
        int levelSize = queue.size();
        List<Integer> level = new ArrayList<>(levelSize);
        for (int i = 0; i < levelSize; i++) {
            TreeNode node = queue.removeFirst();
            level.add(node.value);
            if (node.left != null) queue.addLast(node.left);
            if (node.right != null) queue.addLast(node.right);
        }
        result.add(level);
    }
    return result;
}
```

Snapshot `levelSize` before processing. The queue grows with children for the next level.

## Binary search tree invariant

A common distinct-key BST contract is:

```text
every key in left subtree < node key < every key in right subtree
```

This is a global subtree rule. Checking only immediate children is insufficient; a deep descendant can violate an ancestor bound. Duplicate policy must be explicit.

BST operations cost O(h), not automatically O(log n). Without balancing, sorted insertion can create a height-n chain.

## Tries from zero

A trie stores keys by prefix. A node represents a consumed prefix; outgoing edges represent next symbols; a terminal flag distinguishes a complete word from a prefix.

```text
root
  c
  |
  a -- terminal for "ca" if allowed
 / \
t   r
*   *      words "cat" and "car"
```

Operation cost is O(L) in key length under a suitable child representation. Memory depends on total created prefix nodes and child-container overhead.

## State-ownership clinic

- Local accumulators returned from children avoid stale instance fields.
- A path list shared down DFS must be restored on return.
- A global `previous` pointer for BST validation must be reset before reuse and is unsafe for concurrent calls.
- Serialization must encode null structure, not only values, unless another invariant makes shape recoverable.

## Foundation checkpoint

1. Give a one-sentence contract for tree height.
2. Why is recursive space O(h) rather than always O(log n)?
3. What unresolved work does an iterative inorder stack contain?
4. Why is parent-child comparison insufficient for BST validation?
5. How does a trie distinguish a word from a prefix?
