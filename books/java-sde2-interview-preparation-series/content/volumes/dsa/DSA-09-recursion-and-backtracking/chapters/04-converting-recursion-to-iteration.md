# 4. Converting Recursion to Iteration

## Why this chapter exists

Java does not perform tail-call optimization. A recursive method that calls itself ten thousand deep will throw `StackOverflowError` regardless of how simple the call is, and no compiler flag changes that. This is not a Java quirk to complain about - it is a constraint you design around, and interviewers test whether you can.

The question arrives in two forms. **"What happens if the input is a million elements?"** is asking whether you know the stack will blow. **"Now do it iteratively"** is asking whether you can perform the conversion systematically rather than by inventing a new algorithm under pressure.

The conversion is mechanical once you know the recipe, and the recipe differs depending on where the recursive call sits.

## Why Java has no tail-call optimization

A tail call is a recursive call in the final position, where nothing remains to be done after it returns:

```java
// Tail recursive: nothing happens after the call.
static int sumTail(int[] values, int index, int accumulator) {
    if (index == values.length) {
        return accumulator;
    }
    return sumTail(values, index + 1, accumulator + values[index]);
}

// NOT tail recursive: the addition happens after the call returns.
static int sumNaive(int[] values, int index) {
    if (index == values.length) {
        return 0;
    }
    return values[index] + sumNaive(values, index + 1);
}
```

A language with TCO reuses the current stack frame for `sumTail`, making it a loop. Java does not, and both versions overflow at the same depth.

The reason is deliberate. The JVM's security and stack-inspection model - historically `SecurityManager`, and still stack traces and `StackWalker` - depends on frames being real. Eliminating them changes observable behaviour. The Loom and Valhalla work has revisited this, but as of Java 25 there is no TCO, and writing `return f(...)` gains nothing.

**The practical consequence:** the depth at which you overflow depends on frame size and the `-Xss` setting, typically somewhere in the range of a few thousand to a few tens of thousands of frames. Treat it as "roughly ten thousand" and never as a number to rely on.

> **Specification boundary:** the JVM specification does not mandate a stack size, does not require TCO, and does not define the depth at which `StackOverflowError` is thrown. It is a property of the implementation, the platform, the frame size, and `-Xss`. Any algorithm whose recursion depth scales with input size is therefore unbounded in a way the language will not protect you from.

## Recognizing the danger before it fires

Recursion depth is safe when it is bounded by something small, and dangerous when it scales with `n`:

| Shape | Depth | Safe? |
|---|---|---|
| Balanced tree traversal | O(log n) | Yes - 10^9 nodes is depth 30 |
| **Degenerate tree traversal** | O(n) | **No** - a linked-list-shaped tree overflows |
| Linked-list recursion | O(n) | **No** |
| Quicksort, recursing into both halves | O(n) worst case | **No** - recurse into the smaller side only |
| Merge sort | O(log n) | Yes |
| DFS on a graph | O(V) | **No** on a long path |
| Backtracking - permutations, n-queens | O(n) where n is small | Usually yes |

![Depth is safe when bounded by log n and dangerous when it scales with n](content/volumes/dsa/DSA-09-recursion-and-backtracking/assets/01-recursion-depth-and-quicksort.png)

The tree row is the trap. A recursive traversal is perfectly safe on the balanced trees used in examples and overflows on the degenerate one an adversarial test supplies. "It works on my test data" is exactly the failure mode.

Backtracking is usually fine because depth is bounded by the *solution* size - permutations of 10 elements recurse 10 deep, not 10! deep. Knowing which dimension bounds the depth is the distinction.

## Conversion recipe 1: tail position becomes a loop

When the recursive call is the last action and nothing is pending, replace the call with reassignment and loop.

```java
// Recursive
static int gcd(int a, int b) {
    if (b == 0) {
        return a;
    }
    return gcd(b, a % b);
}

// Iterative: parameters become mutable locals, the call becomes a loop step
static int gcdIterative(int a, int b) {
    while (b != 0) {
        int remainder = a % b;
        a = b;
        b = remainder;
    }
    return a;
}
```

The mechanical rule: **each recursive call's arguments become assignments to the parameter variables, and the whole body becomes a `while` loop.** The base case becomes the loop's exit condition.

This is the easy case, and it is also the one people wrongly assume the JVM will handle for them.

## Conversion recipe 2: one pending operation becomes an accumulator

If work remains after the recursive call, first try to move that work *before* the call by carrying an accumulator. This transforms non-tail recursion into tail recursion, which then becomes a loop by recipe 1.

```java
// Non-tail: the multiply happens after the call
static long factorial(int n) {
    return n <= 1 ? 1 : n * factorial(n - 1);
}

// Accumulator-passing: now tail recursive
static long factorialTail(int n, long accumulator) {
    return n <= 1 ? accumulator : factorialTail(n - 1, n * accumulator);
}

// Loop
static long factorialIterative(int n) {
    long accumulator = 1;
    for (int i = 2; i <= n; i++) {
        accumulator *= i;
    }
    return accumulator;
}
```

