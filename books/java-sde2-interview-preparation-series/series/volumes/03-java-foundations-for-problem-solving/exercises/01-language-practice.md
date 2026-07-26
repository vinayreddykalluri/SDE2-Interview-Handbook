# Practice Lab A - Language and Execution

Attempt these without the solution guide. Record both your answer and the rule that supports it.

**Start here:** work in four short passes instead of treating this as one exam. First answer K01-K25 aloud. Next predict O01-O19 before running anything. Then repair D01-D19 in a scratch file and compile each fix. Finish by implementing C01-C19 and discussing F01-F13 with explicit contracts and edge cases.

Keep an error log with three columns: the mistaken prediction, the Java rule that corrects it, and one boundary input that exposes the mistake. A correct answer without the rule is recall; a correct answer with the rule is reusable interview reasoning. Use Solution Studio A only after an honest attempt, then retry missed items the next day without looking.

## Knowledge check

- **K01 [Foundation]** Distinguish source code, bytecode, and machine execution.
- **K02 [Foundation]** Define JDK, runtime/JRE, and JVM without treating them as synonyms.
- **K03 [Foundation]** Explain each token in `public static void main(String[] args)`.
- **K04 [Foundation]** When must a public top-level class match its filename?
- **K05 [Foundation]** Distinguish declaration, initialization, assignment, and reassignment.
- **K06 [Foundation]** Why must a local variable be definitely assigned before reading?
- **K07 [Foundation]** List Java's eight primitive types and identify integral versus floating types.
- **K08 [Interview Core]** Why does primitive bit width not prove total object or field-layout size?
- **K09 [Foundation]** Distinguish a primitive value from a reference value.
- **K10 [Interview Core]** Explain why Java is always pass-by-value.
- **K11 [Foundation]** What is the type and range role of `char`?
- **K12 [Interview Core]** Why can `long result = intA * intB` still overflow?
- **K13 [Foundation]** Distinguish widening and narrowing conversion.
- **K14 [Interview Core]** Describe binary numeric promotion for `byte + byte`.
- **K15 [Foundation]** Explain integer division and remainder with negative operands.
- **K16 [Foundation]** Distinguish `&&` from boolean `&`.
- **K17 [Interview Core]** What hidden cast does compound assignment provide?
- **K18 [Foundation]** Distinguish prefix from postfix increment.
- **K19 [Foundation]** When is a guard clause clearer than nested `if` blocks?
- **K20 [Foundation]** Distinguish traditional switch fall-through from a switch expression.
- **K21 [Foundation]** Choose among `for`, `while`, `do-while`, and enhanced-for.
- **K22 [Interview Core]** What does `break` affect in nested loops?
- **K23 [Interview Core]** What makes an overload distinct, and why is return type insufficient?
- **K24 [Interview Core]** State the base case and recursive case in a recursive method preview.
- **K25 [SDE-2 Follow-up]** Explain the limits of Java's platform-independence claim.

## Predict the output

- **O01 [Foundation]** Predict `System.out.println(5 / 2);`.
- **O02 [Foundation]** Predict `System.out.println(-5 % 2);`.
- **O03 [Interview Core]** Predict `(byte) 130` printed as an integer.
- **O04 [Interview Core]** Predict `long x = Integer.MAX_VALUE + 1; System.out.println(x);`.
- **O05 [Foundation]** Predict `int x=3; System.out.println(x++ + " " + x);`.
- **O06 [Foundation]** Predict `int x=3; System.out.println(++x + " " + x);`.
- **O07 [Interview Core]** Predict `short x=10; x+=5; System.out.println(x);`.
- **O08 [Foundation]** Predict `System.out.println('7' - '0');`.
- **O09 [Interview Core]** Predict `System.out.println('A' + 1);`.
- **O10 [Foundation]** Predict `boolean x=false; System.out.println(x && fail());` assuming `fail` would throw.
- **O11 [Interview Core]** Predict `int x=0; if (true || ++x>0){} System.out.println(x);`.
- **O12 [Foundation]** Predict a loop `for(int i=2;i>=0;i--) print(i)`.
- **O13 [Foundation]** Predict `int i=0; do { i++; } while(false); print(i)`.
- **O14 [Interview Core]** Predict a traditional switch with case 1 printing A and falling into case 2 printing B.
- **O15 [Foundation]** Predict `sum(2, 3)` when the method returns `left + right`.
- **O16 [Interview Core]** Predict a caller's primitive after a method increments only its parameter.
- **O17 [Interview Core]** Predict an object's field after a method mutates it through a copied reference.
- **O18 [Interview Core]** Predict a caller reference after a method reassigns only its parameter.
- **O19 [SDE-2 Follow-up]** Predict the evaluation result of `true ? 1 : 2.0` and state its type.

