// SPDX-License-Identifier: MIT
package io.github.vinayreddykalluri.interviewhandbook.volume01;

public final class CharacterType {
    private CharacterType() {
    }

    public static String type(char c) {
        if (Character.isLetter(c)) return "Letter";
        if (Character.isDigit(c)) return "Digit";
        return "Other";
    }
}
