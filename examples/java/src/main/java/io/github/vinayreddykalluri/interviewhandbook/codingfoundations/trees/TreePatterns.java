// SPDX-License-Identifier: MIT
package io.github.vinayreddykalluri.interviewhandbook.codingfoundations.trees;

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
