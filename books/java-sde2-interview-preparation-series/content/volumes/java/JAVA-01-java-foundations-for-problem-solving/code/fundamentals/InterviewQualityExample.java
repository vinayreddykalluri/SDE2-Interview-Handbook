import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

public final class InterviewQualityExample {
    static int[] findTwoSumIndices(int[] numbers, int target) {
        if (numbers == null) {
            throw new IllegalArgumentException("numbers must not be null");
        }

        Map<Integer, Integer> earliestIndex = new HashMap<>();
        for (int index = 0; index < numbers.length; index++) {
            long complement = (long) target - numbers[index];
            if (complement >= Integer.MIN_VALUE && complement <= Integer.MAX_VALUE) {
                Integer earlier = earliestIndex.get((int) complement);
                if (earlier != null) {
                    return new int[] {earlier, index};
                }
            }
            earliestIndex.putIfAbsent(numbers[index], index);
        }
        return new int[0];
    }

    public static void main(String[] args) {
        System.out.println(Arrays.toString(
                findTwoSumIndices(new int[] {3, 2, 4}, 6)));
        System.out.println(Arrays.toString(
                findTwoSumIndices(new int[] {3, 3}, 6)));
        System.out.println(Arrays.toString(
                findTwoSumIndices(new int[] {1, 2}, 9)));
    }
}
