public class LoopPatterns {
    public static int firstPositive(int[] nums) {
        for (int i = 0; i < nums.length; i++) {
            if (nums[i] > 0) {
                return i;
            }
        }
        return -1;
    }
}
