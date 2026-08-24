#!/usr/bin/env bash
set -euo pipefail

VERSION="${1:-1.0.0}"

if ! command -v mvn >/dev/null 2>&1; then
  echo "Maven is required but was not found in PATH." >&2
  exit 1
fi

mvn -B -ntp -Drevision="$VERSION" clean package

echo
echo "Built: target/MarsRankup-${VERSION}.jar"
