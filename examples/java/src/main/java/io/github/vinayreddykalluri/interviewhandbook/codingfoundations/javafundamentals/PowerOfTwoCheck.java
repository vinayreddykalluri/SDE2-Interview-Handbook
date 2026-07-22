// SPDX-License-Identifier: MIT
package io.github.vinayreddykalluri.interviewhandbook.codingfoundations.javafundamentals;

public final class PowerOfTwoCheck {
    private PowerOfTwoCheck() {
    }

    public static boolean power2(int n) {
        return n > 0 && (n & (n - 1)) == 0;
    }
}
