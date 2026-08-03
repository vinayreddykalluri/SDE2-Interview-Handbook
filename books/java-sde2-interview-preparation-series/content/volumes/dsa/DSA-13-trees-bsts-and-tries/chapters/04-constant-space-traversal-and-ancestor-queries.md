# 4. Constant-Space Traversal and Repeated Ancestor Queries

## Why this chapter exists

The tree material so far solves each problem once. This chapter covers the two follow-ups that arrive when an interviewer pushes on *resources* rather than correctness:

- **"Can you do that traversal in O(1) extra space?"** Recursion costs O(h) stack; an explicit stack costs O(h) heap. Morris traversal costs neither, and the trick that makes it work is worth understanding.
- **"Now answer a million LCA queries."** The postorder LCA from chapter 1 is O(n) per query, so a million queries on a million-node tree is 10^12 operations. Binary lifting preprocesses once and answers each query in O(log n).

Both are recognizably "second-half of the interview" questions. Neither is exotic, and each has a specific condition under which it is the wrong choice - which is the part worth being able to say.

## Part 1: Morris traversal

### The idea

An inorder traversal must, after finishing a left subtree, return to the parent. Recursion and an explicit stack both *remember* the parent. Morris traversal instead **temporarily writes the way back into the tree itself**, using the null right pointers that leaf nodes already have.

For any node with a left child, its inorder predecessor is the rightmost node of that left subtree - and that node's right pointer is null. Point it at the current node. Now, after the left subtree is exhausted, following right pointers naturally arrives back at the current node. That temporary pointer is called a **thread**.

```text
        4
       / \
      2   6
     / \
    1   3

Processing 4: predecessor is 3 (rightmost of the left subtree).
Thread 3.right -> 4.

        4
       / \
      2   6
     / \
    1   3 ....thread back to 4

Descend left. After 1, 2, 3 are visited, following 3.right returns to 4.
The thread is then removed and the tree is restored.
```

### Implementation

```java
static List<Integer> morrisInorder(TreeNode root) {
    List<Integer> out = new ArrayList<>();
    TreeNode current = root;

    while (current != null) {
        if (current.left == null) {
            out.add(current.val);          // no left subtree: visit and go right
            current = current.right;
            continue;
        }

        // Find the inorder predecessor: rightmost node of the left subtree.
        // Stop at `current` too, because a thread may already point back.
        TreeNode predecessor = current.left;
        while (predecessor.right != null && predecessor.right != current) {
            predecessor = predecessor.right;
        }

        if (predecessor.right == null) {
            predecessor.right = current;   // first visit: thread and descend
            current = current.left;
        } else {
            predecessor.right = null;      // second visit: unthread and visit
            out.add(current.val);
            current = current.right;
        }
    }
    return out;
}
```

Each node is reached at most twice, and each edge is walked at most a constant number of times, so the traversal remains **O(n) time with O(1) extra space** - the output list aside.

The `predecessor.right != current` condition in the inner loop is what makes the second visit detectable. Without it, the search for the predecessor follows the thread it created and loops forever. That single condition is the most commonly omitted line.

### Preorder is one line different

Move the visit to the moment the thread is created rather than removed:

```java
if (predecessor.right == null) {
    out.add(current.val);       // preorder: visit on the way down
    predecessor.right = current;
    current = current.left;
} else {
    predecessor.right = null;   // inorder would visit here instead
    current = current.right;
}
```

Postorder with Morris is possible but requires reversing the right-edge chain on each retreat. It is significantly more code, rarely asked, and reasonable to name without implementing.

### When Morris is the wrong answer

This is the part that separates understanding from recitation.

**It mutates the tree during traversal.** The structure is restored by the end, but at any intermediate moment the tree contains threads. That means:

- **It is not safe under concurrent readers.** Another thread traversing simultaneously will follow a thread pointer and see a corrupted structure. An explicit stack is read-only and safe.
- **It requires mutable nodes.** If `TreeNode.right` is final, or the tree is a shared immutable structure, Morris is simply unavailable.
- **An exception mid-traversal leaves the tree threaded.** There is no natural try/finally repair, because the repair state is spread across the tree.

So the honest answer to "can you traverse in O(1) space" is: yes, with Morris, and here is the trade - the tree is temporarily modified, so it is unsuitable for shared or concurrently-read structures. That framing is what an interviewer is listening for. In most production code, the O(h) stack is a price worth paying for a read-only traversal.

## Part 2: Binary lifting for repeated LCA

### Why the O(n) LCA is not enough

The recursive postorder LCA visits every node, so it is O(n) per query. That is optimal for a single query. For `q` queries it is O(n*q), and interviewers escalate to exactly this point.

Binary lifting preprocesses the tree once in O(n log n) and answers each query in O(log n). The idea rests on one observation: **every integer is a sum of powers of two**, so any upward jump of `k` levels can be made in at most log(k) jumps of power-of-two size.

