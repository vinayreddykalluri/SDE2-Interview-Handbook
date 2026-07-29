# Essential Tree and BST Pattern Clinics

Tree interviews often hide two distinct questions inside one traversal: what a subtree returns to its parent, and what complete answer may pass through that subtree. Maximum path sum makes that distinction explicit. BST successor makes ancestor bounds and ordering equally concrete.

## Clinic 1: maximum path sum in a binary tree

A path may start and end at any nodes but cannot reuse a node. At each node, distinguish:

- **return value:** the best downward path that the parent may extend;
- **complete candidate:** the node plus the best nonnegative contribution from both children.

The parent can extend at most one child branch because taking both would create a fork rather than a path. The global answer may use both branches because it ends the connection at the current node.

```text
down(node) = node.value + max(0, down(left), down(right))
through(node) = node.value + max(0, down(left)) + max(0, down(right))
```

Initialize the global best below every possible node value, not to zero. Otherwise an all-negative tree incorrectly returns an empty path even though the contract requires at least one node.

For `-10` with children `9` and `20`, where `20` has children `15` and `7`, the best complete candidate is `15 + 20 + 7 = 42`. The downward value returned from `20` is only `20 + 15 = 35`.

## Clinic 2: inorder successor in a BST

Assume distinct keys. The successor is the smallest key greater than the target.

- If the target has a right subtree, the successor is the leftmost node in that subtree.
- Otherwise, it is the lowest ancestor for which the target lies in the ancestor's left subtree.

During search, moving left records the current node as a candidate successor; moving right discards the current node because its key is smaller. This gives O(h) time and O(1) auxiliary space without parent links.

If duplicate keys are allowed, the ordering policy and whether the target is a key or a node identity must be stated first. A value-only API cannot distinguish equal nodes.

## Runnable Java 21 clinic

```java
import java.util.NoSuchElementException;

public final class TreeCoverageClinic {
    private TreeCoverageClinic() {
    }

    public static final class Node {
        private final int value;
        private Node left;
        private Node right;

        public Node(int value) {
            this.value = value;
        }
    }

    public static long maximumPathSum(Node root) {
        if (root == null) {
            throw new IllegalArgumentException("root must be nonnull");
        }
        long[] best = {Long.MIN_VALUE};
        maximumDownwardContribution(root, best);
        return best[0];
    }

    private static long maximumDownwardContribution(Node node, long[] best) {
        if (node == null) {
            return 0;
        }
        long left = Math.max(0, maximumDownwardContribution(node.left, best));
        long right = Math.max(0, maximumDownwardContribution(node.right, best));
        best[0] = Math.max(best[0], node.value + left + right);
        return node.value + Math.max(left, right);
    }

    public static Node inorderSuccessor(Node root, int target) {
        Node current = root;
        Node successor = null;
        while (current != null && current.value != target) {
            if (target < current.value) {
                successor = current;
                current = current.left;
            } else {
                current = current.right;
            }
        }
        if (current == null) {
            throw new NoSuchElementException("target is not in the BST");
        }
        if (current.right != null) {
            successor = current.right;
            while (successor.left != null) {
                successor = successor.left;
            }
        }
        return successor;
    }

    public static void main(String[] args) {
        Node root = new Node(-10);
        root.left = new Node(9);
        root.right = new Node(20);
        root.right.left = new Node(15);
        root.right.right = new Node(7);
        assert maximumPathSum(root) == 42;

        Node bst = new Node(20);
        bst.left = new Node(10);
        bst.right = new Node(30);
        bst.left.left = new Node(5);
        bst.left.right = new Node(15);
        assert inorderSuccessor(bst, 15).value == 20;
        assert inorderSuccessor(bst, 30) == null;
        System.out.println("PASS essential tree clinics");
    }
}
```

Expected output with assertions enabled:

```text
PASS essential tree clinics
```

## Interviewer follow-up chain with model answers

**Interviewer:** Why can the recursive function not return the full through-node path?

**Candidate:** A parent could attach to only one endpoint. Returning a path that already uses both child branches would give the current node degree three when the parent attaches, which is not a simple path.

**Interviewer:** What is the auxiliary space?

**Candidate:** O(h) active call frames, where h is tree height. It is O(log n) for a balanced tree and O(n) for a chain, which can overflow the Java stack on adversarial depth.

**Interviewer:** How would successor change with parent pointers?

**Candidate:** If there is no right subtree, walk upward until leaving an ancestor's left edge. That uses O(1) extra space and O(h) time without starting again from the root.
