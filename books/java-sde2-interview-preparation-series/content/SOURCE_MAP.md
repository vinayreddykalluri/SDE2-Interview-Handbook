# Focused Series Source Map

## Mapping policy

The focused series preserves the master guide rather than cloning or deleting its material. [`publishing/series.json`](../publishing/series.json) is the canonical, machine-readable mapping from each book to its Markdown, Java companions, diagrams, PDF name, public shelf, and reader order. A source entry may select a named section when one master chapter supports several focused books; this avoids copying unrelated material.

Reader-facing navigation always uses the four shelf codes:

- Java Engineering: `JAVA-01` through `JAVA-09`
- Data Structures and Algorithms: `DSA-01` through `DSA-17`
- Frameworks, Data, and Messaging: `FW-01` through `FW-12`
- System Design: `SD-01` through `SD-02`

The manifest also retains older internal IDs because build commands, artifact history, and catalog automation depend on stable keys. An internal ID such as `01B` or `18A` is an implementation identifier, not a public study step. `path_labels` translates those keys to current shelf codes, and `segments[].books` defines order inside each shelf.

## Public-to-internal identity map

| Public books | Stable internal IDs |
|---|---|
| `JAVA-01`, `JAVA-02`, `JAVA-03` | `03`, `GIT`, `BUILD` |
| `JAVA-04` through `JAVA-09` | `18B`, `18C`, `18A`, `18D`, `18E`, `18G` |
| `DSA-01`, `DSA-02`, `DSA-03` | `02`, `01`, `01B` |
| `DSA-04` through `DSA-17` | `04` through `17` |
| `FW-01` through `FW-04` | `MYSQL`, `HIBERNATE`, `SPRING`, `BOOT` |
| `FW-05` through `FW-08` | `18H`, `DATA`, `MONGO`, `REDIS` |
| `FW-09` through `FW-12` | `18I`, `KAFKA`, `SPRINGX`, `SPRINGAI` |
| `SD-01`, `SD-02` | `18F`, `18J` |

Use public codes in prose, issue titles, website navigation, covers, and PDF filenames. Use an internal ID only where a tool explicitly requires `--volume <id>` or code reads a manifest key.

## Source ownership by shelf

All 41 focused books are publication editions. Their principal source ownership is:

| Public book or range | Principal source ownership |
|---|---|
| `JAVA-01` | Beginner-first Java chapters under `content/volumes/java/JAVA-01-*`, with selected master quick-reference material |
| `JAVA-02` and `JAVA-03` | Series-native Git/GitHub and Maven/Gradle chapters, labs, and solutions under their Java workspaces |
| `JAVA-04` | Master Chapters 16-24 and 58, plus feature reference material and a focused language-contract workshop |
| `JAVA-05` | Master Chapters 25-32, 55, and 56, plus collection reference material and a focused low-level collections lab |
| `JAVA-06` | Master Chapters 1-10 with 57 placed after Chapter 5, plus a focused JVM evidence workshop |
| `JAVA-07` | Master Chapter 11 and Chapters 33-38, plus a focused concurrency workshop |
| `JAVA-08` | Master Chapters 39-41 and production reference material, plus a focused diagnostics workshop |
| `JAVA-09` | Master Chapters 48, 53, and 54 and revision appendices, plus a focused mock-interview studio |
| `DSA-01` | Series-native complexity chapters, exercises, and solutions with selected master measurement and collection-cost reference material |
| `DSA-02` and `DSA-03` | One shared number-systems workspace: foundations in `DSA-02`, then interview patterns and rapid revision in `DSA-03` |
| `DSA-04` through `DSA-17` | Series-native beginner-first chapters, practice, solutions, and executable companions; selected master sections are reused only where they remain the canonical Java contract explanation |
| `FW-01` through `FW-12` | Series-native framework, database, caching, messaging, Spring ecosystem, and Spring AI workspaces with focused labs and validation evidence |
| `SD-01` | Master backend-design chapters plus series-native boundary exercises, solutions, and executable checks |
| `SD-02` | Series-native distributed-systems chapters, design drills, solutions, and executable checks |

The shared DSA number-systems workspace contains both `DSA-02` and `DSA-03` source because foundational representation and interview arithmetic are developed together. They remain separate public books and separate PDFs.

Dependency-requiring framework examples are labeled explicitly. Focused Java companions that model a contract without external dependencies are compiled and executed where the manifest declares them; real framework labs are validated through their owning build tool.

## Cross-reference policy

Master chapter numbers retained in extracted prose are labeled as `Master Chapter` or `Master Chapters`. Focused chapter numbers are local to each web book and PDF. The master book remains the stable comprehensive reference, while focused books provide prerequisite-aware learning and revision paths.

No valuable master source is deleted. Consolidation happens through selection at build time. Put a change in the shared master source when that is its canonical owner; put book-specific explanations, exercises, solutions, and labs in the matching public shelf workspace.
