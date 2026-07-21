# Contributing

Contributions to explanations, diagrams, exercises, Java examples, automation, accessibility, and editorial quality are welcome.

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

1. Put numbered chapters in the matching `docs/volume-XX-topic/` directory.
2. Use lowercase kebab-case filenames such as `02-breadth-first-search.md`.
3. Start from `docs/chapter-template.md` and retain all required sections.
4. Explain the invariant, trade-offs, failure modes, and complexity instead of only presenting a solution.
5. Use Mermaid for diagrams and keep labels readable in light, dark, and printed output.
6. Link to canonical source under `examples/java/src/main/java/` instead of embedding large implementations in prose.
7. Add attribution for material adapted from another source and ensure its license is compatible.

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

## Review criteria

Maintainers review technical accuracy, interview relevance, production realism, readability, source-code correctness, accessibility, and print behavior. A requested revision is part of normal collaborative editing.

## Licensing

Code and tooling contributions are accepted under the [MIT License](LICENSE). Documentation and diagram contributions are accepted under [CC BY 4.0](LICENSE-CONTENT.md). By submitting a contribution, you confirm that you have the right to license it under the applicable terms.