## Debug the code

- **D01 [Foundation]** Repair a missing semicolon after `int count = 3`.
- **D02 [Foundation]** Repair reading an uninitialized local `int total;`.
- **D03 [Foundation]** Repair a public class `Main` stored in `Program.java` without renaming the file.
- **D04 [Interview Core]** Repair `long area = width * height` for two arbitrary `int` inputs.
- **D05 [Interview Core]** Repair `short value = value + 1`.
- **D06 [Foundation]** Repair an average computed with integer division.
- **D07 [Interview Core]** Repair a null check that uses `text != null & !text.isEmpty()`.
- **D08 [Foundation]** Repair `if (score = 100)`.
- **D09 [Foundation]** Repair `for (int i=0; i<=array.length; i++)`.
- **D10 [Interview Core]** Repair a reverse loop that starts at `array.length`.
- **D11 [Foundation]** Repair a loop whose update moves away from its termination condition.
- **D12 [Interview Core]** Repair accidental switch fall-through.
- **D13 [Foundation]** Repair an enhanced-for loop that tries to use an undeclared index.
- **D14 [Interview Core]** Repair an overload pair differing only by return type.
- **D15 [Interview Core]** Repair an ambiguous `print(null)` call with `String` and `StringBuilder` overloads.
- **D16 [Foundation]** Repair a method declared `int` that reaches the end without returning.
- **D17 [Interview Core]** Repair recursion with no reachable base case.
- **D18 [Interview Core]** Repair `Math.abs(Integer.MIN_VALUE)` when a non-negative long magnitude is required.
- **D19 [SDE-2 Follow-up]** Repair a deeply nested validator using early returns while preserving behavior.

## Small coding tasks

- **C01 [Foundation]** Print a greeting with a default name when no command-line argument is supplied.
- **C02 [Foundation]** Return the larger of two integers without using `Math.max`.
- **C03 [Foundation]** Classify an integer as negative, zero, or positive.
- **C04 [Foundation]** Return whether a year is a leap year.
- **C05 [Foundation]** Sum integers from 1 through `n` with a loop.
- **C06 [Foundation]** Traverse an array forward and print values.
- **C07 [Foundation]** Traverse an array backward.
- **C08 [Foundation]** Count values at even indexes.
- **C09 [Interview Core]** Compute a safe `long` product of two `int` values.
- **C10 [Interview Core]** Implement checked narrowing from `long` to `int`.
- **C11 [Foundation]** Implement a calculator with traditional switch and explicit breaks.
- **C12 [Foundation]** Implement the same calculator with a Java 14+ switch expression.
- **C13 [Foundation]** Write a method with parameters and a return value for rectangle area.
- **C14 [Interview Core]** Overload `clamp` for `int` and `long`.
- **C15 [Interview Core]** Demonstrate primitive pass-by-value with a caller assertion.
- **C16 [Interview Core]** Demonstrate shared object mutation through a parameter.
- **C17 [Interview Core]** Demonstrate why reference reassignment does not escape a method.
- **C18 [Interview Core]** Convert an ASCII digit only after validation.
- **C19 [SDE-2 Follow-up]** Refactor one large input-validation method into guard clauses and small helpers.

## Interview follow-ups

- **F01 [Interview Core]** What changes when a compiler accepts source but runtime dependencies are incompatible?
- **F02 [SDE-2 Follow-up]** What does `--release 17` constrain and what does it not prove?
- **F03 [Interview Core]** When should overflow wrap, throw, saturate, or widen?
- **F04 [Interview Core]** Why is memorizing the full precedence table less useful than parentheses?
- **F05 [Interview Core]** Explain `&&` as both a correctness and cost tool.
- **F06 [SDE-2 Follow-up]** Defend a loop termination proof using a decreasing measure.
- **F07 [Interview Core]** When is a labeled break clearer than a helper method with early return?
- **F08 [Interview Core]** How can adding an overload break source compatibility?
- **F09 [Interview Core]** Why is varargs represented as an array inside the method?
- **F10 [SDE-2 Follow-up]** Explain StackOverflowError without claiming a fixed recursion depth.
- **F11 [Interview Core]** Which input boundaries expose integer-division mistakes?
- **F12 [SDE-2 Follow-up]** What evidence separates a compile-time, runtime, and logical defect?
- **F13 [SDE-2 Follow-up]** Explain a platform-dependent behavior that valid Java bytecode cannot hide.

## Cumulative assessment 1

**A01:** In 35 minutes, write and explain a program that parses command-line integers, validates them, computes a safe `long` sum and product, reports negative/zero/positive counts, and avoids overflow-before-widening. Include six tests and classify every possible failure stage.
