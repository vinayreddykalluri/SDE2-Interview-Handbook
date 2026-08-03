# GitHub Governance: Rulesets, CODEOWNERS, Reviews, and Merge Queues

Good repository governance turns team expectations into enforceable controls while preserving an auditable emergency path. A rule is useful only if its scope, bypass policy, required checks, and ownership model match the delivery risk.

## Learning objectives

- compare classic branch protection and rulesets;
- design required reviews, status checks, and conversation resolution;
- use CODEOWNERS as routing and approval policy;
- understand linear history, signed commits, deployment gates, and merge queues;
- avoid governance deadlocks and unsafe bypasses;
- propose a protected `main` policy for a Java service.

## Policy layers

```text
organization / enterprise policy
            |
repository rulesets and Actions policy
            |
branch or tag rules
            |
pull request approvals and required checks
            |
merge or deployment authorization
```

GitHub rulesets can target branches or tags, and multiple applicable rulesets can layer. Classic branch protection may also coexist. Effective behavior is governed by applicable controls, commonly with stricter requirements winning. Test policies in a noncritical repository or evaluate mode before relying on assumptions.

## A practical `main` policy

For a production Java service, consider:

- require a pull request before merge;
- require one or two approvals based on risk;
- require code-owner review for owned areas;
- dismiss stale approvals or require approval of the most recent reviewable push;
- require all review conversations to be resolved;
- require current status checks such as compile, tests, static analysis, and dependency review;
- require the branch to be current or use a merge queue;
- block force pushes and deletion;
- restrict bypass to a small accountable role or team;
- require signed commits only when the organization can support every merge and automation path;
- require successful deployments when environment promotion is part of merge governance.

Do not require a check whose job name collides with another workflow. Required status check job names should be unique and stable.

## Rulesets versus classic branch protection

| Capability | Rulesets | Classic protection |
|---|---|---|
| multiple policies can layer | yes | classic rules can have matching limitations |
| evaluate policy before enforcement | available for rulesets where supported | not the same model |
| target branches/tags with rule conditions | rich ruleset targeting | branch-name pattern protection |
| bypass actors and visibility | ruleset-specific management | protection-specific management |
| established compatibility | newer policy model | widely used legacy model |

The product evolves, so use current GitHub documentation and repository UI when implementing. The design principles remain stable: default deny destructive updates, require independent evidence, minimize bypass, and audit exceptions.

## CODEOWNERS

CODEOWNERS maps paths to GitHub users or teams:

```text
# Default review route
*                         @acme/java-platform

# More specific ownership
/src/main/java/payments/  @acme/payments
/db/migration/            @acme/database @acme/payments
/.github/workflows/       @acme/platform-security
/CODEOWNERS               @acme/platform-security
```

GitHub searches supported locations such as `.github/`, repository root, or `docs/`, according to documented precedence. The CODEOWNERS file from the pull request's base branch governs review requests. Protect the CODEOWNERS file itself so contributors cannot silently reroute ownership.

CODEOWNERS is path-based review routing. It does not prove expertise, replace general review, or guarantee a required approval unless the branch/ruleset policy requires code-owner review.

Avoid ownership patterns so broad that every PR needs five teams, or so narrow that unowned sensitive paths slip through. Use teams rather than individual accounts for organizational continuity.

## Required reviews and stale approvals

A useful policy answers:

- How many independent reviewers?
- Does the author count? Usually not for independent review.
- Are approvals dismissed after new commits?
- Must the last push be approved by someone else?
- Can code owners approve only their relevant files or the whole PR?
- What happens if a reviewer leaves or a team is unavailable?
- Which role can bypass during a production incident, and how is it audited?

Overly rigid review can encourage unsafe admin bypass. Underpowered review can turn approvals into rubber stamps. Match controls to repository criticality and staff an escalation route.

## Required status checks

A check should be deterministic, appropriately scoped, and difficult to spoof. Typical Java gates:

```text
compile-jdk-17
test-jdk-21
integration-test
static-analysis
dependency-review
migration-validation
```

