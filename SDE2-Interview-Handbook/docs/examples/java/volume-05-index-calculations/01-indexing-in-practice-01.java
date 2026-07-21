public class Indexing {
    public static int rangeSum(int[] nums, int l, int r) { // [l, r)
        int sum = 0;
        for (int i = l; i < r; i++) {
            sum += nums[i];
        }
        return sum;
    }
}
