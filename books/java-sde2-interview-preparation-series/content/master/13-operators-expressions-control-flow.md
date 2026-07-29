# 13. Operators, Expressions, and Control Flow

## Learning objectives

By the end of this chapter, you should be able to:

- evaluate Java expressions using precedence, associativity, promotion, and short-circuit rules;
- distinguish statement forms from value-producing expressions;
- design readable branching and iteration with explicit invariants;
- use modern `switch` expressions safely; and
- identify control-flow bugs caused by side effects, fall-through, labels, and abrupt completion.

## Why this matters at SDE-2

Control flow is where requirements become executable decisions. At SDE-2 level, correctness includes more than reaching the happy-path result. You must reason about whether a side effect runs, whether a loop terminates, whether cleanup executes, and whether all domain states are represented.

Interview code often compresses these questions into a loop or conditional. Production code spreads them across authorization, retries, state transitions, and pricing rules. A precise expression model makes both settings easier.

> **Focused series path:** Finish this Java foundation first, then study Time and Space Complexity Study Step 02. Continue to Number Systems Study Step 03A for mathematical modulo and overflow-safe numeric patterns, Study Step 04 for full bit manipulation, and Study Step 05 for loop, pattern, and index-calculation drills. The stable filenames and recommended order are listed in `Java-SDE2-Interview-Preparation-Series-Index.pdf`.

## First-principles model

An expression is evaluated to produce a value, a variable, or a side effect. A statement controls execution: declaration, expression statement, block, branch, loop, transfer, or exception. Operators define how operand values are combined, but evaluation order determines when operands and side effects occur.

Java evaluates operand expressions from left to right. Precedence determines grouping, not temporal reordering. Parentheses should communicate intent even when precedence already makes code legal.

Control normally completes a statement and continues to the next one. `break`, `continue`, `return`, and `throw` complete abruptly and transfer control. Loops are best understood through an invariant: a property true before and after each iteration, plus a progress measure that proves termination.

> **Specification boundary:** Java specifies left-to-right operand evaluation and short-circuit behavior. An optimizing JVM may rearrange machine instructions only if the observable Java behavior, including exceptions and synchronization semantics, is preserved.

## Core terminology

- **Operand:** input expression to an operator.
- **Precedence:** which operator binds more tightly in source parsing.
- **Associativity:** how equal-precedence operators group.
- **Short-circuiting:** skipping the right operand when the left determines a boolean result.
- **Side effect:** observable state change, I/O, synchronization action, or exception.
- **Statement expression:** an expression allowed as a statement, such as assignment or method invocation.
- **Loop invariant:** claim maintained by every completed iteration.
- **Abrupt completion:** transfer caused by `break`, `continue`, `return`, or `throw`.
- **Exhaustiveness:** every possible selector value is handled by a `switch` expression.

## Detailed mechanics

### Operator families

Arithmetic operators are `+`, `-`, `*`, `/`, and `%`. Unary `+` and `-` apply numeric promotion. Prefix and postfix `++` and `--` both mutate a variable; postfix yields the old value and prefix yields the new value. Avoid embedding them in larger expressions.

Relational operators (`<`, `<=`, `>`, `>=`) compare numeric values. Equality (`==`, `!=`) compares primitive values or reference identity after permitted conversions. It does not invoke `equals`. Boolean logical operators are `!`, `&&`, `||`, `&`, `|`, and `^`. With booleans, `&` and `|` evaluate both sides; `&&` and `||` short-circuit.

Bitwise `&`, `|`, `^`, and `~` operate on promoted integral values. Shifts are `<<`, `>>`, and `>>>`. Assignment operators include simple `=` and compound forms such as `+=` and `&=`.

The conditional operator `condition ? whenTrue : whenFalse` evaluates exactly one branch. Its result type follows detailed rules involving numeric constants, boxing, reference least-upper-bound analysis, and `null`; do not assume it is simply the type of one textual branch.

### Evaluation and short-circuiting

```java
static boolean valid(String value) {
    return value != null && !value.isBlank();
}
```

If `value` is `null`, `value != null` is false and `isBlank` is never called. Replacing `&&` with `&` would evaluate it and throw. Short-circuit operators also encode ordering, so avoid hiding expensive or state-changing operations in their right operands.

Java evaluates the target and arguments of a method invocation from left to right before calling the method. In `array[index()] += delta()`, the array reference and index are evaluated once, then the current element and right operand are combined.

