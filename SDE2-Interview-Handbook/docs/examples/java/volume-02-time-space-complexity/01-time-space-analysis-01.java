public class ComplexityDemo {
    public static int complexityExample(int[] nums) {
        int max = 0;
        for (int i = 0; i < nums.length; i++) {            // O(n)
            int running = 0;
            for (int j = i; j < nums.length; j++) {        // O(n)
                running += nums[j];
            }
            max = Math.max(max, running);
        }
        return max; // O(n^2)
    }
}
