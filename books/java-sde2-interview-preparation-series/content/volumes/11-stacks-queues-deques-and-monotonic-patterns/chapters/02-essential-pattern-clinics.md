# Essential Monotonic-Structure Pattern Clinics

Next-greater and sliding-window maximum introduce monotonic structures. Histogram area and trapped rain water test whether the reader can define what a popped index means and compute its boundaries without guessing.

## Clinic 1: largest rectangle in a histogram

Maintain indexes of bars whose heights are nondecreasing. A shorter current bar proves that every taller bar on top can no longer extend right. When height index `middle` is popped:

- the current index `right` is the first strictly shorter bar to its right;
- the new stack top is the nearest shorter bar to its left;
- width is `right` when the stack is empty, otherwise `right - stack.peek() - 1`.

For `[2, 1, 5, 6, 2, 3]`, index 4 with height 2 closes heights 6 and 5. Height 5 spans indexes 2 and 3, producing area 10.

A virtual zero-height bar at the end flushes every unresolved bar. Area uses `long` because `height * width` can overflow `int` under a large-input contract.

### Duplicate policy

Keeping equal heights or collapsing them can both be correct, but the width proof must match the chosen comparison. This implementation pops only strictly taller bars, allowing the earlier equal bar to inherit the wider span later.

## Clinic 2: trapped rain water with a monotonic stack

Maintain indexes in nonincreasing height order. When the current bar is taller than the top, the popped bar becomes a basin bottom. If the stack is then empty, no left boundary exists. Otherwise:

```text
distance = currentIndex - leftBoundaryIndex - 1
boundedHeight = min(leftHeight, currentHeight) - bottomHeight
water += distance * boundedHeight
```

Each index is pushed once and popped at most once, so the total is O(n), not O(n squared), despite the nested loop.

A two-pointer solution uses O(1) auxiliary space and is often simpler once the left-max/right-max invariant is understood. The stack version is valuable because it generalizes the boundary-closing idea used by histogram and next-greater problems.

## Runnable Java 21 clinic

```java
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Objects;

public final class MonotonicCoverageClinic {
    private MonotonicCoverageClinic() {
    }

    public static long largestRectangle(int[] heights) {
        Objects.requireNonNull(heights, "heights");
        Deque<Integer> increasing = new ArrayDeque<>();
        long best = 0;

        for (int right = 0; right <= heights.length; right++) {
            int currentHeight = right == heights.length ? 0 : heights[right];
            if (currentHeight < 0) {
                throw new IllegalArgumentException("heights must be nonnegative");
            }
            while (!increasing.isEmpty()
                    && heights[increasing.peek()] > currentHeight) {
                int height = heights[increasing.pop()];
                int width = increasing.isEmpty()
                        ? right
                        : right - increasing.peek() - 1;
                best = Math.max(best, (long) height * width);
            }
            if (right < heights.length) {
                increasing.push(right);
            }
        }
        return best;
    }

    public static long trappedWater(int[] heights) {
        Objects.requireNonNull(heights, "heights");
        Deque<Integer> decreasing = new ArrayDeque<>();
        long water = 0;

        for (int right = 0; right < heights.length; right++) {
            if (heights[right] < 0) {
                throw new IllegalArgumentException("heights must be nonnegative");
            }
            while (!decreasing.isEmpty()
                    && heights[right] > heights[decreasing.peek()]) {
                int bottom = decreasing.pop();
                if (decreasing.isEmpty()) {
                    break;
                }
                int left = decreasing.peek();
                int distance = right - left - 1;
                int boundedHeight = Math.min(heights[left], heights[right])
                        - heights[bottom];
                water += (long) distance * boundedHeight;
            }
            decreasing.push(right);
        }
        return water;
    }

    public static void main(String[] args) {
        assert largestRectangle(new int[] {2, 1, 5, 6, 2, 3}) == 10;
        assert largestRectangle(new int[0]) == 0;
        assert trappedWater(new int[] {0, 1, 0, 2, 1, 0, 1, 3, 2, 1, 2, 1}) == 6;
        System.out.println("PASS essential monotonic clinics");
    }
}
```

Expected output with assertions enabled:

```text
PASS essential monotonic clinics
```

## Interviewer follow-up chain with model answers

**Interviewer:** Why store indexes rather than heights?

**Candidate:** Width depends on positions, and duplicates need separate boundary histories. An index also gives the height, so it preserves both pieces of information.

**Interviewer:** Why is the histogram algorithm linear?

**Candidate:** Every index enters once and leaves at most once. The while-loop work aggregates to at most n pops across the complete scan.

**Interviewer:** Which rain-water solution would you code in an interview?

**Candidate:** If I can explain the left-max/right-max invariant cleanly, I would use two pointers for O(1) space. I would choose the stack when the interviewer wants a boundary-closing monotonic formulation or when extending to related basin events.
