# Remotes, Forks, Pull Requests, and Review Engineering

Team Git begins when local references and remote-tracking references are kept distinct. GitHub pull requests then add a review and automation conversation around a proposed branch comparison.

## Learning objectives

- explain `origin`, upstream configuration, and remote-tracking branches;
- distinguish fetch, pull, push, and clone;
- collaborate through a shared repository or fork;
- build a pull request that is reviewable and operationally safe;
- review Java changes beyond style;
- handle stale branches and force updates without overwriting teammates.

## Remote names are configuration, not magic

After cloning:

```bash
git clone https://github.com/example/payments-service.git
cd payments-service
git remote -v
git branch -vv
```

`origin` is a conventional remote name. It is not necessarily authoritative and does not mean "original branch." A remote-tracking reference such as `origin/main` is local knowledge of a remote reference from the last successful fetch.

```text
remote repository             your local repository
refs/heads/main   --fetch-->  refs/remotes/origin/main
                                      |
refs/heads/main   <--push---  refs/heads/main (local)
```

Your local `main` and `origin/main` can point to different commits. The remote may also have advanced since your last fetch.

## Fetch, pull, and push

```bash
git fetch --prune origin
git log --oneline --left-right --graph main...origin/main
```

- `fetch` downloads reachable objects and updates configured remote-tracking references.
- `pull` runs fetch and then integrates into the current branch according to configuration or command options.
- `push` requests updates to references in another repository and sends missing objects.

Because `pull` combines two phases, make the integration policy explicit:

```bash
git pull --ff-only
git pull --rebase
```

The first refuses divergence. The second rebases local commits after fetching; it rewrites those local commit IDs. Choose according to branch ownership and team policy.

## Upstream branch configuration

The first push can create a remote branch and configure tracking:

```bash
git push -u origin feature/order-idempotency
```

After that, `git status -sb`, `git pull`, and `git push` can use the configured upstream. Inspect it with:

```bash
git rev-parse --abbrev-ref --symbolic-full-name '@{upstream}'
```

An upstream is a configuration relationship, not proof that histories are synchronized.

## Shared-repository and fork workflows

In a shared repository, contributors push feature branches to the same remote. In a fork workflow, a contributor has a fork and usually configures the source repository separately:

```bash
git remote rename origin fork
git remote add upstream https://github.com/example/payments-service.git
git fetch --prune upstream
```

Names are team conventions. The key is knowing which remote you can push to and which base branch the pull request targets.

Update a fork branch safely:

```bash
git switch feature/order-idempotency
git fetch upstream
git rebase upstream/main
git push --force-with-lease fork feature/order-idempotency
```

The force update is appropriate only if this is a rewritable branch you own and repository policy permits it.

## Why `--force-with-lease` is safer, not safe by itself

A normal push refuses non-fast-forward updates. `--force` disables that ancestry guard. `--force-with-lease` adds an expectation: update the remote reference only if it still has the value your local repository expects.

```bash
git fetch origin
git push --force-with-lease origin feature/order-idempotency
```

It reduces the chance of overwriting an unseen teammate update. It does not make rewriting a shared branch acceptable, and background fetches can update local remote-tracking knowledge in ways that weaken a casual lease assumption. Protect important branches server-side and coordinate ownership.

## A pull request is an engineering argument

A strong pull request answers:

1. What problem or user impact exists?
2. What behavior changes?
3. Why was this design chosen?
4. How was it tested?
5. What are the compatibility, migration, security, and performance effects?
6. How can it be observed and rolled back?

Example structure:

```markdown
## Problem
Duplicate retries can create two orders when the gateway times out.

## Change
- persist a unique idempotency key before charging
- return the original order for a repeated key

## Verification
- unit tests for same and different payloads
- concurrent integration test with 20 identical requests
- migration verified against a production-size snapshot

## Risk and rollout
- unique-index creation may lock the table; deploy migration first
- metric: order_idempotency_conflict_total
- rollback application first, then remove index in a later migration
```

