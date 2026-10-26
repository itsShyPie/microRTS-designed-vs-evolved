package ai.modular;

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

public class RunModularTest {
    public static void main(String[] args) throws Exception {
        String map = args.length > 0 ? args[0] : "maps/16x16/basesWorkers16x16.xml";
        int games = args.length > 1 ? Integer.parseInt(args[1]) : 20;
        int maxCycles = 5000;

        UnitTypeTable utt = new UnitTypeTable();
        String[] names = { "RandomBiasedAI", "WorkerRush", "LightRush", "CoacAI", "Mayari" };

        System.out.println("Map: " + map + "   games/opponent: " + games);
        System.out.println("micro: basic = v0 (attack nearest), smart = focus fire + kiting");
        System.out.println("-----------------------------------------------------------");

        for (int o = 0; o < names.length; o++) {
            for (boolean smart : new boolean[] { false, true }) {
                int wins = 0, losses = 0, draws = 0;
                for (int g = 0; g < games; g++) {
                    boolean botIsPlayer0 = (g % 2 == 0);
                    AI opp = makeOpponent(o, utt);
                    int winner = playOne(map, utt, botIsPlayer0, opp, maxCycles, smart);
                    int botPlayer = botIsPlayer0 ? 0 : 1;
                    if (winner == -1) draws++;
                    else if (winner == botPlayer) wins++;
                    else losses++;
                }
                double wr = 100.0 * wins / games;
                System.out.printf("%-16s [%-5s]  W:%2d  L:%2d  D:%2d   winrate=%.1f%%%n",
                        names[o], smart ? "smart" : "basic", wins, losses, draws, wr);
            }
            System.out.println();
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

    private static int playOne(String map, UnitTypeTable utt, boolean botIsPlayer0,
                               AI opp, int maxCycles, boolean smartMicro) throws Exception {
        PhysicalGameState pgs = PhysicalGameState.load(map, utt);
        GameState gs = new GameState(pgs, utt);

        AI bot = new ModularBot(utt, new AStarPathFinding(), smartMicro);
        AI ai0 = botIsPlayer0 ? bot : opp;
        AI ai1 = botIsPlayer0 ? opp : bot;

        boolean gameover = false;
        do {
            gs.issueSafe(ai0.getAction(0, gs));
            gs.issueSafe(ai1.getAction(1, gs));
            gameover = gs.cycle();
        } while (!gameover && gs.getTime() < maxCycles);

        ai0.gameOver(gs.winner());
        ai1.gameOver(gs.winner());
        return gs.winner();
    }
}
