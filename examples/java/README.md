# Java Example Project

This directory contains the canonical Java implementations linked from the handbook. It is intentionally independent from MkDocs so examples can be compiled, reviewed, and tested as normal source code.

## Requirements

- JDK 17 or newer
- Python 3.11 or newer for the cross-platform validator

## Validate everything

From the repository root:

```bash
make validate-code
```

The validator:

1. discovers every source under `src/main/java` and `src/test/java`;
2. compiles with `javac --release 17 -Xlint:all` into a temporary directory;
3. runs the dependency-free smoke suite with Java assertions enabled; and
4. removes generated class files automatically.

No compiled artifacts are written into the source tree.

## Source layout

```text
examples/java/
├── README.md
└── src/
    ├── main/java/io/github/vinayreddykalluri/interviewhandbook/
    │   ├── problemsolving/
    │   ├── javafundamentals/
    │   ├── complexity/
    │   └── ... dynamicprogramming/
    └── test/java/io/github/vinayreddykalluri/interviewhandbook/tests/
        └── ExampleSmokeTest.java
```

Volume 01 contains focused Java/JVM demonstrations and warm-up solutions. Volumes 02-19 contain the principal data-structure and algorithm implementation associated with each volume.

The `problemsolving` package contains SDE-2 interview extensions that combine multiple foundations or require deeper proof. It includes the handbook's worked threshold-window problem, prefix-plus-hash reasoning, monotonic stacks, topological sorting, Dijkstra, union-find, top-k selection, greedy interval scheduling, dynamic programming, backtracking, binary search on the answer, and reservoir sampling.

## Programming problem-solving examples

| Family | Examples |
| --- | --- |
| Range and prefix state | `MinimumSizeSubarraySum`, `SubarraySumCount` |
| Ordered and monotone state | `NextGreaterElement`, `MinimumShipCapacity` |
| Graphs and connectivity | `CourseScheduleOrder`, `DijkstraShortestPath`, `DisjointSet` |
| Selection and optimization | `TopKFrequentElements`, `IntervalScheduling`, `CoinChange`, `UniquePermutations` |
| Streaming and randomization | `ReservoirSampling` |

All public methods document their contract, invariant, and complexity. The dependency-free smoke suite executes every example through `make validate-code`.

## Run a class with a main method

The full validator is the preferred route. To experiment with an individual main class:

```bash
mkdir -p /tmp/sde2-handbook-classes
javac --release 17 -d /tmp/sde2-handbook-classes \
  examples/java/src/main/java/io/github/vinayreddykalluri/interviewhandbook/codingfoundations/javafundamentals/PrimitiveDemo.java
java -cp /tmp/sde2-handbook-classes \
  io.github.vinayreddykalluri.interviewhandbook.codingfoundations.javafundamentals.PrimitiveDemo
```

Use the actual semantic filename and fully qualified class name shown in the source tree.

## Example design rules

- One public top-level class per file.
- Public class name and filename must match.
- Package names identify the handbook volume.
- Utility methods are deterministic and avoid hidden mutable state.
- Preconditions are checked when invalid input would make the example misleading.
- Arithmetic uses overflow-aware operations where overflow changes correctness.
- Interview assumptions, invariants, and complexity belong in the linked chapter.
- Behavior changes require a smoke check.

## Add an example

1. Choose the package for the matching volume.
2. Use a semantic class name that describes the concept or pattern.
3. Add a link from the chapter's Code Example section.
4. Extend `ExampleSmokeTest` for executable behavior.
5. Run `make validate`.

Source files are licensed under the repository's MIT License.
