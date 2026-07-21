// SPDX-License-Identifier: MIT
package io.github.vinayreddykalluri.interviewhandbook.volume01;

public class HotPath {
    static long compute(int x) {
        return x * x + 2L * x + 1;
    }

    public static void main(String[] args) {
        long sum = 0;
        for (int i = 0; i < 10_000_000; i++) {
            sum += compute(i);
        }
        System.out.println(sum);
    }
}
