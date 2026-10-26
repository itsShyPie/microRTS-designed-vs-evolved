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
import mayariBot.mayari;

import rts.GameState;
import rts.PhysicalGameState;
import rts.units.UnitTypeTable;

public class Tournament {
    static GPNode gpTree;
    static ScoreNode scoreTree;

    static final String[] MAPS = {
        "maps/8x8/basesWorkers8x8.xml",
        "maps/12x12/basesWorkers12x12.xml",
        "maps/16x16/basesWorkers16x16.xml",
        "maps/24x24/basesWorkers24x24.xml",
    };
    static final String[] BOTS = { "Modular", "GP-direct", "GP-score" };
    static final String[] OPPS = { "Random", "Worker", "Light", "Coac*", "Mayari*" };

    public static void main(String[] args) throws Exception {
        int games     = args.length > 0 ? Integer.parseInt(args[0]) : 10;
        String gpFile = args.length > 1 ? args[1] : "runs/2026-09_v1/gp_best.txt";
        String scFile = args.length > 2 ? args[2] : "runs/2026-09_v1/score_best.txt";
        int maxCycles = 5000;

        UnitTypeTable utt = new UnitTypeTable();
        try { gpTree = GPSerial.load(gpFile); } catch (Exception e) { System.out.println("(no GP-direct tree)"); }
        try { scoreTree = ScoreSerial.load(scFile); } catch (Exception e) { System.out.println("(no GP-score tree)"); }

        System.out.println("Tournament: " + games + " games/cell, sides swapped. * = held-out opponent.");

        double[][] agg = new double[BOTS.length][OPPS.length];

        for (String map : MAPS) {
            System.out.println("\n=== " + map + " ===");
            header();
            for (int bi = 0; bi < BOTS.length; bi++) {
                if (bi == 1 && gpTree == null) continue;
                if (bi == 2 && scoreTree == null) continue;
                System.out.printf("%-11s", BOTS[bi]);
                for (int oi = 0; oi < OPPS.length; oi++) {
                    double wr = winRate(bi, oi, games, map, utt, maxCycles);
                    agg[bi][oi] += wr;
                    System.out.printf("%9.0f%%", wr);
                }
                System.out.println();
            }
        }

        System.out.println("\n=== AGGREGATE (mean win rate across " + MAPS.length + " maps) ===");
        header();
        for (int bi = 0; bi < BOTS.length; bi++) {
            if (bi == 1 && gpTree == null) continue;
            if (bi == 2 && scoreTree == null) continue;
            System.out.printf("%-11s", BOTS[bi]);
            double heldOut = 0; int ho = 0;
            for (int oi = 0; oi < OPPS.length; oi++) {
                double mean = agg[bi][oi] / MAPS.length;
                System.out.printf("%9.0f%%", mean);
                if (OPPS[oi].endsWith("*")) { heldOut += mean; ho++; }
            }
            System.out.printf("     held-out avg: %.0f%%%n", heldOut / ho);
        }
    }

    private static void header() {
        System.out.printf("%-11s", "bot \\ opp");
        for (String o : OPPS) System.out.printf("%10s", o);
        System.out.println();
    }

    private static double winRate(int bi, int oi, int games, String map,
                                  UnitTypeTable utt, int maxCycles) throws Exception {
        int wins = 0;
        for (int g = 0; g < games; g++) {
            boolean botIsP0 = (g % 2 == 0);
            int w = playOne(bi, oi, botIsP0, map, utt, maxCycles);
            if (w == (botIsP0 ? 0 : 1)) wins++;
        }
        return 100.0 * wins / games;
    }

    private static AI makeBot(int bi, UnitTypeTable utt) {
        switch (bi) {
            case 0:  return new ModularBot(utt, new AStarPathFinding());
            case 1:  return new GPBot(utt, new AStarPathFinding(), gpTree.copy());
            default: return new ScoreGPBot(utt, scoreTree.copy());
        }
    }

    private static AI makeOpp(int oi, UnitTypeTable utt) {
        switch (oi) {
            case 0:  return new RandomBiasedAI();
            case 1:  return new WorkerRush(utt, new AStarPathFinding());
            case 2:  return new LightRush(utt, new AStarPathFinding());
            case 3:  return new CoacAI(utt);
            default: return new mayari(utt);
        }
    }

    private static int playOne(int bi, int oi, boolean botIsP0, String map,
                               UnitTypeTable utt, int maxCycles) throws Exception {
        PhysicalGameState pgs = PhysicalGameState.load(map, utt);
        GameState gs = new GameState(pgs, utt);
        AI bot = makeBot(bi, utt);
        AI opp = makeOpp(oi, utt);
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
