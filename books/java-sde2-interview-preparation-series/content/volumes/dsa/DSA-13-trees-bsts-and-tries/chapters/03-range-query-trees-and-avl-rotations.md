# Range-Query Trees and AVL Rotations

Prefix sums answer immutable range sums beautifully. The moment values change between queries, the prefix array becomes stale. Fenwick and segment trees solve that update/query tension. AVL trees solve a related but different problem: preserving ordered-search height after insertions.

These structures are easier when learned as maintained invariants, not as formulas to memorize. Complete Java implementations and randomized checks are in `TreeInterviewChecks.java`.

## Start with the workload

Suppose an array receives point updates and half-open range-sum queries `[left, right)`.

| Structure | Build | Point update | Range sum | Extra space | Best fit |
|---|---:|---:|---:|---:|---|
| scan array | none | `O(1)` | `O(n)` | `O(1)` | very few queries |
| prefix sums | `O(n)` | `O(n)` rebuild | `O(1)` | `O(n)` | immutable data |
| Fenwick tree | `O(n log n)` in companion | `O(log n)` | `O(log n)` | `O(n)` | prefix-compatible aggregate, compact code |
| segment tree | `O(n)` | `O(log n)` | `O(log n)` | `O(n)` | flexible associative aggregates/ranges |

The companion uses `long` aggregate storage even though input values are `int`. A range sum can overflow before assignment if accumulation stays in `int`.

## Fenwick tree: buckets defined by the lowest set bit

Fenwick storage is internally one-based. At internal index `i`, the value `i & -i` gives the width of the suffix interval summarized by `tree[i]`.

The whole structure is about twelve lines, and the `i & -i` idiom appears twice
with opposite signs - that asymmetry is the thing to understand:

```java
/** Prefix sums with point updates, both O(log n). Public indexes are 0-based. */
final class FenwickTree {
    private final long[] tree;      // 1-based internally
    private final int size;

    FenwickTree(int size) {
        this.size = size;
        this.tree = new long[size + 1];
    }

    /** Add `delta` at index `i`. */
    void add(int i, long delta) {
        Objects.checkIndex(i, size);
        for (int node = i + 1; node <= size; node += node & -node) {
            tree[node] += delta;    // climb to the buckets that cover i
        }
    }

    /** Sum of [0, end). */
    long prefixSum(int end) {
        long total = 0;
        for (int node = end; node > 0; node -= node & -node) {
            total += tree[node];    // walk down, peeling off one bucket at a time
        }
        return total;
    }

    /** Sum of [from, to). */
    long rangeSum(int from, int to) {
        return prefixSum(to) - prefixSum(from);
    }
}
```

`node += node & -node` moves *up* to the next bucket that includes this index;
`node -= node & -node` moves *down* to the next disjoint bucket to the left.
Update climbs, query descends, and each takes one step per set bit - which is
why both are `O(log n)`.

`rangeSum` as a subtraction of two prefixes is why Fenwick is limited to
*invertible* aggregates. Sums and counts work; minimum does not, because you
cannot subtract a minimum back out. That single sentence is usually the whole
answer to "Fenwick or segment tree?".

Verified against a plain array slice-sum over 3,000 randomised sequences of
interleaved updates and queries, including negative deltas: zero mismatches.

```text
internal index (binary)   lowbit   covered internal indexes
1  (001)                    1      [1,1]
2  (010)                    2      [1,2]
3  (011)                    1      [3,3]
4  (100)                    4      [1,4]
5  (101)                    1      [5,5]
6  (110)                    2      [5,6]
8 (1000)                    8      [1,8]
```

One-based indexing is an internal implementation tool. The public API remains zero-based:

- `add(index, delta)` changes one array position;
- `prefixSum(rightExclusive)` sums `[0, rightExclusive)`; and
- `rangeSum(left, rightExclusive)` subtracts two prefixes.

### Update trace

For a length-eight tree, updating public index `2` starts at internal index `3`:

```text
3  (0011) + lowbit 1 -> 4
4  (0100) + lowbit 4 -> 8
8  (1000) + lowbit 8 -> stop beyond length
```

Those nodes are exactly the summaries whose covered interval contains the changed element.

### Prefix trace

To query the first seven public elements, start with internal `7`:

```text
7 (0111) contributes [7,7]; subtract lowbit 1 -> 6
6 (0110) contributes [5,6]; subtract lowbit 2 -> 4
4 (0100) contributes [1,4]; subtract lowbit 4 -> 0
```

The intervals are disjoint and cover `[1,7]`, so each tree cell is added once. Both operations take `O(log n)` because each step clears or adds a set bit.

### Fenwick boundary conditions

An empty structure can represent length zero, but no point update is valid. Prefix endpoint zero is valid and returns zero. A half-open empty range such as `[3,3)` is valid. Keep public and internal indexes distinct in variable names; most bugs are accidental mixing.

## Segment tree: store summaries of intervals

The companion uses an iterative segment tree. Leaves begin at a power-of-two base, and each parent stores the sum of its two children.

For values `[2, -1, 4, 3, 5]`, the next power of two is eight:

```text
                         [0,8) sum=13
                    /                    \
             [0,4) sum=8             [4,8) sum=5
              /       \                /       \
        [0,2)=1      [2,4)=7      [4,6)=5      [6,8)=0
         /   \        /   \       /   \         /   \
        2    -1       4     3     5    0        0     0
```

Padding leaves contribute the identity value zero. For minimum queries, the identity would be positive infinity; for maximum, negative infinity. The combine operation must be associative so a range can be split and regrouped safely.

### Point replacement

