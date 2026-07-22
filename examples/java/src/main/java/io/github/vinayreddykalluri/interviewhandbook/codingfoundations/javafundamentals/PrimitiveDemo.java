// SPDX-License-Identifier: MIT
package io.github.vinayreddykalluri.interviewhandbook.codingfoundations.javafundamentals;

public class PrimitiveDemo {
    public static void main(String[] args) {
        int a = 2_000_000_000;
        int b = 1_000_000_000;
        int c = a + b;
        System.out.println(c); // overflow risk

        long safe = (long) a + b;
        System.out.println(safe);
    }
}
