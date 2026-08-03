import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;

/**
 * Dependency-free educational implementations for collection interview study.
 * These classes expose invariants; production code should normally use the JDK.
 */
public final class CollectionsImplementationChecks {
    private CollectionsImplementationChecks() {}

    public static void main(String[] args) {
        verifyDynamicArray();
        verifyLinkedList();
        verifyHashMap();
        verifyBinaryHeap();
        ComparatorSortingEdgeHarness.verify();
        System.out.println("PASS 5 low-level collection implementation suites");
    }

    private static void verifyDynamicArray() {
        MiniDynamicArray<String> values = new MiniDynamicArray<>(2);
        values.add("A");
        values.add("C");
        int originalCapacity = values.capacity();
        values.add(1, "B");

        check(values.size() == 3, "dynamic-array size");
        check(values.get(0).equals("A") && values.get(1).equals("B")
                && values.get(2).equals("C"), "dynamic-array insertion");
        check(values.capacity() > originalCapacity, "dynamic-array resize");
        check(values.remove(1).equals("B") && values.get(1).equals("C"),
                "dynamic-array removal");
        expectIndexFailure(() -> values.get(values.size()),
                "dynamic-array upper bound");
    }

    private static void verifyLinkedList() {
        MiniLinkedList<Integer> values = new MiniLinkedList<>();
        values.addFirst(2);
        values.addFirst(1);
        values.addLast(4);
        values.add(2, 3);

        check(values.size() == 4, "linked-list size");
        check(values.get(0) == 1 && values.get(3) == 4,
                "linked-list endpoints");
        check(values.remove(1) == 2 && values.get(1) == 3,
                "linked-list unlink");
        check(values.linksAreConsistent(), "linked-list bidirectional invariant");
    }

    private static void verifyHashMap() {
        MiniHashMap<CollisionKey, String> values = new MiniHashMap<>(2);
        CollisionKey first = new CollisionKey(1);
        CollisionKey second = new CollisionKey(2);
        CollisionKey third = new CollisionKey(3);

        values.put(first, "one");
        int originalCapacity = values.capacity();
        values.put(second, "two");
        values.put(third, "three");

        check(values.size() == 3, "hash-map size after collisions");
        check(values.get(second).equals("two"), "hash-map collision lookup");
        check(values.capacity() > originalCapacity, "hash-map resize");
        check(values.put(second, "TWO").equals("two") && values.size() == 3,
                "hash-map update");
        check(values.remove(first).equals("one") && !values.containsKey(first),
                "hash-map removal");

        MiniHashMap<String, Integer> nullPolicy = new MiniHashMap<>();
        nullPolicy.put(null, 7);
        nullPolicy.put("present-null", null);
        check(nullPolicy.get(null) == 7, "hash-map null key");
        check(nullPolicy.get("present-null") == null
                        && nullPolicy.containsKey("present-null"),
                "hash-map null value ambiguity");
    }

    private static void verifyBinaryHeap() {
        BinaryHeap<Integer> heap = new BinaryHeap<>(Integer::compare);
        for (int value : new int[] {7, 3, 9, 1, 4}) {
            heap.offer(value);
            check(heap.heapInvariantHolds(), "heap invariant after offer");
        }

        List<Integer> order = new ArrayList<>();
        while (!heap.isEmpty()) {
            order.add(heap.poll());
            check(heap.heapInvariantHolds(), "heap invariant after poll");
        }
        check(order.equals(List.of(1, 3, 4, 7, 9)), "heap poll order");
        check(heap.poll() == null, "heap empty poll");
    }

    private static void expectIndexFailure(Runnable operation, String message) {
        try {
            operation.run();
            throw new AssertionError("expected index failure: " + message);
        } catch (IndexOutOfBoundsException expected) {
            // Expected by the check.
        }
    }

    private static void check(boolean condition, String message) {
        if (!condition) {
            throw new AssertionError(message);
        }
    }

    static final class MiniDynamicArray<E> {
        private static final int DEFAULT_CAPACITY = 4;

        private Object[] elements;
        private int size;

        MiniDynamicArray() {
            this(DEFAULT_CAPACITY);
        }

        MiniDynamicArray(int initialCapacity) {
            if (initialCapacity < 0) {
                throw new IllegalArgumentException("negative capacity");
            }
            elements = new Object[Math.max(1, initialCapacity)];
        }

