import java.util.Comparator;
import java.util.OptionalInt;
import java.util.PriorityQueue;

public final class HeapInterviewChecks {
    private HeapInterviewChecks() {}

    static final class KthLargest {
        private final int k;
        private final PriorityQueue<Integer> largest = new PriorityQueue<>();

        KthLargest(int k) {
            if (k <= 0) {
                throw new IllegalArgumentException("k must be positive");
            }
            this.k = k;
        }

        OptionalInt add(int value) {
            if (largest.size() < k) {
                largest.add(value);
            } else if (value > largest.peek()) {
                largest.poll();
                largest.add(value);
            }
            return largest.size() == k
                    ? OptionalInt.of(largest.peek()) : OptionalInt.empty();
        }
    }

    static final class MedianTracker {
        private final PriorityQueue<Integer> lower =
                new PriorityQueue<>(Comparator.reverseOrder());
        private final PriorityQueue<Integer> upper = new PriorityQueue<>();

        void add(int value) {
            if (lower.isEmpty() || value <= lower.peek()) lower.add(value);
            else upper.add(value);
            if (lower.size() > upper.size() + 1) upper.add(lower.poll());
            else if (upper.size() > lower.size()) lower.add(upper.poll());
        }

        double median() {
            if (lower.isEmpty()) throw new IllegalStateException("empty");
            if (lower.size() != upper.size()) return lower.peek();
            return ((long) lower.peek() + upper.peek()) / 2.0;
        }
    }

    private static void check(boolean condition, String message) {
        if (!condition) throw new AssertionError(message);
    }

    public static void main(String[] args) {
        KthLargest kth = new KthLargest(3);
        check(kth.add(4).isEmpty(), "first");
        check(kth.add(5).isEmpty(), "second");
        check(kth.add(8).orElseThrow() == 4, "third");
        check(kth.add(2).orElseThrow() == 4, "discard");
        MedianTracker medians = new MedianTracker();
        medians.add(Integer.MAX_VALUE);
        medians.add(Integer.MAX_VALUE);
        check(medians.median() == Integer.MAX_VALUE, "overflow-safe median");
        System.out.println("PASS 5 heap checks");
    }
}