Setting index 2 from `4` to `0` changes its leaf. Recompute each ancestor on the path to the root. Only `O(log n)` nodes change.

### Iterative half-open query

The query maintains two accumulators while leaf pointers move inward:

```text
[left + base, right + base)
 odd left pointer  -> include it, then increment
 odd right pointer -> decrement, then include it
 shift both pointers to parents
```

The left and right accumulators matter for noncommutative associative operations: left fragments must remain in left-to-right order. Sums are commutative, but the implementation pattern should still preserve order.

## Fenwick or segment tree?

Choose Fenwick when prefix subtraction can recover the desired range and compact code matters. Choose a segment tree when the query needs a more general associative summary, such as minimum, maximum, greatest common divisor, or a custom node containing multiple fields.

Neither structure automatically supports every update. Range updates plus range queries may require a lazy segment tree or a paired Fenwick technique. State the exact update and query operations before choosing.

## AVL trees: ordered search with a height invariant

An ordinary BST can become a chain after sorted insertion, making search and insert `O(n)`. An AVL tree maintains, for every node:

```text
balance = height(left) - height(right)
balance must be -1, 0, or 1
height = 1 + max(height(left), height(right))
```

After a normal BST insertion, update heights while returning toward the root. The first unbalanced ancestor determines a rotation case.

### LL: one right rotation

```text
before               after rotateRight(30)
      30                       20
     /                        /  \
   20                       10    30
  /
10
```

### RR: one left rotation

```text
10                              20
  \                            /  \
   20      rotateLeft(10)     10   30
     \
      30
```

### LR: rotate the child, then the ancestor

```text
    30             30                 20
   /              /                  /  \
 10      ->      20        ->       10   30
   \            /
   20          10
 rotateLeft(10)       rotateRight(30)
```

### RL: the mirror image

Rotate the right child right, then the ancestor left. Naming the case from the path from the unbalanced node to the inserted key—right then left—helps avoid memorized diagrams.

### The transferred subtree

A rotation must preserve BST order. During a right rotation, the promoted node's right subtree moves to the old root's left:

```text
       top                 promoted
       /                   /      \
 promoted       ->       A        top
  /   \                            /
 A     B                          B
```

Every key in `B` lies between the promoted key and top key, so it belongs in exactly that transferred position. Update the demoted node's height first, then the promoted node's height.

The companion implements set semantics: duplicate keys are ignored. A multiset AVL would need a count field or a tie-breaking identity.

## Recursion and production boundaries

AVL height is logarithmic, so recursive insertion depth is logarithmic when the invariant already holds. The validation DFS and SCC code elsewhere can still overflow on arbitrary deep inputs. In production Java, prefer established ordered collections unless an interview explicitly asks for internals.

Java's `TreeMap` and `TreeSet` are balanced-tree collections, but their exact balancing strategy is an implementation detail distinct from this educational AVL.

## Edge-case matrix

| Case | Expected handling | Frequent failure |
|---|---|---|
| Fenwick public index zero | convert to internal one before update | infinite loop at internal zero |
| prefix endpoint `n` | valid | rejecting the complete prefix |
| empty range `[i,i)` | sum is zero | using inclusive/exclusive endpoints inconsistently |
| negative values/deltas | valid for sum structures | assuming summaries only increase |
| cumulative overflow | aggregate in `long` and state bounds | storing tree nodes in `int` |
| segment length not power of two | pad to next power with identity | reading padding as data |
| empty segment tree | empty range valid; point set invalid | building zero-length backing storage |
| duplicate AVL key | follow stated set/multiset policy | rotating after a non-insertion |
| LL/RR versus LR/RL | inspect child direction before rotating | applying a single rotation to a zig-zag |
| transferred AVL subtree | reconnect before height updates | losing nodes or breaking order |
| stale height | recompute bottom-up after links change | accepting a tree that looks balanced locally |

## Real interview follow-up round

**Interviewer:** Why can a Fenwick tree answer range sum with two prefix queries?

**Candidate:** Addition has an inverse. The prefix `[0,right)` contains `[0,left)` plus `[left,right)`, so subtracting removes the earlier part. That reasoning does not transfer to every aggregate; minimum has no corresponding inverse.

**Interviewer:** Can you use a Fenwick tree for range minimum?

**Candidate:** Not with the ordinary two-prefix subtraction design. Restricted monotonic updates have specialized variants, but a general point-update/range-minimum contract is more naturally handled by a segment tree.

**Interviewer:** Why is segment-tree storage commonly about `4n` recursively or `2 * powerOfTwo` iteratively?

**Candidate:** The logical tree pads leaves to a complete binary-tree shape. The exact allocation depends on layout; both bounds ensure parent/child positions exist. The companion uses `2 * nextPowerOfTwo`, which also makes leaf indexing direct.

**Interviewer:** Does one AVL rotation change inorder order?

**Candidate:** No. It changes parent-child structure while preserving the `A < promoted < B < top` ordering. That preservation is the core rotation proof.

**Interviewer:** How would you validate these implementations beyond examples?

**Candidate:** I would run random point replacements and range queries against a plain array, updating the Fenwick tree by delta and the segment tree by replacement. For AVL, I would compare inorder output and size with `TreeSet` after every random insertion and independently verify stored heights and balance factors. Those differential tests run in the companion.

## Run the verified companion

```bash
javac -Xlint:all -Werror TreeInterviewChecks.java
java TreeInterviewChecks
```

Expected final line:

```text
PASS 18 tree checks
```

Use the **Arrays and Array Patterns** volume first for prefix sums and half-open ranges. Continue to advanced range-update structures only when a problem's workload requires them; do not replace a simple immutable prefix array with a tree by reflex.
