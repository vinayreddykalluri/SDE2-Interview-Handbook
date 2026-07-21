// SPDX-License-Identifier: MIT
package io.github.vinayreddykalluri.interviewhandbook.volume06;

public class BitOps {
    public static boolean isPowerOfTwo(int x) {
        return x > 0 && (x & (x - 1)) == 0;
    }

    public static int toggleBit(int x, int pos) {
        if (pos < 0 || pos >= Integer.SIZE) {
            throw new IllegalArgumentException("pos must be in [0, 31]");
        }
        return x ^ (1 << pos);
    }
}
