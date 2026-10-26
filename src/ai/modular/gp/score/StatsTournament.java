package ai.modular.gp.score;

import ai.RandomBiasedAI;
import ai.abstraction.LightRush;
import ai.abstraction.WorkerRush;
import ai.abstraction.pathfinding.AStarPathFinding;
import ai.coac.CoacAI;
import ai.core.AI;
import ai.modular.ModularBot;
import ai.modular.gp.GPBot;
import ai.modular.gp.GPNode;
import ai.modular.gp.GPSerial;
import ai.modular.gp.Stats;
import mayariBot.mayari;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import rts.GameState;
import rts.PhysicalGameState;
import rts.units.UnitTypeTable;

public class StatsTournament {
    static final String[] MAPS = {
        "maps/8x8/basesWorkers8x8.xml",
        "maps/12x12/basesWorkers12x12.xml",
        "maps/16x16/basesWorkers16x16.xml",
        "maps/24x24/basesWorkers24x24.xml",
    };
    static final String[] OPPS = { "Random", "Worker", "Light", "Coac*", "Mayari*" };
    static final int COAC = 3, MAYARI = 4;
    static int randomGames;
    static int maxCycles = 5000;
    static UnitTypeTable utt;

    public static void main(String[] args) throws Exception {
        randomGames = args.length > 0 ? Integer.parseInt(args[0]) : 10;
        int seeds   = args.length > 1 ? Integer.parseInt(args[1]) : 5;
        String dir  = args.length > 2 ? args[2] : "runs/2026-09_v1";
        utt = new UnitTypeTable();

        List<GPNode> gpTrees = new ArrayList<>();
        List<ScoreNode> scTrees = new ArrayList<>();
        List<GPNode> hyTrees = new ArrayList<>();
        for (int s = 1; s <= seeds; s++) {
            try { gpTrees.add(GPSerial.load(dir + "/gp_seed" + s + ".txt")); } catch (Exception ignore) {}
            try { scTrees.add(ScoreSerial.load(dir + "/score_seed" + s + ".txt")); } catch (Exception ignore) {}
            try { hyTrees.add(GPSerial.load(dir + "/hybrid_seed" + s + ".txt")); } catch (Exception ignore) {}
        }
        System.out.println("Loaded " + gpTrees.size() + " GP-direct seeds, " + scTrees.size() + " GP-score seeds, "
                + hyTrees.size() + " hybrid seeds.");
        System.out.println("Random games/cell: " + randomGames + "; deterministic opponents: 2 (sides). * = held-out.\n");

        double[][] gp = new double[gpTrees.size()][];
        for (int i = 0; i < gpTrees.size(); i++) gp[i] = evalTree(1, gpTrees.get(i), null);
        double[][] sc = new double[scTrees.size()][];
        for (int i = 0; i < scTrees.size(); i++) sc[i] = evalTree(2, null, scTrees.get(i));
        double[][] hy = new double[hyTrees.size()][];
        for (int i = 0; i < hyTrees.size(); i++) hy[i] = evalTree(3, hyTrees.get(i), null);
        double[] modular = evalTree(0, null, null);

        printSingle("Modular (designed, reference)", modular);
        printDist("GP-direct", gp);
        printDist("GP-score", sc);
        printDist("Hybrid", hy);

        double[] gpHeld = heldOut(gp);
        double[] scHeld = heldOut(sc);
        double[] hyHeld = heldOut(hy);
        System.out.println("\nHeld-out (Coac+Mayari) win rate per seed:");
        System.out.println("  GP-direct: " + Arrays.toString(round(gpHeld)));
        System.out.println("  GP-score : " + Arrays.toString(round(scHeld)));
        System.out.println("  Hybrid   : " + Arrays.toString(round(hyHeld)));

        System.out.println();
        compare("GP-direct vs GP-score", gpHeld, scHeld);
        compare("Hybrid vs GP-direct", hyHeld, gpHeld);
        compare("Hybrid vs GP-score", hyHeld, scHeld);
        System.out.println("(Small-n note: with few seeds the p-value is indicative only; raise numSeeds for power.)");
    }

