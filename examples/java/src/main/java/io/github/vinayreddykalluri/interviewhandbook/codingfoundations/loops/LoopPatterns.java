// SPDX-License-Identifier: MIT
package io.github.vinayreddykalluri.interviewhandbook.codingfoundations.loops;

public class LoopPatterns {
    public static int firstPositive(int[] nums) {
        java.util.Objects.requireNonNull(nums, "nums");
        for (int i = 0; i < nums.length; i++) {
            if (nums[i] > 0) {
                return i;
            }
        }
        return -1;
    }
}