This works when the pending operation is **associative** - addition, multiplication, min, max, string concatenation. It does not work when the pending work depends on the *result* in a way that cannot be reordered.

Reversing a linked list is the canonical case where accumulator passing is not just possible but produces the standard iterative solution:

```java
static ListNode reverse(ListNode head) {
    ListNode previous = null;               // the accumulator
    ListNode current = head;
    while (current != null) {
        ListNode next = current.next;       // save before overwriting
        current.next = previous;            // the pending work, done early
        previous = current;
        current = next;
    }
    return previous;
}
```

## Conversion recipe 3: two or more calls need an explicit stack

When a method recurses more than once - tree traversal, divide and conquer - no accumulator trick applies. You must simulate the call stack.

The mechanical translation: **push what the recursive calls would have been, in reverse order, so they pop in the original order.**

```java
// Recursive preorder
static void preorder(TreeNode node, List<Integer> out) {
    if (node == null) {
        return;
    }
    out.add(node.val);
    preorder(node.left, out);
    preorder(node.right, out);
}

// Iterative: push right first so left pops first
static List<Integer> preorderIterative(TreeNode root) {
    List<Integer> out = new ArrayList<>();
    if (root == null) {
        return out;
    }
    Deque<TreeNode> stack = new ArrayDeque<>();
    stack.push(root);
    while (!stack.isEmpty()) {
        TreeNode node = stack.pop();
        out.add(node.val);
        if (node.right != null) {
            stack.push(node.right);      // pushed first, popped last
        }
        if (node.left != null) {
            stack.push(node.left);
        }
    }
    return out;
}
```

Preorder is the easy one because the visit happens *before* both calls. Inorder and postorder are harder, because the method must resume at a point *between* or *after* the recursive calls - and that resumption point is state the recursive version stored implicitly in the program counter.

### The general technique: an explicit state machine

When a method has several resumption points, make the state explicit. Each stack frame records both the data and *where in the method to continue*.

```java
static List<Integer> postorderIterative(TreeNode root) {
    List<Integer> out = new ArrayList<>();
    if (root == null) {
        return out;
    }
    // phase 0 = descend left, 1 = descend right, 2 = visit
    record Frame(TreeNode node, int phase) {}
    Deque<Frame> stack = new ArrayDeque<>();
    stack.push(new Frame(root, 0));

    while (!stack.isEmpty()) {
        Frame frame = stack.pop();
        TreeNode node = frame.node();
        switch (frame.phase()) {
            case 0 -> {
                stack.push(new Frame(node, 1));            // resume here after left
                if (node.left != null) {
                    stack.push(new Frame(node.left, 0));
                }
            }
            case 1 -> {
                stack.push(new Frame(node, 2));            // resume here after right
                if (node.right != null) {
                    stack.push(new Frame(node.right, 0));
                }
            }
            default -> out.add(node.val);
        }
    }
    return out;
}
```

This is verbose, and that verbosity is the honest cost of the conversion. But it is **fully general**: any recursive method converts this way, with one phase per resumption point. When an interviewer asks for an iterative version of something awkward, this recipe always works, and saying "I would make the resumption point explicit" is a better answer than searching for a clever trick.

There is a well-known shortcut for postorder - do a reversed preorder visiting right before left, then reverse the output - and it is worth knowing. But it is specific to postorder, whereas the phase technique generalizes.

## Bounding depth instead of converting

Sometimes the cheapest fix is not conversion but reducing the depth.

**Recurse into the smaller half.** Quicksort's worst-case O(n) depth becomes O(log n) if you always recurse into the smaller partition and loop on the larger:

```java
static void quicksort(int[] values, int low, int high) {
    while (low < high) {
        int pivot = partition(values, low, high);
        if (pivot - low < high - pivot) {
            quicksort(values, low, pivot - 1);   // recurse into the smaller side
            low = pivot + 1;                     // loop on the larger
        } else {
            quicksort(values, pivot + 1, high);
            high = pivot - 1;
        }
    }
}
```

Depth is now O(log n) even on adversarial input, because each recursive call handles at most half the range. This is a two-line change with a large effect, and it is a good answer to "your quicksort overflows on sorted input".

**Run on a dedicated thread with a larger stack** when the recursion is genuinely deep and conversion is not worth it:

```java
Thread worker = new Thread(null, task, "deep-recursion", 64 * 1024 * 1024);
```

The four-argument `Thread` constructor takes a stack size. This is a legitimate engineering answer for a batch job, and a poor one for a request handler - it raises the ceiling without removing it. Note also that **virtual threads do not accept a stack-size hint**; their stacks grow on the heap, which changes the failure mode but does not give unbounded depth.

## Edge cases and common mistakes

- Assuming the JVM optimizes tail calls. It does not, at any release.
- Writing `return f(...)` believing it costs no frame.
- Testing recursion only on balanced trees and shipping it against degenerate ones.
- Recursing into both quicksort partitions, leaving O(n) worst-case depth.
- Pushing children in the wrong order and getting a mirrored traversal.
- Converting inorder or postorder by pattern-matching on preorder's shape without handling resumption.
- Catching `StackOverflowError` and continuing; the stack is in an unknown state and recovery is unreliable.
- Relying on a specific overflow depth. It varies with platform, frame size, and `-Xss`.
- Passing a stack size to a virtual thread, which ignores it.
- Converting to iteration when reducing depth would do, at a fraction of the complexity.
- Forgetting that an explicit stack still uses O(n) heap; conversion removes the *stack* limit, not the space cost.