    private static void compare(String label, double[] a, double[] b) {
        if (a.length == 0 || b.length == 0) return;
        double u = Stats.mannWhitneyU(a, b);
        double p = Stats.approxP(u, a.length, b.length);
        System.out.printf("Mann-Whitney U (%s, held-out): U=%.1f  approx p=%.3f%n", label, u, p);
    }

    private static double[] evalTree(int repType, GPNode g, ScoreNode s) throws Exception {
        double[] row = new double[OPPS.length];
        for (int oi = 0; oi < OPPS.length; oi++) {
            int gamesPerMap = (oi == 0) ? randomGames : 2;
            int wins = 0, total = 0;
            for (String map : MAPS) {
                for (int game = 0; game < gamesPerMap; game++) {
                    boolean botIsP0 = (game % 2 == 0);
                    if (playOne(repType, g, s, oi, botIsP0, map) == (botIsP0 ? 0 : 1)) wins++;
                    total++;
                }
            }
            row[oi] = 100.0 * wins / total;
        }
        return row;
    }

    private static double[] heldOut(double[][] perSeed) {
        double[] out = new double[perSeed.length];
        for (int i = 0; i < perSeed.length; i++) out[i] = (perSeed[i][COAC] + perSeed[i][MAYARI]) / 2.0;
        return out;
    }

    private static void printSingle(String name, double[] row) {
        System.out.printf("%-30s", name);
        for (double v : row) System.out.printf("%9.0f%%", v);
        System.out.println();
        header();
    }

    private static void printDist(String name, double[][] perSeed) {
        if (perSeed.length == 0) { System.out.println(name + ": no seeds"); return; }
        double[] mean = new double[OPPS.length], min = new double[OPPS.length], max = new double[OPPS.length];
        Arrays.fill(min, 1e9); Arrays.fill(max, -1e9);
        for (double[] r : perSeed) for (int o = 0; o < OPPS.length; o++) {
            mean[o] += r[o] / perSeed.length;
            min[o] = Math.min(min[o], r[o]);
            max[o] = Math.max(max[o], r[o]);
        }
        System.out.printf("%-30s", name + " mean");
        for (double v : mean) System.out.printf("%9.0f%%", v);
        System.out.println();
        System.out.printf("%-30s", "   [min,max]");
        for (int o = 0; o < OPPS.length; o++) System.out.printf("%9s", (int) min[o] + "-" + (int) max[o]);
        System.out.println();
    }

    private static void header() {
        System.out.printf("%-30s", "");
        for (String o : OPPS) System.out.printf("%10s", o);
        System.out.println();
    }

    private static double[] round(double[] v) {
        double[] r = new double[v.length];
        for (int i = 0; i < v.length; i++) r[i] = Math.round(v[i]);
        return r;
    }

    private static AI makeBot(int repType, GPNode g, ScoreNode s) {
        switch (repType) {
            case 0:  return new ModularBot(utt, new AStarPathFinding());
            case 1:  return new GPBot(utt, new AStarPathFinding(), g.copy());
            case 3:  return new ModularBot(utt, new AStarPathFinding(), false, g.copy());
            default: return new ScoreGPBot(utt, s.copy());
        }
    }

    private static AI makeOpp(int oi) {
        switch (oi) {
            case 0:  return new RandomBiasedAI();
            case 1:  return new WorkerRush(utt, new AStarPathFinding());
            case 2:  return new LightRush(utt, new AStarPathFinding());
            case 3:  return new CoacAI(utt);
            default: return new mayari(utt);
        }
    }

    private static int playOne(int repType, GPNode g, ScoreNode s, int oi, boolean botIsP0, String map) throws Exception {
        PhysicalGameState pgs = PhysicalGameState.load(map, utt);
        GameState gs = new GameState(pgs, utt);
        AI bot = makeBot(repType, g, s);
        AI opp = makeOpp(oi);
        AI a0 = botIsP0 ? bot : opp;
        AI a1 = botIsP0 ? opp : bot;
        boolean over = false;
        do {
            gs.issueSafe(a0.getAction(0, gs));
            gs.issueSafe(a1.getAction(1, gs));
            over = gs.cycle();
        } while (!over && gs.getTime() < maxCycles);
        bot.gameOver(gs.winner());
        opp.gameOver(gs.winner());
        return gs.winner();
    }
}
