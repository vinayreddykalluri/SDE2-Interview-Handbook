#!/usr/bin/env bash
set -euo pipefail

LAB_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
VOLUME_DIR="$(cd "${LAB_DIR}/.." && pwd)"
CLASS_DIR="$(mktemp -d)"
trap 'rm -rf "${CLASS_DIR}"' EXIT

javac --release 21 -Xlint:all -Werror \
    -d "${CLASS_DIR}" \
    "${VOLUME_DIR}/code/SpringDataInterviewCompanion.java"
java -ea -cp "${CLASS_DIR}" SpringDataInterviewCompanion

mvn -q -f "${LAB_DIR}/maven-demo/pom.xml" test

echo "Spring Data executable labs passed"
