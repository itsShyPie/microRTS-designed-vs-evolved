# Running on an HPC cluster

150 training jobs (3 bots × 50 runs), then one evaluation job. Run every command from the
repo folder. Finished runs are skipped, so failed or timed-out jobs can simply be resubmitted.

## 1. Build

Needs Java 11 or newer (a JDK).

```
module load <java module>
bash hpc/build.sh
```

## 2. Smoke test (about a minute)

```
OUT=runs/smoke GENS=1 POP=20 bash hpc/run_one.sh gp 1 4
rm -r runs/smoke
```

It should end with `run 1 done in ...`.

## 3. Train

PBS:

```
qsub -v BOT=gp hpc/pbs_train.pbs
qsub -v BOT=score hpc/pbs_train.pbs
qsub -v BOT=hybrid hpc/pbs_train.pbs
```

Slurm:

```
sbatch --export=ALL,BOT=gp hpc/slurm_train.sbatch
sbatch --export=ALL,BOT=score hpc/slurm_train.sbatch
sbatch --export=ALL,BOT=hybrid hpc/slurm_train.sbatch
```

Check progress with `bash hpc/progress.sh`. Logs are in `hpc/logs/`.

## 4. Evaluate (once all 150 runs are done)

```
qsub hpc/pbs_eval.pbs
sbatch hpc/slurm_eval.sbatch
```

Use whichever matches your scheduler. Results go to `runs/2026-10_final/`, with the overview in
`summary.md`.

## Notes

- Load Java before submitting; the jobs inherit it.
- If your cluster needs a project or queue, add `#PBS -P` / `#PBS -q` (or `#SBATCH --account` / `--partition`) to the job scripts.
- A run takes 16–70 minutes on 16 threads; the job scripts allow 4–6 hours.
