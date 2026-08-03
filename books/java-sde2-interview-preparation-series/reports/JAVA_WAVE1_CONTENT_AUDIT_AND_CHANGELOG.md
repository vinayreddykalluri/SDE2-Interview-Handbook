# Java Wave 1 Content Audit and Changelog

Date: 2026-08-02

Scope: Java Fundamentals (`03`) and low-level collection mechanics for Advanced Java C (`18C`)

Status: content and executable companions implemented; publishing-manifest integration intentionally left to the coordinating change

## Executive finding

The Java Fundamentals PDF was large enough but not prerequisite-first. Its 199-page build mixed beginner syntax with advanced language and library chapters already owned by later books. A restarting reader could encounter boxing, `BigDecimal`, lambdas, generic varargs, bridge methods, array covariance, grapheme handling, and advanced collection contracts before the smaller rule underneath was secure.

Wave 1 replaces those early jumps with eleven native chapters that progress from values to interview-quality code. Each chapter has a complete executable example, an edge-case matrix, realistic interviewer questions with model answers, and focused practice. A separate low-level Collections lab now demonstrates the mechanics behind dynamic arrays, linked lists, hash maps, binary heaps, comparators, and sorting failures.

## Canonical sources audited

### Java Fundamentals

- manifest entry `03` in `publishing/series.json`;
- existing native platform chapter;
- master chapters 12 through 15;
- existing combined OOP and wrapper/generic/I/O chapters;
- existing basic Collections and Java traps chapters;
- all four exercise labs and four solution studios;
- the 70-example `JavaFundamentalsExamples.java` companion;
- duplicated master chapters 20, 25, 30, and 48;
- current generated PDF: 199 pages, 4,159,237 bytes.

### Collections, Streams, and I/O

- manifest entry `18C`;
- master chapters 25 through 32 and collection-complexity appendix;
- current generated PDF: 96 pages, 4,141,766 bytes;
- existing collection selection, equality, hashing, list, map, queue, heap, comparator, stream, and I/O explanations.

## Pre-change content-quality matrix

| Topic | Previous state | Beginner clarity | Interview relevance | Accuracy | Required action |
|---|---|---:|---:|---:|---|
| values, primitives, references | deep master chapter mixed basics with `var`, boxing, and `BigDecimal` | weak sequence | strong facts | generally accurate | rewrite prerequisite-first |
| operators and control flow | one broad chapter used advanced types while teaching syntax | uneven | adequate | generally accurate | split expressions from flow |
| pass-by-value | present but adjacent to generic-varargs and bridge-method depth | confusing transition | high | accurate core | isolate three canonical cases |
| arrays and strings | mixed basic traversal with covariance, text blocks, and Unicode depth | abrupt | high | mostly accurate | split and stage edge cases |
| classes and OOP | compressed into one native chapter | too shallow | high | adequate | separate objects/constructors from OOP relationships |
| wrappers/generics/enums | combined with I/O and utilities | crowded | high | adequate | teach before collections; split boundary APIs |
| exceptions and I/O | advanced master exception chapter duplicated later-book content | too advanced | high | strong | create fundamentals-sized boundary chapter |
| collections usage | strong basic API chapter existed | strong | high | strong | retain without advanced duplicates |
| interview-quality coding | full advanced playbook duplicated in Fundamentals | too large a jump | high | strong | replace with fundamentals-sized workflow |
| runnable examples | one 70-check monolith | hard to map to chapter | high | compiled | add chapter-scoped files |
| exercise solutions | answer material existed, but high-value new chapter drills lacked scoped runnable checks | uneven | medium | adequate | add complete solution studio and harness |
| collection internals | master prose covered many details but had no focused executable miniature structures | missing implementation lab | very high | conceptual | add low-level implementations and adversarial tests |

## Priority findings

### Critical

1. The Fundamentals manifest used concepts before their prerequisites. This is a learning-sequence defect even when the individual statements are accurate.
2. Four advanced master chapters appeared both in Fundamentals and their owning advanced books, creating duplication and an artificial jump in depth.
3. Complete examples were centralized in one large companion, so a reader could not easily compile the example beside the chapter that explained it.

### High value

1. Primitive/reference behavior needed a single concrete progression through aliasing, mutation, and reassignment.
2. Overflow-before-assignment, numeric promotion, null unboxing, comparator overflow, and hash-map cost assumptions needed more prominent edge treatment.
3. Array copying needed an explicit distinction among alias, shallow outer copy, and row-by-row jagged copy.
4. Collection internals needed code that makes capacity, links, buckets, collisions, resizing, and heap invariants inspectable.
5. Interview prompts needed model answers and follow-ups, not lists of question titles.

### Nice to improve after integration

