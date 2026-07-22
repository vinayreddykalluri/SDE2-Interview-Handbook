// SPDX-License-Identifier: MIT
package io.github.vinayreddykalluri.interviewhandbook.codingfoundations.javafundamentals;

public final class VowelCheck {
    private VowelCheck() {
    }

    public static boolean isVowel(char c) {
        return "aeiouAEIOU".indexOf(c) >= 0;
    }
}
