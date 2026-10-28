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
| modular | 1 | 59.7% | 75.9% | 35.4% | 58.3% | 62.5% | 43.3 | n/a | n/a |
| gp | 50 | 50.8 ± 3.4% | 70.9 ± 2.9% | 20.6 ± 5.7% | 50.2 ± 3.9% | 52.0 ± 4.8% | 46.6 | 0.121 ± 0.018 | 1240 ± 346 s (total 17.2 h) |
| score | 50 | 55.0 ± 4.3% | 74.0 ± 5.3% | 26.6 ± 4.7% | 58.1 ± 4.5% | 48.9 ± 7.5% | 42.8 | 0.123 ± 0.024 | 3248 ± 888 s (total 45.1 h) |
| hybrid | 50 | 60.5 ± 1.0% | 80.7 ± 1.3% | 30.3 ± 2.4% | 60.8 ± 1.5% | 60.0 ± 1.6% | 43.9 | 0.110 ± 0.015 | 2028 ± 511 s (total 28.2 h) |

- **SD per run**: standard deviation of a run's win rate across the 90 map × opponent cells, averaged over runs.
- **SD over population**: standard deviation of fitness across the final population (per generation in `gens_runK.csv`).
- **± SD**: standard deviation of the per-run value across runs.

## Per opponent (win rate, mean ± SD across runs)

| Bot | RandomBiasedAI | WorkerRush | LightRush | HeavyRush | RangedRush | EconomyLightRush | Coac* | Mayari* | Rojo* | Droplet* |
|---|---|---|---|---|---|---|---|---|---|---|
| modular | 88.9% | 38.9% | 83.3% | 94.4% | 94.4% | 55.6% | 33.3% | 16.7% | 58.3% | 33.3% |
| gp | 68.9 ± 8.0% | 19.8 ± 9.0% | 87.3 ± 3.7% | 88.8 ± 3.5% | 87.8 ± 2.5% | 72.9 ± 4.4% | 21.1 ± 5.6% | 24.8 ± 3.7% | 22.6 ± 11.7% | 14.1 ± 7.1% |
| score | 92.4 ± 4.6% | 54.9 ± 11.5% | 75.6 ± 8.2% | 82.1 ± 7.8% | 82.6 ± 7.9% | 56.2 ± 8.8% | 24.8 ± 7.2% | 13.3 ± 6.2% | 43.1 ± 8.4% | 25.2 ± 8.3% |
| hybrid | 90.6 ± 5.4% | 44.2 ± 1.1% | 89.8 ± 2.6% | 99.3 ± 1.8% | 99.6 ± 1.5% | 60.6 ± 4.6% | 22.4 ± 1.1% | 18.5 ± 2.5% | 52.2 ± 7.0% | 28.1 ± 7.4% |

## Per map (win rate, mean ± SD across runs)

| Bot | basesWorkersBarracks8x8 | basesWorkers8x8A | basesWorkers12x12 | SixBasesWorkers12x12 | melee16x16Mixed12 | TwoBasesBarracks16x16 | basesWorkers8x8Obstacle* | basesWorkers12x12F* | EightBasesWorkers16x16* |
|---|---|---|---|---|---|---|---|---|---|
| modular | 77.5% | 65.0% | 65.0% | 47.5% | 47.5% | 47.5% | 60.0% | 87.5% | 40.0% |
| gp | 68.9 ± 17.4% | 52.3 ± 4.0% | 67.2 ± 4.6% | 47.0 ± 10.6% | 57.4 ± 8.0% | 8.4 ± 6.4% | 51.6 ± 3.6% | 70.7 ± 4.6% | 33.9 ± 10.1% |
| score | 72.1 ± 6.6% | 56.3 ± 6.3% | 74.6 ± 7.0% | 63.3 ± 7.2% | 58.8 ± 17.1% | 23.5 ± 6.9% | 42.9 ± 8.7% | 67.0 ± 12.0% | 36.9 ± 13.0% |
| hybrid | 70.0 ± 3.6% | 63.1 ± 2.3% | 65.7 ± 3.3% | 49.0 ± 3.8% | 62.5 ± 6.4% | 54.4 ± 3.2% | 61.4 ± 2.0% | 86.7 ± 3.3% | 32.0 ± 3.5% |

## Mann-Whitney U between evolved bots (per-run win rates)

| Comparison | Measure | U | p |
|---|---|---|---|
| gp vs score | overall | 531.0 | 0.000 |
| gp vs score | held-out opps | 529.5 | 0.000 |
| gp vs score | test maps | 1062.5 | 0.196 |
| gp vs hybrid | overall | 51.5 | 0.000 |
| gp vs hybrid | held-out opps | 156.0 | 0.000 |
| gp vs hybrid | test maps | 164.5 | 0.000 |
| score vs hybrid | overall | 290.0 | 0.000 |
| score vs hybrid | held-out opps | 541.0 | 0.000 |
| score vs hybrid | test maps | 113.5 | 0.000 |
