# Git and GitHub Scenario Validation

## Validation result

Status: **PASS**

Environment:

```text
Git 2.48.1
OpenJDK 24 runtime compiling with --release 21
Python 3.12.6
```

## Executable Git scenarios

Command:

```bash
bash content/volumes/java/JAVA-02-git-and-github/labs/validate_git_labs.sh
```

| Scenario | Assertion | Result |
|---|---|---|
| staged plus unstaged path | short status reports `MM` and both diffs exist | Pass |
| content conflict | unresolved `UU` state appears and combined intent is committed | Pass |
| reflog recovery | pre-reset commit is found and attached to rescue branch | Pass |
| cherry-pick provenance | `-x` records source identity and target content exists | Pass |
| automated bisect | first bad commit is exactly `Set value 4` | Pass |
| linked worktree | independent branch `HEAD` is checked out and cleanly removed | Pass |
| remote-tracking staleness | second clone remains stale until explicit fetch | Pass |

All repositories are created under `mktemp -d` and removed by a trap. The validator does not alter user repositories, call GitHub, or require network access.

## Java companion

The series validator discovered, compiled, and executed `GitBookJavaFixture` with:

```bash
javac --release 21 -Xlint:all -Werror
java -ea GitBookJavaFixture
```

New companion result: **1 compiled, 1 executed, 0 failures**.

Full focused-series result: **41 series-native classes compiled and executed, 0 failures**.

The companion asserts overflow-safe pull-request ordering at integer extremes and dependency-aware module selection for leaf and root build changes.

## Source and example inventory

| Metric | Count |
|---|---:|
| canonical chapter sources | 17 |
| source words | 23,848 |
| fenced examples/diagrams | 145 |
| Bash blocks | 82 |
| YAML blocks | 21 |
| text diagrams/outputs | 33 |
| Java blocks in chapters | 5 |
| indexed Java examples including companion | 6 |
| complete Java companion classes | 1 |
| intentionally invalid Java classes submitted to compiler | 0 |

Java and shell fragments are pedagogical command fragments unless explicitly identified as complete programs. The standalone companion is the normal compilation boundary.

## Validation boundaries

Skipped from local execution:

- GitHub rulesets, protected-branch, CODEOWNERS, merge-queue, secret-scanning, and release UI configuration, because they require repository administration and plan-dependent external state.
- Cloud OIDC exchange, because a provider trust policy and credentials are intentionally outside this repository.
- Malicious `pull_request_target` examples, because executing the unsafe pattern would violate the validation trust boundary.

These behaviors were checked against official Git or GitHub documentation dated 2026-07-29 and are labeled as version-sensitive in the book.

## Full command

```bash
python3 scripts/validate_series.py --source-only
```

Result: sources mapped, Number Systems validation passed, seven Git scenarios passed, 41 focused Java classes compiled and ran, and focused-series validation passed.
