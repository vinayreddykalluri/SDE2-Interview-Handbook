# Solution Studio B - Arrays, Strings, and Objects

Use this studio only after Practice Lab B. Redraw every alias diagram before reading an answer, and distinguish object identity, value equality, copy depth, mutation, and runtime dispatch. A solution is complete when the code compiles, the object/reference picture matches the code, and one boundary test would fail under the original misconception.

The coding guidance gives design constraints rather than a single canonical implementation. Preserve those constraints while choosing clear names and the smallest useful API. Add missed rules to the same error log used in Practice Lab A.

## Knowledge answers K26-K50

- **K26:** Declaration introduces an array reference; creation allocates fixed-length storage; initialization supplies initial element values or defaults.
- **K27:** Numeric zero, false, `\u0000` for char, and null for references.
- **K28:** Assignment copies the same array reference, so element mutation is shared.
- **K29:** Shallow copy duplicates one container while nested references remain shared; deep copy recursively duplicates the state required by the contract.
- **K30:** Arrays inherit identity-oriented `Object.toString`; use `Arrays.toString` or `deepToString`.
- **K31:** `equals` compares one-dimensional element values/references appropriately; `deepEquals` descends into nested arrays.
- **K32:** A two-dimensional array is an array of row references, and each row can have a different length or be null.
- **K33:** Its public contract exposes no mutation; operations return new results, enabling safe sharing and stable hashing.
- **K34:** Identity asks same object, content asks same characters, pooling may share selected strings but is not the value contract.
- **K35:** Empty has length zero; blank contains only whitespace under the chosen API; null means no String reference.
- **K36:** Use a builder when repeated updates would copy a growing immutable result, especially in loops.
- **K37:** One UTF-16 code unit.
- **K38:** Code unit is one char, code point is a Unicode scalar value represented by one/two units, grapheme is a user-perceived cluster that may include multiple code points.
- **K39:** Class defines type; object is instance; field stores state; method defines behavior.
- **K40:** A reference value designating an object or null, not the full object.
- **K41:** Only when the class declares no constructor, subject to superclass construction accessibility.
- **K42:** Receiver access/disambiguation, constructor chaining with `this(...)`, and briefly passing/returning the receiver.
- **K43:** It is shared, order-dependent state that leaks across tests and requires concurrency reasoning.
- **K44:** Same-package access is broad; cross-package subclass access is through inherited context, not arbitrary base/sibling instances.
- **K45:** It enables a short source name; it does not copy, load, or repackage the type.
- **K46:** Encapsulation protects representation and invariants through domain operations; indiscriminate setters may destroy both.
- **K47:** Overloading is compile-time parameter-list selection; overriding is runtime instance dispatch; fields/static methods hide by declared type.
- **K48:** Interface is a role/contract, abstract class may share state/construction/implementation, concrete class is instantiable and complete.
- **K49:** For HAS-A, configurable collaboration, false subtype relationships, localized invariants, and test seams.
- **K50:** If two keys are equal, their hash codes must be equal, and equality/hash-relevant state must remain stable while keyed.

## Output answers O20-O38

- **O20:** `0`; int array elements receive default zero.
- **O21:** The original shows the mutation because both variables alias one array.
- **O22:** Original nested element changes because outer clone shares row references.
- **O23:** `[1, 2]`; the utility formats contents.
- **O24:** Both rows are null; only the outer array was created.
- **O25:** `av`; start is inclusive and end exclusive.
- **O26:** Usually/defined for identical interned literal occurrence as true, but value code must use equals because nonliteral construction/runtime concatenation need not share identity.
- **O27:** `false`; explicit construction creates another object.
- **O28:** Original lowercase value; the returned String was ignored.
- **O29:** Apply operations in source order; the builder mutates in place, unlike String.
- **O30:** `2`; the supplementary code point occupies a surrogate pair.
- **O31:** Only the renamed object's field changes; objects have independent state.
- **O32:** Both aliases observe the new field value.
- **O33:** Target constructor body prints before the delegating constructor continues.
- **O34:** Three if each constructor increments once; static state is shared.
- **O35:** Parent field, child method; fields are hidden and instance methods dispatch.
- **O36:** Compile-time overload chosen from the declared type and argument type.
- **O37:** Implementing object's override runs through the interface reference.
- **O38:** Lookup can fail after key mutation because hash/equality no longer match original bucket placement.

## Debug answers D20-D38

- **D20:** Use `< length`. **D21:** copy before mutation when preservation is the contract.
- **D22:** Allocate outer storage and clone/copy each non-null row. **D23:** use `Arrays.equals` or `deepEquals`.
- **D24:** use `Arrays.toString/deepToString`. **D25:** check each row and use `matrix[row].length`.
- **D26:** use `equals` or `Objects.equals`. **D27:** reject null, `Objects.equals`, or constant-first equality according to contract.
- **D28:** use a builder. **D29:** validate ASCII range or use `Character.digit`.
- **D30:** traverse code points with `codePoints()`/`codePointAt` and define grapheme limitations.
- **D31:** remove `void`; otherwise it is a method. **D32:** declare the no-argument constructor explicitly and delegate to validation.
- **D33:** pass a receiver or make it an instance method. **D34:** expose validated domain operations instead of raw assignment.
- **D35:** match parameters and add `@Override`. **D36:** guard with `instanceof` or move needed behavior to the base contract.
- **D37:** implement an interface's public method as public. **D38:** defensively copy on input and return an unmodifiable copy/view that cannot expose mutation.

## Coding guidance C20-C38

- **C20-C25:** State shape, null-row, mutation, and copy-depth contracts; test empty, singleton, jagged, and null rows.
- **C26-C29:** Use equals for value, builder for repeated construction, and code-point/digit APIs when the contract exceeds ASCII code units.
- **C30-C34:** Validate constructors, keep fields private, make class/static ownership explicit, and implement equality from stable value state.
- **C35-C37:** Demonstrate one actual override with `@Override`; call through base/interface references to prove runtime dispatch. Abstract classes may construct shared state.
- **C38:** Inject the collaborator through the constructor, delegate through its interface, and test with a deterministic fake; no subtype claim is needed.

## Follow-up guidance F14-F26

- **F14-F16:** Mutation is an API decision; copy depth and sorted-input prerequisites must be stated and tested.
- **F17-F18:** `char` is correct for an explicitly UTF-16-code-unit/ASCII contract; complexity units must match traversal.
- **F19-F21:** Final stops reassignment, not nested mutation; constructor dispatch can expose default subclass state; protected cross-package access is inheritance-scoped.
- **F22-F24:** Abstract class for shared state/template, interface for role/open implementations, composition for replaceable HAS-A behavior.
- **F25-F26:** Hash tables rely on equal hash codes; immutable owners copy mutable input and avoid exposing mutable internal aliases.

**A02 model:** A strong design uses immutable value identifiers, one guarded aggregate for state transitions, an enum rather than strings, an injected policy interface, no public mutable collections, and tests illegal transitions, duplicate identifiers, null collaborators, and alias attempts.
