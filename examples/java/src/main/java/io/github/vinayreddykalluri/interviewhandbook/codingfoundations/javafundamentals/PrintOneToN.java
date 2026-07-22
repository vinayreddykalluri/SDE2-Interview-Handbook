// SPDX-License-Identifier: MIT
package io.github.vinayreddykalluri.interviewhandbook.codingfoundations.javafundamentals;

public final class PrintOneToN {
    private PrintOneToN() {
    }

    public static void printN(int n) {
        for (int i = 1; i <= n; i++) {
            System.out.print(i + " ");
        }
    }
}
