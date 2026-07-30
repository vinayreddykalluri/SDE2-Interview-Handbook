# Practice Workbook: From Foundation to SDE-2

Complete these without looking at the solutions. Use disposable repositories for destructive labs. Before every mutating command, write the expected branch, index, working-tree, and remote state.

## How to use this workbook

Work in four passes. First, answer the knowledge and state-prediction questions aloud. Second, run the command labs in repositories created only for practice. Third, review the Java and workflow cases as if you were the final approver. Fourth, complete the cumulative assessments under a time limit and explain every recovery boundary.

| Practice block | Write down before starting | Evidence of completion |
|---|---|---|
| knowledge check | the exact object, reference, or policy being tested | a two-to-four sentence explanation without command trivia |
| state prediction | branch tip, `HEAD`, index, working tree, and remote observation | predicted and actual state agree |
| command lab | before graph, intended after graph, and abort path | command transcript plus final graph and diff |
| PR review | contract, failure mode, production risk, and required test | actionable review comment and corrected design |
| SDE-2 design | trust boundary, ownership, enforcement, evidence, and exception path | a policy that handles normal, bot, fork, and incident cases |

If a lab result differs from your prediction, stop and explain the difference before continuing. The learning goal is not to finish the command sequence; it is to build a reliable model that makes the next state unsurprising.

## Knowledge checks

1. **Foundation:** What is stored in a blob, tree, commit, and branch reference?
2. **Foundation:** Why can one file be staged and unstaged at the same time?
3. **Foundation:** Does `.gitignore` remove a tracked secret from history?
4. **Foundation:** What does `origin/main` represent?
5. **Foundation:** What changes locally when `git fetch origin` succeeds?
6. **Foundation:** How is `pull` related to fetch?
7. **Foundation:** What does detached `HEAD` mean?
8. **Foundation:** Why does an empty directory not appear in a commit?
9. **Interview Core:** Why do `git diff main...feature` and `git log main..feature` use dots differently?
10. **Interview Core:** What makes a fast-forward possible?
11. **Interview Core:** How does squash integration differ from a merge commit?
12. **Interview Core:** Why does rebase create new commit IDs?
13. **Interview Core:** When is force-with-lease appropriate?
14. **Interview Core:** Why can a conflict-free merge break Java behavior?
15. **Interview Core:** What does mainline mean when reverting a merge?
16. **Interview Core:** Why is reflog local rather than remote backup?
17. **Interview Core:** What is the practical difference between cache and artifact in Actions?
18. **Interview Core:** Why must required job names be unique?
19. **Interview Core:** What does CODEOWNERS do, and what does it not do?
20. **SDE-2 Follow-up:** How can a merge queue reveal a failure no individual PR exposed?
21. **SDE-2 Follow-up:** Why can a signed commit still be unsafe?
22. **SDE-2 Follow-up:** Why is OIDC not automatically least privilege?
23. **SDE-2 Follow-up:** Why does deleting a large file not shrink history?
24. **SDE-2 Follow-up:** What makes a deterministic bisect difficult across years of Java history?

## Predict the state

For each, state branch tip, index, working tree, and whether data can be recovered through ordinary Git references.

1. Edit `A.java`, run `git add A.java`, edit it again.
2. Run `git commit -am "Fix"` while `NewTest.java` is untracked.
3. Run `git restore --staged A.java` when A is staged.
4. Run `git reset --soft HEAD^` after a private commit.
5. Run `git reset HEAD^` after a private commit.
6. Run `git reset --hard HEAD^` with extra unstaged tracked edits.
7. Create commit D in detached `HEAD`, then switch to main.
8. Rebase C-D from B onto F.
9. Cherry-pick C onto release branch R.
10. Revert published commit P.
11. Add `target/` to `.gitignore` after `target/app.jar` is tracked.
12. Fetch while local `main` is three commits behind remote.
13. A force-with-lease push expects remote X, but remote points to Y.
14. A required workflow has only a `pull_request` trigger while a merge queue creates a merge group.
15. A workflow interpolates a malicious branch name into `run`.

## Debug the workflow

### Exercise A: wrong ref

```yaml
on: pull_request
jobs:
  test:
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v6
        with:
          ref: main
      - run: ./mvnw test
```

Explain why it can report green for broken PR code and correct the checkout contract.

### Exercise B: excessive permissions

```yaml
permissions: write-all
```

The workflow only compiles Java and uploads no result. Write the minimal repository permission declaration.

### Exercise C: injection

```yaml
- run: echo "Reviewing ${{ github.head_ref }}"
```

Explain the data-to-code path and repair it.

### Exercise D: privileged fork code

