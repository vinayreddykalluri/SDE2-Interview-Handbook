# 4. Skip Lists and Probabilistic Balance

## Why this chapter exists

Everything so far treats a linked list as a sequential structure: O(1) insertion given a node, O(n) to find one. That O(n) search is the linked list's defining weakness, and there is a standard interview escalation built on it - **"how would you get O(log n) search on a linked structure?"**

The answer is a skip list, and it is worth knowing for three reasons. It is the cleanest example of trading memory for search time on a list. It reaches balanced-tree performance without any rotation logic, which makes it far simpler to implement correctly than an AVL or red-black tree. And it is used in production: Redis sorted sets and Java's `ConcurrentSkipListMap` are both skip lists, the latter because the structure lends itself to lock-free concurrent updates in a way balanced trees do not.

## The idea: express lanes

Binary search needs random access, which a linked list cannot provide. A skip list gets the same effect by adding **layers of shortcuts** over the base list.

```text
level 3   1 ----------------------------------> 9
level 2   1 ------------> 5 ------------------> 9
level 1   1 ------> 3 --> 5 --------> 7 ------> 9
level 0   1 -> 2 -> 3 -> 4 -> 5 -> 6 -> 7 -> 8 -> 9
```

Level 0 is the complete sorted list. Each higher level contains a subset, acting as an express lane. Searching descends:

**Search rule.** Start at the top-left. Move right while the next node's key is less than the target. When it is not, drop down one level. Repeat until level 0.

Searching for 7: at level 3, next is 9, too big - drop. At level 2, move to 5, next is 9, too big - drop. At level 1, move to 7. Found. Four steps instead of seven.

If every second node were promoted, every fourth, and so on, this would be exactly binary search and the height would be log n. Maintaining that perfect distribution under insertion and deletion is expensive - it is the same problem balanced trees solve with rotations.

**Skip lists do not maintain it. They randomize it.**

## Randomized promotion

When inserting, flip a coin. On heads, promote the node one level and flip again. Stop on tails.

```java
private int randomLevel() {
    int level = 1;
    while (level < MAX_LEVEL && random.nextDouble() < 0.5) {
        level++;
    }
    return level;
}
```

A node reaches level `k` with probability `2^-(k-1)`, so on average half the nodes appear at level 1, a quarter at level 2, and so on. That is the *expected* distribution binary search wants, achieved without ever rebalancing.

**Expected O(log n) search, with no worst-case guarantee.** A pathological run of coin flips could promote nothing and leave a plain linked list. The probability is vanishingly small - the chance that no node among a million reaches level 10 is astronomically low - but it is not zero, and saying so precisely is the interview signal. Compare:

| Structure | Search | Guarantee | Rebalancing |
|---|---|---|---|
| Sorted linked list | O(n) | worst case | none |
| Skip list | O(log n) | **expected** | none - randomized |
| AVL / red-black tree | O(log n) | worst case | rotations |
| Sorted array | O(log n) | worst case | O(n) insertion |

**The trade is guarantee for simplicity.** A skip list has no rotation code, no colour invariants, no rebalancing cases to get wrong - and in exchange the bound is probabilistic. For most workloads that is an excellent trade, which is why production systems use it.

The randomness is also **independent of the input**. An adversary who knows your data cannot construct a bad case, because the structure depends on your coin flips rather than on insertion order. That is a real advantage over a plain BST, which degenerates on sorted input.

## Implementation

