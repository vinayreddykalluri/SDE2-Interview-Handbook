# Conflict Resolution: Text, Trees, and Java Semantics

A conflict is Git refusing to choose a result automatically. It is not a tool failure. The correct resolution is the version that preserves the intended combined behavior, which may be neither side exactly.

## Learning objectives

- explain the base, ours, and theirs inputs to a three-way merge;
- resolve content, add/add, modify/delete, rename, and binary conflicts;
- distinguish merge, rebase, cherry-pick, and revert conflict context;
- validate Java semantics after resolving text;
- abort safely when the operation or intended result is unclear.

## Before resolving: identify the operation

Start with:

```bash
git status
git diff --name-only --diff-filter=U
```

`git status` tells you whether Git is merging, rebasing, cherry-picking, or reverting and which continuation or abort command applies. Do not blindly run `git merge --continue` during a rebase.

| Operation | Continue after staging | Abort |
|---|---|---|
| merge | `git merge --continue` | `git merge --abort` |
| rebase | `git rebase --continue` | `git rebase --abort` |
| cherry-pick | `git cherry-pick --continue` | `git cherry-pick --abort` |
| revert | `git revert --continue` | `git revert --abort` |

Abort returns toward the pre-operation state, subject to documented command behavior and any pre-existing changes. The safest prerequisite is a clean working tree or a deliberately recorded state.

## The three-way model

```text
                    base
                   /    \
              ours        theirs
                   \    /
                resolved result
```

- **base**: selected common ancestor content;
- **ours**: content from the currently checked-out side in a normal merge;
- **theirs**: content being merged.

During rebase, the intuitive labels can surprise: commits are replayed onto the upstream, and low-level ours/theirs terminology reflects that operation. Prefer reasoning from `git status`, commit IDs, and explicit stage entries rather than memorized UI colors.

Inspect all three index stages for one path:

```bash
git ls-files -u -- src/main/java/example/RetryPolicy.java
git show :1:src/main/java/example/RetryPolicy.java
git show :2:src/main/java/example/RetryPolicy.java
git show :3:src/main/java/example/RetryPolicy.java
```

Stage 1 is base, stage 2 is ours, and stage 3 is theirs for an unresolved normal merge entry.

## Content conflict walkthrough

An unresolved conflicted file is intentionally not compilable. Git's markers may look like the labeled form below:

```java
// [current branch]
Duration timeout = Duration.ofMillis(800);
// [separator]
Duration timeout = configuration.paymentTimeout();
// [incoming branch: feature/configurable-timeout]
```

Git writes each bracketed label above as a marker made from seven repeated angle-bracket or equals characters. The descriptive labels keep this published example readable without leaving marker-shaped text in the repository.

A weak resolution deletes markers and chooses one line. A correct resolution asks why both branches changed it. Perhaps the feature makes timeout configurable while `main` lowered the default. The combined solution may be:

```java
Duration timeout = configuration.paymentTimeoutOrDefault(Duration.ofMillis(800));
```

Then verify no markers remain, stage, test, and continue:

```bash
git diff --check
git add src/main/java/example/RetryPolicy.java
./mvnw --batch-mode verify
git diff --staged
git merge --continue
```

For rebase, the test can be run at every stopped commit or at least at behavioral boundaries; then use `git rebase --continue`.

## Conflict categories

### Add/add

Both sides add the same path with different content. Decide whether one replaces the other, the files should be combined, or one should be renamed. This is common when two engineers independently add `OrderMapper.java`.

### Modify/delete

One side edits a path while the other deletes it. Determine whether the feature was moved, intentionally retired, or accidentally deleted. Search for replacements and callers before choosing:

```bash
git log --all --name-status -- path/to/LegacyClient.java
git grep 'LegacyClient'
```

If deletion is correct:

```bash
git rm path/to/LegacyClient.java
```

If the modified file should remain, edit and `git add` it.

### Rename-related conflicts

Git detects renames by similarity during comparison; a rename is not a permanent object type. A rename/rename conflict can assign two new names to one base path. A rename/delete conflict asks whether the renamed concept still belongs. Confirm package declarations, imports, Spring component scanning, reflection-based names, configuration, and tests after the path decision.

### Directory/file conflict

One side adds a file where the other side creates a directory at the same path. Choose a new structure and update all build and runtime references.

### Binary conflict

