# Java and Program Foundations

## Learning objectives

By the end of this chapter, you can explain why Java is useful in coding interviews, trace a source file through compilation and execution, read the smallest complete program, and distinguish compile-time, runtime, and logical failures.

## Why Java works well for DSA interviews

Java makes many contracts visible. A variable has a declared type, array indexes are checked, and the standard library provides lists, sets, maps, queues, deques, heaps, sorting, parsing, and numeric utilities. Automatic memory management removes manual deallocation from ordinary interview code. These properties let a candidate concentrate on the invariant and the algorithm while still writing code that is explicit enough to review.

The trade-off is ceremony. Java solutions are usually longer than equivalent Python solutions, primitive and reference behavior must be understood, and a careless choice such as `int` instead of `long` can silently overflow. Good interview Java is intentionally plain: familiar APIs, descriptive names, small helpers, explicit boundary handling, and no abstraction that does not clarify the solution.

Java is not "always faster," and platform independence is not absolute. `javac` normally produces platform-neutral class files, and a compatible JVM translates their operations for a host. Native libraries, file systems, default charsets, environment variables, timing, and resource limits can still make a program environment-dependent.

## Source, bytecode, and execution

```text
Hello.java source
       |
       | javac Hello.java
       v
Hello.class bytecode
       |
       | java Hello
       v
JVM execution on the host machine
```

- The **JDK** contains development tools such as `javac`, the `java` launcher, and a runtime.
- The **JVM** is the execution engine for class files.
- **JRE** remains useful as the conceptual name for a JVM plus the libraries and support files needed to run Java. Modern deployments often ship a JDK or a custom runtime image rather than a separately branded JRE.

Keep this mental model high level here. Class loading, JIT compilation, garbage collectors, and stack-frame internals belong in the JVM volume.

## The first complete program

File: `HelloInterview.java`

```java
public class HelloInterview {
    public static void main(String[] args) {
        System.out.println("Ready for Java");
    }
}
```

Expected output:

```text
Ready for Java
```

Compile and run:

```bash
javac HelloInterview.java
java HelloInterview
```

Read it from the outside inward:

1. `public class HelloInterview` declares a class named `HelloInterview`. A public top-level class normally lives in a file with the same name.
2. `{` and `}` delimit the class body and method body.
3. `main` is the entry method used by the launcher.
4. `public` lets the launcher access it.
5. `static` means the launcher does not need to construct `HelloInterview` first.
6. `void` means the method returns no value.
7. `String[] args` receives command-line arguments as an array of strings.
8. `System.out.println(...)` calls a method that writes a line.
9. The semicolon terminates the statement.

Do not memorize the signature as unexplained punctuation. Read it as: "an accessible class method named main, returning nothing, that accepts an array of text arguments."

## Statements, blocks, and comments

A **statement** performs an action. A **block** groups statements and creates a scope. Java ignores most whitespace, but consistent indentation exposes structure.

```java
int score = 72;                 // end-of-line comment
if (score >= 70) {              // block begins
    System.out.println("pass");
}

/* A block comment can span lines. */
```

Use comments to explain a decision or invariant, not to translate obvious syntax. In interviews, `sum += number; // add number to sum` adds noise; `// sum equals numbers[0..index)` records useful reasoning.

## Three failure stages

| Failure | When found | Example | Repair direction |
|---|---|---|---|
| Compile-time error | before a class file is produced | missing semicolon, incompatible type, uninitialized local | repair Java syntax or type rules |
| Runtime error | while the program executes | null dereference, invalid index, division by zero | validate state or correct the execution path |
| Logical error | program runs but result is wrong | loop skips the last element | repair the invariant, condition, or formula |

Compile-time-error demonstration:

```java
int answer;
System.out.println(answer); // local variable answer might not have been initialized
```

Corrected version:

```java
int answer = 0;
System.out.println(answer);
```

Fields receive specified default values when an object or class is initialized. Local variables do not; a local must be definitely assigned before it is read.

## A beginner execution dry run

```java
public class SumArguments {
    public static void main(String[] args) {
        int sum = 0;
        for (String argument : args) {
            sum += Integer.parseInt(argument);
        }
        System.out.println(sum);
    }
}
```

For `java SumArguments 4 7 9`:

1. `args` refers to `{"4", "7", "9"}`.
2. `sum` starts at 0.
3. Each string is parsed as an `int`; `sum` becomes 4, then 11, then 20.
4. The loop ends and the program prints `20`.

`Integer.parseInt` can throw `NumberFormatException`; later chapters define parsing and exception contracts. The point here is the execution sequence: initialize, repeat, update state, produce output.

## Interview angle and common mistakes

- Saying "write once, run anywhere" without the compatible-JVM and environment qualification.
- Calling bytecode machine code.
- Saying the JDK, JRE, and JVM are interchangeable names.
- Omitting the filename/class-name relationship for a public top-level class.
- Claiming local variables automatically start at zero.
- Copying a long `main` signature from memory without being able to explain each token.
- Adding console-input code when the interview platform already provides a method signature.

An interviewer may ask where a failure occurs. Answer with evidence: the compiler rejects an incompatible assignment; the JVM throws an exception on a reached bad operation; a wrong answer with no error is a logical defect.

## Quick check

1. What artifact does `javac` normally produce?
2. Why is `main` static?
3. Does platform independence guarantee identical operating-system behavior?
4. Why can a field be read at its default value while an unassigned local cannot?
5. Which failure category describes an off-by-one result with no exception?

## Small practice

**Foundation:** Write `Greeting.java` so `java Greeting Vinay` prints `Hello, Vinay`. Handle an empty `args` array.

**Interview Core:** Predict which of four lines in a supplied program fail at compilation, runtime, or logically, and justify the stage.

**SDE-2 Follow-up:** Explain what a build using JDK 21 and `javac --release 17` does and does not guarantee.

## Cross-book boundary

Use this chapter's execution model for the rest of Volume 03. Continue to the JVM volume for class loading, JIT, garbage collection, stack frames, and memory diagnostics. Continue to Advanced Java for modules, packaging, and language evolution in depth.

## Chapter summary

Java interview code is valuable because types and library contracts are explicit. Source is compiled to class files and executed by a compatible JVM; that portability has environmental limits. A complete program is a class containing an accessible static `main`, and failures must be located at compile time, runtime, or in logic before they can be repaired.
