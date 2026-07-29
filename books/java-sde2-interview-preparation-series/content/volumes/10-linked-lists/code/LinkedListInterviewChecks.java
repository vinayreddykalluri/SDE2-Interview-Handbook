public final class LinkedListInterviewChecks {
    static final class Node {
        final int value;
        Node next;

        Node(int value) {
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

    public static void main(String[] args) {
        check(render(reverse(of(1, 2, 3))).equals("3->2->1"), "reverse");
        Node palindrome = of(1, 2, 2, 1);
        check(isPalindrome(palindrome), "palindrome");
        check(render(palindrome).equals("1->2->2->1"), "restored");
        check(!isPalindrome(of(1, 2)), "not palindrome");
        System.out.println("PASS 4 linked-list checks");
    }
}
