# Java SDE-2 Interview Preparation Series

[![Java 21](https://img.shields.io/badge/Java-21-0B2545?logo=openjdk)](https://openjdk.org/projects/jdk/21/)
[![Book content: CC BY 4.0](https://img.shields.io/badge/book%20content-CC%20BY%204.0-C58A22)](LICENSE.md)
[![Code: MIT](https://img.shields.io/badge/code-MIT-087E8B)](LICENSE-CODE)
[![Contributions welcome](https://img.shields.io/badge/contributions-welcome-2E7D66)](CONTRIBUTING.md)
[![Latest release](https://img.shields.io/github/v/release/vinayreddykalluri/SDE2-Interview-Handbook?label=download)](https://github.com/vinayreddykalluri/SDE2-Interview-Handbook/releases/latest)

A free, open-source, basics-to-advanced Java and DSA book series for SDE-2 interview preparation.

The project combines clear explanations, runnable Java 21 examples, visual dry runs, debugging exercises, interview follow-ups, and production-oriented engineering judgment. It is designed for readers restarting from Java fundamentals as well as experienced developers preparing for deeper SDE-2 discussions.

This directory is the canonical book package inside the consolidated [SDE2 Interview Handbook repository](https://github.com/vinayreddykalluri/SDE2-Interview-Handbook). Repository-wide contribution, conduct, security, and governance policies apply in addition to the series-specific editorial files kept here.

[![Cover of the Java SDE-2 Interview Preparation Series Index](docs/images/series-index-cover.png)](series/dist/Java-SDE2-Interview-Preparation-Series-Index.pdf)

> Start with Java Foundations. Do not jump directly to advanced patterns if loop execution, types, collections, or complexity analysis are not yet automatic.

## Read the books

The release contains 28 focused topic PDFs plus one series index. All published PDFs are available in [`series/dist/`](series/dist/).

[Download the latest release](https://github.com/vinayreddykalluri/SDE2-Interview-Handbook/releases/latest) to get the modules as individually named PDF assets.

- [Open the complete series index](series/dist/Java-SDE2-Interview-Preparation-Series-Index.pdf)
- [Start with Java Foundations](series/dist/Java-SDE2-DSA-03-Java-Foundations-for-Problem-Solving.pdf)
- [Continue to Time and Space Complexity](series/dist/Java-SDE2-DSA-02-Time-and-Space-Complexity.pdf)
- [Study Number Systems — Part A](series/dist/Java-SDE2-DSA-01-Number-Systems-and-Math-Foundations.pdf)
- [Practice Number Systems — Part B](series/dist/Java-SDE2-DSA-01B-Number-Systems-Interview-Workbook.pdf)
- [Study Bit Manipulation](series/dist/Java-SDE2-DSA-04-Bit-Manipulation-in-Java.pdf)
- [Study Loop Mastery](series/dist/Java-SDE2-DSA-05-Loop-Mastery-and-Index-Calculations.pdf)

The detailed order, prerequisites, and completion gates are in [SERIES_ROADMAP.md](SERIES_ROADMAP.md).

### Recommended learning path

| Step | Focus | What the reader should gain |
|---:|---|---|
| 1 | Java Foundations | Language fluency, object/reference behavior, exceptions, generics, and core collections |
| 2 | Time and Space Complexity | Growth, memory, Java collection cost models, and trade-off explanations |
| 3 | Number Systems | Overflow-safe numeric reasoning, bases, modulo, GCD, powers, and interview practice |
| 4 | Bit Manipulation | Masks, shifts, XOR families, subsets, shortcuts, and safe Java techniques |
| 5 | Loop Mastery | Bounds, invariants, pointers, windows, searches, and matrix indexes |
| 6-17 | DSA Patterns | Arrays through dynamic programming in prerequisite order |
| 18 | Advanced Java | JVM, language, collections, concurrency, performance, backend, and system design |

Stable PDF volume numbers are retained for filenames; the learning-step labels inside the books identify the recommended order.

## What makes this series different

- **Beginner-first sequencing:** each volume establishes prerequisites before introducing interview patterns.
- **Java behavior, not syntax alone:** numeric promotion, references, iterators, equality, collection contracts, and failure behavior are explained precisely.
- **Visual reasoning:** diagrams show loop execution, state transitions, pointer elimination, memory relationships, and algorithm invariants.
- **Runnable evidence:** standalone Java companions compile with Java 21 and warnings treated as errors.
- **Interview depth:** examples include assumptions, correctness arguments, complexity, edge cases, follow-ups, and production extensions.
- **Active practice:** conceptual, output-prediction, debugging, coding, cumulative, and readiness exercises are separated from their solutions.
- **Reproducible publishing:** the Markdown sources, diagram generators, PDF builder, validation scripts, and release manifest are included.

## Repository map

```text
book/                 master-book Markdown chapters and appendices
series/
  series.json         canonical learning and physical-volume manifest
  volumes/            focused source, exercises, solutions, code, and figures
  dist/               published PDFs and integrity manifest
code-examples/        compilable Java 21 companion project
diagrams/             generated master-book diagrams
scripts/              build, diagram, validation, and visual-QA tooling
*.md                  audit, coverage, changelog, roadmap, and build reports
```

Regenerable working directories such as `build/`, `tmp/`, `series/build/`, and `series/tmp/` are intentionally ignored. The canonical source and published PDFs remain versioned.

## Build locally

### Prerequisites

- JDK 21 (`java` and `javac`)
- Python 3.11+
- Pandoc 3+
- Poppler for PDF inspection
- LibreOffice for Word rendering and verification
- Python packages from `requirements.txt`

```bash
python3 -m venv .venv
source .venv/bin/activate
python -m pip install -r requirements.txt
```

### Build the focused PDF series

```bash
python3 scripts/build_series.py
```

Build one physical volume during editing:

```bash
python3 scripts/build_series.py --volume 05 --skip-index
```

### Build the master book

```bash
python3 scripts/generate_diagrams.py
python3 scripts/build_book.py
```

## Validate changes

```bash
python3 scripts/validate_book.py --source-only
python3 scripts/validate_series.py --source-only
python3 scripts/qa_semantic_layout.py
```

Compile the master Java examples:

```bash
mkdir -p code-examples/build/classes
find code-examples/src/main/java -name '*.java' -print0 \
  | xargs -0 javac --release 21 -Xlint:all -Werror -d code-examples/build/classes
java -cp code-examples/build/classes com.interviewbook.examples.AllExamplesSmokeTest
```

Focused volumes may provide an additional standalone companion in their `code/` directory. Each volume's validation report records the exact command and observed output.

## Contribute

Contributions are welcome from Java developers, interviewers, educators, students, technical writers, and reviewers.

High-value contributions include:

- correcting an inaccurate or release-dependent Java claim;
- improving a confusing beginner explanation;
- adding a smaller dry run before an advanced example;
- contributing an original diagram or accessible text alternative;
- adding compiling boundary tests or debugging exercises;
- improving exercise solutions and interview follow-ups;
- reporting clipped PDF content, broken navigation, or build portability issues; and
- reviewing examples for Java 21 correctness, complexity, accessibility, and inclusive language.

Start with [CONTRIBUTING.md](CONTRIBUTING.md). GitHub issue forms collect reproducible details, and the pull-request template keeps educational, code, attribution, and PDF checks visible.

Every accepted contribution is credited to an individual in [AUTHORS.md](AUTHORS.md). Contributors retain copyright in their original work and license accepted contributions under the repository's content or code license. Commit and pull-request history provide an additional permanent authorship record.

## Editorial leadership and authorship

**Vinay Reddy Kalluri** is the series creator, founding author, **Editor-in-Chief**, and **Chief Auditor**.

The editorial model separates responsibilities clearly:

- individual contributors receive authorship credit for accepted original work;
- the Editor-in-Chief owns learning sequence, scope, voice, and publication decisions;
- the Chief Auditor owns technical-accuracy gates, evidence quality, Java validation, and release readiness; and
- substantial disagreements and attribution corrections are handled transparently under [GOVERNANCE.md](GOVERNANCE.md).

See [AUTHORS.md](AUTHORS.md) for the credit registry and [CITATION.cff](CITATION.cff) for citation metadata.

## Quality standard

Educational changes should move through this sequence:

1. beginner intuition;
2. exact Java mechanics;
3. compiling example and expected output;
4. dry run or state diagram where useful;
5. common failure and correction;
6. interview application and follow-up;
7. edge cases, complexity, and validation evidence.

Simplification must remain technically accurate. Implementation-specific JVM behavior must be labeled, collection complexity must be qualified, and intentionally invalid code must stay isolated from compilation.

## Licenses

- Book prose, exercises, diagrams, and published PDFs: [Creative Commons Attribution 4.0 International](LICENSE.md).
- Build scripts and source code: [MIT License](LICENSE-CODE).

Attribution does not imply endorsement. Java and related marks belong to their respective owners.

## Community

Please read the [Code of Conduct](CODE_OF_CONDUCT.md). Ask usage questions through [GitHub Discussions](https://github.com/vinayreddykalluri/SDE2-Interview-Handbook/discussions); use issues for actionable corrections or build defects. Security-sensitive reports should follow [SECURITY.md](SECURITY.md).

If this project helps you, star the repository, share the learning path, report confusing sections, and contribute one improvement for the next reader.
