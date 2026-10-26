#!/bin/bash
#SBATCH --job-name=microrts-train
#SBATCH --array=1-50
#SBATCH --nodes=1
#SBATCH --ntasks=1
#SBATCH --cpus-per-task=16
#SBATCH --mem=3G
#SBATCH --time=04:00:00
#SBATCH --output=hpc/logs/train_%A_%a.out
set -euo pipefail
: "${BOT:?Submit with --export=ALL,BOT=gp|score|hybrid}"

cd "$SLURM_SUBMIT_DIR"
bash hpc/run_one.sh "$BOT" "$SLURM_ARRAY_TASK_ID" "${SLURM_CPUS_PER_TASK:-16}"
