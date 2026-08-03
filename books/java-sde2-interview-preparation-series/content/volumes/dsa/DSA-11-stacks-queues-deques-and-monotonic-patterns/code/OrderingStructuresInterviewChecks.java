import java.util.ArrayDeque;
import java.util.Arrays;
import java.util.Deque;
import java.util.NoSuchElementException;
import java.util.Random;

public final class OrderingStructuresInterviewChecks {
    private OrderingStructuresInterviewChecks() {}

    /** Primitive resizable circular deque used to expose head/size mechanics. */
    static final class IntArrayDeque {
        private int[] elements = new int[1];
        private int head;
        private int size;

        int size() {
            return size;
        }

        int capacity() {
            return elements.length;
        }

        boolean isEmpty() {
            return size == 0;
        }

        void addFirst(int value) {
            ensureCapacity();
            head = decrement(head, elements.length);
            elements[head] = value;
            size++;
        }

        void addLast(int value) {
            ensureCapacity();
            elements[physicalIndex(size)] = value;
            size++;
        }

        int peekFirst() {
            requireNotEmpty();
            return elements[head];
        }

        int peekLast() {
            requireNotEmpty();
            return elements[physicalIndex(size - 1)];
        }

        int removeFirst() {
            int value = peekFirst();
            head = increment(head, elements.length);
            size--;
            if (size == 0) {
                head = 0;
            }
            return value;
        }

        int removeLast() {
            int value = peekLast();
            size--;
            if (size == 0) {
                head = 0;
            }
            return value;
        }

        int[] logicalOrder() {
            int[] result = new int[size];
            for (int offset = 0; offset < size; offset++) {
                result[offset] = elements[physicalIndex(offset)];
            }
            return result;
        }

        private int physicalIndex(int logicalOffset) {
            return (head + logicalOffset) % elements.length;
        }

        private void ensureCapacity() {
            if (size < elements.length) {
                return;
            }
            int[] expanded = new int[Math.multiplyExact(elements.length, 2)];
            for (int offset = 0; offset < size; offset++) {
                expanded[offset] = elements[physicalIndex(offset)];
            }
            elements = expanded;
            head = 0;
        }

        private void requireNotEmpty() {
            if (size == 0) {
                throw new NoSuchElementException("empty deque");
            }
        }

        private static int increment(int index, int length) {
            return index + 1 == length ? 0 : index + 1;
        }

        private static int decrement(int index, int length) {
            return index == 0 ? length - 1 : index - 1;
        }
    }

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

    static long largestRectangleArea(int[] heights) {
        Deque<Integer> increasing = new ArrayDeque<>();
        long best = 0;
        for (int index = 0; index <= heights.length; index++) {
            int currentHeight = index == heights.length ? 0 : heights[index];
            if (currentHeight < 0) {
                throw new IllegalArgumentException("heights cannot be negative");
            }
            while (!increasing.isEmpty()
                    && heights[increasing.peek()] > currentHeight) {
                int height = heights[increasing.pop()];
                int leftSmaller = increasing.isEmpty() ? -1 : increasing.peek();
                long width = index - (long) leftSmaller - 1L;
                best = Math.max(best, height * width);
            }
            if (index < heights.length) {
                increasing.push(index);
            }
        }
        return best;
    }

    static long evaluatePostfix(String[] tokens) {
        Deque<Long> operands = new ArrayDeque<>();
        for (String token : tokens) {
            if (token.equals("+") || token.equals("-")
                    || token.equals("*") || token.equals("/")) {
                if (operands.size() < 2) {
                    throw new IllegalArgumentException("operator needs two operands");
                }
                long right = operands.pop();
                long left = operands.pop();
                long result = switch (token) {
                    case "+" -> Math.addExact(left, right);
                    case "-" -> Math.subtractExact(left, right);
                    case "*" -> Math.multiplyExact(left, right);
                    case "/" -> {
                        if (right == 0 || left == Long.MIN_VALUE && right == -1) {
                            throw new ArithmeticException("invalid division");
                        }
                        yield left / right;
                    }
                    default -> throw new AssertionError("unreachable operator");
                };
                operands.push(result);
            } else {
                try {
                    operands.push(Long.parseLong(token));
                } catch (NumberFormatException invalid) {
                    throw new IllegalArgumentException("invalid token: " + token, invalid);
                }
            }
        }
        if (operands.size() != 1) {
            throw new IllegalArgumentException("expression must produce one value");
        }
        return operands.pop();
    }

    private static boolean circularDequeMatchesArrayDeque() {
        Random random = new Random(53L);
        IntArrayDeque custom = new IntArrayDeque();
        Deque<Integer> expected = new ArrayDeque<>();
        for (int operation = 0; operation < 10_000; operation++) {
            int choice = random.nextInt(6);
            if (expected.isEmpty() || choice < 2) {
                int value = random.nextInt();
                if ((choice & 1) == 0) {
                    custom.addFirst(value);
                    expected.addFirst(value);
                } else {
                    custom.addLast(value);
                    expected.addLast(value);
                }
            } else if (choice == 2) {
                if (custom.removeFirst() != expected.removeFirst()) {
                    return false;
                }
            } else if (choice == 3) {
                if (custom.removeLast() != expected.removeLast()) {
                    return false;
                }
            } else if (choice == 4 && custom.peekFirst() != expected.peekFirst()) {
                return false;
            } else if (choice == 5 && custom.peekLast() != expected.peekLast()) {
                return false;
            }
            if (custom.size() != expected.size()) {
                return false;
            }
            int index = 0;
            int[] logical = custom.logicalOrder();
            for (int value : expected) {
                if (logical[index++] != value) {
                    return false;
                }
            }
        }
        return true;
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

        IntArrayDeque deque = new IntArrayDeque();
        int initialCapacity = deque.capacity();
        deque.addLast(2);
        deque.addFirst(1);
        deque.addLast(3);
        check(Arrays.equals(deque.logicalOrder(), new int[] {1, 2, 3}), "deque order");
        check(deque.capacity() > initialCapacity, "deque resizes");
        check(deque.removeFirst() == 1 && deque.removeLast() == 3 && deque.peekFirst() == 2,
                "both ends");
        check(deque.removeFirst() == 2 && deque.isEmpty(), "empty after removal");
        expectFailure(deque::removeFirst, NoSuchElementException.class);
        check(circularDequeMatchesArrayDeque(), "deque differential test");

        check(largestRectangleArea(new int[] {2, 1, 5, 6, 2, 3}) == 10L,
                "largest rectangle");
        check(largestRectangleArea(new int[0]) == 0L, "empty histogram");
        check(largestRectangleArea(new int[] {Integer.MAX_VALUE, Integer.MAX_VALUE})
                == 2L * Integer.MAX_VALUE, "histogram uses long area");
        expectFailure(() -> largestRectangleArea(new int[] {1, -1}),
                IllegalArgumentException.class);

        check(evaluatePostfix(new String[] {"2", "1", "+", "3", "*"}) == 9L,
                "postfix expression");
        check(evaluatePostfix(new String[] {"7", "-3", "/"}) == -2L,
                "division truncates toward zero");
        expectFailure(() -> evaluatePostfix(new String[] {"+"}),
                IllegalArgumentException.class);
        System.out.println("PASS 16 ordering-structure checks");
    }
}
