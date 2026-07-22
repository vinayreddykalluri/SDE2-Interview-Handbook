// SPDX-License-Identifier: MIT
package io.github.vinayreddykalluri.interviewhandbook.codingfoundations.slidingwindow;

public class SlidingWindow {
    public static int maxLenUnique(int[] nums) {
        java.util.Objects.requireNonNull(nums, "nums");
        java.util.HashSet<Integer> set = new java.util.HashSet<>();
        int l = 0, best = 0;
        for (int r = 0; r < nums.length; r++) {
            while (set.contains(nums[r])) {
                set.remove(nums[l++]);
            }
            set.add(nums[r]);
            best = Math.max(best, r - l + 1);
        }
        return best;
    }
}
