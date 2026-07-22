// SPDX-License-Identifier: MIT
package io.github.vinayreddykalluri.interviewhandbook.codingfoundations.javafundamentals;

public final class LeapYearCheck {
    private LeapYearCheck() {
    }

    public static boolean isLeapYear(int y) {
        return (y % 400 == 0) || (y % 4 == 0 && y % 100 != 0);
    }
}
