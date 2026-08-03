#!/usr/bin/env bash
set -euo pipefail

script_dir=$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)
volume_dir=$(cd "$script_dir/.." && pwd)
temp_dir=$(mktemp -d "${TMPDIR:-/tmp}/spring-framework-book-labs.XXXXXX")

cleanup() {
  rm -rf "$temp_dir"
}
trap cleanup EXIT

mkdir -p "$temp_dir/classes"
javac --release 21 -Xlint:all -Werror \
  -d "$temp_dir/classes" \
  "$volume_dir/code/SpringFrameworkInterviewCompanion.java"
java -ea -cp "$temp_dir/classes" SpringFrameworkInterviewCompanion \
  >"$temp_dir/companion-output.txt"
grep -q "SpringFrameworkInterviewCompanion checks passed" \
  "$temp_dir/companion-output.txt"

if ! command -v mvn >/dev/null 2>&1; then
  printf 'Spring Framework labs: Java companion passed; Maven fixture skipped (mvn unavailable)\n'
  exit 0
fi

mvn -q -f "$script_dir/maven-demo/pom.xml" clean test

printf 'Spring Framework labs: Java companion and Spring integration fixture passed\n'
