package ai.modular;

import ai.abstraction.LightRush;
import ai.abstraction.WorkerRush;
import ai.abstraction.pathfinding.AStarPathFinding;
import ai.core.AI;

import rts.GameState;
import rts.PhysicalGameState;
import rts.units.UnitTypeTable;

public class SweepConfigs {
    static class Config {
        final String label, mil;
        final int workers, barracks;
        Config(String label, String mil, int workers, int barracks) {
            this.label = label; this.mil = mil; this.workers = workers; this.barracks = barracks;
        }
    }

    public static void main(String[] args) throws Exception {
        String map = args.length > 0 ? args[0] : "maps/24x24/basesWorkers24x24.xml";
        int games = args.length > 1 ? Integer.parseInt(args[1]) : 20;
        int maxCycles = 5000;

        UnitTypeTable utt = new UnitTypeTable();

        Config[] configs = {
            new Config("Ranged x1 (current)", "Ranged", 2, 1),
            new Config("Light  x1 (mirror) ", "Light",  2, 1),
            new Config("Ranged x2 (thruput)", "Ranged", 3, 2),
            new Config("Light  x2 (both)   ", "Light",  3, 2),
        };

        System.out.println("Map: " + map + "   games/opponent: " + games + "   (basic micro)");
        System.out.println("config                 | vs WorkerRush | vs LightRush");
        System.out.println("---------------------------------------------------------");

        for (Config c : configs) {
            double wrW = winRate(c, 1, games, map, utt, maxCycles);
            double wrL = winRate(c, 2, games, map, utt, maxCycles);
            System.out.printf("%-22s |     %5.1f%%    |    %5.1f%%%n", c.label, wrW, wrL);
        }
    }

    private static double winRate(Config c, int oppKind, int games, String map,
                                  UnitTypeTable utt, int maxCycles) throws Exception {
        int wins = 0;
        for (int g = 0; g < games; g++) {
            boolean botIsPlayer0 = (g % 2 == 0);
            AI opp = (oppKind == 1) ? new WorkerRush(utt, new AStarPathFinding())
                                    : new LightRush(utt, new AStarPathFinding());
            int winner = playOne(c, map, utt, botIsPlayer0, opp, maxCycles);
            int botPlayer = botIsPlayer0 ? 0 : 1;
            if (winner == botPlayer) wins++;
        }
        return 100.0 * wins / games;
    }

    private static int playOne(Config c, String map, UnitTypeTable utt, boolean botIsPlayer0,
                               AI opp, int maxCycles) throws Exception {
        PhysicalGameState pgs = PhysicalGameState.load(map, utt);
        GameState gs = new GameState(pgs, utt);

        ModularBot bot = new ModularBot(utt, new AStarPathFinding(), false);
        bot.configure(c.mil, c.workers, c.barracks);

        AI ai0 = botIsPlayer0 ? bot : opp;
        AI ai1 = botIsPlayer0 ? opp : bot;

        boolean gameover = false;
        do {
            gs.issueSafe(ai0.getAction(0, gs));
            gs.issueSafe(ai1.getAction(1, gs));
            gameover = gs.cycle();
        } while (!gameover && gs.getTime() < maxCycles);

        bot.gameOver(gs.winner());
        opp.gameOver(gs.winner());
        return gs.winner();
    }
}
