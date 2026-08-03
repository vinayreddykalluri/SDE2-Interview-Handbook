# Java Fundamentals Prerequisite Map

This file records the canonical beginner-first source order prepared for the Java Fundamentals volume. It is an integration note for the publishing manifest; it is not intended to become a reader-facing chapter.

## Canonical reader order

1. `01-platform-program-foundations.md`
2. `02-values-types-and-literals.md`
3. `03-operators-conversions-and-expressions.md`
4. `04-conditions-loops-and-indexes.md`
5. `05-methods-and-pass-by-value.md`
6. `06-arrays-from-first-principles.md`
7. `07-strings-characters-and-builders.md`
8. `08-classes-objects-and-constructors.md`
9. `09-object-oriented-foundations-prerequisite-first.md`
10. `10-wrappers-generics-and-enums.md`
11. `05-java-collections-basics.md`
12. `11-exceptions-input-output-and-utilities.md`
13. `12-writing-interview-quality-java.md`
14. `04-java-interview-traps.md`
15. Existing practice labs, followed by their solution studios
16. `content/master/appendices/a-java-quick-reference.md`

The manifest controls reader order. Obsolete combined chapters were removed after their essential material was verified in the prerequisite-first chapters below.

## Sources replaced in this volume

| Current manifest source | Replacement in Java Fundamentals | Long-term owner |
|---|---|---|
| `content/master/12-variables-types-literals.md` | `02-values-types-and-literals.md` | Advanced Java Language may retain the original deep treatment |
| `content/master/13-operators-expressions-control-flow.md` | `03-operators-conversions-and-expressions.md` plus `04-conditions-loops-and-indexes.md` | Advanced Java Language may retain the original deep treatment |
| `content/master/14-methods-overloading-varargs-pass-by-value.md` | `05-methods-and-pass-by-value.md` | Advanced Java Language owns overload-resolution and generic-varargs depth |
| `content/master/15-arrays-strings-text-blocks-unicode.md` | `06-arrays-from-first-principles.md` plus `07-strings-characters-and-builders.md` | Advanced Java Language owns covariance, text blocks, and Unicode depth |

## Duplicated advanced chapters to remove from Java Fundamentals

| Duplicated source | Keep in | Fundamentals replacement |
|---|---|---|
| `content/master/20-exceptions-resource-management.md` | Java Language, OOP, and Modern Java | `11-exceptions-input-output-and-utilities.md` teaches the required basics |
| `content/master/25-collections-framework.md` | Collections, Streams, and I/O | `05-java-collections-basics.md` teaches interview usage only |
| `content/master/30-comparable-comparator-sorting-and-selection.md` | Collections, Streams, and I/O | Comparator safety remains in the collections basics chapter |
| `content/master/48-the-java-coding-interview-playbook.md` | Java Interview Readiness | `12-writing-interview-quality-java.md` supplies the fundamentals-sized workflow |

Removing these four duplicates is a content-boundary correction, not a deletion from the repository. The Fundamentals book should link to the owning volumes instead of reproducing them.

## Content rules established by this wave

- Do not require a lambda before interfaces and methods have been introduced.
- Do not require collections before wrappers and basic generics have been introduced.
- Do not use `BigDecimal`, reflection, bridge methods, generic varargs, or JVM optimization as the first explanation of a basic language rule.
- Every complete example names its class, states expected output, and has a matching executable source under `code/fundamentals/`.
- Intentionally invalid examples are labeled and are not copied into executable companions.
- Fundamentals teaches collection use; the Collections book owns implementation mechanics.
