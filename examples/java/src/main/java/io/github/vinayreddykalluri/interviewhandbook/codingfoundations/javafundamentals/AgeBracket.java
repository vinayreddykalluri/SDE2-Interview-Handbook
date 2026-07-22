// SPDX-License-Identifier: MIT
package io.github.vinayreddykalluri.interviewhandbook.codingfoundations.javafundamentals;

public final class AgeBracket {
    private AgeBracket() {
    }

    public static String bracket(int age) {
        if (age < 13) return "Child";
        if (age < 20) return "Teen";
        return "Adult";
    }
}
