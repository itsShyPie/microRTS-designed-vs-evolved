package ai.modular.gp;

import ai.abstraction.pathfinding.AStarPathFinding;
import ai.core.AI;
import ai.modular.ModularBot;

import rts.GameState;
import rts.PhysicalGameState;
import rts.units.UnitTypeTable;

public class HeadToHead {
    public static void main(String[] args) throws Exception {
        String treeFile = args.length > 0 ? args[0] : "runs/2026-09_v1/gp_best.txt";
        int games       = args.length > 1 ? Integer.parseInt(args[1]) : 20;
        int maxCycles = 5000;

        UnitTypeTable utt = new UnitTypeTable();
        GPNode tree = GPSerial.load(treeFile);

        String[] maps = {
            "maps/8x8/basesWorkers8x8.xml",
            "maps/12x12/basesWorkers12x12.xml",
            "maps/16x16/basesWorkers16x16.xml",
            "maps/24x24/basesWorkers24x24.xml",
        };

        System.out.println("Head-to-head: ModularBot (designed) vs GPBot (evolved)   " + games + " games/map");
        System.out.println("map                                 modular%   gp%   draws");
        System.out.println("-------------------------------------------------------------");

        for (String map : maps) {
            int mod = 0, gp = 0, draws = 0;
            for (int g = 0; g < games; g++) {
                boolean modIsP0 = (g % 2 == 0);
                PhysicalGameState pgs = PhysicalGameState.load(map, utt);
                GameState gs = new GameState(pgs, utt);
                AI modular = new ModularBot(utt, new AStarPathFinding());
                AI gpbot = new GPBot(utt, new AStarPathFinding(), tree.copy());
                AI a0 = modIsP0 ? modular : gpbot;
                AI a1 = modIsP0 ? gpbot : modular;
                boolean over = false;
                do {
                    gs.issueSafe(a0.getAction(0, gs));
                    gs.issueSafe(a1.getAction(1, gs));
                    over = gs.cycle();
                } while (!over && gs.getTime() < maxCycles);
                int w = gs.winner();
                int modPlayer = modIsP0 ? 0 : 1;
                if (w == -1) draws++;
                else if (w == modPlayer) mod++;
                else gp++;
            }
            System.out.printf("%-34s  %5.1f%%  %5.1f%%    %d%n",
                    map, 100.0 * mod / games, 100.0 * gp / games, draws);
        }
    }
}
