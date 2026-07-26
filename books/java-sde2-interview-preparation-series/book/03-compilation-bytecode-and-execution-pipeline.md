# Chapter 3: Compilation, Bytecode, and the Execution Pipeline

## Learning objectives

- Trace a Java program from source characters to CPU instructions.
- Explain lexical analysis, parsing, type checking, class-file generation, loading, linking, and initialization.
- Read representative output from `javap -c`.
- Distinguish symbolic bytecode from resolved runtime structures and native code.
- Explain why an IDE Run action is orchestration rather than a separate Java execution model.

## Why this matters at SDE-2

This pipeline locates failures. A syntax error, linkage error, initialization failure, verifier error, JIT deoptimization, and native crash occur in different layers and require different evidence. SDE-2 interviews also expect more than "Java compiles to bytecode": candidates should describe when types are checked, how symbolic references are resolved, why warm-up exists, and how a source line may map to multiple execution forms.

## First-principles model

A compiler converts one representation into another while preserving specified meaning. `javac` is primarily an ahead-of-time source-to-class-file compiler. The JVM then performs dynamic work that cannot or need not be finalized at source compilation time.

```text
.java text
  -> tokens
  -> syntax tree
  -> attributed/type-checked program
  -> class-file structures and bytecode
  -> loaded Class objects and metadata
  -> verified, prepared, optionally resolved classes
  -> initialized class
  -> interpreted/compiled execution
  -> native instructions on a CPU
```

There can be other paths: annotation processors can generate source; alternative compilers can emit valid class files; JVM languages can target bytecode; and ahead-of-time tools can build native executables. None changes the conceptual contract between valid class files and a conforming JVM.

## Core terminology

- **Lexing:** Grouping characters into tokens such as identifiers, literals, and operators.
- **Parsing:** Checking grammatical structure and constructing a syntax tree.
- **Attribution/type checking:** Resolving names and validating types, overloads, access, and language rules.
- **Class file:** A binary structure containing a version, constant pool, fields, methods, attributes, and bytecode.
- **Constant pool:** A per-class table of literals and symbolic references used by class-file structures.
- **Descriptor:** A compact JVM encoding of field or method types, such as `(II)I`.
- **Verification:** Checking structural and type-safety constraints before unsafe bytecode can execute.
- **Resolution:** Converting a symbolic reference into a concrete runtime entity.
- **Interpreter:** Executes bytecode according to its meaning without first producing a fully optimized method body.
- **JIT compiler:** Compiles selected runtime code to native instructions.
- **Deoptimization:** Transfers execution from optimized code when an assumption is invalidated.

## Detailed mechanics

The compiler first recognizes Unicode input according to Java's lexical rules. It forms tokens, parses declarations and expressions, resolves imported and qualified names, infers applicable generic types, chooses overloads, performs definite-assignment checks, and reports errors. Some constructs are translated into lower-level class-file patterns. For example, an enhanced `for` loop becomes index-based or iterator-based logic, and string concatenation can use an invokedynamic-based recipe in modern compilers.

Compilation does not load every application class in the same sense as the runtime class-loader subsystem. It reads declarations needed to check code. Successful compilation also does not prove the runtime classpath contains compatible definitions.

A class file contains methods expressed in an operand-stack instruction set. Many Java source variables map to local-variable slots; expressions push and consume operand-stack values. Object member references are symbolic constant-pool entries until runtime resolution associates them with actual definitions.

Loading finds bytes and creates the runtime representation. Linking includes verification, preparation, and resolution. Preparation allocates static storage and gives fields their default values; initialization later executes class initialization code. Resolution may be eager or lazy within JVMS constraints.

> **Specification boundary:** The JVMS defines class-file validity, instruction behavior, loading/linking/initialization constraints, and observable error conditions. It does not require an interpreter, a particular JIT compiler, a hotness threshold, or one-to-one mapping from bytecode to machine instructions.

At invocation, a JVM may interpret a method, compile it, invoke a precompiled form, or combine strategies. Adaptive JVMs gather profiles such as branch frequencies and receiver types. Optimized native code can inline methods and eliminate allocations based on assumptions. If a newly loaded subclass or uncommon branch invalidates an assumption, the runtime can deoptimize to a less specialized form while preserving Java semantics.

An IntelliJ IDEA Run action ordinarily performs orchestration:

1. Save or use the in-memory project state according to settings.
2. Invoke a compiler or incremental build.
3. Construct an output directory/module path/classpath.
4. Build a command containing a selected JDK launcher, JVM options, main class, and arguments.
5. Start an operating-system process and connect console/debug channels.
6. The Java launcher creates the JVM and locates `main`.

The IDE does not bypass class loading or cause the CPU to execute Java source directly.

## Worked Java example

```java
public final class BytecodeTour {
    static int max(int left, int right) {
        return left >= right ? left : right;
    }

    public static void main(String[] args) {
        int answer = max(4, 9);
        System.out.println(answer);
    }
}
```

Run:

```bash
javac -g BytecodeTour.java
javap -c -p BytecodeTour
```

Representative output for `max` is conceptually:

```text
0: iload_0
1: iload_1
2: if_icmplt 9
5: iload_0
6: goto 10
9: iload_1
10: ireturn
```

`iload_0` pushes the integer in local slot 0. `if_icmplt` pops two integers and branches if the first is less than the second. One branch pushes `left`, the other pushes `right`, and `ireturn` returns the top integer. Actual offsets or branch shape may differ by compiler release while behavior remains the same.

