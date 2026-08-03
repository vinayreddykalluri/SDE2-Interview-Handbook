#!/usr/bin/env bash
set -euo pipefail

SCRIPT_DIR="$(CDPATH= cd -- "$(dirname -- "$0")" && pwd)"
VOLUME_DIR="$(CDPATH= cd -- "$SCRIPT_DIR/.." && pwd)"
BUILD_DIR="$(mktemp -d "${TMPDIR:-/tmp}/persistence-capstone.XXXXXX")"
trap 'rm -rf "$BUILD_DIR"' EXIT

javac -Xlint:all -Werror --release 21 -d "$BUILD_DIR" \
  "$VOLUME_DIR/code/PersistencePatterns.java"

OUTPUT="$(java -ea -cp "$BUILD_DIR" PersistencePatterns)"
EXPECTED="PersistencePatterns assertions passed"

if [[ "$OUTPUT" != "$EXPECTED" ]]; then
  printf 'Expected: %s\nActual:   %s\n' "$EXPECTED" "$OUTPUT" >&2
  exit 1
fi

printf '%s\n' "$OUTPUT"
