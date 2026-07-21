public class TwoPointers {
    public static boolean twoSumPair(int[] nums, int target) {
        int l = 0, r = nums.length - 1;
        while (l < r) {
            int sum = nums[l] + nums[r];
            if (sum == target) return true;
            if (sum < target) l++;
            else r--;
        }
        return false;
    }
}