Keep the diff small enough to review. Separate generated changes, mechanical renames, and behavior changes when possible. Link the issue, but make the PR understandable without chasing a private conversation.

## Review Java behavior, not only formatting

Use a risk-based checklist:

| Area | Reviewer questions |
|---|---|
| contract | nullability, validation, compatibility, status codes, schemas |
| correctness | overflow, equality/hash contract, time zones, concurrency, transaction boundaries |
| collections | order assumptions, mutable keys, comparator safety, null behavior |
| persistence | migration safety, index use, N+1 queries, locking, rollback |
| reliability | timeout, retry, idempotency, partial failure, resource closure |
| security | authorization, secret exposure, injection, dependency change, logging |
| tests | behavior and failure boundaries, not implementation-only assertions |
| operations | metrics, logs, alerts, deployment order, feature flag, rollback |

Review the commit or PR diff, but also inspect surrounding code and call sites. A five-line signature change can affect a hundred callers.

## Review states and conversation quality

GitHub reviews can comment, approve, or request changes. A review approval is evidence from one revision, not a permanent property of a branch. New commits may dismiss stale approvals if rules require it.

Label feedback by intent:

```text
blocker: correctness, security, data loss, broken contract
suggestion: meaningful improvement that is not required for merge
question: context or reasoning needed
nit: optional polish
```

State the reason and an actionable direction. "This is bad" is not review engineering. "This subtraction comparator can overflow; use `Integer.compare(left.score(), right.score())`" is.

## Update a pull request branch

Merge-based update:

```bash
git fetch origin
git switch feature/order-idempotency
git merge origin/main
git push origin feature/order-idempotency
```

Rebase-based update for an owned branch:

```bash
git fetch origin
git switch feature/order-idempotency
git rebase origin/main
git push --force-with-lease origin feature/order-idempotency
```

The first preserves existing IDs and adds a merge boundary. The second produces a linear rewritten branch. Use the repository's policy; do not repeatedly alternate strategies.

## Common mistakes

- Assuming `origin/main` updates itself without fetch.
- Pulling on the wrong current branch.
- Pushing to the wrong remote in a fork workflow.
- Rewriting a branch another engineer has based work on.
- Treating approval as a substitute for passing current checks.
- Mixing a dependency upgrade, schema migration, refactor, and feature into one opaque PR.
- Reviewing only changed lines when the risk lies in unchanged callers or configuration.

## Interview questions and model answers

**What exactly does fetch change?**

It downloads objects and updates configured remote-tracking references and fetch metadata. It does not merge or rebase those commits into the current local branch.

**Why can push be rejected as non-fast-forward?**

The proposed remote tip would not retain the current remote tip as an ancestor, so the update could discard visible history. Fetch, inspect divergence, and choose an intentional integration or coordinated rewrite.

**How do you make a large PR reviewable?**

Reduce scope; separate mechanical and behavioral changes; document contract, risks, and tests; use focused commits; provide a review order; and split dependent work into stacked PRs only when the team can manage their base relationships.

## Exercises

1. **Foundation - lab:** Create a bare repository as `origin`, clone it twice, and observe how one clone's `origin/main` remains stale until fetch.
2. **Foundation - prediction:** Explain what `git push -u` changes locally and remotely.
3. **Interview Core - PR writing:** Draft a PR description for a Java endpoint timeout fix, including verification and rollback.
4. **Interview Core - review:** Find five non-style risks in a change that adds a mutable object as a `HashMap` key.
5. **SDE-2 Follow-up:** Explain a case where `--force-with-lease` should still be prohibited by policy.

## Chapter summary

Remote-tracking references are local observations, not live remote pointers. Pull requests add an engineering review process around branch comparisons. Safe collaboration requires explicit remotes, explicit integration policy, reviewable scope, current verification, and server-side controls for important branches.

## Revision checklist

- [ ] I can distinguish local, remote-tracking, and remote references.
- [ ] I can explain fetch, pull, and push separately.
- [ ] I can configure and inspect an upstream.
- [ ] I can write and review a risk-aware Java pull request.
- [ ] I use force-with-lease only within an explicit rewrite policy.
