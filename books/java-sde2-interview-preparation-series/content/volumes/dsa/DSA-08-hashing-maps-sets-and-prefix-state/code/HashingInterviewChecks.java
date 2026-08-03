import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

public final class HashingInterviewChecks {
    private HashingInterviewChecks() {}

    /**
     * A deliberately small separate-chaining map for learning hash-table mechanics.
     * Production interview solutions should normally use {@link HashMap}.
     */
    static final class EducationalChainedHashMap<K, V> {
        private static final int MINIMUM_CAPACITY = 4;
        private static final double LOAD_FACTOR = 0.75;

        private Node<K, V>[] buckets;
        private int size;
        private int resizeThreshold;

        @SuppressWarnings("unchecked")
        EducationalChainedHashMap(int requestedCapacity) {
            if (requestedCapacity < 0) {
                throw new IllegalArgumentException("capacity cannot be negative");
            }
            int capacity = MINIMUM_CAPACITY;
            while (capacity < requestedCapacity) {
                if (capacity > 1 << 29) {
                    throw new IllegalArgumentException("capacity is too large");
                }
                capacity <<= 1;
            }
            buckets = (Node<K, V>[]) new Node<?, ?>[capacity];
            resizeThreshold = thresholdFor(capacity);
        }

        int size() {
            return size;
        }

        int capacity() {
            return buckets.length;
        }

        V get(K key) {
            Node<K, V> node = findNode(key);
            return node == null ? null : node.value;
        }

        boolean containsKey(K key) {
            return findNode(key) != null;
        }

        V put(K key, V value) {
            int bucket = bucketIndex(key, buckets.length);
            for (Node<K, V> node = buckets[bucket]; node != null; node = node.next) {
                if (Objects.equals(node.key, key)) {
                    V previous = node.value;
                    node.value = value;
                    return previous;
                }
            }
            buckets[bucket] = new Node<>(key, value, buckets[bucket]);
            size++;
            if (size > resizeThreshold) {
                resize();
            }
            return null;
        }

        V remove(K key) {
            int bucket = bucketIndex(key, buckets.length);
            Node<K, V> previous = null;
            Node<K, V> current = buckets[bucket];
            while (current != null) {
                if (Objects.equals(current.key, key)) {
                    if (previous == null) {
                        buckets[bucket] = current.next;
                    } else {
                        previous.next = current.next;
                    }
                    size--;
                    return current.value;
                }
                previous = current;
                current = current.next;
            }
            return null;
        }

        int maximumChainLength() {
            int maximum = 0;
            for (Node<K, V> head : buckets) {
                int length = 0;
                for (Node<K, V> node = head; node != null; node = node.next) {
                    length++;
                }
                maximum = Math.max(maximum, length);
            }
            return maximum;
        }

        private Node<K, V> findNode(K key) {
            int bucket = bucketIndex(key, buckets.length);
            for (Node<K, V> node = buckets[bucket]; node != null; node = node.next) {
                if (Objects.equals(node.key, key)) {
                    return node;
                }
            }
            return null;
        }

        @SuppressWarnings("unchecked")
        private void resize() {
            if (buckets.length >= 1 << 30) {
                resizeThreshold = Integer.MAX_VALUE;
                return;
            }
            Node<K, V>[] previous = buckets;
            buckets = (Node<K, V>[]) new Node<?, ?>[previous.length << 1];
            for (Node<K, V> head : previous) {
                Node<K, V> node = head;
                while (node != null) {
                    Node<K, V> next = node.next;
                    int bucket = bucketIndex(node.key, buckets.length);
                    node.next = buckets[bucket];
                    buckets[bucket] = node;
                    node = next;
                }
            }
            resizeThreshold = thresholdFor(buckets.length);
        }

        private static int thresholdFor(int capacity) {
            return (int) (capacity * LOAD_FACTOR);
        }

        private static int bucketIndex(Object key, int capacity) {
            int hash = Objects.hashCode(key);
            int spread = hash ^ (hash >>> 16);
            return spread & (capacity - 1);
        }

        private static final class Node<K, V> {
            private final K key;
            private V value;
            private Node<K, V> next;

            private Node(K key, V value, Node<K, V> next) {
                this.key = key;
                this.value = value;
                this.next = next;
            }
        }
    }

    record CollisionKey(int id) {
        @Override
        public int hashCode() {
            return 7;
        }
    }

    static final class MutableKey {
        private int id;

        MutableKey(int id) {
            this.id = id;
        }

        void setId(int id) {
            this.id = id;
        }

        @Override
        public boolean equals(Object other) {
            return other instanceof MutableKey key && id == key.id;
        }

