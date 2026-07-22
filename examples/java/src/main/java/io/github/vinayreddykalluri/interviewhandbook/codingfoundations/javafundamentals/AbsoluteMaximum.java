// SPDX-License-Identifier: MIT
package io.github.vinayreddykalluri.interviewhandbook.codingfoundations.javafundamentals;

public final class AbsoluteMaximum {
    private AbsoluteMaximum() {
    }

    public static int absMax(int a, int b) {
        return Math.abs(a) >= Math.abs(b) ? a : b;
    }
}