Keep job names unique. If a workflow is renamed, update rules only after the replacement check reports successfully on the target branch. A required check that never runs can deadlock merging.

Do not use path filters in a way that causes required workflows to remain permanently pending when their paths do not match. One design is a required gate job that always runs, computes scope, and reports success only after all applicable jobs finish.

## Merge queue

A merge queue evaluates pull requests in an ordered, up-to-date merge context instead of making every author repeatedly update the branch. It is valuable for a busy protected branch where individually green PRs can conflict when combined.

GitHub Actions workflows that provide required checks for queued merges must listen for the merge-group event as documented:

```yaml
on:
  pull_request:
  merge_group:
```

If required checks run only on `pull_request`, the queue-created merge group may never receive them and merging stalls.

Operational questions:

- What happens when the front item fails?
- Are checks batched or run per synthetic group?
- How are flaky tests handled without hiding real failures?
- Which changes are urgent enough for priority or bypass?
- Does deployment happen for the queued result or only after base-branch merge?

## Linear history and merge method compatibility

Requiring linear history prevents merge commits from being pushed to the protected branch. Ensure enabled GitHub merge methods are compatible, such as squash or rebase merge. If audit policy requires explicit merge commits, do not simultaneously require linear history.

Policy controls must agree with each other.

## Signed commits and sign-off are different

A cryptographic signature can let GitHub verify that a supported signing key signed a commit or tag. A sign-off line is a textual attestation often used for a Developer Certificate of Origin process. One is not a substitute for the other.

Before requiring signed commits, test:

- local developer signing;
- web-based edits and merges;
- bot and dependency-update commits;
- release automation;
- key rotation, expiry, revocation, and offboarding;
- the selected GitHub merge method.

## Bypass and emergency access

An emergency bypass should be:

- limited to named roles or teams;
- unavailable by default to ordinary contributors;
- used only under a documented incident condition;
- accompanied by issue/incident evidence and retrospective review;
- followed by restoration of normal controls;
- monitored so habitual bypass becomes a process defect.

"Administrators can always fix it" is not governance. It is an unmeasured exception path.

## Interview questions and model answers

**How would you protect `main` so nobody merges without the repository owner's approval?**

Require pull requests, require code-owner or specifically accountable team review on all paths, restrict bypass, block force pushes and deletion, require current checks and conversation resolution, and protect the CODEOWNERS/rules configuration. On plans that support it, use rulesets for targeted enforcement and audit. Then test the policy using non-owner, owner, bot, and emergency paths; do not assume UI configuration works as intended.

**Why use a merge queue?**

It validates queued changes in a current combined base context, reducing the race where several PRs are independently green but fail together. CI must support the queue's merge-group event and the team must define failure and priority behavior.

**What can go wrong with required checks?**

Duplicate job names can create ambiguity, path-filtered workflows may never report, renamed checks can deadlock the branch, flaky tests can block throughput, and a privileged workflow can make a green check untrustworthy.

## Exercises

1. **Foundation - policy:** Write a minimal protected-branch rule set for an open-source Java library.
2. **Interview Core - ownership:** Design CODEOWNERS for application, database migration, build, workflow, and ownership files.
3. **Interview Core - deadlock:** Diagnose a required check that remains expected because its workflow was skipped by path filters.
4. **Interview Core - merge queue:** Explain why a PR-only workflow fails in a merge queue and correct the trigger.
5. **SDE-2 Follow-up:** Design a two-person emergency bypass with audit evidence, time bounds, and post-incident restoration.

## Chapter summary

Governance should enforce independent review and current evidence without creating contradictory or unstaffed rules. Protect the policy files, keep checks unique and reliable, support merge-queue events, minimize bypass, and test the complete contributor and automation paths.

## Revision checklist

- [ ] I can compare rulesets and classic protection.
- [ ] I can design practical CODEOWNERS coverage.
- [ ] I can explain stale approvals and unique required checks.
- [ ] I can configure CI for a merge queue conceptually.
- [ ] I can distinguish signed commits from sign-off.
