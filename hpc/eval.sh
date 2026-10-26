#!/usr/bin/env bash
set -euo pipefail
cd "$(dirname "$0")/.."
source hpc/common.sh

OUTDIR="${OUT:-runs/2026-10_final}"
java -Xmx"${HEAP:-4g}" "${JAVA_OPTS[@]}" -cp "$CP" ai.modular.exp.EvalExperiment \
    "$OUTDIR" "${2:-2}" "${1:-$(nproc)}"
echo "Summary: $OUTDIR/summary.md"
