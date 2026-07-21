// SPDX-License-Identifier: MIT
package io.github.vinayreddykalluri.interviewhandbook.volume01;

public class StringDemo {
    public static void main(String[] args) {
        String a = "hello";
        String b = new String("hello");
        System.out.println(a == b);         // false
        System.out.println(a.equals(b));     // true

        StringBuilder sb = new StringBuilder();
        sb.append("a").append("b").append("c");
        System.out.println(sb.toString());
    }
}
