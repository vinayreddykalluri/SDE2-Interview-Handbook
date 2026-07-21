// SPDX-License-Identifier: MIT
package io.github.vinayreddykalluri.interviewhandbook.volume01;

public final class ArrayValueSwap {
    private ArrayValueSwap() {
    }

    public static void swap(int[] a) {
        a[0] = a[0] ^ a[1];
        a[1] = a[0] ^ a[1];
        a[0] = a[0] ^ a[1];
    }
}
