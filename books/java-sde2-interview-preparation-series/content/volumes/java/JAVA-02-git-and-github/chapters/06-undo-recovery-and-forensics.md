# Undo, Recovery, and Repository Forensics

The safest undo command depends on which state changed and whether that state was shared. `restore`, `reset`, `revert`, and `reflog` are not interchangeable synonyms.

## Learning objectives

- select restore, reset, revert, or a new correction by state and audience;
- explain soft, mixed, and hard reset precisely;
- recover lost-looking commits with reflog;
- revert a normal commit or merge commit safely;
- preserve evidence during an incident;
- recognize when local recovery cannot restore remote or expired data.

## The undo decision map

```text
What is wrong?

Unstaged working-tree edit
  -> inspect, then git restore <path>

Staged content, but keep local edit
  -> git restore --staged <path>

Latest private commit needs repackaging
  -> amend or reset with chosen mode

Published commit must be negated
  -> git revert <commit>

Branch moved and commit seems lost
  -> git reflog, verify object, create rescue branch

Secret exposed
  -> rotate first; history cleanup is a separate coordinated incident
```

## Restore changes paths, not the current branch

Unstage while keeping the working-tree content:

```bash
git restore --staged src/main/java/example/OrderService.java
```

Restore the working-tree file from the index:

```bash
git diff -- src/main/java/example/OrderService.java
git restore src/main/java/example/OrderService.java
```

Restore from an explicit commit:

```bash
git restore --source=<commit> -- path/to/file
```

This writes content into the working tree, and optional `--staged` can update the index. It does not move the branch reference.

## Reset moves a branch or updates the index

Commit-mode reset changes the current branch tip. Think across three states:

| Mode | Branch / `HEAD` | Index | Working tree |
|---|---|---|---|
| `--soft` | move | keep | keep |
| `--mixed` | move | match target | keep |
| `--hard` | move | match target | match target |

Examples for a private latest commit:

```bash
git reset --soft HEAD^
```

The commit is removed from the branch, but its change remains staged.

```bash
git reset HEAD^
```

Mixed is the default: change remains in the working tree but becomes unstaged.

```bash
git reset --hard HEAD^
```

This overwrites tracked working-tree state to match the target and can destroy uncommitted changes. Use only after inspecting exact targets and preserving anything valuable.

Path-mode reset historically unstages paths; `git restore --staged` communicates that intent more clearly.

## Revert adds history

For a published bad commit:

```bash
git switch main
git pull --ff-only
git revert <bad-commit>
./mvnw --batch-mode verify
git push origin main
```

Revert applies the inverse change and records a new commit. It preserves the historical fact that the original change existed. Conflicts can occur if later commits modified the same area; resolve based on desired present behavior.

Reverting a revert normally reapplies the original change, but intervening history may produce conflicts or make that result inappropriate.

## Reverting a merge commit

A merge has multiple parents. Revert needs a mainline parent:

```bash
git show --no-patch --pretty=raw <merge-commit>
git revert -m 1 <merge-commit>
```

`-m 1` says to treat parent 1 as the mainline and reverse the difference introduced relative to that parent. Do not assume parent 1 without inspecting the merge and release topology.

Important consequence: reverting a merge does not erase its ancestry. A later attempt to merge the same unchanged branch may not reintroduce those commits because Git still sees them as already merged. Common strategies are to revert the revert after fixing forward, or create new commits that reapply the intended changes. Test the actual graph.

## Reflog recovery

Reference logs record local reference movements. If an interactive rebase or reset moved a branch away from a commit:

```bash
git reflog --date=iso
git show <candidate-object-id>
git branch rescue/lost-work <candidate-object-id>
```

Always create a rescue reference before attempting another rewrite. Then compare:

```bash
git log --oneline --graph --decorate --all
git diff rescue/lost-work...main
```

Reflogs are local and expire according to repository maintenance and configuration. A different clone may not have your local branch movements. Do not describe reflog as permanent backup.

## Other forensic references

Git often records temporary pointers:

- `ORIG_HEAD` may retain a prior tip for operations such as merge or reset;
- `MERGE_HEAD` identifies the commit being merged while a merge is active;
- `REBASE_HEAD` identifies the commit currently being replayed during a stopped rebase;
- `CHERRY_PICK_HEAD` identifies a stopped cherry-pick commit.

Inspect before relying on them:

```bash
git rev-parse --verify ORIG_HEAD
git show --summary ORIG_HEAD
```

These are operational aids, not a universal retention contract.

## Recovery scenarios

### Committed on the wrong branch, not pushed

Preserve the commit, move it to the correct branch, then restore the original branch:

```bash
git branch rescue/wrong-branch
git switch correct-feature
git cherry-pick rescue/wrong-branch
git switch main
git reset --hard origin/main
```

The final hard reset is appropriate only after verifying `main` should exactly match `origin/main` and all valuable work is referenced by `rescue/wrong-branch` or another branch.

### Deleted a local branch

Find its tip in the reflog or all-object history, verify it, and recreate a branch:

```bash
git reflog --all
git show <candidate>
git branch recovered-feature <candidate>
```

### Bad commit already on shared main

Do not reset and force push. Open a revert pull request or use the documented emergency path, run required checks, and preserve audit history.

### Uncommitted file overwritten

Git may have no object for content that was never staged, stashed, or committed. IDE local history, file-system snapshots, or backups may help, but Git cannot recover data it never stored.

### Object appears unreachable

`git fsck --lost-found` can identify certain dangling objects, but it is a last-resort forensic tool, not a normal workflow. Verify candidate contents and create a branch. Garbage collection may already have pruned unreachable objects.

## Incident discipline

When repository state is confusing:

1. stop making history-changing attempts;
2. capture `git status`, `git log --graph --all`, branch details, and reflog;
3. create references to valuable candidate commits;
4. clone or copy repository metadata only if policy allows and evidence preservation matters;
5. reproduce the intended graph on paper;
6. select the smallest recovery operation;
7. verify build, diff, references, and remote policy before publishing.

## Interview questions and model answers

**Reset versus revert?**

Reset moves a branch reference and optionally changes index and working tree; it is suitable for local private correction with care. Revert creates a new commit that negates an earlier change and is normally appropriate for shared history.

**How would you recover after `reset --hard`?**

Stop changing references, inspect the reflog for the old tip, verify it with `git show`, and create a rescue branch. This can recover committed content while the object and reflog entry remain. Uncommitted overwritten content may not exist in Git.

**Why is reverting a merge special?**

The inverse must be defined relative to one chosen parent, so `-m` selects the mainline. The revert changes content but preserves merge ancestry, affecting future merge behavior.

## Exercises

1. **Foundation - table:** Predict branch, index, and working-tree state after each reset mode.
2. **Foundation - lab:** Reset a private commit, recover it through reflog, and attach a rescue branch.
3. **Interview Core - decision:** Choose restore, reset, revert, or correction for six supplied states and justify each.
4. **Interview Core - merge revert:** Draw a two-parent merge, select mainline parent, and explain future remerge consequences.
5. **SDE-2 Follow-up:** Write an incident runbook for an accidental force push to a release branch, including evidence preservation and communication.

## Chapter summary

Undo is state-specific. Restore edits path state, reset moves a private branch or index, revert adds a public correction, and reflog can reconnect local committed work. Preserve evidence and create rescue references before attempting further surgery.

## Revision checklist

- [ ] I choose undo tools by state and sharing boundary.
- [ ] I can predict all three reset modes.
- [ ] I can recover a commit through reflog.
- [ ] I understand merge-revert mainline and ancestry effects.
- [ ] I know Git cannot recover content it never recorded.