```java
import java.util.Random;

public final class SkipList {
    private static final int MAX_LEVEL = 16;      // supports ~65k elements comfortably
    private static final double PROMOTE = 0.5;

    private static final class Node {
        final int key;
        final Node[] forward;                     // forward[i] = next node at level i

        Node(int key, int level) {
            this.key = key;
            this.forward = new Node[level];
        }
    }

    private final Node head = new Node(Integer.MIN_VALUE, MAX_LEVEL);
    private final Random random = new Random();
    private int levels = 1;
    private int size;

    public boolean contains(int key) {
        Node current = head;
        for (int level = levels - 1; level >= 0; level--) {
            while (current.forward[level] != null && current.forward[level].key < key) {
                current = current.forward[level];       // move right
            }
        }                                               // else drop down
        Node candidate = current.forward[0];
        return candidate != null && candidate.key == key;
    }

    public boolean add(int key) {
        // update[i] is the node whose level-i pointer will change.
        Node[] update = new Node[MAX_LEVEL];
        Node current = head;
        for (int level = levels - 1; level >= 0; level--) {
            while (current.forward[level] != null && current.forward[level].key < key) {
                current = current.forward[level];
            }
            update[level] = current;
        }

        Node next = current.forward[0];
        if (next != null && next.key == key) {
            return false;                               // already present
        }

        int newLevel = randomLevel();
        if (newLevel > levels) {
            for (int level = levels; level < newLevel; level++) {
                update[level] = head;                   // new levels start at head
            }
            levels = newLevel;
        }

        Node fresh = new Node(key, newLevel);
        for (int level = 0; level < newLevel; level++) {
            fresh.forward[level] = update[level].forward[level];
            update[level].forward[level] = fresh;
        }
        size++;
        return true;
    }

    public boolean remove(int key) {
        Node[] update = new Node[MAX_LEVEL];
        Node current = head;
        for (int level = levels - 1; level >= 0; level--) {
            while (current.forward[level] != null && current.forward[level].key < key) {
                current = current.forward[level];
            }
            update[level] = current;
        }

        Node target = current.forward[0];
        if (target == null || target.key != key) {
            return false;
        }
        for (int level = 0; level < levels; level++) {
            if (update[level].forward[level] != target) {
                break;                                  // target absent above here
            }
            update[level].forward[level] = target.forward[level];
        }
        while (levels > 1 && head.forward[levels - 1] == null) {
            levels--;                                   // shrink empty top levels
        }
        size--;
        return true;
    }

    private int randomLevel() {
        int level = 1;
        while (level < MAX_LEVEL && random.nextDouble() < PROMOTE) {
            level++;
        }
        return level;
    }

    public int size() {
        return size;
    }
}
```

Three details carry the implementation:

**The `update` array is the whole trick.** It records, per level, the last node whose forward pointer must change. Search already visits exactly those nodes, so building it costs nothing extra - and both insertion and deletion become a simple pointer rewrite per level. Without it you would search once per level.

**A sentinel head at every level** removes every null check for "inserting at the front". `Integer.MIN_VALUE` as its key means no real key compares below it. This is the same sentinel discipline the earlier chapters use for ordinary lists, applied per level.

**Shrinking `levels` on removal** matters. Without it, deleting the highest node leaves an empty top level that every subsequent search walks through for nothing. It is a slow leak of search time rather than memory.

## Why concurrency favours skip lists

This is the part that explains `ConcurrentSkipListMap`, and it is a strong senior answer.

A balanced tree rebalances by **rotation**, which restructures a subtree - several pointers change together, and readers must not observe an intermediate state. Making that lock-free is genuinely hard.

A skip list changes only **forward pointers**, level by level, and each is a single reference write. A reader traversing level 0 sees either the old or the new pointer, and both are consistent states. Insertion can therefore proceed with compare-and-swap per level, and readers never block.

That is why `java.util.concurrent` offers `ConcurrentSkipListMap` and no concurrent balanced tree: the structure's update pattern is compatible with lock-free reads, and a tree's is not. `ConcurrentSkipListMap` is also the standard answer for a *sorted* concurrent map, since `ConcurrentHashMap` provides no ordering.

## When to use one

**Reach for a skip list when** you need ordered operations - range queries, floor and ceiling, ordered iteration - with concurrent access, or when you want balanced-tree performance without implementing rebalancing.

**Do not** when a `TreeMap` will do; it is well-tested, gives worst-case guarantees, and is less memory per entry. A hand-written skip list in production needs a reason.

In interviews the question is almost always conceptual - "how would you make a linked list searchable in O(log n)" - so the express-lane picture, the randomized promotion, and the expected-versus-worst-case distinction carry more weight than the code.

## Edge cases and common mistakes

- Claiming worst-case O(log n). The bound is expected; a pathological sequence of coin flips is possible.
- Omitting the `update` array and searching separately for each level.
- Forgetting to raise `levels` when a new node is promoted above the current height.
- Not initializing the new `update` entries to `head` when the height grows.
- Failing to shrink `levels` after removal, leaving empty top levels that every search walks.
- Continuing to unlink above the level where the target stops appearing.
- Choosing a `MAX_LEVEL` too small for the expected size; it caps the achievable height.
- Using a promotion probability far from 0.5 without a measured reason - lower means less memory and slower search.
- Assuming insertion order matters. It does not; the randomness is independent of the input, which is the advantage over an unbalanced BST.
- Writing one in production where `TreeMap` or `ConcurrentSkipListMap` already exists.

