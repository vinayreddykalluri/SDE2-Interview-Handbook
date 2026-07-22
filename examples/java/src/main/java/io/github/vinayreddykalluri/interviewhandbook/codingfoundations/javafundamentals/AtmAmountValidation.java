// SPDX-License-Identifier: MIT
package io.github.vinayreddykalluri.interviewhandbook.codingfoundations.javafundamentals;

public final class AtmAmountValidation {
    private AtmAmountValidation() {
    }

    public static boolean validNotes(int amount) {
        return amount > 0 && amount % 10 == 0;
    }
}
