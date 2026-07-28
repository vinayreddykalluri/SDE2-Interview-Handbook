# Project Governance

## Mission

The SDE2 Interview Handbook exists to provide accurate, practical, freely reusable preparation material for experienced software engineers.

## Editorial leadership

### Editor-in-Chief — Vinay Reddy Kalluri

The Editor-in-Chief owns curriculum sequence, publication scope, voice, accessibility, contributor credit, and final editorial decisions across the handbook and published books.

### Chief Auditor — Vinay Reddy Kalluri

The Chief Auditor owns Java and API accuracy, executable evidence, complexity claims, PDF quality, attribution checks, and release-readiness approval. The auditor may block a release for unresolved correctness, licensing, privacy, build, or readability defects.

The same person may hold both roles, but editorial and technical-audit responsibilities remain distinct release gates.

## Community roles

- Maintainers set project direction, merge changes, publish releases, and enforce community policies.
- Contributors improve content, code, diagrams, automation, accessibility, and review quality.
- Reviewers provide evidence-based technical and editorial feedback.

## Decision making

Routine changes are decided through pull-request review. Significant changes to licensing, chapter architecture, supported Java versions, or publishing systems should begin with a public issue so trade-offs can be discussed before implementation.

The default branch follows the repository's [documented protection policy](https://github.com/vinayreddykalluri/SDE2-Interview-Handbook/blob/master/.github/BRANCH_PROTECTION.md). Contributor changes require a pull request, successful validation, and approval from the Editor-in-Chief and Chief Auditor through the repository code-owner rule.

The Editor-in-Chief has final editorial responsibility. The Chief Auditor has final technical release-gate responsibility. Decisions should be documented in issues or pull requests and favor technical correctness, learner value, maintainability, individual credit, and open access.

## Becoming a maintainer

Consistent contributors may be invited to maintain areas where they have demonstrated technical judgment, respectful review, and reliable follow-through. Permissions are granted incrementally and may be removed for inactivity, security reasons, or policy violations.

## Releases

The default branch is the continuously published source of truth. Tagged releases may be used for stable book snapshots. GitHub Actions provides reproducible site and book artifacts from committed source.

Published book sources and release PDFs live under `books/java-sde2-interview-preparation-series/`. Individual credit follows the [authorship record](authors.md), Git history, and accepted pull requests.

### Book release gates

A changed volume is publishable only when prerequisites precede dependent concepts, valid complete Java examples compile under the declared baseline, invalid snippets are isolated and labeled, affected PDFs build, and representative contents/code/table/diagram pages are inspected. Accuracy, attribution, accessibility, manifest integrity, and navigation are independent release gates; a successful PDF build alone is not approval.

## Changes to governance

Governance changes use the normal pull-request process and should clearly explain the motivation and community impact.
