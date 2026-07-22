// SPDX-License-Identifier: MIT
package io.github.vinayreddykalluri.interviewhandbook.problemsolving;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;

/** Backtracking example that prunes duplicate branches at each decision depth. */
public final class UniquePermutations {
    private UniquePermutations() {
    }

    /** Returns every distinct permutation in lexicographic traversal order. */
    public static List<List<Integer>> generate(int[] values) {
        Objects.requireNonNull(values, "values");
        int[] ordered = values.clone();
        Arrays.sort(ordered);
        List<List<Integer>> result = new ArrayList<>();
        build(ordered, new boolean[ordered.length], new ArrayList<>(), result);
        return result;
    }

    private static void build(
            int[] values,
            boolean[] used,
            List<Integer> current,
            List<List<Integer>> result
    ) {
        if (current.size() == values.length) {
            result.add(List.copyOf(current));
            return;
        }

        for (int index = 0; index < values.length; index++) {
            if (used[index]) {
                continue;
            }
            if (index > 0 && values[index] == values[index - 1] && !used[index - 1]) {
                continue;
            }
            used[index] = true;
            current.add(values[index]);
            build(values, used, current, result);
            current.remove(current.size() - 1);
            used[index] = false;
        }
    }
}
