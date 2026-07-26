# Solution Studio A - Language and Execution

Use these after attempting Practice Lab A. Each answer names the rule, because an unexplained answer is not interview-ready.

**How to review:** compare your rule before comparing your final value. Mark a response correct only if both match. For each miss, retype the smallest compiling example and change one operand, boundary, or control-flow condition to prove that the rule transfers. The grouped coding guidance is intentionally not a paste-ready answer: implement it, compile it, and defend the contract yourself.

## Knowledge answers K01-K25

- **K01:** Source is human-authored `.java`; `javac` emits class-file bytecode; a compatible JVM executes the class-file operations on the host.
- **K02:** JDK is the development kit; a runtime/JRE is JVM plus runtime libraries/support; JVM executes class files. Modern packaging may not expose a separate JRE product.
- **K03:** `public` accessible, `static` no receiver required, `void` no result, `main` launcher entry name, `String[]` text argument array, `args` parameter name.
- **K04:** A public top-level class must normally use a same-named source file so the compiler/source lookup contract is satisfied.
- **K05:** Declaration introduces a variable; initialization gives its first value; assignment stores a value; reassignment replaces a prior value when permitted.
- **K06:** Java's definite-assignment analysis rejects a local read unless every reachable path assigns it; locals do not receive field defaults.
- **K07:** Integral: byte, short, int, long, char. Floating: float, double. boolean is logical.
- **K08:** The language fixes primitive value sets and numeric behavior, while field padding, object headers, alignment, and optimized representation are JVM/layout concerns.
- **K09:** A primitive variable stores its primitive value; a reference variable stores a reference value that may designate an object or be null.
- **K10:** Every argument expression produces a value copied into a parameter. For objects that value is a reference value, so mutation can be shared but parameter reassignment cannot escape.
- **K11:** `char` is an unsigned 16-bit UTF-16 code unit from 0 through 65535, not necessarily a full code point or glyph.
- **K12:** Expression type is decided before assignment; two int operands produce an int product, which may overflow before conversion to long.
- **K13:** Widening moves to a type whose range/representation is accepted implicitly; narrowing needs a cast and can discard range or precision.
- **K14:** Each byte promotes to int, so byte addition produces int.
- **K15:** Integer division truncates toward zero; remainder satisfies `a == (a / b) * b + a % b` and follows the dividend's sign when nonzero.
- **K16:** `&&` short-circuits after false; boolean `&` evaluates both operands.
- **K17:** `x += y` behaves like `x = (typeOfX) (x + y)` with one evaluation of the left side, so a narrowing conversion may be implicit.
- **K18:** Prefix updates then yields the new value; postfix yields the old value then updates.
- **K19:** A guard clause is clearer when invalid/special cases can exit early and keep the main path shallow.
- **K20:** Colon cases can fall through; arrow switch expression arms do not fall through and produce a value or complete an action.
- **K21:** `for` for explicit counter/update, `while` for condition-driven repetition, `do-while` for at least one pass, enhanced-for for value traversal without index mutation.
- **K22:** Unlabeled break exits only the nearest loop or switch; use a label or helper return deliberately for outer exit.
- **K23:** Name plus parameter types distinguish overloads. Return type is not part of overload selection at a call expression.
- **K24:** Base case stops recursion; recursive case reduces the problem toward it. Without progress, frames grow until StackOverflowError.
- **K25:** Class files are portable across compatible JVMs, but native libraries, OS services, paths, charsets, resources, timing, and environment remain host-dependent.

## Output answers O01-O19

- **O01:** `2`; both operands are int, so the fractional part is discarded.
- **O02:** `-1`; remainder follows the dividend and preserves the division identity.
- **O03:** `-126`; 130 modulo 256 maps to that signed byte value.
- **O04:** `-2147483648`; overflow occurs as int before widening to long.
- **O05:** `3 4`; postfix yields 3, then x becomes 4.
- **O06:** `4 4`; prefix updates before yielding.
- **O07:** `15`; compound assignment includes the required narrowing cast.
- **O08:** `7`; digit code units are consecutive.
- **O09:** `66`; char participates in int arithmetic.
- **O10:** `false`; `fail()` is skipped by short-circuiting.
- **O11:** `0`; true on the left skips `++x`.
- **O12:** `2 1 0`; the condition includes zero and the decrement then reaches -1.
- **O13:** `1`; do-while runs once before testing.
- **O14:** `AB`; case 1 falls into case 2 without break.
- **O15:** `5`; arguments 2 and 3 are added and returned.
- **O16:** Caller primitive unchanged; only the copied parameter value increments.
- **O17:** Field changed; both reference values designate the same mutable object.
- **O18:** Caller reference unchanged; only the local parameter is reassigned.
- **O19:** Value `1.0`, type `double`; numeric conditional typing promotes the int arm.

