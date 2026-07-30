# Essential Greedy Invariant Clinics

Greedy problems are not unified by sorting alone. Candy allocation and partition labels use different forms of irreversible information: local directional constraints and a farthest required boundary.

## Clinic 1: candy allocation with two directional passes

Each child must receive at least one candy. A child with a higher rating than an adjacent child must receive more candy than that neighbor.

A left-to-right pass satisfies every rising constraint from the left. A right-to-left pass satisfies every rising constraint from the right. At each position, keep the maximum requirement from the two directions.

For ratings `[1, 0, 2]`:

- initial candies: `[1, 1, 1]`;
- left pass: `[1, 1, 2]`;
- right pass: `[2, 1, 2]`;
- answer: 5.

Why is it minimal? Each pass computes a lower bound forced by one neighbor direction. Taking their maximum is necessary to satisfy both, and the construction meets those bounds exactly.

Use `long` for the total when n can be large. A slope-counting solution can reduce auxiliary space to O(1), but its peak accounting is easier to get wrong and should follow, not replace, the two-pass derivation.

## Clinic 2: partition labels by closing obligations

Record the last position of each symbol. While scanning a partition, extend its required end to the farthest last occurrence of any symbol already seen. When the current index reaches that end, every symbol in the partition has fulfilled its future obligation, so the partition can close.

Closing earlier is impossible because at least one seen symbol would occur again outside. Closing exactly at the farthest obligation therefore yields the maximum number of valid partitions.

The code below defines symbols as Java UTF-16 `char` values. If the product contract means Unicode code points or user-perceived characters, iterate and index that representation explicitly.

## Runnable Java 21 clinic

```java
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;

public final class GreedyCoverageClinic {
    private GreedyCoverageClinic() {
    }

    public static long minimumCandies(int[] ratings) {
        Objects.requireNonNull(ratings, "ratings");
        if (ratings.length == 0) {
            return 0;
        }
        int[] candies = new int[ratings.length];
        Arrays.fill(candies, 1);

        for (int index = 1; index < ratings.length; index++) {
            if (ratings[index] > ratings[index - 1]) {
                candies[index] = candies[index - 1] + 1;
            }
        }
        for (int index = ratings.length - 2; index >= 0; index--) {
            if (ratings[index] > ratings[index + 1]) {
                candies[index] = Math.max(candies[index], candies[index + 1] + 1);
            }
        }

        long total = 0;
        for (int candy : candies) {
            total += candy;
        }
        return total;
    }

    public static List<Integer> partitionLabels(String text) {
        Objects.requireNonNull(text, "text");
        int[] last = new int[Character.MAX_VALUE + 1];
        Arrays.fill(last, -1);
        for (int index = 0; index < text.length(); index++) {
            last[text.charAt(index)] = index;
        }

        List<Integer> lengths = new ArrayList<>();
        int start = 0;
        int requiredEnd = -1;
        for (int index = 0; index < text.length(); index++) {
            requiredEnd = Math.max(requiredEnd, last[text.charAt(index)]);
            if (index == requiredEnd) {
                lengths.add(index - start + 1);
                start = index + 1;
            }
        }
        return List.copyOf(lengths);
    }

    public static void main(String[] args) {
        assert minimumCandies(new int[] {1, 0, 2}) == 5;
        assert minimumCandies(new int[] {1, 2, 2}) == 4;
        assert partitionLabels("ababcbacadefegdehijhklij").equals(List.of(9, 7, 8));
        System.out.println("PASS essential greedy clinics");
    }
}
```

Expected output with assertions enabled:

```text
PASS essential greedy clinics
```

## Interviewer follow-up chain with model answers

**Interviewer:** Why can candy not be solved by one left-to-right pass?

**Candidate:** A descending run imposes constraints from the right that are unknown during a left-only pass. The second pass supplies exactly those missing lower bounds.

**Interviewer:** Is partition labels an interval-merging problem?

**Candidate:** It can be viewed that way: each symbol spans first to last occurrence, and overlapping spans belong to one component. The one-pass farthest-end formulation performs that merge implicitly.

**Interviewer:** What should you say before presenting a greedy proof?

**Candidate:** State the exact objective and constraints, name the invariant or exchange, and show why no discarded decision can improve a later solution. A rule that merely works on examples is a heuristic, not yet a greedy proof.
