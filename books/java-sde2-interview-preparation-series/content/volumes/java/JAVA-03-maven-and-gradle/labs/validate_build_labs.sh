#!/usr/bin/env bash
set -euo pipefail

script_dir=$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)
volume_dir=$(cd "$script_dir/.." && pwd)
temp_dir=$(mktemp -d "${TMPDIR:-/tmp}/maven-gradle-book-labs.XXXXXX")
trap 'rm -rf "$temp_dir"' EXIT

mkdir -p "$temp_dir/companion"
javac --release 21 -Xlint:all -Werror \
  -d "$temp_dir/companion" \
  "$volume_dir/code/BuildToolModel.java"
java -ea -cp "$temp_dir/companion" BuildToolModel \
  >"$temp_dir/companion-output.txt"
grep -q "BuildToolModel checks passed" "$temp_dir/companion-output.txt"

cp -R "$script_dir/maven-demo" "$temp_dir/maven-demo"
cp -R "$script_dir/gradle-demo" "$temp_dir/gradle-demo"

maven_status="skipped (mvn unavailable)"
if command -v mvn >/dev/null 2>&1; then
  mvn -q -B -ntp -f "$temp_dir/maven-demo/pom.xml" verify
  maven_output=$(java \
    -cp "$temp_dir/maven-demo/app/target/app-1.0.0-SNAPSHOT.jar:$temp_dir/maven-demo/core/target/core-1.0.0-SNAPSHOT.jar" \
    com.example.buildlab.Main)
  test "$maven_output" = "total=42"
  jar tf "$temp_dir/maven-demo/app/target/app-1.0.0-SNAPSHOT.jar" \
    | grep -q "com/example/buildlab/Main.class"
  maven_status="passed"
fi

gradle_status="skipped (gradle unavailable)"
if command -v gradle >/dev/null 2>&1; then
  gradle --no-daemon --console=plain -q \
    -p "$temp_dir/gradle-demo" build
  gradle_output=$(gradle --no-daemon --console=plain -q \
    -p "$temp_dir/gradle-demo" :app:run)
  test "$gradle_output" = "total=42"
  jar tf "$temp_dir/gradle-demo/app/build/libs/app-1.0.0-SNAPSHOT.jar" \
    | grep -q "com/example/buildlab/Main.class"
  gradle_status="passed"
fi

printf 'Build book labs: companion passed; Maven %s; Gradle %s\n' \
  "$maven_status" "$gradle_status"
