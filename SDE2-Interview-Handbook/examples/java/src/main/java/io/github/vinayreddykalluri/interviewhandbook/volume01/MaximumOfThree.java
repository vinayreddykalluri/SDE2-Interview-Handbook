// SPDX-License-Identifier: MIT
package io.github.vinayreddykalluri.interviewhandbook.volume01;

public final class MaximumOfThree {
    private MaximumOfThree() {
    }

    public static int max3(int a, int b, int c) {
        return Math.max(a, Math.max(b, c));
    }
}
