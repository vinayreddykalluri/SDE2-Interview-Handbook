package com.interviewbook.examples;

/** A minimal class that is useful with javap -c -v. */
public final class CompilationPipelineDemo {
    private CompilationPipelineDemo() {}

    public static int triangular(int n) {
        if (n < 0) {
            throw new IllegalArgumentException("n must be non-negative");
        }
        int total = 0;
        for (int i = 1; i <= n; i++) {
            total += i;
        }
        return total;
    }

    public static void main(String[] args) {
        System.out.println(triangular(10));
    }
}