Git cannot merge arbitrary binary content semantically. Choose a version or regenerate the asset from an authoritative editable source. Do not casually mark database files, JARs, generated PDFs, or lock artifacts as mergeable text.

### File-mode and line-ending noise

An executable-bit change or CRLF/LF churn can obscure the real edit. Use `.gitattributes` and repository conventions before the conflict occurs. Do not solve line-ending policy through repeated mass rewrites inside feature PRs.

## Conflict tools without surrendering judgment

```bash
git mergetool
git diff --cc
git checkout --conflict=diff3 -- path/to/file
```

`diff3`-style markers include the base, which can reveal what each side intended to change. Modern Git also supports conflict styles that show more context. Tool labels still require operation-aware interpretation.

## Java-specific semantic resolution checklist

After the markers are gone, check:

1. **Compilation:** package names, imports, overload resolution, generated types.
2. **Contracts:** nullability, exceptions, return values, HTTP/schema compatibility.
3. **Equality:** `equals` and `hashCode` use compatible state.
4. **Concurrency:** lock order, atomicity, publication, idempotency.
5. **Persistence:** migration order, entity mapping, indexes, transaction boundary.
6. **Configuration:** property names, defaults, profiles, environment variables.
7. **Dependency graph:** both versions and exclusions were not combined blindly.
8. **Tests:** run affected tests and the complete required build.

## Generated-file conflicts

Prefer resolving the editable sources and regenerating with the repository-owned toolchain:

```text
OpenAPI source -> generator version in build -> generated Java
schema migration -> code generation -> checked output if policy requires it
```

If generated files are tracked, review the regenerated diff for unexpected output. If they are not meant to be tracked, fix the repository policy instead of resolving the same noise repeatedly.

## Repeated conflicts and rerere

`rerere` means reuse recorded resolution. When enabled, Git can remember a conflict shape and the resolution you staged, then propose that resolution when the same conflict recurs:

```bash
git config rerere.enabled true
git rerere status
git rerere diff
```

This is useful during a long rebase or repeated integration testing. It is not proof that the old semantic choice is still correct. Inspect and test the reused result.

## Common mistakes

- Deleting markers without understanding both branch intents.
- Choosing "accept ours" for every file to make the operation finish.
- Confusing rebase-side labels with normal-merge intuition.
- Staging every conflict before reviewing the combined diff.
- Resolving generated output rather than its source.
- Assuming a successful compile proves runtime or data compatibility.
- forgetting to test after `rerere` reuses a prior resolution.

## Interview questions and model answers

**How do you resolve a merge conflict?**

Identify the operation and conflicted paths; inspect the base and both sides; understand intended behavior; edit a combined resolution; ensure markers and whitespace errors are gone; stage only resolved paths; run focused and required tests; review the staged result; continue or abort if the intent remains unclear.

**What are ours and theirs during rebase?**

The labels follow the internal operation, not always a developer's branch intuition. During replay, the checked-out result is based on the upstream and each original commit is applied in turn. I rely on status, commit IDs, base stages, and the shown patch rather than choosing by label alone.

**Why might Git not report a Java semantic conflict?**

The conflicting decisions may touch different lines or files. Git can merge text while application contracts, schemas, locking, or configuration become incompatible.

## Exercises

1. **Foundation - lab:** Create a same-line conflict in two branches, inspect stages 1/2/3, and write a combined result.
2. **Interview Core - debugging:** One branch deletes an adapter while another fixes it. List the evidence needed before choosing delete or retain.
3. **Interview Core - semantic conflict:** Design two cleanly merging changes that break `equals`/`hashCode` behavior; add a failing test.
4. **Interview Core - operation awareness:** Compare the correct continue and abort commands for merge, rebase, cherry-pick, and revert.
5. **SDE-2 Follow-up:** Define a policy for generated Java, OpenAPI specifications, and binary diagrams that minimizes conflicts while preserving reproducibility.

## Chapter summary

Conflict resolution is intent reconstruction. The base and two sides provide evidence; status identifies the operation; the resolved Java behavior must be compiled, tested, and reviewed. Finishing the Git operation is only one part of correctness.

## Revision checklist

- [ ] I identify the active operation before resolving.
- [ ] I can inspect base, ours, and theirs.
- [ ] I can handle tree conflicts, not only marker conflicts.
- [ ] I validate Java semantics after textual resolution.
- [ ] I know when to abort rather than guess.
