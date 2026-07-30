# Selected Solutions, Command Reference, and Official Sources

The solutions explain reasoning rather than providing command recipes. For the workbook labs, compare your graph and state transitions with these principles.

## Knowledge-check solutions

1. A blob stores file content; a tree maps path components to objects and modes; a commit points to a root tree, parent(s), and metadata; a branch is a movable reference to a commit.
2. The index can retain one staged version while the working tree contains later edits. The two diffs compare different boundaries.
3. No. Ignore rules affect ordinary addition of untracked paths. They neither untrack an indexed path nor erase old commits.
4. It is a local remote-tracking reference recording the last fetched observation of the remote's `main` under the configured refspec.
5. Git downloads missing reachable objects and updates configured remote-tracking references and fetch metadata; it does not integrate the current branch.
6. Pull performs fetch followed by a configured integration such as merge or rebase.
7. `HEAD` identifies a commit directly rather than through a local branch. New commits are valid but no branch automatically retains them.
8. Trees record named entries for files/subtrees; an empty directory has no entry by itself.
9. A log double-dot range selects reachability difference. A diff three-dot form compares one side to a merge base. The syntaxes are related to graph ancestry but invoke command-specific semantics.
10. The current tip must be an ancestor of the incoming tip, allowing the branch reference to move forward without a merge commit.
11. Squash records one aggregate commit without feature-tip parent ancestry; a merge commit has both histories as parents.
12. Parent and often committer/tree/message data change during replay, so content-derived commit identity changes.
13. On an explicitly rewritable owned branch after fetching, inspecting, and coordinating. It should not bypass protected shared-history policy.
14. Git merges text/trees, not Java invariants, configuration contracts, schema compatibility, or concurrency behavior.
15. The selected parent is the baseline relative to which a merge's introduced change is inverted.
16. Reflog records movements in one local repository and expires; it is not automatically shared with the remote or other clones.
17. Cache accelerates reuse and may be evicted; artifact deliberately preserves a workflow output for consumption or evidence.
18. Ambiguous duplicate check names can make rule matching unreliable or confusing. Stable unique names make required evidence explicit.
19. CODEOWNERS routes reviews based on paths. A separate rule must require code-owner approval, and ownership does not replace general review or prove expertise.
20. It tests combined queued changes against a current base, exposing semantic or textual interactions invisible when PRs were independently evaluated.
21. A signature proves supported signer evidence, not code correctness or signer trustworthiness.
22. OIDC removes long-lived static credentials, but cloud trust claims and role permissions can still be too broad.
23. Older commits still reference the blob. The latest tree no longer shows it, but reachable history retains it.
24. The test/build predicate may be flaky or incompatible across toolchain, dependency, schema, fixture, environment, or benchmark changes.

## Predict-the-state solutions

1. The index has the first edited version; working tree has the second; `HEAD` remains unchanged.
2. Tracked modifications are considered, but untracked `NewTest.java` is omitted.
3. The index path returns to `HEAD`; working-tree content remains.
4. Branch moves to parent; former commit change remains staged and in working tree.
5. Branch moves to parent; index matches parent; change remains unstaged in working tree.
6. Branch, index, and tracked working tree match the parent. Former commit and extra tracked edits disappear from visible state; the commit may remain in reflog, while uncommitted overwritten edits may not be recoverable by Git.
7. D becomes temporarily unreachable from named branches after switching, but may be found in reflog until expiration. Create a branch before leaving or recover immediately.
8. Replayed equivalents C-prime and D-prime receive new parents/IDs after F; original C and D remain until references and retention no longer reach them.
9. A new commit C-prime is created with release tip R as parent and analogous patch content, subject to conflict resolution.
10. A new inverse commit is added; P remains in ancestry.
11. The JAR remains tracked. Ignore affects only future untracked discovery; remove it from the index and address history separately.
12. `origin/main` advances locally; local `main` does not move until explicit integration.
13. The remote rejects the conditional update, preserving Y. Fetch and inspect instead of switching to raw force.
14. Required results may never appear for the merge group, stalling the queue. Add `merge_group` and test the rule/check mapping.
15. The value can become shell syntax after expression substitution. Pass it as environment data and quote it, or use a structured action/program.

## Workflow-debugging solution sketches

### Wrong ref

Remove `ref: main` for the normal pull-request checkout contract, then verify the workflow tests the intended event revision. If the organization chooses head-only versus merge-result testing, document and name that contract explicitly.

