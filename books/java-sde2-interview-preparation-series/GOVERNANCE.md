# Editorial Governance

## Purpose

This project is an open educational publication with transparent individual credit and a consistent technical standard. Community participation is encouraged; publication decisions remain accountable to named editorial leadership.

## Roles

### Editor-in-Chief — Vinay Reddy Kalluri

The Editor-in-Chief is responsible for:

- the basics-to-advanced learning sequence;
- book scope, structure, voice, accessibility, and reader experience;
- deciding whether material belongs in the current volume or a cross-referenced volume;
- resolving editorial conflicts and approving publication-level authorship credit;
- approving release composition, titles, covers, and roadmap changes; and
- protecting the project from unnecessary duplication or framework drift.

### Chief Auditor — Vinay Reddy Kalluri

The Chief Auditor is responsible for:

- Java language and API accuracy;
- clear separation of specification guarantees, implementation behavior, and engineering guidance;
- compilable examples, documented output, and isolated invalid snippets;
- complexity, overflow, concurrency, collection, and production-contract review;
- PDF readability, navigation, build reproducibility, and release evidence; and
- final readiness approval after required checks pass.

The two roles may be held by the same person, but each review responsibility must still be applied explicitly.

### Maintainers

Maintainers may triage issues, review pull requests, run checks, and merge work within delegated areas. Maintainer status is based on sustained, accurate, constructive contributions and may be revised by the Editor-in-Chief.

### Authors and contributors

Authors contribute substantial original educational content. Contributors may also improve examples, exercises, diagrams, tooling, accessibility, or accuracy. Individual credit follows [AUTHORS.md](AUTHORS.md) and Git history.

## Decision process

1. Discuss material changes in an issue before writing a large chapter or changing the learning path.
2. Prefer evidence from Java specifications, Java SE APIs, OpenJDK JEPs, official project documentation, or original executable tests.
3. Review educational quality separately from build correctness.
4. Request changes when an explanation is accurate but poorly sequenced, or readable but technically unsupported.
5. The Editor-in-Chief makes the final editorial decision.
6. The Chief Auditor can block a release for unresolved correctness, attribution, validation, security, or publishing defects.

Decisions should include the reason, affected volume, evidence, and follow-up needed. Disagreement is welcome when it is specific and evidence-based.

## Release gates

A changed volume is publishable only when:

- prerequisites appear before dependent concepts;
- the source contains no draft markers or accidental placeholders;
- valid complete Java examples compile under the declared baseline;
- intentionally invalid snippets are labeled and isolated;
- expected output matches execution where practical;
- affected PDFs build successfully;
- contents, code, tables, diagrams, links, and representative pages are inspected;
- individual authorship and third-party attribution are correct; and
- audit, coverage, validation, changelog, or build evidence is updated in proportion to the change.

## Attribution and licensing

Contributors retain copyright in their original contributions and license accepted work under the repository's applicable license. Pull requests must not include material that the contributor lacks permission to share. The project may edit accepted work for consistency while preserving individual credit.

## Amendments

Governance changes use the normal pull-request process and require explicit approval from the Editor-in-Chief. Changes affecting authorship or licensing must be called out prominently and cannot be hidden inside unrelated edits.
