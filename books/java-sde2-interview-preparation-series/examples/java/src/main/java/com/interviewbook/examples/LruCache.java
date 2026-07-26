package com.interviewbook.examples;

import java.util.LinkedHashMap;
import java.util.Map;

public final class LruCache<K, V> {
    private final int capacity;
    private final LinkedHashMap<K, V> entries;

    public LruCache(int capacity) {
        if (capacity <= 0) {
            throw new IllegalArgumentException("capacity must be positive");
        }
        this.capacity = capacity;
        this.entries = new LinkedHashMap<>(capacity, 0.75f, true);
    }

    public synchronized V get(K key) {
        return entries.get(key);
    }

    public synchronized void put(K key, V value) {
        entries.put(key, value);
        if (entries.size() > capacity) {
            K leastRecentlyUsed = entries.keySet().iterator().next();
            entries.remove(leastRecentlyUsed);
        }
    }

    public synchronized Map<K, V> snapshot() {
        return Map.copyOf(entries);
    }

    public static void verify() {
        LruCache<Integer, String> cache = new LruCache<>(2);
        cache.put(1, "one");
        cache.put(2, "two");
        cache.get(1);
        cache.put(3, "three");
        if (cache.get(2) != null || cache.get(1) == null) {
            throw new AssertionError(cache.snapshot());
        }
    }
}
