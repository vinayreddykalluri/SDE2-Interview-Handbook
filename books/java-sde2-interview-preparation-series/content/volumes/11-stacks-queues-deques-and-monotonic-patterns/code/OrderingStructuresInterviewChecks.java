import java.util.ArrayDeque;
import java.util.Arrays;
import java.util.Deque;

public final class OrderingStructuresInterviewChecks {
    private OrderingStructuresInterviewChecks() {}

    static int[] daysUntilWarmer(int[] temperatures) {
        int[] answer = new int[temperatures.length];
        Deque<Integer> unresolved = new ArrayDeque<>();
        for (int day = 0; day < temperatures.length; day++) {
            while (!unresolved.isEmpty()
                    && temperatures[day] > temperatures[unresolved.peek()]) {
                int earlier = unresolved.pop();
                answer[earlier] = day - earlier;
            }
            unresolved.push(day);
        }
        return answer;
    }

    static int[] windowMaximum(int[] values, int k) {
        if (k < 1 || k > values.length) {
            throw new IllegalArgumentException("invalid window");
        }
        int[] answer = new int[values.length - k + 1];
        Deque<Integer> candidates = new ArrayDeque<>();
        for (int right = 0; right < values.length; right++) {
            int left = right - k + 1;
            while (!candidates.isEmpty() && candidates.peekFirst() < left) {
                candidates.removeFirst();
            }
            while (!candidates.isEmpty()
                    && values[candidates.peekLast()] <= values[right]) {
                candidates.removeLast();
            }
            candidates.addLast(right);
            if (left >= 0) {
                answer[left] = values[candidates.peekFirst()];
            }
        }
        return answer;
    }

    private static void check(boolean condition, String message) {
        if (!condition) {
            throw new AssertionError(message);
        }
    }

    public static void main(String[] args) {
        check(Arrays.equals(daysUntilWarmer(new int[] {73, 74, 75, 71, 69, 72, 76, 73}),
                new int[] {1, 1, 4, 2, 1, 1, 0, 0}), "temperatures");
        check(Arrays.equals(windowMaximum(new int[] {1, 3, -1, -3, 5, 3, 6, 7}, 3),
                new int[] {3, 3, 5, 5, 6, 7}), "window maximum");
        check(Arrays.equals(windowMaximum(new int[] {4}, 1), new int[] {4}), "single window");
        System.out.println("PASS 3 ordering-structure checks");
    }
}
