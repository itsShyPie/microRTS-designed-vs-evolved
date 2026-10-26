package ai.modular.exp;

import ai.modular.gp.EvoParams;
import ai.modular.gp.GPConfig;
import ai.modular.gp.score.ScoreConfig;

import java.io.OutputStream;
import java.io.PrintStream;
import java.util.Arrays;

public final class Experiment {
    private Experiment() {}

    public static final String[] TRAIN_MAPS = {
        "maps/8x8/basesWorkersBarracks8x8.xml",
        "maps/8x8/basesWorkers8x8A.xml",
        "maps/12x12/basesWorkers12x12.xml",
        "maps/12x12/SixBasesWorkers12x12.xml",
        "maps/16x16/melee16x16Mixed12.xml",
        "maps/16x16/TwoBasesBarracks16x16.xml",
    };
    public static final String[] TEST_MAPS = {
        "maps/8x8/basesWorkers8x8Obstacle.xml",
        "maps/12x12/basesWorkers12x12F.xml",
        "maps/16x16/EightBasesWorkers16x16.xml",
    };
    public static final String[] ALL_MAPS = concat(TRAIN_MAPS, TEST_MAPS);

    public static final String[] TRAIN_OPPONENTS = {
        "RandomBiasedAI", "WorkerRush", "LightRush", "HeavyRush", "RangedRush", "EconomyLightRush",
    };
    public static final String[] HELD_OUT_OPPONENTS = { "Coac", "Mayari", "Rojo", "Droplet" };
    public static final String[] ALL_OPPONENTS = concat(TRAIN_OPPONENTS, HELD_OUT_OPPONENTS);

    public static final String[] BOTS = { "gp", "score", "hybrid" };

    public static final int POP         = 1200;
    public static final int GENERATIONS = 200;
    public static final int MAX_DEPTH   = 15;
    public static final int MAX_CYCLES  = 2000;
    public static final int RUNS        = 50;
    public static final int FITNESS_GAMES_PER_OPPONENT = 2;

    public static final String DEFAULT_OUT = "runs/2026-10_final";

    public static final String STATS_HEADER =
        "run,seed,train_seconds,generations,pop_size,threads,host,final_validation,final_pop_mean,final_pop_sd,best_size,best_depth";

    public interface Body {
        void run() throws Exception;
    }

    public static void runQuietly(Body body) {
        PrintStream realErr = System.err;
        if (!Boolean.getBoolean("exp.engineLog")) System.setErr(new PrintStream(OutputStream.nullOutputStream()));
        try {
            body.run();
        } catch (Throwable e) {
            System.setErr(realErr);
            e.printStackTrace(realErr);
            System.exit(1);
        }
        System.setErr(realErr);
    }

    public static EvoParams params(String bot) {
        EvoParams p = new EvoParams();
        p.popSize = POP;
        p.generations = GENERATIONS;
        p.initDepth = bot.equals("score") ? ScoreConfig.MAX_DEPTH : GPConfig.MAX_DEPTH;
        p.maxDepth = MAX_DEPTH;
        p.maxNodes = Integer.MAX_VALUE;
        p.rejectToParent = true;
        p.maxCycles = MAX_CYCLES;
        p.trainOpponents = TRAIN_OPPONENTS;
        p.fitnessGames = new int[TRAIN_OPPONENTS.length];
        Arrays.fill(p.fitnessGames, FITNESS_GAMES_PER_OPPONENT);
        p.finalCandidates = 6;
        p.finalValidationGames = 2;
        return p;
    }

    public static String trainMapsCsv() {
        return String.join(",", TRAIN_MAPS);
    }

    public static boolean isHeldOutOpponent(String name) {
        return Arrays.asList(HELD_OUT_OPPONENTS).contains(name);
    }

    public static boolean isTestMap(String map) {
        return Arrays.asList(TEST_MAPS).contains(map);
    }

    public static String shortMap(String m) {
        int i = m.lastIndexOf('/');
        return (i >= 0 ? m.substring(i + 1) : m).replace(".xml", "");
    }

    public static String describe(EvoParams p) {
        return "train maps        : " + String.join(", ", TRAIN_MAPS) + "\n"
             + "test maps         : " + String.join(", ", TEST_MAPS) + "\n"
             + "train opponents   : " + String.join(", ", TRAIN_OPPONENTS) + "\n"
             + "held-out opponents: " + String.join(", ", HELD_OUT_OPPONENTS) + "\n"
             + "population        : " + p.popSize + "\n"
             + "generations       : " + p.generations + "\n"
             + "max depth         : " + p.maxDepth + " (offspring cap; initial depth " + p.initDepth + ")\n"
             + "tournament / elite: " + p.tournament + " / " + p.eliteFrac + "\n"
             + "crossover / mut.  : " + p.crossoverRate + " / " + p.mutationRate + "\n"
             + "fitness games     : " + FITNESS_GAMES_PER_OPPONENT + " per training opponent (one per side), map rotated per generation\n"
             + "max game cycles   : " + p.maxCycles + "\n"
             + "unit type table   : default (new UnitTypeTable())\n";
    }

    private static String[] concat(String[] a, String[] b) {
        String[] out = Arrays.copyOf(a, a.length + b.length);
        System.arraycopy(b, 0, out, a.length, b.length);
        return out;
    }
}
