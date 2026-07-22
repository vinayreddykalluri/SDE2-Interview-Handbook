// SPDX-License-Identifier: MIT
package io.github.vinayreddykalluri.interviewhandbook.codingfoundations.javafundamentals;

public final class Factorial {
    private Factorial() {
    }

    public static long factorial(int n) {
        long ans = 1;
        for (int i = 2; i <= n; i++) ans *= i;
        return ans;
    }
}
