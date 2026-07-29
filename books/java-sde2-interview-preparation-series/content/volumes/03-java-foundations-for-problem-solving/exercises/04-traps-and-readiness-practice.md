# Practice Lab D - Traps, Refactoring, and Readiness

**Start here:** this lab is a retrieval test across the full volume. Do not run output questions until a prediction and governing rule are written. For debugging items, preserve the intended contract, make the smallest correct change, and add a test that fails before the repair. For coding tasks, state time and auxiliary space after the code works.

Use three sessions: K76-K100 plus O58-O75, D58-D75 plus C58-C66, then C67-C75 plus F39-F50 and the assessments. Finish with the readiness rubric at the end of this chapter. If a miss comes from an earlier concept, return to that chapter instead of memorizing this lab's answer.

## Knowledge check

- **K76 [Foundation]** Distinguish reference identity from semantic equality.
- **K77 [Interview Core]** State what string interning does and does not guarantee.
- **K78 [Interview Core]** Why does explicit String construction affect identity but not content?
- **K79 [Interview Core]** Separate mutation through a copied reference from parameter reassignment.
- **K80 [Interview Core]** Why can overflow happen before a wider assignment?
- **K81 [Interview Core]** When does floating comparison require a domain tolerance?
- **K82 [Foundation]** Why does narrowing truncate rather than round?
- **K83 [Interview Core]** What is special about primitive minimum magnitudes?
- **K84 [Interview Core]** What is copied by an array clone?
- **K85 [Interview Core]** Why is Arrays.asList different for object and primitive arrays?
- **K86 [Foundation]** Why must String method results usually be captured?
- **K87 [Interview Core]** When is `Character.digit` safer than subtracting `'0'`?
- **K88 [Interview Core]** How does fall-through differ from multi-label switch cases?
- **K89 [Foundation]** Why can a constructor not declare a return type?
- **K90 [Interview Core]** What is the first-statement rule for constructor chaining?
- **K91 [Interview Core]** Which members are dynamically dispatched?
- **K92 [Interview Core]** Why is downcasting evidence of a possible contract gap?
- **K93 [Interview Core]** Why does catch ordering affect compilability?
- **K94 [SDE-2 Follow-up]** Name conditions under which `finally` may not complete normally.
- **K95 [Interview Core]** Why must hash keys remain equality-stable?
- **K96 [Interview Core]** Why must comparator logic avoid subtraction?
- **K97 [Interview Core]** What does fail-fast collection iteration guarantee and not guarantee?
- **K98 [Foundation]** State the half-open array-index interval.
- **K99 [Interview Core]** What progress measure proves a loop terminates?
- **K100 [SDE-2 Follow-up]** List the final review order for contract, type, state, control flow, API, and complexity.

## Predict the output

- **O58 [Interview Core]** Predict two equal new Strings under `==` and `equals`.
- **O59 [Interview Core]** Predict literal-plus-constant literal identity.
- **O60 [Interview Core]** Predict caller state after object mutation and parameter reassignment in one method.
- **O61 [Interview Core]** Predict `100_000 * 100_000` assigned to long.
- **O62 [Foundation]** Predict `-5 / 2` and `-5 % 2`.
- **O63 [Interview Core]** Predict `0.1 + 0.2 == 0.3`.
- **O64 [Foundation]** Predict `(int) -10.8`.
- **O65 [Interview Core]** Predict `Math.abs(Integer.MIN_VALUE)`.
- **O66 [Interview Core]** Predict a nested array mutation through a shallow copy.
- **O67 [Interview Core]** Predict add versus set on `Arrays.asList`.
- **O68 [Foundation]** Predict ignored `String.replace` result.
- **O69 [Interview Core]** Predict code-point count versus String length for a surrogate pair.
- **O70 [Foundation]** Predict traditional switch without breaks.
- **O71 [Interview Core]** Predict field and method access through parent reference/child object.
- **O72 [Interview Core]** Predict which overload runs for a base-typed argument.
- **O73 [Interview Core]** Predict a return in try replaced by return in finally.
- **O74 [Interview Core]** Predict map lookup after mutable-key mutation.
- **O75 [SDE-2 Follow-up]** Predict whether heap iteration order is guaranteed after seeing one sample run.

## Debug the code

