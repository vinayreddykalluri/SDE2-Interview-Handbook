import java.util.ArrayDeque;

public class QueuePatterns {
    public static int maxInWindow(int[] nums, int k) {
        ArrayDeque<Integer> q = new ArrayDeque<>();
        int best = Integer.MIN_VALUE;
        for (int i = 0; i < nums.length; i++) {
            while (!q.isEmpty() && nums[q.peekLast()] <= nums[i]) q.pollLast();
            q.offerLast(i);
            while (!q.isEmpty() && q.peekFirst() <= i - k) q.pollFirst();
            if (i >= k - 1) best = Math.max(best, nums[q.peekFirst()]);
        }
        return best;
    }
}
