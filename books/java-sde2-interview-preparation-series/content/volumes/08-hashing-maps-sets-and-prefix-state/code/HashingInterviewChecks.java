import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public final class HashingInterviewChecks {
    private HashingInterviewChecks() {}

    static int[] twoSum(int[] values, int target) {
        Map<Integer, Integer> indexByValue = new HashMap<>();
        for (int i = 0; i < values.length; i++) {
            Integer partner = indexByValue.get(target - values[i]);
            if (partner != null) {
                return new int[] {partner, i};
            }
            indexByValue.put(values[i], i);
        }
        return new int[0];
    }

    static long countSubarrays(int[] values, long target) {
        Map<Long, Long> prefixFrequency = new HashMap<>();
        prefixFrequency.put(0L, 1L);
        long prefix = 0L;
        long answer = 0L;
        for (int value : values) {
            prefix += value;
            answer += prefixFrequency.getOrDefault(prefix - target, 0L);
            prefixFrequency.merge(prefix, 1L, Long::sum);
        }
        return answer;
    }

    static int longestBalancedBinarySubarray(int[] bits) {
        Map<Integer, Integer> earliest = new HashMap<>();
        earliest.put(0, -1);
        int prefix = 0;
        int best = 0;
        for (int i = 0; i < bits.length; i++) {
            if (bits[i] != 0 && bits[i] != 1) {
                throw new IllegalArgumentException("expected binary values");
            }
            prefix += bits[i] == 0 ? -1 : 1;
            Integer first = earliest.putIfAbsent(prefix, i);
            if (first != null) {
                best = Math.max(best, i - first);
            }
        }
        return best;
    }

    static int longestConsecutive(int[] values) {
        Set<Integer> present = new HashSet<>();
        for (int value : values) {
            present.add(value);
        }
        int best = 0;
        for (int value : present) {
            if (value != Integer.MIN_VALUE && present.contains(value - 1)) {
                continue;
            }
            int current = value;
            int length = 1;
            while (current != Integer.MAX_VALUE && present.contains(current + 1)) {
                current++;
                length++;
            }
            best = Math.max(best, length);
        }
        return best;
    }

    private static void check(boolean condition, String message) {
        if (!condition) {
            throw new AssertionError(message);
        }
    }

    public static void main(String[] args) {
        int[] indexes = twoSum(new int[] {3, 3}, 6);
        check(indexes.length == 2 && indexes[0] == 0 && indexes[1] == 1, "two sum");
        check(countSubarrays(new int[] {1, -1, 1}, 1L) == 3L, "prefix count");
        check(longestBalancedBinarySubarray(new int[] {0, 1, 0, 1, 1}) == 4, "balanced");
        check(longestConsecutive(new int[] {100, 4, 200, 1, 3, 2}) == 4, "consecutive");
        check(longestConsecutive(new int[] {Integer.MAX_VALUE, Integer.MIN_VALUE}) == 1, "overflow guard");
        System.out.println("PASS 5 hashing checks");
    }
}