Representative `main` instructions include `iconst_4` or a constant push, `bipush 9`, `invokestatic` for `max`, `istore_1`, `getstatic` for `System.out`, `iload_1`, and `invokevirtual` for `println`.

## Execution or memory walkthrough

For `main`:

1. The JVM creates a frame whose local slots include `args` and later `answer`.
2. Constants 4 and 9 are pushed on `main`'s operand stack.
3. `invokestatic` invokes `max`; a new frame receives the arguments in local slots 0 and 1.
4. `max` compares them and returns 9. Its frame is removed.
5. The value 9 appears as the invocation result on `main`'s operand stack, then is stored in `answer`'s slot.
6. `getstatic` obtains the `PrintStream` reference represented by `System.out`.
7. The receiver and integer argument are placed on the operand stack and `println(int)` is invoked.
8. `return` completes `main`; non-daemon thread state then determines process lifetime.

Resolution may occur when an instruction first uses `System.out`, `println`, or `max`, or earlier. Initialization of `BytecodeTour` must occur before its active use, though it has no explicit static initializer.

```text
Source local       JVM frame representation       Possible native form
answer             local variable slot            register, stack location,
                                                or optimized away
```

Debug metadata can preserve source names and line mappings, but the runtime does not need a source-level local variable object.

## Complexity and performance

`max` is O(1) time and O(1) auxiliary space. Bytecode instruction count is not a stable performance measure. One instruction can have complex effects; JIT compilation may inline the entire method; and machine code depends on profiles and CPU.

Compilation time, startup time, warm-up time, and steady-state time are distinct. Incremental IDE builds reduce compilation work. Class-data sharing, lazy class loading, JIT activity, and library initialization affect startup. Hot methods can become faster after profiling, but compilation consumes CPU and code-cache space. Use a harness such as JMH for microbenchmarks rather than timing one invocation around `System.nanoTime()`.

> **HotSpot note:** HotSpot's tiered compilation commonly uses an interpreter and multiple compiled tiers, with C1 and C2 compilers in mainstream builds. Profiling counters, inlining policies, on-stack replacement, and deoptimization are HotSpot mechanisms and can change by version.

## Edge cases and common mistakes

- Treating `javac` as the only valid Java compiler or assuming bytecode uniquely identifies source.
- Assuming all type errors are caught at compile time. Separate compilation, raw types, reflection, malformed bytecode, and linkage can move failures later.
- Confusing the class-file constant pool with the runtime string pool.
- Saying resolution always happens completely at startup.
- Reading `javap` output as the final native instructions.
- Assuming local slot numbers equal physical CPU registers.
- Forgetting static initialization can throw `ExceptionInInitializerError`, followed by later `NoClassDefFoundError` for the erroneous class.
- Benchmarking IDE debug mode and attributing all overhead to Java.

## Production engineering notes

Preserve build provenance: compiler JDK, `--release`, dependency lock state, generated sources, and artifact digest. At runtime, capture the actual command, classpath/module path, flags, and environment. A failure that appears after a library upgrade may be a binary linkage issue even though the service compiled in another module.

Use `javap -c -p -s` to validate overload descriptors, bridge methods, or generated bytecode. Use `-verbose` for constant-pool and class-file metadata. For JIT questions, prefer JFR and supported diagnostics over conclusions drawn from source alone. Optimized code can omit allocations and frames that source structure suggests.

## Interview questions and model answers

**Describe Java execution from source to CPU.**

`javac` lexes, parses, resolves names, checks types, and emits class files. At runtime, class loaders find definitions; the JVM verifies, prepares, resolves, and initializes them under specified rules. The execution engine interprets or compiles bytecode. Native instructions then execute on the CPU, with runtime services handling calls, allocation, GC, synchronization, and deoptimization.

**Why does Java need bytecode verification if `javac` checked the source?**

The JVM may receive class files from any compiler or transformer, not necessarily trusted `javac`. Verification protects runtime invariants such as operand types, control-flow consistency, access, and valid initialization before unsafe operations execute.

**What is in the constant pool?**

It is a class-file table containing constants and symbolic references used by the class. Runtime structures derive from it during loading and resolution. It is not merely a pool of Java `String` objects.

**Can the JIT change program behavior?**

It may change implementation and timing but must preserve allowed Java behavior. For data-racy programs, the JMM may permit multiple outcomes, so optimization can expose an already legal outcome rather than violate semantics.

## Exercises

1. Compile the example and annotate every `main` instruction from `javap -c`.
2. Add an instance method and compare `invokevirtual` with `invokestatic`.
3. Add a static initializer that prints a message; predict and observe when it runs.
4. Compile with and without `-g`, then compare `javap -l -v` output.
5. Break runtime binary compatibility by compiling against a method and removing it from the runtime class; identify the error phase.
6. Draw the complete timeline for your IDE's Run command, including OS process creation.

## Chapter summary

Java execution is a sequence of representations and validation boundaries. Source compilation produces specified class-file structures and stack-oriented bytecode. Runtime loading, linking, initialization, and execution connect symbolic definitions to live classes and host instructions. Interpretation and JIT compilation are implementation strategies; optimized native state can differ radically from source structure while preserving specified behavior.

## Revision checklist

- [ ] I can name the major compiler front-end phases.
- [ ] I can explain class files, descriptors, and the constant pool.
- [ ] I can read loads, stores, branches, field access, calls, and returns in `javap -c`.
- [ ] I can distinguish loading, linking, initialization, interpretation, and JIT compilation.
- [ ] I can explain an IDE Run action through to CPU execution.
- [ ] I do not confuse bytecode with native code or JVM implementation policy with the JVMS.