```yaml
on: pull_request_target
steps:
  - uses: actions/checkout@v6
    with:
      ref: ${{ github.event.pull_request.head.sha }}
  - run: ./gradlew check
```

Design a safe two-workflow boundary.

### Exercise E: stuck status

A workflow is required on `main` but has `paths: ["src/**"]`. Documentation-only PRs wait forever. Design a stable gate.

## Command labs

1. **Foundation:** Initialize a repository, create two focused Java commits, and explain every `status --short` transition.
2. **Foundation:** Stage only one of two changes in the same Java method.
3. **Foundation:** Create a remote with `git init --bare`, clone it twice, and demonstrate stale remote-tracking state.
4. **Interview Core:** Create diverged branches and perform both a merge and a rebase on copied branches; compare graphs.
5. **Interview Core:** Resolve a content conflict by combining intent rather than selecting a side.
6. **Interview Core:** Create a modify/delete conflict and document the evidence for the final choice.
7. **Interview Core:** Lose a committed branch tip through reset and recover it with reflog.
8. **Interview Core:** Use interactive rebase to split a mixed commit into behavior and formatting commits.
9. **Interview Core:** Backport one fix to a release branch with cherry-pick `-x`.
10. **Interview Core:** Use bisect to find a Java test regression.
11. **SDE-2 Follow-up:** Use two worktrees to maintain a feature and hotfix concurrently.
12. **SDE-2 Follow-up:** Enable rerere, resolve a conflict, recreate it, and verify the proposed reuse.

## Pull-request review cases

### Case 1: idempotency

A PR adds an in-memory `HashSet` of request IDs to a horizontally scaled order service. Review correctness, concurrency, persistence, memory, deploy behavior, and tests.

### Case 2: comparator

```java
requests.sort((left, right) -> left.priority() - right.priority());
```

Review overflow and ordering contract; propose a correction.

### Case 3: migration

A PR makes `customer_id` non-null and deploys application validation in the same release. Review existing rows, mixed-version deployment, locks, rollback, and observability.

### Case 4: build repository

A dependency fix adds an unauthenticated artifact repository over HTTP. Review supply-chain and reproducibility impact.

### Case 5: logging

A debug statement logs the complete Spring `Environment` when startup fails. Review secret exposure and operational alternatives.

### Case 6: equality

One commit changes `equals` to include `tenantId`; `hashCode` remains unchanged. Explain collection behavior and required test cases.

## SDE-2 design tasks

1. Design protected-branch and ruleset controls for a public Java library with external contributors.
2. Design GitHub Actions for Java 17 and 21, Maven wrapper, unit/integration tests, dependency review, and merge queue.
3. Design a release flow connecting signed/annotated tag, source SHA, JAR digest, SBOM, attestation, registry, and production deployment.
4. Design a secret-leak incident plan across Git history, Actions, packages, forks, caches, and cloud logs.
5. Design monorepo affected-module CI where build-parent changes fan out but leaf documentation does not.
6. Design repository ownership for application, security, database, workflows, build tooling, and CODEOWNERS itself.
7. Design a hotfix process for two supported release lines without dropping the fix from main.
8. Design a stacked-PR workflow compatible with squash merge and required reviews.

## Cumulative assessments

### Assessment 1: local mastery

Without notes, create a branch, make two focused commits from mixed edits, amend the private second commit, compare it to main, and recover the original pre-amend commit through reflog.

### Assessment 2: collaboration

Using a bare remote and two clones, create concurrent changes, demonstrate a rejected push, fetch and inspect divergence, resolve through the chosen team policy, and explain why force was not the default.

### Assessment 3: protected delivery

Review a Java CI workflow for revision correctness, wrappers, matrix, permissions, action identity, merge queue, cache, and artifacts. Produce a branch policy that makes its stable gate required.

### Assessment 4: incident

Tabletop a public secret leak followed by a malicious package publication. Separate credential containment, Git history, workflow compromise, artifact revocation, consumer notification, and prevention.

### Assessment 5: release

Starting from production tag 2.8.0 and divergent main, design and draw the complete path for hotfix 2.8.1, artifact verification, deployment, rollback condition, and forward-port.

## Final readiness assessment

You are ready when you can complete all of the following without unsafe guesses:

- explain working tree, index, object graph, references, and remotes;
- draw merge, rebase, squash, cherry-pick, revert, and reset outcomes;
- resolve text and semantic conflicts with Java verification;
- recover committed work through reflog and state the recovery limits;
- write and review a production-quality pull request;
- design protected branches, CODEOWNERS, required checks, and merge queue;
- secure untrusted Actions workflows and respond to a secret leak;
- connect source, tag, artifact, attestation, and deployment;
- lead one complex repository incident using containment, evidence, recovery, validation, and prevention.
