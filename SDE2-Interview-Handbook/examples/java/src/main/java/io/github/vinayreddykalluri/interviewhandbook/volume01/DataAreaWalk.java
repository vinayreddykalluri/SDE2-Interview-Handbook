// SPDX-License-Identifier: MIT
package io.github.vinayreddykalluri.interviewhandbook.volume01;

public class DataAreaWalk {
    private static int value = 10;

    public static void main(String[] args) {
        int local = 5;
        Integer boxed = local; // boxing allocation semantics
        recursive(local);
        System.out.println(value + boxed);
    }

    private static void recursive(int n) {
        if (n <= 0) return;
        recursive(n - 1);
    }
}
