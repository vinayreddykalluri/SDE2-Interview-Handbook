#!/usr/bin/env bash
set -euo pipefail

LAB_DIR="$(cd "$(dirname "$0")" && pwd)"
BOOK_DIR="$(cd "$LAB_DIR/.." && pwd)"
BUILD_DIR="$(mktemp -d "${TMPDIR:-/tmp}/spring-boot-book.XXXXXX")"
trap 'rm -rf "$BUILD_DIR"' EXIT

javac --release 21 -Xlint:all -Werror \
  -d "$BUILD_DIR" \
  "$BOOK_DIR/code/SpringBootInterviewCompanion.java"

java -ea -cp "$BUILD_DIR" SpringBootInterviewCompanion
mvn -q -f "$LAB_DIR/maven-demo/pom.xml" test

echo "Spring Boot labs: Java companion and Boot integration fixture passed"
