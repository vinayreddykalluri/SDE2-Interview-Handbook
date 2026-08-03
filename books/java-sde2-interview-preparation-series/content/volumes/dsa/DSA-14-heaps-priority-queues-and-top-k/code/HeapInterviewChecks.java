import java.util.Arrays;
import java.util.Comparator;
import java.util.NoSuchElementException;
import java.util.OptionalInt;
import java.util.PriorityQueue;
import java.util.Random;

public final class HeapInterviewChecks {
    private HeapInterviewChecks() {}

    /** A resizable primitive min-heap: parent=(i-1)/2, children=2i+1 and 2i+2. */
    static final class IntMinHeap {
        private int[] heap;
        private int size;

        IntMinHeap() {
            heap = new int[1];
        }

        IntMinHeap(int[] values) {
            heap = Arrays.copyOf(values, Math.max(1, values.length));
            size = values.length;
            for (int parent = size / 2 - 1; parent >= 0; parent--) {
                siftDown(parent);
            }
        }

        int size() {
            return size;
        }

        int capacity() {
            return heap.length;
        }

        boolean isEmpty() {
            return size == 0;
        }

        void offer(int value) {
            ensureCapacity();
            heap[size] = value;
            siftUp(size++);
        }

        int peek() {
            if (size == 0) {
                throw new NoSuchElementException("empty heap");
            }
            return heap[0];
        }

        int poll() {
            int minimum = peek();
            heap[0] = heap[--size];
            if (size > 0) {
                siftDown(0);
            }
            return minimum;
        }

        boolean invariantHolds() {
            for (int child = 1; child < size; child++) {
                int parent = (child - 1) / 2;
                if (heap[parent] > heap[child]) {
                    return false;
                }
            }
            return true;
        }

        int[] levelOrderForTeaching() {
            return Arrays.copyOf(heap, size);
        }

        private void siftUp(int child) {
            while (child > 0) {
                int parent = (child - 1) / 2;
                if (heap[parent] <= heap[child]) {
                    return;
                }
                swap(heap, parent, child);
                child = parent;
            }
        }

        private void siftDown(int parent) {
            while (true) {
                int left = parent * 2 + 1;
                if (left >= size) {
                    return;
                }
                int right = left + 1;
                int smaller = right < size && heap[right] < heap[left] ? right : left;
                if (heap[parent] <= heap[smaller]) {
                    return;
                }
                swap(heap, parent, smaller);
                parent = smaller;
            }
        }

        private void ensureCapacity() {
            if (size < heap.length) {
                return;
            }
            int newCapacity = heap.length <= Integer.MAX_VALUE / 2
                    ? heap.length * 2 : Integer.MAX_VALUE;
            if (newCapacity == heap.length) {
                throw new IllegalStateException("heap is too large");
            }
            heap = Arrays.copyOf(heap, newCapacity);
        }
    }

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

    /** Returns the one-based kth-largest value without changing caller input. */
    static int quickselectKthLargest(int[] input, int k, long seed) {
        if (input == null || input.length == 0) {
            throw new IllegalArgumentException("non-empty input required");
        }
        if (k < 1 || k > input.length) {
            throw new IllegalArgumentException("k must be in [1,n]");
        }
        int[] values = input.clone();
        int target = values.length - k;
        int left = 0;
        int right = values.length - 1;
        Random random = new Random(seed);
        while (left <= right) {
            int pivot = values[left + random.nextInt(right - left + 1)];
            int lower = left;
            int scan = left;
            int upper = right;
            while (scan <= upper) {
                if (values[scan] < pivot) {
                    swap(values, lower++, scan++);
                } else if (values[scan] > pivot) {
                    swap(values, scan, upper--);
                } else {
                    scan++;
                }
            }
            if (target < lower) {
                right = lower - 1;
            } else if (target > upper) {
                left = upper + 1;
            } else {
                return values[target];
            }
        }
        throw new AssertionError("valid rank was not found");
    }

    private static boolean customHeapMatchesPriorityQueue() {
        Random random = new Random(29L);
        IntMinHeap custom = new IntMinHeap();
        PriorityQueue<Integer> standard = new PriorityQueue<>();
        for (int operation = 0; operation < 5_000; operation++) {
            if (standard.isEmpty() || random.nextInt(3) != 0) {
                int value = random.nextInt();
                custom.offer(value);
                standard.offer(value);
            } else if (custom.poll() != standard.remove()) {
                return false;
            }
            if (custom.size() != standard.size() || !custom.invariantHolds()
                    || (!standard.isEmpty() && custom.peek() != standard.peek())) {
                return false;
            }
        }
        while (!standard.isEmpty()) {
            if (custom.poll() != standard.remove()) {
                return false;
            }
        }
        return custom.isEmpty();
    }

    private static boolean quickselectMatchesSorting() {
        Random random = new Random(31L);
        for (int trial = 0; trial < 1_000; trial++) {
            int length = 1 + random.nextInt(40);
            int[] input = new int[length];
            for (int index = 0; index < length; index++) {
                input[index] = random.nextInt(31) - 15;
            }
            int[] sorted = input.clone();
            Arrays.sort(sorted);
            for (int k = 1; k <= length; k++) {
                int expected = sorted[length - k];
                if (quickselectKthLargest(input, k, trial * 41L + k) != expected) {
                    return false;
                }
            }
        }
        return true;
    }

    private static void swap(int[] values, int first, int second) {
        int temporary = values[first];
        values[first] = values[second];
        values[second] = temporary;
    }

    private static void expectFailure(Runnable action, Class<? extends Throwable> type) {
        try {
            action.run();
        } catch (Throwable failure) {
            if (type.isInstance(failure)) {
                return;
            }
            throw new AssertionError("wrong failure type", failure);
        }
        throw new AssertionError("expected " + type.getSimpleName());
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

        IntMinHeap heapified = new IntMinHeap(new int[] {7, 2, 9, 2, -1});
        check(heapified.invariantHolds(), "bottom-up heapify invariant");
        check(Arrays.equals(heapified.levelOrderForTeaching(), new int[] {-1, 2, 9, 7, 2}),
                "heap shape is level order, not sorted order");
        check(heapified.poll() == -1 && heapified.poll() == 2 && heapified.poll() == 2,
                "poll order");
        IntMinHeap growing = new IntMinHeap();
        int initialCapacity = growing.capacity();
        growing.offer(Integer.MAX_VALUE);
        growing.offer(Integer.MIN_VALUE);
        check(growing.capacity() > initialCapacity, "backing array resizes");
        check(growing.peek() == Integer.MIN_VALUE, "extreme values need no subtraction comparator");
        expectFailure(() -> new IntMinHeap().poll(), NoSuchElementException.class);

        int[] selectionInput = {5, 1, 5, -2, 9, 9, 3};
        check(quickselectKthLargest(selectionInput, 1, 1L) == 9, "largest rank");
        check(quickselectKthLargest(selectionInput, 4, 2L) == 5, "duplicate rank");
        check(Arrays.equals(selectionInput, new int[] {5, 1, 5, -2, 9, 9, 3}),
                "selection preserves caller input");
        expectFailure(() -> quickselectKthLargest(selectionInput, 0, 3L),
                IllegalArgumentException.class);
        check(customHeapMatchesPriorityQueue(), "heap differential test");
        check(quickselectMatchesSorting(), "selection differential test");

        System.out.println("PASS 17 heap checks");
    }
}
