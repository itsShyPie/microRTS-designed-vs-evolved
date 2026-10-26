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
import rts.units.Unit;
import rts.units.UnitTypeTable;

public class DiagnoseGame {
    public static void main(String[] args) throws Exception {
        String map = args.length > 0 ? args[0] : "maps/24x24/basesWorkers24x24.xml";
        String oppName = args.length > 1 ? args[1] : "light";
        boolean smart = args.length > 2 && Boolean.parseBoolean(args[2]);
        int maxCycles = 5000;
        int period = 200;

        UnitTypeTable utt = new UnitTypeTable();
        AI opp;
        if (oppName.equalsIgnoreCase("worker")) opp = new WorkerRush(utt, new AStarPathFinding());
        else if (oppName.equalsIgnoreCase("random")) opp = new RandomBiasedAI();
        else if (oppName.equalsIgnoreCase("coac")) opp = new CoacAI(utt);
        else if (oppName.equalsIgnoreCase("mayari")) opp = new mayari(utt);
        else { opp = new LightRush(utt, new AStarPathFinding()); oppName = "light"; }

        PhysicalGameState pgs = PhysicalGameState.load(map, utt);
        GameState gs = new GameState(pgs, utt);
        AI bot = new ModularBot(utt, new AStarPathFinding(), smart);

        System.out.println("Diagnose: us (player0, micro=" + (smart ? "smart" : "basic")
                + ") vs " + oppName + " on " + map);
        System.out.println("cycle |  us  work/combat/bldg  res  |  them work/combat/bldg  res");
        System.out.println("---------------------------------------------------------------");

        boolean gameover = false;
        int nextSnap = 0;
        do {
            if (gs.getTime() >= nextSnap) {
                snapshot(gs);
                nextSnap += period;
            }
            gs.issueSafe(bot.getAction(0, gs));
            gs.issueSafe(opp.getAction(1, gs));
            gameover = gs.cycle();
        } while (!gameover && gs.getTime() < maxCycles);

        snapshot(gs);
        bot.gameOver(gs.winner());
        opp.gameOver(gs.winner());

        int w = gs.winner();
        System.out.println("---------------------------------------------------------------");
        System.out.println("winner = " + (w == 0 ? "US" : w == 1 ? "THEM" : "draw")
                + "   at cycle " + gs.getTime());
    }

    private static void snapshot(GameState gs) {
        PhysicalGameState pgs = gs.getPhysicalGameState();
        int[] work = new int[2], combat = new int[2], bldg = new int[2];
        for (Unit u : pgs.getUnits()) {
            int pl = u.getPlayer();
            if (pl != 0 && pl != 1) continue;
            if (u.getType().canHarvest) work[pl]++;
            else if (u.getType().canAttack) combat[pl]++;
            else bldg[pl]++;
        }
        int r0 = gs.getPlayer(0).getResources();
        int r1 = gs.getPlayer(1).getResources();
        System.out.printf("%5d |      %d / %d / %d       %2d  |      %d / %d / %d       %2d%n",
                gs.getTime(), work[0], combat[0], bldg[0], r0, work[1], combat[1], bldg[1], r1);
    }
}
