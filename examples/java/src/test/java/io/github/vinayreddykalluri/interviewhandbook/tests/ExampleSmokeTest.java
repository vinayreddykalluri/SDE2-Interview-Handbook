// SPDX-License-Identifier: MIT
package io.github.vinayreddykalluri.interviewhandbook.tests;

import io.github.vinayreddykalluri.interviewhandbook.problemsolving.CoinChange;
import io.github.vinayreddykalluri.interviewhandbook.problemsolving.CourseScheduleOrder;
import io.github.vinayreddykalluri.interviewhandbook.problemsolving.DijkstraShortestPath;
import io.github.vinayreddykalluri.interviewhandbook.problemsolving.DisjointSet;
import io.github.vinayreddykalluri.interviewhandbook.problemsolving.IntervalScheduling;
import io.github.vinayreddykalluri.interviewhandbook.problemsolving.MinimumShipCapacity;
import io.github.vinayreddykalluri.interviewhandbook.problemsolving.MinimumSizeSubarraySum;
import io.github.vinayreddykalluri.interviewhandbook.problemsolving.NextGreaterElement;
import io.github.vinayreddykalluri.interviewhandbook.problemsolving.ReservoirSampling;
import io.github.vinayreddykalluri.interviewhandbook.problemsolving.SubarraySumCount;
import io.github.vinayreddykalluri.interviewhandbook.problemsolving.TopKFrequentElements;
import io.github.vinayreddykalluri.interviewhandbook.problemsolving.UniquePermutations;
import io.github.vinayreddykalluri.interviewhandbook.codingfoundations.javafundamentals.DaysInMonth;
import io.github.vinayreddykalluri.interviewhandbook.codingfoundations.javafundamentals.EvenNumberCheck;
import io.github.vinayreddykalluri.interviewhandbook.codingfoundations.javafundamentals.SumNumbers;
import io.github.vinayreddykalluri.interviewhandbook.codingfoundations.math.MathUtils;
import io.github.vinayreddykalluri.interviewhandbook.codingfoundations.loops.LoopPatterns;
import io.github.vinayreddykalluri.interviewhandbook.codingfoundations.indexing.Indexing;
import io.github.vinayreddykalluri.interviewhandbook.codingfoundations.bitmanipulation.BitOps;
import io.github.vinayreddykalluri.interviewhandbook.codingfoundations.arrays.ArrayOps;
import io.github.vinayreddykalluri.interviewhandbook.codingfoundations.strings.StringPatterns;
import io.github.vinayreddykalluri.interviewhandbook.codingfoundations.hashing.HashingDemo;
import io.github.vinayreddykalluri.interviewhandbook.codingfoundations.slidingwindow.SlidingWindow;
import io.github.vinayreddykalluri.interviewhandbook.codingfoundations.twopointers.TwoPointers;
import io.github.vinayreddykalluri.interviewhandbook.codingfoundations.prefixsum.PrefixSum;
import io.github.vinayreddykalluri.interviewhandbook.codingfoundations.binarysearch.BinarySearch;
import io.github.vinayreddykalluri.interviewhandbook.codingfoundations.stacks.StackPatterns;
import io.github.vinayreddykalluri.interviewhandbook.codingfoundations.queues.QueuePatterns;
import io.github.vinayreddykalluri.interviewhandbook.codingfoundations.graphs.GraphPatterns;
import io.github.vinayreddykalluri.interviewhandbook.codingfoundations.dynamicprogramming.DpPatterns;

import java.util.Arrays;
import java.util.List;
import java.util.Objects;

public final class ExampleSmokeTest {
    private ExampleSmokeTest() {
    }

    public static void main(String[] args) {
        checkVolume01Warmups();
        checkMathAndIndexing();
        checkLinearPatterns();
        checkSearchAndTraversal();
        checkProblemSolvingExtensions();
        System.out.println("Java example smoke checks passed");
    }

    private static void checkVolume01Warmups() {
        expectEquals(5, SumNumbers.sum(2, 3), "sum");
        expectTrue(EvenNumberCheck.isEven(-4), "negative even number");
        expectEquals(29, DaysInMonth.days(2, 2024), "leap-year February");
    }

    private static void checkMathAndIndexing() {
        expectEquals(6, MathUtils.gcd(54, 24), "greatest common divisor");
        expectEquals(10L, MathUtils.nCk(5, 2), "combination count");
        expectEquals(1, LoopPatterns.firstPositive(new int[] {-5, 3, 7}), "first positive index");
        expectEquals(5, Indexing.rangeSum(new int[] {1, 2, 3, 4}, 1, 3), "half-open range sum");
        expectTrue(BitOps.isPowerOfTwo(1024), "power of two");
    }

    private static void checkLinearPatterns() {
        int[] compacted = {0, 1, 0, 3, 12};
        ArrayOps.moveZeroes(compacted);
        expectArrayEquals(new int[] {1, 3, 12, 0, 0}, compacted, "stable zero compaction");
        expectEquals("day good a", StringPatterns.reverseWords("  a good day  "), "reverse words");
        expectEquals(2, HashingDemo.mostFrequent(new int[] {1, 2, 2, 3}), "frequency map");
        expectEquals(3, SlidingWindow.maxLenUnique(new int[] {1, 2, 1, 3, 2}), "unique window");
        expectTrue(TwoPointers.twoSumPair(new int[] {1, 2, 4, 7, 11}, 9), "sorted two sum");

        int[] prefix = PrefixSum.buildPrefix(new int[] {2, 4, 6, 8});
        expectEquals(18, PrefixSum.rangeSum(prefix, 1, 3), "inclusive prefix range");
    }

