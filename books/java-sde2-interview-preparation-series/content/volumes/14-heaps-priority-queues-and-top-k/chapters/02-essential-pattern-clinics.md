# Essential Heap Pattern Clinics

Bounded selection and k-way merge are the main heap families. K closest points applies bounded selection with safe comparison, while smallest range covering k lists extends k-way merge by tracking both frontier extremes.

## Clinic 1: k closest points with a bounded max-heap

To retain the k best points seen so far, keep the worst retained point at the heap root. For closest points, that means a max-heap by squared distance. After inserting a point, remove the root when size exceeds k.

Squared distance uses `long`:

```text
(long) x * x + (long) y * y
```

Do not use square roots, and do not subtract distances inside a comparator. Deterministic coordinate tie-breakers make tests and APIs repeatable.

This approach costs O(n log k) time and O(k) space. Sorting all points costs O(n log n). Quickselect has expected O(n) time but mutates or copies input and needs a worst-case policy.

## Clinic 2: smallest range covering one value from every sorted list

Put the first value from each list into a min-heap and track the maximum value currently represented. The heap root and current maximum define a range containing one frontier value from every list.

After evaluating the range, advance only the list that supplied the minimum. Advancing another list cannot increase the minimum, so it cannot improve the left boundary. Stop when any list is exhausted because no later frontier can cover all lists.

For:

```text
[4,10,15,24,26]
[0,9,12,20]
[5,18,22,30]
```

the best range is `[20,24]`.

There are N values across k lists. Each enters and leaves the heap at most once, giving O(N log k) time and O(k) heap space.

## Runnable Java 21 clinic

```java
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.PriorityQueue;

public final class HeapCoverageClinic {
    private HeapCoverageClinic() {
    }

    public record Point(int x, int y) {
        public long squaredDistance() {
            return (long) x * x + (long) y * y;
        }
    }

    private record Entry(int value, int listIndex, int valueIndex) {
    }

    public static List<Point> kClosest(List<Point> points, int k) {
        Objects.requireNonNull(points, "points");
        if (k < 0 || k > points.size()) {
            throw new IllegalArgumentException("k is outside the input");
        }
        Comparator<Point> worstFirst = (first, second) -> {
            int byDistance = Long.compare(second.squaredDistance(), first.squaredDistance());
            if (byDistance != 0) {
                return byDistance;
            }
            int byX = Integer.compare(second.x(), first.x());
            return byX != 0 ? byX : Integer.compare(second.y(), first.y());
        };
        PriorityQueue<Point> retained = new PriorityQueue<>(worstFirst);
        for (Point point : points) {
            retained.add(Objects.requireNonNull(point, "point"));
            if (retained.size() > k) {
                retained.remove();
            }
        }
        List<Point> answer = new ArrayList<>(retained);
        answer.sort(Comparator.comparingLong(Point::squaredDistance)
                .thenComparingInt(Point::x).thenComparingInt(Point::y));
        return List.copyOf(answer);
    }

    public static int[] smallestCoveringRange(List<List<Integer>> lists) {
        Objects.requireNonNull(lists, "lists");
        if (lists.isEmpty()) {
            throw new IllegalArgumentException("at least one list is required");
        }
        PriorityQueue<Entry> minimums = new PriorityQueue<>(
                Comparator.comparingInt(Entry::value)
                        .thenComparingInt(Entry::listIndex));
        int currentMaximum = Integer.MIN_VALUE;

        for (int listIndex = 0; listIndex < lists.size(); listIndex++) {
            List<Integer> values = Objects.requireNonNull(lists.get(listIndex), "list");
            if (values.isEmpty()) {
                throw new IllegalArgumentException("every list must be nonempty");
            }
            int first = values.get(0);
            minimums.add(new Entry(first, listIndex, 0));
            currentMaximum = Math.max(currentMaximum, first);
        }

        int bestStart = minimums.element().value();
        int bestEnd = currentMaximum;
        while (minimums.size() == lists.size()) {
            Entry minimum = minimums.remove();
            long width = (long) currentMaximum - minimum.value();
            long bestWidth = (long) bestEnd - bestStart;
            if (width < bestWidth || (width == bestWidth && minimum.value() < bestStart)) {
                bestStart = minimum.value();
                bestEnd = currentMaximum;
            }

            int nextIndex = minimum.valueIndex() + 1;
            List<Integer> source = lists.get(minimum.listIndex());
            if (nextIndex == source.size()) {
                break;
            }
            int nextValue = source.get(nextIndex);
            minimums.add(new Entry(nextValue, minimum.listIndex(), nextIndex));
            currentMaximum = Math.max(currentMaximum, nextValue);
        }
        return new int[] {bestStart, bestEnd};
    }

    public static void main(String[] args) {
        List<Point> closest = kClosest(
                List.of(new Point(1, 3), new Point(-2, 2), new Point(5, 8)), 2);
        assert closest.equals(List.of(new Point(-2, 2), new Point(1, 3)));

        int[] range = smallestCoveringRange(List.of(
                List.of(4, 10, 15, 24, 26),
                List.of(0, 9, 12, 20),
                List.of(5, 18, 22, 30)));
        assert range[0] == 20 && range[1] == 24;
        System.out.println("PASS essential heap clinics");
    }
}
```

Expected output with assertions enabled:

```text
PASS essential heap clinics
```

## Interviewer follow-up chain with model answers

**Interviewer:** Why use a max-heap for the k smallest distances?

**Candidate:** The root should be the easiest retained candidate to evict. Keeping the largest retained distance at the root makes each replacement O(log k).

**Interviewer:** Does iterating the heap give the answer in distance order?

**Candidate:** No. A priority queue guarantees only its head. If ordered output is required, I sort the k retained values or repeatedly poll a copy.

**Interviewer:** Why can the covering-range loop stop when one source ends?

**Candidate:** Every candidate range needs one value from every list. Once the list that supplied the current minimum has no successor, no later complete frontier exists.