1. Add small bespoke diagrams through the existing diagram pipeline if the rendered ASCII diagrams need more visual emphasis.
2. Link each chapter practice item to its corresponding solution anchor where the PDF renderer supports stable internal anchors.
3. After PDF integration, tune only tables or code blocks that produce content-induced page breaks.

## Implemented Java Fundamentals sequence

| Order | New source | Purpose | Executable example | Practice prompts |
|---:|---|---|---|---:|
| 1 | `01-platform-program-foundations.md` | retain existing source-to-execution introduction | existing | existing |
| 2 | `02-values-types-and-literals.md` | primitives, references, locals/fields, literals, early overflow | `ValuesAndTypesExample` | 5 |
| 3 | `03-operators-conversions-and-expressions.md` | promotion, conversion, equality, short-circuit, compound assignment | `OperatorsAndConversionsExample` | 5 |
| 4 | `04-conditions-loops-and-indexes.md` | conditions, version-labeled switch, loops, half-open ranges, progress | `ControlFlowExample` | 5 |
| 5 | `05-methods-and-pass-by-value.md` | contracts, scope, overloads, varargs, three pass-by-value cases | `MethodsAndPassByValueExample` | 5 |
| 6 | `06-arrays-from-first-principles.md` | traversal, mutation, aliasing, copy, jagged arrays, core utilities | `ArraysExample` | 6 |
| 7 | `07-strings-characters-and-builders.md` | equality, pool, immutability, builder, digits, Unicode boundary | `StringsAndCharactersExample` | 6 |
| 8 | `08-classes-objects-and-constructors.md` | state, constructors, `this`, static, access, packages, invariants | `ClassesAndConstructorsExample` | 6 |
| 9 | `09-object-oriented-foundations-prerequisite-first.md` | inheritance, dispatch, abstraction, interface, composition, equality | `ObjectOrientedFoundationsExample` | 6 |
| 10 | `10-wrappers-generics-and-enums.md` | boxing, null, identity, type safety, generic method, finite state | `WrappersGenericsEnumsExample` | 6 |
| 11 | existing `05-java-collections-basics.md` | usage-first List/Set/Map/Deque/PriorityQueue | existing 70-check companion | existing |
| 12 | `11-exceptions-input-output-and-utilities.md` | failure boundaries, ownership, parsing, Scanner/reader, core utilities | `ExceptionsIoUtilitiesExample` | 7 |
| 13 | `12-writing-interview-quality-java.md` | contracts, safe types, invariants, mutation, tests, communication | `InterviewQualityExample` | 7 |
| 14 | existing `04-java-interview-traps.md` | cumulative trap review | existing 70-check companion | existing |

The eleven new chapters contain 12,292 words, 64 embedded practice prompts, eleven exact-output runnable examples, and recurring edge-case/interview-room sections.

## Accuracy and clarity corrections

- States without qualification that Java always passes argument values by value.
- Separates a copied reference value from the object it designates.
- Distinguishes field default values from uninitialized local variables.
- Shows that arithmetic can overflow before assignment to `long` and widens an operand first.
- Separates primitive equality, reference identity, and semantic equality.
- Labels switch expressions as Java 14+ and pattern matching for `instanceof` as Java 16+.
- Explains that a constructor has no return type and that a generated default constructor exists only when no constructor is declared.
- Separates overloading, overriding, static hiding, and field access.
- States that abstract classes can have constructors and interfaces can have implemented methods on supported Java versions.
- Explains that `final` reference fields do not make reachable mutable state immutable.
- Treats wrapper caching as an identity trap, never a value-comparison technique.
- Covers null unboxing and rejects `List<int>` in favor of `List<Integer>`.
- Qualifies `finally`, `HashMap` operation cost, priority-queue iteration order, and `Math.abs(MIN_VALUE)`.
- Preserves parsing causes, explains try-with-resources suppression, and makes resource ownership explicit.
- Uses `Integer.compare`/comparator builders instead of subtraction.

## Complete solution coverage added

`solutions/05-wave1-complete-solutions.md` and `FundamentalsSolutionChecks.java` provide eleven explained, executable solutions:

1. overflow-safe average;
2. widening before multiplication;
3. pass-by-value mutation versus reassignment;
4. jagged primitive deep copy with null rows;
5. validated decimal-digit conversion;
6. invariant-preserving account mutation;
7. type-safe generic `last`;
8. deterministic most-frequent value with an explicit tie rule;
9. contextual parsing failure with preserved cause;
10. comparator without subtraction overflow;
11. overflow-safe complement calculation.

## Collections implementation lab added

New canonical source area: `content/volumes/java/JAVA-05-collections-streams-and-io/`.

The lab adds:

