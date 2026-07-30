# Rebase, Cherry-Pick, and Intentional History Design

History-editing commands are powerful because they create new commits or move references. Use them to improve private, owned history or to transfer a well-defined change. Do not treat a pleasing graph as more important than coordination and traceability.

## Learning objectives

- explain rebase as replay rather than movement;
- update an owned feature branch safely;
- use interactive rebase to reorder, squash, fix, or split private commits;
- cherry-pick a specific change with provenance;
- compare merge, rebase, and cherry-pick trade-offs;
- recover or abort when rewriting does not produce the intended behavior.

## Rebase mechanics

Suppose:

```text
      C---D  feature
     /
A---B---E---F  main
```

Running this while on `feature`:

```bash
git rebase main
```

conceptually performs:

1. find the merge base `B`;
2. identify branch commits `C` and `D`;
3. temporarily detach the branch from those commits;
4. check out `F` as the new base;
5. replay changes for `C`, creating `C'`;
6. replay changes for `D`, creating `D'`;
7. move `feature` to `D'`.

```text
A---B---E---F---C'---D'  feature
```

Author identity may be preserved, while committer metadata, parent, tree, or message can change. The commit IDs therefore change.

## Safe branch update workflow

For a feature branch that only you own:

```bash
git status --short
git fetch origin
git branch backup/feature-before-rebase
git rebase origin/main
./mvnw --batch-mode verify
git range-diff origin/main...backup/feature-before-rebase \
               origin/main...feature/order-idempotency
git push --force-with-lease origin feature/order-idempotency
```

The backup reference is cheap and removable later. `range-diff` compares two versions of a patch series and is useful after a nontrivial rebase. It does not replace tests or review.

## Resolve, skip, or abort

When replay stops:

```bash
git status
git rebase --show-current-patch
```

After resolving and staging:

```bash
git rebase --continue
```

Use `--skip` only when the current patch is genuinely already present or intentionally omitted. Skipping because a conflict is difficult can silently lose behavior. Use `git rebase --abort` to return to the original branch state when the plan is wrong.

## Interactive rebase

To edit the latest four private commits:

```bash
git rebase -i HEAD~4
```

The interactive instruction list is processed from oldest to newest:

```text
pick a1b2c3 Add retry policy
fixup d4e5f6 Fix typo
reword 112233 Add timeout metric
edit 445566 Combine configuration sources
```

Common actions:

- `pick`: replay unchanged;
- `reword`: change the message;
- `edit`: pause after applying so content can change;
- `squash`: combine with previous commit and edit combined message;
- `fixup`: combine and normally discard this message;
- `drop`: omit the commit.

To split at an `edit` stop:

```bash
git reset HEAD^
git add -p
git commit -m "Add timeout configuration"
git add -p
git commit -m "Record timeout metric"
git rebase --continue
```

This mixed reset moves the branch one commit back while retaining the changes in the working tree. Verify state before every staging step.

## Rebase onto for transplanting a branch

If a branch was based on the wrong parent:

```text
        P---Q  old-base
             \
              R---S  feature

A---B---C  desired-base
```

Use the old base as the cut point and the desired base as the destination:

```bash
git rebase --onto desired-base old-base feature
```

The commits reachable from `feature` but not `old-base` are replayed onto `desired-base`. Draw the graph and verify the selected range first:

```bash
git log --oneline old-base..feature
```

## Cherry-pick

Cherry-pick applies the change introduced by selected commit(s) to the current branch and records new commit(s):

```bash
git switch release/2.8
git cherry-pick -x <fix-commit>
```

`-x` adds a reference to the original commit ID when the cherry-pick succeeds without conflict, which helps hotfix traceability. The new commit still has a new parent and ID.

Good uses include:

- backporting a focused fix to a maintained release branch;
- transferring one independent commit from an abandoned experiment;
- reconstructing a branch from known-good changes.

Weak uses include:

- copying an entire feature instead of merging or rebasing its branch;
- repeatedly synchronizing long-lived branches through duplicate commits;
- selecting a patch without its prerequisite schema or configuration changes.

## Dependencies between commits

A commit that compiles alone is easier to reorder or backport. Before cherry-picking, inspect:

```bash
git show --stat --summary <commit>
git show <commit>
git branch --contains <commit>
```

Then identify dependencies:

- Does the Java type already exist on the target branch?
- Does the database migration exist?
- Are compatible library versions present?
- Is a feature flag or configuration property required?
- Does the fix assume a later API contract?

If the dependency set is large, a release-specific implementation may be safer than a chain of cherry-picks.

## Public versus private history

Use this decision:

```text
Has the commit been shared?
  no  -> rewriting is usually locally safe; still verify
  yes -> does the branch have explicit coordinated rewrite ownership?
           yes -> rebase and force-with-lease under policy
           no  -> preserve history; merge, revert, or add a correction
```

Protected default and release branches should normally reject force pushes. Local elegance is not worth invalidating another engineer's references, reviews, build provenance, or release evidence.

## Interview questions and model answers

**What is the difference between merge and rebase?**

Merge combines histories and normally creates a two-parent commit when they diverged, preserving existing commit identities. Rebase replays selected commits onto a new base, producing new identities and a linearized branch. I choose based on ownership, audit needs, review, and team policy.

**When would you cherry-pick?**

For a focused independent change that must be applied to another line of development, such as a release backport. I inspect prerequisites, use `-x` when provenance matters, test on the target branch, and avoid using cherry-pick as a general synchronization strategy.

**How do you verify a complicated rebase?**

Preserve a backup reference; inspect the selected commit range; resolve each stop deliberately; compare old and new patch series with `range-diff`; run the required build and tests; review the final graph and diff; then update the owned remote branch with force-with-lease.

## Exercises

1. **Foundation - graph:** Draw the commit IDs that change during rebase and the ones that remain.
2. **Interview Core - lab:** Reorder and squash three private commits, then compare old and new ranges.
3. **Interview Core - debugging:** A developer used `rebase --skip` to escape a conflict and lost validation. Show how a backup branch and range comparison reveal it.
4. **Interview Core - backport:** List the dependency checks before cherry-picking a Spring Boot fix into an older release.
5. **SDE-2 Follow-up:** Propose history rules for default, release, feature, and automated dependency-update branches.

## Chapter summary

Rebase and cherry-pick create new commits from existing changes. Use rebase for owned patch-series design and cherry-pick for deliberate transfer, with graph inspection, provenance, tests, and coordination guarding every rewrite.

## Revision checklist

- [ ] I can explain exactly why rebase changes IDs.
- [ ] I can use continue, skip, and abort intentionally.
- [ ] I can split a private commit with interactive rebase.
- [ ] I inspect prerequisites before cherry-picking.
- [ ] I do not rewrite shared branches casually.
