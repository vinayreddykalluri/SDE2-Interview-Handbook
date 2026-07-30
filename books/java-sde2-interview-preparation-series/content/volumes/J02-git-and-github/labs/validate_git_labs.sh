#!/usr/bin/env bash
set -eu

lab_root="$(mktemp -d "${TMPDIR:-/tmp}/git-github-book-labs.XXXXXX")"
trap 'rm -rf "$lab_root"' EXIT INT TERM

configure_repo() {
    git config user.name "Git Book Validator"
    git config user.email "git-book-validator@example.invalid"
    git config commit.gpgsign false
}

assert_equal() {
    expected="$1"
    actual="$2"
    message="$3"
    if [ "$expected" != "$actual" ]; then
        echo "FAILED: $message; expected=$expected actual=$actual" >&2
        exit 1
    fi
}

scenario_count=0

repo="$lab_root/local"
git init -q -b main "$repo"
cd "$repo"
configure_repo
printf '%s\n' 'class PriceCalculator {}' > PriceCalculator.java
git add PriceCalculator.java
git commit -qm "Add calculator"
printf '%s\n' 'final class PriceCalculator {}' > PriceCalculator.java
git add PriceCalculator.java
printf '%s\n' 'final class PriceCalculator { /* local */ }' > PriceCalculator.java
git diff --cached --quiet && exit 1
git diff --quiet && exit 1
assert_equal "MM PriceCalculator.java" "$(git status --short PriceCalculator.java)" \
    "one path must retain staged and unstaged versions"
scenario_count=$((scenario_count + 1))

git restore PriceCalculator.java
git commit -qm "Make calculator final"
git switch -qc feature
printf '%s\n' 'final class PriceCalculator { int timeout = 10; }' > PriceCalculator.java
git commit -qam "Configure feature timeout"
git switch -q main
printf '%s\n' 'final class PriceCalculator { int timeout = 20; }' > PriceCalculator.java
git commit -qam "Change main timeout"
if git merge feature >/dev/null 2>&1; then
    echo "FAILED: expected content conflict" >&2
    exit 1
fi
git status --porcelain | grep -q '^UU PriceCalculator.java$'
printf '%s\n' 'final class PriceCalculator { int timeout = 15; }' > PriceCalculator.java
git add PriceCalculator.java
git commit -qm "Resolve timeout intent"
git show HEAD:PriceCalculator.java | grep -q 'timeout = 15'
scenario_count=$((scenario_count + 1))

before_reset="$(git rev-parse HEAD)"
git reset --hard -q HEAD^
git reflog --format='%H' | grep -q "^$before_reset$"
git branch rescue/reflog "$before_reset"
assert_equal "$before_reset" "$(git rev-parse rescue/reflog)" \
    "reflog candidate must be attached to rescue branch"
scenario_count=$((scenario_count + 1))

git switch -qc release main
printf '%s\n' 'release-fix' > fix.txt
git add fix.txt
git commit -qm "Add release fix"
fix_commit="$(git rev-parse HEAD)"
git switch -q main
git switch -qc supported main
git cherry-pick -x "$fix_commit" >/dev/null
git log -1 --format='%B' | grep -q 'cherry picked from commit'
test -f fix.txt
scenario_count=$((scenario_count + 1))

bisect_repo="$lab_root/bisect"
git init -q -b main "$bisect_repo"
cd "$bisect_repo"
configure_repo
for value in 1 2 3 4 5 6 7 8; do
    printf '%s\n' "$value" > value.txt
    git add value.txt
    git commit -qm "Set value $value"
    if [ "$value" -eq 3 ]; then
        good_commit="$(git rev-parse HEAD)"
    fi
done
bad_commit="$(git rev-parse HEAD)"
git bisect start "$bad_commit" "$good_commit" >/dev/null
git bisect run sh -c 'test "$(cat value.txt)" -lt 4' >/dev/null
first_bad="$(git rev-parse refs/bisect/bad)"
assert_equal "Set value 4" "$(git show -s --format='%s' "$first_bad")" \
    "bisect must identify the first failing value"
git bisect reset >/dev/null
scenario_count=$((scenario_count + 1))

worktree_path="$lab_root/hotfix-worktree"
git worktree add -q -b hotfix/test "$worktree_path" main
assert_equal "hotfix/test" "$(git -C "$worktree_path" branch --show-current)" \
    "linked worktree must use isolated branch HEAD"
git worktree remove "$worktree_path"
scenario_count=$((scenario_count + 1))

remote="$lab_root/remote.git"
git init -q --bare "$remote"
first_clone="$lab_root/first"
second_clone="$lab_root/second"
git clone -q "$remote" "$first_clone"
cd "$first_clone"
configure_repo
git switch -qc main
printf '%s\n' 'one' > shared.txt
git add shared.txt
git commit -qm "Initial shared state"
git push -qu origin main
git -C "$remote" symbolic-ref HEAD refs/heads/main
git clone -q "$remote" "$second_clone"
cd "$first_clone"
printf '%s\n' 'two' >> shared.txt
git commit -qam "Advance remote"
git push -q origin main
stale="$(git -C "$second_clone" rev-parse origin/main)"
fresh="$(git -C "$first_clone" rev-parse origin/main)"
test "$stale" != "$fresh"
git -C "$second_clone" fetch -q origin
assert_equal "$fresh" "$(git -C "$second_clone" rev-parse origin/main)" \
    "fetch must update remote-tracking state"
scenario_count=$((scenario_count + 1))

echo "Git/GitHub book labs: $scenario_count scenarios passed"
