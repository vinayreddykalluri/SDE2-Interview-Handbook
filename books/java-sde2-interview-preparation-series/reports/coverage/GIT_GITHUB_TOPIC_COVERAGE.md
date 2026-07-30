# Git and GitHub Topic Coverage

| Topic | Required depth | Previous state | Final state | Chapter | Examples / practice | Validation |
|---|---|---|---|---:|---|---|
| Git versus GitHub | foundation | one sentence | Strong | 1 | platform table, interview answer | source/PDF |
| working tree, index, repository | deep foundation | named only | Strong | 1-2 | state diagrams, `MM` lab | executable Git lab |
| staged versus unstaged diffs | deep foundation | missing | Strong | 1-2 | partial staging, state prediction | executable Git lab |
| commit design and amend | interview core | missing | Strong | 2 | commit review and rewrite exercises | source/PDF |
| ignore and tracked state | interview core | missing | Strong | 2, 8 | ignore diagnostics and policy tasks | source/PDF |
| objects, refs, `HEAD` | interview core | shallow | Strong | 3 | object graph, detached recovery | source/PDF |
| merge base and graph ranges | interview core | named | Strong | 3 | graphs and query examples | source/PDF |
| merge, squash, rebase comparison | interview core | named | Strong | 3, 6 | before/after graphs and decision table | source/PDF |
| remotes and upstreams | foundation/core | named | Strong | 4 | two-clone stale-reference lab | executable Git lab |
| forks and safe force update | interview core | missing | Strong | 4 | lease failure scenario | source/PDF |
| pull-request writing and review | Java SDE-2 | shallow | Strong | 4 | risk matrix and six PR cases | web/PDF review |
| conflict mechanics | interview core | completion goal | Strong | 5 | base/ours/theirs and marker lab | executable Git lab |
| tree and semantic conflicts | Java SDE-2 | missing | Strong | 5 | rename/delete/generated/Java checklist | source/PDF |
| interactive rebase | interview core | command name | Strong | 6 | reorder/split/range-diff exercise | source/PDF |
| cherry-pick and backports | Java SDE-2 | command name | Strong | 6 | `-x` release lab | executable Git lab |
| restore/reset/revert | deep core | command names | Strong | 7 | three-state reset table | source/PDF |
| reflog recovery | deep core | outcome statement | Strong | 7 | reset and rescue branch | executable Git lab |
| merge revert | SDE-2 | missing | Strong | 7 | mainline and remerge scenario | source/PDF |
| `.gitattributes` and line endings | Java core | missing | Strong | 8 | cross-platform policy | source/PDF |
| wrappers/generated code/migrations | Java SDE-2 | missing | Strong | 8 | ownership and migration reviews | source/PDF |
| rulesets and branch protection | SDE-2 | named | Strong | 9 | protected-main design exercise | official docs checked |
| CODEOWNERS and reviews | SDE-2 | named | Strong | 9 | ownership example and bottleneck incident | official docs checked |
| required checks and merge queue | SDE-2 | missing | Strong | 9-10 | merge-group workflow and failure case | official docs checked |
| Maven and Gradle CI | Java core | missing | Strong | 10 | wrapper workflows, JDK matrix | web/PDF + official docs |
| cache/artifact/gate/flakiness | SDE-2 | missing | Strong | 10 | gate YAML and triage ladder | source/PDF |
| Actions least privilege | security core | missing | Strong | 11 | per-job permissions | official docs checked |
| script injection | security core | missing | Strong | 11 | unsafe and corrected YAML | official docs checked |
| fork and `pull_request_target` safety | security core | missing | Strong | 11 | compromise incident and interview round | official docs checked |
| dependency review | Java security | missing | Strong | 11 | dependency-change review | official docs checked |
| secret scanning/push protection | security core | named only | Strong | 11 | local/public incident sequence | official docs checked |
| OIDC/signing/attestations | SDE-2 awareness | missing | Adequate and scoped | 11-12 | trust-boundary questions | official docs checked |
| tags/releases/artifact identity | SDE-2 | named | Strong | 12 | reproducible release pipeline | source/PDF |
| hotfix and forward-port | SDE-2 | named | Strong | 12, 14 | divergent schema incident | source/PDF |
| worktrees | follow-up | missing | Strong | 13 | parallel hotfix lab | executable Git lab |
| bisect | follow-up | missing | Strong | 13 | automated first-bad lab | executable Git lab |
| rerere | follow-up | missing | Adequate and scoped | 13 | repeated conflict exercise | source/PDF |
| monorepo/sparse/submodule | SDE-2 awareness | missing | Strong | 13-14 | CI and unreachable-pointer cases | source/PDF |
| stacked pull requests | SDE-2 awareness | missing | Strong | 13 | ancestry and squash design task | source/PDF |
| production incident method | SDE-2 | missing | Strong | 14 | 16 scenario playbooks | source/PDF |
| realistic interview answers | SDE-2 | missing | Strong | 15 | 14 full dialogue rounds | source/PDF |
| cumulative practice | publication depth | missing | Strong | 16-17 | 146 tasks, five assessments, readiness gate | source/PDF |
| Java companion | executable | missing | Strong | 18 | comparator and module selection | Java 21 lint-as-error |

## Scope boundaries

- Maven and Gradle dependency resolution and lifecycle depth -> JAVA 03.
- Deep Java language, collections, JVM, and concurrency -> JAVA 04-08.
- Cloud-provider-specific deployment identity -> System Design and Backend security material.
- Organization-specific incident response and compliance -> local policy, not universal book prescription.
