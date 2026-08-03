# Realistic Git and GitHub Interview Rounds with Model Answers

These are complete interview-room simulations. Answer by inspecting state, drawing the graph, stating the sharing boundary, choosing the smallest safe operation, and describing verification. Commands without reasoning are incomplete answers.

## Round 1: staged and unstaged changes in one Java file

**Interviewer:** You changed `OrderService.java`, staged it, then added debug logging. What enters the next commit?

**Candidate:** The index contains the version as of `git add`; later working-tree edits are not automatically staged. I would confirm with `git status --short`, `git diff --staged`, and `git diff`. The file should show `MM` if both staged and unstaged versions differ from their baselines.

**Interviewer:** How do you commit the behavior change without the logs?

**Candidate:** If the staged version already contains only behavior, I review `git diff --staged`, run tests against the intended snapshot where practical, and commit. If edits are mixed, I unstage with `git restore --staged`, use `git add -p`, inspect again, and commit. I do not discard the logs unless they are no longer needed.

**Strong signal:** distinguishes index from working tree and inspects both diffs.

## Round 2: merge versus rebase

**Interviewer:** Your feature branch is behind main. Should you merge main or rebase?

**Candidate:** First I ask whether the branch is shared, whether repository policy requires one history shape, and whether current reviews/build provenance depend on its commit IDs. A merge preserves existing IDs and adds an integration commit when histories diverged. A rebase replays feature commits on current main, creating new IDs and requiring a coordinated force-with-lease update. For my private branch, rebase can make the patch series easier to review; for a shared branch, merge is safer unless the team explicitly coordinates rewrites. Either way I fetch, draw or inspect the graph, resolve intentionally, run checks, and inspect the final diff.

**Interviewer:** Which is more correct?

**Candidate:** Neither universally. Correctness is the resulting behavior and adherence to collaboration policy. History design is a trade-off involving audit, bisect, rollback, and review.

## Round 3: recover a lost commit

**Interviewer:** You ran `git reset --hard HEAD~3` and lost three committed changes. What do you do?

**Candidate:** I stop changing references. I inspect `git reflog --date=iso`, identify the old branch tip, verify it with `git show` and the graph, and immediately create `git branch rescue/lost-work <sha>`. Then I decide whether to reset the private branch back, cherry-pick selected commits, or compare the rescue branch. This can recover committed content while reflog/object retention remains. It may not recover uncommitted content overwritten by the hard reset.

**Interviewer:** Why not reset back immediately?

**Candidate:** A rescue reference preserves evidence before another mutation. It reduces the chance that a mistaken second reset makes the state harder to understand.

## Round 4: published bad commit

**Interviewer:** A faulty commit is on protected main and already deployed. Reset or revert?

**Candidate:** Normally revert through the protected emergency process because it adds an auditable inverse commit without rewriting shared history. But Git action is only one layer: I assess user impact, whether deployment rollback is faster, schema compatibility, external side effects, and whether a feature flag can contain it. I run the required checks on the actual revert and verify production. If it was a merge commit, I inspect parent order before selecting `-m`.

**Interviewer:** Would `revert` always restore production?

**Candidate:** No. Data, schema, messages, caches, and external effects can persist. Source revert, artifact rollback, and data recovery are separate decisions.

## Round 5: conflict resolution

**Interviewer:** Both branches changed the payment timeout line. How do you resolve it?

**Candidate:** I first identify whether the operation is merge, rebase, cherry-pick, or revert. I inspect the base and both sides, then understand intent: perhaps one branch lowered a default while the other made the value configurable. The correct combined code may preserve both changes rather than accept one side. I remove markers, inspect `git diff --check` and staged result, run focused and full required tests, then use the operation-specific continue command. If intent is unclear, I abort and ask the owners rather than guess.

**Interviewer:** What if no conflict markers appear but tests fail?

**Candidate:** That is a semantic conflict. Git merged text, but the combined Java contract is inconsistent. I diagnose from failing behavior and add a regression test so future integration detects it.

## Round 6: secure GitHub Actions

**Interviewer:** Review this workflow: it runs on `pull_request_target`, checks out the PR SHA, and runs Maven with repository secrets.

**Candidate:** That is a critical trust-boundary flaw. `pull_request_target` is privileged relative to an untrusted fork. Checking out and executing the fork's wrapper or build scripts can exfiltrate secrets or write to the repository. I would disable that path, rotate potentially exposed credentials, audit writes/artifacts/caches/runners, move compilation to a read-only `pull_request` workflow without secrets, and keep any privileged metadata or deployment workflow separate. I would also declare minimal token permissions and pin actions to verified full SHAs.

**Interviewer:** Can we just review the PR before running it?

**Candidate:** Manual approval can reduce exposure but is not a sufficient design for arbitrary privileged code execution. The architecture should keep untrusted code out of privileged contexts.

## Round 7: branch protection design

**Interviewer:** Design controls so production main cannot merge without owner approval.

**Candidate:** I would use a ruleset or branch protection requiring pull requests, a code-owner approval covering all paths, current unique required checks, resolved conversations, and no force push or deletion. I would protect CODEOWNERS and workflow files with security/platform ownership. Bypass would be limited to a small audited emergency role, not every admin by habit. If throughput is high, I would add a merge queue and ensure required Actions run on `merge_group`. I would test contributor, owner, bot, stale-approval, workflow-change, and emergency cases before trusting the policy.

