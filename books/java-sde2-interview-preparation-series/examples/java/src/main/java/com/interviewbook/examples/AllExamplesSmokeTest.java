package com.interviewbook.examples;

public final class AllExamplesSmokeTest {
    private AllExamplesSmokeTest() {}

    public static void main(String[] args) throws Exception {
        if (CompilationPipelineDemo.triangular(10) != 55) {
            throw new AssertionError();
        }
        PassByValueDemo.verify();
        EqualityAndHashingDemo.verify();
        GenericAlgorithms.verify();
        CollectionsChoicesDemo.verify();
        StreamAggregationDemo.verify();
        ExecutorCancellationDemo.verify();
        SafePublicationDemo.verify();
        ConcurrentMemoizer.verify();
        VirtualThreadsDemo.verify();
        LruCache.verify();
        GraphAlgorithms.verify();
        DynamicProgramming.verify();
        System.out.println("All Java 21 examples passed.");
    }
}