### Branches

An `if` condition must be `boolean` or `Boolean` subject to unboxing; Java does not treat integers or arbitrary references as truthy. Braces prevent misleading indentation and reduce maintenance risk.

Classic `switch` statements use colon labels and can fall through. Arrow labels do not fall through. A `switch` expression produces a value and must be exhaustive.

```java
enum Tier { FREE, STANDARD, PREMIUM }

static int retentionDays(Tier tier) {
    return switch (tier) {
        case FREE -> 7;
        case STANDARD -> 30;
        case PREMIUM -> 365;
    };
}
```

For an enum selector, covering every declared constant makes this source exhaustive. A compiler-generated defensive path can still throw if separately compiled code supplies a newer enum constant at runtime. `switch` on traditional supported reference selectors throws `NullPointerException` for `null` unless modern pattern-switch labels explicitly handle `null`.

A block arm in a switch expression uses `yield` to provide its value:

```java
int cost = switch (tier) {
    case FREE -> 0;
    case STANDARD -> {
        audit("standard pricing");
        yield 20;
    }
    case PREMIUM -> 50;
};
```

### Loops and transfer

`while` checks before an iteration. `do-while` runs the body once before checking. A basic `for` has initialization, condition, and update components. Enhanced `for` delegates traversal to an iterator for `Iterable` objects or uses array indexing semantics for arrays.

```java
static int firstAtLeast(int[] values, int threshold) {
    for (int i = 0; i < values.length; i++) {
        if (values[i] >= threshold) {
            return i;
        }
    }
    return -1;
}
```

`break` exits the nearest loop or switch statement. `continue` moves to the next loop iteration; in a basic `for`, the update expression still runs. A labeled `break` or `continue` can target an enclosing statement or loop, but extraction into a method is often clearer.

`return` exits a method, and `throw` propagates an exception. A `finally` block runs during most abrupt transfers, but a `return` or `throw` from `finally` can replace the original outcome and should be avoided.

## Worked Java example

The following retry policy combines an exhaustive switch expression with a loop whose termination is explicit.

```java
import java.time.Duration;

public class RetryPolicy {
    enum Result { SUCCESS, RETRYABLE, PERMANENT_FAILURE }

    interface Operation {
        Result run(int attempt);
    }

    static boolean execute(int maxAttempts, Operation operation) {
        if (maxAttempts < 1) {
            throw new IllegalArgumentException("maxAttempts must be positive");
        }

        for (int attempt = 1; attempt <= maxAttempts; attempt++) {
            Result result = operation.run(attempt);
            boolean stop = switch (result) {
                case SUCCESS -> true;
                case PERMANENT_FAILURE -> true;
                case RETRYABLE -> attempt == maxAttempts;
            };

            if (stop) {
                return result == Result.SUCCESS;
            }
            Duration delay = Duration.ofMillis(100L * attempt);
            System.out.println("retry after " + delay);
        }
        throw new AssertionError("loop is exhaustive");
    }

    public static void main(String[] args) {
        boolean ok = execute(3, attempt ->
                attempt < 3 ? Result.RETRYABLE : Result.SUCCESS);
        System.out.println(ok);
    }
}
```

The last throw documents an unreachable condition based on the validated positive limit and loop exits. In a real service, sleeping should respect interruption and policy jitter; the example focuses on control flow.

## Execution or memory walkthrough

`maxAttempts` is validated once. Iteration one invokes the operation with `1`, which returns `RETRYABLE`. The switch selects its third arm; `attempt == maxAttempts` is false, so `stop` is false. A 100 millisecond duration is printed.

Iteration two follows the same path and prints 200 milliseconds. The for-update increments `attempt` after each normally completed body. Iteration three returns `SUCCESS`; the first switch arm yields true. The `if` executes and returns `true`, so neither the update nor another condition check runs.

At every loop entry, the invariant is `1 <= attempt <= maxAttempts`. Progress is the increment of `attempt`, and the upper bound proves termination. Only one switch arm executes on each iteration.

## Complexity and performance

Expression operators on fixed-width primitives are O(1). A branch is O(1), while loop cost is the iteration count multiplied by body cost. The example makes at most `maxAttempts` operation calls, so it is O(n) time and O(1) extra space, excluding work performed by the operation.

Branch shape can influence CPU prediction, and switch compilation may use tables, lookups, or comparison chains. Such choices are implementation details and rarely justify distorting domain code. First minimize unnecessary work and choose an appropriate algorithm; then profile representative loads.

