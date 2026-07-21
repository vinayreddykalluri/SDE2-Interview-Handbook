// SPDX-License-Identifier: MIT
package io.github.vinayreddykalluri.interviewhandbook.volume01;

public class OperatorDemo {
    public static void main(String[] args) {
        int a = 10;
        int b = 3;
        boolean safe = b != 0 && a / b > 1;
        System.out.println(safe);
        System.out.println(a & 3);
    }
}
