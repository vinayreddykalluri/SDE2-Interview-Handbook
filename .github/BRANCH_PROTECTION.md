# Default Branch Protection

The `master` branch is the protected publication source for the handbook, website, examples, and book artifacts. GitHub stores the active protection configuration as repository settings; this document records the intended policy so maintainers and contributors can audit it.

## Required controls

Changes to `master` must satisfy all of these controls:

- use a pull request;
- pass the required GitHub Actions `validate` check;
- be current with `master` before merge;
- receive at least one approving review;
- receive the required review from the repository code owner;
- dismiss approvals when reviewable code changes;
- require approval after the most recent reviewable push;
- resolve every pull-request conversation;
- preserve linear history;
- reject force pushes; and
- reject branch deletion.

[`CODEOWNERS`](CODEOWNERS) assigns the complete repository to `@vinayreddykalluri`, the founding author, Editor-in-Chief, and Chief Auditor. Consequently, a contributor cannot merge a pull request without Vinay's approval.

## Administrator boundary

Vinay Reddy Kalluri is the repository's only administrator. Administrator enforcement is intentionally disabled so the owner retains an emergency recovery path and can merge owner-authored work, which GitHub does not allow the author to approve personally.

Do not grant another account administrator access without first reassessing this exception. A future administrator could otherwise use the same bypass. Write and maintain permissions do not bypass the required code-owner review.

If unconditional administrator enforcement becomes necessary, enable it only after adding a second trusted reviewer; otherwise owner-authored pull requests can become impossible to merge.

## Validation availability

The `validate` job runs for every pull request targeting `master`, not only book-path changes. This guarantees that the required check is created for documentation, community-health, application, tooling, and book contributions alike.

## Audit procedure

Repository administrators can verify the live policy with:

```bash
gh api repos/vinayreddykalluri/SDE2-Interview-Handbook/branches/master \
  --jq '{name: .name, protected: .protected}'

gh api repos/vinayreddykalluri/SDE2-Interview-Handbook/branches/master/protection \
  --jq '{
    checks: .required_status_checks,
    reviews: .required_pull_request_reviews,
    conversations: .required_conversation_resolution.enabled,
    linear_history: .required_linear_history.enabled,
    force_pushes: .allow_force_pushes.enabled,
    deletions: .allow_deletions.enabled,
    admin_enforcement: .enforce_admins.enabled
  }'
```

The live GitHub setting is authoritative. If it diverges from this document, treat that as a repository-integrity issue and restore or deliberately revise both the setting and policy through an auditable change.
