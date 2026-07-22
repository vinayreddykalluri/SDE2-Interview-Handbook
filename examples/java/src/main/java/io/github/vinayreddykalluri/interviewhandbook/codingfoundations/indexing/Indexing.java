// SPDX-License-Identifier: MIT
package io.github.vinayreddykalluri.interviewhandbook.codingfoundations.indexing;

public class Indexing {
    public static int rangeSum(int[] nums, int l, int r) { // [l, r)
        java.util.Objects.requireNonNull(nums, "nums");
        if (l < 0 || l > r || r > nums.length) {
            throw new IndexOutOfBoundsException("Expected 0 <= l <= r <= nums.length");
        }
        int sum = 0;
        for (int i = l; i < r; i++) {
            sum = Math.addExact(sum, nums[i]);
        }
        return sum;
    }
}