### The ancestor table

`up[k][v]` is the 2^k-th ancestor of `v`, or a sentinel when that ancestor does not exist.

```text
up[0][v] = parent(v)
up[k][v] = up[k-1][ up[k-1][v] ]     jump halfway, then halfway again
```

That recurrence is the whole technique. Building the table is a DFS to record parents and depths, then a doubling loop.

```java
public final class LcaBinaryLifting {
    private static final int NONE = -1;

    private final int[][] up;      // up[k][v] = 2^k-th ancestor of v
    private final int[] depth;
    private final int levels;

    public LcaBinaryLifting(List<List<Integer>> children, int root) {
        int n = children.size();
        levels = Math.max(1, 32 - Integer.numberOfLeadingZeros(Math.max(1, n)));
        up = new int[levels][n];
        depth = new int[n];
        for (int[] row : up) {
            Arrays.fill(row, NONE);
        }

        // Iterative DFS: recursion would overflow on a degenerate chain.
        Deque<int[]> stack = new ArrayDeque<>();
        stack.push(new int[]{root, NONE, 0});
        while (!stack.isEmpty()) {
            int[] frame = stack.pop();
            int node = frame[0];
            up[0][node] = frame[1];
            depth[node] = frame[2];
            for (int child : children.get(node)) {
                stack.push(new int[]{child, node, frame[2] + 1});
            }
        }

        for (int k = 1; k < levels; k++) {
            for (int v = 0; v < n; v++) {
                int middle = up[k - 1][v];
                up[k][v] = middle == NONE ? NONE : up[k - 1][middle];
            }
        }
    }

    /** Lowest common ancestor of a and b in O(log n). */
    public int lca(int a, int b) {
        if (depth[a] < depth[b]) {
            int swap = a;
            a = b;
            b = swap;                       // a is now the deeper node
        }

        int difference = depth[a] - depth[b];
        for (int k = 0; k < levels; k++) {  // lift a to b's depth
            if ((difference >> k & 1) == 1) {
                a = up[k][a];
            }
        }
        if (a == b) {
            return a;                       // b was an ancestor of a
        }

        // Descend from the largest jump: move both up while they stay distinct.
        for (int k = levels - 1; k >= 0; k--) {
            if (up[k][a] != up[k][b]) {
                a = up[k][a];
                b = up[k][b];
            }
        }
        return up[0][a];                    // parents are now equal
    }

    /** Distance in edges, a direct application of the LCA. */
    public int distance(int a, int b) {
        return depth[a] + depth[b] - 2 * depth[lca(a, b)];
    }
}
```

Three details carry the correctness:

**The equal-depth check before the second loop.** If `b` is an ancestor of `a`, lifting `a` to `b`'s depth lands exactly on `b`. The second loop would then never move and return `up[0][b]`, which is one level too high. Returning early is not an optimization; it is required.

**The second loop descends from the largest `k`.** It moves both nodes up only while their `2^k`-th ancestors *differ*, which keeps them strictly below the LCA. After the loop they are children of it, so `up[0][a]` is the answer. Ascending `k` order does not work: a small jump taken first can overshoot past the LCA and there is no way back.

**The DFS is iterative.** A recursive build overflows on a degenerate chain - exactly the shape a hostile test uses - and this is a preprocessing step, so the depth is the tree's full height.

### Costs, and when not to bother

| Approach | Preprocess | Per query | Extra space |
|---|---|---|---|
| Recursive postorder | none | O(n) | O(h) stack |
| Binary lifting | O(n log n) | O(log n) | O(n log n) |
| Euler tour + sparse table | O(n log n) | **O(1)** | O(n log n) |
| Tarjan offline (Union-Find) | O(n + q) total | amortized | O(n) |

**Do not preprocess for a single query.** With one LCA to answer, the recursive version wins on every axis - simpler, less memory, no build cost. Binary lifting pays for itself once `q` is comparable to `log n` and clearly once it is large.

If all queries are known in advance, Tarjan's offline algorithm with Union-Find is O(n + q) overall and beats both - worth naming as the answer to "what if I gave you every query up front", and it connects directly to the Union-Find material in the graphs volume.

Binary lifting also generalizes past LCA: the same table answers "the k-th ancestor of v" in O(log k), and with an extra table storing aggregates it answers max, min, or sum along the path between two nodes in O(log n).

## Edge cases and common mistakes

- Omitting `predecessor.right != current` in Morris, so the predecessor search follows its own thread and loops forever.
- Using Morris on a tree that other threads read concurrently, or on immutable nodes.
- Assuming an exception during Morris leaves the tree intact; threads persist.
- Placing the Morris visit in the wrong branch, silently producing preorder instead of inorder.
- Claiming Morris is O(1) space while returning an O(n) result list; the bound covers auxiliary space only.
- Omitting the `a == b` early return in binary lifting, returning the LCA's parent when one node is an ancestor of the other.
- Iterating `k` upward in the descent loop, overshooting past the LCA.
- Building the ancestor table with recursion and overflowing on a degenerate chain.
- Sizing `levels` too small; it must satisfy `2^levels > n`.
- Forgetting to propagate the NONE sentinel, so a missing ancestor indexes `up[k-1][-1]`.
- Preprocessing for a single query, where the O(n) recursion is strictly better.
- Reaching for binary lifting when every query is known up front and Tarjan's offline method is O(n + q).