**Interviewer:** Why not require five approvals?

**Candidate:** Approval count should match risk and staffing. An impossible rule encourages bypass and delays incidents. Coverage, independence, expertise, and current evidence matter more than raw count.

## Round 8: hotfix with divergent main

**Interviewer:** Production 2.8 is broken, but main contains unfinished 2.9. Walk me through the hotfix.

**Candidate:** I identify the exact production tag and artifact, branch from that tag or the maintained 2.8 line, write the smallest fix plus regression test under the 2.8 toolchain, and use the protected emergency PR. I publish a new version rather than moving the old tag, verify artifact provenance and production metrics, then explicitly forward-port the invariant to main. If main changed the schema or architecture, I implement an equivalent fix instead of blindly cherry-picking.

**Interviewer:** What if a database migration is involved?

**Candidate:** I assess old/new application overlap, lock and backfill risk, rollback compatibility, and whether a forward fix is safer. Code rollback alone may be invalid after a destructive migration.

## Round 9: force-with-lease

**Interviewer:** Why use `--force-with-lease` after rebase?

**Candidate:** Rebase created new IDs, so a normal push is non-fast-forward. A lease asks the remote to update only if its current branch value matches my expected remote-tracking value, reducing the risk of overwriting an unseen teammate commit. I fetch and inspect first, and use it only on a branch with explicit rewrite ownership. It is not permission to rewrite main or a shared branch.

**Interviewer:** Is it race-free?

**Candidate:** It provides a conditional update, but safety still depends on the expected value and local remote-tracking state. Background fetch behavior and ownership matter. Protected server-side rules remain essential.

## Round 10: bisect a Java regression

**Interviewer:** A latency regression appeared somewhere in 300 commits. How would you use bisect?

**Candidate:** I identify a known-good and known-bad commit under comparable environment and data. I create a deterministic test or benchmark threshold with exit codes, then run `git bisect start`, mark bad and good endpoints, and use `git bisect run`. I validate the candidate first-bad commit manually. If build formats or fixtures changed across the range, the script must handle them or return skip. For performance, I control JVM warmup, machine noise, and statistical variance; a naive single timing is not reliable.

**Interviewer:** What is the complexity?

**Candidate:** Roughly logarithmic candidate selections when the predicate cleanly partitions history, multiplied by the cost and repetitions of the test.

## Round 11: green PR, red merge queue

**Interviewer:** Two green PRs fail when queued together. Is the queue broken?

**Candidate:** Not necessarily. Each was tested against a prior base; their combination may create a semantic conflict. I inspect the merge-group revision and failure, identify which contract crossed the PRs, fix or reorder the responsible change, and add an integration test. If no queue workflow ran, I check that required Actions listen to `merge_group` and that job names align with rules.

## Round 12: large monorepo CI

**Interviewer:** How would you avoid testing every Java module for every change?

**Candidate:** I would derive affected modules from both changed paths and the build dependency graph. A leaf-only source change can test the leaf and dependents; a root parent, shared plugin, schema, or toolchain change may fan out to all. A stable required gate always reports and records why modules were selected. I would periodically run full builds to detect gaps and treat the change-selection program as production logic with tests.

## Round 13: mutable release tag

**Interviewer:** Someone moved `v2.8.0` to a new commit after publishing. Why is that serious?

**Candidate:** Consumers, caches, SBOMs, attestations, deployments, and audit evidence may now associate one version with different bytes or sources. I would freeze publication, determine every artifact and deployment identity, restore or deprecate according to policy, publish a new version, notify consumers, and protect tags or enable immutable release controls. Published version identities must not be reused.

## Round 14: code-owner bottleneck

**Interviewer:** CODEOWNERS requires one engineer who is on vacation; a critical fix is blocked.

**Candidate:** Ownership should normally target staffed teams, not a single individual. For the current incident I use the documented limited bypass or alternate owner with audit and retrospective. Then I repair ownership coverage, backup rotations, and escalation without weakening review for sensitive paths.

## Candidate answer framework

For an unfamiliar Git scenario, speak in this order:

1. current operation and exact state;
2. local versus shared history boundary;
3. commit graph before and intended after;
4. smallest safe command or GitHub control;
5. Java build, test, schema, and production verification;
6. recovery or abort path;
7. preventive policy.

## Interviewer scoring rubric

| Signal | Weak | Strong |
|---|---|---|
| state | jumps to a command | inspects working tree, index, refs, graph, remote |
| safety | uses force or hard reset casually | preserves work and identifies destructive boundary |
| collaboration | ignores shared users | distinguishes owned and shared history |
| Java delivery | stops at Git success | verifies build, tests, migration, compatibility |
| GitHub security | trusts green badge | examines trigger, code, permissions, identity, context |
| communication | memorized command list | graph, trade-off, rollback, and evidence |

## Chapter summary

SDE-2 answers connect command mechanics to team and production outcomes. State the graph, sharing boundary, risk, verification, and recovery before presenting a command.

## Revision checklist

- [ ] I can answer each round aloud without notes.
- [ ] I draw history when identity or ancestry matters.
- [ ] I include Java and production verification.
- [ ] I identify GitHub workflow trust boundaries.
- [ ] I provide a recovery path, not only the happy path.
