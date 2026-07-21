public class ListPatterns {
    static class Node {
        int val;
        Node next;
        Node(int v) { this.val = v; }
    }

    public static Node reverse(Node head) {
        Node prev = null, cur = head;
        while (cur != null) {
            Node nxt = cur.next;
            cur.next = prev;
            prev = cur;
            cur = nxt;
        }
        return prev;
    }
}
