// SPDX-License-Identifier: MIT
package io.github.vinayreddykalluri.interviewhandbook.volume13;

public class BinarySearch {
    public static int lowerBound(int[] nums, int target) {
        java.util.Objects.requireNonNull(nums, "nums");
        int l = 0, r = nums.length; // [l, r)
        while (l < r) {
            int m = l + ((r - l) >> 1);
            if (nums[m] < target) l = m + 1;
            else r = m;
        }
        return l;
    }
}
