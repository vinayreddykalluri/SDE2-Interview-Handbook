// SPDX-License-Identifier: MIT
package io.github.vinayreddykalluri.interviewhandbook.volume09;

import java.util.HashMap;

public class HashingDemo {
    public static int mostFrequent(int[] nums) {
        java.util.Objects.requireNonNull(nums, "nums");
        if (nums.length == 0) {
            throw new IllegalArgumentException("nums must not be empty");
        }
        HashMap<Integer, Integer> freq = new HashMap<>();
        int best = nums[0];
        int bestCount = 0;
        for (int n : nums) {
            int count = freq.merge(n, 1, Integer::sum);
            if (count > bestCount) {
                best = n;
                bestCount = count;
            }
        }
        return best;
    }
}
