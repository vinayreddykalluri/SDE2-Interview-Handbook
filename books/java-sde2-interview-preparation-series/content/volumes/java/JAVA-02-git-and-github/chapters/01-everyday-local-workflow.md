# Everyday Local Git: Stage, Commit, Inspect, and Amend

The everyday Git loop is small: understand the task, inspect the repository, create a branch, edit, stage deliberately, verify, commit, and inspect history. Most team problems begin when one of those checks is skipped.

## Learning objectives

- distinguish tracked, untracked, staged, and ignored files;
- stage whole files or selected hunks;
- write reviewable commits and messages;
- amend a private commit safely;
- use diff and log options to answer specific questions;
- handle temporary work without turning stash into permanent storage.

## Start every task from known state

```bash
git status --short --branch
git branch --show-current
git log -5 --oneline --decorate
```

Then synchronize remote knowledge without altering your current branch:

```bash
git fetch --prune origin
```

`fetch` downloads objects and updates remote-tracking references such as `origin/main`. It does not merge those commits into your current branch. This makes it a strong inspection-first default.

Create a task branch from the intended base:

```bash
git switch main
git pull --ff-only
git switch -c feature/order-idempotency
```

`--ff-only` refuses to create a merge commit if local and remote histories diverged. The refusal is information, not a reason to add a force flag.

## The four common path states

```text
untracked --git add--> staged --git commit--> tracked in HEAD
    |                     ^                         |
    |                     |                         |
    +------ ignored       +------ edit tracked -----+
```

`git status --short` uses two status columns: index state, then working-tree state.

```text
?? notes.txt       untracked
A  NewType.java    added to index
 M Service.java    modified only in working tree
M  pom.xml         modified and staged
MM Config.java     staged version plus further unstaged edits
```

The `MM` state is why "I staged the file" does not mean "all current edits are staged."

## Build a focused commit

Suppose `OrderService.java` contains both a bug fix and temporary diagnostics. Stage only the fix:

```bash
git diff -- src/main/java/example/OrderService.java
git add -p src/main/java/example/OrderService.java
git diff --staged --check
git diff --staged
```

Interactive staging presents hunks. Common responses include `y` to stage, `n` to skip, `s` to split, and `e` to edit the proposed patch. Use `?` in the prompt for the local help rather than memorizing every key.

Run the project checks before committing. In a Maven repository:

```bash
./mvnw --batch-mode verify
```

In a Gradle repository:

```bash
./gradlew check
```

Then commit and inspect:

```bash
git commit -m "Prevent duplicate order submission"
git show --stat --oneline HEAD
git status --short --branch
```

## Commit design for reviewers

A focused commit should answer one coherent question. It normally includes the behavior change, tests, and necessary documentation together. It should avoid drive-by formatting and unrelated refactors.

Useful subject lines are imperative, specific, and outcome-oriented:

```text
Reject reused idempotency keys
Preserve request ID in timeout responses
Add migration for order status index
```

Weak subjects hide intent:

```text
fix
changes
WIP final 2
```

The body should explain why when the diff cannot: the incident, invariant, trade-off, compatibility constraint, or rollback plan.

## Amend safely

If the most recent commit is local and unpublished, add the correction and replace the commit:

```bash
git add src/test/java/example/OrderServiceTest.java
git commit --amend --no-edit
```

Amending creates a new commit ID because the commit content or metadata changed. If the old commit was already shared, replacing it requires history rewriting. Prefer a follow-up commit unless your team explicitly allows rewriting that feature branch.

## Remove something from the next commit

To unstage without discarding the working-tree edit:

```bash
git restore --staged path/to/file
```

To discard an unstaged tracked-file edit, first inspect it, then:

```bash
git diff -- path/to/file
git restore path/to/file
```

The second operation overwrites working-tree content. It is intentionally separate from unstaging. Do not run it on work you may need.

## Ignoring is not untracking

A root `.gitignore` for a typical Java repository might begin with:

```gitignore
.idea/
.vscode/
*.iml
target/
build/
.gradle/
*.log
.env
```

If `application-secret.yml` is already tracked, adding it to `.gitignore` does not remove it from the index:

```bash
git rm --cached src/main/resources/application-secret.yml
git commit -m "Stop tracking local secret configuration"
```

This stops future tracking but does not erase prior history or revoke exposed credentials.

Use these diagnostics:

```bash
git check-ignore -v path/to/file
git ls-files path/to/file
```

## Diff and log as query tools

| Question | Command |
|---|---|
| unstaged edits | `git diff` |
| staged edits | `git diff --staged` |
| branch changes since divergence | `git diff origin/main...HEAD` |
| commits unique to current branch | `git log origin/main..HEAD --oneline` |
| history of one path | `git log --follow -- path/to/file` |
| who last changed lines | `git blame -L 20,40 -- path/to/file` |
| find a string added or removed | `git log -S'legacyFlag' -p` |
| find diff lines matching a regex | `git log -G'execute\(' -p` |

The double-dot commit range asks for commits reachable from the right side but not the left. The three-dot diff uses a merge base and shows branch work since divergence. Because log-range and diff syntax answer different graph questions, say the question before selecting the command.

## Stash: a short-lived shelf

Stash can temporarily record tracked changes while you switch context:

```bash
git stash push -u -m "partial idempotency work"
git stash list
git stash show -p stash@{0}
git stash apply stash@{0}
```

`-u` includes untracked files but not ignored files. `apply` keeps the stash entry; `pop` applies and then drops it only when application succeeds. Prefer `apply`, verify, then `drop` when the work is valuable. For work that must be shared, reviewed, or retained, create a branch and commit instead.

## Common mistakes

- Running `git add .` from the wrong directory and staging unrelated files.
- Using `git commit -am` and assuming it includes untracked files. It does not.
- Amending a shared commit without coordinating the rewrite.
- Treating `.gitignore` as secret removal.
- Using stash as unnamed long-term storage.
- Reviewing only the GitHub diff after push instead of the staged diff before commit.

## Interview questions and model answers

**Can a file be staged and modified at the same time?**

Yes. The index can contain one version while the working tree contains later edits. `git diff --staged` shows the staged version relative to `HEAD`; `git diff` shows the later working-tree changes relative to the index.

**What does `git commit -am` miss?**

It stages modifications and deletions of already tracked paths, then commits. It does not add untracked files and can still bundle unrelated tracked changes.

**When is amend appropriate?**

When correcting the latest private commit before others depend on its identity. On shared history, prefer an additive correction or follow the team's explicit rewrite policy.

## Exercises

1. **Foundation - lab:** Create an `MM` file state and explain each column of `git status --short`.
2. **Foundation - practice:** Split two changes in one file into two commits with `git add -p`.
3. **Interview Core - debugging:** A new test did not enter a `git commit -am`. Explain why and correct it.
4. **Interview Core - review:** Rewrite three vague commit messages into outcome-oriented subjects.
5. **SDE-2 Follow-up:** Compare a stash, a temporary commit on a branch, and a worktree for an urgent interruption.

## Chapter summary

The everyday loop is about constructing a deliberate snapshot. Use status and both diffs to distinguish the working tree from the index, keep commits coherent, and rewrite only private history.

## Revision checklist

- [ ] I can interpret two-column short status.
- [ ] I can stage and unstage without discarding work.
- [ ] I can split changes with partial staging.
- [ ] I understand why amend changes a commit ID.
- [ ] I can choose a diff or log query by the question it answers.