## Interview questions and model answers

**Traverse a binary tree inorder in O(1) extra space.**

Morris traversal. For each node with a left child, find its inorder predecessor - the rightmost node of the left subtree - and point that node's null right pointer back at the current node. Descend left; when the thread is followed back, remove it and visit. Each node is reached at most twice, so it is O(n) time and O(1) auxiliary space.

**What is the catch with Morris?**

It mutates the tree while running. The structure is restored by the end, but intermediate states contain threads, so it is unsafe if another thread reads concurrently, impossible if nodes are immutable, and leaves the tree corrupted if an exception escapes mid-traversal. In most production code an O(h) stack is worth paying for a read-only traversal.

**Your postorder LCA is O(n). I need a million queries.**

Binary lifting. Preprocess `up[k][v]`, the 2^k-th ancestor, with `up[k][v] = up[k-1][up[k-1][v]]`, in O(n log n). Per query, lift the deeper node to equal depth using the binary representation of the depth difference, return early if they now coincide, then move both up from the largest jump while their ancestors differ. Answer is the common parent. O(log n) per query.

**Why does the descent loop go from the largest k down?**

Because it moves both nodes only while their `2^k`-th ancestors differ, which guarantees they stay strictly below the LCA. Starting from small jumps could take a step that lands at or above the LCA, and there is no mechanism to undo it.

**Why the early return when the nodes coincide after lifting?**

That case means `b` was an ancestor of `a`. The descent loop would find all ancestors equal, never move, and return `up[0][b]` - the parent of the true answer. The early return is required for correctness, not speed.

**What if I gave you all the queries in advance?**

Tarjan's offline LCA with Union-Find, which is O(n + q) overall and beats binary lifting's O(n log n + q log n). It works by a single DFS that unions each finished subtree into its parent and answers queries when the second endpoint is reached.

## Exercises

1. **Foundation:** Trace Morris inorder on a three-node tree, drawing the thread at each step.
2. **Foundation:** Remove `predecessor.right != current` and describe exactly why the traversal never ends.
3. **Interview Core:** Implement Morris inorder and preorder, and verify both against recursive traversals over a thousand random trees.
4. **Interview Core:** Assert the tree is structurally unchanged after Morris completes, then throw from inside the loop and assert it is *not*.
5. **Interview Core:** Build the binary-lifting table and answer LCA queries. Verify against the recursive postorder version on random trees.
6. **Interview Core:** Remove the `a == b` early return and find the input where it returns the wrong node.
7. **Interview Core:** Reverse the descent loop to ascending `k` and construct the tree where it overshoots.
8. **SDE-2 Follow-up:** Measure the query count at which binary lifting overtakes the recursive LCA on a 10^5-node tree.
9. **SDE-2 Follow-up:** Extend the table to answer "maximum edge weight on the path between a and b" in O(log n).
10. **Challenge:** Implement Tarjan's offline LCA with Union-Find and compare against binary lifting for 10^6 known queries.

## Chapter summary

Both techniques answer resource follow-ups rather than correctness ones. Morris traversal achieves O(1) auxiliary space by writing the return path into the tree's own null right pointers, threading each node to its inorder predecessor and removing the thread on the way back; the predecessor search must stop at the current node or it follows its own thread forever. Its cost is that the tree is mutated during the walk, which rules it out for concurrently-read or immutable structures and leaves damage if an exception escapes - so the complete answer names the trade rather than just the space bound. Binary lifting turns O(n)-per-query LCA into O(log n) by tabulating 2^k-th ancestors through the doubling recurrence, requiring an early return when one node is an ancestor of the other and a descent from the largest jump downward so the pair never overshoots. It is not worth building for a single query, and if every query is known in advance Tarjan's offline method with Union-Find is better still.

## Revision checklist

- [ ] I can explain what a Morris thread is and which pointer it reuses.
- [ ] I know why the predecessor search must stop at the current node.
- [ ] I can convert Morris inorder to preorder by moving one line.
- [ ] I can state the three situations where Morris is unusable.
- [ ] I can write the binary-lifting recurrence from memory.
- [ ] I know why the early equal-node return is required, not an optimization.
- [ ] I can explain why the descent loop runs from the largest k downward.
- [ ] I build the ancestor table iteratively and can say why.
- [ ] I can compute the query count at which preprocessing pays for itself.
- [ ] I know Tarjan's offline LCA is better when all queries are known up front.
