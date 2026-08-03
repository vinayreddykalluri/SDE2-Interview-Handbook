# Pointer Surgery and Node Identity

Linked-list code is not hard because nodes are advanced mathematics. It is hard because one assignment can disconnect the only reference to the remaining list. Strong candidates draw identities and arrows, save the next edge before overwriting it, and state whether they are reusing nodes or allocating copies.

The complete executable implementations are in `LinkedListInterviewChecks.java`.

## A reference is not a node

```text
head ──> [1] ──> [2] ──> [3] ──> null
          ^
        current
```

`head` and `current` can refer to the same node. Assigning `current = current.next` moves one reference; it does not move or copy a node. Assigning `current.next = previous` changes the graph.

Two nodes with equal `value` remain different nodes. Cycle entry and intersection are identity questions, so compare references with `==`, not values.

## Full reversal: save before cutting

Maintain:

```text
previous -> already reversed prefix
current  -> first node not yet reversed
```

One iteration is:

```text
next = current.next
current.next = previous
previous = current
current = next
```

Saving `next` first preserves access to the unprocessed suffix. When `current == null`, `previous` is the new head. The loop is `O(n)` time and `O(1)` auxiliary space.

## Sentinels remove head special cases

A temporary sentinel points at the real head:

```text
sentinel -> head -> ...
```

Deleting or inserting at position one then looks like the same “change `before.next`” operation used in the middle. The sentinel is not returned as data.

For one-based sublist reversal `[left,right]`, the companion validates the complete range before mutating. It then repeatedly removes the node after the range tail and inserts it after `before`:

```text
1 -> 2 -> 3 -> 4 -> 5, reverse [2,4]

1 -> 3 -> 2 -> 4 -> 5
1 -> 4 -> 3 -> 2 -> 5
```

Prevalidation matters: discovering an invalid right endpoint halfway through pointer changes would leave a partially modified input.

## Two pointers with a measured gap

To remove the nth node from the end, place `ahead` exactly `n` nodes beyond a sentinel-based follower. Move both until `ahead` reaches the last node. The follower then sits immediately before the target.

The sentinel makes removing the original head ordinary. Validate `n > 0` and advance the gap before mutation; if the list ends early, reject.

## Merge sorted lists by relinking

The companion reuses nodes and takes from the left list on equality, making the merge stable across the two inputs. Its contract requires both lists to be:

- acyclic;
- nondecreasing; and
- disjoint by identity.

If inputs share a suffix, naïvely relinking can attach the same node twice or create a cycle. Contract assumptions are part of correctness, even when a coding-platform prompt guarantees them.

## Floyd cycle entry

Move `slow` one step and `fast` two. If they meet, reset a `seeker` to head and move both one step; their next meeting is the cycle entry.

```text
head -> prefix length a -> cycle entry
                          cycle length c
```

At the first meeting, fast has traveled twice slow's distance, so slow's distance differs by a multiple of `c`. The remaining offset from meeting to entry matches the prefix offset modulo the cycle. The second phase aligns them without measuring `c`.

Check `fast` and `fast.next` before advancing two steps. Empty, singleton, self-cycle, and no-cycle inputs must all terminate.

## Intersection means shared suffix

For acyclic singly linked lists, once two lists share a node, every later node is shared. Align their remaining lengths, then advance together until references match.

```text
A: 1 -> 2 ---\
              -> 8 -> 9
B:      3 ---/
```

Equal values in independently allocated nodes are not intersection. For cyclic lists, the problem has additional cases and this acyclic length algorithm is not a valid general solution.

## Copying random pointers without a map

The usual map solution is clear: original node to copied node, then a second pass connects `next` and `random`. The companion also demonstrates the `O(1)` auxiliary mapping technique:

1. weave each copy after its original;
2. set `copy.random = original.random.next`;
3. detach the two lists while restoring original `next` links.

```text
original A -> original B
becomes
original A -> copy A -> original B -> copy B
```

This saves a hash map but temporarily mutates the input and is harder to make exception-safe or concurrency-safe. In production, the map solution may be the better engineering choice.

## Edge-case matrix

| Case | Correct handling | Common failure |
|---|---|---|
| null head | return null/true according to method | dereference first node |
| one node | reversal unchanged | creating self-cycle |
| reverse starts at head | sentinel handles it | losing new head |
| invalid sublist endpoint | reject before mutation | leave partial reversal |
| remove `n == length` | remove head | off-by-one gap |
| `n > length` | reject | null dereference |
| equal merge values | state stable tie policy | accidental reordering |
| shared merge inputs | prohibit or handle explicitly | duplicate links/cycle |
| self-cycle | Floyd returns that node | infinite render/length scan |
| equal values, separate nodes | not an intersection | compare payload instead of identity |
| random pointer to self | copy must point to copied self | pointer back to original |
| preservation required | restore every temporary weave/reversal | hidden input mutation |

## Six live interview Q&A chains

### 1. Reversal order

**Interviewer:** Why save `current.next` first?

**Candidate:** Reassigning `current.next` cuts the only forward edge from the processed node. Without a saved reference, the unprocessed suffix becomes unreachable.

### 2. Sentinel choice

**Interviewer:** Is a sentinel extra space?

**Candidate:** It is one node, so `O(1)` auxiliary space. It removes separate head logic and reduces pointer branches, which is usually worth the constant allocation.

### 3. Validation and mutation

**Interviewer:** Your sublist right endpoint is invalid. Can you discover that while reversing?

**Candidate:** I could, but then failure may leave input partially mutated. I first establish length or walk to validate both boundaries, then perform pointer surgery.

### 4. Intersection

**Interviewer:** Both lists contain a node with value 8. Is that the intersection?

**Candidate:** Only if both `next` chains reach the same node object. Intersection is shared identity and therefore a shared suffix, not equal payload.

### 5. Palindrome restoration

**Interviewer:** Why reverse the second half back after comparing?

**Candidate:** The method's contract is observational—it should answer a question, not leave the list modified. Restoration prevents a surprising side effect and is explicitly tested.

### 6. Random-pointer copy

**Interviewer:** Would you choose weaving in production?

**Candidate:** Only when temporary mutation is safe and auxiliary memory is a real constraint. A map is clearer, does not expose a transient corrupted view to other readers, and is easier to recover from exceptions. I can explain both trade-offs.

## Run the companion

```bash
javac --release 21 -Xlint:all -Werror LinkedListInterviewChecks.java
java LinkedListInterviewChecks
```

Expected final line: `PASS 20 linked-list checks`.
