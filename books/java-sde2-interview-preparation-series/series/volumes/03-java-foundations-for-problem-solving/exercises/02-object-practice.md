# Practice Lab B - Arrays, Strings, and Objects

**Start here:** draw references before writing code. For every array or object question, mark which variables hold primitive values, which hold reference values, and which references designate the same object. For strings, state whether the requirement is identity, UTF-16 content, code points, or user-visible text before selecting an API.

Complete K26-K50 aloud, predict O20-O38 on paper, compile repairs for D20-D38, and implement C20-C38 in small files. Use a mutation table for each task: input alias, state before, operation, state after, and whether the caller observes the change. Check Solution Studio B later and add every missed aliasing, constructor, dispatch, or equality rule to your error log.

## Knowledge check

- **K26 [Foundation]** Distinguish array declaration, creation, and initialization.
- **K27 [Foundation]** What default values do array elements receive?
- **K28 [Interview Core]** Explain array aliasing.
- **K29 [Interview Core]** Distinguish shallow from deep copy.
- **K30 [Foundation]** Why does printing an array reference not print its contents?
- **K31 [Interview Core]** Distinguish `Arrays.equals` from `Arrays.deepEquals`.
- **K32 [Foundation]** What makes a Java two-dimensional array jagged-capable?
- **K33 [Foundation]** Why is `String` immutable?
- **K34 [Interview Core]** Distinguish string identity, content, and pooling.
- **K35 [Foundation]** Distinguish empty, blank, and null strings.
- **K36 [Interview Core]** When should repeated concatenation use `StringBuilder`?
- **K37 [Foundation]** What does a Java `char` represent?
- **K38 [Interview Core]** Distinguish code unit, code point, and user-perceived character.
- **K39 [Foundation]** Define class, object, field, method, state, and behavior.
- **K40 [Interview Core]** What does a reference variable store?
- **K41 [Foundation]** When is a default constructor supplied?
- **K42 [Foundation]** List the three common uses of `this` in fundamentals.
- **K43 [Interview Core]** Why is mutable static state hard to test?
- **K44 [Interview Core]** Explain protected access across package boundaries.
- **K45 [Foundation]** What does an import declaration do and not do?
- **K46 [Interview Core]** Why is encapsulation more than getters and setters?
- **K47 [Interview Core]** Distinguish overloading, overriding, field hiding, and static hiding.
- **K48 [Foundation]** Compare interface, abstract class, and concrete class.
- **K49 [Interview Core]** When is composition preferable to inheritance?
- **K50 [SDE-2 Follow-up]** State the `equals`/`hashCode` requirement for hash keys.

## Predict the output

- **O20 [Foundation]** Predict an `int[3]` element before assignment.
- **O21 [Interview Core]** Predict output after mutating an aliased one-dimensional array.
- **O22 [Interview Core]** Predict output after mutating a nested row through `matrix.clone()`.
- **O23 [Foundation]** Predict `Arrays.toString(new int[]{1,2})`.
- **O24 [Interview Core]** Predict `new int[2][]` row values.
- **O25 [Foundation]** Predict `"java".substring(1,3)`.
- **O26 [Interview Core]** Predict literal equality for `"a" == "a"` and explain why not to rely on it generally.
- **O27 [Interview Core]** Predict equality for `new String("a") == "a"`.
- **O28 [Foundation]** Predict a string after ignored `toUpperCase()` return value.
- **O29 [Foundation]** Predict a builder after `append`, `deleteCharAt`, and `reverse`.
- **O30 [Interview Core]** Predict the length of one supplementary emoji encoded as UTF-16.
- **O31 [Foundation]** Predict two objects' independent field values after one is renamed.
- **O32 [Interview Core]** Predict a shared object's field after alias mutation.
- **O33 [Foundation]** Predict constructor-chaining print order with `this(...)`.
- **O34 [Interview Core]** Predict static field count after creating three instances.
- **O35 [Interview Core]** Predict field and method outputs through a base reference to a child.
- **O36 [Interview Core]** Predict overloaded method selection through a base reference.
- **O37 [Interview Core]** Predict overridden method dispatch through an interface reference.
- **O38 [SDE-2 Follow-up]** Predict map lookup after mutating an equality-participating key.

## Debug the code

