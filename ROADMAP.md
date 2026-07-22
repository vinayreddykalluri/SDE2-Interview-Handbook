# Project Roadmap

This roadmap defines open-source priorities for the SDE-2 Interview Handbook. It is outcome-based rather than date-based so contributors can work independently without false release commitments.

## Current Baseline

- One repository root with GitHub-standard governance and community files.
- A primary backend interview track covering programming, LLD, HLD, data, distributed systems, reliability, cloud, security, and leadership.
- Nineteen ordered coding-foundation modules with theory, diagrams, and Java examples.
- Separate documentation, runnable code, portal, build tooling, and rendering-template layers.
- Local validation for structure, links, Java examples, portal assets, MkDocs, PDF, and DOCX generation.
- A Vercel-ready static build contract for review deployments without GitHub Actions.
- GitHub Actions intentionally disabled until the locally rendered product is approved.

## Priority 1: Curriculum Depth and Consistency

- Give every backend module the same learning flow: mental model, detailed theory, decision framework, diagram, worked example, failure modes, interview prompts, and revision checklist.
- Expand advanced LLD and HLD case studies with requirements, APIs, data models, sequence diagrams, trade-offs, and evolution paths.
- Add problem-solving drills by pattern, difficulty, constraint, and expected interview signal.
- Connect every substantial code sample to the exact documentation section that explains it.
- Add explicit prerequisites and next-module links to every track page.

## Priority 2: Executable Learning

- Add focused tests for examples with edge cases and complexity expectations.
- Add runnable reference implementations for backend concurrency, caching, messaging, persistence, resiliency, and observability patterns.
- Introduce repeatable mock-interview exercises and scorecards.
- Keep examples small enough to study independently; use dedicated sample applications only when interactions between components are the learning objective.

## Priority 3: Contributor Experience

- Label starter issues by content, code, diagrams, tooling, and accessibility.
- Add review checklists for technical accuracy, pedagogy, accessibility, and mobile behavior.
- Document how to add a module without editing unrelated layers.
- Define a lightweight release and changelog process after the curriculum stabilizes.

## Later, After Local Approval

- Re-enable GitHub Actions for validation and printable artifact generation.
- Approve and publish a production Vercel deployment; retain GitHub Pages as an optional alternative.
- Add versioned handbook releases and downloadable books.
- Evaluate additional language implementations without weakening the Java reference path.
- Add translations only after terminology and source-link stability are established.

## Definition of Ready

A module is ready when:

- its audience, prerequisites, learning outcomes, and interview signals are explicit;
- theory is technically defensible and distinguishes guarantees from implementation details;
- diagrams remain readable in light and dark themes;
- examples compile and demonstrate the stated concept;
- complexity, edge cases, alternatives, and failure modes are covered;
- navigation links work from both the portal and MkDocs;
- the local validation and build commands pass.

Propose roadmap changes through an issue before opening a broad implementation pull request. This keeps parallel contributions aligned and avoids duplicate work.
