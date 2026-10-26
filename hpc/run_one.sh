#!/usr/bin/env bash
set -euo pipefail
cd "$(dirname "$0")/.."
source hpc/common.sh

BOT="$1"
RUN="$2"
THREADS="${3:-$(nproc)}"
EXTRA=()
if [ -n "${GENS:-}" ]; then EXTRA=("$GENS" "${POP:-1200}"); fi

java -Xmx"${HEAP:-2g}" "${JAVA_OPTS[@]}" -cp "$CP" ai.modular.exp.RunExperiment \
    "$BOT" "$RUN" "$RUN" "${OUT:-runs/2026-10_final}" "$THREADS" ${EXTRA[@]+"${EXTRA[@]}"}