        int size() {
            return size;
        }

        int capacity() {
            return elements.length;
        }

        E get(int index) {
            checkElementIndex(index);
            return elementAt(index);
        }

        E set(int index, E value) {
            checkElementIndex(index);
            E previous = elementAt(index);
            elements[index] = value;
            return previous;
        }

        void add(E value) {
            add(size, value);
        }

        void add(int index, E value) {
            checkPositionIndex(index);
            ensureCapacity(size + 1);
            int moved = size - index;
            if (moved > 0) {
                System.arraycopy(elements, index, elements, index + 1, moved);
            }
            elements[index] = value;
            size++;
        }

        E remove(int index) {
            checkElementIndex(index);
            E previous = elementAt(index);
            int moved = size - index - 1;
            if (moved > 0) {
                System.arraycopy(elements, index + 1, elements, index, moved);
            }
            elements[--size] = null;
            return previous;
        }

        void clear() {
            Arrays.fill(elements, 0, size, null);
            size = 0;
        }

        private void ensureCapacity(int minimum) {
            if (minimum <= elements.length) {
                return;
            }
            int grown = elements.length + Math.max(1, elements.length / 2);
            elements = Arrays.copyOf(elements, Math.max(minimum, grown));
        }

        @SuppressWarnings("unchecked")
        private E elementAt(int index) {
            return (E) elements[index];
        }

        private void checkElementIndex(int index) {
            if (index < 0 || index >= size) {
                throw new IndexOutOfBoundsException(
                        "index=" + index + ", size=" + size);
            }
        }

        private void checkPositionIndex(int index) {
            if (index < 0 || index > size) {
                throw new IndexOutOfBoundsException(
                        "index=" + index + ", size=" + size);
            }
        }
    }

    static final class MiniLinkedList<E> {
        private Node<E> first;
        private Node<E> last;
        private int size;

        int size() {
            return size;
        }

        void addFirst(E value) {
            Node<E> oldFirst = first;
            Node<E> added = new Node<>(null, value, oldFirst);
            first = added;
            if (oldFirst == null) {
                last = added;
            } else {
                oldFirst.previous = added;
            }
            size++;
        }

        void addLast(E value) {
            Node<E> oldLast = last;
            Node<E> added = new Node<>(oldLast, value, null);
            last = added;
            if (oldLast == null) {
                first = added;
            } else {
                oldLast.next = added;
            }
            size++;
        }

        void add(int index, E value) {
            checkPositionIndex(index);
            if (index == size) {
                addLast(value);
                return;
            }
            if (index == 0) {
                addFirst(value);
                return;
            }

            Node<E> successor = nodeAt(index);
            Node<E> predecessor = successor.previous;
            Node<E> added = new Node<>(predecessor, value, successor);
            predecessor.next = added;
            successor.previous = added;
            size++;
        }

        E get(int index) {
            checkElementIndex(index);
            return nodeAt(index).value;
        }

        E remove(int index) {
            checkElementIndex(index);
            return unlink(nodeAt(index));
        }

        void clear() {
            Node<E> current = first;
            while (current != null) {
                Node<E> next = current.next;
                current.previous = null;
                current.next = null;
                current.value = null;
                current = next;
            }
            first = null;
            last = null;
            size = 0;
        }

        boolean linksAreConsistent() {
            if (size == 0) {
                return first == null && last == null;
            }
            if (first == null || last == null
                    || first.previous != null || last.next != null) {
                return false;
            }

            int count = 0;
            Node<E> previous = null;
            for (Node<E> current = first; current != null; current = current.next) {
                if (current.previous != previous) {
                    return false;
                }
                previous = current;
                count++;
            }
            return previous == last && count == size;
        }

        private E unlink(Node<E> node) {
            Node<E> predecessor = node.previous;
            Node<E> successor = node.next;

            if (predecessor == null) {
                first = successor;
            } else {
                predecessor.next = successor;
            }
            if (successor == null) {
                last = predecessor;
            } else {
                successor.previous = predecessor;
            }

            E value = node.value;
            node.value = null;
            node.previous = null;
            node.next = null;
            size--;
            return value;
        }

        private Node<E> nodeAt(int index) {
            if (index < size / 2) {
                Node<E> current = first;
                for (int position = 0; position < index; position++) {
                    current = current.next;
                }
                return current;
            }

            Node<E> current = last;
            for (int position = size - 1; position > index; position--) {
                current = current.previous;
            }
            return current;
        }