- **D58 [Foundation]** Replace object `==` with the correct null-safe value comparison.
- **D59 [Interview Core]** Repair code whose correctness depends on string interning.
- **D60 [Interview Core]** Repair a swap method that tries to swap caller references.
- **D61 [Interview Core]** Repair multiplication widened only after overflow.
- **D62 [Interview Core]** Repair floating equality for a currency-domain value.
- **D63 [Foundation]** Repair narrowing code that intended rounding.
- **D64 [Interview Core]** Repair a hash index derived with `Math.abs(hash)`.
- **D65 [Interview Core]** Repair a matrix deep-copy routine.
- **D66 [Interview Core]** Repair `Arrays.asList(primitiveArray)` under a `List<Integer>` requirement.
- **D67 [Foundation]** Capture a String transformation result.
- **D68 [Interview Core]** Repair character logic under a Unicode code-point contract.
- **D69 [Foundation]** Repair accidental switch fall-through.
- **D70 [Foundation]** Add a missing no-argument constructor deliberately.
- **D71 [Interview Core]** Repair a constructor that calls `this` after validation code.
- **D72 [Interview Core]** Repair code expecting static method overriding.
- **D73 [Interview Core]** Repair an unsafe cast by changing the base contract.
- **D74 [Interview Core]** Repair a catch hierarchy and preserve specific handling.
- **D75 [SDE-2 Follow-up]** Repair a loop whose termination depends on eventually hitting exactly zero.

## Small coding tasks

- **C58 [Foundation]** Build a null-safe string equality helper.
- **C59 [Interview Core]** Demonstrate pool, constructor, and `intern` behavior without using identity for business logic.
- **C60 [Interview Core]** Build a pass-by-value diagram generator for one mutable object scenario.
- **C61 [Interview Core]** Implement safe average and safe product helpers.
- **C62 [Interview Core]** Implement domain-tolerant floating comparison with documented scale.
- **C63 [Interview Core]** Compute a non-negative bucket index without `Math.abs` overflow.
- **C64 [Interview Core]** Deep-copy a jagged object array under a stated element-copy contract.
- **C65 [Interview Core]** Convert a primitive array to a boxed resizable list.
- **C66 [Foundation]** Build text with StringBuilder using append, insert, delete, setCharAt, and reverse.
- **C67 [Interview Core]** Count Unicode code points without splitting surrogate pairs.
- **C68 [Foundation]** Rewrite a fall-through switch as an arrow switch expression.
- **C69 [Interview Core]** Demonstrate constructor chaining and initialization order.
- **C70 [Interview Core]** Demonstrate overload, override, field hiding, and static hiding separately.
- **C71 [Interview Core]** Implement safe polymorphic behavior without downcasting.
- **C72 [Interview Core]** Preserve a primary exception and inspect suppressed close failures.
- **C73 [Interview Core]** Create an immutable hash-map key and prove stable lookup.
- **C74 [Interview Core]** Poll a copy of a PriorityQueue into sorted order.
- **C75 [SDE-2 Follow-up]** Refactor a weak array-sum solution into interview-quality Java with contract, type choice, tests, and complexity.

## Interview follow-ups

- **F39 [Interview Core]** Which equality operation belongs to primitives, strings, arrays, and custom keys?
- **F40 [SDE-2 Follow-up]** How can a seemingly harmless refactor change compile-time overload selection?
- **F41 [Interview Core]** Which boundary tests expose overflow-before-widening?
- **F42 [SDE-2 Follow-up]** How do you choose integer units versus decimal arithmetic versus tolerance?
- **F43 [Interview Core]** When is a shallow copy the intended contract?
- **F44 [SDE-2 Follow-up]** How would you document Unicode assumptions in a string algorithm?
- **F45 [Interview Core]** When is traditional switch fall-through useful enough to keep?
- **F46 [SDE-2 Follow-up]** Why are fields not polymorphic even when methods are?
- **F47 [Interview Core]** What should a boundary catch log or translate without hiding?
- **F48 [SDE-2 Follow-up]** Explain how mutable-key failure follows from the equality/hash contract.
- **F49 [Interview Core]** What ordering does PriorityQueue promise at each operation?
- **F50 [SDE-2 Follow-up]** Give a five-minute verbal review of an interview solution before coding.

## Cumulative assessment 5

**A05:** In 60 minutes, diagnose a mixed program containing eight traps from this chapter, repair it, and explain why every corrected line follows Java's language or library contract. Add tests for null, empty, singleton, extremes, duplicates, and malformed input.

## Final readiness assessment

Complete without notes:

1. Explain source-to-execution, primitive/reference values, promotion, pass-by-value, equality, construction, dispatch, generics, collection choice, exception ownership, and I/O boundaries.
2. Implement one array/string task and one map/queue task in Java 21.
3. Predict ten outputs chosen randomly from O01-O75 with at least eight correct.
4. Repair ten defects chosen randomly from D01-D75 with the governing rule stated.
5. Complete five coding tasks chosen randomly from C01-C75 and explain time and auxiliary space.
6. Answer ten follow-ups chosen randomly from F01-F50 without relying on folklore.

Readiness standard: at least 80 percent on prediction/debugging, compiling solutions for all five coding tasks, no incorrect pass-by-reference or equality explanation, and explicit boundary tests. If numeric overflow is weak, return to Number Systems. If loop invariants are weak, continue with Loop Mastery after Study Step 02. Otherwise proceed to Bit Manipulation, Arrays, Strings, and Hashing.
