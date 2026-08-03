#!/usr/bin/env bash
set -euo pipefail

volume_dir="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
classes_dir="$(mktemp -d)"
trap 'rm -rf "${classes_dir}"' EXIT

javac --release 21 -Xlint:all -Werror -d "${classes_dir}" \
  "${volume_dir}/code/MongoDbInterviewCompanion.java"
java -ea -cp "${classes_dir}" MongoDbInterviewCompanion
mvn -q -f "${volume_dir}/labs/maven-demo/pom.xml" test

echo "MongoDB volume validation passed"