> **HotSpot note:** HotSpot may inline branches, eliminate unreachable paths, unroll loops, or compile a switch to different machine structures based on profiling. Source semantics and exception behavior remain the contract.

## Edge cases and common mistakes

- `a < b < c` is illegal; the first comparison is boolean. Write `a < b && b < c`.
- `if (flag = true)` assigns and then tests. Prefer `if (flag)` and enable static analysis.
- `&` and `|` do not short-circuit boolean operands.
- Post-increment returns the previous value: `a[i++] = i` is legal but hard to review.
- A missing `break` in a colon-style switch falls through, sometimes intentionally and often accidentally.
- A semicolon after `if` or `while` creates an empty controlled statement.
- Mutating a collection structurally during enhanced-for traversal usually triggers fail-fast behavior; use the iterator's removal operation or another strategy.
- Comparing references with `==` does not compare object content.
- Floating-point NaN makes all relational comparisons false, including both `x < y` and `x >= y`.
- A `continue` can skip state updates placed in the loop body and create nontermination.
- Deep labels often signal that the body should become a named method.

## Production engineering notes

Keep decision tables centralized. An exhaustive enum switch can make a missing business state a compiler concern, but account for enum evolution across independently deployed components. For workflows with many transitions, model legal state transitions explicitly rather than building a maze of conditionals.

Separate predicates from side effects. A name such as `isEligible` should not write to a database. This makes short-circuiting safe to understand and lets tests cover decision logic deterministically.

Loops over external work need budgets: maximum attempts, deadlines, pagination termination, cancellation, and metrics. Check thread interruption when doing blocking or long-running work. In retry code, distinguish an attempt count from a retry count and test off-by-one boundaries.

## Interview questions and model answers

**What is the difference between precedence and evaluation order?**

Precedence decides how tokens group, as multiplication grouping before addition. Java then evaluates operand expressions left to right. Parentheses can change grouping but do not authorize arbitrary operand reordering.

**When should `&` be used with booleans?**

Only when both boolean operands intentionally must be evaluated, which is uncommon in application predicates. `&&` is usually safer because it encodes conditional evaluation. `&` remains useful as a bitwise operator for integral masks.

**How is a switch expression different from a classic switch statement?**

A switch expression produces a value, must be exhaustive, and commonly uses non-fall-through arrow arms. A classic colon-style statement controls execution and may fall through. A block expression arm supplies its result with `yield`.

**How do you prove a loop terminates?**

State an invariant and a bounded progress measure. For an index loop, show that the index begins in range, advances on every continuing path, and is bounded by a finite length. External loops also need a deadline or other finite budget.

**Does `finally` always execute?**

It executes for normal and most abrupt completion of its `try`, but not if the JVM process terminates or cannot continue, such as `System.exit`, a crash, or forced kill. It is also dangerous for `finally` to return because that replaces an earlier return or exception.

## Exercises

1. Trace `int x = 1; int y = x++ + ++x;` using left-to-right evaluation. Then rewrite it for clarity.
2. Convert a fall-through day-category switch into an exhaustive switch expression.
3. State an invariant and progress measure for binary search.
4. Implement pagination that stops on an empty page, a repeated cursor, a maximum page count, or interruption.
5. Explain why `user != null & user.active()` is unsafe and whether changing to `&&` fully solves the design problem.
6. Write a truth table for `&&`, `||`, `^`, and equality on booleans, noting which operands execute.

## Chapter summary

Java expressions follow fixed grouping, conversion, left-to-right evaluation, and short-circuit rules. Statements organize those expressions into branches, loops, and abrupt transfers. Modern switch expressions make value mapping and exhaustiveness explicit. Reliable iteration requires an invariant, a progress measure, and a finite budget. Readable control flow minimizes hidden side effects and makes every exit path intentional.

## Revision checklist

- [ ] I distinguish precedence, associativity, and evaluation order.
- [ ] I know which boolean operators short-circuit.
- [ ] I avoid mutation hidden inside complex expressions.
- [ ] I can use switch expressions, arrow arms, and `yield`.
- [ ] I understand fall-through and null selector behavior.
- [ ] I can state a loop invariant and prove termination.
- [ ] I understand `break`, `continue`, `return`, and `throw` as abrupt completion.
- [ ] I place explicit budgets around retries and external iteration.