## Interview questions and model answers

**Does Java optimize tail recursion?**

No, at any release. Writing the call in tail position gains nothing, and the method overflows at the same depth as the non-tail version. The reason is that the JVM's stack-inspection behaviour - stack traces, `StackWalker`, historically the security manager - depends on frames actually existing.

**Your recursive tree traversal is given a million nodes. What happens?**

It depends on the shape. A balanced tree is depth 20 and fine. A degenerate tree - every node having one child - is depth one million and throws `StackOverflowError`. Since the shape is usually not guaranteed, I would convert to an explicit stack, or use Morris traversal if O(1) space is also wanted.

**Convert this recursion to iteration.**

It depends where the call sits. Tail position becomes a `while` loop with the arguments reassigned to the parameters. One pending associative operation becomes an accumulator carried forward, which makes it tail recursive first. Two or more calls need an explicit stack, pushing in reverse so they pop in order - and if the method resumes at several points, each stack frame carries a phase saying where to continue. That last recipe is fully general.

**Your quicksort overflows on sorted input. Fix it without converting to iteration.**

Recurse into the smaller partition and loop on the larger. Each recursive call then covers at most half the range, so depth is O(log n) even in the worst case. Two lines, and it removes the overflow entirely.

**Can you just catch `StackOverflowError`?**

You can catch it, but you should not treat it as recoverable. It is an `Error`, the stack is in an unknown state, and any cleanup you attempt may itself need frames you do not have. It is a signal that the design assumed bounded depth and the assumption was wrong.

**When would you increase the stack size instead of converting?**

For a batch or offline job where the depth is known and bounded and the conversion is not worth the complexity - a dedicated thread with a large stack via the four-argument `Thread` constructor. Not for a request handler, because it raises the ceiling rather than removing it, and every concurrent request pays the memory. Virtual threads ignore the hint entirely.

## Exercises

1. **Foundation:** Write a recursive sum over an array and find, empirically, the depth at which it overflows on your JVM. Then change `-Xss` and measure again.
2. **Foundation:** Convert `gcd` and `factorial` to iteration using recipes 1 and 2. State which recipe each needed and why.
3. **Interview Core:** Convert recursive preorder, inorder, and postorder to iteration. Use the phase technique for postorder, then compare against the reverse-preorder shortcut.
4. **Interview Core:** Build a degenerate tree of 10^6 nodes and demonstrate the recursive traversal overflowing where the iterative one succeeds.
5. **Interview Core:** Implement quicksort recursing into both halves, overflow it on sorted input, then apply the smaller-half fix and show the depth drop.
6. **Interview Core:** Convert recursive linked-list reversal to iteration and identify what plays the role of the accumulator.
7. **SDE-2 Follow-up:** Take a recursive method with three resumption points and convert it with the phase technique. Count the phases before writing code.
8. **SDE-2 Follow-up:** Run a deep recursion on a `Thread` with a 64 MB stack, then on a virtual thread, and describe how the failure differs.
9. **SDE-2 Follow-up:** Convert recursive DFS on a graph to iteration and confirm the visit order matches.
10. **Challenge:** Write a general converter for a two-call recursive method: given the recursive form, produce the phase-based iterative version and verify equivalence on random inputs.

## Chapter summary

Java performs no tail-call optimization and never has, so recursion depth is a hard resource bound rather than a stylistic concern, and the depth at which it fails is an implementation property you cannot rely on. Depth is safe when bounded by something small - a balanced tree, a backtracking solution size - and dangerous whenever it scales with `n`, which is why a traversal that passes every balanced test overflows on a degenerate one. Conversion follows three recipes by call position: tail position becomes a loop with reassigned parameters; a single pending associative operation becomes an accumulator that makes the call tail recursive first; and two or more calls need an explicit stack, with a phase recorded per frame when the method resumes at more than one point. That last recipe is verbose and fully general, and naming it is a better answer than hunting for a trick. Before converting at all, consider bounding the depth instead - recursing into quicksort's smaller partition turns worst-case O(n) depth into O(log n) for two lines.

## Revision checklist

- [ ] I know Java has no TCO and can say why.
- [ ] I can identify which recursions have depth proportional to input size.
- [ ] I know a degenerate tree breaks a traversal that balanced tests pass.
- [ ] I can convert tail recursion to a loop mechanically.
- [ ] I can introduce an accumulator and say when it is valid.
- [ ] I can convert multi-call recursion with an explicit stack, pushing in reverse.
- [ ] I can apply the phase technique when a method has several resumption points.
- [ ] I can fix quicksort's depth by recursing into the smaller partition.
- [ ] I know `StackOverflowError` is not meaningfully recoverable.
- [ ] I know virtual threads ignore a stack-size hint.
