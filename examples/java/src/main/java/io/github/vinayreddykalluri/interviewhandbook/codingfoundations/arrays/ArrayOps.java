// SPDX-License-Identifier: MIT
package io.github.vinayreddykalluri.interviewhandbook.codingfoundations.arrays;

public class ArrayOps {
    public static void moveZeroes(int[] nums) {
        java.util.Objects.requireNonNull(nums, "nums");
        int write = 0;
        for (int x : nums) {
            if (x != 0) nums[write++] = x;
        }
        while (write < nums.length) nums[write++] = 0;
    }
}
