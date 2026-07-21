// SPDX-License-Identifier: MIT
package io.github.vinayreddykalluri.interviewhandbook.volume08;

public class StringPatterns {
    public static String reverseWords(String s) {
        java.util.Objects.requireNonNull(s, "s");
        if (s.isBlank()) {
            return "";
        }
        String[] parts = s.trim().split("\\s+");
        StringBuilder sb = new StringBuilder();
        for (int i = parts.length - 1; i >= 0; i--) {
            if (!parts[i].isEmpty()) {
                sb.append(parts[i]);
                if (i > 0) sb.append(' ');
            }
        }
        return sb.toString();
    }
}
