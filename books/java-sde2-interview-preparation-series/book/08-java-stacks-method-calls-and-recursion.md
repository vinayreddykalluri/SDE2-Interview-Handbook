# Chapter 8: Java Stacks, Method Calls, and Recursion

## Learning objectives

- Trace method invocation through argument evaluation, frame state, dispatch, return, and exceptions.
- Explain parameters and local variables as values, including copied reference values.
- Distinguish mutation through a reference from reassignment of a parameter.
- Model recursive calls and diagnose stack overflow.
- Explain why Java does not guarantee tail-call optimization.

## Why this matters at SDE-2

Call mechanics answer practical questions about aliasing, side effects, recursion depth, stack traces, synchronization boundaries, and debugging optimized code. Pass-by-reference folklore causes API bugs and poor interview answers. Recursive algorithms that look elegant can fail at production input depths because Java does not promise constant-space tail calls.

An SDE-2 also needs a layered view: the bytecode frame model is specified; the source language defines evaluation and pass-by-value; an optimizing runtime may inline calls, keep values in registers, or reconstruct frames for debugging. All three descriptions can be true at once.

## First-principles model

A method call transfers control while giving the callee new parameter variables initialized from argument values. Each active invocation has logically independent local state.

```text
caller expression
  1. evaluate target, if any
  2. evaluate arguments left to right
  3. select/invoke method according to call kind
          |
          v
callee invocation state
  parameters = copies of argument values
  locals + operand stack + return/linkage state
          |
          v
normal return value OR abrupt exception propagation
```

Java has only pass-by-value. For an object expression, the value is a reference. Copying that reference creates another route to the same object; it does not make the parameter an alias for the caller's variable itself.

## Core terminology

- **Argument:** Expression/value supplied at a call site.
- **Parameter:** Callee variable initialized with the argument value.
- **Frame:** Per-invocation storage and control state in the JVM model.
- **Dynamic dispatch:** Runtime selection of an overriding instance method based on receiver class.
- **Static binding:** Selection not based on receiver override, as with static methods and constructors.
- **Recursion:** A method invocation chain in which a method eventually invokes itself.
- **Base case:** Condition that terminates recursive expansion.
- **Stack overflow:** Exhaustion caused by excessive frame depth or related stack limits.
- **Tail call:** A call whose result is immediately returned with no remaining caller computation.
- **Inlining:** Compiler replacement of a call with callee operations; unrelated to source-level recursion guarantees.

## Detailed mechanics

Java evaluates the target expression and argument expressions left to right. Each evaluation can have side effects or throw before invocation begins. Overload selection occurs at compile time based on declared types and applicable conversions. Overriding dispatch occurs at runtime for eligible instance methods based on the actual receiver type.

Representative JVM invocation instructions are:

- `invokestatic` for static methods;
- `invokespecial` for constructors, private methods in relevant class-file forms, and special superclass/interface calls;
- `invokevirtual` for ordinary class instance dispatch;
- `invokeinterface` for interface method dispatch;
- `invokedynamic` for dynamically linked call sites such as common lambda and modern string-concatenation translations.

These are bytecode categories, not a performance ranking. A JIT can inline interface calls when profiles show a stable receiver and can deoptimize if assumptions change.

A JVM frame contains local-variable slots and an operand stack. Parameters occupy initial local slots. For an instance method, local slot 0 initially contains `this`. Bytecodes load operands, perform calculations, and store results. Invocation consumes receiver/argument operands and establishes callee state; return places a result in the caller's computation state. Exceptions skip ordinary return and search handlers while unwinding frames.

> **Specification boundary:** Java specifies argument evaluation order, pass-by-value, method selection/dispatch, and exception behavior. The JVMS specifies abstract frames and invocation instructions. Neither guarantees that optimized native execution materializes one physical frame per source call.

For primitives, the copied parameter contains the primitive value. Reassigning it cannot change the caller variable. For references, the copied parameter and caller variable can initially designate the same object. Mutating that object is visible through other aliases. Reassigning the parameter makes only the parameter hold another reference.

Arrays follow the same rule. A method can mutate elements through its copied array reference; it cannot replace the caller's variable by assigning its parameter to a new array. Wrapper types are objects but immutable, so apparent mutation such as `value++` unboxes and reboxes, then reassigns only the local parameter.

Recursion creates a new logical invocation for each level. Each has independent parameters and locals, while references may point to shared heap objects. A base case stops growth; returns then unwind in reverse order. Direct recursion calls the same method; mutual recursion cycles through methods.

Tail recursion does not change the language rule. The JVM specifications do not require elimination of tail frames, and mainstream Java code must budget O(depth) stack for recursive calls. A JIT may inline a bounded recursion prefix or optimize details, but an algorithm cannot rely on tail-call optimization for correctness.

## Worked Java example

```java
public final class CallSemantics {
    static final class Box {
        int value;
        Box(int value) { this.value = value; }
    }

    static void change(int number, Box box) {
        number = 99;
        box.value = 20;
        box = new Box(30);
        box.value++;
    }

    static long factorial(int n) {
        if (n < 0) throw new IllegalArgumentException("negative");
        return n <= 1 ? 1 : n * factorial(n - 1);
    }

    public static void main(String[] args) {
        int number = 5;
        Box box = new Box(10);
        change(number, box);
        System.out.println(number + ":" + box.value);
        System.out.println(factorial(4));
    }
}
```

The output is:

```text
5:20
24
```

`number` remains 5 because the callee reassigns its copy. The original `Box` becomes 20 through shared-object mutation. Reassigning callee `box` to a second object does not redirect caller `box`.

