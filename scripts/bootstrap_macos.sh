#!/usr/bin/env bash
set -euo pipefail

ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
cd "$ROOT"

if [[ "$(uname -s)" != "Darwin" ]]; then
  echo "This bootstrap script supports macOS. See LOCAL_DEVELOPMENT.md for manual setup." >&2
  exit 1
fi

if ! command -v brew >/dev/null 2>&1; then
  echo "Homebrew is required. Install it from https://brew.sh and rerun make bootstrap." >&2
  exit 1
fi

for command_name in git make; do
  if ! command -v "$command_name" >/dev/null 2>&1; then
    echo "$command_name is missing. Run 'xcode-select --install', then rerun make bootstrap." >&2
    exit 1
  fi
done

formulae=()

add_formula() {
  local formula="$1"
  local existing
  for existing in "${formulae[@]:-}"; do
    [[ "$existing" == "$formula" ]] && return
  done
  formulae+=("$formula")
}

command -v python3 >/dev/null 2>&1 || add_formula python
if ! command -v java >/dev/null 2>&1 || ! command -v javac >/dev/null 2>&1; then
  add_formula openjdk@21
fi
if ! command -v node >/dev/null 2>&1 || ! command -v npm >/dev/null 2>&1; then
  add_formula node
fi
command -v gh >/dev/null 2>&1 || add_formula gh
command -v pandoc >/dev/null 2>&1 || add_formula pandoc
command -v tectonic >/dev/null 2>&1 || add_formula tectonic

if (( ${#formulae[@]} > 0 )); then
  echo "Installing missing Homebrew packages: ${formulae[*]}"
  brew install "${formulae[@]}"
fi

if brew --prefix openjdk@21 >/dev/null 2>&1; then
  export PATH="$(brew --prefix openjdk@21)/bin:$PATH"
fi
hash -r

for command_name in python3 java javac node npm pandoc tectonic gh; do
  if ! command -v "$command_name" >/dev/null 2>&1; then
    echo "Required command is still unavailable after installation: $command_name" >&2
    exit 1
  fi
done

if [[ ! -x .venv/bin/python ]]; then
  python3 -m venv .venv
fi

.venv/bin/python -m pip install --upgrade pip
.venv/bin/python -m pip install -r requirements.txt
python3 scripts/check_local_environment.py

echo
echo "Local environment is ready. Run 'make build-all' followed by 'make serve-web'."
