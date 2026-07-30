# Git and GitHub Foundations for Java Engineers

Git is a local version-control system. GitHub is a collaboration and delivery platform that stores Git repositories and adds pull requests, reviews, automation, security controls, releases, and project coordination. They solve related problems, but they are not the same product.

This book begins with the files on your laptop and ends with the decisions an SDE-2 engineer makes during a risky release or repository incident. Do not jump directly to rebase, rulesets, or GitHub Actions. Those tools become predictable only after the working tree, index, commits, references, and commit graph are clear.

## Learning objectives

By the end of this chapter, you should be able to:

- explain what Git records and what it does not record;
- distinguish Git from GitHub;
- install and verify the tools without copying unsafe global settings;
- describe the working tree, index, repository, commit, branch, and `HEAD`;
- create a local repository and make a first focused commit;
- choose the correct next chapter from the learning route.

## Why this matters at SDE-2

An SDE-2 engineer is expected to deliver changes through an existing team process. The job is not merely to type `git add .` and `git push`. You should know what is staged, what will enter history, which branch will move, what a pull request compares, what automation will run, and how to recover if an operation was wrong.

The strongest signal is controlled reasoning:

1. inspect state;
2. predict the operation;
3. make the smallest safe change;
4. inspect the result;
5. publish only after local verification.

## The study route

```text
FOUNDATION
files -> working tree -> index -> commit -> branch -> HEAD
   |
   v
COLLABORATION
remote -> fetch -> push -> pull request -> review -> merge
   |
   v
CONTROL
conflicts -> rebase -> revert -> reflog -> protected branches
   |
   v
DELIVERY
Java CI -> security -> tags -> releases -> hotfixes
   |
   v
SDE-2 OPERATIONS
incident playbooks -> scalable workflows -> interview simulations
```

Use the route in order on a first read. On revision, begin at the first command whose effect you cannot predict before running it.

## Git, GitHub, and your Java project

| Layer | Responsibility | Java example |
|---|---|---|
| File system | editable files and directories | `src/main/java`, `pom.xml`, `build.gradle.kts` |
| Git | snapshots, history, branches, local recovery | commit an API change and its tests |
| Remote server | shared Git objects and references | a GitHub repository |
| GitHub collaboration | pull requests, reviews, issues, rules | require two reviews for `main` |
| GitHub automation | CI, security checks, release jobs | compile on Java 17 and 21 |

Git can work without GitHub. GitHub can host a Git repository, but most Git operations still happen locally. A pull request is not a Git object. An issue, review approval, branch rule, or workflow run is also GitHub metadata rather than part of the commit graph.

## First-principles model

For now, use this three-area model:

```text
Working tree                 Index                    Repository
files you can edit      snapshot being prepared      committed snapshots

OrderService.java  --add-->  staged version  --commit-->  commit 91ab...
      ^                         |
      +------ restore ----------+
```

- The **working tree** is the checked-out file content you edit.
- The **index**, also called the staging area, is the proposed content for the next commit.
- The **repository** stores Git objects and references, usually under `.git`.

The index is not simply a list of filenames. It stores a staged version of each tracked path. That is why one file can contain both staged and unstaged changes.

## Core terminology

| Term | Precise beginner meaning |
|---|---|
| repository | Git database plus references and configuration |
| tracked file | a path Git already knows through the index or a commit |
| untracked file | a working-tree path not yet known to Git |
| commit | immutable snapshot metadata pointing to a tree and parent commit(s) |
| branch | movable name that normally points to a commit |
| `HEAD` | reference identifying the current checkout, usually the current branch |
| hash / object ID | content-derived identifier for a Git object |
| remote | a named configuration for another repository, such as `origin` |
| clone | local repository initialized from another repository |

## Setup without accidental repository changes

Verify the installed tools:

```bash
git --version
java --version
```

Configure the identity written into new commits:

```bash
git config --global user.name "Your Name"
git config --global user.email "you@example.com"
git config --global init.defaultBranch main
```

Inspect configuration and its source before changing anything else:

```bash
git config --list --show-origin
```