        @Override
        public int hashCode() {
            return Integer.hashCode(id);
        }
    }

    static int[] twoSum(int[] values, int target) {
        Map<Integer, Integer> indexByValue = new HashMap<>();
        for (int i = 0; i < values.length; i++) {
            Integer partner = indexByValue.get(target - values[i]);
            if (partner != null) {
                return new int[] {partner, i};
            }
            indexByValue.put(values[i], i);
        }
        return new int[0];
    }

    static long countSubarrays(int[] values, long target) {
        Map<Long, Long> prefixFrequency = new HashMap<>();
        prefixFrequency.put(0L, 1L);
        long prefix = 0L;
        long answer = 0L;
        for (int value : values) {
            prefix += value;
            answer += prefixFrequency.getOrDefault(prefix - target, 0L);
            prefixFrequency.merge(prefix, 1L, Long::sum);
        }
        return answer;
    }

    static int longestBalancedBinarySubarray(int[] bits) {
        Map<Integer, Integer> earliest = new HashMap<>();
        earliest.put(0, -1);
        int prefix = 0;
        int best = 0;
        for (int i = 0; i < bits.length; i++) {
            if (bits[i] != 0 && bits[i] != 1) {
                throw new IllegalArgumentException("expected binary values");
            }
            prefix += bits[i] == 0 ? -1 : 1;
            Integer first = earliest.putIfAbsent(prefix, i);
            if (first != null) {
                best = Math.max(best, i - first);
            }
        }
        return best;
    }

    static int longestConsecutive(int[] values) {
        Set<Integer> present = new HashSet<>();
        for (int value : values) {
            present.add(value);
        }
        int best = 0;
        for (int value : present) {
            if (value != Integer.MIN_VALUE && present.contains(value - 1)) {
                continue;
            }
            int current = value;
            int length = 1;
            while (current != Integer.MAX_VALUE && present.contains(current + 1)) {
                current++;
                length++;
            }
            best = Math.max(best, length);
        }
        return best;
    }

    private static void check(boolean condition, String message) {
        if (!condition) {
            throw new AssertionError(message);
        }
    }

    public static void main(String[] args) {
        int[] indexes = twoSum(new int[] {3, 3}, 6);
        check(indexes.length == 2 && indexes[0] == 0 && indexes[1] == 1, "two sum");
        check(countSubarrays(new int[] {1, -1, 1}, 1L) == 3L, "prefix count");
        check(longestBalancedBinarySubarray(new int[] {0, 1, 0, 1, 1}) == 4, "balanced");
        check(longestConsecutive(new int[] {100, 4, 200, 1, 3, 2}) == 4, "consecutive");
        check(longestConsecutive(new int[] {Integer.MAX_VALUE, Integer.MIN_VALUE}) == 1, "overflow guard");

        EducationalChainedHashMap<CollisionKey, String> collisionMap =
                new EducationalChainedHashMap<>(4);
        collisionMap.put(new CollisionKey(1), "one");
        collisionMap.put(new CollisionKey(2), "two");
        collisionMap.put(new CollisionKey(3), "three");
        check(collisionMap.maximumChainLength() == 3, "collisions share a chain");
        check("two".equals(collisionMap.get(new CollisionKey(2))), "collision lookup");
        check("two".equals(collisionMap.put(new CollisionKey(2), "TWO")), "replace value");
        check(collisionMap.size() == 3, "replace does not grow size");
        check("TWO".equals(collisionMap.remove(new CollisionKey(2))), "remove middle node");
        check(!collisionMap.containsKey(new CollisionKey(2)), "removed key is absent");

        EducationalChainedHashMap<Integer, Integer> growingMap =
                new EducationalChainedHashMap<>(1);
        int originalCapacity = growingMap.capacity();
        for (int value = 0; value < 100; value++) {
            growingMap.put(value, value * value);
        }
        check(growingMap.capacity() > originalCapacity, "load factor triggers resizing");
        check(growingMap.size() == 100 && growingMap.get(99) == 9_801,
                "rehashing preserves entries");
        growingMap.put(null, 42);
        check(growingMap.containsKey(null) && growingMap.get(null) == 42, "null key policy");

        EducationalChainedHashMap<MutableKey, String> dangerousMap =
                new EducationalChainedHashMap<>(4);
        MutableKey mutable = new MutableKey(1);
        dangerousMap.put(mutable, "stored-before-mutation");
        mutable.setId(2);
        check(dangerousMap.get(mutable) == null, "mutated hash key becomes unreachable");
        check(dangerousMap.size() == 1, "unreachable entry still occupies the table");

        System.out.println("PASS 16 hashing checks");
    }
}
