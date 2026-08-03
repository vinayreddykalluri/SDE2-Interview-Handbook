# Complex Production Scenarios and Incident Playbooks

These scenarios combine Git graph reasoning, Java delivery, GitHub policy, and operational risk. For each, begin with evidence and containment. Do not improvise destructive commands under time pressure.

## Scenario 1: secret in three unpublished commits

### Situation

A developer added a real database password in commit A, refactored the file in B, and deleted it in C. The branch has not been pushed.

### Reasoning

The secret remains in A and possibly B. Deletion in C is insufficient. Determine whether local logs, cloud backup, AI tools, or build artifacts could already have exposed it; rotate if uncertain.

### Playbook

1. Revoke or rotate according to credential criticality.
2. Create a local safety branch that is never pushed to an untrusted remote.
3. Remove the secret from every commit using interactive rebase or approved history-filter tooling.
4. Replace it with a configuration reference.
5. Search the full branch range and generated artifacts.
6. Compare rewritten behavior and run tests.
7. Delete unsafe local references only after verification and let retention policy handle object expiry.

Do not publish the safety branch. A backup that contains a secret is sensitive evidence.

## Scenario 2: secret already pushed to public GitHub

### Containment order

1. revoke/rotate immediately;
2. assess use through provider logs;
3. remove the value from the active branch and deployment configuration;
4. contact repository/security owners;
5. coordinate history rewrite if it reduces exposure;
6. inspect forks, PR refs, caches, Actions logs, artifacts, packages, releases, and mirrors;
7. document scope and preventive controls.

Even a perfect history rewrite cannot recall every clone. Credential invalidation is the security boundary.

## Scenario 3: accidental force push to `main`

### Situation

The remote `main` moved from M5 to an older M3, hiding two released commits.

### Playbook

1. Freeze pushes and deployments; communicate the incident.
2. Capture remote state, Actions runs, deployments, releases, and local reflogs from known-good clones.
3. Identify the exact released tip M5 from tag, deployment provenance, PR merge commit, or another clone.
4. Create protected rescue references to M3 and M5.
5. Verify ancestry, diffs, signatures/checks, and production identity.
6. Restore through the repository's audited emergency process, ideally a fast-forward from M3 to M5 if topology permits.
7. Re-run required validation and verify downstream automation.
8. Enable or repair rules blocking force pushes and restricting bypass.

Do not accept the first SHA posted in chat without verification.

## Scenario 4: wrong branch with five local commits

### Situation

Feature commits were created on local `main`; `origin/main` has not changed.

### Playbook

```bash
git status --short
git branch feature/recovered-orders
git fetch origin
git log --oneline --left-right origin/main...main
git switch main
git reset --hard origin/main
git switch feature/recovered-orders
```

The branch command preserves the current tip before reset. The hard reset is justified only after proving the feature branch references all work and `main` should match `origin/main`.

## Scenario 5: force-with-lease rejects a rebased feature

### Situation

The developer rebased, but a teammate pushed another commit to the feature branch. Lease rejection protects that update.

### Playbook

1. Do not retry with `--force`.
2. Fetch and inspect `origin/feature...feature`.
3. Identify ownership of the new remote commit.
4. Preserve both tips with local branches.
5. Integrate the teammate's work through rebase or merge under agreed history policy.
6. Test and only then use force-with-lease if the branch remains rewritable.

## Scenario 6: merge queue fails while both PRs were green

### Situation

PR A renames `payment.timeout`; PR B adds a new consumer of the old property in a different file. Each passes against current main. Their queued combination fails.

### Analysis

The merge group tests combined changes and exposes a semantic integration conflict. This is a successful queue control, not CI instability.

### Resolution

Remove or update the failing queued item according to ownership, fix the consumer, add a configuration contract test, and requeue. Consider central typed configuration or deprecation tests so independent branches cannot silently diverge.

## Scenario 7: revert of a faulty merge does not remerge cleanly

### Situation

A feature merge was reverted. After fixing the feature branch, merging reports little or no change because its original commits remain ancestors of `main`.

### Resolution choices

- revert the revert, then add corrective commits;
- create new commits that reapply the intended feature on top of current main;
- rebuild the feature branch by cherry-picking or reimplementing the necessary changes.

Inspect the graph and combined diff. Do not force unrelated histories or move protected main to manufacture a merge.

## Scenario 8: production hotfix conflicts with unreleased schema work

### Situation

Production 2.8 needs a null-handling fix. Main 2.9 has renamed the column and entity field.

### Playbook

1. Branch from the production tag or 2.8 release line.
2. Implement and test the fix using the 2.8 schema and JDK.
3. Release 2.8.1 with protected review and provenance.
4. Implement the equivalent invariant in main rather than blindly cherry-picking incompatible code.
5. Add a cross-version regression test or migration compatibility check.
6. Link the two fixes in release records.

## Scenario 9: binary artifact added and repository grows dramatically

### Situation

A 400 MB heap dump was committed, pushed, then deleted.

### Playbook