## Interview questions and model answers

**How would you search a linked list in O(log n)?**

A skip list. Keep the sorted list at level 0 and add layers of express lanes above it, each holding a random subset of the level below. Search starts top-left, moves right while the next key is smaller, and drops down otherwise. With promotion probability one half, the expected height is log n and the expected search is O(log n).

**Why randomize instead of maintaining perfect levels?**

Because maintaining a perfect distribution under insertion and deletion is exactly the rebalancing problem, and it needs rotations. Randomized promotion gets the same expected distribution with no rebalancing code at all. The cost is that the bound becomes expected rather than worst case.

**What is the worst case?**

O(n). If the coin flips promote nothing, the structure degenerates to a plain sorted linked list. The probability is negligible for any real size, but it is not zero, and it is worth stating precisely rather than claiming a guarantee the structure does not give.

**Skip list or balanced tree?**

A tree gives worst-case O(log n) and uses less memory per entry. A skip list gives expected O(log n) with far simpler code - no rotations, no colour invariants - and it is much friendlier to concurrency. For a single-threaded ordered map I would use `TreeMap`; for a concurrent one, `ConcurrentSkipListMap`.

**Why is `ConcurrentSkipListMap` a skip list rather than a tree?**

Because a skip list's updates are single forward-pointer writes, level by level, so a reader always sees a consistent state and insertion can proceed by compare-and-swap without locking. A balanced tree rebalances by rotation, which changes several pointers together and exposes intermediate states, making a lock-free version much harder.

**What does the `update` array do?**

It records, per level, the last node whose forward pointer will change. The search pass already visits exactly those nodes, so collecting them is free, and insertion or deletion then becomes one pointer rewrite per level rather than a fresh search per level.

## Exercises

1. **Foundation:** Draw a four-level skip list over 1 to 9 and trace the search for 7, counting steps against a plain list.
2. **Foundation:** Compute the probability a node reaches level 5 with promotion probability one half, and the expected height for a thousand nodes.
3. **Interview Core:** Implement the skip list above and verify `contains` against a `TreeSet` over a hundred thousand random operations.
4. **Interview Core:** Remove the `update` array and search per level instead; measure the slowdown.
5. **Interview Core:** Delete the level-shrinking loop, then measure average search steps after removing the highest keys.
6. **Interview Core:** Instrument the structure to report actual height over ten runs of ten thousand insertions, and compare with log n.
7. **SDE-2 Follow-up:** Vary the promotion probability across 0.25, 0.5, and 0.75; plot memory against average search steps and pick a value with a reason.
8. **SDE-2 Follow-up:** Add `floor` and `ceiling`, and explain why they are natural here and awkward on a hash map.
9. **SDE-2 Follow-up:** Insert one to a million in sorted order into both a skip list and an unbalanced BST, and compare heights.
10. **Challenge:** Sketch a lock-free insert using compare-and-swap per level, and state which step needs care when two threads insert adjacent keys.

## Chapter summary

A skip list answers the linked list's defining weakness by layering express lanes over a sorted base list, so search moves right while it can and drops down when it cannot, reaching O(log n) expected time. The distinguishing decision is that it does not maintain a perfect layer distribution - it randomizes promotion with a coin flip, obtaining the expected distribution for free and eliminating rebalancing code entirely. The bound is therefore expected rather than worst case, and stating that distinction precisely matters more than the implementation. The `update` array collected during search is what makes insertion and deletion one pointer rewrite per level, sentinels at every level remove the front-insertion special case, and shrinking the height on removal prevents searches walking empty top levels. The structure earns its production place through concurrency: updates are single forward-pointer writes that leave readers always seeing a consistent state, which is why `ConcurrentSkipListMap` exists and no concurrent balanced tree does.

## Revision checklist

- [ ] I can draw the express-lane structure and trace a search.
- [ ] I can explain randomized promotion and the resulting level distribution.
- [ ] I say "expected O(log n)" and can state the worst case.
- [ ] I know the `update` array is collected during the search pass at no extra cost.
- [ ] I raise the height on promotion and shrink it on removal.
- [ ] I can compare skip lists against balanced trees on guarantee, memory, and code size.
- [ ] I can explain why skip lists suit lock-free concurrency and trees do not.
- [ ] I know `ConcurrentSkipListMap` is the sorted concurrent map.
- [ ] I know the randomness is independent of insertion order.
- [ ] I would use `TreeMap` unless there is a reason not to.