- `MiniDynamicArray<E>` with size/capacity separation, growth, middle insertion, removal, tail clearing, bounds, and controlled generic casts;
- `MiniLinkedList<E>` with first/last handling, bidirectional rewiring, nearest-end traversal, unlink, clear, and a link-consistency invariant check;
- `MiniHashMap<K,V>` with power-of-two buckets, hash spreading, separate chaining, update-before-resize, re-bucketing, collision keys, explicit null policy, removal, and capacity guards;
- `BinaryHeap<E>` with comparator-driven min-heap order, growth, sift-up, sift-down, empty behavior, and invariant checks after every mutation;
- `ComparatorSortingEdgeHarness` with integer-subtraction overflow, extreme-value sorting, and deterministic tie-breaking.

The 3,188-word reader chapter supplies representation diagrams, operation dry runs, conditional complexity claims, an edge matrix for every structure, realistic interviewer/model-answer exchanges, failure injection, and ten practice tasks.

## Content boundaries established

Remove these sources from the **Fundamentals manifest only**; keep the files and retain them in their advanced owners:

| Remove from volume `03` | Fundamentals replacement | Keep in advanced owner |
|---|---|---|
| master 12 | native chapter 02 | Advanced Java language material |
| master 13 | native chapters 03-04 | Advanced Java language material |
| master 14 | native chapter 05 | Advanced Java language material |
| master 15 | native chapters 06-07 | Advanced Java language material |
| old native OOP chapter | native chapters 08-09 | file can remain temporarily for link compatibility |
| old native wrappers/generics/I/O chapter | native chapters 10-11 | file can remain temporarily for link compatibility |
| master 20 | native chapter 11 | Advanced Java B (`18B`) |
| master 25 | existing basics chapter | Advanced Java C (`18C`) |
| master 30 | basics comparator section | Advanced Java C (`18C`) |
| master 48 | native chapter 12 | Java Interview Readiness (`18G`) |

No advanced source file was deleted.

## Exact manifest integration required

### Volume `03`

Replace the current chapter source sequence with:

```text
chapters/01-platform-program-foundations.md
chapters/02-values-types-and-literals.md
chapters/03-operators-conversions-and-expressions.md
chapters/04-conditions-loops-and-indexes.md
chapters/05-methods-and-pass-by-value.md
chapters/06-arrays-from-first-principles.md
chapters/07-strings-characters-and-builders.md
exercises/01-language-practice.md
chapters/08-classes-objects-and-constructors.md
chapters/09-object-oriented-foundations-prerequisite-first.md
exercises/02-object-practice.md
chapters/10-wrappers-generics-and-enums.md
chapters/05-java-collections-basics.md
chapters/11-exceptions-input-output-and-utilities.md
exercises/03-library-practice.md
chapters/12-writing-interview-quality-java.md
chapters/04-java-interview-traps.md
exercises/04-traps-and-readiness-practice.md
solutions/01-language-solutions.md
solutions/02-object-solutions.md
solutions/03-library-solutions.md
solutions/04-traps-and-readiness-solutions.md
solutions/05-wave1-complete-solutions.md
content/master/appendices/a-java-quick-reference.md
```

Paths above are relative to `content/volumes/java/JAVA-01-java-foundations-for-problem-solving/` until the final appendix path. Mark each new native source `"series_native": true`.

### Volume `18C`

After `content/master/25-collections-framework.md`, add:

```json
{
  "path": "content/volumes/java/JAVA-05-collections-streams-and-io/chapters/00-low-level-collections-implementation-lab.md",
  "series_native": true
}
```

Add this companion metadata to volume `18C`:

```json
"code_companion": {
  "path": "content/volumes/java/JAVA-05-collections-streams-and-io/code/CollectionsImplementationChecks.java",
  "title": "Low-Level Collections Implementation Checks",
  "description": "Dependency-free Java 21 implementations and adversarial checks for a dynamic array, doubly linked list, separate-chaining hash map, binary min-heap, and comparator edge cases. A successful run prints exactly PASS 5 low-level collection implementation suites."
}
```

## Deferred and intentionally out of scope

- Publishing manifest, build scripts, website, repository organization, and README were not edited in this wave.
- PDFs were not rebuilt because the coordinating agent owns manifest integration and full-series build validation.
- Advanced overload resolution, erasure, wildcard capture, grapheme algorithms, JDK tree-bin thresholds, iterator implementation, concurrent collections, streams, and NIO internals remain with their later books.
- The old combined native chapters remain on disk only for compatibility; after link audit they may be archived in a later cleanup.

## Acceptance criteria for integration

- volume `03` no longer includes the ten superseded/duplicated sources listed above;
- all eleven new complete examples compile in focused-series validation;
- volume `18C` exposes exactly one dependency-free companion;
- both PDFs build inside their configured page ranges;
- table of contents follows the prerequisite order;
- no code/table clipping or orphaned headings appear on affected pages;
- chapter and solution links resolve in web and PDF outputs.