1. Check whether the dump contains secrets or personal data; treat as security/privacy incident if so.
2. Block further distribution and remove exposed release/artifact copies.
3. Add ignore and size controls.
4. Assess forks, clones, open PRs, and release references.
5. Coordinate approved history rewriting if required.
6. Tell contributors how to reclone or repair references after rewrite.
7. Store diagnostics in approved restricted incident storage, not Git.

Deleting the file in the latest tree does not shrink existing history.

## Scenario 10: `pull_request_target` compromise attempt

### Situation

A public fork changes `mvnw` to print environment variables. A privileged `pull_request_target` workflow checks out the fork head and runs the wrapper.

### Containment

1. disable the workflow or remove privileged trigger path;
2. revoke exposed tokens and credentials;
3. audit repository writes, releases, packages, caches, artifacts, and runner hosts;
4. rebuild trusted artifacts from known-good source;
5. separate read-only `pull_request` validation from privileged metadata/deployment;
6. pin actions and minimize token permissions;
7. add workflow ownership and security review.

## Scenario 11: required check is green but tested the wrong revision

### Situation

A workflow checks out `main` explicitly on a pull-request event, so tests ignore PR changes while the required check succeeds.

### Repair

Inspect checkout configuration and workflow event context. For ordinary PR validation, use the event's intended merge commit or explicitly choose the head only when that is the contract. Name the check to reflect what it validates, add a test that prints source SHA and merge context, and prevent privileged arbitrary ref input.

## Scenario 12: flaky test blocks the release branch

### Situation

An integration test fails once in fifty runs. The team repeatedly reruns it until green.

### Playbook

1. preserve failure logs, reports, seed, timing, runner, and dependency versions;
2. classify shared state, clock, network, resource, ordering, or concurrency causes;
3. reproduce with repetition and controlled scheduling;
4. assign an owner and impact severity;
5. quarantine only if policy allows, with expiry and substitute coverage;
6. fix the underlying test or product race;
7. measure recurrence.

Rerun-to-green corrupts the meaning of required checks.

## Scenario 13: submodule pointer references an inaccessible commit

### Situation

The superproject PR updates a submodule to a commit from a contributor's fork. CI with cached objects passes, but clean consumers cannot fetch it.

### Resolution

Verify the submodule URL and commit reachability from the canonical remote, inspect the nested change range, push or merge the commit to the supported source, test a clean recursive clone, and prevent pointer updates whose objects are unavailable.

## Scenario 14: partial staging creates a noncompiling commit

### Situation

An engineer splits a refactor with `git add -p`, but the first commit renames a method without updating callers.

### Resolution

Focused does not mean artificially tiny. Every commit should satisfy the repository's chosen buildability contract unless explicitly marked and never exposed as an integration point. Amend private history so the signature change and necessary caller/test updates are coherent; keep unrelated cleanup separate.

## Scenario 15: migration filenames merge cleanly but collide

### Situation

Two branches each add `V104__...sql`. Git sees different file content at the same path and may conflict, or separate naming schemes can still produce ordering ambiguity.

### Resolution

Rebase/update before merge, allocate or regenerate migration identifiers according to project policy, run a clean migration from supported baseline and latest production snapshot, verify old/new application overlap, and never edit a migration already applied in production merely to resolve Git history.

## Scenario 16: dependency bot PR passes but release fails

### Situation

The bot updates a Maven plugin. Unit CI passes from warm cache; clean release resolution fails because the artifact repository changed or a transitive plugin dependency is unavailable.

### Resolution

Reproduce from a clean environment, inspect effective build and dependency/plugin graphs, verify repositories and checksums, run the release-equivalent lifecycle on dependency PRs, and use lock/reproducibility controls appropriate to the build. Never solve it by adding an unaudited public repository.

## Cross-scenario response framework

```text
1. CONTAIN: stop unsafe pushes, releases, credentials, or writes.
2. PRESERVE: capture references, logs, SHAs, artifacts, and timelines.
3. VERIFY: identify authoritative source and production state.
4. RECOVER: make the smallest auditable correction.
5. VALIDATE: graph, diff, build, tests, security, deployment behavior.
6. PREVENT: rules, tests, ownership, automation, training.
```

## Exercises

1. **Interview Core - tabletop:** Run the accidental force-push scenario with one person as incident lead and one as verifier.
2. **Interview Core - diagnosis:** For each scenario, identify which evidence is local Git, GitHub metadata, CI artifact, or production state.
3. **Interview Core - trade-off:** Compare revert, rollback, forward fix, and history rewrite for three published incidents.
4. **SDE-2 Follow-up:** Extend the merge-queue scenario to three services sharing an event schema.
5. **SDE-2 Follow-up:** Write a post-incident action list that adds controls without creating an impossible contributor workflow.

## Chapter summary

Complex incidents are solved by preserving identity and evidence across layers: commit graph, GitHub policy, CI execution, artifact provenance, schema, and production. Contain first, verify authoritative state, recover with the smallest auditable operation, and repair the control that allowed the failure.

## Revision checklist

- [ ] I do not improvise destructive commands during incidents.
- [ ] I separate credential containment from history cleanup.
- [ ] I can restore references from verified evidence.
- [ ] I reason across source, artifact, schema, and runtime state.
- [ ] I convert incident findings into tested controls.
