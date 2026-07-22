// SPDX-License-Identifier: MIT
package io.github.vinayreddykalluri.interviewhandbook.codingfoundations.twopointers;

public class TwoPointers {
    public static boolean twoSumPair(int[] nums, int target) {
        java.util.Objects.requireNonNull(nums, "nums");
        int l = 0, r = nums.length - 1;
        while (l < r) {
            long sum = (long) nums[l] + nums[r];
            if (sum == target) return true;
            if (sum < target) l++;
            else r--;
        }
        return false;
    }
}
