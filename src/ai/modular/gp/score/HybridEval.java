package ai.modular.gp.score;

import ai.RandomBiasedAI;
import ai.abstraction.LightRush;
import ai.abstraction.WorkerRush;
import ai.abstraction.pathfinding.AStarPathFinding;
import ai.coac.CoacAI;
import ai.core.AI;
import ai.modular.ModularBot;
import ai.modular.gp.GPNode;
import ai.modular.gp.GPSerial;
import mayariBot.mayari;

import rts.GameState;
import rts.PhysicalGameState;
import rts.units.UnitTypeTable;

public class HybridEval {
    static final String[] MAPS = {
        "maps/8x8/basesWorkers8x8.xml",
        "maps/12x12/basesWorkers12x12.xml",
        "maps/16x16/basesWorkers16x16.xml",
        "maps/24x24/basesWorkers24x24.xml",
    };
    static final String[] OPPS = { "Random", "Worker", "Light", "Coac*", "Mayari*" };
    static GPNode tree;
    static UnitTypeTable utt;
    static int maxCycles = 5000;

    public static void main(String[] args) throws Exception {
        String treeFile = args.length > 0 ? args[0] : "runs/2026-09_v1/hybrid_best.txt";
        int randomGames = args.length > 1 ? Integer.parseInt(args[1]) : 10;
        utt = new UnitTypeTable();
        tree = GPSerial.load(treeFile);

        System.out.println("Hybrid (scripted macro + evolved combat) vs panel, per map. * = held-out.");
        System.out.printf("%-10s", "map \\ opp");
        for (String o : OPPS) System.out.printf("%10s", o);
        System.out.println();

        double[] agg = new double[OPPS.length];
        for (String map : MAPS) {
            System.out.printf("%-10s", shortMap(map));
            for (int oi = 0; oi < OPPS.length; oi++) {
                int games = (oi == 0) ? randomGames : 2;
                int wins = 0;
                for (int g = 0; g < games; g++) {
                    boolean botIsP0 = (g % 2 == 0);
                    if (playOne(oi, botIsP0, map) == (botIsP0 ? 0 : 1)) wins++;
                }
                double wr = 100.0 * wins / games;
                agg[oi] += wr;
                System.out.printf("%9.0f%%", wr);
            }
            System.out.println();
        }

        System.out.printf("%-10s", "MEAN");
        double held = 0;
        for (int oi = 0; oi < OPPS.length; oi++) {
            double m = agg[oi] / MAPS.length;
            System.out.printf("%9.0f%%", m);
            if (OPPS[oi].endsWith("*")) held += m;
        }
        System.out.printf("     held-out avg: %.0f%%%n", held / 2);
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

    private static int playOne(int oi, boolean botIsP0, String map) throws Exception {
        PhysicalGameState pgs = PhysicalGameState.load(map, utt);
        GameState gs = new GameState(pgs, utt);
        AI bot = new ModularBot(utt, new AStarPathFinding(), false, tree.copy());
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

    private static String shortMap(String m) {
        int i = m.lastIndexOf('/');
        return i >= 0 ? m.substring(i + 1).replace("basesWorkers", "").replace(".xml", "") : m;
    }
}
