# Advanced Git at Scale: Worktrees, Bisect, Sparse Work, and Repository Boundaries

Advanced Git features solve concrete scale or diagnosis problems. Introduce them after the everyday model is stable; otherwise they create more state than they remove.

## Learning objectives

- use worktrees for concurrent branch checkouts;
- find a regression efficiently with bisect;
- reuse recurring conflict resolutions with rerere;
- compare monorepo, multi-repo, submodule, and subtree boundaries;
- understand sparse checkout and partial clone at a high level;
- manage stacked pull requests without merging dependent work accidentally.

## Worktrees for parallel tasks

One repository can have multiple linked working trees:

```bash
git worktree add ../payments-hotfix -b hotfix/payment-timeout origin/main
git worktree list
```

Use cases:

- keep a long-running feature intact while fixing production;
- run two Java versions or builds side by side;
- review another branch without stashing current work;
- prepare a release branch in isolation.

Linked worktrees share most object storage and references, but each has its own checkout and `HEAD`. A branch normally cannot be checked out in two worktrees simultaneously. Remove through Git so metadata is cleaned:

```bash
git worktree remove ../payments-hotfix
git worktree prune
```

Do not delete linked directories casually or place one worktree inside another.

## Bisect: binary search over history

If one known commit is good and a later commit is bad:

```bash
git bisect start
git bisect bad <bad-commit>
git bisect good <good-commit>
```

Git checks out a midpoint. Test it and mark:

```bash
git bisect good
# or
git bisect bad
```

After Git identifies the first bad commit:

```bash
git bisect reset
```

Automate with an exit-code-based script:

```bash
git bisect run ./mvnw --batch-mode -Dtest=PaymentRetryTest test
```

Exit 0 means good, 1-127 except 125 means bad, and 125 means skip. The test must classify the historical commits correctly. Build-system transitions, database fixtures, flaky tests, and environmental dependencies can invalidate a simplistic bisect.

Complexity is roughly logarithmic in the number of candidate commits when each test cleanly partitions the range. The test cost often dominates.

## First-parent and ancestry queries

For a merge-heavy main branch:

```bash
git log --first-parent --oneline main
```

This follows the integration spine, useful for releases and PR-level history. It intentionally hides detail inside merged branches; use the full graph for debugging.

Determine ancestry directly:

```bash
git merge-base --is-ancestor <older> <newer>
```

The exit status answers whether the first commit is an ancestor of the second, which is more robust in scripts than parsing graphical output.

## Rerere for repeated integration

On a long-lived topic branch, repeated test merges or rebases may meet the same conflict. With rerere enabled, Git records a conflict preimage and the resolved postimage. On recurrence it can apply the learned resolution.

Use it when:

- the same patch series is rebased repeatedly;
- release branches receive similar backports;
- a maintainer performs trial merges before final integration.

Still inspect `git rerere diff`, staged content, and tests. The same textual conflict shape may have a changed semantic context.

## Monorepo trade-offs

A monorepo can support atomic cross-service changes, one policy surface, unified search, and shared tooling. It can also amplify CI cost, access-control complexity, checkout size, and ownership contention.

Git concerns include:

- stable path ownership;
- build graph-aware change detection;
- required checks that always report;
- release versioning per component or repository;
- generated source and shared schema boundaries;
- repository maintenance and large-file prevention;
- avoiding one global workflow with excessive privileges.

Path filters alone do not understand Java module dependencies. A root Gradle convention plugin or shared Maven parent change may require every module.

## Sparse checkout and partial clone

Sparse checkout reduces which paths appear in the working tree. Partial clone can defer downloading selected object content according to a filter. They address different costs:

```text
sparse checkout -> working-tree population
partial clone   -> object transfer/storage behavior
```

Use repository-supported commands and current Git documentation because modes evolve. Build tools, IDE indexing, code generation, and scripts may assume a complete tree, so validate the actual Java workflow.

## Submodules

A superproject records a gitlink: a specific commit in another repository. Cloning the superproject does not turn that dependency into ordinary files in the same history.

```bash
git submodule update --init --recursive
git diff --submodule=log
```

Benefits:

- independent repository history and permissions;
- exact dependency commit recorded;
- suitable for externally versioned source under explicit ownership.

Costs:

- contributors must initialize and update separately;
- detached submodule `HEAD` and accidental pointer changes confuse workflows;
- atomic cross-repository changes require coordination;
- CI credentials and recursive operations become more complex.

A submodule pointer update should be reviewed like a dependency upgrade: inspect old-to-new commits and verify the target commit is reachable and trusted.

## Subtree and vendoring boundaries

Subtree workflows copy another project into a subdirectory while preserving some synchronization metadata through commands and commits. They simplify checkout for consumers but make synchronization and history heavier. Manual vendoring is simpler conceptually but requires explicit provenance, license, update, and security controls.

Choose between package dependency, submodule, subtree, generated source, and monorepo based on release ownership and atomic-change needs, not fashion.

## Stacked pull requests

Stacked changes split a large feature into dependent review units:

```text
main <- PR A: domain contract
         <- PR B: persistence implementation
              <- PR C: API endpoint
```

Each PR initially targets its parent branch. Risks:

- reviewer confuses inherited changes with this PR's delta;
- parent changes force restacking;
- merging out of order can duplicate or hide commits;
- squash merging a parent changes identities expected by children;
- branch protection and automation may assume `main` as base.

Make dependency order explicit, keep each layer independently understandable, use `range-diff` after restacking, and retarget children after parent merge according to the chosen merge method.

## Repository maintenance

Large active repositories may benefit from Git's maintenance facilities, commit-graph data, and background optimization. These are operational optimizations, not application correctness requirements. Coordinate with hosting, developer tooling, backup, and CI policies before tuning garbage collection or pruning; aggressive expiration can reduce recovery windows.

## Interview questions and model answers

**How does `git bisect` help find a regression?**

It checks out candidate commits between known good and bad points and narrows the first-bad boundary using test outcomes. Automation is powerful only if the test is deterministic and valid across historical build and data contracts.

**When would you use a worktree instead of stash?**

When two branch contexts must remain active, such as an urgent hotfix during a large feature. A worktree gives each branch its own index and files while sharing repository objects, reducing context churn and risky stash juggling.

**Why are submodule updates easy to under-review?**

The superproject diff may show only an old and new commit ID. Reviewers must inspect the nested commit range, provenance, build compatibility, and whether the commit is available to all consumers.

## Exercises

1. **Foundation - worktree:** Keep a feature open and create a second worktree for a hotfix.
2. **Interview Core - bisect:** Introduce a deterministic regression across eight commits and locate it automatically.
3. **Interview Core - monorepo:** Design affected-module CI for a shared Maven parent change and a leaf-only Java change.
4. **Interview Core - submodule:** Review a pointer update for code, provenance, and accessibility.
5. **SDE-2 Follow-up:** Design a three-PR stack and explain retargeting after squash-merging the parent.

## Chapter summary

Advanced features should reduce a measured cost: worktrees isolate parallel tasks, bisect narrows regressions, rerere reduces repeated conflict labor, and sparse or repository-boundary choices control scale. Each adds operational state that must be documented and verified.

## Revision checklist

- [ ] I can use and remove a linked worktree safely.
- [ ] I can automate a deterministic bisect.
- [ ] I understand rerere's benefit and semantic risk.
- [ ] I can evaluate monorepo and submodule trade-offs.
- [ ] I can manage the ancestry of stacked PRs.
