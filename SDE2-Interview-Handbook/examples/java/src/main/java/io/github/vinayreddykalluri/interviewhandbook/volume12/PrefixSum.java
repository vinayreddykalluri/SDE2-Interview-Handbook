// SPDX-License-Identifier: MIT
package io.github.vinayreddykalluri.interviewhandbook.volume12;

public class PrefixSum {
    public static int[] buildPrefix(int[] nums) {
        java.util.Objects.requireNonNull(nums, "nums");
        int[] pref = new int[nums.length + 1];
        for (int i = 0; i < nums.length; i++) {
            pref[i + 1] = Math.addExact(pref[i], nums[i]);
        }
        return pref;
    }

    public static int rangeSum(int[] pref, int l, int r) {
        java.util.Objects.requireNonNull(pref, "pref");
        int originalLength = pref.length - 1;
        if (l < 0 || l > r || r >= originalLength) {
            throw new IndexOutOfBoundsException("Expected 0 <= l <= r < original array length");
        }
        return Math.subtractExact(pref[r + 1], pref[l]);
    }
}