        private void checkElementIndex(int index) {
            if (index < 0 || index >= size) {
                throw new IndexOutOfBoundsException(
                        "index=" + index + ", size=" + size);
            }
        }

        private void checkPositionIndex(int index) {
            if (index < 0 || index > size) {
                throw new IndexOutOfBoundsException(
                        "index=" + index + ", size=" + size);
            }
        }

        private static final class Node<E> {
            private Node<E> previous;
            private E value;
            private Node<E> next;

            Node(Node<E> previous, E value, Node<E> next) {
                this.previous = previous;
                this.value = value;
                this.next = next;
            }
        }
    }

    static final class MiniHashMap<K, V> {
        private static final int DEFAULT_CAPACITY = 4;
        private static final int MAXIMUM_CAPACITY = 1 << 30;
        private static final float LOAD_FACTOR = 0.75f;

        private Node<K, V>[] buckets;
        private int size;
        private int resizeThreshold;

        MiniHashMap() {
            this(DEFAULT_CAPACITY);
        }

        MiniHashMap(int requestedCapacity) {
            if (requestedCapacity < 0) {
                throw new IllegalArgumentException("negative capacity");
            }
            int capacity = tableSizeFor(Math.max(2, requestedCapacity));
            buckets = newTable(capacity);
            resizeThreshold = thresholdFor(capacity);
        }

        int size() {
            return size;
        }

        int capacity() {
            return buckets.length;
        }

        boolean containsKey(K key) {
            return findNode(key) != null;
        }

        V get(K key) {
            Node<K, V> node = findNode(key);
            return node == null ? null : node.value;
        }

        V put(K key, V value) {
            int hash = spreadHash(key);
            int index = bucketIndex(hash, buckets.length);
            for (Node<K, V> node = buckets[index]; node != null; node = node.next) {
                if (node.hash == hash && Objects.equals(node.key, key)) {
                    V previous = node.value;
                    node.value = value;
                    return previous;
                }
            }

            if (size + 1 > resizeThreshold) {
                resize();
                index = bucketIndex(hash, buckets.length);
            }
            buckets[index] = new Node<>(hash, key, value, buckets[index]);
            size++;
            return null;
        }