## Debug answers D01-D19

- **D01:** Add `;`; Java statements require termination.
- **D02:** Initialize `total` on every path before read, usually `int total = 0;` for an accumulator.
- **D03:** Rename the file `Main.java` or make the top-level class non-public/rename it to `Program`.
- **D04:** `long area = (long) width * height;`; widen before multiplication.
- **D05:** Use `value++`, `value += 1`, or an explicit checked/narrowing conversion; plain addition produces int.
- **D06:** Use `(double) sum / count` after checking `count != 0`.
- **D07:** Use `&&`; it skips the dereference when text is null.
- **D08:** Use `==`; assignment is not a boolean expression for int.
- **D09:** Use `i < array.length`; array indexes form a half-open interval.
- **D10:** Start at `array.length - 1` and continue while `i >= 0`.
- **D11:** Change the update or condition so a well-founded measure approaches the exit.
- **D12:** Add `break`/return/throw or use arrow switch syntax if the target Java version supports it.
- **D13:** Use an indexed loop or maintain an explicit, correctly updated counter.
- **D14:** Change parameter lists or method names; return type alone cannot overload.
- **D15:** Cast null to the intended parameter type or redesign ambiguous overloads.
- **D16:** Return on every reachable path or declare `void` if no value is part of the contract.
- **D17:** Add a reachable base case and make every recursive call reduce toward it.
- **D18:** `long magnitude = Math.abs((long) value);`; widening before abs makes 2147483648 representable.
- **D19:** Validate each failure with an early return/throw, then leave one shallow success path; verify original exception/result behavior with tests.

## Coding guidance C01-C19

- **C01-C04:** Define null/empty/negative contracts first, then use one clear branch chain and test each boundary.
- **C05-C08:** Write the loop interval and invariant before code; test empty, singleton, first, and last positions.
- **C09:** Cast an operand before multiplication. **C10:** compare against `Integer.MIN_VALUE/MAX_VALUE` or use `Math.toIntExact`.
- **C11-C12:** The colon switch needs explicit completion; the arrow switch should return/assign one value per arm and label its Java version.
- **C13-C14:** Validate dimensions/bounds; overloading requires distinct parameter types and should preserve one conceptual contract.
- **C15-C17:** Assert caller state after primitive copy, shared mutation, and reference reassignment; the distinction is the copied value plus aliasing.
- **C18:** Check `character >= '0' && character <= '9'` or use `Character.digit` and reject `-1`.
- **C19:** Extract named predicates/helpers only when they clarify the contract; preserve failure order and output with tests.

## Follow-up guidance F01-F13

- **F01-F02:** Separate source compatibility, class-file level, runtime linkage, dependency compatibility, and behavior; `--release` constrains language/API signature set, not third-party or environment behavior.
- **F03-F05:** Make numeric and evaluation policy explicit; parentheses communicate intent and short-circuiting prevents invalid/expensive work.
- **F06-F07:** Name a non-negative measure that strictly decreases. Prefer a helper return when a label would obscure ownership of the exit.
- **F08-F09:** Recompilation can select a new overload or become ambiguous; varargs is an array parameter with call-site packing syntax.
- **F10:** Recursion depth depends on frames, JVM configuration, and execution; the language promises no fixed safe depth.
- **F11:** Test odd sums, negative values, zero count, and large totals.
- **F12-F13:** Use compiler diagnostics, thrown stack traces, wrong outputs, and environment evidence rather than guesses.

**A01 model:** A strong answer widens before arithmetic, rejects invalid tokens at the input boundary, never catches unrelated runtime defects, uses a single pass, and tests no args, one value, negatives, zero, extreme ints, malformed text, and product overflow policy.
