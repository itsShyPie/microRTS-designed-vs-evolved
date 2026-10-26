package ai.modular.gp;

import ai.RandomBiasedAI;
import ai.abstraction.LightRush;
import ai.abstraction.WorkerRush;
import ai.abstraction.pathfinding.AStarPathFinding;
import ai.coac.CoacAI;
import ai.core.AI;
import mayariBot.mayari;

import rts.GameState;
import rts.PhysicalGameState;
import rts.units.UnitTypeTable;

public class GPEval {
    public static void main(String[] args) throws Exception {
        String treeFile = args.length > 0 ? args[0] : "runs/2026-09_v1/gp_best.txt";
        String map      = args.length > 1 ? args[1] : "maps/16x16/basesWorkers16x16.xml";
        int games       = args.length > 2 ? Integer.parseInt(args[2]) : 20;
        int maxCycles = 5000;

        UnitTypeTable utt = new UnitTypeTable();
        GPNode tree = GPSerial.load(treeFile);
        String[] names = { "RandomBiasedAI", "WorkerRush", "LightRush", "CoacAI", "Mayari" };

        System.out.println("GP eval: " + treeFile + " on " + map + "   games/opponent: " + games);
        System.out.println("-----------------------------------------------");

        for (int o = 0; o < names.length; o++) {
            int wins = 0, losses = 0, draws = 0;
            for (int g = 0; g < games; g++) {
                boolean botIsP0 = (g % 2 == 0);
                AI opp = makeOpponent(o, utt);
                int winner = playOne(tree, map, utt, botIsP0, opp, maxCycles);
                int me = botIsP0 ? 0 : 1;
                if (winner == -1) draws++;
                else if (winner == me) wins++;
                else losses++;
            }
            System.out.printf("%-16s W:%2d  L:%2d  D:%2d   winrate=%.1f%%%n",
                    names[o], wins, losses, draws, 100.0 * wins / games);
        }
    }

    private static AI makeOpponent(int o, UnitTypeTable utt) {
        switch (o) {
            case 0:  return new RandomBiasedAI();
            case 1:  return new WorkerRush(utt, new AStarPathFinding());
            case 2:  return new LightRush(utt, new AStarPathFinding());
            case 3:  return new CoacAI(utt);
            default: return new mayari(utt);
        }
    }

    private static int playOne(GPNode tree, String map, UnitTypeTable utt, boolean botIsP0,
                               AI opp, int maxCycles) throws Exception {
        PhysicalGameState pgs = PhysicalGameState.load(map, utt);
        GameState gs = new GameState(pgs, utt);
        AI bot = new GPBot(utt, new AStarPathFinding(), tree.copy());
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
