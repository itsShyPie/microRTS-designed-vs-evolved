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

public class CompareReps {
    static GPNode gpTree;
    static ScoreNode scoreTree;

    public static void main(String[] args) throws Exception {
        String map     = args.length > 0 ? args[0] : "maps/16x16/basesWorkers16x16.xml";
        int games      = args.length > 1 ? Integer.parseInt(args[1]) : 20;
        String gpFile  = args.length > 2 ? args[2] : "runs/2026-09_v1/gp_best.txt";
        String scFile  = args.length > 3 ? args[3] : "runs/2026-09_v1/score_best.txt";
        int maxCycles = 5000;

        UnitTypeTable utt = new UnitTypeTable();
        try { gpTree = GPSerial.load(gpFile); } catch (Exception e) { System.out.println("(no GP-direct tree: " + gpFile + ")"); }
        try { scoreTree = ScoreSerial.load(scFile); } catch (Exception e) { System.out.println("(no GP-score tree: " + scFile + ")"); }

        String[] botNames = { "Modular", "GP-direct", "GP-score" };
        String[] oppNames = { "Random", "Worker", "Light", "Coac", "Mayari" };

        System.out.println("Comparison on " + map + "   " + games + " games/cell (win rate %)");
        System.out.printf("%-10s", "bot \\ opp");
        for (String o : oppNames) System.out.printf("%9s", o);
        System.out.println();
        System.out.println("-------------------------------------------------------------");

        for (int bi = 0; bi < botNames.length; bi++) {
            if (bi == 1 && gpTree == null) continue;
            if (bi == 2 && scoreTree == null) continue;
            System.out.printf("%-10s", botNames[bi]);
            for (int oi = 0; oi < oppNames.length; oi++) {
                int wins = 0;
                for (int g = 0; g < games; g++) {
                    boolean botIsP0 = (g % 2 == 0);
                    int w = playOne(bi, oi, botIsP0, map, utt, maxCycles);
                    if (w == (botIsP0 ? 0 : 1)) wins++;
                }
                System.out.printf("%8.0f%%", 100.0 * wins / games);
            }
            System.out.println();
        }
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
