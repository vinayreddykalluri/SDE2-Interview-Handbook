// SPDX-License-Identifier: MIT
package io.github.vinayreddykalluri.interviewhandbook.codingfoundations.javafundamentals;

public final class MinutesToHours {
    private MinutesToHours() {
    }

    public static String hm(int m) { return (m / 60) + " h " + (m % 60) + " m"; }
}
