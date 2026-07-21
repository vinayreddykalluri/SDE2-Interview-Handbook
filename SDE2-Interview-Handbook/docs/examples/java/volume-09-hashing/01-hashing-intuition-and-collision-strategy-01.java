import java.util.HashMap;

public class HashingDemo {
    public static int mostFrequent(int[] nums) {
        HashMap<Integer, Integer> freq = new HashMap<>();
        for (int n : nums) freq.put(n, freq.getOrDefault(n, 0) + 1);
        int best = nums[0], bestCount = 0;
        for (var e : freq.entrySet()) {
            if (e.getValue() > bestCount) {
                best = e.getKey();
                bestCount = e.getValue();
            }
        }
        return best;
    }
}
