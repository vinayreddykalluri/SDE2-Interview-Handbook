// SPDX-License-Identifier: MIT
package io.github.vinayreddykalluri.interviewhandbook.tests;

import io.github.vinayreddykalluri.interviewhandbook.volume01.DaysInMonth;
import io.github.vinayreddykalluri.interviewhandbook.volume01.EvenNumberCheck;
import io.github.vinayreddykalluri.interviewhandbook.volume01.SumNumbers;
import io.github.vinayreddykalluri.interviewhandbook.volume03.MathUtils;
import io.github.vinayreddykalluri.interviewhandbook.volume04.LoopPatterns;
import io.github.vinayreddykalluri.interviewhandbook.volume05.Indexing;
import io.github.vinayreddykalluri.interviewhandbook.volume06.BitOps;
import io.github.vinayreddykalluri.interviewhandbook.volume07.ArrayOps;
import io.github.vinayreddykalluri.interviewhandbook.volume08.StringPatterns;
import io.github.vinayreddykalluri.interviewhandbook.volume09.HashingDemo;
import io.github.vinayreddykalluri.interviewhandbook.volume10.SlidingWindow;
import io.github.vinayreddykalluri.interviewhandbook.volume11.TwoPointers;
import io.github.vinayreddykalluri.interviewhandbook.volume12.PrefixSum;
import io.github.vinayreddykalluri.interviewhandbook.volume13.BinarySearch;
import io.github.vinayreddykalluri.interviewhandbook.volume14.StackPatterns;
import io.github.vinayreddykalluri.interviewhandbook.volume15.QueuePatterns;
import io.github.vinayreddykalluri.interviewhandbook.volume18.GraphPatterns;
import io.github.vinayreddykalluri.interviewhandbook.volume19.DpPatterns;

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
}
