// SPDX-License-Identifier: MIT
package io.github.vinayreddykalluri.interviewhandbook.codingfoundations.javafundamentals;

public final class AccountLockStatus {
    private AccountLockStatus() {
    }

    public static String status(int fails) {
        return fails >= 5 ? "LOCKED" : "OPEN";
    }
}
