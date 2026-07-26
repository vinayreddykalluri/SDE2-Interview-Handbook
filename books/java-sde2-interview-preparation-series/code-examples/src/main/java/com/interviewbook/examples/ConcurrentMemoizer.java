package com.interviewbook.examples;

import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.function.Function;

public final class ConcurrentMemoizer<K, V> {
    private final ConcurrentMap<K, CompletableFuture<V>> cache = new ConcurrentHashMap<>();

    public V compute(K key, Function<? super K, ? extends V> loader) {
        Objects.requireNonNull(key);
        Objects.requireNonNull(loader);
        CompletableFuture<V> created = new CompletableFuture<>();
        CompletableFuture<V> existing = cache.putIfAbsent(key, created);
        CompletableFuture<V> result = existing == null ? created : existing;
        if (existing == null) {
            try {
                created.complete(loader.apply(key));
            } catch (Throwable failure) {
                created.completeExceptionally(failure);
                cache.remove(key, created);
            }
        }
        return result.join();
    }

    public static void verify() {
        ConcurrentMemoizer<String, Integer> memoizer = new ConcurrentMemoizer<>();
        int length = memoizer.compute("java", String::length);
        if (length != 4) {
            throw new AssertionError();
        }
    }
}
