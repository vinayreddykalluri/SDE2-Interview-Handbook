import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;

public final class TreeInterviewChecks {
    static final class TreeNode {
        final int value;
        TreeNode left;
        TreeNode right;

        TreeNode(int value) {
            this.value = value;
        }
    }

    private TreeInterviewChecks() {}

    static boolean isValidBst(TreeNode root) {
        return valid(root, Long.MIN_VALUE, Long.MAX_VALUE);
    }

    private static boolean valid(TreeNode node, long lower, long upper) {
        if (node == null) {
            return true;
        }
        return node.value > lower && node.value < upper
                && valid(node.left, lower, node.value)
                && valid(node.right, node.value, upper);
    }

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

    private static void check(boolean condition, String message) {
        if (!condition) {
            throw new AssertionError(message);
        }
    }

    public static void main(String[] args) {
        TreeNode root = new TreeNode(8);
        root.left = new TreeNode(3);
        root.right = new TreeNode(10);
        root.left.left = new TreeNode(1);
        root.left.right = new TreeNode(6);
        check(isValidBst(root), "valid BST");
        check(inorder(root).equals(List.of(1, 3, 6, 8, 10)), "inorder");
        root.left.right.right = new TreeNode(12);
        check(!isValidBst(root), "ancestor violation");
        System.out.println("PASS 3 tree checks");
    }
}
