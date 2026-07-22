// SPDX-License-Identifier: MIT
package io.github.vinayreddykalluri.interviewhandbook.codingfoundations.javafundamentals;

public final class PrintOddNumbers {
    private PrintOddNumbers() {
    }

    public static void printOdd(int n) {
        for (int i = 0; i < n; i++) {
            System.out.print((2 * i + 1) + " ");
        }
    }
}