## Execution or memory walkthrough

At entry to `change`:

```text
main frame                       change frame             heap
number = 5                       number = 5               Box #1 {value=10}
box ----- reference R1 ------->  box = copy of R1 ------> ^
```

1. `number = 99` changes only the `change` frame slot.
2. `box.value = 20` follows R1 and mutates Box #1.
3. `new Box(30)` creates Box #2; assigning its reference changes only `change.box`.
4. `box.value++` changes Box #2 to 31.
5. On return, the `change` frame disappears. If no other reference escaped, Box #2 is collectible. `main.box` still points to Box #1 with value 20.

For `factorial(4)`, logical frames expand:

```text
factorial(4): waiting for 4 * result
factorial(3): waiting for 3 * result
factorial(2): waiting for 2 * result
factorial(1): returns 1
```

Unwinding computes 2, then 6, then 24. At depth `d`, there are O(d) active invocations. `long` overflow occurs after relatively small `n`; correct recursion structure does not guarantee numerically correct unbounded factorials.

When an exception is thrown for negative input, the runtime searches the current frame's exception table. If no compatible handler covers the instruction, the frame is removed and search continues in its caller. `finally` behavior compiled into control flow must execute according to language rules.

## Complexity and performance

`change` is O(1) time and allocates one additional O(1)-sized object. `factorial(n)` is O(n) time and O(n) call depth. An iterative factorial is O(n) time and O(1) auxiliary call-stack space.

Method calls are not automatically expensive. JIT inlining can eliminate dispatch and expose further optimizations. Excessive large methods can inhibit inlining, while tiny abstraction methods often disappear in hot compiled code. Measure rather than manually flattening well-designed APIs.

Recursion is appropriate when problem depth is naturally bounded, such as a balanced tree with controlled height. For adversarial linked lists, arbitrary graph DFS, parsers, or user-supplied nesting, an explicit heap-backed stack gives controllable capacity and avoids process-threatening `StackOverflowError`.

> **HotSpot note:** HotSpot stack size, inlining thresholds, frame layout, guard pages, and stack-bang mechanisms are implementation and platform dependent. The `-Xss` option is common but exact supported syntax and minimums vary.

## Edge cases and common mistakes

- Saying Java passes objects by reference. It passes a reference value by value.
- Confusing overload selection with override dispatch.
- Assuming a `final` parameter creates caller immutability; it only prevents parameter reassignment in that method.
- Forgetting argument expressions run left to right and can fail before the method begins.
- Catching `StackOverflowError` and attempting normal recovery on the same deeply recursive design.
- Assuming tail recursion is constant-space Java.
- Increasing stack size without estimating total memory across thousands of threads.
- Reading an optimized stack trace/profile as a perfect history of physical frames.
- Using recursion on a cyclic graph without a visited set.

## Production engineering notes

API documentation should state mutation behavior. Prefer returning a value or an immutable result over hidden mutation unless mutation is intentional and clear. Defensive copies protect boundaries when callers must not retain mutable aliases.

Diagnose stack overflow from the repeated frame pattern. A repeating short cycle indicates mutual recursion; a single repeated frame suggests missing/late base case or unexpectedly deep input. Capture the triggering input shape. Do not blindly increase `-Xss`: larger per-platform-thread stacks reduce the number of threads supportable under a memory limit.

Virtual threads still preserve Java call semantics, but their stack implementation and scheduling differ from traditional one-OS-thread-per-platform-thread expectations. Never use thread count alone as a substitute for call-depth control.

## Interview questions and model answers

**Is Java pass-by-reference?**

No. Every parameter receives a copied value. For objects, that value is a reference, so caller and callee can reach the same object and observe mutation. Reassigning the callee parameter cannot reassign the caller variable.

**What is stored in a method frame?**

In the JVM model, local-variable slots, an operand stack, and control/linkage state for return and exceptions. Optimized execution can inline calls or keep values elsewhere, so physical state need not mirror source frames.

**Why can recursion overflow when the algorithm has a base case?**

The base case may be reached only after more active calls than the thread stack supports. Input depth, frame size, and stack configuration matter. A base case ensures logical termination, not sufficient memory.

**Does Java optimize tail recursion?**

Java and the JVMS do not guarantee general tail-call elimination. Production Java must assume tail recursion consumes O(depth) stack and convert unbounded cases to iteration or an explicit stack.

## Exercises

1. Modify `change` to return the new `Box`; show how the caller can deliberately replace its reference.
2. Draw frames for `factorial(5)` during expansion and unwinding.
3. Convert factorial to an iterative version and compare overflow behavior.
4. Implement depth-first traversal both recursively and with `ArrayDeque`.
5. Create overload and override examples and identify compile-time versus runtime decisions.
6. Explain how an exception propagates through three calls with one matching handler.

## Chapter summary

Method calls copy argument values into independent invocation state. A copied reference can reach a shared object, so mutation propagates through aliases while parameter reassignment does not. Frames model locals, operand computation, return, and exceptions. Recursion creates O(depth) active state, and Java offers no portable tail-call elimination. Optimized physical execution may inline or reconstruct frames without changing these semantics.

## Revision checklist

- [ ] I can explain Java pass-by-value with primitives and references.
- [ ] I can separate mutation from parameter reassignment.
- [ ] I know the main JVM invocation instruction categories.
- [ ] I can draw frame expansion and unwinding for recursion.
- [ ] I can choose recursion versus an explicit stack based on depth risk.
- [ ] I do not rely on tail-call optimization or physical frame layout.