- **D20 [Foundation]** Repair array traversal that reads index `length`.
- **D21 [Interview Core]** Repair a method that unintentionally mutates the caller's array.
- **D22 [Interview Core]** Repair a supposed deep copy that clones only the outer array.
- **D23 [Foundation]** Repair `firstArray == secondArray` for content equality.
- **D24 [Foundation]** Repair direct array printing.
- **D25 [Interview Core]** Repair matrix traversal that assumes every row has `matrix[0].length`.
- **D26 [Foundation]** Repair `firstString == secondString` for value equality.
- **D27 [Interview Core]** Repair null-unsafe `text.equals(target)`.
- **D28 [Interview Core]** Repair repeated string concatenation in a large loop.
- **D29 [Foundation]** Repair ASCII digit conversion without validation.
- **D30 [Interview Core]** Repair string reversal that corrupts surrogate pairs under a code-point contract.
- **D31 [Foundation]** Repair a constructor mistakenly declared `void Student(...)`.
- **D32 [Foundation]** Restore a missing no-argument constructor intentionally.
- **D33 [Interview Core]** Repair a static method that directly reads an instance field.
- **D34 [Interview Core]** Repair a public setter that violates an account invariant.
- **D35 [Interview Core]** Repair an intended override with the wrong parameter type.
- **D36 [Interview Core]** Repair unsafe downcasting with a type check or stronger contract.
- **D37 [Interview Core]** Repair an interface implementation that reduces method visibility.
- **D38 [SDE-2 Follow-up]** Repair a mutable list field in an allegedly immutable class.

## Small coding tasks

- **C20 [Foundation]** Create and traverse a one-dimensional integer array.
- **C21 [Foundation]** Create and traverse a rectangular matrix.
- **C22 [Foundation]** Create a jagged array with row lengths 1, 2, and 3.
- **C23 [Interview Core]** Demonstrate array aliasing and then repair it with a copy.
- **C24 [Interview Core]** Deep-copy a nullable jagged array.
- **C25 [Foundation]** Compare and print one-dimensional arrays correctly.
- **C26 [Foundation]** Demonstrate string content versus identity comparison.
- **C27 [Interview Core]** Demonstrate literal pooling versus explicit construction.
- **C28 [Foundation]** Build a comma-separated string with `StringBuilder`.
- **C29 [Interview Core]** Convert validated decimal characters with `Character.digit`.
- **C30 [Foundation]** Implement the validated `Student` class from this volume.
- **C31 [Foundation]** Add constructor overloading and chaining to a `Rectangle`.
- **C32 [Interview Core]** Implement a class-level ID generator and discuss shared state.
- **C33 [Interview Core]** Encapsulate a bounded counter with invariant-preserving methods.
- **C34 [Interview Core]** Implement an immutable `Coordinate` with equality and hash code.
- **C35 [Foundation]** Implement a base `Notification` and overriding subtype.
- **C36 [Interview Core]** Demonstrate runtime polymorphism through a list of interface references.
- **C37 [Interview Core]** Implement an abstract parser with a concrete decimal parser.
- **C38 [SDE-2 Follow-up]** Replace a false inheritance relationship with a composed collaborator and test double.

## Interview follow-ups

- **F14 [Interview Core]** When should an array method preserve versus mutate its input?
- **F15 [SDE-2 Follow-up]** Define a copy contract for jagged arrays containing null rows.
- **F16 [Interview Core]** Why can `Arrays.binarySearch` be wrong on an unsorted array?
- **F17 [Interview Core]** When is `char` traversal the correct contract despite Unicode limits?
- **F18 [SDE-2 Follow-up]** How would you state string complexity in code units versus code points?
- **F19 [Interview Core]** Why does `final` not prove deep immutability?
- **F20 [Interview Core]** What construction bug can an overridable method expose?
- **F21 [SDE-2 Follow-up]** Explain protected access through a subclass in another package.
- **F22 [Interview Core]** When is an abstract class better than an interface?
- **F23 [Interview Core]** How can a default interface method create an evolution conflict?
- **F24 [SDE-2 Follow-up]** Defend composition for replaceability and testability.
- **F25 [Interview Core]** Why must equal hash keys have equal hash codes?
- **F26 [SDE-2 Follow-up]** Design an immutable class that owns a mutable list safely.

## Cumulative assessment 2

**A02:** In 45 minutes, build a small library checkout model using an enum state, an interface policy, composition, an immutable identifier, a mutable aggregate with guarded transitions, and a collection-free array snapshot. Explain aliases, constructor invariants, dispatch, equality, and five boundary tests.
