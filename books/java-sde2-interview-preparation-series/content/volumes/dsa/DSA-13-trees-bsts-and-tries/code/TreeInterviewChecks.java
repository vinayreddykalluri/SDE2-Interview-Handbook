import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Deque;
import java.util.List;
import java.util.Random;
import java.util.TreeSet;

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

    /** One-based internal storage; callers use zero-based indexes. */
    static final class FenwickTree {
        private final long[] tree;

        FenwickTree(int length) {
            if (length < 0) {
                throw new IllegalArgumentException("length cannot be negative");
            }
            tree = new long[length + 1];
        }

        FenwickTree(int[] values) {
            this(values.length);
            for (int index = 0; index < values.length; index++) {
                add(index, values[index]);
            }
        }

        int length() {
            return tree.length - 1;
        }

        void add(int index, long delta) {
            requireIndex(index, length());
            for (int internal = index + 1; internal < tree.length;
                    internal += internal & -internal) {
                tree[internal] += delta;
            }
        }

        long prefixSum(int rightExclusive) {
            if (rightExclusive < 0 || rightExclusive > length()) {
                throw new IndexOutOfBoundsException("right endpoint is outside [0,n]");
            }
            long sum = 0;
            for (int internal = rightExclusive; internal > 0;
                    internal -= internal & -internal) {
                sum += tree[internal];
            }
            return sum;
        }

        long rangeSum(int left, int rightExclusive) {
            requireRange(left, rightExclusive, length());
            return prefixSum(rightExclusive) - prefixSum(left);
        }
    }

    /** Iterative point-update/range-sum segment tree with half-open queries. */
    static final class SegmentTree {
        private final int length;
        private final int leafBase;
        private final long[] tree;

        SegmentTree(int[] values) {
            length = values.length;
            int base = 1;
            while (base < Math.max(1, length)) {
                base <<= 1;
            }
            leafBase = base;
            tree = new long[leafBase << 1];
            for (int index = 0; index < length; index++) {
                tree[leafBase + index] = values[index];
            }
            for (int node = leafBase - 1; node > 0; node--) {
                tree[node] = tree[node << 1] + tree[node << 1 | 1];
            }
        }

        int length() {
            return length;
        }

        void set(int index, long value) {
            requireIndex(index, length);
            int node = leafBase + index;
            tree[node] = value;
            for (node >>= 1; node > 0; node >>= 1) {
                tree[node] = tree[node << 1] + tree[node << 1 | 1];
            }
        }

        long rangeSum(int left, int rightExclusive) {
            requireRange(left, rightExclusive, length);
            long leftSum = 0;
            long rightSum = 0;
            int first = left + leafBase;
            int afterLast = rightExclusive + leafBase;
            while (first < afterLast) {
                if ((first & 1) == 1) {
                    leftSum += tree[first++];
                }
                if ((afterLast & 1) == 1) {
                    rightSum = tree[--afterLast] + rightSum;
                }
                first >>= 1;
                afterLast >>= 1;
            }
            return leftSum + rightSum;
        }

        long[] levelOrderStorageForTeaching() {
            return tree.clone();
        }
    }

    /** Minimal set-style AVL tree that records which balancing rotation ran. */
    static final class AvlTree {
        private AvlNode root;
        private int size;
        private final List<String> rotationTrace = new ArrayList<>();

        int size() {
            return size;
        }

        Integer rootKey() {
            return root == null ? null : root.key;
        }

        List<String> rotationTrace() {
            return List.copyOf(rotationTrace);
        }

        void add(int key) {
            int previousSize = size;
            root = insert(root, key);
            if (size == previousSize) {
                rotationTrace.add("duplicate " + key + " ignored");
            }
        }

        List<Integer> inorder() {
            List<Integer> values = new ArrayList<>();
            collectInorder(root, values);
            return values;
        }

        boolean isHeightBalanced() {
            return verifiedHeight(root) >= 0;
        }

        private AvlNode insert(AvlNode node, int key) {
            if (node == null) {
                size++;
                return new AvlNode(key);
            }
            if (key < node.key) {
                node.left = insert(node.left, key);
            } else if (key > node.key) {
                node.right = insert(node.right, key);
            } else {
                return node;
            }
            updateHeight(node);
            int balance = balance(node);
            if (balance > 1) {
                if (key > node.left.key) {
                    rotationTrace.add("LR: rotateLeft(" + node.left.key + ")");
                    node.left = rotateLeft(node.left);
                }
                rotationTrace.add("rotateRight(" + node.key + ")");
                return rotateRight(node);
            }
            if (balance < -1) {
                if (key < node.right.key) {
                    rotationTrace.add("RL: rotateRight(" + node.right.key + ")");
                    node.right = rotateRight(node.right);
                }
                rotationTrace.add("rotateLeft(" + node.key + ")");
                return rotateLeft(node);
            }
            return node;
        }

        private static AvlNode rotateRight(AvlNode top) {
            AvlNode promoted = top.left;
            AvlNode transferred = promoted.right;
            promoted.right = top;
            top.left = transferred;
            updateHeight(top);
            updateHeight(promoted);
            return promoted;
        }

        private static AvlNode rotateLeft(AvlNode top) {
            AvlNode promoted = top.right;
            AvlNode transferred = promoted.left;
            promoted.left = top;
            top.right = transferred;
            updateHeight(top);
            updateHeight(promoted);
            return promoted;
        }

        private static int height(AvlNode node) {
            return node == null ? 0 : node.height;
        }

        private static int balance(AvlNode node) {
            return height(node.left) - height(node.right);
        }

        private static void updateHeight(AvlNode node) {
            node.height = 1 + Math.max(height(node.left), height(node.right));
        }

        private static void collectInorder(AvlNode node, List<Integer> output) {
            if (node == null) {
                return;
            }
            collectInorder(node.left, output);
            output.add(node.key);
            collectInorder(node.right, output);
        }

        private static int verifiedHeight(AvlNode node) {
            if (node == null) {
                return 0;
            }
            int leftHeight = verifiedHeight(node.left);
            int rightHeight = verifiedHeight(node.right);
            if (leftHeight < 0 || rightHeight < 0
                    || Math.abs(leftHeight - rightHeight) > 1
                    || node.height != 1 + Math.max(leftHeight, rightHeight)) {
                return -1;
            }
            return 1 + Math.max(leftHeight, rightHeight);
        }
    }

    static final class AvlNode {
        private final int key;
        private int height = 1;
        private AvlNode left;
        private AvlNode right;

        AvlNode(int key) {
            this.key = key;
        }
    }

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

    private static void requireIndex(int index, int length) {
        if (index < 0 || index >= length) {
            throw new IndexOutOfBoundsException("index " + index + " outside [0," + length + ")");
        }
    }

    private static void requireRange(int left, int rightExclusive, int length) {
        if (left < 0 || left > rightExclusive || rightExclusive > length) {
            throw new IndexOutOfBoundsException("invalid half-open range");
        }
    }

    private static boolean rangeTreesMatchArrayOnRandomOperations() {
        Random random = new Random(13L);
        int[] values = new int[24];
        for (int index = 0; index < values.length; index++) {
            values[index] = random.nextInt(101) - 50;
        }
        FenwickTree fenwick = new FenwickTree(values);
        SegmentTree segment = new SegmentTree(values);
        long[] expected = Arrays.stream(values).asLongStream().toArray();
        for (int operation = 0; operation < 2_000; operation++) {
            if (random.nextBoolean()) {
                int index = random.nextInt(values.length);
                int replacement = random.nextInt(201) - 100;
                long delta = replacement - expected[index];
                expected[index] = replacement;
                fenwick.add(index, delta);
                segment.set(index, replacement);
            } else {
                int first = random.nextInt(values.length + 1);
                int second = random.nextInt(values.length + 1);
                int left = Math.min(first, second);
                int right = Math.max(first, second);
                long sum = 0;
                for (int index = left; index < right; index++) {
                    sum += expected[index];
                }
                if (fenwick.rangeSum(left, right) != sum
                        || segment.rangeSum(left, right) != sum) {
                    return false;
                }
            }
        }
        return true;
    }

    private static boolean avlMatchesTreeSetOnRandomInputs() {
        Random random = new Random(17L);
        AvlTree avl = new AvlTree();
        TreeSet<Integer> expected = new TreeSet<>();
        for (int operation = 0; operation < 1_000; operation++) {
            int value = random.nextInt(401) - 200;
            avl.add(value);
            expected.add(value);
            if (!avl.isHeightBalanced()
                    || !avl.inorder().equals(new ArrayList<>(expected))) {
                return false;
            }
        }
        return avl.size() == expected.size();
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

        FenwickTree fenwick = new FenwickTree(new int[] {2, -1, 4, 3, 5});
        check(fenwick.prefixSum(0) == 0, "empty Fenwick prefix");
        check(fenwick.rangeSum(1, 4) == 6, "Fenwick half-open range");
        fenwick.add(2, -4);
        check(fenwick.rangeSum(0, 5) == 9, "Fenwick point delta");

        SegmentTree segment = new SegmentTree(new int[] {2, -1, 4, 3, 5});
        check(segment.rangeSum(1, 4) == 6, "segment range");
        segment.set(2, 0);
        check(segment.rangeSum(0, 5) == 9, "segment point replacement");
        check(segment.rangeSum(3, 3) == 0, "empty segment range");
        check(segment.levelOrderStorageForTeaching().length == 16, "power-of-two storage");

        AvlTree leftLeft = new AvlTree();
        leftLeft.add(30);
        leftLeft.add(20);
        leftLeft.add(10);
        check(leftLeft.rootKey() == 20, "LL rotation root");
        check(leftLeft.rotationTrace().equals(List.of("rotateRight(30)")), "LL trace");

        AvlTree leftRight = new AvlTree();
        leftRight.add(30);
        leftRight.add(10);
        leftRight.add(20);
        check(leftRight.rootKey() == 20, "LR rotation root");
        check(leftRight.rotationTrace().equals(
                List.of("LR: rotateLeft(10)", "rotateRight(30)")), "LR trace");
        leftRight.add(20);
        check(leftRight.size() == 3, "duplicate ignored");
        check(leftRight.isHeightBalanced(), "AVL invariant");
        check(rangeTreesMatchArrayOnRandomOperations(), "range-tree differential test");
        check(avlMatchesTreeSetOnRandomInputs(), "AVL differential test");

        System.out.println("PASS 18 tree checks");
    }
}
