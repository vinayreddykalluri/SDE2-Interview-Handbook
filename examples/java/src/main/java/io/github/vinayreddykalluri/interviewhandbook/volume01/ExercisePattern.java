// SPDX-License-Identifier: MIT
package io.github.vinayreddykalluri.interviewhandbook.volume01;

public final class ExercisePattern {
    public static int solveIfSafe(boolean condition, int value) {
        if (!condition) {
            throw new IllegalArgumentException("Invalid inputs");
        }
        return value;
    }
}
