# Contributing

## Start with the repository map

Read [the repository structure guide](../docs/community/repository-structure.md) before moving or adding files. Documentation, runnable code, the portal shell, build tooling, and generated output have separate ownership boundaries. The [project roadmap](../docs/project/roadmap.md) lists current contribution priorities.


Contributions to explanations, diagrams, exercises, Java examples, automation, accessibility, and editorial quality are welcome.

The independently navigable PDF series and its canonical sources live under [`books/java-sde2-interview-preparation-series/`](../books/java-sde2-interview-preparation-series/). Book contributions should improve an existing canonical source rather than create a competing copy.

By participating, you agree to follow the [Code of Conduct](CODE_OF_CONDUCT.md).

## Choose the right contribution path

- Use the content-improvement issue form for inaccurate, incomplete, or unclear handbook material.
- Use the code-example issue form for correctness, edge-case, complexity, or Java-version problems.
- Use the feature-request form for new volumes, tooling, and publishing improvements.
- Small corrections can go directly to a pull request.

## Local setup

```bash
git clone https://github.com/vinayreddykalluri/SDE2-Interview-Handbook.git
cd SDE2-Interview-Handbook
python -m venv .venv
source .venv/bin/activate
make install
make validate
```

Windows PowerShell users can activate the environment with `.venv\Scripts\Activate.ps1`.

## Documentation standards

1. Put numbered chapters in the matching `docs/coding-foundations/NN-topic/` directory.
2. Use lowercase kebab-case filenames such as `02-breadth-first-search.md`.
3. Start from `docs/chapter-template.md` and retain all required sections.
4. Explain the invariant, trade-offs, failure modes, and complexity instead of only presenting a solution.
5. Use Mermaid for diagrams and keep labels readable in light, dark, and printed output.
6. Link to canonical source under `examples/java/src/main/java/` instead of embedding large implementations in prose.
7. Add attribution for material adapted from another source and ensure its license is compatible.

## Published book standards

- Preserve the existing Markdown, Java 21, diagram, and PDF publishing system.
- Teach prerequisites before dependent interview patterns.
- Keep valid complete Java examples compiling and isolate intentionally invalid snippets.
- Update the affected audit, coverage, validation, changelog, or build evidence in proportion to the change.
- Rebuild and inspect affected PDFs when prose, code, tables, diagrams, covers, links, or navigation change.
- Add publication-level individual credit to the [authorship record](../docs/community/authors.md) when the contribution qualifies.

### Book contribution workflow

1. Choose the existing volume under `books/java-sde2-interview-preparation-series/content/volumes/` or the master chapter under `content/master/`.
2. Confirm its mapping in `books/java-sde2-interview-preparation-series/publishing/series.json`.
3. Add the smallest prerequisite explanation before adding an advanced pattern.
4. Keep exercises and reasoning-based solutions in the owning focused volume.
5. Add or update a compiling companion under the owning volume's `code/` directory when executable evidence is required.
6. Update the relevant file under `books/java-sde2-interview-preparation-series/reports/`.
7. Build and inspect the affected PDF in `books/java-sde2-interview-preparation-series/dist/`.

Open an issue before adding or removing a volume, changing the Java baseline, changing the learning path, or introducing a publishing dependency. This prevents parallel contributors from producing competing sources.

Good first book contributions include one confusing-paragraph rewrite, one verified edge case, one output-prediction or debugging exercise with explanation, one accessibility correction, or one reproducible PDF layout report.

## Java example standards

- Target Java 17 language features unless the chapter explicitly compares Java 8, 17, and 21.
- Use the package prefix `io.github.vinayreddykalluri.interviewhandbook`.
- Use semantic class names. Do not use `Solution1`, `Example2`, or chapter-number-only names.
- Validate null, empty, malformed, and overflow-prone inputs when those cases affect the contract.
- Document assumptions and time/space complexity in the chapter or source Javadoc.
- Keep examples dependency-free unless a dependency is essential to the concept.
- Add or extend a smoke check when behavior changes.

## Required checks

```bash
make validate
make build-site
```

For changes to print automation or formatting, also run:

```bash
make build-all
```

## Pull request expectations

- Keep one coherent change per pull request.
- Explain what changed, why it is correct, and how it was validated.
- Include screenshots for visual site changes.
- Identify any generated artifact that was manually inspected.
- Do not commit `site/`, `output/`, virtual environments, IDE state, or secrets.
- The versioned artifacts under `books/java-sde2-interview-preparation-series/dist/` are intentional release files, not disposable root build output.

## Review criteria

Maintainers review technical accuracy, interview relevance, production realism, readability, source-code correctness, accessibility, and print behavior. A requested revision is part of normal collaborative editing.

## Licensing

Code and tooling contributions are accepted under the [MIT License](../LICENSE). Documentation and diagram contributions are accepted under [CC BY 4.0](../LICENSE-CONTENT.md). By submitting a contribution, you confirm that you have the right to license it under the applicable terms.
