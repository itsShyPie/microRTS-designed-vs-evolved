#!/usr/bin/env bash
cd "$(dirname "$0")/.."
OUTDIR="${OUT:-runs/2026-10_final}"
for b in gp score hybrid; do
    n=$(ls "$OUTDIR/$b"/run*.stats.csv 2>/dev/null | wc -l)
    echo "$b: $n / 50 runs finished"
done