Global configuration affects all repositories for the current user. Repository-local configuration, written with `--local` or with no scope while inside a repository, can override it. Never paste a large configuration block that you do not understand.

## First repository: one complete cycle

Create a disposable learning directory, then initialize it:

```bash
mkdir pricing-service-lab
cd pricing-service-lab
git init
git status
```

Create this file as `src/main/java/example/PriceCalculator.java`:

```java
package example;

final class PriceCalculator {
    long totalInCents(long unitPriceInCents, int quantity) {
        if (unitPriceInCents < 0 || quantity < 0) {
            throw new IllegalArgumentException("values must be nonnegative");
        }
        return Math.multiplyExact(unitPriceInCents, quantity);
    }
}
```

Inspect, stage, inspect again, and commit:

```bash
git status --short
git add src/main/java/example/PriceCalculator.java
git diff --staged
git commit -m "Add overflow-safe price calculation"
git log --oneline --decorate --graph --all
```

The commit records the staged file content, author and committer metadata, message, parent relationship, and tree reference. It does not record an empty directory, a running JVM process, an IDE window, or unstaged edits.

## The inspection-before-mutation habit

Use these commands constantly:

```bash
git status --short --branch
git diff
git diff --staged
git log --oneline --decorate --graph --all
git show --stat --oneline HEAD
```

Their questions differ:

- `git status` asks, "What is the relationship among `HEAD`, index, and working tree?"
- `git diff` asks, "What is changed but not staged?"
- `git diff --staged` asks, "What would the next commit contain?"
- `git log` asks, "How are commits and references connected?"
- `git show HEAD` asks, "What did this commit record?"

## Common beginner mistakes

### Treating GitHub as the source of all local truth

Your local repository can have unpushed commits, branches, and reflog entries GitHub has never seen. `git status` and `git log --all` inspect local state; the GitHub page reflects only pushed references and platform metadata.

### Staging everything without review

`git add .` is convenient, but it may stage logs, IDE files, credentials, generated artifacts, or unrelated edits. Prefer explicit paths or `git add -p`, then inspect `git diff --staged`.

### Using a commit as a backup for secrets

A later deletion does not remove a secret from earlier commits. Do not commit credentials. If a real secret enters history, rotate or revoke it first, then follow the incident process in the security chapter.

### Describing a commit as a diff

A commit represents a snapshot plus metadata and parents. Git can compute a diff between snapshots, but the commit is not merely the patch text.

## Interview questions and model answers

**What is the difference between Git and GitHub?**

Git is the version-control system that stores content-addressed objects and references, usually locally. GitHub hosts Git repositories and adds collaboration, automation, policy, and security features. A commit and branch exist in Git; a pull request and required review exist in GitHub.

**Why does Git have a staging area?**

It lets a developer assemble and review the exact next snapshot independently of other working-tree edits. This enables focused commits and partial staging.

**Does Git track folders?**

Git tracks file paths and file content through trees. An empty directory has no tracked file entry, so it is not represented by itself.

**Is a branch a copy of every file?**

No. A normal branch is a movable reference to a commit. The commit reaches a tree snapshot and its history through parent links.

## Exercises

1. **Foundation - knowledge:** Explain the working tree, index, and repository without using the word "save."
2. **Foundation - command prediction:** Edit one tracked file, stage it, edit it again, and predict both `git diff` outputs before running them.
3. **Foundation - debugging:** A commit contains a debug log but the working tree does not. Which inspection command proves the debug log is staged?
4. **Interview Core - modeling:** Draw two commits and a branch name. Which part moves after the next commit?
5. **SDE-2 Follow-up:** Why should a reviewer care that a change is split into focused commits even if the pull request will be squash-merged?

## Chapter summary

Git records staged snapshots as commits and moves references through a commit graph. GitHub adds collaboration and delivery controls around that graph. The safest working style is inspect, predict, mutate, inspect, and only then publish.

## Revision checklist

- [ ] I can distinguish Git objects from GitHub metadata.
- [ ] I can explain working tree, index, repository, branch, and `HEAD`.
- [ ] I can create and inspect a first commit.
- [ ] I do not stage or discard changes without inspecting them.
