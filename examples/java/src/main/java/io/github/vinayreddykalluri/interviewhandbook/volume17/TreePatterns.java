// SPDX-License-Identifier: MIT
package io.github.vinayreddykalluri.interviewhandbook.volume17;

public class TreePatterns {
    public static int maxDepth(TreeNode root) {
        if (root == null) return 0;
        return 1 + Math.max(maxDepth(root.left), maxDepth(root.right));
    }

    static class TreeNode {
        int val; TreeNode left, right;
        TreeNode(int v){ val = v; }
    }
}