    private static void checkSearchAndTraversal() {
        expectEquals(1, BinarySearch.lowerBound(new int[] {1, 3, 3, 8}, 3), "lower bound");
        expectTrue(StackPatterns.isBalanced("([]{})"), "balanced delimiters");
        expectEquals(7, QueuePatterns.maxInWindow(new int[] {1, 3, -1, -3, 5, 3, 6, 7}, 3),
                "maximum across fixed-size windows");

        List<Integer> order = GraphPatterns.bfs(
                4,
                new int[] {0, 0, 1},
                new int[] {1, 2, 3},
                0
        );
        expectEquals(List.of(0, 1, 2, 3), order, "breadth-first traversal");
        expectEquals(8, DpPatterns.climbStairs(5), "compressed dynamic programming");
    }

    private static void checkProblemSolvingExtensions() {
        expectEquals(
                2,
                MinimumSizeSubarraySum.minLengthAtLeast(new int[] {2, 3, 1, 2, 4, 3}, 7),
                "minimum threshold window"
        );
        expectEquals(2L, SubarraySumCount.count(new int[] {1, 1, 1}, 2), "signed prefix count");
        expectArrayEquals(
                new int[] {3, 2, 3, -1, -1},
                NextGreaterElement.nextGreaterIndices(new int[] {2, 1, 2, 4, 3}),
                "next-greater indices"
        );
        expectArrayEquals(
                new int[] {0, 1, 2, 3},
                CourseScheduleOrder.order(4, new int[][] {{1, 0}, {2, 0}, {3, 1}, {3, 2}}),
                "topological course order"
        );
        expectArrayEquals(
                new long[] {0, 3, 1, 4},
                DijkstraShortestPath.shortestPaths(
                        4,
                        new int[][] {{0, 1, 4}, {0, 2, 1}, {2, 1, 2}, {1, 3, 1}, {2, 3, 5}},
                        0
                ),
                "Dijkstra distances"
        );

        DisjointSet components = new DisjointSet(5);
        expectTrue(components.union(0, 1), "first union");
        expectTrue(components.union(1, 2), "second union");
        expectTrue(components.connected(0, 2), "union-find connectivity");
        expectEquals(3, components.componentSize(1), "union-find component size");
        expectEquals(3, components.componentCount(), "union-find component count");

        expectArrayEquals(
                new int[] {1, 2},
                TopKFrequentElements.select(new int[] {1, 1, 1, 2, 2, 3}, 2),
                "top-k frequency"
        );
        List<IntervalScheduling.Interval> selected = IntervalScheduling.selectMaximum(List.of(
                new IntervalScheduling.Interval(1, 3),
                new IntervalScheduling.Interval(2, 4),
                new IntervalScheduling.Interval(3, 5),
                new IntervalScheduling.Interval(0, 7),
                new IntervalScheduling.Interval(5, 7)
        ));
        expectEquals(3, selected.size(), "greedy interval count");
        expectEquals(3, CoinChange.minCoins(new int[] {1, 2, 5}, 11), "minimum coin count");

        List<List<Integer>> permutations = UniquePermutations.generate(new int[] {1, 1, 2});
        expectEquals(3, permutations.size(), "unique permutation count");
        expectTrue(permutations.contains(List.of(2, 1, 1)), "unique permutation content");
        expectEquals(
                15L,
                MinimumShipCapacity.minimumCapacity(new int[] {1, 2, 3, 4, 5, 6, 7, 8, 9, 10}, 5),
                "minimum shipping capacity"
        );

        int[] sample = ReservoirSampling.sample(new int[] {1, 2, 3, 4, 5, 6}, 3, 42L);
        expectEquals(3, sample.length, "reservoir sample size");
        expectTrue(Arrays.stream(sample).allMatch(value -> value >= 1 && value <= 6),
                "reservoir sample membership");
        expectEquals(3L, Arrays.stream(sample).distinct().count(), "reservoir sample uniqueness");
    }

    private static void expectTrue(boolean actual, String label) {
        if (!actual) {
            throw new AssertionError(label + ": expected true");
        }
    }

    private static void expectEquals(Object expected, Object actual, String label) {
        if (!Objects.equals(expected, actual)) {
            throw new AssertionError(label + ": expected " + expected + ", got " + actual);
        }
    }

    private static void expectArrayEquals(int[] expected, int[] actual, String label) {
        if (!Arrays.equals(expected, actual)) {
            throw new AssertionError(
                    label + ": expected " + Arrays.toString(expected) + ", got " + Arrays.toString(actual)
            );
        }
    }

    private static void expectArrayEquals(long[] expected, long[] actual, String label) {
        if (!Arrays.equals(expected, actual)) {
            throw new AssertionError(
                    label + ": expected " + Arrays.toString(expected) + ", got " + Arrays.toString(actual)
            );
        }
    }
}
