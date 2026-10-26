#!/bin/bash
#SBATCH --job-name=microrts-eval
#SBATCH --nodes=1
#SBATCH --ntasks=1
#SBATCH --cpus-per-task=16
#SBATCH --mem=6G
#SBATCH --time=12:00:00
#SBATCH --output=hpc/logs/eval_%j.out
set -euo pipefail

cd "$SLURM_SUBMIT_DIR"
bash hpc/eval.sh "${SLURM_CPUS_PER_TASK:-16}"
