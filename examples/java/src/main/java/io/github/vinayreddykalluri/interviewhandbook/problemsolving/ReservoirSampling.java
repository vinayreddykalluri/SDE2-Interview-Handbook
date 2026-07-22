// SPDX-License-Identifier: MIT
package io.github.vinayreddykalluri.interviewhandbook.problemsolving;

import java.util.Arrays;
import java.util.Objects;
import java.util.SplittableRandom;

/** One-pass uniform sampling with memory bounded by the requested sample size. */
public final class ReservoirSampling {
    private ReservoirSampling() {
    }

    /**
     * Selects {@code sampleSize} positions uniformly without replacement using O(k) memory.
     * The seed makes examples and smoke checks reproducible.
     */
    public static int[] sample(int[] stream, int sampleSize, long seed) {
        Objects.requireNonNull(stream, "stream");
        if (sampleSize < 0 || sampleSize > stream.length) {
            throw new IllegalArgumentException("sampleSize must be in [0, stream.length]");
        }

        int[] reservoir = Arrays.copyOf(stream, sampleSize);
        SplittableRandom random = new SplittableRandom(seed);
        for (int index = sampleSize; index < stream.length; index++) {
            int replacement = random.nextInt(index + 1);
            if (replacement < sampleSize) {
                reservoir[replacement] = stream[index];
            }
        }
        return reservoir;
    }
}
