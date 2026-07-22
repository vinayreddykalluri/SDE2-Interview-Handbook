// SPDX-License-Identifier: MIT
package io.github.vinayreddykalluri.interviewhandbook.codingfoundations.javafundamentals;

public final class ReverseInteger {
    private ReverseInteger() {
    }

    public static int rev(int n) {
        int r = 0;
        while (n != 0) {
            r = r * 10 + (n % 10);
            n /= 10;
        }
        return r;
    }
}
