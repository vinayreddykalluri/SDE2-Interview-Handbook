# Quality Standards

Quality in this project means the material is technically correct, useful under interview pressure, maintainable by contributors, and reproducible through automation.

## Automated checks

| Check | Detects |
| --- | --- |
| Structure validator | missing navigation targets, sparse chapters, missing required sections, duplicate titles, placeholders, unbalanced Mermaid fences |
| Link validator | broken relative Markdown links and missing local targets |
| Java validator | missing JDK, source-layout drift, compiler errors, and smoke-test failures |
| Strict site build | invalid MkDocs configuration, plugin failures, and documentation warnings |
| Book build | Pandoc, DOCX, PDF, template, and artifact-generation failures |

## Editorial checklist

- Define the problem before naming a pattern.
- State the invariant and why it remains true.
- Separate correctness from optimization.
- Include edge cases and common wrong approaches.
- Explain time and auxiliary-space complexity.
- Connect the interview abstraction to production engineering.
- Prefer a diagram when state or control flow is hard to understand linearly.

## Java checklist

- Compile with Java 17.
- Match public class names and filenames.
- Keep packages under the project namespace.
- Avoid hidden global state.
- Use overflow-safe arithmetic where required by the contract.
- State mutation behavior.
- Validate invalid input when silent corruption would be misleading.
- Exercise representative behavior in the smoke suite.

## Review standard

Automation establishes a baseline; it does not prove that an explanation is correct or that an algorithm covers every contract. Pull-request review remains responsible for technical reasoning, pedagogy, and production realism.
