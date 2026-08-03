public final class LinkedListInterviewChecks {
    static final class Node {
        final int value;
        Node next;

        Node(int value) {
            this.value = value;
        }
    }

    static final class RandomNode {
        final int value;
        RandomNode next;
        RandomNode random;

        RandomNode(int value) {
            this.value = value;
        }
    }

    private LinkedListInterviewChecks() {}

    static Node of(int... values) {
        Node sentinel = new Node(0);
        Node tail = sentinel;
        for (int value : values) {
            tail.next = new Node(value);
            tail = tail.next;
        }
        return sentinel.next;
    }

    static Node reverse(Node head) {
        Node previous = null;
        Node current = head;
        while (current != null) {
            Node next = current.next;
            current.next = previous;
            previous = current;
            current = next;
        }
        return previous;
    }

    static boolean isPalindrome(Node head) {
        if (head == null || head.next == null) {
            return true;
        }
        Node slow = head;
        Node fast = head;
        while (fast.next != null && fast.next.next != null) {
            slow = slow.next;
            fast = fast.next.next;
        }
        Node second = reverse(slow.next);
        boolean equal = true;
        for (Node left = head, right = second; right != null;
             left = left.next, right = right.next) {
            if (left.value != right.value) {
                equal = false;
                break;
            }
        }
        slow.next = reverse(second);
        return equal;
    }

    static Node reverseBetween(Node head, int left, int right) {
        int length = length(head);
        if (left < 1 || right < left || right > length) {
            throw new IllegalArgumentException("range must be one-based and inside the list");
        }
        Node sentinel = new Node(0);
        sentinel.next = head;
        Node before = sentinel;
        for (int position = 1; position < left; position++) {
            before = before.next;
        }
        Node rangeTail = before.next;
        for (int move = 0; move < right - left; move++) {
            Node moved = rangeTail.next;
            rangeTail.next = moved.next;
            moved.next = before.next;
            before.next = moved;
        }
        return sentinel.next;
    }

    /** Reuses nodes; inputs must be disjoint, acyclic, and nondecreasing. */
    static Node mergeSorted(Node first, Node second) {
        Node sentinel = new Node(0);
        Node tail = sentinel;
        while (first != null && second != null) {
            if (first.value <= second.value) {
                tail.next = first;
                first = first.next;
            } else {
                tail.next = second;
                second = second.next;
            }
            tail = tail.next;
        }
        tail.next = first != null ? first : second;
        return sentinel.next;
    }

    static Node removeNthFromEnd(Node head, int n) {
        if (n <= 0) {
            throw new IllegalArgumentException("n must be positive");
        }
        Node sentinel = new Node(0);
        sentinel.next = head;
        Node ahead = sentinel;
        for (int step = 0; step < n; step++) {
            ahead = ahead.next;
            if (ahead == null) {
                throw new IllegalArgumentException("n exceeds list length");
            }
        }
        Node beforeTarget = sentinel;
        while (ahead.next != null) {
            ahead = ahead.next;
            beforeTarget = beforeTarget.next;
        }
        beforeTarget.next = beforeTarget.next.next;
        return sentinel.next;
    }

    static Node cycleEntry(Node head) {
        Node slow = head;
        Node fast = head;
        do {
            if (fast == null || fast.next == null) {
                return null;
            }
            slow = slow.next;
            fast = fast.next.next;
        } while (slow != fast);

        Node seeker = head;
        while (seeker != slow) {
            seeker = seeker.next;
            slow = slow.next;
        }
        return seeker;
    }

    /** Returns the first shared node by identity; inputs must be acyclic. */
    static Node intersection(Node first, Node second) {
        int firstLength = length(first);
        int secondLength = length(second);
        while (firstLength > secondLength) {
            first = first.next;
            firstLength--;
        }
        while (secondLength > firstLength) {
            second = second.next;
            secondLength--;
        }
        while (first != second) {
            first = first.next;
            second = second.next;
        }
        return first;
    }

    /** O(1) auxiliary-node mapping: weave copies, connect randoms, then detach. */
    static RandomNode copyRandomList(RandomNode head) {
        if (head == null) {
            return null;
        }
        for (RandomNode original = head; original != null;) {
            RandomNode nextOriginal = original.next;
            RandomNode copy = new RandomNode(original.value);
            original.next = copy;
            copy.next = nextOriginal;
            original = nextOriginal;
        }
        for (RandomNode original = head; original != null; original = original.next.next) {
            original.next.random = original.random == null ? null : original.random.next;
        }
        RandomNode copyHead = head.next;
        for (RandomNode original = head; original != null;) {
            RandomNode copy = original.next;
            RandomNode nextOriginal = copy.next;
            original.next = nextOriginal;
            copy.next = nextOriginal == null ? null : nextOriginal.next;
            original = nextOriginal;
        }
        return copyHead;
    }

    private static int length(Node head) {
        int length = 0;
        for (Node current = head; current != null; current = current.next) {
            length++;
        }
        return length;
    }

    static String render(Node head) {
        StringBuilder output = new StringBuilder();
        for (Node current = head; current != null; current = current.next) {
            if (!output.isEmpty()) {
                output.append("->");
            }
            output.append(current.value);
        }
        return output.toString();
    }

    private static void check(boolean condition, String message) {
        if (!condition) {
            throw new AssertionError(message);
        }
    }

    private static void expectFailure(Runnable action) {
        try {
            action.run();
        } catch (IllegalArgumentException expected) {
            return;
        }
        throw new AssertionError("expected IllegalArgumentException");
    }

    public static void main(String[] args) {
        check(render(reverse(of(1, 2, 3))).equals("3->2->1"), "reverse");
        Node palindrome = of(1, 2, 2, 1);
        check(isPalindrome(palindrome), "palindrome");
        check(render(palindrome).equals("1->2->2->1"), "restored");
        check(!isPalindrome(of(1, 2)), "not palindrome");

        Node segment = reverseBetween(of(1, 2, 3, 4, 5), 2, 4);
        check(render(segment).equals("1->4->3->2->5"), "reverse sublist");
        check(render(reverseBetween(of(1), 1, 1)).equals("1"), "single-node range");
        expectFailure(() -> reverseBetween(of(1, 2), 1, 3));

        Node leftOne = new Node(1);
        Node leftThree = new Node(3);
        leftOne.next = leftThree;
        Node merged = mergeSorted(leftOne, of(1, 2, 4));
        check(render(merged).equals("1->1->2->3->4"), "merge values");
        check(merged == leftOne, "stable merge takes left equality first");

        check(render(removeNthFromEnd(of(1, 2, 3, 4), 2)).equals("1->2->4"),
                "remove from end");
        check(render(removeNthFromEnd(of(7), 1)).isEmpty(), "remove head");
        expectFailure(() -> removeNthFromEnd(of(1), 2));

        Node cycle = of(1, 2, 3, 4);
        Node entry = cycle.next;
        cycle.next.next.next.next = entry;
        check(cycleEntry(cycle) == entry, "cycle entry by identity");
        check(cycleEntry(of(1, 2)) == null, "acyclic list");

        Node shared = of(8, 9);
        Node first = of(1, 2);
        first.next.next = shared;
        Node second = of(3);
        second.next = shared;
        check(intersection(first, second) == shared, "intersection by identity");
        check(intersection(of(1), of(1)) == null, "equal values are not intersection");

        RandomNode randomFirst = new RandomNode(7);
        RandomNode randomSecond = new RandomNode(13);
        RandomNode randomThird = new RandomNode(11);
        randomFirst.next = randomSecond;
        randomSecond.next = randomThird;
        randomFirst.random = randomThird;
        randomSecond.random = randomFirst;
        randomThird.random = randomThird;
        RandomNode copy = copyRandomList(randomFirst);
        check(copy != randomFirst && copy.value == 7 && copy.next.value == 13,
                "random list deep nodes");
        check(copy.random == copy.next.next && copy.next.random == copy
                && copy.next.next.random == copy.next.next, "random links target copies");
        check(randomFirst.next == randomSecond && randomSecond.next == randomThird,
                "original random list restored");
        check(copyRandomList(null) == null, "copy empty random list");
        System.out.println("PASS 20 linked-list checks");
    }
}
