# Git and GitHub for Java Engineers - Planned Learning Roadmap

> **Publication status:** roadmap edition. This book establishes the learning order and scope now; executable labs, screenshots, failure drills, and interview exercises will be expanded in later revisions.

Git is the local version-control system. GitHub is a collaboration and delivery platform built around Git repositories. An SDE-2 candidate should be able to protect work, inspect history, collaborate through pull requests, and recover from routine mistakes without treating Git as a list of commands to memorize.

## Planned sequence

1. Repositories, working tree, staging area, commits, and object identity.
2. Branches, `HEAD`, merge bases, fast-forward merges, and three-way merges.
3. Remote-tracking branches, fetch, pull, push, and upstream configuration.
4. Rebase, interactive rebase, cherry-pick, revert, reset, restore, and reflog.
5. Conflict resolution with explicit verification before committing.
6. GitHub pull requests, reviews, required checks, CODEOWNERS, and protected branches.
7. Issues, labels, milestones, releases, tags, and contribution workflows.
8. Secrets, signed commits, dependency alerts, and safe repository hygiene.

## Interview and production focus

The completed edition will explain when history rewriting is safe, why `revert` is preferred on shared branches, how a merge base affects diffs, how to recover a lost commit through the reflog, and how branch protection turns review policy into an enforceable control. Labs will use a small Java repository and include both command-line and GitHub workflows.

## Completion gate

A reader is ready to continue when they can create a feature branch, make focused commits, update it safely, resolve a conflict, open a pull request, explain the resulting history, and recover from an incorrect local operation without deleting unrelated work.
