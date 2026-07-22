// SPDX-License-Identifier: MIT
package io.github.vinayreddykalluri.interviewhandbook.codingfoundations.javafundamentals;

import java.util.Arrays;

public final class PrimeCount {
    private PrimeCount() {
    }

    public static int primeCount(int n) {
        if (n < 2) return 0;
        boolean[] p = new boolean[n + 1];
        Arrays.fill(p, true);
        p[0] = p[1] = false;
        for (int i = 2; i * i <= n; i++) if (p[i])
            for (int j = i * i; j <= n; j += i) p[j] = false;
        int c = 0;
        for (int i = 2; i <= n; i++) if (p[i]) c++;
        return c;
    }
}
