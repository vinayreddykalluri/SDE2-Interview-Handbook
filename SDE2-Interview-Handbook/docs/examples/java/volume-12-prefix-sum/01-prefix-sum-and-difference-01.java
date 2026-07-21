public class PrefixSum {
    public static int[] buildPrefix(int[] nums) {
        int[] pref = new int[nums.length + 1];
        for (int i = 0; i < nums.length; i++) {
            pref[i + 1] = pref[i] + nums[i];
        }
        return pref;
    }

    public static int rangeSum(int[] pref, int l, int r) {
        return pref[r + 1] - pref[l];
    }
}
