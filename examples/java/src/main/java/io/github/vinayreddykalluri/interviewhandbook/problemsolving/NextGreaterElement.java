// SPDX-License-Identifier: MIT
package io.github.vinayreddykalluri.interviewhandbook.problemsolving;

import java.util.ArrayDeque;
import java.util.Arrays;
import java.util.Objects;

/** Monotonic-stack example that resolves each index once. */
public final class NextGreaterElement {
    private NextGreaterElement() {
    }

    /**
     * Returns the index of the first strictly greater value to the right, or -1.
     *
     * <p>The stack contains unresolved indices with non-increasing values. Each index is pushed
     * and popped at most once, so time is O(n) and auxiliary space is O(n).
     */
    public static int[] nextGreaterIndices(int[] values) {
        Objects.requireNonNull(values, "values");
        int[] result = new int[values.length];
        Arrays.fill(result, -1);
        ArrayDeque<Integer> unresolved = new ArrayDeque<>();

        for (int index = 0; index < values.length; index++) {
            while (!unresolved.isEmpty() && values[unresolved.peek()] < values[index]) {
                result[unresolved.pop()] = index;
            }
            unresolved.push(index);
        }
        return result;
    }
}
