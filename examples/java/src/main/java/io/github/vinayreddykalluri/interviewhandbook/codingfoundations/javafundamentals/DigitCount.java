// SPDX-License-Identifier: MIT
package io.github.vinayreddykalluri.interviewhandbook.codingfoundations.javafundamentals;

public final class DigitCount {
    private DigitCount() {
    }

    public static int digits(int n) {
        if (n == 0) return 1;
        int c = 0;
        while (n != 0) { n /= 10; c++; }
        return c;
    }
}
