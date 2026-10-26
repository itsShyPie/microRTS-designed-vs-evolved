#!/usr/bin/env bash
set -euo pipefail
cd "$(dirname "$0")/.."
source hpc/common.sh
command -v javac >/dev/null || { echo "javac not found: load a JDK module (not just a JRE)" >&2; exit 1; }

find src -name '*.java' > hpc/sources.list
mkdir -p bin
javac --release 11 -encoding UTF-8 -nowarn -cp "lib/*${SEP}lib/bots/*" -d bin @hpc/sources.list
echo "Built $(find bin -name '*.class' | wc -l) classes into bin/ ($(javac -version 2>&1))"
