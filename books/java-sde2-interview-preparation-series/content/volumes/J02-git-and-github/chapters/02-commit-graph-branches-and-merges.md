# Commit Graphs, Branches, HEAD, and Merges

Git becomes easier when history is drawn as a graph rather than imagined as a folder of versions. Commits are nodes. Parent links are directed edges. Branches and tags are names pointing into that graph.

## Learning objectives

- explain blobs, trees, commits, annotated tags, and references;
- distinguish branch identity from commit identity;
- reason about detached `HEAD`;
- identify fast-forward, three-way, squash, and rebase outcomes;
- calculate and use a merge base;
- choose a team integration strategy deliberately.

## From snapshot to object graph

At an interview-appropriate level, Git stores:

```text
branch refs/heads/main
          |
          v
commit C: tree T3, parent B, author/committer/message
          |
          +--> tree T3
          |      +--> blob for pom.xml content
          |      +--> tree for src/
          |
          +--> commit B --> commit A
```

- A **blob** stores file content, not the filename.
- A **tree** maps names to blobs or subtrees plus modes.
- A **commit** points to one tree, zero or more parents, and metadata.
- An **annotated tag object** can point to another object and carry tagger metadata, message, and signature.
- A **reference** is a movable or fixed name for an object ID.

Content-addressing means changing tracked content changes the relevant object IDs. It does not mean a hash alone proves trusted authorship; signatures and trusted delivery controls address a different problem.

Inspect without mutating:

```bash
git cat-file -t HEAD
git cat-file -p HEAD
git ls-tree -r --name-only HEAD
git rev-parse HEAD
```

## Branch creation and switching

```bash
git switch -c feature/refund-policy
```

Conceptually:

```text
Before:
A---B  main, HEAD

After branch creation:
A---B  main, feature/refund-policy, HEAD -> feature/refund-policy

After one commit:
A---B  main
     \
      C  feature/refund-policy, HEAD
```

The files did not need to be copied into a second repository. Git changed the current reference and checked out the target tree.

Use `git branch --show-current` to ask which branch is current. `git switch` is branch-oriented; `git restore` is path-oriented. This separation is clearer than overloading older `checkout` forms.

## Detached HEAD

Checking out a commit or tag directly can detach `HEAD`:

```bash
git switch --detach <commit>
```

New commits are still valid objects, but no local branch automatically moves with them:

```text
A---B---C  main
     \
      D---E  HEAD (detached)
```

Before leaving, attach valuable work:

```bash
git switch -c rescue/investigation
```

If you already left it, the reflog chapter shows recovery.

## Merge bases

For two commits, a merge base is a best common ancestor used as the comparison baseline for a three-way merge.

```text
        C---D  feature
       /
A---B---E---F  main
    ^
    merge base of D and F
```

Ask Git:

```bash
git merge-base main feature
git diff main...feature
```

The three-dot diff describes what the feature changed since the merge base. A pull request's displayed comparison is fundamentally about the base branch and head branch around their divergence, though platform behavior and updates can affect the exact presented merge result.

## Fast-forward merge

If the current branch is an ancestor of the incoming branch, Git can move the current reference forward without creating a merge commit:

```text
Before: A---B  main
            \
             C---D  feature

After:  A---B---C---D  main, feature
```

```bash
git switch main
git merge --ff-only feature
```

Fast-forward preserves the feature commits but does not record a separate integration node.

## Three-way merge

When both sides have new commits, Git uses the merge base and both tips:

```text
      C---D  feature
     /     \
A---B---E---F  main after merge
```

The merge commit `F` has two parents. Its first parent is the branch that was checked out; its second parent is the merged tip. Parent order matters later when reverting a merge.

```bash
git switch main
git merge --no-ff feature
```

`--no-ff` asks for a merge commit even when fast-forward is possible. Whether that improves history depends on the team's release, audit, and rollback model.

## Squash merge

A squash integration applies the aggregate branch changes and records one new commit on the base branch. The new commit does not have the feature tip as a parent:

```text
      C---D  feature
     /
A---B---S  main
```

This creates compact base history but loses ancestry that would say `D` was merged. The original commits may still exist through the feature branch or platform metadata until references are deleted and objects become unreachable.

## Rebase as commit replay

Rebase finds commits unique to the current branch, computes their changes, and reapplies them on another base as new commits:

```text
Before:       C---D  feature
             /
        A---B---E---F  main

After:  A---B---E---F---C'---D'  feature
```

`C'` and `D'` have new parent relationships and therefore new IDs. Rebase does not move the original commits "as-is." It creates rewritten equivalents and moves the branch.

## Choosing an integration strategy

| Strategy | Base history | Preserves branch ancestry | Common benefit | Important cost |
|---|---|---:|---|---|
| fast-forward | linear | yes | no extra merge node | no explicit integration commit |
| merge commit | graph | yes | records integration boundary | noisier first-parent history if overused |
| squash merge | one commit per PR | no | compact, easy PR-level revert | loses individual base-branch commit ancestry |
| rebase then merge | linear rewritten commits | yes after fast-forward | clean per-commit story | rewritten IDs require coordination |

There is no universally best strategy. Decide based on how the team reviews, debugs, audits, releases, reverts, and preserves contributor authorship.

## Semantic conflicts can occur without markers

Git merges text. It does not prove Java behavior. Two branches may edit different files and merge cleanly while violating a contract:

- one branch changes `equals`, another changes fields used by `hashCode`;
- one renames a configuration property, another adds a consumer of the old name;
- one migration makes a column non-null, another writes null;
- one changes an interface, another compiles only under a stale generated source;
- one adds a lock, another introduces a second lock in reverse order.

A clean merge still requires compilation, tests, static analysis, and human reasoning.

## Interview questions and model answers

**What is a branch internally?**

A normal local branch is a reference under `refs/heads` that points to a commit. New commits normally advance the current branch reference.

**What is the merge base used for?**

It is a best common ancestor that provides the original version for a three-way comparison against both tips. It helps determine what each side changed since divergence.

**Does squash merge equal rebase?**

No. Squash produces one aggregate commit on the base and does not preserve the feature commits as parents. Rebase replays each selected commit on a new base, producing rewritten per-commit identities.

**Why can a conflict-free merge still be wrong?**

Git detects certain textual and tree conflicts, not violated application invariants. Independent changes can be syntactically mergeable but semantically incompatible.

## Exercises

1. **Foundation - drawing:** Draw branch names before and after a fast-forward.
2. **Foundation - prediction:** What changes when you commit in detached `HEAD`?
3. **Interview Core - graph:** Given two diverged tips, identify the three inputs to a three-way merge.
4. **Interview Core - comparison:** Choose merge, squash, or rebase for a regulated repository and justify the audit and rollback consequences.
5. **SDE-2 Follow-up:** Create an example of a clean textual merge that breaks a Java invariant. Name the test that should catch it.

## Chapter summary

Commits form a parent graph; branches are movable names into it. Fast-forward, merge, squash, and rebase produce materially different graphs. Draw the before and after graph before selecting an integration command.

## Revision checklist

- [ ] I can explain Git's core object types at an interview level.
- [ ] I can reason about attached and detached `HEAD`.
- [ ] I can find and explain a merge base.
- [ ] I can draw merge, squash, and rebase outcomes.
- [ ] I know that textual success is not behavioral correctness.
