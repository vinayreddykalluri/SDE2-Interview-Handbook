// SPDX-License-Identifier: MIT
package io.github.vinayreddykalluri.interviewhandbook.volume01;

public class WrapperDemo {
    public static void main(String[] args) {
        Integer a = 100;
        Integer b = 100;
        Integer c = 200;
        Integer d = 200;

        System.out.println(a == b); // true due to Integer cache
        System.out.println(c == d); // usually false
        System.out.println(a.equals(b));
    }
}
