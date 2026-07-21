// SPDX-License-Identifier: MIT
package io.github.vinayreddykalluri.interviewhandbook.volume01;

public final class SquarePattern {
    private SquarePattern() {
    }

    public static void star(int n) {
        for (int i = 0; i < n; i++) {
            for (int j = 0; j < n; j++) System.out.print("*");
            System.out.println();
        }
    }
}
