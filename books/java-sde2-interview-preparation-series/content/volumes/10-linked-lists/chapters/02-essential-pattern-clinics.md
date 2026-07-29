# Essential Linked-List Pattern Clinics

Singly linked lists teach forward reachability. Doubly linked lists add constant-time unlinking when the node is already known, which is the structural reason they appear in LRU caches and intrusive schedulers.

## Clinic 1: doubly linked invariants

Every live interior node must satisfy both directions:

```text
node.previous.next == node
node.next.previous == node
```

Two sentinels remove empty, first, and last special cases. `front.next` is the most-recent real node and `back.previous` is the least-recent real node. The sentinels are structure, not user data.

Unlinking a known node is four reference reads/writes and does not traverse the list:

```text
node.previous.next = node.next
node.next.previous = node.previous
```

Clear detached links when doing so improves misuse detection and garbage-retention behavior. Do not clear them before reconnecting neighbors.

## Clinic 2: LRU cache as two synchronized representations

An LRU cache combines:

- a hash map from key to node for expected constant-time lookup;
- a doubly linked list ordered from most recent to least recent;
- a capacity invariant.

The map and list are not independent. At every public-method boundary:

1. each map entry points to exactly one real list node;
2. each real list node appears in the map under its key;
3. no key appears twice;
4. the list order matches recency;
5. size is at most capacity.

`get` is a mutation because it moves the node to the front. `put` updates and moves an existing node or inserts a new node; if capacity is exceeded, it removes `back.previous` from both structures.

### SDE-2 boundaries

The simple implementation below is not thread-safe. Adding a concurrent map alone would not make it safe because map and list updates form one compound invariant. Use confinement or a lock around the complete operation. Production caches also need decisions for weight-based capacity, expiry, load failure, statistics, and whether null values are allowed.

## Runnable Java 21 clinic

```java
import java.util.HashMap;
import java.util.Map;

public final class LinkedListCoverageClinic {
    private LinkedListCoverageClinic() {
    }

    public static final class LruCache {
        private static final class Node {
            private final int key;
            private int value;
            private Node previous;
            private Node next;

            private Node(int key, int value) {
                this.key = key;
                this.value = value;
            }
        }

        private final int capacity;
        private final Map<Integer, Node> byKey = new HashMap<>();
        private final Node front = new Node(0, 0);
        private final Node back = new Node(0, 0);

        public LruCache(int capacity) {
            if (capacity <= 0) {
                throw new IllegalArgumentException("capacity must be positive");
            }
            this.capacity = capacity;
            front.next = back;
            back.previous = front;
        }

        public int getOrDefault(int key, int defaultValue) {
            Node node = byKey.get(key);
            if (node == null) {
                return defaultValue;
            }
            moveToFront(node);
            return node.value;
        }

        public void put(int key, int value) {
            Node existing = byKey.get(key);
            if (existing != null) {
                existing.value = value;
                moveToFront(existing);
                return;
            }

            Node inserted = new Node(key, value);
            byKey.put(key, inserted);
            addAfterFront(inserted);
            if (byKey.size() > capacity) {
                Node evicted = back.previous;
                detach(evicted);
                byKey.remove(evicted.key);
            }
        }

        public int size() {
            return byKey.size();
        }

        private void moveToFront(Node node) {
            detach(node);
            addAfterFront(node);
        }

        private void addAfterFront(Node node) {
            node.previous = front;
            node.next = front.next;
            front.next.previous = node;
            front.next = node;
        }

        private static void detach(Node node) {
            node.previous.next = node.next;
            node.next.previous = node.previous;
            node.previous = null;
            node.next = null;
        }
    }

    public static void main(String[] args) {
        LruCache cache = new LruCache(2);
        cache.put(1, 10);
        cache.put(2, 20);
        assert cache.getOrDefault(1, -1) == 10;
        cache.put(3, 30);
        assert cache.getOrDefault(2, -1) == -1;
        assert cache.getOrDefault(1, -1) == 10;
        assert cache.getOrDefault(3, -1) == 30;
        assert cache.size() == 2;
        System.out.println("PASS essential linked-list clinics");
    }
}
```

Expected output with assertions enabled:

```text
PASS essential linked-list clinics
```

## Interviewer follow-up chain with model answers

**Interviewer:** Why is a singly linked list insufficient for a conventional O(1) LRU update?

**Candidate:** The map can find a node, but a singly linked node does not know its predecessor. Removing that known interior node would still require a traversal or an additional predecessor mapping. A doubly linked node can unlink itself directly.

**Interviewer:** Could `LinkedHashMap` replace this code?

**Candidate:** Yes for many in-process caches. Access-order `LinkedHashMap` plus `removeEldestEntry` expresses the same policy with less custom mutation code. I would still define concurrency, capacity, expiry, and loading contracts explicitly.
