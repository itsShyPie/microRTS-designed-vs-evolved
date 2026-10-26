# Shared experiment results

```
train maps        : maps/8x8/basesWorkersBarracks8x8.xml, maps/8x8/basesWorkers8x8A.xml, maps/12x12/basesWorkers12x12.xml, maps/12x12/SixBasesWorkers12x12.xml, maps/16x16/melee16x16Mixed12.xml, maps/16x16/TwoBasesBarracks16x16.xml
test maps         : maps/8x8/basesWorkers8x8Obstacle.xml, maps/12x12/basesWorkers12x12F.xml, maps/16x16/EightBasesWorkers16x16.xml
train opponents   : RandomBiasedAI, WorkerRush, LightRush, HeavyRush, RangedRush, EconomyLightRush
held-out opponents: Coac, Mayari, Rojo, Droplet
population        : 1200
generations       : 200
max depth         : 15 (offspring cap; initial depth 5)
tournament / elite: 3 / 0.1
crossover / mut.  : 0.85 / 0.25
fitness games     : 2 per training opponent (one per side), map rotated per generation
max game cycles   : 2000
unit type table   : default (new UnitTypeTable())
```

Win rate = games won / games played (draws, including the 2000-cycle cap, count as not won). Opponents marked * are held out of training; maps marked * are held out of training.

## Overall (mean ± SD across runs)

| Bot | Runs | Win rate | Train opps | Held-out opps* | Train maps | Test maps* | SD per run | SD over population | Training time |
|---|---|---|---|---|---|---|---|---|---|
| modular | 1 | 60.0% | 77.3% | 34.0% | 60.0% | 60.0% | 42.9 | n/a | n/a |
| gp | 50 | 51.6 ± 4.5% | 71.7 ± 4.1% | 21.6 ± 6.3% | 51.4 ± 4.9% | 52.1 ± 5.9% | 46.7 | 0.124 ± 0.019 | 1004 ± 115 s (total 13.9 h) |
| score | 50 | 51.1 ± 5.1% | 69.5 ± 5.8% | 23.6 ± 5.7% | 54.0 ± 4.6% | 45.5 ± 8.9% | 42.3 | 0.121 ± 0.024 | 4186 ± 581 s (total 58.1 h) |
| hybrid | 50 | 57.9 ± 2.1% | 77.9 ± 2.8% | 27.8 ± 2.6% | 57.8 ± 2.5% | 58.1 ± 2.3% | 44.3 | 0.099 ± 0.017 | 1329 ± 119 s (total 18.5 h) |

- **SD per run**: standard deviation of a run's win rate across the 90 map × opponent cells, averaged over runs.
- **SD over population**: standard deviation of fitness across the final population (per generation in `gens_runK.csv`).
- **± SD**: standard deviation of the per-run value across runs.

## Per opponent (win rate, mean ± SD across runs)

| Bot | RandomBiasedAI | WorkerRush | LightRush | HeavyRush | RangedRush | EconomyLightRush | Coac* | Mayari* | Rojo* | Droplet* |
|---|---|---|---|---|---|---|---|---|---|---|
| modular | 91.7% | 38.9% | 83.3% | 94.4% | 94.4% | 61.1% | 36.1% | 16.7% | 55.6% | 27.8% |
| gp | 71.6 ± 11.5% | 23.6 ± 12.6% | 87.0 ± 3.8% | 89.6 ± 3.8% | 87.8 ± 2.7% | 70.6 ± 5.4% | 24.1 ± 6.5% | 25.2 ± 5.4% | 24.3 ± 9.9% | 12.7 ± 10.1% |
| score | 89.8 ± 5.0% | 54.9 ± 11.8% | 68.2 ± 9.3% | 78.2 ± 6.7% | 78.3 ± 9.3% | 47.4 ± 8.4% | 19.2 ± 9.2% | 12.6 ± 7.4% | 41.9 ± 8.8% | 20.8 ± 7.1% |
| hybrid | 87.9 ± 4.9% | 42.8 ± 3.2% | 88.4 ± 3.9% | 96.7 ± 4.5% | 96.7 ± 4.0% | 55.1 ± 4.7% | 22.8 ± 1.8% | 18.4 ± 3.4% | 50.9 ± 7.8% | 19.2 ± 7.0% |

## Per map (win rate, mean ± SD across runs)

| Bot | basesWorkersBarracks8x8 | basesWorkers8x8A | basesWorkers12x12 | SixBasesWorkers12x12 | melee16x16Mixed12 | TwoBasesBarracks16x16 | basesWorkers8x8Obstacle* | basesWorkers12x12F* | EightBasesWorkers16x16* |
|---|---|---|---|---|---|---|---|---|---|
| modular | 77.5% | 62.5% | 72.5% | 52.5% | 47.5% | 47.5% | 62.5% | 85.0% | 32.5% |
| gp | 73.6 ± 20.0% | 55.1 ± 5.2% | 67.2 ± 4.7% | 49.0 ± 11.1% | 54.7 ± 9.8% | 8.8 ± 7.1% | 51.1 ± 3.7% | 70.9 ± 5.6% | 34.5 ± 13.2% |
| score | 69.4 ± 6.6% | 56.3 ± 7.6% | 66.9 ± 11.5% | 58.9 ± 6.8% | 50.1 ± 19.0% | 22.2 ± 7.2% | 44.5 ± 8.7% | 62.1 ± 15.4% | 30.0 ± 13.5% |
| hybrid | 68.4 ± 4.5% | 61.4 ± 1.8% | 63.1 ± 4.7% | 48.4 ± 4.1% | 55.7 ± 10.6% | 49.9 ± 4.9% | 60.6 ± 1.3% | 85.8 ± 3.2% | 28.0 ± 5.8% |

## Mann-Whitney U between evolved bots (per-run win rates)

| Comparison | Measure | U | p |
|---|---|---|---|
| gp vs score | overall | 1224.5 | 0.860 |
| gp vs score | held-out opps | 946.5 | 0.036 |
| gp vs score | test maps | 700.0 | 0.000 |
| gp vs hybrid | overall | 277.5 | 0.000 |
| gp vs hybrid | held-out opps | 400.0 | 0.000 |
| gp vs hybrid | test maps | 359.0 | 0.000 |
| score vs hybrid | overall | 232.5 | 0.000 |
| score vs hybrid | held-out opps | 702.0 | 0.000 |
| score vs hybrid | test maps | 164.5 | 0.000 |

> **Note:** interim local batch. These bots were trained against EconomyRush, not EconomyLightRush (the setup block at the top shows the final configuration). Their EconomyLightRush games were played after training, so EconomyLightRush was not seen in training even though it is counted under "Train opps". The final results come from the HPC run in `runs/2026-10_final`.
