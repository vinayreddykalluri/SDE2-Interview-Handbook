// SPDX-License-Identifier: MIT
package io.github.vinayreddykalluri.interviewhandbook.codingfoundations.stacks;

import java.util.ArrayDeque;

public class StackPatterns {
    public static boolean isBalanced(String s) {
        ArrayDeque<Character> st = new ArrayDeque<>();
        for (char ch : s.toCharArray()) {
            if (ch == '(' || ch == '{' || ch == '[') st.push(ch);
            else if (ch == ')' || ch == '}' || ch == ']') {
                if (st.isEmpty()) return false;
                char p = st.pop();
                if ((ch == ')' && p != '(') || (ch == '}' && p != '{') || (ch == ']' && p != '[')) {
                    return false;
                }
            } else {
                return false;
            }
        }
        return st.isEmpty();
    }
}