### Excessive permissions

```yaml
permissions:
  contents: read
```

If the job does not need repository content through the token because checkout is absent, permissions can be even more restrictive. Evaluate each job rather than copying one broad workflow declaration.

### Injection

```yaml
- name: Report branch
  env:
    HEAD_REF: ${{ github.head_ref }}
  run: printf '%s\n' "Reviewing $HEAD_REF"
```

The shell parses fixed source while the attacker-controlled value remains quoted data.

### Privileged fork code

Run code compilation under `pull_request` with read-only contents and no repository secrets. Use a separate default-branch privileged workflow only for metadata or trusted artifacts, never directly execute fork content there. Protect artifact handoff and validate provenance.

### Stuck status

Use an always-triggered stable required gate. It computes whether Java work is affected, requires applicable jobs, and reports success for legitimately unaffected changes. Test every path combination and shared-build fan-out.

## PR review case solutions

### In-memory idempotency

An in-process set does not coordinate multiple service instances, may lose state on restart, grows without retention, and needs thread safety. Define the idempotency contract, persistence/uniqueness boundary, payload mismatch behavior, TTL, transaction interaction, and concurrent tests.

### Comparator

Subtraction can overflow and violate comparator ordering. Use:

```java
requests.sort((left, right) ->
        Integer.compare(left.priority(), right.priority()));
```

Then test extremes and equal priorities; add secondary ordering only if the contract requires it.

### Non-null migration

Use an expand/backfill/validate/contract sequence where necessary: introduce compatible behavior, populate existing rows, validate completeness, enforce the constraint, and later remove compatibility. Assess lock behavior and mixed-version writes.

### Unauthenticated HTTP artifact repository

It weakens integrity and transport security and adds mutable resolution risk. Use an approved authenticated HTTPS repository, verify artifact provenance/checksums, and diagnose the real unavailable dependency rather than widening trust.

### Logging environment

The environment may contain credentials and tokens. Log validated missing property names or safe diagnostics, use redaction, restrict diagnostic artifacts, and test failure output.

### Equality mismatch

Equal objects can produce different hash codes, breaking lookup/deduplication in hash collections. Update both methods from the same immutable equality state and test reflexive/symmetric/transitive behavior, equal hash codes, collection lookup, and mutation boundaries.

## Command decision reference

| Goal | Inspect first | Typical command | Principal caution |
|---|---|---|---|
| see state | - | `git status -sb` | concise output omits content details |
| see unstaged | status | `git diff` | compares working tree to index |
| see next commit | status | `git diff --staged` | verify tests match staged intent |
| stage selected work | diff | `git add -p` | partial commit must remain coherent |
| unstage, keep edit | staged diff | `git restore --staged <path>` | index changes only by default |
| discard tracked edit | diff | `git restore <path>` | overwrites working-tree content |
| update remote knowledge | branch/status | `git fetch --prune` | remote-tracking state is still local |
| integrate only by fast-forward | graph | `git pull --ff-only` | refusal means histories diverged |
| combine histories | graph/base | `git merge <branch>` | validate semantic integration |
| replay private commits | graph/range | `git rebase <base>` | IDs change; coordinate publishing |
| transfer focused change | show/dependencies | `git cherry-pick -x <sha>` | target prerequisites may differ |
| negate public commit | show/history | `git revert <sha>` | later changes can conflict |
| undo private commit | status/reflog | `git reset --soft <target>` or `git reset --mixed <target>` | branch moves |
| overwrite private tracked state | status/backups | `git reset --hard <target>` | can destroy uncommitted work |
| find moved tip | stop mutations | `git reflog` | local and expiring |
| update rewritten owned branch | fetch/graph | `git push --force-with-lease` | not safe for shared/protected branches |
| locate regression | known good/bad | `git bisect` | predicate must be deterministic |
| parallel checkout | branch/worktree list | `git worktree add` | shared refs, separate HEAD/index |

## SDE-2 verbal check

Before running a high-risk command, complete this sentence:

```text
Current reference _____ points to _____; the index contains _____;
the working tree contains _____; the remote last observed at _____;
this command will change _____; recovery evidence will be _____;
verification will include _____.
```

If the blanks cannot be filled, inspect more.

## Pre-command risk matrix

Use this quick classification when a command can move a reference, overwrite files, publish history, or trigger automation:

| Dimension | Lower risk | Higher risk question |
|---|---|---|
| reach | local owned branch | Who else has fetched, based work on, or deployed this reference? |
| data | committed and rescue-referenced | Which uncommitted, ignored, generated, or secret content could disappear or escape? |
| identity | additive new commit | Which commit, tag, review, attestation, or release identity will change? |
| automation | read-only local check | Which workflow, token, cache, package, deployment, or webhook will run? |
| recovery | verified rescue reference | What independent evidence restores the intended source and production state? |

A higher-risk answer does not always forbid the operation. It requires stronger ownership, evidence, communication, and an auditable recovery path.

## Official sources and version-awareness

Git and GitHub features evolve. The mechanics and security claims in this book were checked against official documentation on 2026-07-29. Consult current official pages before implementing organization policy:

- Git command reference: https://git-scm.com/docs/git
- Reset, restore, and revert overview: https://git-scm.com/docs/git
- Rebase: https://git-scm.com/docs/git-rebase
- Reflog: https://git-scm.com/docs/git-reflog
- Merge base: https://git-scm.com/docs/git-merge-base
- Worktree: https://git-scm.com/docs/git-worktree
- Rerere: https://git-scm.com/docs/git-rerere
- Attributes: https://git-scm.com/docs/gitattributes
- GitHub rulesets: https://docs.github.com/en/repositories/configuring-branches-and-merges-in-your-repository/managing-rulesets/about-rulesets
- Protected branches: https://docs.github.com/en/repositories/configuring-branches-and-merges-in-your-repository/managing-protected-branches/about-protected-branches
- CODEOWNERS: https://docs.github.com/en/repositories/managing-your-repositorys-settings-and-features/customizing-your-repository/about-code-owners
- Merge queue: https://docs.github.com/en/repositories/configuring-branches-and-merges-in-your-repository/configuring-pull-request-merges/managing-a-merge-queue
- Java with Maven Actions: https://docs.github.com/en/actions/tutorials/build-and-test-code/java-with-maven
- Java with Gradle Actions: https://docs.github.com/en/actions/tutorials/build-and-test-code/java-with-gradle
- Secure use of Actions: https://docs.github.com/en/actions/reference/security/secure-use
- Script injection: https://docs.github.com/en/actions/concepts/security/script-injections
- OIDC deployment identity: https://docs.github.com/en/actions/how-tos/secure-your-work/security-harden-deployments/oidc-in-cloud-providers
- Dependency review: https://docs.github.com/en/code-security/concepts/supply-chain-security/dependency-review
- Push-protection bypass requests: https://docs.github.com/en/code-security/concepts/secret-security/bypass-requests
- Commit signature verification: https://docs.github.com/en/authentication/managing-commit-signature-verification/about-commit-signature-verification
- Immutable releases: https://docs.github.com/en/code-security/concepts/supply-chain-security/immutable-releases

## Version check before implementation

Official documentation is the starting point, but repository policy still needs local proof. Before enabling a command-dependent workflow or GitHub control, record:

| Check | Evidence to capture |
|---|---|
| client capability | minimum Git version on developer machines, CI images, and release hosts |
| platform availability | repository visibility, organization plan, enterprise policy, and feature status |
| event behavior | exact webhook or Actions event, checked-out revision, token permissions, and fork behavior |
| policy interaction | every matching ruleset/protection rule, bypass actor, merge method, and required check |
| failure behavior | skipped, cancelled, timed-out, stale-approval, queue-failure, and emergency cases |
| audit and recovery | event logs, protected references, artifact identity, owner, and rollback or disable path |

Test controls with representative identities: an external contributor, ordinary member, code owner, automation bot, repository administrator, and emergency responder. A policy that works only for the person who configured it is incomplete.

When documentation and observed behavior differ, do not normalize the difference as folklore. Preserve the repository, plan, version, event payload, workflow revision, and timestamps; reduce the case in a noncritical environment; consult current release notes and support channels; and keep the stricter safe behavior until the discrepancy is resolved.

For interview answers, distinguish a stable principle from a versioned feature. "Do not execute untrusted fork code with privileged credentials" is a stable security principle. The exact settings screen, available rules, event payload, and licensing boundary are versioned implementation details that should be verified.

## Final summary

Professional Git work is controlled state transformation. Professional GitHub work adds independent review, trusted automation, policy, and provenance. The command is the smallest part of the answer: graph, ownership, trust, verification, recovery, and production behavior make it SDE-2 engineering.
