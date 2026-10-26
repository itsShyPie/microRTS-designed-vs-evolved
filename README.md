# Designed vs evolved microRTS bots

A research project by Shahil Narsing.

A hand-designed microRTS bot compared with three bots evolved by genetic programming:
- **GP-direct:** the evolved tree picks each unit's action.
- **GP-score:** the evolved tree scores each legal action.
- **Hybrid:** the designed strategy with evolved combat.

Built on [microRTS](https://github.com/santiontanon/microrts); its original README is in
[README-microRTS.md](README-microRTS.md).

## Layout

- `src/ai/modular/`: the designed bot and the hybrid's combat module
- `src/ai/modular/gp/`: GP-direct and the GP engine
- `src/ai/modular/gp/score/`: GP-score
- `src/ai/modular/exp/`: the experiment (setup, training, evaluation)
- `hpc/`: Linux and cluster scripts ([hpc/README.md](hpc/README.md))
- `run_local.ps1`: runs the whole experiment on a Windows PC
- `runs/`: results

## Experiment

Each bot is evolved 50 times: population 1200, 200 generations, max depth 15, games capped at
2000 cycles. Bots train on 6 maps against 6 scripted bots, and are evaluated on 9 maps against
10 bots. Coac, Mayari, Rojo, Droplet and 3 of the maps are never seen in training.

## Results

- `runs/2026-10_final/`: the final results from the HPC (to be added)
- `runs/2026-10_v2/`: interim local results (trained against EconomyRush; see its `summary.md`)
- `runs/2026-09_v1/`: the September pilot

Each results folder has a `summary.md` with win rates, standard deviations and training times.

## Build and run

Needs Java 11 or newer.

```
bash hpc/build.sh
bash hpc/run_one.sh gp 1
bash hpc/eval.sh
```

`run_one.sh` trains one run of one bot (`gp`, `score` or `hybrid`); `eval.sh` evaluates all finished
runs. On Windows, use `run_local.ps1` (set the JDK path at the top first).

## Licence

GPL-3.0, as microRTS (see `LICENSE`). The bots in `lib/bots/` belong to their authors.