        V remove(K key) {
            int hash = spreadHash(key);
            int index = bucketIndex(hash, buckets.length);
            Node<K, V> previous = null;
            Node<K, V> current = buckets[index];

            while (current != null) {
                if (current.hash == hash && Objects.equals(current.key, key)) {
                    if (previous == null) {
                        buckets[index] = current.next;
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

        private Node<K, V> findNode(K key) {
            int hash = spreadHash(key);
            int index = bucketIndex(hash, buckets.length);
            for (Node<K, V> node = buckets[index]; node != null; node = node.next) {
                if (node.hash == hash && Objects.equals(node.key, key)) {
                    return node;
                }
            }
            return null;
        }

        private void resize() {
            Node<K, V>[] oldBuckets = buckets;
            if (oldBuckets.length >= MAXIMUM_CAPACITY) {
                throw new IllegalStateException("maximum capacity reached");
            }
            buckets = newTable(oldBuckets.length * 2);
            resizeThreshold = thresholdFor(buckets.length);

            for (Node<K, V> head : oldBuckets) {
                Node<K, V> current = head;
                while (current != null) {
                    Node<K, V> next = current.next;
                    int index = bucketIndex(current.hash, buckets.length);
                    current.next = buckets[index];
                    buckets[index] = current;
                    current = next;
                }
            }
        }

        private static int spreadHash(Object key) {
            if (key == null) {
                return 0;
            }
            int hash = key.hashCode();
            return hash ^ (hash >>> 16);
        }

        private static int bucketIndex(int hash, int length) {
            return hash & (length - 1);
        }

        private static int tableSizeFor(int requestedCapacity) {
            if (requestedCapacity > MAXIMUM_CAPACITY) {
                throw new IllegalArgumentException("capacity too large");
            }
            int highest = Integer.highestOneBit(requestedCapacity - 1);
            return highest >= MAXIMUM_CAPACITY / 2
                    ? MAXIMUM_CAPACITY
                    : highest << 1;
        }

        private static int thresholdFor(int capacity) {
            return Math.max(1, (int) (capacity * LOAD_FACTOR));
        }

        @SuppressWarnings("unchecked")
        private static <K, V> Node<K, V>[] newTable(int capacity) {
            return (Node<K, V>[]) new Node<?, ?>[capacity];
        }

        private static final class Node<K, V> {
            private final int hash;
            private final K key;
            private V value;
            private Node<K, V> next;

            Node(int hash, K key, V value, Node<K, V> next) {
                this.hash = hash;
                this.key = key;
                this.value = value;
                this.next = next;
            }
        }
    }

    static final class BinaryHeap<E> {
        private static final int DEFAULT_CAPACITY = 4;

        private final Comparator<? super E> comparator;
        private Object[] elements = new Object[DEFAULT_CAPACITY];
        private int size;

        BinaryHeap(Comparator<? super E> comparator) {
            this.comparator = Objects.requireNonNull(comparator, "comparator");
        }

        int size() {
            return size;
        }

        boolean isEmpty() {
            return size == 0;
        }

        void offer(E value) {
            Objects.requireNonNull(value, "value");
            ensureCapacity(size + 1);
            elements[size] = value;
            siftUp(size);
            size++;
        }

        E peek() {
            return size == 0 ? null : elementAt(0);
        }

        E poll() {
            if (size == 0) {
                return null;
            }
            E result = elementAt(0);
            E last = elementAt(size - 1);
            elements[size - 1] = null;
            size--;
            if (size > 0) {
                elements[0] = last;
                siftDown(0);
            }
            return result;
        }

        boolean heapInvariantHolds() {
            for (int child = 1; child < size; child++) {
                int parent = (child - 1) / 2;
                if (comparator.compare(elementAt(parent), elementAt(child)) > 0) {
                    return false;
                }
            }
            return true;
        }

        private void siftUp(int child) {
            E value = elementAt(child);
            while (child > 0) {
                int parent = (child - 1) / 2;
                E parentValue = elementAt(parent);
                if (comparator.compare(value, parentValue) >= 0) {
                    break;
                }
                elements[child] = parentValue;
                child = parent;
            }
            elements[child] = value;
        }

        private void siftDown(int parent) {
            E value = elementAt(parent);
            int firstLeaf = size / 2;
            while (parent < firstLeaf) {
                int left = 2 * parent + 1;
                int right = left + 1;
                int smallerChild = left;
                if (right < size
                        && comparator.compare(elementAt(right), elementAt(left)) < 0) {
                    smallerChild = right;
                }
                E childValue = elementAt(smallerChild);
                if (comparator.compare(value, childValue) <= 0) {
                    break;
                }
                elements[parent] = childValue;
                parent = smallerChild;
            }
            elements[parent] = value;
        }

        private void ensureCapacity(int minimum) {
            if (minimum > elements.length) {
                elements = Arrays.copyOf(elements, elements.length * 2);
            }
        }

        @SuppressWarnings("unchecked")
        private E elementAt(int index) {
            return (E) elements[index];
        }
    }

    static final class ComparatorSortingEdgeHarness {
        private ComparatorSortingEdgeHarness() {}

        static void verify() {
            Comparator<Integer> unsafe = (left, right) -> left - right;
            check(unsafe.compare(Integer.MIN_VALUE, Integer.MAX_VALUE) > 0,
                    "subtraction comparator overflow demonstration");

            List<Integer> extremes = new ArrayList<>(
                    List.of(Integer.MAX_VALUE, 0, Integer.MIN_VALUE));
            extremes.sort(Integer::compare);
            check(extremes.equals(List.of(Integer.MIN_VALUE, 0, Integer.MAX_VALUE)),
                    "safe integer comparator");

            List<Candidate> candidates = new ArrayList<>(List.of(
                    new Candidate("Lin", 90),
                    new Candidate("Ada", 90),
                    new Candidate("Grace", 80)));
            candidates.sort(Comparator.comparingInt(Candidate::score)
                    .reversed()
                    .thenComparing(Candidate::name));
            check(candidates.equals(List.of(
                            new Candidate("Ada", 90),
                            new Candidate("Lin", 90),
                            new Candidate("Grace", 80))),
                    "deterministic comparator tie-break");
        }

        record Candidate(String name, int score) {}
    }

    record CollisionKey(int value) {
        @Override
        public int hashCode() {
            return 7;
        }
    }
}
