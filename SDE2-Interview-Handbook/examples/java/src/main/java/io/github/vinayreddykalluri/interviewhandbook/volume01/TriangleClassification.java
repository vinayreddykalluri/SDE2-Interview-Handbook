// SPDX-License-Identifier: MIT
package io.github.vinayreddykalluri.interviewhandbook.volume01;

public final class TriangleClassification {
    private TriangleClassification() {
    }

    public static String tri(int a, int b, int c) {
        if (a == b && b == c) return "equilateral";
        if (a == b || b == c || a == c) return "isosceles";
        return "scalene";
    }
}
