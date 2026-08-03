import java.util.Arrays;

public final class ComplexityDeepDiveExamples {
    private ComplexityDeepDiveExamples() {
    }

    private static final class DoublingIntArray {
        private int[] values = new int[1];
        private int size;
        private long copiedElements;

        void add(int value) {
            if (size == values.length) {
                int[] grown = new int[Math.multiplyExact(values.length, 2)];
                System.arraycopy(values, 0, grown, 0, size);
                copiedElements = Math.addExact(copiedElements, size);
                values = grown;
            }
            values[size++] = value;
        }

        long copiedElements() {
            return copiedElements;
        }
    }

    private record PointerCount(int leftMoves, int rightMoves) {
        int totalMoves() {
            return Math.addExact(leftMoves, rightMoves);
        }
    }

    private static PointerCount countWindowMoves(int[] values, int limit) {
        int left = 0;
        int right = 0;
        int sum = 0;
        int leftMoves = 0;
        int rightMoves = 0;

        while (right < values.length) {
            sum = Math.addExact(sum, values[right]);
            right++;
            rightMoves++;
            while (sum > limit && left < right) {
                sum -= values[left];
                left++;
                leftMoves++;
            }
        }
        return new PointerCount(leftMoves, rightMoves);
    }

    private static long naiveFibonacciCalls(int n) {
        if (n < 2) {
            return 1;
        }
        return Math.addExact(
                1,
                Math.addExact(naiveFibonacciCalls(n - 1), naiveFibonacciCalls(n - 2)));
    }

    private static long memoizedFibonacciCalls(int n) {
        long[] values = new long[n + 1];
        Arrays.fill(values, -1);
        long[] calls = {0};
        fibonacci(n, values, calls);
        return calls[0];
    }

    private static long fibonacci(int n, long[] values, long[] calls) {
        if (values[n] >= 0) {
            return values[n];
        }
        calls[0]++;
        if (n < 2) {
            values[n] = n;
        } else {
            values[n] = Math.addExact(
                    fibonacci(n - 1, values, calls),
                    fibonacci(n - 2, values, calls));
        }
        return values[n];
    }

    private static void require(boolean condition, String message) {
        if (!condition) {
            throw new AssertionError(message);
        }
    }

    public static void main(String[] args) {
        int appendCount = 1_000;
        DoublingIntArray numbers = new DoublingIntArray();
        for (int value = 0; value < appendCount; value++) {
            numbers.add(value);
        }
        require(numbers.copiedElements() < 2L * appendCount,
                "geometric copy bound must remain below 2n");

        int[] windowInput = {2, 1, 3, 1, 1, 2, 4};
        PointerCount moves = countWindowMoves(windowInput, 5);
        require(moves.leftMoves() <= windowInput.length, "left pointer moved too far");
        require(moves.rightMoves() == windowInput.length, "right pointer must visit every item");
        require(moves.totalMoves() <= 2 * windowInput.length, "aggregate pointer bound failed");

        long naiveCalls = naiveFibonacciCalls(20);
        long memoCalls = memoizedFibonacciCalls(20);
        require(naiveCalls > memoCalls * 100, "memoization should remove repeated states");

        System.out.println("appends=" + appendCount
                + ", copied=" + numbers.copiedElements());
        System.out.println("windowMoves=" + moves.totalMoves()
                + ", bound=" + (2 * windowInput.length));
        System.out.println("fib20 calls: naive=" + naiveCalls
                + ", memoizedStates=" + memoCalls);
        System.out.println("PASS complexity deep-dive checks");
    }
}
